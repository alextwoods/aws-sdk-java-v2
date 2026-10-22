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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.openjdk.jmh.annotations.Param;
import software.amazon.awssdk.benchmark.serde.BenchmarkTestCaseLoader;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.protocols.query.internal.marshall.QueryProtocolMarshaller;
import software.amazon.awssdk.protocols.query.internal.unmarshall.QueryProtocolUnmarshaller;
import software.amazon.awssdk.protocols.rpcv2.SmithyRpcV2CborProtocolFactory;
import software.amazon.awssdk.protocols.rpcv2.internal.SdkStructuredRpcV2CborFactory;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlGenerator;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlProtocolMarshaller;
import software.amazon.awssdk.protocols.xml.internal.unmarshall.XmlProtocolUnmarshaller;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Cross-arm wire dump for the serde A/B (scripts/schema-serde/serde-ab.sh, check D4c of the serde benchmark plan).
 *
 * <p>Runs the LEGACY serde code of the Baseline_Build's ({@code fc8b74e9fe21}) {@code benchmark.serde} classes for
 * all 71 (class, test case) combinations against whatever {@code benchmarks.jar} is on the class path, and prints one
 * tab-separated row per combination. It only uses APIs whose source is identical on both arms (BenchmarkTestCaseLoader,
 * ShapeModelReflector, IntermediateModel and the legacy core/protocols marshallers and unmarshallers), with the
 * marshaller/unmarshaller calls copied from the baseline {@code @Benchmark} bodies. Equal dumps from two jars therefore
 * show that both jars build the same request objects and read the same responses from the same models and payloads.
 *
 * <p>Usage (Java 11+ source launcher): {@code java -cp <benchmarks.jar> scripts/schema-serde/SerdeWireDump.java > dump.tsv}
 *
 * <p>Columns: class, case, kind (marshall|unmarshall), method, uri (scheme://host[:port]path[?sorted raw query]),
 * headers (sorted lower-cased name=value, values joined with ','; '|' between headers), body_length, body_sha256. For
 * unmarshall rows method/uri/headers are '-' and body_length/body_sha256 describe the UTF-8 {@code toString()} of the
 * unmarshalled POJO (for Query: of the POJO and the sorted response metadata).
 *
 * <p>The case lists are those of {@code fc8b74e9fe21}. The program fails if a list differs from the {@code @Param}
 * list of the class in the jar, or if a test case id is missing from the jar's serde-tests data.
 */
public class SerdeWireDump {

    private static final String PKG = "software.amazon.awssdk.benchmark.serde.";

    interface Call {
        Object apply(String testCaseId) throws Exception;
    }

    private static final class Bench {
        private final String className;
        private final String kind;
        private final List<String> ids;
        private final Call call;

        Bench(String className, String kind, Call call, String... ids) {
            this.className = className;
            this.kind = kind;
            this.call = call;
            this.ids = Arrays.asList(ids);
        }
    }

    public static void main(String[] args) throws Exception {
        List<Bench> benches = benches();
        System.out.println("class\tcase\tkind\tmethod\turi\theaders\tbody_length\tbody_sha256");
        int rows = 0;
        for (Bench b : benches) {
            checkParams(b);
            for (String id : b.ids) {
                Object result = b.call.apply(id);
                System.out.println(b.className + "\t" + id + "\t" + b.kind + "\t"
                                   + ("marshall".equals(b.kind) ? marshallColumns((SdkHttpFullRequest) result)
                                                                 : unmarshallColumns(result)));
                rows++;
            }
        }
        if (rows != 71) {
            throw new IllegalStateException("Expected 71 combinations, dumped " + rows);
        }
    }

    private static List<Bench> benches() {
        List<Bench> b = new ArrayList<>();
        b.add(new Bench("JsonRpc10MarshallBenchmark", "marshall", SerdeWireDump::jsonRpc10Marshall,
                        "awsJson1_0_GetItemInput_Baseline", "awsJson1_0_HealthcheckRequest_Example",
                        "awsJson1_0_PutItemRequest_Baseline", "awsJson1_0_PutItemRequest_ShallowMap_S",
                        "awsJson1_0_PutItemRequest_ShallowMap_M", "awsJson1_0_PutItemRequest_ShallowMap_L",
                        "awsJson1_0_PutItemRequest_Nested_M", "awsJson1_0_PutItemRequest_Nested_L",
                        "awsJson1_0_PutItemRequest_MixedItem_S", "awsJson1_0_PutItemRequest_MixedItem_M",
                        "awsJson1_0_PutItemRequest_MixedItem_L", "awsJson1_0_PutItemRequest_BinaryData_S",
                        "awsJson1_0_PutItemRequest_BinaryData_M", "awsJson1_0_PutItemRequest_BinaryData_L"));
        b.add(new Bench("JsonRpc10UnmarshallBenchmark", "unmarshall", SerdeWireDump::jsonRpc10Unmarshall,
                        "awsJson1_0_GetItemOutput_Baseline", "awsJson1_0_GetItemOutput_S",
                        "awsJson1_0_GetItemOutput_M", "awsJson1_0_GetItemOutput_L",
                        "awsJson1_0_GetItemOutputBinary_S", "awsJson1_0_GetItemOutputBinary_M",
                        "awsJson1_0_GetItemOutputBinary_L", "awsJson1_0_HealthcheckResponse_Example"));
        b.add(new Bench("RestJsonMarshallBenchmark", "marshall", SerdeWireDump::restJsonMarshall,
                        "restJson1_CopyObjectRequest_Baseline", "restJson1_CopyObjectRequest_M",
                        "restJson1_PutObject_S", "restJson1_PutObject_M", "restJson1_PutObject_L"));
        b.add(new Bench("RestJsonUnmarshallBenchmark", "unmarshall", SerdeWireDump::restJsonUnmarshall,
                        "restJson1_CopyObjectOutput_Baseline", "restJson1_CopyObjectOutput_M",
                        "restJson1_GetObject_S", "restJson1_GetObject_M", "restJson1_GetObject_L"));
        b.add(new Bench("RestXmlMarshallBenchmark", "marshall", SerdeWireDump::restXmlMarshall,
                        "restXml_CopyObjectRequest_Baseline", "restXml_CopyObjectRequest_M",
                        "restXml_PutObject_S", "restXml_PutObject_M", "restXml_PutObject_L"));
        b.add(new Bench("RestXmlUnmarshallBenchmark", "unmarshall", SerdeWireDump::restXmlUnmarshall,
                        "restXml_CopyObjectOutput_Baseline", "restXml_CopyObjectOutput_M",
                        "restXml_GetObject_S", "restXml_GetObject_M", "restXml_GetObject_L"));
        b.add(new Bench("QueryMarshallBenchmark", "marshall", SerdeWireDump::queryMarshall,
                        "awsQuery_GetMetricDataRequest_S", "awsQuery_GetMetricDataRequest_M",
                        "awsQuery_GetMetricDataRequest_L", "awsQuery_PutMetricDataRequest_Baseline",
                        "awsQuery_PutMetricDataRequest_S", "awsQuery_PutMetricDataRequest_M",
                        "awsQuery_PutMetricDataRequest_L"));
        b.add(new Bench("QueryUnmarshallBenchmark", "unmarshall", SerdeWireDump::queryUnmarshall,
                        "awsQuery_GetMetricDataResponse_S", "awsQuery_GetMetricDataResponse_M",
                        "awsQuery_GetMetricDataResponse_L"));
        b.add(new Bench("RpcV2CborMarshallBenchmark", "marshall", SerdeWireDump::rpcV2CborMarshall,
                        "rpcv2Cbor_PutItemRequest_Baseline", "rpcv2Cbor_PutItemRequest_ShallowMap_S",
                        "rpcv2Cbor_PutItemRequest_ShallowMap_M", "rpcv2Cbor_PutItemRequest_ShallowMap_L",
                        "rpcv2Cbor_PutItemRequest_Nested_M", "rpcv2Cbor_PutItemRequest_Nested_L",
                        "rpcv2Cbor_PutItemRequest_MixedItem_S", "rpcv2Cbor_PutItemRequest_MixedItem_M",
                        "rpcv2Cbor_PutItemRequest_MixedItem_L", "rpcv2Cbor_PutItemRequest_BinaryData_S",
                        "rpcv2Cbor_PutItemRequest_BinaryData_M", "rpcv2Cbor_PutItemRequest_BinaryData_L"));
        b.add(new Bench("RpcV2CborUnmarshallBenchmark", "unmarshall", SerdeWireDump::rpcV2CborUnmarshall,
                        "rpcv2Cbor_GetItemOutput_Baseline", "rpcv2Cbor_GetItemOutput_S",
                        "rpcv2Cbor_GetItemOutput_M", "rpcv2Cbor_GetItemOutput_L",
                        "rpcv2Cbor_GetItemOutputBinary_S", "rpcv2Cbor_GetItemOutputBinary_M",
                        "rpcv2Cbor_GetItemOutputBinary_L"));
        return b;
    }

    /** Fails unless the jar's benchmark class declares exactly the baseline case list. */
    private static void checkParams(Bench b) throws Exception {
        Param param = Class.forName(PKG + b.className).getDeclaredField("testCaseId").getAnnotation(Param.class);
        List<String> declared = param == null ? null : Arrays.asList(param.value());
        if (!b.ids.equals(declared)) {
            throw new IllegalStateException(b.className + ": @Param testCaseId is " + declared
                                            + ", expected the fc8b74e9fe21 list " + b.ids);
        }
    }

    // ---- Output columns ----

    private static String marshallColumns(SdkHttpFullRequest r) throws Exception {
        StringBuilder uri = new StringBuilder(r.protocol()).append("://").append(r.host());
        if (!SdkHttpUtils.isUsingStandardPort(r.protocol(), r.port())) {
            uri.append(':').append(r.port());
        }
        uri.append(r.encodedPath());
        List<String> query = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : new TreeMap<>(r.rawQueryParameters()).entrySet()) {
            for (String v : e.getValue()) {
                query.add(e.getKey() + "=" + v);
            }
        }
        if (!query.isEmpty()) {
            uri.append('?').append(String.join("&", query));
        }
        Map<String, String> headers = new TreeMap<>();
        for (Map.Entry<String, List<String>> e : r.headers().entrySet()) {
            headers.merge(e.getKey().toLowerCase(Locale.ROOT), String.join(",", e.getValue()),
                          (a, c) -> a + "," + c);
        }
        List<String> headerCells = new ArrayList<>();
        headers.forEach((k, v) -> headerCells.add(k + "=" + v));
        byte[] body = new byte[0];
        if (r.contentStreamProvider().isPresent()) {
            try (InputStream in = r.contentStreamProvider().get().newStream()) {
                body = IoUtils.toByteArray(in);
            }
        }
        return r.method() + "\t" + clean(uri.toString()) + "\t" + clean(String.join("|", headerCells)) + "\t"
               + body.length + "\t" + sha256(body);
    }

    private static String unmarshallColumns(Object result) throws Exception {
        String text;
        if (result instanceof Pair) {
            Pair<?, ?> pair = (Pair<?, ?>) result;
            Object right = pair.right() instanceof Map ? new TreeMap<>((Map<?, ?>) pair.right()) : pair.right();
            text = pair.left() + "\n" + right;
        } else {
            text = String.valueOf(result);
        }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        return "-\t-\t-\t" + bytes.length + "\t" + sha256(bytes);
    }

    private static String clean(String cell) {
        return cell.replace('\t', ' ').replace('\n', ' ');
    }

    private static String sha256(byte[] bytes) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte x : MessageDigest.getInstance("SHA-256").digest(bytes)) {
            hex.append(String.format("%02x", x));
        }
        return hex.toString();
    }

    // ---- Marshall: the fc8b74e9fe21 @Benchmark bodies ----

    static SdkHttpFullRequest jsonRpc10Marshall(String testCaseId) {
        URI endpoint = URI.create("http://localhost/");
        String contentType = "application/x-amz-json-1.0";
        MarshallSetup setup = marshallSetup("models/awsjsonrpc10dataplane-1999-12-31-intermediate.json",
                                            "serde-tests/json-rpc-1-0/input/json_1_0.json", testCaseId);
        AwsJsonProtocolMetadata protocolMetadata = AwsJsonProtocolMetadata.builder()
                .protocol(AwsJsonProtocol.AWS_JSON)
                .contentType(contentType)
                .build();
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = JsonProtocolMarshallerBuilder.create()
                .endpoint(endpoint)
                .jsonGenerator(AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY.createWriter(contentType))
                .contentType(contentType)
                .operationInfo(setup.operationInfo)
                .sendExplicitNullForPayload(false)
                .protocolMetadata(protocolMetadata)
                .build();
        return marshaller.marshall(setup.request);
    }

    static SdkHttpFullRequest restJsonMarshall(String testCaseId) {
        MarshallSetup setup = marshallSetup("models/awsrestjsondataplane-1999-12-31-intermediate.json",
                                            "serde-tests/rest-json/input/rest_json.json", testCaseId);
        AwsJsonProtocolFactory protocolFactory = AwsJsonProtocolFactory.builder()
                .clientConfiguration(SdkClientConfiguration.builder()
                        .option(SdkClientOption.ENDPOINT, URI.create("http://localhost"))
                        .build())
                .defaultServiceExceptionSupplier(null)
                .protocol(AwsJsonProtocol.REST_JSON)
                .protocolVersion("1.1")
                .build();
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = protocolFactory.createProtocolMarshaller(setup.operationInfo);
        return marshaller.marshall(setup.request);
    }

    static SdkHttpFullRequest restXmlMarshall(String testCaseId) {
        URI endpoint = URI.create("http://localhost/");
        String xmlns = "https://awsrestxmldataplane.amazonaws.com";
        MarshallSetup setup = marshallSetup("models/awsrestxmldataplane-1999-12-31-intermediate.json",
                                            "serde-tests/rest-xml/input/rest_xml.json", testCaseId);
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = XmlProtocolMarshaller.builder()
                .endpoint(endpoint)
                .xmlGenerator(XmlGenerator.create(xmlns, false))
                .operationInfo(setup.operationInfo)
                .build();
        return marshaller.marshall(setup.request);
    }

    static SdkHttpFullRequest queryMarshall(String testCaseId) {
        URI endpoint = URI.create("http://localhost/");
        MarshallSetup setup = marshallSetup("models/awsquerydataplane-1999-12-31-intermediate.json",
                                            "serde-tests/query/input/query.json", testCaseId);
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = QueryProtocolMarshaller.builder()
                .endpoint(endpoint)
                .operationInfo(setup.operationInfo)
                .build();
        return marshaller.marshall(setup.request);
    }

    static SdkHttpFullRequest rpcV2CborMarshall(String testCaseId) {
        URI endpoint = URI.create("http://localhost/");
        String contentType = "application/cbor";
        MarshallSetup setup = marshallSetup("models/smithyrpcv2cbordataplane-1999-12-31-intermediate.json",
                                            "serde-tests/rpc-v2-cbor/input/rpc_v2_cbor.json", testCaseId);
        AwsJsonProtocolMetadata protocolMetadata = AwsJsonProtocolMetadata.builder()
                .protocol(AwsJsonProtocol.SMITHY_RPC_V2_CBOR)
                .contentType(contentType)
                .build();
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = JsonProtocolMarshallerBuilder.create()
                .endpoint(endpoint)
                .jsonGenerator(SdkStructuredRpcV2CborFactory.SDK_CBOR_FACTORY.createWriter(contentType))
                .contentType(contentType)
                .operationInfo(setup.operationInfo)
                .sendExplicitNullForPayload(false)
                .protocolMetadata(protocolMetadata)
                .build();
        return marshaller.marshall(setup.request);
    }

    // ---- Unmarshall: the fc8b74e9fe21 @Benchmark bodies ----

    static Object jsonRpc10Unmarshall(String testCaseId) throws Exception {
        UnmarshallSetup setup = unmarshallSetup("serde-tests/json-rpc-1-0/output/json_1_0.json", testCaseId, false,
                                                "software.amazon.awssdk.services.jsonrpc10dataplane.model.");
        JsonProtocolUnmarshaller unmarshaller = JsonProtocolUnmarshaller.builder()
                .enableFastUnmarshalling(true)
                .protocolUnmarshallDependencies(JsonProtocolUnmarshaller.defaultProtocolUnmarshallDependencies())
                .build();
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                .statusCode(setup.statusCode)
                .putHeader("Content-Type", "application/x-amz-json-1.0")
                .content(AbortableInputStream.create(new ByteArrayInputStream(setup.responseBytes)))
                .build();
        return unmarshaller.unmarshall((SdkPojo) setup.emptyResponse.toBuilder(), response);
    }

    static Object restJsonUnmarshall(String testCaseId) throws Exception {
        UnmarshallSetup setup = unmarshallSetup("serde-tests/rest-json/output/rest_json.json", testCaseId, false,
                                                "software.amazon.awssdk.services.restjsondataplane.model.");
        JsonProtocolUnmarshaller unmarshaller = JsonProtocolUnmarshaller.builder()
                .enableFastUnmarshalling(true)
                .protocolUnmarshallDependencies(JsonProtocolUnmarshaller.defaultProtocolUnmarshallDependencies())
                .build();
        SdkHttpFullResponse.Builder responseBuilder = SdkHttpFullResponse.builder()
                .statusCode(setup.statusCode)
                .putHeader("Content-Type", "application/x-amz-json-1.1")
                .content(AbortableInputStream.create(new ByteArrayInputStream(setup.responseBytes)));
        if (setup.responseHeaders != null) {
            setup.responseHeaders.forEach(responseBuilder::putHeader);
        }
        return unmarshaller.unmarshall((SdkPojo) setup.emptyResponse.toBuilder(), responseBuilder.build());
    }

    static Object restXmlUnmarshall(String testCaseId) throws Exception {
        UnmarshallSetup setup = unmarshallSetup("serde-tests/rest-xml/output/rest_xml.json", testCaseId, false,
                                                "software.amazon.awssdk.services.restxmldataplane.model.");
        XmlProtocolUnmarshaller unmarshaller = XmlProtocolUnmarshaller.create();
        SdkHttpFullResponse.Builder responseBuilder = SdkHttpFullResponse.builder()
                .statusCode(setup.statusCode)
                .content(AbortableInputStream.create(new ByteArrayInputStream(setup.responseBytes)));
        if (setup.responseHeaders != null) {
            setup.responseHeaders.forEach(responseBuilder::putHeader);
        }
        return unmarshaller.unmarshall((SdkPojo) setup.emptyResponse.toBuilder(), responseBuilder.build());
    }

    static Object queryUnmarshall(String testCaseId) throws Exception {
        UnmarshallSetup setup = unmarshallSetup("serde-tests/query/output/query.json", testCaseId, false,
                                                "software.amazon.awssdk.services.querydataplane.model.");
        QueryProtocolUnmarshaller unmarshaller = QueryProtocolUnmarshaller.builder()
                .hasResultWrapper(true)
                .build();
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                .statusCode(setup.statusCode)
                .content(AbortableInputStream.create(new ByteArrayInputStream(setup.responseBytes)))
                .build();
        return unmarshaller.unmarshall((SdkPojo) setup.emptyResponse.toBuilder(), response);
    }

    static Object rpcV2CborUnmarshall(String testCaseId) throws Exception {
        UnmarshallSetup setup = unmarshallSetup("serde-tests/rpc-v2-cbor/output/rpc_v2_cbor.json", testCaseId, true,
                                                "software.amazon.awssdk.services.rpccbordataplane.model.");
        JsonProtocolUnmarshaller unmarshaller = JsonProtocolUnmarshaller.builder()
                .enableFastUnmarshalling(true)
                .protocolUnmarshallDependencies(SmithyRpcV2CborProtocolFactory.defaultProtocolUnmarshallDependencies())
                .build();
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                .statusCode(setup.statusCode)
                .putHeader("Content-Type", "application/cbor")
                .content(AbortableInputStream.create(new ByteArrayInputStream(setup.responseBytes)))
                .build();
        return unmarshaller.unmarshall((SdkPojo) setup.emptyResponse.toBuilder(), response);
    }

    // ---- The fc8b74e9fe21 @Setup steps ----

    private static MarshallSetup marshallSetup(String intermediateModelPath, String testDataPath, String testCaseId) {
        BenchmarkTestCaseLoader.MarshallTestCase testCase = BenchmarkTestCaseLoader
                .loadMarshallTestCases(testDataPath).stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));
        IntermediateModel model = BenchmarkTestCaseLoader.loadIntermediateModel(intermediateModelPath);
        ShapeModelReflector reflector = new ShapeModelReflector(model, testCase.getOperationName() + "Request",
                                                                testCase.getInputData());
        SdkPojo request = (SdkPojo) reflector.createShapeObject();
        OperationInfo operationInfo = BenchmarkTestCaseLoader.buildOperationInfo(model, testCase);
        return new MarshallSetup(request, operationInfo);
    }

    private static UnmarshallSetup unmarshallSetup(String testDataPath, String testCaseId, boolean base64Body,
                                                   String modelPackage) throws Exception {
        BenchmarkTestCaseLoader.UnmarshallTestCase testCase = BenchmarkTestCaseLoader
                .loadUnmarshallTestCases(testDataPath).stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));
        byte[] responseBytes = base64Body
                               ? Base64.getDecoder().decode(testCase.getResponseBody().trim())
                               : testCase.getResponseBody().getBytes(StandardCharsets.UTF_8);
        int statusCode = testCase.getStatusCode() != null ? testCase.getStatusCode() : 200;
        Class<?> responseClass = Class.forName(modelPackage + testCase.getOperationName() + "Response");
        Method builderMethod = responseClass.getMethod("builder");
        SdkResponse.Builder builder = (SdkResponse.Builder) builderMethod.invoke(null);
        return new UnmarshallSetup(responseBytes, statusCode, testCase.getHeaders(), builder.build());
    }

    private static final class MarshallSetup {
        private final SdkPojo request;
        private final OperationInfo operationInfo;

        MarshallSetup(SdkPojo request, OperationInfo operationInfo) {
            this.request = request;
            this.operationInfo = operationInfo;
        }
    }

    private static final class UnmarshallSetup {
        private final byte[] responseBytes;
        private final int statusCode;
        private final Map<String, String> responseHeaders;
        private final SdkResponse emptyResponse;

        UnmarshallSetup(byte[] responseBytes, int statusCode, Map<String, String> responseHeaders,
                        SdkResponse emptyResponse) {
            this.responseBytes = responseBytes;
            this.statusCode = statusCode;
            this.responseHeaders = responseHeaders;
            this.emptyResponse = emptyResponse;
        }
    }
}
