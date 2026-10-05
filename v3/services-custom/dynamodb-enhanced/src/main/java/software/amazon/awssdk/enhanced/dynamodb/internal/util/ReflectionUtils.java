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

import java.util.Map;

/** Maps primitive classes to their wrapper classes. */
public final class ReflectionUtils {
    private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS = Map.of(
        boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class, double.class, Double.class,
        float.class, Float.class, int.class, Integer.class, long.class, Long.class, short.class, Short.class,
        void.class, Void.class);

    private ReflectionUtils() {
    }

    public static Class<?> getWrappedClass(Class<?> clazz) {
        return clazz.isPrimitive() ? PRIMITIVES_TO_WRAPPERS.getOrDefault(clazz, clazz) : clazz;
    }
}
