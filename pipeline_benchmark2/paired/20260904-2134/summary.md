# Paired A/B summary — `20260904-2134`

Arms (baseline first): `bridge`, `bridge2`

- `bridge`: sdk_commit `ee9a74f69db`, 3 reps per case
- `bridge2`: sdk_commit `ee9a74f69db`, 3 reps per case

**6 of 12 runs were not steady-state** (JIT still compiling inside the measured window): v2-sync/small-get. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `ee9a74f69db`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | bridge mean | bridge2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 95.5 | 96.5 | +1.1% | ±3.3% | 1/3 |
| v2-sync | batch-get | 485.2 | 461.4 | -4.9% | ±6.5% | 3/3 |

## mean latency µs

| client | scenario | bridge mean | bridge2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 136.4 | 135.4 | -0.7% | ±3.3% | 1/3 |
| v2-sync | batch-get | 541.5 | 517.0 | -4.5% | ±6.2% | 3/3 |

## wall µs/op (1/throughput)

| client | scenario | bridge mean | bridge2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 136.6 | 135.6 | -0.7% | ±3.4% | 1/3 |
| v2-sync | batch-get | 542.2 | 517.8 | -4.5% | ±6.2% | 3/3 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | bridge mean | bridge2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 106.5 | 105.3 | -1.1% | ±3.9% | 1/3 |
| v2-sync | batch-get | 501.0 | 475.2 | -5.1% | ±6.3% | 3/3 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `bridge` | 133.4 | 136.6 | 140.0 | 4.9% |
| v2-sync | small-get | `bridge2` | 133.7 | 135.6 | 136.9 | 2.4% |
| v2-sync | batch-get | `bridge` | 534.1 | 542.2 | 547.5 | 2.5% |
| v2-sync | batch-get | `bridge2` | 483.8 | 517.8 | 536.8 | 11.0% |

