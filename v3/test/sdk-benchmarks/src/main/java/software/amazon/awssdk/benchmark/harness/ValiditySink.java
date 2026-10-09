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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Applies {@link ValidityRules} to every run record and {@link CollectionValidity} at the end, then forwards to a
 * downstream sink (statistics, reports and gates in 19.3/19.4):
 * <ul>
 *     <li>each forwarded run record gains {@code validity: {valid, violations: [{rule, scope, detail}]}};</li>
 *     <li>{@link #finish} writes the full verdict to {@code validity.json} (cells, arms, repetitions, null-experiment
 *     ratios, collection violations) and forwards the manifest with a {@code validity} summary;</li>
 *     <li>{@link #verdict()} returns the same verdict in process.</li>
 * </ul>
 */
public final class ValiditySink implements ResultSink {

    private final Path validityFile;
    private final ResultSink downstream;
    private final List<CollectionValidity.RepetitionVerdict> verdicts = new ArrayList<>();
    private ValidityRules.Expectations expectations;
    private List<String> scheduleProblems = List.of();
    private boolean releaseEvidence;
    private CollectionValidity.CollectionVerdict verdict;

    /**
     * @param validityFile where {@link #finish} writes the verdict, or {@code null} for none
     */
    public ValiditySink(Path validityFile, ResultSink downstream) {
        this.validityFile = validityFile;
        this.downstream = downstream;
    }

    @Override
    public void begin(ObjectNode plan) throws IOException {
        expectations = ValidityRules.Expectations.fromPlan(plan);
        scheduleProblems = plan.getObjectMember("schedule").flatMap(s -> s.getArrayMember("problems"))
                               .map(a -> a.getElementsAs(n -> n.expectStringNode().getValue())).orElse(List.of());
        releaseEvidence = plan.getBooleanMember("release_evidence").map(b -> b.getValue()).orElse(false);
        downstream.begin(plan);
    }

    @Override
    public void accept(ObjectNode runRecord) throws IOException {
        if (expectations == null) {
            throw new IllegalStateException("begin(plan) was not called");
        }
        List<Violation> violations = ValidityRules.repetition(runRecord, expectations);
        CollectionValidity.RepetitionVerdict v = CollectionValidity.RepetitionVerdict.of(runRecord, violations);
        verdicts.add(v);
        downstream.accept(runRecord.withMember("validity", ObjectNode.builder()
            .withMember("valid", v.valid())
            .withMember("violations", ArrayNode.fromNodes(violations.stream().map(Violation::toNode).toList()))
            .build()));
    }

    @Override
    public void finish(ObjectNode manifest) throws IOException {
        verdict = CollectionValidity.evaluate(expectations.mode(), releaseEvidence, verdicts, scheduleProblems);
        ObjectNode node = verdict.toNode();
        if (validityFile != null) {
            Files.writeString(validityFile, Node.prettyPrintJson(node) + "\n", StandardCharsets.UTF_8);
        }
        ObjectNode.Builder summary = ObjectNode.builder()
            .withMember("valid", verdict.valid())
            .withMember("release_evidence", verdict.releaseEvidence())
            .withMember("cells_total", verdict.cells().size())
            .withMember("cells_invalid", verdict.invalidCells())
            .withMember("violations", node.expectArrayMember("violations"));
        if (validityFile != null) {
            summary.withMember("file", validityFile.toString());
        }
        downstream.finish(manifest.withMember("validity", summary.build()));
    }

    /** The collection verdict; available after {@link #finish}. */
    public CollectionValidity.CollectionVerdict verdict() {
        if (verdict == null) {
            throw new IllegalStateException("finish(manifest) was not called");
        }
        return verdict;
    }
}
