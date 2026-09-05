# Benchmark collection 20260904-2313

## Environment

- Date: 2026-09-04T23:13:18Z (UTC)
- Host: dev-dsk-alexwoo-2b-ee3cc828.us-west-2.amazon.com, Linux x86_64
- Hardware: Intel(R) Xeon(R) Platinum 8124M CPU @ 3.00GHz, 8 logical cores, 15 GiB
- Java: openjdk version "21.0.9" 2025-10-21 LTS
- Git: branch `smithy-java-bridge-alexwoo-full`, commit `ee9a74f69dbba396e4e6c67419210154b968f0ae`, dirty files: 54
- SDK V2 version: 2.46.11-SNAPSHOT
- Artifact under test: /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar
- Artifact provenance: phase=components git.commit=ee9a74f69dbba396e4e6c67419210154b968f0ae git.branch=smithy-java-bridge-alexwoo-full git.dirty.files=true sdk.commit=ee9a74f69db build.time=2026-09-04T23:12:13Z sdk.v2.version=2.46.11-SNAPSHOT sdk.v1.version=1.12.797 smithy.java.version=1.6.1 
- Benchmark module: test/standalone-e2e-benchmarks

## Parameters

- iterations: 60000
- warmup: 40000
- warmup ceiling: 120
- phases: timing only (--timing-only)
- timing repetitions per case: 4
- clients: v2-sync,v2-sync-strip-endpoints,v2-sync-strip-retries,v2-sync-strip-errors,v2-sync-strip-interceptors,v2-sync-stripped
- scenarios: small-get,small-put
- concurrency: 1 (sync clients use this many threads; async clients keep this many in flight)
- async mode: inflight
- pinning: client=[0,1] server=[4,5,6]
- client jvm args: (none)
- server jvm args: (none)
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
- Profiler recordings (async-profiler, JFR format) cover the whole JVM, including the 40000
  warmup ops and one-time client/connection setup (~40%
  of samples).
- The mock server shares the host with the client: ops_per_wall_sec includes contention effects;
  ops_per_cpu_sec / ops_per_user_cpu_sec count client-process CPU only.

## Runs

### v2-sync_small-get — timing rep 1

- started: 2026-09-04T23:13:18Z
- status: ok
- log: `v2-sync_small-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-endpoints_small-get — timing rep 1

- started: 2026-09-04T23:13:47Z
- status: ok
- log: `v2-sync-strip-endpoints_small-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-endpoints --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-retries_small-get — timing rep 1

- started: 2026-09-04T23:14:15Z
- status: ok
- log: `v2-sync-strip-retries_small-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-retries --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-errors_small-get — timing rep 1

- started: 2026-09-04T23:14:44Z
- status: ok
- log: `v2-sync-strip-errors_small-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-errors --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-interceptors_small-get — timing rep 1

- started: 2026-09-04T23:15:13Z
- status: ok
- log: `v2-sync-strip-interceptors_small-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-interceptors --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-stripped_small-get — timing rep 1

- started: 2026-09-04T23:15:41Z
- status: ok
- log: `v2-sync-stripped_small-get/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-stripped --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync_small-put — timing rep 1

- started: 2026-09-04T23:16:07Z
- status: ok
- log: `v2-sync_small-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-endpoints_small-put — timing rep 1

- started: 2026-09-04T23:16:35Z
- status: ok
- log: `v2-sync-strip-endpoints_small-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-endpoints --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-retries_small-put — timing rep 1

- started: 2026-09-04T23:17:03Z
- status: ok
- log: `v2-sync-strip-retries_small-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-retries --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-errors_small-put — timing rep 1

- started: 2026-09-04T23:17:31Z
- status: ok
- log: `v2-sync-strip-errors_small-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-errors --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-interceptors_small-put — timing rep 1

- started: 2026-09-04T23:17:59Z
- status: ok
- log: `v2-sync-strip-interceptors_small-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-interceptors --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-stripped_small-put — timing rep 1

- started: 2026-09-04T23:18:25Z
- status: ok
- log: `v2-sync-stripped_small-put/timing-rep1.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-stripped --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync_small-get — timing rep 2

- started: 2026-09-04T23:18:50Z
- status: ok
- log: `v2-sync_small-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-endpoints_small-get — timing rep 2

- started: 2026-09-04T23:19:19Z
- status: ok
- log: `v2-sync-strip-endpoints_small-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-endpoints --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-retries_small-get — timing rep 2

- started: 2026-09-04T23:19:48Z
- status: ok
- log: `v2-sync-strip-retries_small-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-retries --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-errors_small-get — timing rep 2

- started: 2026-09-04T23:20:16Z
- status: ok
- log: `v2-sync-strip-errors_small-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-errors --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-interceptors_small-get — timing rep 2

- started: 2026-09-04T23:20:45Z
- status: ok
- log: `v2-sync-strip-interceptors_small-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-interceptors --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-stripped_small-get — timing rep 2

- started: 2026-09-04T23:21:13Z
- status: ok
- log: `v2-sync-stripped_small-get/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-stripped --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync_small-put — timing rep 2

- started: 2026-09-04T23:21:39Z
- status: ok
- log: `v2-sync_small-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-endpoints_small-put — timing rep 2

- started: 2026-09-04T23:22:07Z
- status: ok
- log: `v2-sync-strip-endpoints_small-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-endpoints --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-retries_small-put — timing rep 2

