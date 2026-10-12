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

package software.amazon.awssdk.http.apache5.internal.impl;

import java.net.URISyntaxException;
import org.apache.hc.core5.http.HttpEntityContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.BufferedHttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache5.internal.Apache5HttpRequestConfig;
import software.amazon.awssdk.http.apache5.internal.RepeatableInputStreamRequestEntity;

class ApacheHttpRequestFactoryTest {

    private Apache5HttpRequestConfig requestConfig;
    private Apache5HttpRequestFactory instance;

    @BeforeEach
    public void setup() {
        instance = new Apache5HttpRequestFactory();
        requestConfig = Apache5HttpRequestConfig.builder()
                                                .connectionAcquireTimeout(Duration.ZERO)
                                                .socketTimeout(Duration.ZERO)
                                                .build();
    }

    @Test
    public void createSetsHostHeaderByDefault() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:12345/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                .request(sdkRequest)
                .build();
        HttpUriRequestBase result = instance.create(request, requestConfig);
        Header[] hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals("localhost:12345", hostHeaders[0].getValue());
    }

    @Test
    public void createRespectsUserHostHeader() {
        String hostOverride = "virtual.host:123";
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
            .uri(URI.create("http://localhost:12345/"))
            .method(SdkHttpMethod.HEAD)
            .putHeader("Host", hostOverride)
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
            .request(sdkRequest)
            .build();

        HttpUriRequestBase result = instance.create(request, requestConfig);

        Header[] hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals(hostOverride, hostHeaders[0].getValue());
    }

    @Test
    public void createRespectsLowercaseUserHostHeader() {
        String hostOverride = "virtual.host:123";
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
            .uri(URI.create("http://localhost:12345/"))
            .method(SdkHttpMethod.HEAD)
            .putHeader("host", hostOverride)
            .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
            .request(sdkRequest)
            .build();

        HttpUriRequestBase result = instance.create(request, requestConfig);

        Header[] hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals(hostOverride, hostHeaders[0].getValue());
    }

    @Test
    public void putRequest_withTransferEncodingChunked_isChunkedAndDoesNotIncludeHeader() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                                                  .uri(URI.create("http://localhost:12345/"))
                                                  .method(SdkHttpMethod.PUT)
                                                  .putHeader("Transfer-Encoding", "chunked")
                                                  .build();
        InputStream inputStream = new ByteArrayInputStream("TestStream".getBytes(StandardCharsets.UTF_8));
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(sdkRequest)
                                                       .contentStreamProvider(() -> inputStream)
                                                       .build();
        HttpUriRequestBase result = instance.create(request, requestConfig);
        Header[] transferEncodingHeaders = result.getHeaders("Transfer-Encoding");
        assertThat(transferEncodingHeaders).isEmpty();

        assertThat(result).isInstanceOf(HttpEntityContainer.class);
        HttpEntity httpEntity = ((HttpEntityContainer) result).getEntity();

        assertThat(httpEntity.isChunked()).isTrue();
        assertThat(httpEntity).isNotInstanceOf(BufferedHttpEntity.class);
        assertThat(httpEntity).isInstanceOf(RepeatableInputStreamRequestEntity.class);
    }

    @Test
    public void bodyWithArrayBackedBuffer_isSentAsByteArrayEntityOverSameArray() throws Exception {
        byte[] bytes = "zero-copy body".getBytes(StandardCharsets.UTF_8);
        HttpExecuteRequest request = postRequest(ContentStreamProvider.fromByteArrayUnsafe(bytes),
                                                 String.valueOf(bytes.length));

        HttpEntity httpEntity = entityOf(instance.create(request, requestConfig));

        assertThat(httpEntity).isInstanceOf(ByteArrayEntity.class);
        assertThat(httpEntity.getContentLength()).isEqualTo(bytes.length);
        assertThat(httpEntity.isChunked()).isFalse();
        assertThat(httpEntity.isRepeatable()).isTrue();
        assertThat(httpEntity.getContentType()).isEqualTo("application/cbor");
        assertThat(written(httpEntity)).isEqualTo(bytes);
        assertThat(written(httpEntity)).isEqualTo(bytes);
        assertWritesFromArray(httpEntity, bytes, 0);
    }

    @Test
    public void bodyWithBufferSlice_sendsOnlyTheSliceOfTheBackingArray() throws Exception {
        byte[] bytes = "zero-copy body".getBytes(StandardCharsets.UTF_8);
        ByteBuffer slice = ByteBuffer.wrap(bytes, 5, 4).slice();
        HttpExecuteRequest request = postRequest(bufferProvider(slice), "4");

        HttpEntity httpEntity = entityOf(instance.create(request, requestConfig));

        assertThat(httpEntity).isInstanceOf(ByteArrayEntity.class);
        assertThat(httpEntity.getContentLength()).isEqualTo(4);
        assertThat(written(httpEntity)).isEqualTo("copy".getBytes(StandardCharsets.UTF_8));
        assertWritesFromArray(httpEntity, bytes, 5);
    }

    @Test
    public void bodyWithoutContentLengthHeader_isSentAsByteArrayEntity() throws Exception {
        byte[] bytes = "zero-copy body".getBytes(StandardCharsets.UTF_8);
        HttpExecuteRequest request = postRequest(ContentStreamProvider.fromByteArrayUnsafe(bytes), null);

        HttpEntity httpEntity = entityOf(instance.create(request, requestConfig));

        assertThat(httpEntity).isInstanceOf(ByteArrayEntity.class);
        assertThat(httpEntity.getContentLength()).isEqualTo(bytes.length);
        assertWritesFromArray(httpEntity, bytes, 0);
    }

    @Test
    public void bodyWithoutBuffer_readOnlyBuffer_mismatchedLengthOrChunked_fallsBackToStreamEntity() {
        byte[] bytes = "zero-copy body".getBytes(StandardCharsets.UTF_8);
        String length = String.valueOf(bytes.length);

        assertThat(entityOf(instance.create(postRequest(() -> new ByteArrayInputStream(bytes), length), requestConfig)))
            .isInstanceOf(RepeatableInputStreamRequestEntity.class);
        assertThat(entityOf(instance.create(postRequest(bufferProvider(ByteBuffer.wrap(bytes).asReadOnlyBuffer()), length),
                                            requestConfig)))
            .isInstanceOf(RepeatableInputStreamRequestEntity.class);
        assertThat(entityOf(instance.create(postRequest(ContentStreamProvider.fromByteArrayUnsafe(bytes), "3"),
                                            requestConfig)))
            .isInstanceOf(RepeatableInputStreamRequestEntity.class);

        SdkHttpRequest chunked = SdkHttpRequest.builder()
                                               .uri(URI.create("http://localhost:12345/"))
                                               .method(SdkHttpMethod.PUT)
                                               .putHeader("Transfer-Encoding", "chunked")
                                               .build();
        HttpExecuteRequest chunkedRequest = HttpExecuteRequest.builder()
                                                              .request(chunked)
                                                              .contentStreamProvider(
                                                                  ContentStreamProvider.fromByteArrayUnsafe(bytes))
                                                              .build();
        assertThat(entityOf(instance.create(chunkedRequest, requestConfig)))
            .isInstanceOf(RepeatableInputStreamRequestEntity.class);
    }

    private static HttpExecuteRequest postRequest(ContentStreamProvider provider, String contentLength) {
        SdkHttpRequest.Builder sdkRequest = SdkHttpRequest.builder()
                                                          .uri(URI.create("http://localhost:12345/"))
                                                          .method(SdkHttpMethod.POST)
                                                          .putHeader("Content-Type", "application/cbor");
        if (contentLength != null) {
            sdkRequest.putHeader("Content-Length", contentLength);
        }
        return HttpExecuteRequest.builder()
                                 .request(sdkRequest.build())
                                 .contentStreamProvider(provider)
                                 .build();
    }

    private static ContentStreamProvider bufferProvider(ByteBuffer buffer) {
        return new ContentStreamProvider() {
            @Override
            public InputStream newStream() {
                ByteBuffer copy = buffer.duplicate();
                byte[] bytes = new byte[copy.remaining()];
                copy.get(bytes);
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public ByteBuffer contentAsByteBufferOrNull() {
                return buffer.duplicate();
            }
        };
    }

    private static HttpEntity entityOf(HttpUriRequestBase request) {
        return ((HttpEntityContainer) request).getEntity();
    }

    private static byte[] written(HttpEntity entity) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        return out.toByteArray();
    }

    /**
     * Checks that the entity writes directly from {@code array} (no intermediate copy) by changing the byte at
     * {@code offset} after the entity was created and observing the change in the written body.
     */
    private static void assertWritesFromArray(HttpEntity entity, byte[] array, int offset) throws Exception {
        byte original = array[offset];
        array[offset] = 'X';
        try {
            assertThat(written(entity)[0]).isEqualTo((byte) 'X');
        } finally {
            array[offset] = original;
        }
    }

    @Test
    public void defaultHttpPortsAreNotInDefaultHostHeader() {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("http://localhost:80/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                .request(sdkRequest)
                .build();
        HttpUriRequestBase result = instance.create(request, requestConfig);
        Header[] hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals("localhost", hostHeaders[0].getValue());

        sdkRequest = SdkHttpRequest.builder()
                .uri(URI.create("https://localhost:443/"))
                .method(SdkHttpMethod.HEAD)
                .build();
        request = HttpExecuteRequest.builder()
                .request(sdkRequest)
                .build();
        result = instance.create(request, requestConfig);
        hostHeaders = result.getHeaders(HttpHeaders.HOST);
        assertNotNull(hostHeaders);
        assertEquals(1, hostHeaders.length);
        assertEquals("localhost", hostHeaders[0].getValue());
    }

    @Test
    public void pathWithLeadingSlash_shouldEncode() {
        assertThat(sanitizedUri("/foobar")).isEqualTo("http://localhost/%2Ffoobar");
    }

    @Test
    public void pathWithOnlySlash_shouldEncode() {
        assertThat(sanitizedUri("/")).isEqualTo("http://localhost/%2F");
    }

    @Test
    public void pathWithoutSlash_shouldReturnSameUri() {
        assertThat(sanitizedUri("path")).isEqualTo("http://localhost/path");
    }

    @Test
    public void pathWithSpecialChars_shouldPreserveEncoding() {
        assertThat(sanitizedUri("/special-chars-%40%24%25")).isEqualTo("http://localhost/%2Fspecial-chars-%40%24%25");
    }

    private String sanitizedUri(String path) {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                                                  .uri(URI.create("http://localhost:80"))
                                                  .encodedPath("/" + path)
                                                  .method(SdkHttpMethod.HEAD)
                                                  .build();
        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(sdkRequest)
                                                       .build();

        try {
            return instance.create(request, requestConfig).getUri().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
