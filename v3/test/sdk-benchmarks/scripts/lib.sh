#!/usr/bin/env bash
# Shared settings for the V3 benchmark scripts. Sourced, not run. Written for the bash 3.2 that macOS ships.
#
# Ported from feature/poc/racecar:test/standalone-e2e-benchmarks/scripts. The harness runs from the module's
# target/classes plus target/classpath.txt (written by `mvn ... -pl :sdk-benchmarks -am install`); nothing is shaded.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_DIR="$(cd "$MODULE_DIR/../../.." && pwd)"

# The benchmark JDK is pinned rather than inherited from the caller's JAVA_HOME (a shell-level JDK manager silently
# ran an early smoke run on JDK 25). Override deliberately with BENCH_JAVA_HOME; the run header and manifest record it.
JAVA_HOME="${BENCH_JAVA_HOME:-/Library/Java/JavaVirtualMachines/amazon-corretto-21.jdk/Contents/Home}"
JAVA="$JAVA_HOME/bin/java"
if [[ ! -x "$JAVA" ]]; then
    echo "error: no java at $JAVA; set BENCH_JAVA_HOME" >&2
    exit 1
fi

# Identical on client and server, as racecar's host runs use them: a fixed pre-touched heap keeps heap growth and page
# faults out of the window, and fixed GC/compiler thread counts keep the JVM's own threads from scaling with the host.
DEFAULT_JVM_FLAGS="-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4"
JVM_FLAGS="${BENCH_JVM_FLAGS:-$DEFAULT_JVM_FLAGS}"
# CRT loads a native library; silences the JDK 21+ restricted-method warning (racecar passes it always).
NATIVE_FLAGS="--enable-native-access=ALL-UNNAMED"

require_build() {
    if [[ ! -f "$MODULE_DIR/target/classpath.txt" || ! -d "$MODULE_DIR/target/classes" ]]; then
        echo "error: $MODULE_DIR/target/classpath.txt missing; build first:" >&2
        echo "  mvn -B -ntp -f $REPO_DIR/v3/pom.xml -Dv3.aggregate=benchmarks -pl :sdk-benchmarks -am install" >&2
        exit 1
    fi
    CLASSPATH_ARG="$MODULE_DIR/target/classes:$(cat "$MODULE_DIR/target/classpath.txt")"
}

sha_of_stdin() {
    shasum -a 256 | awk '{print $1}'
}

# Build provenance, passed to the client JVM as -Dbench.* (the module does no resource filtering).
provenance_flags() {
    local phase="${1:-unspecified}"
    local commit branch dirty harness sdk
    commit="$(git -C "$REPO_DIR" rev-parse HEAD 2>/dev/null || echo unknown)"
    branch="$(git -C "$REPO_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)"
    dirty="$(git -C "$REPO_DIR" status --porcelain --untracked-files=no -- v3 2>/dev/null | wc -l | tr -d ' ')"
    harness="$(cd "$MODULE_DIR/target/classes" && find . -type f | LC_ALL=C sort | xargs shasum -a 256 | sha_of_stdin)"
    # The SDK under test: every V3 jar on the classpath, by content.
    sdk="$(tr ':' '\n' < "$MODULE_DIR/target/classpath.txt" | grep '/software/amazon/awssdk/' | LC_ALL=C sort \
           | xargs shasum -a 256 | sha_of_stdin)"
    echo "-Dbench.phase=$phase -Dbench.commit=$commit -Dbench.branch=$branch -Dbench.dirty=$dirty" \
         "-Dbench.harness=$harness -Dbench.sdk=$sdk"
}

