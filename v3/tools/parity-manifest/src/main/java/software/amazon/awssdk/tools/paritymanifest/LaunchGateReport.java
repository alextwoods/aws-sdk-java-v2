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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The outcome of one {@link LaunchGate} evaluation: per-category launch-surface comparison and every blocking problem.
 *
 * @param stage      the evaluated stage
 * @param candidate  the manifest's candidate identity, or {@code null}
 * @param categories one summary per category that has at least one manifest entry, in category order
 * @param problems   every blocking problem, sorted
 */
public record LaunchGateReport(LaunchGate.Stage stage, CandidateIdentity candidate, List<CategorySummary> categories,
                               List<LaunchProblem> problems) {
    /** The schema version of the JSON form. */
    public static final int SCHEMA_VERSION = 1;

    /**
     * Counts for one launch category. Every count except {@code deprecated} is over non-deprecated entries, the
     * entries that gate the release (16.15).
     *
     * @param category   the category
     * @param deprecated deprecated entries (reported, not gated except for ADAPTED replacements, 16.12)
     * @param entries    non-deprecated entries
     * @param preserved  non-deprecated {@code PRESERVED} entries
     * @param adapted    non-deprecated {@code ADAPTED} entries
     * @param excluded   non-deprecated {@code EXCLUDED} entries
     * @param shipped    non-deprecated entries whose capability (PRESERVED) or replacement (ADAPTED) ships
     * @param hooked     non-deprecated entries with at least one evidence hook
     * @param verified   non-deprecated entries with passing candidate evidence (0 below general availability)
     * @param blocked    entries with at least one problem
     */
    public record CategorySummary(LaunchCategory category, int deprecated, int entries, int preserved, int adapted,
                                  int excluded, int shipped, int hooked, int verified, int blocked) {
        public CategorySummary {
            Objects.requireNonNull(category, "category");
        }
    }

    public LaunchGateReport {
        Objects.requireNonNull(stage, "stage");
        categories = categories.stream().sorted(Comparator.comparing(CategorySummary::category)).toList();
        problems = problems.stream().sorted().toList();
    }

    /** True if nothing blocks {@link #stage()}. */
    public boolean passed() {
        return problems.isEmpty();
    }

    /** A human-readable rendering: verdict, category table, then every problem. */
    public String render() {
        return render(Integer.MAX_VALUE);
    }

    /**
     * A human-readable rendering: verdict, problem counts by code, category table, then at most {@code maxProblems}
     * problems (and how many were omitted).
     */
    public String render(int maxProblems) {
        StringBuilder out = new StringBuilder();
        out.append("Launch gate ").append(stage.cliName()).append(": ").append(passed() ? "PASSED" : "BLOCKED")
           .append(" (").append(problems.size()).append(" problem(s))\n");
        out.append(String.format("  %-18s %10s %10s %10s %10s %10s %10s %10s %10s %10s\n", "category", "entries", "preserved",
                                 "adapted", "excluded", "shipped", "hooked", "verified", "blocked", "deprecated"));
        for (CategorySummary c : categories) {
            out.append(String.format("  %-18s %10d %10d %10d %10d %10d %10d %10d %10d %10d\n", c.category(), c.entries(),
                                     c.preserved(), c.adapted(), c.excluded(), c.shipped(), c.hooked(), c.verified(),
                                     c.blocked(), c.deprecated()));
        }
        problemCounts().forEach((code, n) -> out.append("  ").append(code).append(": ").append(n).append('\n'));
        out.append(problems.stream().limit(maxProblems).map(p -> "  " + p + "\n").collect(Collectors.joining()));
        if (problems.size() > maxProblems) {
            out.append("  ... and ").append(problems.size() - maxProblems).append(" more problem(s)\n");
        }
        return out.toString();
    }

    /** The number of problems per code, for codes that occur. */
    public SortedMap<LaunchProblem.Code, Integer> problemCounts() {
        SortedMap<LaunchProblem.Code, Integer> counts = new TreeMap<>();
        problems.forEach(p -> counts.merge(p.code(), 1, Integer::sum));
        return counts;
    }

    /** The canonical JSON form, ending with a newline. */
    public String toJson() {
        Node c = candidate == null
                 ? Node.nullNode()
                 : Node.objectNodeBuilder()
                       .withMember("v3Version", candidate.v3Version())
                       .withMember("gitRevision", candidate.gitRevision() == null
                                                  ? Node.nullNode() : Node.from(candidate.gitRevision()))
                       .build();
        ArrayNode cats = categories.stream()
                                   .map(s -> (Node) Node.objectNodeBuilder()
                                                        .withMember("category", s.category().name())
                                                        .withMember("entries", s.entries())
                                                        .withMember("preserved", s.preserved())
                                                        .withMember("adapted", s.adapted())
                                                        .withMember("excluded", s.excluded())
                                                        .withMember("shipped", s.shipped())
                                                        .withMember("hooked", s.hooked())
                                                        .withMember("verified", s.verified())
                                                        .withMember("blocked", s.blocked())
                                                        .withMember("deprecated", s.deprecated())
                                                        .build())
                                   .collect(ArrayNode.collect());
        ArrayNode probs = problems.stream()
                                  .map(p -> (Node) Node.objectNodeBuilder()
                                                       .withMember("code", p.code().name())
                                                       .withMember("stage", p.stage().name())
                                                       .withMember("subject", p.subject())
                                                       .withMember("category", p.category() == null
                                                                               ? Node.nullNode()
                                                                               : Node.from(p.category().name()))
                                                       .withMember("message", p.message())
                                                       .build())
                                  .collect(ArrayNode.collect());
        ObjectNode.Builder counts = Node.objectNodeBuilder();
        problemCounts().forEach((code, n) -> counts.withMember(code.name(), n));
        ObjectNode top = Node.objectNodeBuilder()
                             .withMember("schemaVersion", SCHEMA_VERSION)
                             .withMember("stage", stage.name())
                             .withMember("candidate", c)
                             .withMember("passed", passed())
                             .withMember("problemCounts", counts.build())
                             .withMember("categories", cats)
                             .withMember("problems", probs)
                             .build();
        return Node.prettyPrintJson(top) + "\n";
    }
}
