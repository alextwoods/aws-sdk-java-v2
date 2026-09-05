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

package software.amazon.awssdk.bridge.smithyjava.error;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.smithy.java.client.core.CallContext;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.OutputHook;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.retries.api.RetrySafety;

/**
 * Completes a failed attempt's exception with the transport-level facts the deserializer never sees,
 * and classifies it the way v2 would.
 *
 * <p>smithy-java builds errors from the payload alone: the builder handed to the protocol has no access
 * to the HTTP response or the normalized error code. But v2's retry classification depends on exactly
 * those — {@code AwsServiceException.isThrottlingException()} tests the AWS error code and
 * {@code isClockSkewException()} tests the error code plus the server {@code Date} header.
 *
 * <p>This runs at {@code modifyBeforeAttemptCompletion}: per attempt, inside the retry loop, and
 * before the retry strategy inspects the error. That ordering is what makes the retry bridge work — a
 * throttling error is classified as throttling before the retry decision is made.
 *
 * <h2>Three kinds of failure</h2>
 *
 * <ol>
 *   <li>A {@link V2ModeledError}: the error matched the operation's {@code TypeRegistry} and a real v2
 *       exception already exists. {@link V2ModeledError#enrich} fills in its transport fields.
 *   <li>Any other {@link CallException} carrying an HTTP <em>error</em> response: nothing matched the
 *       registry, so a v2 exception is built here from the response and thrown as a
 *       {@link V2UnmodeledError}. Without this the caller sees {@code statusCode() == 0}, a null
 *       request ID, and — because retries are classified off the v2 exception — no retry at all.
 *   <li>Anything else, including a deserialization failure on a 2xx: there is no service exception to
 *       build, so all that is left is the retry classification. See
 *       {@link #retriedByV2(RuntimeException, boolean)} for how v2's rule translates, and
 *       {@link V2RetryableError} for why stamping it on the error in place is not always possible.
 * </ol>
 */
@SdkProtectedApi
public final class V2ErrorEnricher implements ClientInterceptor {

    /**
     * v2 reads {@code Retry-After} as whole seconds and ignores the HTTP-date form; see
     * {@code RetryableStage.retryAfter}. Mirrored rather than improved on, so the two pipelines back
     * off by the same amount. Lowercase because smithy normalizes header names.
     */
    private static final String RETRY_AFTER = "retry-after";

    private final String serviceName;
    private final Supplier<? extends AwsServiceException.Builder> baseExceptionBuilder;

    /**
     * @param serviceName          v2 service name to record in {@code awsErrorDetails}, e.g. {@code "DynamoDb"}.
     * @param baseExceptionBuilder the service's base exception builder, e.g. {@code DynamoDbException::builder},
     *                             used for error responses that matched no modeled shape.
     */
    public V2ErrorEnricher(String serviceName, Supplier<? extends AwsServiceException.Builder> baseExceptionBuilder) {
        this.serviceName = serviceName;
        this.baseExceptionBuilder = baseExceptionBuilder;
    }

    @Override
    public <O extends SerializableStruct> O modifyBeforeAttemptCompletion(
            OutputHook<?, O, ?, ?> hook,
            RuntimeException error
    ) {
        HttpResponse response = hook.response() instanceof HttpResponse http ? http : null;

        if (error instanceof V2ModeledError modeled) {
            if (response != null) {
                modeled.enrich(response, hook.context().get(CallContext.RESPONSE_ERROR_CODE), serviceName);
                applyRetryAfter(modeled, response);
            }
            return hook.forward(error);
        }

        // A 2xx that failed to deserialize is a client-side problem, not a service error: building a
        // service exception for it would report `statusCode() == 200`, which no v2 caller expects.
        if (response != null && response.statusCode() >= 400) {
            V2UnmodeledError replacement =
                    new V2UnmodeledError(build(response, hook.context().get(CallContext.RESPONSE_ERROR_CODE)), error);
            applyRetryAfter(replacement, response);
            throw replacement;
        }

        // No service exception to build: a transport failure, or a 2xx the client could not parse.
        // Not gated on `instanceof CallException`, because the most common case is not one: smithy's
        // SerializationException is a plain RuntimeException and reaches the retry gate carrying no
        // RetryInfo at all.
        if (!retriedByV2(error, response != null)) {
            return hook.forward(error);
        }
        if (error instanceof CallException call) {
            call.isRetrySafe(RetrySafety.YES);
            if (call.isRetrySafe() == RetrySafety.YES) {
                return hook.forward(error);
            }
            // The setter had no effect: this type overrides the getter with a constant. Read back
            // rather than testing for the known offenders by name, so a new one cannot slip past.
        }
        throw new V2RetryableError(error);
    }

