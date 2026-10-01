# How the smithy-java SPI bridge works

This branch (`smithy-java-spi-bridge-poc`) takes a different route from `smithy-java-bridge-alexwoo-full`.
Instead of codegen wiring each client straight into a smithy-java `Client`, `sdk-core` gains a small
execution-pipeline SPI. The generated sync client asks a `ServiceLoader` for an `SdkPipeline` at
construction, and per call hands the request to it if the pipeline claims the operation. The
`smithy-java-bridge` module registers a provider whose pipeline runs smithy-java's `Client.call`.

The v2 public API is unchanged. Only DynamoDB's sync client is wired; the async client is stock v2.

```
DynamoDbClient.getItem(req)                       DynamoDbAsyncClient.getItem(req)
  │                                                 └─ stock v2 clientHandler (no SPI wiring, §7)
  ├─ v2 per-call setup still runs: response/error handlers, request plugins, endpoint discovery, metrics
  ├─ sdkPipeline.supportsOperation(params)?  ── yes ──► SmithyJavaPipeline.execute(params, config)
  │                                                       └─ BridgeClient.callOperation    // smithy-java Client.call
  │                                                            ├─ protocol:  ProtocolResolver → AwsJson1Protocol (per-call override)
  │                                                            ├─ identity:  V2CredentialsBridge → v2 AwsCredentialsProvider
  │                                                            ├─ endpoint:  static, from CLIENT_ENDPOINT_PROVIDER
  │                                                            ├─ signing:   smithy-java SigV4AuthScheme
  │                                                            ├─ retries:   smithy StandardRetryStrategy(maxAttempts from v2)
  │                                                            ├─ interceptors: one FullV2InterceptorBridge per v2 interceptor
  │                                                            └─ transport: V2TransportBridge → v2 SdkHttpClient
  └─ no ──► hard-wired Phase-2 path: createRequest → V2TransportBridge.send → deserializeResponse  (not v2)
```

Snippets are trimmed from DynamoDB sources regenerated from this branch's codegen (2026-10-01) and from
`core/sdk-core` and `core/smithy-java-bridge`. Statements marked "probe" come from a throwaway test that
drove a real `DynamoDbClient` against a local Jetty server and recorded the wire and interceptor
activity. The probe was deleted afterwards; see §8 for what it found.

---

## 1. Client operation method

### The SPI contracts (`core/sdk-core`)

Three types in `software.amazon.awssdk.core.client.handler`, with no smithy-java dependency:

```java
public interface SdkPipeline extends SdkAutoCloseable {
    <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        SdkClientConfiguration clientConfiguration);

    default <InputT extends SdkRequest, OutputT extends SdkResponse> boolean supportsOperation(
        ClientExecutionParams<InputT, OutputT> executionParams) {
        return true;
    }
}

public interface SdkPipelineProvider {
    int priority();                                   // lowest wins
    default boolean isAvailable() { return true; }
    SdkPipeline createPipeline(SdkClientConfiguration clientConfiguration);   // once per client
}
```

`SdkPipelineLoader` is the discovery logic. An explicit `SdkClientOption.SDK_PIPELINE` short-circuits it;
otherwise it loads every provider, sorts by priority, and returns the first available one that builds
without throwing:

```java
// SdkPipelineLoader.loadPipeline
SdkPipeline explicitPipeline = clientConfiguration.option(SdkClientOption.SDK_PIPELINE);
if (explicitPipeline != null) {
    return Optional.of(explicitPipeline);
}
List<SdkPipelineProvider> providers = discoverProviders();          // ServiceLoader, every client build
providers.sort(Comparator.comparingInt(SdkPipelineProvider::priority));
for (SdkPipelineProvider provider : providers) {
    if (provider.isAvailable()) {
        try {
            return Optional.of(provider.createPipeline(clientConfiguration));
        } catch (Exception e) {
            log.warn(() -> "... failed to create pipeline, trying next provider.", e);
        }
    }
}
return Optional.empty();
```

`SDK_PIPELINE` has no public builder setter. The benchmark sets it by reflecting into the builder's
`clientConfiguration` field.

The bridge registers `SmithyJavaPipelineProvider` in
`META-INF/services/software.amazon.awssdk.core.client.handler.SdkPipelineProvider`. It has priority 0 and
is available whenever smithy-java's `SerializableStruct` class loads.

The intent is opt-in by classpath. In practice `services/dynamodb/pom.xml` declares `smithy-java-bridge`
as a compile dependency, so every DynamoDB user gets the provider.

### The operation body

`SyncClientClass` adds the SPI check to every non-streaming, non-event-stream operation, just before the
protocol's execution handler. Everything v2 builds before that point still runs on every call:

