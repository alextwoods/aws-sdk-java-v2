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
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * The resolved settings of a transfer manager. {@code ownedClient} and {@code ownedExecutor} are non-null only for
 * resources the transfer manager created, which it closes.
 */
record TransferManagerConfiguration(S3AsyncClient s3Client,
                                    S3AsyncClient ownedClient,
                                    Executor executor,
                                    ExecutorService ownedExecutor,
                                    boolean uploadDirectoryFollowSymbolicLinks,
                                    int uploadDirectoryMaxDepth,
                                    int transferDirectoryMaxConcurrency,
                                    long multipartThresholdInBytes,
                                    long minimumPartSizeInBytes,
                                    int maxInFlightParts) {
    static final int DEFAULT_DIRECTORY_MAX_CONCURRENCY = 100;
    static final long DEFAULT_MULTIPART_THRESHOLD = 8L * 1024 * 1024;
    static final long DEFAULT_MINIMUM_PART_SIZE = 8L * 1024 * 1024;
    static final int DEFAULT_MAX_IN_FLIGHT_PARTS = 8;
    static final int MAX_PARTS = 10_000;

    /** The part size of an object of {@code length} bytes (unknown: {@code null}). */
    long partSize(Long length) {
        if (length == null) {
            return minimumPartSizeInBytes;
        }
        return Math.max(minimumPartSizeInBytes, (length + MAX_PARTS - 1) / MAX_PARTS);
    }
}
