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
# One racecar optimization experiment (task 9.19; Requirements 37.1, 37.9; design D.6). See
# docs/design/schema-serde/optimizations/README.md.
#
#   1. Null experiment on the feature build: paired-ab.sh --null feature=<feature-ref> (the e2e noise floor).
#   2. Paired A/B with the same settings: paired-ab.sh --jar feature=<step 1's harness jar>
#      --arm candidate=<candidate-ref> (the feature arm is the build from step 1, not rebuilt).
#   3. If the id has JMH benchmarks (docs/design/schema-serde/optimizations/targets.json): benchmarks.jar built per
#      ref in paired-ab.sh's worktree for that commit (same worktree and local repository naming), then run as
#      feature, candidate, feature again. The two feature runs give the JMH floor.
#   4. optimization_experiment.py verdict: KEEP or REVERT, the record docs/design/schema-serde/optimizations/<id>.md
#      and the YAML change to apply by hand (this script never edits racecar-decisions.yaml).
#
# When the candidate resolves to the feature commit (a self-comparison), its jars are the feature jars.
#
# Jar mode: with both --feature-jar and --candidate-jar the script never calls git (a benchmark host needs no
# checkout). The feature and candidate commits are the jars' sdk.commit, and the refs are labels only. A JMH id then
# also needs --feature-jmh-jar and --candidate-jmh-jar (or --skip-jmh). A JMH jar with a <jar>.commit file next to
# it (commit=<sha>, written by --build-only) must match its arm's commit.
#
# Usage: scripts/schema-serde/optimization_experiment.sh <id> <candidate-ref> [options]
#   <id> is a racecar id of the decision YAML, or SJ-<sha> for a smithy-java item ("source": "smithy-java" in
#   targets.json)
#   --feature-ref REF        the current feature build (default: HEAD)
#   --feature-jar PATH       prebuilt e2e harness jar for the feature arm (no build)
#   --candidate-jar PATH     prebuilt e2e harness jar for the candidate arm (no build)
#   --feature-jmh-jar PATH   prebuilt benchmarks.jar for the feature arm
#   --candidate-jmh-jar PATH prebuilt benchmarks.jar for the candidate arm
#   --build-only             build the e2e jars (<out>/jars/{feature,candidate}.jar, through paired-ab.sh
#                            --build-only) and the JMH jars (<out>/jmh/{feature,candidate}.jar with a .commit file),
#                            then exit 0; prebuilt jars passed with the options above are not rebuilt
#   --null-run DIR           reuse the null experiment in paired-ab run directory DIR instead of running stage 1.
#                            Needs --feature-jar with the same sha256 as the jar in DIR/arms.tsv, and the same
#                            iterations, warmup, reps, clients, scenarios, concurrency, metrics, JVM options and
#                            pinning as this run (exit 2 otherwise)
#   --jmh-first              run stage 3 first; if the JMH results alone give REVERT, skip the e2e stages and write
#                            the record with optimization_experiment.py verdict --no-e2e
#   --jmh-pin CPUS           run the JMH jar under taskset -c CPUS (its forks inherit the affinity)
#   --jmh-jvm-args "..."     JVM options for the JMH forks (-jvmArgsAppend)
#   --skip-build             passed to paired-ab.sh; also reuses a benchmarks.jar already in the worktree
#   --skip-jmh               do not run the JMH benchmarks
#   --jmh-include REGEX      JMH benchmark filter (default: the id's include in targets.json)
#   --jmh-param K=V          JMH -p value; repeatable; replaces the id's params in targets.json
#   --jmh-opts "..."         extra JMH options, e.g. "-wi 1 -i 1 -f 1 -w 1s -r 1s" for a smoke run
#   --reps, --iterations, --warmup, --clients, --scenarios, --concurrency, --jvm-args, --server-jvm-args,
#   --pin-client, --pin-server, --port, --online
#                            passed unchanged to both paired-ab.sh runs
#   --out DIR                experiment directory (default: ~/.cache/aws-sdk-java-v2/optimization-experiments/<id>-<time>)
#   --record PATH            record to write (default: docs/design/schema-serde/optimizations/<id>.md)
#   --preliminary            mark the record as smoke data, not measurements
#
# Exit status: 0 KEEP (or --build-only done), 3 REVERT, anything else an error.
#
set -euo pipefail
if [[ $# -ge 1 && ( "$1" == "-h" || "$1" == "--help" ) ]]; then
    sed -n '16,/^set -euo pipefail/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'
    exit 0
fi
if [[ $# -lt 2 ]]; then
    echo "Usage: $0 <racecar-id> <candidate-ref> [options] (--help)" >&2
    exit 2
fi
RACECAR_ID="$1"
CANDIDATE_REF="$2"
shift 2
FEATURE_REF=HEAD
FEATURE_JAR=""
CANDIDATE_JAR=""
FEATURE_JMH_JAR=""
CANDIDATE_JMH_JAR=""
SKIP_BUILD=0
SKIP_JMH=0
JMH_INCLUDE=""
JMH_PARAMS=()
JMH_OPTS=""
PASS=()
OUT=""
RECORD=""
PRELIMINARY=0
OFFLINE="-o"
BUILD_ONLY=0
NULL_RUN=""
JMH_FIRST=0
JMH_PIN=""
JMH_JVM_ARGS=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --feature-ref) FEATURE_REF="$2"; shift 2 ;;
        --feature-jar) FEATURE_JAR="$2"; shift 2 ;;
        --candidate-jar) CANDIDATE_JAR="$2"; shift 2 ;;
        --feature-jmh-jar) FEATURE_JMH_JAR="$2"; shift 2 ;;
        --candidate-jmh-jar) CANDIDATE_JMH_JAR="$2"; shift 2 ;;
        --skip-build) SKIP_BUILD=1; PASS+=(--skip-build); shift ;;
        --skip-jmh) SKIP_JMH=1; shift ;;
        --jmh-include) JMH_INCLUDE="$2"; shift 2 ;;
        --jmh-param) JMH_PARAMS+=("$2"); shift 2 ;;
        --jmh-opts) JMH_OPTS="$2"; shift 2 ;;
        --reps|--iterations|--warmup|--clients|--scenarios|--concurrency|--jvm-args|--server-jvm-args|\
        --pin-client|--pin-server|--port) PASS+=("$1" "$2"); shift 2 ;;
        --online) OFFLINE=""; PASS+=(--online); shift ;;
        --out) OUT="$2"; shift 2 ;;
        --record) RECORD="$2"; shift 2 ;;
        --preliminary) PRELIMINARY=1; shift ;;
        --build-only) BUILD_ONLY=1; shift ;;
        --null-run) NULL_RUN="$2"; shift 2 ;;
        --jmh-first) JMH_FIRST=1; shift ;;
        --jmh-pin) JMH_PIN="$2"; shift 2 ;;
        --jmh-jvm-args) JMH_JVM_ARGS="$2"; shift 2 ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done
