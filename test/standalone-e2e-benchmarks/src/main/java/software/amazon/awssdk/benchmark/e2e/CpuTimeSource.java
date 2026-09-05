package software.amazon.awssdk.benchmark.e2e;

/**
 * A source of whole-process CPU time. Exactly one implementation is bound at startup via
 * {@link CpuTimeSources#bind(String)} and used for the lifetime of the run.
 *
 * <p>Whether user/system time can be reported separately is a fixed property of the source
 * ({@link #hasUserSystemSplit()}), not of individual snapshots: consumers decide once, up front,
 * what they will be able to report. Total CPU time is always valid. All sources read the same
 * underlying OS counters (utime/stime on Linux, {@code proc_pidinfo} times on macOS), so user and
 * system time mean the same thing regardless of which source is bound.
 */
public interface CpuTimeSource {

    /** Human-readable source name for the run header, e.g. {@code "oshi (macOS)"}. */
    String name();

    /** Whether {@link Snapshot#userNanos()} / {@link Snapshot#systemNanos()} are populated. */
    boolean hasUserSystemSplit();

    Snapshot snapshot();

    /**
     * Process CPU time in nanoseconds. {@code totalNanos} is always valid. {@code userNanos} and
     * {@code systemNanos} are {@code >= 0} only for sources with a user/system split (in which
     * case {@code totalNanos == userNanos + systemNanos}); otherwise they are {@code -1} so a
     * consumer that forgets to check cannot mistake a total for a user time.
     */
    record Snapshot(long totalNanos, long userNanos, long systemNanos) {

        public static Snapshot ofSplit(long userNanos, long systemNanos) {
            return new Snapshot(userNanos + systemNanos, userNanos, systemNanos);
        }

        public static Snapshot ofTotal(long totalNanos) {
            return new Snapshot(totalNanos, -1, -1);
        }

        public boolean hasSplit() {
            return userNanos >= 0 && systemNanos >= 0;
        }

        /** Difference between two snapshots from the same source; preserves split absence. */
        public Snapshot minus(Snapshot other) {
            if (hasSplit() && other.hasSplit()) {
                return ofSplit(userNanos - other.userNanos, systemNanos - other.systemNanos);
            }
            return ofTotal(totalNanos - other.totalNanos);
        }
    }
}
