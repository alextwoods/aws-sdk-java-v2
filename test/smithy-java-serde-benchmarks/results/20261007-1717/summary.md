
### Whole call, µs/op (lower is better)

| case | v2 | smithy | smithy-native | smithy vs v2 | native vs v2 |
|---|---:|---:|---:|---:|---:|
| `smallGet` | 36.344 | 35.511 | 32.116 | -2.3% | -11.6% |
| `smallPut` | 36.042 | 31.692 | 30.004 | -12.1% | -16.8% |
| `batchGet` | 432.444 | 428.097 | 359.361 | -1.0% | -16.9% |
| `batchPut` | 552.870 | 346.697 | 262.717 | -37.3% | -52.5% |

### Whole call, allocated bytes per op

| case | v2 | smithy | smithy-native | smithy vs v2 | native vs v2 |
|---|---:|---:|---:|---:|---:|
| `smallGet` | 47,603 | 51,688 | 49,067 | +8.6% | +3.1% |
| `smallPut` | 40,867 | 44,643 | 42,718 | +9.2% | +4.5% |
| `batchGet` | 521,867 | 648,400 | 662,781 | +24.2% | +27.0% |
| `batchPut` | 197,633 | 206,736 | 207,972 | +4.6% | +5.2% |

### Serde only, µs/op

| case | v2 | smithy | smithy-native | smithy vs v2 | native vs v2 |
|---|---:|---:|---:|---:|---:|
| `marshallSmallGet` | 1.363 | 0.909 | 0.540 | -33.3% | -60.4% |
| `marshallSmallPut` | 6.250 | 3.169 | 2.116 | -49.3% | -66.1% |
| `marshallBatchGet` | 9.794 | 4.617 | 3.038 | -52.9% | -69.0% |
| `marshallBatchPut` | 374.999 | 173.212 | 117.440 | -53.8% | -68.7% |
| `unmarshallSmallGet` | 13.421 | 6.070 | 5.103 | -54.8% | -62.0% |
| `unmarshallSmallPut` | 0.724 | 1.355 | 1.017 | +87.2% | +40.5% |
| `unmarshallBatchGet` | 891.482 | 339.661 | 302.348 | -61.9% | -66.1% |
| `unmarshallBatchPut` | 1.254 | 1.310 | 0.931 | +4.5% | -25.8% |

### Serde only, allocated bytes per op

| case | v2 | smithy | smithy-native | smithy vs v2 | native vs v2 |
|---|---:|---:|---:|---:|---:|
| `marshallSmallGet` | 3,136 | 2,277 | 1,355 | -27.4% | -56.8% |
| `marshallSmallPut` | 3,867 | 3,720 | 2,120 | -3.8% | -45.2% |
| `marshallBatchGet` | 5,483 | 3,824 | 2,853 | -30.3% | -48.0% |
| `marshallBatchPut` | 157,354 | 164,371 | 165,784 | +4.5% | +5.4% |
| `unmarshallSmallGet` | 15,245 | 13,960 | 12,659 | -8.4% | -17.0% |
| `unmarshallSmallPut` | 1,952 | 5,763 | 5,168 | +195.2% | +164.8% |
| `unmarshallBatchGet` | 835,294 | 563,960 | 578,797 | -32.5% | -30.7% |
| `unmarshallBatchPut` | 2,589 | 5,952 | 5,411 | +129.9% | +109.0% |
