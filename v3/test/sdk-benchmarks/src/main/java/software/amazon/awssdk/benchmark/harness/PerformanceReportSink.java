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
import java.util.function.Supplier;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The consumer of {@link ValiditySink}'s output: collects the plan and every validity-annotated run record, and on
 * {@link #finish} builds the {@link PerformanceReport} from them and the collection verdict, writes it to
 * {@code report.json}, and forwards the manifest with a {@code report} summary. {@link #chain} wires both sinks.
 */
public final class PerformanceReportSink implements ResultSink {

    private final Path reportFile;
    private final List<PerformanceReferences.Reference> references;
    private final Supplier<CollectionValidity.CollectionVerdict> verdict;
    private final ResultSink downstream;
    private final PerformanceReport.Basis basis;
    private final List<ObjectNode> runs = new ArrayList<>();
    private ObjectNode plan;
    private ObjectNode report;

    PerformanceReportSink(Path reportFile, List<PerformanceReferences.Reference> references,
                          Supplier<CollectionValidity.CollectionVerdict> verdict, ResultSink downstream,
                          PerformanceReport.Basis basis) {
        this.reportFile = reportFile;
        this.basis = basis;
        this.references = List.copyOf(references);
        this.verdict = verdict;
        this.downstream = downstream;
    }

    /** Validity rules, then this report, then {@code downstream}; the returned sink goes to the orchestrator. */
    public static Chain chain(Path validityFile, Path reportFile, List<PerformanceReferences.Reference> references,
                              ResultSink downstream) {
        return chain(validityFile, reportFile, references, downstream, PerformanceReport.Basis.VALID);
    }

    /** {@link #chain(Path, Path, List, ResultSink)} with the report's ratio {@code basis}. */
    public static Chain chain(Path validityFile, Path reportFile, List<PerformanceReferences.Reference> references,
                              ResultSink downstream, PerformanceReport.Basis basis) {
        ValiditySink[] validity = new ValiditySink[1];
        PerformanceReportSink report = new PerformanceReportSink(reportFile, references, () -> validity[0].verdict(),
                                                                 downstream, basis);
        validity[0] = new ValiditySink(validityFile, report);
        return new Chain(validity[0], report);
    }

    /** The wired sinks: give {@link #validity()} to the orchestrator. */
    public record Chain(ValiditySink validity, PerformanceReportSink report) {
    }

    @Override
    public void begin(ObjectNode plan) throws IOException {
        this.plan = plan;
        downstream.begin(plan);
    }

    @Override
    public void accept(ObjectNode runRecord) throws IOException {
        runs.add(runRecord);
        downstream.accept(runRecord);
    }

    @Override
    public void finish(ObjectNode manifest) throws IOException {
        if (plan == null) {
            throw new IllegalStateException("begin(plan) was not called");
        }
        report = PerformanceReport.build(plan, runs, verdict.get(), references, basis);
        Files.writeString(reportFile, Node.prettyPrintJson(report) + "\n", StandardCharsets.UTF_8);
        downstream.finish(manifest.withMember("report", ObjectNode.builder()
            .withMember("schema", PerformanceReport.SCHEMA)
            .withMember("file", reportFile.toString())
            .withMember("references_ready", report.expectBooleanMember("references_ready"))
            .build()));
    }

    /** The report; available after {@link #finish}. */
    public ObjectNode report() {
        if (report == null) {
            throw new IllegalStateException("finish(manifest) was not called");
        }
        return report;
    }
}
