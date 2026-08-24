package software.amazon.awssdk.benchmark.e2e;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Whole-process CPU time, preferred source {@code /proc/self/stat} (Linux).
 *
 * <p>Reading process-level utime+stime covers every thread in the JVM (event loops, GC, JIT,
 * virtual-thread carriers), which per-thread {@code getCurrentThreadUserTime} would miss. Fields
 * 14 (utime) and 15 (stime) of {@code /proc/self/stat} are in clock ticks; tick frequency is
 * assumed to be 100 Hz (the value on every mainstream Linux kernel build) and can be overridden
 * with {@code -DclkTck=N} if the kernel was built with a different HZ.
 *
 * <p>On platforms without procfs (e.g. macOS) it falls back to
 * {@code com.sun.management.OperatingSystemMXBean#getProcessCpuTime()}, which reports combined
 * user+system time; the user/system split is then unavailable.
 */
public final class ProcessCpu {

    private static final Path PROC_SELF_STAT = Path.of("/proc/self/stat");
    private static final boolean HAS_PROCFS = Files.isReadable(PROC_SELF_STAT);
    private static final long TICKS_PER_SEC = Long.getLong("clkTck", 100);

    /** Snapshot of process CPU time in nanoseconds. */
    public record Snapshot(long userNanos, long systemNanos, boolean splitAvailable) {
        public long totalNanos() {
            return userNanos + systemNanos;
        }

        public Snapshot minus(Snapshot other) {
            return new Snapshot(userNanos - other.userNanos, systemNanos - other.systemNanos,
                                splitAvailable && other.splitAvailable);
        }
    }

    private ProcessCpu() {
    }

    public static String source() {
        return HAS_PROCFS ? "/proc/self/stat (ticks=" + TICKS_PER_SEC + "/s)"
                          : "OperatingSystemMXBean.getProcessCpuTime (no user/sys split)";
    }

    public static Snapshot snapshot() {
        if (HAS_PROCFS) {
            try {
                return readProcSelfStat();
            } catch (Exception e) {
                // fall through to MX bean
            }
        }
        return readMxBean();
    }

    private static Snapshot readProcSelfStat() throws Exception {
        String stat = new String(Files.readAllBytes(PROC_SELF_STAT), StandardCharsets.US_ASCII);
        // The comm field (2) is in parentheses and may contain spaces; parse after the last ')'.
        int close = stat.lastIndexOf(')');
        String[] rest = stat.substring(close + 2).split(" ");
        // rest[0] is field 3 (state); utime is field 14, stime field 15 -> rest[11], rest[12].
        long utimeTicks = Long.parseLong(rest[11]);
        long stimeTicks = Long.parseLong(rest[12]);
        long nanosPerTick = 1_000_000_000L / TICKS_PER_SEC;
        return new Snapshot(utimeTicks * nanosPerTick, stimeTicks * nanosPerTick, true);
    }

    private static Snapshot readMxBean() {
        java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        if (bean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            long total = sunBean.getProcessCpuTime();
            if (total >= 0) {
                return new Snapshot(total, 0, false);
            }
        }
        return new Snapshot(0, 0, false);
    }
}
