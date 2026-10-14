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

package software.amazon.awssdk.protocols.client.http;

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
import software.amazon.awssdk.schema.traits.AwsQueryErrorTrait;
import software.amazon.awssdk.schema.traits.ErrorTrait;

/**
 * Hand-written shapes shaped like generated model classes, a CBOR codec and response helpers for the tests.
 */
final class TestShapes {
    static final String NS = "smithy.example";
    static final ShapeId SERVICE_ID = ShapeId.fromParts(NS, "ExampleService");
    static final Schema SERVICE_SCHEMA = Schema.createService(SERVICE_ID);
    static final ApiService SERVICE = () -> SERVICE_SCHEMA;
    static final Codec CODEC = Rpcv2CborCodec.builder().build();

    static final ErrorRegistry ERRORS = ErrorRegistry.builder()
                                                     .putLazyType(FooError.ID, () -> FooError.SCHEMA)
                                                     .putLazyType(QueueMissing.ID, () -> QueueMissing.SCHEMA)
                                                     .putLazyType(Greeting.ID, () -> Greeting.SCHEMA)
                                                     .errorCode(QueueMissing.CODE, QueueMissing.ID)
                                                     .build();

    static final SimpleApiOperation<Greeting, Greeting> GREET = operation("Greet");

    private TestShapes() {
    }

    static SimpleApiOperation<Greeting, Greeting> operation(String name) {
        return SimpleApiOperation.<Greeting, Greeting>builder()
                                 .schema(Schema.createOperation(ShapeId.fromParts(NS, name)))
                                 .inputSchema(() -> Greeting.SCHEMA)
                                 .outputSchema(() -> Greeting.SCHEMA)
                                 .errorRegistry(ERRORS)
                                 .service(SERVICE)
                                 .build();
    }

    /**
     * Serializes a map of members, for example {@code "__type" -> "FooError"}, as a CBOR map.
     */
    static byte[] cbor(Object... keysAndValues) {
        Map<String, Document> members = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            Object value = keysAndValues[i + 1];
            members.put((String) keysAndValues[i],
                        value instanceof Integer ? Document.of((Integer) value) : Document.of((String) value));
        }
        ByteBuffer buffer = CODEC.serialize(Document.of(members));
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
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
                                           .builderSupplier(FooError::builder)
                                           .shapeClass(FooError.class)
                                           .build();

        private final String reason;

        private FooError(FooBuilder builder) {
            super(builder);
            this.reason = builder.reason;
        }

        public static FooBuilder builder() {
            return new FooBuilder(SCHEMA);
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
    }

    static final class FooBuilder extends ExampleServiceException.BuilderImpl implements ShapeBuilder<FooError> {
        private final Schema schema;
        private String reason;

        FooBuilder(Schema schema) {
            this.schema = schema;
        }

        @Override
        public FooError build() {
            return new FooError(this);
        }

        @Override
        public FooBuilder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(schema, this, (builder, member, d) -> {
                if ("message".equals(member.memberName())) {
                    builder.message(d.readString(member));
                } else {
                    builder.reason = d.readString(member);
                }
            });
            return this;
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            if (!"reason".equals(member.memberName())) {
                ShapeBuilder.super.setMemberValue(member, value);
            }
            reason = (String) value;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
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
                                           .builderSupplier(QueueMissing::builder)
                                           .shapeClass(QueueMissing.class)
                                           .build();

        private QueueMissing(QueueMissingBuilder builder) {
            super(builder);
        }

        public static QueueMissingBuilder builder() {
            return new QueueMissingBuilder();
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
    }

    static final class QueueMissingBuilder extends ExampleServiceException.BuilderImpl
        implements ShapeBuilder<QueueMissing> {
        @Override
        public QueueMissing build() {
            return new QueueMissing(this);
        }

        @Override
        public QueueMissingBuilder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(QueueMissing.SCHEMA, this, (builder, member, d) -> d.readDocument());
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return QueueMissing.SCHEMA;
        }
    }

    /**
     * A structure with a {@code message} member, used as operation input and output, and registered as an "error"
     * whose builder does not build an exception.
     */
    static final class Greeting implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "Greeting");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("message", PreludeSchemas.STRING)
                                           .builderSupplier(GreetingBuilder::new)
                                           .shapeClass(Greeting.class)
                                           .build();

        private final String message;

        Greeting(String message) {
            this.message = message;
        }

        String message() {
            return message;
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
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) message;
        }
    }

    /**
     * An operation output shaped like a generated response class: an {@link AwsResponse} whose builder is also a
     * {@link ShapeBuilder}.
     */
    static final class GreetingResponse extends AwsResponse implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "GreetingResponse");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("message", PreludeSchemas.STRING)
                                           .builderSupplier(GreetingResponseBuilder::new)
                                           .shapeClass(GreetingResponse.class)
                                           .build();

        private final String message;

        private GreetingResponse(GreetingResponseBuilder builder) {
            super(builder);
            this.message = builder.message;
        }

        String message() {
            return message;
        }

        @Override
        public GreetingResponseBuilder toBuilder() {
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
            if (message != null) {
                serializer.writeString(SCHEMA.member("message"), message);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) message;
        }

        static final class GreetingResponseBuilder extends AwsResponse.BuilderImpl
            implements ShapeBuilder<GreetingResponse> {
            private String message;

            @Override
            public GreetingResponse build() {
                return new GreetingResponse(this);
            }

            @Override
            public GreetingResponseBuilder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(GreetingResponse.SCHEMA, this,
                                   (builder, member, d) -> builder.message = d.readString(member));
                return this;
            }

            @Override
            public Schema sdkSchema() {
                return GreetingResponse.SCHEMA;
            }
        }
    }

    static final class GreetingBuilder implements ShapeBuilder<Greeting> {
        private String message;

        @Override
        public Greeting build() {
            return new Greeting(message);
        }

        @Override
        public GreetingBuilder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(Greeting.SCHEMA, this, (builder, member, d) -> builder.message = d.readString(member));
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return Greeting.SCHEMA;
        }
    }
}
