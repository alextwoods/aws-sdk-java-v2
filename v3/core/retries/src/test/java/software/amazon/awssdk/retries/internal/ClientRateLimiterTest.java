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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ClientRateLimiterTest {
    private final AtomicLong nanos = new AtomicLong(1_000_000L);
    private final ClientRateLimiter limiter = new ClientRateLimiter(nanos::get);

    private void sendAt(int requestsPerSecond, Duration period) {
        long step = 1_000_000_000L / requestsPerSecond;
        for (long elapsed = 0; elapsed < period.toNanos(); elapsed += step) {
            nanos.addAndGet(step);
            Duration wait = limiter.reserve();
            nanos.addAndGet(wait.toNanos());
            limiter.updateAfterSuccess();
        }
    }

    @Test
    void throttlingCutsTheRateAndSuccessesRecoverIt() {
        assertFalse(limiter.enabled());
        sendAt(10, Duration.ofSeconds(5));
        limiter.updateAfterThrottling();
        assertTrue(limiter.enabled());
        double throttled = limiter.fillRate();
        // CUBIC multiplicative decrease: about 0.7 of the measured ~10 rps.
        assertTrue(throttled > 5 && throttled < 8, "throttled rate " + throttled);
        sendAt(10, Duration.ofSeconds(10));
        assertTrue(limiter.fillRate() > throttled, "recovered rate " + limiter.fillRate());
    }

    @Test
    void reservationsQueueBehindDebtAndRepayOverTime() {
        sendAt(10, Duration.ofSeconds(2));
        limiter.updateAfterThrottling();
        double rate = limiter.fillRate();
        Duration last = Duration.ZERO;
        for (int i = 0; i < 20; i++) {
            last = limiter.reserve();
        }
        assertTrue(last.compareTo(Duration.ZERO) > 0);
        Duration next = limiter.reserve();
        // Each further reservation at the same instant waits one more send interval.
        long intervalNanos = (long) Math.ceil(1_000_000_000.0 / rate);
        assertEquals((double) intervalNanos, (double) next.minus(last).toNanos(), 2.0);
        // After the debt is repaid (plus some refill), sending is free again.
        nanos.addAndGet(next.toNanos() + 2 * intervalNanos);
        assertEquals(Duration.ZERO, limiter.reserve());
    }

    @Test
    void disabledLimiterIsFree() {
        for (int i = 0; i < 1_000; i++) {
            assertEquals(Duration.ZERO, limiter.reserve());
        }
    }
}
