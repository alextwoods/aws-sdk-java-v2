# Client sweep summary — `20260904-2313`

Reference client: `v2-sync`. 4 interleaved reps per case, 48 runs total.

All runs from one artifact (phase `components`, sdk_commit `ee9a74f69db`), so the client is the only difference.

**33 of 48 runs were not steady-state** (JIT still compiling inside the measured window): v2-sync-strip-endpoints/small-get, v2-sync-strip-endpoints/small-put, v2-sync-strip-errors/small-get, v2-sync-strip-errors/small-put, v2-sync-strip-interceptors/small-get, v2-sync-strip-interceptors/small-put, v2-sync-strip-retries/small-get, v2-sync-strip-retries/small-put, v2-sync-stripped/small-get, v2-sync-stripped/small-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or --warmup-max-seconds.

## application cpu µs/op

| client | small-get mean | vs v2-sync | spread | wins | small-put mean | vs v2-sync | spread | wins |
|--------|----:|----:|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 91.6 | — | — | — | 86.9 | — | — | — |
| `v2-sync-strip-endpoints` | 87.9 | -4.0% | ±2.6% | 4/4 | 82.5 | -5.1% | ±2.0% | 4/4 |
| `v2-sync-strip-retries` | 91.2 | -0.4% | ±3.0% | 3/4 | 86.6 | -0.3% | ±2.9% | 2/4 |
| `v2-sync-strip-errors` | 91.6 | +0.1% | ±3.7% | 3/4 | 86.5 | -0.4% | ±3.4% | 2/4 |
| `v2-sync-strip-interceptors` | 86.2 | -5.9% | ±1.5% | 4/4 | 79.6 | -8.4% | ±1.5% | 4/4 |
| `v2-sync-stripped` | 79.0 | -13.7% | ±2.7% | 4/4 | 74.9 | -13.8% | ±1.2% | 4/4 |

## mean latency µs

| client | small-get mean | vs v2-sync | spread | wins | small-put mean | vs v2-sync | spread | wins |
|--------|----:|----:|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 126.7 | — | — | — | 121.5 | — | — | — |
| `v2-sync-strip-endpoints` | 122.3 | -3.5% | ±2.6% | 4/4 | 117.2 | -3.6% | ±2.0% | 4/4 |
| `v2-sync-strip-retries` | 125.4 | -1.1% | ±2.9% | 2/4 | 123.0 | +1.2% | ±3.9% | 1/4 |
| `v2-sync-strip-errors` | 126.7 | -0.1% | ±3.6% | 1/4 | 121.3 | -0.1% | ±4.4% | 1/4 |
| `v2-sync-strip-interceptors` | 121.5 | -4.1% | ±1.6% | 4/4 | 114.2 | -6.1% | ±1.8% | 4/4 |
| `v2-sync-stripped` | 113.4 | -10.5% | ±2.5% | 4/4 | 108.6 | -10.6% | ±1.7% | 4/4 |

## wall µs/op (1/throughput)

| client | small-get mean | vs v2-sync | spread | wins | small-put mean | vs v2-sync | spread | wins |
|--------|----:|----:|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 126.8 | — | — | — | 121.7 | — | — | — |
| `v2-sync-strip-endpoints` | 122.4 | -3.5% | ±2.6% | 4/4 | 117.3 | -3.6% | ±2.0% | 4/4 |
| `v2-sync-strip-retries` | 125.5 | -1.0% | ±2.9% | 2/4 | 123.1 | +1.2% | ±3.9% | 1/4 |
| `v2-sync-strip-errors` | 126.8 | -0.0% | ±3.5% | 1/4 | 121.4 | -0.2% | ±4.5% | 1/4 |
| `v2-sync-strip-interceptors` | 121.6 | -4.1% | ±1.6% | 4/4 | 114.2 | -6.1% | ±1.8% | 4/4 |
| `v2-sync-stripped` | 113.5 | -10.5% | ±2.5% | 4/4 | 108.7 | -10.7% | ±1.6% | 4/4 |

## process cpu µs/op (includes JIT/GC/VM)

| client | small-get mean | vs v2-sync | spread | wins | small-put mean | vs v2-sync | spread | wins |
|--------|----:|----:|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 95.0 | — | — | — | 90.8 | — | — | — |
| `v2-sync-strip-endpoints` | 92.2 | -2.9% | ±2.8% | 3/4 | 86.5 | -4.6% | ±3.0% | 4/4 |
| `v2-sync-strip-retries` | 96.0 | +1.0% | ±3.6% | 3/4 | 90.5 | -0.2% | ±3.7% | 2/4 |
| `v2-sync-strip-errors` | 96.0 | +1.0% | ±4.8% | 1/4 | 90.5 | -0.2% | ±4.6% | 2/4 |
| `v2-sync-strip-interceptors` | 89.9 | -5.4% | ±1.7% | 4/4 | 83.8 | -7.6% | ±1.7% | 4/4 |
| `v2-sync-stripped` | 83.1 | -12.5% | ±3.3% | 4/4 | 78.0 | -14.0% | ±0.5% | 4/4 |

## Per-client run-to-run spread

Unpaired noise, for comparison with the paired spreads above. wall µs/op.

| client | scenario | min | mean | max | spread |
|--------|----------|----:|----:|----:|-------:|
| `v2-sync` | small-get | 125.7 | 126.8 | 127.6 | 1.5% |
| `v2-sync` | small-put | 120.0 | 121.7 | 124.6 | 3.8% |
| `v2-sync-strip-endpoints` | small-get | 118.6 | 122.4 | 125.6 | 5.9% |
| `v2-sync-strip-endpoints` | small-put | 116.5 | 117.3 | 117.9 | 1.2% |
| `v2-sync-strip-retries` | small-get | 122.3 | 125.5 | 128.6 | 5.2% |
| `v2-sync-strip-retries` | small-put | 119.6 | 123.1 | 126.6 | 5.9% |
| `v2-sync-strip-errors` | small-get | 120.8 | 126.8 | 130.6 | 8.1% |
| `v2-sync-strip-errors` | small-put | 116.4 | 121.4 | 124.6 | 7.0% |
| `v2-sync-strip-interceptors` | small-get | 120.5 | 121.6 | 123.3 | 2.3% |
| `v2-sync-strip-interceptors` | small-put | 112.7 | 114.2 | 116.4 | 3.3% |
| `v2-sync-stripped` | small-get | 110.4 | 113.5 | 116.2 | 5.3% |
| `v2-sync-stripped` | small-put | 106.0 | 108.7 | 113.7 | 7.3% |

