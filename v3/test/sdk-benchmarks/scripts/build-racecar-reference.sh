#!/usr/bin/env bash
# Builds the pinned Racecar Performance Reference (Req 20.14, 20.15) and installs it into the local Maven repository
# under a distinct version, so it can never be mistaken for a released V2 or a V3 artifact.
#
#   scripts/build-racecar-reference.sh [--dry-run]
#
# What it does:
#   1. Checks that the pinned commit (PerformanceReferences.RACECAR_COMMIT on feature/poc/racecar) exists in this
#      repository; it never fetches, so the build is of exactly the inspected commit.
#   2. Checks out that commit, detached, into a git worktree OUTSIDE the main tree (RACECAR_WORKTREE, default
#      ~/.cache/v3-benchmarks/racecar-<commit>), so the main checkout and its v3/ reactor are untouched.
#   3. Sets the worktree's reactor version from its 2.54.18-SNAPSHOT to RACECAR_VERSION (default
#      2.54.18-RACECAR-<12-char commit>) with versions:set, offline; skipped on a re-run whose POMs already carry
#      exactly that version.
#   4. Installs, offline and without tests, the clients of the harness's finite fixtures that the Racecar reference
#      serves (DynamoDB, Kinesis, Lambda, SNS, EC2, CloudWatch), the transports the harness cells name and
#      bom-internal, with their reactor dependencies (-am). The installed coordinates are
#      software.amazon.awssdk:<artifact>:$RACECAR_VERSION.
#   5. Writes $RACECAR_WORKTREE/racecar-reference.properties (commit, version, artifacts, build time, JDK).
#
# The Racecar reference client (test/sdk-benchmarks-racecar-reference) is built against these artifacts; until both
# exist, PerformanceReferences reports the reference artifact-missing / client-missing and a release-gate collection
# fails closed. Not covered: restXml (route53 is not listed and there is no Racecar Route 53 workload; S3 is installed
# only as a reactor dependency) and the streaming and Event Stream kinds, so those cells stay uncovered.
#
# The -am build compiles the V2-derived codegen, core, the listed services and the HTTP clients (a few minutes).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
RACECAR_BRANCH="feature/poc/racecar"
RACECAR_COMMIT="ef9112895aca3057dd3a01b1731fc97ae0cdbe6e"
RACECAR_VERSION="${RACECAR_VERSION:-2.54.18-RACECAR-${RACECAR_COMMIT:0:12}}"
RACECAR_WORKTREE="${RACECAR_WORKTREE:-$HOME/.cache/v3-benchmarks/racecar-$RACECAR_COMMIT}"
JAVA_HOME="${BENCH_JAVA_HOME:-/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
# The clients of the finite fixtures the Racecar reference serves (awsJson1.0 DynamoDB, awsJson1.1 Kinesis, restJson1
# Lambda, awsQuery SNS, ec2Query EC2, rpcv2Cbor CloudWatch) and every transport a cell names, plus bom-internal: the
# core, services and http-clients parents import it, so a consumer cannot resolve the installed POMs without it, and
# -am does not install import-scoped BOMs.
MODULES="bom-internal,services/dynamodb,services/kinesis,services/lambda,services/sns,services/ec2,services/cloudwatch,http-clients/apache5-client,http-clients/apache-client,http-clients/url-connection-client,http-clients/netty-nio-client,http-clients/aws-crt-client"

DRY_RUN=false
if [[ "${1:-}" == "--dry-run" ]]; then
    DRY_RUN=true
fi

run() {
    echo "+ $*"
    if [[ "$DRY_RUN" == false ]]; then
        "$@"
    fi
}

if ! git -C "$REPO_DIR" cat-file -e "$RACECAR_COMMIT^{commit}" 2>/dev/null; then
    echo "error: pinned Racecar commit $RACECAR_COMMIT ($RACECAR_BRANCH) is not in $REPO_DIR; not fetching" >&2
    exit 1
fi
case "$RACECAR_WORKTREE" in
    "$REPO_DIR"|"$REPO_DIR"/*)
        echo "error: RACECAR_WORKTREE must be outside the main tree $REPO_DIR" >&2
        exit 1
        ;;
esac

if [[ -d "$RACECAR_WORKTREE" ]]; then
    head="$(git -C "$RACECAR_WORKTREE" rev-parse HEAD)"
    if [[ "$head" != "$RACECAR_COMMIT" ]]; then
        echo "error: existing worktree $RACECAR_WORKTREE is at $head, not $RACECAR_COMMIT" >&2
        exit 1
    fi
else
    run mkdir -p "$(dirname "$RACECAR_WORKTREE")"
    run git -C "$REPO_DIR" worktree add --detach "$RACECAR_WORKTREE" "$RACECAR_COMMIT"
fi

# versions:set only on unmodified POMs: re-running it over an already-set reactor rewrites the child modules'
# parent versions to 1.0. A previous run's POMs are reused when exactly that version was set; anything else fails.
if git -C "$RACECAR_WORKTREE" diff --quiet -- '*pom.xml'; then
    run mvn -o -B -ntp -f "$RACECAR_WORKTREE/pom.xml" versions:set -DnewVersion="$RACECAR_VERSION" \
        -DgenerateBackupPoms=false -DprocessAllModules=true
elif git -C "$RACECAR_WORKTREE" diff -U0 -- '*pom.xml' | grep '^+ ' | grep -qv "<version>$RACECAR_VERSION</version>"; then
    echo "error: $RACECAR_WORKTREE has POM changes other than the version $RACECAR_VERSION; restore its POMs" >&2
    exit 1
else
    echo "worktree POMs already at $RACECAR_VERSION; skipping versions:set"
fi
run mvn -o -B -ntp -f "$RACECAR_WORKTREE/pom.xml" install -pl "$MODULES" -am -DskipTests \
    -Dcheckstyle.skip -Dspotbugs.skip -Djapicmp.skip -Dmaven.javadoc.skip -Drat.skip

if [[ "$DRY_RUN" == false ]]; then
    {
        echo "commit=$RACECAR_COMMIT"
        echo "branch=$RACECAR_BRANCH"
        echo "version=$RACECAR_VERSION"
        echo "modules=$MODULES"
        echo "built=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
        echo "java=$("$JAVA_HOME/bin/java" -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.runtime.version/ {print $2}')"
    } > "$RACECAR_WORKTREE/racecar-reference.properties"
    echo "installed software.amazon.awssdk:*:$RACECAR_VERSION from $RACECAR_COMMIT ($RACECAR_WORKTREE)"
fi
