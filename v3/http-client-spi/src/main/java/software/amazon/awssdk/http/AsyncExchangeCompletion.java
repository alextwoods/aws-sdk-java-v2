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

package software.amazon.awssdk.http;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * The terminal state of one async exchange, shared by every {@link SdkAsyncHttpClient} implementation so terminal
 * uniqueness holds by construction (Requirement 12.8): the first of {@link #complete()}, {@link #fail(Throwable)} and
 * {@link #cancel(CancellationReason)} wins, and each returns whether it won. A transport signals the handler or body
 * subscriber only when its call won (or, for {@link #cancel}, in the {@link CancelAction}), so exactly one terminal
 * signal is delivered even when completion, failure and cancellation race on different threads.
 *
 * <p>The transport records the current {@link HttpPhase} with {@link #enter(HttpPhase)}; a cancellation or an
 * unmapped failure is reported in that phase. Thread safe.
 */
public final class AsyncExchangeCompletion implements AsyncHttpExchange {
    private final CompletableFuture<Void> state = new CompletableFuture<>();
    private final CompletableFuture<Void> completion = state.copy();
    private final String clientName;
    private final CancelAction cancelAction;
    private volatile HttpPhase phase = HttpPhase.CONNECTION_ACQUISITION;

    /** The transport's reaction to a winning {@link #cancel}. */
    @FunctionalInterface
    public interface CancelAction {
        /**
         * Stops pending acquisition, transmission and response consumption, discards the connection, and signals
         * {@code failure} to the handler ({@code onError}) or, after {@code onStream}, to the body subscriber. Called
         * at most once, on the cancelling thread; must not block.
         */
        void cancelled(CancellationReason reason, HttpTransportException failure);
    }

    /**
     * @param clientName the transport's {@code clientName()}, recorded in failures
     * @param cancelAction run once if {@link #cancel} wins
     */
    public AsyncExchangeCompletion(String clientName, CancelAction cancelAction) {
        this.clientName = Objects.requireNonNull(clientName, "clientName");
        this.cancelAction = Objects.requireNonNull(cancelAction, "cancelAction");
    }

    /** Records that the exchange entered {@code phase}. */
    public void enter(HttpPhase phase) {
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    /** The current phase. */
    public HttpPhase phase() {
        return phase;
    }

    /** True once the exchange has terminated. */
    public boolean isDone() {
        return state.isDone();
    }

    /** Completes the exchange normally; true if this call terminated it. */
    public boolean complete() {
        return state.complete(null);
    }

    /**
     * Fails the exchange; true if this call terminated it. A failure that is not an {@link HttpTransportException}
     * is wrapped as an {@link HttpFailureCategory.Kind#IO} failure of the current phase.
     */
    public boolean fail(Throwable failure) {
        return state.completeExceptionally(asTransportException(failure));
    }

    /** {@code failure} as an {@link HttpTransportException}: itself, or wrapped in the current phase. */
    public HttpTransportException asTransportException(Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        if (failure instanceof HttpTransportException transport) {
            return transport;
        }
        return HttpTransportException.builder().phase(phase)
                                     .category(HttpFailureCategory.of(phase, HttpFailureCategory.Kind.IO))
                                     .clientName(clientName).cause(failure).build();
    }

    @Override
    public CompletableFuture<Void> completion() {
        return completion;
    }

    @Override
    public boolean cancel(CancellationReason reason) {
        Objects.requireNonNull(reason, "reason");
        HttpPhase current = phase;
        HttpTransportException failure =
            HttpTransportException.builder().phase(current).category(HttpFailureCategory.CANCELLED)
                                  .clientName(clientName)
                                  .message("The HTTP exchange was cancelled (" + reason + ") during " + current + ".")
                                  .build();
        if (!state.completeExceptionally(failure)) {
            return false;
        }
        cancelAction.cancelled(reason, failure);
        return true;
    }

    @Override
    public String toString() {
        return "AsyncExchangeCompletion[" + clientName + ", " + phase + (state.isDone() ? ", done" : "") + "]";
    }
}
