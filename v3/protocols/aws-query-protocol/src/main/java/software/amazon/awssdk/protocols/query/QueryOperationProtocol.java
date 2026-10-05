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

package software.amazon.awssdk.protocols.query;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.protocols.core.serde.HttpBindingReader;
import software.amazon.awssdk.protocols.core.serde.LocatingDeserializer;
import software.amazon.awssdk.protocols.core.serde.RequestTargets;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.awssdk.protocols.core.xml.XmlElement;
import software.amazon.awssdk.protocols.core.xml.XmlErrorEnvelope;
import software.amazon.awssdk.protocols.core.xml.XmlShapeDeserializer;
import software.amazon.awssdk.protocols.core.xml.XmlTreeReader;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.model.shapes.ShapeId;

/** The query SERDE plan of one operation (see {@link QueryClientProtocol} for the wire rules). */
final class QueryOperationProtocol implements OperationProtocol {
    static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final QueryClientProtocol client;
    private final OperationSchemas schemas;
    private final WireContext wire;
    private final String action;
    private final String version;
    private final String responseElement;
    private final String resultElement;
    private final List<UriSegment> hostPrefix;
    private final Map<String, ShapeId> operationErrors;
    private final boolean streaming;

    QueryOperationProtocol(QueryClientProtocol client, OperationSchemas schemas) {
        this.client = client;
        this.schemas = Objects.requireNonNull(schemas, "operation");
        this.wire = new WireContext(client.protocolId(), schemas.id());
        this.action = schemas.id().getName();
        this.version = client.context().service().serviceVersion();
        this.responseElement = action + "Response";
        this.resultElement = action + "Result";
        this.hostPrefix = RequestTargets.hostPrefix(schemas.operation());
        this.operationErrors = ModeledErrors.operationErrorsByName(schemas);
        this.streaming = schemas.inputStreamMember().isPresent() || schemas.outputStreamMember().isPresent();
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
        String body = QueryFormSerializer.serialize(action, version, input, client.rules());
        SdkHttpRequest.Builder request = RequestTargets.start(endpoint, "POST", "/")
                                                       .addHeader("Content-Type", CONTENT_TYPE);
        if (client.context().hostPrefixInjection()) {
            RequestTargets.applyHostPrefix(request, endpoint.getHost(), hostPrefix, input);
        }
        return request.body(HttpBody.fromBytes(body.getBytes(StandardCharsets.US_ASCII))).build();
    }

    @Override
    public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response, ShapeBuilder<T> output) {
        requireNotStreaming();
        Objects.requireNonNull(output, "output");
        if (!output.schema().id().equals(schemas.output().id())) {
            throw new IllegalArgumentException("The output of " + schemas.id() + " is " + schemas.output().id()
                                               + ", not " + output.schema().id());
        }
        byte[] body = body(response);
        if (XmlTreeReader.isBlank(body)) {
            return LocatingDeserializer.build(wire, WireContext.BODY, output);
        }
        XmlElement root;
        try {
            root = XmlTreeReader.parse(body);
        } catch (SerializationException e) {
            throw wire.failure(output.schema().id(), List.of(), WireContext.BODY, "the body is not well-formed XML");
        }
        XmlElement members = members(root);
        if (members == null) {
            return LocatingDeserializer.build(wire, WireContext.BODY, output);
        }
        return LocatingDeserializer.deserialize(wire, WireContext.BODY, XmlShapeDeserializer.of(members), output,
                                                null);
    }

    /**
     * The element holding the output members: {@code <OpResult>} inside the response element (a response without
     * one, such as {@code <OpResponse><ResponseMetadata/></OpResponse>}, has none), or the response element itself.
     */
    private XmlElement members(XmlElement root) {
        if (client.wrapping() == QueryClientProtocol.ResultWrapping.NONE) {
            return root;
        }
        XmlElement result = root.child(resultElement);
        if (result == null && !root.matches(responseElement) && root.matches(resultElement)) {
            return root;
        }
        return result;
    }

    @Override
    public AwsServiceException deserializeError(SdkHttpResponse errorResponse) {
        SdkHttpResponse response = HttpBindingReader.buffered(errorResponse);
        byte[] body = body(response);
        Optional<XmlErrorEnvelope> envelope = Optional.empty();
        if (!XmlTreeReader.isBlank(body)) {
            try {
                envelope = XmlErrorEnvelope.read(XmlTreeReader.parse(body));
            } catch (SerializationException e) {
                // Not an XML error envelope (for example an HTML proxy error page): the error is generic.
            }
        }
        ModeledErrors errors = client.errors();
        String code = envelope.map(XmlErrorEnvelope::code).orElse(null);
        ModeledErrors.ErrorResponse headersMetadata = errors.errorResponse(
            code, envelope.map(XmlErrorEnvelope::message).orElse(null), response);
        String requestId = envelope.map(XmlErrorEnvelope::requestId).orElse(headersMetadata.requestId());
        ModeledErrors.ErrorResponse metadata = new ModeledErrors.ErrorResponse(
            headersMetadata.details(), requestId, headersMetadata.extendedRequestId(), headersMetadata.statusCode());
        Optional<ShapeId> error = errors.byQueryCode(code);
        if (error.isEmpty()) {
            error = errors.byName(operationErrors, code);
        }
        if (error.isEmpty()) {
            return errors.generic(metadata);
        }
        ShapeBuilder<?> builder = LocatingDeserializer.deserializeInto(
            wire, WireContext.BODY, XmlShapeDeserializer.of(envelope.get().error()), errors.newBuilder(error.get()),
            null);
        return errors.complete(builder, metadata);
    }

    private void requireNotStreaming() {
        if (streaming) {
            throw new UnsupportedOperationException(
                "Operation " + schemas.id() + " streams; " + client.protocolId() + " does not support streaming blob "
                + "or event stream members");
        }
    }

    private static byte[] body(SdkHttpResponse response) {
        return response.body().buffered().asByteArray();
    }
}
