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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.test.reactor.CoreRuntimeSuiteReports.Suite;

/** Unit tests of the core-runtime suite gate logic against synthetic surefire reports. */
class CoreRuntimeSuiteReportsCheckTest {
    private static final Suite SYNC = new Suite("core/example", "software.amazon.awssdk.example.SyncTest");
    private static final Suite ASYNC = new Suite("test/example-tests", "software.amazon.awssdk.example.AsyncTest");

    @TempDir
    Path v3Root;

    private Instant buildStart;

    @BeforeEach
    void startBuild() {
        // Whole seconds, so the comparison does not depend on the file system's timestamp precision.
        buildStart = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    void passesWhenEverySuiteRanAndPassedInThisBuild() throws IOException {
        report(SYNC, 4, 0, 0, 1);
        report(ASYNC, 1, 0, 0, 0);
        assertEquals(List.of(), CoreRuntimeSuiteReports.problems(v3Root, List.of(SYNC, ASYNC), buildStart));
    }

    @Test
    void missingReportFails() throws IOException {
        report(SYNC, 4, 0, 0, 0);
        List<String> problems = CoreRuntimeSuiteReports.problems(v3Root, List.of(SYNC, ASYNC), buildStart);
        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).startsWith(ASYNC + ": no surefire report "
                                              + "test/example-tests/target/surefire-reports/TEST-"
                                              + ASYNC.className() + ".xml"), problems.get(0));
    }

    @Test
    void reportWithZeroTestsFails() throws IOException {
        report(SYNC, 0, 0, 0, 0);
        assertEquals(List.of(SYNC + ": executed no tests (tests=0, skipped=0)"),
                     CoreRuntimeSuiteReports.problems(v3Root, List.of(SYNC), buildStart));
    }

    @Test
    void reportWhoseTestsWereAllSkippedFails() throws IOException {
        report(SYNC, 3, 0, 0, 3);
        assertEquals(List.of(SYNC + ": executed no tests (tests=3, skipped=3)"),
                     CoreRuntimeSuiteReports.problems(v3Root, List.of(SYNC), buildStart));
    }

    @Test
    void reportWithFailuresFails() throws IOException {
        report(SYNC, 5, 2, 0, 0);
        assertEquals(List.of(SYNC + ": 2 failure(s) and 0 error(s) of 5 test(s)"),
                     CoreRuntimeSuiteReports.problems(v3Root, List.of(SYNC), buildStart));
    }

    @Test
    void reportWithErrorsFails() throws IOException {
        report(SYNC, 5, 0, 1, 0);
        assertEquals(List.of(SYNC + ": 0 failure(s) and 1 error(s) of 5 test(s)"),
                     CoreRuntimeSuiteReports.problems(v3Root, List.of(SYNC), buildStart));
    }

    @Test
    void failedTestcaseFailsEvenWhenTheSummaryAttributesClaimSuccess() throws IOException {
        write(SYNC, "<testsuite name=\"" + SYNC.className() + "\" tests=\"1\" errors=\"0\" skipped=\"0\" "
                    + "failures=\"0\"><testcase name=\"t\"><failure message=\"boom\"/></testcase></testsuite>");
        assertEquals(List.of(SYNC + ": 1 failure(s) and 0 error(s) of 1 test(s)"),
                     CoreRuntimeSuiteReports.problems(v3Root, List.of(SYNC), buildStart));
    }

    @Test
    void staleReportFromAnEarlierBuildFails() throws IOException {
        Path report = report(SYNC, 4, 0, 0, 0);
        Instant earlier = buildStart.minus(Duration.ofHours(1));
        Files.setLastModifiedTime(report, FileTime.from(earlier));
        assertEquals(List.of(SYNC + ": stale surefire report core/example/target/surefire-reports/TEST-"
                             + SYNC.className() + ".xml written at " + earlier + ", before this build started at "
                             + buildStart + " (the suite did not run in this build: tests skipped, resumed build, or "
                             + "filtered out)"),
                     CoreRuntimeSuiteReports.problems(v3Root, List.of(SYNC), buildStart));
    }

    @Test
    void reportWithinTheTimestampToleranceIsFresh() throws IOException {
        Path report = report(SYNC, 4, 0, 0, 0);
        Files.setLastModifiedTime(report, FileTime.from(buildStart.minusSeconds(1)));
        assertEquals(List.of(), CoreRuntimeSuiteReports.problems(v3Root, List.of(SYNC), buildStart));
    }

    @Test
    void reportForAnotherClassOrMalformedReportFails() throws IOException {
        write(SYNC, "<testsuite name=\"software.amazon.awssdk.example.Other\" tests=\"1\"/>");
        write(ASYNC, "<testsuite name=");
        List<String> problems = CoreRuntimeSuiteReports.problems(v3Root, List.of(SYNC, ASYNC), buildStart);
        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.get(0).startsWith(SYNC + ": surefire report ")
                   && problems.get(0).endsWith(" is for 'software.amazon.awssdk.example.Other'"), problems.get(0));
        assertTrue(problems.get(1).startsWith(ASYNC + ": unreadable surefire report "), problems.get(1));
    }

    @Test
    void everyBrokenSuiteIsReportedAtOnce() throws IOException {
        Suite failed = new Suite("core/example", "software.amazon.awssdk.example.FailedTest");
        Suite empty = new Suite("core/example", "software.amazon.awssdk.example.EmptyTest");
        Suite stale = new Suite("core/example", "software.amazon.awssdk.example.StaleTest");
        Suite passed = new Suite("core/example", "software.amazon.awssdk.example.PassedTest");
        report(failed, 2, 1, 0, 0);
        report(empty, 0, 0, 0, 0);
        Files.setLastModifiedTime(report(stale, 2, 0, 0, 0), FileTime.from(buildStart.minus(Duration.ofDays(1))));
        report(passed, 2, 0, 0, 0);

        List<String> problems = CoreRuntimeSuiteReports.problems(
            v3Root, List.of(SYNC, failed, empty, stale, passed), buildStart);

        assertEquals(List.of(SYNC.toString(), failed.toString(), empty.toString(), stale.toString()),
                     problems.stream().map(p -> p.substring(0, p.indexOf(": "))).toList());
    }

    @Test
    void parsesTheSuiteListIgnoringCommentsAndBlankLines() {
        List<Suite> suites = CoreRuntimeSuiteReports.parse(List.of(
            "# Task 6",
            "",
            "core/example software.amazon.awssdk.example.SyncTest   # trailing comment",
            "  test/example-tests\tsoftware.amazon.awssdk.example.AsyncTest  "), "suites.txt");
        assertEquals(List.of(SYNC, ASYNC), suites);
    }

    @Test
    void rejectsMalformedDuplicateAndEmptySuiteLists() {
        IllegalArgumentException malformed = assertThrows(IllegalArgumentException.class, () ->
            CoreRuntimeSuiteReports.parse(List.of("core/example", "../escape software.amazon.awssdk.example.SyncTest",
                                                  "core/example SyncTest",
                                                  "core/example software.amazon.awssdk.example.SyncTest",
                                                  "core/other software.amazon.awssdk.example.SyncTest"),
                                          "suites.txt"));
        String message = malformed.getMessage();
        for (String expected : List.of("suites.txt:1:", "suites.txt:2:", "suites.txt:3:",
                                       "suites.txt:5: duplicate suite software.amazon.awssdk.example.SyncTest")) {
            assertTrue(message.contains(expected), message);
        }
        assertThrows(IllegalArgumentException.class, () -> CoreRuntimeSuiteReports.parse(List.of("# none"), "x"));
    }

    @Test
    void parsesTheMavenSessionStartAndRejectsAnUninterpolatedValue() {
        assertEquals(Instant.parse("2026-10-01T12:34:56.789Z"),
                     CoreRuntimeSuiteReports.parseBuildStart("2026-10-01T12:34:56.789Z"));
        assertEquals(Instant.parse("2026-10-01T10:34:56.789Z"),
                     CoreRuntimeSuiteReports.parseBuildStart("2026-10-01T12:34:56.789+02:00"));
        for (String invalid : new String[] {null, "", "${maven.build.timestamp}", "20261001-1234"}) {
            assertThrows(IllegalArgumentException.class, () -> CoreRuntimeSuiteReports.parseBuildStart(invalid));
        }
    }

    private Path report(Suite suite, int tests, int failures, int errors, int skipped) throws IOException {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuite name=\"")
            .append(suite.className()).append("\" time=\"0.1\" tests=\"").append(tests).append("\" errors=\"")
            .append(errors).append("\" skipped=\"").append(skipped).append("\" failures=\"").append(failures)
            .append("\">\n");
        for (int i = 0; i < tests; i++) {
            xml.append("  <testcase name=\"t").append(i).append("\" classname=\"").append(suite.className())
               .append("\">");
            if (i < failures) {
                xml.append("<failure message=\"f\"/>");
            } else if (i < failures + errors) {
                xml.append("<error message=\"e\"/>");
            } else if (i >= tests - skipped) {
                xml.append("<skipped/>");
            }
            xml.append("</testcase>\n");
        }
        return write(suite, xml.append("</testsuite>\n").toString());
    }

    private Path write(Suite suite, String content) throws IOException {
        Path report = suite.report(v3Root);
        Files.createDirectories(report.getParent());
        Files.writeString(report, content);
        return report;
    }
}
