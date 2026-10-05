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

package software.amazon.awssdk.awscodegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.awssdk.awscodegen.validation.ModelSource;
import software.amazon.awssdk.awscodegen.validation.ModelSourceValidator;
import software.amazon.awssdk.awscodegen.validation.ModelValidationDiagnostics;
import software.amazon.awssdk.awscodegen.validation.SemanticValidator;
import software.amazon.smithy.codegen.core.directed.CodegenDirector;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidatedResult;

/**
 * The single entry point of the AWS SDK for Java V3 code generator.
 *
 * <p>Every generated service source is produced by {@link #generate(AwsCodegenRequest)}. Build-tool wrappers such as
 * {@code aws-codegen-maven-plugin} only translate their configuration into an {@link AwsCodegenRequest} and register
 * the returned source root; they contain no emitters of their own.
 *
 * <p>One invocation:
 * <ol>
 *   <li>classifies the model-directory files by content, rejects every C2J or V2 sidecar input
 *       ({@link ModelSourceValidator}), and computes a location-independent digest of the canonical model files;</li>
 *   <li>discovers the {@link AwsCodegenIntegration}s and orders them by {@code (order(), identifier())};</li>
 *   <li>assembles and validates the Smithy {@link Model}, the only semantic input, retaining unknown traits, resolves
 *       the service shape, and checks the required AWS semantics ({@link SemanticValidator});</li>
 *   <li>runs Smithy's {@link CodegenDirector} with {@link AwsDirectedCodegen}, the {@link AwsSymbolProvider} and an
 *       in-memory {@link StagedFileManifest}, producing an {@link AwsCodegenContext} of
 *       {@code Model + ServiceShape + SymbolProvider + Settings + FileManifest + ordered integrations}; every
 *       integration hook and generator directive is attributed in a {@link ContributionLedger}, and conflicting
 *       model changes, symbol decorations or file outputs of two contributors fail generation with diagnostics
 *       naming both contributors and the shape; before any file is generated, two shapes whose generated Java types
 *       would share a name or source file (case-insensitively) fail generation with a
 *       {@code GENERATED_NAME_COLLISION} diagnostic naming both shapes and the type;</li>
 *   <li>rejects the staged tree if any file resolves code by class name or reflection, or embeds a Smithy model
 *       ({@link GeneratedSourceGuard}), or contains an absolute input or output path
 *       ({@link LocationIndependenceGuard});</li>
 *   <li>stages the model provenance and the generation manifest into a separate resource tree
 *       ({@link GenerationManifestGenerator}): the exact service shape ID and untruncated model revision, the
 *       generator, JDK and configuration identity, the digest of every generated file, and the semantic inventory;</li>
 *   <li>publishes the staged source and resource trees to the output directories only after every step succeeded
 *       ({@link StagedFileManifest#publishTogether}).</li>
 * </ol>
 * No step builds or serializes an intermediate whole-service representation: emitters and integrations query the
 * {@code Model} (and Smithy indexes over it) directly. Validation steps report every problem they find as structured
 * {@link Diagnostic}s on the {@link AwsCodegenException}; they run before anything is staged, so a rejected service
 * produces no generated tree.
 */
public final class AwsCodegen {

    /** Name of the generated identity type emitted into the service package. */
    public static final String SERVICE_INFO_TYPE = "GeneratedServiceInfo";

    /**
     * Contributor identifier of the core generator in generation diagnostics; reserved, so no integration can use it.
     */
    public static final String GENERATOR_IDENTIFIER = "aws-codegen";

    private AwsCodegen() {
    }

    /**
     * Generates the Java sources of one service and publishes them to {@link AwsCodegenRequest#outputDirectory()},
     * and its provenance and generation manifest to {@link AwsCodegenRequest#resourceOutputDirectory()}, replacing
     * any previous content of those directories. On failure the previously published trees are left untouched.
     * Integrations are discovered from the class loader that loaded {@code aws-codegen}.
     *
     * @throws AwsCodegenException if the model inputs are missing, unreadable or invalid, the service is not in the
     *         model, an integration is invalid or fails, or the tree cannot be published
     */
    public static AwsCodegenResult generate(AwsCodegenRequest request) {
        return generate(request, AwsCodegenIntegrations.discover(AwsCodegen.class.getClassLoader()));
    }

