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

package software.amazon.awssdk.dynamic.client;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.dynamic.model.DynamicModel;
import software.amazon.awssdk.dynamic.model.DynamicService;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.pipeline.AwsFeaturePlugin;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.client.AwsClientBuildState;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The options every dynamic client builder shares (Requirements 14.2-14.5, 14.14, 14.15): the runtime model and
 * service selection, and the same client options as a generated service's builder, forwarded to the same
 * {@link AwsClientBuildState}, which resolves the configuration once per client and builds the client's runtime and
 * engine.
 *
 * <p>The service is selected when the client is built: a model with exactly one service selects it without a
 * {@link #serviceId(ShapeId) service ID}; a model with several services requires one; an unknown ID, an ambiguous
 * model or a model without services fails the build with a
 * {@link software.amazon.awssdk.dynamic.model.DynamicModelException} listing every service shape ID in lexical
 * order, before any resource is acquired.
 *
 * <p>Not thread safe; one builder per client being built.
 *
 * @param <B> the concrete builder type
 * @param <C> the client type it builds
 */
public abstract sealed class DynamicClientBuilder<B extends DynamicClientBuilder<B, C>, C>
    permits DynamicClient.Builder, DynamicAsyncClient.Builder {

    final AwsClientBuildState state = new AwsClientBuildState();
    private Path modelPath;
    private DynamicModel model;
    private DynamicService service;
    private ShapeId serviceId;

    DynamicClientBuilder() {
    }

    /** The Smithy JSON AST model file (UTF-8) the client calls a service of; read when the client is built. */
    public B model(Path modelPath) {
        this.modelPath = Objects.requireNonNull(modelPath, "modelPath");
        this.model = null;
        this.service = null;
        return self();
    }

    /** The loaded runtime model the client calls a service of. */
    public B model(DynamicModel model) {
        this.model = Objects.requireNonNull(model, "model");
        this.modelPath = null;
        this.service = null;
        return self();
    }

    /**
     * The already selected runtime service the client calls; replaces any model. A {@link #serviceId(ShapeId)
     * service ID}, if set, must be its ID.
     */
    public B service(DynamicService service) {
        this.service = Objects.requireNonNull(service, "service");
        this.model = null;
        this.modelPath = null;
        return self();
    }

    /** The exact shape ID of the service to select; required when the model has more than one service. */
    public B serviceId(ShapeId serviceId) {
        this.serviceId = serviceId;
        return self();
    }

    /** The region the client sends requests to. */
    public B region(Region region) {
        state.region(region);
        return self();
    }

    /** The provider of the client's AWS credentials; without one, services with AWS auth use the default chain. */
    public B credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
        state.credentialsProvider(credentialsProvider);
        return self();
    }

    /** The endpoint every request is sent to, instead of the resolved one. */
    public B endpointOverride(URI endpointOverride) {
        state.endpointOverride(endpointOverride);
        return self();
    }

    /** The client's retry strategy. */
    public B retryStrategy(RetryStrategy retryStrategy) {
        state.retryStrategy(retryStrategy);
        return self();
    }

    /**
     * An explicit protocol implementation; without one, the installed protocol providers are discovered and one of
     * the service's declared protocols is selected, as for generated clients.
     */
    public B protocolProvider(ProtocolProvider protocolProvider) {
        state.protocolProvider(protocolProvider);
        return self();
    }

    /** The external configuration sources (JVM properties, environment, profile); system sources by default. */
    public B configurationSources(ConfigurationSources sources) {
        state.configurationSources(sources);
        return self();
    }

    /** Appends an interceptor of every call of the client. */
    public B addExecutionInterceptor(ExecutionInterceptor interceptor) {
        state.addInterceptor(interceptor);
        return self();
    }

    /** Appends a feature plugin of every operation plan of the client; plugin IDs must be unique. */
    public B addPlugin(AwsFeaturePlugin plugin) {
        state.addPlugin(plugin);
        return self();
    }

    /**
     * Selects the service, resolves the configuration once and builds the client.
     *
     * @throws software.amazon.awssdk.dynamic.model.DynamicModelException if the model is invalid or the service
     *         selection is ambiguous, unknown or impossible
     * @throws IllegalStateException if no model or service was given, or the service ID does not match the given
     *         service
     */
    public abstract C build();

    /** The selected service's binding, with its Modern Auth installed in {@link #state}. */
    final DynamicServiceBinding resolve() {
        DynamicService selected;
        if (service != null) {
            if (serviceId != null && !serviceId.equals(service.id())) {
                throw new IllegalStateException("The service ID " + serviceId + " is not the ID of the given service "
                                                + service.id());
            }
            selected = service;
        } else {
            DynamicModel loaded = model;
            if (loaded == null) {
                if (modelPath == null) {
                    throw new IllegalStateException("A dynamic client needs a model or a service");
                }
                loaded = DynamicModel.load(modelPath);
            }
            selected = serviceId == null ? loaded.service() : loaded.service(serviceId);
        }
        DynamicServiceBinding binding = DynamicServiceBinding.of(selected);
        binding.configure(state);
        return binding;
    }

    @SuppressWarnings("unchecked")
    final B self() {
        return (B) this;
    }
}
