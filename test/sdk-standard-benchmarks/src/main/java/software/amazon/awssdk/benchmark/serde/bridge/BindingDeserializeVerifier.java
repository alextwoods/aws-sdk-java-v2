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
import java.util.function.Function;
import software.amazon.awssdk.benchmark.serde.BenchmarkTestCaseLoader;
import software.amazon.awssdk.bridge.smithyjava.serde.BridgeOutputBuilder;
import software.amazon.awssdk.bridge.smithyjava.serde.BridgeOutputOperation;
import software.amazon.awssdk.bridge.smithyjava.serde.GeneratedOutputOperation;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.smithy.java.aws.client.awsquery.AwsQueryClientProtocol;
import software.amazon.smithy.java.aws.client.restjson.RestJsonClientProtocol;
import software.amazon.smithy.java.aws.client.restxml.RestXmlClientProtocol;
import software.amazon.smithy.java.client.core.ClientProtocol;
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
 * Deserialize correctness for the binding protocols: deserialize each response BOTH via the bridge
 * (BridgeOutputBuilder, leaderboard-validated reference) AND via the CODEGEN path (generated v2
 * builder driven by the full HTTP-binding deserializer), and compare the resulting v2 POJO by
 * toString(). Covers header/payload binding (e.g. CopyObjectResponse has 10 header members).
 */
public final class BindingDeserializeVerifier {

