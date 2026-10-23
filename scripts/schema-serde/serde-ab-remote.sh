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
# Drives scripts/schema-serde/serde-ab.sh on a remote Linux benchmark host (an EC2 metal instance) over ssh. The host
# needs no checkout and no Maven: jars are built locally by serde-ab.sh (which evaluates the resource overlay and
# --sdk-pin at build time and records them in <jar>.provenance.properties), then deployed with the scripts, and
# serde-ab.sh runs them with --jar / --null LABEL=PATH.
#
# Two things are easy to get wrong, so this script does them:
#   Detaching. A backgrounded remote command that keeps any of its three streams on the ssh channel keeps ssh open
#   until the job ends. start runs the session under setsid nohup with stdin, stdout and stderr redirected, and ssh
#   itself with -n and its output discarded.
#   Liveness. setsid forks and exits at once, so a pid captured at launch is useless. status, wait and start check
#   for running sessions, serde-ab.sh, benchmark jars and JMH forks by process pattern.
# Run it with bash (it is a bash script; zsh mangles scp targets when ssh/scp are typed by hand). Nothing it starts
# listens on a non-loopback address: the serde benchmarks run in-process, and JMH's runner-to-fork link binds
# 127.0.0.1. setup records the host's listening TCP sockets in host-info.txt.
#
# Remote layout (under the remote home): <remote-dir>/repo/scripts/schema-serde/ (the scripts, at their repo paths),
# <remote-dir>/jars/ (jars with .provenance.properties and .resources.tsv), <remote-dir>/out/<run-id>/ (run with
# serde-ab.sh --out ~/<remote-dir>/out), <remote-dir>/sessions/<name>-<UTC stamp>/{session.sh,session.log},
# <remote-dir>/host-info.txt.
#
# Usage: scripts/schema-serde/serde-ab-remote.sh COMMAND [ARG] [options]
#   setup                  provision the host (idempotent): dnf install java-25-amazon-corretto-devel python3 tar
#                          util-linux procps-ng (+ perf, optional); async-profiler 4.4 for the host's architecture
#                          into /opt/async-profiler from the official GitHub release tarball (sha256-checked);
#                          sudo sysctl kernel.perf_event_paranoid=1 kernel.kptr_restrict=0 (not persistent); writes
#                          host-info.txt (JDK, kernel, CPU model, cores, NUMA, memory, cpufreq, listening sockets)
#   deploy --jar PATH ...  copy the scripts and each jar with its .provenance.properties (required) and .resources.tsv;
#                          a jar is skipped when the remote copy has the same sha256; then runs the Python self-tests
#                          on the host
#   start SCRIPT           ship a local bash script and run it detached as a session (cwd: the session directory);
#                          refuses while a session, serde-ab.sh or a benchmark JVM is running
#   status                 RUNNING or NOT-RUNNING, the matching processes, load, the newest runs and sessions
#   log [N]                the last N lines (default 40) of the newest session log
#   wait SECONDS           poll every 60 s until nothing runs; exit 1 if something still runs after SECONDS (keep
#                          SECONDS under the caller's own timeout and loop)
#   fetch RUN_ID|latest    copy the whole <remote-dir>/out/<run-id>/ (JMH JSON, tsv files, serde-ab.json, summary,
#                          verify, logs, profiles) to <dest>/<run-id>/, with host-info.txt and the session directories
#                          whose log names the run
#   stop                   terminate the sessions, serde-ab.sh and the benchmark JVMs
# Options:
#   --target USER@HOST     ssh destination (default: $SERDE_AB_REMOTE_TARGET)
#   --key PATH             ssh identity file (default: $SERDE_AB_REMOTE_KEY)
#   --remote-dir DIR       remote root, relative to the remote home (default serde-ab)
#   --dest DIR             fetch destination (default: $SERDE_AB_REMOTE_FETCH_DIR, else
#                          <main checkout>/.agents/tasks/serde-benchmark-metal/runs)
#   --ap-tarball PATH      setup: upload this async-profiler tarball instead of downloading it on the host
#   -h, --help             this text
#
# Exit status: 0 success, 1 a remote step failed (or wait timed out, or start found a run active), 2 usage error.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${SERDE_AB_REMOTE_TARGET:-}"
KEY="${SERDE_AB_REMOTE_KEY:-}"
REMOTE_DIR="serde-ab"
DEST="${SERDE_AB_REMOTE_FETCH_DIR:-}"
AP_TARBALL=""
JARS=()
CMD=""
ARGS=()
AP_VERSION="4.4"
# sha256 of the official release tarballs (GitHub release v4.4 asset digests).
AP_SHA_ARM64="86ff97b4436accdb6d7bb65c1cf6e38a756f2037a921994d8fa1dcb97d1dc53c"
AP_SHA_X64="1233f26fc95753e75ce32733bbcaf8f0bedc2c098b0e798af87935b08a63b24e"
# What deploy copies: serde-ab.sh and everything it runs or imports.
SCRIPTS=(serde-ab.sh serde_ab_checks.py serde_ab_summary.py serde_ab_profile.py SerdeWireDump.java
         hot_path_frames.py optimization_experiment.py paired_ab_summary.py racecar_decisions.py)
