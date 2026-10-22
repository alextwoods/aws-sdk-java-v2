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
# Serde A/B of the standard JMH benchmarks (test/sdk-standard-benchmarks, benchmark.serde.*: 10 classes, 71
# benchmark/param combinations) between builds made from git refs, typically the Baseline_Build (fc8b74e9fe21,
# legacy marshallers) and the feature branch (the same classes on ClientProtocol and the ported codecs).
#
# For each distinct commit the script checks it out into its own git worktree, overlays the serde models and payloads
# (src/main/resources/codegen-resources and serde-tests) from --bench-src so every arm reads the same inputs, and
# builds sdk-standard-benchmarks (-P quick -am) into the worktree's own local Maven repository (<worktree>-m2, chained
# in front of the shared one, read-only). Each jar is copied to <out>/jars/<label>.jar with
# <label>.jar.provenance.properties (ref, commit, bench-src, overlay status, sdk-pin result, jar sha256, JDK, Maven,
# host) and <label>.jar.resources.tsv (sha256 manifest of the overlaid resources). Every build and run uses the JDK
# that runs this script.
#
# Verify (before timing; serde_ab_checks.py): resources and intermediate models identical, SerdeWireDump.java output
# identical (the legacy path over each jar), and the expected runtime code path per arm (async-profiler collapsed
# stacks when async-profiler is found, and always a -Xlog:class+load log). Any failure stops the run (exit 1).
#
# Timing: for each repetition, each class, each arm (A,B on odd reps and B,A on even reps unless --order fixed):
# java -jar <jar> <class> -foe true -prof gc -rf json ... with identical JMH options and JVM arguments. Null
# experiment (--null): one jar as LABEL-a and LABEL-b, so every difference is noise; pass its serde-ab.json to
# serde_ab_summary.py summarize --null. Profiling (--profile, --profile-only): a separate async-profiler pass over
# every class, param, arm and event, compared side by side by serde_ab_profile.py, which also checks the runtime code
# path of every cpu profile (profile-path.tsv; a FAIL fails the run) and writes the work-boundary shares
# (boundary.tsv: A1 per-call legacy marshaller construction, A2 response metadata, A5 Query form encoding). With
# --profile after timing, summary.md is rewritten with serde_ab_summary.py summarize --boundary (serde-only estimate).
#
# Output (<out>/<run-id>/): arms.tsv, params.tsv, runs.tsv, provenance/<label>.properties (the jar's record plus
# this run's SDK pin result; jar provenance files are not edited after their build), jmh/, verify/ (verify.md),
# logs/, profile/, serde-ab.json, summary.md, profile-comparison.md, profile-path.tsv, boundary.tsv.
#
# Usage: scripts/schema-serde/serde-ab.sh (--arm LABEL=REF ... | --null LABEL=REF|PATH) [options]
#   --arm LABEL=REF        arm built from git ref REF (first arm = baseline); repeat
#   --jar LABEL=PATH       prebuilt benchmarks.jar; reads PATH.provenance.properties if present; may mix with --arm
#   --null LABEL=REF|PATH  null experiment: one jar as LABEL-a and LABEL-b
#   --bench-src REF        source of the resource overlay (default HEAD)
#   --no-overlay           build every arm's resources as committed (recorded; the resources check then fails)
#   --sdk-pin LABEL=REF    fail unless LABEL's commit differs from REF only under test/sdk-standard-benchmarks/,
#                          scripts/ or docs/ (the changed paths are recorded); repeat per label
#   --expect-path LABEL=legacy|schema   expected runtime code path (default: first arm legacy, others schema;
#                          a null experiment defaults to schema)
#   --include REGEX        JMH include (default 'benchmark\.serde\.[A-Za-z0-9]+Benchmark\.(marshall|unmarshall)$')
#   --classes LIST         comma-separated subset of the 10 serde classes (default: all the include matches)
#   --jmh-opts "..."       extra JMH options for timing runs (default none: the class annotations apply)
#   --jvm-args "..."       -jvmArgsAppend for every fork (default "-Xms2g -Xmx2g -XX:+AlwaysPreTouch")
#   --reps N               repetitions (default 2)    --order alternate|fixed (default alternate)
#   --pin CPUS             taskset -c CPUS around every java (Linux only; exit 2 elsewhere)
#   --verify-only | --skip-verify       checks only / no checks (skip is recorded in params.tsv)
#   --path-profiler auto|async|classload   (auto: async when async-profiler is found, always classload)
#   --profile              after timing, run the profiling pass and write profile-comparison.md
#   --profile-only         the profiling pass only (no verify, no timing)
#   --profile-events LIST  async-profiler events (default cpu,alloc; cpu falls back to itimer on Linux without
#                          perf_events)
#   --profile-jmh-opts "..."  JMH options of the profiling pass (default "-f 1 -wi 2 -w 2s -i 3 -r 3s")
#   --skip-build           reuse <out>/jars/<label>.jar when its provenance commit matches
#   --online               let Maven download missing artifacts (default: offline, -o)
#   --out DIR              output root (default ~/.cache/aws-sdk-java-v2/serde-ab)
#
# Environment: SERDE_AB_WORKTREE_ROOT (default $TMPDIR or /tmp), MAVEN_LOCAL_REPO (default ~/.m2/repository),
# JAVA_HOME (default: the java on PATH), ASYNC_PROFILER_HOME (default: Homebrew, then /opt/async-profiler).
#
# Exit status: 0 success, 1 a build, check or JMH run failed, 2 usage or environment error.
set -euo pipefail
ARMS=()
NULL_SPEC=""
BENCH_SRC="HEAD"
OVERLAY=1
SDK_PINS=()
EXPECT_PATHS=()
INCLUDE='benchmark\.serde\.[A-Za-z0-9]+Benchmark\.(marshall|unmarshall)$'
CLASSES=""
JMH_OPTS=""
JVM_ARGS="-Xms2g -Xmx2g -XX:+AlwaysPreTouch"
REPS=2
ORDER="alternate"
PIN=""
VERIFY_ONLY=0
SKIP_VERIFY=0
PATH_PROFILER="auto"
PROFILE=0
PROFILE_ONLY=0
PROFILE_EVENTS="cpu,alloc"
PROFILE_JMH_OPTS="-f 1 -wi 2 -w 2s -i 3 -r 3s"
SKIP_BUILD=0
OFFLINE="-o"
OUT_ROOT="$HOME/.cache/aws-sdk-java-v2/serde-ab"
usage() { sed -n '/^# Usage:/,/^# Exit status/p' "$0" | sed 's/^# \{0,1\}//'; }
need() { [[ $# -ge 2 ]] || { echo "$1 needs a value" >&2; exit 2; }; }
while [[ $# -gt 0 ]]; do
    case "$1" in
        --arm) need "$@"; ARMS+=("ref:$2"); shift 2 ;;
        --jar) need "$@"; ARMS+=("jar:$2"); shift 2 ;;
        --null) need "$@"; NULL_SPEC="$2"; shift 2 ;;
        --bench-src) need "$@"; BENCH_SRC="$2"; shift 2 ;;
        --no-overlay) OVERLAY=0; shift ;;
        --sdk-pin) need "$@"; SDK_PINS+=("$2"); shift 2 ;;
        --expect-path) need "$@"; EXPECT_PATHS+=("$2"); shift 2 ;;
        --include) need "$@"; INCLUDE="$2"; shift 2 ;;
        --classes) need "$@"; CLASSES="$2"; shift 2 ;;
        --jmh-opts) need "$@"; JMH_OPTS="$2"; shift 2 ;;
        --jvm-args) need "$@"; JVM_ARGS="$2"; shift 2 ;;
        --reps) need "$@"; REPS="$2"; shift 2 ;;
        --order) need "$@"; ORDER="$2"; shift 2 ;;
        --pin) need "$@"; PIN="$2"; shift 2 ;;
        --verify-only) VERIFY_ONLY=1; shift ;;
        --skip-verify) SKIP_VERIFY=1; shift ;;
        --path-profiler) need "$@"; PATH_PROFILER="$2"; shift 2 ;;
        --profile) PROFILE=1; shift ;;
        --profile-only) PROFILE_ONLY=1; PROFILE=1; shift ;;
        --profile-events) need "$@"; PROFILE_EVENTS="$2"; shift 2 ;;
        --profile-jmh-opts) need "$@"; PROFILE_JMH_OPTS="$2"; shift 2 ;;
        --skip-build) SKIP_BUILD=1; shift ;;
        --online) OFFLINE=""; shift ;;
        --out) need "$@"; OUT_ROOT="$2"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown argument: $1 (see --help)" >&2; exit 2 ;;
    esac
