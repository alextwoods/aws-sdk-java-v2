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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Pure evaluation of {@link SteadyStatePolicy} over a series of cumulative counter samples. Used by the client for the
 * gate (stop warming up at the first stable window) and by the validity rules to re-check, from the recorded samples,
 * both the closing warmup window and every rolling window of the measured series.
 */
public final class SteadyStateDetector {

    private SteadyStateDetector() {
    }

    /**
     * Cumulative counters at {@code tNanos} after the series origin (the first sample, at 0).
     *
     * @param allocatedBytes heap bytes allocated by all threads, or -1 when the JVM cannot measure it
     * @param compileMillis total JIT compilation time, or -1 when the JVM cannot measure it
     * @param systemCpuLoad whole-host CPU load (0..1) over the interval ending here, or -1 when unavailable (always
     *     -1 at the origin)
     */
    public record Sample(long tNanos, long operations, long failures, long cpuNanos, long allocatedBytes,
                         long compileMillis, double systemCpuLoad) {
    }

    /** The difference of two consecutive samples. */
    public record Interval(long wallNanos, long operations, long failures, long cpuNanos, long allocatedBytes,
                           long compileMillis, double systemCpuLoad) {

        double throughput() {
            return operations / (wallNanos / 1e9);
        }

        double cpuPerOp() {
            return cpuNanos / (double) operations;
        }

        double allocPerOp() {
            return allocatedBytes / (double) operations;
        }
    }

    /**
     * The evaluation of one window.
     *
     * @param reason why the window is not stable, or {@code null} when it is
     */
    public record WindowCheck(int firstInterval, int lastInterval, double compileCpuFraction,
                              double throughputRange, double cpuRange, double allocationRange, String reason) {

        public boolean stable() {
            return reason == null;
        }

        public ObjectNode toNode() {
            ObjectNode.Builder b = ObjectNode.builder()
                                             .withMember("stable", stable())
                                             .withMember("first_interval", firstInterval)
                                             .withMember("last_interval", lastInterval)
                                             .withMember("compile_cpu_fraction", finite(compileCpuFraction))
                                             .withMember("throughput_range", finite(throughputRange))
                                             .withMember("cpu_per_op_range", finite(cpuRange))
                                             .withMember("alloc_per_op_range", finite(allocationRange));
            return reason == null ? b.build() : b.withMember("reason", reason).build();
        }
    }

    /** Consecutive differences of {@code samples}; empty when fewer than two samples. */
    public static List<Interval> intervals(List<Sample> samples) {
        List<Interval> out = new ArrayList<>(Math.max(0, samples.size() - 1));
        for (int i = 1; i < samples.size(); i++) {
            Sample a = samples.get(i - 1);
            Sample b = samples.get(i);
            long alloc = a.allocatedBytes() < 0 || b.allocatedBytes() < 0 ? -1
                                                                           : b.allocatedBytes() - a.allocatedBytes();
            long compile = a.compileMillis() < 0 || b.compileMillis() < 0 ? -1
                                                                           : b.compileMillis() - a.compileMillis();
            out.add(new Interval(b.tNanos() - a.tNanos(), b.operations() - a.operations(),
                                 b.failures() - a.failures(), b.cpuNanos() - a.cpuNanos(), alloc, compile,
                                 b.systemCpuLoad()));
        }
        return out;
    }

