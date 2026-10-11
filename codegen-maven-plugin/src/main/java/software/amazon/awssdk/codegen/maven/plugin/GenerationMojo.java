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

package software.amazon.awssdk.codegen.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.pipeline.CodegenPipeline;
import software.amazon.awssdk.codegen.pipeline.CodegenPipelineException;
import software.amazon.awssdk.codegen.pipeline.PreparedService;
import software.amazon.awssdk.codegen.pipeline.ServiceInputs;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.awssdk.codegen.validation.ModelInvalidException;
import software.amazon.awssdk.codegen.validation.ModelValidationReport;
import software.amazon.awssdk.utils.StringUtils;

/**
 * The Maven mojo to generate Java client code using software.amazon.awssdk:codegen module.
 *
 * <p>A module is generated in one of three ways:
 * <ul>
 *     <li>with a {@code smithy-build.json}: through smithy-build and the {@code aws-sdk-java-v2-codegen} plugin;</li>
 *     <li>with Smithy model files in {@code smithyModelDirectory}: through the {@link CodegenPipeline}, from stage 4;</li>
 *     <li>otherwise, for each {@code service-2.json} under {@code codeGenResources}: through all six stages of the
 *     {@link CodegenPipeline} (C2J load, customizations, C2J to Smithy conversion, Smithy validation, intermediate
 *     model, generation).</li>
 * </ul>
 */
