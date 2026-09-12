# Two ways to make the V2 pipeline fast: incremental optimization vs a smithy-java bridge

| | |
|---|---|
| Date | 2026-09-12 |
| Host | EC2 `c6g.metal` — Graviton2 Neoverse-N1, 64 physical cores, no SMT, single socket/NUMA node, 126 GiB, Amazon Linux 2023, JDK 25.0.4 |
| Scope | DynamoDB: GetItem, PutItem, BatchGetItem (25×2 KB), BatchWriteItem (25×2 KB), DescribeTable |
| Raw data | `pipeline_benchmark3/data/` — `results.csv` (153 timing runs), `profiles/` (56 recordings), `timing-tables.txt`, `profile-tables.txt` |
| Runs | 153 timing runs + 56 profiles, **0 failures** |
| Verification | `local-verification.md` — every arm's pipeline verified at runtime; response equality checked |

Two competing approaches to the same problem, measured against each other:

- **Optimization** — the incremental work recorded in `pipeline_benchmark2/project_racecar_summary.md`:
  ~20 changes to V2's own pipeline, serde, signing and transport adapters, each measured. Fully
  compatible; ships as V2.
- **Bridge** — a shim that keeps V2's public interface and delegates the entire call to smithy-java
  (`core/smithy-java-bridge`, branch `smithy-java-bridge-alexwoo-full`). Prototype: sync only, limited
  configuration support, with known compatibility gaps.

---

## 1. Executive summary

**Neither approach wins outright. The answer splits cleanly on payload size, and the split is
mechanical rather than incidental.**

Application CPU per operation, and each approach against the other:

| scenario | v1 | stock V2 2.54.0 | **optimized V2** | **bridged V2** | smithy-java | bridged / optimized |
|---|---:|---:|---:|---:|---:|---:|
| small-get | 116.2 | 151.5 | 105.7 | **74.3** | 50.9 | **0.70×** |
| small-put | 97.5 | 146.0 | 102.0 | **68.3** | 47.6 | **0.67×** |
| describe-table | – | 191.5 | 133.0 | **102.8** | – | **0.77×** |
| batch-get | 1495.5 | 670.3 | **335.0** | 481.0 | 345.5 | 1.44× |
| batch-put | 751.8 | 817.6 | **290.2** | 431.5 | 303.6 | 1.49× |

1. **On per-request cost the bridge wins decisively** — 30–33% cheaper than the fully optimized V2
   pipeline on small operations, and 23% on a structure-heavy read. It gets there by not running V2's
   per-call machinery at all: framework orchestration falls 22.8 → 8.1 µs and signing 12.9 → 4.5 µs
   (§4.1). This is the part of V2 that twenty incremental changes could not remove, because it is the
   cost of the contracts themselves.
2. **On payload-scaled work the optimization work wins, and has caught up with native smithy-java** —
   optimized V2 is **1.03× smithy on batch-get and 0.96× on batch-put** (i.e. slightly ahead), while the
   bridge is 1.44×/1.49× *more* expensive than optimized V2. The bridge's deserialization costs 349 µs
   against optimized V2's 174 µs on batch-get (§4.2): it pays to translate V2 POJOs across smithy's
   schema layer, and that translation scales with the payload.
3. **Both beat stock V2 substantially**, in different places: optimization is 0.70× stock on small ops
   and 0.35–0.50× on batch; the bridge is 0.47–0.49× stock on small ops and 0.53–0.72× on batch.
4. **The bridge allocates more than optimized V2 while being faster** — 49.7 vs 32.7 KB/op on small-get —
   so its advantage is not an allocation story. Its serde adapters allocate heavily (unmarshall 21.0 vs
   5.2 KB/op) and its framework savings pay for that anyway.
5. **A correctness gap, found before any timing:** the bridge returns DynamoDB `NULL` attributes with no
   member set instead of `NUL=true`, top-level and nested in lists. Everything else matches across all
   arms. See §2.4 — this is the kind of thing a timing-only comparison ships by accident.

**Read together:** the bridge is the better answer for small, high-rate requests; the optimization work
is the better answer for payload-heavy ones, and it is compatible today. The two are not mutually
exclusive — the bridge's wins are in framework and signing, the optimization work's wins are in serde.

---

