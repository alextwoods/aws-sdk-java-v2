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

package software.amazon.awssdk.http.smithy.internal;

import java.util.concurrent.Flow;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Presents a reactive-streams {@link Publisher} as a {@link Flow.Publisher}.
 *
 * <p>The SDK's request bodies are reactive-streams publishers while smithy consumes {@link Flow}. The two
 * specifications are identical in semantics and differ only in package, so this forwards straight through
 * without buffering or rescheduling.
 */
@SdkInternalApi
public final class ReactiveToFlowPublisher<T> implements Flow.Publisher<T> {

    private final Publisher<T> delegate;

    public ReactiveToFlowPublisher(Publisher<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> flowSubscriber) {
        delegate.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                flowSubscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        subscription.request(n);
                    }

                    @Override
                    public void cancel() {
                        subscription.cancel();
                    }
                });
            }

            @Override
            public void onNext(T item) {
                flowSubscriber.onNext(item);
            }

            @Override
            public void onError(Throwable throwable) {
                flowSubscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                flowSubscriber.onComplete();
            }
        });
    }
}
