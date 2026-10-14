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

package software.amazon.awssdk.core.internal.handler;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.CombinedResponseHandler;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.http.PipelineCache;
import software.amazon.awssdk.core.internal.http.SyncApiCallPipeline;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.schema.ApiOperation;

@SdkInternalApi
public abstract class BaseSyncClientHandler extends BaseClientHandler implements SyncClientHandler {
    private final AmazonSyncHttpClient client;
    private final PipelineCache<SyncApiCallPipeline> pipelines;

    protected BaseSyncClientHandler(SdkClientConfiguration clientConfiguration,
                                    AmazonSyncHttpClient client) {
        super(clientConfiguration);
        this.client = client;
        this.pipelines = PipelineCache.forSync(client.requestExecutionBuilder().httpClientDependencies());
    }

    /**
     * {@inheritDoc}
     *
     * <p>A call with an {@linkplain ClientExecutionParams#getOperation() operation} runs on the client's
     * {@link SyncApiCallPipeline}; any other call runs on the legacy pipeline.
     */
    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> ReturnT execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ResponseTransformer<OutputT, ReturnT> responseTransformer) {

        ApiOperation<?, ?> operation = executionParams.getOperation();
        if (operation != null) {
            return newPathPipeline(executionParams).execute(operation, executionParams,
                                                            this::invokeInterceptorsAndCreateExecutionContext,
                                                            responseTransformer);
        }

        return measureApiCallSuccess(executionParams, () -> {
            // Running beforeExecution interceptors and modifyRequest interceptors.
            ExecutionContext executionContext = invokeInterceptorsAndCreateExecutionContext(executionParams);

            CombinedResponseHandler<ReturnT> streamingCombinedResponseHandler =
                createStreamingCombinedResponseHandler(executionParams, responseTransformer, executionContext);
            return doExecute(executionParams, executionContext, streamingCombinedResponseHandler);
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>A call with an {@linkplain ClientExecutionParams#getOperation() operation} runs on the client's
     * {@link SyncApiCallPipeline}; any other call runs on the legacy pipeline.
     */
    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
        ClientExecutionParams<InputT, OutputT> executionParams) {

        ApiOperation<?, ?> operation = executionParams.getOperation();
        if (operation != null) {
            return newPathPipeline(executionParams).execute(operation, executionParams,
                                                            this::invokeInterceptorsAndCreateExecutionContext);
        }

        return measureApiCallSuccess(executionParams, () -> {
            // Running beforeExecution interceptors and modifyRequest interceptors.
            ExecutionContext executionContext = invokeInterceptorsAndCreateExecutionContext(executionParams);

            HttpResponseHandler<Response<OutputT>> combinedResponseHandler =
                createCombinedResponseHandler(executionParams, executionContext);
            return doExecute(executionParams, executionContext, combinedResponseHandler);
        });
    }

    @Override
    public void close() {
        client.close();
    }

    /**
     * The schema-based pipeline of the call's configuration. The pipeline reports {@code API_CALL_SUCCESSFUL} itself,
     * so the call is not wrapped in {@code measureApiCallSuccess}; only a failure to build the pipeline is reported
     * here, as the legacy handler reports any failure.
     */
    private SyncApiCallPipeline newPathPipeline(ClientExecutionParams<?, ?> executionParams) {
        try {
            return pipelines.pipeline(resolveRequestConfiguration(executionParams));
        } catch (RuntimeException e) {
            reportApiCallSuccess(executionParams, false);
            throw e;
        }
    }

    /**
     * Invoke the request using the http client. Assumes credentials (or lack thereof) have been
     * configured in the OldExecutionContext beforehand.
     **/
    private <OutputT> OutputT invoke(SdkClientConfiguration clientConfiguration,
                                     SdkHttpFullRequest request,
                                     SdkRequest originalRequest,
                                     ExecutionContext executionContext,
                                     HttpResponseHandler<Response<OutputT>> responseHandler) {
        return client.requestExecutionBuilder()
                     .request(request)
                     .originalRequest(originalRequest)
                     .executionContext(executionContext)
                     .httpClientDependencies(c -> c.clientConfiguration(clientConfiguration))
                     .execute(responseHandler);
    }

    private <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CombinedResponseHandler<ReturnT>
        createStreamingCombinedResponseHandler(ClientExecutionParams<InputT, OutputT> executionParams,
                                               ResponseTransformer<OutputT, ReturnT> responseTransformer,
                                               ExecutionContext executionContext) {
        if (executionParams.getCombinedResponseHandler() != null) {
            // There is no support for catching errors in a body for streaming responses
            throw new IllegalArgumentException("A streaming 'responseTransformer' may not be used when a "
                                               + "'combinedResponseHandler' has been specified in a "
                                               + "ClientExecutionParams object.");
        }

        HttpResponseHandler<OutputT> decoratedResponseHandlers =
            decorateResponseHandlers(executionParams.getResponseHandler(), executionContext);

        HttpResponseHandler<ReturnT> httpResponseHandler =
            new HttpResponseHandlerAdapter<>(decoratedResponseHandlers, responseTransformer);

        return new CombinedResponseHandler<>(httpResponseHandler, executionParams.getErrorResponseHandler());
    }

    private <InputT extends SdkRequest, OutputT extends SdkResponse> HttpResponseHandler<Response<OutputT>>
        createCombinedResponseHandler(ClientExecutionParams<InputT, OutputT> executionParams,
                                      ExecutionContext executionContext) {
        validateCombinedResponseHandler(executionParams);
        HttpResponseHandler<Response<OutputT>> combinedResponseHandler;
        if (executionParams.getCombinedResponseHandler() != null) {
            combinedResponseHandler = decorateSuccessResponseHandlers(executionParams.getCombinedResponseHandler(),
                                                                      executionContext);
        } else {
            HttpResponseHandler<OutputT> decoratedResponseHandlers =
                decorateResponseHandlers(executionParams.getResponseHandler(), executionContext);

            combinedResponseHandler = new CombinedResponseHandler<>(decoratedResponseHandlers,
                                                                    executionParams.getErrorResponseHandler());
        }
        return combinedResponseHandler;
    }

    private <InputT extends SdkRequest, OutputT, ReturnT> ReturnT doExecute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ExecutionContext executionContext,
        HttpResponseHandler<Response<ReturnT>> responseHandler) {

        InputT inputT = (InputT) executionContext.interceptorContext().request();

        InterceptorContext sdkHttpFullRequestContext = finalizeSdkHttpFullRequest(executionParams,
                                                                                  executionContext,
                                                                                  inputT,
                                                                                  resolveRequestConfiguration(
                                                                                      executionParams));

        SdkHttpFullRequest marshalled = (SdkHttpFullRequest) sdkHttpFullRequestContext.httpRequest();

        // Ensure that the signing configuration is still valid after the
        // request has been potentially transformed.
        validateSigningConfiguration(marshalled, executionContext.signer());

        // TODO Pass requestBody as separate arg to invoke
        Optional<RequestBody> requestBody = sdkHttpFullRequestContext.requestBody();

        if (requestBody.isPresent()) {
            marshalled = marshalled.toBuilder()
                                   .contentStreamProvider(requestBody.get().contentStreamProvider())
                                   .build();
        }

        SdkClientConfiguration clientConfiguration = resolveRequestConfiguration(executionParams);
        return invoke(clientConfiguration,
                      marshalled,
                      inputT,
                      executionContext,
                      responseHandler);
    }

    private <T> T measureApiCallSuccess(ClientExecutionParams<?, ?> executionParams, Supplier<T> thingToMeasureSuccessOf) {
        try {
            T result = thingToMeasureSuccessOf.get();
            reportApiCallSuccess(executionParams, true);
            return result;
        } catch (Exception e) {
            reportApiCallSuccess(executionParams, false);
            throw e;
        }
    }

    private void reportApiCallSuccess(ClientExecutionParams<?, ?> executionParams, boolean value) {
        MetricCollector metricCollector = executionParams.getMetricCollector();
        if (metricCollector != null) {
            metricCollector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, value);
        }
    }

    private static class HttpResponseHandlerAdapter<ReturnT, OutputT extends SdkResponse>
        implements HttpResponseHandler<ReturnT> {

        private final HttpResponseHandler<OutputT> httpResponseHandler;
        private final ResponseTransformer<OutputT, ReturnT> responseTransformer;

        private HttpResponseHandlerAdapter(HttpResponseHandler<OutputT> httpResponseHandler,
                                           ResponseTransformer<OutputT, ReturnT> responseTransformer) {
            this.httpResponseHandler = httpResponseHandler;
            this.responseTransformer = responseTransformer;
        }

        @Override
        public ReturnT handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
            OutputT resp = httpResponseHandler.handle(response, executionAttributes);
            return transformResponse(resp, response.content().orElseGet(AbortableInputStream::createEmpty));
        }

        @Override
        public boolean needsConnectionLeftOpen() {
            return responseTransformer.needsConnectionLeftOpen();
        }


        private ReturnT transformResponse(OutputT resp, AbortableInputStream inputStream) throws Exception {
            try {
                InterruptMonitor.checkInterrupted();
                ReturnT result = responseTransformer.transform(resp, inputStream);
                InterruptMonitor.checkInterrupted();
                return result;
            }  catch (RetryableException | InterruptedException | AbortedException e) {
                throw e;
            } catch (Exception e) {
                InterruptMonitor.checkInterrupted();
                throw NonRetryableException.builder().cause(e).build();
            }
        }
    }
}