for jar in "$FEATURE_JAR" "$CANDIDATE_JAR" "$FEATURE_JMH_JAR" "$CANDIDATE_JMH_JAR"; do
    [[ -z "$jar" || -f "$jar" ]] || { echo "Jar not found: $jar" >&2; exit 2; }
done
if [[ -n "$NULL_RUN" && -z "$FEATURE_JAR" ]]; then
    echo "--null-run needs --feature-jar (the jar the null experiment ran)" >&2
    exit 2
fi
if [[ $BUILD_ONLY -eq 1 && ( -n "$NULL_RUN" || $JMH_FIRST -eq 1 ) ]]; then
    echo "--build-only runs nothing: it cannot be combined with --null-run or --jmh-first" >&2
    exit 2
fi
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PY="$SCRIPT_DIR/optimization_experiment.py"
JAR_MODE=0
if [[ -n "$FEATURE_JAR" && -n "$CANDIDATE_JAR" ]]; then
    JAR_MODE=1
    REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
else
    REPO_ROOT="$(git rev-parse --show-toplevel)"
fi
SHARED_REPO="${MAVEN_LOCAL_REPO:-$HOME/.m2/repository}"
WT_ROOT="${PAIRED_AB_WORKTREE_ROOT:-${TMPDIR:-/tmp}}"
WT_ROOT="${WT_ROOT%/}"
if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# ---- Validate the id and read its plan ----
PLAN="$(python3 "$PY" plan "$RACECAR_ID")" || exit 2
JMH_MODULE=""
PLAN_INCLUDE=""
PLAN_PARAMS=()
while IFS=$'\t' read -r key value; do
    case "$key" in
        decision) DECISION="$value" ;;
        jmh_module) JMH_MODULE="$value" ;;
        jmh_include) PLAN_INCLUDE="$value" ;;
        jmh_param) PLAN_PARAMS+=("$value") ;;
    esac
