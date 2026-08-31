# Project Racecar — Agent Guide #1: Marshalling Optimization

A handoff guide for an agent taking on deep marshalling optimization in the AWS SDK for Java v2.
Everything here was learned the hard way over phases 0–G; read it before running anything.

---

## 1. High-level project summary

**Goal:** incrementally optimize the SDK v2 request pipeline, one optimization per commit, each
measured against a paired baseline, results logged in `pipeline_benchmark2/project_racecar_summary.md`
(the authoritative project record — read it in full before starting).

**Branch:** `feature/poc/racecar` in `/Users/alexwoo/java/java2-repo2`. Every optimization is a
separate commit (Conventional Commits style, `perf(module): ...`). Doc updates are separate
`docs(benchmarks):` commits.

**Completed phases** (all kept, all measured):

| phase | commit | what |
|---|---|---|
| F | `d9da6c9ff0d` | SigV4 fast header signer (merge of `alexwoo/sigv4_smithy-java-opts`) |
| B1 | `b283db70db0` | Shallow header copy-on-write in `LowCopyListMap` |
| A (1–5) | `0c55ba2a691`…`b199c36972a` | Materialized-body contract: zero-copy non-streaming bodies through async publisher, Apache entity, sync wrappers |
| E1 | `b70aa6b5b45` | **Marshalling buffer sized from recent body sizes** (see §6) |
| D1 | `c01f8e84f29` | User-agent + Apache header work trimmed |
| G1 | `ac0c028febb` | Straight-line **sync** pipeline (`SyncApiCallPipeline`) |
| G2 | `ee3fb0765c2` | Straight-line **async** pipeline (`AsyncApiCallPipeline`, shared `RequestMutationStages`) |

**Cumulative results, phase 0 → G2** (paired, bare-metal host):

| client | scenario | app CPU/op | allocation |
|---|---|---:|---:|
| v2-sync | small-get / small-put | ≈ −18% | −36% / −42% |
| v2-async | small-get / small-put | ≈ −13/−14% | −31% / −37% |
| both | batch-put | ≈ −6/−7% | −49% / −70% |
| both | batch-get | ≈ −2/−3% | −4/−6% |

v2 batch-put now allocates **less than smithy-java** (0.87×/0.92×). Small ops remain 4–5× smithy's
allocation; batch-get has barely moved (response-side, needs codegen work — not your problem).

---

## 2. Repo and project layout

```
/Users/alexwoo/java/java2-repo2/            repo root (branch feature/poc/racecar)
├── core/
│   ├── sdk-core/                           pipeline, stages, handlers
│   │   └── .../core/internal/http/         SyncApiCallPipeline, AsyncApiCallPipeline,
│   │                                       RequestMutationStages, AmazonSync/AsyncHttpClient
│   ├── protocols/
│   │   ├── aws-json-protocol/              ★ MARSHALLING LIVES HERE ★
│   │   │   └── .../protocol/json/          SdkJsonGenerator, MarshallBufferSizeHints,
│   │   │       internal/marshall/          JsonProtocolMarshaller, marshaller registry,
│   │   │                                   JsonMarshallerContext, per-type marshallers
│   │   ├── protocol-core/                  SdkField, traits, MarshallingType
│   │   └── aws-cbor-protocol/, smithy-rpcv2-protocol/   share StructuredJsonFactory interfaces
│   └── sdk-core .../protocol/              SdkPojo, SdkField plumbing
├── services/dynamodb/                      generated client + model (AttributeValue etc.)
│   └── target/generated-sources/           read generated marshaller code here
├── test/standalone-e2e-benchmarks/         ★ THE MEASUREMENT HARNESS ★ (see §4)
│   ├── README.md                           read in full — methodology documented there
│   └── scripts/                            benchmark.sh, collect.sh, paired-ab.sh,
│                                           concurrency-sweep.sh, build-jar.sh,
│                                           deploy-remote.sh, remote-run.sh
├── pipeline_benchmark2/
│   ├── project_racecar_summary.md          ★ AUTHORITATIVE RECORD — update it per phase ★
│   ├── racecar_agentguide_1.md             this file
│   ├── analysis/scripts/                   paired_ab_summary.py, phase_alloc_compare.py,
│   │                                       phase_compare.py, concurrency_sweep_summary.py,
│   │                                       profile_agg.py (allocation categorizer)
│   ├── analysis/20260824-1618/report.md    original deep-dive that motivated everything
│   ├── jars/                               (gitignored) provenance-stamped benchmark jars
│   ├── paired/host-*/                      (gitignored) fetched host results
│   └── raw/, sweeps/                       (gitignored) collections and sweeps
└── .kiro/reference/smithy-java/            smithy-java source for comparison/inspiration
```

