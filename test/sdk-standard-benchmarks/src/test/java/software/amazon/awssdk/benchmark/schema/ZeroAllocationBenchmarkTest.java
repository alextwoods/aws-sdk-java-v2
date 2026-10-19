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

package software.amazon.awssdk.benchmark.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Runs {@link TraitLookupBenchmark} and {@link SchemaExtensionBenchmark} with JMH's GC profiler and asserts that each
 * benchmark allocates 0 bytes per op (Requirements 4.3, 4.4 and 5.7).
 *
 * <p>{@code gc.alloc.rate.norm} is the bytes allocated by the benchmark threads divided by the operations. JMH's own
 * per-iteration bookkeeping lands in that number too, spread over hundreds of millions of nanosecond operations, so a
 * lookup that allocates nothing measures well below 0.01 B/op; one allocation per call (an iterator, a boxed value, a
 * capturing lambda) is 16 B/op or more. The threshold is {@value #MAX_BYTES_PER_OP} B/op.
 *
 * <p>The benchmarks run in one fork with short iterations (about 20 seconds in total), so the fork JVM reaches C2 for
 * the lookups.
 */
class ZeroAllocationBenchmarkTest {
    private static final double MAX_BYTES_PER_OP = 1.0;

    @Test
    void lookups_whenRunWithGcProfiler_allocateZeroBytesPerOp() throws RunnerException {
        Options options = new OptionsBuilder()
            .include(TraitLookupBenchmark.class.getName())
            .include(SchemaExtensionBenchmark.class.getName())
            .addProfiler(GCProfiler.class)
            .forks(1)
            .warmupIterations(2)
            .warmupTime(TimeValue.milliseconds(500))
            .measurementIterations(2)
            .measurementTime(TimeValue.milliseconds(500))
            .shouldFailOnError(true)
            .build();

        Collection<RunResult> results = new Runner(options).run();

        Map<String, Double> bytesPerOp = new TreeMap<>();
        for (RunResult result : results) {
            Result<?> norm = result.getSecondaryResults().get("gc.alloc.rate.norm");
            assertThat(norm).as("gc.alloc.rate.norm of %s", result.getParams().getBenchmark()).isNotNull();
            bytesPerOp.put(result.getParams().getBenchmark(), norm.getScore());
        }
        System.out.println("gc.alloc.rate.norm (B/op): " + bytesPerOp);

        assertThat(bytesPerOp).hasSize(6);
        assertThat(bytesPerOp).allSatisfy((benchmark, bytes) -> assertThat(bytes).as(benchmark)
                                                                                  .isLessThan(MAX_BYTES_PER_OP));
    }
}
