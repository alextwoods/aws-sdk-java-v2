## CPU profile: % of client-code samples by category

(JIT/GC/VM threads and benchmark harness excluded from the base; their share of all
samples is shown in the bottom rows. Whole-process asprof CPU samples, includes warmup.)

### small-get

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| socket-syscall | 57.7% | 58.7% | 62.0% | 72.6% |
| http-client | 18.4% | 4.4% | 5.6% | 13.5% |
| pipeline-framework | 0.1% | 17.8% | 4.0% | 1.6% |
| signing | 7.6% | 6.7% | 2.7% | 2.5% |
| unmarshall | 7.8% | 3.8% | 4.0% | 1.7% |
| thread-sync | 0.1% | 0.1% | 9.6% | 0.3% |
| json | 4.0% | 3.0% | 2.0% | 5.1% |
| other | 0.3% | 0.7% | 7.2% | 0.3% |
| retry | 2.5% | 1.0% | 0.9% | 0.5% |
| endpoint-rules | 0.0% | 1.6% | 0.7% | 1.4% |
| marshall | 0.5% | 1.5% | 1.1% | 0.1% |
| crypto | 0.9% | 0.7% | 0.1% | 0.4% |
| _jit-compiler (% of all samples)_ | 24.8% | 31.6% | 26.4% | 22.3% |
| _gc-vm (% of all samples)_ | 1.1% | 1.4% | 0.2% | 0.8% |
| _client-code samples_ | 1,484 | 1,515 | 2,301 | 1,063 |

### small-put

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| socket-syscall | 61.7% | 58.6% | 57.9% | 73.6% |
| http-client | 18.3% | 5.5% | 6.2% | 13.8% |
| pipeline-framework | 0.1% | 17.5% | 4.5% | 1.9% |
| signing | 7.5% | 7.2% | 4.3% | 3.4% |
| thread-sync | 0.2% | 0.2% | 13.8% | 0.2% |
| other | 0.4% | 0.5% | 7.8% | 0.2% |
| json | 3.4% | 2.3% | 0.8% | 3.1% |
| marshall | 3.4% | 2.2% | 1.3% | 0.5% |
| retry | 3.5% | 1.2% | 0.5% | 0.8% |
| unmarshall | 0.7% | 1.8% | 1.7% | 0.3% |
| endpoint-rules | 0.0% | 1.9% | 0.7% | 1.5% |
| crypto | 0.8% | 1.1% | 0.5% | 0.6% |
| _jit-compiler (% of all samples)_ | 28.8% | 31.7% | 20.2% | 24.2% |
| _gc-vm (% of all samples)_ | 1.2% | 1.9% | 0.4% | 0.3% |
| _client-code samples_ | 1,381 | 1,624 | 2,594 | 1,091 |

### batch-get

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| unmarshall | 58.1% | 30.0% | 10.9% | 9.8% |
| socket-syscall | 17.9% | 24.1% | 52.8% | 34.6% |
| json | 17.3% | 26.9% | 9.0% | 47.9% |
| http-client | 3.8% | 2.2% | 2.3% | 4.7% |
| pipeline-framework | 0.0% | 10.5% | 4.1% | 0.6% |
| thread-sync | 0.0% | 0.1% | 11.7% | 0.1% |
| signing | 1.1% | 2.5% | 3.4% | 0.8% |
| marshall | 0.7% | 2.4% | 1.1% | 0.2% |
| other | 0.1% | 0.2% | 3.0% | 0.2% |
| retry | 0.7% | 0.3% | 0.5% | 0.3% |
| endpoint-rules | 0.0% | 0.7% | 0.8% | 0.4% |
| crypto | 0.2% | 0.2% | 0.3% | 0.6% |
| _jit-compiler (% of all samples)_ | 4.8% | 14.6% | 9.2% | 9.5% |
| _gc-vm (% of all samples)_ | 1.0% | 1.8% | 1.1% | 1.0% |
| _client-code samples_ | 9,634 | 4,546 | 6,272 | 3,209 |

