# Optimization experiment: racecar F

Racecar F: SigV4 fast-path header signer (`FastV4HeaderSigner`, `V4SigningResources`, `V4SigningKeyCache`). Task 9.20. Current decision: Deferred (later individual optimization).

| arm | ref | commit | SDK version |
|---|---|---|---|
| `feature` | `perf/racecar-e2e@a255b67e611c` | `a255b67e611c` | 2.49.7-SNAPSHOT |
| `candidate` | `cand/F@282d21499ae1` | `282d21499ae1` | 2.49.7-SNAPSHOT |

E2E: reps 7, iterations 50000, warmup 10000, clients v2-sync,v2-async, scenarios small-get,small-put,describe-table,batch-get,batch-put, openjdk version "25.0.4.1" 2026-08-18 LTS, ip-172-31-43-109.ec2.internal, Linux aarch64, Neoverse-N1, 64 logical cores. Null run `20261026-152046`, A/B run `20261026-164218`.

JMH: jmh-null-a `feature-a.json`, jmh-null-b `feature-b.json`, jmh-candidate `candidate.json`.

Note: 9.20's fast path sits in DefaultAwsV4HttpSigner, above the internal V4RequestSigner, so the target is AwsV4HttpSignerBenchmark (the public signer). It is in sdk-standard-benchmarks on both arms (its own commit before the candidate): sdk-benchmarks does not build on this branch (ec2 and cloudfront are in the off-by-default fleet-services profile, and protocol-tests is no longer a module). Sigv4SignerBenchmark (sdk-benchmarks, internal V4RequestSigner) was a guard and is dropped with it: F leaves that path unchanged apart from making three V4CanonicalRequest helpers package-private, so the e2e CPU and B/op guards cover the request path. Measured with smithy-java c62499ffd's Java 8 ideas folded in.

## Verdict: KEEP

Suggested decision: **Applied (re-implemented here)**.

- e2e v2-sync small-get app_cpu_us_per_op: -13.6%, beyond the 4.0% floor
- e2e v2-async small-get app_cpu_us_per_op: -10.9%, beyond the 3.4% floor
- e2e v2-sync small-put app_cpu_us_per_op: -13.2%, beyond the 2.2% floor
- e2e v2-async small-put app_cpu_us_per_op: -9.9%, beyond the 3.3% floor
- jmh signer.AwsV4HttpSignerBenchmark.sign [credentials=session] score: -41.5%, beyond the 14.4% floor
- jmh signer.AwsV4HttpSignerBenchmark.sign [credentials=basic] score: -36.9%, beyond the 13.9% floor

## Targets

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-sync small-get | app_cpu_us_per_op | 114.37 | 98.74 | -13.6% | 4.0% | gain |
| e2e | v2-async small-get | app_cpu_us_per_op | 159.76 | 142.29 | -10.9% | 3.4% | gain |
| e2e | v2-sync small-put | app_cpu_us_per_op | 108.87 | 94.50 | -13.2% | 2.2% | gain |
| e2e | v2-async small-put | app_cpu_us_per_op | 157.44 | 141.77 | -9.9% | 3.3% | gain |
| jmh | signer.AwsV4HttpSignerBenchmark.sign [credentials=session] | score | 9,469.00 | 5,543.47 | -41.5% | 14.4% | gain |
| jmh | signer.AwsV4HttpSignerBenchmark.sign [credentials=basic] | score | 8,990.25 | 5,676.69 | -36.9% | 13.9% | gain |

## Other scenarios and benchmarks (guards)

| source | case | metric | feature | candidate | change | floor | result |
|---|---|---|---:|---:|---:|---:|---|
| e2e | v2-sync small-get | alloc_bytes_per_op | 53,382.00 | 37,231.57 | -30.3% | 0.7% | ok |
| e2e | v2-async small-get | alloc_bytes_per_op | 54,235.71 | 38,716.29 | -28.6% | 0.3% | ok |
| e2e | v2-sync small-put | alloc_bytes_per_op | 47,699.71 | 31,502.00 | -34.0% | 1.0% | ok |
| e2e | v2-async small-put | alloc_bytes_per_op | 49,072.29 | 33,608.00 | -31.5% | 0.3% | ok |
| e2e | v2-sync describe-table | app_cpu_us_per_op | 139.14 | 125.34 | -9.9% | 1.9% | ok |
| e2e | v2-sync describe-table | alloc_bytes_per_op | 58,758.00 | 42,580.86 | -27.5% | 0.6% | ok |
| e2e | v2-async describe-table | app_cpu_us_per_op | 177.70 | 164.14 | -7.6% | 2.3% | ok |
| e2e | v2-async describe-table | alloc_bytes_per_op | 63,396.86 | 47,867.71 | -24.5% | 0.3% | ok |
| e2e | v2-sync batch-get | app_cpu_us_per_op | 429.54 | 411.91 | -4.0% | 4.1% | ok |
| e2e | v2-sync batch-get | alloc_bytes_per_op | 452,492.86 | 436,921.43 | -3.4% | 0.1% | ok |
| e2e | v2-async batch-get | app_cpu_us_per_op | 479.36 | 458.41 | -4.4% | 0.8% | ok |
| e2e | v2-async batch-get | alloc_bytes_per_op | 492,424.14 | 476,965.86 | -3.1% | 0.0% | ok |
| e2e | v2-sync batch-put | app_cpu_us_per_op | 362.84 | 349.84 | -3.6% | 1.3% | ok |
| e2e | v2-sync batch-put | alloc_bytes_per_op | 92,071.14 | 76,428.86 | -17.0% | 0.1% | ok |
| e2e | v2-async batch-put | app_cpu_us_per_op | 403.74 | 393.03 | -2.6% | 1.6% | ok |
| e2e | v2-async batch-put | alloc_bytes_per_op | 258,039.57 | 242,400.29 | -6.1% | 0.0% | ok |
| jmh | signer.AwsV4HttpSignerBenchmark.sign [credentials=session] | gc.alloc.rate.norm | 18,464.06 | 2,832.01 | -84.7% | 0.7% | ok |
| jmh | signer.AwsV4HttpSignerBenchmark.sign [credentials=basic] | gc.alloc.rate.norm | 17,992.01 | 2,684.01 | -85.1% | 0.7% | ok |

