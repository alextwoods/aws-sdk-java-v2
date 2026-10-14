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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.baseConfig;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.dependencies;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Compares {@link RetryDecisions} with the legacy {@link RetryableStageHelper} use of {@code RetryableStage} and
 * {@code AsyncRetryableStage}.
 */
class RetryDecisionsTest {

    @Test
    void startAttempt_firstAttemptSendsThePreparedRequestAndRetriesStampTheirAttempt() throws Exception {
        SdkClientConfiguration config = baseConfig().build();
        SdkHttpFullRequest prepared = prepared(config);
        RequestExecutionContext context = context(prepared);
        RetryDecisions decisions = RetryDecisions.create(prepared, context, dependencies(config), ClientType.SYNC);

        assertThat(decisions.startAttempt()).isSameAs(prepared);
        assertThat(context.executionAttributes().getAttribute(InternalCoreExecutionAttribute.EXECUTION_ATTEMPT)).isEqualTo(1);
        SdkHttpFullRequest second = decisions.startAttempt();

        assertThat(second.matchingHeaders(RetryableStageHelper.SDK_RETRY_INFO_HEADER)).containsExactly("attempt=2; max=3");
        assertThat(second.firstMatchingHeader("amz-sdk-invocation-id"))
            .isEqualTo(prepared.firstMatchingHeader("amz-sdk-invocation-id"));
        assertThat(decisions.attemptNumber()).isEqualTo(2);
    }

    @Test
    void startAttempt_matchesTheLegacyHelperRequestForEveryAttempt() throws Exception {
        SdkClientConfiguration config = baseConfig().option(SdkClientOption.RETRY_POLICY,
                                                            RetryPolicy.builder().numRetries(4).build())
                                                    .build();
        SdkHttpFullRequest prepared = prepared(config);
        SdkHttpFullRequest unstamped = prepared.toBuilder().removeHeader(RetryableStageHelper.SDK_RETRY_INFO_HEADER).build();
        RetryableStageHelper legacy = new RetryableStageHelper(unstamped, context(unstamped), dependencies(config));
        RetryDecisions decisions = RetryDecisions.create(prepared, context(prepared), dependencies(config), ClientType.SYNC);

        for (int attempt = 1; attempt <= 3; attempt++) {
            legacy.startingAttempt();
            assertThat(decisions.startAttempt().headers()).isEqualTo(legacy.requestToSend().headers());
        }
    }

    @Test
    void attemptFailed_retriesUntilTheStrategyStopsAndFailsAsTheLegacyHelper() throws Exception {
        List<Boolean> legacyRetries = new ArrayList<>();
        SdkException legacyFailure = null;
        RetryableStageHelper legacy = newLegacyHelper();
        legacy.acquireInitialToken();
        for (int attempt = 1; legacyFailure == null; attempt++) {
            legacy.startingAttempt();
            legacy.setLastException(serviceError(attempt));
            boolean retry = legacy.tryRefreshToken(Duration.ZERO).left().isPresent();
            legacyRetries.add(retry);
            if (!retry) {
                legacyFailure = legacy.retryPolicyDisallowedRetryException();
            }
        }

        List<Boolean> retries = new ArrayList<>();
        SdkException failure = null;
        SdkClientConfiguration config = baseConfig().build();
        SdkHttpFullRequest prepared = prepared(config);
        RetryDecisions decisions = RetryDecisions.create(prepared, context(prepared), dependencies(config), ClientType.SYNC);
        decisions.acquireInitialToken();
        for (int attempt = 1; failure == null; attempt++) {
            decisions.startAttempt();
            RetryDecisions.Decision decision = decisions.attemptFailed(serviceError(attempt), Duration.ZERO);
            retries.add(decision.retry());
            if (!decision.retry()) {
                failure = decision.exception();
            }
        }

        assertThat(retries).isEqualTo(legacyRetries).containsExactly(true, true, false);
        assertThat(failure).isInstanceOf(legacyFailure.getClass()).hasMessage(legacyFailure.getMessage());
        assertThat(failure.numAttempts()).isEqualTo(legacyFailure.numAttempts()).isEqualTo(3);
        assertThat(failure.getSuppressed()).extracting(Throwable::getMessage)
                                           .isEqualTo(messages(legacyFailure.getSuppressed()));
    }

    @Test
    void attemptFailed_nonSdkException_isWrappedAsTheLegacyHelper() throws Exception {
        SdkClientConfiguration config = baseConfig().build();
        SdkHttpFullRequest prepared = prepared(config);
        RetryDecisions decisions = RetryDecisions.create(prepared, context(prepared), dependencies(config), ClientType.ASYNC);
        decisions.acquireInitialToken();
        decisions.startAttempt();

        decisions.attemptFailed(new IOException("reset"), Duration.ZERO);

        assertThat(decisions.lastException()).isInstanceOf(SdkClientException.class)
                                             .hasMessage("Unable to execute HTTP request: reset");
    }

