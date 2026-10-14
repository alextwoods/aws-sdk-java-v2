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

package software.amazon.awssdk.core.internal.http.transport;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.protocol.OwnedResponseBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.Validate;

/**
 * Collects an asynchronous response body into one array, copying each received byte once, and completes
 * {@link #result()} with it as an {@link OwnedResponseBody} (racecar E10, re-implemented).
 *
 * <p>When the expected length is known (from {@code Content-Length}), the array is allocated at that size on the first
 * chunk, so a body of the declared length is copied exactly once and never reallocated. Otherwise, or if more bytes
 * arrive than declared, the array grows by doubling. The array is not trimmed at the end; the owned body records its
 * length.
 *
 * <p>The subscriber requests all elements on subscription, since it keeps every byte anyway. {@link #cancel()} may be
 * called from any thread, before or after subscription; it cancels the subscription and later signals are ignored.
 * {@code onNext}, {@code onError} and {@code onComplete} are serialized by the publisher (Reactive Streams rule 1.3).
 *
 * <p>Used by {@link SdkAsyncHttpClientTransport} for the bodies it buffers, and by the asynchronous request pipeline
 * for a response body that an interceptor replaced in {@code modifyAsyncHttpResponseContent}.
 */
@SdkInternalApi
public class ResponseBodyAggregator implements Subscriber<ByteBuffer> {
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private static final int MIN_UNKNOWN_LENGTH_CAPACITY = 1024;
    private static final byte[] EMPTY = new byte[0];

    private final CompletableFuture<OwnedResponseBody> result = new CompletableFuture<>();
    private final AtomicReference<Subscription> subscription = new AtomicReference<>();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final GrowableArray body;

    /**
     * @param expectedLength the declared body length, or a negative value if it is unknown
     */
    public ResponseBodyAggregator(long expectedLength) {
        this.body = new GrowableArray(expectedLength >= 0 && expectedLength <= MAX_ARRAY_SIZE ? (int) expectedLength : -1);
    }

    /**
     * Completed with the body when the publisher completes, or exceptionally with the publisher's error.
     */
    public CompletableFuture<OwnedResponseBody> result() {
        return result;
    }

    /**
     * Cancels the subscription, now or as soon as it arrives, and ignores any later signal.
     */
    public void cancel() {
        cancelled.set(true);
        Subscription s = subscription.get();
        if (s != null) {
            s.cancel();
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        Validate.paramNotNull(s, "subscription");
        if (!subscription.compareAndSet(null, s)) {
            // Rule 2.5: a subscriber has at most one active subscription.
            s.cancel();
            return;
        }
        if (cancelled.get()) {
            s.cancel();
            return;
        }
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(ByteBuffer chunk) {
        Validate.paramNotNull(chunk, "chunk");
        if (cancelled.get() || result.isDone()) {
            return;
        }
        int length = chunk.remaining();
        if (length == 0) {
            return;
        }
        if (!body.append(chunk)) {
            cancel();
            result.completeExceptionally(SdkClientException.create(
                "The response body is larger than the maximum in-memory size of " + MAX_ARRAY_SIZE + " bytes."));
        }
    }

    @Override
    public void onError(Throwable t) {
        Validate.paramNotNull(t, "throwable");
        result.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        result.complete(body.toOwnedBody());
    }

    /**
     * The received bytes. Only the publisher's serialized {@code onNext}/{@code onComplete} signals touch it, and rule 1.3
     * gives those signals a happens-before order, so it needs no synchronization of its own.
     */
    private static final class GrowableArray {
        private final int expectedLength;
        private byte[] bytes;
        private int count;

        private GrowableArray(int expectedLength) {
            this.expectedLength = expectedLength;
        }

        /**
         * Copies the chunk's remaining bytes, without changing its position.
         *
         * @return false, copying nothing, if the body would exceed {@link #MAX_ARRAY_SIZE}
         */
        private boolean append(ByteBuffer chunk) {
            int length = chunk.remaining();
            if ((long) count + length > MAX_ARRAY_SIZE) {
                return false;
            }
            ensureCapacity(count + length);
            chunk.duplicate().get(bytes, count, length);
            count += length;
            return true;
        }

        private OwnedResponseBody toOwnedBody() {
            return OwnedResponseBody.of(bytes == null ? EMPTY : bytes, count);
        }

        private void ensureCapacity(int required) {
            if (bytes == null) {
                int initial = expectedLength >= 0 ? expectedLength : MIN_UNKNOWN_LENGTH_CAPACITY;
                bytes = new byte[Math.max(initial, required)];
                return;
            }
            if (required <= bytes.length) {
                return;
            }
            int doubled = (int) Math.min((long) bytes.length * 2, MAX_ARRAY_SIZE);
            byte[] grown = new byte[Math.max(doubled, required)];
            System.arraycopy(bytes, 0, grown, 0, count);
            bytes = grown;
        }
    }
}
