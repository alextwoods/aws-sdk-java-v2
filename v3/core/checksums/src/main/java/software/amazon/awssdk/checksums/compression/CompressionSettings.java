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

package software.amazon.awssdk.checksums.compression;

import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;

/**
 * The request compression settings one call uses: the Smithy {@code DISABLE_REQUEST_COMPRESSION} and
 * {@code REQUEST_MIN_COMPRESSION_SIZE_BYTES} client settings, after per-request overrides are merged.
 *
 * @param enabled false if request compression is disabled
 * @param minCompressionSizeBytes the smallest non-streaming payload that is compressed, 0 to 10485760 inclusive
 */
public record CompressionSettings(boolean enabled, int minCompressionSizeBytes) {
    /** The default minimum compressed payload size, 10240 bytes. */
    public static final int DEFAULT_MIN_COMPRESSION_SIZE_BYTES = 10_240;

    /** The largest allowed minimum compressed payload size, 10485760 bytes. */
    public static final int MAX_MIN_COMPRESSION_SIZE_BYTES = 10_485_760;

    /** Compression enabled with the default threshold. */
    public static final CompressionSettings DEFAULT = new CompressionSettings(true, DEFAULT_MIN_COMPRESSION_SIZE_BYTES);

    /** @throws SdkClientConfigurationException if the threshold is outside 0 to 10485760 */
    public CompressionSettings {
        requireValidMinimum(minCompressionSizeBytes);
    }

    /**
     * The settings of a resolved client or call configuration: {@link CompressionOptions#DISABLE_REQUEST_COMPRESSION}
     * and {@link CompressionOptions#REQUEST_MIN_COMPRESSION_SIZE_BYTES}, each defaulted when absent.
     *
     * @throws SdkClientConfigurationException if the threshold is outside 0 to 10485760
     */
    public static CompressionSettings from(SdkClientConfiguration configuration) {
        boolean disabled = configuration.get(CompressionOptions.DISABLE_REQUEST_COMPRESSION).orElse(false);
        int minimum = configuration.get(CompressionOptions.REQUEST_MIN_COMPRESSION_SIZE_BYTES)
                                   .orElse(DEFAULT_MIN_COMPRESSION_SIZE_BYTES);
        return new CompressionSettings(!disabled, minimum);
    }

    static int requireValidMinimum(int minCompressionSizeBytes) {
        if (minCompressionSizeBytes < 0 || minCompressionSizeBytes > MAX_MIN_COMPRESSION_SIZE_BYTES) {
            throw SdkClientConfigurationException.builder()
                                                 .option(CompressionOptions.REQUEST_MIN_COMPRESSION_SIZE_BYTES.name())
                                                 .message("The minimum compression size must be between 0 and "
                                                          + MAX_MIN_COMPRESSION_SIZE_BYTES + " bytes inclusive, "
                                                          + "but was " + minCompressionSizeBytes)
                                                 .build();
        }
        return minCompressionSizeBytes;
    }
}
