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

package software.amazon.awssdk.tools.paritymanifest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

class ManifestJsonTest {
    @Test
    void roundTripsAndIsByteReproducible(@TempDir Path temp) throws Exception {
        ParityManifest manifest = Entries.sample();
        byte[] bytes = ManifestJson.write(manifest);
        ParityManifest read = ManifestJson.read(new String(bytes, StandardCharsets.UTF_8));
        assertEquals(manifest, read);
        assertArrayEquals(bytes, ManifestJson.write(read));

        // Entry input order does not change the output.
        List<ManifestEntry> shuffled = new ArrayList<>(manifest.entries());
        Collections.shuffle(shuffled, new Random(7));
        assertArrayEquals(bytes, ManifestJson.write(new ParityManifest(manifest.baseline(), manifest.candidate(),
                                                                       shuffled)));

        Path file = temp.resolve("out/parity-manifest.json");
        ManifestJson.write(manifest, file);
        assertArrayEquals(bytes, Files.readAllBytes(file));
        assertEquals(manifest, ManifestJson.read(file));
    }

    @Test
    void outputIsUtf8SortedAndNewlineTerminated() {
        String json = new String(ManifestJson.write(Entries.sample()), StandardCharsets.UTF_8);
        assertTrue(json.endsWith("}\n") && !json.endsWith("\n\n"), json);
        assertTrue(json.contains("Délai exponentiel \u2014 unchanged"), "non-ASCII text is written as UTF-8");
        ObjectNode root = Node.parse(json).expectObjectNode();
        assertEquals(ManifestJson.TOP_MEMBERS, keys(root));
        List<String> ids = root.expectArrayMember("entries").getElementsAs(ObjectNode.class).stream()
                               .map(e -> e.expectStringMember("id").getValue()).toList();
        assertEquals(new ArrayList<>(new TreeSet<>(ids)), ids);
        for (ObjectNode entry : root.expectArrayMember("entries").getElementsAs(ObjectNode.class)) {
            assertEquals(ManifestJson.ENTRY_MEMBERS, keys(entry));
        }
        ObjectNode summary = root.expectObjectMember("summary");
        assertEquals(6, summary.expectNumberMember("PRESERVED").getValue().intValue());
        assertEquals(1, summary.expectNumberMember("ADAPTED").getValue().intValue());
        assertEquals(1, summary.expectNumberMember("EXCLUDED").getValue().intValue());
    }

    @Test
    void readerReportsEveryMalformedMemberAtOnce() {
        String json = new String(ManifestJson.write(Entries.sample()), StandardCharsets.UTF_8)
            .replace("\"binary\": true", "\"binary\": false")
            .replace("\"evidence\": \"AdaptedTest\"", "\"evidence\": \"Other\"")
            .replace("\"EXCLUDED\": 1", "\"EXCLUDED\": 4")
            .replace("\"schemaVersion\": 1,", "\"schemaVersion\": 1, \"extra\": 1,");
        ManifestValidationException e = assertThrows(ManifestValidationException.class, () -> ManifestJson.read(json));
        List<String> problems = e.problems().stream().map(ManifestProblem::toString).toList();
        assertEquals(4, problems.size(), problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.contains("DynamoDbClient#getItem") && p.contains("'binary'")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("[api:software.amazon.awssdk.services.dynamodb"
                                                              + ".DynamoDbClient]") && p.contains("'evidence'")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("[summary]") && p.contains("'EXCLUDED'")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("[$]") && p.contains("'extra'")));
    }

    @Test
    void readerAppliesValidation() {
        String json = new String(ManifestJson.write(Entries.sample()), StandardCharsets.UTF_8)
            .replace("\"migration\": \"apply recipe X\"", "\"migration\": null");
        ManifestValidationException e = assertThrows(ManifestValidationException.class, () -> ManifestJson.read(json));
        assertEquals(List.of("MISSING_FIELD [api:software.amazon.awssdk.services.dynamodb.DynamoDbClient]: "
                             + "ADAPTED requires migration"),
                     e.problems().stream().map(ManifestProblem::toString).toList());
    }

    @Test
    void schemaDeclaresExactlyTheWrittenMembers() throws Exception {
        ObjectNode schema;
        try (InputStream in = ManifestJson.class.getResourceAsStream(ManifestJson.SCHEMA_RESOURCE)) {
            schema = Node.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)).expectObjectNode();
        }
        assertEquals(Set.copyOf(ManifestJson.TOP_MEMBERS), Set.copyOf(keys(schema.expectObjectMember("properties"))));
        ObjectNode entry = schema.expectObjectMember("$defs").expectObjectMember("entry");
        assertEquals(Set.copyOf(ManifestJson.ENTRY_MEMBERS), Set.copyOf(keys(entry.expectObjectMember("properties"))));
        assertEquals(ManifestJson.ENTRY_MEMBERS, entry.expectArrayMember("required").getElementsAs(
            n -> n.expectStringNode().getValue()));
        ObjectNode baseline = schema.expectObjectMember("properties").expectObjectMember("baseline");
        assertEquals(Set.copyOf(ManifestJson.BASELINE_MEMBERS),
                     Set.copyOf(keys(baseline.expectObjectMember("properties"))));
        List<String> kinds = entry.expectObjectMember("properties").expectObjectMember("kind")
                                  .expectArrayMember("enum").getElementsAs(n -> n.expectStringNode().getValue());
        assertEquals(Set.copyOf(java.util.Arrays.stream(EntryKind.values()).map(Enum::name).toList()),
                     Set.copyOf(kinds));
    }

    private static List<String> keys(ObjectNode node) {
        return List.copyOf(node.getStringMap().keySet());
    }
}
