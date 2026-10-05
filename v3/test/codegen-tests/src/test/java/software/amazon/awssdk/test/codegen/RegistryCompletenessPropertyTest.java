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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.smithy.runtime.document.SchemaDocuments;
import software.amazon.awssdk.smithy.runtime.registry.RegisteredType;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.smithy.runtime.registry.UnknownDiscriminatorException;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels.Member;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels.ModelSpec;
import software.amazon.awssdk.test.codegen.support.SchemaFidelityModels;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Property 16 (Requirements 6.5, 6.6, 6.7, 6.9): for any generated service artifact, its one primary registry contains
 * every eligible public aggregate/error generated type, and each shape ID resolves to that type's exact schema, Java
 * class and functioning builder factory.
 *
 * <p><b>Services.</b> Five tries in six draw a fresh {@link RandomSmithyModels} service enriched with
 * {@link SchemaFidelityModels} (recursion, requiredness, defaults, constraints, unreferenced shapes, one or two
 * namespaces), rendered as random model files, generated with {@code aws-codegen} and compiled in-test. Registries
 * are partitioned only above 200 types, which random models of compilable size do not reach, so one try in six uses
 * one of two pooled large services (compiled once): a random service padded to exactly 201 registered types (a
 * partition of 200 and one of 1, the boundary) and one padded to 437 (three partitions). Every try, pooled or not,
 * loads the compiled classes in a fresh class loader, so it observes a fresh, never-used registry.
 *
 * <p><b>Oracle.</b> The canonical model is assembled independently of {@code aws-codegen} with Smithy's
 * {@link ModelAssembler}. The expected registered set is every structure (including inputs, outputs and errors) and
 * union of the service closure ({@link Walker}) outside the {@code smithy.api} prelude; enums, intEnums, collections,
 * simple shapes, operations, the service, resources, members and shapes outside the closure are not registered. The
 * expected schema and class of an ID are read from the generated metadata: the {@code <Service>ServiceMetadata}
 * schema index and the model class whose {@code ID} is the shape ID (whose {@code SCHEMA} must be the same object).
 *
 * <p><b>Per try.</b>
 * <ol>
 *   <li><em>One primary registry.</em> {@code <Service>ServiceMetadata.typeRegistry()} and
 *       {@code <Service>TypeRegistry.registry()} return the same object on every call; the holder is the only public
 *       registry class (partitions are package-private).</li>
 *   <li><em>Laziness.</em> Before any lookup, getting the registry, {@code shapeIds()} and {@code contains} of every
 *       expected and near-miss ID load no partition: observed both through the registry's partition state (read
 *       reflectively from {@code SchemaTypeRegistry}'s private fields, the only way to see an unloaded partition) and
 *       through class loading (only the metadata and registry classes are loaded, no model type class and not the
 *       schema holder, which a partition's entries reference). Both are needed: a partition whose entries are
 *       computed eagerly but handed over through a supplier looks unloaded to the state probe, but loads the model
 *       classes.</li>
 *   <li><em>Queries.</em> A generated interleaving of {@code contains}, {@code entry}, {@code schema},
 *       {@code getShapeClass}, {@code createBuilder}, {@code deserialize}/{@code deserializeStrict}, {@code shapeIds}
 *       and {@code entries} over exact IDs and near misses (case variants, member IDs, other namespaces, prelude IDs,
 *       enum, collection, simple, operation, service and resource IDs, V2 Java names used as shape names and unknown
 *       IDs) is run twice, in generated order on one fresh registry and in a shuffled order on another. After each
 *       query, every result matches the oracle, repeated lookups return the identical entry, the loaded partitions are
 *       exactly those declaring a registered ID looked up so far, and near misses return empty, {@code null} or
 *       {@code false} or fail with {@link UnknownDiscriminatorException} naming the ID. The two runs give the same
 *       result for every query.</li>
 *   <li><em>Completeness sweep.</em> For every expected ID, the entry has the exact ID, the exported schema (the index
 *       schema and the class's {@code SCHEMA}, as the same object) and the model class; {@code createBuilder()},
 *       {@code builderFactory().get()} and the registry's {@code createBuilder} return a new builder each time whose
 *       {@code schema()} is the exported constant; builders from the factory, filled with generated values through
 *       {@code setMemberValue}, build values equal to ones built through the class's own {@code builder()} (and two
 *       builders filled differently do not share state); and {@code deserialize} and {@code deserializeStrict} of
 *       {@code SchemaDocuments.of(value)} return an equal value of the exact class. {@code entries()} lists exactly the
 *       entries, in ID order, as the identical objects. The index's structures and unions are the registered IDs.</li>
 * </ol>
 *
 * <p>Values are generated from the schemas with a seeded {@link Random}: every member kind the models use, required
 * members always present, optional ones at random, one member per union, bounded depth. Modeled errors are exceptions,
 * whose equality is identity, so their values are compared member by member through {@code getMemberValue}.
 *
 * <p>{@link #plantedDefectsAreReported} checks that the checks are not vacuous.
 *
 * <p><b>Tries.</b> 100 (the design's minimum). A fresh small service takes a fraction of a second to generate and
 * compile and the pooled large services a few seconds each, once, so the property runs in under half a minute.
 * Coverage checks require single-file, two-partition and three-partition registries and every near-miss kind.
 */
class RegistryCompletenessPropertyTest {

    private static final String PRELUDE = "smithy.api";
    private static final int MAX_DEPTH = 3;
    private static final Path WORK = Path.of("target", "registry-completeness");

    // Feature: aws-sdk-java-v3, Property 16: Registry completeness and exact lookup
    @Property(tries = 100, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 16: Registry completeness and exact lookup")
    void registryIsCompleteAndLookupIsExact(@ForAll("scenarios") Scenario scenario) throws IOException {
        Compiled compiled;
        Path freshDirectory = null;
        if (scenario.pooled() >= 0) {
            compiled = Pool.compiled(scenario.pooled());
        } else {
            freshDirectory = Files.createTempDirectory(Files.createDirectories(WORK), "try-");
            compiled = Compiled.of(scenario.model(), freshDirectory);
        }
        try {
            collectStatistics(scenario, compiled);
            check(scenario, compiled);
        } catch (AssertionError | RuntimeException e) {
            throw new AssertionError("Property 16 failed for " + scenario + "; model files:\n"
                                     + scenario.model().describe(), e);
        } finally {
            if (freshDirectory != null) {
                deleteRecursively(freshDirectory);
            }
        }
    }

    /**
     * The checks are not vacuous: each defect planted in otherwise correct generated sources is reported, and the
     * unmodified sources pass. The defects are an eagerly loaded partition, a service metadata registry that is not
     * the primary registry, and a builder factory that returns one shared builder.
     */
    @Example
    void plantedDefectsAreReported() throws IOException {
        Random random = new Random(16);
        ModelFiles model = ModelFiles.of(
            SchemaFidelityModels.enrich(RandomSmithyModels.model(random, 2, 8, 0, 1, false), random), random);
        Path directory = Files.createTempDirectory(Files.createDirectories(WORK), "mutants-");
        try {
            Generated generated = Generated.of(model, directory);
            List<Query> queries = new ArrayList<>();
            for (ShapeId id : generated.oracle().registered()) {
                queries.add(new Query(Op.ENTRY, "model structure or union", id, 1));
                queries.add(new Query(Op.CREATE_BUILDER, "model structure or union", id, 2));
            }
            Scenario scenario = new Scenario(model, -1, queries, nearMisses(model.spec()), 3);
            check(scenario, generated.compile(directory.resolve("classes")));

            String service = model.spec().service().getName();
            Path root = generated.sourceRoot().resolve(model.spec().packageName().replace('.', '/'));
            Path registry = root.resolve("model/internal/" + service + "TypeRegistry.java");
            Path metadata = root.resolve(service + "ServiceMetadata.java");
            String registryText = Files.readString(registry);
            String entries = service + "TypeRegistry::entries)";
            Matcher factory = Pattern.compile("(\\w+)::builder\\)").matcher(registryText);
            assertTrue(factory.find(), registryText);
            String sharedBuilder = "java.util.Optional.of(" + factory.group(1) + ".builder())::get)";

            record Mutant(String name, Path file, String from, String to, String expected) {
            }
            List<Mutant> mutants = List.of(
                new Mutant("eager partition", registry, entries,
                           "java.util.Optional.of(" + service + "TypeRegistry.entries())::get)",
                           "classes loaded by getting the registry, shapeIds() and contains"),
                new Mutant("other metadata registry", metadata, "return " + service + "TypeRegistry.registry();",
                           "return SchemaTypeRegistry.builder().build();",
                           "ServiceMetadata.typeRegistry() is the primary registry"),
                new Mutant("shared builder", registry, factory.group(0), sharedBuilder, "returns a new builder"));
            for (Mutant mutant : mutants) {
                String original = Files.readString(mutant.file());
                assertTrue(original.contains(mutant.from()), mutant.name() + ": " + mutant.from() + " not in\n"
                                                              + original);
                Files.writeString(mutant.file(), original.replace(mutant.from(), mutant.to()));
                try {
                    Path classes = directory.resolve("classes-" + mutant.name().replace(' ', '-'));
                    Compiled compiled = generated.compile(classes);
                    String failure = null;
                    try {
                        check(scenario, compiled);
                    } catch (AssertionError | RuntimeException e) {
                        StringBuilder causes = new StringBuilder();
                        for (Throwable t = e; t != null; t = t.getCause()) {
                            causes.append(t).append(" caused by ");
                        }
                        failure = causes.toString();
                    }
                    assertTrue(failure != null && failure.contains(mutant.expected()),
                               mutant.name() + ": expected a failure mentioning '" + mutant.expected() + "', got "
                               + failure);
                } finally {
                    Files.writeString(mutant.file(), original);
                }
            }
        } finally {
            deleteRecursively(directory);
        }
    }

    @AfterContainer
    static void deletePooledServices() throws IOException {
        Pool.clear();
    }

    private static void collectStatistics(Scenario scenario, Compiled compiled) {
        Oracle oracle = compiled.oracle();
        Statistics.label("registry").collect(compiled.partitionClasses() == 0 ? "single file"
                                                                               : compiled.partitionClasses()
                                                                                 + " partitions");
        Statistics.label("registry").coverage(c -> c.check("single file").percentage(p -> p >= 50));
        Statistics.label("registry").coverage(c -> c.check("2 partitions").count(n -> n >= 1));
        Statistics.label("registry").coverage(c -> c.check("3 partitions").count(n -> n >= 1));
        for (Query query : scenario.queries()) {
            boolean registered = oracle.registered().contains(query.id());
            Statistics.label("query ID").collect(query.kind() + (registered ? " (registered)" : " (not registered)"));
        }
        Statistics.label("query ID").coverage(c -> {
            c.check("model structure or union (registered)").count(n -> n >= 100);
            c.check("model structure or union (not registered)").count(n -> n >= 1);
            for (String kind : List.of("case variant", "member", "other namespace", "prelude", "enum",
                                       "collection or simple", "operation, service or resource", "V2 Java name",
                                       "unknown")) {
                c.check(kind + " (not registered)").count(n -> n >= 5);
            }
        });
    }

    private static void check(Scenario scenario, Compiled compiled) throws IOException {
        try (Run first = compiled.run()) {
            first.checkPrimaryRegistryAndLaziness(scenario.nearMisses());
            List<String> outcomes = first.execute(scenario.queries());

            try (Run second = compiled.run()) {
                second.checkPrimaryRegistryAndLaziness(scenario.nearMisses());
                List<Integer> order = new ArrayList<>();
                for (int i = 0; i < scenario.queries().size(); i++) {
                    order.add(i);
                }
                Collections.shuffle(order, new Random(scenario.shuffleSeed()));
                List<Query> shuffled = order.stream().map(scenario.queries()::get).toList();
                List<String> shuffledOutcomes = second.execute(shuffled);
                for (int i = 0; i < order.size(); i++) {
                    assertEquals(outcomes.get(order.get(i)), shuffledOutcomes.get(i),
                                 "query " + scenario.queries().get(order.get(i)) + " gives a different result in "
                                 + "the order " + shuffled + " than in " + scenario.queries());
                }
            }

            first.sweep(new Random(scenario.shuffleSeed() ^ 0x5DEECE66DL));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Scenarios
    // ---------------------------------------------------------------------------------------------------------------

    /** A rendered model: its spec and model files (relative path to content). */
    record ModelFiles(ModelSpec spec, Map<String, String> files) {

        static ModelFiles of(ModelSpec spec, Random random) {
            return new ModelFiles(spec, RandomSmithyModels.render(spec, random));
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
    }

    /** A registry operation of a query. */
    enum Op {
        CONTAINS, ENTRY, SCHEMA, SHAPE_CLASS, CREATE_BUILDER, DESERIALIZE, SHAPE_IDS, ENTRIES;

        /** Whether the operation may load the partition declaring its ID. */
        boolean loadsPartition() {
            return this == ENTRY || this == SCHEMA || this == SHAPE_CLASS || this == CREATE_BUILDER
                   || this == DESERIALIZE;
        }
    }

    /** A candidate ID of a query and how it was derived. */
    record Candidate(String kind, ShapeId id) {
    }

    /**
     * One registry query.
     *
     * @param op the operation
     * @param kind how the ID was derived (for statistics; the oracle alone decides whether it is registered)
     * @param id the queried ID (ignored by {@link Op#SHAPE_IDS} and {@link Op#ENTRIES})
     * @param valueSeed seed of the value a {@link Op#DESERIALIZE} of a registered ID converts to a document
     */
    record Query(Op op, String kind, ShapeId id, long valueSeed) {
        @Override
        public String toString() {
            return op == Op.SHAPE_IDS || op == Op.ENTRIES ? op.toString() : op + "(" + id + ")";
        }
    }

    /**
     * One try.
     *
     * @param model the service model
     * @param pooled the index of the pooled large service, or -1 for a fresh one
     * @param queries the generated queries
     * @param nearMisses every near-miss candidate of the model (for the laziness checks of {@code contains})
     * @param shuffleSeed seed of the second run's query order and of the sweep's values
     */
    record Scenario(ModelFiles model, int pooled, List<Query> queries, List<Candidate> nearMisses, long shuffleSeed) {
        @Override
        public String toString() {
            return model.spec() + (pooled >= 0 ? " (pooled #" + pooled + ")" : "") + " queries " + queries;
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Tuple.Tuple2<ModelFiles, Integer>> fresh = Combinators.combine(
            Arbitraries.integers().between(1, 4),
            Arbitraries.integers().between(2, 14),
            Arbitraries.integers().between(0, 2),
            Arbitraries.integers().between(0, 3),
            Arbitraries.of(false, true),
            Arbitraries.longs().withoutEdgeCases()
        ).as((operations, data, resources, errors, twoNamespaces, seed) -> {
            Random random = new Random(seed);
            ModelSpec base = RandomSmithyModels.model(random, operations, data, resources, errors, twoNamespaces);
            return Tuple.of(ModelFiles.of(SchemaFidelityModels.enrich(base, random), random), -1);
        });
        Arbitrary<Tuple.Tuple2<ModelFiles, Integer>> pooled = Arbitraries.integers().between(0, Pool.SIZE - 1)
                                                                         .map(i -> Tuple.of(Pool.model(i), i));
        return Arbitraries.frequencyOf(Tuple.of(5, fresh), Tuple.of(1, pooled)).flatMap(model -> {
            ModelSpec spec = model.get1().spec();
            List<Candidate> nearMisses = nearMisses(spec);
            return Combinators.combine(queries(spec, nearMisses), Arbitraries.longs().withoutEdgeCases())
                              .as((queries, shuffle) -> new Scenario(model.get1(), model.get2(), queries, nearMisses,
                                                                     shuffle));
        });
    }

    private static Arbitrary<List<Query>> queries(ModelSpec spec, List<Candidate> nearMisses) {
        List<Candidate> aggregates = new ArrayList<>();
        for (RandomSmithyModels.Shape shape : spec.shapes()) {
            if (shape.type().equals("structure") || shape.type().equals("union")) {
                aggregates.add(new Candidate("model structure or union", shape.id()));
            }
        }
        // Near misses: a kind first, then an ID of that kind, so that rare kinds (enums, V2 names) are queried too.
        Map<String, List<Candidate>> byKind = new LinkedHashMap<>();
        nearMisses.forEach(c -> byKind.computeIfAbsent(c.kind(), k -> new ArrayList<>()).add(c));
        Arbitrary<Candidate> nearMiss = Arbitraries.of(List.copyOf(byKind.keySet()))
                                                   .flatMap(kind -> Arbitraries.of(byKind.get(kind)));
        Arbitrary<Candidate> candidates = Arbitraries.frequencyOf(Tuple.of(1, Arbitraries.of(aggregates)),
                                                                  Tuple.of(1, nearMiss));
        Arbitrary<Op> ops = Arbitraries.frequencyOf(
            Tuple.of(9, Arbitraries.just(Op.CONTAINS)), Tuple.of(9, Arbitraries.just(Op.ENTRY)),
            Tuple.of(6, Arbitraries.just(Op.SCHEMA)), Tuple.of(6, Arbitraries.just(Op.SHAPE_CLASS)),
            Tuple.of(6, Arbitraries.just(Op.CREATE_BUILDER)), Tuple.of(9, Arbitraries.just(Op.DESERIALIZE)),
            Tuple.of(3, Arbitraries.just(Op.SHAPE_IDS)), Tuple.of(1, Arbitraries.just(Op.ENTRIES)));
        return Combinators.combine(ops, candidates, Arbitraries.longs().withoutEdgeCases())
                          .as((op, candidate, seed) -> new Query(op, candidate.kind(), candidate.id(), seed))
                          .list().ofMinSize(4).ofMaxSize(40);
    }

    /**
     * IDs near the registered ones, derived from the spec alone; whether one is registered is decided by the oracle
     * (for example, swapping the namespace of a shape may name another real shape).
     */
    static List<Candidate> nearMisses(ModelSpec spec) {
        Set<Candidate> result = new LinkedHashSet<>();
        List<String> namespaces = List.copyOf(spec.namespaces());
        for (RandomSmithyModels.Shape shape : spec.shapes()) {
            ShapeId id = shape.id();
            String name = id.getName();
            String namespace = id.getNamespace();
            switch (shape.type()) {
                case "structure", "union" -> {
                    for (String variant : List.of(name.toLowerCase(Locale.ROOT), name.toUpperCase(Locale.ROOT),
                                                  swapFirst(name))) {
                        if (!variant.equals(name)) {
                            result.add(new Candidate("case variant", ShapeId.fromParts(namespace, variant)));
                        }
                    }
                    result.add(new Candidate("case variant",
                                             ShapeId.fromParts(namespace.toUpperCase(Locale.ROOT), name)));
                    result.add(new Candidate("other namespace", ShapeId.fromParts("example.elsewhere", name)));
                    namespaces.stream().filter(n -> !n.equals(namespace)).forEach(
                        other -> result.add(new Candidate("other namespace", ShapeId.fromParts(other, name))));
                    result.add(new Candidate("prelude", ShapeId.fromParts(PRELUDE, name)));
                    result.add(new Candidate("unknown", ShapeId.fromParts(namespace, name + "Extra")));
                    if (shape.traits().containsKey("smithy.api#error")) {
                        String v2 = RandomSmithyModels.v2ExceptionName(name, spec.service().getName());
                        if (!v2.equals(name)) {
                            result.add(new Candidate("V2 Java name", ShapeId.fromParts(namespace, v2)));
                        }
                    }
                }
                case "enum", "intEnum" -> result.add(new Candidate("enum", id));
                case "operation" -> {
                    result.add(new Candidate("operation, service or resource", id));
                    result.add(new Candidate("V2 Java name", ShapeId.fromParts(namespace, name + "Request")));
                    result.add(new Candidate("V2 Java name", ShapeId.fromParts(namespace, name + "Response")));
                }
                case "service", "resource" -> result.add(new Candidate("operation, service or resource", id));
                default -> result.add(new Candidate("collection or simple", id));
            }
            for (Member member : shape.members()) {
                result.add(new Candidate("member", ShapeId.fromParts(namespace, name, member.name())));
            }
        }
        for (String prelude : List.of("Unit", "String", "Integer", "Document", "Timestamp", "BigDecimal", "Blob")) {
            result.add(new Candidate("prelude", ShapeId.fromParts(PRELUDE, prelude)));
        }
        result.add(new Candidate("unknown", ShapeId.fromParts("example.unknown", "Nothing")));
        result.add(new Candidate("unknown",
                                 ShapeId.fromParts(spec.service().getNamespace(), "Zz" + spec.shapes().size())));
        return List.copyOf(result);
    }

    private static String swapFirst(String name) {
        char first = name.charAt(0);
        char swapped = Character.isUpperCase(first) ? Character.toLowerCase(first) : Character.toUpperCase(first);
        return swapped + name.substring(1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Pooled large services
    // ---------------------------------------------------------------------------------------------------------------

    /** Two large services, padded to exact registered-type counts, generated and compiled once on first use. */
    private static final class Pool {
        static final int SIZE = 2;
        private static final long[] SEEDS = {1601, 1602};
        private static final int[] REGISTERED = {201, 437};
        private static final ModelFiles[] MODELS = new ModelFiles[SIZE];
        private static final Compiled[] COMPILED = new Compiled[SIZE];

        static synchronized ModelFiles model(int index) {
            if (MODELS[index] == null) {
                MODELS[index] = pad(SEEDS[index], REGISTERED[index]);
            }
            return MODELS[index];
        }

        /** Deletes the compiled pooled services; they are compiled again on next use. */
        static synchronized void clear() throws IOException {
            for (int i = 0; i < SIZE; i++) {
                if (COMPILED[i] != null) {
                    deleteRecursively(COMPILED[i].classes().getParent());
                    COMPILED[i] = null;
                }
            }
        }

        static synchronized Compiled compiled(int index) throws IOException {
            if (COMPILED[index] == null) {
                Path directory = Files.createTempDirectory(Files.createDirectories(WORK), "pooled-" + index + "-");
                COMPILED[index] = Compiled.of(model(index), directory);
                assertEquals(REGISTERED[index], COMPILED[index].oracle().registered().size(), "pooled service size");
            }
            return COMPILED[index];
        }

        /**
         * A random service whose closure has exactly {@code registered} structures and unions: padding structures and
         * unions ({@code Zpad<n>}) hang off a chain of hub structures ({@code ZpadHub<n>}, at most 40 pads each)
         * reachable from an operation input.
         */
        private static ModelFiles pad(long seed, int registered) {
            Random random = new Random(seed);
            ModelSpec base = SchemaFidelityModels.enrich(RandomSmithyModels.model(random, 2, 10, 0, 1, true), random);
            int needed;
            try {
                Path directory = Files.createTempDirectory(Files.createDirectories(WORK), "pad-");
                try {
                    ModelFiles files = ModelFiles.of(base, new Random(seed));
                    files.write(directory);
                    needed = registered - Oracle.of(assemble(directory, files.files().keySet()), base.service())
                                                .registered().size();
                } finally {
                    deleteRecursively(directory);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            assertTrue(needed > 0, "the base service is too large to pad");
            String namespace = base.service().getNamespace();
            int hubs = (needed + 40) / 41;
            int pads = needed - hubs;
            List<RandomSmithyModels.Shape> added = new ArrayList<>();
            int pad = 0;
            for (int hub = 0; hub < hubs; hub++) {
                List<Member> members = new ArrayList<>();
                for (int i = 0; i < 40 && pad < pads; i++, pad++) {
                    ShapeId padId = ShapeId.fromParts(namespace, "Zpad" + pad);
                    members.add(new Member("p" + pad, padId.toString(), Map.of()));
                    List<Member> padMembers = pad % 2 == 0
                                              ? List.of(new Member("v", "smithy.api#String", Map.of()))
                                              : List.of(new Member("a", "smithy.api#String", Map.of()),
                                                        new Member("b", "smithy.api#Integer", Map.of()));
                    added.add(shape(padId, pad % 2 == 0 ? "structure" : "union", padMembers));
                }
                if (hub + 1 < hubs) {
                    members.add(new Member("next", ShapeId.fromParts(namespace, "ZpadHub" + (hub + 1)).toString(),
                                           Map.of()));
                }
                added.add(shape(ShapeId.fromParts(namespace, "ZpadHub" + hub), "structure", members));
            }
            ShapeId input = base.ofType("operation").stream()
                                .map(id -> base.shape(id).input())
                                .filter(i -> i != null && !i.equals(RandomSmithyModels.UNIT))
                                .map(ShapeId::from)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("no operation input to attach padding to"));
            List<RandomSmithyModels.Shape> shapes = new ArrayList<>();
            for (RandomSmithyModels.Shape shape : base.shapes()) {
                if (shape.id().equals(input)) {
                    List<Member> members = new ArrayList<>(shape.members());
                    members.add(new Member("zpadHub", ShapeId.fromParts(namespace, "ZpadHub0").toString(), Map.of()));
                    shape = new RandomSmithyModels.Shape(shape.id(), shape.type(), members, shape.traits(),
                                                         shape.version(), shape.input(), shape.output(),
                                                         shape.errors(), shape.operations(), shape.resources(),
                                                         shape.identifiers());
                }
                shapes.add(shape);
            }
            shapes.addAll(added);
            return ModelFiles.of(new ModelSpec(base.service(), base.packageName(), shapes), new Random(seed + 1));
        }

        private static RandomSmithyModels.Shape shape(ShapeId id, String type, List<Member> members) {
            return new RandomSmithyModels.Shape(id, type, members, Map.of(), null, null, null, List.of(), List.of(),
                                                List.of(), Map.of());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Oracle
    // ---------------------------------------------------------------------------------------------------------------

    /** The expected registered IDs, from the canonical model. */
    record Oracle(ShapeId service, SortedSet<ShapeId> registered) {
        static Oracle of(Model model, ShapeId service) {
            SortedSet<ShapeId> registered = new TreeSet<>();
            for (Shape shape : new Walker(model).walkShapes(model.expectShape(service))) {
                if ((shape.getType() == ShapeType.STRUCTURE || shape.getType() == ShapeType.UNION)
                    && !shape.getId().getNamespace().equals(PRELUDE)) {
                    registered.add(shape.getId());
                }
            }
            return new Oracle(service, Collections.unmodifiableSortedSet(registered));
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

    // ---------------------------------------------------------------------------------------------------------------
    // Compiled services and runs
    // ---------------------------------------------------------------------------------------------------------------

    /** A generated service: its spec, oracle (from the independently assembled model) and generated source root. */
    record Generated(ModelSpec spec, Oracle oracle, Path sourceRoot) {
        static Generated of(ModelFiles model, Path directory) throws IOException {
            ModelSpec spec = model.spec();
            Path models = model.write(directory.resolve("model"));
            Oracle oracle = Oracle.of(assemble(models, model.files().keySet()), spec.service());
            AwsCodegenResult result = AwsCodegen.generate(
                new AwsCodegenRequest(spec.service().toString(), spec.packageName(), models, directory.resolve("out")),
                List.of());
            return new Generated(spec, oracle, result.sourceRoot());
        }

        Compiled compile(Path classes) throws IOException {
            return Compiled.of(this, classes);
        }
    }

    /**
     * A generated and compiled service.
     *
     * @param oracle the expected registry content
     * @param classes the compiled classes directory
     * @param metadataClass the {@code <Service>ServiceMetadata} class name
     * @param registryClass the {@code <Service>TypeRegistry} class name
     * @param modelPackage the model package
     * @param partitionClasses the number of generated {@code <Service>TypeRegistry<n>} partition classes
     */
    record Compiled(Oracle oracle, Path classes, String metadataClass, String registryClass, String modelPackage,
                    int partitionClasses) {

        static Compiled of(ModelFiles model, Path directory) throws IOException {
            return Generated.of(model, directory).compile(directory.resolve("classes"));
        }

        static Compiled of(Generated generated, Path classes) throws IOException {
            ModelSpec spec = generated.spec();
            Oracle oracle = generated.oracle();
            CompiledSources.compile(generated.sourceRoot(), classes);
            String packagePath = spec.packageName().replace('.', '/');
            String registry = single(classes, packagePath + "/model/internal", "TypeRegistry");
            String metadata = single(classes, packagePath, "ServiceMetadata");
            String registryName = registry.substring(registry.lastIndexOf('.') + 1);
            int partitions;
            try (Stream<Path> files = Files.list(classes.resolve(packagePath + "/model/internal"))) {
                partitions = (int) files.map(p -> p.getFileName().toString())
                                        .filter(n -> n.matches(Pattern.quote(registryName)
                                                               + "[0-9]+\\.class"))
                                        .count();
            }
            return new Compiled(oracle, classes, metadata, registry, spec.packageName() + ".model", partitions);
        }

        /** The one class of {@code directory} (not nested) whose simple name ends with {@code suffix}. */
        private static String single(Path classes, String directory, String suffix) throws IOException {
            List<String> names;
            try (Stream<Path> files = Files.list(classes.resolve(directory))) {
                names = files.map(p -> p.getFileName().toString())
                             .filter(n -> n.endsWith(suffix + ".class") && !n.contains("$"))
                             .map(n -> directory.replace('/', '.') + "." + n.substring(0, n.length() - 6))
                             .sorted().toList();
            }
            assertEquals(1, names.size(), "classes ending with " + suffix + " in " + directory + ": " + names);
            return names.get(0);
        }

        /** A fresh class loader over the compiled classes: a registry never used before. */
        Run run() {
            return new Run(this, new TrackingLoader(classes));
        }
    }

    /** A class loader of generated classes that records every class it defines; runtime classes are shared. */
    static final class TrackingLoader extends URLClassLoader {
        static {
            registerAsParallelCapable();
        }

        private final Set<String> defined = new ConcurrentSkipListSet<>();

        TrackingLoader(Path classes) {
            super(new URL[] {url(classes)}, RegistryCompletenessPropertyTest.class.getClassLoader());
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            Class<?> type = super.findClass(name);
            defined.add(name);
            return type;
        }

        SortedSet<String> defined() {
            return new TreeSet<>(defined);
        }

        private static URL url(Path path) {
            try {
                return path.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(path.toString(), e);
            }
        }
    }

    /** One fresh registry of a compiled service and the checks run against it. */
    static final class Run implements AutoCloseable {
        private final Compiled compiled;
        private final TrackingLoader loader;
        private final Oracle oracle;
        private SchemaTypeRegistry registry;
        private Map<ShapeId, Object> partitionOf;
        private final Set<Object> expectedLoaded = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Map<ShapeId, RegisteredType<?>> seen = new HashMap<>();
        private Map<ShapeId, Class<?>> modelClasses;
        private ServiceSchemaIndex index;

        Run(Compiled compiled, TrackingLoader loader) {
            this.compiled = compiled;
            this.loader = loader;
            this.oracle = compiled.oracle();
        }

        /**
         * The metadata's registry is the holder's registry on every call, the holder is the only public registry
         * class, and neither getting the registry nor {@code shapeIds()} and {@code contains} load a partition.
         */
        void checkPrimaryRegistryAndLaziness(List<Candidate> nearMisses) {
            SchemaTypeRegistry fromMetadata = (SchemaTypeRegistry) CompiledSources.invokeStatic(
                loader, compiled.metadataClass(), "typeRegistry");
            registry = (SchemaTypeRegistry) CompiledSources.invokeStatic(loader, compiled.registryClass(), "registry");
            assertSame(registry, fromMetadata, "ServiceMetadata.typeRegistry() is the primary registry");
            assertSame(registry, CompiledSources.invokeStatic(loader, compiled.metadataClass(), "typeRegistry"),
                       "typeRegistry() returns one registry");

            partitionOf = Partitions.of(registry);
            assertEquals(oracle.registered(), registry.shapeIds(), "shapeIds()");
            assertEquals(oracle.registered(), partitionOf.keySet(), "partition declarations");
            Set<Object> partitions = Collections.newSetFromMap(new IdentityHashMap<>());
            partitions.addAll(partitionOf.values());
            for (Object partition : partitions) {
                Set<ShapeId> declared = Partitions.declared(partition);
                assertTrue(declared.size() <= 200, "a partition declares " + declared.size() + " types");
                for (ShapeId id : declared) {
                    assertSame(partition, partitionOf.get(id), "partition of " + id);
                }
            }
            int expectedPartitions = oracle.registered().isEmpty() ? 0 : Math.max(1, compiled.partitionClasses());
            assertEquals(expectedPartitions, partitions.size(), "partitions of the registry");

            for (ShapeId id : oracle.registered()) {
                assertTrue(registry.contains(id), "contains(" + id + ")");
            }
            for (Candidate candidate : nearMisses) {
                assertEquals(oracle.registered().contains(candidate.id()), registry.contains(candidate.id()),
                             "contains(" + candidate.id() + ") of a " + candidate.kind());
            }
            assertEquals(oracle.registered(), registry.shapeIds(), "shapeIds()");
            assertLoadedPartitions("after shapeIds() and contains");

            // Class loading: only the metadata and registry classes; no model type and not the schema holder.
            String registryName = compiled.registryClass();
            assertTrue(loader.defined().contains(registryName), "registry class not loaded: " + loader.defined());
            List<String> unexpected = loader.defined().stream()
                                            .filter(n -> !n.equals(compiled.metadataClass()))
                                            .filter(n -> !n.equals(registryName) && !n.equals(registryName + "$Holder")
                                                         && !n.matches(Pattern.quote(registryName)
                                                                       + "[0-9]+"))
                                            .toList();
            assertEquals(List.of(), unexpected, "classes loaded by getting the registry, shapeIds() and contains");

            // The holder is the only public registry class.
            for (String name : loader.defined()) {
                if (name.startsWith(registryName)) {
                    Class<?> type = load(name);
                    boolean holder = name.equals(registryName);
                    assertEquals(holder, Modifier.isPublic(type.getModifiers()), "public modifier of " + name);
                }
            }
        }

        /** Runs {@code queries} in order, checking each against the oracle; returns one outcome per query. */
        List<String> execute(List<Query> queries) {
            List<String> outcomes = new ArrayList<>();
            for (Query query : queries) {
                String outcome = execute(query);
                outcomes.add(outcome);
                if (query.op() == Op.ENTRIES) {
                    expectedLoaded.addAll(partitionOf.values());
                } else if (query.op().loadsPartition() && oracle.registered().contains(query.id())) {
                    expectedLoaded.add(partitionOf.get(query.id()));
                }
                assertLoadedPartitions("after " + query);
            }
            return outcomes;
        }

        private String execute(Query query) {
            ShapeId id = query.id();
            boolean registered = oracle.registered().contains(id);
            String where = query + " (" + query.kind() + ")";
            switch (query.op()) {
                case CONTAINS -> {
                    assertEquals(registered, registry.contains(id), where);
                    return "contains " + registered;
                }
                case ENTRY -> {
                    Optional<RegisteredType<?>> entry = registry.entry(id);
                    assertEquals(registered, entry.isPresent(), where);
                    if (!registered) {
                        return "no entry";
                    }
                    checkIdentity(entry.get(), where);
                    return "entry " + describe(entry.get());
                }
                case SCHEMA -> {
                    Optional<Schema> schema = registry.schema(id);
                    assertEquals(registered, schema.isPresent(), where);
                    if (!registered) {
                        return "no schema";
                    }
                    assertSame(exported(id), schema.get(), where);
                    return "schema " + schema.get().id();
                }
                case SHAPE_CLASS -> {
                    Class<?> type = registry.getShapeClass(id);
                    if (!registered) {
                        assertNull(type, where);
                        return "no class";
                    }
                    assertSame(modelClass(id), type, where);
                    return "class " + type.getName();
                }
                case CREATE_BUILDER -> {
                    ShapeBuilder<?> builder = registry.createBuilder(id);
                    if (!registered) {
                        assertNull(builder, where);
                        return "no builder";
                    }
                    assertSame(exported(id), builder.schema(), where);
                    assertNotSame(builder, registry.createBuilder(id), where + " returns a new builder");
                    return "builder of " + builder.schema().id();
                }
                case DESERIALIZE -> {
                    if (!registered) {
                        Document document = new Discriminated(id);
                        for (boolean strict : List.of(false, true)) {
                            try {
                                Object value = strict ? registry.deserializeStrict(document)
                                                      : registry.deserialize(document);
                                throw new AssertionError(where + " returned " + value);
                            } catch (UnknownDiscriminatorException e) {
                                assertEquals(id, e.shapeId(), where);
                            }
                        }
                        return "unknown discriminator " + id;
                    }
                    Class<?> type = modelClass(id);
                    SerializableStruct value = build(directBuilder(id), values(exported(id),
                                                                             new Random(query.valueSeed())));
                    Document document = SchemaDocuments.of(value);
                    assertEquals(id, document.discriminator(), where);
                    for (boolean strict : List.of(false, true)) {
                        SerializableStruct result = strict ? registry.deserializeStrict(document)
                                                           : registry.deserialize(document);
                        assertSame(type, result.getClass(), where + (strict ? " strict" : ""));
                        assertSameValue(value, result, where + (strict ? " strict" : ""));
                    }
                    return "deserialized " + type.getName();
                }
                case SHAPE_IDS -> {
                    assertEquals(oracle.registered(), registry.shapeIds(), where);
                    return "shape IDs " + registry.shapeIds();
                }
                case ENTRIES -> {
                    List<RegisteredType<?>> entries = registry.entries();
                    assertEquals(List.copyOf(oracle.registered()),
                                 entries.stream().map(RegisteredType::shapeId).toList(), where);
                    entries.forEach(entry -> checkIdentity(entry, where));
                    return "entries " + entries.stream().map(Run::describe).toList();
                }
                default -> throw new AssertionError(query.op());
            }
        }

        /** An entry is the one entry of its ID, with the exported schema and the model class. */
        private void checkIdentity(RegisteredType<?> entry, String where) {
            ShapeId id = entry.shapeId();
            RegisteredType<?> previous = seen.putIfAbsent(id, entry);
            if (previous != null) {
                assertSame(previous, entry, where + ": a second lookup returns another entry object");
            }
            assertSame(exported(id), entry.schema(), where + ": entry schema");
            assertSame(modelClass(id), entry.type(), where + ": entry class");
        }

        /**
         * Every expected ID resolves to its exact entry, schema, class and a functioning builder factory; loads every
         * partition.
         */
        void sweep(Random random) {
            List<ShapeId> indexed = index().types().stream()
                                           .filter(s -> s.type() == ShapeType.STRUCTURE || s.type() == ShapeType.UNION)
                                           .map(Schema::id).sorted().toList();
            assertEquals(List.copyOf(oracle.registered()), indexed, "structures and unions of the schema index");
            for (ShapeId id : oracle.registered()) {
                String where = "entry of " + id;
                RegisteredType<?> entry = registry.entry(id).orElseThrow(() -> new AssertionError("no " + where));
                assertEquals(id, entry.shapeId(), where);
                checkIdentity(entry, where);
                assertSame(entry, registry.entry(id).orElseThrow(), where + " is stable");
                Schema schema = exported(id);
                assertSame(schema, registry.schema(id).orElseThrow(), where + ": schema(id)");
                assertSame(entry.type(), registry.getShapeClass(id), where + ": getShapeClass(id)");

                ShapeBuilder<?> first = entry.createBuilder();
                ShapeBuilder<?> second = entry.createBuilder();
                ShapeBuilder<?> fromFactory = entry.builderFactory().get();
                ShapeBuilder<?> fromRegistry = registry.createBuilder(id);
                List<ShapeBuilder<?>> builders = List.of(first, second, fromFactory, fromRegistry);
                for (int i = 0; i < builders.size(); i++) {
                    assertSame(schema, builders.get(i).schema(), where + ": builder schema");
                    for (int j = 0; j < i; j++) {
                        assertNotSame(builders.get(j), builders.get(i), where + ": builders are new");
                    }
                }

                Map<Schema, Object> valuesA = values(schema, random);
                Map<Schema, Object> valuesB = values(schema, random);
                SerializableStruct directA = build(directBuilder(id), valuesA);
                SerializableStruct directB = build(directBuilder(id), valuesB);
                // Fill two factory builders before building either: they must not share state.
                fill(first, valuesA);
                fill(second, valuesB);
                SerializableStruct builtA = (SerializableStruct) first.build();
                SerializableStruct builtB = (SerializableStruct) second.build();
                assertSame(entry.type(), builtA.getClass(), where + ": built class");
                assertSameValue(directA, builtA, where + ": factory builder");
                assertSameValue(directB, builtB, where + ": second factory builder");
                assertSameValue(directA, build(fromFactory, valuesA), where + ": builderFactory().get()");
                assertSameValue(directB, build(fromRegistry, valuesB), where + ": registry createBuilder");

                Document document = SchemaDocuments.of(directA);
                assertEquals(id, document.discriminator(), where + ": discriminator");
                SerializableStruct lenient = registry.deserialize(document);
                SerializableStruct strict = registry.deserializeStrict(document);
                assertSame(entry.type(), lenient.getClass(), where + ": deserialize class");
                assertSame(entry.type(), strict.getClass(), where + ": deserializeStrict class");
                assertSameValue(directA, lenient, where + ": deserialize");
                assertSameValue(directA, strict, where + ": deserializeStrict");
            }
            List<RegisteredType<?>> entries = registry.entries();
            assertEquals(oracle.registered().size(), entries.size(), "entries()");
            int i = 0;
            for (ShapeId id : oracle.registered()) {
                assertSame(registry.entry(id).orElseThrow(), entries.get(i++), "entries() element " + id);
            }
            expectedLoaded.addAll(partitionOf.values());
            assertLoadedPartitions("after the sweep");
        }

        private void assertLoadedPartitions(String when) {
            SortedSet<String> expected = new TreeSet<>();
            SortedSet<String> actual = new TreeSet<>();
            Set<Object> partitions = Collections.newSetFromMap(new IdentityHashMap<>());
            partitions.addAll(partitionOf.values());
            for (Object partition : partitions) {
                String name = "partition from " + new TreeSet<>(Partitions.declared(partition)).first();
                if (expectedLoaded.contains(partition)) {
                    expected.add(name);
                }
                if (Partitions.loaded(partition)) {
                    actual.add(name);
                }
            }
            assertEquals(expected, actual, "loaded partitions " + when);
        }

        // -----------------------------------------------------------------------------------------------------------
        // Generated metadata

        private ServiceSchemaIndex index() {
            if (index == null) {
                index = (ServiceSchemaIndex) CompiledSources.invokeStatic(loader, compiled.metadataClass(),
                                                                         "schemaIndex");
            }
            return index;
        }

        /** The exported schema of {@code id}: the schema index's, which is also the model class's {@code SCHEMA}. */
        private Schema exported(ShapeId id) {
            Schema schema = index().schema(id).orElseThrow(() -> new AssertionError("no indexed schema " + id));
            assertSame(schema, CompiledSources.staticField(loader, modelClass(id).getName(), "SCHEMA"),
                       "SCHEMA of " + modelClass(id).getName() + " is the indexed schema");
            return schema;
        }

        /** The model class whose {@code ID} is {@code id}; exactly one per registered ID. */
        private Class<?> modelClass(ShapeId id) {
            if (modelClasses == null) {
                Map<ShapeId, Class<?>> classes = new TreeMap<>();
                Path directory = compiled.classes().resolve(compiled.modelPackage().replace('.', '/'));
                try (Stream<Path> files = Files.list(directory)) {
                    for (String file : files.map(p -> p.getFileName().toString())
                                            .filter(n -> n.endsWith(".class") && !n.contains("$")).sorted().toList()) {
                        Class<?> type = load(compiled.modelPackage() + "." + file.substring(0, file.length() - 6));
                        Field field = declaredField(type, "ID");
                        if (field != null) {
                            Class<?> previous = classes.put((ShapeId) get(field), type);
                            assertNull(previous, "two model classes for " + get(field));
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                assertTrue(classes.keySet().containsAll(oracle.registered()),
                           "model classes " + classes.keySet() + " cover " + oracle.registered());
                modelClasses = classes;
            }
            return Objects.requireNonNull(modelClasses.get(id), () -> "no model class for " + id);
        }

        /** A new builder from the model class's own static {@code builder()}, not through the registry. */
        private ShapeBuilder<?> directBuilder(ShapeId id) {
            return (ShapeBuilder<?>) CompiledSources.invokeStatic(loader, modelClass(id).getName(), "builder");
        }

        private Class<?> load(String name) {
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);
            }
        }

        // -----------------------------------------------------------------------------------------------------------
        // Generated values

        /** Generated member values of the aggregate {@code schema}: required members always, one member of a union. */
        private Map<Schema, Object> values(Schema schema, Random random) {
            return members(schema, random, 0);
        }

        private Map<Schema, Object> members(Schema aggregate, Random random, int depth) {
            List<Schema> members = aggregate.members().stream()
                                            .filter(m -> !m.hasTrait(TraitKey.STREAMING_TRAIT)).toList();
            Map<Schema, Object> result = new LinkedHashMap<>();
            if (aggregate.type() == ShapeType.UNION) {
                if (members.isEmpty()) {
                    return result;
                }
                List<Schema> choices = members;
                if (depth >= MAX_DEPTH) {
                    // Deep: prefer the least recursive members, simple ones first.
                    int best = members.stream().mapToInt(m -> recursionRank(m.memberTarget().type())).min()
                                      .orElseThrow();
                    choices = members.stream().filter(m -> recursionRank(m.memberTarget().type()) == best).toList();
                }
                Schema member = choices.get(random.nextInt(choices.size()));
                result.put(member, value(member, random, depth + 1));
                return result;
            }
            for (Schema member : members) {
                if (member.hasTrait(TraitKey.REQUIRED_TRAIT) || depth < MAX_DEPTH && random.nextInt(3) != 0) {
                    result.put(member, value(member, random, depth + 1));
                }
            }
            return result;
        }

        private static int recursionRank(ShapeType type) {
            return switch (type) {
                case LIST, SET, MAP -> 1;
                case STRUCTURE -> 2;
                case UNION -> 3;
                default -> 0;
            };
        }

        private Object value(Schema member, Random random, int depth) {
            if (depth > 30) {
                throw new AssertionError("unbounded value recursion at " + member.id());
            }
            Schema target = member.isMember() ? member.memberTarget() : member;
            boolean deep = depth >= MAX_DEPTH;
            return switch (target.type()) {
                case BOOLEAN -> random.nextBoolean();
                case BYTE -> (byte) random.nextInt();
                case SHORT -> (short) random.nextInt();
                case INTEGER -> random.nextInt();
                case LONG -> random.nextLong();
                case FLOAT -> List.of(random.nextFloat() * 1000 - 500, -0.0f, Float.MAX_VALUE, 1.5f)
                                  .get(random.nextInt(4));
                case DOUBLE -> List.of(random.nextDouble() * 1e6 - 5e5, -0.0d, Double.MIN_VALUE, 2.25d)
                                   .get(random.nextInt(4));
                case BIG_INTEGER -> new BigInteger(80, random).subtract(BigInteger.ONE.shiftLeft(79));
                case BIG_DECIMAL -> new BigDecimal(new BigInteger(64, random).subtract(BigInteger.ONE.shiftLeft(63)),
                                                   random.nextInt(8));
                case STRING -> string(random);
                case ENUM -> random.nextInt(4) == 0 ? "unknown" + random.nextInt(100)
                                                    : pick(List.copyOf(new TreeSet<>(target.stringEnumValues())),
                                                           random);
                case INT_ENUM -> random.nextInt(4) == 0 ? random.nextInt()
                                                        : pick(List.copyOf(new TreeSet<>(target.intEnumValues())),
                                                               random);
                case BLOB -> {
                    byte[] bytes = new byte[random.nextInt(9)];
                    random.nextBytes(bytes);
                    yield SdkBytes.fromByteArray(bytes);
                }
                case TIMESTAMP -> Instant.ofEpochMilli((long) (random.nextDouble() * 4_102_444_800_000L));
                case DOCUMENT -> document(random, deep ? 0 : 2);
                case LIST, SET -> {
                    List<Object> list = new ArrayList<>();
                    int size = deep ? 0 : random.nextInt(4);
                    boolean sparse = target.hasTrait(TraitKey.SPARSE_TRAIT);
                    boolean unique = target.hasTrait(TraitKey.UNIQUE_ITEMS_TRAIT);
                    for (int i = 0; i < size; i++) {
                        Object element = sparse && random.nextInt(5) == 0
                                         ? null : value(target.listMember(), random, depth + 1);
                        if (!unique || !list.contains(element)) {
                            list.add(element);
                        }
                    }
                    yield list;
                }
                case MAP -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    int size = deep ? 0 : random.nextInt(4);
                    boolean sparse = target.hasTrait(TraitKey.SPARSE_TRAIT);
                    for (int i = 0; i < size; i++) {
                        String key = (String) value(target.mapKeyMember(), random, depth + 1);
                        map.put(key, sparse && random.nextInt(5) == 0
                                     ? null : value(target.mapValueMember(), random, depth + 1));
                    }
                    yield map;
                }
                case STRUCTURE, UNION -> target.id().equals(Unit.ID)
                                         ? Unit.getInstance()
                                         : build(directBuilder(target.id()), members(target, random, depth));
                default -> throw new AssertionError("Unsupported member " + member.id() + " of type " + target.type());
            };
        }

        private static String string(Random random) {
            String alphabet = "abcXYZ019 -_\u00e9\u2603\u4e2d";
            StringBuilder out = new StringBuilder();
            for (int i = random.nextInt(8); i > 0; i--) {
                out.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            return out.toString();
        }

        private static Document document(Random random, int depth) {
            int kind = random.nextInt(depth > 0 ? 6 : 4);
            return switch (kind) {
                case 0 -> Document.of(string(random));
                case 1 -> Document.of(random.nextBoolean());
                case 2 -> Document.of(random.nextLong());
                case 3 -> Document.of(BigDecimal.valueOf(random.nextInt(100_000), 2));
                case 4 -> {
                    List<Document> list = new ArrayList<>();
                    for (int i = random.nextInt(3); i > 0; i--) {
                        list.add(document(random, depth - 1));
                    }
                    yield Document.of(list);
                }
                default -> {
                    Map<String, Document> map = new LinkedHashMap<>();
                    for (int i = random.nextInt(3); i > 0; i--) {
                        map.put(string(random), document(random, depth - 1));
                    }
                    yield Document.of(map);
                }
            };
        }

        @Override
        public void close() throws IOException {
            loader.close();
        }

        private static String describe(RegisteredType<?> entry) {
            return entry.shapeId() + " -> " + entry.type().getName() + " (schema " + entry.schema().id() + ")";
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    /** Reads {@code SchemaTypeRegistry}'s private partition state; it has no public view of unloaded partitions. */
    private static final class Partitions {
        private static final Field PARTITIONS = requiredField(SchemaTypeRegistry.class, "partitions");

        /** Each registered ID's partition object. */
        static Map<ShapeId, Object> of(SchemaTypeRegistry registry) {
            Map<?, ?> partitions = (Map<?, ?>) get(PARTITIONS, registry);
            Map<ShapeId, Object> result = new TreeMap<>();
            partitions.forEach((id, partition) -> result.put((ShapeId) id, partition));
            return result;
        }

        static boolean loaded(Object partition) {
            return get(requiredField(partition.getClass(), "entries"), partition) != null;
        }

        @SuppressWarnings("unchecked")
        static Set<ShapeId> declared(Object partition) {
            return (Set<ShapeId>) get(requiredField(partition.getClass(), "declared"), partition);
        }

        private static Field requiredField(Class<?> type, String name) {
            return Objects.requireNonNull(declaredField(type, name),
                                          () -> type.getName() + " has no field " + name + "; update this test's "
                                                + "partition-state probe");
        }
    }

    /** A document whose only content is a discriminator, for near-miss IDs. */
    private record Discriminated(ShapeId id) implements Document {
        @Override
        public ShapeType type() {
            return ShapeType.STRUCTURE;
        }

        @Override
        public ShapeId discriminator() {
            return id;
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void fill(ShapeBuilder<?> builder, Map<Schema, Object> values) {
        values.forEach(((ShapeBuilder) builder)::setMemberValue);
    }

    private static SerializableStruct build(ShapeBuilder<?> builder, Map<Schema, Object> values) {
        fill(builder, values);
        return (SerializableStruct) builder.build();
    }

    /**
     * {@code actual} is a value of the class of {@code expected} with the same state: {@code equals} and
     * {@code hashCode} for ordinary types, every member through {@code getMemberValue} for errors (exceptions, whose
     * equality is identity).
     */
    private static void assertSameValue(SerializableStruct expected, SerializableStruct actual, String where) {
        assertSame(expected.getClass(), actual.getClass(), where + ": class");
        if (expected instanceof Throwable) {
            for (Schema member : expected.schema().members()) {
                assertEquals((Object) expected.getMemberValue(member), actual.getMemberValue(member),
                             where + ": member " + member.memberName());
            }
            assertFalse(expected == actual, where + ": a new error value");
            return;
        }
        assertEquals(expected, actual, where);
        assertEquals(expected.hashCode(), actual.hashCode(), where + ": hashCode");
    }

    private static <T> T pick(List<T> values, Random random) {
        return values.get(random.nextInt(values.size()));
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

    private static Object get(Field field) {
        return get(field, null);
    }

    private static Object get(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
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
}
