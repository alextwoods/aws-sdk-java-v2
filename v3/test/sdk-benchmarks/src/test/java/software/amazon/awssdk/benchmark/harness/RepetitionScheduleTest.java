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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RepetitionScheduleTest {
    private static final BenchmarkArm REF = new BenchmarkArm("ref", "/ref.jar", "example.RefClient", null);
    private static final List<BenchmarkCell> ALL = CellMatrix.enumerate(CellMatrix.Selection.parse(
        null, null, null, null, null, null, null));
    private static final List<BenchmarkCell> THREE = ALL.subList(0, 3);

    @Test
    void onceRunsEachCellOnceWithTheV3Arm() {
        List<RepetitionSchedule.ScheduledRun> runs = RepetitionSchedule.once().schedule(THREE);
        assertEquals(3, runs.size());
        for (int i = 0; i < 3; i++) {
            RepetitionSchedule.ScheduledRun r = runs.get(i);
            assertEquals(THREE.get(i), r.cell());
            assertEquals(BenchmarkArm.V3, r.arm());
            assertEquals(1, r.repetition());
            assertEquals(0, r.slot());
            assertEquals(i + 1, r.position());
            assertEquals("v3/r1/" + THREE.get(i).id(), r.label());
        }
        assertEquals(List.of(), RepetitionSchedule.problems(THREE, runs, HarnessMode.EXPLORATORY));
        assertFalse(RepetitionSchedule.problems(THREE, runs, HarnessMode.RELEASE).isEmpty());
    }

    @Test
    void twoArmsReverseEachRound() {
        BenchmarkCell cell = THREE.get(0);
        List<String> order = RepetitionSchedule.withNullExperiment(List.of(BenchmarkArm.V3), 4)
                                               .schedule(List.of(cell)).stream()
                                               .map(r -> r.arm().id() + "@" + r.repetition()).toList();
        assertEquals(List.of("v3@1", "v3#null@1", "v3#null@2", "v3@2", "v3@3", "v3#null@3", "v3#null@4", "v3@4"),
                     order);
    }

    @Test
    void threeArmsRotateAndEachCellRoundIsContiguous() {
        RepetitionSchedule s = RepetitionSchedule.withNullExperiment(List.of(BenchmarkArm.V3, REF), 5);
        List<RepetitionSchedule.ScheduledRun> runs = s.schedule(THREE);
        assertEquals(3 * 3 * 5, runs.size());
        // Round 2 of the first cell: rotated left once.
        assertEquals(List.of("ref", "v3#null", "v3"),
                     runs.subList(9, 12).stream().map(r -> r.arm().id()).toList());
        for (int i = 0; i < runs.size(); i += 3) {
            List<RepetitionSchedule.ScheduledRun> block = runs.subList(i, i + 3);
            assertTrue(block.stream().allMatch(r -> r.cell().equals(block.get(0).cell())
                                                    && r.repetition() == block.get(0).repetition()));
        }
        assertEquals(List.of(), RepetitionSchedule.problems(THREE, runs, HarnessMode.RELEASE));
    }

    /**
     * Seeded random plans: counts, rounds, slot balance and null coverage hold for every plan, and the release
     * verdict is exactly "5..10 repetitions and a null arm".
     */
    @Test
    void randomPlansAreWellFormed() {
        Random random = new Random(20_6_8);
        for (int trial = 0; trial < 300; trial++) {
            List<BenchmarkCell> cells = new ArrayList<>();
            int n = 1 + random.nextInt(6);
            for (int i = 0; i < n; i++) {
                BenchmarkCell c = ALL.get(random.nextInt(ALL.size()));
                if (!cells.contains(c)) {
                    cells.add(c);
                }
            }
            List<BenchmarkArm> compared = new ArrayList<>(List.of(BenchmarkArm.V3));
            int refs = random.nextInt(3);
            for (int i = 0; i < refs; i++) {
                compared.add(new BenchmarkArm("ref" + i, "/ref" + i + ".jar", "example.Ref", null));
            }
            int reps = 1 + random.nextInt(RepetitionSchedule.MAX_REPETITIONS);
            boolean withNull = random.nextBoolean();
            RepetitionSchedule s = withNull ? RepetitionSchedule.withNullExperiment(compared, reps)
                                            : RepetitionSchedule.interleaved(compared, reps);
            List<RepetitionSchedule.ScheduledRun> runs = s.schedule(cells);
            int arms = compared.size() + (withNull ? 1 : 0);
            assertEquals(cells.size() * arms * reps, runs.size());
            Map<String, Integer> perCellArm = new HashMap<>();
            Map<String, int[]> slots = new HashMap<>();
            for (RepetitionSchedule.ScheduledRun r : runs) {
                String key = r.cell().id() + " " + r.arm().id();
                perCellArm.merge(key, 1, Integer::sum);
                slots.computeIfAbsent(key, k -> new int[arms])[r.slot()]++;
            }
            assertEquals(cells.size() * arms, perCellArm.size());
            assertTrue(perCellArm.values().stream().allMatch(v -> v == reps));
            for (int[] sl : slots.values()) {
                int min = java.util.Arrays.stream(sl).min().orElseThrow();
                int max = java.util.Arrays.stream(sl).max().orElseThrow();
                assertTrue(max - min <= 1, java.util.Arrays.toString(sl));
            }
            assertEquals(List.of(), RepetitionSchedule.problems(cells, runs, HarnessMode.EXPLORATORY));
            boolean releaseOk = reps >= RepetitionSchedule.MIN_RELEASE_REPETITIONS && withNull;
            assertEquals(releaseOk, RepetitionSchedule.problems(cells, runs, HarnessMode.RELEASE).isEmpty(),
                         "reps=" + reps + " null=" + withNull);
        }
    }

    @Test
    void problemsDetectBrokenSchedules() {
        BenchmarkCell a = THREE.get(0);
        BenchmarkCell b = THREE.get(1);
        BenchmarkArm nullArm = BenchmarkArm.V3.nullTwin();
        List<RepetitionSchedule.ScheduledRun> good =
            RepetitionSchedule.withNullExperiment(List.of(BenchmarkArm.V3), 5).schedule(List.of(a));

        // Round 2 before round 1.
        List<RepetitionSchedule.ScheduledRun> swapped = renumber(List.of(good.get(2), good.get(3), good.get(0),
                                                                         good.get(1)));
        assertProblem(List.of(a), swapped, HarnessMode.EXPLORATORY, "runs after round");
        // A round missing one arm.
        assertProblem(List.of(a), renumber(good.subList(0, 9)), HarnessMode.EXPLORATORY, "ran 1 of 2 arms");
        // Fixed order every round: slots unbalanced.
        List<RepetitionSchedule.ScheduledRun> fixed = new ArrayList<>();
        for (int k = 1; k <= 5; k++) {
            fixed.add(new RepetitionSchedule.ScheduledRun(a, BenchmarkArm.V3, k, 0, 0));
            fixed.add(new RepetitionSchedule.ScheduledRun(a, nullArm, k, 1, 0));
        }
        assertProblem(List.of(a), renumber(fixed), HarnessMode.RELEASE, "slots are unbalanced");
        // A planned cell never scheduled; an unplanned cell scheduled.
        assertProblem(List.of(a, b), good, HarnessMode.EXPLORATORY, "never scheduled");
        assertProblem(List.of(b), good, HarnessMode.EXPLORATORY, "not in the plan");
        // A null arm whose source does not run.
        List<RepetitionSchedule.ScheduledRun> onlyNull = new ArrayList<>();
        for (int k = 1; k <= 5; k++) {
            onlyNull.add(new RepetitionSchedule.ScheduledRun(a, nullArm, k, 0, 0));
        }
        assertProblem(List.of(a), renumber(onlyNull), HarnessMode.EXPLORATORY, "has no source arm");
        // Positions must be 1..N.
        List<RepetitionSchedule.ScheduledRun> badPosition = new ArrayList<>(good);
        RepetitionSchedule.ScheduledRun first = badPosition.get(0);
        badPosition.set(0, new RepetitionSchedule.ScheduledRun(first.cell(), first.arm(), 1, 0, 7));
        assertProblem(List.of(a), badPosition, HarnessMode.EXPLORATORY, "has position 7");
    }

    @Test
    void schedulesRejectBadArmsAndCounts() {
        assertThrows(IllegalArgumentException.class, () -> RepetitionSchedule.interleaved(List.of(), 5));
        assertThrows(IllegalArgumentException.class,
                     () -> RepetitionSchedule.interleaved(List.of(BenchmarkArm.V3), 11));
        assertThrows(IllegalArgumentException.class,
                     () -> RepetitionSchedule.interleaved(List.of(BenchmarkArm.V3), 0));
        assertThrows(IllegalArgumentException.class,
                     () -> RepetitionSchedule.interleaved(List.of(BenchmarkArm.V3, BenchmarkArm.V3), 5));
        assertThrows(IllegalArgumentException.class,
                     () -> RepetitionSchedule.interleaved(List.of(BenchmarkArm.V3.nullTwin()), 5));
        // A "null" arm running a different artifact is not a null experiment.
        BenchmarkArm fake = new BenchmarkArm("v3#null", "/other.jar", ClientMain.class.getName(), "v3");
        assertThrows(IllegalArgumentException.class,
                     () -> RepetitionSchedule.interleaved(List.of(BenchmarkArm.V3, fake), 5));
        assertThrows(IllegalArgumentException.class,
                     () -> new BenchmarkArm("v3-null", null, "Main", "v3"));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkArm("x#null", null, "Main", null));
        assertThrows(IllegalStateException.class, () -> BenchmarkArm.V3.nullTwin().nullTwin());
        assertTrue(BenchmarkArm.V3.nullTwin().sameArtifact(BenchmarkArm.V3));
        assertFalse(REF.sameArtifact(BenchmarkArm.V3));
    }

    private static List<RepetitionSchedule.ScheduledRun> renumber(List<RepetitionSchedule.ScheduledRun> runs) {
        List<RepetitionSchedule.ScheduledRun> out = new ArrayList<>();
        for (RepetitionSchedule.ScheduledRun r : runs) {
            out.add(new RepetitionSchedule.ScheduledRun(r.cell(), r.arm(), r.repetition(), r.slot(), out.size() + 1));
        }
        return out;
    }

    private static void assertProblem(List<BenchmarkCell> cells, List<RepetitionSchedule.ScheduledRun> runs,
                                      HarnessMode mode, String expected) {
        List<String> problems = RepetitionSchedule.problems(cells, runs, mode);
        assertTrue(problems.stream().anyMatch(p -> p.contains(expected)), expected + " not in " + problems);
    }
}
