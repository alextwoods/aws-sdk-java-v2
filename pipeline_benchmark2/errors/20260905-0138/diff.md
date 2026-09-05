# Error behavior diff — `20260905-0138`

Baseline arm: `baseline`. Compared against: `bridge`.
19 cases, 76 rows.

Every arm gave the same answer on every rep, so each case below is a single deterministic outcome.

## `baseline` vs `bridge`

**87 behavioral difference(s).**

| fault | case | field | `baseline` | `bridge` |
|---|---|---|---|---|
| `throughput-exceeded` | persistent | `rawResponse` | present | absent |
| `throttling` | persistent | `rawResponse` | present | absent |
| `internal-error` | persistent | `attempts` | 3 | 1 |
| `internal-error` | persistent | `exception` | software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException | software.amazon.awssdk.services.dynamodb.model.DynamoDbException |
| `internal-error` | persistent | `cause` | - | software.amazon.smithy.java.core.error.CallException |
| `internal-error` | persistent | `errorCode` | InternalServerError | - |
| `internal-error` | persistent | `statusCode` | 500 | 0 |
| `internal-error` | persistent | `requestId` | FAULTREQID000000000000000 | - |
| `internal-error` | persistent | `serviceName` | DynamoDb | - |
| `internal-error` | persistent | `rawResponse` | present | absent |
| `internal-error` | transient | `outcome` | ok | throw |
| `internal-error` | transient | `attempts` | 3 | 1 |
| `internal-error` | transient | `exception` | - | software.amazon.awssdk.services.dynamodb.model.DynamoDbException |
| `internal-error` | transient | `cause` | - | software.amazon.smithy.java.core.error.CallException |
| `internal-error` | transient | `statusCode` | - | 0 |
| `internal-error` | transient | `retryable` | - | false |
| `internal-error` | transient | `throttling` | - | false |
| `internal-error` | transient | `clockSkew` | - | false |
| `internal-error` | transient | `rawResponse` | - | absent |
| `unavailable` | persistent | `attempts` | 3 | 1 |
| `unavailable` | persistent | `cause` | - | software.amazon.smithy.java.core.error.CallException |
| `unavailable` | persistent | `statusCode` | 503 | 0 |
| `unavailable` | persistent | `requestId` | FAULTREQID000000000000000 | - |
| `unavailable` | persistent | `serviceName` | DynamoDb | - |
| `unavailable` | persistent | `rawResponse` | present | absent |
| `unavailable` | transient | `outcome` | ok | throw |
| `unavailable` | transient | `attempts` | 3 | 1 |
| `unavailable` | transient | `exception` | - | software.amazon.awssdk.services.dynamodb.model.DynamoDbException |
| `unavailable` | transient | `cause` | - | software.amazon.smithy.java.core.error.CallException |
| `unavailable` | transient | `statusCode` | - | 0 |
| `unavailable` | transient | `retryable` | - | false |
| `unavailable` | transient | `throttling` | - | false |
| `unavailable` | transient | `clockSkew` | - | false |
| `unavailable` | transient | `rawResponse` | - | absent |
| `unavailable-retry-after` | persistent | `attempts` | 3 | 1 |
| `unavailable-retry-after` | persistent | `cause` | - | software.amazon.smithy.java.core.error.CallException |
| `unavailable-retry-after` | persistent | `statusCode` | 503 | 0 |
| `unavailable-retry-after` | persistent | `requestId` | FAULTREQID000000000000000 | - |
| `unavailable-retry-after` | persistent | `serviceName` | DynamoDb | - |
| `unavailable-retry-after` | persistent | `rawResponse` | present | absent |
| `unavailable-retry-after` | transient | `outcome` | ok | throw |
| `unavailable-retry-after` | transient | `attempts` | 3 | 1 |
| `unavailable-retry-after` | transient | `exception` | - | software.amazon.awssdk.services.dynamodb.model.DynamoDbException |
| `unavailable-retry-after` | transient | `cause` | - | software.amazon.smithy.java.core.error.CallException |
| `unavailable-retry-after` | transient | `statusCode` | - | 0 |
| `unavailable-retry-after` | transient | `retryable` | - | false |
| `unavailable-retry-after` | transient | `throttling` | - | false |
| `unavailable-retry-after` | transient | `clockSkew` | - | false |
| `unavailable-retry-after` | transient | `rawResponse` | - | absent |
| `resource-not-found` | persistent | `rawResponse` | present | absent |
| `resource-not-found` | transient | `rawResponse` | present | absent |
| `conditional-check-failed` | persistent | `exception` | software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException | software.amazon.awssdk.services.dynamodb.model.DynamoDbException |
| `conditional-check-failed` | persistent | `cause` | - | software.amazon.smithy.java.core.error.CallException |
| `conditional-check-failed` | persistent | `errorCode` | ConditionalCheckFailedException | - |
| `conditional-check-failed` | persistent | `statusCode` | 400 | 0 |
| `conditional-check-failed` | persistent | `requestId` | FAULTREQID000000000000000 | - |
| `conditional-check-failed` | persistent | `serviceName` | DynamoDb | - |
| `conditional-check-failed` | persistent | `rawResponse` | present | absent |
| `conditional-check-failed` | transient | `exception` | software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException | software.amazon.awssdk.services.dynamodb.model.DynamoDbException |
| `conditional-check-failed` | transient | `cause` | - | software.amazon.smithy.java.core.error.CallException |
| `conditional-check-failed` | transient | `errorCode` | ConditionalCheckFailedException | - |
| `conditional-check-failed` | transient | `statusCode` | 400 | 0 |
| `conditional-check-failed` | transient | `requestId` | FAULTREQID000000000000000 | - |
| `conditional-check-failed` | transient | `serviceName` | DynamoDb | - |
| `conditional-check-failed` | transient | `rawResponse` | present | absent |
| `malformed-body` | persistent | `attempts` | 3 | 1 |
| `malformed-body` | persistent | `cause` | java.io.UncheckedIOException | software.amazon.smithy.java.client.core.error.TransportException |
| `malformed-body` | transient | `outcome` | ok | throw |
| `malformed-body` | transient | `attempts` | 3 | 1 |
| `malformed-body` | transient | `exception` | - | software.amazon.awssdk.core.exception.SdkClientException |
| `malformed-body` | transient | `cause` | - | software.amazon.smithy.java.client.core.error.TransportException |
| `malformed-body` | transient | `retryable` | - | false |
| `empty-500` | persistent | `attempts` | 3 | 1 |
| `empty-500` | persistent | `cause` | - | software.amazon.smithy.java.core.error.CallException |
| `empty-500` | persistent | `statusCode` | 500 | 0 |
| `empty-500` | persistent | `requestId` | FAULTREQID000000000000000 | - |
| `empty-500` | persistent | `serviceName` | DynamoDb | - |
| `empty-500` | persistent | `rawResponse` | present | absent |
| `empty-500` | transient | `outcome` | ok | throw |
| `empty-500` | transient | `attempts` | 3 | 1 |
| `empty-500` | transient | `exception` | - | software.amazon.awssdk.services.dynamodb.model.DynamoDbException |
| `empty-500` | transient | `cause` | - | software.amazon.smithy.java.core.error.CallException |
| `empty-500` | transient | `statusCode` | - | 0 |
| `empty-500` | transient | `retryable` | - | false |
| `empty-500` | transient | `throttling` | - | false |
| `empty-500` | transient | `clockSkew` | - | false |
| `empty-500` | transient | `rawResponse` | - | absent |

