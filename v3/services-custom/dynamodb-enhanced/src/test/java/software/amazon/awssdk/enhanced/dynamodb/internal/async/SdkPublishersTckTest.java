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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.LongStream;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Factory;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.async.AsyncPageFetcher;
import software.amazon.awssdk.core.pagination.async.PagePublisher;

/**
 * Reactive Streams TCK (TestNG) for the enhanced client's publisher operators: {@link SdkPublishers#map} and
 * {@link SdkPublishers#flatMapIterable} over the V3 core {@link PagePublisher} that backs every paginated enhanced
 * operation. {@code createPublisher(n)} publishes exactly {@code n} elements; the flattening variant splits them into
 * pages of up to three elements, including empty pages, so item demand and page demand differ.
 */
public final class SdkPublishersTckTest extends PublisherVerification<Long> {
    private final boolean flatten;

    public SdkPublishersTckTest(boolean flatten) {
        super(new TestEnvironment());
        this.flatten = flatten;
    }

    @Factory
    public static Object[] factories() {
        return new Object[] {new SdkPublishersTckTest(false), new SdkPublishersTckTest(true)};
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        if (!flatten) {
            SdkPublisher<List<Long>> source = elements == 0 ? empty() : pages(elements, 1);
            return SdkPublishers.map(source, page -> page.get(0));
        }
        return SdkPublishers.flatMapIterable(pages(elements, 3), page -> page);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        // A source that fails as soon as it is subscribed to, without waiting for demand (rule 1.4).
        SdkPublisher<List<Long>> failed = subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            subscriber.onError(new IllegalStateException("failed page"));
        };
        return flatten ? SdkPublishers.flatMapIterable(failed, page -> page)
                       : SdkPublishers.map(failed, page -> page.get(0));
    }

    @Override
    public long maxElementsFromPublisher() {
        return 64;
    }

    /**
     * Pages holding {@code elements} consecutive numbers in total, {@code perPage} per page, with an empty page after
     * each full one when {@code perPage > 1}.
     */
    private static SdkPublisher<List<Long>> pages(long elements, int perPage) {
        return subscriber -> PagePublisher.create(new AsyncPageFetcher<List<Long>>() {
            private long emitted;
            private boolean emptyNext;

            @Override
            public boolean hasNextPage(List<Long> previousPage) {
                return emitted < elements;
            }

            @Override
            public CompletableFuture<List<Long>> nextPage(List<Long> previousPage) {
                if (emptyNext) {
                    emptyNext = false;
                    return CompletableFuture.completedFuture(List.of());
                }
                long start = emitted;
                long end = Math.min(elements, start + perPage);
                emitted = end;
                emptyNext = perPage > 1;
                return CompletableFuture.completedFuture(LongStream.range(start, end).boxed().toList());
            }
        }).subscribe(subscriber);
    }

    private static SdkPublisher<List<Long>> empty() {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (!done) {
                    done = true;
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }
}
