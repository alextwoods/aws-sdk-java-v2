#!/usr/bin/env bash
# Full benchmark collection: every client x every scenario, with timing repetitions, async-profiler
# CPU/alloc JFR recordings, and SDK metrics — written as raw data under pipeline_benchmark2/raw/.
#
# Layout (RUNID = yyyymmdd-HHMM):
#   pipeline_benchmark2/raw/<RUNID>/manifest.md          run metadata + every command executed
#   pipeline_benchmark2/raw/<RUNID>/results.csv          timing rows only (reps x cases)
#   pipeline_benchmark2/raw/<RUNID>/<caseid>/            caseid = <client>_<scenario>
#       timing-rep<N>.log    stdout of each clean timing run
#       cpu.jfr, cpu.log     async-profiler CPU recording + run stdout
#       alloc.jfr, alloc.log async-profiler allocation recording + run stdout
#       metrics.txt, metrics.log  SDK metric summary + run stdout
#
# Only the clean timing runs append to results.csv; profiler and metrics runs are separate JVM
# executions because they perturb timing. Timing reps are interleaved (rep 1 of every case, then
# rep 2, ...) so machine drift spreads across cases instead of accumulating on one client.
#
# Usage: scripts/collect.sh [options]
#   --iterations N    measured ops per run (default: 200000)
#   --warmup N        warmup ops per run (default: 20000)
#   --reps N          clean timing repetitions per case (default: 3)
#   --clients LIST    comma-separated (default: v1,v2-sync,v2-async,smithy)
#   --scenarios LIST  comma-separated (default: small-get,small-put,batch-get,batch-put)
#   --concurrency N   operations kept in flight (default: 1). Higher values give more samples per
#                     second of wall clock and a more realistic workload, but on a box where the mock
#                     server shares the client's cores they cost more precision than they buy: a null
#                     experiment resolved to +/-2.0% at concurrency 1 and +/-8.4% at 2. Re-measure with
#                     a null run before raising it on new hardware.
#   --async-mode X    inflight | join (default: inflight) for async clients
#   --pin-client CPUS taskset CPU list for the client JVM (Linux). Pinning the client and the mock
#                     server to disjoint cores is the largest available variance reduction, since the
#                     server competes for the same cores and is itself the throughput ceiling.
#   --pin-server CPUS taskset CPU list for the mock server JVM (Linux)
#   --jvm-args "..."  extra JVM args for the client
#   --server-jvm-args "..."
#                     extra JVM args for the mock server
#   --port N          mock server port (default: 19080)
#   --out DIR         output root (default: <repo>/pipeline_benchmark2/raw)
#   --jar PATH        run everything from a shaded benchmark jar instead of the local build. The
#                     jar carries its own SDK and provenance stamp, so a collection is reproducible
#                     from the archived artifact alone and does not depend on ~/.m2 state.
set -uo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$DIR/../.." && pwd)"

ITERATIONS=200000
WARMUP=20000
REPS=3
CLIENTS="v1,v2-sync,v2-async,smithy"
SCENARIOS="small-get,small-put,batch-get,batch-put"
CONCURRENCY=1
ASYNC_MODE="inflight"
PIN_CLIENT=""
PIN_SERVER=""
CLIENT_JVM_ARGS=""
SERVER_JVM_ARGS=""
PORT=19080
OUT="$REPO/pipeline_benchmark2/raw"
JAR=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --iterations)  ITERATIONS="$2"; shift 2 ;;
        --warmup)      WARMUP="$2"; shift 2 ;;
        --reps)        REPS="$2"; shift 2 ;;
        --clients)     CLIENTS="$2"; shift 2 ;;
        --scenarios)   SCENARIOS="$2"; shift 2 ;;
        --concurrency) CONCURRENCY="$2"; shift 2 ;;
        --async-mode)  ASYNC_MODE="$2"; shift 2 ;;
        --pin-client)  PIN_CLIENT="$2"; shift 2 ;;
        --pin-server)  PIN_SERVER="$2"; shift 2 ;;
        --jvm-args)        CLIENT_JVM_ARGS="$2"; shift 2 ;;
        --server-jvm-args) SERVER_JVM_ARGS="$2"; shift 2 ;;
        --port)        PORT="$2"; shift 2 ;;
        --out)         OUT="$2"; shift 2 ;;
        --jar)         JAR="$2"; shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

