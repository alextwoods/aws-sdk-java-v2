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

package software.amazon.awssdk.protocols.client.restxml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
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
import software.amazon.awssdk.schema.traits.ErrorTrait;
import software.amazon.awssdk.schema.traits.HttpErrorTrait;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.HttpLabelTrait;
import software.amazon.awssdk.schema.traits.HttpPayloadTrait;
import software.amazon.awssdk.schema.traits.HttpQueryTrait;
import software.amazon.awssdk.schema.traits.HttpResponseCodeTrait;
import software.amazon.awssdk.schema.traits.HttpTrait;
import software.amazon.awssdk.schema.traits.StreamingTrait;
import software.amazon.awssdk.schema.traits.XmlAttributeTrait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;

/**
 * Hand-written schemas, an error class shaped like a generated one, protocol settings and response helpers for the
 * tests. Inputs and outputs are {@link SchemaStruct}s.
 */
final class TestShapes {
    static final String NS = "smithy.example";
    static final String XMLNS = "https://example.com/doc/2024-01-01/";
    static final ShapeId SERVICE_ID = ShapeId.fromParts(NS, "ExampleService");
    static final Schema SERVICE_SCHEMA = Schema.createService(SERVICE_ID, new XmlNamespaceTrait(XMLNS));
    static final ApiService SERVICE = () -> SERVICE_SCHEMA;
    static final ProtocolSettings SETTINGS = ProtocolSettings.builder()
                                                             .serviceSchema(SERVICE_SCHEMA)
                                                             .baseExceptionBuilder(ExampleServiceException::builder)
                                                             .build();
    static final ErrorRegistry ERRORS = ErrorRegistry.builder()
                                                     .putLazyType(FooError.ID, () -> FooError.SCHEMA)
                                                     .build();

    static final Schema NESTED = Schema.structureBuilder(ShapeId.fromParts(NS, "Nested"))
                                       .putMember("value", PreludeSchemas.STRING)
                                       .builderSupplier(() -> SchemaStruct.builder(TestShapes.NESTED))
                                       .build();

    /**
     * {@code PUT /greet/{label}/{path+}?mode=fast}: a label, a greedy label, a query parameter, a header and body
     * members, one with {@code xmlName} and one with {@code xmlAttribute}. The structure's {@code xmlName} names the
     * root element, as the C2J converter writes it from the input's {@code locationName}.
     */
    static final Schema GREET_INPUT =
        Schema.structureBuilder(ShapeId.fromParts(NS, "GreetInput"), new XmlNameTrait("GreetRequest"))
              .putMember("label", PreludeSchemas.STRING, HttpLabelTrait.INSTANCE)
              .putMember("path", PreludeSchemas.STRING, HttpLabelTrait.INSTANCE)
              .putMember("query", PreludeSchemas.STRING, new HttpQueryTrait("q"))
              .putMember("header", PreludeSchemas.STRING, new HttpHeaderTrait("X-Header"))
              .putMember("id", PreludeSchemas.STRING, XmlAttributeTrait.INSTANCE)
              .putMember("message", PreludeSchemas.STRING, new XmlNameTrait("Msg"))
              .putMember("nested", NESTED)
              .builderSupplier(() -> SchemaStruct.builder(TestShapes.GREET_INPUT))
              .build();

    /**
     * A body member with {@code xmlName}, a header and the status code.
     */
    static final Schema GREET_OUTPUT =
        Schema.structureBuilder(ShapeId.fromParts(NS, "GreetOutput"))
              .putMember("greeting", PreludeSchemas.STRING, new XmlNameTrait("Hello"))
              .putMember("count", PreludeSchemas.INTEGER, new HttpHeaderTrait("X-Count"))
              .putMember("status", PreludeSchemas.INTEGER, HttpResponseCodeTrait.INSTANCE)
              .builderSupplier(() -> SchemaStruct.builder(TestShapes.GREET_OUTPUT))
              .build();

