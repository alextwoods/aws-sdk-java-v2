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

package software.amazon.awssdk.protocols.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.transport.OwnedResponseBody;
import software.amazon.awssdk.core.internal.http.transport.SdkAsyncHttpClientTransport;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

class ResponseBodiesTest {

    @Test
    void knownLengthIsReadIntoAnExactSizeArray() {
        byte[] body = bytes(1000);
        ByteBuffer buffer = ResponseBodies.asByteBuffer(new ExecutionAttributes(), withBody(body, "1000"));

        assertThat(buffer.hasArray()).isTrue();
        assertThat(buffer.array()).hasSize(1000);
        assertThat(buffer.arrayOffset() + buffer.position()).isZero();
        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @Test
    void knownLengthIsReadInPieces() {
        byte[] body = bytes(5000);
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                                                          .statusCode(200)
                                                          .putHeader("content-length", "5000")
                                                          .content(AbortableInputStream.create(trickle(body)))
                                                          .build();
        ByteBuffer buffer = ResponseBodies.asByteBuffer(null, response);

        assertThat(buffer.array()).hasSize(5000);
        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @Test
    void bodyShorterThanContentLengthIsReturnedAsRead() {
        byte[] body = bytes(10);
        ByteBuffer buffer = ResponseBodies.asByteBuffer(null, withBody(body, "100"));

        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @Test
    void bodyLongerThanContentLengthIsReadCompletely() {
        byte[] body = bytes(3000);
        ByteBuffer buffer = ResponseBodies.asByteBuffer(null, withBody(body, "100"));

        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 1023, 1024, 1025, 70_000})
    void unknownLengthUsesAGrowableArray(int size) {
        byte[] body = bytes(size);
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                                                          .statusCode(200)
                                                          .content(AbortableInputStream.create(trickle(body)))
                                                          .build();
        ByteBuffer buffer = ResponseBodies.asByteBuffer(null, response);

        assertThat(buffer.hasArray()).isTrue();
        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "-1", "99999999999"})
    void invalidContentLengthIsTreatedAsUnknown(String contentLength) {
        byte[] body = bytes(2000);
        assertThat(toArray(ResponseBodies.asByteBuffer(null, withBody(body, contentLength)))).isEqualTo(body);
    }

    @Test
    void responseWithoutContentIsEmpty() {
        SdkHttpFullResponse response = SdkHttpFullResponse.builder().statusCode(204).putHeader("Content-Length", "10").build();
        assertThat(ResponseBodies.asByteBuffer(new ExecutionAttributes(), response).remaining()).isZero();
    }

    @Test
    void readFailureIsUnchecked() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("reset");
            }
        };
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                                                          .statusCode(200)
                                                          .content(AbortableInputStream.create(failing))
                                                          .build();

        assertThatThrownBy(() -> ResponseBodies.asByteBuffer(null, response))
            .isInstanceOf(UncheckedIOException.class)
            .hasRootCauseMessage("reset");
    }

    @Test
    void ownedBufferFromTheAsyncTransportIsReturnedWithoutCopy() {
        byte[] body = bytes(4096);
        ExecutionAttributes context = new ExecutionAttributes();
        SdkHttpFullResponse response = sendThroughAsyncTransport(context, body);

        ByteBuffer owned = OwnedResponseBody.ownedBuffer(context, response);
        assertThat(owned).isNotNull();

        ByteBuffer buffer = ResponseBodies.asByteBuffer(context, response);
        assertThat(buffer.array()).isSameAs(owned.array());
        assertThat(toArray(buffer)).isEqualTo(body);

        // Without the call's attributes the body is read from the stream into a new array.
        ByteBuffer copied = ResponseBodies.asByteBuffer(null, response);
        assertThat(copied.array()).isNotSameAs(owned.array());
        assertThat(toArray(copied)).isEqualTo(body);
    }

    @Test
    void replacedContentIsReadFromTheStream() {
        byte[] body = bytes(64);
        ExecutionAttributes context = new ExecutionAttributes();
        SdkHttpFullResponse response = sendThroughAsyncTransport(context, body);
        byte[] replacement = bytes(16);
        SdkHttpFullResponse modified = response.toBuilder()
                                               .content(AbortableInputStream.create(new ByteArrayInputStream(replacement)))
                                               .putHeader("Content-Length", "16")
                                               .build();

        assertThat(toArray(ResponseBodies.asByteBuffer(context, modified))).isEqualTo(replacement);
    }

    private static SdkHttpFullResponse sendThroughAsyncTransport(ExecutionAttributes context, byte[] body) {
        SdkAsyncHttpClient client = new SdkAsyncHttpClient() {
            @Override
            public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
                request.responseHandler().onHeaders(SdkHttpResponse.builder()
                                                                   .statusCode(200)
                                                                   .putHeader("Content-Length",
                                                                              String.valueOf(body.length))
                                                                   .build());
                request.responseHandler().onStream(AsyncRequestBody.fromBytes(body));
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void close() {
            }
        };
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.POST)
                                                       .uri(URI.create("https://example.com/"))
                                                       .build();
        try (SdkAsyncHttpClientTransport transport = SdkAsyncHttpClientTransport.create(client, true)) {
            return transport.send(context, request, null).join();
        }
    }

    private static SdkHttpFullResponse withBody(byte[] body, String contentLength) {
        return SdkHttpFullResponse.builder()
                                  .statusCode(200)
                                  .putHeader("Content-Length", contentLength)
                                  .content(AbortableInputStream.create(new ByteArrayInputStream(body)))
                                  .build();
    }

    /**
     * A stream that returns at most 7 bytes per read, to exercise partial reads.
     */
    private static InputStream trickle(byte[] body) {
        return new ByteArrayInputStream(body) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                return super.read(b, off, Math.min(len, 7));
            }
        };
    }

    private static byte[] bytes(int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) (i * 31);
        }
        return bytes;
    }

    private static byte[] toArray(ByteBuffer buffer) {
        return Arrays.copyOfRange(buffer.array(), buffer.arrayOffset() + buffer.position(),
                                  buffer.arrayOffset() + buffer.limit());
    }
}
