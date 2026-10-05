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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

/** Shared fixtures of the transfer manager suites. */
final class TransferTestSupport {
    static final Duration TIMEOUT = Duration.ofSeconds(30);
    static final int PART = 64 * 1024;

    private TransferTestSupport() {
    }

    /** A transfer manager over {@code s3} with small parts, so multipart paths run on small objects. */
    static S3TransferManager transferManager(FakeS3 s3, int maxInFlightParts) {
        return S3TransferManager.builder()
                                .s3Client(s3.client())
                                .multipartThresholdInBytes((long) PART)
                                .minimumPartSizeInBytes((long) PART)
                                .maxInFlightParts(maxInFlightParts)
                                .build();
    }

    static byte[] randomBytes(int size, long seed) {
        byte[] data = new byte[size];
        new Random(seed).nextBytes(data);
        return data;
    }

    static <T> T join(CompletableFuture<T> future) {
        try {
            return future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new AssertionError("Transfer did not complete normally: " + e, e);
        }
    }

    static Throwable failureOf(CompletableFuture<?> future) {
        try {
            future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            return e.getCause();
        } catch (java.util.concurrent.CancellationException e) {
            // get() reports a future failed with a CancellationException as a new one caused by the original.
            return e.getCause() instanceof java.util.concurrent.CancellationException ? e.getCause() : e;
        } catch (Exception e) {
            throw new AssertionError("Transfer did not terminate: " + e, e);
        }
        throw new AssertionError("Transfer completed normally but was expected to fail");
    }

    static void await(BooleanSupplier condition, String description) {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timed out waiting for: " + description);
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }

    /** A publisher of {@code data} in chunks of {@code chunk} bytes, honoring demand, with no declared length. */
    static Publisher<ByteBuffer> publisherOf(byte[] data, int chunk) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private int position;
            private boolean done;
            private long demand;
            private boolean emitting;

            @Override
            public void request(long n) {
                synchronized (this) {
                    demand += n;
                    if (emitting) {
                        return;
                    }
                    emitting = true;
                }
                while (true) {
                    ByteBuffer next;
                    synchronized (this) {
                        if (done || demand == 0) {
                            emitting = false;
                            return;
                        }
                        if (position >= data.length) {
                            done = true;
                            emitting = false;
                            next = null;
                        } else {
                            int size = Math.min(chunk, data.length - position);
                            next = ByteBuffer.wrap(data, position, size).asReadOnlyBuffer();
                            position += size;
                            demand--;
                        }
                    }
                    if (next == null) {
                        subscriber.onComplete();
                        return;
                    }
                    subscriber.onNext(next);
                }
            }

            @Override
            public synchronized void cancel() {
                done = true;
            }
        });
    }

    /** Records every listener callback. */
    static final class RecordingListener implements TransferListener {
        final List<String> events = new CopyOnWriteArrayList<>();
        final List<Long> transferred = new CopyOnWriteArrayList<>();
        volatile Context.TransferComplete complete;
        volatile Context.TransferFailed failed;

        @Override
        public void transferInitiated(Context.TransferInitiated context) {
            events.add("initiated");
        }

        @Override
        public void bytesTransferred(Context.BytesTransferred context) {
            transferred.add(context.progressSnapshot().transferredBytes());
        }

        @Override
        public void transferComplete(Context.TransferComplete context) {
            events.add("complete");
            complete = context;
        }

        @Override
        public void transferFailed(Context.TransferFailed context) {
            events.add("failed");
            failed = context;
        }

        void assertCompletedWith(long totalBytes) {
            await(() -> complete != null, "transferComplete");
            assertThat(events).containsExactly("initiated", "complete");
            assertThat(complete.progressSnapshot().transferredBytes()).isEqualTo(totalBytes);
            assertThat(complete.progressSnapshot().ratioTransferred()).hasValue(1.0);
        }
    }
}
