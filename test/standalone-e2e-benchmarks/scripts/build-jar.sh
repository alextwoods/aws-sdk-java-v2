#!/usr/bin/env bash
# Build a self-contained, provenance-stamped benchmark jar and file it in the archive.
#
# The jar contains both SDKs, smithy-java, the mock server and the runner, so it is the complete
# unit of measurement: one file to scp to a benchmark host, and one file per phase to keep around
# for reruns. Two jars can be compared back-to-back without reinstalling Maven artifacts, which is
# what makes paired A/B measurement practical.
#
# Usage: scripts/build-jar.sh PHASE [--skip-sdk-build] [--archive DIR]
#
#   PHASE               label stamped into the jar and used in its filename, e.g. phaseA, baseline
#   --skip-sdk-build    don't rebuild/install the SDK modules first (use what's already in ~/.m2)
#   --archive DIR       archive location (default: <repo>/pipeline_benchmark2/jars)
#
# The SDK modules are rebuilt and installed by default, because the benchmark resolves the SDK from
# ~/.m2 at build time and baking a stale SDK into a phase-labelled jar is the single easiest way to
# produce a wrong measurement.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$DIR/../.." && pwd)"

PHASE="${1:-}"
if [[ -z "$PHASE" || "$PHASE" == --* ]]; then
    echo "usage: scripts/build-jar.sh PHASE [--skip-sdk-build] [--archive DIR]" >&2
    exit 2
fi
shift

SKIP_SDK_BUILD=0
ARCHIVE="$REPO/pipeline_benchmark2/jars"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-sdk-build) SKIP_SDK_BUILD=1; shift ;;
        --archive)        ARCHIVE="$2"; shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

COMMIT="$(git -C "$REPO" rev-parse --short=11 HEAD)"
# "dirty" means tracked files differ from the commit, i.e. the jar does not correspond to
# $COMMIT and is not reproducible from it. Untracked files are reported separately: scratch
# files at the repo root are normal and do not affect the build, but an untracked file under a
# module's src/ does get compiled in, so it is worth seeing.
DIRTY_COUNT="$(git -C "$REPO" status --porcelain --untracked-files=no | wc -l | tr -d ' ')"
SUFFIX=""
if [[ "$DIRTY_COUNT" != "0" ]]; then
    SUFFIX="-dirty"
    echo "WARNING: $DIRTY_COUNT tracked file(s) modified; jar will not be reproducible from $COMMIT." >&2
fi
UNTRACKED_SRC="$(git -C "$REPO" status --porcelain | awk '/^\?\?/ {print $2}' | grep -c '/src/' || true)"
if [[ "$UNTRACKED_SRC" != "0" ]]; then
    echo "NOTE: $UNTRACKED_SRC untracked file(s) under a module src/ directory; these are compiled in." >&2
fi

if [[ $SKIP_SDK_BUILD -eq 0 ]]; then
    echo "==> Building and installing SDK modules (consistent set, excluding codegen-maven-plugin)"
    # codegen-maven-plugin is excluded from the reactor and resolved from ~/.m2 instead: its
    # descriptor goal fails under recent JDKs, and its sources are not what we are changing.
    # Installing a *consistent* set matters: installing a single core module on its own has
    # previously desynchronized ~/.m2 and produced VerifyErrors at runtime.
    (cd "$REPO" && mvn clean install \
        -pl ':dynamodb,:apache-client,:apache5-client,:aws-crt-client,!:codegen-maven-plugin' \
        --am -P quick -Dmaven.test.skip=true -q)
fi

echo "==> Building benchmark jar (phase=$PHASE commit=$COMMIT)"
(cd "$DIR" && mvn -q clean package -Dbenchmark.phase="$PHASE")

BUILT="$DIR/target/racecar-$PHASE.jar"
if [[ ! -f "$BUILT" ]]; then
    echo "error: expected $BUILT to exist after package" >&2
    exit 1
fi

mkdir -p "$ARCHIVE"
ARCHIVED="$ARCHIVE/racecar-$PHASE-$COMMIT$SUFFIX.jar"
cp "$BUILT" "$ARCHIVED"

echo
echo "built:    $BUILT"
echo "archived: $ARCHIVED ($(du -h "$ARCHIVED" | cut -f1))"
echo "provenance:"
unzip -p "$ARCHIVED" benchmark-provenance.properties | grep -v '^#' | sed 's/^/  /'
echo
echo "Smoke test with:"
echo "  scripts/benchmark.sh --jar $ARCHIVED --client v2-sync --scenario small-get --iterations 300"
