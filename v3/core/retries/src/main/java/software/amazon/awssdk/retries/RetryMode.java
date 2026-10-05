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

package software.amazon.awssdk.retries;

import java.util.Locale;
import java.util.Objects;

/**
 * The Modern Retry modes, named as in the shared AWS {@code retry_mode} setting. The pre-SRA {@code legacy} mode is
 * excluded from V3.
 */
public enum RetryMode {
    /** {@link StandardRetryStrategy}. */
    STANDARD("standard"),
    /** {@link AdaptiveRetryStrategy}. */
    ADAPTIVE("adaptive");

    private final String value;

    RetryMode(String value) {
        this.value = value;
    }

    /** The {@code retry_mode} setting value of this mode. */
    public String value() {
        return value;
    }

    /**
     * The mode named {@code value}, ignoring case.
     *
     * @throws IllegalArgumentException if {@code value} names no Modern Retry mode, including {@code legacy}
     */
    public static RetryMode fromValue(String value) {
        Objects.requireNonNull(value, "value");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (RetryMode mode : values()) {
            if (mode.value.equals(normalized)) {
                return mode;
            }
        }
        if ("legacy".equals(normalized)) {
            throw new IllegalArgumentException("The legacy retry mode is not supported by the AWS SDK for Java V3; "
                                               + "use retry_mode \"standard\" (StandardRetryStrategy) instead.");
        }
        throw new IllegalArgumentException("Unknown retry mode \"" + value + "\"; expected \"standard\" or "
                                           + "\"adaptive\".");
    }

    /** A builder of this mode's strategy with its defaults. */
    public RetryStrategy.Builder<?, ?> strategyBuilder() {
        return this == ADAPTIVE ? AdaptiveRetryStrategy.builder() : StandardRetryStrategy.builder();
    }
}
