#!/usr/bin/env bash
# Coordinated launcher: starts the mock server out-of-process, waits for readiness, runs the
# benchmark client (optionally under a profiler), and tears the server down on exit.
#
# Usage: scripts/benchmark.sh --client <v1|v2-sync|v2-async|smithy> [runner options] [launcher options]
#
# Launcher-only options (everything else is passed through to BenchmarkRunner):
#   --port N              port for the auto-launched mock server (default: 19080)
#   --no-server           do not launch a server; requires --endpoint pointing at a running one
#   --profile MODE        jfr | cpu | alloc | wall  (cpu/alloc/wall need async-profiler)
#   --profile-format FMT  html | jfr — output format for cpu/alloc/wall modes (default: html)
#   --profile-file PATH   exact profiler output path (default: <profile-out>/<client>-<mode>-<ts>.<ext>)
#   --profile-out DIR     profiler output directory (default: ./profiles)
#   --jvm-args "..."      extra JVM args for the client JVM
#
# Environment:
#   ASYNC_PROFILER_LIB  path to libasyncProfiler (auto-detected from common install locations)
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

PORT=19080
LAUNCH_SERVER=1
PROFILE=""
PROFILE_FORMAT="html"
PROFILE_FILE=""
PROFILE_OUT="$DIR/profiles"
EXTRA_JVM_ARGS=""
SERVER_JVM_ARGS=""
JAR=""
ENDPOINT=""
PIN_CLIENT=""
PIN_SERVER=""
RUNNER_ARGS=()
CLIENT="client"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --port)        PORT="$2"; shift 2 ;;
        --no-server)   LAUNCH_SERVER=0; shift ;;
        --profile)        PROFILE="$2"; shift 2 ;;
        --profile-format) PROFILE_FORMAT="$2"; shift 2 ;;
        --profile-file)   PROFILE_FILE="$2"; shift 2 ;;
        --profile-out)    PROFILE_OUT="$2"; shift 2 ;;
        --jvm-args)        EXTRA_JVM_ARGS="$2"; shift 2 ;;
        --server-jvm-args) SERVER_JVM_ARGS="$2"; shift 2 ;;
        --pin-client)  PIN_CLIENT="$2"; shift 2 ;;
        --pin-server)  PIN_SERVER="$2"; shift 2 ;;
        --jar)         JAR="$2"; shift 2 ;;
        --endpoint)    ENDPOINT="$2"; LAUNCH_SERVER=0; RUNNER_ARGS+=("$1" "$2"); shift 2 ;;
        --client)      CLIENT="$2"; RUNNER_ARGS+=("$1" "$2"); shift 2 ;;
        *)             RUNNER_ARGS+=("$1"); shift ;;
    esac
done

if [[ $LAUNCH_SERVER -eq 0 && -z "$ENDPOINT" ]]; then
    echo "error: --no-server requires --endpoint" >&2
    exit 2
fi

# ---- CPU pinning ----
# The client and the mock server share this machine, and the server is not a passive party: it is the
# throughput ceiling and it competes for the same cores. Pinning them to disjoint CPU sets is the
# single biggest available reduction in run-to-run variance — on an unpinned laptop, going from 1 to 2
# client threads cost 4x precision purely through that interference.
CLIENT_PREFIX=()
SERVER_PREFIX=()
if [[ -n "$PIN_CLIENT" || -n "$PIN_SERVER" ]]; then
    if ! command -v taskset >/dev/null 2>&1; then
        echo "error: --pin-client/--pin-server need taskset, which is Linux-only" >&2
        exit 2
    fi
    if [[ -n "$PIN_CLIENT" ]]; then
        CLIENT_PREFIX=(taskset -c "$PIN_CLIENT")
    fi
    if [[ -n "$PIN_SERVER" ]]; then
        SERVER_PREFIX=(taskset -c "$PIN_SERVER")
    fi
fi

# ---- Resolve what to run: a self-contained shaded jar, or the local build's classpath ----
# With --jar, everything (both SDKs, smithy-java, the mock server and the runner) comes from that
# one file, so no build is needed and nothing is read from the local Maven repo. This is how
# benchmarks run on a remote host, and how two variants are compared back-to-back without
# reinstalling artifacts between runs.
if [[ -n "$JAR" ]]; then
    if [[ ! -f "$JAR" ]]; then
        echo "error: --jar $JAR not found" >&2
        exit 2
    fi
    CP="$JAR"
else
    if [[ ! -f "$DIR/target/classpath.txt" || ! -d "$DIR/target/classes" ]]; then
        echo "Building (first run)..."
        (cd "$DIR" && mvn -q package)
    fi
    CP="$DIR/target/classes:$(cat "$DIR/target/classpath.txt")"
