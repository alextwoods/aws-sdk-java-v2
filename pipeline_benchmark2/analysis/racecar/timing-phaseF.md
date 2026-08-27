# Phase comparison

Runs compared (first is the baseline):

- `baseline`: `pipeline_benchmark2/raw/phase0-baseline/20260827-1134`
- `signerF`: `pipeline_benchmark2/raw/phaseF-signer/20260827-1223`

Deltas are vs the baseline. Positive % always means *better* (more throughput, less
time). The `spread` column is the rep-to-rep spread of the baseline as a noise
reference: treat deltas smaller than it as inconclusive.

## v2-async

### user-CPU ops/s

| scenario | baseline | signerF | delta | baseline spread |
|----------|----:|----:|----:|----:|
| small-get | 19,557.7 | 16,287.5 | -16.7% | 7.9% |
| small-put | 18,792.4 | 19,565.3 | +4.1% | 31.5% |
| batch-get | 5,727.2 | 5,366.8 | -6.3% | 6.2% |
| batch-put | 5,123.2 | 4,865.9 | -5.0% | 13.4% |

### total-CPU ops/s

| scenario | baseline | signerF | delta | baseline spread |
|----------|----:|----:|----:|----:|
| small-get | 9,159.1 | 7,755.3 | -15.3% | 8.0% |
| small-put | 8,936.5 | 8,858.5 | -0.9% | 22.1% |
| batch-get | 3,624.2 | 3,377.6 | -6.8% | 7.3% |
| batch-put | 3,621.3 | 3,414.4 | -5.7% | 10.3% |

### wall ops/s

| scenario | baseline | signerF | delta | baseline spread |
|----------|----:|----:|----:|----:|
| small-get | 6,753.8 | 5,318.0 | -21.3% | 8.0% |
| small-put | 6,680.5 | 6,127.5 | -8.3% | 15.4% |
| batch-get | 2,713.8 | 2,542.5 | -6.3% | 9.7% |
| batch-put | 2,753.5 | 2,542.4 | -7.7% | 8.1% |

### avg us/op

| scenario | baseline | signerF | delta | baseline spread |
|----------|----:|----:|----:|----:|
| small-get | 148.3 | 215.2 | -45.1% | 8.1% |
| small-put | 150.5 | 166.4 | -10.6% | 16.1% |
| batch-get | 369.1 | 395.1 | -7.1% | 9.6% |
| batch-put | 363.7 | 393.8 | -8.3% | 8.3% |

## v2-sync

### user-CPU ops/s

| scenario | baseline | signerF | delta | baseline spread |
|----------|----:|----:|----:|----:|
| small-get | 23,654.9 | 20,855.7 | -11.8% | 7.9% |
| small-put | 24,386.4 | 26,346.6 | +8.0% | 26.3% |
| batch-get | 5,813.4 | 6,035.5 | +3.8% | 28.2% |
| batch-put | 4,789.6 | 4,328.7 | -9.6% | 11.2% |

### total-CPU ops/s

| scenario | baseline | signerF | delta | baseline spread |
|----------|----:|----:|----:|----:|
| small-get | 12,817.2 | 10,742.2 | -16.2% | 4.2% |
| small-put | 12,945.7 | 13,200.1 | +2.0% | 23.6% |
| batch-get | 4,532.4 | 4,650.0 | +2.6% | 29.4% |
| batch-put | 3,304.6 | 3,035.4 | -8.1% | 11.3% |

### wall ops/s

| scenario | baseline | signerF | delta | baseline spread |
|----------|----:|----:|----:|----:|
| small-get | 9,212.9 | 6,697.9 | -27.3% | 11.3% |
| small-put | 9,075.0 | 8,999.7 | -0.8% | 22.3% |
| batch-get | 3,625.7 | 3,706.0 | +2.2% | 30.5% |
| batch-put | 2,644.0 | 2,422.4 | -8.4% | 14.0% |

### avg us/op

| scenario | baseline | signerF | delta | baseline spread |
|----------|----:|----:|----:|----:|
| small-get | 108.8 | 157.5 | -44.7% | 11.7% |
| small-put | 111.3 | 111.2 | +0.1% | 23.5% |
| batch-get | 282.3 | 270.2 | +4.3% | 33.9% |
| batch-put | 379.6 | 413.7 | -9.0% | 14.3% |

