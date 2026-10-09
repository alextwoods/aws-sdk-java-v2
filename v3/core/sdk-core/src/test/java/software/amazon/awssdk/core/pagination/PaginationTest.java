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

package software.amazon.awssdk.core.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.pagination.async.AsyncPageFetcher;
import software.amazon.awssdk.core.pagination.async.PagePublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.pagination.sync.SyncPageFetcher;

/** Lazy sync pages, demand-driven async pages, and the modeled continuation condition. */
class PaginationTest {

    /** Pages {@code 0..count-1}; page {@code i} carries the token {@code i + 1} unless it is the last. */
    private static final class Pages implements SyncPageFetcher<Integer> {
        private final int count;
        final AtomicInteger calls = new AtomicInteger();
        final List<CompletableFuture<Integer>> pending = new ArrayList<>();
        boolean defer;

        Pages(int count) {
            this.count = count;
        }

        @Override
        public boolean hasNextPage(Integer previousPage) {
            return PaginationTokens.hasToken(previousPage + 1 < count ? String.valueOf(previousPage + 1) : "");
        }

        @Override
        public Integer nextPage(Integer previousPage) {
            calls.incrementAndGet();
            return previousPage == null ? 0 : previousPage + 1;
        }

        CompletableFuture<Integer> next(Integer previousPage) {
            calls.incrementAndGet();
            int page = previousPage == null ? 0 : previousPage + 1;
            if (!defer) {
                return CompletableFuture.completedFuture(page);
            }
            CompletableFuture<Integer> future = new CompletableFuture<>();
            pending.add(future);
            return future;
        }
    }

    @Test
    void tokensAreAbsentWhenNullOrEmpty() {
        assertFalse(PaginationTokens.hasToken(null));
        assertFalse(PaginationTokens.hasToken(""));
        assertFalse(PaginationTokens.hasToken(List.of()));
        assertFalse(PaginationTokens.hasToken(Map.of()));
        assertTrue(PaginationTokens.hasToken("t"));
        assertTrue(PaginationTokens.hasToken(Map.of("k", 1)));
    }

    @Test
    void syncPagesAreFetchedOnlyWhenPulled() {
        Pages pages = new Pages(3);
        Iterator<Integer> iterator = SdkIterable.pages(pages).iterator();
        assertEquals(0, pages.calls.get());
        assertEquals(0, iterator.next());
        assertEquals(1, pages.calls.get());
        assertEquals(List.of(1, 2), List.of(iterator.next(), iterator.next()));
        assertFalse(iterator.hasNext());
        assertEquals(3, pages.calls.get());
        assertEquals(List.of(0, 0, 1, 1, 2, 2),
                     SdkIterable.pages(new Pages(3)).flatMapIterable(p -> List.of(p, p)).stream().toList());
    }

    /** Pagination emits each modeled page once, in order, and stops exactly at the last one. */
    @Property
    void syncPaginationEmitsEveryPageOnceInOrder(@ForAll @IntRange(min = 1, max = 50) int count) {
        Pages pages = new Pages(count);
        List<Integer> seen = SdkIterable.pages(pages).stream().toList();
        assertEquals(count, seen.size());
        for (int i = 0; i < count; i++) {
            assertEquals(i, seen.get(i));
        }
        assertEquals(count, pages.calls.get());
    }

    /** Async pages never exceed cumulative demand and call at most once per unit of it. */
    @Property
    void asyncPaginationCallsOncePerDemand(@ForAll @IntRange(min = 1, max = 20) int count,
                                           @ForAll @Size(max = 10) List<@IntRange(min = 1, max = 4) Integer> demands) {
        Pages pages = new Pages(count);
        Recorder recorder = new Recorder();
        PagePublisher.create(fetcher(pages)).subscribe(recorder);
        assertEquals(0, pages.calls.get());
        long demand = 0;
        for (int n : demands) {
            recorder.subscription.request(n);
            demand += n;
            assertEquals(Math.min(demand, count), recorder.pages.size());
            assertEquals(Math.min(demand, count), pages.calls.get());
        }
        assertEquals(demand > count, recorder.completed);
    }

    @Test
    void cancellationCancelsTheInFlightCallAndStopsCalling() {
        Pages pages = new Pages(10);
        pages.defer = true;
        Recorder recorder = new Recorder();
        PagePublisher.create(fetcher(pages)).subscribe(recorder);
        recorder.subscription.request(5);
        assertEquals(1, pages.calls.get(), "one call in flight at a time");
        pages.pending.get(0).complete(0);
        assertEquals(2, pages.calls.get());
        recorder.subscription.cancel();
        assertTrue(pages.pending.get(1).isCancelled());
        recorder.subscription.request(5);
        assertEquals(2, pages.calls.get());
        assertEquals(List.of(0), recorder.pages);
    }

    /**
     * Reactive Streams rule 1.3 (found by the generated-paginator TCK of task 18.8): a page requested inside
     * onSubscribe may complete on another thread while onSubscribe is still running, so no page call starts before
     * onSubscribe returns. Here the page completes inside onSubscribe, standing in for that other thread.
     */
    @Test
    void noPageIsSignalledBeforeOnSubscribeReturns() {
        Pages pages = new Pages(3);
        pages.defer = true;
        List<Integer> received = new ArrayList<>();
        List<String> violations = new ArrayList<>();
        PagePublisher.create(fetcher(pages)).subscribe(new Subscriber<Integer>() {
            private boolean inOnSubscribe;

            @Override
            public void onSubscribe(Subscription s) {
                inOnSubscribe = true;
                s.request(1);
                pages.pending.forEach(page -> page.complete(0));
                inOnSubscribe = false;
            }

            @Override
            public void onNext(Integer page) {
                if (inOnSubscribe) {
                    violations.add("onNext(" + page + ") during onSubscribe");
                }
                received.add(page);
            }

            @Override
            public void onError(Throwable t) {
                throw new AssertionError(t);
            }

            @Override
            public void onComplete() {
            }
        });
        assertEquals(List.of(), violations);
        assertEquals(1, pages.calls.get(), "the demand from onSubscribe starts the call once onSubscribe returns");
        pages.pending.get(0).complete(0);
        assertEquals(List.of(0), received);
    }

    private static AsyncPageFetcher<Integer> fetcher(Pages pages) {
        return new AsyncPageFetcher<>() {
            @Override
            public boolean hasNextPage(Integer previousPage) {
                return pages.hasNextPage(previousPage);
            }

            @Override
            public CompletableFuture<Integer> nextPage(Integer previousPage) {
                return pages.next(previousPage);
            }
        };
    }

    private static final class Recorder implements Subscriber<Integer> {
        final List<Integer> pages = new ArrayList<>();
        Subscription subscription;
        boolean completed;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        @Override
        public void onNext(Integer page) {
            pages.add(page);
        }

        @Override
        public void onError(Throwable t) {
            throw new AssertionError(t);
        }

        @Override
        public void onComplete() {
            completed = true;
        }
    }
}
