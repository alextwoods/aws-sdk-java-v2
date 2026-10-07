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

package software.amazon.awssdk.protocols.smithyjava;

import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.MetricCollectingHttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolUtils;
import software.amazon.awssdk.protocols.smithyjava.internal.SerdeSupport;
import software.amazon.awssdk.protocols.smithyjava.internal.V2EpochSecondsFormatter;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.json.JsonCodec;

/**
 * awsJson 1.0 / 1.1 marshalling and unmarshalling for the stock v2 pipeline, done by smithy-java's
 * {@link JsonCodec}.
 *
 * <p>This replaces exactly two things in a v2 call: the {@code Marshaller} that turns the request POJO into
 * an {@link SdkHttpFullRequest}, and the success {@code HttpResponseHandler} that turns the HTTP response
 * into the response POJO. Endpoint resolution, signing, retries, interceptors, CRC32 validation, metrics and
 * error unmarshalling are untouched and still v2's. The generated v2 POJOs are themselves smithy
 * {@code SerializableStruct}s and their builders {@code ShapeBuilder}s, so the codec reads from and writes
 * into them directly, with no intermediate representation.
 *
 * <p>The request line and headers are the ones v2's {@code JsonProtocolMarshaller} produces for an awsJson
 * operation: {@code POST} to the client endpoint's path, {@code X-Amz-Target}, {@code Content-Type},
 * {@code Content-Length}, and {@code x-amzn-query-mode} for awsQuery-compatible services.
 */
@SdkProtectedApi
public final class SmithyAwsJsonProtocol {

    static {
        SmithyJsonDefaults.apply();
    }

    private static final OperationInfo OPERATION_INFO = OperationInfo.builder()
                                                                     .httpMethod(SdkHttpMethod.POST)
                                                                     .requestUri("/")
                                                                     .hasPayloadMembers(true)
                                                                     .build();

    private final JsonCodec codec;
    private final URI endpoint;
    private final String contentType;
    private final String targetPrefix;
    private final boolean hasAwsQueryCompatible;

    // Keyed by the generated ApiOperation singletons, so a call costs one identity-hash lookup and no allocation.
    private final Map<ApiOperation<?, ?>, Marshaller<?>> marshallers = new ConcurrentHashMap<>();
    private final Map<ApiOperation<?, ?>, HttpResponseHandler<?>> responseHandlers = new ConcurrentHashMap<>();