## YAML change

Apply by hand to `docs/design/schema-serde/racecar-decisions.yaml`, then run `python3 scripts/schema-serde/racecar_decisions.py` to regenerate the Markdown (and `--check`).

```yaml
- id: "F"
  # keep optimization, component and task as they are
  decision: "Applied (re-implemented here)"
  classes: ["<fully qualified class that implements it>"]
  applied_in_phase: 5
  reason: "<existing reason>; 9.19 experiment: v2-sync small-get app_cpu_us_per_op -13.6% (floor 4.0%); v2-async small-get app_cpu_us_per_op -10.9% (floor 3.4%); v2-sync small-put app_cpu_us_per_op -13.2% (floor 2.2%); v2-async small-put app_cpu_us_per_op -9.9% (floor 3.3%); signer.AwsV4HttpSignerBenchmark.sign [credentials=session] score -41.5% (floor 14.4%); signer.AwsV4HttpSignerBenchmark.sign [credentials=basic] score -36.9% (floor 13.9%)"
  ab_record: "optimizations/F.md"
```

## Decision applied: Applied (re-implemented here) (KEEP)

Every target gained well beyond its floor. Application CPU fell by 15.6 µs/op on v2-sync small-get (−13.6%, floor
4.0%), 17.5 µs/op on v2-async small-get (−10.9%, floor 3.4%), 14.4 µs/op on v2-sync small-put (−13.2%, floor 2.2%) and
15.7 µs/op on v2-async small-put (−9.9%, floor 3.3%). The public signer benchmark fell by 41.5% (session credentials)
and 36.9% (basic credentials) against floors of 14.4% and 13.9%. No guard regressed: every guard moved in the
improving direction. Allocation fell by 15.5–16.2 KB/op on every e2e scenario (−24% to −34% on small-get, small-put
and describe-table, −3% to −17% on the batch scenarios), which is one signing call per operation. The JMH allocation fell from about
18.0–18.5 KB to 2.7–2.8 KB per sign. No run failed (140/140 in each run). `cand/F` is merged into
`perf/racecar-e2e`, and the row in `racecar-decisions.yaml` is now `Applied (re-implemented here)` with `classes`
(`FastV4HeaderSigner`, `V4SigningResources`, `V4SigningKeyCache`), `applied_in_phase: 5` and this record.

The JMH floors are wide (14.4% and 13.9%) because the second feature run had a large error (±1,396 and ±1,288
ns/op, about 14% of the score). The candidate's gain is about three times the floor in both cases, so this does not
affect the decision. The e2e CPU gain also shows on the guards: describe-table −9.9% (v2-sync) and −7.6% (v2-async),
and batch-get/batch-put −2.6% to −4.4%, each a single signing call per operation.

## Combined design: F and smithy-java `c62499ffd`

`c62499ffd` (smithy-java, "Improve SigV4 performance") rewrites smithy-java's SigV4 signer around per-thread
signing resources. F already has the same structure (`V4SigningResources` per thread, `V4SigningKeyCache`, a cached
date string per second, in-place header sort). Each upstream idea and what happened to it here:

| upstream idea (`c62499ffd`) | here |
|---|---|
| Per-thread reusable buffers, digest and header table (`SigningResources`, `StripedPool`) | already in F (`V4SigningResources`, a `ThreadLocal`) |
| Derived signing key memoized per (secret, day, region, service) | already in F (`V4SigningKeyCache`, a `BoundedCache`; upstream memoizes the last key per thread instead) |
| Date strings formatted once per second | already in F (`V4SigningResources.dateTime`/`dateStamp`) |
| HMAC from precomputed inner and outer pad digests (`prepareHmacPads`), cloned per use, `Mac` fallback | **added**: `V4SigningResources.hmacSha256`/`preparePads`, keyed by the identity of the `V4SigningKeyCache` array, per thread; falls back to `Mac.init` if the digest cannot be cloned |
| Two-digit hex table (`HEX2`) | **added**: `FastV4HeaderSigner.HEX2` for the payload hash, canonical request hash and signature |
| Reuse the previous call's header order when the names match (`sameNames`, `sortAndMemoize`) | not added: F sorts in place with an insertion sort and allocates nothing, so there is no per-call allocation to save (the plan's condition) |
| SWAR uppercase and whitespace checks through `VarHandle`; Latin-1 `String` internals (`CompactStringAccess`) | excluded: JDK 9+ APIs (FU-23, Java 8 target) |
| Query pairs sorted in place | not added: the harness requests have no query string, and F keeps the legacy `V4CanonicalRequest` helpers so both paths canonicalize the same way |

