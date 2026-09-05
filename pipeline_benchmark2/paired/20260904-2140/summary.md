# Paired A/B summary — `20260904-2140`

Arms (baseline first): `baseline`, `bridge`

- `baseline`: sdk_commit `published-2`, 4 reps per case
- `bridge`: sdk_commit `ee9a74f69db`, 4 reps per case

**19 of 96 runs were not steady-state** (JIT still compiling inside the measured window): smithy/small-get, smithy/small-put, v2-sync-stripped/small-get, v2-sync-stripped/small-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `ee9a74f69db`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | baseline mean | bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 136.6 | 92.3 | -32.4% | ±1.1% | 4/4 |
| v2-sync-stripped | small-get | 137.4 | 82.1 | -40.3% | ±1.9% | 4/4 |
| smithy | small-get | 61.8 | 60.0 | -2.8% | ±4.4% | 3/4 |
| v2-sync | small-put | 132.3 | 87.8 | -33.5% | ±3.2% | 4/4 |
| v2-sync-stripped | small-put | 132.0 | 79.5 | -39.7% | ±1.2% | 4/4 |
| smithy | small-put | 59.1 | 59.7 | +1.1% | ±2.3% | 2/4 |
| v2-sync | batch-get | 580.3 | 469.8 | -19.0% | ±4.2% | 4/4 |
| v2-sync-stripped | batch-get | 563.6 | 464.4 | -17.6% | ±3.2% | 4/4 |
| smithy | batch-get | 324.6 | 319.9 | -1.4% | ±1.9% | 3/4 |
| v2-sync | batch-put | 749.6 | 424.6 | -43.3% | ±1.5% | 4/4 |
| v2-sync-stripped | batch-put | 781.1 | 396.9 | -49.1% | ±2.0% | 4/4 |
| smithy | batch-put | 337.2 | 335.2 | -0.6% | ±2.3% | 3/4 |

## mean latency µs

| client | scenario | baseline mean | bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 174.7 | 127.9 | -26.8% | ±1.0% | 4/4 |
| v2-sync-stripped | small-get | 176.5 | 117.3 | -33.5% | ±3.4% | 4/4 |
| smithy | small-get | 95.2 | 94.9 | -0.4% | ±2.2% | 3/4 |
| v2-sync | small-put | 170.6 | 124.8 | -26.8% | ±3.4% | 4/4 |
| v2-sync-stripped | small-put | 171.0 | 115.9 | -32.2% | ±2.0% | 4/4 |
| smithy | small-put | 91.6 | 100.4 | +9.5% | ±18.9% | 2/4 |
| v2-sync | batch-get | 634.9 | 522.4 | -17.7% | ±4.6% | 4/4 |
| v2-sync-stripped | batch-get | 616.0 | 510.8 | -17.0% | ±3.0% | 4/4 |
| smithy | batch-get | 369.5 | 359.6 | -2.5% | ±3.3% | 3/4 |
| v2-sync | batch-put | 793.5 | 461.7 | -41.8% | ±1.4% | 4/4 |
| v2-sync-stripped | batch-put | 823.2 | 436.1 | -47.0% | ±2.0% | 4/4 |
| smithy | batch-put | 383.0 | 381.2 | -0.4% | ±2.9% | 3/4 |

## wall µs/op (1/throughput)

