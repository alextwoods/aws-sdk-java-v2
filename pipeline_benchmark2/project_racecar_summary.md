# Project Racecar — incremental SDK pipeline optimization

Running log of the optimization phases applied to the AWS SDK for Java v2 request pipeline on
`feature/poc/racecar`, with measured results per phase.

The end goal is options A–F from the pipeline analysis, finishing with G: collapsing the
composable stage chain into a straight-line `ClientPipeline`-style call sequence. Phases are
applied as a **stack** — each measurement includes all preceding phases — so the deltas below are
cumulative unless stated otherwise.

Source of truth for measurement: `test/standalone-e2e-benchmarks` via
`./scripts/collect.sh --clients v2-sync,v2-async`, 200,000 measured ops + 20,000 warmup, 3
interleaved timing reps, out-of-process canned-response mock DynamoDB server.

---

## Measurement methodology (read this before trusting any number below)

**Allocation is the primary signal. CPU time from the e2e harness is currently not usable for
changes of this size.**

Allocation per operation (async-profiler `alloc --total`, bytes) reproduces to within 0.7% of the
[earlier deep-dive report](analysis/20260824-1618/report.md) on the same hardware:

| case | report §5.2 | phase-0 baseline here | agreement |
|------|------------:|----------------------:|----------:|
| v2-sync small-get | 60,963 | 61,387 | 0.7% |
| v2-sync batch-put | 205,292 | 204,272 | 0.5% |
| v2-async small-get | 69,509 | 69,702 | 0.3% |
| v2-async batch-put | 372,456 | 371,884 | 0.2% |

CPU time does not. Rep-to-rep spread in this environment is **6–31%**, against the 2–6% the
original report achieved on the same box. Two causes, one of them mine:

1. **Self-inflicted (fixed):** the first baseline attempt ran while I executed a `git merge` on the
   repo. Two of eight cases showed 2–6× inflated wall time and doubled CPU. That collection was
   discarded (`raw/DISCARDED-phase0-baseline-contended/`) and re-run with the machine idle.
2. **Environmental (not fixed):** even idle, spread stayed at 6–31%. The machine is a developer
   workstation with an IDE and other applications resident; the original report's run was
   evidently quieter. A 5–15% CPU improvement is below this noise floor.

Consequences for how phases are judged:

- **Allocation deltas** from the e2e harness are treated as authoritative.
- **CPU-time claims about a specific component** come from a JMH microbenchmark of that component,
  not from the e2e harness.
- **e2e CPU/wall numbers** are recorded for completeness with their spread shown, and are called
  inconclusive whenever the delta is smaller than the spread. They are *not* used to accept or
  reject a phase.

To make e2e CPU time usable later, the environment needs to be quiesced (dedicated host, no IDE,
ideally core-pinned client and server) or the rep count raised substantially. Worth doing before
phase G, whose payoff is CPU-shaped rather than allocation-shaped.

Secondary caveat: `SigningDuration` and friends from the `--metrics` runs are single-run, not
repped, and inherit the same noise. Phase F's `SigningDuration` moved by −24% to +53% depending on
scenario — inconsistent in *sign*, i.e. measuring nothing. Ignore those rows.

### Build provenance

The benchmark module resolves the SDK from `~/.m2`, so each phase requires installing the changed
modules. Two wrinkles worth recording:

- Building any *service* module hits
  `maven-plugin-plugin:3.6.0:descriptor ... Unsupported class file major version 61` on
  `codegen-maven-plugin` under JDK 25. Avoided by installing only the non-generated modules that
  actually change (`:sdk-core`, `:http-auth-aws`, `:http-client-spi`, …) and leaving the already
  installed generated `dynamodb` artifact in place. All phase changes are internal, so the
  pre-built `dynamodb` jar stays compatible; the benchmark exercises real DynamoDB calls, so an
  incompatibility would fail loudly rather than silently.
- Switching branches leaves stale `target/classes` that poison compilation
  (`cannot access SdkBuilder`, `cannot find symbol` in unrelated modules). `mvn clean install` on
  the reduced module set is required after a branch switch.

---

## Phase 0 — baseline

- Commit: `10f88f7bffd` (pre-merge tip of `feature/poc/racecar`)
- Raw data: `raw/phase0-baseline/20260827-1134/`

Allocation, bytes/op (client code only):

