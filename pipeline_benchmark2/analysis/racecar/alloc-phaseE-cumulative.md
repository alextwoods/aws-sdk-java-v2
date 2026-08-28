# Allocation per operation by phase

Runs compared (first is the baseline):

- `baseline`: `pipeline_benchmark2/raw/phase0-baseline/20260827-1134`
- `phaseE`: `pipeline_benchmark2/raw/phaseE-marshal/20260828-0225`

asprof alloc `--total` bytes divided by 220,000 ops (warmup + measured). Client-code
bytes only: JIT, GC/VM and benchmark-harness stacks are excluded.

## Totals (bytes/op, client code)

| client | scenario | baseline | phaseE | delta |
|--------|----------|----:|----:|----:|
| v2-sync | small-get | 61,387 | 40,153 | -34.6% |
| v2-sync | small-put | 54,509 | 33,094 | -39.3% |
| v2-sync | batch-get | 533,097 | 511,523 | -4.0% |
| v2-sync | batch-put | 204,272 | 105,355 | -48.4% |
| v2-async | small-get | 69,702 | 49,085 | -29.6% |
| v2-async | small-put | 61,797 | 39,345 | -36.3% |
| v2-async | batch-get | 742,955 | 717,944 | -3.4% |
| v2-async | batch-put | 371,884 | 112,345 | -69.8% |

## By category (bytes/op)

### v2-sync / small-get

| category | baseline | phaseE | delta |
|----------|----:|----:|----:|
| pipeline-framework | 25,356 | 20,185 | -20.4% |
| signing | 20,905 | 5,190 | -75.2% |
| unmarshall | 7,142 | 7,342 | +2.8% |
| json | 3,334 | 3,432 | +2.9% |
| retry | 1,544 | 1,315 | -14.8% |
| endpoint-rules | 1,497 | 1,304 | -12.9% |
| marshall | 1,246 | 1,292 | +3.6% |
| crypto | 338 | 83 | -75.4% |
| http-client | 12 | 2 | -80.0% |
| other | 12 | 7 | -40.0% |

### v2-sync / small-put

| category | baseline | phaseE | delta |
|----------|----:|----:|----:|
| pipeline-framework | 24,775 | 19,606 | -20.9% |
| signing | 20,733 | 5,274 | -74.6% |
| json | 2,605 | 2,445 | -6.1% |
| marshall | 1,809 | 1,804 | -0.3% |
| retry | 1,561 | 1,218 | -22.0% |
| endpoint-rules | 1,423 | 1,437 | +1.0% |
| unmarshall | 1,282 | 1,225 | -4.5% |
| crypto | 303 | 76 | -74.8% |
| http-client | 12 | 5 | -60.0% |
| other | 7 | 5 | -33.3% |

### v2-sync / batch-get

| category | baseline | phaseE | delta |
|----------|----:|----:|----:|
| unmarshall | 368,004 | 367,899 | -0.0% |
| json | 69,983 | 69,930 | -0.1% |
| pipeline-framework | 67,826 | 62,495 | -7.9% |
| signing | 20,781 | 5,324 | -74.4% |
| endpoint-rules | 2,328 | 2,431 | +4.4% |
| marshall | 2,121 | 1,914 | -9.8% |
| retry | 1,713 | 1,423 | -17.0% |
| crypto | 322 | 100 | -68.9% |
| other | 14 | 2 | -83.3% |
| http-client | 5 | 5 | +0.0% |

### v2-sync / batch-put

| category | baseline | phaseE | delta |
|----------|----:|----:|----:|
| json | 117,297 | 38,175 | -67.5% |
| marshall | 35,952 | 36,338 | +1.1% |
| pipeline-framework | 24,546 | 20,137 | -18.0% |
| signing | 20,817 | 5,198 | -75.0% |
| endpoint-rules | 2,312 | 2,357 | +2.0% |
| unmarshall | 1,554 | 1,518 | -2.3% |
| retry | 1,501 | 1,444 | -3.8% |
| crypto | 276 | 98 | -64.7% |
| other | 12 | 10 | -20.0% |
| http-client | 5 | 81 | +1600.0% |

### v2-async / small-get

