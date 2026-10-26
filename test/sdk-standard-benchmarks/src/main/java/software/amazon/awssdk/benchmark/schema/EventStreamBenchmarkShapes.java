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

package software.amazon.awssdk.benchmark.schema;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
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
import software.amazon.awssdk.schema.traits.EventHeaderTrait;
import software.amazon.awssdk.schema.traits.EventPayloadTrait;
import software.amazon.awssdk.schema.traits.StreamingTrait;

/**
 * The event stream model of smithy-java's {@code aws-event-streams} tests and its {@code EventEncodeBenchmark} and
 * {@code EventDecodeBenchmark} (smithy-java {@code 8e892b96c}), with map-backed structures standing in for generated
 * classes. The {@code Struct} and {@code Builder} helpers are copied from {@code EventStreamTestShapes} in the
 * {@code http-client-protocol} tests, which are not on this module's classpath.
 */
final class EventStreamBenchmarkShapes {
    private static final String NS = "smithy.example.eventstreaming";

    static final Schema BLOB_EVENT = Schema.structureBuilder(ShapeId.fromParts(NS, "BlobEvent"))
                                           .putMember("payload", PreludeSchemas.BLOB, EventPayloadTrait.INSTANCE)
                                           .builderSupplier(() -> new Builder(EventStreamBenchmarkShapes.BLOB_EVENT))
                                           .build();

    static final Schema BODY_AND_HEADER_EVENT =
        Schema.structureBuilder(ShapeId.fromParts(NS, "BodyAndHeaderEvent"))
              .putMember("intMember", PreludeSchemas.INTEGER, EventHeaderTrait.INSTANCE)
              .putMember("stringMember", PreludeSchemas.STRING)
              .builderSupplier(() -> new Builder(EventStreamBenchmarkShapes.BODY_AND_HEADER_EVENT))
              .build();

    static final Schema HEADERS_ONLY_EVENT =
        Schema.structureBuilder(ShapeId.fromParts(NS, "HeadersOnlyEvent"))
              .putMember("sequenceNum", PreludeSchemas.INTEGER, EventHeaderTrait.INSTANCE)
              .builderSupplier(() -> new Builder(EventStreamBenchmarkShapes.HEADERS_ONLY_EVENT))
              .build();

    static final Schema STRING_EVENT = Schema.structureBuilder(ShapeId.fromParts(NS, "StringEvent"))
                                             .putMember("payload", PreludeSchemas.STRING, EventPayloadTrait.INSTANCE)
                                             .builderSupplier(() -> new Builder(
                                                 EventStreamBenchmarkShapes.STRING_EVENT))
                                             .build();

    static final Schema STRUCTURE_EVENT = Schema.structureBuilder(ShapeId.fromParts(NS, "StructureEvent"))
                                                .putMember("foo", PreludeSchemas.STRING)
                                                .builderSupplier(() -> new Builder(
                                                    EventStreamBenchmarkShapes.STRUCTURE_EVENT))
                                                .build();

    static final Schema TEST_EVENT_STREAM =
        Schema.unionBuilder(ShapeId.fromParts(NS, "TestEventStream"), StreamingTrait.INSTANCE)
              .putMember("structureMember", STRUCTURE_EVENT)
              .putMember("stringMember", STRING_EVENT)
              .putMember("blobMember", BLOB_EVENT)
              .putMember("headersOnlyMember", HEADERS_ONLY_EVENT)
              .putMember("bodyAndHeaderMember", BODY_AND_HEADER_EVENT)
              .build();

    static final Schema TEST_OPERATION_INPUT =
        Schema.structureBuilder(ShapeId.fromParts(NS, "TestOperationInput"))
              .putMember("headerString", PreludeSchemas.STRING, EventHeaderTrait.INSTANCE)
              .putMember("inputStringMember", PreludeSchemas.STRING)
              .putMember("stream", TEST_EVENT_STREAM)
              .builderSupplier(() -> new Builder(EventStreamBenchmarkShapes.TEST_OPERATION_INPUT))
              .build();

    static final Schema TEST_OPERATION_OUTPUT =
        Schema.structureBuilder(ShapeId.fromParts(NS, "TestOperationOutput"))
              .putMember("intMemberHeader", PreludeSchemas.INTEGER, EventHeaderTrait.INSTANCE)
              .putMember("stringMember", PreludeSchemas.STRING)
              .putMember("outputStream", TEST_EVENT_STREAM)
              .builderSupplier(() -> new Builder(EventStreamBenchmarkShapes.TEST_OPERATION_OUTPUT))
              .build();

    private static final ApiService SERVICE =
        () -> Schema.createService(ShapeId.fromParts(NS, "EventStreamingTestService"));

    static final SimpleApiOperation<Struct, Struct> OPERATION =
        SimpleApiOperation.<Struct, Struct>builder()
                          .schema(Schema.createOperation(ShapeId.fromParts(NS, "TestOperation")))
                          .inputSchema(() -> TEST_OPERATION_INPUT)
                          .outputSchema(() -> TEST_OPERATION_OUTPUT)
                          .errorRegistry(TypeRegistry.empty())
                          .service(SERVICE)
                          .build();

    private EventStreamBenchmarkShapes() {
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

        @Override
        public Schema sdkSchema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
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
                    case BLOB:
                        serializer.writeBlob(member, ByteBuffer.wrap((byte[]) value));
                        break;
                    default:
                        throw new IllegalArgumentException(member.toString());
                }
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) values.get(member.memberName());
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

        private static Object readMember(Schema member, ShapeDeserializer d) {
            switch (member.type()) {
                case STRING:
                    return d.readString(member);
                case INTEGER:
                    return d.readInteger(member);
                case BLOB:
                    ByteBuffer buffer = d.readBlob(member);
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.duplicate().get(bytes);
                    return bytes;
                default:
                    return d.readDocument();
            }
        }
    }
}