JAR_ARGS=()
if [[ -n "$JAR" ]]; then
    if [[ ! -f "$JAR" ]]; then
        echo "error: --jar $JAR not found" >&2
        exit 2
    fi
    JAR_ARGS=(--jar "$JAR")
fi

TUNING_ARGS=()
[[ -n "$PIN_CLIENT" ]]      && TUNING_ARGS+=(--pin-client "$PIN_CLIENT")
[[ -n "$PIN_SERVER" ]]      && TUNING_ARGS+=(--pin-server "$PIN_SERVER")
[[ -n "$CLIENT_JVM_ARGS" ]] && TUNING_ARGS+=(--jvm-args "$CLIENT_JVM_ARGS")
[[ -n "$SERVER_JVM_ARGS" ]] && TUNING_ARGS+=(--server-jvm-args "$SERVER_JVM_ARGS")

IFS=',' read -r -a CLIENT_ARR <<< "$CLIENTS"
IFS=',' read -r -a SCENARIO_ARR <<< "$SCENARIOS"

RUNID="$(date +%Y%m%d-%H%M)"
RUNDIR="$OUT/$RUNID"
MANIFEST="$RUNDIR/manifest.md"
RESULTS="$RUNDIR/results.csv"
mkdir -p "$RUNDIR"

# ---- Resolve the artifact under test up front, so no build time lands inside a run ----
if [[ -n "$JAR" ]]; then
    # The jar is self-contained and carries its own provenance; nothing to build or version-patch.
    PROVENANCE_RAW="$(unzip -p "$JAR" benchmark-provenance.properties 2>/dev/null)"
    LOCAL_SDK_VERSION="$(printf '%s\n' "$PROVENANCE_RAW" | sed -n 's/^sdk.v2.version=//p')"
    JAR_PROVENANCE="$(printf '%s\n' "$PROVENANCE_RAW" | grep -v '^[#[:space:]]*$\|^#' | tr '\n' ' ')"
    echo "Using jar: $JAR"
    echo "  provenance: $JAR_PROVENANCE"
else
    echo "Building..."
    # Detect the local SDK V2 version from the repo's root pom and patch the benchmark pom if needed,
    # so the benchmarks always run against the locally-installed SDK artifacts.
    LOCAL_SDK_VERSION="$(grep -m1 '<version>' "$REPO/pom.xml" | sed 's/.*<version>//;s/<\/version>.*//')"
    CURRENT_SDK_VERSION="$(grep -m1 '<aws.sdk.v2.version>' "$DIR/pom.xml" | sed 's/.*<aws.sdk.v2.version>//;s/<\/aws.sdk.v2.version>.*//')"
    if [[ "$LOCAL_SDK_VERSION" != "$CURRENT_SDK_VERSION" ]]; then
        echo "Updating aws.sdk.v2.version in benchmark pom: $CURRENT_SDK_VERSION -> $LOCAL_SDK_VERSION"
        sed -i.bak "s|<aws.sdk.v2.version>$CURRENT_SDK_VERSION</aws.sdk.v2.version>|<aws.sdk.v2.version>$LOCAL_SDK_VERSION</aws.sdk.v2.version>|" "$DIR/pom.xml"
        rm -f "$DIR/pom.xml.bak"
    fi
    (cd "$DIR" && mvn -q package)
    JAR_PROVENANCE="(local build, not a shaded jar)"
fi

