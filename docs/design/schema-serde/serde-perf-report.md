# Serde performance report: schema-based serde vs legacy serde (EC2 metal)

This report compares the cost of request serialization and response deserialization (serde) between the
baseline build and the feature build. The baseline is stock V2 with the legacy marshallers and unmarshallers.
The feature build uses schema-based serde with the codecs ported from smithy-java. All results come from the 10
`benchmark.serde` JMH classes of `test/sdk-standard-benchmarks`, run on a dedicated m6g.metal host. The headline includes the marshall allocation fix
(`fix/marshall-allocation`, section 6). This report covers serde only. The end-to-end report (`perf-report.md`) is
separate and still preliminary.

## 1. Summary

Across the five protocols, the feature build's serde is **3.1× faster (ratio 0.319) and allocates 62% less (ratio
0.383)**. This is the comparable view of set 3, which includes the FU-12 blob member fix and the marshall allocation
fix (`fix/marshall-allocation`, merged at `a8557684f83`; section 6). Each figure is the geometric mean of the five
per-protocol geometric means of feature / baseline. Lower ratios favor the feature build.

Set 3 is set 2 with its 43 marshall rows replaced by the feature-marshall / baseline ratios of run
`20261023-212558`. That run measured the same baseline jar again, on the same host with the same settings. The 28
unmarshall rows keep their set 2 values, because the fix changes no code on the response path (section 6.2). Before
the marshall fix (set 2), the same aggregate was 2.7× faster (0.376) with 49% less allocation (0.513).

The comparable view takes out of each arm's timed method only the work that the other arm's client does outside its
serde call (section 2.4). On the baseline, that is the legacy marshaller objects it builds on every call
(A1-objects). On the feature, it is the response status check and metadata attachment (A2) and the encoded writes of
the Query form values (A5), which the legacy client does after its marshaller. The request work inside the legacy
marshaller's constructor (request builder, URI, static headers, JSON generator or XML writer; A1-request) stays in
the baseline, because the feature does the same work inside its own serde call.

Measured as each arm's whole timed method (the as-measured view), the set 3 aggregate is 2.9× faster (0.343) with 60%
less allocation (0.398). Set 2 was 2.5× faster (0.403) with 47% less allocation (0.532). An earlier version of this
report led with a serde-only estimate that also took A1-request out of the baseline. Section 2.4 explains why that
estimate is no longer used and how its numbers map onto the new ones.

Neither view includes signing, the execution pipeline or an HTTP client. Both views call the protocol directly.

Per protocol (set 3). Each protocol's geomean is the geometric mean of its marshall-class geomean and its
unmarshall-class geomean. The range is the protocol geomean computed from each of the 4 repetitions alone.

**ns/op, feature / baseline (set 3)**

| protocol | comparable marshall | comparable unmarshall | **comparable protocol** (per-rep range) | as measured marshall | as measured unmarshall | as measured protocol |
|---|---:|---:|---:|---:|---:|---:|
| awsJson1_0 | 0.310 | 0.703 | **0.466** (0.466–0.468) | 0.307 | 0.792 | 0.493 |
| awsQuery | 0.369 | 0.151 | **0.236** (0.235–0.237) | 0.500 | 0.151 | 0.275 |
| restJson1 | 0.171 | 0.458 | **0.280** (0.276–0.283) | 0.170 | 0.509 | 0.294 |
| restXml | 0.207 | 0.304 | **0.251** (0.250–0.252) | 0.206 | 0.347 | 0.267 |
| rpcv2Cbor | 0.305 | 0.601 | **0.428** (0.426–0.430) | 0.303 | 0.660 | 0.447 |
| **aggregate** (geomean of the 5) | | | **0.319** (0.318–0.320), 3.1× faster | | | 0.343 (0.342–0.344), 2.9× faster |
| pooled, all 71 cases (reference) | | | 0.342 | | | 0.365 |

**B/op (`gc.alloc.rate.norm`), feature / baseline (set 3)**

| protocol | comparable marshall | comparable unmarshall | **comparable protocol** (per-rep range) | as measured marshall | as measured unmarshall | as measured protocol |
|---|---:|---:|---:|---:|---:|---:|
| awsJson1_0 | 0.513 | 0.797 | **0.639** (0.638–0.640) | 0.492 | 0.904 | 0.667 |
| awsQuery | 0.449 | 0.054 | **0.156** (0.156–0.157) | 0.444 | 0.055 | 0.157 |
| restJson1 | 0.137 | 0.624 | **0.293** (0.292–0.294) | 0.135 | 0.717 | 0.311 |
| restXml | 0.522 | 0.364 | **0.436** (0.432–0.437) | 0.498 | 0.419 | 0.457 |
| rpcv2Cbor | 0.473 | 0.894 | **0.650** (0.648–0.653) | 0.457 | 0.978 | 0.668 |
| **aggregate** (geomean of the 5) | | | **0.383** (0.383–0.384), 62% less | | | 0.398 (0.397–0.398), 60% less |
| pooled, all 71 cases (reference) | | | 0.455 | | | 0.466 |

Set 2 and set 3 side by side (protocol geomeans; only the marshall half of each protocol differs):

| protocol | comparable ns/op, set 2 | comparable ns/op, set 3 | comparable B/op, set 2 | comparable B/op, set 3 | as measured ns/op, set 2 | as measured ns/op, set 3 | as measured B/op, set 2 | as measured B/op, set 3 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| awsJson1_0 | 0.495 | 0.466 | 0.804 | 0.639 | 0.523 | 0.493 | 0.840 | 0.667 |
| awsQuery | 0.257 | 0.236 | 0.206 | 0.156 | 0.293 | 0.275 | 0.207 | 0.157 |
| restJson1 | 0.376 | 0.280 | 0.411 | 0.293 | 0.394 | 0.294 | 0.438 | 0.311 |
| restXml | 0.342 | 0.251 | 0.622 | 0.436 | 0.364 | 0.267 | 0.651 | 0.457 |
| rpcv2Cbor | 0.460 | 0.428 | 0.834 | 0.650 | 0.481 | 0.447 | 0.858 | 0.668 |
| **aggregate** | 0.376 | 0.319 | 0.513 | 0.383 | 0.403 | 0.343 | 0.532 | 0.398 |

Set 1 is the full A/B as run, without FU-12 or the marshall fix. Its aggregate is 0.379 ns/op (2.6× faster) and 0.519
B/op (48% less) comparable, and 0.405 (2.5× faster) and 0.539 (46% less) as measured. Only awsJson1_0 changes between
sets 1 and 2: in set 1 it is 0.512 ns/op and 0.858 B/op comparable.

What the numbers say:

- Every protocol is faster in both views. awsQuery gains the most (0.236). Its legacy unmarshaller builds a DOM, and
  the feature build streams (section 4).
- All 71 cases are faster than their noise floor in the comparable view. As measured, 70 are faster and
  `awsJson1_0_HealthcheckResponse_Example` is within its floor (1.020). In set 2, `awsQuery_PutMetricDataRequest_Baseline`
  was slower (1.132). In set 3 it is 0.890 (section 4.3).
- Allocation goes down for unmarshall in every protocol. The comparable marshall class geomean is below 1 on all five
  protocols, 0.137–0.522 (section 4.2). restJson1 marshall is 0.137, partly because the legacy restJson1 marshaller
  creates a JSON generator on every call and, on `PutObject_*`, never closes it, so Jackson's buffers are not
  recycled: 17,369–17,411 B/op of the baseline's 19,112–19,139 B/op in the full A/B (`20261023-031253`). A legacy
  restJson1 client pays the same per request. The feature creates no JSON serializer for these blob-payload
  operations.
- 1 of the 43 marshall cases allocates more than the baseline in the comparable view, against 14 in set 2:
  `awsQuery_PutMetricDataRequest_Baseline` (1.183; section 6.4, FU-19). The 7 unmarshall cases that allocate more in
  set 2 are unchanged (section 4.1).
- The marshall fix alone (feature-marshall / feature-fu12 in the same run) brings marshall B/op to 0.580 and ns/op to
  0.784 of the pre-fix feature build (geomeans over the 43 cases). No case is slower or allocates more beyond its
  floor (section 6.3).
- Two of the fixes (F5 and F6) change code that the legacy path also runs: `SdkHttpUtils.appendUri` and
  `DefaultSdkHttpFullRequest.standardizePath`. The baseline here is stock V2 without them. With their sites also
  removed from the baseline, the set 3 comparable aggregate would be 0.3224 ns/op and 0.3985 B/op instead of 0.3192
  and 0.3834 (section 6.1).
- Spread: the per-repetition ranges are within 0.25% of the set 3 aggregates and within 1.6% of each protocol geomean
  (the widest is restJson1 ns/op, −1.5% / +1.1%). The null experiment, run through the same aggregation, gives 0.990
  for ns/op and 0.999 for B/op. So the aggregate is resolved to about 1%. The per-case floors are in section 2.5.

## 2. What was compared and how

### 2.1 Arms

| arm | commit | SDK code | benchmark source | jar sha256 (prefix) | expected code path |
|---|---|---|---|---|---|
| baseline | `fc8b74e9fe21` (merge base of this branch) | stock V2 | `2549a5b6828` with a resource overlay (below) | `129cfb3caa1a` | legacy `core/protocols/*` marshallers and unmarshallers |
| feature | `2549a5b6828` | pinned to `4acbd0b03ae`; `4acbd0b03ae..2549a5b6828` changes only `test/sdk-standard-benchmarks/`, `scripts/`, `docs/`, `.kiro/` (6 files, checked at build time) | `2549a5b6828` | `ef5b2f2b7154` | schema path: `ClientProtocol`, ported codecs, http-binding |
| feature-fu12 (FU-12 run and marshall A/B) | `b17e79d6e1f` | adds the FU-12 blob member fix | `b17e79d6e1f` (adds only the `BlobMemberZeroCopyTest` test) | `413b506b92cf` | schema path |
| feature-marshall (marshall A/B only) | `a8557684f83` | pinned to `3f460f18acb`, which adds the marshall allocation fix; `3f460f18acb..a8557684f83` changes only 2 files, in `docs/` and `.kiro/` (checked at build time) | `a8557684f83` (differs from `b17e79d6e1f` in `test/sdk-standard-benchmarks/` only in the README) | `079653dfb8e4` | schema path |

The SDK pin `4acbd0b03ae` contains both copy fixes made during this measurement: `1f5f257a7f2` (blob
`@httpPayload`) and `4acbd0b03ae` (response body). It does not contain the FU-12 blob member fix (`cf3dff2dff5`,
`cca1bca22d1`, `976f12c8e66`). That fix is measured in a separate run (section 3.1). Section 5 describes all three
fixes. The marshall allocation fix is measured in run `20261023-212558`, with the baseline, feature-fu12 and
feature-marshall jars (sections 3.1 and 6).

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
  definite-length CBOR maps, the Query form body, and response metadata attached on the feature arm. The marshall
  fix workflow ran the same test on the fix: 142 of 142, with no change to the allowed differences.