usage() { sed -n '/^# Usage:/,/^# Exit status/p' "$0" | sed 's/^# \{0,1\}//'; }
fail_usage() { echo "$1" >&2; exit 2; }
need() { [[ $# -ge 2 ]] || fail_usage "$1 needs a value"; }
while [[ $# -gt 0 ]]; do
    case "$1" in
        --target) need "$@"; TARGET="$2"; shift 2 ;;
        --key) need "$@"; KEY="$2"; shift 2 ;;
        --remote-dir) need "$@"; REMOTE_DIR="$2"; shift 2 ;;
        --dest) need "$@"; DEST="$2"; shift 2 ;;
        --ap-tarball) need "$@"; AP_TARBALL="$2"; shift 2 ;;
        --jar) need "$@"; JARS+=("$2"); shift 2 ;;
        -h|--help) usage; exit 0 ;;
        -*) fail_usage "Unknown option: $1 (see --help)" ;;
        *) if [[ -z "$CMD" ]]; then CMD="$1"; else ARGS+=("$1"); fi; shift ;;
    esac
done
[[ -n "$CMD" ]] || { usage >&2; exit 2; }
case "$CMD" in
    setup|deploy|start|status|log|wait|fetch|stop) ;;
    *) fail_usage "Unknown command: $CMD (see --help)" ;;
esac
[[ ${#JARS[@]} -eq 0 || "$CMD" == "deploy" ]] || fail_usage "--jar applies to deploy only"
[[ -n "$TARGET" && -n "$KEY" ]] || fail_usage "Set SERDE_AB_REMOTE_TARGET and SERDE_AB_REMOTE_KEY, or pass --target and --key"
# The target and the remote dir are interpolated into ssh/scp arguments and remote commands: allow plain names only.
[[ "$TARGET" =~ ^([A-Za-z0-9._-]+@)?[A-Za-z0-9][A-Za-z0-9.-]*$ ]] || fail_usage "--target must be USER@HOST or HOST, got '$TARGET'"
[[ "$REMOTE_DIR" =~ ^[A-Za-z0-9_][A-Za-z0-9._-]*$ ]] || fail_usage "--remote-dir must be one plain directory name under the remote home"
KEY="${KEY/#\~/$HOME}"
[[ -f "$KEY" ]] || fail_usage "Key not found: $KEY"
SSH_OPTS=(-i "$KEY" -o BatchMode=yes -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15
          -o ServerAliveInterval=30 -o ServerAliveCountMax=6)
R="$REMOTE_DIR"  # relative to the remote home: scp paths without a leading / are relative to it too
R_RE="${R//./[.]}"
# Running sessions, serde-ab.sh, java -jar <remote-dir>/jars/*.jar and JMH forks. Each alternative has a bracket so
# the pattern never matches the remote shell that runs pgrep with it.
ACTIVE_PATTERN="$R_RE/session[s]/[^ ]*/session[.]sh|schema-serde/serde-a[b][.]sh|$R_RE/jar[s]/[^ ]*[.]jar|org[.]openjdk[.]jm[h][.]runner[.]ForkedMain"
rssh() { ssh "${SSH_OPTS[@]}" "$TARGET" "$@"; }
rscp() { scp "${SSH_OPTS[@]}" -q "$@"; }
active() {  # 0: something runs, 1: nothing runs; exits 1 when the host cannot be asked (never a false "finished")
    local attempt status
    for attempt in 1 2 3; do
        status=0
        rssh "pgrep -f '$ACTIVE_PATTERN' > /dev/null 2>&1" || status=$?
        case $status in
            0) return 0 ;;
            1) return 1 ;;
        esac
        echo "Cannot ask $TARGET whether a run is active (ssh/pgrep exit $status; attempt $attempt of 3)" >&2
        sleep 10
    done
    exit 1
}
sha256_of() { if command -v sha256sum > /dev/null; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi; }

case "$CMD" in
    setup)
        if [[ -n "$AP_TARBALL" ]]; then
            [[ -f "$AP_TARBALL" ]] || fail_usage "--ap-tarball not found: $AP_TARBALL"
            rssh "mkdir -p '$R'"
            echo "Uploading $AP_TARBALL"
            rscp "$AP_TARBALL" "$TARGET:$R/async-profiler-upload.tar.gz"
        fi
        echo "Provisioning $TARGET"
        rssh "bash -s -- '$R' '$AP_VERSION' '$AP_SHA_ARM64' '$AP_SHA_X64'" <<'REMOTE'