# ---- Manifest header ----
# The scripts can run from a deployed bundle on a benchmark machine that has no clone of the repo. In
# that case there is no git state to report, and saying so is better than recording empty strings that
# look like a commit. It costs nothing either way, because with --jar the artifact's own provenance is
# the authoritative record of what is being measured.
if git -C "$REPO" rev-parse --git-dir >/dev/null 2>&1; then
    GIT_COMMIT="$(git -C "$REPO" rev-parse HEAD)"
    GIT_BRANCH="$(git -C "$REPO" rev-parse --abbrev-ref HEAD)"
    GIT_DIRTY="$(git -C "$REPO" status --porcelain | wc -l | tr -d ' ')"
    GIT_LINE="branch \`$GIT_BRANCH\`, commit \`$GIT_COMMIT\`, dirty files: $GIT_DIRTY"
else
    GIT_LINE="not a git checkout (deployed script bundle) — see artifact provenance below"
fi
if [[ "$(uname)" == "Darwin" ]]; then
    HW_CPU="$(sysctl -n machdep.cpu.brand_string 2>/dev/null || echo unknown)"
    HW_CORES="$(sysctl -n hw.ncpu 2>/dev/null || echo unknown)"
    HW_MEM="$(($(sysctl -n hw.memsize 2>/dev/null || echo 0) / 1073741824)) GiB"
else
    # /proc/cpuinfo has no "model name" on aarch64; lscpu does.
    HW_CPU="$(lscpu 2>/dev/null | sed -n 's/^Model name: *//p' | head -1)"
    if [[ -z "$HW_CPU" ]]; then
        HW_CPU="$(grep -m1 'model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2- | sed 's/^ //' || echo unknown)"
    fi
    HW_CORES="$(nproc 2>/dev/null || echo unknown)"
    HW_MEM="$(awk '/MemTotal/ {printf "%.0f GiB", $2/1048576}' /proc/meminfo 2>/dev/null || echo unknown)"
fi

