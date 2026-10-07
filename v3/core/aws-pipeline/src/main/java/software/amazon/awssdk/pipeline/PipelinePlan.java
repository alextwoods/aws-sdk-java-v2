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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.checksums.ChecksumValidationResult;
import software.amazon.awssdk.checksums.RequestChecksumFeature;
import software.amazon.awssdk.checksums.ResponseChecksumFeature;
import software.amazon.awssdk.checksums.compression.RequestCompressionFeature;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.endpoints.EndpointPlan;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.pipeline.feature.IdempotencyTokenFeature;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.retries.AcquireInitialTokenRequest;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The immutable pipeline plan of one operation of one client (design "{@code PipelinePlan}"; Requirements 9.5, 10.1,
 * 10.11): everything about executing the operation that does not depend on a call, resolved once by
 * {@link ClientRuntime#plan(AwsOperation)} and shared by every call.
 *
 * <p>A plan holds the operation's protocol SERDE plan, endpoint subplan, modeled auth metadata, retry-token request,
 * the feature subplans that apply to it (idempotency token, request/response checksums, compression), the typed
 * {@link ContextLayout} of its calls' contexts, the {@link StageMetadata} of its active stages, the per-hook
 * interceptor dispatch arrays (only the hooks an interceptor implements are active stages), the plugins that
 * contributed to it and their derived {@link PlanAttribute}s. It holds no customer data: no request, identity, body,
 * header value or other value of any call. All per-call mutable state lives in the {@link CallState} and
 * {@link AttemptState} objects a call creates. Plans may share immutable subplans (endpoint rule set, protocol) by
 * identity; plan equality is identity.
 *
 * @param <I> the input type
 * @param <O> the output type
 */
public final class PipelinePlan<I extends SerializableStruct, O extends SerializableStruct> {
    private final ClientRuntime runtime;
    private final AwsOperation<I, O> operation;
    private final OperationProtocol protocol;
    private final EndpointPlan endpoint;
    private final ModeledAuth auth;
    private final AcquireInitialTokenRequest initialTokenRequest;
    private final IdempotencyTokenFeature<I> idempotencyToken;
    private final RequestChecksumFeature requestChecksum;
    private final ResponseChecksumFeature responseChecksum;
    private final RequestCompressionFeature requestCompression;
    private final ContextSlot<String> idempotencyTokenSlot;
    private final ContextSlot<ChecksumValidationResult> checksumValidationSlot;
    private final ContextLayout contextLayout;
    private final InterceptorDispatch interceptors;
    private final StageMetadata stages;
    private final List<ShapeId> plugins;
    private final PlanAttribute<?>[] attributeKeys;
    private final Object[] attributeValues;
    /** The operation's user agent, resolved once; null when the client sends no SDK user agent. */
    private final UserAgent.Operation userAgent;
    /** The operation's telemetry plan; null unless the client enables telemetry. */
    private final PipelineTelemetry telemetry;

    private PipelinePlan(Builder<I, O> builder, InterceptorDispatch interceptors) {
        this.runtime = builder.runtime;
        this.operation = builder.operation;
        this.protocol = builder.protocol;
        this.endpoint = builder.endpoint;
        this.auth = operation.auth().orElse(null);
        this.initialTokenRequest = new AcquireInitialTokenRequest(runtime.retryScope(), operation.longPolling());
        this.idempotencyToken = builder.idempotencyToken;
        this.requestChecksum = builder.requestChecksum;
        this.responseChecksum = builder.responseChecksum;
        this.requestCompression = builder.requestCompression;
        this.idempotencyTokenSlot = builder.idempotencyTokenSlot;
        this.checksumValidationSlot = builder.checksumValidationSlot;
        this.contextLayout = builder.layout.build();
        this.interceptors = interceptors;
        this.stages = StageMetadata.of(builder.activated);
        this.plugins = List.copyOf(builder.plugins);
        this.attributeKeys = builder.attributeKeys.toArray(new PlanAttribute<?>[0]);
        this.attributeValues = builder.attributeValues.toArray();
        this.userAgent = runtime.userAgent()
                                .map(ua -> ua.forOperation(operation.id().getName(), builder.userAgentFeatures))
                                .orElse(null);
        this.telemetry = builder.telemetry;
    }

    /**
     * Builds the plan of {@code operation} for {@code runtime}; called once per client and operation by
     * {@link ClientRuntime#plan(AwsOperation)}.
     */
    static <I extends SerializableStruct, O extends SerializableStruct> PipelinePlan<I, O> create(
        ClientRuntime runtime, AwsOperation<I, O> operation) {
        Builder<I, O> builder = new Builder<>(runtime, operation);
        for (AwsFeaturePlugin plugin : runtime.plugins()) {
            if (plugin.appliesTo(operation)) {
                builder.plugins.add(plugin.id());
                plugin.configure(builder);
            }
        }
        return builder.build();
    }

    /** The client runtime this plan belongs to. */
    public ClientRuntime runtime() {
        return runtime;
    }

    /** The operation. */
    public AwsOperation<I, O> operation() {
        return operation;
    }

    /** The operation's protocol SERDE plan, obtained once from the client's protocol. */
    public OperationProtocol protocol() {
        return protocol;
    }

    /** The operation's endpoint subplan; empty when the client has no endpoint rule set. */
    public Optional<EndpointPlan> endpoint() {
        return Optional.ofNullable(endpoint);
    }

    /** The operation's modeled auth metadata, if recorded. */
    public Optional<ModeledAuth> auth() {
        return Optional.ofNullable(auth);
    }

    /** The client's transport mode. */
    public HttpTransportMode transportMode() {
        return runtime.transportMode();
    }

    /** The request every call of this plan acquires its first retry token with. */
    public AcquireInitialTokenRequest initialTokenRequest() {
        return initialTokenRequest;
    }

    /** The idempotency token feature, if the input has an {@code @idempotencyToken} member. */
    public Optional<IdempotencyTokenFeature<I>> idempotencyToken() {
        return Optional.ofNullable(idempotencyToken);
    }

    /** The request checksum feature, if the operation has request checksum behavior. */
    public Optional<RequestChecksumFeature> requestChecksum() {
        return Optional.ofNullable(requestChecksum);
    }

    /** The response checksum feature, if the operation validates response checksums. */
    public Optional<ResponseChecksumFeature> responseChecksum() {
        return Optional.ofNullable(responseChecksum);
    }

    /** The request compression feature, if the operation compresses requests. */
    public Optional<RequestCompressionFeature> requestCompression() {
        return Optional.ofNullable(requestCompression);
    }

    /** The execution slot of {@link IdempotencyTokenFeature#GENERATED_TOKEN}, when the feature applies. */
    public Optional<ContextSlot<String>> idempotencyTokenSlot() {
        return Optional.ofNullable(idempotencyTokenSlot);
    }

    /** The attempt slot of {@link ResponseChecksumFeature#VALIDATION_RESULT}, when the feature applies. */
    public Optional<ContextSlot<ChecksumValidationResult>> checksumValidationSlot() {
        return Optional.ofNullable(checksumValidationSlot);
    }

    /** The slot assignment of this plan's contexts. */
    public ContextLayout contextLayout() {
        return contextLayout;
    }

    /** The stages active in calls of this plan. */
    public StageMetadata stages() {
        return stages;
    }

    /**
     * The interceptors of this plan's calls, in dispatch order: those the plugins contributed (in plugin order),
     * then the client's. Immutable.
     */
    public List<ExecutionInterceptor> interceptors() {
        return interceptors.interceptors();
    }

    /** The per-hook dispatch arrays of {@link #interceptors()}. */
    InterceptorDispatch dispatch() {
        return interceptors;
    }

    /** The operation's telemetry plan, or {@code null} when the client does not enable telemetry. */
    PipelineTelemetry telemetry() {
        return telemetry;
    }

    /**
     * The operation's user agent (Requirements 21.6, 21.7): the client's dimensions, the operation and the feature
     * identifiers the plan's plugins contributed, resolved once; empty when the client sends no SDK user agent.
     */
    public Optional<UserAgent.Operation> userAgent() {
        return Optional.ofNullable(userAgent);
    }

    /** {@link #userAgent()} without the {@code Optional}, for the per-attempt serialize stage. */
    UserAgent.Operation userAgentOrNull() {
        return userAgent;
    }

    /** The IDs of the plugins that contributed to this plan, in contribution order. Immutable. */
    public List<ShapeId> plugins() {
        return plugins;
    }

    /** The value a plugin contributed under {@code attribute}, if any. */
    public <T> Optional<T> attribute(PlanAttribute<T> attribute) {
        Objects.requireNonNull(attribute, "attribute");
        for (int i = 0; i < attributeKeys.length; i++) {
            if (attributeKeys[i] == attribute) {
                return Optional.of(attribute.cast(attributeValues[i]));
            }
        }
        return Optional.empty();
    }

    /**
     * The state of one new call of this plan: a fresh execution context seeded with the call's execution attributes, a
     * fresh cancellation scope and terminal state. Nothing in it is shared with another call.
     */
    CallState<I, O> newCall(I request, CallOptions options) {
        return new CallState<>(this, request, options);
    }

    @Override
    public String toString() {
        return "PipelinePlan[" + operation.id() + "]";
    }

    /**
     * The plan under construction, as seen by {@link AwsFeaturePlugin#configure}. Plan-time only: not thread safe, and
     * every method throws once the plan is built.
     *
     * @param <I> the input type
     * @param <O> the output type
     */
    public static final class Builder<I extends SerializableStruct, O extends SerializableStruct> {
        private final ClientRuntime runtime;
        private final AwsOperation<I, O> operation;
        private final OperationProtocol protocol;
        private final EndpointPlan endpoint;
        private final IdempotencyTokenFeature<I> idempotencyToken;
        private final RequestChecksumFeature requestChecksum;
        private final ResponseChecksumFeature responseChecksum;
        private final RequestCompressionFeature requestCompression;
        private final ContextLayout.Builder layout = ContextLayout.builder();
        private final EnumSet<PipelineStage> activated = EnumSet.noneOf(PipelineStage.class);
        private final List<ShapeId> plugins = new ArrayList<>();
        private final List<ExecutionInterceptor> interceptors = new ArrayList<>();
        private final List<PlanAttribute<?>> attributeKeys = new ArrayList<>();
        private final List<Object> attributeValues = new ArrayList<>();
        private ContextSlot<String> idempotencyTokenSlot;
        private ContextSlot<ChecksumValidationResult> checksumValidationSlot;
        private final PipelineTelemetry telemetry;
        private final List<String> userAgentFeatures = new ArrayList<>();
        private boolean built;

        private Builder(ClientRuntime runtime, AwsOperation<I, O> operation) {
            this.runtime = runtime;
            this.operation = operation;
            OperationSchemas schemas = operation.schemas();
            this.protocol = Objects.requireNonNull(runtime.protocol().operation(schemas),
                                                   "The client protocol returned no plan for " + operation.id());
            this.endpoint = runtime.endpointRules().map(rules -> new EndpointPlan(rules, operation.endpointBinding()))
                                   .orElse(null);
            this.idempotencyToken = IdempotencyTokenFeature.forOperation(operation.id(), schemas.input(),
                                                                         operation.inputBuilder())
                                                           .orElse(null);
            this.requestChecksum = RequestChecksumFeature.forOperation(schemas.operation(), schemas.input(),
                                                                       operation.httpChecksum().orElse(null))
                                                         .orElse(null);
            this.responseChecksum = ResponseChecksumFeature.forOperation(schemas.operation(), schemas.input(),
                                                                         schemas.output(),
                                                                         operation.httpChecksum().orElse(null))
                                                           .orElse(null);
            this.requestCompression = RequestCompressionFeature.forOperation(schemas.operation(), schemas.input())
                                                               .orElse(null);
            if (idempotencyToken != null) {
                activated.add(PipelineStage.IDEMPOTENCY_TOKEN);
                idempotencyTokenSlot = layout.declare(IdempotencyTokenFeature.GENERATED_TOKEN);
            }
            if (requestChecksum != null) {
                activated.add(PipelineStage.REQUEST_CHECKSUM);
            }
            if (responseChecksum != null) {
                activated.add(PipelineStage.RESPONSE_CHECKSUM);
                checksumValidationSlot = layout.declare(ResponseChecksumFeature.VALIDATION_RESULT);
            }
            if (requestCompression != null) {
                activated.add(PipelineStage.REQUEST_COMPRESSION);
            }
            // Response metadata applies to every operation whose output is a generated AWS response (task 16.1).
            if (operation.outputBuilder().get() instanceof AwsResponse.Builder) {
                activated.add(PipelineStage.RESPONSE_METADATA);
            }
            // Telemetry is a feature of the client: active in every plan of a client with a publisher, else absent.
            PipelineTelemetry clientTelemetry = runtime.telemetry();
            this.telemetry = clientTelemetry == null ? null : clientTelemetry.forOperation(operation.id().getName());
            if (telemetry != null) {
                activated.add(PipelineStage.TELEMETRY);
            }
        }

        /** The operation being planned. */
        public AwsOperation<I, O> operation() {
            requireBuilding();
            return operation;
        }

        /** The client's resolved configuration, read-only. */
        public SdkClientConfiguration configuration() {
            requireBuilding();
            return runtime.configuration();
        }

        /** The client's transport mode. */
        public HttpTransportMode transportMode() {
            requireBuilding();
            return runtime.transportMode();
        }

        /** Whether {@code stage} is active so far. */
        public boolean isActive(PipelineStage stage) {
            requireBuilding();
            return stage.kind() == PipelineStage.Kind.CORE || activated.contains(stage);
        }

        /**
         * Assigns {@code key} a dense slot in the plan's contexts (idempotent per key), so stages read and write it
         * by index.
         */
        public <T> ContextSlot<T> declare(ContextKey<T> key) {
            requireBuilding();
            return layout.declare(key);
        }

        /** Activates a feature or hook stage in the plan; core stages are always active. */
        public Builder<I, O> activate(PipelineStage stage) {
            requireBuilding();
            if (stage.kind() != PipelineStage.Kind.CORE) {
                activated.add(stage);
            }
            return this;
        }

        /**
         * Contributes an interceptor to the plan's calls (Requirement 11.11). Contributed interceptors run before the
         * client's, in contribution order; the hook stages it implements become active.
         */
        public Builder<I, O> addInterceptor(ExecutionInterceptor interceptor) {
            requireBuilding();
            interceptors.add(Objects.requireNonNull(interceptor, "interceptor"));
            return this;
        }

        /**
         * Contributes a feature identifier to the user agent of every call of the plan (Requirement 21.6), for a
         * plugin that takes part in every call of the operation; a feature that takes part in only some calls adds
         * it per call instead ({@code RequestOverrideConfiguration.Builder#addApiName}). Identifiers are listed
         * once, in contribution order.
         */
        public Builder<I, O> addUserAgentFeature(String featureId) {
            requireBuilding();
            userAgentFeatures.add(Objects.requireNonNull(featureId, "featureId"));
            return this;
        }

        /**
         * Contributes client-constant derived metadata under {@code attribute}, replacing an earlier contribution.
         * The value must be immutable and hold no customer data.
         */
        public <T> Builder<I, O> attribute(PlanAttribute<T> attribute, T value) {
            requireBuilding();
            Objects.requireNonNull(attribute, "attribute");
            T typed = attribute.cast(Objects.requireNonNull(value, "value"));
            int index = attributeKeys.indexOf(attribute);
            if (index >= 0) {
                attributeValues.set(index, typed);
            } else {
                attributeKeys.add(attribute);
                attributeValues.add(typed);
            }
            return this;
        }

        private PipelinePlan<I, O> build() {
            requireBuilding();
            built = true;
            interceptors.addAll(runtime.interceptors());
            InterceptorDispatch dispatch = InterceptorDispatch.of(interceptors);
            activated.addAll(dispatch.activeStages());
            return new PipelinePlan<>(this, dispatch);
        }

        private void requireBuilding() {
            if (built) {
                throw new IllegalStateException("The plan of " + operation.id() + " is already built");
            }
        }

        @Override
        public String toString() {
            return "PipelinePlan.Builder[" + operation.id() + ", stages=" + Arrays.toString(activated.toArray()) + "]";
        }
    }
}
