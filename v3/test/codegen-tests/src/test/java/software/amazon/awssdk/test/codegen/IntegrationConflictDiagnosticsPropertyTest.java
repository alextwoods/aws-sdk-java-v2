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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
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
import software.amazon.awssdk.awscodegen.AwsCodegenContext;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.awssdk.awscodegen.AwsCodegenIntegration;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.awssdk.awscodegen.validation.DiagnosticCode;
import software.amazon.awssdk.test.codegen.support.GeneratedTree;
import software.amazon.awssdk.test.codegen.support.Generation;
import software.amazon.awssdk.test.codegen.support.SmithyModelFixture;
import software.amazon.awssdk.test.codegen.support.TestIntegration;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Property 6 (Requirement 3.7): whenever code-generation integrations produce conflicting output or settings,
 * generation fails with diagnostics that name every integration involved and the conflicting Smithy shape, and
 * publishes nothing.
 *
 * <p>Each try generates the {@linkplain SmithyModelFixture#weather() weather} service with one to four integrations
 * that have random identifiers and orders. Every integration is described by plain actions, drawn from small pools so
 * that they collide often:
 * <ul>
 *   <li>{@code preprocessModel}: set the documentation or deprecated trait of a shape or member (values include the
 *       fixture's own, so re-setting an unchanged value occurs), or add a member to a structure (a definition
 *       change);</li>
 *   <li>{@code decorateSymbolProvider}: rename a structure's, an error's or a list's Java type (including to its own
 *       name or to another shape's name; a renamed list gets a model type and definition file no directive renders)
 *       or a member's Java name, add a symbol property only, or decorate without changing anything;</li>
 *   <li>{@code customize}: write a file through the file manifest, a writer from {@code useFileWriter}, or a shape's
 *       writer from {@code useShapeWriter}; paths include the files the core generator owns, among them the model
 *       types it renders for structures and errors (which symbol renames move).</li>
 * </ul>
 *
 * <p>{@link Oracle} predicts the exact diagnostics from the action descriptions alone, without the generator's
 * ledger: it replays the actions in {@code (order, identifier)} order over plain value maps (trait values, member
 * targets, type and member names), and knows the fixture's generated files, the V2 names of its model types, the
 * directive rendering each generated file, and the definition files independently.
 * Setting conflicts (model and symbol) fail generation when the codegen context is created, before any file is
 * generated; without them, generated-name collisions of the final symbols (a structure or error renamed onto another
 * generated type's name, {@code GENERATED_NAME_COLLISION}) fail it at the same point. Output conflicts are predicted
 * only for tries with neither. The property then checks
 * that generation fails exactly when a conflict is predicted, that the diagnostics are exactly the predicted
 * {@code (code, shape, integrations, element)} set, that each rendered diagnostic names all its integrations and the
 * shape, that a previously published tree is left untouched without staging leftovers, that a successful generation
 * publishes exactly the predicted source paths, and that a second run with the integrations enumerated in a shuffled
 * order gives the same result.
 */
class IntegrationConflictDiagnosticsPropertyTest {

    private static final SmithyModelFixture FIXTURE = SmithyModelFixture.weather();
    private static final String GENERATOR = AwsCodegen.GENERATOR_IDENTIFIER;
    /** Parent of the per-try work directories, inside the module's build output (surefire runs in the module). */
    private static final Path TRIALS = Path.of("target", "property6-trials").toAbsolutePath();

    private static final String NAMESPACE = "example.weather#";
    private static final String SERVICE = SmithyModelFixture.SERVICE;
    private static final String FORECAST = NAMESPACE + "Forecast";
    private static final String CITY_NOT_FOUND = NAMESPACE + "CityNotFound";
    private static final String GET_FORECAST = NAMESPACE + "GetForecast";
    private static final String LIST_CITIES = NAMESPACE + "ListCities";
    /** A list shape: it has no Java type or definition file of its own until an integration's symbol gives it one. */
    private static final String CITY_LIST = NAMESPACE + "CityList";
    private static final String FORECAST_SUMMARY = FORECAST + "$summary";
    private static final String FORECAST_HIGH = FORECAST + "$high";

    private static final String PACKAGE_PATH = SmithyModelFixture.PACKAGE_PATH;
    private static final String MODEL_PACKAGE = SmithyModelFixture.PACKAGE + ".model";
    private static final String MODEL_DIRECTORY = PACKAGE_PATH + "/model";
    private static final String OPERATION_DIRECTORY = PACKAGE_PATH + "/internal/operation";
    private static final String SERVICE_INFO = PACKAGE_PATH + "/GeneratedServiceInfo.java";
    private static final String OPERATION_TABLE = OPERATION_DIRECTORY + "/WeatherOperations.java";
    private static final String GET_FORECAST_DESCRIPTOR = OPERATION_DIRECTORY + "/GetForecastOperation.java";
    private static final String LIST_CITIES_DESCRIPTOR = OPERATION_DIRECTORY + "/ListCitiesOperation.java";
    private static final String SCHEMA_HOLDER = MODEL_DIRECTORY + "/internal/WeatherSchemas.java";
    /** The public service metadata and the primary type registry, which the service directive renders. */
    private static final String SERVICE_METADATA = PACKAGE_PATH + "/WeatherServiceMetadata.java";
    private static final String TYPE_REGISTRY = MODEL_DIRECTORY + "/internal/WeatherTypeRegistry.java";
    /** The service's base exception, which no shape defines: the service directive renders it. */
    private static final String SERVICE_EXCEPTION = MODEL_DIRECTORY + "/WeatherException.java";

    /**
     * Source files the core generator emits for the fixture whatever the symbol renames, with the shape whose
     * directive renders each. The generator also emits one model type per structure and error, see
     * {@link #MODEL_TYPES}.
     */
    private static final Map<String, String> FIXED_GENERATOR_FILES = new TreeMap<>(Map.of(
        SERVICE_INFO, SERVICE,
        SCHEMA_HOLDER, SERVICE,
        OPERATION_TABLE, SERVICE,
        SERVICE_EXCEPTION, SERVICE,
        SERVICE_METADATA, SERVICE,
        TYPE_REGISTRY, SERVICE,
        GET_FORECAST_DESCRIPTOR, GET_FORECAST,
        LIST_CITIES_DESCRIPTOR, LIST_CITIES));

    /**
     * Structures and errors of the service closure (it has no unions or enums), by shape ID, with their V2 Java type
     * names: errors are {@code <Name>Exception} (a trailing {@code Fault} becomes {@code Exception}), and the input
     * and output of one operation are {@code <Operation>Request} and {@code <Operation>Response}. The directive of
     * each renders {@code model/<Java name>.java}, where a symbol rename moves it.
     */
    private static final Map<String, String> MODEL_TYPES = new TreeMap<>(Map.of(
        NAMESPACE + "AccessDenied", "AccessDeniedException",
        CITY_NOT_FOUND, "CityNotFoundException",
        FORECAST, "Forecast",
        NAMESPACE + "GetForecastInput", "GetForecastRequest",
        NAMESPACE + "GetForecastOutput", "GetForecastResponse",
        NAMESPACE + "ListCitiesInput", "ListCitiesRequest",
        NAMESPACE + "ListCitiesOutput", "ListCitiesResponse",
        NAMESPACE + "ServiceFault", "ServiceException",
        NAMESPACE + "Throttled", "ThrottledException"));

    /** Trait values of the fixture before any integration runs, by {@code shape + " " + aspect}. */
    private static final Map<String, String> FIXTURE_TRAITS = Map.of(
        FORECAST + " " + TraitKind.DOCUMENTATION.aspect(), "A forecast.",
        GET_FORECAST + " " + TraitKind.DOCUMENTATION.aspect(), "Gets a forecast.");

    private static final List<String> TRAIT_TARGETS = List.of(FORECAST, CITY_NOT_FOUND, GET_FORECAST, FORECAST_SUMMARY);
    private static final List<String> TRAIT_VALUES = List.of("A forecast.", "Gets a forecast.", "Changed.");
    private static final List<String> MEMBER_CONTAINERS = List.of(FORECAST, CITY_NOT_FOUND);
    private static final List<String> NEW_MEMBERS = List.of("extra", "other");
    private static final List<String> MEMBER_TARGETS = List.of("smithy.api#String", "smithy.api#Integer");
    /** A structure and an error whose generated Java type a decorator renames, which moves the generated file. */
    private static final List<String> RENAMED_TYPES = List.of(FORECAST, CITY_NOT_FOUND);
    /** A type name no shape has, and its file, which no shape defines unless a rename moves a type there. */
    private static final String REPORT = "Report";
    private static final String REPORT_PATH = MODEL_DIRECTORY + "/" + REPORT + ".java";
    /**
     * New names of {@link #RENAMED_TYPES}: each shape's own V2 name (an unchanged rename), the error's name without
     * its {@code Exception} suffix (a change), a free name, and another error's name (a generated-name collision).
     */
    private static final List<String> TYPE_NAMES =
        List.of("Forecast", "CityNotFound", "CityNotFoundException", REPORT, "ThrottledException");
    // CityList is also renamed, to Report or Forecast: that gives the list a model type and a definition file that no
    // directive renders (Report.java), or shares the structure's generated file (Forecast.java).
    private static final List<String> RENAMED_MEMBERS = List.of(FORECAST_SUMMARY, FORECAST_HIGH);
    private static final List<String> MEMBER_NAMES = List.of("summary", "high", "headline");
    private static final List<String> PROPERTY_SHAPES = List.of(FORECAST, GET_FORECAST);
    private static final List<String> GENERATOR_PATHS = List.copyOf(FIXED_GENERATOR_FILES.keySet());
    /**
     * Model-type files the generator renders without renames; which shape defines each, and whether the generator
     * renders it, depends on the symbol renames (as for {@link #REPORT_PATH}).
     */
    private static final List<String> MODEL_PATHS = List.of(
        MODEL_DIRECTORY + "/Forecast.java", MODEL_DIRECTORY + "/CityNotFoundException.java");
    private static final List<String> RESOURCE_PATHS = List.of("META-INF/p6/shared.txt", "META-INF/p6/other.properties");
    private static final List<String> WRITER_SHAPES = List.of(SERVICE, LIST_CITIES, FORECAST, CITY_NOT_FOUND);
    private static final List<String> FIXED_IDENTIFIERS =
        List.of("aws.alpha", "aws.beta", "aws-codegen0", "aws-codegen.extra", "aa");

    // Feature: aws-sdk-java-v3, Property 6: Integration conflicts are complete diagnostics
    // Embedded edge cases are disabled: they collapse most choices to their first alternative (no actions), starving
    // conflicting tries. The statistics coverage checks fail the property if the generated mix degrades.
    @Property(tries = 300, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 6: Integration conflicts are complete diagnostics")
    void integrationConflictsAreCompleteDiagnostics(@ForAll("scenarios") Scenario scenario) throws IOException {
        Prediction expected = Oracle.predict(scenario);
        Path trial = Files.createTempDirectory(Files.createDirectories(TRIALS), "try-");
        try {
            List<AwsCodegenIntegration> enumerated = scenario.specs().stream().map(Scenario::integration).toList();
            List<AwsCodegenIntegration> shuffled = new ArrayList<>(enumerated);
            Collections.shuffle(shuffled, new Random(scenario.enumerationSeed()));

            Outcome first = generate(trial.resolve("first"), enumerated);
            if (expected.conflicts().isEmpty()) {
                assertNull(first.failure(), () -> "No conflict is predicted, but generation failed: "
                                                   + first.failure().getMessage());
                // The oracle predicts the integration and model-type sources; the generated clients are added
                // to every service (GeneratedClientTest covers them).
                assertEquals(expected.sources(), withoutClients(first.sources().paths()), "Published source paths");
            } else {
                assertNotNull(first.failure(), () -> "Generation succeeded although conflicts are predicted: "
                                                      + expected.conflicts());
                assertEquals(List.copyOf(expected.conflicts()), actualConflicts(first.failure()),
                             () -> "Conflict diagnostics:\n  " + String.join(
                                 "\n  ", first.failure().diagnostics().stream().map(Diagnostic::render).toList()));
                // The single-line rendering in the exception message and build log names the shape and every
                // involved integration (the lists themselves were compared with the oracle above).
                for (Diagnostic diagnostic : first.failure().diagnostics()) {
                    String rendered = diagnostic.render();
                    assertEquals(SERVICE, diagnostic.service(), rendered);
                    assertTrue(rendered.contains(", shape " + diagnostic.shape().orElseThrow() + ", integrations "
                                                 + diagnostic.integrations() + ", element "), rendered);
                    assertTrue(first.failure().getMessage().contains(rendered), first.failure().getMessage());
                }
            }

            Outcome second = generate(trial.resolve("second"), shuffled);
            if (first.failure() == null) {
                assertNull(second.failure(), "Shuffled enumeration order must not change the outcome");
                GeneratedTree.assertByteIdentical(first.sources(), second.sources());
            } else {
                assertNotNull(second.failure(), "Shuffled enumeration order must not change the outcome");
                assertEquals(first.failure().diagnostics(), second.failure().diagnostics(),
                             "Shuffled enumeration order must not change the diagnostics");
            }
            collectStatistics(expected);
        } finally {
            deleteRecursively(trial);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Running generation

    /** A generation outcome: the published source tree, or the failure. */
    private record Outcome(GeneratedTree sources, AwsCodegenException failure) {
    }

    /**
     * Generates into {@code work} over a previously published tree. On failure, checks that the previous tree is
     * untouched and that no staging or previous-tree directory remains.
     */
    private static Outcome generate(Path work, List<AwsCodegenIntegration> integrations) throws IOException {
        Path sources = Files.createDirectories(Generation.sourceRoot(work));
        Path resources = Files.createDirectories(Generation.resourceRoot(work));
        Files.writeString(sources.resolve("Existing.java"), "class Existing {}");
        Files.writeString(resources.resolve("existing.txt"), "existing");
        try {
            Generation generation = Generation.run(work, FIXTURE, FIXTURE.files(), integrations);
            assertTrue(!generation.resources().isEmpty(), "A successful generation publishes its resources");
            return new Outcome(generation.sources(), null);
        } catch (AwsCodegenException e) {
            assertEquals(Set.of("Existing.java"), GeneratedTree.read(sources).paths(),
                         "The previously published source tree is untouched");
            assertEquals(Set.of("existing.txt"), GeneratedTree.read(resources).paths(),
                         "The previously published resource tree is untouched");
            try (Stream<Path> siblings = Files.list(work.resolve("out"))) {
                assertEquals(List.of("resources", "src"), siblings.map(p -> p.getFileName().toString()).sorted()
                                                                  .toList(),
                             "No staging or previous-tree directory remains");
            }
            return new Outcome(null, e);
        }
    }

    private static List<Conflict> actualConflicts(AwsCodegenException failure) {
        return failure.diagnostics().stream()
                      .map(d -> new Conflict(d.code(), d.shape().orElse("<none>"), d.integrations(), d.element()))
                      .sorted(Conflict.ORDER)
                      .toList();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Scenario description

    /** A trait an integration sets while preprocessing the model. */
    enum TraitKind {
        DOCUMENTATION("smithy.api#documentation"),
        DEPRECATED("smithy.api#deprecated");

        private final String id;

        TraitKind(String id) {
            this.id = id;
        }

        /** The conflict element of a change of this trait. */
        String aspect() {
            return "trait " + id;
        }

        Trait create(String value) {
            return this == DOCUMENTATION ? new DocumentationTrait(value) : DeprecatedTrait.builder().message(value).build();
        }
    }

    /** One change an integration's {@code preprocessModel} applies. */
    sealed interface ModelAction permits SetTrait, AddMember {
    }

    record SetTrait(String shape, TraitKind trait, String value) implements ModelAction {
        @Override
        public String toString() {
            return "set " + trait + "='" + value + "' on " + shape;
        }
    }

    /** Adds (or retargets a previously added) member of a structure: a definition change. */
    record AddMember(String container, String member, String target) implements ModelAction {
        @Override
        public String toString() {
            return "add " + container + "$" + member + " -> " + target;
        }
    }

    /** One change an integration's symbol-provider decorator applies. */
    sealed interface SymbolAction permits RenameType, RenameMember, AddProperty {
    }

    record RenameType(String shape, String name) implements SymbolAction {
        @Override
        public String toString() {
            return "rename " + shape + " to " + name;
        }
    }

    record RenameMember(String member, String name) implements SymbolAction {
        @Override
        public String toString() {
            return "rename member " + member + " to " + name;
        }
    }

    /** Adds a symbol property, keeping the symbol identity. */
    record AddProperty(String shape) implements SymbolAction {
        @Override
        public String toString() {
            return "add property to " + shape;
        }
    }

    /** One file an integration's {@code customize} writes. */
    sealed interface Write permits ManifestWrite, FileWriterWrite, ShapeWriterWrite {
    }

    record ManifestWrite(String path) implements Write {
        @Override
        public String toString() {
            return "fileManifest " + path;
        }
    }

    record FileWriterWrite(String path) implements Write {
        @Override
        public String toString() {
            return "useFileWriter " + path;
        }
    }

    record ShapeWriterWrite(String shape) implements Write {
        @Override
        public String toString() {
            return "useShapeWriter " + shape;
        }
    }

    /** One integration: identifier, order, and the actions of each hook ({@code decorates} with no-op actions). */
    record Spec(String id, int order, List<ModelAction> preprocess, boolean decorates, List<SymbolAction> symbols,
                List<Write> writes) {
        @Override
        public String toString() {
            return id + "(order " + order + ", preprocess " + preprocess + ", "
                   + (decorates ? "decorates " + symbols : "no decorator") + ", writes " + writes + ")";
        }
    }

    /**
     * The integrations of one try, in enumeration order, and the seed of the shuffled enumeration of the second run.
     */
    record Scenario(List<Spec> specs, long enumerationSeed) {

        /** The integration performing exactly the actions of {@code spec}. */
        static AwsCodegenIntegration integration(Spec spec) {
            TestIntegration integration = TestIntegration.named(spec.id()).withOrder(spec.order());
            if (!spec.preprocess().isEmpty()) {
                integration = integration.withPreprocess(model -> preprocess(model, spec.preprocess()));
            }
            if (spec.decorates()) {
                integration = integration.withSymbolDecorator(decorator(spec.symbols()));
            }
            if (!spec.writes().isEmpty()) {
                integration = integration.withCustomize(context -> customize(context, spec));
            }
            return integration;
        }

        private static Model preprocess(Model model, List<ModelAction> actions) {
            Model result = model;
            for (ModelAction action : actions) {
                result = switch (action) {
                    case SetTrait set -> {
                        Shape shape = result.expectShape(ShapeId.from(set.shape()));
                        yield ModelTransformer.create().replaceShapes(result, List.of(
                            Shape.shapeToBuilder(shape).addTrait(set.trait().create(set.value())).build()));
                    }
                    case AddMember add -> {
                        StructureShape container = result.expectShape(ShapeId.from(add.container()),
                                                                      StructureShape.class);
                        yield ModelTransformer.create().replaceShapes(result, List.of(
                            container.toBuilder().addMember(add.member(), ShapeId.from(add.target())).build()));
                    }
                };
            }
            return result;
        }

        /** A decorator applying the last rename of each shape and member, and the property additions. */
        private static UnaryOperator<SymbolProvider> decorator(List<SymbolAction> actions) {
            Map<ShapeId, String> typeNames = new HashMap<>();
            Map<ShapeId, String> memberNames = new HashMap<>();
            Set<ShapeId> properties = new TreeSet<>();
            for (SymbolAction action : actions) {
                switch (action) {
                    case RenameType rename -> typeNames.put(ShapeId.from(rename.shape()), rename.name());
                    case RenameMember rename -> memberNames.put(ShapeId.from(rename.member()), rename.name());
                    case AddProperty add -> properties.add(ShapeId.from(add.shape()));
                }
            }
            return delegate -> new SymbolProvider() {
                @Override
                public Symbol toSymbol(Shape shape) {
                    Symbol symbol = delegate.toSymbol(shape);
                    String name = typeNames.get(shape.getId());
                    if (name != null) {
                        // A model type in the model package; for the list this also gives it a definition file.
                        symbol = symbol.toBuilder().namespace(MODEL_PACKAGE, ".").name(name)
                                       .definitionFile(MODEL_DIRECTORY + "/" + name + ".java").build();
                    }
                    if (properties.contains(shape.getId())) {
                        symbol = symbol.toBuilder().putProperty("property6", shape.getId().toString()).build();
                    }
                    return symbol;
                }

                @Override
                public String toMemberName(MemberShape shape) {
                    String name = memberNames.get(shape.getId());
                    return name != null ? name : delegate.toMemberName(shape);
                }
            };
        }

        private static void customize(AwsCodegenContext context, Spec spec) {
            for (Write write : spec.writes()) {
                switch (write) {
                    case ManifestWrite manifest ->
                        context.fileManifest().writeFile(manifest.path(), "written by " + spec.id() + "\n");
                    case FileWriterWrite file -> context.writerDelegator().useFileWriter(
                        file.path(), file.path().substring(0, file.path().lastIndexOf('/')).replace('/', '.'),
                        writer -> writer.write("// written by $L", spec.id()));
                    case ShapeWriterWrite shape -> context.writerDelegator().useShapeWriter(
                        context.model().expectShape(ShapeId.from(shape.shape())),
                        writer -> writer.write("// written by $L for $L", spec.id(), shape.shape()));
                }
            }
        }

        @Override
        public String toString() {
            return "Scenario" + specs + " shuffled with seed " + enumerationSeed;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Oracle

    /** One predicted (or observed) conflict diagnostic, without its message. */
    record Conflict(DiagnosticCode code, String shape, List<String> integrations, String element) {
        static final Comparator<Conflict> ORDER = Comparator.comparing(Conflict::toString);

        @Override
        public String toString() {
            return code + " shape " + shape + " integrations " + integrations + " element " + element;
        }
    }

    /**
     * The predicted outcome.
     *
     * @param conflicts every predicted diagnostic; empty when generation succeeds
     * @param sources the published source paths when generation succeeds
     * @param settingPhase whether the conflicts are model/symbol conflicts found before any file is generated
     * @param features scenario features observed while predicting, for statistics
     */
    record Prediction(SortedSet<Conflict> conflicts, SortedSet<String> sources, boolean settingPhase,
                      Set<String> features) {
    }

    /**
     * Predicts the diagnostics from the action descriptions by replaying them over plain value maps. It shares
     * nothing with the generator's ledger except the documented conflict rules.
     */
    static final class Oracle {

        private final SortedSet<Conflict> conflicts = new TreeSet<>(Conflict.ORDER);
        private final Set<String> features = new TreeSet<>();
        /** Shape to the integrations whose decorator changed its Java type name, in execution order. */
        private final Map<String, SortedSet<String>> typeChangers = new TreeMap<>();

        private Oracle() {
        }

        static Prediction predict(Scenario scenario) {
            List<Spec> ordered = new ArrayList<>(scenario.specs());
            ordered.sort(Comparator.comparingInt(Spec::order).thenComparing(Spec::id));
            Oracle oracle = new Oracle();
            oracle.modelConflicts(ordered);
            Map<String, String> typeNames = oracle.symbolConflicts(ordered);
            if (!oracle.conflicts.isEmpty()) {
                return new Prediction(oracle.conflicts, new TreeSet<>(), true, oracle.features);
            }
            // Checked next, on the final symbols and still before any file is generated.
            oracle.nameCollisions(typeNames);
            if (!oracle.conflicts.isEmpty()) {
                return new Prediction(oracle.conflicts, new TreeSet<>(), true, oracle.features);
            }
            SortedSet<String> sources = oracle.outputConflicts(ordered, typeNames);
            return new Prediction(oracle.conflicts, oracle.conflicts.isEmpty() ? sources : new TreeSet<>(), false,
                                  oracle.features);
        }

        /**
         * Replays every integration's preprocessing over the trait values ({@code shape + " " + aspect}) and added
         * member targets ({@code container$member}), recording the aspects whose net value it changed.
         */
        private void modelConflicts(List<Spec> ordered) {
            Map<String, String> traits = new HashMap<>(FIXTURE_TRAITS);
            Map<String, String> addedMembers = new HashMap<>();
            // shape -> aspect -> integrations that changed it
            Map<String, Map<String, SortedSet<String>>> changes = new TreeMap<>();
            for (Spec spec : ordered) {
                Map<String, String> traitsBefore = new HashMap<>(traits);
                Map<String, String> membersBefore = new HashMap<>(addedMembers);
                for (ModelAction action : spec.preprocess()) {
                    switch (action) {
                        case SetTrait set -> {
                            String key = set.shape() + " " + set.trait().aspect();
                            if (set.value().equals(traits.get(key))) {
                                features.add("same-value trait re-set");
                            }
                            traits.put(key, set.value());
                        }
                        case AddMember add -> addedMembers.put(add.container() + "$" + add.member(), add.target());
                    }
                }
                traits.forEach((key, value) -> {
                    if (!value.equals(traitsBefore.get(key))) {
                        int split = key.indexOf(' ');
                        record(changes, key.substring(0, split), key.substring(split + 1), spec.id());
                    }
                });
                addedMembers.forEach((member, target) -> {
                    if (!target.equals(membersBefore.get(member))) {
                        record(changes, member.substring(0, member.indexOf('$')), "definition", spec.id());
                        record(changes, member, "definition", spec.id());
                    }
                });
            }
            changes.forEach((shape, aspects) -> {
                SortedSet<String> definers = aspects.getOrDefault("definition", new TreeSet<>());
                aspects.forEach((aspect, changers) -> {
                    SortedSet<String> involved = new TreeSet<>(changers);
                    if (!aspect.equals("definition")) {
                        involved.addAll(definers);
                        if (!definers.isEmpty() && !definers.equals(changers) && involved.size() > 1) {
                            features.add("definition change conflicts with trait change");
                        }
                    }
                    if (involved.size() > 1) {
                        add(DiagnosticCode.INTEGRATION_MODEL_CONFLICT, shape, involved, aspect);
                    }
                });
            });
        }

        /**
         * Replays the symbol decorators, in execution order, over the Java type and member names; returns the final
         * type names of the renamed shapes.
         */
        private Map<String, String> symbolConflicts(List<Spec> ordered) {
            // The list has no model type name, so every rename of it changes its symbol.
            Map<String, String> typeNames = new HashMap<>(MODEL_TYPES);
            Map<String, String> memberNames = new HashMap<>();
            RENAMED_MEMBERS.forEach(member -> memberNames.put(member, member.substring(member.indexOf('$') + 1)));
            Map<String, SortedSet<String>> memberChangers = new TreeMap<>();
            for (Spec spec : ordered) {
                if (!spec.decorates()) {
                    continue;
                }
                if (spec.symbols().isEmpty()) {
                    features.add("no-op symbol decorator");
                }
                Map<String, String> types = new LinkedHashMap<>();
                Map<String, String> members = new LinkedHashMap<>();
                for (SymbolAction action : spec.symbols()) {
                    switch (action) {
                        case RenameType rename -> types.put(rename.shape(), rename.name());
                        case RenameMember rename -> members.put(rename.member(), rename.name());
                        case AddProperty add -> features.add("property-only symbol change");
                    }
                }
                types.forEach((shape, name) -> {
                    if (!name.equals(typeNames.put(shape, name))) {
                        typeChangers.computeIfAbsent(shape, s -> new TreeSet<>()).add(spec.id());
                    } else {
                        features.add("unchanged symbol rename");
                    }
                });
                members.forEach((member, name) -> {
                    if (!name.equals(memberNames.put(member, name))) {
                        memberChangers.computeIfAbsent(member, s -> new TreeSet<>()).add(spec.id());
                    }
                });
            }
            typeChangers.forEach((shape, changers) -> {
                if (changers.size() > 1) {
                    add(DiagnosticCode.INTEGRATION_SYMBOL_CONFLICT, shape, changers, "symbol");
                }
            });
            memberChangers.forEach((member, changers) -> {
                if (changers.size() > 1) {
                    add(DiagnosticCode.INTEGRATION_SYMBOL_CONFLICT, member, changers, "member-name");
                }
            });
            return typeNames;
        }

        /**
         * Predicts the generated-name collisions of the final type names: the generated model types (a renamed list
         * is not one) and the service's base exception, which share the model package, grouped case-insensitively.
         * Each set is one diagnostic for its lowest shape ID, contributed by the integration that renamed each
         * colliding type, or the core generator for a type it named itself.
         */
        private void nameCollisions(Map<String, String> typeNames) {
            Map<String, SortedMap<String, String>> byName = new TreeMap<>();
            MODEL_TYPES.keySet().forEach(shape -> byName.computeIfAbsent(
                typeNames.get(shape).toLowerCase(Locale.ROOT), k -> new TreeMap<>()).put(shape, typeNames.get(shape)));
            byName.computeIfAbsent("weatherexception", k -> new TreeMap<>()).put(SERVICE, "WeatherException");
            byName.values().stream().filter(group -> group.size() > 1).forEach(group -> {
                SortedSet<String> contributors = new TreeSet<>();
                group.keySet().forEach(shape -> contributors.addAll(
                    typeChangers.getOrDefault(shape, new TreeSet<>(Set.of(GENERATOR)))));
                features.add(contributors.contains(GENERATOR) ? "type renamed onto a generator-named type"
                                                              : "two types renamed onto one name");
                add(DiagnosticCode.GENERATED_NAME_COLLISION, group.firstKey(), contributors,
                    "type " + MODEL_PACKAGE + "." + group.get(group.firstKey()));
            });
        }

        /** Claims of one generated file. */
        private static final class Claims {
            final SortedSet<String> contributors = new TreeSet<>();
            final SortedSet<String> renderedShapes = new TreeSet<>();
            int manifestWrites;
            boolean writer;

            void manifest(String contributor) {
                contributors.add(contributor);
                manifestWrites++;
            }

            void writer(String contributor, String renderedShape) {
                contributors.add(contributor);
                writer = true;
                if (renderedShape != null) {
                    renderedShapes.add(renderedShape);
                }
            }
        }

        /**
         * Predicts the output conflicts from the final type names; returns every source path that would be
         * published.
         */
        private SortedSet<String> outputConflicts(List<Spec> ordered, Map<String, String> typeNames) {
            Map<String, String> definitionFiles = definitionFiles(typeNames);
            Map<String, Claims> files = new TreeMap<>();
            FIXED_GENERATOR_FILES.forEach((path, shape) -> files.computeIfAbsent(path, p -> new Claims())
                                                                .writer(GENERATOR, shape));
            // Each structure or error directive renders its type's file; renames can put two types in one file.
            MODEL_TYPES.keySet().forEach(shape -> files.computeIfAbsent(definitionFiles.get(shape), p -> new Claims())
                                                       .writer(GENERATOR, shape));
            if (MODEL_TYPES.entrySet().stream().anyMatch(e -> !e.getValue().equals(typeNames.get(e.getKey())))) {
                features.add("generated model type moved by a rename");
            }
            if (definitionFiles.values().stream().distinct().count() < definitionFiles.size()) {
                features.add("two shapes defined in one file");
            }
            for (Spec spec : ordered) {
                for (Write write : spec.writes()) {
                    switch (write) {
                        case ManifestWrite manifest -> files.computeIfAbsent(manifest.path(), p -> new Claims())
                                                            .manifest(spec.id());
                        case FileWriterWrite file -> files.computeIfAbsent(file.path(), p -> new Claims())
                                                          .writer(spec.id(), null);
                        case ShapeWriterWrite shape -> files.computeIfAbsent(definitionFiles.get(shape.shape()),
                                                                             p -> new Claims())
                                                            .writer(spec.id(), shape.shape());
                    }
                }
            }
            // Integrations write resources through the context's file manifest too, so every path is a source path.
            SortedSet<String> sources = new TreeSet<>(files.keySet());
            files.forEach((path, claims) -> {
                String shape = !claims.renderedShapes.isEmpty()
                               ? claims.renderedShapes.first()
                               : definitionFiles.entrySet().stream().filter(e -> e.getValue().equals(path))
                                                .map(Map.Entry::getKey).sorted().findFirst().orElse(SERVICE);
                int physicalWrites = claims.manifestWrites + (claims.writer ? 1 : 0);
                if (claims.contributors.size() > 1) {
                    add(DiagnosticCode.INTEGRATION_OUTPUT_CONFLICT, shape, claims.contributors, "output " + path);
                    if (claims.contributors.contains(GENERATOR)) {
                        features.add("conflict with the core generator");
                        if (claims.renderedShapes.stream().anyMatch(MODEL_TYPES::containsKey)) {
                            features.add("conflict with a generated model type");
                        }
                    }
                    if (claims.renderedShapes.isEmpty()) {
                        features.add(shape.equals(SERVICE) ? "output conflict reported for the service"
                                                           : "output conflict reported for the defining shape");
                    }
                } else if (physicalWrites > 1) {
                    add(DiagnosticCode.DUPLICATE_GENERATED_FILE, shape, claims.contributors, "output " + path);
                }
            });
            return sources;
        }

        /**
         * Definition file of every non-member shape of the closure that has one, by shape ID: the model types (and
         * the list once renamed) at their final type names, the service and the operations.
         */
        private static Map<String, String> definitionFiles(Map<String, String> typeNames) {
            Map<String, String> result = new TreeMap<>();
            MODEL_TYPES.keySet().forEach(shape -> result.put(shape, MODEL_DIRECTORY + "/" + typeNames.get(shape)
                                                                    + ".java"));
            if (typeNames.containsKey(CITY_LIST)) {
                result.put(CITY_LIST, MODEL_DIRECTORY + "/" + typeNames.get(CITY_LIST) + ".java");
            }
            result.put(SERVICE, SERVICE_INFO);
            result.put(GET_FORECAST, GET_FORECAST_DESCRIPTOR);
            result.put(LIST_CITIES, LIST_CITIES_DESCRIPTOR);
            return result;
        }

        private static void record(Map<String, Map<String, SortedSet<String>>> changes, String shape, String aspect,
                                   String integration) {
            changes.computeIfAbsent(shape, s -> new TreeMap<>()).computeIfAbsent(aspect, a -> new TreeSet<>())
                   .add(integration);
        }

        private void add(DiagnosticCode code, String shape, SortedSet<String> integrations, String element) {
            conflicts.add(new Conflict(code, shape, List.copyOf(integrations), element));
            if (integrations.size() > 2) {
                features.add("three or more integrations in one diagnostic");
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generators

    @Provide
    Arbitrary<Scenario> scenarios() {
        return identifiers().list().ofMinSize(1).ofMaxSize(4).uniqueElements().flatMap(ids -> Combinators.combine(
            Combinators.combine(ids.stream().map(IntegrationConflictDiagnosticsPropertyTest::spec).toList())
                       .as(List::copyOf),
            Arbitraries.longs()).as(Scenario::new));
    }

    /** Valid integration identifiers other than the reserved core-generator identifier. */
    private static Arbitrary<String> identifiers() {
        Arbitrary<String> head = Combinators.combine(
            Arbitraries.chars().range('a', 'z'),
            Arbitraries.strings().withCharRange('a', 'z').withCharRange('0', '9').ofMaxLength(3)).as((c, s) -> c + s);
        Arbitrary<String> tail = Arbitraries.strings().withCharRange('a', 'z').withCharRange('0', '9')
                                            .ofMinLength(1).ofMaxLength(4);
        Arbitrary<String> random = Combinators.combine(head, Arbitraries.of("", ".", "-"), tail)
                                              .as((h, separator, t) -> separator.isEmpty() ? h : h + separator + t);
        return Arbitraries.frequencyOf(Tuple.of(3, random), Tuple.of(1, Arbitraries.of(FIXED_IDENTIFIERS)))
                          .filter(id -> !id.equals(GENERATOR));
    }

    private static Arbitrary<Spec> spec(String id) {
        Arbitrary<Integer> order = Arbitraries.of(-10, 0, 0, 7);
        Arbitrary<List<ModelAction>> preprocess = Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.just(List.<ModelAction>of())),
            Tuple.of(2, modelAction().list().ofMinSize(1).ofMaxSize(3)));
        // Empty: no decorator. Present but empty: a decorator that changes nothing.
        Arbitrary<Optional<List<SymbolAction>>> decorator = Arbitraries.frequencyOf(
            Tuple.of(4, Arbitraries.just(Optional.<List<SymbolAction>>empty())),
            Tuple.of(1, Arbitraries.just(Optional.of(List.<SymbolAction>of()))),
            Tuple.of(4, symbolAction().list().ofMinSize(1).ofMaxSize(3).map(Optional::of)));
        Arbitrary<List<Write>> writes = write().list().ofMaxSize(3);
        return Combinators.combine(order, preprocess, decorator, writes)
                          .as((o, p, d, w) -> new Spec(id, o, p, d.isPresent(), d.orElse(List.of()), w));
    }

    private static Arbitrary<ModelAction> modelAction() {
        Arbitrary<SetTrait> setTrait = Combinators.combine(Arbitraries.of(TRAIT_TARGETS),
                                                           Arbitraries.of(TraitKind.class),
                                                           Arbitraries.of(TRAIT_VALUES)).as(SetTrait::new);
        Arbitrary<AddMember> addMember = Combinators.combine(Arbitraries.of(MEMBER_CONTAINERS),
                                                             Arbitraries.of(NEW_MEMBERS),
                                                             Arbitraries.of(MEMBER_TARGETS)).as(AddMember::new);
        return Arbitraries.frequencyOf(Tuple.of(3, setTrait), Tuple.of(1, addMember));
    }

    private static Arbitrary<SymbolAction> symbolAction() {
        Arbitrary<RenameType> type = Combinators.combine(Arbitraries.of(RENAMED_TYPES), Arbitraries.of(TYPE_NAMES))
                                                .as(RenameType::new);
        // Mostly to Report, the only way to define a file that no directive renders (the defining-shape rule).
        Arbitrary<RenameType> list = Arbitraries.frequencyOf(Tuple.of(3, Arbitraries.just(REPORT)),
                                                             Tuple.of(1, Arbitraries.just("Forecast")))
                                                .map(name -> new RenameType(CITY_LIST, name));
        Arbitrary<RenameMember> member = Combinators.combine(Arbitraries.of(RENAMED_MEMBERS),
                                                             Arbitraries.of(MEMBER_NAMES)).as(RenameMember::new);
        Arbitrary<AddProperty> property = Arbitraries.of(PROPERTY_SHAPES).map(AddProperty::new);
        // The list weight (with REPORT_PATH's in write()) keeps the defining-shape output conflict near 7% of tries.
        return Arbitraries.frequencyOf(Tuple.of(3, type), Tuple.of(6, list), Tuple.of(2, member),
                                       Tuple.of(1, property));
    }

    private static Arbitrary<Write> write() {
        Arbitrary<String> javaPaths = Arbitraries.frequencyOf(Tuple.of(2, Arbitraries.of(GENERATOR_PATHS)),
                                                              Tuple.of(1, Arbitraries.of(MODEL_PATHS)),
                                                              Tuple.of(5, Arbitraries.just(REPORT_PATH)));
        Arbitrary<String> manifestPaths = Arbitraries.frequencyOf(Tuple.of(4, javaPaths),
                                                                  Tuple.of(1, Arbitraries.of(RESOURCE_PATHS)));
        return Arbitraries.frequencyOf(
            Tuple.of(3, manifestPaths.map(ManifestWrite::new)),
            Tuple.of(2, javaPaths.map(FileWriterWrite::new)),
            Tuple.of(2, Arbitraries.of(WRITER_SHAPES).map(ShapeWriterWrite::new)));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Statistics

    private static final List<DiagnosticCode> CONFLICT_CODES = List.of(
        DiagnosticCode.INTEGRATION_OUTPUT_CONFLICT, DiagnosticCode.INTEGRATION_SYMBOL_CONFLICT,
        DiagnosticCode.INTEGRATION_MODEL_CONFLICT, DiagnosticCode.DUPLICATE_GENERATED_FILE,
        DiagnosticCode.GENERATED_NAME_COLLISION);

    private static final List<String> FEATURES = List.of(
        "three or more integrations in one diagnostic", "definition change conflicts with trait change",
        "same-value trait re-set", "unchanged symbol rename", "property-only symbol change", "no-op symbol decorator",
        "conflict with the core generator", "conflict with a generated model type",
        "generated model type moved by a rename", "two shapes defined in one file",
        "output conflict reported for the defining shape", "output conflict reported for the service",
        "type renamed onto a generator-named type");

    /**
     * Collects the outcome, conflict codes and scenario features per try, and fails the property if any of them is
     * generated too rarely to be exercised.
     */
    private static void collectStatistics(Prediction expected) {
        String outcome = expected.conflicts().isEmpty() ? "generated"
                                                        : expected.settingPhase() ? "setting conflicts"
                                                                                  : "output conflicts";
        Statistics.label("outcome").collect(outcome);
        Statistics.label("outcome").coverage(c -> {
            c.check("generated").percentage(p -> p >= 6);
            c.check("setting conflicts").percentage(p -> p >= 8);
            c.check("output conflicts").percentage(p -> p >= 30);
        });
        for (DiagnosticCode code : CONFLICT_CODES) {
            Statistics.label(code.toString()).collect(expected.conflicts().stream().anyMatch(c -> c.code() == code));
            Statistics.label(code.toString()).coverage(c -> c.check(true).percentage(p -> p >= 1));
        }
        for (String feature : FEATURES) {
            Statistics.label(feature).collect(expected.features().contains(feature));
            Statistics.label(feature).coverage(c -> c.check(true).percentage(p -> p >= 1));
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

    private static final java.util.regex.Pattern CLIENT_SOURCE = java.util.regex.Pattern.compile(
        "software/amazon/awssdk/services/weather/((Default)?Weather(Async)?Client(Builder)?|WeatherOperationBindings)"
        + "\\.java");

    /** {@code paths} without the generated client sources of the service root package. */
    private static SortedSet<String> withoutClients(java.util.Collection<String> paths) {
        SortedSet<String> filtered = new TreeSet<>();
        for (String path : paths) {
            if (!CLIENT_SOURCE.matcher(path).matches()) {
                filtered.add(path);
            }
        }
        return filtered;
    }
}
