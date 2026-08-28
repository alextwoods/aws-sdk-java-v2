# Allocation per operation by phase

Runs compared (first is the baseline):

- `baseline`: `pipeline_benchmark2/raw/phase0-baseline/20260827-1134`
- `phaseA`: `pipeline_benchmark2/raw/phaseA-final/20260828-0112`

asprof alloc `--total` bytes divided by 220,000 ops (warmup + measured). Client-code
bytes only: JIT, GC/VM and benchmark-harness stacks are excluded.

## Totals (bytes/op, client code)

| client | scenario | baseline | phaseA | delta |
|--------|----------|----:|----:|----:|
| v2-sync | small-get | 61,387 | 39,109 | -36.3% |
| v2-sync | small-put | 54,509 | 33,514 | -38.5% |
| v2-sync | batch-get | 533,097 | 514,464 | -3.5% |
| v2-sync | batch-put | 204,272 | 184,380 | -9.7% |
| v2-async | small-get | 69,702 | 49,240 | -29.4% |
| v2-async | small-put | 61,797 | 39,383 | -36.3% |
| v2-async | batch-get | 742,955 | 719,408 | -3.2% |
| v2-async | batch-put | 371,884 | 192,275 | -48.3% |

## By category (bytes/op)

### v2-sync / small-get

| category | baseline | phaseA | delta |
|----------|----:|----:|----:|
| pipeline-framework | 25,356 | 19,761 | -22.1% |
| signing | 20,905 | 5,050 | -75.8% |
| unmarshall | 7,142 | 7,004 | -1.9% |
| json | 3,334 | 3,229 | -3.1% |
| retry | 1,544 | 1,349 | -12.7% |
| endpoint-rules | 1,497 | 1,389 | -7.2% |
| marshall | 1,246 | 1,218 | -2.3% |
| crypto | 338 | 86 | -74.6% |
| http-client | 12 | 7 | -40.0% |
| other | 12 | 17 | +40.0% |

### v2-sync / small-put

| category | baseline | phaseA | delta |
|----------|----:|----:|----:|
| pipeline-framework | 24,775 | 19,882 | -19.7% |
| signing | 20,733 | 5,164 | -75.1% |
| json | 2,605 | 2,526 | -3.0% |
| marshall | 1,809 | 1,723 | -4.7% |
| retry | 1,561 | 1,420 | -9.0% |
| endpoint-rules | 1,423 | 1,406 | -1.2% |
| unmarshall | 1,282 | 1,268 | -1.1% |
| crypto | 303 | 102 | -66.1% |
| http-client | 12 | 14 | +20.0% |
| other | 7 | 7 | +0.0% |

### v2-sync / batch-get

| category | baseline | phaseA | delta |
|----------|----:|----:|----:|
| unmarshall | 368,004 | 370,256 | +0.6% |
| json | 69,983 | 70,593 | +0.9% |
| pipeline-framework | 67,826 | 62,621 | -7.7% |
| signing | 20,781 | 5,243 | -74.8% |
| endpoint-rules | 2,328 | 2,288 | -1.7% |
| marshall | 2,121 | 1,878 | -11.5% |
| retry | 1,713 | 1,475 | -13.9% |
| crypto | 322 | 98 | -69.6% |
| other | 14 | 7 | -50.0% |
| http-client | 5 | 5 | +0.0% |

### v2-sync / batch-put

| category | baseline | phaseA | delta |
|----------|----:|----:|----:|
| json | 117,297 | 117,188 | -0.1% |
| marshall | 35,952 | 36,919 | +2.7% |
| pipeline-framework | 24,546 | 19,334 | -21.2% |
| signing | 20,817 | 5,479 | -73.7% |
| endpoint-rules | 2,312 | 2,290 | -0.9% |
| unmarshall | 1,554 | 1,609 | +3.5% |
| retry | 1,501 | 1,432 | -4.6% |
| crypto | 276 | 105 | -62.1% |
| other | 12 | 17 | +40.0% |
| http-client | 5 | 7 | +50.0% |

### v2-async / small-get

