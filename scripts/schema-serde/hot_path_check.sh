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
# Hot-path check for the schema serde (Requirement 34.6, design D.3; task 9.18). Runs the Requirement 34.1 JMH
# benchmarks (SchemaMarshallBenchmark/SchemaUnmarshallBenchmark, task 9.16) under JMH's async-profiler integration
# with collapsed-stack output, once per event (cpu for call traces, alloc for allocation traces). JMH profiles the
# measurement iterations only, so warm-up is excluded. scripts/schema-serde/hot_path_frames.py then fails the run on
# java.lang.reflect frames or ShapeId-/Class-keyed map lookups above the benchmark stub, and warns on lambda
# allocations.
#
# Usage: scripts/schema-serde/hot_path_check.sh [options] [-- JMH args...]
#   --jar PATH      benchmarks jar (default: test/sdk-standard-benchmarks/target/benchmarks.jar; build it with
#                   mvn -o install -pl :sdk-standard-benchmarks -P quick)
#   --bench REGEX   JMH include pattern (default: the 34.1 benchmarks, 'benchmark\.schema\.Schema(Un)?MarshallBenchmark')
#   --events LIST   comma-separated async-profiler events (default: cpu,alloc)
#   --out DIR       profiler output directory (default: a new mktemp directory; kept and printed)
#   JMH args after -- are passed through, e.g. '-- -wi 1 -i 1 -f 1 -w 1s -r 1s -p payload=LARGE' for a smoke run.
#   Without them the benchmark classes' own defaults apply.
#
# Environment: ASYNC_PROFILER_HOME (the async-profiler install; default: Homebrew's /opt/homebrew/opt/async-profiler,
# then /usr/local/opt/async-profiler and /opt/async-profiler), JAVA_HOME (default: the java on PATH).
#
# Exit status: 0 clean, 1 violations found, 2 usage or setup error.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JAR="$ROOT/test/sdk-standard-benchmarks/target/benchmarks.jar"
BENCH='benchmark\.schema\.Schema(Un)?MarshallBenchmark'
EVENTS="cpu,alloc"
OUT=""
JMH_ARGS=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --jar)    JAR="$2"; shift 2 ;;
        --bench)  BENCH="$2"; shift 2 ;;
        --events) EVENTS="$2"; shift 2 ;;
        --out)    OUT="$2"; shift 2 ;;
        --)       shift; JMH_ARGS=("$@"); break ;;
        -h|--help) sed -n '17,38p' "$0"; exit 0 ;;
        *) echo "error: unknown option $1" >&2; exit 2 ;;
    esac
done
find_lib() {
    local home
    for home in "${ASYNC_PROFILER_HOME:-}" /opt/homebrew/opt/async-profiler /usr/local/opt/async-profiler \
                /opt/async-profiler; do
        [[ -z "$home" ]] && continue
        for lib in "$home/lib/libasyncProfiler.dylib" "$home/lib/libasyncProfiler.so"; do
            if [[ -e "$lib" ]]; then
                echo "$lib"
                return 0
            fi
        done
    done
    return 1
}
LIB="$(find_lib)" || { echo "error: async-profiler not found; set ASYNC_PROFILER_HOME" >&2; exit 2; }
[[ -f "$JAR" ]] || { echo "error: $JAR not found (build test/sdk-standard-benchmarks first)" >&2; exit 2; }
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
OUT="${OUT:-$(mktemp -d "${TMPDIR:-/tmp}/hot-path.XXXXXXXX")}"
mkdir -p "$OUT"
echo "async-profiler: $LIB"
echo "output: $OUT"
STATUS=0
IFS=',' read -r -a EVENT_LIST <<< "$EVENTS"
for event in "${EVENT_LIST[@]}"; do
    dir="$OUT/$event"
    mkdir -p "$dir"
    echo "=== $event: $BENCH ${JMH_ARGS[*]+${JMH_ARGS[*]}}"
    if ! "$JAVA" -jar "$JAR" "$BENCH" -prof "async:libPath=$LIB;output=collapsed;event=$event;dir=$dir" \
            ${JMH_ARGS[@]+"${JMH_ARGS[@]}"} > "$dir/jmh.log" 2>&1; then
        echo "error: JMH failed for event $event; see $dir/jmh.log" >&2
        tail -20 "$dir/jmh.log" >&2
        exit 2
    fi
    files=()
    while IFS= read -r f; do files+=("$f"); done < <(find "$dir" -name 'collapsed-*.csv' | sort)
    if [[ ${#files[@]} -eq 0 ]]; then
        echo "error: no collapsed output under $dir; see $dir/jmh.log" >&2
        exit 2
    fi
    python3 "$ROOT/scripts/schema-serde/hot_path_frames.py" "${files[@]}" || STATUS=1
done
if [[ $STATUS -ne 0 ]]; then
    echo "FAIL: hot-path violations found (Requirement 34.6); stacks are under $OUT"
else
    echo "PASS: no hot-path violations"
fi
exit $STATUS
