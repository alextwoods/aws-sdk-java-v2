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

package software.amazon.awssdk.benchmark.e2e;

import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/**
 * JIT compilation and GC activity, sampled at scenario boundaries.
 *
 * <p>Exists to make the JVM's own work visible instead of silently folded into the measurement.
 * Whole-process CPU includes the compiler threads, and that cost is fixed per JVM rather than per
 * operation, so dividing it by the operation count produces a number that shrinks as the window
 * grows instead of converging — measured here as 114 → 77 → 49 µs/op across 40k/100k/300k operations
 * on an unchanged client. Reporting compilation time inside the measured window turns that from an
 * invisible bias into a number a reader can check, and it is also the signal
 * {@link Warmup} uses to decide when the JVM has settled.
 */
final class JvmActivity {

    private static final CompilationMXBean COMPILATION = ManagementFactory.getCompilationMXBean();

    private JvmActivity() {
    }

    /** Whether total compilation time is available; some JVMs do not report it. */
    static boolean compilationTimeSupported() {
        return COMPILATION != null && COMPILATION.isCompilationTimeMonitoringSupported();
    }

    /**
     * Total time spent JIT-compiling since JVM start, in milliseconds, or -1 if unsupported.
     *
     * <p>This is wall time across all compiler threads, not CPU time, so it is an indicator rather
     * than a term that can be subtracted from a CPU total.
     */
    static long compilationMillis() {
        return compilationTimeSupported() ? COMPILATION.getTotalCompilationTime() : -1;
    }

    /** Cumulative GC count and time across all collectors. */
    record Gc(long count, long millis) {

        Gc minus(Gc before) {
            return new Gc(count - before.count, millis - before.millis);
        }
    }

    /**
     * Heap bytes allocated by all threads since JVM start (including threads that have exited), or -1 if the JVM
     * does not report it.
     *
     * <p>Added on this branch (not in racecar's harness) so a paired run reports bytes allocated per operation,
     * which Requirement 37.9 compares against the racecar build. The figure covers every Java thread in the client
     * JVM, so it includes the HTTP client's and the SDK's own threads, as JMH's {@code gc.alloc.rate.norm} does
     * for the threads it measures. Only the client JVM is counted: the mock server runs in its own process.
     */
    static long allocatedBytes() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean instanceof com.sun.management.ThreadMXBean sun && sun.isThreadAllocatedMemorySupported()
            && sun.isThreadAllocatedMemoryEnabled()) {
            return sun.getTotalThreadAllocatedBytes();
        }
        return -1;
    }

    static Gc gc() {
        long count = 0;
        long millis = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = bean.getCollectionCount();
            long t = bean.getCollectionTime();
            if (c > 0) {
                count += c;
            }
            if (t > 0) {
                millis += t;
            }
        }
        return new Gc(count, millis);
    }
}
