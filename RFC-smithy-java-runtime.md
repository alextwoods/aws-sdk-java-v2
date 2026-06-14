# RFC: Re-host the AWS SDK for Java v2 on smithy-java

**Status:** Draft / for discussion
**Author:** (pitch)
**Scope:** Replace the SDK's hand-maintained runtime *and* its C2J code generator with
smithy-java. The published SDK becomes a thin shell: Smithy models in, smithy-java
runtime underneath, the v2 public API preserved by a compatibility layer.

---

## 1. Summary

The AWS SDK for Java v2 currently *is* its own runtime. It maintains ~47 build modules
of execution machinery (`core/sdk-core` pipeline, `core/protocols/*`, `core/http-auth-aws`,
`core/retries`, `http-client-spi` + 5 transports) plus a bespoke **C2J → Java** code
generator (`codegen/`, `codegen-lite/`), and 451 generated service packages on top.

`smithy-java` (`software.amazon.smithy.java`, v1.4.0 on Maven Central, JDK 21,
client modules marked production-ready) already reimplements that entire runtime —
schema-driven serde, a 20-hook interceptor pipeline, SigV4 (+ S3Express), JSON/CBOR/XML
codecs, all four AWS protocols (awsJson, restJson1, restXml, awsQuery) + rpcv2, the AWS
endpoint rules engine, token-based retries, waiters, a pluggable transport
(JDK/Netty/CRT/Apache), and the AWS credential chain (env → props → IMDS → STS). It even
ships `aws/sdkv2/` shims (retries, shapes, auth) for exactly this kind of integration.

**Proposal:** retarget code generation to emit smithy-java-backed clients from **Smithy
models**, preserve the v2 public surface via a shim, and delete the redundant runtime and
the C2J pipeline. We stop maintaining two copies of the same engine.

---

## 2. The forcing function: schema-based serde is incompatible with C2J

This is the single most important point in this document, and it is not a preference — it
is a hard architectural constraint that dictates everything below.

### 2.1 The two serde models

**SDK v2 today is code-based serde.** The C2J model (`service-2.json`) is a *build-time
only* input. `codegen/` (`IntermediateModelBuilder`, `AddShapes`, `AddOperations`, the C2J
`Marshaller` emitters) reads it and generates a hand-shaped `Marshaller`/`Unmarshaller`
Java class per operation and shape (e.g.
`services/<svc>/.../transform/<Op>RequestMarshaller.java`). At runtime **there is no
model** — only imperative, generated marshalling code. Every AWS-specific wire quirk was
baked into per-shape generated Java.

**smithy-java is schema-based serde.** Codegen emits a compact runtime `Schema` object
(`software.amazon.smithy.java.core.schema.Schema`, sealed) per shape, embedded in the
generated `*ApiService`. The codecs
(`software.amazon.smithy.java.json.JsonCodec`, `…cbor.CborCodec`, `…xml.XmlCodec`) are
**generic and schema-driven**: they walk the `Schema` at runtime to serialize and
deserialize. The model *survives into runtime* as the Schema. There is no per-shape
marshaller class and **no per-shape escape hatch** — the `Schema` is the wire contract.

### 2.2 Why C2J cannot drive a schema runtime

A correct `Schema` must capture the full Smithy semantic model at **member granularity**:
`@httpLabel`, `@httpQuery`, `@httpHeader`, `@httpPayload`, `@httpPrefixHeaders`,
`@jsonName`, `@xmlName`, `@xmlAttribute`, `@xmlFlattened`, `@xmlNamespace`,
`@timestampFormat`, `@mediaType`, `@sparse`, `@required`, `@default`, `@eventHeader`,
`@eventPayload`, `@streaming`, plus mixins, resource graphs, and arbitrary trait
extensibility.

C2J is a **flattened, protocol-coupled down-projection** of that model. It encodes an
AWS-convention subset and discards member-level trait granularity, mixins, and the general
trait system. A C2J → Smithy converter therefore has to *guess* the traits C2J flattened
away. Because serde is now generic and schema-driven, **any wrong guess produces silently
wire-incorrect output** — a mis-inferred `@xmlAttribute` vs element, a missing
`@timestampFormat`, an `@httpPayload` member treated as a body field — and there is no
per-shape generated marshaller left to patch the quirk. With 426 services, the silent
failure surface is unacceptable.

### 2.3 Therefore: cut over, do not convert

Two further facts close the argument:

1. **C2J is already a lossy export of Smithy.** AWS services are authored in Smithy
   upstream; `service-2.json` is a downstream projection. Converting C2J → Smithy means
   round-tripping through a lossy format to reconstruct, at lower fidelity, an artifact
   that already exists upstream at full fidelity.
