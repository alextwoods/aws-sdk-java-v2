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

package software.amazon.awssdk.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.DefaultTrait;
import software.amazon.awssdk.schema.traits.GenericTrait;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.RequiredTrait;
import software.amazon.awssdk.schema.traits.SensitiveTrait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;

/**
 * Example-based tests for the schema hierarchy (Requirement 3, 4.6, 4.7).
 */
class SchemaTest {
    private static final String NS = "com.example";
    private static final ShapeId CUSTOM_TRAIT_ID = ShapeId.fromParts(NS, "custom");

    private static ShapeId id(String name) {
        return ShapeId.fromParts(NS, name);
    }

    // Requirement 3.2

    @Test
    void rootSchema_reportsNoMemberInfo() {
        Schema string = Schema.createString(id("Name"));

        assertThat(string.id()).isEqualTo(id("Name"));
        assertThat(string.type()).isEqualTo(ShapeType.STRING);
        assertThat(string.isMember()).isFalse();
        assertThat(string.memberName()).isNull();
        assertThat(string.memberIndex()).isEqualTo(-1);
        assertThat(string.memberTarget()).isNull();
        assertThat(string.members()).isEmpty();
        assertThat(string.member("x")).isNull();
        assertThat(string.member(0)).isNull();
    }

    @Test
    void memberSchema_reportsMemberInfo() {
        Schema struct = Schema.structureBuilder(id("S"))
                              .putMember("a", PreludeSchemas.STRING)
                              .putMember("b", PreludeSchemas.INTEGER)
                              .build();

        Schema b = struct.member("b");
        assertThat(b.isMember()).isTrue();
        assertThat(b.memberName()).isEqualTo("b");
        assertThat(b.memberIndex()).isEqualTo(1);
        assertThat(b.id()).isEqualTo(ShapeId.fromParts(NS, "S", "b"));
        assertThat(b.memberTarget()).isSameAs(PreludeSchemas.INTEGER);
    }

    @Test
    void simpleFactories_createSchemasOfEachType() {
        assertThat(Schema.createBoolean(id("A")).type()).isEqualTo(ShapeType.BOOLEAN);
        assertThat(Schema.createByte(id("A")).type()).isEqualTo(ShapeType.BYTE);
        assertThat(Schema.createShort(id("A")).type()).isEqualTo(ShapeType.SHORT);
        assertThat(Schema.createInteger(id("A")).type()).isEqualTo(ShapeType.INTEGER);
        assertThat(Schema.createLong(id("A")).type()).isEqualTo(ShapeType.LONG);
        assertThat(Schema.createFloat(id("A")).type()).isEqualTo(ShapeType.FLOAT);
        assertThat(Schema.createDouble(id("A")).type()).isEqualTo(ShapeType.DOUBLE);
        assertThat(Schema.createBigInteger(id("A")).type()).isEqualTo(ShapeType.BIG_INTEGER);
        assertThat(Schema.createBigDecimal(id("A")).type()).isEqualTo(ShapeType.BIG_DECIMAL);
        assertThat(Schema.createString(id("A")).type()).isEqualTo(ShapeType.STRING);
        assertThat(Schema.createBlob(id("A")).type()).isEqualTo(ShapeType.BLOB);
        assertThat(Schema.createDocument(id("A")).type()).isEqualTo(ShapeType.DOCUMENT);
        assertThat(Schema.createTimestamp(id("A")).type()).isEqualTo(ShapeType.TIMESTAMP);
        assertThat(Schema.createOperation(id("A")).type()).isEqualTo(ShapeType.OPERATION);
        assertThat(Schema.createResource(id("A")).type()).isEqualTo(ShapeType.RESOURCE);
        assertThat(Schema.createService(id("A")).type()).isEqualTo(ShapeType.SERVICE);
        assertThat(Schema.createEnum(id("A"), setOf("x")).type()).isEqualTo(ShapeType.ENUM);
        assertThat(Schema.createIntEnum(id("A"), setOf(1)).type()).isEqualTo(ShapeType.INT_ENUM);
    }

