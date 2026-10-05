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

package software.amazon.awssdk.retries;

/**
 * A single-use authorization for one attempt of one call, issued by a {@link RetryStrategy}.
 *
 * <p>{@link #attempt()} is the one-based number of the attempt this token authorizes; the AWS pipeline exposes it
 * through the attempt-scoped execution context (Requirement 10.8). A token starts {@link State#ACTIVE} and moves once
 * to a terminal state when it is passed back to its strategy.
 */
public interface RetryToken {
    /** Lifecycle of a token. */
    enum State {
        /** Issued and not yet passed back to the strategy; its attempt may be sent. */
        ACTIVE,
        /** Refreshed into a token for the next attempt. */
        RETRIED,
        /** Its attempt succeeded. */
        SUCCEEDED,
        /** Its attempt failed and the call made no further attempt. */
        FAILED
    }

    /** The retry-quota scope of the call. */
    String scope();

    /** The one-based number of the attempt this token authorizes. */
    int attempt();

    /** The current state of this token. */
    State state();

    /** The retry quota drawn to issue this token: zero for attempt 1 and when the circuit breaker is disabled. */
    int quotaAcquired();

    /** The scope's remaining retry quota when this token was issued or consumed. */
    int quotaRemaining();
}
