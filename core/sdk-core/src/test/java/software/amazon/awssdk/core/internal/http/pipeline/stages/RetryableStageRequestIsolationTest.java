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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;

/**
 * Per-attempt request guarantees around {@link RetryableStageHelper#requestToSend()}.
 *
 * <p>{@code requestToSend()} now returns the incoming request untouched when the retry-info header already reads what the
 * current attempt needs, which {@link ApplyRetryInfoStage} arranges for the first attempt. That makes the following
 * properties load-bearing, where before they were guaranteed structurally by rebuilding the request every attempt:
 *
 * <ol>
 *   <li>The fast path must only trigger when the header genuinely matches, so each attempt's request describes that
 *       attempt and no other.</li>
 *   <li>A later attempt must not disturb an earlier attempt's request. Callers hold onto them: mock HTTP clients record
 *       every attempt's request and assert on it afterwards.</li>
 *   <li>The attempt counter is replaced, not appended, so {@code amz-sdk-request} carries exactly one value.</li>
 *   <li>Each attempt starts from the <em>unsigned</em> request. Signing runs downstream of this stage on every attempt;
 *       a signed request must never be what a subsequent attempt starts from, or it would be signed over a stale
 *       {@code Authorization} header.</li>
 * </ol>
 */
public class RetryableStageRequestIsolationTest {
    private static final String AUTHORIZATION = "Authorization";

    private RetryStrategy retryStrategy;
    private RetryToken retryToken;
    private HttpClientDependencies dependencies;
    private RequestExecutionContext context;

    @BeforeEach
    void setup() {
        retryToken = mock(RetryToken.class);
        AcquireInitialTokenResponse initialToken = mock(AcquireInitialTokenResponse.class);
        when(initialToken.token()).thenReturn(retryToken);
        when(initialToken.delay()).thenReturn(Duration.ZERO);

        retryStrategy = mock(RetryStrategy.class);
        when(retryStrategy.acquireInitialToken(any())).thenReturn(initialToken);
        when(retryStrategy.maxAttempts()).thenReturn(3);

        dependencies = HttpClientDependencies.builder()
                                             .clientConfiguration(SdkClientConfiguration.builder()
                                                                                        .option(SdkClientOption
                                                                                                    .RETRY_STRATEGY,
                                                                                                retryStrategy)
                                                                                        .build())
                                             .build();

        context = RequestExecutionContext.builder()
                                         .originalRequest(mock(SdkRequest.class))
                                         .executionContext(ExecutionContext.builder()
                                                                           .metricCollector(MetricCollector.create("test"))
                                                                           .executionAttributes(ExecutionAttributes
                                                                                                    .builder().build())
                                                                           .build())
                                         .build();
    }

    @Test
    void requestToSend_acrossAttempts_eachAttemptGetsItsOwnRequestObject() throws Exception {
        List<SdkHttpFullRequest> seen = runThreeFailingAttempts(request -> { });

        assertThat(seen).hasSize(3);
        assertThat(seen.get(0)).isNotSameAs(seen.get(1));
        assertThat(seen.get(1)).isNotSameAs(seen.get(2));
    }

    @Test
    void requestToSend_whenRetryInfoAlreadyMatchesTheAttempt_returnsTheRequestUntouched() {
        // What ApplyRetryInfoStage sets up for the first attempt: the header is already correct, so no rebuild.
        SdkHttpFullRequest prestamped = prestampedRequest();

        RetryableStageHelper helper = new RetryableStageHelper(prestamped, context, dependencies);
        helper.startingAttempt();

        assertThat(helper.requestToSend()).isSameAs(prestamped);
    }

    @Test
    void requestToSend_whenRetryInfoIsStale_rebuildsWithTheCurrentAttempt() {
        // A pre-stamped value that disagrees with this attempt must not be trusted: the fast path is a match check,
        // not an assumption that the header is already right.
        SdkHttpFullRequest stale = baseRequest().toBuilder()
                                               .putHeader(RetryableStageHelper.SDK_RETRY_INFO_HEADER,
                                                          "attempt=1; max=99")
                                               .build();

        RetryableStageHelper helper = new RetryableStageHelper(stale, context, dependencies);
        helper.startingAttempt();

        SdkHttpFullRequest requestToSend = helper.requestToSend();
        assertThat(requestToSend).isNotSameAs(stale);
        assertThat(attemptHeader(requestToSend)).isEqualTo("attempt=1; max=3");
    }

    @Test
    void requestToSend_whenRetryInfoIsAbsent_stampsIt() {
        // ApplyRetryInfoStage declines to stamp when max-attempts cannot be resolved; the helper must still be correct.
        SdkHttpFullRequest unstamped = baseRequest();

        RetryableStageHelper helper = new RetryableStageHelper(unstamped, context, dependencies);
        helper.startingAttempt();

        assertThat(attemptHeader(helper.requestToSend())).isEqualTo("attempt=1; max=3");
    }

    @Test
    void requestToSend_acrossAttempts_earlierAttemptRequestIsNotMutatedByLaterAttempts() throws Exception {
        List<SdkHttpFullRequest> seen = runThreeFailingAttempts(request -> { });

        // Read after all three attempts have completed: attempt 1's request must still describe attempt 1.
        assertThat(attemptHeader(seen.get(0))).isEqualTo("attempt=1; max=3");
        assertThat(attemptHeader(seen.get(1))).isEqualTo("attempt=2; max=3");
        assertThat(attemptHeader(seen.get(2))).isEqualTo("attempt=3; max=3");
    }