- **Runtime code path on the metal host.** The verify step of each run passes: 67 of 67 checks in the full A/B, 48
  of 48 in the FU-12 run and 57 of 57 in the marshall A/B. These cover a JMH smoke run, the code path and class
  loading per class and arm, the resource and model checks, the wire dump and pin affinity. The
  profiling pass checks every case (`profile-path.tsv`): 142 of 142 pass. Baseline profiles have 0 schema-path
  samples. Feature profiles have 0 legacy-path samples and 0 `SdkFieldsFromSchema` frames. The marshall profiling
  pass passes 129 of 129 (43 cases × 3 arms), with 0 other-path samples and 0 `SdkFieldsFromSchema` samples.

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
ns/op and the allocation share for B/op. For sets 1 and 2, the shares come from the profiling pass `20261023-095831`
of the pre-FU-12 feature jar `2549a5b6828`. Set 2 applies the same shares to its three substituted rows. For the 43
marshall rows of set 3, the shares come from the marshall profiling pass `20261024-033608` of the baseline and
feature-marshall jars. There, the baseline's A1-objects share is 0.0–3.9% of CPU and 0.0–6.7% of allocation, within
0.7 points of the earlier pass on every case. Taking the baseline's shares from the earlier pass instead leaves the
set 3 aggregates at 0.3192 ns/op and 0.3834 B/op. The feature-marshall marshall profiles have no A1 or A2 samples,
so only A5 (Query) is removed from that arm. Section 7 lists the estimate's caveats.

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
restJson1 marshall allocates far less on the feature (comparable class geomean 0.271 in set 2, 0.137 in set 3). The
four files cited here are
the same in the baseline commit `fc8b74e9fe21`.

**A2 is narrower than before.** The previous rule took every sample of the profile category `metadata`. That category
also holds the awsQuery request-id read, which both timed methods do. The legacy
`QueryProtocolUnmarshaller.unmarshall` returns `parseMetadata(document)` with its result (line 88). The feature's
`QueryClientProtocol.deserializeOutput` calls `readRequestId` (line 230), which parses the body a second time (lines
247–257; FU-15 (b)). The request-id pass is 36.5–37.2% of feature CPU on the 3 awsQuery unmarshall cases. The
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
writing its string values into the body. In the feature-marshall profiles (set 3), A5 is 8.3–39.6% of CPU, the
escaping 0.06–1.41% and the copy 96.4–99.8% of A5. A5's share comes from one single-fork profile run per case and
arm, and it can move between runs: on `awsQuery_PutMetricDataRequest_M` it is 30.5% (feature, first pass), 8.7%
(feature-fu12) and 31.1% (feature-marshall).

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
choice matters only on the smallest requests. In set 2, `awsQuery_PutMetricDataRequest_Baseline` would be 1.191
instead of 1.132, and `PutMetricDataRequest_S` 0.394 instead of 0.383. The awsQuery marshall class geomean would be
0.446 instead of 0.439, the awsQuery protocol 0.259 instead of 0.257, and the aggregate 0.3766 instead of 0.3760
ns/op (0.377 instead of 0.376 at three decimals). In set 3, the write is 0.14–5.37% of feature-marshall CPU.
`awsQuery_PutMetricDataRequest_Baseline` would be 0.943 instead of 0.890, the awsQuery marshall class geomean 0.3780
instead of 0.3690, and the aggregate 0.3200 instead of 0.3192 ns/op. B/op does not change, because the write
allocates nothing, and in both sets the verdict counts stay the same. This report keeps the rule as it is.

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
removed" row is kept only for this reconciliation. For set 3 it is 0.341 ns/op and 0.518 B/op.

### 2.5 Host, settings, null experiment

**Host.** `i-018869b3464a7fae5`, m6g.metal, us-east-1c: Graviton2 (Neoverse-N1), 64 cores, no SMT, 1 NUMA node,
251 GiB, no cpufreq driver (fixed frequency). Amazon Linux 2023, kernel 6.18.54. Corretto 25.0.4.1+11-LTS.
async-profiler 4.4.

**JMH.** JMH 1.37, SampleTime, `-f 2 -wi 5 -w 2s -i 5 -r 2s -prof gc -foe true`. These options override the class
annotations. JVM options: `-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4`.

**Runs.** Each JMH invocation runs one class on one arm. Each run has 4 repetitions. Within a repetition, the arm
order alternates. Every invocation is pinned with `taskset` to cores 32–47; the verify step checks the pin. The full
A/B ran 80 of 80 invocations ok, and the FU-12 run 48 of 48. The marshall A/B ran the 5 marshall classes (43 cases)
on 3 arms with the full A/B's JMH options, JVM options, repetitions, arm order and pin: 60 of 60 invocations ok, 20
per arm.

**Statistics** (`serde_ab_summary.py` docstring):

- Time is the JMH SampleTime mean (ns/op). Memory is `gc.alloc.rate.norm` (B/op).
- The ± values are the 99.9% Student-t half-width over the 4 repetitions.
- A case's ratio is the mean of the per-repetition paired ratios feature / baseline.
- A geomean is the geometric mean of case ratios.

**Null experiment** (`20261023-011610`). The feature jar ran against itself on 20 cases: the smallest and largest
case of each class, 4 repetitions, the same settings. The null jar is `ce95211425c` (before both copy fixes). The
two copy fixes change no benchmark code, and the floors measure run-to-run noise under the same options, so this
report applies them to the post-fix, FU-12 and feature-marshall jars. That is an assumption for the cases the fixes
changed (section 7).

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

**Uncertainty of the geomeans** (`.agents/tasks/serde-report-comparable/analysis/report_numbers_comparable.py`; for
set 3, `.agents/tasks/serde-report-marshall/analysis/report_numbers_marshall.py`):

- **Per-rep range.** The statistic computed from each repetition's paired ratios alone, min to max.
- **Bootstrap.** An exact bootstrap over the repetition indices (all 256 resamples). Its intervals agree with the
  per-rep ranges within 0.001, in sets 2 and 3.
- **Null aggregate.** The same aggregation on the null experiment gives 0.990 for ns/op and 0.999 for B/op. The
  per-protocol null geomeans are 0.954–1.007 for ns/op; restJson1's 0.954 comes from its 18.1% row. For B/op they
  are 0.995–1.002.

## 3. Results

### 3.1 Result sets, the FU-12 substitution and the marshall A/B

- **Set 1 (as run).** The full A/B `20261023-031253`: all 71 cases, baseline vs feature `2549a5b6828`.
- **Set 2 (with FU-12).** Set 1 with three rows replaced: `JsonRpc10MarshallBenchmark`
  `awsJson1_0_PutItemRequest_BinaryData_S`, `_M` and `_L`. In set 2 these rows use feature-fu12 / baseline from run
  `20261023-111214`. That run used the same host, settings and baseline jar, and measured the baseline again. The
  comparable factors for the replaced rows use the same profiling-pass shares as in set 1. The feature arm has no
  boundary item in marshall, so only the baseline's A1-objects share matters: the factors are 1.0002–1.0116 for ns/op
  and 1.0003–1.0642 for B/op.
- **Set 3 (with the marshall fix; the headline).** Set 2 with all 43 marshall rows replaced by feature-marshall /
  baseline from the marshall A/B `20261023-212558`. That run has 3 arms (baseline, feature-fu12, feature-marshall), the
  5 marshall classes, 4 repetitions and the full A/B's settings, and it measured the same baseline jar again. The
  comparable factors of these rows come from the marshall profiling pass `20261024-033608` (section 2.4). The 28
  unmarshall rows keep their set 2 values, because the fix does not change the response path (section 6.2). All
  marshall rows of set 3, including the rpcv2Cbor `BinaryData_*` rows, therefore include FU-12.

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
is 2,668 B/op in set 1 and 2,704 in the FU-12 run, a 36 B step. The substitution is therefore consistent. Set 2 was
the headline until the marshall fix was measured.

The substituted rows:

| case | baseline ns/op | feature-fu12 ns/op | ratio | comparable | baseline B/op | feature-fu12 B/op | B/op ratio | comparable |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `awsJson1_0_PutItemRequest_BinaryData_S` | 2,000.6 ± 178.8 | 1,091.3 ± 44.2 | 0.546 | 0.552 | 2,704 ± 0 | 2,781 ± 116 | 1.028 | 1.094 |
| `awsJson1_0_PutItemRequest_BinaryData_M` | 22,315.2 ± 2,565.2 | 4,664.6 ± 223.2 | 0.209 | 0.209 | 26,730 ± 0 | 14,448 ± 0 | 0.541 | 0.544 |
| `awsJson1_0_PutItemRequest_BinaryData_L` | 174,288.7 ± 1,333.5 | 32,849.6 ± 1,122.4 | 0.188 | 0.189 | 251,333 ± 123 | 112,034 ± 0 | 0.446 | 0.446 |

In set 1 (feature without FU-12), the same rows are 0.591, 0.321 and 0.291 ns/op and 1.146, 1.214 and 1.078 B/op.

**Controls in the marshall A/B.** On 40 of the 43 marshall rows, the feature-fu12 arm of run `20261023-212558` runs
the same jar as set 2. The exceptions are the rpcv2Cbor `BinaryData_*` rows, where set 2 keeps the pre-FU-12 jar. On
those 40 rows, its ratio against the baseline should reproduce set 2. For ns/op, 33 of 40 are within their floor, and
the other 7 differ by 0.4–5.0%. For B/op, 27 of 40 are within their floor. Of the 13 others, 9 have the same
feature-arm B/op (to the byte) in both runs, so the difference is on the baseline side, as in the FU-12 run. The 4
with a different feature-arm B/op (by 0.02–1.21%) differ by at most 1.2% in ratio. The table has the 7 ns/op rows,
the 3 largest baseline-side B/op differences and the 4 rows whose feature-arm B/op differs:

| case | metric | floor | set 2 ratio | run `20261023-212558` feature-fu12 ratio | difference | baseline mean, set 2 / this run | feature mean, set 2 / this run |
|---|---|---:|---:|---:|---:|---|---|
| `awsJson1_0_PutItemRequest_ShallowMap_S` | ns/op | 2.7%* | 0.318 | 0.334 | +5.0% | 4,776.2 / 4,783.0 | 1,518.4 / 1,596.7 |
| `awsQuery_PutMetricDataRequest_Baseline` | ns/op | 2.5% | 1.186 | 1.234 | +4.0% | 462.0 / 446.7 | 547.2 / 551.2 |
| `awsQuery_PutMetricDataRequest_L` | ns/op | 0.3% | 0.562 | 0.560 | −0.4% | 163,030.7 / 163,733.9 | 91,679.3 / 91,735.1 |
| `restJson1_CopyObjectRequest_Baseline` | ns/op | 1.2% | 0.293 | 0.298 | +1.6% | 3,005.7 / 2,933.6 | 881.5 / 873.7 |
| `restJson1_PutObject_S` | ns/op | 1.2%* | 0.210 | 0.213 | +1.3% | 3,931.3 / 3,904.7 | 826.4 / 831.7 |
| `restJson1_PutObject_M` | ns/op | 1.2%* | 0.211 | 0.214 | +1.5% | 3,969.5 / 3,927.8 | 836.7 / 840.3 |
| `restXml_CopyObjectRequest_Baseline` | ns/op | 1.1% | 0.312 | 0.301 | −3.2% | 2,776.6 / 2,832.4 | 864.5 / 853.8 |
| `awsJson1_0_PutItemRequest_Nested_M` | B/op | 1.3%* | 0.790 | 0.805 | +1.9% | 3,202 / 3,142 | 2,528 / 2,528 |
| `awsQuery_PutMetricDataRequest_S` | B/op | 0.0%* | 0.651 | 0.662 | +1.7% | 4,228 / 4,156 | 2,752 / 2,752 |
| `rpcv2Cbor_PutItemRequest_ShallowMap_M` | B/op | 2.8%* | 0.849 | 0.887 | +4.5% | 5,266 / 5,009 | 4,445 / 4,445 |
| `restJson1_CopyObjectRequest_M` | B/op | 0.8%* | 1.140 | 1.154 | +1.2% | 8,641 / 8,641 | 9,854 / 9,973 |
| `awsQuery_PutMetricDataRequest_M` | B/op | 0.0%* | 0.673 | 0.671 | −0.2% | 19,653 / 19,653 | 13,225 / 13,195 |
| `awsQuery_GetMetricDataRequest_M` | B/op | 0.0%* | 0.627 | 0.628 | +0.0% | 26,232 / 26,241 | 16,457 / 16,469 |
| `awsQuery_GetMetricDataRequest_L` | B/op | 0.0%* | 0.695 | 0.695 | +0.0% | 53,249 / 53,240 | 37,002 / 37,009 |

