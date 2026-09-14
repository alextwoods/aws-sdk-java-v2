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


import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.util.ThrowableUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@ThreadSafe
@SdkInternalApi
//TODO: come up with better name
public final class AmazonAsyncHttpClient implements SdkAutoCloseable {
    private final HttpClientDependencies httpClientDependencies;
    /**
     * The stage graph per client configuration; see {@link PipelineCache}.
     */
    private final PipelineCache<RequestPipeline<SdkHttpFullRequest, ? extends CompletableFuture<?>>> pipelines;

    public AmazonAsyncHttpClient(SdkClientConfiguration clientConfiguration) {
        this.httpClientDependencies = HttpClientDependencies.builder()
                                                            .clientConfiguration(clientConfiguration)
                                                            .build();
        this.pipelines = new PipelineCache<>(httpClientDependencies, AsyncApiCallPipeline::create);
    }

    /**
     * Shuts down this HTTP client object, releasing any resources that might be held open. This is
     * an optional method, and callers are not expected to call it, but can if they want to
     * explicitly release any open resources. Once a client has been shutdown, it cannot be used to
     * make more requests.
     */
    @Override
    public void close() {
        httpClientDependencies.close();
    }

    /**
     * @return A builder used to configure and execute a HTTP request.
     */
    public RequestExecutionBuilder requestExecutionBuilder() {
        return new RequestExecutionBuilderImpl(this);
    }

    /**
     * Interface to configure a request execution and execute the request.
     */
    public interface RequestExecutionBuilder {
        /**
         * Fluent setter for {@link AsyncRequestBody}
         *
         * @param requestProvider Request provider object
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder requestProvider(AsyncRequestBody requestProvider);

        /**
         * Fluent setter for {@link SdkHttpFullRequest}
         *
         * @param request Request object
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder request(SdkHttpFullRequest request);

        /**
         * Fluent setter for the execution context
         *
         * @param executionContext Execution context
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder executionContext(ExecutionContext executionContext);

        /**
         * Fluent setter for {@link SdkRequest}
         *
         * @param originalRequest Request object
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder originalRequest(SdkRequest originalRequest);

        /**
         * The client configuration for this call: the client's own, or the one a request's plugins produced. The
         * stage graph for it is reused across calls (see {@link PipelineCache}); prefer this over supplying
         * dependencies, which builds a graph for this call only.
         */
        RequestExecutionBuilder clientConfiguration(SdkClientConfiguration clientConfiguration);

        RequestExecutionBuilder httpClientDependencies(HttpClientDependencies httpClientDependencies);

        HttpClientDependencies httpClientDependencies();

        default RequestExecutionBuilder httpClientDependencies(Consumer<HttpClientDependencies.Builder> mutator) {
            HttpClientDependencies.Builder builder = httpClientDependencies().toBuilder();
            mutator.accept(builder);
            return httpClientDependencies(builder.build());
        }

        /**
         * Executes the request with the given configuration.
         *
         * @param responseHandler Response handler that outputs the actual result type which is
         *                        preferred going forward.
         * @param <OutputT>       Result type
         * @return Unmarshalled result type.
         */
        <OutputT> CompletableFuture<OutputT> execute(TransformingAsyncResponseHandler<Response<OutputT>> responseHandler);
    }

    private static class RequestExecutionBuilderImpl implements RequestExecutionBuilder {

        private final AmazonAsyncHttpClient client;
        private HttpClientDependencies httpClientDependencies;
        private SdkClientConfiguration clientConfiguration;
        private AsyncRequestBody requestProvider;
        private SdkHttpFullRequest request;
        private SdkRequest originalRequest;
        private ExecutionContext executionContext;

        RequestExecutionBuilderImpl(AmazonAsyncHttpClient client) {
            this.client = client;
            this.httpClientDependencies = client.httpClientDependencies;
        }

        @Override
        public RequestExecutionBuilder clientConfiguration(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
            return this;
        }

        @Override
        public RequestExecutionBuilder httpClientDependencies(HttpClientDependencies httpClientDependencies) {
            this.httpClientDependencies = httpClientDependencies;
            this.clientConfiguration = null;
            return this;
        }

        @Override
        public HttpClientDependencies httpClientDependencies() {
            return httpClientDependencies;
        }

        @Override
        public RequestExecutionBuilder requestProvider(AsyncRequestBody requestProvider) {
            this.requestProvider = requestProvider;
            return this;
        }

        @Override
        public RequestExecutionBuilder request(SdkHttpFullRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public RequestExecutionBuilder executionContext(
                ExecutionContext executionContext) {
            this.executionContext = executionContext;
            return this;
        }

        @Override
        public RequestExecutionBuilder originalRequest(SdkRequest originalRequest) {
            this.originalRequest = originalRequest;
            return this;
        }

        @Override
        public <OutputT> CompletableFuture<OutputT> execute(
            TransformingAsyncResponseHandler<Response<OutputT>> responseHandler) {

            try {
                // The stage chain lives in AsyncApiCallPipeline as straight-line code, built once per client
                // configuration and reused; this call contributes only its context.
                @SuppressWarnings("unchecked")
                RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> pipeline =
                    (RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>>) prepared().pipeline();
                return pipeline.execute(request, createRequestExecutionDependencies(responseHandler));
            } catch (RuntimeException e) {
                throw ThrowableUtils.asSdkException(e);
            } catch (Exception e) {
                throw SdkClientException.builder().cause(e).build();
            }
        }

        private PipelineCache.Prepared<RequestPipeline<SdkHttpFullRequest, ? extends CompletableFuture<?>>> prepared() {
            if (clientConfiguration != null) {
                return client.pipelines.forConfiguration(clientConfiguration);
            }
            return client.pipelines.forDependencies(httpClientDependencies);
        }

        private RequestExecutionContext createRequestExecutionDependencies(Object responseHandler) {
            return RequestExecutionContext.builder()
                                          .requestProvider(requestProvider)
                                          .originalRequest(originalRequest)
                                          .executionContext(executionContext)
                                          .responseHandler(responseHandler)
                                          .build();
        }
    }
}
