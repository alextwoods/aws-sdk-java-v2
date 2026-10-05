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

import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;

/**
 * Reactive Streams TCK (TestNG) for {@link ResponseBodyPublisher} on a real Netty event loop. The source stands in
 * for the channel: each {@code read()} decodes one element asynchronously on the loop (like a channel read), and the
 * body completes after {@code n} elements, so {@code createPublisher(n)} publishes exactly {@code n} one-byte chunks.
 */
public final class ResponseBodyPublisherTckTest extends PublisherVerification<ByteBuffer> {
    private final EventLoop loop = new DefaultEventLoop();

    public ResponseBodyPublisherTckTest() {
        super(new TestEnvironment());
    }

    @AfterClass
    public void shutdown() throws InterruptedException {
        loop.shutdownGracefully(0, 1, TimeUnit.SECONDS).await(5, TimeUnit.SECONDS);
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        ResponseBodyPublisher[] holder = new ResponseBodyPublisher[1];
        long[] produced = {0};
        ResponseBodyPublisher.Source source = new ResponseBodyPublisher.Source() {
            @Override
            public void read() {
                loop.execute(() -> {
                    if (produced[0] < elements) {
                        produced[0]++;
                        holder[0].offer(ByteBuffer.wrap(new byte[] {(byte) produced[0]}).asReadOnlyBuffer());
                    }
                    if (produced[0] == elements) {
                        holder[0].complete();
                    } else {
                        holder[0].readComplete();
                    }
                });
            }

            @Override
            public void cancelled() {
            }

            @Override
            public boolean completing() {
                return true;
            }
        };
        holder[0] = new ResponseBodyPublisher(loop, source);
        if (elements == 0) {
            loop.execute(holder[0]::complete);
        }
        return holder[0];
    }

    /** Failures come from the exchange after subscription (teardown), so eager-error tests do not apply. */
    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }
}
