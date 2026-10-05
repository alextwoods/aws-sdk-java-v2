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

package software.amazon.awssdk.core.pagination.async;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.SdkPublisher;

/**
 * The demand-driven pages of a paginated operation. Each subscription starts again from the first page and has at
 * most one page call in flight; it calls for a page only while downstream demand is positive, so it never prefetches.
 * Cancelling a subscription cancels its in-flight call and issues no further call.
 *
 * @param <T> the page (response) type
 */
public final class PagePublisher<T> implements SdkPublisher<T> {
    private final AsyncPageFetcher<T> fetcher;

    private PagePublisher(AsyncPageFetcher<T> fetcher) {
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher");
    }

    /** The pages {@code fetcher} produces. */
    public static <T> PagePublisher<T> create(AsyncPageFetcher<T> fetcher) {
        return new PagePublisher<>(fetcher);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        PageSubscription<T> subscription = new PageSubscription<>(fetcher, subscriber);
        subscriber.onSubscribe(subscription);
    }

    /**
     * One subscription. {@link #drain()} is serialized by a work-in-progress counter; the single in-flight page call
     * is held until its page has been delivered, so signals to the subscriber are never concurrent.
     */
    private static final class PageSubscription<T> implements Subscription {
        private final AsyncPageFetcher<T> fetcher;
        private final Subscriber<? super T> subscriber;
        private final AtomicLong demand = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        private final AtomicReference<CompletableFuture<T>> inFlight = new AtomicReference<>();
        private volatile boolean cancelled;
        private volatile boolean terminated;
        private T previous;
        private boolean started;

        PageSubscription(AsyncPageFetcher<T> fetcher, Subscriber<? super T> subscriber) {
            this.fetcher = fetcher;
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (cancelled || terminated) {
                return;
            }
            if (n <= 0) {
                // Reactive Streams rule 3.9.
                cancel();
                terminated = true;
                subscriber.onError(new IllegalArgumentException("Demand must be positive (rule 3.9): " + n));
                return;
            }
            demand.getAndUpdate(current -> current + n < 0 ? Long.MAX_VALUE : current + n);
            drain();
        }

        @Override
        public void cancel() {
            cancelled = true;
            CompletableFuture<T> call = inFlight.get();
            if (call != null) {
                call.cancel(false);
            }
        }

        private void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            do {
                step();
            } while (wip.decrementAndGet() != 0);
        }

        private void step() {
            if (cancelled || terminated || inFlight.get() != null || demand.get() == 0) {
                return;
            }
            if (started && !fetcher.hasNextPage(previous)) {
                terminated = true;
                subscriber.onComplete();
                return;
            }
            CompletableFuture<T> call;
            try {
                call = Objects.requireNonNull(fetcher.nextPage(started ? previous : null), "page future");
            } catch (RuntimeException e) {
                terminated = true;
                subscriber.onError(e);
                return;
            }
            inFlight.set(call);
            if (cancelled) {
                call.cancel(false);
            }
            call.whenComplete((page, failure) -> {
                if (cancelled || terminated) {
                    inFlight.set(null);
                    return;
                }
                if (failure != null) {
                    terminated = true;
                    inFlight.set(null);
                    subscriber.onError(failure instanceof CompletionException && failure.getCause() != null
                                       ? failure.getCause() : failure);
                    return;
                }
                previous = page;
                started = true;
                demand.decrementAndGet();
                subscriber.onNext(page);
                // Released only after delivery, so a concurrent request cannot start a call whose page would be
                // signalled while this one is.
                inFlight.set(null);
                drain();
            });
        }
    }
}
