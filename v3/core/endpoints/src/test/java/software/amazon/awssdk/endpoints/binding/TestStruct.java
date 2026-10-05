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

package software.amazon.awssdk.endpoints.binding;

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaBuilder;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A schema-backed structure for binding tests: every member is a string-targeting member of a structure schema, and
 * {@link #getMemberValue} reads the value by the member schema, as generated types do.
 */
record TestStruct(Schema schema, Map<String, Object> values) implements SerializableStruct {

    /** A structure with members {@code names} and the given values (by member name). */
    static TestStruct of(Map<String, Object> values, String... names) {
        SchemaBuilder builder = Schema.structureBuilder(ShapeId.from("test#Input"));
        for (String name : names) {
            builder.putMember(name, PreludeSchemas.STRING);
        }
        return new TestStruct(builder.build(), new LinkedHashMap<>(values));
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        if (schema.member(member.memberName()) != member) {
            throw new IllegalArgumentException("Not a member of " + schema.id() + ": " + member.id());
        }
        return (T) values.get(member.memberName());
    }
}
