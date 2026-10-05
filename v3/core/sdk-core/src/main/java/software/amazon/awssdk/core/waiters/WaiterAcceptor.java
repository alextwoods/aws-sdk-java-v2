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
import java.util.function.Predicate;

/**
 * One modeled waiter acceptor: the state a poll result transitions to when it matches. A response acceptor matches
 * only successful polls and an exception acceptor only failed ones.
 *
 * @param <T> the polled operation's response type
 */
public final class WaiterAcceptor<T> {
    private final WaiterState state;
    private final Predicate<? super T> response;
    private final Predicate<? super Throwable> exception;

    private WaiterAcceptor(WaiterState state, Predicate<? super T> response, Predicate<? super Throwable> exception) {
        this.state = Objects.requireNonNull(state, "state");
        this.response = response;
        this.exception = exception;
    }

    /** An acceptor matching successful polls whose response satisfies {@code predicate}. */
    public static <T> WaiterAcceptor<T> onResponse(WaiterState state, Predicate<? super T> predicate) {
        return new WaiterAcceptor<>(state, Objects.requireNonNull(predicate, "predicate"), null);
    }

    /** An acceptor matching failed polls whose exception satisfies {@code predicate}. */
    public static <T> WaiterAcceptor<T> onException(WaiterState state, Predicate<? super Throwable> predicate) {
        return new WaiterAcceptor<>(state, null, Objects.requireNonNull(predicate, "predicate"));
    }

    /** The state this acceptor transitions to. */
    public WaiterState state() {
        return state;
    }

    /** Whether this acceptor matches a successful poll's response. */
    public boolean matches(T value) {
        return response != null && response.test(value);
    }

    /** Whether this acceptor matches a failed poll's exception. */
    public boolean matchesException(Throwable value) {
        return exception != null && exception.test(value);
    }
}
