# Error behavior sweep 20260905-1532

- Date: 2026-09-05T15:32:09Z (UTC)
- Host: dev-dsk-alexwoo-2b-ee3cc828.us-west-2.amazon.com, Linux x86_64, 8 logical cores
- Java: openjdk version "21.0.9" 2025-10-21 LTS
- faults: internal-error,throughput-exceeded,unavailable,empty-500,malformed-body,resource-not-found, reps: 60, backoff: immediate, warmup: --warmup
- pinning: client=[0,1] server=[4,5,6], port 19082

## Arms

- `baseline`: ../../pipeline_benchmark2/jars/bridge-errbase6-published-2.46.10-dirty.jar — phase=errbase6 git.commit=ee9a74f69dbba396e4e6c67419210154b968f0ae git.branch=smithy-java-bridge-alexwoo-full git.dirty.files=true sdk.commit=published-2.46.10 build.time=2026-09-05T15:31:50Z sdk.v2.version=2.46.10 sdk.v1.version=1.12.797 smithy.java.version=1.6.1 
- `bridge`: ../../pipeline_benchmark2/jars/bridge-errbridge10-ee9a74f69db-dirty.jar — phase=errbridge10 git.commit=ee9a74f69dbba396e4e6c67419210154b968f0ae git.branch=smithy-java-bridge-alexwoo-full git.dirty.files=true sdk.commit=ee9a74f69db build.time=2026-09-05T15:31:38Z sdk.v2.version=2.46.11-SNAPSHOT sdk.v1.version=1.12.797 smithy.java.version=1.6.1 

The mock server comes from `baseline` and is shared by every arm; the fault catalogue
is a shared class, so the arms cannot disagree about what a mode means.

## Summary

- finished: 2026-09-05T15:33:38Z
- arms: 2, rows: 1560, failures: 0
- data: `errors.csv`
