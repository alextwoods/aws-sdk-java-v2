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

package software.amazon.awssdk.protocols.client.rpcv2;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codecs.cbor.Rpcv2CborCodec;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.AwsQueryCompatibleTrait;
import software.amazon.awssdk.schema.traits.AwsQueryErrorTrait;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.HttpLabelTrait;
import software.amazon.awssdk.schema.traits.HttpQueryTrait;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.UnitTypeTrait;

/**
 * Hand-written shapes shaped like generated model classes, protocol settings and response helpers for the tests.
 */
final class TestShapes {
    static final String NS = "smithy.example";
    static final ShapeId SERVICE_ID = ShapeId.fromParts(NS, "ExampleService");
    static final Schema SERVICE_SCHEMA = Schema.createService(SERVICE_ID);
    static final Schema QUERY_COMPATIBLE_SERVICE_SCHEMA = Schema.createService(SERVICE_ID,
                                                                               AwsQueryCompatibleTrait.INSTANCE);
    static final ApiService SERVICE = () -> SERVICE_SCHEMA;
    static final Codec CODEC = Rpcv2CborCodec.builder().build();

    static final ProtocolSettings SETTINGS = ProtocolSettings.builder()
                                                             .serviceSchema(SERVICE_SCHEMA)
                                                             .baseExceptionBuilder(ExampleServiceException::builder)
                                                             .build();
    static final ProtocolSettings QUERY_COMPATIBLE_SETTINGS =
        SETTINGS.toBuilder().serviceSchema(QUERY_COMPATIBLE_SERVICE_SCHEMA).build();

    static final ErrorRegistry ERRORS = ErrorRegistry.builder()
                                                     .putLazyType(FooError.ID, () -> FooError.SCHEMA)
                                                     .putLazyType(QueueMissing.ID, () -> QueueMissing.SCHEMA)
                                                     .errorCode(QueueMissing.CODE, QueueMissing.ID)
                                                     .build();

    static final SimpleApiOperation<GreetRequest, GreetResponse> GREET =
        SimpleApiOperation.<GreetRequest, GreetResponse>builder()
                          .schema(Schema.createOperation(ShapeId.fromParts(NS, "Greet")))
                          .inputSchema(() -> GreetRequest.SCHEMA)
                          .outputSchema(() -> GreetResponse.SCHEMA)
                          .errorRegistry(ERRORS)
                          .service(SERVICE)
                          .build();

    static final SimpleApiOperation<NoInputRequest, GreetResponse> NO_INPUT =
        SimpleApiOperation.<NoInputRequest, GreetResponse>builder()
                          .schema(Schema.createOperation(ShapeId.fromParts(NS, "NoInput")))
                          .inputSchema(() -> NoInputRequest.SCHEMA)
                          .outputSchema(() -> GreetResponse.SCHEMA)
                          .errorRegistry(ERRORS)
                          .service(SERVICE)
                          .build();

    private TestShapes() {
    }

