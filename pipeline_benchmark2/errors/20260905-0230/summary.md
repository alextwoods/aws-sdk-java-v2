# Error behavior sweep — 20260905-0230

Arms: `baseline` = published v2 2.46.10, `bridge` = `errbridge5` (ee9a74f69db-dirty).
21 cases, 2 reps, port 19082. Nondeterminism across reps: 0.

Same as run `20260905-0223` except that `truncated-stream` now actually fires — the mock server flushes
the response buffer before returning, so Jetty has to abort the connection instead of converting the
`Content-Length` shortfall into a tidy 500.

## Attempt counts identical on all 21 cases; 13 behavioral differences

Per `diff.md`: 13 behavioral, plus 12 message-wording differences counted separately (25 if the two classes
are added, which is how an earlier revision of this file reported it).

Unchanged in substance from run 0223 (14 behavioral), the one fewer being a `statusCode` that no longer
differs now that `truncated-stream` produces no HTTP status at all in either arm. The residual
differences are the same four groups: `rawResponse` never populated, the `(SDK Attempt Count: N)` message
suffix absent, the `conditional-check-failed` type mismatch (ledger 1.3), and the `malformed-body` cause
type (ledger 3.5).

## `truncated-stream` fires — and disproves what it was built to measure

| | baseline | bridge |
|---|---|---|
| attempts (persistent) | 3 | 3 |
| exception | `SdkClientException` | `SdkClientException` |
| cause | `java.io.UncheckedIOException` | `java.io.UncheckedIOException` |
| statusCode | — | — |
| transient variant | recovers on 3rd attempt | recovers on 3rd attempt |

Identical in both arms, including the cause type. The fault was added to measure ledger 3.6 — smithy's
`TransportException` hardcoding `isRetrySafe() == NO` — and **it does not reach that code at all.**

The reason is that bodies are streamed. `V2TransportBridge.send` wraps only
`v2HttpClient.prepareRequest(...).call()` in its `catch (IOException)`, and that call returns as soon as
the response *headers* arrive; the body is handed onward as
`DataStream.ofInputStream(...)` and drained later by the codec. A torn body therefore fails **after**
`send` has already returned, so `ClientTransport.remapExceptions` never runs and no `TransportException`
is ever constructed. What reaches the retry layer is the `UncheckedIOException` the stream threw, which
`V2ErrorEnricher.retriedByV2` matches on the `IOException` rule, exactly as v2 does.

This is worth recording as its own finding, because it cuts both ways:

- It is why the bridge gets this case right, and it does so through the same `V2RetryableError`
  substitution as ledger 3.5 (an `UncheckedIOException` is not a `CallException`, so it too arrives with
  no `RetryInfo`).
- It means 3.6 remains **unmeasured**, and that the surface of 3.6 is narrower than first assumed: it is
  reachable only for failures during connect, TLS handshake, request send, or response-header read — not
  for anything that goes wrong once a response has started arriving.

The narrower surface is not a reprieve. A connection reset before headers is the most common transport
failure in production — an idle pooled connection reaped by a load balancer looks precisely like that —
and it is the one v2 retries most consistently.

## Follow-up

Added a `connection-reset` fault: the server closes the socket with no response at all, which is the only
way to make the failure land inside `V2TransportBridge`'s `catch`. The request is counted before the
fault is claimed, so `/stats` still reports the attempt count — necessary here, because a transport
failure produces an exception carrying no attempt number for the probe to read. Measured in the next run.
