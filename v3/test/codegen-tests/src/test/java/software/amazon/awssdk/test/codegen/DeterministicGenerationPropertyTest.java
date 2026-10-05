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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenContext;
import software.amazon.awssdk.awscodegen.AwsCodegenIntegration;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.test.codegen.support.GeneratedTree;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels.ModelSpec;
import software.amazon.awssdk.test.codegen.support.TestIntegration;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Property 5 (Requirement 3.6): for any valid model, generator configuration, generator version, JDK and equivalent
 * output roots, repeated generation produces the same relative paths and byte-identical contents.
 *
 * <p>Each try draws a random model ({@link RandomSmithyModels}), a random rendering of it as model files, a random set
 * of compatible, interacting integrations, and a random generator configuration (pinned or unpinned model revision),
 * then generates three times in one JVM (so the generator version and JDK are fixed):
 * <ol>
 *   <li><b>A</b> and <b>B</b> generate from <em>identical model inputs</em> (the same relative file paths and bytes),
 *       which is exactly the premise of Requirement 3.6, but with independently random model-file creation order
 *       (and so directory listing order), integration enumeration order, and equivalent model-directory and output
 *       roots: plain, deeply nested, containing spaces and non-ASCII characters, behind a symbolic link, relative to
 *       the working directory, non-normalized ({@code x/..} and trailing {@code .}), pre-populated with stale output
 *       and stale staging directories, or the very roots the previous run published to. Their source trees, resource
 *       trees, file lists, model digests and revisions must be byte-identical with no normalization.</li>
 *   <li><b>C</b> generates from an <em>independent equivalent rendering</em> of the same model: different file
 *       count, names, nesting and IDL/JSON AST encodings, shape-to-file assignment, declaration order, inline versus
 *       {@code apply} trait placement, and order of set-valued properties. This is stronger than Requirement 3.6
 *       (the inputs are not identical), so only what is by definition a function of the input <em>bytes</em> is
 *       normalized, and each normalized value is first checked for self-consistency: the model digest (in
 *       {@code GeneratedServiceInfo}, the provenance, and the revision when unpinned) is replaced by a placeholder;
 *       the provenance's per-file list is checked to be exactly the rendering's paths and SHA-256 digests and then
 *       dropped; and the generation manifest's file digests are checked against the run's own published bytes and
 *       then dropped (every file they digest is itself compared). Everything else, including every generated source,
 *       every integration output, the manifest configuration and the semantic inventory, must be byte-identical.</li>
 * </ol>
 *
 * <p>The integrations interact: documenters add documentation traits in {@code preprocessModel}, one integration may
 * add an operation to the service, renamers move operation descriptors to other symbols in
 * {@code decorateSymbolProvider}, and writers emit a resource-like report and a Java file (through the file manifest
 * and the writer delegator) whose content depends on the other integrations' model and symbol changes. Targets are
 * disjoint so no configuration is a conflict (conflicts are Property 6).
 *
 * <p>Each try runs three complete generations (about 25-60 ms each once warm), so the property runs the design's
 * minimum of 100 tries; models are kept small (at most 6 operations, 10 data shapes, 3 resources, 3 errors) because
 * determinism failures come from ordering and location, not size.
 */
class DeterministicGenerationPropertyTest {
    private static final String DIGEST_PLACEHOLDER = "<model-digest>";
    private static final String PROVENANCE_SUFFIX = "/model-provenance.json";
    private static final String MANIFEST_SUFFIX = "/generation-manifest.json";
    private static final String REPORT_DIRECTORY = "META-INF/aws-sdk-java-v3-test/";
    private static final String ADDED_OPERATION = "InjectedByIntegration";
    private static final List<String> MODEL_LEAVES = List.of("model", "models/smithy", "input model");
    private static final List<String> SOURCE_LEAVES = List.of("out/src", "generated-sources/aws", "src-gen");
    private static final List<String> RESOURCE_LEAVES = List.of("out/resources", "generated-resources/aws", "res-gen");
    private static final List<String> INTEGRATION_IDS = List.of(
        "test.alpha", "test.beta-docs", "vendor.gamma", "test.delta", "zeta.one", "eta");

