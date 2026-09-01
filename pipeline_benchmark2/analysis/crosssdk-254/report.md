# DynamoDB Client Performance: V1 vs V2 vs smithy-java

**Re-measurement on dedicated bare-metal hardware, unmodified SDK v2 2.54.0.**

| | |
|---|---|
| Date | 2026-08-31 |
| Host | EC2 `c6g.metal` — Graviton2 Neoverse-N1, **64 physical cores, no SMT**, single socket/NUMA node, 126 GiB, Amazon Linux 2023 (kernel 6.18), JDK 25.0.4 |
| Clients | V1 **1.12.797** (Apache 4.x) · V2 **2.54.0, published, unmodified** (sync=Apache5, async=CRT) · smithy-java **1.5.1** (HTTP/1.1) |
| Scope | DynamoDB only: GetItem, PutItem, BatchGetItem (25×2 KB), BatchWriteItem (25×2 KB) |
| Raw data | `pipeline_benchmark2/raw/crosssdk-254/` (`small/20260831-2258`, `batch/20260831-2339`, `profiles-*.txt`) |
| Runs | 96 JVM invocations, **0 failures**, server request counts reconciled on every run |
| Supersedes | `analysis/20260824-1618/report.md` (same workload, laptop, several harness defects since fixed) |

The V2 measured here is the **published 2.54.0 artifact from Maven Central** — none of the Project
Racecar optimizations are present. Verified by asserting the jar contains no `SyncApiCallPipeline`
and no `MarshallBufferSizeHints` class.

---

## 1. Executive summary

**smithy-java is 2–3× more CPU-efficient than V2 on every DynamoDB scenario, and the gap is
concentrated in four places: request marshalling, signing, per-call framework orchestration, and
response materialization.** V1 is not a uniformly worse V2 — it beats V2 on small operations and
collapses on batch reads.

Application CPU per operation (excludes JIT/GC/VM threads — see §2):

| scenario | V1 | V2 sync | V2 async | smithy | V2 sync vs smithy |
|---|---:|---:|---:|---:|---:|
| small-get | 114.5 | 151.6 | 204.3 | **46.0** | **3.30×** |
| small-put | 96.6 | 143.4 | 197.1 | **49.8** | **2.88×** |
| batch-get | 1,496.6 | 664.7 | 740.6 | **343.3** | 1.94× |
| batch-put | 731.2 | 798.5 | 784.1 | **304.3** | 2.62× |

Allocation per operation (client code, bytes):

| scenario | V1 | V2 sync | V2 async | smithy | V2 sync vs smithy |
|---|---:|---:|---:|---:|---:|
| small-get | 40,554 | 60,335 | 68,246 | **10,551** | **5.7×** |
| small-put | 36,535 | 54,315 | 61,007 | **6,839** | **7.9×** |
| batch-get | 266,199 | 535,507 | 747,959 | **218,304** | 2.5× |
| batch-put | 191,155 | 209,089 | 371,304 | **123,184** | 1.7× |

Throughput (single-threaded closed loop, ops/wall-sec): smithy 12.7k / 12.1k / 2.6k / 2.8k;
V2 sync 5.2k / 5.4k / 1.4k / 1.2k.

### The five findings that matter

1. **Marshalling dominates writes.** On batch-put, marshalling is **51% of V2 sync's client CPU**
   against smithy's 6.2%, and V2's own metric reports 595 µs/op against smithy's 206 µs/op — a 2.9×
   gap and the single largest attributable difference in the dataset. Two distinct causes (§4.1):
   a buffer that doubles its way to the body size, and a metadata-driven field loop where iterator
   advance and `SdkField` reflection alone are ~14% of all client CPU.
2. **Framework orchestration is the small-operation story.** `pipeline-framework` is **23.7% of V2
   sync's client CPU on small-get** and 11.8 KB/op of allocation, against smithy's 7.8% and 1.4 KB.
   This is per-call plumbing — execution attributes, interceptor context, metric stages, header
   copies — that scales with *requests*, not payload, so it is pure overhead on small operations.
