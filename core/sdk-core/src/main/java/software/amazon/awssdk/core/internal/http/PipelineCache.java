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

import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.util.SingleEntryIdentityCache;

/**
 * The stage graph for a client configuration, built once per configuration instance and reused across calls.
 *
 * <p>Every call used to construct its own {@link HttpClientDependencies} (a copy of the client's with that call's
 * configuration substituted) and its own stage graph on top of it — around fifteen stage objects, several of whose
 * constructors read client options. The stages are stateless after construction and depend on nothing but the
 * dependencies, and the configuration they are built for is one instance for the life of the client unless a request
 * carries plugins (which produces a fresh configuration for that call). So the graph is keyed on the configuration's
 * identity: the common case hits, a plugin request builds its own graph as before, and a client whose every request
 * has plugins stops paying for a cache that never hits (see {@link SingleEntryIdentityCache}).
 *
 * @param <P> the pipeline type ({@code RequestPipeline<SdkHttpFullRequest, ?>} in its sync or async form)
 */
@SdkInternalApi
final class PipelineCache<P> {
    private final HttpClientDependencies baseDependencies;
    private final Function<HttpClientDependencies, P> factory;
    private final SingleEntryIdentityCache<SdkClientConfiguration, Prepared<P>> cache = new SingleEntryIdentityCache<>();

    PipelineCache(HttpClientDependencies baseDependencies, Function<HttpClientDependencies, P> factory) {
        this.baseDependencies = baseDependencies;
        this.factory = factory;
    }

    /**
     * The dependencies and pipeline for this configuration: the retained ones if it is the same instance as last
     * time, otherwise built now (the dependencies as a copy of the client's with this configuration substituted).
     */
    Prepared<P> forConfiguration(SdkClientConfiguration clientConfiguration) {
        return cache.get(clientConfiguration, this::build);
    }

    private Prepared<P> build(SdkClientConfiguration clientConfiguration) {
        HttpClientDependencies dependencies = clientConfiguration == baseDependencies.clientConfiguration()
                                              ? baseDependencies
                                              : baseDependencies.toBuilder().clientConfiguration(clientConfiguration).build();
        return forDependencies(dependencies);
    }

    /**
     * A pipeline over the given dependencies, built now and not retained — the path for callers that supply their
     * own dependencies rather than a configuration.
     */
    Prepared<P> forDependencies(HttpClientDependencies dependencies) {
        return new Prepared<>(dependencies, factory.apply(dependencies));
    }

    /**
     * A pipeline together with the dependencies it was built over.
     */
    static final class Prepared<P> {
        private final HttpClientDependencies dependencies;
        private final P pipeline;

        private Prepared(HttpClientDependencies dependencies, P pipeline) {
            this.dependencies = dependencies;
            this.pipeline = pipeline;
        }

        HttpClientDependencies dependencies() {
            return dependencies;
        }

        P pipeline() {
            return pipeline;
        }
    }
}
