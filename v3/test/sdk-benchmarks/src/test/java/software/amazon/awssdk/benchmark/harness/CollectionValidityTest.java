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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ObjectNode;

/** Cell invalidation, null-experiment ratios and collection invalidation on synthetic repetition verdicts. */
class CollectionValidityTest {
    private static final String CELL = "awsJson1_0/generated-sync/apache5-sync/finite/small/success/c1";
    private static final ValidityRules.Metrics BASE = new ValidityRules.Metrics(90_000, 4000, 100_000, 300_000,
                                                                               10_000);

    private static CollectionValidity.RepetitionVerdict rep(String arm, int k, double scale, String digest,
                                                            Violation... violations) {
        String nullOf = arm.endsWith(BenchmarkArm.NULL_SUFFIX)
                        ? arm.substring(0, arm.length() - BenchmarkArm.NULL_SUFFIX.length()) : null;
        ValidityRules.Metrics m = new ValidityRules.Metrics(BASE.cpuPerOpNanos() * scale,
                                                            BASE.allocatedBytesPerOp() * scale,
                                                            BASE.p50Nanos() * scale, BASE.p99Nanos() * scale,
                                                            BASE.throughput() / scale);
        return new CollectionValidity.RepetitionVerdict(k, CELL, arm, nullOf, k, digest, List.of(violations),
                                                        Optional.of(m));
    }

    /** {@code n} repetitions of v3 and v3#null; the null arm's metrics are scaled by {@code nullScale}. */
    private static List<CollectionValidity.RepetitionVerdict> pair(int n, double nullScale) {
        List<CollectionValidity.RepetitionVerdict> out = new ArrayList<>();
        for (int k = 1; k <= n; k++) {
            out.add(rep("v3", k, 1 + 0.01 * (k % 2), "sha256:a"));
            out.add(rep("v3#null", k, nullScale * (1 + 0.01 * ((k + 1) % 2)), "sha256:a"));
        }
        return out;
    }

    private static CollectionValidity.CollectionVerdict evaluate(HarnessMode mode,
                                                                 List<CollectionValidity.RepetitionVerdict> reps) {
        return CollectionValidity.evaluate(mode, true, reps, List.of());
    }

    @Test
    void fiveValidRepetitionsPerArmAndANullWithinBoundsAreValid() {
        CollectionValidity.CollectionVerdict v = evaluate(HarnessMode.RELEASE, pair(5, 1.0));
        assertTrue(v.valid(), v.violations().toString());
        assertTrue(v.releaseEvidence());
        CollectionValidity.CellVerdict cell = v.cells().get(0);
        assertTrue(cell.valid(), cell.violations().toString());
        assertEquals(2, cell.arms().size());
        CollectionValidity.NullExperiment n = cell.nullExperiments().get(0);
        assertEquals(CollectionValidity.NullExperiment.WITHIN, n.status());
        assertEquals("v3", n.sourceArm());
        assertEquals(1.0, n.ratios().get("cpu_per_op_ns"), 0.011);
        ObjectNode node = v.toNode();
        assertEquals(CollectionValidity.SCHEMA, node.expectStringMember("schema").getValue());
        assertEquals(0, node.expectNumberMember("cells_invalid").getValue().intValue());
        assertFalse(evaluate(HarnessMode.EXPLORATORY, pair(5, 1.0)).releaseEvidence());
    }

    @Test
    void aNullRatioOutsideBoundsInvalidatesTheCollection() {
        CollectionValidity.CollectionVerdict v = evaluate(HarnessMode.RELEASE, pair(5, 1.06));
        assertFalse(v.valid());
        assertEquals(List.of(CollectionValidity.NULL_RATIO), v.violations().stream().map(Violation::rule).toList());
        assertEquals(Violation.Scope.COLLECTION, v.violations().get(0).scope());
        assertEquals(CollectionValidity.NullExperiment.OUTSIDE, v.cells().get(0).nullExperiments().get(0).status());
        assertTrue(v.cells().get(0).valid(), "the cell itself is measured validly");
        assertTrue(evaluate(HarnessMode.RELEASE, pair(5, 1.035)).valid());
        assertFalse(evaluate(HarnessMode.RELEASE, pair(5, 0.94)).valid());
    }