3. **Signing costs V2 15× smithy's allocation.** 20.3 KB/op vs 1.4 KB/op on small-get, and ~14.8%
   vs 11.0% of CPU. Crypto itself is fair (SHA-256 CPU is comparable); the difference is the object
   graph built per signature plus an extra full body traversal for the payload checksum (§4.2).
4. **V2 batch-get materializes the response twice.** `BatchGetResponseMapCopier` re-copies the
   already-unmarshalled map: 119 KB/op, and with `AttributeValue.builder`/`build`/`<init>` the
   generated model layer accounts for **~320 KB/op of V2 sync's 535 KB/op**. smithy deserializes
   straight into its shapes (§4.4).
5. **V1 is bimodal.** It is *faster than V2* on small operations (114.5 vs 151.6 µs/op — a thin,
   primitive pipeline) and then catastrophic on batch-get at 1,497 µs/op (4.4× smithy), because its
   legacy unmarshaller runs on synchronized `Stack`/`Vector`: `Stack.peek` 21.8% + `Vector.isEmpty`
   21.0% + `Vector.size` 6.3% = **49% of its client CPU in collection bookkeeping**.

---

## 2. Method, and what changed since the previous report

Same workload and fairness rules as `analysis/20260824-1618` — out-of-process Jetty server with
canned byte-identical responses, retry parity (3 attempts everywhere), no payload-wrapper asymmetry,
prebuilt request objects, plain HTTP over loopback. What is different, and why the numbers should be
trusted more than the previous round:

- **Dedicated bare-metal host**, client and mock server pinned to disjoint core sets (server 0–15,
  client 32–47), JVM ergonomics tamed (`-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4
  -XX:CICompilerCount=4`; the 64-core defaults are 18 compiler and 43 GC threads).
- **Application CPU, not process CPU.** Process CPU includes compiler/GC/VM threads and does not
  converge — it read 114→49 µs/op on an unchanged client purely by lengthening the window. All CPU
  figures here are per-thread application CPU (`ThreadMXBean`, native compiler/GC/VM threads absent
  by construction).
- **Warm-up runs until JIT compilation quiesces**, not a fixed count. Actual warmup was 45k–155k
  operations depending on client. 23 of 24 timing cases reported `steady_state=true`.
- **Progress reporting is off.** The previous harness printed a line per operation inside the timed
  loop, adding ~28–39 µs/op to every measurement.
- **Transports pinned.** Three `SdkHttpService` implementations sit on the classpath and V2 chooses
  by an internal priority table; `v2-sync` is Apache5 (which is what the previous report was
  measuring too, while stating Apache 4.x).
- **Per-case operation counts.** Allocation is divided by the operations each recording actually
  covered (warmup + measured), read from that case's log. This matters: with quiescence warmup,
  smithy warmed 155k operations against v2-sync's 45k, so a single nominal divisor would have
  overstated smithy's bytes/op by ~60% relative to V2's.
- Timing is the mean of 3 interleaved reps; rep spread was 0.5–6.5% (mostly under 4%).

**Caveats.** (a) `v2-async` small-op runs reported `steady_state=true` on only 1 of 3 reps — the
async client compiles longer on these cores — so treat its small-op CPU as ±few % and prefer its
latency. (b) Single-threaded closed loop: async pays thread-hop cost per call that a concurrent
workload would amortize; its allocation would not improve. (c) Loopback, no TLS or DNS — this is
pipeline overhead, not end-to-end AWS latency. (d) Graviton2 is ~3–4× slower per core than the
laptop used previously, so absolute µs are larger; ratios are the comparable quantity.

**Sanity checks that passed.** Server-side CPU per operation is within 2% across the three V2/V1
clients on every scenario (70–73 µs small, 105–130 µs batch), i.e. all clients gave the server
identical work; smithy is ~15% lower purely because it completes more operations per second. Every
run's server request count equalled its iteration count.

---

## 3. Where the time goes

### 3.1 CPU by category (% of client-code samples)

**small-get** — the per-request overhead scenario