## 2. Method, and why these numbers can be trusted

Same harness, host, pinning and methodology as `pipeline_benchmark2/analysis/crosssdk-254/report.md`:
out-of-process Jetty server with canned byte-identical responses, retry parity (3 attempts everywhere),
prebuilt request objects, plain HTTP over loopback, client pinned to cores 32–47 and server to 0–15,
JVM ergonomics tamed, warmup run until JIT compilation quiesces, application CPU (not process CPU).
Timing is the mean of 5 repetitions for small scenarios and 3 for batch, with arms cycled inside each
repetition and the order reversed on even repetitions.

### 2.1 The measurement problem this comparison creates, and how it is closed

The bridge replaces the generated client's execution path but **keeps every class name and Maven
coordinate stock V2 uses.** A bridged and a stock `DynamoDbClient` therefore cannot share a classpath,
and are distinguishable only by which jar the JVM loaded. Nothing about a successful run would look
wrong if two jars were swapped — an arm would report one pipeline's numbers under the other's name.

So: **three jars, one harness commit** (`ac977512cbe`), process-isolated (the harness already runs one
JVM per run), with the SDK the only difference between arms. The bridge SDK was built in a disposable
`git worktree` and installed as `2.46.11-SMITHY-BRIDGED` so `~/.m2` cannot be ambiguous — which
mattered, because it already held a plain `2.46.11-SNAPSHOT` from unrelated work.

And every V2 arm **verifies its own pipeline before being measured**, two ways: the loaded client's
wiring (does it hold a `SmithyBridgeClient`?) and a probe call's stack (which framework frames actually
execute?). 21 positive and 4 negative cases pass — an arm pointed at the wrong jar fails the run rather
than reporting numbers. Verdicts are recorded next to every result in `all-results.txt`.

### 2.2 Cross-jar comparability, measured rather than assumed

The headline comparison is cross-jar, so the session includes a null: **`v1` is identical code at an
identical version in every jar** (its Maven coordinate does not depend on the V2 version).

| scenario | v1 on optimized jar | v1 on bridged jar | delta |
|---|---:|---:|---:|
| small-get | 116.2 | 116.6 | +0.3% |
| small-put | 97.5 | 95.6 | −1.9% |
| batch-get | 1495.5 | 1506.8 | +0.8% |
| batch-put | 751.8 | 737.3 | −1.9% |

Within ±2% — the floor this rig has always had. Cross-jar comparisons at the sizes reported here are
sound. (The intended null was the `smithy` arm, as the bridge branch's own report used; it cannot serve
here because the bridged jar carries smithy-java **1.6.1** while the harness's smithy client is
generated against **1.5.1**, and smithy-java's runtime version check refuses the mismatch. `v1` is a
better null anyway.)

### 2.3 Independent agreement with the earlier report

Three arms in this collection also appear in `crosssdk-254`, measured months earlier with a different
harness commit. Allocation per operation on small-get:

| arm | crosssdk-254 | here | agreement |
|---|---:|---:|---:|
| v1 | 40,554 | 42,107 | 3.8% |
| v2-sync stock 2.54.0 | 60,335 | 63,353 | 5.0% |
| smithy-java | 10,551 | 10,689 | 1.3% |

### 2.4 Response equality — checked, and it found something

Timing is only comparable if the arms return the same thing; the harness discards responses, so a path
that dropped members would simply look fast. `ResponseDigest` renders GetItem, BatchGetItem and
DescribeTable field by field from every jar against one server.

**Three stock builds agree exactly** (published 2.46.10, published 2.54.0, and the full optimization
stack) — independent confirmation that the optimization work changed no observable result. **The bridge
differs on one thing:**

```
stock  : nullField=AttributeValue(NUL=true) ... listField=[..., AttributeValue(NUL=true)]
bridged: nullField=AttributeValue()         ... listField=[..., AttributeValue()]
```

`av.nul()` returns `null` instead of `TRUE` and `av.type()` reports the value as unset. Everything else
across all three responses is identical, including 25 batch items and 45 DescribeTable structures. It is
too small to move any timing; it belongs in the bridge's compatibility ledger.

### 2.5 Caveats that matter

