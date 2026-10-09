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

package software.amazon.awssdk.benchmark.harness;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * A preallocated log-linear latency histogram (64 linear sub-buckets per power of two, so a recorded value is reported
 * within 1/64 of itself). Recording never allocates, so it adds no bytes to the measured window's allocation.
 */
final class LatencyHistogram {
    private static final int SUB_BITS = 6;
    private static final int SUB = 1 << SUB_BITS;
    private static final int MAGNITUDES = 64 - SUB_BITS;
    private final AtomicLongArray counts = new AtomicLongArray(SUB * (MAGNITUDES + 1));

    void record(long nanos) {
        counts.incrementAndGet(index(Math.max(0, nanos)));
    }

    static int index(long v) {
        if (v < SUB) {
            return (int) v;
        }
        int magnitude = 63 - Long.numberOfLeadingZeros(v) - SUB_BITS + 1;
        int sub = (int) (v >>> (magnitude - 1)) - SUB;
        return magnitude * SUB + sub;
    }

    /** The upper bound of the values in bucket {@code i}. */
    static long upperBound(int i) {
        int magnitude = i / SUB;
        int sub = i % SUB;
        if (magnitude == 0) {
            return sub;
        }
        return ((long) (sub + SUB + 1) << (magnitude - 1)) - 1;
    }

    long count() {
        long n = 0;
        for (int i = 0; i < counts.length(); i++) {
            n += counts.get(i);
        }
        return n;
    }

    /** The value at quantile {@code q} in (0, 1], or -1 if nothing was recorded. */
    long quantile(double q) {
        long total = count();
        if (total == 0) {
            return -1;
        }
        long rank = Math.max(1, (long) Math.ceil(q * total));
        long seen = 0;
        for (int i = 0; i < counts.length(); i++) {
            seen += counts.get(i);
            if (seen >= rank) {
                return upperBound(i);
            }
        }
        return upperBound(counts.length() - 1);
    }
}
