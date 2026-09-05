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
