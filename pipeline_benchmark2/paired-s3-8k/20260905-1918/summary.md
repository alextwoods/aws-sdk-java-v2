# Paired A/B summary — `20260905-1918`

Arms (baseline first): `s3-stock`, `s3-bridge`

- `s3-stock`: sdk_commit `published-2.46.10`, 5 reps per case
- `s3-bridge`: sdk_commit `unrecorded`, 5 reps per case

**11 of 20 runs were not steady-state** (JIT still compiling inside the measured window): s3-v2-sync/get-object-8k, s3-v2-sync/put-object-8k. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `697d4f08734`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8k | 174.4 | 116.5 | -33.2% | ±2.6% | 5/5 |
| s3-v2-sync | put-object-8k | 182.9 | 123.8 | -32.3% | ±1.7% | 5/5 |

## mean latency µs

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8k | 224.1 | 164.1 | -26.7% | ±4.9% | 5/5 |
| s3-v2-sync | put-object-8k | 226.5 | 160.7 | -29.0% | ±2.6% | 5/5 |

## wall µs/op (1/throughput)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8k | 224.5 | 164.7 | -26.6% | ±5.0% | 5/5 |
| s3-v2-sync | put-object-8k | 227.0 | 161.3 | -28.9% | ±2.5% | 5/5 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8k | 200.2 | 133.3 | -32.6% | ±13.9% | 5/5 |
| s3-v2-sync | put-object-8k | 195.1 | 129.8 | -33.1% | ±5.9% | 5/5 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| s3-v2-sync | get-object-8k | `s3-stock` | 215.9 | 224.5 | 232.6 | 7.7% |
| s3-v2-sync | get-object-8k | `s3-bridge` | 155.4 | 164.7 | 179.2 | 15.3% |
| s3-v2-sync | put-object-8k | `s3-stock` | 219.6 | 227.0 | 234.5 | 6.8% |
| s3-v2-sync | put-object-8k | `s3-bridge` | 157.6 | 161.3 | 168.2 | 6.7% |

