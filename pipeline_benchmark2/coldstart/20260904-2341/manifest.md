# Cold-start sweep 20260904-2341

- Date: 2026-09-04T23:41:19Z (UTC)
- Host: dev-dsk-alexwoo-2b-ee3cc828.us-west-2.amazon.com, Linux x86_64, 8 logical cores
- Java: openjdk version "21.0.9" 2025-10-21 LTS
- clients: v2-sync,v2-sync-stripped,smithy, scenario: small-get, reps: 10, ops per JVM: 5
- pinning: client=[0,1] server=[4,5,6], port 19081

## Arms

- `baseline`: ../../pipeline_benchmark2/jars/bridge-coldbase-published-2.46.10-dirty.jar — phase=coldbase git.commit=ee9a74f69dbba396e4e6c67419210154b968f0ae git.branch=smithy-java-bridge-alexwoo-full git.dirty.files=true sdk.commit=published-2.46.10 build.time=2026-09-04T23:40:59Z sdk.v2.version=2.46.10 sdk.v1.version=1.12.797 smithy.java.version=1.6.1 
- `bridge`: ../../pipeline_benchmark2/jars/bridge-coldstart-ee9a74f69db-dirty.jar — phase=coldstart git.commit=ee9a74f69dbba396e4e6c67419210154b968f0ae git.branch=smithy-java-bridge-alexwoo-full git.dirty.files=true sdk.commit=ee9a74f69db build.time=2026-09-04T23:39:27Z sdk.v2.version=2.46.11-SNAPSHOT sdk.v1.version=1.12.797 smithy.java.version=1.6.1 

One JVM per (arm, client, rep), arms and clients interleaved within each repetition. The mock
server comes from `baseline` and is shared and warm for the whole sweep.

## Summary

- finished: 2026-09-04T23:42:35Z
- runs: 60, failures: 0
- data: `coldstart.csv`
