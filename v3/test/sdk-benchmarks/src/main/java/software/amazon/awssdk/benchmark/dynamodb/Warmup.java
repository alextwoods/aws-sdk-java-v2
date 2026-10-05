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

package software.amazon.awssdk.benchmark.dynamodb;

import java.util.Locale;

/**
 * Runs unmeasured operations until JIT compilation quiesces; ported from racecar's {@code Warmup} with the same
 * constants: at least {@code minOps} and 5 s, then chunks until total compilation time has not grown by more than
 * 1 ms for 3 s, capped by {@code maxSeconds}. The runner separately checks compilation inside the measured window.
 */
final class Warmup {

    enum Mode {
        FIXED,
        QUIESCE
    }

    private static final long QUIET_COMPILE_MS = 1;
    private static final long MIN_WARMUP_WALL_NANOS = 5_000_000_000L;
    private static final long QUIET_WALL_NANOS = 3_000_000_000L;

    record Result(long ops, long wallNanos, long compileMillisDuring, boolean quiesced, String stopReason) {
        String summary() {
            return String.format(Locale.US, "ops=%,d wall=%.1fs jit=%dms settled=%s (%s)",
                                 ops, wallNanos / 1e9, compileMillisDuring, quiesced, stopReason);
        }
    }

    private Warmup() {
    }

    static Result run(V3Workload workload, Scenario scenario, Driver.Mode mode, Mode warmupMode, int minOps,
                      int concurrency, int maxSeconds) throws Exception {
        long start = System.nanoTime();
        long compileAtStart = JvmActivity.compilationMillis();
        if (minOps > 0) {
            Driver.run(workload, scenario, mode, minOps, concurrency, null);
        }
        long ops = minOps;
        if (warmupMode == Mode.FIXED) {
            return new Result(ops, System.nanoTime() - start, compileDelta(compileAtStart), false, "fixed warmup");
        }
        if (!JvmActivity.compilationTimeSupported()) {
            return new Result(ops, System.nanoTime() - start, -1, false,
                              "compilation time unavailable, fell back to fixed");
        }
        int chunk = Math.max(500, minOps / 4);
        long deadline = start + maxSeconds * 1_000_000_000L;
        long lastCompile = JvmActivity.compilationMillis();
        long quietSince = System.nanoTime();
        while (true) {
            long now = System.nanoTime();
            if (now - start >= MIN_WARMUP_WALL_NANOS && now - quietSince >= QUIET_WALL_NANOS) {
                return new Result(ops, now - start, compileDelta(compileAtStart), true,
                                  "compilation quiet for " + QUIET_WALL_NANOS / 1_000_000 + "ms");
            }
            if (now >= deadline) {
                return new Result(ops, now - start, compileDelta(compileAtStart), false,
                                  "hit --warmup-max-seconds " + maxSeconds + " while still compiling");
            }
            Driver.run(workload, scenario, mode, chunk, concurrency, null);
            ops += chunk;
            long compile = JvmActivity.compilationMillis();
            if (compile - lastCompile > QUIET_COMPILE_MS) {
                lastCompile = compile;
                quietSince = System.nanoTime();
            }
        }
    }

    private static long compileDelta(long compileAtStart) {
        long now = JvmActivity.compilationMillis();
        return now < 0 || compileAtStart < 0 ? -1 : now - compileAtStart;
    }
}
