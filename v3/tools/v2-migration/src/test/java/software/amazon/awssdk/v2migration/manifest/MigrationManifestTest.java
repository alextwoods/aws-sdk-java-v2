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

package software.amazon.awssdk.v2migration.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

class MigrationManifestTest {

    private static final String SAMPLE = "/software/amazon/awssdk/v2migration/manifest/sample-parity-manifest.json";
    private static final Path PUBLISHER = moduleDirectory().resolve("../parity-manifest").normalize();
    private static final String REGION = "api:software.amazon.awssdk.regions.Region";
    private static final String RETRY = "api:software.amazon.awssdk.core.retry.RetryPolicy#defaultRetryPolicy()"
                                        + "Lsoftware/amazon/awssdk/core/retry/RetryPolicy;";

    @Test
    void read_validManifest_indexesEveryEntry() throws Exception {
        MigrationManifest manifest = MigrationManifest.read(sample());

        assertEquals(new ManifestBaseline("2.54.0", "e1ec10e99088686ca5f238aaca21a15b9a7b09d4", "2026-01-15",
                                          "general-availability", "parity-manifest/3.0.0", "3.0.0", null),
                     manifest.baseline());
        assertEquals(5, manifest.size());
        assertEquals(Map.of(Classification.PRESERVED, 3, Classification.ADAPTED, 1, Classification.EXCLUDED, 1),
                     manifest.summary());
        List<String> ids = manifest.entries().stream().map(ManifestEntry::id).toList();
        assertEquals(ids.stream().sorted().toList(), ids);

        ManifestEntry retry = manifest.entry(RETRY).orElseThrow();
        assertEquals(Classification.EXCLUDED, retry.classification());
        assertEquals(MigrationKind.DIAGNOSTIC, retry.migrationKind());
        assertEquals("api:software.amazon.awssdk.retries.StandardRetryStrategy", retry.replacement());
        assertEquals("Use StandardRetryStrategy.", retry.migration());
        assertTrue(retry.deprecated());

        ManifestEntry region = manifest.entry(REGION).orElseThrow();
        assertEquals(List.of("binary", "source"), region.dimensions());
        assertTrue(region.binary());
        assertEquals(List.of("CompileTest", "LinkTest"), region.evidenceIds());
    }

    @Test
    void typeEntry_nestedSourceName_resolvesTheBinaryName() throws Exception {
        MigrationManifest manifest = MigrationManifest.read(sample());

        assertEquals("api:software.amazon.awssdk.services.s3.S3Client$Builder",
                     manifest.typeEntry("software.amazon.awssdk.services.s3.S3Client.Builder").orElseThrow().id());
        assertEquals(REGION, manifest.typeEntry("software.amazon.awssdk.regions.Region").orElseThrow().id());
        assertTrue(manifest.typeEntry("software.amazon.awssdk.regions.Missing").isEmpty());
    }

    @Test
    void members_ownerAndName_returnsEveryOverloadAndNoOtherMember() throws Exception {
        MigrationManifest manifest = MigrationManifest.read(sample());

        assertEquals(List.of(RETRY), manifest.members("software.amazon.awssdk.core.retry.RetryPolicy",
                                                      "defaultRetryPolicy").stream().map(ManifestEntry::id).toList());
        assertEquals(List.of(), manifest.members("software.amazon.awssdk.core.retry.RetryPolicy", "default"));
        assertEquals(List.of(), manifest.members("software.amazon.awssdk.core.retry.Retry", "defaultRetryPolicy"));
    }

    @Test
    void read_entryProblems_reportsEveryProblemAtOnce() throws Exception {
        String json = mutate(sample(), entries -> {
            entries.set(1, entries.get(1).withMember("binary", false));
            entries.set(2, entries.get(2).withMember("migration", "Do something."));
            entries.set(3, entries.get(3).withMember("unexpected", 1));
            entries.set(4, entries.get(4).withMember("kind", "ARTIFACT"));
            entries.set(0, entries.get(0).withMember("deprecated", false));
            return entries;
        });

        ManifestFormatException e = assertThrows(ManifestFormatException.class, () -> MigrationManifest.read(json));

        assertEquals(5, e.problems().size(), e.getMessage());
        assertProblem(e, REGION + ": 'binary' disagrees with dimensions");
        assertProblem(e, "S3Client$Builder: PRESERVED forbids a migration action");
        assertProblem(e, "'unexpected' is not a member of the schema");
        assertProblem(e, "config:env:AWS_REGION: 'id' does not follow the ARTIFACT ID scheme");
        assertProblem(e, RETRY + ": EXCLUDED requires supportEvidence unless the V2 baseline deprecates it");
    }

    @Test
    void read_adaptedWithoutEvidence_isRejected() throws Exception {
        String json = mutate(sample(), entries -> {
            entries.set(3, entries.get(3).withMember("evidenceIds", ArrayNode.arrayNode())
                                     .withMember("evidence", Node.nullNode()));
            return entries;
        });

        ManifestFormatException e = assertThrows(ManifestFormatException.class, () -> MigrationManifest.read(json));

        assertProblem(e, "ADAPTED requires verification evidence");
    }

