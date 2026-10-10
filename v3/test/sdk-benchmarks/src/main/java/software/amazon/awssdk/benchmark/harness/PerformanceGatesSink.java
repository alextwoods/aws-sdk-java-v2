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
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The consumer of {@link PerformanceReportSink}'s output: on {@link #finish} it evaluates the {@link PerformanceGates}
 * over the finished report and the collection verdict, writes {@code gates.json}, and forwards the manifest with a
 * {@code gates} summary. The separately produced evidence of Req 20.20-20.22 is read from {@value #LARGE_STREAM_FILE}
 * and {@value #HOT_PATH_FILE} in the output directory when present; absent evidence makes those gates not evaluable.
 * {@link #evaluateDirectory} is the same evaluation of an existing output directory ({@code HarnessMain
 * --evaluate-gates DIR}).
 */
public final class PerformanceGatesSink implements ResultSink {

    public static final String GATES_FILE = "gates.json";
    public static final String REPORT_FILE = "report.json";
    public static final String VALIDITY_FILE = "validity.json";
    public static final String LARGE_STREAM_FILE = "large-stream.json";
    public static final String HOT_PATH_FILE = "hot-path.json";

    private final Path dir;
    private final Supplier<ObjectNode> report;
    private final Supplier<CollectionValidity.CollectionVerdict> verdict;
    private final ResultSink downstream;
    private PerformanceGates.Result result;

    PerformanceGatesSink(Path dir, Supplier<ObjectNode> report, Supplier<CollectionValidity.CollectionVerdict> verdict,
                         ResultSink downstream) {
        this.dir = dir;
        this.report = report;
        this.verdict = verdict;
        this.downstream = downstream;
    }

    /** Validity rules, the report, these gates, then {@code downstream}; files go to {@code dir}. */
    public static Chain chain(Path dir, List<PerformanceReferences.Reference> references, ResultSink downstream,
                              PerformanceReport.Basis basis) {
        PerformanceReportSink.Chain[] inner = new PerformanceReportSink.Chain[1];
        PerformanceGatesSink gates = new PerformanceGatesSink(dir, () -> inner[0].report().report(),
                                                              () -> inner[0].validity().verdict(), downstream);
        inner[0] = PerformanceReportSink.chain(dir.resolve(VALIDITY_FILE), dir.resolve(REPORT_FILE), references, gates,
                                               basis);
        return new Chain(inner[0].validity(), inner[0].report(), gates);
    }

    /** The wired sinks: give {@link #validity()} to the orchestrator. */
    public record Chain(ValiditySink validity, PerformanceReportSink report, PerformanceGatesSink gates) {
    }

    @Override
    public void begin(ObjectNode plan) throws IOException {
        downstream.begin(plan);
    }

    @Override
    public void accept(ObjectNode runRecord) throws IOException {
        downstream.accept(runRecord);
    }

    @Override
    public void finish(ObjectNode manifest) throws IOException {
        ObjectNode r = report.get();
        result = PerformanceGates.evaluate(new PerformanceGates.Inputs(
            r, Optional.of(verdict.get().toNode()), PerformanceGates.requiredCellsFor(r),
            readOptional(dir.resolve(LARGE_STREAM_FILE)), readOptional(dir.resolve(HOT_PATH_FILE))));
        write(dir.resolve(GATES_FILE), result);
        downstream.finish(manifest.withMember("gates", ObjectNode.builder()
            .withMember("schema", PerformanceGates.SCHEMA)
            .withMember("file", dir.resolve(GATES_FILE).toString())
            .withMember("decision", result.decision().id())
            .build()));
    }

    /** The gate result; available after {@link #finish}. */
    public PerformanceGates.Result result() {
        if (result == null) {
            throw new IllegalStateException("finish(manifest) was not called");
        }
        return result;
    }

    /**
     * Evaluates the gates of an existing harness output directory and writes its {@code gates.json}. The required
     * cells are those of {@link PerformanceGates#requiredCellsFor}; evidence files default to the directory's.
     */
    public static PerformanceGates.Result evaluateDirectory(Path dir, Optional<Path> largeStream,
                                                            Optional<Path> hotPath) throws IOException {
        ObjectNode r = Node.parse(Files.readString(dir.resolve(REPORT_FILE))).expectObjectNode();
        PerformanceGates.Result result = PerformanceGates.evaluate(new PerformanceGates.Inputs(
            r, readOptional(dir.resolve(VALIDITY_FILE)), PerformanceGates.requiredCellsFor(r),
            readOptional(largeStream.orElse(dir.resolve(LARGE_STREAM_FILE))),
            readOptional(hotPath.orElse(dir.resolve(HOT_PATH_FILE)))));
        write(dir.resolve(GATES_FILE), result);
        return result;
    }

    static void write(Path file, PerformanceGates.Result result) throws IOException {
        Files.writeString(file, Node.prettyPrintJson(result.toNode()) + "\n", StandardCharsets.UTF_8);
    }

    static Optional<ObjectNode> readOptional(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(Node.parse(Files.readString(file)).expectObjectNode());
    }
}
