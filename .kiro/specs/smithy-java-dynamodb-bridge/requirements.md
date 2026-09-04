# Requirements: DynamoDB smithy-java Bridge Runtime Integration

## Goal

Make DynamoDB's generated code use the smithy-java bridge components at runtime for
serialization and deserialization. This proves Phase 1 of the RFC end-to-end: canonical
Smithy model in, smithy-java schema-driven serde underneath, v2 public API preserved.

## Context

- The codegen already has a `generateSmithyJavaSerde` flag in `customization.config` that
  makes `AwsServiceModel` emit `$SCHEMA`, `implements SerializableStruct`,
  `serializeMembers`, and `SmithyMemberConsumer` (deserialization) on POJOs.
- The `core/smithy-java-bridge` module provides `SdkSchemaFactory`, `SdkPojoSerializer`,
  `SdkPojoDeserializer`, `BridgeOutputOperation`, and `GeneratedOutputOperation`.
- The smithy-java-demo proves the full loop works for a native smithy-java client.
- What's missing:
  1. **ApiOperation classes** — smithy-java's protocol layer needs `ApiOperation` instances
     to drive serde (operation schema, input/output schemas, error registry). The v2 codegen
     does not generate these; the benchmarks work around this with `DynamicClient` (which
     loads the full Smithy model at runtime). We need to generate static `ApiOperation`
     singletons per operation.
  2. Wiring the generated code so that `DefaultDynamoDbClient` uses the smithy-java
     protocol + bridge serde instead of the v2 `AwsJsonProtocol` marshallers.

## Requirements

### REQ-1: Generated POJOs implement SerializableStruct

When `generateSmithyJavaSerde = true` in DynamoDB's `customization.config`:
- All request, response, and model POJOs implement `SerializableStruct`
- Each has a `public static final Schema $SCHEMA` field built via `SdkSchemaFactory`
- `serializeMembers(ShapeSerializer)` delegates to a static `SdkPojoSerializer.Plan`
- `schema()` returns `$SCHEMA`
- Builder inner classes implement the smithy-java deserialize consumer

**Acceptance:** DynamoDB compiles with the flag enabled and all existing tests still pass
(the v2 runtime path is unaffected; the new interfaces are additive).

### REQ-2: Generated ApiOperation classes per operation

When `generateSmithyJavaSerde = true`, the codegen emits one `ApiOperation` implementation
per service operation (e.g. `PutItemOperation`, `GetItemOperation`, ...) plus a single
`ApiService` singleton. Each operation class provides:
- `inputSchema()` / `outputSchema()` referencing the generated POJO `$SCHEMA` fields
- `inputBuilder()` / `outputBuilder()` returning the POJO builders (which implement `ShapeBuilder`)
- `errorRegistry()` mapping error `$SCHEMA` to exception builder factories
- `effectiveAuthSchemes()` based on the service's auth configuration
- `service()` returning the `ApiService` singleton

These are static singletons — no Smithy model or DynamicClient needed at runtime.

**Acceptance:** `PutItemOperation.instance().inputSchema()` returns the same `Schema`
as `PutItemRequest.$SCHEMA`. The `errorRegistry()` correctly maps DynamoDB exception types.

### REQ-3: Bridge protocol serde path works for awsJson1_0

DynamoDB uses `awsJson1_0`. The bridge must be able to:
- Serialize a `PutItemRequest` (v2 POJO implementing `SerializableStruct`) through
  smithy-java's `AwsJson1Protocol.createRequest()` and produce the correct HTTP body
- Deserialize a `GetItemResponse` JSON body back through smithy-java's protocol layer
  into the v2 POJO builder (via `SdkPojoDeserializer` / `BridgeOutputBuilder`)
- Deserialize error responses into the correct modeled exception type

**Acceptance:** A test serializes a `PutItemRequest` through the smithy-java awsJson1_0
protocol and asserts the output JSON matches what the v2 marshaller produces. A test
deserializes a known JSON body into `GetItemResponse` and asserts member values.

### REQ-4: Transport bridge wires v2 HTTP client to smithy-java client pipeline

The existing `V2TransportBridge` / `SmithyTransportSdkHttpClient` must be usable to
execute DynamoDB requests end-to-end through the smithy-java pipeline with SigV4 signing.

**Acceptance:** An integration test (using a mock HTTP backend or DynamoDB Local) makes a
`ListTables` or `PutItem` call through the full smithy-java pipeline (protocol, signing,
transport) and gets a valid response.

### REQ-5: DynamoDB module builds and tests pass

The DynamoDB module with `generateSmithyJavaSerde = true`:
- Compiles cleanly (`mvn install -pl :dynamodb -P quick`)
- Passes all existing unit tests (the v2 runtime path is still the default execution path
  for the generated client; the bridge path is exercised by the new tests)

**Acceptance:** `mvn install -pl :dynamodb -Dawssdk.codegen.skipValidation=true` succeeds.

## Testing Strategy

Focus on **protocol-level serde correctness** since that's the highest risk area. We do
not need full integration test suites — this is a prototype.

### Test 1: Marshal round-trip (request serialization)

Construct v2 DynamoDB request POJOs (e.g. `PutItemRequest` with various `AttributeValue`
types), serialize them via:
1. The existing v2 marshaller (baseline)
2. The smithy-java bridge path: use the generated `PutItemOperation.instance()` with
   `AwsJson1Protocol` — the POJO is directly usable as `SerializableStruct`

Assert the JSON bodies are identical.

This is analogous to the existing protocol tests (`jsonrpc-suite.json` marshall cases) but
exercised through the bridge using generated `ApiOperation` classes.

### Test 2: Unmarshal round-trip (response deserialization)

Take known JSON response bodies for DynamoDB operations, deserialize through:
1. The existing v2 unmarshaller (baseline)
2. The smithy-java bridge path: use generated `ApiOperation.outputBuilder()` (which
   returns a `ShapeBuilder`) through the protocol

Assert the resulting POJO field values are identical.

### Test 3: Error deserialization

Take a known DynamoDB error JSON response (e.g. `ResourceNotFoundException`), deserialize
through the bridge using the operation's `errorRegistry()`, and assert:
- The correct exception type is thrown
- Error fields (message, error code) are populated

### Test 4: End-to-end smoke test (optional, if DynamoDB Local available)

Use `V2TransportBridge` + a v2 `UrlConnectionHttpClient` to make a real `ListTables` call
against DynamoDB Local, proving the full pipeline (schema serde + SigV4 + HTTP) works.
Uses generated `ApiOperation` to drive the call.

## Out of Scope

- Changing the default client execution path (v2 runtime remains default)
- Presigning, streaming, multipart, or DynamoDB enhanced client
- Other protocols (rest-json, rest-xml, query) — only awsJson1_0
- Production-ready error handling / edge cases
- Performance optimization beyond what the bridge already does
- DynamoDB Streams (shared model issues are a separate concern)
