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

import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.RequiredTrait;

/** The exact-ID schema index, built the way generated {@code <Service>Schemas} holders build it. */
class ServiceSchemaIndexTest {

    private static final Schema SERVICE = Schema.createService(id("Weather"));
    private static final Schema OPERATION = Schema.createOperation(id("GetThing"));
    private static final Schema NAME = Schema.createString(id("Name"));
    private static final Schema COLOR = Schema.createEnum(id("Color"), Set.of("red", "green"));
    private static final Schema LEVEL = Schema.createIntEnum(id("Level"), Set.of(1, 2));

    // A structure <-> list cycle, built in two phases as generated holders do.
    private static final SchemaBuilder NODE_BUILDER = Schema.structureBuilder(id("Node"));
    private static final SchemaBuilder NODES_BUILDER = Schema.listBuilder(id("Nodes"));

    static {
        NODE_BUILDER.putMember("label", NAME)
                    .putMember("children", NODES_BUILDER)
                    .putMember("id", NAME, new RequiredTrait());
        NODES_BUILDER.putMember("member", NODE_BUILDER);
    }

    private static final Schema NODE = NODE_BUILDER.build().resolve();
    private static final Schema NODES = NODES_BUILDER.build().resolve();
    private static final Schema OUTPUT = Schema.structureBuilder(id("GetThingOutput"))
                                               .putMember("node", NODE)
                                               .putMember("color", COLOR)
                                               .putMember("level", LEVEL)
                                               .putMember("unit", Unit.SCHEMA)
                                               .putMember("count", PreludeSchemas.PRIMITIVE_INTEGER,
                                                          new DefaultTrait(Node.from(0)))
                                               .build();

    private static ServiceSchemaIndex.Builder complete() {
        return ServiceSchemaIndex.builder(SERVICE)
                                 .operation(OPERATION)
                                 .type(COLOR)
                                 .type(LEVEL)
                                 .type(NODE)
                                 .type(OUTPUT)
                                 .shape(NAME)
                                 .shape(NODES)
                                 .memberOrder(NODE, "label", "children", "id")
                                 .memberOrder(OUTPUT, "node", "color", "level", "unit", "count")
                                 .enumMember(COLOR, "RED", EnumValueTrait.builder().stringValue("red").build())
                                 .enumMember(COLOR, "GREEN", EnumValueTrait.builder().stringValue("green").build())
                                 .enumMember(LEVEL, "LOW", EnumValueTrait.builder().intValue(1).build())
                                 .enumMember(LEVEL, "HIGH", EnumValueTrait.builder().intValue(2).build());
    }

    @Test
    void indexesEverySchemaAndMemberByExactId() {
        ServiceSchemaIndex index = complete().build();

        assertThat(index.service()).isSameAs(SERVICE);
        assertThat(index.operations()).containsExactly(OPERATION);
        assertThat(index.types()).containsExactly(COLOR, OUTPUT, LEVEL, NODE);
        assertThat(index.shapes()).containsExactly(NAME, NODES);
        assertThat(index.schema(id("Node"))).containsSame(NODE);
        assertThat(index.schema(id("Node$children")).orElseThrow().memberTarget()).isSameAs(NODES);
        assertThat(index.schema(id("Nodes$member")).orElseThrow().memberTarget()).isSameAs(NODE);
        assertThat(index.schema(ShapeId.from("smithy.api#PrimitiveInteger"))).containsSame(
            PreludeSchemas.PRIMITIVE_INTEGER);
        assertThat(index.schema(Unit.ID)).containsSame(Unit.SCHEMA);
        assertThat(index.schema(id("Missing"))).isEmpty();
    }

    @Test
    void membersAreInModeledOrderNotSmithyJavaRequiredFirstOrder() {
        ServiceSchemaIndex index = complete().build();

        assertThat(NODE.members()).extracting(Schema::memberName).containsExactly("id", "label", "children");
        assertThat(index.members(NODE)).extracting(Schema::memberName).containsExactly("label", "children", "id");
        assertThat(index.members(index.schema(id("Nodes$member")).orElseThrow()))
            .extracting(Schema::memberName).containsExactly("label", "children", "id");
        assertThat(index.members(NODES)).extracting(Schema::memberName).containsExactly("member");
        assertThat(index.members(NAME)).isEmpty();
    }

