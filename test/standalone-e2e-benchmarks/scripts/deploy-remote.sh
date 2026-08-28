#!/usr/bin/env bash
# Push the harness scripts and one or more benchmark jars to a remote benchmark machine.
#
# The remote needs nothing but a JDK and async-profiler: the jars are self-contained, so there is no
# repo to clone, no Maven to configure and no ~/.m2 to keep in sync. That is the whole point of the
# jar-based flow — the only things that travel are the scripts and the artifacts under test.
#
# The layout created mirrors the repo just deeply enough that the scripts' relative paths resolve
# (each computes REPO as two levels above itself, and looks for the analysis scripts under
# pipeline_benchmark2/analysis/scripts):
#
#   <remote-dir>/repo/test/standalone-e2e-benchmarks/scripts/   harness scripts
#   <remote-dir>/repo/pipeline_benchmark2/analysis/scripts/     summarizers
#   <remote-dir>/repo/pipeline_benchmark2/jars/                 artifacts under test
#   <remote-dir>/repo/pipeline_benchmark2/{raw,paired,sweeps}/  results
#
# Usage: scripts/deploy-remote.sh --target USER@HOST --key PATH [options]
#   --target U@H     ssh destination (required)
#   --key PATH       ssh identity file (required)
#   --jar PATH       jar to upload; repeatable. Skipped if an identical size already exists remotely.
#   --remote-dir D   remote root (default: ~/racecar)
#   --scripts-only   don't upload jars, just refresh the scripts
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$DIR/../.." && pwd)"

TARGET=""
KEY=""
REMOTE_DIR="racecar"
SCRIPTS_ONLY=0
JARS=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        --target)       TARGET="$2"; shift 2 ;;
        --key)          KEY="$2"; shift 2 ;;
        --jar)          JARS+=("$2"); shift 2 ;;
        --remote-dir)   REMOTE_DIR="$2"; shift 2 ;;
        --scripts-only) SCRIPTS_ONLY=1; shift ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

if [[ -z "$TARGET" || -z "$KEY" ]]; then
    echo "usage: scripts/deploy-remote.sh --target USER@HOST --key PATH [--jar PATH]..." >&2
    exit 2
fi
if [[ ! -f "$KEY" ]]; then
    echo "error: key not found: $KEY" >&2
    exit 2
fi

SSH_OPTS=(-i "$KEY" -o BatchMode=yes -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15)
RBASE="$REMOTE_DIR/repo"
RBENCH="$RBASE/test/standalone-e2e-benchmarks"
RANALYSIS="$RBASE/pipeline_benchmark2/analysis/scripts"
RJARS="$RBASE/pipeline_benchmark2/jars"

echo "==> preparing $TARGET:$REMOTE_DIR"
ssh "${SSH_OPTS[@]}" "$TARGET" "mkdir -p '$RBENCH/scripts' '$RANALYSIS' '$RJARS' \
    '$RBASE/pipeline_benchmark2/raw' '$RBASE/pipeline_benchmark2/paired' \
    '$RBASE/pipeline_benchmark2/sweeps'"

echo "==> harness scripts"
# build-jar.sh is deliberately excluded: building requires the repo and Maven, and a jar built
# remotely would not be traceable to a commit the way the deployed ones are.
scp "${SSH_OPTS[@]}" -q \
    "$DIR/scripts/benchmark.sh" "$DIR/scripts/server.sh" "$DIR/scripts/collect.sh" \
    "$DIR/scripts/paired-ab.sh" "$DIR/scripts/concurrency-sweep.sh" \
    "$TARGET:$RBENCH/scripts/"
ssh "${SSH_OPTS[@]}" "$TARGET" "chmod +x '$RBENCH/scripts/'*.sh"

echo "==> analysis scripts"
scp "${SSH_OPTS[@]}" -q "$REPO/pipeline_benchmark2/analysis/scripts/"*.py "$TARGET:$RANALYSIS/"

if [[ $SCRIPTS_ONLY -eq 0 && ${#JARS[@]} -gt 0 ]]; then
    for jar in "${JARS[@]}"; do
        if [[ ! -f "$jar" ]]; then
            echo "error: jar not found: $jar" >&2
            exit 2
        fi
        name="$(basename "$jar")"
        local_size="$(wc -c < "$jar" | tr -d ' ')"
        # 2>/dev/null inside the remote shell too: without it, a not-yet-uploaded jar makes the
        # remote redirection itself complain before `wc` ever runs.
        remote_size="$(ssh "${SSH_OPTS[@]}" "$TARGET" \
            "{ wc -c < '$RJARS/$name' ; } 2>/dev/null || echo 0" | tr -d ' ')"
        if [[ "$local_size" == "$remote_size" ]]; then
            echo "==> $name already present ($(du -h "$jar" | cut -f1)), skipping"
            continue
        fi
        echo "==> uploading $name ($(du -h "$jar" | cut -f1))"
        scp "${SSH_OPTS[@]}" "$jar" "$TARGET:$RJARS/"
    done
fi

echo
echo "==> deployed"
ssh "${SSH_OPTS[@]}" "$TARGET" "
    echo '  scripts:'; ls '$RBENCH/scripts/' | sed 's/^/    /'
    echo '  jars:';    ls -la '$RJARS/' 2>/dev/null | tail -n +2 | awk '{print \"    \" \$5, \$9}'
"
echo
echo "Run remotely from $RBENCH, e.g.:"
echo "  cd $RBENCH && scripts/benchmark.sh --jar ../../pipeline_benchmark2/jars/<jar> \\"
echo "      --client v2-sync --scenario small-get --iterations 200000"