```java
// DefaultDynamoDbClient.getItem (generated)
JsonOperationMetadata operationMetadata = ...;
HttpResponseHandler<GetItemResponse> responseHandler = protocolFactory.createResponseHandler(...);
Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> { switch (errorCode) { ... } };
HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(...);
if (endpointDiscoveryEnabled) { ... cachedEndpoint = endpointDiscoveryCache.get(key, endpointDiscoveryRequest); }
SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(getItemRequest, this.clientConfiguration);
List<MetricPublisher> metricPublishers = resolveMetricPublishers(...);
MetricCollector apiCallMetricCollector = ...;
try {
    apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "DynamoDB");
    apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetItem");
    ClientExecutionParams<GetItemRequest, GetItemResponse> pipelineExecutionParams =
            new ClientExecutionParams<GetItemRequest, GetItemResponse>()
                    .withOperationName("GetItem").withProtocolMetadata(protocolMetadata).withInput(getItemRequest)
                    .withMetricCollector(apiCallMetricCollector).withRequestConfiguration(clientConfiguration);
    if (sdkPipeline != null && sdkPipeline.supportsOperation(pipelineExecutionParams)) {
        return sdkPipeline.execute(pipelineExecutionParams, clientConfiguration);
    }

    // Fallback: the Phase-2 hard-wired smithy-java path, not v2.
    Context smithyContext = Context.create();
    ApiOperation<SerializableStruct, SerializableStruct> op = (ApiOperation) GetItemOperation.instance();
    HttpRequest httpRequest = smithyProtocol.createRequest(op, (SerializableStruct) getItemRequest, smithyContext,
            smithyEndpoint);
    HttpResponse httpResponse = smithyTransport.send(smithyContext, httpRequest);
    try {
        return (GetItemResponse) smithyProtocol.deserializeResponse(op, smithyContext, op.errorRegistry(),
                httpRequest, httpResponse);
    } catch (CallException e) {
        if (e.getCause() instanceof SdkServiceException) throw (SdkServiceException) e.getCause();
        throw DynamoDbException.builder().message(e.getMessage()).cause(e).build();
    }
} finally {
    metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
}
```

Two things about that fallback:

- The SPI commit was layered on the Phase-2 commit rather than replacing it. `JsonProtocolSpec.executionHandler`
  returns the hard-wired smithy block for `generateSmithyJavaSerde` services, so `clientHandler.execute`
  appears zero times in `DefaultDynamoDbClient`. The fallback is a bare
  serialize → send → deserialize with no signing, retries or interceptors.
- `DynamoDbException` is hardcoded in `JsonProtocolSpec`, so the block only compiles for DynamoDB.

Because of the fallback, the generated client also carries smithy-java fields and imports
(`AwsJson1Protocol smithyProtocol`, `ClientTransport smithyTransport`, `SmithyUri smithyEndpoint`), contrary
to the spec's "only sdk-core" goal.

### Building the pipeline (once, in the constructor)

```java
// DefaultDynamoDbClient constructor (generated)
this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
this.clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.SDK_CLIENT, this)...build();
this.sdkPipeline = SdkPipelineLoader.instance().loadPipeline(this.clientConfiguration).orElse(null);
...
this.smithyProtocol = new AwsJson1Protocol(ShapeId.from("com.amazonaws.dynamodb#DynamoDB_20120810"));
this.smithyTransport = new V2TransportBridge(clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT));
this.smithyEndpoint = SmithyUri.of(clientConfiguration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER)
        .clientEndpoint().toString());
```

`SmithyJavaPipeline`'s constructor reads the client-level v2 config once and builds a smithy `Client`
subclass, `BridgeClient`:

```java
// SmithyJavaPipeline.buildSmithyClient
ClientTransport<HttpRequest, HttpResponse> transport = new V2TransportBridge(httpClient);           // §4
V2CredentialsBridge credentialsBridge = new V2CredentialsBridge(credentialsProvider);              // §5
SigV4AuthScheme sigV4AuthScheme = new SigV4AuthScheme(signingName != null ? signingName : "service");
EndpointResolver endpointResolver = resolveEndpoint(config);                                       // static, below
V2RetryBridge retryBridge = V2RetryBridge.fromV2Config(config);
RetryStrategy smithyRetryStrategy = StandardRetryStrategy.builder()
        .maxAttempts(retryBridge.maxAttempts())                                                    // §1 retries
        .build();

BridgeClient.BridgeBuilder builder = BridgeClient.builder();
builder.transport(transport);
builder.addIdentityResolver(credentialsBridge);
builder.putSupportedAuthSchemes(sigV4AuthScheme);
builder.endpointResolver(endpointResolver);
builder.retryStrategy(smithyRetryStrategy);
builder.putConfig(RegionSetting.REGION, region.id());
builder.putConfig(SigV4Settings.SIGNING_NAME, signingName);
for (ExecutionInterceptor v2Interceptor : v2Interceptors) {
    if (!isInternalInterceptor(v2Interceptor)) {                                                   // §6
        builder.addInterceptor(new FullV2InterceptorBridge(v2Interceptor));
    }
}
builder.disableAutoPlugins();
return builder.build();
```

If the config has no `SYNC_HTTP_CLIENT` or no `CLIENT_ENDPOINT_PROVIDER`, the constructor throws.
The loader catches that and returns empty, which sends calls down the hard-wired path. A v2 builder always
sets both, so this doesn't happen for DynamoDB.

