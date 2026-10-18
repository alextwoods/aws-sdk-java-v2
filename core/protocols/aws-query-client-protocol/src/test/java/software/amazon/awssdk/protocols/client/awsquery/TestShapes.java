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

package software.amazon.awssdk.protocols.client.awsquery;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
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
import software.amazon.awssdk.schema.traits.AwsQueryErrorTrait;
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.HttpQueryTrait;
import software.amazon.awssdk.schema.traits.UnitTypeTrait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;

/**
 * Hand-written shapes shaped like generated model classes, protocol settings and response helpers for the tests.
 */
final class TestShapes {
    static final String NS = "smithy.example";
    static final String VERSION = "2024-01-01";
    static final ShapeId SERVICE_ID = ShapeId.fromParts(NS, "ExampleService");
    static final Schema SERVICE_SCHEMA = Schema.createService(SERVICE_ID);
    static final ApiService SERVICE = () -> SERVICE_SCHEMA;

    static final ProtocolSettings SETTINGS = ProtocolSettings.builder()
                                                             .serviceSchema(SERVICE_SCHEMA)
                                                             .serviceVersion(VERSION)
                                                             .baseExceptionBuilder(ExampleServiceException::builder)
                                                             .build();

    /**
     * The registry of the example service. {@code ResourceNotFound} is both the shape name of {@link NotFound} and the
     * {@code awsQueryError} code of {@link DashboardMissing}, as in CloudWatch: the code index wins.
     */
    static final ErrorRegistry ERRORS = ErrorRegistry.builder()
                                                     .putLazyType(FooError.ID, () -> FooError.SCHEMA)
                                                     .putLazyType(QueueMissing.ID, () -> QueueMissing.SCHEMA)
                                                     .putLazyType(DashboardMissing.ID, () -> DashboardMissing.SCHEMA)
                                                     .putLazyType(NotFound.ID, () -> NotFound.SCHEMA)
                                                     .errorCode(QueueMissing.CODE, QueueMissing.ID)
                                                     .errorCode(DashboardMissing.CODE, DashboardMissing.ID)
                                                     .build();

    static final SimpleApiOperation<GreetRequest, GreetResponse> GREET = operation("Greet", GreetRequest.SCHEMA,
                                                                                    GreetResponse.SCHEMA);

    static final SimpleApiOperation<NoInputRequest, GreetResponse> NO_INPUT =
        operation("NoInput", NoInputRequest.SCHEMA, GreetResponse.SCHEMA);

    /**
     * An operation without input or output members, like a generated operation whose input and output are
     * {@code smithy.api#Unit}.
     */
    static final SimpleApiOperation<NoInputRequest, GreetResponse> NO_OUTPUT = operation("NoOutput",
                                                                                         NoInputRequest.SCHEMA,
                                                                                         EmptyResponse.SCHEMA);

    private TestShapes() {
    }

    @SuppressWarnings("unchecked")
    private static <I extends SerializableStruct, O extends SerializableStruct> SimpleApiOperation<I, O> operation(
        String name, Schema input, Schema output) {
        return SimpleApiOperation.<I, O>builder()
                                 .schema(Schema.createOperation(ShapeId.fromParts(NS, name)))
                                 .inputSchema(() -> input)
                                 .outputSchema(() -> output)
                                 .errorRegistry(ERRORS)
                                 .service(SERVICE)
                                 .build();
    }

    static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    static SdkHttpFullResponse.Builder response(int status, String body) {
        SdkHttpFullResponse.Builder builder = SdkHttpFullResponse.builder().statusCode(status);
        if (body != null) {
            byte[] bytes = utf8(body);
            builder.content(AbortableInputStream.create(new ByteArrayInputStream(bytes)))
                   .putHeader("Content-Length", String.valueOf(bytes.length));
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
     * A modeled error with a {@code Reason} member. Like the generated query exceptions, it has no message member: the
     * message is the error's {@code <Message>}.
     */
    static final class FooError extends ExampleServiceException implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "FooError");
        static final Schema SCHEMA = Schema.structureBuilder(ID, new ErrorTrait("client"))
                                           .putMember("Reason", PreludeSchemas.STRING)
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
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            throw new UnsupportedOperationException();
        }

        static final class FooBuilder extends ExampleServiceException.BuilderImpl implements ShapeBuilder<FooError> {
            private String reason;

            @Override
            public FooError build() {
                return new FooError(this);
            }

