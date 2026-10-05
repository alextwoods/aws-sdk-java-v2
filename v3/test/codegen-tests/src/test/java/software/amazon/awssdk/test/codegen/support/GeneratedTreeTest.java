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

package software.amazon.awssdk.test.codegen.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneratedTreeTest {

    @TempDir
    Path temp;

    @Test
    void readsRelativePortablePathsAndIgnoresDirectories() throws IOException {
        Path root = Files.createDirectories(temp.resolve("root/a/empty"));
        Files.writeString(temp.resolve("root/a/A.java"), "a");
        Files.writeString(temp.resolve("root/b.txt"), "b");

        GeneratedTree tree = GeneratedTree.read(temp.resolve("root"));

        assertEquals(Set.of("a/A.java", "b.txt"), tree.paths());
        assertEquals("a", tree.text("a/A.java"));
        assertTrue(GeneratedTree.read(temp.resolve("absent")).isEmpty());
        assertTrue(Files.isDirectory(root));
    }

    @Test
    void identicalTreesHaveNoDifference() {
        GeneratedTree a = tree(Map.of("x", "1", "y/z", "2"));
        GeneratedTree b = tree(Map.of("y/z", "2", "x", "1"));
        assertTrue(a.compare(b).isEmpty());
        GeneratedTree.assertByteIdentical(a, b);
    }

    @Test
    void reportsEveryMissingExtraAndDifferingFile() {
        GeneratedTree left = tree(Map.of("same", "s", "changed", "line1\nline2\n", "gone", "g"));
        GeneratedTree right = tree(Map.of("same", "s", "changed", "line1\nLINE2\n", "added", "a"));

        GeneratedTree.Difference difference = left.compare(right);

        assertEquals(Set.of("gone"), difference.onlyLeft());
        assertEquals(Set.of("added"), difference.onlyRight());
        assertEquals(List.of(new GeneratedTree.FileDifference("changed", 6, 2, "line2", "LINE2")),
                     difference.differing());
        AssertionError error = assertThrows(AssertionError.class,
                                            () -> GeneratedTree.assertByteIdentical(left, right));
        assertTrue(error.getMessage().contains("only in left:  gone")
                   && error.getMessage().contains("only in right: added")
                   && error.getMessage().contains("changed at byte 6 (line 2)"), error.getMessage());
    }

    @Test
    void filtersByPath() {
        GeneratedTree tree = tree(Map.of("a/A.java", "a", "b/B.java", "b"));
        assertEquals(Set.of("a/A.java"), tree.filter(p -> p.startsWith("a/")).paths());
    }

    private static GeneratedTree tree(Map<String, String> files) {
        Map<String, byte[]> bytes = new TreeMap<>();
        files.forEach((path, text) -> bytes.put(path, text.getBytes(StandardCharsets.UTF_8)));
        return GeneratedTree.of(bytes);
    }
}
