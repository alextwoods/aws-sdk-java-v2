# Allocation per operation by phase

Runs compared (first is the baseline):

- `baseline`: `pipeline_benchmark2/raw/phase0-baseline/20260827-1134`
- `signerF`: `pipeline_benchmark2/raw/phaseF-signer/20260827-1223`

asprof alloc `--total` bytes divided by 220,000 ops (warmup + measured). Client-code
bytes only: JIT, GC/VM and benchmark-harness stacks are excluded.

## Totals (bytes/op, client code)

| client | scenario | baseline | signerF | delta |
|--------|----------|----:|----:|----:|
| v2-sync | small-get | 61,387 | 44,922 | -26.8% |
| v2-sync | small-put | 54,509 | 37,803 | -30.6% |
| v2-sync | batch-get | 533,097 | 517,121 | -3.0% |
| v2-sync | batch-put | 204,272 | 191,024 | -6.5% |
| v2-async | small-get | 69,702 | 55,184 | -20.8% |
| v2-async | small-put | 61,797 | 46,759 | -24.3% |
| v2-async | batch-get | 742,955 | 725,353 | -2.4% |
| v2-async | batch-put | 371,884 | 356,448 | -4.2% |

## By category (bytes/op)

### v2-sync / small-get

| category | baseline | signerF | delta |
|----------|----:|----:|----:|
| pipeline-framework | 25,356 | 24,680 | -2.7% |
| signing | 20,905 | 5,607 | -73.2% |
| unmarshall | 7,142 | 7,154 | +0.2% |
| json | 3,334 | 3,293 | -1.2% |
| retry | 1,544 | 1,525 | -1.2% |
| endpoint-rules | 1,497 | 1,261 | -15.8% |
| marshall | 1,246 | 1,268 | +1.7% |
| crypto | 338 | 105 | -69.0% |
| http-client | 12 | 12 | +0.0% |
| other | 12 | 17 | +40.0% |

### v2-sync / small-put

| category | baseline | signerF | delta |
|----------|----:|----:|----:|
| pipeline-framework | 24,775 | 23,915 | -3.5% |
| signing | 20,733 | 5,553 | -73.2% |
| json | 2,605 | 2,416 | -7.2% |
| marshall | 1,809 | 1,825 | +0.9% |
| retry | 1,561 | 1,389 | -11.0% |
| endpoint-rules | 1,423 | 1,401 | -1.5% |
| unmarshall | 1,282 | 1,189 | -7.2% |
| crypto | 303 | 86 | -71.7% |
| http-client | 12 | 7 | -40.0% |
| other | 7 | 21 | +200.0% |

### v2-sync / batch-get

| category | baseline | signerF | delta |
|----------|----:|----:|----:|
| unmarshall | 368,004 | 369,103 | +0.3% |
| json | 69,983 | 69,618 | -0.5% |
| pipeline-framework | 67,826 | 66,751 | -1.6% |
| signing | 20,781 | 5,548 | -73.3% |
| endpoint-rules | 2,328 | 2,395 | +2.9% |
| marshall | 2,121 | 2,057 | -3.0% |
| retry | 1,713 | 1,530 | -10.7% |
| crypto | 322 | 95 | -70.4% |
| other | 14 | 19 | +33.3% |
| http-client | 5 | 5 | +0.0% |

### v2-sync / batch-put

| category | baseline | signerF | delta |
|----------|----:|----:|----:|
| json | 117,297 | 118,019 | +0.6% |
| marshall | 35,952 | 37,255 | +3.6% |
| pipeline-framework | 24,546 | 24,694 | +0.6% |
| signing | 20,817 | 5,445 | -73.8% |
| endpoint-rules | 2,312 | 2,352 | +1.8% |
| unmarshall | 1,554 | 1,437 | -7.5% |
| retry | 1,501 | 1,680 | +11.9% |
| crypto | 276 | 122 | -56.0% |
| other | 12 | 14 | +20.0% |
| http-client | 5 | 5 | +0.0% |

### v2-async / small-get

| category | baseline | signerF | delta |
|----------|----:|----:|----:|
| signing | 23,042 | 7,879 | -65.8% |
| pipeline-framework | 17,804 | 18,338 | +3.0% |
| unmarshall | 11,143 | 11,353 | +1.9% |
| http-client | 8,555 | 8,605 | +0.6% |
| json | 3,382 | 3,253 | -3.8% |
| retry | 1,897 | 2,071 | +9.2% |
| marshall | 1,847 | 1,892 | +2.5% |
| endpoint-rules | 1,406 | 1,397 | -0.7% |
| crypto | 324 | 119 | -63.2% |
| other | 300 | 276 | -7.9% |

### v2-async / small-put

