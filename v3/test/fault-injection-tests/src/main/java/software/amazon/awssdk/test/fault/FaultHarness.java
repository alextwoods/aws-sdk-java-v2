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

package software.amazon.awssdk.test.fault;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.pipeline.AwsCallEngine;
import software.amazon.awssdk.pipeline.AwsOperation;
import software.amazon.awssdk.pipeline.ClientRuntime;
import software.amazon.awssdk.pipeline.PipelinePlan;
import software.amazon.awssdk.pipeline.client.AwsClientBuildState;
import software.amazon.awssdk.pipeline.client.AwsClientParts;
import software.amazon.awssdk.pipeline.client.AwsServiceClientDefinition;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * Runs one {@link FaultTrace} through the real pipeline of a service: the client runtime and engine are built by the
 * same {@link AwsClientBuildState} a generated client uses, from the service's real protocol and type registry, with
 * every dependency replaced by the trace's controllable fake ({@link FaultRig}): the transport, the credentials
 * provider, the SigV4 scheme's signer, the endpoint rules ({@link FaultEndpoints}), the retry strategy and, on the
 * async driver, the retry scheduler (virtual time) and an inline completion executor. No configuration is read from
 * the environment, so a run is deterministic.
 *
 * <p>{@link #execute} starts the call, applies the trace's cancellation at its point (see {@link FaultCalls}), waits
 * for the call to end within the harness bound and returns the {@link FaultOutcome}. One harness runs one call; close
 * it afterwards.
 */
public final class FaultHarness implements AutoCloseable {
    /** The client endpoint the {@link EndpointMode#RESOLVE} rule set resolves to. */
    public static final URI ENDPOINT = URI.create("https://fault-injection.example.com");

    private final FaultRig rig;
    private final AwsClientParts parts;
    private final FaultCalls.Wiring wiring;
    private final AtomicBoolean used = new AtomicBoolean();

    private FaultHarness(Builder builder) {
        this.rig = new FaultRig(builder.trace, builder.mode, builder.deliveries);
        this.wiring = new FaultCalls.Wiring(true, true, builder.callTimeout);
        AwsServiceClientDefinition service = builder.service;
        AwsServiceClientDefinition faulted = new AwsServiceClientDefinition(
            service.serviceId(), service.serviceName(), service.protocols(), service.typeRegistry(),
            FaultEndpoints.ruleSet(builder.trace.endpoint()));
        AwsClientBuildState state = new AwsClientBuildState();
        state.configurationSources(ConfigurationSources.none());
        state.region(Region.US_EAST_1);
        state.endpointOverride(ENDPOINT);
        state.credentialsProvider(rig.identities());
        state.authSchemeSelector(rig.authScheme().selector());
        state.retryStrategy(rig.retryStrategy());
        state.addInterceptor(rig.lifecycle());
        state.telemetry(rig.telemetry().configuration());
        if (builder.mode == HttpTransportMode.SYNC) {
            state.httpClient(rig.http().sync());
            this.parts = state.buildSync(faulted);
        } else {
            state.asyncHttpClient(rig.http().async());
            state.completionExecutor(builder.completionExecutor);
            state.retryScheduler(rig.scheduler());
            this.parts = state.buildAsync(faulted);
        }
    }

    /** A harness builder for a call of {@code service} (for example a generated client's definition). */
    public static Builder builder(AwsServiceClientDefinition service) {
        return new Builder(service);
    }

    /** The rig of fakes and recorders, for assertions beyond {@link FaultOutcome}. */
    public FaultRig rig() {
        return rig;
    }

    public ClientRuntime runtime() {
        return parts.runtime();
    }

    public AwsCallEngine engine() {
        return parts.engine();
    }

    /** Runs one call of {@code operation} with {@code input}, and the trace's body if it has one. */
    public <I extends SerializableStruct, O extends SerializableStruct> FaultOutcome execute(
        AwsOperation<I, O> operation, I input) {
        PipelinePlan<I, O> plan = runtime().plan(operation);
        return run(() -> engine().executeSync(plan, input, null, rig.bodies().sync()),
                   () -> engine().executeAsync(plan, input, null, rig.bodies().async()));
    }

    /**
     * Runs one call of a streaming-output {@code operation}: the sync driver hands the payload to {@code sync}, the
     * async driver to a new transformer from {@code async}; either is counted by the rig's {@link TransformerProbe}.
     */
    public <I extends SerializableStruct, O extends SerializableStruct, R> FaultOutcome executeStreaming(
        AwsOperation<I, O> operation, I input, ResponseTransformer<O, R> sync,
        Supplier<? extends AsyncResponseTransformer<O, R>> async) {
        PipelinePlan<I, O> plan = runtime().plan(operation);
        TransformerProbe probe = rig.transformers();
        return run(() -> engine().executeSync(plan, input, null, rig.bodies().sync(), probe.sync(sync)),
                   () -> engine().executeAsync(plan, input, null, rig.bodies().async(), probe.async(async.get())));
    }

    /** Runs one arbitrary call on {@link #engine()}: {@code sync} on the sync driver, {@code async} on the async one. */
    public FaultOutcome run(Supplier<?> sync, Supplier<? extends CompletableFuture<?>> async) {
        if (!used.compareAndSet(false, true)) {
            throw new IllegalStateException("A fault harness runs one call");
        }
        return FaultCalls.run(rig, wiring, sync, async);
    }

    @Override
    public void close() {
        parts.close();
    }

    /** Builder of a {@link FaultHarness}. */
    public static final class Builder {
        private final AwsServiceClientDefinition service;
        private FaultTrace trace;
        private HttpTransportMode mode = HttpTransportMode.SYNC;
        private Executor deliveries = Runnable::run;
        private Executor completionExecutor = Runnable::run;
        private Duration callTimeout = FaultRig.defaultCallTimeout();

        private Builder(AwsServiceClientDefinition service) {
            this.service = Objects.requireNonNull(service, "service");
        }

        public Builder trace(FaultTrace trace) {
            this.trace = trace;
            return this;
        }

        /** The driver; sync by default. */
        public Builder mode(HttpTransportMode mode) {
            this.mode = mode;
            return this;
        }

        /** Where the async transport delivers its events (a fake event loop); inline by default. */
        public Builder deliveries(Executor deliveries) {
            this.deliveries = deliveries;
            return this;
        }

        /** The async completion executor; inline by default, so an async call runs on the threads that drive it. */
        public Builder completionExecutor(Executor completionExecutor) {
            this.completionExecutor = completionExecutor;
            return this;
        }

        /** The bound on one call; a call that does not end in time is {@link FaultOutcome.Terminal#DID_NOT_COMPLETE}. */
        public Builder callTimeout(Duration callTimeout) {
            this.callTimeout = callTimeout;
            return this;
        }

        public FaultHarness build() {
            Objects.requireNonNull(trace, "trace");
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(deliveries, "deliveries");
            Objects.requireNonNull(completionExecutor, "completionExecutor");
            Objects.requireNonNull(callTimeout, "callTimeout");
            return new FaultHarness(this);
        }
    }
}