done <<< "$PLAN"
if [[ "$DECISION" != "Deferred (later individual optimization)" && "$DECISION" != none* ]]; then
    echo "Note: $RACECAR_ID is currently '$DECISION', not Deferred." >&2
fi
jar_sdk_commit() { unzip -p "$1" benchmark-provenance.properties | sed -n 's/^sdk.commit=//p' | head -1; }
if [[ $JAR_MODE -eq 1 ]]; then
    FEATURE_SHA="$(jar_sdk_commit "$FEATURE_JAR")"
    CANDIDATE_SHA="$(jar_sdk_commit "$CANDIDATE_JAR")"
    [[ "$FEATURE_SHA" =~ ^[0-9a-f]{40}$ && "$CANDIDATE_SHA" =~ ^[0-9a-f]{40}$ ]] || {
        echo "The e2e jars' provenance has no sdk.commit (feature '$FEATURE_SHA', candidate '$CANDIDATE_SHA')" >&2
        exit 2
    }
    echo "Jar mode: feature ${FEATURE_SHA:0:12} ($FEATURE_REF), candidate ${CANDIDATE_SHA:0:12} ($CANDIDATE_REF)"
else
    FEATURE_SHA="$(git -C "$REPO_ROOT" rev-parse --verify "${FEATURE_REF}^{commit}")"
    CANDIDATE_SHA="$(git -C "$REPO_ROOT" rev-parse --verify "${CANDIDATE_REF}^{commit}")"
fi
SELF=0
if [[ "$FEATURE_SHA" == "$CANDIDATE_SHA" ]]; then
    SELF=1
    echo "Candidate $CANDIDATE_REF is the feature commit ${FEATURE_SHA:0:12}: a self-comparison; reusing the feature jars."
fi
OUT="${OUT:-$HOME/.cache/aws-sdk-java-v2/optimization-experiments/$RACECAR_ID-$(date +%Y%m%d-%H%M%S)}"
RECORD="${RECORD:-$REPO_ROOT/docs/design/schema-serde/optimizations/$RACECAR_ID.md}"
mkdir -p "$OUT/jmh"
echo "Experiment directory: $OUT"

# Runs paired-ab.sh with the shared settings and prints the run directory it reports.
paired_ab() {
    local log="$1"
    shift
    local status=0
    "$SCRIPT_DIR/paired-ab.sh" --out "$OUT" ${PASS[@]+"${PASS[@]}"} "$@" 2>&1 | tee "$log" >&2 || status=$?
    local rundir
    rundir="$(sed -n 's/^Run directory: //p' "$log" | head -1)"
    if [[ -z "$rundir" || ! -f "$rundir/paired-ab.json" ]]; then
        echo "paired-ab.sh did not produce paired-ab.json (status $status); see $log" >&2
        return 1
    fi
    [[ $status -eq 0 ]] || echo "paired-ab.sh reported failed runs (status $status); the verdict will say so" >&2
    printf '%s\n' "$rundir"
}