    @Test
    void read_duplicateIdAndWrongSummary_areRejected() throws Exception {
        String json = mutate(sample(), entries -> {
            entries.add(entries.get(1));
            return entries;
        });

        ManifestFormatException e = assertThrows(ManifestFormatException.class, () -> MigrationManifest.read(json));

        assertProblem(e, REGION + ": the manifest contains this ID more than once");
        assertProblem(e, "summary: 'PRESERVED' must equal the PRESERVED entry count 4");
    }

    @Test
    void read_wrongSchemaVersionOrNotJson_isRejected() throws Exception {
        String wrongVersion = Node.prettyPrintJson(Node.parse(sample()).expectObjectNode().withMember("schemaVersion", 2));

        assertProblem(assertThrows(ManifestFormatException.class, () -> MigrationManifest.read(wrongVersion)),
                      "$: 'schemaVersion' must be 1");
        assertProblem(assertThrows(ManifestFormatException.class, () -> MigrationManifest.read("{not json")),
                      "$: is not JSON");
    }

    /** The reader's member lists and enumerations are exactly those of the publisher's schema in this checkout. */
    @Test
    void readerMembers_matchThePublishedSchema() throws IOException {
        Path schema = PUBLISHER.resolve("src/main/resources/software/amazon/awssdk/tools/paritymanifest/"
                                        + "parity-manifest-1.schema.json");
        ObjectNode root = Node.parse(Files.readString(schema, StandardCharsets.UTF_8)).expectObjectNode();
        ObjectNode properties = root.expectObjectMember("properties");
        ObjectNode entry = root.expectObjectMember("$defs").expectObjectMember("entry");
        ObjectNode entryProperties = entry.expectObjectMember("properties");

        assertEquals(ManifestReader.TOP_MEMBERS, strings(root.expectArrayMember("required")));
        assertEquals(ManifestReader.BASELINE_MEMBERS,
                     strings(properties.expectObjectMember("baseline").expectArrayMember("required")));
        assertEquals(ManifestReader.CANDIDATE_MEMBERS,
                     strings(properties.expectObjectMember("candidate").expectArrayMember("required")));
        assertEquals(ManifestReader.ENTRY_MEMBERS, strings(entry.expectArrayMember("required")));
        assertEquals(ManifestReader.SOURCE_MEMBERS,
                     strings(entryProperties.expectObjectMember("source").expectArrayMember("required")));
        assertEquals(names(EntryKind.values()), strings(entryProperties.expectObjectMember("kind").expectArrayMember("enum")));
        assertEquals(names(Classification.values()),
                     strings(entryProperties.expectObjectMember("classification").expectArrayMember("enum")));
        assertEquals(names(MigrationKind.values()),
                     strings(entryProperties.expectObjectMember("migrationKind").expectArrayMember("enum")));
        assertEquals(ManifestReader.DIMENSIONS, strings(entryProperties.expectObjectMember("dimensions")
                                                                       .expectObjectMember("items")
                                                                       .expectArrayMember("enum")));
        assertEquals(MigrationManifest.SCHEMA_VERSION, properties.expectObjectMember("schemaVersion")
                                                                 .expectNumberMember("const").getValue().intValue());
    }

    /** When the publisher's build has produced the V2 baseline manifest, this reader accepts it. */
    @Test
    void read_publishedBaselineManifest_isAccepted() throws Exception {
        Path baseline = PUBLISHER.resolve("target/parity-manifest/v2-baseline-manifest.json");
        assumeTrue(Files.isRegularFile(baseline), "tools/parity-manifest has not generated " + baseline);

        MigrationManifest manifest = MigrationManifest.read(baseline);

        assertTrue(manifest.size() > 0);
        assertEquals(manifest.size(), manifest.summary().values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(manifest.entry("artifact:software.amazon.awssdk:bom").isPresent());
    }

    private static String sample() throws IOException {
        try (InputStream in = MigrationManifestTest.class.getResourceAsStream(SAMPLE)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String mutate(String json, UnaryOperator<List<ObjectNode>> change) {
        ObjectNode root = Node.parse(json).expectObjectNode();
        List<ObjectNode> entries = new ArrayList<>(root.expectArrayMember("entries").getElementsAs(ObjectNode.class));
        return Node.prettyPrintJson(root.withMember("entries", ArrayNode.fromNodes(new ArrayList<>(change.apply(entries)))));
    }

    private static void assertProblem(ManifestFormatException e, String expected) {
        assertTrue(e.problems().stream().anyMatch(p -> p.contains(expected)),
                   "expected a problem containing '" + expected + "' in " + e.problems());
    }

    private static List<String> strings(ArrayNode array) {
        return array.getElements().stream().filter(Node::isStringNode).map(n -> n.expectStringNode().getValue())
                    .toList();
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private static Path moduleDirectory() {
        String basedir = System.getProperty("basedir");
        return Path.of(basedir == null ? "" : basedir).toAbsolutePath();
    }
}
