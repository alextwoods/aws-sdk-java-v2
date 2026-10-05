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

package software.amazon.awssdk.policybuilder.iam.internal;

import java.util.Collection;

/**
 * The argument checks of the policy builder, with the exception types and messages of the V2 {@code Validate} methods
 * the V2 policy builder used, so that V3 rejects invalid input exactly as V2 did. Not a public API.
 */
public final class Validate {
    private Validate() {
    }

    /**
     * Returns {@code object}.
     *
     * @throws NullPointerException {@code "<paramName> must not be null."} if it is null
     */
    public static <T> T paramNotNull(T object, String paramName) {
        if (object == null) {
            throw new NullPointerException(String.format("%s must not be null.", paramName));
        }
        return object;
    }

    /**
     * Returns {@code collection}.
     *
     * @throws NullPointerException with the formatted message if it is null
     * @throws IllegalArgumentException with the formatted message if it is empty
     */
    public static <T extends Collection<?>> T notEmpty(T collection, String message, Object... values) {
        if (collection == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return collection;
    }

    /**
     * Checks {@code expression}.
     *
     * @throws IllegalArgumentException with the formatted message if it is false
     */
    public static void isTrue(boolean expression, String message, Object... values) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }
}
