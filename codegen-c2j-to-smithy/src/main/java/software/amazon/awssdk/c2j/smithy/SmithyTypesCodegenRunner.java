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

package software.amazon.awssdk.c2j.smithy;

import java.nio.file.Path;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Runs smithy-java's own type/schema code generator on a model produced by {@link C2jToSmithyConverter}.
 *
 * <p>This is the {@code smithy -> java} step, and it deliberately <b>reuses smithy-java's published
 * code generator</b> ({@code JavaCodegenPlugin} in {@code types} mode) rather than re-implementing
 * schema-literal emission in the v2 codegen. The full pipeline is:
 *
 * <pre>C2J service-2.json --(C2jToSmithyConverter)--&gt; Smithy Model --(JavaCodegenPlugin types)--&gt;
 * native smithy-java POJOs (with native $SCHEMA literals, Schemas holder, SharedSerde, and
 * switch(memberIndex) builders).</pre>
 *
 * <p>Because we reuse the upstream generator, the generated types inherit document types, type
 * registries, and runtime protocol selection as smithy-java adds them.
 */
public final class SmithyTypesCodegenRunner {

    private SmithyTypesCodegenRunner() {
    }

    /**
     * Convert a C2J {@code service-2.json} and generate Smithy-java types into {@code outputDir} under
     * the given Java package {@code namespace}. Input path: {@code C2J -> Smithy -> codegen}, no IR.
     */
    public static void generate(Path serviceJson, Path outputDir, String namespace) {
        Model model = C2jToSmithyConverter.convert(serviceJson);
        generate(model, outputDir, namespace);
    }

    /**
     * Generate Smithy-java types directly from a Smithy model on disk ({@code .smithy} or Smithy JSON
     * AST files, or a directory of them) — the native Smithy input path, with no C2J and no
     * {@link software.amazon.awssdk.codegen.model.intermediate.IntermediateModel IR} anywhere.
     */
    public static void generateFromSmithy(Path modelPathOrDir, Path outputDir, String namespace) {
        generate(assembleFromDisk(modelPathOrDir), outputDir, namespace);
    }

    /**
     * Serialize a Smithy {@link Model} to a Smithy JSON-AST file on disk. Used to materialize a
     * Smithy model (e.g. one produced by {@link C2jToSmithyConverter}) so it can be fed back through
     * the native Smithy input path independently of C2J.
     */
    public static void writeSmithyJson(Model model, Path outputFile) {
        // Drop the C2J-only carrier traits (com.amazonaws.c2j#*) on the way out: they exist purely to
        // round-trip C2J->IR back-compat metadata and have no trait definition / meaning for native
        // Smithy codegen, so a re-assembled model must not reference them.
        software.amazon.smithy.model.shapes.ModelSerializer serializer =
                software.amazon.smithy.model.shapes.ModelSerializer.builder()
                        .traitFilter(trait -> !trait.toShapeId().getNamespace().equals("com.amazonaws.c2j"))
                        .build();
        software.amazon.smithy.model.node.ObjectNode ast = serializer.serialize(model);
        try {
            java.nio.file.Files.createDirectories(outputFile.getParent());
            java.nio.file.Files.writeString(outputFile, Node.prettyPrintJson(ast));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("Failed to write Smithy model to " + outputFile, e);
        }
    }

