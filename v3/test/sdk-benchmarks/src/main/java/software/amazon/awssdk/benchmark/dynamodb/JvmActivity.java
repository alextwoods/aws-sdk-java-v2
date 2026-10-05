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

import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/** JIT compilation and GC activity sampled at scenario boundaries, ported from racecar's {@code JvmActivity}. */
final class JvmActivity {

    private static final CompilationMXBean COMPILATION = ManagementFactory.getCompilationMXBean();

    private JvmActivity() {
    }

    static boolean compilationTimeSupported() {
        return COMPILATION != null && COMPILATION.isCompilationTimeMonitoringSupported();
    }

    /** Total JIT compilation time since JVM start in milliseconds (wall time across compiler threads), or -1. */
    static long compilationMillis() {
        return compilationTimeSupported() ? COMPILATION.getTotalCompilationTime() : -1;
    }

    /** Cumulative GC count and time across all collectors. */
    record Gc(long count, long millis) {
        Gc minus(Gc before) {
            return new Gc(count - before.count, millis - before.millis);
        }
    }

    static Gc gc() {
        long count = 0;
        long millis = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            count += Math.max(0, bean.getCollectionCount());
            millis += Math.max(0, bean.getCollectionTime());
        }
        return new Gc(count, millis);
    }
}