`BridgeClient` is built with a placeholder `ApiService` (`aws.sdk#BridgeService`) and a placeholder
protocol that throws if used. Both are needed because `ClientConfig` requires non-null values. The real
protocol is supplied per call (§2).

#### Endpoint

The endpoint is static: the URI from `CLIENT_ENDPOINT_PROVIDER`, resolved once per client.

```java
URI endpointUri = config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).clientEndpoint();
return EndpointResolver.staticEndpoint(Endpoint.builder().uri(endpointUri).build());
```

The v2 endpoint rules provider is never called. Rules-driven behavior (FIPS, dual-stack, account-ID
endpoints) is lost. A request-level `endpointProvider` is ignored, and so is an endpoint-discovery
result: the generated method still computes `cachedEndpoint`, but nothing passes it to the pipeline.

#### Retries

`V2RetryBridge` takes one number from the v2 config, the max attempt count. It reads it from
`RETRY_STRATEGY`, falls back to `RETRY_POLICY`, and defaults to 3. v2's `RetryStrategy` is not invoked,
so its backoff, token bucket and error classification are not used. The bridge's
`isThrottlingError`/`isTransientError` helpers have no caller.

smithy-java only retries an error it considers retry-safe. Its default `ApplyModelRetryInfoPlugin` marks
an error safe when the operation schema has `@readonly` or `@idempotent`, or when the error has
`@retryable`. Smithy transport exceptions report `RetrySafety.NO`. The generated schemas carry none of
those traits (§3), so nothing is ever retried. Probe: a 500 and a `ThrottlingException` each got exactly
one attempt.

The E2E test `retryBehavior_transientError_retriesAndSucceeds` still passes. It catches the
`SdkServiceException` and accepts one attempt, so it doesn't establish that retries work.

#### Timeouts

`API_CALL_TIMEOUT` and `API_CALL_ATTEMPT_TIMEOUT` are not read. smithy-java has no timeout concept, so
the only limits on this path are the HTTP client's own socket timeouts.

### `SmithyJavaPipeline.supportsOperation` / `execute`

An operation is claimed if its input implements `SerializableStruct` and a generated `<Op>Operation`
class exists. The class is found by naming convention and reflection, then cached:

```java
// SmithyJavaPipeline.resolveOperation
String cacheKey = input.getClass().getPackageName() + "#" + operationName;
return operationCache.computeIfAbsent(cacheKey, k -> {
    // "...dynamodb.model" -> "...dynamodb.operations.GetItemOperation"
    String operationClassName = servicePackage + ".operations." + operationName + "Operation";
    Class<?> opClass = Class.forName(operationClassName, true, input.getClass().getClassLoader());
    return (ApiOperation<SerializableStruct, SerializableStruct>) opClass.getMethod("instance").invoke(null);
});
```

`execute` picks the protocol, passes it as a per-call override, and calls smithy's protected `call`:

```java
ClientProtocol<HttpRequest, HttpResponse> resolvedProtocol =
        protocolResolver.resolve(operation, executionParams.getProtocolMetadata());
RequestOverrideConfig overrideConfig = RequestOverrideConfig.builder().protocol(resolvedProtocol).build();
try {
    return (OutputT) smithyClient.callOperation((SerializableStruct) input, operation, overrideConfig);
} catch (CallException e) {
    if (e.getCause() instanceof SdkServiceException) {
        throw (SdkServiceException) e.getCause();
    }
    throw SdkServiceException.builder().message(e.getMessage()).cause(e).build();
}

// BridgeClient
<I extends SerializableStruct, O extends SerializableStruct> O callOperation(
        I input, ApiOperation<I, O> operation, RequestOverrideConfig overrideConfig) {
    return call(input, operation, overrideConfig);
}
```

### Request-level overrides

None are applied. The client-level `BridgeClient` is built once. `execute` ignores both its
`clientConfiguration` argument (the per-request config from `updateSdkClientConfiguration`, including
request plugins) and the request's `overrideConfiguration()`.

| Request override field | Handled by |
|---|---|
| `credentialsProvider` | ignored. Probe: signed with the client's key, not the request's |
| `headers`, `rawQueryParameters` | ignored. Probe: `putHeader` value absent on the wire |
| `plugins` | run by `updateSdkClientConfiguration`, result discarded |
| `endpointProvider`, `authSchemeProvider`, `signer` | ignored |
| `executionAttributes` | ignored (§6) |
| `apiCallTimeout`, `apiCallAttemptTimeout` | ignored |
| `metricPublishers` | honored by the generated method (§8) |

---

## 2. Input/output translation

As on the full branch, requests and responses aren't copied or wrapped. The v2 POJO implements smithy's
`SerializableStruct`, and its `BuilderImpl` implements `ShapeBuilder`. The generated `ApiOperation`
hands those v2 types to the pipeline:

