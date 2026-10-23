# Serde performance report: schema-based serde vs legacy serde (EC2 metal)

This report compares the cost of request serialization and response deserialization (serde) between the
baseline build and the feature build. The baseline is stock V2 with the legacy marshallers and unmarshallers.
The feature build uses schema-based serde with the codecs ported from smithy-java. All results come from the 10
`benchmark.serde` JMH classes of `test/sdk-standard-benchmarks`, run on a dedicated m6g.metal host. This report covers
serde only. The end-to-end report (`perf-report.md`) is separate and still preliminary.

## 1. Summary

Across the five protocols, the feature build's serde is **2.7× faster (ratio 0.376) and allocates 49% less (ratio
0.513)**. This is the comparable view, with the FU-12 blob member fix included. Each figure is the geometric mean of
the five per-protocol geometric means of feature / baseline. Lower ratios favor the feature build.

The comparable view takes out of each arm's timed method only the work that the other arm's client does outside its
serde call (section 2.4). On the baseline, that is the legacy marshaller objects it builds on every call
(A1-objects). On the feature, it is the response status check and metadata attachment (A2) and the encoded writes of
the Query form values (A5), which the legacy client does after its marshaller. The request work inside the legacy
marshaller's constructor (request builder, URI, static headers, JSON generator or XML writer; A1-request) stays in
the baseline, because the feature does the same work inside its own serde call.

Measured as each arm's whole timed method (the as-measured view), the same aggregate is 2.5× faster (0.403) with 47%
less allocation (0.532). The previous version of this report led with a serde-only estimate that also took
A1-request out of the baseline. Section 2.4 explains why that estimate is no longer used and how its numbers map
onto the new ones.

Neither view includes signing, the execution pipeline or an HTTP client. Both views call the protocol directly.

Per protocol (set 2, which includes FU-12; section 3.1 lists the substituted rows). Each protocol's geomean is the
geometric mean of its marshall-class geomean and its unmarshall-class geomean. The range is the protocol geomean
computed from each of the 4 repetitions alone.

**ns/op, feature / baseline**

| protocol | comparable marshall | comparable unmarshall | **comparable protocol** (per-rep range) | as measured marshall | as measured unmarshall | as measured protocol |
|---|---:|---:|---:|---:|---:|---:|
| awsJson1_0 | 0.348 | 0.703 | **0.495** (0.493–0.497) | 0.346 | 0.792 | 0.523 |
| awsQuery | 0.439 | 0.151 | **0.257** (0.256–0.258) | 0.569 | 0.151 | 0.293 |
| restJson1 | 0.308 | 0.458 | **0.376** (0.371–0.379) | 0.305 | 0.509 | 0.394 |
| restXml | 0.384 | 0.304 | **0.342** (0.341–0.344) | 0.382 | 0.347 | 0.364 |
| rpcv2Cbor | 0.352 | 0.601 | **0.460** (0.458–0.462) | 0.350 | 0.660 | 0.481 |
| **aggregate** (geomean of the 5) | | | **0.376** (0.375–0.377), 2.7× faster | | | 0.403 (0.401–0.404), 2.5× faster |
| pooled, all 71 cases (reference) | | | 0.397 | | | 0.422 |

**B/op (`gc.alloc.rate.norm`), feature / baseline**

| protocol | comparable marshall | comparable unmarshall | **comparable protocol** (per-rep range) | as measured marshall | as measured unmarshall | as measured protocol |
|---|---:|---:|---:|---:|---:|---:|
| awsJson1_0 | 0.812 | 0.797 | **0.804** (0.803–0.806) | 0.781 | 0.904 | 0.840 |
| awsQuery | 0.782 | 0.054 | **0.206** (0.206–0.207) | 0.773 | 0.055 | 0.207 |
| restJson1 | 0.271 | 0.624 | **0.411** (0.410–0.413) | 0.267 | 0.717 | 0.438 |
| restXml | 1.063 | 0.364 | **0.622** (0.620–0.624) | 1.013 | 0.419 | 0.651 |
| rpcv2Cbor | 0.778 | 0.894 | **0.834** (0.831–0.837) | 0.753 | 0.978 | 0.858 |
| **aggregate** (geomean of the 5) | | | **0.513** (0.512–0.513), 49% less | | | 0.532 (0.531–0.532), 47% less |
| pooled, all 71 cases (reference) | | | 0.632 | | | 0.648 |

Set 1 is the full A/B as run, without FU-12. Its aggregate is 0.379 ns/op (2.6× faster) and 0.519 B/op (48% less)
comparable, and 0.405 (2.5× faster) and 0.539 (46% less) as measured. Only awsJson1_0 changes between the two sets:
in set 1 it is 0.512 ns/op and 0.858 B/op comparable.

What the numbers say:

- Every protocol is faster in both views. awsQuery gains the most (0.257). Its legacy unmarshaller builds a DOM, and
  the feature build streams (section 4).
- 69 of 71 cases are faster than their noise floor in the comparable view. `restXml_CopyObjectRequest_M` is within
  its floor (1.001), and `awsQuery_PutMetricDataRequest_Baseline` is slower (1.132; section 4.3).
- Allocation goes down for unmarshall in every protocol. For marshall, the comparable class geomean is below 1 on
  four protocols and 1.063 on restXml (section 4.2). restJson1 marshall is 0.271, mostly because the legacy restJson1
  marshaller creates a JSON generator on every call and, on `PutObject_*`, never closes it, so Jackson's buffers are
  not recycled: 17,369–17,411 B/op of the baseline's 19,112–19,139 B/op. A legacy restJson1 client pays the same per
  request. The feature creates no JSON serializer for these blob-payload operations.
- 14 of the 43 marshall cases allocate more than the baseline in the comparable view: 4 each on awsJson1_0,
  rpcv2Cbor and restXml, and 1 each on restJson1 and awsQuery. A fix for the marshall allocation
  (`fix/marshall-allocation`) is in progress. Its metal results will be added to this report later.
- Spread: the per-repetition ranges are within 0.34% of the aggregates and within 1.2% of each protocol geomean
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
  path: the greedy label on the feature's legacy path (FU-11), and a `xmlNamespace` on two input shapes
  (`GetMetricDataRequest`, `PutMetricDataRequest`) whose operations no restXml case uses.
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

