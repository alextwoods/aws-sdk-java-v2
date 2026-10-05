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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** The V2 {@code software.amazon.awssdk.utils.CollectionUtils} operations the enhanced client uses. */
public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNullOrEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    /** A copy of {@code map} whose list values are copied too, in the source's iteration order. */
    public static <T, U> Map<T, List<U>> deepCopyMap(Map<T, ? extends List<U>> map) {
        Map<T, List<U>> result = new LinkedHashMap<>(map.size());
        map.forEach((k, v) -> result.put(k, new ArrayList<>(v)));
        return result;
    }

    /** An unmodifiable deep copy of {@code map} whose list values are unmodifiable too. */
    public static <T, U> Map<T, List<U>> deepUnmodifiableMap(Map<T, ? extends List<U>> map) {
        Map<T, List<U>> result = new LinkedHashMap<>(map.size());
        map.forEach((k, v) -> result.put(k, Collections.unmodifiableList(new ArrayList<>(v))));
        return Collections.unmodifiableMap(result);
    }

    public static <K, V> Map<K, V> filterMap(Map<K, V> map, Predicate<Map.Entry<K, V>> condition) {
        return map.entrySet()
                  .stream()
                  .filter(condition)
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <K, V> Map<K, V> inverseMap(Map<V, K> inputMap) {
        return inputMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
}
