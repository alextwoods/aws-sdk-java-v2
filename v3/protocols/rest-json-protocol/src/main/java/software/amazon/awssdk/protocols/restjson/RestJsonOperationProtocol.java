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

package software.amazon.awssdk.protocols.restjson;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.binding.HttpMemberBinding;
import software.amazon.awssdk.protocols.core.binding.HttpOperationBindings;
import software.amazon.awssdk.protocols.core.binding.UriSegment;
import software.amazon.awssdk.protocols.core.error.ErrorCodes;
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.protocols.core.eventstream.EventPayloadCodec;
import software.amazon.awssdk.protocols.core.eventstream.OperationEventStreams;
import software.amazon.awssdk.protocols.core.eventstream.OutputEventStream;
import software.amazon.awssdk.protocols.core.serde.HttpBindingReader;
import software.amazon.awssdk.protocols.core.serde.HttpBindingWriter;
import software.amazon.awssdk.protocols.core.serde.RequestTargets;
import software.amazon.awssdk.protocols.core.serde.StreamingPayloads;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.SpecificShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.MediaTypeTrait;

/** The {@code restJson1} SERDE plan of one operation (see {@link RestJsonClientProtocol} for the wire rules). */
final class RestJsonOperationProtocol implements OperationProtocol {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String JSON = "application/json";
    private static final byte[] EMPTY_OBJECT = {'{', '}'};
    private static final String[] MESSAGE_MEMBERS = {"message", "Message", "errorMessage"};

    private final RestJsonClientProtocol client;
    private final OperationSchemas schemas;
    private final WireContext wire;
    private final HttpOperationBindings bindings;
    private final HttpBindingWriter writer;
    private final HttpBindingReader output;
    private final Map<ShapeId, HttpBindingReader> errorReaders;
    private final List<UriSegment> hostPrefix;
    private final Map<String, ShapeId> operationErrors;
    private final String method;
    private final String payloadContentType;
    /** Why this operation's streaming members cannot be served, or {@code null}. */
    private final String streamingRejection;
    private final HttpBindingReader.BodyDeserializerFactory bodies;
    private final OperationEventStreams eventStreams;

    RestJsonOperationProtocol(RestJsonClientProtocol client, OperationSchemas schemas) {
        this.client = client;
        this.schemas = Objects.requireNonNull(schemas, "operation");
        this.wire = new WireContext(client.protocolId(), schemas.id());
        this.bindings = HttpOperationBindings.of(schemas);
        this.writer = HttpBindingWriter.of(bindings);
        this.output = HttpBindingReader.of(bindings.response());
        Map<ShapeId, HttpBindingReader> readers = new HashMap<>();
        bindings.errors().forEach((id, errorBindings) -> readers.put(id, HttpBindingReader.of(errorBindings)));
        this.errorReaders = Map.copyOf(readers);
        this.hostPrefix = bindings.hostPrefix();
        this.operationErrors = ModeledErrors.operationErrorsByName(schemas);
        this.method = bindings.method().orElse("POST");
        this.payloadContentType = bindings.request().payload().map(RestJsonOperationProtocol::payloadContentType)
                                          .orElse(JSON);
        this.streamingRejection = StreamingPayloads.streamingRejection(schemas, client.protocolId(), true, true);
        this.bodies = client.codec()::createDeserializer;
        this.eventStreams = OperationEventStreams.of(wire, schemas, EventPayloadCodec.of(client.codec(), JSON),
                                                     client.errors());
    }

    private static String payloadContentType(HttpMemberBinding payload) {
        Schema member = payload.member();
        MediaTypeTrait mediaType = member.getTrait(TraitKey.MEDIA_TYPE_TRAIT);
        if (mediaType != null) {
            return mediaType.getValue();
        }
        return switch (member.type()) {
            case BLOB -> "application/octet-stream";
            case STRING, ENUM -> "text/plain";
            default -> JSON;
        };
    }

    @Override
    public OperationSchemas schemas() {
        return schemas;
    }

    @Override
    public SdkHttpRequest serializeRequest(SerializableStruct input, URI endpoint) {
        requireNotStreaming();
        Objects.requireNonNull(input, "input");
        if (!input.schema().id().equals(schemas.input().id())) {
            throw new IllegalArgumentException("The input of " + schemas.id() + " is " + schemas.input().id()
                                               + ", not " + input.schema().id());
        }
        SdkHttpRequest.Builder request = RequestTargets.start(endpoint, method, "/");
        writer.write(input, endpoint.getRawPath(), request);
        if (client.context().hostPrefixInjection()) {
            RequestTargets.applyHostPrefix(request, endpoint.getHost(), hostPrefix, input);
        }
        if (StreamingPayloads.isStreamingBlob(bindings.request())) {
            StreamingPayloads.writeRequest(bindings.request(), writer, input, request);
            return request.build();
        }
        if (eventStreams.hasInput()) {
            eventStreams.writeRestRequest(input, request);
            return request.build();
        }
        byte[] body = body(input);
        if (body != null) {
            if (!request.hasHeader(CONTENT_TYPE)) {
                request.addHeader(CONTENT_TYPE, payloadContentType);
            }
            request.body(HttpBody.fromBytes(body));
        }
        return request.build();
    }

