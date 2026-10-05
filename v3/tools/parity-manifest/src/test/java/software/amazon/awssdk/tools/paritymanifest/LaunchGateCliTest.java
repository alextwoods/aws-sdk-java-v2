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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.tools.paritymanifest.CandidateArtifacts.DEMO;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The launch gate command line and candidate-tree discovery ({@link CandidateTree}): a built V3 tree whose BOM lists
 * a built and an unbuilt artifact, gated against a manifest written to disk.
 */
class LaunchGateCliTest {
    private static final String CLIENT = "api:" + DEMO + ".DemoClient";

    @TempDir
    Path temp;

    @Test
    void discoversTheCandidateOfABuiltTree() throws Exception {
        Path tree = tree();

        assertEquals(Set.of("software.amazon.awssdk:demo", "software.amazon.awssdk:unbuilt"),
                     CandidateTree.bomArtifacts(tree.resolve("bom/pom.xml")));
        assertEquals(List.of(tree.resolve("services/demo/target/classes")),
                     List.copyOf(CandidateTree.moduleOutputs(tree, CandidateTree.bomArtifacts(
                         tree.resolve("bom/pom.xml"))).values()));
        assertEquals(Set.of("DemoGeneratedApiTest"), CandidateTree.testClasses(tree));
        assertEquals(Set.of("software.amazon.awssdk:bom"), CandidateTree.pomArtifacts(tree));

        // A packaged jar that is not older than the compiled output is the module's output.
        Path classes = tree.resolve("services/demo/target/classes");
        Path jar = CandidateArtifacts.jar(classes, tree.resolve("services/demo/target/demo-3.0.0.jar"));
        assertEquals(List.of(jar), List.copyOf(CandidateTree.moduleOutputs(tree, Set.of("software.amazon.awssdk:demo"))
                                                            .values()));
        Files.setLastModifiedTime(jar, java.nio.file.attribute.FileTime.fromMillis(0));
        assertEquals(List.of(classes), List.copyOf(CandidateTree.moduleOutputs(
            tree, Set.of("software.amazon.awssdk:demo")).values()), "a stale jar is not the output");
    }

    @Test
    void passesBlocksAndRejectsUsageWithItsExitStatus() throws Exception {
        Path tree = tree();
        Entries.B client = Entries.preserved(CLIENT);
        client.v3Artifact = null;
        Entries.B service = Entries.preserved("service:demo");
        service.v3Artifact = null;
        Path hooks = Files.writeString(temp.resolve("hooks.txt"), "DemoGeneratedApiTest " + CLIENT + " service:demo\n");

        // Packaging with the unbuilt BOM artifact: blocked, with both reports written.
        Path manifest = manifest("blocked.json", client.build(), service.build());
        Run blocked = run("--manifest", manifest.toString(), "--candidate-tree", tree.toString(), "--hooks",
                          hooks.toString(), "--report", temp.resolve("r/report.json").toString(),
                          "--report-text", temp.resolve("r/report.txt").toString());
        assertEquals(1, blocked.status, blocked.output);
        assertTrue(blocked.output.contains("CANDIDATE_INCOMPLETE [candidate]: release artifact "
                                           + "software.amazon.awssdk:unbuilt is not part of the candidate"),
                   blocked.output);
        assertTrue(Files.readString(temp.resolve("r/report.json")).contains("\"passed\": false"));
        assertTrue(Files.readString(temp.resolve("r/report.txt")).startsWith("Launch gate packaging: BLOCKED"));

        // The same candidate given as an explicit root passes packaging.
        Run passed = run("--manifest", manifest.toString(), "--candidate",
                         tree.resolve("services/demo/target/classes").toString(), "--hooks", hooks.toString());
        assertEquals(0, passed.status, passed.output);
        assertTrue(passed.output.startsWith("Launch gate packaging: PASSED (0 problem(s))"), passed.output);

        // General availability without candidate evidence is blocked.
        Run ga = run("--manifest", manifest.toString(), "--stage", "general-availability", "--candidate",
                     tree.resolve("services/demo/target/classes").toString(), "--hooks", hooks.toString());
        assertEquals(1, ga.status, ga.output);
        assertTrue(ga.output.contains("MISSING_CANDIDATE_EVIDENCE"), ga.output);

        assertEquals(2, run("--candidate", "x").status);
        assertEquals(2, run("--manifest", manifest.toString(), "--stage", "launch").status);
        assertEquals(2, run("--manifest", temp.resolve("absent.json").toString()).status);
        assertEquals(2, run("--manifest", manifest.toString(), "--manifest", manifest.toString()).status);
    }

    @Test
    void builtinHookRegistryParsesAndBindsOnlyWellFormedEntries() throws Exception {
        EvidenceHooks builtin = LaunchGateCli.builtinHooks();
        assertTrue(builtin.known("V2BytecodeOnV3Test"));
        assertEquals(Set.of("service:dynamodb"), builtin.hooks().get("DynamoDbGeneratedApiTest"));
        assertEquals(Set.of("http-mode:aws-crt-client:async"), builtin.hooks().get("CrtAsyncTckTest"));
    }

    /** A V3 tree: a BOM of two artifacts, of which only {@code demo} is built and has a test class. */
    private Path tree() throws Exception {
        Path tree = temp.resolve("v3");
        CandidateArtifacts.write(tree, "bom/pom.xml", """
            <project><groupId>software.amazon.awssdk</groupId><artifactId>bom</artifactId>
              <dependencyManagement><dependencies>
                <dependency><groupId>${project.groupId}</groupId><artifactId>demo</artifactId></dependency>
                <dependency><groupId>software.amazon.awssdk</groupId><artifactId>unbuilt</artifactId></dependency>
              </dependencies></dependencyManagement></project>
            """);
        CandidateArtifacts.write(tree, "services/demo/pom.xml", """
            <project><parent><groupId>software.amazon.awssdk</groupId><artifactId>services</artifactId></parent>
              <artifactId>demo</artifactId></project>
            """);
        CandidateArtifacts.service(tree.resolve("services/demo/target/classes"));
        CandidateArtifacts.write(tree, "services/demo/src/test/java/x/DemoGeneratedApiTest.java", "class X {}");
        // Build output is never a module, even when it holds a POM of the same artifact.
        CandidateArtifacts.write(tree, "services/demo/target/copy/pom.xml", """
            <project><groupId>software.amazon.awssdk</groupId><artifactId>demo</artifactId></project>
            """);
        CandidateArtifacts.write(tree, "core/unbuilt/pom.xml", """
            <project><groupId>software.amazon.awssdk</groupId><artifactId>unbuilt</artifactId></project>
            """);
        return tree;
    }

    private Path manifest(String name, ManifestEntry... entries) throws Exception {
        Path file = temp.resolve(name);
        ManifestJson.write(new ParityManifest(Entries.BASELINE, new CandidateIdentity("3.0.0", null), List.of(entries)),
                           file);
        return file;
    }

    private record Run(int status, String output) {
    }

    private static Run run(String... args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        int status = LaunchGateCli.run(args, stream, stream);
        return new Run(status, bytes.toString(StandardCharsets.UTF_8));
    }
}
