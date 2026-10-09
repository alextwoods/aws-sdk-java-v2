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

package software.amazon.awssdk.benchmark.harness;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Cell and collection validity from the per-repetition verdicts (Req 20.24, 20.25, 20.26, 20.28).
 *
 * <p>A cell is invalid when any compared arm (null arms included) has fewer than
 * {@link RepetitionSchedule#MIN_RELEASE_REPETITIONS} valid repetitions ({@link #TOO_FEW_VALID_REPETITIONS}); when any
 * repetition carries a cell-scoped violation, e.g. a steady-state gate that timed out; when an arm's artifact digest
 * changed between its repetitions ({@link #ARTIFACT_DRIFT}); when a null arm's artifact differs from its source arm's
 * ({@link #NULL_ARTIFACT}); or, in release mode, when the cell has no null arm ({@link #NULL_MISSING}).
 *
 * <p>The collection is invalid when any null experiment ratio (null mean / source mean over valid repetitions, for CPU
 * per operation, allocation per operation, p50, p99 and throughput) is outside {@link #NULL_RATIO_MIN} ..
 * {@link #NULL_RATIO_MAX} ({@link #NULL_RATIO}); when a null experiment could not be evaluated because one of its arms
 * has no valid repetition ({@link #NULL_NOT_EVALUABLE}); or when the schedule had structural problems
 * ({@link #SCHEDULE}). Whether invalid cells block release is 19.4's gate; the verdict lists them.
 */
public final class CollectionValidity {

    public static final String SCHEMA = "v3-benchmark-validity/1";

    public static final String TOO_FEW_VALID_REPETITIONS = "too-few-valid-repetitions";
    public static final String ARTIFACT_DRIFT = "artifact-drift";
    public static final String NULL_ARTIFACT = "null-artifact";
    public static final String NULL_MISSING = "null-missing";
    public static final String NULL_RATIO = "null-ratio";
    public static final String NULL_NOT_EVALUABLE = "null-not-evaluable";
    public static final String SCHEDULE = "schedule";

    public static final double NULL_RATIO_MIN = 0.95;
    public static final double NULL_RATIO_MAX = 1.05;

    private CollectionValidity() {
    }

    /**
     * The verdict of one run record.
     *
     * @param index the record's 1-based position in the collection
     * @param artifactDigest the client launch digest, or {@code null} when the record has none
     * @param metrics the null-experiment metrics, when the run measured anything
     */
    public record RepetitionVerdict(int index, String cellId, String armId, String nullOf, int repetition,
                                    String artifactDigest, List<Violation> violations,
                                    Optional<ValidityRules.Metrics> metrics) {
        public RepetitionVerdict {
            violations = List.copyOf(violations);
        }

        public boolean valid() {
            return violations.isEmpty();
        }

        public static RepetitionVerdict of(ObjectNode run, List<Violation> violations) {
            ObjectNode arm = run.getObjectMember("arm").orElse(Node.objectNode());
            return new RepetitionVerdict(
                (int) ValidityRules.num(run, "index"),
                run.expectObjectMember("cell").expectStringMember("id").getValue(),
                arm.getStringMember("id").map(s -> s.getValue()).orElse(BenchmarkArm.V3.id()),
                arm.getStringMember("null_of").map(s -> s.getValue()).orElse(null),
                (int) ValidityRules.num(run, "repetition"),
                arm.getStringMember("artifact_digest").map(s -> s.getValue()).orElse(null),
                violations, ValidityRules.metrics(run));
        }

        public ObjectNode toNode() {
            return ObjectNode.builder()
                             .withMember("index", index)
                             .withMember("repetition", repetition)
                             .withMember("valid", valid())
                             .withMember("violations", ArrayNode.fromNodes(
                                 violations.stream().map(Violation::toNode).toList()))
                             .build();
        }
    }

    public record ArmVerdict(String armId, String nullOf, List<RepetitionVerdict> repetitions,
                             List<Violation> violations) {
        public long validRepetitions() {
            return repetitions.stream().filter(RepetitionVerdict::valid).count();
        }

        public boolean valid() {
            return violations.isEmpty();
        }
    }

    /**
     * The same-artifact null experiment of a cell.
     *
     * @param ratios null/source mean per {@link ValidityRules.Metrics#NAMES} entry, empty when not evaluable
     */
    public record NullExperiment(String nullArm, String sourceArm, String status, Map<String, Double> ratios) {
        public static final String WITHIN = "within-bounds";
        public static final String OUTSIDE = "out-of-bounds";
        public static final String NOT_EVALUABLE = "not-evaluable";
    }

    public record CellVerdict(String cellId, List<ArmVerdict> arms, List<Violation> violations,
                              List<NullExperiment> nullExperiments) {
        public boolean valid() {
            return violations.isEmpty();
        }
    }

    public record CollectionVerdict(HarnessMode mode, boolean releaseEvidence, List<CellVerdict> cells,
                                    List<Violation> violations) {
        public boolean valid() {
            return violations.isEmpty();
        }

        public long invalidCells() {
            return cells.stream().filter(c -> !c.valid()).count();
        }

        public ObjectNode toNode() {
            ArrayNode.Builder cellNodes = ArrayNode.builder();
            for (CellVerdict c : cells) {
                ArrayNode.Builder armNodes = ArrayNode.builder();
                for (ArmVerdict a : c.arms()) {
                    ObjectNode.Builder an = ObjectNode.builder()
                        .withMember("arm", a.armId())
                        .withMember("scheduled_repetitions", a.repetitions().size())
                        .withMember("valid_repetitions", a.validRepetitions())
                        .withMember("valid", a.valid())
                        .withMember("violations", violations(a.violations()))
                        .withMember("repetitions", ArrayNode.fromNodes(
                            a.repetitions().stream().map(RepetitionVerdict::toNode).toList()));
                    if (a.nullOf() != null) {
                        an.withMember("null_of", a.nullOf());
                    }
                    armNodes.withValue(an.build());
                }
                ArrayNode.Builder nulls = ArrayNode.builder();
                for (NullExperiment n : c.nullExperiments()) {
                    ObjectNode.Builder ratios = ObjectNode.builder();
                    n.ratios().forEach(ratios::withMember);
                    nulls.withValue(ObjectNode.builder().withMember("arm", n.nullArm())
                                              .withMember("source", n.sourceArm()).withMember("status", n.status())
                                              .withMember("ratios", ratios.build()).build());
                }
                cellNodes.withValue(ObjectNode.builder()
                                              .withMember("cell", c.cellId())
                                              .withMember("valid", c.valid())
                                              .withMember("violations", violations(c.violations()))
                                              .withMember("arms", armNodes.build())
                                              .withMember("null_experiments", nulls.build())
                                              .build());
            }
            return ObjectNode.builder()
                             .withMember("schema", SCHEMA)
                             .withMember("mode", mode.id())
                             .withMember("release_evidence", releaseEvidence)
                             .withMember("valid", valid())
                             .withMember("violations", violations(violations))
                             .withMember("cells_total", cells.size())
                             .withMember("cells_invalid", invalidCells())
                             .withMember("thresholds", ObjectNode.builder()
                                 .withMember("min_valid_repetitions", RepetitionSchedule.MIN_RELEASE_REPETITIONS)
                                 .withMember("max_repetitions", RepetitionSchedule.MAX_REPETITIONS)
                                 .withMember("null_ratio_min", NULL_RATIO_MIN)
                                 .withMember("null_ratio_max", NULL_RATIO_MAX)
                                 .withMember("background_min_cpus", ValidityRules.BACKGROUND_MIN_CPUS)
                                 .withMember("background_host_fraction", ValidityRules.BACKGROUND_HOST_FRACTION)
                                 .build())
                             .withMember("cells", cellNodes.build())
                             .build();
        }

        private static ArrayNode violations(List<Violation> list) {
            return ArrayNode.fromNodes(list.stream().map(Violation::toNode).toList());
        }
    }

    /**
     * Evaluates a collection.
     *
     * @param repetitions every run's verdict, in run order
     * @param scheduleProblems {@link RepetitionSchedule#problems} of the executed schedule
     * @param releaseEvidence whether the preflight qualified the collection as release evidence
     */
    public static CollectionVerdict evaluate(HarnessMode mode, boolean releaseEvidence,
                                             List<RepetitionVerdict> repetitions, List<String> scheduleProblems) {
        Map<String, Map<String, List<RepetitionVerdict>>> byCell = new LinkedHashMap<>();
        Map<String, String> nullOf = new LinkedHashMap<>();
        for (RepetitionVerdict r : repetitions) {
            byCell.computeIfAbsent(r.cellId(), c -> new LinkedHashMap<>())
                  .computeIfAbsent(r.armId(), a -> new ArrayList<>()).add(r);
            if (r.nullOf() != null) {
                nullOf.put(r.armId(), r.nullOf());
            }
        }
        List<Violation> collection = new ArrayList<>();
        for (String p : scheduleProblems) {
            collection.add(Violation.collection(SCHEDULE, p));
        }
        List<CellVerdict> cells = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<RepetitionVerdict>>> cell : byCell.entrySet()) {
            cells.add(cell(mode, cell.getKey(), cell.getValue(), nullOf, collection));
        }
        return new CollectionVerdict(mode, releaseEvidence && mode == HarnessMode.RELEASE, cells, collection);
    }

    private static CellVerdict cell(HarnessMode mode, String cellId, Map<String, List<RepetitionVerdict>> arms,
                                    Map<String, String> nullOf, List<Violation> collection) {
        List<Violation> cellViolations = new ArrayList<>();
        List<ArmVerdict> armVerdicts = new ArrayList<>();
        Map<String, Set<String>> digests = new LinkedHashMap<>();
        for (Map.Entry<String, List<RepetitionVerdict>> arm : arms.entrySet()) {
            List<Violation> armViolations = new ArrayList<>();
            List<RepetitionVerdict> reps = arm.getValue();
            long valid = reps.stream().filter(RepetitionVerdict::valid).count();
            if (valid < RepetitionSchedule.MIN_RELEASE_REPETITIONS) {
                armViolations.add(Violation.cell(TOO_FEW_VALID_REPETITIONS, "arm " + arm.getKey() + " has " + valid
                                                 + " valid of " + reps.size() + " repetitions; at least "
                                                 + RepetitionSchedule.MIN_RELEASE_REPETITIONS + " required"));
            }
            Set<String> armDigests = new LinkedHashSet<>();
            for (RepetitionVerdict r : reps) {
                armDigests.add(String.valueOf(r.artifactDigest()));
                for (Violation v : r.violations()) {
                    if (v.scope() == Violation.Scope.CELL && !containsRule(armViolations, v)) {
                        armViolations.add(Violation.cell(v.rule(), "arm " + arm.getKey() + " repetition "
                                                                   + r.repetition() + ": " + v.detail()));
                    }
                }
            }
            if (armDigests.size() > 1) {
                armViolations.add(Violation.cell(ARTIFACT_DRIFT, "arm " + arm.getKey() + " ran " + armDigests.size()
                                                                 + " different artifacts across repetitions"));
            }
            digests.put(arm.getKey(), armDigests);
            armVerdicts.add(new ArmVerdict(arm.getKey(), nullOf.get(arm.getKey()), reps, armViolations));
            cellViolations.addAll(armViolations);
        }
        List<NullExperiment> nulls = new ArrayList<>();
        for (String arm : arms.keySet()) {
            String source = nullOf.get(arm);
            if (source == null) {
                continue;
            }
            if (!arms.containsKey(source)) {
                cellViolations.add(Violation.cell(NULL_ARTIFACT, "null arm " + arm + " ran without its source arm "
                                                                 + source));
                collection.add(Violation.collection(NULL_NOT_EVALUABLE, cellId + ": null arm " + arm
                                                                        + " has no source arm " + source));
                nulls.add(new NullExperiment(arm, source, NullExperiment.NOT_EVALUABLE, Map.of()));
                continue;
            }
            // String.valueOf(null): a run without a recorded digest cannot prove it ran the same artifact.
            if (!digests.get(arm).equals(digests.get(source)) || digests.get(arm).contains("null")) {
                cellViolations.add(Violation.cell(NULL_ARTIFACT, "null arm " + arm + " artifact " + digests.get(arm)
                                                                 + " is not source arm " + source + "'s "
                                                                 + digests.get(source)));
            }
            nulls.add(nullExperiment(cellId, arm, source, arms.get(arm), arms.get(source), collection));
        }
        if (nulls.isEmpty() && mode == HarnessMode.RELEASE) {
            cellViolations.add(Violation.cell(NULL_MISSING, "no same-artifact null experiment"));
        }
        return new CellVerdict(cellId, armVerdicts, cellViolations, nulls);
    }

    private static NullExperiment nullExperiment(String cellId, String arm, String source,
                                                 List<RepetitionVerdict> nullReps, List<RepetitionVerdict> sourceReps,
                                                 List<Violation> collection) {
        Optional<double[]> nullMeans = means(nullReps);
        Optional<double[]> sourceMeans = means(sourceReps);
        if (nullMeans.isEmpty() || sourceMeans.isEmpty()) {
            collection.add(Violation.collection(NULL_NOT_EVALUABLE, cellId + ": null experiment " + arm + " vs "
                                                                    + source + " has an arm with no valid repetition"));
            return new NullExperiment(arm, source, NullExperiment.NOT_EVALUABLE, Map.of());
        }
        Map<String, Double> ratios = new LinkedHashMap<>();
        List<String> outside = new ArrayList<>();
        for (int i = 0; i < ValidityRules.Metrics.NAMES.size(); i++) {
            double ratio = nullMeans.get()[i] / sourceMeans.get()[i];
            String name = ValidityRules.Metrics.NAMES.get(i);
            ratios.put(name, Double.isFinite(ratio) ? ratio : -1);
            if (!(ratio >= NULL_RATIO_MIN && ratio <= NULL_RATIO_MAX)) {
                outside.add(String.format(Locale.ROOT, "%s %.4f", name, ratio));
            }
        }
        if (!outside.isEmpty()) {
            collection.add(Violation.collection(NULL_RATIO, cellId + ": null " + arm + "/" + source + " outside "
                                                            + NULL_RATIO_MIN + ".." + NULL_RATIO_MAX + ": "
                                                            + String.join(", ", outside)));
            return new NullExperiment(arm, source, NullExperiment.OUTSIDE, ratios);
        }
        return new NullExperiment(arm, source, NullExperiment.WITHIN, ratios);
    }

    /** Arithmetic means of each metric over the valid repetitions, or empty when none is valid. */
    static Optional<double[]> means(List<RepetitionVerdict> reps) {
        double[] sum = new double[ValidityRules.Metrics.NAMES.size()];
        int n = 0;
        for (RepetitionVerdict r : reps) {
            if (r.valid() && r.metrics().isPresent()) {
                for (int i = 0; i < sum.length; i++) {
                    sum[i] += r.metrics().get().get(i);
                }
                n++;
            }
        }
        if (n == 0) {
            return Optional.empty();
        }
        for (int i = 0; i < sum.length; i++) {
            sum[i] /= n;
        }
        return Optional.of(sum);
    }

    private static boolean containsRule(List<Violation> list, Violation v) {
        return list.stream().anyMatch(x -> x.rule().equals(v.rule()) && x.scope() == v.scope());
    }
}