```java
// operations/GetItemOperation (generated)
public final class GetItemOperation implements ApiOperation<GetItemRequest, GetItemResponse> {
    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#GetItem"));
    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    @Override public ShapeBuilder<GetItemRequest>  inputBuilder()  { return GetItemRequest.builder(); }
    @Override public ShapeBuilder<GetItemResponse> outputBuilder() { return GetItemResponse.builder(); }
    @Override public Schema inputSchema()  { return GetItemRequest.$SCHEMA; }
    @Override public Schema outputSchema() { return GetItemResponse.$SCHEMA; }
    @Override public TypeRegistry errorRegistry() { return TYPE_REGISTRY; }
    @Override public List<ShapeId> effectiveAuthSchemes() { return SCHEMES; }
    @Override public ApiService service() { return DynamoDbApiService.instance(); }
}
```

`supportsOperation` casts the `SdkRequest` to `SerializableStruct`, and `Client.call` returns a
`GetItemResponse` directly.

### Protocol selection: `ProtocolResolver`

The generated `ApiService` schema has no protocol trait, so `ProtocolResolver` falls back to v2's
`AwsProtocolMetadata`. It caches one protocol instance per service shape ID:

```java
private ClientProtocol<HttpRequest, HttpResponse> createFromServiceProtocol(AwsServiceProtocol p, ShapeId id) {
    switch (p) {
        case AWS_JSON:                  return new AwsJson1Protocol(id);
        case REST_JSON:                 return new RestJsonClientProtocol(id);
        case REST_XML:                  return new RestXmlClientProtocol(id);
        case CBOR: case SMITHY_RPC_V2_CBOR: return new RpcV2CborProtocol(id);
        default: throw SdkClientException.create("Unsupported protocol: " + p + ...);   // awsQuery, ec2
    }
}
```

Problems with that:

- **Wrong `X-Amz-Target`.** The `id` comes from `DynamoDbApiService`, which `ApiServiceSpec` names
  `com.amazonaws.dynamodb#DynamoDb`. smithy's awsJson protocol builds the target header as
  `service.getName() + "." + operation`. Probe: the wire carried `X-Amz-Target: DynamoDb.GetItem`. Real
  DynamoDB expects `DynamoDB_20120810.GetItem`, which is what v2 and the hard-wired path send. The E2E mock
  routes on body content, so it doesn't notice. This path has not been run against the real service.
- v2's `AWS_JSON` covers both awsJson1_0 and awsJson1_1, and the resolver always picks
  `AwsJson1Protocol`. A 1.1 service would get the wrong `Content-Type`.
- Only awsJson has been exercised. The REST and CBOR branches exist but no service with those protocols
  has generated operations.

### Errors

There's no shim. Each operation's `TypeRegistry` registers the v2 exception classes directly:

```java
// GetItemOperation (generated). Only GetItem's own errors, unlike v2's per-service mapping.
private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ResourceNotFoundException.$SCHEMA.id(), ResourceNotFoundException.class,
                 ResourceNotFoundException::builder)
        ...
        .build();
```

smithy's HTTP error deserializer asks the registry for a builder of type `ModeledException`, and the
registry checks the registered class against it:

```java
// smithy-java TypeRegistry.createBuilder(shapeId, type)
if (!type.isAssignableFrom(expectedType)) {
    throw new SerializationException("Polymorphic shape " + shapeId + " is not compatible with " + type);
}
```

`ResourceNotFoundException extends DynamoDbException`, not `ModeledException`, so every modeled error
fails there. `execute` then wraps the failure in a plain `SdkServiceException`. Probe, for a 400 with
`x-amzn-ErrorType: ResourceNotFoundException`:

```
SdkServiceException status=0 requestId=null
  msg=Polymorphic shape com.amazonaws.dynamodb#ResourceNotFoundException is not compatible with
      class software.amazon.smithy.java.core.error.ModeledException
  cause=software.amazon.smithy.java.client.core.error.TransportException
```

Unmodeled errors come back the same way. The message is `Server HTTP/1.1 500 response from operation
...GetItemOperation@1894593a`. None of these is an `AwsServiceException`: there's no `awsErrorDetails`,
the status code is 0 and the request ID is null. `catch (ResourceNotFoundException e)` never matches. The
E2E error test only asserts `SdkServiceException`, so it passes.

The full branch fixed this with the `V2ModeledError` shim and `V2ErrorEnricher`.

### Runtime wrappers (unused here)

`BridgeStruct`, `BridgeOutputBuilder`, `BridgeOutputOperation`, `GeneratedOutputOperation`,
`BridgeSchemaExtension` and `SdkFieldsFromSchema` are in the module, but the pipeline doesn't reference
them. They're used by the serde benchmarks in `test/sdk-standard-benchmarks`.

---

## 3. Generated model classes

The model codegen is the same Phase-1 design the full branch started from. Each model class keeps
everything v2 generates and adds smithy-java members:

