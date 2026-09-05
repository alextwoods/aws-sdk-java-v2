#!/usr/bin/env bash
# Recapture the golden wire files from a STOCK (unbridged) S3 build, then restore the bridged build.
#
# The goldens have to come from a build with "generateSmithyJavaSerde" OFF for S3, and the test has to
# run against a build with it ON, so producing them means installing S3 twice. Doing that by hand is
# how you end up diffing a bridged build against itself and concluding everything is fine -- hence a
# script, and hence the verification after each install that the generated client really is (or is not)
# on the smithy path.
#
# Leaves ~/.m2 holding the BRIDGED S3, which is what the test needs. Safe to interrupt only between
# steps; if it dies midway, rerun it.
#
# Usage: scripts/capture-golden.sh
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$DIR/../.." && pwd)"
CONFIG="$REPO/services/s3/src/main/resources/codegen-resources/customization.config"
CLIENT="$REPO/services/s3/target/generated-sources/sdk/software/amazon/awssdk/services/s3/DefaultS3Client.java"
GOLDEN="$DIR/src/test/resources/golden"

# The system mvn on some hosts is too old to parse this repo's checkstyle config; the wrapper is 3.9.5.
MVN="$REPO/mvnw"
MVN_ARGS=(-P quick -Dpmd.skip=true -Dmaven.test.skip=true -q)

# "clean" is not optional: codegen refuses to overwrite a generated file whose content changed, so
# flipping the flag without cleaning fails with "Attempted to clobber existing file".
install_s3() {
    "$MVN" -f "$REPO/pom.xml" clean install -pl ':s3' "${MVN_ARGS[@]}"
}

# $1: "on" or "off" -- the expected state of the smithy path in the generated client.
assert_smithy_path() {
    local want="$1" count
    count="$(grep -c 'smithyClient' "$CLIENT" || true)"
    if [[ "$want" == "on" && "$count" -eq 0 ]]; then
        echo "expected a bridged client but DefaultS3Client has no smithyClient references" >&2
        exit 1
    fi
    if [[ "$want" == "off" && "$count" -ne 0 ]]; then
        echo "expected a stock client but DefaultS3Client has $count smithyClient references" >&2
        exit 1
    fi
}

flag_on() {
    python3 - "$CONFIG" <<'PY'
import sys, pathlib
p = pathlib.Path(sys.argv[1])
t = p.read_text()
if '"generateSmithyJavaSerde"' not in t:
    t = t.replace('{\n', '{\n  "generateSmithyJavaSerde": true,\n', 1)
    p.write_text(t)
PY
}

flag_off() {
    python3 - "$CONFIG" <<'PY'
import sys, pathlib
p = pathlib.Path(sys.argv[1])
t = p.read_text()
p.write_text(t.replace('  "generateSmithyJavaSerde": true,\n', '', 1))
PY
}

# Restore the bridged flag no matter how we exit, so an interrupted run does not leave the tree looking
# like a stock checkout -- a state that builds and tests fine and silently measures nothing.
trap flag_on EXIT

echo "==> installing STOCK s3"
flag_off
install_s3
assert_smithy_path off

echo "==> capturing goldens into $GOLDEN"
"$MVN" -f "$DIR/pom.xml" clean package -DskipTests -q
"$MVN" -f "$DIR/pom.xml" dependency:build-classpath -Dmdep.outputFile="$DIR/target/cp.txt" -q
java -cp "$DIR/target/classes:$(cat "$DIR/target/cp.txt")" \
     software.amazon.awssdk.wirediff.CaptureMain "$GOLDEN"

echo "==> restoring BRIDGED s3"
flag_on
install_s3
assert_smithy_path on

echo "done; goldens are from stock v2 and ~/.m2 holds the bridged build"