1. **Async arms are not steady-state.** 0/5 of optimized-async and 1–3/5 of stock-async runs reached
   quiescence; the async client compiles longer on these cores. Async per-operation CPU should be read as
   ±a few percent, and latency preferred. All sync arms — including every bridged arm — are 5/5 steady.
2. **Different SDK vintages.** The bridge is based on 2.46.11, the optimization work on 2.54.4. The
   head-to-head is unaffected (both are real artifacts measured side by side), but *attribution* is:
   part of any bridged-vs-optimized difference could be stock drift across those releases. The
   collection includes a free handle on that — stock async at 2.46.11 vs 2.54.0 on the same harness:
   187.0 vs 206.5 µs on small-get (+10.4% for the newer), 693.8 vs 737.9 on batch-get (+6.4%), 837.7 vs
   857.0 on batch-put (+2.3%). **Stock V2 got 2–10% more expensive between those releases**, so the
   assumption that this drift is negligible is not quite right. It does not overturn any conclusion here,
   but a sync stock-2.46 arm would be needed to attribute cleanly.
3. **smithy-java versions differ between arms.** The `smithy-java` reference arm is 1.5.1; the bridge
   internally runs 1.6.1. The two "smithy" numbers in this report are therefore not the same runtime.
4. **The bridge is a prototype**: sync only, limited configuration support, with a compatibility ledger
   of its own (`compatability_issues.md` on its branch). None of that is priced into these numbers.
5. Loopback, no TLS, no connection churn, single-threaded closed loop. This is pipeline cost.

---

## 3. Throughput and latency

Throughput (ops/wall-sec, single-threaded closed loop) and p50 latency:

| scenario | metric | v1 | stock 2.54.0 | optimized | bridged | smithy |
|---|---|---:|---:|---:|---:|---:|
| small-get | ops/s | 6,404 | 5,189 | 6,745 | **9,306** | 11,943 |
| small-get | p50 µs | 154.6 | 191.0 | 146.8 | **106.0** | 82.6 |
| batch-get | ops/s | 647 | 1,388 | **2,590** | 1,910 | 2,598 |
| describe-table | ops/s | – | 4,260 | 5,681 | **7,319** | – |

Same shape as the CPU tables: the bridge is 1.38× optimized V2's throughput on small-get and 0.74× on
batch-get.

---

## 4. Where the difference comes from

