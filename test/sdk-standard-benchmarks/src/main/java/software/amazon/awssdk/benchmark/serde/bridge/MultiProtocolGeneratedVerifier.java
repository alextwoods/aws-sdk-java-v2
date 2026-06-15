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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import software.amazon.awssdk.benchmark.serde.BenchmarkTestCaseLoader;
import software.amazon.awssdk.bridge.smithyjava.serde.BridgeStruct;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.smithy.java.aws.client.awsquery.AwsQueryClientProtocol;
import software.amazon.smithy.java.aws.client.restjson.RestJsonClientProtocol;
import software.amazon.smithy.java.aws.client.restxml.RestXmlClientProtocol;
import software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.dynamicclient.DynamicClient;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.io.uri.SmithyUri;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Cross-protocol correctness check for the codegen serialize path. For each protocol it serializes
 * the SAME regenerated v2 request two ways and compares the request body bytes:
 * <ol>
 *   <li>the runtime bridge ({@link BridgeStruct} over the canonical operation schema) — the
 *       leaderboard-validated reference, and</li>
 *   <li>the CODEGEN path — the regenerated v2 POJO that IS a {@link SerializableStruct}, via its
 *       generated {@code $SCHEMA} + direct-write {@code serializeMembers}.</li>
 * </ol>
 * Prints OK/MISMATCH per case and a per-protocol tally. This tells us which protocols the
 * synthetic-schema codegen path serializes correctly (JSON/CBOR key on member name; XML/Query
 * depend on wire-name traits the synthetic schema may not carry).
 */
public final class MultiProtocolGeneratedVerifier {

    private static final SmithyUri ENDPOINT = SmithyUri.of("http://localhost/");

    private MultiProtocolGeneratedVerifier() {
    }

    private static final class Protocol {
        final String name;
        final String intermediateModel;
        final String testData;
        final String smithyModel;
        final ShapeId serviceId;
        final Function<ShapeId, Object> protocolFactory;

        Protocol(String name, String intermediateModel, String testData, String smithyModel,
                 String serviceId, Function<ShapeId, Object> protocolFactory) {
            this.name = name;
            this.intermediateModel = intermediateModel;
            this.testData = testData;
            this.smithyModel = smithyModel;
            this.serviceId = ShapeId.from(serviceId);
            this.protocolFactory = protocolFactory;
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("smithy-java.json-provider", "smithy");
        System.setProperty("smithy-java.xml-provider", "smithy");

        List<Protocol> protocols = List.of(
            new Protocol("restJson", "models/awsrestjsondataplane-1999-12-31-intermediate.json",
                "serde-tests/rest-json/input/rest_json.json", "/smithy-models/rest-json/model.json",
                "com.amazonaws.sdk.benchmark#AwsRestJsonDataPlane", RestJsonClientProtocol::new),
            new Protocol("restXml", "models/awsrestxmldataplane-1999-12-31-intermediate.json",
                "serde-tests/rest-xml/input/rest_xml.json", "/smithy-models/rest-xml/model.json",
                "com.amazonaws.sdk.benchmark#AwsRestXmlDataPlane", RestXmlClientProtocol::new),
            new Protocol("rpcv2Cbor", "models/smithyrpcv2cbordataplane-1999-12-31-intermediate.json",
                "serde-tests/rpc-v2-cbor/input/rpc_v2_cbor.json", "/smithy-models/rpc-v2-cbor/model.json",
                "com.amazonaws.sdk.benchmark#SmithyRpcV2CborDataPlane", RpcV2CborProtocol::new),
            new Protocol("awsQuery", "models/awsquerydataplane-1999-12-31-intermediate.json",
                "serde-tests/query/input/query.json", "/smithy-models/query/model.json",
                "com.amazonaws.sdk.benchmark#AwsQueryDataPlane",
                sid -> new AwsQueryClientProtocol(sid, "2020-01-01"))
        );

        int totalFail = 0;
        for (Protocol p : protocols) {
            totalFail += verifyProtocol(p);
        }
        System.out.println("\n==== " + (totalFail == 0 ? "ALL PROTOCOLS CLEAN" : totalFail + " TOTAL MISMATCHES") + " ====");
    }

    @SuppressWarnings("unchecked")
    private static int verifyProtocol(Protocol p) throws Exception {
        IntermediateModel model = BenchmarkTestCaseLoader.loadIntermediateModel(p.intermediateModel);
        List<BenchmarkTestCaseLoader.MarshallTestCase> cases =
                BenchmarkTestCaseLoader.loadMarshallTestCases(p.testData);

        Model smithyModel;
        try (InputStream is = MultiProtocolGeneratedVerifier.class.getResourceAsStream(p.smithyModel)) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            smithyModel = Model.assembler().addUnparsedModel("m.json", json).assemble().unwrap();
        }
        Object protocolObj = p.protocolFactory.apply(p.serviceId);
        DynamicClient client = DynamicClient.builder()
                .model(smithyModel).serviceId(p.serviceId)
                .protocol((software.amazon.smithy.java.client.core.ClientProtocol<?, ?>) protocolObj)
                .endpoint("http://localhost/").build();

        Context context = Context.create();
        int ok = 0;
        int fail = 0;
        int skip = 0;
        System.out.println("== " + p.name + " ==");
        for (BenchmarkTestCaseLoader.MarshallTestCase tc : cases) {
            String inputShapeName = tc.getOperationName() + "Request";
            SdkPojo request;
            ApiOperation<SerializableStruct, SerializableStruct> op;
            try {
                ShapeModelReflector reflector = new ShapeModelReflector(model, inputShapeName, tc.getInputData());
                request = (SdkPojo) reflector.createShapeObject();
                op = (ApiOperation<SerializableStruct, SerializableStruct>) (ApiOperation<?, ?>)
                        client.getOperation(tc.getOperationName());
            } catch (Exception e) {
                System.out.println("  SKIP (setup): " + tc.getId() + " (" + e.getClass().getSimpleName() + ")");
                skip++;
                continue;
            }
            if (!(request instanceof SerializableStruct)) {
                System.out.println("  SKIP (not generated): " + tc.getId());
                skip++;
                continue;
            }

            String bridge;
            String gen;
            try {
                software.amazon.smithy.java.client.core.ClientProtocol<SerializableStruct, SerializableStruct> proto =
                        (software.amazon.smithy.java.client.core.ClientProtocol<SerializableStruct, SerializableStruct>) protocolObj;
                bridge = body((HttpRequest) proto.createRequest(
                        op, BridgeStruct.of(op.inputSchema(), request), context, ENDPOINT));
                gen = body((HttpRequest) proto.createRequest(
                        op, (SerializableStruct) request, context, ENDPOINT));
            } catch (Exception e) {
                System.out.println("  ERROR: " + tc.getId() + " -> " + e);
                fail++;
                continue;
            }

            if (bridge.equals(gen)) {
                ok++;
            } else {
                fail++;
                System.out.println("  MISMATCH: " + tc.getId());
                System.out.println("    bridge: " + truncate(bridge));
                System.out.println("    gen:    " + truncate(gen));
            }
        }
        System.out.println("  -> " + ok + " ok, " + fail + " mismatch, " + skip + " skip");
        return fail;
    }

    private static String body(HttpRequest req) {
        ByteBuffer bb = req.body().asByteBuffer();
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    private static String truncate(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