Across the 43 rows, the baseline means of run `20261023-212558` and of the full A/B `20261023-031253` differ by −3.3% to +2.0% in ns/op (median −0.2%) and by −4.9%
to +1.0% in B/op (median 0.0%). At the aggregate level, set 2 with its marshall rows replaced by this run's
feature-fu12 / baseline gives 0.3779 ns/op and 0.5133 B/op comparable, against 0.3760 and 0.5127 for set 2 (+0.51%
and +0.12%). As measured, the differences are +0.18% and +0.11%. With the set 2 comparable factors (the shares of
`20261023-095831`) instead of this pass's, the comparable differences are +0.18% and +0.11% too, so the rest of the
+0.51% comes from the shares of the two profiling passes. These are small next to the fix's effect, and each set 3
ratio is paired with the baseline of its own run.

### 3.2 Per class (geomeans)

| class | cases | set 3 comparable ns/op | set 3 comparable B/op | set 3 as measured ns/op | set 3 as measured B/op | set 2 comparable ns/op | set 2 comparable B/op | set 2 as measured ns/op | set 2 as measured B/op |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| JsonRpc10MarshallBenchmark | 14 | 0.310 | 0.513 | 0.307 | 0.492 | 0.348 | 0.812 | 0.346 | 0.781 |
| JsonRpc10UnmarshallBenchmark | 8 | 0.703 | 0.797 | 0.792 | 0.904 | 0.703 | 0.797 | 0.792 | 0.904 |
| QueryMarshallBenchmark | 7 | 0.369 | 0.449 | 0.500 | 0.444 | 0.439 | 0.782 | 0.569 | 0.773 |
| QueryUnmarshallBenchmark | 3 | 0.151 | 0.054 | 0.151 | 0.055 | 0.151 | 0.054 | 0.151 | 0.055 |
| RestJsonMarshallBenchmark | 5 | 0.171 | 0.137 | 0.170 | 0.135 | 0.308 | 0.271 | 0.305 | 0.267 |
| RestJsonUnmarshallBenchmark | 5 | 0.458 | 0.624 | 0.509 | 0.717 | 0.458 | 0.624 | 0.509 | 0.717 |
| RestXmlMarshallBenchmark | 5 | 0.207 | 0.522 | 0.206 | 0.498 | 0.384 | 1.063 | 0.382 | 1.013 |
| RestXmlUnmarshallBenchmark | 5 | 0.304 | 0.364 | 0.347 | 0.419 | 0.304 | 0.364 | 0.347 | 0.419 |
| RpcV2CborMarshallBenchmark | 12 | 0.305 | 0.473 | 0.303 | 0.457 | 0.352 | 0.778 | 0.350 | 0.753 |
| RpcV2CborUnmarshallBenchmark | 7 | 0.601 | 0.894 | 0.660 | 0.978 | 0.601 | 0.894 | 0.660 | 0.978 |

Set 1 differs from set 2 only in the JsonRpc10 marshall class: 0.372 ns/op and 0.924 B/op comparable, 0.370
ns/op and 0.888 B/op as measured.

Verdicts per case:

| view | metric | set 3 faster / less | set 3 within floor | set 3 slower / more | set 2 faster / less | set 2 within floor | set 2 slower / more |
|---|---|---:|---:|---:|---:|---:|---:|
| comparable | ns/op | 71 | 0 | 0 | 69 | 1 | 1 |
| comparable | B/op | 63 | 0 | 8 | 48 | 2 | 21 |
| as measured | ns/op | 70 | 1 | 0 | 68 | 2 | 1 |
| as measured | B/op | 63 | 0 | 8 | 51 | 3 | 17 |

In set 3, the 8 cases with more B/op are 7 unmarshall cases (section 4.1) and
`awsQuery_PutMetricDataRequest_Baseline` (section 6.4). The case within its floor as measured is
`awsJson1_0_HealthcheckResponse_Example` (1.020).

The per-case tables are in the appendix (set 3) and in appendix B (the marshall fix alone).

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

The marshall classes after the fix come from the marshall profiling pass `20261024-033608`: the baseline,
feature-fu12 and feature-marshall jars on the 43 marshall cases, with the same profiler options. All 258 profile runs
were ok. The allocation totals agree with `gc.alloc.rate.norm` within 0.98–1.04 for 123 of the 129 profiles; the six
`BinaryData_L` profiles (awsJson1_0 and rpcv2Cbor, all three arms) are at 0.92–0.94. Its `profile-comparison.md`
raises 1,215 flags. They are differences above 2× between arms (779) and categories that only one path has, for
example `construction` and `pojo` on the baseline and `protocol` on the feature arms; the first pass raised the same
kinds. `alloc_top_marshall.py` in `.agents/tasks/serde-report-marshall/analysis/` gives the allocation sites cited
for the fixed build.

### 4.1 Per protocol

The categories with at least 5% of CPU (at most four per cell), from the first pass (`20261023-095831`; the feature
column is `2549a5b6828`, the jar of sets 1 and 2):

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

The marshall classes before and after the marshall fix, from the marshall pass (`20261024-033608`):

| class | feature-fu12: top CPU categories (mean ns/op) | feature-marshall: top CPU categories (mean ns/op) |
|---|---|---|
| JsonRpc10 marshall | codec_write 42% (3.9 µs), http 21% (0.4 µs), traversal 19% (1.6 µs), format 7% (1.4 µs) | codec_write 42% (3.9 µs), http 21% (0.3 µs), traversal 21% (1.5 µs), format 8% (1.4 µs) |
| Query marshall | codec_write 40% (8.7 µs), format 20% (5.5 µs), http 18% (0.6 µs), bytes 11% (2.7 µs) | codec_write 50% (9.2 µs), http 18% (0.4 µs), format 16% (4.9 µs), traversal 8% (1.8 µs) |
| RestJson marshall | http 66% (1.5 µs), pojo 17% (1.1 µs), bytes 7% (0.1 µs) | http 83% (1.2 µs), traversal 6% (0.0 µs), bytes 5% (0.0 µs) |
| RestXml marshall | http 66% (1.5 µs), pojo 17% (1.1 µs), bytes 9% (0.1 µs) | http 82% (1.3 µs), traversal 7% (0.0 µs), bytes 6% (0.0 µs) |
| RpcV2Cbor marshall | codec_write 44% (3.7 µs), traversal 20% (1.7 µs), http 20% (0.5 µs), bytes 10% (0.8 µs) | codec_write 44% (3.6 µs), traversal 23% (1.7 µs), http 20% (0.4 µs), bytes 6% (0.5 µs) |

On JsonRpc10 and RpcV2Cbor, the top categories barely change: there the fix removed allocation more than CPU. On
Query, `bytes` (the growth of the form array) drops out of the top four. On REST marshall, the http category falls
from 1.5 to 1.2–1.3 µs per call while its share rises to 82–83%, because the rest of the call got cheaper too
(`pojo` drops below 5%).

Where the feature build wins:

- **Member dispatch.** The legacy marshallers walk the `SdkField` registry. They dispatch through `MarshallingType`
  and read every value through `SdkField` getters, which shows as traversal plus pojo. Generated `serializeMembers`
  reads the fields directly. Marshall traversal plus pojo drops from 14.0–18.8 µs to 1.6–1.9 µs per call on the
  JsonRpc10, RpcV2Cbor and Query class means. That drop accounts for most of the comparable marshall ratios of
  those three classes: 0.35–0.44 in set 2 and 0.305–0.369 in set 3.
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

- **Header and request building on REST marshall (sets 1 and 2).** Before the marshall fix, the feature's http
  category was 66% of restJson1 and restXml marshall CPU. `HttpBindingSerializer.putHeader` built a
  case-insensitive `TreeMap`, and `CollectionUtils.deepCopyMap` copied it into the request builder's own `TreeMap`.
  So every bound header was inserted twice with case-insensitive compares. The legacy marshaller appends to the
  builder's map once. F3 now writes each header straight into the request builder, once (section 6.1). On
  `CopyObjectRequest_M`, the header and query writes fall from 8,900 to 3,842 ns/op (restJson1) and from 8,916 to
  4,064 ns/op (restXml) in the profiles. They are still 78–79% of that case's feature CPU: the builder's
  case-insensitive map insert remains, and it is the same insert the legacy marshaller makes.
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

Per protocol, the marshall class geomeans of B/op and the cases that allocate more than their floor allows, in set 3
and, for reference, set 2:

| protocol | as measured marshall B/op | comparable marshall B/op | cases with more B/op (comparable) | set 2 comparable marshall B/op | set 2 cases with more B/op |
|---|---:|---:|---:|---:|---:|
| awsJson1_0 | 0.492 | 0.513 | 0 of 14 | 0.812 | 4 of 14 |
| awsQuery | 0.444 | 0.449 | 1 of 7 | 0.782 | 1 of 7 |
| restJson1 | 0.135 | 0.137 | 0 of 5 | 0.271 | 1 of 5 |
| restXml | 0.498 | 0.522 | 0 of 5 | 1.063 | 4 of 5 |
| rpcv2Cbor | 0.457 | 0.473 | 0 of 12 | 0.778 | 4 of 12 |

The baseline's A1-objects and A1-request B/op are in section 2.4. By protocol, in the comparable view of set 3:

- **restXml.** All 5 cases allocate less, from 0.336 (`CopyObjectRequest_Baseline`) to 0.615 (`CopyObjectRequest_M`).
  In set 2, 4 of them allocated more: `PutObject_S/M/L` 1.135, 1.139 and 1.134, and `CopyObjectRequest_M` 1.115.
- **restJson1.** All 5 allocate less, from 0.058 (`PutObject_L`) to 0.649 (`CopyObjectRequest_M`). `PutObject_*` are
  low because of the legacy generator buffers (section 2.4). In set 2, `CopyObjectRequest_M` allocated more (1.157).
- **awsJson1_0.** All 14 allocate less, from 0.359 (`Nested_L`) to 0.940 (`ShallowMap_L`). In set 2, 4 allocated
  more.
- **rpcv2Cbor.** All 12 allocate less, from 0.273 (`Nested_L`) to 0.925 (`BinaryData_M`). In set 2, 4 allocated
  more.
