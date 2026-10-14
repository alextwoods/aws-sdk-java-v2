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

package software.amazon.awssdk.core.internal.http;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.RetryableStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Logger;

/**
 * The retry decisions of one call on the schema-based request pipeline, shared by the sync and async pipelines. It is a
 * thin layer over the legacy {@link RetryableStageHelper}, which makes every decision (tokens, backoff, clock skew,
 * attempt count, the final exception), so retries behave as in {@link RetryableStage} and {@link AsyncRetryableStage}.
 * The pipelines keep only the waiting: the sync pipeline sleeps, the async pipeline schedules.
 *
 * <p>The retry-info header {@code amz-sdk-request} of the first attempt is stamped by {@link RequestMutationStages} in
 * its single pass over the request (racecar B2.b, re-implemented), so the first attempt sends the prepared request
 * without another copy; a retry copies it with the attempt's header. The header value is the legacy
 * {@code attempt=<n>; max=<max attempts>}.
 *
 * <p>An instance is created per call and is not thread-safe; the async pipeline uses it from one attempt at a time.
 */
@SdkInternalApi
@NotThreadSafe
public final class RetryDecisions {
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String X_AMZ_RETRY_AFTER_HEADER = "x-amz-retry-after";
    private static final Logger LOG = Logger.loggerFor(RetryableStage.class);

    private final SdkHttpFullRequest preparedRequest;
    private final RequestExecutionContext context;
    private final RetryableStageHelper helper;
    private final int maxAttempts;
    private final boolean async;

    private RetryDecisions(SdkHttpFullRequest preparedRequest,
                           RequestExecutionContext context,
                           HttpClientDependencies dependencies,
                           ClientType clientType) {
        this.preparedRequest = preparedRequest;
        this.context = context;
        this.helper = new RetryableStageHelper(preparedRequest, context, dependencies);
        this.maxAttempts = maxAttempts(dependencies.clientConfiguration());
        this.async = clientType == ClientType.ASYNC;
    }

    /**
     * Creates the retry decisions of a call.
     *
     * @param preparedRequest the request {@link RequestMutationStages#prepare} returned, with the first attempt's
     *     retry-info header
     * @param context the call's context
     * @param dependencies the client's dependencies (retry strategy, clock skew)
     * @param clientType whether the call is on the sync or the async pipeline; only the suggested delay differs, as in
     *     the legacy stages
     */
    public static RetryDecisions create(SdkHttpFullRequest preparedRequest,
                                        RequestExecutionContext context,
                                        HttpClientDependencies dependencies,
                                        ClientType clientType) {
        return new RetryDecisions(preparedRequest, context, dependencies, clientType);
    }

    /**
     * The maximum number of attempts of the client's retry policy or strategy, as {@link RetryableStageHelper} reports
     * it in the retry-info header.
     */
    public static int maxAttempts(SdkClientConfiguration clientConfiguration) {
        RetryPolicy retryPolicy = clientConfiguration.option(SdkClientOption.RETRY_POLICY);
        if (retryPolicy != null) {
            return retryPolicy.numRetries() + 1;
        }
        RetryStrategy retryStrategy = clientConfiguration.option(SdkClientOption.RETRY_STRATEGY);
        return retryStrategy.maxAttempts();
    }

    /**
     * The retry-info header value of an attempt.
     */
    static String retryInfo(int attempt, int maxAttempts) {
        return "attempt=" + attempt + "; max=" + maxAttempts;
    }

    /**
     * Acquires the initial retry token; the caller waits the returned delay before the first attempt.
     */
    public Duration acquireInitialToken() {
        return helper.acquireInitialToken();
    }

    /**
     * Starts the next attempt, and returns its unsigned request: the prepared request for the first attempt, a copy with
     * the attempt's retry-info header for a retry.
     */
    public SdkHttpFullRequest startAttempt() {
        helper.startingAttempt();
        helper.logSendingRequest();
        int attempt = helper.getAttemptNumber();
        if (attempt == 1) {
            return preparedRequest;
        }
        return preparedRequest.toBuilder()
                              .putHeader(RetryableStageHelper.SDK_RETRY_INFO_HEADER, retryInfo(attempt, maxAttempts))
                              .build();
    }

    /**
     * Records the attempt's response, successful or not, and adjusts the client's clock skew for a failed one, as the
     * legacy stages do when the attempt returns a response.
     */
    public void responseReceived(Response<?> response) {
        helper.setLastResponse(response.httpResponse());
        if (!response.isSuccess()) {
            helper.adjustClockIfClockSkew(response);
        }
    }

