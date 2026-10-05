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

import java.util.Optional;

/**
 * The poll result that ended a wait: exactly one of a response and an exception.
 *
 * @param <T> the polled operation's response type
 */
public final class ResponseOrException<T> {
    private final T response;
    private final Throwable exception;

    private ResponseOrException(T response, Throwable exception) {
        this.response = response;
        this.exception = exception;
    }

    /** A successful poll's response. */
    public static <T> ResponseOrException<T> response(T response) {
        return new ResponseOrException<>(response, null);
    }

    /** A failed poll's exception. */
    public static <T> ResponseOrException<T> exception(Throwable exception) {
        return new ResponseOrException<>(null, exception);
    }

    /** The response, when the poll succeeded. */
    public Optional<T> response() {
        return Optional.ofNullable(response);
    }

    /** The exception, when the poll failed. */
    public Optional<Throwable> exception() {
        return Optional.ofNullable(exception);
    }

    @Override
    public String toString() {
        return exception != null ? "ResponseOrException(exception=" + exception + ")"
                                 : "ResponseOrException(response=" + response + ")";
    }
}