### batch-put

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| marshall | 30.7% | 39.4% | 42.1% | 3.1% |
| socket-syscall | 34.4% | 32.2% | 24.5% | 40.2% |
| json | 22.8% | 13.3% | 21.6% | 38.4% |
| crypto | 4.3% | 4.5% | 2.0% | 9.9% |
| http-client | 5.0% | 1.7% | 1.6% | 5.5% |
| pipeline-framework | 0.0% | 5.3% | 2.2% | 0.9% |
| signing | 1.6% | 2.0% | 0.9% | 1.0% |
| thread-sync | 0.1% | 0.1% | 2.4% | 0.1% |
| other | 0.1% | 0.1% | 1.8% | 0.1% |
| unmarshall | 0.2% | 0.5% | 0.5% | 0.1% |
| retry | 0.8% | 0.1% | 0.1% | 0.3% |
| endpoint-rules | 0.0% | 0.6% | 0.4% | 0.3% |
| _jit-compiler (% of all samples)_ | 7.4% | 9.9% | 10.0% | 9.5% |
| _gc-vm (% of all samples)_ | 0.9% | 0.6% | 0.5% | 0.4% |
| _client-code samples_ | 6,345 | 6,466 | 5,709 | 3,278 |


## Allocation profile: bytes per operation by category

(asprof alloc, --total bytes; divided by 220,000 ops = warmup + measured. Includes
one-time setup allocations, which are negligible at this op count.)

### small-get

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| signing | 14,949 | 20,569 | 22,628 | 1,218 |
| pipeline-framework | 160 | 25,271 | 17,993 | 1,289 |
| http-client | 14,844 | 2 | 8,539 | 1,728 |
| unmarshall | 3,491 | 7,183 | 11,334 | 2,057 |
| json | 3,487 | 3,446 | 3,265 | 3,208 |
| retry | 2,412 | 1,578 | 1,959 | 64 |
| marshall | 715 | 1,237 | 1,856 | 2 |
| endpoint-rules | 0 | 1,337 | 1,356 | 393 |
| crypto | 226 | 329 | 310 | 86 |
| other | 5 | 12 | 269 | 17 |
| **total (client code)** | **40,289** | **60,963** | **69,509** | **10,062** |

### small-put

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| signing | 14,792 | 20,960 | 23,038 | 1,330 |
| pipeline-framework | 138 | 24,556 | 17,404 | 1,349 |
| http-client | 14,899 | 21 | 8,436 | 1,313 |
| json | 2,829 | 2,433 | 2,304 | 2,257 |
| retry | 2,602 | 1,504 | 1,911 | 88 |
| unmarshall | 496 | 1,192 | 3,448 | 62 |
| marshall | 720 | 1,775 | 2,509 | 2 |
| endpoint-rules | 0 | 1,451 | 1,358 | 350 |
| crypto | 193 | 353 | 357 | 100 |
| other | 7 | 14 | 236 | 12 |
| **total (client code)** | **36,676** | **54,259** | **61,003** | **6,863** |

### batch-get

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| unmarshall | 158,323 | 368,988 | 573,067 | 107,326 |
| json | 70,867 | 70,443 | 69,814 | 70,726 |
| pipeline-framework | 169 | 67,249 | 61,211 | 1,375 |
| http-client | 14,852 | 0 | 8,722 | 37,925 |
| signing | 14,928 | 21,217 | 23,269 | 1,313 |
| marshall | 2,266 | 1,883 | 2,557 | 2 |
| retry | 2,467 | 1,394 | 1,856 | 102 |
| endpoint-rules | 0 | 2,381 | 2,364 | 648 |
| crypto | 176 | 334 | 336 | 114 |
| other | 12 | 12 | 384 | 10 |
| **total (client code)** | **264,060** | **533,901** | **743,580** | **219,543** |

### batch-put

| category | v1 | v2-sync | v2-async | smithy |
|----------|----:|----:|----:|----:|
| json | 154,124 | 118,110 | 118,127 | 116,654 |
| pipeline-framework | 186 | 25,018 | 176,494 | 1,277 |
| marshall | 2,240 | 35,902 | 37,432 | 5 |
| signing | 15,109 | 20,538 | 22,885 | 1,425 |
| http-client | 15,111 | 2 | 8,627 | 1,294 |
| unmarshall | 622 | 1,501 | 3,877 | 236 |
| retry | 2,452 | 1,506 | 2,023 | 169 |
| endpoint-rules | 0 | 2,407 | 2,340 | 572 |
| crypto | 226 | 300 | 348 | 62 |
| other | 12 | 7 | 303 | 14 |
| **total (client code)** | **190,083** | **205,292** | **372,456** | **121,708** |

