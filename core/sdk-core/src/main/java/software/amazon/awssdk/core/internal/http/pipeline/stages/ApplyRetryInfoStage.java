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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.retries.api.RetryStrategy;

/**
 * Pre-stamps the retry-info header ({@code amz-sdk-request}) for the first attempt.
 *
 * <p>{@link RetryableStageHelper#requestToSend()} owns this header — it has to rewrite it on every retry, since the
 * attempt number changes. But rewriting it for the <i>first</i> attempt, which is the only attempt the vast majority of
 * calls ever make, used to cost a full header-map clone and a second request materialization: the retry stage receives an
 * immutable request, and the first mutation of a builder derived from one copies the map
 * ({@link software.amazon.awssdk.internal.http.LowCopyListMap}).
 *
 * <p>Writing the same bytes here instead is free. This stage runs inside the mutation sequence, where the request is
 * already in builder form and the map is already privately owned (the transaction-id stage immediately before it has
 * done the one copy that sequence pays), so this is one more in-place put among ten. The retry helper then finds the
 * header already correct and returns the request untouched.
 *
 * <p>Correctness does not depend on this stage agreeing with the helper. The helper compares the existing header against
 * the value it would write itself and rebuilds if it differs, so a mismatch — or this stage declining to stamp because
 * max-attempts could not be resolved — costs performance, never correctness.
 */
@SdkInternalApi
public final class ApplyRetryInfoStage implements MutableRequestToRequestPipeline {

    /**
     * The first attempt's header value, or null if the retry configuration does not expose a max-attempts count, in which
     * case this stage stamps nothing. Resolved once per pipeline rather than per attempt because the retry configuration
     * is fixed for the life of the client.
     */
    private final String firstAttemptRetryInfo;

    public ApplyRetryInfoStage(HttpClientDependencies dependencies) {
        this.firstAttemptRetryInfo = resolveFirstAttemptRetryInfo(dependencies);
    }

    private static String resolveFirstAttemptRetryInfo(HttpClientDependencies dependencies) {
        Integer maxAttempts = resolveMaxAttempts(dependencies);
        return maxAttempts == null ? null : RetryableStageHelper.retryInfoHeaderValue(1, maxAttempts);
    }

    /**
     * Resolve max attempts the same way {@link RetryableStageHelper} does, but without the retry-policy adapter: a
     * configured {@link RetryPolicy} wins over a {@link RetryStrategy}, and the adapter the helper would build around it
     * reports {@code numRetries() + 1} regardless of whether it has been initialized, so it can be read here too.
     */
    private static Integer resolveMaxAttempts(HttpClientDependencies dependencies) {
        RetryPolicy retryPolicy = dependencies.clientConfiguration().option(SdkClientOption.RETRY_POLICY);
        if (retryPolicy != null) {
            return retryPolicy.numRetries() + 1;
        }

        RetryStrategy retryStrategy = dependencies.clientConfiguration().option(SdkClientOption.RETRY_STRATEGY);
        return retryStrategy == null ? null : retryStrategy.maxAttempts();
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context) {
        if (firstAttemptRetryInfo == null) {
            return request;
        }
        return request.putHeader(RetryableStageHelper.SDK_RETRY_INFO_HEADER, firstAttemptRetryInfo);
    }
}
