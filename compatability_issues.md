# V2-on-smithy-java: compatibility issue ledger

Branch: `smithy-java-bridge-alexwoo-full`. Scope: **DynamoDB, sync client, non-streaming
operations only.** smithy-java 1.6.1 / smithy 1.73.0 / AWS SDK for Java v2 2.46.11-SNAPSHOT.

This is an **exhaustive inventory of behavioral differences**, not a fix list. The goal of the
prototype is to learn where a smithy-java-based pipeline cannot reproduce v2 semantics, and what
it costs to make it. Nothing here is expected to be fixed on this branch unless it blocks a
working request/response.

Status legend:

| Status | Meaning |
|---|---|
| `BRIDGED` | v2 component is adapted and driven by the smithy pipeline; behavior intended to match |
| `TRANSLATED` | v2 configuration is read once at client construction and mapped to a smithy-native equivalent |
| `DEGRADED` | works, but observably differs from v2 |
| `MISSING` | v2 feature has no effect at all on this path |
| `BLOCKED` | cannot be represented in smithy-java without an upstream change |
| `FRAGILE` | behavior currently matches, but rests on a coincidence that nothing enforces — a name, a class hierarchy — so it can regress silently |

Each entry records what a customer would observe, not just the internal difference.

---

## 1. Errors and exceptions

### 1.1 `BLOCKED` — v2 modeled exceptions cannot be smithy `ModeledException`s

`software.amazon.smithy.java.core.error.ModeledException` is an **abstract class** extending
`CallException`. Every v2 modeled error already extends `DynamoDbException -> AwsServiceException
-> SdkServiceException -> SdkException -> RuntimeException`. Java has no multiple inheritance, so a
generated v2 exception can never be a `ModeledException`.

This matters because `HttpErrorDeserializer.ErrorPayloadParser.parsePayload` resolves the error
builder with:

```java
typeRegistry.createBuilder(id, ModeledException.class)
```

`TypeRegistry.createBuilder(id, type)` throws `SerializationException` when the registered class is
not assignable to `type`. Registering the v2 exception directly is therefore not viable.

**Consequence before mitigation:** modeled errors never deserialize. Every service error surfaces
as a bare `CallException` with no error code, no modeled members, and — because
`ApplyModelRetryInfoPlugin` keys off `error instanceof ModeledException` — no retry classification.
Throttling errors are not retried.

**Mitigation on this branch:** a single hand-written shim,
`software.amazon.awssdk.bridge.smithyjava.error.V2ModeledError extends ModeledException`, wraps the
built v2 exception. Its `ShapeBuilder` delegates deserialization to the v2 exception's generated
builder. `ApiOperationSpec` registers the shim per error shape instead of the v2 exception. The
bridge unwraps `V2ModeledError` at the client boundary so callers still catch e.g.
`ProvisionedThroughputExceededException`.

**Residual difference:** the exception a *smithy `ClientInterceptor`* observes inside the pipeline is
`V2ModeledError`, not the v2 exception. Anything inspecting error types inside the pipeline (including
a customer-supplied smithy retry strategy) sees the shim.

**Upstream change that would remove this:** make `HttpErrorDeserializer` resolve builders as
`SerializableStruct` and let the protocol decide how to throw, or accept a
`KnownErrorFactory`/`ErrorPayloadParser` on `AwsJson1Protocol`. Today `AwsJsonProtocol` is
`sealed`/package-private and builds its `HttpErrorDeserializer` internally, so there is no injection
point.

### 1.2 `DEGRADED` — generated smithy `Schema`s carry no traits

`SdkSchemaFactory.structure(...)` and `Schema.createOperation(ShapeId)` produce schemas with **no
traits**. Everything in smithy-java that reads traits off a schema is therefore inert:

| Trait consumer | Trait | Effect of absence |
|---|---|---|
| `ApplyModelRetryInfoPlugin` | `@retryable`, `@readonly`, `@idempotent` | no model-driven retry classification |
| `ModeledException.getHttpStatusCode` | `@httpError`, `@error` | status code always defaults to 500 |
| `CallException.getFault` | `@error` | fault always `OTHER`, never `CLIENT`/`SERVER` |
| `InjectIdempotencyTokenPlugin` | `@idempotencyToken` | tokens not auto-generated |
| `HostLabelEndpointResolver` | `@hostLabel`, `@endpoint` | host prefixes not applied |

On this branch retry classification is instead recomputed from the **v2** exception by
`V2RetryClassification` (see 3.2), so the missing traits do not break retries — but they do mean the
smithy-native retry arm of the benchmark classifies nothing as retryable.

The two status/fault rows are about smithy's *internal* view of the error, not the caller's: the status
code and fault a v2 caller reads come from the HTTP response via `V2ErrorEnricher` (1.4) and are
correct. `V2UnmodeledError` derives its fault with `ErrorFault.ofHttpStatusCode` for the same reason —
what smithy inferred without a parsed payload is not worth propagating.

### 1.3 `DEGRADED` — unmodeled errors keep their metadata but lose their concrete type

When the wire error code matches no shape in the operation's `TypeRegistry`, smithy throws a bare
`CallException` whose only content is a synthesized message (`"Server HTTP/1.1 503 response from
operation ...GetItemOperation@476a736d."`). This is the common case, not the exceptional one: a 503
from a load balancer carries no AWS error payload, an empty 5xx has no body to parse, and an error the
*service* models but the *operation* does not declare is absent from a per-operation registry.

**Measured, before mitigation** (`pipeline_benchmark2/errors/20260905-0138`): the bare `CallException`
path was catastrophic, not merely lossy. On five of nine injected faults the bridge returned a
`DynamoDbException` with `statusCode() == 0`, a null request ID, a null error code, no
`awsErrorDetails()`, and **one attempt where v2 made three** — a 503 was handed straight back to the
caller. See §3.2 for why losing the metadata also loses the retry.

**Mitigation on this branch:** `V2ErrorEnricher` no longer early-outs on non-`V2ModeledError`
failures. When the attempt has an HTTP response with a status `>= 400`, it builds the service's base
exception from that response — status code, request ID, `x-amz-id-2`, `awsErrorDetails` with the error
code smithy put on `CallContext.RESPONSE_ERROR_CODE`, and `sdkHttpResponse` — wraps it in
`V2UnmodeledError`, and *throws that in place of* the bare `CallException`. `ClientPipeline` treats an
error thrown from `modifyBeforeAttemptCompletion` as the attempt's error (it routes it through
`swapError`), so the retry strategy sees the classification stamped on the new exception. The client
boundary then unwraps it exactly as it does a modeled error.

The `>= 400` guard matters: a 2xx whose body fails to deserialize is a client-side problem, and
building a service exception for it would report `statusCode() == 200`, which no v2 caller expects.
Those keep going through `SmithyBridgeClient.translate`, which returns the first `SdkException` on the
cause chain unchanged (so transport, credential, and endpoint failures the bridge itself raised keep
their real v2 type) and otherwise maps a smithy `TransportException` to `SdkClientException`.

**Residual difference — the exception class.** smithy's error registry is **per operation**
(`ApiOperation.errorRegistry()`), built from the errors that operation declares. v2's is effectively
**per service**: `AwsJsonProtocolErrorUnmarshaller` is handed the whole service's exception list, so it
can name an error the operation never declared. Injecting `ConditionalCheckFailedException` — modeled
by DynamoDB, not declared on `GetItem` — shows the gap exactly: v2 throws
`ConditionalCheckFailedException` with `errorCode=ConditionalCheckFailedException`; the bridge throws
`DynamoDbException` with the same status code, request ID, service name and error code, but not the
same class. A caller with `catch (ConditionalCheckFailedException e)` around a `GetItem` misses.

