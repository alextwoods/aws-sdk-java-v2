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

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CPU consumed and bytes allocated by <em>application</em> (Java) threads, ported from racecar's {@code AppCpuMeter}.
 *
 * <p>{@code ThreadMXBean} only knows Java threads, so compiler, VM and GC threads are excluded by construction. A
 * thread's counters vanish when it dies, so every harness-created thread folds its totals into "retired" counters on
 * the way out ({@link #accounted(Runnable)}). SDK-created threads that are born and die inside a window remain a gap,
 * which is why the runner prints {@code unattributed_ms} (process minus application CPU).
 *
 * <p>V3 addition: allocated bytes are summed the same way through {@code com.sun.management.ThreadMXBean
 * .getThreadAllocatedBytes}. Racecar measured allocation only with async-profiler; this makes {@code alloc_bytes_per_op}
 * part of every RESULT line. It is per Java thread, so allocation on transport event-loop threads (Netty, CRT
 * callbacks) is included whenever those are Java threads, and native CRT allocations are not.
 */
final class AppCpuMeter {

    private static final com.sun.management.ThreadMXBean THREADS = initThreads();
    private static final AtomicLong RETIRED_CPU_NANOS = new AtomicLong();
    private static final AtomicLong RETIRED_ALLOC_BYTES = new AtomicLong();
    private static final boolean CPU_SUPPORTED = initCpu();
    private static final boolean ALLOC_SUPPORTED = initAlloc();

    private AppCpuMeter() {
    }

    private static com.sun.management.ThreadMXBean initThreads() {
        return ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean t ? t : null;
    }

    private static boolean initCpu() {
        if (THREADS == null || !THREADS.isThreadCpuTimeSupported()) {
            return false;
        }
        if (!THREADS.isThreadCpuTimeEnabled()) {
            THREADS.setThreadCpuTimeEnabled(true);
        }
        return THREADS.isThreadCpuTimeEnabled();
    }

    private static boolean initAlloc() {
        if (THREADS == null || !THREADS.isThreadAllocatedMemorySupported()) {
            return false;
        }
        if (!THREADS.isThreadAllocatedMemoryEnabled()) {
            THREADS.setThreadAllocatedMemoryEnabled(true);
        }
        return THREADS.isThreadAllocatedMemoryEnabled();
    }

    static boolean supported() {
        return CPU_SUPPORTED;
    }

    static boolean allocationSupported() {
        return ALLOC_SUPPORTED;
    }

    /** Fold the calling thread's totals into the retired counters; call exactly once, last, from the thread itself. */
    static void retireCurrentThread() {
        if (CPU_SUPPORTED) {
            long cpu = THREADS.getCurrentThreadCpuTime();
            if (cpu > 0) {
                RETIRED_CPU_NANOS.addAndGet(cpu);
            }
        }
        if (ALLOC_SUPPORTED) {
            long bytes = THREADS.getCurrentThreadAllocatedBytes();
            if (bytes > 0) {
                RETIRED_ALLOC_BYTES.addAndGet(bytes);
            }
        }
    }

    /** Application CPU in nanoseconds (live Java threads plus retired), or -1 when unavailable. */
    static long snapshotNanos() {
        if (!CPU_SUPPORTED) {
            return -1;
        }
        long total = RETIRED_CPU_NANOS.get();
        long[] ids = THREADS.getAllThreadIds();
        long[] cpus = THREADS.getThreadCpuTime(ids);
        for (long cpu : cpus) {
            if (cpu > 0) {
                total += cpu;
            }
        }
        return total;
    }

    /** Bytes allocated by Java threads (live plus retired), or -1 when unavailable. */
    static long snapshotAllocatedBytes() {
        if (!ALLOC_SUPPORTED) {
            return -1;
        }
        long total = RETIRED_ALLOC_BYTES.get();
        long[] ids = THREADS.getAllThreadIds();
        long[] bytes = THREADS.getThreadAllocatedBytes(ids);
        for (long b : bytes) {
            if (b > 0) {
                total += b;
            }
        }
        return total;
    }

    /** Wrap a worker body so its CPU and allocation are always retired, including on failure. */
    static Runnable accounted(Runnable body) {
        return () -> {
            try {
                body.run();
            } finally {
                retireCurrentThread();
            }
        };
    }
}
