# Error behavior sweep — 20260905-0239

Arms: `baseline` = published v2 2.46.10, `bridge` = `errbridge6` (ee9a74f69db-dirty).
23 cases (11 faults × persistent/transient, plus a control), 2 reps, port 19082.
Nondeterminism across reps: 0.

First run to include `connection-reset`, and it is the run that finally measures ledger 3.6.

## Headline: the bridge does not retry a connection reset at all

| | baseline | bridge |
|---|---|---|
| attempts, persistent | **3** | **1** |
| attempts, transient (first 2 fail, 3rd would succeed) | **3** | **1** |
| outcome, transient | **ok** — item returned | **throw** |
| exception | `SdkClientException` | `SdkClientException` |
| cause | `org.apache.hc.core5.http.NoHttpResponseException` | `software.amazon.smithy.java.client.core.error.TransportException` |
| wall, persistent | 131 / 243 ms | **2 ms** |

The transient row is the one that matters: v2 recovers on the third attempt and returns the item; the
bridge throws on the first. A 2 ms wall time against v2's 131 ms is the same fact stated as a timing —
there is no backoff because there is no second attempt.

This is the most severe behavioral difference measured on this branch, and it is the most common
transport failure in production: an idle pooled connection reaped by a load balancer looks exactly like
this to a client.

## Why the existing mitigation did not fire

Two separate causes, and only the first was known before this run:

1. **`TransportException` hardcodes `isRetrySafe() == NO`** (ledger 3.6), and every one of
   `ClientTransport.remapExceptions`'s six targets descends from it. The setter `CallException` exposes
   writes a field the overridden getter ignores.

2. **The enricher has no hook on this path.** `V2ErrorEnricher` works through
   `modifyBeforeAttemptCompletion`, and `ClientPipeline` invokes that hook in exactly one place — inside
   `deserialize`. `afterIdentity` wraps `transport.send(...)` *and* `deserialize(...)` in a single
   `try { ... } catch (Exception e) { throw ClientTransport.remapExceptions(e); }`, so a failure in `send`
   is remapped and rethrown without ever reaching `deserialize`. The `V2RetryableError` substitution that
   fixes 3.5 and covers the rest of section 3 therefore cannot run here at all.

Cause 2 is why `truncated-stream` (run `20260905-0230`) looked fine while this does not, despite both
being torn connections: bodies are streamed, so a truncated body fails *after* `send` returned, inside
`deserialize`, where the hook exists.

## Fix

`V2TransportBridge.send` now classifies in its own catch clause: every `IOException` it catches is by
definition one v2 retries (v2's retryable set contains `IOException`, matched against the whole cause
chain), so it wraps the remapped `CallException` in a `V2RetryableError`. That survives the outer
`remapExceptions`, which passes a `CallException` through unchanged, and reaches the retry gate reporting
`YES`. Re-measured with `errbridge7`.

Note this leaves the transport as the one component that classifies its own errors. That is not tidy, but
it is forced: it is the only code in the pipeline that sees a transport failure before the retry gate.

## The other 22 cases

Attempt counts identical, as in runs 0223 and 0230. Of the 20 behavioral differences in `diff.md`, 7 belong
to `connection-reset` (`attempts` twice, plus `outcome`, `exception`, `cause` twice, `retryable`); the other
13 are the metadata groups from before — `rawResponse` never populated (10), the `conditional-check-failed`
type mismatch (2, ledger 1.3), and the `malformed-body` cause type (1, ledger 3.5). A further 14
differences are message wording, including the absent `(SDK Attempt Count: N)` suffix, counted separately
from behavior.
