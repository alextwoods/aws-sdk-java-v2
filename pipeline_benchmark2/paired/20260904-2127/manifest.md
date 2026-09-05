# Paired A/B timing comparison 20260904-2127

## Arms

### nullA

- jar: `/local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-bridge-ee9a74f69db-dirty.jar`
- provenance:
    - phase=bridge
    - git.commit=ee9a74f69dbba396e4e6c67419210154b968f0ae
    - git.branch=smithy-java-bridge-alexwoo-full
    - git.dirty.files=true
    - sdk.commit=ee9a74f69db
    - build.time=2026-09-04T21:26:41Z
    - sdk.v2.version=2.46.11-SNAPSHOT
    - sdk.v1.version=1.12.797
    - smithy.java.version=1.6.1

### nullB

- jar: `/local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-bridge-ee9a74f69db-dirty.jar`
- provenance:
    - phase=bridge
    - git.commit=ee9a74f69dbba396e4e6c67419210154b968f0ae
    - git.branch=smithy-java-bridge-alexwoo-full
    - git.dirty.files=true
    - sdk.commit=ee9a74f69db
    - build.time=2026-09-04T21:26:41Z
    - sdk.v2.version=2.46.11-SNAPSHOT
    - sdk.v1.version=1.12.797
    - smithy.java.version=1.6.1

## Environment

- Date: 2026-09-04T21:27:52Z (UTC)
- Host: dev-dsk-alexwoo-2b-ee3cc828.us-west-2.amazon.com, Linux x86_64
- Hardware: Intel(R) Xeon(R) Platinum 8124M CPU @ 3.00GHz, 8 logical cores
- Java: openjdk version "21.0.9" 2025-10-21 LTS

## Parameters

- iterations: 20000, warmup: 10000
- reps of the whole pair: 3
- clients: v2-sync
- scenarios: small-get,batch-get
- concurrency: 1, async mode: inflight
- pinning: client=[0,1] server=[4,5,6]
- client jvm args: (none)
- server jvm args: (none)
- server port: 19080 (fresh out-of-process mock server per run)
- total JVM runs: 12

## Design

Arms alternate within each repetition, and the arm order reverses on even repetitions, so
neither arm systematically occupies the warmer or colder position. Only timing is measured;
profiling perturbs it and belongs in a separate collect.sh run.

## Runs

- [1] rep 1, arm `nullA`, v2-sync/small-get — ok, started 2026-09-04T21:27:52Z, log `logs/nullA_v2-sync_small-get_rep1.log`
- [2] rep 1, arm `nullB`, v2-sync/small-get — ok, started 2026-09-04T21:28:14Z, log `logs/nullB_v2-sync_small-get_rep1.log`
- [3] rep 1, arm `nullA`, v2-sync/batch-get — ok, started 2026-09-04T21:28:38Z, log `logs/nullA_v2-sync_batch-get_rep1.log`
- [4] rep 1, arm `nullB`, v2-sync/batch-get — ok, started 2026-09-04T21:29:14Z, log `logs/nullB_v2-sync_batch-get_rep1.log`
- [5] rep 2, arm `nullB`, v2-sync/small-get — ok, started 2026-09-04T21:29:52Z, log `logs/nullB_v2-sync_small-get_rep2.log`
- [6] rep 2, arm `nullA`, v2-sync/small-get — ok, started 2026-09-04T21:30:15Z, log `logs/nullA_v2-sync_small-get_rep2.log`
- [7] rep 2, arm `nullB`, v2-sync/batch-get — ok, started 2026-09-04T21:30:35Z, log `logs/nullB_v2-sync_batch-get_rep2.log`
- [8] rep 2, arm `nullA`, v2-sync/batch-get — ok, started 2026-09-04T21:31:13Z, log `logs/nullA_v2-sync_batch-get_rep2.log`
- [9] rep 3, arm `nullA`, v2-sync/small-get — ok, started 2026-09-04T21:31:52Z, log `logs/nullA_v2-sync_small-get_rep3.log`
- [10] rep 3, arm `nullB`, v2-sync/small-get — ok, started 2026-09-04T21:32:15Z, log `logs/nullB_v2-sync_small-get_rep3.log`
- [11] rep 3, arm `nullA`, v2-sync/batch-get — ok, started 2026-09-04T21:32:38Z, log `logs/nullA_v2-sync_batch-get_rep3.log`
- [12] rep 3, arm `nullB`, v2-sync/batch-get — ok, started 2026-09-04T21:33:15Z, log `logs/nullB_v2-sync_batch-get_rep3.log`

## Summary

- finished: 2026-09-04T21:33:53Z
- runs: 12, failures: 0
