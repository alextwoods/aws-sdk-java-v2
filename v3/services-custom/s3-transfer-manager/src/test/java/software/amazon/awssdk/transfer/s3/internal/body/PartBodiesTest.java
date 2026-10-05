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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.transfer.s3.internal.upload.BufferedPartSource;
import software.amazon.awssdk.transfer.s3.internal.upload.PartSource;
import software.amazon.awssdk.transfer.s3.internal.upload.UploadPart;

/** Replayable part bodies: file ranges, progress retraction on retry, and the splitter's bounded buffering. */
class PartBodiesTest {
    private final ExecutorService io = Executors.newFixedThreadPool(2);

    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        io.shutdownNow();
    }

    private static byte[] read(AsyncRequestBody body) throws Exception {
        CompletableFuture<byte[]> result = new CompletableFuture<>();
        body.subscribe(new Subscriber<ByteBuffer>() {
            private final ByteArrayOutputStream out = new ByteArrayOutputStream();
            private Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer b) {
                assertThat(b.isReadOnly()).isTrue();
                byte[] bytes = new byte[b.remaining()];
                b.get(bytes);
                out.writeBytes(bytes);
                s.request(1);
            }

            @Override
            public void onError(Throwable t) {
                result.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                result.complete(out.toByteArray());
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }

    @Test
    void fileRange_everySubscriptionReplaysTheSameRange() throws Exception {
        byte[] data = new byte[100_000];
        new Random(1).nextBytes(data);
        Path file = Files.write(dir.resolve("f"), data);
        FileRangeAsyncRequestBody body = new FileRangeAsyncRequestBody(file, 1000, 50_000, 4096, io,
                                                                       Files.getLastModifiedTime(file));

        byte[] first = read(body);
        byte[] second = read(body);

        assertThat(first).isEqualTo(java.util.Arrays.copyOfRange(data, 1000, 51_000)).isEqualTo(second);
        assertThat(body.contentLength()).hasValue(50_000L);
    }

    @Test
    void fileRange_fileModifiedDuringUpload_failsTheSubscription() throws Exception {
        Path file = Files.write(dir.resolve("f"), new byte[10_000]);
        FileTime before = Files.getLastModifiedTime(file);
        Files.setLastModifiedTime(file, FileTime.from(Instant.now().plusSeconds(10)));
        FileRangeAsyncRequestBody body = new FileRangeAsyncRequestBody(file, 0, 10_000, 4096, io, before);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> read(body)))
            .hasCauseInstanceOf(SdkClientException.class).hasMessageContaining("modified");
    }

    @Test
    void fileRange_fileShorterThanTheRange_failsTheSubscription() throws Exception {
        Path file = Files.write(dir.resolve("f"), new byte[1_000]);
        FileRangeAsyncRequestBody body = new FileRangeAsyncRequestBody(file, 0, 5_000, 4096, io, null);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> read(body)))
            .hasMessageContaining("before the end of the range");
    }

    @Test
    void progressReporting_resubscription_retractsThePreviousAttemptsBytes() throws Exception {
        AtomicLong progress = new AtomicLong();
        AsyncRequestBody body = new ProgressReportingRequestBody(AsyncRequestBody.fromBytes(new byte[200_000]),
                                                                 progress::addAndGet);
        read(body);
        assertThat(progress.get()).isEqualTo(200_000);

        read(body);
        assertThat(progress.get()).isEqualTo(200_000);
    }

    @Test
    void bufferedPartSource_neverBuffersMorePartsThanAllowedAndSubscribesOnce() {
        int partSize = 1000;
        int maxParts = 2;
        AtomicLong requested = new AtomicLong();
        AtomicLong delivered = new AtomicLong();
        int[] subscriptions = new int[1];
        AsyncRequestBody infinite = AsyncRequestBody.fromPublisher(s -> {
            subscriptions[0]++;
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    requested.addAndGet(n);
                    for (long i = 0; i < n; i++) {
                        delivered.incrementAndGet();
                        s.onNext(ByteBuffer.wrap(new byte[300]));
                    }
                }

                @Override
                public void cancel() {
                }
            });
        });
        BufferedPartSource source = new BufferedPartSource(infinite, partSize, maxParts, null, b -> b);
        List<UploadPart> handedOut = new ArrayList<>();
        PartSource.Receiver receiver = new PartSource.Receiver() {
            @Override
            public void onPart(UploadPart part) {
                handedOut.add(part);
            }

            @Override
            public void onEnd() {
            }

            @Override
            public void onError(Throwable error) {
            }
        };

        source.next(receiver);
        source.next(receiver);
        source.next(receiver);

        // Two parts are out (the bound); the third request waits, and upstream stops at the bound plus one part
        // being filled plus one pending chunk.
        assertThat(handedOut).hasSize(2);
        assertThat(handedOut).extracting(UploadPart::partNumber).containsExactly(1, 2);
        assertThat(handedOut).allSatisfy(p -> assertThat(p.length()).isEqualTo(partSize));
        assertThat(delivered.get() * 300).isLessThanOrEqualTo((long) (maxParts + 1) * partSize + 300);
        long beforeRelease = delivered.get();

        handedOut.get(0).release();

        assertThat(handedOut).hasSize(3);
        assertThat(delivered.get()).isGreaterThan(beforeRelease);
        assertThat(delivered.get() * 300).isLessThanOrEqualTo((long) (maxParts + 2) * partSize + 300);
        assertThat(subscriptions[0]).isEqualTo(1);
        source.cancel();
    }

    @Test
    void bufferedPartSource_partsReplayTheirBytes() throws Exception {
        byte[] data = new byte[2_500];
        new Random(3).nextBytes(data);
        BufferedPartSource source = new BufferedPartSource(AsyncRequestBody.fromBytes(data), 1000, 4,
                                                           (long) data.length, b -> b);
        List<UploadPart> parts = new ArrayList<>();
        boolean[] ended = new boolean[1];
        for (int i = 0; i < 4; i++) {
            source.next(new PartSource.Receiver() {
                @Override
                public void onPart(UploadPart part) {
                    parts.add(part);
                }

                @Override
                public void onEnd() {
                    ended[0] = true;
                }

                @Override
                public void onError(Throwable error) {
                    throw new AssertionError(error);
                }
            });
        }

        assertThat(ended[0]).isTrue();
        assertThat(parts).extracting(UploadPart::length).containsExactly(1000L, 1000L, 500L);
        assertThat(parts).extracting(UploadPart::last).containsExactly(false, false, true);
        assertThat(read(parts.get(2).body())).isEqualTo(read(parts.get(2).body()))
                                             .isEqualTo(java.util.Arrays.copyOfRange(data, 2000, 2500));
    }
}