    private SmithyAwsJsonProtocol(Builder builder) {
        this.endpoint = SerdeSupport.clientEndpoint(Validate.paramNotNull(builder.clientConfiguration,
                                                                          "clientConfiguration"));
        this.contentType = Validate.paramNotNull(builder.contentType, "contentType");
        this.targetPrefix = builder.targetPrefix;
        this.hasAwsQueryCompatible = builder.hasAwsQueryCompatible;
        // useJsonName(true) is deliberate, and the opposite of what smithy-java's AwsJsonProtocol does. awsJson
        // puts the *modeled* member name on the wire, and in these schemas that name is not the member name:
        // SdkSchemaFactory names members after v2's Java-facing member name, which customizations rename
        // (DynamoDB's AttributeValue NULL is the member NUL), and records the wire name as @jsonName whenever
        // the two differ. Ignoring @jsonName sent {"NUL":true}, which DynamoDB rejects, and dropped NULL
        // from every response.
        this.codec = JsonCodec.builder()
                              .defaultNamespace(builder.serviceNamespace)
                              .useJsonName(true)
                              .useTimestampFormat(true)
                              .defaultTimestampFormat(V2EpochSecondsFormatter.INSTANCE)
                              .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The request marshaller for {@code operation}; what generated code hands to {@code withMarshaller}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <I extends SerializableStruct> Marshaller<I> marshaller(ApiOperation<I, ?> operation) {
        return (Marshaller<I>) marshallers.computeIfAbsent(operation, op -> new JsonMarshaller<>((ApiOperation) op));
    }

    /**
     * The success response handler for {@code operation}. Only consulted for 2xx responses; v2's error
     * response handler still owns everything else.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <O extends SerializableStruct> HttpResponseHandler<O> responseHandler(ApiOperation<?, O> operation) {
        return (HttpResponseHandler<O>) responseHandlers.computeIfAbsent(
            operation, op -> MetricCollectingHttpResponseHandler.create(CoreMetric.UNMARSHALLING_DURATION,
                                                                        new JsonResponseHandler<>((ApiOperation) op)));
    }

    private final class JsonMarshaller<I extends SerializableStruct> implements Marshaller<I> {
        private final String target;

        private JsonMarshaller(ApiOperation<I, ?> operation) {
            String operationName = operation.schema().id().getName();
            this.target = targetPrefix == null ? null : targetPrefix + "." + operationName;
        }

        @Override
        public SdkHttpFullRequest marshall(I input) {
            Validate.paramNotNull(input, "input");
            try {
                byte[] body = SerdeSupport.bytes(codec.serialize(input));
                SdkHttpFullRequest.Builder request = ProtocolUtils.createSdkHttpRequest(OPERATION_INFO, endpoint);
                if (target != null) {
                    request.putHeader("X-Amz-Target", target);
                }
                request.putHeader(CONTENT_LENGTH, Integer.toString(body.length));
                request.putHeader(CONTENT_TYPE, contentType);
                if (hasAwsQueryCompatible) {
                    request.putHeader("x-amzn-query-mode", "true");
                }
                request.contentStreamProvider(ContentStreamProvider.fromByteArrayUnsafe(body));
                return request.build();
            } catch (Exception e) {
                throw SdkClientException.builder()
                                        .message("Unable to marshall request to JSON: " + e.getMessage())
                                        .cause(e)
                                        .build();
            }
        }
    }

    private final class JsonResponseHandler<O extends SerializableStruct> implements HttpResponseHandler<O> {
        private final ApiOperation<?, O> operation;

        private JsonResponseHandler(ApiOperation<?, O> operation) {
            this.operation = operation;
        }

        @Override
        public O handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
            InputStream content = response.content().orElse(null);
            try {
                byte[] body = SerdeSupport.readAll(content, response);
                ShapeBuilder<O> builder = operation.outputBuilder();
                if (body.length > 0) {
                    try (ShapeDeserializer deserializer = codec.createDeserializer(body)) {
                        builder.deserialize(deserializer);
                    }
                }
                // Set on the builder rather than via toBuilder() on the result, which is what v2's
                // AwsJsonResponseHandler does: same metadata, one less copy of the response.
                if (builder instanceof AwsResponse.Builder) {
                    ((AwsResponse.Builder) builder).responseMetadata(SerdeSupport.responseMetadata(response));
                }
                return builder.build();
            } finally {
                if (content != null) {
                    IoUtils.closeQuietlyV2(content, null);
                }
            }
        }
    }

    public static final class Builder {
        private SdkClientConfiguration clientConfiguration;
        private String contentType;
        private String targetPrefix;
        private String serviceNamespace;
        private boolean hasAwsQueryCompatible;

        private Builder() {
        }

        public Builder clientConfiguration(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
            return this;
        }

        /** {@code application/x-amz-json-1.0} or {@code -1.1}, as v2's protocol factory would send. */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /** The awsJson target prefix, e.g. {@code DynamoDB_20120810}; {@code X-Amz-Target} is it plus the operation. */
        public Builder targetPrefix(String targetPrefix) {
            this.targetPrefix = targetPrefix;
            return this;
        }

        /** The namespace the codec resolves relative shape ids against, e.g. {@code com.amazonaws.dynamodb}. */
        public Builder serviceNamespace(String serviceNamespace) {
            this.serviceNamespace = serviceNamespace;
            return this;
        }

        public Builder hasAwsQueryCompatible(boolean hasAwsQueryCompatible) {
            this.hasAwsQueryCompatible = hasAwsQueryCompatible;
            return this;
        }

        public SmithyAwsJsonProtocol build() {
            return new SmithyAwsJsonProtocol(this);
        }
    }
}
