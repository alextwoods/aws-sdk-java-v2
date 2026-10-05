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

package software.amazon.awssdk.transfer.s3.internal.util;

import java.util.function.Supplier;

/**
 * Argument checks with the V2 {@code software.amazon.awssdk.utils.Validate} messages, for the members the Transfer
 * Manager uses (V3 {@code sdk-core} does not publish a general validation utility). Not a public API.
 */
public final class Validate {
    private Validate() {
    }

    /** {@code object}, or {@link NullPointerException} "{@code paramName} must not be null." */
    public static <T> T paramNotNull(T object, String paramName) {
        if (object == null) {
            throw new NullPointerException(String.format("%s must not be null.", paramName));
        }
        return object;
    }

    /** {@code num}, or {@link IllegalArgumentException} if it is negative. */
    public static long isNotNegative(long num, String fieldName) {
        if (num < 0) {
            throw new IllegalArgumentException(String.format("%s must not be negative", fieldName));
        }
        return num;
    }

    /** {@code num}, or {@link IllegalArgumentException} if it is not null and not positive. */
    public static Long isPositiveOrNull(Long num, String fieldName) {
        if (num != null && num <= 0) {
            throw new IllegalArgumentException(String.format("%s must be positive", fieldName));
        }
        return num;
    }

    /** {@code num}, or {@link IllegalArgumentException} if it is not null and not positive. */
    public static Integer isPositiveOrNull(Integer num, String fieldName) {
        if (num != null && num <= 0) {
            throw new IllegalArgumentException(String.format("%s must be positive", fieldName));
        }
        return num;
    }

    /** Throws {@link IllegalArgumentException} with the formatted message unless {@code expression} holds. */
    public static void isTrue(boolean expression, String message, Object... values) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }

    /** {@code value}, or the default when it is null. */
    public static <T> T getOrDefault(T value, Supplier<T> defaultValue) {
        return value != null ? value : defaultValue.get();
    }
}