- **awsQuery.** `PutMetricDataRequest_Baseline` still allocates more: 1.183, against 2.489 in set 2 (section 6.4). The
  other 6 cases allocate less, from 0.264 (`GetMetricDataRequest_L`) to 0.507 (`PutMetricDataRequest_L`).

That is 1 marshall case. With the 7 unmarshall cases of section 4.1, it makes up the 8 "more" verdicts of set 3 in
section 3.2.

**The remaining feature allocation, per protocol.** These are the profiled sites of the feature-marshall arm in the
marshall pass (`alloc_top_marshall.py`; B/op of the profile runs, inclusive where a frame is named):

- **awsJson1_0 and rpcv2Cbor** (`PutItemRequest_Baseline`: 1,160 and 1,216 B/op). The codec's 1 KiB scratch buffer is
  gone (F1, section 6.1). What remains is mostly the request itself: the header writes into the request builder (334
  and 354 B), the builder and its `LowCopyListMap`s (`SdkHttpFullRequest.builder`, 212 and 205 B), `build()` (176 and
  177 B), the JSON serializer (`JsonCodec.createSerializer`, 191 B) and the body buffer (`ByteBufferOutputStream`, 79
  B on JSON; 72 B plus 120 B of growth on CBOR).
- **restJson1 and restXml** (`PutObject_S`: 1,112 B/op on both). The header map copy, the method lookup, the URI join,
  the path copy and the unused body buffer are gone (F3 to F9). What remains: the header writes into the builder (259
  and 265 B), the builder (211 and 206 B), `build()` (180 and 177 B), the path (`PathSerializer`, 175 and 176 B), the
  per-call `HttpBindingSerializer` (95 B) and the payload view (`ByteBufferContentStreamProvider.create`, 72 and 73
  B). On `restJson1_CopyObjectRequest_M` (5,024 B/op in the profile run), the header writes are 3,496 B (70%) and the
  RFC 822 timestamps (`HttpDateFormat.format`) 619 B.
- **awsQuery** (`PutMetricDataRequest_Baseline`: 1,112 B/op). `QueryFormSerializer.forAwsQuery` is 475 B, of which
  the body array is 250 B. The builder is 211 B, `build()` 180 B and the header writes 177 B (section 6.4).

The builder and `build()` objects are the FU-18 sites: `http-client-spi` code that the legacy path shares. The legacy
marshaller builds the same request objects in A1-request (section 2.4).

### 4.3 Cases slower in ns/op

In set 3, no case is slower than its floor in either view. `awsQuery_PutMetricDataRequest_Baseline`, the only slower
case in set 2 (1.132 comparable, 1.186 as measured, 2.5% floor), is 0.890 comparable and 0.955 as measured in set 3
(446.7 vs 426.6 ns/op). The fix alone brings it to 0.774 of feature-fu12 (section 6.3). In the feature-fu12 profile,
the URI join (F5) was 10.5% of CPU and the path copy (F6) 3.1%. In the feature-marshall profile they are 0.1% and
0.5%, and F2 replaced the 1 KiB form array with one sized from the operation's last request.

Set 2's analysis of the case, for reference:

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

  As measured, the case was 1.19× slower. It was the only case of set 2 slower than its floor in either view.

After the fix, `SdkHttpFullRequest.Builder.uri` is still the top self frame of this case's feature profile: 51.3% of
feature-marshall CPU (53.0% inclusive, of 461 ns per call in the profile run), against 45.0% inclusive of 597 ns
on feature-fu12 and 14.7% inclusive on the baseline. The fix did not touch it, and it is not investigated.

The two header-heavy `CopyObjectRequest_M` cases were slower in the earlier serde-only view. In the comparable view
of set 2, `restJson1_CopyObjectRequest_M` is faster (0.976) and `restXml_CopyObjectRequest_M` is within its floor
(1.001). Their feature cost was the double header insertion of section 4.1: `TreeMap.put` is 72–77% of feature CPU
(inclusive), and 38.6–41% is self time in `StringLatin1.compareToCI`. The baseline spends 19–20% formatting RFC 822
dates instead. In set 3, after F3, they are 0.489 and 0.497 comparable.

## 5. Fixes made during this measurement

The laptop smoke and the first profiles showed three copies in the feature build. Each was fixed in its own
workflow before or alongside the metal runs. The "before" metal numbers come from the null run, whose jar
`ce95211425c` has neither copy fix. The "after" numbers come from the full A/B feature jar `2549a5b6828`. Both runs
used the same host and settings. Between those commits, main code changes only in the two fixes.

| fix | commits | what changed | metal effect |
|---|---|---|---|
| Blob `@httpPayload` copy (A3) | `1f5f257a7f2`, test `7b5117f3d8e` | `HttpBindingSerializer.setBlobPayload` copied the read-only `SdkBytes` view into a new array on every marshall. It now shares the buffer through `ByteBufferContentStreamProvider`, as legacy does. | `restJson1_PutObject_L`: 19,121 → 838 ns/op, 343,425 → 2,128 B/op. `restXml_PutObject_L`: 19,241 → 856 ns/op, 343,489 → 2,192 B/op. The other marshall cases of the null: within 1.7% ns/op and 0.2% B/op. |
| Response body growth copies | `4acbd0b03ae` | Bodies of unknown length started at 1 KiB and doubled. They now start from a per-operation size hint (the last body + 1/8). `Crc32ResponseValidation` reads into an exact array. | `awsJson1_0_GetItemOutputBinary_L` −33% B/op, −6.3% ns/op. `awsQuery_GetMetricDataResponse_L` −29% B/op, −2.5% ns/op. `rpcv2Cbor_GetItemOutputBinary_L` −8.7% B/op. The four small `Baseline`/`Example` outputs −48% to −59% B/op: `restJson1_CopyObjectOutput_Baseline` 2,027 → 1,048, `restXml_CopyObjectOutput_Baseline` 1,799 → 788, `awsJson1_0_HealthcheckResponse_Example` 1,688 → 696, `rpcv2Cbor_GetItemOutput_Baseline` 1,688 → 689. `awsQuery_GetMetricDataResponse_S` −22% B/op. `GetObject_L` unchanged (streaming payload). |
| FU-12 blob member copy | `cf3dff2dff5`, `cca1bca22d1`, `976f12c8e66`, docs `b17e79d6e1f` | Generated code passed blob members as `SdkBytes.asByteBuffer()`, a read-only view that the JDK base64 encoder copies. It now passes `asByteArrayUnsafe()` through `writeBlob(Schema, byte[])`. | FU-12 run, same jars and run: awsJson1_0 `BinaryData_S/M/L` feature-fu12 vs feature 0.921 / 0.650 / 0.649 ns/op and 0.909 / 0.447 / 0.414 B/op. Against the baseline: 1.146 → 1.028, 1.214 → 0.541, 1.078 → 0.446 B/op. `PutObject_L` on both REST protocols (0.997 / 1.001 ns/op, 1.000 B/op) is unchanged within floor. rpcv2Cbor `BinaryData_S` is 8.6% slower in ns/op (1.086, floor 3.8%*, slower in every repetition); `_M` and `_L` are 0.987 and 0.974; B/op is within floor on all three (0.994–1.008). Set 2 keeps the pre-FU-12 rpcv2Cbor rows (section 3.1, FU-16). Set 3 has FU-12 on every marshall row. |

The response-body fix's effect is a best case on the JsonRpc10, RpcV2Cbor and Query unmarshall cases and on the
restJson1 and restXml `CopyObjectOutput_*` cases (section 7).

## 6. Marshall allocation fix

In set 2, 14 of the 43 marshall cases allocated more than the baseline in the comparable view (section 4.2). The
marshall allocation fix (`fix/marshall-allocation`, merged into this branch at `a8557684f83`) went after the sites
behind them. Its target was a feature marshall B/op no higher than the Baseline_Build's, comparable (the baseline
minus only A1-objects), on every case. Its report is `.agents/tasks/fix-marshall-allocation/report.md` (not
committed).