- started: 2026-09-04T23:22:34Z
- status: ok
- log: `v2-sync-strip-retries_small-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-retries --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-errors_small-put — timing rep 2

- started: 2026-09-04T23:23:03Z
- status: ok
- log: `v2-sync-strip-errors_small-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-errors --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-interceptors_small-put — timing rep 2

- started: 2026-09-04T23:23:31Z
- status: ok
- log: `v2-sync-strip-interceptors_small-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-interceptors --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-stripped_small-put — timing rep 2

- started: 2026-09-04T23:23:57Z
- status: ok
- log: `v2-sync-stripped_small-put/timing-rep2.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-stripped --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync_small-get — timing rep 3

- started: 2026-09-04T23:24:23Z
- status: ok
- log: `v2-sync_small-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-endpoints_small-get — timing rep 3

- started: 2026-09-04T23:24:51Z
- status: ok
- log: `v2-sync-strip-endpoints_small-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-endpoints --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-retries_small-get — timing rep 3

- started: 2026-09-04T23:25:19Z
- status: ok
- log: `v2-sync-strip-retries_small-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-retries --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-errors_small-get — timing rep 3

- started: 2026-09-04T23:25:48Z
- status: ok
- log: `v2-sync-strip-errors_small-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-errors --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-interceptors_small-get — timing rep 3

- started: 2026-09-04T23:26:16Z
- status: ok
- log: `v2-sync-strip-interceptors_small-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-interceptors --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-stripped_small-get — timing rep 3

- started: 2026-09-04T23:26:44Z
- status: ok
- log: `v2-sync-stripped_small-get/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-stripped --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync_small-put — timing rep 3

- started: 2026-09-04T23:27:10Z
- status: ok
- log: `v2-sync_small-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-endpoints_small-put — timing rep 3

- started: 2026-09-04T23:27:38Z
- status: ok
- log: `v2-sync-strip-endpoints_small-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-endpoints --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-retries_small-put — timing rep 3

- started: 2026-09-04T23:28:05Z
- status: ok
- log: `v2-sync-strip-retries_small-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-retries --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-errors_small-put — timing rep 3

- started: 2026-09-04T23:28:32Z
- status: ok
- log: `v2-sync-strip-errors_small-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-errors --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-interceptors_small-put — timing rep 3

- started: 2026-09-04T23:29:00Z
- status: ok
- log: `v2-sync-strip-interceptors_small-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-interceptors --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-stripped_small-put — timing rep 3

- started: 2026-09-04T23:29:26Z
- status: ok
- log: `v2-sync-stripped_small-put/timing-rep3.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-stripped --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync_small-get — timing rep 4

- started: 2026-09-04T23:29:52Z
- status: ok
- log: `v2-sync_small-get/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-endpoints_small-get — timing rep 4

- started: 2026-09-04T23:30:21Z
- status: ok
- log: `v2-sync-strip-endpoints_small-get/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-endpoints --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-retries_small-get — timing rep 4

- started: 2026-09-04T23:30:48Z
- status: ok
- log: `v2-sync-strip-retries_small-get/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-retries --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-errors_small-get — timing rep 4

- started: 2026-09-04T23:31:16Z
- status: ok
- log: `v2-sync-strip-errors_small-get/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-errors --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-interceptors_small-get — timing rep 4

- started: 2026-09-04T23:31:46Z
- status: ok
- log: `v2-sync-strip-interceptors_small-get/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-interceptors --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-stripped_small-get — timing rep 4

- started: 2026-09-04T23:32:13Z
- status: ok
- log: `v2-sync-stripped_small-get/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-stripped --scenario small-get --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync_small-put — timing rep 4

- started: 2026-09-04T23:32:39Z
- status: ok
- log: `v2-sync_small-put/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-endpoints_small-put — timing rep 4

- started: 2026-09-04T23:33:07Z
- status: ok
- log: `v2-sync-strip-endpoints_small-put/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-endpoints --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-retries_small-put — timing rep 4

- started: 2026-09-04T23:33:34Z
- status: ok
- log: `v2-sync-strip-retries_small-put/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-retries --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-errors_small-put — timing rep 4

- started: 2026-09-04T23:34:03Z
- status: ok
- log: `v2-sync-strip-errors_small-put/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-errors --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-strip-interceptors_small-put — timing rep 4

- started: 2026-09-04T23:34:30Z
- status: ok
- log: `v2-sync-strip-interceptors_small-put/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-strip-interceptors --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

### v2-sync-stripped_small-put — timing rep 4

- started: 2026-09-04T23:34:57Z
- status: ok
- log: `v2-sync-stripped_small-put/timing-rep4.log`
- output: `results.csv`
- command (from test/standalone-e2e-benchmarks): `scripts/benchmark.sh --client v2-sync-stripped --scenario small-put --iterations 60000 --warmup 40000 --progress-seconds 0 --concurrency 1 --async-mode inflight --cpu-source auto --port 19080 --jar /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-components-ee9a74f69db-dirty.jar --warmup-max-seconds 120 --pin-client 0,1 --pin-server 4,5,6 --append-to-results-file /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/components/20260904-2313/results.csv`

## Summary

- finished: 2026-09-04T23:35:22Z
- runs: 48, failures: 0
- results: `results.csv` (48 data rows)
- cpu/alloc/metrics phases skipped (--timing-only)