    @Test
    void requestToSend_acrossAttempts_attemptHeaderHasSingleValue() throws Exception {
        List<SdkHttpFullRequest> seen = runThreeFailingAttempts(request -> { });

        // Appending instead of replacing would accumulate "attempt=1", "attempt=2", ... on the third attempt.
        for (SdkHttpFullRequest request : seen) {
            assertThat(request.matchingHeaders(RetryableStageHelper.SDK_RETRY_INFO_HEADER)).hasSize(1);
        }
    }

    @Test
    void requestToSend_whenDownstreamSignsTheRequest_laterAttemptsAreStillUnsigned() throws Exception {
        // Stands in for the signing stage, which runs downstream of RetryableStage on every attempt.
        List<SdkHttpFullRequest> seen = runThreeFailingAttempts(
            request -> request.toBuilder().putHeader(AUTHORIZATION, "AWS4-HMAC-SHA256 signature-of-attempt").build());

        for (SdkHttpFullRequest request : seen) {
            assertThat(request.firstMatchingHeader(AUTHORIZATION)).isEmpty();
        }
    }

    @Test
    void applyRetryInfoStage_withLegacyRetryPolicy_agreesWithTheRetryHelper() throws Exception {
        // The stage and the helper resolve max-attempts by different routes (the helper goes through the retry-policy
        // adapter). If they ever disagree, the fast path silently stops firing, so pin the agreement for both
        // configuration styles.
        assertStageAgreesWithHelper(SdkClientConfiguration.builder()
                                                          .option(SdkClientOption.RETRY_POLICY,
                                                                  RetryPolicy.builder().numRetries(7).build())
                                                          .build());

        assertStageAgreesWithHelper(SdkClientConfiguration.builder()
                                                          .option(SdkClientOption.RETRY_STRATEGY, retryStrategy)
                                                          .build());
    }

    private void assertStageAgreesWithHelper(SdkClientConfiguration clientConfiguration) throws Exception {
        HttpClientDependencies deps = HttpClientDependencies.builder()
                                                            .clientConfiguration(clientConfiguration)
                                                            .build();

        SdkHttpFullRequest stamped = new ApplyRetryInfoStage(deps).execute(baseRequest().toBuilder(), context).build();

        RetryableStageHelper helper = new RetryableStageHelper(stamped, context, deps);
        helper.startingAttempt();

        assertThat(helper.requestToSend()).isSameAs(stamped);
    }

    /**
     * Drive {@link RetryableStage} through three failing attempts, returning the request each attempt was given.
     *
     * @param downstreamMutation applied to the request inside the delegate pipeline, standing in for stages (signing above
     *                           all) that derive a new request from the one handed to them.
     */
    private List<SdkHttpFullRequest> runThreeFailingAttempts(
        Consumer<SdkHttpFullRequest> downstreamMutation) throws Exception {

        List<SdkHttpFullRequest> seen = new ArrayList<>();

        Response<SdkResponse> failure = Response.<SdkResponse>builder()
                                                .httpResponse(SdkHttpFullResponse.builder().statusCode(500).build())
                                                .isSuccess(false)
                                                .exception(SdkException.builder().message("retry me").build())
                                                .build();

        @SuppressWarnings("unchecked")
        RequestPipeline<SdkHttpFullRequest, Response<SdkResponse>> delegate = mock(RequestPipeline.class);
        when(delegate.execute(any(), any())).thenAnswer(invocation -> {
            SdkHttpFullRequest request = invocation.getArgument(0);
            seen.add(request);
            downstreamMutation.accept(request);
            return failure;
        });

        // Allow two retries, then refuse, so the stage runs exactly three attempts and throws.
        when(retryStrategy.refreshRetryToken(any())).thenAnswer(invocation -> {
            if (seen.size() < 3) {
                return RefreshRetryTokenResponse.create(retryToken, Duration.ZERO);
            }
            throw new TokenAcquisitionFailedException("no more retries", retryToken, null, Duration.ZERO);
        });

        RetryableStage<SdkResponse> stage = new RetryableStage<>(dependencies, delegate);
        assertThatThrownBy(() -> stage.execute(prestampedRequest(), context)).isInstanceOf(SdkException.class);

        return seen;
    }

    /**
     * The request as {@link RetryableStage} actually receives it in production: the mutation sequence has run, so
     * {@link ApplyRetryInfoStage} has already stamped the first attempt's retry-info header.
     */
    private static SdkHttpFullRequest prestampedRequest() {
        return baseRequest().toBuilder()
                            .putHeader(RetryableStageHelper.SDK_RETRY_INFO_HEADER, "attempt=1; max=3")
                            .build();
    }

    private static SdkHttpFullRequest baseRequest() {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.GET)
                                 .uri(URI.create("https://my-service.amazonaws.com"))
                                 .putHeader("amz-sdk-invocation-id", "fixed-invocation-id")
                                 .putHeader("User-Agent", "test")
                                 .build();
    }

    private static String attemptHeader(SdkHttpFullRequest request) {
        return request.firstMatchingHeader(RetryableStageHelper.SDK_RETRY_INFO_HEADER).orElse(null);
    }
}