# Host, JDK, power and background metadata (Req 20.10), as far as the OS exposes it. Markdown bullet lines.
host_metadata() {
    echo "- date: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "- host: $(hostname -s) $(uname -srm)"
    if [[ "$(uname -s)" == "Darwin" ]]; then
        echo "- os: $(sw_vers -productName) $(sw_vers -productVersion) ($(sw_vers -buildVersion))"
        echo "- model: $(sysctl -n hw.model) cpu=\"$(sysctl -n machdep.cpu.brand_string)\""
        echo "- cores: logical=$(sysctl -n hw.logicalcpu) physical=$(sysctl -n hw.physicalcpu)" \
             "perf_levels=$(sysctl -n hw.nperflevels 2>/dev/null || echo ?)" \
             "p_cores=$(sysctl -n hw.perflevel0.physicalcpu 2>/dev/null || echo ?)" \
             "e_cores=$(sysctl -n hw.perflevel1.physicalcpu 2>/dev/null || echo ?)"
        echo "- memory: $(( $(sysctl -n hw.memsize) / 1073741824 )) GiB"
        echo "- power: $(pmset -g batt 2>/dev/null | head -1 | sed "s/Now drawing from //; s/'//g")" \
             "| $(pmset -g batt 2>/dev/null | sed -n 2p | sed 's/^[[:space:]]*//; s/[[:space:]]*present: true//')"
        echo "- power settings: $(pmset -g 2>/dev/null | grep -E ' (lowpowermode|powermode|sleep|displaysleep) ' \
             | awk '{printf "%s=%s ", $1, $2}')"
        echo "- thermal: $(pmset -g therm 2>/dev/null | grep -iE 'CPU_Speed_Limit|thermal warning|performance warning' \
             | tr -s ' \t' ' ' | tr '\n' ';')"
        echo "- pinning: UNAVAILABLE on macOS (no taskset/cpuset; thread affinity is advisory only)." \
             "Documented deviation from Req 20.5: client and server are scheduled freely on all cores."
    else
        echo "- os: $(. /etc/os-release 2>/dev/null && echo "$PRETTY_NAME")"
        echo "- cpu: $(grep -m1 'model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2) cores=$(nproc)"
        echo "- memory: $(awk '/MemTotal/ {printf "%d GiB", $2/1048576}' /proc/meminfo)"
        echo "- governor: $(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo n/a)"
    fi
    echo "- load average: $(uptime | sed 's/.*load averages*: //')"
    echo "- processes: $(ps -A -o pid= | wc -l | tr -d ' ')" \
         "top-cpu: $(ps -A -o %cpu=,comm= -r 2>/dev/null | head -5 | awk '{c=$2; sub(".*/","",c); printf "%s(%s%%) ", c, $1}')"
    echo "- jdk: $("$JAVA" -version 2>&1 | tr '\n' ' ')"
    echo "- java home: $JAVA_HOME"
    echo "- jvm flags (client and server): $JVM_FLAGS $NATIVE_FLAGS"
}

# Starts MockDdbServer as a child on $1 and waits for ITS OWN "READY" line (not /ping: a stale server on the same port
# answers /ping instantly while the new one dies with "Address already in use"; racecar was bitten by exactly that).
# Sets SERVER_PID and SERVER_LOG. Call stop_server (or trap it) to tear down.
start_server() {
    local port="$1"
    if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
        echo "error: port $port is already in use; find the squatter with: lsof -nP -iTCP:$port -sTCP:LISTEN" >&2
        exit 1
    fi
    SERVER_LOG="$(mktemp "${TMPDIR:-/tmp}/v3-mockddb.XXXXXX")"
    # shellcheck disable=SC2086
    "$JAVA" $JVM_FLAGS $NATIVE_FLAGS -cp "$CLASSPATH_ARG" software.amazon.awssdk.benchmark.dynamodb.MockDdbServer \
        --port "$port" > "$SERVER_LOG" 2>&1 &
    SERVER_PID=$!
    local waited=0
    while ! grep -q "^READY port=$port pid=$SERVER_PID " "$SERVER_LOG" 2>/dev/null; do
        if ! kill -0 "$SERVER_PID" 2>/dev/null; then
            echo "error: mock server exited before READY:" >&2
            cat "$SERVER_LOG" >&2
            exit 1
        fi
        sleep 0.1
        waited=$((waited + 1))
        if [[ $waited -gt 300 ]]; then
            echo "error: mock server not READY after 30s" >&2
            exit 1
        fi
    done
    grep '^READY' "$SERVER_LOG"
}

stop_server() {
    if [[ -n "${SERVER_PID:-}" ]]; then
        kill "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
        SERVER_PID=""
    fi
    [[ -n "${SERVER_LOG:-}" ]] && rm -f "$SERVER_LOG"
    SERVER_LOG=""
}
