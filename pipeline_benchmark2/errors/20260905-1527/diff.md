# Error behavior diff — `20260905-1527`

Baseline arm: `baseline`. Compared against: `bridge`.
5 cases, 50 rows.

Every arm gave the same answer on every rep, so each case below is a single deterministic outcome.

## `baseline` vs `bridge`

**2 behavioral difference(s).**

| fault | case | field | `baseline` | `bridge` |
|---|---|---|---|---|
| `internal-error` | persistent | `rawResponse` | present | absent |
| `throughput-exceeded` | persistent | `rawResponse` | present | absent |

Wall time differing by 2.0x or more — read as a backoff signal, not a latency:

| fault | case | `baseline` ms | `bridge` ms |
|---|---|---:|---:|
| `none` | control | 6 | 3 |

Message wording differences (not behavioral on their own):

| fault | case | `baseline` | `bridge` |
|---|---|---|---|
| `internal-error` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `(Service:_DynamoDb;_Status_Code:_500;_Request_ID:_FAULTREQID000000000000000)` |
| `throughput-exceeded` | persistent | `injected_fault_(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)_(SDK_Attempt_Count:_3)` | `(Service:_DynamoDb;_Status_Code:_400;_Request_ID:_FAULTREQID000000000000000)` |

## What each arm did

| fault | case | arm | attempts | outcome | exception | errorCode | status | requestId | retryable | throttling | wall ms |
|---|---|---|---:|---|---|---|---:|---|---|---|---:|
| `none` | control | `baseline` | 1 | ok | `-` | - | - | - | - | - | 6 |
| `none` | control | `bridge` | 1 | ok | `-` | - | - | - | - | - | 3 |
| `internal-error` | persistent | `baseline` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 426 |
| `internal-error` | persistent | `bridge` | 3 | throw | `InternalServerErrorException` | InternalServerError | 500 | FAULTREQID000000000000000 | false | false | 431 |
| `internal-error` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 410 |
| `internal-error` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 408 |
| `throughput-exceeded` | persistent | `baseline` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 412 |
| `throughput-exceeded` | persistent | `bridge` | 3 | throw | `ProvisionedThroughputExceededException` | ProvisionedThroughputExceededException | 400 | FAULTREQID000000000000000 | false | true | 408 |
| `throughput-exceeded` | transient | `baseline` | 3 | ok | `-` | - | - | - | - | - | 410 |
| `throughput-exceeded` | transient | `bridge` | 3 | ok | `-` | - | - | - | - | - | 407 |

Median wall time per arm across all cases: `baseline` 408 ms, `bridge` 407 ms

