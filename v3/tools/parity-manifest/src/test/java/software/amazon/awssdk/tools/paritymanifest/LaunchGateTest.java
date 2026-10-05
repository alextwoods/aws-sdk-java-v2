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
import static software.amazon.awssdk.tools.paritymanifest.LaunchGate.Stage.GENERAL_AVAILABILITY;
import static software.amazon.awssdk.tools.paritymanifest.LaunchGate.Stage.PACKAGING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.tools.paritymanifest.CandidateEvidence.EvidenceRecord;
import software.amazon.awssdk.tools.paritymanifest.CandidateEvidence.Outcome;
import software.amazon.awssdk.tools.paritymanifest.CandidateInventory.CandidateArtifact;
import software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code;
import software.amazon.smithy.model.node.Node;

/**
 * The launch gate's per-category comparison of a candidate with the manifest (task 13.3; Requirements 16.4, 16.5,
 * 16.9, 16.10, 16.12, 16.14, 16.15), on small hand-built manifests and candidate inventories.
 */
class LaunchGateTest {
    private static final CandidateIdentity CANDIDATE = new CandidateIdentity("3.0.0", "c0ffee");
    private static final String CLIENT = "api:software.amazon.awssdk.services.dynamodb.DynamoDbClient";
    private static final String GET_ITEM = CLIENT + "#getItem(Lsoftware/amazon/awssdk/services/dynamodb/model/"
                                           + "GetItemRequest;)Lsoftware/amazon/awssdk/services/dynamodb/model/"
                                           + "GetItemResponse;";
    private static final String GET_ITEM_V3 = CLIENT + "#getItem(Lsoftware/amazon/awssdk/services/dynamodb/model/"
                                              + "GetItemRequest;)Lsoftware/amazon/awssdk/core/Result;";
    private static final String PAGINATOR = "api:software.amazon.awssdk.services.route53.paginators."
                                            + "ListResourceRecordSetsIterable";
    private static final String CORE = "software.amazon.awssdk:sdk-core";

    @Test
    void shippedPreservedEntriesWithHooksPassPackagingInEveryCategory() {
        List<ManifestEntry> entries = List.of(
            hooked(Entries.preserved("service:dynamodb")), hooked(Entries.preserved("http-mode:apache-client:sync")),
            hooked(Entries.preserved("artifact:software.amazon.awssdk:dynamodb")), hooked(Entries.preserved(CLIENT)),
            hooked(Entries.preserved(GET_ITEM)), hooked(Entries.preserved("config:env:AWS_REGION")),
            hooked(Entries.preserved("behavior:checksum.request-calculation")),
            hooked(Entries.preserved("behavior:module-path.support")));
        CandidateInventory candidate = inventory(b -> b
            .capability("service:dynamodb", "software.amazon.awssdk:dynamodb")
            .capability("http-mode:apache-client:sync", "software.amazon.awssdk:apache-client")
            .capability(CLIENT, "software.amazon.awssdk:dynamodb")
            .capability(GET_ITEM, "software.amazon.awssdk:dynamodb")
            .capability("config:env:AWS_REGION", "software.amazon.awssdk:regions")
            .capability("behavior:checksum.request-calculation", "software.amazon.awssdk:checksums"),
                                                 "software.amazon.awssdk:dynamodb");

        LaunchGateReport report = gate(entries, candidate).build().evaluate(PACKAGING);

        assertEquals(List.of(), report.problems());
        assertTrue(report.passed());
        Map<LaunchCategory, LaunchGateReport.CategorySummary> byCategory = summaries(report);
        assertEquals(Set.of(LaunchCategory.SERVICE, LaunchCategory.HTTP_MODE, LaunchCategory.ARTIFACT,
                            LaunchCategory.CLIENT, LaunchCategory.REGION, LaunchCategory.CHECKSUM,
                            LaunchCategory.MODULE_PATH), byCategory.keySet());
        assertEquals(new LaunchGateReport.CategorySummary(LaunchCategory.CLIENT, 0, 2, 2, 0, 0, 2, 2, 0, 0),
                     byCategory.get(LaunchCategory.CLIENT));
    }

