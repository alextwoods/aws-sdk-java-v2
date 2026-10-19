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

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.services.schemabenchjson10.model.Item;
import software.amazon.awssdk.services.schemabenchjson10.model.SchemaBenchJson10Operations;

/**
 * {@code Schema.getExtension(key)} after the first computation, which must allocate 0 bytes per call (Requirement 5.7:
 * the codecs read their metadata through extensions on every call). {@code ZeroAllocationBenchmarkTest} runs this class
 * with the GC profiler and asserts it.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class SchemaExtensionBenchmark {
    private final Schema schema = Item.SCHEMA;
    private final ApiOperation<?, ?> operation = SchemaBenchJson10Operations.PUT_PAYLOAD;

    /**
     * Computes and caches the extensions, so the benchmarks measure only the cached lookup.
     */
    @Setup
    public void setup() {
        schema.getExtension(MemberCountExtension.KEY);
        operation.idempotencyTokenMember();
    }

    /**
     * An extension registered by this module through {@code ServiceLoader}.
     */
    @Benchmark
    public MemberCountExtension.MemberCount cachedExtension() {
        return schema.getExtension(MemberCountExtension.KEY);
    }

    /**
     * The SDK's operation-members extension on a generated input schema, read by the pipeline on every call.
     */
    @Benchmark
    public Schema operationMembersExtension() {
        return operation.idempotencyTokenMember();
    }
}
