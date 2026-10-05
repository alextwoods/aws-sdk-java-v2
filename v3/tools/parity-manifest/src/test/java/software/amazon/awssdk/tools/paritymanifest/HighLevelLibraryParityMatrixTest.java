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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.tools.paritymanifest.HighLevelLibraryMatrix.Library;
import software.amazon.awssdk.tools.paritymanifest.HighLevelLibraryMatrix.Result;
import software.amazon.awssdk.tools.paritymanifest.HighLevelLibraryMatrix.Row;
import software.amazon.awssdk.tools.paritymanifest.HighLevelLibraryMatrix.Status;

/**
 * The high-level-library parity gate (task 14.6) on this repository: the Parity Manifest of the released V2 API jars
 * of the five high-level libraries ({@code target/v2-api}, the same manifest the compatibility fixtures are generated
 * from) plus the V2 artifact and behavior inventories, against the checked-in evidence: the launch gate's hook
 * registry, the generated compatibility fixtures of {@code test/codegen-generated-classes-test} and their hooks, and
 * the test sources of the V3 tree. It fails on any {@link HighLevelLibraryMatrix} problem: an owned entry without
 * evidence for a claimed dimension, evidence that names no V3 test, a missing or unverified lifecycle aspect of the
 * DynamoDB Enhanced Client or the Transfer Manager, or a known gap that is no longer one.
 *
 * <p>Whether that evidence passes is decided where it runs: the fixture suites and module suites fail their own
 * builds, and general availability requires a passing candidate record per hook ({@link LaunchGate}). The matrix is
 * written to {@code target/high-level-library-matrix/}.
 */
class HighLevelLibraryParityMatrixTest {
    private static final Path V3 = Path.of("../..").toAbsolutePath().normalize();
    private static final Path OUTPUT = Path.of("target", "high-level-library-matrix").toAbsolutePath();
    private static final Set<String> LIBRARIES = Set.of("dynamodb-enhanced", "s3-transfer-manager",
                                                        "iam-policy-builder", "s3-event-notifications",
                                                        "sns-message-manager");

    @Test
    void everyHighLevelLibraryEntryHasEvidenceForEachClaimedDimension() throws Exception {
        HighLevelLibraryMatrix matrix = HighLevelLibraryMatrix.loadDefault();
        assertEquals(LIBRARIES, matrix.libraries().stream().map(Library::name).collect(Collectors.toSet()));
        for (Library library : matrix.libraries()) {
            boolean async = library.name().equals("dynamodb-enhanced") || library.name().equals("s3-transfer-manager");
            assertEquals(async, !library.lifecycle().isEmpty(),
                         library.name() + ": the libraries with asynchronous transfers, and only they, declare the "
                         + "cancellation, cleanup, fault and concurrency lifecycle");
        }

        InventorySource.Context context = new InventorySource.Context(V3.getParent(), null);
        List<InventoryItem> extra = new ArrayList<>(new BehaviorInventorySource().scan(context));
        extra.addAll(new ArtifactInventorySource().scan(context));
        ParityManifest manifest = CompatibilityFixturesGoldenTest.baselineManifest(
            CompatibilityFixturesGoldenTest.v2ApiJars(), extra);
        EvidenceHooks hooks = LaunchGateCli.builtinHooks().merge(EvidenceHooks.parse(
            Files.readString(CompatibilityFixturesGoldenTest.HOOKS, StandardCharsets.UTF_8),
            CompatibilityFixturesGoldenTest.HOOKS.toString()));
        Map<String, String> suites = new TreeMap<>();
        CompatibilityFixturesGoldenTest.SUITES.forEach((kind, suite) -> suites.put(kind.name(), suite));

        Result result = matrix.evaluate(manifest, hooks, HighLevelLibraryMatrix.Fixtures.parse(
                                            CompatibilityFixturesGoldenTest.checkedInFixtures()),
                                        suites, HighLevelLibraryMatrix.TestIndex.of(CandidateTree.testMethods(V3)));
        Files.createDirectories(OUTPUT);
        Files.writeString(OUTPUT.resolve("matrix.json"), result.toJson(), StandardCharsets.UTF_8);
        Files.writeString(OUTPUT.resolve("matrix.txt"), result.toText(), StandardCharsets.UTF_8);
        System.out.print(result.toText());

        assertEquals(List.of(), result.problems().stream().map(Object::toString).toList(), result.toText());
        for (String library : LIBRARIES) {
            List<Row> rows = result.rows().stream().filter(r -> r.library().equals(library) && r.owned()).toList();
            assertTrue(rows.stream().anyMatch(r -> r.status() == Status.VERIFIED), library + " has verified entries");
            assertTrue(rows.stream().anyMatch(r -> r.entryId().startsWith("api:")), library + " has api: entries");
            assertTrue(rows.stream().anyMatch(r -> r.entryId().startsWith("artifact:")),
                       library + " has its artifact entry");
        }
        // The known gaps are reported, not hidden: each stays a blocked row of the matrix.
        assertFalse(result.rows().stream().noneMatch(r -> r.status() == Status.KNOWN_GAP));
    }
}
