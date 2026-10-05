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

package software.amazon.awssdk.http.crt.internal;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * The async response body: a single-subscriber Reactive Streams publisher of read-only chunks fed by the CRT event
 * loop ({@link #onData}, {@link #onComplete}, {@link #onError}).
 *
 * <p>Demand and buffering: a chunk is delivered only against outstanding demand, never more than requested. The CRT
 * stream uses manual window management; the {@link Source} window is reopened by a chunk's size only when that chunk
 * is delivered, so bytes queued here never exceed the initial window ({@code readBufferSizeInBytes}). Signals are
 * serialized by a work-in-progress counter, so they are never concurrent, even when {@code request} is called from
 * {@code onNext}.
 *
 * <p>Terminal signals: completion and failure go through {@link Source#terminate}, which lets the exchange decide
 * whether this terminal wins (the exchange's completion is terminal exactly once). Cancelling the subscription calls
 * {@link Source#cancelled()}, which discards the connection. A second subscriber receives {@code onError}.
 */
public final class CrtResponsePublisher implements Publisher<ByteBuffer> {
    /** The native side the publisher controls. */
    public interface Source {
        /** Reopens the read window by {@code bytes} (a delivered chunk). */
        void incrementWindow(int bytes);

        /**
         * The body ends: {@code failure} null for success. Returns the throwable to signal, or null to signal
         * {@code onComplete}; returns {@link #SUPPRESS} if another terminal already won.
         */
        Throwable terminate(Throwable failure);

        /** The subscriber cancelled (or violated rule 3.9); discard the connection. */
        void cancelled(Throwable ruleViolation);
    }

    /** Returned by {@link Source#terminate} when nothing must be signalled. */
    public static final Throwable SUPPRESS = new Throwable("suppressed", null, false, false) {
        private static final long serialVersionUID = 1L;
    };

    private final Source source;
    private final AtomicInteger wip = new AtomicInteger();
    private final ArrayDeque<byte[]> queue = new ArrayDeque<>();
    private Subscriber<? super ByteBuffer> subscriber;
    private boolean subscribed;
    private long demand;
    private boolean upstreamDone;
    private Throwable upstreamFailure;
    private boolean forcedFailure;
    // A terminal of the body lost to a cancellation of the exchange; only the cancellation's failure may follow.
    private boolean awaitingCancellation;
    private Throwable ruleViolation;
    private boolean terminated;
    private boolean cancelled;
    private long queuedBytes;
    private long maxQueuedBytes;

    public CrtResponsePublisher(Source source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        Objects.requireNonNull(s, "subscriber");
        boolean first;
        synchronized (this) {
            first = !subscribed;
            if (first) {
                subscribed = true;
                subscriber = s;
            }
        }
        if (!first) {
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            s.onError(new IllegalStateException("The response body publisher accepts exactly one subscriber"));
            return;
        }
        s.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                synchronized (CrtResponsePublisher.this) {
                    if (terminated || cancelled) {
                        return;
                    }
                    if (n <= 0) {
                        ruleViolation = new IllegalArgumentException("Non-positive request (rule 3.9): " + n);
                    } else {
                        demand = demand + n < 0 ? Long.MAX_VALUE : demand + n;
                    }
                }
                drain();
            }

            @Override
            public void cancel() {
                boolean first;
                synchronized (CrtResponsePublisher.this) {
                    first = !cancelled && !terminated;
                    cancelled = true;
                    subscriber = null;
                    queue.clear();
                    queuedBytes = 0;
                }
                if (first) {
                    source.cancelled(null);
                }
            }
        });
        drain();
    }

    /** A body chunk from the event loop. */
    public void onData(byte[] chunk) {
        synchronized (this) {
            if (terminated || cancelled || chunk.length == 0) {
                return;
            }
            queue.add(chunk);
            queuedBytes += chunk.length;
            maxQueuedBytes = Math.max(maxQueuedBytes, queuedBytes);
        }
        drain();
    }

    /** The body ended successfully. */
    public void onComplete() {
        synchronized (this) {
            upstreamDone = true;
        }
        drain();
    }

    /** The body failed (a transport failure or cancellation of the exchange); delivered ahead of queued chunks. */
    public void onError(Throwable failure) {
        synchronized (this) {
            if (upstreamFailure == null) {
                upstreamFailure = failure;
            }
            queue.clear();
            queuedBytes = 0;
        }
        drain();
    }

    /**
     * The exchange was cancelled after the body was handed out: {@code failure} is signalled to the subscriber
     * (without asking {@link Source#terminate}, since the cancellation already terminated the exchange).
     */
    public void onCancelled(Throwable failure) {
        synchronized (this) {
            if (!forcedFailure) {
                // The cancellation won the exchange, so it replaces any pending body failure.
                upstreamFailure = failure;
                forcedFailure = true;
            }
            queue.clear();
            queuedBytes = 0;
        }
        drain();
    }

    /** Test view: the most bytes ever queued ahead of demand. */
    public synchronized long maxQueuedBytes() {
        return maxQueuedBytes;
    }

    /**
     * The body's own terminal lost to another terminal of the exchange (a cancellation racing the end of the body):
     * keep the subscriber attached so the cancellation's failure, delivered through {@link #onCancelled}, is its one
     * terminal signal.
     */
    private synchronized void lostToCancellation(Subscriber<? super ByteBuffer> s) {
        if (cancelled) {
            return;
        }
        terminated = false;
        subscriber = s;
        awaitingCancellation = true;
        if (!forcedFailure) {
            upstreamFailure = null;
        }
    }

    private void drain() {
        if (wip.getAndIncrement() != 0) {
            return;
        }
        int missed = 1;
        while (true) {
            while (true) {
                Subscriber<? super ByteBuffer> s;
                byte[] chunk = null;
                Throwable violation = null;
                Throwable failure = null;
                boolean forced = false;
                boolean complete = false;
                synchronized (this) {
                    s = subscriber;
                    if (s == null || terminated || cancelled) {
                        break;
                    }
                    if (ruleViolation != null) {
                        violation = ruleViolation;
                        cancelled = true;
                        subscriber = null;
                        queue.clear();
                    } else if (upstreamFailure != null && (forcedFailure || !awaitingCancellation)) {
                        failure = upstreamFailure;
                        forced = forcedFailure;
                        terminated = true;
                        subscriber = null;
                    } else if (demand > 0 && !queue.isEmpty()) {
                        chunk = queue.poll();
                        queuedBytes -= chunk.length;
                        demand--;
                    } else if (!awaitingCancellation && queue.isEmpty() && upstreamDone) {
                        complete = true;
                        terminated = true;
                        subscriber = null;
                    } else {
                        break;
                    }
                }
                if (violation != null) {
                    source.cancelled(violation);
                    s.onError(violation);
                    break;
                }
                if (failure != null) {
                    Throwable signal = forced ? failure : source.terminate(failure);
                    if (signal == SUPPRESS) {
                        lostToCancellation(s);
                        continue;
                    }
                    s.onError(signal == null ? failure : signal);
                    break;
                }
                if (complete) {
                    Throwable signal = source.terminate(null);
                    if (signal == SUPPRESS) {
                        lostToCancellation(s);
                        continue;
                    }
                    if (signal == null) {
                        s.onComplete();
                    } else {
                        s.onError(signal);
                    }
                    break;
                }
                s.onNext(ByteBuffer.wrap(chunk).asReadOnlyBuffer());
                source.incrementWindow(chunk.length);
            }
            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                return;
            }
        }
    }
}
