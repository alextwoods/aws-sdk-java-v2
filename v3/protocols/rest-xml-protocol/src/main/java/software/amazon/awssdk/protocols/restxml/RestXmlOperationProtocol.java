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

package software.amazon.awssdk.protocols.restxml;

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
import software.amazon.awssdk.protocols.core.binding.HttpMessageBindings;
import software.amazon.awssdk.protocols.core.binding.HttpOperationBindings;
import software.amazon.awssdk.protocols.core.binding.UriSegment;
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.protocols.core.eventstream.EventPayloadCodec;
import software.amazon.awssdk.protocols.core.eventstream.OperationEventStreams;
import software.amazon.awssdk.protocols.core.eventstream.OutputEventStream;
import software.amazon.awssdk.protocols.core.serde.HttpBindingReader;
import software.amazon.awssdk.protocols.core.serde.HttpBindingWriter;
import software.amazon.awssdk.protocols.core.serde.RequestTargets;
import software.amazon.awssdk.protocols.core.serde.StreamingPayloads;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.awssdk.protocols.core.xml.XmlElement;
import software.amazon.awssdk.protocols.core.xml.XmlErrorEnvelope;
import software.amazon.awssdk.protocols.core.xml.XmlShapeDeserializer;
import software.amazon.awssdk.protocols.core.xml.XmlShapeSerializer;
import software.amazon.awssdk.protocols.core.xml.XmlTreeReader;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.SpecificShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.MediaTypeTrait;

/** The {@code restXml} SERDE plan of one operation (see {@link RestXmlClientProtocol} for the wire rules). */
final class RestXmlOperationProtocol implements OperationProtocol {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String XML = "application/xml";

    /** Parses a non-blank body and reads the members held by its root element. */
    private static final HttpBindingReader.BodyDeserializerFactory BODIES =
        body -> XmlShapeDeserializer.of(XmlTreeReader.parse(bytes(body)));

    private final RestXmlClientProtocol client;
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
    private final OperationEventStreams eventStreams;
    private final String streamingRejection;

    RestXmlOperationProtocol(RestXmlClientProtocol client, OperationSchemas schemas) {
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
        this.payloadContentType = bindings.request().payload().map(RestXmlOperationProtocol::payloadContentType)
                                          .orElse(XML);
        this.streamingRejection = StreamingPayloads.streamingRejection(schemas, client.protocolId(), true, true);
        this.eventStreams = OperationEventStreams.of(wire, schemas, new EventPayloadCodec() {
            @Override
            public String mediaType() {
                return XML;
            }

            @Override
            public byte[] serialize(Schema schema, SerializableStruct value) {
                return XmlShapeSerializer.serialize(schema, value, client.serviceNamespace());
            }

            @Override
            public ShapeDeserializer deserializer(ByteBuffer payload) {
                return XmlShapeDeserializer.of(XmlTreeReader.parse(bytes(payload)));
            }
        }, client.errors());
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
            default -> XML;
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
            case MEMBERS -> XmlShapeSerializer.serialize(writer.bodyMembers(input), client.serviceNamespace());
            case STRUCTURED -> {
                PayloadCapture capture = capture(input);
                Schema member = bindings.request().payload().orElseThrow().member();
                yield capture.struct == null ? null
                                             : XmlShapeSerializer.serialize(member, capture.struct,
                                                                            client.serviceNamespace());
            }
            case BLOB, STRING -> capture(input).bytes;
            default -> throw new IllegalStateException("Unexpected payload kind " + bindings.request().payloadKind());
        };
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
        return output.read(wire, withoutBlankXml(response), BODIES, builder, null,
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
        return output.readWithoutStreamingPayload(wire, response, BODIES, builder, null);
    }

