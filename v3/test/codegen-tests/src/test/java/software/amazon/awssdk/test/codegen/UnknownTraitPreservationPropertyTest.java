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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.RandomDistribution;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.dynamic.model.DynamicModel;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.awssdk.test.codegen.support.Generation;
import software.amazon.awssdk.test.codegen.support.SmithyModelFixture;
import software.amazon.awssdk.test.codegen.support.TestIntegration;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Property 2 (Requirements 2.11, 14.1, 14.17): for any unknown trait identifier, target and Smithy node value in a
 * canonical or runtime model, generation or dynamic loading preserves that identifier, target and value exactly.
 *
 * <p>Each try generates arbitrary absolute trait IDs (valid Smithy namespaces and names outside the {@code smithy},
 * {@code aws} and model namespaces, so none has a trait definition or trait service on the classpath) and applies
 * them, with arbitrary node values, to targets of every kind: the service, the operation, its inline input/output
 * structures and members, structures, unions, string and int enums and their members, lists, maps and their members,
 * simple shapes, a mixin shape and mixin member (inherited by the shape that mixes it in), and an inherited mixin
 * member. Traits are written inline in Smithy IDL, as IDL {@code apply} statements, or as Smithy JSON AST
 * {@code apply} shapes. The oracle is Smithy {@link Node#equals structural node equality} of the value together with
 * exact {@link ShapeId} equality of the trait ID and the target shape, over the exact set of {@link DynamicTrait}s
 * (nothing missing, nothing extra), in four views:
 * <ol>
 *   <li><b>canonical model</b>: the model files assembled as {@code aws-codegen} assembles them (unknown traits
 *       allowed); mixin shapes carry their own traits and every shape that mixes them in inherits them;</li>
 *   <li><b>generation</b>: the final model {@code aws-codegen} hands to integrations after Smithy's default codegen
 *       transforms (mixins flattened and removed, dedicated operation input/output), captured through a
 *       {@code customize} hook of the public {@link AwsCodegen#generate(AwsCodegenRequest, Iterable)} entry point;
 *       the traits of removed mixins must be on the shapes that mixed them in;</li>
 *   <li><b>runtime model</b>: the canonical model serialized to a Smithy JSON AST and loaded by the V3 dynamic model
 *       ({@link DynamicModel#fromJsonAst}), as the dynamic client loads it (Requirement 14.1);</li>
 *   <li><b>runtime schema</b> (Requirements 2.11, 14.17): the schemas {@code aws-codegen} generates for the service,
 *       compiled in-test against the generated-client runtime and loaded, and the schemas the V3 dynamic model builds
 *       for the service from the runtime JSON AST ({@code DynamicService#schemaIndex()}); in both, every target of the
 *       concrete (mixin-free) model, including enum and intEnum members, must have a schema, and the
 *       {@code ServiceSchemaIndex} must expose exactly the expected unknown traits, keyed by exact trait ID, any number
 *       per shape or member.</li>
 * </ol>
 *
 * <p>Views 1–3 are enforced by {@link #unknownTraitsArePreservedExactly}. View 4 is a separate property,
 * {@link #runtimeSchemasExposeUnknownTraitsExactly}, over the same generators and oracle, because each try compiles
 * the generated schema holder. Smithy Java's {@code Schema} stores traits by Java class ({@link TraitKey}), so it can
 * hold at most one {@code DynamicTrait} and no lookup by trait ID; generated and dynamic-model schemas therefore keep
 * unknown traits out of the Smithy Java schema (view 4 also checks that no schema carries one by class) and expose
 * them through the index, which also holds the member schemas of enum and intEnum members.
 *
 * <p>Before task 3.1, view 4 was disabled and used Smithy Java's model-to-schema conversion ({@code SchemaConverter},
 * the path Smithy Java's dynamic client uses) as a stand-in, where this property found two gaps: enum and intEnum
 * schemas have no member schemas (minimal counterexample: {@code @a#a(null)} on {@code Color$RED}), and a second
 * unknown trait on the same shape is lost. Task 3.1 ran view 4 against generated schemas; task 12.1 replaced the
 * stand-in with the V3 dynamic model, which builds its own schemas rather than use the converter.
 * {@link #runtimeSchemasCloseTheStandInGaps} checks the same two examples against both, and
 * {@link #dynamicModelSchemasHaveTheGeneratedSchemaStructure} checks that dynamic-model schemas have the structure of
 * the generated schemas.
 *
 * <p>Numbers are generated in Smithy's canonical node representation: {@code long}s, integers beyond the
 * {@code long} range, finite {@code double}s of arbitrary bit patterns, and decimals that no {@code double}
 * represents exactly (more than 17 significant digits, or beyond the {@code double} range). Smithy's node model does
 * not keep the lexical scale of a decimal that a {@code double} represents exactly ({@code 1.50} loads as the
 * {@code double} {@code 1.5}), so such decimals are generated as {@code double}s.
 */
class UnknownTraitPreservationPropertyTest {

    private static final String NAMESPACE = "example.weather";
    private static final TraitKey<DynamicTrait> DYNAMIC_TRAIT = TraitKey.get(DynamicTrait.class);
    /** The generated schema holder of the fixture service. */
    private static final String SCHEMA_HOLDER = SmithyModelFixture.PACKAGE + ".model.internal.WeatherSchemas";
    /** First namespace segments that could resolve to a trait definition or trait service on the classpath. */
    private static final Set<String> RESERVED_ROOT_NAMESPACES = Set.of("smithy", "aws", "example");
    private static final int MAX_NODE_DEPTH = 3;

    // Feature: aws-sdk-java-v3, Property 2: Unknown trait preservation
    // Embedded edge cases are disabled: through the flatMaps they pick one-application models for most tries. The
    // generators weight boundary identifiers, numbers and strings explicitly instead.
    @Property(tries = 300, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 2: Unknown trait preservation")
    void unknownTraitsArePreservedExactly(@ForAll("traitModels") TraitModel traitModel) throws IOException {
        Path work = Files.createTempDirectory("unknown-trait-preservation-");
        try {
            SmithyModelFixture fixture = traitModel.fixture();
            AwsCodegenRequest request = Generation.request(work, fixture, fixture.files());
            List<String> problems = new ArrayList<>();

            // Requirement 2.11: the canonical model retains every unknown trait.
            Model canonical = assemble(assembler(), request.modelDirectory(), fixture.files());
            problems.addAll(compare("canonical model", traitModel.expected(View.CANONICAL), unknownTraits(canonical)));

            // Requirement 2.11: aws-codegen keeps them through its default transforms, as seen by integrations.
            AtomicReference<Model> generated = new AtomicReference<>();
            AwsCodegen.generate(request, List.of(TestIntegration.named("test.capture-model")
                                                                .withCustomize(c -> generated.set(c.model()))));
            problems.addAll(compare("aws-codegen model", traitModel.expected(View.GENERATED),
                                    unknownTraits(generated.get())));

            // Requirement 14.1: the runtime JSON AST model loaded by the dynamic model retains them.
            Model runtime = runtimeModel(canonical).model();
            problems.addAll(compare("runtime model", traitModel.expected(View.CANONICAL), unknownTraits(runtime)));

            Statistics.label("applications").collect(traitModel.applications().size());
            Statistics.label("modes").collect(traitModel.applications().stream().map(Application::mode)
                                                        .collect(Collectors.toCollection(TreeSet::new)));
            assertEquals(List.of(), problems, "Unknown traits not preserved exactly; model files:\n"
                                              + traitModel.describe());
        } finally {
            deleteRecursively(work);
        }
    }

    // View 4 of Property 2 (Requirements 2.11, 14.17): the generated and the dynamic-model schemas (see class comment).
    @Property(tries = 300, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 2 (runtime schema view): Unknown trait preservation")
    void runtimeSchemasExposeUnknownTraitsExactly(@ForAll("traitModels") TraitModel traitModel) throws IOException {
        Path work = Files.createTempDirectory("unknown-trait-preservation-schemas-");
        try {
            SmithyModelFixture fixture = traitModel.fixture();
            AwsCodegenRequest request = Generation.request(work, fixture, fixture.files());
            AwsCodegen.generate(request, List.of());
            Map<String, Node> expected = traitModel.expected(View.GENERATED);

            // Requirements 2.11 and 14.17: the generated runtime schemas expose them as runtime objects, by exact ID.
            List<String> problems = new ArrayList<>(checkGeneratedSchemas(work, expected));
            // Requirements 14.1 and 14.17: so do the schemas the dynamic model builds from the runtime JSON AST.
            Model canonical = assemble(assembler(), request.modelDirectory(), fixture.files());
            problems.addAll(checkSchemas("dynamic-model schema", runtimeModel(canonical).service().schemaIndex(),
                                         expected));
            assertEquals(List.of(), problems, "Unknown traits not exposed exactly by the runtime schemas; model "
                                              + "files:\n" + traitModel.describe());
            Statistics.label("enum member targets").collect(
                traitModel.applications().stream().anyMatch(a -> a.slot() == Slot.COLOR_RED
                                                                  || a.slot() == Slot.LEVEL_LOW));
            Statistics.label("several traits on one target").collect(severalOnOneTarget(traitModel));
            Statistics.label("enum member targets").coverage(c -> c.check(true).percentage(p -> p >= 10));
            Statistics.label("several traits on one target").coverage(c -> c.check(true).percentage(p -> p >= 10));
        } finally {
            deleteRecursively(work);
        }
    }

    /**
     * The two minimal examples on which the Smithy Java 1.6.1 {@code SchemaConverter} stand-in failed view 4 (see class
     * comment) hold for the generated and the dynamic-model schemas: an unknown trait on an enum and an intEnum member,
     * and two unknown traits on one shape.
     */
    @Test
    void runtimeSchemasCloseTheStandInGaps() throws IOException {
        TraitModel traitModel = TraitModel.of(List.of(
            new Application(ShapeId.from("a#a"), Slot.COLOR_RED, Mode.INLINE_IDL, Node.nullNode()),
            new Application(ShapeId.from("a#a"), Slot.LEVEL_LOW, Mode.INLINE_IDL, Node.nullNode()),
            new Application(ShapeId.from("a#a"), Slot.NAME, Mode.INLINE_IDL, Node.from(1)),
            new Application(ShapeId.from("b#b"), Slot.NAME, Mode.APPLY_IDL, Node.from("two"))));
        assertEquals(4, traitModel.applications().size());
        Path work = Files.createTempDirectory("unknown-trait-preservation-gaps-");
        try {
            SmithyModelFixture fixture = traitModel.fixture();
            AwsCodegenRequest request = Generation.request(work, fixture, fixture.files());
            AwsCodegen.generate(request, List.of());
            Map<String, Node> expected = traitModel.expected(View.GENERATED);
            assertEquals(List.of(), checkGeneratedSchemas(work, expected));

            ServiceSchemaIndex dynamic = runtimeModel(assemble(assembler(), request.modelDirectory(),
                                                               fixture.files())).service().schemaIndex();
            assertEquals(List.of(), checkSchemas("dynamic-model schema", dynamic, expected));
            for (String member : List.of("Color$RED", "Level$LOW")) {
                Schema schema = dynamic.schema(shapeId(member)).orElseThrow();
                assertEquals(shapeId(member), schema.id());
                assertNotNull(schema.getDirectTrait(TraitKey.get(EnumValueTrait.class)), member + " enum value");
            }
            assertEquals(List.of(ShapeId.from("a#a"), ShapeId.from("b#b")),
                         dynamic.unknownTraits(shapeId("Name")).ids());
        } finally {
            deleteRecursively(work);
        }
    }

    /**
     * Dynamic-model schemas have the structure of the generated schemas of the same model, so dynamic and generated
     * operations share one schema view: the same schema IDs and types, the same members in modeled order with the same
     * targets, and the same unknown traits, on a model with unknown traits on every slot.
     */
    @Test
    void dynamicModelSchemasHaveTheGeneratedSchemaStructure() throws IOException {
        List<Application> everySlot = new ArrayList<>();
        for (Slot slot : Slot.values()) {
            everySlot.add(new Application(ShapeId.from("x.y#z"), slot, slot.modes().iterator().next(),
                                          Node.from(slot.name())));
        }
        TraitModel traitModel = TraitModel.of(everySlot);
        Path work = Files.createTempDirectory("unknown-trait-preservation-structure-");
        try {
            SmithyModelFixture fixture = traitModel.fixture();
            AwsCodegenRequest request = Generation.request(work, fixture, fixture.files());
            AwsCodegen.generate(request, List.of());
            ServiceSchemaIndex dynamic = runtimeModel(assemble(assembler(), request.modelDirectory(),
                                                               fixture.files())).service().schemaIndex();
            CompiledSources compiled = CompiledSources.compile(Generation.sourceRoot(work), work.resolve("classes"),
                                                               SmithyModelFixture.PACKAGE_PATH
                                                               + "/model/internal/WeatherSchemas");
            try (URLClassLoader loader = compiled.loader()) {
                ServiceSchemaIndex generated = CompiledSources.index(loader, SCHEMA_HOLDER);
                assertEquals(structure(generated), structure(dynamic));
                assertEquals(generated.unknownTraits(), dynamic.unknownTraits());
            }
        } finally {
            deleteRecursively(work);
        }
    }

    /** Every schema of {@code index} reachable by ID: its type and its members with their target IDs, by ID. */
    private static Map<ShapeId, String> structure(ServiceSchemaIndex index) {
        Map<ShapeId, String> result = new TreeMap<>();
        List<Schema> roots = new ArrayList<>();
        roots.add(index.service());
        roots.addAll(index.operations());
        roots.addAll(index.types());
        roots.addAll(index.shapes());
        for (Schema root : roots) {
            result.put(root.id(), root.type() + " " + index.members(root).stream()
                .map(m -> m.memberName() + ":" + m.memberTarget().id()).toList());
        }
        return result;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Views and oracle
    // ---------------------------------------------------------------------------------------------------------------

    /** The views whose unknown traits are compared. */
    enum View {
        /** Assembled model files, including mixin shapes. */
        CANONICAL,
        /** After Smithy's default codegen transforms: mixins are flattened into the shapes that use them. */
        GENERATED
    }

    /** Assembles as {@code AwsCodegen.assemble} does: aws-codegen's class loader, discovered models, unknown traits. */
    private static ModelAssembler assembler() {
        ClassLoader classLoader = AwsCodegen.class.getClassLoader();
        return Model.assembler(classLoader).discoverModels(classLoader)
                    .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
    }

    private static Model assemble(ModelAssembler assembler, Path directory, List<String> files) {
        files.forEach(file -> assembler.addImport(directory.resolve(file)));
        return assembler.assemble().unwrap();
    }

    /** {@code model} serialized to a Smithy JSON AST and loaded by the V3 dynamic model, as the dynamic client does. */
    private static DynamicModel runtimeModel(Model model) {
        String jsonAst = Node.prettyPrintJson(ModelSerializer.builder().build().serialize(model));
        return DynamicModel.fromJsonAst(jsonAst, "runtime-model.json");
    }

    private static ShapeId shapeId(String relative) {
        return ShapeId.from(NAMESPACE + "#" + relative);
    }

    /** Every {@link DynamicTrait} of every shape and member, keyed by {@code target @traitId}. */
    private static Map<String, Node> unknownTraits(Model model) {
        Map<String, Node> traits = new TreeMap<>();
        model.shapes().sorted(Comparator.comparing(Shape::getId)).forEach(shape -> {
            for (Trait trait : shape.getAllTraits().values()) {
                if (trait instanceof DynamicTrait) {
                    traits.put(key(shape.getId(), trait.toShapeId()), trait.toNode());
                }
            }
        });
        return traits;
    }

    private static String key(ShapeId target, ShapeId trait) {
        return target + " @" + trait;
    }

    /** Every missing, extra or unequal trait, compared by exact key and structural node equality. */
    private static List<String> compare(String view, Map<String, Node> expected, Map<String, Node> actual) {
        List<String> problems = new ArrayList<>();
        expected.forEach((key, value) -> {
            Node found = actual.get(key);
            if (found == null) {
                problems.add(view + ": missing " + key + " = " + Node.printJson(value));
            } else if (!found.equals(value)) {
                problems.add(view + ": " + key + " = " + Node.printJson(found) + ", expected " + Node.printJson(value));
            }
        });
        actual.keySet().stream().filter(key -> !expected.containsKey(key))
              .forEach(key -> problems.add(view + ": unexpected " + key + " = " + Node.printJson(actual.get(key))));
        return problems;
    }

    /** Compiles the generated schema holder in {@code work} and checks its index with {@link #checkSchemas}. */
    private static List<String> checkGeneratedSchemas(Path work, Map<String, Node> expected) throws IOException {
        // Only the schema holder and its partitions: the type registry beside them references the model types.
        CompiledSources compiled = CompiledSources.compile(Generation.sourceRoot(work), work.resolve("classes"),
                                                           SmithyModelFixture.PACKAGE_PATH
                                                           + "/model/internal/WeatherSchemas");
        try (URLClassLoader loader = compiled.loader()) {
            return checkSchemas("generated schema", CompiledSources.index(loader, SCHEMA_HOLDER), expected);
        }
    }

    /**
     * Compares the unknown traits {@code index} exposes, by exact target and trait ID, with {@code expected}: nothing
     * missing, nothing extra, structurally equal values. Every expected target must have a schema with exactly its
     * ID, reached through the index, whose unknown traits are also found from the schema object; no schema may carry
     * an unknown trait by Java class.
     */
    private static List<String> checkSchemas(String view, ServiceSchemaIndex index, Map<String, Node> expected) {
        Map<String, Node> actual = new TreeMap<>();
        index.unknownTraits().forEach((target, traits) -> traits.asMap().forEach(
            (traitId, trait) -> actual.put(key(target, traitId), trait.toNode())));
        List<String> problems = new ArrayList<>(compare(view, expected, actual));
        expected.forEach((key, value) -> {
            String[] parts = key.split(" @", 2);
            ShapeId target = ShapeId.from(parts[0]);
            ShapeId traitId = ShapeId.from(parts[1]);
            Schema schema = index.schema(target).orElse(null);
            if (schema == null) {
                problems.add(view + ": no schema for " + target + " to hold @" + traitId);
            } else if (!schema.id().equals(target)) {
                problems.add(view + ": schema of " + target + " has ID " + schema.id());
            } else if (!index.unknownTraits(schema).value(traitId).map(value::equals).orElse(false)) {
                problems.add(view + ": " + key + " is not found from the schema of " + target);
            } else if (schema.getDirectTrait(DYNAMIC_TRAIT) != null) {
                problems.add(view + ": " + target + " carries @" + schema.getDirectTrait(DYNAMIC_TRAIT).toShapeId()
                             + " by Java class");
            }
        });
        return problems;
    }

    private static boolean severalOnOneTarget(TraitModel traitModel) {
        Map<ShapeId, Integer> perTarget = new TreeMap<>();
        traitModel.expected(View.GENERATED).keySet()
                  .forEach(key -> perTarget.merge(ShapeId.from(key.split(" @", 2)[0]), 1, Integer::sum));
        return perTarget.values().stream().anyMatch(count -> count > 1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Model under test
    // ---------------------------------------------------------------------------------------------------------------

    /** How an unknown trait is written. */
    enum Mode {
        /** On the shape or member definition in the IDL. */
        INLINE_IDL,
        /** An IDL {@code apply} statement in a separate file. */
        APPLY_IDL,
        /** A JSON AST {@code apply} shape in a separate file. */
        APPLY_JSON_AST
    }

    /**
     * A trait target of the skeleton model, with the shapes that carry the trait in each view: a trait on a mixin
     * shape or member is inherited by the shape that mixes it in, and only the latter survives generation.
     */
    enum Slot {
        SERVICE("Weather"),
        OPERATION("GetThing"),
        INPUT("GetThingInput"),
        INPUT_CITY("GetThingInput$city"),
        OUTPUT_THING("GetThingOutput$thing"),
        THING("Thing"),
        THING_NAME("Thing$name"),
        CHOICE("Choice"),
        CHOICE_A("Choice$a"),
        COLOR("Color"),
        COLOR_RED("Color$RED"),
        LEVEL("Level"),
        LEVEL_LOW("Level$LOW"),
        THINGS("Things"),
        THINGS_MEMBER("Things$member"),
        TAGS("Tags"),
        TAGS_KEY("Tags$key"),
        TAGS_VALUE("Tags$value"),
        BASE("Base", "Derived"),
        BASE_ID("Base$id", "Derived$id"),
        DERIVED("Derived"),
        DERIVED_EXTRA("Derived$extra"),
        /** Inherited from the mixin member; can only be targeted by {@code apply}. */
        DERIVED_ID("Derived$id"),
        NAME("Name"),
        COUNT("Count"),
        DATA("Data"),
        WHEN("When"),
        FLAG("Flag"),
        AMOUNT("Amount"),
        BIG("Big"),
        RATIO("Ratio"),
        TINY("Tiny"),
        TOTAL("Total");

        private final String target;
        private final String inheritedBy;

        Slot(String target) {
            this(target, null);
        }

        Slot(String target, String inheritedBy) {
            this.target = target;
            this.inheritedBy = inheritedBy;
        }

        Set<Mode> modes() {
            return this == DERIVED_ID ? EnumSet.of(Mode.APPLY_IDL, Mode.APPLY_JSON_AST) : EnumSet.allOf(Mode.class);
        }

        ShapeId shapeId() {
            return UnknownTraitPreservationPropertyTest.shapeId(target);
        }

        /** The shapes that carry a trait applied to this slot in {@code view}. */
        List<ShapeId> targets(View view) {
            if (inheritedBy == null) {
                return List.of(shapeId());
            }
            ShapeId inheriting = UnknownTraitPreservationPropertyTest.shapeId(inheritedBy);
            return view == View.CANONICAL ? List.of(shapeId(), inheriting) : List.of(inheriting);
        }
    }

    /** One unknown trait application. */
    record Application(ShapeId traitId, Slot slot, Mode mode, Node value) {
        String idl() {
            return "@" + traitId + "(" + Node.printJson(value) + ")";
        }

        /** Readable counterexamples: the node value as JSON rather than its identity. */
        @Override
        public String toString() {
            return slot + " " + mode + " " + idl();
        }
    }

    /**
     * Applications whose (trait ID, target) pairs are disjoint in the canonical view, so that each pair has exactly
     * one expected value (Smithy merges or rejects conflicting applications of one trait to one shape).
     */
    record TraitModel(List<Application> applications) {

        static TraitModel of(List<Application> candidates) {
            Set<String> taken = new TreeSet<>();
            List<Application> kept = new ArrayList<>();
            for (Application application : candidates) {
                List<String> keys = application.slot().targets(View.CANONICAL).stream()
                                               .map(target -> key(target, application.traitId())).toList();
                if (keys.stream().noneMatch(taken::contains)) {
                    taken.addAll(keys);
                    kept.add(application);
                }
            }
            return new TraitModel(List.copyOf(kept));
        }

        Map<String, Node> expected(View view) {
            Map<String, Node> expected = new TreeMap<>();
            for (Application application : applications) {
                application.slot().targets(view)
                           .forEach(target -> expected.put(key(target, application.traitId()), application.value()));
            }
            return expected;
        }

        SmithyModelFixture fixture() {
            return SmithyModelFixture.of(files());
        }

        /** The model files (relative path to content) in the order they are written. */
        Map<String, String> files() {
            Map<String, String> files = new LinkedHashMap<>();
            files.put("weather.smithy", skeleton());
            files.put("traits/apply.smithy", applyIdl());
            files.put("traits/apply.json", applyJsonAst());
            return files;
        }

        String describe() {
            StringBuilder description = new StringBuilder();
            files().forEach((file, content) -> description.append("--- ").append(file).append('\n').append(content)
                                                          .append('\n'));
            return description.toString();
        }

        /** The inline IDL traits of {@code slot}, one per line, each followed by a newline and {@code indent}. */
        private String t(Slot slot, String indent) {
            return applications.stream().filter(a -> a.slot() == slot && a.mode() == Mode.INLINE_IDL)
                               .map(a -> a.idl() + "\n" + indent).collect(Collectors.joining());
        }

        private String skeleton() {
            String m = "    ";
            return "$version: \"2\"\n"
                   + "namespace " + NAMESPACE + "\n\n"
                   + "@aws.protocols#awsJson1_0\n"
                   + "@aws.auth#sigv4(name: \"weather\")\n"
                   + t(Slot.SERVICE, "") + "service Weather {\n"
                   + "    version: \"2020-01-01\"\n"
                   + "    operations: [GetThing]\n"
                   + "}\n\n"
                   + t(Slot.OPERATION, "") + "operation GetThing {\n"
                   + "    input := " + t(Slot.INPUT, m) + "{\n"
                   + "        " + t(Slot.INPUT_CITY, m + m) + "city: Name\n"
                   + "    }\n"
                   + "    output := {\n"
                   + "        " + t(Slot.OUTPUT_THING, m + m) + "thing: Thing\n"
                   + "        choice: Choice\n"
                   + "        color: Color\n"
                   + "        level: Level\n"
                   + "        things: Things\n"
                   + "        tags: Tags\n"
                   + "        derived: Derived\n"
                   + "        data: Data\n"
                   + "        when: When\n"
                   + "        flag: Flag\n"
                   + "        amount: Amount\n"
                   + "        big: Big\n"
                   + "        ratio: Ratio\n"
                   + "        tiny: Tiny\n"
                   + "        total: Total\n"
                   + "    }\n"
                   + "}\n\n"
                   + t(Slot.THING, "") + "structure Thing {\n"
                   + m + t(Slot.THING_NAME, m) + "name: Name\n"
                   + "    count: Count\n"
                   + "}\n\n"
                   + t(Slot.CHOICE, "") + "union Choice {\n"
                   + m + t(Slot.CHOICE_A, m) + "a: Name\n"
                   + "    b: Count\n"
                   + "}\n\n"
                   + t(Slot.COLOR, "") + "enum Color {\n"
                   + m + t(Slot.COLOR_RED, m) + "RED\n"
                   + "    GREEN\n"
                   + "}\n\n"
                   + t(Slot.LEVEL, "") + "intEnum Level {\n"
                   + m + t(Slot.LEVEL_LOW, m) + "LOW = 1\n"
                   + "    HIGH = 2\n"
                   + "}\n\n"
                   + t(Slot.THINGS, "") + "list Things {\n"
                   + m + t(Slot.THINGS_MEMBER, m) + "member: Thing\n"
                   + "}\n\n"
                   + t(Slot.TAGS, "") + "map Tags {\n"
                   + m + t(Slot.TAGS_KEY, m) + "key: Name\n"
                   + m + t(Slot.TAGS_VALUE, m) + "value: Name\n"
                   + "}\n\n"
                   + t(Slot.BASE, "") + "@mixin\n"
                   + "structure Base {\n"
                   + m + t(Slot.BASE_ID, m) + "id: Name\n"
                   + "}\n\n"
                   + t(Slot.DERIVED, "") + "structure Derived with [Base] {\n"
                   + m + t(Slot.DERIVED_EXTRA, m) + "extra: Name\n"
                   + "}\n\n"
                   + t(Slot.NAME, "") + "string Name\n\n"
                   + t(Slot.COUNT, "") + "integer Count\n\n"
                   + t(Slot.DATA, "") + "blob Data\n\n"
                   + t(Slot.WHEN, "") + "timestamp When\n\n"
                   + t(Slot.FLAG, "") + "boolean Flag\n\n"
                   + t(Slot.AMOUNT, "") + "bigDecimal Amount\n\n"
                   + t(Slot.BIG, "") + "bigInteger Big\n\n"
                   + t(Slot.RATIO, "") + "double Ratio\n\n"
                   + t(Slot.TINY, "") + "byte Tiny\n\n"
                   + t(Slot.TOTAL, "") + "long Total\n";
        }

        private String applyIdl() {
            return "$version: \"2\"\n"
                   + "namespace " + NAMESPACE + "\n\n"
                   + applications.stream().filter(a -> a.mode() == Mode.APPLY_IDL)
                                 .map(a -> "apply " + a.slot().target + " " + a.idl() + "\n")
                                 .collect(Collectors.joining());
        }

        private String applyJsonAst() {
            Map<String, ObjectNode.Builder> traitsByTarget = new TreeMap<>();
            applications.stream().filter(a -> a.mode() == Mode.APPLY_JSON_AST).forEach(
                a -> traitsByTarget.computeIfAbsent(a.slot().shapeId().toString(), k -> ObjectNode.builder())
                                   .withMember(a.traitId().toString(), a.value()));
            ObjectNode.Builder shapes = ObjectNode.builder();
            traitsByTarget.forEach((target, traits) -> shapes.withMember(
                target, ObjectNode.builder().withMember("type", "apply").withMember("traits", traits.build()).build()));
            return Node.prettyPrintJson(ObjectNode.builder().withMember("smithy", "2.0")
                                                  .withMember("shapes", shapes.build()).build());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------------------------------------------------

    @Provide
    Arbitrary<TraitModel> traitModels() {
        return traitIds().list().ofMinSize(1).ofMaxSize(4).uniqueElements().flatMap(ids -> {
            Arbitrary<Application> application = Arbitraries.of(Slot.values()).flatMap(
                slot -> Combinators.combine(Arbitraries.of(ids), Arbitraries.of(slot.modes()), nodes(MAX_NODE_DEPTH))
                                   .as((id, mode, value) -> new Application(id, slot, mode, value)));
            return application.list().ofMinSize(1).ofMaxSize(10).withSizeDistribution(RandomDistribution.uniform());
        }).map(TraitModel::of);
    }

    /** Absolute shape IDs that no trait definition or trait service on the classpath defines. */
    static Arbitrary<ShapeId> traitIds() {
        Arbitrary<String> namespace = identifiers().list().ofMinSize(1).ofMaxSize(3)
                                                   .filter(s -> !RESERVED_ROOT_NAMESPACES.contains(
                                                       s.get(0).toLowerCase(Locale.ROOT)))
                                                   .map(segments -> String.join(".", segments));
        return Combinators.combine(namespace, identifiers()).as((ns, name) -> ShapeId.from(ns + "#" + name));
    }

    /** Smithy identifiers: {@code (1*"_" (ALPHA / DIGIT) / ALPHA) *(ALPHA / DIGIT / "_")}. */
    static Arbitrary<String> identifiers() {
        Arbitrary<String> start = Arbitraries.frequencyOf(
            Tuple.of(5, Arbitraries.strings().withCharRange('a', 'z').withCharRange('A', 'Z').ofLength(1)),
            Tuple.of(1, Combinators.combine(Arbitraries.strings().withChars('_').ofMinLength(1).ofMaxLength(2),
                                            Arbitraries.strings().withCharRange('a', 'z').withCharRange('A', 'Z')
                                                       .withCharRange('0', '9').ofLength(1))
                                   .as(String::concat)));
        Arbitrary<String> rest = Arbitraries.strings().withCharRange('a', 'z').withCharRange('A', 'Z')
                                            .withCharRange('0', '9').withChars('_').ofMaxLength(10);
        return Combinators.combine(start, rest).as(String::concat);
    }

    /** Node trees of at most {@code depth} nested arrays/objects, including empty containers. */
    static Arbitrary<Node> nodes(int depth) {
        Arbitrary<Node> leaf = Arbitraries.frequencyOf(
            Tuple.of(1, Arbitraries.just(Node.nullNode())),
            Tuple.of(1, Arbitraries.of(true, false).map(Node::from)),
            Tuple.of(3, numbers()),
            Tuple.of(3, strings().map(Node::from)));
        if (depth == 0) {
            return leaf;
        }
        Arbitrary<Node> child = nodes(depth - 1);
        return Arbitraries.frequencyOf(
            Tuple.of(4, leaf),
            Tuple.of(1, child.list().ofMaxSize(4).map(Node::fromNodes)),
            Tuple.of(1, Arbitraries.maps(strings(), child).ofMaxSize(4).map(members -> {
                ObjectNode.Builder object = ObjectNode.builder();
                members.forEach(object::withMember);
                return object.build();
            })));
    }

    /** Numbers in Smithy's canonical node representation (see the class comment). */
    static Arbitrary<Node> numbers() {
        BigInteger beyondLong = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        Arbitrary<BigInteger> bigIntegers = Combinators.combine(
            Arbitraries.bigIntegers().between(beyondLong, BigInteger.TEN.pow(40)), Arbitraries.of(true, false))
            .as((magnitude, negative) -> negative ? magnitude.negate() : magnitude);
        Arbitrary<Double> doubles = Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.longs().map(Double::longBitsToDouble).filter(Double::isFinite)),
            Tuple.of(2, Arbitraries.doubles().between(-1e6, 1e6).ofScale(3)),
            Tuple.of(1, Arbitraries.of(0.0, -0.0, 0.1, 1.0, -1.5, 1e20, 1e-300, 1e300, Double.MIN_VALUE,
                                       -Double.MIN_VALUE, Double.MIN_NORMAL, Double.MAX_VALUE, -Double.MAX_VALUE,
                                       9007199254740993.0)));
        Arbitrary<BigDecimal> decimals = Arbitraries.frequencyOf(
            Tuple.of(4, Combinators.combine(Arbitraries.bigIntegers().between(BigInteger.TEN.pow(17),
                                                                              BigInteger.TEN.pow(40)),
                                            Arbitraries.integers().between(-450, 450), Arbitraries.of(true, false))
                                   .as((unscaled, scale, negative) -> new BigDecimal(
                                       negative ? unscaled.negate() : unscaled, scale))),
            Tuple.of(1, Arbitraries.of(new BigDecimal("1E+400"), new BigDecimal("-1E+400"), new BigDecimal("1E-400"),
                                       new BigDecimal("0.1000000000000000000001"),
                                       new BigDecimal("12345678901234567890.123456789"),
                                       new BigDecimal("1.7976931348623157E+309"))))
            .filter(decimal -> !isExactDouble(decimal));
        return Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.longs().map(Node::from)),
            Tuple.of(1, Arbitraries.of(0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE).map(Node::from)),
            Tuple.of(1, bigIntegers.map(Node::from)),
            Tuple.of(1, Arbitraries.of(beyondLong, BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE))
                                   .map(Node::from)),
            Tuple.of(3, doubles.map(Node::from)),
            Tuple.of(2, decimals.map(Node::from)));
    }

    private static boolean isExactDouble(BigDecimal decimal) {
        double d = decimal.doubleValue();
        return Double.isFinite(d) && new BigDecimal(Double.toString(d)).compareTo(decimal) == 0;
    }

    /**
     * Unicode strings (no lone surrogates: model files are UTF-8), weighted towards characters that need escaping:
     * quotes, backslashes, control characters, line/paragraph separators, BOM and supplementary characters.
     */
    static Arbitrary<String> strings() {
        Arbitrary<Integer> codePoint = Arbitraries.frequencyOf(
            Tuple.of(6, Arbitraries.integers().between(0x20, 0x7E)),
            Tuple.of(2, Arbitraries.of((int) '"', (int) '\\', (int) '/', (int) '\n', (int) '\r', (int) '\t',
                                       (int) '\b', (int) '\f', 0, 0x7F)),
            Tuple.of(1, Arbitraries.integers().between(0, 0x1F)),
            Tuple.of(2, Arbitraries.integers().between(0x80, 0xFFFF).filter(c -> c < 0xD800 || c > 0xDFFF)),
            Tuple.of(1, Arbitraries.integers().between(0x10000, 0x10FFFF)),
            Tuple.of(1, Arbitraries.of(0x85, 0x2028, 0x2029, 0xFEFF, 0xFFFD, 0x1F600)));
        return Arbitraries.frequencyOf(
            Tuple.of(8, codePoint.list().ofMaxSize(12).map(
                codePoints -> new String(codePoints.stream().mapToInt(Integer::intValue).toArray(), 0,
                                         codePoints.size()))),
            Tuple.of(1, Arbitraries.of("", "\"\"\"", "\\u0041", "$version", "${x}", "\r\n", "a\u0000b", "\\",
                                       "example.weather#Thing")));
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
