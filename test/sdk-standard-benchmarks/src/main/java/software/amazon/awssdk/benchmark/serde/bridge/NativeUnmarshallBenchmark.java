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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
import software.amazon.awssdk.bridge.smithyjava.serde.GeneratedOutputOperation;
import software.amazon.smithy.java.client.http.HttpClientProtocol;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.java.dynamicclient.DynamicClient;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.HttpVersion;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Generic FULLY-NATIVE unmarshall benchmark across all 5 protocols. Times
 * {@code deserializeResponse} into a smithy-java type generated from the C2J→Smithy converted model.
 * The output builder is the native generated builder (a {@link ShapeBuilder}), supplied via
 * {@link GeneratedOutputOperation} — no {@code BridgeOutputBuilder}, no v2 SdkPojo on the timed path.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class NativeUnmarshallBenchmark {

    @Param({})
    private String protocol;

    @Param({})
    private String testCaseId;

    private HttpClientProtocol proto;
    private ApiOperation<SerializableStruct, ?> operation;
    private Context context;
    private TypeRegistry errorRegistry;
    private HttpResponse response;

    @Setup
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        NativeProtocols.Spec spec = NativeProtocols.byName(protocol);
        System.setProperty("smithy-java.json-provider", "smithy");
        System.setProperty("smithy-java.xml-provider", "smithy");

        List<BenchmarkTestCaseLoader.UnmarshallTestCase> cases =
                BenchmarkTestCaseLoader.loadUnmarshallTestCases(spec.outputData);
        BenchmarkTestCaseLoader.UnmarshallTestCase tc = cases.stream()
                .filter(c -> c.getId().equals(testCaseId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("no case " + testCaseId));

        byte[] bytes = spec.kind == NativeProtocols.Kind.RPC_V2_CBOR
                ? Base64.getDecoder().decode(tc.getResponseBody().trim())
                : tc.getResponseBody().getBytes(StandardCharsets.UTF_8);
        int status = tc.getStatusCode() != null ? tc.getStatusCode() : 200;
        this.response = HttpResponse.of(HttpVersion.HTTP_1_1, status,
                HttpHeaders.of(java.util.Map.of("Content-Type", List.of(contentType(spec)))),
                DataStream.ofBytes(bytes));

        Model smithyModel;
        try (InputStream is = NativeUnmarshallBenchmark.class.getResourceAsStream(spec.smithyModel)) {
            smithyModel = Model.assembler()
                    .addUnparsedModel("m.json", new String(is.readAllBytes(), StandardCharsets.UTF_8))
                    .assemble().unwrap();
        }
        ShapeId serviceId = ShapeId.from(spec.serviceId);
        DynamicClient client = DynamicClient.builder()
                .model(smithyModel).serviceId(serviceId)
                .protocol(spec.protocol(serviceId)).endpoint("http://localhost/").build();
        ApiOperation<?, ?> realOp = client.getOperation(tc.getOperationName());

        // Native output class = the operation's OUTPUT SHAPE name (read from the resolved operation,
        // not guessed), since shape naming varies per service.
        String nativeOutput = spec.nativePkg + realOp.outputSchema().id().getName();
        java.lang.reflect.Method builderMethod = Class.forName(nativeOutput).getMethod("builder");
        this.operation = new GeneratedOutputOperation(realOp, () -> {
            try {
                return (ShapeBuilder<? extends SerializableStruct>) builderMethod.invoke(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        this.proto = (HttpClientProtocol) spec.protocol(serviceId);
        this.context = Context.create();
        this.errorRegistry = realOp.errorRegistry();
    }

    private static String contentType(NativeProtocols.Spec spec) {
        switch (spec.kind) {
            case AWS_JSON: return "application/x-amz-json-1.0";
            case REST_JSON: return "application/json";
            case RPC_V2_CBOR: return "application/cbor";
            default: return "application/xml";
        }
    }

    @Benchmark
    public void nativeUnmarshall(Blackhole bh) {
        bh.consume(proto.deserializeResponse((ApiOperation) operation, context, errorRegistry, null, response));
    }
}
