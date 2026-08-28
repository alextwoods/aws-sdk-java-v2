#!/usr/bin/env bash
# Sweep --concurrency for one or more clients and report where the measurement stops being about
# the client.
#
# Concurrency buys samples per unit of wall clock, but only up to a point. Three things break as it
# rises, and all three are silent unless measured:
#
#   1. The server stops being free. It shares this host's cores, so past some level a throughput
#      number is partly a measurement of Jetty. `server_cpu_ms` and the saturation flag show it.
#   2. Client and server together exceed the core count. Then everything queues on the CPU and
#      per-operation cost inflates for reasons unrelated to the SDK. The `cores` column is
#      (client CPU + server CPU) / wall time; compare it against the host's core count.
#   3. Per-operation CPU stops being flat. cpu_us_per_op is the metric concurrency is supposed to
#      leave alone. Where it starts climbing, contention is being measured instead of the pipeline.
#
# Pick the highest concurrency at which cpu_us_per_op is still flat, cores is comfortably under the
# core count, and the server never saturates.
#
# Usage: scripts/concurrency-sweep.sh [options]
#   --clients LIST      comma-separated (default: v2-sync,v2-async)
#   --scenario X        single scenario (default: small-get)
#   --levels LIST       concurrency levels (default: 1,2,4,8,16,32)
#   --iterations N      measured ops per run (default: 300000). Large on purpose: JIT compilation
#                       tails off in bursts, so a short window is disproportionately likely to contain
#                       one, and a run whose window is mostly compilation cannot answer this question.
#   --warmup N          minimum warmup ops per run (default: 20000); quiesce mode extends it
#   --jar PATH          run from a shaded benchmark jar
#   --port N            mock server port (default: 19080)
#   --out DIR           output dir (default: <repo>/pipeline_benchmark2/sweeps/<runid>)
set -uo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$DIR/../.." && pwd)"

CLIENTS="v2-sync,v2-async"
SCENARIO="small-get"
LEVELS="1,2,4,8,16,32"
ITERATIONS=300000
WARMUP=20000
JAR=""
PORT=19080
OUT=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --clients)    CLIENTS="$2"; shift 2 ;;
        --scenario)   SCENARIO="$2"; shift 2 ;;
        --levels)     LEVELS="$2"; shift 2 ;;
        --iterations) ITERATIONS="$2"; shift 2 ;;
        --warmup)     WARMUP="$2"; shift 2 ;;
        --jar)        JAR="$2"; shift 2 ;;
        --port)       PORT="$2"; shift 2 ;;
        --out)        OUT="$2"; shift 2 ;;
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

if [[ -z "$OUT" ]]; then
    OUT="$REPO/pipeline_benchmark2/sweeps/$(date +%Y%m%d-%H%M)"
fi
mkdir -p "$OUT/logs"
RESULTS="$OUT/results.csv"

IFS=',' read -r -a CLIENT_ARR <<< "$CLIENTS"
IFS=',' read -r -a LEVEL_ARR <<< "$LEVELS"

if [[ "$(uname)" == "Darwin" ]]; then
    CORES="$(sysctl -n hw.ncpu 2>/dev/null || echo 0)"
else
    CORES="$(nproc 2>/dev/null || echo 0)"
fi

echo "Sweep: clients=$CLIENTS scenario=$SCENARIO levels=$LEVELS iterations=$ITERATIONS"
echo "Host has $CORES logical cores. Output: $OUT"
echo ""

TOTAL=$(( ${#CLIENT_ARR[@]} * ${#LEVEL_ARR[@]} ))
N=0
FAILURES=0
for client in "${CLIENT_ARR[@]}"; do
    for level in "${LEVEL_ARR[@]}"; do
        N=$((N + 1))
        echo "[$N/$TOTAL] $client concurrency=$level"
        log="$OUT/logs/${client}_c${level}.log"
        if ! (cd "$DIR" && scripts/benchmark.sh --client "$client" --scenario "$SCENARIO" \
                --iterations "$ITERATIONS" --warmup "$WARMUP" --concurrency "$level" \
                --progress-seconds 0 --cpu-source auto --port "$PORT" \
                ${JAR_ARGS[@]+"${JAR_ARGS[@]}"} \
                --append-to-results-file "$RESULTS") > "$log" 2>&1; then
            echo "  FAILED — see $log" >&2
            FAILURES=$((FAILURES + 1))
        fi
    done
done

echo ""
echo "Sweep complete: $OUT (runs: $N, failures: $FAILURES)"
echo ""

SUMMARIZER="$REPO/pipeline_benchmark2/analysis/scripts/concurrency_sweep_summary.py"
if [[ -f "$SUMMARIZER" ]]; then
    python3 "$SUMMARIZER" "$RESULTS" --cores "$CORES" | tee "$OUT/summary.md"
else
    echo "(no summarizer at $SUMMARIZER; results in $RESULTS)"
fi

[[ $FAILURES -eq 0 ]]
