# Serde performance report: schema-based serde vs legacy serde (EC2 metal)

This report compares the cost of request serialization and response deserialization (serde) between the
baseline build and the feature build. The baseline is stock V2 with the legacy marshallers and unmarshallers.
The feature build uses schema-based serde with the codecs ported from smithy-java. All results come from the 10
`benchmark.serde` JMH classes of `test/sdk-standard-benchmarks`, run on a dedicated m6g.metal host. This report covers
serde only. The end-to-end report (`perf-report.md`) is separate and still preliminary.

## 1. Summary

Across the five protocols, the feature build's serde is **2.6× faster (ratio 0.384) and allocates 31% less (ratio
0.689)**. This is the serde-only estimate, with the FU-12 blob member fix included. Each figure is the geometric mean
of the five per-protocol geometric means of feature / baseline. Lower ratios favor the feature build.

Measured as each arm's whole timed method (the as-measured view), the same aggregate is 2.5× faster (0.403) with 47%
less allocation (0.532). The serde-only estimate removes work that only one arm does inside the timed method
(section 2.4). That work includes the legacy arm building its marshaller on every call. Removing it takes away most
of the baseline's extra allocation, so the serde-only B/op ratio is higher than the as-measured one.

Neither view includes signing, the execution pipeline or an HTTP client. Both views call the protocol directly.

Per protocol (set 2, which includes FU-12; section 3.1 lists the substituted rows). Each protocol's geomean is the
geometric mean of its marshall-class geomean and its unmarshall-class geomean. The range is the protocol geomean
computed from each of the 4 repetitions alone.

**ns/op, feature / baseline**

| protocol | serde-only marshall | serde-only unmarshall | **serde-only protocol** (per-rep range) | as measured marshall | as measured unmarshall | as measured protocol |
|---|---:|---:|---:|---:|---:|---:|
| awsJson1_0 | 0.365 | 0.704 | **0.507** (0.505–0.509) | 0.346 | 0.792 | 0.523 |
| awsQuery | 0.479 | 0.095 | **0.214** (0.212–0.214) | 0.569 | 0.151 | 0.293 |
| restJson1 | 0.453 | 0.459 | **0.456** (0.451–0.460) | 0.305 | 0.509 | 0.394 |
| restXml | 0.424 | 0.305 | **0.360** (0.359–0.362) | 0.382 | 0.347 | 0.364 |
| rpcv2Cbor | 0.369 | 0.601 | **0.471** (0.470–0.473) | 0.350 | 0.660 | 0.481 |
| **aggregate** (geomean of the 5) | | | **0.384** (0.383–0.385), 2.6× faster | | | 0.403 (0.401–0.404), 2.5× faster |
| pooled, all 71 cases (reference) | | | 0.413 | | | 0.422 |

**B/op (`gc.alloc.rate.norm`), feature / baseline**

| protocol | serde-only marshall | serde-only unmarshall | **serde-only protocol** (per-rep range) | as measured marshall | as measured unmarshall | as measured protocol |
|---|---:|---:|---:|---:|---:|---:|
| awsJson1_0 | 0.924 | 0.797 | **0.858** (0.856–0.860) | 0.781 | 0.904 | 0.840 |
| awsQuery | 0.969 | 0.053 | **0.226** (0.225–0.226) | 0.773 | 0.055 | 0.207 |
| restJson1 | 1.913 | 0.624 | **1.092** (1.089–1.096) | 0.267 | 0.717 | 0.438 |
| restXml | 1.827 | 0.364 | **0.815** (0.812–0.818) | 1.013 | 0.419 | 0.651 |
| rpcv2Cbor | 0.910 | 0.894 | **0.902** (0.898–0.905) | 0.753 | 0.978 | 0.858 |
| **aggregate** (geomean of the 5) | | | **0.689** (0.689–0.690), 31% less | | | 0.532 (0.531–0.532), 47% less |
| pooled, all 71 cases (reference) | | | 0.809 | | | 0.648 |

Set 1 is the full A/B as run, without FU-12. Its aggregate is 0.387 ns/op (2.6× faster) and 0.698 B/op (30% less)
serde-only, and 0.405 (2.5× faster) and 0.539 (46% less) as measured. Only awsJson1_0 changes between the two sets:
in set 1 it is 0.524 ns/op and 0.915 B/op serde-only.

What the numbers say:

- Every protocol is faster in both views. awsQuery gains the most (0.214). Its legacy unmarshaller builds a DOM, and
  the feature build streams (section 4).
- 68 of 71 cases are faster than their noise floor in the serde-only view. 3 are slower (section 4.3).
- Allocation goes down for unmarshall in every protocol. It goes up for marshall in restJson1 (1.91×) and restXml
  (1.83×) in the serde-only view. That increase is partly an artifact of the estimate: the removed legacy
  marshaller construction also contains the legacy arm's request-builder setup, which the feature arm still pays for.
  Section 4.2 measures this. If that setup is kept in the baseline, the two figures become 1.18× and 1.18×. A fix for
  the marshall allocation is in progress.
- Spread: the per-repetition ranges are within ±0.35% of the aggregates and within 1.2% of each protocol geomean
  (the widest is restJson1 ns/op, −1.2% / +0.8%). The null experiment, run through the same aggregation, gives 0.990
  for ns/op and 0.999 for B/op. So the aggregate is resolved to about 1%. The per-case floors are in section 2.5.

## 2. What was compared and how

### 2.1 Arms

| arm | commit | SDK code | benchmark source | jar sha256 (prefix) | expected code path |
|---|---|---|---|---|---|
| baseline | `fc8b74e9fe21` (merge base of this branch) | stock V2 | `2549a5b6828` with a resource overlay (below) | `129cfb3caa1a` | legacy `core/protocols/*` marshallers and unmarshallers |
| feature | `2549a5b6828` | pinned to `4acbd0b03ae`; `4acbd0b03ae..2549a5b6828` changes only `test/sdk-standard-benchmarks/`, `scripts/`, `docs/`, `.kiro/` (6 files, checked at build time) | `2549a5b6828` | `ef5b2f2b7154` | schema path: `ClientProtocol`, ported codecs, http-binding |
| feature-fu12 (FU-12 run only) | `b17e79d6e1f` | adds the FU-12 blob member fix | `b17e79d6e1f` (adds only the `BlobMemberZeroCopyTest` test) | `413b506b92cf` | schema path |

The SDK pin `4acbd0b03ae` contains both copy fixes made during this measurement: `1f5f257a7f2` (blob
`@httpPayload`) and `4acbd0b03ae` (response body). It does not contain the FU-12 blob member fix (`cf3dff2dff5`,
`cca1bca22d1`, `976f12c8e66`). That fix is measured in a separate run (section 3.1). Section 5 describes all three
fixes.

All jars were built on a laptop with JDK 21.0.2 (Java 8 bytecode) by `scripts/schema-serde/serde-ab.sh`. They ran
on the host with Corretto 25.0.4.1. Every run directory records the build provenance of each jar (`arms.tsv`,
`provenance/`).

### 2.2 Benchmark rewrite onto `ClientProtocol`

Up to `cabc26b41aa`, the 10 serde classes called the legacy marshallers directly. On this branch, that measured the
legacy code over schema-derived `SdkField`s. It did not measure the ported codecs. Commit `c0468fe3781` changes each
`@Benchmark` to call `ClientProtocol.serializeRequest` or `deserializeResponse` (awsJson1_0, restJson1, restXml,
awsQuery, rpcv2Cbor). The protocol and its `ApiOperation` are resolved once per trial, as a generated client does.
The class names, method names, `@Param` values and JMH annotations are unchanged, so both jars run the same 71
benchmark keys. Inputs and responses are built as in the baseline.

### 2.3 Comparability evidence

The detailed evidence is in the comparability matrix (`.agents/tasks/serde-benchmark-comparison/comparability-matrix.md`,
not committed). These checks test behavior, not timing. The equivalence test ran on the laptop only. The resource,
model and wire checks ran on the laptop and again in the verify step of each metal run, together with the code-path
checks. In summary:

- **Resources and models.** Both arms build the same `codegen-resources` and `serde-tests` files. The baseline gets
  a 5-file overlay that restores the `ContentLength` members the feature models dropped. The sha256 manifests match.
  The normalized model projections match for 5 models and 140 shapes. Two known differences are not on a measured
  path: the greedy label on the feature's legacy path (FU-11), and a `xmlNamespace` on operations that no restXml
  case uses.
- **Wire dump.** Running the legacy path on both jars gives the same bytes for 67 of 71 rows, including every
  unmarshall `toString()` hash. The other 4 rows are the `CopyObject` marshall rows, which differ only in the
  `{Key+}` URI on the feature classpath (FU-11).
- **Equivalence test.** `SerdeBenchmarkEquivalenceTest` passes 142 of 142: 71 equivalence tests and 71
  `ExecutionAttributes` reuse tests. For every case it runs the baseline's benchmark code and the schema path. It
  compares the requests part by part, the bodies by value and the outputs member by member. Every allowed
  difference is listed with its reason. Examples: the `X-Amz-Target` value, the rpcv2Cbor service/operation path,
  definite-length CBOR maps, the Query form body, and response metadata attached on the feature arm.
