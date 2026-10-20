#!/usr/bin/env bash
#
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#  http://aws.amazon.com/apache2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
#
# Paired A/B comparison of SDK builds on the E2E DynamoDB harness (test/standalone-e2e-benchmarks), built from git
# refs (Requirements 34.9-34.11, 37.9, 37.10; design D.3 and D.6). Ported from racecar's
# test/standalone-e2e-benchmarks/scripts/paired-ab.sh (feature/poc/racecar), which compared prebuilt jars.
#
# Each arm is a git ref: the Baseline_Build (fc8b74e9fe21), the racecar reference arm (feature/poc/racecar), the
# feature build (HEAD) or a candidate. For each distinct commit the script checks it out into its own git worktree
# and builds dynamodb, apache5-client and aws-crt-client (-P quick -am) into that worktree's own local Maven
# repository (<worktree>-m2, chained in front of the shared one, read-only), as build-size-baseline.sh does. It then
# builds THIS checkout's harness against each arm's SDK, so the harness is identical in every arm and the SDK is the
# only difference. Every build and every run uses the JDK that runs this script, and every client JVM gets the
# same JVM options.
#
# Runs are interleaved: within each repetition every arm runs each scenario back to back, and the arm order
# reverses on even repetitions. A fresh out-of-process mock server (127.0.0.1 only) is started per run.
#
# Null experiment (--null LABEL=REF): the same ref as both arms (LABEL-a and LABEL-b, the same jar), so every
# difference is noise. The summary reports it as the noise floor per scenario; pass its paired-ab.json to
# paired_ab_summary.py --null to apply that floor to a real comparison.
#
# Output (<out>/<run-id>/): arms.tsv, params.tsv, runs.tsv, runs/<n>.csv (one harness results row per run),
# logs/, paired-ab.json (the input of paired_ab_summary.py) and summary.md.
#
# Usage: scripts/schema-serde/paired-ab.sh (--arm LABEL=REF ... | --null LABEL=REF) [options]
#   --arm LABEL=REF     an arm built from git ref REF; repeat for each arm; the first arm is the baseline
#   --null LABEL=REF    null experiment: REF as both arms (LABEL=PATH: a prebuilt harness jar as both arms)
#   --jar LABEL=PATH    an arm from a prebuilt harness jar (no build); may be mixed with --arm
#   --skip-build        reuse <out>/jars/<label>.jar from an earlier build of the same arms
#   --iterations N      measured ops per run (default: 50000)
#   --warmup N          warmup ops per run (default: 10000)
#   --reps N            repetitions (default: 7, as racecar did for small ops)
#   --clients LIST      comma-separated: v2-sync,v2-async (default: v2-sync,v2-async)
#   --scenarios LIST    comma-separated (default: small-get,small-put,describe-table,batch-get,batch-put)
#   --concurrency N     operations in flight (default: 1)
#   --metrics           attach an SDK metric publisher to every run
#   --jvm-args "..."    client JVM options (default: racecar's -Xms2g -Xmx2g -XX:+AlwaysPreTouch
#                       -XX:ParallelGCThreads=4 -XX:CICompilerCount=4)
#   --server-jvm-args "..."  mock server JVM options (default: none)
#   --pin-client CPUS / --pin-server CPUS   taskset CPU lists (Linux)
#   --port N            mock server port (default: 19080)
#   --out DIR           output root (default: ~/.cache/aws-sdk-java-v2/paired-ab)
#   --online            let Maven download missing artifacts (default: offline, -o)
#
# Environment: PAIRED_AB_WORKTREE_ROOT (default: $TMPDIR or /tmp), MAVEN_LOCAL_REPO (default: ~/.m2/repository),
# JAVA_HOME (the JDK for every build and run; default: the java on PATH).
#
set -euo pipefail
ARMS=()
NULL_SPEC=""
SKIP_BUILD=0
ITERATIONS=50000
WARMUP=10000
REPS=7
CLIENTS="v2-sync,v2-async"
SCENARIOS="small-get,small-put,describe-table,batch-get,batch-put"
CONCURRENCY=1
METRICS=0
JVM_ARGS="-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4"
SERVER_JVM_ARGS=""
PIN_CLIENT=""
PIN_SERVER=""
PORT=19080
OUT_ROOT="$HOME/.cache/aws-sdk-java-v2/paired-ab"
OFFLINE="-o"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --arm) ARMS+=("ref:$2"); shift 2 ;;
        --jar) ARMS+=("jar:$2"); shift 2 ;;
        --null) NULL_SPEC="$2"; shift 2 ;;
        --skip-build) SKIP_BUILD=1; shift ;;
        --iterations) ITERATIONS="$2"; shift 2 ;;
        --warmup) WARMUP="$2"; shift 2 ;;
        --reps) REPS="$2"; shift 2 ;;
        --clients) CLIENTS="$2"; shift 2 ;;
        --scenarios) SCENARIOS="$2"; shift 2 ;;
        --concurrency) CONCURRENCY="$2"; shift 2 ;;
        --metrics) METRICS=1; shift ;;
        --jvm-args) JVM_ARGS="$2"; shift 2 ;;
        --server-jvm-args) SERVER_JVM_ARGS="$2"; shift 2 ;;
        --pin-client) PIN_CLIENT="$2"; shift 2 ;;
        --pin-server) PIN_SERVER="$2"; shift 2 ;;
        --port) PORT="$2"; shift 2 ;;
        --out) OUT_ROOT="$2"; shift 2 ;;
        --online) OFFLINE=""; shift ;;
        -h|--help) sed -n '16,62p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done