| category | baseline | phaseA | delta |
|----------|----:|----:|----:|
| signing | 23,042 | 7,273 | -68.4% |
| pipeline-framework | 17,804 | 13,212 | -25.8% |
| unmarshall | 11,143 | 11,508 | +3.3% |
| http-client | 8,555 | 8,634 | +0.9% |
| json | 3,382 | 3,348 | -1.0% |
| retry | 1,897 | 1,740 | -8.3% |
| marshall | 1,847 | 1,780 | -3.6% |
| endpoint-rules | 1,406 | 1,394 | -0.8% |
| crypto | 324 | 81 | -75.0% |
| other | 300 | 269 | -10.3% |

### v2-async / small-put

| category | baseline | phaseA | delta |
|----------|----:|----:|----:|
| signing | 22,849 | 7,304 | -68.0% |
| pipeline-framework | 18,124 | 11,904 | -34.3% |
| http-client | 8,594 | 8,534 | -0.7% |
| unmarshall | 3,570 | 3,398 | -4.8% |
| marshall | 2,414 | 2,498 | +3.5% |
| json | 2,397 | 2,481 | +3.5% |
| retry | 1,871 | 1,547 | -17.3% |
| endpoint-rules | 1,342 | 1,339 | -0.2% |
| crypto | 357 | 117 | -67.3% |
| other | 279 | 262 | -6.0% |

### v2-async / batch-get

| category | baseline | phaseA | delta |
|----------|----:|----:|----:|
| unmarshall | 572,452 | 570,906 | -0.3% |
| json | 69,821 | 69,759 | -0.1% |
| pipeline-framework | 61,444 | 55,624 | -9.5% |
| signing | 22,835 | 7,300 | -68.0% |
| http-client | 8,801 | 8,798 | -0.0% |
| marshall | 2,500 | 2,481 | -0.8% |
| endpoint-rules | 2,335 | 2,383 | +2.0% |
| retry | 2,009 | 1,804 | -10.2% |
| other | 388 | 269 | -30.7% |
| crypto | 369 | 83 | -77.4% |

### v2-async / batch-put

| category | baseline | phaseA | delta |
|----------|----:|----:|----:|
| pipeline-framework | 176,701 | 12,685 | -92.8% |
| json | 117,478 | 118,012 | +0.5% |
| marshall | 37,763 | 37,553 | -0.6% |
| signing | 22,654 | 7,273 | -67.9% |
| http-client | 8,689 | 8,565 | -1.4% |
| unmarshall | 3,632 | 3,715 | +2.3% |
| endpoint-rules | 2,331 | 2,259 | -3.1% |
| retry | 1,988 | 1,749 | -12.0% |
| crypto | 353 | 133 | -62.2% |
| other | 296 | 329 | +11.3% |

## Top 12 allocation sites, baseline vs final phase (bytes/op)

### v2-sync / small-get

