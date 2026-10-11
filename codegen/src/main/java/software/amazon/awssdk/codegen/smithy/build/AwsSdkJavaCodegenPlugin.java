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

package software.amazon.awssdk.codegen.smithy.build;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.pipeline.CodegenPipeline;
import software.amazon.awssdk.codegen.pipeline.PreparedService;
import software.amazon.awssdk.codegen.pipeline.ServiceInputs;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Generates AWS SDK for Java sources from a Smithy build projection, through the same {@link CodegenPipeline} as the
 * Maven plugin. The projection model is native Smithy, so the pipeline starts at stage 4.
 */
@SdkInternalApi
public final class AwsSdkJavaCodegenPlugin implements SmithyBuildPlugin {

    /** Plugin name. */
    public static final String NAME = "aws-sdk-java-v2-codegen";

    private static final Logger log = LoggerFactory.getLogger(AwsSdkJavaCodegenPlugin.class);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean requiresValidModel() {
        return false;
    }

    @Override
    public void execute(PluginContext context) {
        Model model = context.getModel();
        AwsSdkJavaCodegenSettings settings = AwsSdkJavaCodegenSettings.fromNode(context.getSettings());
        CustomizationConfig customizationConfig = loadCustomizationConfig(settings);

        FileManifest manifest = context.getFileManifest();
        Path base = manifest.getBaseDir();
        String serviceName = serviceName(model, context);

        // Stages 1 to 3 are skipped for a native model; stage 4 checks the projection's validation events.
        PreparedService prepared = CodegenPipeline.create().run(
            ServiceInputs.builder()
                         .serviceName(serviceName)
                         .smithyModel(model, context.getEvents())
                         .customizationConfig(customizationConfig)
                         .sourcesDirectory(base.resolve("generated-sources"))
                         .resourcesDirectory(base.resolve("generated-resources"))
                         .testsDirectory(base.resolve("generated-test-sources"))
                         .buildDirectory(base)
                         .build());

        registerGeneratedFiles(manifest, base);
        log.info("{} generated {} files for service {}",
                 NAME, manifest.getFiles().size(), prepared.intermediateModel().getMetadata().getServiceName());
    }

    /**
     * The projection's service shape name, or the projection name when the model has no single service; stage 5 then
     * reports the service count.
     */
    private static String serviceName(Model model, PluginContext context) {
        Set<ServiceShape> services = model.getServiceShapes();
        if (services.size() == 1) {
            return services.iterator().next().getId().getName();
        }
        return context.getProjectionName();
    }

    private CustomizationConfig loadCustomizationConfig(AwsSdkJavaCodegenSettings settings) {
        Path location = settings.customizationConfig();
        if (location == null) {
            return CustomizationConfig.create();
        }
        if (!Files.exists(location)) {
            throw new IllegalArgumentException(String.format(
                "The configured '%s' does not exist: %s",
                AwsSdkJavaCodegenSettings.CUSTOMIZATION_CONFIG, location.toAbsolutePath()));
        }
        CustomizationConfig config = ModelLoaderUtils.loadModel(CustomizationConfig.class, location.toFile());
        config.resolveProtocolTestSkips(location.toAbsolutePath().getParent());
        return config;
    }

    private void registerGeneratedFiles(FileManifest manifest, Path base) {
        if (!Files.isDirectory(base)) {
            return;
        }
        try (Stream<Path> files = Files.walk(base)) {
            files.filter(Files::isRegularFile).forEach(manifest::addFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to register generated files with the manifest", e);
        }
    }
}
