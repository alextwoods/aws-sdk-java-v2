# Excluded module tests (Requirement 33.8)

Existing tests of the Prototype_Service_Set modules that are excluded or rewritten because their service runs on the
schema path. An excluded test is one that asserts a legacy behavior the schema path does not keep (see "Phase 5
behavior policy" in `porting-deviations.md`) or tests a legacy component that is no longer generated. Tasks 9.3–9.5 add
their rows. A prototype service swapped for its design D.2 fallback is also recorded here.

## Excluded tests

| Module | Test | Reason | Policy row or defect | Task |
|---|---|---|---|---|
| — | None | | | 9.2 |

## Rewritten tests

| Module | Test | Change | Task |
|---|---|---|---|
| `dynamodb` | `PutItemRequestMarshallerTest.onlyRemainingByteBufferDataIsMarshalled` | Called the generated `PutItemRequestMarshaller`, which the schema path no longer generates. Now sends `PutItem` through `DynamoDbClient` (mock HTTP client) and checks the body at `beforeTransmission`: the same wire assertion (only the remaining bytes of a partly read `ByteBuffer` are sent, as base64) | 9.2 |
| `architecture-tests` | `MarshallerDependencyScannerTest.scan_legacySqsClientAndMarshallers_areReported` → `scan_legacyDynamoDbStreamsClientAndMarshallers_areReported` | The real legacy fixture was SQS's `DefaultSqsClient` and `SendMessageRequestMarshaller`, which no longer exist. Now DynamoDB Streams' `DefaultDynamoDbStreamsClient` and `GetRecordsRequestMarshaller`, with the same expected violations | 9.2 |

## Fallback swaps (design D.2)

None. SQS and DynamoDB are generated themselves (task 9.2).
