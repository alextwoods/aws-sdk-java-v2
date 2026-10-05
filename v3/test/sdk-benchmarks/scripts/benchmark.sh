#!/usr/bin/env bash
# One client JVM against a fresh out-of-process mock server: start the server, wait for its own READY line, run the
# runner, kill the server. Everything not listed here passes through to BenchmarkRunner (see --help there).
#
#   scripts/benchmark.sh --arm v3-sync-apache5 --scenario small-get --iterations 200000 [--port N] [--phase LABEL]
#
# Launcher-only options:
#   --port N          mock server port (default 19080)
#   --phase LABEL     provenance label recorded in every results row (default: unspecified)
#   --jvm-args "..."  extra client JVM args, appended after the shared flags (use sparingly: flags must match arms)
#
# The same JVM flags ($BENCH_JVM_FLAGS, default "-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4
# -XX:CICompilerCount=4") are used for client and server. There is no pinning on macOS (documented deviation).
set -eo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
require_build

PORT=19080
PHASE=unspecified
EXTRA_JVM_ARGS=""
RUNNER_ARGS=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --port) PORT="$2"; shift 2 ;;
        --phase) PHASE="$2"; shift 2 ;;
        --jvm-args) EXTRA_JVM_ARGS="$2"; shift 2 ;;
        --endpoint) echo "error: benchmark.sh launches its own server; use --port" >&2; exit 2 ;;
        *) RUNNER_ARGS+=("$1"); shift ;;
    esac
done

trap stop_server EXIT
start_server "$PORT"

# shellcheck disable=SC2046,SC2086
"$JAVA" $JVM_FLAGS $NATIVE_FLAGS $EXTRA_JVM_ARGS $(provenance_flags "$PHASE") -cp "$CLASSPATH_ARG" \
    software.amazon.awssdk.benchmark.dynamodb.BenchmarkRunner --endpoint "http://127.0.0.1:$PORT" "${RUNNER_ARGS[@]}"
