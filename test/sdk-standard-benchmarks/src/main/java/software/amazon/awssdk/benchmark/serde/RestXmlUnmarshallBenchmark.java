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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;

/**
 * JMH benchmark for REST XML unmarshalling (deserialization).
 *
 * <p>
 * Measures the schema-based path: {@code RestXmlClientProtocol.deserializeResponse} into the generated output.
 * The protocol and the operation are resolved once per trial, as the generated client resolves them once.
 * </p>
 *
 * <p>
 * SampleTime mode is used (instead of AverageTime) to enable percentile
 * collection (p50, p90, p95, p99) for the cross-language output schema.
 * </p>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class RestXmlUnmarshallBenchmark {

    private static final String TEST_DATA_PATH = "serde-tests/rest-xml/output/rest_xml.json";

    @Param({
            "restXml_CopyObjectOutput_Baseline",
            "restXml_CopyObjectOutput_M",
            "restXml_GetObject_S",
            "restXml_GetObject_M",
            "restXml_GetObject_L",
    })
    private String testCaseId;

    private byte[] responseBytes;
    private int statusCode;
    private java.util.Map<String, String> responseHeaders;
    private ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol;
    private ApiOperation<?, ?> operation;
    private ExecutionAttributes attributes;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // 1. Load test cases
        List<BenchmarkTestCaseLoader.UnmarshallTestCase> allCases = BenchmarkTestCaseLoader
                .loadUnmarshallTestCases(TEST_DATA_PATH);
        BenchmarkTestCaseLoader.UnmarshallTestCase testCase = allCases.stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));

        // 2. Pre-store response bytes
        this.responseBytes = testCase.getResponseBody().getBytes(StandardCharsets.UTF_8);
        this.statusCode = testCase.getStatusCode() != null ? testCase.getStatusCode() : 200;
        this.responseHeaders = testCase.getHeaders();

        // 3. Resolve the client protocol and the operation (same as the generated client)
        this.protocol = SerdeBenchmarkProtocols.REST_XML.createProtocol();
        this.operation = SerdeBenchmarkProtocols.REST_XML.operation(testCase.getOperationName());
        this.attributes = new ExecutionAttributes();
    }

    @Benchmark
    public void unmarshall(Blackhole bh) throws Exception {
        bh.consume(unmarshallOnce());
    }

    SerializableStruct unmarshallOnce() {
        SdkHttpFullResponse.Builder responseBuilder = SdkHttpFullResponse.builder()
                .statusCode(statusCode)
                .content(AbortableInputStream.create(new ByteArrayInputStream(responseBytes)));
        if (responseHeaders != null) {
            responseHeaders.forEach(responseBuilder::putHeader);
        }
        return SerdeBenchmarkProtocols.deserialize(protocol, operation, attributes, responseBuilder.build());
    }
}
