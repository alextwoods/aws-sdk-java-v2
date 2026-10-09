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
package software.amazon.awssdk.test.boundedmemory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.SkipException;
import org.testng.annotations.Factory;
import software.amazon.awssdk.core.async.AsyncRequestBody;
/**
 * Reactive Streams TCK (TestNG: demand, cancellation, early termination) for the public {@link AsyncRequestBody}
 * factories that sdk-core's own {@code AsyncRequestBodyTckTest} does not run ({@code fromBytes} and {@code fromFile}
 * are verified there): {@code fromBytesUnsafe}, {@code fromByteBuffer}, {@code fromRemainingByteBuffer},
 * {@code fromString} (UTF-8 and an explicit charset), {@code fromPublisher} and {@code empty}. {@code createPublisher(n)}
 * is a body of exactly {@code n} full {@link AsyncRequestBody#DEFAULT_CHUNK_SIZE} chunks; {@code empty()} has no other
 * length, so its verification runs only the zero-element cases and skips the rest.
 */
public final class AsyncRequestBodyFactoriesTckTest extends PublisherVerification<ByteBuffer> {
    private static final int CHUNK = AsyncRequestBody.DEFAULT_CHUNK_SIZE;
    private final Kind factory;
    /** The factories under verification. */
    enum Kind {
        BYTES_UNSAFE, BYTE_BUFFER, REMAINING_BYTE_BUFFER, STRING, STRING_CHARSET, PUBLISHER, EMPTY
    }
    public AsyncRequestBodyFactoriesTckTest(Kind factory) {
        super(new TestEnvironment());
        this.factory = factory;
    }
    @Factory
    public static Object[] factories() {
        return Arrays.stream(Kind.values()).map(AsyncRequestBodyFactoriesTckTest::new).toArray();
    }
    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        int size = Math.toIntExact(elements * CHUNK);
        switch (factory) {
            case BYTES_UNSAFE:
                return AsyncRequestBody.fromBytesUnsafe(new byte[size]);
            case BYTE_BUFFER: {
                // fromByteBuffer copies the whole buffer regardless of its position.
                ByteBuffer buffer = ByteBuffer.allocate(size);
                buffer.position(size / 2);
                return AsyncRequestBody.fromByteBuffer(buffer);
            }
            case REMAINING_BYTE_BUFFER: {
                // Only the remaining bytes are the body: a prefix of 7 bytes is skipped.
                ByteBuffer buffer = ByteBuffer.allocate(size + 7);
                buffer.position(7);
                return AsyncRequestBody.fromRemainingByteBuffer(buffer);
            }
            case STRING:
                return AsyncRequestBody.fromString("x".repeat(size));
            case STRING_CHARSET:
                return AsyncRequestBody.fromString("y".repeat(size), StandardCharsets.US_ASCII);
            case PUBLISHER:
                return AsyncRequestBody.fromPublisher(AsyncRequestBody.fromBytes(new byte[size]));
            case EMPTY:
                if (elements != 0) {
                    throw new SkipException("AsyncRequestBody.empty() has exactly zero elements");
                }
                return AsyncRequestBody.empty();
            default:
                throw new IllegalStateException(String.valueOf(factory));
        }
    }
    /** Failures surface only under demand, so the eager-error tests do not apply. */
    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }
    @Override
    public long maxElementsFromPublisher() {
        return 32;
    }
    @Override
    public String toString() {
        return "AsyncRequestBodyFactoriesTckTest[" + factory + "]";
    }
}
