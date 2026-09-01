# Benchmark collection 20260831-2339

## Environment

- Date: 2026-08-31T23:39:06Z (UTC)
- Host: ip-172-31-87-116.ec2.internal, Linux aarch64
- Hardware: Neoverse-N1, 64 logical cores, 126 GiB
- Java: openjdk version "25.0.4" 2026-07-21 LTS
- Git: not a git checkout (deployed script bundle) — see artifact provenance below
- SDK V2 version: 2.54.0
- Artifact under test: ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar
- Artifact provenance: phase=baseline254 git.commit=bdb1acd9b9b628f5bc1187bc46d9ed49eeb8a640 git.branch=feature/poc/racecar git.dirty.files=true sdk.commit=published-2.54.0 build.time=2026-08-31T22:57:02Z sdk.v2.version=2.54.0 sdk.v1.version=1.12.797 smithy.java.version=1.5.1 
- Benchmark module: test/standalone-e2e-benchmarks

## Parameters

- iterations: 40000
- warmup: 10000
- timing repetitions per case: 3
- clients: v1,v2-sync,v2-async,smithy
- scenarios: batch-get,batch-put
- concurrency: 1 (sync clients use this many threads; async clients keep this many in flight)
- async mode: inflight
- pinning: client=[32-47] server=[0-15]
- client jvm args: -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4
- server jvm args: -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4
- cpu source: auto
- server port: 19080 (fresh out-of-process mock server per run)
- total JVM runs: 48

## Notes

- Only clean timing runs append to results.csv. CPU-profile, alloc-profile and metrics runs are
  separate JVM executions because they perturb timing; their RESULT lines are in the per-case
  .log files, labeled by kind, and must not be compared against results.csv rows.
- Timing reps are interleaved: rep 1 of every case, then rep 2, etc., so machine drift spreads
  across cases. Phase order: timing (all reps), then cpu profiles, then alloc profiles, then
  metrics.
- Profiler recordings (async-profiler, JFR format) cover the whole JVM, including the 10000
  warmup ops and one-time client/connection setup (~20%
  of samples).
- The mock server shares the host with the client: ops_per_wall_sec includes contention effects;
  ops_per_cpu_sec / ops_per_user_cpu_sec count client-process CPU only.

## Runs

### v1_batch-get — timing rep 1

- started: 2026-08-31T23:39:06Z
- status: ok
- log: `v1_batch-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-sync_batch-get — timing rep 1

- started: 2026-08-31T23:41:01Z
- status: ok
- log: `v2-sync_batch-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-async_batch-get — timing rep 1

- started: 2026-08-31T23:42:00Z
- status: ok
- log: `v2-async_batch-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### smithy_batch-get — timing rep 1

- started: 2026-08-31T23:43:00Z
- status: ok
- log: `smithy_batch-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v1_batch-put — timing rep 1

- started: 2026-08-31T23:43:38Z
- status: ok
- log: `v1_batch-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-sync_batch-put — timing rep 1

- started: 2026-08-31T23:44:34Z
- status: ok
- log: `v2-sync_batch-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-async_batch-put — timing rep 1

- started: 2026-08-31T23:45:39Z
- status: ok
- log: `v2-async_batch-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### smithy_batch-put — timing rep 1

- started: 2026-08-31T23:46:46Z
- status: ok
- log: `smithy_batch-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v1_batch-get — timing rep 2

- started: 2026-08-31T23:47:15Z
- status: ok
- log: `v1_batch-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-sync_batch-get — timing rep 2

- started: 2026-08-31T23:48:58Z
- status: ok
- log: `v2-sync_batch-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-async_batch-get — timing rep 2

- started: 2026-08-31T23:49:57Z
- status: ok
- log: `v2-async_batch-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### smithy_batch-get — timing rep 2

- started: 2026-08-31T23:51:00Z
- status: ok
- log: `smithy_batch-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v1_batch-put — timing rep 2

- started: 2026-08-31T23:51:38Z
- status: ok
- log: `v1_batch-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-sync_batch-put — timing rep 2

- started: 2026-08-31T23:52:33Z
- status: ok
- log: `v2-sync_batch-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-async_batch-put — timing rep 2

- started: 2026-08-31T23:53:42Z
- status: ok
- log: `v2-async_batch-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### smithy_batch-put — timing rep 2

- started: 2026-08-31T23:54:49Z
- status: ok
- log: `smithy_batch-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v1_batch-get — timing rep 3

- started: 2026-08-31T23:55:23Z
- status: ok
- log: `v1_batch-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-sync_batch-get — timing rep 3

- started: 2026-08-31T23:57:06Z
- status: ok
- log: `v2-sync_batch-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-async_batch-get — timing rep 3

- started: 2026-08-31T23:58:05Z
- status: ok
- log: `v2-async_batch-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### smithy_batch-get — timing rep 3

- started: 2026-08-31T23:59:07Z
- status: ok
- log: `smithy_batch-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v1_batch-put — timing rep 3

- started: 2026-08-31T23:59:46Z
- status: ok
- log: `v1_batch-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-sync_batch-put — timing rep 3

- started: 2026-09-01T00:00:42Z
- status: ok
- log: `v2-sync_batch-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v2-async_batch-put — timing rep 3

