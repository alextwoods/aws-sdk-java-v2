#!/usr/bin/env bash
# Error and retry behavior sweep: what each SDK does when the service fails.
#
# The companion to collect.sh and cold-start.sh, and the answer to the one thing they cannot measure.
# The component sweep found retry bridging and error bridging to cost nothing, which is correct and
# uninformative: neither component does anything on a call that succeeds. This runs ErrorBehaviorProbe,
# which arms a fault on the mock server, makes one call, and records what the SDK threw and how many
# attempts it took — so the comparison is behavioral rather than a timing.
#
# Unlike the timing scripts, a difference here is a bug or a ledger entry, not a percentage. By default
# there is no averaging and no warmup: each row is one deterministic outcome, and reps exist only to catch
# nondeterminism (a retry that sometimes happens and sometimes does not).
#
# --backoff and --warmup switch it into a second, narrower job: measuring retry *timing*. Both exist
# because v2's default backoff has full jitter, which makes a single wall_ms sample worthless for
# comparison — the arms differ by 4x on identical policy. Removing the jitter and warming the connection
# turns wall_ms into a number, at the cost of no longer measuring the default configuration.
#
# One mock server serves the whole sweep, from the first jar. That is safe in a way it would not be for
# a timing run: the fault catalogue is a shared class (Faults), so both jars agree on what each mode
# means, and the server is not being measured.
#
# Usage: scripts/error-behavior.sh [options]
#   --jars LIST     label=path[,label=path...] — the arms to compare (required unless --jar)
#   --jar PATH      single jar, labelled from its own provenance phase
#   --faults LIST   comma-separated fault modes, or "all" (default: all)
#   --reps N        repetitions per arm (default: 2 — enough to expose nondeterminism, and these
#                   cases are slow: a persistent retryable fault spends seconds in backoff)
#   --backoff MODE  standard | immediate | fixed:MS (default: standard). Anything but `standard`
#                   removes v2's full jitter, which is what makes wall_ms readable as a number instead
#                   of a signal: `fixed:MS` checks the configured backoff survives bridging, and
#                   `immediate` measures the cost of retrying with no sleep in it.
#   --warmup        one successful call per case before the fault is armed, so connection setup and
#                   first-call class loading (~200 ms) are outside wall_ms. Use with --backoff immediate.
#   --pin-client CPUS   taskset CPU list for the probe JVM
#   --pin-server CPUS   taskset CPU list for the mock server
#   --port N        mock server port (default: 19082, clear of collect.sh and cold-start.sh)
#   --out DIR       output root (default: <repo>/pipeline_benchmark2/errors)
set -uo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$DIR/../.." && pwd)"

FAULTS="all"
REPS=2
BACKOFF="standard"
WARMUP=""
PIN_CLIENT=""
PIN_SERVER=""
PORT=19082
JAR=""
JARS=""
OUT="$REPO/pipeline_benchmark2/errors"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --jars)        JARS="$2"; shift 2 ;;
        --jar)         JAR="$2"; shift 2 ;;
        --faults)      FAULTS="$2"; shift 2 ;;
        --reps)        REPS="$2"; shift 2 ;;
        --backoff)     BACKOFF="$2"; shift 2 ;;
        --warmup)      WARMUP="--warmup"; shift ;;
        --pin-client)  PIN_CLIENT="$2"; shift 2 ;;
        --pin-server)  PIN_SERVER="$2"; shift 2 ;;
        --port)        PORT="$2"; shift 2 ;;
        --out)         OUT="$2"; shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

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
        [[ -f "$path" ]] || { echo "error: jar not found: $path" >&2; exit 2; }
        ARM_LABELS+=("$label")
        ARM_PATHS+=("$path")
    done
elif [[ -n "$JAR" ]]; then
    [[ -f "$JAR" ]] || { echo "error: --jar $JAR not found" >&2; exit 2; }
    ARM_LABELS+=("$(unzip -p "$JAR" benchmark-provenance.properties 2>/dev/null | sed -n 's/^phase=//p')")
    [[ -z "${ARM_LABELS[0]}" ]] && ARM_LABELS[0]="jar"
    ARM_PATHS+=("$JAR")
else
    echo "error: one of --jar or --jars is required" >&2
    exit 2
fi

RUNID="$(date +%Y%m%d-%H%M)"
RUNDIR="$OUT/$RUNID"
mkdir -p "$RUNDIR"
CSV="$RUNDIR/errors.csv"
FIELDS=(rep fault case armed backoff attempts faults_served wall_ms wall_us outcome exception cause errorCode
        statusCode requestId extendedRequestId retryable throttling clockSkew serviceName
        rawResponse message)