    @Test
    void enumAndIntEnumMembersHaveMemberSchemas() {
        ServiceSchemaIndex index = complete().build();

        assertThat(COLOR.members()).isEmpty();
        assertThat(index.members(COLOR)).extracting(Schema::id).containsExactly(id("Color$RED"), id("Color$GREEN"));
        Schema red = index.schema(id("Color$RED")).orElseThrow();
        assertThat(red.isMember()).isTrue();
        assertThat(red.memberName()).isEqualTo("RED");
        assertThat(red.memberTarget()).isSameAs(Unit.SCHEMA);
        assertThat(red.getDirectTrait(TraitKey.get(EnumValueTrait.class)).expectStringValue()).isEqualTo("red");
        Schema high = index.schema(id("Level$HIGH")).orElseThrow();
        assertThat(high.getDirectTrait(TraitKey.get(EnumValueTrait.class)).expectIntValue()).isEqualTo(2);
        assertThat(index.members(red)).isEmpty();
    }

    @Test
    void exposesSeveralUnknownTraitsPerShapeAndMemberByTraitId() {
        DynamicTrait a = new DynamicTrait(ShapeId.from("a#a"), Node.nullNode());
        DynamicTrait b = new DynamicTrait(ShapeId.from("b.c#b"), Node.objectNode().withMember("x", Node.from(1)));
        ServiceSchemaIndex index = complete().unknownTraits(id("Color$RED"), b, a)
                                             .unknownTraits(id("Node"), a)
                                             .unknownTraits(id("Weather"), b)
                                             .build();

        UnknownTraits red = index.unknownTraits(index.schema(id("Color$RED")).orElseThrow());
        assertThat(red.ids()).containsExactly(ShapeId.from("a#a"), ShapeId.from("b.c#b"));
        assertThat(red.value(ShapeId.from("b.c#b"))).contains(Node.objectNode().withMember("x", Node.from(1)));
        assertThat(index.unknownTraits(NODE)).isEqualTo(UnknownTraits.of(a));
        assertThat(index.unknownTraits(SERVICE).contains(ShapeId.from("b.c#b"))).isTrue();
        assertThat(index.unknownTraits(NAME)).isSameAs(UnknownTraits.EMPTY);
        assertThat(index.unknownTraits().keySet()).containsExactly(id("Color$RED"), id("Node"), id("Weather"));
    }

    @Test
    void rejectsInconsistentRegistrations() {
        assertThatThrownBy(() -> ServiceSchemaIndex.builder(OPERATION))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> complete().type(NAME)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> complete().shape(NAME)).hasMessageContaining("registered twice");
        assertThatThrownBy(() -> complete().shape(OPERATION)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServiceSchemaIndex.builder(SERVICE).type(OUTPUT).build())
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("not registered");
        assertThatThrownBy(() -> complete().unknownTraits(id("Nowhere"),
                                                          new DynamicTrait(ShapeId.from("a#a"), Node.from(1)))
                                           .build())
            .hasMessageContaining("Nowhere");
        assertThatThrownBy(() -> complete().enumMember(COLOR, "BLUE"))
            .hasMessageContaining("enumValue");
        assertThatThrownBy(() -> complete().enumMember(COLOR, "BLUE", EnumValueTrait.builder().stringValue("blue")
                                                                                     .build()))
            .hasMessageContaining("not a value");
        assertThatThrownBy(() -> ServiceSchemaIndex.builder(SERVICE).type(COLOR).build())
            .hasMessageContaining("No member schemas");
        assertThatThrownBy(() -> ServiceSchemaIndex.builder(SERVICE).type(COLOR)
                                                   .enumMember(COLOR, "RED",
                                                               EnumValueTrait.builder().stringValue("red").build())
                                                   .build())
            .hasMessageContaining("2 values but 1 member schemas");
        assertThatThrownBy(() -> ServiceSchemaIndex.builder(SERVICE).type(NODE).shape(NAME).shape(NODES)
                                                   .memberOrder(NODE, "label", "id").build())
            .hasMessageContaining("exactly once");
        assertThatThrownBy(() -> complete().build().members(Schema.createString(id("Name"))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enumMemberSchemasAreUnitMembersWithTheGivenDirectTraits() {
        Schema red = complete().build().schema(id("Color$RED")).orElseThrow();
        // A member schema has the type of its target, smithy.api#Unit, and inherits the target's traits.
        assertThat(red.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(red.hasTrait(TraitKey.UNIT_TYPE_TRAIT)).isTrue();
        assertThat(red.getDirectTrait(TraitKey.UNIT_TYPE_TRAIT)).isNull();
        assertThat(red.getDirectTrait(TraitKey.get(EnumValueTrait.class))).isNotNull();
        assertThat(red.hasTrait(TraitKey.REQUIRED_TRAIT)).isFalse();
    }

    private static ShapeId id(String name) {
        return ShapeId.from("example.weather#" + name);
    }
}
