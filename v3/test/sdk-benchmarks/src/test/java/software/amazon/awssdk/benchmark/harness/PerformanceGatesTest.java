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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.benchmark.harness.PerformanceGates.Decision.GA_ALLOWED;
import static software.amazon.awssdk.benchmark.harness.PerformanceGates.Decision.GA_BLOCKED;
import static software.amazon.awssdk.benchmark.harness.PerformanceGates.Status.FAIL;
import static software.amazon.awssdk.benchmark.harness.PerformanceGates.Status.NOT_EVALUABLE;
import static software.amazon.awssdk.benchmark.harness.PerformanceGates.Status.PASS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.benchmark.harness.PerformanceGates.Gate;
import software.amazon.awssdk.benchmark.harness.PerformanceGates.Inputs;
import software.amazon.awssdk.benchmark.harness.PerformanceGates.Result;
import software.amazon.awssdk.benchmark.harness.PerformanceGates.Status;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Every gate on synthetic {@value PerformanceReport#SCHEMA} reports: pass, fail, the confidence-interval borderline,
 * not-evaluable inputs and invalid collections; the release decision under exploratory and release modes; and the
 * {@value PerformanceGates#SCHEMA} round trip. The synthetic report is a minimal release matrix of three cells:
 * a generated sync cell, its generated async counterpart and its dynamic counterpart.
 */
class PerformanceGatesTest {

    static final String SYNC = "awsJson1_0/generated-sync/apache5-sync/finite/small/success/c1";
    static final String ASYNC = "awsJson1_0/generated-async/apache5-async/finite/small/success/c1";
    static final String DYN = "awsJson1_0/dynamic-sync/apache5-sync/finite/small/success/c1";
    static final List<String> REQUIRED = List.of(SYNC, ASYNC, DYN);
    static final String BASELINE = "v2-baseline";

    /** A mutable synthetic report: every value passes by default. */
    static final class Synthetic {
        String mode = "release";
        boolean releaseEvidence = true;
        boolean collectionValid = true;
        String basis = "valid-repetitions";
        Map<String, String> referenceStatus = new LinkedHashMap<>(Map.of(BASELINE, "ready", "racecar", "ready",
                                                                         "smithy-java", "ready"));
        List<String> cells = new ArrayList<>(REQUIRED);
        Map<String, Boolean> cellValid = new LinkedHashMap<>();
        Map<String, String> nullStatus = new LinkedHashMap<>();
        Map<String, Integer> validReps = new LinkedHashMap<>();
        double[] cpuAggregate = {0.5, 0.48, 0.52};
        double[] allocAggregate = {0.4, 0.38, 0.42};
        List<String> aggregateExcluded = new ArrayList<>();
        Map<String, double[]> bestCpu = new LinkedHashMap<>();
        Map<String, double[]> bestAlloc = new LinkedHashMap<>();
        Map<String, double[]> startup = new LinkedHashMap<>();
        double[] asyncCpu = {1.2, 1.1, 1.3};
        double[] dynCpu = {1.1, 1.0, 1.2};
        double[] dynAlloc = {1.1, 1.0, 1.2};
        boolean pairsValid = true;

        Synthetic() {
            for (String c : List.of(SYNC, ASYNC)) {
                bestCpu.put(c, new double[] {0.9, 0.88, 0.92});
                bestAlloc.put(c, new double[] {0.8, 0.78, 0.82});
                startup.put(c, new double[] {0.95, 0.93, 0.97});
            }
        }

        ObjectNode report() {
            ArrayNode.Builder cellNodes = ArrayNode.builder();
            for (String id : cells) {
                boolean valid = cellValid.getOrDefault(id, true);
                ArrayNode.Builder best = ArrayNode.builder();
                ArrayNode.Builder comparisons = ArrayNode.builder();
                if (bestCpu.containsKey(id)) {
                    best.withValue(ratio("cpu_per_op_ns", bestCpu.get(id)).withMember("reference", "racecar").build());
                }
                if (bestAlloc.containsKey(id)) {
                    best.withValue(ratio("alloc_per_op_bytes", bestAlloc.get(id)).withMember("reference", BASELINE)
                                                                                  .build());
                }
                if (startup.containsKey(id)) {
                    comparisons.withValue(ratio("startup_ns", startup.get(id)).withMember("reference", BASELINE)
                                                                               .build());
                }
                ArrayNode.Builder arms = ArrayNode.builder();
                for (String arm : List.of("v3", BASELINE, "racecar", "v3#null")) {
                    arms.withValue(ObjectNode.builder().withMember("arm", arm).withMember("valid", valid)
                                             .withMember("valid_repetitions", validReps.getOrDefault(id, 5))
                                             .withMember("repetitions", ArrayNode.builder().build())
                                             .withMember("summaries", Node.objectNode()).build());
                }
                cellNodes.withValue(ObjectNode.builder()
                    .withMember("cell", id).withMember("valid", valid)
                    .withMember("violations", valid ? ArrayNode.builder().build()
                                                    : ArrayNode.fromStrings(List.of("steady-state-gate")))
                    .withMember("arms", arms.build())
                    .withMember("null_experiments", ArrayNode.fromNodes(List.of(ObjectNode.builder()
                        .withMember("arm", "v3#null").withMember("source", "v3")
                        .withMember("status", nullStatus.getOrDefault(id, "within-bounds"))
                        .withMember("ratios", Node.objectNode()).build())))
                    .withMember("comparisons", comparisons.build())
                    .withMember("best_reference", best.build()).build());
            }
            ArrayNode.Builder pairs = ArrayNode.builder();
            if (cells.contains(ASYNC) && cells.contains(SYNC)) {
                pairs.withValue(pair(PerformanceReport.ASYNC_OVER_SYNC, ASYNC, SYNC,
                                     List.of(ratio("cpu_per_op_ns", asyncCpu).build())));
            }
            if (cells.contains(DYN) && cells.contains(SYNC)) {
                pairs.withValue(pair(PerformanceReport.DYNAMIC_OVER_GENERATED, DYN, SYNC,
                                     List.of(ratio("cpu_per_op_ns", dynCpu).build(),
                                             ratio("alloc_per_op_bytes", dynAlloc).build())));
            }
            ArrayNode.Builder aggregates = ArrayNode.builder();
            List<String> included = cells.stream().filter(c -> !c.equals(DYN))
                                         .filter(c -> !aggregateExcluded.contains(c)).toList();
            aggregates.withValue(aggregate("cpu_per_op_ns", cpuAggregate, included));
            aggregates.withValue(aggregate("alloc_per_op_bytes", allocAggregate, included));
            ArrayNode.Builder refs = ArrayNode.builder();
            Map<String, String> roles = Map.of(BASELINE, "launch-parity-baseline", "racecar", "racecar",
                                               "smithy-java", "smithy-java");
            List<String> ran = new ArrayList<>();
            referenceStatus.forEach((id, status) -> {
                refs.withValue(ObjectNode.builder().withMember("id", id).withMember("role", roles.get(id))
                                         .withMember("status", status).build());
                if (status.equals("ready") && !id.equals("smithy-java")) {
                    ran.add(id);
                }
            });
            return ObjectNode.builder()
                .withMember("schema", PerformanceReport.SCHEMA).withMember("run_id", "synthetic")
                .withMember("mode", mode).withMember("release_evidence", releaseEvidence)
                .withMember("collection_valid", collectionValid)
                .withMember("collection_violations", ArrayNode.builder().build())
                .withMember("candidate_arm", "v3").withMember("ratio_basis", basis)
                .withMember("reference_arms", ArrayNode.fromStrings(ran))
                .withMember("statistics", Node.objectNode()).withMember("metrics", ArrayNode.builder().build())
                .withMember("host", Node.objectNode()).withMember("config", Node.objectNode())
                .withMember("references", refs.build())
                .withMember("references_ready", referenceStatus.values().stream().allMatch("ready"::equals))
                .withMember("cells", cellNodes.build()).withMember("pairs", pairs.build())
                .withMember("aggregates", aggregates.build()).build();
        }

        ObjectNode validity() {
            ArrayNode.Builder c = ArrayNode.builder();
            for (String id : cells) {
                c.withValue(ObjectNode.builder().withMember("cell", id)
                                      .withMember("valid", cellValid.getOrDefault(id, true)).build());
            }
            return ObjectNode.builder().withMember("schema", CollectionValidity.SCHEMA).withMember("mode", mode)
                             .withMember("release_evidence", releaseEvidence).withMember("valid", collectionValid)
                             .withMember("violations", ArrayNode.builder().build()).withMember("cells", c.build())
                             .build();
        }

        private ObjectNode pair(String kind, String cell, String base, List<ObjectNode> ratios) {
            return ObjectNode.builder().withMember("kind", kind).withMember("cell", cell)
                             .withMember("baseline_cell", base).withMember("arm", "v3")
                             .withMember("valid", pairsValid)
                             .withMember("ratios", ArrayNode.fromNodes(new ArrayList<>(ratios))).build();
        }

        private ObjectNode aggregate(String metric, double[] r, List<String> included) {
            List<String> excluded = aggregateExcluded.stream().map(c -> c + ": invalid cell").toList();
            return ObjectNode.builder().withMember("group", PerformanceReport.GENERATED_NON_STREAMING)
                             .withMember("candidate", "v3").withMember("reference", BASELINE)
                             .withMember("metric", metric).withMember("cells", ArrayNode.fromStrings(included))
                             .withMember("excluded", ArrayNode.fromStrings(excluded))
                             .withMember("geometric_mean", ratioNode(r, included.size())).build();
        }
    }

    /** {ratio, ci_low, ci_high} as a report ratio entry. */
    static ObjectNode.Builder ratio(String metric, double[] r) {
        return ObjectNode.builder().withMember("metric", metric).build().merge(ratioNode(r, 1)).toBuilder();
    }

    static ObjectNode ratioNode(double[] r, int cells) {
        return new BenchmarkStatistics.Ratio(r[0], Math.log(r[0]), 0.01, 8, r[1], r[2], (r[2] - r[1]) / (2 * r[0]),
                                             cells).toNode();
    }

    static ObjectNode largeStream(Consumer<ObjectNode.Builder> edit) {
        ObjectNode.Builder t = ObjectNode.builder().withMember("subject", "sync-download")
                                         .withMember("transfer_bytes", 1L << 30).withMember("bytes_observed", 1L << 30)
                                         .withMember("max_heap_bytes", 256L << 20)
                                         .withMember("duration_ns", 20_000_000_000L).withMember("completed", true)
                                         .withMember("out_of_memory", false).withMember("content_matches", true);
        edit.accept(t);
        return ObjectNode.builder().withMember("schema", PerformanceGates.LARGE_STREAM_SCHEMA)
                         .withMember("transfers", ArrayNode.fromNodes(List.of(t.build()))).build();
    }

    static ObjectNode hotPath(boolean passed) {
        List<Node> checks = new ArrayList<>();
        for (String gate : List.of(PerformanceGates.INACTIVE_EXTENSIONS, PerformanceGates.HOT_PATH)) {
            for (String p : List.of("generated-sync", "generated-async")) {
                checks.add(ObjectNode.builder().withMember("gate", gate).withMember("path", p)
                                     .withMember("subject", "dynamodb " + p).withMember("passed", passed).build());
            }
        }
        return ObjectNode.builder().withMember("schema", PerformanceGates.HOT_PATH_SCHEMA)
                         .withMember("checks", ArrayNode.fromNodes(checks)).build();
    }

    static Result evaluate(Synthetic s) {
        return evaluate(s, Optional.of(largeStream(t -> { })), Optional.of(hotPath(true)));
    }

    static Result evaluate(Synthetic s, Optional<ObjectNode> large, Optional<ObjectNode> hot) {
        ObjectNode report = s.report();
        assertEquals(List.of(), PerformanceReport.schemaProblems(report), "the synthetic report conforms");
        return PerformanceGates.evaluate(new Inputs(report, Optional.of(s.validity()), REQUIRED, large, hot));
    }

    static Status status(Result r, String gate) {
        return r.gate(gate).status();
    }

    @Test
    void everyGatePassingAllowsGeneralAvailabilityInRelease() {
        Result r = evaluate(new Synthetic());
        for (Gate g : r.gates()) {
            assertEquals(PASS, g.status(), g.id() + " " + g.reasons() + " " + g.checks());
        }
        assertEquals(PerformanceGates.GATES, r.gates().stream().map(Gate::id).toList());
        assertEquals(GA_ALLOWED, r.decision());
        assertEquals(List.of(), r.reasons());
    }

    @Test
    void exploratoryNeverAllowsGeneralAvailability() {
        Synthetic s = new Synthetic();
        s.mode = "exploratory";
        s.releaseEvidence = false;
        Result r = evaluate(s);
        assertEquals(PASS, status(r, PerformanceGates.CPU_AGGREGATE), "measured gates are still reported");
        assertEquals(FAIL, status(r, PerformanceGates.COLLECTION));
        assertEquals(GA_BLOCKED, r.decision());
        assertTrue(r.reasons().get(0).contains("not release evidence"), r.reasons().toString());
    }

    @Test
    void releaseModeWithoutPreflightEvidenceIsBlocked() {
        Synthetic s = new Synthetic();
        s.releaseEvidence = false;
        Result r = evaluate(s);
        assertEquals(FAIL, status(r, PerformanceGates.COLLECTION));
        assertEquals(GA_BLOCKED, r.decision());
    }

    @Test
    void launchParityAggregatesUseThePointEstimateAtTheBoundary() {
        Synthetic s = new Synthetic();
        s.cpuAggregate = new double[] {0.70, 0.60, 0.80};
        s.allocAggregate = new double[] {0.60, 0.50, 0.70};
        Result r = evaluate(s);
        assertEquals(PASS, status(r, PerformanceGates.CPU_AGGREGATE));
        assertEquals(PASS, status(r, PerformanceGates.ALLOC_AGGREGATE));

        s.cpuAggregate = new double[] {0.7001, 0.69, 0.71};
        s.allocAggregate = new double[] {0.6001, 0.59, 0.61};
        r = evaluate(s);
        assertEquals(FAIL, status(r, PerformanceGates.CPU_AGGREGATE));
        assertEquals(FAIL, status(r, PerformanceGates.ALLOC_AGGREGATE));
        assertEquals(GA_BLOCKED, r.decision());
    }

    @Test
    void anAggregateOmittingARequiredCellIsNotEvaluable() {
        Synthetic s = new Synthetic();
        s.aggregateExcluded.add(ASYNC);
        Result r = evaluate(s);
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.CPU_AGGREGATE));
        assertEquals(GA_BLOCKED, r.decision());
    }

    @Test
    void bestReferenceNeedsTheUpperConfidenceBoundAtMostTenPercent() {
        Synthetic s = new Synthetic();
        s.bestCpu.put(SYNC, new double[] {1.08, 1.06, 1.10});
        assertEquals(PASS, status(evaluate(s), PerformanceGates.CPU_BEST_REFERENCE), "ci_high == 1.10 passes");

        s.bestCpu.put(SYNC, new double[] {1.05, 1.0, 1.1001});
        assertEquals(FAIL, status(evaluate(s), PerformanceGates.CPU_BEST_REFERENCE),
                     "a point estimate under the boundary with an interval crossing it fails");

        s.bestCpu.put(SYNC, new double[] {0.95, 0.9, 1.04});
        Result r = evaluate(s);
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.CPU_BEST_REFERENCE),
                     "a 7% relative half-width is not a Statistically Valid Result");
        assertEquals(GA_BLOCKED, r.decision());

        s = new Synthetic();
        s.bestAlloc.put(ASYNC, new double[] {1.2, 1.15, 1.25});
        assertEquals(FAIL, status(evaluate(s), PerformanceGates.ALLOC_BEST_REFERENCE));
    }

    @Test
    void statisticallyValidChecksNeedAValidCellNullExperimentAndRepetitions() {
        Synthetic s = new Synthetic();
        s.nullStatus.put(SYNC, "out-of-bounds");
        assertEquals(NOT_EVALUABLE, status(evaluate(s), PerformanceGates.CPU_BEST_REFERENCE));

        s = new Synthetic();
        s.validReps.put(ASYNC, 4);
        assertEquals(NOT_EVALUABLE, status(evaluate(s), PerformanceGates.STARTUP));

        s = new Synthetic();
        s.cellValid.put(SYNC, false);
        Result r = evaluate(s);
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.ALLOC_BEST_REFERENCE));
        assertEquals(FAIL, status(r, PerformanceGates.COLLECTION), "an invalid required cell blocks (Req 20.29)");
        assertEquals(GA_BLOCKED, r.decision());
    }

    @Test
    void anUnreadyReferenceMakesBestReferenceGatesNotEvaluable() {
        Synthetic s = new Synthetic();
        s.referenceStatus.put("smithy-java", "client-missing");
        Result r = evaluate(s);
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.CPU_BEST_REFERENCE));
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.ALLOC_BEST_REFERENCE));
        assertEquals(PASS, status(r, PerformanceGates.CPU_AGGREGATE), "the baseline aggregate is still judged");
        assertEquals(FAIL, status(r, PerformanceGates.COLLECTION));
        assertEquals(GA_BLOCKED, r.decision());
    }

    @Test
    void anUnreadyBaselineMakesBaselineGatesNotEvaluable() {
        Synthetic s = new Synthetic();
        s.referenceStatus.put(BASELINE, "artifact-missing");
        Result r = evaluate(s);
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.CPU_AGGREGATE));
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.ALLOC_AGGREGATE));
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.STARTUP));
    }

    @Test
    void startupNeedsTheUpperConfidenceBoundAtMostFivePercent() {
        Synthetic s = new Synthetic();
        s.startup.put(SYNC, new double[] {1.03, 1.01, 1.05});
        assertEquals(PASS, status(evaluate(s), PerformanceGates.STARTUP));
        s.startup.put(SYNC, new double[] {1.03, 1.01, 1.0501});
        assertEquals(FAIL, status(evaluate(s), PerformanceGates.STARTUP));
    }

    @Test
    void asyncAndDynamicPairsUseThePointEstimate() {
        Synthetic s = new Synthetic();
        s.asyncCpu = new double[] {1.35, 1.30, 1.40};
        s.dynCpu = new double[] {1.25, 1.20, 1.30};
        s.dynAlloc = new double[] {1.25, 1.20, 1.30};
        Result r = evaluate(s);
        assertEquals(PASS, status(r, PerformanceGates.ASYNC_CPU));
        assertEquals(PASS, status(r, PerformanceGates.DYNAMIC_CPU));
        assertEquals(PASS, status(r, PerformanceGates.DYNAMIC_ALLOC));

        s.asyncCpu = new double[] {1.3501, 1.30, 1.40};
        s.dynCpu = new double[] {1.2501, 1.20, 1.30};
        s.dynAlloc = new double[] {1.2501, 1.20, 1.30};
        r = evaluate(s);
        assertEquals(FAIL, status(r, PerformanceGates.ASYNC_CPU));
        assertEquals(FAIL, status(r, PerformanceGates.DYNAMIC_CPU));
        assertEquals(FAIL, status(r, PerformanceGates.DYNAMIC_ALLOC));
        assertEquals(GA_BLOCKED, r.decision());

        s = new Synthetic();
        s.pairsValid = false;
        assertEquals(NOT_EVALUABLE, status(evaluate(s), PerformanceGates.ASYNC_CPU));
    }

    @Test
    void aMissingRequiredCellFailsTheCollectionAndLeavesItsChecksNotEvaluable() {
        Synthetic s = new Synthetic();
        s.cells.remove(DYN);
        Result r = evaluate(s);
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.DYNAMIC_CPU));
        assertEquals(FAIL, status(r, PerformanceGates.COLLECTION));
        assertEquals(GA_BLOCKED, r.decision());
    }

    @Test
    void invalidCollectionsBlockGeneralAvailability() {
        Synthetic s = new Synthetic();
        s.collectionValid = false;
        assertEquals(GA_BLOCKED, evaluate(s).decision());

        s = new Synthetic();
        s.basis = "operation-correct-repetitions";
        s.mode = "exploratory";
        assertEquals(FAIL, evaluate(s).gate(PerformanceGates.COLLECTION).checks().stream()
                                      .filter(c -> c.subject().equals("ratio_basis")).findFirst().orElseThrow()
                                      .status());

        s = new Synthetic();
        Result noVerdict = PerformanceGates.evaluate(new Inputs(s.report(), Optional.empty(), REQUIRED,
                                                                Optional.of(largeStream(t -> { })),
                                                                Optional.of(hotPath(true))));
        assertEquals(FAIL, status(noVerdict, PerformanceGates.COLLECTION), "validity.json is required");

        ObjectNode inconsistent = s.validity().withMember("valid", false);
        Result r = PerformanceGates.evaluate(new Inputs(s.report(), Optional.of(inconsistent), REQUIRED,
                                                        Optional.of(largeStream(t -> { })),
                                                        Optional.of(hotPath(true))));
        assertEquals(FAIL, status(r, PerformanceGates.COLLECTION), "the verdict must match the report");

        Result malformed = PerformanceGates.evaluate(new Inputs(Node.objectNode().withMember("schema", "x"),
                                                                Optional.empty(), REQUIRED, Optional.empty(),
                                                                Optional.empty()));
        assertEquals(GA_BLOCKED, malformed.decision());
        assertEquals(FAIL, status(malformed, PerformanceGates.COLLECTION));
        malformed.gates().forEach(g -> assertTrue(g.status() != PASS, g.id()));
    }

    @Test
    void absentEvidenceIsNotEvaluable() {
        Result r = evaluate(new Synthetic(), Optional.empty(), Optional.empty());
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.LARGE_STREAM));
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.INACTIVE_EXTENSIONS));
        assertEquals(NOT_EVALUABLE, status(r, PerformanceGates.HOT_PATH));
        assertEquals(GA_BLOCKED, r.decision());

        assertEquals(FAIL, status(evaluate(new Synthetic(), Optional.of(largeStream(t -> { })),
                                           Optional.of(hotPath(false))), PerformanceGates.HOT_PATH));
    }

    @Test
    void largeStreamEvidenceMustBeExactBoundedAndTimely() {
        List<Consumer<ObjectNode.Builder>> failures = List.of(
            t -> t.withMember("transfer_bytes", (1L << 30) - 1).withMember("bytes_observed", (1L << 30) - 1),
            t -> t.withMember("bytes_observed", (1L << 30) - 1),
            t -> t.withMember("max_heap_bytes", (256L << 20) + 1),
            t -> t.withMember("duration_ns", PerformanceGates.LARGE_STREAM_TIMEOUT.toNanos() + 1),
            t -> t.withMember("completed", false),
            t -> t.withMember("content_matches", false),
            t -> t.withMember("out_of_memory", true),
            t -> t.withoutMember("out_of_memory"));
        for (Consumer<ObjectNode.Builder> f : failures) {
            Result r = evaluate(new Synthetic(), Optional.of(largeStream(f)), Optional.of(hotPath(true)));
            assertEquals(FAIL, status(r, PerformanceGates.LARGE_STREAM), r.gate(PerformanceGates.LARGE_STREAM)
                                                                          .checks().toString());
        }
        Result atTimeout = evaluate(new Synthetic(), Optional.of(largeStream(
            t -> t.withMember("duration_ns", PerformanceGates.LARGE_STREAM_TIMEOUT.toNanos()))),
                                    Optional.of(hotPath(true)));
        assertEquals(PASS, status(atTimeout, PerformanceGates.LARGE_STREAM));
    }

    @Test
    void gatesJsonRoundTrips(@TempDir Path dir) throws Exception {
        Synthetic s = new Synthetic();
        s.bestCpu.put(SYNC, new double[] {1.2, 1.1, 1.3});
        Result r = evaluate(s, Optional.empty(), Optional.of(hotPath(true)));
        ObjectNode node = r.toNode();
        assertEquals(PerformanceGates.SCHEMA, node.expectStringMember("schema").getValue());
        assertEquals(node, Result.fromNode(node).toNode());
        assertEquals(node, Node.parse(Node.prettyPrintJson(node)));
        assertEquals("ga-blocked", node.expectObjectMember("release_decision").expectStringMember("decision")
                                       .getValue());

        // The directory entry point reads report.json, validity.json and the evidence files, and writes gates.json.
        Files.writeString(dir.resolve("report.json"), Node.prettyPrintJson(s.report()));
        Files.writeString(dir.resolve("validity.json"), Node.prettyPrintJson(s.validity()));
        Files.writeString(dir.resolve("hot-path.json"), Node.prettyPrintJson(hotPath(true)));
        Result fromDir = PerformanceGatesSink.evaluateDirectory(dir, Optional.empty(), Optional.empty());
        assertEquals(Node.parse(Files.readString(dir.resolve("gates.json"))), fromDir.toNode());
        assertEquals(FAIL, status(fromDir, PerformanceGates.CPU_BEST_REFERENCE));
        assertEquals(NOT_EVALUABLE, status(fromDir, PerformanceGates.LARGE_STREAM));
        // A release report's required cells are the whole matrix, so this three-cell report is incomplete.
        assertEquals(FAIL, status(fromDir, PerformanceGates.COLLECTION));
        assertEquals(GA_BLOCKED, fromDir.decision());
    }

    @Test
    void releaseRequiresTheFullMatrix() {
        assertEquals(CellMatrix.full().size(), PerformanceGates.releaseRequiredCells().size());
        Synthetic s = new Synthetic();
        assertEquals(PerformanceGates.releaseRequiredCells(), PerformanceGates.requiredCellsFor(s.report()));
        s.mode = "exploratory";
        assertEquals(REQUIRED, PerformanceGates.requiredCellsFor(s.report()));
    }
}