```java
public final class GetItemRequest extends DynamoDbRequest
        implements ToCopyableBuilder<GetItemRequest.Builder, GetItemRequest>, SerializableStruct {

    public static final Schema $SCHEMA =
            SdkSchemaFactory.structure("com.amazonaws.dynamodb#GetItemRequest", SDK_FIELDS);
    private static final Schema SCHEMA_TABLE_NAME = $SCHEMA.member(0);
    private static final Schema SCHEMA_KEY        = $SCHEMA.member(1);
    ...

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (this.tableName != null) {
            serializer.writeString(SCHEMA_TABLE_NAME, this.tableName);
        }
        if (this.key != null && !(this.key instanceof SdkAutoConstructMap)) {
            SdkPojoSerializer.writeMap(serializer, SCHEMA_KEY, this.key);
        }
        ...
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        switch (member.memberIndex()) {
            case 0: return (T) this.tableName;
            case 1: return (T) this.key;
            ...
        }
    }

    static final class BuilderImpl extends DynamoDbRequest.BuilderImpl implements Builder, ShapeBuilder<GetItemRequest> {
        @Override
        public ShapeBuilder<GetItemRequest> deserialize(ShapeDeserializer decoder) {
            decoder.readStruct($SCHEMA, this, SmithyMemberConsumer.INSTANCE);
            return this;
        }

        private static final class SmithyMemberConsumer implements ShapeDeserializer.StructMemberConsumer<Builder> {
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {
                switch (member.memberIndex()) {
                    case 0: builder.tableName(de.readString(member)); break;
                    case 1: builder.key((Map) SdkPojoDeserializer.readStringMap(member, SDK_FIELDS.get(1), de)); break;
                    ...
                }
            }
        }
    }
}
```

Exceptions get the same treatment: `ResourceNotFoundException implements SerializableStruct`, with a
`ShapeBuilder` builder. That's what lets codegen register them in the `TypeRegistry`, which is where §2's
error failure comes from.

Structure shape IDs use the v2 class name (`GetItemRequest`) rather than the Smithy model's
(`GetItemInput`). That doesn't matter for awsJson, which doesn't put structure names on the wire.

### Idempotency tokens

They aren't generated. `serializeMembers` writes the member only if the caller set it:

```java
// TransactWriteItemsRequest.serializeMembers (generated)
if (this.clientRequestToken != null) {
    serializer.writeString(SCHEMA_CLIENT_REQUEST_TOKEN, this.clientRequestToken);
}
```

smithy's `InjectIdempotencyTokenPlugin` is active, but it looks for `@idempotencyToken` on the member
schema, and `SdkSchemaFactory` doesn't emit that trait. Probe: `TransactWriteItems` without a token went
out with no `ClientRequestToken` in the body. v2 fills one in.

### Generated operations and service

Per service, an `operations` package (a sibling of `model`) holds one `<Service>ApiService` and one
`<Op>Operation` per operation (58 files for DynamoDB). Gaps relative to what smithy-java's runtime
expects:

- `DynamoDbApiService.$SCHEMA` is `Schema.createService(ShapeId.from("com.amazonaws.dynamodb#DynamoDb"))`
  with no traits. There's no protocol trait (hence §2's fallback), and the name isn't the model's
  `DynamoDB_20120810`.
- Operation schemas are `Schema.createOperation(id)` with no traits: no `@readonly`, `@idempotent`
  or `@http`. Nothing is retry-safe as a result (§1).
- `effectiveAuthSchemes()` is hardcoded to `aws.auth#sigv4` for every operation.

There's no equivalent of the full branch's `V2OperationMetadata`. `httpChecksum` and `requestCompression`
don't apply to DynamoDB in any case.

### Where the schema's traits come from

As on the full branch, `SdkSchemaFactory` rebuilds smithy binding traits from C2J `SdkField` metadata.
This branch has the earlier, smaller mapping:

```java
switch (location) {
    case PATH: case GREEDY_PATH: traits.add(new HttpLabelTrait()); break;
    case HEADER:                 traits.add(new HttpHeaderTrait(wireName)); break;
    case QUERY_PARAM:            traits.add(new HttpQueryTrait(wireName)); break;
    case PAYLOAD:                /* ordinary body member */ break;
    ...
}
if (field.getTrait(PayloadTrait.class) != null) traits.add(new HttpPayloadTrait());
// + JsonNameTrait/XmlNameTrait when the wire name differs, TimestampFormatTrait
```

It has no `HttpPrefixHeadersTrait` or `XmlFlattenedTrait`, which S3 needed on the full branch. That's moot
here, since only awsJson is exercised.

DynamoDB itself is generated from the canonical Smithy model (`codegen-resources/dynamodb/smithy-model.json`)
through the C2J → Smithy front-end. Endpoint rules, paginators and waiters still come from the C2J-era
sidecar files, and the build needs `-Dawssdk.codegen.skipValidation=true`.

---

## 4. HTTP client translation

`V2TransportBridge` implements smithy's `ClientTransport<HttpRequest, HttpResponse>` over the customer's
v2 `SdkHttpClient`, so any v2 sync HTTP client works unchanged. The probe used `Apache5HttpClient`.
Bodies are streamed in both directions:

