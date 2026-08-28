# Allocation per operation by phase

Runs compared (first is the baseline):

- `baseline`: `pipeline_benchmark2/raw/phase0-baseline/20260827-1134`
- `phaseD`: `pipeline_benchmark2/raw/phaseD-framework/20260828-0729`

asprof alloc `--total` bytes divided by 220,000 ops (warmup + measured). Client-code
bytes only: JIT, GC/VM and benchmark-harness stacks are excluded.

## Totals (bytes/op, client code)

| client | scenario | baseline | phaseD | delta |
|--------|----------|----:|----:|----:|
| v2-sync | small-get | 61,387 | 38,995 | -36.5% |
| v2-sync | small-put | 54,509 | 31,472 | -42.3% |
| v2-sync | batch-get | 533,097 | 509,926 | -4.3% |
| v2-sync | batch-put | 204,272 | 104,150 | -49.0% |
| v2-async | small-get | 69,702 | 47,877 | -31.3% |
| v2-async | small-put | 61,797 | 39,128 | -36.7% |
| v2-async | batch-get | 742,955 | 699,732 | -5.8% |
| v2-async | batch-put | 371,884 | 111,664 | -70.0% |

## By category (bytes/op)

### v2-sync / small-get

| category | baseline | phaseD | delta |
|----------|----:|----:|----:|
| pipeline-framework | 25,356 | 19,001 | -25.1% |
| signing | 20,905 | 5,124 | -75.5% |
| unmarshall | 7,142 | 7,502 | +5.0% |
| json | 3,334 | 3,210 | -3.7% |
| retry | 1,544 | 1,444 | -6.5% |
| endpoint-rules | 1,497 | 1,323 | -11.6% |
| marshall | 1,246 | 1,285 | +3.1% |
| crypto | 338 | 93 | -72.5% |
| other | 12 | 14 | +20.0% |
| http-client | 12 | 0 | -100.0% |

### v2-sync / small-put

| category | baseline | phaseD | delta |
|----------|----:|----:|----:|
| pipeline-framework | 24,775 | 17,964 | -27.5% |
| signing | 20,733 | 5,362 | -74.1% |
| json | 2,605 | 2,490 | -4.4% |
| marshall | 1,809 | 1,699 | -6.1% |
| retry | 1,561 | 1,270 | -18.6% |
| endpoint-rules | 1,423 | 1,351 | -5.0% |
| unmarshall | 1,282 | 1,218 | -5.0% |
| crypto | 303 | 98 | -67.7% |
| http-client | 12 | 10 | -20.0% |
| other | 7 | 10 | +33.3% |

### v2-sync / batch-get

| category | baseline | phaseD | delta |
|----------|----:|----:|----:|
| unmarshall | 368,004 | 368,200 | +0.1% |
| json | 69,983 | 70,407 | +0.6% |
| pipeline-framework | 67,826 | 60,450 | -10.9% |
| signing | 20,781 | 5,331 | -74.3% |
| endpoint-rules | 2,328 | 2,281 | -2.0% |
| marshall | 2,121 | 1,911 | -9.9% |
| retry | 1,713 | 1,237 | -27.8% |
| crypto | 322 | 91 | -71.9% |
| other | 14 | 12 | -16.7% |
| http-client | 5 | 7 | +50.0% |

### v2-sync / batch-put

| category | baseline | phaseD | delta |
|----------|----:|----:|----:|
| json | 117,297 | 38,633 | -67.1% |
| marshall | 35,952 | 36,636 | +1.9% |
| pipeline-framework | 24,546 | 18,264 | -25.6% |
| signing | 20,817 | 5,355 | -74.3% |
| endpoint-rules | 2,312 | 2,347 | +1.5% |
| unmarshall | 1,554 | 1,506 | -3.1% |
| retry | 1,501 | 1,270 | -15.4% |
| crypto | 276 | 107 | -61.2% |
| other | 12 | 17 | +40.0% |
| http-client | 5 | 14 | +200.0% |

### v2-async / small-get

