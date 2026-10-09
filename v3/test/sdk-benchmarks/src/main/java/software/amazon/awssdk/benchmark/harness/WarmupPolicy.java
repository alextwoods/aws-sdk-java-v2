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

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

/**
 * How a client repetition warms up before measuring.
 *
 * <ul>
 *     <li>{@link Kind#STEADY_STATE}: the JIT steady-state gate of {@link SteadyStatePolicy}; required in
 *     {@code HarnessMode.RELEASE} with {@link SteadyStatePolicy#RELEASE}.</li>
 *     <li>{@link Kind#FIXED}: a fixed duration, for exploratory runs only. Such a repetition never passed a gate, so
 *     the validity rules mark it invalid (it is not steady-state evidence).</li>
 * </ul>
 *
 * <p>Either way, the measured window is sampled at {@code sampling.interval()} so its rolling stability is checked.
 *
 * @param fixedDuration the fixed warmup; zero for {@link Kind#STEADY_STATE}
 */
public record WarmupPolicy(Kind kind, Duration fixedDuration, SteadyStatePolicy sampling) {

    public enum Kind {
        FIXED, STEADY_STATE;

        public String id() {
            return this == FIXED ? "fixed-duration" : "steady-state";
        }

        public static Kind fromId(String id) {
            return switch (id.toLowerCase(Locale.ROOT)) {
                case "fixed", "fixed-duration" -> FIXED;
                case "steady-state" -> STEADY_STATE;
                default -> throw new IllegalArgumentException("unknown warmup policy: " + id);
            };
        }
    }

    public WarmupPolicy {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(sampling, "sampling");
        if (fixedDuration.isNegative()) {
            throw new IllegalArgumentException("fixed warmup must be non-negative");
        }
    }

    public static WarmupPolicy fixed(Duration duration, SteadyStatePolicy sampling) {
        return new WarmupPolicy(Kind.FIXED, duration, sampling);
    }

    public static WarmupPolicy steadyState(SteadyStatePolicy policy) {
        return new WarmupPolicy(Kind.STEADY_STATE, Duration.ZERO, policy);
    }
}
