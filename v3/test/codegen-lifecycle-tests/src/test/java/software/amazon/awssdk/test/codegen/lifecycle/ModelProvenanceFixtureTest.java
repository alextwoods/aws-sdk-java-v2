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

package software.amazon.awssdk.test.codegen.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.metadata.ModelProvenance;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;

/**
 * The fixture service publishes its model provenance and generation manifest as resources of its own artifact, and
 * the generated identity type reads the provenance back with the exact revision and service shape ID.
 */
class ModelProvenanceFixtureTest {

    private static final String SERVICE = "software.amazon.awssdk.test.codegen.lifecycle#CodegenLifecycleFixture";
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.lifecycle";
    private static final String REVISION = System.getProperty("v3.fixture.modelRevision");
    private static final Path MODEL_DIRECTORY = Path.of(System.getProperty("v3.fixture.modelDirectory"));
    private static final Path SOURCE_ROOT = Path.of(System.getProperty("v3.fixture.generatedSourceRoot"));
    private static final Path RESOURCE_ROOT = Path.of(System.getProperty("v3.fixture.generatedResourceRoot"));
    private static final String RESOURCES = "META-INF/aws-sdk-java-v3/services/" + PACKAGE + "/";

    @Test
    void generatedIdentityReadsTheExactProvenanceFromTheModuleArtifact() throws IOException, NoSuchAlgorithmException {
        ModelProvenance provenance = CodegenLifecycleFixture.provenance();

        assertTrue(REVISION.matches("[0-9a-f]{40}"), REVISION);
        assertEquals(SERVICE, provenance.serviceShapeId());
        assertEquals(REVISION, provenance.modelRevision());
        assertEquals(REVISION, CodegenLifecycleFixture.modelRevision());
        assertEquals(ModelProvenance.RevisionKind.GIT_COMMIT, provenance.revisionKind());
        assertEquals(CodegenLifecycleFixture.modelDigest(), provenance.modelDigest());
        assertEquals(List.of(new ModelProvenance.ModelFile(
                         "codegen-lifecycle-fixture.smithy",
                         sha256(Files.readAllBytes(MODEL_DIRECTORY.resolve("codegen-lifecycle-fixture.smithy"))))),
                     provenance.modelFiles());
    }

    @Test
    void metadataResourcesArePackagedWithTheCompiledGeneratedSources() throws URISyntaxException {
        String codeSource = GeneratedServiceInfo.class.getProtectionDomain().getCodeSource().getLocation().toURI()
                                                      .toString();
        for (String name : List.of("model-provenance.json", "generation-manifest.json")) {
            URL resource = GeneratedServiceInfo.class.getResource("/" + RESOURCES + name);
            assertTrue(resource != null, name + " is on the class path");
            assertTrue(resource.toURI().toString().startsWith(codeSource),
                       resource + " comes from the module output " + codeSource);
        }
        assertEquals(RESOURCES + "model-provenance.json", ModelProvenance.resourceName(PACKAGE));
    }

    @Test
    void manifestDigestsEveryGeneratedSourceAndTheProvenance() throws IOException, NoSuchAlgorithmException {
        Document manifest = JsonCodec.builder().build()
                                     .createDeserializer(Files.readAllBytes(
                                         RESOURCE_ROOT.resolve(RESOURCES + "generation-manifest.json")))
                                     .readDocument();

        assertEquals(SERVICE, manifest.getMember("service").asString());
        List<String> expected = new ArrayList<>();
        expected.add("resources " + RESOURCES + "model-provenance.json "
                     + sha256(Files.readAllBytes(RESOURCE_ROOT.resolve(RESOURCES + "model-provenance.json"))));
        for (String source : files(SOURCE_ROOT)) {
            expected.add("sources " + source + " " + sha256(Files.readAllBytes(SOURCE_ROOT.resolve(source))));
        }
        List<String> actual = manifest.getMember("files").asList().stream()
                                      .map(f -> f.getMember("root").asString() + " " + f.getMember("path").asString()
                                                + " " + f.getMember("digest").asString())
                                      .toList();
        assertEquals(expected, actual);

        List<String> operations = manifest.getMember("inventory").getMember("operations").asList().stream()
                                          .map(o -> o.getMember("id").asString() + " -> "
                                                    + o.getMember("descriptor").asString())
                                          .toList();
        String descriptors = PACKAGE + ".internal.operation.";
        assertEquals(List.of("software.amazon.awssdk.test.codegen.lifecycle#GetStatus -> " + descriptors
                             + "GetStatusOperation",
                             "software.amazon.awssdk.test.codegen.lifecycle#GetWidget -> " + descriptors
                             + "GetWidgetOperation",
                             "software.amazon.awssdk.test.codegen.lifecycle#Ping -> " + descriptors + "PingOperation",
                             "software.amazon.awssdk.test.codegen.lifecycle#Upload -> " + descriptors
                             + "UploadOperation"),
                     operations);
    }

    private static List<String> files(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                       .map(p -> root.relativize(p).toString().replace(root.getFileSystem().getSeparator(), "/"))
                       .sorted()
                       .toList();
        }
    }

    private static String sha256(byte[] content) throws NoSuchAlgorithmException {
        return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }
}