done

# ---- Validation (before any build) ----
fail_usage() { echo "$1" >&2; exit 2; }
if [[ -n "$PIN" ]]; then
    if [[ "$(uname)" != "Linux" ]] || ! command -v taskset > /dev/null 2>&1; then
        fail_usage "CPU pinning (--pin) needs taskset; Linux only"
    fi
fi
[[ "$ORDER" == "alternate" || "$ORDER" == "fixed" ]] || fail_usage "--order must be alternate or fixed"
[[ "$PATH_PROFILER" =~ ^(auto|async|classload)$ ]] || fail_usage "--path-profiler must be auto, async or classload"
[[ "$REPS" =~ ^[1-9][0-9]*$ ]] || fail_usage "--reps must be a positive integer"
[[ $VERIFY_ONLY -eq 1 && $SKIP_VERIFY -eq 1 ]] && fail_usage "--verify-only and --skip-verify exclude each other"
[[ $VERIFY_ONLY -eq 1 && $PROFILE -eq 1 ]] && fail_usage "--verify-only cannot be combined with --profile"
for kv in ${EXPECT_PATHS[@]+"${EXPECT_PATHS[@]}"}; do
    [[ "$kv" =~ ^[^=]+=(legacy|schema)$ ]] || fail_usage "--expect-path needs LABEL=legacy|schema, got '$kv'"
