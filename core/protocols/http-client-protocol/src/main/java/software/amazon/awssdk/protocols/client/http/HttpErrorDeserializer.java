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

package software.amazon.awssdk.protocols.client.http;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Turns an HTTP error response into the exception to throw: the modeled exception from the error registry when the
 * response's error discriminator resolves to one, and otherwise the service's base exception.
 *
 * <p>{@link #createError} never fails with a deserialization error. A body that cannot be read or parsed, a
 * discriminator that is missing or unknown, and a modeled error whose members cannot be deserialized all produce the
 * base exception, carrying the status code, request IDs and whatever error code and message could be read.
 *
 * <p>The discriminator is taken from the first of these that is present:
 * <ol>
 *     <li>the code part of the {@code x-amzn-query-error} header, when {@link Builder#awsQueryCompatible(boolean)} is
 *     set. If it does not resolve, the next sources are tried;</li>
 *     <li>the {@linkplain Builder#headerErrorExtractor(HeaderErrorExtractor) header extractor}, for example
 *     {@code X-Amzn-Errortype};</li>
 *     <li>the {@linkplain Builder#errorPayloadParser(ErrorPayloadParser) payload parser}, for example {@code __type}
 *     then {@code code}.</li>
 * </ol>
 * It is resolved with {@link ErrorTypeUtils#resolve}, looking up {@code awsQueryError} custom codes first when
 * {@link Builder#useErrorCodeIndex(boolean)} or {@code awsQueryCompatible} is set.
 *
 * <p>The exception is populated the way earlier SDK clients populate it: the message (the header extractor's message,
 * then the payload parser's, then a modeled {@code message} member), the status code, the request ID (from the payload
 * parser, then {@code x-amzn-RequestId}, then {@code x-amz-request-id}), the extended request ID ({@code x-amz-id-2})
 * and, for an {@link AwsServiceException}, the {@link AwsErrorDetails} (error code, message, service name, raw body and
 * HTTP response) and the clock skew from {@link SdkExecutionAttribute#TIME_OFFSET}.
 *
 * <p>Instances are immutable and safe to use from multiple threads.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.http.HttpErrorDeserializer}. It returns SDK
 * exceptions instead of {@code CallException}s, falls back to the base exception from
 * {@link ProtocolSettings#baseExceptionBuilder()} instead of an unknown-error factory, and its extension points read
 * plain values: {@link HeaderErrorExtractor} returns the raw discriminator, {@link ErrorPayloadParser} returns an
 * {@link ErrorPayload}, and {@link KnownErrorFactory} populates the registry's builder instead of building it.
 */
@SdkPublicApi
@ThreadSafe
public final class HttpErrorDeserializer {
    /**
     * Parses the body with the codec into a document and reads the error type from {@code __type}, then {@code code},
     * and the message from {@code message}, {@code Message}, then {@code errorMessage}. This is the parser for the
     * awsJson1_0, awsJson1_1 and restJson1 protocols, and the default.
     */
    public static final ErrorPayloadParser DEFAULT_ERROR_PAYLOAD_PARSER = new DocumentErrorPayloadParser(true);

    /**
     * Like {@link #DEFAULT_ERROR_PAYLOAD_PARSER} but reads the error type only from {@code __type}, as the rpcv2Cbor
     * protocol requires.
     */
    public static final ErrorPayloadParser TYPE_ONLY_ERROR_PAYLOAD_PARSER = new DocumentErrorPayloadParser(false);

    /**
     * Deserializes a non-empty body with the codec into the modeled error's builder, without HTTP bindings. This is the
     * default; REST protocols that bind error members to headers use their own factory.
     */
    public static final KnownErrorFactory DEFAULT_KNOWN_ERROR_FACTORY = new KnownErrorFactory() {
        @Override
        public void populateError(ExecutionAttributes context, Codec codec, SdkHttpFullResponse response, ByteBuffer body,
                                  ShapeBuilder<?> builder) {
            if (body.hasRemaining()) {
                builder.deserialize(codec.createDeserializer(body));
            }
        }

        @Override
        public String toString() {
            return "PayloadKnownErrorFactory";
        }
    };

    private static final Logger log = Logger.loggerFor(HttpErrorDeserializer.class);

    private static final String QUERY_ERROR_HEADER = "x-amzn-query-error";
    private static final String REQUEST_ID_HEADER = "x-amzn-RequestId";
    private static final String REQUEST_ID_HEADER_ALTERNATE = "x-amz-request-id";
    private static final String EXTENDED_REQUEST_ID_HEADER = "x-amz-id-2";

    /**
     * Reads no headers.
     */
    private static final HeaderErrorExtractor NO_HEADERS = new HeaderErrorExtractor() {
        @Override
        public String errorType(SdkHttpFullResponse response) {
            return null;
        }

        @Override
        public String toString() {
            return "NoHeaderErrorExtractor";
        }
    };

    private final Codec codec;
    private final ShapeId serviceId;
    private final Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder;
    private final HeaderErrorExtractor headerErrorExtractor;
    private final ErrorPayloadParser errorPayloadParser;
    private final KnownErrorFactory knownErrorFactory;
    private final boolean awsQueryCompatible;
    private final boolean useErrorCodeIndex;

    private HttpErrorDeserializer(Builder builder) {
        this.codec = Validate.paramNotNull(builder.codec, "codec");
        this.serviceId = Validate.paramNotNull(builder.serviceId, "serviceId");
        this.baseExceptionBuilder = builder.baseExceptionBuilder;
        this.headerErrorExtractor = builder.headerErrorExtractor;
        this.errorPayloadParser = builder.errorPayloadParser;
        this.knownErrorFactory = builder.knownErrorFactory;
        this.awsQueryCompatible = builder.awsQueryCompatible;
        this.useErrorCodeIndex = builder.useErrorCodeIndex || builder.awsQueryCompatible;
    }

    /**
     * Creates a builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates the exception for an error response. The response body is read with
     * {@link ResponseBodies#asByteBuffer(ExecutionAttributes, SdkHttpFullResponse)}.
     *
     * @param operation the operation that was called
     * @param context the attributes of the call
     * @param registry the registry used to create modeled errors; may be null, in which case the base exception is
     *     always used
     * @param response the error response
     * @return the modeled exception or the service's base exception, never null; the caller throws it
     */
    public SdkServiceException createError(ApiOperation<?, ?> operation, ExecutionAttributes context, TypeRegistry registry,
                                           SdkHttpFullResponse response) {
        ByteBuffer body = readBody(operation, context, response);

        ErrorPayload payload = null;
        if (body != null && body.hasRemaining()) {
            try {
                payload = errorPayloadParser.parse(codec, response, body.duplicate());
            } catch (RuntimeException e) {
                log.debug(() -> "Unable to parse the error response body of " + operation.name()
                                + "; the error type and message are read from headers only.", e);
            }
        }

        String queryErrorCode = awsQueryCompatible
                                ? ErrorTypeUtils.queryErrorCode(response.firstMatchingHeader(QUERY_ERROR_HEADER)
                                                                        .orElse(null))
                                : null;
        String protocolErrorType = headerErrorExtractor.errorType(response);
        if (protocolErrorType == null && payload != null) {
            protocolErrorType = payload.errorType();
        }

        String errorCode = queryErrorCode != null ? queryErrorCode : ErrorTypeUtils.removeNamespaceAndUri(protocolErrorType);
        String message = headerErrorExtractor.errorMessage(response);
        if (message == null && payload != null) {
            message = payload.message();
        }
        ErrorFields fields = new ErrorFields(errorCode, message, payload != null ? payload.requestId() : null, body);

        String namespace = serviceId.getNamespace();
        ShapeId id = ErrorTypeUtils.resolve(queryErrorCode, namespace, registry, useErrorCodeIndex);
        if (id == null) {
            id = ErrorTypeUtils.resolve(protocolErrorType, namespace, registry, useErrorCodeIndex);
        }
        if (id != null) {
            SdkServiceException modeled = createModeledError(operation, context, registry, id, response, fields);
            if (modeled != null) {
                return modeled;
            }
        }
        return populate(baseExceptionBuilder.get(), context, response, fields).build();
    }

    private ByteBuffer readBody(ApiOperation<?, ?> operation, ExecutionAttributes context, SdkHttpFullResponse response) {
        try {
            return ResponseBodies.asByteBuffer(context, response);
        } catch (RuntimeException e) {
            log.debug(() -> "Unable to read the error response body of " + operation.name()
                            + "; the error type and message are read from headers only.", e);
            return null;
        }
    }

    /**
     * Creates the modeled error, or returns null if the registry's builder is not an exception builder, or anything
     * about deserializing or building it fails.
     */
    private SdkServiceException createModeledError(ApiOperation<?, ?> operation, ExecutionAttributes context,
                                                   TypeRegistry registry, ShapeId id, SdkHttpFullResponse response,
                                                   ErrorFields fields) {
        try {
            ShapeBuilder<?> builder = registry.createBuilder(id);
            if (!(builder instanceof SdkServiceException.Builder)) {
                log.debug(() -> "The error registry builder for " + id + " does not build an SdkServiceException; "
                                + "using the base exception for " + operation.name() + ".");
                return null;
            }
            ByteBuffer body = fields.body != null ? fields.body.duplicate() : ByteBuffer.wrap(new byte[0]);
            knownErrorFactory.populateError(context, codec, response, body, builder);
            builder.errorCorrection();
            return populate((SdkServiceException.Builder) builder, context, response, fields).build();
        } catch (RuntimeException e) {
            log.debug(() -> "Unable to deserialize the modeled error " + id + " of " + operation.name()
                            + "; using the base exception.", e);
            return null;
        }
    }

    private SdkServiceException.Builder populate(SdkServiceException.Builder builder, ExecutionAttributes context,
                                                 SdkHttpFullResponse response, ErrorFields fields) {
        String message = fields.message != null ? fields.message : builder.message();
        builder.message(message);
        builder.statusCode(response.statusCode());
        builder.requestId(fields.requestId != null ? fields.requestId : headerRequestId(response));
        builder.extendedRequestId(response.firstMatchingHeader(EXTENDED_REQUEST_ID_HEADER).orElse(null));

        if (builder instanceof AwsServiceException.Builder) {
            AwsServiceException.Builder awsBuilder = (AwsServiceException.Builder) builder;
            AwsErrorDetails.Builder details = AwsErrorDetails.builder()
                                                             .errorCode(fields.errorCode)
                                                             .errorMessage(message)
                                                             .sdkHttpResponse(response);
            if (context != null) {
                details.serviceName(context.getAttribute(SdkExecutionAttribute.SERVICE_NAME));
                Integer timeOffset = context.getAttribute(SdkExecutionAttribute.TIME_OFFSET);
                if (timeOffset != null) {
                    awsBuilder.clockSkew(Duration.ofSeconds(timeOffset));
                }
            }
            if (fields.body != null && fields.body.hasRemaining()) {
                details.rawResponse(rawBytes(fields.body));
            }
            awsBuilder.awsErrorDetails(details.build());
        } else if (message == null && fields.errorCode != null) {
            // A plain SdkServiceException has no error code field; keep the code in the message.
            builder.message("Service returned error code " + fields.errorCode);
        }
        return builder;
    }

    private static String headerRequestId(SdkHttpFullResponse response) {
        String requestId = response.firstMatchingHeader(REQUEST_ID_HEADER).orElse(null);
        return requestId != null ? requestId : response.firstMatchingHeader(REQUEST_ID_HEADER_ALTERNATE).orElse(null);
    }

    private static SdkBytes rawBytes(ByteBuffer body) {
        if (body.hasArray() && body.arrayOffset() == 0 && body.position() == 0 && body.remaining() == body.array().length) {
            return SdkBytes.fromByteArrayUnsafe(body.array());
        }
        return SdkBytes.fromByteBuffer(body.duplicate());
    }

    @Override
    public String toString() {
        return ToString.builder("HttpErrorDeserializer")
                       .add("serviceId", serviceId)
                       .add("codec", codec)
                       .add("headerErrorExtractor", headerErrorExtractor)
                       .add("errorPayloadParser", errorPayloadParser)
                       .add("knownErrorFactory", knownErrorFactory)
                       .add("awsQueryCompatible", awsQueryCompatible)
                       .add("useErrorCodeIndex", useErrorCodeIndex)
                       .build();
    }

    /**
     * The values read from an error response that every exception receives.
     */
    private static final class ErrorFields {
        private final String errorCode;
        private final String message;
        private final String requestId;
        private final ByteBuffer body;

        private ErrorFields(String errorCode, String message, String requestId, ByteBuffer body) {
            this.errorCode = errorCode;
            this.message = message;
            this.requestId = requestId;
            this.body = body;
        }
    }

    /**
     * Reads the error discriminator, and optionally the message, from the headers of an error response. The headers are
     * checked before the payload.
     *
     * <p>Implementations must be safe to use from multiple threads.
     */
    public interface HeaderErrorExtractor {
        /**
         * Returns the raw error discriminator from the response headers.
         *
         * @param response the error response
         * @return the raw value, as sent (it is normalized by {@link ErrorTypeUtils#resolve}), or null if absent
         */
        String errorType(SdkHttpFullResponse response);

        /**
         * Returns the error message from the response headers. A header message takes precedence over one in the
         * payload. The default returns null.
         *
         * @param response the error response
         * @return the message, or null if absent
         */
        default String errorMessage(SdkHttpFullResponse response) {
            return null;
        }
    }

    /**
     * Reads the error discriminator, message and request ID from the body of an error response.
     *
     * <p>Implementations must be safe to use from multiple threads. They may throw any runtime exception for a body they
     * cannot parse; {@link HttpErrorDeserializer} then continues without payload values.
     */
    @FunctionalInterface
    public interface ErrorPayloadParser {
        /**
         * Parses an error body.
         *
         * @param codec the codec of the protocol
         * @param response the error response
         * @param body the non-empty body; the parser may consume it
         * @return the values read, or null if none could be read
         */
        ErrorPayload parse(Codec codec, SdkHttpFullResponse response, ByteBuffer body);
    }

    /**
     * Populates the builder of a modeled error from an error response.
     *
     * <p>Implementations must be safe to use from multiple threads. They may throw any runtime exception for a response
     * they cannot deserialize; {@link HttpErrorDeserializer} then discards the builder and uses the base exception.
     */
    @FunctionalInterface
    public interface KnownErrorFactory {
        /**
         * Deserializes the modeled members of an error into its builder. The message, status code, request IDs and
         * error details are set afterwards by {@link HttpErrorDeserializer}.
         *
         * @param context the attributes of the call
         * @param codec the codec of the protocol
         * @param response the error response
         * @param body the body, possibly empty; the factory may consume it
         * @param builder the builder from the error registry, which also implements {@link SdkServiceException.Builder}
         */
        void populateError(ExecutionAttributes context, Codec codec, SdkHttpFullResponse response, ByteBuffer body,
                           ShapeBuilder<?> builder);
    }

    /**
     * The values an {@link ErrorPayloadParser} read from an error body. Each may be null.
     */
    public static final class ErrorPayload {
        private final String errorType;
        private final String message;
        private final String requestId;

        private ErrorPayload(String errorType, String message, String requestId) {
            this.errorType = errorType;
            this.message = message;
            this.requestId = requestId;
        }

        /**
         * Creates payload values.
         *
         * @param errorType the raw error discriminator, for example the value of {@code __type}; may be null
         * @param message the error message; may be null
         * @param requestId the request ID, for protocols that send it in the body; may be null
         * @return the values
         */
        public static ErrorPayload create(String errorType, String message, String requestId) {
            return new ErrorPayload(errorType, message, requestId);
        }

        /**
         * Returns the raw error discriminator.
         *
         * @return the discriminator, or null
         */
        public String errorType() {
            return errorType;
        }

        /**
         * Returns the error message.
         *
         * @return the message, or null
         */
        public String message() {
            return message;
        }

        /**
         * Returns the request ID read from the body. It takes precedence over the request ID headers.
         *
         * @return the request ID, or null
         */
        public String requestId() {
            return requestId;
        }

        @Override
        public String toString() {
            return ToString.builder("ErrorPayload")
                           .add("errorType", errorType)
                           .add("message", message)
                           .add("requestId", requestId)
                           .build();
        }
    }

    /**
     * Builds an {@link HttpErrorDeserializer}. A builder is not safe for concurrent use.
     */
    public static final class Builder {
        private Codec codec;
        private ShapeId serviceId;
        private Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder = AwsServiceException::builder;
        private HeaderErrorExtractor headerErrorExtractor = NO_HEADERS;
        private ErrorPayloadParser errorPayloadParser = DEFAULT_ERROR_PAYLOAD_PARSER;
        private KnownErrorFactory knownErrorFactory = DEFAULT_KNOWN_ERROR_FACTORY;
        private boolean awsQueryCompatible;
        private boolean useErrorCodeIndex;

        private Builder() {
        }

        /**
         * Sets the codec used to parse error bodies. Required.
         *
         * @param codec the codec
         * @return this builder
         */
        public Builder codec(Codec codec) {
            this.codec = Validate.paramNotNull(codec, "codec");
            return this;
        }

        /**
         * Sets the shape ID of the service, whose namespace resolves relative error discriminators. Required.
         *
         * @param serviceId the service shape ID
         * @return this builder
         */
        public Builder serviceId(ShapeId serviceId) {
            this.serviceId = Validate.paramNotNull(serviceId, "serviceId");
            return this;
        }

        /**
         * Sets the supplier of builders for the service's base exception, used when no modeled error applies. Defaults
         * to {@code AwsServiceException::builder}.
         *
         * @param baseExceptionBuilder the supplier, for example {@code SqsException::builder}
         * @return this builder
         */
        public Builder baseExceptionBuilder(Supplier<? extends SdkServiceException.Builder> baseExceptionBuilder) {
            this.baseExceptionBuilder = Validate.paramNotNull(baseExceptionBuilder, "baseExceptionBuilder");
            return this;
        }

        /**
         * Sets the service ID, the base exception supplier and, from the
         * {@code aws.protocols#awsQueryCompatible} trait of the service schema when there is one,
         * {@link #awsQueryCompatible(boolean)}.
         *
         * @param settings the settings the protocol was created with
         * @return this builder
         */
        public Builder protocolSettings(ProtocolSettings settings) {
            Validate.paramNotNull(settings, "settings");
            serviceId(settings.service());
            baseExceptionBuilder(settings.baseExceptionBuilder());
            Schema serviceSchema = settings.serviceSchema();
            awsQueryCompatible(serviceSchema != null && serviceSchema.hasTrait(TraitKey.AWS_QUERY_COMPATIBLE_TRAIT));
            return this;
        }

        /**
         * Sets how the error discriminator and message are read from headers. Defaults to reading no headers.
         *
         * @param headerErrorExtractor the extractor, for example {@link AmznErrorHeaderExtractor#INSTANCE}
         * @return this builder
         */
        public Builder headerErrorExtractor(HeaderErrorExtractor headerErrorExtractor) {
            this.headerErrorExtractor = Validate.paramNotNull(headerErrorExtractor, "headerErrorExtractor");
            return this;
        }

        /**
         * Sets how the error discriminator, message and request ID are read from the body. Defaults to
         * {@link #DEFAULT_ERROR_PAYLOAD_PARSER}.
         *
         * @param errorPayloadParser the parser
         * @return this builder
         */
        public Builder errorPayloadParser(ErrorPayloadParser errorPayloadParser) {
            this.errorPayloadParser = Validate.paramNotNull(errorPayloadParser, "errorPayloadParser");
            return this;
        }

        /**
         * Sets how a modeled error's members are deserialized. Defaults to {@link #DEFAULT_KNOWN_ERROR_FACTORY}.
         *
         * @param knownErrorFactory the factory
         * @return this builder
         */
        public Builder knownErrorFactory(KnownErrorFactory knownErrorFactory) {
            this.knownErrorFactory = Validate.paramNotNull(knownErrorFactory, "knownErrorFactory");
            return this;
        }

        /**
         * Sets whether the service has the {@code aws.protocols#awsQueryCompatible} trait. When true, the code in the
         * {@code x-amzn-query-error} header is the reported error code and is resolved first, and discriminators are
         * looked up as {@code awsQueryError} custom codes first. Defaults to false.
         *
         * @param awsQueryCompatible whether the service is awsQuery-compatible
         * @return this builder
         */
        public Builder awsQueryCompatible(boolean awsQueryCompatible) {
            this.awsQueryCompatible = awsQueryCompatible;
            return this;
        }

        /**
         * Sets whether discriminators are looked up as {@code awsQueryError} custom codes, with
         * {@link ErrorRegistry#resolveCode(String)}, before they are resolved as shape IDs. The awsQuery and ec2Query
         * protocols set it. Defaults to false.
         *
         * @param useErrorCodeIndex whether to resolve custom error codes first
         * @return this builder
         */
        public Builder useErrorCodeIndex(boolean useErrorCodeIndex) {
            this.useErrorCodeIndex = useErrorCodeIndex;
            return this;
        }

        /**
         * Builds the deserializer.
         *
         * @return the deserializer
         * @throws NullPointerException if the codec or the service ID is not set
         */
        public HttpErrorDeserializer build() {
            return new HttpErrorDeserializer(this);
        }
    }

    /**
     * Reads the error body into a document with the codec.
     */
    private static final class DocumentErrorPayloadParser implements ErrorPayloadParser {
        private final boolean readCode;

        private DocumentErrorPayloadParser(boolean readCode) {
            this.readCode = readCode;
        }

        @Override
        public ErrorPayload parse(Codec codec, SdkHttpFullResponse response, ByteBuffer body) {
            Document document = codec.createDeserializer(body).readDocument();
            String errorType = readCode ? ErrorTypeUtils.readTypeAndCode(document) : ErrorTypeUtils.readType(document);
            return ErrorPayload.create(errorType, ErrorTypeUtils.readMessage(document), null);
        }

        @Override
        public String toString() {
            return readCode ? "DefaultErrorPayloadParser" : "TypeOnlyErrorPayloadParser";
        }
    }
}
