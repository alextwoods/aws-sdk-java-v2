/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http.crt.internal.request;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.HttpTestUtils.createProvider;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequestBase;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.http.crt.internal.CrtAsyncRequestContext;
import software.amazon.awssdk.http.crt.internal.CrtRequestContext;

public class CrtRequestAdapterTest {

    @Test
    public void toAsyncCrtRequest_headersAreOrderedAndMultiValuesArePreserved() {
        List<String> zValues = new ArrayList<>(Arrays.asList("z1", "z2", "z3", "z4", "z5", "z6"));
        Map<String, List<String>> sourceHeaders = new LinkedHashMap<>();
        sourceHeaders.put("z-Header", zValues);
        sourceHeaders.put("Empty", Collections.emptyList());
        sourceHeaders.put("a-Header", Arrays.asList("a1", "a2"));
        SdkHttpFullRequest sdkRequest = requestBuilder().headers(sourceHeaders).build();

        HttpRequestBase crtRequest = toAsyncCrtRequest(sdkRequest, createProvider("content-with-known-length"),
                                                       Protocol.HTTP1_1);

        assertHeaders(crtRequest,
                      "Host=localhost",
                      "Connection=keep-alive",
                      "Content-Length=25",
                      "a-Header=a1",
                      "a-Header=a2",
                      "z-Header=z1",
                      "z-Header=z2",
                      "z-Header=z3",
                      "z-Header=z4",
                      "z-Header=z5",
                      "z-Header=z6");
        assertThat(sourceHeaders).containsEntry("z-Header", zValues)
                                 .containsEntry("Empty", Collections.emptyList())
                                 .containsEntry("a-Header", Arrays.asList("a1", "a2"));
    }

    @ParameterizedTest
    @CsvSource({
        "http://example.com:8443, example.com",
        "http://127.0.0.1:8443, 127.0.0.1",
        "'http://[::1]:8443', '[::1]'"
    })
    public void toAsyncCrtRequest_hostDerivedWithoutPort(String uri, String expectedHost) {
        SdkHttpFullRequest sdkRequest = requestBuilder().uri(URI.create(uri)).build();

        HttpRequestBase crtRequest = toAsyncCrtRequest(sdkRequest, unknownLengthPublisher(), Protocol.HTTP2);

        assertHeaders(crtRequest, "Host=" + expectedHost);
    }

    @Test
    public void toAsyncCrtRequest_explicitHostPreservesOriginalCasingAndValue() {
        SdkHttpFullRequest sdkRequest = requestBuilder().putHeader("hOsT", "example.com:8443").build();

        HttpRequestBase crtRequest = toAsyncCrtRequest(sdkRequest, unknownLengthPublisher(), Protocol.HTTP2);

        assertHeaders(crtRequest, "hOsT=example.com:8443");
    }

    @Test
    public void toAsyncCrtRequest_http2DoesNotAddConnection() {
        SdkHttpFullRequest sdkRequest = requestBuilder().build();

        HttpRequestBase crtRequest = toAsyncCrtRequest(sdkRequest, unknownLengthPublisher(), Protocol.HTTP2);

        assertHeaders(crtRequest, "Host=localhost");
    }

    @Test
    public void toAsyncCrtRequest_http2PreservesExplicitConnection() {
        SdkHttpFullRequest sdkRequest = requestBuilder().putHeader("cOnNeCtIoN", "close").build();

        HttpRequestBase crtRequest = toAsyncCrtRequest(sdkRequest, unknownLengthPublisher(), Protocol.HTTP2);

        assertHeaders(crtRequest, "Host=localhost", "cOnNeCtIoN=close");
    }

    @Test
    public void toAsyncCrtRequest_transferEncodingPresent_doesNotAddContentLength() {
        SdkHttpFullRequest sdkRequest = requestBuilder().putHeader(Header.TRANSFER_ENCODING, "chunked").build();
        SdkHttpContentPublisher publisher = createProvider("content-with-known-length");

        HttpRequestBase crtRequest = toAsyncCrtRequest(sdkRequest, publisher);

        assertThat(headerNames(crtRequest)).contains(Header.TRANSFER_ENCODING)
                                           .doesNotContain(Header.CONTENT_LENGTH);
    }

    @Test
    public void toAsyncCrtRequest_noTransferEncoding_addsContentLengthFromPublisher() {
        SdkHttpFullRequest sdkRequest = requestBuilder().build();
        SdkHttpContentPublisher publisher = createProvider("content-with-known-length");

        HttpRequestBase crtRequest = toAsyncCrtRequest(sdkRequest, publisher);

        assertThat(headerNames(crtRequest)).contains(Header.CONTENT_LENGTH)
                                           .doesNotContain(Header.TRANSFER_ENCODING);
    }