| client | small-get | small-put | batch-get | batch-put |
|--------|----------:|----------:|----------:|----------:|
| v2-sync | 61,387 | 54,509 | 533,097 | 204,272 |
| v2-async | 69,702 | 61,797 | 742,955 | 371,884 |

v2-sync small-get by category — the four subsystems the analysis called out:

| category | bytes/op |
|----------|---------:|
| pipeline-framework | 25,356 |
| signing | 20,905 |
| unmarshall | 7,142 |
| json | 3,334 |
| retry | 1,544 |
| endpoint-rules | 1,497 |
| marshall | 1,246 |
| crypto | 338 |

e2e timing (mean of 3 reps, user-CPU ops/s, with spread):

| client | small-get | small-put | batch-get | batch-put |
|--------|----------:|----------:|----------:|----------:|
| v2-sync | 23,655 (7.9%) | 24,386 (26.3%) | 5,813 (28.2%) | 4,790 (11.2%) |
| v2-async | 19,558 (7.9%) | 18,792 (31.5%) | 5,727 (6.2%) | 5,123 (13.4%) |

---

## Phase F — SigV4 fast-path header signer

- Commit: `d9da6c9ff0d` — merge of `alexwoo/sigv4_smithy-java-opts`
- Raw data: `raw/phaseF-signer/20260827-1223/`
- Analysis: `analysis/racecar/alloc-phaseF.md`, `analysis/racecar/timing-phaseF.md`

### What changed

A fast path for header-based SigV4 signing, modelled on smithy-java's `SigV4Signer`:

- `FastV4HeaderSigner` runs the whole SigV4 algorithm in one method instead of composing
  `Checksummer` → `V4RequestSigner` → `V4PayloadSigner`. Streams the body through a pooled
  `MessageDigest` (no `ChecksumInputStream` + per-call `byte[4096]`), builds the canonical request
  and string-to-sign directly into pooled ASCII byte buffers (no `String.getBytes(UTF_8)`), keeps
  headers in a strided `String[]` sorted in place, and applies the signer-managed headers in a
  single builder pass at the end.
- `V4SigningResources` + `Pool`: bounded (32) lock-free pool of `MessageDigest`, `Mac`,
  `StringBuilder` and byte-buffer scratch, reset on acquire and cleared on release.
- `V4SigningKeyCache`: shared bounded (300-entry, LRU) `(secret, region, service)` → signing-key
  cache with a precomputed-hashCode key, replacing a per-lookup `String` key. Same caching posture
  as the `FifoCache<SignerKey>` it replaces — no new secret-material exposure.
- `DefaultAwsV4HttpSigner` dispatches to the fast path only for header-auth SigV4; presigning,
  query auth, flexible checksums, chunk encoding, event streams, aws-chunked trailers and
  anonymous credentials all keep the legacy pipeline.

### Correctness

`FastV4HeaderSignerTest` asserts byte-identical `Authorization`, `X-Amz-Date` and
`X-Amz-Content-Sha256` against `signLegacyPath` across 11 shapes (no body, query parameters,
whitespace in header values, ignored headers, session credentials, payload signing disabled,
multi-valued headers, mixed-case names, non-standard port, nested path). Full `mvn install` on
`:http-auth-aws`: 268 JUnit + 166 TestNG pass, checkstyle and spotbugs clean.

### Isolated signer measurement (JMH, low noise)

`AwsV4HttpSignerBenchmark`, 2 forks × (3×2 s warmup + 5×2 s measurement):

| | legacy path | fast path | delta |
|---|---:|---:|---:|
| time | 2,253.1 ± 83.4 ns/op | 1,323.8 ± 21.8 ns/op | **−41.2%** |
| allocation | 16,552 ± 13 B/op | 2,608 ± 25 B/op | **−84.2%** |

Non-overlapping error bars. This is the authoritative CPU-time claim for phase F.

### End-to-end allocation (authoritative)

Total bytes/op, client code:

| client | scenario | baseline | phase F | delta |
|--------|----------|---------:|--------:|------:|
| v2-sync | small-get | 61,387 | 44,922 | **−26.8%** |
| v2-sync | small-put | 54,509 | 37,803 | **−30.6%** |
| v2-sync | batch-get | 533,097 | 517,121 | −3.0% |
| v2-sync | batch-put | 204,272 | 191,024 | −6.5% |
| v2-async | small-get | 69,702 | 55,184 | **−20.8%** |
| v2-async | small-put | 61,797 | 46,759 | **−24.3%** |
| v2-async | batch-get | 742,955 | 725,353 | −2.4% |
| v2-async | batch-put | 371,884 | 356,448 | −4.2% |

