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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.benchmark.harness.BenchmarkStatistics.Ratio;
import software.amazon.awssdk.benchmark.harness.BenchmarkStatistics.Summary;
import software.amazon.awssdk.benchmark.harness.RepetitionMetrics.Metric;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The machine-readable performance report ({@value #SCHEMA}): a pure function of the plan, the validity-annotated run
 * records, the collection verdict and the resolved references. It publishes everything the release gates (19.4) read
 * and decides nothing itself:
 * <ul>
 *     <li>{@code cells[]}: per cell its validity, and per arm every repetition's {@link RepetitionMetrics} and validity
 *     plus a {@link Summary} of each metric over the valid repetitions; the cell's null-experiment ratios (from the
 *     verdict); {@code comparisons[]}: the candidate ({@value #CANDIDATE}) over each reference arm per metric
 *     ({@link Ratio}); and {@code best_reference[]}: per metric, the candidate over the best valid reference arm (lowest
 *     mean cost, highest mean throughput; Req 20.14, 20.15).</li>
 *     <li>{@code pairs[]}: candidate-arm ratios between corresponding cells: {@code async-over-sync} (same protocol,
 *     finite workload, payload, outcome, concurrency and transport family; Req 20.16) and
 *     {@code dynamic-over-generated} (same everything but the path's generated/dynamic kind; Req 20.17, 20.18).</li>
 *     <li>{@code aggregates[]}: equal-weight geometric means of the per-cell ratios over the valid cells of a group
 *     (Req 20.11): {@code generated-non-streaming} per reference arm and metric, and each pair kind; with the included
 *     cells and the excluded ones and why.</li>
 * </ul>
 * Only valid repetitions enter summaries and ratios ({@link Basis#VALID}); an exploratory report may opt into
 * {@link Basis#OPERATION_CORRECT}, recorded as {@code ratio_basis}. Statistics are those of {@link BenchmarkStatistics}.
 */
public final class PerformanceReport {

    public static final String SCHEMA = "v3-benchmark-report/1";
    public static final String CANDIDATE = "v3";
    public static final String GENERATED_NON_STREAMING = "generated-non-streaming";
    public static final String ASYNC_OVER_SYNC = "async-over-sync";
    public static final String DYNAMIC_OVER_GENERATED = "dynamic-over-generated";

    private PerformanceReport() {
    }

    /**
     * Which repetitions enter summaries, ratios and aggregates. {@link #VALID} (the default, and the only basis of a
     * release report): valid repetitions only. {@link #OPERATION_CORRECT} (exploratory reports only, never evidence):
     * every completed repetition whose operations were correct, i.e. without a violation of
     * {@link #OPERATION_RULES} (run status, failed operations, operation count, outcome, unmatched requests, wire bytes,
     * transport), whatever its host-policy and sample-quality violations; so a development host that cannot pin CPUs
     * still yields comparison ratios of correctly measured runs, labelled as such.
     */
    public enum Basis {
        VALID("valid-repetitions"),
        OPERATION_CORRECT("operation-correct-repetitions");

        private final String id;

        Basis(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /** The validity rules a repetition must pass to be measured correctly at all (basis {@link Basis#OPERATION_CORRECT}). */
    public static final List<String> OPERATION_RULES = List.of(
        ValidityRules.RUN_STATUS, ValidityRules.FAILED_OPERATIONS, ValidityRules.OPERATION_COUNT,
        ValidityRules.UNEXPECTED_OUTCOME, ValidityRules.UNMATCHED_REQUESTS, ValidityRules.WIRE_BYTES,
        ValidityRules.TRANSPORT);

    /**
     * One arm's repetitions on one cell.
     *
     * @param included per repetition, whether it enters statistics under the report's {@link Basis}
     */
    record ArmData(String armId, String nullOf, List<Integer> repetitions, List<Boolean> valid,
                   List<Boolean> included, List<RepetitionMetrics> metrics) {

        double[] values(Metric m) {
            List<Double> v = new ArrayList<>();
            for (int i = 0; i < metrics.size(); i++) {
                if (included.get(i)) {
                    metrics.get(i).get(m).ifPresent(v::add);
                }
            }
            return v.stream().mapToDouble(Double::doubleValue).toArray();
        }
    }

    /** One cell's arms. */
    record CellData(BenchmarkCell cell, Map<String, ArmData> arms) {
    }

    /** Builds the report on {@link Basis#VALID}. */
    public static ObjectNode build(ObjectNode plan, List<ObjectNode> runs, CollectionValidity.CollectionVerdict verdict,
                                   List<PerformanceReferences.Reference> references) {
        return build(plan, runs, verdict, references, Basis.VALID);
    }

    /**
     * Builds the report on {@code basis}.
     *
     * @throws IllegalArgumentException for {@link Basis#OPERATION_CORRECT} outside an exploratory collection
     */
    public static ObjectNode build(ObjectNode plan, List<ObjectNode> runs, CollectionValidity.CollectionVerdict verdict,
                                   List<PerformanceReferences.Reference> references, Basis basis) {
        if (basis != Basis.VALID && verdict.mode() != HarnessMode.EXPLORATORY) {
            throw new IllegalArgumentException(basis.id() + " statistics are exploratory only");
        }
        boolean strict = basis == Basis.VALID;
        Map<String, CellData> cells = group(runs, basis);
        Map<String, CollectionValidity.CellVerdict> cellVerdicts = new LinkedHashMap<>();
        verdict.cells().forEach(c -> cellVerdicts.put(c.cellId(), c));
        List<String> referenceArms = new ArrayList<>();
        cells.values().forEach(c -> c.arms().values().forEach(a -> {
            if (a.nullOf() == null && !a.armId().equals(CANDIDATE) && !referenceArms.contains(a.armId())) {
                referenceArms.add(a.armId());
            }
        }));

        ArrayNode.Builder cellNodes = ArrayNode.builder();
        // aggregate key -> per-cell ratios, included cell ids, excluded "cell: reason"
        Map<String, List<Ratio>> aggRatios = new LinkedHashMap<>();
        Map<String, List<String>> aggIncluded = new LinkedHashMap<>();
        Map<String, List<String>> aggExcluded = new LinkedHashMap<>();
        for (CellData c : cells.values()) {
            CollectionValidity.CellVerdict cv = cellVerdicts.get(c.cell().id());
            boolean valid = !strict || cv != null && cv.valid();
            cellNodes.withValue(cellNode(c, cv, referenceArms, strict));
            if (!isGeneratedNonStreaming(c.cell())) {
                continue;
            }
            for (String ref : referenceArms) {
                for (Metric m : Metric.values()) {
                    String key = GENERATED_NON_STREAMING + "|" + ref + "|" + m.id();
                    Optional<Ratio> r = cellRatio(c, CANDIDATE, ref, m);
                    collect(key, c.cell().id(), valid, cv, r, aggRatios, aggIncluded, aggExcluded);
                }
            }
        }

        ArrayNode.Builder pairNodes = ArrayNode.builder();
        for (CellData c : cells.values()) {
            for (String kind : List.of(ASYNC_OVER_SYNC, DYNAMIC_OVER_GENERATED)) {
                Optional<BenchmarkCell> base = counterpart(kind, c.cell());
                if (base.isEmpty() || !cells.containsKey(base.get().id())) {
                    continue;
                }
                CellData b = cells.get(base.get().id());
                ArmData cand = c.arms().get(CANDIDATE);
                ArmData baseArm = b.arms().get(CANDIDATE);
                if (cand == null || baseArm == null) {
                    continue;
                }
                CollectionValidity.CellVerdict cv = cellVerdicts.get(c.cell().id());
                CollectionValidity.CellVerdict bv = cellVerdicts.get(b.cell().id());
                boolean valid = !strict || cv != null && cv.valid() && bv != null && bv.valid();
                ArrayNode.Builder ratios = ArrayNode.builder();
                for (Metric m : List.of(Metric.CPU_PER_OP, Metric.ALLOC_PER_OP, Metric.P50, Metric.P99,
                                        Metric.THROUGHPUT)) {
                    Optional<Ratio> r = BenchmarkStatistics.ratio(cand.values(m), baseArm.values(m));
                    r.ifPresent(x -> ratios.withValue(withMetric(m, x)));
                    String key = kind + "|" + CANDIDATE + "|" + m.id();
                    String pairId = c.cell().id() + " / " + b.cell().id();
                    collect(key, pairId, valid, valid ? null : (cv != null && !cv.valid() ? cv : bv), r, aggRatios,
                            aggIncluded, aggExcluded);
                }
                pairNodes.withValue(ObjectNode.builder().withMember("kind", kind)
                                              .withMember("cell", c.cell().id())
                                              .withMember("baseline_cell", b.cell().id())
                                              .withMember("arm", CANDIDATE).withMember("valid", valid)
                                              .withMember("ratios", ratios.build()).build());
            }
        }

        ArrayNode.Builder aggregates = ArrayNode.builder();
        for (String key : aggIncluded.keySet()) {
            String[] parts = key.split("\\|");
            ObjectNode.Builder a = ObjectNode.builder().withMember("group", parts[0]).withMember("candidate",
                                                                                                   CANDIDATE)
                                             .withMember("reference", parts[1]).withMember("metric", parts[2])
                                             .withMember("cells", ArrayNode.fromStrings(aggIncluded.get(key)))
                                             .withMember("excluded", ArrayNode.fromStrings(aggExcluded.get(key)));
            BenchmarkStatistics.geometricMean(aggRatios.get(key)).ifPresent(g -> a.withMember("geometric_mean",
                                                                                             g.toNode()));
            aggregates.withValue(a.build());
        }

        ObjectNode verdictNode = verdict.toNode();
        return ObjectNode.builder()
            .withMember("schema", SCHEMA)
            .withMember("run_id", plan.getStringMember("run_id").map(s -> s.getValue()).orElse(""))
            .withMember("mode", verdict.mode().id())
            .withMember("release_evidence", verdict.releaseEvidence())
            .withMember("collection_valid", verdict.valid())
            .withMember("collection_violations", verdictNode.expectArrayMember("violations"))
            .withMember("candidate_arm", CANDIDATE)
            .withMember("ratio_basis", basis.id())
            .withMember("reference_arms", ArrayNode.fromStrings(referenceArms))
            .withMember("statistics", ObjectNode.builder()
                .withMember("confidence", BenchmarkStatistics.CONFIDENCE)
                .withMember("summary", "arithmetic mean of valid repetitions; Student-t interval, n-1 df")
                .withMember("ratio", "ratio of geometric means; Welch t interval on log values")
                .withMember("aggregate", "equal-weight geometric mean of per-cell ratios; Welch-Satterthwaite df")
                .build())
            .withMember("metrics", ArrayNode.fromNodes(java.util.Arrays.stream(Metric.values()).map(m ->
                (Node) ObjectNode.builder().withMember("id", m.id()).withMember("lower_is_better", m.lowerIsBetter())
                                 .withMember("steady_state", m.steadyState()).build()).toList()))
            .withMember("host", plan.getObjectMember("host").orElse(Node.objectNode()))
            .withMember("config", plan.getObjectMember("config").orElse(Node.objectNode()))
            .withMember("references", ArrayNode.fromNodes(references.stream().map(r -> (Node) r.toNode()).toList()))
            .withMember("references_ready", references.stream()
                .allMatch(r -> r.status() == PerformanceReferences.Status.READY))
            .withMember("cells", cellNodes.build())
            .withMember("pairs", pairNodes.build())
            .withMember("aggregates", aggregates.build())
            .build();
    }

    private static void collect(String key, String id, boolean valid, CollectionValidity.CellVerdict cv,
                                Optional<Ratio> r, Map<String, List<Ratio>> ratios, Map<String, List<String>> included,
                                Map<String, List<String>> excluded) {
        ratios.computeIfAbsent(key, k -> new ArrayList<>());
        included.computeIfAbsent(key, k -> new ArrayList<>());
        excluded.computeIfAbsent(key, k -> new ArrayList<>());
        if (!valid) {
            String why = cv == null ? "no verdict" : cv.violations().stream().map(Violation::rule).distinct()
                                                       .reduce((a, b) -> a + "," + b).orElse("invalid");
            excluded.get(key).add(id + ": invalid cell (" + why + ")");
        } else if (r.isEmpty()) {
            excluded.get(key).add(id + ": no ratio (fewer than two positive valid values in an arm)");
        } else {
            ratios.get(key).add(r.get());
            included.get(key).add(id);
        }
    }

    private static ObjectNode cellNode(CellData c, CollectionValidity.CellVerdict cv, List<String> referenceArms,
                                       boolean strict) {
        ArrayNode.Builder arms = ArrayNode.builder();
        for (ArmData a : c.arms().values()) {
            ArrayNode.Builder reps = ArrayNode.builder();
            for (int i = 0; i < a.metrics().size(); i++) {
                reps.withValue(ObjectNode.builder().withMember("repetition", a.repetitions().get(i))
                                         .withMember("valid", a.valid().get(i))
                                         .withMember("metrics", a.metrics().get(i).toNode()).build());
            }
            ObjectNode.Builder summaries = ObjectNode.builder();
            for (Metric m : Metric.values()) {
                BenchmarkStatistics.summarize(a.values(m)).ifPresent(s -> summaries.withMember(m.id(), s.toNode()));
            }
            Optional<CollectionValidity.ArmVerdict> av = cv == null ? Optional.empty()
                : cv.arms().stream().filter(x -> x.armId().equals(a.armId())).findFirst();
            ObjectNode.Builder an = ObjectNode.builder().withMember("arm", a.armId())
                                              .withMember("valid", av.map(CollectionValidity.ArmVerdict::valid)
                                                                     .orElse(false))
                                              .withMember("valid_repetitions",
                                                          a.valid().stream().filter(v -> v).count())
                                              .withMember("repetitions", reps.build())
                                              .withMember("summaries", summaries.build());
            if (a.nullOf() != null) {
                an.withMember("null_of", a.nullOf());
            }
            arms.withValue(an.build());
        }
        ArrayNode.Builder comparisons = ArrayNode.builder();
        ArrayNode.Builder best = ArrayNode.builder();
        if (c.arms().containsKey(CANDIDATE)) {
            for (Metric m : Metric.values()) {
                String bestArm = null;
                double bestMean = 0;
                for (String ref : referenceArms) {
                    ArmData ra = c.arms().get(ref);
                    if (ra == null) {
                        continue;
                    }
                    Optional<Ratio> r = cellRatio(c, CANDIDATE, ref, m);
                    r.ifPresent(x -> comparisons.withValue(withMetric(m, x).toBuilder()
                                                                           .withMember("reference", ref).build()));
                    boolean refValid = !strict || cv != null && cv.arms().stream()
                                                                   .anyMatch(x -> x.armId().equals(ref) && x.valid());
                    double[] v = ra.values(m);
                    if (refValid && r.isPresent() && v.length > 0) {
                        double mean = BenchmarkStatistics.mean(v);
                        if (bestArm == null || (m.lowerIsBetter() ? mean < bestMean : mean > bestMean)) {
                            bestArm = ref;
                            bestMean = mean;
                        }
                    }
                }
                if (bestArm != null) {
                    best.withValue(withMetric(m, cellRatio(c, CANDIDATE, bestArm, m).orElseThrow()).toBuilder()
                                       .withMember("reference", bestArm).build());
                }
            }
        }
        ArrayNode.Builder nulls = ArrayNode.builder();
        if (cv != null) {
            for (CollectionValidity.NullExperiment n : cv.nullExperiments()) {
                ObjectNode.Builder ratios = ObjectNode.builder();
                n.ratios().forEach(ratios::withMember);
                nulls.withValue(ObjectNode.builder().withMember("arm", n.nullArm()).withMember("source", n.sourceArm())
                                          .withMember("status", n.status()).withMember("ratios", ratios.build())
                                          .build());
            }
        }
        return HarnessOrchestrator.cellNode(c.cell()).toBuilder()
                                  .withMember("cell", c.cell().id())
                                  .withMember("valid", cv != null && cv.valid())
                                  .withMember("violations", ArrayNode.fromStrings(cv == null ? List.of("no-verdict")
                                      : cv.violations().stream().map(Violation::rule).distinct().toList()))
                                  .withMember("generated_non_streaming", isGeneratedNonStreaming(c.cell()))
                                  .withMember("arms", arms.build())
                                  .withMember("null_experiments", nulls.build())
                                  .withMember("comparisons", comparisons.build())
                                  .withMember("best_reference", best.build())
                                  .build();
    }

    private static ObjectNode withMetric(Metric m, Ratio r) {
        return ObjectNode.builder().withMember("metric", m.id()).build().merge(r.toNode());
    }

    static Optional<Ratio> cellRatio(CellData c, String candidate, String reference, Metric m) {
        ArmData a = c.arms().get(candidate);
        ArmData b = c.arms().get(reference);
        if (a == null || b == null) {
            return Optional.empty();
        }
        return BenchmarkStatistics.ratio(a.values(m), b.values(m));
    }

    /** Generated (sync or async) finite cells: the Req 20.11-20.13 aggregation. */
    static boolean isGeneratedNonStreaming(BenchmarkCell cell) {
        return !cell.path().isDynamic() && cell.kind() == WorkloadKind.FINITE;
    }

    /** The baseline cell a pair kind compares {@code cell} with, if it has one. */
    static Optional<BenchmarkCell> counterpart(String kind, BenchmarkCell cell) {
        if (cell.kind() != WorkloadKind.FINITE) {
            return Optional.empty();
        }
        ClientPath path;
        Transport transport = cell.transport();
        if (kind.equals(ASYNC_OVER_SYNC)) {
            if (cell.path() != ClientPath.GENERATED_ASYNC) {
                return Optional.empty();
            }
            path = ClientPath.GENERATED_SYNC;
            transport = switch (cell.transport()) {
                case APACHE5_ASYNC -> Transport.APACHE5_SYNC;
                case CRT_ASYNC -> Transport.CRT_SYNC;
                default -> null;
            };
            if (transport == null) {
                return Optional.empty();
            }
        } else if (kind.equals(DYNAMIC_OVER_GENERATED)) {
            path = switch (cell.path()) {
                case DYNAMIC_SYNC -> ClientPath.GENERATED_SYNC;
                case DYNAMIC_ASYNC -> ClientPath.GENERATED_ASYNC;
                default -> null;
            };
            if (path == null) {
                return Optional.empty();
            }
        } else {
            throw new IllegalArgumentException("unknown pair kind " + kind);
        }
        return Optional.of(new BenchmarkCell(cell.protocol(), path, transport, cell.kind(), cell.payload(),
                                             cell.outcome(), cell.concurrency()));
    }

    private static Map<String, CellData> group(List<ObjectNode> runs, Basis basis) {
        Map<String, CellData> cells = new LinkedHashMap<>();
        for (ObjectNode run : runs) {
            Optional<RepetitionMetrics> metrics = RepetitionMetrics.of(run);
            String id = run.expectObjectMember("cell").expectStringMember("id").getValue();
            ObjectNode arm = run.getObjectMember("arm").orElse(Node.objectNode());
            String armId = arm.getStringMember("id").map(s -> s.getValue()).orElse(CANDIDATE);
            String nullOf = arm.getStringMember("null_of").map(s -> s.getValue()).orElse(null);
            boolean valid = run.getObjectMember("validity").flatMap(v -> v.getBooleanMember("valid"))
                               .map(b -> b.getValue()).orElse(false);
            boolean correct = "completed".equals(run.getStringMember("status").map(s -> s.getValue()).orElse(""))
                              && run.getObjectMember("validity").flatMap(v -> v.getArrayMember("violations"))
                                    .map(vs -> vs.getElements().stream().noneMatch(x -> OPERATION_RULES.contains(
                                        x.expectObjectNode().getStringMember("rule").map(s -> s.getValue())
                                         .orElse(""))))
                                    .orElse(false);
            CellData c = cells.computeIfAbsent(id, k -> new CellData(BenchmarkCell.parse(k), new LinkedHashMap<>()));
            ArmData a = c.arms().computeIfAbsent(armId, k -> new ArmData(k, nullOf, new ArrayList<>(),
                                                                          new ArrayList<>(), new ArrayList<>(),
                                                                          new ArrayList<>()));
            a.repetitions().add(run.getNumberMember("repetition").map(n -> n.getValue().intValue()).orElse(0));
            a.valid().add(valid && metrics.isPresent());
            a.included().add(metrics.isPresent() && (basis == Basis.VALID ? valid : correct));
            a.metrics().add(metrics.orElse(new RepetitionMetrics(Map.of())));
        }
        return cells;
    }

    /** Structural problems of a report node; empty when it conforms to {@value #SCHEMA}. */
    public static List<String> schemaProblems(ObjectNode report) {
        List<String> p = new ArrayList<>();
        if (!report.getStringMember("schema").map(s -> s.getValue().equals(SCHEMA)).orElse(false)) {
            p.add("schema is not " + SCHEMA);
        }
        for (String m : List.of("run_id", "mode", "candidate_arm", "ratio_basis")) {
            if (report.getStringMember(m).isEmpty()) {
                p.add("missing string " + m);
            }
        }
        for (String m : List.of("release_evidence", "collection_valid", "references_ready")) {
            if (report.getBooleanMember(m).isEmpty()) {
                p.add("missing boolean " + m);
            }
        }
        for (String m : List.of("host", "config", "statistics")) {
            if (report.getObjectMember(m).isEmpty()) {
                p.add("missing object " + m);
            }
        }
        for (String m : List.of("collection_violations", "reference_arms", "metrics", "references", "cells", "pairs",
                                "aggregates")) {
            if (report.getArrayMember(m).isEmpty()) {
                p.add("missing array " + m);
            }
        }
        if (!p.isEmpty()) {
            return p;
        }
        if (HarnessMode.RELEASE.id().equals(report.expectStringMember("mode").getValue())
            && !Basis.VALID.id().equals(report.expectStringMember("ratio_basis").getValue())) {
            p.add("a release report's ratio_basis must be " + Basis.VALID.id());
        }
        for (Node cn : report.expectArrayMember("cells").getElements()) {
            ObjectNode c = cn.expectObjectNode();
            String id = c.getStringMember("cell").map(s -> s.getValue()).orElse("?");
            for (String m : List.of("arms", "null_experiments", "comparisons", "best_reference", "violations")) {
                if (c.getArrayMember(m).isEmpty()) {
                    p.add(id + ": missing array " + m);
                }
            }
            if (c.getBooleanMember("valid").isEmpty()) {
                p.add(id + ": missing boolean valid");
            }
            c.getArrayMember("arms").ifPresent(arms -> arms.getElements().forEach(an -> {
                ObjectNode a = an.expectObjectNode();
                a.getObjectMember("summaries").ifPresentOrElse(s -> s.getMembers().forEach((k, v) -> {
                    checkMetric(p, id, k.getValue());
                    tryParse(p, id + " " + k.getValue(), () -> Summary.fromNode(v.expectObjectNode()));
                }), () -> p.add(id + ": arm without summaries"));
            }));
            for (String m : List.of("comparisons", "best_reference")) {
                c.getArrayMember(m).ifPresent(arr -> arr.getElements().forEach(r -> {
                    checkMetric(p, id, r.expectObjectNode().getStringMember("metric").map(s -> s.getValue())
                                        .orElse("?"));
                    tryParse(p, id + " " + m, () -> Ratio.fromNode(r.expectObjectNode()));
                }));
            }
        }
        for (Node an : report.expectArrayMember("aggregates").getElements()) {
            ObjectNode a = an.expectObjectNode();
            checkMetric(p, "aggregate", a.getStringMember("metric").map(s -> s.getValue()).orElse("?"));
            a.getObjectMember("geometric_mean").ifPresent(g -> tryParse(p, "aggregate", () -> Ratio.fromNode(g)));
        }
        return p;
    }

    private static void checkMetric(List<String> p, String where, String id) {
        try {
            Metric.fromId(id);
        } catch (IllegalArgumentException e) {
            p.add(where + ": unknown metric " + id);
        }
    }

    private static void tryParse(List<String> p, String where, Runnable parse) {
        try {
            parse.run();
        } catch (RuntimeException e) {
            p.add(where + ": " + e.getMessage());
        }
    }
}
