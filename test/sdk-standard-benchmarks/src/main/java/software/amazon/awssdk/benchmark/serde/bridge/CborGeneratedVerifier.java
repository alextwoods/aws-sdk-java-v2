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
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import software.amazon.awssdk.benchmark.serde.BenchmarkTestCaseLoader;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.rpcv2.internal.SdkStructuredRpcV2CborFactory;
import software.amazon.smithy.java.client.core.ClientProtocol;
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
 * CBOR correctness: serialize the same regenerated v2 request via (1) v2's own CBOR marshaller and
 * (2) the CODEGEN path (the v2 POJO as a smithy SerializableStruct through RpcV2CborProtocol), and
 * compare the body bytes (hex). CBOR is body-only + member-name-keyed (like awsJson), so the
 * synthetic-schema codegen path should match v2 exactly.
 */
public final class CborGeneratedVerifier {

    private static final String INTERMEDIATE_MODEL_PATH = "models/smithyrpcv2cbordataplane-1999-12-31-intermediate.json";
    private static final String TEST_DATA_PATH = "serde-tests/rpc-v2-cbor/input/rpc_v2_cbor.json";
    private static final String SMITHY_MODEL_PATH = "/smithy-models/rpc-v2-cbor/model.json";
    private static final ShapeId SERVICE_ID = ShapeId.from("com.amazonaws.sdk.benchmark#SmithyRpcV2CborDataPlane");
    private static final String CONTENT_TYPE = "application/cbor";
    private static final URI V2_ENDPOINT = URI.create("http://localhost/");
    private static final SmithyUri ENDPOINT = SmithyUri.of("http://localhost/");

    private CborGeneratedVerifier() {
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        IntermediateModel model = BenchmarkTestCaseLoader.loadIntermediateModel(INTERMEDIATE_MODEL_PATH);
        List<BenchmarkTestCaseLoader.MarshallTestCase> cases =
                BenchmarkTestCaseLoader.loadMarshallTestCases(TEST_DATA_PATH);

        Model smithyModel;
        try (InputStream is = CborGeneratedVerifier.class.getResourceAsStream(SMITHY_MODEL_PATH)) {
            smithyModel = Model.assembler()
                    .addUnparsedModel("m.json", new String(is.readAllBytes(), StandardCharsets.UTF_8))
                    .assemble().unwrap();
        }
        RpcV2CborProtocol protocol = new RpcV2CborProtocol(SERVICE_ID);
        DynamicClient client = DynamicClient.builder()
                .model(smithyModel).serviceId(SERVICE_ID).protocol(protocol)
                .endpoint("http://localhost/").build();
        Context context = Context.create();

        int ok = 0;
        int fail = 0;
        int skip = 0;
        for (BenchmarkTestCaseLoader.MarshallTestCase tc : cases) {
            if (!tc.getId().startsWith("rpcv2Cbor_")) {
                continue;
            }
            String inputShapeName = tc.getOperationName() + "Request";
            SdkPojo request = (SdkPojo) new ShapeModelReflector(model, inputShapeName, tc.getInputData())
                    .createShapeObject();
            if (!(request instanceof SerializableStruct)) {
                skip++;
                continue;
            }

            String v2Hex = hex(v2Cbor(request, model, tc));
            ApiOperation<SerializableStruct, SerializableStruct> op =
                    (ApiOperation<SerializableStruct, SerializableStruct>) (ApiOperation<?, ?>)
                            client.getOperation(tc.getOperationName());
            ClientProtocol<SerializableStruct, SerializableStruct> proto =
                    (ClientProtocol<SerializableStruct, SerializableStruct>) (ClientProtocol<?, ?>) protocol;
            HttpRequest req = (HttpRequest) proto.createRequest(op, (SerializableStruct) request, context, ENDPOINT);
            String genHex = hex(toBytes(req.body().asByteBuffer()));

            if (v2Hex.equals(genHex)) {
                ok++;
                System.out.println("OK: " + tc.getId());
            } else {
                fail++;
                System.out.println("MISMATCH: " + tc.getId());
                System.out.println("  v2:  " + v2Hex);
                System.out.println("  gen: " + genHex);
            }
        }
        System.out.println("\n" + ok + " ok, " + fail + " mismatch, " + skip + " skip");
        if (fail > 0) {
            System.exit(1);
        }
    }

    private static byte[] v2Cbor(SdkPojo request, IntermediateModel model,
                                 BenchmarkTestCaseLoader.MarshallTestCase tc) throws Exception {
        OperationInfo info = BenchmarkTestCaseLoader.buildOperationInfo(model, tc);
        AwsJsonProtocolMetadata md = AwsJsonProtocolMetadata.builder()
                .protocol(AwsJsonProtocol.SMITHY_RPC_V2_CBOR).contentType(CONTENT_TYPE).build();
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = JsonProtocolMarshallerBuilder.<SdkHttpFullRequest>create()
                .endpoint(V2_ENDPOINT)
                .jsonGenerator(SdkStructuredRpcV2CborFactory.SDK_CBOR_FACTORY.createWriter(CONTENT_TYPE))
                .contentType(CONTENT_TYPE)
                .operationInfo(info)
                .sendExplicitNullForPayload(false)
                .protocolMetadata(md)
                .build();
        SdkHttpFullRequest r = marshaller.marshall(request);
        ContentStreamProvider body = r.contentStreamProvider().orElse(null);
        if (body == null) {
            return new byte[0];
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        InputStream in = body.newStream();
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static byte[] toBytes(ByteBuffer bb) {
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return b;
    }

    private static String hex(byte[] b) {
        return new BigInteger(1, b).toString(16);
    }
}
