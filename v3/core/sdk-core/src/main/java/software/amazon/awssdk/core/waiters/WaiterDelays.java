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

import java.time.Duration;

/** The Smithy waiter backoff ({@code smithy.waiters#waitable}, "Waiter retries"). */
final class WaiterDelays {
    private WaiterDelays() {
    }

    /**
     * The delay after attempt {@code attempt} (1-based), or {@code null} when the remaining time no longer allows a
     * poll: {@code minDelay * 2^(attempt - 1)} up to the attempt ceiling and {@code maxDelay} past it, jittered
     * uniformly between {@code minDelay} and that value by {@code random} in [0, 1), and shortened to
     * {@code remaining - minDelay} when it would leave less than {@code minDelay}.
     */
    static Duration delay(int attempt, Duration minDelay, Duration maxDelay, long remainingNanos, double random) {
        long min = minDelay.toNanos();
        long max = maxDelay.toNanos();
        if (remainingNanos <= min) {
            return null;
        }
        double ceiling = Math.log((double) max / min) / Math.log(2) + 1;
        long upper = attempt > ceiling ? max : Math.min(max, (long) (min * Math.pow(2, attempt - 1)));
        long delay = min + (long) ((upper - min) * random);
        if (remainingNanos - delay <= min) {
            delay = remainingNanos - min;
        }
        return Duration.ofNanos(delay);
    }
}
