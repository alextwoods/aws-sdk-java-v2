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

import static software.amazon.awssdk.utils.FunctionalUtils.safeFunction;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AfterExecutionInterceptorsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallAttemptMetricCollectionStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallTimeoutTrackingStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncBeforeTransmissionExecutionInterceptorsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncExecutionFailureExceptionReportingStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncSigningStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.UnwrapResponseContainer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * The async API call pipeline, written in a straight line — the async counterpart of
 * {@link SyncApiCallPipeline}, with the same two deliberate properties: stage logic is reused, not
 * copied, and construction stays per-request. See that class for the rationale.
 *
 * <p>One async-specific subtlety is preserved with care. The builder form adapted the two trailing
 * synchronous stages (unwrap, after-execution interceptors) through
 * {@code RequestPipelineBuilder.async(...)}, whose wrapper chains them with {@code thenApply} and
 * then calls {@link CompletableFutureUtils#forwardExceptionTo} so that a caller cancelling the
 * <i>returned</i> future propagates the cancellation backwards to the in-flight HTTP future.
 * {@link FinishStages} reproduces exactly that structure — a {@code thenApply} per stage, each
 * followed by the backward exception link — rather than approximating it, because dropping the
 * backward link would leave a cancelled call's HTTP exchange running.
 *
 * <p>The nesting, outermost first, preserved exactly from the builder form:
 * [ 11 mutation stages → api-call metrics → api-call timeout → failure-reporting →
 * [ retry( attempt-metrics → [ sign → before-transmission → http ] ) → unwrap → after-execution ] ].
 */
@SdkInternalApi
final class AsyncApiCallPipeline {

    private AsyncApiCallPipeline() {
    }

    /**
     * Build the full pipeline for one request. Input is the marshalled {@link SdkHttpFullRequest};
     * output is a future of the unmarshalled response, produced through {@code responseHandler}.
     */
    static <OutputT> RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> create(
            HttpClientDependencies dependencies,
            TransformingAsyncResponseHandler<Response<OutputT>> responseHandler) {

        // Everything inside AsyncRetryableStage re-runs on every retry attempt.
        RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> retrying =
            new AsyncRetryableStage<>(responseHandler, dependencies,
                new AsyncApiCallAttemptMetricCollectionStage<>(
                    new AttemptStages<>(dependencies, responseHandler)));

        // Note: API_CALL_DURATION is measured by BaseAsyncClientHandler
        RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> call =
            new AsyncApiCallTimeoutTrackingStage<>(dependencies,
                new AsyncExecutionFailureExceptionReportingStage<>(
                    new FinishStages<>(retrying)));

        return new MutationThenCall<>(dependencies, call);
    }

    /** The shared request-mutation sequence, then the async call chain. */
    private static final class MutationThenCall<OutputT>
            implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> {

        private final RequestMutationStages mutation;
        private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> call;

        MutationThenCall(HttpClientDependencies dependencies,
                         RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> call) {
            this.mutation = new RequestMutationStages(dependencies, ClientType.ASYNC);
            this.call = call;
        }

        @Override
        public CompletableFuture<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context)
                throws Exception {
            return call.execute(mutation.execute(request, context), context);
        }
    }

    /**
     * One HTTP attempt: sign, run transmission interceptors, make the call. Replaces three
     * {@code .then(...)} compositions. Re-executed by {@link AsyncRetryableStage} on retries.
     * Each stage receives the previous stage's future, exactly as the composed form chained them.
     */
    private static final class AttemptStages<OutputT>
            implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> {

        private final AsyncSigningStage signing;
        private final AsyncBeforeTransmissionExecutionInterceptorsStage beforeTransmission =
            new AsyncBeforeTransmissionExecutionInterceptorsStage();
        private final MakeAsyncHttpRequestStage<OutputT> makeHttpRequest;

        AttemptStages(HttpClientDependencies dependencies,
                      TransformingAsyncResponseHandler<Response<OutputT>> responseHandler) {
            this.signing = new AsyncSigningStage(dependencies);
            this.makeHttpRequest = new MakeAsyncHttpRequestStage<>(responseHandler, dependencies);
        }

        @Override
        public CompletableFuture<Response<OutputT>> execute(SdkHttpFullRequest request,
                                                            RequestExecutionContext context) throws Exception {
            CompletableFuture<SdkHttpFullRequest> signed = signing.execute(request, context);
            return makeHttpRequest.execute(beforeTransmission.execute(signed, context), context);
        }
    }

    /**
     * Unwrap the response container and run after-execution interceptors as one dependent stage on the
     * response future, with the backward exception link that lets cancellation of the downstream future
     * reach upstream.
     */
    private static final class FinishStages<OutputT>
            implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> {

        private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> call;
        private final UnwrapResponseContainer<OutputT> unwrap = new UnwrapResponseContainer<>();
        private final AfterExecutionInterceptorsStage<OutputT> afterExecution = new AfterExecutionInterceptorsStage<>();

        FinishStages(RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> call) {
            this.call = call;
        }

        @Override
        public CompletableFuture<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context)
                throws Exception {
            CompletableFuture<Response<OutputT>> responseFuture = call.execute(request, context);

            // Both transforms are synchronous and run back to back, so one dependent stage carries them, with one
            // backward exception link to the response future (which is what cancellation needs to reach). The
            // stage-per-transform futures and the intermediate link did nothing a caller could observe.
            CompletableFuture<OutputT> finished =
                responseFuture.thenApply(safeFunction(r -> afterExecution.execute(unwrap.execute(r, context), context)));
            return CompletableFutureUtils.forwardExceptionTo(finished, responseFuture);
        }
    }
}
