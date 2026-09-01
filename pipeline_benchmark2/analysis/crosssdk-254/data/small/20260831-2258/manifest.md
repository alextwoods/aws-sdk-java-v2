# Benchmark collection 20260831-2258

## Environment

- Date: 2026-08-31T22:58:25Z (UTC)
- Host: ip-172-31-87-116.ec2.internal, Linux aarch64
- Hardware: Neoverse-N1, 64 logical cores, 126 GiB
- Java: openjdk version "25.0.4" 2026-07-21 LTS
- Git: not a git checkout (deployed script bundle) — see artifact provenance below
- SDK V2 version: 2.54.0
- Artifact under test: ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar
- Artifact provenance: phase=baseline254 git.commit=bdb1acd9b9b628f5bc1187bc46d9ed49eeb8a640 git.branch=feature/poc/racecar git.dirty.files=true sdk.commit=published-2.54.0 build.time=2026-08-31T22:57:02Z sdk.v2.version=2.54.0 sdk.v1.version=1.12.797 smithy.java.version=1.5.1 
- Benchmark module: test/standalone-e2e-benchmarks

## Parameters

- iterations: 200000
- warmup: 20000
- timing repetitions per case: 3
- clients: v1,v2-sync,v2-async,smithy
- scenarios: small-get,small-put
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
- Profiler recordings (async-profiler, JFR format) cover the whole JVM, including the 20000
  warmup ops and one-time client/connection setup (~9%
  of samples).
- The mock server shares the host with the client: ops_per_wall_sec includes contention effects;
  ops_per_cpu_sec / ops_per_user_cpu_sec count client-process CPU only.

## Runs

### v1_small-get — timing rep 1

- started: 2026-08-31T22:58:25Z
- status: ok
- log: `v1_small-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-sync_small-get — timing rep 1

- started: 2026-08-31T22:59:11Z
- status: ok
- log: `v2-sync_small-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-async_small-get — timing rep 1

- started: 2026-08-31T23:00:08Z
- status: ok
- log: `v2-async_small-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### smithy_small-get — timing rep 1

- started: 2026-08-31T23:01:14Z
- status: ok
- log: `smithy_small-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v1_small-put — timing rep 1

- started: 2026-08-31T23:01:48Z
- status: ok
- log: `v1_small-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-sync_small-put — timing rep 1

- started: 2026-08-31T23:02:35Z
- status: ok
- log: `v2-sync_small-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-async_small-put — timing rep 1

- started: 2026-08-31T23:03:31Z
- status: ok
- log: `v2-async_small-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### smithy_small-put — timing rep 1

- started: 2026-08-31T23:04:35Z
- status: ok
- log: `smithy_small-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v1_small-get — timing rep 2

- started: 2026-08-31T23:05:10Z
- status: ok
- log: `v1_small-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-sync_small-get — timing rep 2

- started: 2026-08-31T23:05:56Z
- status: ok
- log: `v2-sync_small-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-async_small-get — timing rep 2

- started: 2026-08-31T23:06:53Z
- status: ok
- log: `v2-async_small-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### smithy_small-get — timing rep 2

- started: 2026-08-31T23:08:00Z
- status: ok
- log: `smithy_small-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v1_small-put — timing rep 2

- started: 2026-08-31T23:08:33Z
- status: ok
- log: `v1_small-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-sync_small-put — timing rep 2

- started: 2026-08-31T23:09:20Z
- status: ok
- log: `v2-sync_small-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-async_small-put — timing rep 2

- started: 2026-08-31T23:10:18Z
- status: ok
- log: `v2-async_small-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### smithy_small-put — timing rep 2

- started: 2026-08-31T23:11:21Z
- status: ok
- log: `smithy_small-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v1_small-get — timing rep 3

- started: 2026-08-31T23:11:56Z
- status: ok
- log: `v1_small-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-sync_small-get — timing rep 3

- started: 2026-08-31T23:12:43Z
- status: ok
- log: `v2-sync_small-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-async_small-get — timing rep 3

- started: 2026-08-31T23:13:42Z
- status: ok
- log: `v2-async_small-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### smithy_small-get — timing rep 3

- started: 2026-08-31T23:14:46Z
- status: ok
- log: `smithy_small-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v1_small-put — timing rep 3

- started: 2026-08-31T23:15:18Z
- status: ok
- log: `v1_small-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-sync_small-put — timing rep 3

- started: 2026-08-31T23:16:06Z
- status: ok
- log: `v2-sync_small-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v2-async_small-put — timing rep 3