Widening the registry to the whole service would close it, at the cost of one `TypeRegistry` per
service rather than per operation (DynamoDB: ~30 error shapes against `GetItem`'s 6) plus the
construction cost on every client. Not attempted here.

### 1.4 `DEGRADED` — `requestId` / `extendedRequestId` and HTTP metadata

v2 populates `SdkResponse.responseMetadata()`, `SdkResponse.sdkHttpResponse()` and
`SdkServiceException.requestId()`/`statusCode()`/`awsErrorDetails()` from the HTTP response during
unmarshalling. On this path it splits three ways:

- **Modeled errors: restored.** `V2ErrorEnricher` runs at smithy's `modifyBeforeAttemptCompletion`
  and calls `V2ModeledError.enrich(httpResponse, RESPONSE_ERROR_CODE, serviceName)`, which sets
  `requestId()`, `statusCode()` and `awsErrorDetails()` on the wrapped v2 exception before it is
  unwrapped. It exists for retry classification (3.2), which needs the AWS error code per attempt;
  the caller-visible fields are a deliberate side effect of the same pass.
- **Unmodeled errors: restored.** Same pass, via `V2UnmodeledError`; see 1.3. `extendedRequestId`
  (`x-amz-id-2`) is read on both error paths.
- **Successful responses: missing, and not null-safe.** Nothing in the deserialization bridge sets
  `responseMetadata` or `sdkHttpResponse`, and the generated `DynamoDbResponse` simply returns the
  unset field. `response.responseMetadata().requestId()` and
  `response.sdkHttpResponse().statusCode()` therefore throw `NullPointerException` rather than
  returning `null`/`UNKNOWN`. v2 always populates both.

### 1.5 `BRIDGED` — retry-after honoring from response headers

Originally recorded as `MISSING`, and the sweep showed it mattered: v2 spent 2 010 ms on a
`503` + `Retry-After: 1` where it spent 54 ms on the same 503 without the header, so v2 does honor it
and the bridge visibly did not.

The plumbing turned out to be present on both sides. `ClientPipeline` reads `RetryInfo.retryAfter()`
off the attempt's error and passes it to `RefreshRetryTokenRequest` as a suggested delay; nothing on
this path was populating it. `V2ErrorEnricher` now does, for both error paths, parsing `Retry-After` as
whole seconds.

**Residual differences:** only whole seconds are read and the HTTP-date form of `Retry-After` is
ignored — deliberately, because `RetryableStage.retryAfter` in v2 does the same and the point is for
both pipelines to back off by the same amount. `x-amz-retry-after` (milliseconds) is *not* read; v2
reads it only under `NEW_RETRIES_2026_ENABLED`, which this branch does not implement.

### 1.6 `DEGRADED` — the message on an unmodeled error omits the payload's `message`

`CallContext` exposes the normalized error *code* (`RESPONSE_ERROR_CODE`) but not the error *message*,
and by `modifyBeforeAttemptCompletion` the response body has been consumed. So `V2ErrorEnricher` cannot
recover a `message` member from an error whose shape it could not resolve, and reproduces v2's two
*fallback* messages instead: `"Service returned error code <code>"` when a code is available, else
`"Service returned HTTP status code <n>"` (mirroring
`AwsJsonProtocolErrorUnmarshaller.errorMessageForException`).

That is exact whenever v2 also had no message — a body-less 503, an empty 5xx. It differs when the
payload did carry one: for an unmodeled-on-this-operation error, v2 reports `injected fault (Service:
DynamoDb; Status Code: 400; Request ID: ...)` and the bridge reports `Service returned error code
ConditionalCheckFailedException (Service: DynamoDb; ...)`.

Separately, and on **every** error including modeled ones, v2 appends `(SDK Attempt Count: N)` to the
message and this path does not: v2 stamps it from `SdkStandardLogger`/`AwsServiceException`'s
`numAttempts`, which the bridge never sets because the attempt count lives in smithy's
`CallContext.RETRY_ATTEMPT` rather than on the v2 exception. Message text only — no field a caller
reads differs.

### 1.7 `FRAGILE` — the error registry is keyed on shape ID, so any codegen rename silently disables it

Found by the error sweep, not by inspection: `internal-error` (500, `InternalServerError`, **declared
on `GetItem`**) was falling through to the unmodeled path while `throughput-exceeded` resolved
correctly. The cause was `AwsServiceModel.smithyShapeId()` building the id from
`shapeModel.getShapeName()` — the generated **Java class name** — where v2's naming strategy has
already appended `Exception` to error shapes. So `InternalServerError` on the wire was looked up
against a registry keyed `com.amazonaws.dynamodb#InternalServerErrorException` and missed. Fixed by
using `shapeModel.getC2jName()`, the name as the model spells it.

Worth recording as a hazard rather than just a fixed bug, because the failure mode is silent and total:
a mismatched id produces no error, no log line, and a plausible-looking `DynamoDbException` — it just
quietly costs the concrete exception type, the error code, and (before 1.3's mitigation) the retry.
Every place v2 codegen renames a shape is a place this can recur, and there is no build-time check that
a registered id matches what the service actually sends.

---

## 2. Interceptors and plugins

### 2.1 `BRIDGED` (lazily) — `ExecutionInterceptor`

v2's generated builder (`BaseClientBuilderClass`) installs interceptors from five sources: three
service built-ins (auth-scheme resolution, endpoint resolution, endpoint request-modifier), the
customization config's list, classpath discovery via
`ClasspathInterceptorChainFactory` (`software/amazon/awssdk/services/dynamodb/execution.interceptors`),
`overrideConfiguration().addExecutionInterceptor(...)`, and — for every AWS client —
`AwsDefaultClientBuilder.awsInterceptors()`.

On this branch the **three service built-ins are dropped**, because smithy owns auth-scheme resolution
and endpoint resolution natively (see 4.1, 5.1). Everything else is wrapped by `V2InterceptorBridge`
**only when the resolved list is non-empty**.

That lazy install was expected to keep the default path free, and at first it did not:
`awsInterceptors()` unconditionally adds `HelpfulUnknownHostExceptionInterceptor`,
`EventStreamInitialRequestInterceptor` and `TraceIdExecutionInterceptor` to every AWS client, so the
bridge was installed on **every** client and a plain `DynamoDbClient.create()` paid per-attempt v2
`Context`/`ExecutionAttributes` construction at all four bridged hook points to run three interceptors
that could not do anything. Measured cost of that: 5.9% of app CPU on `small-get` and 8.4% on
`small-put` (4/4 paired wins, `pipeline_benchmark2/components/20260904-2313`) — the largest
single-component cost of the four bridges.

`V2ConfigTranslator.inert()` now drops an interceptor that provably cannot act, in two layers:

1. **General:** an interceptor overriding none of the six hooks the bridge invokes is dropped. It was
   already silently ineffective (see the 12-hook list below); this stops charging for it.
   `HelpfulUnknownHostExceptionInterceptor` falls out here, since it implements only `modifyException`.
2. **Named, scope-specific:** `EventStreamInitialRequestInterceptor` gates both its hooks on
   `HAS_INITIAL_REQUEST_EVENT`, which only an async event-stream operation sets — inert for sync clients,
   and **this drop must be revisited if async comes into scope** (10.2). `TraceIdExecutionInterceptor`
   gates all four of its hooks on the `AWS_LAMBDA_FUNCTION_NAME` environment variable, which cannot
   change while the JVM runs, so it is dropped only when that variable is unset.

The compatibility consequence is narrow but real: **the inert-ness is judged once, at client
construction, from the class and the environment** rather than per call. If a future v2 release gives
one of those two named classes an effect outside the gate the filter assumes, a bridged client silently
loses it. `awssdk.bridge.keepInertInterceptors=true` restores the unfiltered behavior, and the
`v2-sync-inert-interceptors` benchmark arm uses it to measure what the filter recovers. Nothing else
changes: with the filter on, a default DynamoDB client installs no interceptor bridge at all —
`V2InterceptorBridge` is not even class-loaded — which is what the lazy install was supposed to achieve.

Measured recovery (`pipeline_benchmark2/inertfilter/20260904-2355`): restoring the old behavior costs
7.4% of app CPU on `small-get` and 8.4% on `small-put` (0/4 paired wins), and stripping the interceptor
bridge now buys +0.7%/+0.0% — nothing, because nothing is left to strip. A customer interceptor
overriding a bridged hook still runs, verified directly.

Six of v2's eighteen hooks are invoked, mapped onto four smithy hooks:

| smithy `ClientInterceptor` | v2 `ExecutionInterceptor` |
|---|---|
| `readBeforeExecution` | `beforeExecution` |
| `modifyBeforeSerialization` | `modifyRequest` |
| `modifyBeforeSigning` | `modifyHttpRequest`, `modifyHttpContent` |
| `readAfterExecution` | `afterExecution` *or* `onExecutionFailure` |

`modifyHttpRequest` maps to `modifyBeforeSigning` so that a header an interceptor adds still ends up
inside the signature, as in v2.

The other **12 hooks are never invoked**: `beforeMarshalling`, `afterMarshalling`,
`modifyAsyncHttpContent`, `beforeTransmission`, `afterTransmission`, `modifyHttpResponse`,
`modifyHttpResponseContent`, `modifyAsyncHttpResponseContent`, `beforeUnmarshalling`,
`afterUnmarshalling`, `modifyResponse`, `modifyException`. Notably, an interceptor that rewrites the
response (`modifyResponse`) or the exception (`modifyException`) is silently inert.

Residual differences in the hooks that *are* bridged:

- **`modifyHttpRequest` does not see the resolved endpoint.** smithy resolves the endpoint *after*
  `modifyBeforeSigning` (`ClientPipeline.doSendOrRetry` calls the hook, then `afterIdentity` does
  `resolveEndpoint` + `ClientProtocol.setServiceEndpoint`), so the request smithy hands the bridge
  carries its unresolved placeholder URI: no scheme, no host, path `/`. A v2
  `SdkHttpFullRequest` cannot even be *built* without a protocol, so the bridge substitutes the
  **client endpoint** (`CLIENT_ENDPOINT_PROVIDER.clientEndpoint()`, falling back to
  `https://unresolved.invalid`). That equals the resolved endpoint for a client with an
  `endpointOverride`, and differs from it whenever the rules engine rewrites the host — account-ID
  endpoints, FIPS, dual-stack, host prefixes. An interceptor that inspects the host, or that keys off
  it (region sniffing, per-endpoint routing decisions, logging), sees the wrong value.
- **Host/scheme/port changes made by an interceptor are silently discarded.** `setServiceEndpoint`
  runs after the hook and overwrites scheme, host and port from the resolved endpoint, so only the
  path, query and headers written back survive. In v2, `modifyHttpRequest` *can* redirect a request to
  a different host; here it cannot, and fails silently rather than erroring.
- The path written back has to be de-prefixed by the bridge, because `setServiceEndpoint`
  concatenates the endpoint's path in front of it; an interceptor that rewrites the path to something
  not starting with the client endpoint's path prefix produces a different final path than in v2.
- **A `RequestBody` returned by `modifyHttpContent` is dropped.** smithy owns serialization and the
  body is already a `DataStream` by this point. The hook runs (v2's chain calls both together via
  `modifyHttpRequestAndHttpContent`) and its request modifications are kept, but body replacement —
  the whole point of the hook — has no effect.
- `ExecutionAttributes` passed to bridged hooks is synthesized from client config and holds four
  attributes only: `AWS_REGION`, `ENDPOINT_PREFIX`, `SERVICE_NAME`, `CLIENT_TYPE`, plus
  `OPERATION_NAME` per call. Everything else v2 populates is absent — metric collectors, checksum
  specs (`RESOLVED_CHECKSUM_SPECS`), the auth-scheme attributes (`SELECTED_AUTH_SCHEME`,
  `SIGNING_REGION`, `SIGNER`), `CLIENT_ENDPOINT`, `RESOLVED_ENDPOINT`, `API_CALL_ATTEMPT_*`. An
  interceptor reading any of them sees `null`, and one *writing* an attribute for a later built-in to
  read is writing into a void.
- Attributes are not shared with anything else: v2's built-in interceptors are not in the chain
  (below), so cross-interceptor attribute conventions between customer and SDK code are broken.
- **`modifyHttpRequest` is invoked once per attempt instead of once per execution.** v2 calls it from
  `BaseClientHandler.finalizeSdkHttpFullRequest`, during marshalling and outside the retry loop;
  smithy's `modifyBeforeSigning` is inside it. An interceptor that appends rather than sets — a
  counter, a sequence number, a header it adds unconditionally — compounds across retries here where
  it did not in v2.
- `dynamodb-enhanced`'s `ApplyUserAgentInterceptor` is discovered and bridged, so the
  enhanced-client user-agent suffix is preserved.

### 2.2 `MISSING` — `SdkPlugin`

`overrideConfiguration()`/`addPlugin(SdkPlugin)` mutates `SdkServiceClientConfiguration.Builder` at
client build time. Client-level plugins still run (they operate on v2 config before the bridge reads
it), but **request-level** plugins do not, because the bridge never rebuilds its `ClientConfig`
per request.

### 2.3 `MISSING` — request-level `overrideConfiguration()`

`AwsRequestOverrideConfiguration` on an individual request (credentials, interceptors, headers,
query params, API call timeouts, metric publishers, signer, plugins,
`putExecutionAttribute`) is entirely ignored. smithy has an equivalent concept
(`RequestOverrideConfig` on `Client.call`), so this is bridgeable in principle; it is simply not
wired here — `SmithyBridgeClient.invoke` passes `null` for it.

Concretely: codegen still emits `updateSdkClientConfiguration(SdkRequest, SdkClientConfiguration)`,
the method that folds request-level overrides into a per-request config, but on the smithy path
**nothing calls it** (one definition, zero call sites in `DefaultDynamoDbClient`; the async client
calls it per operation). The one piece of request override configuration that does survive is metric
publishers, because the generated method resolves them before delegating — see 7.1.

### 2.4 `MISSING` — `executionAttributes()` on requests

No `SdkRequest`-level execution attribute reaches the pipeline.

---

## 3. Retries

### 3.1 `BRIDGED` — retry strategy

`software.amazon.smithy.java:aws-sdkv2-retries`'s `SdkRetryStrategy.of(v2Strategy)` wraps v2's
resolved `RetryStrategy`. This preserves, exactly: `maxAttempts`, backoff strategy, the retry token
bucket, `RetryMode` (`LEGACY`/`STANDARD`/`ADAPTIVE`/`ADAPTIVE_V2`), the `new-retries-2026`
resolution, and DynamoDB's `DynamoDbRetryPolicy` overrides (8 retries pre-2026 / 4 attempts post,
25 ms base delay, exponential/full-jitter backoff).

The backoff half of that is **measured, not just asserted**. `javap` on the published jar shows
`refreshRetryToken` to be pure delegation — it rebuilds a v2 `RefreshRetryTokenRequest` from
(token, failure, suggestedDelay), calls the wrapped v2 strategy, and returns its `delay()` verbatim;
`acquireInitialToken` and `maxAttempts` likewise. So delay *computation* is v2's in both arms and
cannot differ by construction. Empirically, with an unjittered 200 ms backoff pinned on both the
throttling and non-throttling paths, every 3-attempt case takes **203.0 ms per extra attempt in both
arms** (`pipeline_benchmark2/errors/20260905-1527`). A dropped, halved or defaulted backoff would be
a flat offset and is excluded. Scope: the probe pins `RetryMode.STANDARD`, so the measurement covers
the standard strategy. The `DynamoDbRetryPolicy` and `RetryMode` claims above still rest on the
delegation argument alone, and the deprecated `retryPolicy(...)` path is a separate defect (3.4).

Note: `aws-sdkv2-retries` declares `software.amazon.awssdk:retries-spi:2.52.0`. The bridge pom
excludes that transitive dependency so the in-tree `${awsjavasdk.version}` is used instead.

Cost on a call that succeeds: **nothing measurable.** Swapping the wrapper for smithy's own
`StandardRetryStrategy` (`awssdk.bridge.stripRetries`) moves app CPU by −0.4% / −0.3% on small
operations — inside the ±3% noise floor, and only 3/4 and 2/4 paired wins
(`pipeline_benchmark2/components/20260904-2313`). Same for the error enricher (1.3, 3.2): +0.1% /
−0.4%, i.e. unmeasurable, which is expected since it only does work on the error path.

Cost on a call that *does* retry, with the backoff sleep removed so wall time is the client's own
work (`pipeline_benchmark2/errors/20260905-1532`, 60 reps): the marginal cost of one extra attempt is
**675 µs baseline vs 578 µs bridge**, against a warm 1-attempt call of 1,110 vs 669 µs. The bridge is
cheaper on both, but note the ratios — **0.60x for the one-time work and only 0.86x per attempt**, so
roughly a fifth of the bridge's per-call advantage recurs on a retry and its relative advantage
decays with attempt count. The likely cause is 3.3, not this section.

### 3.2 `DEGRADED` — retry *classification* is recomputed, not reused

`SdkRetryStrategy.of()` deliberately **replaces** the delegate's `retryOnException` and
`treatAsThrottling` predicates with ones that read smithy's `RetryInfo`. v2's own classification
(`RetryOnExceptionsCondition`, `RetryOnStatusCodeCondition`, `RetryOnClockSkewCondition`,
`AwsRetryStrategy`'s error-code lists) is therefore **not consulted**.

This is a sharper constraint than "the predicates are replaced": `SdkRetryStrategy`'s substitute
predicate is `getInfo(t) != null && getInfo(t).isRetrySafe() == RetrySafety.YES`. Not-`YES` is not
retried, and `RetrySafety.MAYBE` — the default on a bare `CallException` — is not `YES`. **Any error
nothing explicitly classifies is silently non-retryable.**

On this branch `V2RetryClassification` repopulates `RetryInfo` from a v2 exception using v2's own
predicates (`isThrottlingException()`, `isClockSkewException()`, retryable status codes,
`RetryableException`, `IOException`), which matches v2's *default* classification. It is applied on
three paths, all of which had to exist before the behavior matched:

- `V2ModeledError`, at construction and again after `enrich` (the AWS error code is what makes a
  throttling error retryable, and that is only known once the response is in hand).
- `V2UnmodeledError`, whose v2 exception carries the real status code — this is what makes a 503 or an
  empty 500 retryable (1.3).
- Anything else, classified in place by `V2ErrorEnricher`: `RetrySafety.YES` if the cause chain reaches
  an `IOException`/`UncheckedIOException`, or — when a response was received — a smithy
  `SerializationException`. Not forced to `NO` otherwise, since a bridge failure of unknown origin is
  not evidence that retrying is unsafe. The second clause exists because of 3.5.

**Measured** (`pipeline_benchmark2/errors/20260905-0138`, before the second and third paths existed):
of nine injected faults, the bridge retried **two**. `internal-error` (500), `unavailable` (503),
`unavailable-retry-after`, `empty-500`, and `malformed-body` all returned on the first attempt where v2
made three — and in the transient variants, where the third attempt would have succeeded, the bridge
threw and v2 returned the item. That is the most severe finding in this ledger: a working-looking client
that does not retry server errors.

**Re-measured** with all three paths in place (`pipeline_benchmark2/errors/20260905-0159`): the whole
diff fell from 87 behavioral differences to 19, and eight of the nine faults now retry exactly as v2
does — same attempt count on every persistent case, and every transient case recovers on the third
attempt in both arms. `malformed-body` was the one holdout, for the reason in 3.5.

**Re-measured again** once 3.5 was fixed (`pipeline_benchmark2/errors/20260905-0223`): **attempt counts
identical on all 21 cases** then in the catalogue, with zero variation across reps. What remained was 14
behavioral differences plus 12 message-wording ones, all metadata or text — `rawResponse` never populated,
the `(SDK Attempt Count: N)` message suffix absent, the 1.3 type mismatch, and the cause type in 3.5 — none
of which changes whether or how often a call is retried. (Counts throughout this ledger are
`error_behavior_diff.py`'s, which reports message text separately from behavior; the sum of the two is the
"26 field differences" figure an earlier revision quoted here.)

Adding a fault the catalogue had been missing then found the one case that still differs, and it is not a
classification problem at all: a connection reset before response headers is retried three times by v2 and
once by the bridge, because smithy-java's retry loop is unreachable from the transport path. See 3.6. Every
*response* the service sends, well-formed or not, is now retried as v2 retries it; nothing that fails
before a response is retried at all.

What still differs:

- A customer-supplied `RetryStrategy` whose `retryOnException` adds custom conditions has those
  conditions silently discarded.
- `RetryPolicy` (the deprecated API) adapted via `RetryPolicyAdapter` — its `RetryCondition`s are
  likewise discarded. This includes `additionalRetryConditionsAllowed(false)`, which
  `DynamoDbRetryPolicy` sets on the `ADAPTIVE` path.
- Transport-level exceptions from `V2TransportBridge` are remapped by
  `ClientTransport.remapExceptions`, so what reaches the strategy is a smithy `TransportException`
  subtype, not the original v2 `SdkClientException`/`IOException`. This is worse than a lost type: the
  remapped types cannot be classified at all — see 3.6.

### 3.3 `DEGRADED` — endpoint and identity are re-resolved on every attempt

smithy's `ClientPipeline.doSendOrRetry` resolves the auth scheme, identity, and endpoint **inside**
the retry loop. v2 resolves the endpoint once per execution (in `modifyRequest`) and the identity
once per execution (in the auth-scheme interceptor). Retried calls therefore do extra work here, and
a resolver with side effects observes more invocations than under v2.

There is a cost signature consistent with this. On calls with the backoff removed
(`pipeline_benchmark2/errors/20260905-1532`) the bridge is 0.60x baseline on the once-per-execution
work but only 0.86x on each additional attempt — i.e. the advantage erodes in proportion to attempts,
which is the shape of once-per-execution work being paid per attempt. **This is a hypothesis the run
is consistent with, not one it isolates**; the run measures the aggregate. Memoizing the endpoint
bridge is the test, and 1532 is its before-measurement. A configuration allowing more than 3 attempts
would sharpen it further, since the gap should widen linearly.

### 3.4 `MISSING` — `RetryPolicy`-only configuration path

A client configured with the deprecated `overrideConfiguration().retryPolicy(...)` gets **none of
it**. v2 resolves `RETRY_STRATEGY` to a `RetryPolicyAdapter` wrapping the policy, and that adapter
cannot be bridged: `SdkRetryStrategy.of` rewires the strategy's predicates through its builder, and
`RetryPolicyAdapter.Builder` throws `UnsupportedOperationException` from exactly those setters. It
also needs a per-request `RetryPolicyContext` that smithy's pipeline never constructs.

`V2ConfigTranslator.resolveRetryStrategy` therefore returns `null` for a `RetryPolicyAdapter`, which
leaves smithy-java on its **own default `StandardRetryStrategy`**: 3 attempts (v2's `RetryPolicy`
default is 4), smithy's own backoff, and no `maxBackoffTime`/token-bucket setting the customer chose.
This is silent — no warning, no exception. `DynamoDbRetryPolicy`'s overrides (8 retries pre-2026,
25 ms base delay) are lost with it, so a `retryPolicy(...)`-configured DynamoDB client retries
*fewer* times and *faster* than v2.

`retryStrategy(...)` (the current API) is unaffected; see 3.1.

### 3.5 `FRAGILE` — v2 classifies a torn response body by its Jackson exception, which changed type in Jackson 3

v2's retryable set is `RetryableException`, `IOException`, `UncheckedIOException`,
`ApiCallAttemptTimeoutException`, matched against the exception **or any cause of it**
(`SdkDefaultRetrySetting.RETRYABLE_EXCEPTIONS`, applied via `retryOnExceptionOrCauseInstanceOf`). It
has no rule about malformed responses as such. A truncated response body is retried only as a
side effect of Jackson 2's hierarchy: `JsonEOFException` extends `JsonProcessingException` extends
`IOException`, so the parse failure lands in the retryable set on its own.

smithy-java parses with Jackson 3 (`tools.jackson`), where `JacksonException` extends
`RuntimeException`, and wraps the result in a `SerializationException` — also a `RuntimeException`.
So the two pipelines' cause chains for the *same* torn body are:

| | v2 | bridge |
|---|---|---|
| top | `SdkClientException` | `SdkClientException` |
| | `UncheckedIOException` | `TransportException` |
| | `JsonEOFException` (**an `IOException`**) | `SerializationException` |
| | | `SerializationException` |
| | | `UnexpectedEndOfInputException` (**a `RuntimeException`**) |

The chain walk that reproduces v2 for transport errors therefore found nothing to match, and
`SdkRetryStrategy`'s `YES`-only predicate (3.2) turned "not classified" into "not retried".
**Measured** (`pipeline_benchmark2/errors/20260905-0159`): 1 attempt against v2's 3, and in the
transient variant v2 returned the item while the bridge threw.

`V2ErrorEnricher.retriedByV2` now also treats a `SerializationException` as retry-safe **when a response
was received**, which reproduces v2's effective behavior (every unparseable response is retried) without
also retrying a request-serialization failure, which in v2 happens before the attempt and is never
retried. Two further changes were needed to make that reachable at all: the enricher used to return
early unless the error was a `CallException`, and a `SerializationException` is not one — it is a plain
`RuntimeException` carrying no `RetryInfo` — so the classification could not be stamped on it either.
That second half is the same defect as 3.6 and uses the same `V2RetryableError` substitution.

**Re-measured** (`pipeline_benchmark2/errors/20260905-0223`): 3 attempts against v2's 3, and the
transient variant recovers in both arms. What still differs is only the cause type — v2's
`UncheckedIOException` wrapping `JsonEOFException` against the bridge's `SerializationException` — and
the parser's message text. Both arms throw `SdkClientException`.

Filed as `FRAGILE` rather than `DEGRADED` because the mapping is a coincidence on both sides: v2's
behavior here is emergent from a third-party class hierarchy rather than intended, and the bridge's
replacement is pinned to smithy's own exception type. If either codec's exception hierarchy changes,
this silently diverges again, in whichever direction. The general shape — v2 classifying retries off
concrete exception *types* from a JSON library that smithy-java does not use — applies to anything
else that reaches v2's retryable set by inheritance rather than by declaration.

### 3.6 `BLOCKED` — transport failures are never retried

`CallException` holds retry safety in a mutable field with a setter, which is the mechanism the whole
retry bridge depends on: `V2RetryClassification` and `V2ErrorEnricher` stamp v2's verdict onto the
error, and `ClientPipeline` reads it back through `RetryInfo`. `TransportException` **overrides the
getter with a constant**, discarding that field:

```java
// TransportException (smithy-java 1.6.1); ConnectTimeoutException repeats it verbatim
public RetrySafety isRetrySafe() {
    return RetrySafety.NO;
}
```

`V2TransportBridge` funnels every `IOException` from the v2 HTTP client through
`ClientTransport.remapExceptions`, and **every one of that method's six targets descends from
`TransportException`**:

| `IOException` from the v2 HTTP client | smithy type | `isRetrySafe()` |
|---|---|---|
| `ConnectException` | `ConnectTimeoutException` | `NO` (own override) |
| `SocketTimeoutException` | `TransportSocketTimeout` → `TransportSocketException` | `NO` (inherited) |
| `SocketException` | `TransportSocketException` | `NO` (inherited) |
| `SSLException` | `TlsException` → `TransportProtocolException` | `NO` (inherited) |
| `ProtocolException` | `TransportProtocolException` | `NO` (inherited) |
| anything else | `TransportException` | `NO` (own override) |

So the setter writes a field nothing reads, and combined with `SdkRetryStrategy`'s `YES`-only predicate
(3.2) the result is categorical: **no error of any of those types is retryable in a bridged client**,
while stock v2 retries every one of them — `SdkDefaultRetrySetting.RETRYABLE_EXCEPTIONS` contains
`IOException`, matched against the whole cause chain.

**How much this covers is narrower than it looks, and measurement is what established that.**
`V2TransportBridge.send` wraps only `prepareRequest(...).call()` in its `catch (IOException)`, and that
returns as soon as the response *headers* arrive: bodies are streamed on as a `DataStream`, drained later
by the codec. So the remapping — and this defect — is reachable only for failures during **connect, TLS
handshake, request send, or response-header read**. Anything that fails once a response has begun
arriving, including a torn response body, never becomes a `TransportException` at all; it surfaces as
whatever the stream or codec threw, and is handled by 3.5's rule instead. See
`errors/20260905-0230`, where a truncated body produced an identical `UncheckedIOException` cause and an
identical 3 attempts in both arms.

That narrower surface is not a reprieve. A connection reset before headers is the most common transport
failure in production — an idle pooled connection reaped by a load balancer is exactly this — and connect
timeouts are the next.

**The hardcoded getter is the symptom. The cause is that the retry loop is inside `deserialize`.**
Trying to fix this from the bridge is what established that, and the attempt is worth recording because
the obvious mitigation looks correct and is inert.

Everywhere else in section 3, a failure that smithy will not classify can be replaced by one that will:
`V2ErrorEnricher` throws a `V2RetryableError` — a `CallException` that returns `YES` — carrying the
original, which the client boundary unwraps once retries are over. It detects the need by *writing then
reading back* `isRetrySafe` rather than by naming the offending classes, so a new upstream type with the
same override is handled without a code change. That is what fixes 3.5.

It cannot work here, for two reasons, and the second is fatal:

1. The enricher never runs. It hooks `modifyBeforeAttemptCompletion`, and `ClientPipeline` invokes that
   in exactly one place — inside `deserialize`. `afterIdentity` wraps both calls in one try:

   ```java
   try {
       ResponseT response = transport.send(context, request);
       return deserialize(call, request, response, interceptor);
   } catch (Exception e) {
       throw ClientTransport.remapExceptions(e);   // a send failure skips deserialize entirely
   }
   ```

2. **Neither does the retry loop.** `refreshRetryToken` and the recursive `retry(...)` call live inside
   `deserialize` too, in the `catch (RuntimeException)` around `deserializeResponse`. `doSendOrRetry`, in
   spite of its name, contains no exception handler at all, and neither does anything above it. So the
   only code path that can produce a second attempt is one that got far enough to have a response to
   deserialize.

Which means **smithy-java 1.6.1 does not retry transport failures at all** — not because they are
classified `NO`, but because nothing on that path ever consults the classification. The hardcoded getter
is consistent with the design rather than an oversight in it. A native smithy-java client has the same
behavior; this is not a bridging artifact.

The bridge has no fix. `V2TransportBridge.send` throwing a `V2RetryableError` instead of the remapped
exception was implemented and measured: **no change, 1 attempt**, because there is no handler between
that throw and the caller. It was reverted, and the catch clause now carries a comment saying so, since
it is exactly the change a later reader would try. The remaining options are all above the pipeline —
wrapping `Client.call` in a retry loop of the bridge's own, which would duplicate the strategy, the token
bucket, and the attempt accounting — and none belong in a compatibility layer.

`BLOCKED`, and the most consequential entry in this ledger for a real deployment.

`BLOCKED` because the underlying defect is upstream: a transport error that cannot be marked retryable
is not a property a bridge should have to work around, and the fix belongs in smithy-java — either drop
the overrides or make the constructors take a `RetrySafety`. Until then any smithy-java client, bridged
or native, has the same behavior.

**Measured** (`pipeline_benchmark2/errors/20260905-0239`) with the `connection-reset` fault, added to
`Faults` for this: the server closes the socket with no response at all, which per the paragraph above is
the only way to make the failure land inside the transport. Attempt counts come from the server's own
request counter, since a transport failure yields an exception with no attempt number on it.

| | v2 | bridge (before the fix) |
|---|---|---|
| attempts, persistent | 3 | **1** |
| attempts, transient (3rd would succeed) | 3 | **1** |
| outcome, transient | **ok**, item returned | **throw** |
| wall, persistent | 131 ms | 2 ms |

The transient row is the whole finding: v2 recovers and returns the item where the bridge throws. The 2 ms
wall is the same fact as a timing — no backoff, because no second attempt.

---

## 4. Authentication and credentials

### 4.1 `TRANSLATED` — auth scheme resolution

v2 resolves auth schemes with a generated `ExecutionInterceptor` driven by an
`AuthSchemeProvider` and the model's `@auth` traits, and can select sigv4a, bearer, or `noAuth`
per operation. The bridge installs smithy's `SigV4AuthScheme` and lets
`AuthSchemeResolver.DEFAULT` pick from `ApiOperation.effectiveAuthSchemes()`.

`ApiOperationSpec` currently hardcodes `SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"))` for every
operation. Consequences:

- A customer-supplied `authSchemeProvider(...)` has no effect.
- `@optionalAuth` / `@auth([])` operations are still signed.
- sigv4a is not available; a service or endpoint requiring it would fail. (Not reachable for
  DynamoDB.)
- Endpoint-driven auth-scheme overrides *are* honored, because smithy's
  `applyEndpointAuthSchemeOverrides` reads `Endpoint.authSchemes()` — but the bridged endpoint
  resolver does not translate v2's `AwsEndpointAttribute.AUTH_SCHEMES` into smithy
  `EndpointAuthScheme`s, so in practice they are dropped.

### 4.2 `BRIDGED` — credentials

`V2IdentityResolver` adapts v2's `IdentityProvider<? extends AwsCredentialsIdentity>` to smithy's
`IdentityResolver<AwsCredentialsIdentity>`. This is deliberately bridged rather than translated:
smithy-java's `aws-credential-chain` ships only environment-variable and system-property resolvers,
whereas v2's `DefaultCredentialsProvider` chain covers profile files, SSO, container credentials,
IMDS, process credentials, web identity, and STS. Preserving that chain is the whole point.

Residual: v2's `resolveIdentity(ResolveIdentityRequest)` properties are not forwarded; the bridge
calls the no-arg overload. Credential-scoped properties from an auth scheme (e.g. S3 Express)
therefore do not reach the provider.

### 4.3 `TRANSLATED` — SigV4 signing

smithy's `SigV4Signer` replaces v2's `AwsV4HttpSigner`. Region and signing name come from
`SdkClientConfiguration` (`AwsClientOption.SIGNING_REGION`, `SERVICE_SIGNING_NAME`) into
`SigV4Settings.REGION` / `SIGNING_NAME`. This is the single largest allocation win measured on the
prototype.

Differences:

- `SdkAdvancedClientOption.SIGNER` and any customer-supplied `HttpSigner`/`AuthScheme` are ignored.
- v2's signer properties (`AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME`,
  `DOUBLE_URL_ENCODE`, `NORMALIZE_PATH`, `PAYLOAD_SIGNING_ENABLED`, `CHUNK_ENCODING_ENABLED`,
  `EXPIRATION_DURATION`) are not translated. Defaults happen to match for DynamoDB.
