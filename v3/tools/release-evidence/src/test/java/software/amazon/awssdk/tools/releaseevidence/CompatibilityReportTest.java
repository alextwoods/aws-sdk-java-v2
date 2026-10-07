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

package software.amazon.awssdk.tools.releaseevidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.tools.releaseevidence.CompatibilityReport.CorpusProject;
import software.amazon.awssdk.tools.releaseevidence.CompatibilityReport.Diagnostic;
import software.amazon.awssdk.tools.releaseevidence.CompatibilityReport.Migration;
import software.amazon.awssdk.tools.releaseevidence.CompatibilityReport.StepResult;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

class CompatibilityReportTest {
    private static final Diagnostic DIAGNOSTIC =
        new Diagnostic("src/main/java/A.java", 7, 5, "Aws4Signer.create()", "SIGNER",
                       "api:software.amazon.awssdk.auth.signer.Aws4Signer", "EXCLUDED",
                       "api:software.amazon.awssdk.auth.api.HttpSigner",
                       "api:software.amazon.awssdk.auth.signer.Aws4Signer", "excluded.legacy-signer-retry",
                       "Excluded legacy signer");

    @Test
    void consistentReportHasNoProblemsAndBlocksNothing() {
        CompatibilityReport report = report(clean(StepResult.passed("compiled"), StepResult.passed("same transcript")),
                                            unresolved(StepResult.notRun("1 unresolved"), StepResult.notRun("-")));
        assertEquals(List.of(), report.problems());
        assertEquals(List.of(), report.blockingResults(true));
    }

    @Test
    void jsonHasTheSchemaMembersInOrderAndSummarizesTheCorpus() {
        CompatibilityReport report = report(clean(StepResult.passed("compiled"), StepResult.passed("same transcript")),
                                            unresolved(StepResult.notRun("1 unresolved"), StepResult.notRun("-")));
        String json = report.toJson();
        assertEquals(json, report.toJson(), "byte-reproducible");
        assertTrue(json.endsWith("}\n"));
        ObjectNode top = Node.parse(json).expectObjectNode();
        assertEquals(List.of("schemaVersion", "candidate", "baseline", "manifest", "summary", "recipes", "corpus"),
                     top.getMembers().keySet().stream().map(Object::toString).toList());
        ObjectNode summary = top.expectObjectMember("summary");
        assertEquals(2, summary.expectNumberMember("projects").getValue().intValue());
        assertEquals(1, summary.expectObjectMember("compile").expectNumberMember("PASSED").getValue().intValue());
        assertEquals(1, summary.expectObjectMember("compile").expectNumberMember("NOT_RUN").getValue().intValue());
        assertEquals(1, summary.expectObjectMember("unresolvedByClassification").expectNumberMember("EXCLUDED")
                               .getValue().intValue());
        assertEquals(1, summary.expectObjectMember("unresolvedByRecipe")
                               .expectNumberMember("excluded.legacy-signer-retry").getValue().intValue());
        ObjectNode diagnostic = top.expectArrayMember("corpus").get(1).get().expectObjectNode()
                                   .expectObjectMember("migration").expectArrayMember("unresolved").get(0).get()
                                   .expectObjectNode();
        assertEquals("api:software.amazon.awssdk.auth.api.HttpSigner",
                     diagnostic.expectStringMember("replacement").getValue());
        assertNotNull(CompatibilityReport.class.getResource(CompatibilityReport.SCHEMA_RESOURCE));
    }

    @Test
    void stepResultsMustFollowTheMigration() {
        CompatibilityReport report = report(
            clean(StepResult.notRun("skipped"), StepResult.notRun("-")),
            unresolved(StepResult.passed("compiled anyway"), StepResult.passed("ran")));
        assertEquals(List.of("project clean: compile must run: the migration left no unresolved item",
                             "project partial: compile must be NOT_RUN with 1 unresolved items"),
                     report.problems());
    }

