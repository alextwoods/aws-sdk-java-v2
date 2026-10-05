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

import java.util.Locale;

/**
 * When the SDK calculates a request checksum for an {@code httpChecksum} operation, with the V2 constant names
 * (V2 {@code software.amazon.awssdk.core.checksums.RequestChecksumCalculation}).
 *
 * <p>A checksum is always calculated when the caller selects an algorithm or the operation requires one
 * ({@code requestChecksumRequired} or {@code httpChecksumRequired}); this setting only decides the optional case.
 */
public enum RequestChecksumCalculation {
    /** Calculate a checksum for every operation that supports one (the default). */
    WHEN_SUPPORTED,
    /** Calculate a checksum only when the caller selects an algorithm or the operation requires one. */
    WHEN_REQUIRED;

    /**
     * The value named by {@code text}, ignoring case and surrounding white space, as the environment, JVM property
     * and profile sources spell it ({@code when_supported}, {@code when_required}).
     *
     * @throws IllegalArgumentException if {@code text} names no value
     */
    public static RequestChecksumCalculation fromValue(String text) {
        String name = text.trim().toUpperCase(Locale.ROOT);
        for (RequestChecksumCalculation value : values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported request checksum calculation; expected when_supported or "
                                           + "when_required");
    }
}