NULL_EXPERIMENT=false
if [[ -n "$NULL_SPEC" ]]; then
    if [[ ${#ARMS[@]} -gt 0 ]]; then
        echo "--null cannot be combined with --arm/--jar" >&2
        exit 2
    fi
    NULL_EXPERIMENT=true
    if [[ -f "${NULL_SPEC#*=}" ]]; then
        ARMS=("jar:$NULL_SPEC")
    else
        ARMS=("ref:$NULL_SPEC")
    fi
elif [[ ${#ARMS[@]} -lt 2 ]]; then
    echo "Need at least two arms (--arm/--jar), or --null LABEL=REF" >&2
    exit 2
fi
REPO_ROOT="$(git rev-parse --show-toplevel)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HARNESS_SRC="$REPO_ROOT/test/standalone-e2e-benchmarks"
HARNESS_COMMIT="$(git -C "$REPO_ROOT" rev-parse HEAD)"
HARNESS_DIRTY="$(git -C "$REPO_ROOT" status --porcelain -- test/standalone-e2e-benchmarks | wc -l | tr -d ' ')"
SHARED_REPO="${MAVEN_LOCAL_REPO:-$HOME/.m2/repository}"
WT_ROOT="${PAIRED_AB_WORKTREE_ROOT:-${TMPDIR:-/tmp}}"
WT_ROOT="${WT_ROOT%/}"
if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi
# Unset so every arm sees the same environment as the feature build.
unset AWS_SDK_UA_APP_ID || true
RUN_ID="$(date +%Y%m%d-%H%M%S)"
RUNDIR="$OUT_ROOT/$RUN_ID"
JARDIR="$OUT_ROOT/jars"
mkdir -p "$RUNDIR/logs" "$RUNDIR/runs" "$JARDIR"
echo "Run directory: $RUNDIR"
# ---- Build: one SDK worktree per distinct commit, one harness jar per arm ----
build_sdk() {  # <sha> -> prints "<worktree>\t<m2>\t<sdk version>"
    local sha="$1" id="${1:0:12}"
    local wt="$WT_ROOT/aws-sdk-paired-ab-$id"
    local m2="$wt-m2"
    if [[ -d "$wt" ]]; then
        local actual
        actual="$(git -C "$wt" rev-parse HEAD)"
        if [[ "$actual" != "$sha" ]]; then
            echo "$wt is at $actual, not $sha. Remove it (git worktree remove) or set PAIRED_AB_WORKTREE_ROOT." >&2
            return 1
        fi
    else
        git -C "$REPO_ROOT" worktree add --detach "$wt" "$sha" >&2
    fi
    local version
    version="$(grep -m1 -o '<version>[^<]*</version>' "$wt/pom.xml" | sed 's/<[^>]*>//g')"
    if [[ $SKIP_BUILD -eq 0 ]]; then
        local log="$RUNDIR/logs/sdk-build-$id.log"
        echo "Building the SDK at $id ($version) in $wt; log: $log" >&2
        (cd "$wt" && mvn -B $OFFLINE -Dmaven.repo.local="$m2" -Dmaven.repo.local.tail="$SHARED_REPO" \
            clean install -P quick -pl :dynamodb,:apache5-client,:aws-crt-client -am) > "$log" 2>&1 || {
            tail -n 40 "$log" >&2
            echo "SDK build failed at $sha; see $log" >&2
            return 1
        }
    fi
    printf '%s\t%s\t%s\n' "$wt" "$m2" "$version"
}
build_harness() {  # <label> <sha> <m2> <version> -> builds $JARDIR/<label>.jar
    local label="$1" sha="$2" m2="$3" version="$4"
    local copy="$RUNDIR/harness-build/$label"
    local log="$RUNDIR/logs/harness-build-$label.log"
    rm -rf "$copy" && mkdir -p "$copy"
    (cd "$HARNESS_SRC" && tar --exclude ./target -cf - .) | (cd "$copy" && tar -xf -)
    echo "Building the harness for $label against SDK $version; log: $log"
    (cd "$copy" && mvn -B $OFFLINE -Dmaven.repo.local="$m2" -Dmaven.repo.local.tail="$SHARED_REPO" clean package \
        -Daws.sdk.v2.version="$version" -Dbenchmark.phase="$label" -Dbenchmark.sdk.commit="$sha" \
        -Dbenchmark.git.commit="$HARNESS_COMMIT" -Dbenchmark.git.dirty="$HARNESS_DIRTY" \
        -Dbenchmark.git.branch=paired-ab) > "$log" 2>&1 || {
        tail -n 40 "$log" >&2
        echo "Harness build failed for $label; see $log" >&2
        return 1
    }
    cp "$copy/target/e2e-$label.jar" "$JARDIR/$label.jar"
    rm -rf "$copy"
}
LABELS=()
JARS=()
printf 'label\tref\tcommit\tsdk_version\tjar\n' > "$RUNDIR/arms.tsv"
for spec in "${ARMS[@]}"; do
    kind="${spec%%:*}"
    body="${spec#*:}"
    if [[ "$body" != *=* ]]; then
        echo "Arm '$body' is not LABEL=REF or LABEL=PATH" >&2
        exit 2
    fi
    label="${body%%=*}"
    target="${body#*=}"
    if [[ "$kind" == "jar" ]]; then
        [[ -f "$target" ]] || { echo "Jar not found: $target" >&2; exit 2; }
        jar="$(cd "$(dirname "$target")" && pwd)/$(basename "$target")"
        sha="$(unzip -p "$jar" benchmark-provenance.properties | sed -n 's/^sdk.commit=//p')"
        version="$(unzip -p "$jar" benchmark-provenance.properties | sed -n 's/^sdk.v2.version=//p')"
        ref="(prebuilt jar)"
    else
        ref="$target"
        sha="$(git -C "$REPO_ROOT" rev-parse --verify "${ref}^{commit}")"
        sdk_info="$(build_sdk "$sha")" || exit 1
        IFS=$'\t' read -r _wt m2 version <<< "$sdk_info"
        if [[ $SKIP_BUILD -eq 1 && -f "$JARDIR/$label.jar" ]]; then
            echo "Reusing $JARDIR/$label.jar (--skip-build)"
        else
            build_harness "$label" "$sha" "$m2" "$version"
        fi
        jar="$JARDIR/$label.jar"
    fi
    if [[ "$NULL_EXPERIMENT" == true ]]; then
        # One build, one jar, two arm labels: the arms are byte-identical by construction.
        for suffix in a b; do
            LABELS+=("$label-$suffix")
            JARS+=("$jar")
            printf '%s\t%s\t%s\t%s\t%s\n' "$label-$suffix" "$ref" "$sha" "$version" "$jar" >> "$RUNDIR/arms.tsv"
        done
    else
        LABELS+=("$label")
        JARS+=("$jar")
        printf '%s\t%s\t%s\t%s\t%s\n' "$label" "$ref" "$sha" "$version" "$jar" >> "$RUNDIR/arms.tsv"
    fi
done
# ---- Parameters and environment ----
if [[ "$(uname)" == "Darwin" ]]; then
    HW_CPU="$(sysctl -n machdep.cpu.brand_string 2>/dev/null || echo unknown)"
    HW_CORES="$(sysctl -n hw.ncpu 2>/dev/null || echo unknown)"
else
    HW_CPU="$(lscpu 2>/dev/null | sed -n 's/^Model name: *//p' | head -1)"
    HW_CORES="$(nproc 2>/dev/null || echo unknown)"
fi
{
    printf 'key\tvalue\n'
    printf 'run_id\t%s\n' "$RUN_ID"
    printf 'null_experiment\t%s\n' "$NULL_EXPERIMENT"
    printf 'harness_commit\t%s\n' "$HARNESS_COMMIT"
    printf 'harness_dirty_files\t%s\n' "$HARNESS_DIRTY"
    printf 'iterations\t%s\n' "$ITERATIONS"
    printf 'warmup\t%s\n' "$WARMUP"
    printf 'reps\t%s\n' "$REPS"
    printf 'clients\t%s\n' "$CLIENTS"
    printf 'scenarios\t%s\n' "$SCENARIOS"
    printf 'concurrency\t%s\n' "$CONCURRENCY"
    printf 'metrics\t%s\n' "$([[ $METRICS -eq 1 ]] && echo true || echo false)"
    printf 'jvm_args\t%s\n' "$JVM_ARGS"
    printf 'server_jvm_args\t%s\n' "$SERVER_JVM_ARGS"
    printf 'pin_client\t%s\n' "$PIN_CLIENT"
    printf 'pin_server\t%s\n' "$PIN_SERVER"
    printf 'java\t%s\n' "$(java -version 2>&1 | head -1)"
    printf 'host\t%s\n' "$(hostname), $(uname -sm)"
    printf 'hardware\t%s\n' "$HW_CPU, $HW_CORES logical cores"
    printf 'started\t%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > "$RUNDIR/params.tsv"
# ---- Interleaved runs ----
TUNING_ARGS=()
[[ -n "$PIN_CLIENT" ]] && TUNING_ARGS+=(--pin-client "$PIN_CLIENT")
[[ -n "$PIN_SERVER" ]] && TUNING_ARGS+=(--pin-server "$PIN_SERVER")
[[ -n "$JVM_ARGS" ]] && TUNING_ARGS+=(--jvm-args "$JVM_ARGS")
[[ -n "$SERVER_JVM_ARGS" ]] && TUNING_ARGS+=(--server-jvm-args "$SERVER_JVM_ARGS")
[[ $METRICS -eq 1 ]] && TUNING_ARGS+=(--metrics)
IFS=',' read -r -a CLIENT_ARR <<< "$CLIENTS"
IFS=',' read -r -a SCENARIO_ARR <<< "$SCENARIOS"
NARMS=${#LABELS[@]}
TOTAL=$(( ${#CLIENT_ARR[@]} * ${#SCENARIO_ARR[@]} * REPS * NARMS ))
printf 'run\trep\tarm\tclient\tscenario\tstatus\tstarted\n' > "$RUNDIR/runs.tsv"
RUN_NO=0
FAILURES=0
for rep in $(seq 1 "$REPS"); do
    if (( rep % 2 == 1 )); then
        order=($(seq 0 $((NARMS - 1))))
    else
        order=($(seq $((NARMS - 1)) -1 0))
    fi
    for scenario in "${SCENARIO_ARR[@]}"; do
        for client in "${CLIENT_ARR[@]}"; do
            for i in "${order[@]}"; do
                RUN_NO=$((RUN_NO + 1))
                label="${LABELS[$i]}"
                started="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
                echo "[$RUN_NO/$TOTAL] rep$rep $label $client $scenario"
                if "$HARNESS_SRC/scripts/benchmark.sh" --jar "${JARS[$i]}" --client "$client" --scenario "$scenario" \
                        --iterations "$ITERATIONS" --warmup "$WARMUP" --concurrency "$CONCURRENCY" \
                        --progress-seconds 0 --cpu-source auto --port "$PORT" \
                        ${TUNING_ARGS[@]+"${TUNING_ARGS[@]}"} \
                        --append-to-results-file "$RUNDIR/runs/$RUN_NO.csv" \
                        > "$RUNDIR/logs/run-$RUN_NO-${label}_${client}_${scenario}_rep$rep.log" 2>&1; then
                    status=ok
                else
                    status=failed
                    FAILURES=$((FAILURES + 1))
                    echo "  FAILED; see $RUNDIR/logs/run-$RUN_NO-${label}_${client}_${scenario}_rep$rep.log" >&2
                fi
                printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$RUN_NO" "$rep" "$label" "$client" "$scenario" "$status" \
                    "$started" >> "$RUNDIR/runs.tsv"
            done
        done
    done
done
echo "Runs: $RUN_NO, failures: $FAILURES"
python3 "$SCRIPT_DIR/paired_ab_summary.py" collect "$RUNDIR"
python3 "$SCRIPT_DIR/paired_ab_summary.py" summarize "$RUNDIR/paired-ab.json" | tee "$RUNDIR/summary.md"
[[ $FAILURES -eq 0 ]]
