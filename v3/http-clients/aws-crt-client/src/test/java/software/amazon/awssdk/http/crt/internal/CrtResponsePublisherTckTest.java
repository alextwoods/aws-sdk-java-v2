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

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

/**
 * Reactive Streams TCK verification of the async response-body publisher, fed by a simulated CRT stream that pushes
 * one-byte chunks while its manual read window is open (window reopened only by {@code incrementWindow}), exactly as
 * the native stream does.
 */
public final class CrtResponsePublisherTckTest extends PublisherVerification<ByteBuffer> {
    private static final int WINDOW = 4;

    public CrtResponsePublisherTckTest() {
        super(new TestEnvironment());
    }

    /** A simulated native stream of {@code elements} one-byte chunks. */
    static final class SimulatedStream implements CrtResponsePublisher.Source {
        private final long elements;
        private final CrtResponsePublisher publisher;
        private long sent;
        private long window = WINDOW;
        private boolean pushing;
        private boolean completed;

        SimulatedStream(long elements) {
            this.elements = elements;
            this.publisher = new CrtResponsePublisher(this);
        }

        void push() {
            synchronized (this) {
                if (pushing) {
                    return;
                }
                pushing = true;
            }
            while (true) {
                boolean complete = false;
                synchronized (this) {
                    if (sent < elements && window > 0) {
                        sent++;
                        window--;
                    } else {
                        if (sent == elements && !completed) {
                            completed = true;
                            complete = true;
                        }
                        pushing = false;
                        if (!complete) {
                            return;
                        }
                    }
                }
                if (complete) {
                    publisher.onComplete();
                    return;
                }
                publisher.onData(new byte[] {1});
            }
        }

        @Override
        public void incrementWindow(int bytes) {
            synchronized (this) {
                window += bytes;
            }
            push();
        }

        @Override
        public Throwable terminate(Throwable failure) {
            return failure;
        }

        @Override
        public void cancelled(Throwable ruleViolation) {
        }
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        SimulatedStream stream = new SimulatedStream(elements);
        stream.push();
        return stream.publisher;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        CrtResponsePublisher publisher = new CrtResponsePublisher(new SimulatedStream(0));
        publisher.onError(new IllegalStateException("failed exchange"));
        return publisher;
    }

    @Override
    public long maxElementsFromPublisher() {
        return Long.MAX_VALUE - 1;
    }
}
