package software.amazon.awssdk.benchmark.e2e;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * Factory for {@link CpuTimeSource}. Probes each candidate once and binds exactly one source for
 * the process lifetime — there is no per-snapshot fallback, so all snapshots in a run come from
 * the same counters.
 *
 * <p>Sources:
 * <ul>
 *   <li>{@code oshi} — OSHI {@link OSProcess} user/kernel time. User/system split on both Linux
 *       (procfs with the real {@code USER_HZ}) and macOS ({@code proc_pidinfo}). Millisecond
 *       resolution. Default.</li>
 *   <li>{@code procfs} — direct {@code /proc/self/stat} parse (Linux only). User/system split in
 *       clock ticks, assumed 100/s unless {@code -DclkTck=N}. Useful to cross-check oshi.</li>
 *   <li>{@code mxbean} — {@code com.sun.management.OperatingSystemMXBean#getProcessCpuTime()}.
 *       Total only, no split. Last resort.</li>
 * </ul>
 */
final class CpuTimeSources {

    private CpuTimeSources() {
    }

    /**
     * Bind a CPU time source. {@code preference} is {@code auto} (probe oshi, procfs, mxbean in
     * order) or a specific source name, which fails fast if that source is unavailable.
     */
    static CpuTimeSource bind(String preference) {
        switch (preference) {
            case "auto":
                CpuTimeSource oshi = OshiSource.tryCreate();
                if (oshi != null) {
                    return oshi;
                }
                CpuTimeSource procfs = ProcfsSource.tryCreate();
                if (procfs != null) {
                    return procfs;
                }
                return require(MxBeanSource.tryCreate(), "mxbean");
            case "oshi":
                return require(OshiSource.tryCreate(), "oshi");
            case "procfs":
                return require(ProcfsSource.tryCreate(), "procfs");
            case "mxbean":
                return require(MxBeanSource.tryCreate(), "mxbean");
            default:
                throw new IllegalArgumentException(
                    "unknown cpu source: " + preference + " (auto|oshi|procfs|mxbean)");
        }
    }

    private static CpuTimeSource require(CpuTimeSource source, String name) {
        if (source == null) {
            throw new IllegalStateException("cpu source '" + name + "' is not available on this platform");
        }
        return source;
    }

    // ==================== oshi ====================

    private static final class OshiSource implements CpuTimeSource {
        private final OperatingSystem os;
        private final int pid;

        private OshiSource(OperatingSystem os, int pid) {
            this.os = os;
            this.pid = pid;
        }

        static OshiSource tryCreate() {
            try {
                OperatingSystem os = new SystemInfo().getOperatingSystem();
                OshiSource source = new OshiSource(os, os.getProcessId());
                // Probe once so a broken native path is rejected at bind time, not mid-run.
                source.snapshot();
                return source;
            } catch (Throwable t) {
                return null;
            }
        }

        @Override
        public String name() {
            return "oshi (" + os.getFamily() + ")";
        }

        @Override
        public boolean hasUserSystemSplit() {
            return true;
        }

        @Override
        public Snapshot snapshot() {
            OSProcess proc = os.getProcess(pid);
            if (proc == null || proc.getUserTime() < 0 || proc.getKernelTime() < 0) {
                throw new IllegalStateException("oshi could not read process CPU time");
            }
            return Snapshot.ofSplit(proc.getUserTime() * 1_000_000L, proc.getKernelTime() * 1_000_000L);
        }
    }

    // ==================== procfs ====================

    private static final class ProcfsSource implements CpuTimeSource {
        private static final Path PROC_SELF_STAT = Path.of("/proc/self/stat");
        private final long ticksPerSec = Long.getLong("clkTck", 100);

        static ProcfsSource tryCreate() {
            try {
                ProcfsSource source = new ProcfsSource();
                source.snapshot();
                return source;
            } catch (Throwable t) {
                return null;
            }
        }

        @Override
        public String name() {
            return "/proc/self/stat (ticks=" + ticksPerSec + "/s)";
        }

        @Override
        public boolean hasUserSystemSplit() {
            return true;
        }

        @Override
        public Snapshot snapshot() {
            try {
                String stat = new String(Files.readAllBytes(PROC_SELF_STAT), StandardCharsets.US_ASCII);
                // The comm field (2) is in parentheses and may contain spaces; parse after the last ')'.
                int close = stat.lastIndexOf(')');
                String[] rest = stat.substring(close + 2).split(" ");
                // rest[0] is field 3 (state); utime is field 14, stime field 15 -> rest[11], rest[12].
                long utimeTicks = Long.parseLong(rest[11]);
                long stimeTicks = Long.parseLong(rest[12]);
                long nanosPerTick = 1_000_000_000L / ticksPerSec;
                return Snapshot.ofSplit(utimeTicks * nanosPerTick, stimeTicks * nanosPerTick);
            } catch (Exception e) {
                throw new IllegalStateException("failed to read /proc/self/stat", e);
            }
        }
    }

    // ==================== mxbean ====================

    private static final class MxBeanSource implements CpuTimeSource {
        private final com.sun.management.OperatingSystemMXBean bean;

        private MxBeanSource(com.sun.management.OperatingSystemMXBean bean) {
            this.bean = bean;
        }

        static MxBeanSource tryCreate() {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean sunBean
                && sunBean.getProcessCpuTime() >= 0) {
                return new MxBeanSource(sunBean);
            }
            return null;
        }

        @Override
        public String name() {
            return "OperatingSystemMXBean.getProcessCpuTime";
        }

        @Override
        public boolean hasUserSystemSplit() {
            return false;
        }

        @Override
        public Snapshot snapshot() {
            return Snapshot.ofTotal(bean.getProcessCpuTime());
        }
    }
}
