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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.checksums.internal.AwsChunkedTrailerTransform;
import software.amazon.awssdk.checksums.internal.ChecksumValidatingTransform;
import software.amazon.awssdk.checksums.internal.ChunkTransform;
import software.amazon.awssdk.checksums.internal.GzipTransform;
import software.amazon.awssdk.checksums.internal.TransformedBodies;
import software.amazon.awssdk.checksums.internal.TransformingPublisher;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;

class ChunkTransformsTest {
    private static final int CHUNK = 16;

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 15, 16, 17, 48, 50, 1000})
    void awsChunked_bothViews_produceExactFramingAndLength(int size) {
        byte[] payload = random(size);
        StreamingBody source = StreamingBodies.fromBytes(payload, null);
        long length = AwsChunkedTrailerTransform.encodedLength(size, ChecksumAlgorithm.CRC32, CHUNK);
        StreamingBody encoded = TransformedBodies.transform(
            source, length, () -> new AwsChunkedTrailerTransform(ChecksumAlgorithm.CRC32, CHUNK));

        byte[] viaStream = TestSupport.readStream(HttpBody.streaming(encoded));
        byte[] viaPublisher = TestSupport.readPublisher(encoded);

        assertThat(viaStream).isEqualTo(viaPublisher).hasSize((int) length);
        assertThat(viaStream).isEqualTo(expectedChunked(payload));
    }

    @Test
    void awsChunked_whenSourceChunksAreSmallAndDemandIsOne_framesStayFixedSize() throws Exception {
        byte[] payload = random(100);
        StreamingBody source = StreamingBodies.fromInputStream(new ByteArrayInputStream(payload), payload.length,
                                                               null, 3);
        TransformingPublisher publisher = new TransformingPublisher(
            source, () -> new AwsChunkedTrailerTransform(ChecksumAlgorithm.CRC32, CHUNK));

        byte[] bytes = TestSupport.collect(publisher, 1).get(10, TimeUnit.SECONDS);

        assertThat(bytes).isEqualTo(expectedChunked(payload));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 100, 200_000})
    void gzip_bothViews_roundTripThroughJdkGzip(int size) throws IOException {
        byte[] payload = compressible(size);
        StreamingBody gzipped = TransformedBodies.transform(StreamingBodies.fromBytes(payload, null), -1,
                                                            GzipTransform::new);

        assertThat(gunzip(TestSupport.readStream(HttpBody.streaming(gzipped)))).isEqualTo(payload);
        assertThat(gunzip(TestSupport.readPublisher(gzipped))).isEqualTo(payload);
        assertThat(gunzip(GzipTransform.compress(payload))).isEqualTo(payload);
    }

    @Test
    void validating_whenChecksumMatches_passesBytesThrough() {
        byte[] payload = random(70_000);
        String expected = SdkChecksum.base64(ChecksumAlgorithm.SHA256, payload);
        StreamingBody body = TransformedBodies.transform(
            StreamingBodies.fromBytes(payload, null), payload.length,
            () -> new ChecksumValidatingTransform(ChecksumAlgorithm.SHA256, expected));

        assertThat(TestSupport.readStream(HttpBody.streaming(body))).isEqualTo(payload);
        assertThat(TestSupport.readPublisher(body)).isEqualTo(payload);
    }

    @Test
    void validating_whenChecksumDiffers_failsAtEndOfBothViews() {
        byte[] payload = random(1000);
        String wrong = SdkChecksum.base64(ChecksumAlgorithm.CRC32, "other".getBytes(StandardCharsets.UTF_8));
        StreamingBody body = TransformedBodies.transform(
            StreamingBodies.fromBytes(payload, null), payload.length,
            () -> new ChecksumValidatingTransform(ChecksumAlgorithm.CRC32, wrong));

        assertThatThrownBy(() -> TestSupport.readStream(HttpBody.streaming(body)))
            .isInstanceOf(ChecksumMismatchException.class)
            .hasMessageContaining("CRC32")
            .hasMessageContaining(wrong);
        assertThatThrownBy(() -> TestSupport.readPublisher(body)).isInstanceOf(ChecksumMismatchException.class);
    }

    @Test
    void transformingPublisher_whenCancelled_releasesTransformOnceAndStops() throws Exception {
        AtomicInteger released = new AtomicInteger();
        AtomicInteger delivered = new AtomicInteger();
        StreamingBody source = StreamingBodies.fromInputStream(new ByteArrayInputStream(random(100)), 100, null, 1);
        TransformingPublisher publisher = new TransformingPublisher(source, () -> new PassThrough(released));
        CompletableFuture<Void> done = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                s.request(3);
            }

            @Override
            public void onNext(ByteBuffer chunk) {
                if (delivered.incrementAndGet() == 3) {
                    subscription.cancel();
                    subscription.cancel();
                    done.complete(null);
                }
            }

            @Override
            public void onError(Throwable t) {
                done.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                done.completeExceptionally(new AssertionError("completed after cancel"));
            }
        });
        done.get(10, TimeUnit.SECONDS);
        assertThat(delivered).hasValue(3);
        assertThat(released).hasValue(1);
    }

    @Test
    void transformingPublisher_whenRequestNotPositive_signalsErrorAndReleases() throws Exception {
        AtomicInteger released = new AtomicInteger();
        StreamingBody source = StreamingBodies.fromBytes(random(10), null);
        TransformingPublisher publisher = new TransformingPublisher(source, () -> new PassThrough(released));
        CompletableFuture<Throwable> error = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(0);
            }

            @Override
            public void onNext(ByteBuffer chunk) {
            }

            @Override
            public void onError(Throwable t) {
                error.complete(t);
            }

            @Override
            public void onComplete() {
            }
        });
        assertThat(error.get(10, TimeUnit.SECONDS)).isInstanceOf(IllegalArgumentException.class)
                                                    .hasMessageContaining("3.9");
        assertThat(released).hasValue(1);
    }

    @Test
    void transformingInputStream_close_releasesTransformOnce() throws IOException {
        AtomicInteger released = new AtomicInteger();
        StreamingBody body = TransformedBodies.transform(StreamingBodies.fromBytes(random(10), null), 10,
                                                         () -> new PassThrough(released));
        InputStream in = body.openStream();
        in.read();
        in.close();
        in.close();
        assertThat(released).hasValue(1);
    }

    static byte[] expectedChunked(byte[] payload) {
        StringBuilder expected = new StringBuilder();
        for (int offset = 0; offset < payload.length; offset += CHUNK) {
            int n = Math.min(CHUNK, payload.length - offset);
            expected.append(Integer.toHexString(n)).append("\r\n")
                    .append(new String(payload, offset, n, StandardCharsets.ISO_8859_1)).append("\r\n");
        }
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(payload);
        byte[] digest = ByteBuffer.allocate(4).putInt((int) crc.getValue()).array();
        expected.append("0\r\nx-amz-checksum-crc32:")
                .append(java.util.Base64.getEncoder().encodeToString(digest)).append("\r\n\r\n");
        // Each payload byte is one ISO-8859-1 character; the framing is ASCII.
        return expected.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] gunzip(byte[] gzip) throws IOException {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(gzip))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toByteArray();
        }
    }

    private static byte[] random(int size) {
        byte[] bytes = new byte[size];
        new Random(size).nextBytes(bytes);
        return bytes;
    }

    private static byte[] compressible(int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) ('a' + (i % 7));
        }
        return bytes;
    }

    private static final class PassThrough implements ChunkTransform {
        private final AtomicInteger released;

        PassThrough(AtomicInteger released) {
            this.released = released;
        }

        @Override
        public ByteBuffer onChunk(ByteBuffer chunk) {
            return chunk;
        }

        @Override
        public ByteBuffer onComplete() {
            return ByteBuffer.allocate(0);
        }

        @Override
        public void release() {
            released.incrementAndGet();
        }
    }
}