| category | V1 | V2 sync | V2 async | smithy |
|---|---:|---:|---:|---:|
| socket-syscall | 14.0% | 15.1% | 14.6% | 25.4% |
| http-client | 32.9% | 20.9% | 12.7% | 15.2% |
| **pipeline-framework** | 0.2% | **23.7%** | **20.2%** | 7.8% |
| signing | 14.6% | 14.8% | 12.3% | 11.0% |
| unmarshall | 19.5% | 8.4% | 8.8% | 5.2% |
| json | 6.4% | 4.7% | 2.9% | 15.2% |
| marshall | 3.0% | 4.3% | 3.7% | 1.0% |
| retry | 7.1% | 1.6% | 2.5% | 2.7% |
| endpoint-rules | – | 4.4% | 3.3% | 5.0% |
| thread-sync | 0.0% | 0.1% | 7.0% | 7.5% |

**batch-put** — the write/marshalling scenario

| category | V1 | V2 sync | V2 async | smithy |
|---|---:|---:|---:|---:|
| **marshall** | 50.7% | **51.1%** | 45.5% | **6.2%** |
| json | 19.1% | 17.6% | 18.0% | **56.9%** |
| socket-syscall | 10.0% | 7.5% | 4.5% | 11.7% |
| http-client | 10.3% | 7.0% | 4.4% | 5.5% |
| crypto | 3.6% | 3.3% | 4.0% | 9.1% |
| pipeline-framework | 0.1% | 5.8% | 9.8% | 2.8% |
| signing | 3.4% | 4.0% | 5.1% | 2.7% |

The `marshall`/`json` split is the whole story. smithy spends its write CPU *inside the Jackson
generator* (56.9% json, 6.2% marshall glue): its generated serializers call the generator directly.
V2 spends half its CPU in the marshaller framework *around* Jackson (51.1% marshall, 17.6% json).
Crypto is a fairness check — 3.3% vs 9.1% only because smithy's denominator is 2.5× smaller; the
absolute SHA-256 work is comparable.

**batch-get** — the read/response scenario

| category | V1 | V2 sync | V2 async | smithy |
|---|---:|---:|---:|---:|
| **unmarshall** | **74.1%** | 36.4% | 37.5% | 12.5% |
| json | 14.0% | 23.9% | 24.4% | **64.5%** |
| pipeline-framework | 0.1% | 12.1% | 9.6% | 2.3% |
| http-client | 5.0% | 8.8% | 5.1% | 4.3% |
| socket-syscall | 2.8% | 6.7% | 7.9% | 8.3% |

Same architectural split, mirrored: smithy's deserialization *is* the parse loop; V2 splits work
between its unmarshaller layer and shaded Jackson; V1 does nearly everything in its own framework.

### 3.2 Pipeline phases, from each SDK's own metrics (avg µs/op)

Semantics vary between SDKs — read the notes:

| phase | V1 | V2 sync | V2 async | smithy |
|---|---:|---:|---:|---:|
| **small-get** | | | | |
| marshall / serialize | 5.0 | 8.4 | 8.7 | **2.3** |
| sign | 18.1 | 21.4 | 24.4 | **11.4** |
| endpoint resolve | – | 6.4 | 6.5 | 2.6 |
| transport | 84.4 | 94.1 | 143.4 | ~59 † |
| unmarshall / deserialize | 28.1 | 18.6 | 14.8 | **8.9** |
| reported total | 158.9 | 168.5 ‡ | 184.3 ‡ | 85.9 |
| **batch-put** | | | | |
| **marshall / serialize** | 594.2 | **594.7** | 524.5 | **206.0** |
| sign | 48.8 | 49.1 | 52.1 | 39.9 |
| transport | 139.3 | 141.0 | 170.6 | ~107 † |
| reported total | 818.9 | 238.4 ‡ | 255.6 ‡ | 358.1 |
| **batch-get** | | | | |
| marshall | 21.4 | 24.8 | 25.4 | 8.5 |
| **unmarshall / deserialize** | **1,374.3** | **462.6** | 434.7 | **281.4** |
| transport | 101.6 | 110.1 | 654.2 § | ~81 † |
| reported total | 1,547.7 | 664.3 ‡ | 696.5 ‡ | 389.9 |

