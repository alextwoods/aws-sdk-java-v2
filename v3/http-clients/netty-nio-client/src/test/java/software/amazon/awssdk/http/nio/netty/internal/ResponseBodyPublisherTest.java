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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class ResponseBodyPublisherTest {
    private final EventLoop loop = new DefaultEventLoop();

    @AfterEach
    void shutdown() throws InterruptedException {
        loop.shutdownGracefully(0, 1, TimeUnit.SECONDS).await(5, TimeUnit.SECONDS);
    }

    /**
     * Regression (HTTP SPI TCK, cancel racing the terminal signal): when the exchange was cancelled just before the
     * body ended, {@code completing()} loses and the publisher used to terminate silently, so the exchange's later
     * {@code fail()} was dropped and the subscriber never received a terminal signal.
     */
    @Test
    void endOfBodyLosingToACancellation_stillDeliversTheExchangeFailure() throws Exception {
        ResponseBodyPublisher publisher = new ResponseBodyPublisher(loop, new ResponseBodyPublisher.Source() {
            @Override
            public void read() {
            }

            @Override
            public void cancelled() {
            }

            @Override
            public boolean completing() {
                return false; // the exchange already terminated (cancelled)
            }
        });
        List<String> signals = new CopyOnWriteArrayList<>();
        RuntimeException cancelled = new RuntimeException("cancelled");
        loop.submit(() -> publisher.subscribe(new Subscriber<ByteBuffer>() {
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
                assertSame(cancelled, t);
                signals.add("onError");
            }

            @Override
            public void onComplete() {
                signals.add("onComplete");
            }
        })).get(5, TimeUnit.SECONDS);
        loop.submit(publisher::complete).get(5, TimeUnit.SECONDS);
        loop.submit(() -> publisher.fail(cancelled)).get(5, TimeUnit.SECONDS);
        loop.submit(() -> publisher.fail(cancelled)).get(5, TimeUnit.SECONDS);
        assertEquals(List.of("onError"), signals);
    }
}
