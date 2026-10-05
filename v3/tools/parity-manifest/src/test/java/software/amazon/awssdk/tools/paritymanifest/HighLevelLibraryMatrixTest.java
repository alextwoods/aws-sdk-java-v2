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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension.BEHAVIOR;
import static software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension.BINARY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.tools.paritymanifest.HighLevelLibraryMatrix.Fixture;
import software.amazon.awssdk.tools.paritymanifest.HighLevelLibraryMatrix.Fixtures;
import software.amazon.awssdk.tools.paritymanifest.HighLevelLibraryMatrix.Result;
import software.amazon.awssdk.tools.paritymanifest.HighLevelLibraryMatrix.Row;
import software.amazon.awssdk.tools.paritymanifest.HighLevelLibraryMatrix.Status;
import software.amazon.awssdk.tools.paritymanifest.HighLevelLibraryMatrix.TestIndex;
import software.amazon.smithy.model.node.Node;

/** {@link HighLevelLibraryMatrix} over a synthetic library, manifest, fixture set, hook registry and test tree. */
class HighLevelLibraryMatrixTest {
    private static final String TYPE = "api:software.amazon.awssdk.lib.Client";
    private static final String MEMBER = "api:software.amazon.awssdk.lib.Client#get()V";
    private static final String ADAPTED = "api:software.amazon.awssdk.lib.Client#response()Ljava/lang/Object;";
    private static final String GAP = "api:software.amazon.awssdk.lib.Client#presign()V";
    private static final String ARTIFACT = "artifact:software.amazon.awssdk:lib";
    private static final String CANCEL = "behavior:lib.cancellation";
    private static final List<String> SCOPE = List.of("api:software.amazon.awssdk.lib.*",
                                                      "!api:software.amazon.awssdk.lib.Client#presign()*");
    private static final Map<String, String> SUITES = Map.of("COMPILE", "CompileSuite", "LINK", "LinkSuite",
                                                             "SIGNATURE", "ShapeSuite", "HIERARCHY", "ShapeSuite",
                                                             "BEHAVIOR", "BehaviorSuite",
                                                             "COMPILE_REJECTED", "CompileSuite",
                                                             "LINK_REJECTED", "LinkSuite");
    private static final String DEFINITION = """
        {"schemaVersion": 1, "libraries": [{
            "name": "lib", "artifact": "software.amazon.awssdk:lib",
            "entries": ["artifact:software.amazon.awssdk:lib", "api:software.amazon.awssdk.lib.*", "behavior:lib.*"],
            "lifecycle": {
                "cancellation": {"entry": "behavior:lib.cancellation", "tests": ["LibTest#cancels"]},
                "failure-cleanup": {"entry": "behavior:lib.cancellation", "tests": ["LibTest#cleansUp"]},
                "fault-handling": {"entry": "behavior:lib.cancellation", "tests": ["LibTest#faults"]},
                "concurrency": {"entry": "behavior:lib.cancellation", "tests": ["LibTest#concurrent"]}
            },
            "knownGaps": [{"entries": ["api:software.amazon.awssdk.lib.Client#presign()*"], "reason": "not built"}]
        }]}
        """;

    @Test
    void entriesWithEvidenceForEveryClaimedDimensionAreVerifiedAndGapsAreReported() {
        Result result = evaluate(manifest(), fixtures(), hooks("LibTest " + ARTIFACT + " " + CANCEL), tests());

        assertEquals(List.of(), result.problems());
        assertEquals(Map.of(ADAPTED, Status.VERIFIED, ARTIFACT, Status.VERIFIED, CANCEL, Status.VERIFIED,
                            GAP, Status.KNOWN_GAP, MEMBER, Status.VERIFIED, TYPE, Status.VERIFIED),
                     statuses(result));
        Row member = row(result, MEMBER);
        assertEquals(List.of("source", "binary"), member.claimed());
        assertEquals(Set.of("LinkSuite", "ShapeSuite"), member.evidence().get("binary"));
        assertEquals(Set.of("BehaviorSuite", "AdaptedTest"), row(result, ADAPTED).evidence().get("differences"));
        assertEquals(Set.of("CompileSuite", "LinkSuite"), row(result, ADAPTED).evidence().get("rejected"));
        assertEquals("not built", row(result, GAP).note());
    }

