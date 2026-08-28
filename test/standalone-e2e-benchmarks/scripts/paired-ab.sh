#!/usr/bin/env bash
# Paired A/B timing comparison of two (or more) benchmark jars.
#
# Why pairing: sequential collections taken hours apart are confounded by whatever else the machine
# was doing in between, which on a developer workstation has been the dominant error term — one
# collection here saw a first repetition run 5.6x slow. Alternating the arms inside a single session
# makes drift hit both arms nearly equally, so the *difference* survives noise that swamps either
# arm's absolute number.
#
# Why jars: each arm is a self-contained, provenance-stamped jar, so switching arms costs nothing
# and involves no Maven reinstall. Comparing jars is only sound if the harness is identical in both;
# the summary checks that and refuses to report if it isn't.
#
# This script measures timing only. Use collect.sh for allocation/CPU profiles and SDK metrics.
#
# Usage: scripts/paired-ab.sh --jars LABEL=PATH[,LABEL=PATH...] [options]
#   --jars LIST       arms to compare, in a label=path list (required, at least 2)
#   --iterations N    measured ops per run (default: 50000)
#   --warmup N        warmup ops per run (default: 10000)
#   --reps N          repetitions of the whole pair (default: 5)
#   --clients LIST    comma-separated (default: v2-sync,v2-async)
#   --scenarios LIST  comma-separated (default: small-get,small-put,batch-get,batch-put)
#   --concurrency N   operations kept in flight (default: 1), identical in both arms
#   --async-mode X    inflight | join (default: inflight) for async clients
#   --port N          mock server port (default: 19080)
#   --out DIR         output root (default: <repo>/pipeline_benchmark2/paired)
set -uo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$DIR/../.." && pwd)"

JARS=""
ITERATIONS=50000
WARMUP=10000
REPS=5
CLIENTS="v2-sync,v2-async"
SCENARIOS="small-get,small-put,batch-get,batch-put"
CONCURRENCY=1
ASYNC_MODE="inflight"
PORT=19080
OUT="$REPO/pipeline_benchmark2/paired"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --jars)        JARS="$2"; shift 2 ;;
        --iterations)  ITERATIONS="$2"; shift 2 ;;
        --warmup)      WARMUP="$2"; shift 2 ;;
        --reps)        REPS="$2"; shift 2 ;;
        --clients)     CLIENTS="$2"; shift 2 ;;
        --scenarios)   SCENARIOS="$2"; shift 2 ;;
        --concurrency) CONCURRENCY="$2"; shift 2 ;;
        --async-mode)  ASYNC_MODE="$2"; shift 2 ;;
        --port)        PORT="$2"; shift 2 ;;
        --out)         OUT="$2"; shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

if [[ -z "$JARS" ]]; then
    echo "error: --jars LABEL=PATH[,LABEL=PATH...] is required" >&2
    exit 2
fi

