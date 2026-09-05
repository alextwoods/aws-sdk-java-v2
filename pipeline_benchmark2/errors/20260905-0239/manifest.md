# Error behavior sweep 20260905-0239

- Date: 2026-09-05T02:39:11Z (UTC)
- Host: dev-dsk-alexwoo-2b-ee3cc828.us-west-2.amazon.com, Linux x86_64, 8 logical cores
- Java: openjdk version "21.0.9" 2025-10-21 LTS
- faults: all, reps: 2
- pinning: client=[unpinned] server=[unpinned], port 19082

## Arms

- `baseline`: ../../pipeline_benchmark2/jars/bridge-errbase4-published-2.46.10-dirty.jar — phase=errbase4 git.commit=ee9a74f69dbba396e4e6c67419210154b968f0ae git.branch=smithy-java-bridge-alexwoo-full git.dirty.files=true sdk.commit=published-2.46.10 build.time=2026-09-05T02:35:11Z sdk.v2.version=2.46.10 sdk.v1.version=1.12.797 smithy.java.version=1.6.1 
- `bridge`: ../../pipeline_benchmark2/jars/bridge-errbridge6-ee9a74f69db-dirty.jar — phase=errbridge6 git.commit=ee9a74f69dbba396e4e6c67419210154b968f0ae git.branch=smithy-java-bridge-alexwoo-full git.dirty.files=true sdk.commit=ee9a74f69db build.time=2026-09-05T02:34:58Z sdk.v2.version=2.46.11-SNAPSHOT sdk.v1.version=1.12.797 smithy.java.version=1.6.1 

The mock server comes from `baseline` and is shared by every arm; the fault catalogue
is a shared class, so the arms cannot disagree about what a mode means.

## Summary

- finished: 2026-09-05T02:40:04Z
- arms: 2, rows: 92, failures: 0
- data: `errors.csv`
