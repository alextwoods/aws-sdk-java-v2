#!/usr/bin/env bash
# Compiles and runs LegacyDispositionTool, which regenerates test/smithy-protocol-tests/legacy-disposition.csv, against
# jars from the local Maven repository. Run from anywhere; paths resolve relative to this script. Pass
# --allow-unresolved to write the CSV while rows are still unresolved. The tool's tests, including the check that the
# committed CSV is up to date, run in the smithy-protocol-tests Maven build.
set -euo pipefail

TOOL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$TOOL_DIR/../../../.." && pwd)"
M2="${M2_REPO:-$HOME/.m2/repository}"
SMITHY="${SMITHY_VERSION:-1.71.0}"
OUT="$ROOT/test/smithy-protocol-tests/target/tool"

jar() { # group/path artifact version
  local f="$M2/$1/$2/$3/$2-$3.jar"
  [[ -f "$f" ]] || { echo "Missing $f (run a Maven build that resolves it first)" >&2; exit 1; }
  printf '%s' "$f"
}

CP=""
for a in smithy-model smithy-utils smithy-protocol-test-traits smithy-aws-traits smithy-aws-protocol-tests \
         smithy-protocol-tests smithy-validation-model smithy-protocol-traits smithy-rules-engine smithy-jmespath \
         smithy-model-jmespath; do
  CP="$CP:$(jar software/amazon/smithy "$a" "$SMITHY")"
done
CP="${CP#:}"

rm -rf "$OUT" && mkdir -p "$OUT/classes"
javac --release 8 -Xlint:-options -d "$OUT/classes" -cp "$CP" \
  $(find "$TOOL_DIR/java" -name '*.java')

exec java -cp "$CP:$OUT/classes" software.amazon.awssdk.protocol.smithytests.tool.LegacyDispositionTool \
  --root "$ROOT" "$@"
