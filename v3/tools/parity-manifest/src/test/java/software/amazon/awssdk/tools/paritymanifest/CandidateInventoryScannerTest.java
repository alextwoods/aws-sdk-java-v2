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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.tools.paritymanifest.CandidateArtifacts.DEMO;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.tools.paritymanifest.CandidateInventory.CandidateArtifact;

/** What a release candidate ships, scanned from compiled class directories and jars. */
class CandidateInventoryScannerTest {
    private static final Set<String> SHIPPED = Set.of(
        "artifact:software.amazon.awssdk:demo", "service:demo", "http-mode:demo:sync",
        "api:" + DEMO + ".DemoClient", "api:" + DEMO + ".DemoClient#getThing(Ljava/lang/String;)Ljava/lang/String;",
        "behavior:checksum.request-calculation", "config:env:AWS_REGION",
        "api:" + DEMO + ".Declared#member()V", "config:sysprop:aws.region");

    @TempDir
    Path temp;

    @Test
    void scansDirectoriesAndJarsIntoTheSameInventory() throws Exception {
        Path classes = CandidateArtifacts.service(temp.resolve("classes"));
        Path jar = CandidateArtifacts.jar(classes, temp.resolve("demo.jar"));

        for (Path root : List.of(classes, jar)) {
            CandidateInventory inventory = CandidateInventoryScanner.scan(List.of(root),
                                                                          List.of("config:sysprop:aws.region"));
            assertEquals(SHIPPED, inventory.ids(), root.toString());
            assertEquals(Set.of("software.amazon.awssdk:demo"), inventory.owners("service:demo"));
            assertEquals(Set.of(CandidateInventory.DECLARED), inventory.owners("config:sysprop:aws.region"));
            CandidateArtifact artifact = inventory.artifacts().get("software.amazon.awssdk:demo");
            assertEquals(new CandidateArtifact("software.amazon.awssdk:demo", "3.0.0", root.toString(), "demo.module",
                                               true), artifact);
            assertFalse(inventory.ships("http-mode:demo:async"), "a registration without providers ships nothing");
            assertTrue(inventory.shipsMemberNamed("api:" + DEMO + ".DemoClient#getThing(I)V"));
            assertFalse(inventory.shipsMemberNamed("api:" + DEMO + ".DemoClient#getThings(I)V"));
        }
    }

    @Test
    void rejectsUnidentifiableDuplicateAndMalformedInput() throws Exception {
        Path classes = CandidateArtifacts.service(temp.resolve("classes"));
        assertThrows(IllegalArgumentException.class,
                     () -> CandidateInventoryScanner.scan(List.of(classes, CandidateArtifacts.jar(
                         classes, temp.resolve("again.jar"))), List.of()));

        Path anonymous = temp.resolve("anonymous");
        CandidateArtifacts.write(anonymous, "README", "no identity");
        assertThrows(IllegalArgumentException.class, () -> CandidateInventoryScanner.scan(List.of(anonymous),
                                                                                          List.of()));
        assertThrows(IllegalArgumentException.class, () -> CandidateInventoryScanner.scan(List.of(),
                                                                                          List.of("not an id")));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                                  () -> CandidateInventoryScanner.readIdList("service:s3\nbad id\n",
                                                                                             "ids.txt"));
        assertTrue(e.getMessage().startsWith("ids.txt:2:"), e.getMessage());
    }
}