    /**
     * {@code response}, with an empty body if its body is blank XML (whitespace only), which has no body members.
     * A blob or text payload is the raw body, so it is kept as is.
     */
    private SdkHttpResponse withoutBlankXml(SdkHttpResponse response) {
        HttpMessageBindings.PayloadKind kind = bindings.response().payloadKind();
        if (kind == HttpMessageBindings.PayloadKind.STREAMING_BLOB
            || kind == HttpMessageBindings.PayloadKind.EVENT_STREAM) {
            return response; // the stream is the payload; never buffered
        }
        if (kind == HttpMessageBindings.PayloadKind.BLOB || kind == HttpMessageBindings.PayloadKind.STRING) {
            return response;
        }
        SdkHttpResponse finite = HttpBindingReader.buffered(response);
        byte[] body = finite.body().asByteArray();
        if (body.length > 0 && XmlTreeReader.isBlank(body)) {
            return finite.toBuilder().body(HttpBody.empty()).build();
        }
        return finite;
    }

    @Override
    public Optional<OutputEventStream> outputEventStream() {
        return Optional.ofNullable(eventStreams.restOutput());
    }

    @Override
    public AwsServiceException deserializeError(SdkHttpResponse errorResponse) {
        SdkHttpResponse response = HttpBindingReader.buffered(errorResponse);
        Optional<XmlErrorEnvelope> envelope = readEnvelope(response.body().asByteArray());
        ModeledErrors errors = client.errors();
        String code = envelope.map(XmlErrorEnvelope::code).orElse(null);
        ModeledErrors.ErrorResponse headersMetadata = errors.errorResponse(
            code, envelope.map(XmlErrorEnvelope::message).orElse(null), response);
        String requestId = envelope.map(XmlErrorEnvelope::requestId).orElse(headersMetadata.requestId());
        ModeledErrors.ErrorResponse metadata = new ModeledErrors.ErrorResponse(
            headersMetadata.details(), requestId, headersMetadata.extendedRequestId(), headersMetadata.statusCode());
        Optional<ShapeId> error = errors.byName(operationErrors, code);
        if (error.isEmpty()) {
            return errors.generic(metadata);
        }
        ShapeBuilder<?> builder = errors.newBuilder(error.get());
        HttpBindingReader reader = errorReaders.get(error.get());
        if (reader == null) {
            reader = client.registryErrorReader(builder.schema());
        }
        // The error's body members are the children of the <Error> element, wherever the envelope places it.
        XmlElement members = envelope.get().error();
        reader.readInto(wire, response, body -> XmlShapeDeserializer.of(members), builder, null);
        return errors.complete(builder, metadata);
    }

    /**
     * The restXml error envelope of {@code body}: {@code <ErrorResponse><Error>} or a bare {@code <Error>}; empty
     * for a blank or non-XML body (for example an HTML proxy error page) or any other document.
     */
    private static Optional<XmlErrorEnvelope> readEnvelope(byte[] body) {
        if (XmlTreeReader.isBlank(body)) {
            return Optional.empty();
        }
        XmlElement root;
        try {
            root = XmlTreeReader.parse(body);
        } catch (SerializationException e) {
            return Optional.empty();
        }
        if (!root.matches("ErrorResponse") && !root.matches("Error")) {
            return Optional.empty();
        }
        return XmlErrorEnvelope.read(root);
    }

    private void requireNotStreaming() {
        if (streamingRejection != null) {
            throw new UnsupportedOperationException(streamingRejection);
        }
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] out = new byte[buffer.remaining()];
        buffer.duplicate().get(out);
        return out;
    }

    /** Captures the payload member's value: a structure or union, or the bytes of a blob or string. */
    private static final class PayloadCapture extends SpecificShapeSerializer {
        private SerializableStruct struct;
        private byte[] bytes;

        @Override
        public void writeStruct(Schema schema, SerializableStruct value) {
            struct = value;
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            throw new SerializationException("Documents are not supported by restXml");
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            bytes = bytes(value);
        }

        @Override
        public void writeString(Schema schema, String value) {
            bytes = value.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void writeNull(Schema schema) {
        }
    }
}
