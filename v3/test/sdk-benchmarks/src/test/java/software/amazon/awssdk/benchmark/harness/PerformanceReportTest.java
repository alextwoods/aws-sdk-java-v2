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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.benchmark.harness.RepetitionMetrics.Metric;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Metric extraction from synthetic records, the report's statistics against {@link BenchmarkStatistics} on the same
 * values, schema conformance and JSON round trip, reference resolution and fail-closed release, and per-arm cell
 * applicability in the schedule. The arm {@code ref} here is a synthetic statistical input, not a reference result.
 */
class PerformanceReportTest {
    private static final BenchmarkCell GEN =
        BenchmarkCell.parse("awsJson1_0/generated-sync/apache5-sync/finite/small/success/c1");
    private static final BenchmarkCell DYN =
        BenchmarkCell.parse("awsJson1_0/dynamic-sync/apache5-sync/finite/small/success/c1");

    static ObjectNode run(BenchmarkCell cell, String arm, String nullOf, int rep, long cpuPerOp, long allocated,
                          boolean valid) {
        long ops = 1000;
        ObjectNode.Builder a = ObjectNode.builder().withMember("id", arm).withMember("artifact_digest",
                                                                       "sha256:" + (nullOf == null ? arm : nullOf))
                                         .withMember("artifact_bytes", 5_000_000);
        if (nullOf != null) {
            a.withMember("null_of", nullOf);
        }
        return ObjectNode.builder()
            .withMember("index", rep).withMember("repetition", rep).withMember("status", "completed")
            .withMember("cell", HarnessOrchestrator.cellNode(cell)).withMember("arm", a.build())
            .withMember("result", ObjectNode.builder()
                .withMember("construction_ns", 40_000_000L + rep).withMember("first_call_ns", 60_000_000L)
                .withMember("loaded_classes_after_first_call", 3000)
                .withMember("process", ObjectNode.builder().withMember("max_heap_bytes", 134_217_728L).build())
                .withMember("measurement", ObjectNode.builder()
                    .withMember("operations", ops).withMember("wall_ns", 1_000_000_000L)
                    .withMember("process_cpu_ns", cpuPerOp * ops).withMember("allocated_bytes", allocated)
                    .withMember("p50_ns", 50_000 + rep).withMember("p99_ns", 90_000 + rep)
                    .withMember("loaded_classes", 3500).withMember("peak_heap_bytes", 60_000_000L).build())
                .build())
            .withMember("validity", ObjectNode.builder().withMember("valid", valid).build())
            .build();
    }

