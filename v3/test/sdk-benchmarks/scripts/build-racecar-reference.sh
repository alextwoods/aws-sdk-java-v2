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
#      2.54.18-RACECAR-<12-char commit>) with versions:set, offline.
#   4. Installs, offline and without tests, the DynamoDB client and the transports the harness cells name, with their
#      reactor dependencies (-am). The installed coordinates are software.amazon.awssdk:<artifact>:$RACECAR_VERSION.
#   5. Writes $RACECAR_WORKTREE/racecar-reference.properties (commit, version, artifacts, build time, JDK).
#
# This script does not make the Racecar reference READY: PerformanceReferences also needs a Racecar reference client
# main (a module like test/sdk-benchmarks-v2-reference built against these artifacts), which does not exist yet. Until
# then the reference stays artifact-missing / client-missing and a release-gate collection fails closed.
#
# The full -am build compiles a large part of the V2-derived reactor (codegen, core, services/dynamodb, the HTTP
# clients); expect tens of minutes on a laptop.
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
# The modules of the harness's awsJson1.0 cells: the DynamoDB client and every transport a cell names.
MODULES="services/dynamodb,http-clients/apache5-client,http-clients/apache-client,http-clients/url-connection-client,http-clients/netty-nio-client,http-clients/aws-crt-client"

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

run mvn -o -B -ntp -f "$RACECAR_WORKTREE/pom.xml" versions:set -DnewVersion="$RACECAR_VERSION" \
    -DgenerateBackupPoms=false -DprocessAllModules=true
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
