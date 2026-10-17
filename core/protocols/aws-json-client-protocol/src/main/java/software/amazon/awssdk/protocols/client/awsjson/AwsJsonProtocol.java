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

package software.amazon.awssdk.protocols.client.awsjson;

import java.net.URI;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.client.http.AmznErrorHeaderExtractor;
import software.amazon.awssdk.protocols.client.http.HttpClientProtocol;
import software.amazon.awssdk.protocols.client.http.HttpErrorDeserializer;
import software.amazon.awssdk.protocols.client.http.ResponseBodies;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Shared base of the RPC-style AWS JSON protocols, {@code aws.protocols#awsJson1_0} and {@code aws.protocols#awsJson1_1},
 * which differ only in their content type.
 *
 * <p>Requests follow the <a href="https://smithy.io/2.0/aws/protocols/aws-json-1_0-protocol.html">AWS JSON
 * protocols</a>:
 * <ul>
 *     <li>{@code POST /}, appended to the endpoint path;</li>
 *     <li>{@code X-Amz-Target: <service shape name>.<operation name>};</li>
 *     <li>the input serialized by {@link JsonCodec} with {@link JsonSettings#AWS_JSON} (member names, never
 *     {@code jsonName}), {@code {}} for an input with no members, with {@code Content-Type:
 *     application/x-amz-json-1.0|1.1} and {@code Content-Length};</li>
 *     <li>{@code x-amzn-query-mode: true} when the service has the {@code aws.protocols#awsQueryCompatible} trait.</li>
 * </ul>
 * HTTP binding traits are ignored: every input member is serialized into the body, and every output member is read from
 * it.
 *
 * <p>A 2xx response is deserialized from its body through the operation's output builder; an empty body is read as
 * {@code {}}, content after the JSON value is rejected, and the body of an output without members is not read. Any
 * other response is an error, whose discriminator is the
 * {@code X-Amzn-Errortype} header, then the body's {@code __type} member, then its {@code code} member, or, for an
 * awsQueryCompatible service, first the code in the {@code x-amzn-query-error} header.
 *
 * <p>Event streams are not supported yet: operations with an event stream input or output fail with an
 * {@code SdkClientException}.
 *
 * <p>Instances are immutable after construction, apart from the request body size hints, and safe to use from multiple
 * threads.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.client.awsjson.AwsJsonProtocol}. It is created from
 * {@link ProtocolSettings} instead of the service ID, and adds the awsQueryCompatible headers.
 */
@SdkProtectedApi
@ThreadSafe
abstract class AwsJsonProtocol extends HttpClientProtocol {
    private static final String TARGET_HEADER = "X-Amz-Target";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";
    private static final String QUERY_MODE_HEADER = "x-amzn-query-mode";
    private static final String REQUEST_PATH = "/";

    private final ShapeId service;
    private final String contentType;
    private final String targetPrefix;
    private final JsonCodec codec;
    private final boolean awsQueryCompatible;
    private final HttpErrorDeserializer errorDeserializer;

    /**
     * Creates a protocol.
     *
     * @param protocolId the shape ID of the protocol definition trait
     * @param settings the settings of the service. The service shape name forms {@code X-Amz-Target}, and its
     *     namespace resolves relative error types
     * @param contentType the content type of requests, {@code application/x-amz-json-1.0} or {@code -1.1}
     * @throws NullPointerException if {@code settings} is null
     */
    AwsJsonProtocol(ShapeId protocolId, ProtocolSettings settings, String contentType) {
        super(protocolId);
        Validate.paramNotNull(settings, "settings");
        this.service = settings.service();
        this.contentType = Validate.paramNotNull(contentType, "contentType");
        this.targetPrefix = service.getName() + ".";
        this.codec = JsonCodec.builder()
                              .settings(JsonSettings.AWS_JSON)
                              .defaultNamespace(service.getNamespace())
                              .build();
        Schema serviceSchema = settings.serviceSchema();
        this.awsQueryCompatible = serviceSchema != null && serviceSchema.hasTrait(TraitKey.AWS_QUERY_COMPATIBLE_TRAIT);
        this.errorDeserializer = HttpErrorDeserializer.builder()
                                                      .codec(codec)
                                                      .protocolSettings(settings)
                                                      .headerErrorExtractor(AmznErrorHeaderExtractor.INSTANCE)
                                                      .errorPayloadParser(
                                                          HttpErrorDeserializer.DEFAULT_ERROR_PAYLOAD_PARSER)
                                                      .build();
    }

    /**
     * Returns the ID of the service the protocol was created for.
     *
     * @return the service shape ID
     */
    final ShapeId service() {
        return service;
    }

    @Override
    public final Codec payloadCodec() {
        return codec;
    }

    @Override
    protected final <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest createRequest(
        ApiOperation<I, O> operation, I input, ExecutionAttributes context, URI endpoint) {
        if (operation.inputEventBuilderSupplier() != null) {
            throw new UnsupportedOperationException("Event stream input is not supported by " + id());
        }
        String endpointPath = endpoint.getRawPath() == null ? "" : endpoint.getRawPath();
        SdkHttpFullRequest.Builder request =
            SdkHttpFullRequest.builder()
                              .method(SdkHttpMethod.POST)
                              .uri(endpoint)
                              .encodedPath(SdkHttpUtils.appendUri(endpointPath, REQUEST_PATH))
                              .putHeader(TARGET_HEADER, targetPrefix + operation.name());
        if (awsQueryCompatible) {
            request.putHeader(QUERY_MODE_HEADER, "true");
        }

        // Every input is a structure, so the body is at least "{}", including for smithy.api#Unit inputs.
        ByteBufferOutputStream body = newRequestBodyBuffer(operation);
        try (ShapeSerializer serializer = codec.createSerializer(body)) {
            input.serialize(serializer);
        }
        return request.putHeader(CONTENT_TYPE_HEADER, contentType)
                      .putHeader(CONTENT_LENGTH_HEADER, Integer.toString(body.size()))
                      .contentStreamProvider(finishRequestBody(operation, body))
                      .build();
    }

    @Override
    protected final <I extends SerializableStruct, O extends SerializableStruct> O deserializeOutput(
        ApiOperation<I, O> operation, ExecutionAttributes context, SdkHttpFullRequest request,
        SdkHttpFullResponse response) {
        if (operation.outputEventBuilderSupplier() != null) {
            throw new UnsupportedOperationException("Event stream output is not supported by " + id());
        }
        ShapeBuilder<O> builder = operation.outputBuilder();
        // Like earlier SDK clients, the body of an output without members is not read, so it need not be JSON. An empty
        // body is read as "{}": an output with no members set.
        if (!operation.outputSchema().members().isEmpty()) {
            readBody(builder, ResponseBodies.asByteBuffer(context, response));
        }
        builder.errorCorrection();
        return buildOutput(builder, response);
    }

    private void readBody(ShapeBuilder<?> builder, ByteBuffer body) {
        if (body.hasRemaining()) {
            // close() rejects content after the JSON value, as JsonCodec.deserializeShape does.
            try (ShapeDeserializer deserializer = codec.createDeserializer(body)) {
                builder.deserialize(deserializer);
            }
        }
    }

    @Override
    protected final SdkServiceException createError(ApiOperation<?, ?> operation, ExecutionAttributes context,
                                                    TypeRegistry errorRegistry, SdkHttpFullRequest request,
                                                    SdkHttpFullResponse response) {
        return errorDeserializer.createError(operation, context, errorRegistry, response);
    }
}
