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
# Drives the E2E DynamoDB harness experiments (scripts/schema-serde/optimization_experiment.sh and paired-ab.sh in
# jar mode) on a remote Linux benchmark host over ssh. The sibling of serde-ab-remote.sh, which drives the serde JMH
# A/B. The host needs no checkout and no Maven: the harness jars and JMH jars are built locally
# (optimization_experiment.sh --build-only) and deployed with the scripts.
#
# What this script does for the caller:
#   Queue. Every session runs through bench-session.sh: it waits while any benchmark runs on the host (any remote
#   dir) and then holds ~/bench.lock. See "Running on a remote metal host" in test/sdk-standard-benchmarks/README.md.
#   Detaching. The session runs under setsid nohup with all three streams redirected, and ssh itself with -n.
#   Snapshots. A session runs the scripts from its own copy of <remote-dir>/repo, taken when the session is
#   created, so a later deploy never changes what a queued or running session executes.
#   Immutable jars. A jar is stored under a name that contains its sha256 (e2e-<sdk sha12>-<sha256 12>.jar), is
#   uploaded to a temporary name, checked and then renamed, and is never overwritten.
#   Bounded waits. wait SECONDS returns after at most SECONDS, so a caller can poll under its own tool timeout.
# Run it with bash. Nothing it starts listens on a non-loopback address: the mock server binds 127.0.0.1 and
# JMH's runner-to-fork link is loopback. setup records the host's listening TCP sockets in host-info.txt.
#
# Remote layout (under the remote home):
#   <remote-dir>/repo/{scripts/schema-serde,test/standalone-e2e-benchmarks/scripts,docs/design/schema-serde}/...
#                                  the scripts and the two docs files at their repo paths
#   <remote-dir>/jars/e2e-<sdk sha12>-<sha256 12>.jar (+ .provenance.properties, .sha256)
#   <remote-dir>/jmh/<module>-<commit sha12>-<sha256 12>.jar (+ .commit, .sha256)
#   <remote-dir>/out/<name>/       experiment directories (<id>-<UTC stamp>) and other session output
#   <remote-dir>/sessions/<name>/{session.sh,session.log,state,repo/}
#   <remote-dir>/host-info.txt     (written by serde-ab-remote.sh setup)
#   ~/bench.lock, ~/bench.lock.holder   the host-wide session lock (bench-session.sh)
#
# Usage: scripts/schema-serde/e2e-remote.sh [options] COMMAND [ARGS]
#   setup [--if-needed]    provision the host through serde-ab-remote.sh setup (JDK 25, async-profiler 4.4, python3,
#                          the non-persistent sysctls perf_event_paranoid=1 and kptr_restrict=0, host-info.txt) and
#                          create the layout. --if-needed returns at once when the sysctls, async-profiler 4.4,
#                          JDK 25 and the layout are already in place (run it before every deploy: it catches a
#                          reboot). Refuses while a benchmark runs on the host
#   deploy [--jar PATH]... [--jmh-jar PATH]...
#                          refresh the scripts and docs files; upload each harness jar (its provenance is read with
#                          unzip -p) and each JMH jar (PATH.commit is required: commit=<sha>, module=<module>, as
#                          optimization_experiment.sh --build-only writes it) under its content name, skipped when
#                          already there; print the remote name of each jar; then run paired_ab_summary.py,
#                          optimization_experiment.py and bench-session.sh --self-test on the host under
#                          taskset -c 48-63 nice -n 19 (off the benchmark cores)
#   experiment ID --feature-jar NAME --candidate-jar NAME [--feature-jmh-jar NAME --candidate-jmh-jar NAME]
#              [--null-run DIR] [--jmh-first] [--feature-ref LABEL] [--candidate-ref LABEL] [-- OPTION...]
#                          start a session that runs optimization_experiment.sh in jar mode with the canonical
#                          settings (--pin-client 32-47 --pin-server 0-15, TUNE as --jvm-args, --server-jvm-args
#                          and --jmh-jvm-args, --jmh-pin 32-47, --skip-build) into out/ID-<stamp>, with the record
#                          out/ID-<stamp>/ID.md, and prints EXPERIMENT_EXIT=<status> at the end. NAME is a deployed
#                          jar name; DIR is a host path (a paired-ab run directory of an earlier null experiment).
#                          OPTIONs after -- are appended (they win over the canonical ones). Labels default to
#                          perf/racecar-e2e and cand/ID. Prints the session name
#   start SCRIPT           run a local bash script as a session (cwd: the session directory, with the repo
#                          snapshot in ./repo); it queues instead of refusing. Prints the session name
#   status [SESSION]       the session's state (queued with the blocking commands, running, done exit=N, stopped)
#                          and whether its processes are alive; host-wide benchmark processes, the lock holder,
#                          load; the newest out dirs and sessions. SESSION defaults to the newest session
#   log [N] [SESSION]      the last N lines (default 40) of the session log
#   wait SECONDS [SESSION] poll every 60 s until the session is done, for at most SECONDS
#   fetch NAME|latest      copy out/NAME (without jars) and the sessions that name it into DEST/NAME/, with
#                          host-info.txt
#   stop SESSION           terminate that session's processes only (its setsid session); never other sessions
#   --self-test            local checks of the session script this script writes (no host)
# Options:
#   --target USER@HOST     ssh destination (default: $E2E_REMOTE_TARGET, else the task's metal host)
#   --key PATH             ssh identity file (default: $E2E_REMOTE_KEY, else the task's key)
#   --remote-dir DIR       remote root under the remote home (default e2e-racecar; serde-ab* is refused, those
#                          belong to serde-ab-remote.sh)
#   --dest DIR             fetch destination (default: <main checkout>/.agents/tasks/racecar-opt-e2e/runs)
#   -h, --help             this text
#
# Exit status: 0 success (wait: the session is done; status: always 0 when the host answers), 1 a remote step
# failed, setup refused, or wait timed out with the session still queued or running, 2 usage error, 4 the host
# cannot be reached (three attempts), 5 wait: the session ended without finishing (stopped, or its processes are
# gone without a done state, e.g. after a host restart).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TARGET="${E2E_REMOTE_TARGET:-ec2-user@ec2-52-23-247-58.compute-1.amazonaws.com}"
KEY="${E2E_REMOTE_KEY:-/Users/alexwoo/alexwoo-ec2-us-east-1.pem}"
REMOTE_DIR="e2e-racecar"
DEST=""
TUNE="-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4"
# What deploy copies, as repo-relative paths (the scripts resolve each other by these paths).
REPO_FILES=(scripts/schema-serde/paired-ab.sh scripts/schema-serde/paired_ab_summary.py
            scripts/schema-serde/optimization_experiment.sh scripts/schema-serde/optimization_experiment.py
            scripts/schema-serde/racecar_decisions.py scripts/schema-serde/hot_path_check.sh
            scripts/schema-serde/hot_path_frames.py scripts/schema-serde/bench-session.sh
            test/standalone-e2e-benchmarks/scripts/benchmark.sh test/standalone-e2e-benchmarks/scripts/server.sh
            docs/design/schema-serde/optimizations/targets.json docs/design/schema-serde/racecar-decisions.yaml)
