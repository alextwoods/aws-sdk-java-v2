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

package software.amazon.awssdk.retries.internal;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.awssdk.utils.ToString;

/** The token of {@link TokenRetryCore}: one attempt's authorization with a one-way state transition. */
public final class DefaultRetryToken implements RetryToken {
    private static final AtomicReferenceFieldUpdater<DefaultRetryToken, State> STATE =
        AtomicReferenceFieldUpdater.newUpdater(DefaultRetryToken.class, State.class, "state");

    final TokenRetryCore owner;
    final ScopeStore.State scopeState;
    final boolean longPolling;
    private final String scope;
    private final int attempt;
    private final int quotaAcquired;
    private volatile int quotaRemaining;
    private volatile State state = State.ACTIVE;

    DefaultRetryToken(TokenRetryCore owner, ScopeStore.State scopeState, String scope, boolean longPolling,
                      int attempt, int quotaAcquired, int quotaRemaining) {
        this.owner = owner;
        this.scopeState = scopeState;
        this.scope = scope;
        this.longPolling = longPolling;
        this.attempt = attempt;
        this.quotaAcquired = quotaAcquired;
        this.quotaRemaining = quotaRemaining;
    }

    @Override
    public String scope() {
        return scope;
    }

    @Override
    public int attempt() {
        return attempt;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public int quotaAcquired() {
        return quotaAcquired;
    }

    @Override
    public int quotaRemaining() {
        return quotaRemaining;
    }

    /** Moves this token from {@link State#ACTIVE} to {@code to}; false if it was no longer active. */
    boolean consume(State to) {
        return STATE.compareAndSet(this, State.ACTIVE, to);
    }

    /** Records the scope's remaining quota when this token was consumed. */
    void quotaRemaining(int remaining) {
        this.quotaRemaining = remaining;
    }

    @Override
    public String toString() {
        return ToString.builder("RetryToken")
                       .add("scope", scope)
                       .add("attempt", attempt)
                       .add("state", state)
                       .add("quotaAcquired", quotaAcquired)
                       .add("quotaRemaining", quotaRemaining)
                       .build();
    }
}
