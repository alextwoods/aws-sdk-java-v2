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

/**
 * The terminal outcome of one call (Requirement 10.15; design "{@code CallState}"): {@link Status#RUNNING} until one
 * compare-and-set moves it, exactly once and irreversibly, to {@link Status#SUCCEEDED}, {@link Status#FAILED} or
 * {@link Status#CANCELLED}. Every later transition attempt, including a racing one from another thread (a transport
 * callback against a caller's cancellation), loses and changes nothing.
 *
 * <p>The transitions are driven only by the call's {@link CallState}; extension code reads the outcome. Thread safe.
 *
 * @param <O> the output type
 */
public final class TerminalState<O> {
    /** The status of a call. */
    public enum Status {
        /** No terminal outcome yet. */
        RUNNING,
        /** The call completed with an output. */
        SUCCEEDED,
        /** The call completed with a failure, after its one exception-mapping path. */
        FAILED,
        /** The call was cancelled successfully. */
        CANCELLED,
        /** The call ended after signing without transmission: a presigned request, with no output. */
        PRESIGNED;

        /** Whether this is one of the terminal statuses. */
        public boolean isTerminal() {
            return this != RUNNING;
        }
    }

    private static final VarHandle OUTCOME;
    private static final Outcome RUNNING = new Outcome(Status.RUNNING, null, null);

    static {
        try {
            OUTCOME = MethodHandles.lookup().findVarHandle(TerminalState.class, "outcome", Outcome.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused") // accessed through OUTCOME
    private volatile Outcome outcome = RUNNING;

    TerminalState() {
    }

    /** Completes with {@code output}; true if this call made the one terminal transition. */
    boolean succeed(O output) {
        return transition(new Outcome(Status.SUCCEEDED, Objects.requireNonNull(output, "output"), null));
    }

    /** Completes as presigned; true if this call made the one terminal transition. */
    boolean presigned() {
        return transition(new Outcome(Status.PRESIGNED, null, null));
    }

    /** Completes with {@code failure}; true if this call made the one terminal transition. */
    boolean fail(Throwable failure) {
        return transition(new Outcome(Status.FAILED, null, Objects.requireNonNull(failure, "failure")));
    }

    /** Completes as cancelled with {@code cause}; true if this call made the one terminal transition. */
    boolean cancel(CancellationException cause) {
        return transition(new Outcome(Status.CANCELLED, null, Objects.requireNonNull(cause, "cause")));
    }

    private boolean transition(Outcome terminal) {
        return OUTCOME.compareAndSet(this, RUNNING, terminal);
    }

    /** The current status. */
    public Status status() {
        return outcome.status();
    }

    /** Whether the call has its terminal outcome. */
    public boolean isTerminal() {
        return outcome != RUNNING;
    }

    /** The output of a {@link Status#SUCCEEDED} call. */
    @SuppressWarnings("unchecked")
    public Optional<O> output() {
        return Optional.ofNullable((O) outcome.output());
    }

    /** The failure of a {@link Status#FAILED} call, or the {@link CancellationException} of a cancelled one. */
    public Optional<Throwable> failure() {
        return Optional.ofNullable(outcome.failure());
    }

    @Override
    public String toString() {
        return "TerminalState(" + status() + ")";
    }

    private record Outcome(Status status, Object output, Throwable failure) {
    }
}