done
NULL_EXPERIMENT=false
if [[ -n "$NULL_SPEC" ]]; then
    [[ ${#ARMS[@]} -eq 0 ]] || fail_usage "--null cannot be combined with --arm/--jar"
    [[ "$NULL_SPEC" == *=* ]] || fail_usage "--null needs LABEL=REF or LABEL=PATH"
    NULL_EXPERIMENT=true
    if [[ -f "${NULL_SPEC#*=}" ]]; then ARMS=("jar:$NULL_SPEC"); else ARMS=("ref:$NULL_SPEC"); fi
elif [[ ${#ARMS[@]} -lt 2 ]]; then
    fail_usage "Need at least two arms (--arm/--jar), or --null LABEL=REF|PATH"
fi
REPO_ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SHARED_REPO="${MAVEN_LOCAL_REPO:-$HOME/.m2/repository}"
WT_ROOT="${SERDE_AB_WORKTREE_ROOT:-${TMPDIR:-/tmp}}"
WT_ROOT="${WT_ROOT%/}"
if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi
command -v java > /dev/null || fail_usage "java not found (set JAVA_HOME)"
# Unset so every arm sees the same environment.
unset AWS_SDK_UA_APP_ID || true
BENCH_SRC_SHA="$(git -C "$REPO_ROOT" rev-parse --verify "${BENCH_SRC}^{commit}")" \
    || fail_usage "--bench-src $BENCH_SRC is not a commit"
OVERLAY_PATHS=(test/sdk-standard-benchmarks/src/main/resources/codegen-resources
               test/sdk-standard-benchmarks/src/main/resources/serde-tests)
find_async_profiler() {
    local home lib
    for home in "${ASYNC_PROFILER_HOME:-}" /opt/homebrew/opt/async-profiler /usr/local/opt/async-profiler \
                /opt/async-profiler; do
        [[ -z "$home" ]] && continue
        for lib in "$home/lib/libasyncProfiler.dylib" "$home/lib/libasyncProfiler.so"; do
            [[ -e "$lib" ]] && { echo "$lib"; return 0; }
        done
    done
    return 1
}
ASYNC_LIB="$(find_async_profiler || true)"
if [[ "$PATH_PROFILER" == "async" || $PROFILE -eq 1 ]] && [[ -z "$ASYNC_LIB" ]]; then
    fail_usage "async-profiler not found; set ASYNC_PROFILER_HOME (needed by --path-profiler async / --profile)"
fi
[[ "$PATH_PROFILER" == "classload" ]] && PATH_ASYNC_LIB="" || PATH_ASYNC_LIB="$ASYNC_LIB"
lookup() {  # <key> <key=value>... -> value of the first matching key
    local key="$1" kv
    shift
    for kv in "$@"; do
        [[ "${kv%%=*}" == "$key" ]] && { echo "${kv#*=}"; return 0; }
    done
    return 1
}
prop() { [[ -f "$1" ]] && sed -n "s/^$2=//p" "$1" | head -1 || true; }
sha256_of() { if command -v sha256sum > /dev/null; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi; }
one_line() { tr '\n' ';' | sed 's/;$//'; }

RUN_ID="$(date +%Y%m%d-%H%M%S)"
RUNDIR="$OUT_ROOT/$RUN_ID"
JARDIR="$OUT_ROOT/jars"
mkdir -p "$RUNDIR/logs" "$RUNDIR/jmh" "$RUNDIR/verify" "$RUNDIR/provenance" "$JARDIR"
echo "Run directory: $RUNDIR"

# ---- Build: one worktree and jar per arm commit ----
sdk_pin_check() {  # <pin ref> <commit> -> prints "result<TAB>changed paths"; returns 1 on violation
    local pin="$1" sha="$2" pin_sha changed bad
    pin_sha="$(git -C "$REPO_ROOT" rev-parse --verify "${pin}^{commit}")" || { printf 'failed: %s is not a commit\t\n' "$pin"; return 1; }
    changed="$(git -C "$REPO_ROOT" diff --name-only "$pin_sha" "$sha")"
    bad="$(printf '%s\n' "$changed" | grep -v -E '^(test/sdk-standard-benchmarks/|scripts/|docs/|$)' || true)"
    if [[ -n "$bad" ]]; then
        printf 'failed: %s..%s changes %s files outside test/sdk-standard-benchmarks/, scripts/, docs/ (first: %s)\t%s\n' \
            "${pin_sha:0:12}" "${sha:0:12}" "$(printf '%s\n' "$bad" | grep -c .)" \
            "$(printf '%s\n' "$bad" | head -5 | one_line)" "$(printf '%s' "$changed" | one_line)"
        return 1
    fi
    printf 'ok: %s..%s changes only test/sdk-standard-benchmarks/, scripts/, docs/ (%s files)\t%s\n' \
        "${pin_sha:0:12}" "${sha:0:12}" "$(printf '%s' "$changed" | grep -c . || true)" "$(printf '%s' "$changed" | one_line)"
}
build_jmh() {  # <label> <ref> <sha> <sdk pin> <pin result> <pin changed paths> -> $JARDIR/<label>.jar + provenance
    local label="$1" ref="$2" sha="$3" id="${3:0:12}" sdk_pin="$4" sdk_pin_result="$5" sdk_pin_paths="$6"
    local wt="$WT_ROOT/aws-sdk-serde-ab-$id"
    local m2="$wt-m2"
    local log="$RUNDIR/logs/build-$label-$id.log"
    local jar="$JARDIR/$label.jar"
    local props="$jar.provenance.properties"
    if [[ $SKIP_BUILD -eq 1 && -f "$jar" && "$(prop "$props" commit)" == "$sha" \
          && "$(prop "$props" bench_src_sha)" == "$BENCH_SRC_SHA" && "$(prop "$props" overlay)" == "$([[ $OVERLAY -eq 1 ]] && echo true || echo false)" ]]; then
        echo "Reusing $jar (--skip-build; provenance matches $id)"
        return 0
    fi
    if [[ -d "$wt" ]]; then
        local actual
        actual="$(git -C "$wt" rev-parse HEAD)"
        if [[ "$actual" != "$sha" ]]; then
            echo "$wt is at $actual, not $sha. Remove it (git worktree remove) or set SERDE_AB_WORKTREE_ROOT." >&2
            return 1
        fi
    else
        git -C "$REPO_ROOT" worktree add --detach "$wt" "$sha" >&2
    fi
    # The overlay is idempotent; --no-overlay restores the committed resources and fails if anything is left over.
    if [[ $OVERLAY -eq 1 ]]; then
        git -C "$wt" checkout "$BENCH_SRC_SHA" -- "${OVERLAY_PATHS[@]}"
    else
        git -C "$wt" checkout "$sha" -- "${OVERLAY_PATHS[@]}"
    fi
    local overlay_status
    overlay_status="$(git -C "$wt" status --porcelain -- "${OVERLAY_PATHS[@]}" | one_line)"
    if [[ $OVERLAY -eq 0 && -n "$overlay_status" ]]; then
        echo "$wt still differs from $id under the resource paths ($overlay_status); remove the worktree." >&2
        return 1
    fi
    local other_changes
    other_changes="$(git -C "$wt" status --porcelain | grep -v -F -e "${OVERLAY_PATHS[0]}" -e "${OVERLAY_PATHS[1]}" | one_line || true)"
    if [[ -n "$other_changes" ]]; then
        echo "$wt has changes outside the overlay ($other_changes); remove the worktree." >&2
        return 1
    fi
    echo "Building sdk-standard-benchmarks at $id ($ref) in $wt; log: $log"
    (cd "$wt" && mvn -B $OFFLINE -Dmaven.repo.local="$m2" -Dmaven.repo.local.tail="$SHARED_REPO" \
        clean install -P quick -pl :sdk-standard-benchmarks -am) > "$log" 2>&1 || {
        tail -n 40 "$log" >&2
        echo "Build failed at $sha; see $log" >&2
        return 1
    }
    cp "$wt/test/sdk-standard-benchmarks/target/benchmarks.jar" "$jar"
    python3 "$SCRIPT_DIR/serde_ab_checks.py" manifest "$wt" > "$jar.resources.tsv"
    local version
    version="$(grep -m1 -o '<version>[^<]*</version>' "$wt/pom.xml" | sed 's/<[^>]*>//g')"
    {
        echo "label=$label"
        echo "ref=$ref"
        echo "commit=$sha"
        echo "sdk_version=$version"
        echo "bench_src_ref=$BENCH_SRC"
        echo "bench_src_sha=$BENCH_SRC_SHA"
        echo "overlay=$([[ $OVERLAY -eq 1 ]] && echo true || echo false)"
        echo "overlay_status=${overlay_status:-(none: resources as committed)}"
        echo "jar_sha256=$(sha256_of "$jar")"
        echo "resources_manifest=$jar.resources.tsv"
        echo "worktree=$wt"
        echo "maven_repo=$m2 -> $SHARED_REPO"
        echo "java_version=$(java -version 2>&1 | one_line)"
        echo "mvn_version=$(mvn -v 2>/dev/null | head -1)"
        echo "build_host=$(hostname), $(uname -sm)"
        echo "build_time=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
        echo "build_log=$log"
        echo "sdk_pin=$sdk_pin"
        echo "sdk_pin_result=$sdk_pin_result"
        if [[ -n "$sdk_pin_paths" ]]; then echo "sdk_pin_changed_paths=$sdk_pin_paths"; fi
    } > "$props"
}

# Fail fast on an SDK pin violation of any ref arm, before building anything.
for spec in "${ARMS[@]}"; do
    body="${spec#*:}"
    if [[ "${spec%%:*}" == "ref" ]] && pin="$(lookup "${body%%=*}" ${SDK_PINS[@]+"${SDK_PINS[@]}"})"; then
        sha="$(git -C "$REPO_ROOT" rev-parse --verify "${body#*=}^{commit}")" || fail_usage "Not a commit: ${body#*=}"
        pin_line="$(sdk_pin_check "$pin" "$sha")" || { echo "--sdk-pin ${body%%=*}=$pin ${pin_line%%$'\t'*}" >&2; exit 1; }
    fi
done
LABELS=()
JARS=()
EXPECTS=()
NARMS_DECLARED=0
printf 'label\tref\tcommit\tsdk_version\tjar\tjar_sha256\tbench_src\toverlay\tsdk_pin\texpect_path\tprovenance\n' \
    > "$RUNDIR/arms.tsv"
for spec in "${ARMS[@]}"; do
    kind="${spec%%:*}"
    body="${spec#*:}"
    [[ "$body" == *=* ]] || fail_usage "Arm '$body' is not LABEL=REF or LABEL=PATH"
    label="${body%%=*}"
    target="${body#*=}"
    if [[ "$kind" == "jar" ]]; then
        [[ -f "$target" ]] || fail_usage "Jar not found: $target"
        jar="$(cd "$(dirname "$target")" && pwd)/$(basename "$target")"
        props="$jar.provenance.properties"
        [[ -f "$props" ]] || props=""
        ref="$(prop "$props" ref)"; ref="${ref:-(prebuilt jar)}"
        sha="$(prop "$props" commit)"
    else
        ref="$target"
        sha="$(git -C "$REPO_ROOT" rev-parse --verify "${ref}^{commit}")" || fail_usage "Not a commit: $ref"
    fi
    # SDK pin, checked before any build: recorded in the run's provenance copy and arms.tsv; a violation stops the run.
    pin_result="none"
    pin_paths=""
    pin=""
    if pin="$(lookup "$label" ${SDK_PINS[@]+"${SDK_PINS[@]}"})"; then
        [[ -n "$sha" ]] || { echo "--sdk-pin $label: the jar records no commit" >&2; exit 1; }
        if pin_line="$(sdk_pin_check "$pin" "$sha")"; then
            pin_result="${pin_line%%$'\t'*}"
            pin_paths="${pin_line#*$'\t'}"
            echo "SDK pin $label=$pin: $pin_result"
        else
            echo "--sdk-pin $label=$pin ${pin_line%%$'\t'*}" >&2
            exit 1
        fi
    fi
    if [[ "$kind" == "ref" ]]; then
        build_jmh "$label" "$ref" "$sha" "${pin:-none}" "$pin_result" "$pin_paths" || exit 1
        jar="$JARDIR/$label.jar"
        props="$jar.provenance.properties"
    fi
    # The run's own provenance: a copy of the jar's record with this run's SDK pin result. The jar's file is never
    # edited after its build, so a later run cannot rewrite a record an earlier run cited.
    run_props="$RUNDIR/provenance/$label.properties"
    if [[ -n "$props" ]]; then sed '/^sdk_pin/d' "$props" > "$run_props"; else : > "$run_props"; fi
    { echo "source_provenance=${props:-none}"; echo "sdk_pin=${pin:-none}"; echo "sdk_pin_result=$pin_result"
      if [[ -n "$pin_paths" ]]; then echo "sdk_pin_changed_paths=$pin_paths"; fi; } >> "$run_props"
    props="$run_props"
    if expect="$(lookup "$label" ${EXPECT_PATHS[@]+"${EXPECT_PATHS[@]}"})"; then :;
    elif [[ "$NULL_EXPERIMENT" == true ]]; then expect="schema";
    elif [[ $NARMS_DECLARED -eq 0 ]]; then expect="legacy";
    else expect="schema"; fi
    NARMS_DECLARED=$((NARMS_DECLARED + 1))
    jar_sha="$(sha256_of "$jar")"
    version="$(prop "$props" sdk_version)"
    overlay="$(prop "$props" overlay)"
    bench_src="$(prop "$props" bench_src_sha)"
    suffixes=("")
    [[ "$NULL_EXPERIMENT" == true ]] && suffixes=("-a" "-b")
    for suffix in "${suffixes[@]}"; do
        LABELS+=("$label$suffix")
        JARS+=("$jar")
        EXPECTS+=("$expect")
        printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$label$suffix" "$ref" "$sha" "${version:-?}" "$jar" \
            "$jar_sha" "${bench_src:-?}" "${overlay:-?}" "$pin_result" "$expect" "${props:-none}" >> "$RUNDIR/arms.tsv"
    done
done
NARMS=${#LABELS[@]}

# ---- Benchmarks and params, from the first jar (the include and the class subset) ----
BENCH_LIST="$RUNDIR/benchmarks.tsv"  # benchmark<TAB>comma-separated testCaseId values
java -jar "${JARS[0]}" -lp "$INCLUDE" 2>/dev/null | awk '
    /^software\./ { if (b != "") print b "\t" p; b = $1; p = "" }
    /param "testCaseId"/ { sub(/.*= *\{/, ""); sub(/\}.*/, ""); gsub(/, */, ","); p = $0 }
    END { if (b != "") print b "\t" p }' > "$BENCH_LIST"
[[ -s "$BENCH_LIST" ]] || fail_usage "No benchmark in ${JARS[0]} matches --include '$INCLUDE'"
ALL_CLASSES=($(cut -f1 "$BENCH_LIST" | awk -F. '{print $(NF-1)}' | awk '!seen[$0]++'))
if [[ -n "$CLASSES" ]]; then
    IFS=',' read -r -a CLASS_ARR <<< "$CLASSES"
    for c in "${CLASS_ARR[@]}"; do
        printf '%s\n' "${ALL_CLASSES[@]}" | grep -qx "$c" || fail_usage "--classes: $c is not a serde class matching the include"
    done
else
    CLASS_ARR=("${ALL_CLASSES[@]}")
fi
class_regex() {  # <class> -> anchored JMH include for the selected benchmarks of that class
    local names
    names="$(cut -f1 "$BENCH_LIST" | grep "\.$1\.[^.]*$" | sed 's/\./\\./g' | paste -sd'|' -)"
    echo "^($names)\$"
}
class_params() { grep "\.$1\.[^.]*"$'\t' "$BENCH_LIST" | head -1 | cut -f2; }

# ---- Parameters and environment ----
if [[ "$(uname)" == "Darwin" ]]; then
    HW_CPU="$(sysctl -n machdep.cpu.brand_string 2>/dev/null || echo unknown)"
    HW_CORES="$(sysctl -n hw.ncpu 2>/dev/null || echo unknown)"
else
    HW_CPU="$(lscpu 2>/dev/null | sed -n 's/^Model name: *//p' | head -1)"
    HW_CORES="$(nproc 2>/dev/null || echo unknown)"
fi
CPU_EVENT="cpu"
PARANOID="$(cat /proc/sys/kernel/perf_event_paranoid 2>/dev/null || echo n/a)"
KPTR="$(cat /proc/sys/kernel/kptr_restrict 2>/dev/null || echo n/a)"
if [[ "$(uname)" == "Linux" && "$PARANOID" =~ ^[0-9]+$ && "$PARANOID" -gt 1 && "$(id -u)" -ne 0 ]]; then
    CPU_EVENT="itimer"  # perf_events unavailable to this user
fi
if [[ $PROFILE_ONLY -eq 1 ]]; then VERIFY_STATE="skipped (--profile-only)";
elif [[ $SKIP_VERIFY -eq 1 ]]; then VERIFY_STATE="skipped (--skip-verify)";
else VERIFY_STATE="run"; fi
{
    printf 'key\tvalue\n'
    printf 'run_id\t%s\n' "$RUN_ID"
    printf 'null_experiment\t%s\n' "$NULL_EXPERIMENT"
    printf 'include\t%s\n' "$INCLUDE"
    printf 'classes\t%s\n' "$(IFS=,; echo "${CLASS_ARR[*]}")"
    printf 'jmh_opts\t%s\n' "$JMH_OPTS"
    printf 'jvm_args\t%s\n' "$JVM_ARGS"
    printf 'reps\t%s\n' "$REPS"
    printf 'order\t%s\n' "$ORDER"
    printf 'pin\t%s\n' "$PIN"
    printf 'verify\t%s\n' "$VERIFY_STATE"
    printf 'path_profiler\t%s\n' "$PATH_PROFILER${PATH_ASYNC_LIB:+ ($PATH_ASYNC_LIB)}"
    printf 'bench_src\t%s %s\n' "$BENCH_SRC" "$BENCH_SRC_SHA"
    printf 'overlay\t%s\n' "$([[ $OVERLAY -eq 1 ]] && echo true || echo false)"
    printf 'profile\t%s\n' "$([[ $PROFILE -eq 1 ]] && echo "events $PROFILE_EVENTS, cpu event $CPU_EVENT, JMH $PROFILE_JMH_OPTS" || echo false)"
    printf 'async_profiler\t%s\n' "${ASYNC_LIB:-not found}"
    printf 'java\t%s\n' "$(java -version 2>&1 | one_line)"
    printf 'host\t%s\n' "$(hostname), $(uname -sm)"
    printf 'hardware\t%s\n' "$HW_CPU, $HW_CORES logical cores"
    if [[ "$(uname)" == "Linux" ]]; then
        printf 'kernel\t%s\n' "$(uname -r)"
        printf 'cpufreq_governor\t%s\n' "$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo n/a)"
        printf 'no_turbo\t%s\n' "$(cat /sys/devices/system/cpu/intel_pstate/no_turbo 2>/dev/null || cat /sys/devices/system/cpu/cpufreq/boost 2>/dev/null || echo n/a)"
    fi
    printf 'perf_event_paranoid\t%s\n' "$PARANOID"
    printf 'kptr_restrict\t%s\n' "$KPTR"
    printf 'started\t%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > "$RUNDIR/params.tsv"
PIN_CMD=()
[[ -n "$PIN" ]] && PIN_CMD=(taskset -c "$PIN")

# ---- Verify ----
VERIFY_FAILED=0
VERIFY_ROWS=()
verify_row() {  # <check> <arm> <command...>: runs a check, records PASS/FAIL and its output file
    local check="$1" arm="$2" out="$RUNDIR/verify/$1-$2.txt"
    shift 2
    if "$@" > "$out" 2>&1; then
        VERIFY_ROWS+=("| $check | $arm | PASS | $(grep -m1 -E '^PASS' "$out" | cut -c1-160 || true) | \`verify/$(basename "$out")\` |")
    else
        VERIFY_FAILED=1
        VERIFY_ROWS+=("| $check | $arm | **FAIL** | $(grep -m1 -E '^(FAIL|ERROR)' "$out" | cut -c1-160 || true) | \`verify/$(basename "$out")\` |")
        echo "  verify $check $arm FAILED; see $out" >&2
    fi
}
wire_dump() {  # <label> <jar>
    java -cp "$2" "$SCRIPT_DIR/SerdeWireDump.java" > "$RUNDIR/verify/wire-$1.tsv" 2> "$RUNDIR/logs/wire-$1.err"
}
code_path_run() {  # <label> <jar> <class> <param> -> verify/path/<label>/<class>/ and verify/classload-<label>-<class>.log
    local label="$1" jar="$2" cls="$3" param="$4"
    local dir="$RUNDIR/verify/path/$label/$cls"
    local cl_log="$RUNDIR/verify/classload-$label-$cls.log"
    local prof=()
    mkdir -p "$dir"
    [[ -n "$PATH_ASYNC_LIB" ]] && prof=(-prof "async:libPath=$PATH_ASYNC_LIB;event=$CPU_EVENT;interval=1000000;output=collapsed;dir=$dir")
    ${PIN_CMD[@]+"${PIN_CMD[@]}"} java -jar "$jar" "$(class_regex "$cls")" -p "testCaseId=$param" \
        -f 1 -wi 1 -i 1 -w 1s -r 1s -foe true \
        -jvmArgsAppend "$JVM_ARGS -Xlog:class+load=info:file=$cl_log" ${prof[@]+"${prof[@]}"} \
        > "$RUNDIR/logs/path-$label-$cls.log" 2>&1
}
if [[ "$VERIFY_STATE" == "run" ]]; then
    echo "== Verify"
    base_label="${LABELS[0]}"
    base_jar="${JARS[0]}"
    for i in $(seq 1 $((NARMS - 1))); do
        label="${LABELS[$i]}"
        jar="${JARS[$i]}"
        verify_row resources-jar "$label" python3 "$SCRIPT_DIR/serde_ab_checks.py" resources "$base_jar" "$jar"
        if [[ -f "$base_jar.resources.tsv" && -f "$jar.resources.tsv" ]]; then
            verify_row resources-src "$label" python3 "$SCRIPT_DIR/serde_ab_checks.py" resources \
                "$base_jar.resources.tsv" "$jar.resources.tsv"
        else
            VERIFY_ROWS+=("| resources-src | $label | SKIPPED | no worktree manifest next to a prebuilt jar; the models check covers codegen-resources | - |")
        fi
        verify_row models "$label" python3 "$SCRIPT_DIR/serde_ab_checks.py" models "$base_jar" "$jar"
    done
    for i in $(seq 0 $((NARMS - 1))); do
        echo "Wire dump ${LABELS[$i]}"
        verify_row wire-dump "${LABELS[$i]}" wire_dump "${LABELS[$i]}" "${JARS[$i]}"
    done
    for i in $(seq 1 $((NARMS - 1))); do
        verify_row wire "${LABELS[$i]}" python3 "$SCRIPT_DIR/serde_ab_checks.py" wire \
            "$RUNDIR/verify/wire-$base_label.tsv" "$RUNDIR/verify/wire-${LABELS[$i]}.tsv"
    done
    for i in $(seq 0 $((NARMS - 1))); do
        label="${LABELS[$i]}"
        for cls in "${CLASS_ARR[@]}"; do
            param="$(class_params "$cls" | cut -d, -f1)"
            echo "Code path ${LABELS[$i]} $cls [$param] (expect ${EXPECTS[$i]})"
            verify_row "jmh-$cls" "$label" code_path_run "$label" "${JARS[$i]}" "$cls" "$param"
            if [[ -n "$PATH_ASYNC_LIB" ]]; then
                verify_row "path-$cls" "$label" python3 "$SCRIPT_DIR/serde_ab_checks.py" path \
                    --expect "${EXPECTS[$i]}" "$RUNDIR/verify/path/$label/$cls"
            fi
            verify_row "classload-$cls" "$label" python3 "$SCRIPT_DIR/serde_ab_checks.py" classload \
                --expect "${EXPECTS[$i]}" "$RUNDIR/verify/classload-$label-$cls.log"
        done
    done
    {
        echo "# Serde A/B verify \`$RUN_ID\`"
        echo
        echo "Baseline arm \`$base_label\`; expected code paths: $(for i in $(seq 0 $((NARMS - 1))); do printf '`%s` %s; ' "${LABELS[$i]}" "${EXPECTS[$i]}"; done)"
        echo "path profiler: ${PATH_ASYNC_LIB:-none (class-load only)}. Result: $([[ $VERIFY_FAILED -eq 0 ]] && echo PASS || echo FAIL)."
        echo
        echo "| check | arm | result | detail | output |"
        echo "|---|---|---|---|---|"
        printf '%s\n' "${VERIFY_ROWS[@]}"
        echo
        echo "KNOWN lines in the models and wire outputs are value-checked differences documented in"
        echo "\`scripts/schema-serde/serde_ab_checks.py\`; anything else fails."
    } > "$RUNDIR/verify/verify.md"
    echo "Verify: $([[ $VERIFY_FAILED -eq 0 ]] && echo PASS || echo FAIL); see $RUNDIR/verify/verify.md"
    if [[ $VERIFY_FAILED -ne 0 ]]; then
        exit 1
    fi
    if [[ $VERIFY_ONLY -eq 1 ]]; then
        echo "Run directory: $RUNDIR"
        exit 0
    fi
fi

# ---- Timing ----
FAILURES=0
printf 'run\trep\tarm\tclass\tstatus\tstarted\tjson\n' > "$RUNDIR/runs.tsv"
if [[ $PROFILE_ONLY -eq 0 ]]; then
    echo "== Timing"
    # shellcheck disable=SC2206 # --jmh-opts is word-split on purpose
    TIMING_OPTS=($JMH_OPTS)
    TOTAL=$(( REPS * ${#CLASS_ARR[@]} * NARMS ))
    RUN_NO=0
    for rep in $(seq 1 "$REPS"); do
        if [[ "$ORDER" == "alternate" && $((rep % 2)) -eq 0 ]]; then
            order=($(seq $((NARMS - 1)) -1 0))
        else
            order=($(seq 0 $((NARMS - 1))))
        fi
        for cls in "${CLASS_ARR[@]}"; do
            for i in "${order[@]}"; do
                RUN_NO=$((RUN_NO + 1))
                label="${LABELS[$i]}"
                json="jmh/rep$rep-$cls-$label.json"
                log="$RUNDIR/logs/rep$rep-$cls-$label.log"
                started="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
                echo "[$RUN_NO/$TOTAL] rep$rep $cls $label"
                if ${PIN_CMD[@]+"${PIN_CMD[@]}"} java -jar "${JARS[$i]}" "$(class_regex "$cls")" -foe true -prof gc \
                        -rf json -rff "$RUNDIR/$json" -jvmArgsAppend "$JVM_ARGS" \
                        ${TIMING_OPTS[@]+"${TIMING_OPTS[@]}"} > "$log" 2>&1; then
                    status=ok
                else
                    status=failed
                    FAILURES=$((FAILURES + 1))
                    echo "  FAILED; see $log" >&2
                fi
                printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$RUN_NO" "$rep" "$label" "$cls" "$status" "$started" "$json" \
                    >> "$RUNDIR/runs.tsv"
            done
        done
    done
    echo "Runs: $RUN_NO, failures: $FAILURES"
    python3 "$SCRIPT_DIR/serde_ab_summary.py" collect "$RUNDIR"
    python3 "$SCRIPT_DIR/serde_ab_summary.py" summarize "$RUNDIR/serde-ab.json" | tee "$RUNDIR/summary.md"
fi

# ---- Profiling pass ----
if [[ $PROFILE -eq 1 ]]; then
    echo "== Profile (events $PROFILE_EVENTS; cpu event $CPU_EVENT)"
    # shellcheck disable=SC2206 # --profile-jmh-opts is word-split on purpose
    PROF_OPTS=($PROFILE_JMH_OPTS)
    IFS=',' read -r -a EVENT_ARR <<< "$PROFILE_EVENTS"
    printf 'arm\tclass\tparam\tevent\tstatus\tdir\n' > "$RUNDIR/profile-runs.tsv"
    for cls in "${CLASS_ARR[@]}"; do
        IFS=',' read -r -a PARAM_ARR <<< "$(class_params "$cls")"
        for param in "${PARAM_ARR[@]}"; do
            for event in "${EVENT_ARR[@]}"; do
                ev="$event"
                [[ "$ev" == "cpu" ]] && ev="$CPU_EVENT"
                # CPU: 1 ms sampling interval (10x the default) for enough samples per category. Allocation
                # profiles record bytes (async-profiler "total"), so their shares are shares of bytes.
                raw=""
                [[ "$event" == "cpu" ]] && raw=";interval=1000000"
                [[ "$event" == "alloc" ]] && raw=";rawCommand=total"
                for i in $(seq 0 $((NARMS - 1))); do
                    label="${LABELS[$i]}"
                    dir="$RUNDIR/profile/$label/$cls/$param/$event"
                    mkdir -p "$dir"
                    echo "profile $label $cls [$param] $event"
                    if ${PIN_CMD[@]+"${PIN_CMD[@]}"} java -jar "${JARS[$i]}" "$(class_regex "$cls")" \
                            -p "testCaseId=$param" -foe true -prof gc -rf json -rff "$dir/jmh.json" \
                            -jvmArgsAppend "$JVM_ARGS -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints" \
                            -prof "async:libPath=$ASYNC_LIB;event=$ev;output=collapsed,flamegraph;dir=$dir$raw" \
                            ${PROF_OPTS[@]+"${PROF_OPTS[@]}"} > "$dir/jmh.log" 2>&1; then
                        status=ok
                    else
                        status=failed
                        FAILURES=$((FAILURES + 1))
                        echo "  FAILED; see $dir/jmh.log" >&2
                    fi
                    printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$label" "$cls" "$param" "$event" "$status" \
                        "profile/$label/$cls/$param/$event" >> "$RUNDIR/profile-runs.tsv"
                done
            done
        done
    done
    COMPARE_ARGS=(compare "$RUNDIR")
    [[ -f "$RUNDIR/serde-ab.json" ]] && COMPARE_ARGS+=(--timing "$RUNDIR/serde-ab.json")
    # compare also checks the runtime code path of every cpu profile (profile-path.tsv; exit 1 on any FAIL) and
    # writes the work-boundary shares (boundary.tsv).
    python3 "$SCRIPT_DIR/serde_ab_profile.py" "${COMPARE_ARGS[@]}" || FAILURES=$((FAILURES + 1))
    echo "Profile comparison: $RUNDIR/profile-comparison.md"
    if [[ $PROFILE_ONLY -eq 0 && "$NULL_EXPERIMENT" == false && -f "$RUNDIR/boundary.tsv" ]]; then
        # The summary again, with the boundary flags and the serde-only estimate.
        python3 "$SCRIPT_DIR/serde_ab_summary.py" summarize "$RUNDIR/serde-ab.json" \
            --boundary "$RUNDIR/boundary.tsv" > "$RUNDIR/summary.md"
        echo "Summary with the serde-only estimate: $RUNDIR/summary.md"
    fi
fi
echo "Run directory: $RUNDIR"
[[ $FAILURES -eq 0 ]]