**Where the HMAC state lives.** The plan suggested keying a cloned `Mac` off the cache entry. A `Mac` clone does not
save the pad hashing (`HmacCore` absorbs the inner pad lazily on the first update), and sharing one prototype across
threads relies on concurrent `clone()` of a mutable object. So the pad digests are kept per thread in
`V4SigningResources`, as upstream does, and keyed by the identity of the key array, which `V4SigningKeyCache`
returns unchanged for the same scope. A design probe (laptop, 5 M HMACs of a 200-byte string to sign with a 32-byte
key) compared three options:

| HMAC strategy | JDK 21 ns/op | JDK 25 ns/op | B/op |
|---|---:|---:|---:|
| A: per-thread `Mac`, `init` per call (opt/F) | 158–163 | 162 | 144 |
| B: pad digests cloned per call (`c62499ffd`, chosen) | 95–96 | 94–96 | 384 |
| C: own `Mac`, `init` only when the key array changes | 128–130 | 131–136 | 48 |

B is the fastest and is the upstream design; it allocates 240 B more per sign than A (the two digest clones). The
target metric is CPU, and the e2e allocation still fell by 15.5–16.2 KB/op, so B was kept.

## Candidate, settings and tests

- **Candidate:** `cand/F` `282d21499ae`, one commit on `perf/racecar-e2e` `a255b67e611`: the cherry-pick of `opt/F`
  `d1161cfc5ac` (clean; none of its files had changed on the base) without its benchmark class, plus the two
  `c62499ffd` ideas above. It touches `DefaultAwsV4HttpSigner` (dispatch to the fast path), `V4CanonicalRequest`
  (three helpers and the ignore list made package-private, no other change), and adds `FastV4HeaderSigner`,
  `V4SigningResources`, `V4SigningKeyCache` and `FastV4HeaderSignerDifferentialTest`.
- **Benchmark and targets (commits before the candidate):**
  - `4b0c9171e7d` adds `AwsV4HttpSignerBenchmark` (from `opt/F`) to `test/sdk-standard-benchmarks`, so both arms'
    benchmark jars contain the target.
  - `a255b67e611` moves F's JMH target in `targets.json` from `sdk-benchmarks` to `sdk-standard-benchmarks` and drops
    the `Sigv4SignerBenchmark` guard. Reason: `sdk-benchmarks` does not build on this branch. It depends on `ec2` and
    `cloudfront`, which are in the off-by-default `fleet-services` profile, and on `protocol-tests`, which is no
    longer a module (Requirement 32.9). The dropped guard measured the internal `V4RequestSigner`, which F leaves
    unchanged apart from the visibility change above, so it could only report noise. The e2e CPU and B/op guards
    cover the request path. Approved by the orchestrator (2026-10-26).
- **Null experiment: fresh**, run `20261026-152046`. E13's null (`20261025-195609`, feature `7cd6a5db432`) has the
  same SDK code, but could not be reused. In jar mode the script takes the feature commit from the e2e feature jar
  and refuses a feature JMH jar built at another commit, and the JMH feature arm had to contain the benchmark commit.
  So the feature e2e jar was rebuilt at `a255b67e611`: `git diff 7cd6a5db432 a255b67e611 -- . ':!docs' ':!scripts'
  ':!.kiro' ':!.changes'` shows only the new benchmark class, which is not in the e2e jar. In the null run, run 3
  (feature-a, v2-async small-get, rep 1) has `steady_state=false` (`jit_ms=201`); its application CPU (7,978 ms, others
  7,749–8,200) and B/op (−0.12% from the mean) are inside the other repetitions' range. In the A/B, run 48
  (candidate, v2-async small-put, rep 3) has `steady_state=false` (`jit_ms=225`); application CPU 7,209 ms (others
  6,909–7,213), B/op +0.05% from the mean. No floor or result depends on either run.
- **Jars:** feature `e2e-a255b67e611c-747a92d7b67b.jar` (sha256 `747a92d7…`), candidate
  `e2e-282d21499ae1-990797f1649d.jar` (sha256 `990797f1…`), both from harness commit `fed6d903057` with 0 dirty
  files. JMH: `sdk-standard-benchmarks-a255b67e611c-177b4ec6bc7e.jar` and
  `sdk-standard-benchmarks-282d21499ae1-f3dd6a580b5e.jar`. All built locally with `optimization_experiment.sh
  --build-only` (JDK 21.0.2) and deployed with `e2e-remote.sh deploy`.
- **Host run:** session `F-20261026-151130` on the m6g.metal host, 2026-10-26 15:11–18:02 UTC, `--jmh-first`: JMH
  15:11–15:21 (it did not decide REVERT), null run `20261026-152046` (15:21–16:42), A/B run `20261026-164218`
  (16:42–18:02). Host `EXPERIMENT_EXIT=0` (KEEP). This record was re-rendered locally from the fetched
  `paired-ab.json` and JMH files; the local exit code was also 0.
