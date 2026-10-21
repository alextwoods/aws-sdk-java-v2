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

package software.amazon.awssdk.dynamicclient.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.dynamicclient.schema.TestModels.id;
import static software.amazon.awssdk.dynamicclient.schema.TestModels.rec;
import static software.amazon.awssdk.dynamicclient.schema.TestModels.sdkId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.GenericTrait;
import software.amazon.awssdk.schema.traits.HttpErrorTrait;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.HttpQueryTrait;
import software.amazon.awssdk.schema.traits.HttpTrait;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.RequiredTrait;
import software.amazon.awssdk.schema.traits.RestJson1Trait;
import software.amazon.awssdk.schema.traits.SigV4Trait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;

class SchemaConverterTest {
    private static final Model MODEL = TestModels.converter();
    private static final ServiceShape SERVICE = MODEL.expectShape(id("DynService"), ServiceShape.class);

    private final SchemaConverter converter = new SchemaConverter(MODEL, SERVICE);

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "blob, BLOB, smithy.api#Blob",
        "bool, BOOLEAN, smithy.api#Boolean",
        "str, STRING, smithy.api#String",
        "time, TIMESTAMP, smithy.api#Timestamp",
        "epoch, TIMESTAMP, test.dyn#EpochTime",
        "b, BYTE, smithy.api#Byte",
        "s, SHORT, smithy.api#Short",
        "i, INTEGER, smithy.api#Integer",
        "l, LONG, smithy.api#Long",
        "f, FLOAT, smithy.api#Float",
        "d, DOUBLE, smithy.api#Double",
        "bi, BIG_INTEGER, smithy.api#BigInteger",
        "bd, BIG_DECIMAL, smithy.api#BigDecimal",
        "doc, DOCUMENT, smithy.api#Document",
        "color, ENUM, test.dyn#Color",
        "level, INT_ENUM, test.dyn#Level",
        "legacy, ENUM, test.dyn#LegacyEnum",
        "list, LIST, test.dyn#StringList",
        "map, MAP, test.dyn#IntMap",
        "choice, UNION, test.dyn#Choice",
        "nested, STRUCTURE, test.dyn#Nested",
    })
    void getSchema_memberTargets_haveTheModelIdAndType(String member, ShapeType type, String targetId) {
        Schema output = converter.getSchema(id("GetThingOutput"));
        Schema memberSchema = output.member(member);

        assertThat(memberSchema.id()).hasToString("test.dyn#GetThingOutput$" + member);
        assertThat(memberSchema.type()).isEqualTo(type);
        assertThat(memberSchema.memberTarget().id()).hasToString(targetId);
        assertThat(memberSchema.memberTarget()).isSameAs(converter.getSchema(ShapeId.from(targetId)));
    }

    @Test
    void getSchema_preludeShapes_areThePreludeSchemasConstantsExceptUnit() {
        assertThat(converter.getSchema(ShapeId.from("smithy.api#String"))).isSameAs(PreludeSchemas.STRING);
        assertThat(converter.getSchema(ShapeId.from("smithy.api#PrimitiveInteger")))
            .isSameAs(PreludeSchemas.PRIMITIVE_INTEGER);
        assertThat(converter.getSchema(id("GetThingOutput")).member("str").memberTarget())
            .isSameAs(PreludeSchemas.STRING);

        Schema unit = converter.getSchema(ShapeId.from("smithy.api#Unit"));
        assertThat(unit).isNotSameAs(PreludeSchemas.UNIT);
        assertThat(unit.id()).isEqualTo(PreludeSchemas.UNIT.id());
        assertThat(unit.hasTrait(TraitKey.UNIT_TYPE_TRAIT)).isTrue();
        assertThat(unit.shapeBuilder().build()).isInstanceOf(StructDocument.class);
    }

    @Test
    void getSchema_structure_hasMembersInDeclarationOrderWithIndexes() {
        Schema input = converter.getSchema(id("GetThingInput"));

        assertThat(input.id()).hasToString("test.dyn#GetThingInput");
        assertThat(input.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(input.isMember()).isFalse();
        assertThat(names(input.members())).containsExactly("id", "count", "verbose");
        for (int i = 0; i < input.members().size(); i++) {
            assertThat(input.members().get(i).memberIndex()).isEqualTo(i);
        }
    }

    @Test
    void getSchema_enums_keepDeclarationOrder() {
        assertThat(converter.getSchema(id("Color")).stringEnumValues()).containsExactly("red", "green");
        assertThat(converter.getSchema(id("Level")).intEnumValues()).containsExactly(1, 10);
        assertThat(converter.getSchema(id("LegacyEnum")).stringEnumValues()).containsExactly("b", "a");
    }

    @Test
    void getSchema_listAndMap_haveTheirMembers() {
        Schema list = converter.getSchema(id("StringList"));
        Schema map = converter.getSchema(id("IntMap"));

        assertThat(list.listMember().memberTarget().id()).hasToString("smithy.api#String");
        assertThat(map.mapKeyMember().memberTarget().id()).hasToString("smithy.api#String");
        assertThat(map.mapValueMember().memberTarget().type()).isEqualTo(ShapeType.INTEGER);
        assertThat(converter.getSchema(id("SparseList")).hasTrait(TraitKey.SPARSE_TRAIT)).isTrue();
    }

    @Test
    void getSchema_serviceOperationAndResource_haveTheirTypes() {
        assertThat(converter.getSchema(id("DynService")).type()).isEqualTo(ShapeType.SERVICE);
        assertThat(converter.getSchema(id("GetThing")).type()).isEqualTo(ShapeType.OPERATION);
        assertThat(converter.getSchema(id("Thing")).type()).isEqualTo(ShapeType.RESOURCE);
    }

    @Test
    void getSchema_memberId_returnsTheContainersMemberSchema() {
        Schema container = converter.getSchema(id("GetThingInput"));

        assertThat(converter.getSchema(id("GetThingInput").withMember("count"))).isSameAs(container.member("count"));
        assertThat(converter.getSchema(MODEL.expectShape(id("GetThingInput").withMember("id"))))
            .isSameAs(container.member("id"));
    }

    @Test
    void getSchema_includedTraits_areTypedRuntimeTraits() {
        Schema input = converter.getSchema(id("GetThingInput"));

        assertThat(input.member("id").getDirectTrait(TraitKey.REQUIRED_TRAIT)).isSameAs(RequiredTrait.INSTANCE);
        assertThat(input.member("id").getDirectTrait(TraitKey.get(HttpHeaderTrait.class)).getValue()).isEqualTo("x-id");
        assertThat(input.member("verbose").getDirectTrait(TraitKey.get(HttpQueryTrait.class)).getValue())
            .isEqualTo("v");
        assertThat(input.member("count").getDirectTrait(TraitKey.get(JsonNameTrait.class)).getValue())
            .isEqualTo("cnt");

        HttpTrait http = converter.getSchema(id("GetThing")).getTrait(TraitKey.get(HttpTrait.class));
        assertThat(http.getMethod()).isEqualTo("POST");
        assertThat(http.getUri()).isEqualTo("/thing");

        Schema service = converter.getSchema(id("DynService"));
        assertThat(service.getTrait(TraitKey.get(RestJson1Trait.class))).isNotNull();
        assertThat(service.getTrait(TraitKey.get(SigV4Trait.class)).getName()).isEqualTo("dyn");

        Schema error = converter.getSchema(id("NotFound"));
        assertThat(error.getTrait(TraitKey.get(ErrorTrait.class)).isClientError()).isTrue();
        assertThat(error.getTrait(TraitKey.get(HttpErrorTrait.class)).getCode()).isEqualTo(404);
        assertThat(converter.getSchema(id("EpochTime")).getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT).getValue())
            .isEqualTo("epoch-seconds");
        assertThat(converter.getSchema(id("Stream")).hasTrait(TraitKey.STREAMING_TRAIT)).isTrue();
    }

    @Test
    void getSchema_excludedTraits_areOmitted() {
        Schema input = converter.getSchema(id("GetThingInput"));

        // documentation, length and c2j marker traits (Requirements 9.3, 9.4), and input, which is not in the policy.
        assertThat(traitIds(input)).isEmpty();
        assertThat(traitIds(input.member("id"))).containsExactlyInAnyOrder("smithy.api#httpHeader",
                                                                          "smithy.api#required");
        assertThat(traitIds(input.member("count"))).containsExactly("smithy.api#jsonName");
        assertThat(traitIds(input.member("verbose"))).containsExactly("smithy.api#httpQuery");
        assertThat(traitIds(converter.getSchema(id("GetThingOutput")).member("patterned"))).isEmpty();
        assertThat(traitIds(converter.getSchema(id("GetThing")))).containsExactly("smithy.api#http");
        assertThat(traitIds(converter.getSchema(id("DynService"))))
            .containsExactlyInAnyOrder("aws.protocols#restJson1", "aws.auth#sigv4");
    }

    @Test
    void getSchema_memberSchema_mergesTargetTraitsButKeepsDirectTraitsApart() {
        Schema secret = converter.getSchema(id("GetThingOutput")).member("secret");

        assertThat(secret.getDirectTrait(TraitKey.SENSITIVE_TRAIT)).isNull();
        assertThat(secret.getTrait(TraitKey.SENSITIVE_TRAIT)).isNotNull();
    }

    @Test
    void getSchema_protocolDefinitionTraitWithoutTypedClass_isGenericTraitWithTheModelValue() {
        Model model = TestModels.customProtocol();
        SchemaConverter custom = new SchemaConverter(model);
        Schema value = custom.getSchema(ShapeId.from("test.custom#PingInput")).member("value");

        Trait meta = value.getDirectGenericTrait(sdkId(ShapeId.from("test.custom#customMeta")));
        assertThat(meta).isInstanceOf(GenericTrait.class);
        Map<String, Document> expected = new LinkedHashMap<>();
        expected.put("name", Document.of("n"));
        expected.put("small", Document.of(1));
        expected.put("big", Document.of(5000000000L));
        expected.put("huge", Document.of(new java.math.BigInteger("99999999999999999999")));
        expected.put("ratio", Document.of(1.5));
        expected.put("items", Document.of(Arrays.asList(Document.of("x"), Document.of("y"))));
        assertThat(meta.toDocument()).isEqualTo(Document.of(expected));
        assertThat(new ArrayList<>(meta.toDocument().getMemberNames()))
            .containsExactly("name", "small", "big", "huge", "ratio", "items");
        // Not listed by any protocol definition.
        assertThat(value.getDirectGenericTrait(sdkId(ShapeId.from("test.custom#unlisted")))).isNull();
        assertThat(custom.getSchema(ShapeId.from("test.custom#CustomService")).getGenericTrait(
            sdkId(ShapeId.from("test.custom#customProtocol")))).isNotNull();
    }

    @Test
    void getSchema_serviceConstructor_usesOnlyThatServicesProtocolTraits() {
        Model model = TestModels.converter();
        SchemaConverter withService = new SchemaConverter(model, SERVICE);
        SchemaConverter allServices = new SchemaConverter(model);

        assertThat(traitIds(withService.getSchema(id("GetThingInput")).member("id")))
            .contains("smithy.api#httpHeader");
        assertThat(traitIds(allServices.getSchema(id("GetThingInput")).member("id")))
            .contains("smithy.api#httpHeader");

        // With no service in the model, only the default traits are included.
        Model withoutService = model.toBuilder().removeShape(SERVICE.getId()).build();
        assertThat(traitIds(new SchemaConverter(withoutService).getSchema(id("GetThingInput")).member("id")))
            .containsExactly("smithy.api#required");
    }

    @Test
    void getSchema_sameShape_returnsTheSameInstance() {
        Schema first = converter.getSchema(id("GetThingOutput"));

        assertThat(converter.getSchema(id("GetThingOutput"))).isSameAs(first);
        assertThat(converter.getSchema(MODEL.expectShape(id("GetThingOutput")))).isSameAs(first);
        assertThat(converter.getSchemaIndex().getSchema(sdkId(id("GetThingOutput")))).isSameAs(first);
        assertThat(converter.getSchemaIndex().getSchema(sdkId(id("GetThingOutput").withMember("nested"))))
            .isSameAs(first.member("nested"));
    }

    @Test
    void getSchema_twoConverters_doNotShareSchemas() {
        SchemaConverter other = new SchemaConverter(MODEL, SERVICE);

        assertThat(other.getSchema(id("GetThingOutput"))).isNotSameAs(converter.getSchema(id("GetThingOutput")));
    }

    @Test
    void getSchemaIndex_visit_seesConvertedSchemasAndUnknownIdsAreNull() {
        converter.getSchema(id("Nested"));
        List<String> visited = new ArrayList<>();
        converter.getSchemaIndex().visit(s -> visited.add(s.id().toString()));

        assertThat(visited).containsExactlyInAnyOrder("test.dyn#Nested", "test.dyn#StringList", "smithy.api#String");
        assertThat(converter.getSchemaIndex().getSchema(sdkId(id("Missing")))).isNull();
        assertThat(converter.getSchemaIndex().getSchema(sdkId(id("Nested").withMember("missing")))).isNull();
    }

    @Test
    void getSchema_unknownShapeOrMember_throwsNamingTheId() {
        assertThatThrownBy(() -> converter.getSchema(id("Missing")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Shape test.dyn#Missing is not in the model");
        assertThatThrownBy(() -> converter.getSchema(id("Nested").withMember("missing")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Member test.dyn#Nested$missing is not in the model");
    }

    @Test
    void getSchema_shapeOfAnotherModel_throws() {
        StructureShape foreign = StructureShape.builder().id(id("Nested")).build();

        assertThatThrownBy(() -> converter.getSchema(foreign))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("test.dyn#Nested is not in the converter's model");
    }

    @Test
    void constructor_serviceNotInModel_throws() {
        ServiceShape foreign = ServiceShape.builder().id(id("Other")).version("1").build();

        assertThatThrownBy(() -> new SchemaConverter(MODEL, foreign))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Service test.dyn#Other is not in the model");
    }

    @Test
    void getSchema_memberTargetMissingFromModel_throwsNamingItAndCachesNothing() {
        // Model.builder() does not validate, so a member can target a shape that is not in the model.
        ShapeId ok = ShapeId.from("test.bad#Ok");
        ShapeId outer = ShapeId.from("test.bad#Outer");
        StringShape text = StringShape.builder().id("test.bad#Text").build();
        Model model = Model.builder()
                           .addShape(text)
                           .addShape(StructureShape.builder().id(ok).addMember("text", text.getId()).build())
                           .addShape(StructureShape.builder().id(outer)
                                                   .addMember("ok", ok)
                                                   .addMember(MemberShape.builder().id(outer.withMember("bad"))
                                                                         .target("test.bad#Missing").build())
                                                   .build())
                           .build();
        SchemaConverter bad = new SchemaConverter(model);

        assertThatThrownBy(() -> bad.getSchema(outer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Shape test.bad#Missing targeted by member test.bad#Outer$bad is not in the model");
        // Ok and Text were converted before the failure, but nothing was published.
        List<Schema> cached = new ArrayList<>();
        bad.getSchemaIndex().visit(cached::add);
        assertThat(cached).isEmpty();
        // A second attempt fails the same way, and shapes outside the broken closure still convert.
        assertThatThrownBy(() -> bad.getSchema(outer)).isInstanceOf(IllegalArgumentException.class);
        Schema okSchema = bad.getSchema(ok);
        assertThat(okSchema.member("text").memberTarget()).isSameAs(bad.getSchema(text.getId()));
    }

    @Test
    void getSchema_structureSchema_hasAStructDocumentShapeBuilder() {
        Schema nested = converter.getSchema(id("Nested"));
        ShapeBuilder<?> builder = nested.shapeBuilder();

        assertThat(builder).isInstanceOf(SchemaGuidedDocumentBuilder.class);
        assertThat(builder.sdkSchema()).isSameAs(nested);
        assertThat(nested.shapeBuilder()).isNotSameAs(builder);
        assertThat(converter.getSchema(id("Choice")).shapeBuilder().sdkSchema())
            .isSameAs(converter.getSchema(id("Choice")));
        assertThat(converter.getSchema(id("StringList")).shapeBuilder()).isNull();
    }

    @Test
    void createDocumentBuilder_nonAggregateSchema_throws() {
        assertThatThrownBy(() -> SchemaConverter.createDocumentBuilder(converter.getSchema(id("StringList"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("StructDocument can only deserialize a structure or union");
    }

    @Test
    void getSchema_selfRecursion_targetsTheSameInstance() {
        SchemaConverter recursive = new SchemaConverter(TestModels.recursive());
        Schema self = recursive.getSchema(rec("SelfRef"));

        assertThat(self.member("next").memberTarget()).isSameAs(self);
        assertThat(self.member("next").memberTarget().member("next").memberTarget()).isSameAs(self);
        assertThat(names(self.member("next").members())).containsExactly("name", "next");
    }

    @Test
    void getSchema_mutualRecursion_targetsTheSameInstancesInEitherOrder() {
        SchemaConverter aFirst = new SchemaConverter(TestModels.recursive());
        Schema a = aFirst.getSchema(rec("MutualA"));
        Schema b = aFirst.getSchema(rec("MutualB"));
        assertThat(a.member("b").memberTarget()).isSameAs(b);
        assertThat(b.member("a").memberTarget()).isSameAs(a);

        SchemaConverter bFirst = new SchemaConverter(TestModels.recursive());
        Schema b2 = bFirst.getSchema(rec("MutualB"));
        Schema a2 = bFirst.getSchema(rec("MutualA"));
        assertThat(a2.member("b").memberTarget()).isSameAs(b2);
        assertThat(b2.member("a").memberTarget()).isSameAs(a2);
        assertThat(names(b2.member("a").memberTarget().member("b").memberTarget().members()))
            .containsExactly("a", "name");
    }

    @Test
    void getSchema_recursionThroughListMapAndUnion_targetsTheSameInstances() {
        SchemaConverter recursive = new SchemaConverter(TestModels.recursive());
        Schema root = recursive.getSchema(rec("Root"));

        Schema tree = recursive.getSchema(rec("TreeNode"));
        Schema treeList = recursive.getSchema(rec("TreeNodeList"));
        assertThat(root.member("tree").memberTarget()).isSameAs(tree);
        assertThat(tree.member("children").memberTarget()).isSameAs(treeList);
        assertThat(treeList.listMember().memberTarget()).isSameAs(tree);

        Schema mapNode = recursive.getSchema(rec("MapNode"));
        Schema mapNodeMap = recursive.getSchema(rec("MapNodeMap"));
        assertThat(mapNode.member("children").memberTarget()).isSameAs(mapNodeMap);
        assertThat(mapNodeMap.mapValueMember().memberTarget()).isSameAs(mapNode);

        Schema expr = recursive.getSchema(rec("Expr"));
        assertThat(expr.type()).isEqualTo(ShapeType.UNION);
        assertThat(expr.member("negate").memberTarget()).isSameAs(expr);
        assertThat(recursive.getSchema(rec("ExprList")).listMember().memberTarget()).isSameAs(expr);
        assertThat(root.member("self").memberTarget()).isSameAs(recursive.getSchema(rec("SelfRef")));
    }

    private static List<String> names(List<Schema> members) {
        return members.stream().map(Schema::memberName).collect(Collectors.toList());
    }

    private static List<String> traitIds(Schema schema) {
        List<String> ids = new ArrayList<>();
        for (Trait trait : schema.isMember() ? directTraits(schema) : schema.traits()) {
            ids.add(trait.traitId().toString());
        }
        return ids;
    }

    /**
     * The traits a member declares itself: its trait view minus the traits it inherits unchanged from its target.
     */
    private static List<Trait> directTraits(Schema member) {
        List<Trait> result = new ArrayList<>();
        for (Trait trait : member.traits()) {
            if (!member.memberTarget().traits().contains(trait)) {
                result.add(trait);
            }
        }
        return result;
    }
}
