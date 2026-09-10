# Paired A/B summary — `20260905-2014`

Arms (baseline first): `s3-stock`, `s3-bridge`

- `s3-stock`: sdk_commit `published-2.46.10`, 5 reps per case
- `s3-bridge`: sdk_commit `unrecorded`, 5 reps per case

**5 of 20 runs were not steady-state** (JIT still compiling inside the measured window): s3-v2-sync/get-object-256k, s3-v2-sync/put-object-256k. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `697d4f08734`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-256k | 462.6 | 414.2 | -10.5% | ±0.5% | 5/5 |
| s3-v2-sync | put-object-256k | 567.9 | 499.0 | -12.1% | ±1.9% | 5/5 |

## mean latency µs

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-256k | 519.7 | 477.5 | -8.1% | ±1.5% | 5/5 |
| s3-v2-sync | put-object-256k | 605.3 | 536.5 | -11.3% | ±3.5% | 5/5 |

## wall µs/op (1/throughput)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-256k | 520.5 | 478.2 | -8.1% | ±1.5% | 5/5 |
| s3-v2-sync | put-object-256k | 606.1 | 537.2 | -11.3% | ±3.5% | 5/5 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-256k | 476.1 | 427.1 | -10.2% | ±3.3% | 5/5 |
| s3-v2-sync | put-object-256k | 578.3 | 507.7 | -12.2% | ±2.7% | 5/5 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| s3-v2-sync | get-object-256k | `s3-stock` | 513.7 | 520.5 | 531.0 | 3.4% |
| s3-v2-sync | get-object-256k | `s3-bridge` | 474.7 | 478.2 | 480.8 | 1.3% |
| s3-v2-sync | put-object-256k | `s3-stock` | 593.4 | 606.1 | 636.2 | 7.2% |
| s3-v2-sync | put-object-256k | `s3-bridge` | 525.1 | 537.2 | 547.7 | 4.3% |

