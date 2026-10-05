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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class CrtResponsePublisherTest {

    /** A source whose exchange was already cancelled: every body terminal loses. */
    private static final CrtResponsePublisher.Source CANCELLED_EXCHANGE = new CrtResponsePublisher.Source() {
        @Override
        public void incrementWindow(int bytes) {
        }

        @Override
        public Throwable terminate(Throwable failure) {
            return CrtResponsePublisher.SUPPRESS;
        }

        @Override
        public void cancelled(Throwable ruleViolation) {
        }
    };

    /**
     * Regression (HTTP SPI TCK, cancel racing the terminal signal): the end of the body lost to a cancellation of the
     * exchange and the publisher terminated silently, so the cancellation's failure was dropped and the subscriber
     * never received a terminal signal.
     */
    @Test
    void endOfBodyLosingToACancellation_deliversTheCancellationFailure() {
        CrtResponsePublisher publisher = new CrtResponsePublisher(CANCELLED_EXCHANGE);
        List<String> signals = subscribe(publisher);
        publisher.onComplete();
        RuntimeException cancelled = new RuntimeException("cancelled");
        publisher.onCancelled(cancelled);
        publisher.onCancelled(cancelled);
        assertEquals(List.of("onError:cancelled"), signals);
    }

    @Test
    void bodyFailureLosingToACancellation_deliversTheCancellationFailure() {
        CrtResponsePublisher publisher = new CrtResponsePublisher(CANCELLED_EXCHANGE);
        List<String> signals = subscribe(publisher);
        publisher.onError(new RuntimeException("reset"));
        publisher.onCancelled(new RuntimeException("cancelled"));
        assertEquals(List.of("onError:cancelled"), signals);
    }

    private static List<String> subscribe(CrtResponsePublisher publisher) {
        List<String> signals = new CopyOnWriteArrayList<>();
        publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer b) {
                signals.add("onNext");
            }

            @Override
            public void onError(Throwable t) {
                signals.add("onError:" + t.getMessage());
            }

            @Override
            public void onComplete() {
                signals.add("onComplete");
            }
        });
        return signals;
    }
}
