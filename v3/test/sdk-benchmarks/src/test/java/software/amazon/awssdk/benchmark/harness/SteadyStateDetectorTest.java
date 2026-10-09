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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The steady-state rule on synthetic counter series, interval by interval. */
class SteadyStateDetectorTest {
    private static final SteadyStatePolicy POLICY = SteadyStatePolicy.RELEASE;
    private static final long SECOND = 1_000_000_000L;

    /** Per-interval values; {@link #series} accumulates them into samples. */
    record Step(long ops, long cpuNanos, long allocBytes, long compileMillis, long failures) {
        static Step steady() {
            return new Step(10_000, 900_000_000L, 40_000_000L, 0, 0);
        }

        Step ops(long v) {
            return new Step(v, cpuNanos, allocBytes, compileMillis, failures);
        }

        Step cpu(long v) {
            return new Step(ops, v, allocBytes, compileMillis, failures);
        }

        Step alloc(long v) {
            return new Step(ops, cpuNanos, v, compileMillis, failures);
        }

        Step compile(long v) {
            return new Step(ops, cpuNanos, allocBytes, v, failures);
        }

        Step failures(long v) {
            return new Step(ops, cpuNanos, allocBytes, compileMillis, v);
        }
    }

    static List<SteadyStateDetector.Sample> series(List<Step> steps) {
        List<SteadyStateDetector.Sample> out = new ArrayList<>();
        long ops = 0;
        long fail = 0;
        long cpu = 0;
        long alloc = 0;
        long compile = 500;
        out.add(new SteadyStateDetector.Sample(0, 0, 0, 0, 0, compile, -1));
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            ops += s.ops();
            fail += s.failures();
            cpu += s.cpuNanos();
            alloc = s.allocBytes() < 0 ? -1 : alloc + s.allocBytes();
            compile = s.compileMillis() < 0 ? -1 : compile + s.compileMillis();
            out.add(new SteadyStateDetector.Sample((i + 1) * SECOND, ops, fail, cpu, alloc, compile, 0.2));
        }
        return out;
    }

    static List<SteadyStateDetector.Interval> intervals(List<Step> steps) {
        return SteadyStateDetector.intervals(series(steps));
    }

    static List<Step> steady(int n) {
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            // +-2% deterministic jitter on every metric.
            double j = 1 + 0.02 * Math.sin(i * 1.7);
            Step s = Step.steady();
            steps.add(s.ops((long) (s.ops() * j)).cpu((long) (s.cpuNanos() * j * j))
                       .alloc((long) (s.allocBytes() * j)));
        }
        return steps;
    }

    @Test
    void steadySeriesIsStableEverywhere() {
        List<SteadyStateDetector.Interval> iv = intervals(steady(30));
        assertTrue(SteadyStateDetector.closingWindow(iv, POLICY).stable());
        SteadyStateDetector.WindowCheck all = SteadyStateDetector.everyWindow(iv, POLICY);
        assertTrue(all.stable(), all.toString());
        assertEquals(25, all.firstInterval());
        assertEquals(29, all.lastInterval());
    }

    /** JIT compiling hard for 8 s, then quiet: the gate passes at the first all-quiet 5-interval window. */
    @Test
    void gatePassesAtTheFirstStableWindowAfterCompilationSettles() {
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            steps.add(Step.steady().compile(200 - 20L * i).ops(3_000 + 800L * i));
        }
        steps.addAll(steady(10));
        List<SteadyStateDetector.Sample> samples = series(steps);
        int passedAt = -1;
        for (int n = 2; n <= samples.size(); n++) {
            if (SteadyStateDetector.closingWindow(SteadyStateDetector.intervals(samples.subList(0, n)), POLICY)
                                   .stable()) {
                passedAt = n - 1;
                break;
            }
        }
        assertEquals(8 + POLICY.window(), passedAt);
    }

    @Test
    void eachThresholdRejectsItsOwnInstability() {
        // About 4.5 CPU-seconds per window: 1% is about 45 ms of compilation.
        assertReason(withStep(steady(5), 2, Step.steady().compile(50)), "JIT compilation");
        assertTrue(SteadyStateDetector.closingWindow(intervals(withStep(steady(5), 2, Step.steady().compile(40))),
                                                     POLICY).stable(), "under 1% of process CPU is stable");
        assertReason(withStep(steady(5), 4, Step.steady().ops(12_500)), "throughput range");
        assertReason(withStep(steady(5), 1, Step.steady().cpu(1_200_000_000L)), "CPU/op range");
        assertReason(withStep(steady(5), 3, Step.steady().alloc(60_000_000L)), "allocation/op range");
        assertReason(withStep(steady(5), 0, Step.steady().ops(9)), "fewer than 10");
        assertReason(withStep(steady(5), 0, Step.steady().failures(1)), "failed operation");
        assertReason(withStep(steady(5), 0, Step.steady().compile(-1)), "compilation time is not measurable");
        assertReason(withStep(steady(5), 0, Step.steady().alloc(-1)), "allocated bytes are not measurable");
        assertReason(steady(4), "fewer than 5 full intervals");
    }

    @Test
    void everyWindowReportsTheFirstUnstableWindowOfAMeasuredSeries() {
        List<Step> steps = withStep(steady(30), 17, Step.steady().compile(100));
        SteadyStateDetector.WindowCheck w = SteadyStateDetector.everyWindow(intervals(steps), POLICY);
        assertFalse(w.stable());
        assertEquals(13, w.firstInterval());
        assertEquals(17, w.lastInterval());
        assertTrue(w.reason().contains("JIT compilation"), w.reason());
        assertFalse(SteadyStateDetector.everyWindow(intervals(steady(3)), POLICY).stable());
    }

    @Test
    void samplesRoundTripThroughJson() {
        List<SteadyStateDetector.Sample> s = series(steady(7));
        assertEquals(s, SteadyStateDetector.parse(SteadyStateDetector.toNode(s)));
        assertEquals(0.0, SteadyStateDetector.relativeRange(new double[] {0, 0}));
        assertEquals(SteadyStatePolicy.RELEASE, SteadyStatePolicy.fromNode(SteadyStatePolicy.RELEASE.toNode()));
        assertFalse(new SteadyStatePolicy(Duration.ofMillis(100), 3, Duration.ofSeconds(1)).isDocumented());
    }

    private static List<Step> withStep(List<Step> steps, int index, Step step) {
        List<Step> out = new ArrayList<>(steps);
        out.set(index, step);
        return out;
    }

    private static void assertReason(List<Step> steps, String expected) {
        SteadyStateDetector.WindowCheck w = SteadyStateDetector.closingWindow(intervals(steps), POLICY);
        assertFalse(w.stable(), expected);
        assertTrue(w.reason().contains(expected), w.reason());
    }
}