The fix was measured in two runs on the same host, with the settings of section 2.5. The marshall A/B
`20261023-212558` has 3 arms: the same baseline jar, feature-fu12 (`b17e79d6e1f`, the fix's base) and
feature-marshall (`a8557684f83`, SDK pin `3f460f18acb`, the fix's last SDK code commit). The marshall profiling pass
`20261024-033608` profiled the same 3 arms.

### 6.1 The fixes and where their effect shows

| fix | commits | what it removed |
|---|---|---|
| F1 | `2d27360411a` (F1(a)), `0ec04cd8914` (F1(b)); iteration 2: `94eab8f12c1`, `bb6a15660f0`, `3f460f18acb` | The 1 KiB scratch buffer that `SmithyJsonSerializer` and `CborSerializer` allocated on every call and drained into the body buffer. With a `ByteBufferOutputStream` sink, which every SDK request path passes, the serializers now write into the stream's own array (F1(b); new public methods `ByteBufferOutputStream.reserve` and `advance`). Other sinks get a 1 KiB array lent by a pool (F1(a)). Iteration 2 fixed caller writes between flushes and kept the steady state from growing the array, and `3f460f18acb` reads `useJsonName` from the serializer's settings instead of a cached field, which keeps `SmithyJsonSerializer` at 64 bytes. |
| F2 | `a1f1f74b6a7` | The fixed 1 KiB awsQuery and ec2Query form array and its growth. The array now starts at the capacity the operation's last request needed. |
| F3 | `808b8656f4a` | The per-call header and query maps of `HttpBindingSerializer` and their deep copy into the request builder. Headers and query parameters are now written into the builder directly. |
| F4 | `e9cf178433b` | The per-request `SdkHttpMethod.fromValue` lookup. The method is resolved once per operation binding. |
| F5 | `6dc479211ee` | The new string that `SdkHttpUtils.appendUri` built when the join adds nothing (`utils`, shared with the legacy path). |
| F6 | `07d46c74a27` | The copy that `DefaultSdkHttpFullRequest.standardizePath` made of a path that already starts with `/` (`http-client-spi`, shared with the legacy path). |
| F7, F9 | `d4a19a09029` | The request body buffer and content provider of HTTP binding operations that write no codec body. |

F8 (the per-request RPC strings, FU-20) and the request builder's own objects (FU-18) were not changed.

The A/B measures all the fixes together. To see each fix, `fix_sites.py` assigns every sample of the marshall pass's
profiles to a fix by the fix plan's frame rules, on both feature arms. Two rules are widened so that the before and
after are comparable: F3 counts every sample of the header and query writes, and F2 counts every `byte[]` the form
serializer allocates. The values below are class means, B/op from the alloc profiles and ns/op from the cpu
profiles, before (feature-fu12) → after (feature-marshall). A dash means the rule matches nothing in that class.

| fix | JsonRpc10 | Query | RestJson | RestXml | RpcV2Cbor |
|---|---:|---:|---:|---:|---:|
| F1 | 1,042 → 0 | - | - | - | 1,044 → 0 |
| F2 | - | 31,891 → 15,453 | - | - | - |
| F3 | - | - | 2,014 → 905 | 2,022 → 1,008 | - |
| F4 | - | - | 266 → 0 | 263 → 0 | - |
| F5 | 181 → 0 | 177 → 0 | 243 → 0 | 310 → 0 | 385 → 0 |
| F6 | 80 → 0 | 77 → 0 | 154 → 0 | 155 → 0 | 191 → 0 |
| F7 | - | - | 25 → 0 | 25 → 0 | - |
| F9 | - | - | 70 → 0 | 71 → 0 | - |

| fix | JsonRpc10 | Query | RestJson | RestXml | RpcV2Cbor |
|---|---:|---:|---:|---:|---:|
| F3 | - | - | 1,882 → 806 | 1,883 → 852 | - |
| F4 | - | - | 60 → 0 | 61 → 0 | - |
| F5 | 43 → 1 | 73 → 0 | 76 → 1 | 86 → 1 | 75 → 16 |
| F6 | 18 → 5 | 21 → 2 | 28 → 2 | 30 → 2 | 33 → 2 |
| F7 | - | - | 10 → 0 | 11 → 0 | - |
| F9 | - | - | 4 → 0 | 3 → 0 | - |

F1 and F2 match allocated arrays only, so they have no CPU row. The A/B drop per class, and the part of it in these
sites:

| class | measured drop (A/B, feature-fu12 − feature-marshall) | in the F1–F9 sites (profile) | elsewhere |
|---|---:|---:|---:|
| JsonRpc10 | 1,648 | 1,302 | 346 |
| Query | 16,737 | 16,692 | 45 |
| RestJson | 1,777 | 1,867 | -90 |
| RestXml | 1,830 | 1,837 | -7 |
| RpcV2Cbor | 1,885 | 1,620 | 265 |

On the REST classes and Query, the sites hold the whole drop, within 90 B/op. On JsonRpc10 and RpcV2Cbor, 346 and
265 B/op of the drop is elsewhere, mostly on the `BinaryData_*` cases. On `awsJson1_0_PutItemRequest_BinaryData_L`,
the A/B drop is 6,240 B/op and the sites hold 1,485 B/op. There, F1(b) changed how the body buffer fills. The class
means of the codec_write category fall from 1,761 to 724 B/op (JsonRpc10) and from 1,133 to 93 B/op (RpcV2Cbor).

For time, F3 is most of the REST change. On `CopyObjectRequest_M`, the header and query writes fall from 8,900 to
3,842 ns/op (restJson1) and from 8,916 to 4,064 ns/op (restXml) in the profiles. The A/B change of those cases is
9,967.1 → 4,958.5 and 9,925.1 → 4,940.2 ns/op.

**F5 and F6 are shared code.** The legacy path also runs `SdkHttpUtils.appendUri` and `standardizePath`, and the
baseline jar (`fc8b74e9fe21`, stock V2) does not have the fixes. In the baseline's profiles, the F5 sites are 108–247
B/op and up to 6.9% of CPU, and the F6 sites are up to 155 B/op and 2.3% of CPU. Together they are 163–398 B/op and
0.1–9.2% of CPU per case. A legacy client built from the fix commits would save that. As an estimate, with those
shares also removed from the baseline, the set 3 comparable aggregate would be 0.3224 ns/op instead of 0.3192 and
0.3985 B/op instead of 0.3834. restXml marshall B/op would move most, from 0.522 to 0.621. The report keeps the stock
V2 baseline.

### 6.2 The response path is unchanged

The unmarshall rows of set 3 were not measured again, so this needs the code to show it. Between `b17e79d6e1f` and
`a8557684f83`, main code changes in 15 files, from the 11 fix commits only (`fix-diff.txt`). None of the 15 is a
deserializer, parser, unmarshaller or response reader, and no codegen or generated service code changes. Where a
changed class is also on the response path, the change is on its request side:

- `HttpClientProtocol` adds `recordRequestBodySize` and `lastRequestBodyWasEmpty` for the request size hints.
  `readResponseBody` is unchanged.
- `MarshallBufferSizeTracker` also holds the per-operation response size hint. `record` of a size of 0 now stores
  an empty marker, and `initialCapacity` returns the minimum capacity (32) for it. Before, a size of 0 stored
  `hintFor(0)`, which is the same 32. So a response buffer starts at the same capacity as before, also after an
  empty response.
- `QueryClientProtocol` and `HttpBindingClientProtocol` change only in `createRequest`.
- `ByteBufferOutputStream` adds `reserve` and `advance`. Its other methods are unchanged.
- `HttpBindingSerializer`, `HttpBindingSchemaExtensions` and `RequestSerializer` serialize requests. The response
  side (`ResponseDeserializer`) is not changed.
- `SdkHttpUtils.appendUri` and `DefaultSdkHttpFullRequest.standardizePath` build request URIs.

In `test/sdk-standard-benchmarks/`, only the README differs. The fix report also states that no work moved out of
the timed method and that wire bytes, headers, query strings, paths and `Content-Length` are unchanged.

### 6.3 The fix alone

feature-marshall / feature-fu12 inside run `20261023-212558`, with the null floors. Appendix B lists every case.

| class | cases | ns/op, fix / before (geomean) | B/op, fix / before (geomean) | mean B/op before → after | ns/op faster / within floor / slower | B/op less / within floor / more |
|---|---:|---:|---:|---:|---|---|
| JsonRpc10MarshallBenchmark | 14 | 0.881 | 0.629 | 12,539 → 10,891 | 12 / 2 / 0 | 14 / 0 / 0 |
| QueryMarshallBenchmark | 7 | 0.872 | 0.572 | 36,992 → 20,255 | 7 / 0 / 0 | 7 / 0 / 0 |
| RestJsonMarshallBenchmark | 5 | 0.550 | 0.505 | 3,747 → 1,970 | 5 / 0 / 0 | 5 / 0 / 0 |
| RestXmlMarshallBenchmark | 5 | 0.547 | 0.493 | 3,787 → 1,957 | 5 / 0 / 0 | 5 / 0 / 0 |
| RpcV2CborMarshallBenchmark | 12 | 0.866 | 0.604 | 11,098 → 9,214 | 8 / 4 / 0 | 12 / 0 / 0 |
| all 43 | 43 | 0.784 | 0.580 | | | |

Over the 43 cases, the geomeans are 0.784 ns/op and 0.580 B/op. Every case allocates less beyond its floor (0.380 to
0.944). 37 cases are faster beyond their floor, and 6 are within it (0.971–1.022): awsJson1_0 `ShallowMap_L` and
`MixedItem_M`, and rpcv2Cbor `ShallowMap_M`, `ShallowMap_L`, `MixedItem_M` and `MixedItem_L`. None is slower. The REST
cases take 0.498–0.583 of the time.

FU-21 asked for timing checks on metal. `awsQuery_GetMetricDataRequest_S`, slower in both unprofiled laptop rounds, is
faster on metal: 2,124.3 → 1,991.6 ns/op (0.938, per repetition 0.924–0.953, floor 2.5%*).
`rpcv2Cbor_PutItemRequest_BinaryData_S`, which FU-12 had made 8.6% slower (FU-16), is 0.692 (1,020.3 → 705.4
ns/op). The cases flagged slower in single laptop rounds (`awsJson1_0_PutItemRequest_ShallowMap_L` and
`MixedItem_L`, rpcv2Cbor `ShallowMap_L` and `MixedItem_M`) are 0.967–0.982 on metal: faster or within their floor.
The awsJson1_0 `MixedItem_S` and `MixedItem_M` cases, bimodal per fork on the laptop, are 0.920 (faster, 1,825.0 →
1,679.2 ns/op, per repetition 0.912–0.927) and 0.987 (within the 2.7%* floor, 8,562.1 → 8,453.8 ns/op, per repetition
0.984–0.990).

### 6.4 The acceptance bar

On metal, 42 of the 43 marshall cases allocate less than the comparable Baseline_Build beyond their floor, with
comparable ratios of 0.058–0.940 (the set 3 appendix). One does not:

| case | comparable baseline B/op | feature-marshall B/op | comparable ratio | floor | verdict |
|---|---:|---:|---:|---:|---|
| awsQuery_PutMetricDataRequest_Baseline | 940 | 1,112 | 1.183 | 0.0% | more |

The fix report expected this case: 1,112 B/op against 902 for the comparable Baseline_Build on the laptop (1.23×,
with the baseline taken from the first metal profile). On metal, the comparable baseline is 940 B/op in the same run.
The feature's 1,112 B/op are the form serializer (475 B, of which the body array is 250 B) and the request objects
(section 4.2). The legacy client builds its form body later, in `QueryParametersToBodyStage`, outside the timed
method, so the baseline's 940 B/op hold no body, and the comparable view does not credit that stage to the baseline
(section 2.4). The fix moved no work out of the timed method. FU-19 records the case, with lazy creation of the
inner serializers as an option.

### 6.5 Laptop and projection against metal

The fix report gave laptop numbers and, per case, a projected drop from the first metal profile. Per class, as class
means of the B/op drop and geomeans of fix / before:

| class | projected drop, B/op (mean) | laptop drop | metal drop | laptop B/op, fix / before | metal B/op, fix / before | laptop ns/op, fix / before | metal ns/op, fix / before | laptop fix / comparable baseline | metal fix / comparable baseline |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| JsonRpc10 | 1,305 | 1,651 | 1,648 | 0.62 | 0.63 | 0.86 | 0.88 | 0.50 | 0.51 |
| Query | 1,198 | 16,736 | 16,737 | 0.57 | 0.57 | 0.83 | 0.87 | 0.45 | 0.45 |
| RestJson | 1,472 | 1,739 | 1,777 | 0.51 | 0.50 | 0.50 | 0.55 | 0.14 | 0.14 |
| RestXml | 1,520 | 1,805 | 1,830 | 0.50 | 0.49 | 0.50 | 0.55 | 0.53 | 0.52 |
| RpcV2Cbor | 1,639 | 1,887 | 1,885 | 0.61 | 0.60 | 0.81 | 0.87 | 0.47 | 0.47 |

B/op does not depend on the machine, and metal confirms the laptop. The fix's B/op on metal is within 1% of the
laptop's on 36 of 43 cases and within 5% on 41. The two others are `awsQuery_PutMetricDataRequest_M` (8,793 on the
laptop, 8,227 on metal) and `rpcv2Cbor_PutItemRequest_Baseline` (1,312 and 1,240; the fix report gives 1,216–1,264
for other laptop runs). The projection counted only the plan's sites. On Query the drop is 16,737 B/op per case on
average against 1,198 projected, because the sized form array also removes the growth on the M and L requests. 40 of the 43 cases reach at least 80% of their projected drop on metal. The 3 below are the 3 the
laptop flagged: `awsQuery_GetMetricDataRequest_S` (21%), `awsQuery_PutMetricDataRequest_S` (72%) and
`rpcv2Cbor_PutItemRequest_Nested_M` (78%).

The laptop's ns/op was optimistic by 2–6 points. On metal, fix / before is 0.87–0.88 on the three codec classes,
against 0.81–0.86 on the laptop, and 0.55 on REST, against 0.50. The fix report's laptop acceptance (fix / comparable
baseline per class) matches metal within 0.01.

## 7. Caveats