set -euo pipefail
R="$1"; AP_VERSION="$2"; AP_SHA_ARM64="$3"; AP_SHA_X64="$4"
ROOT="$HOME/$R"
mkdir -p "$ROOT/repo/scripts/schema-serde" "$ROOT/jars" "$ROOT/out" "$ROOT/sessions"
echo "== packages"
sudo dnf install -y -q java-25-amazon-corretto-devel python3 tar util-linux procps-ng
sudo dnf install -y -q perf || echo "WARNING: perf not installed (optional)"
echo "== async-profiler $AP_VERSION"
case "$(uname -m)" in
    aarch64) AP_ARCH=linux-arm64; AP_SHA="$AP_SHA_ARM64" ;;
    x86_64) AP_ARCH=linux-x64; AP_SHA="$AP_SHA_X64" ;;
    *) echo "Unsupported architecture $(uname -m)" >&2; exit 1 ;;
esac
if /opt/async-profiler/bin/asprof --version 2>/dev/null | grep -q "Async-profiler $AP_VERSION "; then
    echo "already installed: $(/opt/async-profiler/bin/asprof --version | head -1)"
else
    tarball="$ROOT/async-profiler-upload.tar.gz"
    if [[ ! -f "$tarball" ]]; then
        tarball="$(mktemp /tmp/async-profiler-XXXXXX.tar.gz)"
        curl -fsSL -o "$tarball" \
            "https://github.com/async-profiler/async-profiler/releases/download/v$AP_VERSION/async-profiler-$AP_VERSION-$AP_ARCH.tar.gz"
    fi
    actual="$(sha256sum "$tarball" | cut -d' ' -f1)"
    if [[ "$actual" != "$AP_SHA" ]]; then
        echo "async-profiler tarball sha256 $actual, expected $AP_SHA (async-profiler-$AP_VERSION-$AP_ARCH.tar.gz)" >&2
        exit 1
    fi
    staging="/opt/async-profiler.new"
    sudo rm -rf "$staging"
    sudo mkdir -p "$staging"
    sudo tar -xzf "$tarball" -C "$staging" --strip-components=1
    # Keep a previous install instead of deleting it.
    [[ -e /opt/async-profiler ]] && sudo mv /opt/async-profiler "/opt/async-profiler.old-$(date -u +%Y%m%d%H%M%S)"
    sudo mv "$staging" /opt/async-profiler
    echo "installed: $(/opt/async-profiler/bin/asprof --version | head -1)"
