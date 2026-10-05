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

package software.amazon.awssdk.test.generated.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/** Collects every element of a publisher, requesting one element at a time. */
public final class Collect {

    private Collect() {
    }

    public static <T> CompletableFuture<List<T>> all(Publisher<T> publisher) {
        CompletableFuture<List<T>> result = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<T>() {
            private final List<T> elements = new ArrayList<>();
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(T element) {
                elements.add(element);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable failure) {
                result.completeExceptionally(failure);
            }

            @Override
            public void onComplete() {
                result.complete(List.copyOf(elements));
            }
        });
        return result;
    }
}
