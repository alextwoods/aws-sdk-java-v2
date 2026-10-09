#!/usr/bin/env bash
# Forked-process harness (Req 20.1-20.10, 20.23-20.28): for each scheduled run, one fresh mock-service JVM and one
# fresh client JVM, pinned to disjoint CPUs where the host supports it (taskset on Linux; recorded as unavailable on
# macOS), with identical JVM flags, and a manifest of host/JDK/JVM/power/background metadata. Runs are interleaved
# repetitions of each arm (V3 and its same-artifact null twin) behind the JIT steady-state gate; every run record gets
# its validity, and validity.json holds the cell and collection verdicts. Arguments pass through to
# software.amazon.awssdk.benchmark.harness.HarnessMain; see its javadoc. Examples:
#
#   scripts/harness.sh --list
#   scripts/harness.sh --dry-run --protocols awsJson1_0 --paths generated-sync --payloads small --concurrency 1
#   scripts/harness.sh --protocols awsJson1_0 --transports apache5-sync --outcomes success --concurrency 1,8,32
#   # Release-gate collection (5 interleaved repetitions + null arm, documented gate, 30 s samples, fail-closed):
#   scripts/harness.sh --mode release --client-cpus 8-15 --service-cpus 2-5 --protocols awsJson1_0
#   # Exploratory, seconds per run, never evidence:
#   scripts/harness.sh --repetitions 2 --null-experiment true --warmup-policy steady-state \
#       --sample-interval-seconds 0.1 --steady-window 3 --warmup-timeout-seconds 3 --measure-seconds 1 ...
#
# The orchestrator JVM runs with small fixed flags of its own; the measured processes get --jvm-flags (default
# HarnessConfig.DEFAULT_JVM_FLAGS) and the JDK given by --java (default: the JDK pinned in lib.sh).
set -eo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
require_build
cd "$MODULE_DIR"
exec "$JAVA" -Xmx256m -cp "$CLASSPATH_ARG" software.amazon.awssdk.benchmark.harness.HarnessMain --java "$JAVA" "$@"
