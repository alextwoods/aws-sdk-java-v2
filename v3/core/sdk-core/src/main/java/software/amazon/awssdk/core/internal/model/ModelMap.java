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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An unmodifiable, insertion-ordered map over a private backing map that no other object references. Every mutator
 * (including those of the key, value and entry views, their iterators and the entries themselves) throws
 * {@link UnsupportedOperationException}, even when it would not change the map.
 *
 * <p>{@link #deep} records whether every value is itself deeply immutable as far as collections go; only deep
 * instances are reused without copying. Keys are strings or enum values.
 */
final class ModelMap<K, V> extends AbstractMap<K, V> {

    static final ModelMap<Object, Object> EMPTY = new ModelMap<>(new LinkedHashMap<>(), true);

    private final Map<K, V> view;
    final boolean deep;

    ModelMap(Map<K, V> entries, boolean deep) {
        this.view = Collections.unmodifiableMap(entries);
        this.deep = deep;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return view.entrySet();
    }

    @Override
    public Set<K> keySet() {
        return view.keySet();
    }

    @Override
    public Collection<V> values() {
        return view.values();
    }

    @Override
    public int size() {
        return view.size();
    }

    @Override
    public boolean isEmpty() {
        return view.isEmpty();
    }

    @Override
    public V get(Object key) {
        return view.get(key);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return view.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean containsKey(Object key) {
        return view.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return view.containsValue(value);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        view.forEach(action);
    }

    // Every mutator throws unconditionally, as V2's Collections.unmodifiableMap does. AbstractMap's and Map's defaults
    // return normally when they would not change the map (for example putAll({}) or remove of an absent key).

    @Override
    public V put(K key, V value) {
        throw unsupported();
    }

    @Override
    public V remove(Object key) {
        throw unsupported();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw unsupported();
    }

    @Override
    public void clear() {
        throw unsupported();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        throw unsupported();
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw unsupported();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        throw unsupported();
    }

    @Override
    public V replace(K key, V value) {
        throw unsupported();
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        throw unsupported();
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        throw unsupported();
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw unsupported();
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw unsupported();
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        throw unsupported();
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Modeled collections are immutable");
    }
}