    @Test
    void fewerThanFiveValidRepetitionsInvalidateTheCell() {
        List<CollectionValidity.RepetitionVerdict> reps = pair(5, 1.0);
        reps.set(0, rep("v3", 1, 1.0, "sha256:a",
                        Violation.repetition(ValidityRules.FAILED_OPERATIONS, "1 failed")));
        CollectionValidity.CellVerdict cell = evaluate(HarnessMode.RELEASE, reps).cells().get(0);
        assertFalse(cell.valid());
        assertEquals(List.of(CollectionValidity.TOO_FEW_VALID_REPETITIONS),
                     cell.violations().stream().map(Violation::rule).toList());
        assertEquals(4, cell.arms().get(0).validRepetitions());
        // Six repetitions with one invalid leave five: valid.
        List<CollectionValidity.RepetitionVerdict> six = pair(6, 1.0);
        six.set(0, reps.get(0));
        assertTrue(evaluate(HarnessMode.RELEASE, six).cells().get(0).valid());
    }

    @Test
    void aSteadyStateTimeoutInvalidatesTheCellEvenWithEnoughValidRepetitions() {
        List<CollectionValidity.RepetitionVerdict> reps = pair(6, 1.0);
        reps.set(0, rep("v3", 1, 1.0, "sha256:a", Violation.cell(ValidityRules.STEADY_STATE_GATE, "timeout")));
        CollectionValidity.CellVerdict cell = evaluate(HarnessMode.RELEASE, reps).cells().get(0);
        assertEquals(List.of(ValidityRules.STEADY_STATE_GATE), cell.violations().stream().map(Violation::rule).toList());
        assertEquals(Violation.Scope.CELL, cell.violations().get(0).scope());
    }

    @Test
    void artifactRules() {
        List<CollectionValidity.RepetitionVerdict> drift = pair(5, 1.0);
        drift.set(4, rep("v3", 3, 1.0, "sha256:rebuilt"));
        assertEquals(List.of(CollectionValidity.ARTIFACT_DRIFT, CollectionValidity.NULL_ARTIFACT),
                     evaluate(HarnessMode.RELEASE, drift).cells().get(0).violations().stream()
                                                         .map(Violation::rule).toList());
        List<CollectionValidity.RepetitionVerdict> other = new ArrayList<>();
        for (CollectionValidity.RepetitionVerdict r : pair(5, 1.0)) {
            other.add(r.armId().equals("v3#null") ? rep("v3#null", r.repetition(), 1.0, "sha256:b") : r);
        }
        assertEquals(List.of(CollectionValidity.NULL_ARTIFACT),
                     evaluate(HarnessMode.RELEASE, other).cells().get(0).violations().stream()
                                                         .map(Violation::rule).toList());
    }

    @Test
    void nullCoverage() {
        List<CollectionValidity.RepetitionVerdict> onlyV3 = pair(5, 1.0).stream()
            .filter(r -> r.armId().equals("v3")).toList();
        assertEquals(List.of(CollectionValidity.NULL_MISSING),
                     evaluate(HarnessMode.RELEASE, onlyV3).cells().get(0).violations().stream()
                                                          .map(Violation::rule).toList());
        assertTrue(evaluate(HarnessMode.EXPLORATORY, onlyV3).cells().get(0).valid());

        List<CollectionValidity.RepetitionVerdict> deadNull = new ArrayList<>();
        for (CollectionValidity.RepetitionVerdict r : pair(5, 1.0)) {
            deadNull.add(r.armId().equals("v3#null")
                         ? rep("v3#null", r.repetition(), 1.0, "sha256:a",
                               Violation.repetition(ValidityRules.RUN_STATUS, "timeout")) : r);
        }
        CollectionValidity.CollectionVerdict v = evaluate(HarnessMode.RELEASE, deadNull);
        assertEquals(List.of(CollectionValidity.NULL_NOT_EVALUABLE),
                     v.violations().stream().map(Violation::rule).toList());
        assertEquals(CollectionValidity.NullExperiment.NOT_EVALUABLE,
                     v.cells().get(0).nullExperiments().get(0).status());
    }

