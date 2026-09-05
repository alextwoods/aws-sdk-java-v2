# Cold-start summary — `20260904-2341`

Scenario `small-get`, 10 JVMs per (arm, client), 60 JVMs total. Arms: `baseline`, `bridge`.

One JVM per row of data. Medians, because a single outlier is a whole data point here.

## client build + first call (ms)

| client | baseline median | min–max | bridge median | min–max | paired vs baseline | wins |
|--------|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 693.2 | 660–729 | 757.0 | 712–833 | +10.1% | 0/10 |
| `v2-sync-stripped` | 713.4 | 678–772 | 722.9 | 688–740 | +0.4% | 4/10 |
| `smithy` | 600.3 | 573–615 | 596.9 | 574–632 | -1.0% | 5/10 |

## client construction (ms)

| client | baseline median | min–max | bridge median | min–max | paired vs baseline | wins |
|--------|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 507.4 | 481–524 | 602.9 | 580–641 | +22.3% | 0/10 |
| `v2-sync-stripped` | 512.6 | 485–558 | 606.8 | 570–626 | +18.5% | 0/10 |
| `smithy` | 511.8 | 495–536 | 508.9 | 489–556 | -0.2% | 6/10 |

## first call (ms)

| client | baseline median | min–max | bridge median | min–max | paired vs baseline | wins |
|--------|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 192.2 | 164–209 | 154.8 | 132–213 | -20.5% | 9/10 |
| `v2-sync-stripped` | 197.9 | 166–215 | 113.7 | 104–132 | -42.5% | 10/10 |
| `smithy` | 86.2 | 75–110 | 82.1 | 76–103 | -4.4% | 6/10 |

## jvm start to first response (ms)

| client | baseline median | min–max | bridge median | min–max | paired vs baseline | wins |
|--------|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 1,074.5 | 1,048–1,140 | 1,129.5 | 1,082–1,209 | +5.0% | 1/10 |
| `v2-sync-stripped` | 1,101.0 | 1,039–1,153 | 1,086.5 | 1,056–1,120 | -1.2% | 7/10 |
| `smithy` | 966.5 | 926–1,018 | 966.5 | 954–1,004 | +0.4% | 4/10 |

## jit compilation at exit (ms)

| client | baseline median | min–max | bridge median | min–max | paired vs baseline | wins |
|--------|----:|----:|----:|----:|----:|----:|
| `v2-sync` | 821.5 | 769–887 | 964.0 | 839–1,268 | +17.6% | 2/10 |
| `v2-sync-stripped` | 899.0 | 794–1,114 | 885.0 | 853–1,073 | -1.0% | 6/10 |
| `smithy` | 663.5 | 640–849 | 668.0 | 620–718 | -0.6% | 6/10 |

## Later calls in the same JVM (µs)

How fast calls 2..N were, as the JVM warms. Not steady state — these are the first
handful of operations ever executed, so they are still hundreds of times slower than the
numbers a timing collection reports.

| client | arm | median of call 2 | median of last call |
|--------|-----|----:|----:|
| `v2-sync` | `baseline` | 4,984 | 4,590 |
| `v2-sync` | `bridge` | 8,234 | 2,903 |
| `v2-sync-stripped` | `baseline` | 4,894 | 3,792 |
| `v2-sync-stripped` | `bridge` | 3,502 | 2,090 |
| `smithy` | `baseline` | 2,560 | 1,863 |
| `smithy` | `bridge` | 2,323 | 1,338 |

