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

package software.amazon.awssdk.http.apache5;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;

/**
 * Reactive Streams TCK (TestNG) for the async request-body subscriber ({@link PublisherEntityProducer}). Each
 * subscriber is attached to a simulated connection whose {@code requestOutput} schedules {@code produce} on another
 * thread and whose writes accept every byte, as the I/O reactor does when the socket is writable.
 */
public final class PublisherEntityProducerTckTest extends SubscriberBlackboxVerification<ByteBuffer> {
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public PublisherEntityProducerTckTest() {
        super(new TestEnvironment());
    }

    @AfterClass
    public void shutdown() {
        io.shutdownNow();
    }

    @Override
    public Subscriber<ByteBuffer> createSubscriber() {
        PublisherEntityProducer producer = new PublisherEntityProducer(s -> { }, -1, () -> { });
        DataStreamChannel channel = new DataStreamChannel() {
            @Override
            public void requestOutput() {
                io.execute(() -> {
                    try {
                        producer.produce(this);
                    } catch (IOException e) {
                        // The body failed; the exchange would fail here.
                    }
                });
            }

            @Override
            public int write(ByteBuffer src) {
                int n = src.remaining();
                src.position(src.limit());
                return n;
            }

            @Override
            public void endStream() {
            }

            @Override
            public void endStream(List<? extends Header> trailers) {
            }
        };
        try {
            // The first write opportunity subscribes the producer to its (inert) body; the TCK then subscribes it.
            producer.produce(channel);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return producer;
    }

    @Override
    public ByteBuffer createElement(int element) {
        return ByteBuffer.wrap(new byte[] {(byte) element}).asReadOnlyBuffer();
    }
}
