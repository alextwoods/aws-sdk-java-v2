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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.REQUEST;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.SERVICE;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.containsFileEndingWith;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.project;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.write;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.pipeline.CodegenPipelineException;

/**
 * Covers the {@link GenerationMojo} routes that run the Codegen_Pipeline: a C2J model, and a native Smithy model in
 * {@code model/}.
 */
class GenerationMojoPipelineTest {

    private static final String C2J_MODEL =
        "{\n"
        + "  \"version\": \"2.0\",\n"
        + "  \"metadata\": {\n"
        + "    \"apiVersion\": \"2024-01-01\", \"endpointPrefix\": \"thing\", \"jsonVersion\": \"1.1\",\n"
        + "    \"protocol\": \"json\", \"protocols\": [\"json\"], \"serviceFullName\": \"Thing Service\",\n"
        + "    \"serviceId\": \"Thing\", \"signatureVersion\": \"v4\", \"targetPrefix\": \"ThingService\",\n"
        + "    \"uid\": \"thing-2024-01-01\"\n"
        + "  },\n"
        + "  \"operations\": {\n"
        + "    \"GetThing\": { \"name\": \"GetThing\", \"http\": { \"method\": \"POST\", \"requestUri\": \"/\" },\n"
        + "      \"input\": { \"shape\": \"GetThingRequest\" }, \"output\": { \"shape\": \"GetThingResponse\" } }\n"
        + "  },\n"
        + "  \"shapes\": {\n"
        + "    \"GetThingRequest\": { \"type\": \"structure\", \"members\": { \"Name\": { \"shape\": \"String\" } } },\n"
        + "    \"GetThingResponse\": { \"type\": \"structure\", \"members\": { \"Value\": { \"shape\": \"String\" } } },\n"
        + "    \"String\": { \"type\": \"string\" }\n"
        + "  }\n"
        + "}\n";

    @TempDir
    Path moduleDir;

    @Test
    void c2jModel_isGeneratedThroughThePipeline() throws Exception {
        writeC2jModel();
        MavenProject project = project(moduleDir);

        mojo(project).execute();

        Path sources = moduleDir.resolve("target/generated-sources/sdk");
        assertThat(containsFileEndingWith(sources, "ThingClient.java")).isTrue();
        assertThat(project.getCompileSourceRoots()).contains(sources.toAbsolutePath().toString());
        assertThat(moduleDir.resolve("target/c2j-conversion-gaps/thing-module.json")).exists();
        assertThat(moduleDir.resolve("target/codegen-staging")).doesNotExist();
    }

    @Test
    void nativeSmithyModel_takesPrecedenceOverTheC2jModel() throws Exception {
        writeC2jModel();
        Files.createDirectories(moduleDir.resolve("model"));
        write(moduleDir.resolve("model/main.smithy"), SERVICE + REQUEST);

        mojo(project(moduleDir)).execute();

        Path sources = moduleDir.resolve("target/generated-sources/sdk");
        assertThat(containsFileEndingWith(sources, "DemoClient.java")).isTrue();
        assertThat(containsFileEndingWith(sources, "ThingClient.java")).isFalse();
        assertThat(moduleDir.resolve("target/c2j-conversion-gaps")).doesNotExist();
    }

    @Test
    void invalidNativeSmithyModel_failsTheBuild_andGeneratesNothing() throws Exception {
        Files.createDirectories(moduleDir.resolve("model"));
        write(moduleDir.resolve("model/main.smithy"), SERVICE + "structure OpRequest { name: NoSuchShape }\n");

        assertThatThrownBy(() -> mojo(project(moduleDir)).execute())
            .isInstanceOf(MojoExecutionException.class)
            .hasCauseInstanceOf(CodegenPipelineException.class)
            .hasMessageContaining("demo#OpRequest$name");
        assertThat(moduleDir.resolve("target/generated-sources")).doesNotExist();
    }

    @Test
    void smithyValidationSuppressions_applyOnlyWhenTheModuleAllowsThem() throws Exception {
        // "Thing Service" is a DANGER SdkServiceId event.
        writeC2jModel(C2J_MODEL.replace("\"serviceId\": \"Thing\"", "\"serviceId\": \"Thing Service\""));
        write(moduleDir.resolve("src/main/resources/codegen-resources/customization.config"),
              "{\"smithyValidationSuppressions\": [{\"id\": \"SdkServiceId\", \"reason\": \"A test service ID.\"}]}");

        assertThatThrownBy(() -> mojo(project(moduleDir)).execute())
            .isInstanceOf(MojoExecutionException.class)
            .hasMessageContaining("does not allow them");

        GenerationMojo allowing = mojo(project(moduleDir));
        setField(allowing, "allowSmithyValidationSuppressions", true);
        allowing.execute();
        assertThat(containsFileEndingWith(moduleDir.resolve("target/generated-sources/sdk"), "Client.java")).isTrue();
    }

    @Test
    void serviceModule_cannotAllowSmithyValidationSuppressions() throws Exception {
        writeC2jModel(C2J_MODEL);
        MavenProject project = project(moduleDir);
        Parent services = new Parent();
        services.setArtifactId("services");
        project.getModel().setParent(services);
        GenerationMojo mojo = mojo(project);
        setField(mojo, "allowSmithyValidationSuppressions", true);

        assertThatThrownBy(mojo::execute)
            .isInstanceOf(MojoExecutionException.class)
            .hasMessageContaining("a service module");
        assertThat(moduleDir.resolve("target/generated-sources")).doesNotExist();
    }

    private void writeC2jModel() throws Exception {
        writeC2jModel(C2J_MODEL);
    }

    private void writeC2jModel(String model) throws Exception {
        Path resources = moduleDir.resolve("src/main/resources/codegen-resources");
        Files.createDirectories(resources);
        write(resources.resolve("service-2.json"), model);
    }

    private GenerationMojo mojo(MavenProject project) throws Exception {
        project.setArtifactId("thing-module");
        GenerationMojo mojo = new GenerationMojo();
        // Maven injects these private @Parameter fields at runtime.
        setField(mojo, "project", project);
        setField(mojo, "outputDirectory", moduleDir.resolve("target").toString());
        setField(mojo, "codeGenResources", moduleDir.resolve("src/main/resources/codegen-resources").toFile());
        setField(mojo, "smithyModelDirectory", moduleDir.resolve("model").toFile());
        return mojo;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
