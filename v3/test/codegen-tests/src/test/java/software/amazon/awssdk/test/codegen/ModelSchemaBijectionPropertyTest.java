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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
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
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscodegen.validation.DiagnosticCode;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.smithy.runtime.registry.RegisteredType;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels.ModelSpec;
import software.amazon.awssdk.test.codegen.support.SchemaFidelityModels;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait;
import software.amazon.smithy.model.traits.HttpBasicAuthTrait;
import software.amazon.smithy.model.traits.HttpBearerAuthTrait;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.model.traits.HttpDigestAuthTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * Property 1 (Requirements 2.1, 2.2, 4.1, 4.5, 4.6, 4.7, 4.12, 19.3): for every generated service model and every
 * exported model shape, exactly one exported generated type and one exported {@code Schema} exist, and the schema graph
 * has the same absolute identity, shape type, ordered member graph, targets, requiredness, defaults, constraints,
 * recursive links and runtime-relevant traits as the canonical shape.
 *
 * <p><b>Models.</b> Each try draws a {@link RandomSmithyModels} service (operations, resources, errors, structures,
 * unions, enums, intEnums, lists, maps and simple shapes in one or two namespaces, recursion through structures and
 * unions, unreferenced shapes, documentation, deprecation, constraint and unknown traits) and enriches it with
 * {@link SchemaFidelityModels} (recursive clusters through structures, unions, lists and maps; {@code @required},
 * including required members after optional ones, which Smithy Java reorders first; {@code @default}s; member and shape
 * constraints; other runtime traits; design-time {@code @tags}). The model is rendered as a random mix of Smithy IDL
 * and JSON AST files, generated with {@code aws-codegen}, and the model, schema, registry and operation sources are
 * compiled in-test and loaded.
 *
 * <p><b>Oracle.</b> The canonical model is assembled independently of {@code aws-codegen} with Smithy's
 * {@link ModelAssembler} (unknown traits allowed). Its expected inventory is the service closure ({@link Walker}) less
 * members, resources and {@code smithy.api} prelude shapes. ({@code aws-codegen} applies Smithy's default codegen
 * transforms, which copy service errors to operations and flatten mixins but create no dedicated inputs or outputs, so
 * an operation with a {@code smithy.api#Unit} input or output links Smithy Java's {@code Unit.SCHEMA}; the oracle
 * computes an operation's errors as its own plus the service's.) Each expected shape carries its exact ID and shape
 * type, its members in modeled order with
 * exact target IDs, and the {@link Trait#toNode() node values} of the compared traits of the shape and of each member
 * (including enum and intEnum members). The shapes are classified as the service, operations, public types
 * (structures, including inputs, outputs and errors, unions, enums, intEnums) and other member targets (lists, maps
 * and simple shapes).
 *
 * <p><b>Compared traits.</b> Runtime-relevant traits are the prelude traits of {@link #RUNTIME_TRAITS}, the set the
 * design fixes for generated schemas (requiredness, defaults and constraints among them); {@link
 * #oracleTraitSetIsTheGeneratorSet} pins it against {@code aws-codegen}'s own table, so the two cannot drift silently.
 * They are compared exactly: the traits a schema holds (read from Smithy Java's trait map, since {@code Schema} has no
 * public trait enumeration) must be exactly the canonical runtime traits, nothing missing and nothing extra, each also
 * found through the public {@link Schema#getDirectTrait} with the canonical trait class and node. Unknown traits
 * ({@link DynamicTrait}s) are compared exactly, by trait ID, through {@link ServiceSchemaIndex#unknownTraits()} over
 * every shape and member. Design-time traits (documentation, deprecation, tags, {@code @input}/{@code @output} and the
 * like) are excluded by the design and must therefore be absent from schemas, which the exact comparison checks. AWS
 * protocol and auth traits ({@code aws.protocols#*}, {@code aws.auth#sigv4}, {@code aws.api#service}) are known
 * non-prelude traits that generated schemas deliberately omit (read through generated metadata instead), so they are
 * excluded as well.
 *
 * <p><b>Views compared.</b>
 * <ol>
 *   <li>the exported {@code public static} {@code Schema} constants of {@code <Service>Schemas} (partitions are
 *       re-exported there): exactly one per service, operation and public type, distinct objects, no constant for any
 *       other shape; and the generated {@link ServiceSchemaIndex}: service, operations, types and non-exported shapes
 *       are exactly the expected sets, the exported ones as the same objects. Every schema matches its expected shape:
 *       ID, type, traits, unknown traits, members in modeled order ({@link ServiceSchemaIndex#members}, which records
 *       the order Smithy Java's {@code members()} does not keep) with exact member IDs, target IDs and member traits,
 *       and each member target is the indexed schema of that ID as the same object, which is the exported constant for
 *       a type: recursive links are the exported objects;</li>
 *   <li>the generated model type classes of {@code <package>.model}: exactly one per public type, whose {@code SCHEMA}
 *       is the exported constant and whose {@code ID} is the shape ID; the only other class is the service's base
 *       exception;</li>
 *   <li>the primary {@link SchemaTypeRegistry}: exactly the structures and unions (it maps
 *       {@code SerializableStruct}s, so enums are not registered), each to the exported schema, the model type class
 *       and a builder of that schema;</li>
 *   <li>the operation descriptors (Requirement 4.6): each operation's schemas are the exported operation, input,
 *       output and error (operation and service errors) constants.</li>
 * </ol>
 *
 * <p>{@link #plantedDefectsAreReported} checks that the views are not vacuous: single defects planted in correct
 * generated sources are each reported.
 *
 * <p><b>Tries.</b> Each try assembles, generates, compiles (in-process {@code javac}, only the classes the views need)
 * and loads a service. Small models take about 0.1 s and large ones about 1 s, so the property runs 300 tries (the
 * design's minimum is 100) in about a minute. Models have one to four operations and 2-14 data shapes plus the
 * enrichment; one try in ten has 150-200 more data shapes, all made reachable, so that schema holders are also
 * partitioned (about a tenth of tries). Coverage checks require recursive shapes and Smithy Java member reordering in at
 * least half of the tries and at least one partitioned holder. Fleet-wide fidelity over real services is an
 * integration gate (design, Testing Strategy), not this property.
 *
 * <p><b>Colliding models.</b> When two public types would get the same V2 Java name (compared case-insensitively),
 * no bijection exists and generation must be rejected. One try in ten plants two errors of one operation with the
 * same V2 class name ({@link #plantErrorNameCollision}, for example {@code ClusterFault} and {@code ClusterException},
 * both {@code ClusterException}); {@link #expectedCollisions} derives every collision from the canonical model
 * independently, and such a try asserts that generation fails with exactly one {@code GENERATED_NAME_COLLISION}
 * diagnostic per colliding set (naming every shape and the Java type) and publishes nothing. A coverage check
 * requires such tries.
 */
class ModelSchemaBijectionPropertyTest {

    private static final String PRELUDE = "smithy.api";
    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    /** The runtime-relevant prelude traits generated schemas carry (see the class documentation). */
    static final List<Class<? extends Trait>> RUNTIME_TRAITS = List.of(
        RequiredTrait.class, DefaultTrait.class, LengthTrait.class, PatternTrait.class, RangeTrait.class,
        UniqueItemsTrait.class, SparseTrait.class, RequiresLengthTrait.class, EnumValueTrait.class, EnumTrait.class,
        ErrorTrait.class, HttpErrorTrait.class, RetryableTrait.class,
        JsonNameTrait.class, TimestampFormatTrait.class, MediaTypeTrait.class, XmlNameTrait.class,
        XmlAttributeTrait.class, XmlFlattenedTrait.class, XmlNamespaceTrait.class, EventHeaderTrait.class,
        EventPayloadTrait.class, StreamingTrait.class,
        HttpTrait.class, HttpHeaderTrait.class, HttpLabelTrait.class, HttpQueryTrait.class, HttpQueryParamsTrait.class,
        HttpPayloadTrait.class, HttpPrefixHeadersTrait.class, HttpResponseCodeTrait.class,
        HttpChecksumRequiredTrait.class, EndpointTrait.class, HostLabelTrait.class,
        IdempotencyTokenTrait.class, IdempotentTrait.class, ReadonlyTrait.class, PaginatedTrait.class,
        RequestCompressionTrait.class, SensitiveTrait.class, UnitTypeTrait.class,
        AuthTrait.class, OptionalAuthTrait.class, HttpBasicAuthTrait.class, HttpBearerAuthTrait.class,
        HttpDigestAuthTrait.class, HttpApiKeyAuthTrait.class);

    private static final Set<ShapeId> RUNTIME_TRAIT_IDS = RUNTIME_TRAITS.stream()
                                                                        .map(ModelSchemaBijectionPropertyTest::traitId)
                                                                        .collect(Collectors.toCollection(TreeSet::new));

    // Feature: aws-sdk-java-v3, Property 1: Canonical model-to-schema bijection and fidelity
    @Property(tries = 300, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 1: Canonical model-to-schema bijection and fidelity")
    void generatedSchemasAndTypesAreACanonicalBijection(@ForAll("scenarios") Scenario scenario) throws IOException {
        Path work = Files.createTempDirectory("schema-bijection-");
        try {
            Path models = scenario.write(work.resolve("model"));
            Model canonical = assemble(models, scenario.files().keySet());
            ModelSpec spec = scenario.spec();
            List<Collision> collisions = expectedCollisions(canonical, spec.service(), spec.packageName());
            Statistics.label("generated name collision").collect(!collisions.isEmpty());
            Statistics.label("generated name collision").coverage(c -> {
                c.check(true).percentage(p -> p >= 3);
                c.check(false).percentage(p -> p >= 80);
            });
            if (!collisions.isEmpty()) {
                // Two public types would be one Java type: no bijection exists, so generation must be rejected.
                assertRejectedAsCollisions(spec, models, work.resolve("out"), collisions, scenario.describe());
                return;
            }
            Oracle oracle = Oracle.of(canonical, spec.service());

            Path sources;
            Views views;
            try {
                sources = generate(spec.service(), spec.packageName(), models, work.resolve("out"));
                views = check(oracle, spec.packageName(), sources, work.resolve("classes"));
            } catch (AssertionError | RuntimeException e) {
                throw new AssertionError("Generating, compiling or loading " + spec + " failed; model files:\n"
                                         + scenario.describe(), e);
            }
            boolean partitioned = Files.exists(sources.resolve(spec.packageName().replace('.', '/') + "/model/internal/"
                                                               + spec.service().getName() + "Schemas1.java"));


            Statistics.label("partitioned holder").collect(partitioned);
            Statistics.label("recursive shapes").collect(oracle.hasCycle());
            Statistics.label("Smithy Java reorders members").collect(views.reordered);
            Statistics.label("required, default, constrained members").collect(
                oracle.memberTraitCount(RequiredTrait.ID) > 0, oracle.memberTraitCount(DefaultTrait.ID) > 0,
                oracle.memberTraitCount(LengthTrait.ID) + oracle.memberTraitCount(RangeTrait.ID) > 0);
            Statistics.label("unknown traits").collect(!oracle.unknownTargets().isEmpty());
            Statistics.label("Unit input or output").collect(oracle.unitLinks() > 0);
            Statistics.label("recursive shapes").coverage(c -> c.check(true).percentage(p -> p >= 50));
            Statistics.label("Smithy Java reorders members").coverage(c -> c.check(true).percentage(p -> p >= 50));
            Statistics.label("partitioned holder").coverage(c -> c.check(true).count(n -> n >= 1));

            List<String> problems = views.problems;
            assertEquals(List.of(), problems.subList(0, Math.min(problems.size(), 60)),
                         problems.size() + " fidelity problems for " + spec + "; model files:\n"
                         + scenario.describe());
        } finally {
            deleteRecursively(work);
        }
    }

    /**
     * The views are not vacuous: each single defect planted in otherwise correct generated sources (a lost
     * requiredness, a changed default, a lost member order, a changed unknown trait value, a broken recursive link, an
     * extra exported constant, a model type holding another type's schema) is reported, and the unmodified sources
     * pass.
     */
    @Test
    void plantedDefectsAreReported() throws IOException {
        Path work = Files.createTempDirectory("schema-bijection-mutants-");
        try {
            Path models = Files.createDirectories(work.resolve("model"));
            Files.writeString(models.resolve("model.smithy"), MUTANT_MODEL, StandardCharsets.UTF_8);
            ShapeId service = ShapeId.from("example.fidelity#Fidelity");
            String packageName = "software.amazon.awssdk.services.fidelity";
            Oracle oracle = Oracle.of(assemble(models, List.of("model.smithy")), service);
            Path sources = generate(service, packageName, models, work.resolve("out"));
            String packagePath = packageName.replace('.', '/');
            Path holder = sources.resolve(packagePath + "/model/internal/FidelitySchemas.java");
            Path treeType = sources.resolve(packagePath + "/model/Tree.java");

            assertEquals(List.of(), check(oracle, packageName, sources, work.resolve("classes")).problems);

            record Mutant(String name, Path file, String from, String to, String expected) {
            }
            List<Mutant> mutants = List.of(
                new Mutant("requiredness", holder, ", new RequiredTrait())", ")", "PutInput$count: schema traits"),
                new Mutant("default", holder, "Node.from(5L)", "Node.from(6L)", "PutInput$count: schema traits"),
                new Mutant("member order", holder, "builder.memberOrder(PUT_INPUT, ", "//",
                           "PutInput: members in order"),
                new Mutant("unknown trait", holder, "Node.from(\"x\")", "Node.from(\"y\")",
                           "Tree$label: unknown traits"),
                new Mutant("recursive link", holder, "bTreeList.putMember(\"member\", bTree)",
                           "bTreeList.putMember(\"member\", "
                           + "Schema.structureBuilder(ShapeId.from(\"example.fidelity#Tree\")).build())",
                           "TreeList$member targets a different example.fidelity#Tree"),
                new Mutant("extra constant", holder, "    private FidelitySchemas() {",
                           "    public static final Schema EXTRA = "
                           + "Schema.createString(ShapeId.from(\"example.fidelity#X\"));\n"
                           + "    private FidelitySchemas() {", "exported schema constants: missing [], extra ["),
                new Mutant("model type schema", treeType, "SCHEMA = FidelitySchemas.TREE;",
                           "SCHEMA = FidelitySchemas.PUT_OUTPUT;", "model classes "));
            for (Mutant mutant : mutants) {
                String original = Files.readString(mutant.file());
                if (!original.contains(mutant.from())) {
                    throw new AssertionError(mutant.name() + ": the generated source no longer contains "
                                             + mutant.from() + "\n" + original);
                }
                Files.writeString(mutant.file(), original.replaceFirst(java.util.regex.Pattern.quote(mutant.from()),
                                                                       java.util.regex.Matcher.quoteReplacement(
                                                                           mutant.to())));
                try {
                    List<String> problems;
                    try {
                        problems = check(oracle, packageName, sources,
                                         work.resolve("classes-" + mutant.name().replace(' ', '-'))).problems;
                    } catch (AssertionError | RuntimeException | LinkageError e) {
                        // A defect the runtime itself rejects on first use (the index checks recursive links too).
                        List<String> causes = new ArrayList<>();
                        for (Throwable t = e; t != null; t = t.getCause()) {
                            causes.add(String.valueOf(t));
                        }
                        problems = List.of(String.join(" caused by ", causes));
                    }
                    if (problems.stream().noneMatch(p -> p.contains(mutant.expected()))) {
                        throw new AssertionError(mutant.name() + ": no problem mentions '" + mutant.expected()
                                                 + "': " + problems);
                    }
                } finally {
                    Files.writeString(mutant.file(), original);
                }
            }
        } finally {
            deleteRecursively(work);
        }
    }

    /** The model of {@link #plantedDefectsAreReported}. */
    private static final String MUTANT_MODEL = """
        $version: "2"
        namespace example.fidelity

        @aws.protocols#awsJson1_0
        @aws.auth#sigv4(name: "fidelity")
        service Fidelity {
            version: "1"
            operations: [Put]
        }

        operation Put {
            input: PutInput
            output: PutOutput
        }

        @input
        structure PutInput {
            note: String
            @required
            count: Integer = 5
            tree: Tree
            @required
            id: String
        }

        @output
        structure PutOutput {
        }

        structure Tree {
            children: TreeList
            @test.meta#note("x")
            label: String
        }

        list TreeList {
            member: Tree
        }
        """;

    /**
     * The oracle's runtime-relevant trait set is exactly the set {@code aws-codegen} documents and uses
     * ({@code awscodegen.schema.RuntimeTraits}, package-private, read reflectively). A change to either must be a
     * deliberate change to both, reviewed against the design's definition of runtime-relevant traits.
     */
    @Test
    void oracleTraitSetIsTheGeneratorSet() throws ReflectiveOperationException {
        Field table = Class.forName("software.amazon.awssdk.awscodegen.schema.RuntimeTraits")
                           .getDeclaredField("TRAITS");
        table.setAccessible(true);
        SortedMap<?, ?> traits = (SortedMap<?, ?>) table.get(null);
        assertEquals(RUNTIME_TRAIT_IDS, new TreeSet<>(traits.keySet()));
        Map<ShapeId, String> classes = new TreeMap<>();
        RUNTIME_TRAITS.forEach(type -> classes.put(traitId(type), type.getName()));
        assertEquals(classes, new TreeMap<>(traits));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Scenarios
    // ---------------------------------------------------------------------------------------------------------------

    /** One enriched model and its rendering as model files (relative path to content). */
    record Scenario(ModelSpec spec, Map<String, String> files) {

        static Scenario of(int operations, int dataShapes, int resources, int errors, boolean twoNamespaces,
                           boolean large, boolean collide, long seed) {
            Random random = new Random(seed);
            ModelSpec base = RandomSmithyModels.model(random, operations, dataShapes, resources, errors, twoNamespaces);
            ModelSpec spec = SchemaFidelityModels.enrich(base, random, large);
            if (collide) {
                spec = plantErrorNameCollision(spec, random);
            }
            return new Scenario(spec, RandomSmithyModels.render(spec, random));
        }

        Path write(Path directory) throws IOException {
            for (Map.Entry<String, String> file : files.entrySet()) {
                Path target = directory.resolve(file.getKey());
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.getValue(), StandardCharsets.UTF_8);
            }
            return directory;
        }

        String describe() {
            StringBuilder out = new StringBuilder();
            files.forEach((path, content) -> out.append("--- ").append(path).append('\n').append(content));
            return out.toString();
        }

        @Override
        public String toString() {
            return spec.toString();
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Integer> extraDataShapes = Arbitraries.frequencyOf(
            Tuple.of(9, Arbitraries.just(0)), Tuple.of(1, Arbitraries.integers().between(150, 200)));
        return Combinators.combine(
            Arbitraries.integers().between(1, 4),
            Arbitraries.integers().between(2, 14),
            extraDataShapes,
            Arbitraries.integers().between(0, 2),
            Arbitraries.integers().between(0, 3),
            Arbitraries.of(false, true),
            // One try in ten plants two errors with the same V2 class name, which generation must reject.
            Arbitraries.integers().between(0, 9).map(n -> n == 0),
            Arbitraries.longs().withoutEdgeCases()
        ).as((operations, data, extra, resources, errors, twoNamespaces, collide, seed) -> Scenario.of(
            operations, data + extra, resources, errors, twoNamespaces, extra > 0, collide, seed));
    }

    /**
     * Adds two errors of one operation whose V2 class names coincide, {@code <Noun>Fault} and
     * {@code <Noun>Exception} (both {@code <Noun>Exception}). {@link RandomSmithyModels} never generates such names,
     * since {@code aws-codegen} rejects them by design.
     */
    static ModelSpec plantErrorNameCollision(ModelSpec spec, Random random) {
        List<String> nouns = List.of("Cluster", "Quota", "Shard", "Replica", "Tenant");
        Set<String> taken = spec.lowerCaseNames();
        String noun;
        for (int attempt = 0; ; attempt++) {
            noun = nouns.get(random.nextInt(nouns.size())) + (attempt < 8 ? "" : Integer.toString(attempt));
            if (!taken.contains(noun.toLowerCase(Locale.ROOT) + "fault")
                && !taken.contains(noun.toLowerCase(Locale.ROOT) + "exception")) {
                break;
            }
        }
        String namespace = spec.service().getNamespace();
        List<RandomSmithyModels.Shape> errors = new ArrayList<>();
        for (String suffix : List.of("Fault", "Exception")) {
            errors.add(new RandomSmithyModels.Shape(
                ShapeId.fromParts(namespace, noun + suffix), "structure",
                List.of(new RandomSmithyModels.Member("message", "smithy.api#String", Map.of())),
                Map.of("smithy.api#error", Node.from(suffix.equals("Fault") ? "server" : "client")), null, null, null,
                List.of(), List.of(), List.of(), Map.of()));
        }
        ShapeId operation = spec.ofType("operation").get(0);
        List<RandomSmithyModels.Shape> shapes = new ArrayList<>();
        for (RandomSmithyModels.Shape shape : spec.shapes()) {
            if (shape.id().equals(operation)) {
                List<String> operationErrors = new ArrayList<>(shape.errors());
                errors.forEach(error -> operationErrors.add(error.id().toString()));
                shape = new RandomSmithyModels.Shape(shape.id(), shape.type(), shape.members(), shape.traits(),
                                                     shape.version(), shape.input(), shape.output(), operationErrors,
                                                     shape.operations(), shape.resources(), shape.identifiers());
            }
            shapes.add(shape);
        }
        shapes.addAll(errors);
        return new ModelSpec(spec.service(), spec.packageName(), shapes);
    }

    /**
     * One expected {@code GENERATED_NAME_COLLISION}: the lowest shape ID of the colliding set, the diagnostic's
     * element ({@code type <FQCN>} of the lowest entry) and every colliding shape ID.
     */
    record Collision(String shape, String element, SortedSet<String> shapes) {
    }

    /**
     * The generated-name collisions of {@code serviceId}, derived from the canonical model independently of
     * {@code aws-codegen}: the V2 simple name of every structure, union, enum and intEnum of the service closure (an
     * error is {@link RandomSmithyModels#v2ExceptionName}, the input or output of exactly one operation is
     * {@code <Operation>Request} or {@code <Operation>Response}, anything else its capitalized contextual name) and
     * the service's base exception {@code <Service>Exception}, grouped case-insensitively.
     */
    static List<Collision> expectedCollisions(Model model, ShapeId serviceId, String packageName) {
        ServiceShape service = model.expectShape(serviceId, ServiceShape.class);
        String serviceName = capitalize(serviceId.getName());
        Map<ShapeId, List<String>> ioNames = new HashMap<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            String name = capitalize(operation.getId().getName(service));
            name = name.equals(serviceName) ? "Default" + name : name;
            ioNames.computeIfAbsent(operation.getInputShape(), k -> new ArrayList<>()).add(name + "Request");
            ioNames.computeIfAbsent(operation.getOutputShape(), k -> new ArrayList<>()).add(name + "Response");
        }
        // Lower-cased simple name -> (shape ID -> simple name), in shape-ID order.
        Map<String, SortedMap<ShapeId, String>> byName = new TreeMap<>();
        for (Shape shape : new Walker(model).walkShapes(service)) {
            ShapeType type = shape.getType();
            boolean publicType = type == ShapeType.STRUCTURE || type == ShapeType.UNION || type == ShapeType.ENUM
                                 || type == ShapeType.INT_ENUM;
            if (!publicType || shape.getId().getNamespace().equals(PRELUDE)) {
                continue;
            }
            String name = capitalize(shape.getId().getName(service));
            List<String> uses = ioNames.getOrDefault(shape.getId(), List.of());
            if (shape.hasTrait(ErrorTrait.class)) {
                name = RandomSmithyModels.v2ExceptionName(name, serviceName);
            } else if (type == ShapeType.STRUCTURE && uses.size() == 1) {
                name = uses.get(0);
            }
            byName.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new TreeMap<>()).put(shape.getId(), name);
        }
        byName.computeIfAbsent((serviceName + "Exception").toLowerCase(Locale.ROOT), k -> new TreeMap<>())
              .put(serviceId, serviceName + "Exception");
        List<Collision> result = new ArrayList<>();
        byName.values().stream().filter(group -> group.size() > 1).forEach(group -> {
            SortedSet<String> shapes = new TreeSet<>();
            group.keySet().forEach(id -> shapes.add(id.toString()));
            result.add(new Collision(group.firstKey().toString(),
                                     "type " + packageName + ".model." + group.get(group.firstKey()), shapes));
        });
        result.sort(Comparator.comparing(Collision::shape));
        return result;
    }

    private static String capitalize(String name) {
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Generation of a model with name collisions fails with exactly the expected diagnostics, each naming every
     * colliding shape and the Java type, and publishes nothing.
     */
    private static void assertRejectedAsCollisions(ModelSpec spec, Path models, Path output,
                                                   List<Collision> expected, String describe) {
        AwsCodegenException e;
        try {
            generate(spec.service(), spec.packageName(), models, output);
            throw new AssertionError("Generation of " + spec + " succeeded although it has colliding generated "
                                     + "names " + expected + "; model files:\n" + describe);
        } catch (AwsCodegenException failure) {
            e = failure;
        }
        List<String> actual = e.diagnostics().stream()
                               .map(d -> d.code() + " " + d.shape().orElse("<none>") + " " + d.integrations() + " "
                                         + d.element())
                               .toList();
        List<String> predicted = expected.stream()
                                         .map(c -> DiagnosticCode.GENERATED_NAME_COLLISION + " " + c.shape() + " "
                                                   + List.of(AwsCodegen.GENERATOR_IDENTIFIER) + " " + c.element())
                                         .toList();
        assertEquals(predicted, actual, () -> e.getMessage() + "\nmodel files:\n" + describe);
        for (int i = 0; i < expected.size(); i++) {
            String message = e.diagnostics().get(i).message();
            expected.get(i).shapes().forEach(shape -> assertTrue(message.contains(shape), message));
            assertTrue(message.contains(expected.get(i).element().substring("type ".length())), message);
        }
        assertFalse(Files.exists(output), "a rejected service publishes no source tree");
        assertFalse(Files.exists(output.resolveSibling(output.getFileName() + "-resources")),
                    "a rejected service publishes no resource tree");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Oracle
    // ---------------------------------------------------------------------------------------------------------------

    /** How the generator exposes the schema of a shape. */
    enum Kind {
        /** The exported service schema. */
        SERVICE,
        /** An exported operation schema. */
        OPERATION,
        /** The exported schema of a public model type: structure, union, enum or intEnum. */
        TYPE,
        /** The non-exported schema of another member target: list, map or simple shape. */
        SHAPE
    }

    /**
     * A member of an expected shape.
     *
     * @param id exact member ID
     * @param target exact target ID
     * @param targetType shape type of the target
     * @param traits the compared runtime traits by trait ID
     * @param unknown the unknown traits by trait ID, as node values
     */
    record ExpectedMember(ShapeId id, ShapeId target, ShapeType targetType, SortedMap<ShapeId, Trait> traits,
                          SortedMap<ShapeId, Node> unknown) {
    }

    /**
     * A shape of the expected inventory.
     *
     * @param id exact shape ID
     * @param type shape type
     * @param kind how its schema is exposed
     * @param members members in modeled order (enum and intEnum members included)
     * @param traits the compared runtime traits by trait ID
     * @param unknown the unknown traits by trait ID, as node values
     */
    record ExpectedShape(ShapeId id, ShapeType type, Kind kind, List<ExpectedMember> members,
                         SortedMap<ShapeId, Trait> traits, SortedMap<ShapeId, Node> unknown) {
    }

    /**
     * An operation's expected schema links.
     *
     * @param input the input structure, or {@code smithy.api#Unit}
     * @param output the output structure, or {@code smithy.api#Unit}
     * @param errors every error the operation can return in the service, sorted
     */
    record ExpectedLinks(ShapeId input, ShapeId output, List<ShapeId> errors) {
    }

    /** The expected inventory, built directly from the canonical model. */
    record Oracle(ShapeId service, SortedMap<ShapeId, ExpectedShape> shapes, SortedMap<ShapeId, ExpectedLinks> links,
                  int unitLinks, boolean hasCycle) {

        static Oracle of(Model model, ShapeId serviceId) {
            ServiceShape service = model.expectShape(serviceId, ServiceShape.class);
            SortedMap<ShapeId, ExpectedShape> shapes = new TreeMap<>();
            for (Shape shape : new Walker(model).walkShapes(service)) {
                if (shape.isMemberShape() || shape.isResourceShape() || prelude(shape.getId())) {
                    continue;
                }
                shapes.put(shape.getId(), expected(model, shape));
            }
            SortedMap<ShapeId, ExpectedLinks> links = new TreeMap<>();
            int unitLinks = 0;
            for (ExpectedShape shape : shapes.values()) {
                if (shape.kind() != Kind.OPERATION) {
                    continue;
                }
                OperationShape operation = model.expectShape(shape.id(), OperationShape.class);
                unitLinks += (operation.getInputShape().equals(UNIT) ? 1 : 0)
                             + (operation.getOutputShape().equals(UNIT) ? 1 : 0);
                // The errors an operation can return in the service: its own and the service's.
                Set<ShapeId> errors = new TreeSet<>(operation.getErrors());
                errors.addAll(service.getErrors());
                links.put(operation.getId(), new ExpectedLinks(operation.getInputShape(), operation.getOutputShape(),
                                                               List.copyOf(errors)));
            }
            return new Oracle(serviceId, shapes, links, unitLinks, hasCycle(shapes));
        }

        private static ExpectedShape expected(Model model, Shape shape) {
            Kind kind = switch (shape.getType()) {
                case SERVICE -> Kind.SERVICE;
                case OPERATION -> Kind.OPERATION;
                case STRUCTURE, UNION, ENUM, INT_ENUM -> Kind.TYPE;
                default -> Kind.SHAPE;
            };
            List<ExpectedMember> members = new ArrayList<>();
            for (MemberShape member : shape.members()) {
                members.add(new ExpectedMember(member.getId(), member.getTarget(),
                                               model.expectShape(member.getTarget()).getType(),
                                               runtimeTraits(member), unknownTraits(member)));
            }
            return new ExpectedShape(shape.getId(), shape.getType(), kind, List.copyOf(members), runtimeTraits(shape),
                                     unknownTraits(shape));
        }

        private static SortedMap<ShapeId, Trait> runtimeTraits(Shape shape) {
            SortedMap<ShapeId, Trait> result = new TreeMap<>();
            shape.getAllTraits().forEach((id, trait) -> {
                if (RUNTIME_TRAIT_IDS.contains(id)) {
                    result.put(id, trait);
                }
            });
            return result;
        }

        private static SortedMap<ShapeId, Node> unknownTraits(Shape shape) {
            SortedMap<ShapeId, Node> result = new TreeMap<>();
            shape.getAllTraits().forEach((id, trait) -> {
                if (trait instanceof DynamicTrait) {
                    result.put(id, trait.toNode());
                }
            });
            return result;
        }

        /** Whether some expected shape reaches itself through member targets. */
        private static boolean hasCycle(SortedMap<ShapeId, ExpectedShape> shapes) {
            for (ShapeId start : shapes.keySet()) {
                List<ShapeId> pending = new ArrayList<>();
                Set<ShapeId> seen = new HashSet<>();
                shapes.get(start).members().forEach(m -> pending.add(m.target()));
                while (!pending.isEmpty()) {
                    ShapeId id = pending.remove(pending.size() - 1);
                    if (id.equals(start)) {
                        return true;
                    }
                    ExpectedShape next = shapes.get(id);
                    if (next != null && seen.add(id)) {
                        next.members().forEach(m -> pending.add(m.target()));
                    }
                }
            }
            return false;
        }

        SortedSet<ShapeId> ids(Kind kind) {
            return shapes.values().stream().filter(s -> s.kind() == kind).map(ExpectedShape::id)
                         .collect(Collectors.toCollection(TreeSet::new));
        }

        /** Every shape and member ID with at least one unknown trait. */
        SortedSet<ShapeId> unknownTargets() {
            SortedSet<ShapeId> result = new TreeSet<>();
            for (ExpectedShape shape : shapes.values()) {
                if (!shape.unknown().isEmpty()) {
                    result.add(shape.id());
                }
                shape.members().stream().filter(m -> !m.unknown().isEmpty()).forEach(m -> result.add(m.id()));
            }
            return result;
        }

        long memberTraitCount(ShapeId trait) {
            return shapes.values().stream().flatMap(s -> s.members().stream())
                         .filter(m -> m.traits().containsKey(trait)).count();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generated views
    // ---------------------------------------------------------------------------------------------------------------

    /** The generated views of one service, compared with the oracle; every mismatch is recorded in problems. */
    static final class Views {
        final List<String> problems = new ArrayList<>();
        boolean reordered;

        private final Oracle oracle;
        private final ClassLoader loader;
        private final Path modelClasses;
        private final String modelPackage;
        private final String serviceName;
        private final String holder;
        private final Map<ShapeId, Schema> exported = new TreeMap<>();
        private final Map<ShapeId, Class<?>> types = new TreeMap<>();

        Views(Oracle oracle, String packageName, ClassLoader loader, Path modelClasses) {
            this.oracle = oracle;
            this.loader = loader;
            this.modelClasses = modelClasses;
            this.modelPackage = packageName + ".model";
            this.serviceName = oracle.service().getName();
            this.holder = modelPackage + ".internal." + serviceName + "Schemas";
        }

        void check() {
            exportedConstants();
            ServiceSchemaIndex index = CompiledSources.index(loader, holder);
            indexInventory(index);
            for (ExpectedShape shape : oracle.shapes().values()) {
                index.schema(shape.id()).ifPresentOrElse(schema -> fidelity(index, shape, schema),
                                                         () -> problems.add("no schema for " + shape.id()));
            }
            SortedSet<ShapeId> unknownTargets = new TreeSet<>(index.unknownTraits().keySet());
            same("unknown-trait targets", oracle.unknownTargets(), unknownTargets);
            modelTypes();
            registry();
            operations();
        }

        /** View (a): one distinct exported constant per service, operation and public type, and nothing else. */
        private void exportedConstants() {
            Map<Schema, String> names = new IdentityHashMap<>();
            for (Field field : load(holder).getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !Schema.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                Schema schema = (Schema) get(field, null);
                String previous = names.put(schema, field.getName());
                if (previous != null) {
                    problems.add("constants " + previous + " and " + field.getName() + " export the same object");
                } else if (schema.isMember()) {
                    problems.add("constant " + field.getName() + " exports member schema " + schema.id());
                } else if (exported.put(schema.id(), schema) != null) {
                    problems.add("two exported constants for " + schema.id());
                }
            }
            SortedSet<ShapeId> expected = new TreeSet<>(oracle.ids(Kind.SERVICE));
            expected.addAll(oracle.ids(Kind.OPERATION));
            expected.addAll(oracle.ids(Kind.TYPE));
            same("exported schema constants", expected, exported.keySet());
        }

        /** View (a): the index registers exactly the expected service, operations, types and shapes. */
        private void indexInventory(ServiceSchemaIndex index) {
            identical("index service", exported.get(oracle.service()), index.service());
            inventory("index operations", Kind.OPERATION, index.operations(), true);
            inventory("index types", Kind.TYPE, index.types(), true);
            inventory("index shapes", Kind.SHAPE, index.shapes(), false);
        }

        private void inventory(String what, Kind kind, List<Schema> schemas, boolean exportedKind) {
            SortedSet<ShapeId> ids = new TreeSet<>();
            for (Schema schema : schemas) {
                if (!ids.add(schema.id())) {
                    problems.add(what + ": " + schema.id() + " is listed twice");
                }
                if (exportedKind) {
                    identical(what + " " + schema.id(), exported.get(schema.id()), schema);
                }
            }
            same(what, oracle.ids(kind), ids);
        }

        /** View (a): one schema against its canonical shape, with its members and their targets. */
        private void fidelity(ServiceSchemaIndex index, ExpectedShape shape, Schema schema) {
            String where = shape.id().toString();
            if (!schema.id().equals(shape.id()) || schema.isMember()) {
                problems.add(where + ": indexed schema is " + schema.id() + (schema.isMember() ? " (a member)" : ""));
                return;
            }
            if (shape.kind() != Kind.SHAPE) {
                identical(where + " exported constant", exported.get(shape.id()), schema);
            }
            if (schema.type() != shape.type()) {
                problems.add(where + ": type " + schema.type() + ", expected " + shape.type());
            }
            traits(where, schema, shape.traits());
            unknown(where, index, shape.id(), shape.unknown());

            List<Schema> ordered = index.members(schema);
            List<String> expectedNames = shape.members().stream().map(m -> m.id().getMember().orElseThrow()).toList();
            List<String> orderedNames = ordered.stream().map(Schema::memberName).toList();
            if (!expectedNames.equals(orderedNames)) {
                problems.add(where + ": members in order " + orderedNames + ", expected " + expectedNames);
                return;
            }
            switch (shape.type()) {
                case ENUM -> same(where + " enum values", enumValues(shape, false), new TreeSet<>(
                    schema.stringEnumValues()));
                case INT_ENUM -> same(where + " intEnum values", enumValues(shape, true), new TreeSet<>(
                    schema.intEnumValues().stream().map(String::valueOf).toList()));
                default -> {
                    List<String> declared = schema.members().stream().map(Schema::memberName).toList();
                    if (!new TreeSet<>(declared).equals(new TreeSet<>(expectedNames))
                        || declared.size() != expectedNames.size()) {
                        problems.add(where + ": Schema.members() " + declared + ", expected " + expectedNames);
                        return;
                    }
                    reordered |= !declared.equals(expectedNames);
                    for (Schema member : ordered) {
                        identical(where + " member " + member.memberName(), schema.member(member.memberName()),
                                  member);
                    }
                    if (shape.type() == ShapeType.LIST) {
                        identical(where + " list member", ordered.get(0), schema.listMember());
                    } else if (shape.type() == ShapeType.MAP) {
                        identical(where + " map key", ordered.get(0), schema.mapKeyMember());
                        identical(where + " map value", ordered.get(1), schema.mapValueMember());
                    }
                }
            }
            for (int i = 0; i < ordered.size(); i++) {
                member(index, shape.members().get(i), ordered.get(i));
            }
        }

        private void member(ServiceSchemaIndex index, ExpectedMember expected, Schema member) {
            String where = expected.id().toString();
            if (!member.isMember() || !member.id().equals(expected.id())) {
                problems.add(where + ": member schema is " + member.id());
                return;
            }
            Schema target = member.memberTarget();
            if (!target.id().equals(expected.target())) {
                problems.add(where + ": targets " + target.id() + ", expected " + expected.target());
            } else if (target.type() != expected.targetType()) {
                problems.add(where + ": target type " + target.type() + ", expected " + expected.targetType());
            } else if (!prelude(expected.target())) {
                // Recursive or not, a member's target is the one schema object of that ID: the exported constant of
                // a type, or the indexed schema of a list, map or simple shape.
                identical(where + " target", index.schema(expected.target()).orElse(null), target);
                if (oracle.shapes().get(expected.target()).kind() == Kind.TYPE) {
                    identical(where + " target constant", exported.get(expected.target()), target);
                }
            }
            traits(where, member, expected.traits());
            unknown(where, index, expected.id(), expected.unknown());
        }

        private static SortedSet<String> enumValues(ExpectedShape shape, boolean intEnum) {
            SortedSet<String> result = new TreeSet<>();
            for (ExpectedMember member : shape.members()) {
                EnumValueTrait value = (EnumValueTrait) member.traits().get(EnumValueTrait.ID);
                result.add(intEnum ? String.valueOf(value.expectIntValue()) : value.expectStringValue());
            }
            return result;
        }

        /** The schema's own traits are exactly the expected runtime traits, by node value and trait class. */
        private void traits(String where, Schema schema, SortedMap<ShapeId, Trait> expected) {
            SortedMap<ShapeId, Node> actual = new TreeMap<>();
            for (Trait trait : directTraits(schema)) {
                if (actual.put(trait.toShapeId(), trait.toNode()) != null) {
                    problems.add(where + ": two schema traits have the ID " + trait.toShapeId());
                }
            }
            SortedMap<ShapeId, Node> expectedNodes = new TreeMap<>();
            expected.forEach((id, trait) -> expectedNodes.put(id, trait.toNode()));
            if (!expectedNodes.equals(actual)) {
                problems.add(where + ": schema traits " + render(actual) + ", expected " + render(expectedNodes));
                return;
            }
            for (Trait trait : expected.values()) {
                Trait direct = schema.getDirectTrait(TraitKey.get(trait.getClass()));
                if (direct == null || direct.getClass() != trait.getClass()) {
                    problems.add(where + ": getDirectTrait(" + trait.getClass().getSimpleName() + ") is " + direct);
                }
            }
            if (schema.getDirectTrait(TraitKey.get(DynamicTrait.class)) != null) {
                problems.add(where + ": carries an unknown trait by Java class");
            }
        }

        private void unknown(String where, ServiceSchemaIndex index, ShapeId target,
                             SortedMap<ShapeId, Node> expected) {
            SortedMap<ShapeId, Node> actual = new TreeMap<>();
            index.unknownTraits(target).asMap().forEach((id, trait) -> actual.put(id, trait.toNode()));
            if (!expected.equals(actual)) {
                problems.add(where + ": unknown traits " + render(actual) + ", expected " + render(expected));
            }
        }

        /** View (b): exactly one model type class per public type, holding the exported schema. */
        private void modelTypes() {
            List<String> classNames;
            try (Stream<Path> files = Files.list(modelClasses)) {
                classNames = files.map(p -> p.getFileName().toString())
                                  .filter(n -> n.endsWith(".class") && !n.contains("$"))
                                  .map(n -> modelPackage + "." + n.substring(0, n.length() - ".class".length()))
                                  .sorted().toList();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            int serviceExceptions = 0;
            int serviceResponses = 0;
            for (String className : classNames) {
                Class<?> type = load(className);
                Field schemaField = declaredField(type, "SCHEMA");
                if (schemaField == null) {
                    if (type.getSimpleName().equals(serviceName + "Exception")
                        && type.getSuperclass() == AwsServiceException.class) {
                        serviceExceptions++;
                    } else if (type.getSimpleName().equals(serviceName + "Response")
                               && type.getSuperclass() == AwsResponse.class
                               || type.getSimpleName().equals(serviceName + "ResponseMetadata")
                                  && type.getSuperclass() == AwsResponseMetadata.class) {
                        // The service's base response and its metadata view (task 16.1) are not modeled types.
                        serviceResponses++;
                    } else {
                        problems.add("model class " + className + " has no SCHEMA and is not the service exception");
                    }
                    continue;
                }
                Schema schema = (Schema) get(schemaField, null);
                identical("SCHEMA of " + className, exported.get(schema.id()), schema);
                Field idField = declaredField(type, "ID");
                Object id = idField == null ? null : get(idField, null);
                if (!schema.id().equals(id)) {
                    problems.add("ID of " + className + " is " + id + ", its SCHEMA is " + schema.id());
                }
                Class<?> previous = types.put(schema.id(), type);
                if (previous != null) {
                    problems.add("model classes " + previous.getName() + " and " + className + " both hold "
                                 + schema.id());
                }
            }
            same("model type classes", oracle.ids(Kind.TYPE), types.keySet());
            if (serviceExceptions != 1) {
                problems.add(serviceExceptions + " service base exceptions, expected 1");
            }
            if (serviceResponses != 2) {
                problems.add(serviceResponses + " service base response and metadata classes, expected 2");
            }
        }

        /** View (c): the registry maps exactly the structures and unions to their schema, class and builder. */
        private void registry() {
            SchemaTypeRegistry registry = (SchemaTypeRegistry) CompiledSources.invokeStatic(
                loader, modelPackage + ".internal." + serviceName + "TypeRegistry", "registry");
            SortedSet<ShapeId> expected = oracle.shapes().values().stream()
                                                .filter(s -> s.kind() == Kind.TYPE)
                                                .filter(s -> s.type() == ShapeType.STRUCTURE
                                                             || s.type() == ShapeType.UNION)
                                                .map(ExpectedShape::id)
                                                .collect(Collectors.toCollection(TreeSet::new));
            same("registry shape IDs", expected, registry.shapeIds());
            for (RegisteredType<?> entry : registry.entries()) {
                ShapeId id = entry.shapeId();
                identical("registry schema of " + id, exported.get(id), entry.schema());
                identical("registry schema lookup of " + id, exported.get(id), registry.schema(id).orElse(null));
                if (entry.type() != types.get(id) || registry.getShapeClass(id) != types.get(id)) {
                    problems.add("registry class of " + id + " is " + entry.type() + ", expected " + types.get(id));
                }
                identical("registry builder schema of " + id, exported.get(id), entry.createBuilder().schema());
            }
        }

        /** View (d): each operation descriptor links the exported operation, input, output and error schemas. */
        private void operations() {
            List<?> descriptors = (List<?>) CompiledSources.invokeStatic(
                loader, modelPackage.substring(0, modelPackage.length() - ".model".length()) + ".internal.operation."
                        + serviceName + "Operations", "operations");
            SortedSet<ShapeId> ids = new TreeSet<>();
            for (Object object : descriptors) {
                AwsOperationDescriptor descriptor = (AwsOperationDescriptor) object;
                ShapeId id = descriptor.id();
                ids.add(id);
                ExpectedLinks links = oracle.links().get(id);
                OperationSchemas schemas = descriptor.schemas().orElse(null);
                if (links == null || schemas == null) {
                    problems.add("operation " + id + ": unexpected, or no schemas");
                    continue;
                }
                if (!descriptor.serviceId().equals(oracle.service())) {
                    problems.add("operation " + id + ": service " + descriptor.serviceId());
                }
                identical("operation schema of " + id, exported.get(id), schemas.operation());
                identical("input schema of " + id, link(links.input()), schemas.input());
                identical("output schema of " + id, link(links.output()), schemas.output());
                List<Schema> errors = new ArrayList<>(schemas.errors());
                errors.sort(Comparator.comparing(Schema::id));
                same("errors of " + id, new TreeSet<>(links.errors()),
                     errors.stream().map(Schema::id).collect(Collectors.toCollection(TreeSet::new)));
                if (errors.size() == links.errors().size()) {
                    for (Schema error : errors) {
                        identical("error schema " + error.id() + " of " + id, exported.get(error.id()), error);
                    }
                }
            }
            same("operation descriptors", oracle.links().keySet(), ids);
        }

        /** The schema an operation links for {@code id}: Smithy Java's {@code Unit.SCHEMA} or the exported constant. */
        private Schema link(ShapeId id) {
            return id.equals(UNIT) ? Unit.SCHEMA : exported.get(id);
        }

        // -------------------------------------------------------------------------------------------------------------

        private void identical(String what, Object expected, Object actual) {
            if (expected == null || expected != actual) {
                problems.add(what + ": " + describe(actual) + " is not the same object as " + describe(expected));
            }
        }

        private <T> void same(String what, Set<T> expected, Set<T> actual) {
            if (!expected.equals(actual)) {
                Set<T> missing = new TreeSet<>(expected);
                missing.removeAll(actual);
                Set<T> extra = new TreeSet<>(actual);
                extra.removeAll(expected);
                problems.add(what + ": missing " + missing + ", extra " + extra);
            }
        }

        private Class<?> load(String className) {
            try {
                return Class.forName(className, true, loader);
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * The traits a schema holds itself (a member's own traits, not its target's), read from Smithy Java 1.6.1's trait
     * map: {@code Schema} has no public enumeration, and exactness (nothing extra) needs one. Member schemas keep their
     * own traits in {@code directTraits}; root schemas in {@code Schema.traits}.
     */
    static List<Trait> directTraits(Schema schema) {
        Field field = null;
        for (Class<?> type = schema.getClass(); type != null && field == null; type = type.getSuperclass()) {
            field = declaredField(type, "directTraits");
        }
        if (field == null) {
            field = Objects.requireNonNull(declaredField(Schema.class, "traits"), "Schema.traits");
        }
        Object traitMap = get(field, schema);
        Field values = Objects.requireNonNull(declaredField(traitMap.getClass(), "values"), "TraitMap.values");
        List<Trait> result = new ArrayList<>();
        for (Trait trait : (Trait[]) get(values, traitMap)) {
            if (trait != null) {
                result.add(trait);
            }
        }
        return result;
    }

    private static Field declaredField(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static Object get(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static ShapeId traitId(Class<? extends Trait> type) {
        try {
            return (ShapeId) type.getField("ID").get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(type.getName(), e);
        }
    }

    private static boolean prelude(ShapeId id) {
        return id.getNamespace().equals(PRELUDE);
    }

    private static String describe(Object value) {
        return value instanceof Schema schema ? "schema " + schema.id() + "@" + System.identityHashCode(schema)
                                              : String.valueOf(value);
    }

    private static String render(Map<ShapeId, Node> traits) {
        Map<String, String> rendered = new LinkedHashMap<>();
        traits.forEach((id, node) -> rendered.put(id.toString(), Node.printJson(node)));
        return rendered.toString();
    }

    /** Generates {@code service} from the model files in {@code models}; returns the generated source root. */
    private static Path generate(ShapeId service, String packageName, Path models, Path output) {
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest(service.toString(), packageName, models,
                                                                            output), List.of());
        return result.sourceRoot();
    }

    /**
     * Compiles the generated model types, schema holder, registry and operation descriptors (not the client or
     * metadata classes, which the views do not need) into {@code classes}, loads them and compares every view.
     */
    private static Views check(Oracle oracle, String packageName, Path sources, Path classes) throws IOException {
        String packagePath = packageName.replace('.', '/');
        CompiledSources compiled = CompiledSources.compile(sources, classes, packagePath + "/model/",
                                                           packagePath + "/internal/operation/");
        try (URLClassLoader loader = compiled.loader()) {
            Views views = new Views(oracle, packageName, loader, classes.resolve(packagePath + "/model"));
            views.check();
            return views;
        }
    }

    /** The model files assembled independently of {@code aws-codegen}, with unknown traits allowed. */
    private static Model assemble(Path models, Collection<String> files) {
        ClassLoader loader = AwsCodegen.class.getClassLoader();
        ModelAssembler assembler = Model.assembler(loader).discoverModels(loader)
                                        .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
        files.forEach(file -> assembler.addImport(models.resolve(file)));
        return assembler.assemble().unwrap();
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
}
