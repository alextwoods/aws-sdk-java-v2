# Error behavior sweep — 20260905-0246

Arms: `baseline` = published v2 2.46.10, `bridge` = `errbridge7` (ee9a74f69db-dirty).
23 cases, 2 reps, port 19082. Nondeterminism across reps: 0.

`errbridge7` differs from `errbridge6` (run `20260905-0239`) only in `V2TransportBridge.send`, which
threw a `V2RetryableError` — a `CallException` reporting `RetrySafety.YES` — in place of the remapped
transport exception. This run is the verification of that fix.

## Negative result: no change

| `connection-reset` | baseline | errbridge6 | errbridge7 |
|---|---|---|---|
| attempts, persistent | 3 | 1 | **1** |
| attempts, transient | 3 (ok) | 1 (throw) | **1 (throw)** |
| wall, persistent | 103 ms | 2 ms | **2 ms** |

20 behavioral differences (plus 14 message-wording, counted separately — see `diff.md`), identical to run
0239 row for row. The bytecode confirms the change is in the
jar (`V2TransportBridge.send` constructs and throws `V2RetryableError`), so the substitution happens and
is simply not consulted.

## Why: the retry loop is below the transport, not above it

Decompiling `ClientPipeline` (smithy-java 1.6.1) locates every retry decision inside **`deserialize`**,
in the `catch (RuntimeException)` around `deserializeResponse`: that is where `RetryInfo` is read,
`refreshRetryToken` is called, and the recursive `retry(...)` happens. Above it:

- `doSendOrRetry` — despite the name — has **no exception table at all**.
- `afterIdentity` has one handler, and it rethrows:
  `catch (Exception e) { throw ClientTransport.remapExceptions(e); }`.
- Nothing above either of those catches anything.

So the only code path that can produce a second attempt is one that got far enough to have a response to
deserialize. A failure in `transport.send` is remapped and propagated to the caller, and no exception
thrown from the transport — of any type, reporting any `RetrySafety` — can change that.

This reframes ledger 3.6. The hardcoded `TransportException.isRetrySafe() == NO` is not the cause but a
consequence: on this path nothing reads it. **smithy-java 1.6.1 does not retry transport failures**, and a
native smithy-java client behaves the same way. It is not a bridging artifact and the bridge cannot fix
it short of wrapping `Client.call` in a retry loop of its own, duplicating the strategy, the token bucket,
and the attempt accounting.

The change was therefore reverted. `V2TransportBridge.send`'s catch clause now carries a comment recording
that this was tried and measured inert, because it is precisely the fix a later reader would attempt.

## Standing state of the error sweep

Of 23 cases, **21 match on attempt count and outcome**. The two that do not are both `connection-reset`,
for the structural reason above. Every response the service sends — well-formed, malformed, empty, or
with a status alone — is now retried exactly as v2 retries it; nothing that fails before a response
arrives is retried at all.

The other differences are unchanged and all cosmetic or metadata: `rawResponse` never populated (10
behavioral), the `conditional-check-failed` type mismatch (2, ledger 1.3), the `malformed-body` cause type
(1, ledger 3.5), and message text including the missing `(SDK Attempt Count: N)` suffix (14, counted as
wording rather than behavior).
