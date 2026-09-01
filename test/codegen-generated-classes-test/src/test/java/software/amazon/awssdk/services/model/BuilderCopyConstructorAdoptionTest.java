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

package software.amazon.awssdk.services.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.SimpleStruct;

/**
 * The generated builder copy constructor takes collection members straight from the model rather than routing them
 * through the fluent setter, which would deep-copy them again. That is only sound because of the properties asserted
 * here, none of which had coverage before:
 *
 * <ul>
 *   <li>a collection reached through a built model is unmodifiable, so sharing it with a builder cannot let either side
 *       mutate what the other sees;</li>
 *   <li>builder setters replace a collection field wholesale rather than mutating it in place, so setting a member on a
 *       builder obtained from {@code toBuilder()} cannot write through to the original model;</li>
 *   <li>a caller-supplied collection is still defensively copied on the way in, so mutating it afterwards does not
 *       affect an already-built model — that is what makes the model's collections safe to adopt in the first place.</li>
 * </ul>
 */
public class BuilderCopyConstructorAdoptionTest {

    @Test
    public void toBuilderRoundTrip_preservesCollections() {
        AllTypesRequest original = request();

        AllTypesRequest roundTripped = original.toBuilder().build();

        assertThat(roundTripped.simpleList()).isEqualTo(original.simpleList());
        assertThat(roundTripped.mapOfStringToString()).isEqualTo(original.mapOfStringToString());
        assertThat(roundTripped.listOfStructs()).isEqualTo(original.listOfStructs());
        assertThat(roundTripped.listOfMaps()).isEqualTo(original.listOfMaps());
        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    public void modelCollections_areUnmodifiable() {
        AllTypesRequest original = request();

        // This is the invariant the adoption depends on: nothing that reaches a builder from a model can be mutated,
        // by the builder's holder or by anyone else.
        assertThatThrownBy(() -> original.simpleList().add("nope")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> original.mapOfStringToString().put("k", "v"))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> original.listOfMaps().clear()).isInstanceOf(UnsupportedOperationException.class);

        AllTypesRequest.Builder builder = original.toBuilder();
        assertThatThrownBy(() -> builder.build().simpleList().add("nope"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void settingCollectionOnDerivedBuilder_doesNotAffectOriginalModel() {
        AllTypesRequest original = request();

        AllTypesRequest modified = original.toBuilder()
                                           .simpleList("replaced")
                                           .mapOfStringToString(new HashMap<>())
                                           .build();

        assertThat(modified.simpleList()).containsExactly("replaced");
        assertThat(modified.mapOfStringToString()).isEmpty();

        // The original must be untouched: setters replace the field, they do not mutate the shared collection.
        assertThat(original.simpleList()).containsExactly("a", "b");
        assertThat(original.mapOfStringToString()).containsEntry("k", "v");
    }

    @Test
    public void mutatingCallerCollectionAfterBuild_doesNotAffectModel() {
        List<String> callerList = new ArrayList<>(Arrays.asList("a", "b"));
        Map<String, String> callerMap = new HashMap<>();
        callerMap.put("k", "v");

        AllTypesRequest built = AllTypesRequest.builder()
                                               .simpleList(callerList)
                                               .mapOfStringToString(callerMap)
                                               .build();

        callerList.add("added-after-build");
        callerMap.put("added", "after-build");

        // Defensive copying on the way in is what makes the model's own collections safe to adopt later.
        assertThat(built.simpleList()).containsExactly("a", "b");
        assertThat(built.mapOfStringToString()).containsOnlyKeys("k");

        // And the same must hold across a toBuilder() round trip, which no longer re-copies.
        AllTypesRequest roundTripped = built.toBuilder().build();
        assertThat(roundTripped.simpleList()).containsExactly("a", "b");
        assertThat(roundTripped.mapOfStringToString()).containsOnlyKeys("k");
    }

    @Test
    public void mutatingCallerCollectionAfterToBuilder_doesNotAffectDerivedModel() {
        List<String> callerList = new ArrayList<>(Arrays.asList("a", "b"));

        AllTypesRequest.Builder builder = AllTypesRequest.builder().simpleList(callerList);
        callerList.add("added-after-set");

        assertThat(builder.build().simpleList()).containsExactly("a", "b");
    }

    private static AllTypesRequest request() {
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");

        Map<String, String> nested = new HashMap<>();
        nested.put("nk", "nv");

        return AllTypesRequest.builder()
                              .simpleList("a", "b")
                              .mapOfStringToString(map)
                              .listOfMaps(Arrays.asList(nested))
                              .listOfStructs(SimpleStruct.builder().stringMember("s").build())
                              .build();
    }
}
