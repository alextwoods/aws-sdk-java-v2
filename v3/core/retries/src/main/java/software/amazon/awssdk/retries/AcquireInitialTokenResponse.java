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

import java.time.Duration;
import java.util.Objects;

/**
 * Output of {@link RetryStrategy#acquireInitialToken}.
 *
 * @param token the {@link RetryToken.State#ACTIVE} token authorizing attempt 1
 * @param delay the non-negative wait before sending attempt 1; zero unless a client-side rate limiter is throttling
 */
public record AcquireInitialTokenResponse(RetryToken token, Duration delay) {
    public AcquireInitialTokenResponse {
        Objects.requireNonNull(token, "token");
        Delays.requireNonNegative(delay, "delay");
    }
}