    /** Evaluates intervals {@code [from, from + policy.window())} of {@code intervals}. */
    public static WindowCheck check(List<Interval> intervals, int from, SteadyStatePolicy policy) {
        int to = from + policy.window() - 1;
        if (from < 0 || to >= intervals.size()) {
            return new WindowCheck(from, to, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                                   "fewer than " + policy.window() + " full intervals");
        }
        List<Interval> w = intervals.subList(from, to + 1);
        long wall = 0;
        long cpu = 0;
        long compile = 0;
        boolean compileKnown = true;
        boolean allocKnown = true;
        String reason = null;
        for (int i = 0; i < w.size(); i++) {
            Interval iv = w.get(i);
            wall += iv.wallNanos();
            cpu += iv.cpuNanos();
            compileKnown &= iv.compileMillis() >= 0;
            allocKnown &= iv.allocatedBytes() >= 0;
            compile += Math.max(0, iv.compileMillis());
            if (reason == null && iv.failures() > 0) {
                reason = "interval " + (from + i) + " has " + iv.failures() + " failed operation(s)";
            }
            if (reason == null && iv.operations() < SteadyStatePolicy.MIN_OPERATIONS_PER_INTERVAL) {
                reason = "interval " + (from + i) + " completed " + iv.operations() + " operation(s), fewer than "
                         + SteadyStatePolicy.MIN_OPERATIONS_PER_INTERVAL;
            }
            if (reason == null && iv.wallNanos() <= 0) {
                reason = "interval " + (from + i) + " has no wall time";
            }
        }
        if (reason != null) {
            return new WindowCheck(from, to, Double.NaN, Double.NaN, Double.NaN, Double.NaN, reason);
        }
        double compileFraction = compileKnown && cpu > 0 ? compile * 1e6 / cpu : Double.NaN;
        double throughput = relativeRange(w.stream().mapToDouble(Interval::throughput).toArray());
        double cpuRange = relativeRange(w.stream().mapToDouble(Interval::cpuPerOp).toArray());
        double alloc = allocKnown ? relativeRange(w.stream().mapToDouble(Interval::allocPerOp).toArray())
                                  : Double.NaN;
        if (!compileKnown) {
            reason = "JIT compilation time is not measurable on this JVM";
        } else if (cpu <= 0) {
            reason = "no process CPU time in the window";
        } else if (!allocKnown) {
            reason = "allocated bytes are not measurable on this JVM";
        } else if (!(compileFraction <= SteadyStatePolicy.MAX_COMPILE_CPU_FRACTION)) {
            reason = String.format(Locale.ROOT, "JIT compilation %.2f%% of process CPU > %.2f%%",
                                   compileFraction * 100, SteadyStatePolicy.MAX_COMPILE_CPU_FRACTION * 100);
        } else if (!(throughput <= SteadyStatePolicy.MAX_THROUGHPUT_RANGE)) {
            reason = String.format(Locale.ROOT, "throughput range %.4f > %.2f", throughput,
                                   SteadyStatePolicy.MAX_THROUGHPUT_RANGE);
        } else if (!(cpuRange <= SteadyStatePolicy.MAX_CPU_RANGE)) {
            reason = String.format(Locale.ROOT, "CPU/op range %.4f > %.2f", cpuRange,
                                   SteadyStatePolicy.MAX_CPU_RANGE);
        } else if (!(alloc <= SteadyStatePolicy.MAX_ALLOCATION_RANGE)) {
            reason = String.format(Locale.ROOT, "allocation/op range %.4f > %.2f", alloc,
                                   SteadyStatePolicy.MAX_ALLOCATION_RANGE);
        }
        return new WindowCheck(from, to, compileFraction, throughput, cpuRange, alloc, reason);
    }

    /** The window closing at the last interval: what the gate evaluates after each sample. */
    public static WindowCheck closingWindow(List<Interval> intervals, SteadyStatePolicy policy) {
        return check(intervals, intervals.size() - policy.window(), policy);
    }

    /**
     * Every rolling window of a measured series ({@code intervals[i .. i+window-1]} for each {@code i}); returns the
     * first unstable window, or the last window when all are stable, or a not-evaluable result when the series is
     * shorter than one window.
     */
    public static WindowCheck everyWindow(List<Interval> intervals, SteadyStatePolicy policy) {
        if (intervals.size() < policy.window()) {
            return check(intervals, 0, policy);
        }
        WindowCheck last = null;
        for (int i = 0; i + policy.window() <= intervals.size(); i++) {
            last = check(intervals, i, policy);
            if (!last.stable()) {
                return last;
            }
        }
        return last;
    }

    static double relativeRange(double[] values) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        for (double v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
            sum += v;
        }
        double mean = sum / values.length;
        if (mean == 0) {
            return max == min ? 0 : Double.POSITIVE_INFINITY;
        }
        return (max - min) / Math.abs(mean);
    }

    /** {@code [t_ns, ops, failures, cpu_ns, alloc_bytes, compile_ms, system_cpu_load]} per sample. */
    public static ArrayNode toNode(List<Sample> samples) {
        ArrayNode.Builder b = ArrayNode.builder();
        for (Sample s : samples) {
            b.withValue(ArrayNode.fromNodes(Node.from(s.tNanos()), Node.from(s.operations()),
                                            Node.from(s.failures()), Node.from(s.cpuNanos()),
                                            Node.from(s.allocatedBytes()), Node.from(s.compileMillis()),
                                            Node.from(s.systemCpuLoad())));
        }
        return b.build();
    }

    public static List<Sample> parse(ArrayNode node) {
        List<Sample> out = new ArrayList<>(node.size());
        for (Node n : node.getElements()) {
            List<Node> v = n.expectArrayNode().getElements();
            out.add(new Sample(v.get(0).expectNumberNode().getValue().longValue(),
                               v.get(1).expectNumberNode().getValue().longValue(),
                               v.get(2).expectNumberNode().getValue().longValue(),
                               v.get(3).expectNumberNode().getValue().longValue(),
                               v.get(4).expectNumberNode().getValue().longValue(),
                               v.get(5).expectNumberNode().getValue().longValue(),
                               v.get(6).expectNumberNode().getValue().doubleValue()));
        }
        return out;
    }

    /** JSON has no NaN/Infinity: an unevaluated metric is recorded as -1. */
    private static double finite(double v) {
        return Double.isFinite(v) ? v : -1;
    }
}
