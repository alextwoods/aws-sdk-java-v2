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
package software.amazon.awssdk.enhanced.dynamodb.internal.async;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.SdkPublisher;

/**
 * The {@code map} and {@code flatMapIterable} operators the V2 enhanced client took from V2's {@code SdkPublisher}
 * default methods, which the V3 {@link SdkPublisher} does not declare. Both honor Reactive Streams demand and
 * cancellation: {@link #map} is one-to-one, so it passes the upstream subscription through, and
 * {@link #flatMapIterable} requests one upstream element at a time, only while downstream demand is outstanding and
 * the previous element's items are exhausted. A mapper or iterator failure cancels the upstream subscription and is
 * signalled as {@code onError}.
 */
public final class SdkPublishers {
    private SdkPublishers() {
    }

    public static <T, U> SdkPublisher<U> map(SdkPublisher<T> source, Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        return subscriber -> source.subscribe(new MappingSubscriber<>(Objects.requireNonNull(subscriber, "subscriber"),
                                                                       mapper));
    }

    public static <T, U> SdkPublisher<U> flatMapIterable(SdkPublisher<T> source,
                                                         Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        return subscriber -> source.subscribe(
            new FlatteningSubscriber<>(Objects.requireNonNull(subscriber, "subscriber"), mapper));
    }

    private static final class MappingSubscriber<T, U> implements Subscriber<T> {
        private final Subscriber<? super U> downstream;
        private final Function<? super T, ? extends U> mapper;
        private Subscription upstream;
        private boolean done;

        MappingSubscriber(Subscriber<? super U> downstream, Function<? super T, ? extends U> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream != null) {
                subscription.cancel();
                return;
            }
            upstream = subscription;
            downstream.onSubscribe(subscription);
        }

        @Override
        public void onNext(T element) {
            if (done) {
                return;
            }
            U mapped;
            try {
                mapped = mapper.apply(element);
            } catch (RuntimeException | Error e) {
                done = true;
                upstream.cancel();
                downstream.onError(e);
                return;
            }
            downstream.onNext(mapped);
        }

        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                downstream.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                downstream.onComplete();
            }
        }
    }

    private static final class FlatteningSubscriber<T, U> implements Subscriber<T>, Subscription {
        private final Subscriber<? super U> downstream;
        private final Function<? super T, ? extends Iterable<? extends U>> mapper;
        private final Queue<T> elements = new ConcurrentLinkedQueue<>();
        private final AtomicLong demand = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        private final AtomicBoolean upstreamRequestOutstanding = new AtomicBoolean();
        private volatile Subscription upstream;
        private volatile boolean upstreamDone;
        private volatile Throwable failure;
        private volatile boolean cancelled;
        // Accessed only by the thread that owns the drain loop.
        private Iterator<? extends U> current;
        private boolean terminated;

        FlatteningSubscriber(Subscriber<? super U> downstream,
                             Function<? super T, ? extends Iterable<? extends U>> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream != null) {
                subscription.cancel();
                return;
            }
            upstream = subscription;
            downstream.onSubscribe(this);
        }

        @Override
        public void onNext(T element) {
            elements.offer(element);
            upstreamRequestOutstanding.set(false);
            drain();
        }

        @Override
        public void onError(Throwable t) {
            failure = t;
            upstreamDone = true;
            drain();
        }

        @Override
        public void onComplete() {
            upstreamDone = true;
            drain();
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                // Reactive Streams rule 3.9.
                cancelUpstream();
                failure = new IllegalArgumentException("Demand must be positive (rule 3.9): " + n);
                upstreamDone = true;
                elements.clear();
                drain();
                return;
            }
            demand.getAndUpdate(d -> d + n < 0 ? Long.MAX_VALUE : d + n);
            drain();
        }

        @Override
        public void cancel() {
            cancelled = true;
            cancelUpstream();
            drain();
        }

        private void cancelUpstream() {
            Subscription s = upstream;
            if (s != null) {
                s.cancel();
            }
        }

        private void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            do {
                drainLoop();
            } while (wip.decrementAndGet() != 0);
        }

        private void drainLoop() {
            while (true) {
                if (terminated) {
                    return;
                }
                if (cancelled) {
                    terminated = true;
                    current = null;
                    elements.clear();
                    return;
                }
                Throwable error = failure;
                if (error != null) {
                    terminate(error);
                    return;
                }
                if (current != null) {
                    boolean hasNext;
                    try {
                        hasNext = current.hasNext();
                    } catch (RuntimeException | Error e) {
                        fail(e);
                        return;
                    }
                    if (hasNext) {
                        if (demand.get() == 0) {
                            return;
                        }
                        U item;
                        try {
                            item = current.next();
                        } catch (RuntimeException | Error e) {
                            fail(e);
                            return;
                        }
                        if (demand.get() != Long.MAX_VALUE) {
                            demand.decrementAndGet();
                        }
                        downstream.onNext(item);
                        continue;
                    }
                    current = null;
                }
                T element = elements.poll();
                if (element != null) {
                    try {
                        Iterable<? extends U> items = mapper.apply(element);
                        current = items == null ? Collections.emptyIterator() : items.iterator();
                    } catch (RuntimeException | Error e) {
                        fail(e);
                        return;
                    }
                    continue;
                }
                if (upstreamDone) {
                    terminate(null);
                    return;
                }
                if (demand.get() > 0 && upstreamRequestOutstanding.compareAndSet(false, true)) {
                    upstream.request(1);
                }
                return;
            }
        }

        private void fail(Throwable t) {
            cancelUpstream();
            terminate(t);
        }

        private void terminate(Throwable error) {
            terminated = true;
            current = null;
            elements.clear();
            if (error == null) {
                downstream.onComplete();
            } else {
                downstream.onError(error);
            }
        }
    }
}