| client | scenario | baseline mean | bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 174.9 | 128.1 | -26.8% | ±1.0% | 4/4 |
| v2-sync-stripped | small-get | 176.6 | 117.4 | -33.5% | ±3.4% | 4/4 |
| smithy | small-get | 95.3 | 95.1 | -0.3% | ±2.1% | 3/4 |
| v2-sync | small-put | 170.8 | 124.9 | -26.8% | ±3.3% | 4/4 |
| v2-sync-stripped | small-put | 171.2 | 116.0 | -32.2% | ±2.0% | 4/4 |
| smithy | small-put | 91.8 | 100.6 | +9.5% | ±18.9% | 2/4 |
| v2-sync | batch-get | 635.0 | 522.7 | -17.7% | ±4.6% | 4/4 |
| v2-sync-stripped | batch-get | 616.2 | 511.1 | -17.0% | ±3.0% | 4/4 |
| smithy | batch-get | 369.8 | 359.9 | -2.5% | ±3.4% | 3/4 |
| v2-sync | batch-put | 793.7 | 462.1 | -41.7% | ±1.4% | 4/4 |
| v2-sync-stripped | batch-put | 823.4 | 436.4 | -47.0% | ±2.0% | 4/4 |
| smithy | batch-put | 383.1 | 381.4 | -0.4% | ±2.9% | 3/4 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | baseline mean | bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 139.8 | 97.0 | -30.6% | ±0.9% | 4/4 |
| v2-sync-stripped | small-get | 141.2 | 86.6 | -38.7% | ±2.4% | 4/4 |
| smithy | small-get | 63.6 | 62.8 | -1.3% | ±4.5% | 1/4 |
| v2-sync | small-put | 135.8 | 93.2 | -31.2% | ±3.6% | 4/4 |
| v2-sync-stripped | small-put | 135.4 | 83.8 | -38.1% | ±0.7% | 4/4 |
| smithy | small-put | 61.0 | 61.5 | +0.8% | ±2.2% | 1/4 |
| v2-sync | batch-get | 589.0 | 479.2 | -18.6% | ±4.4% | 4/4 |
| v2-sync-stripped | batch-get | 571.6 | 472.6 | -17.3% | ±3.5% | 4/4 |
| smithy | batch-get | 330.6 | 325.5 | -1.5% | ±2.7% | 3/4 |
| v2-sync | batch-put | 755.6 | 429.6 | -43.1% | ±1.5% | 4/4 |
| v2-sync-stripped | batch-put | 787.2 | 404.7 | -48.5% | ±2.0% | 4/4 |
| smithy | batch-put | 342.6 | 339.8 | -0.8% | ±2.5% | 3/4 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `baseline` | 167.5 | 174.9 | 179.0 | 6.9% |
| v2-sync | small-get | `bridge` | 122.5 | 128.1 | 132.1 | 7.8% |
| v2-sync-stripped | small-get | `baseline` | 173.9 | 176.6 | 182.7 | 5.1% |
| v2-sync-stripped | small-get | `bridge` | 113.7 | 117.4 | 124.8 | 9.8% |
| smithy | small-get | `baseline` | 91.3 | 95.3 | 99.9 | 9.4% |
| smithy | small-get | `bridge` | 89.7 | 95.1 | 102.6 | 14.4% |
| v2-sync | small-put | `baseline` | 162.9 | 170.8 | 176.9 | 8.6% |
| v2-sync | small-put | `bridge` | 122.2 | 124.9 | 130.2 | 6.5% |
| v2-sync-stripped | small-put | `baseline` | 164.7 | 171.2 | 178.4 | 8.3% |
| v2-sync-stripped | small-put | `bridge` | 111.1 | 116.0 | 121.3 | 9.2% |
| smithy | small-put | `baseline` | 88.6 | 91.8 | 96.6 | 9.0% |
| smithy | small-put | `bridge` | 87.9 | 100.6 | 128.3 | 46.0% |
| v2-sync | batch-get | `baseline` | 622.2 | 635.0 | 666.0 | 7.0% |
| v2-sync | batch-get | `bridge` | 482.4 | 522.7 | 550.3 | 14.1% |
| v2-sync-stripped | batch-get | `baseline` | 591.4 | 616.2 | 632.4 | 6.9% |
| v2-sync-stripped | batch-get | `bridge` | 505.7 | 511.1 | 516.7 | 2.2% |
| smithy | batch-get | `baseline` | 348.6 | 369.8 | 398.5 | 14.3% |
| smithy | batch-get | `bridge` | 352.9 | 359.9 | 374.8 | 6.2% |
| v2-sync | batch-put | `baseline` | 757.4 | 793.7 | 846.0 | 11.7% |
| v2-sync | batch-put | `bridge` | 454.9 | 462.1 | 478.4 | 5.2% |
| v2-sync-stripped | batch-put | `baseline` | 793.8 | 823.4 | 855.9 | 7.8% |
| v2-sync-stripped | batch-put | `bridge` | 428.8 | 436.4 | 452.7 | 5.6% |
| smithy | batch-put | `baseline` | 377.3 | 383.1 | 393.6 | 4.3% |
| smithy | batch-put | `bridge` | 372.1 | 381.4 | 391.9 | 5.3% |

