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

package software.amazon.awssdk.schema.serde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;

/**
 * Tests for the {@link ShapeDeserializer} default methods and consumer interfaces (Requirement 7.2).
 */
class ShapeDeserializerTest {
    private static final Schema STRUCT = Schema.structureBuilder(ShapeId.fromParts("com.example", "S"))
                                               .putMember("a", PreludeSchemas.STRING)
                                               .putMember("b", PreludeSchemas.STRING)
                                               .build();

    @Test
    void containerSize_byDefault_isUnknown() {
        assertThat(new StringStructDeserializer("x").containerSize()).isEqualTo(-1);
    }

    @Test
    void containerPreAllocationLimit_byDefault_isTheConstant() {
        assertThat(new StringStructDeserializer("x").containerPreAllocationLimit())
            .isEqualTo(ShapeDeserializer.CONTAINER_PRE_ALLOCATION_LIMIT);
    }

    @Test
    void containerPreAllocationLimitConstant_withoutSystemProperty_is10000() {
        // The surefire configuration does not set aws.serde.containerPreAllocationLimit.
        assertThat(ShapeDeserializer.CONTAINER_PRE_ALLOCATION_LIMIT).isEqualTo(10_000);
    }

    @Test
    void readNull_byDefault_returnsNull() {
        String value = new StringStructDeserializer("x").readNull();

        assertThat(value).isNull();
    }

    @Test
    void close_byDefault_doesNothing() {
        StringStructDeserializer deserializer = new StringStructDeserializer("x");

        deserializer.close();

        assertThat(deserializer.readString(PreludeSchemas.STRING)).isEqualTo("x");
    }

    @Test
    void readStruct_withLambdaConsumer_unknownMemberDefaultIsNoOp() {
        List<String> seen = new ArrayList<>();

        new StringStructDeserializer("x").readStruct(STRUCT, seen,
                                                     (state, member, d) -> state.add(member.memberName() + "="
                                                                                     + d.readString(member)));

        assertThat(seen).containsExactly("a=x", "b=x");
    }

    @Test
    void readStruct_withUnknownMemberOverride_reportsUnknownMember() {
        List<String> unknown = new ArrayList<>();
        ShapeDeserializer.StructMemberConsumer<List<String>> consumer =
            new ShapeDeserializer.StructMemberConsumer<List<String>>() {
                @Override
                public void accept(List<String> state, Schema memberSchema, ShapeDeserializer memberDeserializer) {
                }

                @Override
                public void unknownMember(List<String> state, String memberName) {
                    state.add(memberName);
                }
            };

        new StringStructDeserializer("x").readStruct(STRUCT, unknown, consumer);

        assertThat(unknown).containsExactly("unknown");
    }
}