| category | baseline | phaseD | delta |
|----------|----:|----:|----:|
| signing | 23,042 | 7,385 | -67.9% |
| pipeline-framework | 17,804 | 12,287 | -31.0% |
| unmarshall | 11,143 | 11,401 | +2.3% |
| http-client | 8,555 | 8,229 | -3.8% |
| json | 3,382 | 3,272 | -3.2% |
| retry | 1,897 | 1,652 | -12.9% |
| marshall | 1,847 | 1,809 | -2.1% |
| endpoint-rules | 1,406 | 1,416 | +0.7% |
| crypto | 324 | 86 | -73.5% |
| other | 300 | 341 | +13.5% |

### v2-async / small-put

| category | baseline | phaseD | delta |
|----------|----:|----:|----:|
| signing | 22,849 | 7,454 | -67.4% |
| pipeline-framework | 18,124 | 11,301 | -37.6% |
| http-client | 8,594 | 8,698 | +1.2% |
| unmarshall | 3,570 | 3,520 | -1.4% |
| marshall | 2,414 | 2,273 | -5.8% |
| json | 2,397 | 2,438 | +1.7% |
| retry | 1,871 | 1,785 | -4.6% |
| endpoint-rules | 1,342 | 1,263 | -5.9% |
| crypto | 357 | 119 | -66.7% |
| other | 279 | 276 | -0.9% |

### v2-async / batch-get

| category | baseline | phaseD | delta |
|----------|----:|----:|----:|
| unmarshall | 572,452 | 552,127 | -3.6% |
| json | 69,821 | 69,759 | -0.1% |
| pipeline-framework | 61,444 | 54,919 | -10.6% |
| signing | 22,835 | 7,240 | -68.3% |
| http-client | 8,801 | 8,775 | -0.3% |
| marshall | 2,500 | 2,443 | -2.3% |
| endpoint-rules | 2,335 | 2,302 | -1.4% |
| retry | 2,009 | 1,737 | -13.5% |
| other | 388 | 338 | -12.9% |
| crypto | 369 | 93 | -74.8% |

### v2-async / batch-put

| category | baseline | phaseD | delta |
|----------|----:|----:|----:|
| pipeline-framework | 176,701 | 12,118 | -93.1% |
| json | 117,478 | 38,468 | -67.3% |
| marshall | 37,763 | 36,915 | -2.2% |
| signing | 22,654 | 7,428 | -67.2% |
| http-client | 8,689 | 8,708 | +0.2% |
| unmarshall | 3,632 | 3,775 | +3.9% |
| endpoint-rules | 2,331 | 2,247 | -3.6% |
| retry | 1,988 | 1,618 | -18.6% |
| crypto | 353 | 105 | -70.3% |
| other | 296 | 281 | -4.8% |

## Top 12 allocation sites, baseline vs final phase (bytes/op)

### v2-sync / small-get

