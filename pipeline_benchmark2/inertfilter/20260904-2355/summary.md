# Client sweep summary — `20260904-2355`

Reference client: `v2-sync`. 4 interleaved reps per case, 32 runs total.

All runs from one artifact (phase `inertfilter`, sdk_commit `ee9a74f69db`), so the client is the only difference.

**22 of 32 runs were not steady-state** (JIT still compiling inside the measured window): v2-sync-inert-interceptors/small-put, v2-sync-strip-interceptors/small-get, v2-sync-strip-interceptors/small-put, v2-sync-stripped/small-get, v2-sync-stripped/small-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or --warmup-max-seconds.

## application cpu µs/op

| client | small-get mean | vs v2-sync | spread | wins | small-put mean | vs v2-sync | spread | wins |
|--------|----:|----:|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 85.2 | — | — | — | 80.4 | — | — | — |
| `v2-sync-inert-interceptors` | 91.5 | +7.4% | ±0.4% | 0/4 | 87.2 | +8.4% | ±2.8% | 0/4 |
| `v2-sync-strip-interceptors` | 85.8 | +0.7% | ±2.3% | 2/4 | 80.4 | +0.0% | ±1.4% | 2/4 |
| `v2-sync-stripped` | 81.1 | -4.8% | ±1.0% | 4/4 | 75.4 | -6.2% | ±3.0% | 4/4 |

## mean latency µs

| client | small-get mean | vs v2-sync | spread | wins | small-put mean | vs v2-sync | spread | wins |
|--------|----:|----:|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 119.7 | — | — | — | 117.2 | — | — | — |
| `v2-sync-inert-interceptors` | 126.6 | +5.8% | ±1.3% | 0/4 | 123.5 | +5.4% | ±3.3% | 0/4 |
| `v2-sync-strip-interceptors` | 121.6 | +1.6% | ±4.1% | 2/4 | 114.7 | -2.2% | ±1.7% | 4/4 |
| `v2-sync-stripped` | 117.0 | -2.2% | ±2.5% | 3/4 | 110.1 | -6.1% | ±3.3% | 4/4 |

## wall µs/op (1/throughput)

| client | small-get mean | vs v2-sync | spread | wins | small-put mean | vs v2-sync | spread | wins |
|--------|----:|----:|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 119.8 | — | — | — | 117.3 | — | — | — |
| `v2-sync-inert-interceptors` | 126.8 | +5.8% | ±1.3% | 0/4 | 123.7 | +5.4% | ±3.3% | 0/4 |
| `v2-sync-strip-interceptors` | 121.7 | +1.7% | ±4.1% | 2/4 | 114.8 | -2.2% | ±1.6% | 4/4 |
| `v2-sync-stripped` | 117.2 | -2.2% | ±2.5% | 3/4 | 110.2 | -6.1% | ±3.3% | 4/4 |

## process cpu µs/op (includes JIT/GC/VM)

| client | small-get mean | vs v2-sync | spread | wins | small-put mean | vs v2-sync | spread | wins |
|--------|----:|----:|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 89.1 | — | — | — | 84.0 | — | — | — |
| `v2-sync-inert-interceptors` | 95.8 | +7.5% | ±1.8% | 0/4 | 91.5 | +8.9% | ±3.4% | 0/4 |
| `v2-sync-strip-interceptors` | 90.2 | +1.3% | ±3.8% | 1/4 | 84.1 | +0.1% | ±1.8% | 3/4 |
| `v2-sync-stripped` | 85.2 | -4.3% | ±3.2% | 4/4 | 79.1 | -5.8% | ±2.6% | 4/4 |

## Per-client run-to-run spread

Unpaired noise, for comparison with the paired spreads above. wall µs/op.

| client | scenario | min | mean | max | spread |
|--------|----------|----:|----:|----:|-------:|
| `v2-sync` | small-get | 116.9 | 119.8 | 122.8 | 5.0% |
| `v2-sync` | small-put | 115.5 | 117.3 | 118.9 | 2.9% |
| `v2-sync-inert-interceptors` | small-get | 124.7 | 126.8 | 127.6 | 2.3% |
| `v2-sync-inert-interceptors` | small-put | 121.2 | 123.7 | 128.9 | 6.4% |
| `v2-sync-strip-interceptors` | small-get | 117.8 | 121.7 | 124.9 | 6.0% |
| `v2-sync-strip-interceptors` | small-put | 113.7 | 114.8 | 117.4 | 3.3% |
| `v2-sync-stripped` | small-get | 115.9 | 117.2 | 118.9 | 2.6% |
| `v2-sync-stripped` | small-put | 105.9 | 110.2 | 113.6 | 7.3% |