    /**
     * Assemble a Smithy {@link Model} from a file or a directory tree of Smithy model files.
     *
     * <p>Trait <i>definitions</i> shipped on the classpath (aws.protocols#*, aws.api#*, the smithy
     * prelude) are discovered so the model's trait applications resolve; but those discovered
     * definition shapes are NOT part of the authored model and must not be code-generated. The
     * authored model is exactly the shapes that came from {@code modelPathOrDir}, so we capture those
     * shape ids before discovery and return a model containing only them (plus the prelude, which the
     * codegen plugin already filters out).
     */
    public static Model assembleFromDisk(Path modelPathOrDir) {
        ClassLoader cl = SmithyTypesCodegenRunner.class.getClassLoader();

        // First pass: the authored shapes only (no discovery), so we know what to keep.
        software.amazon.smithy.model.loader.ModelAssembler authored = Model.assembler()
                .disableValidation();   // trait defs aren't present yet; we only need shape ids here
        addImports(authored, modelPathOrDir);
        java.util.Set<software.amazon.smithy.model.shapes.ShapeId> authoredIds =
                new java.util.HashSet<>();
        authored.assemble().getResult().ifPresent(m -> m.shapes()
                .filter(s -> !s.isMemberShape())
                .forEach(s -> authoredIds.add(s.getId())));

        // Second pass: with trait definitions discovered, so applications resolve and validate.
        software.amazon.smithy.model.loader.ModelAssembler assembler = Model.assembler(cl).discoverModels(cl);
        addImports(assembler, modelPathOrDir);
        Model full = assembler.assemble().unwrap();

        // Keep only the authored shapes (+ prelude, which codegen filters). This drops the discovered
        // aws.*/smithy.* trait-definition carrier shapes that would otherwise be generated as types.
        return Model.builder()
                .addShapes(full.toSet().stream()
                        .filter(s -> authoredIds.contains(s.getId())
                                     || s.getId().getNamespace().equals("smithy.api"))
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }

    private static void addImports(software.amazon.smithy.model.loader.ModelAssembler assembler,
                                   Path modelPathOrDir) {
        try (java.util.stream.Stream<Path> paths = java.nio.file.Files.walk(modelPathOrDir)) {
            paths.filter(java.nio.file.Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".smithy") || p.toString().endsWith(".json"))
                 .forEach(p -> assembler.addImport(p));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("Failed to read Smithy model at " + modelPathOrDir, e);
        }
    }

    /** Generate Smithy-java types for an already-converted {@link Model}. */
    public static void generate(Model model, Path outputDir, String namespace) {
        FileManifest manifest = FileManifest.create(outputDir);

        // In types mode the generator uses a synthetic service with no protocol trait, so the
        // protocol-declared serialization traits aren't auto-retained in the generated Schemas.
        // List them explicitly as runtimeTraits so HTTP binding + wire-name traits survive into the
        // schema literals (otherwise members serialize as plain body fields and HTTP binding breaks).
        ObjectNode settings = ObjectNode.builder()
                .withMember("name", namespace + ".types")
                .withMember("namespace", namespace)
                .withMember("modes", Node.fromStrings("types"))
                .withMember("runtimeTraits", Node.fromStrings(
                        "smithy.api#httpLabel",
                        "smithy.api#httpHeader",
                        "smithy.api#httpQuery",
                        "smithy.api#httpQueryParams",
                        "smithy.api#httpPayload",
                        "smithy.api#httpPrefixHeaders",
                        "smithy.api#httpResponseCode",
                        "smithy.api#jsonName",
                        "smithy.api#xmlName",
                        "smithy.api#xmlAttribute",
                        "smithy.api#xmlFlattened",
                        "smithy.api#xmlNamespace",
                        "smithy.api#timestampFormat",
                        "smithy.api#mediaType",
                        "smithy.api#hostLabel",
                        "smithy.api#sparse",
                        "smithy.api#required"))
                .build();

        PluginContext context = PluginContext.builder()
                .model(model)
                .fileManifest(manifest)
                .settings(settings)
                .build();

        SmithyBuildPlugin plugin = new software.amazon.smithy.java.codegen.JavaCodegenPlugin();
        plugin.execute(context);
    }

    /**
     * CLI for both no-IR input paths:
     * <ul>
     *   <li>{@code c2j <service-2.json> <outputDir> <javaNamespace>} — C2J input (via converter)</li>
     *   <li>{@code smithy <model-file-or-dir> <outputDir> <javaNamespace>} — Smithy input (direct)</li>
     * </ul>
     * For backwards compatibility, a 3-arg form with no leading mode is treated as {@code c2j}.
     */
    public static void main(String[] args) {
        String mode;
        int i;
        if (args.length == 4) {
            mode = args[0];
            i = 1;
        } else if (args.length == 3) {
            mode = "c2j";
            i = 0;
        } else {
            throw new IllegalArgumentException(
                "Usage: (c2j <service-2.json>|smithy <model>) <outputDir> <javaNamespace>");
        }
        Path input = Path.of(args[i]);
        Path outputDir = Path.of(args[i + 1]);
        String namespace = args[i + 2];
        switch (mode) {
            case "c2j":
                generate(input, outputDir, namespace);
                break;
            case "smithy":
                generateFromSmithy(input, outputDir, namespace);
                break;
            default:
                throw new IllegalArgumentException("Unknown input mode '" + mode + "' (expected c2j|smithy)");
        }
        System.out.println("Generated Smithy-java types into " + outputDir + " (package " + namespace
                           + ", input=" + mode + ", no IR)");
    }
}
