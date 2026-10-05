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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Model provenance and generation manifest resources (Requirements 2.12, 22.2): schema conformance, exact revision
 * and service shape ID, file digests, generator/JDK/configuration identity, semantic inventory, and reproducibility.
 */
class GenerationManifestTest {

    private static final String SERVICE = "example.weather.v2006#Weather_2006";
    private static final String PACKAGE = "software.amazon.awssdk.services.weather";
    private static final String REVISION = "a3f1c9b2d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f";
    private static final String RESOURCES = "META-INF/aws-sdk-java-v3/services/" + PACKAGE + "/";
    private static final Path PROVENANCE = Path.of(RESOURCES + "model-provenance.json");
    private static final Path MANIFEST = Path.of(RESOURCES + "generation-manifest.json");
    private static final String OPERATIONS = PACKAGE + ".internal.operation.";

    private static final JsonSchemaSubset PROVENANCE_SCHEMA =
        JsonSchemaSubset.load(GenerationManifestGenerator.PROVENANCE_SCHEMA);
    private static final JsonSchemaSubset MANIFEST_SCHEMA =
        JsonSchemaSubset.load(GenerationManifestGenerator.MANIFEST_SCHEMA);

    @TempDir
    Path temp;

    @Test
    void publishesExactlyTheProvenanceAndManifestIntoTheResourceRoot() throws IOException {
        AwsCodegenResult result = generate(temp.resolve("a"));

        assertEquals(temp.resolve("a/resources"), result.resourceRoot());
        assertEquals(List.of(MANIFEST, PROVENANCE), result.generatedResources());
        assertEquals(REVISION, result.modelRevision());
        assertEquals(List.of(), filesUnder(result.sourceRoot(), "META-INF"),
                     "generated resources are never published into the source root");
    }

    @Test
    void resourcesConformToTheirSchemas() throws IOException {
        AwsCodegenResult result = generate(temp.resolve("a"));

        assertEquals(List.of(), PROVENANCE_SCHEMA.validate(read(result, PROVENANCE)));
        assertEquals(List.of(), MANIFEST_SCHEMA.validate(read(result, MANIFEST)));
    }

    @Test
    void provenanceRecordsTheExactRevisionServiceIdAndModelFiles() throws IOException {
        Path models = writeModels(temp.resolve("model"));
        AwsCodegenResult result = generate(models, temp.resolve("out"), REVISION);

        ObjectNode provenance = Node.parse(read(result, PROVENANCE)).expectObjectNode();
        assertEquals(SERVICE, provenance.expectStringMember("service").getValue());
        ObjectNode model = provenance.expectObjectMember("model");
        assertEquals(REVISION, model.expectStringMember("revision").getValue());
        assertEquals("git-commit", model.expectStringMember("revisionKind").getValue());
        assertEquals(result.modelDigest(), model.expectStringMember("digest").getValue());
        assertEquals(List.of(file("nested/operations.smithy", sha256(models.resolve("nested/operations.smithy"))),
                             file("weather.smithy", sha256(models.resolve("weather.smithy")))),
                     model.expectArrayMember("files").getElements().stream()
                          .map(n -> file(n.expectObjectNode().expectStringMember("path").getValue(),
                                         n.expectObjectNode().expectStringMember("digest").getValue()))
                          .toList());

        String info = Files.readString(result.sourceRoot().resolve(PACKAGE.replace('.', '/')
                                                                   + "/GeneratedServiceInfo.java"));
        assertTrue(info.contains("static final String MODEL_REVISION = \"" + REVISION + "\";"), info);
        assertTrue(info.contains("static final String SERVICE_SHAPE_ID = \"" + SERVICE + "\";"), info);
    }

    @Test
    void sha1CommitRevisionsAreRecordedVerbatim() throws IOException {
        String sha1 = "0000000000000000000000000000000000000001";
        AwsCodegenResult result = generate(writeModels(temp.resolve("model")), temp.resolve("out"), sha1);

        assertEquals(sha1, Node.parse(read(result, PROVENANCE)).expectObjectNode().expectObjectMember("model")
                               .expectStringMember("revision").getValue());
    }

