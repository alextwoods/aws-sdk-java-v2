#!/usr/bin/env bash
# All pre-timing verification, against a fresh out-of-process mock server:
#   1. VerifyAll (one JVM): every arm's transport identity, server-side request count, wire bytes and response digest;
#      digests compared across all arms; negative cases (wrong transport, wrong mode, wrong request body, wrong
#      response from a tampered in-process server, request never seen) must all be rejected.
#   2. One JVM per arm (`BenchmarkRunner --verify-only`): the DIGEST lines must be identical across arms.
#   scripts/verify.sh [--port N]
set -eo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
require_build

PORT=19080
[[ "$1" == "--port" ]] && PORT="$2"
ARMS="v3-sync-apache5 v3-sync-apache4 v3-sync-urlconnection v3-sync-crt v3-async-netty v3-async-crt v3-async-apache5"

trap stop_server EXIT
start_server "$PORT"
ENDPOINT="http://127.0.0.1:$PORT"

echo "== VerifyAll (single JVM, positive + negative)"
# shellcheck disable=SC2086
"$JAVA" $JVM_FLAGS $NATIVE_FLAGS -cp "$CLASSPATH_ARG" software.amazon.awssdk.benchmark.dynamodb.VerifyAll "$ENDPOINT"

echo "== per-arm JVMs (--verify-only), digests compared across arms"
DIGESTS="$(mktemp "${TMPDIR:-/tmp}/v3-digests.XXXXXX")"
for arm in $ARMS; do
    # shellcheck disable=SC2046,SC2086
    "$JAVA" $JVM_FLAGS $NATIVE_FLAGS $(provenance_flags verify) -cp "$CLASSPATH_ARG" \
        software.amazon.awssdk.benchmark.dynamodb.BenchmarkRunner --arm "$arm" --endpoint "$ENDPOINT" --verify-only \
        | tee /dev/stderr | grep '^DIGEST' | sed 's/arm=[^ ]* //' > "$DIGESTS.$arm"
done 2> >(grep -E '^(=== transport verified|DIGEST)' >&2)
first=""
status=0
for arm in $ARMS; do
    if [[ ! -s "$DIGESTS.$arm" ]]; then
        echo "FAIL $arm printed no DIGEST lines"; status=1; continue
    fi
    if [[ -z "$first" ]]; then
        first="$arm"
    elif ! diff -q "$DIGESTS.$first" "$DIGESTS.$arm" >/dev/null; then
        echo "FAIL digests differ: $first vs $arm"; diff "$DIGESTS.$first" "$DIGESTS.$arm" || true; status=1
    fi
done
[[ $status -eq 0 ]] && { echo "per-JVM digests identical across $(echo $ARMS | wc -w | tr -d ' ') arms:"; cat "$DIGESTS.$first"; }
rm -f "$DIGESTS" "$DIGESTS".*
[[ $status -eq 0 ]] && echo "VERIFY.SH PASSED" || echo "VERIFY.SH FAILED"
exit $status
