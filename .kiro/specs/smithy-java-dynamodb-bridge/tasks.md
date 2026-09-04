# Tasks: DynamoDB smithy-java Bridge Runtime Integration

## Task 1: Enable `generateSmithyJavaSerde` flag and add bridge dependency for DynamoDB

**Requirements:** REQ-1, REQ-5

1. Add `"generateSmithyJavaSerde": true` to
   `services/dynamodb/src/main/resources/codegen-resources/dynamodb/customization.config`
2. Add `smithy-java-bridge` as a compile dependency to `services/dynamodb/pom.xml`:
   ```xml
   <dependency>
       <groupId>software.amazon.awssdk</groupId>
       <artifactId>smithy-java-bridge</artifactId>
       <version>${awsjavasdk.version}</version>
   </dependency>
   ```
3. Build DynamoDB: `mvn clean install -pl :dynamodb -P quick --am -Dawssdk.codegen.skipValidation=true`
4. Fix any compilation errors in generated code (missing imports, type mismatches between
   `ShapeBuilder` and v2 builder patterns, union shape handling)
5. Verify all existing tests still pass (the v2 runtime path is unaffected)

**Done when:** DynamoDB compiles with `generateSmithyJavaSerde = true` and all existing
unit tests pass. Generated POJOs have `$SCHEMA`, implement `SerializableStruct`, and
builders implement `ShapeBuilder`.

---

## Task 2: Create `ApiOperationSpec` codegen emitter

**Requirements:** REQ-2

1. Create `codegen/src/main/java/software/amazon/awssdk/codegen/poet/model/ApiOperationSpec.java`
   — a new poet spec class that emits one `ApiOperation<InputType, OutputType>`
   implementation per operation.
2. The emitter should:
   - Accept an `OperationModel` and the `IntermediateModel`
   - Generate a `public final class <OpName>Operation implements ApiOperation<InputReq, OutputResp>`
   - Include a static `$SCHEMA = Schema.createOperation(ShapeId.from(...))`
   - Emit `inputBuilder()` → `<InputType>.builder()`
   - Emit `outputBuilder()` → `<OutputType>.builder()`
   - Emit `inputSchema()` → `<InputType>.$SCHEMA`
   - Emit `outputSchema()` → `<OutputType>.$SCHEMA`
   - Emit `effectiveAuthSchemes()` from service metadata (sigv4)
   - Emit `errorRegistry()` — `TypeRegistry.builder()` with `.putType(Exception.$SCHEMA, Exception::builder)` for each operation error
   - Emit `errorSchemas()` — `List.of(Exception1.$SCHEMA, Exception2.$SCHEMA, ...)`
   - Emit `service()` → `<ServiceName>ApiService.instance()`
   - Use static singleton pattern (`private static final INSTANCE`)
3. Place generated classes in `<service>.operations` package (e.g.
   `software.amazon.awssdk.services.dynamodb.operations`)
4. Derive the Smithy namespace from `IntermediateModel.getMetadata()`:
   `"com.amazonaws." + metadata.getEndpointPrefix() + "#" + operationModel.getOperationName()`

**Done when:** The codegen emitter can produce a syntactically correct `ApiOperation`
class for any operation in the intermediate model. Does not need to be wired into the
build yet.

---

## Task 3: Create `ApiServiceSpec` codegen emitter

**Requirements:** REQ-2

1. Create `codegen/src/main/java/software/amazon/awssdk/codegen/poet/model/ApiServiceSpec.java`
   — emits a single `ApiService` singleton per service.
2. The emitter should:
   - Generate `public final class <ServiceName>ApiService implements ApiService`
   - Include `static final Schema $SCHEMA = Schema.createService(ShapeId.from(...))`
   - Derive the ShapeId from service metadata (service shape name from the Smithy model
     or `"com.amazonaws.<endpointPrefix>#<serviceId>"`)
   - Static singleton with `instance()` method
3. Place in same `<service>.operations` package as the operation classes.

**Done when:** A correct `ApiService` class can be generated for DynamoDB.

---

## Task 4: Wire operation/service codegen into the build pipeline

**Requirements:** REQ-2, REQ-5

1. Modify `codegen/src/main/java/software/amazon/awssdk/codegen/CodeGenerator.java` (or the
   appropriate task orchestrator) to invoke `ApiOperationSpec` and `ApiServiceSpec` when
   `customizationConfig.isGenerateSmithyJavaSerde()` is true.