    @Test
    void missingEvidenceUnresolvedHooksAndStaleGapsAreProblems() {
        List<Fixture> fixtures = new ArrayList<>(fixtures().fixtures());
        // The member loses its binary fixtures, and the gap gains a fixture.
        fixtures.removeIf(f -> f.entryId().equals(MEMBER) && "binary".equals(f.dimension()));
        fixtures.add(new Fixture(GAP, "COMPILE", "source", List.of()));
        Result result = evaluate(manifest(), new Fixtures(List.of("api:software.amazon.awssdk.lib.*"), fixtures),
                                 hooks("LibTest " + CANCEL + "\nGhostTest " + ARTIFACT), tests());

        assertEquals(List.of(
            "[lib] " + MEMBER + ": no evidence for its binary claim",
            "[lib] " + GAP + ": the known gap now has evidence {source=[CompileSuite]}; verify it ships and remove "
            + "it from knownGaps",
            "[lib] " + ARTIFACT + ": evidence GhostTest is not a test class of the V3 tree"),
                     result.problems().stream().map(Object::toString).toList());
        assertEquals(Status.MISSING_EVIDENCE, row(result, MEMBER).status());
    }

    @Test
    void lifecycleAspectsMustBeOwnedVerifiedAndBackedByExistingMethodsOfAHook() {
        // The lifecycle test class is not a hook of the aspect entry, and one test method does not exist.
        Result result = evaluate(manifest(), fixtures(), hooks("LibTest " + ARTIFACT + "\nOtherTest " + CANCEL),
                                 TestIndex.of(Map.of("LibTest", Set.of("cancels", "cleansUp", "faults"),
                                                     "OtherTest", Set.of("x"), "AdaptedTest", Set.of("x"),
                                                     "CompileSuite", Set.of(), "LinkSuite", Set.of(),
                                                     "ShapeSuite", Set.of(), "BehaviorSuite", Set.of())));
        List<String> problems = result.problems().stream().map(Object::toString).toList();
        assertEquals(List.of(
            "[lib] lifecycle cancellation: LibTest is not an evidence hook of behavior:lib.cancellation",
            "[lib] lifecycle concurrency: LibTest is not an evidence hook of behavior:lib.cancellation",
            "[lib] lifecycle concurrency: LibTest#concurrent: no such test method",
            "[lib] lifecycle failure-cleanup: LibTest is not an evidence hook of behavior:lib.cancellation",
            "[lib] lifecycle fault-handling: LibTest is not an evidence hook of behavior:lib.cancellation"), problems);
    }

    @Test
    void aGapReclassifiedExcludedOrAnUnscopedApiEntryIsAProblem() {
        List<ManifestEntry> entries = new ArrayList<>(manifest().entries());
        entries.removeIf(e -> e.id().equals(GAP) || e.id().equals(MEMBER));
        Entries.B excludedGap = Entries.excluded(GAP);
        entries.add(excludedGap.build());
        Entries.B member = Entries.preserved(MEMBER);
        member.dimensions.add(BINARY);
        entries.add(member.build());
        Result result = evaluate(new ParityManifest(Entries.BASELINE, null, entries),
                                 new Fixtures(List.of(TYPE, ADAPTED), fixtures().fixtures()),
                                 hooks("LibTest " + ARTIFACT + " " + CANCEL), tests());
        assertEquals(List.of(
            "[lib] " + MEMBER + ": not in the compatibility fixture scope (compatibility-scope.txt) and not a known "
            + "gap",
            "[lib] " + GAP + ": a known gap must stay PRESERVED or ADAPTED so the launch gate blocks it, but it is "
            + "EXCLUDED"), result.problems().stream().map(Object::toString).toList());
    }