| category | baseline | signerF | delta |
|----------|----:|----:|----:|
| signing | 22,849 | 7,960 | -65.2% |
| pipeline-framework | 18,124 | 18,040 | -0.5% |
| http-client | 8,594 | 8,625 | +0.4% |
| unmarshall | 3,570 | 3,641 | +2.0% |
| marshall | 2,414 | 2,390 | -1.0% |
| json | 2,397 | 2,457 | +2.5% |
| retry | 1,871 | 1,945 | +3.9% |
| endpoint-rules | 1,342 | 1,320 | -1.6% |
| crypto | 357 | 74 | -79.3% |
| other | 279 | 307 | +10.3% |

### v2-async / batch-get

| category | baseline | signerF | delta |
|----------|----:|----:|----:|
| unmarshall | 572,452 | 570,760 | -0.3% |
| json | 69,821 | 69,625 | -0.3% |
| pipeline-framework | 61,444 | 61,263 | -0.3% |
| signing | 22,835 | 7,740 | -66.1% |
| http-client | 8,801 | 8,605 | -2.2% |
| marshall | 2,500 | 2,600 | +4.0% |
| endpoint-rules | 2,335 | 2,283 | -2.2% |
| retry | 2,009 | 2,026 | +0.8% |
| other | 388 | 348 | -10.4% |
| crypto | 369 | 102 | -72.3% |

### v2-async / batch-put

| category | baseline | signerF | delta |
|----------|----:|----:|----:|
| pipeline-framework | 176,701 | 176,556 | -0.1% |
| json | 117,478 | 117,867 | +0.3% |
| marshall | 37,763 | 37,065 | -1.8% |
| signing | 22,654 | 7,702 | -66.0% |
| http-client | 8,689 | 8,684 | -0.1% |
| unmarshall | 3,632 | 3,865 | +6.4% |
| endpoint-rules | 2,331 | 2,409 | +3.4% |
| retry | 1,988 | 1,921 | -3.4% |
| crypto | 353 | 105 | -70.3% |
| other | 296 | 274 | -7.3% |

## Top 12 allocation sites, baseline vs final phase (bytes/op)

### v2-sync / small-get

| site | baseline | signerF | delta |
|------|----:|----:|----:|
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,113 | 4,213 | +2.4% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,020 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,579 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,402 | 1,756 | -26.9% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeMapCopier.lambda$copy$0` | 1,599 | 1,528 | -4.5% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,561 | 1,165 | -25.3% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,203 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 1,094 | 1,075 | -1.7% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,051 | 1,041 | -0.9% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 946 | 1,032 | +9.1% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 939 | 913 | -2.8% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 908 | 975 | +7.3% |

### v2-sync / small-put

| site | baseline | signerF | delta |
|------|----:|----:|----:|
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,159 | 4,042 | -2.8% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,006 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,400 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,252 | 1,568 | -30.4% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,547 | 1,168 | -24.5% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,096 | 0 | -100.0% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,051 | 972 | -7.5% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption.<init>` | 958 | 803 | -16.2% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 922 | 898 | -2.6% |
| `software/amazon/awssdk/internal/http/LowCopyListMap$ForBuildable.<init>` | 910 | 794 | -12.8% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 903 | 937 | +3.7% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/CredentialScope.scope` | 903 | 0 | -100.0% |

### v2-sync / batch-get

| site | baseline | signerF | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$2` | 119,132 | 119,361 | +0.2% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 84,472 | 85,111 | +0.8% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 74,663 | 74,790 | +0.2% |
| `software/amazon/awssdk/thirdparty/jackson/core/util/TextBuffer.setCurrentAndReturn` | 67,097 | 66,649 | -0.7% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 60,627 | 60,646 | +0.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.<init>` | 42,686 | 42,429 | -0.6% |
| `software/amazon/awssdk/services/dynamodb/model/MapAttributeValueCopier.lambda$copy$0` | 11,723 | 11,794 | +0.6% |
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$1` | 7,266 | 7,292 | +0.4% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,337 | 0 | -100.0% |
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,023 | 4,039 | +0.4% |
| `software/amazon/awssdk/services/dynamodb/model/ListAttributeValueCopier.copy` | 2,593 | 2,679 | +3.3% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,474 | 0 | -100.0% |

### v2-sync / batch-put

| site | baseline | signerF | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.write` | 114,812 | 115,584 | +0.7% |
| `software/amazon/awssdk/protocols/json/internal/marshall/JsonProtocolMarshaller.doMarshall` | 32,430 | 33,371 | +2.9% |
| `org/apache/hc/core5/http/io/entity/InputStreamEntity.writeTo` | 4,223 | 3,930 | -6.9% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,051 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,528 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,366 | 1,797 | -24.1% |
| `software/amazon/awssdk/http/apache5/internal/impl/Apache5HttpRequestFactory.lambda$addHeadersToRequest$0` | 1,716 | 1,616 | -5.8% |
| `software/amazon/awssdk/protocols/json/internal/marshall/SimpleTypeJsonMarshaller$13.marshall` | 1,568 | 1,740 | +10.9% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,108 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 963 | 1,120 | +16.3% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 963 | 1,027 | +6.7% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption.<init>` | 922 | 1,037 | +12.4% |