build_jmh() {  # <label> <sha> -> copies benchmarks.jar to $OUT/jmh/<label>.jar
    local label="$1" sha="$2" id="${2:0:12}"
    local wt="$WT_ROOT/aws-sdk-paired-ab-$id"
    local m2="$wt-m2"
    local log="$OUT/jmh/build-$label.log"
    if [[ -d "$wt" ]]; then
        [[ "$(git -C "$wt" rev-parse HEAD)" == "$sha" ]] || {
            echo "$wt is not at $sha. Remove it (git worktree remove) or set PAIRED_AB_WORKTREE_ROOT." >&2
            return 1
        }
    else
        git -C "$REPO_ROOT" worktree add --detach "$wt" "$sha" >&2
    fi
    local jar="$wt/test/$JMH_MODULE/target/benchmarks.jar"
    if [[ $SKIP_BUILD -eq 1 && -f "$jar" ]]; then
        echo "Reusing $jar (--skip-build)"
    else
        echo "Building $JMH_MODULE at $id in $wt; log: $log"
        (cd "$wt" && mvn -B $OFFLINE -Dmaven.repo.local="$m2" -Dmaven.repo.local.tail="$SHARED_REPO" \
            install -P quick -pl ":$JMH_MODULE" -am) > "$log" 2>&1 || {
            tail -n 40 "$log" >&2
            echo "JMH build failed at $sha; see $log" >&2
            return 1
        }
    fi
    cp "$jar" "$OUT/jmh/$label.jar"
    printf 'commit=%s\nmodule=%s\n' "$sha" "$JMH_MODULE" > "$OUT/jmh/$label.jar.commit"
}

# Copies a prebuilt JMH jar (and its .commit file) to $OUT/jmh/<label>.jar after checking the commit it records.
use_jmh_jar() {  # <label> <path> <expected sha>
    local label="$1" jar="$2" sha="$3" recorded=""
    if [[ -f "$jar.commit" ]]; then
        recorded="$(sed -n 's/^commit=//p' "$jar.commit" | head -1)"
        if [[ "$recorded" != "$sha" ]]; then
            echo "$jar.commit records commit $recorded, but the $label arm is $sha" >&2
            exit 2
        fi
    fi
    if [[ "$(cd "$(dirname "$jar")" && pwd)/$(basename "$jar")" != "$OUT/jmh/$label.jar" ]]; then
        cp "$jar" "$OUT/jmh/$label.jar"
        [[ -f "$jar.commit" ]] && cp "$jar.commit" "$OUT/jmh/$label.jar.commit"
    fi
    return 0
}

RUN_JMH=0
[[ -n "$JMH_MODULE" && $SKIP_JMH -eq 0 ]] && RUN_JMH=1
if [[ $JAR_MODE -eq 1 && $RUN_JMH -eq 1 && -z "$FEATURE_JMH_JAR" ]]; then
    echo "$RACECAR_ID has JMH benchmarks ($JMH_MODULE): jar mode needs --feature-jmh-jar and --candidate-jmh-jar" \
         "(or --skip-jmh), since it cannot build them without git" >&2
    exit 2
fi
if [[ $JAR_MODE -eq 1 && $RUN_JMH -eq 1 && -z "$CANDIDATE_JMH_JAR" && $SELF -eq 0 ]]; then
    echo "Jar mode needs --candidate-jmh-jar for a candidate that is not the feature commit (or --skip-jmh)" >&2
    exit 2
fi

