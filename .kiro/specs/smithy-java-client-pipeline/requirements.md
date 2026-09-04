# Requirements: Phase 2 — DynamoDB Client Uses smithy-java Pipeline

## Goal

Make `DefaultDynamoDbClient` actually use the smithy-java protocol for serialization and
deserialization when executing operations. When a user calls `dynamoDbClient.putItem(...)`,
the request flows through `AwsJson1Protocol.createRequest()` → `V2TransportBridge` →
`AwsJson1Protocol.deserializeResponse()` — not the v2 marshallers.

## Context

Phase 1 made POJOs capable of smithy-java serde (`SerializableStruct`, `ShapeBuilder`,
`$SCHEMA`) and generated `ApiOperation` singletons. But `DefaultDynamoDbClient` still
uses the v2 pipeline (`ClientExecutionParams` → `PutItemRequestMarshaller` →
`protocolFactory.createResponseHandler`).

The generated client method body currently looks like:
```java
return clientHandler.execute(new ClientExecutionParams<PutItemRequest, PutItemResponse>()
    .withMarshaller(new PutItemRequestMarshaller(protocolFactory))
    .withResponseHandler(responseHandler)
    .withErrorResponseHandler(errorResponseHandler)
    ...);
```

We need it to look like:
```java
HttpRequest httpRequest = smithyProtocol.createRequest(
    PutItemOperation.instance(), (SerializableStruct) putItemRequest, context, endpoint);
HttpResponse httpResponse = transport.send(context, httpRequest);
return (PutItemResponse) smithyProtocol.deserializeResponse(
    PutItemOperation.instance(), context,
    PutItemOperation.instance().errorRegistry(), httpRequest, httpResponse);
```

## Requirements

### REQ-1: Client constructor initializes smithy-java protocol and transport

When `generateSmithyJavaSerde = true`, the generated `DefaultDynamoDbClient`:
- Instantiates `AwsJson1Protocol` with the correct service ShapeId
- Wraps the resolved `SdkHttpClient` in a `V2TransportBridge`
- Stores these as fields for use in operation methods

**Acceptance:** Client compiles with the new fields and constructor logic.

### REQ-2: Operation methods use smithy-java for serde

Each non-streaming operation method:
- Calls `AwsJson1Protocol.createRequest(operation, input, context, endpoint)`
- Sends through `V2TransportBridge`
- Calls `AwsJson1Protocol.deserializeResponse(operation, ..., response)`
- Catches smithy-java `CallException` and maps to the appropriate v2 exception type

**Acceptance:** `DynamoDbClient.create()` (with mock HTTP) successfully serializes
requests via smithy-java and deserializes responses back into v2 POJOs.

### REQ-3: Error handling maps smithy-java exceptions to v2 exceptions

When the service returns an error response, smithy-java's protocol layer throws a
`CallException` (or `ModeledException`). The client must catch this and re-throw as
the corresponding v2 modeled exception (e.g. `ResourceNotFoundException`).

**Acceptance:** A 400 response with a DynamoDB error JSON body results in the correct
v2 exception type being thrown to the caller.

### REQ-4: Endpoint resolution still works

The client must resolve the endpoint (from region, endpoint override, etc.) and pass it
to the smithy-java protocol as a `SmithyUri`.

**Acceptance:** Client uses the configured endpoint/region for requests.

### REQ-5: Existing tests pass

All existing DynamoDB unit tests that exercise the generated client (via WireMock or
mock HTTP) continue to pass.

**Acceptance:** `mvn test -pl :dynamodb` passes (excluding the pre-existing timeout).

## Out of Scope

- Streaming operations (keep using v2 pipeline for those)
- SigV4 signing through smithy-java (keep using v2's signing — the request goes through
  the v2 `SdkHttpClient` which handles signing via the v2 auth pipeline)
- Interceptor bridge (v2 ExecutionInterceptors still fire via the v2 pipeline)
- Retry/timeout logic through smithy-java (keep v2's retry strategy)
- Metrics/tracing
- Async client

## Approach

The simplest path for a prototype: modify `JsonProtocolSpec` to emit a different method
body when `generateSmithyJavaSerde` is true. The new body directly calls the smithy-java
protocol layer instead of going through `clientHandler.execute(ClientExecutionParams...)`.

This bypasses the v2 pipeline entirely (no interceptors, no retry, no metrics) — which
is acceptable for a prototype proving the serde path works.