    @Test
    public void toAsyncCrtRequest_unknownPublisherLength_doesNotAddContentLength() {
        SdkHttpFullRequest sdkRequest = requestBuilder().build();

        HttpRequestBase crtRequest = toAsyncCrtRequest(sdkRequest, unknownLengthPublisher());

        assertThat(headerNames(crtRequest)).doesNotContain(Header.CONTENT_LENGTH);
    }

    @Test
    public void toAsyncCrtRequest_explicitContentLengthIsPreserved() {
        SdkHttpFullRequest sdkRequest = requestBuilder().putHeader("cOnTeNt-LeNgTh", "42").build();

        HttpRequestBase crtRequest = toAsyncCrtRequest(sdkRequest, createProvider("content-with-known-length"));

        assertThat(crtRequest.getHeaders()).filteredOn(header -> header.getName().equalsIgnoreCase(Header.CONTENT_LENGTH))
                                           .extracting(HttpHeader::getValue)
                                           .containsExactly("42");
    }

    @Test
    public void toCrtRequest_headersAreOrderedAndSyntheticHeadersComeFirst() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("z-Header", Arrays.asList("z1", "z2"));
        headers.put("a-Header", Collections.singletonList("a1"));
        SdkHttpFullRequest sdkRequest = requestBuilder().headers(headers).build();

        HttpRequestBase crtRequest = toCrtRequest(sdkRequest);

        assertHeaders(crtRequest,
                      "Host=localhost",
                      "Connection=keep-alive",
                      "a-Header=a1",
                      "z-Header=z1",
                      "z-Header=z2");
    }

    @Test
    public void toCrtRequest_explicitConnectionIsPreservedWithoutSyntheticConnection() {
        SdkHttpFullRequest sdkRequest = requestBuilder().putHeader("cOnNeCtIoN", "close").build();

        HttpRequestBase crtRequest = toCrtRequest(sdkRequest);

        assertHeaders(crtRequest, "Host=localhost", "cOnNeCtIoN=close");
    }

    @Test
    public void toCrtRequest_transferEncodingPresent_doesNotAddContentLength() {
        SdkHttpFullRequest sdkRequest = requestBuilder().putHeader(Header.TRANSFER_ENCODING, "chunked").build();

        HttpRequestBase crtRequest = toCrtRequest(sdkRequest);

        assertThat(headerNames(crtRequest)).contains(Header.TRANSFER_ENCODING)
                                           .doesNotContain(Header.CONTENT_LENGTH);
    }

    @Test
    public void toCrtRequest_contentLengthOnRequest_isPreserved() {
        SdkHttpFullRequest sdkRequest = requestBuilder().putHeader(Header.CONTENT_LENGTH, "42").build();

        HttpRequestBase crtRequest = toCrtRequest(sdkRequest);

        assertThat(headerNames(crtRequest)).contains(Header.CONTENT_LENGTH)
                                           .doesNotContain(Header.TRANSFER_ENCODING);
    }

    private static SdkHttpFullRequest.Builder requestBuilder() {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://localhost:8080"))
                                 .method(SdkHttpMethod.POST)
                                 .encodedPath("/");
    }

    private static HttpRequestBase toAsyncCrtRequest(SdkHttpFullRequest sdkRequest, SdkHttpContentPublisher publisher) {
        return toAsyncCrtRequest(sdkRequest, publisher, Protocol.HTTP1_1);
    }

    private static HttpRequestBase toAsyncCrtRequest(SdkHttpFullRequest sdkRequest, SdkHttpContentPublisher publisher,
                                                     Protocol protocol) {
        AsyncExecuteRequest asyncRequest = AsyncExecuteRequest.builder()
                                                              .request(sdkRequest)
                                                              .requestContentPublisher(publisher)
                                                              .build();
        CrtAsyncRequestContext context = CrtAsyncRequestContext.builder()
                                                               .request(asyncRequest)
                                                               .readBufferSize(2000)
                                                               .protocol(protocol)
                                                               .build();
        return CrtRequestAdapter.toAsyncCrtRequest(context);
    }

    private static HttpRequestBase toCrtRequest(SdkHttpFullRequest sdkRequest) {
        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                                                              .request(sdkRequest)
                                                              .build();
        CrtRequestContext context = CrtRequestContext.builder()
                                                     .request(executeRequest)
                                                     .readBufferSize(2000)
                                                     .build();
        return CrtRequestAdapter.toCrtRequest(context);
    }

    private static SdkHttpContentPublisher unknownLengthPublisher() {
        return new SdkHttpContentPublisher() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            }
        };
    }

    private static void assertHeaders(HttpRequestBase crtRequest, String... expectedHeaders) {
        assertThat(crtRequest.getHeaders()).extracting(header -> header.getName() + "=" + header.getValue())
                                           .containsExactly(expectedHeaders);
    }

    private static List<String> headerNames(HttpRequestBase crtRequest) {
        List<String> result = new ArrayList<>();
        for (HttpHeader header : crtRequest.getHeaders()) {
            result.add(header.getName());
        }
        return result;
    }
}
