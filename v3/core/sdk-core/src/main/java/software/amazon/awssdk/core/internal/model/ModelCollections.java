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

package software.amazon.awssdk.core.internal.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

/**
 * Collection state of generated model types (Requirement 4.10). Internal to generated code: not a public API.
 *
 * <p>Generated builders hold whatever collections callers pass; {@code build()} takes a deep snapshot with
 * {@link #copyList} and {@link #copyMap}, so the built value shares no mutable collection with any caller at any
 * nesting level. Snapshots are unmodifiable at every level, keep element order (and map insertion order), keep
 * {@code null} elements and values, and turn every nested collection into an unmodifiable list and every nested map
 * into an unmodifiable map. A snapshot that is already deeply immutable (one taken by this class, for example the value
 * {@code toBuilder()} passes back) is reused without copying, so copying a built value costs nothing.
 *
 * <p>Absence follows V2: a {@code null} or {@link SdkAutoConstructList}/{@link SdkAutoConstructMap} member is absent
 * (the snapshot is {@code null}), and {@link #orAutoConstruct} turns an absent member into the empty auto-construct
 * collection that accessors return. A nested auto-construct collection is an empty collection.
 */
public final class ModelCollections {

    private ModelCollections() {
    }

    /**
     * The deep snapshot of a list member; {@code null} if the member is absent. The element type is the declared type
     * of the member: nested collections and maps become lists and maps.
     */
    public static <T> List<T> copyList(Collection<?> source) {
        if (source == null || source instanceof SdkAutoConstructList) {
            return null;
        }
        return nestedList(source);
    }

    /** The deep snapshot of a map member; {@code null} if the member is absent. */
    public static <K, V> Map<K, V> copyMap(Map<?, ?> source) {
        if (source == null || source instanceof SdkAutoConstructMap) {
            return null;
        }
        return nestedMap(source);
    }

    /**
     * An unmodifiable view of {@code fresh}, a list a generated deserializer just created and hands over: no copy is
     * taken, so no other object may reference {@code fresh}. Nested lists and maps must have been adopted the same way
     * to be reused by a later snapshot.
     */
    public static <T> List<T> adoptList(List<T> fresh) {
        if (fresh == null) {
            return null;
        }
        return fresh.isEmpty() ? empty() : new ModelList<>(fresh, allDeep(fresh));
    }

    /** An unmodifiable view of {@code fresh}, a map a generated deserializer just created; see {@link #adoptList}. */
    public static <K, V> Map<K, V> adoptMap(Map<K, V> fresh) {
        if (fresh == null) {
            return null;
        }
        return fresh.isEmpty() ? emptyMap() : new ModelMap<>(fresh, allDeep(fresh.values()));
    }

    /** {@code list}, or the empty {@link DefaultSdkAutoConstructList} if it is absent. */
    public static <T> List<T> orAutoConstruct(List<T> list) {
        return list != null ? list : DefaultSdkAutoConstructList.getInstance();
    }

    /** {@code map}, or the empty {@link DefaultSdkAutoConstructMap} if it is absent. */
    public static <K, V> Map<K, V> orAutoConstruct(Map<K, V> map) {
        return map != null ? map : DefaultSdkAutoConstructMap.getInstance();
    }

    /**
     * A new unmodifiable list of {@code element} applied to every element of {@code source} (used to convert between
     * enum values and their modeled values); {@code null} if {@code source} is absent. {@code element} must return
     * deeply immutable values.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> convertList(Collection<?> source, Function<Object, ?> element) {
        if (source == null || source instanceof SdkAutoConstructList) {
            return null;
        }
        if (source.isEmpty()) {
            return empty();
        }
        List<Object> result = new ArrayList<>(source.size());
        for (Object e : source) {
            result.add(element.apply(e));
        }
        return (List<T>) new ModelList<>(result, allDeep(result));
    }

    /**
     * A new unmodifiable map of {@code key} and {@code value} applied to every entry of {@code source}; {@code null}
     * if {@code source} is absent. Entries whose converted keys collide keep the last value, in iteration order.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> convertMap(Map<?, ?> source, Function<Object, ?> key, Function<Object, ?> value) {
        if (source == null || source instanceof SdkAutoConstructMap) {
            return null;
        }
        if (source.isEmpty()) {
            return emptyMap();
        }
        Map<Object, Object> result = new LinkedHashMap<>(capacity(source.size()));
        source.forEach((k, v) -> result.put(key.apply(k), value.apply(v)));
        return (Map<K, V>) new ModelMap<>(result, allDeep(result.values()));
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> nestedList(Collection<?> source) {
        if (source instanceof ModelList<?> list && list.deep) {
            return (List<T>) list;
        }
        if (source.isEmpty()) {
            return empty();
        }
        List<Object> copy = new ArrayList<>(source.size());
        for (Object e : source) {
            copy.add(nested(e));
        }
        return (List<T>) new ModelList<>(copy, true);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> nestedMap(Map<?, ?> source) {
        if (source instanceof ModelMap<?, ?> map && map.deep) {
            return (Map<K, V>) map;
        }
        if (source.isEmpty()) {
            return emptyMap();
        }
        Map<Object, Object> copy = new LinkedHashMap<>(capacity(source.size()));
        for (Map.Entry<?, ?> e : source.entrySet()) {
            copy.put(e.getKey(), nested(e.getValue()));
        }
        return (Map<K, V>) new ModelMap<>(copy, true);
    }

    private static Object nested(Object value) {
        if (value instanceof Collection<?> collection) {
            return nestedList(collection);
        } else if (value instanceof Map<?, ?> map) {
            return nestedMap(map);
        }
        return value;
    }

    private static boolean allDeep(Collection<?> values) {
        for (Object value : values) {
            if (!isDeep(value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDeep(Object value) {
        if (value instanceof ModelList<?> list) {
            return list.deep;
        } else if (value instanceof ModelMap<?, ?> map) {
            return map.deep;
        }
        return !(value instanceof Collection || value instanceof Map)
               || value instanceof DefaultSdkAutoConstructList || value instanceof DefaultSdkAutoConstructMap;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> empty() {
        return (List<T>) ModelList.EMPTY;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> emptyMap() {
        return (Map<K, V>) ModelMap.EMPTY;
    }

    private static int capacity(int size) {
        return size < 3 ? size + 1 : (int) (size / 0.75f + 1.0f);
    }
}