- **Settings:** 7 repetitions, 50,000 operations and 10,000 warmup per run, concurrency 1, both clients, all five
  scenarios. Client pinned to CPUs 32–47, server to 0–15. Client and server JVM options:
  `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`. JMH: the class defaults (3 × 5 s
  warmup, 3 × 10 s measurement, 2 forks), `-prof gc`, pinned to 32–47 with the same JVM options appended, run as
  feature, candidate, feature. JDK 25.0.4.1.
- **Tests before the experiment** (on `cand/F` `3d1f89db0fb`, whose `core/` tree equals `282d21499ae`):
  - `FastV4HeaderSignerDifferentialTest`: 4 tests, 0 failures. 10,000 randomized requests (seed 20260920); 4 threads
    × 1,000; new: 4 threads × 2,000 requests whose signing scope (secret, date across a UTC midnight, region,
    service) changes or repeats between calls, to exercise the per-thread pad state; new: `hmacSha256` against `Mac`
    for keys of 1–130 bytes that change, repeat, or repeat with equal content in a new array, on both the clone and
    the `Mac.init` fallback path. A mutation (`0x36` → `0x37` in the inner pad) made 4 of the 4 tests fail.
  - `install -pl :http-auth-aws`: JUnit 261 tests, 0 failures, 6 skipped; TestNG 166, 0 failures, 74 skipped.
    Checkstyle, SpotBugs and japicmp passed. Existing tests were not modified.
  - `install -pl :sdk-standard-benchmarks`: `SerdeBenchmarkEquivalenceTest` 142/142, module 190/0/0.
  - `test -pl :smithy-protocol-tests`: 923 run, 0 failures, 16 skipped, the same 16 ids as before the marshall
    series.
  - After the decision, `install -pl :architecture-tests`: 79 tests, 0 failures, 11 skipped (the existing skips).
    `RacecarDecisionTableTest` (2/0) loads the three classes from the new row and finds this record.

## JMH runs (feature, candidate, feature again)

| benchmark | params | metric | feature-a | candidate | feature-b |
|---|---|---|---:|---:|---:|
| software.amazon.awssdk.benchmark.signer.AwsV4HttpSignerBenchmark.sign | credentials=session | score | 9,469.005 ± 104.671 | 5,543.468 ± 192.242 | 9,688.334 ± 1,395.815 |
| software.amazon.awssdk.benchmark.signer.AwsV4HttpSignerBenchmark.sign | credentials=session | gc.alloc.rate | 1,859.600 ± 20.170 | 487.258 ± 16.865 | 1,823.668 ± 250.639 |
| software.amazon.awssdk.benchmark.signer.AwsV4HttpSignerBenchmark.sign | credentials=session | gc.alloc.rate.norm | 18,464.062 ± 49.151 | 2,832.006 ± 0.000 | 18,488.071 ± 122.865 |
| software.amazon.awssdk.benchmark.signer.AwsV4HttpSignerBenchmark.sign | credentials=session | gc.count | 92.000 ± - | 24.000 ± - | 89.000 ± - |
| software.amazon.awssdk.benchmark.signer.AwsV4HttpSignerBenchmark.sign | credentials=session | gc.time | 62.000 ± - | 38.000 ± - | 62.000 ± - |
| software.amazon.awssdk.benchmark.signer.AwsV4HttpSignerBenchmark.sign | credentials=basic | score | 8,990.251 ± 456.751 | 5,676.689 ± 423.998 | 9,284.044 ± 1,288.322 |
| software.amazon.awssdk.benchmark.signer.AwsV4HttpSignerBenchmark.sign | credentials=basic | gc.alloc.rate | 1,908.984 ± 83.974 | 451.214 ± 39.831 | 1,848.981 ± 252.806 |
| software.amazon.awssdk.benchmark.signer.AwsV4HttpSignerBenchmark.sign | credentials=basic | gc.alloc.rate.norm | 17,992.006 ± 122.874 | 2,684.006 ± 36.862 | 17,964.041 ± 36.757 |
| software.amazon.awssdk.benchmark.signer.AwsV4HttpSignerBenchmark.sign | credentials=basic | gc.count | 94.000 ± - | 22.000 ± - | 91.000 ± - |
| software.amazon.awssdk.benchmark.signer.AwsV4HttpSignerBenchmark.sign | credentials=basic | gc.time | 63.000 ± - | 38.000 ± - | 60.000 ± - |

## Null experiment (e2e)

### Paired A/B summary `20261026-152046`