Wall time differing by 2.0x or more — read as a backoff signal, not a latency:

| fault | case | `baseline` ms | `bridge` ms |
|---|---|---:|---:|
| `throughput-exceeded` | persistent | 2,504 | 790 |
| `throttling` | persistent | 2,369 | 417 |
| `internal-error` | persistent | 229 | 6 |
| `internal-error` | transient | 82 | 2 |
| `unavailable` | persistent | 54 | 7 |
| `unavailable` | transient | 198 | 2 |
| `unavailable-retry-after` | persistent | 2,010 | 2 |
| `unavailable-retry-after` | transient | 2,010 | 2 |
| `conditional-check-failed` | persistent | 6 | 2 |
| `malformed-body` | persistent | 36 | 3 |
| `malformed-body` | transient | 200 | 3 |
| `empty-500` | persistent | 111 | 4 |
| `empty-500` | transient | 198 | 2 |

Message wording differences (not behavioral on their own):

| fault | case | `baseline` | `bridge` |
|---|---|---|---|
| `throughput-exceeded` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `throttling` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `internal-error` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Server_HTTP/1.1_500_response_from_operation_software.amazon.awssdk.services.dynamodb.operations.GetItemOperation@476a736d.` |
| `internal-error` | transient | `-` | `Server_HTTP/1.1_500_response_from_operation_software.amazon.awssdk.services.dynamodb.operations.GetItemOperation@476a736d.` |
| `unavailable` | persistent | `Service_returned_HTTP_status_code_503_(Service:_DynamoDb;_Status_Code:_503;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Server_HTTP/1.1_503_response_from_operation_software.amazon.awssdk.services.dynamodb.operations.GetItemOperation@476a736d.` |
| `unavailable` | transient | `-` | `Server_HTTP/1.1_503_response_from_operation_software.amazon.awssdk.services.dynamodb.operations.GetItemOperation@476a736d.` |
| `unavailable-retry-after` | persistent | `Service_returned_HTTP_status_code_503_(Service:_DynamoDb;_Status_Code:_503;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Server_HTTP/1.1_503_response_from_operation_software.amazon.awssdk.services.dynamodb.operations.GetItemOperation@476a736d.` |
| `unavailable-retry-after` | transient | `-` | `Server_HTTP/1.1_503_response_from_operation_software.amazon.awssdk.services.dynamodb.operations.GetItemOperation@476a736d.` |
| `resource-not-found` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `resource-not-found` | transient | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |
| `conditional-check-failed` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `Client_HTTP/1.1_400_response_from_operation_software.amazon.awssdk.services.dynamodb.operations.GetItemOperation@476a736d.` |
| `conditional-check-failed` | transient | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_1)` | `Client_HTTP/1.1_400_response_from_operation_software.amazon.awssdk.services.dynamodb.operations.GetItemOperation@476a736d.` |
| `malformed-body` | persistent | `Unable_to_unmarshall_response_(software.amazon.awssdk.thirdparty.jackson.core.io.JsonEOFException:_Unexpected_end-of-input_in_VALUE_STRING_at_[Source:_(software...` | `Unexpected_end-of-input_in_VALUE_STRING_at_[Source:_REDACTED_(`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION`_disabled);_byte_offset:_#25]` |
| `malformed-body` | transient | `-` | `Unexpected_end-of-input_in_VALUE_STRING_at_[Source:_REDACTED_(`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION`_disabled);_byte_offset:_#25]` |
| `empty-500` | persistent | `Service_returned_HTTP_status_code_500_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `Server_HTTP/1.1_500_response_from_operation_software.amazon.awssdk.services.dynamodb.operations.GetItemOperation@476a736d.` |
| `empty-500` | transient | `-` | `Server_HTTP/1.1_500_response_from_operation_software.amazon.awssdk.services.dynamodb.operations.GetItemOperation@476a736d.` |

## What each arm did

| fault | case | arm | attempts | outcome | exception | errorCode | status | requestId | retryable | throttling | wall ms |
|---|---|---|---:|---|---|---|---:|---|---|---|---:|
| `none` | control | `baseline` | 1 | ok | `-` | - | - | - | - | - | 196 |
| `none` | control | `bridge` | 1 | ok | `-` | - | - | - | - | - | 170 |
| `throughput-exceeded` | persistent | `baseline` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 2504 |
| `throughput-exceeded` | persistent | `bridge` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 790 |
| `throughput-exceeded` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 2991 |
| `throughput-exceeded` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 2691 |
| `throttling` | persistent | `baseline` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 2369 |
| `throttling` | persistent | `bridge` | 3 | throw | `ThrottlingException` | ThrottlingException | 400 | FAULTREQID000000000000000 | false | true | 417 |
| `throttling` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 567 |
| `throttling` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 965 |
| `internal-error` | persistent | `baseline` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 229 |
| `internal-error` | persistent | `bridge` | 1 | throw | `DynamoDbException` | - | 0 | - | false | false | 6 |
| `internal-error` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 82 |
| `internal-error` | transient | `bridge` | 1 | throw | `DynamoDbException` | - | 0 | - | false | false | 2 |
| `unavailable` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 54 |
| `unavailable` | persistent | `bridge` | 1 | throw | `DynamoDbException` | - | 0 | - | false | false | 7 |
| `unavailable` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 198 |
| `unavailable` | transient | `bridge` | 1 | throw | `DynamoDbException` | - | 0 | - | false | false | 2 |
| `unavailable-retry-after` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 503 | FAULTREQID000000000000000 | false | false | 2010 |
| `unavailable-retry-after` | persistent | `bridge` | 1 | throw | `DynamoDbException` | - | 0 | - | false | false | 2 |
| `unavailable-retry-after` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 2010 |
| `unavailable-retry-after` | transient | `bridge` | 1 | throw | `DynamoDbException` | - | 0 | - | false | false | 2 |
| `resource-not-found` | persistent | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 5 |
| `resource-not-found` | persistent | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `resource-not-found` | transient | `baseline` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `resource-not-found` | transient | `bridge` | 1 | throw | `ResourceNotFoundException` | ResourceNotFoundException | 400 | FAULTREQID000000000000000 | false | false | 2 |
| `conditional-check-failed` | persistent | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 6 |
| `conditional-check-failed` | persistent | `bridge` | 1 | throw | `DynamoDbException` | - | 0 | - | false | false | 2 |
| `conditional-check-failed` | transient | `baseline` | 1 | throw | `ConditionalCheckFailedException` | ConditionalCheckFailedException | 400 | FAULTREQID000000000000000 | false | false | 3 |
| `conditional-check-failed` | transient | `bridge` | 1 | throw | `DynamoDbException` | - | 0 | - | false | false | 2 |
| `malformed-body` | persistent | `baseline` | 3 | throw | `SdkClientException` | - | - | - | false | - | 36 |
| `malformed-body` | persistent | `bridge` | 1 | throw | `SdkClientException` | - | - | - | false | - | 3 |
| `malformed-body` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 200 |
| `malformed-body` | transient | `bridge` | 1 | throw | `SdkClientException` | - | - | - | false | - | 3 |
| `empty-500` | persistent | `baseline` | 3 | throw | `DynamoDbException` | - | 500 | FAULTREQID000000000000000 | false | false | 111 |
| `empty-500` | persistent | `bridge` | 1 | throw | `DynamoDbException` | - | 0 | - | false | false | 4 |
| `empty-500` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 198 |
| `empty-500` | transient | `bridge` | 1 | throw | `DynamoDbException` | - | 0 | - | false | false | 2 |

Median wall time per arm across all cases: `baseline` 173 ms, `bridge` 2 ms

