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

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Issues a scenario's operations and records each one's latency; ported from racecar's {@code Driver}.
 *
 * <ul>
 *   <li>{@link Mode#BLOCKING}: N caller threads, each in its own closed loop (sync arms, and async arms in
 *       {@code join} mode).</li>
 *   <li>{@link Mode#INFLIGHT}: one submitting thread keeps N operations outstanding, completion-driven (async
 *       arms).</li>
 * </ul>
 *
 * <p>Latencies go into preallocated arrays; nothing prints from the measured loop, and progress is read racily from
 * worker-local counters by a separate thread.
 */
final class Driver {

    enum Mode {
        BLOCKING,
        INFLIGHT
    }

    record Samples(long[] nanos, int count) {
        LatencyStats stats() {
            return LatencyStats.of(nanos, count);
        }
    }

    private Driver() {
    }

    static Samples run(V3Workload workload, Scenario scenario, Mode mode, int iterations, int concurrency,
                       Progress progress) throws Exception {
        if (iterations == 0) {
            return new Samples(new long[0], 0);
        }
        return mode == Mode.INFLIGHT
               ? runInflight(workload, scenario, iterations, concurrency, progress)
               : runBlocking(workload, scenario, iterations, concurrency, progress);
    }

    private static Samples runBlocking(V3Workload workload, Scenario scenario, int iterations, int concurrency,
                                       Progress progress) throws Exception {
        int workers = Math.min(concurrency, iterations);
        long[][] samples = new long[workers][];
        int[] budgets = new int[workers];
        for (int i = 0; i < workers; i++) {
            budgets[i] = iterations / workers + (i < iterations % workers ? 1 : 0);
            samples[i] = new long[budgets[i]];
        }
        Thread[] threads = new Thread[workers];
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(workers);
        for (int w = 0; w < workers; w++) {
            int id = w;
            // Retires its CPU and allocation before exiting: workers are joined before the closing snapshot.
            threads[w] = new Thread(AppCpuMeter.accounted(() -> {
                long[] mine = samples[id];
                int budget = budgets[id];
                try {
                    ready.countDown();
                    startGate.await();
                    for (int i = 0; i < budget; i++) {
                        long t0 = System.nanoTime();
                        workload.call(scenario);
                        mine[i] = System.nanoTime() - t0;
                        if (progress != null) {
                            progress.set(id, i + 1);
                        }
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            }), "bench-worker-" + w);
            threads[w].start();
        }
        ready.await();
        startGate.countDown();
        for (Thread t : threads) {
            t.join();
        }
        rethrow(failure.get());
        long[] all = new long[iterations];
        int at = 0;
        for (int i = 0; i < workers; i++) {
            System.arraycopy(samples[i], 0, all, at, budgets[i]);
            at += budgets[i];
        }
        return new Samples(all, at);
    }

    private static Samples runInflight(V3Workload workload, Scenario scenario, int iterations, int concurrency,
                                       Progress progress) throws Exception {
        if (!workload.supportsAsync()) {
            throw new IllegalArgumentException("in-flight mode needs an async arm");
        }
        long[] samples = new long[iterations];
        AtomicInteger cursor = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        // Every acquire is matched by exactly one release on completion, after the sample is recorded, so reacquiring
        // all permits at the end proves every submitted operation completed and was counted.
        Semaphore permits = new Semaphore(concurrency);
        for (int i = 0; i < iterations && failure.get() == null; i++) {
            permits.acquire();
            long t0 = System.nanoTime();
            CompletableFuture<?> f;
            try {
                f = workload.callAsync(scenario);
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

    private static void rethrow(Throwable t) throws Exception {
        if (t == null) {
            return;
        }
        if (t instanceof Exception e) {
            throw e;
        }
        throw new RuntimeException(t);
    }

    /** Worker-local completion counters, 16 longs apart so two workers never share a cache line. */
    static final class Progress {
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

    /** Nearest-rank latency distribution over every kept sample. */
    record LatencyStats(long count, double meanUs, double p50Us, double p90Us, double p99Us, double p999Us,
                        double maxUs) {
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
            return new LatencyStats(count, sum / 1_000.0 / count, percentile(sorted, 50.0), percentile(sorted, 90.0),
                                    percentile(sorted, 99.0), percentile(sorted, 99.9),
                                    sorted[sorted.length - 1] / 1_000.0);
        }

        private static double percentile(long[] sorted, double pct) {
            int idx = (int) Math.ceil(pct / 100.0 * sorted.length) - 1;
            return sorted[Math.max(0, Math.min(sorted.length - 1, idx))] / 1_000.0;
        }
    }
}
