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

package software.amazon.awssdk.core.internal.http.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.http.Crc32Validation;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.io.BufferedResponseInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

class AsyncResponseHandlerTest {

    @Test
    void onStream_withMultipleBufferKinds_preservesBytesAndSourceState() {
        ByteBuffer heapSource = ByteBuffer.wrap("xx-heap-yy".getBytes(StandardCharsets.UTF_8));
        heapSource.position(2);
        heapSource.limit(8);
        ByteBuffer heap = heapSource.slice();
        heap.position(1);
        heap.limit(5);
        ByteBuffer direct = ByteBuffer.allocateDirect(8);
        direct.put("direct".getBytes(StandardCharsets.UTF_8)).flip();
        direct.position(1);
        ByteBuffer readOnly = ByteBuffer.wrap("readonly".getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
        readOnly.position(2);

        int heapPosition = heap.position();
        int heapLimit = heap.limit();
        int directPosition = direct.position();
        int readOnlyPosition = readOnly.position();

        AsyncResponseHandler<byte[]> handler = newHandler(readBody());
        CompletableFuture<byte[]> result = handler.prepare();
        handler.onHeaders(responseWithLength(4 + 5 + 6));
        handler.onStream(Flowable.just(heap, direct, readOnly));

        assertThat(result.join()).isEqualTo("heapirectadonly".getBytes(StandardCharsets.UTF_8));
        assertThat(heap.position()).isEqualTo(heapPosition);
        assertThat(heap.limit()).isEqualTo(heapLimit);
        assertThat(direct.position()).isEqualTo(directPosition);
        assertThat(readOnly.position()).isEqualTo(readOnlyPosition);
    }

    @Test
    void onStream_exposesOwnedBufferThroughInternalStream() {
        AsyncResponseHandler<Boolean> handler = newHandler((response, ignored) ->
            response.content().get().delegate() instanceof BufferedResponseInputStream);
        CompletableFuture<Boolean> result = handler.prepare();
        handler.onHeaders(responseWithLength(3));

        handler.onStream(Flowable.just(ByteBuffer.wrap(new byte[] {1, 2, 3})));

        assertThat(result.join()).isTrue();
    }

    @Test
    void onStream_whenDeclaredLengthIsTooSmall_growsWithoutTruncating() {
        AsyncResponseHandler<byte[]> handler = newHandler(readBody());
        CompletableFuture<byte[]> result = handler.prepare();
        handler.onHeaders(responseWithLength(1));
        handler.onStream(Flowable.just(ByteBuffer.wrap("content".getBytes(StandardCharsets.UTF_8))));

        assertThat(result.join()).isEqualTo("content".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void onStream_whenDeclaredLengthIsTooLarge_exposesOnlyReceivedBytes() {
        AsyncResponseHandler<byte[]> handler = newHandler(readBody());
        CompletableFuture<byte[]> result = handler.prepare();
        handler.onHeaders(responseWithLength(100));
        handler.onStream(Flowable.just(ByteBuffer.wrap("abc".getBytes(StandardCharsets.UTF_8))));

        assertThat(result.join()).isEqualTo("abc".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void onStream_withoutChunks_leavesContentAbsent() {
        AsyncResponseHandler<Boolean> handler = newHandler((response, ignored) -> response.content().isPresent());
        CompletableFuture<Boolean> result = handler.prepare();
        handler.onHeaders(responseWithLength(0));
        handler.onStream(Flowable.empty());

        assertThat(result.join()).isFalse();
    }

    @Test
    void onStream_withEmptyChunk_preservesPresentEmptyContent() {
        AsyncResponseHandler<Boolean> handler = newHandler((response, ignored) -> response.content().isPresent());
        CompletableFuture<Boolean> result = handler.prepare();
        handler.onHeaders(responseWithLength(0));
        handler.onStream(Flowable.just(ByteBuffer.allocate(0)));

        assertThat(result.join()).isTrue();
    }

    @Test
    void prepare_onConsecutiveAttempts_usesIndependentBodies() {
        AsyncResponseHandler<byte[]> handler = newHandler(readBody());

        CompletableFuture<byte[]> first = handler.prepare();
        handler.onHeaders(responseWithLength(5));
        handler.onStream(Flowable.just(ByteBuffer.wrap("first".getBytes(StandardCharsets.UTF_8))));

        CompletableFuture<byte[]> second = handler.prepare();
        handler.onHeaders(responseWithLength(6));
        handler.onStream(Flowable.just(ByteBuffer.wrap("second".getBytes(StandardCharsets.UTF_8))));

        assertThat(first.join()).isEqualTo("first".getBytes(StandardCharsets.UTF_8));
        assertThat(second.join()).isEqualTo("second".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void onError_completesPreparedFutureExceptionally() {
        AsyncResponseHandler<byte[]> handler = newHandler(readBody());
        CompletableFuture<byte[]> result = handler.prepare();
        RuntimeException failure = new RuntimeException("failed");

        handler.onError(failure);

        assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).hasCause(failure);
    }

    @Test
    void onStream_whenPublisherFails_completesPreparedFutureExceptionally() {
        AsyncResponseHandler<byte[]> handler = newHandler(readBody());
        CompletableFuture<byte[]> result = handler.prepare();
        RuntimeException failure = new RuntimeException("publisher failed");
        handler.onHeaders(responseWithLength(10));

        handler.onStream(Flowable.error(failure));

        assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).hasCause(failure);
    }

    @Test
    void onError_withActiveSubscription_cancelsUpstream() {
        AsyncResponseHandler<byte[]> handler = newHandler(readBody());
        CompletableFuture<byte[]> result = handler.prepare();
        Subscription subscription = mock(Subscription.class);
        handler.onHeaders(responseWithLength(10));
        handler.onStream(subscriber -> subscriber.onSubscribe(subscription));
        RuntimeException failure = new RuntimeException("request failed");

        handler.onError(failure);

        verify(subscription).request(Long.MAX_VALUE);
        verify(subscription).cancel();
        assertThatThrownBy(result::join).isInstanceOf(CompletionException.class).hasCause(failure);
    }

    @Test
    void onStream_withCrcValidation_readsOwnedBufferToEnd() {
        byte[] content = "crc-content".getBytes(StandardCharsets.UTF_8);
        CRC32 crc32 = new CRC32();
        crc32.update(content);
        AsyncResponseHandler<byte[]> handler =
            new AsyncResponseHandler<>(readBody(), response -> Crc32Validation.validate(false, response),
                                       new ExecutionAttributes());
        CompletableFuture<byte[]> result = handler.prepare();
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                                                          .statusCode(200)
                                                          .putHeader("Content-Length", String.valueOf(content.length))
                                                          .putHeader("x-amz-crc32", String.valueOf(crc32.getValue()))
                                                          .build();
        handler.onHeaders(response);

        handler.onStream(Flowable.just(ByteBuffer.wrap(content)));

        assertThat(result.join()).isEqualTo(content);
    }

    @Test
    void responseBodySubscriber_usesValidContentLengthAsLazyInitialCapacity() {
        CompletableFuture<AsyncResponseHandler.BufferedResponseBody> result = new CompletableFuture<>();
        AsyncResponseHandler.ResponseBodySubscriber subscriber =
            new AsyncResponseHandler.ResponseBodySubscriber(result, 128);
        subscriber.onSubscribe(mock(Subscription.class));

        assertThat(result).isNotDone();
        subscriber.onNext(ByteBuffer.wrap(new byte[] {1}));
        subscriber.onComplete();

        assertThat(result.join().capacity()).isEqualTo(128);
        assertThat(result.join().size()).isOne();
    }

    @Test
    void responseBodySubscriber_duplicateSubscription_cancelsSecondSubscription() {
        CompletableFuture<AsyncResponseHandler.BufferedResponseBody> result = new CompletableFuture<>();
        AsyncResponseHandler.ResponseBodySubscriber subscriber =
            new AsyncResponseHandler.ResponseBodySubscriber(result, 32);
        Subscription first = mock(Subscription.class);
        Subscription second = mock(Subscription.class);

        subscriber.onSubscribe(first);
        subscriber.onSubscribe(second);

        verify(first).request(Long.MAX_VALUE);
        verify(second).cancel();
        verifyNoMoreInteractions(first, second);
    }

    @Test
    void initialBufferCapacity_usesOnlySafeValidHints() {
        assertThat(AsyncResponseHandler.initialBufferCapacity(responseWithHeader("123"))).isEqualTo(123);
        assertThat(AsyncResponseHandler.initialBufferCapacity(responseWithHeader("0"))).isZero();
        assertThat(AsyncResponseHandler.initialBufferCapacity(responseWithHeader("-1")))
            .isEqualTo(AsyncResponseHandler.DEFAULT_INITIAL_CAPACITY);
        assertThat(AsyncResponseHandler.initialBufferCapacity(responseWithHeader("invalid")))
            .isEqualTo(AsyncResponseHandler.DEFAULT_INITIAL_CAPACITY);
        assertThat(AsyncResponseHandler.initialBufferCapacity(
            responseWithHeader(String.valueOf((long) AsyncResponseHandler.MAX_PRESIZED_ALLOCATION + 1))))
            .isEqualTo(AsyncResponseHandler.DEFAULT_INITIAL_CAPACITY);
    }

    private static <T> AsyncResponseHandler<T> newHandler(HttpResponseHandler<T> responseHandler) {
        return new AsyncResponseHandler<>(responseHandler, response -> response, new ExecutionAttributes());
    }

    private static HttpResponseHandler<byte[]> readBody() {
        return (response, ignored) -> {
            if (!response.content().isPresent()) {
                return null;
            }
            try {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[32];
                int read;
                while ((read = response.content().get().read(buffer)) >= 0) {
                    result.write(buffer, 0, read);
                }
                return result.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static SdkHttpFullResponse responseWithLength(int length) {
        return responseWithHeader(String.valueOf(length));
    }

    private static SdkHttpFullResponse responseWithHeader(String length) {
        return SdkHttpFullResponse.builder()
                                  .statusCode(200)
                                  .putHeader("Content-Length", length)
                                  .build();
    }
}