    /**
     * Records that the attempt succeeded.
     */
    public void attemptSucceeded() {
        helper.recordAttemptSucceeded();
    }

    /**
     * The delay the service suggested for retrying an attempt, as the legacy stage of the pipeline computes it:
     * <ul>
     *     <li>sync ({@link RetryableStage}): for a failed response, {@code x-amz-retry-after} (milliseconds) with the
     *     2026 retry behavior, otherwise {@code Retry-After} (seconds); zero for an attempt that threw;</li>
     *     <li>async ({@link AsyncRetryableStage}): {@code x-amz-retry-after} of the last response received with the
     *     2026 retry behavior, otherwise zero.</li>
     * </ul>
     *
     * @param failedResponse the failed response of the attempt, or null when the attempt threw
     */
    public Duration suggestedDelay(Response<?> failedResponse) {
        boolean newRetries = context.executionAttributes()
                                    .getOptionalAttribute(SdkInternalExecutionAttribute.NEW_RETRIES_2026_ENABLED)
                                    .orElse(false);
        if (async) {
            return newRetries ? header(helper.getLastResponse(), X_AMZ_RETRY_AFTER_HEADER, Duration::ofMillis)
                                .orElse(Duration.ZERO)
                              : Duration.ZERO;
        }
        if (failedResponse == null) {
            return Duration.ZERO;
        }
        Optional<Duration> delay = newRetries
                                   ? header(failedResponse.httpResponse(), X_AMZ_RETRY_AFTER_HEADER, Duration::ofMillis)
                                   : header(failedResponse.httpResponse(), RETRY_AFTER_HEADER, Duration::ofSeconds);
        return delay.orElse(Duration.ZERO);
    }

    /**
     * Decides whether to retry after a failed attempt.
     *
     * @param failure the attempt's exception: the service error of a failed response, or what the attempt threw
     *     (wrapped as the legacy stages wrap it when it is not an {@link SdkException})
     * @param suggestedDelay the delay from {@link #suggestedDelay}
     * @return the decision; the caller waits {@link Decision#delay()} before retrying or before failing the call
     */
    public Decision attemptFailed(Throwable failure, Duration suggestedDelay) {
        helper.setLastException(failure);
        Either<Duration, Duration> backoff = helper.tryRefreshToken(suggestedDelay);
        Optional<Duration> retryDelay = backoff.left();
        if (retryDelay.isPresent()) {
            helper.logBackingOff(retryDelay.get());
            return new Decision(retryDelay.get(), null);
        }
        Duration failureDelay = backoff.right().orElse(Duration.ZERO);
        helper.logAcquireFailureBackingOff(failureDelay);
        return new Decision(failureDelay, helper.retryPolicyDisallowedRetryException());
    }

    /**
     * The last exception recorded by {@link #attemptFailed}, as the retry strategy saw it.
     */
    public SdkException lastException() {
        return helper.getLastException();
    }

    /**
     * The number of the current attempt, starting at 1.
     */
    public int attemptNumber() {
        return helper.getAttemptNumber();
    }

    private static Optional<Duration> header(SdkHttpResponse response, String name, Function<Long, Duration> unit) {
        if (response == null) {
            return Optional.empty();
        }
        return response.firstMatchingHeader(name).map(value -> {
            try {
                return unit.apply((long) Integer.parseInt(value));
            } catch (NumberFormatException e) {
                LOG.debug(() -> String.format("Unable to parse header '%s' value '%s' as integer", name, value), e);
                return null;
            }
        });
    }

    /**
     * The result of {@link #attemptFailed}: retry after {@link #delay()}, or fail the call with {@link #exception()}
     * after {@link #delay()}.
     */
    public static final class Decision {
        private final Duration delay;
        private final SdkException exception;

        private Decision(Duration delay, SdkException exception) {
            this.delay = delay;
            this.exception = exception;
        }

        /**
         * Whether the retry strategy allows another attempt.
         */
        public boolean retry() {
            return exception == null;
        }

        /**
         * How long to wait before the next attempt, or before failing the call.
         */
        public Duration delay() {
            return delay;
        }

        /**
         * The exception to fail the call with, or null when the call is retried.
         */
        public SdkException exception() {
            return exception;
        }
    }
}