2. **The canonical Smithy models are exactly what `Schema` was designed to represent.**
   Feeding them to smithy-java's codegen is lossless by construction.

So we do **not** commit a converted-from-C2J model set. We adopt the **canonical Smithy
models** as the SDK's new source of truth and delete C2J — both the 426 `service-2.json`
model sets and the `codegen/` module that compiles them.

> If, in practice, canonical Smithy is not available for some long-tail service, that
> specific service is handled as an explicit exception (model it, or temporarily keep its
> legacy generated client), **not** by reintroducing C2J as a general input path.

---

## 3. The C2J satellite files fold into the model

C2J ships five bespoke sidecar formats. Smithy already models every one of them as
first-class traits, so the cutover is a *consolidation*, not just a swap:

| C2J sidecar | Smithy equivalent |
|---|---|
| `waiters-2.json` | `smithy.waiters#waiter` |
| `paginators-1.json` | `smithy.api#paginated` |
| `endpoint-rule-set.json` / `endpoint-tests.json` | `smithy.rules#endpointRuleSet` / `endpointTests` |
| `examples-1.json` | `smithy.api#examples` |
| `customization.config` | codegen-plugin config + custom Smithy traits / `JavaCodegenIntegration` |

Five hand-maintained JSON dialects collapse into one modeled artifact that smithy-java's
codegen already understands.

---

## 4. Target architecture: a shell over smithy-java

```
                 ┌──────────────────────────────────────────┐
   v2 public API │  DynamoDbClient / S3Client / builders /    │  ← preserved by shim
   (unchanged)   │  request+response POJOs / exceptions       │
                 └───────────────────┬───────────────────────┘
                                     │ compatibility shim (thin adapters)
                 ┌───────────────────▼───────────────────────┐
   smithy-java   │  Client / ClientInterceptor pipeline        │
   runtime       │  schema-driven codecs (JSON/CBOR/XML)       │
   (the guts)    │  ClientProtocol (awsJson/restJson1/restXml/ │
                 │  awsQuery/rpcv2) · SigV4 · rules engine ·   │
                 │  retries · ClientTransport (JDK/Netty/CRT)  │
                 └────────────────────────────────────────────┘
                                     ▲
                 ┌───────────────────┴───────────────────────┐
   build time    │  Smithy models  →  smithy-java codegen      │
                 │  (canonical, replacing C2J + codegen/)      │
                 └────────────────────────────────────────────┘
```

The SDK repo's job shrinks to three things: (a) the Smithy models, (b) an AWS codegen
"flavor" that shapes generated output to the v2 surface, and (c) the compatibility shim +
the gap features smithy-java doesn't yet cover.

---

## 5. Module disposition

| SDK v2 module(s) | Fate | smithy-java counterpart |
|---|---|---|
| `codegen/`, `codegen-lite/`, `*-maven-plugin` | **Delete** | smithy-java Gradle codegen plugin |
| 426 × `service-2.json` + sidecars | **Delete** | canonical Smithy models |
| `core/sdk-core` execution pipeline | **Delete → bridge** | `client-core` `ClientPipeline` + `ClientInterceptor` |
| `core/protocols/*` | **Delete** | `aws-client-*` + codecs |
| `core/http-auth-aws`, `http-auth-aws-crt` | **Delete → bridge** | `aws-sigv4`, `aws-sigv4-s3express` |
| `core/retries`, `retries-spi` | **Delete → bridge** | `retries` + `aws-sdkv2-retries` shim |
| `http-client-spi` + `http-clients/*` | **Bridge then delete** | `ClientTransport` + JDK/Netty/CRT/Apache |
| `core/endpoints-spi` + endpoint rules | **Delete** | `rulesengine` + `aws-client-rulesengine` |
| `core/identity-spi`, `auth/` cred providers | **Bridge** | `aws-credential-chain` + adapters (gap, §7) |
| `core/aws-core`, public types | **Keep (shim layer)** | n/a — this is the preserved surface |
| `services-custom/*` | **Port case-by-case** | Smithy integrations / interceptors |

"Bridge" = a thin adapter retained only while both runtimes coexist, deleted once cutover
completes.

---

## 6. Public API compatibility

The published v2 surface — `Default<Svc>Client` / `<Svc>AsyncClient`, builders,
request/response POJOs, service exceptions, `AwsCredentialsProvider`, `Region`,
`ExecutionInterceptor`, `ClientOverrideConfiguration` — is what millions of deployments
compile against. The codegen flavor must emit these types, delegating their bodies to
smithy-java.

