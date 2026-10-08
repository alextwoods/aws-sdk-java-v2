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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.telemetry.spi.TelemetryOutcome;

/**
 * Runs one call wired to a {@link FaultRig} on the rig's driver, applies the rig trace's cancellation at its point,
 * waits for the call to end within a bound and returns its {@link FaultOutcome}. Works for any client: the engine of
 * {@link FaultHarness}, or a generated client a test built with the rig's fakes.
 *
 * <ul>
 *     <li>Async: the call's future is cancelled ({@code cancel(true)}) once the call is parked at the point.</li>
 *     <li>Sync: the call runs on a dedicated thread, which is interrupted once the call is parked at the point. The
 *     pipeline's one telemetry record tells a cancelled call from a failed one, since the caller sees an exception
 *     either way; the caller's interrupt status must survive the call.</li>
 * </ul>
 */
public final class FaultCalls {
    private FaultCalls() {
    }

    /** How the rig is wired into the client under test. */
    public record Wiring(boolean signerWired, boolean schedulerWired, Duration callTimeout) {
        /** Every fake of the rig is wired ({@link FaultHarness}). */
        public static Wiring all() {
            return new Wiring(true, true, FaultRig.defaultCallTimeout());
        }

        /** A generated client: real SigV4 signer and an SDK-owned retry scheduler. */
        public static Wiring generatedClient() {
            return new Wiring(false, false, FaultRig.defaultCallTimeout());
        }
    }

    /** Runs {@code call} on the rig's driver: {@code sync} for a sync rig, {@code async} for an async one. */
    public static FaultOutcome run(FaultRig rig, Wiring wiring, Supplier<?> sync,
                                   Supplier<? extends CompletableFuture<?>> async) {
        return rig.mode() == HttpTransportMode.SYNC ? runSync(rig, wiring, sync) : runAsync(rig, wiring, async);
    }

    private static FaultOutcome runAsync(FaultRig rig, Wiring wiring, Supplier<? extends CompletableFuture<?>> async) {
        boolean signed = wiring.signerWired();
        boolean scheduled = wiring.schedulerWired();
        CompletableFuture<?> future;
        try {
            future = async.get();
        } catch (RuntimeException | Error e) {
            rig.violations().record("the async call threw instead of failing its future: " + e);
            return rig.outcome(FaultOutcome.Terminal.FAILED, null, e, signed, scheduled);
        }
        CancelPoint cancel = rig.trace().cancel();
        if (cancel != CancelPoint.NONE) {
            awaitCancelPoint(rig, wiring, cancel);
            if (!future.cancel(true)) {
                rig.violations().record("cancellation at " + cancel + " lost: the call had already ended");
            }
        }
        try {
            future.handle((value, error) -> null).get(wiring.callTimeout().toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return rig.outcome(FaultOutcome.Terminal.DID_NOT_COMPLETE, null, null, signed, scheduled);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the call", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
        if (future.isCancelled()) {
            return rig.outcome(FaultOutcome.Terminal.CANCELLED, null, failureOf(future), signed, scheduled);
        }
        if (future.isCompletedExceptionally()) {
            return rig.outcome(FaultOutcome.Terminal.FAILED, null, failureOf(future), signed, scheduled);
        }
        return rig.outcome(FaultOutcome.Terminal.SUCCEEDED, future.join(), null, signed, scheduled);
    }

    private static FaultOutcome runSync(FaultRig rig, Wiring wiring, Supplier<?> sync) {
        boolean signed = wiring.signerWired();
        CompletableFuture<Object> result = new CompletableFuture<>();
        AtomicBoolean interruptPreserved = new AtomicBoolean();
        Thread caller = new Thread(() -> {
            try {
                result.complete(sync.get());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            } finally {
                interruptPreserved.set(Thread.interrupted());
            }
        }, "fault-sync-caller");
        caller.setDaemon(true);
        caller.start();
        CancelPoint cancel = rig.trace().cancel();
        if (cancel != CancelPoint.NONE) {
            awaitCancelPoint(rig, wiring, cancel);
            caller.interrupt();
        }
        Duration timeout = wiring.callTimeout();
        try {
            result.handle((value, error) -> null).get(timeout.toNanos(), TimeUnit.NANOSECONDS);
            caller.join(timeout.toMillis());
        } catch (TimeoutException e) {
            caller.interrupt();
            return rig.outcome(FaultOutcome.Terminal.DID_NOT_COMPLETE, null, null, signed, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the call", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
        if (!result.isCompletedExceptionally()) {
            if (cancel != CancelPoint.NONE && interruptPreserved.get()) {
                rig.violations().record("the sync call succeeded but left its caller interrupted");
            }
            return rig.outcome(FaultOutcome.Terminal.SUCCEEDED, result.join(), null, signed, false);
        }
        Throwable failure = failureOf(result);
        if (cancel != CancelPoint.NONE && !interruptPreserved.get()) {
            rig.violations().record("the sync driver cleared the caller's interrupt status");
        }
        boolean cancelled = rig.telemetry().calls().size() == 1
                            && rig.telemetry().calls().get(0).outcome() == TelemetryOutcome.CANCELLED;
        return rig.outcome(cancelled ? FaultOutcome.Terminal.CANCELLED : FaultOutcome.Terminal.FAILED, null, failure,
                           signed, false);
    }

    /** Waits until the call is parked where the trace cancels it. */
    private static void awaitCancelPoint(FaultRig rig, Wiring wiring, CancelPoint cancel) {
        Duration timeout = wiring.callTimeout();
        switch (cancel) {
            case IDENTITY -> rig.identities().awaitHeld(timeout);
            case TRANSMISSION, RESPONSE_BODY -> rig.http().awaitParked(timeout);
            case RETRY_DELAY -> {
                if (rig.mode() == HttpTransportMode.ASYNC && wiring.schedulerWired()) {
                    rig.scheduler().awaitPending(timeout);
                } else {
                    // The driver waits right after the retry decision; a cancellation that lands before the wait
                    // begins ends it at once (an interrupted sleep throws; a cancelled call schedules no attempt), so
                    // waiting for the decision is enough.
                    rig.retryStrategy().awaitEntry(RecordingRetryStrategy.Kind.RETRY, timeout);
                }
            }
            default -> throw new IllegalStateException("Not a cancellation point: " + cancel);
        }
    }

    private static Throwable failureOf(CompletableFuture<?> future) {
        Throwable failure = future.handle((value, error) -> error).join();
        return failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
    }
}
