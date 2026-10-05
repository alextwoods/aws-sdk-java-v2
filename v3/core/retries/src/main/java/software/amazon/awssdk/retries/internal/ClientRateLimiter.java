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

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * The adaptive mode's client-side rate limiter of one scope: a token bucket whose fill rate follows the CUBIC
 * congestion-control algorithm, lowered on throttling responses and raised on successes (the same algorithm and
 * constants as the V2 adaptive strategy).
 *
 * <p>Unlike V2, the limiter never blocks or owns a thread. {@link #reserve()} takes one send token immediately, even
 * into debt, and returns how long the caller must wait before sending; later callers queue behind the debt. The AWS
 * pipeline waits the returned delay (sleeping in sync calls, scheduling in async calls). Before the first throttling
 * response the limiter is disabled and every reservation is free.
 *
 * <p>Thread safe; state changes are short critical sections without allocation.
 */
public final class ClientRateLimiter {
    private static final double MIN_FILL_RATE = 0.5;
    private static final double MIN_CAPACITY = 1.0;
    private static final double SMOOTH = 0.8;
    private static final double BETA = 0.7;
    private static final double SCALE_CONSTANT = 0.4;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final LongSupplier nanoClock;
    private final long origin;

    private double fillRate;
    private double maxCapacity;
    private double currentCapacity;
    private boolean lastTimestampIsSet;
    private double lastTimestamp;
    private boolean enabled;
    private double measuredTxRate;
    private double lastTxRateBucket;
    private long requestCount;
    private double lastMaxRate;
    private double lastThrottleTime;
    private double timeWindow;

    public ClientRateLimiter(LongSupplier nanoClock) {
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
        this.origin = nanoClock.getAsLong();
    }

    /** Takes one send token; the wait before the caller may send (zero while the limiter is disabled). */
    public synchronized Duration reserve() {
        if (!enabled) {
            return Duration.ZERO;
        }
        refill(now());
        currentCapacity -= 1.0;
        if (currentCapacity >= 0) {
            return Duration.ZERO;
        }
        double waitSeconds = -currentCapacity / fillRate;
        return Duration.ofNanos((long) Math.ceil(waitSeconds * NANOS_PER_SECOND));
    }

    /** Lowers the send rate after a throttling response and enables the limiter. */
    public synchronized void updateAfterThrottling() {
        updateSendingRate(true);
    }

    /** Raises the send rate after a successful response. */
    public synchronized void updateAfterSuccess() {
        updateSendingRate(false);
    }

    /** Whether a throttling response has enabled the limiter. */
    public synchronized boolean enabled() {
        return enabled;
    }

    /** The current fill rate in sends per second. */
    public synchronized double fillRate() {
        return fillRate;
    }

    private double now() {
        return (nanoClock.getAsLong() - origin) / NANOS_PER_SECOND;
    }

    private void updateSendingRate(boolean throttling) {
        double time = now();
        updateMeasuredRate(time);
        double calculatedRate;
        if (throttling) {
            double rateToUse = enabled ? Math.min(measuredTxRate, fillRate) : measuredTxRate;
            lastMaxRate = rateToUse;
            calculateTimeWindow();
            lastThrottleTime = time;
            calculatedRate = rateToUse * BETA;
            enabled = true;
        } else {
            calculateTimeWindow();
            calculatedRate = SCALE_CONSTANT * Math.pow(time - lastThrottleTime - timeWindow, 3) + lastMaxRate;
        }
        updateRate(time, Math.min(calculatedRate, 2 * measuredTxRate));
    }

    private void refill(double time) {
        if (lastTimestampIsSet) {
            double fillAmount = (time - lastTimestamp) * fillRate;
            currentCapacity = Math.min(maxCapacity, currentCapacity + fillAmount);
        }
        lastTimestamp = time;
        lastTimestampIsSet = true;
    }

    private void updateRate(double time, double newRps) {
        refill(time);
        fillRate = Math.max(newRps, MIN_FILL_RATE);
        maxCapacity = Math.max(newRps, MIN_CAPACITY);
        currentCapacity = Math.min(currentCapacity, maxCapacity);
    }

    private void updateMeasuredRate(double time) {
        requestCount += 1;
        double timeBucket = Math.floor(time * 2) / 2;
        if (timeBucket > lastTxRateBucket) {
            double currentRate = requestCount / (timeBucket - lastTxRateBucket);
            measuredTxRate = currentRate * SMOOTH + measuredTxRate * (1 - SMOOTH);
            requestCount = 0;
            lastTxRateBucket = timeBucket;
        }
    }

    private void calculateTimeWindow() {
        timeWindow = Math.cbrt(lastMaxRate * (1 - BETA) / SCALE_CONSTANT);
    }
}
