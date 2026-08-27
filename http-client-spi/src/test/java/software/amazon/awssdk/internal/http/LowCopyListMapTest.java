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
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The point of {@link LowCopyListMap} is to share storage between builders and buildables until something is mutated, so
 * these tests are about aliasing: after each kind of mutation, whoever else holds the data must not see the change.
 */
class LowCopyListMapTest {

    @Test
    void putHeader_afterSharingWithBuildable_doesNotMutateBuildable() {
        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyHeaders();
        builder.forInternalWrite().put("a", newList("1"));

        LowCopyListMap.ForBuildable buildable = builder.forBuildable();
        Map<String, List<String>> snapshot = buildable.forExternalRead();

        builder.forInternalWrite().put("b", newList("2"));

        assertThat(snapshot).containsOnlyKeys("a");
        assertThat(builder.forInternalRead()).containsOnlyKeys("a", "b");
    }

    @Test
    void replaceEntry_afterSharingWithBuildable_doesNotMutateBuildable() {
        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyHeaders();
        builder.forInternalWrite().put("a", newList("1"));

        LowCopyListMap.ForBuildable buildable = builder.forBuildable();

        builder.forInternalWrite().put("a", newList("2"));

        assertThat(buildable.forInternalRead().get("a")).containsExactly("1");
        assertThat(builder.forInternalRead().get("a")).containsExactly("2");
    }

    @Test
    void removeEntry_afterSharingWithBuildable_doesNotMutateBuildable() {
        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyHeaders();
        builder.forInternalWrite().put("a", newList("1"));

        LowCopyListMap.ForBuildable buildable = builder.forBuildable();

        builder.forInternalWrite().remove("a");

        assertThat(buildable.forInternalRead()).containsOnlyKeys("a");
        assertThat(builder.forInternalRead()).isEmpty();
    }

    /**
     * The case the shallow copy has to be careful about: appending mutates a value list in place, so the lists have to be
     * privatized first even though the map may already be private.
     */
    @Test
    void appendToExistingList_afterSharingWithBuildable_doesNotMutateBuildable() {
        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyHeaders();
        builder.forInternalWrite().put("a", newList("1"));

        LowCopyListMap.ForBuildable buildable = builder.forBuildable();

        builder.forInternalWriteWithListMutation().get("a").add("2");

        assertThat(buildable.forInternalRead().get("a")).containsExactly("1");
        assertThat(builder.forInternalRead().get("a")).containsExactly("1", "2");
    }

    /**
     * A put makes the map private but leaves the lists shared. A later append must still privatize the lists.
     */
    @Test
    void appendAfterPut_afterSharingWithBuildable_doesNotMutateBuildable() {
        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyHeaders();
        builder.forInternalWrite().put("a", newList("1"));

        LowCopyListMap.ForBuildable buildable = builder.forBuildable();

        // Map becomes private here; "a" is still the list the buildable holds.
        builder.forInternalWrite().put("b", newList("9"));
        builder.forInternalWriteWithListMutation().get("a").add("2");

        assertThat(buildable.forInternalRead().get("a")).containsExactly("1");
        assertThat(builder.forInternalRead().get("a")).containsExactly("1", "2");
    }

    @Test
    void appendTwice_afterSharingWithBuildable_accumulatesOnBuilderOnly() {
        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyHeaders();
        builder.forInternalWrite().put("a", newList("1"));

        LowCopyListMap.ForBuildable buildable = builder.forBuildable();

        builder.forInternalWriteWithListMutation().get("a").add("2");
        builder.forInternalWriteWithListMutation().get("a").add("3");

        assertThat(buildable.forInternalRead().get("a")).containsExactly("1");
        assertThat(builder.forInternalRead().get("a")).containsExactly("1", "2", "3");
    }

    @Test
    void appendToNewKey_afterSharingWithBuildable_doesNotAddToBuildable() {
        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyHeaders();
        builder.forInternalWrite().put("a", newList("1"));

        LowCopyListMap.ForBuildable buildable = builder.forBuildable();

        builder.forInternalWriteWithListMutation().computeIfAbsent("b", k -> new ArrayList<>()).add("2");

        assertThat(buildable.forInternalRead()).containsOnlyKeys("a");
        assertThat(builder.forInternalRead().get("b")).containsExactly("2");
    }