IFS=',' read -r -a ARM_ARR <<< "$JARS"
if [[ ${#ARM_ARR[@]} -lt 2 ]]; then
    echo "error: --jars needs at least two arms" >&2
    exit 2
fi

ARM_LABELS=()
ARM_PATHS=()
for spec in "${ARM_ARR[@]}"; do
    if [[ "$spec" != *=* ]]; then
        echo "error: arm '$spec' is not LABEL=PATH" >&2
        exit 2
    fi
    label="${spec%%=*}"
    path="${spec#*=}"
    if [[ ! -f "$path" ]]; then
        echo "error: arm '$label' jar not found: $path" >&2
        exit 2
    fi
    ARM_LABELS+=("$label")
    ARM_PATHS+=("$(cd "$(dirname "$path")" && pwd)/$(basename "$path")")
done

IFS=',' read -r -a CLIENT_ARR <<< "$CLIENTS"
IFS=',' read -r -a SCENARIO_ARR <<< "$SCENARIOS"

RUNID="$(date +%Y%m%d-%H%M)"
RUNDIR="$OUT/$RUNID"
MANIFEST="$RUNDIR/manifest.md"
RESULTS="$RUNDIR/results.csv"
mkdir -p "$RUNDIR/logs"

NARMS=${#ARM_LABELS[@]}
TOTAL_RUNS=$(( ${#CLIENT_ARR[@]} * ${#SCENARIO_ARR[@]} * REPS * NARMS ))

if [[ "$(uname)" == "Darwin" ]]; then
    HW_CPU="$(sysctl -n machdep.cpu.brand_string 2>/dev/null || echo unknown)"
    HW_CORES="$(sysctl -n hw.ncpu 2>/dev/null || echo unknown)"
else
    # /proc/cpuinfo has no "model name" on aarch64; lscpu does.
    HW_CPU="$(lscpu 2>/dev/null | sed -n 's/^Model name: *//p' | head -1)"
    if [[ -z "$HW_CPU" ]]; then
        HW_CPU="$(grep -m1 'model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2- | sed 's/^ //' || echo unknown)"
    fi
    HW_CORES="$(nproc 2>/dev/null || echo unknown)"
fi

{
    echo "# Paired A/B timing comparison $RUNID"
    echo ""
    echo "## Arms"
    echo ""
    for i in "${!ARM_LABELS[@]}"; do
        echo "### ${ARM_LABELS[$i]}"
        echo ""
        echo "- jar: \`${ARM_PATHS[$i]}\`"
        echo "- provenance:"
        unzip -p "${ARM_PATHS[$i]}" benchmark-provenance.properties 2>/dev/null \
            | grep -v '^#' | sed 's/^/    - /'
        echo ""
    done
    echo "## Environment"
    echo ""
    echo "- Date: $(date -u +"%Y-%m-%dT%H:%M:%SZ") (UTC)"
    echo "- Host: $(hostname), $(uname -sm)"
    echo "- Hardware: $HW_CPU, $HW_CORES logical cores"
    echo "- Java: $(java -version 2>&1 | head -1)"
    echo ""
    echo "## Parameters"
    echo ""
    echo "- iterations: $ITERATIONS, warmup: $WARMUP"
    echo "- reps of the whole pair: $REPS"
    echo "- clients: $CLIENTS"
    echo "- scenarios: $SCENARIOS"
    echo "- concurrency: $CONCURRENCY, async mode: $ASYNC_MODE"
    echo "- server port: $PORT (fresh out-of-process mock server per run)"
    echo "- total JVM runs: $TOTAL_RUNS"
    echo ""
    echo "## Design"
    echo ""
    echo "Arms alternate within each repetition, and the arm order reverses on even repetitions, so"
    echo "neither arm systematically occupies the warmer or colder position. Only timing is measured;"
    echo "profiling perturbs it and belongs in a separate collect.sh run."
    echo ""
    echo "## Runs"
    echo ""
} > "$MANIFEST"

RUN_NO=0
FAILURES=0

for rep in $(seq 1 "$REPS"); do
    # Reverse the arm order on even reps so ordering bias cancels across the pair.
    if (( rep % 2 == 1 )); then
        order=($(seq 0 $((NARMS - 1))))
    else
        order=($(seq $((NARMS - 1)) -1 0))
    fi
    for scenario in "${SCENARIO_ARR[@]}"; do
        for client in "${CLIENT_ARR[@]}"; do
            for i in "${order[@]}"; do
                label="${ARM_LABELS[$i]}"
                jar="${ARM_PATHS[$i]}"
                RUN_NO=$((RUN_NO + 1))
                log="logs/${label}_${client}_${scenario}_rep${rep}.log"
                echo "[$RUN_NO/$TOTAL_RUNS] rep$rep $label $client $scenario"
                start_ts="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
                if (cd "$DIR" && scripts/benchmark.sh --jar "$jar" --client "$client" \
                        --scenario "$scenario" --iterations "$ITERATIONS" --warmup "$WARMUP" \
                        --concurrency "$CONCURRENCY" --async-mode "$ASYNC_MODE" \
                        --progress-seconds 0 --cpu-source auto --port "$PORT" \
                        --append-to-results-file "$RESULTS") > "$RUNDIR/$log" 2>&1; then
                    status="ok"
                else
                    status="FAILED"
                    FAILURES=$((FAILURES + 1))
                    echo "  FAILED — see $RUNDIR/$log" >&2
                fi
                {
                    echo "- [$RUN_NO] rep $rep, arm \`$label\`, $client/$scenario — $status," \
                         "started $start_ts, log \`$log\`"
                } >> "$MANIFEST"
            done
        done
    done
done

{
    echo ""
    echo "## Summary"
    echo ""
    echo "- finished: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    echo "- runs: $RUN_NO, failures: $FAILURES"
} >> "$MANIFEST"

echo ""
echo "Paired run complete: $RUNDIR (runs: $RUN_NO, failures: $FAILURES)"
echo ""

SUMMARIZER="$REPO/pipeline_benchmark2/analysis/scripts/paired_ab_summary.py"
if [[ -f "$SUMMARIZER" ]]; then
    python3 "$SUMMARIZER" "$RUNDIR" | tee "$RUNDIR/summary.md"
else
    echo "(no summarizer at $SUMMARIZER; results in $RESULTS)"
fi

[[ $FAILURES -eq 0 ]]