- **Runtime code path on the metal host.** The verify step of each run passes: 67 of 67 checks in the full A/B and
  48 of 48 in the FU-12 run. These cover a JMH smoke run, the code path and class loading per class and arm, the
  resource and model checks, the wire dump and pin affinity. The
  profiling pass checks every case (`profile-path.tsv`): 142 of 142 pass. Baseline profiles have 0 schema-path
  samples. Feature profiles have 0 legacy-path samples and 0 `SdkFieldsFromSchema` frames.

### 2.4 Work boundary and the two views

Some items sit inside one arm's timed method while the other arm's client does the same work outside the serde
call (comparability matrix A1–A5):

- **A1.** The baseline builds its legacy marshaller on every call, as the generated `<Op>RequestMarshaller` does.
  The feature arm resolves `ClientProtocol` once per trial.
- **A2.** The feature unmarshall also runs the status check and builds `AwsResponseMetadata` / `SdkHttpResponse`. The
  legacy client does this in its response handler, outside the baseline timed method.
- **A5.** The feature Query marshall form-encodes the body and sets `Content-Length`. The legacy client does this
  later, in `QueryParametersToBodyStage`.
- **A3 and A4.** These are real per-call costs of the feature build with no legacy counterpart: the blob payload copy
  and the response body read. They stay in both views. A3 was fixed during this measurement (section 5).

**As measured** is everything in each arm's timed method.

**Serde-only** is an estimate made by `scripts/schema-serde/serde_ab_summary.py --boundary`. It scales each arm's
mean by 1 minus the share of A1, A2 and A5 in that arm's profile from the profiling pass: the CPU share for ns/op
and the allocation share for B/op. The shares come from a different run than the timing, and A5 is a lower bound.
Section 6 lists the estimate's caveats, including how A1 is classified.

### 2.5 Host, settings, null experiment

**Host.** `i-018869b3464a7fae5`, m6g.metal, us-east-1c: Graviton2 (Neoverse-N1), 64 cores, no SMT, 1 NUMA node,
251 GiB, no cpufreq driver (fixed frequency). Amazon Linux 2023, kernel 6.18.54. Corretto 25.0.4.1+11-LTS.
async-profiler 4.4.

**JMH.** JMH 1.37, SampleTime, `-f 2 -wi 5 -w 2s -i 5 -r 2s -prof gc -foe true`. These options override the class
annotations. JVM options: `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`.

**Runs.** Each JMH invocation runs one class on one arm. Each run has 4 repetitions. Within a repetition, the arm
order alternates. Every invocation is pinned with `taskset` to cores 32–47; the verify step checks the pin. The full
A/B ran 80 of 80 invocations ok, and the FU-12 run 48 of 48.

**Statistics** (`serde_ab_summary.py` docstring):

- Time is the JMH SampleTime mean (ns/op). Memory is `gc.alloc.rate.norm` (B/op).
- The ± values are the 99.9% Student-t half-width over the 4 repetitions.
- A case's ratio is the mean of the per-repetition paired ratios feature / baseline.
- A geomean is the geometric mean of case ratios.

**Null experiment** (`20261023-011610`). The feature jar ran against itself on 20 cases: the smallest and largest
case of each class, 4 repetitions, the same settings. The null jar is `ce95211425c` (before both copy fixes). The
two copy fixes change no benchmark code, and the floors measure run-to-run noise under the same options, so this
report applies them to the post-fix and FU-12 jars. That is an assumption for the cases the fixes changed (section 6).

- **Floor.** A case's floor is max(|ratio − 1|, the spread of the paired ratios, every run's relative JMH error).
- **ns/op floors.** Median 1.4%, max 18.1%. The max is `restJson1_CopyObjectOutput_Baseline`, a 0.6 µs case.
- **B/op floors.** 0–4.8%.
- **Cases not in the null.** 51 of the 71 cases have no row of their own in the null. Each gets the largest floor of
  the null rows of its class ("class max"). These floors are marked * in the appendix.
- **Verdicts.** faster or slower when |ratio − 1| exceeds the floor, "within floor" otherwise.
- **Steady state.** For each fork, the null summary measures the drift from the first to the last measurement
  iteration and the trend over them. A row is flagged when the mean over its 16 forks exceeds max(the row's floor,
  5%), because a warmup that is too short drifts the same way in every fork. 0 of 20 rows were flagged. Single forks
  drift more: up to −20.5% on `restXml_CopyObjectOutput_Baseline`, −10.2% on `awsJson1_0_HealthcheckResponse_Example`
  and −9.1% on `rpcv2Cbor_GetItemOutput_Baseline`, all cases of about 0.4 µs in the null run. Their means over
  forks stay within 1.4%.

**Uncertainty of the geomeans** (`.agents/tasks/serde-benchmark-metal/analysis/report_numbers.py`):

- **Per-rep range.** The statistic computed from each repetition's paired ratios alone, min to max.
- **Bootstrap.** An exact bootstrap over the repetition indices (all 256 resamples). It gives the same intervals to 3
  decimals.
- **Null aggregate.** The same aggregation on the null experiment gives 0.990 for ns/op and 0.999 for B/op. The
  per-protocol null geomeans are 0.954–1.007 for ns/op; restJson1's 0.954 comes from its 18.1% row. For B/op they
  are 0.995–1.002.

## 3. Results

### 3.1 Result sets and the FU-12 substitution

- **Set 1 (as run).** The full A/B `20261023-031253`: all 71 cases, baseline vs feature `2549a5b6828`.
- **Set 2 (with FU-12).** Set 1 with three rows replaced: `JsonRpc10MarshallBenchmark`
  `awsJson1_0_PutItemRequest_BinaryData_S`, `_M` and `_L`. In set 2 these rows use feature-fu12 / baseline from run
  `20261023-111214`. That run used the same host, settings and baseline jar, and measured the baseline again. The
  serde-only factors for the replaced rows are the same profiling-pass shares as in set 1. The baseline's A1 share
  is 0.2–24% B/op, and the feature arm has no boundary item in marshall.

Set 2 includes FU-12 for the awsJson1_0 rows only. The rpcv2Cbor `BinaryData_*` rows stay at their set 1 values,
as decided for set 2. The FU-12 jar does change those rows on metal: `rpcv2Cbor_PutItemRequest_BinaryData_S` is 8.6%
slower in ns/op than on the feature jar in the same run (feature-fu12 / feature 1.086; per repetition 1.075, 1.052,
1.112 and 1.103, against a 3.8%* floor). The per-repetition means are 1,022–1,074 ns/op vs 951–979 ns/op. Against
the baseline, the case is 0.525 on feature-fu12 vs 0.483 on feature. `_M` (0.987) and `_L` (0.974) are not slower.
B/op is unchanged within floor on all three. The cause is not investigated yet (FU-16 in `followups.md`). If the `_S` row were substituted
too, the rpcv2Cbor marshall class geomean would rise by 0.5% (0.3695 → 0.3715 ns/op), the rpcv2Cbor protocol by 0.3%
and the aggregate by 0.05% (0.3841 → 0.3843). Substituting all three rpcv2Cbor rows changes the aggregate by 0.02%.

The FU-12 run also re-measured the feature `2549a5b6828` jar on all 8 of its cases. Every control should reproduce
set 1:

| case | metric | floor | set 1 ratio | FU-12 run, same jars | difference |
|---|---|---:|---:|---:|---:|
| awsJson1_0 `BinaryData_S` | ns/op | 2.7%* | 0.591 | 0.592 | +0.3% |
| awsJson1_0 `BinaryData_M` | ns/op | 2.7%* | 0.321 | 0.322 | +0.2% |
| awsJson1_0 `BinaryData_L` | ns/op | 1.0% | 0.291 | 0.290 | −0.4% |
| rpcv2Cbor `BinaryData_S` | ns/op | 3.8%* | 0.492 | 0.483 | −1.8% |
| rpcv2Cbor `BinaryData_M` | ns/op | 3.8%* | 0.485 | 0.486 | +0.2% |
| rpcv2Cbor `BinaryData_L` | ns/op | 1.8% | 0.461 | 0.459 | −0.4% |
| restJson1 `PutObject_L` | ns/op | 1.1% | 0.210 | 0.212 | +0.7% |
| restXml `PutObject_L` | ns/op | 2.7% | 0.300 | 0.298 | −0.5% |
| awsJson1_0 `BinaryData_S` | B/op | 1.3%* | 1.146 | 1.131 | −1.3% |
| awsJson1_0 `BinaryData_M` / `_L` | B/op | 1.3% | 1.214 / 1.078 | 1.210 / 1.076 | −0.4% / −0.2% |
| rpcv2Cbor `BinaryData_S` / `_M` / `_L` | B/op | 2.8% | 1.013 / 1.065 / 0.328 | 1.008 / 1.066 / 0.328 | −0.4% / +0.1% / 0.0% |
| restJson1 / restXml `PutObject_L` | B/op | 0.0% | 0.111 / 1.064 | 0.111 / 1.070 | +0.1% / +0.6% |

