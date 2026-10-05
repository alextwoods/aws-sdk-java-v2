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

package software.amazon.awssdk.http;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Factory;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * Reactive Streams TCK (TestNG) for the http-spi body publishers. {@code createPublisher(n)} publishes exactly
 * {@code n} chunks:
 * <ul>
 *   <li>{@code INPUT_STREAM}: {@link StreamingBodies#fromInputStream(java.io.InputStream, long, String, int)} of
 *       {@code n} bytes in one-byte chunks;</li>
 *   <li>{@code BOUNDED_CHUNKS}: {@link StreamingBodies#boundedChunks} re-chunking one {@code n}-byte upstream buffer
 *       into one-byte chunks;</li>
 *   <li>{@code REQUEST_BODY} / {@code ASYNC_REQUEST_BODY}: {@link RequestBodyAdapter} bodies of {@code n} full
 *       {@link StreamingBodies#DEFAULT_MAX_CHUNK_SIZE} chunks.</li>
 * </ul>
 */
public final class StreamingBodiesTckTest extends PublisherVerification<ByteBuffer> {
    private static final int MAX = StreamingBodies.DEFAULT_MAX_CHUNK_SIZE;

    /** Which publisher is verified. */
    enum Source {
        INPUT_STREAM, BOUNDED_CHUNKS, REQUEST_BODY, ASYNC_REQUEST_BODY
    }

    private final Source source;

    public StreamingBodiesTckTest(Source source) {
        super(new TestEnvironment());
        this.source = source;
    }

    @Factory
    public static Object[] sources() {
        Source[] sources = Source.values();
        Object[] tests = new Object[sources.length];
        for (int i = 0; i < sources.length; i++) {
            tests[i] = new StreamingBodiesTckTest(sources[i]);
        }
        return tests;
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        int n = Math.toIntExact(elements);
        return switch (source) {
            case INPUT_STREAM -> StreamingBodies.fromInputStream(new ByteArrayInputStream(new byte[n]), n, null, 1);
            case BOUNDED_CHUNKS -> StreamingBodies.boundedChunks(AsyncRequestBody.fromBytes(new byte[n]), 1, n);
            case REQUEST_BODY -> RequestBodyAdapter.toStreamingBody(RequestBody.fromBytes(new byte[n * MAX]));
            case ASYNC_REQUEST_BODY -> RequestBodyAdapter.toStreamingBody(AsyncRequestBody.fromBytes(new byte[n * MAX]));
        };
    }

    /** Failures (source errors, length mismatches) surface only under demand, so eager-error tests do not apply. */
    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return source == Source.REQUEST_BODY || source == Source.ASYNC_REQUEST_BODY ? 32 : 1024;
    }

    @Override
    public String toString() {
        return "StreamingBodiesTckTest(" + source + ")";
    }
}
