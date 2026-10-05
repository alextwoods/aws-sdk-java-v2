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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link StagedFileManifest#publishTogether}: the source and resource trees of a service are published as a unit. */
class StagedPublicationTest {

    @TempDir
    Path temp;

    @Test
    void publishesEveryTreeAndLeavesNoStagingDirectories() throws IOException {
        StagedFileManifest sources = manifest("sources", "a/A.java", "class A {}");
        StagedFileManifest resources = manifest("resources", "META-INF/x.json", "{}\n");
        Files.createDirectories(temp.resolve("resources/stale"));
        Files.writeString(temp.resolve("resources/stale/old.json"), "old");

        StagedFileManifest.publishTogether(List.of(sources, resources));

        assertEquals("class A {}", Files.readString(temp.resolve("sources/a/A.java")));
        assertEquals("{}\n", Files.readString(temp.resolve("resources/META-INF/x.json")));
        assertFalse(Files.exists(temp.resolve("resources/stale")));
        assertEquals(List.of("resources", "sources"), children(temp));
    }

    @Test
    void aTreeThatCannotBeStagedLeavesEveryPublishedTreeUntouched() throws IOException {
        Files.createDirectories(temp.resolve("sources"));
        Files.writeString(temp.resolve("sources/Existing.java"), "class Existing {}");
        // The resource tree's parent is a regular file, so its staging directory cannot be created.
        Files.writeString(temp.resolve("blocked"), "not a directory");
        StagedFileManifest sources = manifest("sources", "a/A.java", "class A {}");
        StagedFileManifest resources = manifest("blocked/resources", "META-INF/x.json", "{}\n");

        AwsCodegenException e = assertThrows(AwsCodegenException.class,
                                             () -> StagedFileManifest.publishTogether(List.of(sources, resources)));

        assertTrue(e.getMessage().contains("Unable to publish generated output"), e.getMessage());
        assertEquals("class Existing {}", Files.readString(temp.resolve("sources/Existing.java")));
        assertFalse(Files.exists(temp.resolve("sources/a")));
        assertEquals(List.of("blocked", "sources"), children(temp));
    }

    @Test
    void overlappingRootsAreRejectedBeforeAnythingIsWritten() {
        StagedFileManifest outer = manifest("out", "A.java", "class A {}");
        StagedFileManifest inner = manifest("out/resources", "x.json", "{}");

        assertThrows(AwsCodegenException.class, () -> StagedFileManifest.publishTogether(List.of(outer, inner)));
        assertFalse(Files.exists(temp.resolve("out")));
        assertThrows(AwsCodegenException.class,
                     () -> new AwsCodegenRequest("example#Service", "example", temp, temp.resolve("out"),
                                                 temp.resolve("out/resources"), null));
    }

    private StagedFileManifest manifest(String root, String file, String content) {
        StagedFileManifest manifest = new StagedFileManifest(temp.resolve(root));
        manifest.writeFile(file, content);
        return manifest;
    }

    private static List<String> children(Path directory) throws IOException {
        try (Stream<Path> list = Files.list(directory)) {
            return list.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }
}