printf 'jar,client' > "$CSV"
for f in "${FIELDS[@]}"; do printf ',%s' "$f" >> "$CSV"; done
printf '\n' >> "$CSV"

# ---- Shared server (from the first arm's jar) ----
SERVER_CMD=(scripts/server.sh --jar "${ARM_PATHS[0]}" --port "$PORT")
[[ -n "$PIN_SERVER" ]] && SERVER_CMD=(taskset -c "$PIN_SERVER" "${SERVER_CMD[@]}")
# `exec` so that $! is the JVM itself: without it the subshell is the child, the trap kills only
# the subshell, and the orphaned server keeps holding the port -- which then makes the *next* run
# fail to bind, having silently measured against the previous jar's server.
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
    echo "# Error behavior sweep $RUNID"
    echo ""
    echo "- Date: $(date -u +"%Y-%m-%dT%H:%M:%SZ") (UTC)"
    echo "- Host: $(hostname), $(uname -sm), $(nproc 2>/dev/null || echo '?') logical cores"
    echo "- Java: $(java -version 2>&1 | head -1)"
    echo "- faults: $FAULTS, reps: $REPS, backoff: $BACKOFF, warmup: ${WARMUP:-no}"
    echo "- pinning: client=[${PIN_CLIENT:-unpinned}] server=[${PIN_SERVER:-unpinned}], port $PORT"
    echo ""
    echo "## Arms"
    echo ""
    for i in "${!ARM_LABELS[@]}"; do
        prov="$(unzip -p "${ARM_PATHS[$i]}" benchmark-provenance.properties 2>/dev/null \
                | grep -v '^#' | grep . | tr '\n' ' ')"
        echo "- \`${ARM_LABELS[$i]}\`: ${ARM_PATHS[$i]} — ${prov:-(no provenance)}"
    done
    echo ""
    echo "The mock server comes from \`${ARM_LABELS[0]}\` and is shared by every arm; the fault catalogue"
    echo "is a shared class, so the arms cannot disagree about what a mode means."
    echo ""
} > "$RUNDIR/manifest.md"

FAILURES=0
ROWS=0
for i in "${!ARM_LABELS[@]}"; do
    arm="${ARM_LABELS[$i]}"
    LOG="$RUNDIR/${arm}.log"
    echo "==> $arm"
    CMD=(java --enable-native-access=ALL-UNNAMED -cp "${ARM_PATHS[$i]}"
         software.amazon.awssdk.benchmark.e2e.ErrorBehaviorProbe
         --client v2-sync --endpoint "http://127.0.0.1:$PORT" --faults "$FAULTS" --reps "$REPS"
         --backoff "$BACKOFF" ${WARMUP:+$WARMUP})
    [[ -n "$PIN_CLIENT" ]] && CMD=(taskset -c "$PIN_CLIENT" "${CMD[@]}")
    if ! (cd "$DIR" && "${CMD[@]}") > "$LOG" 2>&1; then
        echo "  FAILED — see $LOG" >&2
        FAILURES=$((FAILURES + 1))
        continue
    fi
    # Named-field extraction, so adding a field to the probe cannot silently shift a column.
    while IFS= read -r line; do
        row="$arm,v2-sync"
        for f in "${FIELDS[@]}"; do
            value="$(printf '%s\n' "$line" | sed -n "s/.* $f=\([^ ]*\).*/\1/p")"
            row+=",${value:--}"
        done
        echo "$row" >> "$CSV"
        ROWS=$((ROWS + 1))
    done < <(grep '^PROBE ' "$LOG")
done

{
    echo "## Summary"
    echo ""
    echo "- finished: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    echo "- arms: ${#ARM_LABELS[@]}, rows: $ROWS, failures: $FAILURES"
    echo "- data: \`errors.csv\`"
} >> "$RUNDIR/manifest.md"

echo ""
echo "Error behavior sweep complete: $RUNDIR (rows: $ROWS, failures: $FAILURES)"
echo ""

# Write diff.md rather than summary.md: summary.md is hand-written narrative for these runs, and the
# generated diff is what it cites. Keeping them in separate files is what stops the two from drifting —
# they did drift, when this step was run by hand and then stopped being run at all.
DIFFER="$REPO/pipeline_benchmark2/analysis/scripts/error_behavior_diff.py"
if [[ -f "$DIFFER" ]]; then
    python3 "$DIFFER" "$RUNDIR" > "$RUNDIR/diff.md"
    echo "Diff written to $RUNDIR/diff.md"
    grep -m1 'behavioral difference' "$RUNDIR/diff.md" || true
else
    echo "(no differ at $DIFFER; raw rows in $CSV)"
fi

[[ $FAILURES -eq 0 ]]
