# Tasks: Phase 2 — DynamoDB Client Uses smithy-java Pipeline

## Task 1: Add smithy-java protocol and transport fields to generated client

**Requirements:** REQ-1

1. Modify `JsonProtocolSpec` (or `SyncClientClass`) to add fields to the generated
   `DefaultDynamoDbClient` when `generateSmithyJavaSerde` is true:
   - `private final AwsJson1Protocol smithyProtocol`
   - `private final ClientTransport<HttpRequest, HttpResponse> smithyTransport`
   - `private final SmithyUri smithyEndpoint`
2. Initialize in the constructor:
   - `smithyProtocol = new AwsJson1Protocol(ShapeId.from("com.amazonaws.dynamodb#DynamoDB_20120810"))`
   - `smithyTransport = new V2TransportBridge(httpClient)` (using the resolved SdkHttpClient)
   - `smithyEndpoint = SmithyUri.of(clientConfiguration.get(SdkClientOption.ENDPOINT).toString())`
3. Build and verify compilation.

**Done when:** `DefaultDynamoDbClient` has the three new fields initialized.

---

## Task 2: Generate smithy-java operation method bodies

**Requirements:** REQ-2, REQ-4

1. In `JsonProtocolSpec.executionHandler()`, when `generateSmithyJavaSerde` is true,
   emit a new method body that:
   ```java
   return (PutItemResponse) smithyProtocol.deserializeResponse(
       PutItemOperation.instance(),
       software.amazon.smithy.java.context.Context.create(),
       PutItemOperation.instance().errorRegistry(),
       smithyProtocol.createRequest(
           PutItemOperation.instance(),
           (SerializableStruct) putItemRequest,
           software.amazon.smithy.java.context.Context.create(),
           smithyEndpoint),
       smithyTransport.send(
           software.amazon.smithy.java.context.Context.create(),
           smithyProtocol.createRequest(
               PutItemOperation.instance(),
               (SerializableStruct) putItemRequest,
               software.amazon.smithy.java.context.Context.create(),
               smithyEndpoint)));
   ```
   (Or the equivalent two-step: create request, send, deserialize.)
2. Skip streaming operations (keep using the v2 path for those).
3. Build DynamoDB and verify the generated client compiles.

**Done when:** Non-streaming operation methods use the smithy-java protocol directly.

---

## Task 3: Handle error responses

**Requirements:** REQ-3

1. When `AwsJson1Protocol.deserializeResponse()` throws a `CallException` (smithy-java's
   error type), catch it and either:
   - Let it propagate as-is (if we don't need exact v2 exception type compatibility)
   - Or wrap/map it to the v2 exception type
2. For the prototype, the simplest approach: let smithy-java's `TypeRegistry` produce
   the correct exception via the generated `ApiOperation.errorRegistry()`. Since
   exception builders now implement `ShapeBuilder`, the smithy-java protocol should
   deserialize directly into the v2 exception type. Verify this works.

**Done when:** Error responses produce typed exceptions (e.g. `ResourceNotFoundException`).

---

## Task 4: Verify with existing tests and new E2E test

**Requirements:** REQ-5

1. Run `DynamoDbSmithyClientE2ETest` — should still pass (it manually calls the protocol).
2. Modify the test to use `DynamoDbClient.builder()...build()` with a mock HTTP client,
   proving the actual generated client now goes through smithy-java.
3. Run all DynamoDB tests and fix any failures.

**Done when:** The generated `DynamoDbClient` uses smithy-java serde end-to-end and tests pass.

---

## Task dependency

```
Task 1 (fields)
    ↓
Task 2 (method bodies)
    ↓
Task 3 (error handling)
    ↓
Task 4 (verification)
```