† smithy transport derived as `attempt − sign − deserialize` (its `serialization_duration` sits
outside `attempt_duration`).
‡ **V2's `ApiCallDuration` excludes marshalling** — `BaseClientHandler` measures
`MarshallingDuration` before the pipeline timer starts, contradicting the additivity formula in
`CoreMetric.API_CALL_DURATION`'s javadoc. On batch-put this makes the reported total (238 µs) less
than marshalling alone (595 µs). Anyone consuming these metrics needs to know this.
§ V2 async's `UnmarshallingDuration` overlaps `ServiceCallDuration`, so async response-heavy phases
double-count.

Metrics and profiles agree on the diagnosis: marshalling on writes, response materialization on
reads, and a fixed per-call tax that only shows up on small operations.

---

## 4. The four gaps, with the responsible code

### 4.1 Marshalling — the largest gap (batch-put: 595 vs 206 µs/op, 2.9×)

Two independent causes.

**Buffer growth.** `SdkByteArrayOutputStream.write` is **56% of V2 sync batch-put allocation
(117,759 B/op)**. The generator starts at 1 KB and doubles to reach a ~50 KB body, so it allocates
1+2+4+…+64 KB of intermediate arrays per request. smithy's equivalent
(`ByteBufferOutputStream.ensureCapacity`, 116,182 B/op, 94% of its allocation) does the same thing —
**neither SDK sizes this buffer well**, and it is the reason batch-put is the one scenario where V2's
*total* allocation is close to smithy's.

**The field loop.** V2 sync batch-put's hot self frames:

| % client CPU | frame |
|---:|---|
| 13.89% | `itable stub` (megamorphic interface dispatch) |
| 8.20% | `jackson UTF8JsonGenerator._writeStringSegment` |
| **6.58%** | `java/util/Arrays$ArrayItr.next` |
| 6.06% | `__GI___libc_write` (socket) |
| **4.66%** | `java/util/Arrays$ArrayItr.hasNext` |
| 3.92% | `JsonProtocolMarshaller.marshallPayloadField` |
| 3.37% | `JsonProtocolMarshaller.doMarshal` |
| 2.83% | `sha256_implCompressMB` |
| **2.81%** | `SdkField.get` |

**~11% of all client CPU is iterator allocation and advance over `sdkFields()`**, plus 2.8% in
`SdkField` value reflection, plus a 13.9% megamorphic dispatch bill from resolving a marshaller per
field per value. smithy's equivalent frames are the Jackson generator itself (`_writeStringSegment`
22.7%, `writeName` 4.4%) plus `String.charAt` 7.0% — it has no per-field metadata layer because its
serializers are generated straight-line code. `JsonProtocolMarshaller.doMarshal` also allocates
33,539 B/op in its own right.

### 4.2 Signing — 15× smithy's allocation (20.3 vs 1.4 KB/op)

V2 sync small-get signing sites: `ChecksumUtil.lambda$readAll$0` **4,077 B/op** (a fresh 4 KB buffer
per request to drain the body for the payload checksum), `V4CanonicalRequest.getCanonicalHeadersString`
2,360 B/op, `V4RequestSigner.lambda$header$0` 1,123 B/op. smithy's entire signing allocation is
`SigV4Signer.getAuthHeader` at 313 B/op. The crypto is identical; the difference is the object graph
and an extra full-body traversal.

### 4.3 Framework orchestration — the small-operation tax

23.7% of client CPU and 11.8 KB/op on small-get, versus smithy's 7.8% / 1.4 KB. Named sites:
`CollectionUtils.lambda$deepCopyMap$1` 2,176 B/op (header-map deep copy on builder mutation),
`Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` 1,686 B/op (restating headers for Apache),
`SdkByteArrayOutputStream.<init>` 1,091 B/op. In CPU, `DefaultAuthSchemeOption.consumeProperty`
alone is 1.42% and `itable stub` 6.27%. Note `http-client` is also 20.9% of CPU and 13.4 KB/op for
V2 sync, of which `InputStreamEntity.writeTo` is 4,017 B/op — the body is streamed through *another*
4 KB copy buffer on its way to the socket.

### 4.4 Response materialization — batch-get (463 vs 281 µs/op; 535 vs 218 KB/op)

