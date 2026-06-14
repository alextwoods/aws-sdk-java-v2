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
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
import software.amazon.awssdk.benchmark.serde.BenchmarkTestCaseLoader;
import software.amazon.awssdk.bridge.smithyjava.serde.BridgeOutputOperation;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.java.dynamicclient.DynamicClient;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.HttpVersion;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Bridge counterpart to {@code RpcV2CborUnmarshallBenchmark}: deserializes an rpcv2Cbor response
 * into an AWS SDK v2 {@link SdkPojo} via the smithy-java runtime + the reverse bridge
 * ({@code BridgeOutputOperation} / {@code BridgeOutputBuilder}).
 *
 * <p>Times {@code protocol.deserializeResponse(...)} on a pre-built {@link HttpResponse}, which
 * routes wire-decoding into a v2 builder. The v2 baseline builds the same response bytes and
 * calls its own unmarshaller, so the two are comparable.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class RpcV2CborBridgeUnmarshallBenchmark {

    private static final String TEST_DATA_PATH = "serde-tests/rpc-v2-cbor/output/rpc_v2_cbor.json";
    private static final String SMITHY_MODEL_PATH = "/smithy-models/rpc-v2-cbor/model.json";
    private static final ShapeId SERVICE_ID = ShapeId.from("com.amazonaws.sdk.benchmark#SmithyRpcV2CborDataPlane");
    private static final String V2_MODEL_PKG = "software.amazon.awssdk.services.rpccbordataplane.model.";

    @Param({
            "rpcv2Cbor_GetItemOutput_Baseline",
            "rpcv2Cbor_GetItemOutput_S",
            "rpcv2Cbor_GetItemOutput_M",
            "rpcv2Cbor_GetItemOutput_L",
            "rpcv2Cbor_GetItemOutputBinary_S",
            "rpcv2Cbor_GetItemOutputBinary_M",
            "rpcv2Cbor_GetItemOutputBinary_L",
    })
    private String testCaseId;

    private RpcV2CborProtocol protocol;
    private ApiOperation<SerializableStruct, ?> bridgeOperation;
    private Context context;
    private TypeRegistry errorRegistry;
    private HttpResponse response;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        List<BenchmarkTestCaseLoader.UnmarshallTestCase> cases =
                BenchmarkTestCaseLoader.loadUnmarshallTestCases(TEST_DATA_PATH);
        BenchmarkTestCaseLoader.UnmarshallTestCase testCase = cases.stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));

        // CBOR responses are base64-encoded in the test data.
        byte[] responseBytes = Base64.getDecoder().decode(testCase.getResponseBody().trim());
        int statusCode = testCase.getStatusCode() != null ? testCase.getStatusCode() : 200;

        HttpHeaders headers = HttpHeaders.of(java.util.Map.of(
                "Content-Type", List.of("application/cbor")));
        this.response = HttpResponse.of(HttpVersion.HTTP_1_1, statusCode, headers,
                DataStream.ofBytes(responseBytes));

        // smithy-java side: resolve the real operation, then wrap it so the output is a v2 pojo.
        Model smithyModel;
        try (InputStream is = RpcV2CborBridgeUnmarshallBenchmark.class.getResourceAsStream(SMITHY_MODEL_PATH)) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            smithyModel = Model.assembler().addUnparsedModel("benchmark-model.json", json).assemble().unwrap();
        }
        // Build the protocol explicitly and set it on the client builder, so the DynamicClient's
        // protocol auto-detection (which can pick a config that doesn't match the explicit protocol
        // this benchmark constructs) is bypassed.
        this.protocol = new RpcV2CborProtocol(SERVICE_ID);
        DynamicClient client = DynamicClient.builder()
                .model(smithyModel)
                .serviceId(SERVICE_ID)
                .protocol(protocol)
                .endpoint("http://localhost/")
                .build();
        ApiOperation<?, ?> realOp = client.getOperation(testCase.getOperationName());

        // v2 builder factory: services.rpccbordataplane.model.<Op>Response.builder()
        String fqcn = V2_MODEL_PKG + testCase.getOperationName() + "Response";
        Class<?> responseClass = Class.forName(fqcn);
        Method builderMethod = responseClass.getMethod("builder");
        this.bridgeOperation = new BridgeOutputOperation(realOp, () -> {
            try {
                return (SdkPojo) builderMethod.invoke(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });

        this.context = Context.create();
        this.errorRegistry = realOp.errorRegistry();
    }

    @Benchmark
    public void bridgeUnmarshall(Blackhole bh) {
        bh.consume(protocol.deserializeResponse(bridgeOperation, context, errorRegistry, null, response));
    }
}
