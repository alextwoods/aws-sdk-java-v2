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

package software.amazon.awssdk.benchmark.dynamodb;

/**
 * Whole-process CPU time, ported from racecar's {@code CpuTimeSource}. Exactly one implementation is bound at startup
 * (see {@link CpuTimeSources}).
 */
interface CpuTimeSource {
    /** Human-readable source name for the run header. */
    String name();

    /** Whether {@link Snapshot#userNanos()} / {@link Snapshot#systemNanos()} are populated. */
    boolean hasUserSystemSplit();

    Snapshot snapshot();

    /** A point-in-time reading; {@code userNanos}/{@code systemNanos} are -1 when the source has no split. */
    record Snapshot(long totalNanos, long userNanos, long systemNanos) {
        static Snapshot ofSplit(long userNanos, long systemNanos) {
            return new Snapshot(userNanos + systemNanos, userNanos, systemNanos);
        }

        static Snapshot ofTotal(long totalNanos) {
            return new Snapshot(totalNanos, -1, -1);
        }

        boolean hasSplit() {
            return userNanos >= 0 && systemNanos >= 0;
        }

        /** Difference between two snapshots from the same source; preserves split absence. */
        Snapshot minus(Snapshot other) {
            if (hasSplit() && other.hasSplit()) {
                return ofSplit(userNanos - other.userNanos, systemNanos - other.systemNanos);
            }
            return ofTotal(totalNanos - other.totalNanos);
        }
    }
}