OPTIONAL_REPO_FILES=(scripts/schema-serde/e2e-final-session.sh)
PLAIN='^[A-Za-z0-9][A-Za-z0-9._-]*$'
usage() { sed -n '/^# Usage:/,/^set -euo pipefail/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; }
fail_usage() { echo "$1" >&2; exit 2; }
need() { [[ $# -ge 2 ]] || fail_usage "$1 needs a value"; }
utc_stamp() { date -u +%Y%m%d-%H%M%S; }
sha256_of() { if command -v sha256sum > /dev/null; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi; }
q() { printf '%q' "$1"; }

# ==================== session scripts ====================

# Writes the session script of one experiment. Every value is shell-quoted with %q.
# <file> <id> <out name> <feature ref> <candidate ref> <feature jar> <candidate jar> <feature jmh> <candidate jmh>
# <null run> <jmh first 0|1> [extra option...]
write_experiment_session() {
    local file="$1" id="$2" name="$3" fref="$4" cref="$5" fjar="$6" cjar="$7" fjmh="$8" cjmh="$9"
    local null_run="${10}" jmh_first="${11}"
    shift 11
    local args=("$id" "$cref" --feature-ref "$fref")
    {
        echo '#!/usr/bin/env bash'
        echo "# Experiment $id, written by e2e-remote.sh experiment at $(date -u +%Y-%m-%dT%H:%M:%SZ)."
        echo 'set -uo pipefail'
        echo "R=\"\$HOME\"/$(q "$REMOTE_DIR")"
        echo "TUNE=$(q "$TUNE")"
        echo "OUT=\"\$R/out\"/$(q "$name")"
        echo 'mkdir -p "$OUT"'
        echo "echo \"experiment $(q "$id") starting at \$(date -u +%Y-%m-%dT%H:%M:%SZ) in \$OUT\""
        echo 'status=0'
        printf 'bash ./repo/scripts/schema-serde/optimization_experiment.sh'
        printf ' %q' "${args[@]}"
        printf ' \\\n    --feature-jar "$R/jars"/%q --candidate-jar "$R/jars"/%q' "$fjar" "$cjar"
        [[ -n "$fjmh" ]] && printf ' \\\n    --feature-jmh-jar "$R/jmh"/%q' "$fjmh"
        [[ -n "$cjmh" ]] && printf ' --candidate-jmh-jar "$R/jmh"/%q' "$cjmh"
        printf ' \\\n    --pin-client 32-47 --pin-server 0-15 --jvm-args "$TUNE" --server-jvm-args "$TUNE"'
        printf ' \\\n    --jmh-pin 32-47 --jmh-jvm-args "$TUNE" --skip-build'
        printf ' \\\n    --out "$OUT" --record "$OUT"/%q' "$id.md"
        [[ -n "$null_run" ]] && printf ' \\\n    --null-run %q' "$null_run"
        [[ "$jmh_first" == 1 ]] && printf ' --jmh-first'
        if [[ $# -gt 0 ]]; then
            printf ' \\\n   '
            printf ' %q' "$@"
        fi
        printf ' || status=$?\n'
        echo 'echo "EXPERIMENT_EXIT=$status"'
        echo "echo \"experiment $(q "$id") finished at \$(date -u +%Y-%m-%dT%H:%M:%SZ)\""
        echo 'exit "$status"'
    } > "$file"
}

self_test() {
    set +e  # every check reports its own status
    local tmp failures=0
    tmp="$(mktemp -d "${TMPDIR:-/tmp}/e2e-remote-test.XXXXXXXX")"
    check() { if [[ "$2" -eq 0 ]]; then echo "  ok: $1"; else echo "  FAIL: $1"; failures=$((failures + 1)); fi; }
    write_experiment_session "$tmp/session.sh" "SJ-298b5d799" "SJ-298b5d799-20261024-170000" "perf/racecar-e2e" \
        "cand/SJ-298b5d799" "e2e-aaaaaaaaaaaa-111111111111.jar" "e2e-bbbbbbbbbbbb-222222222222.jar" \
        "sdk-benchmarks-aaaaaaaaaaaa-333333333333.jar" "sdk-benchmarks-bbbbbbbbbbbb-444444444444.jar" \
        "/home/ec2-user/e2e-racecar/out/B1-20261024-150000/20261024-150001" 1 \
        --preliminary --jmh-opts "-wi 1 -i 1 -f 1 -w 1s -r 1s" --clients 'v2-sync' --jvm-args "-Da='b c'"
    bash -n "$tmp/session.sh"
    check "the session script parses" $?
    # Run it against a stub optimization_experiment.sh that prints its argv, one argument per line.
    mkdir -p "$tmp/home" "$tmp/s/repo/scripts/schema-serde"
    printf 'printf "%%s\\n" "$@"; exit 3\n' > "$tmp/s/repo/scripts/schema-serde/optimization_experiment.sh"
    (cd "$tmp/s" && HOME="$tmp/home" bash "$tmp/session.sh") > "$tmp/argv.txt" 2>&1
    check "the session exits with the experiment's status 3" $(( $? == 3 ? 0 : 1 ))
    local R="$tmp/home/$REMOTE_DIR" expected
    expected="$(printf '%s\n' SJ-298b5d799 cand/SJ-298b5d799 --feature-ref perf/racecar-e2e \
        --feature-jar "$R/jars/e2e-aaaaaaaaaaaa-111111111111.jar" \
        --candidate-jar "$R/jars/e2e-bbbbbbbbbbbb-222222222222.jar" \
        --feature-jmh-jar "$R/jmh/sdk-benchmarks-aaaaaaaaaaaa-333333333333.jar" \
        --candidate-jmh-jar "$R/jmh/sdk-benchmarks-bbbbbbbbbbbb-444444444444.jar" \
        --pin-client 32-47 --pin-server 0-15 --jvm-args "$TUNE" --server-jvm-args "$TUNE" \
        --jmh-pin 32-47 --jmh-jvm-args "$TUNE" --skip-build \
        --out "$R/out/SJ-298b5d799-20261024-170000" --record "$R/out/SJ-298b5d799-20261024-170000/SJ-298b5d799.md" \
        --null-run /home/ec2-user/e2e-racecar/out/B1-20261024-150000/20261024-150001 --jmh-first \
        --preliminary --jmh-opts "-wi 1 -i 1 -f 1 -w 1s -r 1s" --clients v2-sync --jvm-args "-Da='b c'")"
    diff <(sed -n '/^experiment .* starting/,/^EXPERIMENT_EXIT=/p' "$tmp/argv.txt" | sed '1d;$d') \
        <(printf '%s\n' "$expected") > "$tmp/diff.txt"
    check "the experiment gets every argument intact, in order" $?
    [[ -s "$tmp/diff.txt" ]] && sed 's/^/    /' "$tmp/diff.txt"
    grep -q '^EXPERIMENT_EXIT=3$' "$tmp/argv.txt"
    check "the session prints EXPERIMENT_EXIT" $?
    [[ -d "$R/out/SJ-298b5d799-20261024-170000" ]]
    check "the session creates its out directory" $?
    rm -rf "$tmp"
    if [[ $failures -eq 0 ]]; then echo "PASS"; return 0; fi
    echo "FAILED: $failures"
    return 1
}

# ==================== arguments ====================

CMD=""
ARGS=()
while [[ $# -gt 0 ]]; do
    if [[ -n "$CMD" ]]; then ARGS+=("$1"); shift; continue; fi
    case "$1" in
        --target) need "$@"; TARGET="$2"; shift 2 ;;
        --key) need "$@"; KEY="$2"; shift 2 ;;
        --remote-dir) need "$@"; REMOTE_DIR="$2"; shift 2 ;;
        --dest) need "$@"; DEST="$2"; shift 2 ;;
        --self-test) self_test; exit $? ;;
        -h|--help) usage; exit 0 ;;
        -*) fail_usage "Unknown option: $1 (see --help)" ;;
        *) CMD="$1"; shift ;;
    esac
