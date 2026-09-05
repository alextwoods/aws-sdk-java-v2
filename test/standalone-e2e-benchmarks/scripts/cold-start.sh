#!/usr/bin/env bash
# Cold-start sweep: how long a fresh JVM takes to build each client and get its first answer back.
#
# The counterpart to collect.sh. Steady-state throughput is the wrong measure for a short-lived
# process, and it systematically flatters a pipeline with a larger class graph, because loading and
# compiling that graph is paid once and then amortized over the measured window. Here it is not
# amortized at all: one JVM per data point, N=--ops operations, then exit.
#
# One mock server serves the whole sweep — it is warm and stays warm, so the client under test is the
# only cold thing in the picture. Clients are interleaved within each repetition, as everywhere else
# in this harness, so machine drift spreads across clients instead of landing on one.
#
# Usage: scripts/cold-start.sh [options]
#   --clients LIST    comma-separated (default: v1,v2-sync,smithy)
#   --scenario X      single scenario (default: small-get)
#   --reps N          JVM launches per client (default: 10). Cold numbers are noisier than
#                     steady-state ones — a page-cache miss or a scheduler hiccup lands entirely in
#                     one measurement instead of being averaged over 60,000 operations — so this
#                     wants more repetitions than a timing collection, not fewer.
#   --ops N           operations per JVM (default: 5). Beyond the first they show how fast the calls
#                     get; keep it small or the JVM stops being cold.
#   --pin-client CPUS taskset CPU list for each client JVM
#   --pin-server CPUS taskset CPU list for the mock server
#   --port N          mock server port (default: 19081, so a running collection on 19080 is untouched)
#   --jar PATH        run from a shaded benchmark jar (recommended: the jar's provenance is recorded)
#   --jars LIST       label=path[,label=path...] to compare SDKs, interleaved within each repetition
#                     the way paired-ab.sh does. The first jar also runs the shared mock server, so
#                     build every jar from the same harness commit or the server changes with it.
#   --out DIR         output root (default: <repo>/pipeline_benchmark2/coldstart)
set -uo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$DIR/../.." && pwd)"

CLIENTS="v1,v2-sync,smithy"
SCENARIO="small-get"
REPS=10
OPS=5
PIN_CLIENT=""
PIN_SERVER=""
PORT=19081
JAR=""
JARS=""
OUT="$REPO/pipeline_benchmark2/coldstart"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --clients)     CLIENTS="$2"; shift 2 ;;
        --scenario)    SCENARIO="$2"; shift 2 ;;
        --reps)        REPS="$2"; shift 2 ;;
        --ops)         OPS="$2"; shift 2 ;;
        --pin-client)  PIN_CLIENT="$2"; shift 2 ;;
        --pin-server)  PIN_SERVER="$2"; shift 2 ;;
        --port)        PORT="$2"; shift 2 ;;
        --jar)         JAR="$2"; shift 2 ;;
        --jars)        JARS="$2"; shift 2 ;;
        --out)         OUT="$2"; shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

if [[ -n "$JAR" && ! -f "$JAR" ]]; then
    echo "error: --jar $JAR not found" >&2
    exit 2
fi

# One code path for both spellings: a single --jar is a one-arm sweep labelled from its own provenance
# phase, so the CSV always has a jar column and never needs a special case.
ARM_LABELS=()
ARM_PATHS=()
if [[ -n "$JARS" ]]; then
    IFS=',' read -r -a JAR_SPECS <<< "$JARS"
    for spec in "${JAR_SPECS[@]}"; do
        label="${spec%%=*}"
        path="${spec#*=}"
        if [[ "$label" == "$path" || -z "$label" || -z "$path" ]]; then
            echo "error: --jars entries must be label=path, got '$spec'" >&2
            exit 2
        fi
        if [[ ! -f "$path" ]]; then
            echo "error: jar not found: $path" >&2
            exit 2
        fi
        ARM_LABELS+=("$label")
        ARM_PATHS+=("$path")
    done
elif [[ -n "$JAR" ]]; then
    ARM_LABELS+=("$(unzip -p "$JAR" benchmark-provenance.properties 2>/dev/null | sed -n 's/^phase=//p')")
    [[ -z "${ARM_LABELS[0]}" ]] && ARM_LABELS[0]="jar"
    ARM_PATHS+=("$JAR")
else
    ARM_LABELS+=("local-build")
    ARM_PATHS+=("")
fi

IFS=',' read -r -a CLIENT_ARR <<< "$CLIENTS"
RUNID="$(date +%Y%m%d-%H%M)"
RUNDIR="$OUT/$RUNID"
mkdir -p "$RUNDIR"
CSV="$RUNDIR/coldstart.csv"
echo "jar,client,scenario,rep,client_build_ms,first_call_ms,build_plus_first_ms,jvm_to_first_response_ms,jit_ms,later_calls_us" > "$CSV"

