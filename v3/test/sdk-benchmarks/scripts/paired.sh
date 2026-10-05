#!/usr/bin/env bash
# Interleaved repetitions across V3 arms (racecar's paired-ab.sh, over arms instead of jars).
#
# One JVM per (arm, scenario, repetition), each against a fresh mock server (benchmark.sh). Within a repetition the
# arms alternate per scenario; the arm order reverses on even repetitions so no arm sits in the warmer slot. Every
# RESULT is appended to one results.csv; manifest.md records host/JDK/power/background metadata, the exact commands and
# every run's status.
#
#   scripts/paired.sh [--arms a,b,...] [--scenarios s,...] [--reps N] [--iterations N] [--warmup N]
#                     [--scenario-iterations small-get=N,batch-get=N,...] [--null-arm ARM]
#                     [--v2-reference-jar PATH]
#                     [--concurrency N] [--async-mode inflight|join] [--port N] [--phase LABEL] [--out DIR]
#
#   --scenario-iterations  per-scenario measured ops, overriding --iterations for the listed scenarios
#                          (racecar's collections: 200000 small, 80000 batch).
#   --null-arm ARM         same-artifact null experiment (Req 20.8/20.24): ARM runs a second time per scenario and
#                          repetition under the label ARM#null (BenchmarkRunner --label), placed last in the arm list
#                          so it sits at the opposite end of the order from ARM on every repetition.
#   --v2-reference-jar P   opt-in reference arms from racecar's standalone runner (v2-sync = V2 sync Apache5, smithy =
#                          smithy-java), run against this module's MockDdbServer with --endpoint. P is a racecar jar
#                          built against a published V2, e.g. on a racecar worktree:
#                            test/standalone-e2e-benchmarks/scripts/build-jar.sh stock254 --sdk-version 2.54.0
#                          A shaded jar runs alone; a thin jar uses the classpath.txt next to it. (The jars in the main
#                          checkout's test/standalone-e2e-benchmarks/target are the SMITHY-BRIDGED build and fail
#                          racecar's own pipeline/version checks, so they cannot serve as the reference.)
#                          Rows go to ext_<label>_<scenario>_rep<k>.csv; summarize.py merges them and marks their
#                          different scope (full client: signing, endpoints, retries) and lack of V3 verification.
set -eo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
require_build

ARMS="v3-sync-apache5,v3-sync-apache4,v3-sync-urlconnection,v3-sync-crt,v3-async-netty,v3-async-crt,v3-async-apache5"
SCENARIOS="small-get,small-put,batch-get,batch-put"
REPS=5
ITERATIONS=200000
WARMUP=20000
CONCURRENCY=1
ASYNC_MODE=inflight
PORT=19080
PHASE=unspecified
OUT=""
SCEN_ITERS=""
NULL_ARM=""
V2_JAR=""
EXTRA=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --arms) ARMS="$2"; shift 2 ;;
        --scenarios) SCENARIOS="$2"; shift 2 ;;
        --reps) REPS="$2"; shift 2 ;;
        --iterations) ITERATIONS="$2"; shift 2 ;;
        --warmup) WARMUP="$2"; shift 2 ;;
        --concurrency) CONCURRENCY="$2"; shift 2 ;;
        --async-mode) ASYNC_MODE="$2"; shift 2 ;;
        --port) PORT="$2"; shift 2 ;;
        --phase) PHASE="$2"; shift 2 ;;
        --out) OUT="$2"; shift 2 ;;
        --scenario-iterations) SCEN_ITERS="$2"; shift 2 ;;
        --null-arm) NULL_ARM="$2"; shift 2 ;;
        --v2-reference-jar) V2_JAR="$2"; shift 2 ;;
        *) EXTRA+=("$1"); shift ;;
    esac