TOTAL_RUNS=$(( ${#CLIENT_ARR[@]} * ${#SCENARIO_ARR[@]} * (REPS + 3) ))

cat > "$MANIFEST" <<EOF
# Benchmark collection $RUNID

## Environment

- Date: $(date -u +"%Y-%m-%dT%H:%M:%SZ") (UTC)
- Host: $(hostname), $(uname -sm)
- Hardware: $HW_CPU, $HW_CORES logical cores, $HW_MEM
- Java: $(java -version 2>&1 | head -1)
- Git: $GIT_LINE
- SDK V2 version: $LOCAL_SDK_VERSION
- Artifact under test: ${JAR:-local build (target/classes + classpath.txt)}
- Artifact provenance: $JAR_PROVENANCE
- Benchmark module: test/standalone-e2e-benchmarks

## Parameters

- iterations: $ITERATIONS
- warmup: $WARMUP
- timing repetitions per case: $REPS
- clients: $CLIENTS
- scenarios: $SCENARIOS
- concurrency: $CONCURRENCY (sync clients use this many threads; async clients keep this many in flight)
- async mode: $ASYNC_MODE
- pinning: client=[${PIN_CLIENT:-unpinned}] server=[${PIN_SERVER:-unpinned}]
- client jvm args: ${CLIENT_JVM_ARGS:-(none)}
- server jvm args: ${SERVER_JVM_ARGS:-(none)}
- cpu source: auto
- server port: $PORT (fresh out-of-process mock server per run)
- total JVM runs: $TOTAL_RUNS

## Notes

- Only clean timing runs append to results.csv. CPU-profile, alloc-profile and metrics runs are
  separate JVM executions because they perturb timing; their RESULT lines are in the per-case
  .log files, labeled by kind, and must not be compared against results.csv rows.
- Timing reps are interleaved: rep 1 of every case, then rep 2, etc., so machine drift spreads
  across cases. Phase order: timing (all reps), then cpu profiles, then alloc profiles, then
  metrics.
- Profiler recordings (async-profiler, JFR format) cover the whole JVM, including the $WARMUP
  warmup ops and one-time client/connection setup (~$(( WARMUP * 100 / (WARMUP + ITERATIONS) ))%
  of samples).
- The mock server shares the host with the client: ops_per_wall_sec includes contention effects;
  ops_per_cpu_sec / ops_per_user_cpu_sec count client-process CPU only.

## Runs

EOF

# ---- Run helpers ----
RUN_NO=0
FAILURES=0

# run_one <caseid> <kind> <logfile> <benchmark.sh args...>
run_one() {
    local caseid="$1" kind="$2" logfile="$3"
    shift 3
    local casedir="$RUNDIR/$caseid"
    mkdir -p "$casedir"
    RUN_NO=$((RUN_NO + 1))

    local cmd=(scripts/benchmark.sh --client "${caseid%%_*}" --scenario "${caseid#*_}"
               --iterations "$ITERATIONS" --warmup "$WARMUP" --progress-seconds 0
               --concurrency "$CONCURRENCY" --async-mode "$ASYNC_MODE"
               --cpu-source auto --port "$PORT" ${JAR_ARGS[@]+"${JAR_ARGS[@]}"}
               ${TUNING_ARGS[@]+"${TUNING_ARGS[@]}"} "$@")
    echo "[$RUN_NO/$TOTAL_RUNS] $caseid $kind"

    local start_ts status
    start_ts="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    if (cd "$DIR" && "${cmd[@]}") > "$casedir/$logfile" 2>&1; then
        status="ok"
    else
        status="FAILED"
        FAILURES=$((FAILURES + 1))
        echo "  FAILED — see $casedir/$logfile" >&2
    fi

    {
        echo "### $caseid — $kind"
        echo ""
        echo "- started: $start_ts"
        echo "- status: $status"
        echo "- log: \`$caseid/$logfile\`"
        for f in "$@"; do
            case "$f" in
                "$RUNDIR"/*) echo "- output: \`${f#"$RUNDIR/"}\`" ;;
            esac
        done
        echo "- command (from test/standalone-e2e-benchmarks): \`${cmd[*]}\`"
        echo ""
    } >> "$MANIFEST"
}

# ---- Phase 1: clean timing, reps interleaved ----
for rep in $(seq 1 "$REPS"); do
    for scenario in "${SCENARIO_ARR[@]}"; do
        for client in "${CLIENT_ARR[@]}"; do
            run_one "${client}_${scenario}" "timing rep $rep" "timing-rep$rep.log" \
                    --append-to-results-file "$RESULTS"
        done
    done
done

# ---- Phase 2: async-profiler CPU (JFR) ----
for scenario in "${SCENARIO_ARR[@]}"; do
    for client in "${CLIENT_ARR[@]}"; do
        caseid="${client}_${scenario}"
        run_one "$caseid" "cpu profile" "cpu.log" \
                --profile cpu --profile-format jfr --profile-file "$RUNDIR/$caseid/cpu.jfr"
    done
done

# ---- Phase 3: async-profiler alloc (JFR) ----
for scenario in "${SCENARIO_ARR[@]}"; do
    for client in "${CLIENT_ARR[@]}"; do
        caseid="${client}_${scenario}"
        run_one "$caseid" "alloc profile" "alloc.log" \
                --profile alloc --profile-format jfr --profile-file "$RUNDIR/$caseid/alloc.jfr"
    done
done

# ---- Phase 4: SDK metrics ----
for scenario in "${SCENARIO_ARR[@]}"; do
    for client in "${CLIENT_ARR[@]}"; do
        caseid="${client}_${scenario}"
        run_one "$caseid" "sdk metrics" "metrics.log" \
                --metrics --metrics-file "$RUNDIR/$caseid/metrics.txt"
    done
done

CSV_ROWS=0
if [[ -f "$RESULTS" ]]; then
    CSV_ROWS=$(($(grep -c . "$RESULTS") - 1))
fi
{
    echo "## Summary"
    echo ""
    echo "- finished: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    echo "- runs: $RUN_NO, failures: $FAILURES"
    echo "- results: \`results.csv\` ($CSV_ROWS data rows)"
} >> "$MANIFEST"

echo ""
echo "Collection complete: $RUNDIR (runs: $RUN_NO, failures: $FAILURES)"
[[ $FAILURES -eq 0 ]]
