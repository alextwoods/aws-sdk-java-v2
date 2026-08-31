#!/usr/bin/env bash
# Build a self-contained, provenance-stamped benchmark jar and file it in the archive.
#
# The jar contains both SDKs, smithy-java, the mock server and the runner, so it is the complete
# unit of measurement: one file to scp to a benchmark host, and one file per phase to keep around
# for reruns. Two jars can be compared back-to-back without reinstalling Maven artifacts, which is
# what makes paired A/B measurement practical.
#
# Usage: scripts/build-jar.sh PHASE [--skip-sdk-build] [--sdk-commit SHA] [--archive DIR]
#
#   PHASE               label stamped into the jar and used in its filename, e.g. phaseA, baseline
#   --skip-sdk-build    don't rebuild/install the SDK modules first (use what's already in ~/.m2)
#   --sdk-commit SHA    record SHA as the commit the SDK in ~/.m2 was built from. Only meaningful
#                       with --skip-sdk-build; without it the SDK is built from HEAD and that is
#                       what gets recorded.
#   --sdk-version V     build against a PUBLISHED SDK v2 release from Maven Central (e.g. 2.54.0)
#                       instead of the local build. Implies --skip-sdk-build, and records the version
#                       in the provenance with sdk.commit=published-V. This is how you get a
#                       genuinely unmodified baseline: no local tree, no doubt about what is in it.
#   --archive DIR       archive location (default: <repo>/pipeline_benchmark2/jars)
#
# The SDK modules are rebuilt and installed by default, because the benchmark resolves the SDK from
# ~/.m2 at build time and baking a stale SDK into a phase-labelled jar is the single easiest way to
# produce a wrong measurement.
#
# Two commits are recorded, because they are not always the same one. A baseline jar is built by
# installing an older SDK and then shading it with today's harness — which is exactly what you want
# (comparing two jars is only sound if the harness is identical), but it means the harness commit
# does not describe the SDK bytes. Build such a jar with:
#
#   git checkout <baseline-sha>
#   ... install the SDK modules ...
#   git checkout <working-branch>
#   scripts/build-jar.sh phase0 --skip-sdk-build --sdk-commit <baseline-sha>
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
SDK_COMMIT=""
SDK_VERSION=""
ARCHIVE="$REPO/pipeline_benchmark2/jars"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-sdk-build) SKIP_SDK_BUILD=1; shift ;;
        --sdk-commit)     SDK_COMMIT="$2"; shift 2 ;;
        --sdk-version)    SDK_VERSION="$2"; SKIP_SDK_BUILD=1; shift 2 ;;
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

if [[ -n "$SDK_VERSION" ]]; then
    # A published release has no commit in this repo; say so rather than implying one.
    SDK_COMMIT="published-$SDK_VERSION"
    echo "==> Using PUBLISHED SDK v2 $SDK_VERSION from Maven Central (no local SDK involved)"
fi

if [[ $SKIP_SDK_BUILD -eq 0 ]]; then
    if [[ -n "$SDK_COMMIT" ]]; then
        echo "error: --sdk-commit only applies with --skip-sdk-build; without it the SDK is built" >&2
        echo "       from HEAD and that commit is recorded automatically." >&2
        exit 2
    fi
    # The SDK is about to be built from the working tree, so HEAD is a verified answer.
    SDK_COMMIT="$COMMIT"
    echo "==> Building and installing SDK modules (consistent set, excluding codegen-maven-plugin)"
    # codegen-maven-plugin is excluded from the reactor and resolved from ~/.m2 instead: its
    # descriptor goal fails under recent JDKs, and its sources are not what we are changing.
    # Installing a *consistent* set matters: installing a single core module on its own has
    # previously desynchronized ~/.m2 and produced VerifyErrors at runtime.
    (cd "$REPO" && mvn clean install \
        -pl ':dynamodb,:apache-client,:apache5-client,:aws-crt-client,!:codegen-maven-plugin' \
        --am -P quick -Dmaven.test.skip=true -q)
fi

SDK_COMMIT="${SDK_COMMIT:-unrecorded}"
if [[ "$SDK_COMMIT" == "unrecorded" ]]; then
    echo "NOTE: --skip-sdk-build without --sdk-commit; the SDK in ~/.m2 cannot be attributed to a" >&2
    echo "      commit, so the jar records sdk.commit=unrecorded." >&2
elif [[ "$SDK_COMMIT" != "$COMMIT" ]]; then
    echo "==> SDK is from a different revision than the harness: sdk=$SDK_COMMIT harness=$COMMIT"
fi

echo "==> Building benchmark jar (phase=$PHASE harness=$COMMIT sdk=$SDK_COMMIT)"
MVN_ARGS=(-q clean package -Dbenchmark.phase="$PHASE" -Dbenchmark.sdk.commit="$SDK_COMMIT")
if [[ -n "$SDK_VERSION" ]]; then
    MVN_ARGS+=(-Daws.sdk.v2.version="$SDK_VERSION")
fi
(cd "$DIR" && mvn "${MVN_ARGS[@]}")

BUILT="$DIR/target/racecar-$PHASE.jar"
if [[ ! -f "$BUILT" ]]; then
    echo "error: expected $BUILT to exist after package" >&2
    exit 1
fi

# The filename carries the SDK commit, since that is the variable under test when jars are compared;
# the harness commit is in the embedded provenance.
mkdir -p "$ARCHIVE"
ARCHIVED="$ARCHIVE/racecar-$PHASE-$SDK_COMMIT$SUFFIX.jar"
cp "$BUILT" "$ARCHIVED"

echo
echo "built:    $BUILT"
echo "archived: $ARCHIVED ($(du -h "$ARCHIVED" | cut -f1))"
echo "provenance:"
unzip -p "$ARCHIVED" benchmark-provenance.properties | grep -v '^#' | sed 's/^/  /'
echo
echo "Smoke test with:"
echo "  scripts/benchmark.sh --jar $ARCHIVED --client v2-sync --scenario small-get --iterations 300"
