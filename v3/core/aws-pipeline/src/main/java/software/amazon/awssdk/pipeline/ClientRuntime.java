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

package software.amazon.awssdk.pipeline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.core.resource.ResourceScope;
import software.amazon.awssdk.endpoints.binding.EndpointClientValues;
import software.amazon.awssdk.endpoints.rules.EndpointRuleSet;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.ProtocolTransportRequirements;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.telemetry.spi.TelemetryOptions;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The immutable runtime of one client (design "Core runtime objects"; Requirements 9.5, 10.1, 10.11): the resolved
 * configuration, the client-constant components every call uses (protocol runtime, transport mode, retry strategy
 * and scope, endpoint rule set and client values, auth-scheme selector, completion executor, retry scheduler, feature
 * plugins), the client's owned resources, and the cache of one {@link PipelinePlan} per operation.
 *
 * <p>{@link #plan(AwsOperation)} builds an operation's plan the first time it is asked for and returns that same
 * instance for the client's lifetime, so the protocol plan, feature subplans, context layout, stage metadata and
 * plugin contributions are resolved exactly once per client and operation; calls never construct plans. Plans are
 * built lazily so a client of a large service pays only for the operations it calls; generated clients may hold the
 * returned plans in final fields.
 *
 * <p>Thread safe. Holds no customer data. {@link #close()} closes the SDK-owned resources exactly once.
 */
public final class ClientRuntime implements AutoCloseable {
    private final SdkClientConfiguration configuration;
    private final CallOptions defaultCallOptions;
    private final ClientProtocol protocol;
    private final HttpTransportMode transportMode;
    private final ProtocolTransportRequirements transportRequirements;
    private final boolean eventStreamOperations;
    private final RetryStrategy retryStrategy;
    private final String retryScope;
    private final EndpointRuleSet endpointRules;
    private final EndpointClientValues endpointClientValues;
    private final AuthSchemeSelector authSchemeSelector;
    private final Executor completionExecutor;
    private final RetryScheduler retryScheduler;
    private final List<AwsFeaturePlugin> plugins;
    private final List<ExecutionInterceptor> interceptors;
    /** The client-constant user-agent dimensions; null when the client sends no SDK user agent. */
    private final UserAgent userAgent;
    /** The client's telemetry plan; null (no telemetry work at all) unless a publisher is configured. */
    private final PipelineTelemetry telemetry;
    private final ResourceScope resources;
    private final ConcurrentHashMap<AwsOperation<?, ?>, PipelinePlan<?, ?>> plans = new ConcurrentHashMap<>();

    private ClientRuntime(Builder builder) {
        this.configuration = Objects.requireNonNull(builder.configuration, "configuration");
        this.protocol = Objects.requireNonNull(builder.protocol, "protocol");
        this.transportMode = Objects.requireNonNull(builder.transportMode, "transportMode");
        this.transportRequirements = builder.transportRequirements;
        this.eventStreamOperations = builder.eventStreamOperations;
        if (transportRequirements != null && !transportRequirements.protocolId().equals(protocol.protocolId())) {
            throw new IllegalArgumentException("The transport requirements of " + transportRequirements.protocolId()
                                               + " do not belong to the client's protocol " + protocol.protocolId());
        }
        this.retryStrategy = Objects.requireNonNull(builder.retryStrategy, "retryStrategy");
        this.retryScope = Objects.requireNonNull(builder.retryScope, "retryScope");
        this.endpointRules = builder.endpointRules;
        this.endpointClientValues = builder.endpointClientValues;
        this.authSchemeSelector = builder.authSchemeSelector;
        this.completionExecutor = builder.completionExecutor;
        this.retryScheduler = builder.retryScheduler;
        this.plugins = List.copyOf(builder.plugins);
        this.interceptors = List.copyOf(builder.interceptors);
        this.userAgent = builder.userAgent;
        this.telemetry = PipelineTelemetry.forClient(
            configuration.get(TelemetryOptions.TELEMETRY).orElse(null),
            builder.telemetryService != null ? builder.telemetryService : retryScope,
            builder.telemetryHttpImplementation != null ? builder.telemetryHttpImplementation
                                                        : PipelineTelemetry.NO_HTTP_IMPLEMENTATION,
            retryStrategy);
        this.defaultCallOptions = CallOptions.of(configuration);
        this.resources = ResourceScope.of(configuration.resources());
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The plan of {@code operation} for this client: built on first use, then the same instance for every call.
     *
     * @throws software.amazon.awssdk.core.exception.ModelConflictException if the operation's modeled features
     *     conflict; nothing is cached and nothing can be transmitted
     * @throws IllegalArgumentException if the client's protocol cannot serve the operation
     */
    @SuppressWarnings("unchecked")
    public <I extends SerializableStruct, O extends SerializableStruct> PipelinePlan<I, O> plan(
        AwsOperation<I, O> operation) {
        Objects.requireNonNull(operation, "operation");
        PipelinePlan<?, ?> plan = plans.get(operation);
        if (plan == null) {
            plan = plans.computeIfAbsent(operation, op -> PipelinePlan.create(this, operation));
        }
        return (PipelinePlan<I, O>) plan;
    }

    /** The number of plans built so far; for diagnostics and tests. */
    int builtPlans() {
        return plans.size();
    }

    /** The client's resolved configuration. */
    public SdkClientConfiguration configuration() {
        return configuration;
    }

    /** The call options of a call without overrides: reads go to the client configuration. Created once. */
    public CallOptions callOptions() {
        return defaultCallOptions;
    }

    /**
     * The call options of one call: the client configuration with the request-embedded and then the explicit
     * operation-overload override layered over it (either may be {@code null}); the shared default instance when
     * neither overrides anything. Costs time proportional to the overrides only.
     */
    public CallOptions callOptions(RequestOverrideConfiguration embedded, RequestOverrideConfiguration explicit) {
        return defaultCallOptions.withOverrides(embedded, explicit);
    }

    /** The client's protocol runtime. */
    public ClientProtocol protocol() {
        return protocol;
    }

    /** Whether the client executes through a sync or an async transport. */
    public HttpTransportMode transportMode() {
        return transportMode;
    }

    /**
     * What the client's protocol requires of the transport, if configured; {@link AwsCallEngine} checks it against
     * the transport's capabilities before it returns (Requirement 10.13).
     */
    public Optional<ProtocolTransportRequirements> transportRequirements() {
        return Optional.ofNullable(transportRequirements);
    }

    /** Whether the client has event-stream operations, whose HTTP versions the transport must also support. */
    public boolean eventStreamOperations() {
        return eventStreamOperations;
    }

    /** The client's retry strategy, shared by every call. */
    public RetryStrategy retryStrategy() {
        return retryStrategy;
    }

    /** The retry-quota scope of the client's calls. */
    public String retryScope() {
        return retryScope;
    }

    /** The service's endpoint rule set, if the client resolves endpoints from rules. */
    public Optional<EndpointRuleSet> endpointRules() {
        return Optional.ofNullable(endpointRules);
    }

    /** The client-constant endpoint built-ins and client context parameters. */
    public EndpointClientValues endpointClientValues() {
        return endpointClientValues;
    }

    /** The client's auth-scheme selector, if configured. */
    public Optional<AuthSchemeSelector> authSchemeSelector() {
        return Optional.ofNullable(authSchemeSelector);
    }

    /** The executor async calls complete and run codec/interceptor work on, if configured. */
    public Optional<Executor> completionExecutor() {
        return Optional.ofNullable(completionExecutor);
    }

    /** The scheduler of non-blocking retry delays, if configured. */
    public Optional<RetryScheduler> retryScheduler() {
        return Optional.ofNullable(retryScheduler);
    }

    /** The client's feature plugins, in registration order. Immutable. */
    public List<AwsFeaturePlugin> plugins() {
        return plugins;
    }

    /**
     * The client's interceptors, in registration order; every plan dispatches them after the interceptors its plugins
     * contributed. Immutable.
     */
    public List<ExecutionInterceptor> interceptors() {
        return interceptors;
    }

    /** The client's resources and who owns each. */
    public ResourceScope resources() {
        return resources;
    }

    /**
     * The client-constant user-agent dimensions (Requirement 21.6), if the client sends the SDK user agent; each plan
     * resolves its operation's user agent from them once.
     */
    public Optional<UserAgent> userAgent() {
        return Optional.ofNullable(userAgent);
    }

    /**
     * Whether the client's calls emit telemetry: its configuration has a {@link TelemetryOptions#TELEMETRY} value
     * with at least one publisher. When false, no plan activates {@link PipelineStage#TELEMETRY}.
     */
    public boolean telemetryEnabled() {
        return telemetry != null;
    }

    /** The client's telemetry plan, or {@code null} when telemetry is not enabled. */
    PipelineTelemetry telemetry() {
        return telemetry;
    }

    /**
     * Closes every SDK-owned resource exactly once; idempotent.
     *
     * @throws software.amazon.awssdk.core.exception.SdkCloseException from the first call, if any close failed
     */
    @Override
    public void close() {
        resources.close();
    }

    @Override
    public String toString() {
        return "ClientRuntime(protocol=" + protocol.protocolId() + ", transport=" + transportMode + ", plans="
               + plans.size() + ")";
    }

    /**
     * Builder of a {@link ClientRuntime}; configuration, protocol, transport mode, retry strategy and retry scope are
     * required.
     */
    public static final class Builder {
        private SdkClientConfiguration configuration;
        private ClientProtocol protocol;
        private HttpTransportMode transportMode;
        private ProtocolTransportRequirements transportRequirements;
        private boolean eventStreamOperations;
        private RetryStrategy retryStrategy;
        private String retryScope;
        private EndpointRuleSet endpointRules;
        private EndpointClientValues endpointClientValues = EndpointClientValues.empty();
        private AuthSchemeSelector authSchemeSelector;
        private Executor completionExecutor;
        private RetryScheduler retryScheduler;
        private final List<AwsFeaturePlugin> plugins = new ArrayList<>();
        private final Set<ShapeId> pluginIds = new HashSet<>();
        private final List<ExecutionInterceptor> interceptors = new ArrayList<>();
        private UserAgent userAgent;
        private String telemetryService;
        private String telemetryHttpImplementation;

        private Builder() {
        }

        /** The client's resolved configuration; its resources become the runtime's resource scope. */
        public Builder configuration(SdkClientConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        /** The client's protocol runtime, created once per client. */
        public Builder protocol(ClientProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        /** The client's transport mode. */
        public Builder transportMode(HttpTransportMode transportMode) {
            this.transportMode = transportMode;
            return this;
        }

        /**
         * What the client's protocol requires of the transport (for example
         * {@code SelectedProtocol.transportRequirements()}) and whether the client has event-stream operations. When
         * set, {@link AwsCallEngine} rejects a transport whose capabilities cannot carry the protocol; when not set,
         * the engine checks only that the transport's mode matches. {@code null} clears it.
         */
        public Builder transportRequirements(ProtocolTransportRequirements transportRequirements,
                                             boolean eventStreamOperations) {
            this.transportRequirements = transportRequirements;
            this.eventStreamOperations = eventStreamOperations;
            return this;
        }

        /** The client's retry strategy and the retry-quota scope of its calls. */
        public Builder retryStrategy(RetryStrategy retryStrategy, String retryScope) {
            this.retryStrategy = retryStrategy;
            this.retryScope = retryScope;
            return this;
        }

        /** The service's endpoint rule set; {@code null} when the client does not resolve endpoints from rules. */
        public Builder endpointRules(EndpointRuleSet endpointRules) {
            this.endpointRules = endpointRules;
            return this;
        }

        /** The client-constant endpoint values. */
        public Builder endpointClientValues(EndpointClientValues endpointClientValues) {
            this.endpointClientValues = Objects.requireNonNull(endpointClientValues, "endpointClientValues");
            return this;
        }

        /** The client's auth-scheme selector. */
        public Builder authSchemeSelector(AuthSchemeSelector authSchemeSelector) {
            this.authSchemeSelector = authSchemeSelector;
            return this;
        }

        /** The executor async calls complete on. */
        public Builder completionExecutor(Executor completionExecutor) {
            this.completionExecutor = completionExecutor;
            return this;
        }

        /** The scheduler of non-blocking retry delays. */
        public Builder retryScheduler(RetryScheduler retryScheduler) {
            this.retryScheduler = retryScheduler;
            return this;
        }

        /**
         * Appends a feature plugin; plugins configure plans in registration order.
         *
         * @throws IllegalArgumentException if a plugin with the same ID was already added
         */
        public Builder addPlugin(AwsFeaturePlugin plugin) {
            Objects.requireNonNull(plugin, "plugin");
            ShapeId id = Objects.requireNonNull(plugin.id(), "plugin id");
            if (!pluginIds.add(id)) {
                throw new IllegalArgumentException("A feature plugin with ID " + id + " is already registered");
            }
            plugins.add(plugin);
            return this;
        }

        /**
         * Appends an interceptor; interceptors run in registration order at each hook they implement. Each plan
         * computes, once, which hooks each interceptor implements.
         */
        public Builder addInterceptor(ExecutionInterceptor interceptor) {
            interceptors.add(Objects.requireNonNull(interceptor, "interceptor"));
            return this;
        }

        /**
         * The client-constant user-agent dimensions; with them every call sets the {@value UserAgent#HEADER} header
         * unless its override configuration sets one. {@code null} (the default) sends no SDK user agent.
         */
        public Builder userAgent(UserAgent userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * The client-constant dimensions of the telemetry records, used only when the configuration enables telemetry
         * ({@link TelemetryOptions#TELEMETRY}): the service ID (default: the retry scope) and the client name of the
         * HTTP implementation (default: {@code none}, for a client without a transport).
         */
        public Builder telemetryDimensions(String service, String httpImplementation) {
            this.telemetryService = Objects.requireNonNull(service, "service");
            this.telemetryHttpImplementation = Objects.requireNonNull(httpImplementation, "httpImplementation");
            return this;
        }

        /**
         * Builds the runtime.
         *
         * @throws NullPointerException if a required component is missing
         * @throws IllegalArgumentException if the transport requirements are of another protocol
         */
        public ClientRuntime build() {
            return new ClientRuntime(this);
        }
    }
}
