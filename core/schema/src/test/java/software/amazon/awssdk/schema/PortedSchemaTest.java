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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.DefaultTrait;
import software.amazon.awssdk.schema.traits.GenericTrait;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.RequiredTrait;
import software.amazon.awssdk.schema.traits.SensitiveTrait;
import software.amazon.awssdk.schema.traits.Trait;

/**
 * Port of smithy-java 1.7.0 {@code software.amazon.smithy.java.core.schema.SchemaTest} (task 1.18), plus the two
 * schema-level checks from {@code ValidatorTest} that do not depend on the (unported) validator or presence tracker.
 *
 * <p>Adaptations: {@code Set.of} becomes {@code LinkedHashSet}; annotation traits are SDK singletons; the source's
 * {@code DocumentationTrait} has no SDK typed trait (the Trait_Inclusion_Policy excludes {@code documentation}), so
 * it is a {@link GenericTrait} read through {@link Schema#getGenericTrait(ShapeId)}.
 */
class PortedSchemaTest {

    private static final ShapeId DOCUMENTATION = ShapeId.from("smithy.api#documentation");

    @Test
    void enumWorksWithEnumSchema() {
        Schema schema = Schema.createEnum(ShapeId.from("smithy.example#Foo"),
                                          new LinkedHashSet<>(Arrays.asList("a", "b")));

        assertThat(schema.stringEnumValues()).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void intEnumWorksWithIntEnumSchema() {
        Schema schema = Schema.createIntEnum(ShapeId.from("smithy.example#Foo"),
                                             new LinkedHashSet<>(Arrays.asList(1, 2, 3)));

        assertThat(schema.intEnumValues()).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void directlyRecursiveSchema() {
        SchemaBuilder builder = Schema.structureBuilder(ShapeId.from("smithy.example#Foo"), SensitiveTrait.INSTANCE);
        Schema directlyRecursiveSchema = builder
            .putMember("foo", builder, documentation("Hi"))
            .build();

        assertThat(directlyRecursiveSchema.members()).hasSize(1);
        assertThat(directlyRecursiveSchema.member("foo").memberTarget()).isSameAs(directlyRecursiveSchema);
        assertThat(directlyRecursiveSchema.member("foo").hasTrait(TraitKey.get(SensitiveTrait.class))).isTrue();
        assertThat(directlyRecursiveSchema.member("foo").type()).isEqualTo(ShapeType.STRUCTURE);
    }

    @Test
    void supportsMutualRecursion() {
        SchemaBuilder aBuilder = Schema.structureBuilder(ShapeId.from("smithy.example#A"));
        SchemaBuilder bBuilder = Schema.structureBuilder(ShapeId.from("smithy.example#B"));

        Schema mutuallyRecursiveA = aBuilder.putMember("b", bBuilder).build();
        Schema mutuallyRecursiveB = bBuilder.putMember("a", mutuallyRecursiveA).build();

        assertThat(mutuallyRecursiveA.members()).hasSize(1);
        assertThat(mutuallyRecursiveB.members()).hasSize(1);
        // Schema equality is identity in the SDK, so the source's equalTo is the same as isSameAs.
        assertThat(mutuallyRecursiveA.member("b").memberTarget()).isSameAs(mutuallyRecursiveB);
        assertThat(mutuallyRecursiveB.member("a").memberTarget()).isSameAs(mutuallyRecursiveA);
    }

    /**
     * Adapted: checked once with the generic {@code documentation} trait (as in the source) and once with a typed
     * trait ({@code jsonName}), because the SDK stores the two kinds separately.
     */
    @Test
    void supportsSupercedingTargetTraits() {
        Schema intSchema = Schema.createInteger(ShapeId.from("smithy.example#DocumentedInt"),
                                                documentation("Target"), new JsonNameTrait("target"));
        Schema structWithMember = Schema.structureBuilder(ShapeId.from("smithy.example#StructWithMember"))
                                        .putMember("member", intSchema, documentation("Member"),
                                                   new JsonNameTrait("member"))
                                        .build();

        Schema member = structWithMember.member("member");
        assertThat(member.getGenericTrait(DOCUMENTATION).value().asString()).isEqualTo("Member");
        assertThat(member.expectTrait(TraitKey.get(JsonNameTrait.class)).getValue()).isEqualTo("member");
    }

    /**
     * Adapted from {@code ValidatorTest.validatesRequiredMembersOfBigStructs}. The validator is not ported (OD-13;
     * required-member enforcement lands with the presence tracker), so instead of counting validation failures this
     * checks the schema's required-member summary that the presence tracker reads: the number of required members
     * without a default equals the source's expected failure count, and each member's flag and mask bit match.
     */
    @ParameterizedTest
    @MethodSource("validatesRequiredMembersOfBigStructsProvider")
    void validatesRequiredMembersOfBigStructs(int totalMembers, int requiredCount, int defaultedCount, int failures) {
        Schema struct = createBigRequiredSchema(totalMembers, requiredCount, defaultedCount);

        assertThat(struct.requiredMemberCount()).isEqualTo(failures);
        for (Schema member : struct.members()) {
            int i = member.memberIndex();
            boolean expected = i < requiredCount && i >= defaultedCount;
            assertThat(member.isRequiredByValidation).as(member.memberName()).isEqualTo(expected);
            if (i < 64) {
                assertThat((struct.requiredMemberMask() & (1L << i)) != 0).as(member.memberName()).isEqualTo(expected);
            }
        }
    }

    static List<Arguments> validatesRequiredMembersOfBigStructsProvider() {
        return Arrays.asList(
            // int totalMembers, int requiredCount, int defaultedCount, int failures
            Arguments.of(100, 100, 0, 100),
            Arguments.of(100, 80, 0, 80),
            Arguments.of(100, 80, 20, 60),
            Arguments.of(100, 100, 100, 0),
            Arguments.of(1000, 10, 0, 10),
            Arguments.of(0, 0, 0, 0),
            Arguments.of(63, 63, 0, 63),
            Arguments.of(64, 64, 0, 64),
            Arguments.of(65, 65, 0, 65));
    }

    /**
     * Adapted from {@code ValidatorTest.presenceTrackerType} and {@code PresenceTrackerTest.presenceTrackerType}.
     * smithy-java picks a no-op, a 64-bit, or a big presence tracker from the required-member count. The SDK presence
     * tracker is not ported yet; this checks the summary it will choose from: no required members means a zero mask,
     * up to 64 fit the mask, and more than 64 overflow it ({@code requiredMemberCount() > bitCount(mask)}).
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 63, 64, 65, 128})
    void presenceTrackerType(int requiredFields) {
        Schema schema = createBigRequiredSchema(requiredFields, requiredFields, 0);

        long expectedMask = requiredFields >= 64 ? -1L : (1L << requiredFields) - 1;
        assertThat(schema.requiredMemberMask()).isEqualTo(expectedMask);
        assertThat(schema.requiredMemberCount()).isEqualTo(requiredFields);
        assertThat(schema.requiredMemberCount() > Long.bitCount(schema.requiredMemberMask()))
            .isEqualTo(requiredFields > 64);
    }

    private static Schema createBigRequiredSchema(int totalMembers, int requiredCount, int defaultedCount) {
        SchemaBuilder builder = Schema.structureBuilder(ShapeId.from("smithy.example#Foo"));
        for (int i = 0; i < totalMembers; i++) {
            String name = "member" + i;
            List<Trait> traits = new ArrayList<>();
            if (i < requiredCount) {
                traits.add(RequiredTrait.INSTANCE);
            }
            if (i < defaultedCount) {
                traits.add(new DefaultTrait(Document.of("")));
            }
            builder.putMember(name, PreludeSchemas.STRING, traits.toArray(new Trait[0]));
        }
        return builder.build();
    }

    private static GenericTrait documentation(String value) {
        return new GenericTrait(DOCUMENTATION, Document.of(value));
    }
}