**Recommendation:** **strict v2-surface shim** — zero source/binary churn for customers,
at the cost of an adapter layer (e.g. v2 `ExecutionInterceptor` ⇄ smithy-java
`ClientInterceptor`, v2 `SdkHttpClient` ⇄ `ClientTransport`, v2 `AwsCredentialsProvider` ⇄
`IdentityResolver`). Roll out as an **opt-in parallel artifact line** first, then make it
the default in the next major version. *(Open question — see §10.)*

---

## 7. Gaps smithy-java does not yet cover

These need explicit workstreams; none are blockers, but they are real:

- **Presigning** (S3 etc.) — not in smithy-java; port as protocol-aware logic /
  interceptors.
- **Flexible checksums** (CRC32/CRC32C/SHA) — verify coverage; port if absent.
- **`DefaultCredentialsProvider` richness** — SSO, profile/config-file resolution, web
  identity, process credentials. smithy-java has the chain skeleton (env → props → IMDS →
  STS); the long tail needs porting or adapting via `aws-sdkv2-auth`.
- **SigV4a** (region-set / multi-region signing) — confirm parity with `http-auth-aws`.
- **Metric publishers** (`metric-publishers/`) — wire to `client-metrics-otel` or bridge.
- **`services-custom/` behaviors** — S3 (multipart, virtual-host addressing), DynamoDB
  enhanced client, etc. — ported individually as Smithy integrations.

---

## 8. JDK 21 baseline

smithy-java mandates JDK 21. The SDK is pinned to Java 8: root `pom.xml` `jre.version`
= `1.8`, with `maven.compiler.release` = `8` enforced in the `jdk-11-plus` profile, plus
`jdk-13-plus` / doclint / spotbugs-skip profiles built around the 8 baseline. Phase 0
bumps `jre.version` → `21`, removes the release=8 pinning and obsolete JDK profiles, and
establishes a clean JDK 21 build. This is a breaking, major-version change and a hard
prerequisite for everything else, so it ships first and independently.

---

## 9. Phased plan

- **Phase 0 — JDK 21 baseline.** Bump `jre.version` to 21, strip Java-8 profiles, green
  build. Self-contained; prerequisite for all that follows.
- **Phase 1 — cutover spike (one service).** Take the *canonical Smithy* model for one
  service (DynamoDB), run it through smithy-java's codegen, wrap it in the v2-surface shim,
  and pass the existing DynamoDB protocol + integration test suites. Retires the two
  central risks at once: canonical-model availability/fidelity and shim surface accuracy.
- **Phase 2 — codegen replacement.** Stand up smithy-java's codegen as the SDK generator
  with an AWS flavor (integrations/traits) shaping output to the v2 surface. `codegen/`
  enters end-of-life.
- **Phase 3 — breadth + gaps.** Bring all services onto canonical Smithy; deliver the §7
  gap features as integrations/interceptors.
- **Phase 4 — delete.** Remove `codegen/`, `codegen-lite/`, the C2J models + sidecars, and
  the redundant runtime modules (§5). This is where the maintenance win is realized.

---

## 10. Locked decisions

These were resolved and now constrain the prototype:

1. **Model source.** Canonical Smithy models come **from Maven Central**, pulled as
   ordinary smithy artifact dependencies (different services / sidecar concerns arrive via
   different smithy deps). No C2J, no in-repo conversion. Ownership/sync cadence is a
   non-question for the prototype — it's just Maven coordinates.
2. **Every service has a Smithy model**, so there is no long-tail fallback to design for.
3. **API compatibility: preserve the v2 surface "to an extent."** Make it work first,
   make it perfect later — source/binary compatibility is not gated up front. **If a hard
   compatibility conflict surfaces, escalate to the maintainer for a judgment call** rather
   than silently breaking or silently shimming.
4. **Interceptors:** likely need a **two-way bridge** (v2 `ExecutionInterceptor` ⇄
   smithy-java `ClientInterceptor`). Direction TBD during implementation.
5. **Rollout: straight major-version replacement.** No parallel `-smithy` artifact line —
   "just yeet the code."
6. **Presigning:** reuse the **existing v2 presigner** for now rather than porting/recreating
   it on smithy-java.
7. **Transport:** do **not** preserve the 5 v2 HTTP clients. Put a **shim over smithy-java's
   transport** and move on.
8. **`services-custom/` (S3 multipart/virtual-host, DynamoDB enhanced client, etc.):**
   **out of scope** — this is a prototype.
9. **JDK 21 baseline: DONE (Phase 0).** `jre.version` 1.8 → 21, `maven.compiler.release`
   8 → 21 in the `jdk-11-plus` profile, javadoc `<source>` 8 → 21. Verified: the full
   `sdk-core` dependency closure compiles clean under Corretto 21 with Java-21 bytecode
   (class major version 65).
10. **Audience:** working document for the maintainer + assistant during prototype
    iteration; not a wide-review artifact.
