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

package software.amazon.awssdk.protocols.client.http.eventstream;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.EventHeaderTrait;
import software.amazon.awssdk.schema.traits.EventPayloadTrait;
import software.amazon.awssdk.schema.traits.StreamingTrait;

/**
 * An event stream model shaped like smithy-java's {@code aws-event-streams} test model, with map-backed structures
 * standing in for generated classes.
 */
final class EventStreamTestShapes {
    static final String NS = "smithy.example";

    static final Schema STRUCTURE = Schema.structureBuilder(ShapeId.fromParts(NS, "Nested"))
                                          .putMember("value", PreludeSchemas.STRING)
                                          .builderSupplier(() -> new Builder(EventStreamTestShapes.STRUCTURE))
                                          .build();

    static final Schema BODY_AND_HEADER_EVENT =
        Schema.structureBuilder(ShapeId.fromParts(NS, "BodyAndHeaderEvent"))
              .putMember("stringHeader", PreludeSchemas.STRING, EventHeaderTrait.INSTANCE)
              .putMember("intHeader", PreludeSchemas.INTEGER, EventHeaderTrait.INSTANCE)
              .putMember("longHeader", PreludeSchemas.LONG, EventHeaderTrait.INSTANCE)
              .putMember("booleanHeader", PreludeSchemas.BOOLEAN, EventHeaderTrait.INSTANCE)
              .putMember("timestampHeader", PreludeSchemas.TIMESTAMP, EventHeaderTrait.INSTANCE)
              .putMember("blobHeader", PreludeSchemas.BLOB, EventHeaderTrait.INSTANCE)
              .putMember("message", PreludeSchemas.STRING)
              .putMember("count", PreludeSchemas.INTEGER)
              .builderSupplier(() -> new Builder(EventStreamTestShapes.BODY_AND_HEADER_EVENT))
              .build();

    static final Schema HEADERS_ONLY_EVENT = Schema.structureBuilder(ShapeId.fromParts(NS, "HeadersOnlyEvent"))
                                                   .putMember("sequence", PreludeSchemas.LONG,
                                                              EventHeaderTrait.INSTANCE)
                                                   .builderSupplier(() -> new Builder(
                                                       EventStreamTestShapes.HEADERS_ONLY_EVENT))
                                                   .build();

    static final Schema BLOB_EVENT = Schema.structureBuilder(ShapeId.fromParts(NS, "BlobEvent"))
                                           .putMember("id", PreludeSchemas.STRING, EventHeaderTrait.INSTANCE)
                                           .putMember("data", PreludeSchemas.BLOB, EventPayloadTrait.INSTANCE)
                                           .builderSupplier(() -> new Builder(EventStreamTestShapes.BLOB_EVENT))
                                           .build();

    static final Schema STRING_EVENT = Schema.structureBuilder(ShapeId.fromParts(NS, "StringEvent"))
                                             .putMember("text", PreludeSchemas.STRING, EventPayloadTrait.INSTANCE)
                                             .builderSupplier(() -> new Builder(EventStreamTestShapes.STRING_EVENT))
                                             .build();

    static final Schema STRUCTURE_EVENT = Schema.structureBuilder(ShapeId.fromParts(NS, "StructureEvent"))
                                                .putMember("nested", STRUCTURE, EventPayloadTrait.INSTANCE)
                                                .builderSupplier(() -> new Builder(
                                                    EventStreamTestShapes.STRUCTURE_EVENT))
                                                .build();

    static final Schema MY_ERROR = Schema.structureBuilder(ShapeId.fromParts(NS, "MyError"), new ErrorTrait("client"))
                                         .putMember("message", PreludeSchemas.STRING)
                                         .builderSupplier(MyErrorBuilder::new)
                                         .build();

    static final Schema EVENT_STREAM =
        Schema.unionBuilder(ShapeId.fromParts(NS, "TestEventStream"), StreamingTrait.INSTANCE)
              .putMember("bodyAndHeader", BODY_AND_HEADER_EVENT)
              .putMember("secondBodyAndHeader", BODY_AND_HEADER_EVENT)
              .putMember("headersOnly", HEADERS_ONLY_EVENT)
              .putMember("blob", BLOB_EVENT)
              .putMember("string", STRING_EVENT)
              .putMember("structure", STRUCTURE_EVENT)
              .putMember("myError", MY_ERROR, new ErrorTrait("client"))
              .build();

    static final Schema INPUT = Schema.structureBuilder(ShapeId.fromParts(NS, "TestOperationInput"))
                                      .putMember("inputHeader", PreludeSchemas.STRING)
                                      .putMember("stream", EVENT_STREAM)
                                      .builderSupplier(() -> new Builder(EventStreamTestShapes.INPUT))
                                      .build();

    static final Schema OUTPUT = Schema.structureBuilder(ShapeId.fromParts(NS, "TestOperationOutput"))
                                       .putMember("outputMember", PreludeSchemas.STRING)
                                       .putMember("stream", EVENT_STREAM)
                                       .builderSupplier(() -> new Builder(EventStreamTestShapes.OUTPUT))
                                       .build();

