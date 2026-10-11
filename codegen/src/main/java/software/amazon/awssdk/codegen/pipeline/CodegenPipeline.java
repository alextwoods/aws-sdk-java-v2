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

package software.amazon.awssdk.codegen.pipeline;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.CodeGenerator;
import software.amazon.awssdk.codegen.c2j2smithy.C2jInput;
import software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverter;
import software.amazon.awssdk.codegen.c2j2smithy.ConversionResult;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * The Codegen_Pipeline (design A.14, Requirement 17): turns one service's model into generated code in six stages that
 * run strictly in order.
 *
 * <ol>
 *     <li>{@link Stage#C2J_LOAD}: load {@code service-2.json} and its sidecar files.</li>
 *     <li>{@link Stage#C2J_CUSTOMIZATIONS}: apply the C2J half of the {@code customization.config} customizations.</li>
 *     <li>{@link Stage#C2J_CONVERSION}: convert to Smithy with the C2J_Converter, and write the gap report.</li>
 *     <li>{@link Stage#ASSEMBLY_AND_VALIDATION}: assemble and validate the Smithy model; any {@code ERROR} or
 *     {@code DANGER} event stops the service.</li>
 *     <li>{@link Stage#INTERMEDIATE_MODEL}: build the intermediate model with {@link SmithyIntermediateModelBuilder};
 *     for a C2J model, supplement its paginators and waiters with the sidecar definitions the converter could not
 *     express ({@link C2jSidecarSupplement}).</li>
 *     <li>{@link Stage#GENERATION}: generate code into a staging directory and move it into place.</li>
 * </ol>
 *
 * <p>Stages 1 to 3 run only for a C2J model; a native Smithy model starts at stage 4. A failing stage throws
 * {@link CodegenPipelineException} and leaves the sources, resources and tests directories untouched. The only file a
 * failed run can leave behind is the gap report, which is written on every converter run, and the codegen validation
 * report when it was requested.
 *
 * <p>The Maven plugin ({@code GenerationMojo}) and the smithy-build plugin ({@code AwsSdkJavaCodegenPlugin}) both
 * generate through this class.
 */
@SdkInternalApi
public final class CodegenPipeline {

    /** The directory under {@link ServiceInputs#buildDirectory()} that holds staging directories. */
    public static final String STAGING_DIRECTORY = "codegen-staging";

    private static final String VALIDATION_REPORT = "models/validation-report.json";

    private final CodegenPipelineListener listener;

    private CodegenPipeline(Builder builder) {
        this.listener = builder.listener == null ? CodegenPipelineListener.logging() : builder.listener;
    }

    public static CodegenPipeline create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Run all stages for one service.
     *
     * @throws CodegenPipelineException if a stage fails; nothing is generated for the service
     */
    public PreparedService run(ServiceInputs inputs) {
        PreparedService prepared = prepare(inputs);
        generate(prepared, null);
        return prepared;
    }

    /**
     * Run stages 1 to 5 for one service. Nothing is written to the output directories.
     *
     * @throws CodegenPipelineException if a stage fails
     */
    public PreparedService prepare(ServiceInputs inputs) {
        Objects.requireNonNull(inputs, "inputs");
        String service = inputs.serviceName();

        Model model;
        ConversionResult conversion = null;
        C2jFiles c2jFiles = null;
        switch (inputs.source()) {
            case C2J:
                C2jFiles c2j = stage(service, Stage.C2J_LOAD, () -> load(inputs));
                c2jFiles = c2j;
                ServiceModel customized = stage(service, Stage.C2J_CUSTOMIZATIONS, () -> C2jCustomizations.apply(
                    service, c2j.serviceModel, inputs.customizationConfig(), listener));
                conversion = stage(service, Stage.C2J_CONVERSION, () -> convert(inputs, c2j.input, customized));
                ConversionResult converted = conversion;
                model = stage(service, Stage.ASSEMBLY_AND_VALIDATION, () -> assembleConverted(inputs, converted));
                break;
            case SMITHY_FILES:
                model = stage(service, Stage.ASSEMBLY_AND_VALIDATION, () -> assembleFiles(inputs));
                break;
            case SMITHY_MODEL:
                model = stage(service, Stage.ASSEMBLY_AND_VALIDATION, () -> checkAssembled(inputs));
                break;
            default:
                throw new IllegalStateException("Unknown source " + inputs.source());
        }

        Model validated = model;
        C2jFiles sidecars = c2jFiles;
        List<List<String>> supplemented = new ArrayList<>();
        IntermediateModel intermediateModel = stage(service, Stage.INTERMEDIATE_MODEL, () -> {
            IntermediateModel built = new SmithyIntermediateModelBuilder(
                SmithyModels.builder()
                            .model(validated)
                            .customizationConfig(inputs.customizationConfig())
                            .build())
                .build();
            if (sidecars != null) {
                C2jSidecarSupplement supplement = C2jSidecarSupplement.apply(built, sidecars.paginators,
                                                                             sidecars.waiters);
                supplemented.add(supplement.supplementedPaginators());
                supplemented.add(supplement.supplementedWaiters());
            }
            return built;
        });

        return new PreparedService(inputs, model, conversion == null ? null : conversion.gapReport(), intermediateModel,
                                   supplemented.isEmpty() ? Collections.emptyList() : supplemented.get(0),
                                   supplemented.isEmpty() ? Collections.emptyList() : supplemented.get(1));
    }

    /**
     * Run stage 6 for a prepared service: generate into a staging directory under
     * {@link ServiceInputs#buildDirectory()}, then move the generated files into the sources, resources and tests
     * directories. On failure the staging directory is deleted and the output directories are left untouched.
     *
     * @param shareModelsTarget the intermediate model of the service this one shares model classes with, or null
     * @throws CodegenPipelineException if generation fails
     */
    public void generate(PreparedService prepared, IntermediateModel shareModelsTarget) {
        generate(Collections.singletonList(prepared), s -> shareModelsTarget);
    }

    /**
     * Run stage 6 for services that generate into the same directories, such as the services of one Maven module, in
     * list order. They share one staging directory, so that a service that shares model classes with an earlier one
     * (shareModelConfig) finds them, and the output of all of them is moved into place only when all succeed.
     *
     * @param shareModelsTargets the intermediate model each service shares model classes with, or null
     * @throws CodegenPipelineException if generation fails for any of the services
     * @throws IllegalArgumentException if the services do not have the same output and build directories
     */
    public void generate(List<PreparedService> services, Function<PreparedService, IntermediateModel> shareModelsTargets) {
        if (services.isEmpty()) {
            return;
        }
        ServiceInputs first = services.get(0).inputs();
        for (PreparedService service : services) {
            ServiceInputs inputs = service.inputs();
            if (!inputs.sourcesDirectory().equals(first.sourcesDirectory())
                || !inputs.resourcesDirectory().equals(first.resourcesDirectory())
                || !inputs.testsDirectory().equals(first.testsDirectory())
                || !inputs.buildDirectory().equals(first.buildDirectory())) {
                throw new IllegalArgumentException("Services generated together must have the same output and build "
                                                   + "directories, but " + inputs.serviceName() + " and "
                                                   + first.serviceName() + " differ");
            }
        }

        Path stagingRoot = first.buildDirectory().resolve(STAGING_DIRECTORY);
        Path staging = stage(first.serviceName(), Stage.GENERATION,
                             () -> createStagingDirectory(stagingRoot, first.serviceName()));
        Path sources = staging.resolve("sources");
        Path resources = staging.resolve("resources");
        Path tests = staging.resolve("tests");
        boolean moved = false;
        try {
            for (int i = 0; i < services.size(); i++) {
                PreparedService service = services.get(i);
                String name = service.inputs().serviceName();
                if (i > 0) {
                    // The first service's stage was reported when the staging directory was created.
                    listener.stageStarted(name, Stage.GENERATION);
                }
                runInStage(name, () -> generateInto(service, shareModelsTargets.apply(service), sources, resources,
                                                    tests));
            }
            String names = services.stream().map(s -> s.inputs().serviceName()).collect(Collectors.joining(", "));
            runInStage(names, () -> {
                moveContents(sources, first.sourcesDirectory());
                moveContents(resources, first.resourcesDirectory());
                moveContents(tests, first.testsDirectory());
            });
            moved = true;
        } finally {
            if (!moved && services.stream().anyMatch(s -> s.inputs().emitValidationReport())) {
                keepValidationReport(sources, first.sourcesDirectory());
            }
            deleteRecursively(staging);
            deleteIfEmpty(stagingRoot);
        }
    }

    /**
     * Runs part of stage 6 whose start was already reported to the listener.
     */
    private static void runInStage(String service, Runnable body) {
        try {
            body.run();
        } catch (CodegenPipelineException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CodegenPipelineException(service, Stage.GENERATION, String.valueOf(e.getMessage()), e);
        }
    }

    private <T> T stage(String service, Stage stage, Supplier<T> body) {
        listener.stageStarted(service, stage);
        try {
            return body.get();
        } catch (CodegenPipelineException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CodegenPipelineException(service, stage, String.valueOf(e.getMessage()), e);
        }
    }

    // Stage 1

    private static C2jFiles load(ServiceInputs inputs) {
        Path directory = inputs.c2jModelDirectory();
        C2jInput input = C2jInput.fromDirectory(inputs.serviceName(), directory);
        ServiceModel serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class,
                                                               directory.resolve(C2jInput.SERVICE_MODEL_FILE).toFile());
        Paginators paginators = ModelLoaderUtils.loadOptionalModel(Paginators.class,
                                                                   directory.resolve(C2jInput.PAGINATORS_FILE).toFile())
                                                .orElse(Paginators.none());
        Waiters waiters = ModelLoaderUtils.loadOptionalModel(Waiters.class, directory.resolve(C2jInput.WAITERS_FILE).toFile())
                                          .orElse(Waiters.none());
        return new C2jFiles(input, serviceModel, paginators, waiters);
    }

    // Stage 3

    private static ConversionResult convert(ServiceInputs inputs, C2jInput loaded, ServiceModel customized) {
        C2jInput input = C2jInput.builder()
                                 .serviceName(loaded.serviceName())
                                 .serviceModel(C2jServiceModelSerializer.toTree(customized))
                                 .paginatorsModel(loaded.paginatorsModel())
                                 .waitersModel(loaded.waitersModel())
                                 .endpointRuleSetModel(loaded.endpointRuleSetModel())
                                 .endpointTestsModel(loaded.endpointTestsModel())
                                 .build();
        ConversionResult result = C2jToSmithyConverter.convert(input);
        result.gapReport().writeTo(inputs.buildDirectory());
        return result;
    }

    // Stage 4

    private static Model assembleConverted(ServiceInputs inputs, ConversionResult conversion) {
        // Re-assembled from the serialized model, so that every trait is validated against its definition, including
        // the com.amazonaws.c2j marker traits discovered from the converter's META-INF/smithy manifest. The rule-set
        // built-ins come from smithy-aws-endpoints on the class path.
        ClassLoader classLoader = inputs.classLoader();
        ModelAssembler assembler = SmithyModelDiscovery.discoverModels(Model.assembler(classLoader), classLoader)
                                                       .addDocumentNode(ModelSerializer.builder().build()
                                                                                       .serialize(conversion.model()));
        return withConvertedErrorOrder(validated(inputs, assembler.assemble()), conversion.model());
    }

    /**
     * {@link ModelSerializer} sorts the errors of operations and services by ShapeId, so the re-assembled model loses
     * the C2J declaration order that the converter kept. That order decides the order of the generated
     * {@code @throws} documentation and {@code throws} clauses, so it is restored from the converted model.
     */
    private static Model withConvertedErrorOrder(Model assembled, Model converted) {
        List<Shape> reordered = new ArrayList<>();
        for (OperationShape operation : converted.getOperationShapes()) {
            assembled.getShape(operation.getId()).flatMap(Shape::asOperationShape)
                     .filter(a -> !a.getErrors().equals(operation.getErrors())
                                  && a.getErrorsSet().equals(operation.getErrorsSet()))
                     .ifPresent(a -> reordered.add(a.toBuilder().errors(operation.getErrors()).build()));
        }
        for (ServiceShape service : converted.getServiceShapes()) {
            assembled.getShape(service.getId()).flatMap(Shape::asServiceShape)
                     .filter(a -> !a.getErrors().equals(service.getErrors())
                                  && a.getErrorsSet().equals(service.getErrorsSet()))
                     .ifPresent(a -> reordered.add(a.toBuilder().errors(service.getErrors()).build()));
        }
        return reordered.isEmpty() ? assembled : assembled.toBuilder().addShapes(reordered).build();
    }

    private static Model assembleFiles(ServiceInputs inputs) {
        ClassLoader classLoader = inputs.classLoader();
        ModelAssembler assembler = SmithyModelDiscovery.discoverModels(Model.assembler(classLoader), classLoader);
        for (Path source : inputs.smithyModelSources()) {
            if (!Files.exists(source)) {
                throw new IllegalArgumentException("The Smithy model source " + source + " does not exist");
            }
            assembler.addImport(source);
        }
        return validated(inputs, assembler.assemble());
    }

    private static Model checkAssembled(ServiceInputs inputs) {
        failOnErrors(inputs, inputs.smithyModelEvents());
        return inputs.smithyModel();
    }

    private static Model validated(ServiceInputs inputs, ValidatedResult<Model> result) {
        failOnErrors(inputs, result.getValidationEvents());
        return result.getResult().orElseThrow(() -> new IllegalStateException(
            "The Smithy model of " + inputs.serviceName() + " could not be assembled"));
    }

    private static void failOnErrors(ServiceInputs inputs, List<ValidationEvent> events) {
        List<ValidationEvent> errors = events.stream()
                                             .filter(e -> e.getSeverity() == Severity.ERROR
                                                          || e.getSeverity() == Severity.DANGER)
                                             .collect(Collectors.toList());
        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                "The Smithy model of " + inputs.serviceName() + " has " + errors.size() + " validation event(s) of "
                + "severity ERROR or DANGER:\n" + errors.stream()
                                                        .map(CodegenPipeline::describe)
                                                        .collect(Collectors.joining("\n")));
        }
    }

    static String describe(ValidationEvent event) {
        return String.format("  %s %s [%s]: %s", event.getSeverity(), event.getId(),
                             event.getShapeId().map(ShapeId::toString).orElse("-"), event.getMessage());
    }

    // Stage 6

    private static void generateInto(PreparedService prepared, IntermediateModel shareModelsTarget, Path sources,
                                     Path resources, Path tests) {
        ServiceInputs inputs = prepared.inputs();
        CodeGenerator.builder()
                     .intermediateModel(prepared.intermediateModel())
                     .shareModelsTarget(shareModelsTarget)
                     .sourcesDirectory(sources.toString())
                     .resourcesDirectory(resources.toString())
                     .testsDirectory(tests.toString())
                     .intermediateModelFileNamePrefix(inputs.writeIntermediateModel()
                                                      ? fileNamePrefix(prepared.intermediateModel())
                                                      : null)
                     .emitValidationReport(inputs.emitValidationReport())
                     .build()
                     .execute();
    }

    private static String fileNamePrefix(IntermediateModel model) {
        Metadata metadata = model.getMetadata();
        return metadata.getEndpointPrefix() + "-" + metadata.getApiVersion();
    }

    private static Path createStagingDirectory(Path stagingRoot, String service) {
        try {
            Files.createDirectories(stagingRoot);
            return Files.createTempDirectory(stagingRoot, service.replaceAll("[^A-Za-z0-9._-]", "_") + "-");
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create a staging directory under " + stagingRoot, e);
        }
    }

    /**
     * Moves every file under {@code from} to the same relative path under {@code to}, replacing existing files. Files
     * already under {@code to} that were not generated are kept.
     */
    static void moveContents(Path from, Path to) {
        if (!Files.isDirectory(from)) {
            return;
        }
        List<Path> files;
        try (Stream<Path> walk = Files.walk(from)) {
            files = walk.filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to list generated files in " + from, e);
        }
        for (Path file : files) {
            Path target = to.resolve(from.relativize(file).toString());
            try {
                createParentDirectories(target);
                Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to move generated file " + file + " to " + target, e);
            }
        }
    }

    private static void keepValidationReport(Path stagingSources, Path sourcesDirectory) {
        Path report = stagingSources.resolve(VALIDATION_REPORT);
        if (!Files.isRegularFile(report)) {
            return;
        }
        Path target = sourcesDirectory.resolve(VALIDATION_REPORT);
        try {
            createParentDirectories(target);
            Files.move(report, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to move the validation report to " + target, e);
        }
    }

    private static void createParentDirectories(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static void deleteRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(directory)) {
            paths = walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to delete staging directory " + directory, e);
        }
        List<IOException> failures = new ArrayList<>();
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                failures.add(e);
            }
        }
        if (!failures.isEmpty()) {
            UncheckedIOException failure = new UncheckedIOException("Unable to delete staging directory " + directory,
                                                                    failures.get(0));
            failures.stream().skip(1).forEach(failure::addSuppressed);
            throw failure;
        }
    }

    private static void deleteIfEmpty(Path directory) {
        try {
            Files.deleteIfExists(directory);
        } catch (DirectoryNotEmptyException e) {
            // Another service is generating concurrently.
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to delete " + directory, e);
        }
    }

    /**
     * The stages of the pipeline, in the order they run.
     */
    public enum Stage {
        C2J_LOAD(1, "C2J load"),
        C2J_CUSTOMIZATIONS(2, "C2J customizations"),
        C2J_CONVERSION(3, "C2J to Smithy conversion"),
        ASSEMBLY_AND_VALIDATION(4, "Smithy model assembly and validation"),
        INTERMEDIATE_MODEL(5, "intermediate model"),
        GENERATION(6, "generation");

        private final int number;
        private final String description;

        Stage(int number, String description) {
            this.number = number;
            this.description = description;
        }

        /** The stage number, 1 to 6. */
        public int number() {
            return number;
        }

        @Override
        public String toString() {
            return number + " (" + description + ")";
        }
    }

    public static final class Builder {
        private CodegenPipelineListener listener;

        private Builder() {
        }

        /**
         * Receives stage starts and warnings. Defaults to {@link CodegenPipelineListener#logging()}.
         */
        public Builder listener(CodegenPipelineListener listener) {
            this.listener = listener;
            return this;
        }

        public CodegenPipeline build() {
            return new CodegenPipeline(this);
        }
    }

    private static final class C2jFiles {
        private final C2jInput input;
        private final ServiceModel serviceModel;
        private final Paginators paginators;
        private final Waiters waiters;

        private C2jFiles(C2jInput input, ServiceModel serviceModel, Paginators paginators, Waiters waiters) {
            this.input = input;
            this.serviceModel = serviceModel;
            this.paginators = paginators;
            this.waiters = waiters;
        }
    }
}
