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
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.Rpcv2CborTrait;
import software.amazon.awssdk.schema.traits.SensitiveTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;
import software.amazon.awssdk.services.schemabenchjson10.model.Item;

/**
 * {@code Schema.getTrait(TraitKey)} lookups, which must allocate 0 bytes per call (Requirement 4.3 and 4.4).
 * {@code ZeroAllocationBenchmarkTest} runs this class with the GC profiler and asserts it.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class TraitLookupBenchmark {
    /**
     * A member with traits on the member, read through a member schema as the codecs read them.
     */
    private final Schema member = Schema.structureBuilder(ShapeId.from("benchmark#Traits"))
                                        .putMember("name", PreludeSchemas.STRING,
                                                   new JsonNameTrait("Name"), new XmlNameTrait("N"),
                                                   SensitiveTrait.INSTANCE)
                                        .build()
                                        .member("name");

    /**
     * A generated member schema: the {@code UpdatedAt} timestamp member of a benchmark service.
     */
    private final Schema generatedMember = Item.SCHEMA.member("UpdatedAt");

    @Benchmark
    public JsonNameTrait presentTrait() {
        return member.getTrait(TraitKey.JSON_NAME_TRAIT);
    }

    @Benchmark
    public TimestampFormatTrait absentTrait() {
        return member.getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT);
    }

    /**
     * A key whose index is beyond the member's trait storage (the protocol trait keys are created last).
     */
    @Benchmark
    public Rpcv2CborTrait absentTraitBeyondStorage() {
        return member.getTrait(TraitKey.RPCV2_CBOR_TRAIT);
    }

    @Benchmark
    public TimestampFormatTrait generatedMemberTrait() {
        return generatedMember.getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT);
    }
}
