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
package software.amazon.awssdk.test.boundedmemory;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
/**
 * An async response transformer that verifies the streamed payload as it arrives ({@link Payload.Verifier}) with a
 * demand of one chunk at a time, recording demand and consumed bytes in a gauge. With {@code cancelAfter} smaller
 * than the payload it cancels its subscription mid-stream and completes with the bytes it read.
 *
 * @param <ResponseT> the response type
 */
final class VerifyingTransformer<ResponseT> implements AsyncResponseTransformer<ResponseT, Payload.Verifier> {
    private final InFlightGauge gauge;
    private final long cancelAfter;
    private volatile CompletableFuture<Payload.Verifier> result;
    private volatile Payload.Verifier verifier;
    VerifyingTransformer(InFlightGauge gauge, long cancelAfter) {
        this.gauge = gauge;
        this.cancelAfter = cancelAfter;
    }
    @Override
    public CompletableFuture<Payload.Verifier> prepare() {
        verifier = new Payload.Verifier();
        result = new CompletableFuture<>();
        return result;
    }
    @Override
    public void onResponse(ResponseT response) {
    }
    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        Payload.Verifier v = verifier;
        CompletableFuture<Payload.Verifier> future = result;
        publisher.subscribe(new Subscriber<ByteBuffer>() {
            private Subscription subscription;
            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                more();
            }
            @Override
            public void onNext(ByteBuffer buffer) {
                gauge.delivered();
                gauge.consumed(buffer.remaining());
                try {
                    v.update(buffer);
                } catch (AssertionError e) {
                    subscription.cancel();
                    future.completeExceptionally(e);
                    return;
                }
                if (v.position() >= cancelAfter) {
                    subscription.cancel();
                    future.complete(v);
                    return;
                }
                more();
            }
            private void more() {
                gauge.requested(1);
                subscription.request(1);
            }
            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }
            @Override
            public void onComplete() {
                future.complete(v);
            }
        });
    }
    @Override
    public void exceptionOccurred(Throwable error) {
        CompletableFuture<Payload.Verifier> future = result;
        if (future != null) {
            future.completeExceptionally(error);
        }
    }
}