# The JMH jars for both arms, built or copied into $OUT/jmh.
prepare_jmh() {
    if [[ -n "$FEATURE_JMH_JAR" ]]; then use_jmh_jar feature "$FEATURE_JMH_JAR" "$FEATURE_SHA"; else build_jmh feature "$FEATURE_SHA"; fi
    if [[ -n "$CANDIDATE_JMH_JAR" ]]; then
        use_jmh_jar candidate "$CANDIDATE_JMH_JAR" "$CANDIDATE_SHA"
    elif [[ $SELF -eq 1 ]]; then
        cp "$OUT/jmh/feature.jar" "$OUT/jmh/candidate.jar"
        [[ -f "$OUT/jmh/feature.jar.commit" ]] && cp "$OUT/jmh/feature.jar.commit" "$OUT/jmh/candidate.jar.commit"
    else
        build_jmh candidate "$CANDIDATE_SHA"
    fi
    return 0
}

# ---- --build-only: the jars of both arms, no runs ----
if [[ $BUILD_ONLY -eq 1 ]]; then
    echo "== Build only"
    BUILD_ARMS=()
    if [[ -n "$FEATURE_JAR" ]]; then BUILD_ARMS+=(--jar "feature=$FEATURE_JAR"); else BUILD_ARMS+=(--arm "feature=$FEATURE_REF"); fi
    if [[ -n "$CANDIDATE_JAR" ]]; then
        BUILD_ARMS+=(--jar "candidate=$CANDIDATE_JAR")
    elif [[ $SELF -eq 1 && -n "$FEATURE_JAR" ]]; then
        BUILD_ARMS+=(--jar "candidate=$FEATURE_JAR")
    else
        BUILD_ARMS+=(--arm "candidate=$CANDIDATE_REF")
    fi
    if [[ "${BUILD_ARMS[*]}" == *"--arm "* ]]; then
        status=0
        "$SCRIPT_DIR/paired-ab.sh" --out "$OUT" ${PASS[@]+"${PASS[@]}"} --build-only "${BUILD_ARMS[@]}" 2>&1 \
            | tee "$OUT/build.log" || status=$?
        [[ $status -eq 0 ]] || { echo "paired-ab.sh --build-only failed; see $OUT/build.log" >&2; exit 1; }
    fi
    [[ $RUN_JMH -eq 1 ]] && prepare_jmh
    echo "Build only done. e2e jars: $OUT/jars/ (unless prebuilt); JMH jars: $OUT/jmh/"
    exit 0
fi