All ns/op controls match within the floor. Three B/op cells differ by 0.1–1.3% where the floor is 0.0% or 1.3%. In
each of them, the feature arm's B/op is identical in both runs: 3,058, 2,128 and 2,192 B/op. The difference is on
the baseline side, where B/op moves in steps of about one object per operation between runs. For example, `BinaryData_S`
is 2,668 B/op in set 1 and 2,704 in the FU-12 run, a 36 B step. The substitution is therefore consistent, and set 2
is the headline.

The substituted rows:

| case | baseline ns/op | feature-fu12 ns/op | ratio | serde-only | baseline B/op | feature-fu12 B/op | B/op ratio | serde-only |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `awsJson1_0_PutItemRequest_BinaryData_S` | 2,000.6 ± 178.8 | 1,091.3 ± 44.2 | 0.546 | 0.593 | 2,704 ± 0 | 2,781 ± 116 | 1.028 | 1.359 |
| `awsJson1_0_PutItemRequest_BinaryData_M` | 22,315.2 ± 2,565.2 | 4,664.6 ± 223.2 | 0.209 | 0.211 | 26,730 ± 0 | 14,448 ± 0 | 0.541 | 0.555 |
| `awsJson1_0_PutItemRequest_BinaryData_L` | 174,288.7 ± 1,333.5 | 32,849.6 ± 1,122.4 | 0.188 | 0.189 | 251,333 ± 123 | 112,034 ± 0 | 0.446 | 0.447 |

In set 1 (feature without FU-12), the same rows are 0.591, 0.321 and 0.291 ns/op and 1.146, 1.214 and 1.078 B/op.

### 3.2 Per class (geomeans)

| class | cases | set 2 serde-only ns/op | set 2 serde-only B/op | set 2 as measured ns/op | set 2 as measured B/op | set 1 serde-only ns/op | set 1 serde-only B/op | set 1 as measured ns/op | set 1 as measured B/op |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| JsonRpc10MarshallBenchmark | 14 | 0.365 | 0.924 | 0.346 | 0.781 | 0.390 | 1.051 | 0.370 | 0.888 |
| JsonRpc10UnmarshallBenchmark | 8 | 0.704 | 0.797 | 0.792 | 0.904 | 0.704 | 0.797 | 0.792 | 0.904 |
| QueryMarshallBenchmark | 7 | 0.479 | 0.969 | 0.569 | 0.773 | 0.479 | 0.969 | 0.569 | 0.773 |
| QueryUnmarshallBenchmark | 3 | 0.095 | 0.053 | 0.151 | 0.055 | 0.095 | 0.053 | 0.151 | 0.055 |
| RestJsonMarshallBenchmark | 5 | 0.453 | 1.913 | 0.305 | 0.267 | 0.453 | 1.913 | 0.305 | 0.267 |
| RestJsonUnmarshallBenchmark | 5 | 0.459 | 0.624 | 0.509 | 0.717 | 0.459 | 0.624 | 0.509 | 0.717 |
| RestXmlMarshallBenchmark | 5 | 0.424 | 1.827 | 0.382 | 1.013 | 0.424 | 1.827 | 0.382 | 1.013 |
| RestXmlUnmarshallBenchmark | 5 | 0.305 | 0.364 | 0.347 | 0.419 | 0.305 | 0.364 | 0.347 | 0.419 |
| RpcV2CborMarshallBenchmark | 12 | 0.369 | 0.910 | 0.350 | 0.753 | 0.369 | 0.910 | 0.350 | 0.753 |
| RpcV2CborUnmarshallBenchmark | 7 | 0.601 | 0.894 | 0.660 | 0.978 | 0.601 | 0.894 | 0.660 | 0.978 |

Verdicts per case (set 2):

| view | metric | faster / less | within floor | slower / more |
|---|---|---:|---:|---:|
| serde-only | ns/op | 68 | 0 | 3 |
| serde-only | B/op | 36 | 2 | 33 |
| as measured | ns/op | 68 | 2 | 1 |
| as measured | B/op | 51 | 3 | 17 |

The per-case tables are in the appendix.

## 4. Where time and allocation go

The source for this section is the metal profiling pass (`20261023-095831`, `profile-comparison.md`). It used
async-profiler `cpu` (perf_events) and `alloc` on both arms and all 71 cases, with `-f 1 -wi 2 -w 2s -i 3 -r 3s` and
`-XX:+DebugNonSafepoints`. All 284 profile runs were ok. The categories follow the rules in
`scripts/schema-serde/serde_ab_profile.py`. A category's ns/op and B/op are its share times the profile run's score,
and the profiler perturbs those runs, so use the shares and the order of magnitude.

The profiles' allocation totals agree with `gc.alloc.rate.norm` within 0.98–1.04 for 131 of the 142 profiles. Seven
are at 0.92–0.97: awsJson1_0 `ShallowMap_L` (both arms), awsJson1_0 and rpcv2Cbor `BinaryData_L` (both arms each),
and the feature arm of `awsQuery_GetMetricDataResponse_L`. The four `GetObject_L` profiles are at 0.70–0.74, where
very large arrays are under-sampled. The tables below are class means over
the cases. `alloc_top.py` and `a1_request_builder.py` in the analysis directory give the allocation sites cited
below.

### 4.1 Per protocol

| class | baseline: top CPU categories (mean ns/op) | feature: top CPU categories (mean ns/op) |
|---|---|---|
| JsonRpc10 marshall | traversal 27% (8.3 µs), codec_write 23% (8.4 µs), pojo 21% (5.7 µs), format 9% (9.0 µs) | codec_write 42% (4.0 µs), http 20% (0.4 µs), traversal 19% (1.6 µs), bytes 9% (1.8 µs) |
| JsonRpc10 unmarshall | codec_read 68% (18.0 µs), pojo 13% (4.3 µs) | codec_read 33% (6.6 µs), traversal 19% (6.0 µs), pojo 14% (3.9 µs), metadata (A2) 10% |
| Query marshall | traversal 51% (17.3 µs), http 21% (9.6 µs), format 8% | codec_write 45% (9.4 µs), http 18% (0.6 µs), format 14% (4.9 µs), bytes 11% |
| Query unmarshall | codec_read 73% (486 µs, 750 KB/op), format 14% (137 µs) | metadata (A2) 37% (39 µs), codec_read 33% (39 µs), traversal 16% (10 µs) |
| RestJson marshall | construction (A1) 31%, http 22%, traversal 22%, pojo 18% | http 67% (1.5 µs), pojo 17% (1.1 µs), bytes 7% |
| RestJson unmarshall | format 18% (2.1 µs), http 18%, bytes 18% (17.1 µs), traversal 16% | http 41% (1.7 µs), bytes 21% (9.8 µs), input 11%, metadata (A2) 10% |
| RestXml marshall | traversal 38% (1.3 µs), http 23%, pojo 21%, construction (A1) 10% | http 66% (1.4 µs), pojo 16% (1.0 µs), bytes 9% |
| RestXml unmarshall | bytes 18% (16.8 µs), codec_read 17% (3.4 µs, 11 KB/op), traversal 17%, format 16% | http 35% (1.4 µs), bytes 21% (9.7 µs), metadata (A2) 12%, input 10% |
| RpcV2Cbor marshall | traversal 34% (10.1 µs), pojo 25% (6.4 µs), codec_write 19% (4.8 µs) | codec_write 43% (3.7 µs), traversal 20% (1.7 µs), http 20% (0.5 µs) |
| RpcV2Cbor unmarshall | codec_read 75% (19.6 µs), pojo 15% (4.8 µs) | traversal 26% (6.8 µs), codec_read 25% (3.7 µs), pojo 21% (4.2 µs), bytes 11% |

Where the feature build wins:

- **Member dispatch.** The legacy marshallers walk the `SdkField` registry. They dispatch through `MarshallingType`
  and read every value through `SdkField` getters, which shows as traversal plus pojo. Generated `serializeMembers`
  reads the fields directly. Marshall traversal plus pojo drops from 14.0–18.8 µs to 1.6–1.9 µs per call on the
  JsonRpc10, RpcV2Cbor and Query class means. That drop accounts for most of the 0.37–0.48 serde-only marshall
  ratios of those three classes.
- **Codec write and read.** The ported JSON and CBOR codecs write faster than the legacy Jackson generators:
  4.0 vs 8.4 µs (JsonRpc10) and 3.7 vs 4.8 µs (RpcV2Cbor). Formatting (numbers, timestamps, base64) also costs
  less: 1.4 vs 9.0 µs on JsonRpc10. On the read side, codec_read falls from 18.0 to
  6.6 µs (JsonRpc10) and from 19.6 to 3.7 µs (RpcV2Cbor). For RpcV2Cbor and JsonRpc10 unmarshall, part of that
  saving moves into traversal: the schema deserializer dispatches members for the map-heavy `AttributeValue`
  outputs, 6.0–6.8 µs vs 0.5 µs. This is why unmarshall gains less (0.60–0.70) than marshall on these two
  protocols.
- **No DOM on XML and Query.** The legacy restXml and awsQuery unmarshallers build a full DOM with `XmlDomParser`
  before they unmarshal. It allocates 750 KB per call on average in Query, and 17 KB of `char[]` alone on
  `restXml_CopyObjectOutput_M`. The feature build streams with `SmithyXmlDeserializer`. The results are awsQuery
  unmarshall at 0.095 ns/op and 0.053 B/op (serde-only), and `restXml_CopyObjectOutput_M` at 59.0 KB → 3.4 KB.