    @Test
    void missingPreservedCapabilitiesBlockPackagingPerKind() {
        Entries.B binary = hookedBuilder(Entries.preserved(GET_ITEM));
        binary.dimensions.add(CompatibilityDimension.BINARY);
        List<ManifestEntry> entries = List.of(
            hooked(Entries.preserved("service:ghost")), hooked(Entries.preserved("http-mode:netty-nio-client:async")),
            hooked(Entries.preserved(PAGINATOR)), binary.build(),
            hooked(Entries.preserved("config:profile:region")), hooked(Entries.preserved("behavior:compression.request")),
            hooked(Entries.preserved("artifact:software.amazon.awssdk:bundle")));
        // The candidate has getItem, but with another descriptor: enough for a source claim, not a binary one.
        CandidateInventory candidate = inventory(b -> b.capability(GET_ITEM_V3, "software.amazon.awssdk:dynamodb"));

        LaunchGateReport report = gate(entries, candidate).build().evaluate(PACKAGING);

        assertEquals(List.of("NOT_SHIPPED api:software.amazon.awssdk.services.dynamodb.DynamoDbClient#getItem("
                             + "Lsoftware/amazon/awssdk/services/dynamodb/model/GetItemRequest;)Lsoftware/amazon/awssdk/"
                             + "services/dynamodb/model/GetItemResponse; CLIENT",
                             "NOT_SHIPPED api:software.amazon.awssdk.services.route53.paginators."
                             + "ListResourceRecordSetsIterable PAGINATOR",
                             "NOT_SHIPPED artifact:software.amazon.awssdk:bundle ARTIFACT",
                             "NOT_SHIPPED behavior:compression.request COMPRESSION",
                             "NOT_SHIPPED config:profile:region REGION",
                             "NOT_SHIPPED http-mode:netty-nio-client:async HTTP_MODE",
                             "NOT_SHIPPED service:ghost SERVICE"),
                     brief(report));
        assertTrue(report.problems().stream().allMatch(p -> p.stage() == PACKAGING));

        Entries.B source = hookedBuilder(Entries.preserved(GET_ITEM));
        assertTrue(gate(List.of(source.build()), candidate).build().evaluate(PACKAGING).passed(),
                   "a source-only claim accepts a member of the same name with another descriptor");
    }

    @Test
    void modulePathBehaviorIsVerifiedStructurally() {
        Entries.B b = hookedBuilder(Entries.preserved("behavior:module-path.support"));
        b.v3Artifact = null;
        ManifestEntry modulePath = b.build();
        CandidateInventory unnamed = CandidateInventory.builder()
            .artifact(new CandidateArtifact("software.amazon.awssdk:named", "3.0.0", "named.jar", "a.named", false))
            .artifact(new CandidateArtifact("software.amazon.awssdk:plain", "3.0.0", "plain.jar", null, false))
            .capability("behavior:module-path.support", CandidateInventory.DECLARED)
            .build();

        LaunchGateReport report = gate(List.of(modulePath), unnamed).build().evaluate(PACKAGING);

        assertEquals(List.of("NOT_SHIPPED behavior:module-path.support MODULE_PATH"), brief(report));
        assertTrue(report.problems().get(0).message().contains("software.amazon.awssdk:plain"),
                   "a declaration does not replace the structural check");
    }

    @Test
    void replacementsMustShipWhateverTheClassificationOrDeprecation() {
        Entries.B adapted = Entries.adapted(CLIENT);
        adapted.replacement = GET_ITEM_V3;
        Entries.B deprecatedAdapted = Entries.adapted(GET_ITEM);
        deprecatedAdapted.deprecated = true;
        deprecatedAdapted.replacement = "api:software.amazon.awssdk.v3.Missing";
        Entries.B excluded = Entries.excluded("api:software.amazon.awssdk.core.retry.RetryPolicy");
        excluded.deprecated = true;
        excluded.replacement = "api:software.amazon.awssdk.retries.api.RetryStrategy";
        Entries.B deprecatedPreserved = Entries.preserved(PAGINATOR);
        deprecatedPreserved.deprecated = true;

        LaunchGateReport report = gate(List.of(adapted.build(), deprecatedAdapted.build(), excluded.build(),
                                               deprecatedPreserved.build()),
                                       inventory(b -> { }))
            .hooks(registry("AdaptedTest")).build().evaluate(PACKAGING);

        assertEquals(List.of("REPLACEMENT_NOT_SHIPPED api:software.amazon.awssdk.core.retry.RetryPolicy OTHER",
                             "REPLACEMENT_NOT_SHIPPED " + CLIENT + " CLIENT",
                             "REPLACEMENT_NOT_SHIPPED " + GET_ITEM + " CLIENT"),
                     brief(report));
        LaunchGateReport.CategorySummary client = summaries(report).get(LaunchCategory.CLIENT);
        assertEquals(1, client.deprecated());
        assertEquals(1, client.adapted());
        assertEquals(0, client.shipped());
        assertEquals(2, client.blocked());
        assertEquals(1, summaries(report).get(LaunchCategory.PAGINATOR).deprecated(),
                     "a deprecated PRESERVED entry is reported, not gated");
    }

