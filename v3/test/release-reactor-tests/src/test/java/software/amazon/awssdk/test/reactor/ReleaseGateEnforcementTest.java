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

package software.amazon.awssdk.test.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.test.reactor.CoreRuntimeSuiteReports.Suite;
import software.amazon.awssdk.test.reactor.ReleaseGateEnforcement.Approvals;
import software.amazon.awssdk.test.reactor.ReleaseGateEnforcement.Finding;
import software.amazon.awssdk.test.reactor.ReleaseGateEnforcement.Kind;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.DeploymentMode;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.DeploymentModes;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.ExpectedCase;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.ExpectedSuite;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.Inventory;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.ProtocolEvidence;

/** {@link ReleaseGateEnforcement} against synthetic surefire reports of a synthetic inventory. */
class ReleaseGateEnforcementTest {
    private static final String CONFORMANCE = "example.ConformanceTest";
    private static final String NATIVE = "example.NativeImageSmokeTest";
    private static final DeploymentModes CLASS_PATH_ONLY = new DeploymentModes(false);

    @TempDir
    Path root;
    private Instant buildStart;

    @BeforeEach
    void startBuild() {
        buildStart = Instant.now().minusSeconds(60);
    }

    @Test
    void everyCasePassingIsClean() throws IOException {
        report(CONFORMANCE, null, 1.0, pass("factory()[1]"), pass("factory()[2]"), pass("other"));
        report(NATIVE, null, 0.1, skip("smoke"));
        assertEquals(List.of(), check(CLASS_PATH_ONLY, approvals()));
    }

    @Test
    void aMissingReportAndAMissingExpectedCaseAreMissing() throws IOException {
        report(CONFORMANCE, null, 1.0, pass("factory()[1]"));
        List<Finding> findings = check(CLASS_PATH_ONLY, approvals());
        assertEquals(Set.of("MISSING example.ConformanceTest factory()[2]", "MISSING example.NativeImageSmokeTest -"),
                     keys(findings));
    }

    @Test
    void aStaleReportIsMissing() throws IOException {
        report(CONFORMANCE, null, 1.0, pass("factory()[1]"), pass("factory()[2]"));
        report(NATIVE, null, 0.1, skip("smoke"));
        Path stale = new Suite("m", CONFORMANCE).report(root);
        Files.setLastModifiedTime(stale, FileTime.from(buildStart.minus(Duration.ofHours(1))));
        assertEquals(Set.of("MISSING example.ConformanceTest -"), keys(check(CLASS_PATH_ONLY, approvals())));
    }

    @Test
    void failuresAndErrorsAreFailed() throws IOException {
        report(CONFORMANCE, null, 1.0, fail("factory()[1]", "failure", "org.opentest4j.AssertionFailedError", "wrong"),
               fail("factory()[2]", "error", "java.lang.IllegalStateException", "boom"));
        report(NATIVE, null, 0.1, skip("smoke"));
        assertEquals(Set.of("FAILED example.ConformanceTest factory()[1]", "FAILED example.ConformanceTest factory()[2]"),
                     keys(check(CLASS_PATH_ONLY, approvals())));
    }

    @Test
    void timeoutFailuresSlowCasesAndSlowSuitesAreTimedOut() throws IOException {
        report(CONFORMANCE, null, 999.0,
               fail("factory()[1]", "error", "java.util.concurrent.TimeoutException", "factory() timed out after 5 s"),
               fail("factory()[2]", "failure", "org.opentest4j.AssertionFailedError", "did not finish within PT30S"),
               pass("slow", 31.0));
        report(NATIVE, null, 0.1, skip("smoke"));
        assertEquals(Set.of("TIMED_OUT example.ConformanceTest factory()[1]",
                            "TIMED_OUT example.ConformanceTest factory()[2]",
                            "TIMED_OUT example.ConformanceTest slow", "TIMED_OUT example.ConformanceTest -"),
                     keys(check(CLASS_PATH_ONLY, approvals())));
    }

    @Test
    void anUnapprovedSkipFailsAndAnApprovedSkipPasses() throws IOException {
        report(CONFORMANCE, null, 1.0, pass("factory()[1]"), pass("factory()[2]"), skip("assumed"));
        report(NATIVE, null, 0.1, skip("smoke"));
        assertEquals(Set.of("UNAPPROVED_SKIP example.ConformanceTest assumed"), keys(check(CLASS_PATH_ONLY,
                                                                                            approvals())));
        Approvals approved = approvals(CONFORMANCE + " | assu* | always | documented | Requirement 19.15",
                                       NATIVE + " | * | native-image-not-required | optional | Task 18.9");
        assertEquals(List.of(), check(CLASS_PATH_ONLY, approved));
    }

    @Test
    void aConditionalApprovalLapsesWhenItsDeploymentModeIsRequired() throws IOException {
        report(CONFORMANCE, null, 1.0, pass("factory()[1]"), pass("factory()[2]"));
        report(NATIVE, null, 0.1, skip("smoke"));
        DeploymentModes nativeRequired = DeploymentModes.of("false", "general-availability");
        assertTrue(nativeRequired.required(DeploymentMode.NATIVE_IMAGE));
        assertEquals(Set.of("UNAPPROVED_SKIP example.NativeImageSmokeTest smoke"),
                     keys(check(nativeRequired, approvals())));
    }