@Mojo(name = "generate", requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerationMojo extends AbstractMojo {
    private static final String MODEL_FILE = "service-2.json";
    private static final String SMITHY_BUILD_FILE = "smithy-build.json";
    private static final String SMITHY_PROJECTIONS_DIR = "smithyprojections";
    private static final String CUSTOMIZATION_CONFIG_FILE = "customization.config";

    @Parameter(property = "codeGenResources", defaultValue = "${basedir}/src/main/resources/codegen-resources/")
    private File codeGenResources;

    /**
     * Native Smithy model files. When this directory holds {@code .smithy} or {@code .json} files, the module is
     * generated from them instead of from the C2J model in {@code codeGenResources}; its {@code customization.config}
     * is still read from {@code codeGenResources}.
     */
    @Parameter(property = "smithyModelDirectory", defaultValue = "${basedir}/model")
    private File smithyModelDirectory;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    private String outputDirectory;

    @Parameter(property = "writeIntermediateModel", defaultValue = "false")
    private boolean writeIntermediateModel;

    @Parameter(property = "writeValidationReport", defaultValue = "false")
    private boolean writeValidationReport;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    private Path sourcesDirectory;
    private Path resourcesDirectory;
    private Path testsDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        Path buildDirectory = Paths.get(outputDirectory);
        this.sourcesDirectory = buildDirectory.resolve("generated-sources").resolve("sdk");
        this.resourcesDirectory = buildDirectory.resolve("generated-resources").resolve("sdk-resources");
        this.testsDirectory = buildDirectory.resolve("generated-test-sources").resolve("sdk-tests");

        Path smithyBuildFile = project.getBasedir().toPath().resolve(SMITHY_BUILD_FILE);
        if (Files.exists(smithyBuildFile)) {
            new SmithyBuildGenerator(project, getLog())
                .generate(smithyBuildFile, buildDirectory.resolve(SMITHY_PROJECTIONS_DIR));
            return;
        }

        List<ServiceInputs> services = hasNativeSmithyModel() ? nativeSmithyInputs(buildDirectory)
                                                              : c2jInputs(buildDirectory);
        generate(services);

        project.addCompileSourceRoot(sourcesDirectory.toFile().getAbsolutePath());
        project.addTestCompileSourceRoot(testsDirectory.toFile().getAbsolutePath());
    }

    /**
     * Runs the Codegen_Pipeline for every service. Stages 1 to 5 run for all services first, so that a service that
     * shares model classes with another (shareModelConfig) is generated with that service's intermediate model.
     */
    private void generate(List<ServiceInputs> services) throws MojoExecutionException {
        CodegenPipeline pipeline = CodegenPipeline.create();
        List<PreparedService> prepared = new ArrayList<>();
        for (ServiceInputs service : services) {
            prepared.add(run(() -> pipeline.prepare(service)));
        }

        Map<String, IntermediateModel> serviceNameToModelMap = new HashMap<>();
        for (PreparedService service : prepared) {
            IntermediateModel model = service.intermediateModel();
            String lowercaseServiceName = StringUtils.lowerCase(model.getMetadata().getServiceName());
            IntermediateModel previous = serviceNameToModelMap.put(lowercaseServiceName, model);
            if (previous != null) {
                getLog().warn(String.format("Multiple service models found with service name %s. Model validation "
                                            + "will likely be incorrect", lowercaseServiceName));
            }
        }

        // One stage 6 for the whole module: the services share a staging directory, and nothing is moved into place
        // unless all of them generate.
        run(() -> {
            pipeline.generate(prepared, service -> {
                CustomizationConfig customizationConfig = service.intermediateModel().getCustomizationConfig();
                if (customizationConfig.getShareModelConfig() == null) {
                    return null;
                }
                return serviceNameToModelMap.get(customizationConfig.getShareModelConfig().getShareModelWith());
            });
            return null;
        });
    }

    /**
     * Runs one pipeline call, writing the validation report for a model validation failure when one was requested.
     */
    private <T> T run(Supplier<T> call) throws MojoExecutionException {
        try {
            return call.get();
        } catch (CodegenPipelineException e) {
            ModelInvalidException invalid = findCause(e, ModelInvalidException.class);
            if (invalid != null && writeValidationReport) {
                ModelValidationReport report = new ModelValidationReport();
                report.setValidationEntries(invalid.validationEntries());
                emitValidationReport(report);
            }
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return type.cast(cause);
            }
        }
        return null;
    }

    /**
     * A module with Smithy model files in {@code smithyModelDirectory} is generated from them, even if it also has a
     * C2J model (Requirement 17.4).
     */
    private boolean hasNativeSmithyModel() throws MojoExecutionException {
        if (smithyModelDirectory == null || !smithyModelDirectory.isDirectory()) {
            return false;
        }
        try (Stream<Path> files = Files.walk(smithyModelDirectory.toPath())) {
            return files.anyMatch(p -> Files.isRegularFile(p)
                                       && (p.toString().endsWith(".smithy") || p.toString().endsWith(".json")));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to list Smithy model files in " + smithyModelDirectory, e);
        }
    }

    private List<ServiceInputs> nativeSmithyInputs(Path buildDirectory) {
        getLog().info("Loading Smithy model from: " + smithyModelDirectory);
        Path customization = codeGenResources.toPath().resolve(CUSTOMIZATION_CONFIG_FILE);
        CustomizationConfig customizationConfig = Files.isRegularFile(customization)
                                                  ? loadCustomizationConfig(codeGenResources.toPath())
                                                  : CustomizationConfig.create();
        return Collections.singletonList(
            inputs(project.getArtifactId(), buildDirectory, customizationConfig)
                .smithyModelSources(Collections.singletonList(smithyModelDirectory.toPath()))
                .build());
    }

    private List<ServiceInputs> c2jInputs(Path buildDirectory) throws MojoExecutionException {
        return findModelRoots().map(r -> {
            getLog().info("Loading from: " + r.modelRoot.toString());
            return inputs(serviceName(r.modelRoot), buildDirectory, r.customizationConfig)
                .c2jModelDirectory(r.modelRoot)
                .build();
        }).collect(Collectors.toList());
    }

    private ServiceInputs.Builder inputs(String serviceName, Path buildDirectory,
                                         CustomizationConfig customizationConfig) {
        return ServiceInputs.builder()
                            .serviceName(serviceName)
                            .customizationConfig(customizationConfig)
                            .sourcesDirectory(sourcesDirectory)
                            .resourcesDirectory(resourcesDirectory)
                            .testsDirectory(testsDirectory)
                            .buildDirectory(buildDirectory)
                            .writeIntermediateModel(writeIntermediateModel)
                            .emitValidationReport(writeValidationReport)
                            .classLoader(CodegenPipeline.class.getClassLoader());
    }

    /**
     * The module's artifact ID when {@code service-2.json} is directly in {@code codeGenResources}, otherwise the name
     * of its directory (for example {@code dynamodbstreams}). Used in messages and for the gap report file name.
     */
    private String serviceName(Path modelRoot) {
        Path directoryName = modelRoot.getFileName();
        if (directoryName == null
            || modelRoot.toAbsolutePath().normalize().equals(codeGenResources.toPath().toAbsolutePath().normalize())) {
            return project.getArtifactId();
        }
        return directoryName.toString();
    }

    private Stream<ModelRoot> findModelRoots() throws MojoExecutionException {
        try {
            return Files.find(codeGenResources.toPath(), 10, this::isModelFile)
                        .map(Path::getParent)
                        .map(p -> new ModelRoot(p, loadCustomizationConfig(p)))
                        .sorted(this::modelSharersLast);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to find '" + MODEL_FILE + "' files in " + codeGenResources, e);
        }
    }

    private int modelSharersLast(ModelRoot lhs, ModelRoot rhs) {
        return lhs.customizationConfig.getShareModelConfig() == null ? -1 : 1;
    }

    private boolean isModelFile(Path p, BasicFileAttributes a) {
        return p.toString().endsWith(MODEL_FILE);
    }

    private CustomizationConfig loadCustomizationConfig(Path root) {
        CustomizationConfig config = ModelLoaderUtils.loadOptionalModel(CustomizationConfig.class,
                                                                        root.resolve(CUSTOMIZATION_CONFIG_FILE).toFile(),
                                                                        true)
                                                     .orElse(CustomizationConfig.create());
        config.resolveProtocolTestSkips(root);
        return config;
    }

    private void emitValidationReport(ModelValidationReport report) {
        Path modelsDir = sourcesDirectory.resolve("models");
        try {
            Files.createDirectories(modelsDir);
        } catch (IOException e) {
            getLog().warn(String.format("Failed to create directory %s, unable to write validation-report.json", modelsDir), e);
            return;
        }

        try (Writer writer = Files.newBufferedWriter(modelsDir.resolve("validation-report.json"),
                                                     StandardCharsets.UTF_8);) {

            Jackson.writeWithObjectMapper(report, writer);
        } catch (IOException e) {
            getLog().warn("Failed to write validation report to " + modelsDir, e);
        }
    }

    private static class ModelRoot {
        private final Path modelRoot;
        private final CustomizationConfig customizationConfig;

        private ModelRoot(Path modelRoot, CustomizationConfig customizationConfig) {
            this.modelRoot = modelRoot;
            this.customizationConfig = customizationConfig;
        }
    }
}