    @Test
    void unpinnedGenerationRecordsTheModelDigestAsItsRevision() throws IOException {
        AwsCodegenResult result = AwsCodegen.generate(
            new AwsCodegenRequest(SERVICE, PACKAGE, writeModels(temp.resolve("model")), temp.resolve("out")),
            List.of());

        assertEquals(temp.resolve("out-resources"), result.resourceRoot());
        ObjectNode model = Node.parse(read(result, PROVENANCE)).expectObjectNode().expectObjectMember("model");
        assertEquals("model-digest", model.expectStringMember("revisionKind").getValue());
        assertEquals(result.modelDigest(), model.expectStringMember("revision").getValue());
        assertEquals(result.modelDigest(), result.modelRevision());
        assertEquals(List.of(), PROVENANCE_SCHEMA.validate(read(result, PROVENANCE)));
    }

    @Test
    void truncatedOrNormalizedRevisionsAreRejectedBeforeAnythingIsPublished() throws IOException {
        Path models = writeModels(temp.resolve("model"));
        for (String revision : List.of("", REVISION.substring(0, 7), REVISION.substring(0, 39),
                                       REVISION.substring(0, 40) + "0", REVISION.substring(0, 63), REVISION + "0",
                                       REVISION.toUpperCase(), " " + REVISION, "refs/heads/main",
                                       "sha256:" + REVISION)) {
            AwsCodegenException e = assertThrows(AwsCodegenException.class,
                                                 () -> generate(models, temp.resolve("out"), revision), revision);
            assertTrue(e.getMessage().contains("not an untruncated immutable source revision"), e.getMessage());
        }
        try (Stream<Path> children = Files.list(temp)) {
            assertEquals(List.of("model"), children.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    void manifestDigestsEveryOtherGeneratedFile() throws IOException {
        AwsCodegenResult result = generate(temp.resolve("a"));

        List<Map<String, String>> expected = new ArrayList<>();
        expected.add(entry("resources", PROVENANCE, sha256(result.resourceRoot().resolve(PROVENANCE))));
        for (Path source : result.generatedFiles()) {
            expected.add(entry("sources", source, sha256(result.sourceRoot().resolve(source))));
        }
        List<Map<String, String>> actual = Node.parse(read(result, MANIFEST)).expectObjectNode()
                                               .expectArrayMember("files").getElements().stream()
                                               .map(n -> Map.of("root", member(n, "root"),
                                                                "path", member(n, "path"),
                                                                "digest", member(n, "digest")))
                                               .toList();
        assertEquals(expected, actual);
        assertEquals(19, actual.size(), "provenance plus identity type, service metadata, descriptor, operation "
                                    + "table, request, response, service exception, schemas, type registry and "
                                    + "the nine client sources");
    }

    @Test
    void manifestRecordsGeneratorJdkAndConfigurationIdentity() throws IOException {
        ObjectNode manifest = Node.parse(read(generate(temp.resolve("a")), MANIFEST)).expectObjectNode();

        assertEquals(1, manifest.expectNumberMember("formatVersion").getValue().intValue());
        assertEquals(SERVICE, manifest.expectStringMember("service").getValue());
        assertEquals(PROVENANCE.toString().replace('\\', '/'), manifest.expectStringMember("provenance").getValue());

        GeneratorIdentity generator = GeneratorIdentity.current();
        assertEquals(Node.objectNodeBuilder()
                         .withMember("artifactId", "aws-codegen")
                         .withMember("groupId", "software.amazon.awssdk")
                         .withMember("smithyVersion", generator.smithyVersion())
                         .withMember("version", generator.version())
                         .build(),
                     manifest.expectObjectMember("generator"));
        assertTrue(generator.version().startsWith("3."), generator.version());
        assertTrue(generator.smithyVersion().matches("\\d+\\.\\d+\\.\\d+"), generator.smithyVersion());
        assertEquals(Node.objectNodeBuilder()
                         .withMember("specificationVersion", System.getProperty("java.specification.version"))
                         .withMember("vendor", System.getProperty("java.vendor"))
                         .build(),
                     manifest.expectObjectMember("jdk"));

        ObjectNode configuration = manifest.expectObjectMember("configuration");
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("service", SERVICE);
        settings.put("packageName", PACKAGE);
        settings.put("integrations", List.of());
        assertEquals(Node.parse(CanonicalJson.write(settings)), configuration.expectObjectMember("settings"));
        assertEquals(GenerationManifestGenerator.sha256(CanonicalJson.bytes(settings)),
                     configuration.expectStringMember("digest").getValue());
    }

    @Test
    void configurationIdentityIncludesTheIntegrationOrder() throws IOException {
        Path models = writeModels(temp.resolve("model"));
        AwsCodegenResult result = AwsCodegen.generate(request(models, temp.resolve("out"), REVISION),
                                                      List.of(new Noop("aws.b", 0), new Noop("aws.a", 1)));

        ObjectNode configuration = Node.parse(read(result, MANIFEST)).expectObjectNode()
                                       .expectObjectMember("configuration");
        assertEquals(List.of("aws.b", "aws.a"),
                     configuration.expectObjectMember("settings").expectArrayMember("integrations")
                                  .getElementsAs(n -> n.expectStringNode().getValue()));
    }

    @Test
    void manifestRecordsTheRequiredSemanticInventory() throws IOException {
        ObjectNode inventory = Node.parse(read(generate(temp.resolve("a")), MANIFEST)).expectObjectNode()
                                   .expectObjectMember("inventory");

        assertEquals("2006-03-01", inventory.expectStringMember("serviceVersion").getValue());
        assertEquals(List.of("aws.protocols#awsJson1_0"), strings(inventory, "protocols"));
        assertEquals(List.of("aws.auth#sigv4"), strings(inventory, "authSchemes"));
        assertEquals(OPERATIONS + "Weather_2006Operations",
                     inventory.expectStringMember("operationTable").getValue());
        assertEquals(List.of(Map.of("id", "example.weather.v2006#GetForecast",
                                    "descriptor", OPERATIONS + "GetForecastOperation")),
                     inventory.expectArrayMember("operations").getElements().stream()
                              .map(n -> Map.of("id", member(n, "id"), "descriptor", member(n, "descriptor")))
                              .toList());
        assertEquals(List.of("operation example.weather.v2006#GetForecast",
                             "structure example.weather.v2006#GetForecastInput",
                             "structure example.weather.v2006#GetForecastOutput",
                             "service example.weather.v2006#Weather_2006"),
                     inventory.expectArrayMember("shapeClosure").getElements().stream()
                              .map(n -> member(n, "type") + " " + member(n, "id"))
                              .toList());
    }

    @Test
    void resourcesAreByteIdenticalAcrossRunsAndCheckoutLocations() throws IOException {
        AwsCodegenResult a = generate(temp.resolve("a"));
        AwsCodegenResult b = generate(temp.resolve("somewhere/else/entirely"));
        AwsCodegenResult again = generate(temp.resolve("a"));

        assertEquals(a.generatedResources(), b.generatedResources());
        for (Path resource : a.generatedResources()) {
            byte[] content = Files.readAllBytes(a.resourceRoot().resolve(resource));
            assertArrayEquals(content, Files.readAllBytes(b.resourceRoot().resolve(resource)), resource.toString());
            assertArrayEquals(content, Files.readAllBytes(again.resourceRoot().resolve(resource)), resource.toString());
            String text = new String(content, StandardCharsets.UTF_8);
            assertFalse(text.contains(temp.toString()), "resources must not contain absolute paths");
            assertFalse(text.contains("\r"), "resources use \\n line endings");
            // Canonical form: re-encoding the parsed document reproduces the exact bytes (sorted keys, layout).
            assertEquals(text, CanonicalJson.write(toJava(Node.parse(text))));
        }
    }

    @Test
    void schemasRejectNonConformingDocuments() throws IOException {
        String provenance = read(generate(temp.resolve("a")), PROVENANCE);

        assertViolation(PROVENANCE_SCHEMA, provenance.replace(REVISION, REVISION.substring(0, 12)),
                        "/model/revision");
        assertViolation(PROVENANCE_SCHEMA, provenance.replace(REVISION, REVISION.toUpperCase()), "/model/revision");
        assertViolation(PROVENANCE_SCHEMA, provenance.replace("\"formatVersion\": 1", "\"formatVersion\": 2"),
                        "/formatVersion");
        assertViolation(PROVENANCE_SCHEMA, provenance.replace("\"formatVersion\": 1,",
                                                              "\"formatVersion\": 1,\n  \"generatedAt\": \"now\","),
                        "/generatedAt");
        assertViolation(PROVENANCE_SCHEMA, provenance.replace("\"service\": \"" + SERVICE + "\"",
                                                              "\"service\": \"Weather_2006\""), "/service");
        assertViolation(PROVENANCE_SCHEMA, provenance.replace("\"path\": \"weather.smithy\"",
                                                              "\"path\": \"/abs/weather.smithy\""),
                        "/model/files/1/path");
        // A schema relying on a keyword the validator does not implement is refused rather than half-checked.
        assertThrows(IllegalArgumentException.class,
                     () -> JsonSchemaSubset.parse("{\"type\": \"object\", \"oneOf\": []}"));
    }

    private static void assertViolation(JsonSchemaSubset schema, String json, String pointer) {
        List<String> errors = schema.validate(json);
        assertTrue(errors.stream().anyMatch(e -> e.startsWith(pointer + ":")), pointer + " not reported: " + errors);
    }

    private AwsCodegenResult generate(Path root) throws IOException {
        return generate(writeModels(root.resolve("model")), root.resolve("out"), REVISION);
    }

    private static AwsCodegenResult generate(Path models, Path root, String revision) {
        return AwsCodegen.generate(request(models, root, revision), List.of());
    }

    private static AwsCodegenRequest request(Path models, Path root, String revision) {
        return new AwsCodegenRequest(SERVICE, PACKAGE, models, root.resolveSibling("sources"),
                                     root.resolveSibling("resources"), revision);
    }

    private static Path writeModels(Path directory) throws IOException {
        Files.createDirectories(directory.resolve("nested"));
        Files.writeString(directory.resolve("weather.smithy"),
                          "$version: \"2\"\nnamespace example.weather.v2006\n"
                          + "@aws.protocols#awsJson1_0\n@aws.auth#sigv4(name: \"weather\")\n"
                          + "service Weather_2006 { version: \"2006-03-01\", operations: [GetForecast] }\n",
                          StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("nested/operations.smithy"),
                          "$version: \"2\"\nnamespace example.weather.v2006\n"
                          + "operation GetForecast {\n    input := { city: String }\n    output := { summary: String }\n}\n",
                          StandardCharsets.UTF_8);
        return directory;
    }

    private static String read(AwsCodegenResult result, Path resource) throws IOException {
        return Files.readString(result.resourceRoot().resolve(resource), StandardCharsets.UTF_8);
    }

    private static List<String> filesUnder(Path root, String directory) throws IOException {
        Path dir = root.resolve(directory);
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).map(Path::toString).toList();
        }
    }

    private static String sha256(Path file) throws IOException {
        return GenerationManifestGenerator.sha256(Files.readAllBytes(file));
    }

    private static Map<String, String> file(String path, String digest) {
        return Map.of("path", path, "digest", digest);
    }

    private static Map<String, String> entry(String root, Path path, String digest) {
        return Map.of("root", root, "path", StagedFileManifest.portable(path), "digest", digest);
    }

    private static String member(Node node, String name) {
        return node.expectObjectNode().expectStringMember(name).getValue();
    }

    private static List<String> strings(ObjectNode object, String name) {
        return object.expectArrayMember(name).getElementsAs(n -> n.expectStringNode().getValue());
    }

    /** Converts a parsed document to the value types {@link CanonicalJson} accepts. */
    private static Object toJava(Node node) {
        if (node.isObjectNode()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.expectObjectNode().getMembers().forEach((k, v) -> map.put(k.getValue(), toJava(v)));
            return map;
        } else if (node.isArrayNode()) {
            return node.expectArrayNode().getElements().stream().map(GenerationManifestTest::toJava).toList();
        } else if (node.isStringNode()) {
            return node.expectStringNode().getValue();
        } else if (node.isBooleanNode()) {
            return node.expectBooleanNode().getValue();
        }
        // The only numbers in these documents are small format versions.
        return Math.toIntExact(node.expectNumberNode().getValue().longValue());
    }

    private record Noop(String identifier, int order) implements AwsCodegenIntegration {
    }
}
