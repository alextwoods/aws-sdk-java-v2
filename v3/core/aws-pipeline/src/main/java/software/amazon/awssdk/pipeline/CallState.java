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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * The mutable state of one call (design "{@code CallState}"; Requirements 9.4, 10.8, 10.15, 11.6): created once per
 * invocation from the operation's shared {@link PipelinePlan} and never shared with another call.
 *
 * <p>It owns the modeled request (replaceable only by execution-scoped stages before the first attempt, so every
 * attempt serializes the same request), the call's {@link CallOptions} (sparse deltas over the client
 * configuration), its {@link ExecutionContext}, the current retry token and the deterministic attempt counter, the
 * current {@link AttemptState}, one root {@link CancellationScope} and the {@link TerminalState}.
 *
 * <p>Attempts are strictly sequential: {@link #startAttempt(RetryToken)} requires the previous attempt to be complete
 * and an {@link RetryToken.State#ACTIVE} token for exactly the next attempt number, so one granted token authorizes at
 * most one attempt and numbering is one-based and gapless. The terminal transition happens exactly once; the first of
 * {@link #succeed}, {@link #fail} and {@link #cancel} wins, and later ones change nothing.
 *
 * <p>Each {@link RetryToken.State#ACTIVE} token the call records is consumed exactly once (design "State invariants"
 * 4): whichever stage consumes it (classification, success, the exception mapping, or a caller's cancellation on
 * another thread) first {@linkplain #claimToken claims} it, and only the one winning claim may hand it back to the
 * retry strategy.
 *
 * <p>Driven by the call engine: the lifecycle methods are package private. Confined to the call's driver, except
 * {@link #cancel(CancellationReason)}, the token claims and the terminal/cancellation reads, which are thread safe.
 *
 * @param <I> the input type
 * @param <O> the output type
 */
public final class CallState<I extends SerializableStruct, O extends SerializableStruct> {
    private static final VarHandle UNCONSUMED_TOKEN;

    static {
        try {
            UNCONSUMED_TOKEN = MethodHandles.lookup().findVarHandle(CallState.class, "unconsumedToken",
                                                                    RetryToken.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final PipelinePlan<I, O> plan;
    private final CallOptions options;
    private final ExecutionContext executionContext;
    private final CancellationScope cancellation = CancellationScope.root();
    private final TerminalState<O> terminal = new TerminalState<>();
    /** The call's telemetry state; null (no telemetry work) unless the plan has telemetry. */
    private final CallTelemetry telemetry;
    private I request;
    private volatile RetryToken retryToken;
    /** The recorded active token no stage has claimed yet, or null; accessed through {@link #UNCONSUMED_TOKEN}. */
    @SuppressWarnings("unused")
    private volatile RetryToken unconsumedToken;
    private int attemptsStarted;
    private AttemptState currentAttempt;

    CallState(PipelinePlan<I, O> plan, I request, CallOptions options) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.request = Objects.requireNonNull(request, "request");
        this.options = Objects.requireNonNull(options, "options");
        this.executionContext = new ExecutionContext(plan.contextLayout(), options.executionAttributes());
        PipelineTelemetry planTelemetry = plan.telemetry();
        this.telemetry = planTelemetry == null ? null : planTelemetry.newCall();
    }

    /** The plan this call executes. */
    public PipelinePlan<I, O> plan() {
        return plan;
    }

    /** The modeled request every attempt serializes. */
    public I request() {
        return request;
    }

    /** The call's options: its override deltas over the client configuration. */
    public CallOptions options() {
        return options;
    }

    /** The call's execution-scoped context; it survives every attempt. */
    public ExecutionContext executionContext() {
        return executionContext;
    }

    /** The call's cancellation scope. */
    public CancellationScope cancellation() {
        return cancellation;
    }

    /** The call's terminal outcome. */
    public TerminalState<O> terminal() {
        return terminal;
    }

    /** The number of attempts started so far. */
    public int attemptsStarted() {
        return attemptsStarted;
    }

    /** The call's telemetry state, or {@code null} when its plan has no telemetry. */
    CallTelemetry telemetry() {
        return telemetry;
    }

    /** The token of the current or last attempt, as last recorded. */
    public Optional<RetryToken> retryToken() {
        return Optional.ofNullable(retryToken);
    }

    /** The current attempt, if one has started and not completed. */
    public Optional<AttemptState> currentAttempt() {
        AttemptState attempt = currentAttempt;
        return attempt == null || attempt.isCompleted() ? Optional.empty() : Optional.of(attempt);
    }

    /**
     * Replaces the modeled request (for {@code modifyRequest} and idempotency-token injection).
     *
     * @throws IllegalStateException if an attempt has started or the call is terminal
     */
    void replaceRequest(I replacement) {
        Objects.requireNonNull(replacement, "replacement");
        requireRunning();
        if (attemptsStarted > 0) {
            throw new IllegalStateException("The request of " + plan.operation().id() + " can only be replaced before "
                                            + "the first attempt");
        }
        this.request = replacement;
    }

    /**
     * Records {@code token} (for example the consumed token a strategy returned) as the call's current token; an
     * active one becomes the call's one unconsumed token, to be {@linkplain #claimToken claimed} once. Only the
     * driver records tokens, and only a token it obtained by consuming a claimed one (or the initial token).
     */
    void retryToken(RetryToken token) {
        this.retryToken = Objects.requireNonNull(token, "token");
        if (token.state() == RetryToken.State.ACTIVE) {
            UNCONSUMED_TOKEN.setVolatile(this, token);
        }
    }

    /**
     * Claims {@code token} for consumption. Thread safe.
     *
     * @return true for exactly one claim of a recorded active token; false if it was claimed already
     */
    boolean claimToken(RetryToken token) {
        return UNCONSUMED_TOKEN.compareAndSet(this, token, null);
    }

    /**
     * Claims the call's unconsumed token, whichever it is. Thread safe.
     *
     * @return the claimed token, or null if every recorded token was claimed
     */
    RetryToken claimUnconsumedToken() {
        return (RetryToken) UNCONSUMED_TOKEN.getAndSet(this, null);
    }

    /**
     * Starts the attempt {@code token} authorizes: a fresh attempt context and child cancellation scope.
     *
     * @throws IllegalStateException if the call is terminal or cancelled, the previous attempt has not completed, or
     *     the token is not active
     * @throws IllegalArgumentException if the token is not for exactly the next attempt number
     */
    AttemptState startAttempt(RetryToken token) {
        Objects.requireNonNull(token, "token");
        requireRunning();
        if (!cancellation.isOpen()) {
            throw new IllegalStateException("The call is cancelled; no further attempt may start");
        }
        if (currentAttempt != null && !currentAttempt.isCompleted()) {
            throw new IllegalStateException("Attempt " + currentAttempt.number() + " has not completed");
        }
        if (token.state() != RetryToken.State.ACTIVE) {
            throw new IllegalStateException("A " + token.state() + " retry token authorizes no attempt");
        }
        if (token.attempt() != attemptsStarted + 1) {
            throw new IllegalArgumentException("The retry token authorizes attempt " + token.attempt() + ", but the "
                                               + "next attempt is " + (attemptsStarted + 1));
        }
        AttemptState attempt = new AttemptState(token, executionContext, cancellation.child(), telemetry, terminal);
        attemptsStarted++;
        retryToken = token;
        currentAttempt = attempt;
        return attempt;
    }

    /**
     * Completes the call with {@code output}, ending the current attempt and releasing the cancellation scope.
     *
     * @return true if this made the call's one terminal transition
     */
    boolean succeed(O output) {
        if (!terminal.succeed(output)) {
            return false;
        }
        release();
        return true;
    }

    /**
     * Ends a presign call after signing, without transmission: ends the current attempt and releases the
     * cancellation scope.
     *
     * @return true if this made the call's one terminal transition
     */
    boolean presigned() {
        if (!terminal.presigned()) {
            return false;
        }
        release();
        return true;
    }

    /**
     * Completes the call with {@code failure} after its one exception-mapping path, ending the current attempt and
     * releasing the cancellation scope.
     *
     * @return true if this made the call's one terminal transition
     */
    boolean fail(Throwable failure) {
        if (!terminal.fail(failure)) {
            return false;
        }
        release();
        return true;
    }

    /**
     * Cancels the call: the terminal transition, then cancellation of every registered handle and attempt scope. The
     * current attempt is left for the driver to complete when it observes the cancellation. Thread safe and never
     * throws for a failing handle: every handle still runs once, and the failures are suppressed onto the call's
     * terminal {@link CancellationException}.
     *
     * @return true if this made the call's one terminal transition
     */
    boolean cancel(CancellationReason reason) {
        Objects.requireNonNull(reason, "reason");
        CancellationException cause = new CancellationException(plan.operation().id() + " was cancelled: " + reason);
        if (!terminal.cancel(cause)) {
            return false;
        }
        try {
            cancellation.cancel(reason);
        } catch (RuntimeException handleFailure) {
            cause.addSuppressed(handleFailure);
        }
        return true;
    }

    private void release() {
        AttemptState attempt = currentAttempt;
        if (attempt != null) {
            attempt.complete();
        }
        cancellation.close();
    }

    private void requireRunning() {
        if (terminal.isTerminal()) {
            throw new IllegalStateException("The call of " + plan.operation().id() + " is already "
                                            + terminal.status());
        }
    }

    @Override
    public String toString() {
        return "CallState(" + plan.operation().id() + ", attempts=" + attemptsStarted + ", " + terminal.status() + ")";
    }
}
