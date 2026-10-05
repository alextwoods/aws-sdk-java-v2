#!/usr/bin/env bash
# Standalone V3 mock DynamoDB server (MockDdbServer). Prints "READY port=..." when listening; runs until killed.
#   scripts/server.sh [--port N] [--threads N]
#   curl -s http://127.0.0.1:19080/stats
set -eo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
require_build
# shellcheck disable=SC2086
exec "$JAVA" $JVM_FLAGS $NATIVE_FLAGS -cp "$CLASSPATH_ARG" software.amazon.awssdk.benchmark.dynamodb.MockDdbServer "$@"
