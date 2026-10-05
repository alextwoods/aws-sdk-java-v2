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

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.test.codegen.support.GeneratedTree.assertByteIdentical;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.awssdk.awscodegen.validation.DiagnosticCode;
import software.amazon.awssdk.test.codegen.support.GeneratedTree;
import software.amazon.awssdk.test.codegen.support.SmithyModelFixture;

/**
 * Property 3 (Requirements 2.3-2.9): for any generation request whose model directory contains a C2J model or a V2
 * paginator, waiter, endpoint, example, customization (or smoke-test) sidecar, generation reports the service and the
 * exact legacy format of every such file and publishes no artifact for that service.
 *
 * <p>Each try builds several services side by side, each with its own model directory and output roots. A random
 * subset of them receives random combinations of legacy files: identified by content under their V2 file name, under
 * a disguised or another format's V2 file name, or identified only by their V2 file name because the content is
 * inconclusive or not JSON; placed in nested directories; commented like V2 {@code customization.config} files; and
 * mixed with valid Smithy IDL and Smithy JSON AST files (the latter sometimes carrying a V2 file name) and ignored
 * notes that contain legacy content. The other services stay clean. The fixture generator knows what it injected, so
 * the expected diagnostics are computed from the injection itself, never by calling the validator.
 *
 * <p>Checked for every try:
 * <ul>
 *   <li>every affected service fails with exactly one diagnostic per injected legacy file, naming the service, the file
 *       and the legacy format, sorted by file, and nothing else;</li>
 *   <li>no published artifact exists for an affected service: a fresh service gets no output directory at all, a
 *       previously published tree (from an earlier successful generation or placed by hand) is byte-identical to its
 *       content before the call, and no staging or previous-tree directory remains;</li>
 *   <li>every clean service generates, and its trees are byte-identical to those of the same service generated alone,
 *       so neither the other services' legacy inputs nor their failures affect it;</li>
 *   <li>the diagnostics and the failure message do not depend on the order in which model files were created.</li>
 * </ul>
 */
class LegacyInputRejectionPropertyTest {

    private static final String SOURCES = "out/src";
    private static final String RESOURCES = "out/resources";

    private static final String BY_CONTENT = "identified by content";
    private static final String BY_FILE_NAME = "identified by V2 file name";
    private static final String BY_FILE_NAME_NOT_JSON = BY_FILE_NAME + "; content is not valid JSON";

    /** Directories (relative to the model directory) an injected file is placed in. */
    private static final List<String> DIRECTORIES =
        List.of("", "codegen-resources/", "legacy/v2/", "services/example/src/main/resources/codegen-resources/");

    /** Content that is valid JSON but carries no legacy or Smithy signature. */
    private static final List<String> INCONCLUSIVE_JSON =
        List.of("{}", "[]", "{\"unrelated\": true}", "\"just a string\"", "42", "null", "{\n  // only a comment\n}\n",
                "{\"version\": \"1.0\"}", "{\"testCases\": \"not a list\"}", "{\"pagination\": []}");

    /** Content that is not JSON at all. */
    private static final List<String> NOT_JSON = List.of("", "not json", "{\"pagination\": ", "[1, 2", "}{");

    /** Names of files that are not model inputs whatever they contain. */
    private static final List<String> IGNORED_NAMES =
        List.of("README.md", "notes.txt", "service-2.json.bak", "codegen-resources/paginators-1.json.orig",
                "legacy/customization.yaml");

    /** Where a Smithy JSON AST file is placed; several use V2 file names, which must not reject a canonical model. */
    private static final List<String> AST_NAMES =
        List.of("", "", "model.json", "shapes/extra.json", "service-2.json", "codegen-resources/api-2.json",
                "paginators-1.json", "codegen-resources/waiters-2.json");

    private static final List<String> C2J_PROTOCOLS = List.of("json", "rest-json", "rest-xml", "query", "ec2");