    /**
     * {@link #generate(AwsCodegenRequest)} with an explicit set of integration candidates instead of the discovered
     * ones. The candidates are validated and ordered by {@code (order(), identifier())}, so the result does not depend
     * on their iteration order.
     *
     * <p>Every integration hook and every generator directive is attributed to its contributor. Before anything is
     * published, generation fails with one {@link Diagnostic} per conflict, naming every integration involved and the
     * conflicting shape (Requirement 3.7): two contributors producing the same file, two integrations changing the
     * symbol of the same shape, or two integrations changing the same trait or definition of a shape while
     * preprocessing the model (see {@code ContributionLedger}). It also fails if a generated file contains the absolute
     * path of the model or output directories (Requirement 3.6).
     */
    public static AwsCodegenResult generate(AwsCodegenRequest request,
                                            Iterable<? extends AwsCodegenIntegration> candidates) {
        AwsCodegenSettings settings = AwsCodegenSettings.from(request);
        List<Path> modelFiles = validatedModelFiles(settings.service(), request.modelDirectory());
        String modelDigest = digest(request.modelDirectory(), modelFiles);
        ModelRevision revision = ModelRevision.of(request.modelRevision(), modelDigest);
        List<GenerationManifestGenerator.ModelFile> modelFileDigests =
            modelFileDigests(request.modelDirectory(), modelFiles);
        GeneratorIdentity generator = GeneratorIdentity.current();
        List<AwsCodegenIntegration> integrations = AwsCodegenIntegrations.order(candidates);
        Model model = assemble(settings.service(), request.modelDirectory(), modelFiles);
        ServiceShape service = expectService(model, settings.service());
        requireNoDiagnostics(settings.service(), "lacks required AWS semantics",
                             SemanticValidator.validate(model, service));

        ContributionLedger ledger = new ContributionLedger(settings.service().toString());
        List<AwsCodegenIntegration> attributed = AttributedIntegration.wrap(integrations, ledger);
        StagedFileManifest manifest = new StagedFileManifest(request.outputDirectory(), ledger);
        CodegenDirector<AwsJavaWriter, AwsCodegenIntegration, AwsCodegenContext, AwsCodegenSettings> director =
            new CodegenDirector<>();
        director.integrationClass(AwsCodegenIntegration.class);
        director.integrationFinder(() -> attributed);
        AwsDirectedCodegen directedCodegen = new AwsDirectedCodegen(
            modelDigest, revision, integrations.stream().map(AwsCodegenIntegration::identifier).toList());
        director.directedCodegen(directedCodegen);
        director.model(model);
        director.settings(settings);
        director.service(settings.service());
        director.fileManifest(manifest);
        director.performDefaultCodegenTransforms();
        // Most canonical AWS models still declare enums as Smithy 1 string shapes with @enum. Like Smithy Java, convert
        // them to enum shapes (synthesizing constant names for unnamed values) so every modeled enum is generated as
        // a Java enum through the enum directive instead of reaching it as a plain string shape.
        director.changeStringEnumsToEnumShapes(true);
        try {
            director.run();
        } catch (AwsCodegenException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AwsCodegenException("Code generation failed for service " + settings.service() + ": "
                                          + e.getMessage(), e);
        }

        ledger.requireNoConflicts();
        GeneratedSourceGuard.check(manifest);
        StagedFileManifest resources = new StagedFileManifest(request.resourceOutputDirectory(), ledger);
        try {
            GenerationManifestGenerator.generate(directedCodegen.context(), modelFileDigests, generator, manifest,
                                                 resources);
        } catch (AwsCodegenException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AwsCodegenException("Generating the provenance and generation manifest failed for service "
                                          + settings.service() + ": " + e.getMessage(), e);
        }
        GeneratedSourceGuard.check(resources);
        LocationIndependenceGuard.check(ledger, List.of(manifest, resources),
                                        List.of(request.modelDirectory(), request.outputDirectory(),
                                                request.resourceOutputDirectory()));
        StagedFileManifest.publishTogether(List.of(manifest, resources));
        return new AwsCodegenResult(request.outputDirectory(), manifest.relativeFiles(), modelDigest,
                                    request.resourceOutputDirectory(), resources.relativeFiles(), revision.value());
    }

    /**
     * Assembles the model files with the traits and models available to {@code aws-codegen}'s class loader (the
     * Smithy prelude plus the AWS, rules-engine and waiter trait definitions). Traits without a definition on that
     * class loader are retained verbatim as {@code DynamicTrait}s with their exact ID, target and node value
     * (Requirement 2.11). Any {@code DANGER} or {@code ERROR} event fails assembly.
     */
    static Model assemble(ShapeId service, Path modelDirectory, List<Path> relativeModelFiles) {
        ClassLoader classLoader = AwsCodegen.class.getClassLoader();
        ModelAssembler assembler = Model.assembler(classLoader).discoverModels(classLoader)
                                        .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
        relativeModelFiles.forEach(relative -> assembler.addImport(modelDirectory.resolve(relative)));
        ValidatedResult<Model> result = assembler.assemble();
        requireNoDiagnostics(service, "is invalid",
                             ModelValidationDiagnostics.from(service.toString(), modelDirectory, result));
        return result.getResult().orElseThrow();
    }