    /**
     * Serializes a map of string members, for example {@code "__type" -> "FooError"}, as a CBOR map.
     */
    static byte[] cbor(String... keysAndValues) {
        Map<String, Document> members = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            members.put(keysAndValues[i], Document.of(keysAndValues[i + 1]));
        }
        return bytes(CODEC.serialize(Document.of(members)));
    }

    static byte[] bytes(ByteBuffer buffer) {
        ByteBuffer copy = buffer.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        return bytes;
    }

    static SdkHttpFullResponse.Builder response(int status, byte[] body) {
        SdkHttpFullResponse.Builder builder = SdkHttpFullResponse.builder().statusCode(status);
        if (body != null) {
            builder.content(AbortableInputStream.create(new ByteArrayInputStream(body)))
                   .putHeader("Content-Length", String.valueOf(body.length));
        }
        return builder;
    }

    /**
     * The base exception of the example service, like a generated {@code <Svc>Exception}.
     */
    static class ExampleServiceException extends AwsServiceException {
        ExampleServiceException(BuilderImpl builder) {
            super(builder);
        }

        public static Builder builder() {
            return new BuilderImpl();
        }

        static class BuilderImpl extends AwsServiceException.BuilderImpl {
            @Override
            public ExampleServiceException build() {
                return new ExampleServiceException(this);
            }
        }
    }

    /**
     * A modeled error with a {@code message} member and a {@code reason} member.
     */
    static final class FooError extends ExampleServiceException implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "FooError");
        static final Schema SCHEMA = Schema.structureBuilder(ID, new ErrorTrait("client"))
                                           .putMember("message", PreludeSchemas.STRING)
                                           .putMember("reason", PreludeSchemas.STRING)
                                           .builderSupplier(FooBuilder::new)
                                           .shapeClass(FooError.class)
                                           .build();

        private final String reason;

        private FooError(FooBuilder builder) {
            super(builder);
            this.reason = builder.reason;
        }

        String reason() {
            return reason;
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (reason != null) {
                serializer.writeString(SCHEMA.member("reason"), reason);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) reason;
        }

        static final class FooBuilder extends ExampleServiceException.BuilderImpl implements ShapeBuilder<FooError> {
            private String reason;

            @Override
            public FooError build() {
                return new FooError(this);
            }

            @Override
            public FooBuilder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (builder, member, d) -> {
                    if ("message".equals(member.memberName())) {
                        builder.message(d.readString(member));
                    } else {
                        builder.reason = d.readString(member);
                    }
                });
                return this;
            }

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }
        }
    }

    /**
     * A modeled error with an {@code awsQueryError} custom code, like SQS {@code QueueDoesNotExist}.
     */
    static final class QueueMissing extends ExampleServiceException implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "QueueMissing");
        static final String CODE = "AWS.SimpleQueueService.NonExistentQueue";
        static final Schema SCHEMA = Schema.structureBuilder(ID, new ErrorTrait("client"),
                                                             new AwsQueryErrorTrait(CODE, 400))
                                           .builderSupplier(QueueMissingBuilder::new)
                                           .shapeClass(QueueMissing.class)
                                           .build();

        private QueueMissing(QueueMissingBuilder builder) {
            super(builder);
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            throw new IllegalArgumentException(member.toString());
        }

        static final class QueueMissingBuilder extends ExampleServiceException.BuilderImpl
            implements ShapeBuilder<QueueMissing> {
            @Override
            public QueueMissing build() {
                return new QueueMissing(this);
            }

            @Override
            public QueueMissingBuilder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (builder, member, d) -> d.readDocument());
                return this;
            }

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }
        }
    }

    /**
     * An operation input whose {@code name} member carries HTTP binding traits and {@code jsonName}, which rpcv2Cbor
     * ignores.
     */
    static final class GreetRequest implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "GreetRequest");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("message", PreludeSchemas.STRING)
                                           .putMember("name", PreludeSchemas.STRING, new HttpHeaderTrait("X-Name"),
                                                      new HttpQueryTrait("name"), HttpLabelTrait.INSTANCE,
                                                      new JsonNameTrait("Alias"))
                                           .builderSupplier(GreetRequestBuilder::new)
                                           .shapeClass(GreetRequest.class)
                                           .build();

        private final String message;
        private final String name;

        GreetRequest(String message, String name) {
            this.message = message;
            this.name = name;
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (message != null) {
                serializer.writeString(SCHEMA.member("message"), message);
            }
            if (name != null) {
                serializer.writeString(SCHEMA.member("name"), name);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) ("message".equals(member.memberName()) ? message : name);
        }
    }

    static final class GreetRequestBuilder implements ShapeBuilder<GreetRequest> {
        private String message;
        private String name;

        @Override
        public GreetRequest build() {
            return new GreetRequest(message, name);
        }

        @Override
        public GreetRequestBuilder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(GreetRequest.SCHEMA, this, (builder, member, d) -> {
                if ("message".equals(member.memberName())) {
                    builder.message = d.readString(member);
                } else {
                    builder.name = d.readString(member);
                }
            });
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return GreetRequest.SCHEMA;
        }
    }

    /**
     * The input of an operation whose model input is {@code smithy.api#Unit}. Generated clients still have a request
     * class for it, whose schema carries the {@code unitType} trait.
     */
    static final class NoInputRequest implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "NoInputRequest");
        static final Schema SCHEMA = Schema.structureBuilder(ID, UnitTypeTrait.INSTANCE)
                                           .builderSupplier(NoInputRequestBuilder::new)
                                           .shapeClass(NoInputRequest.class)
                                           .build();

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            throw new IllegalArgumentException(member.toString());
        }
    }

    static final class NoInputRequestBuilder implements ShapeBuilder<NoInputRequest> {
        @Override
        public NoInputRequest build() {
            return new NoInputRequest();
        }

        @Override
        public NoInputRequestBuilder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(NoInputRequest.SCHEMA, this, (builder, member, d) -> d.readDocument());
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return NoInputRequest.SCHEMA;
        }
    }

    /**
     * An operation output shaped like a generated response class: an {@link AwsResponse} whose builder is also a
     * {@link ShapeBuilder}. Its {@code count} member is bound to a header, which rpcv2Cbor ignores.
     */
    static final class GreetResponse extends AwsResponse implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "GreetResponse");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("greeting", PreludeSchemas.STRING)
                                           .putMember("count", PreludeSchemas.STRING, new HttpHeaderTrait("X-Count"))
                                           .builderSupplier(GreetResponseBuilder::new)
                                           .shapeClass(GreetResponse.class)
                                           .build();

        private final String greeting;
        private final String count;

        private GreetResponse(GreetResponseBuilder builder) {
            super(builder);
            this.greeting = builder.greeting;
            this.count = builder.count;
        }

        String greeting() {
            return greeting;
        }

        String count() {
            return count;
        }

        @Override
        public GreetResponseBuilder toBuilder() {
            throw new UnsupportedOperationException("Outputs are built once");
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            if (greeting != null) {
                serializer.writeString(SCHEMA.member("greeting"), greeting);
            }
            if (count != null) {
                serializer.writeString(SCHEMA.member("count"), count);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) ("greeting".equals(member.memberName()) ? greeting : count);
        }

        static final class GreetResponseBuilder extends AwsResponse.BuilderImpl implements ShapeBuilder<GreetResponse> {
            private String greeting;
            private String count;

            @Override
            public GreetResponse build() {
                return new GreetResponse(this);
            }

            @Override
            public GreetResponseBuilder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (builder, member, d) -> {
                    if ("greeting".equals(member.memberName())) {
                        builder.greeting = d.readString(member);
                    } else {
                        builder.count = d.readString(member);
                    }
                });
                return this;
            }

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }
        }
    }
}
