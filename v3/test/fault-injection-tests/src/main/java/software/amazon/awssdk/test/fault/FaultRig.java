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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import software.amazon.awssdk.http.HttpTransportMode;

/**
 * Every fake and recorder of one faulted call, created from its {@link FaultTrace}, sharing one {@link Violations}.
 * {@link FaultHarness} wires a rig into a client runtime it builds; a test that builds a client itself (for example
 * a generated client through its public builder) wires the rig's parts into that client and calls
 * {@link #outcome} once the call ended.
 *
 * <p>One rig per call: the scripts are consumed and the probes count one call's events.
 */
public final class FaultRig {
    private final FaultTrace trace;
    private final HttpTransportMode mode;
    private final Violations violations = new Violations();
    private final FaultyHttpClient http;
    private final ScriptedIdentityProvider identities;
    private final ScriptedSigner signer;
    private final FaultAuthScheme authScheme;
    private final VirtualRetryScheduler scheduler;
    private final RecordingRetryStrategy retryStrategy;
    private final LifecycleProbe lifecycle;
    private final TelemetryProbe telemetry;
    private final FaultBodies bodies;
    private final TransformerProbe transformers;

    /** A rig of {@code trace} on {@code mode}, delivering async transport events on {@code deliveries}. */
    public FaultRig(FaultTrace trace, HttpTransportMode mode, Executor deliveries) {
        this.trace = Objects.requireNonNull(trace, "trace");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.http = new FaultyHttpClient(trace.transmissions(), deliveries, violations);
        this.identities = new ScriptedIdentityProvider(trace.identities(), violations);
        this.signer = new ScriptedSigner(trace.signings(), violations);
        this.authScheme = new FaultAuthScheme(signer);
        this.scheduler = trace.cancel() == CancelPoint.RETRY_DELAY ? VirtualRetryScheduler.holding()
                                                                    : VirtualRetryScheduler.elapsing();
        this.retryStrategy = new RecordingRetryStrategy(trace.retry().strategy());
        this.lifecycle = new LifecycleProbe(violations);
        this.telemetry = new TelemetryProbe(violations);
        this.bodies = new FaultBodies(trace.body());
        this.transformers = new TransformerProbe(violations);
    }

    /** A rig of {@code trace} on {@code mode}, delivering async transport events inline. */
    public FaultRig(FaultTrace trace, HttpTransportMode mode) {
        this(trace, mode, Runnable::run);
    }

    public FaultTrace trace() {
        return trace;
    }

    public HttpTransportMode mode() {
        return mode;
    }

    public Violations violations() {
        return violations;
    }

    public FaultyHttpClient http() {
        return http;
    }

    public ScriptedIdentityProvider identities() {
        return identities;
    }

    public ScriptedSigner signer() {
        return signer;
    }

    public FaultAuthScheme authScheme() {
        return authScheme;
    }

    public VirtualRetryScheduler scheduler() {
        return scheduler;
    }

    public RecordingRetryStrategy retryStrategy() {
        return retryStrategy;
    }

    public LifecycleProbe lifecycle() {
        return lifecycle;
    }

    public TelemetryProbe telemetry() {
        return telemetry;
    }

    public FaultBodies bodies() {
        return bodies;
    }

    /** Counts the response transformer callbacks of a streaming-output call; wrap its transformer with it. */
    public TransformerProbe transformers() {
        return transformers;
    }

    /**
     * The outcome of the rig's call, which ended as {@code terminal}. Signings and the scheduler are only reported
     * if the rig's signer and scheduler were wired into the client (a generated client signs with real SigV4 and
     * owns its scheduler); pass {@code signed} and {@code scheduled} accordingly.
     */
    public FaultOutcome outcome(FaultOutcome.Terminal terminal, Object result, Throwable failure, boolean signed,
                                boolean scheduled) {
        List<String> leaks = new ArrayList<>(http.leaks());
        leaks.addAll(bodies.leaks());
        if (scheduled) {
            scheduler.unresolved().forEach(timer -> leaks.add("retry delay of " + timer.delay() + " never elapsed "
                                                              + "or was cancelled"));
        }
        List<String> problems = new ArrayList<>(violations.all());
        problems.addAll(retryStrategy.tokenProblems());
        if (identities.anyHeldCancelled()) {
            problems.add("identity: the pipeline cancelled a provider-owned identity future");
        }
        if (terminal == FaultOutcome.Terminal.DID_NOT_COMPLETE) {
            problems.add("the call did not complete within the harness bound");
        }
        if (scheduled) {
            scheduler.timers().stream().filter(t -> t.cancels() > 1)
                     .forEach(t -> problems.add("retry delay of " + t.delay() + " cancelled " + t.cancels()
                                                + " times"));
        }
        return new FaultOutcome(trace, mode, terminal, result, failure, lifecycle.attempts(), http.transmissions(),
                                identities.resolutions(), signed ? signer.signings() : 0,
                                retryStrategy.retryDelays(),
                                scheduled ? scheduler.requested() : List.of(),
                                lifecycle.gapsBetweenAttempts(), retryStrategy.ledger(), http.exchanges(),
                                lifecycle.afterExecutions(), transformers.handoffs(), telemetry.calls(), leaks,
                                problems, http.unused());
    }

    /** The default bound on one call of a rig. */
    public static Duration defaultCallTimeout() {
        return Duration.ofSeconds(20);
    }
}