    private BindingDeserializeVerifier() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("smithy-java.json-provider", "smithy");
        System.setProperty("smithy-java.xml-provider", "smithy");
        int fail = 0;
        fail += run("restJson", "serde-tests/rest-json/output/rest_json.json", "/smithy-models/rest-json/model.json",
                "com.amazonaws.sdk.benchmark#AwsRestJsonDataPlane",
                "software.amazon.awssdk.services.restjsondataplane.model.", "restJson1_", "application/json",
                RestJsonClientProtocol::new, false);
        fail += run("restXml", "serde-tests/rest-xml/output/rest_xml.json", "/smithy-models/rest-xml/model.json",
                "com.amazonaws.sdk.benchmark#AwsRestXmlDataPlane",
                "software.amazon.awssdk.services.restxmldataplane.model.", "restXml_", "application/xml",
                RestXmlClientProtocol::new, false);
        fail += run("awsQuery", "serde-tests/query/output/query.json", "/smithy-models/query/model.json",
                "com.amazonaws.sdk.benchmark#AwsQueryDataPlane",
                "software.amazon.awssdk.services.querydataplane.model.", "awsQuery_", "text/xml",
                sid -> new AwsQueryClientProtocol(sid, "2020-01-01"), false);
        System.out.println("\n==== " + (fail == 0 ? "ALL BINDING DESER == bridge" : fail + " MISMATCHES") + " ====");
        if (fail > 0) {
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private static int run(String label, String testData, String smithyModelPath, String serviceId,
                           String modelPkg, String prefix, String contentType,
                           Function<ShapeId, Object> protocolFactory, boolean base64Body) throws Exception {
        List<BenchmarkTestCaseLoader.UnmarshallTestCase> cases =
                BenchmarkTestCaseLoader.loadUnmarshallTestCases(testData);
        Model smithyModel;
        try (InputStream is = BindingDeserializeVerifier.class.getResourceAsStream(smithyModelPath)) {
            smithyModel = Model.assembler().addUnparsedModel("m.json",
                    new String(is.readAllBytes(), StandardCharsets.UTF_8)).assemble().unwrap();
        }
        ShapeId sid = ShapeId.from(serviceId);
        Object protocolObj = protocolFactory.apply(sid);
        DynamicClient client = DynamicClient.builder().model(smithyModel).serviceId(sid)
                .protocol((ClientProtocol<?, ?>) protocolObj).endpoint("http://localhost/").build();
        Context context = Context.create();

        // v2's own unmarshaller (per protocol) — the true reference. Returns the deserialized v2 POJO.
        V2Unmarshaller v2Unmarshal = v2UnmarshallerFor(label, modelPkg, base64Body);

        int ok = 0;
        int fail = 0;
        int skip = 0;
        System.out.println("== " + label + " ==");
        for (BenchmarkTestCaseLoader.UnmarshallTestCase tc : cases) {
            if (!tc.getId().startsWith(prefix)) {
                continue;
            }
            byte[] bytes = base64Body ? Base64.getDecoder().decode(tc.getResponseBody().trim())
                    : tc.getResponseBody().getBytes(StandardCharsets.UTF_8);
            int status = tc.getStatusCode() != null ? tc.getStatusCode() : 200;
            HttpResponse response = HttpResponse.of(HttpVersion.HTTP_1_1, status,
                    HttpHeaders.of(java.util.Map.of("Content-Type", List.of(contentType))), DataStream.ofBytes(bytes));

            ApiOperation<?, ?> realOp = client.getOperation(tc.getOperationName());
            String fqcn = modelPkg + tc.getOperationName() + "Response";
            Method builderMethod;
            try {
                builderMethod = Class.forName(fqcn).getMethod("builder");
            } catch (ClassNotFoundException e) {
                skip++;
                continue;
            }
            if (!(builderMethod.invoke(null) instanceof ShapeBuilder)) {
                System.out.println("  SKIP (not generated): " + tc.getId());
                skip++;
                continue;
            }

            software.amazon.smithy.java.client.http.HttpClientProtocol proto =
                    (software.amazon.smithy.java.client.http.HttpClientProtocol) protocolObj;
            TypeRegistry reg = realOp.errorRegistry();

            String ref;
            String gen;
            try {
                // v2 REFERENCE: v2's own unmarshaller (the true oracle — the bridge has its own deser
                // gaps, e.g. it drops body timestamps, so it can't be the reference).
                ref = String.valueOf(v2Unmarshal.unmarshall(tc, bytes, status));

                ApiOperation<SerializableStruct, ?> genOp = new GeneratedOutputOperation(realOp, () -> {
                    try {
                        return (ShapeBuilder<? extends SerializableStruct>) builderMethod.invoke(null);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                });
                gen = String.valueOf(proto.deserializeResponse((ApiOperation) genOp, context, reg, null, response));
            } catch (Exception e) {
                fail++;
                System.out.println("  ERROR: " + tc.getId() + " -> " + e.getClass().getSimpleName()
                                   + ": " + e.getMessage());
                continue;
            }
            // The harness injects a synthetic Content-Type header that the smithy (gen) path binds
            // to a @httpHeader ContentType member but v2's unmarshaller (fed no headers here) does
            // not — a harness asymmetry, not a serde difference. Normalize it out of both sides.
            ref = stripContentType(ref);
            gen = stripContentType(gen);
            if (ref.equals(gen)) {
                ok++;
                System.out.println("  OK: " + tc.getId());
            } else {
                fail++;
                System.out.println("  MISMATCH: " + tc.getId());
                System.out.println("    ref: " + trunc(ref));
                System.out.println("    gen: " + trunc(gen));
            }
        }
        System.out.println("  -> " + ok + " ok, " + fail + " mismatch, " + skip + " skip");
        return fail;
    }

    private static String trunc(String s) {
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    private static String stripContentType(String s) {
        return s.replaceAll(", ContentType=[^,)]+", "").replaceAll("ContentType=[^,)]+, ", "");
    }

    @FunctionalInterface
    private interface V2Unmarshaller {
        Object unmarshall(BenchmarkTestCaseLoader.UnmarshallTestCase tc, byte[] bytes, int status) throws Exception;
    }

    // v2's own per-protocol unmarshaller, the correctness oracle. Builds a fresh response builder
    // per case and feeds body bytes (the harness has no response headers, same as the gen path).
    private static V2Unmarshaller v2UnmarshallerFor(String label, String modelPkg, boolean base64Body) {
        if (label.equals("restJson")) {
            software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory f =
                    software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory.builder()
                        .clientConfiguration(software.amazon.awssdk.core.client.config.SdkClientConfiguration.builder()
                            .option(software.amazon.awssdk.core.client.config.SdkClientOption.ENDPOINT,
                                    java.net.URI.create("http://localhost")).build())
                        .defaultServiceExceptionSupplier(null)
                        .protocol(software.amazon.awssdk.protocols.json.AwsJsonProtocol.REST_JSON)
                        .protocolVersion("1.1").build();
            software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller u =
                    software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller.builder()
                        .enableFastUnmarshalling(true)
                        .protocolUnmarshallDependencies(software.amazon.awssdk.protocols.json.internal.unmarshall
                            .JsonProtocolUnmarshaller.defaultProtocolUnmarshallDependencies())
                        .build();
            return (tc, bytes, status) -> {
                SdkPojo builder = freshBuilder(modelPkg, tc);
                software.amazon.awssdk.http.SdkHttpFullResponse resp =
                        software.amazon.awssdk.http.SdkHttpFullResponse.builder().statusCode(status)
                            .putHeader("Content-Type", "application/json")
                            .content(software.amazon.awssdk.http.AbortableInputStream.create(
                                    new java.io.ByteArrayInputStream(bytes))).build();
                return u.unmarshall(builder, resp);
            };
        } else if (label.equals("restXml")) {
            software.amazon.awssdk.protocols.xml.internal.unmarshall.XmlProtocolUnmarshaller u =
                    software.amazon.awssdk.protocols.xml.internal.unmarshall.XmlProtocolUnmarshaller.create();
            return (tc, bytes, status) -> {
                SdkPojo builder = freshBuilder(modelPkg, tc);
                software.amazon.awssdk.http.SdkHttpFullResponse resp =
                        software.amazon.awssdk.http.SdkHttpFullResponse.builder().statusCode(status)
                            .content(software.amazon.awssdk.http.AbortableInputStream.create(
                                    new java.io.ByteArrayInputStream(bytes))).build();
                return u.unmarshall(builder, resp);
            };
        } else { // awsQuery
            software.amazon.awssdk.protocols.query.internal.unmarshall.QueryProtocolUnmarshaller u =
                    software.amazon.awssdk.protocols.query.internal.unmarshall.QueryProtocolUnmarshaller.builder()
                        .hasResultWrapper(true).build();
            return (tc, bytes, status) -> {
                SdkPojo builder = freshBuilder(modelPkg, tc);
                software.amazon.awssdk.http.SdkHttpFullResponse resp =
                        software.amazon.awssdk.http.SdkHttpFullResponse.builder().statusCode(status)
                            .content(software.amazon.awssdk.http.AbortableInputStream.create(
                                    new java.io.ByteArrayInputStream(bytes))).build();
                Object result = u.unmarshall(builder, resp);
                // QueryProtocolUnmarshaller returns Pair<Response, metadata>; unwrap to the POJO.
                if (result instanceof software.amazon.awssdk.utils.Pair) {
                    return ((software.amazon.awssdk.utils.Pair<?, ?>) result).left();
                }
                return result;
            };
        }
    }

    private static SdkPojo freshBuilder(String modelPkg, BenchmarkTestCaseLoader.UnmarshallTestCase tc)
            throws Exception {
        Class<?> rc = Class.forName(modelPkg + tc.getOperationName() + "Response");
        return (SdkPojo) rc.getMethod("builder").invoke(null);
    }
}
