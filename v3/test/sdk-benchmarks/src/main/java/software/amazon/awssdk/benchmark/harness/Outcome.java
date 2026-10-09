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

/**
 * What the mock service makes of each logical operation (Req 20.3).
 *
 * <ul>
 *     <li>{@code success}: every attempt succeeds.</li>
 *     <li>{@code retry}: the service answers each request with a retryable 5xx service error with probability 1/2,
 *     decided by a fixed coin per request number, so an operation costs one failed attempt on average and the exact
 *     count is recorded by the service. V3 sends no per-attempt header the service could key on, and plain
 *     alternation correlates with lockstep callers. Clients run retry cells with a retry strategy that has no
 *     backoff, no circuit breaker and {@code 64} attempts, so exhausting it (probability 2^-63 per operation) does
 *     not happen in practice.</li>
 *     <li>{@code error}: every attempt gets a modeled, non-retryable service error, which the client must surface as
 *     the generated (or dynamic) modeled exception. Anything else is a failed operation.</li>
 * </ul>
 */
public enum Outcome {
    SUCCESS("success"),
    RETRY("retry"),
    MODELED_ERROR("error");

    private final String id;

    Outcome(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Outcome fromId(String id) {
        for (Outcome o : values()) {
            if (o.id.equals(id)) {
                return o;
            }
        }
        throw new IllegalArgumentException("unknown outcome: " + id);
    }
}
