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
# Builds the Size_Report Baseline_Build (Requirements 36.1 and 36.7) for test/size-report.
#
# The Baseline_Build is the SDK built from the merge base of the schema-serde branch. This script checks that commit
# out into a git worktree, builds the requested service modules (and the modules they need) there with the JDK and
# Maven that run this script, with the same flags as the feature build (-P quick), and copies each service JAR and
# its software.amazon.awssdk runtime dependencies to
#
#   <out>/jars/<artifactId>.jar
#   <out>/build-info.properties      JDK, Maven, compiler options and commit of the build
#
# The default <out> is the directory test/size-report reads (size-report.baselineDir in its pom.xml).
#
# Isolation: the worktree build installs into its own local repository (<worktree>-m2), chained in front of the
# shared one (read-only), so the baseline's 2.x-SNAPSHOT artifacts never replace the feature build's.
#
# The script fails if the effective maven-compiler-plugin configuration of a service differs between the baseline
# commit and the current checkout, or if a baseline service JAR still looks like a feature build.
#
# Usage: scripts/schema-serde/build-size-baseline.sh [--ref <commit>] [--modules <a,b>] [--out <dir>] [--online]
#   --ref      Baseline_Build commit (default: fc8b74e9fe21, the merge base with feature/master/smithy-migration)
#   --modules  comma-separated service artifactIds (default: cloudwatch)
#   --out      output directory (default: ~/.cache/aws-sdk-java-v2/size-baseline/<12-char commit>)
#   --online   let Maven download missing artifacts (default: offline, -o)
#
set -euo pipefail

REF="fc8b74e9fe21"
MODULES="cloudwatch"
OUT=""
OFFLINE="-o"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --ref) REF="$2"; shift 2 ;;
        --modules) MODULES="$2"; shift 2 ;;
        --out) OUT="$2"; shift 2 ;;
        --online) OFFLINE=""; shift ;;
        -h|--help) sed -n '16,38p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done

REPO_ROOT="$(git rev-parse --show-toplevel)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_SHA="$(git -C "$REPO_ROOT" rev-parse --verify "${REF}^{commit}")"
BASE_ID="${BASE_SHA:0:12}"
OUT="${OUT:-$HOME/.cache/aws-sdk-java-v2/size-baseline/$BASE_ID}"
WORKTREE="${SIZE_BASELINE_WORKTREE:-${TMPDIR:-/tmp}/aws-sdk-size-baseline-$BASE_ID}"
WORKTREE="${WORKTREE%/}"
HEAD_REPO="$WORKTREE-m2"
SHARED_REPO="${MAVEN_LOCAL_REPO:-$HOME/.m2/repository}"
LOG_DIR="$OUT/logs"

mkdir -p "$OUT/jars" "$LOG_DIR"
# Unset so the build and the report see the same environment as the feature build.
unset AWS_SDK_UA_APP_ID || true

MVN=(mvn -B $OFFLINE -Dmaven.repo.local="$HEAD_REPO" -Dmaven.repo.local.tail="$SHARED_REPO")

echo "Baseline_Build commit: $BASE_SHA"
echo "Worktree:              $WORKTREE"
echo "Output:                $OUT"

if [[ -d "$WORKTREE" ]]; then
    actual="$(git -C "$WORKTREE" rev-parse HEAD)"
    if [[ "$actual" != "$BASE_SHA" ]]; then
        echo "$WORKTREE is at $actual, not $BASE_SHA. Remove it (git worktree remove) or set SIZE_BASELINE_WORKTREE." >&2
        exit 1
    fi
else
    git -C "$REPO_ROOT" worktree add --detach "$WORKTREE" "$BASE_SHA"
fi

