#!/usr/bin/env bash
# Drive benchmark runs on a remote machine: start detached, check, tail, fetch, stop.
#
# Collections run for tens of minutes, so they must survive the SSH session that started them. This
# wraps the two things that are easy to get subtly wrong:
#
#   Detaching properly. A backgrounded remote command keeps the SSH channel's stdout open, so ssh
#   hangs until the job finishes even though the job itself is correctly detached. All three streams
#   of the outer remote command are redirected to prevent that.
#
#   Knowing whether it is still running. `setsid` forks and exits immediately, so the pid captured at
#   launch is dead within milliseconds while the job runs on. A pidfile check therefore reports
#   NOT-RUNNING for a healthy job — the worst possible error, since it invites reading half-written
#   results as final. Liveness is checked by process pattern instead.
#
# Connection settings come from the environment so they are stated once:
#   export RACECAR_REMOTE_TARGET=ec2-user@my-host
#   export RACECAR_REMOTE_KEY=~/my-key.pem
# or pass --target/--key.
#
# Usage: scripts/remote-run.sh COMMAND [args]
#   start SCRIPT     ship a local script and run it detached; names the run after the script
#   status           report whether a benchmark process is running, and what
#   log [N]          tail N lines (default 40) of the running/last run's log
#   wait [SECS]      poll until nothing is running (default timeout 7200s)
#   fetch KIND       copy results back: KIND is raw | paired | sweeps. Fetches the newest run,
#                    results and manifest only — per-case logs and .jfr stay on the host unless
#                    --with-profiles is given.
#   stop             terminate running benchmark processes on the host
#   exec 'CMD'       run a command synchronously on the host
set -uo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$(cd "$DIR/../.." && pwd)"

TARGET="${RACECAR_REMOTE_TARGET:-}"
KEY="${RACECAR_REMOTE_KEY:-}"
REMOTE_DIR="racecar"
WITH_PROFILES=0

CMD="${1:-}"
shift || true

ARGS=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --target)        TARGET="$2"; shift 2 ;;
        --key)           KEY="$2"; shift 2 ;;
        --remote-dir)    REMOTE_DIR="$2"; shift 2 ;;
        --with-profiles) WITH_PROFILES=1; shift ;;
        *)               ARGS+=("$1"); shift ;;
    esac
done

if [[ -z "$CMD" ]]; then
    sed -n '2,40p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit 2
fi
if [[ -z "$TARGET" || -z "$KEY" ]]; then
    echo "error: set RACECAR_REMOTE_TARGET and RACECAR_REMOTE_KEY, or pass --target/--key" >&2
    exit 2
fi
KEY="${KEY/#\~/$HOME}"
if [[ ! -f "$KEY" ]]; then
    echo "error: key not found: $KEY" >&2
    exit 2
fi

SSH_OPTS=(-i "$KEY" -o BatchMode=yes -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15
          -o ServerAliveInterval=30 -o ServerAliveCountMax=6)
RBENCH="$REMOTE_DIR/repo/test/standalone-e2e-benchmarks"
RRESULTS="$REMOTE_DIR/repo/pipeline_benchmark2"
# Any of the harness entry points counts as "a benchmark is running".
BENCH_PATTERN='scripts/(paired-ab|collect|concurrency-sweep|benchmark)\.sh'

rssh() { ssh "${SSH_OPTS[@]}" "$TARGET" "$@"; }

