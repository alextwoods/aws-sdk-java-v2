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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.w3c.dom.Element;

/**
 * The core-runtime suite gate (task 11.1): checks that every required suite produced a surefire report during the
 * current Maven session, executed at least one test, and recorded no failures or errors.
 *
 * <p>A required suite is a {@code <module> <class>} pair, where {@code module} is the module directory relative to the
 * V3 root. Its report is {@code <module>/target/surefire-reports/TEST-<class>.xml}; no V3 module uses failsafe or a
 * custom reports directory.
 *
 * <p><b>Freshness.</b> Surefire's XML carries no run timestamp and never deletes reports of suites that did not run, so
 * a report on disk proves nothing by itself: it may come from an earlier build, a build resumed with {@code -rf}, or a
 * build that skipped tests. The gate therefore compares the report's last-modified time with the start of the current
 * Maven session ({@code ${maven.build.timestamp}}, which Maven fixes once per session before any module builds) and
 * treats every report written before it as missing. {@link #MTIME_TOLERANCE} absorbs file systems that store
 * modification times at coarse (up to 2 s) granularity.
 */
public final class CoreRuntimeSuiteReports {
    /** Allowance for file systems that truncate modification times (FAT: 2 s; HFS+, ext3: 1 s). */
    public static final Duration MTIME_TOLERANCE = Duration.ofSeconds(2);

    private static final Pattern MODULE = Pattern.compile("[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)*");
    private static final Pattern CLASS_NAME = Pattern.compile("([a-z_][A-Za-z0-9_]*\\.)+[A-Z][A-Za-z0-9_]*");

    private CoreRuntimeSuiteReports() {
    }

    /**
     * One required suite.
     *
     * @param module module directory relative to the V3 root, with {@code /} separators
     * @param className fully qualified name of the top-level test class
     */
    public record Suite(String module, String className) {
        /** The surefire XML report this suite writes, under {@code v3Root}. */
        public Path report(Path v3Root) {
            return v3Root.resolve(module).resolve("target/surefire-reports").resolve("TEST-" + className + ".xml");
        }

        /** The test source file of this suite, under {@code v3Root}. */
        public Path source(Path v3Root) {
            return v3Root.resolve(module).resolve("src/test/java").resolve(className.replace('.', '/') + ".java");
        }

        public String simpleName() {
            return className.substring(className.lastIndexOf('.') + 1);
        }

        @Override
        public String toString() {
            return module + " " + className;
        }
    }

    /** Reads a suite list: one {@code <module> <class>} pair per line; blank lines and {@code #} comments ignored. */
    public static List<Suite> parse(Path suiteList) {
        try {
            return parse(Files.readAllLines(suiteList), suiteList.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + suiteList, e);
        }
    }

    static List<Suite> parse(List<String> lines, String source) {
        List<Suite> suites = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int comment = line.indexOf('#');
            String content = (comment < 0 ? line : line.substring(0, comment)).strip();
            if (content.isEmpty()) {
                continue;
            }
            String[] fields = content.split("\\s+");
            String location = source + ":" + (i + 1);
            if (fields.length != 2 || !MODULE.matcher(fields[0]).matches() || fields[0].contains("..")
                || !CLASS_NAME.matcher(fields[1]).matches()) {
                problems.add(location + ": expected '<module directory> <fully qualified class>', got '" + content
                             + "'");
            } else if (!seen.add(fields[1])) {
                problems.add(location + ": duplicate suite " + fields[1]);
            } else {
                suites.add(new Suite(fields[0], fields[1]));
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Invalid suite list:\n  " + String.join("\n  ", problems));
        }
        if (suites.isEmpty()) {
            throw new IllegalArgumentException(source + " lists no suites");
        }
        return List.copyOf(suites);
    }

    /**
     * Parses the session start as interpolated from {@code ${maven.build.timestamp}} with the ISO-8601 offset format
     * {@code yyyy-MM-dd'T'HH:mm:ss.SSSXXX} (set by release-reactor-tests' core-runtime-gate profile).
     */
    public static Instant parseBuildStart(String value) {
        if (value == null || value.isBlank() || value.contains("${")) {
            throw new IllegalArgumentException("build start time is not set (got '" + value + "'); run the gate "
                                               + "through the core-runtime-gate profile (-Dv3.aggregate=core-runtime)");
        }
        try {
            return OffsetDateTime.parse(value.strip()).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("build start time '" + value + "' is not an ISO-8601 offset date-time",
                                               e);
        }
    }

    /**
     * Returns one problem per suite that is missing, stale, unreadable, executed no tests, or failed; an empty list
     * means every suite ran and passed in the build that started at {@code buildStart}.
     */
    public static List<String> problems(Path v3Root, List<Suite> suites, Instant buildStart) {
        Instant freshAfter = buildStart.minus(MTIME_TOLERANCE);
        List<String> problems = new ArrayList<>();
        for (Suite suite : suites) {
            String problem = problem(v3Root, suite, buildStart, freshAfter);
            if (problem != null) {
                problems.add(suite + ": " + problem);
            }
        }
        return problems;
    }

    private static String problem(Path v3Root, Suite suite, Instant buildStart, Instant freshAfter) {
        Path report = suite.report(v3Root);
        if (!Files.isRegularFile(report)) {
            return "no surefire report " + v3Root.relativize(report) + " (the suite did not run in this build: "
                   + "renamed, filtered out, excluded, or its module was skipped)";
        }
        Instant modified;
        try {
            modified = Files.getLastModifiedTime(report).toInstant();
        } catch (IOException e) {
            return "cannot read the modification time of " + report + ": " + e;
        }
        if (modified.isBefore(freshAfter)) {
            return "stale surefire report " + v3Root.relativize(report) + " written at " + modified
                   + ", before this build started at " + buildStart
                   + " (the suite did not run in this build: tests skipped, resumed build, or filtered out)";
        }
        Element testsuite;
        try {
            testsuite = Xml.read(report);
        } catch (RuntimeException e) {
            return "unreadable surefire report " + report + ": " + e.getMessage();
        }
        if (!"testsuite".equals(testsuite.getTagName())) {
            return "surefire report " + report + " has root element <" + testsuite.getTagName()
                   + ">, expected <testsuite>";
        }
        if (!suite.className().equals(testsuite.getAttribute("name"))) {
            return "surefire report " + report + " is for '" + testsuite.getAttribute("name") + "'";
        }
        try {
            long tests = count(testsuite, "tests");
            long skipped = count(testsuite, "skipped");
            // The <testcase> elements are counted too, so a report whose summary attributes disagree with its body
            // cannot hide a failure.
            long failures = Math.max(count(testsuite, "failures"), testcaseResults(testsuite, "failure"));
            long errors = Math.max(count(testsuite, "errors"), testcaseResults(testsuite, "error"));
            if (failures > 0 || errors > 0) {
                return failures + " failure(s) and " + errors + " error(s) of " + tests + " test(s)";
            }
            if (tests - skipped <= 0) {
                return "executed no tests (tests=" + tests + ", skipped=" + skipped + ")";
            }
        } catch (NumberFormatException e) {
            return "surefire report " + report + " has a malformed count: " + e.getMessage();
        }
        return null;
    }

    private static long count(Element testsuite, String attribute) {
        String value = testsuite.getAttribute(attribute);
        return value.isEmpty() ? 0 : Long.parseLong(value.strip());
    }

    private static long testcaseResults(Element testsuite, String result) {
        return Xml.children(testsuite, "testcase").stream()
                  .filter(testcase -> Xml.child(testcase, result) != null)
                  .count();
    }
}
