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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.io.BufferedResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 *
 * Response handler for asynchronous non-streaming operations.
 *
 * <p>
 * Adapts an {@link HttpResponseHandler} to the asynchronous {@link TransformingAsyncResponseHandler}. Buffers
 * all content into a {@link ByteArrayInputStream} then invokes the {@link HttpResponseHandler#handle}
 * method.
 *
 * @param <T> Type that the response handler produces.
 */
@SdkInternalApi
public final class AsyncResponseHandler<T> implements TransformingAsyncResponseHandler<T> {
    static final int DEFAULT_INITIAL_CAPACITY = 32;
    static final int MAX_PRESIZED_ALLOCATION = 8 * 1024 * 1024;

    private volatile CompletableFuture<BufferedResponseBody> streamFuture;
    private final HttpResponseHandler<T> responseHandler;
    private final ExecutionAttributes executionAttributes;
    private final Function<SdkHttpFullResponse, SdkHttpFullResponse> crc32Validator;
    private SdkHttpFullResponse.Builder httpResponse;
    private volatile int initialBufferCapacity = DEFAULT_INITIAL_CAPACITY;
    private volatile ResponseBodySubscriber activeSubscriber;

    public AsyncResponseHandler(HttpResponseHandler<T> responseHandler,
                                Function<SdkHttpFullResponse, SdkHttpFullResponse> crc32Validator,
                                ExecutionAttributes executionAttributes) {
        this.responseHandler = responseHandler;
        this.executionAttributes = executionAttributes;
        this.crc32Validator = crc32Validator;
    }

    @Override
    public void onHeaders(SdkHttpResponse response) {
        this.httpResponse = ((SdkHttpFullResponse) response).toBuilder();
        this.initialBufferCapacity = initialBufferCapacity(response);
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        ResponseBodySubscriber subscriber = new ResponseBodySubscriber(streamFuture, initialBufferCapacity);
        activeSubscriber = subscriber;
        publisher.subscribe(subscriber);
    }

    @Override
    public void onError(Throwable err) {
        ResponseBodySubscriber subscriber = activeSubscriber;
        if (subscriber != null) {
            subscriber.cancel();
        }
        if (streamFuture == null) {
            prepare();
        }
        streamFuture.completeExceptionally(err);
    }

    @Override
    public CompletableFuture<T> prepare() {
        activeSubscriber = null;
        streamFuture = new CompletableFuture<>();
        return streamFuture.thenCompose(body -> {
            if (body != null) {
                // Ignore aborts - we already have all of the content. The input stream reads the accumulator's
                // owned backing array directly; the array is never modified after the subscriber completes.
                httpResponse.content(AbortableInputStream.create(body.inputStream()));
            }

            try {
                return CompletableFuture.completedFuture(responseHandler.handle(crc32Validator.apply(httpResponse.build()),
                                                                                executionAttributes));
            } catch (Exception e) {
                return CompletableFutureUtils.failedFuture(e);
            }
        });
    }

    static int initialBufferCapacity(SdkHttpResponse response) {
        return response.firstMatchingHeader("Content-Length")
                       .map(AsyncResponseHandler::parseContentLengthHint)
                       .orElse(DEFAULT_INITIAL_CAPACITY);
    }

    private static int parseContentLengthHint(String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed >= 0 && parsed <= MAX_PRESIZED_ALLOCATION) {
                return (int) parsed;
            }
        } catch (NumberFormatException ignored) {
            // Treat malformed Content-Length as an unusable sizing hint. The transport is responsible for wire validation.
        }
        return DEFAULT_INITIAL_CAPACITY;
    }

    static class ResponseBodySubscriber implements Subscriber<ByteBuffer> {
        private final CompletableFuture<BufferedResponseBody> streamFuture;
        private BufferedResponseBody body;
        private Subscription subscription;
        private boolean dataWritten;

        ResponseBodySubscriber(CompletableFuture<BufferedResponseBody> streamFuture, int initialBufferCapacity) {
            this.streamFuture = streamFuture;
            this.body = new BufferedResponseBody(initialBufferCapacity);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (this.subscription != null) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
            if (streamFuture.isDone()) {
                cancel();
                return;
            }
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            Objects.requireNonNull(byteBuffer, "byteBuffer");
            if (streamFuture.isDone()) {
                return;
            }
            dataWritten = true;
            try {
                body.write(byteBuffer);
            } catch (RuntimeException e) {
                body = null;
                streamFuture.completeExceptionally(e);
                if (subscription != null) {
                    subscription.cancel();
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            body = null;
            streamFuture.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            if (!streamFuture.isDone()) {
                streamFuture.complete(dataWritten ? body : null);
            }
        }

        void cancel() {
            body = null;
            if (subscription != null) {
                subscription.cancel();
            }
        }
    }

    static final class BufferedResponseBody {
        private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
        private static final byte[] EMPTY = new byte[0];

        private final int initialCapacity;
        private byte[] bytes;
        private int count;

        BufferedResponseBody(int initialCapacity) {
            this.initialCapacity = Math.max(0, initialCapacity);
        }

        void write(ByteBuffer source) {
            ByteBuffer duplicate = source.duplicate();
            int length = duplicate.remaining();
            ensureCapacity(length);
            duplicate.get(bytes, count, length);
            count += length;
        }

        BufferedResponseInputStream inputStream() {
            return new BufferedResponseInputStream(bytes == null ? EMPTY : bytes, 0, count);
        }

        int capacity() {
            return bytes == null ? 0 : bytes.length;
        }

        int size() {
            return count;
        }

        private void ensureCapacity(int additionalBytes) {
            long requiredLong = (long) count + additionalBytes;
            if (requiredLong > MAX_ARRAY_SIZE) {
                throw new IllegalStateException("Async response body exceeds the maximum supported in-memory size.");
            }
            int required = (int) requiredLong;
            if (bytes == null) {
                bytes = new byte[Math.max(initialCapacity, required)];
                return;
            }
            if (required <= bytes.length) {
                return;
            }

            int doubled = bytes.length <= MAX_ARRAY_SIZE / 2 ? bytes.length * 2 : MAX_ARRAY_SIZE;
            int newCapacity = Math.max(required, Math.max(DEFAULT_INITIAL_CAPACITY, doubled));
            bytes = Arrays.copyOf(bytes, newCapacity);
        }
    }
}
