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

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;

class PipelineCacheTest {

    private static SdkClientConfiguration config(String serviceName) {
        return SdkClientConfiguration.builder().option(SdkClientOption.SERVICE_NAME, serviceName).build();
    }

    private static HttpClientDependencies dependencies(SdkClientConfiguration config) {
        return HttpClientDependencies.builder().clientConfiguration(config).build();
    }

    /** The "pipeline" under test is just a record of which dependencies it was built over. */
    private static PipelineCache<HttpClientDependencies> cache(HttpClientDependencies base, AtomicInteger builds) {
        return new PipelineCache<>(base, deps -> {
            builds.incrementAndGet();
            return deps;
        });
    }

    @Test
    void sameConfiguration_reusesThePipelineAndDependencies() {
        SdkClientConfiguration clientConfig = config("svc");
        HttpClientDependencies base = dependencies(clientConfig);
        AtomicInteger builds = new AtomicInteger();
        PipelineCache<HttpClientDependencies> cache = cache(base, builds);

        // The generated client's configuration is a different instance from the one the handler was built with.
        SdkClientConfiguration perCall = clientConfig.toBuilder().option(SdkClientOption.SERVICE_NAME, "svc").build();
        PipelineCache.Prepared<HttpClientDependencies> first = cache.forConfiguration(perCall);
        PipelineCache.Prepared<HttpClientDependencies> second = cache.forConfiguration(perCall);

        assertThat(second).isSameAs(first);
        assertThat(builds.get()).isEqualTo(1);
        assertThat(first.dependencies().clientConfiguration()).isSameAs(perCall);
        assertThat(first.dependencies()).as("a copy of the client's, with this configuration").isNotSameAs(base);
        assertThat(first.dependencies().authSchemeResolutionCache())
            .as("per-client caches are shared across the copy").isSameAs(base.authSchemeResolutionCache());
        assertThat(first.dependencies().userAgentHeaderCache()).isSameAs(base.userAgentHeaderCache());
    }

    @Test
    void clientLevelConfiguration_usesTheClientDependenciesThemselves() {
        SdkClientConfiguration clientConfig = config("svc");
        HttpClientDependencies base = dependencies(clientConfig);
        PipelineCache<HttpClientDependencies> cache = cache(base, new AtomicInteger());

        assertThat(cache.forConfiguration(clientConfig).dependencies()).isSameAs(base);
    }

    @Test
    void differentConfiguration_buildsAFreshPipelineOverIt() {
        HttpClientDependencies base = dependencies(config("svc"));
        AtomicInteger builds = new AtomicInteger();
        PipelineCache<HttpClientDependencies> cache = cache(base, builds);

        SdkClientConfiguration a = config("a");
        SdkClientConfiguration b = config("b");   // what a request with plugins produces
        PipelineCache.Prepared<HttpClientDependencies> forA = cache.forConfiguration(a);
        PipelineCache.Prepared<HttpClientDependencies> forB = cache.forConfiguration(b);

        assertThat(forB).isNotSameAs(forA);
        assertThat(forB.dependencies().clientConfiguration()).isSameAs(b);
        assertThat(builds.get()).isEqualTo(2);
    }

    @Test
    void forDependencies_isNotRetained() {
        HttpClientDependencies base = dependencies(config("svc"));
        AtomicInteger builds = new AtomicInteger();
        PipelineCache<HttpClientDependencies> cache = cache(base, builds);

        HttpClientDependencies supplied = dependencies(config("supplied"));
        cache.forDependencies(supplied);
        cache.forDependencies(supplied);
        assertThat(builds.get()).isEqualTo(2);
    }
}