done
[[ -n "$CMD" ]] || { usage >&2; exit 2; }
case "$CMD" in
    setup|deploy|experiment|start|status|log|wait|fetch|stop) ;;
    *) fail_usage "Unknown command: $CMD (see --help)" ;;
esac
# The target and the remote dir are interpolated into ssh arguments and remote commands: allow plain names only.
[[ "$TARGET" =~ ^([A-Za-z0-9._-]+@)?[A-Za-z0-9][A-Za-z0-9.-]*$ ]] || fail_usage "--target must be USER@HOST or HOST, got '$TARGET'"
[[ "$REMOTE_DIR" =~ $PLAIN ]] || fail_usage "--remote-dir must be one plain directory name under the remote home"
[[ "$REMOTE_DIR" != serde-ab* ]] || fail_usage "--remote-dir $REMOTE_DIR: serde-ab* directories belong to serde-ab-remote.sh"
KEY="${KEY/#\~/$HOME}"
[[ -f "$KEY" ]] || fail_usage "Key not found: $KEY"
SSH_OPTS=(-i "$KEY" -o BatchMode=yes -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15
          -o ServerAliveInterval=30 -o ServerAliveCountMax=6)
R="$REMOTE_DIR"   # relative to the remote home
R_RE="${R//./[.]}"
WRAPPER="$R/repo/scripts/schema-serde/bench-session.sh"