fi

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
    # Readiness is confirmed from OUR child's own READY line, not from the port answering /ping.
    # A /ping probe cannot tell our server apart from someone else's: a stale MockDdbServer left
    # running on this port answers instantly, the launched child dies with "Address already in use",
    # and the benchmark then measures happily against a server from an unknown build. That happened —
    # a server from a two-hour-old jar served an entire experiment before the mismatched
    # server_requests count gave it away.
    SERVER_LOG="$(mktemp "${TMPDIR:-/tmp}/mockddb.XXXXXXXX.log")"
    # --enable-native-access silences the JNA warning oshi triggers when the server reads its own
    # CPU for /stats. Four lines per server start is four lines times every run in a collection.
    ${SERVER_PREFIX[@]+"${SERVER_PREFIX[@]}"} \
        java --enable-native-access=ALL-UNNAMED ${SERVER_JVM_ARGS:+$SERVER_JVM_ARGS} \
        -cp "$CP" \
        software.amazon.awssdk.benchmark.e2e.MockDdbServer --port "$PORT" > "$SERVER_LOG" 2>&1 &
    SERVER_PID=$!
    ENDPOINT="http://127.0.0.1:$PORT"
    RUNNER_ARGS+=(--endpoint "$ENDPOINT")

    server_failed() {
        echo "error: mock server did not start: $1" >&2
        sed 's/^/  /' "$SERVER_LOG" >&2
        if grep -q "Address already in use" "$SERVER_LOG" 2>/dev/null; then
            echo "  hint: something else is already on port $PORT — very likely a MockDdbServer left" >&2
            echo "        over from an earlier run, possibly from a different build. Find it with" >&2
            echo "          lsof -nP -iTCP:$PORT -sTCP:LISTEN" >&2
            echo "        then kill it, or pass --port to use a different one." >&2
        fi
        rm -f "$SERVER_LOG"
        exit 1
    }

    SERVER_READY=0
    for _ in $(seq 1 150); do
        if grep -q "^READY " "$SERVER_LOG" 2>/dev/null; then
            SERVER_READY=1
            break
        fi
        # Diagnose from the log rather than from the process: a backgrounded child that has already
        # exited still satisfies `kill -0` until the shell reaps it, so waiting on liveness alone
        # turns a one-second port conflict into a 30-second timeout with no explanation.
        if grep -q "Address already in use" "$SERVER_LOG" 2>/dev/null; then
            server_failed "port $PORT is already in use"
        fi
        if grep -q "^Exception\|^Caused by" "$SERVER_LOG" 2>/dev/null; then
            server_failed "it threw during startup"
        fi
        if ! kill -0 "$SERVER_PID" 2>/dev/null; then
            server_failed "the process exited"
        fi
        sleep 0.2
    done
    if [[ $SERVER_READY -eq 0 ]]; then
        server_failed "no READY line within 30s"
    fi
    # Surface the server's own line so the run log records which server answered.
    grep "^READY " "$SERVER_LOG"
    rm -f "$SERVER_LOG"
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
    STAMP="$(date +%Y%m%d-%H%M%S)"
    case "$PROFILE" in
        jfr)
            OUT="${PROFILE_FILE:-$PROFILE_OUT/$CLIENT-$STAMP.jfr}"
            mkdir -p "$(dirname "$OUT")"
            JVM_ARGS+=("-XX:StartFlightRecording=filename=$OUT,settings=profile,dumponexit=true")
            ;;
        cpu|alloc|wall)
            LIB="$(find_async_profiler)" || {
                echo "error: async-profiler library not found; set ASYNC_PROFILER_LIB" >&2
                exit 1
            }
            case "$PROFILE_FORMAT" in
                html|jfr) ;;
                *)
                    echo "error: unknown --profile-format '$PROFILE_FORMAT' (html|jfr)" >&2
                    exit 2
                    ;;
            esac
            # async-profiler picks the output format from the file extension (.html/.jfr).
            OUT="${PROFILE_FILE:-$PROFILE_OUT/$CLIENT-$PROFILE-$STAMP.$PROFILE_FORMAT}"
            mkdir -p "$(dirname "$OUT")"
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
# Tested against the plain strings rather than array lengths: ${#arr[@]} on an unset array is also
# an error under `set -u` in bash 3.2.
if [[ -n "$PIN_CLIENT" || -n "$PIN_SERVER" ]]; then
    echo "pinning: client=[${PIN_CLIENT:-unpinned}] server=[${PIN_SERVER:-unpinned}]"
fi
${CLIENT_PREFIX[@]+"${CLIENT_PREFIX[@]}"} \
    java "${JVM_ARGS[@]}" -cp "$CP" \
    software.amazon.awssdk.benchmark.e2e.BenchmarkRunner "${RUNNER_ARGS[@]}"
