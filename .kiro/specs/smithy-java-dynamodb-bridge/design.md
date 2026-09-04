# Design: DynamoDB smithy-java Bridge Runtime Integration

## Overview

Enable the `generateSmithyJavaSerde` flag for DynamoDB so that generated POJOs implement
`SerializableStruct` / `ShapeBuilder`, generate `ApiOperation` + `ApiService` classes,
**and rewire `DefaultDynamoDbClient` to use the smithy-java protocol pipeline** for all
non-streaming operations. The generated client now serializes requests and deserializes
responses entirely through smithy-java — no v2 marshallers or unmarshallers involved.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ Generated DynamoDB POJOs (PutItemRequest, GetItemResponse…) │
│ • extends existing v2 base classes (unchanged)              │
│ • implements SerializableStruct                             │
│ • static $SCHEMA field (built via SdkSchemaFactory)         │
│ • serializeMembers(serializer) — direct field reads         │
│ • Builder implements ShapeBuilder — switch-based deserialize│
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ DefaultDynamoDbClient (generated)                           │
│                                                             │
│ Fields:                                                     │
│   • smithyProtocol  = AwsJson1Protocol(serviceShapeId)      │
│   • smithyTransport = V2TransportBridge(configuredHttpClient)│
│   • smithyEndpoint  = SmithyUri.of(configured endpoint)     │
│                                                             │
│ Operation method body (e.g. putItem):                       │
│   1. smithyProtocol.createRequest(op, input, ctx, endpoint) │
│   2. smithyTransport.send(ctx, httpRequest)                 │
│   3. smithyProtocol.deserializeResponse(op, ..., response)  │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌──────────────────────────┐   ┌──────────────────────────────┐
│ Generated ApiOperation   │   │ V2TransportBridge             │
│ (PutItemOperation, etc.) │   │ • wraps configured SdkHttpClient│
│ • inputSchema/outputSchema│   │ • converts smithy-java HttpReq │
│ • errorRegistry (TypeReg)│   │   to v2 SdkHttpFullRequest     │
│ • effectiveAuthSchemes   │   │ • converts v2 response back    │
│ • service() → ApiService │   └──────────────────────────────┘
└──────────────────────────┘
```

---

## Component Changes

### 1. DynamoDB `customization.config`

`"generateSmithyJavaSerde": true` — triggers all smithy-java serde generation.

### 2. DynamoDB `pom.xml` — dependencies

```xml
<!-- Bridge module (compile) — provides SdkSchemaFactory, SdkPojoSerializer, V2TransportBridge -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>smithy-java-bridge</artifactId>
    <version>${awsjavasdk.version}</version>
</dependency>
<!-- smithy-java awsJson protocol (compile) — used by the generated client -->
<dependency>
    <groupId>software.amazon.smithy.java</groupId>
    <artifactId>aws-client-awsjson</artifactId>
    <version>${smithy.java.version}</version>
</dependency>
```

### 3. Codegen: `ApiOperationSpec` — one per operation

Emits `PutItemOperation.java`, `GetItemOperation.java`, etc. into a `.operations` package.
Each is a static singleton implementing `ApiOperation<InputType, OutputType>` with
inputSchema, outputSchema, builders, errorRegistry, and auth schemes.

### 4. Codegen: `ApiServiceSpec` — singleton per service

`DynamoDbApiService implements ApiService` with `Schema.createService(serviceShapeId)`.

### 5. Codegen: Client constructor — smithy-java fields

`SyncClientClass` adds three fields when `generateSmithyJavaSerde` is true:
```java
private final AwsJson1Protocol smithyProtocol;
private final ClientTransport<HttpRequest, HttpResponse> smithyTransport;
private final SmithyUri smithyEndpoint;
```

Initialized in constructor from the resolved `SdkHttpClient` and endpoint configuration.

### 6. Codegen: Operation method bodies — smithy-java execution path

`JsonProtocolSpec.executionHandler()` emits the smithy-java path for non-streaming ops:
```java
Context smithyContext = Context.create();
ApiOperation<SerializableStruct, SerializableStruct> op = (ApiOperation) PutItemOperation.instance();
HttpRequest httpRequest = smithyProtocol.createRequest(op, (SerializableStruct) input, smithyContext, smithyEndpoint);
HttpResponse httpResponse = smithyTransport.send(smithyContext, httpRequest);
try {
    return (PutItemResponse) smithyProtocol.deserializeResponse(op, smithyContext, op.errorRegistry(), httpRequest, httpResponse);
} catch (CallException e) {
    if (e.getCause() instanceof SdkServiceException) throw (SdkServiceException) e.getCause();
    throw DynamoDbException.builder().message(e.getMessage()).cause(e).build();
}
```

Streaming operations still use the v2 `clientHandler.execute(ClientExecutionParams...)` path.

### 7. Codegen: Exception shapes get `$SCHEMA`

`AwsServiceModel.smithyJavaSerde()` returns `true` for `ShapeType.Exception`, enabling
error deserialization via `TypeRegistry`.

### 8. SmithyToServiceModel: Endpoint discovery trait handling

Processes `aws.api#clientEndpointDiscovery` and `aws.api#clientDiscoveredEndpoint` from the
Smithy model to ensure `endpointDiscoveryEnabled(boolean)` is generated on the client builder.

---

## Known Issues & Limitations

| Issue | Description |
|-------|-------------|
| Renamed members (`NULL` → `NUL`) | `SdkSchemaFactory` uses Java property name instead of wire name |
| v2 interceptors not invoked | smithy-java path bypasses `clientHandler.execute()` |
| Retry/timeout not applied | No retry loop in smithy-java path |
| Metrics not published | `MetricPublisher` not wired |
| DynamoDB Streams removed | Shared-model clobber issues; needs separate fix |
| Signing via v2 pipeline | Auth handled by v2 HTTP client, not smithy-java SigV4 |

---

## Non-Goals (Prototype Scope)

- Streaming operations (keep using v2 pipeline)
- Async client (`DefaultDynamoDbAsyncClient`)
- SigV4 signing through smithy-java
- v2 `ExecutionInterceptor` bridge
- Retry through smithy-java
- Performance optimization
- Other services beyond DynamoDB
- DynamoDB Streams shared-model resolution
