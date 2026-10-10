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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.benchmark.harness.BenchmarkStatistics.Ratio;
import software.amazon.awssdk.benchmark.harness.RepetitionMetrics.Metric;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The automated performance release gates (Req 20.12-20.22, 20.29): a pure function of a {@link PerformanceReport}
 * ({@code report.json}), its {@link CollectionValidity} verdict ({@code validity.json}), the required cell set and the
 * optional separately produced evidence of the checks that are not harness cells. It recomputes no measurement: every
 * number it judges is read from the report.
 *
 * <p>Each {@link Gate} has one {@link Status}: {@link Status#PASS} only when every check of the gate passed and at least
 * one check exists; {@link Status#FAIL} when any check missed its boundary; otherwise {@link Status#NOT_EVALUABLE}
 * (missing cells, invalid cells, missing references, no ratio, absent evidence). Not-evaluable never passes: the
 * {@link Decision} allows general availability only for a release-mode, release-evidence collection whose every gate
 * passed; an exploratory collection is evaluated and reported but always yields {@link Decision#GA_BLOCKED}.
 *
 * <p>Confidence-interval rules, by the requirement's wording:
 * <ul>
 *     <li>{@link Rule#POINT_ESTIMATE} (Req 20.12, 20.13: "no more than N% ... by equal-weight geometric mean"; 20.16,
 *     20.17, 20.18: "no more than N% additional ... over the corresponding ... cell"): the ratio itself must be at most
 *     the boundary; its interval is reported, not judged.</li>
 *     <li>{@link Rule#STATISTICALLY_VALID} (Req 20.14, 20.15, 20.19: "... as a Statistically Valid Result"): at least
 *     five valid interleaved repetitions in each compared arm, a within-bounds same-artifact null experiment, a valid
 *     (steady-state, failure-free) cell, and a 95% interval that does not cross the boundary, i.e. the upper bound is
 *     at most the boundary ({@link Ratio#upperBoundAtMost}); and (Req 20.23) a relative half-width of at most
 *     {@value #MAX_RELATIVE_HALF_WIDTH}. An upper bound above the boundary is a {@link Status#FAIL}; an interval
 *     below it that is wider than 5% is {@link Status#NOT_EVALUABLE} (not a Statistically Valid Result).</li>
 * </ul>
 */
public final class PerformanceGates {

    public static final String SCHEMA = "v3-performance-gates/1";
    public static final String LARGE_STREAM_SCHEMA = "v3-large-stream-check/1";
    public static final String HOT_PATH_SCHEMA = "v3-hot-path-check/1";

    /** Req 20.12: V3 / Launch Parity Baseline CPU per operation, generated non-streaming geometric mean. */
    public static final double CPU_VS_BASELINE_AGGREGATE = 0.70;
    /** Req 20.13: V3 / Launch Parity Baseline bytes per operation, generated non-streaming geometric mean. */
    public static final double ALLOC_VS_BASELINE_AGGREGATE = 0.60;
    /** Req 20.14, 20.15: V3 / best valid Performance Reference per generated cell ("by no more than 10%"). */
    public static final double VS_BEST_REFERENCE = 1.10;
    /** Req 20.16: async / sync application CPU per operation ("no more than 35% additional"). */
    public static final double ASYNC_OVER_SYNC_CPU = 1.35;
    /** Req 20.17, 20.18: dynamic / generated CPU and bytes per operation ("no more than 25% additional"). */
    public static final double DYNAMIC_OVER_GENERATED = 1.25;
    /** Req 20.19: V3 / Launch Parity Baseline combined construction-and-first-call time ("by no more than 5%"). */
    public static final double STARTUP_VS_BASELINE = 1.05;
    /** Req 20.23: the relative half-width a Statistically Valid Result may have. */
    public static final double MAX_RELATIVE_HALF_WIDTH = 0.05;
    /** Req 20.6, 20.28: valid interleaved repetitions each compared arm needs. */
    public static final int MIN_VALID_REPETITIONS = RepetitionSchedule.MIN_RELEASE_REPETITIONS;
    /** Req 20.20: the transfer and the maximum heap. */
    public static final long LARGE_STREAM_BYTES = 1L << 30;
    public static final long LARGE_STREAM_MAX_HEAP_BYTES = 256L << 20;
    /**
     * Req 20.20's documented release-gate timeout: the per-transfer timeout of the bounded-memory suite
     * ({@code StreamingBoundedMemoryTest}, {@code @Timeout(300 s)}) that moves 1 GiB through a 256 MiB heap.
     */
    public static final Duration LARGE_STREAM_TIMEOUT = Duration.ofSeconds(300);

    public static final String CPU_AGGREGATE = "cpu-vs-launch-parity-aggregate";
    public static final String ALLOC_AGGREGATE = "alloc-vs-launch-parity-aggregate";
    public static final String CPU_BEST_REFERENCE = "cpu-vs-best-reference";
    public static final String ALLOC_BEST_REFERENCE = "alloc-vs-best-reference";
    public static final String ASYNC_CPU = "async-over-sync-cpu";
    public static final String DYNAMIC_CPU = "dynamic-over-generated-cpu";
    public static final String DYNAMIC_ALLOC = "dynamic-over-generated-alloc";
    public static final String STARTUP = "startup-vs-launch-parity";
    public static final String LARGE_STREAM = "large-stream-bounded-memory";
    public static final String INACTIVE_EXTENSIONS = "inactive-extension-allocation";
    public static final String HOT_PATH = "hot-path-no-reflection-lookup-or-plan";
    public static final String COLLECTION = "release-gate-collection-valid";

    /** Every gate, in evaluation order. */
    public static final List<String> GATES = List.of(CPU_AGGREGATE, ALLOC_AGGREGATE, CPU_BEST_REFERENCE,
                                                     ALLOC_BEST_REFERENCE, ASYNC_CPU, DYNAMIC_CPU, DYNAMIC_ALLOC,
                                                     STARTUP, LARGE_STREAM, INACTIVE_EXTENSIONS, HOT_PATH,
                                                     COLLECTION);

    private PerformanceGates() {
    }

    public enum Status {
        PASS("pass"),
        FAIL("fail"),
        NOT_EVALUABLE("not-evaluable");

        private final String id;

        Status(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Status fromId(String id) {
            for (Status s : values()) {
                if (s.id.equals(id)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("unknown gate status " + id);
        }
    }

    /** How a gate's ratio is judged against its boundary. */
    public enum Rule {
        POINT_ESTIMATE("point-estimate-at-most-boundary"),
        STATISTICALLY_VALID("statistically-valid-ci-high-at-most-boundary"),
        EVIDENCE("separate-evidence"),
        COLLECTION("collection-validity");

        private final String id;

        Rule(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Rule fromId(String id) {
            for (Rule r : values()) {
                if (r.id.equals(id)) {
                    return r;
                }
            }
            throw new IllegalArgumentException("unknown gate rule " + id);
        }
    }

    public enum Decision {
        GA_ALLOWED("ga-allowed"),
        GA_BLOCKED("ga-blocked");

        private final String id;

        Decision(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Decision fromId(String id) {
            for (Decision d : values()) {
                if (d.id.equals(id)) {
                    return d;
                }
            }
            throw new IllegalArgumentException("unknown release decision " + id);
        }
    }

    /**
     * One judged subject (a cell, a pair, an aggregate, an evidence entry, a collection property).
     *
     * @param inputs the report values the decision read (ratio, interval, boundary, reference ...)
     */
    public record Check(String subject, Status status, ObjectNode inputs, String reason) {
        public Check {
            Objects.requireNonNull(subject, "subject");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(inputs, "inputs");
            Objects.requireNonNull(reason, "reason");
        }

        ObjectNode toNode() {
            return ObjectNode.builder().withMember("subject", subject).withMember("status", status.id())
                             .withMember("reason", reason).withMember("inputs", inputs).build();
        }

        static Check fromNode(ObjectNode n) {
            return new Check(n.expectStringMember("subject").getValue(),
                             Status.fromId(n.expectStringMember("status").getValue()), n.expectObjectMember("inputs"),
                             n.expectStringMember("reason").getValue());
        }
    }

    /** One gate. {@code boundary} is {@code NaN} for gates without a numeric ratio boundary. */
    public record Gate(String id, List<String> requirements, String metric, double boundary, Rule rule,
                       Status status, List<Check> checks, List<String> reasons) {
        public Gate {
            requirements = List.copyOf(requirements);
            checks = List.copyOf(checks);
            reasons = List.copyOf(reasons);
        }

        ObjectNode toNode() {
            ObjectNode.Builder b = ObjectNode.builder().withMember("id", id)
                .withMember("requirements", ArrayNode.fromStrings(requirements))
                .withMember("rule", rule.id()).withMember("status", status.id())
                .withMember("reasons", ArrayNode.fromStrings(reasons))
                .withMember("checks", ArrayNode.fromNodes(checks.stream().map(c -> (Node) c.toNode()).toList()));
            if (metric != null) {
                b.withMember("metric", metric);
            }
            if (!Double.isNaN(boundary)) {
                b.withMember("boundary", boundary);
            }
            return b.build();
        }

        static Gate fromNode(ObjectNode n) {
            List<Check> checks = new ArrayList<>();
            n.expectArrayMember("checks").getElements().forEach(c -> checks.add(Check.fromNode(c.expectObjectNode())));
            return new Gate(n.expectStringMember("id").getValue(), strings(n.expectArrayMember("requirements")),
                            n.getStringMember("metric").map(s -> s.getValue()).orElse(null),
                            n.getNumberMember("boundary").map(x -> x.getValue().doubleValue()).orElse(Double.NaN),
                            Rule.fromId(n.expectStringMember("rule").getValue()),
                            Status.fromId(n.expectStringMember("status").getValue()), checks,
                            strings(n.expectArrayMember("reasons")));
        }
    }

    /**
     * Everything the gates read.
     *
     * @param report {@code report.json} ({@value PerformanceReport#SCHEMA})
     * @param validity {@code validity.json} ({@value CollectionValidity#SCHEMA}), empty when absent
     * @param requiredCells the cell ids a release-gate collection must contain (the full matrix for release)
     * @param largeStream the Req 20.20 evidence ({@value #LARGE_STREAM_SCHEMA}), empty when absent
     * @param hotPath the Req 20.21/20.22 evidence ({@value #HOT_PATH_SCHEMA}), empty when absent
     */
    public record Inputs(ObjectNode report, Optional<ObjectNode> validity, List<String> requiredCells,
                         Optional<ObjectNode> largeStream, Optional<ObjectNode> hotPath) {
        public Inputs {
            Objects.requireNonNull(report, "report");
            requiredCells = List.copyOf(new LinkedHashSet<>(requiredCells));
        }
    }

    /** The versioned gate result: every gate and the release decision. */
    public record Result(String runId, HarnessMode mode, boolean releaseEvidence, List<Gate> gates,
                         Decision decision, List<String> reasons) {
        public Result {
            gates = List.copyOf(gates);
            reasons = List.copyOf(reasons);
        }

        public Gate gate(String id) {
            return gates.stream().filter(g -> g.id().equals(id)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("no gate " + id));
        }

        public ObjectNode toNode() {
            return ObjectNode.builder()
                .withMember("schema", SCHEMA)
                .withMember("report_schema", PerformanceReport.SCHEMA)
                .withMember("run_id", runId)
                .withMember("mode", mode.id())
                .withMember("release_evidence", releaseEvidence)
                .withMember("release_decision", ObjectNode.builder().withMember("decision", decision.id())
                                                          .withMember("reasons", ArrayNode.fromStrings(reasons))
                                                          .build())
                .withMember("thresholds", thresholds())
                .withMember("gates", ArrayNode.fromNodes(gates.stream().map(g -> (Node) g.toNode()).toList()))
                .build();
        }

        public static Result fromNode(ObjectNode n) {
            if (!SCHEMA.equals(n.expectStringMember("schema").getValue())) {
                throw new IllegalArgumentException("not a " + SCHEMA + " document");
            }
            List<Gate> gates = new ArrayList<>();
            n.expectArrayMember("gates").getElements().forEach(g -> gates.add(Gate.fromNode(g.expectObjectNode())));
            ObjectNode d = n.expectObjectMember("release_decision");
            return new Result(n.expectStringMember("run_id").getValue(),
                              HarnessMode.fromId(n.expectStringMember("mode").getValue()),
                              n.expectBooleanMember("release_evidence").getValue(), gates,
                              Decision.fromId(d.expectStringMember("decision").getValue()),
                              strings(d.expectArrayMember("reasons")));
        }
    }

    static ObjectNode thresholds() {
        return ObjectNode.builder()
            .withMember("cpu_vs_launch_parity_aggregate", CPU_VS_BASELINE_AGGREGATE)
            .withMember("alloc_vs_launch_parity_aggregate", ALLOC_VS_BASELINE_AGGREGATE)
            .withMember("vs_best_reference", VS_BEST_REFERENCE)
            .withMember("async_over_sync_cpu", ASYNC_OVER_SYNC_CPU)
            .withMember("dynamic_over_generated", DYNAMIC_OVER_GENERATED)
            .withMember("startup_vs_launch_parity", STARTUP_VS_BASELINE)
            .withMember("max_relative_half_width", MAX_RELATIVE_HALF_WIDTH)
            .withMember("min_valid_repetitions", MIN_VALID_REPETITIONS)
            .withMember("null_ratio_min", CollectionValidity.NULL_RATIO_MIN)
            .withMember("null_ratio_max", CollectionValidity.NULL_RATIO_MAX)
            .withMember("large_stream_bytes", LARGE_STREAM_BYTES)
            .withMember("large_stream_max_heap_bytes", LARGE_STREAM_MAX_HEAP_BYTES)
            .withMember("large_stream_timeout_ns", LARGE_STREAM_TIMEOUT.toNanos())
            .build();
    }

    /** The cell ids a release collection must contain: every cell of the full matrix. */
    public static List<String> releaseRequiredCells() {
        return CellMatrix.full().stream().map(BenchmarkCell::id).toList();
    }

    /**
     * The required cells for {@code report}: the full matrix for a release report; for an exploratory report, the
     * cells it ran (an exploratory decision is blocked whatever its coverage, so its gates judge what it measured).
     */
    public static List<String> requiredCellsFor(ObjectNode report) {
        if (HarnessMode.RELEASE.id().equals(report.getStringMember("mode").map(s -> s.getValue()).orElse(""))) {
            return releaseRequiredCells();
        }
        return report.getArrayMember("cells").map(a -> a.getElements().stream()
            .map(c -> c.expectObjectNode().getStringMember("cell").map(s -> s.getValue()).orElse("?")).toList())
            .orElse(List.of());
    }

    /** Evaluates every gate and the release decision. Pure: same inputs, same result. */
    public static Result evaluate(Inputs in) {
        List<String> schema = PerformanceReport.schemaProblems(in.report());
        HarnessMode mode = in.report().getStringMember("mode").map(s -> {
            try {
                return HarnessMode.fromId(s.getValue());
            } catch (IllegalArgumentException e) {
                return HarnessMode.EXPLORATORY;
            }
        }).orElse(HarnessMode.EXPLORATORY);
        String runId = in.report().getStringMember("run_id").map(s -> s.getValue()).orElse("");
        boolean evidence = in.report().getBooleanMember("release_evidence").map(b -> b.getValue()).orElse(false);
        List<Gate> gates = new ArrayList<>();
        if (!schema.isEmpty()) {
            // An unreadable report decides nothing but the collection gate, which fails on it.
            for (String id : GATES) {
                if (!id.equals(COLLECTION)) {
                    gates.add(gate(id, List.of(), List.of("report does not conform to " + PerformanceReport.SCHEMA)));
                }
            }
            gates.add(collectionGate(in, schema));
        } else {
            View v = new View(in.report(), in.requiredCells());
            gates.add(aggregateGate(CPU_AGGREGATE, v, Metric.CPU_PER_OP, CPU_VS_BASELINE_AGGREGATE));
            gates.add(aggregateGate(ALLOC_AGGREGATE, v, Metric.ALLOC_PER_OP, ALLOC_VS_BASELINE_AGGREGATE));
            gates.add(bestReferenceGate(CPU_BEST_REFERENCE, v, Metric.CPU_PER_OP));
            gates.add(bestReferenceGate(ALLOC_BEST_REFERENCE, v, Metric.ALLOC_PER_OP));
            gates.add(pairGate(ASYNC_CPU, v, PerformanceReport.ASYNC_OVER_SYNC, Metric.CPU_PER_OP,
                               ASYNC_OVER_SYNC_CPU));
            gates.add(pairGate(DYNAMIC_CPU, v, PerformanceReport.DYNAMIC_OVER_GENERATED, Metric.CPU_PER_OP,
                               DYNAMIC_OVER_GENERATED));
            gates.add(pairGate(DYNAMIC_ALLOC, v, PerformanceReport.DYNAMIC_OVER_GENERATED, Metric.ALLOC_PER_OP,
                               DYNAMIC_OVER_GENERATED));
            gates.add(startupGate(v));
            gates.add(largeStreamGate(in.largeStream()));
            gates.add(hotPathGate(INACTIVE_EXTENSIONS, in.hotPath()));
            gates.add(hotPathGate(HOT_PATH, in.hotPath()));
            gates.add(collectionGate(in, schema));
        }
        List<String> reasons = new ArrayList<>();
        if (mode != HarnessMode.RELEASE) {
            reasons.add("an " + mode.id() + " collection is not release evidence");
        } else if (!evidence) {
            reasons.add("the collection did not qualify as release evidence in preflight");
        }
        for (Gate g : gates) {
            if (g.status() != Status.PASS) {
                reasons.add(g.id() + ": " + g.status().id());
            }
        }
        Decision decision = reasons.isEmpty() ? Decision.GA_ALLOWED : Decision.GA_BLOCKED;
        return new Result(runId, mode, evidence, gates, decision, reasons);
    }

    // ---- gate definitions ----

    private static final Map<String, List<String>> REQUIREMENTS = Map.ofEntries(
        Map.entry(CPU_AGGREGATE, List.of("20.12")), Map.entry(ALLOC_AGGREGATE, List.of("20.13")),
        Map.entry(CPU_BEST_REFERENCE, List.of("20.14", "20.23")),
        Map.entry(ALLOC_BEST_REFERENCE, List.of("20.15", "20.23")), Map.entry(ASYNC_CPU, List.of("20.16")),
        Map.entry(DYNAMIC_CPU, List.of("20.17")), Map.entry(DYNAMIC_ALLOC, List.of("20.18")),
        Map.entry(STARTUP, List.of("20.19", "20.23")), Map.entry(LARGE_STREAM, List.of("20.20")),
        Map.entry(INACTIVE_EXTENSIONS, List.of("20.21")), Map.entry(HOT_PATH, List.of("20.22")),
        Map.entry(COLLECTION, List.of("20.24", "20.25", "20.26", "20.27", "20.28", "20.29")));

    private static final Map<String, Rule> RULES = Map.ofEntries(
        Map.entry(CPU_AGGREGATE, Rule.POINT_ESTIMATE), Map.entry(ALLOC_AGGREGATE, Rule.POINT_ESTIMATE),
        Map.entry(CPU_BEST_REFERENCE, Rule.STATISTICALLY_VALID),
        Map.entry(ALLOC_BEST_REFERENCE, Rule.STATISTICALLY_VALID), Map.entry(ASYNC_CPU, Rule.POINT_ESTIMATE),
        Map.entry(DYNAMIC_CPU, Rule.POINT_ESTIMATE), Map.entry(DYNAMIC_ALLOC, Rule.POINT_ESTIMATE),
        Map.entry(STARTUP, Rule.STATISTICALLY_VALID), Map.entry(LARGE_STREAM, Rule.EVIDENCE),
        Map.entry(INACTIVE_EXTENSIONS, Rule.EVIDENCE), Map.entry(HOT_PATH, Rule.EVIDENCE),
        Map.entry(COLLECTION, Rule.COLLECTION));

    private static final Map<String, Metric> METRICS = Map.of(
        CPU_AGGREGATE, Metric.CPU_PER_OP, ALLOC_AGGREGATE, Metric.ALLOC_PER_OP, CPU_BEST_REFERENCE, Metric.CPU_PER_OP,
        ALLOC_BEST_REFERENCE, Metric.ALLOC_PER_OP, ASYNC_CPU, Metric.CPU_PER_OP, DYNAMIC_CPU, Metric.CPU_PER_OP,
        DYNAMIC_ALLOC, Metric.ALLOC_PER_OP, STARTUP, Metric.STARTUP);

    private static final Map<String, Double> BOUNDARIES = Map.of(
        CPU_AGGREGATE, CPU_VS_BASELINE_AGGREGATE, ALLOC_AGGREGATE, ALLOC_VS_BASELINE_AGGREGATE,
        CPU_BEST_REFERENCE, VS_BEST_REFERENCE, ALLOC_BEST_REFERENCE, VS_BEST_REFERENCE, ASYNC_CPU, ASYNC_OVER_SYNC_CPU,
        DYNAMIC_CPU, DYNAMIC_OVER_GENERATED, DYNAMIC_ALLOC, DYNAMIC_OVER_GENERATED, STARTUP, STARTUP_VS_BASELINE);

    /** Builds a gate from its checks: any FAIL fails; PASS needs at least one check, all passing, and no reason. */
    private static Gate gate(String id, List<Check> checks, List<String> preconditions) {
        Status status;
        List<String> reasons = new ArrayList<>(preconditions);
        if (checks.stream().anyMatch(c -> c.status() == Status.FAIL)) {
            status = Status.FAIL;
        } else if (!preconditions.isEmpty() || checks.isEmpty()
                   || checks.stream().anyMatch(c -> c.status() != Status.PASS)) {
            status = Status.NOT_EVALUABLE;
            if (checks.isEmpty() && preconditions.isEmpty()) {
                reasons.add("nothing to evaluate");
            }
        } else {
            status = Status.PASS;
        }
        long failed = checks.stream().filter(c -> c.status() == Status.FAIL).count();
        long notEvaluable = checks.stream().filter(c -> c.status() == Status.NOT_EVALUABLE).count();
        if (failed > 0) {
            reasons.add(failed + " of " + checks.size() + " checks missed the boundary");
        }
        if (notEvaluable > 0) {
            reasons.add(notEvaluable + " of " + checks.size() + " checks not evaluable");
        }
        Metric metric = METRICS.get(id);
        return new Gate(id, REQUIREMENTS.get(id), metric == null ? null : metric.id(),
                        BOUNDARIES.getOrDefault(id, Double.NaN), RULES.get(id), status, checks, reasons);
    }

    /** Req 20.12 / 20.13: the generated-non-streaming geometric mean over the Launch Parity Baseline. */
    private static Gate aggregateGate(String id, View v, Metric metric, double boundary) {
        List<String> pre = new ArrayList<>();
        Optional<String> baseline = v.baseline(pre);
        if (baseline.isEmpty()) {
            return gate(id, List.of(), pre);
        }
        Optional<ObjectNode> agg = v.aggregate(PerformanceReport.GENERATED_NON_STREAMING, baseline.get(), metric);
        ObjectNode.Builder inputs = ObjectNode.builder().withMember("reference", baseline.get())
                                              .withMember("boundary", boundary);
        String subject = PerformanceReport.GENERATED_NON_STREAMING + " " + metric.id() + " / " + baseline.get();
        if (agg.isEmpty()) {
            return gate(id, List.of(new Check(subject, Status.NOT_EVALUABLE, inputs.build(),
                                              "no aggregate over the Launch Parity Baseline")), List.of());
        }
        Set<String> included = new LinkedHashSet<>(strings(agg.get().expectArrayMember("cells")));
        List<String> excluded = strings(agg.get().expectArrayMember("excluded"));
        List<String> missing = v.required().stream().filter(c -> PerformanceReport.isGeneratedNonStreaming(c))
                                .map(BenchmarkCell::id).filter(c -> !included.contains(c)).toList();
        inputs.withMember("cells", included.size()).withMember("excluded", ArrayNode.fromStrings(excluded))
              .withMember("missing_required_cells", ArrayNode.fromStrings(missing));
        Optional<Ratio> g = agg.get().getObjectMember("geometric_mean").map(Ratio::fromNode);
        g.ifPresent(r -> ratioInputs(inputs, r));
        Status s;
        String reason;
        if (g.isEmpty()) {
            s = Status.NOT_EVALUABLE;
            reason = "no geometric mean (no valid cell with a ratio)";
        } else if (!missing.isEmpty()) {
            s = Status.NOT_EVALUABLE;
            reason = "the aggregate omits " + missing.size() + " required generated non-streaming cells";
        } else if (g.get().ratio() <= boundary) {
            s = Status.PASS;
            reason = fmt("geometric mean %.4f <= %.2f", g.get().ratio(), boundary);
        } else {
            s = Status.FAIL;
            reason = fmt("geometric mean %.4f > %.2f", g.get().ratio(), boundary);
        }
        return gate(id, List.of(new Check(subject, s, inputs.build(), reason)), List.of());
    }

    /** Req 20.14 / 20.15: every required generated cell against its best valid Performance Reference. */
    private static Gate bestReferenceGate(String id, View v, Metric metric) {
        List<String> pre = new ArrayList<>();
        if (!v.referencesReady()) {
            pre.add("not every Performance Reference is ready (" + v.unreadyReferences()
                    + "): the best valid Performance Reference is undefined");
        }
        List<Check> checks = new ArrayList<>();
        for (BenchmarkCell cell : v.required()) {
            if (cell.path().isDynamic()) {
                continue;
            }
            Optional<ObjectNode> c = v.cell(cell.id());
            Optional<ObjectNode> best = c.flatMap(n -> metricEntry(n.expectArrayMember("best_reference"), metric,
                                                                   null));
            String ref = best.flatMap(b -> b.getStringMember("reference")).map(s -> s.getValue()).orElse(null);
            checks.add(statisticallyValid(cell.id(), c, ref, best.map(Ratio::fromNode), VS_BEST_REFERENCE,
                                          "no valid Performance Reference result for this cell"));
        }
        return gate(id, checks, pre);
    }

    /** Req 20.19: construction plus first call of every minimal generated cell against the Launch Parity Baseline. */
    private static Gate startupGate(View v) {
        List<String> pre = new ArrayList<>();
        Optional<String> baseline = v.baseline(pre);
        List<Check> checks = new ArrayList<>();
        for (BenchmarkCell cell : v.required()) {
            if (!isMinimalStartupCell(cell)) {
                continue;
            }
            Optional<ObjectNode> c = v.cell(cell.id());
            Optional<Ratio> r = baseline.flatMap(b -> c.flatMap(n -> metricEntry(
                n.expectArrayMember("comparisons"), Metric.STARTUP, b))).map(Ratio::fromNode);
            checks.add(statisticallyValid(cell.id(), c, baseline.orElse(null), r, STARTUP_VS_BASELINE,
                                          "no Launch Parity Baseline startup comparison for this cell"));
        }
        return gate(STARTUP, checks, pre);
    }

    /**
     * The cells whose client is the minimal generated client of Req 20.19: generated sync or async, finite, small,
     * success, concurrency 1 (one per protocol, path and transport).
     */
    static boolean isMinimalStartupCell(BenchmarkCell cell) {
        return !cell.path().isDynamic() && cell.kind() == WorkloadKind.FINITE && cell.payload() == PayloadClass.SMALL
               && cell.outcome() == Outcome.SUCCESS && cell.concurrency() == 1;
    }

    /** Req 20.16 / 20.17 / 20.18: every expected pair's candidate ratio. */
    private static Gate pairGate(String id, View v, String kind, Metric metric, double boundary) {
        List<Check> checks = new ArrayList<>();
        for (BenchmarkCell cell : v.required()) {
            Optional<BenchmarkCell> base = PerformanceReport.counterpart(kind, cell);
            if (base.isEmpty()) {
                continue;
            }
            String subject = cell.id() + " / " + base.get().id();
            ObjectNode.Builder inputs = ObjectNode.builder().withMember("kind", kind)
                                                  .withMember("baseline_cell", base.get().id())
                                                  .withMember("boundary", boundary);
            Optional<ObjectNode> pair = v.pair(kind, cell.id());
            if (pair.isEmpty()) {
                checks.add(new Check(subject, Status.NOT_EVALUABLE, inputs.build(),
                                     v.cell(cell.id()).isEmpty() || v.cell(base.get().id()).isEmpty()
                                     ? "a cell of the pair was not measured" : "no pair in the report"));
                continue;
            }
            boolean valid = pair.get().getBooleanMember("valid").map(b -> b.getValue()).orElse(false);
            Optional<Ratio> r = metricEntry(pair.get().expectArrayMember("ratios"), metric, null).map(Ratio::fromNode);
            r.ifPresent(x -> ratioInputs(inputs, x));
            inputs.withMember("valid", valid);
            if (!valid) {
                checks.add(new Check(subject, Status.NOT_EVALUABLE, inputs.build(), "a cell of the pair is invalid"));
            } else if (r.isEmpty()) {
                checks.add(new Check(subject, Status.NOT_EVALUABLE, inputs.build(), "no " + metric.id() + " ratio"));
            } else if (r.get().ratio() <= boundary) {
                checks.add(new Check(subject, Status.PASS, inputs.build(),
                                     fmt("ratio %.4f <= %.2f", r.get().ratio(), boundary)));
            } else {
                checks.add(new Check(subject, Status.FAIL, inputs.build(),
                                     fmt("ratio %.4f > %.2f", r.get().ratio(), boundary)));
            }
        }
        return gate(id, checks, List.of());
    }

    /** The Statistically Valid Result rule of one cell comparison. */
    private static Check statisticallyValid(String subject, Optional<ObjectNode> cell, String reference,
                                            Optional<Ratio> r, double boundary, String noRatio) {
        ObjectNode.Builder inputs = ObjectNode.builder().withMember("boundary", boundary);
        if (reference != null) {
            inputs.withMember("reference", reference);
        }
        if (cell.isEmpty()) {
            return new Check(subject, Status.NOT_EVALUABLE, inputs.build(), "the cell was not measured");
        }
        ObjectNode c = cell.get();
        r.ifPresent(x -> ratioInputs(inputs, x));
        if (!c.getBooleanMember("valid").map(b -> b.getValue()).orElse(false)) {
            return new Check(subject, Status.NOT_EVALUABLE, inputs.build(), "the cell is invalid ("
                             + String.join(",", strings(c.expectArrayMember("violations"))) + ")");
        }
        List<Node> nulls = c.expectArrayMember("null_experiments").getElements();
        if (nulls.isEmpty()) {
            return new Check(subject, Status.NOT_EVALUABLE, inputs.build(), "no same-artifact null experiment");
        }
        for (Node n : nulls) {
            String st = n.expectObjectNode().getStringMember("status").map(s -> s.getValue()).orElse("?");
            if (!CollectionValidity.NullExperiment.WITHIN.equals(st)) {
                return new Check(subject, Status.NOT_EVALUABLE, inputs.build(), "null experiment " + st);
            }
        }
        if (r.isEmpty() || reference == null) {
            return new Check(subject, Status.NOT_EVALUABLE, inputs.build(), noRatio);
        }
        for (String arm : List.of(PerformanceReport.CANDIDATE, reference)) {
            long valid = c.expectArrayMember("arms").getElements().stream().map(Node::expectObjectNode)
                          .filter(a -> arm.equals(a.getStringMember("arm").map(s -> s.getValue()).orElse(null)))
                          .mapToLong(a -> a.getNumberMember("valid_repetitions").map(x -> x.getValue().longValue())
                                           .orElse(0L)).max().orElse(0);
            if (valid < MIN_VALID_REPETITIONS) {
                return new Check(subject, Status.NOT_EVALUABLE, inputs.build(), "arm " + arm + " has " + valid
                                 + " valid repetitions; " + MIN_VALID_REPETITIONS + " required");
            }
        }
        Ratio x = r.get();
        if (!x.upperBoundAtMost(boundary)) {
            return new Check(subject, Status.FAIL, inputs.build(), fmt("95%% CI upper bound %.4f > %.2f (ratio %.4f)",
                                                                       x.ciHigh(), boundary, x.ratio()));
        }
        if (!(x.relativeHalfWidth() <= MAX_RELATIVE_HALF_WIDTH)) {
            return new Check(subject, Status.NOT_EVALUABLE, inputs.build(),
                             fmt("relative half-width %.4f > %.2f: not a Statistically Valid Result",
                                 x.relativeHalfWidth(), MAX_RELATIVE_HALF_WIDTH));
        }
        return new Check(subject, Status.PASS, inputs.build(),
                         fmt("95%% CI upper bound %.4f <= %.2f, relative half-width %.4f", x.ciHigh(), boundary,
                             x.relativeHalfWidth()));
    }

    /** Req 20.20 from its separately produced evidence ({@value #LARGE_STREAM_SCHEMA}). */
    private static Gate largeStreamGate(Optional<ObjectNode> evidence) {
        if (evidence.isEmpty()) {
            return gate(LARGE_STREAM, List.of(), List.of("no " + LARGE_STREAM_SCHEMA
                + " evidence: the 1 GiB / 256 MiB transfer is not a harness cell"));
        }
        ObjectNode e = evidence.get();
        if (!LARGE_STREAM_SCHEMA.equals(e.getStringMember("schema").map(s -> s.getValue()).orElse(""))) {
            return gate(LARGE_STREAM, List.of(), List.of("evidence is not " + LARGE_STREAM_SCHEMA));
        }
        List<Check> checks = new ArrayList<>();
        for (Node tn : e.getArrayMember("transfers").map(ArrayNode::getElements).orElse(List.of())) {
            ObjectNode t = tn.expectObjectNode();
            String subject = t.getStringMember("subject").map(s -> s.getValue()).orElse("?");
            long bytes = num(t, "transfer_bytes");
            long observed = num(t, "bytes_observed");
            long heap = num(t, "max_heap_bytes");
            long duration = num(t, "duration_ns");
            boolean completed = bool(t, "completed");
            // Absent is not proof of absence: fail closed.
            boolean outOfMemory = t.getBooleanMember("out_of_memory").map(b -> b.getValue()).orElse(true);
            boolean content = bool(t, "content_matches");
            List<String> problems = new ArrayList<>();
            if (bytes != LARGE_STREAM_BYTES) {
                problems.add("transfer_bytes " + bytes + " is not " + LARGE_STREAM_BYTES);
            }
            if (heap <= 0 || heap > LARGE_STREAM_MAX_HEAP_BYTES) {
                problems.add("max_heap_bytes " + heap + " is not in 1.." + LARGE_STREAM_MAX_HEAP_BYTES);
            }
            if (!completed) {
                problems.add("did not complete");
            }
            if (duration < 0 || duration > LARGE_STREAM_TIMEOUT.toNanos()) {
                problems.add("duration_ns " + duration + " exceeds the release-gate timeout "
                             + LARGE_STREAM_TIMEOUT.toNanos());
            }
            if (observed != bytes) {
                problems.add("bytes_observed " + observed + " != transfer_bytes " + bytes);
            }
            if (!content) {
                problems.add("byte values or order differ");
            }
            if (outOfMemory) {
                problems.add("out-of-memory error");
            }
            checks.add(new Check(subject, problems.isEmpty() ? Status.PASS : Status.FAIL, t,
                                 problems.isEmpty() ? "exact 1 GiB transfer within a 256 MiB heap and the timeout"
                                                    : String.join("; ", problems)));
        }
        return gate(LARGE_STREAM, checks, List.of());
    }

    /** Req 20.21 / 20.22 from the hot-path suite's evidence ({@value #HOT_PATH_SCHEMA}). */
    private static Gate hotPathGate(String id, Optional<ObjectNode> evidence) {
        if (evidence.isEmpty()) {
            return gate(id, List.of(), List.of("no " + HOT_PATH_SCHEMA + " evidence"));
        }
        ObjectNode e = evidence.get();
        if (!HOT_PATH_SCHEMA.equals(e.getStringMember("schema").map(s -> s.getValue()).orElse(""))) {
            return gate(id, List.of(), List.of("evidence is not " + HOT_PATH_SCHEMA));
        }
        List<Check> checks = new ArrayList<>();
        Set<String> paths = new LinkedHashSet<>();
        for (Node cn : e.getArrayMember("checks").map(ArrayNode::getElements).orElse(List.of())) {
            ObjectNode c = cn.expectObjectNode();
            if (!id.equals(c.getStringMember("gate").map(s -> s.getValue()).orElse(""))) {
                continue;
            }
            String subject = c.getStringMember("subject").map(s -> s.getValue()).orElse("?");
            c.getStringMember("path").ifPresent(p -> paths.add(p.getValue()));
            boolean passed = bool(c, "passed");
            checks.add(new Check(subject, passed ? Status.PASS : Status.FAIL, c,
                                 c.getStringMember("detail").map(s -> s.getValue()).orElse(passed ? "passed"
                                                                                                    : "failed")));
        }
        List<String> pre = new ArrayList<>();
        for (ClientPath p : List.of(ClientPath.GENERATED_SYNC, ClientPath.GENERATED_ASYNC)) {
            if (!paths.contains(p.id())) {
                pre.add("no " + p.id() + " check");
            }
        }
        return gate(id, checks, pre);
    }

    /** Req 20.24-20.29: the collection, every required cell's presence and validity, and the report's own form. */
    private static Gate collectionGate(Inputs in, List<String> schema) {
        List<Check> checks = new ArrayList<>();
        ObjectNode r = in.report();
        ObjectNode none = Node.objectNode();
        checks.add(schema.isEmpty() ? new Check("report-schema", Status.PASS, none, PerformanceReport.SCHEMA)
                                    : new Check("report-schema", Status.FAIL, none, String.join("; ", schema)));
        String mode = r.getStringMember("mode").map(s -> s.getValue()).orElse("?");
        checks.add(new Check("mode", HarnessMode.RELEASE.id().equals(mode) ? Status.PASS : Status.FAIL, none,
                             "mode " + mode));
        checks.add(boolCheck(r, "release_evidence", "preflight qualified the collection as release evidence"));
        checks.add(boolCheck(r, "collection_valid", "no collection-scoped violation (null ratios, schedule)"));
        String basis = r.getStringMember("ratio_basis").map(s -> s.getValue()).orElse("?");
        checks.add(new Check("ratio_basis", PerformanceReport.Basis.VALID.id().equals(basis) ? Status.PASS
                                                                                             : Status.FAIL, none,
                             "ratio basis " + basis));
        checks.add(boolCheck(r, "references_ready", "every Performance Reference ran"));
        if (in.validity().isEmpty()) {
            checks.add(new Check("validity.json", Status.FAIL, none, "no " + CollectionValidity.SCHEMA + " verdict"));
        } else {
            checks.add(validityConsistency(r, in.validity().get(), schema.isEmpty()));
        }
        if (schema.isEmpty()) {
            View v = new View(r, in.requiredCells());
            List<String> missing = new ArrayList<>();
            List<String> invalid = new ArrayList<>();
            for (String id : in.requiredCells()) {
                Optional<ObjectNode> c = v.cell(id);
                if (c.isEmpty()) {
                    missing.add(id);
                } else if (!c.get().getBooleanMember("valid").map(b -> b.getValue()).orElse(false)) {
                    invalid.add(id + " (" + String.join(",", strings(c.get().expectArrayMember("violations")))
                                + ")");
                }
            }
            checks.add(new Check("required-cells-present", missing.isEmpty() ? Status.PASS : Status.FAIL,
                                 ObjectNode.builder().withMember("required", in.requiredCells().size())
                                           .withMember("missing", ArrayNode.fromStrings(missing)).build(),
                                 missing.isEmpty() ? "every required cell ran"
                                                   : missing.size() + " required cells did not run"));
            checks.add(new Check("required-cells-valid", invalid.isEmpty() ? Status.PASS : Status.FAIL,
                                 ObjectNode.builder().withMember("invalid", ArrayNode.fromStrings(invalid)).build(),
                                 invalid.isEmpty() ? "every required cell that ran is valid"
                                                   : invalid.size() + " required cells are invalid"));
            if (in.requiredCells().isEmpty()) {
                checks.add(new Check("required-cells", Status.FAIL, none, "no required cell"));
            }
        }
        return gate(COLLECTION, checks, List.of());
    }

    /** {@code validity.json} must be the verdict the report was built from. */
    private static Check validityConsistency(ObjectNode report, ObjectNode validity, boolean reportReadable) {
        List<String> p = new ArrayList<>();
        if (!CollectionValidity.SCHEMA.equals(validity.getStringMember("schema").map(s -> s.getValue()).orElse(""))) {
            p.add("validity.json is not " + CollectionValidity.SCHEMA);
        } else if (reportReadable) {
            if (!Objects.equals(validity.getStringMember("mode").map(s -> s.getValue()).orElse(null),
                                report.expectStringMember("mode").getValue())) {
                p.add("mode differs from the report's");
            }
            if (validity.getBooleanMember("valid").map(b -> b.getValue()).orElse(false)
                != report.expectBooleanMember("collection_valid").getValue()) {
                p.add("collection validity differs from the report's");
            }
            if (validity.getBooleanMember("release_evidence").map(b -> b.getValue()).orElse(false)
                != report.expectBooleanMember("release_evidence").getValue()) {
                p.add("release evidence differs from the report's");
            }
            if (!validity.getBooleanMember("valid").map(b -> b.getValue()).orElse(false)) {
                p.add("the collection verdict is invalid: " + validity.getArrayMember("violations")
                    .map(a -> a.getElements().stream().map(x -> x.expectObjectNode().getStringMember("rule")
                                                                .map(s -> s.getValue()).orElse("?"))
                                .distinct().toList().toString()).orElse("[]"));
            }
            Map<String, Boolean> verdictCells = new LinkedHashMap<>();
            validity.getArrayMember("cells").ifPresent(a -> a.getElements().forEach(c -> verdictCells.put(
                c.expectObjectNode().expectStringMember("cell").getValue(),
                c.expectObjectNode().getBooleanMember("valid").map(b -> b.getValue()).orElse(false))));
            for (Node cn : report.expectArrayMember("cells").getElements()) {
                ObjectNode c = cn.expectObjectNode();
                String id = c.expectStringMember("cell").getValue();
                Boolean v = verdictCells.get(id);
                if (v == null) {
                    p.add(id + ": no verdict");
                } else if (v != c.expectBooleanMember("valid").getValue()) {
                    p.add(id + ": validity differs from the report's");
                }
            }
        }
        return new Check("validity.json", p.isEmpty() ? Status.PASS : Status.FAIL, Node.objectNode(),
                         p.isEmpty() ? "consistent valid verdict" : String.join("; ", p));
    }

    private static Check boolCheck(ObjectNode r, String member, String meaning) {
        boolean v = r.getBooleanMember(member).map(b -> b.getValue()).orElse(false);
        return new Check(member, v ? Status.PASS : Status.FAIL, Node.objectNode(), meaning + ": " + v);
    }

    // ---- report access ----

    /** Indexed read-only view of a schema-conforming report. */
    private static final class View {
        private final ObjectNode report;
        private final List<BenchmarkCell> required;
        private final Map<String, ObjectNode> cells = new LinkedHashMap<>();
        private final Map<String, ObjectNode> pairs = new LinkedHashMap<>();

        View(ObjectNode report, List<String> requiredCells) {
            this.report = report;
            this.required = requiredCells.stream().map(BenchmarkCell::parse).toList();
            report.expectArrayMember("cells").getElements().forEach(c -> cells.put(
                c.expectObjectNode().expectStringMember("cell").getValue(), c.expectObjectNode()));
            report.expectArrayMember("pairs").getElements().forEach(p -> {
                ObjectNode o = p.expectObjectNode();
                pairs.put(o.expectStringMember("kind").getValue() + "|" + o.expectStringMember("cell").getValue(), o);
            });
        }

        List<BenchmarkCell> required() {
            return required;
        }

        Optional<ObjectNode> cell(String id) {
            return Optional.ofNullable(cells.get(id));
        }

        Optional<ObjectNode> pair(String kind, String cellId) {
            return Optional.ofNullable(pairs.get(kind + "|" + cellId));
        }

        boolean referencesReady() {
            return report.expectBooleanMember("references_ready").getValue();
        }

        List<String> unreadyReferences() {
            return report.expectArrayMember("references").getElements().stream().map(Node::expectObjectNode)
                         .filter(r -> !"ready".equals(r.getStringMember("status").map(s -> s.getValue()).orElse("")))
                         .map(r -> r.getStringMember("id").map(s -> s.getValue()).orElse("?") + " "
                                   + r.getStringMember("status").map(s -> s.getValue()).orElse("?"))
                         .toList();
        }

        /** The ready Launch Parity Baseline's arm id that ran, or a reason in {@code pre}. */
        Optional<String> baseline(List<String> pre) {
            Optional<ObjectNode> ref = report.expectArrayMember("references").getElements().stream()
                .map(Node::expectObjectNode)
                .filter(r -> PerformanceReferences.Role.LAUNCH_PARITY_BASELINE.id()
                    .equals(r.getStringMember("role").map(s -> s.getValue()).orElse(""))).findFirst();
            if (ref.isEmpty()) {
                pre.add("no Launch Parity Baseline reference");
                return Optional.empty();
            }
            String id = ref.get().expectStringMember("id").getValue();
            String status = ref.get().getStringMember("status").map(s -> s.getValue()).orElse("?");
            if (!PerformanceReferences.Status.READY.id().equals(status)) {
                pre.add("Launch Parity Baseline " + id + " is " + status);
                return Optional.empty();
            }
            if (!strings(report.expectArrayMember("reference_arms")).contains(id)) {
                pre.add("Launch Parity Baseline " + id + " did not run");
                return Optional.empty();
            }
            return Optional.of(id);
        }

        Optional<ObjectNode> aggregate(String group, String reference, Metric metric) {
            return report.expectArrayMember("aggregates").getElements().stream().map(Node::expectObjectNode)
                         .filter(a -> group.equals(a.expectStringMember("group").getValue())
                                      && reference.equals(a.expectStringMember("reference").getValue())
                                      && metric.id().equals(a.expectStringMember("metric").getValue()))
                         .findFirst();
        }
    }

    /** The entry of {@code metric} (and {@code reference}, when given) in a ratio list. */
    private static Optional<ObjectNode> metricEntry(ArrayNode list, Metric metric, String reference) {
        return list.getElements().stream().map(Node::expectObjectNode)
                   .filter(e -> metric.id().equals(e.getStringMember("metric").map(s -> s.getValue()).orElse("")))
                   .filter(e -> reference == null
                                || reference.equals(e.getStringMember("reference").map(s -> s.getValue()).orElse("")))
                   .findFirst();
    }

    private static void ratioInputs(ObjectNode.Builder b, Ratio r) {
        b.withMember("ratio", BenchmarkStatistics.number(r.ratio()))
         .withMember("ci_low", BenchmarkStatistics.number(r.ciLow()))
         .withMember("ci_high", BenchmarkStatistics.number(r.ciHigh()))
         .withMember("relative_half_width", BenchmarkStatistics.number(r.relativeHalfWidth()))
         .withMember("ratio_cells", r.cells());
    }

    static List<String> strings(ArrayNode a) {
        return a.getElements().stream().map(n -> n.isStringNode() ? n.expectStringNode().getValue() : n.toString())
                .toList();
    }

    private static long num(ObjectNode n, String member) {
        return n.getNumberMember(member).map(x -> x.getValue().longValue()).orElse(-1L);
    }

    private static boolean bool(ObjectNode n, String member) {
        return n.getBooleanMember(member).map(b -> b.getValue()).orElse(false);
    }

    private static String fmt(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }
}
