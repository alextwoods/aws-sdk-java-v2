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

class SchemaUtilsTest {
    private static Schema struct(String name) {
        return Schema.structureBuilder(ShapeId.fromParts("test", name))
                     .putMember("a", PreludeSchemas.STRING)
                     .putMember("b", PreludeSchemas.INTEGER)
                     .build();
    }

    @Test
    void validateSameMember_sameInstance_returnsValue() {
        Schema schema = struct("S");
        assertThat(SchemaUtils.validateSameMember(schema.member("a"), schema.member("a"), "v")).isEqualTo("v");
    }

    @Test
    void validateSameMember_equalMemberOfAnotherSchemaInstance_returnsValue() {
        Schema first = struct("S");
        Schema second = struct("S");
        assertThat(SchemaUtils.validateSameMember(first.member("b"), second.member("b"), 1)).isEqualTo(1);
    }

    @Test
    void validateSameMember_otherMemberWithSameIndex_throws() {
        Schema schema = struct("S");
        Schema other = struct("Other");
        assertThatThrownBy(() -> SchemaUtils.validateSameMember(schema.member("a"), other.member("a"), "v"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("test#S$a")
            .hasMessageContaining("test#Other$a");
    }

    @Test
    void validateSameMember_rootSchema_throws() {
        Schema schema = struct("S");
        assertThatThrownBy(() -> SchemaUtils.validateSameMember(schema.member("a"), PreludeSchemas.STRING, "v"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notAMember_namesTheSchemaAndTheShape() {
        Schema schema = struct("S");
        assertThat(SchemaUtils.notAMember(schema, struct("Other").member("b")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Schema test#Other$b (member index 1) is not a member of test#S");
    }
}