CPU by category, µs/op (from per-arm profiles, scaled by each run's application CPU). Categories are the
cross-SDK report's, so smithy's own frames land in comparable buckets — its per-call orchestration in
`pipeline-framework`, its codec in `json`.

### 4.1 small-get: the bridge's win is framework and signing

| category | stock 2.54.0 | optimized | bridged | smithy |
|---|---:|---:|---:|---:|
| socket-syscall | 31.4 | 22.2 | 17.5 | 14.6 |
| http-client | 35.1 | 23.8 | 23.8 | 8.0 |
| **pipeline-framework** | 31.6 | 22.8 | **8.1** | 5.3 |
| **signing** | 20.8 | 12.9 | **4.5** | 5.4 |
| unmarshall + json | 20.5 | 10.6 | 12.8 | 10.0 |
| marshall | 4.6 | 2.8 | 1.0 | 0.5 |
| endpoint-rules | 6.6 | 4.7 | 3.4 | 3.2 |
| **total** | **156.8** | **104.5** | **74.1** | **55.0** |

The 30 µs the bridge saves over optimized V2 decomposes as **−14.7 framework, −8.4 signing, −4.7
syscall, −1.3 endpoint rules, +2.2 serde**. Two observations worth separating out:

- **`http-client` is identical (23.8 µs).** The bridge keeps V2's Apache5 transport through an adapter,
  so it inherits that cost exactly — and it is now the single largest attributable line in the bridged
  arm. smithy-java's native client does the same job for 8.0 µs, which is where the bridge's remaining
  19 µs gap to native smithy mostly lives.
- **smithy's signer is cheaper than ours even after phase F** — 4.5–5.4 µs against our optimized 12.9.
  That is a concrete, portable target for the optimization track, independent of any bridge.

### 4.2 batch-get: the bridge's loss is deserialization

| category | stock 2.54.0 | optimized | bridged | smithy |
|---|---:|---:|---:|---:|
| unmarshall | 228.2 | 170.7 | 85.0 | 41.5 |
| json | 174.0 | 2.8 | 264.3 | 224.7 |
| **unmarshall + json** | **402.2** | **173.5** | **349.3** | **266.2** |
| pipeline-framework | 72.9 | 31.6 | 12.1 | 6.0 |
| signing | 27.3 | 17.3 | 5.9 | 7.1 |
| http-client + socket | 117.3 | 74.1 | 104.1 | 40.8 |
| **total** | **669.8** | **320.5** | **489.0** | **340.9** |

The bridge keeps its framework and signing wins (−19.5 and −11.4 µs) but loses 176 µs on
deserialization. Optimized V2's serde is the fastest of the four — ahead of native smithy-java's by 93 µs
— because the optimization track replaced Jackson with a byte-level reader and generates straight-line
deserialization (the `json` category collapsing to 2.8 µs is that work). The bridge cannot benefit from
any of it: it hands the payload to smithy's codec and then translates the result across the schema
boundary into V2 POJOs.

Its transport cost also grows (104.1 vs 74.1 µs) where the payload is 38 KB, which is the adapter
boundary showing up a second time.

### 4.3 Allocation, bytes/op

| scenario | v1 | stock 2.54.0 | optimized | bridged | smithy |
|---|---:|---:|---:|---:|---:|
| small-get | 42,107 | 63,353 | **32,709** | 49,669 | 10,689 |
| small-put | 39,721 | 57,573 | **27,833** | 42,735 | 7,504 |
| batch-get | 273,869 | 546,219 | 295,156 | 516,752 | **224,342** |
| batch-put | 194,702 | 213,471 | **71,180** | 158,978 | 124,483 |

The bridge allocates 1.5× optimized V2 on small operations while being 30% cheaper in CPU — its
advantage is not allocation. On batch-put optimized V2 allocates **less than native smithy-java**
(71 KB vs 124 KB), which is the marshalling work from the optimization track.

---

## 5. What this means

- **For small, high-rate requests the bridge is the stronger approach**, and the reason is structural:
  V2's per-call framework and signing cost ~35 µs that the optimization track reduced but could not
  remove, because the contracts require it. Delegating the call removes it outright.
- **For payload-heavy work the optimization track is stronger**, and also structural: it reaches
  parity-to-better against native smithy-java on serde, and the bridge pays a translation on every
  payload byte that neither native path pays.
- **The two are complementary, not alternatives.** The bridge's wins are framework and signing; the
  optimization track's wins are serde and marshalling. A bridge that carried V2's optimized serde, or an
  optimized V2 whose framework and signer were as cheap as smithy's, would beat both arms measured here.
- **Concrete portable targets for the optimization track**, from §4.1: smithy signs for ~5 µs against our
  12.9; smithy's HTTP client does the transport for 8 µs against Apache5's 23.8. Neither requires a
  bridge.
- **The bridge's costs are not only performance.** Sync only, limited configuration, and a `NULL`
  handling defect found here. Those belong in the decision alongside the 30%.

---

## 6. Reproducing

```bash
# bridge SDK, installed under a distinguishing version from a disposable worktree
git worktree add /tmp/bridge-wt smithy-java-bridge-alexwoo-full
cd /tmp/bridge-wt && mvn versions:set -DnewVersion=2.46.11-SMITHY-BRIDGED -DgenerateBackupPoms=false
mvn install -pl ':dynamodb,:apache-client,:apache5-client,:aws-crt-client,:smithy-java-bridge,:bom-internal' \
    --am -P quick -Dmaven.test.skip=true

# three jars, one harness commit
cd test/standalone-e2e-benchmarks
./scripts/build-jar.sh bench3-racecar
./scripts/build-jar.sh bench3-bridged  --sdk-version 2.46.11-SMITHY-BRIDGED --sdk-commit c1d88972e18
./scripts/build-jar.sh bench3-stock254 --sdk-version 2.54.0

# verify before measuring, then collect
./scripts/verify-pipelines.sh && ./scripts/verify-responses.sh
./scripts/remote-run.sh start <collection script>   # see data/progress.txt for the exact cell order

# tables
python3 pipeline_benchmark3/scripts/bench3_tables.py   pipeline_benchmark3/data/results.csv
python3 pipeline_benchmark3/scripts/bench3_profiles.py pipeline_benchmark3/data/profiles
```
