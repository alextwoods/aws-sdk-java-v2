#!/usr/bin/env bash
#
# Before/after byte-diff oracle for codegen refactors (e.g. removing the IR).
#
# Generates v2 code for a set of services via the REAL codegen-maven-plugin, snapshots the generated
# tree, and diffs two snapshots. Because the build inputs (models, customization.config) and the
# generated output are both frozen contracts, an EMPTY diff is the acceptance test for any internal
# codegen change: same models in -> byte-identical code out.
#
# Usage:
#   codegen-diff.sh snapshot <label>          # build codegen, regenerate, save snapshot under <label>
#   codegen-diff.sh diff <labelA> <labelB>    # diff two saved snapshots
#   codegen-diff.sh baseline                  # shorthand: snapshot "baseline"
#   codegen-diff.sh check                     # shorthand: snapshot "candidate" + diff baseline candidate
#
# Env:
#   SERVICES="s3 dynamodb sqs ..."  services to generate (default: protocol-spanning sample below)
#   ALL=1                           generate ALL services that have codegen-resources (slow, full gate)
#   CODEGEN_FLAGS="-Dawssdk.codegen.legacyC2jIr=true"   extra -D flags for the regen (e.g. to snapshot
#                                   the legacy direct-C2J path for a legacy-vs-smithy generated-code diff)
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SNAP_DIR="$ROOT/codegen-c2j-to-smithy/target/codegen-snapshots"
MAVEN_OPTS_COMMON="-o -q -DskipTests -Dmdep.analyze.skip=true -Dcheckstyle.skip=true -Dspotbugs.skip=true -Dmaven.javadoc.skip=true"

# Protocol-spanning default sample: at least one service per wire protocol.
DEFAULT_SAMPLE="s3 dynamodb sqs cloudwatch polly route53"

pick_services() {
    if [[ "${ALL:-0}" == "1" ]]; then
        for d in "$ROOT"/services/*/src/main/resources/codegen-resources; do
            [[ -f "$d/service-2.json" ]] && basename "$(dirname "$(dirname "$(dirname "$(dirname "$d")")")")"
        done
    else
        echo "${SERVICES:-$DEFAULT_SAMPLE}"
    fi
}

build_codegen() {
    echo "==> Building + installing codegen + maven plugin (so regen uses current code)..."
    ( cd "$ROOT/codegen" && mvn $MAVEN_OPTS_COMMON -Djapicmp.skip=true install ) || return 1
    ( cd "$ROOT/codegen-c2j-to-smithy" && mvn $MAVEN_OPTS_COMMON install ) || return 1
    ( cd "$ROOT/codegen-maven-plugin" && mvn $MAVEN_OPTS_COMMON install ) || return 1
}

regen_service() {
    local svc="$1"
    local moddir="$ROOT/services/$svc"
    [[ -d "$moddir" ]] || { echo "  SKIP $svc (no module)"; return 0; }
    # Regenerate just this module's sources via the real plugin. CODEGEN_FLAGS lets the caller pick the
    # path (default: Smithy front-end; -Dawssdk.codegen.legacyC2jIr=true for the legacy direct path).
    ( cd "$moddir" && mvn $MAVEN_OPTS_COMMON ${CODEGEN_FLAGS:-} clean generate-sources ) >/dev/null 2>&1 \
        && echo "  ok   $svc ($(find "$moddir/target/generated-sources/sdk" -name '*.java' 2>/dev/null | wc -l | tr -d ' ') files)" \
        || echo "  FAIL $svc"
}

cmd_snapshot() {
    local label="$1"
    build_codegen || { echo "BUILD FAILED"; exit 1; }
    local dest="$SNAP_DIR/$label"
    rm -rf "$dest"; mkdir -p "$dest"
    echo "==> Regenerating + snapshotting to $label ..."
    for svc in $(pick_services); do
        regen_service "$svc"
        local gen="$ROOT/services/$svc/target/generated-sources/sdk"
        [[ -d "$gen" ]] && { mkdir -p "$dest/$svc"; cp -r "$gen" "$dest/$svc/"; }
    done
    echo "==> Snapshot '$label' saved: $(find "$dest" -name '*.java' | wc -l | tr -d ' ') java files."
}

cmd_diff() {
    local a="$1" b="$2"
    echo "==> diff $a vs $b"
    if diff -rq "$SNAP_DIR/$a" "$SNAP_DIR/$b" > "$SNAP_DIR/diff-$a-$b.txt" 2>&1; then
        echo "RESULT: IDENTICAL — generated code byte-for-byte unchanged."
        return 0
    fi
    local n; n=$(grep -c . "$SNAP_DIR/diff-$a-$b.txt")
    echo "RESULT: $n differing path(s). First 40:"
    head -40 "$SNAP_DIR/diff-$a-$b.txt"
    echo "(full list: $SNAP_DIR/diff-$a-$b.txt)"
    return 1
}

case "${1:-}" in
    snapshot) cmd_snapshot "${2:?label required}";;
    diff)     cmd_diff "${2:?labelA}" "${3:?labelB}";;
    baseline) cmd_snapshot "baseline";;
    check)    cmd_snapshot "candidate" && cmd_diff "baseline" "candidate";;
    *) echo "usage: codegen-diff.sh {snapshot <label>|diff <a> <b>|baseline|check}"; exit 2;;
esac