| site | baseline | phaseA | delta |
|------|----:|----:|----:|
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,113 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,020 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,579 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,402 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeMapCopier.lambda$copy$0` | 1,599 | 1,458 | -8.8% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,561 | 1,285 | -17.7% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,203 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 1,094 | 1,089 | -0.4% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,051 | 987 | -6.1% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 946 | 1,013 | +7.1% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 939 | 906 | -3.6% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 908 | 953 | +5.0% |

### v2-sync / small-put

| site | baseline | phaseA | delta |
|------|----:|----:|----:|
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,159 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,006 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,400 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,252 | 0 | -100.0% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,547 | 1,175 | -24.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,096 | 0 | -100.0% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,051 | 1,106 | +5.2% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption.<init>` | 958 | 887 | -7.5% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 922 | 1,008 | +9.3% |
| `software/amazon/awssdk/internal/http/LowCopyListMap$ForBuildable.<init>` | 910 | 851 | -6.5% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 903 | 922 | +2.1% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/CredentialScope.scope` | 903 | 0 | -100.0% |

### v2-sync / batch-get

| site | baseline | phaseA | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$2` | 119,132 | 119,530 | +0.3% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 84,472 | 85,108 | +0.8% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 74,663 | 74,067 | -0.8% |
| `software/amazon/awssdk/thirdparty/jackson/core/util/TextBuffer.setCurrentAndReturn` | 67,097 | 67,728 | +0.9% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 60,627 | 61,632 | +1.7% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.<init>` | 42,686 | 42,312 | -0.9% |
| `software/amazon/awssdk/services/dynamodb/model/MapAttributeValueCopier.lambda$copy$0` | 11,723 | 12,190 | +4.0% |
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$1` | 7,266 | 7,221 | -0.6% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,337 | 0 | -100.0% |
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,023 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/ListAttributeValueCopier.copy` | 2,593 | 2,741 | +5.7% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,474 | 0 | -100.0% |

### v2-sync / batch-put

| site | baseline | phaseA | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.write` | 114,812 | 114,478 | -0.3% |
| `software/amazon/awssdk/protocols/json/internal/marshall/JsonProtocolMarshaller.doMarshall` | 32,430 | 33,256 | +2.5% |
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,223 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,051 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,528 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,366 | 0 | -100.0% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,716 | 1,547 | -9.9% |
| `software/amazon/awssdk/protocols/json/internal/marshall/SimpleTypeJsonMarshaller$13.marshall` | 1,568 | 1,473 | -6.1% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,108 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 963 | 1,130 | +17.3% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 963 | 989 | +2.7% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption.<init>` | 922 | 963 | +4.4% |

### v2-async / small-get

| site | baseline | phaseA | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 4,509 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,209 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,386 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,264 | 0 | -100.0% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,243 | 2,173 | -3.1% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 2,226 | 2,033 | -8.7% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeMapCopier.lambda$copy$0` | 1,487 | 1,640 | +10.3% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,306 | 1,230 | -5.8% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,230 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 1,201 | 1,103 | -8.1% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,103 | 1,094 | -0.9% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 1,084 | 1,022 | -5.7% |

### v2-async / small-put

| site | baseline | phaseA | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 5,059 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,173 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,457 | 0 | -100.0% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,369 | 2,273 | -4.0% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 2,283 | 2,061 | -9.7% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,214 | 0 | -100.0% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,275 | 1,306 | +2.4% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,091 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CompletableFutureUtils.forwardExceptionTo` | 1,049 | 1,070 | +2.0% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,020 | 1,099 | +7.7% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 999 | 1,030 | +3.1% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 910 | 941 | +3.4% |

### v2-async / batch-get

| site | baseline | phaseA | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$2` | 120,238 | 119,840 | -0.3% |
| `software/amazon/awssdk/core/internal/http/async/AsyncResponseHandler$BaosSubscriber.onNext` | 89,908 | 91,064 | +1.3% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 85,332 | 85,137 | -0.2% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 74,530 | 74,103 | -0.6% |
| `software/amazon/awssdk/thirdparty/jackson/core/util/TextBuffer.setCurrentAndReturn` | 67,071 | 67,066 | -0.0% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 60,236 | 59,707 | -0.9% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.<init>` | 42,863 | 43,323 | +1.1% |
| `software/amazon/awssdk/crt/http/HttpStreamResponseHandlerNativeAdapter.onResponseBody` | 37,453 | 35,933 | -4.1% |
| `software/amazon/awssdk/utils/BinaryUtils.copyBytesFrom` | 36,729 | 36,593 | -0.4% |
| `software/amazon/awssdk/core/internal/http/async/AsyncResponseHandler.lambda$prepare$0` | 36,293 | 36,462 | +0.5% |
| `software/amazon/awssdk/services/dynamodb/model/MapAttributeValueCopier.lambda$copy$0` | 12,047 | 12,137 | +0.8% |
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$1` | 7,178 | 6,885 | -4.1% |

### v2-async / batch-put

| site | baseline | phaseA | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 163,713 | 0 | -100.0% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.write` | 115,183 | 115,465 | +0.2% |
| `software/amazon/awssdk/protocols/json/internal/marshall/JsonProtocolMarshaller.doMarshall` | 33,342 | 33,216 | -0.4% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,159 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,414 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,378 | 0 | -100.0% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,207 | 2,259 | +2.4% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 1,916 | 2,147 | +12.1% |
| `software/amazon/awssdk/protocols/json/internal/marshall/SimpleTypeJsonMarshaller$13.marshall` | 1,621 | 1,628 | +0.4% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,263 | 1,370 | +8.5% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 1,130 | 1,060 | -6.1% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 1,032 | 1,058 | +2.5% |

