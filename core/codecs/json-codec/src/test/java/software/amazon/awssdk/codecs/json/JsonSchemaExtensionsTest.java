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


package software.amazon.awssdk.codecs.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.codecs.json.internal.JsonSchemaExtensions;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaBuilder;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

public class JsonSchemaExtensionsTest extends ProviderTestBase {

    private static final Schema STRUCT = Schema.structureBuilder(ShapeId.from("test#Struct"))
            .putMember("name", PreludeSchemas.STRING)
            .putMember("renamedField", PreludeSchemas.INTEGER, new JsonNameTrait("renamed"))
            .putMember("createdAt",
                    PreludeSchemas.TIMESTAMP,
                    new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
            .build();

    private static final Schema UNION = Schema.unionBuilder(ShapeId.from("test#Union"))
            .putMember("stringValue", PreludeSchemas.STRING)
            .putMember("intValue", PreludeSchemas.INTEGER)
            .build();

    @Test
    public void memberExtensionInitialized() {
        assertThat(STRUCT.member("name").getExtension(JsonSchemaExtensions.KEY)).isNotNull();
        assertThat(STRUCT.member("renamedField").getExtension(JsonSchemaExtensions.KEY)).isNotNull();
        assertThat(STRUCT.member("createdAt").getExtension(JsonSchemaExtensions.KEY)).isNotNull();
    }

    @Test
    public void memberExtensionCachesTimestampFormatter() {
        JsonSchemaExtensions.JsonSchemaExtension ext = STRUCT.member("createdAt").getExtension(JsonSchemaExtensions.KEY);

        assertThat(ext).isNotNull();
        assertThat(ext.timestampFormatter()).isEqualTo(TimestampFormatter.Prelude.DATE_TIME);
    }

    @Test
    public void nonTimestampMemberHasNullFormatter() {
        JsonSchemaExtensions.JsonSchemaExtension ext = STRUCT.member("name").getExtension(JsonSchemaExtensions.KEY);

        assertThat(ext).isNotNull();
        assertThat(ext.timestampFormatter()).isNull();
    }

    @Test
    public void structRootHasMemberLookup() {
        JsonSchemaExtensions.JsonSchemaExtension ext = STRUCT.getExtension(JsonSchemaExtensions.KEY);

        assertThat(ext).isNotNull();
        assertThat(ext.jsonMemberLookup()).isNotNull();
        assertThat(ext.jsonMemberLookup().member("name")).isEqualTo(STRUCT.member("name"));
        // Lookup by jsonName trait value
        assertThat(ext.jsonMemberLookup().member("renamed")).isEqualTo(STRUCT.member("renamedField"));
    }

    @Test
    public void unionRootHasMemberLookup() {
        JsonSchemaExtensions.JsonSchemaExtension ext = UNION.getExtension(JsonSchemaExtensions.KEY);

        assertThat(ext).isNotNull();
        assertThat(ext.jsonMemberLookup()).isNotNull();
        assertThat(ext.jsonMemberLookup().member("stringValue")).isEqualTo(UNION.member("stringValue"));
        assertThat(ext.jsonMemberLookup().member("intValue")).isEqualTo(UNION.member("intValue"));
    }

    @PerProvider
    public void roundtripWithJsonName(JsonSerdeProvider provider) {
        JsonCodec codec = codecBuilder(provider).useJsonName(true).useTimestampFormat(true).build();
        TestStruct pojo = new TestStruct("hello", 42, Instant.EPOCH);

        ByteBuffer serialized = codec.serialize(pojo);
        String json = StandardCharsets.UTF_8.decode(serialized.duplicate()).toString();

        assertThat(json).isEqualTo("{\"name\":\"hello\",\"renamed\":42,\"createdAt\":\"1970-01-01T00:00:00Z\"}");

        TestStruct deserialized = codec.deserializeShape(serialized, TestStruct.builder());
        assertThat(deserialized.name).isEqualTo("hello");
        assertThat(deserialized.renamedField).isEqualTo(42);
        assertThat(deserialized.createdAt).isEqualTo(Instant.EPOCH);
    }

    @PerProvider
    public void roundtripWithoutJsonName(JsonSerdeProvider provider) {
        JsonCodec codec = codecBuilder(provider).useTimestampFormat(true).build();
        TestStruct pojo = new TestStruct("hello", 42, Instant.EPOCH);

        ByteBuffer serialized = codec.serialize(pojo);
        String json = StandardCharsets.UTF_8.decode(serialized.duplicate()).toString();

        assertThat(json).isEqualTo("{\"name\":\"hello\",\"renamedField\":42,\"createdAt\":\"1970-01-01T00:00:00Z\"}");

        TestStruct deserialized = codec.deserializeShape(serialized, TestStruct.builder());
        assertThat(deserialized.name).isEqualTo("hello");
        assertThat(deserialized.renamedField).isEqualTo(42);
        assertThat(deserialized.createdAt).isEqualTo(Instant.EPOCH);
    }

    @Test
    public void recursiveSchemaExtensionsWork() {
        SchemaBuilder builder = Schema.structureBuilder(ShapeId.from("test#Recursive"));
        builder.putMember("value", PreludeSchemas.STRING);
        builder.putMember("next", builder);
        Schema schema = builder.build();

        // Trigger lazy resolution of deferred schema
        List<Schema> members = schema.members();
        assertThat(members).isNotEmpty();

        // Members should have extensions
        assertThat(schema.member("value").getExtension(JsonSchemaExtensions.KEY)).isNotNull();
        assertThat(schema.member("next").getExtension(JsonSchemaExtensions.KEY)).isNotNull();

        // Root should have member lookup after resolution
        JsonSchemaExtensions.JsonSchemaExtension ext = schema.getExtension(JsonSchemaExtensions.KEY);
        assertThat(ext).isNotNull();
        assertThat(ext.jsonMemberLookup()).isNotNull();
        assertThat(ext.jsonMemberLookup().member("value")).isNotNull();
        assertThat(ext.jsonMemberLookup().member("next")).isNotNull();
    }

    private static final class TestStruct implements SerializableStruct {
        final String name;
        final int renamedField;
        final Instant createdAt;

        TestStruct(String name, int renamedField, Instant createdAt) {
            this.name = name;
            this.renamedField = renamedField;
            this.createdAt = createdAt;
        }

        @Override
        public Schema sdkSchema() {
            return STRUCT;
        }

        @Override
        public void serialize(ShapeSerializer serializer) {
            serializer.writeStruct(STRUCT, this);
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeString(STRUCT.member("name"), name);
            serializer.writeInteger(STRUCT.member("renamedField"), renamedField);
            serializer.writeTimestamp(STRUCT.member("createdAt"), createdAt);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getMemberValue(Schema member) {
            switch (member.memberName()) {
                case "name":
                    return (T) name;
                case "renamedField":
                    return (T) Integer.valueOf(renamedField);
                case "createdAt":
                    return (T) createdAt;
                default:
                    return (T) null;
            }
        }

        static Builder builder() {
            return new Builder();
        }

        private static final class Builder implements ShapeBuilder<TestStruct> {
            String name;
            int renamedField;
            Instant createdAt;

            @Override
            public Schema sdkSchema() {
                return STRUCT;
            }

            @Override
            public Builder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(STRUCT, this, (state, member, deser) -> {
                    switch (member.memberName()) {
                        case "name":
                            state.name = deser.readString(member);
                            break;
                        case "renamedField":
                            state.renamedField = deser.readInteger(member);
                            break;
                        case "createdAt":
                            state.createdAt = deser.readTimestamp(member);
                            break;
                    }
                });
                return this;
            }

            @Override
            public TestStruct build() {
                return new TestStruct(name, renamedField, createdAt);
            }
        }
    }
}