    static final SimpleApiOperation<SerializableStruct, SerializableStruct> GREET =
        operation("Greet", new HttpTrait("PUT", "/greet/{label}/{path+}?mode=fast"), GREET_INPUT, GREET_OUTPUT);

    /**
     * An input with only a header member: no body members and no payload member.
     */
    static final Schema HEADER_ONLY =
        Schema.structureBuilder(ShapeId.fromParts(NS, "HeaderOnly"))
              .putMember("header", PreludeSchemas.STRING, new HttpHeaderTrait("X-Header"))
              .builderSupplier(() -> SchemaStruct.builder(TestShapes.HEADER_ONLY))
              .build();

    static final Schema EMPTY = Schema.structureBuilder(ShapeId.fromParts(NS, "Empty"))
                                      .builderSupplier(() -> SchemaStruct.builder(TestShapes.EMPTY))
                                      .build();

    static final SimpleApiOperation<SerializableStruct, SerializableStruct> HEADER_ONLY_OPERATION =
        operation("HeaderOnlyOperation", new HttpTrait("GET", "/header"), HEADER_ONLY, HEADER_ONLY);

    static final SimpleApiOperation<SerializableStruct, SerializableStruct> NO_OUTPUT =
        operation("NoOutput", new HttpTrait("POST", "/no-output"), EMPTY, EMPTY);

    /**
     * A payload structure with its own {@code xmlName} and {@code xmlNamespace}, like CloudFront's
     * {@code DistributionConfig}.
     */
    static final Schema PAYLOAD_CONFIG =
        Schema.structureBuilder(ShapeId.fromParts(NS, "PayloadConfig"), new XmlNameTrait("Config"),
                                new XmlNamespaceTrait("https://config.example.com/"))
              .putMember("value", PreludeSchemas.STRING)
              .builderSupplier(() -> SchemaStruct.builder(TestShapes.PAYLOAD_CONFIG))
              .build();

    /**
     * Structure {@code @httpPayload} members, in both directions: one targeting {@link #NESTED} and one targeting
     * {@link #PAYLOAD_CONFIG}.
     */
    static final Schema STRUCT_PAYLOAD =
        Schema.structureBuilder(ShapeId.fromParts(NS, "StructPayload"))
              .putMember("header", PreludeSchemas.STRING, new HttpHeaderTrait("X-Header"))
              .putMember("nested", NESTED, HttpPayloadTrait.INSTANCE)
              .builderSupplier(() -> SchemaStruct.builder(TestShapes.STRUCT_PAYLOAD))
              .build();

    static final SimpleApiOperation<SerializableStruct, SerializableStruct> STRUCT_PAYLOAD_OPERATION =
        operation("StructPayloadOperation", new HttpTrait("POST", "/struct"), STRUCT_PAYLOAD, STRUCT_PAYLOAD);

    static final Schema CONFIG_PAYLOAD =
        Schema.structureBuilder(ShapeId.fromParts(NS, "ConfigPayload"))
              .putMember("config", PAYLOAD_CONFIG, HttpPayloadTrait.INSTANCE)
              .builderSupplier(() -> SchemaStruct.builder(TestShapes.CONFIG_PAYLOAD))
              .build();

    static final SimpleApiOperation<SerializableStruct, SerializableStruct> CONFIG_PAYLOAD_OPERATION =
        operation("ConfigPayloadOperation", new HttpTrait("POST", "/config"), CONFIG_PAYLOAD, CONFIG_PAYLOAD);

    /**
     * A blob {@code @httpPayload} member, in both directions.
     */
    static final Schema BLOB_PAYLOAD =
        Schema.structureBuilder(ShapeId.fromParts(NS, "BlobPayload"))
              .putMember("data", PreludeSchemas.BLOB, HttpPayloadTrait.INSTANCE)
              .builderSupplier(() -> SchemaStruct.builder(TestShapes.BLOB_PAYLOAD))
              .build();

