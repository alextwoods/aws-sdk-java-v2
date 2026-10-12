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

package software.amazon.awssdk.protocol.smithytests.tool;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Generates {@code test/smithy-protocol-tests/legacy-disposition.csv} (Requirement 32.10, design D.1 and OD-12): one row
 * per legacy protocol test case per legacy suite (protocol) that runs it, with its Smithy equivalent or its
 * disposition.
 *
 * <p>Inputs: the legacy case files and C2J models (left on disk under {@code test/protocol-tests-core} and
 * {@code test/protocol-tests}), the upstream Smithy protocol test models from the classpath, the SDK-owned protocol test
 * models under {@code smithy/sdk-protocol-tests}, and the hand-review decisions in
 * {@code src/tool/legacy-disposition-review.json}, which take precedence over the automatic match. Matching details
 * for review go to {@code target/legacy-disposition-candidates.txt}.
 *
 * <p>Regenerate the CSV after changing any input with {@code src/tool/run.sh} ({@code [--allow-unresolved]}).
 * {@code LegacyDispositionCsvTest} fails the module build when the committed CSV is out of date.
 *
 * <p>Exit status is non-zero when any row is unresolved (unless {@code --allow-unresolved}), when a review entry is stale
 * or invalid, or when the SDK-owned Smithy model has validation errors.
 */
public final class LegacyDispositionTool {

    static final String SDK_NAMESPACE_PREFIX = "software.amazon.awssdk.protocoltests";
    static final String CSV_HEADER = "legacy file,description,protocol,smithy id|none,disposition,reason";

    private final Path root;

    LegacyDispositionTool(Path root) {
        this.root = root;
    }

    Path suitesDir() {
        return root.resolve("test/protocol-tests-core/src/main/resources/software/amazon/awssdk/protocol/suites");
    }

    Path modelsDir() {
        return root.resolve("test/protocol-tests/src/main/resources/codegen-resources");
    }

    Path moduleDir() {
        return root.resolve("test/smithy-protocol-tests");
    }

    Path sdkModelDir() {
        return moduleDir().resolve("smithy/sdk-protocol-tests");
    }

    Path reviewFile() {
        return moduleDir().resolve("src/tool/legacy-disposition-review.json");
    }

    Path csvFile() {
        return moduleDir().resolve("legacy-disposition.csv");
    }

    Path candidatesFile() {
        return moduleDir().resolve("target/legacy-disposition-candidates.txt");
    }

    public static void main(String[] args) throws IOException {
        Path root = Paths.get(".");
        boolean allowUnresolved = false;
        for (int i = 0; i < args.length; i++) {
            if ("--root".equals(args[i])) {
                root = Paths.get(args[++i]);
            } else if ("--allow-unresolved".equals(args[i])) {
                allowUnresolved = true;
            } else {
                throw new IllegalArgumentException("Unknown argument " + args[i]);
            }
        }
        int status = new LegacyDispositionTool(root.toAbsolutePath().normalize()).run(allowUnresolved, System.out);
        System.exit(status);
    }

    /** The output of one tool run, before anything is written. */
    static final class Output {
        final List<Dispositions.Row> rows;
        final List<SmithyCase> smithyCases;
        /** Review entries that matched no legacy row. */
        final List<String> staleReviews;
        final String csv;
        /** Matching details for every row that is not a copied Smithy case or an API Gateway case. */
        final String candidates;

        Output(List<Dispositions.Row> rows, List<SmithyCase> smithyCases, List<String> staleReviews, String csv,
               String candidates) {
            this.rows = rows;
            this.smithyCases = smithyCases;
            this.staleReviews = staleReviews;
            this.csv = csv;
            this.candidates = candidates;
        }

        long unresolved() {
            return rows.stream().filter(r -> Dispositions.UNRESOLVED.equals(r.disposition)).count();
        }
    }

