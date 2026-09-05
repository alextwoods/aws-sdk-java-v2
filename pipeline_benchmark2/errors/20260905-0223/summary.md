# Error behavior sweep — 20260905-0223

Arms: `baseline` = published v2 2.46.10, `bridge` = `errbridge4` (ee9a74f69db-dirty).
21 cases (10 faults × persistent/transient, plus a no-fault control), 2 reps, port 19082.

**Nondeterminism across reps: 0.** Every field that this sweep compares was identical between rep 1
and rep 2 in both arms, so each row below is a deterministic outcome rather than a sample.

## Headline: attempt counts now match on all 21 cases

This is the first sweep in which the retry behavior of the two pipelines is indistinguishable:

| | base | bridge |
|---|---|---|
| retried 3× | `throughput-exceeded`, `throttling`, `internal-error`, `unavailable`, `unavailable-retry-after`, `empty-500`, `malformed-body`, `truncated-stream` | same 8 |
| not retried | `resource-not-found`, `conditional-check-failed` | same 2 |

`malformed-body` is the fix this run was built to confirm. It was 1 attempt vs 3 in run `20260905-0159`;
it is 3 vs 3 here. The cause was that v2 retries a torn response body only as a side effect of Jackson
2's exception hierarchy (`JsonEOFException extends IOException`), which does not hold in the shaded
Jackson 3 that smithy-java uses — see ledger 3.5. Two defects had to be fixed for it: the enricher's
classification path was unreachable for a non-`CallException` error, and `SerializationException` is a
plain `RuntimeException` carrying no `RetryInfo` at all.

## 14 remaining behavioral differences, plus 12 message-wording

Per `diff.md`, which keeps message text out of the behavioral count. Added together these are the "26 field
differences" an earlier revision of this file reported. Three distinct causes, none a retry difference:

### 1. `rawResponse` absent — 11 of the 12 cases

Every case where the bridge throws a service exception reports `rawResponse` absent where v2 reports it
present. `AwsServiceException` can carry the raw HTTP response and v2 populates it; the bridge does not,
because the enricher builds the v2 exception from a smithy `HttpResponse` and never converts it back
into the v2 `SdkHttpResponse` the field expects. Affects only callers that reach through to the raw
response.

### 2. `(SDK Attempt Count: N)` suffix absent — 11 cases

v2 appends the attempt count to the exception message. The bridge's messages are otherwise identical.
Cosmetic, but it means message-equality assertions in customer tests will fail.

Two cases also differ in the message *body*: the bridge drops the service's `injected fault` message
text and leads with the parenthetical for modeled errors it built itself.

### 3. `conditional-check-failed` type mismatch — 2 cases

`ConditionalCheckFailedException` in v2 vs `DynamoDbException` in the bridge. Known and recorded as
ledger 1.3: the error is modeled by DynamoDB but not declared by `GetItem`, and smithy's `TypeRegistry`
is per-operation. Deliberately not fixed.

### 4. `malformed-body` cause type — 1 case

`java.io.UncheckedIOException` (wrapping `JsonEOFException`) vs
`software.amazon.smithy.java.core.serde.SerializationException`. Both arms throw `SdkClientException`
with the same attempt count; only the cause type and its message text differ. This is the visible
residue of ledger 3.5 and is not fixable without either shading Jackson 2 into smithy-java or having v2
classify by something other than exception type.

## `truncated-stream` was inert in this run — do not cite it

The fault was added to measure ledger 3.6 (smithy's transport exceptions report `RetrySafety.NO`
unconditionally), and it did not: both arms show `statusCode=500`, `DynamoDbException`, 3 attempts,
which is `empty-500` by another name.

Cause is in the mock server, not in either SDK. The 25-byte body stayed in Jetty's response buffer, so
Jetty noticed the `Content-Length` shortfall while the status line was still uncommitted and sent a
clean 500 instead of aborting the connection. No `IOException` ever reached the client, so the retry
classification this fault exists to probe was never consulted.

Fixed by flushing the buffer before returning (`MockDdbServer`), which commits the response and leaves
the container no option but to abort. Re-measured in the following run with `errbridge5`/`errbase3`.

## Wall times

Not the point of this sweep — a persistent retryable fault spends most of its wall time in backoff, and
backoff is deliberately jittered. Recorded only to confirm nothing pathological: the bridge is faster
on the two cases dominated by deserialization work (`malformed-body` 113 ms vs 209 ms) and comparable
elsewhere.