            @Override
            public FooBuilder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (builder, member, d) -> builder.reason = d.readString(member));
                return this;
            }

            @Override
            public Schema sdkSchema() {
                return SCHEMA;
            }
        }
    }

    /**
     * A modeled error without members, created from its schema's builder supplier.
     */
    abstract static class MemberlessError extends ExampleServiceException implements SerializableStruct {
        MemberlessError(BuilderImpl builder) {
            super(builder);
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            throw new IllegalArgumentException(member.toString());
        }
    }

    /**
     * A modeled error with an {@code awsQueryError} custom code, like SQS {@code QueueDoesNotExist}.
     */
    static final class QueueMissing extends MemberlessError {
        static final ShapeId ID = ShapeId.fromParts(NS, "QueueMissing");
        static final String CODE = "AWS.SimpleQueueService.NonExistentQueue";
        static final Schema SCHEMA = Schema.structureBuilder(ID, new ErrorTrait("client"),
                                                             new AwsQueryErrorTrait(CODE, 400))
                                           .builderSupplier(() -> new MemberlessBuilder<>(
                                               TestShapes.QueueMissing.SCHEMA, QueueMissing::new))
                                           .shapeClass(QueueMissing.class)
                                           .build();

        private QueueMissing(BuilderImpl builder) {
            super(builder);
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }
    }

    /**
     * Like CloudWatch's {@code DashboardNotFoundError}: {@code awsQueryError} code {@code ResourceNotFound}.
     */
    static final class DashboardMissing extends MemberlessError {
        static final ShapeId ID = ShapeId.fromParts(NS, "DashboardMissing");
        static final String CODE = "ResourceNotFound";
        static final Schema SCHEMA = Schema.structureBuilder(ID, new ErrorTrait("client"),
                                                             new AwsQueryErrorTrait(CODE, 404))
                                           .builderSupplier(() -> new MemberlessBuilder<>(
                                               TestShapes.DashboardMissing.SCHEMA, DashboardMissing::new))
                                           .shapeClass(DashboardMissing.class)
                                           .build();

        private DashboardMissing(BuilderImpl builder) {
            super(builder);
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }
    }

    /**
     * Like CloudWatch's {@code ResourceNotFound} shape, whose name is another error's custom code.
     */
    static final class NotFound extends MemberlessError {
        static final ShapeId ID = ShapeId.fromParts(NS, "ResourceNotFound");
        static final Schema SCHEMA = Schema.structureBuilder(ID, new ErrorTrait("client"))
                                           .builderSupplier(() -> new MemberlessBuilder<>(TestShapes.NotFound.SCHEMA,
                                                                                          NotFound::new))
                                           .shapeClass(NotFound.class)
                                           .build();

        private NotFound(BuilderImpl builder) {
            super(builder);
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }
    }

    static final class MemberlessBuilder<E extends MemberlessError> extends ExampleServiceException.BuilderImpl
        implements ShapeBuilder<E> {
        private final Schema schema;
        private final Function<MemberlessBuilder<E>, E> constructor;

        MemberlessBuilder(Schema schema, Function<MemberlessBuilder<E>, E> constructor) {
            this.schema = schema;
            this.constructor = constructor;
        }

        @Override
        public E build() {
            return constructor.apply(this);
        }

        @Override
        public MemberlessBuilder<E> deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(schema, this, (builder, member, d) -> d.readString(member));
            return this;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }
    }

    /**
     * An operation input with a member renamed by {@code xmlName} and one with HTTP binding traits, which the query
     * protocols ignore.
     */
    static final class GreetRequest implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "GreetRequest");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("message", PreludeSchemas.STRING, new XmlNameTrait("Text"))
                                           .putMember("name", PreludeSchemas.STRING, new HttpHeaderTrait("X-Name"),
                                                      new HttpQueryTrait("name"))
                                           .builderSupplier(() -> {
                                               throw new UnsupportedOperationException();
                                           })
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

    /**
     * The input of an operation whose model input is {@code smithy.api#Unit}. Generated clients still have a request
     * class for it, whose schema carries the {@code unitType} trait.
     */
    static final class NoInputRequest implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "NoInputRequest");
        static final Schema SCHEMA = Schema.structureBuilder(ID, UnitTypeTrait.INSTANCE)
                                           .builderSupplier(() -> {
                                               throw new UnsupportedOperationException();
                                           })
                                           .shapeClass(NoInputRequest.class)
                                           .build();

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            throw new AssertionError("A Unit input is not serialized");
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            throw new IllegalArgumentException(member.toString());
        }
    }

    /**
     * An operation output shaped like a generated response class: an {@link AwsResponse} whose builder is also a
     * {@link ShapeBuilder}. Its {@code count} member is bound to a header, which the query protocols ignore.
     */
    static class GreetResponse extends AwsResponse implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "GreetResponse");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .putMember("greeting", PreludeSchemas.STRING)
                                           .putMember("count", PreludeSchemas.STRING, new HttpHeaderTrait("X-Count"))
                                           .builderSupplier(() -> new GreetResponseBuilder(
                                               TestShapes.GreetResponse.SCHEMA))
                                           .shapeClass(GreetResponse.class)
                                           .build();

        private final Schema schema;
        private final String greeting;
        private final String count;

        GreetResponse(GreetResponseBuilder builder) {
            super(builder);
            this.schema = builder.schema;
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
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) ("greeting".equals(member.memberName()) ? greeting : count);
        }

        static final class GreetResponseBuilder extends AwsResponse.BuilderImpl implements ShapeBuilder<GreetResponse> {
            private final Schema schema;
            private String greeting;
            private String count;

            GreetResponseBuilder(Schema schema) {
                this.schema = schema;
            }

            @Override
            public GreetResponse build() {
                return new GreetResponse(this);
            }

            @Override
            public GreetResponseBuilder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(schema, this, (builder, member, d) -> {
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
                return schema;
            }
        }
    }

    /**
     * An output without members; its builder fails if it is asked to read the body.
     */
    static final class EmptyResponse {
        static final ShapeId ID = ShapeId.fromParts(NS, "EmptyResponse");
        static final Schema SCHEMA = Schema.structureBuilder(ID)
                                           .builderSupplier(() -> new GreetResponse.GreetResponseBuilder(
                                               TestShapes.EmptyResponse.SCHEMA))
                                           .build();

        private EmptyResponse() {
        }
    }
}
