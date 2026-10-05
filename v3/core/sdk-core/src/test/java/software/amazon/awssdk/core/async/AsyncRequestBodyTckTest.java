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

package software.amazon.awssdk.core.async;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Factory;

/**
 * Reactive Streams TCK (TestNG) for the {@link AsyncRequestBody} factories: {@code createPublisher(n)} is a body of
 * exactly {@code n} full chunks, from bytes or from a file. Bodies are read lazily under demand, so element counts are
 * bounded to keep the fixtures small.
 */
public final class AsyncRequestBodyTckTest extends PublisherVerification<ByteBuffer> {
    private static final int CHUNK = AsyncRequestBody.DEFAULT_CHUNK_SIZE;

    private final boolean fromFile;

    public AsyncRequestBodyTckTest(boolean fromFile) {
        super(new TestEnvironment());
        this.fromFile = fromFile;
    }

    /** Runs the verification once per factory family. */
    @Factory
    public static Object[] factories() {
        return new Object[] {new AsyncRequestBodyTckTest(false), new AsyncRequestBodyTckTest(true)};
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        byte[] bytes = new byte[Math.toIntExact(elements * CHUNK)];
        if (!fromFile) {
            return AsyncRequestBody.fromBytes(bytes);
        }
        try {
            Path file = Files.createTempFile("async-request-body-tck", ".bin");
            file.toFile().deleteOnExit();
            Files.write(file, bytes);
            return AsyncRequestBody.fromFile(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Failures surface only under demand (a vanished file), so the eager-error tests do not apply. */
    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 32;
    }
}