rssh() { ssh "${SSH_OPTS[@]}" "$TARGET" "$@"; }
# A remote command that reads this script's stdin. Not retried (the input is consumed); exits 4 when ssh fails.
rssh_in() {
    local status=0
    rssh "$@" || status=$?
    if [[ $status -eq 255 ]]; then echo "ssh to $TARGET failed (exit 255)" >&2; exit 4; fi
    return $status
}
# Runs an idempotent remote command (no stdin) up to three times while ssh itself fails (exit 255); exits 4 when
# the host cannot be reached.
rssh_retry() {
    local attempt status
    for attempt in 1 2 3; do
        status=0
        ssh -n "${SSH_OPTS[@]}" "$TARGET" "$@" || status=$?
        [[ $status -ne 255 ]] && return $status
        echo "Cannot reach $TARGET (ssh exit 255; attempt $attempt of 3)" >&2
        [[ $attempt -lt 3 ]] && sleep 10
    done
    echo "The host $TARGET cannot be reached" >&2
    exit 4
}
# 0: a benchmark process runs anywhere on the host (bench-session.sh --busy, sent over stdin so it needs no deploy);
# prints the processes. 1: none runs. Call it as: busy_status=0; busy="$(remote_busy)" || busy_status=$?  (4: the
# host cannot be asked). An answer without the BUSY/IDLE header is an error, never "idle".
remote_busy() {
    local attempt status out
    for attempt in 1 2 3; do
        status=0
        out="$(rssh "bash -s -- --busy" < "$SCRIPT_DIR/bench-session.sh" 2>&1)" || status=$?
        if [[ $status -eq 0 && "$out" == BUSY* ]]; then printf '%s\n' "${out#BUSY$'\n'}"; return 0; fi
        if [[ $status -eq 1 && "$out" == IDLE ]]; then return 1; fi
        echo "Cannot ask $TARGET whether a benchmark runs (exit $status: ${out:0:200}; attempt $attempt of 3)" >&2
        [[ $attempt -lt 3 ]] && sleep 10
    done
    exit 4
}
session_arg() {  # [SESSION] -> the named session, or the newest one in this remote dir
    local name="${1:-}"
    if [[ -z "$name" ]]; then
        name="$(rssh_retry "ls -1t '$R/sessions' 2>/dev/null | head -1" | tr -d '\r')"
        [[ -n "$name" ]] || { echo "No sessions in $TARGET:$R/sessions" >&2; exit 1; }
    fi
    [[ "$name" =~ $PLAIN ]] || fail_usage "Session name must be plain, got '$name'"
    printf '%s\n' "$name"
}
# "<alive 0|1><TAB><state>" of one session. Liveness is read before the state, so a session that finishes in
# between reads as not alive with its final state.
session_probe() {
    local name="$1" name_re="${1//./[.]}"
    rssh_retry "if pgrep -f '$R_RE/session[s]/$name_re/session[.]sh' > /dev/null 2>&1; then a=1; else a=0; fi
                if [ -d '$R/sessions/$name' ]; then s=\$(head -c 400 '$R/sessions/$name/state' 2>/dev/null || echo 'no state yet'); else s=missing; fi
                printf '%s\t%s\n' \"\$a\" \"\$s\""
}
# Ships a local script as sessions/<name>/session.sh with a snapshot of repo/, and starts it through the wrapper.
start_session() {  # <local script> <name>
    local script="$1" name="$2" dir="$R/sessions/$2"
    rssh_retry "test -f '$WRAPPER'" || fail_usage "$TARGET:$WRAPPER is missing; run deploy first"
    rssh_retry "test ! -e '$dir'" || fail_usage "Session $name already exists on $TARGET"
    rssh_retry "mkdir -p '$dir' && cp -a '$R/repo' '$dir/repo'"
    rssh_in "cat > '$dir/session.sh'" < "$script"
    # All three streams of the job and of ssh redirected, or ssh waits for the job; the background job must be the
    # redirected command alone (see serde-ab-remote.sh).
    ssh "${SSH_OPTS[@]}" -n "$TARGET" \
        "cd \"\$HOME/$dir\" || exit 1; setsid nohup bash \"\$HOME/$WRAPPER\" \"\$HOME/$dir/session.sh\" > session.log 2>&1 < /dev/null &" \
        > /dev/null 2>&1 < /dev/null || { echo "Could not start session $name on $TARGET" >&2; exit 1; }
    sleep 3
    local probe
    probe="$(session_probe "$name")"
    echo "Started session $name on $TARGET (~/$dir); state: ${probe#*$'\t'}"
    [[ "${probe%%$'\t'*}" == 1 ]] || echo "WARNING: the session's processes are gone; see: e2e-remote.sh log 40 $name" >&2
    echo "SESSION=$name"
}

