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

package software.amazon.awssdk.core.internal.async;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;

/**
 * {@link AsyncResponseTransformer#toBytes()}: collects the payload with one chunk of demand at a time.
 *
 * @param <ResponseT> the unmarshalled response type
 */
public final class ByteArrayAsyncResponseTransformer<ResponseT>
    implements AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> {
    private volatile CompletableFuture<ResponseBytes<ResponseT>> result;
    private volatile ResponseT response;

    @Override
    public CompletableFuture<ResponseBytes<ResponseT>> prepare() {
        response = null;
        CompletableFuture<ResponseBytes<ResponseT>> future = new CompletableFuture<>();
        result = future;
        return future;
    }

    @Override
    public void onResponse(ResponseT response) {
        this.response = response;
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        publisher.subscribe(new CollectingSubscriber<>(result, response));
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        CompletableFuture<ResponseBytes<ResponseT>> future = result;
        if (future != null) {
            future.completeExceptionally(error);
        }
    }

    @Override
    public String name() {
        return "BYTES";
    }

    /** Accumulates every chunk, requesting the next one only after the previous is copied. */
    static final class CollectingSubscriber<ResponseT> implements Subscriber<ByteBuffer> {
        private final CompletableFuture<ResponseBytes<ResponseT>> future;
        private final ResponseT response;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private Subscription subscription;

        CollectingSubscriber(CompletableFuture<ResponseBytes<ResponseT>> future, ResponseT response) {
            this.future = Objects.requireNonNull(future, "prepare() was not called");
            this.response = response;
        }

        @Override
        public void onSubscribe(Subscription s) {
            Objects.requireNonNull(s, "subscription");
            if (subscription != null) {
                s.cancel();
                return;
            }
            subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            Objects.requireNonNull(chunk, "chunk");
            ByteBuffer view = chunk.duplicate();
            byte[] copy = new byte[view.remaining()];
            view.get(copy);
            bytes.writeBytes(copy);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
            future.completeExceptionally(Objects.requireNonNull(t, "throwable"));
        }

        @Override
        public void onComplete() {
            if (response == null) {
                future.completeExceptionally(new IllegalStateException("onStream was called before onResponse"));
            } else {
                future.complete(ResponseBytes.fromByteArrayUnsafe(response, bytes.toByteArray()));
            }
        }
    }
}
