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
# Runs one benchmark session on a shared benchmark host, one session at a time. serde-ab-remote.sh and
# e2e-remote.sh start every session through this wrapper (see "Running on a remote metal host" in
# test/sdk-standard-benchmarks/README.md).
#
#   1. Queue: while any benchmark process runs (BENCH_PATTERN below; queued wrappers are not counted), write
#      "queued: ..." with the blocking command lines to <session dir>/state and sleep 60 s.
#   2. Lock: open ~/bench.lock on fd 9 and take it with flock -n. If another wrapper holds it, go back to 1. If a
#      benchmark process appeared meanwhile (one started without the wrapper), release the lock and go back to 1.
#   3. Run: write "running", run the session script with bash in the session directory, then write
#      "done exit=<status>" and exit with that status. The session's processes inherit fd 9, so the lock is held
#      until the last of them exits.
#
# <session dir> is the directory of SESSION_SCRIPT. Each state change is also printed with a UTC timestamp on
# stdout, which the launchers redirect to <session dir>/session.log. A session killed by a signal writes
# "stopped (signal <name>)".
#
# Usage: scripts/schema-serde/bench-session.sh SESSION_SCRIPT
#        scripts/schema-serde/bench-session.sh --busy        print BUSY and the benchmark processes and exit 0, or
#                                                            print IDLE and exit 1 (also works as bash -s -- --busy)
#        scripts/schema-serde/bench-session.sh --self-test   pattern and queue/lock tests (Linux: needs flock)
#
# Environment (tests only): BENCH_SESSION_PATTERN replaces BENCH_PATTERN, BENCH_SESSION_POLL the 60 s poll.
#
# Exit status: the session script's status; 2 usage error.
set -uo pipefail

# Every alternative has a bracket so the pattern never matches a shell whose command line contains the pattern
# itself (a remote "pgrep -f '<pattern>'"). Session scripts started by the launchers match
# /sessions/<name>/session.sh, so a session between two benchmark commands still counts as busy.
BENCH_PATTERN='serde-a[b][.]sh|paired-a[b][.]sh|optimization_experimen[t][.](sh|py)|hot_path_chec[k][.]sh'
BENCH_PATTERN+='|scripts/benchmar[k][.]sh|/session[s]/[^ ]*/session[.]sh'
BENCH_PATTERN+='|jav[a] .*-jar [^ ]*(jars|e2e|benchmarks|jmh)[^ ]*[.]jar'
BENCH_PATTERN+='|org[.]openjdk[.]jm[h][.]runner[.]ForkedMain|software[.]amazon[.]awssdk[.]benchmark[.]e2[e][.]'
PATTERN="${BENCH_SESSION_PATTERN:-$BENCH_PATTERN}"
POLL="${BENCH_SESSION_POLL:-60}"
SOURCE="${BASH_SOURCE[0]:-$0}"
SELF="$(cd "$(dirname "$SOURCE")" && pwd)/$(basename "$SOURCE")"

if [[ "$(uname)" == "Linux" ]]; then PGREP_LIST=(pgrep -af); else PGREP_LIST=(pgrep -lf); fi

# Benchmark processes, one "pid command line" per line, without queued or running wrappers.
busy_lines() {
    "${PGREP_LIST[@]}" -- "$PATTERN" 2>/dev/null | grep -v -E 'bench-sessio[n][.]sh' | cut -c1-160
}

usage() { sed -n '/^# Usage:/,/^# Exit status/p' "$SELF" | sed 's/^# \{0,1\}//'; }

# ==================== self-test ====================

