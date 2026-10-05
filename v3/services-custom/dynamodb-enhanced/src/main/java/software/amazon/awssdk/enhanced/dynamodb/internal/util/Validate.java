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

package software.amazon.awssdk.enhanced.dynamodb.internal.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Argument and state checks used by the enhanced client. The exception types and messages are those of the V2
 * {@code software.amazon.awssdk.utils.Validate} methods the V2 enhanced client called, so V3 callers observe the same
 * failures. V3 core does not publish a validation utility, so this module keeps its own internal copy.
 */
public final class Validate {
    private static final String DEFAULT_IS_NULL_EX_MESSAGE = "The validated object is null";

    private Validate() {
    }

    public static <T, U> U isInstanceOf(Class<U> type, T obj, String message, Object... values) {
        if (!type.isInstance(obj)) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return type.cast(obj);
    }

    public static void isTrue(boolean expression, String message, Object... values) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }

    public static <T> T[] noNullElements(T[] array, String message, Object... values) {
        notNull(array, message);
        for (T element : array) {
            if (element == null) {
                throw new IllegalArgumentException(String.format(message, values));
            }
        }
        return array;
    }

    public static <T extends Iterable<?>> T noNullElements(T iterable, String message, Object... values) {
        notNull(iterable, DEFAULT_IS_NULL_EX_MESSAGE);
        for (Object element : iterable) {
            if (element == null) {
                throw new IllegalArgumentException(String.format(message, values));
            }
        }
        return iterable;
    }

    public static <T> T[] notEmpty(T[] array, String message, Object... values) {
        if (array == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (array.length == 0) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return array;
    }

    public static <T extends Collection<?>> T notEmpty(T collection, String message, Object... values) {
        if (collection == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return collection;
    }

    public static <T extends Map<?, ?>> T notEmpty(T map, String message, Object... values) {
        if (map == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return map;
    }

    public static <T extends CharSequence> T notEmpty(T chars, String message, Object... values) {
        if (chars == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (chars.length() == 0) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return chars;
    }

    public static <T> T notNull(T object, String message, Object... values) {
        if (object == null) {
            throw new NullPointerException(String.format(message, values));
        }
        return object;
    }

    public static <T extends CharSequence> T paramNotBlank(T chars, String paramName) {
        if (chars == null) {
            throw new NullPointerException(String.format("%s must not be null.", paramName));
        }
        if (chars.toString().isBlank()) {
            throw new IllegalArgumentException(String.format("%s must not be blank or empty.", paramName));
        }
        return chars;
    }

    public static <T> T paramNotNull(T object, String paramName) {
        if (object == null) {
            throw new NullPointerException(String.format("%s must not be null.", paramName));
        }
        return object;
    }

    public static <T> T validState(T object, Predicate<T> test, String message, Object... values) {
        if (!test.test(object)) {
            throw new IllegalStateException(String.format(message, values));
        }
        return object;
    }

    public static void validState(boolean expression, String message, Object... values) {
        if (!expression) {
            throw new IllegalStateException(String.format(message, values));
        }
    }
}