    @Test
    void launchProblemsAreCountedPerLibraryAndPerKnownGapDependency() {
        HighLevelLibraryMatrix matrix = HighLevelLibraryMatrix.parse(DEFINITION.replace(
            "\"api:software.amazon.awssdk.lib.Client#presign()*\"]",
            "\"api:software.amazon.awssdk.lib.Client#presign()*\", \"behavior:other.gap\"]"));
        String report = """
            {"problems": [
              {"code": "NOT_SHIPPED", "subject": "api:software.amazon.awssdk.lib.Client#presign()V"},
              {"code": "MISSING_EVIDENCE_HOOK", "subject": "api:software.amazon.awssdk.lib.Client#presign()V"},
              {"code": "NOT_SHIPPED", "subject": "behavior:other.gap"},
              {"code": "NOT_SHIPPED", "subject": "api:software.amazon.awssdk.unrelated.Type"}]}
            """;
        assertEquals(Map.of("lib", Map.of("MISSING_EVIDENCE_HOOK", 1, "NOT_SHIPPED", 1),
                            "lib (known-gap dependencies)", Map.of("NOT_SHIPPED", 1)),
                     matrix.launchProblems(Node.parse(report).expectObjectNode()));
    }

    @Test
    void malformedDefinitionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> HighLevelLibraryMatrix.parse("{\"libraries\": []}"));
        assertThrows(IllegalArgumentException.class, () -> HighLevelLibraryMatrix.parse(
            "{\"libraries\": [{\"name\": \"x\", \"artifact\": \"g:x\", \"bogus\": 1}]}"));
    }

    @Test
    void theCheckedInDefinitionParses() {
        assertEquals(5, HighLevelLibraryMatrix.loadDefault().libraries().size());
    }

    // ---- helpers ----

    private static Result evaluate(ParityManifest manifest, Fixtures fixtures, EvidenceHooks hooks, TestIndex tests) {
        return HighLevelLibraryMatrix.parse(DEFINITION).evaluate(manifest, hooks, fixtures, SUITES, tests);
    }

    private static ParityManifest manifest() {
        Entries.B type = Entries.preserved(TYPE);
        Entries.B member = Entries.preserved(MEMBER);
        member.dimensions.add(BINARY);
        Entries.B cancel = Entries.preserved(CANCEL);
        cancel.dimensions = java.util.EnumSet.of(BEHAVIOR);
        return new ParityManifest(Entries.BASELINE, null, List.of(
            type.build(), member.build(), Entries.adapted(ADAPTED).build(), Entries.preserved(GAP).build(),
            Entries.preserved(ARTIFACT).build(), cancel.build(),
            Entries.preserved("api:software.amazon.awssdk.unrelated.Type").build()));
    }

    private static Fixtures fixtures() {
        return new Fixtures(SCOPE, List.of(
            new Fixture(TYPE, "COMPILE", "source", List.of()),
            new Fixture(TYPE, "HIERARCHY", "source", List.of()),
            new Fixture(MEMBER, "COMPILE", "source", List.of()),
            new Fixture(MEMBER, "SIGNATURE", "source", List.of()),
            new Fixture(MEMBER, "LINK", "binary", List.of()),
            new Fixture(MEMBER, "SIGNATURE", "binary", List.of()),
            new Fixture(ADAPTED, "COMPILE_REJECTED", null, List.of()),
            new Fixture(ADAPTED, "LINK_REJECTED", null, List.of()),
            new Fixture(ADAPTED, "BEHAVIOR", null, List.of("AdaptedTest"))));
    }

    private static EvidenceHooks hooks(String bindings) {
        return EvidenceHooks.parse("CompileSuite\nLinkSuite\nShapeSuite\nBehaviorSuite\nAdaptedTest\n" + bindings,
                                   "test");
    }

    private static TestIndex tests() {
        return TestIndex.of(Map.of("LibTest", Set.of("cancels", "cleansUp", "faults", "concurrent"),
                                   "AdaptedTest", Set.of("x"), "CompileSuite", Set.of(), "LinkSuite", Set.of(),
                                   "ShapeSuite", Set.of(), "BehaviorSuite", Set.of()));
    }

    private static Map<String, Status> statuses(Result result) {
        Map<String, Status> statuses = new java.util.TreeMap<>();
        result.rows().forEach(r -> statuses.put(r.entryId(), r.status()));
        return statuses;
    }

    private static Row row(Result result, String id) {
        return result.rows().stream().filter(r -> r.entryId().equals(id)).findFirst().orElseThrow();
    }
}
