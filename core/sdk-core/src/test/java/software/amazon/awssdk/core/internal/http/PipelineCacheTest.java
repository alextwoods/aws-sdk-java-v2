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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.OPERATION;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SdkAsyncClientHandler;
import software.amazon.awssdk.core.client.handler.SdkSyncClientHandler;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.HttpStubProtocol;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestRequest;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.IoUtils;
import utils.HttpTestUtils;

/**
 * Tests that the schema-based stage graph is built once per client configuration (Requirement 37.4).
 *
 * <p>The first tests cover {@link PipelineCache}'s retention rules with a counting factory. The client-level tests run
 * real calls through the SDK client handlers on the new path ({@link ClientExecutionParams#withOperation}), sequentially
 * and from concurrent threads, sync and async, and count every {@link SyncApiCallPipeline} and
 * {@link AsyncApiCallPipeline} construction: each pipeline creates all of its stage objects in its constructor, so an
 * unchanged count proves a call constructed no stage graph.
 */
class PipelineCacheTest {
    private static final long TIMEOUT_SECONDS = 10;
    private static final int THREADS = 16;
    private static final int CALLS_PER_THREAD = 20;
    private static final int SEQUENTIAL_CALLS = 50;

    private final SdkClientConfiguration handlerConfig = SdkClientConfiguration.builder().build();
    private final HttpClientDependencies dependencies =
        HttpClientDependencies.builder().clientConfiguration(handlerConfig).build();
    private final List<HttpClientDependencies> built = new ArrayList<>();
    private final PipelineCache<Object> cache = new PipelineCache<>(dependencies, this::build);
    private final List<AutoCloseable> handlers = new ArrayList<>();

    @AfterEach
    void tearDown() throws Exception {
        for (AutoCloseable handler : handlers) {
            handler.close();
        }
    }

    @Test
    void pipeline_sameConfiguration_isBuiltOnceAndReused() {
        SdkClientConfiguration config = clientConfig();

        Object first = cache.pipeline(config);

        for (int i = 0; i < 10; i++) {
            assertThat(cache.pipeline(config)).isSameAs(first);
        }
        assertThat(cache.pipelinesBuilt()).isEqualTo(1);
    }

    @Test
    void pipeline_handlerConfiguration_usesTheHandlerDependencies() {
        cache.pipeline(handlerConfig);

        assertThat(built).containsExactly(dependencies);
    }

    @Test
    void pipeline_otherConfiguration_isBuiltOverACopyOfTheHandlerDependencies() {
        SdkClientConfiguration config = clientConfig();

        cache.pipeline(config);

        assertThat(built).hasSize(1);
        assertThat(built.get(0).clientConfiguration()).isSameAs(config);
        assertThat(built.get(0).clockSkewAdjuster()).isSameAs(dependencies.clockSkewAdjuster());
    }

    @Test
    void pipeline_otherConfigurationAfterTheRetainedOneWasReused_isBuiltForThatCallOnly() {
        SdkClientConfiguration config = clientConfig();
        Object retained = cache.pipeline(config);
        cache.pipeline(config);

        Object pluginPipeline = cache.pipeline(clientConfig());
        Object againPlugin = cache.pipeline(clientConfig());

        assertThat(pluginPipeline).isNotSameAs(retained);
        assertThat(againPlugin).isNotSameAs(pluginPipeline);
        assertThat(cache.pipeline(config)).isSameAs(retained);
        assertThat(cache.pipelinesBuilt()).isEqualTo(3);
    }

    @Test
    void pipeline_firstCallWithAnotherConfiguration_doesNotKeepTheClientConfigurationFromBeingRetained() {
        SdkClientConfiguration pluginConfig = clientConfig();
        SdkClientConfiguration config = clientConfig();
        cache.pipeline(pluginConfig);

        Object retained = cache.pipeline(config);

        for (int i = 0; i < 10; i++) {
            assertThat(cache.pipeline(config)).isSameAs(retained);
        }
        assertThat(cache.pipelinesBuilt()).isEqualTo(2);
    }

