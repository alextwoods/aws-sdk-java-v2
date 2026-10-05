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

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

/**
 * Reactive Streams TCK (TestNG) for the async response-body publisher. {@code createPublisher(n)} publishes exactly
 * {@code n} one-byte chunks from a simulated connection that, like the engine, offers bytes only within the credit
 * the publisher grants (a 16-byte window, then one grant per delivered chunk).
 */
public final class ResponseBodyPublisherTckTest extends PublisherVerification<ByteBuffer> {
    public ResponseBodyPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        AtomicLong remaining = new AtomicLong(elements);
        AtomicReference<ResponseBodyPublisher> ref = new AtomicReference<>();
        ResponseBodyPublisher publisher = new ResponseBodyPublisher(16, new ResponseBodyPublisher.Upstream() {
            @Override
            public void request(int bytes) {
                for (int i = 0; i < bytes && remaining.get() > 0; i++) {
                    if (remaining.decrementAndGet() >= 0) {
                        ref.get().offer(ByteBuffer.wrap(new byte[] {1}).asReadOnlyBuffer());
                    }
                }
                if (remaining.get() <= 0) {
                    ref.get().end();
                }
            }

            @Override
            public void cancel() {
            }

            @Override
            public boolean complete() {
                return true;
            }
        });
        ref.set(publisher);
        if (elements == 0) {
            publisher.end();
        } else {
            publisher.onCapacityRequested();
        }
        return publisher;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        ResponseBodyPublisher publisher = new ResponseBodyPublisher(16, new ResponseBodyPublisher.Upstream() {
            @Override
            public void request(int bytes) {
            }

            @Override
            public void cancel() {
            }

            @Override
            public boolean complete() {
                return true;
            }
        });
        publisher.fail(new IllegalStateException("connection failed"));
        return publisher;
    }
}