- **Comparable is an estimate.** It scales each arm by work-boundary shares from separate profiling runs: of the
  pre-FU-12 feature jar for sets 1 and 2 and for the unmarshall rows, and of the feature-marshall jar for the set 3
  marshall rows. Each share comes from one single-fork profile run per case and arm, and A5's share can move between
  runs (section 2.4). The A1 split is made by frame rules (`serde_ab_profile.py`), and A1-request includes the
  restJson1 generator buffers that the feature does not allocate (section 2.4). A5 is mostly the feature's
  per-character copy of string values into the form body, not escaping. By the operations each side performs it
  removes less than the legacy `QueryParametersToBodyStage` does, but that stage is not measured, and its
  allocation is not credited to the baseline. A5 includes the feature's `Action` / `Version` write, while the
  baseline's put of those values stays; keeping that write in the feature would move the aggregate from 0.3760 to
  0.3766 ns/op in set 2 and from 0.3192 to 0.3200 in set 3. A2 no longer removes the awsQuery request-id pass. The
  as-measured view has no estimate in it.
- **Set 3 combines two runs.** Its marshall rows come from the marshall A/B `20261023-212558` and its unmarshall rows
  from the full A/B `20261023-031253`. Each ratio is paired with the baseline of its own run. Re-measured in the
  marshall A/B, the set 2 marshall rows reproduce the set 2 aggregate within 0.51% (section 3.1). The unmarshall rows
  were not measured again; the fix does not change their code (section 6.2).
- **The baseline is stock V2.** F5 and F6 also make the legacy path cheaper, and the baseline jar does not have them.
  The comparison is against today's V2, not against a V2 built with this branch's shared code (section 6.1).
- **Response size hint, best case.** The JsonRpc10, RpcV2Cbor and Query unmarshall benchmarks send no
  `Content-Length` and repeat one response. So do the restJson1 and restXml `CopyObjectOutput_Baseline` and `_M`
  cases, 2 of the 5 cases in each REST unmarshall class: in the `serde-tests` output data, only `GetObject` declares
  `Content-Length`. Since `4acbd0b03ae`, the feature reads such bodies with a per-operation size hint (for the REST
  protocols, through `HttpBindingClientProtocol`), so it measures the hint's steady state: one copy, no growth. Real
  responses with `Content-Length` were already read with one exact array. Real chunked responses of varying size may
  still grow. The DynamoDB CRC32 path (`Crc32ResponseValidation`) is not in these benchmarks.
- **Laptop numbers are not used for results.** The laptop smoke runs (`smoke-results.md`) and the laptop
  before/after tables in the fix reports checked tooling and direction only. Section 6.5 compares the marshall fix's
  laptop numbers with metal.
- **One host type.** All results are from Graviton2 (Neoverse-N1) on JDK 25. x86 and other JDKs were not run. The
  jars are Java 8 bytecode built with JDK 21.
- **The null experiment** measured the pre-fix feature jar on 20 of the 71 cases. The other 51 cases use their
  class's largest floor. The steady-state check found no drift. Applying these floors to workloads that the fixes
  changed is an assumption: `PutObject_L` went from about 19 µs to 0.84 µs per call, and the feature-fu12 and
  feature-marshall rows run different code.
- **No smithy-java arm.** The comparability work found a smithy-java arm feasible but did not build it. It needs a
  Gradle build of the reference with the same JDK, `implementation=generic` (runtime codegen needs JDK 25), a diff
  of its Smithy test inputs against `serde-tests/*.json`, a class and method name mapping
  (`*SerializeBenchmark.serialize` onto ours), and explicit JMH options. The driver accepts it as another `--jar`
  arm once the name mapping exists.
- **Benchmark data quirk.** The restJson1 and restXml `GetObject_*` responses declare 1, 1,000 and 256,000 bytes but
  send the base64 text. Both arms read them the same way. The L case is dominated by those body copies on both
  arms (about 1.1–1.4 MB/op).

## 8. Reproduction

Raw data is not committed. It lives in the main checkout under `.agents/tasks/serde-benchmark-metal/` and in the jar
caches. The comparable view was recomputed from that data under `.agents/tasks/serde-report-comparable/`; the
original `20261023-095831/boundary.tsv` (format 1) is kept unchanged. Set 3 and section 6 were computed under
`.agents/tasks/serde-report-marshall/`. The marshall runs used the same instance, whose public DNS changed after a
restart: `ec2-52-23-247-58.compute-1.amazonaws.com`.

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
| marshall A/B | `.agents/tasks/serde-benchmark-metal/runs/20261023-212558/` (`serde-ab.json`, `jmh/`, `verify/`, `provenance/`) |
| marshall profiling pass | `.agents/tasks/serde-benchmark-metal/runs/20261024-033608/` (`profile/<arm>/<class>/<case>/<event>/`, `profile-runs.tsv`) |
| marshall shares, format 2 | `.agents/tasks/serde-report-marshall/profile-20261024-033608/` (regenerated; `boundary.tsv` equals the fetched one) |
| marshall summary | `.agents/tasks/serde-report-marshall/marshall-ab-summary.md` |
| set 3 numbers | `.agents/tasks/serde-report-marshall/analysis/report_numbers_marshall.py` → `report-numbers-marshall.md` (it imports `report_numbers_base.py`, a copy of `report_numbers_comparable.py` pointed at the main checkout); `fix_sites.py` → `fix-sites.tsv`; `alloc_top_marshall.sh` → `alloc-top-marshall.txt`; `fix-diff.txt` |
| set 3 report check | `.agents/tasks/serde-report-marshall/analysis/verify_report_marshall.py` → `verify-report-output.txt` |
| marshall fix | `.agents/tasks/fix-marshall-allocation/` (`report.md`, `metal-status.md`, `jmh-laptop.md`, `jmh-laptop-r3.md`, `acceptance.md`, session scripts) |
| jars | `~/.cache/aws-sdk-java-v2/serde-ab-metal/jars` (null), `serde-ab-metal-postfix/jars` (A/B, profiling, FU-12 and marshall baseline, feature), `serde-ab-metal-fu12/jars` (feature-fu12), `serde-ab-metal-marshall/jars` (feature-marshall) |

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
# feature-marshall jar (fix-marshall-allocation/build-marshall-jar.sh; serde-ab.sh needs two arms, so the A/B
# baseline jar is the first)
JAVA_HOME=$J21 scripts/schema-serde/serde-ab.sh --jar baseline=$HOME/.cache/aws-sdk-java-v2/serde-ab-metal-postfix/jars/baseline.jar \
  --arm feature-marshall=a8557684f83042838c7192c0645ba7ec6a4c8808 \
  --sdk-pin feature-marshall=3f460f18acb4cb35ded453c2e9c4c98b73f155a6 \
  --verify-only --out ~/.cache/aws-sdk-java-v2/serde-ab-metal-marshall
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

