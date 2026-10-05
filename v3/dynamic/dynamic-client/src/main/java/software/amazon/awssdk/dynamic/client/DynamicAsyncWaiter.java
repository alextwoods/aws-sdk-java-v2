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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The modeled waiters of a {@link DynamicAsyncClient}'s service, by exact waiter name (Requirement 14.15): the
 * dynamic counterpart of a generated {@code <Service>AsyncWaiter}, on the same sdk-core {@link Waiter#runAsync}
 * runtime with the modeled acceptors in model order and the modeled delays.
 *
 * <p>Waiting never blocks the caller: {@code waitUntil} returns its future at once; each poll is one async call on
 * the exact client the waiter was built with, and each delay between polls is a future of the configured scheduler
 * (by default the client's own retry scheduler, as {@code client.waiter()} of a generated async client uses), never
 * the common pool. Cancelling the returned future cancels the pending poll, through the shared pipeline (its
 * exchange, body subscription or retry delay), or the pending delay, and no further poll starts. The waiter borrows
 * its client and scheduler and closes neither.
 *
 * <p>Immutable and thread safe.
 */
public final class DynamicAsyncWaiter {
    private final DynamicAsyncClient client;
    private final WaiterOverrideConfiguration overrides;
    private final RetryScheduler scheduler;

    private DynamicAsyncWaiter(Builder builder) {
        this.client = Objects.requireNonNull(builder.client, "client");
        this.overrides = builder.overrides;
        RetryScheduler configured = builder.scheduler;
        if (configured == null && builder.executor != null) {
            configured = RetryScheduler.of(builder.executor);
        }
        this.scheduler = configured != null ? configured : client.calls().retryScheduler().orElseThrow(
            () -> new IllegalStateException("The async client has no retry scheduler"));
    }

    /** A builder of a waiter. */
    public static Builder builder() {
        return new Builder();
    }

    /** The modeled waiter names of the client's service, in lexical order. */
    public List<String> waiterNames() {
        return client.service().waiterNames();
    }

    /**
     * Polls until the waiter named exactly {@code waiterName} ends.
     *
     * @see #waitUntil(String, Document, WaiterOverrideConfiguration)
     */
    public CompletableFuture<WaiterResponse<Document>> waitUntil(String waiterName, Document input) {
        return waitUntil(waiterName, input, null);
    }

    /**
     * Starts polling the waiter's operation with {@code input} until the waiter named exactly {@code waiterName}
     * succeeds, with the per-wait overrides, and returns the wait's future before the first poll completes.
     *
     * @param waiterName the exact (case-sensitive) modeled waiter name
     * @param input the polled operation's input document; validated once, before the first poll
     * @param overrideConfiguration the per-wait overrides, or {@code null}
     * @return the future of the poll result a success acceptor matched and the number of polls. It fails with a
     *     {@link software.amazon.awssdk.dynamic.model.DynamicModelException} for an unknown waiter name, a
     *     {@link DynamicInvocationException} for a nonconforming input or unevaluable acceptors (in these cases
     *     nothing was sent), or a {@link software.amazon.awssdk.core.exception.SdkClientException} when a failure
     *     acceptor matched, a poll failed with an exception no acceptor matched, or the attempts or time ran out
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<WaiterResponse<Document>> waitUntil(String waiterName, Document input,
                                                                 WaiterOverrideConfiguration overrideConfiguration) {
        Objects.requireNonNull(waiterName, "waiterName");
        Objects.requireNonNull(input, "input");
        DynamicCalls calls = client.calls();
        DynamicCalls.Call call;
        Waiter<Document> waiter;
        try {
            DynamicWaiters.Compiled compiled = calls.waiter(waiterName);
            call = calls.prepare(compiled.operationName(), input);
            waiter = compiled.waiter(call.input(), overrides);
        } catch (SdkException rejected) {
            return CompletableFuture.failedFuture(rejected);
        }
        // Each poll is the engine's own call future, so cancelling the wait cancels the pending call.
        return waiter.runAsync(() -> (CompletableFuture<Document>) (CompletableFuture<?>) calls.engine().executeAsync(
            call.plan(), call.input(), null, null), scheduler::delay, overrideConfiguration);
    }

    @Override
    public String toString() {
        return "DynamicAsyncWaiter[" + client.service().id() + "]";
    }

    /** Builder of a {@link DynamicAsyncWaiter}; not thread safe. */
    public static final class Builder {
        private DynamicAsyncClient client;
        private WaiterOverrideConfiguration overrides;
        private RetryScheduler scheduler;
        private ScheduledExecutorService executor;

        private Builder() {
        }

        /** The client to poll: required, borrowed, never closed by the waiter. */
        public Builder client(DynamicAsyncClient client) {
            this.client = Objects.requireNonNull(client, "client");
            return this;
        }

        /** Overrides of every wait of the waiter. */
        public Builder overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration) {
            this.overrides = overrideConfiguration;
            return this;
        }

        /** The executor delays are scheduled on: caller-owned, never closed by the waiter. */
        public Builder scheduledExecutorService(ScheduledExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * The scheduler of delays: caller-owned. Takes precedence over a scheduled executor; without either, the
         * client's retry scheduler.
         */
        public Builder retryScheduler(RetryScheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        /**
         * The waiter.
         *
         * @throws NullPointerException if no client was given
         */
        public DynamicAsyncWaiter build() {
            return new DynamicAsyncWaiter(this);
        }
    }
}
