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

import java.net.URI;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.client.http.HttpClientProtocol;
import software.amazon.awssdk.protocols.client.http.HttpErrorDeserializer;
import software.amazon.awssdk.protocols.client.http.ResponseBodies;
import software.amazon.awssdk.protocols.client.http.eventstream.AwsEventDecoderFactory;
import software.amazon.awssdk.protocols.client.http.eventstream.AwsEventEncoderFactory;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Shared base for the Smithy RPC v2 client protocols, which differ only in their codec and payload media type.
 *
 * <p>Requests follow the <a href="https://smithy.io/2.0/additional-specs/protocols/smithy-rpc-v2.html">Smithy RPC v2
 * protocol</a>:
 * <ul>
 *     <li>{@code POST /service/<service name>/operation/<operation name>}, appended to the endpoint path;</li>
 *     <li>{@code smithy-protocol: rpc-v2-<format>} and {@code Accept: <media type>} on every request;</li>
 *     <li>for an operation whose input is {@code smithy.api#Unit}, no body, no {@code Content-Type} and
 *     {@code Content-Length: 0}; otherwise the input serialized by the codec, with {@code Content-Type: <media type>}
 *     and {@code Content-Length};</li>
 *     <li>{@code x-amzn-query-mode: true} when the service has the {@code aws.protocols#awsQueryCompatible} trait.</li>
 * </ul>
 * HTTP binding traits are ignored: every input member is serialized into the body, and every output member is read from
 * it.
 *
 * <p>A 2xx response is deserialized from its body through the operation's output builder; an empty body produces an
 * empty output. Any other response is an error, whose discriminator is the body's {@code __type} member, or, for an
 * awsQueryCompatible service, first the code in the {@code x-amzn-query-error} header.
 *
 * <p>An event stream input is sent as {@code Content-Type: application/vnd.amazon.eventstream} and
 * {@code Transfer-Encoding: chunked}, without {@code Content-Length}, whose body is an {@code initial-request} message
 * holding the other input members, followed by the events the request pipeline encodes with
 * {@link #inputEventEncoderFactory}. An event stream output's members are read from the {@code initial-response}
 * message, the response body's first frame, by {@link #outputEventDecoderFactory}'s decoders; the body is not read
 * here. A streaming blob input or output fails with an {@code SdkClientException}.
 *
 * <p>Instances are immutable after construction, apart from the request body size hints, and safe to use from multiple
 * threads.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.rpcv2.AbstractRpcV2ClientProtocol}. It is created
 * from {@link ProtocolSettings} instead of the service ID, takes its codec as a constructor argument, and adds the
 * awsQueryCompatible headers.
 */
@SdkProtectedApi
@ThreadSafe
public abstract class AbstractRpcV2ClientProtocol extends HttpClientProtocol {
    private static final String SMITHY_PROTOCOL_HEADER = "smithy-protocol";
    private static final String SMITHY_PROTOCOL_PREFIX = "rpc-v2-";
    private static final String MEDIA_TYPE_PREFIX = "application/";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";
    private static final String QUERY_MODE_HEADER = "x-amzn-query-mode";

    private final ShapeId service;
    private final Codec codec;
    private final String payloadMediaType;
    private final String smithyProtocolValue;
    private final String targetPathPrefix;
    private final boolean awsQueryCompatible;
    private final HttpErrorDeserializer errorDeserializer;

    /**
     * Creates a protocol.
     *
     * @param protocolId the shape ID of the protocol definition trait
     * @param settings the settings of the service
     * @param codec the codec of request and response payloads
     * @param payloadMediaType the media type of request and response payloads, of the form
     *     {@code application/<format>}, for example {@code application/cbor}
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the media type does not start with {@code application/}
     */
    protected AbstractRpcV2ClientProtocol(ShapeId protocolId, ProtocolSettings settings, Codec codec,
                                          String payloadMediaType) {
        super(protocolId);
        Validate.paramNotNull(settings, "settings");
        this.service = settings.service();
        this.codec = Validate.paramNotNull(codec, "codec");
        this.payloadMediaType = Validate.paramNotNull(payloadMediaType, "payloadMediaType");
        Validate.isTrue(payloadMediaType.startsWith(MEDIA_TYPE_PREFIX)
                        && payloadMediaType.length() > MEDIA_TYPE_PREFIX.length(),
                        "payloadMediaType must be of the form application/<format>, but was %s", payloadMediaType);
        this.smithyProtocolValue = SMITHY_PROTOCOL_PREFIX + payloadMediaType.substring(MEDIA_TYPE_PREFIX.length());
        this.targetPathPrefix = "/service/" + service.getName() + "/operation/";
        Schema serviceSchema = settings.serviceSchema();
        this.awsQueryCompatible = serviceSchema != null && serviceSchema.hasTrait(TraitKey.AWS_QUERY_COMPATIBLE_TRAIT);
        this.errorDeserializer = HttpErrorDeserializer.builder()
                                                      .codec(codec)
                                                      .protocolSettings(settings)
                                                      .errorPayloadParser(
                                                          HttpErrorDeserializer.TYPE_ONLY_ERROR_PAYLOAD_PARSER)
                                                      .build();
    }

    /**
     * Returns the ID of the service the protocol was created for.
     *
     * @return the service shape ID
     */
    protected final ShapeId service() {
        return service;
    }

    @Override
    public final Codec payloadCodec() {
        return codec;
    }

    @Override
    protected <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest createRequest(
        ApiOperation<I, O> operation, I input, ExecutionAttributes context, URI endpoint) {
        if (isStreamingBlob(operation.inputStreamMember())) {
            // Smithy binds a streaming blob only through @httpPayload, which this protocol does not have (task 7.19).
            throw new UnsupportedOperationException("Streaming blob input is not supported by " + id());
        }
        String endpointPath = endpoint.getRawPath() == null ? "" : endpoint.getRawPath();
        SdkHttpFullRequest.Builder request =
            SdkHttpFullRequest.builder()
                              .method(SdkHttpMethod.POST)
                              .uri(endpoint)
                              .encodedPath(SdkHttpUtils.appendUri(endpointPath, targetPathPrefix + operation.name()))
                              .putHeader(SMITHY_PROTOCOL_HEADER, smithyProtocolValue)
                              .putHeader(ACCEPT_HEADER, payloadMediaType);
        if (awsQueryCompatible) {
            request.putHeader(QUERY_MODE_HEADER, "true");
        }
        if (isEventStream(operation.inputStreamMember())) {
            // The input's other members are the initial-request message, the body's first frame; the request pipeline
            // sends the encoded events after it.
            return putEventStreamHeaders(request).contentStreamProvider(initialRequestContent(operation, input)).build();
        }

        if (operation.inputSchema().hasTrait(TraitKey.UNIT_TYPE_TRAIT)) {
            return request.putHeader(CONTENT_LENGTH_HEADER, "0").build();
        }

        ByteBufferOutputStream body = newRequestBodyBuffer(operation);
        try (ShapeSerializer serializer = codec.createSerializer(body)) {
            input.serialize(serializer);
        }
        return request.putHeader(CONTENT_TYPE_HEADER, payloadMediaType)
                      .putHeader(CONTENT_LENGTH_HEADER, Integer.toString(body.size()))
                      .contentStreamProvider(finishRequestBody(operation, body))
                      .build();
    }

    @Override
    protected <I extends SerializableStruct, O extends SerializableStruct> O deserializeOutput(
        ApiOperation<I, O> operation, ExecutionAttributes context, SdkHttpFullRequest request,
        SdkHttpFullResponse response) {
        if (isStreamingBlob(operation.outputStreamMember())) {
            throw new UnsupportedOperationException("Streaming blob output is not supported by " + id());
        }
        ShapeBuilder<O> builder = operation.outputBuilder();
        if (isEventStream(operation.outputStreamMember())) {
            // The output's members are the initial-response message, the body's first frame, which the request pipeline
            // decodes with outputEventDecoderFactory; the body is left unread.
            builder.errorCorrection();
            return buildOutput(builder, response);
        }
        ByteBuffer body = ResponseBodies.asByteBuffer(context, response);
        if (body.hasRemaining()) {
            builder.deserialize(codec.createDeserializer(body));
        }
        builder.errorCorrection();
        return buildOutput(builder, response);
    }

    @Override
    public final AwsEventEncoderFactory inputEventEncoderFactory(ApiOperation<?, ?> operation) {
        return AwsEventEncoderFactory.forInputStream(operation, codec, payloadMediaType, true,
                                                     HttpClientProtocol::eventStreamInternalError);
    }

    @Override
    public final AwsEventDecoderFactory outputEventDecoderFactory(ApiOperation<?, ?> operation) {
        return AwsEventDecoderFactory.forOutputStream(operation, codec);
    }

    /**
     * Returns true: event stream inputs and outputs carry their other members in {@code initial-request} and
     * {@code initial-response} messages.
     *
     * @return true
     */
    @Override
    public final boolean sendsInitialEventMessages() {
        return true;
    }

    @Override
    protected SdkServiceException createError(ApiOperation<?, ?> operation, ExecutionAttributes context,
                                              TypeRegistry errorRegistry, SdkHttpFullRequest request,
                                              SdkHttpFullResponse response) {
        return errorDeserializer.createError(operation, context, errorRegistry, response);
    }
}
