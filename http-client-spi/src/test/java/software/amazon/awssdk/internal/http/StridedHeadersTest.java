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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * {@link StridedHeaders} replaces a {@code TreeMap(String.CASE_INSENSITIVE_ORDER)}-backed store, so these tests pin
 * two things: the aliasing guarantees of the copy-on-write share (mirroring {@link LowCopyListMapTest}), and the
 * observable behaviors of the map representation that callers of {@code SdkHttpRequest} depend on — case-insensitive
 * matching with preserved casing, sorted iteration, multi-value order, and the empty-value-list edge case.
 */
class StridedHeadersTest {

    // ----------------------------------------------------------------------------------------------------
    // Aliasing: after each kind of mutation, whoever else holds the data must not see the change.
    // ----------------------------------------------------------------------------------------------------

    @Test
    void put_afterSharingWithBuildable_doesNotMutateBuildable() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", singletonList("1"));

        StridedHeaders.ForBuildable buildable = builder.forBuildable();
        builder.put("b", singletonList("2"));

        assertThat(buildable.externalMap()).containsOnlyKeys("a");
        assertThat(builder.externalMapSnapshot()).containsOnlyKeys("a", "b");
    }

    @Test
    void replace_afterSharingWithBuildable_doesNotMutateBuildable() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", singletonList("1"));

        StridedHeaders.ForBuildable buildable = builder.forBuildable();
        builder.put("a", singletonList("2"));

        assertThat(buildable.valuesFor("a")).containsExactly("1");
        assertThat(builder.valuesFor("a")).containsExactly("2");
    }

    @Test
    void remove_afterSharingWithBuildable_doesNotMutateBuildable() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", singletonList("1"));

        StridedHeaders.ForBuildable buildable = builder.forBuildable();
        builder.remove("a");

        assertThat(buildable.valuesFor("a")).containsExactly("1");
        assertThat(builder.valuesFor("a")).isEmpty();
        assertThat(builder.distinctNames()).isZero();
    }

    @Test
    void append_afterSharingWithBuildable_doesNotMutateBuildable() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", singletonList("1"));

        StridedHeaders.ForBuildable buildable = builder.forBuildable();
        builder.append("a", "2");

        assertThat(buildable.valuesFor("a")).containsExactly("1");
        assertThat(builder.valuesFor("a")).containsExactly("1", "2");
    }

    @Test
    void mutationOnDerivedBuilder_doesNotMutateOriginalBuildable() {
        StridedHeaders.ForBuilder original = StridedHeaders.emptyHeaders();
        original.put("a", singletonList("1"));
        StridedHeaders.ForBuildable first = original.forBuildable();

        StridedHeaders.ForBuilder derived = first.forBuilder();
        derived.put("b", singletonList("2"));
        derived.append("a", "3");
        StridedHeaders.ForBuildable second = derived.forBuildable();

        assertThat(first.externalMap()).containsOnlyKeys("a");
        assertThat(first.valuesFor("a")).containsExactly("1");
        assertThat(second.externalMap()).containsOnlyKeys("a", "b");
        assertThat(second.valuesFor("a")).containsExactly("1", "3");
    }

    @Test
    void twoBuildersFromSameBuildable_areIndependent() {
        StridedHeaders.ForBuilder seed = StridedHeaders.emptyHeaders();
        seed.put("a", singletonList("1"));
        StridedHeaders.ForBuildable buildable = seed.forBuildable();

        StridedHeaders.ForBuilder left = buildable.forBuilder();
        StridedHeaders.ForBuilder right = buildable.forBuilder();

        left.append("a", "L");
        right.append("a", "R");

        assertThat(left.valuesFor("a")).containsExactly("1", "L");
        assertThat(right.valuesFor("a")).containsExactly("1", "R");
        assertThat(buildable.valuesFor("a")).containsExactly("1");
    }

    @Test
    void mutationOnSeedBuilder_afterDerivingBuilder_doesNotLeakEitherWay() {
        StridedHeaders.ForBuilder seed = StridedHeaders.emptyHeaders();
        seed.put("a", singletonList("1"));
        StridedHeaders.ForBuildable buildable = seed.forBuildable();

        // The seed builder also shares the array after forBuildable(); its own further mutation must copy too.
        seed.append("a", "seed");

        assertThat(buildable.valuesFor("a")).containsExactly("1");
        assertThat(seed.valuesFor("a")).containsExactly("1", "seed");
    }

    @Test
    void clear_afterSharingWithBuildable_doesNotMutateBuildable() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", singletonList("1"));
        StridedHeaders.ForBuildable buildable = builder.forBuildable();

        builder.clear();
        builder.append("a", "2");

        assertThat(buildable.valuesFor("a")).containsExactly("1");
        assertThat(builder.valuesFor("a")).containsExactly("2");
    }

    @Test
    void setFromExternal_doesNotAliasCallerLists() {
        List<String> callerList = new ArrayList<>(singletonList("1"));
        Map<String, List<String>> external = new HashMap<>();
        external.put("a", callerList);

        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.setFromExternal(external);

        callerList.add("caller-mutation");

        assertThat(builder.valuesFor("a")).containsExactly("1");
    }

    @Test
    void putListIsCopied_callerMutationInvisible() {
        List<String> callerList = new ArrayList<>(asList("1", "2"));
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", callerList);

        callerList.add("3");

        assertThat(builder.valuesFor("a")).containsExactly("1", "2");
    }

    // ----------------------------------------------------------------------------------------------------
    // Case-insensitivity and casing preservation.
    // ----------------------------------------------------------------------------------------------------

    @Test
    void matching_isCaseInsensitive() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("Content-Length", singletonList("1"));

        assertThat(builder.firstValue("content-length")).isEqualTo("1");
        assertThat(builder.firstValue("CONTENT-LENGTH")).isEqualTo("1");
        assertThat(builder.valuesFor("content-LENGTH")).containsExactly("1");
    }

    @Test
    void replaceWithDifferentCasing_keepsFirstSeenName() {
        // TreeMap.put with a comparator-equal key retains the existing key; the strided store must match.
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("Content-Length", singletonList("1"));
        builder.put("content-length", singletonList("2"));

        assertThat(builder.externalMapSnapshot().keySet()).containsExactly("Content-Length");
        assertThat(builder.firstValue("content-length")).isEqualTo("2");
        assertThat(builder.distinctNames()).isEqualTo(1);
    }

    @Test
    void appendWithDifferentCasing_extendsSameHeader() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("X-Foo", singletonList("1"));
        builder.append("x-foo", "2");

        assertThat(builder.valuesFor("X-FOO")).containsExactly("1", "2");
        assertThat(builder.distinctNames()).isEqualTo(1);
        assertThat(builder.externalMapSnapshot().keySet()).containsExactly("X-Foo");
    }

    // ----------------------------------------------------------------------------------------------------
    // Ordering.
    // ----------------------------------------------------------------------------------------------------

    @Test
    void iterationAndMap_areSortedCaseInsensitively() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("delta", singletonList("4"));
        builder.put("Alpha", singletonList("1"));
        builder.put("charlie", singletonList("3"));
        builder.put("Bravo", singletonList("2"));

        assertThat(builder.externalMapSnapshot().keySet()).containsExactly("Alpha", "Bravo", "charlie", "delta");

        List<String> visited = new ArrayList<>();
        builder.forEach((name, values) -> visited.add(name));
        assertThat(visited).containsExactly("Alpha", "Bravo", "charlie", "delta");
    }

    @Test
    void multiValueOrder_isInsertionOrder_andPreservedThroughShare() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", asList("1", "2", "3"));
        StridedHeaders.ForBuildable buildable = builder.forBuildable();

        assertThat(buildable.externalMap().get("a")).containsExactly("1", "2", "3");
        assertThat(buildable.valuesFor("a")).containsExactly("1", "2", "3");
    }

    @Test
    void forEach_groupsMultipleValuesUnderOneName() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", asList("1", "2"));
        builder.put("b", singletonList("3"));

        Map<String, List<String>> visited = new LinkedHashMap<>();
        builder.forEach(visited::put);

        assertThat(visited).hasSize(2);
        assertThat(visited.get("a")).containsExactly("1", "2");
        assertThat(visited.get("b")).containsExactly("3");
    }

    @Test
    void forEachEntry_visitsPairsInForEachOrder() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("delta", singletonList("4"));
        builder.put("Alpha", asList("1a", "1b"));
        builder.put("charlie", singletonList("3"));

        List<String> entries = new ArrayList<>();
        builder.forBuildable().forEachEntry((name, value) -> entries.add(name + "=" + value));

        assertThat(entries).containsExactly("Alpha=1a", "Alpha=1b", "charlie=3", "delta=4");
    }

    @Test
    void forEachEntry_skipsEmptyValueListNames() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", singletonList("1"));
        builder.put("b", emptyList());
        builder.put("c", singletonList("3"));

        List<String> entries = new ArrayList<>();
        builder.forBuildable().forEachEntry((name, value) -> entries.add(name + "=" + value));

        assertThat(entries).containsExactly("a=1", "c=3");
    }

    @Test
    void forEachEntry_matchesForEachExactly() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("X-Multi", asList("1", "2", "3"));
        builder.put("b-empty", emptyList());
        builder.put("Content-Type", singletonList("application/json"));
        builder.append("x-multi", "4");
        StridedHeaders.ForBuildable buildable = builder.forBuildable();

        List<String> viaForEach = new ArrayList<>();
        buildable.forEach((name, values) -> values.forEach(v -> viaForEach.add(name + "=" + v)));
        List<String> viaEntries = new ArrayList<>();
        buildable.forEachEntry((name, value) -> viaEntries.add(name + "=" + value));

        assertThat(viaEntries).isEqualTo(viaForEach);
    }

    // ----------------------------------------------------------------------------------------------------
    // Map-representation edge cases.
    // ----------------------------------------------------------------------------------------------------

    @Test
    void emptyValueList_keepsNameVisibleWithNoValues() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", emptyList());

        assertThat(builder.externalMapSnapshot()).containsOnlyKeys("a");
        assertThat(builder.externalMapSnapshot().get("a")).isEmpty();
        assertThat(builder.firstValue("a")).isNull();
        assertThat(builder.valuesFor("a")).isEmpty();
        assertThat(builder.distinctNames()).isEqualTo(1);
    }

    @Test
    void appendOntoEmptyValueList_behavesLikeComputeIfAbsentAdd() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", emptyList());
        builder.append("a", "1");

        assertThat(builder.valuesFor("a")).containsExactly("1");
        assertThat(builder.distinctNames()).isEqualTo(1);
    }

    @Test
    void appendOntoSharedEmptyValueList_doesNotMutateBuildable() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", emptyList());
        StridedHeaders.ForBuildable buildable = builder.forBuildable();

        builder.append("a", "1");

        assertThat(buildable.valuesFor("a")).isEmpty();
        assertThat(buildable.externalMap()).containsOnlyKeys("a");
        assertThat(builder.valuesFor("a")).containsExactly("1");
    }

    @Test
    void emptyStringValue_isDistinctFromEmptyList() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", singletonList(""));

        assertThat(builder.valuesFor("a")).containsExactly("");
        assertThat(builder.externalMapSnapshot().get("a")).containsExactly("");
        // firstValue returns the stored empty string; the empty-Optional quirk lives in the caller.
        assertThat(builder.firstValue("a")).isEmpty();
    }

    @Test
    void nullValue_isStoredAndReturned() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", singletonList(null));

        assertThat(builder.firstValue("a")).isNull();
        assertThat(builder.valuesFor("a")).containsExactly((String) null);
    }

    @Test
    void externalMap_isUnmodifiable() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", asList("1", "2"));
        StridedHeaders.ForBuildable buildable = builder.forBuildable();

        assertThatThrownBy(() -> buildable.externalMap().put("b", emptyList()))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> buildable.externalMap().get("a").add("3"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void externalMap_getIsCaseInsensitive() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("Content-Type", singletonList("application/json"));
        StridedHeaders.ForBuildable buildable = builder.forBuildable();

        assertThat(buildable.externalMap().get("content-type")).containsExactly("application/json");
        assertThat(buildable.externalMap()).isEqualTo(mapOf("Content-Type", "application/json"));
    }

    @Test
    void removeAbsentName_isNoOp() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", singletonList("1"));
        builder.remove("zz");

        assertThat(builder.valuesFor("a")).containsExactly("1");
        assertThat(builder.distinctNames()).isEqualTo(1);
    }

    @Test
    void removeMiddleRun_leavesNeighborsIntact() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", singletonList("1"));
        builder.put("b", asList("2", "3"));
        builder.put("c", singletonList("4"));

        builder.remove("b");

        assertThat(builder.externalMapSnapshot().keySet()).containsExactly("a", "c");
        assertThat(builder.valuesFor("c")).containsExactly("4");
    }

    @Test
    void replaceMultiValueWithFewerValues_compactsCorrectly() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", asList("1", "2", "3"));
        builder.put("b", singletonList("9"));
        builder.put("a", singletonList("x"));

        assertThat(builder.valuesFor("a")).containsExactly("x");
        assertThat(builder.valuesFor("b")).containsExactly("9");
        assertThat(builder.distinctNames()).isEqualTo(2);
    }

    @Test
    void replaceSingleValueWithMoreValues_growsCorrectly() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", singletonList("1"));
        builder.put("b", singletonList("9"));
        builder.put("a", asList("x", "y", "z"));

        assertThat(builder.valuesFor("a")).containsExactly("x", "y", "z");
        assertThat(builder.valuesFor("b")).containsExactly("9");
    }

    @Test
    void manyHeaders_growBeyondInitialCapacity() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        for (int i = 0; i < 100; i++) {
            builder.put("header-" + String.format("%03d", i), singletonList(String.valueOf(i)));
        }

        assertThat(builder.distinctNames()).isEqualTo(100);
        assertThat(builder.firstValue("HEADER-042")).isEqualTo("42");
    }

    @Test
    void anyName_visitsEachDistinctNameOnce() {
        StridedHeaders.ForBuilder builder = StridedHeaders.emptyHeaders();
        builder.put("a", asList("1", "2"));
        builder.put("b", singletonList("3"));

        List<String> visited = new ArrayList<>();
        assertThat(builder.anyName(name -> {
            visited.add(name);
            return false;
        })).isFalse();
        assertThat(visited).containsExactly("a", "b");

        assertThat(builder.anyName("b"::equals)).isTrue();
    }

    // ----------------------------------------------------------------------------------------------------
    // Equality.
    // ----------------------------------------------------------------------------------------------------

    @Test
    void equalContentDifferentNameCasing_isEqualWithSameHashCode() {
        StridedHeaders.ForBuilder left = StridedHeaders.emptyHeaders();
        left.put("Content-Type", singletonList("v"));
        StridedHeaders.ForBuilder right = StridedHeaders.emptyHeaders();
        right.put("content-type", singletonList("v"));

        assertThat(left).isEqualTo(right);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
        assertThat(left.forBuildable()).isEqualTo(right.forBuildable());
    }

    @Test
    void differentValues_areNotEqual() {
        StridedHeaders.ForBuilder left = StridedHeaders.emptyHeaders();
        left.put("a", singletonList("1"));
        StridedHeaders.ForBuilder right = StridedHeaders.emptyHeaders();
        right.put("a", singletonList("2"));

        assertThat(left).isNotEqualTo(right);
    }

    private static Map<String, List<String>> mapOf(String name, String value) {
        Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        map.put(name, singletonList(value));
        return map;
    }
}
