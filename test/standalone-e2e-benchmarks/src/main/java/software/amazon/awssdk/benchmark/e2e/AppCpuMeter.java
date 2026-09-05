package software.amazon.awssdk.benchmark.e2e;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CPU consumed by <em>application</em> threads, as opposed to the whole process.
 *
 * <p>Why this is needed: {@code CpuTimeSource} reads process CPU, which includes the C1/C2 compiler
 * threads, the VM thread and the GC threads. That work is roughly fixed per JVM rather than
 * proportional to the operation count, so per-operation process CPU does not converge — on an
 * unchanged client it measured 114, 77 and 49 µs/op at 40k, 100k and 300k operations. Two real
 * comparisons were distorted badly enough by this to shrink to nothing (and one to reverse sign)
 * when re-measured at a longer window, so process CPU cannot support a per-operation claim.
 *
 * <p>{@link ThreadMXBean} only knows about Java threads, which is exactly the filter wanted here:
 * compiler, VM and GC threads are native and simply absent, so summing per-thread CPU yields
 * application CPU without needing to match thread names.
 *
 * <p>The catch is that a thread's CPU time disappears when the thread dies, so a naive
 * before/after sum loses every thread that exited inside the window — including the benchmark's own
 * workers, which are joined before the closing snapshot is taken. Each thread the harness creates
 * therefore reports its own total on the way out, into a cumulative "retired" counter that is part
 * of the snapshot. That makes the arithmetic correct across thread churn: a worker alive at the
 * opening snapshot with 100 ms that exits at 150 ms contributes 150 to retired while its 100 was
 * counted in the opening sum, so the delta attributes exactly the 50 ms it spent in the window.
 *
 * <p>Remaining gap, deliberately visible rather than hidden: a thread created by the SDK that also
 * dies inside the window is not accounted for, since it cannot report itself. V2's async-response
 * executor allows core threads to time out after 10 s, so this is possible on long windows. The
 * runner prints application CPU alongside process CPU and the compilation and GC totals, so the
 * unattributed remainder is always on show instead of being assumed to be zero.
 */
final class AppCpuMeter {

    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final AtomicLong RETIRED_NANOS = new AtomicLong();
    private static final boolean SUPPORTED = initSupport();

    private AppCpuMeter() {
    }

    private static boolean initSupport() {
        if (THREADS == null || !THREADS.isThreadCpuTimeSupported()) {
            return false;
        }
        if (!THREADS.isThreadCpuTimeEnabled()) {
            try {
                THREADS.setThreadCpuTimeEnabled(true);
            } catch (UnsupportedOperationException | SecurityException e) {
                return false;
            }
        }
        return THREADS.isThreadCpuTimeEnabled();
    }

    static boolean supported() {
        return SUPPORTED;
    }

    /**
     * Fold the calling thread's total CPU into the retired counter. Must be called exactly once, by
     * the thread itself, as the last thing it does — calling it twice double-counts, and not calling
     * it loses the thread's contribution entirely.
     */
    static void retireCurrentThread() {
        if (!SUPPORTED) {
            return;
        }
        long cpu = THREADS.getCurrentThreadCpuTime();
        if (cpu > 0) {
            RETIRED_NANOS.addAndGet(cpu);
        }
    }

    /**
     * Application CPU in nanoseconds: every live Java thread plus everything already retired.
     * Returns -1 when per-thread CPU is unavailable.
     */
    static long snapshotNanos() {
        if (!SUPPORTED) {
            return -1;
        }
        long total = RETIRED_NANOS.get();
        long[] ids = THREADS.getAllThreadIds();
        for (long id : ids) {
            long cpu = THREADS.getThreadCpuTime(id);
            // -1 means the thread died between getAllThreadIds() and here. If it is one of ours it
            // has already retired itself; if not, it falls into the documented gap above.
            if (cpu > 0) {
                total += cpu;
            }
        }
        return total;
    }

    /** Wrap a worker body so its CPU is always retired, including on failure. */
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
