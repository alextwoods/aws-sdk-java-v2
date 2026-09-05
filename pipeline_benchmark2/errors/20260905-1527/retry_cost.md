# Retry timing — `20260905-1527`

Backoff mode: **`fixed:200`**. Arms: `baseline`, `bridge`. Times in **milliseconds**.

Delay is fixed and unjittered, so `implied delay` must be `(attempts-1) x 200 ms` in every arm.

Warm 1-attempt control (the floor every row below stands on): `baseline` 2 ms, `bridge` 1 ms

| fault | case | arm | attempts | n | median ms | mean ms | sd | min | max | over control | per extra attempt |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `none` | control | `baseline` | 1 | 5 | 2 | 3.0 | 1.7 | 2 | 6 | 0 | - |
| `none` | control | `bridge` | 1 | 5 | 1 | 1.6 | 0.9 | 1 | 3 | 0 | - |
| `internal-error` | persistent | `baseline` | 3 | 5 | 408 | 411.4 | 8.2 | 407 | 426 | 406 | 203.0 |
| `internal-error` | persistent | `bridge` | 3 | 5 | 407 | 411.6 | 10.9 | 406 | 431 | 406 | 203.0 |
| `internal-error` | transient | `baseline` | 3 | 5 | 408 | 408.4 | 1.1 | 407 | 410 | 406 | 203.0 |
| `internal-error` | transient | `bridge` | 3 | 5 | 407 | 406.8 | 0.8 | 406 | 408 | 406 | 203.0 |
| `throughput-exceeded` | persistent | `baseline` | 3 | 5 | 408 | 408.8 | 2.2 | 407 | 412 | 406 | 203.0 |
| `throughput-exceeded` | persistent | `bridge` | 3 | 5 | 408 | 407.4 | 0.9 | 406 | 408 | 407 | 203.5 |
| `throughput-exceeded` | transient | `baseline` | 3 | 5 | 408 | 408.0 | 1.2 | 407 | 410 | 406 | 203.0 |
| `throughput-exceeded` | transient | `bridge` | 3 | 5 | 407 | 406.4 | 0.9 | 405 | 407 | 406 | 203.0 |

## `baseline` vs each other arm

`P(faster)` is the chance a random sample from the arm beats a random `baseline` sample; 0.5 is indistinguishable, 1.0 is total separation. `95% CI` is a bootstrap interval on the difference of medians — it excludes 0 exactly when the difference is real at this sample size. Both are rank-based, because a handful of GC outliers make the standard deviation useless here while barely moving the median.

| fault | case | arm | median ms | vs base | ratio | P(faster) | 95% CI |
|---|---|---|---:|---:|---:|---:|---:|
| `none` | control | `bridge` | 1 | -1 | 0.50x | 0.80 | [-5, +0] |
| `internal-error` | persistent | `bridge` | 407 | -1 | 1.00x | 0.66 | [-19, +23] |
| `internal-error` | transient | `bridge` | 407 | -1 | 1.00x | 0.88 | [-3, +0] |
| `throughput-exceeded` | persistent | `bridge` | 408 | +0 | 1.00x | 0.66 | [-4, +1] |
| `throughput-exceeded` | transient | `bridge` | 407 | -1 | 1.00x | 0.88 | [-3, +0] |

`bridge` has the lower median in 4 of 5 cases, and the difference clears the bootstrap interval in 0 of 5. Nothing here separates the arms — which is the expected result for a backoff fidelity run, where the delay dominates and both arms take it from the same code.
