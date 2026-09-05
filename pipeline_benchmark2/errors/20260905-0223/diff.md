# Error behavior diff — `20260905-0223`

Baseline arm: `baseline`. Compared against: `bridge`.
21 cases, 84 rows.

Every arm gave the same answer on every rep, so each case below is a single deterministic outcome.

## `baseline` vs `bridge`

**14 behavioral difference(s).**

| fault | case | field | `baseline` | `bridge` |
|---|---|---|---|---|
| `throughput-exceeded` | persistent | `rawResponse` | present | absent |
| `throttling` | persistent | `rawResponse` | present | absent |
| `internal-error` | persistent | `rawResponse` | present | absent |
| `unavailable` | persistent | `rawResponse` | present | absent |
| `unavailable-retry-after` | persistent | `rawResponse` | present | absent |
| `resource-not-found` | persistent | `rawResponse` | present | absent |
| `resource-not-found` | transient | `rawResponse` | present | absent |
| `conditional-check-failed` | persistent | `exception` | software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException | software.amazon.awssdk.services.dynamodb.model.DynamoDbException |
| `conditional-check-failed` | persistent | `rawResponse` | present | absent |
| `conditional-check-failed` | transient | `exception` | software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException | software.amazon.awssdk.services.dynamodb.model.DynamoDbException |
| `conditional-check-failed` | transient | `rawResponse` | present | absent |
| `malformed-body` | persistent | `cause` | java.io.UncheckedIOException | software.amazon.smithy.java.core.serde.SerializationException |
| `empty-500` | persistent | `rawResponse` | present | absent |
| `truncated-stream` | persistent | `rawResponse` | present | absent |

Wall time differing by 2.0x or more — read as a backoff signal, not a latency:

| fault | case | `baseline` ms | `bridge` ms |
|---|---|---:|---:|
| `internal-error` | persistent | 52 | 225 |
| `resource-not-found` | transient | 4 | 2 |
| `conditional-check-failed` | persistent | 5 | 2 |
| `empty-500` | transient | 99 | 258 |
| `truncated-stream` | transient | 128 | 12 |

Message wording differences (not behavioral on their own):

| fault | case | `baseline` | `bridge` |
|---|---|---|---|
| `throughput-exceeded` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `throttling` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `internal-error` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)` |
| `unavailable` | persistent | `Service_returned_HTTP_status_code_503_(Service:_DynamoDb;_Status_Code:_503;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Service_returned_HTTP_status_code_503_(Service:_DynamoDb;_Status_Code:_503;_Request_ID:_FAULTREQID000000000000000)` |
| `unavailable-retry-after` | persistent | `Service_returned_HTTP_status_code_503_(Service:_DynamoDb;_Status_Code:_503;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Service_returned_HTTP_status_code_503_(Service:_DynamoDb;_Status_Code:_503;_Request_ID:_FAULTREQID000000000000000)` |
| `resource-not-found` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `resource-not-found` | transient | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `conditional-check-failed` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `Service_returned_error_code_ConditionalCheckFailedException_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `conditional-check-failed` | transient | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `Service_returned_error_code_ConditionalCheckFailedException_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `malformed-body` | persistent | `Unable_to_unmarshall_response_(software.amazon.awssdk.thirdparty.jackson.core.io.JsonEOFException:_Unexpected_end-of-input_in_VALUE_STRING_at_[Source:_(software...` | `Unexpected_end-of-input_in_VALUE_STRING_at_[Source:_REDACTED_(`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION`_disabled);_byte_offset:_#25]` |
| `empty-500` | persistent | `Service_returned_HTTP_status_code_500_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Service_returned_HTTP_status_code_500_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)` |
| `truncated-stream` | persistent | `Service_returned_HTTP_status_code_500_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Service_returned_HTTP_status_code_500_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)` |

## What each arm did

| fault | case | arm | attempts | outcome | exception | errorCode | status | requestId | retryable | throttling | wall ms |
|---|---|---|---:|---|---|---|---:|---|---|---|---:|
| `none` | control | `baseline` | 1 | ok | `-` | - | - | - | - | - | 194 |
| `none` | control | `bridge` | 1 | ok | `-` | - | - | - | - | - | 168 |
| `throughput-exceeded` | persistent | `baseline` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 1858 |
| `throughput-exceeded` | persistent | `bridge` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 2771 |
| `throughput-exceeded` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 1161 |
| `throughput-exceeded` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 1372 |
| `throttling` | persistent | `baseline` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 2288 |
| `throttling` | persistent | `bridge` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 1572 |
| `throttling` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 1272 |
| `throttling` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 1457 |
| `internal-error` | persistent | `baseline` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 52 |
| `internal-error` | persistent | `bridge` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 225 |
| `internal-error` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 195 |
| `internal-error` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 141 |
| `unavailable` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 208 |
| `unavailable` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 222 |
| `unavailable` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 209 |
| `unavailable` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 105 |
| `unavailable-retry-after` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 2010 |
| `unavailable-retry-after` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 2008 |
| `unavailable-retry-after` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 2009 |
| `unavailable-retry-after` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 2008 |
| `resource-not-found` | persistent | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 6 |
| `resource-not-found` | persistent | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 4 |
| `resource-not-found` | transient | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 4 |
| `resource-not-found` | transient | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 2 |
| `conditional-check-failed` | persistent | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 5 |
| `conditional-check-failed` | persistent | `bridge` | 1 | throw | `DynamoDbException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 2 |
| `conditional-check-failed` | transient | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `conditional-check-failed` | transient | `bridge` | 1 | throw | `DynamoDbException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `malformed-body` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 209 |
| `malformed-body` | persistent | `bridge` | 3 | throw | `SdkClientException` | - | - | - | false | - | 113 |
| `malformed-body` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 235 |
| `malformed-body` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 229 |
| `empty-500` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 155 |
| `empty-500` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 173 |
| `empty-500` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 99 |
| `empty-500` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 258 |
| `truncated-stream` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 302 |
| `truncated-stream` | persistent | `bridge` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 192 |
| `truncated-stream` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 128 |
| `truncated-stream` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 12 |

Median wall time per arm across all cases: `baseline` 192 ms, `bridge` 195 ms

