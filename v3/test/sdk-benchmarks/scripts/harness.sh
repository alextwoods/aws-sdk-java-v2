#!/usr/bin/env bash
# Forked-process harness (Req 20.1-20.5, 20.10): for each selected cell, one fresh mock-service JVM and one fresh
# client JVM, pinned to disjoint CPUs where the host supports it (taskset on Linux; recorded as unavailable on macOS),
# with identical JVM flags, and a manifest of host/JDK/JVM/power/background metadata. Arguments pass through to
# software.amazon.awssdk.benchmark.harness.HarnessMain; see its javadoc. Examples:
#
#   scripts/harness.sh --list
#   scripts/harness.sh --dry-run --protocols awsJson1_0 --paths generated-sync --payloads small --concurrency 1
#   scripts/harness.sh --protocols awsJson1_0 --transports apache5-sync --outcomes success --concurrency 1,8,32
#
# The orchestrator JVM runs with small fixed flags of its own; the measured processes get --jvm-flags (default
# HarnessConfig.DEFAULT_JVM_FLAGS) and the JDK given by --java (default: the JDK pinned in lib.sh).
set -eo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
require_build
cd "$MODULE_DIR"
exec "$JAVA" -Xmx256m -cp "$CLASSPATH_ARG" software.amazon.awssdk.benchmark.harness.HarnessMain --java "$JAVA" "$@"