# ---- 3. JMH (run first with --jmh-first) ----
JMH_ARGS=()
run_jmh() {
    echo "== 3. JMH ($JMH_MODULE)"
    prepare_jmh
    local params=(${PLAN_PARAMS[@]+"${PLAN_PARAMS[@]}"})
    [[ ${#JMH_PARAMS[@]} -gt 0 ]] && params=("${JMH_PARAMS[@]}")
    local bench_args=("${JMH_INCLUDE:-$PLAN_INCLUDE}" -prof gc -rf json)
    local p
    for p in ${params[@]+"${params[@]}"}; do bench_args+=(-p "$p"); done
    [[ -n "$JMH_JVM_ARGS" ]] && bench_args+=(-jvmArgsAppend "$JMH_JVM_ARGS")
    # shellcheck disable=SC2206 # --jmh-opts is word-split on purpose
    [[ -n "$JMH_OPTS" ]] && bench_args+=($JMH_OPTS)
    local prefix=()
    [[ -n "$JMH_PIN" ]] && prefix=(taskset -c "$JMH_PIN")
    # feature, candidate, feature again: the two feature runs bracket the candidate and give the floor.
    local run name jar
    for run in feature-a:feature candidate:candidate feature-b:feature; do
        name="${run%%:*}"
        jar="$OUT/jmh/${run#*:}.jar"
        echo "JMH $name: ${prefix[*]+${prefix[*]} }java -jar $jar ${bench_args[*]}"
        ${prefix[@]+"${prefix[@]}"} java -jar "$jar" "${bench_args[@]}" -rff "$OUT/jmh/$name.json" \
                > "$OUT/jmh/$name.log" 2>&1 || {
            tail -n 30 "$OUT/jmh/$name.log" >&2
            echo "JMH run $name failed; see $OUT/jmh/$name.log" >&2
            exit 1
        }
    done
    JMH_ARGS=(--jmh-null-a "$OUT/jmh/feature-a.json" --jmh-candidate "$OUT/jmh/candidate.json"
              --jmh-null-b "$OUT/jmh/feature-b.json")
}

VERDICT_ARGS=(--out "$RECORD" --feature-ref "$FEATURE_REF" --candidate-ref "$CANDIDATE_REF")
[[ $PRELIMINARY -eq 1 ]] && VERDICT_ARGS+=(--preliminary)
if [[ $JMH_FIRST -eq 1 ]]; then
    if [[ $RUN_JMH -eq 1 ]]; then
        run_jmh
        status=0
        python3 "$PY" jmh-check "$RACECAR_ID" "${JMH_ARGS[@]}" || status=$?
        if [[ $status -eq 3 ]]; then
            echo "== 4. Verdict (e2e not run: the JMH result decides REVERT)"
            status=0
            python3 "$PY" verdict "$RACECAR_ID" --no-e2e "${VERDICT_ARGS[@]}" "${JMH_ARGS[@]}" || status=$?
            echo "Experiment directory: $OUT"
            exit $status
        fi
        [[ $status -eq 0 ]] || exit "$status"
        echo "The JMH result does not decide REVERT on its own; running the e2e stages."
    else
        echo "Note: --jmh-first has no effect: $RACECAR_ID runs no JMH benchmarks here." >&2
    fi
fi

# ---- 1. Null experiment on the feature build ----
if [[ -n "$NULL_RUN" ]]; then
    NULL_DIR="$(cd "$NULL_RUN" && pwd)"
    echo "== 1. Null experiment reused: $NULL_DIR"
    "$SCRIPT_DIR/paired-ab.sh" ${PASS[@]+"${PASS[@]}"} --print-params > "$OUT/expected-params.tsv"
    python3 "$PY" check-null "$NULL_DIR" --expected "$OUT/expected-params.tsv" --feature-jar "$FEATURE_JAR" || exit 2
    VERDICT_ARGS+=(--null-reused "$NULL_DIR")
else
    echo "== 1. Null experiment: feature ($FEATURE_REF, ${FEATURE_SHA:0:12})"
    NULL_DIR="$(paired_ab "$OUT/null.log" --null "feature=${FEATURE_JAR:-$FEATURE_REF}")"
    sleep 1  # paired-ab.sh run ids have one-second resolution
fi
FEATURE_E2E_JAR="${FEATURE_JAR:-$OUT/jars/feature.jar}"

# ---- 2. Paired A/B, same settings ----
echo "== 2. Paired A/B: feature vs candidate ($CANDIDATE_REF, ${CANDIDATE_SHA:0:12})"
if [[ -n "$CANDIDATE_JAR" ]]; then
    CANDIDATE_ARM=(--jar "candidate=$CANDIDATE_JAR")
elif [[ $SELF -eq 1 ]]; then
    CANDIDATE_ARM=(--jar "candidate=$FEATURE_E2E_JAR")
else
    CANDIDATE_ARM=(--arm "candidate=$CANDIDATE_REF")
fi
AB_DIR="$(paired_ab "$OUT/ab.log" --jar "feature=$FEATURE_E2E_JAR" "${CANDIDATE_ARM[@]}")"

if [[ $JMH_FIRST -eq 0 && $RUN_JMH -eq 1 ]]; then
    run_jmh
fi

# ---- 4. Verdict and record ----
echo "== 4. Verdict"
VERDICT_ARGS+=(--null "$NULL_DIR/paired-ab.json" --ab "$AB_DIR/paired-ab.json")
status=0
python3 "$PY" verdict "$RACECAR_ID" "${VERDICT_ARGS[@]}" ${JMH_ARGS[@]+"${JMH_ARGS[@]}"} || status=$?
echo "Experiment directory: $OUT"
exit $status
