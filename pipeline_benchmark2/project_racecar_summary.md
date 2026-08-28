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

---

## Phase A — materialized-body contract (zero-copy non-streaming bodies)

- Commits: `0c55ba2a691` (contract + producers + async publisher), `8a018f4b6dc` (signer hashes the
  buffer), `0a662b24412` (Apache ByteArrayEntity), `3add9f48b8b` (sync metrics wrapper propagates),
  `b199c36972a` (BaseClientHandler + StreamManagingStage wrappers propagate)
- Raw data: `raw/phaseA-final/20260828-0112/` (an intermediate run after parts 1–3 is at
  `raw/phaseA-body/20260827-2254/` and `raw/phaseA-body-p4/20260828-0000/`)
- Analysis: `analysis/racecar/alloc-phaseA-cumulative.md`, `analysis/racecar/timing-phaseA.md`

### What changed

The generalization of the earlier `SimpleHttpContentPublisher` fix into a pipeline-wide contract.
Non-streaming bodies are marshalled into memory but carried behind `ContentStreamProvider`, whose
only accessor is `newStream()` — so every consumer re-buffered the stream.

New contract: `ContentStreamProvider.contentAsByteBufferOrNull()`, a default method returning the
content as a `ByteBuffer` when it is already in memory, else `null` (callers fall back to
`newStream()`). Producers: the JSON marshaller's buffer, `fromByteArrayUnsafe`, and
`QueryParametersToBodyStage`. Consumers: the async request publisher (zero-copy `duplicate()`
views), the fast SigV4 signer (hashes the buffer directly, one less full-body traversal per
attempt), and the Apache5 sync client (single-write `ByteArrayEntity` instead of
`InputStreamEntity`'s 4 KiB copy loop, gated on exact Content-Length match so wire framing is
unchanged).

### The lesson of parts 4 and 5: wrappers eat contracts

Parts 1–3 measured **+0–1.5% on sync — no change**. Runtime tracing (temporary debug output in the
gate) showed the provider reaching Apache was a three-deep wrapper chain, each layer hiding the
buffer:

```
TrackingContentStreamProvider          (MakeHttpRequestStage, write metrics)
  -> ClosingStreamProvider             (StreamManagingStage, stream close management)
    -> BaseClientHandler lambda        (length enforcement, round-trips through interceptor context)
      -> SingleBufferContentStreamProvider   <- the buffer, unreachable
```

Every wrapper had to learn to propagate the contract (parts 4–5). The async path was also affected:
the `BaseClientHandler` lambda had silently downgraded async from true zero-copy to the sized-copy
fallback. This is a structural observation worth carrying into phase G: **an optional capability on
an interface is only as good as the least-aware wrapper in the chain.** Any future contract of this
kind either needs a wrapper-audit like this one, or the pipeline needs fewer wrappers — which is
exactly what the straight-line pipeline (option G) buys.

Verification per part: an allocation-profile probe (22k-op sync small-put) showed
`InputStreamEntity.writeTo` at ~86 MB after parts 1–3 and **0 bytes** after part 5.

### Correctness

- `SimpleHttpContentPublisherTest` (24 tests): zero-copy fast path (stream never opened), provider
  reuse across attempts, short/long stream vs Content-Length, partial reads, cap overflow,
  demand/cancel semantics, stream never closed.
- `FastV4HeaderSignerTest` +2: byte-equivalence of the buffer-hashing path (and empty-buffer path)
  against the legacy signing pipeline.
- `ApacheHttpRequestFactoryTest` +5: ByteArrayEntity engagement, repeatability across two writes,
  and fallback on length mismatch / missing length / chunked encoding / plain stream provider.
- Suites: sdk-core 1,534, http-auth-aws 434, apache5-client 14/14 factory tests, http-client-spi +
  aws-json-protocol 125. All pass.
- Flake note: `HttpClientApiCallTimeoutTest.errorResponse_SlowErrorResponseHandler_*` fails ~1-in-5
  in isolation on the **unmodified** tree as well (timing-sensitive 1s timeout vs slow handler
  race); unrelated to these changes.