self_test() {
    local failures=0 tmp marker
    check() {  # <description> <condition result: 0 pass>
        if [[ "$2" -eq 0 ]]; then echo "  ok: $1"; else echo "  FAIL: $1"; failures=$((failures + 1)); fi
    }
    echo "Pattern checks"
    local line
    while IFS= read -r line; do
        [[ -z "$line" ]] && continue
        printf '%s\n' "$line" | grep -q -E "$BENCH_PATTERN"
        check "busy: $line" $?
    done <<'EOF'
bash /home/u/serde-ab/repo/scripts/schema-serde/serde-ab.sh --null feature=/x.jar
bash /home/u/e2e-racecar/repo/scripts/schema-serde/paired-ab.sh --jar feature=/x.jar
bash /home/u/e2e-racecar/repo/scripts/schema-serde/optimization_experiment.sh B1 cand/B1
python3 /home/u/e2e-racecar/repo/scripts/schema-serde/optimization_experiment.py verdict B1
bash /home/u/e2e-racecar/repo/test/standalone-e2e-benchmarks/scripts/benchmark.sh --jar /x.jar
bash /home/u/serde-ab-port-r2/sessions/port-20261024-170000/session.sh
/usr/lib/jvm/java-25/bin/java -Xmx2g -jar /home/u/serde-ab/jars/feature.jar -f 2
java -jar /home/u/e2e-racecar/jmh/sdk-benchmarks-0123456789ab.jar benchmark.signer
java -cp /home/u/e2e-racecar/jars/e2e-0123.jar software.amazon.awssdk.benchmark.e2e.MockDdbServer --port 1
/usr/bin/java -XX:+UseSerialGC -cp /x org.openjdk.jmh.runner.ForkedMain 127.0.0.1 40000
EOF
    while IFS= read -r line; do
        [[ -z "$line" ]] && continue
        ! printf '%s\n' "$line" | grep -q -E "$BENCH_PATTERN"
        check "idle: $line" $?
    done <<EOF
bash -c pgrep -af '$BENCH_PATTERN'
tail -n 40 /home/u/e2e-racecar/sessions/b1-20261024-170000/session.log
sleep 60
java -version
EOF
    printf '%s\n' "4242 bash /home/u/r/repo/scripts/schema-serde/bench-session.sh /home/u/r/sessions/x/session.sh" \
        | grep -v -E 'bench-sessio[n][.]sh' | grep -q .
    check "a queued wrapper is not a benchmark process" $(( $? == 0 ? 1 : 0 ))

    if ! command -v flock > /dev/null 2>&1; then
        echo "SKIP: queue and lock tests (flock not found; they run on the Linux host)"
        [[ $failures -eq 0 ]] && { echo "PASS (pattern checks only)"; return 0; }
        echo "FAILED: $failures"; return 1
    fi
    echo "Queue and lock checks"
    tmp="$(mktemp -d "${TMPDIR:-/tmp}/bench-session-test.XXXXXXXX")"
    marker="bsst-$$-$RANDOM-marker"
    mkdir -p "$tmp/home" "$tmp/a" "$tmp/b"
    printf 'sleep "$1"\n' > "$tmp/$marker.sh"
    printf 'date +%%s%%N > "%s/a/ran"; sleep 4; date +%%s%%N > "%s/a/end"\n' "$tmp" "$tmp" > "$tmp/a/session.sh"
    printf 'date +%%s%%N > "%s/b/ran"; exit 7\n' "$tmp" > "$tmp/b/session.sh"
    local env_vars=(HOME="$tmp/home" BENCH_SESSION_PATTERN="${marker//r/[r]}" BENCH_SESSION_POLL=1)
    wait_state() {  # <dir> <prefix> <seconds>: 0 when <dir>/state starts with <prefix> within <seconds>
        local i
        for (( i = 0; i < $3 * 5; i++ )); do
            [[ "$(cat "$1/state" 2>/dev/null)" == "$2"* ]] && return 0
            sleep 0.2
        done
        echo "    $1/state is '$(cat "$1/state" 2>/dev/null)', expected '$2...'"
        return 1
    }
    bash "$tmp/$marker.sh" 6 &
    local dummy=$!
    sleep 0.5
    env "${env_vars[@]}" bash "$SELF" "$tmp/a/session.sh" > "$tmp/a/session.log" 2>&1 &
    local wa=$!
    wait_state "$tmp/a" "queued" 5
    check "A queues while a benchmark process runs" $?
    grep -q "$marker" "$tmp/a/state" 2>/dev/null
    check "A's state names the blocking command" $?
    [[ ! -f "$tmp/a/ran" ]]
    check "A has not run yet" $?
    wait "$dummy"
    wait_state "$tmp/a" "running" 8
    check "A runs once the benchmark process exits" $?
    env "${env_vars[@]}" bash "$SELF" "$tmp/b/session.sh" > "$tmp/b/session.log" 2>&1 &
    local wb=$!
    wait_state "$tmp/b" "queued: waiting for" 5
    check "B queues on the lock while A runs" $?
    [[ ! -f "$tmp/b/ran" ]]
    check "B has not run while A holds the lock" $?
    wait "$wa"
    check "A exits 0" $?
    wait "$wb"
    [[ $? -eq 7 ]]
    check "B exits with its session's status 7" $?
    [[ "$(cat "$tmp/a/state")" == "done exit=0" && "$(cat "$tmp/b/state")" == "done exit=7" ]]
    check "both states are done" $?
    [[ -f "$tmp/a/end" && -f "$tmp/b/ran" && "$(cat "$tmp/b/ran")" -ge "$(cat "$tmp/a/end")" ]]
    check "B started after A ended" $?
    grep -q "bench-session: running" "$tmp/a/session.log"
    check "state changes are logged" $?
    rm -rf "$tmp"
    if [[ $failures -eq 0 ]]; then echo "PASS"; return 0; fi
    echo "FAILED: $failures"
    return 1
}