    /**
     * Round-tripping buildable -> builder -> buildable is how retries and the signer reuse a request, so a mutation on the
     * derived builder must not be visible on the original buildable.
     */
    @Test
    void mutationOnDerivedBuilder_doesNotMutateOriginalBuildable() {
        LowCopyListMap.ForBuilder original = LowCopyListMap.emptyHeaders();
        original.forInternalWrite().put("a", newList("1"));
        LowCopyListMap.ForBuildable first = original.forBuildable();

        LowCopyListMap.ForBuilder derived = first.forBuilder();
        derived.forInternalWrite().put("b", newList("2"));
        derived.forInternalWriteWithListMutation().get("a").add("3");
        LowCopyListMap.ForBuildable second = derived.forBuildable();

        assertThat(first.forInternalRead()).containsOnlyKeys("a");
        assertThat(first.forInternalRead().get("a")).containsExactly("1");
        assertThat(second.forInternalRead()).containsOnlyKeys("a", "b");
        assertThat(second.forInternalRead().get("a")).containsExactly("1", "3");
    }

    /**
     * Two builders from the same buildable must not see each other's mutations either.
     */
    @Test
    void twoBuildersFromSameBuildable_areIndependent() {
        LowCopyListMap.ForBuilder seed = LowCopyListMap.emptyHeaders();
        seed.forInternalWrite().put("a", newList("1"));
        LowCopyListMap.ForBuildable buildable = seed.forBuildable();

        LowCopyListMap.ForBuilder left = buildable.forBuilder();
        LowCopyListMap.ForBuilder right = buildable.forBuilder();

        left.forInternalWriteWithListMutation().get("a").add("L");
        right.forInternalWriteWithListMutation().get("a").add("R");

        assertThat(left.forInternalRead().get("a")).containsExactly("1", "L");
        assertThat(right.forInternalRead().get("a")).containsExactly("1", "R");
        assertThat(buildable.forInternalRead().get("a")).containsExactly("1");
    }

    @Test
    void clear_afterSharingWithBuildable_doesNotMutateBuildable() {
        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyHeaders();
        builder.forInternalWrite().put("a", newList("1"));
        LowCopyListMap.ForBuildable buildable = builder.forBuildable();

        builder.clear();
        builder.forInternalWriteWithListMutation().computeIfAbsent("a", k -> new ArrayList<>()).add("2");

        assertThat(buildable.forInternalRead().get("a")).containsExactly("1");
        assertThat(builder.forInternalRead().get("a")).containsExactly("2");
    }

    /**
     * A map handed in from outside is owned by the caller, so it has to be deep copied: later mutations on either side must
     * not be visible to the other.
     */
    @Test
    void setFromExternal_copiesCallerLists() {
        List<String> callerList = newList("1");
        Map<String, List<String>> external = new HashMap<>();
        external.put("a", callerList);

        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyHeaders();
        builder.setFromExternal(external);

        callerList.add("caller-mutation");
        builder.forInternalWriteWithListMutation().get("a").add("builder-mutation");

        assertThat(callerList).containsExactly("1", "caller-mutation");
        assertThat(builder.forInternalRead().get("a")).containsExactly("1", "builder-mutation");
    }

    @Test
    void headers_areCaseInsensitive() {
        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyHeaders();
        builder.forInternalWrite().put("Content-Length", newList("1"));

        assertThat(builder.forInternalRead()).containsKey("content-length");
    }

    @Test
    void queryParameters_areCaseSensitiveAndOrdered() {
        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyQueryParameters();
        builder.forInternalWrite().put("b", newList("1"));
        builder.forInternalWrite().put("A", newList("2"));
        builder.forInternalWrite().put("a", newList("3"));

        assertThat(builder.forInternalRead()).containsOnlyKeys("b", "A", "a");
        assertThat(builder.forInternalRead().keySet()).containsExactly("b", "A", "a");
    }

    @Test
    void multiValuedList_isPreservedThroughShare() {
        LowCopyListMap.ForBuilder builder = LowCopyListMap.emptyHeaders();
        builder.forInternalWrite().put("a", new ArrayList<>(asList("1", "2", "3")));

        LowCopyListMap.ForBuildable buildable = builder.forBuildable();

        assertThat(buildable.forExternalRead().get("a")).containsExactly("1", "2", "3");
    }

    private static List<String> newList(String value) {
        return new ArrayList<>(singletonList(value));
    }
}