    @Test
    void scheduleProblemsInvalidateTheCollection() {
        CollectionValidity.CollectionVerdict v = CollectionValidity.evaluate(HarnessMode.RELEASE, true, pair(5, 1.0),
                                                                             List.of("cell x: no null"));
        assertEquals(List.of(CollectionValidity.SCHEDULE), v.violations().stream().map(Violation::rule).toList());
    }

    /** The sink annotates each record, then writes and forwards the verdict. */
    @Test
    void sinkAnnotatesRecordsAndForwardsTheVerdict(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws IOException {
        List<ObjectNode> forwarded = new ArrayList<>();
        ObjectNode[] finished = new ObjectNode[1];
        ResultSink downstream = new ResultSink() {
            @Override
            public void accept(ObjectNode runRecord) {
                forwarded.add(runRecord);
            }

            @Override
            public void finish(ObjectNode manifest) {
                finished[0] = manifest;
            }
        };
        ValiditySink sink = new ValiditySink(dir.resolve("validity.json"), downstream);
        java.util.Map<String, String> host = ValidityRulesTest.HOST;
        ObjectNode.Builder hostNode = ObjectNode.builder();
        host.forEach(hostNode::withMember);
        ObjectNode plan = ObjectNode.builder()
            .withMember("mode", "release")
            .withMember("release_evidence", true)
            .withMember("host", hostNode.build())
            .withMember("config", ObjectNode.builder()
                .withMember("measure_ns", 30_000_000_000L)
                .withMember("jvm_flags", software.amazon.smithy.model.node.ArrayNode.fromStrings(
                    ValidityRulesTest.FLAGS))
                .withMember("warmup", ObjectNode.builder()
                                                .withMember("sampling", SteadyStatePolicy.RELEASE.toNode()).build())
                .build())
            .build();
        sink.begin(plan);
        for (int k = 1; k <= 5; k++) {
            for (String arm : List.of("v3", "v3#null")) {
                BenchmarkArm a = arm.equals("v3") ? BenchmarkArm.V3 : BenchmarkArm.V3.nullTwin();
                sink.accept(ValidityRulesTest.valid().withMember("repetition", k).withMember(
                    "arm", a.toNode().toBuilder().withMember("artifact_digest", "sha256:aa").build()));
            }
        }
        sink.accept(ValidityRulesTest.valid().withMember("status", "timeout"));
        sink.finish(ObjectNode.builder().withMember("schema", "m").build());

        assertEquals(11, forwarded.size());
        assertTrue(forwarded.get(0).expectObjectMember("validity").expectBooleanMember("valid").getValue());
        assertFalse(forwarded.get(10).expectObjectMember("validity").expectBooleanMember("valid").getValue());
        CollectionValidity.CollectionVerdict v = sink.verdict();
        assertTrue(v.valid(), v.violations().toString());
        assertEquals(6, v.cells().get(0).arms().get(0).repetitions().size());
        assertEquals(5, v.cells().get(0).arms().get(0).validRepetitions());
        assertTrue(v.cells().get(0).valid());
        assertTrue(finished[0].expectObjectMember("validity").expectBooleanMember("valid").getValue());
        assertEquals(v.toNode(), software.amazon.smithy.model.node.Node.parse(
            java.nio.file.Files.readString(dir.resolve("validity.json"))));
    }
}
