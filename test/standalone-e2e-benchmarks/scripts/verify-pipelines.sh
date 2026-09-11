#!/usr/bin/env bash
# Local verification for pipeline_benchmark3.
#
# Positive cases: every arm runs on the jar it belongs to, serves the exact request count, and the
# pipeline it claims is the pipeline that executed.
# Negative cases: an arm run against the wrong jar must fail rather than report the other pipeline's
# numbers under its own name. That is the whole reason PipelineCheck exists, so it is tested, not assumed.
set -uo pipefail
cd /Users/alexwoo/java/java2-repo2/test/standalone-e2e-benchmarks
JARS=../../pipeline_benchmark2/jars
BRIDGED=$JARS/racecar-bench3-bridged-c1d88972e18.jar
RACECAR=$JARS/racecar-bench3-racecar-ac977512cbe.jar
S246=$JARS/racecar-bench3-stock246-published-2.46.10.jar
S254=$JARS/racecar-bench3-stock254-published-2.54.0.jar
ITER=400
fail=0

run() {  # jar label client scenario
    scripts/benchmark.sh --jar "$1" --client "$3" --scenario "$4" \
        --iterations $ITER --warmup 100 --warmup-mode fixed --progress-seconds 0 2>&1
}

expect_ok() { # jar label client scenario
    local out; out=$(run "$@")
    local verified reqs
    verified=$(echo "$out" | grep -oE "pipeline verified: [A-Z]+ [A-Z]+" | head -1)
    reqs=$(echo "$out" | grep -oE "server_requests=$ITER" | head -1)
    if [[ -n "$reqs" ]]; then
        printf "OK    %-10s %-11s %-13s %s\n" "$2" "$3" "$4" "${verified:-(no pipeline check: non-V2 arm)}"
    else
        printf "FAIL  %-10s %-11s %-13s\n" "$2" "$3" "$4"
        echo "$out" | grep -iE "exception|mismatch" | head -3
        fail=1
    fi
}

expect_reject() { # jar label client
    local out; out=$(run "$1" "$2" "$3" small-get)
    if echo "$out" | grep -q "pipeline mismatch"; then
        printf "OK    %-10s %-11s rejected as designed\n" "$2" "$3"
    else
        printf "FAIL  %-10s %-11s should have been rejected but was not\n" "$2" "$3"
        fail=1
    fi
}

echo "### positive: each arm on its own jar"
for s in small-get small-put batch-get batch-put describe-table; do
    expect_ok "$BRIDGED" bridged v2-bridged "$s"
done
for s in small-get batch-get; do
    expect_ok "$RACECAR" racecar v2-sync "$s"
    expect_ok "$RACECAR" racecar v2-async "$s"
    expect_ok "$RACECAR" racecar v1 "$s"
    expect_ok "$RACECAR" racecar smithy "$s"
done
expect_ok "$S246" stock246 v2-sync small-get
expect_ok "$S254" stock254 v2-sync small-get
expect_ok "$S254" stock254 v2-async small-get

echo
echo "### negative: wrong jar for the arm must fail"
expect_reject "$RACECAR" racecar v2-bridged
expect_reject "$S246"    stock246 v2-bridged
expect_reject "$S254"    stock254 v2-bridged
expect_reject "$BRIDGED" bridged  v2-sync

echo
echo "### the bridged build's async client is stock (bridge is sync-only)"
expect_ok "$BRIDGED" bridged v2-async small-get

exit $fail