### v2-async / small-get

| site | baseline | signerF | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 4,509 | 4,461 | -1.1% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,209 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,386 | 1,675 | -29.8% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,264 | 0 | -100.0% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,243 | 2,176 | -3.0% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 2,226 | 2,231 | +0.2% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeMapCopier.lambda$copy$0` | 1,487 | 1,516 | +1.9% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,306 | 1,354 | +3.6% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,230 | 0 | -100.0% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 1,201 | 1,065 | -11.3% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,103 | 1,041 | -5.6% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 1,084 | 1,053 | -2.9% |

### v2-async / small-put

| site | baseline | signerF | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 5,059 | 5,217 | +3.1% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,173 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,457 | 0 | -100.0% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,369 | 2,221 | -6.2% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 2,283 | 2,247 | -1.6% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,214 | 1,563 | -29.4% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,275 | 1,287 | +0.9% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4RequestSigner.lambda$header$0` | 1,091 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CompletableFutureUtils.forwardExceptionTo` | 1,049 | 1,032 | -1.6% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.<init>` | 1,020 | 1,049 | +2.8% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 999 | 1,127 | +12.9% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 910 | 994 | +9.2% |

### v2-async / batch-get

| site | baseline | signerF | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$2` | 120,238 | 119,313 | -0.8% |
| `software/amazon/awssdk/core/internal/http/async/AsyncResponseHandler$BaosSubscriber.onNext` | 89,908 | 90,342 | +0.5% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue.builder` | 85,332 | 85,030 | -0.4% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.build` | 74,530 | 74,587 | +0.1% |
| `software/amazon/awssdk/thirdparty/jackson/core/util/TextBuffer.setCurrentAndReturn` | 67,071 | 66,913 | -0.2% |
| `software/amazon/awssdk/protocols/json/internal/unmarshall/JsonUnmarshallingParser.parseMap` | 60,236 | 60,710 | +0.8% |
| `software/amazon/awssdk/services/dynamodb/model/AttributeValue$BuilderImpl.<init>` | 42,863 | 42,441 | -1.0% |
| `software/amazon/awssdk/crt/http/HttpStreamResponseHandlerNativeAdapter.onResponseBody` | 37,453 | 36,459 | -2.7% |
| `software/amazon/awssdk/utils/BinaryUtils.copyBytesFrom` | 36,729 | 36,688 | -0.1% |
| `software/amazon/awssdk/core/internal/http/async/AsyncResponseHandler.lambda$prepare$0` | 36,293 | 36,876 | +1.6% |
| `software/amazon/awssdk/services/dynamodb/model/MapAttributeValueCopier.lambda$copy$0` | 12,047 | 11,732 | -2.6% |
| `software/amazon/awssdk/services/dynamodb/model/BatchGetResponseMapCopier.lambda$copy$1` | 7,178 | 7,030 | -2.1% |

### v2-async / batch-put

| site | baseline | signerF | delta |
|------|----:|----:|----:|
| `software/amazon/awssdk/utils/IoUtils.toByteArray` | 163,713 | 163,692 | -0.0% |
| `software/amazon/awssdk/protocols/json/SdkByteArrayOutputStream.write` | 115,183 | 115,298 | +0.1% |
| `software/amazon/awssdk/protocols/json/internal/marshall/JsonProtocolMarshaller.doMarshall` | 33,342 | 32,694 | -1.9% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/util/ChecksumUtil.lambda$readAll$0` | 4,159 | 0 | -100.0% |
| `software/amazon/awssdk/http/auth/aws/internal/signer/V4CanonicalRequest.getCanonicalHeadersString` | 2,414 | 0 | -100.0% |
| `software/amazon/awssdk/utils/CollectionUtils.lambda$deepCopyMap$1` | 2,378 | 1,578 | -33.7% |
| `software/amazon/awssdk/http/crt/internal/request/CrtRequestAdapter.lambda$createAsyncHttpHeaderList$0` | 2,207 | 2,245 | +1.7% |
| `software/amazon/awssdk/http/DefaultSdkHttpFullRequest$Builder.putHeader` | 1,916 | 2,290 | +19.5% |
| `software/amazon/awssdk/protocols/json/internal/marshall/SimpleTypeJsonMarshaller$13.marshall` | 1,621 | 1,711 | +5.6% |
| `software/amazon/awssdk/crt/http/HttpHeader.<init>` | 1,263 | 1,292 | +2.3% |
| `software/amazon/awssdk/crt/http/HttpRequestBase.marshalForJni` | 1,130 | 1,080 | -4.4% |
| `software/amazon/awssdk/http/auth/spi/internal/scheme/DefaultAuthSchemeOption$BuilderImpl.<init>` | 1,032 | 1,010 | -2.1% |

