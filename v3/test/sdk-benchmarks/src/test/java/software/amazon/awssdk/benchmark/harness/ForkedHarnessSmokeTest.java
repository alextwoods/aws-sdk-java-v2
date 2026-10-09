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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * End to end: forks one mock-service JVM and one client JVM for a single small cell, measures for half a second, and
 * checks the run record, the client's result record and the manifest. A second, uncovered cell checks that it is
 * recorded rather than silently dropped.
 */
class ForkedHarnessSmokeTest {
    private static final BenchmarkCell CELL =
        BenchmarkCell.parse("awsJson1_0/generated-sync/apache5-sync/finite/small/success/c1");
    /**
     * A well-formed cell no provider serves. Every matrix cell has a fixture ({@link CellMatrixTest}), so this is an
     * awsJson1.0 Event Stream cell, outside the matrix because no fleet model binds an event stream on awsJson1.0.
     */
    static final BenchmarkCell UNCOVERED =
        BenchmarkCell.parse("awsJson1_0/generated-async/netty-async/eventstream/-/success/c1");

    @Test
    void forksClientAndMockServiceAndProducesAResultRecord(@TempDir Path out) throws Exception {
        HarnessConfig config = new HarnessConfig(
            HarnessMode.EXPLORATORY, HarnessConfig.currentJava(),
            List.of("-Xms128m", "-Xmx128m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1"),
            System.getProperty("java.class.path"), Duration.ofMillis(200), Duration.ofMillis(500),
            Duration.ofSeconds(60), out, List.of(), List.of(), "unmanaged", Map.of());
        List<ObjectNode> records = new ArrayList<>();
        ObjectNode manifest = new HarnessOrchestrator(config, SystemProbe.live())
            .run(List.of(CELL, UNCOVERED), RepetitionSchedule.once(), records::add);

        assertEquals(2, records.size());
        ObjectNode run = records.get(0);
        assertEquals("completed", run.expectStringMember("status").getValue(), Node.prettyPrintJson(run));
        assertEquals(CELL.id(), run.expectObjectMember("cell").expectStringMember("id").getValue());
        assertFalse(run.expectBooleanMember("release_evidence").getValue());

        ObjectNode client = run.expectObjectMember("client");
        ObjectNode service = run.expectObjectMember("mock_service");
        long clientPid = client.expectNumberMember("pid").getValue().longValue();
        long servicePid = service.expectNumberMember("pid").getValue().longValue();
        assertNotEquals(clientPid, servicePid);
        assertNotEquals(ProcessHandle.current().pid(), clientPid);
        assertEquals(0, client.expectNumberMember("exit_code").getValue().intValue());
        assertEquals(Long.toString(servicePid), service.expectObjectMember("ready").expectStringMember("pid").getValue());
        for (ObjectNode process : List.of(client, service)) {
            ObjectNode pinning = process.expectObjectMember("pinning");
            if (!pinning.expectBooleanMember("applied").getValue()) {
                assertFalse(pinning.expectStringMember("reason").getValue().isBlank());
            }
        }

        ObjectNode result = run.expectObjectMember("result");
        assertEquals(ClientMain.SCHEMA, result.expectStringMember("schema").getValue());
        assertEquals(clientPid, result.expectObjectMember("process").expectNumberMember("pid").getValue().longValue());
        assertTrue(result.expectObjectMember("process").expectStringMember("jvm_input_arguments").getValue()
                         .contains("-Xmx128m"));
        assertEquals("software.amazon.awssdk.http.apache5.Apache5HttpClient",
                     result.expectStringMember("transport_class").getValue());
        ObjectNode m = result.expectObjectMember("measurement");
        long ops = m.expectNumberMember("operations").getValue().longValue();
        assertTrue(ops > 0, Node.prettyPrintJson(result));
        assertEquals(0, m.expectNumberMember("failed_operations").getValue().longValue());
        assertTrue(m.expectNumberMember("process_cpu_ns").getValue().longValue() > 0);
        assertTrue(m.expectNumberMember("p99_ns").getValue().longValue()
                   >= m.expectNumberMember("p50_ns").getValue().longValue());
        ObjectNode served = result.expectObjectMember("service");
        assertEquals(ops, served.expectNumberMember("requests").getValue().longValue());
        assertEquals(0, served.expectNumberMember("unmatched").getValue().longValue());
        long perOp = (served.expectNumberMember("bytes_in").getValue().longValue()
                      + served.expectNumberMember("bytes_out").getValue().longValue()) / ops;
        assertEquals(result.expectNumberMember("expected_encoded_bytes_per_op").getValue().longValue(), perOp);
        assertTrue(PayloadClass.SMALL.contains(perOp));
        assertTrue(Files.exists(Path.of(run.expectStringMember("result_file").getValue())));

        ObjectNode none = records.get(1);
        assertEquals("no-fixture", none.expectStringMember("status").getValue());
        assertFalse(none.getMember("client").isPresent());

        ObjectNode onDisk = Node.parse(Files.readString(out.resolve("manifest.json"), StandardCharsets.UTF_8))
                                .expectObjectNode();
        assertEquals(manifest, onDisk);
        ObjectNode host = manifest.expectObjectMember("host");
        for (String key : HostMetadata.REQUIRED_KEYS) {
            assertTrue(host.getMember(key).isPresent(), key);
        }
        assertEquals(List.of(UNCOVERED.id()), manifest.expectArrayMember("missing_fixtures")
                                                      .getElementsAs(n -> n.expectStringNode().getValue()));
    }