- **Legacy formatting.** On restJson1 and restXml unmarshall, the legacy path's format category (timestamp and
  header value parsing) is 2.1–2.5 µs per call. The feature's is 0.08 µs. On marshall, the legacy path formats RFC
  822 dates with `DateTimeFormatter` (`DateUtils.formatRfc822Date`, 19–20% of `CopyObjectRequest_M` CPU). The feature
  uses `HttpDateFormat`.

Where it doesn't win:

- **Header and request building on REST marshall.** The feature's http category is 66–67% of restJson1 and restXml
  marshall CPU. `HttpBindingSerializer.putHeader` builds a case-insensitive `TreeMap`, and
  `CollectionUtils.deepCopyMap` copies it into the request builder's own `TreeMap`. So every bound header is
  inserted twice with case-insensitive compares. The legacy marshaller appends to the builder's map once.
- **Unmarshall allocation on large awsJson1_0 and rpcv2Cbor outputs.** On 7 of the 8 M and L outputs of these two
  classes, the feature allocates more. Serde-only, that is 1.03–1.23× on awsJson1_0 (`GetItemOutputBinary_M` is
  0.90) and 1.07–1.28× on rpcv2Cbor. The feature reads the whole response into one `byte[]`
  (`ResponseBodyReader.read`, 17.5 KB of 87.6 KB on `awsJson1_0_GetItemOutput_L`), because the ported parsers need a
  contiguous array. The legacy Jackson parsers stream from the `InputStream`. The
  model objects themselves cost about the same on both arms, for example 12.8 vs 13.0 KB of `AttributeValue`
  builders on `awsJson1_0_GetItemOutput_L`.
- **awsQuery request id.** On Query unmarshall, A2 is 37% of feature CPU (39 µs/op on average). The request-id
  lookup parses the body a second time (FU-15 (b)). The serde-only view removes it. As measured, awsQuery unmarshall is
  still 0.151.

### 4.2 Marshall allocation

In the serde-only view, the feature allocates more than the baseline on these marshall cases:

- **restJson1 and restXml.** Class geomeans 1.91× and 1.83×. By case: restJson1 1.46–2.80×, restXml 1.42–2.69×.
  The highest are the `CopyObjectRequest_Baseline` rows.
- **Small awsJson1_0 requests.** 1.05–1.36×: `MixedItem_S` 1.05, `GetItemInput_Baseline` 1.21,
  `PutItemRequest_Baseline` 1.21, `HealthcheckRequest_Example` 1.28, `ShallowMap_S` 1.33, `BinaryData_S` 1.36.
- **Small rpcv2Cbor requests.** 1.07–1.43×: `Nested_M` 1.07, `MixedItem_S` 1.12, `PutItemRequest_Baseline` 1.36,
  `ShallowMap_S` 1.41, `BinaryData_S` 1.43.
- **Larger awsJson1_0 and rpcv2Cbor requests.** awsJson1_0 `ShallowMap_M` 1.12 and `ShallowMap_L` 1.13; rpcv2Cbor
  `ShallowMap_L` 1.16 and `BinaryData_M` 1.16.
- **awsQuery `PutMetricDataRequest_Baseline`.** 7.87×.

That is 26 marshall cases. With the 7 unmarshall cases of section 4.1, they make up the 33 "more" verdicts in
section 3.2. Request size alone does not decide the ratio. The `ShallowMap` requests allocate more at every size on
both protocols, except rpcv2Cbor `ShallowMap_M` (1.03, within floor). The `Nested_L`, `MixedItem_M`/`_L` and
`BinaryData_L` requests, and awsJson1_0 `BinaryData_M`, allocate less: for example awsJson1_0 `Nested_L` 0.60,
rpcv2Cbor `MixedItem_L` 0.62 and `BinaryData_L` 0.33.

**Part of the gap is the estimate.** A1 is the profile category `construction`. That category covers everything
under the legacy marshaller's constructor (`serde_ab_profile.py`: the outermost matching frame wins). The legacy
constructors also create the `SdkHttpFullRequest.Builder`: `JsonProtocolMarshaller` and `QueryProtocolMarshaller` in
`fillBasicRequestParams`, `XmlProtocolMarshaller` in `ProtocolUtils.createSdkHttpRequest`. That setup includes the
endpoint, method, path, static query literals and fixed headers. The serde-only estimate therefore removes the
baseline's request-builder setup. The feature arm builds its own request inside `serializeRequest`, and the estimate
keeps that cost.

`analysis/a1_request_builder.py` measures this part of A1 from the same profiles. It is 440–740 B/op on every
marshall case except `CopyObject`, and 1.66–1.81 KB on the `CopyObject` cases. On those cases,
`ProtocolUtils.addStaticQueryParametersToRequest` compiles a regex on every call for `?x-id=CopyObject`. In CPU,
the request-builder setup is 130–860 ns/op.

If this setup is kept in the baseline, these figures change:

| figure | serde-only | with the setup kept in the baseline |
|---|---:|---:|
| restJson1 marshall B/op geomean | 1.913 | 1.175 |
| restXml marshall B/op geomean | 1.827 | 1.175 |
| awsJson1_0 marshall B/op geomean | 0.924 | 0.812 |
| rpcv2Cbor marshall B/op geomean | 0.910 | 0.778 |
| awsQuery marshall B/op geomean | 0.969 | 0.782 |
| small awsJson1_0 / rpcv2Cbor requests (the cases listed above), B/op | 1.05–1.43 | 0.85–1.09 |
| `restJson1_CopyObjectRequest_Baseline` / `restXml_CopyObjectRequest_Baseline`, B/op | 2.80 / 2.69 | 0.91 / 0.90 |
| aggregate, ns/op / B/op | 0.384 / 0.689 | 0.368 / 0.598 |

This is a sensitivity check, not the headline. The headline keeps the decided A1 definition.

**The remaining feature allocation, per protocol.** These are the profiled sites (`analysis/alloc_top.py`):

- **restJson1 and restXml** (`PutObject_S`: 2,128 and 2,192 B/op). Allocation is spread across request building.
  The shares below are of the bytes allocated under any of the named frames (`analysis/rest_alloc_groups.sh`):
  - building the URI and path: `SdkHttpUtils.appendUri`, `DefaultSdkHttpFullRequest.standardizePath`,
    `PathSerializer`, `UrlEncoding`; 27% (restJson1) and 30% (restXml)
  - the header and query-parameter maps: the `TreeMap` built by `HttpBindingSerializer.putHeader`, its
    `CollectionUtils.deepCopyMap` copy, and the builder's `LowCopyListMap` maps; 32% and 31%
  - a per-call `HttpBindingSerializer`: 101 B
  - `SdkHttpMethod.fromValue`: 264 B of stream objects per call (the legacy path does not call it)
  - the payload `ByteBuffer` view and `ByteBufferContentStreamProvider`

  With the request-builder setup kept in the baseline, 1.15–1.31× of the baseline remains on `PutObject_*` and
  `CopyObjectRequest_M`.
- **awsJson1_0 and rpcv2Cbor.** On every call, the codec serializer allocates a fresh scratch buffer of about 1 KiB:
  `SmithyJsonSerializer.<init>` 1,051 B and `CborSerializer.<init>` 1,035 B on `PutItemRequest_Baseline`. That is
  37–43% of the feature's B/op. The legacy 1 KiB `SdkByteArrayOutputStream` is itself the request body. The feature
  writes into a separate, size-hinted body buffer as well (`ByteBufferOutputStream`, `newRequestBodyBuffer`). On
  small requests this fixed cost decides the ratio. On large ones, the smaller per-member structures win.
- **awsQuery.** `QueryFormSerializer` allocates a new serializer and a 1,181 B buffer on every call: 50% of the
  2,344 B/op on `PutMetricDataRequest_Baseline`. FU-4 records the missing pool. That case's baseline allocates only
  298 B/op once A1 is removed, which is why the ratio is 7.87×. With the request-builder setup kept, it is 2.49×.

### 4.3 The three cases slower in ns/op (serde-only)

| case | serde-only | as measured | floor | serde-only, setup kept in the baseline |
|---|---:|---:|---:|---:|
| `awsQuery_PutMetricDataRequest_Baseline` | 1.846 | 1.186 | 2.5% | 1.132 |
| `restJson1_CopyObjectRequest_M` | 1.031 | 0.972 | 1.2%* | 0.976 |
| `restXml_CopyObjectRequest_M` | 1.066 | 0.999 | 2.7%* | 1.004 |

- **`awsQuery_PutMetricDataRequest_Baseline`** (0.46 vs 0.55 µs; the smallest Query request).
  - **Baseline.** A1 is 40% of baseline CPU, and 38 of those 40 points are the request-builder setup above.
  - **Feature: URI.** 40% of feature CPU is self time in `SdkHttpFullRequest.Builder.uri`. This sets protocol, host,
    port and path from the endpoint `URI`; 45% inclusive vs 12% on the baseline.
  - **Feature: form serializer.** 19% is building the per-call `QueryFormSerializer` (`forAwsQuery`, FU-4).
  - **Feature: A5.** 6.5% is form percent-encoding (A5).

  As measured, the case is 1.19× slower. It is the only case slower than its floor in that view.
