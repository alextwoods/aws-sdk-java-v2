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

package software.amazon.awssdk.protocols.rpcv2cbor;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.binding.UriSegment;
import software.amazon.awssdk.protocols.core.error.ErrorCodes;
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.protocols.core.eventstream.EventPayloadCodec;
import software.amazon.awssdk.protocols.core.eventstream.EventStreams;
import software.amazon.awssdk.protocols.core.eventstream.OperationEventStreams;
import software.amazon.awssdk.protocols.core.eventstream.OutputEventStream;
import software.amazon.awssdk.protocols.core.serde.HttpBindingReader;
import software.amazon.awssdk.protocols.core.serde.LocatingDeserializer;
import software.amazon.awssdk.protocols.core.serde.RequestTargets;
import software.amazon.awssdk.protocols.core.serde.StreamingPayloads;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/** The rpcv2Cbor SERDE plan of one operation (see {@link Rpcv2CborClientProtocol} for the wire rules). */
final class Rpcv2CborOperationProtocol implements OperationProtocol {
    /** The protocol header of every rpcv2Cbor request and response. */
    static final String SMITHY_PROTOCOL_HEADER = "smithy-protocol";
    /** The value of {@value #SMITHY_PROTOCOL_HEADER}. */
    static final String SMITHY_PROTOCOL = "rpc-v2-cbor";
    /** The media type of every buffered rpcv2Cbor body. */
    static final String MEDIA_TYPE = "application/cbor";

    /** The CBOR empty map ({@code {}}), read in place of an empty response body. */
    private static final byte[] EMPTY_MAP = {(byte) 0xA0};
    private static final String TYPE_MEMBER = "__type";
    private static final String[] MESSAGE_MEMBERS = {"message", "Message"};
    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    private final Rpcv2CborClientProtocol client;
    private final OperationSchemas schemas;
    private final WireContext wire;
    private final String path;
    private final List<UriSegment> hostPrefix;
    private final Map<String, ShapeId> operationErrors;
    private final boolean unitInput;
    /** Why this operation's streaming members cannot be served, or {@code null}. */
    private final String streamingRejection;
    private final boolean queryCompatible;
    private final OperationEventStreams eventStreams;

    Rpcv2CborOperationProtocol(Rpcv2CborClientProtocol client, OperationSchemas schemas) {
        this.client = client;
        this.schemas = Objects.requireNonNull(schemas, "operation");
        this.wire = new WireContext(client.protocolId(), schemas.id());
        this.path = client.pathPrefix() + schemas.id().getName();
        this.hostPrefix = RequestTargets.hostPrefix(schemas.operation());
        this.operationErrors = ModeledErrors.operationErrorsByName(schemas);
        this.unitInput = isUnit(schemas.input());
        this.streamingRejection = StreamingPayloads.streamingRejection(schemas, client.protocolId(), false, true);
        this.queryCompatible = client.context().service().awsQueryCompatible();
        this.eventStreams = OperationEventStreams.of(wire, schemas, EventPayloadCodec.of(client.codec(), MEDIA_TYPE),
                                                     client.errors());
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
        SdkHttpRequest.Builder request = RequestTargets.start(endpoint, "POST", path)
                                                       .addHeader(SMITHY_PROTOCOL_HEADER, SMITHY_PROTOCOL)
                                                       .addHeader("Accept", MEDIA_TYPE);
        if (queryCompatible) {
            request.addHeader(ErrorCodes.QUERY_MODE_HEADER, "true");
        }
        if (client.context().hostPrefixInjection()) {
            RequestTargets.applyHostPrefix(request, endpoint.getHost(), hostPrefix, input);
        }
        if (eventStreams.hasOutput()) {
            request.putHeader("Accept", EventStreams.CONTENT_TYPE);
        }
        if (unitInput) {
            // An operation without modeled input sends no body and no Content-Type.
            return request.build();
        }
        if (eventStreams.writeRpcRequest(input, request)) {
            return request.build();
        }
        return request.addHeader("Content-Type", MEDIA_TYPE)
                      .body(HttpBody.fromBytes(client.codec().serialize(input)))
                      .build();
    }

    /** Only status code 200 is success; every other status code, including other 2xx codes, is an error. */
    @Override
    public boolean isError(SdkHttpResponse response) {
        return response.statusCode() != 200;
    }

