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
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.xml.XmlCodec;
import software.amazon.awssdk.codecs.xml.XmlSettings;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.client.awsquery.internal.QueryErrorPayloadParser;
import software.amazon.awssdk.protocols.client.awsquery.internal.QueryFormSerializer;
import software.amazon.awssdk.protocols.client.http.HttpClientProtocol;
import software.amazon.awssdk.protocols.client.http.HttpErrorDeserializer;
import software.amazon.awssdk.protocols.client.http.internal.MarshallBufferSizeTracker;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Shared base of the query protocols, {@code aws.protocols#awsQuery} and {@code aws.protocols#ec2Query}, which
 * differ in their form key rules, the wrapping of response bodies and the location of the error element.
 *
 * <p>Requests are {@code POST /}, appended to the endpoint path, with a form-encoded body written by
 * {@link QueryFormSerializer}: {@code Action=<operation name>&Version=<service version>} followed by the input
 * members, and the headers {@code Content-Type: application/x-www-form-urlencoded} and {@code Content-Length}. HTTP
 * binding traits are ignored.
 *
 * <p>A 2xx response is read with {@link XmlCodec} ({@link XmlSettings#AWS_QUERY_RESPONSE}) inside the protocol's
 * wrapper elements; the members of an output without members are not read. The request ID of the response metadata
 * comes from the body ({@code <ResponseMetadata><RequestId>}, or ec2Query's {@code <requestId>}), else from the
 * request ID headers. Any other response is an error, whose code is the {@code Code} element of the protocol's error
 * element, resolved through {@code awsQueryError} custom codes first.
 *
 * <p>{@link #payloadCodec()} returns null: the request and response formats differ, so there is no single payload
 * codec.
 *
 * <p>Instances are immutable after construction, apart from a cache of per-operation response codecs, and safe to use
 * from multiple threads.
 */
@SdkProtectedApi
@ThreadSafe
abstract class QueryClientProtocol extends HttpClientProtocol {
    private static final Logger log = Logger.loggerFor(QueryClientProtocol.class);

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String REQUEST_PATH = "/";
    private static final String NAMESPACE = "software.amazon.awssdk.protocols.client.awsquery";

    /**
     * The most operations whose response codecs are cached, so per-call operations cannot grow the cache without bound.
     */
    private static final int MAX_CACHED_OPERATIONS = 4096;

    private static final Schema RESPONSE_METADATA =
        Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, "ResponseMetadata"))
              .putMember("RequestId", PreludeSchemas.STRING)
              .build();

    /**
     * The children of an {@code <OpResponse>} that carry the request ID: awsQuery's {@code ResponseMetadata} and
     * ec2Query's {@code requestId}. Earlier SDK clients read both for both protocols.
     */
    private static final Schema ENVELOPE = Schema.structureBuilder(ShapeId.fromParts(NAMESPACE, "Envelope"))
                                                 .putMember("ResponseMetadata", RESPONSE_METADATA)
                                                 .putMember("requestId", PreludeSchemas.STRING)
                                                 .build();

    private static final ShapeDeserializer.StructMemberConsumer<String[]> RESPONSE_METADATA_MEMBERS =
        (requestId, member, deserializer) -> {
            String value = deserializer.readString(member);
            if (requestId[0] == null) {
                requestId[0] = value;
            }
        };

    private static final ShapeDeserializer.StructMemberConsumer<String[]> ENVELOPE_MEMBERS =
        (requestId, member, deserializer) -> {
            if ("requestId".equals(member.memberName())) {
                // Read after ResponseMetadata by earlier SDK clients, so it wins.
                requestId[1] = deserializer.readString(member);
            } else {
                deserializer.readStruct(member, requestId, RESPONSE_METADATA_MEMBERS);
            }
        };

    private final ShapeId service;
    private final String version;
    private final boolean ec2;
    private final HttpErrorDeserializer errorDeserializer;
    private final ConcurrentMap<String, ResponseCodecs> responseCodecs = new ConcurrentHashMap<>();
    private final MarshallBufferSizeTracker formBodySizeHints = new MarshallBufferSizeTracker();

    /**
     * Creates a protocol.
     *
     * @param protocolId the shape ID of the protocol definition trait
     * @param settings the settings of the service; its version is the {@code Version} of every request
     * @param ec2 whether this is the ec2Query protocol
     * @throws NullPointerException if {@code settings} or its service version is null
     */
    QueryClientProtocol(ShapeId protocolId, ProtocolSettings settings, boolean ec2) {
        super(protocolId);
        Validate.paramNotNull(settings, "settings");
        this.service = settings.service();
        this.version = Validate.notNull(settings.serviceVersion(),
                                        "serviceVersion is a required protocol setting for %s", protocolId);
        this.ec2 = ec2;
        // Reads modeled error members: the codec unwraps <ErrorResponse><Error> and <Response><Errors><Error>.
        XmlCodec errorCodec = XmlCodec.builder()
                                      .settings(XmlSettings.AWS_QUERY_RESPONSE)
                                      .strictRootElement(false)
                                      .build();
        this.errorDeserializer = HttpErrorDeserializer.builder()
                                                      .codec(errorCodec)
                                                      .protocolSettings(settings)
                                                      .useErrorCodeIndex(true)
                                                      .errorPayloadParser(ec2 ? QueryErrorPayloadParser.EC2_QUERY
                                                                              : QueryErrorPayloadParser.AWS_QUERY)
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

    /**
     * Returns null: requests are form-encoded and responses are XML, so the query protocols have no payload codec.
     *
     * @return null
     */
    @Override
    public final Codec payloadCodec() {
        return null;
    }

    @Override
    protected final <I extends SerializableStruct, O extends SerializableStruct> SdkHttpFullRequest createRequest(
        ApiOperation<I, O> operation, I input, ExecutionAttributes context, URI endpoint) {
        if (operation.inputStreamMember() != null) {
            // The query protocols have no HTTP bindings, so no streaming blob or event stream member (task 7.19).
            throw new UnsupportedOperationException("Streaming input is not supported by " + id());
        }
        // The body array starts at the capacity the operation's last body needed, so a body of a similar size is
        // written without growing the array. That capacity includes the worst-case reservation the serializer makes
        // for each value, which the body size alone does not cover.
        int capacity = formBodySizeHints.initialCapacity(operation);
        QueryFormSerializer serializer = ec2 ? QueryFormSerializer.forEc2Query(operation.name(), version, capacity)
                                             : QueryFormSerializer.forAwsQuery(operation.name(), version, capacity);
        if (!operation.inputSchema().hasTrait(TraitKey.UNIT_TYPE_TRAIT)) {
            input.serializeMembers(serializer);
        }
        int size = serializer.size();
        formBodySizeHints.record(operation, serializer.requiredCapacity());
        byte[] body = serializer.finishArray();
        String endpointPath = endpoint.getRawPath() == null ? "" : endpoint.getRawPath();
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.POST)
                                 .uri(endpoint)
                                 .encodedPath(SdkHttpUtils.appendUri(endpointPath, REQUEST_PATH))
                                 .putHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE)
                                 .putHeader(CONTENT_LENGTH_HEADER, Integer.toString(size))
                                 .contentStreamProvider(new FormBody(body, size))
                                 .build();
    }

    @Override
    protected final <I extends SerializableStruct, O extends SerializableStruct> O deserializeOutput(
        ApiOperation<I, O> operation, ExecutionAttributes context, SdkHttpFullRequest request,
        SdkHttpFullResponse response) {
        if (operation.outputStreamMember() != null) {
            throw new UnsupportedOperationException("Streaming output is not supported by " + id());
        }
        ShapeBuilder<O> builder = operation.outputBuilder();
        ByteBuffer body = readResponseBody(operation, context, response);
        String requestId = null;
        if (body.hasRemaining()) {
            ResponseCodecs codecs = responseCodecs(operation);
            // As in earlier SDK clients, an output without members (a Unit output included) reads only the response
            // metadata from the body.
            if (!operation.outputSchema().members().isEmpty()) {
                try (ShapeDeserializer deserializer = codecs.output.createDeserializer(body.duplicate())) {
                    builder.deserialize(deserializer);
                }
            }
            requestId = readRequestId(operation, codecs.envelope, body);
        }
        builder.errorCorrection();
        return buildOutput(builder, response, requestId);
    }

    @Override
    protected final SdkServiceException createError(ApiOperation<?, ?> operation, ExecutionAttributes context,
                                                    TypeRegistry errorRegistry, SdkHttpFullRequest request,
                                                    SdkHttpFullResponse response) {
        return errorDeserializer.createError(operation, context, errorRegistry, response);
    }

    /**
     * Reads the request ID of a success body. It is metadata only, so a body whose envelope cannot be read gives null
     * (the request ID headers are used) instead of failing the call.
     */
    private static String readRequestId(ApiOperation<?, ?> operation, XmlCodec envelope, ByteBuffer body) {
        String[] requestId = new String[2];
        try (ShapeDeserializer deserializer = envelope.createDeserializer(body.duplicate())) {
            deserializer.readStruct(ENVELOPE, requestId, ENVELOPE_MEMBERS);
        } catch (RuntimeException e) {
            log.debug(() -> "Unable to read the response metadata of " + operation.name()
                            + "; the request ID is read from the response headers.", e);
            return null;
        }
        return requestId[1] != null ? requestId[1] : requestId[0];
    }

    private ResponseCodecs responseCodecs(ApiOperation<?, ?> operation) {
        String name = operation.name();
        ResponseCodecs codecs = responseCodecs.get(name);
        if (codecs != null) {
            return codecs;
        }
        if (responseCodecs.size() >= MAX_CACHED_OPERATIONS) {
            return new ResponseCodecs(name, ec2);
        }
        return responseCodecs.computeIfAbsent(name, n -> new ResponseCodecs(n, ec2));
    }

    /**
     * The response codecs of one operation: {@code output} reads the output members inside
     * {@code <OpResponse><OpResult>} (awsQuery) or {@code <OpResponse>} (ec2Query), and {@code envelope} reads the
     * children of {@code <OpResponse>}.
     */
    private static final class ResponseCodecs {
        private final XmlCodec output;
        private final XmlCodec envelope;

        private ResponseCodecs(String operationName, boolean ec2) {
            List<String> responseWrapper = Collections.singletonList(operationName + "Response");
            this.envelope = responseCodec(responseWrapper);
            this.output = ec2 ? envelope
                              : responseCodec(Arrays.asList(operationName + "Response", operationName + "Result"));
        }

        private static XmlCodec responseCodec(List<String> wrapperElements) {
            return XmlCodec.builder()
                           .settings(XmlSettings.AWS_QUERY_RESPONSE)
                           .wrapperElements(wrapperElements)
                           .strictRootElement(false)
                           .build();
        }
    }

    /**
     * A form body that shares the serializer's array: {@link #contentAsByteBufferOrNull()} gives HTTP clients the bytes
     * without a copy.
     */
    private static final class FormBody implements ContentStreamProvider {
        private final byte[] bytes;
        private final int length;

        private FormBody(byte[] bytes, int length) {
            this.bytes = bytes;
            this.length = length;
        }

        @Override
        public InputStream newStream() {
            return new ByteArrayInputStream(bytes, 0, length);
        }

        @Override
        public ByteBuffer contentAsByteBufferOrNull() {
            return ByteBuffer.wrap(bytes, 0, length);
        }

        @Override
        public String name() {
            return ProviderType.BYTES.getName();
        }
    }
}
