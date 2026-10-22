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

package software.amazon.awssdk.benchmark.serde;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
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
import software.amazon.awssdk.utils.Pair;

/**
 * The legacy serde code the {@code serde} benchmarks measure on the Baseline_Build ({@code fc8b74e9fe21}): each method
 * reproduces the {@code @Setup} and the {@code @Benchmark} body of the baseline class of the same name, verbatim
 * except that the result is returned instead of consumed. {@code SerdeBenchmarkEquivalenceTest} compares it with the
 * schema-based path the feature classes measure.
 */
final class LegacySerdeReference {

    private LegacySerdeReference() {
    }

    // ---- Marshall ----

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
                .jsonGenerator(AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY
                        .createWriter(contentType))
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

        ProtocolMarshaller<SdkHttpFullRequest> marshaller = protocolFactory
                .createProtocolMarshaller(setup.operationInfo);
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
                .jsonGenerator(SdkStructuredRpcV2CborFactory.SDK_CBOR_FACTORY
                        .createWriter(contentType))
                .contentType(contentType)
                .operationInfo(setup.operationInfo)
                .sendExplicitNullForPayload(false)
                .protocolMetadata(protocolMetadata)
                .build();
        return marshaller.marshall(setup.request);
    }

    // ---- Unmarshall ----

    static SdkPojo jsonRpc10Unmarshall(String testCaseId) throws Exception {
        String contentType = "application/x-amz-json-1.0";
        UnmarshallSetup setup = unmarshallSetup("serde-tests/json-rpc-1-0/output/json_1_0.json", testCaseId, false,
                                                "software.amazon.awssdk.services.jsonrpc10dataplane.model.");
        JsonProtocolUnmarshaller unmarshaller = JsonProtocolUnmarshaller.builder()
                .enableFastUnmarshalling(true)
                .protocolUnmarshallDependencies(
                        JsonProtocolUnmarshaller.defaultProtocolUnmarshallDependencies())
                .build();

        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                .statusCode(setup.statusCode)
                .putHeader("Content-Type", contentType)
                .content(AbortableInputStream.create(new ByteArrayInputStream(setup.responseBytes)))
                .build();
        return unmarshaller.unmarshall((SdkPojo) setup.emptyResponse.toBuilder(), response);
    }

    static SdkPojo restJsonUnmarshall(String testCaseId) throws Exception {
        String contentType = "application/x-amz-json-1.1";
        UnmarshallSetup setup = unmarshallSetup("serde-tests/rest-json/output/rest_json.json", testCaseId, false,
                                                "software.amazon.awssdk.services.restjsondataplane.model.");
        JsonProtocolUnmarshaller unmarshaller = JsonProtocolUnmarshaller.builder()
                .enableFastUnmarshalling(true)
                .protocolUnmarshallDependencies(
                        JsonProtocolUnmarshaller.defaultProtocolUnmarshallDependencies())
                .build();

        SdkHttpFullResponse.Builder responseBuilder = SdkHttpFullResponse.builder()
                .statusCode(setup.statusCode)
                .putHeader("Content-Type", contentType)
                .content(AbortableInputStream.create(new ByteArrayInputStream(setup.responseBytes)));
        if (setup.responseHeaders != null) {
            setup.responseHeaders.forEach(responseBuilder::putHeader);
        }
        return unmarshaller.unmarshall((SdkPojo) setup.emptyResponse.toBuilder(), responseBuilder.build());
    }

    static SdkPojo restXmlUnmarshall(String testCaseId) throws Exception {
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

    /**
     * Returns what the baseline consumes: the unmarshalled response paired with the response metadata the legacy
     * Query unmarshaller reads from {@code ResponseMetadata}.
     */
    static Pair<SdkPojo, Map<String, String>> queryUnmarshall(String testCaseId) throws Exception {
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

    static SdkPojo rpcV2CborUnmarshall(String testCaseId) throws Exception {
        String contentType = "application/cbor";
        UnmarshallSetup setup = unmarshallSetup("serde-tests/rpc-v2-cbor/output/rpc_v2_cbor.json", testCaseId, true,
                                                "software.amazon.awssdk.services.rpccbordataplane.model.");
        JsonProtocolUnmarshaller unmarshaller = JsonProtocolUnmarshaller.builder()
                .enableFastUnmarshalling(true)
                .protocolUnmarshallDependencies(
                        SmithyRpcV2CborProtocolFactory.defaultProtocolUnmarshallDependencies())
                .build();

        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                .statusCode(setup.statusCode)
                .putHeader("Content-Type", contentType)
                .content(AbortableInputStream.create(new ByteArrayInputStream(setup.responseBytes)))
                .build();
        return unmarshaller.unmarshall((SdkPojo) setup.emptyResponse.toBuilder(), response);
    }

    // ---- Baseline @Setup steps ----

    /**
     * Steps 1 to 4 of the baseline marshall {@code setup()}: the test case, the request built by
     * {@link ShapeModelReflector} and the {@link OperationInfo}.
     */
    private static MarshallSetup marshallSetup(String intermediateModelPath, String testDataPath, String testCaseId) {
        BenchmarkTestCaseLoader.MarshallTestCase testCase = BenchmarkTestCaseLoader
                .loadMarshallTestCases(testDataPath).stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));
        IntermediateModel model = BenchmarkTestCaseLoader.loadIntermediateModel(intermediateModelPath);
        String inputShapeName = testCase.getOperationName() + "Request";
        ShapeModelReflector reflector = new ShapeModelReflector(model, inputShapeName, testCase.getInputData());
        SdkPojo request = (SdkPojo) reflector.createShapeObject();
        OperationInfo operationInfo = BenchmarkTestCaseLoader.buildOperationInfo(model, testCase);
        return new MarshallSetup(request, operationInfo);
    }

    /**
     * Steps 1, 2 and 4 of the baseline unmarshall {@code setup()}: the response bytes (base64-decoded for CBOR),
     * status code and headers, and the empty response resolved by reflection.
     */
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

        private MarshallSetup(SdkPojo request, OperationInfo operationInfo) {
            this.request = request;
            this.operationInfo = operationInfo;
        }
    }

    private static final class UnmarshallSetup {
        private final byte[] responseBytes;
        private final int statusCode;
        private final Map<String, String> responseHeaders;
        private final SdkResponse emptyResponse;

        private UnmarshallSetup(byte[] responseBytes, int statusCode, Map<String, String> responseHeaders,
                                SdkResponse emptyResponse) {
            this.responseBytes = responseBytes;
            this.statusCode = statusCode;
            this.responseHeaders = responseHeaders;
            this.emptyResponse = emptyResponse;
        }
    }
}
