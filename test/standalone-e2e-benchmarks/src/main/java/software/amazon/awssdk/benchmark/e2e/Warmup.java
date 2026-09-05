package software.amazon.awssdk.benchmark.e2e;

import java.util.Locale;

/**
 * Runs unmeasured operations until the JVM has stopped changing underneath the benchmark.
 *
 * <p>A fixed warmup count is a guess, and on this workload it was the wrong guess: with a 20k warmup,
 * per-operation process CPU still fell from 114 to 49 µs/op as the measured window grew from 40k to
 * 300k operations, because the JIT was still compiling inside the window and its fixed cost was being
 * amortized over however many operations happened to follow. Fixing the accounting
 * ({@link AppCpuMeter}) removes compiler CPU from the total, but it does not make the measurement
 * steady-state: code still being recompiled mid-window is code whose speed is changing.
 *
 * <p>{@link Mode#QUIESCE} therefore keeps warming in chunks until total compilation time stops
 * growing for a sustained stretch of wall clock, then hands over. The criterion is compilation
 * *inactivity* rather than a target op count, so it adapts to the machine and to whichever client is
 * under test instead of assuming they all settle at the same point.
 */
final class Warmup {

    enum Mode {
        /** Exactly the requested number of operations. Reproduces older collections. */
        FIXED,
        /** The requested operations, then more until compilation goes quiet. */
        QUIESCE
    }

    /** Growth in total compilation time, in ms, small enough to count as "not compiling". */
    private static final long QUIET_COMPILE_MS = 1;
    /**
     * Warmup must run at least this long before quiescence can be declared at all.
     *
     * <p>Without it, a client whose compilation happens to pause early declares itself settled in the
     * first couple of seconds and then compiles heavily inside the measured window — the async client
     * was observed calling it quiet after 40,000 operations and then spending 1.8 s compiling during
     * the measurement. A floor on elapsed warmup makes an early lull insufficient on its own.
     */
    private static final long MIN_WARMUP_WALL_NANOS = 5_000_000_000L;
    /**
     * How long compilation must stay quiet before the JVM is considered settled.
     *
     * <p>Three seconds rather than two, because compilation tails off in bursts rather than stopping:
     * with a 2 s gate, async runs reported settled and then still spent 300–550 ms compiling inside
     * the measured window. Even three seconds is a heuristic, which is why the runner independently
     * checks how much compilation actually happened in the window and flags the run if it was
     * material — a warmup gate can only ever be evidence, not proof.
     */
    private static final long QUIET_WALL_NANOS = 3_000_000_000L;

    /** What the warmup actually did, for the run header and the results row. */
    record Result(long ops, long wallNanos, long compileMillisDuring, boolean quiesced,
                  String stopReason) {

        String summary() {
            return String.format(Locale.US, "ops=%,d wall=%.1fs jit=%dms settled=%s (%s)",
                                 ops, wallNanos / 1e9, compileMillisDuring, quiesced, stopReason);
        }
    }

    private Warmup() {
    }

    /**
     * @param minOps     operations to run before quiescence is even considered
     * @param maxSeconds hard ceiling on total warmup wall time, so a machine that never goes quiet
     *                   cannot hang a collection
     */
    static Result run(Workloads.Workload workload, BenchmarkRunner.Scenario scenario, Driver.Mode mode,
                      Mode warmupMode, int minOps, int concurrency, int maxSeconds) throws Exception {
        long start = System.nanoTime();
        long compileAtStart = JvmActivity.compilationMillis();

        if (minOps > 0) {
            Driver.run(workload, scenario, mode, minOps, concurrency, null);
        }
        long ops = minOps;

        if (warmupMode == Mode.FIXED) {
            return new Result(ops, System.nanoTime() - start,
                              compileDelta(compileAtStart), false, "fixed warmup");
        }
        if (!JvmActivity.compilationTimeSupported()) {
            return new Result(ops, System.nanoTime() - start, -1, false,
                              "compilation time unavailable, fell back to fixed");
        }

        // Chunks are a quarter of the requested warmup, floored so a tiny --warmup does not turn
        // into thousands of round trips through the quiescence check.
        int chunk = Math.max(500, minOps / 4);
        long deadline = start + maxSeconds * 1_000_000_000L;
        long lastCompile = JvmActivity.compilationMillis();
        long quietSince = System.nanoTime();

        while (true) {
            long now = System.nanoTime();
            if (now - start >= MIN_WARMUP_WALL_NANOS && now - quietSince >= QUIET_WALL_NANOS) {
                return new Result(ops, now - start, compileDelta(compileAtStart), true,
                                  "compilation quiet for " + QUIET_WALL_NANOS / 1_000_000 + "ms");
            }
            if (now >= deadline) {
                return new Result(ops, now - start, compileDelta(compileAtStart), false,
                                  "hit --warmup-max-seconds " + maxSeconds + " while still compiling");
            }

            Driver.run(workload, scenario, mode, chunk, concurrency, null);
            ops += chunk;

            long compile = JvmActivity.compilationMillis();
            if (compile - lastCompile > QUIET_COMPILE_MS) {
                // Still compiling: restart the quiet clock.
                lastCompile = compile;
                quietSince = System.nanoTime();
            }
        }
    }

    private static long compileDelta(long compileAtStart) {
        long now = JvmActivity.compilationMillis();
        return now < 0 || compileAtStart < 0 ? -1 : now - compileAtStart;
    }
}