- started: 2026-08-31T23:17:01Z
- status: ok
- log: `v2-async_small-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### smithy_small-put — timing rep 3

- started: 2026-08-31T23:18:04Z
- status: ok
- log: `smithy_small-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --append-to-results-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/results.csv`

### v1_small-get — cpu profile

- started: 2026-08-31T23:18:38Z
- status: ok
- log: `v1_small-get/cpu.log`
- output: `v1_small-get/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v1_small-get/cpu.jfr`

### v2-sync_small-get — cpu profile

- started: 2026-08-31T23:19:25Z
- status: ok
- log: `v2-sync_small-get/cpu.log`
- output: `v2-sync_small-get/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-sync_small-get/cpu.jfr`

### v2-async_small-get — cpu profile

- started: 2026-08-31T23:20:22Z
- status: ok
- log: `v2-async_small-get/cpu.log`
- output: `v2-async_small-get/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-async_small-get/cpu.jfr`

### smithy_small-get — cpu profile

- started: 2026-08-31T23:21:27Z
- status: ok
- log: `smithy_small-get/cpu.log`
- output: `smithy_small-get/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/smithy_small-get/cpu.jfr`

### v1_small-put — cpu profile

- started: 2026-08-31T23:22:02Z
- status: ok
- log: `v1_small-put/cpu.log`
- output: `v1_small-put/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v1_small-put/cpu.jfr`

### v2-sync_small-put — cpu profile

- started: 2026-08-31T23:22:45Z
- status: ok
- log: `v2-sync_small-put/cpu.log`
- output: `v2-sync_small-put/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-sync_small-put/cpu.jfr`

### v2-async_small-put — cpu profile

- started: 2026-08-31T23:23:42Z
- status: ok
- log: `v2-async_small-put/cpu.log`
- output: `v2-async_small-put/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-async_small-put/cpu.jfr`

### smithy_small-put — cpu profile

- started: 2026-08-31T23:24:46Z
- status: ok
- log: `smithy_small-put/cpu.log`
- output: `smithy_small-put/cpu.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile cpu --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/smithy_small-put/cpu.jfr`

### v1_small-get — alloc profile

- started: 2026-08-31T23:25:19Z
- status: ok
- log: `v1_small-get/alloc.log`
- output: `v1_small-get/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v1_small-get/alloc.jfr`

### v2-sync_small-get — alloc profile

- started: 2026-08-31T23:26:07Z
- status: ok
- log: `v2-sync_small-get/alloc.log`
- output: `v2-sync_small-get/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-sync_small-get/alloc.jfr`

### v2-async_small-get — alloc profile

- started: 2026-08-31T23:27:05Z
- status: ok
- log: `v2-async_small-get/alloc.log`
- output: `v2-async_small-get/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-async_small-get/alloc.jfr`

### smithy_small-get — alloc profile

- started: 2026-08-31T23:28:11Z
- status: ok
- log: `smithy_small-get/alloc.log`
- output: `smithy_small-get/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/smithy_small-get/alloc.jfr`

### v1_small-put — alloc profile

- started: 2026-08-31T23:28:43Z
- status: ok
- log: `v1_small-put/alloc.log`
- output: `v1_small-put/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v1_small-put/alloc.jfr`

### v2-sync_small-put — alloc profile

- started: 2026-08-31T23:29:27Z
- status: ok
- log: `v2-sync_small-put/alloc.log`
- output: `v2-sync_small-put/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-sync_small-put/alloc.jfr`

### v2-async_small-put — alloc profile

- started: 2026-08-31T23:30:24Z
- status: ok
- log: `v2-async_small-put/alloc.log`
- output: `v2-async_small-put/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-async_small-put/alloc.jfr`

### smithy_small-put — alloc profile

- started: 2026-08-31T23:31:31Z
- status: ok
- log: `smithy_small-put/alloc.log`
- output: `smithy_small-put/alloc.jfr`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --profile alloc --profile-format jfr --profile-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/smithy_small-put/alloc.jfr`

### v1_small-get — sdk metrics

- started: 2026-08-31T23:32:05Z
- status: ok
- log: `v1_small-get/metrics.log`
- output: `v1_small-get/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v1_small-get/metrics.txt`

### v2-sync_small-get — sdk metrics

- started: 2026-08-31T23:32:52Z
- status: ok
- log: `v2-sync_small-get/metrics.log`
- output: `v2-sync_small-get/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-sync_small-get/metrics.txt`

### v2-async_small-get — sdk metrics

- started: 2026-08-31T23:33:53Z
- status: ok
- log: `v2-async_small-get/metrics.log`
- output: `v2-async_small-get/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-async_small-get/metrics.txt`

### smithy_small-get — sdk metrics

- started: 2026-08-31T23:35:05Z
- status: ok
- log: `smithy_small-get/metrics.log`
- output: `smithy_small-get/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-get --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/smithy_small-get/metrics.txt`

### v1_small-put — sdk metrics

- started: 2026-08-31T23:35:38Z
- status: ok
- log: `v1_small-put/metrics.log`
- output: `v1_small-put/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v1 --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v1_small-put/metrics.txt`

### v2-sync_small-put — sdk metrics

- started: 2026-08-31T23:36:22Z
- status: ok
- log: `v2-sync_small-put/metrics.log`
- output: `v2-sync_small-put/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-sync_small-put/metrics.txt`

### v2-async_small-put — sdk metrics

- started: 2026-08-31T23:37:22Z
- status: ok
- log: `v2-async_small-put/metrics.log`
- output: `v2-async_small-put/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-async --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/v2-async_small-put/metrics.txt`

### smithy_small-put — sdk metrics

- started: 2026-08-31T23:38:32Z
- status: ok
- log: `smithy_small-put/metrics.log`
- output: `smithy_small-put/metrics.txt`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client smithy --scenario small-put --iterations 200000 --warmup 20000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0-dirty.jar --pin-client 32-47 --pin-server 0-15 --jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --server-jvm-args -Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4 --metrics --metrics-file ../../pipeline_benchmark2/raw/crosssdk-254/small/20260831-2258/smithy_small-put/metrics.txt`

## Summary

- finished: 2026-08-31T23:39:06Z
- runs: 48, failures: 0
- results: `results.csv` (24 data rows)
