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

package software.amazon.awssdk.internal.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * A header store backed by a flat, strided {@code String[]}: even indices hold header names (original casing), odd
 * indices hold values. The array is kept sorted case-insensitively by name, with multiple values for one name stored
 * as adjacent pairs in insertion order.
 *
 * <p>This replaces a {@code TreeMap<String, List<String>>} representation ({@link LowCopyListMap}) on the request
 * path. The map representation pays a red-black tree node per header and a full tree clone on the first mutation of
 * every builder derived from a built request; here the same copy-on-write transition is a single array copy, and all
 * lookups are scans or binary searches over one contiguous array.
 *
 * <p>Behavioral parity with the map representation, which callers observe through {@code SdkHttpRequest}:
 * <ul>
 *     <li>Iteration (and the materialized {@link ForBuildable#externalMap()}) is sorted case-insensitively by name,
 *     matching {@code TreeMap(String.CASE_INSENSITIVE_ORDER)} and the {@code SdkHttpHeaders#headers()} javadoc.</li>
 *     <li>Replacing a header whose name differs only by case keeps the previously stored name, matching
 *     {@code TreeMap.put} which retains the existing key.</li>
 *     <li>A name mapped to an <i>empty</i> value list is representable (as a single pair whose value is the
 *     {@link #EMPTY_MARKER} sentinel), matching {@code map.put(name, emptyList())}.</li>
 *     <li>Multi-value insertion order is preserved through build/toBuilder sharing.</li>
 * </ul>
 *
 * <p>Structure mirrors {@link LowCopyListMap}: {@link ForBuilder} lives in builders and supports mutation with
 * copy-on-write; {@link ForBuildable} lives in built (immutable) objects and shares the array with the builder that
 * created it and any builders later derived from it.
 */
@SdkInternalApi
public final class StridedHeaders {
    /**
     * Sentinel value marking a name that is present with an empty value list. Compared by identity; a real empty
     * string value is a different instance.
     */
    private static final String EMPTY_MARKER = new String(new char[0]);

    private static final String[] EMPTY_PAIRS = new String[0];

    private static final int DEFAULT_CAPACITY_PAIRS = 16;

    private StridedHeaders() {
    }

    /**
     * Create an empty {@link ForBuilder}.
     */
    public static ForBuilder emptyHeaders() {
        return new ForBuilder();
    }

    private static int compareNames(String left, String right) {
        return String.CASE_INSENSITIVE_ORDER.compare(left, right);
    }

    /**
     * First pair index whose name compares greater than or equal to {@code name}; also the insertion point when the
     * name is absent.
     */
    private static int findRunStart(String[] pairs, int pairCount, String name) {
        int lo = 0;
        int hi = pairCount;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (compareNames(pairs[mid * 2], name) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private static int runEnd(String[] pairs, int pairCount, int runStart, String name) {
        int i = runStart;
        while (i < pairCount && compareNames(pairs[i * 2], name) == 0) {
            i++;
        }
        return i;
    }

    private static String firstValue(String[] pairs, int pairCount, String name) {
        int start = findRunStart(pairs, pairCount, name);
        if (start >= pairCount || compareNames(pairs[start * 2], name) != 0) {
            return null;
        }
        String value = pairs[start * 2 + 1];
        return value == EMPTY_MARKER ? null : value;
    }

    private static List<String> valuesFor(String[] pairs, int pairCount, String name) {
        int start = findRunStart(pairs, pairCount, name);
        if (start >= pairCount || compareNames(pairs[start * 2], name) != 0) {
            return Collections.emptyList();
        }
        int end = runEnd(pairs, pairCount, start, name);
        return valuesOfRun(pairs, start, end);
    }

    private static List<String> valuesOfRun(String[] pairs, int runStart, int runEnd) {
        String first = pairs[runStart * 2 + 1];
        if (first == EMPTY_MARKER) {
            return Collections.emptyList();
        }
        if (runEnd - runStart == 1) {
            return Collections.singletonList(first);
        }
        List<String> values = new ArrayList<>(runEnd - runStart);
        for (int i = runStart; i < runEnd; i++) {
            values.add(pairs[i * 2 + 1]);
        }
        return Collections.unmodifiableList(values);
    }

    private static void forEachEntry(String[] pairs, int pairCount, HeaderEntryConsumer consumer) {
        for (int i = 0; i < pairCount; i++) {
            String value = pairs[i * 2 + 1];
            if (value == EMPTY_MARKER) {
                continue;
            }
            consumer.accept(pairs[i * 2], value);
        }
    }

    private static void forEach(String[] pairs, int pairCount, BiConsumer<? super String, ? super List<String>> consumer) {
        int i = 0;
        while (i < pairCount) {
            String name = pairs[i * 2];
            int end = i + 1;
            while (end < pairCount && compareNames(pairs[end * 2], name) == 0) {
                end++;
            }
            consumer.accept(name, valuesOfRun(pairs, i, end));
            i = end;
        }
    }

    private static boolean anyName(String[] pairs, int pairCount, Predicate<String> predicate) {
        int i = 0;
        while (i < pairCount) {
            String name = pairs[i * 2];
            if (predicate.test(name)) {
                return true;
            }
            int end = i + 1;
            while (end < pairCount && compareNames(pairs[end * 2], name) == 0) {
                end++;
            }
            i = end;
        }
        return false;
    }

    private static int distinctNames(String[] pairs, int pairCount) {
        int names = 0;
        int i = 0;
        while (i < pairCount) {
            names++;
            String name = pairs[i * 2];
            int end = i + 1;
            while (end < pairCount && compareNames(pairs[end * 2], name) == 0) {
                end++;
            }
            i = end;
        }
        return names;
    }

    /**
     * Materialize the deep-unmodifiable, case-insensitively sorted map representation that {@code headers()} exposes.
     * Equivalent to {@code CollectionUtils.deepUnmodifiableMap} over a {@code TreeMap(String.CASE_INSENSITIVE_ORDER)}.
     */
    private static Map<String, List<String>> toExternalMap(String[] pairs, int pairCount) {
        Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int i = 0;
        while (i < pairCount) {
            String name = pairs[i * 2];
            int end = i + 1;
            while (end < pairCount && compareNames(pairs[end * 2], name) == 0) {
                end++;
            }
            map.put(name, valuesOfRun(pairs, i, end));
            i = end;
        }
        return Collections.unmodifiableMap(map);
    }

    private static boolean pairsEqual(String[] left, int leftCount, String[] right, int rightCount) {
        if (leftCount != rightCount) {
            return false;
        }
        for (int i = 0; i < leftCount; i++) {
            if (compareNames(left[i * 2], right[i * 2]) != 0) {
                return false;
            }
            String lv = left[i * 2 + 1];
            String rv = right[i * 2 + 1];
            if (lv == null ? rv != null : !lv.equals(rv)) {
                return false;
            }
        }
        return true;
    }

    private static int pairsHashCode(String[] pairs, int pairCount) {
        int h = 1;
        for (int i = 0; i < pairCount; i++) {
            h = 31 * h + caseInsensitiveHash(pairs[i * 2]);
            String value = pairs[i * 2 + 1];
            h = 31 * h + (value == null ? 0 : value.hashCode());
        }
        return h;
    }

    private static int caseInsensitiveHash(String name) {
        int h = 0;
        for (int i = 0; i < name.length(); i++) {
            h = 31 * h + Character.toLowerCase(Character.toUpperCase(name.charAt(i)));
        }
        return h;
    }

    /**
     * The mutable half, held by request builders.
     */
    @NotThreadSafe
    public static final class ForBuilder {
        private String[] pairs;
        private int pairCount;

        /**
         * Whether {@link #pairs} is shared with a {@link ForBuildable} (or a builder derived from the same one) and
         * must be copied before any mutation.
         */
        private boolean shared;

        private ForBuilder() {
            this.pairs = EMPTY_PAIRS;
            this.pairCount = 0;
            this.shared = false;
        }

        private ForBuilder(ForBuildable source) {
            this.pairs = source.pairs;
            this.pairCount = source.pairCount;
            this.shared = true;
        }

        /**
         * Replace all values of {@code name}, keeping the previously stored name when one exists (matching
         * {@code TreeMap.put}, which retains the existing key on a comparator-equal put). An empty {@code values}
         * list stores the name mapped to no values.
         */
        public void put(String name, List<String> values) {
            int start = findRunStart(pairs, pairCount, name);
            int end = start < pairCount && compareNames(pairs[start * 2], name) == 0
                      ? runEnd(pairs, pairCount, start, name)
                      : start;
            String storedName = end > start ? pairs[start * 2] : name;
            int added = Math.max(values.size(), 1);
            splice(start, end - start, added);
            int slot = start * 2;
            if (values.isEmpty()) {
                pairs[slot] = storedName;
                pairs[slot + 1] = EMPTY_MARKER;
            } else {
                for (int i = 0; i < values.size(); i++) {
                    pairs[slot] = storedName;
                    pairs[slot + 1] = values.get(i);
                    slot += 2;
                }
            }
        }

        /**
         * Append one value to {@code name}, creating the name when absent (matching
         * {@code map.computeIfAbsent(name, k -> new ArrayList<>()).add(value)}).
         */
        public void append(String name, String value) {
            int start = findRunStart(pairs, pairCount, name);
            boolean exists = start < pairCount && compareNames(pairs[start * 2], name) == 0;
            if (!exists) {
                splice(start, 0, 1);
                pairs[start * 2] = name;
                pairs[start * 2 + 1] = value;
                return;
            }
            String storedName = pairs[start * 2];
            int end = runEnd(pairs, pairCount, start, name);
            if (end - start == 1 && pairs[start * 2 + 1] == EMPTY_MARKER) {
                ensureOwned();
                pairs[start * 2 + 1] = value;
                return;
            }
            splice(end, 0, 1);
            pairs[end * 2] = storedName;
            pairs[end * 2 + 1] = value;
        }

        /**
         * Remove all values of {@code name}.
         */
        public void remove(String name) {
            int start = findRunStart(pairs, pairCount, name);
            if (start >= pairCount || compareNames(pairs[start * 2], name) != 0) {
                return;
            }
            int end = runEnd(pairs, pairCount, start, name);
            splice(start, end - start, 0);
        }

        public void clear() {
            this.pairs = EMPTY_PAIRS;
            this.pairCount = 0;
            this.shared = false;
        }

        /**
         * Replace the contents with a copy of an externally owned map.
         */
        public void setFromExternal(Map<String, List<String>> map) {
            clear();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        public String firstValue(String name) {
            return StridedHeaders.firstValue(pairs, pairCount, name);
        }

        public List<String> valuesFor(String name) {
            return StridedHeaders.valuesFor(pairs, pairCount, name);
        }

        public void forEach(BiConsumer<? super String, ? super List<String>> consumer) {
            StridedHeaders.forEach(pairs, pairCount, consumer);
        }

        public boolean anyName(Predicate<String> predicate) {
            return StridedHeaders.anyName(pairs, pairCount, predicate);
        }

        public int distinctNames() {
            return StridedHeaders.distinctNames(pairs, pairCount);
        }

        /**
         * Snapshot of the current contents in the external map shape. Unlike the map-backed representation this is
         * not a live view; each call materializes the current state.
         */
        public Map<String, List<String>> externalMapSnapshot() {
            return toExternalMap(pairs, pairCount);
        }

        public ForBuildable forBuildable() {
            this.shared = true;
            return new ForBuildable(this);
        }

        /**
         * Open a gap of {@code addedPairs} pairs at {@code startPair}, removing {@code removedPairs} pairs there.
         * Handles ownership: a shared array is copied exactly once, and capacity grows as needed. Gap slots must be
         * filled by the caller before any read.
         */
        private void splice(int startPair, int removedPairs, int addedPairs) {
            int newCount = pairCount - removedPairs + addedPairs;
            int neededSlots = newCount * 2;
            if (shared || neededSlots > pairs.length) {
                int capacity = Math.max(DEFAULT_CAPACITY_PAIRS * 2, pairs.length);
                while (capacity < neededSlots) {
                    capacity *= 2;
                }
                String[] copy = new String[capacity];
                System.arraycopy(pairs, 0, copy, 0, startPair * 2);
                System.arraycopy(pairs, (startPair + removedPairs) * 2, copy, (startPair + addedPairs) * 2,
                                 (pairCount - startPair - removedPairs) * 2);
                this.pairs = copy;
                this.shared = false;
            } else if (removedPairs != addedPairs) {
                System.arraycopy(pairs, (startPair + removedPairs) * 2, pairs, (startPair + addedPairs) * 2,
                                 (pairCount - startPair - removedPairs) * 2);
                if (addedPairs < removedPairs) {
                    for (int i = neededSlots; i < pairCount * 2; i++) {
                        pairs[i] = null;
                    }
                }
            }
            this.pairCount = newCount;
        }

        /**
         * Take ownership of the array without changing contents.
         */
        private void ensureOwned() {
            if (shared) {
                splice(0, 0, 0);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ForBuilder that = (ForBuilder) o;
            return pairsEqual(pairs, pairCount, that.pairs, that.pairCount);
        }

        @Override
        public int hashCode() {
            return pairsHashCode(pairs, pairCount);
        }
    }

    /**
     * The immutable half, held by built requests. Shares the pair array with the builder that created it; the
     * builder (and any builder later derived from this object) copies before mutating.
     */
    @ThreadSafe
    public static final class ForBuildable {
        private final String[] pairs;
        private final int pairCount;

        /**
         * Cached result of {@link #externalMap()}. Lazily initialized with a benign race: concurrent callers may
         * each build the map, but the results are equal and immutable, so whichever write wins is correct. A plain
         * volatile field instead of {@link software.amazon.awssdk.utils.Lazy} because this object is created on
         * every {@code build()}, and the wrapper plus its capturing lambda are measurable per-request garbage.
         */
        private volatile Map<String, List<String>> externalMap;

        private ForBuildable(ForBuilder builder) {
            this.pairs = builder.pairs;
            this.pairCount = builder.pairCount;
        }

        public String firstValue(String name) {
            return StridedHeaders.firstValue(pairs, pairCount, name);
        }

        public List<String> valuesFor(String name) {
            return StridedHeaders.valuesFor(pairs, pairCount, name);
        }

        public void forEach(BiConsumer<? super String, ? super List<String>> consumer) {
            StridedHeaders.forEach(pairs, pairCount, consumer);
        }

        /**
         * One call per name/value pair, in the same order {@link #forEach} would produce, skipping names mapped to an
         * empty value list.
         */
        public void forEachEntry(HeaderEntryConsumer consumer) {
            StridedHeaders.forEachEntry(pairs, pairCount, consumer);
        }

        public int distinctNames() {
            return StridedHeaders.distinctNames(pairs, pairCount);
        }

        public Map<String, List<String>> externalMap() {
            Map<String, List<String>> map = externalMap;
            if (map == null) {
                map = toExternalMap(pairs, pairCount);
                externalMap = map;
            }
            return map;
        }

        public ForBuilder forBuilder() {
            return new ForBuilder(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ForBuildable that = (ForBuildable) o;
            return pairsEqual(pairs, pairCount, that.pairs, that.pairCount);
        }

        @Override
        public int hashCode() {
            return pairsHashCode(pairs, pairCount);
        }
    }
}
