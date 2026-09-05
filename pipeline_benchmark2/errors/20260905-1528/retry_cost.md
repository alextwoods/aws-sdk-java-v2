# Retry timing — `20260905-1528`

Backoff mode: **`immediate`**. Arms: `baseline`, `bridge`.

Delay is zero, so wall time is the cost of *retrying* — no sleep in it.

Warm 1-attempt control (the floor every row below stands on): `baseline` 1 ms, `bridge` 0 ms

| fault | case | arm | attempts | n | median ms | mean ms | sd | min | max | over control | per extra attempt |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `none` | control | `baseline` | 1 | 25 | 1 | 1.2 | 1.0 | 0 | 6 | 0 | - |
| `none` | control | `bridge` | 1 | 25 | 0 | 0.6 | 1.0 | 0 | 4 | 0 | - |
| `internal-error` | persistent | `baseline` | 3 | 25 | 3 | 4.0 | 3.7 | 2 | 18 | 2 | 1.0 |
| `internal-error` | persistent | `bridge` | 3 | 25 | 2 | 3.3 | 4.4 | 1 | 24 | 2 | 1.0 |
| `internal-error` | transient | `baseline` | 3 | 25 | 3 | 3.7 | 2.9 | 2 | 14 | 2 | 1.0 |
| `internal-error` | transient | `bridge` | 3 | 25 | 2 | 2.4 | 1.6 | 1 | 9 | 2 | 1.0 |
| `throughput-exceeded` | persistent | `baseline` | 3 | 25 | 3 | 3.8 | 2.4 | 2 | 11 | 2 | 1.0 |
| `throughput-exceeded` | persistent | `bridge` | 3 | 25 | 2 | 3.6 | 3.9 | 1 | 21 | 2 | 1.0 |
| `throughput-exceeded` | transient | `baseline` | 3 | 25 | 3 | 3.6 | 2.8 | 2 | 15 | 2 | 1.0 |
| `throughput-exceeded` | transient | `bridge` | 3 | 25 | 2 | 2.3 | 1.1 | 1 | 6 | 2 | 1.0 |
| `unavailable` | persistent | `baseline` | 3 | 25 | 3 | 4.1 | 3.0 | 2 | 13 | 2 | 1.0 |
| `unavailable` | persistent | `bridge` | 3 | 25 | 2 | 4.0 | 7.5 | 1 | 40 | 2 | 1.0 |
| `unavailable` | transient | `baseline` | 3 | 25 | 3 | 3.7 | 3.0 | 2 | 16 | 2 | 1.0 |
| `unavailable` | transient | `bridge` | 3 | 25 | 2 | 2.5 | 1.0 | 1 | 5 | 2 | 1.0 |
| `empty-500` | persistent | `baseline` | 3 | 25 | 3 | 3.4 | 1.6 | 2 | 8 | 2 | 1.0 |
| `empty-500` | persistent | `bridge` | 3 | 25 | 2 | 2.3 | 0.7 | 1 | 4 | 2 | 1.0 |
| `empty-500` | transient | `baseline` | 3 | 25 | 3 | 3.6 | 2.7 | 2 | 14 | 2 | 1.0 |
| `empty-500` | transient | `bridge` | 3 | 25 | 2 | 2.7 | 2.0 | 1 | 11 | 2 | 1.0 |
| `malformed-body` | persistent | `baseline` | 3 | 25 | 3 | 3.1 | 1.3 | 2 | 7 | 2 | 1.0 |
| `malformed-body` | persistent | `bridge` | 3 | 25 | 2 | 2.9 | 1.7 | 1 | 8 | 2 | 1.0 |
| `malformed-body` | transient | `baseline` | 3 | 25 | 2 | 3.0 | 1.4 | 2 | 7 | 1 | 0.5 |
| `malformed-body` | transient | `bridge` | 3 | 25 | 2 | 2.9 | 2.6 | 1 | 14 | 2 | 1.0 |
| `resource-not-found` | persistent | `baseline` | 1 | 25 | 1 | 1.4 | 0.7 | 1 | 3 | 0 | - |
| `resource-not-found` | persistent | `bridge` | 1 | 25 | 0 | 0.7 | 1.1 | 0 | 5 | 0 | - |
| `resource-not-found` | transient | `baseline` | 1 | 25 | 1 | 1.4 | 0.9 | 1 | 5 | 0 | - |
| `resource-not-found` | transient | `bridge` | 1 | 25 | 1 | 0.5 | 0.5 | 0 | 1 | 1 | - |

## `baseline` vs each other arm

`separation` compares the gap between medians against the spread within each arm (pooled sd). Under ~1 the arms are indistinguishable at this sample size, whatever the millisecond difference looks like.

| fault | case | arm | median ms | vs base | ratio | pooled sd | separation |
|---|---|---|---:|---:|---:|---:|---:|
| `none` | control | `bridge` | 0 | -1 | 0.00x | 1.0 | 1.0 |
| `internal-error` | persistent | `bridge` | 2 | -1 | 0.67x | 4.1 | 0.2 |
| `internal-error` | transient | `bridge` | 2 | -1 | 0.67x | 2.3 | 0.4 |
| `throughput-exceeded` | persistent | `bridge` | 2 | -1 | 0.67x | 3.2 | 0.3 |
| `throughput-exceeded` | transient | `bridge` | 2 | -1 | 0.67x | 2.1 | 0.5 |
| `unavailable` | persistent | `bridge` | 2 | -1 | 0.67x | 5.7 | 0.2 |
| `unavailable` | transient | `bridge` | 2 | -1 | 0.67x | 2.3 | 0.4 |
| `empty-500` | persistent | `bridge` | 2 | -1 | 0.67x | 1.2 | 0.8 |
| `empty-500` | transient | `bridge` | 2 | -1 | 0.67x | 2.4 | 0.4 |
| `malformed-body` | persistent | `bridge` | 2 | -1 | 0.67x | 1.5 | 0.7 |
| `malformed-body` | transient | `bridge` | 2 | +0 | 1.00x | 2.1 | 0.0 |
| `resource-not-found` | persistent | `bridge` | 0 | -1 | 0.00x | 0.9 | 1.1 |
| `resource-not-found` | transient | `bridge` | 1 | +0 | 1.00x | 0.7 | 0.0 |
