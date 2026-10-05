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

package software.amazon.awssdk.protocols.awsjson;

import java.net.URI;
import java.nio.ByteBuffer;
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
import software.amazon.awssdk.protocols.core.eventstream.OperationEventStreams;
import software.amazon.awssdk.protocols.core.eventstream.OutputEventStream;
import software.amazon.awssdk.protocols.core.serde.HttpBindingReader;
import software.amazon.awssdk.protocols.core.serde.LocatingDeserializer;
import software.amazon.awssdk.protocols.core.serde.RequestTargets;
import software.amazon.awssdk.protocols.core.serde.StreamingPayloads;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/** The AWS JSON SERDE plan of one operation (see {@link AwsJsonClientProtocol} for the wire rules). */
final class AwsJsonOperationProtocol implements OperationProtocol {
    private static final byte[] EMPTY_OBJECT = {'{', '}'};
    private static final String[] MESSAGE_MEMBERS = {"message", "Message", "errorMessage"};

    private final AwsJsonClientProtocol client;
    private final OperationSchemas schemas;
    private final WireContext wire;
    private final String target;
    private final List<UriSegment> hostPrefix;
    private final Map<String, ShapeId> operationErrors;
    /** Why this operation's streaming members cannot be served, or {@code null}. */
    private final String streamingRejection;
    private final boolean queryCompatible;
    private final OperationEventStreams eventStreams;

    AwsJsonOperationProtocol(AwsJsonClientProtocol client, OperationSchemas schemas) {
        this.client = client;
        this.schemas = Objects.requireNonNull(schemas, "operation");
        this.wire = new WireContext(client.protocolId(), schemas.id());
        this.target = client.context().service().serviceId().getName() + "." + schemas.id().getName();
        this.hostPrefix = RequestTargets.hostPrefix(schemas.operation());
        this.operationErrors = ModeledErrors.operationErrorsByName(schemas);
        this.streamingRejection = StreamingPayloads.streamingRejection(schemas, client.protocolId(), false, true);
        this.queryCompatible = client.context().service().awsQueryCompatible();
        this.eventStreams = OperationEventStreams.of(wire, schemas,
                                                     EventPayloadCodec.of(client.codec(), "application/json"),
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
        SdkHttpRequest.Builder request = RequestTargets.start(endpoint, "POST", "/")
                                                       .addHeader("Content-Type", client.contentType())
                                                       .addHeader("X-Amz-Target", target);
        if (queryCompatible) {
            request.addHeader(ErrorCodes.QUERY_MODE_HEADER, "true");
        }
        if (client.context().hostPrefixInjection()) {
            RequestTargets.applyHostPrefix(request, endpoint.getHost(), hostPrefix, input);
        }
        if (eventStreams.writeRpcRequest(input, request)) {
            return request.build();
        }
        ByteBuffer body = client.codec().serialize(input);
        return request.body(HttpBody.fromBytes(body)).build();
    }

    @Override
    public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response, ShapeBuilder<T> output) {
        requireNotStreaming();
        Objects.requireNonNull(output, "output");
        if (!output.schema().id().equals(schemas.output().id())) {
            throw new IllegalArgumentException("The output of " + schemas.id() + " is " + schemas.output().id()
                                               + ", not " + output.schema().id());
        }
        if (eventStreams.hasOutput()) {
            return eventStreams.readRpcResponse(response, output);
        }
        return LocatingDeserializer.deserialize(wire, WireContext.BODY,
                                                client.codec().createDeserializer(bodyOrEmptyObject(response)),
                                                output, client.timestamps());
    }

    @Override
    public Optional<OutputEventStream> outputEventStream() {
        return Optional.ofNullable(eventStreams.rpcOutput());
    }

    @Override
    public AwsServiceException deserializeError(SdkHttpResponse errorResponse) {
        SdkHttpResponse response = HttpBindingReader.buffered(errorResponse);
        byte[] body = bodyOrEmptyObject(response);
        Document document = readErrorDocument(body);
        String rawType = response.headers().firstValue(ErrorCodes.ERROR_TYPE_HEADER)
                                 .orElseGet(() -> member(document, "__type").orElseGet(
                                     () -> member(document, "code").orElse(null)));
        String type = ErrorCodes.sanitizeErrorType(rawType);
        Optional<ErrorCodes.QueryError> queryError = queryCompatible ? ErrorCodes.queryError(response.headers())
                                                                     : Optional.empty();
        String message = null;
        for (String name : MESSAGE_MEMBERS) {
            message = member(document, name).orElse(null);
            if (message != null) {
                break;
            }
        }
        ModeledErrors errors = client.errors();
        String reportedCode = queryError.map(ErrorCodes.QueryError::code).orElse(type);
        ModeledErrors.ErrorResponse metadata = errors.errorResponse(reportedCode, message, response);
        Optional<ShapeId> error = errors.byName(operationErrors, type);
        if (error.isEmpty() && queryError.isPresent()) {
            error = errors.byQueryCode(queryError.get().code());
        }
        if (error.isEmpty()) {
            return errors.generic(metadata);
        }
        ShapeBuilder<?> builder = LocatingDeserializer.deserializeInto(wire, WireContext.BODY,
                                                                       client.codec().createDeserializer(body),
                                                                       errors.newBuilder(error.get()),
                                                                       client.timestamps());
        return errors.complete(builder, metadata);
    }

    private void requireNotStreaming() {
        if (streamingRejection != null) {
            throw new UnsupportedOperationException(streamingRejection);
        }
    }

    private static byte[] bodyOrEmptyObject(SdkHttpResponse response) {
        byte[] bytes = response.body().buffered().asByteArray();
        return isBlank(bytes) ? EMPTY_OBJECT : bytes;
    }

    private static boolean isBlank(byte[] bytes) {
        for (byte b : bytes) {
            if (b != ' ' && b != '\t' && b != '\r' && b != '\n') {
                return false;
            }
        }
        return true;
    }

    /** The error body as a document, or {@code null} if it is not a JSON object (the error is then generic). */
    private Document readErrorDocument(byte[] body) {
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
}
