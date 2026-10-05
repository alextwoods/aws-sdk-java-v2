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

package software.amazon.awssdk.core.waiters;

import java.util.Objects;

/**
 * The successful outcome of a wait.
 *
 * @param <T> the polled operation's response type
 */
public final class WaiterResponse<T> {
    private final ResponseOrException<T> matched;
    private final int attemptsExecuted;

    WaiterResponse(ResponseOrException<T> matched, int attemptsExecuted) {
        this.matched = Objects.requireNonNull(matched, "matched");
        this.attemptsExecuted = attemptsExecuted;
    }

    /** The poll result a success acceptor matched. */
    public ResponseOrException<T> matched() {
        return matched;
    }

    /** The number of polls executed, at least one. */
    public int attemptsExecuted() {
        return attemptsExecuted;
    }

    @Override
    public String toString() {
        return "WaiterResponse(matched=" + matched + ", attemptsExecuted=" + attemptsExecuted + ")";
    }
}
