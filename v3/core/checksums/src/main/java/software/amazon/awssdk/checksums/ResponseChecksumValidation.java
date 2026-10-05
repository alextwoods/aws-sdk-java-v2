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
 * When the SDK validates a response checksum for an {@code httpChecksum} operation, with the V2 constant names
 * (V2 {@code software.amazon.awssdk.core.checksums.ResponseChecksumValidation}).
 *
 * <p>A response is always validated when the caller sets the operation's validation-mode member to {@code ENABLED};
 * this setting decides whether the SDK sets that member implicitly when the caller leaves it unset.
 */
public enum ResponseChecksumValidation {
    /** Validate every response of an operation that supports validation (the default). */
    WHEN_SUPPORTED,
    /** Validate only when the caller enables validation through the operation's validation-mode member. */
    WHEN_REQUIRED;

    /**
     * The value named by {@code text}, ignoring case and surrounding white space ({@code when_supported},
     * {@code when_required}).
     *
     * @throws IllegalArgumentException if {@code text} names no value
     */
    public static ResponseChecksumValidation fromValue(String text) {
        String name = text.trim().toUpperCase(Locale.ROOT);
        for (ResponseChecksumValidation value : values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported response checksum validation; expected when_supported or "
                                           + "when_required");
    }
}
