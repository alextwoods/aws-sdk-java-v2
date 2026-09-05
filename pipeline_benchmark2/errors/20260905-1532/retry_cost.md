# Retry timing — `20260905-1532`

Backoff mode: **`immediate`**. Arms: `baseline`, `bridge`. Times in **microseconds**.

Delay is zero, so wall time is the cost of *retrying* — no sleep in it.

Warm 1-attempt control (the floor every row below stands on): `baseline` 1110 us, `bridge` 669 us

| fault | case | arm | attempts | n | median us | mean us | sd | min | max | over control | per extra attempt |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `none` | control | `baseline` | 1 | 60 | 1110 | 1672.6 | 1621.4 | 636 | 8276 | 0 | - |
| `none` | control | `bridge` | 1 | 60 | 669 | 977.5 | 1218.9 | 342 | 8452 | 0 | - |
| `internal-error` | persistent | `baseline` | 3 | 60 | 2416 | 3271.3 | 2865.7 | 1471 | 21491 | 1306 | 652.8 |
| `internal-error` | persistent | `bridge` | 3 | 60 | 1943 | 2620.1 | 3126.2 | 1091 | 24403 | 1274 | 637.0 |
| `internal-error` | transient | `baseline` | 3 | 60 | 2486 | 3115.6 | 1972.8 | 1427 | 9818 | 1376 | 687.8 |
| `internal-error` | transient | `bridge` | 3 | 60 | 1944 | 2209.5 | 1497.7 | 1036 | 10249 | 1274 | 637.2 |
| `throughput-exceeded` | persistent | `baseline` | 3 | 60 | 2514 | 3358.4 | 2760.2 | 1433 | 15147 | 1404 | 702.2 |
| `throughput-exceeded` | persistent | `bridge` | 3 | 60 | 1963 | 2285.1 | 1809.4 | 1054 | 12504 | 1294 | 647.0 |
| `throughput-exceeded` | transient | `baseline` | 3 | 60 | 2465 | 2902.3 | 1677.1 | 1384 | 10651 | 1355 | 677.5 |
| `throughput-exceeded` | transient | `bridge` | 3 | 60 | 1774 | 2453.3 | 2544.7 | 987 | 17405 | 1104 | 552.2 |
| `unavailable` | persistent | `baseline` | 3 | 60 | 2550 | 3178.2 | 1890.6 | 1585 | 11616 | 1440 | 720.2 |
| `unavailable` | persistent | `bridge` | 3 | 60 | 1828 | 2748.7 | 5485.1 | 1049 | 44056 | 1159 | 579.5 |
| `unavailable` | transient | `baseline` | 3 | 60 | 2456 | 2763.1 | 1207.3 | 1444 | 7282 | 1346 | 673.0 |
| `unavailable` | transient | `bridge` | 3 | 60 | 1780 | 2173.0 | 1442.2 | 972 | 9443 | 1111 | 555.5 |
| `empty-500` | persistent | `baseline` | 3 | 60 | 2354 | 2690.6 | 1356.6 | 1298 | 9239 | 1244 | 622.0 |
| `empty-500` | persistent | `bridge` | 3 | 60 | 1822 | 2289.4 | 1770.0 | 1049 | 12409 | 1154 | 576.8 |
| `empty-500` | transient | `baseline` | 3 | 60 | 2302 | 2951.7 | 2654.9 | 1322 | 19170 | 1192 | 596.0 |
| `empty-500` | transient | `bridge` | 3 | 60 | 1759 | 2072.0 | 1359.8 | 982 | 9670 | 1090 | 545.0 |
| `malformed-body` | persistent | `baseline` | 3 | 60 | 2486 | 3185.9 | 2553.1 | 1513 | 18093 | 1376 | 688.2 |
| `malformed-body` | persistent | `bridge` | 3 | 60 | 1984 | 2316.0 | 1484.1 | 1069 | 11460 | 1315 | 657.5 |
| `malformed-body` | transient | `baseline` | 3 | 60 | 2253 | 2897.7 | 2258.7 | 1402 | 16771 | 1143 | 571.5 |
| `malformed-body` | transient | `bridge` | 3 | 60 | 1710 | 2113.1 | 1585.8 | 1019 | 12161 | 1040 | 520.2 |
| `resource-not-found` | persistent | `baseline` | 1 | 60 | 1176 | 1348.8 | 607.2 | 721 | 4015 | 66 | - |
| `resource-not-found` | persistent | `bridge` | 1 | 60 | 764 | 858.8 | 407.3 | 423 | 3135 | 96 | - |
| `resource-not-found` | transient | `baseline` | 1 | 60 | 1179 | 1286.2 | 512.7 | 721 | 3344 | 69 | - |
| `resource-not-found` | transient | `bridge` | 1 | 60 | 764 | 797.5 | 260.0 | 426 | 1534 | 94 | - |

## `baseline` vs each other arm

`P(faster)` is the chance a random sample from the arm beats a random `baseline` sample; 0.5 is indistinguishable, 1.0 is total separation. `95% CI` is a bootstrap interval on the difference of medians — it excludes 0 exactly when the difference is real at this sample size. Both are rank-based, because a handful of GC outliers make the standard deviation useless here while barely moving the median.

| fault | case | arm | median us | vs base | ratio | P(faster) | 95% CI |
|---|---|---|---:|---:|---:|---:|---:|
| `none` | control | `bridge` | 669 | -441 | 0.60x | 0.80 | [-594, -296] |
| `internal-error` | persistent | `bridge` | 1943 | -472 | 0.80x | 0.69 | [-805, -150] |
| `internal-error` | transient | `bridge` | 1944 | -542 | 0.78x | 0.70 | [-928, -202] |
| `throughput-exceeded` | persistent | `bridge` | 1963 | -552 | 0.78x | 0.72 | [-1102, -248] |
| `throughput-exceeded` | transient | `bridge` | 1774 | -692 | 0.72x | 0.71 | [-1086, -288] |
| `unavailable` | persistent | `bridge` | 1828 | -722 | 0.72x | 0.75 | [-1142, -468] |
| `unavailable` | transient | `bridge` | 1780 | -676 | 0.72x | 0.72 | [-974, -299] |
| `empty-500` | persistent | `bridge` | 1822 | -532 | 0.77x | 0.68 | [-913, -202] |
| `empty-500` | transient | `bridge` | 1759 | -543 | 0.76x | 0.69 | [-926, -171] |
| `malformed-body` | persistent | `bridge` | 1984 | -502 | 0.80x | 0.69 | [-972, -189] |
| `malformed-body` | transient | `bridge` | 1710 | -544 | 0.76x | 0.71 | [-952, -220] |
| `resource-not-found` | persistent | `bridge` | 764 | -412 | 0.65x | 0.82 | [-542, -238] |
| `resource-not-found` | transient | `bridge` | 764 | -416 | 0.65x | 0.85 | [-527, -283] |

`bridge` has the lower median in 13 of 13 cases, and the difference clears the bootstrap interval in 13 of 13. One direction in every case with every interval clear: if the arms were equivalent each case would be a coin flip, so no single row has to carry this.
