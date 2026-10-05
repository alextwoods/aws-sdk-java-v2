#!/usr/bin/env bash
# One racecar-runner JVM (the optional V2 / smithy-java reference arm) against a fresh V3 MockDdbServer, with the same
# JVM flags as the V3 arms. Called by paired.sh --v2-reference-jar; not meant to be run by hand.
#
#   scripts/v2ref-run.sh PORT CLASSPATH <racecar BenchmarkRunner args...>
#
# Racecar's runner (feature/poc/racecar:test/standalone-e2e-benchmarks) takes --endpoint and speaks the same
# DynamoDB_20120810 awsJson1.0 targets; the V3 server returns racecar's canned bytes (MockServerGoldenTest). Its rows
# carry racecar's CSV columns only (a prefix of the V3 header), so summarize.py adds label, repetition, scope and
# validity from the file name and server_requests.
set -eo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
require_build
PORT="$1"
V2_CP="$2"
shift 2
trap stop_server EXIT
start_server "$PORT"
# shellcheck disable=SC2086
"$JAVA" $JVM_FLAGS $NATIVE_FLAGS -cp "$V2_CP" software.amazon.awssdk.benchmark.e2e.BenchmarkRunner \
    --endpoint "http://127.0.0.1:$PORT" "$@"
