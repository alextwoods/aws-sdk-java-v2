# Paired A/B summary — `20260905-1926`

Arms (baseline first): `s3-stock`, `s3-bridge`

- `s3-stock`: sdk_commit `published-2.46.10`, 5 reps per case
- `s3-bridge`: sdk_commit `unrecorded`, 5 reps per case

**9 of 20 runs were not steady-state** (JIT still compiling inside the measured window): s3-v2-sync/get-object-256k, s3-v2-sync/put-object-256k. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `697d4f08734`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-256k | 473.1 | 423.5 | -10.5% | ±3.3% | 5/5 |
| s3-v2-sync | put-object-256k | 582.8 | 520.9 | -10.6% | ±2.6% | 5/5 |

## mean latency µs

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-256k | 535.4 | 494.7 | -7.5% | ±5.5% | 4/5 |
| s3-v2-sync | put-object-256k | 622.5 | 568.3 | -8.7% | ±3.0% | 5/5 |

## wall µs/op (1/throughput)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-256k | 536.3 | 495.5 | -7.5% | ±5.5% | 4/5 |
| s3-v2-sync | put-object-256k | 623.5 | 569.2 | -8.7% | ±3.0% | 5/5 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-256k | 490.2 | 451.8 | -7.6% | ±9.1% | 4/5 |
| s3-v2-sync | put-object-256k | 605.8 | 552.8 | -8.7% | ±4.5% | 5/5 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| s3-v2-sync | get-object-256k | `s3-stock` | 523.7 | 536.3 | 554.8 | 5.9% |
| s3-v2-sync | get-object-256k | `s3-bridge` | 466.1 | 495.5 | 530.8 | 13.9% |
| s3-v2-sync | put-object-256k | `s3-stock` | 607.8 | 623.5 | 635.0 | 4.5% |
| s3-v2-sync | put-object-256k | `s3-bridge` | 551.2 | 569.2 | 580.6 | 5.3% |