PL=""
for m in ${MODULES//,/ }; do PL="$PL,:$m"; done
PL="${PL#,}"

echo "Building $PL (and required modules) with -P quick; log: $LOG_DIR/build.log"
(cd "$WORKTREE" && "${MVN[@]}" clean install -P quick -pl "$PL" -am) > "$LOG_DIR/build.log" 2>&1 || {
    tail -n 40 "$LOG_DIR/build.log" >&2
    echo "Baseline build failed; see $LOG_DIR/build.log" >&2
    exit 1
}

# The JDK that ran javac (maven-compiler-plugin runs in process: fork=false).
mvn_version_out="$(cd "$WORKTREE" && mvn -v)"
java_version="$(sed -n 's/^Java version: \([^,]*\),.*/\1/p' <<< "$mvn_version_out")"
java_vendor="$(sed -n 's/^Java version: [^,]*, vendor: \([^,]*\),.*/\1/p' <<< "$mvn_version_out")"
maven_version="$(sed -n 's/^Apache Maven \([^ ]*\).*/\1/p' <<< "$mvn_version_out")"

compiler_options=""
for m in ${MODULES//,/ }; do
    module_dir="$(cd "$WORKTREE" && find services -maxdepth 2 -name pom.xml -path "services/$m/pom.xml" -exec dirname {} \;)"
    jar="$(ls "$WORKTREE/$module_dir"/target/*.jar | grep -v -e '-sources.jar$' -e '-javadoc.jar$' -e '-tests.jar$' | head -n 1)"
    entries="$(unzip -Z1 "$jar")"
    if grep -q 'Schemas.class$' <<< "$entries"; then
        echo "$jar contains generated Schemas classes; it is not a Baseline_Build JAR" >&2
        exit 1
    fi
    if ! grep -q '/transform/' <<< "$entries"; then
        echo "$jar has no transform package; it is not a Baseline_Build JAR" >&2
        exit 1
    fi
    cp "$jar" "$OUT/jars/$m.jar"

    echo "Copying the runtime dependencies of $m"
    (cd "$WORKTREE" && "${MVN[@]}" org.apache.maven.plugins:maven-dependency-plugin:3.1.1:copy-dependencies \
        -pl ":$m" -DincludeScope=runtime -DincludeGroupIds=software.amazon.awssdk -Dmdep.stripVersion=true \
        -DoutputDirectory="$OUT/jars") > "$LOG_DIR/copy-dependencies-$m.log" 2>&1 || {
        tail -n 40 "$LOG_DIR/copy-dependencies-$m.log" >&2
        exit 1
    }

    echo "Comparing the effective compiler configuration of $m with the current checkout"
    (cd "$WORKTREE" && "${MVN[@]}" -q org.apache.maven.plugins:maven-help-plugin:3.5.1:effective-pom -pl ":$m" \
        -Doutput="$OUT/effective-pom-baseline-$m.xml") > "$LOG_DIR/effective-pom-baseline-$m.log" 2>&1
    (cd "$REPO_ROOT" && mvn -B $OFFLINE -q org.apache.maven.plugins:maven-help-plugin:3.5.1:effective-pom -pl ":$m" \
        -Doutput="$OUT/effective-pom-feature-$m.xml") > "$LOG_DIR/effective-pom-feature-$m.log" 2>&1
    options="$(python3 "$SCRIPT_DIR/compiler_config.py" "$OUT/effective-pom-baseline-$m.xml" \
        "$OUT/effective-pom-feature-$m.xml")"
    if [[ -n "$compiler_options" && "$options" != "$compiler_options" ]]; then
        echo "Compiler options differ between services: '$compiler_options' and '$options'" >&2
        exit 1
    fi
    compiler_options="$options"
done

cat > "$OUT/build-info.properties" <<EOF
# Written by scripts/schema-serde/build-size-baseline.sh
baseline.commit=$BASE_SHA
baseline.modules=$MODULES
java.version=$java_version
java.vendor=$java_vendor
maven.version=$maven_version
maven.flags=clean install -P quick -pl <modules> -am
compiler.options=$compiler_options
EOF

echo "Baseline_Build written to $OUT"
cat "$OUT/build-info.properties"