```java
public final class V2TransportBridge implements ClientTransport<HttpRequest, HttpResponse> {
    @Override
    public HttpResponse send(Context context, HttpRequest request) {
        try {
            HttpExecuteResponse v2Response = v2HttpClient.prepareRequest(toV2Request(request)).call();
            return toSmithyResponse(v2Response);
        } catch (IOException e) {
            throw ClientTransport.remapExceptions(e);          // smithy TransportException
        }
    }

    private static ContentStreamProvider toContentStreamProvider(DataStream body) {
        if (body == null || body.contentLength() == 0) return null;
        return body.isReplayable()
                ? ContentStreamProvider.fromInputStreamSupplier(body::asInputStream)
                : ContentStreamProvider.fromInputStream(body.asInputStream());
    }

    private static HttpResponse toSmithyResponse(HttpExecuteResponse v2Response) {
        DataStream body = v2Response.responseBody()
                .map(in -> DataStream.ofInputStream(in, null, -1))
                .orElse(DataStream.ofBytes(new byte[0]));
        return HttpResponse.of(HttpVersion.HTTP_1_1, v2Response.httpResponse().statusCode(),
                               HttpHeaders.of(v2Response.httpResponse().headers()), body);
    }
}
```

Compared with the full branch's version, this one lacks:

- **Retry of transport failures.** A transport failure becomes a smithy `TransportException`, which
  reports `RetrySafety.NO`, so connection resets and refusals aren't retried. The full branch's
  deferred-failure workaround isn't here.
- **Attempt timeouts.** There's no abort registration.
- **Async.** There's no `SdkAsyncHttpClient` bridge.

`SmithyTransportSdkHttpClient`, the reverse adapter (smithy transport as a v2 `SdkHttpClient`), is in the
module but unused by the pipeline.

---

## 5. Credentials, identity and signing

### Identity

`V2CredentialsBridge` implements smithy's `AwsCredentialsResolver` over the v2 `AwsCredentialsProvider`.
The v2 chain's caching and refresh are kept. Each resolved identity is copied into a smithy identity,
rather than wrapped in a view as on the full branch:

```java
@Override
public IdentityResult<AwsCredentialsIdentity> resolveIdentity(Context properties) {
    try {
        AwsCredentials v2Creds = v2Provider.resolveCredentials();
        return IdentityResult.of(v2Creds instanceof AwsSessionCredentials s
                ? AwsCredentialsIdentity.create(s.accessKeyId(), s.secretAccessKey(), s.sessionToken())
                : AwsCredentialsIdentity.create(v2Creds.accessKeyId(), v2Creds.secretAccessKey()));
    } catch (Exception e) {
        throw SdkClientException.builder().message("Failed to resolve credentials via v2 provider: "
                + e.getMessage()).cause(e).build();
    }
}
```

`accountId` isn't carried over. A request-level `credentialsProvider` is ignored (§1).

### Signing

Signing is smithy-java's own `SigV4AuthScheme`. The signing name comes from `SERVICE_SIGNING_NAME` and the
region from `AWS_REGION`, both via `putConfig`. v2's signer, auth-scheme provider and signer properties
aren't involved.

Probe: every request carried
`Authorization: AWS4-HMAC-SHA256 Credential=CLIENTKEY/20261001/us-west-2/dynamodb/aws4_request,
SignedHeaders=content-type;host;x-amz-date;x-amz-target;...`. A header added by a v2 `modifyHttpRequest`
interceptor appeared in `SignedHeaders`, because that hook runs before signing (§6).

`AuthSchemeResolver` (pick the first registered scheme from `effectiveAuthSchemes()`, treat `noAuth` as
unsigned) is unit-tested but not called by the pipeline; smithy's `Client` does its own scheme
selection. Because every operation declares only SigV4, SigV4a, `noAuth`, bearer and S3 Express aren't
reachable. For DynamoDB, SigV4 is correct.

---

## 6. Interceptor translation

`FullV2InterceptorBridge` adapts one v2 `ExecutionInterceptor` to one smithy `ClientInterceptor`. The
pipeline wraps each v2 interceptor in its own bridge, so ordering comes from smithy's interceptor list
rather than v2's `ExecutionInterceptorChain`, and v2's result validation between hooks doesn't run.

| smithy `ClientInterceptor` | v2 `ExecutionInterceptor` |
|---|---|
| `readBeforeExecution` | `beforeExecution` |
| `modifyBeforeSerialization` | `modifyRequest` |
| `readBeforeSerialization` | `beforeMarshalling` |
| `readAfterSerialization` | `afterMarshalling` |
| `modifyBeforeSigning` | `modifyHttpRequest` |
| `readBeforeTransmit` | `beforeTransmission` |
| `readAfterTransmit` | `afterTransmission` |
| `modifyBeforeDeserialization` | `modifyHttpResponse` |
| `readBeforeDeserialization` | `beforeUnmarshalling` |
| `readAfterDeserialization` | `afterUnmarshalling` |
| `modifyBeforeAttemptCompletion` | `modifyException` (error path only) |
| `modifyBeforeCompletion` | `modifyResponse` |
| `readAfterExecution` | `afterExecution` / `onExecutionFailure` |

That's 14 of v2's 18 hooks. `modifyHttpContent`, `modifyAsyncHttpContent`, `modifyHttpResponseContent` and
`modifyAsyncHttpResponseContent` aren't mapped, so no v2 interceptor can see or replace a request or
response body.

