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
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.PART;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.TIMEOUT;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.join;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.randomBytes;
import static software.amazon.awssdk.transfer.s3.TransferTestSupport.transferManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * Concurrency test of one shared transfer manager (Requirements 19.9, 19.10): {@value #SIMULTANEOUS} callers each run
 * {@value #ROUNDS} rounds of a multipart file upload followed by a multipart file download of the same object, so the
 * total operation count is {@value #TOTAL_OPERATIONS} (greater than the simultaneous-call count). A barrier in the fake
 * S3 holds every {@code CreateMultipartUpload} and every first-part {@code GetObject} until {@value #SIMULTANEOUS}
 * transfers have reached it, which forces the configured overlap; every operation must finish within
 * {@link TransferTestSupport#TIMEOUT}.
 */
class S3TransferManagerConcurrencyTest {
    static final int SIMULTANEOUS = 4;
    static final int ROUNDS = 3;
    static final int TOTAL_OPERATIONS = SIMULTANEOUS * ROUNDS * 2;

    private final FakeS3 s3 = new FakeS3();
    private final S3TransferManager tm = transferManager(s3, 2);
    private final ExecutorService callers = Executors.newFixedThreadPool(SIMULTANEOUS);

    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        callers.shutdownNow();
        tm.close();
        s3.shutdown();
    }

    /** Releases arrivals in groups of {@code parties}. */
    private static final class AsyncBarrier {
        private final int parties;
        private final AtomicInteger arrived = new AtomicInteger();
        private final List<CompletableFuture<Void>> waves = new ArrayList<>();
        private final AtomicInteger maxWaiting = new AtomicInteger();

        AsyncBarrier(int parties) {
            this.parties = parties;
        }

        synchronized CompletableFuture<Void> arrive() {
            int n = arrived.getAndIncrement();
            int wave = n / parties;
            while (waves.size() <= wave) {
                waves.add(new CompletableFuture<>());
            }
            maxWaiting.accumulateAndGet(n % parties + 1, Math::max);
            CompletableFuture<Void> future = waves.get(wave);
            if (n % parties == parties - 1) {
                future.complete(null);
            }
            return future;
        }
    }

    @Test
    void sharedTransferManager_overlappingUploadsAndDownloads_allCompleteWithExactContent() throws Exception {
        AsyncBarrier uploads = new AsyncBarrier(SIMULTANEOUS);
        AsyncBarrier downloads = new AsyncBarrier(SIMULTANEOUS);
        s3.gate(r -> {
            if (r instanceof CreateMultipartUploadRequest) {
                return uploads.arrive();
            }
            if (r instanceof GetObjectRequest g && Integer.valueOf(1).equals(g.partNumber())) {
                return downloads.arrive();
            }
            return CompletableFuture.completedFuture(null);
        });
        AtomicInteger completedOperations = new AtomicInteger();

        List<Future<?>> workers = new ArrayList<>();
        for (int w = 0; w < SIMULTANEOUS; w++) {
            int worker = w;
            workers.add(callers.submit(() -> {
                for (int round = 0; round < ROUNDS; round++) {
                    byte[] data = randomBytes(PART * 3 + worker * 100 + round, worker * 31L + round);
                    String key = "w" + worker + "/r" + round;
                    Path source = Files.write(dir.resolve("in-" + worker + "-" + round), data);
                    join(tm.uploadFile(u -> u.source(source).putObjectRequest(p -> p.bucket("b").key(key)))
                           .completionFuture());
                    completedOperations.incrementAndGet();
                    Path target = dir.resolve("out-" + worker + "-" + round);
                    join(tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("b").key(key)).destination(target))
                           .completionFuture());
                    completedOperations.incrementAndGet();
                    assertThat(Files.readAllBytes(target)).isEqualTo(data);
                }
                return null;
            }));
        }
        for (Future<?> worker : workers) {
            worker.get(TIMEOUT.toMillis() * 2, TimeUnit.MILLISECONDS);
        }

        assertThat(completedOperations.get()).isEqualTo(TOTAL_OPERATIONS);
        assertThat(uploads.maxWaiting.get()).isEqualTo(SIMULTANEOUS);
        assertThat(downloads.maxWaiting.get()).isEqualTo(SIMULTANEOUS);
        assertThat(s3.openUploads()).isEmpty();
        // Per-transfer part concurrency stays bounded even with several transfers in flight.
        assertThat(s3.maxInFlightParts.get()).isLessThanOrEqualTo(SIMULTANEOUS * 2);
    }
}
