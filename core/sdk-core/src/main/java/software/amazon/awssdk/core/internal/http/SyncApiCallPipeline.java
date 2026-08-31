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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AfterExecutionInterceptorsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AfterTransmissionExecutionInterceptorsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApiCallAttemptMetricCollectionStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApiCallAttemptTimeoutTrackingStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApiCallMetricCollectionStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApiCallTimeoutTrackingStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.BeforeTransmissionExecutionInterceptorsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.BeforeUnmarshallingExecutionInterceptorsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ExecutionFailureExceptionReportingStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.HandleResponseStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MakeHttpRequestStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.RetryableStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.SigningStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.TimeoutExceptionHandlingStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.UnwrapResponseContainer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.Pair;

/**
 * The sync API call pipeline, written in a straight line.
 *
 * <p>This is the same pipeline {@link AmazonSyncHttpClient} used to assemble from
 * {@code RequestPipelineBuilder} — the same stage classes, constructed the same way, executed in the
 * same order — with the composition machinery removed. The builder DSL assembled the chain out of
 * ~30 single-use builder objects and ~19 two-field pair nodes <i>per request</i>, and executing it
 * meant hopping through those pair nodes' polymorphic {@code execute} calls. Here the two sequential
 * runs (the eleven request-mutation stages, and the six attempt stages) are plain sequences of method
 * calls in {@link RequestMutationStages} and {@link AttemptStages}, and only the stages that carry
 * real behavior around an inner pipeline (retry, timeouts, metrics, stream management, failure
 * reporting) remain as wrapper objects, hand-nested once.
 *
 * <p>Two deliberate properties:
 *
 * <ul>
 *   <li><b>Stage logic is reused, not copied.</b> Every stage keeps a single implementation; this
 *       class only replaces how they are wired together. A change to a stage cannot silently diverge
 *       between the builder-composed and straight-line forms, because there is only one form of each
 *       stage.</li>
 *   <li><b>Construction stays per-request.</b> Stages capture {@link HttpClientDependencies}, and
 *       {@code BaseSyncClientHandler} hands this class a per-request dependencies object whose
 *       configuration may have been modified by plugins for this call. Hoisting stage construction to
 *       per-client would freeze the first request's configuration into every later request. The
 *       per-request cost this class removes is the composition, which was pure overhead; the stage
 *       objects themselves are small and their construction is trivial.</li>
 * </ul>
 *
 * <p>The nesting, outermost first, preserved exactly from the builder form:
 * failure-reporting → [ api-call metrics → api-call timeout → stream-managing →
 * [ 11 mutation stages → retry( attempt-metrics → timeout-exception-handling → attempt-timeout →
 * [ sign → before-transmission → http → after-transmission → before-unmarshalling →
 * handle-response ] ) ] → unwrap → after-execution ].
 */
@SdkInternalApi
final class SyncApiCallPipeline {

    private SyncApiCallPipeline() {
    }

    /**
     * Build the full pipeline for one request. Input is the marshalled {@link SdkHttpFullRequest};
     * output is the unmarshalled response, produced through {@code responseHandler}.
     */
    static <OutputT> RequestPipeline<SdkHttpFullRequest, OutputT> create(
            HttpClientDependencies dependencies,
            HttpResponseHandler<Response<OutputT>> responseHandler) {

        // Everything inside RetryableStage re-runs on every retry attempt.
        RequestPipeline<SdkHttpFullRequest, Response<OutputT>> attempt =
            new RetryableStage<>(dependencies,
                new ApiCallAttemptMetricCollectionStage<>(
                    new TimeoutExceptionHandlingStage<>(dependencies,
                        new ApiCallAttemptTimeoutTrackingStage<>(dependencies,
                            new AttemptStages<>(dependencies, responseHandler)))));

        RequestPipeline<SdkHttpFullRequest, Response<OutputT>> call =
            new ApiCallMetricCollectionStage<>(
                new ApiCallTimeoutTrackingStage<>(dependencies,
                    new StreamManagingStage<>(
                        new MutationAndCallStages<>(dependencies, attempt))));

        return new ExecutionFailureExceptionReportingStage<>(new FinishStages<>(call));
    }

    /**
     * The shared request-mutation sequence ({@link RequestMutationStages}) followed by the retryable
     * attempt block.
     */
    private static final class MutationAndCallStages<OutputT>
            implements RequestPipeline<SdkHttpFullRequest, Response<OutputT>> {

        private final RequestMutationStages mutation;
        private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> attempt;

        MutationAndCallStages(HttpClientDependencies dependencies,
                              RequestPipeline<SdkHttpFullRequest, Response<OutputT>> attempt) {
            this.mutation = new RequestMutationStages(dependencies, ClientType.SYNC);
            this.attempt = attempt;
        }

        @Override
        public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context)
                throws Exception {
            return attempt.execute(mutation.execute(request, context), context);
        }
    }

    /**
     * One HTTP attempt: sign, run transmission interceptors, make the call, handle the response.
     * Replaces six {@code .then(...)} compositions. Re-executed by {@link RetryableStage} on retries.
     */
    private static final class AttemptStages<OutputT>
            implements RequestPipeline<SdkHttpFullRequest, Response<OutputT>> {

        private final SigningStage signing;
        private final BeforeTransmissionExecutionInterceptorsStage beforeTransmission =
            new BeforeTransmissionExecutionInterceptorsStage();
        private final MakeHttpRequestStage makeHttpRequest;
        private final AfterTransmissionExecutionInterceptorsStage afterTransmission =
            new AfterTransmissionExecutionInterceptorsStage();
        private final BeforeUnmarshallingExecutionInterceptorsStage beforeUnmarshalling =
            new BeforeUnmarshallingExecutionInterceptorsStage();
        private final HandleResponseStage<OutputT> handleResponse;

        AttemptStages(HttpClientDependencies dependencies,
                      HttpResponseHandler<Response<OutputT>> responseHandler) {
            this.signing = new SigningStage(dependencies);
            this.makeHttpRequest = new MakeHttpRequestStage(dependencies);
            this.handleResponse = new HandleResponseStage<>(responseHandler);
        }

        @Override
        public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context)
                throws Exception {
            SdkHttpFullRequest signed = signing.execute(request, context);
            signed = beforeTransmission.execute(signed, context);
            Pair<SdkHttpFullRequest, SdkHttpFullResponse> exchange = makeHttpRequest.execute(signed, context);
            exchange = afterTransmission.execute(exchange, context);
            SdkHttpFullResponse httpResponse = beforeUnmarshalling.execute(exchange, context);
            return handleResponse.execute(httpResponse, context);
        }
    }

    /**
     * Unwrap the response container and run after-execution interceptors. Replaces the two trailing
     * {@code .then(...)} compositions on the outer builder.
     */
    private static final class FinishStages<OutputT> implements RequestPipeline<SdkHttpFullRequest, OutputT> {

        private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> call;
        private final UnwrapResponseContainer<OutputT> unwrap = new UnwrapResponseContainer<>();
        private final AfterExecutionInterceptorsStage<OutputT> afterExecution = new AfterExecutionInterceptorsStage<>();

        FinishStages(RequestPipeline<SdkHttpFullRequest, Response<OutputT>> call) {
            this.call = call;
        }

        @Override
        public OutputT execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
            OutputT output = unwrap.execute(call.execute(request, context), context);
            return afterExecution.execute(output, context);
        }
    }
}