Batch scenarios move less because marshalling and unmarshalling dominate their allocation; signing
is a fixed per-call cost, so its removal shows up proportionally largest on small operations.

By category, v2-sync small-get:

| category | baseline | phase F | delta |
|----------|---------:|--------:|------:|
| signing | 20,905 | 5,607 | **−73.2%** |
| crypto | 338 | 105 | −69.0% |
| endpoint-rules | 1,497 | 1,261 | −15.8% |
| pipeline-framework | 25,356 | 24,680 | −2.7% |
| unmarshall | 7,142 | 7,154 | +0.2% |

Allocation sites that went to zero (v2-sync small-get, bytes/op):

| site | baseline | phase F |
|------|---------:|--------:|
| `ChecksumUtil.lambda$readAll$0` (per-request 4 KiB body-drain buffer) | 4,020 | **0** |
| `V4CanonicalRequest.getCanonicalHeadersString` (2 KiB StringBuilder + String) | 2,579 | **0** |
| `V4RequestSigner.lambda$header$0` | 1,203 | **0** |

The e2e `signing` category (5,607 B/op remaining) is larger than the isolated signer's 2,608
because it also covers `SigningStage` plumbing and auth-scheme resolution, which phase F does not
touch.

### End-to-end CPU (inconclusive — recorded for completeness)

user-CPU ops/s delta vs baseline, with baseline rep spread:

| client | scenario | delta | baseline spread |
|--------|----------|------:|----------------:|
| v2-sync | small-get | −11.8% | 7.9% |
| v2-sync | small-put | +8.0% | 26.3% |
| v2-sync | batch-get | +3.8% | 28.2% |
| v2-sync | batch-put | −9.6% | 11.2% |
| v2-async | small-get | −16.7% | 7.9% |
| v2-async | small-put | +4.1% | 31.5% |
| v2-async | batch-get | −6.3% | 6.2% |
| v2-async | batch-put | −5.0% | 13.4% |

Deltas of both signs at magnitudes at or below the spread: this is noise. Given the JMH result
(−41% on a component worth ~4 µs of a ~78 µs sync call, i.e. ~2% of end-to-end CPU) and a −27%
allocation reduction, no e2e CPU change of this size would be resolvable here anyway.

### Verdict

**Accepted.** −73% signing allocation, −27 to −31% total allocation on small operations, −41% and
−84% on the isolated signer, byte-identical output, full test suite green.

### Follow-up identified

`CollectionUtils.lambda$deepCopyMap$1` is still 1,756 B/op after phase F (was 2,402). The fast
signer still does `source.toBuilder()` → 5 × `putHeader` → `build()`, and the first `putHeader`
after the builder/buildable share triggers a full `deepCopyMap` (TreeMap + one `ArrayList` per
header). That is exactly what phase B targets.

---

## Phase B (part 1) — shallow header copy-on-write

- Commit: `b283db70db0`
- Raw data: `raw/phaseB-mutability/20260827-1754/`
- Analysis: `analysis/racecar/timing-phaseB.md`

### What was intended vs what was done

The plan was "move the immutability barrier to after signing" so the signer mutates the request in
place instead of round-tripping through a builder. **That turned out to be blocked**, so this phase
delivers the other half of option B instead. The blocker is worth recording:

`MakeRequestImmutableStage` sits *outside* the retry loop, and `RetryableStage` re-executes
`SigningStage` with the same input on every attempt. If that input became a mutable builder, attempt
2 would be handed the builder already carrying attempt 1's `Authorization`, `X-Amz-Date` and
`X-Amz-Content-Sha256`. `authorization` is **not** in either signer's canonicalization ignore list
(`V4CanonicalRequest.HEADERS_TO_IGNORE_IN_LOWER_CASE` at `V4CanonicalRequest.java:46`, mirrored in
`FastV4HeaderSigner.IGNORED_HEADERS_LOWERCASE`), so the stale `Authorization` would be folded into
the canonical request and every retry would be signed wrongly.

smithy-java avoids this deliberately — its `isIgnoredHeader` excludes `authorization` with the
comment that "ignoring it keeps re-signing a reused request idempotent". V2 gets away without that
exclusion today only because the request handed to the signer is freshly derived from an immutable
pre-signing request on every attempt.