Useful comparison target: smithy-java's serde (`.kiro/reference/smithy-java`) — it's the "how small
can it be" benchmark for marshalling (its `json` allocation per op is far lower on small ops).

---

## 3. Measurement methodology — the non-negotiables

These rules exist because violating each one produced a wrong result at least once this project:

1. **Paired A/B or it didn't happen.** Arms alternate within one session (`paired-ab.sh`); arm order
   reverses on even reps. Cross-session absolute numbers drift ~2%; never compare them directly.
2. **`app_cpu_us_per_op` is the CPU metric.** Whole-process CPU (`cpu_us_per_op`) includes JIT/GC/VM
   threads and does not converge with window length (it once read 114→49 µs/op on an unchanged
   client purely from a longer window; two "findings" reversed sign when re-measured). Application
   CPU spread across window lengths is 1.3%.
3. **Respect `steady_state`.** Runs with JIT compiling inside the measured window are flagged
   `steady_state=false`; their per-op CPU is unusable. Fix: more iterations. Async is chronically
   worse at this. If in doubt, quote latency (`mean_lat_us`, robust to it) alongside CPU.
4. **Allocation is the most reproducible metric** (±0.7%) and the acceptance metric for
   allocation-shaped changes. But allocation→CPU conversion is ~1:3.7–4.1 on small ops and only
   ~1:8 on batch-put (serialization + socket dominate there). Set expectations accordingly.
5. **Include a control arm.** If your change touches only marshalling of requests, batch-get (response
   dominated) or an untouched client makes a natural control; a control reading nonzero invalidates
   the session. Both phase G runs used this and it certified them.
6. **Noise floor is measured, not assumed:** null experiment (two byte-identical jars as both arms)
   gave ±2.7% sync / ±2.5% async on the host at concurrency 1, 5 reps. A delta below that is nothing.
7. **Concurrency 1 for comparisons.** Higher concurrency costs precision (co-resident server) and
   async throughput >c4 is capped by the harness's single submitter thread (~20.5k ops/s), not CRT.
8. **Stay idle during collections.** Your own builds/git activity on the same machine corrupts runs.

---

## 4. How to collect data

### 4.1 Build a provenance-stamped jar (the unit of measurement)

```bash
cd test/standalone-e2e-benchmarks
./scripts/build-jar.sh phaseE2            # rebuilds SDK consistently, shades, archives
# → pipeline_benchmark2/jars/racecar-phaseE2-<sdk-commit>.jar
```

- Jar embeds phase label, harness commit, **sdk.commit** (the variable under test), versions.
- Baseline jar with an older SDK: check out baseline commit, install SDK, check out branch again,
  then `./scripts/build-jar.sh phaseX --skip-sdk-build --sdk-commit <baseline-sha>`.
- **Jars must share the same harness commit to be compared** — the summarizer refuses otherwise.
- If harness scripts change, REBUILD ALL JARS you intend to compare (old jars reject new flags).

### 4.2 The bare-metal host (where real numbers come from)

```bash
ssh -i /Users/alexwoo/alexwoo-ec2-us-east-1.pem ec2-user@ec2-44-203-70-75.compute-1.amazonaws.com
```

c6g.metal: Graviton2 Neoverse-N1, 64 physical cores (no SMT), 126 GiB, Amazon Linux 2023, JDK 25,
async-profiler 4.5 (`asprof`/`jfrconv` on PATH). Layout on host: `~/racecar/repo/...` mirrors the
repo enough for scripts' relative paths. If reprovisioning:
`sudo dnf install -y java-25-amazon-corretto-devel git perf` + async-profiler linux-arm64 to
`/opt/async-profiler`.

Driving it from the laptop (never hand-roll ssh/scp from the interactive shell — zsh mangles scp
targets; the bash scripts work):

```bash
export RACECAR_REMOTE_TARGET=ec2-user@ec2-44-203-70-75.compute-1.amazonaws.com
export RACECAR_REMOTE_KEY=/Users/alexwoo/alexwoo-ec2-us-east-1.pem
./scripts/deploy-remote.sh --target "$RACECAR_REMOTE_TARGET" --key "$RACECAR_REMOTE_KEY" \
    --jar ../../pipeline_benchmark2/jars/racecar-phaseE2-<sha>.jar     # scripts always refreshed
./scripts/remote-run.sh start /tmp/my_run.sh    # detached; survives disconnect
./scripts/remote-run.sh status | log | wait 1740 | fetch paired | stop
```

