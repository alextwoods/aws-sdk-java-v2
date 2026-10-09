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
import java.util.List;

/**
 * Orders the forked runs of a plan: which cell runs, as which repetition and label, in which position. The
 * orchestrator executes the returned list in order, one fresh mock-service process and one fresh client process per
 * entry.
 *
 * <p>Extension point for 19.2, which supplies the release schedule (5-10 interleaved repetitions per compared arm,
 * order reversal, and a same-artifact null arm per cell, labelled distinctly). This task provides only
 * {@link #once()}.
 */
@FunctionalInterface
public interface RepetitionSchedule {

    /** One scheduled forked run. */
    record ScheduledRun(BenchmarkCell cell, int repetition, int position, String label) {
    }

    List<ScheduledRun> schedule(List<BenchmarkCell> cells);

    /** Each cell once, in plan order, labelled with its id. */
    static RepetitionSchedule once() {
        return cells -> {
            List<ScheduledRun> runs = new ArrayList<>(cells.size());
            for (int i = 0; i < cells.size(); i++) {
                runs.add(new ScheduledRun(cells.get(i), 1, i + 1, cells.get(i).id()));
            }
            return runs;
        };
    }
}