So moving the barrier requires a prerequisite: **make re-signing idempotent** by excluding the
signer-managed headers from canonicalization. That is a signing-behavior change needing its own
justification and test matrix (including a real multi-attempt retry test), so it is deliberately
not bundled here. Tracked as phase B part 2 below.

### What changed

`LowCopyListMap` shares its map between a builder and the object it builds, and deep-copied on the
first write after sharing — a new `TreeMap` **plus a new `ArrayList` per header**. That copy runs
once per signing, per attempt.

Most mutations don't need the value lists copied: `putHeader`/`putRawQueryParameter` replace an
entry's list wholesale, and `remove`/`clear` only touch the map. Only `appendHeader`/
`appendRawQueryParameter` mutate an existing list in place. The copy-on-write is now split
accordingly:

- `forInternalWrite()` — copies the map only, leaving value lists shared (put, remove, clear).
- `forInternalWriteWithListMutation()` — also privatizes the value lists (the append mutators).

Two share flags are tracked rather than one, because a shallow map copy leaves the lists shared: a
put followed by an append still has to privatize them.

### Correctness

No behavior change is intended, and the risk is entirely aliasing, so the tests target that:

- `LowCopyListMapTest` (14 tests) — the storage layer: put/replace/remove/clear/append after
  sharing, append-after-put, two builders from one buildable, external-map ownership.
- `SdkHttpRequestResponseAliasingTest` (11 tests) — the same contract through the public
  `SdkHttpFullRequest`/`SdkHttpFullResponse` builder API, which is what catches a mutator wired to
  the wrong path.

Mutation-tested: reverting `appendHeader` to the shallow path fails 3 of the new tests, while the
pre-existing `SdkHttpRequestResponseTest` stays green — i.e. without the new tests this bug would
have shipped.

`http-client-spi`: 91 tests pass, checkstyle and spotbugs clean. `http-auth-aws`: 268 JUnit + 166
TestNG pass. **Not verified:** `sdk-core`'s suite could not be run — 136 test classes fail
identically with and without this change (`ObjenesisException` from Mockito under JDK 25 in a
partial reactor), so it is environmental, but it does mean sdk-core coverage is currently missing
for these phases. Worth fixing before phase G.

### Allocation (authoritative), phase F → phase B

| client | scenario | phase F | phase B | delta |
|--------|----------|--------:|--------:|------:|
| v2-sync | small-get | 44,922 | 44,421 | −1.1% |
| v2-sync | small-put | 37,803 | 37,520 | −0.8% |
| v2-sync | batch-get | 517,121 | 515,968 | −0.2% |
| v2-sync | batch-put | 191,024 | 187,566 | −1.8% |
| v2-async | small-get | 55,184 | 52,753 | **−4.4%** |
| v2-async | small-put | 46,759 | 45,401 | −2.9% |
| v2-async | batch-get | 725,353 | 725,008 | −0.0% |
| v2-async | batch-put | 356,448 | 355,498 | −0.3% |

The targeted site is gone completely, but is partly replaced:

| site (v2-sync small-get) | phase F | phase B |
|--------------------------|--------:|--------:|
| `CollectionUtils.lambda$deepCopyMap$1` | 1,756 | **0** |
| `LowCopyListMap.shallowCopyMap` | – | 925 |

So the per-header `ArrayList` allocations are eliminated (−1,756 B/op) and replaced by a map-only
copy (+925 B/op), for a net ~830 B/op — consistent with the ~500 B/op measured at the total level
once category re-attribution is accounted for. **The remaining 925 B/op is the `TreeMap` and its
`Entry` nodes**, which this change cannot remove: eliminating it needs either the barrier move
(part 2) or a strided-array header representation like smithy-java's `ArrayHttpHeaders`.

Async benefits ~4× more than sync (−4.4% vs −1.1% on small-get), with `pipeline-framework` down
7.7% and `retry` down 10.4% — the async path does more builder round-trips per call, so it was
paying the copy more often.

### Internal control for the rebuild

Phase B required rebuilding `dynamodb` (see below), so untouched allocation categories were checked
against phase F to confirm the two builds are comparable: `unmarshall` 7,154 → 7,157 (+0.0%),
`marshall` 1,268 → 1,258 (−0.8%), `pipeline-framework` 24,680 → 24,768 (+0.4%). The build sets are
equivalent, so the deltas above are attributable to the change.