done
RUNID="$(date +%Y%m%d-%H%M%S)"
OUT="${OUT:-$MODULE_DIR/target/paired/$RUNID}"
mkdir -p "$OUT"
RESULTS="$OUT/results.csv"
MANIFEST="$OUT/manifest.md"
# Measured ops for a scenario: its --scenario-iterations entry, else --iterations. (bash 3.2: no associative arrays.)
iterations_for() {
    local entry
    for entry in ${SCEN_ITERS//,/ }; do
        if [[ "${entry%%=*}" == "$1" ]]; then
            echo "${entry#*=}"
            return
        fi
    done
    echo "$ITERATIONS"
}
if [[ -n "$NULL_ARM" ]]; then
    ARMS="$ARMS,$NULL_ARM#null"
fi
V2_CP=""
if [[ -n "$V2_JAR" ]]; then
    [[ -f "$V2_JAR" ]] || { echo "error: --v2-reference-jar $V2_JAR not found" >&2; exit 2; }
    V2_CP="$V2_JAR"
    if ! unzip -l "$V2_JAR" 'software/amazon/awssdk/core/SdkClient.class' >/dev/null 2>&1; then
        v2_cpfile="$(dirname "$V2_JAR")/classpath.txt"
        [[ -f "$v2_cpfile" ]] || { echo "error: $V2_JAR is not shaded and has no classpath.txt beside it" >&2; exit 2; }
        V2_CP="$V2_JAR:$(cat "$v2_cpfile")"
    fi
    V2_TAG="$(unzip -p "$V2_JAR" benchmark-provenance.properties 2>/dev/null | sed -n 's/^sdk.v2.version=//p')"
    ARMS="$ARMS,ext:v2-sync:v2ref-sync-apache5@${V2_TAG:-unknown},ext:smithy:v2ref-smithy-java"
fi
IFS=',' read -r -a ARM_ARR <<< "$ARMS"
IFS=',' read -r -a SCEN_ARR <<< "$SCENARIOS"
NARMS=${#ARM_ARR[@]}

{
    echo "# V3 paired run $RUNID"
    echo
    echo "Scope of every arm: serde+transport (awsJson1.0 OperationProtocol marshal -> transport exchange -> unmarshal;"
    echo "no client engine, signer, endpoint resolution, retries or interceptors exist in V3 yet)."
    echo
    echo "## Configuration"
    echo "- arms: $ARMS"
    echo "- scenarios: $SCENARIOS"
    echo "- repetitions: $REPS (arm order reversed on even repetitions)"
    echo "- iterations: $ITERATIONS warmup: $WARMUP concurrency: $CONCURRENCY async-mode: $ASYNC_MODE"
    echo "- scenario iterations: $(for s in "${SCEN_ARR[@]}"; do printf '%s=%s ' "$s" "$(iterations_for "$s")"; done)"
    echo "- null arm: ${NULL_ARM:-(none)}${NULL_ARM:+ (second copy labelled $NULL_ARM#null)}"
    if [[ -n "$V2_JAR" ]]; then
        echo "- v2 reference: racecar runner $V2_JAR ($(shasum -a 256 "$V2_JAR" | cut -c1-12)), V2 version" \
             "${V2_TAG:-unknown}; arms v2-sync (Apache5) and smithy; scope = full client" \
             "(signing, endpoint resolution, retries) vs V3 serde+transport; rows NOT response-verified by V3 harness"
        echo "- v2 reference classpath sha: $(echo "$V2_CP" | tr ':' '\n' | xargs shasum -a 256 | sha_of_stdin | cut -c1-12)"
    fi
    echo "- extra runner args: ${EXTRA[*]:-(none; warmup-mode quiesce)}"
    echo "- server: fresh out-of-process MockDdbServer per run on 127.0.0.1:$PORT (plain HTTP, loopback)"
    echo "- phase: $PHASE"
    echo "- provenance: $(provenance_flags "$PHASE" | tr ' ' '\n' | sed 's/^-Dbench\.//' | tr '\n' ' ')"
    echo
    echo "## Host (before)"
    host_metadata
    echo
    echo "## Runs"
} > "$MANIFEST"

for rep in $(seq 1 "$REPS"); do
    if (( rep % 2 == 1 )); then
        order=($(seq 0 $((NARMS - 1))))
    else
        order=($(seq $((NARMS - 1)) -1 0))
    fi
    for scenario in "${SCEN_ARR[@]}"; do
        pos=0
        for i in "${order[@]}"; do
            label="${ARM_ARR[$i]}"
            pos=$((pos + 1))
            iters="$(iterations_for "$scenario")"
            started="$(date +%H:%M:%S)"
            if [[ "$label" == ext:* ]]; then
                # ext:<racecar client>:<label>. Racecar's runner against a fresh V3 MockDdbServer (same bytes).
                ext_client="$(echo "$label" | cut -d: -f2)"
                label="$(echo "$label" | cut -d: -f3)"
                log="$OUT/${label}_${scenario}_rep${rep}.log"
                cmd=(scripts/v2ref-run.sh "$PORT" "$V2_CP" --client "$ext_client" --scenario "$scenario"
                     --iterations "$iters" --warmup "$WARMUP" --concurrency "$CONCURRENCY" --async-mode "$ASYNC_MODE"
                     --progress-seconds 0 --append-to-results-file "$OUT/ext_${label}_${scenario}_rep${rep}.csv"
                     "${EXTRA[@]}")
                shown="scripts/v2ref-run.sh $PORT <v2-classpath> ${cmd[*]:3}"
            else
                arm="${label%#null}"
                log="$OUT/${label}_${scenario}_rep${rep}.log"
                cmd=(scripts/benchmark.sh --port "$PORT" --phase "$PHASE" --arm "$arm" --label "$label"
                     --scenario "$scenario" --iterations "$iters" --warmup "$WARMUP" --concurrency "$CONCURRENCY"
                     --async-mode "$ASYNC_MODE" --progress-seconds 0 --append-to-results-file "$RESULTS"
                     --repetition "$rep" --position "$pos" "${EXTRA[@]}")
                shown="${cmd[*]}"
            fi
            if (cd "$MODULE_DIR" && "${cmd[@]}") > "$log" 2>&1; then
                status=ok
            else
                status="FAILED(exit $?)"
            fi
            echo "- rep=$rep pos=$pos $label $scenario $started-$(date +%H:%M:%S) $status: \`$shown\`" >> "$MANIFEST"
            grep '^RESULT' "$log" || echo "no RESULT for $label $scenario rep $rep ($status); see $log"
        done
    done
done

{
    echo
    echo "## Host (after)"
    host_metadata
} >> "$MANIFEST"
echo "results: $RESULTS"
echo "manifest: $MANIFEST"