### Allocation (authoritative), phase B → phase A

| client | scenario | phase B | phase A | delta |
|--------|----------|--------:|--------:|------:|
| v2-sync | small-get | 44,421 | 39,109 | **−12.0%** |
| v2-sync | small-put | 37,520 | 33,514 | **−10.7%** |
| v2-sync | batch-get | 515,968 | 514,464 | −0.3% |
| v2-sync | batch-put | 187,566 | 184,380 | −1.7% |
| v2-async | small-get | 52,753 | 49,240 | −6.7% |
| v2-async | small-put | 45,401 | 39,383 | **−13.3%** |
| v2-async | batch-get | 725,008 | 719,408 | −0.8% |
| v2-async | batch-put | 355,498 | 192,275 | **−45.9%** |

`InputStreamEntity.writeTo`: 4,161 → **0** B/op on every sync scenario. Async batch-put's
`pipeline-framework` category (which contained the old `IoUtils.toByteArray` re-copy) went
176,218 → 50,043 B/op after parts 1–3 and further down with true zero-copy in part 5.

Sync batch-put barely moves because its allocation is dominated by the marshalling buffer growth
chain (`json` + `marshall` ≈ 155 KB/op) — that is option E's target, not A's.

### e2e CPU (still noise-dominated, but now positive across the board)

v2-sync user-CPU ops/s vs baseline: small-get +9.0%, small-put +9.3%, batch-get +3.8%, batch-put
+1.8% — first phase where every sync scenario shows positive, though spreads of 8–28% keep this
inconclusive as evidence.

### Verdict

**Accepted. Biggest phase so far.** Option A delivered exactly what the analysis predicted: the
body is written once at marshalling and never copied again by the framework on the common path —
sync writes it straight from the buffer, async publishes views of it, and the signer hashes it in
place.

---

## Cumulative result (phase 0 → F → B → A)

Allocation, bytes/op, client code:

| client | scenario | phase 0 | phase A | total delta | vs smithy-java |
|--------|----------|--------:|--------:|-----------:|---------------:|
| v2-sync | small-get | 61,387 | 39,109 | **−36.3%** | 3.9× (was 6.1×) |
| v2-sync | small-put | 54,509 | 33,514 | **−38.5%** | 4.8× (was 7.8×) |
| v2-sync | batch-get | 533,097 | 514,464 | −3.5% | 2.3× |
| v2-sync | batch-put | 204,272 | 184,380 | −9.7% | 1.51× (was 1.68×) |
| v2-async | small-get | 69,702 | 49,240 | **−29.4%** | 4.9× |
| v2-async | small-put | 61,797 | 39,383 | **−36.3%** | 5.6× |
| v2-async | batch-get | 742,955 | 719,408 | −3.2% | 3.3× |
| v2-async | batch-put | 371,884 | 192,275 | **−48.3%** | 1.58× (was 3.06×) |

(smithy-java reference values from the 20260824-1618 report: small-get 10,062, small-put 6,977,
batch-get 219,894, batch-put 121,708 B/op.)

### Next targets (from the phase A profile, v2-sync small-get)

