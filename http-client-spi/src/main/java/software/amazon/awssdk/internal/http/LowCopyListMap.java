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

import static software.amazon.awssdk.utils.CollectionUtils.deepCopyMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Lazy;

/**
 * A {@code Map<String, List<String>>} for headers and query strings in {@link SdkHttpRequest} and {@link SdkHttpResponse} that
 * avoids copying data during conversion between builders and buildables, unless data is modified.
 *
 * This is created via {@link #emptyHeaders()} or {@link #emptyQueryParameters()}.
 */
@SdkInternalApi
public final class LowCopyListMap {
    private LowCopyListMap() {
    }

    /**
     * Copy the map but not its value lists. The result shares list instances with {@code map}, so it must only be used
     * where those lists will not be mutated in place.
     */
    private static Map<String, List<String>> shallowCopyMap(Map<String, List<String>> map,
                                                            Supplier<Map<String, List<String>>> mapConstructor) {
        Map<String, List<String>> result = mapConstructor.get();
        result.putAll(map);
        return result;
    }

    /**
     * Create an empty {@link LowCopyListMap.ForBuilder} for header storage.
     */
    public static LowCopyListMap.ForBuilder emptyHeaders() {
        return new LowCopyListMap.ForBuilder(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Create an empty {@link LowCopyListMap.ForBuilder} for query parameter storage.
     */
    public static LowCopyListMap.ForBuilder emptyQueryParameters() {
        return new LowCopyListMap.ForBuilder(LinkedHashMap::new);
    }

    @NotThreadSafe
    public static final class ForBuilder {
        /**
         * The constructor that can be used to create new, empty maps.
         */
        private final Supplier<Map<String, List<String>>> mapConstructor;

        /**
         * Whether {@link #map} has been shared with a {@link ForBuildable}. If this is true, we need to make sure to copy the
         * map before we mutate it.
         */
        private boolean mapIsShared = false;

        /**
         * Whether the value lists inside {@link #map} are shared with a {@link ForBuildable} (or with another
         * {@link ForBuilder} derived from the same one). This is tracked separately from {@link #mapIsShared} because most
         * mutations replace a value list wholesale rather than mutating one in place, and those only need the map copied.
         *
         * <p>It stays {@code true} after a shallow map copy: the new map still points at the shared lists.
         */
        private boolean listsAreShared = false;

        /**
         * The data stored in this low-copy list-map.
         */
        private Map<String, List<String>> map;

        /**
         * Created from {@link LowCopyListMap#emptyHeaders()} or {@link LowCopyListMap#emptyQueryParameters()}.
         */
        private ForBuilder(Supplier<Map<String, List<String>>> mapConstructor) {
            this.mapConstructor = mapConstructor;
            this.map = mapConstructor.get();
        }

        /**
         * Created from {@link LowCopyListMap.ForBuildable#forBuilder()}.
         */
        private ForBuilder(ForBuildable forBuildable) {
            this.mapConstructor = forBuildable.mapConstructor;
            this.map = forBuildable.map;
            this.mapIsShared = true;
            this.listsAreShared = true;
        }

        public void clear() {
            this.map = mapConstructor.get();
            this.mapIsShared = false;
            this.listsAreShared = false;
        }

        public void setFromExternal(Map<String, List<String>> map) {
            // The incoming lists are owned by the caller, so they have to be copied.
            this.map = deepCopyMap(map, mapConstructor);
            this.mapIsShared = false;
            this.listsAreShared = false;
        }

        /**
         * Get the map for a mutation that does not modify any existing value list in place: adding an entry, replacing an
         * entry's list wholesale, removing an entry, or clearing the map.
         *
         * <p>Only the map itself is copied when shared. The value lists stay shared, which is safe precisely because this
         * kind of mutation never touches them. Callers that mutate a value list in place must use
         * {@link #forInternalWriteWithListMutation()} instead.
         */
        public Map<String, List<String>> forInternalWrite() {
            if (mapIsShared) {
                this.map = shallowCopyMap(map, mapConstructor);
                this.mapIsShared = false;
            }
            return this.map;
        }

        /**
         * Get the map for a mutation that modifies an existing value list in place (e.g. appending a value). This copies the
         * value lists as well as the map, so the shared lists are not visible to whoever else holds them.
         */
        public Map<String, List<String>> forInternalWriteWithListMutation() {
            if (mapIsShared) {
                this.map = deepCopyMap(map, mapConstructor);
                this.mapIsShared = false;
                this.listsAreShared = false;
            } else if (listsAreShared) {
                // The map is already private but its lists are not. Privatize them all: appends are rare compared with
                // puts, so paying for the whole map once here is preferable to tracking ownership per key.
                this.map.replaceAll((k, v) -> new ArrayList<>(v));
                this.listsAreShared = false;
            }
            return this.map;
        }

        public Map<String, List<String>> forInternalRead() {
            return this.map;
        }

        public ForBuildable forBuildable() {
            this.mapIsShared = true;
            this.listsAreShared = true;
            return new ForBuildable(this);
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

            return map.equals(that.map);
        }

        @Override
        public int hashCode() {
            return map.hashCode();
        }
    }


    @ThreadSafe
    public static final class ForBuildable {
        /**
         * The constructor that can be used to create new, empty maps.
         */
        private final Supplier<Map<String, List<String>>> mapConstructor;

        /**
         * An unmodifiable copy of {@link #map}, which is lazily initialized only when it is needed.
         */
        private final Lazy<Map<String, List<String>>> deeplyUnmodifiableMap;

        /**
         * The data stored in this low-copy list-map.
         */
        private final Map<String, List<String>> map;

        private ForBuildable(ForBuilder forBuilder) {
            this.mapConstructor = forBuilder.mapConstructor;
            this.map = forBuilder.map;
            this.deeplyUnmodifiableMap = new Lazy<>(() -> CollectionUtils.deepUnmodifiableMap(this.map, this.mapConstructor));
        }

        public Map<String, List<String>> forExternalRead() {
            return deeplyUnmodifiableMap.getValue();
        }

        public Map<String, List<String>> forInternalRead() {
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

            return map.equals(that.map);
        }

        @Override
        public int hashCode() {
            return map.hashCode();
        }
    }
}