    /**
     * A 2-arm (V3 and its same-artifact null twin), 2-repetition exploratory schedule on one tiny cell, with the
     * steady-state gate at a smoke-sized sampling geometry (100 ms intervals, 3-interval window, 2.5 s timeout) and
     * 0.6 s of measurement, through {@link ValiditySink}. Checks the interleaved order, that every run is annotated,
     * that no operation, sample or drift rule fails, that both arms ran the same artifact, and that the cell is
     * invalid for having 2 < 5 repetitions. Whether the gate passes in 2.5 s on this host is recorded, not asserted.
     */
    @Test
    void interleavedNullScheduleIsValidatedEndToEnd(@TempDir Path out) throws Exception {
        SteadyStatePolicy smoke = new SteadyStatePolicy(Duration.ofMillis(100), 3, Duration.ofMillis(2500));
        List<String> flags = List.of("-Xms128m", "-Xmx128m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1");
        HarnessConfig config = new HarnessConfig(
            HarnessMode.EXPLORATORY, HarnessConfig.currentJava(), flags, System.getProperty("java.class.path"),
            WarmupPolicy.steadyState(smoke), Duration.ofMillis(600), Duration.ofSeconds(60), out, List.of(),
            List.of(), "unmanaged", Map.of());
        List<ObjectNode> records = new ArrayList<>();
        List<PerformanceReferences.Reference> references = PerformanceReferences.resolveLocal();
        PerformanceReportSink.Chain chain = PerformanceReportSink.chain(out.resolve("validity.json"),
                                                                        out.resolve("report.json"), references,
                                                                        records::add);
        ValiditySink sink = chain.validity();
        ObjectNode manifest = new HarnessOrchestrator(config, SystemProbe.live())
            .run(List.of(CELL), RepetitionSchedule.withNullExperiment(List.of(BenchmarkArm.V3), 2), sink);

        assertEquals(List.of("v3/r1/", "v3#null/r1/", "v3#null/r2/", "v3/r2/"),
                     records.stream().map(r -> r.expectStringMember("label").getValue().replace(CELL.id(), ""))
                            .toList());
        assertEquals(List.of(), manifest.expectObjectMember("schedule").expectArrayMember("problems").getElements());
        List<String> mustPass = List.of(ValidityRules.RUN_STATUS, ValidityRules.FAILED_OPERATIONS,
                                        ValidityRules.OPERATION_COUNT, ValidityRules.UNEXPECTED_OUTCOME,
                                        ValidityRules.UNMATCHED_REQUESTS, ValidityRules.WIRE_BYTES,
                                        ValidityRules.TRANSPORT, ValidityRules.INCOMPLETE_SAMPLE,
                                        ValidityRules.HOST_DRIFT);
        java.util.Set<String> digests = new java.util.HashSet<>();
        for (ObjectNode run : records) {
            String json = Node.prettyPrintJson(run);
            assertEquals("completed", run.expectStringMember("status").getValue(), json);
            List<String> failed = run.expectObjectMember("validity").expectArrayMember("violations")
                                     .getElementsAs(n -> n.expectObjectNode().expectStringMember("rule").getValue());
            for (String rule : mustPass) {
                assertFalse(failed.contains(rule), rule + " failed: " + json);
            }
            ObjectNode warmup = run.expectObjectMember("result").expectObjectMember("warmup");
            assertEquals("steady-state", warmup.expectStringMember("policy").getValue());
            if (!warmup.expectBooleanMember("passed").getValue()) {
                assertTrue(failed.contains(ValidityRules.STEADY_STATE_GATE), json);
            }
            assertTrue(run.expectObjectMember("result").expectObjectMember("measurement")
                          .expectArrayMember("samples").size() >= 7, json);
            assertTrue(run.expectObjectMember("host").getMember("jdk.version").isPresent());
            digests.add(run.expectObjectMember("arm").expectStringMember("artifact_digest").getValue());
        }
        assertEquals(1, digests.size(), "the null arm runs the same artifact");

        CollectionValidity.CollectionVerdict verdict = sink.verdict();
        assertFalse(verdict.releaseEvidence());
        assertEquals(1, verdict.cells().size());
        CollectionValidity.CellVerdict cell = verdict.cells().get(0);
        assertFalse(cell.valid());
        assertTrue(cell.violations().stream()
                       .anyMatch(v -> v.rule().equals(CollectionValidity.TOO_FEW_VALID_REPETITIONS)));
        assertEquals(List.of("v3", "v3#null"), cell.arms().stream().map(CollectionValidity.ArmVerdict::armId).toList());
        assertEquals(1, cell.nullExperiments().size());
        assertEquals("v3", cell.nullExperiments().get(0).sourceArm());
        assertEquals(verdict.toNode(), Node.parse(Files.readString(out.resolve("validity.json"))));

        // The report: schema-conformant, on disk as returned, every repetition's metrics published, and no reference
        // comparison because no reference arm ran (none is fabricated); Racecar has no pinned build, so the references
        // are not all ready.
        ObjectNode report = chain.report().report();
        assertEquals(List.of(), PerformanceReport.schemaProblems(report));
        assertEquals(report, Node.parse(Files.readString(out.resolve("report.json"))));
        assertFalse(report.expectBooleanMember("references_ready").getValue());
        assertEquals(List.of("v2-baseline", "racecar", "smithy-java"), report.expectArrayMember("references")
            .getElementsAs(n -> n.expectObjectNode().expectStringMember("id").getValue()));
        ObjectNode reportCell = report.expectArrayMember("cells").get(0).orElseThrow().expectObjectNode();
        assertEquals(List.of(), reportCell.expectArrayMember("comparisons").getElements());
        assertEquals(List.of("v3", "v3#null"), reportCell.expectArrayMember("arms")
            .getElementsAs(n -> n.expectObjectNode().expectStringMember("arm").getValue()));
        for (Node armNode : reportCell.expectArrayMember("arms").getElements()) {
            for (Node rep : armNode.expectObjectNode().expectArrayMember("repetitions").getElements()) {
                ObjectNode metrics = rep.expectObjectNode().expectObjectMember("metrics");
                for (RepetitionMetrics.Metric m : RepetitionMetrics.Metric.values()) {
                    assertTrue(metrics.getNumberMember(m.id()).isPresent(), m.id() + " in " + metrics);
                }
                assertEquals(128L * 1024 * 1024, metrics.expectNumberMember("max_heap_bytes").getValue().longValue(),
                             (double) 16 * 1024 * 1024);
            }
        }
    }