    @Override
    public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response, ShapeBuilder<T> output) {
        requireNotStreaming();
        Objects.requireNonNull(output, "output");
        if (!output.schema().id().equals(schemas.output().id())) {
            throw new IllegalArgumentException("The output of " + schemas.id() + " is " + schemas.output().id()
                                               + ", not " + output.schema().id());
        }
        if (!hasProtocolHeader(response)) {
            throw wire.failure(schemas.output().id(), List.of(), WireContext.header(SMITHY_PROTOCOL_HEADER),
                               "the response is malformed: it has no " + SMITHY_PROTOCOL_HEADER + " header with the "
                               + "value " + SMITHY_PROTOCOL);
        }
        if (eventStreams.hasOutput()) {
            return eventStreams.readRpcResponse(response, output);
        }
        return LocatingDeserializer.deserialize(wire, WireContext.BODY,
                                                deserializer(bodyOrEmptyMap(response), output.schema().id()),
                                                output, null);
    }

    @Override
    public Optional<OutputEventStream> outputEventStream() {
        return Optional.ofNullable(eventStreams.rpcOutput());
    }

    @Override
    public AwsServiceException deserializeError(SdkHttpResponse errorResponse) {
        SdkHttpResponse response = HttpBindingReader.buffered(errorResponse);
        ModeledErrors errors = client.errors();
        if (!hasProtocolHeader(response)) {
            // A malformed error response is reported from its status code alone; its body and headers (other than
            // the request IDs recorded as metadata) are not interpreted.
            return errors.generic(errors.errorResponse(null, null, response));
        }
        byte[] body = bodyOrEmptyMap(response);
        Document document = readErrorDocument(body);
        String type = stripUri(member(document, TYPE_MEMBER).orElse(null));
        String name = ErrorCodes.sanitizeErrorType(type);
        Optional<ErrorCodes.QueryError> queryError = queryCompatible ? ErrorCodes.queryError(response.headers())
                                                                     : Optional.empty();
        String message = null;
        for (String member : MESSAGE_MEMBERS) {
            message = member(document, member).orElse(null);
            if (message != null) {
                break;
            }
        }
        String reportedCode = queryError.map(ErrorCodes.QueryError::code).orElse(name);
        ModeledErrors.ErrorResponse metadata = errors.errorResponse(reportedCode, message, response);
        Optional<ShapeId> error = errors.byName(operationErrors, type);
        if (error.isEmpty() && queryError.isPresent()) {
            error = errors.byQueryCode(queryError.get().code());
        }
        if (error.isEmpty()) {
            return errors.generic(metadata);
        }
        ShapeBuilder<?> builder = LocatingDeserializer.deserializeInto(wire, WireContext.BODY,
                                                                       deserializer(body, error.get()),
                                                                       errors.newBuilder(error.get()), null);
        return errors.complete(builder, metadata);
    }

    /**
     * A codec deserializer of {@code body}. The CBOR deserializer reads the first data item's head eagerly, so a
     * malformed body can fail here, before {@link LocatingDeserializer} reads it; such a failure is reported at the
     * root of {@code shape}, like any other malformed body.
     */
    private ShapeDeserializer deserializer(byte[] body, ShapeId shape) {
        try {
            return client.codec().createDeserializer(body);
        } catch (RuntimeException e) {
            throw wire.failure(shape, List.of(), WireContext.BODY,
                               "the body is not well-formed CBOR (" + e.getClass().getSimpleName() + ")");
        }
    }

    private void requireNotStreaming() {
        if (streamingRejection != null) {
            throw new UnsupportedOperationException(streamingRejection);
        }
    }

    private static boolean isUnit(Schema input) {
        return input.hasTrait(TraitKey.UNIT_TYPE_TRAIT) || input.id().equals(UNIT);
    }

    private static boolean hasProtocolHeader(SdkHttpResponse response) {
        return response.headers().firstValue(SMITHY_PROTOCOL_HEADER)
                       .map(value -> value.trim().equals(SMITHY_PROTOCOL))
                       .orElse(false);
    }

    private static byte[] bodyOrEmptyMap(SdkHttpResponse response) {
        byte[] bytes = response.body().buffered().asByteArray();
        return bytes.length == 0 ? EMPTY_MAP : bytes;
    }

    /** The error body as a document, or {@code null} if it is not a CBOR map (the error is then generic). */
    private Document readErrorDocument(byte[] body) {
        try {
            Document document = client.codec().createDeserializer(body).readDocument();
            return document != null && (document.type() == ShapeType.MAP || document.type() == ShapeType.STRUCTURE)
                   ? document : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The error type without any {@code :}-separated suffix, or {@code null} if it is absent or blank. */
    private static String stripUri(String type) {
        if (type == null) {
            return null;
        }
        int colon = type.indexOf(':');
        String value = (colon < 0 ? type : type.substring(0, colon)).trim();
        return value.isEmpty() ? null : value;
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
}