| category | baseline | phaseE | delta |
|----------|----:|----:|----:|
| signing | 23,042 | 7,390 | -67.9% |
| pipeline-framework | 17,804 | 13,157 | -26.1% |
| unmarshall | 11,143 | 11,367 | +2.0% |
| http-client | 8,555 | 8,608 | +0.6% |
| json | 3,382 | 3,222 | -4.7% |
| retry | 1,897 | 1,749 | -7.8% |
| marshall | 1,847 | 1,802 | -2.5% |
| endpoint-rules | 1,406 | 1,370 | -2.5% |
| crypto | 324 | 86 | -73.5% |
| other | 300 | 334 | +11.1% |

### v2-async / small-put

| category | baseline | phaseE | delta |
|----------|----:|----:|----:|
| signing | 22,849 | 7,290 | -68.1% |
| pipeline-framework | 18,124 | 11,880 | -34.5% |
| http-client | 8,594 | 8,436 | -1.8% |
| unmarshall | 3,570 | 3,444 | -3.5% |
| marshall | 2,414 | 2,514 | +4.1% |
| json | 2,397 | 2,447 | +2.1% |
| retry | 1,871 | 1,680 | -10.2% |
| endpoint-rules | 1,342 | 1,230 | -8.3% |
| crypto | 357 | 102 | -71.3% |
| other | 279 | 322 | +15.4% |

### v2-async / batch-get

| category | baseline | phaseE | delta |
|----------|----:|----:|----:|
| unmarshall | 572,452 | 570,045 | -0.4% |
| json | 69,821 | 69,408 | -0.6% |
| pipeline-framework | 61,444 | 54,988 | -10.5% |
| signing | 22,835 | 7,435 | -67.4% |
| http-client | 8,801 | 8,992 | +2.2% |
| marshall | 2,500 | 2,583 | +3.3% |
| endpoint-rules | 2,335 | 2,285 | -2.1% |
| retry | 2,009 | 1,780 | -11.4% |
| other | 388 | 317 | -18.4% |
| crypto | 369 | 110 | -70.3% |

### v2-async / batch-put

| category | baseline | phaseE | delta |
|----------|----:|----:|----:|
| pipeline-framework | 176,701 | 12,878 | -92.7% |
| json | 117,478 | 38,619 | -67.1% |
| marshall | 37,763 | 36,938 | -2.2% |
| signing | 22,654 | 7,223 | -68.1% |
| http-client | 8,689 | 8,434 | -2.9% |
| unmarshall | 3,632 | 3,742 | +3.0% |
| endpoint-rules | 2,331 | 2,371 | +1.7% |
| retry | 1,988 | 1,759 | -11.5% |
| crypto | 353 | 86 | -75.7% |
| other | 296 | 296 | +0.0% |

## Top 12 allocation sites, baseline vs final phase (bytes/op)

### v2-sync / small-get

