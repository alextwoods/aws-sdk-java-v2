package software.amazon.awssdk.benchmark.e2e;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * Whole-process CPU time with a user/system split on both Linux and macOS.
 *
 * <p>Reading process-level CPU time covers every thread in the JVM (event loops, GC, JIT,
 * virtual-thread carriers), which per-thread {@code getCurrentThreadUserTime} would miss.
 *
 * <p>Sources, in preference order:
 * <ol>
 *   <li><b>OSHI</b> ({@link OSProcess#getUserTime()} / {@link OSProcess#getKernelTime()}): on
 *       Linux this parses {@code /proc/self/stat} (utime/stime) with the real {@code USER_HZ};
 *       on macOS it uses {@code proc_pidinfo}. Millisecond resolution.</li>
 *   <li>Direct {@code /proc/self/stat} parse (Linux): fields 14 (utime) and 15 (stime) in clock
 *       ticks, assumed 100 Hz unless overridden with {@code -DclkTck=N}.</li>
 *   <li>{@code com.sun.management.OperatingSystemMXBean#getProcessCpuTime()}: combined
 *       user+system only, no split.</li>
 * </ol>
 */
public final class ProcessCpu {

    private static final Path PROC_SELF_STAT = Path.of("/proc/self/stat");
    private static final boolean HAS_PROCFS = Files.isReadable(PROC_SELF_STAT);
    private static final long TICKS_PER_SEC = Long.getLong("clkTck", 100);

    private static final OperatingSystem OSHI_OS = initOshi();
    private static final int PID = OSHI_OS == null ? -1 : OSHI_OS.getProcessId();

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
        if (OSHI_OS != null) {
            return "oshi OSProcess user/kernel time (" + OSHI_OS.getFamily() + ")";
        }
        if (HAS_PROCFS) {
            return "/proc/self/stat (ticks=" + TICKS_PER_SEC + "/s)";
        }
        return "OperatingSystemMXBean.getProcessCpuTime (no user/sys split)";
    }

    public static Snapshot snapshot() {
        if (OSHI_OS != null) {
            try {
                OSProcess proc = OSHI_OS.getProcess(PID);
                if (proc != null) {
                    return new Snapshot(proc.getUserTime() * 1_000_000L,
                                        proc.getKernelTime() * 1_000_000L,
                                        true);
                }
            } catch (Exception e) {
                // fall through
            }
        }
        if (HAS_PROCFS) {
            try {
                return readProcSelfStat();
            } catch (Exception e) {
                // fall through
            }
        }
        return readMxBean();
    }

    private static OperatingSystem initOshi() {
        try {
            OperatingSystem os = new SystemInfo().getOperatingSystem();
            OSProcess self = os.getProcess(os.getProcessId());
            // Probe once so a broken native path falls back at startup, not mid-run.
            if (self != null && self.getUserTime() >= 0 && self.getKernelTime() >= 0) {
                return os;
            }
        } catch (Throwable t) {
            // JNA/native failure: fall back to procfs or the MX bean.
        }
        return null;
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
