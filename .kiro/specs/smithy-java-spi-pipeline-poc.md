# smithy-java SPI Pipeline — Proof of Concept

## Summary

We implemented an SPI-based abstraction that allows the entire SDK request/response
execution pipeline to be transparently replaced by a smithy-java-backed implementation.
When the `smithy-java-bridge` jar is on the classpath, service clients automatically use
the smithy-java protocol layer for serialization, transport, and deserialization — no code
changes, no configuration, no recompilation required by the customer.

This decouples the generated client from its execution engine: the default v2 pipeline
(JDK 8 compatible, battle-tested) remains the fallback, while the smithy-java pipeline
(JDK 21, schema-driven, faster serde) activates when available.

---

## Design

### SPI Contracts (in `core/sdk-core`)

Three new classes in `software.amazon.awssdk.core.client.handler`:

| Class | Role |
|-------|------|
| `SdkPipeline` | The execution interface. Methods: `execute(params, config)` and `supportsOperation(params)`. Extends `SdkAutoCloseable`. |
| `SdkPipelineProvider` | ServiceLoader-discovered factory. Methods: `priority()`, `isAvailable()`, `createPipeline(config)`. |
| `SdkPipelineLoader` | Discovery logic. Finds all providers via ServiceLoader, sorts by priority (lowest wins), returns the first available pipeline. |

These live in `sdk-core` and have **zero** smithy-java dependencies — any implementation
can plug in.

### smithy-java Implementation (in `core/smithy-java-bridge`)

| Class | Role |
|-------|------|
| `SmithyJavaPipelineProvider` | Priority 0 (always wins). Registered in `META-INF/services`. Creates `SmithyJavaPipeline`. |
| `SmithyJavaPipeline` | Resolves `ApiOperation` by convention, lazily initializes `AwsJson1Protocol`, uses `V2TransportBridge` for HTTP. |

### Generated Client Integration (in `codegen`)

`SyncClientClass.java` was modified to:

1. **Add field**: `private final SdkPipeline sdkPipeline;`
2. **Constructor**: `this.sdkPipeline = SdkPipelineLoader.instance().loadPipeline(config).orElse(null);`
3. **Per-operation**: For non-streaming ops, build a `ClientExecutionParams`, check
   `sdkPipeline != null && sdkPipeline.supportsOperation(params)` — if true, delegate.
   Otherwise fall through to the existing `clientHandler.execute(...)` path.
4. **close()**: Close the pipeline before closing the handler.

### Flow Diagram

```
DynamoDbClient.putItem(request)
    |
    +-- [sdkPipeline != null?]
    |       |
    |       +-- [supportsOperation?]  <-- checks: input instanceof SerializableStruct
    |       |       |                       AND ApiOperation class exists
    |       |       |
    |       |       +-- YES: sdkPipeline.execute(params, config)
    |       |       |           |
    |       |       |           +-- protocol.createRequest(op, input, ctx, endpoint)
    |       |       |           +-- transport.send(ctx, httpRequest)
    |       |       |           +-- protocol.deserializeResponse(op, ctx, registry, req, resp)
    |       |       |
    |       |       +-- NO: fall through
    |       |
    |       +-- null: fall through
    |
    +-- clientHandler.execute(executionParams)  <-- existing v2 pipeline
```

### Operation Resolution Convention

`SmithyJavaPipeline` resolves `ApiOperation` instances by class name convention:

```
Input class:     software.amazon.awssdk.services.dynamodb.model.PutItemRequest
Operation class: software.amazon.awssdk.services.dynamodb.operations.PutItemOperation
```

This is resolved via `Class.forName` + `instance()` reflection, cached in a
`ConcurrentHashMap`. If the class does not exist (service has not opted in to
`generateSmithyJavaSerde`), the pipeline returns `supportsOperation() == false` and the
v2 path handles the call.

---

## Files Modified/Created