| site | baseline | phaseE | delta |
|------|----:|----:|----:|
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,113 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,020 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,579 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,402 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeMapCopier.lambda$copy$0` | 1,599 | 1,513 | -5.4% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,561 | 1,251 | -19.8% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,203 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 1,094 | 1,254 | +14.6% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,051 | 1,118 | +6.3% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 946 | 1,030 | +8.8% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 939 | 932 | -0.8% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 908 | 965 | +6.3% |

### v2-sync / small-put

| site | baseline | phaseE | delta |
|------|----:|----:|----:|
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,159 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,006 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,400 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,252 | 0 | -100.0% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,547 | 1,132 | -26.8% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,096 | 0 | -100.0% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,051 | 1,041 | -0.9% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption.<init>` | 958 | 932 | -2.7% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 922 | 906 | -1.8% |
| `software/amazon/awssdk/internal/http/LowCopyListMap$ForBuildable.<init>` | 910 | 901 | -1.0% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 903 | 918 | +1.6% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/CredentialScope.scope` | 903 | 0 | -100.0% |

### v2-sync / batch-get

| site | baseline | phaseE | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$2` | 119,132 | 119,271 | +0.1% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 84,472 | 84,892 | +0.5% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 74,663 | 73,848 | -1.1% |
| `software/amazon/awssdk/thirdparty/jackson/core/util/TextBuffer.setCurrentAndReturn` | 67,097 | 67,130 | +0.0% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 60,627 | 61,103 | +0.8% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.<init>` | 42,686 | 42,131 | -1.3% |
| `software/amazon/awssdk/services/dynamodb/model/MapAttributeValueCopier.lambda$copy$0` | 11,723 | 12,194 | +4.0% |
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$1` | 7,266 | 7,133 | -1.8% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,337 | 0 | -100.0% |
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,023 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/ListAttributeValueCopier.copy` | 2,593 | 2,550 | -1.7% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,474 | 0 | -100.0% |

### v2-sync / batch-put

| site | baseline | phaseE | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.write` | 114,812 | 0 | -100.0% |
| `software/amazon/awssdk/protocols/json/internal/marshall/JsonProtocolMarshaller.doMarshall` | 32,430 | 32,708 | +0.9% |
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,223 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,051 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,528 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,366 | 0 | -100.0% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,716 | 1,637 | -4.6% |
| `software/amazon/awssdk/protocols/json/internal/marshall/SimpleTypeJsonMarshaller$13.marshall` | 1,568 | 1,578 | +0.6% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,108 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 963 | 977 | +1.5% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 963 | 36,657 | +3707.4% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption.<init>` | 922 | 939 | +1.8% |

### v2-async / small-get

| site | baseline | phaseE | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 4,509 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,209 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,386 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,264 | 0 | -100.0% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,243 | 2,192 | -2.2% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 2,226 | 2,088 | -6.2% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeMapCopier.lambda$copy$0` | 1,487 | 1,601 | +7.7% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,306 | 1,220 | -6.6% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,230 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 1,201 | 1,149 | -4.4% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,103 | 960 | -13.0% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 1,084 | 1,161 | +7.0% |

### v2-async / small-put

| site | baseline | phaseE | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 5,059 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,173 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,457 | 0 | -100.0% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,369 | 2,200 | -7.1% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 2,283 | 2,028 | -11.2% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,214 | 0 | -100.0% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,275 | 1,246 | -2.2% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,091 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CompletableFutureUtils.forwardExceptionTo` | 1,049 | 975 | -7.0% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,020 | 1,096 | +7.5% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 999 | 1,063 | +6.4% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 910 | 872 | -4.2% |

### v2-async / batch-get

| site | baseline | phaseE | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$2` | 120,238 | 118,834 | -1.2% |
| `software/amazon/awssdk/core/internal/http/async/AsyncResponseHandler$BaosSubscriber.onNext` | 89,908 | 90,561 | +0.7% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 85,332 | 85,027 | -0.4% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 74,530 | 73,298 | -1.7% |
| `software/amazon/awssdk/thirdparty/jackson/core/util/TextBuffer.setCurrentAndReturn` | 67,071 | 66,792 | -0.4% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 60,236 | 61,051 | +1.4% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.<init>` | 42,863 | 42,167 | -1.6% |
| `software/amazon/awssdk/crt/http/HttpStreamResponseHandlerNativeAdapter.onResponseBody` | 37,453 | 37,046 | -1.1% |
| `software/amazon/awssdk/utils/BinaryUtils.copyBytesFrom` | 36,729 | 36,812 | +0.2% |
| `software/amazon/awssdk/core/internal/http/async/AsyncResponseHandler.lambda$prepare$0` | 36,293 | 36,555 | +0.7% |
| `software/amazon/awssdk/services/dynamodb/model/MapAttributeValueCopier.lambda$copy$0` | 12,047 | 12,171 | +1.0% |
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$1` | 7,178 | 7,302 | +1.7% |

### v2-async / batch-put

| site | baseline | phaseE | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 163,713 | 0 | -100.0% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.write` | 115,183 | 2 | -100.0% |
| `software/amazon/awssdk/protocols/json/internal/marshall/JsonProtocolMarshaller.doMarshall` | 33,342 | 32,623 | -2.2% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,159 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,414 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,378 | 0 | -100.0% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,207 | 2,276 | +3.1% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 1,916 | 1,883 | -1.7% |
| `software/amazon/awssdk/protocols/json/internal/marshall/SimpleTypeJsonMarshaller$13.marshall` | 1,621 | 1,442 | -11.0% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,263 | 1,325 | +4.9% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 1,130 | 965 | -14.6% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 1,032 | 1,175 | +13.9% |