    @Test
    void factories_nullId_throw() {
        assertThatThrownBy(() -> Schema.createString(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Schema.createEnum(null, setOf("x"))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Schema.createEnum(id("E"), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Schema.structureBuilder(null)).isInstanceOf(NullPointerException.class);
    }

    // Requirement 3.3

    @Test
    void members_areInDeclarationOrder_evenWhenRequiredMembersComeLast() {
        Schema struct = Schema.structureBuilder(id("S"))
                              .putMember("zeta", PreludeSchemas.STRING)
                              .putMember("alpha", PreludeSchemas.STRING, RequiredTrait.INSTANCE)
                              .putMember("mid", PreludeSchemas.STRING)
                              .build();

        assertThat(memberNames(struct)).containsExactly("zeta", "alpha", "mid");
        assertThat(struct.member("alpha").memberIndex()).isEqualTo(1);
    }

    @Test
    void members_listIsUnmodifiable() {
        Schema struct = Schema.structureBuilder(id("S")).putMember("a", PreludeSchemas.STRING).build();
        List<Schema> members = struct.members();

        assertThatThrownBy(() -> members.add(PreludeSchemas.STRING)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> members.remove(0)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> members.set(0, null)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(memberNames(struct)).containsExactly("a");
    }

    @Test
    void memberByName_findsEachMember_forOneTwoAndManyMembers() {
        for (int count = 1; count <= 5; count++) {
            SchemaBuilder builder = Schema.structureBuilder(id("S" + count));
            for (int i = 0; i < count; i++) {
                builder.putMember("m" + i, PreludeSchemas.STRING);
            }
            Schema struct = builder.build();
            for (int i = 0; i < count; i++) {
                assertThat(struct.member("m" + i).memberName()).isEqualTo("m" + i);
            }
            assertThat(struct.member("absent")).isNull();
        }
    }

    @Test
    void listAndMapMembers_areFound() {
        Schema list = Schema.listBuilder(id("L")).putMember("member", PreludeSchemas.STRING).build();
        Schema map = Schema.mapBuilder(id("M"))
                           .putMember("key", PreludeSchemas.STRING)
                           .putMember("value", PreludeSchemas.INTEGER)
                           .build();

        assertThat(list.listMember().memberName()).isEqualTo("member");
        assertThat(list.listMember()).isSameAs(list.listMember()).isSameAs(list.member("member"));
        assertThat(map.mapKeyMember().memberTarget()).isSameAs(PreludeSchemas.STRING);
        assertThat(map.mapValueMember().memberTarget()).isSameAs(PreludeSchemas.INTEGER);
    }

    @Test
    void listAndMapMembers_onWrongType_throw() {
        Schema struct = Schema.structureBuilder(id("S")).putMember("member", PreludeSchemas.STRING).build();

        assertThatThrownBy(struct::listMember).isInstanceOf(IllegalStateException.class).hasMessageContaining("com.example#S");
        assertThatThrownBy(struct::mapKeyMember).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(PreludeSchemas.STRING::mapValueMember).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void enumValues_areUnmodifiableCopiesInOrder() {
        Set<String> values = new LinkedHashSet<>(Arrays.asList("b", "a", "c"));
        Schema stringEnum = Schema.createEnum(id("E"), values);
        values.add("d");

        assertThat(stringEnum.stringEnumValues()).containsExactly("b", "a", "c");
        assertThatThrownBy(() -> stringEnum.stringEnumValues().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThat(stringEnum.stringEnumValues()).containsExactly("b", "a", "c");
        assertThat(stringEnum.intEnumValues()).isEmpty();

        Schema intEnum = Schema.createIntEnum(id("I"), setOf(3, 1, 2));
        assertThat(intEnum.intEnumValues()).containsExactly(3, 1, 2);
        assertThatThrownBy(() -> intEnum.intEnumValues().remove(1)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(intEnum.intEnumValues()).containsExactly(3, 1, 2);
        assertThat(intEnum.stringEnumValues()).isEmpty();
    }

    @Test
    void enumValues_ofNonEnums_areEmpty() {
        assertThat(PreludeSchemas.STRING.stringEnumValues()).isEmpty();
        assertThat(PreludeSchemas.INTEGER.intEnumValues()).isEmpty();
    }

    // Requirement 3.4

    @Test
    void schemaTypes_declareNoTypeParameters() {
        List<Class<?>> types = Arrays.asList(Schema.class, RootSchema.class, MemberSchema.class,
                                             DeferredRootSchema.class, DeferredMemberSchema.class,
                                             ResolvedRootSchema.class);
        for (Class<?> type : types) {
            assertThat(type.getTypeParameters()).as(type.getName()).isEmpty();
        }
    }

    // Requirement 3.5

    @Test
    void shapeBuilder_invokesSupplierOnEveryCall() {
        // A supplier of a concrete builder type is accepted (Supplier<? extends ShapeBuilder<?>>).
        Supplier<TestStruct.Builder> supplier = TestStruct::builder;
        Schema struct = Schema.structureBuilder(id("S"))
                              .putMember("a", PreludeSchemas.STRING)
                              .builderSupplier(supplier)
                              .shapeClass(TestStruct.class)
                              .build();

        ShapeBuilder<? extends SerializableShape> first = struct.shapeBuilder();
        ShapeBuilder<? extends SerializableShape> second = struct.shapeBuilder();
        assertThat(first).isInstanceOf(TestStruct.Builder.class);
        assertThat(second).isInstanceOf(TestStruct.Builder.class).isNotSameAs(first);
        assertThat(struct.shapeClass()).isEqualTo(TestStruct.class);
    }

    @Test
    void shapeBuilderAndClass_absent_returnNull() {
        Schema struct = Schema.structureBuilder(id("S")).putMember("a", PreludeSchemas.STRING).build();

        assertThat(struct.shapeBuilder()).isNull();
        assertThat(struct.shapeClass()).isNull();
        assertThat(PreludeSchemas.STRING.shapeBuilder()).isNull();
        assertThat(struct.member("a").shapeClass()).isNull();
    }

    @Test
    void enumFactories_withShapeClass_exposeIt() {
        assertThat(Schema.createEnum(id("E"), setOf("a"), StringBuilder.class).shapeClass())
            .isEqualTo(StringBuilder.class);
        assertThat(Schema.createIntEnum(id("I"), setOf(1), StringBuilder.class).shapeClass())
            .isEqualTo(StringBuilder.class);
    }

    // Requirement 3.6

    @Test
    void memberSchema_delegatesShapeQueriesToTarget() {
        Schema inner = Schema.structureBuilder(id("Inner"))
                             .putMember("x", PreludeSchemas.STRING)
                             .putMember("y", PreludeSchemas.INTEGER)
                             .build();
        Schema list = Schema.listBuilder(id("L")).putMember("member", PreludeSchemas.STRING).build();
        Schema map = Schema.mapBuilder(id("M"))
                           .putMember("key", PreludeSchemas.STRING)
                           .putMember("value", PreludeSchemas.STRING)
                           .build();
        Schema stringEnum = Schema.createEnum(id("E"), setOf("a", "b"));
        Schema intEnum = Schema.createIntEnum(id("I"), setOf(1, 2));
        Schema outer = Schema.structureBuilder(id("Outer"))
                             .putMember("inner", inner)
                             .putMember("list", list)
                             .putMember("map", map)
                             .putMember("stringEnum", stringEnum)
                             .putMember("intEnum", intEnum)
                             .build();

        for (Schema member : outer.members()) {
            Schema target = member.memberTarget();
            assertThat(member.type()).isEqualTo(target.type()).isNotEqualTo(ShapeType.MEMBER);
            assertThat(member.members()).isEqualTo(target.members());
            assertThat(member.stringEnumValues()).isEqualTo(target.stringEnumValues());
            assertThat(member.intEnumValues()).isEqualTo(target.intEnumValues());
            assertThat(member.requiredMemberMask()).isEqualTo(target.requiredMemberMask());
        }
        Schema innerMember = outer.member("inner");
        assertThat(innerMember.member("y")).isSameAs(inner.member("y"));
        assertThat(innerMember.member(1)).isSameAs(inner.member(1));
        assertThat(outer.member("list").listMember()).isSameAs(list.listMember());
        assertThat(outer.member("map").mapKeyMember()).isSameAs(map.mapKeyMember());
        assertThat(outer.member("map").mapValueMember()).isSameAs(map.mapValueMember());
    }

    @Test
    void member_cannotTargetAMember() {
        Schema struct = Schema.structureBuilder(id("S")).putMember("a", PreludeSchemas.STRING).build();

        assertThatThrownBy(() -> Schema.structureBuilder(id("T")).putMember("b", struct.member("a")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#T$b")
            .hasMessageContaining("com.example#S$a");
    }

    // Ported from smithy-java 2fd0f976f (SchemaTest)

    @Test
    void assertMemberTargetIs_returnsMemberWhenTargetMatches() {
        Schema struct = Schema.structureBuilder(id("S")).putMember("a", PreludeSchemas.STRING).build();
        Schema member = struct.member("a");

        assertThat(member.assertMemberTargetIs(PreludeSchemas.STRING)).isSameAs(member);
    }

    @Test
    void assertMemberTargetIs_throwsOnMismatchedTarget() {
        Schema struct = Schema.structureBuilder(id("S")).putMember("a", PreludeSchemas.STRING).build();
        Schema member = struct.member("a");

        assertThatThrownBy(() -> member.assertMemberTargetIs(PreludeSchemas.INTEGER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Expected a member schema that targets smithy.api#Integer, found ");
    }

    @Test
    void assertMemberTargetIs_throwsOnNonMemberSchema() {
        assertThatThrownBy(() -> PreludeSchemas.STRING.assertMemberTargetIs(PreludeSchemas.INTEGER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Expected a member schema that targets smithy.api#Integer, found ");
    }

    @Test
    void assertMemberTargetIs_rejectsNullTargetOnMember() {
        Schema struct = Schema.structureBuilder(id("S")).putMember("a", PreludeSchemas.STRING).build();
        Schema member = struct.member("a");

        assertThatThrownBy(() -> member.assertMemberTargetIs(null)).isInstanceOf(NullPointerException.class);
    }

    // Requirements 3.7 and 3.8

    @Test
    void memberTraits_winOverTargetTraits() {
        Schema target = Schema.createString(id("Target"), new JsonNameTrait("targetName"), SensitiveTrait.INSTANCE,
                                            new GenericTrait(CUSTOM_TRAIT_ID, Document.of("target")));
        Schema struct = Schema.structureBuilder(id("S"))
                              .putMember("both", target, new JsonNameTrait("memberName"),
                                         new GenericTrait(CUSTOM_TRAIT_ID, Document.of("member")))
                              .putMember("targetOnly", target)
                              .putMember("memberOnly", PreludeSchemas.STRING, new XmlNameTrait("x"))
                              .build();

        Schema both = struct.member("both");
        assertThat(both.getTrait(TraitKey.JSON_NAME_TRAIT).getValue()).isEqualTo("memberName");
        assertThat(both.getTrait(TraitKey.SENSITIVE_TRAIT)).isSameAs(SensitiveTrait.INSTANCE);
        assertThat(both.getGenericTrait(CUSTOM_TRAIT_ID).value()).isEqualTo(Document.of("member"));

        Schema targetOnly = struct.member("targetOnly");
        assertThat(targetOnly.getTrait(TraitKey.JSON_NAME_TRAIT).getValue()).isEqualTo("targetName");
        assertThat(targetOnly.hasTrait(TraitKey.SENSITIVE_TRAIT)).isTrue();
        assertThat(targetOnly.getGenericTrait(CUSTOM_TRAIT_ID).value()).isEqualTo(Document.of("target"));

        Schema memberOnly = struct.member("memberOnly");
        assertThat(memberOnly.getTrait(TraitKey.XML_NAME_TRAIT).getValue()).isEqualTo("x");
        assertThat(memberOnly.getTrait(TraitKey.JSON_NAME_TRAIT)).isNull();
        assertThat(memberOnly.getGenericTrait(CUSTOM_TRAIT_ID)).isNull();
    }

    @Test
    void directTraits_returnOnlyMemberDeclaredTraits() {
        Schema target = Schema.createString(id("Target"), new JsonNameTrait("targetName"),
                                            new GenericTrait(CUSTOM_TRAIT_ID, Document.of("target")));
        Schema struct = Schema.structureBuilder(id("S"))
                              .putMember("targetOnly", target)
                              .putMember("both", target, new JsonNameTrait("memberName"))
                              .build();

        assertThat(struct.member("targetOnly").getDirectTrait(TraitKey.JSON_NAME_TRAIT)).isNull();
        assertThat(struct.member("targetOnly").getDirectGenericTrait(CUSTOM_TRAIT_ID)).isNull();
        assertThat(struct.member("both").getDirectTrait(TraitKey.JSON_NAME_TRAIT).getValue()).isEqualTo("memberName");
    }

    @Test
    void directTraits_onRootSchema_equalTraits() {
        Schema target = Schema.createString(id("Target"), new JsonNameTrait("n"),
                                            new GenericTrait(CUSTOM_TRAIT_ID, Document.of(1)));

        assertThat(target.getDirectTrait(TraitKey.JSON_NAME_TRAIT)).isSameAs(target.getTrait(TraitKey.JSON_NAME_TRAIT));
        assertThat(target.getDirectTrait(TraitKey.SENSITIVE_TRAIT)).isNull();
        assertThat(target.getDirectGenericTrait(CUSTOM_TRAIT_ID)).isSameAs(target.getGenericTrait(CUSTOM_TRAIT_ID));
    }

    @Test
    void expectTrait_absent_throws() {
        assertThat(Schema.createString(id("A"), SensitiveTrait.INSTANCE).expectTrait(TraitKey.SENSITIVE_TRAIT))
            .isSameAs(SensitiveTrait.INSTANCE);
        assertThatThrownBy(() -> PreludeSchemas.STRING.expectTrait(TraitKey.SENSITIVE_TRAIT))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("SensitiveTrait");
    }

    // Requirements 4.6 and 4.7

    @Test
    void traits_containsMergedTraitsOnceAndIsUnmodifiable() {
        GenericTrait custom = new GenericTrait(CUSTOM_TRAIT_ID, Document.of("v"));
        Schema target = Schema.createString(id("Target"), new JsonNameTrait("t"), SensitiveTrait.INSTANCE);
        Schema struct = Schema.structureBuilder(id("S"))
                              .putMember("m", target, new JsonNameTrait("m"), custom)
                              .build();

        Schema member = struct.member("m");
        assertThat(member.traits()).containsExactlyInAnyOrder(new JsonNameTrait("m"), SensitiveTrait.INSTANCE, custom);
        assertThatThrownBy(() -> member.traits().add(RequiredTrait.INSTANCE))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(member.traits()).hasSize(3);
        assertThat(PreludeSchemas.STRING.traits()).isEmpty();
    }

    // Requirement 3.11

    @Test
    void memberIndexes_areContiguousAndAcceptedByIndexLookup() {
        SchemaBuilder builder = Schema.structureBuilder(id("S"));
        for (int i = 0; i < 10; i++) {
            builder.putMember("m" + (9 - i), PreludeSchemas.STRING);
        }
        Schema struct = builder.build();

        for (int i = 0; i < 10; i++) {
            Schema member = struct.members().get(i);
            assertThat(member.memberIndex()).isEqualTo(i);
            assertThat(struct.member(i)).isSameAs(member);
        }
        assertThat(struct.member(-1)).isNull();
        assertThat(struct.member(10)).isNull();
    }

    // Required-member bitmask (OD-2)

    @Test
    void requiredMemberMask_marksRequiredMembersWithoutDefault() {
        Schema struct = Schema.structureBuilder(id("S"))
                              .putMember("optional", PreludeSchemas.STRING)
                              .putMember("required", PreludeSchemas.STRING, RequiredTrait.INSTANCE)
                              .putMember("requiredWithDefault", PreludeSchemas.STRING, RequiredTrait.INSTANCE,
                                         new DefaultTrait(Document.of("d")))
                              .putMember("requiredWithNullDefault", PreludeSchemas.STRING, RequiredTrait.INSTANCE,
                                         new DefaultTrait(null))
                              .putMember("requiredPrimitive", PreludeSchemas.PRIMITIVE_INTEGER, RequiredTrait.INSTANCE)
                              .build();

        assertThat(struct.requiredMemberMask()).isEqualTo((1L << 1) | (1L << 3));
        assertThat(struct.requiredMemberCount()).isEqualTo(2);
        assertThat(struct.member("required").isRequiredByValidation).isTrue();
        assertThat(struct.member("requiredPrimitive").isRequiredByValidation).isFalse();
    }

    @Test
    void requiredMemberMask_coversOnlyIndexesBelow64() {
        SchemaBuilder builder = Schema.structureBuilder(id("Wide"));
        for (int i = 0; i < 70; i++) {
            Trait[] traits = i == 2 || i == 63 || i == 64 || i == 69
                             ? new Trait[] {RequiredTrait.INSTANCE} : new Trait[0];
            builder.putMember("m" + i, PreludeSchemas.STRING, traits);
        }
        Schema struct = builder.build();

        assertThat(struct.requiredMemberMask()).isEqualTo((1L << 2) | (1L << 63));
        assertThat(struct.requiredMemberCount()).isEqualTo(4);
        assertThat(struct.member("m69").isRequiredByValidation).isTrue();
    }

    @Test
    void requiredMemberMask_isZeroForNonStructures() {
        Schema union = Schema.unionBuilder(id("U")).putMember("a", PreludeSchemas.STRING, RequiredTrait.INSTANCE).build();

        assertThat(union.requiredMemberMask()).isZero();
        assertThat(PreludeSchemas.STRING.requiredMemberMask()).isZero();
    }

    // Identity and toString

    @Test
    void equality_isIdentity() {
        Schema a = Schema.createString(id("A"));
        Schema b = Schema.createString(id("A"));

        assertThat(a).isEqualTo(a).isNotEqualTo(b);
        assertThat(a.toString()).isEqualTo("Schema{id='com.example#A', type=string}");
    }

    private static List<String> memberNames(Schema schema) {
        List<String> names = new ArrayList<>();
        for (Schema member : schema.members()) {
            names.add(member.memberName());
        }
        return names;
    }

    @SafeVarargs
    private static <T> Set<T> setOf(T... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(values)));
    }
}