    @Test
    void evidenceHooksAreRequiredRegisteredAndPresentInTheCandidate() {
        Entries.B unhooked = Entries.preserved(CLIENT);
        Entries.B unregistered = Entries.preserved("service:dynamodb");
        unregistered.evidenceIds.add("TypoTest");
        Entries.B hookedByRegistry = Entries.preserved("http-mode:apache-client:sync");
        Entries.B excluded = Entries.excluded("api:software.amazon.awssdk.core.retry.RetryPolicy");
        excluded.deprecated = true;
        EvidenceHooks hooks = EvidenceHooks.parse("""
            # Apache4SyncTckTest binds itself; GhostTest is registered but not in the candidate.
            Apache4SyncTckTest http-mode:apache-client:sync
            GhostTest
            OrphanTest service:not-in-the-manifest
            """, "hooks.txt");
        CandidateInventory candidate = inventory(b -> b
            .capability(CLIENT, "x:y").capability("service:dynamodb", "x:y")
            .capability("http-mode:apache-client:sync", "x:y"));

        LaunchGateReport report = gate(List.of(unhooked.build(), unregistered.build(), hookedByRegistry.build(),
                                               excluded.build()), candidate)
            .hooks(hooks).availableEvidence(Set.of("Apache4SyncTckTest", "OrphanTest")).build().evaluate(PACKAGING);

        // An unregistered evidence ID is reported and is no hook, so its entry also lacks one.
        assertEquals(List.of("MISSING_EVIDENCE_HOOK " + CLIENT + " CLIENT",
                             "UNKNOWN_EVIDENCE_HOOK evidence null",
                             "UNKNOWN_EVIDENCE_ENTRY evidence null",
                             "MISSING_EVIDENCE_HOOK service:dynamodb SERVICE",
                             "UNKNOWN_EVIDENCE_HOOK service:dynamodb SERVICE"),
                     brief(report));
        assertTrue(report.problems().stream().anyMatch(p -> p.message().contains("GhostTest")));
        assertTrue(report.problems().stream().anyMatch(p -> p.message().contains("service:not-in-the-manifest")));
        assertEquals(1, summaries(report).get(LaunchCategory.HTTP_MODE).hooked());

        // Without a registry, an evidence ID only has to exist in the candidate.
        LaunchGateReport unregisteredRun = gate(List.of(unregistered.build()), candidate)
            .availableEvidence(Set.of("TypoTest")).build().evaluate(PACKAGING);
        assertEquals(List.of(), unregisteredRun.problems());
    }

    @Test
    void expectedArtifactsAndOwnersMustBePartOfTheCandidate() {
        Entries.B owned = hookedBuilder(Entries.preserved(CLIENT));
        owned.v3Artifact = "software.amazon.awssdk:dynamodb";
        CandidateInventory candidate = inventory(b -> b.capability(CLIENT, "software.amazon.awssdk:other"));

        LaunchGateReport report = gate(List.of(owned.build()), candidate)
            .expectedArtifacts(Set.of(CORE, "software.amazon.awssdk:dynamodb")).build().evaluate(PACKAGING);

        assertEquals(List.of("OWNER_NOT_SHIPPED " + CLIENT + " CLIENT", "CANDIDATE_INCOMPLETE candidate null"),
                     brief(report));
        assertTrue(report.problems().get(1).message().contains("software.amazon.awssdk:dynamodb"));
    }

    @Test
    void invalidManifestBlocksPackagingAndAnUnfrozenBaselineBlocksGeneralAvailability() {
        Entries.B invalid = hookedBuilder(Entries.adapted(CLIENT));
        invalid.differences.clear();
        invalid.replacement = CLIENT;
        ParityManifest unfrozen = new ParityManifest(new BaselineIdentity("2.55.5", null, null, null, "test"),
                                                     CANDIDATE, List.of(invalid.build()));
        CandidateInventory candidate = inventory(b -> b.capability(CLIENT, "x:y"));

        LaunchGateReport packaging = LaunchGate.builder().manifest(unfrozen).candidate(candidate).build()
                                               .evaluate(PACKAGING);
        assertEquals(List.of("INVALID_MANIFEST " + CLIENT + " CLIENT"), brief(packaging));

        LaunchGateReport ga = LaunchGate.builder().manifest(unfrozen).candidate(candidate).build()
                                        .evaluate(GENERAL_AVAILABILITY);
        assertTrue(brief(ga).containsAll(List.of("INVALID_MANIFEST manifest null", "INVALID_MANIFEST " + CLIENT
                                                                                    + " CLIENT")), brief(ga).toString());
        assertTrue(ga.problems().stream().filter(p -> p.subject().equals("manifest"))
                     .allMatch(p -> p.stage() == GENERAL_AVAILABILITY));
    }

