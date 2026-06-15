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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import software.amazon.awssdk.benchmark.serde.BenchmarkTestCaseLoader;
import software.amazon.awssdk.bridge.smithyjava.serde.BridgeStruct;
import software.amazon.awssdk.c2j.smithy.C2jToSmithyConverter;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.dynamicclient.DynamicClient;
import software.amazon.smithy.java.io.uri.SmithyUri;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The decisive C2J→Smithy proof: build a Smithy {@link Model} purely from a C2J service-2.json via
 * {@link C2jToSmithyConverter} (no hand-written .smithy, no SdkSchemaFactory), drive a
 * {@link DynamicClient} from that converted model, serialize the SAME inputs the bridge benchmark
 * uses, and compare the JSON to the canonical bridge output (which matches v2). If these match, the
 * converted model is a faithful canonical model — i.e. the smithy→java front-end is sound.
 */
public final class ConvertedModelSerdeVerifier {

    private static final String INTERMEDIATE_MODEL_PATH = "models/awsjsonrpc10dataplane-1999-12-31-intermediate.json";
    private static final String TEST_DATA_PATH = "serde-tests/json-rpc-1-0/input/json_1_0.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    private ConvertedModelSerdeVerifier() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("smithy-java.json-provider", "smithy");

        // 1. Convert the C2J model to a canonical Smithy Model, then round-trip it to JSON AST and
        //    assemble (exactly what a codegen front-end would consume).
        Path c2j = Path.of(args.length > 0 ? args[0]
                : "test/sdk-standard-benchmarks/src/main/resources/codegen-resources/json-rpc-1-0/service-2.json");
        // Use the in-memory converted Model directly. (Its well-formedness through Smithy's
        // validating assembler is proven separately in the codegen-c2j-to-smithy module; here we
        // feed the Model straight to DynamicClient, avoiding a JSON round-trip whose trait
        // resolution depends on smithy-aws-traits discovery resources that the shaded benchmarks
        // uber-jar drops.)
        Model assembled = C2jToSmithyConverter.convert(c2j);

        // Service id from the converted model.
        ShapeId serviceId = assembled.getServiceShapes().iterator().next().getId();
        DynamicClient client = DynamicClient.builder()
                .model(assembled).serviceId(serviceId)
                .protocol(new AwsJson1Protocol(serviceId))
                .endpoint("http://localhost/").build();
        AwsJson1Protocol protocol = new AwsJson1Protocol(serviceId);
        Context context = Context.create();
        SmithyUri endpoint = SmithyUri.of("http://localhost/");

        // Reference path: the SAME bridge over the hand-authored CANONICAL Smithy model (the one
        // the leaderboard uses, proven == v2). If converted-model output == canonical-model output,
        // the converter produced a faithful model.
        ShapeId canonId = ShapeId.from("com.amazonaws.sdk.benchmark#AwsJsonRpc10DataPlane");
        Model canonical;
        try (java.io.InputStream is = ConvertedModelSerdeVerifier.class
                .getResourceAsStream("/smithy-models/json-rpc-1-0/model.json")) {
            canonical = Model.assembler().addUnparsedModel("canon.json",
                    new String(is.readAllBytes(), StandardCharsets.UTF_8)).assemble().unwrap();
        }
        DynamicClient canonClient = DynamicClient.builder().model(canonical).serviceId(canonId)
                .protocol(new AwsJson1Protocol(canonId)).endpoint("http://localhost/").build();
        AwsJson1Protocol canonProtocol = new AwsJson1Protocol(canonId);

        // 2. Build the same v2 inputs the bridge benchmark uses.
        IntermediateModel im = BenchmarkTestCaseLoader.loadIntermediateModel(INTERMEDIATE_MODEL_PATH);
        List<BenchmarkTestCaseLoader.MarshallTestCase> cases =
                BenchmarkTestCaseLoader.loadMarshallTestCases(TEST_DATA_PATH);

        int ok = 0;
        int fail = 0;
        for (BenchmarkTestCaseLoader.MarshallTestCase tc : cases) {
            String inputShape = tc.getOperationName() + "Request";
            SdkPojo request = (SdkPojo) new ShapeModelReflector(im, inputShape, tc.getInputData())
                    .createShapeObject();

            @SuppressWarnings("unchecked")
            ApiOperation<SerializableStruct, SerializableStruct> op =
                    (ApiOperation<SerializableStruct, SerializableStruct>) (ApiOperation<?, ?>)
                            client.getOperation(tc.getOperationName());

            // Serialize the v2 POJO via the bridge over the CONVERTED model's operation schema.
            String genBody = body(protocol.createRequest(op,
                    BridgeStruct.of(op.inputSchema(), request), context, endpoint));

            // Same, via the canonical hand-authored model (the leaderboard reference == v2).
            @SuppressWarnings("unchecked")
            ApiOperation<SerializableStruct, SerializableStruct> canonOp =
                    (ApiOperation<SerializableStruct, SerializableStruct>) (ApiOperation<?, ?>)
                            canonClient.getOperation(tc.getOperationName());
            String canonBody = body(canonProtocol.createRequest(canonOp,
                    BridgeStruct.of(canonOp.inputSchema(), request), context, endpoint));

            boolean match = JSON.readTree(genBody.isEmpty() ? "{}" : genBody)
                    .equals(JSON.readTree(canonBody.isEmpty() ? "{}" : canonBody));
            if (match) {
                ok++;
                System.out.println("OK: " + tc.getId());
            } else {
                fail++;
                System.out.println("MISMATCH: " + tc.getId());
                System.out.println("  canonical(==v2): " + truncate(canonBody));
                System.out.println("  converted:       " + truncate(genBody));
            }
        }
        System.out.println("\n" + ok + " ok, " + fail
                + " mismatch (converted-from-C2J model vs canonical hand-authored model)");
        if (fail > 0) {
            System.exit(1);
        }
    }

    private static String body(software.amazon.smithy.java.http.api.HttpRequest req) {
        java.nio.ByteBuffer bb = req.body().asByteBuffer();
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    private static String truncate(String s) {
        return s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }
}