2. Ensure the generated operation classes are placed in the correct output directory
   (`generated-sources/sdk`) and package.
3. Rebuild DynamoDB: `mvn clean install -pl :dynamodb -P quick --am -Dawssdk.codegen.skipValidation=true`
4. Verify the operation classes compile and reference the correct POJO `$SCHEMA` fields.
5. Spot-check a few operations (PutItem, GetItem, Query) — confirm `inputSchema()` and
   `outputSchema()` resolve to the correct types.

**Done when:** DynamoDB build produces `PutItemOperation.java`, `GetItemOperation.java`,
etc. in the `operations` package, and they compile without errors.

---

## Task 5: Write serialization parity tests

**Requirements:** REQ-3

1. Create `services/dynamodb/src/test/java/software/amazon/awssdk/services/dynamodb/DynamoDbBridgeSerdeTest.java`
2. For each test case, construct a v2 DynamoDB request POJO, serialize through:
   - v2 path: existing `PutItemRequestMarshaller` (or `AwsJsonProtocol`)
   - Bridge path: `AwsJson1Protocol` from smithy-java + generated `PutItemOperation.instance()`
3. Assert JSON bodies are identical.
4. Test cases should cover:
   - `PutItemRequest` with string, number, binary, bool, null, list, and map `AttributeValue` types
   - `GetItemRequest` with simple string key
   - `ListTablesRequest` (empty body — `{}`)
   - `QueryRequest` with KeyConditionExpression and ExpressionAttributeValues
5. Add smithy-java protocol dependencies to test scope in `pom.xml` if needed:
   ```xml
   <dependency>
       <groupId>software.amazon.smithy.java</groupId>
       <artifactId>aws-client-awsjson</artifactId>
       <version>${smithy.java.version}</version>
       <scope>test</scope>
   </dependency>
   ```

**Done when:** Tests pass, proving serialization through the bridge path produces
byte-identical JSON to the v2 marshallers for DynamoDB's awsJson1_0 protocol.

---

## Task 6: Write deserialization parity tests

**Requirements:** REQ-3

1. Create `services/dynamodb/src/test/java/software/amazon/awssdk/services/dynamodb/DynamoDbBridgeDeserializeTest.java`
2. For each test case, take a known JSON response body and deserialize through the bridge
   using the generated operation's `outputBuilder()` (which returns a `ShapeBuilder`).
3. Assert field values match expectations.
4. Test cases:
   - `GetItemResponse` with a map of AttributeValues (string, number, list, nested map)
   - `QueryResponse` with Items list and Count/ScannedCount
   - Error response for `ResourceNotFoundException` — verify exception type, message,
     and error code
5. Error deserialization: Use the operation's `errorRegistry()` to look up the correct
   exception builder, deserialize through it, and assert the typed exception is produced.

**Done when:** Tests pass, proving deserialization through the bridge path correctly
reconstructs DynamoDB response POJOs and exceptions.

---

## Task 7: End-to-end integration smoke test (optional)

**Requirements:** REQ-4

1. Create `services/dynamodb/src/test/java/software/amazon/awssdk/services/dynamodb/DynamoDbBridgeE2ETest.java`
2. Wire up:
   - `V2TransportBridge` wrapping a `UrlConnectionHttpClient` (or mock HTTP backend)
   - `AwsJson1Protocol` from smithy-java
   - SigV4 signing from smithy-java (`aws-sigv4`)
   - Generated `ListTablesOperation.instance()`
3. Make a `ListTables` call through the full smithy-java pipeline.
4. If using a mock HTTP backend: assert the outgoing request has correct headers
   (`X-Amz-Target`, `Content-Type`, Authorization/SigV4) and the canned response
   deserializes correctly.
5. If DynamoDB Local is available: make a real call and assert success.

**Done when:** A full request/response cycle works through the smithy-java pipeline with
SigV4 signing, using generated `ApiOperation` and v2 POJOs as `SerializableStruct`.

---

## Summary of dependencies between tasks

```
Task 1 (enable flag + compile)
    ↓
Task 2 (ApiOperationSpec emitter)  ←─ Task 3 (ApiServiceSpec emitter)
    ↓                                    ↓
Task 4 (wire into build + verify)  ←─────┘
    ↓
Task 5 (serialize tests)  ←─ Task 6 (deserialize tests)
    ↓                              ↓
Task 7 (e2e smoke test, optional)
```

Tasks 2 and 3 can be done in parallel. Tasks 5 and 6 can be done in parallel after Task 4.