    /** The request body, or {@code null} for none. */
    private byte[] body(SerializableStruct input) {
        return switch (bindings.request().payloadKind()) {
            case NONE -> null;
            case MEMBERS -> bytes(client.codec().serialize(writer.bodyMembers(input)));
            case STRUCTURED -> structuredPayload(input);
            case BLOB, STRING -> {
                PayloadCapture capture = capture(input);
                yield capture.bytes;
            }
            default -> throw new IllegalStateException("Unexpected payload kind " + bindings.request().payloadKind());
        };
    }

    private byte[] structuredPayload(SerializableStruct input) {
        if (!capture(input).present) {
            // An absent structure payload is an empty object; an absent union or document is no body.
            Schema member = bindings.request().payload().orElseThrow().member();
            return member.type() == ShapeType.STRUCTURE ? EMPTY_OBJECT.clone() : null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ShapeSerializer serializer = client.codec().createSerializer(out)) {
            writer.payloadMember(input).serializeMembers(serializer);
            serializer.flush();
        }
        return out.toByteArray();
    }

    private PayloadCapture capture(SerializableStruct input) {
        PayloadCapture capture = new PayloadCapture();
        writer.payloadMember(input).serializeMembers(capture);
        return capture;
    }

    @Override
    public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response, ShapeBuilder<T> builder) {
        requireNotStreaming();
        Objects.requireNonNull(builder, "output");
        if (!builder.schema().id().equals(schemas.output().id())) {
            throw new IllegalArgumentException("The output of " + schemas.id() + " is " + schemas.output().id()
                                               + ", not " + builder.schema().id());
        }
        return output.read(wire, response, bodies, builder, client.timestamps(),
                           eventStreams.hasOutput() ? eventStreams.restResponse() : null);
    }

    @Override
    public <T extends SerializableStruct> T deserializeStreamingOutput(SdkHttpResponse response,
                                                                       ShapeBuilder<T> builder) {
        requireNotStreaming();
        Objects.requireNonNull(builder, "output");
        if (!builder.schema().id().equals(schemas.output().id())) {
            throw new IllegalArgumentException("The output of " + schemas.id() + " is " + schemas.output().id()
                                               + ", not " + builder.schema().id());
        }
        // The payload is the raw, unread body; only the status code and headers are bound.
        return output.readWithoutStreamingPayload(wire, response, bodies, builder, client.timestamps());
    }

    @Override
    public Optional<OutputEventStream> outputEventStream() {
        return Optional.ofNullable(eventStreams.restOutput());
    }

    @Override
    public AwsServiceException deserializeError(SdkHttpResponse errorResponse) {
        SdkHttpResponse response = HttpBindingReader.buffered(errorResponse);
        Document document = readErrorDocument(response.body().asByteArray());
        String rawType = response.headers().firstValue(ErrorCodes.ERROR_TYPE_HEADER)
                                 .orElseGet(() -> member(document, "__type").orElseGet(
                                     () -> member(document, "code").orElse(null)));
        String type = ErrorCodes.sanitizeErrorType(rawType);
        String message = null;
        for (String name : MESSAGE_MEMBERS) {
            message = member(document, name).orElse(null);
            if (message != null) {
                break;
            }
        }
        ModeledErrors errors = client.errors();
        ModeledErrors.ErrorResponse metadata = errors.errorResponse(type, message, response);
        Optional<ShapeId> error = errors.byName(operationErrors, type);
        if (error.isEmpty()) {
            return errors.generic(metadata);
        }
        ShapeBuilder<?> builder = errors.newBuilder(error.get());
        HttpBindingReader reader = errorReaders.get(error.get());
        if (reader == null) {
            reader = client.registryErrorReader(builder.schema());
        }
        reader.readInto(wire, response, bodies, builder, client.timestamps());
        return errors.complete(builder, metadata);
    }

    private void requireNotStreaming() {
        if (streamingRejection != null) {
            throw new UnsupportedOperationException(streamingRejection);
        }
    }

    /** The error body as a document, or {@code null} if it is not a JSON object (the error is then generic). */
    private Document readErrorDocument(byte[] body) {
        if (body.length == 0) {
            return null;
        }
        try {
            Document document = client.codec().createDeserializer(body).readDocument();
            return document != null && (document.type() == ShapeType.MAP || document.type() == ShapeType.STRUCTURE)
                   ? document : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Optional<String> member(Document document, String name) {
        if (document == null) {
            return Optional.empty();
        }
        Document value = document.getMember(name);
        if (value == null || value.type() != ShapeType.STRING) {
            return Optional.empty();
        }
        return Optional.of(value.asString());
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] out = new byte[buffer.remaining()];
        buffer.duplicate().get(out);
        return out;
    }

    /** Captures whether the payload member is present and, for a blob or string, its bytes. */
    private static final class PayloadCapture extends SpecificShapeSerializer {
        private boolean present;
        private byte[] bytes;

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            present = true;
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            present = true;
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            present = true;
            bytes = bytes(value);
        }

        @Override
        public void writeString(Schema schema, String value) {
            present = true;
            bytes = value.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void writeNull(Schema schema) {
        }
    }
}