Kind: null experiment (the same build as both arms). Baseline arm: `feature-a`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature-a` | `(prebuilt jar)` | `a255b67e611c` | 2.49.7-SNAPSHOT | schema |
| `feature-b` | `(prebuilt jar)` | `a255b67e611c` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 50000, warmup 10000, reps 7, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "25.0.4.1" 2026-08-18 LTS, ip-172-31-43-109.ec2.internal, Linux aarch64.

#### application CPU µs/op

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 116.2 ± 6.9 (7) | 114.5 ± 10.6 (7) | 0.986 | ±4.0% | 4/7 | 4.0% | noise floor |
| v2-async | small-get | 159.9 ± 7.6 (7) | 158.6 ± 8.4 (7) | 0.992 | ±3.4% | 4/7 | 3.4% | noise floor |
| v2-sync | small-put | 110.5 ± 5.8 (7) | 109.2 ± 6.3 (7) | 0.989 | ±2.2% | 5/7 | 2.2% | noise floor |
| v2-async | small-put | 158.8 ± 9.5 (7) | 158.5 ± 9.4 (7) | 0.999 | ±3.3% | 5/7 | 3.3% | noise floor |
| v2-sync | describe-table | 138.3 ± 4.1 (7) | 138.9 ± 3.6 (7) | 1.004 | ±1.9% | 3/7 | 1.9% | noise floor |
| v2-async | describe-table | 180.0 ± 5.3 (7) | 176.0 ± 4.1 (7) | 0.977 | ±0.8% | 7/7 | 2.3% | noise floor |
| v2-sync | batch-get | 425.2 ± 20.3 (7) | 426.3 ± 28.1 (7) | 1.003 | ±4.1% | 3/7 | 4.1% | noise floor |
| v2-async | batch-get | 479.5 ± 11.7 (7) | 482.0 ± 6.9 (7) | 1.005 | ±0.8% | 2/7 | 0.8% | noise floor |
| v2-sync | batch-put | 363.7 ± 4.3 (7) | 361.8 ± 10.0 (7) | 0.995 | ±1.3% | 4/7 | 1.3% | noise floor |
| v2-async | batch-put | 404.5 ± 11.7 (7) | 403.4 ± 18.2 (7) | 0.997 | ±1.6% | 4/7 | 1.6% | noise floor |

#### wall µs/op (1/throughput)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 159.9 ± 7.1 (7) | 157.4 ± 10.2 (7) | 0.984 | ±2.9% | 5/7 | 2.9% | noise floor |
| v2-async | small-get | 191.3 ± 7.1 (7) | 190.3 ± 6.1 (7) | 0.995 | ±2.4% | 2/7 | 2.4% | noise floor |
| v2-sync | small-put | 154.7 ± 5.2 (7) | 152.9 ± 6.4 (7) | 0.988 | ±1.7% | 6/7 | 1.7% | noise floor |
| v2-async | small-put | 190.6 ± 8.4 (7) | 190.6 ± 9.4 (7) | 1.000 | ±2.3% | 5/7 | 2.3% | noise floor |
| v2-sync | describe-table | 182.4 ± 5.2 (7) | 183.7 ± 5.1 (7) | 1.007 | ±1.9% | 2/7 | 1.9% | noise floor |
| v2-async | describe-table | 212.4 ± 6.5 (7) | 209.0 ± 3.9 (7) | 0.984 | ±0.8% | 7/7 | 1.6% | noise floor |
| v2-sync | batch-get | 475.2 ± 21.3 (7) | 476.5 ± 27.3 (7) | 1.003 | ±3.7% | 3/7 | 3.7% | noise floor |
| v2-async | batch-get | 515.8 ± 11.0 (7) | 518.3 ± 6.5 (7) | 1.005 | ±0.7% | 2/7 | 0.7% | noise floor |
| v2-sync | batch-put | 415.5 ± 6.3 (7) | 413.3 ± 9.8 (7) | 0.995 | ±0.7% | 5/7 | 0.7% | noise floor |
| v2-async | batch-put | 457.8 ± 14.3 (7) | 457.6 ± 17.9 (7) | 1.000 | ±1.2% | 4/7 | 1.2% | noise floor |

#### mean latency µs

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 159.6 ± 7.1 (7) | 157.1 ± 10.1 (7) | 0.984 | ±2.9% | 5/7 | 2.9% | noise floor |
| v2-async | small-get | 183.4 ± 6.4 (7) | 182.8 ± 5.2 (7) | 0.996 | ±2.1% | 3/7 | 2.1% | noise floor |
| v2-sync | small-put | 154.4 ± 5.1 (7) | 152.6 ± 6.5 (7) | 0.988 | ±1.7% | 6/7 | 1.7% | noise floor |
| v2-async | small-put | 183.2 ± 7.8 (7) | 183.2 ± 8.5 (7) | 1.000 | ±2.2% | 5/7 | 2.2% | noise floor |
| v2-sync | describe-table | 182.0 ± 5.4 (7) | 183.3 ± 5.3 (7) | 1.007 | ±2.0% | 2/7 | 2.0% | noise floor |
| v2-async | describe-table | 204.3 ± 6.2 (7) | 201.5 ± 3.7 (7) | 0.987 | ±1.0% | 7/7 | 1.3% | noise floor |
| v2-sync | batch-get | 474.5 ± 21.2 (7) | 475.7 ± 27.4 (7) | 1.003 | ±3.7% | 3/7 | 3.7% | noise floor |
| v2-async | batch-get | 507.7 ± 9.8 (7) | 509.9 ± 6.0 (7) | 1.004 | ±0.6% | 2/7 | 0.6% | noise floor |
| v2-sync | batch-put | 414.9 ± 6.3 (7) | 412.8 ± 9.8 (7) | 0.995 | ±0.7% | 5/7 | 0.7% | noise floor |
| v2-async | batch-put | 448.9 ± 14.0 (7) | 449.0 ± 16.9 (7) | 1.000 | ±1.1% | 3/7 | 1.1% | noise floor |

#### allocated B/op (client JVM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 53,369.4 ± 658.9 (7) | 53,249.4 ± 229.8 (7) | 0.998 | ±0.7% | 3/7 | 0.7% | noise floor |
| v2-async | small-get | 54,236.7 ± 160.2 (7) | 54,248.3 ± 200.6 (7) | 1.000 | ±0.3% | 3/7 | 0.3% | noise floor |
| v2-sync | small-put | 47,767.1 ± 573.2 (7) | 47,826.6 ± 658.9 (7) | 1.001 | ±1.0% | 2/7 | 1.0% | noise floor |
| v2-async | small-put | 49,103.0 ± 171.2 (7) | 49,171.4 ± 228.2 (7) | 1.001 | ±0.3% | 1/7 | 0.3% | noise floor |
| v2-sync | describe-table | 58,716.9 ± 545.7 (7) | 58,738.9 ± 596.1 (7) | 1.000 | ±0.6% | 3/7 | 0.6% | noise floor |
| v2-async | describe-table | 63,368.3 ± 205.6 (7) | 63,365.9 ± 248.6 (7) | 1.000 | ±0.3% | 4/7 | 0.3% | noise floor |
| v2-sync | batch-get | 452,667.7 ± 584.1 (7) | 452,720.4 ± 1,039.4 (7) | 1.000 | ±0.1% | 4/7 | 0.1% | noise floor |
| v2-async | batch-get | 492,532.3 ± 175.1 (7) | 492,454.7 ± 185.0 (7) | 1.000 | ±0.0% | 5/7 | 0.0% | noise floor |
| v2-sync | batch-put | 92,058.6 ± 131.5 (7) | 92,012.9 ± 173.6 (7) | 1.000 | ±0.1% | 4/7 | 0.1% | noise floor |
| v2-async | batch-put | 257,993.7 ± 251.5 (7) | 258,077.7 ± 245.0 (7) | 1.000 | ±0.0% | 2/7 | 0.0% | noise floor |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature-a mean ± error (n) | feature-b mean ± error (n) | feature-b ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 118.8 ± 7.3 (7) | 117.8 ± 10.9 (7) | 0.992 | ±4.5% | 4/7 | 4.5% | noise floor |
| v2-async | small-get | 164.6 ± 7.7 (7) | 163.3 ± 8.3 (7) | 0.993 | ±3.3% | 4/7 | 3.3% | noise floor |
| v2-sync | small-put | 113.6 ± 5.7 (7) | 112.1 ± 6.7 (7) | 0.987 | ±2.1% | 5/7 | 2.1% | noise floor |
| v2-async | small-put | 162.7 ± 9.6 (7) | 162.2 ± 9.3 (7) | 0.998 | ±3.1% | 5/7 | 3.1% | noise floor |
| v2-sync | describe-table | 141.2 ± 3.5 (7) | 142.4 ± 3.1 (7) | 1.008 | ±1.5% | 1/7 | 1.5% | noise floor |
| v2-async | describe-table | 184.5 ± 6.1 (7) | 180.5 ± 3.7 (7) | 0.979 | ±1.0% | 7/7 | 2.1% | noise floor |
| v2-sync | batch-get | 429.3 ± 20.7 (7) | 429.9 ± 27.3 (7) | 1.002 | ±4.0% | 3/7 | 4.0% | noise floor |
| v2-async | batch-get | 486.2 ± 11.7 (7) | 489.0 ± 6.8 (7) | 1.006 | ±0.8% | 1/7 | 0.8% | noise floor |
| v2-sync | batch-put | 367.1 ± 5.0 (7) | 365.1 ± 9.7 (7) | 0.995 | ±1.3% | 4/7 | 1.3% | noise floor |
| v2-async | batch-put | 413.7 ± 12.1 (7) | 413.6 ± 18.4 (7) | 1.000 | ±1.6% | 4/7 | 1.6% | noise floor |

## Paired A/B (e2e)

### Paired A/B summary `20261026-164218`

Kind: comparison. Baseline arm: `feature`.

| arm | ref | commit | SDK version | pipeline |
|---|---|---|---|---|
| `feature` | `(prebuilt jar)` | `a255b67e611c` | 2.49.7-SNAPSHOT | schema |
| `candidate` | `(prebuilt jar)` | `282d21499ae1` | 2.49.7-SNAPSHOT | schema |

Parameters: iterations 50000, warmup 10000, reps 7, concurrency 1, JVM options `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`, openjdk version "25.0.4.1" 2026-08-18 LTS, ip-172-31-43-109.ec2.internal, Linux aarch64.

#### application CPU µs/op

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 114.4 ± 5.1 (7) | 98.7 ± 6.6 (7) | 0.864 | ±2.9% | 7/7 | 4.0% | -13.6% (beyond the floor) |
| v2-async | small-get | 159.8 ± 11.0 (7) | 142.3 ± 11.4 (7) | 0.891 | ±3.7% | 7/7 | 3.4% | -10.9% (beyond the floor) |
| v2-sync | small-put | 108.9 ± 4.7 (7) | 94.5 ± 6.8 (7) | 0.868 | ±2.8% | 7/7 | 2.2% | -13.2% (beyond the floor) |
| v2-async | small-put | 157.4 ± 8.5 (7) | 141.8 ± 5.4 (7) | 0.901 | ±2.6% | 7/7 | 3.3% | -9.9% (beyond the floor) |
| v2-sync | describe-table | 139.1 ± 6.8 (7) | 125.3 ± 10.4 (7) | 0.901 | ±3.5% | 7/7 | 1.9% | -9.9% (beyond the floor) |
| v2-async | describe-table | 177.7 ± 6.9 (7) | 164.1 ± 6.3 (7) | 0.924 | ±2.2% | 7/7 | 2.3% | -7.6% (beyond the floor) |
| v2-sync | batch-get | 429.5 ± 25.5 (7) | 411.9 ± 9.0 (7) | 0.960 | ±2.6% | 6/7 | 4.1% | no measurable difference |
| v2-async | batch-get | 479.4 ± 18.3 (7) | 458.4 ± 18.4 (7) | 0.956 | ±1.1% | 7/7 | 0.8% | -4.4% (beyond the floor) |
| v2-sync | batch-put | 362.8 ± 6.9 (7) | 349.8 ± 13.2 (7) | 0.964 | ±1.6% | 7/7 | 1.3% | -3.6% (beyond the floor) |
| v2-async | batch-put | 403.7 ± 16.5 (7) | 393.0 ± 13.5 (7) | 0.974 | ±1.5% | 7/7 | 1.6% | -2.6% (beyond the floor) |

#### wall µs/op (1/throughput)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 157.6 ± 5.2 (7) | 143.1 ± 6.4 (7) | 0.909 | ±1.9% | 7/7 | 2.9% | -9.1% (beyond the floor) |
| v2-async | small-get | 191.7 ± 11.6 (7) | 174.0 ± 12.3 (7) | 0.908 | ±3.6% | 7/7 | 2.4% | -9.2% (beyond the floor) |
| v2-sync | small-put | 152.7 ± 4.7 (7) | 138.3 ± 4.5 (7) | 0.906 | ±1.5% | 7/7 | 1.7% | -9.4% (beyond the floor) |
| v2-async | small-put | 189.2 ± 8.9 (7) | 175.0 ± 3.8 (7) | 0.925 | ±2.2% | 7/7 | 2.3% | -7.5% (beyond the floor) |
| v2-sync | describe-table | 183.9 ± 7.5 (7) | 170.1 ± 10.7 (7) | 0.925 | ±2.9% | 7/7 | 1.9% | -7.5% (beyond the floor) |
| v2-async | describe-table | 210.8 ± 6.3 (7) | 197.6 ± 5.5 (7) | 0.937 | ±1.8% | 7/7 | 1.6% | -6.3% (beyond the floor) |
| v2-sync | batch-get | 480.0 ± 26.0 (7) | 462.7 ± 10.0 (7) | 0.965 | ±2.3% | 6/7 | 3.7% | no measurable difference |
| v2-async | batch-get | 515.1 ± 16.5 (7) | 495.1 ± 17.8 (7) | 0.961 | ±1.0% | 7/7 | 0.7% | -3.9% (beyond the floor) |
| v2-sync | batch-put | 415.7 ± 7.2 (7) | 402.4 ± 13.5 (7) | 0.968 | ±1.3% | 7/7 | 0.7% | -3.2% (beyond the floor) |
| v2-async | batch-put | 458.3 ± 17.5 (7) | 447.3 ± 13.1 (7) | 0.976 | ±1.2% | 7/7 | 1.2% | -2.4% (beyond the floor) |

#### mean latency µs

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 157.2 ± 5.1 (7) | 142.9 ± 6.4 (7) | 0.909 | ±1.9% | 7/7 | 2.9% | -9.1% (beyond the floor) |
| v2-async | small-get | 183.9 ± 10.9 (7) | 167.4 ± 11.7 (7) | 0.911 | ±3.6% | 7/7 | 2.1% | -8.9% (beyond the floor) |
| v2-sync | small-put | 152.4 ± 4.6 (7) | 138.1 ± 4.5 (7) | 0.906 | ±1.6% | 7/7 | 1.7% | -9.4% (beyond the floor) |
| v2-async | small-put | 181.4 ± 8.3 (7) | 168.4 ± 3.4 (7) | 0.928 | ±2.1% | 7/7 | 2.2% | -7.2% (beyond the floor) |
| v2-sync | describe-table | 183.5 ± 7.5 (7) | 169.7 ± 10.7 (7) | 0.925 | ±3.0% | 7/7 | 2.0% | -7.5% (beyond the floor) |
| v2-async | describe-table | 203.1 ± 5.6 (7) | 190.9 ± 5.2 (7) | 0.940 | ±1.7% | 7/7 | 1.3% | -6.0% (beyond the floor) |
| v2-sync | batch-get | 479.3 ± 26.1 (7) | 462.1 ± 10.0 (7) | 0.965 | ±2.3% | 6/7 | 3.7% | no measurable difference |
| v2-async | batch-get | 507.1 ± 15.4 (7) | 487.7 ± 17.5 (7) | 0.962 | ±1.0% | 7/7 | 0.6% | -3.8% (beyond the floor) |
| v2-sync | batch-put | 415.1 ± 7.1 (7) | 401.8 ± 13.4 (7) | 0.968 | ±1.3% | 7/7 | 0.7% | -3.2% (beyond the floor) |
| v2-async | batch-put | 449.9 ± 16.9 (7) | 439.1 ± 12.4 (7) | 0.976 | ±1.1% | 7/7 | 1.1% | -2.4% (beyond the floor) |

#### allocated B/op (client JVM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 53,382.0 ± 481.5 (7) | 37,231.6 ± 593.8 (7) | 0.697 | ±0.6% | 7/7 | 0.7% | -30.3% (beyond the floor) |
| v2-async | small-get | 54,235.7 ± 160.7 (7) | 38,716.3 ± 219.0 (7) | 0.714 | ±0.2% | 7/7 | 0.3% | -28.6% (beyond the floor) |
| v2-sync | small-put | 47,699.7 ± 151.4 (7) | 31,502.0 ± 166.1 (7) | 0.660 | ±0.2% | 7/7 | 1.0% | -34.0% (beyond the floor) |
| v2-async | small-put | 49,072.3 ± 114.8 (7) | 33,608.0 ± 129.1 (7) | 0.685 | ±0.1% | 7/7 | 0.3% | -31.5% (beyond the floor) |
| v2-sync | describe-table | 58,758.0 ± 706.0 (7) | 42,580.9 ± 542.8 (7) | 0.725 | ±0.7% | 7/7 | 0.6% | -27.5% (beyond the floor) |
| v2-async | describe-table | 63,396.9 ± 212.5 (7) | 47,867.7 ± 191.2 (7) | 0.755 | ±0.2% | 7/7 | 0.3% | -24.5% (beyond the floor) |
| v2-sync | batch-get | 452,492.9 ± 406.6 (7) | 436,921.4 ± 81.2 (7) | 0.966 | ±0.0% | 7/7 | 0.1% | -3.4% (beyond the floor) |
| v2-async | batch-get | 492,424.1 ± 167.5 (7) | 476,965.9 ± 308.4 (7) | 0.969 | ±0.0% | 7/7 | 0.0% | -3.1% (beyond the floor) |
| v2-sync | batch-put | 92,071.1 ± 47.0 (7) | 76,428.9 ± 66.1 (7) | 0.830 | ±0.0% | 7/7 | 0.1% | -17.0% (beyond the floor) |
| v2-async | batch-put | 258,039.6 ± 112.7 (7) | 242,400.3 ± 319.6 (7) | 0.939 | ±0.1% | 7/7 | 0.0% | -6.1% (beyond the floor) |

#### process CPU µs/op (includes JIT/GC/VM)

| client | scenario | feature mean ± error (n) | candidate mean ± error (n) | candidate ratio | spread | lower/pairs | floor | verdict |
|---|---|---:|---:|---:|---:|---:|---:|---|
| v2-sync | small-get | 117.1 ± 5.4 (7) | 99.8 ± 6.9 (7) | 0.853 | ±2.8% | 7/7 | 4.5% | -14.7% (beyond the floor) |
| v2-async | small-get | 164.5 ± 10.7 (7) | 144.9 ± 11.9 (7) | 0.881 | ±3.5% | 7/7 | 3.3% | -11.9% (beyond the floor) |
| v2-sync | small-put | 111.9 ± 4.5 (7) | 95.6 ± 6.8 (7) | 0.855 | ±3.1% | 7/7 | 2.1% | -14.5% (beyond the floor) |
| v2-async | small-put | 161.2 ± 8.8 (7) | 145.2 ± 6.2 (7) | 0.901 | ±2.6% | 7/7 | 3.1% | -9.9% (beyond the floor) |
| v2-sync | describe-table | 141.9 ± 6.8 (7) | 127.2 ± 10.4 (7) | 0.896 | ±3.6% | 7/7 | 1.5% | -10.4% (beyond the floor) |
| v2-async | describe-table | 182.0 ± 7.4 (7) | 168.1 ± 6.6 (7) | 0.924 | ±2.2% | 7/7 | 2.1% | -7.6% (beyond the floor) |
| v2-sync | batch-get | 433.3 ± 24.4 (7) | 416.0 ± 8.8 (7) | 0.961 | ±2.5% | 6/7 | 4.0% | no measurable difference |
| v2-async | batch-get | 486.3 ± 18.3 (7) | 464.7 ± 18.2 (7) | 0.956 | ±1.1% | 7/7 | 0.8% | -4.4% (beyond the floor) |
| v2-sync | batch-put | 365.9 ± 6.3 (7) | 352.9 ± 13.5 (7) | 0.965 | ±1.6% | 7/7 | 1.3% | -3.5% (beyond the floor) |
| v2-async | batch-put | 413.6 ± 16.1 (7) | 401.9 ± 12.9 (7) | 0.972 | ±1.4% | 7/7 | 1.6% | -2.8% (beyond the floor) |