### e2e CPU (inconclusive, as before)

user-CPU ops/s vs the phase 0 baseline: v2-async small-get −9.7%, small-put +3.8%, batch-get +1.2%,
batch-put −2.7%, against baseline spreads of 6–31%. Still noise-dominated; not used for acceptance.

### Verdict

**Accepted, but a small win.** Removes the header-list deep copy entirely and is a prerequisite for
cleaner header handling later, but nets only ~1% of total allocation on sync and ~4% on async. The
larger prize in this area is still on the table.

### Follow-ups identified

1. **Phase B part 2 — move the barrier.** Requires excluding signer-managed headers from
   canonicalization first, to make re-signing idempotent. Needs a multi-attempt retry test.
2. **Header storage.** The residual 925 B/op per copy plus
   `Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` (1,268 B/op restating headers for
   Apache) and `DefaultSdkHttpFullRequest$Builder.putHeader` (841 B/op) all point at the
   `Map<String, List<String>>` representation itself.

### Environment issue hit during this phase (and how it was resolved)

The first phase B collection produced **48/48 failed runs** with
`VerifyError: AwsAdvancedClientOption is not assignable to AttributeMap$Key`. Cause: while debugging
an unrelated build failure I rebuilt and installed `utils` on its own, desynchronizing `~/.m2` — the
previously installed `dynamodb` and `aws-core` jars had been compiled against a different
`AttributeMap`. The baseline and phase F runs were unaffected because all their jars were mutually
consistent.

Resolution: rebuild the full set consistently, excluding only the module that cannot build under
JDK 25:

```bash
mvn clean install -pl ':dynamodb,:apache-client,:aws-crt-client,!:codegen-maven-plugin' \
    --am -P quick -Dmaven.test.skip=true
```

`codegen-maven-plugin` is excluded from the reactor and resolved from `~/.m2` instead (its source is
unchanged); this sidesteps the `maven-plugin-plugin:3.6.0:descriptor` /
`Unsupported class file major version 61` failure. **This is now the standard build command for
every subsequent phase** — always install a mutually consistent set, and smoke-test with
`./scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 300` before starting a
collection.

---

## Cumulative result so far (phase 0 → phase B)

Allocation, bytes/op, client code:

| client | scenario | phase 0 | phase B | total delta |
|--------|----------|--------:|--------:|-----------:|
| v2-sync | small-get | 61,387 | 44,421 | **−27.6%** |
| v2-sync | small-put | 54,509 | 37,520 | **−31.2%** |
| v2-sync | batch-get | 533,097 | 515,968 | −3.2% |
| v2-sync | batch-put | 204,272 | 187,566 | −8.2% |
| v2-async | small-get | 69,702 | 52,753 | **−24.3%** |
| v2-async | small-put | 61,797 | 45,401 | **−26.5%** |
| v2-async | batch-get | 742,955 | 725,008 | −2.4% |
| v2-async | batch-put | 371,884 | 355,498 | −4.4% |

For reference, smithy-java on the same workloads allocates 10,062 B/op (small-get) and
121,708 B/op (batch-put). v2-sync small-get has gone from 6.1× to 4.4× smithy; batch-put from
1.68× to 1.54×.

### Next targets, ranked by current allocation (v2-sync small-get, bytes/op)

Straight from the phase B profile, so this is where the remaining headroom actually is:

| bytes/op | site | option |
|---------:|------|--------|
| 4,161 | `org/apache/hc/core5 InputStreamEntity.writeTo` | A — body materialized once, written without a stream copy |
| 1,597 | `AttributeMapCopier.lambda$copy$0` | D — generated response copier re-copying parser output |
| 1,268 | `Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | B/header storage |
| 1,015 | `ExecutionAttributes.<init>` (`IdentityHashMap(64)`) | D — typed dense-key attribute store |
| 941 | `SdkByteArrayOutputStream.<init>` | E — size the marshalling buffer from a per-operation hint |
| 939 | `DefaultAuthSchemeOption$BuilderImpl.<init>` | D — cache the constant auth-scheme option per client |
| 925 | `LowCopyListMap.shallowCopyMap` | B part 2 / header storage |
| 841 | `DefaultSdkHttpFullRequest$Builder.putHeader` | header storage |

Option A is the single largest remaining item and is the one that generalizes the
`SimpleHttpContentPublisher` fix into a pipeline-wide "the body is already bytes" contract, so it is
the natural next phase.
