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

package software.amazon.awssdk.checksums;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Factory;
import software.amazon.awssdk.checksums.internal.ChunkTransform;
import software.amazon.awssdk.checksums.internal.TransformingPublisher;
import software.amazon.awssdk.http.StreamingBodies;

/**
 * Reactive Streams TCK (TestNG) for {@link TransformingPublisher}. {@code createPublisher(n)} publishes exactly
 * {@code n} chunks:
 * <ul>
 *   <li>{@code PASS_THROUGH}: {@code n} one-byte source chunks, each delivered unchanged;</li>
 *   <li>{@code WITH_FINAL}: {@code n - 1} one-byte source chunks plus one final chunk produced on completion;</li>
 *   <li>{@code SKIPPING}: {@code 2n} one-byte source chunks of which every other one transforms to nothing.</li>
 * </ul>
 */
public final class TransformingPublisherTckTest extends PublisherVerification<ByteBuffer> {
    enum Mode {
        PASS_THROUGH, WITH_FINAL, SKIPPING
    }

    private final Mode mode;

    public TransformingPublisherTckTest(Mode mode) {
        super(new TestEnvironment());
        this.mode = mode;
    }

    @Factory
    public static Object[] modes() {
        Mode[] modes = Mode.values();
        Object[] tests = new Object[modes.length];
        for (int i = 0; i < modes.length; i++) {
            tests[i] = new TransformingPublisherTckTest(modes[i]);
        }
        return tests;
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        int n = Math.toIntExact(elements);
        return switch (mode) {
            case PASS_THROUGH -> new TransformingPublisher(source(n), () -> new Transform(false, false));
            case WITH_FINAL -> n == 0 ? new TransformingPublisher(source(0), () -> new Transform(false, false))
                                      : new TransformingPublisher(source(n - 1), () -> new Transform(true, false));
            case SKIPPING -> new TransformingPublisher(source(2 * n), () -> new Transform(false, true));
        };
    }

    /** Failures surface only under demand, so eager-error tests do not apply. */
    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }

    @Override
    public String toString() {
        return "TransformingPublisherTckTest(" + mode + ")";
    }

    private static Publisher<ByteBuffer> source(int bytes) {
        return StreamingBodies.fromInputStream(new ByteArrayInputStream(new byte[bytes]), bytes, null, 1);
    }

    private static final class Transform implements ChunkTransform {
        private final boolean emitFinal;
        private final boolean skipEveryOther;
        private int seen;

        Transform(boolean emitFinal, boolean skipEveryOther) {
            this.emitFinal = emitFinal;
            this.skipEveryOther = skipEveryOther;
        }

        @Override
        public ByteBuffer onChunk(ByteBuffer chunk) {
            return skipEveryOther && seen++ % 2 == 0 ? ByteBuffer.allocate(0) : chunk;
        }

        @Override
        public ByteBuffer onComplete() {
            return emitFinal ? ByteBuffer.wrap(new byte[] {1}) : ByteBuffer.allocate(0);
        }
    }
}
