# Paired A/B summary — `20260905-2027`

Arms (baseline first): `s3-stock`, `s3-bridge`

- `s3-stock`: sdk_commit `published-2.46.10`, 5 reps per case
- `s3-bridge`: sdk_commit `unrecorded`, 5 reps per case

**20 of 20 runs were not steady-state** (JIT still compiling inside the measured window): s3-v2-sync/get-object-8m, s3-v2-sync/put-object-8m. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `697d4f08734`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8m | 10,892.2 | 10,795.0 | -0.9% | ±2.8% | 3/5 |
| s3-v2-sync | put-object-8m | 12,989.9 | 12,614.1 | -2.9% | ±1.1% | 5/5 |

## mean latency µs

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8m | 11,984.9 | 11,890.8 | -0.8% | ±2.0% | 3/5 |
| s3-v2-sync | put-object-8m | 13,200.9 | 12,824.9 | -2.8% | ±1.2% | 5/5 |

## wall µs/op (1/throughput)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8m | 11,987.1 | 11,892.9 | -0.8% | ±2.0% | 3/5 |
| s3-v2-sync | put-object-8m | 13,203.1 | 12,827.0 | -2.8% | ±1.2% | 5/5 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8m | 12,089.3 | 11,846.7 | -2.0% | ±2.8% | 4/5 |
| s3-v2-sync | put-object-8m | 14,037.3 | 13,654.7 | -2.7% | ±2.3% | 4/5 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| s3-v2-sync | get-object-8m | `s3-stock` | 11,807.6 | 11,987.1 | 12,179.4 | 3.1% |
| s3-v2-sync | get-object-8m | `s3-bridge` | 11,589.3 | 11,892.9 | 12,031.3 | 3.8% |
| s3-v2-sync | put-object-8m | `s3-stock` | 13,086.3 | 13,203.1 | 13,363.7 | 2.1% |
| s3-v2-sync | put-object-8m | `s3-bridge` | 12,711.3 | 12,827.0 | 12,979.5 | 2.1% |

