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

import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscription;

/** Demand accounting shared by the streaming-body publishers. */
final class Demand {
    /** A subscription that ignores every signal, for subscribers that are rejected before any data. */
    static final Subscription NOOP = new Subscription() {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
    };

    private Demand() {
    }

    /** Adds positive {@code n} to {@code demand}, capping at {@link Long#MAX_VALUE} (unbounded, rule 3.17). */
    static void add(AtomicLong demand, long n) {
        demand.accumulateAndGet(n, (current, delta) -> {
            long sum = current + delta;
            return sum < 0 ? Long.MAX_VALUE : sum;
        });
    }
}
