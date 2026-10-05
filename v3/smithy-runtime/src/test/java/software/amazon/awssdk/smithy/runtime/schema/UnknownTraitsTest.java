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

package software.amazon.awssdk.smithy.runtime.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;

class UnknownTraitsTest {

    private static final ShapeId A = ShapeId.from("a#a");
    private static final ShapeId B = ShapeId.from("b.c#B");

    @Test
    void keysTraitsByExactTraitIdInOrder() {
        UnknownTraits traits = UnknownTraits.of(new DynamicTrait(B, Node.from("two")),
                                                new DynamicTrait(A, Node.fromNodes(Node.nullNode())));

        assertThat(traits.ids()).containsExactly(A, B);
        assertThat(traits.value(B)).contains(Node.from("two"));
        assertThat(traits.value(A)).contains(Node.fromNodes(Node.nullNode()));
        assertThat(traits.get(ShapeId.from("a#A"))).isEmpty();
        assertThat(traits.contains(A)).isTrue();
        assertThat(traits.size()).isEqualTo(2);
        assertThat(traits.asMap().keySet()).containsExactly(A, B);
        assertThat(traits).hasToString("UnknownTraits[@a#a([null]), @b.c#B(\"two\")]");
    }

    @Test
    void equalityIsStructural() {
        UnknownTraits one = UnknownTraits.of(new DynamicTrait(A, Node.from(new BigDecimal("1E+400"))));
        UnknownTraits two = UnknownTraits.of(new DynamicTrait(A, Node.from(new BigDecimal("1E+400"))));
        assertThat(one).isEqualTo(two).hasSameHashCodeAs(two);
        assertThat(one).isNotEqualTo(UnknownTraits.of(new DynamicTrait(A, Node.from(1))));
        assertThat(one).isNotEqualTo(UnknownTraits.EMPTY);
        assertThat(UnknownTraits.of()).isSameAs(UnknownTraits.EMPTY);
    }

    @Test
    void rejectsDuplicateTraitIds() {
        assertThatThrownBy(() -> UnknownTraits.of(new DynamicTrait(A, Node.from(1)), new DynamicTrait(A, Node.from(2))))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("a#a");
    }
}