- started: 2026-09-01T00:01:51Z
- status: ok
- log: `v2-async_batch-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### smithy_batch-put — timing rep 3

- started: 2026-09-01T00:02:58Z
- status: ok
- log: `smithy_batch-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/results.csv`

### v1_batch-get — cpu profile

- started: 2026-09-01T00:03:32Z
- status: ok
- log: `v1_batch-get/cpu.log`
- output: `v1_batch-get/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v1_batch-get/cpu.jfr`

### v2-sync_batch-get — cpu profile

- started: 2026-09-01T00:05:16Z
- status: ok
- log: `v2-sync_batch-get/cpu.log`
- output: `v2-sync_batch-get/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-sync_batch-get/cpu.jfr`

### v2-async_batch-get — cpu profile

- started: 2026-09-01T00:06:16Z
- status: ok
- log: `v2-async_batch-get/cpu.log`
- output: `v2-async_batch-get/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-async_batch-get/cpu.jfr`

### smithy_batch-get — cpu profile

- started: 2026-09-01T00:07:17Z
- status: ok
- log: `smithy_batch-get/cpu.log`
- output: `smithy_batch-get/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/smithy_batch-get/cpu.jfr`

### v1_batch-put — cpu profile

- started: 2026-09-01T00:07:56Z
- status: ok
- log: `v1_batch-put/cpu.log`
- output: `v1_batch-put/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v1_batch-put/cpu.jfr`

### v2-sync_batch-put — cpu profile

- started: 2026-09-01T00:08:51Z
- status: ok
- log: `v2-sync_batch-put/cpu.log`
- output: `v2-sync_batch-put/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-sync_batch-put/cpu.jfr`

### v2-async_batch-put — cpu profile

- started: 2026-09-01T00:10:01Z
- status: ok
- log: `v2-async_batch-put/cpu.log`
- output: `v2-async_batch-put/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-async_batch-put/cpu.jfr`

### smithy_batch-put — cpu profile

- started: 2026-09-01T00:11:07Z
- status: ok
- log: `smithy_batch-put/cpu.log`
- output: `smithy_batch-put/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/smithy_batch-put/cpu.jfr`

### v1_batch-get — alloc profile

- started: 2026-09-01T00:11:36Z
- status: ok
- log: `v1_batch-get/alloc.log`
- output: `v1_batch-get/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v1_batch-get/alloc.jfr`

### v2-sync_batch-get — alloc profile

- started: 2026-09-01T00:13:33Z
- status: ok
- log: `v2-sync_batch-get/alloc.log`
- output: `v2-sync_batch-get/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-sync_batch-get/alloc.jfr`

### v2-async_batch-get — alloc profile

- started: 2026-09-01T00:14:34Z
- status: ok
- log: `v2-async_batch-get/alloc.log`
- output: `v2-async_batch-get/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-async_batch-get/alloc.jfr`

### smithy_batch-get — alloc profile

- started: 2026-09-01T00:15:40Z
- status: ok
- log: `smithy_batch-get/alloc.log`
- output: `smithy_batch-get/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/smithy_batch-get/alloc.jfr`

### v1_batch-put — alloc profile

- started: 2026-09-01T00:16:19Z
- status: ok
- log: `v1_batch-put/alloc.log`
- output: `v1_batch-put/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v1_batch-put/alloc.jfr`

### v2-sync_batch-put — alloc profile

- started: 2026-09-01T00:17:15Z
- status: ok
- log: `v2-sync_batch-put/alloc.log`
- output: `v2-sync_batch-put/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-sync_batch-put/alloc.jfr`

### v2-async_batch-put — alloc profile

- started: 2026-09-01T00:18:23Z
- status: ok
- log: `v2-async_batch-put/alloc.log`
- output: `v2-async_batch-put/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-async_batch-put/alloc.jfr`

### smithy_batch-put — alloc profile

- started: 2026-09-01T00:19:33Z
- status: ok
- log: `smithy_batch-put/alloc.log`
- output: `smithy_batch-put/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/smithy_batch-put/alloc.jfr`

### v1_batch-get — sdk metrics

- started: 2026-09-01T00:20:03Z
- status: ok
- log: `v1_batch-get/metrics.log`
- output: `v1_batch-get/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v1_batch-get/metrics.txt`

### v2-sync_batch-get — sdk metrics

- started: 2026-09-01T00:21:50Z
- status: ok
- log: `v2-sync_batch-get/metrics.log`
- output: `v2-sync_batch-get/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-sync_batch-get/metrics.txt`

### v2-async_batch-get — sdk metrics

- started: 2026-09-01T00:22:50Z
- status: ok
- log: `v2-async_batch-get/metrics.log`
- output: `v2-async_batch-get/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-async_batch-get/metrics.txt`

### smithy_batch-get — sdk metrics

- started: 2026-09-01T00:23:52Z
- status: ok
- log: `smithy_batch-get/metrics.log`
- output: `smithy_batch-get/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-get --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/smithy_batch-get/metrics.txt`

### v1_batch-put — sdk metrics

- started: 2026-09-01T00:24:32Z
- status: ok
- log: `v1_batch-put/metrics.log`
- output: `v1_batch-put/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v1_batch-put/metrics.txt`

### v2-sync_batch-put — sdk metrics

- started: 2026-09-01T00:25:30Z
- status: ok
- log: `v2-sync_batch-put/metrics.log`
- output: `v2-sync_batch-put/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-sync_batch-put/metrics.txt`

### v2-async_batch-put — sdk metrics

- started: 2026-09-01T00:26:39Z
- status: ok
- log: `v2-async_batch-put/metrics.log`
- output: `v2-async_batch-put/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/v2-async_batch-put/metrics.txt`

### smithy_batch-put — sdk metrics

- started: 2026-09-01T00:27:48Z
- status: ok
- log: `smithy_batch-put/metrics.log`
- output: `smithy_batch-put/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario batch-put --iterations 40000 --warmup 10000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/batch/20260831-2339/smithy_batch-put/metrics.txt`

## Summary

- finished: 2026-09-01T00:28:19Z
- runs: 48, failures: 0
- results: `results.csv` (24 data rows)