    // Feature: aws-sdk-java-v3, Property 5: Generation is deterministic
    @Property(tries = 100)
    @Label("Property 5: Generation is deterministic")
    void generationIsDeterministic(@ForAll("scenarios") Scenario scenario) throws IOException {
        Statistics.label("rendering encodings").collect(encodings(scenario.rendering()));
        Statistics.label("B output root").collect(scenario.b().reuse() ? "reused A roots" : scenario.b().outputForm());
        Statistics.label("integrations").collect(scenario.integrations().size());
        Statistics.label("model files (A, B)").collect(scenario.rendering().size());

        Path trial = Files.createTempDirectory("aws-codegen-property5-");
        try {
            Run a = run(trial.resolve("a"), scenario, scenario.rendering(), scenario.a(), null);
            Run b = run(trial.resolve("b"), scenario, scenario.rendering(), scenario.b(), a);
            Run c = run(trial.resolve("c"), scenario, scenario.equivalentRendering(), scenario.c(), b);

            assertNonTrivial(scenario, a);

            // Identical model inputs: no normalization at all.
            GeneratedTree.assertByteIdentical(a.sources(), b.sources());
            GeneratedTree.assertByteIdentical(a.resources(), b.resources());
            assertEquals(a.result().generatedFiles(), b.result().generatedFiles(), "generated source files");
            assertEquals(a.result().generatedResources(), b.result().generatedResources(), "generated resources");
            assertEquals(a.result().modelDigest(), b.result().modelDigest(), "model digest");
            assertEquals(a.result().modelRevision(), b.result().modelRevision(), "model revision");

            // Equivalent rendering: identical modulo the input-byte identity, which is checked before it is removed.
            Normalized left = normalize(a, scenario.rendering());
            Normalized right = normalize(c, scenario.equivalentRendering());
            GeneratedTree.assertByteIdentical(left.sources(), right.sources());
            GeneratedTree.assertByteIdentical(left.resources(), right.resources());
            assertEquals(a.result().generatedFiles(), c.result().generatedFiles(), "generated source files");
            assertEquals(a.result().generatedResources(), c.result().generatedResources(), "generated resources");
        } finally {
            deleteRecursively(trial);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Scenarios

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<ModelSpec> models = Combinators.combine(
            Arbitraries.integers().between(0, 6),
            Arbitraries.integers().between(0, 10),
            Arbitraries.integers().between(0, 3),
            Arbitraries.integers().between(0, 3),
            Arbitraries.of(false, true),
            seeds()
        ).as((operations, data, resources, errors, twoNamespaces, seed) -> RandomSmithyModels.model(
            new Random(seed), operations, data, resources, errors, twoNamespaces));
        return Combinators.combine(
            models,
            seeds(),
            seeds(),
            Arbitraries.integers().between(0, INTEGRATION_IDS.size()).withoutEdgeCases(),
            seeds(),
            seeds(),
            Arbitraries.of(false, true)
        ).as(Scenario::of);
    }

    /**
     * Seeds of the structured choices. Edge-case seeds (0, min, max) would repeat the same model, rendering or layout
     * in many tries; the sizes above keep their edge cases (empty models, no operations, ...).
     */
    private static Arbitrary<Long> seeds() {
        return Arbitraries.longs().withoutEdgeCases();
    }

    /**
     * One try.
     *
     * @param model the semantic model
     * @param rendering model files of runs A and B
     * @param equivalentRendering model files of run C
     * @param integrations integration configuration, in declaration order
     * @param revision pinned model revision, or {@code null} for unpinned generation
     * @param a layout of run A
     * @param b layout of run B
     * @param c layout of run C
     */
    record Scenario(ModelSpec model, Map<String, String> rendering, Map<String, String> equivalentRendering,
                    List<IntegrationSpec> integrations, String revision, Layout a, Layout b, Layout c) {
        static Scenario of(ModelSpec model, long renderingSeed, long equivalentSeed, int integrationCount,
                           long integrationSeed, long layoutSeed, boolean pinned) {
            Map<String, String> rendering = RandomSmithyModels.render(model, new Random(renderingSeed));
            Map<String, String> equivalent = RandomSmithyModels.render(model, new Random(equivalentSeed));
            List<IntegrationSpec> integrations = IntegrationSpec.random(model, integrationCount,
                                                                        new Random(integrationSeed));
            Random layouts = new Random(layoutSeed);
            String revision = pinned ? HexFormat.of().formatHex(bytes(layouts, 20)) : null;
            return new Scenario(model, rendering, equivalent, integrations, revision,
                                Layout.random(layouts, rendering, integrations.size(), false),
                                Layout.random(layouts, rendering, integrations.size(), true),
                                Layout.random(layouts, equivalent, integrations.size(), true));
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder("Scenario[").append(model).append('\n');
            result.append("  revision: ").append(revision).append('\n');
            integrations.forEach(i -> result.append("  ").append(i).append('\n'));
            result.append("  A: ").append(a).append("\n  B: ").append(b).append("\n  C: ").append(c).append('\n');
            describe(result, "rendering (A, B)", rendering);
            describe(result, "equivalent rendering (C)", equivalentRendering);
            return result.append(']').toString();
        }

        private static void describe(StringBuilder result, String title, Map<String, String> files) {
            result.append("  ").append(title).append(":\n");
            files.forEach((path, content) -> result.append("  --- ").append(path).append('\n').append(content));
        }
    }

    /** Forms of an equivalent directory path. */
    enum RootForm {
        PLAIN, DEEP, SPACES_AND_UNICODE, SYMLINKED_PARENT, RELATIVE, UNNORMALIZED
    }

    /**
     * How one run lays out its inputs and outputs.
     *
     * @param modelForm form of the model directory
     * @param outputForm form of the output roots
     * @param placement index into the source and resource root leaves
     * @param stale whether the output roots (and a staging directory) hold stale content before generation
     * @param reuse whether the run publishes to the previous run's output roots instead
     * @param creationOrder order in which the model files are created
     * @param enumeration order in which the integrations are passed to the generator (indexes into the declaration)
     */
    record Layout(RootForm modelForm, RootForm outputForm, int placement, boolean stale, boolean reuse,
                  List<String> creationOrder, List<Integer> enumeration) {
        static Layout random(Random random, Map<String, String> files, int integrations, boolean mayReuse) {
            RootForm[] forms = RootForm.values();
            List<String> order = new ArrayList<>(files.keySet());
            Collections.shuffle(order, random);
            List<Integer> enumeration = new ArrayList<>();
            for (int i = 0; i < integrations; i++) {
                enumeration.add(i);
            }
            Collections.shuffle(enumeration, random);
            return new Layout(forms[random.nextInt(forms.length)], forms[random.nextInt(forms.length)],
                              random.nextInt(SOURCE_LEAVES.size()), random.nextInt(3) == 0,
                              mayReuse && random.nextInt(4) == 0, order, enumeration);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Integrations

    /**
     * A test integration's configuration; {@link #build} creates a fresh {@link TestIntegration} for each run.
     *
     * @param id integration identifier
     * @param order integration order (ties are broken by identifier)
     * @param namespaces the model's namespaces, whose shapes the report describes
     * @param service the service shape
     * @param documents shapes that receive a documentation trait in {@code preprocessModel}
     * @param addsOperation whether {@code preprocessModel} adds an operation to the service
     * @param renames operations whose descriptor symbol is renamed in {@code decorateSymbolProvider}
     * @param report whether {@code customize} writes a report through the file manifest
     * @param java whether {@code customize} writes a Java file through the writer delegator
     */
    record IntegrationSpec(String id, int order, Set<String> namespaces, ShapeId service, List<ShapeId> documents,
                           boolean addsOperation, List<ShapeId> renames, boolean report, boolean java) {
        static List<IntegrationSpec> random(ModelSpec model, int count, Random random) {
            List<String> ids = new ArrayList<>(INTEGRATION_IDS);
            Collections.shuffle(ids, random);
            List<ShapeId> documentable = new ArrayList<>();
            model.shapes().stream().map(RandomSmithyModels.Shape::id).filter(id -> !id.equals(model.service()))
                 .sorted().forEach(documentable::add);
            Collections.shuffle(documentable, random);
            List<ShapeId> renamable = new ArrayList<>(model.ofType("operation"));
            Collections.shuffle(renamable, random);
            boolean adderAvailable = !model.lowerCaseNames().contains(ADDED_OPERATION.toLowerCase(Locale.ROOT));
            List<IntegrationSpec> result = new ArrayList<>();
            for (String id : ids.subList(0, count)) {
                List<ShapeId> documents = take(documentable, random.nextInt(3));
                List<ShapeId> renames = take(renamable, random.nextInt(2));
                boolean adds = adderAvailable && random.nextInt(4) == 0;
                adderAvailable &= !adds;
                result.add(new IntegrationSpec(id, List.of(-1, 0, 0, 1).get(random.nextInt(4)), model.namespaces(),
                                               model.service(), documents, adds, renames, random.nextBoolean(),
                                               random.nextInt(5) < 2));
            }
            return result;
        }

        private static List<ShapeId> take(List<ShapeId> pool, int count) {
            List<ShapeId> taken = new ArrayList<>();
            while (taken.size() < count && !pool.isEmpty()) {
                taken.add(pool.remove(pool.size() - 1));
            }
            return taken;
        }

        TestIntegration build() {
            return TestIntegration.named(id).withOrder(order).withPreprocess(this::preprocess)
                                  .withSymbolDecorator(this::decorate).withCustomize(this::customize);
        }

        private Model preprocess(Model model) {
            List<Shape> replacements = new ArrayList<>();
            for (ShapeId target : documents) {
                replacements.add(withTrait(model.expectShape(target), new DocumentationTrait("Documented by " + id
                                                                                              + ".")));
            }
            if (addsOperation) {
                OperationShape added = OperationShape.builder()
                                                     .id(ShapeId.fromParts(service.getNamespace(), ADDED_OPERATION))
                                                     .addTrait(new DocumentationTrait("Added by " + id + "."))
                                                     .build();
                replacements.add(added);
                replacements.add(model.expectShape(service, ServiceShape.class).toBuilder()
                                      .addOperation(added.getId()).build());
            }
            return replacements.isEmpty() ? model : ModelTransformer.create().replaceShapes(model, replacements);
        }

        private SymbolProvider decorate(SymbolProvider provider) {
            SymbolProvider result = provider;
            String tag = javaName(id);
            for (ShapeId target : renames) {
                result = TestIntegration.mapSymbol(target, (shape, symbol) -> renamed(symbol, tag)).apply(result);
            }
            return result;
        }

        private static Symbol renamed(Symbol symbol, String tag) {
            String name = symbol.getName().replaceFirst("Operation$", "") + "Via" + tag + "Descriptor";
            String file = symbol.getDefinitionFile();
            String directory = file.substring(0, file.lastIndexOf('/') + 1);
            return Symbol.builder().namespace(symbol.getNamespace(), ".").name(name)
                         .definitionFile(directory + name + ".java").build();
        }

        private void customize(AwsCodegenContext context) {
            if (report) {
                context.fileManifest().writeFile(REPORT_DIRECTORY + id + ".txt", report(context));
            }
            if (java) {
                String className = "Report" + javaName(id);
                String packageName = context.settings().packageName() + ".custom";
                List<OperationShape> operations = operations(context);
                context.writerDelegator().useFileWriter(
                    packageName.replace('.', '/') + "/" + className + ".java", packageName, writer -> {
                        writer.write("/** Written by $L after $L. */", id, integrationIds(context));
                        writer.openBlock("final class $L {", "}", className, () -> operations.forEach(
                            operation -> writer.write("// $L -> $T", operation.getId().toString(),
                                                      context.symbolProvider().toSymbol(operation))));
                    });
            }
        }

        /** The model and symbols as this integration sees them, in a defined (sorted) order. */
        private String report(AwsCodegenContext context) {
            StringBuilder out = new StringBuilder("integration ").append(id).append('\n');
            out.append("integrations ").append(integrationIds(context)).append('\n');
            context.model().shapes()
                   .filter(shape -> namespaces.contains(shape.getId().getNamespace()))
                   .sorted(Comparator.comparing(Shape::getId))
                   .forEach(shape -> {
                       out.append("shape ").append(shape.getId()).append(' ').append(shape.getType()).append('\n');
                       shape.getAllTraits().values().stream()
                            .filter(t -> t instanceof DocumentationTrait
                                         || RandomSmithyModels.UNKNOWN_TRAITS.contains(t.toShapeId().toString()))
                            .sorted(Comparator.comparing(Trait::toShapeId))
                            .forEach(t -> out.append("  @").append(t.toShapeId()).append(' ')
                                             .append(Node.printJson(t.toNode())).append('\n'));
                   });
            for (OperationShape operation : operations(context)) {
                Symbol symbol = context.symbolProvider().toSymbol(operation);
                out.append("descriptor ").append(operation.getId()).append(' ').append(symbol.getFullName())
                   .append(' ').append(symbol.getDefinitionFile()).append('\n');
            }
            return out.toString();
        }

        private static List<OperationShape> operations(AwsCodegenContext context) {
            return TopDownIndex.of(context.model()).getContainedOperations(context.service()).stream().sorted()
                               .toList();
        }

        private static String integrationIds(AwsCodegenContext context) {
            return context.integrations().stream().map(AwsCodegenIntegration::identifier).toList().toString();
        }

        private static String javaName(String id) {
            StringBuilder result = new StringBuilder();
            for (String part : id.split("[.-]")) {
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
            return result.toString();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Shape withTrait(Shape shape, Trait trait) {
            AbstractShapeBuilder builder = Shape.shapeToBuilder(shape);
            return (Shape) builder.addTrait(trait).build();
        }

        @Override
        public String toString() {
            return "Integration[" + id + ", order " + order + ", documents " + documents + ", adds operation "
                   + addsOperation + ", renames " + renames + ", report " + report + ", java " + java + "]";
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Runs

    /**
     * One generation and snapshots of its published trees, taken right after it (a later run may reuse the roots).
     */
    record Run(AwsCodegenResult result, Path sourceRoot, Path resourceRoot, GeneratedTree sources,
               GeneratedTree resources) {
    }

    private static Run run(Path directory, Scenario scenario, Map<String, String> files, Layout layout, Run previous)
        throws IOException {
        Files.createDirectories(directory);
        Path models = place(directory, "model", MODEL_LEAVES.get(layout.placement()), layout.modelForm());
        for (String file : layout.creationOrder()) {
            Path target = models.resolve(file);
            Files.createDirectories(target.getParent());
            Files.writeString(target, files.get(file), StandardCharsets.UTF_8);
        }
        Path sources;
        Path resources;
        if (layout.reuse() && previous != null) {
            sources = previous.sourceRoot();
            resources = previous.resourceRoot();
        } else {
            sources = place(directory, "out", SOURCE_LEAVES.get(layout.placement()), layout.outputForm());
            resources = place(directory, "out", RESOURCE_LEAVES.get(layout.placement()), layout.outputForm());
        }
        if (layout.stale()) {
            writeStale(sources, resources);
        }
        List<AwsCodegenIntegration> integrations = new ArrayList<>();
        layout.enumeration().forEach(i -> integrations.add(scenario.integrations().get(i).build()));
        AwsCodegenRequest request = new AwsCodegenRequest(scenario.model().service().toString(),
                                                          scenario.model().packageName(), models, sources, resources,
                                                          scenario.revision());
        AwsCodegenResult result = AwsCodegen.generate(request, integrations);
        return new Run(result, sources, resources, GeneratedTree.read(result.sourceRoot()),
                       GeneratedTree.read(result.resourceRoot()));
    }

    /** {@code leaf} under {@code directory}, reached through a path of the given form. */
    private static Path place(Path directory, String tag, String leaf, RootForm form) throws IOException {
        switch (form) {
            case DEEP:
                return directory.resolve("deep/er/and/deeper").resolve(leaf);
            case SPACES_AND_UNICODE:
                return directory.resolve("dir with spaces/\u00fcn\u00efc\u00f8d\u00e9").resolve(leaf);
            case SYMLINKED_PARENT:
                Path real = Files.createDirectories(directory.resolve("real-" + tag));
                Path link = directory.resolve("link-" + tag);
                if (!Files.exists(link, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                    try {
                        Files.createSymbolicLink(link, real);
                    } catch (UnsupportedOperationException | IOException e) {
                        return directory.resolve(leaf);
                    }
                }
                return link.resolve(leaf);
            case RELATIVE:
                Path workingDirectory = Path.of("").toAbsolutePath();
                if (!workingDirectory.equals(workingDirectory.toRealPath())) {
                    // Lexical ".." normalization would differ from the file system's through a symbolic link.
                    return directory.resolve(leaf);
                }
                try {
                    return workingDirectory.relativize(directory.toAbsolutePath().resolve(leaf));
                } catch (IllegalArgumentException e) {
                    return directory.resolve(leaf);
                }
            case UNNORMALIZED:
                Files.createDirectories(directory.resolve("x-" + tag));
                return directory.resolve("x-" + tag).resolve("..").resolve(leaf).resolve(".");
            default:
                return directory.resolve(leaf);
        }
    }

    private static void writeStale(Path sources, Path resources) throws IOException {
        Path source = sources.toAbsolutePath().normalize();
        Path resource = resources.toAbsolutePath().normalize();
        Files.createDirectories(source.resolve("software/stale"));
        Files.writeString(source.resolve("software/stale/Stale.java"), "final class Stale {}\n");
        Files.createDirectories(resource.resolve("META-INF"));
        Files.writeString(resource.resolve("META-INF/stale.txt"), "stale\n");
        Path staging = source.resolveSibling(source.getFileName() + ".staging");
        Files.createDirectories(staging);
        Files.writeString(staging.resolve("Leftover.java"), "final class Leftover {}\n");
    }

    private static void assertNonTrivial(Scenario scenario, Run run) {
        String packagePath = scenario.model().packageName().replace('.', '/');
        assertTrue(run.sources().paths().contains(packagePath + "/" + AwsCodegen.SERVICE_INFO_TYPE + ".java"),
                   run.sources().paths().toString());
        assertTrue(run.resources().paths().stream().anyMatch(p -> p.endsWith(PROVENANCE_SUFFIX)),
                   run.resources().paths().toString());
        assertTrue(run.resources().paths().stream().anyMatch(p -> p.endsWith(MANIFEST_SUFFIX)),
                   run.resources().paths().toString());
        assertTrue(run.sources().paths().stream().noneMatch(p -> p.contains("stale") || p.contains("Leftover")),
                   run.sources().paths().toString());
        for (IntegrationSpec integration : scenario.integrations()) {
            if (integration.report()) {
                assertTrue(run.sources().paths().contains(REPORT_DIRECTORY + integration.id() + ".txt"),
                           run.sources().paths().toString());
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Input-identity normalization (run C only)

    record Normalized(GeneratedTree sources, GeneratedTree resources) {
    }

    /**
     * {@code run}'s trees without the identity of its input bytes, after checking that identity: see the class
     * documentation.
     */
    private static Normalized normalize(Run run, Map<String, String> files) {
        String digest = run.result().modelDigest();
        SortedMap<String, byte[]> sources = new TreeMap<>();
        for (String path : run.sources().paths()) {
            sources.put(path, run.sources().text(path).replace(digest, DIGEST_PLACEHOLDER)
                                 .getBytes(StandardCharsets.UTF_8));
        }
        SortedMap<String, byte[]> resources = new TreeMap<>();
        for (String path : run.resources().paths()) {
            String text = run.resources().text(path);
            if (path.endsWith(PROVENANCE_SUFFIX)) {
                text = withoutModelFiles(text, files);
            } else if (path.endsWith(MANIFEST_SUFFIX)) {
                text = withoutFileDigests(text, run);
            }
            resources.put(path, text.replace(digest, DIGEST_PLACEHOLDER).getBytes(StandardCharsets.UTF_8));
        }
        return new Normalized(GeneratedTree.of(sources), GeneratedTree.of(resources));
    }

    /** Checks the provenance lists exactly the model files with their digests, then removes that list. */
    private static String withoutModelFiles(String provenance, Map<String, String> files) {
        ObjectNode root = Node.parse(provenance).expectObjectNode();
        ObjectNode model = root.expectObjectMember("model");
        List<String> expected = new ArrayList<>();
        new TreeMap<>(files).forEach((path, content) -> expected.add(
            path + " " + sha256(content.getBytes(StandardCharsets.UTF_8))));
        List<String> actual = new ArrayList<>();
        model.expectArrayMember("files").getElements().forEach(element -> {
            ObjectNode file = element.expectObjectNode();
            actual.add(file.expectStringMember("path").getValue() + " "
                       + file.expectStringMember("digest").getValue());
        });
        assertEquals(expected, actual, "provenance model files");
        return Node.prettyPrintJson(root.withMember("model", model.withoutMember("files")));
    }

    /** Checks every manifest file digest against the run's published bytes, then removes the digests. */
    private static String withoutFileDigests(String manifest, Run run) {
        ObjectNode root = Node.parse(manifest).expectObjectNode();
        List<Node> files = new ArrayList<>();
        for (Node element : root.expectArrayMember("files").getElements()) {
            ObjectNode file = element.expectObjectNode();
            String path = file.expectStringMember("path").getValue();
            GeneratedTree tree = file.expectStringMember("root").getValue().equals("sources") ? run.sources()
                                                                                             : run.resources();
            assertEquals(sha256(tree.content(path)), file.expectStringMember("digest").getValue(),
                         "manifest digest of " + path);
            files.add(file.withoutMember("digest"));
        }
        return Node.prettyPrintJson(root.withMember("files", ArrayNode.fromNodes(files)));
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static String encodings(Map<String, String> files) {
        boolean json = files.keySet().stream().anyMatch(p -> p.endsWith(".json"));
        boolean idl = files.keySet().stream().anyMatch(p -> p.endsWith(".smithy"));
        return json && idl ? "IDL and JSON AST" : json ? "JSON AST only" : "IDL only";
    }

    private static String sha256(byte[] content) {
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] bytes(Random random, int count) {
        byte[] result = new byte[count];
        random.nextBytes(result);
        return result;
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