Each hook builds a fresh `InterceptorContext` from the smithy hook, converting the request and response
to v2 types every time:

```java
@Override
public <RequestT> RequestT modifyBeforeSigning(RequestHook<?, ?, RequestT> hook) {
    if (hook.request() instanceof HttpRequest httpRequest) {
        InterceptorContext context = InterceptorContext.builder()
                .request((SdkRequest) hook.input())
                .httpRequest(toSdkHttpRequest(httpRequest))
                .build();
        SdkHttpRequest modified = v2Interceptor.modifyHttpRequest(context, executionAttributes);
        // Rebuilt unconditionally, even if the interceptor returned its input.
        ModifiableHttpRequest modifiable = HttpRequest.create()
                .setMethod(modified.method().name())
                .setUri(modified.getUri());
        modified.forEachHeader((name, values) -> values.forEach(v -> modifiable.headers().addHeader(name, v)));
        modifiable.setBody(httpRequest.body());
        return (RequestT) modifiable;
    }
    return hook.request();
}

@Override
public <O extends SerializableStruct> O modifyBeforeAttemptCompletion(OutputHook<?, O, ?, ?> hook,
                                                                      RuntimeException error) {
    if (error != null && hook.input() instanceof SdkRequest sdkRequest) {
        Context.FailedExecution failed = DefaultFailedExecutionContext.builder()
                .interceptorContext(...).exception(error).build();
        Throwable modified = v2Interceptor.modifyException(failed, executionAttributes);
        ...
    }
    return hook.forward(error);
}
```

### Which interceptors are bridged

Any interceptor whose class name contains `.internal.` is skipped:

```java
private static boolean isInternalInterceptor(ExecutionInterceptor interceptor) {
    return interceptor.getClass().getName().contains(".internal.");
}
```

On a default DynamoDB client, that drops `DynamoDbAuthSchemeInterceptor`,
`DynamoDbResolveEndpointInterceptor`, `DynamoDbRequestSetEndpointInterceptor` and
`HttpChecksumValidationInterceptor`. Three inert ones get bridged and run on every call:
`HelpfulUnknownHostExceptionInterceptor`, `EventStreamInitialRequestInterceptor` and
`TraceIdExecutionInterceptor`. Customer interceptors are added after them.

### Observed behavior (probe)

On a successful `GetItem`, a recording interceptor saw 12 hooks in v2 order: `beforeExecution`,
`modifyRequest`, `beforeMarshalling`, `afterMarshalling`, `modifyHttpRequest`, `beforeTransmission`,
`afterTransmission`, `modifyHttpResponse`, `beforeUnmarshalling`, `afterUnmarshalling`, `modifyResponse`,
`afterExecution`. Where it diverges from v2:

- **`ExecutionAttributes` are per interceptor and per client, not per call.** The bridge creates one
  instance in its constructor and reuses it for every call, on every thread. An attribute set in call 1's
  `beforeExecution` was still there in call 2. Interceptors don't share attributes with each other, so an
  attribute one sets isn't visible to the next.
- **Almost no attributes are populated.** Only `OPERATION_NAME` is set. `SERVICE_NAME`, `CLIENT_TYPE`,
  signing attributes and the request's own `executionAttributes()` are absent.
- **`afterMarshalling` and `modifyHttpRequest` see a placeholder URI**, `https://localhost/`. The request
  has no host at that point, and `toSdkHttpRequest` fills one in so the v2 builder doesn't throw.
- **On failure, `afterUnmarshalling` still fires**, then `onExecutionFailure`. The exception
  `onExecutionFailure` receives is smithy's `SerializationException` or `CallException`, not the v2
  exception the caller will get.
- **`modifyException` never ran.** smithy's `ClientInterceptorChain` calls each interceptor's
  `modifyBeforeAttemptCompletion` in turn, and `hook.forward(error)` throws, so the first interceptor in
  the chain that forwards the error ends the loop. That's the issue the full branch's ledger 3.7
  describes. Here, an earlier interceptor (smithy's own retry-info interceptor, or one of the inert v2
  bridges) is always ahead of the customer's.

---

## 7. Async clients

There's no async support. `SdkPipeline` has no `executeAsync`, `AsyncClientClass` wasn't changed, and the
generated `DefaultDynamoDbAsyncClient` has zero `SdkPipeline` or smithy-java references. It's the stock v2
async client, still using `clientHandler`. Streaming and event-stream sync operations are excluded by the
codegen guard as well, though DynamoDB has none.