```
core/sdk-core/src/main/java/software/amazon/awssdk/core/client/handler/
  SdkPipeline.java                 (NEW - SPI interface)
  SdkPipelineProvider.java         (NEW - ServiceLoader factory interface)
  SdkPipelineLoader.java           (NEW - discovery logic)

core/smithy-java-bridge/src/main/java/software/amazon/awssdk/bridge/smithyjava/
  SmithyJavaPipelineProvider.java  (NEW - SPI implementation)
  SmithyJavaPipeline.java          (NEW - execution engine)

core/smithy-java-bridge/src/main/resources/META-INF/services/
  software.amazon.awssdk.core.client.handler.SdkPipelineProvider  (NEW)

core/smithy-java-bridge/pom.xml     (MODIFIED - added aws-client-awsjson dependency)

codegen/src/main/java/software/amazon/awssdk/codegen/poet/client/
  SyncClientClass.java             (MODIFIED - SPI field, constructor init, per-op check)
```

---

## Limitations (Prototype Scope)

| Limitation | Impact | Path to Resolution |
|-----------|--------|-------------------|
| awsJson1_0 only | Only DynamoDB-style services work. REST JSON, REST XML, Query, CBOR not wired. | Add protocol resolution in `ensureProtocolInitialized()` based on `ProtocolMetadata` or service traits. |
| No retry | Single attempt per call. Transient failures are not retried. | Wire smithy-java RetryStrategy or bridge v2 retry config. |
| No SigV4 signing | Requests are unsigned. Only works against mock/local endpoints. | Bridge v2 credential provider to smithy-java IdentityResolver, or use smithy-java aws-sigv4 module. |
| No ExecutionInterceptor invocation | Customer-registered v2 interceptors are bypassed. | Use `V2InterceptorBridge` (already exists) to wrap v2 interceptors as smithy-java ClientInterceptors. |
| No metrics | MetricPublisher not invoked on the smithy-java path. | Wire apiCallMetricCollector through or bridge to smithy-java client-metrics-otel. |
| Sync only | AsyncClientHandler / CompletableFuture path not handled. | Add SdkPipeline.executeAsync() and wire into AsyncClientClass codegen. |
| Streaming ops excluded | Streaming input/output and event-stream ops always use v2. | Extend smithy-java transport bridge for streaming bodies. |
| Reflection-based operation resolution | One Class.forName per unique operation (cached after first call). | Could be replaced with a generated static registry or ServiceLoader per service. |

---

## Relationship to Existing Work

This SPI approach coexists with the existing `generateSmithyJavaSerde` codegen path
(which hard-wires smithy-java calls directly into the generated client). The two approaches
serve different purposes:

- `generateSmithyJavaSerde` (current): Hard-codes the smithy-java path into the
  generated client at compile time. Requires the service to opt in via
  customization.config. The generated client has smithy-java as a compile dependency.

- SPI pipeline (this POC): Discovered at runtime. The generated client has no
  smithy-java dependency - only sdk-core. The bridge jar is an optional runtime
  dependency. Any service that has ApiOperation classes generated benefits automatically.

For the prototype/DynamoDB case, both paths exist simultaneously. The SPI path fires first
(priority 0) for operations it supports. In production, the `generateSmithyJavaSerde`
hard-coded path would likely be removed in favor of the SPI approach, which is cleaner and
does not pollute the generated client with smithy-java imports.

---

## How to Use

1. Build sdk-core and smithy-java-bridge:
   ```bash
   mvn install -pl :sdk-core,:smithy-java-bridge -P quick -Dmaven.test.skip=true
   ```

2. Add smithy-java-bridge as a runtime dependency to any service module:
   ```xml
   <dependency>
       <groupId>software.amazon.awssdk</groupId>
       <artifactId>smithy-java-bridge</artifactId>
       <version>${awsjavasdk.version}</version>
       <scope>runtime</scope>
   </dependency>
   ```

3. Ensure the service has `generateSmithyJavaSerde = true` (so ApiOperation classes
   and SerializableStruct POJOs are generated).

4. The pipeline activates automatically - no code changes in the application.
