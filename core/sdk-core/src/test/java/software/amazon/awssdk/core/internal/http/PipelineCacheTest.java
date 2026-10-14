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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;

/**
 * Unit tests for {@link PipelineCache}'s retention rules with a counting factory. Task 3.30 adds the client-level
 * tests (one stage graph per client configuration across sequential and concurrent sync and async calls).
 */
class PipelineCacheTest {
    private final SdkClientConfiguration handlerConfig = SdkClientConfiguration.builder().build();
    private final HttpClientDependencies dependencies =
        HttpClientDependencies.builder().clientConfiguration(handlerConfig).build();
    private final List<HttpClientDependencies> built = new ArrayList<>();
    private final PipelineCache<Object> cache = new PipelineCache<>(dependencies, this::build);

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
        int threads = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return cache.pipeline(config);
                }));
            }
            start.countDown();

            Set<Object> pipelines = new HashSet<>();
            for (Future<Object> result : results) {
                pipelines.add(result.get(10, TimeUnit.SECONDS));
            }
            assertThat(pipelines).hasSize(1);
            assertThat(cache.pipelinesBuilt()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
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
}
