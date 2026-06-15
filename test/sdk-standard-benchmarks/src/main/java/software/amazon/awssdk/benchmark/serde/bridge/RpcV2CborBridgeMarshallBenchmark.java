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

package software.amazon.awssdk.benchmark.serde.bridge;

import java.io.InputStream;
import java.util.List;
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
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.benchmark.serde.BenchmarkTestCaseLoader;
import software.amazon.awssdk.bridge.smithyjava.serde.BridgeStruct;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.dynamicclient.DynamicClient;
import software.amazon.smithy.java.io.uri.SmithyUri;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Bridge counterpart to {@link software.amazon.awssdk.benchmark.serde.RpcV2CborMarshallBenchmark}.
 *
 * <p>Both benchmarks start from the <b>same C2J-built v2 {@link SdkPojo}</b> (built in
 * {@code @Setup} via {@link ShapeModelReflector}). The v2 benchmark then calls
 * {@code marshaller.marshall(pojo)}. This benchmark instead measures the path a
 * v2-API-over-smithy-java shim takes:
 *
 * <ol>
 *   <li>wrap the v2 {@code SdkPojo} as a smithy-java {@link SerializableStruct}
 *       ({@link BridgeStruct}, a reflection-free walk of {@code sdkFields()}), then</li>
 *   <li>run smithy-java's schema-based serialization:
 *       {@code RpcV2CborProtocol#createRequest(operation, input, context, endpoint)}.</li>
 * </ol>
 *
 * <p>The delta between this and the v2 benchmark is the true per-call serialization cost of
 * running the v2 surface on the smithy-java engine. Same {@code @BenchmarkMode}, warmup, and
 * measurement config as the v2 benchmark so the numbers slot into the same aggregate.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class RpcV2CborBridgeMarshallBenchmark {

    private static final String INTERMEDIATE_MODEL_PATH = "models/smithyrpcv2cbordataplane-1999-12-31-intermediate.json";
    private static final String TEST_DATA_PATH = "serde-tests/rpc-v2-cbor/input/rpc_v2_cbor.json";
    private static final String SMITHY_MODEL_PATH = "/smithy-models/rpc-v2-cbor/model.json";
    private static final ShapeId SERVICE_ID = ShapeId.from("com.amazonaws.sdk.benchmark#SmithyRpcV2CborDataPlane");
    private static final SmithyUri ENDPOINT = SmithyUri.of("http://localhost/");

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

    // Untimed: the same C2J-built v2 input the v2 benchmark marshals.
    private SdkPojo request;
    // smithy-java serde state.
    private RpcV2CborProtocol protocol;
    private ApiOperation<SerializableStruct, SerializableStruct> operation;
    private Context context;
    // Pre-built wrapped input so the @Benchmark method times only createRequest (like the native
    // smithy-java serde benchmark, which pre-builds its input in @Setup).
    private BridgeStruct bridgeInput;

    @Setup
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        // --- v2 input (identical to RpcV2CborMarshallBenchmark) ---
        List<BenchmarkTestCaseLoader.MarshallTestCase> cases =
                BenchmarkTestCaseLoader.loadMarshallTestCases(TEST_DATA_PATH);
        BenchmarkTestCaseLoader.MarshallTestCase testCase = cases.stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));

        IntermediateModel model = BenchmarkTestCaseLoader.loadIntermediateModel(INTERMEDIATE_MODEL_PATH);
        String inputShapeName = testCase.getOperationName() + "Request";
        ShapeModelReflector reflector = new ShapeModelReflector(model, inputShapeName, testCase.getInputData());
        this.request = (SdkPojo) reflector.createShapeObject();

        // --- smithy-java side: load the model, resolve the ApiOperation + protocol ---
        Model smithyModel;
        try (InputStream is = RpcV2CborBridgeMarshallBenchmark.class.getResourceAsStream(SMITHY_MODEL_PATH)) {
            String json = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            smithyModel = Model.assembler()
                    .addUnparsedModel("benchmark-model.json", json)
                    .assemble()
                    .unwrap();
        }
        // The DynamicClient is used only to resolve the ApiOperation/schema; we never call it,
        // so a dummy endpoint just satisfies client-config validation.
        DynamicClient client = DynamicClient.builder()
                .model(smithyModel)
                .serviceId(SERVICE_ID)
                .endpoint("http://localhost/")
                .build();
        this.operation = (ApiOperation<SerializableStruct, SerializableStruct>)
                (ApiOperation<?, ?>) client.getOperation(testCase.getOperationName());
        this.protocol = new RpcV2CborProtocol(SERVICE_ID);
        this.context = Context.create();

        // Pre-build the wrapped input once (like native smithy-java's benchmark does).
        this.bridgeInput = BridgeStruct.of(operation.inputSchema(), request);
    }

    /** The runtime bridge: v2 SdkPojo wrapped, serialized via a precompiled field-iteration plan. */
    @Benchmark
    public void bridgeMarshall(Blackhole bh) {
        bh.consume(protocol.createRequest(operation, bridgeInput, context, ENDPOINT));
    }

    /** The CODEGEN path: regenerated v2 POJO that IS a SerializableStruct (direct-write serialize). */
    @Benchmark
    public void generatedStructMarshall(Blackhole bh) {
        bh.consume(protocol.createRequest(operation, (SerializableStruct) request, context, ENDPOINT));
    }
}
