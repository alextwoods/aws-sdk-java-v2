# Paired A/B summary — `20260905-2003`

Arms (baseline first): `s3-stock`, `s3-bridge`

- `s3-stock`: sdk_commit `published-2.46.10`, 5 reps per case
- `s3-bridge`: sdk_commit `unrecorded`, 5 reps per case

**7 of 20 runs were not steady-state** (JIT still compiling inside the measured window): s3-v2-sync/get-object-8k, s3-v2-sync/put-object-8k. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `697d4f08734`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8k | 164.5 | 110.0 | -33.1% | ±1.0% | 5/5 |
| s3-v2-sync | put-object-8k | 174.0 | 118.8 | -31.7% | ±1.6% | 5/5 |

## mean latency µs

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8k | 206.9 | 151.2 | -26.9% | ±2.1% | 5/5 |
| s3-v2-sync | put-object-8k | 211.9 | 153.3 | -27.6% | ±2.5% | 5/5 |

## wall µs/op (1/throughput)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8k | 207.1 | 151.3 | -26.9% | ±2.1% | 5/5 |
| s3-v2-sync | put-object-8k | 212.1 | 153.4 | -27.6% | ±2.5% | 5/5 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8k | 171.0 | 115.4 | -32.5% | ±3.4% | 5/5 |
| s3-v2-sync | put-object-8k | 182.8 | 125.9 | -31.0% | ±5.5% | 5/5 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| s3-v2-sync | get-object-8k | `s3-stock` | 202.7 | 207.1 | 211.0 | 4.1% |
| s3-v2-sync | get-object-8k | `s3-bridge` | 147.8 | 151.3 | 155.9 | 5.5% |
| s3-v2-sync | put-object-8k | `s3-stock` | 205.7 | 212.1 | 217.2 | 5.6% |
| s3-v2-sync | put-object-8k | `s3-bridge` | 149.9 | 153.4 | 160.5 | 7.1% |