- The set of headers excluded from the canonical request is smithy's, not v2's. Any divergence shows
  up as a signature mismatch only if a customer adds one of the differing headers.

### 4.4 `MISSING` — anonymous credentials

`AnonymousCredentialsProvider` short-circuits signing in v2. The bridge always installs
`SigV4AuthScheme`, and `IdentityResolver` would return an `AnonymousIdentity` that
`SigV4Signer` cannot use.

---

## 5. Endpoints

### 5.1 `BRIDGED` — endpoint resolution

`V2EndpointResolverBridge` implements smithy's `EndpointResolver` by calling the **generated v2
rules engine**: `DynamoDbResolveEndpointInterceptor.ruleParams(request, executionAttributes)`
followed by `DynamoDbEndpointProvider.resolveEndpoint(params).join()`. This preserves
`endpointOverride`, FIPS, dual-stack, account-ID-based routing, `ResourceArn`-driven routing,
client context params, static/operation context params, and any customer-supplied
`EndpointProvider`.

The account-ID builtin works because smithy resolves the identity *before* the endpoint
(`ClientPipeline.afterIdentity`), so the bridge reads the resolved `AwsCredentialsIdentity` out of
`CallContext.IDENTITY` and synthesizes the `SelectedAuthScheme` that `ruleParams` expects.