    @Test
    void extractsEveryPublishedMetric() {
        RepetitionMetrics m = RepetitionMetrics.of(run(GEN, "v3", null, 1, 20_000, 4_000_000, true)).orElseThrow();
        assertEquals(20_000, m.get(Metric.CPU_PER_OP).getAsDouble(), 1e-9);
        assertEquals(4000, m.get(Metric.ALLOC_PER_OP).getAsDouble(), 1e-9);
        assertEquals(1000, m.get(Metric.THROUGHPUT).getAsDouble(), 1e-9);
        assertEquals(50_001, m.get(Metric.P50).getAsDouble());
        assertEquals(90_001, m.get(Metric.P99).getAsDouble());
        assertEquals(100_000_001, m.get(Metric.STARTUP).getAsDouble());
        assertEquals(40_000_001, m.get(Metric.CONSTRUCTION).getAsDouble());
        assertEquals(60_000_000, m.get(Metric.FIRST_CALL).getAsDouble());
        assertEquals(3000, m.get(Metric.LOADED_CLASSES_AFTER_FIRST_CALL).getAsDouble());
        assertEquals(3500, m.get(Metric.LOADED_CLASSES).getAsDouble());
        assertEquals(5_000_000, m.get(Metric.ARTIFACT_BYTES).getAsDouble());
        assertEquals(60_000_000, m.get(Metric.PEAK_HEAP).getAsDouble());
        assertEquals(134_217_728, m.get(Metric.MAX_HEAP).getAsDouble());
        assertEquals(Metric.values().length, m.values().size());

        // Unavailable allocation accounting is absent, not zero.
        RepetitionMetrics noAlloc = RepetitionMetrics.of(run(GEN, "v3", null, 1, 20_000, -1, true)).orElseThrow();
        assertTrue(noAlloc.get(Metric.ALLOC_PER_OP).isEmpty());
        assertTrue(RepetitionMetrics.of(ObjectNode.builder().withMember("status", "timeout").build()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new RepetitionMetrics(
            java.util.Map.of(Metric.CPU_PER_OP, Double.NaN)));
    }

    private static List<ObjectNode> collection(boolean invalidV3Rep) {
        long[] v3Cpu = {20_000, 20_400, 19_800, 20_200, 19_900};
        long[] refCpu = {40_000, 41_000, 39_500, 40_500, 40_100};
        long[] slowRef = {60_000, 61_000, 59_000, 60_500, 60_100};
        List<ObjectNode> runs = new ArrayList<>();
        for (int rep = 1; rep <= 5; rep++) {
            boolean v3Valid = !(invalidV3Rep && rep == 3);
            runs.add(run(GEN, "v3", null, rep, v3Valid ? v3Cpu[rep - 1] : 1, 4_000_000, v3Valid));
            runs.add(run(GEN, "ref", null, rep, refCpu[rep - 1], 8_000_000, true));
            runs.add(run(GEN, "slow", null, rep, slowRef[rep - 1], 2_000_000, true));
            runs.add(run(GEN, "v3#null", "v3", rep, v3Cpu[rep - 1], 4_000_000, true));
            runs.add(run(DYN, "v3", null, rep, v3Cpu[rep - 1] + 4000, 4_400_000, true));
            runs.add(run(DYN, "v3#null", "v3", rep, v3Cpu[rep - 1] + 4000, 4_400_000, true));
        }
        return runs;
    }

    private static CollectionValidity.CollectionVerdict verdict(List<ObjectNode> runs) {
        List<CollectionValidity.RepetitionVerdict> reps = new ArrayList<>();
        for (ObjectNode r : runs) {
            boolean valid = r.expectObjectMember("validity").expectBooleanMember("valid").getValue();
            reps.add(CollectionValidity.RepetitionVerdict.of(r, valid ? List.of()
                : List.of(Violation.repetition(ValidityRules.FAILED_OPERATIONS, "synthetic"))));
        }
        return CollectionValidity.evaluate(HarnessMode.EXPLORATORY, false, reps, List.of());
    }

    private static double[] cpu(List<ObjectNode> runs, BenchmarkCell cell, String arm) {
        return runs.stream()
                   .filter(r -> r.expectObjectMember("cell").expectStringMember("id").getValue().equals(cell.id())
                                && r.expectObjectMember("arm").expectStringMember("id").getValue().equals(arm)
                                && r.expectObjectMember("validity").expectBooleanMember("valid").getValue())
                   .mapToDouble(r -> RepetitionMetrics.of(r).orElseThrow().get(Metric.CPU_PER_OP).getAsDouble())
                   .toArray();
    }

    private static ObjectNode find(ObjectNode parent, String array, String key, String value) {
        return parent.expectArrayMember(array).getElements().stream().map(Node::expectObjectNode)
                     .filter(o -> o.getStringMember(key).map(s -> s.getValue().equals(value)).orElse(false))
                     .findFirst().orElseThrow(() -> new AssertionError(array + " has no " + key + "=" + value));
    }

    @Test
    void reportStatisticsEqualTheStatisticsOfTheValidRepetitions() {
        List<ObjectNode> runs = collection(true);
        ObjectNode report = PerformanceReport.build(ObjectNode.builder().withMember("run_id", "t").build(), runs,
                                                    verdict(runs), PerformanceReferences.resolve(Path.of("/none")));
        assertEquals(List.of(), PerformanceReport.schemaProblems(report));
        assertEquals(List.of("ref", "slow"), report.expectArrayMember("reference_arms")
                                                   .getElementsAs(n -> n.expectStringNode().getValue()));
        assertFalse(report.expectBooleanMember("references_ready").getValue());

        ObjectNode gen = find(report, "cells", "cell", GEN.id());
        ObjectNode v3 = find(gen, "arms", "arm", "v3");
        assertEquals(4, v3.expectNumberMember("valid_repetitions").getValue().intValue());
        assertEquals(5, v3.expectArrayMember("repetitions").size());
        double[] v3Cpu = cpu(runs, GEN, "v3");
        assertEquals(4, v3Cpu.length);
        assertEquals(BenchmarkStatistics.summarize(v3Cpu).orElseThrow(), BenchmarkStatistics.Summary.fromNode(
            v3.expectObjectMember("summaries").expectObjectMember(Metric.CPU_PER_OP.id())));

        BenchmarkStatistics.Ratio expected =
            BenchmarkStatistics.ratio(v3Cpu, cpu(runs, GEN, "ref")).orElseThrow();
        ObjectNode cmp = gen.expectArrayMember("comparisons").getElements().stream().map(Node::expectObjectNode)
                            .filter(o -> o.expectStringMember("reference").getValue().equals("ref")
                                         && o.expectStringMember("metric").getValue().equals("cpu_per_op_ns"))
                            .findFirst().orElseThrow();
        assertEquals(expected, BenchmarkStatistics.Ratio.fromNode(cmp));
        // Best reference: lowest CPU is "ref", lowest allocation is "slow".
        ObjectNode bestCpu = find(gen, "best_reference", "metric", "cpu_per_op_ns");
        assertEquals("ref", bestCpu.expectStringMember("reference").getValue());
        assertEquals("slow", find(gen, "best_reference", "metric", "alloc_per_op_bytes")
            .expectStringMember("reference").getValue());
        assertEquals("ref", find(gen, "best_reference", "metric", "throughput_ops_per_s")
            .expectStringMember("reference").getValue());
        assertEquals("v3", gen.expectArrayMember("null_experiments").get(0).orElseThrow().expectObjectNode()
                              .expectStringMember("source").getValue());

        // The generated non-streaming aggregate over the one valid generated cell is that cell's ratio.
        ObjectNode agg = report.expectArrayMember("aggregates").getElements().stream().map(Node::expectObjectNode)
                               .filter(o -> o.expectStringMember("group").getValue()
                                             .equals(PerformanceReport.GENERATED_NON_STREAMING)
                                            && o.expectStringMember("reference").getValue().equals("ref")
                                            && o.expectStringMember("metric").getValue().equals("cpu_per_op_ns"))
                               .findFirst().orElseThrow();
        // The cell is invalid in a 5-repetition collection with one invalid V3 repetition: excluded, not averaged.
        assertFalse(gen.expectBooleanMember("valid").getValue());
        assertEquals(List.of(), agg.expectArrayMember("cells").getElements());
        assertTrue(agg.getObjectMember("geometric_mean").isEmpty());
        assertEquals(1, agg.expectArrayMember("excluded").size());

        // Report JSON round trip.
        assertEquals(report, Node.parse(Node.prettyPrintJson(report)));
    }

    @Test
    void validCellsAggregateAndPair() {
        List<ObjectNode> runs = collection(false);
        ObjectNode report = PerformanceReport.build(ObjectNode.builder().build(), runs, verdict(runs), List.of());
        assertEquals(List.of(), PerformanceReport.schemaProblems(report));
        ObjectNode agg = report.expectArrayMember("aggregates").getElements().stream().map(Node::expectObjectNode)
                               .filter(o -> o.expectStringMember("group").getValue()
                                             .equals(PerformanceReport.GENERATED_NON_STREAMING)
                                            && o.expectStringMember("reference").getValue().equals("ref")
                                            && o.expectStringMember("metric").getValue().equals("cpu_per_op_ns"))
                               .findFirst().orElseThrow();
        assertEquals(List.of(GEN.id()), agg.expectArrayMember("cells")
                                           .getElementsAs(n -> n.expectStringNode().getValue()));
        BenchmarkStatistics.Ratio cell = BenchmarkStatistics.ratio(cpu(runs, GEN, "v3"), cpu(runs, GEN, "ref"))
                                                            .orElseThrow();
        assertEquals(BenchmarkStatistics.geometricMean(List.of(cell)).orElseThrow(),
                     BenchmarkStatistics.Ratio.fromNode(agg.expectObjectMember("geometric_mean")));
        assertTrue(cell.ratio() < 0.55 && cell.ratio() > 0.45, "v3 uses about half the reference CPU");

        ObjectNode pair = find(report, "pairs", "kind", PerformanceReport.DYNAMIC_OVER_GENERATED);
        assertEquals(DYN.id(), pair.expectStringMember("cell").getValue());
        assertEquals(GEN.id(), pair.expectStringMember("baseline_cell").getValue());
        assertTrue(pair.expectBooleanMember("valid").getValue());
        ObjectNode pairCpu = find(pair, "ratios", "metric", "cpu_per_op_ns");
        assertEquals(BenchmarkStatistics.ratio(cpu(runs, DYN, "v3"), cpu(runs, GEN, "v3")).orElseThrow(),
                     BenchmarkStatistics.Ratio.fromNode(pairCpu));

        // Async cells pair with the same transport family's sync cell; Netty has no sync counterpart.
        assertEquals(Optional.of(GEN), PerformanceReport.counterpart(PerformanceReport.ASYNC_OVER_SYNC, BenchmarkCell
            .parse("awsJson1_0/generated-async/apache5-async/finite/small/success/c1")));
        assertTrue(PerformanceReport.counterpart(PerformanceReport.ASYNC_OVER_SYNC, BenchmarkCell
            .parse("awsJson1_0/generated-async/netty-async/finite/small/success/c1")).isEmpty());
    }

    @Test
    void schemaProblemsAreReported() {
        assertFalse(PerformanceReport.schemaProblems(ObjectNode.builder().withMember("schema", "x").build())
                                     .isEmpty());
        List<ObjectNode> runs = collection(false);
        ObjectNode report = PerformanceReport.build(ObjectNode.builder().build(), runs, verdict(runs), List.of());
        ObjectNode broken = report.withMember("aggregates", Node.fromNodes(ObjectNode.builder()
            .withMember("metric", "bogus").build()));
        assertEquals(List.of("aggregate: unknown metric bogus"), PerformanceReport.schemaProblems(broken));
    }

    @Test
    void referencesArePinnedAndFailClosedInRelease(@TempDir Path m2) throws Exception {
        List<PerformanceReferences.Reference> none = PerformanceReferences.resolve(m2);
        assertEquals(List.of("v2-baseline", "racecar", "smithy-java"),
                     none.stream().map(PerformanceReferences.Reference::id).toList());
        assertTrue(none.stream().allMatch(r -> r.status() == PerformanceReferences.Status.ARTIFACT_MISSING));
        assertThrows(HarnessPolicyException.class, () -> PerformanceReferences.requireReady(HarnessMode.RELEASE,
                                                                                              none));
        PerformanceReferences.requireReady(HarnessMode.EXPLORATORY, none);
        assertEquals(List.of(), PerformanceReferences.readyArms(none));

        for (String a : PerformanceReferences.V2_ARTIFACTS) {
            Path jar = m2.resolve("software/amazon/awssdk").resolve(a).resolve("2.54.0").resolve(a + "-2.54.0.jar");
            Files.createDirectories(jar.getParent());
            Files.writeString(jar, a);
        }
        PerformanceReferences.Reference v2 = PerformanceReferences.resolve(m2).get(0);
        assertEquals(PerformanceReferences.Status.CLIENT_MISSING, v2.status());
        assertTrue(v2.arm().isEmpty(), "no reference result without a reference client");
        assertEquals(PerformanceReferences.V2_ARTIFACTS.size(), v2.artifacts().size());
        assertTrue(v2.artifacts().stream().allMatch(x -> x.present() && x.sha256().length() == 64));
        assertTrue(v2.appliesTo(GEN));
        assertFalse(v2.appliesTo(DYN));
        assertThrows(HarnessPolicyException.class, () -> PerformanceReferences.requireReady(
            HarnessMode.RELEASE, PerformanceReferences.resolve(m2)));
    }

    /**
     * With its pinned jars and a built reference client module, the V2 baseline is READY with an arm on that module's
     * class path, for exactly its applicability; a V3 jar on that class path, or a missing client, is not ready; and a
     * release plan selecting a generated cell outside a reference's applicability fails closed.
     */
    @Test
    void v2BaselineIsReadyOnlyWithAnIsolatedBuiltClient(@TempDir Path m2, @TempDir Path root) throws Exception {
        for (String a : PerformanceReferences.V2_ARTIFACTS) {
            Path jar = m2.resolve("software/amazon/awssdk").resolve(a).resolve("2.54.0").resolve(a + "-2.54.0.jar");
            Files.createDirectories(jar.getParent());
            Files.writeString(jar, a);
        }
        Path target = root.resolve(PerformanceReferences.V2_REFERENCE_MODULE).resolve("target");
        Files.createDirectories(target.resolve("classes/software/amazon/awssdk/benchmark/harness"));
        Path main = target.resolve("test-classes")
                          .resolve(PerformanceReferences.V2_REFERENCE_MAIN.replace('.', '/') + ".class");
        Files.createDirectories(main.getParent());
        Files.writeString(main, "class");
        Path v2Jar = m2.resolve("software/amazon/awssdk/dynamodb/2.54.0/dynamodb-2.54.0.jar");
        Files.writeString(target.resolve("classpath.txt"), v2Jar.toString());

        PerformanceReferences.Reference v2 = PerformanceReferences.resolve(m2, root).get(0);
        assertEquals(PerformanceReferences.Status.READY, v2.status(), v2.reason());
        BenchmarkArm arm = v2.arm().orElseThrow();
        assertEquals("v2-baseline", arm.id());
        assertEquals(PerformanceReferences.V2_REFERENCE_MAIN, arm.mainClass());
        assertTrue(arm.classpath().endsWith(v2Jar.toString()), arm.classpath());
        assertEquals(List.of(arm), PerformanceReferences.readyArms(PerformanceReferences.resolve(m2, root)));
        assertTrue(v2.appliesTo(GEN));
        assertFalse(v2.appliesTo(DYN));
        assertFalse(v2.appliesTo(BenchmarkCell.parse("awsJson1_0/generated-async/apache5-async/finite/small/success/c1")));
        assertTrue(v2.appliesTo(BenchmarkCell.parse("awsJson1_0/generated-async/crt-async/finite/large/retry/c32")));
        assertFalse(v2.appliesTo(BenchmarkCell.parse("restJson1/generated-sync/apache5-sync/finite/small/success/c1")));
        assertTrue(v2.toNode().expectObjectMember("applicability").getArrayMember("transports").isPresent());

        // Uncovered generated cells: V2 has no apache5-async client; dynamic cells need no reference.
        BenchmarkCell asyncApache5 = BenchmarkCell.parse(
            "awsJson1_0/generated-async/apache5-async/finite/small/success/c1");
        List<PerformanceReferences.Reference> refs = PerformanceReferences.resolve(m2, root);
        assertTrue(PerformanceReferences.uncovered(refs, List.of(GEN, DYN, asyncApache5))
                                        .contains("v2-baseline: " + asyncApache5.id()));
        assertFalse(PerformanceReferences.uncovered(refs, List.of(DYN)).stream().anyMatch(s -> s.contains(DYN.id())));
        PerformanceReferences.requireReady(HarnessMode.EXPLORATORY, refs, List.of(asyncApache5));
        assertThrows(HarnessPolicyException.class,
                     () -> PerformanceReferences.requireReady(HarnessMode.RELEASE, refs, List.of(GEN)));

        // A V3 (non-2.54.0) SDK jar on the reference class path: not ready.
        Files.writeString(target.resolve("classpath.txt"),
                          v2Jar + java.io.File.pathSeparator + m2.resolve("software/amazon/awssdk/dynamodb/2.54.0")
                                                                 .resolve("../3.0.0-SNAPSHOT/dynamodb.jar"));
        Files.createDirectories(m2.resolve("software/amazon/awssdk/dynamodb/3.0.0-SNAPSHOT"));
        Files.writeString(m2.resolve("software/amazon/awssdk/dynamodb/3.0.0-SNAPSHOT/dynamodb.jar"), "v3");
        PerformanceReferences.Reference leaked = PerformanceReferences.resolve(m2, root).get(0);
        assertEquals(PerformanceReferences.Status.CLIENT_MISSING, leaked.status());
        assertTrue(leaked.arm().isEmpty());
        assertTrue(leaked.reason().contains("non-2.54.0"), leaked.reason());

        Files.delete(main);
        assertEquals(PerformanceReferences.Status.CLIENT_MISSING, PerformanceReferences.resolve(m2, root).get(0).status());
    }

    @Test
    void operationCorrectBasisIsExploratoryOnly() {
        List<ObjectNode> runs = collection(false);
        CollectionValidity.CollectionVerdict release = new CollectionValidity.CollectionVerdict(
            HarnessMode.RELEASE, false, verdict(runs).cells(), List.of());
        assertThrows(IllegalArgumentException.class, () -> PerformanceReport.build(
            ObjectNode.builder().build(), runs, release, List.of(), PerformanceReport.Basis.OPERATION_CORRECT));
        ObjectNode report = PerformanceReport.build(ObjectNode.builder().build(), runs, verdict(runs), List.of());
        assertEquals("valid-repetitions", report.expectStringMember("ratio_basis").getValue());
        ObjectNode forged = report.withMember("mode", "release").withMember("ratio_basis",
                                                                            "operation-correct-repetitions");
        assertEquals(List.of("a release report's ratio_basis must be valid-repetitions"),
                     PerformanceReport.schemaProblems(forged));
    }

    @Test
    void scheduleRunsOnlyApplicableArmsPerCell() {
        BenchmarkArm ref = new BenchmarkArm("ref", "/ref.jar", "ref.Main", null);
        List<BenchmarkArm> arms = List.of(BenchmarkArm.V3, ref, BenchmarkArm.V3.nullTwin());
        RepetitionSchedule s = RepetitionSchedule.interleaved(arms, 5, (arm, cell) -> arm != ref
                                                                                       || !cell.path().isDynamic());
        List<RepetitionSchedule.ScheduledRun> runs = s.schedule(List.of(GEN, DYN));
        assertEquals(5 * 3 + 5 * 2, runs.size());
        assertTrue(runs.stream().noneMatch(r -> r.cell().equals(DYN) && r.arm().equals(ref)));
        assertEquals(List.of(), RepetitionSchedule.problems(List.of(GEN, DYN), runs, HarnessMode.RELEASE));
    }
}
