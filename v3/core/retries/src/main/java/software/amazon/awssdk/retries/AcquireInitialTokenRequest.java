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

import java.util.Objects;

/**
 * Input of {@link RetryStrategy#acquireInitialToken}.
 *
 * @param scope the retry-quota scope; calls with the same scope share one retry quota (and, for the adaptive
 *              strategy, one client-side rate limiter)
 * @param longPolling whether the operation is a long-polling operation; when its retry quota is exhausted the strategy
 *                    still returns the backoff delay with the {@link RetryDecision.Stop} so that a polling loop does
 *                    not spin
 */
public record AcquireInitialTokenRequest(String scope, boolean longPolling) {
    public AcquireInitialTokenRequest {
        Objects.requireNonNull(scope, "scope");
    }

    /** A request for {@code scope} of an operation that is not long polling. */
    public static AcquireInitialTokenRequest create(String scope) {
        return new AcquireInitialTokenRequest(scope, false);
    }
}
