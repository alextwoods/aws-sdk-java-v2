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

package software.amazon.awssdk.transfer.s3.internal;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.util.Validate;

/** Builds {@link DefaultS3TransferManager}s. */
public final class TransferManagerFactory {
    private TransferManagerFactory() {
    }

    /** The {@link S3TransferManager.Builder} returned by {@link S3TransferManager#builder()}. */
    public static final class DefaultBuilder implements S3TransferManager.Builder {
        private S3AsyncClient s3Client;
        private Executor executor;
        private Boolean uploadDirectoryFollowSymbolicLinks;
        private Integer uploadDirectoryMaxDepth;
        private Integer transferDirectoryMaxConcurrency;
        private Long multipartThresholdInBytes;
        private Long minimumPartSizeInBytes;
        private Integer maxInFlightParts;

        @Override
        public DefaultBuilder s3Client(S3AsyncClient s3AsyncClient) {
            this.s3Client = s3AsyncClient;
            return this;
        }

        @Override
        public DefaultBuilder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public DefaultBuilder uploadDirectoryFollowSymbolicLinks(Boolean uploadDirectoryFollowSymbolicLinks) {
            this.uploadDirectoryFollowSymbolicLinks = uploadDirectoryFollowSymbolicLinks;
            return this;
        }

        @Override
        public DefaultBuilder uploadDirectoryMaxDepth(Integer uploadDirectoryMaxDepth) {
            this.uploadDirectoryMaxDepth = uploadDirectoryMaxDepth;
            return this;
        }

        @Override
        public DefaultBuilder transferDirectoryMaxConcurrency(Integer transferDirectoryMaxConcurrency) {
            this.transferDirectoryMaxConcurrency = transferDirectoryMaxConcurrency;
            return this;
        }

        @Override
        public DefaultBuilder multipartThresholdInBytes(Long multipartThresholdInBytes) {
            this.multipartThresholdInBytes = multipartThresholdInBytes;
            return this;
        }

        @Override
        public DefaultBuilder minimumPartSizeInBytes(Long minimumPartSizeInBytes) {
            this.minimumPartSizeInBytes = minimumPartSizeInBytes;
            return this;
        }

        @Override
        public DefaultBuilder maxInFlightParts(Integer maxInFlightParts) {
            this.maxInFlightParts = maxInFlightParts;
            return this;
        }

        @Override
        public S3TransferManager build() {
            Validate.isPositiveOrNull(uploadDirectoryMaxDepth, "uploadDirectoryMaxDepth");
            Validate.isPositiveOrNull(transferDirectoryMaxConcurrency, "transferDirectoryMaxConcurrency");
            Validate.isPositiveOrNull(multipartThresholdInBytes, "multipartThresholdInBytes");
            Validate.isPositiveOrNull(minimumPartSizeInBytes, "minimumPartSizeInBytes");
            Validate.isPositiveOrNull(maxInFlightParts, "maxInFlightParts");
            long partSize = minimumPartSizeInBytes != null ? minimumPartSizeInBytes
                                                           : TransferManagerConfiguration.DEFAULT_MINIMUM_PART_SIZE;
            // Parts are buffered in byte arrays (caller bodies) and addressed with int offsets.
            Validate.isTrue(partSize <= Integer.MAX_VALUE - 8, "minimumPartSizeInBytes must be less than 2 GiB");

            ExecutorService ownedExecutor = executor == null ? defaultExecutor() : null;
            S3AsyncClient ownedClient = null;
            S3AsyncClient client = s3Client;
            if (client == null) {
                try {
                    ownedClient = S3AsyncClient.create();
                } catch (RuntimeException e) {
                    if (ownedExecutor != null) {
                        ownedExecutor.shutdown();
                    }
                    throw e;
                }
                client = ownedClient;
            }
            TransferManagerConfiguration configuration = new TransferManagerConfiguration(
                client,
                ownedClient,
                executor != null ? executor : ownedExecutor,
                ownedExecutor,
                Boolean.TRUE.equals(uploadDirectoryFollowSymbolicLinks),
                uploadDirectoryMaxDepth != null ? uploadDirectoryMaxDepth : Integer.MAX_VALUE,
                transferDirectoryMaxConcurrency != null ? transferDirectoryMaxConcurrency
                                                        : TransferManagerConfiguration.DEFAULT_DIRECTORY_MAX_CONCURRENCY,
                multipartThresholdInBytes != null ? multipartThresholdInBytes
                                                  : TransferManagerConfiguration.DEFAULT_MULTIPART_THRESHOLD,
                partSize,
                maxInFlightParts != null ? maxInFlightParts : TransferManagerConfiguration.DEFAULT_MAX_IN_FLIGHT_PARTS);
            return new DefaultS3TransferManager(configuration);
        }

        /**
         * The transfer manager's own I/O pool: daemon threads that time out when idle. Its size bounds only blocking file
         * work, not transfer concurrency.
         */
        private static ExecutorService defaultExecutor() {
            int threads = Math.max(8, Runtime.getRuntime().availableProcessors());
            AtomicInteger counter = new AtomicInteger();
            ThreadFactory factory = runnable -> {
                Thread thread = new Thread(runnable, "s3-transfer-manager-io-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            };
            ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS,
                                                             new LinkedBlockingQueue<>(), factory);
            pool.allowCoreThreadTimeOut(true);
            return pool;
        }
    }
}