    static final SimpleApiOperation<SerializableStruct, SerializableStruct> BLOB_PAYLOAD_OPERATION =
        operation("BlobPayloadOperation", new HttpTrait("POST", "/blob"), BLOB_PAYLOAD, BLOB_PAYLOAD);

    static final Schema STREAMING_BLOB = Schema.createBlob(ShapeId.fromParts(NS, "StreamingBlob"),
                                                           StreamingTrait.INSTANCE);

    /**
     * A {@code @streaming} blob {@code @httpPayload} output with a header member.
     */
    static final Schema STREAMING_OUTPUT =
        Schema.structureBuilder(ShapeId.fromParts(NS, "StreamingOutput"))
              .putMember("header", PreludeSchemas.STRING, new HttpHeaderTrait("X-Header"))
              .putMember("body", STREAMING_BLOB, HttpPayloadTrait.INSTANCE)
              .builderSupplier(() -> SchemaStruct.builder(TestShapes.STREAMING_OUTPUT))
              .build();

    static final SimpleApiOperation<SerializableStruct, SerializableStruct> STREAMING_OUTPUT_OPERATION =
        operation("StreamingOutputOperation", new HttpTrait("GET", "/stream"), EMPTY, STREAMING_OUTPUT);
    /**
     * A {@code @streaming} blob {@code @httpPayload} input with a header member, as a generated request class holds it:
     * the client method takes the body as a separate argument, so the struct never sets {@code body}.
     */
    static final Schema STREAMING_INPUT =
        Schema.structureBuilder(ShapeId.fromParts(NS, "StreamingInput"))
              .putMember("header", PreludeSchemas.STRING, new HttpHeaderTrait("X-Header"))
              .putMember("body", STREAMING_BLOB, HttpPayloadTrait.INSTANCE)
              .builderSupplier(() -> SchemaStruct.builder(TestShapes.STREAMING_INPUT))
              .build();
    static final SimpleApiOperation<SerializableStruct, SerializableStruct> STREAMING_INPUT_OPERATION =
        operation("StreamingInputOperation", new HttpTrait("PUT", "/stream"), STREAMING_INPUT, EMPTY);

    private TestShapes() {
    }

    private static SimpleApiOperation<SerializableStruct, SerializableStruct> operation(String name, HttpTrait http,
                                                                                         Schema input, Schema output) {
        return SimpleApiOperation.<SerializableStruct, SerializableStruct>builder()
                                 .schema(Schema.createOperation(ShapeId.fromParts(NS, name), http))
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
     * A modeled 404 error with a body member {@code Reason} and a header member {@code retryAfter}. Like the generated
     * restXml exceptions, it has no message member: the message is the error's {@code <Message>}.
     */
    static final class FooError extends ExampleServiceException implements SerializableStruct {
        static final ShapeId ID = ShapeId.fromParts(NS, "FooError");
        static final Schema SCHEMA = Schema.structureBuilder(ID, new ErrorTrait("client"), new HttpErrorTrait(404))
                                           .putMember("Reason", PreludeSchemas.STRING)
                                           .putMember("retryAfter", PreludeSchemas.STRING,
                                                      new HttpHeaderTrait("X-Retry-After"))
                                           .builderSupplier(FooBuilder::new)
                                           .shapeClass(FooError.class)
                                           .build();

        private final String reason;
        private final String retryAfter;

        private FooError(FooBuilder builder) {
            super(builder);
            this.reason = builder.reason;
            this.retryAfter = builder.retryAfter;
        }

        String reason() {
            return reason;
        }

        String retryAfter() {
            return retryAfter;
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
            private String retryAfter;

            @Override
            public FooError build() {
                return new FooError(this);
            }

            @Override
            public FooBuilder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (builder, member, d) -> {
                    String value = d.readString(member);
                    if ("Reason".equals(member.memberName())) {
                        builder.reason = value;
                    } else {
                        builder.retryAfter = value;
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
