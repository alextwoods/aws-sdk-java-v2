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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Schema-based marshalling for each Supported_Protocol and payload size (Requirement 34.1):
 * {@code ClientProtocol.serializeRequest} from the generated input POJO to the {@code SdkHttpFullRequest}, without
 * signing or transport. Run with {@code -prof gc} for bytes per op.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(2)
public class SchemaMarshallBenchmark {
    @Param({"RPCV2_CBOR", "AWS_JSON_1_0", "AWS_JSON_1_1", "REST_JSON_1", "REST_XML", "AWS_QUERY", "EC2_QUERY"})
    private SchemaSerdeProtocol protocol;

    @Param({"SMALL", "LARGE"})
    private SchemaSerdePayload payload;

    private SchemaSerdeFixture fixture;

    @Setup
    public void setup() {
        fixture = new SchemaSerdeFixture(protocol, payload);
    }

    @Benchmark
    public SdkHttpFullRequest marshall() {
        return fixture.marshall();
    }
}
