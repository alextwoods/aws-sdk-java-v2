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

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * The per-scope retry state of one strategy (and of the strategies copied from it): a retry quota and, for the
 * adaptive mode, a client-side rate limiter. At most {@link #MAX_SCOPES} scopes are retained; beyond that an arbitrary
 * other scope is evicted and starts afresh when next used, bounding memory for callers that use unbounded scope names.
 */
public final class ScopeStore {
    /** Maximum number of retained scopes. */
    public static final int MAX_SCOPES = 128;

    private final ConcurrentHashMap<String, State> scopes = new ConcurrentHashMap<>();
    private final int quotaCapacity;
    private final boolean adaptive;
    private final LongSupplier nanoClock;

    ScopeStore(int quotaCapacity, boolean adaptive, LongSupplier nanoClock) {
        this.quotaCapacity = quotaCapacity;
        this.adaptive = adaptive;
        this.nanoClock = nanoClock;
    }

    /** The state of {@code scope}, created on first use. */
    public State get(String scope) {
        State state = scopes.get(scope);
        if (state != null) {
            return state;
        }
        if (scopes.size() >= MAX_SCOPES) {
            Iterator<String> it = scopes.keySet().iterator();
            if (it.hasNext()) {
                scopes.remove(it.next());
            }
        }
        return scopes.computeIfAbsent(scope, s -> new State(new RetryQuota(quotaCapacity),
                                                            adaptive ? new ClientRateLimiter(nanoClock) : null));
    }

    boolean compatibleWith(int quotaCapacity, boolean adaptive) {
        return this.quotaCapacity == quotaCapacity && this.adaptive == adaptive;
    }

    /**
     * The state of one scope.
     *
     * @param quota the scope's retry quota
     * @param rateLimiter the scope's rate limiter, or null outside the adaptive mode
     */
    public record State(RetryQuota quota, ClientRateLimiter rateLimiter) {
    }
}
