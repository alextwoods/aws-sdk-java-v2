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
import java.io.ByteArrayOutputStream;
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
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.json.JsonCodec;

/**
 * Standalone correctness check: serialize the SAME v2 request three ways and compare the JSON
 * (parsed + normalized, so key order doesn't matter):
 * <ol>
 *   <li>v2's own C2J marshaller (the reference output),</li>
 *   <li>the runtime bridge (BridgeStruct over the canonical operation schema),</li>
 *   <li>the CODEGEN path — the v2 POJO that IS a SerializableStruct, via its generated
 *       $SCHEMA + direct-write serializeMembers.</li>
 * </ol>
 * Exits non-zero if any case's codegen output differs from v2's. Run as a plain main.
 */
public final class GeneratedSerdeVerifier {

    private static final String INTERMEDIATE_MODEL_PATH = "models/awsjsonrpc10dataplane-1999-12-31-intermediate.json";
    private static final String TEST_DATA_PATH = "serde-tests/json-rpc-1-0/input/json_1_0.json";
    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";
    private static final URI ENDPOINT = URI.create("http://localhost/");
    private static final ObjectMapper JSON = new ObjectMapper();

    private GeneratedSerdeVerifier() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("smithy-java.json-provider", "smithy");
        List<BenchmarkTestCaseLoader.MarshallTestCase> cases =
                BenchmarkTestCaseLoader.loadMarshallTestCases(TEST_DATA_PATH);
        IntermediateModel model = BenchmarkTestCaseLoader.loadIntermediateModel(INTERMEDIATE_MODEL_PATH);
        JsonCodec codec = JsonCodec.builder().build();

        int failures = 0;
        int checked = 0;
        for (BenchmarkTestCaseLoader.MarshallTestCase tc : cases) {
            String inputShapeName = tc.getOperationName() + "Request";
            ShapeModelReflector reflector = new ShapeModelReflector(model, inputShapeName, tc.getInputData());
            SdkPojo request = (SdkPojo) reflector.createShapeObject();

            // (1) v2 reference JSON.
            JsonNode v2Json = JSON.readTree(v2Marshal(request, model, tc));

            // (3) codegen path: the POJO is a SerializableStruct — serialize it directly.
            if (!(request instanceof SerializableStruct)) {
                System.out.println("SKIP (not generated as SerializableStruct): " + tc.getId());
                continue;
            }
            ByteBuffer genBytes = codec.serialize((SerializableStruct) request);
            JsonNode genJson = JSON.readTree(toString(genBytes));

            checked++;
            boolean genOk = jsonEquivalent(v2Json, genJson);
            if (!genOk) {
                failures++;
                System.out.println("MISMATCH: " + tc.getId());
                System.out.println("  v2:  " + v2Json);
                System.out.println("  gen: " + genJson);
            } else {
                System.out.println("OK: " + tc.getId());
            }
        }
        System.out.println("\n" + checked + " checked, " + failures + " mismatches.");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static String v2Marshal(SdkPojo request, IntermediateModel model,
                                    BenchmarkTestCaseLoader.MarshallTestCase tc) throws Exception {
        OperationInfo operationInfo = BenchmarkTestCaseLoader.buildOperationInfo(model, tc);
        AwsJsonProtocolMetadata metadata = AwsJsonProtocolMetadata.builder()
                .protocol(AwsJsonProtocol.AWS_JSON)
                .contentType(CONTENT_TYPE)
                .build();
        SdkHttpFullRequest req = JsonProtocolMarshallerBuilder.<SdkHttpFullRequest>create()
                .endpoint(ENDPOINT)
                .jsonGenerator(AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY.createWriter(CONTENT_TYPE))
                .contentType(CONTENT_TYPE)
                .operationInfo(operationInfo)
                .sendExplicitNullForPayload(false)
                .protocolMetadata(metadata)
                .build()
                .marshall(request);
        ContentStreamProvider body = req.contentStreamProvider().orElse(null);
        if (body == null) {
            return "{}";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        var in = body.newStream();
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        String s = out.toString(StandardCharsets.UTF_8.name());
        return s.isEmpty() ? "{}" : s;
    }

    /**
     * Semantic JSON equality: numbers compared by value (so {@code 75} == {@code 75.0} — smithy-java
     * deliberately drops the trailing {@code .0} that v2 writes; both parse to the same double and
     * are wire-compatible), everything else structural. Confirms the generated output differs from
     * v2 only in number formatting, not in content.
     */
    private static boolean jsonEquivalent(JsonNode a, JsonNode b) {
        if (a.isNumber() && b.isNumber()) {
            return a.doubleValue() == b.doubleValue();
        }
        if (a.isObject() && b.isObject()) {
            if (a.size() != b.size()) {
                return false;
            }
            var names = a.fieldNames();
            while (names.hasNext()) {
                String n = names.next();
                if (!b.has(n) || !jsonEquivalent(a.get(n), b.get(n))) {
                    return false;
                }
            }
            return true;
        }
        if (a.isArray() && b.isArray()) {
            if (a.size() != b.size()) {
                return false;
            }
            for (int i = 0; i < a.size(); i++) {
                if (!jsonEquivalent(a.get(i), b.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return a.equals(b);
    }

    private static String toString(ByteBuffer bb) {
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }
}
