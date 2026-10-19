# Excluded module tests (Requirement 33.8)

Existing tests of the Prototype_Service_Set modules that are excluded or rewritten because their service runs on the
schema path. An excluded test is one that asserts a legacy behavior the schema path does not keep (see "Phase 5
behavior policy" in `porting-deviations.md`) or tests a legacy component that is no longer generated. Tasks 9.3–9.5 add
their rows. A prototype service swapped for its design D.2 fallback is also recorded here.

## Excluded tests

| Module | Test | Reason | Policy row or defect | Task |
|---|---|---|---|---|
| — | None | | | 9.2 |
| `secretsmanager`, `account` | None. Neither module has tests in the Baseline_Build (no `src/test`); task 9.3 adds `SecretsManagerNewPathTest` (6) and `AccountNewPathTest` (8), all passing | | | 9.3 |
| `sns` | None. The Baseline_Build test `SnsSignatureCheckerTest` (1) passes unchanged; task 9.4 adds `SnsNewPathTest` (12). `SnsNewPathTest` asserts the schema path's form encoding (`%20`), `Content-Type` without charset and `/custom/` endpoint path, so no existing test needed excluding for them | | Policy rows 1–3 (accepted) | 9.4 |
| `route53` | None. `Route53InterceptorTest` (2) passes unchanged; `QueryParamBindingTest` (1) is rewritten (below); task 9.4 adds `Route53NewPathTest` (16). `Route53IntegrationTest` (`src/it`, compiled as a test source) compiles unchanged | | | 9.4 |

## Rewritten tests

| Module | Test | Change | Task |
|---|---|---|---|
| `dynamodb` | `PutItemRequestMarshallerTest.onlyRemainingByteBufferDataIsMarshalled` | Called the generated `PutItemRequestMarshaller`, which the schema path no longer generates. Now sends `PutItem` through `DynamoDbClient` (mock HTTP client) and checks the body at `beforeTransmission`: the same wire assertion (only the remaining bytes of a partly read `ByteBuffer` are sent, as base64) | 9.2 |
| `route53` | `QueryParamBindingTest.testReservedCharInParamValue` | Called the generated `ListHealthChecksRequestMarshaller` and `GetHealthCheckLastFailureReasonRequestMarshaller` with an `AwsXmlProtocolFactory`, none of which the schema path uses. Now sends `ListHealthChecks` and `GetHealthCheckLastFailureReason` through `Route53Client` (mock HTTP client, endpoint `http://localhost`) and checks the request at `beforeTransmission`: the same assertions (encoded path, the two raw query parameters `;foo` and `&bar`, the label `?charlie` sent as `%3Fcharlie`, no query parameters on the second request) | 9.4 |
| `architecture-tests` | `MarshallerDependencyScannerTest.scan_legacySqsClientAndMarshallers_areReported` → `scan_legacyDynamoDbStreamsClientAndMarshallers_areReported` | The real legacy fixture was SQS's `DefaultSqsClient` and `SendMessageRequestMarshaller`, which no longer exist. Now DynamoDB Streams' `DefaultDynamoDbStreamsClient` and `GetRecordsRequestMarshaller`, with the same expected violations | 9.2 |

## Fallback swaps (design D.2)

None. SQS and DynamoDB (task 9.2), Secrets Manager and Account (task 9.3), SNS and Route 53 (task 9.4) are generated
themselves.