case "$CMD" in
    start)
        script="${ARGS[0]:-}"
        if [[ -z "$script" || ! -f "$script" ]]; then
            echo "error: start needs a local script file" >&2
            exit 2
        fi
        name="$(basename "$script" .sh)"
        if rssh "pgrep -f '$BENCH_PATTERN' >/dev/null 2>&1"; then
            echo "error: a benchmark is already running on $TARGET; use 'status' or 'stop' first" >&2
            exit 1
        fi
        scp "${SSH_OPTS[@]}" -q "$script" "$TARGET:/tmp/$name.sh"
        # Outer command's streams all redirected, or ssh waits on a channel the job holds open.
        ssh "${SSH_OPTS[@]}" -n "$TARGET" "bash -c '
            chmod +x /tmp/$name.sh
            mkdir -p ~/$REMOTE_DIR/runs/$name
            cd ~/$REMOTE_DIR/runs/$name
            setsid /tmp/$name.sh > run.log 2>&1 < /dev/null &
        ' >/dev/null 2>&1 </dev/null"
        sleep 3
        echo "started $name on $TARGET"
        rssh "echo '  log: ~/$REMOTE_DIR/runs/$name/run.log'; tail -3 ~/$REMOTE_DIR/runs/$name/run.log 2>/dev/null | sed 's/^/  /'"
        ;;

    status)
        rssh "
            if pgrep -f '$BENCH_PATTERN' >/dev/null 2>&1; then
                echo 'RUNNING'
                pgrep -af '$BENCH_PATTERN' | head -3 | cut -c1-160 | sed 's/^/  /'
            else
                echo 'NOT-RUNNING'
            fi
            echo '  java processes:' \$(pgrep -c java 2>/dev/null)
            echo '  load:' \$(uptime | sed 's/.*load average: //')
            echo '  newest run dirs:'
            for k in raw paired sweeps; do
                d=\$(ls -1t ~/$RRESULTS/\$k 2>/dev/null | head -1)
                [ -n \"\$d\" ] && echo \"    \$k/\$d\"
            done
            # Status is a report, not a test. Without this, a results family with no runs yet leaves
            # the last [ -n ... ] false, the ssh call exits non-zero, and any '&&' chain the caller
            # built around 'status' silently stops.
            exit 0
        "
        ;;

    log)
        n="${ARGS[0]:-40}"
        rssh "d=\$(ls -1t ~/$REMOTE_DIR/runs 2>/dev/null | head -1); \
              if [ -n \"\$d\" ]; then echo \"--- \$d ---\"; tail -$n ~/$REMOTE_DIR/runs/\$d/run.log; \
              else echo 'no runs yet'; fi"
        ;;

    wait)
        timeout="${ARGS[0]:-7200}"
        waited=0
        while true; do
            if ! rssh "pgrep -f '$BENCH_PATTERN' >/dev/null 2>&1"; then
                echo "[$(date +%H:%M:%S)] finished after ${waited}s"
                break
            fi
            if (( waited >= timeout )); then
                echo "[$(date +%H:%M:%S)] still running after ${timeout}s; giving up on waiting" >&2
                exit 1
            fi
            echo "[$(date +%H:%M:%S)] running (${waited}s elapsed)"
            sleep 60
            waited=$((waited + 60))
        done
        ;;

    fetch)
        kind="${ARGS[0]:-}"
        case "$kind" in
            raw|paired|sweeps) ;;
            *) echo "error: fetch needs raw | paired | sweeps" >&2; exit 2 ;;
        esac
        run="$(rssh "ls -1t ~/$RRESULTS/$kind 2>/dev/null | head -1" | tr -d '\r')"
        if [[ -z "$run" ]]; then
            echo "error: no $kind runs on the host" >&2
            exit 1
        fi
        # Prefixed with host- so remote results never collide with local ones in the same tree, and
        # so it is obvious later which machine produced a number.
        dest="$REPO/pipeline_benchmark2/$kind/host-$run"
        mkdir -p "$dest"
        echo "==> fetching $kind/$run into $dest"
        if [[ $WITH_PROFILES -eq 1 ]]; then
            scp "${SSH_OPTS[@]}" -qr "$TARGET:$RRESULTS/$kind/$run/." "$dest/"
        else
            for f in results.csv manifest.md summary.md; do
                scp "${SSH_OPTS[@]}" -q "$TARGET:$RRESULTS/$kind/$run/$f" "$dest/" 2>/dev/null || true
            done
        fi
        find "$dest" -type f | sed "s|$dest/||" | sort | sed 's/^/  /'
        ;;

    stop)
        echo "==> stopping benchmark processes on $TARGET"
        rssh "pkill -f '$BENCH_PATTERN' 2>/dev/null; sleep 2; \
              pkill -f 'benchmark.e2e.(BenchmarkRunner|MockDdbServer)' 2>/dev/null; sleep 1; \
              echo '  remaining java:' \$(pgrep -c java 2>/dev/null)"
        ;;

    exec)
        rssh "${ARGS[@]}"
        ;;

    *)
        echo "error: unknown command '$CMD'" >&2
        exit 2
        ;;
esac