    /**
     * V3 against the Launch Parity Baseline on the tiny awsJson1.0 cell: the V2 2.54.0 reference arm (built by
     * test/sdk-benchmarks-v2-reference) runs on its own V2-only class path, interleaved with V3 and V3's null twin, 2
     * repetitions each at the smoke geometry. Checks that the reference is ready exactly for its cells, that its runs
     * pass every operation rule (V2 sends the fixture's exact bytes: wire-bytes and unmatched-request checks
     * unchanged), that no V3 jar is on its class path, and that the exploratory report, on the operation-correct
     * basis (a development host cannot pin CPUs, so no repetition is valid), carries {@code comparisons[]} and
     * {@code best_reference[]} entries with finite, positive ratios. Exploratory numbers, never evidence.
     */
    @Test
    void v3IsComparedWithTheV2BaselineReference(@TempDir Path out) throws Exception {
        List<PerformanceReferences.Reference> references = PerformanceReferences.resolveLocal();
        PerformanceReferences.Reference v2 = references.get(0);
        assertEquals("v2-baseline", v2.id());
        assertEquals(PerformanceReferences.Status.READY, v2.status(),
                     "build test/sdk-benchmarks-v2-reference first: " + v2.reason());
        assertTrue(v2.appliesTo(CELL));
        assertFalse(v2.appliesTo(BenchmarkCell.parse("awsJson1_0/dynamic-sync/apache5-sync/finite/small/success/c1")));
        assertFalse(v2.appliesTo(BenchmarkCell.parse(
            "awsJson1_0/generated-async/apache5-async/finite/small/success/c1")));
        assertFalse(v2.appliesTo(BenchmarkCell.parse("restJson1/generated-sync/apache5-sync/finite/small/success/c1")));
        BenchmarkArm v2Arm = v2.arm().orElseThrow();
        for (String entry : v2Arm.classpath().split(java.io.File.pathSeparator)) {
            assertFalse(entry.contains("3.0.0-SNAPSHOT") || entry.contains("/v3/core/")
                        || entry.contains("/v3/http-clients/"), "V3 entry on the V2 class path: " + entry);
        }
        // Release collections stay fail-closed: Racecar and Smithy Java are not ready.
        org.junit.jupiter.api.Assertions.assertThrows(HarnessPolicyException.class,
            () -> PerformanceReferences.requireReady(HarnessMode.RELEASE, references, List.of(CELL)));

        SteadyStatePolicy smoke = new SteadyStatePolicy(Duration.ofMillis(100), 3, Duration.ofMillis(2500));
        List<String> flags = List.of("-Xms128m", "-Xmx128m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1");
        HarnessConfig config = new HarnessConfig(
            HarnessMode.EXPLORATORY, HarnessConfig.currentJava(), flags, System.getProperty("java.class.path"),
            WarmupPolicy.steadyState(smoke), Duration.ofMillis(600), Duration.ofSeconds(60), out, List.of(),
            List.of(), "unmanaged", Map.of());
        List<ObjectNode> records = new ArrayList<>();
        PerformanceReportSink.Chain chain = PerformanceReportSink.chain(
            out.resolve("validity.json"), out.resolve("report.json"), references, records::add,
            PerformanceReport.Basis.OPERATION_CORRECT);
        new HarnessOrchestrator(config, SystemProbe.live())
            .run(List.of(CELL), RepetitionSchedule.withNullExperiment(List.of(BenchmarkArm.V3, v2Arm), 2),
                 chain.validity());

        assertEquals(6, records.size());
        for (ObjectNode run : records) {
            String json = Node.prettyPrintJson(run);
            assertEquals("completed", run.expectStringMember("status").getValue(), json);
            List<String> failed = run.expectObjectMember("validity").expectArrayMember("violations")
                                     .getElementsAs(n -> n.expectObjectNode().expectStringMember("rule").getValue());
            for (String rule : PerformanceReport.OPERATION_RULES) {
                assertFalse(failed.contains(rule), rule + " failed: " + json);
            }
            String arm = run.expectObjectMember("arm").expectStringMember("id").getValue();
            String sdk = run.expectObjectMember("result").expectStringMember("client_sdk").getValue();
            assertEquals(arm.equals("v2-baseline") ? "aws-sdk-java-v2 2.54.0" : "aws-sdk-java-v3", sdk, json);
            assertEquals(0, run.expectObjectMember("result").expectObjectMember("service")
                              .expectNumberMember("unmatched").getValue().longValue(), json);
        }

        ObjectNode report = chain.report().report();
        assertEquals(List.of(), PerformanceReport.schemaProblems(report));
        assertEquals("operation-correct-repetitions", report.expectStringMember("ratio_basis").getValue());
        assertFalse(report.expectBooleanMember("release_evidence").getValue());
        assertEquals(List.of("v2-baseline"), report.expectArrayMember("reference_arms")
            .getElementsAs(n -> n.expectStringNode().getValue()));
        ObjectNode cell = report.expectArrayMember("cells").get(0).orElseThrow().expectObjectNode();
        for (String member : List.of("comparisons", "best_reference")) {
            List<ObjectNode> ratios = cell.expectArrayMember(member).getElementsAs(Node::expectObjectNode);
            List<String> metrics = ratios.stream().map(r -> r.expectStringMember("metric").getValue()).toList();
            for (RepetitionMetrics.Metric m : List.of(RepetitionMetrics.Metric.CPU_PER_OP,
                                                      RepetitionMetrics.Metric.ALLOC_PER_OP,
                                                      RepetitionMetrics.Metric.THROUGHPUT)) {
                assertTrue(metrics.contains(m.id()), member + " lacks " + m.id() + ": " + Node.prettyPrintJson(cell));
            }
            for (ObjectNode r : ratios) {
                assertEquals("v2-baseline", r.expectStringMember("reference").getValue());
                BenchmarkStatistics.Ratio ratio = BenchmarkStatistics.Ratio.fromNode(r);
                assertTrue(Double.isFinite(ratio.ratio()) && ratio.ratio() > 0, Node.prettyPrintJson(r));
                assertTrue(ratio.ciLow() <= ratio.ratio() && ratio.ratio() <= ratio.ciHigh(), Node.prettyPrintJson(r));
            }
        }
        // The aggregate over this one generated non-streaming cell exists for the V2 reference.
        assertTrue(report.expectArrayMember("aggregates").getElements().stream().map(Node::expectObjectNode)
                         .anyMatch(a -> a.expectStringMember("reference").getValue().equals("v2-baseline")
                                        && a.expectStringMember("metric").getValue().equals("cpu_per_op_ns")
                                        && a.getObjectMember("geometric_mean").isPresent()));
    }
}
