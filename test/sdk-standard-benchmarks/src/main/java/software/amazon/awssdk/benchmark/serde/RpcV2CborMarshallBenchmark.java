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

package software.amazon.awssdk.benchmark.serde;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;

/**
 * JMH benchmark for RPC v2 CBOR marshalling (serialization).
 *
 * <p>
 * Measures the schema-based path: {@code RpcV2CborProtocol.serializeRequest} over the generated input schema.
 * The protocol and the operation are resolved once per trial, as the generated client resolves them once.
 * </p>
 *
 * <p>
 * SampleTime mode is used (instead of AverageTime) to enable percentile
 * collection (p50, p90, p95, p99) for the cross-language output schema.
 * </p>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class RpcV2CborMarshallBenchmark {

    private static final URI ENDPOINT = URI.create("http://localhost/");
    private static final String INTERMEDIATE_MODEL_PATH = "models/smithyrpcv2cbordataplane-1999-12-31-intermediate.json";
    private static final String TEST_DATA_PATH = "serde-tests/rpc-v2-cbor/input/rpc_v2_cbor.json";

    @Param({
            "rpcv2Cbor_PutItemRequest_Baseline",
            "rpcv2Cbor_PutItemRequest_ShallowMap_S",
            "rpcv2Cbor_PutItemRequest_ShallowMap_M",
            "rpcv2Cbor_PutItemRequest_ShallowMap_L",
            "rpcv2Cbor_PutItemRequest_Nested_M",
            "rpcv2Cbor_PutItemRequest_Nested_L",
            "rpcv2Cbor_PutItemRequest_MixedItem_S",
            "rpcv2Cbor_PutItemRequest_MixedItem_M",
            "rpcv2Cbor_PutItemRequest_MixedItem_L",
            "rpcv2Cbor_PutItemRequest_BinaryData_S",
            "rpcv2Cbor_PutItemRequest_BinaryData_M",
            "rpcv2Cbor_PutItemRequest_BinaryData_L",
    })
    private String testCaseId;

    private SerializableStruct request;
    private ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol;
    private ApiOperation<?, ?> operation;
    private ExecutionAttributes attributes;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // 1. Load test cases
        List<BenchmarkTestCaseLoader.MarshallTestCase> allCases = BenchmarkTestCaseLoader
                .loadMarshallTestCases(TEST_DATA_PATH);
        BenchmarkTestCaseLoader.MarshallTestCase testCase = allCases.stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));

        // 2. Load IntermediateModel
        IntermediateModel model = BenchmarkTestCaseLoader.loadIntermediateModel(INTERMEDIATE_MODEL_PATH);

        // 3. Build SDK request via ShapeModelReflector
        String inputShapeName = testCase.getOperationName() + "Request";
        ShapeModelReflector reflector = new ShapeModelReflector(model, inputShapeName, testCase.getInputData());
        this.request = (SerializableStruct) reflector.createShapeObject();

        // 4. Resolve the client protocol and the operation (same as the generated client)
        this.protocol = SerdeBenchmarkProtocols.RPC_V2_CBOR.createProtocol();
        this.operation = SerdeBenchmarkProtocols.RPC_V2_CBOR.operation(testCase.getOperationName());
        this.attributes = new ExecutionAttributes();
    }

    @Benchmark
    public void marshall(Blackhole bh) {
        bh.consume(marshallOnce());
    }

    SdkHttpFullRequest marshallOnce() {
        return SerdeBenchmarkProtocols.serialize(protocol, operation, request, attributes, ENDPOINT);
    }
}