    @Test
    void anApprovalNamingNoExpectedSuiteOrMalformedIsInvalid() throws IOException {
        report(CONFORMANCE, null, 1.0, pass("factory()[1]"), pass("factory()[2]"));
        report(NATIVE, null, 0.1, skip("smoke"));
        Approvals approvals = approvals(NATIVE + " | * | native-image-not-required | optional | Task 18.9",
                                        "example.Gone | * | always | gone | ref", "only | three | fields",
                                        CONFORMANCE + " | x | sometimes | r | ref");
        assertEquals(Set.of("INVALID approvals -", "INVALID example.Gone -"), keys(check(CLASS_PATH_ONLY, approvals)));
        assertEquals(2, approvals.problems().size());
    }

    @Test
    void protocolEvidenceMustCoverEveryLaunchProtocolAndRunEveryApprovedOmissionAsApproved() throws IOException {
        report(CONFORMANCE, null, 1.0, pass("factory()[1]"), pass("factory()[2]"));
        report(NATIVE, null, 0.1, skip("smoke"));
        Path evidence = root.resolve("evidence.json");
        Files.writeString(evidence, "{\"totals\": {\"applicable\": 2, \"timed-out\": 1},"
                                    + " \"byProtocol\": {\"restJson1\": {\"applicable\": 2}},"
                                    + " \"approvedOmissions\": [{\"case\": \"c1\", \"result\": \"approved-omission\"},"
                                    + " {\"case\": \"c2\", \"result\": \"passed\"}]}");
        ProtocolEvidence protocols = new ProtocolEvidence(evidence, CONFORMANCE, "factory()",
                                                          List.of("aws.protocols#restJson1", "aws.protocols#restXml"));
        List<Finding> findings = ReleaseGateEnforcement.check(root, inventory(protocols), approvals(), CLASS_PATH_ONLY,
                                                              buildStart);
        assertEquals(Set.of("MISSING example.ConformanceTest -", "TIMED_OUT example.ConformanceTest -",
                            "FAILED example.ConformanceTest c2"), keys(findings));
        assertTrue(findings.stream().anyMatch(f -> f.detail().contains("aws.protocols#restXml")), findings::toString);
    }

    @Test
    void inventoryProblemsAreInvalid() {
        Inventory broken = new Inventory(List.of(), null, List.of(), List.of(), List.of(),
                                         List.of("protocol runtime protocols/x has no *ConformanceTest suite"));
        Approvals none = ReleaseGateEnforcement.parseApprovals(List.of(), "none");
        assertEquals(Set.of("INVALID inventory -"), keys(ReleaseGateEnforcement.check(root, broken, none,
                                                                                     CLASS_PATH_ONLY, buildStart)));
    }

    // ---------------------------------------------------------------------------------------------------------------

    private List<Finding> check(DeploymentModes modes, Approvals approvals) {
        return ReleaseGateEnforcement.check(root, inventory(null), approvals, modes, buildStart);
    }

    private static Inventory inventory(ProtocolEvidence evidence) {
        ExpectedSuite conformance = new ExpectedSuite(
            new Suite("m", CONFORMANCE), "conformance", new TreeSet<>(Set.of("protocol:p")),
            List.of(new ExpectedCase("factory()[1]", "service 1 of 2"), new ExpectedCase("factory()[2]",
                                                                                          "service 2 of 2")),
            DeploymentMode.CLASS_PATH, Duration.ofSeconds(30), Duration.ofSeconds(600));
        ExpectedSuite nativeImage = new ExpectedSuite(
            new Suite("m", NATIVE), "native-image", new TreeSet<>(Set.of("deployment:native-image")), List.of(),
            DeploymentMode.NATIVE_IMAGE, Duration.ofSeconds(600), Duration.ofSeconds(600));
        return new Inventory(List.of(conformance, nativeImage), evidence, List.of(), List.of(), List.of(), List.of());
    }

    /** The checked-in record's conditional native-image approval, plus {@code extra}. */
    private static Approvals approvals(String... lines) {
        List<String> all = lines.length > 0 ? List.of(lines)
                                            : List.of(NATIVE + " | * | native-image-not-required | optional | Task 18.9");
        return ReleaseGateEnforcement.parseApprovals(all, "test");
    }

    private static Set<String> keys(List<Finding> findings) {
        return findings.stream().map(f -> f.kind() + " " + f.suite() + " " + (f.testcase() == null ? "-"
                                                                                                    : f.testcase()))
                       .collect(Collectors.toCollection(TreeSet::new));
    }

    private void report(String className, String suffix, double time, String... testcases) throws IOException {
        Suite suite = new Suite("m", className, suffix);
        long failures = List.of(testcases).stream().filter(t -> t.contains("<failure")).count();
        long errors = List.of(testcases).stream().filter(t -> t.contains("<error")).count();
        long skipped = List.of(testcases).stream().filter(t -> t.contains("<skipped")).count();
        Path report = suite.report(root);
        Files.createDirectories(report.getParent());
        Files.writeString(report, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuite name=\""
                                  + suite.reportedName() + "\" time=\"" + time + "\" tests=\"" + testcases.length
                                  + "\" errors=\"" + errors + "\" skipped=\"" + skipped + "\" failures=\"" + failures
                                  + "\">\n" + String.join("\n", testcases) + "\n</testsuite>\n");
    }

    private static String pass(String name) {
        return pass(name, 0.01);
    }

    private static String pass(String name, double time) {
        return "<testcase name=\"" + name + "\" classname=\"c\" time=\"" + time + "\"/>";
    }

    private static String skip(String name) {
        return "<testcase name=\"" + name + "\" classname=\"c\" time=\"0\"><skipped message=\"assumption\"/>"
               + "</testcase>";
    }

    private static String fail(String name, String element, String type, String message) {
        return "<testcase name=\"" + name + "\" classname=\"c\" time=\"0.1\"><" + element + " type=\"" + type
               + "\" message=\"" + message + "\">trace</" + element + "></testcase>";
    }
}
