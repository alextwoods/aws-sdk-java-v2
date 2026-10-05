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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.testing.Collect;

/** The V2 {@code SdkPublisher} operators the enhanced client relies on, re-implemented over the V3 publisher type. */
class SdkPublishersTest {

    @Test
    void mapTransformsEveryElementInOrder() {
        SdkPublisher<Integer> mapped = SdkPublishers.map(source(List.of("a", "bb", "ccc")).publisher(), String::length);
        assertThat(Collect.join(Collect.all(mapped))).containsExactly(1, 2, 3);
    }

    @Test
    void mapFailureCancelsUpstreamAndSignalsTheError() {
        Source<String> source = source(List.of("a", "boom", "c"));
        SdkPublisher<String> mapped = SdkPublishers.map(source.publisher(), s -> {
            if (s.equals("boom")) {
                throw new IllegalStateException("mapper failed");
            }
            return s;
        });
        assertThatThrownBy(() -> Collect.join(Collect.all(mapped))).hasMessage("mapper failed");
        assertThat(source.cancelled).isTrue();
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 5, Long.MAX_VALUE})
    void flatMapIterableFlattensPagesHonoringDemand(long batch) {
        Source<List<Integer>> pages = source(List.of(List.of(1, 2, 3), List.of(), List.of(4), List.of(5, 6)));
        Recorder<Integer> recorder = new Recorder<>(batch);
        SdkPublishers.flatMapIterable(pages.publisher(), page -> page).subscribe(recorder);
        assertThat(Collect.join(recorder.done)).containsExactly(1, 2, 3, 4, 5, 6);
        // Never more items than requested, and the upstream is asked for one page at a time.
        assertThat(recorder.maxOutstandingViolation.get()).isFalse();
        assertThat(pages.maxRequest.get()).isEqualTo(1);
    }

    @Test
    void flatMapIterableRequestsNoPageWithoutDemand() {
        Source<List<Integer>> pages = source(List.of(List.of(1, 2), List.of(3)));
        List<Integer> received = new ArrayList<>();
        Subscription[] subscription = new Subscription[1];
        SdkPublishers.flatMapIterable(pages.publisher(), page -> page).subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscription[0] = s;
            }

            @Override
            public void onNext(Integer item) {
                received.add(item);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
        assertThat(pages.requested.get()).isZero();
        subscription[0].request(1);
        assertThat(received).containsExactly(1);
        assertThat(pages.requested.get()).isEqualTo(1);
        subscription[0].request(1);
        assertThat(received).containsExactly(1, 2);
        assertThat(pages.requested.get()).isEqualTo(1);
        subscription[0].cancel();
        assertThat(pages.cancelled).isTrue();
    }

    @Test
    void flatMapIterableCancellationAfterTheFirstItemStopsUpstream() {
        Source<List<Integer>> pages = source(List.of(List.of(1, 2), List.of(3), List.of(4)));
        assertThat(Collect.join(Collect.first(SdkPublishers.flatMapIterable(pages.publisher(), p -> p), 1)))
            .containsExactly(1);
        assertThat(pages.cancelled).isTrue();
        assertThat(pages.requested.get()).isEqualTo(1);
    }

    @Test
    void flatMapIterablePropagatesUpstreamAndMapperFailures() {
        SdkPublisher<List<Integer>> failing = subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                subscriber.onError(new IllegalStateException("page failed"));
            }

            @Override
            public void cancel() {
            }
        });
        assertThatThrownBy(() -> Collect.join(Collect.all(SdkPublishers.flatMapIterable(failing, p -> p))))
            .hasMessage("page failed");

        Source<List<Integer>> pages = source(List.of(List.of(1), List.of(2)));
        SdkPublisher<Integer> mapperFails = SdkPublishers.flatMapIterable(pages.publisher(), p -> {
            if (p.contains(2)) {
                throw new IllegalArgumentException("mapper failed");
            }
            return p;
        });
        assertThatThrownBy(() -> Collect.join(Collect.all(mapperFails))).hasMessage("mapper failed");
        assertThat(pages.cancelled).isTrue();
    }

    @Test
    void nonPositiveDemandIsSignalledAsAnErrorPerRule39() {
        Source<List<Integer>> pages = source(List.of(List.of(1)));
        CompletableFuture<Throwable> error = new CompletableFuture<>();
        SdkPublishers.flatMapIterable(pages.publisher(), p -> p).subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(0);
            }

            @Override
            public void onNext(Integer item) {
            }

            @Override
            public void onError(Throwable t) {
                error.complete(t);
            }

            @Override
            public void onComplete() {
                error.complete(null);
            }
        });
        assertThat(Collect.join(error)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("3.9");
        assertThat(pages.cancelled).isTrue();
    }

    private static <T> Source<T> source(List<T> elements) {
        return new Source<>(elements);
    }

    /** A synchronous, demand-respecting publisher of a fixed list that records requests and cancellation. */
    private static final class Source<T> {
        private final List<T> elements;
        private final AtomicLong requested = new AtomicLong();
        private final AtomicLong maxRequest = new AtomicLong();
        private volatile boolean cancelled;

        Source(List<T> elements) {
            this.elements = elements;
        }

        SdkPublisher<T> publisher() {
            return subscriber -> subscriber.onSubscribe(new Subscription() {
                private int next;
                private long demand;
                private boolean emitting;
                private boolean done;

                @Override
                public void request(long n) {
                    requested.addAndGet(n);
                    maxRequest.accumulateAndGet(n, Math::max);
                    demand += n;
                    if (emitting) {
                        return;
                    }
                    emitting = true;
                    while (demand > 0 && !cancelled && !done) {
                        if (next == elements.size()) {
                            done = true;
                            subscriber.onComplete();
                            break;
                        }
                        demand--;
                        subscriber.onNext(elements.get(next++));
                    }
                    if (!done && !cancelled && next == elements.size()) {
                        done = true;
                        subscriber.onComplete();
                    }
                    emitting = false;
                }

                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        }
    }

    /** Requests {@code batch} items whenever all previously requested items have arrived. */
    private static final class Recorder<T> implements Subscriber<T> {
        private final long batch;
        private final List<T> items = new CopyOnWriteArrayList<>();
        private final CompletableFuture<List<T>> done = new CompletableFuture<>();
        private final AtomicBoolean maxOutstandingViolation = new AtomicBoolean();
        private Subscription subscription;
        private long outstanding;

        Recorder(long batch) {
            this.batch = batch;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            outstanding = batch;
            s.request(batch);
        }

        @Override
        public void onNext(T item) {
            items.add(item);
            if (batch != Long.MAX_VALUE && --outstanding < 0) {
                maxOutstandingViolation.set(true);
            }
            if (batch != Long.MAX_VALUE && outstanding == 0) {
                outstanding = batch;
                subscription.request(batch);
            }
        }

        @Override
        public void onError(Throwable t) {
            done.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            done.complete(List.copyOf(items));
        }
    }
}
