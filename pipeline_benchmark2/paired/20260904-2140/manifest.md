# Paired A/B timing comparison 20260904-2140

## Arms

### baseline

- jar: `/local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-baseline-published-2.46.10-dirty.jar`
- provenance:
    - phase=baseline
    - git.commit=ee9a74f69dbba396e4e6c67419210154b968f0ae
    - git.branch=smithy-java-bridge-alexwoo-full
    - git.dirty.files=true
    - sdk.commit=published-2.46.10
    - build.time=2026-09-04T21:26:59Z
    - sdk.v2.version=2.46.10
    - sdk.v1.version=1.12.797
    - smithy.java.version=1.6.1

### bridge

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

- Date: 2026-09-04T21:40:45Z (UTC)
- Host: dev-dsk-alexwoo-2b-ee3cc828.us-west-2.amazon.com, Linux x86_64
- Hardware: Intel(R) Xeon(R) Platinum 8124M CPU @ 3.00GHz, 8 logical cores
- Java: openjdk version "21.0.9" 2025-10-21 LTS

## Parameters

- iterations: 50000, warmup: 30000
- reps of the whole pair: 4
- clients: v2-sync,v2-sync-stripped,smithy
- scenarios: small-get,small-put,batch-get,batch-put
- concurrency: 1, async mode: inflight
- pinning: client=[0,1] server=[4,5,6]
- client jvm args: (none)
- server jvm args: (none)
- server port: 19080 (fresh out-of-process mock server per run)
- total JVM runs: 96

## Design

Arms alternate within each repetition, and the arm order reverses on even repetitions, so
neither arm systematically occupies the warmer or colder position. Only timing is measured;
profiling perturbs it and belongs in a separate collect.sh run.

## Runs