    /** Builds the service's base exception from an error response no modeled shape claimed. */
    private AwsServiceException build(HttpResponse response, String errorCode) {
        SdkHttpResponse httpResponse = SdkHttpFullResponse.builder()
                                                          .statusCode(response.statusCode())
                                                          .headers(response.headers().map())
                                                          .build();
        // The payload's own `message` is unavailable here: smithy exposes the error code on the context
        // but not the message, and the body has already been consumed. So this reproduces v2's two
        // *fallback* messages only, which is exact whenever v2 had no message either and differs when
        // it did. See compatability_issues.md 1.6.
        String message = errorCode != null && !errorCode.isEmpty()
                ? "Service returned error code " + errorCode
                : "Service returned HTTP status code " + response.statusCode();
        return baseExceptionBuilder.get()
                                   .message(message)
                                   .awsErrorDetails(AwsErrorDetails.builder()
                                                                   .errorCode(errorCode)
                                                                   .errorMessage(message)
                                                                   .serviceName(serviceName)
                                                                   .sdkHttpResponse(httpResponse)
                                                                   .build())
                                   .statusCode(response.statusCode())
                                   .requestId(requestId(httpResponse))
                                   .extendedRequestId(httpResponse.firstMatchingHeader("x-amz-id-2").orElse(null))
                                   .build();
    }

    private static String requestId(SdkHttpResponse response) {
        return response.firstMatchingHeader("x-amzn-RequestId")
                       .orElseGet(() -> response.firstMatchingHeader("x-amz-request-id").orElse(null));
    }

    /**
     * Whether v2 would retry a failure that produced no service exception — a transport error, or a
     * response that could not be deserialized.
     *
     * <p>v2's rule is {@code IOException} or {@code UncheckedIOException} anywhere in the cause chain
     * ({@code SdkDefaultRetrySetting.RETRYABLE_EXCEPTIONS}, applied through
     * {@code retryOnExceptionOrCauseInstanceOf}). That one rule covers both cases at once because v2
     * parses JSON with Jackson 2, whose {@code JsonProcessingException} <em>is</em> an
     * {@code IOException}. smithy-java parses with Jackson 3, where the equivalent exceptions extend
     * {@code RuntimeException} and arrive wrapped in a {@link SerializationException}; the cause chain
     * of a torn response body therefore contains no {@code IOException} at all, and the plain chain
     * walk that reproduces v2 for transport errors silently fails to retry it.
     *
     * <p>Hence the third clause, gated on having received a response: an unparseable <em>response</em>
     * is retried, matching v2, while a serialization failure with no response — which in v2 happens
     * before the attempt and is never retried — is not.
     *
     * <p>A failure this does not recognize is left at whatever smithy inferred rather than forced to
     * NO: a bridge failure of unknown origin is not evidence that retrying is unsafe.
     */
    private static boolean retriedByV2(RuntimeException error, boolean responseReceived) {
        for (Throwable cause = error; cause != null && cause.getCause() != cause; cause = cause.getCause()) {
            if (cause instanceof IOException || cause instanceof UncheckedIOException
                || (responseReceived && cause instanceof SerializationException)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Records the {@code Retry-After} hint, which {@code ClientPipeline} reads off the error via
     * {@code RetryInfo.retryAfter()} and passes to the retry strategy as a suggested delay.
     */
    private static void applyRetryAfter(CallException target, HttpResponse response) {
        String value = response.headers().firstValue(RETRY_AFTER);
        if (value == null) {
            return;
        }
        try {
            target.retryAfter(Duration.ofSeconds(Integer.parseInt(value.trim())));
        } catch (NumberFormatException e) {
            // Same as v2: an unparseable hint is ignored rather than fatal.
        }
    }
}
