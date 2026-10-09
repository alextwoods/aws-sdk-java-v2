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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The published per-repetition metrics of one run record (Req 20.10), extracted from the orchestrator's run record and
 * the client's result record. A metric the record does not carry, or carries as unavailable (a negative counter, e.g.
 * allocation on a JVM without thread allocation accounting), is absent rather than zero, so it can never make an arm
 * look cheaper. Host, JDK, JVM-flag and configuration metadata are published alongside, from the run and plan records.
 *
 * @param values the present metrics, each finite and non-negative
 */
public record RepetitionMetrics(Map<Metric, Double> values) {

    /** One published metric. */
    public enum Metric {
        /** Application (client process) CPU time per operation over the measured window. */
        CPU_PER_OP("cpu_per_op_ns", true, true),
        /** Bytes allocated by every client thread per operation over the measured window. */
        ALLOC_PER_OP("alloc_per_op_bytes", true, true),
        P50("p50_ns", true, true),
        P99("p99_ns", true, true),
        THROUGHPUT("throughput_ops_per_s", false, true),
        /** Cold client construction: process start of the workload to a built client. */
        CONSTRUCTION("construction_ns", true, false),
        /** The first (untimed-window, verified) call after construction. */
        FIRST_CALL("first_call_ns", true, false),
        /** Combined construction and first call (Req 20.19). */
        STARTUP("startup_ns", true, false),
        LOADED_CLASSES_AFTER_FIRST_CALL("loaded_classes_after_first_call", true, false),
        LOADED_CLASSES("loaded_classes", true, false),
        /** Bytes of every classpath entry of the client launch. */
        ARTIFACT_BYTES("artifact_bytes", true, false),
        /** Peak heap use over the client process lifetime (construction, warmup and measurement). */
        PEAK_HEAP("peak_heap_bytes", true, false),
        /** The client JVM's maximum heap setting. */
        MAX_HEAP("max_heap_bytes", true, false);

        private final String id;
        private final boolean lowerIsBetter;
        private final boolean steadyState;

        Metric(String id, boolean lowerIsBetter, boolean steadyState) {
            this.id = id;
            this.lowerIsBetter = lowerIsBetter;
            this.steadyState = steadyState;
        }

        public String id() {
            return id;
        }

        /** True for costs (CPU, bytes, time, classes); false for throughput. */
        public boolean lowerIsBetter() {
            return lowerIsBetter;
        }

        /** True when the metric is measured over the steady-state window (else cold start / footprint). */
        public boolean steadyState() {
            return steadyState;
        }

        public static Metric fromId(String id) {
            for (Metric m : values()) {
                if (m.id.equals(id)) {
                    return m;
                }
            }
            throw new IllegalArgumentException("unknown metric " + id);
        }
    }

    public RepetitionMetrics {
        EnumMap<Metric, Double> copy = new EnumMap<>(Metric.class);
        values.forEach((k, v) -> {
            if (v == null || !Double.isFinite(v) || v < 0) {
                throw new IllegalArgumentException(k.id() + " must be finite and non-negative: " + v);
            }
            copy.put(k, v);
        });
        values = Collections.unmodifiableMap(copy);
    }

    public OptionalDouble get(Metric m) {
        Double v = values.get(m);
        return v == null ? OptionalDouble.empty() : OptionalDouble.of(v);
    }

    /**
     * The metrics of one run record (as forwarded by the orchestrator), or empty when it has no result record. Steady
     * state metrics need a measurement with at least one operation and a positive wall time.
     */
    public static Optional<RepetitionMetrics> of(ObjectNode run) {
        Optional<ObjectNode> result = run.getObjectMember("result");
        if (result.isEmpty()) {
            return Optional.empty();
        }
        ObjectNode r = result.get();
        EnumMap<Metric, Double> v = new EnumMap<>(Metric.class);
        long construction = num(r, "construction_ns");
        long firstCall = num(r, "first_call_ns");
        put(v, Metric.CONSTRUCTION, construction);
        put(v, Metric.FIRST_CALL, firstCall);
        if (construction >= 0 && firstCall >= 0) {
            put(v, Metric.STARTUP, construction + firstCall);
        }
        put(v, Metric.LOADED_CLASSES_AFTER_FIRST_CALL, num(r, "loaded_classes_after_first_call"));
        put(v, Metric.ARTIFACT_BYTES, run.getObjectMember("arm").map(a -> num(a, "artifact_bytes")).orElse(-1L));
        put(v, Metric.MAX_HEAP, r.getObjectMember("process").map(p -> num(p, "max_heap_bytes")).orElse(-1L));
        r.getObjectMember("measurement").ifPresent(m -> {
            long ops = num(m, "operations");
            long wall = num(m, "wall_ns");
            put(v, Metric.LOADED_CLASSES, num(m, "loaded_classes"));
            put(v, Metric.PEAK_HEAP, num(m, "peak_heap_bytes"));
            if (ops > 0 && wall > 0) {
                long cpu = num(m, "process_cpu_ns");
                long alloc = num(m, "allocated_bytes");
                if (cpu >= 0) {
                    v.put(Metric.CPU_PER_OP, cpu / (double) ops);
                }
                if (alloc >= 0) {
                    v.put(Metric.ALLOC_PER_OP, alloc / (double) ops);
                }
                put(v, Metric.P50, num(m, "p50_ns"));
                put(v, Metric.P99, num(m, "p99_ns"));
                v.put(Metric.THROUGHPUT, ops / (wall / 1e9));
            }
        });
        return Optional.of(new RepetitionMetrics(v));
    }

    public ObjectNode toNode() {
        ObjectNode.Builder b = ObjectNode.builder();
        values.forEach((k, x) -> b.withMember(k.id(), Node.from(x)));
        return b.build();
    }

    private static void put(Map<Metric, Double> v, Metric m, long x) {
        if (x >= 0) {
            v.put(m, (double) x);
        }
    }

    /** A numeric member, or -1 when absent. */
    private static long num(ObjectNode n, String member) {
        return n.getNumberMember(member).map(x -> x.getValue().longValue()).orElse(-1L);
    }
}