    @Test
    void generalAvailabilityRequiresPassingCandidateEvidenceForEveryHook() {
        Entries.B twoHooks = hookedBuilder(Entries.preserved(CLIENT));
        twoHooks.evidenceIds.add("SecondTest");
        List<ManifestEntry> entries = List.of(twoHooks.build(), hooked(Entries.preserved("service:dynamodb")),
                                              hooked(Entries.preserved("service:s3")),
                                              hooked(Entries.preserved("service:sqs")),
                                              Entries.excluded("service:legacy").build());
        CandidateInventory candidate = inventory(b -> b.capability(CLIENT, "x:y").capability("service:dynamodb", "x:y")
                                                       .capability("service:s3", "x:y")
                                                       .capability("service:sqs", "x:y"));
        CandidateEvidence evidence = new CandidateEvidence(CANDIDATE, List.of(
            passed(CLIENT, "GateTest"), passed(CLIENT, "SecondTest"),
            passed("service:dynamodb", "GateTest"),
            new EvidenceRecord("service:s3", "GateTest", "200 OK", "500", Outcome.FAILED),
            new EvidenceRecord("service:sqs", "GateTest", "200 OK", "500", Outcome.PASSED),
            passed("service:sqs", "StrayTest"),
            passed("service:unknown", "GateTest")));

        LaunchGate gate = gate(entries, candidate).evidence(evidence).build();
        assertTrue(gate.evaluate(PACKAGING).passed(), "evidence and EXCLUDED entries only gate general availability: "
                                                      + gate.evaluate(PACKAGING).problems());
        LaunchGateReport report = gate.evaluate(GENERAL_AVAILABILITY);

        assertEquals(List.of("UNKNOWN_EVIDENCE_ENTRY evidence null",
                             "EXCLUDED_NOT_DEPRECATED service:legacy SERVICE",
                             "FAILED_CANDIDATE_EVIDENCE service:s3 SERVICE",
                             "INCONSISTENT_CANDIDATE_EVIDENCE service:sqs SERVICE",
                             "UNBOUND_CANDIDATE_EVIDENCE service:sqs SERVICE"),
                     brief(report));
        assertTrue(report.problems().stream().allMatch(p -> p.stage() == GENERAL_AVAILABILITY));
        assertEquals(1, summaries(report).get(LaunchCategory.CLIENT).verified());
        assertEquals(1, summaries(report).get(LaunchCategory.SERVICE).verified());

        // A hook without a record is missing evidence.
        CandidateEvidence partial = new CandidateEvidence(CANDIDATE, List.of(passed(CLIENT, "GateTest")));
        LaunchGateReport missing = gate(List.of(twoHooks.build()), candidate).evidence(partial).build()
                                                                              .evaluate(GENERAL_AVAILABILITY);
        assertEquals(List.of("MISSING_CANDIDATE_EVIDENCE " + CLIENT + " CLIENT"), brief(missing));
        assertTrue(missing.problems().get(0).message().contains("SecondTest"));
    }

    @Test
    void generalAvailabilityRequiresOneCandidateIdentity() {
        Entries.B owned = hookedBuilder(Entries.preserved(CLIENT));
        owned.v3Artifact = "software.amazon.awssdk:dynamodb";
        ManifestEntry entry = owned.build();
        CandidateInventory candidate = CandidateInventory.builder()
            .artifact(new CandidateArtifact("software.amazon.awssdk:dynamodb", "3.0.1", "d.jar", "m", false))
            .capability(CLIENT, "software.amazon.awssdk:dynamodb").build();
        CandidateEvidence other = new CandidateEvidence(new CandidateIdentity("3.0.0", "beef"),
                                                        List.of(passed(CLIENT, "GateTest")));

        LaunchGateReport report = gate(List.of(entry), candidate).evidence(other).build().evaluate(GENERAL_AVAILABILITY);
        assertEquals(List.of("CANDIDATE_MISMATCH candidate null", "CANDIDATE_MISMATCH candidate null"), brief(report));

        ParityManifest anonymous = new ParityManifest(Entries.BASELINE, null, List.of(hooked(Entries.preserved(CLIENT))));
        LaunchGateReport noEvidence = LaunchGate.builder().manifest(anonymous).candidate(inventory(b -> b.capability(
            CLIENT, "x:y"))).build().evaluate(GENERAL_AVAILABILITY);
        assertEquals(List.of("MISSING_CANDIDATE_EVIDENCE " + CLIENT + " CLIENT", "CANDIDATE_MISMATCH candidate null",
                             "MISSING_CANDIDATE_EVIDENCE evidence null"),
                     brief(noEvidence));
    }

