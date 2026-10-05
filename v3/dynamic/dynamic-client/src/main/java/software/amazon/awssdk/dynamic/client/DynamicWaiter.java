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
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The modeled waiters of a {@link DynamicClient}'s service, by exact waiter name (Requirement 14.15): the dynamic
 * counterpart of a generated {@code <Service>Waiter}, on the same sdk-core {@link Waiter} runtime with the modeled
 * acceptors in model order and the modeled delays.
 *
 * <p>Each {@code waitUntil} polls the waiter's operation on the exact client the waiter was built with (same plan,
 * engine and configuration) and blocks the calling thread until an acceptor reaches a terminal state, the wait's
 * attempts or time are exhausted, or the thread is interrupted. The waiter borrows its client and never closes it.
 *
 * <p>Immutable and thread safe.
 */
public final class DynamicWaiter {
    private final DynamicClient client;
    private final WaiterOverrideConfiguration overrides;

    private DynamicWaiter(Builder builder) {
        this.client = Objects.requireNonNull(builder.client, "client");
        this.overrides = builder.overrides;
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
     * Polls until the waiter named exactly {@code waiterName} succeeds or fails.
     *
     * @see #waitUntil(String, Document, WaiterOverrideConfiguration)
     */
    public WaiterResponse<Document> waitUntil(String waiterName, Document input) {
        return waitUntil(waiterName, input, null);
    }

    /**
     * Polls the waiter's operation with {@code input} until the waiter named exactly {@code waiterName} succeeds, with
     * the per-wait overrides.
     *
     * @param waiterName the exact (case-sensitive) modeled waiter name
     * @param input the polled operation's input document; validated once, before the first poll
     * @param overrideConfiguration the per-wait overrides, or {@code null}
     * @return the poll result a success acceptor matched (an output document, or the matched exception) and the
     *     number of polls
     * @throws software.amazon.awssdk.dynamic.model.DynamicModelException naming the service, the unknown name and
     *         every waiter name in lexical order, if the service has no such waiter; nothing was sent
     * @throws DynamicInvocationException if the input does not conform or the waiter's acceptors cannot be
     *         evaluated; nothing was sent
     * @throws software.amazon.awssdk.core.exception.SdkClientException if a failure acceptor matched, a poll failed
     *         with an exception no acceptor matched (its cause), or the wait's attempts or time were exhausted
     */
    public WaiterResponse<Document> waitUntil(String waiterName, Document input,
                                              WaiterOverrideConfiguration overrideConfiguration) {
        Objects.requireNonNull(waiterName, "waiterName");
        Objects.requireNonNull(input, "input");
        DynamicCalls calls = client.calls();
        DynamicWaiters.Compiled compiled = calls.waiter(waiterName);
        DynamicCalls.Call call = calls.prepare(compiled.operationName(), input);
        Waiter<Document> waiter = compiled.waiter(call.input(), overrides);
        return waiter.run(() -> calls.engine().executeSync(call.plan(), call.input(), null, null),
                          overrideConfiguration);
    }

    @Override
    public String toString() {
        return "DynamicWaiter[" + client.service().id() + "]";
    }

    /** Builder of a {@link DynamicWaiter}; not thread safe. */
    public static final class Builder {
        private DynamicClient client;
        private WaiterOverrideConfiguration overrides;

        private Builder() {
        }

        /** The client to poll: required, borrowed, never closed by the waiter. */
        public Builder client(DynamicClient client) {
            this.client = Objects.requireNonNull(client, "client");
            return this;
        }

        /** Overrides of every wait of the waiter. */
        public Builder overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration) {
            this.overrides = overrideConfiguration;
            return this;
        }

        /**
         * The waiter.
         *
         * @throws NullPointerException if no client was given
         */
        public DynamicWaiter build() {
            return new DynamicWaiter(this);
        }
    }
}
