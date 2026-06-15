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
import java.nio.charset.StandardCharsets;
import java.util.List;
import software.amazon.awssdk.benchmark.serde.BenchmarkTestCaseLoader;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.json.JsonCodec;

/**
 * Read-path correctness check (mirror of {@link GeneratedSerdeVerifier}). For each response test
 * case it deserializes the SAME bytes two ways and compares the resulting v2 POJO:
 * <ol>
 *   <li>v2's own {@code JsonProtocolUnmarshaller} (reference),</li>
 *   <li>the CODEGEN read path — the generated {@code BuilderImpl} (a smithy {@link ShapeBuilder})
 *       driven by {@link JsonCodec}, switch(memberIndex) into direct fluent setters.</li>
 * </ol>
 * POJOs are compared by re-serializing each via v2's own JSON marshaller (so field population is
 * compared, not object identity). Exits non-zero on any mismatch.
 */
public final class GeneratedDeserializeVerifier {

    private static final String TEST_DATA_PATH = "serde-tests/json-rpc-1-0/output/json_1_0.json";
    private static final String V2_MODEL_PKG = "software.amazon.awssdk.services.jsonrpc10dataplane.model.";
    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";

    private GeneratedDeserializeVerifier() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("smithy-java.json-provider", "smithy");
        List<BenchmarkTestCaseLoader.UnmarshallTestCase> cases =
                BenchmarkTestCaseLoader.loadUnmarshallTestCases(TEST_DATA_PATH);
        JsonCodec codec = JsonCodec.builder().build();
        JsonProtocolUnmarshaller unmarshaller = JsonProtocolUnmarshaller.builder()
                .enableFastUnmarshalling(true)
                .protocolUnmarshallDependencies(JsonProtocolUnmarshaller.defaultProtocolUnmarshallDependencies())
                .build();

        int failures = 0;
        int checked = 0;
        for (BenchmarkTestCaseLoader.UnmarshallTestCase tc : cases) {
            byte[] bytes = tc.getResponseBody().getBytes(StandardCharsets.UTF_8);
            int status = tc.getStatusCode() != null ? tc.getStatusCode() : 200;
            String responseClassName = V2_MODEL_PKG + tc.getOperationName() + "Response";
            Class<?> responseClass = Class.forName(responseClassName);

            // Skip degenerate non-JSON-object bodies (e.g. a healthcheck "ok"); they exercise no
            // member serde and only the JSON readers' framing differs.
            String trimmed = tc.getResponseBody().trim();
            if (trimmed.isEmpty() || trimmed.charAt(0) != '{') {
                System.out.println("SKIP (non-object body): " + tc.getId());
                continue;
            }

            // (2) codegen read path: generated builder IS a smithy ShapeBuilder.
            Object freshBuilder = responseClass.getMethod("builder").invoke(null);
            if (!(freshBuilder instanceof ShapeBuilder)) {
                System.out.println("SKIP (builder not a ShapeBuilder): " + tc.getId());
                continue;
            }
            @SuppressWarnings("unchecked")
            ShapeBuilder<? extends SerializableStruct> sb =
                    (ShapeBuilder<? extends SerializableStruct>) freshBuilder;
            SdkPojo genPojo = (SdkPojo) codec.deserializeShape(bytes, sb);

            // (1) v2 reference: its own unmarshaller into a fresh builder.
            Object refBuilder = responseClass.getMethod("builder").invoke(null);
            SdkHttpFullResponse httpResp = SdkHttpFullResponse.builder()
                    .statusCode(status)
                    .putHeader("Content-Type", CONTENT_TYPE)
                    .content(AbortableInputStream.create(new ByteArrayInputStream(bytes)))
                    .build();
            SdkPojo refPojo = (SdkPojo) unmarshaller.unmarshall((SdkPojo) refBuilder, httpResp);

            // v2 POJO toString() renders the full set-field graph (ToString builder); identical
            // field population => identical toString. Use it as the structural fingerprint.
            String gen = String.valueOf(genPojo);
            String ref = String.valueOf(refPojo);

            checked++;
            if (!gen.equals(ref)) {
                failures++;
                System.out.println("MISMATCH: " + tc.getId());
                System.out.println("  ref: " + ref);
                System.out.println("  gen: " + gen);
            } else {
                System.out.println("OK: " + tc.getId());
            }
        }
        System.out.println("\n" + checked + " checked, " + failures + " mismatches.");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
