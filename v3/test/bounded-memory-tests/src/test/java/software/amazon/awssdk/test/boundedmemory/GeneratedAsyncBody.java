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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
/**
 * A replayable async body of the first {@code length} {@link Payload} bytes, generated in 64 KiB chunks strictly under
 * demand. Demand, produced bytes and cancellation are recorded so the test can prove the pipeline reads the body
 * incrementally and stops reading when the call is abandoned.
 */
final class GeneratedAsyncBody implements AsyncRequestBody {
    private static final int CHUNK = 64 * 1024;
    private final long length;
    private final InFlightGauge gauge;
    private final CountDownLatch cancelled = new CountDownLatch(1);
    GeneratedAsyncBody(long length, InFlightGauge gauge) {
        this.length = length;
        this.gauge = gauge;
    }
    @Override
    public Optional<Long> contentLength() {
        return Optional.of(length);
    }
    @Override
    public boolean isReplayable() {
        return true;
    }
    int subscriptions() {
        return gauge.passes();
    }
    CountDownLatch cancelled() {
        return cancelled;
    }
    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        gauge.newPass();
        subscriber.onSubscribe(new Subscription() {
            private long position;
            private long demand;
            private boolean emitting;
            private boolean done;
            @Override
            public void request(long n) {
                if (n <= 0) {
                    synchronized (this) {
                        done = true;
                    }
                    subscriber.onError(new IllegalArgumentException("Rule 3.9: non-positive request " + n));
                    return;
                }
                synchronized (this) {
                    if (done) {
                        return;
                    }
                    demand = demand + n < 0 ? Long.MAX_VALUE : demand + n;
                    gauge.requested(n);
                    if (emitting) {
                        return;
                    }
                    emitting = true;
                }
                // Trampolined: chunks requested from onNext are emitted by this loop, not by recursion.
                while (true) {
                    ByteBuffer chunk;
                    synchronized (this) {
                        if (done || demand == 0) {
                            emitting = false;
                            return;
                        }
                        if (position >= length) {
                            done = true;
                            emitting = false;
                            chunk = null;
                        } else {
                            int size = (int) Math.min(CHUNK, length - position);
                            byte[] bytes = new byte[size];
                            Payload.fill(position, bytes, 0, size);
                            position += size;
                            demand--;
                            gauge.delivered();
                            gauge.produced(size);
                            chunk = ByteBuffer.wrap(bytes);
                        }
                    }
                    if (chunk == null) {
                        subscriber.onComplete();
                        return;
                    }
                    subscriber.onNext(chunk);
                }
            }
            @Override
            public void cancel() {
                synchronized (this) {
                    done = true;
                }
                cancelled.countDown();
            }
        });
    }
}
