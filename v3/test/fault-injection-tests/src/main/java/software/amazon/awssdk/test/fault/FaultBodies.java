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

package software.amazon.awssdk.test.fault;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.ContentStreamProvider;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * Streaming request bodies of each {@link BodyKind}, sync ({@link RequestBody}) and async ({@link AsyncRequestBody}),
 * that record every stream or subscription the pipeline and transport open and how each one ended, so a test can
 * check that none is left open ({@link #leaks()}). The async bodies honour demand, delivering
 * {@link #CHUNK_SIZE}-byte chunks. One instance describes one call's body. Thread-safe.
 */
public final class FaultBodies {
    /** The body's content: {@value #LENGTH} ASCII bytes. */
    public static final byte[] CONTENT = "fault-injection-request-body-0123456789-abcdefghijklmnopqrstuvwxyz!"
        .substring(0, 64).getBytes(StandardCharsets.US_ASCII);
    /** The body's length. */
    public static final int LENGTH = 64;
    /** The async bodies' chunk size. */
    public static final int CHUNK_SIZE = 8;
    /** How many bytes the first stream of a {@link BodyKind#FAILING_ONCE} body delivers before failing. */
    public static final int FAIL_AFTER = 20;

    private final BodyKind kind;
    private final AtomicInteger opened = new AtomicInteger();
    private final AtomicInteger ended = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();
    private final List<Tracked> streams = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** The bodies of {@code kind}; {@link BodyKind#NONE} makes {@link #sync()} and {@link #async()} return null. */
    public FaultBodies(BodyKind kind) {
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public BodyKind kind() {
        return kind;
    }

    /** The sync body, or {@code null} for {@link BodyKind#NONE}. */
    public RequestBody sync() {
        return switch (kind) {
            case NONE -> null;
            case REPLAYABLE, FAILING_ONCE -> RequestBody.fromContentProvider(new ContentStreamProvider() {
                @Override
                public InputStream newStream() {
                    return open(kind == BodyKind.FAILING_ONCE && opened.get() == 0);
                }

                @Override
                public String name() {
                    return "FaultBodies(" + kind + ")";
                }
            }, LENGTH, "application/octet-stream");
            case ONE_SHOT -> RequestBody.fromInputStream(open(false), LENGTH);
        };
    }

    /** The async body, or {@code null} for {@link BodyKind#NONE}. */
    public AsyncRequestBody async() {
        if (kind == BodyKind.NONE) {
            return null;
        }
        return new AsyncRequestBody() {
            private final AtomicBoolean subscribed = new AtomicBoolean();

            @Override
            public Optional<Long> contentLength() {
                return Optional.of((long) LENGTH);
            }

            @Override
            public boolean isReplayable() {
                return kind != BodyKind.ONE_SHOT;
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                Objects.requireNonNull(subscriber, "subscriber");
                if (kind == BodyKind.ONE_SHOT && !subscribed.compareAndSet(false, true)) {
                    failures.incrementAndGet();
                    subscriber.onSubscribe(new Subscription() {
                        @Override
                        public void request(long n) {
                        }

                        @Override
                        public void cancel() {
                        }
                    });
                    subscriber.onError(new IllegalStateException("A one-shot body can be subscribed to only once"));
                    return;
                }
                boolean fail = kind == BodyKind.FAILING_ONCE && opened.get() == 0;
                new ChunkSubscription(subscriber, fail).start();
            }

            @Override
            public String toString() {
                return "FaultBodies(" + kind + ")";
            }
        };
    }

    /** How many streams or subscriptions were opened. */
    public int opened() {
        return opened.get();
    }

    /** How many streams or subscriptions failed (including a refused re-subscription of a one-shot body). */
    public int failures() {
        return failures.get();
    }

    /**
     * Streams or subscriptions that are still open: neither read to their end, failed, closed nor cancelled. The sync
     * one-shot body's stream is caller-owned (the SDK does not close it), so only its reads count.
     */
    public List<String> leaks() {
        List<String> leaks = new ArrayList<>();
        for (Tracked tracked : streams) {
            if (!tracked.ended()) {
                leaks.add("request body " + tracked + " left open");
            }
        }
        return leaks;
    }

    private InputStream open(boolean failing) {
        opened.incrementAndGet();
        TrackedStream stream = new TrackedStream(failing);
        streams.add(stream);
        return stream;
    }

    private interface Tracked {
        boolean ended();
    }

    private final class TrackedStream extends InputStream implements Tracked {
        private final ByteArrayInputStream in = new ByteArrayInputStream(CONTENT);
        private final boolean failing;
        private final int index = opened.get();
        private final AtomicBoolean done = new AtomicBoolean();
        private int read;

        private TrackedStream(boolean failing) {
            this.failing = failing;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (failing && read >= FAIL_AFTER) {
                end(true);
                throw new IOException("Injected request body failure after " + read + " bytes");
            }
            int max = failing ? Math.min(length, FAIL_AFTER - read) : length;
            int n = in.read(buffer, offset, max);
            if (n < 0) {
                // A stream read to its end has ended, even if the reader never closes it (a one-shot body).
                end(false);
                return -1;
            }
            read += n;
            return n;
        }

        @Override
        public void close() {
            end(false);
        }

        private void end(boolean failure) {
            if (done.compareAndSet(false, true)) {
                ended.incrementAndGet();
                if (failure) {
                    failures.incrementAndGet();
                }
            }
        }

        @Override
        public boolean ended() {
            // A one-shot stream is caller-owned and never closed by the SDK: only an unread one is a leak.
            return done.get() || kind == BodyKind.ONE_SHOT;
        }

        @Override
        public String toString() {
            return "stream#" + index + "(" + read + " bytes read)";
        }
    }

    /** One subscription to an async body: {@link #CHUNK_SIZE}-byte chunks on demand. */
    private final class ChunkSubscription implements Subscription, Tracked {
        private final Subscriber<? super ByteBuffer> subscriber;
        private final boolean failing;
        private final int index;
        private final AtomicBoolean done = new AtomicBoolean();
        private long demand;
        private int position;
        private boolean emitting;

        private ChunkSubscription(Subscriber<? super ByteBuffer> subscriber, boolean failing) {
            this.subscriber = subscriber;
            this.failing = failing;
            this.index = opened.incrementAndGet();
            streams.add(this);
        }

        void start() {
            subscriber.onSubscribe(this);
        }

        @Override
        public void request(long n) {
            if (done.get()) {
                return;
            }
            if (n <= 0) {
                finish(new IllegalArgumentException("Reactive Streams 3.9: demand " + n));
                return;
            }
            synchronized (this) {
                demand = demand + n < 0 ? Long.MAX_VALUE : demand + n;
                if (emitting) {
                    return;
                }
                emitting = true;
            }
            // A trampoline: a request(n) from within onNext only adds demand, so the stack stays flat.
            while (true) {
                ByteBuffer chunk;
                synchronized (this) {
                    if (done.get() || demand == 0) {
                        emitting = false;
                        return;
                    }
                    if (failing && position >= FAIL_AFTER) {
                        emitting = false;
                        chunk = null;
                    } else if (position >= LENGTH) {
                        emitting = false;
                        chunk = null;
                    } else {
                        int limit = failing ? FAIL_AFTER : LENGTH;
                        int size = Math.min(CHUNK_SIZE, limit - position);
                        chunk = ByteBuffer.wrap(CONTENT, position, size).slice().asReadOnlyBuffer();
                        position += size;
                        demand--;
                    }
                }
                if (chunk == null) {
                    finish(failing ? new IOException("Injected request body failure after " + position + " bytes")
                                   : null);
                    return;
                }
                subscriber.onNext(chunk);
            }
        }

        @Override
        public void cancel() {
            done.compareAndSet(false, true);
        }

        private void finish(Throwable failure) {
            if (!done.compareAndSet(false, true)) {
                return;
            }
            ended.incrementAndGet();
            if (failure == null) {
                subscriber.onComplete();
            } else {
                failures.incrementAndGet();
                subscriber.onError(failure);
            }
        }

        @Override
        public boolean ended() {
            return done.get();
        }

        @Override
        public String toString() {
            return "subscription#" + index + "(" + position + " bytes sent)";
        }
    }
}
