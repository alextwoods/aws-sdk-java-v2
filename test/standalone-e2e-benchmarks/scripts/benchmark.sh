#!/usr/bin/env bash
# Coordinated launcher: starts the mock server out-of-process, waits for readiness, runs the
# benchmark client (optionally under a profiler), and tears the server down on exit.
#
# Usage: scripts/benchmark.sh --client <v1|v2-sync|v2-async|smithy> [runner options] [launcher options]
#
# Launcher-only options (everything else is passed through to BenchmarkRunner):
#   --port N            port for the auto-launched mock server (default: 19080)
#   --no-server         do not launch a server; requires --endpoint pointing at a running one
#   --profile MODE      jfr | cpu | alloc | wall  (cpu/alloc/wall need async-profiler)
#   --profile-out DIR   profiler output directory (default: ./profiles)
#   --jvm-args "..."    extra JVM args for the client JVM
#
# Environment:
#   ASYNC_PROFILER_LIB  path to libasyncProfiler (auto-detected from common install locations)
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

PORT=19080
LAUNCH_SERVER=1
PROFILE=""
PROFILE_OUT="$DIR/profiles"
EXTRA_JVM_ARGS=""
ENDPOINT=""
RUNNER_ARGS=()
CLIENT="client"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --port)        PORT="$2"; shift 2 ;;
        --no-server)   LAUNCH_SERVER=0; shift ;;
        --profile)     PROFILE="$2"; shift 2 ;;
        --profile-out) PROFILE_OUT="$2"; shift 2 ;;
        --jvm-args)    EXTRA_JVM_ARGS="$2"; shift 2 ;;
        --endpoint)    ENDPOINT="$2"; LAUNCH_SERVER=0; RUNNER_ARGS+=("$1" "$2"); shift 2 ;;
        --client)      CLIENT="$2"; RUNNER_ARGS+=("$1" "$2"); shift 2 ;;
        *)             RUNNER_ARGS+=("$1"); shift ;;
    esac
done

if [[ $LAUNCH_SERVER -eq 0 && -z "$ENDPOINT" ]]; then
    echo "error: --no-server requires --endpoint" >&2
    exit 2
fi

# ---- Build if needed ----
if [[ ! -f "$DIR/target/classpath.txt" || ! -d "$DIR/target/classes" ]]; then
    echo "Building (first run)..."
    (cd "$DIR" && mvn -q package)
fi
CP="$DIR/target/classes:$(cat "$DIR/target/classpath.txt")"

# ---- Start the mock server out-of-process ----
SERVER_PID=""
cleanup() {
    if [[ -n "$SERVER_PID" ]]; then
        kill "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT

if [[ $LAUNCH_SERVER -eq 1 ]]; then
    java -cp "$CP" software.amazon.awssdk.benchmark.e2e.MockDdbServer --port "$PORT" &
    SERVER_PID=$!
    ENDPOINT="http://127.0.0.1:$PORT"
    RUNNER_ARGS+=(--endpoint "$ENDPOINT")
    for _ in $(seq 1 100); do
        if curl -sf "$ENDPOINT/ping" >/dev/null 2>&1; then
            break
        fi
        if ! kill -0 "$SERVER_PID" 2>/dev/null; then
            echo "error: mock server exited during startup" >&2
            exit 1
        fi
        sleep 0.2
    done
fi

# ---- Profiler flags ----
find_async_profiler() {
    for candidate in \
        "${ASYNC_PROFILER_LIB:-}" \
        /opt/homebrew/opt/async-profiler/lib/libasyncProfiler.dylib \
        /usr/local/opt/async-profiler/lib/libasyncProfiler.dylib \
        /opt/async-profiler/lib/libasyncProfiler.so \
        /usr/lib/async-profiler/libasyncProfiler.so; do
        if [[ -n "$candidate" && -e "$candidate" ]]; then
            echo "$candidate"
            return 0
        fi
    done
    return 1
}

JVM_ARGS=(--enable-native-access=ALL-UNNAMED)
if [[ -n "$PROFILE" ]]; then
    mkdir -p "$PROFILE_OUT"
    STAMP="$(date +%Y%m%d-%H%M%S)"
    case "$PROFILE" in
        jfr)
            OUT="$PROFILE_OUT/$CLIENT-$STAMP.jfr"
            JVM_ARGS+=("-XX:StartFlightRecording=filename=$OUT,settings=profile,dumponexit=true")
            ;;
        cpu|alloc|wall)
            LIB="$(find_async_profiler)" || {
                echo "error: async-profiler library not found; set ASYNC_PROFILER_LIB" >&2
                exit 1
            }
            OUT="$PROFILE_OUT/$CLIENT-$PROFILE-$STAMP.html"
            JVM_ARGS+=("-agentpath:$LIB=start,event=$PROFILE,file=$OUT")
            ;;
        *)
            echo "error: unknown --profile mode '$PROFILE' (jfr|cpu|alloc|wall)" >&2
            exit 2
            ;;
    esac
    echo "profiler output: $OUT"
fi
if [[ -n "$EXTRA_JVM_ARGS" ]]; then
    # shellcheck disable=SC2206
    JVM_ARGS+=($EXTRA_JVM_ARGS)
fi

# ---- Run the client ----
java "${JVM_ARGS[@]}" -cp "$CP" software.amazon.awssdk.benchmark.e2e.BenchmarkRunner "${RUNNER_ARGS[@]}"