    // Feature: aws-sdk-java-v3, Property 3: Legacy input rejection is atomic
    // Embedded edge cases are disabled: through the nested flatMaps they pick the first alternative of most choices,
    // starving the identification and root-state combinations. The statistics coverage checks below fail the property
    // if a legacy format, identification mode or root state is never exercised.
    @Property(tries = 100, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 3: Legacy input rejection is atomic")
    void legacyInputRejectionIsAtomic(@ForAll("scenarios") Scenario scenario) throws IOException {
        // Under the module's build directory: a try abandoned by jqwik's shrinking timeout never cleans up.
        Path trial = Files.createTempDirectory(Files.createDirectories(Path.of("target", "legacy-input-rejection")),
                                               "try-").toAbsolutePath();
        try {
            Map<Integer, Trees> before = new LinkedHashMap<>();
            for (ServiceSpec service : scenario.services()) {
                before.put(service.index(), prepare(trial, service));
            }

            Map<Integer, Outcome> outcomes = new LinkedHashMap<>();
            for (int index : scenario.generationOrder()) {
                ServiceSpec service = scenario.services().get(index);
                outcomes.put(index, run(request(service, work(trial, service))));
            }

            for (ServiceSpec service : scenario.services()) {
                Outcome outcome = outcomes.get(service.index());
                if (service.affected()) {
                    verifyRejected(trial, service, outcome, before.get(service.index()));
                } else {
                    verifyGenerated(trial, service, outcome);
                }
            }
            collectStatistics(scenario);
        } finally {
            deleteRecursively(trial);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Execution and checks
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Writes the service's model directory in its creation order and prepares its output roots; returns the published
     * trees as they are before the generation under test (empty for a fresh service).
     */
    private static Trees prepare(Path trial, ServiceSpec service) throws IOException {
        Path work = work(trial, service);
        Path model = work.resolve("model");
        switch (service.rootState()) {
            case FRESH -> write(model, service.files(), service.creationOrder());
            case HAND_PLACED -> {
                write(model, service.files(), service.creationOrder());
                Path sources = Files.createDirectories(work.resolve(SOURCES).resolve("previous/pkg"));
                Files.writeString(sources.resolve("Previous.java"), "final class Previous {} // " + service.index());
                Path resources = Files.createDirectories(work.resolve(RESOURCES).resolve("META-INF"));
                Files.writeString(resources.resolve("previous.txt"), "previously published\n");
            }
            case PREVIOUS_GENERATION -> {
                // A service that generated successfully before its legacy files were added.
                Set<String> legacy = service.injectedPaths();
                List<String> canonicalFirst = service.creationOrder().stream().filter(p -> !legacy.contains(p)).toList();
                write(model, service.files(), canonicalFirst);
                Outcome previous = run(request(service, work));
                assertNull(previous.failure(), () -> "Generation before the legacy files were added failed: "
                                                     + previous.failure().getMessage());
                write(model, service.files(),
                      service.creationOrder().stream().filter(legacy::contains).toList());
            }
            default -> throw new IllegalStateException(service.rootState().toString());
        }
        return Trees.read(work);
    }

    private static void verifyRejected(Path trial, ServiceSpec service, Outcome outcome, Trees before)
        throws IOException {
        Path work = work(trial, service);
        List<ExpectedDiagnostic> expected = service.expectedDiagnostics();
        AwsCodegenException failure = outcome.failure();
        assertNotNull(failure, () -> service.serviceId() + " generated although its model directory contains "
                                     + expected);

        // Exactly one diagnostic per injected legacy file, sorted by file, naming the service and the legacy format.
        List<Diagnostic> diagnostics = failure.diagnostics();
        assertEquals(expected.stream().map(ExpectedDiagnostic::key).toList(),
                     diagnostics.stream().map(DiagnosticKey::of).toList(),
                     () -> "Diagnostics of " + service.serviceId() + ":\n" + failure.getMessage());
        for (int i = 0; i < diagnostics.size(); i++) {
            Diagnostic diagnostic = diagnostics.get(i);
            ExpectedDiagnostic oracle = expected.get(i);
            assertEquals(Optional.empty(), diagnostic.operation(), diagnostic::render);
            assertEquals(Optional.empty(), diagnostic.shape(), diagnostic::render);
            assertEquals(List.of(), diagnostic.integrations(), diagnostic::render);
            String message = diagnostic.message();
            assertTrue(message.startsWith(oracle.key().element() + " is not a canonical Smithy model input ("
                                          + oracle.detail() + "). V3 generates only from canonical Smithy models: "
                                          + "represent this information with "),
                       () -> "Expected the message to name the format and how it was identified (" + oracle.detail()
                             + "): " + diagnostic.render());
            assertTrue(message.endsWith(" and remove the file."), diagnostic::render);
        }
        String rendered = diagnostics.stream().map(d -> "\n  " + d.render()).collect(Collectors.joining());
        assertTrue(failure.getMessage().startsWith("The canonical model input of service " + service.serviceId()
                                                   + " contains non-canonical inputs"), failure::getMessage);
        assertTrue(failure.getMessage().endsWith(rendered), failure::getMessage);

        // Nothing published: no output for a fresh service, the previous trees untouched otherwise, no leftovers.
        assertUnpublished(work, service.rootState() == RootState.FRESH, before);

        // The same inputs created in another order produce the same failure.
        Path reordered = trial.resolve("reordered-" + service.index());
        List<String> reversed = new ArrayList<>(service.creationOrder());
        Collections.reverse(reversed);
        write(reordered.resolve("model"), service.files(), reversed);
        AwsCodegenException again = run(request(service, reordered)).failure();
        assertNotNull(again, "Reordered model directory of " + service.serviceId() + " generated");
        assertEquals(diagnostics, again.diagnostics(), "Diagnostics depend on file-creation order");
        assertEquals(failure.getMessage(), again.getMessage(), "Failure message depends on file-creation order");
        assertUnpublished(reordered, true, Trees.read(reordered));
    }

    private static void assertUnpublished(Path work, boolean fresh, Trees before) {
        if (fresh) {
            assertEquals(Set.of("model"), children(work), "A rejected fresh service must get no output directory");
            return;
        }
        assertEquals(Set.of("model", "out"), children(work));
        assertEquals(Set.of("src", "resources"), children(work.resolve("out")),
                     "Staging or previous-tree directories remain next to the published trees");
        Trees after = Trees.read(work);
        assertByteIdentical(before.sources(), after.sources());
        assertByteIdentical(before.resources(), after.resources());
    }

    private static void verifyGenerated(Path trial, ServiceSpec service, Outcome outcome) {
        Path work = work(trial, service);
        assertNull(outcome.failure(), () -> "Clean service " + service.serviceId() + " failed: "
                                            + outcome.failure().getMessage());
        AwsCodegenResult result = outcome.result();
        Trees published = Trees.read(work);
        assertTrue(published.sources().paths().contains(service.packagePath() + "/" + AwsCodegen.SERVICE_INFO_TYPE
                                                         + ".java"), () -> published.sources().paths().toString());
        assertFalse(published.resources().isEmpty());
        assertEquals(Set.of("src", "resources"), children(work.resolve("out")),
                     "Staging or previous-tree directories remain next to the published trees");

        // The same service generated alone, from files created in reverse order, is byte-identical: the other
        // services' legacy inputs and failures do not affect it.
        Path alone = trial.resolve("alone-" + service.index());
        List<String> reversed = new ArrayList<>(service.creationOrder());
        Collections.reverse(reversed);
        write(alone.resolve("model"), service.files(), reversed);
        Outcome reference = run(request(service, alone));
        assertNull(reference.failure(), () -> "Isolated generation failed: " + reference.failure().getMessage());
        Trees isolated = Trees.read(alone);
        assertByteIdentical(isolated.sources(), published.sources());
        assertByteIdentical(isolated.resources(), published.resources());
        assertEquals(reference.result().generatedFiles(), result.generatedFiles());
        assertEquals(reference.result().generatedResources(), result.generatedResources());
    }

    private static Outcome run(AwsCodegenRequest request) {
        try {
            return new Outcome(AwsCodegen.generate(request, List.of()), null);
        } catch (AwsCodegenException e) {
            return new Outcome(null, e);
        }
    }

    private static AwsCodegenRequest request(ServiceSpec service, Path work) {
        return new AwsCodegenRequest(service.serviceId(), service.packageName(), work.resolve("model"),
                                     work.resolve(SOURCES)).withResourceOutputDirectory(work.resolve(RESOURCES));
    }

    private static Path work(Path trial, ServiceSpec service) {
        return trial.resolve("svc" + service.index());
    }

    /** Writes {@code order} (a subset of {@code files}' paths) with {@link SmithyModelFixture}'s writer. */
    private static void write(Path model, Map<String, String> files, List<String> order) {
        Map<String, String> subset = new LinkedHashMap<>();
        order.forEach(path -> subset.put(path, files.get(path)));
        SmithyModelFixture.of(subset).write(model, order);
    }

    private static Set<String> children(Path directory) {
        try (Stream<Path> list = Files.list(directory)) {
            return list.map(p -> p.getFileName().toString()).collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static void collectStatistics(Scenario scenario) {
        long affected = scenario.services().stream().filter(ServiceSpec::affected).count();
        Statistics.label("services").collect(scenario.services().size() + " services, " + affected + " affected");
        for (ServiceSpec service : scenario.services()) {
            Statistics.label("root state").collect((service.affected() ? "affected " : "clean ") + service.rootState());
            for (Injection injection : service.injections()) {
                Statistics.label("legacy format").collect(injection.kind());
                Statistics.label("identified by").collect(injection.identification());
            }
        }
        Statistics.label("legacy format").coverage(c -> {
            for (LegacyKind kind : LegacyKind.values()) {
                c.check(kind).count(n -> n > 0);
            }
        });
        Statistics.label("identified by").coverage(c -> {
            for (Identification identification : Identification.values()) {
                c.check(identification).count(n -> n > 0);
            }
        });
        Statistics.label("root state").coverage(c -> {
            for (RootState state : RootState.values()) {
                c.check("affected " + state).count(n -> n > 0);
            }
            c.check("clean " + RootState.FRESH).count(n -> n > 0);
        });
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------------------------------------------------

    @Provide
    Arbitrary<Scenario> scenarios() {
        return Arbitraries.integers().between(2, 4).flatMap(count -> {
            List<Arbitrary<ServiceSpec>> services = IntStream.range(0, count).mapToObj(this::service).toList();
            List<Integer> indices = IntStream.range(0, count).boxed().toList();
            return Combinators.combine(services).as(List::copyOf)
                              .filter(list -> list.stream().anyMatch(ServiceSpec::affected))
                              .flatMap(list -> Arbitraries.shuffle(indices)
                                                          .map(order -> new Scenario(list, List.copyOf(order))));
        });
    }

    private Arbitrary<ServiceSpec> service(int index) {
        return Arbitraries.frequency(Tuple.of(3, true), Tuple.of(2, false)).flatMap(affected -> Combinators.combine(
            Arbitraries.of(true, false),
            Arbitraries.of(true, false),
            Arbitraries.of("", "smithy/", "model/v3/"),
            Arbitraries.of(AST_NAMES),
            ignoredFile().list().ofMaxSize(2),
            affected ? injection().list().ofMinSize(1).ofMaxSize(4) : Arbitraries.just(List.<Injection>of()),
            Arbitraries.of(RootState.values())
        ).as((sigv4, split, smithyDirectory, astName, ignored, injections, rootState) ->
                 ServiceDraft.of(index, sigv4, split, smithyDirectory, astName, ignored, injections, rootState))
         .flatMap(draft -> Arbitraries.shuffle(List.copyOf(draft.files().keySet())).map(draft::build)));
    }

    private Arbitrary<Injection> injection() {
        return Combinators.combine(
            Arbitraries.of(LegacyKind.values()),
            Arbitraries.of(Identification.values()),
            Arbitraries.of(DIRECTORIES),
            variation(),
            disguisedName(),
            Arbitraries.of(INCONCLUSIVE_JSON),
            Arbitraries.of(NOT_JSON),
            Arbitraries.integers().between(0, 99)
        ).as(Injection::of);
    }

    private Arbitrary<IgnoredFile> ignoredFile() {
        return Combinators.combine(Arbitraries.of(IGNORED_NAMES), Arbitraries.of(LegacyKind.values()), variation())
                          .as((name, kind, variation) -> new IgnoredFile(name, kind.content(variation).json()));
    }

    /** A {@code .json} or {@code .config} name that is not a V2 file name. */
    private Arbitrary<String> disguisedName() {
        Arbitrary<String> stem = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(8);
        return Combinators.combine(stem, Arbitraries.of(".json", ".config"))
                          .as((s, extension) -> s + extension)
                          .filter(name -> !LegacyKind.ALL_V2_NAMES.contains(name));
    }

    private Arbitrary<Variation> variation() {
        return Combinators.combine(Arbitraries.strings().withCharRange('a', 'z').ofMinLength(3).ofMaxLength(8),
                                   Arbitraries.integers().between(0, 99),
                                   Arbitraries.of(true, false),
                                   Arbitraries.of(true, false),
                                   Arbitraries.integers().between(0, 7))
                          .as(Variation::new);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Fixture model and oracle
    // ---------------------------------------------------------------------------------------------------------------

    /** State of a service's output roots before the generation under test. */
    enum RootState {
        /** No output directory exists. */
        FRESH,
        /** Output roots already contain files placed there by hand. */
        HAND_PLACED,
        /** The canonical files generated successfully before the legacy files were added. */
        PREVIOUS_GENERATION
    }

    /** How the injected legacy file is expected to be identified. */
    enum Identification {
        /** Conclusive content under one of the format's V2 file names. */
        CONTENT_UNDER_V2_NAME,
        /** Conclusive content under a name that is not a V2 file name, or that is another format's V2 file name. */
        CONTENT_UNDER_DISGUISED_NAME,
        /** A V2 file name with valid JSON content that carries no signature. */
        V2_NAME_INCONCLUSIVE_JSON,
        /** A V2 file name with content that is not JSON. */
        V2_NAME_NOT_JSON
    }

    /**
     * The legacy formats, with the diagnostic each must produce. This table is the test's own statement of
     * Requirements 2.3-2.8 (plus V2 smoke tests); it does not read the generator's format enumeration.
     */
    enum LegacyKind {
        C2J_SERVICE(DiagnosticCode.LEGACY_C2J_MODEL, "C2J service model format (service-2.json)",
                    "service-2.json", "api-2.json"),
        C2J_DOCUMENTATION(DiagnosticCode.LEGACY_C2J_MODEL, "C2J documentation model format (docs-2.json)",
                          "docs-2.json"),
        PAGINATORS(DiagnosticCode.LEGACY_PAGINATOR_SIDECAR, "V2 paginator sidecar format (paginators-1.json)",
                   "paginators-1.json", "paginators.json"),
        WAITERS(DiagnosticCode.LEGACY_WAITER_SIDECAR, "V2 waiter sidecar format (waiters-2.json)", "waiters-2.json"),
        ENDPOINT_RULE_SET(DiagnosticCode.LEGACY_ENDPOINT_SIDECAR,
                          "V2 endpoint rule-set sidecar format (endpoint-rule-set-1.json)",
                          "endpoint-rule-set-1.json", "endpoint-rule-set.json"),
        ENDPOINT_BDD(DiagnosticCode.LEGACY_ENDPOINT_SIDECAR, "V2 endpoint BDD sidecar format (endpoint-bdd-1.json)",
                     "endpoint-bdd-1.json"),
        ENDPOINT_TESTS(DiagnosticCode.LEGACY_ENDPOINT_SIDECAR,
                       "V2 endpoint test sidecar format (endpoint-tests-1.json)",
                       "endpoint-tests-1.json", "endpoint-tests.json"),
        EXAMPLES(DiagnosticCode.LEGACY_EXAMPLE_SIDECAR, "V2 example sidecar format (examples-1.json)",
                 "examples-1.json"),
        CUSTOMIZATION(DiagnosticCode.LEGACY_CUSTOMIZATION_SIDECAR,
                      "V2 customization sidecar format (customization.config)", "customization.config"),
        SMOKE_TESTS(DiagnosticCode.LEGACY_SMOKE_TEST_SIDECAR, "V2 smoke-test sidecar format (smoke.json)",
                    "smoke.json");

        static final Set<String> ALL_V2_NAMES = Stream.of(values()).flatMap(k -> k.v2Names.stream())
                                                      .collect(Collectors.toUnmodifiableSet());

        private final DiagnosticCode code;
        private final String element;
        private final List<String> v2Names;

        LegacyKind(DiagnosticCode code, String element, String... v2Names) {
            this.code = code;
            this.element = element;
            this.v2Names = List.of(v2Names);
        }

        /** Content with this format's signature, and how the diagnostic describes its identification. */
        Content content(Variation v) {
            String word = v.word();
            String name = Character.toUpperCase(word.charAt(0)) + word.substring(1);
            List<String> members = new ArrayList<>();
            String detail = BY_CONTENT;
            switch (this) {
                case C2J_SERVICE -> {
                    List<String> metadata = new ArrayList<>();
                    List<String> declared = new ArrayList<>();
                    metadata.add(member("apiVersion", quote("2024-01-0" + (v.number() % 9 + 1))));
                    if ((v.identity() & 1) != 0) {
                        metadata.add(member("serviceId", quote(name)));
                        declared.add("serviceId \"" + name + "\"");
                    }
                    if ((v.identity() & 2) != 0) {
                        metadata.add(member("uid", quote(word + "-2024-01-01")));
                        declared.add("uid \"" + word + "-2024-01-01\"");
                    }
                    if ((v.identity() & 4) != 0) {
                        String protocol = C2J_PROTOCOLS.get(v.number() % C2J_PROTOCOLS.size());
                        metadata.add(member("protocol", quote(protocol)));
                        declared.add("protocol \"" + protocol + "\"");
                    }
                    if (v.number() % 2 == 1) {
                        // Declaration order in the file does not change the reported identity order.
                        Collections.reverse(metadata);
                    }
                    members.add(member("version", quote("2.0")));
                    members.add(member("metadata", inline(metadata)));
                    if (v.number() % 3 != 1) {
                        members.add(member("operations", inline(List.of(member("Get" + name, inline(List.of(
                            member("name", quote("Get" + name)),
                            member("http", "{\"method\": \"POST\", \"requestUri\": \"/\"}"))))))));
                    }
                    if (v.number() % 3 != 0) {
                        members.add(member("shapes", inline(List.of(member(name + "Request",
                            "{\"type\": \"structure\", \"members\": {}}")))));
                    }
                    if (!declared.isEmpty()) {
                        detail = BY_CONTENT + "; declares " + String.join(", ", declared);
                    }
                }
                case C2J_DOCUMENTATION -> {
                    members.add(member("version", quote("2.0")));
                    members.add(member("service", quote("<p>The " + name + " service.</p>")));
                    members.add(member("operations", inline(List.of(member("Get" + name, quote("<p>Gets.</p>"))))));
                    members.add(member("shapes", v.number() % 2 == 0 ? "{}"
                        : inline(List.of(member(name, "{\"base\": \"<p>A shape.</p>\", \"refs\": {}}")))));
                }
                case PAGINATORS -> members.add(member("pagination", v.number() % 4 == 0 ? "{}"
                    : inline(List.of(member("List" + name, "{\"input_token\": \"nextToken\", \"output_token\": "
                                                           + "\"nextToken\", \"limit_key\": \"maxResults\"}")))));
                case WAITERS -> {
                    members.add(member("version", "2"));
                    members.add(member("waiters", inline(List.of(member(name + "Exists",
                        "{\"delay\": " + (v.number() % 30 + 1) + ", \"operation\": \"Describe" + name + "\", "
                        + "\"maxAttempts\": 20, \"acceptors\": [{\"expected\": 200, \"matcher\": \"status\", "
                        + "\"state\": \"success\"}]}")))));
                }
                case ENDPOINT_RULE_SET -> {
                    members.add(member("version", quote("1.0")));
                    members.add(member("parameters", regionParameter()));
                    members.add(member("rules", v.number() % 3 == 0 ? "[]"
                        : "[{\"conditions\": [], \"endpoint\": {\"url\": \"https://" + word
                          + ".example.com\"}, \"type\": \"endpoint\"}]"));
                }
                case ENDPOINT_BDD -> {
                    members.add(member("version", quote("1.1")));
                    members.add(member("parameters", regionParameter()));
                    int present = v.number() % 15 + 1;
                    if ((present & 1) != 0) {
                        members.add(member("conditions", "[]"));
                    }
                    if ((present & 2) != 0) {
                        members.add(member("results", "[]"));
                    }
                    if ((present & 4) != 0) {
                        members.add(member("root", String.valueOf(v.number())));
                    }
                    if ((present & 8) != 0) {
                        members.add(member("nodes", quote("AAAAAQ" + word)));
                    }
                }
                case ENDPOINT_TESTS -> {
                    members.add(member("testCases", v.number() % 3 == 0 ? "[]"
                        : "[{\"documentation\": \"" + word + "\", \"params\": {\"Region\": \"us-east-1\"}, "
                          + "\"expect\": {\"endpoint\": {\"url\": \"https://" + word + ".us-east-1.amazonaws.com\"}}}]"));
                    members.add(member("version", quote("1.0")));
                }
                case SMOKE_TESTS -> {
                    members.add(member("version", "1"));
                    if (v.number() % 2 == 0) {
                        members.add(member("defaultRegion", quote("us-west-2")));
                    }
                    // Without a default region, the first case's operation name identifies smoke tests.
                    members.add(member("testCases", "[{\"operationName\": \"List" + name + "\", \"input\": {}, "
                                                    + "\"errorExpectedFromService\": false}]"));
                }
                case EXAMPLES -> {
                    members.add(member("version", quote("1.0")));
                    members.add(member("examples", v.number() % 2 == 0 ? "{}"
                        : inline(List.of(member("Get" + name, "[{\"id\": \"example-1\", \"input\": {}}]")))));
                }
                case CUSTOMIZATION -> {
                    List<String> properties = List.of(
                        member("verifiedSimpleMethods", "[\"list" + name + "\"]"),
                        member("excludedSimpleMethods", "[\"describe" + name + "\"]"),
                        member("renameShapes", "{\"" + name + "Credentials\": \"Credentials\"}"),
                        member("serviceConfig", "{\"className\": \"" + name + "ServiceClientConfiguration\"}"),
                        member("paginationCustomization", "{\"List" + name + "\": \"LastPageHasPreviousToken\"}"),
                        member("enableEndpointDiscoveryMethodRequired", "true"));
                    int start = v.number() % properties.size();
                    int count = v.identity() % 3 + 1;
                    for (int i = 0; i < count; i++) {
                        members.add(properties.get((start + i) % properties.size()));
                    }
                }
            }
            return new Content(object(v, members), detail);
        }
    }

    private static String regionParameter() {
        return "{\"Region\": {\"builtIn\": \"AWS::Region\", \"required\": false, \"type\": \"String\"}}";
    }

    /**
     * A JSON object; multi-line when {@link Variation#pretty()}, and with line comments before, inside and after the
     * object when {@link Variation#comments()}, as V2 {@code customization.config} files have.
     */
    private static String object(Variation v, List<String> members) {
        boolean multiline = v.pretty() || v.comments();
        StringBuilder out = new StringBuilder();
        if (v.comments()) {
            out.append("// V2 codegen-resources file for ").append(v.word()).append('\n');
        }
        out.append('{');
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            if (multiline) {
                out.append("\n    ");
            }
            if (v.comments()) {
                out.append("// member ").append(i).append(": keep \"quoted\" {braces} in comments\n    ");
            }
            out.append(members.get(i));
        }
        out.append(multiline ? "\n}" : "}");
        if (v.comments()) {
            out.append("\n// end of file\n");
        }
        return out.toString();
    }

    private static String inline(List<String> members) {
        return "{" + String.join(", ", members) + "}";
    }

    private static String member(String name, String json) {
        return quote(name) + ": " + json;
    }

    private static String quote(String value) {
        return "\"" + value + "\"";
    }

    /** Random knobs for generated legacy content. */
    record Variation(String word, int number, boolean pretty, boolean comments, int identity) {
    }

    /** Legacy content and how its diagnostic is expected to describe its identification. */
    record Content(String json, String detail) {
    }

    /** A file that is not a model input (by its name) although it contains legacy content. */
    record IgnoredFile(String name, String content) {
    }

    /**
     * One injected legacy file.
     *
     * @param path path relative to the model directory, before collision resolution
     * @param detail how the diagnostic must describe the identification
     */
    record Injection(LegacyKind kind, Identification identification, String path, String content, String detail) {

        static Injection of(LegacyKind kind, Identification identification, String directory, Variation variation,
                            String disguisedName, String inconclusive, String notJson, int pick) {
            String v2Name = kind.v2Names.get(pick % kind.v2Names.size());
            Content conclusive = kind.content(variation);
            return switch (identification) {
                case CONTENT_UNDER_V2_NAME ->
                    new Injection(kind, identification, directory + v2Name, conclusive.json(), conclusive.detail());
                case CONTENT_UNDER_DISGUISED_NAME -> {
                    String name = disguisedName;
                    if (pick % 2 == 1) {
                        // Another format's V2 file name: content wins.
                        List<String> others = Stream.of(LegacyKind.values()).filter(k -> k != kind)
                                                    .flatMap(k -> k.v2Names.stream()).toList();
                        name = others.get(pick % others.size());
                    }
                    yield new Injection(kind, identification, directory + name, conclusive.json(),
                                        conclusive.detail());
                }
                case V2_NAME_INCONCLUSIVE_JSON ->
                    new Injection(kind, identification, directory + v2Name, inconclusive, BY_FILE_NAME);
                case V2_NAME_NOT_JSON ->
                    new Injection(kind, identification, directory + v2Name, notJson, BY_FILE_NAME_NOT_JSON);
            };
        }

        Injection at(String resolvedPath) {
            return new Injection(kind, identification, resolvedPath, content, detail);
        }
    }

    /** The structured identity of a source diagnostic: what, for which service, in which file, which format. */
    record DiagnosticKey(DiagnosticCode code, String service, String source, String element) {
        static DiagnosticKey of(Diagnostic d) {
            return new DiagnosticKey(d.code(), d.service(), d.source().orElse(null), d.element());
        }
    }

    /**
     * The oracle's diagnostic for one injected file.
     *
     * @param detail how the file must be reported as identified, checked through the message
     */
    record ExpectedDiagnostic(DiagnosticKey key, String detail) {
    }

    /** A service's files before its creation order is chosen. */
    record ServiceDraft(int index, Map<String, String> files, List<Injection> injections, RootState rootState) {

        /** Valid Smithy files (authenticated with SigV4 or anonymous), an optional JSON AST, ignored and legacy files. */
        static ServiceDraft of(int index, boolean sigv4, boolean split, String smithyDirectory, String astName,
                               List<IgnoredFile> ignored, List<Injection> injections, RootState rootState) {
            Map<String, String> files = new LinkedHashMap<>();
            String namespace = "example.svc" + index;
            String header = "$version: \"2\"\nnamespace " + namespace + "\n\n";
            String service = (sigv4 ? "@aws.auth#sigv4(name: \"svc" + index + "\")\n" : "@auth([])\n")
                             + "@aws.protocols#awsJson1_0\n"
                             + "service Svc" + index + " {\n    version: \"2024-01-01\"\n    operations: [Ping]\n}\n";
            String operation = "operation Ping {\n    input := { value: String }\n    output := { value: String }\n}\n";
            if (split) {
                put(files, smithyDirectory + "main.smithy", header + service);
                put(files, smithyDirectory + "operations/ping.smithy", header + operation);
            } else {
                put(files, smithyDirectory + "model.smithy", header + service + "\n" + operation);
            }
            if (!astName.isEmpty()) {
                put(files, astName, "{\n  \"smithy\": \"2.0\",\n  \"shapes\": {\n    \"" + namespace + "#Extra\": {"
                                    + "\"type\": \"structure\", \"members\": {\"note\": {\"target\": "
                                    + "\"smithy.api#String\"}}}\n  }\n}\n");
            }
            ignored.forEach(file -> put(files, file.name(), file.content()));
            List<Injection> placed = new ArrayList<>();
            for (Injection injection : injections) {
                placed.add(injection.at(put(files, injection.path(), injection.content())));
            }
            return new ServiceDraft(index, files, placed, rootState);
        }

        /** Adds {@code content} at {@code path}, moving it into a fresh subdirectory if the path is taken. */
        private static String put(Map<String, String> files, String path, String content) {
            String resolved = path;
            for (int i = 0; files.containsKey(resolved); i++) {
                resolved = "dup" + i + "/" + path;
            }
            files.put(resolved, content);
            return resolved;
        }

        ServiceSpec build(List<String> creationOrder) {
            return new ServiceSpec(index, Collections.unmodifiableMap(files), List.copyOf(injections), rootState,
                                   List.copyOf(creationOrder));
        }
    }

    /**
     * One service of a scenario.
     *
     * @param files every model-directory file (relative path to content)
     * @param injections the legacy files among {@code files}; empty for a clean service
     * @param creationOrder the order in which the files are created
     */
    record ServiceSpec(int index, Map<String, String> files, List<Injection> injections, RootState rootState,
                       List<String> creationOrder) {

        boolean affected() {
            return !injections.isEmpty();
        }

        String serviceId() {
            return "example.svc" + index + "#Svc" + index;
        }

        String packageName() {
            return "software.amazon.awssdk.services.svc" + index;
        }

        String packagePath() {
            return packageName().replace('.', '/');
        }

        Set<String> injectedPaths() {
            return injections.stream().map(Injection::path).collect(Collectors.toUnmodifiableSet());
        }

        /** One diagnostic per injected file, in the documented order: by file path. */
        List<ExpectedDiagnostic> expectedDiagnostics() {
            return injections.stream()
                             .map(i -> new ExpectedDiagnostic(
                                 new DiagnosticKey(i.kind().code, serviceId(), i.path(), i.kind().element), i.detail()))
                             .sorted(Comparator.comparing(e -> e.key().source()))
                             .toList();
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder(serviceId()).append(" (").append(rootState).append(')');
            creationOrder.forEach(path -> {
                Injection injection = injections.stream().filter(i -> i.path().equals(path)).findFirst().orElse(null);
                out.append("\n    ").append(path);
                if (injection != null) {
                    out.append("  <- ").append(injection.kind()).append(" / ").append(injection.identification());
                }
                out.append(": ").append(files.get(path).replace("\n", "\\n"));
            });
            return out.toString();
        }
    }

    /**
     * Services of one try and the order in which they are generated.
     */
    record Scenario(List<ServiceSpec> services, List<Integer> generationOrder) {
        @Override
        public String toString() {
            return "generation order " + generationOrder + services.stream().map(s -> "\n  " + s)
                                                                   .collect(Collectors.joining());
        }
    }

    record Outcome(AwsCodegenResult result, AwsCodegenException failure) {
    }

    /** The published source and resource trees of a service work directory. */
    record Trees(GeneratedTree sources, GeneratedTree resources) {
        static Trees read(Path work) {
            return new Trees(GeneratedTree.read(work.resolve(SOURCES)), GeneratedTree.read(work.resolve(RESOURCES)));
        }
    }
}
