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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ManifestGeneratorTest {
    private static final String RULES = """
        {"schemaVersion": 1,
         "rules": [
           {"name": "adapted-without-replacement", "match": {"id": "api:a.Adapted"}, "classification": "ADAPTED",
            "rationale": "changed", "differences": ["d"], "migrationKind": "RECIPE", "migration": "m",
            "evidenceIds": ["T"]},
           {"name": "adapted-ok", "match": {"id": "api:a.Ok"}, "classification": "ADAPTED", "rationale": "changed",
            "differences": ["d"], "replacement": "api:v3.Ok", "migrationKind": "RECIPE", "migration": "m",
            "evidenceIds": ["T"]},
           {"name": "unknown-replacement", "match": {"id": "api:a.Gone"}, "classification": "EXCLUDED",
            "rationale": "gone", "replacement": "api:v3.Missing", "migrationKind": "DIAGNOSTIC", "migration": "m",
            "supportEvidence": "deprecated"},
           {"name": "tie-1", "match": {"id": "api:a.T*"}, "classification": "PRESERVED", "dimensions": ["source"],
            "rationale": "r"},
           {"name": "tie-2", "match": {"id": "api:a.*e"}, "classification": "PRESERVED", "dimensions": ["source"],
            "rationale": "r"}
         ],
         "defaults": [
           {"name": "default", "match": {"deprecated": false}, "classification": "PRESERVED",
            "dimensions": ["source"], "rationale": "default"}
         ]}
        """;

    @Test
    void reportsEveryProblemAcrossSourcesAtOnceWithEntryIds() throws Exception {
        InventorySource apis = new StaticInventorySource("apis", List.of(
            Entries.item("api:a.Adapted", EntryKind.PUBLIC_TYPE),
            Entries.item("api:a.Ok", EntryKind.PUBLIC_TYPE),
            Entries.item("api:a.Gone", EntryKind.PUBLIC_TYPE),
            Entries.item("api:a.Tie", EntryKind.PUBLIC_TYPE),
            Entries.item("api:a.Old", EntryKind.PUBLIC_TYPE, true),
            Entries.item("api:a.Shared", EntryKind.PUBLIC_TYPE),
            Entries.item("not an id", EntryKind.BEHAVIOR)));
        InventorySource more = new StaticInventorySource("more", List.of(
            Entries.item("api:a.Shared", EntryKind.PUBLIC_TYPE)));
        InventorySource failing = new InventorySource() {
            @Override
            public String name() {
                return "failing";
            }

            @Override
            public List<InventoryItem> scan(Context context) throws IOException {
                throw new IOException("jar unreadable");
            }
        };
        ManifestGenerator generator = new ManifestGenerator(List.of(more, apis, failing),
                                                            ClassificationRules.parse(RULES));
        ManifestValidationException e = assertThrows(ManifestValidationException.class, () -> generator.generate(
            null, Entries.BASELINE, null, new ManifestValidator.Options(false, java.util.Set.of("api:v3.Ok"))));
        assertEquals(List.of(
            "MISSING_FIELD [api:a.Adapted]: ADAPTED requires replacement (classified by rule "
            + "'adapted-without-replacement')",
            "UNKNOWN_REPLACEMENT [api:a.Gone]: replacement 'api:v3.Missing' is not in the candidate inventory "
            + "(classified by rule 'unknown-replacement')",
            "UNMATCHED [api:a.Old]: no classification rule or default matches this deprecated PUBLIC_TYPE entry",
            "DUPLICATE_ID [api:a.Shared]: reported 2 times, by apis, more",
            "CONFLICTING_RULES [api:a.Tie]: equally specific rules match: tie-1, tie-2",
            "INVALID_ID [not an id]: source apis reported an ID that does not follow the BEHAVIOR ID scheme",
            "INVENTORY [source:failing]: scan failed: java.io.IOException: jar unreadable"),
                     e.problems().stream().map(ManifestProblem::toString).toList());
    }

    @Test
    void requiresAtLeastOneSource() {
        ManifestValidationException e = assertThrows(ManifestValidationException.class, () ->
            new ManifestGenerator(List.of(), ClassificationRules.loadDefault())
                .generate(null, Entries.BASELINE, null, ManifestValidator.Options.DEFAULT));
        assertEquals(ManifestProblem.Code.INVENTORY, e.problems().get(0).code());
    }

    @Test
    void cliWritesReproducibleManifestWithBaselineIdentity(@TempDir Path temp) throws Exception {
        Path repo = temp.resolve("repo");
        Files.createDirectories(repo.resolve(".git/refs/heads"));
        Files.writeString(repo.resolve("pom.xml"), """
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <parent><groupId>p</groupId><artifactId>p</artifactId><version>9.9.9</version></parent>
                <artifactId>aws-sdk-java-pom</artifactId>
                <version>2.55.5</version>
            </project>
            """);
        Files.writeString(repo.resolve(".git/HEAD"), "ref: refs/heads/main\n");
        Files.writeString(repo.resolve(".git/packed-refs"), "# pack-refs\nabc123 refs/heads/main\n");
        InventorySource source = new StaticInventorySource("v2-test", List.of(
            Entries.item("artifact:software.amazon.awssdk:sdk-core", EntryKind.ARTIFACT),
            Entries.item("http-mode:apache-client:sync", EntryKind.HTTP_MODE),
            Entries.item("http-mode:netty-nio-client:async", EntryKind.HTTP_MODE)));
        Path out1 = temp.resolve("a/manifest.json");
        Path out2 = temp.resolve("b/manifest.json");
        String[] base = {"--repository", repo.toString(), "--source", "v2-test", "--feature-freeze-cutoff",
                         "2026-09-01", "--support-status", "GA, supported", "--require-frozen-baseline"};
        assertEquals(0, run(concat(base, "--output", out1.toString()), source));
        assertEquals(0, run(concat(base, "--output", out2.toString()), source));
        assertArrayEquals(Files.readAllBytes(out1), Files.readAllBytes(out2));
        ParityManifest manifest = ManifestJson.read(out1);
        assertEquals("2.55.5", manifest.baseline().v2Version());
        assertEquals("abc123", manifest.baseline().gitRevision());
        assertTrue(manifest.baseline().scannerVersion().startsWith("parity-manifest/3."),
                   manifest.baseline().scannerVersion());
        assertEquals(List.of("artifact:software.amazon.awssdk:sdk-core", "http-mode:apache-client:sync",
                             "http-mode:netty-nio-client:async"),
                     manifest.entries().stream().map(ManifestEntry::id).toList());
    }

    @Test
    void cliFailsOnProblemsAndUsageErrors(@TempDir Path temp) throws Exception {
        Files.writeString(temp.resolve("pom.xml"), "<project><version>2.55.6-SNAPSHOT</version></project>");
        InventorySource source = new StaticInventorySource("v2-test", List.of(
            Entries.item("artifact:software.amazon.awssdk:sdk-core", EntryKind.ARTIFACT)));
        Path out = temp.resolve("manifest.json");
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int status = ParityManifestCli.run(new String[] {"--repository", temp.toString(), "--output", out.toString(),
                                                         "--require-frozen-baseline"},
                                           List.of(source), new PrintStream(new ByteArrayOutputStream()),
                                           new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(1, status);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("not a V2 general-availability release"));
        assertTrue(Files.notExists(out));
        assertEquals(2, run(new String[] {"--repository", temp.toString()}, source));
        assertEquals(2, run(new String[] {"--repository", temp.toString(), "--output", out.toString(), "--source",
                                          "nope"}, source));
    }

    private static int run(String[] args, InventorySource source) {
        PrintStream sink = new PrintStream(new ByteArrayOutputStream());
        return ParityManifestCli.run(args, List.of(source), sink, sink);
    }

    private static String[] concat(String[] base, String... more) {
        String[] all = java.util.Arrays.copyOf(base, base.length + more.length);
        System.arraycopy(more, 0, all, base.length, more.length);
        return all;
    }
}