- **`restJson1_CopyObjectRequest_M` and `restXml_CopyObjectRequest_M`.** These are the header-heavy
  requests.
  - **Feature: headers.** `TreeMap.put` is 72–77% of feature CPU (inclusive), and 38.6–41% is self time in
    `StringLatin1.compareToCI`. Every header goes into the `TreeMap` built by `HttpBindingSerializer.putHeader`,
    then again into the builder's map through `CollectionUtils.deepCopyMap` (48% inclusive).
  - **Baseline.** It inserts each header once (`DefaultSdkHttpFullRequest.Builder.appendHeader`, 41–47%
    inclusive). It also spends 19–20% formatting RFC 822 dates with `DateTimeFormatter`
    (`DateUtils.formatRfc822Date`). The feature's `HttpDateFormat` avoids that, which roughly offsets the second
    header insertion.
  - **Result.** As measured, the cases are 0.972 and 0.999. Serde-only, they are 1.03 and 1.07. With the
    request-builder setup kept, they are 0.98 and 1.00.

## 5. Fixes made during this measurement

The laptop smoke and the first profiles showed three copies in the feature build. Each was fixed in its own
workflow before or alongside the metal runs. The "before" metal numbers come from the null run, whose jar
`ce95211425c` has neither copy fix. The "after" numbers come from the full A/B feature jar `2549a5b6828`. Both runs
used the same host and settings. Between those commits, main code changes only in the two fixes.

| fix | commits | what changed | metal effect |
|---|---|---|---|
| Blob `@httpPayload` copy (A3) | `1f5f257a7f2`, test `7b5117f3d8e` | `HttpBindingSerializer.setBlobPayload` copied the read-only `SdkBytes` view into a new array on every marshall. It now shares the buffer through `ByteBufferContentStreamProvider`, as legacy does. | `restJson1_PutObject_L`: 19,121 → 838 ns/op, 343,425 → 2,128 B/op. `restXml_PutObject_L`: 19,241 → 856 ns/op, 343,489 → 2,192 B/op. The other marshall cases of the null: within 1.7% ns/op and 0.2% B/op. |
| Response body growth copies | `4acbd0b03ae` | Bodies of unknown length started at 1 KiB and doubled. They now start from a per-operation size hint (the last body + 1/8). `Crc32ResponseValidation` reads into an exact array. | `awsJson1_0_GetItemOutputBinary_L` −33% B/op, −6.3% ns/op. `awsQuery_GetMetricDataResponse_L` −29% B/op, −2.5% ns/op. `rpcv2Cbor_GetItemOutputBinary_L` −8.7% B/op. The four small `Baseline`/`Example` outputs −48% to −59% B/op: `restJson1_CopyObjectOutput_Baseline` 2,027 → 1,048, `restXml_CopyObjectOutput_Baseline` 1,799 → 788, `awsJson1_0_HealthcheckResponse_Example` 1,688 → 696, `rpcv2Cbor_GetItemOutput_Baseline` 1,688 → 689. `awsQuery_GetMetricDataResponse_S` −22% B/op. `GetObject_L` unchanged (streaming payload). |
| FU-12 blob member copy | `cf3dff2dff5`, `cca1bca22d1`, `976f12c8e66`, docs `b17e79d6e1f` | Generated code passed blob members as `SdkBytes.asByteBuffer()`, a read-only view that the JDK base64 encoder copies. It now passes `asByteArrayUnsafe()` through `writeBlob(Schema, byte[])`. | FU-12 run, same jars and run: awsJson1_0 `BinaryData_S/M/L` feature-fu12 vs feature 0.921 / 0.650 / 0.649 ns/op and 0.909 / 0.447 / 0.414 B/op. Against the baseline: 1.146 → 1.028, 1.214 → 0.541, 1.078 → 0.446 B/op. `PutObject_L` on both REST protocols (0.997 / 1.001 ns/op, 1.000 B/op) is unchanged within floor. rpcv2Cbor `BinaryData_S` is 8.6% slower in ns/op (1.086, floor 3.8%*, slower in every repetition); `_M` and `_L` are 0.987 and 0.974; B/op is within floor on all three (0.994–1.008). Set 2 keeps the pre-FU-12 rpcv2Cbor rows (section 3.1, FU-16). |

The response-body fix's effect is a best case on the JsonRpc10, RpcV2Cbor and Query unmarshall cases and on the
restJson1 and restXml `CopyObjectOutput_*` cases (section 6).

## 6. Caveats

- **Serde-only is an estimate.** It scales each arm by work-boundary shares from a separate profiling run. A5 is a
  lower bound. A1 includes the legacy request-builder setup that the feature arm still pays for, so the estimate is
  conservative for the feature on marshall (section 4.2). The as-measured view has no estimate in it.
- **Response size hint, best case.** The JsonRpc10, RpcV2Cbor and Query unmarshall benchmarks send no
  `Content-Length` and repeat one response. So do the restJson1 and restXml `CopyObjectOutput_Baseline` and `_M`
  cases, 2 of the 5 cases in each REST unmarshall class: in the `serde-tests` output data, only `GetObject` declares
  `Content-Length`. Since `4acbd0b03ae`, the feature reads such bodies with a per-operation size hint (for the REST
  protocols, through `HttpBindingClientProtocol`), so it measures the hint's steady state: one copy, no growth. Real
  responses with `Content-Length` were already read with one exact array. Real chunked responses of varying size may
  still grow. The DynamoDB CRC32 path (`Crc32ResponseValidation`) is not in these benchmarks.
- **Laptop numbers are not used.** The laptop smoke runs (`smoke-results.md`) and the laptop before/after tables in
  the fix reports checked tooling and direction only.
- **One host type.** All results are from Graviton2 (Neoverse-N1) on JDK 25. x86 and other JDKs were not run. The
  jars are Java 8 bytecode built with JDK 21.
- **The null experiment** measured the pre-fix feature jar on 20 of the 71 cases. The other 51 cases use their
  class's largest floor. The steady-state check found no drift. Applying these floors to workloads that the fixes
  changed is an assumption: `PutObject_L` went from about 19 µs to 0.84 µs per call, and the feature-fu12 rows run a
  different code path.
- **No smithy-java arm.** The comparability work found a smithy-java arm feasible but did not build it. It needs a
  Gradle build of the reference with the same JDK, `implementation=generic` (runtime codegen needs JDK 25), a diff
  of its Smithy test inputs against `serde-tests/*.json`, a class and method name mapping
  (`*SerializeBenchmark.serialize` onto ours), and explicit JMH options. The driver accepts it as another `--jar`
  arm once the name mapping exists.
- **Marshall allocation fix in progress.** A separate workflow (`fix/marshall-allocation`) is working on section 4.2.
  This report measures `2549a5b6828` and `b17e79d6e1f` only.
- **Benchmark data quirk.** The restJson1 and restXml `GetObject_*` responses declare 1, 1,000 and 256,000 bytes but
  send the base64 text. Both arms read them the same way. The L case is dominated by those body copies on both
  arms (about 1.1–1.4 MB/op).

## 7. Reproduction

Raw data is not committed. It lives in the main checkout under `.agents/tasks/serde-benchmark-metal/` and in the jar
caches.

| what | local path |
|---|---|
| null experiment | `.agents/tasks/serde-benchmark-metal/runs/20261023-011610/` (`serde-ab.json`, `summary.md`, `jmh/`, `verify/`) |
| full A/B | `.agents/tasks/serde-benchmark-metal/runs/20261023-031253/` |
| profiling pass | `.agents/tasks/serde-benchmark-metal/runs/20261023-095831/` (`profile-comparison.md`, `boundary.tsv`, `profile-path.tsv`, `profile/<arm>/<class>/<case>/<event>/`) |
| FU-12 run | `.agents/tasks/serde-benchmark-metal/runs/20261023-111214/` |
| summaries | `.agents/tasks/serde-benchmark-metal/ab-summary.md`; `analysis/fu12-summary-with-null.md` |
| report numbers | `analysis/report_numbers.py` → `analysis/report-numbers.md`; `analysis/a1_request_builder.py`; `analysis/alloc_top.py`; `analysis/rest_alloc_groups.sh` |
| jars | `~/.cache/aws-sdk-java-v2/serde-ab-metal/jars` (null), `serde-ab-metal-postfix/jars` (A/B, profiling, FU-12 baseline and feature), `serde-ab-metal-fu12/jars` (feature-fu12) |

**Build and verify the jars locally** (JDK 21.0.2, from the repository root; each command also runs the local
verify):

```bash
J21=~/.local/share/mise/installs/java/21.0.2
# null jar (pre-fix feature)
JAVA_HOME=$J21 scripts/schema-serde/serde-ab.sh --arm baseline=fc8b74e9fe21 --arm feature=ce95211425c \
  --sdk-pin feature=cabc26b41aa --verify-only --out ~/.cache/aws-sdk-java-v2/serde-ab-metal
# A/B and profiling jars
JAVA_HOME=$J21 scripts/schema-serde/serde-ab.sh --arm baseline=fc8b74e9fe21 --arm feature=2549a5b6828 \
  --sdk-pin feature=4acbd0b03ae --verify-only --out ~/.cache/aws-sdk-java-v2/serde-ab-metal-postfix
# FU-12 jar
JAVA_HOME=$J21 scripts/schema-serde/serde-ab.sh --arm baseline=fc8b74e9fe21 --arm feature-fu12=b17e79d6e1f \
  --sdk-pin feature-fu12=b17e79d6e1f --verify-only --out ~/.cache/aws-sdk-java-v2/serde-ab-metal-fu12
```

