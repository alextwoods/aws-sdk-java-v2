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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Orders the forked runs of a plan: which cell runs with which arm, as which repetition, in which position. The
 * orchestrator executes the returned list in order, one fresh mock-service process and one fresh client process per
 * entry. Every schedule is a pure function of its arguments, so it can be checked against an oracle.
 *
 * <p>{@link #interleaved} is the release schedule (Req 20.6, 20.8): the plan runs in {@code R} rounds; round
 * {@code k} runs every cell, in plan order, and for each cell every arm exactly once, in the arm order rotated left by
 * {@code k - 1}. So one cell's arms always run back to back (paired in time), a cell's repetitions are spread across
 * the whole collection (slow drift becomes variance, not bias against one arm), and every arm occupies every slot of
 * the round equally often, to within one (with two arms the order simply reverses each round). A same-artifact null
 * arm ({@link BenchmarkArm#nullTwin()}) is an ordinary arm of the rotation, placed last in the base order.
 */
@FunctionalInterface
public interface RepetitionSchedule {

    /** At least five interleaved repetitions per compared arm in a release-gate collection (Req 20.6). */
    int MIN_RELEASE_REPETITIONS = 5;
    /** At most ten interleaved repetitions per compared arm (Req 20.6). */
    int MAX_REPETITIONS = 10;

    /**
     * One scheduled forked run.
     *
     * @param repetition 1-based repetition (round) of this arm on this cell
     * @param slot 0-based position of this arm within the cell's round
     * @param position 1-based position in the whole schedule
     */
    record ScheduledRun(BenchmarkCell cell, BenchmarkArm arm, int repetition, int slot, int position) {

        /** {@code <arm>/r<repetition>/<cell id>}, e.g. {@code v3#null/r2/awsJson1_0/...}. */
        public String label() {
            return arm.id() + "/r" + repetition + "/" + cell.id();
        }
    }

    List<ScheduledRun> schedule(List<BenchmarkCell> cells);

    /** Each cell once with the {@link BenchmarkArm#V3} arm, in plan order (exploratory development runs). */
    static RepetitionSchedule once() {
        return interleaved(List.of(BenchmarkArm.V3), 1);
    }

    /** {@code compared} plus the same-artifact null twin of its first arm (the candidate), interleaved. */
    static RepetitionSchedule withNullExperiment(List<BenchmarkArm> compared, int repetitions) {
        List<BenchmarkArm> arms = new ArrayList<>(compared);
        arms.add(compared.get(0).nullTwin());
        return interleaved(arms, repetitions);
    }

    /**
     * {@code repetitions} rotated rounds of {@code arms} over every cell (see the class documentation).
     *
     * @throws IllegalArgumentException for no arms, duplicate arm ids, a repetition count outside 1..10, or a null
     *     arm whose source arm is absent or runs a different artifact
     */
    static RepetitionSchedule interleaved(List<BenchmarkArm> arms, int repetitions) {
        List<BenchmarkArm> order = List.copyOf(arms);
        if (order.isEmpty()) {
            throw new IllegalArgumentException("a schedule needs at least one arm");
        }
        if (repetitions < 1 || repetitions > MAX_REPETITIONS) {
            throw new IllegalArgumentException("repetitions must be 1.." + MAX_REPETITIONS + ": " + repetitions);
        }
        Map<String, BenchmarkArm> byId = new LinkedHashMap<>();
        for (BenchmarkArm a : order) {
            if (byId.put(a.id(), a) != null) {
                throw new IllegalArgumentException("duplicate arm " + a.id());
            }
        }
        for (BenchmarkArm a : order) {
            if (a.isNull()) {
                BenchmarkArm source = byId.get(a.nullOf());
                if (source == null || !source.sameArtifact(a)) {
                    throw new IllegalArgumentException("null arm " + a.id() + " needs its source arm " + a.nullOf()
                                                       + " with the same artifact in the schedule");
                }
            }
        }
        int n = order.size();
        return cells -> {
            List<ScheduledRun> runs = new ArrayList<>(cells.size() * n * repetitions);
            for (int k = 1; k <= repetitions; k++) {
                for (BenchmarkCell cell : cells) {
                    for (int slot = 0; slot < n; slot++) {
                        BenchmarkArm arm = order.get((slot + k - 1) % n);
                        runs.add(new ScheduledRun(cell, arm, k, slot, runs.size() + 1));
                    }
                }
            }
            return runs;
        };
    }

    /**
     * Structural problems of a scheduled plan; empty when it is a valid interleaved schedule for {@code mode}. Checks:
     * positions are 1..N in order; every planned cell is scheduled and nothing else is; per cell, every arm has the
     * same repetition count, repetitions 1..R each exactly once, in 1..10 (5..10 in release mode); round {@code k} of a
     * cell has every arm exactly once and completes before round {@code k + 1} starts; each arm's slots are balanced
     * to within one; every null arm's source arm runs the same artifact on the same cell; and in release mode every
     * cell has a same-artifact null arm (Req 20.8).
     */
    static List<String> problems(List<BenchmarkCell> cells, List<ScheduledRun> runs, HarnessMode mode) {
        List<String> problems = new ArrayList<>();
        Set<BenchmarkCell> planned = new LinkedHashSet<>(cells);
        Map<BenchmarkCell, List<ScheduledRun>> byCell = new LinkedHashMap<>();
        for (int i = 0; i < runs.size(); i++) {
            ScheduledRun r = runs.get(i);
            if (r.position() != i + 1) {
                problems.add("run " + (i + 1) + " has position " + r.position());
            }
            if (!planned.contains(r.cell())) {
                problems.add("scheduled cell " + r.cell() + " is not in the plan");
            }
            byCell.computeIfAbsent(r.cell(), c -> new ArrayList<>()).add(r);
        }
        for (BenchmarkCell c : planned) {
            if (!byCell.containsKey(c)) {
                problems.add("planned cell " + c + " is never scheduled");
            }
        }
        for (Map.Entry<BenchmarkCell, List<ScheduledRun>> e : byCell.entrySet()) {
            cellProblems(e.getKey(), e.getValue(), mode, problems);
        }
        return problems;
    }

    private static void cellProblems(BenchmarkCell cell, List<ScheduledRun> runs, HarnessMode mode,
                                     List<String> problems) {
        Map<String, BenchmarkArm> arms = new LinkedHashMap<>();
        Map<String, List<Integer>> repetitions = new LinkedHashMap<>();
        for (ScheduledRun r : runs) {
            BenchmarkArm prior = arms.putIfAbsent(r.arm().id(), r.arm());
            if (prior != null && !prior.equals(r.arm())) {
                problems.add(cell + ": arm id " + r.arm().id() + " names two different arms");
            }
            repetitions.computeIfAbsent(r.arm().id(), a -> new ArrayList<>()).add(r.repetition());
        }
        int n = arms.size();
        Set<Integer> counts = new HashSet<>();
        for (Map.Entry<String, List<Integer>> e : repetitions.entrySet()) {
            List<Integer> reps = e.getValue();
            counts.add(reps.size());
            int min = mode == HarnessMode.RELEASE ? MIN_RELEASE_REPETITIONS : 1;
            if (reps.size() < min || reps.size() > MAX_REPETITIONS) {
                problems.add(cell + ": arm " + e.getKey() + " has " + reps.size() + " repetitions, outside " + min
                             + ".." + MAX_REPETITIONS);
            }
            List<Integer> sorted = reps.stream().sorted().toList();
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i) != i + 1) {
                    problems.add(cell + ": arm " + e.getKey() + " repetitions are " + sorted + ", not 1.."
                                 + sorted.size());
                    break;
                }
            }
        }
        if (counts.size() > 1) {
            problems.add(cell + ": arms have different repetition counts " + counts);
        }
        // Rounds: in schedule order, round numbers never decrease and each round has every arm exactly once.
        int round = 0;
        Set<String> seen = new HashSet<>();
        Map<String, int[]> slots = new HashMap<>();
        for (ScheduledRun r : runs) {
            if (r.repetition() != round) {
                if (r.repetition() < round) {
                    problems.add(cell + ": round " + r.repetition() + " runs after round " + round);
                }
                if (round > 0 && seen.size() != n) {
                    problems.add(cell + ": round " + round + " ran " + seen.size() + " of " + n + " arms");
                }
                round = r.repetition();
                seen.clear();
            }
            if (!seen.add(r.arm().id())) {
                problems.add(cell + ": arm " + r.arm().id() + " runs twice in round " + round);
            }
            if (r.slot() < 0 || r.slot() >= n) {
                problems.add(cell + ": slot " + r.slot() + " outside 0.." + (n - 1));
            } else {
                slots.computeIfAbsent(r.arm().id(), a -> new int[n])[r.slot()]++;
            }
        }
        if (round > 0 && seen.size() != n) {
            problems.add(cell + ": round " + round + " ran " + seen.size() + " of " + n + " arms");
        }
        for (Map.Entry<String, int[]> e : slots.entrySet()) {
            int min = Integer.MAX_VALUE;
            int max = 0;
            for (int s : e.getValue()) {
                min = Math.min(min, s);
                max = Math.max(max, s);
            }
            if (max - min > 1) {
                problems.add(cell + ": arm " + e.getKey() + " slots are unbalanced " + java.util.Arrays.toString(
                    e.getValue()));
            }
        }
        boolean hasNull = false;
        for (BenchmarkArm a : arms.values()) {
            if (a.isNull()) {
                BenchmarkArm source = arms.get(a.nullOf());
                if (source == null) {
                    problems.add(cell + ": null arm " + a.id() + " has no source arm " + a.nullOf());
                } else if (!source.sameArtifact(a)) {
                    problems.add(cell + ": null arm " + a.id() + " runs a different artifact than " + source.id());
                } else {
                    hasNull = true;
                }
            }
        }
        if (mode == HarnessMode.RELEASE && !hasNull) {
            problems.add(cell + ": no same-artifact null experiment");
        }
    }

    /** The arms and repetitions of a scheduled plan, for the manifest. */
    static ObjectNode summary(List<ScheduledRun> runs) {
        Map<String, BenchmarkArm> arms = new LinkedHashMap<>();
        Map<String, Integer> maxRepetition = new LinkedHashMap<>();
        for (ScheduledRun r : runs) {
            arms.putIfAbsent(r.arm().id(), r.arm());
            maxRepetition.merge(r.arm().id(), r.repetition(), Math::max);
        }
        ArrayNode.Builder a = ArrayNode.builder();
        arms.forEach((id, arm) -> a.withValue(arm.toNode().toBuilder()
                                                 .withMember("repetitions", maxRepetition.get(id)).build()));
        return ObjectNode.builder().withMember("arms", a.build()).withMember("runs", runs.size()).build();
    }
}