The two `report_numbers_comparable.py` and `verify_report_comparable.py` steps read a worktree that no longer
exists. `report_numbers_base.py` is the same script pointed at the main checkout, and it reproduces
`report-numbers-comparable.md` (only the line naming the summarizer's path differs).

**Marshall A/B, then its profiling pass** (`fix-marshall-allocation/serde-ab-marshall-session.sh`, from
`~/serde-ab-marshall/`; `launch-marshall-session.sh` ran `setup`, `deploy` of the three jars and `start`, all with
`--remote-dir serde-ab-marshall`):

```bash
R="$HOME/serde-ab-marshall"; J="$R/jars"; S="$R/repo/scripts/schema-serde/serde-ab.sh"
JVM="-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4"
ARMS=(--jar "baseline=$J/baseline.jar" --jar "feature-fu12=$J/feature-fu12.jar" --jar "feature-marshall=$J/feature-marshall.jar"
      --sdk-pin feature-fu12=b17e79d6e1f --sdk-pin feature-marshall=3f460f18acb4cb35ded453c2e9c4c98b73f155a6)
CLASSES=JsonRpc10MarshallBenchmark,QueryMarshallBenchmark,RestJsonMarshallBenchmark,RestXmlMarshallBenchmark,RpcV2CborMarshallBenchmark
"$S" "${ARMS[@]}" --classes "$CLASSES" \
    --reps 4 --pin 32-47 --jmh-opts "-f 2 -wi 5 -w 2s -i 5 -r 2s" --jvm-args "$JVM" --out "$R/out"
"$S" "${ARMS[@]}" --classes "$CLASSES" --profile-only --pin 32-47 \
    --profile-events cpu,alloc --profile-jmh-opts "-f 1 -wi 2 -w 2s -i 3 -r 3s" --jvm-args "$JVM" --out "$R/out"
```

Fetch and analysis (local):

```bash
export SERDE_AB_REMOTE_TARGET=ec2-user@ec2-52-23-247-58.compute-1.amazonaws.com SERDE_AB_REMOTE_KEY=<key.pem>
R=scripts/schema-serde/serde-ab-remote.sh; RD=(--remote-dir serde-ab-marshall)
RUNS=.agents/tasks/serde-benchmark-metal/runs; M=.agents/tasks/serde-report-marshall
$R fetch 20261023-212558 "${RD[@]}" --dest $RUNS
$R fetch 20261024-033608 "${RD[@]}" --dest $RUNS
python3 scripts/schema-serde/serde_ab_profile.py compare $RUNS/20261024-033608 \
  --out-dir $M/profile-20261024-033608
python3 scripts/schema-serde/serde_ab_summary.py summarize $RUNS/20261023-212558/serde-ab.json \
  --null $RUNS/20261023-011610/serde-ab.json --boundary $M/profile-20261024-033608/boundary.tsv \
  > $M/marshall-ab-summary.md
python3 $M/analysis/fix_sites.py --tsv $M/fix-sites.tsv
$M/analysis/alloc_top_marshall.sh > $M/alloc-top-marshall.txt
python3 $M/analysis/report_numbers_marshall.py > $M/report-numbers-marshall.md
python3 $M/analysis/verify_report_marshall.py
```

## Appendix: per-case results (set 3)

The tables below give, for each case:

- the baseline and feature means ± their 99.9% half-width over the 4 repetitions;
- the as-measured ratio and the comparable ratio (section 2.4);
- the case's floor and its verdict in the comparable view.

A floor marked * is inferred from the class max. The marshall rows are from the marshall A/B `20261023-212558`
(feature = feature-marshall `a8557684f83`); the unmarshall rows are from the full A/B `20261023-031253` (feature =
`2549a5b6828`). The set 2 marshall rows are in the previous version of this report and in
`.agents/tasks/serde-report-comparable/report-numbers-comparable.md`. For B/op, the verdict words are "less" and
"more".

### awsJson1_0 marshall (`JsonRpc10MarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| awsJson1_0_GetItemInput_Baseline | 1,458.6 ± 139.4 | 577.4 ± 20.7 | 0.396 | 0.402 | 2.7%* | faster | 2,668 ± 190 | 1,160 ± 0 | 0.435 | 0.466 | 1.3%* | less |
| awsJson1_0_HealthcheckRequest_Example | 690.5 ± 12.4 | 358.3 ± 8.9 | 0.519 | 0.540 | 2.7% | faster | 2,563 ± 116 | 1,136 ± 0 | 0.443 | 0.475 | 0.0% | less |
| awsJson1_0_PutItemRequest_Baseline | 1,498.1 ± 111.4 | 573.2 ± 15.1 | 0.383 | 0.390 | 2.7%* | faster | 2,668 ± 190 | 1,160 ± 0 | 0.435 | 0.463 | 1.3%* | less |
| awsJson1_0_PutItemRequest_ShallowMap_S | 4,783.0 ± 248.3 | 1,392.9 ± 12.3 | 0.291 | 0.293 | 2.7%* | faster | 2,704 ± 0 | 1,415 ± 116 | 0.523 | 0.558 | 1.3%* | less |
| awsJson1_0_PutItemRequest_ShallowMap_M | 27,049.5 ± 1,620.2 | 7,662.3 ± 206.2 | 0.283 | 0.284 | 2.7%* | faster | 4,802 ± 0 | 3,425 ± 0 | 0.713 | 0.736 | 1.3%* | less |
| awsJson1_0_PutItemRequest_ShallowMap_L | 89,051.6 ± 3,864.7 | 24,424.0 ± 143.3 | 0.274 | 0.274 | 2.7%* | faster | 9,427 ± 145 | 8,665 ± 0 | 0.919 | 0.940 | 1.3%* | less |
| awsJson1_0_PutItemRequest_Nested_M | 3,743.5 ± 239.1 | 1,226.6 ± 61.7 | 0.328 | 0.331 | 2.7%* | faster | 3,142 ± 388 | 1,232 ± 0 | 0.392 | 0.414 | 1.3%* | less |
| awsJson1_0_PutItemRequest_Nested_L | 12,510.4 ± 625.7 | 4,161.4 ± 93.3 | 0.333 | 0.333 | 2.7%* | faster | 6,705 ± 0 | 2,352 ± 0 | 0.351 | 0.359 | 1.3%* | less |
| awsJson1_0_PutItemRequest_MixedItem_S | 5,550.9 ± 548.1 | 1,679.2 ± 94.3 | 0.303 | 0.304 | 2.7%* | faster | 3,139 ± 223 | 1,352 ± 0 | 0.431 | 0.454 | 1.3%* | less |
| awsJson1_0_PutItemRequest_MixedItem_M | 28,412.7 ± 1,061.4 | 8,453.8 ± 224.4 | 0.298 | 0.298 | 2.7%* | faster | 6,610 ± 0 | 3,129 ± 0 | 0.473 | 0.484 | 1.3%* | less |
| awsJson1_0_PutItemRequest_MixedItem_L | 83,075.1 ± 11,443.6 | 24,739.1 ± 451.3 | 0.298 | 0.298 | 2.7%* | faster | 13,387 ± 82 | 7,145 ± 0 | 0.534 | 0.540 | 1.3%* | less |
| awsJson1_0_PutItemRequest_BinaryData_S | 2,005.7 ± 76.8 | 828.8 ± 22.1 | 0.413 | 0.419 | 2.7%* | faster | 2,695 ± 116 | 1,494 ± 134 | 0.554 | 0.593 | 1.3%* | less |
| awsJson1_0_PutItemRequest_BinaryData_M | 22,036.1 ± 369.8 | 4,211.3 ± 333.0 | 0.191 | 0.192 | 2.7%* | faster | 26,729 ± 0 | 13,152 ± 0 | 0.492 | 0.496 | 1.3%* | less |
| awsJson1_0_PutItemRequest_BinaryData_L | 174,251.3 ± 2,401.1 | 28,116.0 ± 2,643.2 | 0.161 | 0.161 | 1.0% | faster | 251,351 ± 61 | 105,660 ± 3,528 | 0.420 | 0.421 | 1.3% | less |

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
| awsQuery_GetMetricDataRequest_S | 5,296.9 ± 758.4 | 1,991.6 ± 62.5 | 0.376 | 0.289 | 2.5%* | faster | 6,667 ± 39 | 2,536 ± 0 | 0.380 | 0.383 | 0.0%* | less |
| awsQuery_GetMetricDataRequest_M | 19,826.6 ± 1,765.0 | 7,546.3 ± 268.1 | 0.381 | 0.240 | 2.5%* | faster | 26,241 ± 63 | 8,265 ± 0 | 0.315 | 0.315 | 0.0%* | less |
| awsQuery_GetMetricDataRequest_L | 42,621.1 ± 1,289.3 | 14,056.0 ± 649.3 | 0.330 | 0.199 | 2.5%* | faster | 53,240 ± 148 | 14,057 ± 0 | 0.264 | 0.264 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_Baseline | 446.7 ± 51.0 | 426.6 ± 28.1 | 0.955 | 0.890 | 2.5% | faster | 991 ± 74 | 1,112 ± 0 | 1.122 | 1.183 | 0.0% | more |
| awsQuery_PutMetricDataRequest_S | 2,864.9 ± 245.0 | 1,291.1 ± 29.8 | 0.451 | 0.331 | 2.5%* | faster | 4,156 ± 465 | 1,880 ± 0 | 0.452 | 0.458 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_M | 15,456.1 ± 1,463.3 | 11,737.5 ± 3,986.2 | 0.759 | 0.523 | 2.5%* | faster | 19,653 ± 0 | 8,227 ± 388 | 0.419 | 0.419 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_L | 163,733.9 ± 14,975.7 | 83,258.5 ± 880.5 | 0.509 | 0.436 | 0.3% | faster | 208,613 ± 39 | 105,708 ± 1 | 0.507 | 0.507 | 0.0% | less |

### awsQuery unmarshall (`QueryUnmarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| awsQuery_GetMetricDataResponse_S | 48,017.7 ± 1,026.9 | 7,208.0 ± 98.6 | 0.150 | 0.149 | 0.5% | faster | 101,193 ± 77 | 5,073 ± 0 | 0.050 | 0.048 | 0.7% | less |
| awsQuery_GetMetricDataResponse_M | 153,056.4 ± 6,716.7 | 23,856.6 ± 391.8 | 0.156 | 0.156 | 0.5%* | faster | 270,307 ± 2,619 | 15,374 ± 116 | 0.057 | 0.056 | 0.7%* | less |
| awsQuery_GetMetricDataResponse_L | 1,820,590.4 ± 39,908.2 | 268,796.6 ± 4,196.7 | 0.148 | 0.148 | 0.2% | faster | 2,702,261 ± 24,168 | 161,691 ± 52 | 0.060 | 0.060 | 0.0% | less |

### restJson1 marshall (`RestJsonMarshallBenchmark`)

| case | baseline ns/op | feature ns/op | ratio | comparable | floor | verdict (comparable) | baseline B/op | feature B/op | B/op ratio | comparable | B/op floor | B/op verdict (comparable) |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---|
| restJson1_CopyObjectRequest_Baseline | 2,933.6 ± 436.9 | 454.6 ± 22.8 | 0.155 | 0.156 | 1.2% | faster | 2,736 ± 0 | 984 ± 0 | 0.360 | 0.378 | 0.8% | less |
| restJson1_CopyObjectRequest_M | 10,183.1 ± 504.5 | 4,958.5 ± 115.6 | 0.487 | 0.489 | 1.2%* | faster | 8,641 ± 0 | 5,528 ± 0 | 0.640 | 0.649 | 0.8%* | less |
| restJson1_PutObject_S | 3,904.7 ± 186.9 | 477.3 ± 23.2 | 0.122 | 0.123 | 1.2%* | faster | 19,133 ± 132 | 1,112 ± 0 | 0.058 | 0.058 | 0.8%* | less |
| restJson1_PutObject_M | 3,927.8 ± 325.9 | 486.3 ± 31.7 | 0.124 | 0.125 | 1.2%* | faster | 19,121 ± 194 | 1,112 ± 0 | 0.058 | 0.059 | 0.8%* | less |
| restJson1_PutObject_L | 3,949.1 ± 166.8 | 488.3 ± 13.4 | 0.124 | 0.125 | 1.1% | faster | 19,139 ± 39 | 1,112 ± 0 | 0.058 | 0.058 | 0.0% | less |

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
| restXml_CopyObjectRequest_Baseline | 2,832.4 ± 283.0 | 452.2 ± 39.8 | 0.160 | 0.160 | 1.1% | faster | 3,056 ± 0 | 984 ± 0 | 0.322 | 0.336 | 0.8% | less |
| restXml_CopyObjectRequest_M | 9,959.0 ± 1,111.1 | 4,940.2 ± 115.4 | 0.496 | 0.497 | 2.7%* | faster | 8,997 ± 63 | 5,465 ± 814 | 0.607 | 0.615 | 0.8%* | less |
| restXml_PutObject_S | 2,892.3 ± 304.1 | 476.9 ± 18.4 | 0.165 | 0.166 | 2.7%* | faster | 2,060 ± 89 | 1,112 ± 0 | 0.540 | 0.572 | 0.8%* | less |
| restXml_PutObject_M | 2,872.8 ± 201.3 | 485.0 ± 13.3 | 0.169 | 0.170 | 2.7%* | faster | 2,063 ± 172 | 1,112 ± 0 | 0.539 | 0.571 | 0.8%* | less |
| restXml_PutObject_L | 2,876.3 ± 217.5 | 488.8 ± 39.0 | 0.170 | 0.171 | 2.7% | faster | 2,060 ± 155 | 1,112 ± 0 | 0.540 | 0.573 | 0.0% | less |

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
| rpcv2Cbor_PutItemRequest_Baseline | 1,715.6 ± 130.1 | 614.9 ± 20.2 | 0.358 | 0.365 | 3.8% | faster | 2,878 ± 169 | 1,240 ± 310 | 0.431 | 0.457 | 2.8% | less |
| rpcv2Cbor_PutItemRequest_ShallowMap_S | 4,700.2 ± 52.3 | 1,461.2 ± 97.9 | 0.311 | 0.313 | 3.8%* | faster | 2,890 ± 192 | 1,288 ± 0 | 0.446 | 0.472 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_ShallowMap_M | 26,838.8 ± 1,551.6 | 7,964.0 ± 638.9 | 0.297 | 0.297 | 3.8%* | faster | 5,009 ± 135 | 2,797 ± 155 | 0.558 | 0.579 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_ShallowMap_L | 83,689.7 ± 2,277.7 | 20,686.3 ± 264.6 | 0.247 | 0.247 | 3.8%* | faster | 8,118 ± 147 | 6,977 ± 0 | 0.859 | 0.866 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_Nested_M | 3,862.2 ± 427.5 | 1,460.9 ± 64.5 | 0.378 | 0.381 | 3.8%* | faster | 3,431 ± 267 | 1,528 ± 0 | 0.445 | 0.468 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_Nested_L | 12,811.5 ± 874.3 | 3,440.7 ± 97.9 | 0.269 | 0.269 | 3.8%* | faster | 8,282 ± 110 | 2,216 ± 0 | 0.268 | 0.273 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_MixedItem_S | 5,491.5 ± 386.9 | 1,450.6 ± 98.0 | 0.264 | 0.266 | 3.8%* | faster | 3,439 ± 173 | 1,280 ± 0 | 0.372 | 0.392 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_MixedItem_M | 27,535.7 ± 1,315.1 | 6,858.6 ± 216.3 | 0.249 | 0.249 | 3.8%* | faster | 6,923 ± 194 | 2,525 ± 155 | 0.365 | 0.375 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_MixedItem_L | 80,183.5 ± 1,161.3 | 19,267.4 ± 425.1 | 0.240 | 0.240 | 3.8%* | faster | 12,379 ± 121 | 5,513 ± 0 | 0.445 | 0.451 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_BinaryData_S | 2,003.4 ± 163.6 | 705.4 ± 46.2 | 0.352 | 0.357 | 3.8%* | faster | 2,885 ± 167 | 1,292 ± 155 | 0.448 | 0.478 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_BinaryData_M | 6,148.6 ± 511.3 | 2,340.5 ± 80.2 | 0.381 | 0.382 | 3.8%* | faster | 10,303 ± 317 | 9,364 ± 155 | 0.909 | 0.925 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_BinaryData_L | 26,738.9 ± 833.1 | 9,186.0 ± 677.7 | 0.344 | 0.344 | 1.8% | faster | 243,788 ± 170 | 74,545 ± 0 | 0.306 | 0.306 | 2.8% | less |

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

## Appendix B: the marshall fix alone per case

feature-marshall / feature-fu12 in the marshall A/B `20261023-212558`, with the means ± their 99.9% half-width over
the 4 repetitions, the mean of the per-repetition paired ratios, and the case's null floor (section 6.3). A floor
marked * is inferred from the class max.

| case | feature-fu12 ns/op | feature-marshall ns/op | fix / before | floor | verdict | feature-fu12 B/op | feature-marshall B/op | fix / before | B/op floor | verdict |
|---|---:|---:|---:|---:|---|---:|---:|---:|---:|---|
| awsJson1_0_GetItemInput_Baseline | 709.2 ± 59.2 | 577.4 ± 20.7 | 0.814 | 2.7%* | faster | 2,456 ± 0 | 1,160 ± 0 | 0.472 | 1.3%* | less |
| awsJson1_0_HealthcheckRequest_Example | 513.9 ± 50.2 | 358.3 ± 8.9 | 0.697 | 2.7% | faster | 2,432 ± 0 | 1,136 ± 0 | 0.467 | 0.0% | less |
| awsJson1_0_PutItemRequest_Baseline | 710.0 ± 34.7 | 573.2 ± 15.1 | 0.807 | 2.7%* | faster | 2,456 ± 0 | 1,160 ± 0 | 0.472 | 1.3%* | less |
| awsJson1_0_PutItemRequest_ShallowMap_S | 1,596.7 ± 612.2 | 1,392.9 ± 12.3 | 0.875 | 2.7%* | faster | 2,720 ± 0 | 1,415 ± 116 | 0.520 | 1.3%* | less |
| awsJson1_0_PutItemRequest_ShallowMap_M | 7,943.5 ± 248.7 | 7,662.3 ± 206.2 | 0.965 | 2.7%* | faster | 4,713 ± 60 | 3,425 ± 0 | 0.727 | 1.3%* | less |
| awsJson1_0_PutItemRequest_ShallowMap_L | 24,868.6 ± 645.3 | 24,424.0 ± 143.3 | 0.982 | 2.7%* | within floor | 9,961 ± 1 | 8,665 ± 0 | 0.870 | 1.3%* | less |
| awsJson1_0_PutItemRequest_Nested_M | 1,378.2 ± 268.4 | 1,226.6 ± 61.7 | 0.890 | 2.7%* | faster | 2,528 ± 0 | 1,232 ± 0 | 0.487 | 1.3%* | less |
| awsJson1_0_PutItemRequest_Nested_L | 4,317.6 ± 234.7 | 4,161.4 ± 93.3 | 0.964 | 2.7%* | faster | 3,648 ± 0 | 2,352 ± 0 | 0.645 | 1.3%* | less |
| awsJson1_0_PutItemRequest_MixedItem_S | 1,825.0 ± 27.2 | 1,679.2 ± 94.3 | 0.920 | 2.7%* | faster | 2,657 ± 116 | 1,352 ± 0 | 0.509 | 1.3%* | less |
| awsJson1_0_PutItemRequest_MixedItem_M | 8,562.1 ± 231.1 | 8,453.8 ± 224.4 | 0.987 | 2.7%* | within floor | 4,416 ± 116 | 3,129 ± 0 | 0.709 | 1.3%* | less |
| awsJson1_0_PutItemRequest_MixedItem_L | 25,578.2 ± 232.6 | 24,739.1 ± 451.3 | 0.967 | 2.7%* | faster | 8,441 ± 0 | 7,145 ± 0 | 0.846 | 1.3%* | less |
| awsJson1_0_PutItemRequest_BinaryData_S | 1,088.6 ± 88.4 | 828.8 ± 22.1 | 0.761 | 2.7%* | faster | 2,772 ± 190 | 1,494 ± 134 | 0.539 | 1.3%* | less |
| awsJson1_0_PutItemRequest_BinaryData_M | 4,662.0 ± 501.1 | 4,211.3 ± 333.0 | 0.903 | 2.7%* | faster | 14,448 ± 0 | 13,152 ± 0 | 0.910 | 1.3%* | less |
| awsJson1_0_PutItemRequest_BinaryData_L | 32,814.4 ± 510.0 | 28,116.0 ± 2,643.2 | 0.857 | 1.0% | faster | 111,900 ± 1,732 | 105,660 ± 3,528 | 0.944 | 1.3% | less |
| awsQuery_GetMetricDataRequest_S | 2,124.3 ± 196.8 | 1,991.6 ± 62.5 | 0.938 | 2.5%* | faster | 2,784 ± 0 | 2,536 ± 0 | 0.911 | 0.0%* | less |
| awsQuery_GetMetricDataRequest_M | 8,843.2 ± 199.8 | 7,546.3 ± 268.1 | 0.853 | 2.5%* | faster | 16,469 ± 155 | 8,265 ± 0 | 0.502 | 0.0%* | less |
| awsQuery_GetMetricDataRequest_L | 17,749.1 ± 1,018.7 | 14,056.0 ± 649.3 | 0.792 | 2.5%* | faster | 37,009 ± 1 | 14,057 ± 0 | 0.380 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_Baseline | 551.2 ± 13.3 | 426.6 ± 28.1 | 0.774 | 2.5% | faster | 2,344 ± 0 | 1,112 ± 0 | 0.474 | 0.0% | less |
| awsQuery_PutMetricDataRequest_S | 1,435.8 ± 31.0 | 1,291.1 ± 29.8 | 0.899 | 2.5%* | faster | 2,752 ± 0 | 1,880 ± 0 | 0.683 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_M | 12,300.7 ± 2,414.5 | 11,737.5 ± 3,986.2 | 0.956 | 2.5%* | faster | 13,195 ± 388 | 8,227 ± 388 | 0.623 | 0.0%* | less |
| awsQuery_PutMetricDataRequest_L | 91,735.1 ± 1,866.7 | 83,258.5 ± 880.5 | 0.908 | 0.3% | faster | 184,390 ± 91 | 105,708 ± 1 | 0.573 | 0.0% | less |
| restJson1_CopyObjectRequest_Baseline | 873.7 ± 48.0 | 454.6 ± 22.8 | 0.520 | 1.2% | faster | 2,376 ± 0 | 984 ± 0 | 0.414 | 0.8% | less |
| restJson1_CopyObjectRequest_M | 9,967.1 ± 866.5 | 4,958.5 ± 115.6 | 0.498 | 1.2%* | faster | 9,973 ± 1,499 | 5,528 ± 0 | 0.555 | 0.8%* | less |
| restJson1_PutObject_S | 831.7 ± 13.8 | 477.3 ± 23.2 | 0.574 | 1.2%* | faster | 2,128 ± 0 | 1,112 ± 0 | 0.523 | 0.8%* | less |
| restJson1_PutObject_M | 840.3 ± 45.9 | 486.3 ± 31.7 | 0.579 | 1.2%* | faster | 2,128 ± 0 | 1,112 ± 0 | 0.523 | 0.8%* | less |
| restJson1_PutObject_L | 837.0 ± 46.0 | 488.3 ± 13.4 | 0.583 | 1.1% | faster | 2,128 ± 0 | 1,112 ± 0 | 0.523 | 0.0% | less |
| restXml_CopyObjectRequest_Baseline | 853.8 ± 27.2 | 452.2 ± 39.8 | 0.530 | 1.1% | faster | 2,440 ± 0 | 984 ± 0 | 0.403 | 0.8% | less |
| restXml_CopyObjectRequest_M | 9,925.1 ± 320.1 | 4,940.2 ± 115.4 | 0.498 | 2.7%* | faster | 9,921 ± 0 | 5,465 ± 814 | 0.551 | 0.8%* | less |
| restXml_PutObject_S | 835.1 ± 58.2 | 476.9 ± 18.4 | 0.571 | 2.7%* | faster | 2,192 ± 0 | 1,112 ± 0 | 0.507 | 0.8%* | less |
| restXml_PutObject_M | 849.8 ± 54.2 | 485.0 ± 13.3 | 0.571 | 2.7%* | faster | 2,192 ± 0 | 1,112 ± 0 | 0.507 | 0.8%* | less |
| restXml_PutObject_L | 853.8 ± 28.0 | 488.8 ± 39.0 | 0.573 | 2.7% | faster | 2,192 ± 0 | 1,112 ± 0 | 0.507 | 0.0% | less |
| rpcv2Cbor_PutItemRequest_Baseline | 838.5 ± 127.8 | 614.9 ± 20.2 | 0.734 | 3.8% | faster | 2,756 ± 155 | 1,240 ± 310 | 0.450 | 2.8% | less |
| rpcv2Cbor_PutItemRequest_ShallowMap_S | 1,697.0 ± 52.5 | 1,461.2 ± 97.9 | 0.861 | 3.8%* | faster | 2,888 ± 0 | 1,288 ± 0 | 0.446 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_ShallowMap_M | 7,836.0 ± 3,858.7 | 7,964.0 ± 638.9 | 1.022 | 3.8%* | within floor | 4,445 ± 155 | 2,797 ± 155 | 0.629 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_ShallowMap_L | 21,308.7 ± 152.2 | 20,686.3 ± 264.6 | 0.971 | 3.8%* | within floor | 8,601 ± 0 | 6,977 ± 0 | 0.811 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_Nested_M | 1,551.3 ± 135.2 | 1,460.9 ± 64.5 | 0.942 | 3.8%* | faster | 2,812 ± 155 | 1,528 ± 0 | 0.543 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_Nested_L | 3,847.5 ± 190.5 | 3,440.7 ± 97.9 | 0.894 | 3.8%* | faster | 3,816 ± 0 | 2,216 ± 0 | 0.581 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_MixedItem_S | 1,756.7 ± 481.5 | 1,450.6 ± 98.0 | 0.827 | 3.8%* | faster | 2,911 ± 91 | 1,280 ± 0 | 0.440 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_MixedItem_M | 7,017.7 ± 63.9 | 6,858.6 ± 216.3 | 0.977 | 3.8%* | within floor | 4,146 ± 172 | 2,525 ± 155 | 0.609 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_MixedItem_L | 19,775.4 ± 228.9 | 19,267.4 ± 425.1 | 0.974 | 3.8%* | within floor | 7,137 ± 0 | 5,513 ± 0 | 0.772 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_BinaryData_S | 1,020.3 ± 167.5 | 705.4 ± 46.2 | 0.692 | 3.8%* | faster | 2,928 ± 0 | 1,292 ± 155 | 0.441 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_BinaryData_M | 2,908.1 ± 470.5 | 2,340.5 ± 80.2 | 0.805 | 3.8%* | faster | 10,976 ± 179 | 9,364 ± 155 | 0.853 | 2.8%* | less |
| rpcv2Cbor_PutItemRequest_BinaryData_L | 12,030.0 ± 236.9 | 9,186.0 ± 677.7 | 0.764 | 1.8% | faster | 79,765 ± 52 | 74,545 ± 0 | 0.935 | 2.8% | less |