**Host setup and the null experiment** (bash; `test/sdk-standard-benchmarks/README.md`, "Running on a remote metal
host"):

```bash
export SERDE_AB_REMOTE_TARGET=ec2-user@<host> SERDE_AB_REMOTE_KEY=<key.pem>
R=scripts/schema-serde/serde-ab-remote.sh
$R setup
$R deploy --jar ~/.cache/aws-sdk-java-v2/serde-ab-metal/jars/baseline.jar \
          --jar ~/.cache/aws-sdk-java-v2/serde-ab-metal/jars/feature.jar
$R start serde-null-session.sh      # runs the command below on the host, detached
$R wait 1500                        # repeat until exit 0
$R fetch 20261023-011610
```

```bash
# serde-null-session.sh
J="$HOME/serde-ab/jars"
"$HOME/serde-ab/repo/scripts/schema-serde/serde-ab.sh" --null "feature=$J/feature.jar" \
    --sdk-pin feature=cabc26b41aa --param-subset ends \
    --reps 4 --pin 32-47 --jmh-opts "-f 2 -wi 5 -w 2s -i 5 -r 2s" \
    --jvm-args "-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4" \
    --out "$HOME/serde-ab/out"
```

**Full A/B, then the profiling pass.** These ran as one session from `~/serde-ab-postfix/`, so they did not touch the
running null. The session waited for the null to finish. `serde-ab-postfix-session.sh` started it detached, by the
same `setsid nohup` line as `serde-ab-remote.sh start`.

```bash
R="$HOME/serde-ab-postfix"; J="$R/jars"; S="$R/repo/scripts/schema-serde/serde-ab.sh"
JVM="-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4"
"$S" --jar "baseline=$J/baseline.jar" --jar "feature=$J/feature.jar" --sdk-pin feature=4acbd0b03ae \
    --reps 4 --pin 32-47 --jmh-opts "-f 2 -wi 5 -w 2s -i 5 -r 2s" --jvm-args "$JVM" --out "$R/out"
"$S" --jar "baseline=$J/baseline.jar" --jar "feature=$J/feature.jar" --sdk-pin feature=4acbd0b03ae \
    --profile-only --pin 32-47 --profile-events cpu,alloc --profile-jmh-opts "-f 1 -wi 2 -w 2s -i 3 -r 3s" \
    --jvm-args "$JVM" --out "$R/out"
```

**FU-12 run** (`serde-ab-fu12-session.sh`, from `~/serde-ab-fu12/`; the baseline and feature jars are the A/B
jars):

```bash
R="$HOME/serde-ab-fu12"; J="$R/jars"; S="$R/repo/scripts/schema-serde/serde-ab.sh"
JVM="-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4"
"$S" --jar "baseline=$J/baseline.jar" --jar "feature=$J/feature.jar" --jar "feature-fu12=$J/feature-fu12.jar" \
    --sdk-pin feature=4acbd0b03ae --sdk-pin feature-fu12=b17e79d6e1f \
    --classes JsonRpc10MarshallBenchmark,RpcV2CborMarshallBenchmark,RestJsonMarshallBenchmark,RestXmlMarshallBenchmark \
    --params 'JsonRpc10MarshallBenchmark=awsJson1_0_PutItemRequest_BinaryData_S,awsJson1_0_PutItemRequest_BinaryData_M,awsJson1_0_PutItemRequest_BinaryData_L;RpcV2CborMarshallBenchmark=rpcv2Cbor_PutItemRequest_BinaryData_S,rpcv2Cbor_PutItemRequest_BinaryData_M,rpcv2Cbor_PutItemRequest_BinaryData_L;RestJsonMarshallBenchmark=restJson1_PutObject_L;RestXmlMarshallBenchmark=restXml_PutObject_L' \
    --reps 4 --pin 32-47 --jmh-opts "-f 2 -wi 5 -w 2s -i 5 -r 2s" --jvm-args "$JVM" --out "$R/out"
```

**Summaries and report numbers** (local, after `fetch`):

```bash
RUNS=.agents/tasks/serde-benchmark-metal/runs
python3 scripts/schema-serde/serde_ab_summary.py summarize $RUNS/20261023-031253/serde-ab.json \
  --null $RUNS/20261023-011610/serde-ab.json --boundary $RUNS/20261023-095831/boundary.tsv
python3 scripts/schema-serde/serde_ab_summary.py summarize $RUNS/20261023-111214/serde-ab.json \
  --null $RUNS/20261023-011610/serde-ab.json
python3 .agents/tasks/serde-benchmark-metal/analysis/report_numbers.py
```

## Appendix: per-case results (set 2)

The tables below give, for each case:

- the baseline and feature means ± their 99.9% half-width over the 4 repetitions;
- the as-measured ratio and the serde-only ratio;
- the case's floor and its serde-only verdict.

A floor marked * is inferred from the class max. The rows marked † are the FU-12 substitutions from run
`20261023-111214`. All other rows are from run `20261023-031253`. For B/op, the verdict words are "less" and "more".

### awsJson1_0 marshall (`JsonRpc10MarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | serde-only | floor | verdict (serde-only) | baseline B/op | feature B/op | B/op ratio | serde-only | B/op floor | B/op verdict (serde-only) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| awsJson1_0_GetItemInput_Baseline | 1,453.5 ± 88.4 | 702.3 ± 26.1 | 0.483 | 0.548 | 2.7%* | faster | 2,695 ± 116 | 2,456 ± 0 | 0.911 | 1.205 | 1.3%* | more |
| awsJson1_0_HealthcheckRequest_Example | 691.6 ± 51.1 | 508.9 ± 26.2 | 0.736 | 0.957 | 2.7% | faster | 2,554 ± 134 | 2,432 ± 0 | 0.952 | 1.277 | 0.0% | more |
| awsJson1_0_PutItemRequest_Baseline | 1,502.9 ± 82.4 | 703.2 ± 31.7 | 0.468 | 0.526 | 2.7%* | faster | 2,686 ± 134 | 2,456 ± 0 | 0.914 | 1.213 | 1.3%* | more |
| awsJson1_0_PutItemRequest_ShallowMap_S | 4,776.2 ± 466.2 | 1,518.4 ± 103.3 | 0.318 | 0.331 | 2.7%* | faster | 2,695 ± 116 | 2,720 ± 0 | 1.009 | 1.326 | 1.3%* | more |
| awsJson1_0_PutItemRequest_ShallowMap_M | 26,897.8 ± 813.6 | 7,905.7 ± 41.6 | 0.294 | 0.296 | 2.7%* | faster | 4,801 ± 0 | 4,705 ± 0 | 0.980 | 1.120 | 1.3%* | more |
| awsJson1_0_PutItemRequest_ShallowMap_L | 88,573.8 ± 3,041.1 | 24,840.2 ± 104.1 | 0.280 | 0.281 | 2.7%* | faster | 9,430 ± 68 | 9,961 ± 0 | 1.056 | 1.131 | 1.3%* | more |
| awsJson1_0_PutItemRequest_Nested_M | 3,777.2 ± 189.3 | 1,368.9 ± 59.3 | 0.362 | 0.383 | 2.7%* | faster | 3,202 ± 388 | 2,528 ± 0 | 0.790 | 0.999 | 1.3%* | within floor |
| awsJson1_0_PutItemRequest_Nested_L | 12,615.9 ± 147.4 | 4,290.1 ± 154.3 | 0.340 | 0.345 | 2.7%* | faster | 6,705 ± 0 | 3,648 ± 0 | 0.544 | 0.599 | 1.3%* | less |
| awsJson1_0_PutItemRequest_MixedItem_S | 5,620.1 ± 516.0 | 1,831.3 ± 42.4 | 0.326 | 0.336 | 2.7%* | faster | 3,130 ± 134 | 2,648 ± 0 | 0.846 | 1.054 | 1.3%* | more |
| awsJson1_0_PutItemRequest_MixedItem_M | 28,588.4 ± 1,429.4 | 8,556.4 ± 226.0 | 0.299 | 0.302 | 2.7%* | faster | 6,610 ± 0 | 4,425 ± 0 | 0.669 | 0.751 | 1.3%* | less |
| awsJson1_0_PutItemRequest_MixedItem_L | 83,401.9 ± 10,872.0 | 25,558.5 ± 467.5 | 0.307 | 0.307 | 2.7%* | faster | 13,390 ± 157 | 8,441 ± 0 | 0.630 | 0.659 | 1.3%* | less |
| awsJson1_0_PutItemRequest_BinaryData_S † | 2,000.6 ± 178.8 | 1,091.3 ± 44.2 | 0.546 | 0.593 | 2.7%* | faster | 2,704 ± 0 | 2,781 ± 116 | 1.028 | 1.359 | 1.3%* | more |
| awsJson1_0_PutItemRequest_BinaryData_M † | 22,315.2 ± 2,565.2 | 4,664.6 ± 223.2 | 0.209 | 0.211 | 2.7%* | faster | 26,730 ± 0 | 14,448 ± 0 | 0.541 | 0.555 | 1.3%* | less |
| awsJson1_0_PutItemRequest_BinaryData_L † | 174,288.7 ± 1,333.5 | 32,849.6 ± 1,122.4 | 0.188 | 0.189 | 1.0% | faster | 251,333 ± 123 | 112,034 ± 0 | 0.446 | 0.447 | 1.3% | less |

### awsJson1_0 unmarshall (`JsonRpc10UnmarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | serde-only | floor | verdict (serde-only) | baseline B/op | feature B/op | B/op ratio | serde-only | B/op floor | B/op verdict (serde-only) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| awsJson1_0_GetItemOutput_Baseline | 362.5 ± 90.4 | 342.2 ± 74.7 | 0.946 | 0.650 | 3.6%* | faster | 1,068 ± 90 | 768 ± 0 | 0.719 | 0.495 | 1.2%* | less |
| awsJson1_0_GetItemOutput_S | 2,894.0 ± 130.6 | 2,030.7 ± 64.4 | 0.702 | 0.660 | 3.6%* | faster | 3,272 ± 0 | 2,960 ± 0 | 0.905 | 0.832 | 1.2%* | less |
| awsJson1_0_GetItemOutput_M | 10,466.9 ± 408.4 | 8,065.0 ± 813.6 | 0.771 | 0.758 | 3.6%* | faster | 11,209 ± 0 | 11,751 ± 75 | 1.048 | 1.027 | 1.2%* | more |
| awsJson1_0_GetItemOutput_L | 75,969.0 ± 3,426.0 | 59,279.2 ± 499.8 | 0.780 | 0.778 | 3.6%* | faster | 71,109 ± 53 | 87,565 ± 347 | 1.231 | 1.229 | 1.2%* | more |
| awsJson1_0_GetItemOutputBinary_S | 2,999.6 ± 271.8 | 2,127.3 ± 71.7 | 0.709 | 0.667 | 3.6%* | faster | 3,712 ± 0 | 2,952 ± 0 | 0.795 | 0.732 | 1.2%* | less |
| awsJson1_0_GetItemOutputBinary_M | 11,823.3 ± 380.0 | 8,516.4 ± 125.6 | 0.720 | 0.709 | 3.6%* | faster | 13,192 ± 39 | 12,181 ± 151 | 0.923 | 0.904 | 1.2%* | less |
| awsJson1_0_GetItemOutputBinary_L | 87,829.1 ± 6,476.8 | 65,209.2 ± 1,432.8 | 0.743 | 0.740 | 0.6% | faster | 89,006 ± 146 | 91,603 ± 3,278 | 1.029 | 1.026 | 1.2% | more |
| awsJson1_0_HealthcheckResponse_Example | 305.6 ± 28.9 | 311.6 ± 14.3 | 1.020 | 0.679 | 3.6% | faster | 991 ± 65 | 696 ± 0 | 0.702 | 0.460 | 0.0% | less |

### awsQuery marshall (`QueryMarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | serde-only | floor | verdict (serde-only) | baseline B/op | feature B/op | B/op ratio | serde-only | B/op floor | B/op verdict (serde-only) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| awsQuery_GetMetricDataRequest_S | 5,328.4 ± 635.1 | 2,105.3 ± 58.8 | 0.395 | 0.317 | 2.5%* | faster | 6,676 ± 155 | 2,784 ± 0 | 0.417 | 0.466 | 0.0%* | less |
| awsQuery_GetMetricDataRequest_M | 19,600.4 ± 2,185.1 | 8,859.6 ± 302.5 | 0.452 | 0.316 | 2.5%* | faster | 26,232 ± 39 | 16,457 ± 0 | 0.627 | 0.644 | 0.0%* | less |
| awsQuery_GetMetricDataRequest_L | 42,623.8 ± 1,128.1 | 17,614.9 ± 765.2 | 0.413 | 0.284 | 2.5%* | faster | 53,249 ± 75 | 37,002 ± 90 | 0.695 | 0.704 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_Baseline | 462.0 ± 139.1 | 547.2 ± 19.3 | 1.186 | 1.846 | 2.5% | slower | 994 ± 100 | 2,344 ± 0 | 2.359 | 7.873 | 0.0% | more |
| awsQuery_PutMetricDataRequest_S | 2,880.0 ± 207.6 | 1,425.6 ± 52.3 | 0.495 | 0.408 | 2.5%* | faster | 4,228 ± 891 | 2,752 ± 0 | 0.651 | 0.783 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_M | 15,591.6 ± 1,497.6 | 12,361.5 ± 4,141.7 | 0.793 | 0.559 | 2.5%* | faster | 19,653 ± 63 | 13,225 ± 0 | 0.673 | 0.697 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_L | 163,030.7 ± 10,134.3 | 91,679.3 ± 1,386.6 | 0.562 | 0.485 | 0.3% | faster | 209,804 ± 15,381 | 184,390 ± 91 | 0.879 | 0.881 | 0.0% | less |

### awsQuery unmarshall (`QueryUnmarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | serde-only | floor | verdict (serde-only) | baseline B/op | feature B/op | B/op ratio | serde-only | B/op floor | B/op verdict (serde-only) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| awsQuery_GetMetricDataResponse_S | 48,017.7 ± 1,026.9 | 7,208.0 ± 98.6 | 0.150 | 0.094 | 0.5% | faster | 101,193 ± 77 | 5,073 ± 0 | 0.050 | 0.045 | 0.7% | less |
| awsQuery_GetMetricDataResponse_M | 153,056.4 ± 6,716.7 | 23,856.6 ± 391.8 | 0.156 | 0.099 | 0.5%* | faster | 270,307 ± 2,619 | 15,374 ± 116 | 0.057 | 0.055 | 0.7%* | less |
| awsQuery_GetMetricDataResponse_L | 1,820,590.4 ± 39,908.2 | 268,796.6 ± 4,196.7 | 0.148 | 0.093 | 0.2% | faster | 2,702,261 ± 24,168 | 161,691 ± 52 | 0.060 | 0.060 | 0.0% | less |

### restJson1 marshall (`RestJsonMarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | serde-only | floor | verdict (serde-only) | baseline B/op | feature B/op | B/op ratio | serde-only | B/op floor | B/op verdict (serde-only) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| restJson1_CopyObjectRequest_Baseline | 3,005.7 ± 155.7 | 881.5 ± 53.6 | 0.293 | 0.361 | 1.2% | faster | 2,736 ± 0 | 2,376 ± 0 | 0.868 | 2.796 | 0.8% | more |
| restJson1_CopyObjectRequest_M | 10,188.9 ± 319.2 | 9,905.6 ± 202.0 | 0.972 | 1.031 | 1.2%* | slower | 8,641 ± 0 | 9,854 ± 39 | 1.140 | 1.461 | 0.8%* | more |
| restJson1_PutObject_S | 3,931.3 ± 243.9 | 826.4 ± 11.7 | 0.210 | 0.372 | 1.2%* | faster | 19,112 ± 127 | 2,128 ± 0 | 0.111 | 1.852 | 0.8%* | more |
| restJson1_PutObject_M | 3,969.5 ± 336.9 | 836.7 ± 51.2 | 0.211 | 0.386 | 1.2%* | faster | 19,139 ± 183 | 2,128 ± 0 | 0.111 | 1.859 | 0.8%* | more |
| restJson1_PutObject_L | 3,979.4 ± 221.6 | 837.5 ± 26.8 | 0.210 | 0.358 | 1.1% | faster | 19,139 ± 39 | 2,128 ± 0 | 0.111 | 1.821 | 0.0% | more |

### restJson1 unmarshall (`RestJsonUnmarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | serde-only | floor | verdict (serde-only) | baseline B/op | feature B/op | B/op ratio | serde-only | B/op floor | B/op verdict (serde-only) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| restJson1_CopyObjectOutput_Baseline | 1,312.9 ± 144.7 | 576.8 ± 390.6 | 0.440 | 0.367 | 18.1% | faster | 1,288 ± 0 | 1,048 ± 184 | 0.814 | 0.620 | 4.3% | less |
| restJson1_CopyObjectOutput_M | 5,413.0 ± 205.4 | 3,700.9 ± 271.8 | 0.684 | 0.609 | 18.1%* | faster | 4,388 ± 167 | 3,402 ± 233 | 0.775 | 0.631 | 4.3%* | less |
| restJson1_GetObject_S | 7,071.3 ± 793.7 | 3,116.2 ± 293.3 | 0.441 | 0.403 | 18.1%* | faster | 6,550 ± 39 | 3,402 ± 78 | 0.519 | 0.448 | 4.3%* | less |
| restJson1_GetObject_M | 7,405.6 ± 537.3 | 3,414.1 ± 69.0 | 0.461 | 0.406 | 18.1%* | faster | 9,230 ± 39 | 6,698 ± 77 | 0.726 | 0.675 | 4.3%* | less |
| restJson1_GetObject_L | 111,632.2 ± 10,248.7 | 62,681.6 ± 5,756.6 | 0.562 | 0.558 | 1.4% | faster | 1,392,494 ± 1 | 1,111,708 ± 2 | 0.798 | 0.798 | 0.0% | less |

### restXml marshall (`RestXmlMarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | serde-only | floor | verdict (serde-only) | baseline B/op | feature B/op | B/op ratio | serde-only | B/op floor | B/op verdict (serde-only) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| restXml_CopyObjectRequest_Baseline | 2,776.6 ± 534.0 | 864.5 ± 75.2 | 0.312 | 0.390 | 1.1% | faster | 3,056 ± 0 | 2,440 ± 0 | 0.798 | 2.689 | 0.8% | more |
| restXml_CopyObjectRequest_M | 9,911.4 ± 1,051.3 | 9,901.9 ± 441.9 | 0.999 | 1.066 | 2.7%* | slower | 9,003 ± 134 | 9,921 ± 0 | 1.102 | 1.422 | 0.8%* | more |
| restXml_PutObject_S | 2,885.4 ± 224.8 | 844.8 ± 54.5 | 0.293 | 0.317 | 2.7%* | faster | 2,054 ± 78 | 2,192 ± 0 | 1.067 | 1.759 | 0.8%* | more |
| restXml_PutObject_M | 2,841.6 ± 266.3 | 846.6 ± 23.0 | 0.298 | 0.321 | 2.7%* | faster | 2,048 ± 0 | 2,192 ± 0 | 1.070 | 1.750 | 0.8%* | more |
| restXml_PutObject_L | 2,859.2 ± 254.2 | 856.4 ± 10.7 | 0.300 | 0.323 | 2.7% | faster | 2,060 ± 155 | 2,192 ± 0 | 1.064 | 1.729 | 0.0% | more |

### restXml unmarshall (`RestXmlUnmarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | serde-only | floor | verdict (serde-only) | baseline B/op | feature B/op | B/op ratio | serde-only | B/op floor | B/op verdict (serde-only) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| restXml_CopyObjectOutput_Baseline | 1,214.1 ± 91.6 | 281.6 ± 43.6 | 0.232 | 0.183 | 3.8% | faster | 1,000 ± 0 | 788 ± 155 | 0.788 | 0.583 | 4.8% | less |
| restXml_CopyObjectOutput_M | 25,951.3 ± 913.1 | 3,970.8 ± 316.2 | 0.153 | 0.128 | 3.8%* | faster | 58,971 ± 147 | 3,363 ± 278 | 0.057 | 0.048 | 4.8%* | less |
| restXml_GetObject_S | 6,047.7 ± 404.1 | 2,942.2 ± 104.7 | 0.487 | 0.441 | 3.8%* | faster | 6,580 ± 89 | 3,306 ± 233 | 0.502 | 0.433 | 4.8%* | less |
| restXml_GetObject_M | 6,337.4 ± 646.4 | 3,254.2 ± 77.9 | 0.514 | 0.456 | 3.8%* | faster | 9,257 ± 73 | 6,602 ± 134 | 0.713 | 0.663 | 4.8%* | less |
| restXml_GetObject_L | 110,518.5 ± 9,635.9 | 62,318.8 ± 4,871.7 | 0.564 | 0.559 | 1.3% | faster | 1,392,634 ± 90 | 1,111,660 ± 1 | 0.798 | 0.798 | 0.0% | less |

### rpcv2Cbor marshall (`RpcV2CborMarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | serde-only | floor | verdict (serde-only) | baseline B/op | feature B/op | B/op ratio | serde-only | B/op floor | B/op verdict (serde-only) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| rpcv2Cbor_PutItemRequest_Baseline | 1,688.5 ± 57.4 | 821.8 ± 84.9 | 0.487 | 0.585 | 3.8% | faster | 2,859 ± 110 | 2,744 ± 179 | 0.960 | 1.355 | 2.8% | more |
| rpcv2Cbor_PutItemRequest_ShallowMap_S | 4,695.6 ± 216.7 | 1,691.2 ± 70.8 | 0.360 | 0.386 | 3.8%* | faster | 2,887 ± 142 | 2,888 ± 0 | 1.000 | 1.405 | 2.8%* | more |
| rpcv2Cbor_PutItemRequest_ShallowMap_M | 27,291.5 ± 5,424.8 | 8,045.4 ± 3,493.9 | 0.295 | 0.299 | 3.8%* | faster | 5,266 ± 3,159 | 4,445 ± 297 | 0.849 | 1.028 | 2.8%* | within floor |
| rpcv2Cbor_PutItemRequest_ShallowMap_L | 83,691.3 ± 3,481.1 | 21,362.9 ± 330.0 | 0.255 | 0.256 | 3.8%* | faster | 8,152 ± 87 | 8,601 ± 0 | 1.055 | 1.160 | 2.8%* | more |
| rpcv2Cbor_PutItemRequest_Nested_M | 3,878.3 ± 381.3 | 1,555.7 ± 179.9 | 0.401 | 0.427 | 3.8%* | faster | 3,468 ± 478 | 2,800 ± 0 | 0.808 | 1.069 | 2.8%* | more |
| rpcv2Cbor_PutItemRequest_Nested_L | 12,879.3 ± 264.2 | 3,861.5 ± 102.7 | 0.300 | 0.309 | 3.8%* | faster | 8,297 ± 238 | 3,816 ± 0 | 0.460 | 0.516 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_MixedItem_S | 5,526.9 ± 621.3 | 1,715.0 ± 88.3 | 0.310 | 0.328 | 3.8%* | faster | 3,417 ± 116 | 2,904 ± 0 | 0.850 | 1.118 | 2.8%* | more |
| rpcv2Cbor_PutItemRequest_MixedItem_M | 27,625.7 ± 997.7 | 7,006.7 ± 108.2 | 0.254 | 0.256 | 3.8%* | faster | 6,921 ± 158 | 4,137 ± 210 | 0.598 | 0.689 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_MixedItem_L | 80,244.0 ± 2,838.9 | 19,764.3 ± 36.8 | 0.246 | 0.247 | 3.8%* | faster | 12,370 ± 68 | 7,137 ± 0 | 0.577 | 0.615 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_BinaryData_S | 1,978.4 ± 291.4 | 973.5 ± 177.7 | 0.492 | 0.566 | 3.8%* | faster | 2,868 ± 121 | 2,904 ± 179 | 1.013 | 1.426 | 2.8%* | more |
| rpcv2Cbor_PutItemRequest_BinaryData_M | 6,099.2 ± 762.8 | 2,955.8 ± 94.9 | 0.485 | 0.512 | 3.8%* | faster | 10,281 ± 243 | 10,952 ± 0 | 1.065 | 1.158 | 2.8%* | more |
| rpcv2Cbor_PutItemRequest_BinaryData_L | 26,967.9 ± 1,361.5 | 12,438.8 ± 518.2 | 0.461 | 0.470 | 1.8% | faster | 243,773 ± 127 | 79,987 ± 45 | 0.328 | 0.329 | 2.8% | less |

### rpcv2Cbor unmarshall (`RpcV2CborUnmarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | serde-only | floor | verdict (serde-only) | baseline B/op | feature B/op | B/op ratio | serde-only | B/op floor | B/op verdict (serde-only) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| rpcv2Cbor_GetItemOutput_Baseline | 359.3 ± 34.7 | 330.0 ± 68.8 | 0.919 | 0.588 | 4.1% | faster | 1,141 ± 65 | 689 ± 90 | 0.604 | 0.395 | 1.5% | less |
| rpcv2Cbor_GetItemOutput_S | 2,978.8 ± 112.4 | 1,694.9 ± 74.3 | 0.569 | 0.528 | 4.1%* | faster | 3,456 ± 0 | 3,072 ± 0 | 0.889 | 0.821 | 3.7%* | less |
| rpcv2Cbor_GetItemOutput_M | 11,189.3 ± 326.8 | 6,735.3 ± 145.3 | 0.602 | 0.589 | 4.1%* | faster | 11,305 ± 0 | 12,073 ± 60 | 1.068 | 1.047 | 3.7%* | more |
| rpcv2Cbor_GetItemOutput_L | 75,364.6 ± 5,579.6 | 47,000.5 ± 2,336.6 | 0.624 | 0.621 | 4.1%* | faster | 71,188 ± 2 | 83,544 ± 660 | 1.174 | 1.171 | 3.7%* | more |
| rpcv2Cbor_GetItemOutputBinary_S | 2,911.6 ± 76.9 | 1,725.8 ± 88.0 | 0.593 | 0.548 | 4.1%* | faster | 3,544 ± 0 | 3,032 ± 0 | 0.856 | 0.788 | 3.7%* | less |
| rpcv2Cbor_GetItemOutputBinary_M | 10,829.1 ± 312.7 | 7,391.9 ± 667.9 | 0.683 | 0.670 | 4.1%* | faster | 11,714 ± 100 | 13,582 ± 88 | 1.159 | 1.139 | 3.7%* | more |
| rpcv2Cbor_GetItemOutputBinary_L | 74,632.0 ± 4,393.3 | 51,094.4 ± 847.5 | 0.685 | 0.681 | 1.4% | faster | 74,914 ± 105 | 96,195 ± 91 | 1.284 | 1.280 | 3.7% | more |
