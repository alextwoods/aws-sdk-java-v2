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

package software.amazon.awssdk.protocols.json.internal.marshall;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Tracks recently observed marshalled-body sizes per operation, so the marshalling buffer can be allocated once at
 * (approximately) the right size instead of growing to it by doubling. A 50 KB body reached from the default 1 KB
 * initial buffer allocates ~127 KB of cumulative garbage; reached from a correct hint it allocates 64 KB once.
 *
 * <p>One instance is held per protocol factory, which is per client, so hints reflect that client's workload.
 *
 * <p>The hint tracks the largest recently seen size: it jumps up immediately when a larger body is observed and
 * decays by 1/8th on each smaller observation, so a one-off large request stops inflating the hint after a few calls
 * while steady-state workloads stabilize. Hints are clamped to [{@value #MIN_HINT}, {@value #MAX_HINT}]; above the
 * max, {@code SdkByteArrayOutputStream} switches to chunked storage and the base-buffer size no longer matters.
 */
@ThreadSafe
@SdkInternalApi
public final class MarshallBufferSizeHints {
    private static final int MIN_HINT = 1024;
    private static final int MAX_HINT = 128 * 1024;

    /**
     * Cap on tracked operations, as a defensive bound; a service client has a fixed, small operation set.
     */
    private static final int MAX_TRACKED_OPERATIONS = 512;

    private final Map<String, AtomicInteger> hints = new ConcurrentHashMap<>();

    /**
     * The initial buffer capacity to use for the given operation, based on recently observed body sizes.
     *
     * @param operationId the operation identifier, possibly null (returns the default).
     */
    public int hintFor(String operationId) {
        if (operationId == null) {
            return MIN_HINT;
        }
        AtomicInteger hint = hints.get(operationId);
        return hint != null ? hint.get() : MIN_HINT;
    }

    /**
     * Record the size of a just-marshalled body for the given operation.
     */
    public void record(String operationId, int size) {
        if (operationId == null) {
            return;
        }

        AtomicInteger hint = hints.get(operationId);
        if (hint == null) {
            if (hints.size() >= MAX_TRACKED_OPERATIONS) {
                return;
            }
            hint = hints.computeIfAbsent(operationId, k -> new AtomicInteger(MIN_HINT));
        }

        int clamped = Math.max(MIN_HINT, Math.min(size, MAX_HINT));
        // Grow immediately; decay by 1/8th of the gap per observation, always by at least 1 so integer division
        // cannot stall the hint just above a smaller steady-state size.
        hint.updateAndGet(current -> clamped >= current
                                     ? clamped
                                     : current - Math.max(1, (current - clamped) / 8));
    }
}
