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

package software.amazon.awssdk.utils;

import java.util.Arrays;

/**
 * Renders {@code ClassName(field=value, field2=value2)}, skipping {@code null} fields: the {@code toString} format of
 * generated types, identical to V2's {@code ToString}.
 *
 * <p>Byte arrays are rendered as {@code 0x} followed by lower-case hexadecimal digits and other arrays element by
 * element. Not thread safe; create one per rendering.
 */
public final class ToString {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final StringBuilder result;
    private final int startingLength;

    private ToString(String className) {
        this.result = new StringBuilder(className).append('(');
        this.startingLength = result.length();
    }

    /** {@code className()}: the rendering of an object without fields. */
    public static String create(String className) {
        return className + "()";
    }

    /** A rendering of an object of {@code className}. */
    public static ToString builder(String className) {
        return new ToString(className);
    }

    /** Adds {@code fieldName=field} unless {@code field} is {@code null}. */
    public ToString add(String fieldName, Object field) {
        if (field != null) {
            String value;
            if (field instanceof byte[] bytes) {
                value = "0x" + hex(bytes);
            } else if (field instanceof Object[] array) {
                value = Arrays.toString(array);
            } else if (field.getClass().isArray()) {
                value = primitiveArray(field);
            } else {
                value = String.valueOf(field);
            }
            result.append(fieldName).append('=').append(value).append(", ");
        }
        return this;
    }

    /** The rendering. */
    public String build() {
        if (result.length() > startingLength) {
            result.setLength(result.length() - 2);
        }
        return result.append(')').toString();
    }

    private static String primitiveArray(Object array) {
        return switch (array) {
            case int[] a -> Arrays.toString(a);
            case long[] a -> Arrays.toString(a);
            case short[] a -> Arrays.toString(a);
            case char[] a -> Arrays.toString(a);
            case boolean[] a -> Arrays.toString(a);
            case float[] a -> Arrays.toString(a);
            case double[] a -> Arrays.toString(a);
            default -> String.valueOf(array);
        };
    }

    private static String hex(byte[] data) {
        char[] chars = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            chars[2 * i] = HEX[(data[i] >> 4) & 0xF];
            chars[2 * i + 1] = HEX[data[i] & 0xF];
        }
        return new String(chars);
    }
}
