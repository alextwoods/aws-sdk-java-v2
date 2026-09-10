# Paired A/B timing comparison 20260905-1935

## Arms

### stock

- jar: `/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-s3-stock-published-2.46.10-dirty.jar`
- provenance:
    - phase=s3-stock
    - git.commit=697d4f08734ad7afc46c5576e6ee1d4fd00c2181
    - git.branch=smithy-java-bridge-alexwoo-full
    - git.dirty.files=true
    - sdk.commit=published-2.46.10
    - build.time=2026-09-05T19:17:38Z
    - sdk.v2.version=2.46.10
    - sdk.v1.version=1.12.797
    - smithy.java.version=1.6.1

### bridge

- jar: `/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-s3-bridge-unrecorded-dirty.jar`
- provenance:
    - phase=s3-bridge
    - git.commit=697d4f08734ad7afc46c5576e6ee1d4fd00c2181
    - git.branch=smithy-java-bridge-alexwoo-full
    - git.dirty.files=true
    - sdk.commit=unrecorded
    - build.time=2026-09-05T19:17:56Z
    - sdk.v2.version=2.46.11-SNAPSHOT
    - sdk.v1.version=1.12.797
    - smithy.java.version=1.6.1

## Environment

- Date: 2026-09-05T19:35:53Z (UTC)
- Host: dev-dsk-alexwoo-2b-ee3cc828.us-west-2.amazon.com, Linux x86_64
- Hardware: Intel(R) Xeon(R) Platinum 8124M CPU @ 3.00GHz, 8 logical cores
- Java: openjdk version "21.0.9" 2025-10-21 LTS

## Parameters

- iterations: 1000, warmup: 250
- reps of the whole pair: 5
- clients: s3-v2-sync
- scenarios: get-object-8m,put-object-8m
- concurrency: 1, async mode: inflight
- pinning: client=[unpinned] server=[unpinned]
- client jvm args: (none)
- server jvm args: (none)
- server port: 19080 (fresh out-of-process mock server per run)
- total JVM runs: 20

## Design

Arms alternate within each repetition, and the arm order reverses on even repetitions, so
neither arm systematically occupies the warmer or colder position. Only timing is measured;
profiling perturbs it and belongs in a separate collect.sh run.

## Runs

- [1] rep 1, arm `stock`, s3-v2-sync/get-object-8m — ok, started 2026-09-05T19:35:53Z, log `logs/stock_s3-v2-sync_get-object-8m_rep1.log`
- [2] rep 1, arm `bridge`, s3-v2-sync/get-object-8m — ok, started 2026-09-05T19:37:13Z, log `logs/bridge_s3-v2-sync_get-object-8m_rep1.log`
- [3] rep 1, arm `stock`, s3-v2-sync/put-object-8m — ok, started 2026-09-05T19:38:32Z, log `logs/stock_s3-v2-sync_put-object-8m_rep1.log`
- [4] rep 1, arm `bridge`, s3-v2-sync/put-object-8m — ok, started 2026-09-05T19:39:55Z, log `logs/bridge_s3-v2-sync_put-object-8m_rep1.log`
- [5] rep 2, arm `bridge`, s3-v2-sync/get-object-8m — ok, started 2026-09-05T19:41:15Z, log `logs/bridge_s3-v2-sync_get-object-8m_rep2.log`
- [6] rep 2, arm `stock`, s3-v2-sync/get-object-8m — ok, started 2026-09-05T19:42:34Z, log `logs/stock_s3-v2-sync_get-object-8m_rep2.log`
- [7] rep 2, arm `bridge`, s3-v2-sync/put-object-8m — ok, started 2026-09-05T19:43:50Z, log `logs/bridge_s3-v2-sync_put-object-8m_rep2.log`
- [8] rep 2, arm `stock`, s3-v2-sync/put-object-8m — ok, started 2026-09-05T19:45:09Z, log `logs/stock_s3-v2-sync_put-object-8m_rep2.log`
- [9] rep 3, arm `stock`, s3-v2-sync/get-object-8m — ok, started 2026-09-05T19:46:30Z, log `logs/stock_s3-v2-sync_get-object-8m_rep3.log`
- [10] rep 3, arm `bridge`, s3-v2-sync/get-object-8m — ok, started 2026-09-05T19:47:51Z, log `logs/bridge_s3-v2-sync_get-object-8m_rep3.log`
- [11] rep 3, arm `stock`, s3-v2-sync/put-object-8m — ok, started 2026-09-05T19:49:11Z, log `logs/stock_s3-v2-sync_put-object-8m_rep3.log`
- [12] rep 3, arm `bridge`, s3-v2-sync/put-object-8m — ok, started 2026-09-05T19:50:33Z, log `logs/bridge_s3-v2-sync_put-object-8m_rep3.log`
- [13] rep 4, arm `bridge`, s3-v2-sync/get-object-8m — ok, started 2026-09-05T19:51:52Z, log `logs/bridge_s3-v2-sync_get-object-8m_rep4.log`
- [14] rep 4, arm `stock`, s3-v2-sync/get-object-8m — ok, started 2026-09-05T19:53:11Z, log `logs/stock_s3-v2-sync_get-object-8m_rep4.log`
- [15] rep 4, arm `bridge`, s3-v2-sync/put-object-8m — ok, started 2026-09-05T19:54:32Z, log `logs/bridge_s3-v2-sync_put-object-8m_rep4.log`
- [16] rep 4, arm `stock`, s3-v2-sync/put-object-8m — ok, started 2026-09-05T19:55:51Z, log `logs/stock_s3-v2-sync_put-object-8m_rep4.log`
- [17] rep 5, arm `stock`, s3-v2-sync/get-object-8m — ok, started 2026-09-05T19:57:12Z, log `logs/stock_s3-v2-sync_get-object-8m_rep5.log`
- [18] rep 5, arm `bridge`, s3-v2-sync/get-object-8m — ok, started 2026-09-05T19:58:32Z, log `logs/bridge_s3-v2-sync_get-object-8m_rep5.log`
- [19] rep 5, arm `stock`, s3-v2-sync/put-object-8m — ok, started 2026-09-05T19:59:51Z, log `logs/stock_s3-v2-sync_put-object-8m_rep5.log`
- [20] rep 5, arm `bridge`, s3-v2-sync/put-object-8m — ok, started 2026-09-05T20:01:12Z, log `logs/bridge_s3-v2-sync_put-object-8m_rep5.log`

## Summary

- finished: 2026-09-05T20:02:31Z
- runs: 20, failures: 0
