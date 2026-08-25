### small-get — per-op phase timings (avg us, SDK-native metrics)

| phase | v1 | v2-sync | v2-async | smithy |
|-------|----:|----:|----:|----:|
| total call | 81.30 | 76.89 | 109.43 | 64.56 |
| marshall | 0.19 | 0.86 | 1.09 | 0.49 |
| sign | 3.99 | 3.75 | 4.46 | 2.17 |
| endpoint resolve | - | 0.83 | 0.87 | 0.42 |
| credentials | 0.00 | 0.02 | 0.04 | 0.01 |
| http transport | 64.37 | 64.30 | 99.59 | 59.17* |
| unmarshall | 6.90 | 3.68 | 3.57 | 2.31 |
| unattributed (total - phases) | 5.85 | 4.31 | 0.90 | -0.01 |
| total incl marshall (comparable) | 81.30 | 77.75 | 110.52 | 64.56 |

### small-put — per-op phase timings (avg us, SDK-native metrics)

| phase | v1 | v2-sync | v2-async | smithy |
|-------|----:|----:|----:|----:|
| total call | 77.07 | 75.21 | 110.89 | 64.34 |
| marshall | 2.73 | 3.14 | 3.89 | 1.59 |
| sign | 4.13 | 3.87 | 4.97 | 2.32 |
| endpoint resolve | - | 0.80 | 0.96 | 0.42 |
| credentials | 0.00 | 0.02 | 0.04 | 0.01 |
| http transport | 64.03 | 65.91 | 99.37 | 59.65* |
| unmarshall | 0.04 | 0.71 | 0.66 | 0.33 |
| unattributed (total - phases) | 6.14 | 3.90 | 4.89 | 0.02 |
| total incl marshall (comparable) | 77.07 | 78.35 | 114.78 | 64.34 |

### batch-get — per-op phase timings (avg us, SDK-native metrics)

| phase | v1 | v2-sync | v2-async | smithy |
|-------|----:|----:|----:|----:|
| total call | 465.76 | 216.02 | 315.59 | 171.80 |
| marshall | 4.29 | 4.53 | 5.67 | 2.30 |
| sign | 4.24 | 4.08 | 5.18 | 3.00 |
| endpoint resolve | - | 1.11 | 1.44 | 0.63 |
| credentials | 0.00 | 0.02 | 0.05 | 0.01 |
| http transport | 139.00 | 78.93 | 302.31 | 78.53* |
| unmarshall | 312.25 | 121.10 | 108.64 | 87.45 |
| unattributed (total - phases) | 5.98 | 10.78 | -102.03 | -0.12 |
| total incl marshall (comparable) | 465.76 | 220.55 | 321.26 | 171.80 |

### batch-put — per-op phase timings (avg us, SDK-native metrics)

| phase | v1 | v2-sync | v2-async | smithy |
|-------|----:|----:|----:|----:|
| total call | 329.50 | 177.69 | 191.05 | 157.31 |
| marshall | 153.01 | 150.38 | 112.92 | 53.73 |
| sign | 17.17 | 16.89 | 17.02 | 14.33 |
| endpoint resolve | - | 1.15 | 1.16 | 0.47 |
| credentials | 0.00 | 0.02 | 0.05 | 0.01 |
| http transport | 153.61 | 154.81 | 162.96 | 88.35* |
| unmarshall | 0.08 | 0.68 | 0.66 | 0.44 |
| unattributed (total - phases) | 5.63 | 4.14 | 9.20 | -0.02 |
| total incl marshall (comparable) | 329.50 | 328.07 | 303.97 | 157.31 |

\* smithy has no transport metric; derived as attempt - sign - deserialize (serialization is outside attempt_duration).

Note: V2 ApiCallDuration excludes marshalling (measured before the pipeline timer starts),
contrary to the CoreMetric javadoc; the last row adds it back for cross-SDK comparison.

