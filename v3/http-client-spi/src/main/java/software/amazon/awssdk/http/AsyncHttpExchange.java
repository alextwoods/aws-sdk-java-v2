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

import java.util.concurrent.CompletableFuture;

/**
 * An exchange started by {@link SdkAsyncHttpClient#execute}: its completion and its cancellation handle.
 *
 * <p>Terminal uniqueness (Requirement 12.8): {@link #completion()} completes exactly once, normally when the response
 * body subscriber has received {@code onComplete}, or exceptionally with an {@link HttpTransportException} when the
 * exchange fails or is cancelled. Exactly one of the handler's {@code onError} or the body subscriber's terminal
 * signal accompanies it. Completing or cancelling the returned future has no effect on the exchange; use
 * {@link #cancel}.
 *
 * <p>{@link AsyncExchangeCompletion} implements this contract for transports.
 */
public interface AsyncHttpExchange {
    /** The exchange's completion; the same future on every call. */
    CompletableFuture<Void> completion();

    /**
     * Cancels the exchange: pending acquisition, transmission and response consumption stop, the connection is
     * discarded, and {@link #completion()} fails with an {@link HttpTransportException} of category
     * {@link HttpFailureCategory#CANCELLED}, all within {@link HttpClientCapabilities#cancellationTimeout()}.
     * Thread safe.
     *
     * @return true if this call cancelled the exchange; false if it had already terminated or been cancelled
     */
    boolean cancel(CancellationReason reason);
}