Residual differences:

- The `ExecutionAttributes` handed to `ruleParams` is synthesized from client config and contains
  only what the rules engine needs. An `EndpointProvider` reading other attributes sees `null`.
- `AwsEndpointAttribute.AUTH_SCHEMES` returned by the rules engine is not translated (see 4.1).
- Endpoint headers (`Endpoint.headers()`, applied by v2's `modifyHttpRequest`) are dropped.
- Business metrics recorded during endpoint resolution go into a throwaway collection.
- `ENDPOINT_RESOLVE_DURATION` is not reported (no metrics — see 7).
- **Cost, not just behavior:** every resolution allocates an `ExecutionAttributes` copy (a `HashMap`),
  a `BusinessMetricCollection`, a v2 `AwsCredentialsIdentity` converted from smithy's, a
  `SelectedAuthScheme` holding an already-completed `CompletableFuture`, the generated rule-params
  builder and params object, the `CompletableFuture` that `resolveEndpoint` returns and that is
  immediately `join()`ed, and a v2 `Endpoint`. A single-entry memo keyed on the resolved URI keeps the
  smithy `Endpoint` itself from being rebuilt, which is the only part of this that steady state avoids.
  All of it is on the hot path and runs **once per attempt** rather than once per execution (3.3). The
  `awssdk.bridge.stripEndpoints` knob (10.6) exists to measure exactly this, and it comes to **4.0% of
  app CPU on `small-get`, 5.1% on `small-put`** (4/4 paired wins,
  `pipeline_benchmark2/components/20260904-2313`) — the second-largest of the four bridges, behind the
  interceptor bridge (2.1).

### 5.2 `MISSING` — endpoint discovery

DynamoDB's `customization.config` sets `enableEndpointDiscoveryMethodRequired: true`. The
`EndpointDiscoveryRefreshCache`, the `DescribeEndpoints` cache-fill call, and
`AwsEndpointProviderUtils.endpointIsDiscovered` short-circuit are all bypassed. A client built with
`endpointDiscoveryEnabled(true)` silently uses the regional endpoint.

The generated client still *creates* the `EndpointDiscoveryRefreshCache` in its constructor when
discovery is enabled (including via the `AWS_ENABLE_ENDPOINT_DISCOVERY` env var / profile property,
which the builder still resolves), but no operation ever consults it: the constructor holds the only
reference to the field. The knob is fully wired and completely inert.

### 5.3 `DEGRADED` — `smithy.rules#endpointBdd` ignored

The prototype codegen ignores the `endpointBdd` trait and relies on the C2J
`endpoint-rule-set.json` sidecar. Any service whose authoritative rules are BDD-only would resolve
differently. Not an issue for DynamoDB, where both forms exist and agree.

### 5.4 `MISSING` — host prefixes / `@endpoint` trait

Neither smithy's `HostLabelEndpointResolver` (no traits on generated schemas, see 1.2) nor the
bridged v2 path (`AwsEndpointProviderUtils.addHostPrefix` lives in the interceptor's
`modifyRequest`, not in `ruleParams`) applies host prefixes. No DynamoDB operation uses one.

---

## 6. Checksums, compression, and content encoding

### 6.1 `MISSING` — CRC32 response validation

DynamoDB sets `calculateCrc32FromCompressedData: true`. v2 validates the `x-amz-crc32` response
header against the received body and throws on mismatch. smithy-java's `HttpChecksumPlugin` covers
request checksums for `@httpChecksum`-modeled operations; nothing validates DynamoDB's legacy CRC32
response header. **Corrupted responses are accepted silently.**

### 6.2 `MISSING` — request compression

`RequestCompressionTrait` / `@requestCompression` is not applied. smithy has
`RequestCompressionPlugin` but it is trait-driven, and generated schemas have no traits (1.2).
No DynamoDB operation is affected.

### 6.3 `MISSING` — `gzip` response decoding negotiation

v2's DynamoDB client sends `Accept-Encoding: gzip` and decodes. The smithy path does not.

---

## 7. Metrics

### 7.1 `MISSING` — all of v2's metrics

`MetricPublisher`, `CoreMetric` (`API_CALL_DURATION`, `SERVICE_CALL_DURATION`, `RETRY_COUNT`,
`ENDPOINT_RESOLVE_DURATION`, `MARSHALLING_DURATION`, `SIGNING_DURATION`, `CREDENTIALS_FETCH_DURATION`,
`AVAILABLE_CONCURRENCY`, …) and `HttpMetric` are not collected or published.

Worse than silence: the generated operation method still resolves the configured publishers, creates
an `"ApiCall"` `MetricCollector`, reports `SERVICE_ID` and `OPERATION_NAME` into it, and publishes it
in a `finally` block. So a customer with a CloudWatch publisher configured gets a metric record per
call containing **only those two dimensions and no measurements** — an empty datum rather than no
datum, which is harder to notice than an outright gap. It also means the publisher cost is still paid
on the hot path.

smithy-java has its own metrics story (`client-metrics-otel`); nothing translates between the two
vocabularies.

### 7.2 `MISSING` — business metrics / user-agent feature IDs

v2 stamps feature IDs into the `User-Agent` (`m/...`) from `SdkInternalExecutionAttribute.BUSINESS_METRICS`.
smithy has its own `CallContext.FEATURE_IDS`, but the two vocabularies are unrelated and nothing
translates between them. The emitted user agent will not match v2's, which affects AWS-side
telemetry and any customer parsing it.

---

## 8. Timeouts

### 8.1 `MISSING` — `apiCallTimeout`

v2 enforces a whole-call deadline with a scheduled interrupt (`ApiCallTimeoutTracker`). smithy-java
has no equivalent. A hung call is bounded only by the HTTP client's socket timeout.

### 8.2 `MISSING` — `apiCallAttemptTimeout`

Same, per attempt.

Both are configured on `overrideConfiguration()` and are commonly used in production. This is the
most likely of the `MISSING` items to be a launch blocker.

---

## 9. Configuration surface not translated

Read once from `SdkClientConfiguration` and honored: region, signing name, endpoint override,
`SYNC_HTTP_CLIENT`, credentials provider, retry strategy, execution interceptors, endpoint provider.

Silently ignored:

| v2 configuration | Notes |
|---|---|
| `SdkAdvancedClientOption.SIGNER` | see 4.3 |
| `SdkAdvancedClientOption.USER_AGENT_PREFIX` / `USER_AGENT_SUFFIX` | smithy's `UserAgentPlugin` builds its own string |
| `SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION` | nothing injects host prefixes anyway (5.4) |
| `overrideConfiguration().headers(...)` / `putHeader(...)` | client-level extra headers dropped |
| `overrideConfiguration().compressionConfiguration(...)` | see 6.2 |
| `overrideConfiguration().scheduledExecutorService(...)` | only used by timeouts (8) |
| `overrideConfiguration().defaultProfileFile/Name` | used indirectly via `RetryMode` resolution only |
| `dualstackEnabled` / `fipsEnabled` | **honored**, via the bridged rules engine (5.1) |
| `accountIdEndpointMode` | **honored**, via the bridged rules engine (5.1) |
| `responseChecksumValidation` / `requestChecksumCalculation` | see 6.1 |
| `SdkClientOption.API_CALL_ATTEMPT_TIMEOUT`, `API_CALL_TIMEOUT` | see 8 |
| `overrideConfiguration().retryPolicy(...)` | **silently downgraded** to smithy's default strategy — see 3.4 |
| `overrideConfiguration().metricPublishers(...)` | publisher runs, but the record is empty — see 7.1 |
| `endpointDiscoveryEnabled` | resolved and stored, never consulted — see 5.2 |
| request-level `overrideConfiguration()` | ignored except for metric publishers — see 2.3 |

---

## 10. Structural / build-level

### 10.1 `BLOCKED` — Java baseline

smithy-java 1.6.1 requires **JDK 21** (sealed interfaces, records, pattern matching in the runtime
it ships). The AWS SDK for Java v2 supports **Java 8**. Adopting smithy-java as v2's internals would
be a breaking platform change for the entire SDK, independent of any behavioral issue in this
ledger. This branch builds at `jre.version=21`.

### 10.2 out of scope — async clients

`SdkAsyncHttpClient` / `CompletableFuture` operations, and therefore
`DynamoDbAsyncClient`, are untouched. smithy-java's async client path exists but the transport
bridge here is sync-only.

### 10.3 out of scope — streaming and event streams

Operations with streaming input or output fall back to the v2 pipeline
(`JsonProtocolSpec.executionHandler` only diverts non-streaming operations). No DynamoDB operation
streams, so the whole API is on the smithy path.

### 10.4 prototype-only codegen shortcuts

Carried over from the earlier branches, not compatibility issues per se but they limit what can be
concluded: `ALLOW_UNKNOWN_TRAITS`, `-Dawssdk.codegen.skipValidation=true`, C2J sidecars for endpoint
rules / paginators / waiters.

### 10.5 `MISSING` — paginators, waiters, utilities

`*Paginator` types, waiters, and `DynamoDbClient.serviceClientConfiguration()` reflect the v2
configuration objects, not the smithy `ClientConfig`. Paginators call the same operation methods so
they work, but nothing reconciles the two configuration views.

### 10.6 measurement knobs that change behavior

`V2ConfigTranslator` reads six system properties at client construction. Five replace one bridge with
smithy-java's own component so that "what does bridging cost?" can be answered from one binary and one
code path; the sixth restores a behavior the bridge now optimizes away, for the same reason.

| Property | Effect |
|---|---|
| `awssdk.bridge.stripAll` | implies all of the strip properties below |
| `awssdk.bridge.stripEndpoints` | fixed client endpoint instead of the bridged rules engine (5.1) |
| `awssdk.bridge.stripRetries` | smithy's `StandardRetryStrategy` instead of the configured v2 strategy (3.1) |
| `awssdk.bridge.stripInterceptors` | v2 interceptors not run at all (2.1) |
| `awssdk.bridge.stripErrorEnricher` | no AWS error code / request ID / status code on exceptions, and therefore no v2-equivalent retry classification (1.4, 3.2) |
| `awssdk.bridge.keepInertInterceptors` | bridge interceptors that provably cannot act, instead of dropping them (2.1) |

These are **not** a supported feature and they are listed here because they are behavior-changing: a
stripped client is a different SDK, not a faster one. They are read once per client construction,
never on the call path. The benchmark's `v2-sync-stripped` arm sets `stripAll`, and
`v2-sync-inert-interceptors` sets `keepInertInterceptors`.

Also worth recording: the generated v2 response/error handlers
(`createErrorResponseHandler`, the protocol factory's `createResponseHandler`) are still emitted but
unused for smithy-path operations — dead code that makes the generated client look more v2-like than
it is.

### 10.7 `DEGRADED` — client construction loads both pipelines, including `smithy-model`

Measured cold start (`pipeline_benchmark2/coldstart/20260904-2341`): building a `DynamoDbClient` takes
**507 ms on stock v2 and 603 ms here (+22%)**, which is the whole of the bridged client's +10% time to
first response — its first *call* is 20% faster than v2's. For a Lambda or a CLI making a handful of
calls, this is the tradeoff that matters, and it goes the wrong way.

`-verbose:class` on one JVM of each: 4 534 classes loaded versus 4 354, i.e. 534 loaded that stock v2
does not load and 385 no longer loaded. The largest bridge-only groups:

| classes | package | what it is |
|---:|---|---|
| 100 | `smithy.java.internal` | smithy runtime internals |
| 80 | `smithy.java.client` | the client pipeline |
| **114** | **`smithy.model.shapes` + `smithy.model.traits`** | **the Smithy *model* library** |
| 56 | `smithy.java.core` | schemas, serde |
| 46 + 26 | `smithy.java.json`, `awssdk.bridge.smithyjava` | protocol, the bridges themselves |

The 114 `smithy.model` classes are the interesting ones: the generated `SCHEMAS` constants build
`ShapeId`s and trait objects in static initializers, so a *runtime* client drags in the Smithy
model/traits library — machinery meant for build-time model processing. That is a plausible target for
the construction cost, and it is a dependency-surface question as much as a performance one.

Not attributed further here: whether the +95 ms is dominated by class loading, static initialization,
or the bridge objects. A JFR class-load recording on a single JVM would settle it.

---

## 11. Protocol layer

### 11.1 `DEGRADED` — the awsJson target header comes from the *generated service shape name*

`AwsJsonProtocol.createRequest` builds `X-Amz-Target` as
`serviceShapeId.getName() + "." + operationShapeId.getName()`. There is no `@awsJson1_0` target-prefix
trait involved and no way to override it: the header is a pure function of the two generated shape
names.

That makes a codegen naming choice load-bearing on the wire. The first version of `ApiServiceSpec`
used the SDK's Java-friendly service name and produced `X-Amz-Target: DynamoDb.GetItem`, which
DynamoDB answers with **HTTP 400 for every operation**. The fix was to carry the C2J
`metadata.targetPrefix` through `Metadata`/`AddMetadata` and name the service shape after it, so the
generated shape is `com.amazonaws.dynamodb#DynamoDB_20120810`.

Consequences to keep in mind:

- The service shape *name* cannot be chosen for readability; for any awsJson service it must equal
  `targetPrefix`, which for most services does not look like a shape name at all.
- Operation shape names must equal the C2J operation names exactly. Any codegen renaming
  (`customization.config` `renameShapes`, `verbNameOverrides`, or the operation-name sanitizing v2 does
  elsewhere) would silently change the target header.
- The failure mode is a blanket 400 with no hint about the cause, and it is invisible to any test that
  does not talk to a server that validates the target — the mock server in
  `test/standalone-e2e-benchmarks` catches it only because it routes on `X-Amz-Target` and 400s unknown
  targets.
- Services with no `targetPrefix` (non-awsJson protocols) fall back to the service name; harmless
  today because the header is unused there, but it is a latent trap if another protocol is added.

---

## Open questions

1. Does `SigV4Signer`'s canonical-header exclusion list match v2's `AwsV4HttpSigner` exactly? A
   signature-comparison test against a fixed clock and fixed credentials would settle it.
2. Is `ClientPipeline`'s per-attempt endpoint/identity re-resolution a measurable cost at
   concurrency 1, or is it noise? (Benchmark: retry-free path, so probably invisible — but it shows
   up under throttling.)
3. `apiCallTimeout` (8.1) has no smithy concept at all. Would adding one upstream be accepted, or
   does the bridge have to own it?
4. Can `ClientPipeline` resolve the endpoint *before* `modifyBeforeSigning` (2.1)? Every alternative
   the bridge has is worse: showing interceptors a URI with no host, showing them the client endpoint
   and lying, or adding a second smithy hook after `setServiceEndpoint` that v2's chain has no
   position for. This is the one ordering difference that cannot be papered over in the bridge, and it
   silently breaks a documented v2 capability (redirecting a request in `modifyHttpRequest`).
5. Is the `X-Amz-Target`-from-shape-name coupling (11.1) something smithy-java would take a trait or
   protocol setting for? Today it makes generated shape names part of the wire contract for every
   awsJson service.
6. Do the `v2-sync-strip-*` arms resolve individually above the noise floor, or is the bridging tax
   only measurable in aggregate (`v2-sync-stripped`)? If only in aggregate, per-component attribution
   needs allocation profiling rather than timing.
