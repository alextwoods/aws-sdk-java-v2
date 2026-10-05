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

import java.util.Locale;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.ClientSetting;
import software.amazon.awssdk.core.override.CallOption;

/**
 * The client options of request compression, how each is resolved from its sources (the V2 JVM property,
 * environment variable and profile property names), and the call options a request override may set. Per-request
 * values take precedence over client values, as the Smithy {@code requestCompression} client settings require.
 */
public final class CompressionOptions {
    /** True disables request compression (Smithy {@code DISABLE_REQUEST_COMPRESSION}). */
    public static final ClientOption<Boolean> DISABLE_REQUEST_COMPRESSION =
        ClientOption.create("DisableRequestCompression", Boolean.class);

    /** The smallest non-streaming payload that is compressed (Smithy {@code REQUEST_MIN_COMPRESSION_SIZE_BYTES}). */
    public static final ClientOption<Integer> REQUEST_MIN_COMPRESSION_SIZE_BYTES =
        ClientOption.create("RequestMinCompressionSizeBytes", Integer.class);

    /**
     * {@link #DISABLE_REQUEST_COMPRESSION} from builder value, JVM property {@code aws.disableRequestCompression},
     * environment variable {@code AWS_DISABLE_REQUEST_COMPRESSION}, profile property
     * {@code disable_request_compression}, then {@code false}.
     */
    public static final ClientSetting<Boolean> DISABLE_REQUEST_COMPRESSION_SETTING =
        ClientSetting.builder(DISABLE_REQUEST_COMPRESSION)
                     .jvmProperty("aws.disableRequestCompression")
                     .environmentVariable("AWS_DISABLE_REQUEST_COMPRESSION")
                     .profileProperty("disable_request_compression")
                     .parser(CompressionOptions::parseBoolean)
                     .defaultValue(false)
                     .build();

    /**
     * {@link #REQUEST_MIN_COMPRESSION_SIZE_BYTES} from builder value, JVM property
     * {@code aws.requestMinCompressionSizeBytes}, environment variable {@code AWS_REQUEST_MIN_COMPRESSION_SIZE_BYTES},
     * profile property {@code request_min_compression_size_bytes}, then 10240. Text outside 0 to 10485760 is
     * rejected.
     */
    public static final ClientSetting<Integer> REQUEST_MIN_COMPRESSION_SIZE_BYTES_SETTING =
        ClientSetting.builder(REQUEST_MIN_COMPRESSION_SIZE_BYTES)
                     .jvmProperty("aws.requestMinCompressionSizeBytes")
                     .environmentVariable("AWS_REQUEST_MIN_COMPRESSION_SIZE_BYTES")
                     .profileProperty("request_min_compression_size_bytes")
                     .parser(text -> CompressionSettings.requireValidMinimum(Integer.parseInt(text.trim())))
                     .defaultValue(CompressionSettings.DEFAULT_MIN_COMPRESSION_SIZE_BYTES)
                     .build();

    /** The per-request {@link #DISABLE_REQUEST_COMPRESSION}, replacing the client value for one call. */
    public static final CallOption<Boolean> DISABLE_REQUEST_COMPRESSION_CALL =
        CallOption.replacing(DISABLE_REQUEST_COMPRESSION);

    /** The per-request {@link #REQUEST_MIN_COMPRESSION_SIZE_BYTES}, replacing the client value for one call. */
    public static final CallOption<Integer> REQUEST_MIN_COMPRESSION_SIZE_BYTES_CALL =
        CallOption.replacing(REQUEST_MIN_COMPRESSION_SIZE_BYTES);

    private CompressionOptions() {
    }

    private static Boolean parseBoolean(String text) {
        String value = text.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("Expected true or false");
        };
    }
}