- [1] rep 1, arm `baseline`, v2-sync/small-get — ok, started 2026-09-04T21:40:45Z, log `logs/baseline_v2-sync_small-get_rep1.log`
- [2] rep 1, arm `bridge`, v2-sync/small-get — ok, started 2026-09-04T21:41:23Z, log `logs/bridge_v2-sync_small-get_rep1.log`
- [3] rep 1, arm `baseline`, v2-sync-stripped/small-get — ok, started 2026-09-04T21:41:51Z, log `logs/baseline_v2-sync-stripped_small-get_rep1.log`
- [4] rep 1, arm `bridge`, v2-sync-stripped/small-get — ok, started 2026-09-04T21:42:30Z, log `logs/bridge_v2-sync-stripped_small-get_rep1.log`
- [5] rep 1, arm `baseline`, smithy/small-get — ok, started 2026-09-04T21:42:56Z, log `logs/baseline_smithy_small-get_rep1.log`
- [6] rep 1, arm `bridge`, smithy/small-get — ok, started 2026-09-04T21:43:21Z, log `logs/bridge_smithy_small-get_rep1.log`
- [7] rep 1, arm `baseline`, v2-sync/small-put — ok, started 2026-09-04T21:43:42Z, log `logs/baseline_v2-sync_small-put_rep1.log`
- [8] rep 1, arm `bridge`, v2-sync/small-put — ok, started 2026-09-04T21:44:27Z, log `logs/bridge_v2-sync_small-put_rep1.log`
- [9] rep 1, arm `baseline`, v2-sync-stripped/small-put — ok, started 2026-09-04T21:44:54Z, log `logs/baseline_v2-sync-stripped_small-put_rep1.log`
- [10] rep 1, arm `bridge`, v2-sync-stripped/small-put — ok, started 2026-09-04T21:45:32Z, log `logs/bridge_v2-sync-stripped_small-put_rep1.log`
- [11] rep 1, arm `baseline`, smithy/small-put — ok, started 2026-09-04T21:45:58Z, log `logs/baseline_smithy_small-put_rep1.log`
- [12] rep 1, arm `bridge`, smithy/small-put — ok, started 2026-09-04T21:46:22Z, log `logs/bridge_smithy_small-put_rep1.log`
- [13] rep 1, arm `baseline`, v2-sync/batch-get — ok, started 2026-09-04T21:46:48Z, log `logs/baseline_v2-sync_batch-get_rep1.log`
- [14] rep 1, arm `bridge`, v2-sync/batch-get — ok, started 2026-09-04T21:48:13Z, log `logs/bridge_v2-sync_batch-get_rep1.log`
- [15] rep 1, arm `baseline`, v2-sync-stripped/batch-get — ok, started 2026-09-04T21:49:16Z, log `logs/baseline_v2-sync-stripped_batch-get_rep1.log`
- [16] rep 1, arm `bridge`, v2-sync-stripped/batch-get — ok, started 2026-09-04T21:50:49Z, log `logs/bridge_v2-sync-stripped_batch-get_rep1.log`
- [17] rep 1, arm `baseline`, smithy/batch-get — ok, started 2026-09-04T21:51:46Z, log `logs/baseline_smithy_batch-get_rep1.log`
- [18] rep 1, arm `bridge`, smithy/batch-get — ok, started 2026-09-04T21:52:38Z, log `logs/bridge_smithy_batch-get_rep1.log`
- [19] rep 1, arm `baseline`, v2-sync/batch-put — ok, started 2026-09-04T21:53:35Z, log `logs/baseline_v2-sync_batch-put_rep1.log`
- [20] rep 1, arm `bridge`, v2-sync/batch-put — ok, started 2026-09-04T21:55:11Z, log `logs/bridge_v2-sync_batch-put_rep1.log`
- [21] rep 1, arm `baseline`, v2-sync-stripped/batch-put — ok, started 2026-09-04T21:56:09Z, log `logs/baseline_v2-sync-stripped_batch-put_rep1.log`
- [22] rep 1, arm `bridge`, v2-sync-stripped/batch-put — ok, started 2026-09-04T21:57:52Z, log `logs/bridge_v2-sync-stripped_batch-put_rep1.log`
- [23] rep 1, arm `baseline`, smithy/batch-put — ok, started 2026-09-04T21:58:46Z, log `logs/baseline_smithy_batch-put_rep1.log`
- [24] rep 1, arm `bridge`, smithy/batch-put — ok, started 2026-09-04T21:59:45Z, log `logs/bridge_smithy_batch-put_rep1.log`
- [25] rep 2, arm `bridge`, v2-sync/small-get — ok, started 2026-09-04T22:00:43Z, log `logs/bridge_v2-sync_small-get_rep2.log`
- [26] rep 2, arm `baseline`, v2-sync/small-get — ok, started 2026-09-04T22:01:11Z, log `logs/baseline_v2-sync_small-get_rep2.log`
- [27] rep 2, arm `bridge`, v2-sync-stripped/small-get — ok, started 2026-09-04T22:01:56Z, log `logs/bridge_v2-sync-stripped_small-get_rep2.log`
- [28] rep 2, arm `baseline`, v2-sync-stripped/small-get — ok, started 2026-09-04T22:02:22Z, log `logs/baseline_v2-sync-stripped_small-get_rep2.log`
- [29] rep 2, arm `bridge`, smithy/small-get — ok, started 2026-09-04T22:03:01Z, log `logs/bridge_smithy_small-get_rep2.log`
- [30] rep 2, arm `baseline`, smithy/small-get — ok, started 2026-09-04T22:03:27Z, log `logs/baseline_smithy_small-get_rep2.log`
- [31] rep 2, arm `bridge`, v2-sync/small-put — ok, started 2026-09-04T22:03:52Z, log `logs/bridge_v2-sync_small-put_rep2.log`
- [32] rep 2, arm `baseline`, v2-sync/small-put — ok, started 2026-09-04T22:04:19Z, log `logs/baseline_v2-sync_small-put_rep2.log`
- [33] rep 2, arm `bridge`, v2-sync-stripped/small-put — ok, started 2026-09-04T22:04:56Z, log `logs/bridge_v2-sync-stripped_small-put_rep2.log`
- [34] rep 2, arm `baseline`, v2-sync-stripped/small-put — ok, started 2026-09-04T22:05:21Z, log `logs/baseline_v2-sync-stripped_small-put_rep2.log`
- [35] rep 2, arm `bridge`, smithy/small-put — ok, started 2026-09-04T22:05:58Z, log `logs/bridge_smithy_small-put_rep2.log`
- [36] rep 2, arm `baseline`, smithy/small-put — ok, started 2026-09-04T22:06:27Z, log `logs/baseline_smithy_small-put_rep2.log`
- [37] rep 2, arm `bridge`, v2-sync/batch-get — ok, started 2026-09-04T22:06:53Z, log `logs/bridge_v2-sync_batch-get_rep2.log`
- [38] rep 2, arm `baseline`, v2-sync/batch-get — ok, started 2026-09-04T22:07:55Z, log `logs/baseline_v2-sync_batch-get_rep2.log`
- [39] rep 2, arm `bridge`, v2-sync-stripped/batch-get — ok, started 2026-09-04T22:09:09Z, log `logs/bridge_v2-sync-stripped_batch-get_rep2.log`
- [40] rep 2, arm `baseline`, v2-sync-stripped/batch-get — ok, started 2026-09-04T22:10:10Z, log `logs/baseline_v2-sync-stripped_batch-get_rep2.log`
- [41] rep 2, arm `bridge`, smithy/batch-get — ok, started 2026-09-04T22:11:39Z, log `logs/bridge_smithy_batch-get_rep2.log`
- [42] rep 2, arm `baseline`, smithy/batch-get — ok, started 2026-09-04T22:12:33Z, log `logs/baseline_smithy_batch-get_rep2.log`
- [43] rep 2, arm `bridge`, v2-sync/batch-put — ok, started 2026-09-04T22:13:18Z, log `logs/bridge_v2-sync_batch-put_rep2.log`
- [44] rep 2, arm `baseline`, v2-sync/batch-put — ok, started 2026-09-04T22:14:13Z, log `logs/baseline_v2-sync_batch-put_rep2.log`
- [45] rep 2, arm `bridge`, v2-sync-stripped/batch-put — ok, started 2026-09-04T22:15:56Z, log `logs/bridge_v2-sync-stripped_batch-put_rep2.log`
- [46] rep 2, arm `baseline`, v2-sync-stripped/batch-put — ok, started 2026-09-04T22:16:47Z, log `logs/baseline_v2-sync-stripped_batch-put_rep2.log`
- [47] rep 2, arm `bridge`, smithy/batch-put — ok, started 2026-09-04T22:18:32Z, log `logs/bridge_smithy_batch-put_rep2.log`
- [48] rep 2, arm `baseline`, smithy/batch-put — ok, started 2026-09-04T22:19:31Z, log `logs/baseline_smithy_batch-put_rep2.log`
- [49] rep 3, arm `baseline`, v2-sync/small-get — ok, started 2026-09-04T22:20:31Z, log `logs/baseline_v2-sync_small-get_rep3.log`
- [50] rep 3, arm `bridge`, v2-sync/small-get — ok, started 2026-09-04T22:21:13Z, log `logs/bridge_v2-sync_small-get_rep3.log`
- [51] rep 3, arm `baseline`, v2-sync-stripped/small-get — ok, started 2026-09-04T22:21:40Z, log `logs/baseline_v2-sync-stripped_small-get_rep3.log`
- [52] rep 3, arm `bridge`, v2-sync-stripped/small-get — ok, started 2026-09-04T22:22:15Z, log `logs/bridge_v2-sync-stripped_small-get_rep3.log`
- [53] rep 3, arm `baseline`, smithy/small-get — ok, started 2026-09-04T22:22:41Z, log `logs/baseline_smithy_small-get_rep3.log`
- [54] rep 3, arm `bridge`, smithy/small-get — ok, started 2026-09-04T22:23:05Z, log `logs/bridge_smithy_small-get_rep3.log`
- [55] rep 3, arm `baseline`, v2-sync/small-put — ok, started 2026-09-04T22:23:30Z, log `logs/baseline_v2-sync_small-put_rep3.log`
- [56] rep 3, arm `bridge`, v2-sync/small-put — ok, started 2026-09-04T22:24:11Z, log `logs/bridge_v2-sync_small-put_rep3.log`
- [57] rep 3, arm `baseline`, v2-sync-stripped/small-put — ok, started 2026-09-04T22:24:37Z, log `logs/baseline_v2-sync-stripped_small-put_rep3.log`
- [58] rep 3, arm `bridge`, v2-sync-stripped/small-put — ok, started 2026-09-04T22:25:21Z, log `logs/bridge_v2-sync-stripped_small-put_rep3.log`
- [59] rep 3, arm `baseline`, smithy/small-put — ok, started 2026-09-04T22:25:45Z, log `logs/baseline_smithy_small-put_rep3.log`
- [60] rep 3, arm `bridge`, smithy/small-put — ok, started 2026-09-04T22:26:09Z, log `logs/bridge_smithy_small-put_rep3.log`
- [61] rep 3, arm `baseline`, v2-sync/batch-get — ok, started 2026-09-04T22:26:32Z, log `logs/baseline_v2-sync_batch-get_rep3.log`
- [62] rep 3, arm `bridge`, v2-sync/batch-get — ok, started 2026-09-04T22:28:05Z, log `logs/bridge_v2-sync_batch-get_rep3.log`
- [63] rep 3, arm `baseline`, v2-sync-stripped/batch-get — ok, started 2026-09-04T22:29:02Z, log `logs/baseline_v2-sync-stripped_batch-get_rep3.log`
- [64] rep 3, arm `bridge`, v2-sync-stripped/batch-get — ok, started 2026-09-04T22:30:34Z, log `logs/bridge_v2-sync-stripped_batch-get_rep3.log`
- [65] rep 3, arm `baseline`, smithy/batch-get — ok, started 2026-09-04T22:31:29Z, log `logs/baseline_smithy_batch-get_rep3.log`
- [66] rep 3, arm `bridge`, smithy/batch-get — ok, started 2026-09-04T22:32:14Z, log `logs/bridge_smithy_batch-get_rep3.log`
- [67] rep 3, arm `baseline`, v2-sync/batch-put — ok, started 2026-09-04T22:33:07Z, log `logs/baseline_v2-sync_batch-put_rep3.log`
- [68] rep 3, arm `bridge`, v2-sync/batch-put — ok, started 2026-09-04T22:34:51Z, log `logs/bridge_v2-sync_batch-put_rep3.log`
- [69] rep 3, arm `baseline`, v2-sync-stripped/batch-put — ok, started 2026-09-04T22:35:42Z, log `logs/baseline_v2-sync-stripped_batch-put_rep3.log`
- [70] rep 3, arm `bridge`, v2-sync-stripped/batch-put — ok, started 2026-09-04T22:37:33Z, log `logs/bridge_v2-sync-stripped_batch-put_rep3.log`
- [71] rep 3, arm `baseline`, smithy/batch-put — ok, started 2026-09-04T22:38:20Z, log `logs/baseline_smithy_batch-put_rep3.log`
- [72] rep 3, arm `bridge`, smithy/batch-put — ok, started 2026-09-04T22:39:19Z, log `logs/bridge_smithy_batch-put_rep3.log`
- [73] rep 4, arm `bridge`, v2-sync/small-get — ok, started 2026-09-04T22:40:17Z, log `logs/bridge_v2-sync_small-get_rep4.log`
- [74] rep 4, arm `baseline`, v2-sync/small-get — ok, started 2026-09-04T22:40:45Z, log `logs/baseline_v2-sync_small-get_rep4.log`
- [75] rep 4, arm `bridge`, v2-sync-stripped/small-get — ok, started 2026-09-04T22:41:23Z, log `logs/bridge_v2-sync-stripped_small-get_rep4.log`
- [76] rep 4, arm `baseline`, v2-sync-stripped/small-get — ok, started 2026-09-04T22:41:49Z, log `logs/baseline_v2-sync-stripped_small-get_rep4.log`
- [77] rep 4, arm `bridge`, smithy/small-get — ok, started 2026-09-04T22:42:27Z, log `logs/bridge_smithy_small-get_rep4.log`
- [78] rep 4, arm `baseline`, smithy/small-get — ok, started 2026-09-04T22:42:52Z, log `logs/baseline_smithy_small-get_rep4.log`
- [79] rep 4, arm `bridge`, v2-sync/small-put — ok, started 2026-09-04T22:43:16Z, log `logs/bridge_v2-sync_small-put_rep4.log`
- [80] rep 4, arm `baseline`, v2-sync/small-put — ok, started 2026-09-04T22:43:43Z, log `logs/baseline_v2-sync_small-put_rep4.log`
- [81] rep 4, arm `bridge`, v2-sync-stripped/small-put — ok, started 2026-09-04T22:44:31Z, log `logs/bridge_v2-sync-stripped_small-put_rep4.log`
- [82] rep 4, arm `baseline`, v2-sync-stripped/small-put — ok, started 2026-09-04T22:44:55Z, log `logs/baseline_v2-sync-stripped_small-put_rep4.log`
- [83] rep 4, arm `bridge`, smithy/small-put — ok, started 2026-09-04T22:45:30Z, log `logs/bridge_smithy_small-put_rep4.log`
- [84] rep 4, arm `baseline`, smithy/small-put — ok, started 2026-09-04T22:45:54Z, log `logs/baseline_smithy_small-put_rep4.log`
- [85] rep 4, arm `bridge`, v2-sync/batch-get — ok, started 2026-09-04T22:46:18Z, log `logs/bridge_v2-sync_batch-get_rep4.log`
- [86] rep 4, arm `baseline`, v2-sync/batch-get — ok, started 2026-09-04T22:47:16Z, log `logs/baseline_v2-sync_batch-get_rep4.log`
- [87] rep 4, arm `bridge`, v2-sync-stripped/batch-get — ok, started 2026-09-04T22:48:49Z, log `logs/bridge_v2-sync-stripped_batch-get_rep4.log`
- [88] rep 4, arm `baseline`, v2-sync-stripped/batch-get — ok, started 2026-09-04T22:49:46Z, log `logs/baseline_v2-sync-stripped_batch-get_rep4.log`
- [89] rep 4, arm `bridge`, smithy/batch-get — ok, started 2026-09-04T22:51:15Z, log `logs/bridge_smithy_batch-get_rep4.log`
- [90] rep 4, arm `baseline`, smithy/batch-get — ok, started 2026-09-04T22:52:00Z, log `logs/baseline_smithy_batch-get_rep4.log`
- [91] rep 4, arm `bridge`, v2-sync/batch-put — ok, started 2026-09-04T22:52:46Z, log `logs/bridge_v2-sync_batch-put_rep4.log`
- [92] rep 4, arm `baseline`, v2-sync/batch-put — ok, started 2026-09-04T22:53:40Z, log `logs/baseline_v2-sync_batch-put_rep4.log`
- [93] rep 4, arm `bridge`, v2-sync-stripped/batch-put — ok, started 2026-09-04T22:55:27Z, log `logs/bridge_v2-sync-stripped_batch-put_rep4.log`
- [94] rep 4, arm `baseline`, v2-sync-stripped/batch-put — ok, started 2026-09-04T22:56:18Z, log `logs/baseline_v2-sync-stripped_batch-put_rep4.log`
- [95] rep 4, arm `bridge`, smithy/batch-put — ok, started 2026-09-04T22:58:01Z, log `logs/bridge_smithy_batch-put_rep4.log`
- [96] rep 4, arm `baseline`, smithy/batch-put — ok, started 2026-09-04T22:58:59Z, log `logs/baseline_smithy_batch-put_rep4.log`

## Summary

- finished: 2026-09-04T22:59:58Z
- runs: 96, failures: 0
