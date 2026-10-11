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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.serde.StringStructDeserializer;

/**
 * Tests for the {@link ShapeBuilder} default methods (Requirement 6.1).
 */
class ShapeBuilderTest {

    @Test
    void deserialize_populatesMembersAndReturnsSameBuilder() {
        TestStruct.Builder builder = TestStruct.builder();

        ShapeBuilder<TestStruct> result = builder.deserialize(new StringStructDeserializer("value"));

        assertThat(result).isSameAs(builder);
        assertThat(builder.build().name()).isEqualTo("value");
    }

    @Test
    void deserializeMember_byDefault_delegatesToDeserialize() {
        TestStruct.Builder builder = TestStruct.builder();
        Schema member = Schema.structureBuilder(ShapeId.fromParts("com.example", "Holder"))
                              .putMember("struct", TestStruct.SCHEMA)
                              .build()
                              .member("struct");

        ShapeBuilder<TestStruct> result = builder.deserializeMember(new StringStructDeserializer("value"), member);

        assertThat(result).isSameAs(builder);
        assertThat(builder.deserializeCalls()).isEqualTo(1);
        assertThat(builder.build().name()).isEqualTo("value");
    }

    @Test
    void setMemberValue_byDefault_throwsNamingShapeAndMember() {
        TestStruct.Builder builder = TestStruct.builder();

        assertThatThrownBy(() -> builder.setMemberValue(TestStruct.NAME, "value"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#TestStruct")
            .hasMessageContaining("com.example#TestStruct$name");
    }

    @Test
    void errorCorrection_byDefault_returnsSameBuilderUnchanged() {
        TestStruct.Builder builder = TestStruct.builder().name("a");

        assertThat(builder.errorCorrection()).isSameAs(builder);
        assertThat(builder.build().name()).isEqualTo("a");
    }
}