V2 sync batch-get allocation is dominated by the generated model layer:
`BatchGetResponseMapCopier.lambda$copy$0` **119,405 B/op (22%)**, `AttributeValue.builder` 84,220,
`AttributeValue$BuilderImpl.build` 73,781, `BuilderImpl.<init>` 42,324, `MapAttributeValueCopier`
12,016 — **~332 KB/op, 62% of the total**, spent building the response and then *copying it again*
through generated copiers. Jackson's own parse buffer is 67,249 B/op and the unmarshalling parser
61,788. smithy's `unmarshall` category is 106,637 B/op total.

For contrast, **V1's** batch-get problem is entirely different and worse: `Stack.peek` 21.8%,
`Vector.isEmpty` 21.0%, `Vector.size` 6.3%, `String.equals` 7.1% — synchronized legacy collections
driving its token loop, 1,374 µs/op of response processing.

---

## 5. Where V2 async differs from V2 sync

Async is slower than sync on every scenario in this single-threaded closed loop (small-get 204 vs
152 µs/op), for two reasons visible in the data:

- **Thread-hop cost**: `thread-sync` is 7.0% of client CPU on small-get (sync: 0.1%), plus 10.8%
  in `other` (futex/park frames), i.e. roughly 18% of async CPU is coordination. This is partly an
  artifact of the workload shape and would amortize under concurrency.
- **Allocation that would not amortize**: `pipeline-framework` is **175,059 B/op on batch-put**
  (sync: 11,449) — 47% of async's total. This is the async request-body path re-copying an
  already-marshalled body, and it is real work regardless of concurrency.

---

## 6. Opportunity, sized

Combining the phase timings and category shares for **V2 sync**:

| target | scenario | current | plausible | basis |
|---|---|---:|---:|---|
| Marshalling field loop + buffer | batch-put | 595 µs/op | ~250–300 | smithy does the same output at 206 µs with the same Jackson generator |
| Framework per-call overhead | small-get | 23.7% of CPU | ~8–10% | smithy's 7.8% doing equivalent orchestration |
| Signing object graph + body traversal | all | 20.3 KB/op | ~5 KB | identical crypto; the difference is allocation and an extra pass |
| Response copiers | batch-get | 332 KB/op in model layer | ~120 KB | remove the second materialization |

Rough aggregate: **35–45% of V2 sync's application CPU** on small operations and **~40% on
batch-put** appears addressable without changing public API semantics. The marshalling item alone is
the largest single lever, and it is the one place where V2 is beaten by a *drop-in equivalent* — same
Jackson, same wire bytes, 2.9× the CPU.

Ranked by expected value: (1) marshalling field loop, (2) response copier elimination in codegen,
(3) framework per-call allocations, (4) signing object graph, (5) async body copy.

---

## 7. Reproducing this

```bash
cd test/standalone-e2e-benchmarks
./scripts/build-jar.sh baseline254 --sdk-version 2.54.0          # published, unmodified V2
./scripts/deploy-remote.sh --target <host> --key <key> --jar ../../pipeline_benchmark2/jars/racecar-baseline254-published-2.54.0*.jar
./scripts/remote-run.sh start <script>                            # see raw/crosssdk-254 manifests
```

Collections: `--clients v1,v2-sync,v2-async,smithy`, small scenarios at 200k iterations / 20k
minimum warmup, batch at 40k / 10k, 3 reps, concurrency 1, pinned as in §2. Profiles aggregated on
the host with `analysis/scripts/collection_profile_report.py` (per-case operation counts read from
the run logs). Full harness methodology: `test/standalone-e2e-benchmarks/README.md`.

Every number in this report is regenerated from the data committed alongside it — `raw/` is
gitignored, so `data/` holds the `results.csv`, `manifest.md`, per-case `metrics.txt` and aggregated
profile tables that back each table here:

```bash
python3 pipeline_benchmark2/analysis/scripts/crosssdk_tables.py \
        pipeline_benchmark2/analysis/crosssdk-254/data
```

Two categorizer defects were fixed while producing this report, both of which would have distorted
the CPU tables: Linux glibc syscall leaves (`__GI___libc_write`, `__poll`) were not being recognized
as syscalls, inflating `pipeline-framework` from 23.7% to 42.6%; and Apache5 frames
(`org/apache/hc/*`) were not recognized as `http-client`. Any earlier CPU-category table produced on
Linux, or involving Apache5, is suspect.
