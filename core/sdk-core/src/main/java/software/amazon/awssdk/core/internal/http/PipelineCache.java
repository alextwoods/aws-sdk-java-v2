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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Validate;

/**
 * The schema-based pipeline ({@link SyncApiCallPipeline} or {@link AsyncApiCallPipeline}) of a client, built once per
 * client configuration and reused by every call (design D.6, racecar H11, re-implemented). One instance is held by each
 * client handler.
 *
 * <p>The configuration a call runs with is the one the generated client passes as its request configuration. It is the
 * same instance on every call unless the request carries plugins, which produce a new configuration for that call only.
 * The cache therefore retains the pipeline of one configuration, compared by identity:
 * <ul>
 *     <li>a call with the retained configuration reuses its pipeline, with no lookup beyond one reference comparison;</li>
 *     <li>once a retained configuration has been used by a second call, it is the client's configuration and stays
 *     retained; a call with any other configuration (a request with plugins) builds a pipeline for that call only,
 *     as the legacy {@code AmazonSyncHttpClient}/{@code AmazonAsyncHttpClient} build their stage graph for every
 *     call;</li>
 *     <li>until then, a call with another configuration replaces the retained one, so a first call that carries
 *     plugins does not keep the client's own configuration from being retained.</li>
 * </ul>
 *
 * <p>Concurrent first calls with the same configuration build one pipeline: the retained entry holds a {@link Lazy},
 * and a thread that loses the race to retain its entry uses the winner's.
 *
 * <p>A pipeline for a configuration other than the client's is built over a copy of the client's
 * {@link HttpClientDependencies} with that configuration, as the legacy handlers do, so both share the client's clock
 * skew adjustment. The cache owns no resources: the client handler closes the dependencies.
 *
 * @param <P> the pipeline type
 */
@SdkInternalApi
@ThreadSafe
public final class PipelineCache<P> {
    private final HttpClientDependencies clientDependencies;
    private final Function<HttpClientDependencies, P> factory;
    private final AtomicReference<Entry<P>> retained = new AtomicReference<>();
    private final AtomicInteger pipelinesBuilt = new AtomicInteger();

    @SdkTestInternalApi
    PipelineCache(HttpClientDependencies clientDependencies, Function<HttpClientDependencies, P> factory) {
        this.clientDependencies = Validate.paramNotNull(clientDependencies, "clientDependencies");
        this.factory = Validate.paramNotNull(factory, "factory");
    }

    /**
     * The cache of a sync client handler.
     *
     * @param clientDependencies the dependencies of the handler's legacy HTTP client
     */
    public static PipelineCache<SyncApiCallPipeline> forSync(HttpClientDependencies clientDependencies) {
        return new PipelineCache<>(clientDependencies, SyncApiCallPipeline::create);
    }

    /**
     * The cache of an async client handler.
     *
     * @param clientDependencies the dependencies of the handler's legacy HTTP client
     */
    public static PipelineCache<AsyncApiCallPipeline> forAsync(HttpClientDependencies clientDependencies) {
        return new PipelineCache<>(clientDependencies, AsyncApiCallPipeline::create);
    }

    /**
     * Returns the pipeline of a call's configuration: the retained one when the configuration is the retained
     * instance, otherwise one built now (and retained, under the rules in the class documentation).
     *
     * @param configuration the call's resolved client configuration
     * @throws IllegalStateException if the pipeline cannot be built for the configuration (see
     *     {@link SyncApiCallPipeline#create(HttpClientDependencies)} and
     *     {@link AsyncApiCallPipeline#create(HttpClientDependencies)}); a later call tries again
     */
    public P pipeline(SdkClientConfiguration configuration) {
        Entry<P> current = retained.get();
        Entry<P> candidate = null;
        while (true) {
            if (current != null && current.configuration == configuration) {
                return current.reuse();
            }
            if (current != null && current.reused) {
                // The client's configuration is retained; this call's configuration is its own.
                return build(configuration);
            }
            if (candidate == null) {
                candidate = new Entry<>(configuration, new Lazy<>(() -> build(configuration)));
            }
            if (retained.compareAndSet(current, candidate)) {
                return candidate.pipeline.getValue();
            }
            current = retained.get();
        }
    }

    /**
     * The number of pipelines this cache has built (test seam for task 3.30).
     */
    @SdkTestInternalApi
    int pipelinesBuilt() {
        return pipelinesBuilt.get();
    }

    private P build(SdkClientConfiguration configuration) {
        HttpClientDependencies dependencies =
            configuration == clientDependencies.clientConfiguration()
            ? clientDependencies
            : clientDependencies.toBuilder().clientConfiguration(configuration).build();
        P pipeline = factory.apply(dependencies);
        pipelinesBuilt.incrementAndGet();
        return pipeline;
    }

    /**
     * A configuration and its lazily built pipeline.
     */
    private static final class Entry<P> {
        private final SdkClientConfiguration configuration;
        private final Lazy<P> pipeline;

        /**
         * Whether a second call used this entry. Written once, so the steady-state hit path only reads it.
         */
        private volatile boolean reused;

        private Entry(SdkClientConfiguration configuration, Lazy<P> pipeline) {
            this.configuration = configuration;
            this.pipeline = pipeline;
        }

        private P reuse() {
            if (!reused) {
                reused = true;
            }
            return pipeline.getValue();
        }
    }
}
