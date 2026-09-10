# Paired A/B summary — `20260905-1935`

Arms (baseline first): `s3-stock`, `s3-bridge`

- `s3-stock`: sdk_commit `published-2.46.10`, 5 reps per case
- `s3-bridge`: sdk_commit `unrecorded`, 5 reps per case

**20 of 20 runs were not steady-state** (JIT still compiling inside the measured window): s3-v2-sync/get-object-8m, s3-v2-sync/put-object-8m. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `697d4f08734`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8m | 10,935.4 | 10,918.2 | -0.1% | ±2.4% | 3/5 |
| s3-v2-sync | put-object-8m | 13,080.4 | 12,682.2 | -3.0% | ±2.1% | 5/5 |

## mean latency µs

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8m | 12,108.9 | 12,087.2 | -0.1% | ±2.3% | 2/5 |
| s3-v2-sync | put-object-8m | 13,308.9 | 12,913.2 | -2.9% | ±2.0% | 5/5 |

## wall µs/op (1/throughput)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8m | 12,111.1 | 12,089.3 | -0.1% | ±2.3% | 2/5 |
| s3-v2-sync | put-object-8m | 13,311.3 | 12,915.4 | -2.9% | ±2.0% | 5/5 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | s3-stock mean | s3-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| s3-v2-sync | get-object-8m | 12,340.0 | 12,336.0 | +0.1% | ±4.6% | 3/5 |
| s3-v2-sync | put-object-8m | 14,490.0 | 14,100.0 | -2.7% | ±2.2% | 5/5 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| s3-v2-sync | get-object-8m | `s3-stock` | 11,870.8 | 12,111.1 | 12,428.0 | 4.7% |
| s3-v2-sync | get-object-8m | `s3-bridge` | 11,953.6 | 12,089.3 | 12,280.0 | 2.7% |
| s3-v2-sync | put-object-8m | `s3-stock` | 13,087.6 | 13,311.3 | 13,694.9 | 4.6% |
| s3-v2-sync | put-object-8m | `s3-bridge` | 12,853.0 | 12,915.4 | 12,985.6 | 1.0% |

