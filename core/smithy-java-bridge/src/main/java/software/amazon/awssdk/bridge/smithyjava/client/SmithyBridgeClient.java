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

package software.amazon.awssdk.bridge.smithyjava.client;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.bridge.smithyjava.error.V2ModeledError;
import software.amazon.awssdk.bridge.smithyjava.error.V2RetryableError;
import software.amazon.awssdk.bridge.smithyjava.error.V2UnmodeledError;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.smithy.java.client.core.Client;
import software.amazon.smithy.java.client.core.error.TransportException;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * The smithy-java client that a generated AWS SDK v2 client delegates to.
 *
 * <p>Generated v2 clients hold one of these, built by {@link V2ConfigTranslator} from the v2
 * {@code SdkClientConfiguration}, and call {@link #invoke(SerializableStruct, ApiOperation)} once per
 * operation. Everything between that call and the wire — serialization, endpoint resolution, auth,
 * signing, retries, deserialization — is smithy-java's.
 *
 * <p>This class exists for two reasons that {@link Client} alone does not cover:
 *
 * <ol>
 *   <li>{@code Client#call} is {@code protected}, so a subclass is required to expose it.</li>
 *   <li>Exceptions crossing this boundary must look like v2 exceptions. smithy-java throws
 *       {@link CallException} subtypes; v2 callers catch {@code DynamoDbException},
 *       {@code AwsServiceException}, and {@code SdkClientException}. Translating here — rather than in
 *       an interceptor or an {@code ExceptionMapperPlugin} — keeps the translation off the hot path
 *       entirely and keeps this class free of service-specific types.</li>
 * </ol>
 *
 * @see V2ModeledError for how modeled errors survive the round trip as real v2 exception instances
 */
@SdkProtectedApi
public final class SmithyBridgeClient extends Client {

    private final Supplier<? extends AwsServiceException.Builder> baseExceptionBuilder;

    private SmithyBridgeClient(Builder builder) {
        super(builder);
        this.baseExceptionBuilder = builder.baseExceptionBuilder;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Invokes an operation and translates any failure into the v2 exception a caller expects.
     *
     * @param input     operation input; also a v2 {@code SdkRequest}.
     * @param operation the generated operation.
     * @param <I>       input shape.
     * @param <O>       output shape.
     * @return the deserialized output, which is also a v2 {@code SdkResponse}.
     */
    public <I extends SerializableStruct, O extends SerializableStruct> O invoke(I input, ApiOperation<I, O> operation) {
        try {
            return call(input, operation, null);
        } catch (V2ModeledError e) {
            // A modeled error: the real v2 exception was built by the generated builder and is carried
            // inside the shim. Unwrap and throw it, so `catch (ConditionalCheckFailedException e)` works.
            throw e.v2Exception();
        } catch (V2UnmodeledError e) {
            // An error response that matched no modeled shape. V2ErrorEnricher already built the
            // service's base exception from the HTTP response, so this is the same unwrap.
            throw e.v2Exception();
        } catch (V2RetryableError e) {
            // A carrier the enricher substituted purely so the retry strategy could see a
            // classification the real failure's type refuses to report. Retries are over by now, so
            // the carrier has done its job and the caller should see the failure it wrapped.
            throw translate(e.original());
        } catch (CallException e) {
            throw translate(e);
        }
    }

    private RuntimeException translate(RuntimeException e) {
        // Anything the bridge itself threw (transport, credentials, endpoints) already is an
        // SdkException; preserve it rather than re-wrapping so v2 error handling sees the real type.
        for (Throwable cause = e.getCause(); cause != null && cause.getCause() != cause; cause = cause.getCause()) {
            if (cause instanceof SdkException sdkException) {
                return sdkException;
            }
        }
        // A transport failure, or anything that is not a service error at all (a bare
        // SerializationException from a response the codec could not read), maps to the client-side
        // exception v2 uses for the same conditions -- not to the service's exception, which would
        // claim the service reported something it did not.
        if (e instanceof TransportException || !(e instanceof CallException) || baseExceptionBuilder == null) {
            return SdkClientException.builder().message(e.getMessage()).cause(e).build();
        }
        return baseExceptionBuilder.get().message(e.getMessage()).cause(e).build();
    }

    /**
     * Builder for {@link SmithyBridgeClient}.
     *
     * <p>Adds {@code service} — which {@link Client.Builder} does not expose, though
     * {@code ClientConfig} requires it — and the fallback error factory.
     */
    public static final class Builder extends Client.Builder<SmithyBridgeClient, Builder> {

        private Supplier<? extends AwsServiceException.Builder> baseExceptionBuilder;

        private Builder() {
        }

        /**
         * @param service the generated {@code ApiService} for the service being called.
         * @return this builder.
         */
        public Builder service(ApiService service) {
            configBuilder().service(service);
            return this;
        }

        /**
         * Sets the service's base exception, used for any failure with no more specific v2 type.
         *
         * <p>Generated code passes a reference to the generated base exception's builder, e.g.
         * {@code DynamoDbException::builder}, so an unmodeled server error still surfaces as a
         * {@code DynamoDbException} the way v2 does it. The same supplier is handed to
         * {@link software.amazon.awssdk.bridge.smithyjava.error.V2ErrorEnricher}, which populates it
         * from the HTTP response; this class only uses it for failures that never got a response.
         *
         * @param baseExceptionBuilder supplier of the service base exception's builder.
         * @return this builder.
         */
        public Builder baseExceptionBuilder(Supplier<? extends AwsServiceException.Builder> baseExceptionBuilder) {
            this.baseExceptionBuilder = baseExceptionBuilder;
            return this;
        }

        @Override
        public SmithyBridgeClient build() {
            return new SmithyBridgeClient(this);
        }
    }
}
