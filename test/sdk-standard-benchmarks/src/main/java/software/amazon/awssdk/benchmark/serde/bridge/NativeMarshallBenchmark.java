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
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.smithy.java.client.core.ClientProtocol;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.dynamicclient.DynamicClient;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.io.uri.SmithyUri;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Generic FULLY-NATIVE marshall benchmark across all 5 protocols. Times {@code createRequest} on a
 * smithy-java type generated from the C2J→Smithy converted model (build-time generated into
 * {@code com.amazonaws.<svc>dataplane.model}). No bridge / no v2 SdkPojo on the timed path — this is
 * v2 once it code-generates from Smithy. The protocol is selected by the {@code protocol} param.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class NativeMarshallBenchmark {

    private static final SmithyUri ENDPOINT = SmithyUri.of("http://localhost/");

    @Param({}) // set by the launcher; one of NativeProtocols.NAMES
    private String protocol;

    @Param({}) // test case id, set by the launcher
    private String testCaseId;

    private ClientProtocol<SerializableStruct, SerializableStruct> proto;
    private ApiOperation<SerializableStruct, SerializableStruct> operation;
    private Context context;
    private SerializableStruct nativeInput;

    @Setup
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        NativeProtocols.Spec spec = NativeProtocols.byName(protocol);
        System.setProperty("smithy-java.json-provider", "smithy");
        System.setProperty("smithy-java.xml-provider", "smithy");

        List<BenchmarkTestCaseLoader.MarshallTestCase> cases =
                BenchmarkTestCaseLoader.loadMarshallTestCases(spec.inputData);
        BenchmarkTestCaseLoader.MarshallTestCase tc = cases.stream()
                .filter(c -> c.getId().equals(testCaseId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("no case " + testCaseId));

        IntermediateModel im = BenchmarkTestCaseLoader.loadIntermediateModel(spec.intermediateModel);
        String inputShape = tc.getOperationName() + "Request";
        SdkPojo v2 = (SdkPojo) new ShapeModelReflector(im, inputShape, tc.getInputData()).createShapeObject();

        Model smithyModel;
        try (InputStream is = NativeMarshallBenchmark.class.getResourceAsStream(spec.smithyModel)) {
            smithyModel = Model.assembler()
                    .addUnparsedModel("m.json", new String(is.readAllBytes(), StandardCharsets.UTF_8))
                    .assemble().unwrap();
        }
        ShapeId serviceId = ShapeId.from(spec.serviceId);
        this.proto = (ClientProtocol<SerializableStruct, SerializableStruct>)
                (ClientProtocol<?, ?>) spec.protocol(serviceId);
        // Set the protocol on the client builder too: awsQuery's operation resolution requires the
        // serviceVersion the protocol carries (DynamicClient auto-detect otherwise NPEs).
        DynamicClient client = DynamicClient.builder()
                .model(smithyModel).serviceId(serviceId)
                .protocol((ClientProtocol<?, ?>) spec.protocol(serviceId))
                .endpoint("http://localhost/").build();
        this.operation = (ApiOperation<SerializableStruct, SerializableStruct>)
                (ApiOperation<?, ?>) client.getOperation(tc.getOperationName());
        this.context = Context.create();

        // Native input: deserialize v2's CLEAN marshaller JSON into the native builder (clean = no
        // empty auto-construct collections, so unions carry a single member). The native class name
        // is the operation's INPUT SHAPE name (varies per service: CopyObjectRequest vs GetItemInput)
        // — read it from the resolved operation rather than guessing from the v2 "Request" alias.
        String nativeShape = operation.inputSchema().id().getName();
        Class<?> nativeType = Class.forName(spec.nativePkg + nativeShape);
        ShapeBuilder<? extends SerializableStruct> builder =
                (ShapeBuilder<? extends SerializableStruct>) nativeType.getMethod("builder").invoke(null);
        // Build a JSON document carrying ALL members (incl. @httpLabel/@httpHeader-bound ones), then
        // deserialize into the native builder. awsJson/cbor have DynamoDB AttributeValue UNIONS, so
        // use v2's clean marshaller (omits empty members -> one union member). restJson/restXml/query
        // have no unions but DO have HTTP-bound members (Bucket/Key path labels) that v2's body JSON
        // omits, so serialize ALL members via the bridge over the input schema.
        byte[] inputJson = spec.kind == NativeProtocols.Kind.AWS_JSON
                || spec.kind == NativeProtocols.Kind.RPC_V2_CBOR
                ? NativeInputs.cleanJson(spec, v2, im, tc)
                : NativeInputs.allMembersJson(operation.inputSchema(), v2);
        this.nativeInput = NativeInputs.deserialize(spec, inputJson, builder);
    }

    @Benchmark
    public void nativeMarshall(Blackhole bh) {
        HttpRequest req = (HttpRequest) proto.createRequest(operation, nativeInput, context, ENDPOINT);
        bh.consume(req);
    }
}