    @Test
    void failedAndBlockedStepsNeedTheirCauses() {
        CompatibilityReport report = report(
            clean(new StepResult(CompatibilityReport.Status.BLOCKED_BY_CANDIDATE, "needs entries", List.of(),
                                 List.of("error")), StepResult.notRun("-")),
            unresolved(StepResult.notRun("1 unresolved"), StepResult.notRun("-")));
        assertEquals(List.of("project clean: compile: BLOCKED_BY_CANDIDATE without the entries the candidate does not "
                             + "ship"), report.problems());
    }

    @Test
    void countsMustAddUp() {
        Migration wrong = new Migration(3, Map.of("PRESERVED", 2), Map.of("API", Map.of("PRESERVED", 2)),
                                        Map.of("PRESERVED", 2), List.of(), List.of());
        CompatibilityReport report = report(new CorpusProject("wrong", "SYNTHETIC", List.of("PRESERVED_ONLY"), null,
                                                              wrong, StepResult.passed("v2"),
                                                              StepResult.passed("compiled"),
                                                              StepResult.passed("ran"), null));
        assertEquals(List.of("project wrong: dispositions count 2 items, not 3",
                             "project wrong: itemClassifications count 2 items, not 3",
                             "project wrong: categories count 2 items, not 3"),
                     report.problems());
    }

    @Test
    void blockedByCandidateBlocksOnlyLaunchGateStages() {
        CompatibilityReport report = report(
            clean(StepResult.blocked("needs 1 entry", List.of("api:software.amazon.awssdk.Missing"),
                                     List.of("A.java:3: cannot find symbol")), StepResult.notRun("-")),
            unresolved(StepResult.notRun("1 unresolved"), StepResult.notRun("-")));
        assertEquals(List.of(), report.problems());
        assertEquals(List.of(), report.blockingResults(false));
        assertEquals(1, report.blockingResults(true).size());
    }

    @Test
    void failedStepsAlwaysBlock() {
        CompatibilityReport report = report(
            clean(StepResult.passed("compiled"), StepResult.failed("different transcript", List.of("diff"))),
            unresolved(StepResult.notRun("1 unresolved"), StepResult.notRun("-")));
        assertEquals(List.of("clean behavior FAILED: different transcript"), report.blockingResults(false));
    }

    @Test
    void unknownCountKeysAreRejected() {
        assertThrows(IllegalArgumentException.class,
                     () -> new CompatibilityReport.Manifest(1, Map.of("RENAMED", 1)));
    }

    private static CompatibilityReport report(CorpusProject... projects) {
        return new CompatibilityReport(new CompatibilityReport.Candidate("3.0.0", "abc"),
                                       new CompatibilityReport.Baseline("2.55.0", "2.54.0", "abc", "scanner/1"),
                                       new CompatibilityReport.Manifest(3, Map.of("PRESERVED", 1, "ADAPTED", 1,
                                                                                  "EXCLUDED", 1)),
                                       List.of(new CompatibilityReport.Recipe("excluded.legacy-signer-retry",
                                                                              "Excluded facilities", 0, 0, 1)),
                                       List.of(projects));
    }

    private static CorpusProject clean(StepResult compile, StepResult behavior) {
        Migration m = new Migration(2, Map.of("PRESERVED", 1, "TRANSFORMED", 1),
                                    Map.of("DEPENDENCY", Map.of("TRANSFORMED", 1), "CLIENT", Map.of("PRESERVED", 1)),
                                    Map.of("PRESERVED", 2), List.of("pom.xml"), List.of());
        return new CorpusProject("clean", "REPRESENTATIVE", List.of("PRESERVED_ONLY"), "A clean project", m,
                                 StepResult.passed("v2"), compile, behavior, "reports/clean.json");
    }

    private static CorpusProject unresolved(StepResult compile, StepResult behavior) {
        Migration m = new Migration(1, Map.of("UNRESOLVED", 1), Map.of("SIGNER", Map.of("UNRESOLVED", 1)),
                                    Map.of("EXCLUDED", 1), List.of(), List.of(DIAGNOSTIC));
        return new CorpusProject("partial", "REPRESENTATIVE", List.of("EXCLUDED"), "A project with an excluded signer",
                                 m, StepResult.passed("v2"), compile, behavior, "reports/partial.json");
    }
}