fi
echo "== sysctl (benchmark host only; not persistent)"
sudo sysctl -w kernel.perf_event_paranoid=1 kernel.kptr_restrict=0
echo "== host info"
imds() {  # IMDSv2 metadata item, or "n/a"
    local token
    token="$(curl -fsS -m 2 -X PUT http://169.254.169.254/latest/api/token \
        -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' 2>/dev/null)" || { echo n/a; return; }
    curl -fsS -m 2 -H "X-aws-ec2-metadata-token: $token" "http://169.254.169.254/latest/meta-data/$1" 2>/dev/null || echo n/a
}
cpufreq() {
    local d=/sys/devices/system/cpu/cpu0/cpufreq
    if [[ -d "$d" ]]; then
        echo "driver $(cat "$d/scaling_driver" 2>/dev/null || echo ?), governor $(cat "$d/scaling_governor" 2>/dev/null || echo ?), min $(cat "$d/scaling_min_freq" 2>/dev/null || echo ?) kHz, max $(cat "$d/scaling_max_freq" 2>/dev/null || echo ?) kHz, boost $(cat /sys/devices/system/cpu/cpufreq/boost 2>/dev/null || cat /sys/devices/system/cpu/intel_pstate/no_turbo 2>/dev/null || echo n/a)"
    else
        echo "absent (no cpufreq driver; expected on Graviton: fixed frequency)"
    fi
}
lscpu_field() { lscpu | sed -n "s/^$1: *//p" | head -1; }
JAVA_BIN="$(readlink -f "$(command -v java)")"
{
    echo "recorded=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "hostname=$(hostname)"
    echo "instance_id=$(imds instance-id)"
    echo "instance_type=$(imds instance-type)"
    echo "os=$(. /etc/os-release && echo "$PRETTY_NAME")"
    echo "kernel=$(uname -r) $(uname -m)"
    echo "cpu_model=$(lscpu_field 'Model name')"
    echo "cpus=$(nproc --all) logical; sockets $(lscpu_field 'Socket(s)'), cores per socket $(lscpu_field 'Core(s) per socket'), threads per core $(lscpu_field 'Thread(s) per core')"
    echo "numa=$(lscpu | sed -n 's/^NUMA node\(.*\): *\(.*\)/node\1 \2/p' | tr -s ' ' | paste -sd';' -)"
    echo "memory=$(awk '/^MemTotal:/ { printf "%.1f GiB", $2 / 1048576 }' /proc/meminfo)"
    echo "cpufreq=$(cpufreq)"
    echo "java=$(java -version 2>&1 | tr '\n' ';' | sed 's/;$//')"
    echo "java_home=${JAVA_BIN%/bin/java}"
    echo "async_profiler=$(/opt/async-profiler/bin/asprof --version | head -1)"
    echo "perf_event_paranoid=$(cat /proc/sys/kernel/perf_event_paranoid)"
    echo "kptr_restrict=$(cat /proc/sys/kernel/kptr_restrict)"
    echo "python=$(python3 --version 2>&1)"
    echo "perf=$(perf --version 2>/dev/null || echo 'not installed')"
    echo "taskset=$(command -v taskset || echo missing)"
    echo "listening_tcp=$(ss -Htln 2>/dev/null | awk '{ print $4 }' | sort -u | paste -sd' ' -)"
} > "$ROOT/host-info.txt"
cat "$ROOT/host-info.txt"
REMOTE
        ;;
    deploy)
        [[ ${#ARGS[@]} -eq 0 ]] || fail_usage "deploy takes --jar PATH options only"
        for f in "${SCRIPTS[@]}"; do
            [[ -f "$SCRIPT_DIR/$f" ]] || fail_usage "Missing script $SCRIPT_DIR/$f"
        done
        NAMES=()
        for jar in ${JARS[@]+"${JARS[@]}"}; do
            [[ -f "$jar" ]] || fail_usage "Jar not found: $jar"
            [[ -f "$jar.provenance.properties" ]] \
                || fail_usage "$jar has no $jar.provenance.properties (the host reads the SDK pin and overlay from it)"
            name="$(basename "$jar")"
            [[ "$name" =~ ^[A-Za-z0-9._-]+$ ]] || fail_usage "Jar file name must be plain: $name"
            for n in ${NAMES[@]+"${NAMES[@]}"}; do
                [[ "$n" != "$name" ]] || fail_usage "Two jars named $name; the host keeps them in one directory"
            done
            NAMES+=("$name")
        done
        echo "Deploying scripts to $TARGET:$R/repo/scripts/schema-serde/"
        rssh "mkdir -p '$R/repo/scripts/schema-serde' '$R/jars' '$R/out' '$R/sessions'"
        files=()
        for f in "${SCRIPTS[@]}"; do files+=("$SCRIPT_DIR/$f"); done
        rscp "${files[@]}" "$TARGET:$R/repo/scripts/schema-serde/"
        rssh "chmod +x '$R/repo/scripts/schema-serde/serde-ab.sh'"
        for jar in ${JARS[@]+"${JARS[@]}"}; do
            name="$(basename "$jar")"
            local_sha="$(sha256_of "$jar")"
            remote_sha="$(rssh "sha256sum '$R/jars/$name' 2>/dev/null | cut -d' ' -f1" || true)"
            if [[ "$remote_sha" == "$local_sha" ]]; then
                echo "$name: already on the host (sha256 ${local_sha:0:12})"
            else
                echo "$name: uploading ($(du -h "$jar" | cut -f1), sha256 ${local_sha:0:12})"
                rscp "$jar" "$TARGET:$R/jars/$name"
                remote_sha="$(rssh "sha256sum '$R/jars/$name' | cut -d' ' -f1")"
                [[ "$remote_sha" == "$local_sha" ]] || { echo "$name: remote sha256 $remote_sha after upload" >&2; exit 1; }
            fi
            side=("$jar.provenance.properties")
            [[ -f "$jar.resources.tsv" ]] && side+=("$jar.resources.tsv")
            rscp "${side[@]}" "$TARGET:$R/jars/"
        done
        echo "Self-tests on the host (Python $(rssh 'python3 --version 2>&1' | awk '{ print $2 }'))"
        rssh "cd '$R/repo/scripts/schema-serde' && for t in serde_ab_summary.py serde_ab_checks.py serde_ab_profile.py; do
                  PYTHONDONTWRITEBYTECODE=1 python3 \"\$t\" --self-test > \"/tmp/serde-ab-selftest-\$t.log\" 2>&1 \
                      && echo \"  \$t: OK\" || { echo \"  \$t: FAILED\"; tail -20 \"/tmp/serde-ab-selftest-\$t.log\"; exit 1; }
              done"
        echo "Deployed:"
        rssh "cd '$R' && ls -la repo/scripts/schema-serde jars | sed 's/^/  /'"
        ;;
    start)
        [[ ${#ARGS[@]} -eq 1 ]] || fail_usage "start needs one local script"
        script="${ARGS[0]}"
        [[ -f "$script" ]] || fail_usage "Script not found: $script"
        base="$(basename "$script" .sh)"
        [[ "$base" =~ ^[A-Za-z0-9._-]+$ ]] || fail_usage "Script file name must be plain: $base"
        if active; then
            echo "A serde A/B session is already running on $TARGET (see status); stop it or wait." >&2
            exit 1
        fi
        name="$base-$(date -u +%Y%m%d-%H%M%S)"
        dir="$R/sessions/$name"
        rssh "mkdir -p '$dir'"
        rscp "$script" "$TARGET:$dir/session.sh"
        # All three streams of the job and of ssh redirected, or ssh waits for the job.
        ssh "${SSH_OPTS[@]}" -n "$TARGET" \
            "cd \"\$HOME/$dir\" && setsid nohup bash \"\$HOME/$dir/session.sh\" > session.log 2>&1 < /dev/null &" \
            > /dev/null 2>&1 < /dev/null
        sleep 3
        echo "Started session $name on $TARGET; log: ~/$dir/session.log"
        active || echo "WARNING: nothing is running any more; the session ended or failed to start (see the log)." >&2
        rssh "tail -5 '$dir/session.log' 2>/dev/null | sed 's/^/  /'" || true
        ;;
    status)
        rssh "
            if pgrep -f '$ACTIVE_PATTERN' > /dev/null 2>&1; then
                echo RUNNING
                pgrep -af '$ACTIVE_PATTERN' | cut -c1-200 | head -8 | sed 's/^/  /'
            else
                echo NOT-RUNNING
            fi
            echo \"  load: \$(cut -d' ' -f1-3 /proc/loadavg)\"
            echo '  newest runs:'; ls -1t '$R/out' 2>/dev/null | head -3 | sed 's/^/    /'
            echo '  newest sessions:'; ls -1t '$R/sessions' 2>/dev/null | head -3 | sed 's/^/    /'
            exit 0"
        ;;
    log)
        n="${ARGS[0]:-40}"
        [[ "$n" =~ ^[0-9]+$ ]] || fail_usage "log takes a line count"
        rssh "d=\$(ls -1t '$R/sessions' 2>/dev/null | head -1)
              if [ -n \"\$d\" ]; then echo \"--- \$d\"; tail -n $n '$R/sessions/'\"\$d\"/session.log; else echo 'No sessions yet'; fi"
        ;;
    wait)
        [[ ${#ARGS[@]} -eq 1 && "${ARGS[0]}" =~ ^[1-9][0-9]*$ ]] || fail_usage "wait needs SECONDS"
        limit="${ARGS[0]}"
        waited=0
        while active; do
            if (( waited >= limit )); then
                echo "[$(date +%H:%M:%S)] still running after ${limit}s"
                exit 1
            fi
            step=$(( limit - waited < 60 ? limit - waited : 60 ))
            echo "[$(date +%H:%M:%S)] running (${waited}s waited)"
            sleep "$step"
            waited=$(( waited + step ))
        done
        echo "[$(date +%H:%M:%S)] nothing running (after ${waited}s)"
        ;;
    fetch)
        [[ ${#ARGS[@]} -eq 1 ]] || fail_usage "fetch needs RUN_ID or latest"
        run="${ARGS[0]}"
        if [[ "$run" == "latest" ]]; then
            run="$(rssh "ls -1t '$R/out' 2>/dev/null | grep -E '^[0-9]{8}-[0-9]{6}\$' | head -1" | tr -d '\r' || true)"
            [[ -n "$run" ]] || { echo "No runs in $TARGET:$R/out" >&2; exit 1; }
        fi
        [[ "$run" =~ ^[0-9]{8}-[0-9]{6}$ ]] || fail_usage "Run id must look like 20261022-130554, got '$run'"
        if [[ -z "$DEST" ]]; then
            common="$(cd "$SCRIPT_DIR" && git rev-parse --git-common-dir 2>/dev/null)" \
                || fail_usage "Not in a git checkout; pass --dest or set SERDE_AB_REMOTE_FETCH_DIR"
            DEST="$(cd "$SCRIPT_DIR" && cd "$common/.." && pwd)/.agents/tasks/serde-benchmark-metal/runs"
        fi
        mkdir -p "$DEST"
        dest="$DEST/$run"
        active && echo "WARNING: a session is still running on $TARGET; this copy may be partial." >&2
        echo "Fetching $TARGET:$R/out/$run into $dest"
        rssh "tar -C '$R/out' -czf - '$run'" | tar -C "$DEST" -xzf -
        rscp "$TARGET:$R/host-info.txt" "$dest/host-info.txt" || echo "WARNING: no host-info.txt on the host" >&2
        sessions="$(rssh "cd '$R/sessions' 2>/dev/null && grep -l -F '$run' */session.log 2>/dev/null | cut -d/ -f1 | paste -sd' ' -" || true)"
        if [[ -n "$sessions" ]]; then
            mkdir -p "$dest/sessions"
            # shellcheck disable=SC2086 # session names are plain words
            rssh "cd '$R/sessions' && tar -czf - $sessions" | tar -C "$dest/sessions" -xzf -
        fi
        echo "Fetched $(find "$dest" -type f | wc -l | tr -d ' ') files ($(du -sh "$dest" | cut -f1)); sessions: ${sessions:-none}"
        for f in params.tsv arms.tsv runs.tsv serde-ab.json summary.md verify/verify.md profile-comparison.md; do
            if [[ -f "$dest/$f" ]]; then echo "  $f"; fi
        done
        ;;
    stop)
        echo "Stopping serde A/B processes on $TARGET"
        rssh "pkill -f '$R_RE/session[s]/[^ ]*/session[.]sh|schema-serde/serde-a[b][.]sh' 2>/dev/null; sleep 2
              pkill -f '$ACTIVE_PATTERN' 2>/dev/null; sleep 2
              echo \"  still matching: \$(pgrep -fc '$ACTIVE_PATTERN' 2>/dev/null; true)\"; exit 0"
        ;;
esac