    int run(boolean allowUnresolved, PrintStream log) throws IOException {
        Output output = generate(log);
        if (!output.staleReviews.isEmpty()) {
            log.println("ERROR: review entries match no legacy row: " + output.staleReviews);
            return 2;
        }
        Files.write(csvFile(), output.csv.getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(candidatesFile().getParent());
        Files.write(candidatesFile(), output.candidates.getBytes(StandardCharsets.UTF_8));
        printSummary(output.rows, log);
        if (output.unresolved() > 0 && !allowUnresolved) {
            log.printf("ERROR: %d unresolved rows; see %s%n", output.unresolved(), candidatesFile());
            return 1;
        }
        return 0;
    }

    /** Matches every legacy case and applies the review decisions, without writing any file. */
    Output generate(PrintStream log) throws IOException {
        List<LegacyCase> legacy = LegacyCase.loadAll(suitesDir());
        log.printf("Loaded %d legacy case rows from %d case files%n", legacy.size(), LegacyCase.fileCount(legacy));

        Model model = loadSmithyModel(log);
        List<SmithyCase> smithy = SmithyCase.loadAll(model, SDK_NAMESPACE_PREFIX);
        log.printf("Loaded %d client-applicable Smithy cases for Supported_Protocols%n", smithy.size());

        Dispositions dispositions = new Dispositions(Dispositions.loadReviews(reviewFile()), smithy);
        Matcher matcher = new Matcher(smithy);
        Map<String, C2jModel> c2j = new HashMap<>();
        List<Dispositions.Row> rows = new ArrayList<>();
        StringBuilder candidates = new StringBuilder();
        for (LegacyCase c : legacy) {
            Set<String> features = legacyFeatures(c, c2j, log);
            Matcher.Result result = matcher.match(c, features);
            Dispositions.Row row = dispositions.decide(c, result);
            rows.add(row);
            if (!"copied-smithy".equals(row.category) && !"apigateway".equals(row.category)) {
                candidates.append(c.key()).append(" @ ").append(c.suite.protocol).append(" [")
                          .append(row.dispositionColumn()).append("] ").append(c.description).append('\n')
                          .append("    legacy: ").append(features).append('\n');
                for (Matcher.Candidate cand : result.candidates) {
                    candidates.append("    ").append(cand).append('\n');
                }
            }
        }
        return new Output(rows, smithy, dispositions.unusedReviews(), csv(rows), candidates.toString());
    }

    private Set<String> legacyFeatures(LegacyCase c, Map<String, C2jModel> cache, PrintStream log) throws IOException {
        if (c.suite.modelDir == null) {
            return null;
        }
        C2jModel model = cache.get(c.suite.modelDir);
        if (model == null) {
            model = C2jModel.load(modelsDir().resolve(c.suite.modelDir).resolve("service-2.json"));
            cache.put(c.suite.modelDir, model);
        }
        ShapeView.OperationView op = model.operation(c.operation);
        if (op == null) {
            log.println("WARN: " + c.key() + ": operation " + c.operation + " not in " + c.suite.modelDir);
            return null;
        }
        boolean bindings = c.suite.httpBindings();
        switch (c.kind()) {
            case "request":
                return FeatureExtractor.request(op, c.params(), bindings, c.requestFacts());
            case "response":
                return FeatureExtractor.response(op, c.params(), bindings, c.responseFacts());
            default:
                ShapeView error = c.error == null ? null : model.shape(c.error);
                return error == null ? null : FeatureExtractor.error(error, c.params(), bindings, c.responseFacts());
        }
    }

    /** Upstream protocol test models from the classpath, plus the SDK-owned cases under {@code smithy/sdk-protocol-tests}. */
    Model loadSmithyModel(PrintStream log) throws IOException {
        ModelAssembler assembler = Model.assembler(getClass().getClassLoader())
                                        .discoverModels(getClass().getClassLoader());
        List<Path> sdkFiles = new ArrayList<>();
        if (Files.isDirectory(sdkModelDir())) {
            try (Stream<Path> s = Files.list(sdkModelDir())) {
                s.filter(p -> p.toString().endsWith(".smithy")).sorted().forEach(sdkFiles::add);
            }
        }
        for (Path p : sdkFiles) {
            assembler.addImport(p);
        }
        ValidatedResult<Model> result = assembler.assemble();
        int sdkErrors = 0;
        for (ValidationEvent e : result.getValidationEvents()) {
            boolean severe = e.getSeverity() == Severity.ERROR || e.getSeverity() == Severity.DANGER;
            boolean sdk = e.getSourceLocation().getFilename().contains("smithy-protocol-tests/smithy/sdk-protocol-tests/");
            if (severe && (sdk || !result.getResult().isPresent())) {
                log.println("MODEL " + e);
                sdkErrors++;
            }
        }
        if (sdkErrors > 0 || !result.getResult().isPresent()) {
            throw new IllegalStateException(sdkErrors + " validation errors in the Smithy protocol test models");
        }
        log.printf("Assembled Smithy model with %d SDK-owned model files%n", sdkFiles.size());
        return result.getResult().get();
    }

    static String csv(List<Dispositions.Row> rows) {
        StringBuilder out = new StringBuilder(CSV_HEADER).append('\n');
        for (Dispositions.Row r : rows) {
            out.append(csvLine(r.legacy.key(), r.legacy.description, r.legacy.suite.protocol, r.smithyColumn(),
                               r.dispositionColumn(), r.reasonColumn()))
               .append('\n');
        }
        return out.toString();
    }

    static String csvLine(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(csvField(fields[i]));
        }
        return sb.toString();
    }

    static String csvField(String value) {
        String v = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
        if (v.indexOf(',') >= 0 || v.indexOf('"') >= 0) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    private static void printSummary(List<Dispositions.Row> rows, PrintStream log) {
        Map<String, Integer> byDisposition = new TreeMap<>();
        Map<String, Integer> byCategory = new TreeMap<>();
        int review = 0;
        for (Dispositions.Row r : rows) {
            byDisposition.merge(r.disposition, 1, Integer::sum);
            byCategory.merge(r.disposition + " / " + r.category, 1, Integer::sum);
            if (r.needsHumanReview) {
                review++;
            }
        }
        log.println("Rows: " + rows.size());
        log.println("By disposition: " + byDisposition);
        for (Map.Entry<String, Integer> e : byCategory.entrySet()) {
            log.println("  " + e.getKey() + ": " + e.getValue());
        }
        log.println("Rows flagged for human review: " + review);
    }
}
