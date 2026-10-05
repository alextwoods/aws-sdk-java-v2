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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.join;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;

/**
 * Bounded-memory streaming (Requirement 19.11): runs in its own JVM (the {@code bounded-memory} surefire execution,
 * {@code -Xmx64m}) and moves a {@value #PAYLOAD_MIB} MiB payload, larger than that heap, through each streaming path
 * of the transfer manager, verifying complete, ordered, byte-for-byte content:
 * <ul>
 *     <li>{@code upload} of a caller {@link AsyncRequestBody} of unknown length (split into buffered parts): every
 *     part's SHA-256 must equal that of the corresponding range of the generated payload, in part order;</li>
 *     <li>{@code downloadFile} of a multipart object to a file: the file is compared with the payload byte by
 *     byte;</li>
 *     <li>{@code uploadFile} of that file: part digests as for {@code upload}.</li>
 * </ul>
 * The fake S3 never materializes the payload: it hashes uploaded parts and generates downloaded bytes.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class S3TransferManagerBoundedMemoryTest {
    static final int PAYLOAD_MIB = 96;
    static final long PAYLOAD = PAYLOAD_MIB * 1024L * 1024;
    static final long PART = 5L * 1024 * 1024;
    static final long PARTS = (PAYLOAD + PART - 1) / PART;

    private static final FakeS3 S3 = new FakeS3().digestOnly(true);
    private static S3TransferManager tm;

    @TempDir
    static Path dir;

    @BeforeAll
    static void setUp() {
        assertThat(Runtime.getRuntime().maxMemory())
            .as("the payload must be larger than the configured heap (run by the bounded-memory execution)")
            .isLessThan(PAYLOAD);
        tm = S3TransferManager.builder()
                              .s3Client(S3.client())
                              .multipartThresholdInBytes(PART)
                              .minimumPartSizeInBytes(PART)
                              .maxInFlightParts(2)
                              .build();
    }

    @AfterAll
    static void tearDown() {
        tm.close();
        S3.shutdown();
    }

    /** The expected object digest: the SHA-256 of each consecutive {@link #PART}-byte range, in order. */
    private static String expectedPartDigests() {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[64 * 1024];
        for (long start = 0; start < PAYLOAD; start += PART) {
            MessageDigest digest = FakeS3.sha256();
            long end = Math.min(PAYLOAD, start + PART);
            for (long position = start; position < end; ) {
                int n = (int) Math.min(buffer.length, end - position);
                for (int i = 0; i < n; i++) {
                    buffer[i] = FakeS3.generatedByte(position + i);
                }
                digest.update(buffer, 0, n);
                position += n;
            }
            sb.append(HexFormat.of().formatHex(digest.digest())).append(',');
        }
        return sb.toString();
    }

    /** The generated payload as a demand-driven publisher of unknown length (64 KiB chunks). */
    private static Publisher<ByteBuffer> generatedPayload(AtomicLong maxOutstandingDemand) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private long position;
            private long demand;
            private boolean emitting;
            private boolean done;

            @Override
            public void request(long n) {
                synchronized (this) {
                    demand += n;
                    maxOutstandingDemand.accumulateAndGet(demand, Math::max);
                    if (emitting) {
                        return;
                    }
                    emitting = true;
                }
                while (true) {
                    ByteBuffer chunk;
                    synchronized (this) {
                        if (done || demand == 0) {
                            emitting = false;
                            return;
                        }
                        if (position >= PAYLOAD) {
                            done = true;
                            emitting = false;
                            chunk = null;
                        } else {
                            int size = (int) Math.min(64 * 1024, PAYLOAD - position);
                            byte[] bytes = new byte[size];
                            for (int i = 0; i < size; i++) {
                                bytes[i] = FakeS3.generatedByte(position + i);
                            }
                            position += size;
                            demand--;
                            chunk = ByteBuffer.wrap(bytes);
                        }
                    }
                    if (chunk == null) {
                        subscriber.onComplete();
                        return;
                    }
                    subscriber.onNext(chunk);
                }
            }

            @Override
            public synchronized void cancel() {
                done = true;
            }
        });
    }

    @Test
    @Order(1)
    void upload_unknownLengthBodyLargerThanTheHeap_arrivesCompleteAndOrdered() {
        AtomicLong maxDemand = new AtomicLong();
        join(tm.upload(u -> u.requestBody(AsyncRequestBody.fromPublisher(generatedPayload(maxDemand)))
                            .putObjectRequest(p -> p.bucket("b").key("streamed")))
               .completionFuture());

        assertThat(S3.objectDigest("b", "streamed")).isEqualTo(expectedPartDigests());
        assertThat(S3.calls.stream().filter("uploadPart"::equals).count()).isEqualTo(PARTS);
        // The body is read one chunk at a time.
        assertThat(maxDemand.get()).isEqualTo(1);
    }

    @Test
    @Order(2)
    void downloadFile_multipartObjectLargerThanTheHeap_writesEveryByteInOrder() throws IOException {
        S3.putGeneratedObject("b", "generated", PAYLOAD, PART);
        Path target = dir.resolve("downloaded.bin");

        join(tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("b").key("generated")).destination(target))
               .completionFuture());

        assertThat(Files.size(target)).isEqualTo(PAYLOAD);
        byte[] buffer = new byte[64 * 1024];
        long position = 0;
        try (InputStream in = Files.newInputStream(target)) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                for (int i = 0; i < n; i++) {
                    if (buffer[i] != FakeS3.generatedByte(position + i)) {
                        throw new AssertionError("Byte " + (position + i) + " differs");
                    }
                }
                position += n;
            }
        }
        assertThat(position).isEqualTo(PAYLOAD);
    }

    @Test
    @Order(3)
    void uploadFile_fileLargerThanTheHeap_arrivesCompleteAndOrdered() {
        Path source = dir.resolve("downloaded.bin");

        join(tm.uploadFile(u -> u.source(source).putObjectRequest(p -> p.bucket("b").key("from-file")))
               .completionFuture());

        assertThat(S3.objectDigest("b", "from-file")).isEqualTo(expectedPartDigests());
        assertThat(S3.calls.stream().filter("uploadPart"::equals).count()).isEqualTo(2 * PARTS);
    }
}
