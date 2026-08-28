package software.amazon.awssdk.benchmark.e2e;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Issues a scenario's operations and records each one's latency.
 *
 * <p>Two shapes, because "concurrency" means different things to the two programming models and
 * collapsing them would build a bias into the harness:
 *
 * <ul>
 *   <li>{@link Mode#BLOCKING} — N caller threads, each in its own closed loop. This is how a
 *       blocking client is used; N threads is the only way it can have N operations outstanding.</li>
 *   <li>{@link Mode#INFLIGHT} — one submitting thread that keeps N operations outstanding, driven by
 *       completions. This is how an async client is used, and it is the shape async exists for. The
 *       previous {@code join()}-per-call loop held exactly one operation in flight, so it measured
 *       an async client doing a blocking client's job plus thread-hop overhead.</li>
 * </ul>
 *
 * <p>Latency is recorded per operation into preallocated arrays, so the measured loop allocates
 * nothing. Two {@code nanoTime} reads per operation cost roughly 50 ns against operations that take
 * tens of microseconds.
 *
 * <p>Progress reporting reads worker-local counters from a separate daemon thread and never touches
 * the measured path — a per-operation clock check and print previously cost more than the operation
 * itself on the smallest scenario.
 */
final class Driver {

    enum Mode {
        BLOCKING,
        INFLIGHT
    }

    /** Latency samples in nanoseconds, plus the operations that were actually completed. */
    record Samples(long[] nanos, int count) {

        LatencyStats stats() {
            return LatencyStats.of(nanos, count);
        }
    }

    private Driver() {
    }

    /**
     * Run {@code iterations} operations and return their latencies.
     *
     * @param progress counter the caller's progress thread reads; may be null. Incremented with a
     *                 plain write per worker, never shared, so it adds no synchronization.
     */
    static Samples run(Workloads.Workload workload, BenchmarkRunner.Scenario scenario, Mode mode,
                       int iterations, int concurrency, Progress progress) throws Exception {
        if (iterations == 0) {
            return new Samples(new long[0], 0);
        }
        return mode == Mode.INFLIGHT
               ? runInflight(workload, scenario, iterations, concurrency, progress)
               : runBlocking(workload, scenario, iterations, concurrency, progress);
    }

    // ---- Blocking: N threads, each with its own slice of the iteration budget ----

    private static Samples runBlocking(Workloads.Workload workload, BenchmarkRunner.Scenario scenario,
                                       int iterations, int concurrency, Progress progress)
            throws Exception {
        int workers = Math.min(concurrency, iterations);
        // Each worker owns its own sample array and count, so nothing is shared and there is no
        // false sharing on the hot path.
        long[][] samples = new long[workers][];
        int[] budgets = new int[workers];
        for (int i = 0; i < workers; i++) {
            // Spread the remainder over the first workers so the total is exactly `iterations`.
            budgets[i] = iterations / workers + (i < iterations % workers ? 1 : 0);
            samples[i] = new long[budgets[i]];
        }

        Thread[] threads = new Thread[workers];
        AtomicReference<Throwable> failure = new AtomicReference<>();
        // Release all workers at once: staggered starts would let early workers run at a lower
        // concurrency than the run claims to measure.
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(workers);

        for (int w = 0; w < workers; w++) {
            int id = w;
            threads[w] = new Thread(() -> {
                long[] mine = samples[id];
                int budget = budgets[id];
                try {
                    ready.countDown();
                    startGate.await();
                    for (int i = 0; i < budget; i++) {
                        long t0 = System.nanoTime();
                        scenario.run(workload);
                        mine[i] = System.nanoTime() - t0;
                        if (progress != null) {
                            progress.set(id, i + 1);
                        }
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            }, "bench-worker-" + w);
            threads[w].start();
        }

        ready.await();
        startGate.countDown();
        for (Thread t : threads) {
            t.join();
        }
        rethrow(failure.get());

        return merge(samples, budgets, iterations);
    }

    // ---- In-flight: one submitter, N outstanding, completion-driven ----

    private static Samples runInflight(Workloads.Workload workload, BenchmarkRunner.Scenario scenario,
                                       int iterations, int concurrency, Progress progress)
            throws Exception {
        if (!workload.supportsAsync()) {
            throw new IllegalArgumentException(
                "in-flight mode needs an async client; use --async-mode join for a blocking client");
        }

        long[] samples = new long[iterations];
        AtomicInteger cursor = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        // Bounds outstanding work at exactly `concurrency`. Submission stays on this thread rather
        // than chaining the next request from the completion callback: chaining would move
        // marshalling and signing onto the transport's event-loop threads, which both changes where
        // that CPU is attributed and can serialize all of it onto one loop.
        //
        // Every acquire is matched by exactly one release, on the completion callback and after the
        // sample is recorded. So reacquiring all `concurrency` permits at the end is a proof that
        // every submitted operation has completed and been counted — which matters because the
        // caller stops the clock right after, and an operation still in flight would be work whose
        // CPU landed in the window but whose latency did not.
        Semaphore permits = new Semaphore(concurrency);

        for (int i = 0; i < iterations && failure.get() == null; i++) {
            permits.acquire();
            long t0 = System.nanoTime();
            CompletableFuture<?> f;
            try {
                f = scenario.runAsync(workload);
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
                permits.release();
                break;
            }
            f.whenComplete((r, t) -> {
                if (t != null) {
                    failure.compareAndSet(null, t);
                }
                samples[cursor.getAndIncrement()] = System.nanoTime() - t0;
                if (progress != null) {
                    progress.set(0, cursor.get());
                }
                permits.release();
            });
        }

        permits.acquire(concurrency);
        rethrow(failure.get());

        return new Samples(samples, cursor.get());
    }

    private static Samples merge(long[][] samples, int[] budgets, int total) {
        long[] all = new long[total];
        int at = 0;
        for (int i = 0; i < samples.length; i++) {
            System.arraycopy(samples[i], 0, all, at, budgets[i]);
            at += budgets[i];
        }
        return new Samples(all, at);
    }

    private static void rethrow(Throwable t) throws Exception {
        if (t == null) {
            return;
        }
        if (t instanceof Exception e) {
            throw e;
        }
        throw new RuntimeException(t);
    }

    /**
     * Worker-local completion counters for progress display. Written with plain array stores by the
     * owning worker and read racily by the progress thread: a stale or torn read only misprints a
     * progress line, and keeping it unsynchronized is what keeps it off the measured path.
     */
    static final class Progress {
        // 16 longs of stride so two workers never share a cache line.
        private static final int STRIDE = 16;
        private final long[] counters;
        private final int workers;

        Progress(int workers) {
            this.workers = workers;
            this.counters = new long[workers * STRIDE];
        }

        void set(int worker, long completed) {
            counters[worker * STRIDE] = completed;
        }

        long total() {
            long sum = 0;
            for (int i = 0; i < workers; i++) {
                sum += counters[i * STRIDE];
            }
            return sum;
        }
    }

    /** Latency distribution over one scenario's measured operations. */
    record LatencyStats(long count, double meanUs, double p50Us, double p90Us, double p99Us,
                        double p999Us, double maxUs) {

        static LatencyStats of(long[] nanos, int count) {
            if (count <= 0) {
                return new LatencyStats(0, 0, 0, 0, 0, 0, 0);
            }
            long[] sorted = Arrays.copyOf(nanos, count);
            Arrays.sort(sorted);
            long sum = 0;
            for (long v : sorted) {
                sum += v;
            }
            return new LatencyStats(count,
                                    sum / 1_000.0 / count,
                                    percentile(sorted, 50.0),
                                    percentile(sorted, 90.0),
                                    percentile(sorted, 99.0),
                                    percentile(sorted, 99.9),
                                    sorted[sorted.length - 1] / 1_000.0);
        }

        private static double percentile(long[] sorted, double pct) {
            // Nearest-rank on the sorted samples; exact for the sample set, no interpolation or
            // bucketing error, which is affordable because every sample is kept.
            int idx = (int) Math.ceil(pct / 100.0 * sorted.length) - 1;
            return sorted[Math.max(0, Math.min(sorted.length - 1, idx))] / 1_000.0;
        }
    }
}
