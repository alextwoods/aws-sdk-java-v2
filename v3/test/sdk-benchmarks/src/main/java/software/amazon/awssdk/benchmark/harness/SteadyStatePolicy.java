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

package software.amazon.awssdk.benchmark.harness;

import java.time.Duration;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The documented JIT steady-state gate and rolling stability thresholds (Req 20.7, 20.9, 20.26).
 *
 * <p>The client samples cumulative counters (completed operations, process CPU, heap bytes allocated by all threads,
 * total JIT compilation time, system CPU load) every {@link #interval()}. A <em>steady-state window</em> is
 * {@link #window()} consecutive full intervals. A window is stable when, over its intervals:
 * <ul>
 *     <li>compilation: total JIT compilation time grew by at most {@link #MAX_COMPILE_CPU_FRACTION} of the client
 *     process CPU time of the window, which bounds what compilation adds to CPU per operation;</li>
 *     <li>throughput: the relative range {@code (max - min) / mean} of per-interval operations per second is at most
 *     {@link #MAX_THROUGHPUT_RANGE};</li>
 *     <li>CPU: the relative range of per-interval process CPU nanoseconds per operation is at most
 *     {@link #MAX_CPU_RANGE};</li>
 *     <li>allocation: the relative range of per-interval allocated bytes per operation is at most
 *     {@link #MAX_ALLOCATION_RANGE};</li>
 *     <li>every interval completed at least {@link #MIN_OPERATIONS_PER_INTERVAL} operations (fewer makes the rates
 *     quantization noise), and compilation time and allocation are measurable.</li>
 * </ul>
 *
 * <p>The gate passes at the first sample that closes a stable window; measurement starts after the gate. If no
 * window is stable within {@link #warmupTimeout()}, the gate fails and the cell is invalid (Req 20.26). The measured
 * window must itself be at least {@link #MIN_RELEASE_MEASURE} long in release mode (Req 20.7), and every rolling
 * window inside it must be stable (Req 20.9), otherwise the repetition is invalid.
 *
 * <p>The thresholds are constants: no mode or command line can relax them. Only the sampling geometry
 * ({@code interval}, {@code window}, {@code warmupTimeout}) may differ from {@link #RELEASE}, and only in
 * {@code HarnessMode.EXPLORATORY}, where it lets development runs and smoke tests finish in seconds; release
 * preflight refuses any policy other than {@link #RELEASE}.
 *
 * @param interval sampling interval
 * @param window full intervals per steady-state window
 * @param warmupTimeout the longest the gate may take before the cell is invalid
 */
public record SteadyStatePolicy(Duration interval, int window, Duration warmupTimeout) {

    public static final double MAX_COMPILE_CPU_FRACTION = 0.01;
    public static final double MAX_THROUGHPUT_RANGE = 0.10;
    public static final double MAX_CPU_RANGE = 0.10;
    public static final double MAX_ALLOCATION_RANGE = 0.10;
    public static final long MIN_OPERATIONS_PER_INTERVAL = 10;
    /** Minimum measured window after the gate in release mode (Req 20.7). */
    public static final Duration MIN_RELEASE_MEASURE = Duration.ofSeconds(30);

    /** The documented release policy: 1 s intervals, a 5-interval (5 s) window, 300 s warmup timeout. */
    public static final SteadyStatePolicy RELEASE =
        new SteadyStatePolicy(Duration.ofSeconds(1), 5, Duration.ofSeconds(300));

    public SteadyStatePolicy {
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("interval must be positive");
        }
        if (window < 2) {
            throw new IllegalArgumentException("a steady-state window needs at least 2 intervals: " + window);
        }
        if (warmupTimeout.compareTo(interval.multipliedBy(window)) < 0) {
            throw new IllegalArgumentException("warmup timeout " + warmupTimeout + " is shorter than one window");
        }
    }

    public boolean isDocumented() {
        return equals(RELEASE);
    }

    /** Wall time of one window. */
    public Duration windowDuration() {
        return interval.multipliedBy(window);
    }

    /** The geometry recorded by {@link #toNode()}; the thresholds are constants and are not read back. */
    public static SteadyStatePolicy fromNode(ObjectNode node) {
        return new SteadyStatePolicy(Duration.ofNanos(node.expectNumberMember("interval_ns").getValue().longValue()),
                                     node.expectNumberMember("window_intervals").getValue().intValue(),
                                     Duration.ofNanos(node.expectNumberMember("warmup_timeout_ns").getValue()
                                                          .longValue()));
    }

    public ObjectNode toNode() {
        return ObjectNode.builder()
                         .withMember("interval_ns", interval.toNanos())
                         .withMember("window_intervals", window)
                         .withMember("warmup_timeout_ns", warmupTimeout.toNanos())
                         .withMember("max_compile_cpu_fraction", MAX_COMPILE_CPU_FRACTION)
                         .withMember("max_throughput_range", MAX_THROUGHPUT_RANGE)
                         .withMember("max_cpu_per_op_range", MAX_CPU_RANGE)
                         .withMember("max_alloc_per_op_range", MAX_ALLOCATION_RANGE)
                         .withMember("min_ops_per_interval", MIN_OPERATIONS_PER_INTERVAL)
                         .withMember("min_release_measure_s", MIN_RELEASE_MEASURE.toSeconds())
                         .withMember("documented", isDocumented())
                         .build();
    }
}