    @Test
    void suggestedDelay_sync_usesRetryAfterOrXAmzRetryAfterOfTheFailedResponse() throws Exception {
        SdkClientConfiguration config = baseConfig().build();
        SdkHttpFullRequest prepared = prepared(config);
        RequestExecutionContext context = context(prepared);
        RetryDecisions decisions = RetryDecisions.create(prepared, context, dependencies(config), ClientType.SYNC);
        Response<Object> failed = failedResponse("Retry-After", "2", "x-amz-retry-after", "1500");

        assertThat(decisions.suggestedDelay(failed)).isEqualTo(Duration.ofSeconds(2));
        assertThat(decisions.suggestedDelay(null)).isEqualTo(Duration.ZERO);
        context.executionAttributes().putAttribute(SdkInternalExecutionAttribute.NEW_RETRIES_2026_ENABLED, true);
        assertThat(decisions.suggestedDelay(failed)).isEqualTo(Duration.ofMillis(1500));
        assertThat(decisions.suggestedDelay(failedResponse("x-amz-retry-after", "soon"))).isEqualTo(Duration.ZERO);
    }

    @Test
    void suggestedDelay_async_usesXAmzRetryAfterOfTheLastResponseOnlyWithNewRetries() throws Exception {
        SdkClientConfiguration config = baseConfig().build();
        SdkHttpFullRequest prepared = prepared(config);
        RequestExecutionContext context = context(prepared);
        RetryDecisions decisions = RetryDecisions.create(prepared, context, dependencies(config), ClientType.ASYNC);
        Response<Object> failed = failedResponse("Retry-After", "2", "x-amz-retry-after", "1500");
        decisions.responseReceived(failed);

        assertThat(decisions.suggestedDelay(failed)).isEqualTo(Duration.ZERO);
        context.executionAttributes().putAttribute(SdkInternalExecutionAttribute.NEW_RETRIES_2026_ENABLED, true);
        assertThat(decisions.suggestedDelay(null)).isEqualTo(Duration.ofMillis(1500));
    }

    @Test
    void attemptSucceeded_recordsTheRetryCount() throws Exception {
        SdkClientConfiguration config = baseConfig().build();
        SdkHttpFullRequest prepared = prepared(config);
        RequestExecutionContext context = context(prepared);
        RetryDecisions decisions = RetryDecisions.create(prepared, context, dependencies(config), ClientType.SYNC);
        decisions.acquireInitialToken();
        decisions.startAttempt();
        decisions.attemptFailed(serviceError(1), Duration.ZERO);
        decisions.startAttempt();

        decisions.attemptSucceeded();

        assertThat(context.executionContext().metricCollector().collect().metricValues(CoreMetric.RETRY_COUNT))
            .containsExactly(1);
    }

    @Test
    void maxAttempts_followsTheRetryPolicyOrStrategy() {
        assertThat(RetryDecisions.maxAttempts(baseConfig().build())).isEqualTo(3);
        assertThat(RetryDecisions.maxAttempts(baseConfig().option(SdkClientOption.RETRY_POLICY,
                                                                  RetryPolicy.builder().numRetries(0).build())
                                                          .build())).isEqualTo(1);
    }

    private static RetryableStageHelper newLegacyHelper() throws Exception {
        SdkClientConfiguration config = baseConfig().build();
        SdkHttpFullRequest prepared = prepared(config);
        return new RetryableStageHelper(prepared, context(prepared), dependencies(config));
    }

    private static SdkHttpFullRequest prepared(SdkClientConfiguration config) throws Exception {
        SdkHttpFullRequest request = ApiCallStagesTestSupport.marshalledRequest(ApiCallStagesTestSupport.CLIENT_ENDPOINT,
                                                                                "{}");
        return RequestMutationStages.create(dependencies(config), ClientType.SYNC).prepare(request, context(request));
    }

    private static RequestExecutionContext context(SdkHttpFullRequest request) {
        return ApiCallStagesTestSupport.context(NoopTestRequest.builder().build(),
                                                ApiCallStagesTestSupport.attributesWithProtocol(
                                                    new ApiCallStagesTestSupport.HttpStubProtocol()),
                                                request);
    }

    private static SdkServiceException serviceError(int attempt) {
        return SdkServiceException.builder().statusCode(500).message("failure " + attempt).build();
    }

    private static Response<Object> failedResponse(String... headers) {
        SdkHttpFullResponse.Builder response = SdkHttpFullResponse.builder().statusCode(503);
        for (int i = 0; i < headers.length; i += 2) {
            response.putHeader(headers[i], headers[i + 1]);
        }
        return Response.builder()
                       .httpResponse(response.build())
                       .isSuccess(false)
                       .exception(SdkServiceException.builder().statusCode(503).build())
                       .build();
    }

    private static List<String> messages(Throwable[] throwables) {
        List<String> messages = new ArrayList<>();
        for (Throwable t : throwables) {
            messages.add(t.getMessage());
        }
        return messages;
    }
}