# ==================== wrapper ====================

case "${1:-}" in
    --self-test) self_test; exit $? ;;
    --busy) out="$(busy_lines)"; [[ -n "$out" ]] && { printf 'BUSY\n%s\n' "$out"; exit 0; }; echo IDLE; exit 1 ;;
    -h|--help) usage; exit 0 ;;
    ""|-*) usage >&2; exit 2 ;;
esac
[[ $# -eq 1 && -f "$1" ]] || { echo "Session script not found: ${1:-}" >&2; exit 2; }
SCRIPT="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
DIR="$(dirname "$SCRIPT")"
LOCK="$HOME/bench.lock"
LAST=""
state() {
    printf '%s\n' "$1" > "$DIR/state"
    if [[ "$1" != "$LAST" ]]; then
        printf '[%s] bench-session: %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$1"
        LAST="$1"
    fi
}
for sig in TERM INT HUP; do
    # shellcheck disable=SC2064 # expand sig now
    trap "state 'stopped (signal $sig)'; exit 143" "$sig"
done
cd "$DIR" || exit 2
while true; do
    blockers="$(busy_lines | head -5)"
    if [[ -n "$blockers" ]]; then
        state "queued: benchmark processes running: $(printf '%s' "$blockers" | tr '\n' ';' | sed 's/;$//')"
        sleep "$POLL"
        continue
    fi
    exec 9>> "$LOCK"
    if ! flock -n 9; then
        exec 9>&-
        state "queued: waiting for $LOCK (held by: $(cat "$LOCK.holder" 2>/dev/null || echo unknown))"
        sleep "$POLL"
        continue
    fi
    blockers="$(busy_lines | head -5)"
    if [[ -n "$blockers" ]]; then  # a session started without the wrapper raced us
        flock -u 9
        exec 9>&-
        state "queued: benchmark processes running: $(printf '%s' "$blockers" | tr '\n' ';' | sed 's/;$//')"
        sleep "$POLL"
        continue
    fi
    break
done
printf '%s %s %s\n' "$$" "$DIR" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$LOCK.holder"
state "running"
bash "$SCRIPT"
rc=$?
rm -f "$LOCK.holder"
state "done exit=$rc"
exit "$rc"