    static final ApiService SERVICE = () -> Schema.createService(ShapeId.fromParts(NS, "EventService"));

    static final SimpleApiOperation<Struct, Struct> OPERATION =
        SimpleApiOperation.<Struct, Struct>builder()
                          .schema(Schema.createOperation(ShapeId.fromParts(NS, "TestOperation")))
                          .inputSchema(() -> INPUT)
                          .outputSchema(() -> OUTPUT)
                          .errorRegistry(TypeRegistry.empty())
                          .service(SERVICE)
                          .build();

    static final SimpleApiOperation<Struct, Struct> OPERATION_WITHOUT_STREAM =
        SimpleApiOperation.<Struct, Struct>builder()
                          .schema(Schema.createOperation(ShapeId.fromParts(NS, "PlainOperation")))
                          .inputSchema(() -> STRUCTURE)
                          .outputSchema(() -> STRUCTURE)
                          .errorRegistry(TypeRegistry.empty())
                          .service(SERVICE)
                          .build();

    private EventStreamTestShapes() {
    }

    static Struct struct(Schema schema, Object... namesAndValues) {
        Builder builder = new Builder(schema);
        for (int i = 0; i < namesAndValues.length; i += 2) {
            builder.values.put((String) namesAndValues[i], namesAndValues[i + 1]);
        }
        return builder.build();
    }

    /**
     * A structure whose member values are held in a map by member name.
     */
    static final class Struct implements SerializableStruct {
        private final Schema schema;
        private final Map<String, Object> values;

        Struct(Schema schema, Map<String, Object> values) {
            this.schema = schema;
            this.values = values;
        }

        Object get(String member) {
            return values.get(member);
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            writeMembers(schema, values, serializer);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) values.get(member.memberName());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Struct)) {
                return false;
            }
            Struct other = (Struct) o;
            return schema.id().equals(other.schema.id()) && values.equals(other.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(schema.id(), values);
        }

        @Override
        public String toString() {
            return schema.id().getName() + values;
        }
    }

    static void writeMembers(Schema schema, Map<String, Object> values, ShapeSerializer serializer) {
        for (Schema member : schema.members()) {
            Object value = values.get(member.memberName());
            if (value == null) {
                continue;
            }
            switch (member.type()) {
                case STRING:
                    serializer.writeString(member, (String) value);
                    break;
                case INTEGER:
                    serializer.writeInteger(member, (Integer) value);
                    break;
                case LONG:
                    serializer.writeLong(member, (Long) value);
                    break;
                case BOOLEAN:
                    serializer.writeBoolean(member, (Boolean) value);
                    break;
                case TIMESTAMP:
                    serializer.writeTimestamp(member, (Instant) value);
                    break;
                case BLOB:
                    serializer.writeBlob(member, ByteBuffer.wrap((byte[]) value));
                    break;
                case STRUCTURE:
                    serializer.writeStruct(member, (Struct) value);
                    break;
                default:
                    throw new IllegalArgumentException(member.toString());
            }
        }
    }

    static Object readMember(Schema member, ShapeDeserializer d) {
        switch (member.type()) {
            case STRING:
                return d.readString(member);
            case INTEGER:
                return d.readInteger(member);
            case LONG:
                return d.readLong(member);
            case BOOLEAN:
                return d.readBoolean(member);
            case TIMESTAMP:
                return d.readTimestamp(member);
            case BLOB:
                ByteBuffer buffer = d.readBlob(member);
                byte[] bytes = new byte[buffer.remaining()];
                buffer.duplicate().get(bytes);
                return bytes;
            case STRUCTURE:
                return new Builder(member.memberTarget()).deserialize(d).build();
            default:
                return d.readDocument();
        }
    }

    static final class Builder implements ShapeBuilder<Struct> {
        private final Schema schema;
        private final Map<String, Object> values = new LinkedHashMap<>();

        Builder(Schema schema) {
            this.schema = schema;
        }

        @Override
        public Struct build() {
            return new Struct(schema, new LinkedHashMap<>(values));
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(schema, this, (b, member, d) -> b.values.put(member.memberName(), readMember(member, d)));
            return this;
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            values.put(member.memberName(), value);
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }
    }

    /**
     * A modeled exception, like a generated exception class.
     */
    static final class MyError extends RuntimeException implements SerializableStruct {
        private static final long serialVersionUID = 1L;

        MyError(String message) {
            super(message);
        }

        @Override
        public Schema sdkSchema() {
            return MY_ERROR;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (getMessage() != null) {
                serializer.writeString(MY_ERROR.member("message"), getMessage());
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) getMessage();
        }
    }

    static final class MyErrorBuilder implements ShapeBuilder<MyError> {
        private String message;

        @Override
        public MyError build() {
            return new MyError(message);
        }

        @Override
        public MyErrorBuilder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(MY_ERROR, this, (b, member, d) -> b.message = d.readString(member));
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return MY_ERROR;
        }
    }
}