    @Test
    void pipeline_whenTheBuildFails_throwsAndBuildsOnTheNextCall() {
        AtomicInteger attempts = new AtomicInteger();
        PipelineCache<Object> failingOnce = new PipelineCache<>(dependencies, d -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("no transport");
            }
            return new Object();
        });
        SdkClientConfiguration config = clientConfig();

        assertThatThrownBy(() -> failingOnce.pipeline(config)).isInstanceOf(IllegalStateException.class)
                                                               .hasMessage("no transport");
        Object pipeline = failingOnce.pipeline(config);

        assertThat(failingOnce.pipeline(config)).isSameAs(pipeline);
        assertThat(failingOnce.pipelinesBuilt()).isEqualTo(1);
    }

    @Test
    void pipeline_concurrentFirstCallsWithOneConfiguration_buildOnePipeline() throws Exception {
        SdkClientConfiguration config = clientConfig();

        List<Object> pipelines = runConcurrently(THREADS, 1, () -> cache.pipeline(config));

        assertThat(new HashSet<>(pipelines)).hasSize(1);
        assertThat(cache.pipelinesBuilt()).isEqualTo(1);
    }

    @Test
    void syncExecute_sequentialCalls_buildOneStageGraphOnTheFirstCall() {
        ScriptedExchange exchange = respondTimes(new ScriptedExchange(), SEQUENTIAL_CALLS);
        SyncClient client = syncClient(exchange);
        StageGraphCount count = new StageGraphCount();

        assertThat(client.call().value()).isEqualTo("ok");
        assertThat(count.syncBuilt()).isEqualTo(1);
        for (int i = 1; i < SEQUENTIAL_CALLS; i++) {
            assertThat(client.call().value()).isEqualTo("ok");
        }

        assertThat(count.syncBuilt()).as("no call after the first builds a stage graph").isEqualTo(1);
        assertThat(count.asyncBuilt()).isZero();
        assertThat(exchange.requests()).hasSize(SEQUENTIAL_CALLS);
    }

    @Test
    void syncExecute_concurrentCalls_buildOneStageGraph() throws Exception {
        ScriptedExchange exchange = respondTimes(new ScriptedExchange(), THREADS * CALLS_PER_THREAD);
        SyncClient client = syncClient(exchange);
        StageGraphCount count = new StageGraphCount();

        List<TestResponse> outputs = runConcurrently(THREADS, CALLS_PER_THREAD, client::call);

        assertThat(outputs).hasSize(THREADS * CALLS_PER_THREAD).allSatisfy(o -> assertThat(o.value()).isEqualTo("ok"));
        assertThat(count.syncBuilt()).isEqualTo(1);
        assertThat(count.asyncBuilt()).isZero();
        assertThat(exchange.requests()).hasSize(THREADS * CALLS_PER_THREAD);
    }

    @Test
    void syncExecute_clientWithAnotherConfiguration_buildsItsOwnStageGraphOnce() {
        ScriptedExchange first = respondTimes(new ScriptedExchange(), 10);
        ScriptedExchange second = respondTimes(new ScriptedExchange(), 10);
        SyncClient firstClient = syncClient(first);
        SyncClient secondClient = syncClient(second);
        StageGraphCount count = new StageGraphCount();

        for (int i = 0; i < 10; i++) {
            firstClient.call();
            secondClient.call();
        }

        assertThat(count.syncBuilt()).as("one stage graph per client configuration").isEqualTo(2);
        assertThat(first.requests()).as("each client's calls run on its own graph and transport").hasSize(10);
        assertThat(second.requests()).hasSize(10);
    }

    @Test
    void syncExecute_requestsWithPlugins_buildAStageGraphPerCallAndReuseTheClients() {
        ScriptedExchange exchange = respondTimes(new ScriptedExchange(), 10);
        SyncClient client = syncClient(exchange);
        StageGraphCount count = new StageGraphCount();
        client.call();
        client.call();

        for (int i = 0; i < 3; i++) {
            // A request with plugins runs with a configuration of its own, as the generated client builds it per call.
            client.call(client.requestConfig.toBuilder().build());
        }
        for (int i = 0; i < 5; i++) {
            client.call();
        }

        assertThat(count.syncBuilt()).as("the client's graph plus one per call with plugins").isEqualTo(4);
        assertThat(exchange.requests()).hasSize(10);
    }

    @Test
    void asyncExecute_sequentialCalls_buildOneStageGraphOnTheFirstCall() throws Exception {
        ScriptedExchange exchange = respondTimes(new ScriptedExchange(), SEQUENTIAL_CALLS);
        AsyncClient client = asyncClient(exchange);
        StageGraphCount count = new StageGraphCount();

        assertThat(client.call().value()).isEqualTo("ok");
        assertThat(count.asyncBuilt()).isEqualTo(1);
        for (int i = 1; i < SEQUENTIAL_CALLS; i++) {
            assertThat(client.call().value()).isEqualTo("ok");
        }

        assertThat(count.asyncBuilt()).as("no call after the first builds a stage graph").isEqualTo(1);
        assertThat(count.syncBuilt()).isZero();
        assertThat(exchange.requests()).hasSize(SEQUENTIAL_CALLS);
    }

    @Test
    void asyncExecute_concurrentCalls_buildOneStageGraph() throws Exception {
        ScriptedExchange exchange = respondTimes(new ScriptedExchange(), THREADS * CALLS_PER_THREAD);
        AsyncClient client = asyncClient(exchange);
        StageGraphCount count = new StageGraphCount();

        List<TestResponse> outputs = runConcurrently(THREADS, CALLS_PER_THREAD, client::call);

        assertThat(outputs).hasSize(THREADS * CALLS_PER_THREAD).allSatisfy(o -> assertThat(o.value()).isEqualTo("ok"));
        assertThat(count.asyncBuilt()).isEqualTo(1);
        assertThat(count.syncBuilt()).isZero();
        assertThat(exchange.requests()).hasSize(THREADS * CALLS_PER_THREAD);
    }

    @Test
    void asyncExecute_clientWithAnotherConfiguration_buildsItsOwnStageGraphOnce() throws Exception {
        ScriptedExchange first = respondTimes(new ScriptedExchange(), 10);
        ScriptedExchange second = respondTimes(new ScriptedExchange(), 10);
        AsyncClient firstClient = asyncClient(first);
        AsyncClient secondClient = asyncClient(second);
        StageGraphCount count = new StageGraphCount();

        for (int i = 0; i < 10; i++) {
            firstClient.call();
            secondClient.call();
        }

        assertThat(count.asyncBuilt()).as("one stage graph per client configuration").isEqualTo(2);
        assertThat(first.requests()).as("each client's calls run on its own graph and transport").hasSize(10);
        assertThat(second.requests()).hasSize(10);
    }

    private synchronized Object build(HttpClientDependencies pipelineDependencies) {
        built.add(pipelineDependencies);
        return new Object();
    }

    /**
     * A configuration derived from the handler's, as the generated client's (and a request with plugins') is.
     */
    private SdkClientConfiguration clientConfig() {
        return handlerConfig.toBuilder().option(SdkClientOption.SERVICE_NAME, "Thing").build();
    }

    /**
     * A sync client over the exchange's transport: a handler built with the client's configuration, and the
     * configuration the generated client passes with every call (the handler's plus its own options, as the generated
     * client adds {@code SDK_CLIENT}).
     */
    private SyncClient syncClient(ScriptedExchange exchange) {
        SdkClientConfiguration config = baseHandlerConfig()
            .option(SdkClientOption.CLIENT_TRANSPORT, exchange.transport())
            .option(SdkClientOption.SYNC_HTTP_CLIENT, exchange.httpClient())
            .build();
        SyncClientHandler handler = new SdkSyncClientHandler(config) {
        };
        handlers.add(handler);
        return new SyncClient(handler, withClientOptions(config));
    }

    private AsyncClient asyncClient(ScriptedExchange exchange) {
        SdkClientConfiguration config = baseHandlerConfig()
            .option(SdkClientOption.ASYNC_CLIENT_TRANSPORT, exchange.asyncTransport())
            .option(SdkClientOption.ASYNC_HTTP_CLIENT, exchange.asyncHttpClient())
            .build();
        AsyncClientHandler handler = new SdkAsyncClientHandler(config);
        handlers.add(handler);
        return new AsyncClient(handler, withClientOptions(config));
    }

    private static SdkClientConfiguration.Builder baseHandlerConfig() {
        return HttpTestUtils.testClientConfiguration().toBuilder()
                            .option(SdkClientOption.RETRY_STRATEGY, DefaultRetryStrategy.doNotRetry())
                            .option(SdkClientOption.CLIENT_USER_AGENT, ApiCallStagesTestSupport.CLIENT_USER_AGENT)
                            .option(SdkClientOption.CLIENT_PROTOCOL, new EchoProtocol());
    }

    private static SdkClientConfiguration withClientOptions(SdkClientConfiguration handlerConfiguration) {
        return handlerConfiguration.toBuilder().option(SdkClientOption.SERVICE_NAME, "Thing").build();
    }

    private static ScriptedExchange respondTimes(ScriptedExchange exchange, int times) {
        for (int i = 0; i < times; i++) {
            exchange.respond(200, "ok");
        }
        return exchange;
    }

    private static ClientExecutionParams<TestRequest, TestResponse> params(SdkClientConfiguration requestConfig) {
        return new ClientExecutionParams<TestRequest, TestResponse>().withOperation(OPERATION)
                                                                     .withInput(new TestRequest("name"))
                                                                     .withRequestConfiguration(requestConfig);
    }

    /**
     * Runs {@code callsPerThread} calls on each of {@code threads} threads, all released together by a start gate, and
     * returns every result.
     */
    private static <T> List<T> runConcurrently(int threads, int callsPerThread, Callable<T> call) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<List<T>>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    List<T> results = new ArrayList<>();
                    for (int j = 0; j < callsPerThread; j++) {
                        results.add(call.call());
                    }
                    return results;
                }));
            }
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<List<T>> future : futures) {
                results.addAll(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * The stage graphs constructed since this instance was created.
     */
    private static final class StageGraphCount {
        private final long sync = SyncApiCallPipeline.constructions();
        private final long async = AsyncApiCallPipeline.constructions();

        long syncBuilt() {
            return SyncApiCallPipeline.constructions() - sync;
        }

        long asyncBuilt() {
            return AsyncApiCallPipeline.constructions() - async;
        }
    }

    private static final class SyncClient {
        private final SyncClientHandler handler;
        private final SdkClientConfiguration requestConfig;

        private SyncClient(SyncClientHandler handler, SdkClientConfiguration requestConfig) {
            this.handler = handler;
            this.requestConfig = requestConfig;
        }

        TestResponse call() {
            return call(requestConfig);
        }

        TestResponse call(SdkClientConfiguration callConfig) {
            return handler.execute(params(callConfig));
        }
    }

    private static final class AsyncClient {
        private final AsyncClientHandler handler;
        private final SdkClientConfiguration requestConfig;

        private AsyncClient(AsyncClientHandler handler, SdkClientConfiguration requestConfig) {
            this.handler = handler;
            this.requestConfig = requestConfig;
        }

        TestResponse call() throws Exception {
            return handler.execute(params(requestConfig)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * An HTTP protocol whose output's value is the response body.
     */
    private static final class EchoProtocol extends HttpStubProtocol {
        @Override
        @SuppressWarnings("unchecked")
        public <I extends SerializableStruct, O extends SerializableStruct> O deserializeResponse(
            ApiOperation<I, O> operation, ExecutionAttributes context, TypeRegistry errorRegistry,
            SdkHttpFullRequest request, SdkHttpFullResponse response) {
            try {
                return (O) TestResponse.builder().value(IoUtils.toUtf8String(response.content().get())).build();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