    /**
     * Returns the canonical model files of {@code modelDirectory} after rejecting every C2J, V2 sidecar, unrecognized
     * or unreadable input found there (Requirements 2.3-2.9).
     */
    static List<Path> validatedModelFiles(ShapeId service, Path modelDirectory) {
        List<ModelSource> sources = scan(modelDirectory);
        requireNoDiagnostics(service, "contains non-canonical inputs",
                             ModelSourceValidator.validate(service.toString(), sources));
        return canonicalFiles(modelDirectory, sources);
    }

    /** Fails with every diagnostic, sorted, if there is any: nothing is staged or published for the service. */
    private static void requireNoDiagnostics(ShapeId service, String problem, List<Diagnostic> diagnostics) {
        if (!diagnostics.isEmpty()) {
            throw new AwsCodegenException(
                "The canonical model input of service " + service + " " + problem + " (" + diagnostics.size()
                + " diagnostic(s)); no Generated Service Package artifact was produced:",
                diagnostics.stream().sorted().toList());
        }
    }

    private static ServiceShape expectService(Model model, ShapeId service) {
        return model.getShape(service).flatMap(Shape::asServiceShape).orElseThrow(() -> new AwsCodegenException(
            "Service " + service + " is not a service shape in the model; services in the model: "
            + model.getServiceShapes().stream().map(Shape::getId).map(ShapeId::toString).sorted().toList()));
    }

    /**
     * Returns every canonical Smithy model file (Smithy IDL and Smithy JSON AST, identified by content) under
     * {@code modelDirectory}, sorted by relative path. Non-canonical files are not returned; use
     * {@link #validatedModelFiles} to reject them.
     */
    static List<Path> discoverModelFiles(Path modelDirectory) {
        return canonicalFiles(modelDirectory, scan(modelDirectory));
    }

    private static List<ModelSource> scan(Path modelDirectory) {
        if (!Files.isDirectory(modelDirectory)) {
            throw new AwsCodegenException("Model directory " + modelDirectory + " does not exist.");
        }
        try {
            return ModelSourceValidator.scan(modelDirectory);
        } catch (UncheckedIOException e) {
            throw new AwsCodegenException("Unable to list model files under " + modelDirectory, e);
        }
    }

    private static List<Path> canonicalFiles(Path modelDirectory, List<ModelSource> sources) {
        List<Path> models = sources.stream()
                                   .filter(s -> s.format().isCanonical())
                                   .map(ModelSource::relativePath)
                                   .sorted(Comparator.comparing(StagedFileManifest::portable))
                                   .toList();
        if (models.isEmpty()) {
            throw new AwsCodegenException("No Smithy model files (*.smithy, *.json) found under " + modelDirectory + ".");
        }
        return models;
    }

    /** The {@code sha256:<hex>} content digest of every model file, by portable relative path. */
    static List<GenerationManifestGenerator.ModelFile> modelFileDigests(Path modelDirectory,
                                                                       List<Path> relativeModelFiles) {
        return relativeModelFiles.stream().map(relative -> {
            try {
                return new GenerationManifestGenerator.ModelFile(
                    StagedFileManifest.portable(relative),
                    GenerationManifestGenerator.sha256(Files.readAllBytes(modelDirectory.resolve(relative))));
            } catch (IOException e) {
                throw new AwsCodegenException("Unable to read model file " + modelDirectory.resolve(relative), e);
            }
        }).toList();
    }

    /**
     * Digests the relative path and content of every model file. Absolute paths never contribute, so the digest is the
     * same for every checkout location.
     */
    static String digest(Path modelDirectory, List<Path> relativeModelFiles) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
        for (Path relative : relativeModelFiles) {
            byte[] name = StagedFileManifest.portable(relative).getBytes(StandardCharsets.UTF_8);
            byte[] content;
            try {
                content = Files.readAllBytes(modelDirectory.resolve(relative));
            } catch (IOException e) {
                throw new AwsCodegenException("Unable to read model file " + modelDirectory.resolve(relative), e);
            }
            sha256.update(ByteBuffer.allocate(Integer.BYTES).putInt(name.length).array());
            sha256.update(name);
            sha256.update(ByteBuffer.allocate(Long.BYTES).putLong(content.length).array());
            sha256.update(content);
        }
        return "sha256:" + HexFormat.of().formatHex(sha256.digest());
    }
}
