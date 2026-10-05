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

package software.amazon.awssdk.awscodegen.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateMojoTest {
    private static final String REVISION = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";

    @TempDir
    Path temp;

    @Test
    void generatesSourcesAndRegistersCompileSourceRoot() throws Exception {
        GenerateMojo mojo = mojo("jar");
        writeWeatherModel(mojo);

        mojo.execute();

        assertEquals(List.of(mojo.outputDirectory.toPath().toString()), mojo.project.getCompileSourceRoots());
        assertTrue(Files.isRegularFile(
            mojo.outputDirectory.toPath().resolve("example/weather/GeneratedServiceInfo.java")));
        assertEquals(List.of(mojo.resourceOutputDirectory.toPath().toString()),
                     mojo.project.getResources().stream().map(Resource::getDirectory).toList());
        Path provenance = mojo.resourceOutputDirectory.toPath()
                                                      .resolve("META-INF/aws-sdk-java-v3/services/example.weather/"
                                                               + "model-provenance.json");
        assertTrue(Files.readString(provenance).contains("\"revision\": \"" + REVISION + "\""));
        assertTrue(Files.isRegularFile(provenance.resolveSibling("generation-manifest.json")));
    }

    @Test
    void missingModelRevisionFailsTheBuildBeforeGeneration() throws IOException {
        GenerateMojo mojo = mojo("jar");
        writeWeatherModel(mojo);
        mojo.modelRevision = null;

        MojoFailureException e = assertThrows(MojoFailureException.class, mojo::execute);

        assertTrue(e.getMessage().contains("aws.codegen.modelRevision"), e.getMessage());
        assertFalse(Files.exists(mojo.outputDirectory.toPath()));
        assertFalse(Files.exists(mojo.resourceOutputDirectory.toPath()));
    }

    @Test
    void abbreviatedModelRevisionFailsTheBuildWithoutRegisteringOutput() throws IOException {
        GenerateMojo mojo = mojo("jar");
        writeWeatherModel(mojo);
        mojo.modelRevision = REVISION.substring(0, 12);

        MojoFailureException e = assertThrows(MojoFailureException.class, mojo::execute);

        assertTrue(e.getMessage().contains("not an untruncated immutable source revision"), e.getMessage());
        assertTrue(mojo.project.getCompileSourceRoots().isEmpty());
        assertTrue(mojo.project.getResources().isEmpty());
        assertFalse(Files.exists(mojo.resourceOutputDirectory.toPath()));
    }

    @Test
    void legacyInputsFailTheBuildBeforeAnySourceExists() throws IOException {
        GenerateMojo mojo = mojo("jar");
        writeWeatherModel(mojo);
        Files.writeString(mojo.modelDirectory.toPath().resolve("paginators-1.json"),
                          "{\"pagination\": {\"ListCities\": {\"input_token\": \"nextToken\"}}}");

        MojoFailureException e = assertThrows(MojoFailureException.class, mojo::execute);
        assertTrue(e.getMessage().contains("[LEGACY_PAGINATOR_SIDECAR] service example.weather#Weather, "
                                           + "source paginators-1.json, element V2 paginator sidecar format "
                                           + "(paginators-1.json)"), e.getMessage());
        assertTrue(mojo.project.getCompileSourceRoots().isEmpty());
        assertTrue(mojo.project.getResources().isEmpty());
        assertFalse(Files.exists(mojo.outputDirectory.toPath()));
        assertFalse(Files.exists(mojo.resourceOutputDirectory.toPath()));
    }

    @Test
    void thePinnedModelDigestAndRequiredTypesPassWhenTheOutputMatches() throws Exception {
        GenerateMojo pinned = mojo("jar");
        writeWeatherModel(pinned);
        pinned.execute();
        String digest = Files.readString(pinned.resourceOutputDirectory.toPath().resolve(
            "META-INF/aws-sdk-java-v3/services/example.weather/model-provenance.json"))
                             .replaceAll("(?s).*\"digest\": \"(sha256:[0-9a-f]{64})\",\\s*\"files\".*", "$1");
        GenerateMojo mojo = mojo("jar");
        mojo.modelDigest = digest;
        mojo.requiredTypes = "example.weather.WeatherClient,\n example.weather.WeatherAsyncClient";
        mojo.execute();
        assertEquals(List.of(mojo.outputDirectory.toPath().toString()), mojo.project.getCompileSourceRoots());
    }

    @Test
    void aModelThatDiffersFromThePinnedDigestIsAProvenanceMismatch() throws IOException {
        GenerateMojo mojo = mojo("jar");
        writeWeatherModel(mojo);
        mojo.modelDigest = "sha256:" + "0".repeat(64);
        MojoFailureException e = assertThrows(MojoFailureException.class, mojo::execute);
        assertTrue(e.getMessage().contains("provenance mismatch"), e.getMessage());
        assertTrue(e.getMessage().contains("pinned to model digest sha256:" + "0".repeat(64) + " of revision "
                                           + REVISION), e.getMessage());
        assertTrue(mojo.project.getCompileSourceRoots().isEmpty());
        assertTrue(mojo.project.getResources().isEmpty());
    }

    @Test
    void aMissingRequiredTypeFailsTheBuildWithoutRegisteringOutput() throws IOException {
        GenerateMojo mojo = mojo("jar");
        writeWeatherModel(mojo);
        mojo.requiredTypes = "example.weather.WeatherClient example.weather.WeatherV2Client";
        MojoFailureException e = assertThrows(MojoFailureException.class, mojo::execute);
        assertTrue(e.getMessage().contains("missing generated type example.weather.WeatherV2Client"), e.getMessage());
        assertFalse(e.getMessage().contains("missing generated type example.weather.WeatherClient"), e.getMessage());
        assertTrue(mojo.project.getCompileSourceRoots().isEmpty());
        assertTrue(mojo.project.getResources().isEmpty());
    }

    @Test
    void skipsPomPackagedProjects() throws Exception {
        GenerateMojo mojo = mojo("pom");
        mojo.service = null;
        mojo.packageName = null;

        mojo.execute();

        assertTrue(mojo.project.getCompileSourceRoots().isEmpty());
        assertFalse(Files.exists(mojo.outputDirectory.toPath()));
    }

    @Test
    void missingServiceConfigurationFailsTheBuild() throws IOException {
        GenerateMojo mojo = mojo("jar");
        mojo.service = " ";

        MojoFailureException e = assertThrows(MojoFailureException.class, mojo::execute);
        assertTrue(e.getMessage().contains("aws.codegen.service"), e.getMessage());
    }

    @Test
    void generatorFailuresFailTheBuildWithoutRegisteringSources() throws IOException {
        GenerateMojo mojo = mojo("jar");

        MojoFailureException e = assertThrows(MojoFailureException.class, mojo::execute);
        assertTrue(e.getMessage().contains("No Smithy model files"), e.getMessage());
        assertTrue(mojo.project.getCompileSourceRoots().isEmpty());
    }

    private static void writeWeatherModel(GenerateMojo mojo) throws IOException {
        Files.writeString(mojo.modelDirectory.toPath().resolve("weather.smithy"),
                          "$version: \"2\"\nnamespace example.weather\n@aws.protocols#awsJson1_0\n@auth([])\n"
                          + "service Weather { version: \"1\" }\n");
    }

    private GenerateMojo mojo(String packaging) throws IOException {
        Model model = new Model();
        model.setArtifactId("weather");
        model.setPackaging(packaging);
        GenerateMojo mojo = new GenerateMojo();
        mojo.project = new MavenProject(model);
        mojo.service = "example.weather#Weather";
        mojo.packageName = "example.weather";
        mojo.modelDirectory = Files.createDirectories(temp.resolve("src/main/smithy")).toFile();
        mojo.outputDirectory = temp.resolve("target/generated-sources/aws-codegen").toFile();
        mojo.resourceOutputDirectory = temp.resolve("target/generated-resources/aws-codegen").toFile();
        mojo.modelRevision = REVISION;
        return mojo;
    }
}