`wait` tops out at your shell timeout (~29 min per call); loop it. `fetch paired` pulls the newest
run's results.csv/manifest.md/summary.md into `pipeline_benchmark2/paired/host-<runid>/`. For older
runs, write a small bash script that scps file-by-file.

### 4.3 The canonical paired-run script (copy and adapt)

```bash
#!/usr/bin/env bash
set -uo pipefail
cd ~/racecar/repo/test/standalone-e2e-benchmarks
JARS=../../pipeline_benchmark2/jars
TUNE="-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4"
scripts/paired-ab.sh \
  --jars "base=$JARS/racecar-<BASE>.jar,cand=$JARS/racecar-<CAND>.jar" \
  --iterations 150000 --warmup 20000 --reps 5 \
  --clients v2-sync,v2-async --scenarios small-get,small-put \
  --concurrency 1 \
  --pin-server 0-15 --pin-client 32-47 \
  --jvm-args "$TUNE" --server-jvm-args "$TUNE"
# batch scenarios: --iterations 80000 --warmup 15000 (25k was too short: async never got steady)
```

Pinning and `TUNE` are mandatory: JVM defaults on 64 cores are 18 compiler threads / 43 GC threads.
This exact configuration is the one whose noise floor was measured — a different one has no floor.

Summarize locally:
`python3 pipeline_benchmark2/analysis/scripts/paired_ab_summary.py pipeline_benchmark2/paired/host-<runid>`

### 4.4 Allocation profiles (mechanism verification)

```bash
scripts/benchmark.sh --jar <jar> --client v2-sync --scenario batch-put \
  --iterations 30000 --warmup 5000 --warmup-mode fixed --progress-seconds 0 \
  --profile alloc --profile-format jfr --profile-file /tmp/alloc.jfr
jfrconv --alloc --total -o collapsed /tmp/alloc.jfr /tmp/alloc.collapsed
```

`analysis/scripts/profile_agg.py` categorizes collapsed stacks (`marshall`, `json`, `signing`,
`pipeline-framework`, …; excludes `benchmark-harness`, JIT, GC). `phase_alloc_compare.py
LABEL=RUNDIR LABEL2=RUNDIR2` diffs whole collections (needs `collect.sh` output with alloc.jfr per
case; divide by warmup+iterations ops). Always verify the mechanism (frames gone / bytes moved), not
just the topline.

### 4.5 Local (Mac) quick iteration

Fine for correctness smoke tests and allocation profiles; ±2.0% sync CPU at best, async ±11% —
don't make CPU claims from the laptop. `scripts/benchmark.sh` works identically minus `--pin-*`.

---

## 5. Build recipes and gotchas (each cost us real time)

- **Consistent SDK build (the ONLY safe install command):**
  ```bash
  mvn clean install -pl ':dynamodb,:apache-client,:apache5-client,:aws-crt-client,!:codegen-maven-plugin' \
      --am -P quick -Dmaven.test.skip=true
  ```
  Installing a single module desyncs `~/.m2` → `VerifyError: AwsAdvancedClientOption is not
  assignable to AttributeMap$Key` at runtime. `codegen-maven-plugin` is excluded (its descriptor
  goal fails under JDK 25, "class file major version 61").
- **`~/.m2` state is invisible until it bites.** After building a baseline jar, `~/.m2` holds the
  BASELINE SDK; building `:sdk-core` alone then fails against stale `http-client-spi`. Re-run the
  consistent build after any checkout dance.
- **Module tests:** `cd core/protocols/aws-json-protocol && mvn test` (fast). Full
  `core/sdk-core mvn test`: 1,534 tests; ONE known flake:
  `HttpClientApiCallTimeoutTest.errorResponse_SlowErrorResponseHandler_ThrowsApiCallTimeoutException`
  (~1-in-5 on unmodified tree; re-run before blaming your change).
- **Test scripts with literal strings "port"/"host" in `execute_bash` commands** can trip tool
  guards — write scripts to a file and run the file.
- Transport is pinned in the harness (`v2-sync`=Apache5, `v2-async`=CRT). Never let classpath
  resolution choose — the priority table chooses silently.
- IDE autobuild corrupts `target/` (bogus spotbugs findings, missing enum methods). `.vscode/settings.json`
  already disables it; leave that alone.
- Metric-set identity check is a cheap correctness probe: `--metrics` output must list the same 11
  METRIC names before and after your change.

---

## 6. Marshalling: current state and where the work is

### What's already done (don't redo)