    @Test
    void reportJsonAndRenderingCarryVerdictCountsAndProblems() {
        LaunchGateReport report = gate(List.of(hooked(Entries.preserved("service:ghost"))), inventory(b -> { }))
            .build().evaluate(PACKAGING);

        String json = report.toJson();
        var node = Node.parse(json).expectObjectNode();
        assertEquals("PACKAGING", node.expectStringMember("stage").getValue());
        assertFalse(node.expectBooleanMember("passed").getValue());
        assertEquals(1, node.expectObjectMember("problemCounts").expectNumberMember("NOT_SHIPPED").getValue()
                            .intValue());
        assertEquals("3.0.0", node.expectObjectMember("candidate").expectStringMember("v3Version").getValue());
        assertEquals(json, report.toJson(), "deterministic");
        assertTrue(json.endsWith("}\n"));

        String text = report.render(0);
        assertTrue(text.startsWith("Launch gate packaging: BLOCKED (1 problem(s))"), text);
        assertTrue(text.contains("NOT_SHIPPED: 1"));
        assertTrue(text.contains("... and 1 more problem(s)"));
        assertTrue(report.render().contains("NOT_SHIPPED [service:ghost, SERVICE]"));
    }

    @Test
    void stagesParseFromTheirCliNames() {
        assertEquals(PACKAGING, LaunchGate.Stage.fromCliName("packaging"));
        assertEquals(GENERAL_AVAILABILITY, LaunchGate.Stage.fromCliName(" General-Availability "));
        assertThrows(IllegalArgumentException.class, () -> LaunchGate.Stage.fromCliName("ga"));
    }

    private static LaunchGate.Builder gate(List<ManifestEntry> entries, CandidateInventory candidate) {
        return LaunchGate.builder().manifest(new ParityManifest(Entries.BASELINE, CANDIDATE, entries))
                         .candidate(candidate);
    }

    private static EvidenceRecord passed(String entry, String evidence) {
        return new EvidenceRecord(entry, evidence, "ok", "ok", Outcome.PASSED);
    }

    private static EvidenceHooks registry(String... ids) {
        return EvidenceHooks.parse(String.join("\n", ids), "registry");
    }

    /** An inventory with the sdk-core artifact (the test entries' owner), {@code more} artifacts and capabilities. */
    private static CandidateInventory inventory(java.util.function.Consumer<CandidateInventory.Builder> capabilities,
                                                String... more) {
        CandidateInventory.Builder b = CandidateInventory.builder();
        List<String> artifacts = new ArrayList<>(List.of(CORE));
        artifacts.addAll(Arrays.asList(more));
        artifacts.forEach(a -> b.artifact(new CandidateArtifact(a, "3.0.0", a + ".jar", "m." + a.replace(':', '.')
                                                                                          .replace('-', '.'),
                                                                false)));
        capabilities.accept(b);
        return b.build();
    }

    private static Entries.B hookedBuilder(Entries.B b) {
        b.evidenceIds.clear();
        b.evidenceIds.add("GateTest");
        return b;
    }

    private static ManifestEntry hooked(Entries.B b) {
        return hookedBuilder(b).build();
    }

    private static Map<LaunchCategory, LaunchGateReport.CategorySummary> summaries(LaunchGateReport report) {
        Map<LaunchCategory, LaunchGateReport.CategorySummary> map = new java.util.EnumMap<>(LaunchCategory.class);
        report.categories().forEach(c -> map.put(c.category(), c));
        return map;
    }

    /** {@code CODE subject category} of every problem, in report order. */
    private static List<String> brief(LaunchGateReport report) {
        return report.problems().stream().map(p -> p.code() + " " + p.subject() + " " + p.category()).toList();
    }
}