# ---- Shared server (from the first arm's jar) ----
SERVER_JAR="${ARM_PATHS[0]}"
SERVER_CMD=(scripts/server.sh --port "$PORT")
[[ -n "$SERVER_JAR" ]] && SERVER_CMD=(scripts/server.sh --jar "$SERVER_JAR" --port "$PORT")
[[ -n "$PIN_SERVER" ]] && SERVER_CMD=(taskset -c "$PIN_SERVER" "${SERVER_CMD[@]}")
# `exec` so that $! is the JVM itself, not the subshell; see the same note in error-behavior.sh.
(cd "$DIR" && exec "${SERVER_CMD[@]}") > "$RUNDIR/server.log" 2>&1 &
SERVER_PID=$!
trap 'kill $SERVER_PID 2>/dev/null' EXIT

for _ in $(seq 1 60); do
    grep -q "^READY" "$RUNDIR/server.log" 2>/dev/null && break
    sleep 0.5
done
if ! grep -q "^READY" "$RUNDIR/server.log" 2>/dev/null; then
    echo "error: mock server did not come up — see $RUNDIR/server.log" >&2
    exit 1
fi
echo "server ready on port $PORT (pid $SERVER_PID)"

{
    echo "# Cold-start sweep $RUNID"
    echo ""
    echo "- Date: $(date -u +"%Y-%m-%dT%H:%M:%SZ") (UTC)"
    echo "- Host: $(hostname), $(uname -sm), $(nproc 2>/dev/null || echo '?') logical cores"
    echo "- Java: $(java -version 2>&1 | head -1)"
    echo "- clients: $CLIENTS, scenario: $SCENARIO, reps: $REPS, ops per JVM: $OPS"
    echo "- pinning: client=[${PIN_CLIENT:-unpinned}] server=[${PIN_SERVER:-unpinned}], port $PORT"
    echo ""
    echo "## Arms"
    echo ""
    for i in "${!ARM_LABELS[@]}"; do
        prov="$(unzip -p "${ARM_PATHS[$i]}" benchmark-provenance.properties 2>/dev/null \
                | grep -v '^#' | grep . | tr '\n' ' ')"
        echo "- \`${ARM_LABELS[$i]}\`: ${ARM_PATHS[$i]:-local build} — ${prov:-(no provenance)}"
    done
    echo ""
    echo "One JVM per (arm, client, rep), arms and clients interleaved within each repetition. The mock"
    echo "server comes from \`${ARM_LABELS[0]}\` and is shared and warm for the whole sweep."
    echo ""
} > "$RUNDIR/manifest.md"

FAILURES=0
TOTAL=$(( ${#CLIENT_ARR[@]} * ${#ARM_LABELS[@]} * REPS ))
RUN_NO=0
for rep in $(seq 1 "$REPS"); do
    for client in "${CLIENT_ARR[@]}"; do
        # Arm order reverses on even reps, so neither arm always occupies the same position within a
        # repetition — the same reason paired-ab.sh alternates.
        order=("${!ARM_LABELS[@]}")
        if (( rep % 2 == 0 )); then
            reversed=()
            for ((k=${#order[@]}-1; k>=0; k--)); do reversed+=("${order[$k]}"); done
            order=("${reversed[@]}")
        fi
        for i in "${order[@]}"; do
            arm="${ARM_LABELS[$i]}"
            RUN_NO=$((RUN_NO + 1))
            LOG="$RUNDIR/${arm}_${client}_rep${rep}.log"
            CMD=(scripts/benchmark.sh --client "$client" --scenario "$SCENARIO"
                 --cold-start "$OPS" --endpoint "http://127.0.0.1:$PORT")
            [[ -n "${ARM_PATHS[$i]}" ]] && CMD+=(--jar "${ARM_PATHS[$i]}")
            [[ -n "$PIN_CLIENT" ]] && CMD+=(--pin-client "$PIN_CLIENT")
            echo "[$RUN_NO/$TOTAL] $arm $client rep $rep"
            if ! (cd "$DIR" && "${CMD[@]}") > "$LOG" 2>&1; then
                echo "  FAILED — see $LOG" >&2
                FAILURES=$((FAILURES + 1))
                continue
            fi
            line="$(grep -m1 '^COLDSTART ' "$LOG")"
            if [[ -z "$line" ]]; then
                echo "  no COLDSTART line — see $LOG" >&2
                FAILURES=$((FAILURES + 1))
                continue
            fi
            # Pull the named fields out of the COLDSTART line rather than positionally, so adding a
            # field to the runner cannot silently shift a column.
            field() { printf '%s\n' "$line" | sed -n "s/.* $1=\([^ ]*\).*/\1/p"; }
            later="$(printf '%s\n' "$line" | grep -o 'call[0-9]*_us=[0-9]*' | cut -d= -f2 | paste -sd'/')"
            echo "$arm,$client,$SCENARIO,$rep,$(field client_build_ms),$(field first_call_ms),$(field build_plus_first_ms),$(field jvm_to_first_response_ms),$(field jit_ms),${later:-}" >> "$CSV"
        done
    done
done

{
    echo "## Summary"
    echo ""
    echo "- finished: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    echo "- runs: $RUN_NO, failures: $FAILURES"
    echo "- data: \`coldstart.csv\`"
} >> "$RUNDIR/manifest.md"

echo ""
echo "Cold-start sweep complete: $RUNDIR (runs: $RUN_NO, failures: $FAILURES)"
[[ $FAILURES -eq 0 ]]