case "$CMD" in
    setup)
        IF_NEEDED=0
        for a in ${ARGS[@]+"${ARGS[@]}"}; do
            case "$a" in --if-needed) IF_NEEDED=1 ;; *) fail_usage "setup takes --if-needed only" ;; esac
        done
        READY="test \"\$(cat /proc/sys/kernel/perf_event_paranoid)\" = 1 && test \"\$(cat /proc/sys/kernel/kptr_restrict)\" = 0 \
               && /opt/async-profiler/bin/asprof --version 2>/dev/null | grep -q 'Async-profiler 4[.]4 ' \
               && java -version 2>&1 | grep -q 'version \"25' && command -v flock > /dev/null && command -v unzip > /dev/null \
               && test -d '$R/repo/scripts/schema-serde' && test -d '$R/jars' && test -d '$R/jmh' && test -d '$R/sessions'"
        if [[ $IF_NEEDED -eq 1 ]] && rssh_retry "$READY"; then
            echo "$TARGET is ready (sysctls, async-profiler 4.4, JDK 25, flock, unzip, ~/$R layout)"
            exit 0
        fi
        busy_status=0
        busy="$(remote_busy)" || busy_status=$?
        [[ $busy_status -ne 4 ]] || exit 4
        if [[ $busy_status -eq 0 ]]; then
            echo "Refusing to provision $TARGET while benchmarks run:" >&2
            printf '%s\n' "$busy" | sed 's/^/  /' >&2
            exit 1
        fi
        bash "$SCRIPT_DIR/serde-ab-remote.sh" setup --target "$TARGET" --key "$KEY" --remote-dir "$R"
        rssh_retry "sudo dnf install -y -q unzip util-linux > /dev/null && mkdir -p '$R/repo' '$R/jars' '$R/jmh' '$R/out' '$R/sessions'"
        rssh_retry "$READY" || { echo "Provisioned, but $TARGET is still not ready (see the output above)" >&2; exit 1; }
        echo "$TARGET is ready"
        ;;
    deploy)
        E2E_JARS=()
        JMH_JARS=()
        set -- ${ARGS[@]+"${ARGS[@]}"}
        while [[ $# -gt 0 ]]; do
            case "$1" in
                --jar) need "$@"; E2E_JARS+=("$2"); shift 2 ;;
                --jmh-jar) need "$@"; JMH_JARS+=("$2"); shift 2 ;;
                *) fail_usage "deploy takes --jar PATH and --jmh-jar PATH options only" ;;
            esac
        done
        files=()
        for f in "${REPO_FILES[@]}"; do
            [[ -f "$REPO_ROOT/$f" ]] || fail_usage "Missing $REPO_ROOT/$f"
            files+=("$f")
        done
        for f in "${OPTIONAL_REPO_FILES[@]}"; do
            if [[ -f "$REPO_ROOT/$f" ]]; then files+=("$f"); fi
        done
        # Remote name and sidecar contents of each jar, computed before anything is uploaded.
        UPLOADS=()   # "<local path>\t<remote subdir>\t<remote name>\t<sha256>\t<sidecar suffix>\t<sidecar file>"
        sidecars="$(mktemp -d "${TMPDIR:-/tmp}/e2e-remote-deploy.XXXXXXXX")"
        trap 'rm -rf "$sidecars"' EXIT
        for jar in ${E2E_JARS[@]+"${E2E_JARS[@]}"}; do
            [[ -f "$jar" ]] || fail_usage "Jar not found: $jar"
            unzip -p "$jar" benchmark-provenance.properties > "$sidecars/p" 2>/dev/null \
                || fail_usage "$jar has no benchmark-provenance.properties (not a harness jar)"
            sdk="$(sed -n 's/^sdk.commit=//p' "$sidecars/p" | head -1)"
            [[ "$sdk" =~ ^[0-9a-f]{40}$ ]] || fail_usage "$jar: provenance sdk.commit is '$sdk', not a commit"
            sha="$(sha256_of "$jar")"
            name="e2e-${sdk:0:12}-${sha:0:12}.jar"
            mv "$sidecars/p" "$sidecars/$name.provenance.properties"
            UPLOADS+=("$jar"$'\t'jars$'\t'"$name"$'\t'"$sha"$'\t'.provenance.properties$'\t'"$sidecars/$name.provenance.properties")
        done
        for jar in ${JMH_JARS[@]+"${JMH_JARS[@]}"}; do
            [[ -f "$jar" ]] || fail_usage "Jar not found: $jar"
            [[ -f "$jar.commit" ]] || fail_usage "$jar has no $jar.commit (commit=<sha>, module=<module>)"
            commit="$(sed -n 's/^commit=//p' "$jar.commit" | head -1)"
            module="$(sed -n 's/^module=//p' "$jar.commit" | head -1)"
            [[ "$commit" =~ ^[0-9a-f]{40}$ ]] || fail_usage "$jar.commit: commit is '$commit', not a commit"
            [[ "$module" =~ $PLAIN ]] || fail_usage "$jar.commit: module is '$module'"
            sha="$(sha256_of "$jar")"
            name="$module-${commit:0:12}-${sha:0:12}.jar"
            cp "$jar.commit" "$sidecars/$name.commit"
            UPLOADS+=("$jar"$'\t'jmh$'\t'"$name"$'\t'"$sha"$'\t'.commit$'\t'"$sidecars/$name.commit")
        done
        echo "Deploying ${#files[@]} files to $TARGET:$R/repo/"
        # tar replaces each file with a new inode (it unlinks before extracting), so a bash process still reading
        # an old copy is not affected; sessions run from their own snapshot anyway.
        (cd "$REPO_ROOT" && COPYFILE_DISABLE=1 tar -cf - "${files[@]}") \
            | rssh_in "mkdir -p '$R/repo' '$R/jars' '$R/jmh' '$R/out' '$R/sessions' && tar -C '$R/repo' -xf - \
                          && chmod +x '$R/repo/scripts/schema-serde/'*.sh '$R/repo/test/standalone-e2e-benchmarks/scripts/'*.sh"
        for entry in ${UPLOADS[@]+"${UPLOADS[@]}"}; do
            IFS=$'\t' read -r jar sub name sha suffix sidecar <<< "$entry"
            remote="$R/$sub/$name"
            remote_sha="$(rssh_retry "sha256sum '$remote' 2>/dev/null | cut -d' ' -f1" || true)"
            if [[ "$remote_sha" == "$sha" ]]; then
                echo "$jar -> $sub/$name (already on the host)"
            else
                [[ -z "$remote_sha" ]] || echo "$sub/$name on the host has sha256 $remote_sha; replacing it" >&2
                echo "$jar -> $sub/$name (uploading $(du -h "$jar" | cut -f1))"
                scp "${SSH_OPTS[@]}" -q "$jar" "$TARGET:$R/$sub/.$name.part"
                remote_sha="$(rssh_retry "sha256sum '$R/$sub/.$name.part' | cut -d' ' -f1")"
                if [[ "$remote_sha" != "$sha" ]]; then
                    rssh_retry "rm -f '$R/$sub/.$name.part'"
                    echo "$name: sha256 $remote_sha after upload, expected $sha" >&2
                    exit 1
                fi
                rssh_retry "mv -f '$R/$sub/.$name.part' '$remote'"
            fi
            rssh_in "cat > '$remote$suffix' && echo '$sha' > '$remote.sha256'" < "$sidecar"
        done
        echo "Self-tests on the host (off the benchmark cores)"
        rssh_retry "cd '$R/repo/scripts/schema-serde' || exit 1
            if [ \"\$(nproc --all)\" -ge 64 ]; then pin='taskset -c 48-63'; else pin=''; fi
            for t in 'python3 paired_ab_summary.py --self-test' 'python3 optimization_experiment.py --self-test' \
                     'bash bench-session.sh --self-test'; do
                log=\"/tmp/e2e-remote-selftest-\$(echo \"\$t\" | tr -c 'A-Za-z0-9' '_').log\"
                if PYTHONDONTWRITEBYTECODE=1 \$pin nice -n 19 \$t > \"\$log\" 2>&1; then
                    echo \"  \$t: OK (\$(tail -1 \"\$log\"))\"
                else
                    echo \"  \$t: FAILED\"; tail -30 \"\$log\"; exit 1
                fi
            done"
        echo "Deployed. Jars on the host:"
        rssh_retry "cd '$R' && ls -l jars/*.jar jmh/*.jar 2>/dev/null | awk '{ print \"  \" \$5, \$9 }'; true"
        ;;
    experiment)
        [[ ${#ARGS[@]} -ge 1 ]] || fail_usage "experiment needs an ID"
        ID="${ARGS[0]}"
        [[ "$ID" =~ $PLAIN ]] || fail_usage "ID must be plain, got '$ID'"
        FJAR="" CJAR="" FJMH="" CJMH="" NULL_RUN="" JMH_FIRST=0 FREF="perf/racecar-e2e" CREF="cand/$ID"
        EXTRA=()
        set -- "${ARGS[@]:1}"
        while [[ $# -gt 0 ]]; do
            case "$1" in
                --feature-jar) need "$@"; FJAR="$2"; shift 2 ;;
                --candidate-jar) need "$@"; CJAR="$2"; shift 2 ;;
                --feature-jmh-jar) need "$@"; FJMH="$2"; shift 2 ;;
                --candidate-jmh-jar) need "$@"; CJMH="$2"; shift 2 ;;
                --null-run) need "$@"; NULL_RUN="$2"; shift 2 ;;
                --jmh-first) JMH_FIRST=1; shift ;;
                --feature-ref) need "$@"; FREF="$2"; shift 2 ;;
                --candidate-ref) need "$@"; CREF="$2"; shift 2 ;;
                --) shift; EXTRA=("$@"); break ;;
                *) fail_usage "experiment: unknown option $1 (pass experiment options after --)" ;;
            esac
        done
        [[ -n "$FJAR" && -n "$CJAR" ]] || fail_usage "experiment needs --feature-jar NAME and --candidate-jar NAME"
        [[ -z "$FJMH" && -z "$CJMH" || -n "$FJMH" && -n "$CJMH" ]] \
            || fail_usage "pass both --feature-jmh-jar and --candidate-jmh-jar, or neither"
        checks="test -f '$R/jars/$FJAR' || echo 'missing jars/$FJAR'; test -f '$R/jars/$CJAR' || echo 'missing jars/$CJAR'"
        for n in "$FJAR" "$CJAR" ${FJMH:+"$FJMH"} ${CJMH:+"$CJMH"}; do
            [[ "$n" =~ $PLAIN && "$n" == *.jar ]] || fail_usage "Jar names are deployed file names (*.jar), got '$n'"
        done
        [[ -z "$FJMH" ]] || checks+="; test -f '$R/jmh/$FJMH' || echo 'missing jmh/$FJMH'; test -f '$R/jmh/$CJMH' || echo 'missing jmh/$CJMH'"
        if [[ -n "$NULL_RUN" ]]; then
            [[ "$NULL_RUN" =~ ^[A-Za-z0-9_/.-]+$ && "$NULL_RUN" != *..* ]] || fail_usage "--null-run must be a plain host path"
            checks+="; test -f '$NULL_RUN/paired-ab.json' || echo 'missing $NULL_RUN/paired-ab.json'"
        fi
        missing="$(rssh_retry "$checks")"
        [[ -z "$missing" ]] || { printf '%s\n' "$missing" >&2; echo "Deploy the jars first (see deploy's output for their names)" >&2; exit 2; }
        name="$ID-$(utc_stamp)"
        script="$(mktemp -d "${TMPDIR:-/tmp}/e2e-remote-exp.XXXXXXXX")/session.sh"
        write_experiment_session "$script" "$ID" "$name" "$FREF" "$CREF" "$FJAR" "$CJAR" "$FJMH" "$CJMH" \
            "$NULL_RUN" "$JMH_FIRST" ${EXTRA[@]+"${EXTRA[@]}"}
        bash -n "$script"
        start_session "$script" "$name"
        echo "Out directory: ~/$R/out/$name"
        rm -rf "$(dirname "$script")"
        ;;
    start)
        [[ ${#ARGS[@]} -eq 1 ]] || fail_usage "start needs one local script"
        script="${ARGS[0]}"
        [[ -f "$script" ]] || fail_usage "Script not found: $script"
        bash -n "$script" || fail_usage "$script does not parse"
        base="$(basename "$script" .sh)"
        [[ "$base" =~ $PLAIN ]] || fail_usage "Script file name must be plain: $base"
        start_session "$script" "$base-$(utc_stamp)"
        ;;
    status)
        [[ ${#ARGS[@]} -le 1 ]] || fail_usage "status takes at most a session name"
        name="${ARGS[0]:-}"
        [[ -n "$name" ]] || name="$(rssh_retry "ls -1t '$R/sessions' 2>/dev/null | head -1" | tr -d '\r')"
        if [[ -n "$name" ]]; then
            name="$(session_arg "$name")"
            probe="$(session_probe "$name")"
            alive="${probe%%$'\t'*}"
            echo "session $name: ${probe#*$'\t'} ($([[ "$alive" == 1 ]] && echo "processes alive" || echo "no processes"))"
        else
            echo "no sessions in ~/$R/sessions"
        fi
        busy_status=0
        busy="$(remote_busy)" || busy_status=$?
        [[ $busy_status -ne 4 ]] || exit 4
        if [[ $busy_status -eq 0 ]]; then
            echo "host: BUSY"
            printf '%s\n' "$busy" | head -8 | sed 's/^/  /'
        else
            echo "host: no benchmark process running"
        fi
        rssh_retry "
            if [ -f ~/bench.lock.holder ] && kill -0 \"\$(cut -d' ' -f1 ~/bench.lock.holder)\" 2>/dev/null; then
                echo \"  lock: held by \$(cat ~/bench.lock.holder)\"
            else
                echo '  lock: free'
            fi
            echo \"  load: \$(cut -d' ' -f1-3 /proc/loadavg)\"
            echo '  newest out dirs:'; ls -1t '$R/out' 2>/dev/null | head -3 | sed 's/^/    /'
            echo '  newest sessions:'
            for d in \$(ls -1t '$R/sessions' 2>/dev/null | head -3); do
                echo \"    \$d: \$(head -c 160 '$R/sessions/'\"\$d\"/state 2>/dev/null || echo 'no state')\"
            done
            exit 0"
        ;;
    log)
        [[ ${#ARGS[@]} -le 2 ]] || fail_usage "log takes [N] [SESSION]"
        n=40
        sess=""
        for a in ${ARGS[@]+"${ARGS[@]}"}; do
            if [[ "$a" =~ ^[0-9]+$ ]]; then n="$a"; else sess="$a"; fi
        done
        name="$(session_arg "$sess")"
        rssh_retry "echo '--- $name'; tail -n $n '$R/sessions/$name/session.log' 2>/dev/null || echo 'no log yet'"
        ;;
    wait)
        [[ ${#ARGS[@]} -ge 1 && ${#ARGS[@]} -le 2 && "${ARGS[0]}" =~ ^[1-9][0-9]*$ ]] || fail_usage "wait needs SECONDS [SESSION]"
        limit="${ARGS[0]}"
        name="$(session_arg "${ARGS[1]:-}")"
        waited=0
        while true; do
            probe="$(session_probe "$name")"
            alive="${probe%%$'\t'*}"
            state="${probe#*$'\t'}"
            case "$state" in
                done*) echo "[$(date +%H:%M:%S)] $name: $state (after ${waited}s)"; exit 0 ;;
                stopped*|missing) echo "[$(date +%H:%M:%S)] $name: $state" >&2; exit 5 ;;
            esac
            if [[ "$alive" != 1 ]]; then
                echo "[$(date +%H:%M:%S)] $name: no processes, but the state is '$state' (killed, or the host restarted)" >&2
                exit 5
            fi
            if (( waited >= limit )); then
                echo "[$(date +%H:%M:%S)] $name: still ${state%%:*} after ${limit}s"
                exit 1
            fi
            step=$(( limit - waited < 60 ? limit - waited : 60 ))
            echo "[$(date +%H:%M:%S)] $name: ${state:0:160} (${waited}s waited)"
            sleep "$step"
            waited=$(( waited + step ))
        done
        ;;
    fetch)
        [[ ${#ARGS[@]} -eq 1 ]] || fail_usage "fetch needs NAME or latest"
        run="${ARGS[0]}"
        if [[ "$run" == "latest" ]]; then
            run="$(rssh_retry "ls -1t '$R/out' 2>/dev/null | head -1" | tr -d '\r')"
            [[ -n "$run" ]] || { echo "Nothing in $TARGET:$R/out" >&2; exit 1; }
        fi
        [[ "$run" =~ $PLAIN ]] || fail_usage "NAME must be one directory name under out/, got '$run'"
        rssh_retry "test -d '$R/out/$run'" || { echo "No $TARGET:$R/out/$run" >&2; exit 1; }
        if [[ -z "$DEST" ]]; then
            common="$(cd "$SCRIPT_DIR" && git rev-parse --git-common-dir 2>/dev/null)" \
                || fail_usage "Not in a git checkout; pass --dest"
            DEST="$(cd "$SCRIPT_DIR" && cd "$common/.." && pwd)/.agents/tasks/racecar-opt-e2e/runs"
        fi
        mkdir -p "$DEST"
        dest="$DEST/$run"
        sessions="$(rssh_retry "cd '$R/sessions' 2>/dev/null && grep -l -F '$run' */session.sh */session.log 2>/dev/null | cut -d/ -f1 | sort -u | paste -sd' ' -; true")"
        for s in $sessions; do
            probe="$(session_probe "$s")"
            [[ "${probe%%$'\t'*}" != 1 ]] || echo "WARNING: session $s is still active; this copy may be partial." >&2
        done
        echo "Fetching $TARGET:$R/out/$run into $dest (without *.jar)"
        rssh_retry "tar -C '$R/out' --exclude='*.jar' -czf - '$run'" | tar -C "$DEST" -xzf -
        scp "${SSH_OPTS[@]}" -q "$TARGET:$R/host-info.txt" "$dest/host-info.txt" 2>/dev/null \
            || echo "WARNING: no host-info.txt on the host" >&2
        if [[ -n "$sessions" ]]; then
            mkdir -p "$dest/sessions"
            # shellcheck disable=SC2086 # session names are plain words
            rssh_retry "cd '$R/sessions' && tar --exclude='*.jar' -czf - $sessions" | tar -C "$dest/sessions" -xzf -
        fi
        echo "Fetched $(find "$dest" -type f | wc -l | tr -d ' ') files ($(du -sh "$dest" | cut -f1)); sessions: ${sessions:-none}"
        find "$dest" -maxdepth 3 \( -name '*.md' -o -name 'paired-ab.json' -o -name 'params.tsv' \) | sort | sed 's/^/  /'
        ;;
    stop)
        [[ ${#ARGS[@]} -eq 1 ]] || fail_usage "stop needs the SESSION name (it never stops anything else)"
        name="$(session_arg "${ARGS[0]}")"
        name_re="${name//./[.]}"
        rssh_retry "
            sids=\$(for p in \$(pgrep -f '$R_RE/session[s]/$name_re/session[.]sh'); do ps -o sid= -p \"\$p\"; done | tr -d ' ' | sort -u)
            mine=\$(ps -o sid= -p \$\$ | tr -d ' ')
            if [ -z \"\$sids\" ]; then echo 'No processes for session $name'; exit 0; fi
            for sid in \$sids; do
                if [ \"\$sid\" = \"\$mine\" ] || [ \"\$sid\" -le 1 ]; then continue; fi
                echo \"Terminating setsid session \$sid: \$(ps -o pid=,args= -s \"\$sid\" | wc -l | tr -d ' ') processes\"
                pkill -TERM -s \"\$sid\"
            done
            sleep 5
            for sid in \$sids; do
                if [ \"\$sid\" = \"\$mine\" ] || [ \"\$sid\" -le 1 ]; then continue; fi
                if pgrep -s \"\$sid\" > /dev/null; then echo \"Killing what is left of \$sid\"; pkill -KILL -s \"\$sid\"; fi
            done
            case \"\$(cat '$R/sessions/$name/state' 2>/dev/null)\" in
                done*|stopped*) ;;
                *) echo 'stopped (by e2e-remote.sh stop)' > '$R/sessions/$name/state' ;;
            esac
            echo \"state: \$(cat '$R/sessions/$name/state' 2>/dev/null)\""
        ;;
esac
