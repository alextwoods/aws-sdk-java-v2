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

package software.amazon.awssdk.transfer.s3.internal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/** Reads every page of a paginator publisher, one page of demand at a time. Not a public API. */
public final class Pages {
    private Pages() {
    }

    /**
     * Collects the pages of {@code pages}; cancelling the returned future cancels the subscription.
     */
    public static <T> CompletableFuture<List<T>> collect(Publisher<T> pages) {
        CompletableFuture<List<T>> result = new CompletableFuture<>();
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        result.whenComplete((r, t) -> {
            Subscription s = subscription.get();
            if (t != null && s != null) {
                s.cancel();
            }
        });
        pages.subscribe(new Subscriber<T>() {
            private final List<T> collected = new ArrayList<>();

            @Override
            public void onSubscribe(Subscription s) {
                subscription.set(s);
                if (result.isDone()) {
                    s.cancel();
                    return;
                }
                s.request(1);
            }

            @Override
            public void onNext(T page) {
                collected.add(page);
                subscription.get().request(1);
            }

            @Override
            public void onError(Throwable t) {
                result.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                result.complete(collected);
            }
        });
        return result;
    }
}
