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

package software.amazon.awssdk.dynamicclient.schema.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.GenericTrait;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;

class ModelConversionsTest {
    @Test
    void document_scalars_followTheCodegenLiteralMapping() {
        assertThat(ModelConversions.document(null)).isNull();
        assertThat(ModelConversions.document(Node.nullNode())).isNull();
        assertThat(ModelConversions.document(Node.from(true))).isEqualTo(Document.of(true));
        assertThat(ModelConversions.document(Node.from("s"))).isEqualTo(Document.of("s"));
        assertThat(ModelConversions.document(Node.from(7)).type()).isEqualTo(ShapeType.INTEGER);
        assertThat(ModelConversions.document(Node.from(7L)).type()).isEqualTo(ShapeType.INTEGER);
        assertThat(ModelConversions.document(Node.from(5000000000L))).isEqualTo(Document.of(5000000000L));
        assertThat(ModelConversions.document(Node.from(BigInteger.valueOf(3))).type()).isEqualTo(ShapeType.INTEGER);
        BigInteger huge = new BigInteger("99999999999999999999");
        assertThat(ModelConversions.document(Node.from(huge))).isEqualTo(Document.of(huge));
        assertThat(ModelConversions.document(Node.from(new BigDecimal("1.10"))))
            .isEqualTo(Document.of(new BigDecimal("1.10")));
        assertThat(ModelConversions.document(Node.from(1.5))).isEqualTo(Document.of(1.5));
        assertThat(ModelConversions.document(Node.from(1.5f)).type()).isEqualTo(ShapeType.DOUBLE);
    }

    @Test
    void document_arraysAndObjects_keepOrderAndNulls() {
        ObjectNode object = Node.objectNodeBuilder()
                                .withMember("z", Node.from(1))
                                .withMember("a", ArrayNode.fromNodes(Node.from("x"), Node.nullNode()))
                                .withMember("m", Node.nullNode())
                                .build();
        Document document = ModelConversions.document(object);

        assertThat(new ArrayList<>(document.getMemberNames())).containsExactly("z", "a", "m");
        assertThat(document.getMember("a").asList()).containsExactly(Document.of("x"), null);
        assertThat(document.asMap()).containsEntry("m", null);
    }

    @Test
    void trait_typedAndUntypedTraits_comeFromTheTraitFactories() {
        Trait jsonName = ModelConversions.trait(new software.amazon.smithy.model.traits.JsonNameTrait("n"));
        assertThat(jsonName).isEqualTo(new JsonNameTrait("n"));

        Trait custom = ModelConversions.trait(new DynamicTrait(ShapeId.from("test.x#custom"),
                                                               Node.objectNode().withMember("k", Node.from(2))));
        assertThat(custom).isInstanceOf(GenericTrait.class);
        assertThat(custom.traitId()).hasToString("test.x#custom");
        assertThat(custom.toDocument().getMember("k")).isEqualTo(Document.of(2));
    }

    @Test
    void shapeId_roundTripsWithAndWithoutMember() {
        ShapeId shape = ShapeId.from("a.b#Shape");
        ShapeId member = ShapeId.from("a.b#Shape$member");

        assertThat(ModelConversions.shapeId(shape)).hasToString("a.b#Shape");
        assertThat(ModelConversions.shapeId(member)).hasToString("a.b#Shape$member");
        assertThat(ModelConversions.smithyShapeId(ModelConversions.shapeId(member))).isEqualTo(member);
        assertThat(ModelConversions.smithyShapeId(ModelConversions.shapeId(shape))).isEqualTo(shape);
    }
}
