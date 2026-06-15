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

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import software.amazon.awssdk.benchmark.serde.BenchmarkTestCaseLoader;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.protocols.rpcv2.SmithyRpcV2CborProtocolFactory;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.Codec;

/**
 * CBOR read-path correctness (mirror of {@link GeneratedDeserializeVerifier}, for CBOR). Decodes
 * each response two ways and compares the resulting v2 POJO by toString():
 * <ol>
 *   <li>v2's own CBOR unmarshaller (reference),</li>
 *   <li>the CODEGEN read path — generated {@code BuilderImpl} (a smithy {@link ShapeBuilder})
 *       driven by the CBOR {@link Codec}.</li>
 * </ol>
 */
public final class CborDeserializeVerifier {

    private static final String TEST_DATA_PATH = "serde-tests/rpc-v2-cbor/output/rpc_v2_cbor.json";
    private static final String V2_MODEL_PKG = "software.amazon.awssdk.services.rpccbordataplane.model.";
    private static final String CONTENT_TYPE = "application/cbor";

    private CborDeserializeVerifier() {
    }

    public static void main(String[] args) throws Exception {
        List<BenchmarkTestCaseLoader.UnmarshallTestCase> cases =
                BenchmarkTestCaseLoader.loadUnmarshallTestCases(TEST_DATA_PATH);
        Codec codec = software.amazon.smithy.java.cbor.Rpcv2CborCodec.builder().build();
        JsonProtocolUnmarshaller unmarshaller = JsonProtocolUnmarshaller.builder()
                .enableFastUnmarshalling(true)
                .protocolUnmarshallDependencies(SmithyRpcV2CborProtocolFactory.defaultProtocolUnmarshallDependencies())
                .build();

        int ok = 0;
        int fail = 0;
        int skip = 0;
        for (BenchmarkTestCaseLoader.UnmarshallTestCase tc : cases) {
            if (!tc.getId().startsWith("rpcv2Cbor_")) {
                continue;
            }
            byte[] bytes = Base64.getDecoder().decode(tc.getResponseBody().trim());
            int status = tc.getStatusCode() != null ? tc.getStatusCode() : 200;
            String responseClassName = V2_MODEL_PKG + tc.getOperationName() + "Response";
            Class<?> responseClass = Class.forName(responseClassName);

            Object freshBuilder = responseClass.getMethod("builder").invoke(null);
            if (!(freshBuilder instanceof ShapeBuilder)) {
                System.out.println("SKIP (not generated): " + tc.getId());
                skip++;
                continue;
            }
            @SuppressWarnings("unchecked")
            ShapeBuilder<? extends software.amazon.smithy.java.core.schema.SerializableStruct> sb =
                    (ShapeBuilder<? extends software.amazon.smithy.java.core.schema.SerializableStruct>) freshBuilder;
            SdkPojo genPojo;
            try {
                genPojo = (SdkPojo) codec.deserializeShape(bytes, sb);
            } catch (RuntimeException e) {
                System.out.println("GEN-THREW: " + tc.getId() + " -> " + e.getMessage());
                genPojo = null;
            }

            Object refBuilder = responseClass.getMethod("builder").invoke(null);
            SdkHttpFullResponse httpResp = SdkHttpFullResponse.builder()
                    .statusCode(status)
                    .putHeader("Content-Type", CONTENT_TYPE)
                    .content(AbortableInputStream.create(new ByteArrayInputStream(bytes)))
                    .build();
            SdkPojo refPojo = (SdkPojo) unmarshaller.unmarshall((SdkPojo) refBuilder, httpResp);

            String gen = String.valueOf(genPojo);
            String ref = String.valueOf(refPojo);
            if (gen.equals(ref)) {
                ok++;
                System.out.println("OK: " + tc.getId());
            } else {
                fail++;
                System.out.println("MISMATCH: " + tc.getId());
                System.out.println("  ref: " + ref);
                System.out.println("  gen: " + gen);
            }
        }
        System.out.println("\n" + ok + " ok, " + fail + " mismatch, " + skip + " skip");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
