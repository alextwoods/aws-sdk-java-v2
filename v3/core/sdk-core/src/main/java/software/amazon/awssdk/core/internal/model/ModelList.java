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

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * An unmodifiable list over a private backing list that no other object references. Every mutator (including those of
 * iterators and sub-lists) throws {@link UnsupportedOperationException}, even when it would not change the list (for
 * example {@code clear()} of an empty list or {@code addAll} of an empty collection), as V2's
 * {@code Collections.unmodifiableList} does; {@link AbstractList}'s own mutators return normally in those cases.
 *
 * <p>{@link #deep} records whether every element is itself deeply immutable as far as collections go (no element is
 * a collection or map other than a deep {@code ModelList} or {@code ModelMap}); only deep instances are reused
 * without copying.
 */
final class ModelList<E> extends AbstractList<E> implements RandomAccess {

    static final ModelList<Object> EMPTY = new ModelList<>(List.of(), true);

    private final List<E> elements;
    final boolean deep;

    ModelList(List<E> elements, boolean deep) {
        this.elements = elements;
        this.deep = deep;
    }

    @Override
    public E get(int index) {
        return elements.get(index);
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean contains(Object o) {
        return elements.contains(o);
    }

    @Override
    public int indexOf(Object o) {
        return elements.indexOf(o);
    }

    @Override
    public Object[] toArray() {
        return elements.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return elements.toArray(a);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        elements.forEach(action);
    }

    /** An unmodifiable view: {@link AbstractList}'s sub-list accepts no-op mutations such as {@code addAll([])}. */
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return Collections.unmodifiableList(elements.subList(fromIndex, toIndex));
    }

    /** An unmodifiable view: the default reverse-ordered view accepts no-op mutations such as {@code addAll([])}. */
    @Override
    public List<E> reversed() {
        return new ModelList<>(elements.reversed(), deep);
    }

    // Every mutator throws unconditionally. AbstractList's iterators mutate through set, add and remove(int).

    @Override
    public boolean add(E e) {
        throw unsupported();
    }

    @Override
    public void add(int index, E element) {
        throw unsupported();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw unsupported();
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw unsupported();
    }

    @Override
    public E set(int index, E element) {
        throw unsupported();
    }

    @Override
    public E remove(int index) {
        throw unsupported();
    }

    @Override
    public boolean remove(Object o) {
        throw unsupported();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw unsupported();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw unsupported();
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw unsupported();
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        throw unsupported();
    }

    @Override
    public void sort(Comparator<? super E> c) {
        throw unsupported();
    }

    @Override
    public void clear() {
        throw unsupported();
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        throw unsupported();
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Modeled collections are immutable");
    }
}