| bytes/op | site | option |
|---------:|------|--------|
| 1,594 | `AttributeMapCopier.lambda$copy$0` | D — response copier re-copying parser output |
| 1,404 | `Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | header restatement for Apache |
| 1,146 | `ExecutionAttributes.<init>` (`IdentityHashMap(64)`) | D — typed dense-key attribute store |
| 1,107 | `AttributeValue.builder` + `build` (2,296 combined) | batch-get driver; D |
| 1,078 | `SdkByteArrayOutputStream.<init>` | E — size marshalling buffer from a per-op hint |
| 995 | `LowCopyListMap.shallowCopyMap` | B part 2 / header storage |
| 990 | `DefaultAuthSchemeOption.<init>` + builder | D — cache constant auth option per client |
| 990 | `DefaultSdkHttpFullRequest$Builder.putHeader` | header storage |

Remaining big structural items: **E** (marshalling: buffer sizing + straight-line field loop —
the whole batch-put story), **D** (framework: attributes, metric-stage eliding, auth-option
caching, response copiers), **C** (de-future the async request path), then **G**.

---

## Phase E (part 1) — marshalling buffer sized from recent body sizes

- Commit: `b70aa6b5b45`
- Raw data: `raw/phaseE-marshal/20260828-0225/`
- Analysis: `analysis/racecar/alloc-phaseE-cumulative.md`

### What changed

`SdkJsonGenerator` allocated its output buffer at a fixed 1 KB and grew to the body size by
doubling — a 50 KB batch-put body allocates ~127 KB of cumulative garbage per request
(1+2+4+…+64 KB), the dominant allocation on write-heavy workloads.

New `MarshallBufferSizeHints` (one per protocol factory, i.e. per client) tracks recently observed
marshalled-body sizes per operation and the generator allocates the buffer at that size up front.
The hint grows immediately on a larger body and decays by 1/8th of the gap per smaller observation
(with a floor step of 1, so integer division can't stall it above a smaller steady state — caught
by a unit test on the first attempt at the formula). Clamped to [1 KB, 128 KB]; above 128 KB the
buffer switches to chunked storage anyway.

Plumbing: `StructuredJsonFactory.createWriter(contentType, initialBufferCapacity)` default-method
overload (CBOR/RPCv2 factories ignore it, unchanged); the marshaller reports the final size back
via `JsonProtocolMarshallerBuilder.marshalledSizeReporter`.

### Correctness

`MarshallBufferSizeHintsTest` (8 tests): growth, decay, convergence to steady state, clamping both
ends, per-operation independence, null operation id. `aws-json-protocol` suite green with
checkstyle + spotbugs (spotbugs caught a now-unused private method, removed);
`aws-cbor-protocol` and `smithy-rpcv2-protocol` compile and pass against the new default methods.

### Allocation (authoritative), phase A → phase E

| client | scenario | phase A | phase E | delta |
|--------|----------|--------:|--------:|------:|
| v2-sync | batch-put | 184,380 | 105,355 | **−42.9%** |
| v2-async | batch-put | 192,275 | 112,345 | **−41.6%** |
| v2-sync | small-get | 39,109 | 40,153 | +2.7% (noise) |
| v2-sync | small-put | 33,514 | 33,094 | −1.3% |
| others | | | | ±0.6% |

The `json` category on sync batch-put: 117,188 → 38,175 B/op (−67%) — the doubling chain is gone;
what remains is Jackson's own writer scratch plus the single right-sized buffer. Small operations
are unaffected because their bodies already fit in the 1 KB default.

**Milestone: v2 batch-put now allocates less than smithy-java** (sync 105,355 and async 112,345 vs
smithy's 121,708 B/op) — smithy pays Jackson's growth chain on every call since it sizes its
`ByteBufferOutputStream` statically, while V2 now predicts per operation.

### Verdict

**Accepted.** Biggest single-scenario win of the project so far, exactly where the deep-dive
predicted (§6.1). The remaining `marshall` category cost (36 KB/op on batch-put: `sdkFields()`
iterators, trait probes) is option E part 2 — a CPU-shaped change requiring the field-loop rework.

---

## Cumulative scoreboard (phase 0 → F → B → A → E1)

Allocation, bytes/op, client code:

| client | scenario | phase 0 | now | total delta | vs smithy-java |
|--------|----------|--------:|----:|-----------:|---------------:|
| v2-sync | small-get | 61,387 | 40,153 | **−34.6%** | 4.0× (was 6.1×) |
| v2-sync | small-put | 54,509 | 33,094 | **−39.3%** | 4.7× (was 7.8×) |
| v2-sync | batch-get | 533,097 | 511,523 | −4.0% | 2.3× |
| v2-sync | batch-put | 204,272 | 105,355 | **−48.4%** | **0.87×** (was 1.68×) |
| v2-async | small-get | 69,702 | 49,085 | **−29.6%** | 4.9× |
| v2-async | small-put | 61,797 | 39,345 | **−36.3%** | 5.6× |
| v2-async | batch-get | 742,955 | 717,944 | −3.4% | 3.3× |
| v2-async | batch-put | 371,884 | 112,345 | **−69.8%** | **0.92×** (was 3.06×) |

### What remains, and where it lives

- **batch-get (−4% so far)**: dominated by response-side work — `AttributeValue` builders,
  `AttributeMapCopier`/`BatchGetResponseMapCopier` re-copying parser output (~30% of its
  allocation), i.e. option D's generated-copier item, which requires codegen changes.
- **small ops (~40 KB/op, 4-5× smithy)**: a long tail led by pipeline-framework (~20 KB/op):
  Apache header restatement (~2.2 KB), per-call `RequestPipelineBuilder` stage-chain construction
  (~1 KB — a phase G item), user-agent rebuild (~1 KB), `ExecutionAttributes` (~1.1 KB),
  auth-scheme option rebuild (~1 KB), plus response-side unmarshalling (~7 KB).
- **Options not yet started**: C (de-future the async request path — the async wall/CPU story
  rather than allocation), D (framework: attributes store, metric-stage eliding, auth-option
  caching, generated copiers), B part 2 (idempotent re-signing then the barrier move), E part 2
  (field-loop), and G (straight-line pipeline).

---

## Phase D (part 1) — per-request user-agent and header work

- Commit: `c01f8e84f29`
- Raw data: `raw/phaseD-framework/20260828-0729/`
- Analysis: `analysis/racecar/alloc-phaseD-cumulative.md`

### What changed

Four contained items in the `pipeline-framework` bucket, the largest remaining category on small
operations:

- `ApplyUserAgentStage` rebuilt the constant leading portion of the user agent
  (`userAgentPrefix + clientUserAgent`, including a `trim()` and emptiness checks) on every request.
  It is per-client constant — computed once in the constructor now.
- The user-agent `StringBuilder` started at the default 16 chars and grew by doubling to the typical
  100–200 char result. Now sized from the known prefix length plus headroom.
- `groupApiNames` allocated two `ArrayList`s even when the request had no api names (the common
  case). Short-circuits to a shared empty pair.
- `Apache5HttpRequestFactory.addHeadersToRequest` evaluated `IGNORE_HEADERS.stream().noneMatch(...)`
  **per header**, allocating a stream pipeline and capturing lambda for every header of every
  request. Replaced with an indexed loop.

### Allocation, phase E → phase D

| client | scenario | phase E | phase D | delta |
|--------|----------|--------:|--------:|------:|
| v2-sync | small-get | 40,153 | 38,995 | −2.9% |
| v2-sync | small-put | 33,094 | 31,472 | **−4.9%** |
| v2-sync | batch-get | 511,523 | 509,926 | −0.3% |
| v2-sync | batch-put | 105,355 | 104,150 | −1.1% |
| v2-async | small-get | 49,085 | 47,877 | −2.5% |
| v2-async | small-put | 39,345 | 39,128 | −0.6% |
| v2-async | batch-get | 717,944 | 699,732 | −2.5% |
| v2-async | batch-put | 112,345 | 111,664 | −0.6% |

The Apache header site went from ~1,250–1,640 to ~230–245 B/op (**−80 to −85%**) across scenarios —
the stream-per-header was most of its cost.

`sdk-core` 1,534 tests pass (apart from the known pre-existing `HttpClientApiCallTimeoutTest`
flake), `ApplyUserAgentStageTest` 11/11, apache5-client green with checkstyle + spotbugs.

### A measurement that stopped a change

`ExecutionAttributes.<init>` (`new IdentityHashMap<>(64)`, ~1,040 B of table) looked like an easy
win by shrinking the initial size. Instrumenting the real attribute count first showed **53
attributes per request** on every scenario — so `expectedMaxSize=64` is *correctly* sized (53
entries need capacity ≥ 80 → a 256-slot table), and shrinking it would have forced a rehash and
made things worse.

The only way to improve this site is the dense-int-key store (smithy-java's
`ChunkedArrayStorageContext` model): 53 attributes in an `Object[64]` is ~272 B versus ~1,040 B.
That is a ~770 B/op win but it touches `ExecutionAttributes`' public surface and the subtle
derived/mapped attribute `ValueStorage` semantics, so it is deliberately left as a scoped
follow-up rather than bundled here. Worth noting independently: **53 execution attributes per
request** is itself a lot of per-call state, and is the kind of thing phase G should question.

---

## Final scoreboard (phase 0 → F → B → A → E1 → D1)

Allocation, bytes/op, client code, versus the phase 0 baseline and smithy-java:

| client | scenario | phase 0 | now | total delta | vs smithy-java |
|--------|----------|--------:|----:|-----------:|---------------:|
| v2-sync | small-get | 61,387 | 38,995 | **−36.5%** | 3.9× (was 6.1×) |
| v2-sync | small-put | 54,509 | 31,472 | **−42.3%** | 4.5× (was 7.8×) |
| v2-sync | batch-get | 533,097 | 509,926 | −4.3% | 2.3× |
| v2-sync | batch-put | 204,272 | 104,150 | **−49.0%** | **0.86×** (was 1.68×) |
| v2-async | small-get | 69,702 | 47,877 | **−31.3%** | 4.8× (was 6.9×) |
| v2-async | small-put | 61,797 | 39,128 | **−36.7%** | 5.6× (was 8.9×) |
| v2-async | batch-get | 742,955 | 699,732 | −5.8% | 3.2× |
| v2-async | batch-put | 371,884 | 111,664 | **−70.0%** | **0.92×** (was 3.06×) |

Both batch-put cases now allocate **less than smithy-java**. Small operations are down ~1/3 but
remain 4–6× smithy, and batch-get has barely moved.

### Where the remaining gap is

**batch-get (−4 to −6%)** is response-side and needs codegen work: `AttributeValue` builders plus
`AttributeMapCopier`/`BatchGetResponseMapCopier` re-copying the parser's output (~30% of its
allocation). No pipeline change reaches it.

**Small ops (~39 KB/op)** are a long tail with no single dominant site left. From the phase D
profile (v2-sync small-get): response unmarshalling ~7 KB, `ExecutionAttributes` ~1.1 KB,
per-request `RequestPipelineBuilder` stage-chain construction ~1.0 KB, auth-scheme option rebuild
~1.0 KB, `LowCopyListMap` header machinery ~1.0 KB, `putHeader` ~1.0 KB, Jackson parser scratch
~3 KB.

Notably, **the pipeline object graph is rebuilt on every request** (`RequestPipelineBuilder.then` +
`wrappedWith` ≈ 1 KB/op). It cannot simply be cached because `HandleResponseStage` captures the
per-request response handler — which is precisely the argument for option G: a straight-line
pipeline has no per-request stage graph to allocate at all.

### Recommended next order

1. **G (straight-line pipeline)** — now the best-motivated item. It removes the per-request stage
   graph, makes the wrapper-chain problem from phase A structurally impossible, and is where the
   remaining `pipeline-framework` cost lives. **Prerequisite: fix the e2e CPU noise floor**
   (dedicated/quiesced host or many more reps), because G's payoff is CPU-shaped, not
   allocation-shaped, and the current 6–31% spread cannot resolve it.
2. **D part 2** — dense-int-key `ExecutionAttributes` (~770 B/op, needs a design decision on the
   public surface), and eliding the metric stages when no publisher is configured.
3. **C (de-future the async request path)** — the async CPU/latency story; also blocked on the
   noise floor.
4. **B part 2** — idempotent re-signing, then move the immutability barrier.
5. **E part 2 / codegen items** — the `sdkFields()` field loop and the generated response copiers;
   biggest remaining allocation items but they live in codegen, so longest validation tail.
