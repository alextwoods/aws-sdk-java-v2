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

package software.amazon.awssdk.enhanced.dynamodb.testing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/** Reactive Streams collection helpers for the async suites. */
public final class Collect {
    /** The bounded completion timeout of every async collection in these suites. */
    public static final Duration TIMEOUT = Duration.ofSeconds(30);

    private Collect() {
    }

    /** Every element of {@code publisher}, requesting one element at a time. */
    public static <T> CompletableFuture<List<T>> all(Publisher<T> publisher) {
        return first(publisher, Long.MAX_VALUE);
    }

    /**
     * The first {@code limit} elements of {@code publisher}, requesting one at a time and cancelling the subscription
     * once {@code limit} have arrived.
     */
    public static <T> CompletableFuture<List<T>> first(Publisher<T> publisher, long limit) {
        CompletableFuture<List<T>> result = new CompletableFuture<>();
        List<T> items = new ArrayList<>();
        publisher.subscribe(new Subscriber<T>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                if (limit == 0) {
                    s.cancel();
                    result.complete(items);
                } else {
                    s.request(1);
                }
            }

            @Override
            public void onNext(T item) {
                items.add(item);
                if (items.size() >= limit) {
                    subscription.cancel();
                    result.complete(List.copyOf(items));
                } else {
                    subscription.request(1);
                }
            }

            @Override
            public void onError(Throwable t) {
                result.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                result.complete(List.copyOf(items));
            }
        });
        return result;
    }

    /** Waits for {@code future} for at most {@link #TIMEOUT}; a failure is rethrown as its cause when unchecked. */
    public static <T> T join(CompletableFuture<T> future) {
        try {
            return future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (TimeoutException e) {
            throw new AssertionError("Not completed within " + TIMEOUT, e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            while (cause instanceof CompletionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new CompletionException(cause);
        }
    }
}