Adding async would need an `executeAsync` on the SPI, an async transport bridge, and an async envelope
(smithy-java's `Client.call` is blocking). The full branch's §7 covers that.

---

## 8. What's verified, and metrics

### Tests

`test/smithy-java-pipeline-tests/SmithyJavaPipelineE2ETest` passes 6/6. It drives a real
`DynamoDbClient` with the bridge on the test classpath against a Jetty mock. What each case actually
shows:

| Test | Shows | Doesn't show |
|---|---|---|
| `getItem_roundTrip...`, `putItem_roundTrip...` | serde round trip through `Client.call` | wire correctness (mock ignores `X-Amz-Target`) |
| `errorHandling_serviceError_throwsException` | a 400 surfaces as `SdkServiceException` | the modeled type (it's the generic wrapper, §2) |
| `retryBehavior_transientError_retriesAndSucceeds` | nothing about retries | accepts the 1-attempt failure branch (§1) |
| `interceptorExecution_v2InterceptorFires` | `beforeExecution`/`afterExecution` fire | other hooks, attribute scoping |
| `pipelineDiscovery_...` | the provider is found by `ServiceLoader` | |

The bridge module also has jqwik property tests for each component (`FullV2InterceptorBridge`,
`V2CredentialsBridge`, `ProtocolResolver`, `V2RetryBridge`, `AuthSchemeResolver`). They test the adapters
in isolation, not their wiring in the pipeline.

### Probe summary (2026-10-01)

| Area | Observed on the SPI path |
|---|---|
| Wire | `X-Amz-Target: DynamoDb.GetItem` (real service expects `DynamoDB_20120810.GetItem`); `User-Agent` empty; no `amz-sdk-invocation-id` or `amz-sdk-request` |
| Signing | SigV4 with the client credentials, region and signing name; interceptor-added headers are signed |
| Retries | 1 attempt for a 500 and for `ThrottlingException` |
| Errors | modeled → `SdkServiceException("Polymorphic shape ... not compatible ...")`, status 0, no request ID |
| Idempotency | `TransactWriteItems` sent with no `ClientRequestToken` |
| Request overrides | `credentialsProvider` and `putHeader` ignored |
| Interceptors | 12 hooks fire on success; attributes persist across calls; `modifyException` never reached |
| Metrics | each call published only `ServiceId` and `OperationName`; no `ApiCallDuration`, `ApiCallSuccessful` or attempt child |

### Benchmark

`test/smithy-bridge-benchmark` runs two clients on one classpath. One leaves `SDK_PIPELINE` unset, so it
uses `SmithyJavaPipeline`. The other injects a `NoOpPipeline` whose `supportsOperation` returns false.
Because the fallback is the hard-wired path (§1), the "standard" arm is bare smithy serde, not v2.
`smithy-bridge-current-state.md` §7 records 10,959 vs 14,508 ops/s for `GetItem`. That's smithy-java's
pipeline overhead over its own codecs, not a v2 comparison.

Two notes for any real comparison:

- The SPI path still builds all of v2's per-call objects before the SPI check (response and error
  handlers, the error-code switch lambda, `updateSdkClientConfiguration`, `ClientExecutionParams`), so it
  pays for both pipelines' setup.
- Don't take a number from the current harness: it uses an in-process server and serves `GetItem`'s
  response body to `PutItem`.

---

## Compared with `smithy-java-bridge-alexwoo-full`

| | SPI bridge (this branch) | Full bridge |
|---|---|---|
| Selection | runtime `ServiceLoader` provider + `supportsOperation` | codegen flag; client constructs `SmithyBridgeClient` |
| Generated client deps | `sdk-core` SPI + leftover Phase-2 smithy fields | smithy-java + bridge |
| Fallback | Phase-2 hard-wired smithy path (not v2) | stock v2 for event streams only |
| Endpoint | static client endpoint | v2 endpoint rules + host prefix per attempt |
| Signing | smithy `SigV4AuthScheme` | v2 `AwsV4HttpSigner` via `V2SigningAuthScheme` |
| Retries | smithy standard, max attempts only; never fires (no retry-safe traits) | `SdkRetryStrategy` over v2 `RetryStrategy`, deferred transport failures |
| Errors | generic `SdkServiceException` | concrete v2 exceptions via `V2ModeledError`, `V2ErrorEnricher` |
| Interceptors | one bridge per interceptor, 14 hooks, attributes shared per client | one bridge over v2's `ExecutionInterceptorChain`, 13 hooks incl. body hooks, per-call attributes |
| Request overrides | ignored | applied per field (`V2RequestOverrides`) |
| Timeouts | none | `V2Timeouts` |
| Async / streaming | none | async envelope + `V2AsyncTransportBridge`; S3 streaming |
| Services | DynamoDB sync (awsJson) | DynamoDB and S3, sync and async |

The SPI seam is the main idea this branch adds, and it doesn't depend on the bridge's internals. The
full branch's bridge components could sit behind `SmithyJavaPipelineProvider` without changing the
contract, except for request-level config and async, which the SPI doesn't model yet.

---

## Reproducing

JDK 21 and the locally published smithy-java `1.4.2-rebased` in `~/.m2` are required.

```bash
mvn install -pl :codegen,:codegen-maven-plugin -P quick -am
mvn install -pl :smithy-java-bridge -P quick -am -Dawssdk.codegen.skipValidation=true
mvn clean install -pl :dynamodb,:apache5-client -P quick -Dawssdk.codegen.skipValidation=true
mvn test -pl :smithy-java-pipeline-tests
```

Run `clean` on DynamoDB after switching branches. `target/generated-sources` isn't regenerated otherwise,
and the files left there by the full branch look plausible but are wrong for this branch.
