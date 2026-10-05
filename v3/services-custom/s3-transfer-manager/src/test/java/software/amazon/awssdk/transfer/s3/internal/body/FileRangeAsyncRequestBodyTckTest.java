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

package software.amazon.awssdk.transfer.s3.internal.body;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Factory;

/**
 * Reactive Streams TCK (TestNG) for the transfer manager's part bodies: a {@link FileRangeAsyncRequestBody} of
 * exactly {@code n} chunks taken from the middle of a larger file, read on an executor, bare and wrapped in the
 * {@link ProgressReportingRequestBody} the transfer manager puts around every part.
 */
public final class FileRangeAsyncRequestBodyTckTest extends PublisherVerification<ByteBuffer> {
    private static final int CHUNK = 16;
    private static final int OFFSET = 7;
    private static final ExecutorService IO = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "tck-io");
        t.setDaemon(true);
        return t;
    });

    private final boolean withProgress;

    public FileRangeAsyncRequestBodyTckTest(boolean withProgress) {
        super(new TestEnvironment());
        this.withProgress = withProgress;
    }

    @Factory
    public static Object[] factories() {
        return new Object[] {new FileRangeAsyncRequestBodyTckTest(false), new FileRangeAsyncRequestBodyTckTest(true)};
    }

    @AfterClass
    public static void shutdown() {
        IO.shutdownNow();
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        try {
            Path file = Files.createTempFile("file-range-body-tck", ".bin");
            file.toFile().deleteOnExit();
            Files.write(file, new byte[Math.toIntExact(OFFSET + elements * CHUNK + 5)]);
            FileRangeAsyncRequestBody body = new FileRangeAsyncRequestBody(file, OFFSET, elements * CHUNK, CHUNK, IO,
                                                                           Files.getLastModifiedTime(file));
            return withProgress ? new ProgressReportingRequestBody(body, bytes -> { }) : body;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Failures surface only under demand (a vanished or shortened file), so the eager-error tests do not apply. */
    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 64;
    }
}