- **E1 (`b70aa6b5b45`):** output-buffer sizing. `MarshallBufferSizeHints` (per client, per operation)
  predicts body size; `SdkJsonGenerator` allocates once instead of doubling from 1 KB. This removed
  the buffer-growth garbage: json category on sync batch-put 117,188 → 38,175 B/op. Feedback path:
  `JsonProtocolMarshallerBuilder.marshalledSizeReporter`; factory hook:
  `StructuredJsonFactory.createWriter(contentType, initialBufferCapacity)`.
- **A + E1 combined = marshal-once, copy-zero:** the marshalled byte[] flows to the wire without
  copies (SimpleHttpContentPublisher, Apache entity, sync wrappers all fixed).

### What remains — E part 2, the field loop (your target)

`JsonProtocolMarshaller` walks `pojo.sdkFields()` per request; per field it allocates iterators,
probes traits (`LocationTrait` etc.), boxes, looks up marshallers in the registry by
location+type, and recurses through `AttributeValue`'s union-ish structure. Measured remnants:

- `marshall` category ≈ **36 KB/op on sync batch-put** (post-E1): `sdkFields()` ArrayList iterators,
  `JsonMarshallerContext` per call, registry lookups, `SdkField` trait probes per field per item
  (25 items × ~12 attrs each), String/char[] churn in field-name writes.
- On small ops marshalling is ~10 µs of a ~160 µs call (`MarshallingDuration` metric) — small-op
  headroom is limited; **batch-put/batch-get request marshalling is where the meat is**.
- CPU conversion warning: batch-put CPU is dominated by serialize+socket. E1's 43% allocation cut
  converted to only ~6% CPU. To move CPU you must reduce *work in the serializer itself* (fewer
  branches/lookups per field), not just allocation. JMH the marshaller in isolation for CPU claims
  (recipe: classpath from `mvn dependency:build-classpath` + jmh-core; prior art in /tmp is gone,
  but AwsV4HttpSignerBenchmark from phase F shows the pattern in git history).

Ideas on the table (unvalidated, in rough value order):
1. Per-operation marshalling "plan" cached on first use: resolve field→marshaller once per
   (operation, field) instead of per request; array-walk instead of iterator.
2. `AttributeValue` fast path: it's a 10-way union marshalled through generic SdkPojo machinery;
   a dedicated switch would skip trait probes entirely (needs codegen or handwritten special case —
   codegen changes are heavier; check `services/dynamodb/target/generated-sources` marshallers first).
3. Field-name bytes: pre-encoded UTF-8 name constants (Jackson `SerializableString` equivalent)
   instead of String encode per write.
4. Registry lookup: `MarshallerRegistry` does layered map lookups per field; flatten per plan.

Comparison target: smithy-java generates direct serializer code per shape (see
`.kiro/reference/smithy-java`), no per-field reflection-ish dispatch — that's why its small-op json
cost is tiny. The question for E2 is how close V2 can get without breaking the SdkField contract.

### Constraints

- `SdkField`/`SdkPojo`/traits are **public API** — additive changes only.
- CBOR (`aws-cbor-protocol`) and RPCv2 share the marshaller machinery; keep them compiling
  (E1's pattern: default methods on shared interfaces, other protocols ignore them).
- Behavior must be byte-identical on the wire. The mock server doesn't validate request bodies —
  add unit tests comparing marshalled output old-vs-new (exact bytes) for representative shapes,
  and run against DynamoDB Local if in doubt (`--endpoint`, see harness README).
- Generated-code changes (codegen module) currently can't be rebuilt under JDK 25 (plugin issue);
  prefer runtime-library changes, or hand-verify generated-code implications.

---

## 7. Working protocol

1. Read `project_racecar_summary.md` (methodology sections at top; phase E section; conversion-ratio
   analysis under "First credible CPU numbers").
2. One optimization per `perf(...)` commit; build module tests green before committing; record
   results in the summary doc as a separate `docs(benchmarks):` commit, including raw run dirs,
   deltas ± spread, wins/N, steady counts, and a stated control.
3. Sequence per candidate: implement → module tests → consistent SDK build → local smoke
   (all 4 scenarios + metrics identity) → build jar → deploy → paired run vs predecessor jar
   (5 reps, both clients, control identified up front) → allocation profile mechanism check →
   summary doc update.
4. Predecessor jar for your first comparison: `racecar-phaseG2-ee3fb0765c2.jar` (in
   `pipeline_benchmark2/jars/` and already on the host).
5. If a result looks surprising in EITHER direction, re-measure at a different window length before
   believing it. Two plausible findings died that way this project; one experiment ran against a
   stale server from an old jar and was caught only by the `server_requests` cross-check.
6. Do not commit anything under `pipeline_benchmark2/{raw,jars,paired,sweeps}` (gitignored) —
   numbers go in the summary doc with pointers to run dirs.
