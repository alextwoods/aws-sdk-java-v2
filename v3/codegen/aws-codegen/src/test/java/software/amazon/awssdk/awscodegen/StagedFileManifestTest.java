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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.model.node.Node;

class StagedFileManifestTest {

    @TempDir
    Path temp;

    @Test
    void stagesInMemoryUntilPublished() throws IOException {
        Path output = temp.resolve("out");
        StagedFileManifest manifest = new StagedFileManifest(output);

        manifest.writeFile("b/B.java", "class B {}");
        manifest.writeFile(Path.of("a.bin"), new ByteArrayInputStream(new byte[] {1, 2, 3}));
        manifest.writeJson("META-INF/x.json", Node.objectNode().withMember("k", "v"));
        manifest.writeFile(output.resolve("c/C.java"), "class C {}");

        assertFalse(Files.exists(output));
        assertEquals(List.of(Path.of("META-INF/x.json"), Path.of("a.bin"), Path.of("b/B.java"), Path.of("c/C.java")),
                     manifest.relativeFiles());
        assertEquals(List.of(output.resolve("META-INF/x.json"), output.resolve("a.bin"), output.resolve("b/B.java"),
                             output.resolve("c/C.java")),
                     List.copyOf(manifest.getFiles()));
        assertTrue(manifest.hasFile("b/B.java"));
        assertNull(manifest.content(Path.of("missing")));

        manifest.publish();

        assertEquals("class B {}", Files.readString(output.resolve("b/B.java")));
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(output.resolve("a.bin")));
        assertEquals("{\n    \"k\": \"v\"\n}\n", Files.readString(output.resolve("META-INF/x.json")));
        assertEquals(List.of("out"), children(temp));
    }

    @Test
    void publishReplacesThePreviousTreeExactly() throws IOException {
        Path output = Files.createDirectories(temp.resolve("out/old"));
        Files.writeString(output.resolve("Old.java"), "old");
        StagedFileManifest manifest = new StagedFileManifest(temp.resolve("out"));
        manifest.writeFile("New.java", "new");

        manifest.publish();

        try (Stream<Path> files = Files.walk(temp.resolve("out"))) {
            assertEquals(List.of(temp.resolve("out/New.java")), files.filter(Files::isRegularFile).toList());
        }
        assertEquals(List.of("out"), children(temp));
    }

    @Test
    void publishingAnEmptyManifestLeavesAnEmptyTree() throws IOException {
        StagedFileManifest manifest = new StagedFileManifest(temp.resolve("out"));
        manifest.publish();
        assertTrue(Files.isDirectory(temp.resolve("out")));
        assertEquals(List.of(), children(temp.resolve("out")));
    }

    @Test
    void rejectsPathsOutsideTheBaseDirectory() {
        StagedFileManifest manifest = new StagedFileManifest(temp.resolve("out"));
        for (String path : List.of("../escape.java", "a/../../escape.java", "", ".")) {
            AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> manifest.writeFile(path, "x"), path);
            assertTrue(e.getMessage().contains("not inside the generated source root"), e.getMessage());
        }
        assertThrows(AwsCodegenException.class, () -> manifest.writeFile(temp.resolve("elsewhere.java"), "x"));
        assertEquals(List.of(), manifest.relativeFiles());
    }

    @Test
    void rejectsSecondWriteToTheSamePath() {
        StagedFileManifest manifest = new StagedFileManifest(temp.resolve("out"));
        manifest.writeFile("a/A.java", "first");
        AwsCodegenException e = assertThrows(AwsCodegenException.class,
                                             () -> manifest.writeFile("a/./A.java", "second"));
        assertTrue(e.getMessage().contains("a/A.java was written more than once"), e.getMessage());
        assertEquals("first", new String(manifest.content(Path.of("a/A.java")), StandardCharsets.UTF_8));
    }

    @Test
    void addFileOnlyAcceptsStagedFiles() {
        StagedFileManifest manifest = new StagedFileManifest(temp.resolve("out"));
        manifest.writeFile("a/A.java", "a");
        assertEquals(temp.resolve("out/a/A.java").toAbsolutePath(), manifest.addFile(Path.of("a/A.java")));
        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> manifest.addFile(Path.of("b/B.java")));
        assertTrue(e.getMessage().contains("not written through the generation FileManifest"), e.getMessage());
    }

    private static List<String> children(Path directory) throws IOException {
        try (Stream<Path> children = Files.list(directory)) {
            return children.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }
}