| site | baseline | phaseD | delta |
|------|----:|----:|----:|
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,113 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,020 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,579 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,402 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeMapCopier.lambda$copy$0` | 1,599 | 1,497 | -6.4% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,561 | 245 | -84.3% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,203 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 1,094 | 1,194 | +9.2% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,051 | 1,060 | +0.9% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 946 | 1,065 | +12.6% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 939 | 925 | -1.5% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 908 | 1,006 | +10.8% |

### v2-sync / small-put

| site | baseline | phaseD | delta |
|------|----:|----:|----:|
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,159 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,006 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,400 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,252 | 0 | -100.0% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,547 | 231 | -85.1% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,096 | 0 | -100.0% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,051 | 1,072 | +2.0% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption.<init>` | 958 | 858 | -10.4% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 922 | 1,013 | +9.8% |
| `software/amazon/awssdk/internal/http/LowCopyListMap$ForBuildable.<init>` | 910 | 777 | -14.7% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 903 | 891 | -1.3% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/CredentialScope.scope` | 903 | 0 | -100.0% |

### v2-sync / batch-get

| site | baseline | phaseD | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$2` | 119,132 | 118,806 | -0.3% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 84,472 | 85,230 | +0.9% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 74,663 | 74,423 | -0.3% |
| `software/amazon/awssdk/thirdparty/jackson/core/util/TextBuffer.setCurrentAndReturn` | 67,097 | 67,590 | +0.7% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 60,627 | 60,743 | +0.2% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.<init>` | 42,686 | 42,162 | -1.2% |
| `software/amazon/awssdk/services/dynamodb/model/MapAttributeValueCopier.lambda$copy$0` | 11,723 | 12,132 | +3.5% |
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$1` | 7,266 | 7,054 | -2.9% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,337 | 0 | -100.0% |
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,023 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/ListAttributeValueCopier.copy` | 2,593 | 2,688 | +3.7% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,474 | 0 | -100.0% |

### v2-sync / batch-put

| site | baseline | phaseD | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.write` | 114,812 | 2 | -100.0% |
| `software/amazon/awssdk/protocols/json/internal/marshall/JsonProtocolMarshaller.doMarshall` | 32,430 | 32,808 | +1.2% |
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,223 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,051 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,528 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,366 | 0 | -100.0% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,716 | 241 | -86.0% |
| `software/amazon/awssdk/protocols/json/internal/marshall/SimpleTypeJsonMarshaller$13.marshall` | 1,568 | 1,713 | +9.3% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,108 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 963 | 975 | +1.2% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 963 | 37,139 | +3757.4% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption.<init>` | 922 | 1,051 | +14.0% |

### v2-async / small-get

| site | baseline | phaseD | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 4,509 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,209 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,386 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,264 | 0 | -100.0% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,243 | 2,023 | -9.8% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 2,226 | 2,076 | -6.7% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeMapCopier.lambda$copy$0` | 1,487 | 1,585 | +6.6% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,306 | 1,199 | -8.2% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,230 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 1,201 | 1,094 | -8.9% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,103 | 1,049 | -5.0% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 1,084 | 1,034 | -4.6% |

### v2-async / small-put

| site | baseline | phaseD | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 5,059 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,173 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,457 | 0 | -100.0% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,369 | 2,240 | -5.4% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 2,283 | 2,066 | -9.5% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,214 | 0 | -100.0% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,275 | 1,304 | +2.2% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,091 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CompletableFutureUtils.forwardExceptionTo` | 1,049 | 1,010 | -3.6% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,020 | 1,060 | +4.0% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 999 | 1,134 | +13.6% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 910 | 918 | +0.8% |

### v2-async / batch-get

| site | baseline | phaseD | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$2` | 120,238 | 120,224 | -0.0% |
| `software/amazon/awssdk/core/internal/http/async/AsyncResponseHandler$BaosSubscriber.onNext` | 89,908 | 68,767 | -23.5% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 85,332 | 85,881 | +0.6% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 74,530 | 74,518 | -0.0% |
| `software/amazon/awssdk/thirdparty/jackson/core/util/TextBuffer.setCurrentAndReturn` | 67,071 | 67,104 | +0.0% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 60,236 | 60,832 | +1.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.<init>` | 42,863 | 43,332 | +1.1% |
| `software/amazon/awssdk/crt/http/HttpStreamResponseHandlerNativeAdapter.onResponseBody` | 37,453 | 36,269 | -3.2% |
| `software/amazon/awssdk/utils/BinaryUtils.copyBytesFrom` | 36,729 | 36,681 | -0.1% |
| `software/amazon/awssdk/core/internal/http/async/AsyncResponseHandler.lambda$prepare$0` | 36,293 | 36,569 | +0.8% |
| `software/amazon/awssdk/services/dynamodb/model/MapAttributeValueCopier.lambda$copy$0` | 12,047 | 11,916 | -1.1% |
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$1` | 7,178 | 7,342 | +2.3% |

### v2-async / batch-put

| site | baseline | phaseD | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 163,713 | 0 | -100.0% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.write` | 115,183 | 0 | -100.0% |
| `software/amazon/awssdk/protocols/json/internal/marshall/JsonProtocolMarshaller.doMarshall` | 33,342 | 32,687 | -2.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,159 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,414 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,378 | 0 | -100.0% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,207 | 2,362 | +7.0% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 1,916 | 1,904 | -0.6% |
| `software/amazon/awssdk/protocols/json/internal/marshall/SimpleTypeJsonMarshaller$13.marshall` | 1,621 | 1,659 | +2.4% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,263 | 1,439 | +14.0% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 1,130 | 1,060 | -6.1% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 1,032 | 1,056 | +2.3% |