Some work sits inside one arm's timed method while the other arm's client does it outside the serde call
(comparability matrix A1–A5). The profiling pass measures the share of each item in every case's CPU samples and
allocated bytes, per arm (`scripts/schema-serde/serde_ab_profile.py compare`, `boundary.tsv` format 2; the ordered
frame rules are in the script's docstring):

- **A1-objects.** The baseline builds its legacy marshaller objects on every call, as the generated
  `<Op>RequestMarshaller` does: the marshaller, its builder and context, the `OperationInfo` checks and the endpoint
  lookup. The feature arm resolves `ClientProtocol` once per trial. The comparable view removes A1-objects from the
  baseline.
- **A1-request.** The legacy marshaller's constructor also starts the request:
  `fillBasicRequestParams` → `ProtocolUtils.createSdkHttpRequest` (the `SdkHttpFullRequest.Builder`, its two
  `LowCopyListMap`s, `Builder.uri` and `SdkHttpUtils.appendUri`), the static headers (`X-Amz-Target`,
  `Content-Type`), Query's `Action` and `Version` parameters, the `?x-id` query on `CopyObject`, and on restJson1
  and restXml the JSON generator or XML writer. The feature does the same work inside `serializeRequest`. So
  A1-request is not a boundary difference, and it stays in the baseline in both views.
- **A2.** The feature unmarshall also runs the status check and attaches `AwsResponseMetadata` and `SdkHttpResponse`
  (`HttpClientProtocol.isErrorResponse` and `responseMetadata`, and the setters that `buildOutput` and
  `buildResponse` call). The legacy client does this in its response handler, outside the baseline timed method. The
  comparable view removes A2 from the feature.
- **A5.** The feature Query marshall writes each string value, and the `Action` and `Version` values,
  percent-encoded into the form body, and sets `Content-Length`. The legacy marshaller only puts the raw strings
  into the request's query map. `QueryParametersToBodyStage` later encodes the keys and values, joins them into the
  body and sets `Content-Length`, outside the baseline timed method. The comparable view removes A5 from the
  feature.
- **A3 and A4.** These are real per-call costs of the feature build with no legacy counterpart: the blob payload copy
  and the response body read. They stay in both views. A3 was fixed during this measurement (section 5).

**As measured** is everything in each arm's timed method.

**Comparable** is an estimate made by `scripts/schema-serde/serde_ab_summary.py summarize --boundary`. It scales the
baseline's mean by 1 − its A1-objects share and the feature's mean by 1 − its A2 + A5 share, using the CPU share for
ns/op and the allocation share for B/op. The shares come from the profiling pass of the pre-FU-12 feature jar
`2549a5b6828`. Set 2 applies the same shares to its three substituted rows. Section 6 lists the estimate's caveats.

**How A1 is split.** Every sample under a legacy construction frame is A1, and so is every sample in a legacy
marshaller builder setter that the benchmark calls directly (`contentType`, `protocolMetadata`, `operationInfo`,
`jsonGenerator` / `xmlGenerator`, `endpoint`, ...). The setters are up to 0.49% of baseline CPU and allocate nothing.
The rule of the previous version left them out of A1. A sample that also has a generator or writer frame
(`BaseAwsJsonProtocolFactory.createGenerator`, Jackson `JsonFactory` / `BufferRecycler`, `XmlGenerator.create`,
`XmlWriter.<init>`, ...) or a request frame (`fillBasicRequestParams`, `ProtocolUtils.createSdkHttpRequest`,
`SdkHttpFullRequest`, `LowCopyListMap`, `SdkHttpUtils`) is A1-request. Every other A1 sample is A1-objects. The split
equals the one made by `fix-marshall-allocation/alloc_mechanisms.py` on all 86 baseline marshall profiles: 0 samples
differ. Its B/op agree with that script's `mechanisms.tsv` within 0.5 B on all 43 marshall cases
(`check-boundary-v2.txt`). Per marshall class, with ns/op and B/op as the share times the baseline's set 1 mean:

| class | A1-objects B/op | A1-request B/op | of which generator / writer B/op | A1-objects ns/op | A1-request ns/op |
|---|---:|---:|---:|---:|---:|
| awsJson1_0 | 70–192 | 446–562 | 0 | 0–40 | 133–279 |
| awsQuery | 40–55 | 537–670 | 0 | 0–37 | 170–223 |
| restJson1 | 84–128 | 1,763–17,888 | 0–17,411 | 28–44 | 538–1,757 |
| restXml | 108–127 | 665–2,031 | 225–253 | 8–16 | 192–609 |
| rpcv2Cbor | 114–215 | 620–739 | 0 | 9–40 | 214–479 |

A1-objects is 0.0–3.2% of baseline CPU on every marshall case, so the comparable marshall ns/op is close to the
as-measured one, except on awsQuery, where A5 is removed from the feature. A1-request is 0.1–44.3% of baseline CPU.

**The restJson1 generator.** `BaseAwsJsonProtocolFactory.createProtocolMarshaller` creates a JSON generator for every
marshaller (`createGenerator`, line 183). The generator returns its Jackson buffers to the recycler only when it is
closed, which `SdkJsonGenerator.getBytes`, `contentSize` and `contentStreamProvider` do. `JsonProtocolMarshaller`
uses the generator only when no payload was set (`finishMarshalling`, line 289). A `PutObject` request has an
explicit blob payload, so the generator is never used or closed, and every call allocates new buffers:
17,369–17,411 B/op on `restJson1_PutObject_*`, about 91% of the baseline's B/op. A legacy restJson1 client pays the
same per request, because its generated marshaller calls `createProtocolMarshaller` on every call
(`JsonMarshallerSpec.java`, line 71). The feature creates no JSON serializer for these operations. This is why
restJson1 marshall allocates far less on the feature (comparable class geomean 0.271). The four files cited here are
the same in the baseline commit `fc8b74e9fe21`.

**A2 is narrower than before.** The previous rule took every sample of the profile category `metadata`. That category
also holds the awsQuery request-id read, which both timed methods do. The legacy
`QueryProtocolUnmarshaller.unmarshall` returns `parseMetadata(document)` with its result (line 88). The feature's
`QueryClientProtocol.deserializeOutput` calls `readRequestId` (line 222), which parses the body a second time (lines
239–249; FU-15 (b)). The request-id pass is 36.5–37.2% of feature CPU on the 3 awsQuery unmarshall cases. The
baseline's `parseMetadata` is 0.09–0.17% of its CPU. The category also held model getters that `toBuilder()` copies
and response constructors call on both arms, up to 0.72% of baseline CPU. A2 now counts only the feature-only
frames, so on awsQuery unmarshall it falls from 36.7–37.6% to 0.03–0.44% of feature CPU. `check_boundary_v2.py`
checks that nothing else moved. The comparable awsQuery unmarshall class geomean is 0.151 ns/op. With the request-id
pass also removed from the feature, it would be 0.095, the previous value.

**What A5 holds.** The A5 rule is the same as before, but the previous version of this report described it wrongly as
percent-encoding. `QueryFormSerializer.writeUrlEncoded` copies each unreserved character of a string value into the
body buffer and escapes the others in the same loop. In the 7 feature Query marshall CPU profiles, the escaping
itself (`writePercentEncoded`, `percentEncode`) is 0.03–0.83% of feature CPU, and the per-character copy is
96.8–99.8% of A5. A5 as a whole is 6.5–31.7% of feature CPU and 0.0% of its allocation. So A5 is mostly the feature
writing its string values into the body.

The legacy client does that work outside the baseline timed method. `QueryParametersToBodyStage` encodes every key
and value of the raw query map (`SdkHttpUtils.encodeAndFlattenFormData`), joins them into a `String` and converts
that to bytes. The 7 baseline Query marshall CPU profiles have 0 samples with a percent-encoding frame
(`UrlEncoding`, `urlEncode`, `percentEncode`, `encodeAndFlattenFormData`, `URLEncoder`). The baseline's timed
method only puts the raw strings into the map (`putRawQueryParameter`). That is a map insert with no per-character
work, and it stays in the baseline. A5 takes out less than the legacy stage does: the feature's key writes, number
values, separators and buffer growth stay in the feature. This follows from the operations each side performs. It
is not a measurement, because the benchmarks do not run the legacy stage. The legacy stage's form-body allocation
(FU-6) is not added to the baseline.

**The `Action` and `Version` write.** A5 also contains the serializer constructor's write of `Action` and
`Version` (`QueryFormSerializer.<init>`). The baseline puts the same two values into its query map in the
marshaller constructor, which is A1-request and stays. This is the same treatment as every other value: the
legacy map put stays, and the feature's encoded write is removed. One could instead pair the constructor's write
with the baseline's put and keep it in the feature. The constructor's write is 0.02–4.90% of feature CPU, so that
choice matters only on the smallest requests. `awsQuery_PutMetricDataRequest_Baseline` would be 1.191 instead of
1.132, and `PutMetricDataRequest_S` 0.394 instead of 0.383. The awsQuery marshall class geomean would be 0.446
instead of 0.439, the awsQuery protocol 0.259 instead of 0.257, and the aggregate 0.3766 instead of 0.3760 ns/op
(0.377 instead of 0.376 at three decimals). B/op does not change, because the write allocates nothing, and the
verdict counts stay the same. This report keeps the rule as it is.

**Why the serde-only view was dropped.** It removed all of A1 from the baseline. A1-request is 446–739 B/op on every
awsJson1_0, awsQuery and rpcv2Cbor marshall case, and up to 93.5% of a restJson1 `PutObject_*` case's B/op. The
feature does that work in its own timed method, so removing it from the baseline alone made the feature's marshall
B/op look higher and its marshall time advantage smaller than they are. The previous A2 rule also removed the
feature's request-id pass, which the baseline does too. The table maps the previous aggregates (set 2) onto the new
ones:

| view | ns/op | B/op |
|---|---:|---:|
| as measured | 0.403 | 0.532 |
| **comparable** (headline) | 0.376 | 0.513 |
| A1 fully removed, new A2 (reference; overstates the baseline's removal) | 0.402 | 0.692 |
| previous version's serde-only (A1 fully removed, old A2) | 0.384 | 0.689 |

Narrowing A2 changes awsQuery unmarshall (0.095 → 0.151 ns/op) and moves the aggregate ns/op from 0.384 to 0.402.
Splitting A1 changes marshall: the aggregate goes from 0.402 to 0.376 ns/op and from 0.692 to 0.513 B/op, mostly
through restJson1 and restXml marshall B/op (class geomeans 1.913 → 0.271 and 1.827 → 1.063). The "A1 fully
removed" row is kept only for this reconciliation.

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
- **ns/op floors.** Median 1.4%, max 18.1%. The max is `restJson1_CopyObjectOutput_Baseline`, a case of about 0.6–0.7 µs.
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

**Uncertainty of the geomeans** (`.agents/tasks/serde-report-comparable/analysis/report_numbers_comparable.py`):

- **Per-rep range.** The statistic computed from each repetition's paired ratios alone, min to max.
- **Bootstrap.** An exact bootstrap over the repetition indices (all 256 resamples). Its intervals agree with the
  per-rep ranges within 0.001.
- **Null aggregate.** The same aggregation on the null experiment gives 0.990 for ns/op and 0.999 for B/op. The
  per-protocol null geomeans are 0.954–1.007 for ns/op; restJson1's 0.954 comes from its 18.1% row. For B/op they
  are 0.995–1.002.

## 3. Results

### 3.1 Result sets and the FU-12 substitution

- **Set 1 (as run).** The full A/B `20261023-031253`: all 71 cases, baseline vs feature `2549a5b6828`.
- **Set 2 (with FU-12).** Set 1 with three rows replaced: `JsonRpc10MarshallBenchmark`
  `awsJson1_0_PutItemRequest_BinaryData_S`, `_M` and `_L`. In set 2 these rows use feature-fu12 / baseline from run
  `20261023-111214`. That run used the same host, settings and baseline jar, and measured the baseline again. The
  comparable factors for the replaced rows use the same profiling-pass shares as in set 1. The feature arm has no
  boundary item in marshall, so only the baseline's A1-objects share matters: the factors are 1.0002–1.0116 for ns/op
  and 1.0003–1.0642 for B/op.

Set 2 includes FU-12 for the awsJson1_0 rows only. The rpcv2Cbor `BinaryData_*` rows stay at their set 1 values,
as decided for set 2. The FU-12 jar does change those rows on metal: `rpcv2Cbor_PutItemRequest_BinaryData_S` is 8.6%
slower in ns/op than on the feature jar in the same run (feature-fu12 / feature 1.086; per repetition 1.075, 1.052,
1.112 and 1.103, against a 3.8%* floor). The per-repetition means are 1,022–1,074 ns/op vs 951–979 ns/op. Against
the baseline, the case is 0.525 on feature-fu12 vs 0.483 on feature. `_M` (0.987) and `_L` (0.974) are not slower.
B/op is unchanged within floor on all three. The cause is not investigated yet (FU-16 in `followups.md`). If the `_S`
row were substituted too, the rpcv2Cbor marshall class geomean would rise by 0.5% (0.3518 → 0.3537 ns/op,
comparable), the rpcv2Cbor protocol by 0.3% and the aggregate by 0.05% (0.3760 → 0.3762). Substituting all three
rpcv2Cbor rows changes the aggregate by 0.02%.

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
| awsJson1_0 `BinaryData_M` | B/op | 1.3%* | 1.214 | 1.210 | −0.4% |
| awsJson1_0 `BinaryData_L` | B/op | 1.3% | 1.078 | 1.076 | −0.2% |
| rpcv2Cbor `BinaryData_S` | B/op | 2.8%* | 1.013 | 1.008 | −0.4% |
| rpcv2Cbor `BinaryData_M` | B/op | 2.8%* | 1.065 | 1.066 | +0.1% |
| rpcv2Cbor `BinaryData_L` | B/op | 2.8% | 0.328 | 0.328 | 0.0% |
| restJson1 / restXml `PutObject_L` | B/op | 0.0% | 0.111 / 1.064 | 0.111 / 1.070 | +0.1% / +0.6% |

All ns/op controls match within the floor. Three B/op cells differ by 0.1–1.3% where the floor is 0.0% or 1.3%. In
each of them, the feature arm's B/op is identical in both runs: 3,058, 2,128 and 2,192 B/op. The difference is on
the baseline side, where B/op moves in steps of about one object per operation between runs. For example, `BinaryData_S`
is 2,668 B/op in set 1 and 2,704 in the FU-12 run, a 36 B step. The substitution is therefore consistent, and set 2
is the headline.

The substituted rows:

| case | baseline ns/op | feature-fu12 ns/op | ratio | comparable | baseline B/op | feature-fu12 B/op | B/op ratio | comparable |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `awsJson1_0_PutItemRequest_BinaryData_S` | 2,000.6 ± 178.8 | 1,091.3 ± 44.2 | 0.546 | 0.552 | 2,704 ± 0 | 2,781 ± 116 | 1.028 | 1.094 |
| `awsJson1_0_PutItemRequest_BinaryData_M` | 22,315.2 ± 2,565.2 | 4,664.6 ± 223.2 | 0.209 | 0.209 | 26,730 ± 0 | 14,448 ± 0 | 0.541 | 0.544 |
| `awsJson1_0_PutItemRequest_BinaryData_L` | 174,288.7 ± 1,333.5 | 32,849.6 ± 1,122.4 | 0.188 | 0.189 | 251,333 ± 123 | 112,034 ± 0 | 0.446 | 0.446 |

In set 1 (feature without FU-12), the same rows are 0.591, 0.321 and 0.291 ns/op and 1.146, 1.214 and 1.078 B/op.

### 3.2 Per class (geomeans)

| class | cases | set 2 comparable ns/op | set 2 comparable B/op | set 2 as measured ns/op | set 2 as measured B/op | set 1 comparable ns/op | set 1 comparable B/op | set 1 as measured ns/op | set 1 as measured B/op |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| JsonRpc10MarshallBenchmark | 14 | 0.348 | 0.812 | 0.346 | 0.781 | 0.372 | 0.924 | 0.370 | 0.888 |
| JsonRpc10UnmarshallBenchmark | 8 | 0.703 | 0.797 | 0.792 | 0.904 | 0.703 | 0.797 | 0.792 | 0.904 |
| QueryMarshallBenchmark | 7 | 0.439 | 0.782 | 0.569 | 0.773 | 0.439 | 0.782 | 0.569 | 0.773 |
| QueryUnmarshallBenchmark | 3 | 0.151 | 0.054 | 0.151 | 0.055 | 0.151 | 0.054 | 0.151 | 0.055 |
| RestJsonMarshallBenchmark | 5 | 0.308 | 0.271 | 0.305 | 0.267 | 0.308 | 0.271 | 0.305 | 0.267 |
| RestJsonUnmarshallBenchmark | 5 | 0.458 | 0.624 | 0.509 | 0.717 | 0.458 | 0.624 | 0.509 | 0.717 |
| RestXmlMarshallBenchmark | 5 | 0.384 | 1.063 | 0.382 | 1.013 | 0.384 | 1.063 | 0.382 | 1.013 |
| RestXmlUnmarshallBenchmark | 5 | 0.304 | 0.364 | 0.347 | 0.419 | 0.304 | 0.364 | 0.347 | 0.419 |
| RpcV2CborMarshallBenchmark | 12 | 0.352 | 0.778 | 0.350 | 0.753 | 0.352 | 0.778 | 0.350 | 0.753 |
| RpcV2CborUnmarshallBenchmark | 7 | 0.601 | 0.894 | 0.660 | 0.978 | 0.601 | 0.894 | 0.660 | 0.978 |

Verdicts per case (set 2):

| view | metric | faster / less | within floor | slower / more |
|---|---|---:|---:|---:|
| comparable | ns/op | 69 | 1 | 1 |
| comparable | B/op | 48 | 2 | 21 |
| as measured | ns/op | 68 | 2 | 1 |
| as measured | B/op | 51 | 3 | 17 |

The per-case tables are in the appendix.

## 4. Where time and allocation go

The source for this section is the metal profiling pass (`20261023-095831`, `profile-comparison.md`). It used
async-profiler `cpu` (perf_events) and `alloc` on both arms and all 71 cases, with `-f 1 -wi 2 -w 2s -i 3 -r 3s` and
`-XX:+DebugNonSafepoints`. All 284 profile runs were ok. The categories follow the rules in
`scripts/schema-serde/serde_ab_profile.py`; they did not change with the A1 split. A category's ns/op and B/op are
its share times the profile run's score, and the profiler perturbs those runs, so use the shares and the order of
magnitude.

The profiles' allocation totals agree with `gc.alloc.rate.norm` within 0.98–1.04 for 131 of the 142 profiles. Seven
are at 0.92–0.97: awsJson1_0 `ShallowMap_L` (both arms), awsJson1_0 and rpcv2Cbor `BinaryData_L` (both arms each),
and the feature arm of `awsQuery_GetMetricDataResponse_L`. The four `GetObject_L` profiles are at 0.70–0.74, where
very large arrays are under-sampled. The tables below are class means over the cases. `alloc_top.py` in the analysis
directory gives the allocation sites cited below.

### 4.1 Per protocol

The categories with at least 5% of CPU (at most four per cell):

| class | baseline: top CPU categories (mean ns/op) | feature: top CPU categories (mean ns/op) |
|---|---|---|
| JsonRpc10 marshall | traversal 27% (8.3 µs), codec_write 23% (8.4 µs), pojo 21% (5.7 µs), format 9% (9.0 µs) | codec_write 42% (4.0 µs), http 20% (0.4 µs), traversal 19% (1.6 µs), bytes 9% (1.8 µs) |
| JsonRpc10 unmarshall | codec_read 68% (18.0 µs), pojo 13% (4.3 µs), bytes 6% (0.4 µs) | codec_read 33% (6.6 µs), traversal 18% (6.0 µs), pojo 14% (3.9 µs), metadata 10% (0.1 µs) |
| Query marshall | traversal 51% (17.3 µs), http 21% (9.6 µs), format 8% (6.6 µs), construction 8% (0.2 µs) | codec_write 45% (9.4 µs), http 18% (0.6 µs), format 14% (4.9 µs), bytes 11% (2.8 µs) |
| Query unmarshall | codec_read 73% (486 µs), format 14% (137 µs) | metadata 37% (38.7 µs), codec_read 33% (38.8 µs), traversal 16% (9.9 µs), format 8% (12.3 µs) |
| RestJson marshall | construction 31% (1.3 µs), http 22% (1.6 µs), traversal 22% (0.9 µs), pojo 18% (0.8 µs) | http 66% (1.5 µs), pojo 17% (1.1 µs), bytes 7% (0.1 µs) |
| RestJson unmarshall | http 18% (1.5 µs), format 18% (2.1 µs), bytes 18% (17.1 µs), traversal 16% (1.0 µs) | http 41% (1.7 µs), bytes 20% (9.8 µs), input 11% (0.4 µs), metadata 10% (0.4 µs) |
| RestXml marshall | traversal 38% (1.3 µs), http 23% (1.4 µs), pojo 21% (0.8 µs), construction 10% (0.4 µs) | http 66% (1.4 µs), pojo 16% (1.0 µs), bytes 8% (0.1 µs) |
| RestXml unmarshall | bytes 18% (16.8 µs), codec_read 17% (3.4 µs), traversal 17% (1.0 µs), format 16% (2.5 µs) | http 34% (1.4 µs), bytes 21% (9.7 µs), metadata 12% (0.4 µs), input 10% (0.4 µs) |
| RpcV2Cbor marshall | traversal 34% (10.1 µs), pojo 25% (6.4 µs), codec_write 19% (4.8 µs), bytes 5% (1.2 µs) | codec_write 43% (3.7 µs), http 20% (0.5 µs), traversal 20% (1.7 µs), bytes 10% (0.8 µs) |
| RpcV2Cbor unmarshall | codec_read 75% (19.6 µs), pojo 15% (4.8 µs) | traversal 26% (6.8 µs), codec_read 25% (3.7 µs), pojo 20% (4.2 µs), bytes 11% (1.8 µs) |

`construction` is A1-objects plus A1-request (section 2.4). On the feature unmarshall rows, `metadata` is A2, except
on Query, where it is the request-id pass that both arms do.

Where the feature build wins:

- **Member dispatch.** The legacy marshallers walk the `SdkField` registry. They dispatch through `MarshallingType`
  and read every value through `SdkField` getters, which shows as traversal plus pojo. Generated `serializeMembers`
  reads the fields directly. Marshall traversal plus pojo drops from 14.0–18.8 µs to 1.6–1.9 µs per call on the
  JsonRpc10, RpcV2Cbor and Query class means. That drop accounts for most of the 0.35–0.44 comparable marshall
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
  unmarshall at 0.151 ns/op and 0.054 B/op (comparable), and `restXml_CopyObjectOutput_M` at 59.0 KB → 3.4 KB.
- **Legacy formatting.** On restJson1 and restXml unmarshall, the legacy path's format category (timestamp and
  header value parsing) is 2.1–2.5 µs per call. The feature's is 0.08 µs. On marshall, the legacy path formats RFC
  822 dates with `DateTimeFormatter` (`DateUtils.formatRfc822Date`, 19–20% of `CopyObjectRequest_M` CPU). The feature
  uses `HttpDateFormat`.

Where it doesn't win:

- **Header and request building on REST marshall.** The feature's http category is 66% of restJson1 and restXml
  marshall CPU. `HttpBindingSerializer.putHeader` builds a case-insensitive `TreeMap`, and
  `CollectionUtils.deepCopyMap` copies it into the request builder's own `TreeMap`. So every bound header is
  inserted twice with case-insensitive compares. The legacy marshaller appends to the builder's map once.
- **Unmarshall allocation on large awsJson1_0 and rpcv2Cbor outputs.** On 7 of the 8 M and L outputs of these two
  classes, the feature allocates more. In the comparable view, that is 1.03–1.23× on awsJson1_0
  (`GetItemOutputBinary_M` is 0.90) and 1.05–1.28× on rpcv2Cbor. The feature reads the whole response into one
  `byte[]` (`ResponseBodyReader.read`, 17.5 KB of 87.6 KB on `awsJson1_0_GetItemOutput_L`), because the ported
  parsers need a contiguous array. The legacy Jackson parsers stream from the `InputStream`. The model objects
  themselves cost about the same on both arms, for example 12.8 vs 13.0 KB of `AttributeValue` builders on
  `awsJson1_0_GetItemOutput_L`.
- **awsQuery request id.** On Query unmarshall, the request-id pass is 37% of feature CPU (38.7 µs per call on
  average; category `metadata`). It parses the body a second time (FU-15 (b)). The baseline extracts the request id
  in its timed method too, so the comparable view keeps the pass in the feature. awsQuery unmarshall is 0.151
  comparable. Without the second parse, it would be 0.095.

### 4.2 Marshall allocation

Per protocol, set 2, marshall class geomeans of B/op and the cases that allocate more than their floor allows:

| protocol | as measured marshall B/op | comparable marshall B/op | cases with more B/op (comparable) | A1-objects B/op | A1-request B/op |
|---|---:|---:|---:|---:|---:|
| awsJson1_0 | 0.781 | 0.812 | 4 of 14 | 70–192 | 446–562 |
| awsQuery | 0.773 | 0.782 | 1 of 7 | 40–55 | 537–670 |
| restJson1 | 0.267 | 0.271 | 1 of 5 | 84–128 | 1,763–17,888 |
| restXml | 1.013 | 1.063 | 4 of 5 | 108–127 | 665–2,031 |
| rpcv2Cbor | 0.753 | 0.778 | 4 of 12 | 114–215 | 620–739 |

The A1-objects and A1-request columns are the baseline's per-case ranges (section 2.4). By protocol, in the
comparable view:

- **restXml.** 4 of 5 cases allocate more: `PutObject_S/M/L` 1.135, 1.139 and 1.134, and `CopyObjectRequest_M`
  1.115. `CopyObjectRequest_Baseline` allocates less (0.830).
- **restJson1.** `CopyObjectRequest_M` allocates more (1.157). `PutObject_S/M/L` are 0.112 because of the legacy
  generator buffers (section 2.4). `CopyObjectRequest_Baseline` is 0.910.
- **awsJson1_0.** 4 of 14 cases allocate more: `HealthcheckRequest_Example` 1.015, `ShallowMap_S` 1.072,
  `ShallowMap_L` 1.075 and `BinaryData_S` 1.094. `ShallowMap_M` (1.013) is within floor.
- **rpcv2Cbor.** 4 of 12 cases allocate more: `ShallowMap_S` 1.058, `ShallowMap_L` 1.070, `BinaryData_S` 1.073
  and `BinaryData_M` 1.083. `PutItemRequest_Baseline` (1.020) is within floor.
- **awsQuery.** `PutMetricDataRequest_Baseline` allocates 2.489× the baseline. The other 6 cases allocate less.

That is 14 marshall cases. With the 7 unmarshall cases of section 4.1, they make up the 21 "more" verdicts in
section 3.2. The larger requests allocate less, for example awsJson1_0 `Nested_L` 0.556, rpcv2Cbor `MixedItem_L`
0.583 and `BinaryData_L` 0.328.

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

  The legacy baseline builds the same request in A1-request, so these sites are what is left above it: restXml
  `PutObject_*` is 1.13–1.14× and the two `CopyObjectRequest_M` cases are 1.12× (restXml) and 1.16× (restJson1).
- **awsJson1_0 and rpcv2Cbor.** On every call, the codec serializer allocates a fresh scratch buffer of about 1 KiB:
  `SmithyJsonSerializer.<init>` 1,051 B and `CborSerializer.<init>` 1,035 B on `PutItemRequest_Baseline`. That is
  37–43% of the feature's B/op. The legacy 1 KiB `SdkByteArrayOutputStream` is itself the request body. The feature
  writes into a separate, size-hinted body buffer as well (`ByteBufferOutputStream`, `newRequestBodyBuffer`). On
  small requests this fixed cost decides the ratio. On large ones, the smaller per-member structures win.
- **awsQuery.** `QueryFormSerializer` allocates a new serializer and a 1,181 B buffer on every call: 50% of the
  2,344 B/op on `PutMetricDataRequest_Baseline`. FU-4 records the missing pool. That case's baseline allocates
  994 B/op, 942 B/op without A1-objects, which is why the ratio is 2.489.

The `fix/marshall-allocation` workflow works on these sites. Its metal results will be added to this report later.

### 4.3 The case slower in ns/op (comparable)

| case | comparable | as measured | floor |
|---|---:|---:|---:|
| `awsQuery_PutMetricDataRequest_Baseline` | 1.132 | 1.186 | 2.5% |

- **`awsQuery_PutMetricDataRequest_Baseline`** (0.46 vs 0.55 µs; the smallest Query request).
  - **Baseline.** A1 is 40% of baseline CPU. 37.9 points of it are A1-request (the request builder, URI and the
    `Action` and `Version` parameters), which stays in the baseline. A1-objects is 1.9%.
  - **Feature: URI.** 40% of feature CPU is self time in `SdkHttpFullRequest.Builder.uri`. This sets protocol, host,
    port and path from the endpoint `URI`; 45% inclusive vs 12% on the baseline.
  - **Feature: form serializer.** 19% is building the per-call `QueryFormSerializer` (`forAwsQuery`, FU-4).
  - **Feature: A5.** 6.5% is A5, which the comparable view removes. 4.9 points of it are the serializer
    constructor writing `Action` and `Version`, and the rest is the copy of the string values. The baseline's put of
    `Action` and `Version` stays in A1-request (section 2.4). Keeping the constructor's write in the feature as
    well would make this case 1.191 instead of 1.132.

  As measured, the case is 1.19× slower. It is the only case slower than its floor in either view.

The two header-heavy `CopyObjectRequest_M` cases were slower in the previous serde-only view. In the comparable view,
`restJson1_CopyObjectRequest_M` is faster (0.976) and `restXml_CopyObjectRequest_M` is within its floor (1.001).
Their feature cost is the double header insertion of section 4.1: `TreeMap.put` is 72–77% of feature CPU
(inclusive), and 38.6–41% is self time in `StringLatin1.compareToCI`. The baseline spends 19–20% formatting RFC 822
dates instead.

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

- **Comparable is an estimate.** It scales each arm by work-boundary shares from a separate profiling run of the
  pre-FU-12 feature jar. The A1 split is made by frame rules (`serde_ab_profile.py`), and A1-request includes the
  restJson1 generator buffers that the feature does not allocate (section 2.4). A5 is mostly the feature's
  per-character copy of string values into the form body, not escaping. By the operations each side performs it
  removes less than the legacy `QueryParametersToBodyStage` does, but that stage is not measured, and its
  allocation is not credited to the baseline. A5 includes the feature's `Action` / `Version` write, while the
  baseline's put of those values stays; keeping that write in the feature would move the aggregate from 0.3760 to
  0.3766 ns/op. A2 no longer removes the awsQuery request-id pass. The as-measured view has no estimate in it.
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
- **Marshall allocation fix in progress.** A separate workflow (`fix/marshall-allocation`) is working on the sites in
  section 4.2. Its metal results will be added to this report later. This report measures `2549a5b6828` and
  `b17e79d6e1f` only.
- **Benchmark data quirk.** The restJson1 and restXml `GetObject_*` responses declare 1, 1,000 and 256,000 bytes but
  send the base64 text. Both arms read them the same way. The L case is dominated by those body copies on both
  arms (about 1.1–1.4 MB/op).

## 7. Reproduction

Raw data is not committed. It lives in the main checkout under `.agents/tasks/serde-benchmark-metal/` and in the jar
caches. The comparable view was recomputed from that data under `.agents/tasks/serde-report-comparable/`; the
original `20261023-095831/boundary.tsv` (format 1) is kept unchanged.

| what | local path |
|---|---|
| null experiment | `.agents/tasks/serde-benchmark-metal/runs/20261023-011610/` (`serde-ab.json`, `summary.md`, `jmh/`, `verify/`) |
| full A/B | `.agents/tasks/serde-benchmark-metal/runs/20261023-031253/` |
| profiling pass | `.agents/tasks/serde-benchmark-metal/runs/20261023-095831/` (`profile-comparison.md`, `boundary.tsv` format 1, `profile-path.tsv`, `profile/<arm>/<class>/<case>/<event>/`) |
| FU-12 run | `.agents/tasks/serde-benchmark-metal/runs/20261023-111214/` |
| work-boundary shares, format 2 | `.agents/tasks/serde-report-comparable/profile-20261023-095831/` (`boundary.tsv`, `profile-comparison.md`, `profile-path.tsv`) |
| boundary checks | `.agents/tasks/serde-report-comparable/check_boundary_v2.py` → `check-boundary-v2.txt`, `a1-split.tsv`, `a2-evidence.tsv` |
| summaries | `.agents/tasks/serde-report-comparable/ab-summary-comparable.md`, `fu12-summary.md`, `ab-summary-format1.md` |
| report numbers | `.agents/tasks/serde-report-comparable/analysis/report_numbers_comparable.py` → `report-numbers-comparable.md`; `alloc-top.txt` (`serde-benchmark-metal/analysis/alloc_top.py`); `rest-alloc-groups.txt` |
| report check | `.agents/tasks/serde-report-comparable/analysis/verify_report_comparable.py` → `verify-report-output.txt` |
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

**Work-boundary shares, summaries and report numbers** (local, after `fetch`; `compare --out-dir` writes the format-2
files without touching the fetched run):

```bash
RUNS=.agents/tasks/serde-benchmark-metal/runs; C=.agents/tasks/serde-report-comparable
python3 scripts/schema-serde/serde_ab_profile.py compare $RUNS/20261023-095831 \
  --out-dir $C/profile-20261023-095831
python3 $C/check_boundary_v2.py > $C/check-boundary-v2.txt
python3 scripts/schema-serde/serde_ab_summary.py summarize $RUNS/20261023-031253/serde-ab.json \
  --null $RUNS/20261023-011610/serde-ab.json --boundary $C/profile-20261023-095831/boundary.tsv
python3 scripts/schema-serde/serde_ab_summary.py summarize $RUNS/20261023-111214/serde-ab.json \
  --null $RUNS/20261023-011610/serde-ab.json
python3 $C/analysis/report_numbers_comparable.py > $C/report-numbers-comparable.md
python3 $C/analysis/verify_report_comparable.py
```

## Appendix: per-case results (set 2)

The tables below give, for each case:

- the baseline and feature means ± their 99.9% half-width over the 4 repetitions;
- the as-measured ratio and the comparable ratio (section 2.4);
- the case's floor and its verdict in the comparable view.

A floor marked * is inferred from the class max. The rows marked † are the FU-12 substitutions from run
`20261023-111214`. All other rows are from run `20261023-031253`. For B/op, the verdict words are "less" and "more".

### awsJson1_0 marshall (`JsonRpc10MarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| awsJson1_0_GetItemInput_Baseline | 1,453.5 ± 88.4 | 702.3 ± 26.1 | 0.483 | 0.491 | 2.7%* | faster | 2,695 ± 116 | 2,456 ± 0 | 0.911 | 0.976 | 1.3%* | less |
| awsJson1_0_HealthcheckRequest_Example | 691.6 ± 51.1 | 508.9 ± 26.2 | 0.736 | 0.760 | 2.7% | faster | 2,554 ± 134 | 2,432 ± 0 | 0.952 | 1.015 | 0.0% | more |
| awsJson1_0_PutItemRequest_Baseline | 1,502.9 ± 82.4 | 703.2 ± 31.7 | 0.468 | 0.476 | 2.7%* | faster | 2,686 ± 134 | 2,456 ± 0 | 0.914 | 0.974 | 1.3%* | less |
| awsJson1_0_PutItemRequest_ShallowMap_S | 4,776.2 ± 466.2 | 1,518.4 ± 103.3 | 0.318 | 0.320 | 2.7%* | faster | 2,695 ± 116 | 2,720 ± 0 | 1.009 | 1.072 | 1.3%* | more |
| awsJson1_0_PutItemRequest_ShallowMap_M | 26,897.8 ± 813.6 | 7,905.7 ± 41.6 | 0.294 | 0.294 | 2.7%* | faster | 4,801 ± 0 | 4,705 ± 0 | 0.980 | 1.013 | 1.3%* | within floor |
| awsJson1_0_PutItemRequest_ShallowMap_L | 88,573.8 ± 3,041.1 | 24,840.2 ± 104.1 | 0.280 | 0.280 | 2.7%* | faster | 9,430 ± 68 | 9,961 ± 0 | 1.056 | 1.075 | 1.3%* | more |
| awsJson1_0_PutItemRequest_Nested_M | 3,777.2 ± 189.3 | 1,368.9 ± 59.3 | 0.362 | 0.366 | 2.7%* | faster | 3,202 ± 388 | 2,528 ± 0 | 0.790 | 0.836 | 1.3%* | less |
| awsJson1_0_PutItemRequest_Nested_L | 12,615.9 ± 147.4 | 4,290.1 ± 154.3 | 0.340 | 0.341 | 2.7%* | faster | 6,705 ± 0 | 3,648 ± 0 | 0.544 | 0.556 | 1.3%* | less |
| awsJson1_0_PutItemRequest_MixedItem_S | 5,620.1 ± 516.0 | 1,831.3 ± 42.4 | 0.326 | 0.327 | 2.7%* | faster | 3,130 ± 134 | 2,648 ± 0 | 0.846 | 0.895 | 1.3%* | less |
| awsJson1_0_PutItemRequest_MixedItem_M | 28,588.4 ± 1,429.4 | 8,556.4 ± 226.0 | 0.299 | 0.300 | 2.7%* | faster | 6,610 ± 0 | 4,425 ± 0 | 0.669 | 0.686 | 1.3%* | less |
| awsJson1_0_PutItemRequest_MixedItem_L | 83,401.9 ± 10,872.0 | 25,558.5 ± 467.5 | 0.307 | 0.307 | 2.7%* | faster | 13,390 ± 157 | 8,441 ± 0 | 0.630 | 0.636 | 1.3%* | less |
| awsJson1_0_PutItemRequest_BinaryData_S † | 2,000.6 ± 178.8 | 1,091.3 ± 44.2 | 0.546 | 0.552 | 2.7%* | faster | 2,704 ± 0 | 2,781 ± 116 | 1.028 | 1.094 | 1.3%* | more |
| awsJson1_0_PutItemRequest_BinaryData_M † | 22,315.2 ± 2,565.2 | 4,664.6 ± 223.2 | 0.209 | 0.209 | 2.7%* | faster | 26,730 ± 0 | 14,448 ± 0 | 0.541 | 0.544 | 1.3%* | less |
| awsJson1_0_PutItemRequest_BinaryData_L † | 174,288.7 ± 1,333.5 | 32,849.6 ± 1,122.4 | 0.188 | 0.189 | 1.0% | faster | 251,333 ± 123 | 112,034 ± 0 | 0.446 | 0.446 | 1.3% | less |

### awsJson1_0 unmarshall (`JsonRpc10UnmarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| awsJson1_0_GetItemOutput_Baseline | 362.5 ± 90.4 | 342.2 ± 74.7 | 0.946 | 0.646 | 3.6%* | faster | 1,068 ± 90 | 768 ± 0 | 0.719 | 0.495 | 1.2%* | less |
| awsJson1_0_GetItemOutput_S | 2,894.0 ± 130.6 | 2,030.7 ± 64.4 | 0.702 | 0.660 | 3.6%* | faster | 3,272 ± 0 | 2,960 ± 0 | 0.905 | 0.832 | 1.2%* | less |
| awsJson1_0_GetItemOutput_M | 10,466.9 ± 408.4 | 8,065.0 ± 813.6 | 0.771 | 0.758 | 3.6%* | faster | 11,209 ± 0 | 11,751 ± 75 | 1.048 | 1.027 | 1.2%* | more |
| awsJson1_0_GetItemOutput_L | 75,969.0 ± 3,426.0 | 59,279.2 ± 499.8 | 0.780 | 0.778 | 3.6%* | faster | 71,109 ± 53 | 87,565 ± 347 | 1.231 | 1.229 | 1.2%* | more |
| awsJson1_0_GetItemOutputBinary_S | 2,999.6 ± 271.8 | 2,127.3 ± 71.7 | 0.709 | 0.667 | 3.6%* | faster | 3,712 ± 0 | 2,952 ± 0 | 0.795 | 0.732 | 1.2%* | less |
| awsJson1_0_GetItemOutputBinary_M | 11,823.3 ± 380.0 | 8,516.4 ± 125.6 | 0.720 | 0.709 | 3.6%* | faster | 13,192 ± 39 | 12,181 ± 151 | 0.923 | 0.904 | 1.2%* | less |
| awsJson1_0_GetItemOutputBinary_L | 87,829.1 ± 6,476.8 | 65,209.2 ± 1,432.8 | 0.743 | 0.740 | 0.6% | faster | 89,006 ± 146 | 91,603 ± 3,278 | 1.029 | 1.026 | 1.2% | more |
| awsJson1_0_HealthcheckResponse_Example | 305.6 ± 28.9 | 311.6 ± 14.3 | 1.020 | 0.675 | 3.6% | faster | 991 ± 65 | 696 ± 0 | 0.702 | 0.460 | 0.0% | less |

### awsQuery marshall (`QueryMarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| awsQuery_GetMetricDataRequest_S | 5,328.4 ± 635.1 | 2,105.3 ± 58.8 | 0.395 | 0.306 | 2.5%* | faster | 6,676 ± 155 | 2,784 ± 0 | 0.417 | 0.420 | 0.0%* | less |
| awsQuery_GetMetricDataRequest_M | 19,600.4 ± 2,185.1 | 8,859.6 ± 302.5 | 0.452 | 0.314 | 2.5%* | faster | 26,232 ± 39 | 16,457 ± 0 | 0.627 | 0.628 | 0.0%* | less |
| awsQuery_GetMetricDataRequest_L | 42,623.8 ± 1,128.1 | 17,614.9 ± 765.2 | 0.413 | 0.282 | 2.5%* | faster | 53,249 ± 75 | 37,002 ± 90 | 0.695 | 0.696 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_Baseline | 462.0 ± 139.1 | 547.2 ± 19.3 | 1.186 | 1.132 | 2.5% | slower | 994 ± 100 | 2,344 ± 0 | 2.359 | 2.489 | 0.0% | more |
| awsQuery_PutMetricDataRequest_S | 2,880.0 ± 207.6 | 1,425.6 ± 52.3 | 0.495 | 0.383 | 2.5%* | faster | 4,228 ± 891 | 2,752 ± 0 | 0.651 | 0.658 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_M | 15,591.6 ± 1,497.6 | 12,361.5 ± 4,141.7 | 0.793 | 0.552 | 2.5%* | faster | 19,653 ± 63 | 13,225 ± 0 | 0.673 | 0.675 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_L | 163,030.7 ± 10,134.3 | 91,679.3 ± 1,386.6 | 0.562 | 0.485 | 0.3% | faster | 209,804 ± 15,381 | 184,390 ± 91 | 0.879 | 0.879 | 0.0% | less |

### awsQuery unmarshall (`QueryUnmarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| awsQuery_GetMetricDataResponse_S | 48,017.7 ± 1,026.9 | 7,208.0 ± 98.6 | 0.150 | 0.149 | 0.5% | faster | 101,193 ± 77 | 5,073 ± 0 | 0.050 | 0.048 | 0.7% | less |
| awsQuery_GetMetricDataResponse_M | 153,056.4 ± 6,716.7 | 23,856.6 ± 391.8 | 0.156 | 0.156 | 0.5%* | faster | 270,307 ± 2,619 | 15,374 ± 116 | 0.057 | 0.056 | 0.7%* | less |
| awsQuery_GetMetricDataResponse_L | 1,820,590.4 ± 39,908.2 | 268,796.6 ± 4,196.7 | 0.148 | 0.148 | 0.2% | faster | 2,702,261 ± 24,168 | 161,691 ± 52 | 0.060 | 0.060 | 0.0% | less |

### restJson1 marshall (`RestJsonMarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| restJson1_CopyObjectRequest_Baseline | 3,005.7 ± 155.7 | 881.5 ± 53.6 | 0.293 | 0.296 | 1.2% | faster | 2,736 ± 0 | 2,376 ± 0 | 0.868 | 0.910 | 0.8% | less |
| restJson1_CopyObjectRequest_M | 10,188.9 ± 319.2 | 9,905.6 ± 202.0 | 0.972 | 0.976 | 1.2%* | faster | 8,641 ± 0 | 9,854 ± 39 | 1.140 | 1.157 | 0.8%* | more |
| restJson1_PutObject_S | 3,931.3 ± 243.9 | 826.4 ± 11.7 | 0.210 | 0.212 | 1.2%* | faster | 19,112 ± 127 | 2,128 ± 0 | 0.111 | 0.112 | 0.8%* | less |
| restJson1_PutObject_M | 3,969.5 ± 336.9 | 836.7 ± 51.2 | 0.211 | 0.213 | 1.2%* | faster | 19,139 ± 183 | 2,128 ± 0 | 0.111 | 0.112 | 0.8%* | less |
| restJson1_PutObject_L | 3,979.4 ± 221.6 | 837.5 ± 26.8 | 0.210 | 0.213 | 1.1% | faster | 19,139 ± 39 | 2,128 ± 0 | 0.111 | 0.112 | 0.0% | less |

### restJson1 unmarshall (`RestJsonUnmarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| restJson1_CopyObjectOutput_Baseline | 1,312.9 ± 144.7 | 576.8 ± 390.6 | 0.440 | 0.367 | 18.1% | faster | 1,288 ± 0 | 1,048 ± 184 | 0.814 | 0.620 | 4.3% | less |
| restJson1_CopyObjectOutput_M | 5,413.0 ± 205.4 | 3,700.9 ± 271.8 | 0.684 | 0.609 | 18.1%* | faster | 4,388 ± 167 | 3,402 ± 233 | 0.775 | 0.631 | 4.3%* | less |
| restJson1_GetObject_S | 7,071.3 ± 793.7 | 3,116.2 ± 293.3 | 0.441 | 0.401 | 18.1%* | faster | 6,550 ± 39 | 3,402 ± 78 | 0.519 | 0.448 | 4.3%* | less |
| restJson1_GetObject_M | 7,405.6 ± 537.3 | 3,414.1 ± 69.0 | 0.461 | 0.405 | 18.1%* | faster | 9,230 ± 39 | 6,698 ± 77 | 0.726 | 0.675 | 4.3%* | less |
| restJson1_GetObject_L | 111,632.2 ± 10,248.7 | 62,681.6 ± 5,756.6 | 0.562 | 0.558 | 1.4% | faster | 1,392,494 ± 1 | 1,111,708 ± 2 | 0.798 | 0.798 | 0.0% | less |

### restXml marshall (`RestXmlMarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| restXml_CopyObjectRequest_Baseline | 2,776.6 ± 534.0 | 864.5 ± 75.2 | 0.312 | 0.313 | 1.1% | faster | 3,056 ± 0 | 2,440 ± 0 | 0.798 | 0.830 | 0.8% | less |
| restXml_CopyObjectRequest_M | 9,911.4 ± 1,051.3 | 9,901.9 ± 441.9 | 0.999 | 1.001 | 2.7%* | within floor | 9,003 ± 134 | 9,921 ± 0 | 1.102 | 1.115 | 0.8%* | more |
| restXml_PutObject_S | 2,885.4 ± 224.8 | 844.8 ± 54.5 | 0.293 | 0.294 | 2.7%* | faster | 2,054 ± 78 | 2,192 ± 0 | 1.067 | 1.135 | 0.8%* | more |
| restXml_PutObject_M | 2,841.6 ± 266.3 | 846.6 ± 23.0 | 0.298 | 0.299 | 2.7%* | faster | 2,048 ± 0 | 2,192 ± 0 | 1.070 | 1.139 | 0.8%* | more |
| restXml_PutObject_L | 2,859.2 ± 254.2 | 856.4 ± 10.7 | 0.300 | 0.300 | 2.7% | faster | 2,060 ± 155 | 2,192 ± 0 | 1.064 | 1.134 | 0.0% | more |

### restXml unmarshall (`RestXmlUnmarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| restXml_CopyObjectOutput_Baseline | 1,214.1 ± 91.6 | 281.6 ± 43.6 | 0.232 | 0.182 | 3.8% | faster | 1,000 ± 0 | 788 ± 155 | 0.788 | 0.583 | 4.8% | less |
| restXml_CopyObjectOutput_M | 25,951.3 ± 913.1 | 3,970.8 ± 316.2 | 0.153 | 0.128 | 3.8%* | faster | 58,971 ± 147 | 3,363 ± 278 | 0.057 | 0.048 | 4.8%* | less |
| restXml_GetObject_S | 6,047.7 ± 404.1 | 2,942.2 ± 104.7 | 0.487 | 0.440 | 3.8%* | faster | 6,580 ± 89 | 3,306 ± 233 | 0.502 | 0.433 | 4.8%* | less |
| restXml_GetObject_M | 6,337.4 ± 646.4 | 3,254.2 ± 77.9 | 0.514 | 0.455 | 3.8%* | faster | 9,257 ± 73 | 6,602 ± 134 | 0.713 | 0.663 | 4.8%* | less |
| restXml_GetObject_L | 110,518.5 ± 9,635.9 | 62,318.8 ± 4,871.7 | 0.564 | 0.559 | 1.3% | faster | 1,392,634 ± 90 | 1,111,660 ± 1 | 0.798 | 0.798 | 0.0% | less |

### rpcv2Cbor marshall (`RpcV2CborMarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| rpcv2Cbor_PutItemRequest_Baseline | 1,688.5 ± 57.4 | 821.8 ± 84.9 | 0.487 | 0.493 | 3.8% | faster | 2,859 ± 110 | 2,744 ± 179 | 0.960 | 1.020 | 2.8% | within floor |
| rpcv2Cbor_PutItemRequest_ShallowMap_S | 4,695.6 ± 216.7 | 1,691.2 ± 70.8 | 0.360 | 0.362 | 3.8%* | faster | 2,887 ± 142 | 2,888 ± 0 | 1.000 | 1.058 | 2.8%* | more |
| rpcv2Cbor_PutItemRequest_ShallowMap_M | 27,291.5 ± 5,424.8 | 8,045.4 ± 3,493.9 | 0.295 | 0.295 | 3.8%* | faster | 5,266 ± 3,159 | 4,445 ± 297 | 0.849 | 0.879 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_ShallowMap_L | 83,691.3 ± 3,481.1 | 21,362.9 ± 330.0 | 0.255 | 0.255 | 3.8%* | faster | 8,152 ± 87 | 8,601 ± 0 | 1.055 | 1.070 | 2.8%* | more |
| rpcv2Cbor_PutItemRequest_Nested_M | 3,878.3 ± 381.3 | 1,555.7 ± 179.9 | 0.401 | 0.404 | 3.8%* | faster | 3,468 ± 478 | 2,800 ± 0 | 0.808 | 0.849 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_Nested_L | 12,879.3 ± 264.2 | 3,861.5 ± 102.7 | 0.300 | 0.301 | 3.8%* | faster | 8,297 ± 238 | 3,816 ± 0 | 0.460 | 0.469 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_MixedItem_S | 5,526.9 ± 621.3 | 1,715.0 ± 88.3 | 0.310 | 0.312 | 3.8%* | faster | 3,417 ± 116 | 2,904 ± 0 | 0.850 | 0.893 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_MixedItem_M | 27,625.7 ± 997.7 | 7,006.7 ± 108.2 | 0.254 | 0.254 | 3.8%* | faster | 6,921 ± 158 | 4,137 ± 210 | 0.598 | 0.615 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_MixedItem_L | 80,244.0 ± 2,838.9 | 19,764.3 ± 36.8 | 0.246 | 0.246 | 3.8%* | faster | 12,370 ± 68 | 7,137 ± 0 | 0.577 | 0.583 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_BinaryData_S | 1,978.4 ± 291.4 | 973.5 ± 177.7 | 0.492 | 0.501 | 3.8%* | faster | 2,868 ± 121 | 2,904 ± 179 | 1.013 | 1.073 | 2.8%* | more |
| rpcv2Cbor_PutItemRequest_BinaryData_M | 6,099.2 ± 762.8 | 2,955.8 ± 94.9 | 0.485 | 0.487 | 3.8%* | faster | 10,281 ± 243 | 10,952 ± 0 | 1.065 | 1.083 | 2.8%* | more |
| rpcv2Cbor_PutItemRequest_BinaryData_L | 26,967.9 ± 1,361.5 | 12,438.8 ± 518.2 | 0.461 | 0.462 | 1.8% | faster | 243,773 ± 127 | 79,987 ± 45 | 0.328 | 0.328 | 2.8% | less |

### rpcv2Cbor unmarshall (`RpcV2CborUnmarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| rpcv2Cbor_GetItemOutput_Baseline | 359.3 ± 34.7 | 330.0 ± 68.8 | 0.919 | 0.586 | 4.1% | faster | 1,141 ± 65 | 689 ± 90 | 0.604 | 0.395 | 1.5% | less |
| rpcv2Cbor_GetItemOutput_S | 2,978.8 ± 112.4 | 1,694.9 ± 74.3 | 0.569 | 0.528 | 4.1%* | faster | 3,456 ± 0 | 3,072 ± 0 | 0.889 | 0.821 | 3.7%* | less |
| rpcv2Cbor_GetItemOutput_M | 11,189.3 ± 326.8 | 6,735.3 ± 145.3 | 0.602 | 0.589 | 4.1%* | faster | 11,305 ± 0 | 12,073 ± 60 | 1.068 | 1.047 | 3.7%* | more |
| rpcv2Cbor_GetItemOutput_L | 75,364.6 ± 5,579.6 | 47,000.5 ± 2,336.6 | 0.624 | 0.621 | 4.1%* | faster | 71,188 ± 2 | 83,544 ± 660 | 1.174 | 1.171 | 3.7%* | more |
| rpcv2Cbor_GetItemOutputBinary_S | 2,911.6 ± 76.9 | 1,725.8 ± 88.0 | 0.593 | 0.548 | 4.1%* | faster | 3,544 ± 0 | 3,032 ± 0 | 0.856 | 0.788 | 3.7%* | less |
| rpcv2Cbor_GetItemOutputBinary_M | 10,829.1 ± 312.7 | 7,391.9 ± 667.9 | 0.683 | 0.669 | 4.1%* | faster | 11,714 ± 100 | 13,582 ± 88 | 1.159 | 1.139 | 3.7%* | more |
| rpcv2Cbor_GetItemOutputBinary_L | 74,632.0 ± 4,393.3 | 51,094.4 ± 847.5 | 0.685 | 0.681 | 1.4% | faster | 74,914 ± 105 | 96,195 ± 91 | 1.284 | 1.280 | 3.7% | more |
