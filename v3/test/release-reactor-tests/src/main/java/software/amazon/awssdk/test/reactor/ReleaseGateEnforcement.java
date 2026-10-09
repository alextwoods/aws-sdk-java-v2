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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.w3c.dom.Element;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.DeploymentMode;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.DeploymentModes;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.ExpectedCase;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.ExpectedSuite;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.Inventory;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.ProtocolEvidence;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The release-gate enforcement of Requirements 16.14, 16.15, 19.14 and 19.15: checks the generated
 * {@link ReleaseGateInventory} against the surefire reports of the current Maven session, case by case, and fails
 * closed. A finding is raised for
 *
 * <ul>
 *   <li>{@link Kind#MISSING}: an expected suite with no fresh report (not run, filtered, resumed build, or a fork killed
 *       by its timeout), an expected case absent from its suite's report, a suite that executed nothing, or pinned
 *       protocol evidence that does not cover every launch protocol;</li>
 *   <li>{@link Kind#FAILED}: a failure or error of any case, a report whose summary counts more failures or errors than
 *       its cases show, or an approved conformance omission that did not run as approved;</li>
 *   <li>{@link Kind#TIMED_OUT}: a case whose failure is a timeout, a case that ran longer than its suite's case bound,
 *       or a suite that ran longer than its suite bound;</li>
 *   <li>{@link Kind#UNAPPROVED_SKIP}: a skipped case not covered by an {@link Approval} of the release-gate omission
 *       record whose condition holds for the build's {@link DeploymentModes};</li>
 *   <li>{@link Kind#INVALID}: an inconsistency of the inventory's sources, or an approval that is malformed or names no
 *       expected suite.</li>
 * </ul>
 */
public final class ReleaseGateEnforcement {
    private static final Pattern TIMEOUT_MESSAGE = Pattern.compile(
        "(?i)\\btimed out after\\b|\\bdid not finish within\\b|\\bexecution timed out\\b");

    private ReleaseGateEnforcement() {
    }

    /** The kinds of release-gate finding. */
    public enum Kind {
        MISSING, FAILED, TIMED_OUT, UNAPPROVED_SKIP, INVALID
    }

    /** One release-gate finding; {@code testcase} is null for a suite-level finding. */
    public record Finding(Kind kind, String suite, String testcase, String detail) {
        @Override
        public String toString() {
            return kind + " " + suite + (testcase == null ? "" : " [" + testcase + "]") + ": " + detail;
        }
    }

    /** The condition under which an approved omission applies. */
    public enum Condition {
        /** The skip is approved in every build. */
        ALWAYS("always"),
        /** The skip is approved only while the native-image deployment mode is not required. */
        NATIVE_IMAGE_NOT_REQUIRED("native-image-not-required");

        private final String id;

        Condition(String id) {
            this.id = id;
        }

        boolean holds(DeploymentModes modes) {
            return this == ALWAYS || !modes.required(DeploymentMode.NATIVE_IMAGE);
        }

        static Optional<Condition> of(String id) {
            for (Condition c : values()) {
                if (c.id.equals(id)) {
                    return Optional.of(c);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * One approved release-gate omission: skipped cases of {@code suite} whose names match {@code casePattern} ({@code *}
     * matches any characters) are approved while {@code condition} holds.
     */
    public record Approval(String suite, String casePattern, Condition condition, String reason, String reference,
                           int line) {
        boolean covers(String suiteClass, String testcase, DeploymentModes modes) {
            return suite.equals(suiteClass) && wildcard(casePattern).matcher(testcase).matches()
                   && condition.holds(modes);
        }
    }

    /** A parsed omission record: its approvals and its malformed lines. */
    public record Approvals(List<Approval> approvals, List<String> problems) {
    }

    /**
     * Parses a release-gate omission record: one {@code <suite class> | <case pattern> | <condition> | <reason> |
     * <reference>} entry per line; blank lines and {@code #} comment lines are ignored.
     */
    public static Approvals parseApprovals(List<String> lines, String source) {
        List<Approval> approvals = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i).strip();
            if (text.isEmpty() || text.startsWith("#")) {
                continue;
            }
            String[] f = text.split("\\s+\\|\\s+", -1);
            String location = source + ":" + (i + 1);
            if (f.length != 5 || java.util.Arrays.stream(f).anyMatch(String::isBlank)) {
                problems.add(location + ": expected '<suite class> | <case pattern> | <condition> | <reason> | "
                             + "<reference>'");
                continue;
            }
            Optional<Condition> condition = Condition.of(f[2].strip());
            if (condition.isEmpty()) {
                problems.add(location + ": unknown condition '" + f[2].strip() + "'");
                continue;
            }
            approvals.add(new Approval(f[0].strip(), f[1].strip(), condition.get(), f[3].strip(), f[4].strip(),
                                       i + 1));
        }
        return new Approvals(List.copyOf(approvals), List.copyOf(problems));
    }

    /** Checks {@code inventory} against the reports of the build that started at {@code buildStart}. */
    public static List<Finding> check(Path v3Root, Inventory inventory, Approvals approvals, DeploymentModes modes,
                                      Instant buildStart) {
        List<Finding> findings = new ArrayList<>();
        inventory.problems().forEach(p -> findings.add(new Finding(Kind.INVALID, "inventory", null, p)));
        approvals.problems().forEach(p -> findings.add(new Finding(Kind.INVALID, "approvals", null, p)));
        Set<String> expected = inventory.suites().stream().map(ExpectedSuite::className).collect(Collectors.toSet());
        for (Approval approval : approvals.approvals()) {
            if (!expected.contains(approval.suite())) {
                findings.add(new Finding(Kind.INVALID, approval.suite(), null, "approved omission (line "
                                         + approval.line() + ") names no expected release-gate suite"));
            }
        }
        Map<String, Element> reports = new LinkedHashMap<>();
        for (ExpectedSuite suite : inventory.suites()) {
            Element report = checkSuite(v3Root, suite, approvals, modes, buildStart, findings);
            if (report != null) {
                reports.put(suite.className(), report);
            }
        }
        ProtocolEvidence evidence = inventory.protocolEvidence();
        if (evidence != null && expected.contains(evidence.suiteClass())) {
            checkProtocolEvidence(evidence, reports.get(evidence.suiteClass()), buildStart, findings);
        }
        return findings;
    }

    private static Element checkSuite(Path v3Root, ExpectedSuite expected, Approvals approvals, DeploymentModes modes,
                                      Instant buildStart, List<Finding> findings) {
        String suite = expected.suite().reportedName();
        CoreRuntimeSuiteReports.FreshReport fresh = CoreRuntimeSuiteReports.freshReport(v3Root, expected.suite(),
                                                                                         buildStart);
        if (fresh.problem() != null) {
            findings.add(new Finding(Kind.MISSING, suite, null, fresh.problem()));
            return null;
        }
        Element testsuite = fresh.testsuite();
        Duration suiteTime = seconds(testsuite.getAttribute("time"));
        if (suiteTime.compareTo(expected.suiteTimeout()) > 0) {
            findings.add(new Finding(Kind.TIMED_OUT, suite, null, "ran " + suiteTime + ", over the suite bound "
                                     + expected.suiteTimeout()));
        }
        Map<String, Element> cases = new LinkedHashMap<>();
        int executed = 0;
        int approvedSkips = 0;
        int unapprovedSkips = 0;
        long failedCases = 0;
        for (Element testcase : Xml.children(testsuite, "testcase")) {
            String name = testcase.getAttribute("name");
            cases.putIfAbsent(name, testcase);
            Element failure = Optional.ofNullable(Xml.child(testcase, "failure"))
                                      .orElse(Xml.child(testcase, "error"));
            if (failure != null) {
                failedCases++;
                boolean timeout = failure.getAttribute("type").contains("TimeoutException")
                                  || TIMEOUT_MESSAGE.matcher(failure.getAttribute("message")).find();
                findings.add(new Finding(timeout ? Kind.TIMED_OUT : Kind.FAILED, suite, name,
                                         failure.getTagName() + " " + abbreviate(failure.getAttribute("type") + ": "
                                                                                 + failure.getAttribute("message"))));
                continue;
            }
            if (Xml.child(testcase, "skipped") != null) {
                Optional<Approval> approval = approvals.approvals().stream()
                                                       .filter(a -> a.covers(expected.className(), name, modes))
                                                       .findFirst();
                if (approval.isPresent()) {
                    approvedSkips++;
                } else {
                    unapprovedSkips++;
                    findings.add(new Finding(Kind.UNAPPROVED_SKIP, suite, name, "skipped without an approved "
                                             + "omission (" + abbreviate(Xml.child(testcase, "skipped")
                                                                            .getAttribute("message")) + ")"));
                }
                continue;
            }
            executed++;
            Duration time = seconds(testcase.getAttribute("time"));
            if (time.compareTo(expected.caseTimeout()) > 0) {
                findings.add(new Finding(Kind.TIMED_OUT, suite, name, "ran " + time + ", over the case bound "
                                         + expected.caseTimeout()));
            }
        }
        long reported = count(testsuite, "failures") + count(testsuite, "errors");
        if (reported > failedCases) {
            findings.add(new Finding(Kind.FAILED, suite, null, "the report counts " + reported + " failures and "
                                     + "errors but only " + failedCases + " failed cases"));
        }
        // A suite whose every case is a failure or skip is already reported case by case.
        if (executed == 0 && failedCases == 0 && approvedSkips == 0 && unapprovedSkips == 0) {
            findings.add(new Finding(Kind.MISSING, suite, null, "executed no case"));
        }
        for (ExpectedCase c : expected.cases()) {
            if (!cases.containsKey(c.name())) {
                findings.add(new Finding(Kind.MISSING, suite, c.name(), "expected case (" + c.source()
                                         + ") is absent from the report"));
            }
        }
        return testsuite;
    }

    private static void checkProtocolEvidence(ProtocolEvidence evidence, Element testsuite, Instant buildStart,
                                              List<Finding> findings) {
        String suite = evidence.suiteClass();
        Path report = evidence.report();
        if (!Files.isRegularFile(report)) {
            findings.add(new Finding(Kind.MISSING, suite, null, "no protocol conformance evidence " + report));
            return;
        }
        try {
            if (Files.getLastModifiedTime(report).toInstant()
                     .isBefore(buildStart.minus(CoreRuntimeSuiteReports.MTIME_TOLERANCE))) {
                findings.add(new Finding(Kind.MISSING, suite, null, "stale protocol conformance evidence " + report));
                return;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ObjectNode root;
        try {
            root = Node.parse(Files.readString(report)).expectObjectNode();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (RuntimeException e) {
            findings.add(new Finding(Kind.MISSING, suite, null, "unreadable protocol conformance evidence " + report
                                     + ": " + e.getMessage()));
            return;
        }
        ObjectNode totals = root.getObjectMember("totals").orElse(Node.objectNode());
        ObjectNode byProtocol = root.getObjectMember("byProtocol").orElse(Node.objectNode());
        for (String protocol : evidence.launchProtocols()) {
            String name = protocol.substring(protocol.indexOf('#') + 1);
            long applicable = byProtocol.getObjectMember(name).flatMap(p -> p.getNumberMember("applicable"))
                                        .map(n -> n.getValue().longValue()).orElse(0L);
            if (applicable == 0) {
                findings.add(new Finding(Kind.MISSING, suite, null, "launch protocol " + protocol
                                         + " has no applicable pinned conformance case"));
            }
        }
        long notRun = number(totals, "not-run");
        if (notRun > 0) {
            findings.add(new Finding(Kind.MISSING, suite, null, notRun + " applicable pinned cases did not run"));
        }
        long timedOut = number(totals, "timed-out");
        if (timedOut > 0) {
            findings.add(new Finding(Kind.TIMED_OUT, suite, null, timedOut + " pinned cases timed out"));
        }
        root.getArrayMember("approvedOmissions").ifPresent(a -> a.getElements().forEach(node -> {
            ObjectNode omission = node.expectObjectNode();
            String result = omission.getStringMemberOrDefault("result", "not-run");
            if (!result.equals("approved-omission")) {
                findings.add(new Finding(Kind.FAILED, suite, omission.getStringMemberOrDefault("case", "?"),
                                         "approved conformance omission did not run as approved (" + result + ")"));
            }
        }));
        long applicable = number(totals, "applicable");
        if (testsuite != null) {
            long dynamic = Xml.children(testsuite, "testcase").stream()
                              .filter(t -> t.getAttribute("name").startsWith(evidence.caseFactory() + "["))
                              .count();
            if (dynamic != applicable) {
                findings.add(new Finding(Kind.MISSING, suite, null, "the report has " + dynamic + " "
                                         + evidence.caseFactory() + " cases, the evidence " + applicable
                                         + " applicable cases"));
            }
        }
    }

    /** One line per finding kind and suite, with the first cases of each, for a readable gate failure. */
    public static String summarize(List<Finding> findings, int casesPerGroup) {
        Map<String, List<Finding>> groups = new TreeMap<>();
        findings.forEach(f -> groups.computeIfAbsent(f.kind() + " " + f.suite(), k -> new ArrayList<>()).add(f));
        StringBuilder out = new StringBuilder();
        Map<Kind, Long> byKind = new TreeMap<>(findings.stream().collect(Collectors.groupingBy(Finding::kind,
                                                                                               Collectors.counting())));
        out.append(findings.size()).append(" release-gate findings ").append(byKind).append('\n');
        groups.forEach((group, list) -> {
            out.append("  ").append(group).append(" (").append(list.size()).append(")\n");
            list.stream().limit(casesPerGroup).forEach(
                f -> out.append("      ").append(f.testcase() == null ? "" : "[" + f.testcase() + "] ")
                        .append(f.detail()).append('\n'));
            if (list.size() > casesPerGroup) {
                out.append("      ... ").append(list.size() - casesPerGroup).append(" more\n");
            }
        });
        return out.toString();
    }

    private static Duration seconds(String value) {
        if (value == null || value.isBlank()) {
            return Duration.ZERO;
        }
        try {
            return Duration.ofMillis(Math.round(Double.parseDouble(value.strip().replace(",", "")) * 1000));
        } catch (NumberFormatException e) {
            return Duration.ZERO;
        }
    }

    private static long count(Element testsuite, String attribute) {
        String value = testsuite.getAttribute(attribute);
        try {
            return value.isBlank() ? 0 : Long.parseLong(value.strip());
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    private static long number(ObjectNode node, String member) {
        return node.getNumberMember(member).map(n -> n.getValue().longValue()).orElse(0L);
    }

    private static String abbreviate(String text) {
        String line = text == null ? "" : text.strip().replaceAll("\\s+", " ");
        return line.length() > 200 ? line.substring(0, 200) + "..." : line;
    }

    static Pattern wildcard(String pattern) {
        return Pattern.compile(java.util.Arrays.stream(pattern.split("\\*", -1)).map(Pattern::quote)
                                               .collect(Collectors.joining(".*")));
    }
}
