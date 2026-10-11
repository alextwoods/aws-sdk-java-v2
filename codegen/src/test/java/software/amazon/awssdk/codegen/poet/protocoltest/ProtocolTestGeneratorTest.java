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

package software.amazon.awssdk.codegen.poet.protocoltest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.pipeline.CodegenPipeline;
import software.amazon.awssdk.codegen.pipeline.ServiceInputs;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Generates protocol tests for a small Smithy model ({@code demo.smithy}) and checks case selection, class layout and
 * splitting, skips, failures, and the rendered value literals. {@link GeneratedProtocolTestsCompileTest} compiles and
 * runs the output.
 */
class ProtocolTestGeneratorTest {
    static final String SKIP_EVENT_STREAM =
        "[{\"id\": \"WatchThingsEventStream\", \"reason\": \"Event streams have no sync client method\"}]";
    static final String DEMO = "demo.smithy";
    static final String STREAMING = "streaming.smithy";
    private static final String PACKAGE = "software.amazon.awssdk.services.protodemo.protocoltests";

    @TempDir
    Path dir;

    @Test
    void selectsClientCasesOfSupportedProtocols_inOperationThenErrorOrder() throws IOException {
        ProtocolTestGenerator generator = generator(SKIP_EVENT_STREAM, ProtocolTestGenerator.DEFAULT_MAX_TESTS_PER_CLASS);

        assertThat(generator.caseIds()).containsExactly(
            "GetThingOutput", "GetThingEmptyOutput", "PutThingAllTypes", "PutThingRestJson", "WatchThingsEventStream",
            "ServiceFaultError", "ThingNotFoundError");
    }

    @Test
    void groupsByProtocolAndKind_withOneTagPerClassAndTheIdAsDisplayName() throws IOException {
        List<ClassSpec> specs = generator(SKIP_EVENT_STREAM, ProtocolTestGenerator.DEFAULT_MAX_TESTS_PER_CLASS)
            .classSpecs();

        assertThat(specs).extracting(s -> s.className().toString()).containsExactly(
            PACKAGE + ".ProtoDemoAwsJson10RequestTests1",
            PACKAGE + ".ProtoDemoAwsJson10ResponseTests1",
            PACKAGE + ".ProtoDemoRestJson1RequestTests1");
        assertThat(source(specs.get(0))).contains("@Tag(\"protocol:aws.protocols#awsJson1_0\")");
        assertThat(source(specs.get(2))).contains("@Tag(\"protocol:aws.protocols#restJson1\")");
        assertThat(testMethods(specs.get(0))).containsExactly("request_PutThingAllTypes",
                                                              "request_WatchThingsEventStream");
        assertThat(testMethods(specs.get(1))).containsExactly("response_GetThingOutput", "response_GetThingEmptyOutput",
                                                              "error_ServiceFaultError", "error_ThingNotFoundError");
        for (ClassSpec spec : specs) {
            for (MethodSpec method : spec.poetSpec().methodSpecs) {
                if (method.name.contains("_")) {
                    assertThat(method.annotations.toString()).contains("@org.junit.jupiter.api.Test")
                                                             .contains("@org.junit.jupiter.api.DisplayName(\""
                                                                       + method.name.substring(method.name.indexOf('_') + 1)
                                                                       + "\")");
                }
            }
        }
    }

    @Test
    void splitsClassesAtTheConfiguredSize() throws IOException {
        List<ClassSpec> specs = generator(SKIP_EVENT_STREAM, 2).classSpecs();

        assertThat(specs).extracting(s -> s.className().simpleName()).containsExactly(
            "ProtoDemoAwsJson10RequestTests1", "ProtoDemoAwsJson10ResponseTests1", "ProtoDemoAwsJson10ResponseTests2",
            "ProtoDemoRestJson1RequestTests1");
        assertThat(specs.stream().mapToInt(s -> testMethods(s).size()).boxed().collect(Collectors.toList()))
            .containsExactly(2, 2, 2, 1);
        assertThat(testMethods(specs.get(2))).containsExactly("error_ServiceFaultError", "error_ThingNotFoundError");
    }

    @Test
    void skippedCases_areGeneratedDisabledWithTheReason() throws IOException {
        String skips = "[{\"id\": \"WatchThingsEventStream\", \"reason\": \"no sync method\"},"
                       + " {\"id\": \"GetThingEmptyOutput\", \"reason\": \"flaky\", \"protocol\": \"aws.protocols#awsJson1_0\"}]";
        List<ClassSpec> specs = generator(skips, ProtocolTestGenerator.DEFAULT_MAX_TESTS_PER_CLASS).classSpecs();

        MethodSpec eventStream = method(specs.get(0), "request_WatchThingsEventStream");
        assertThat(eventStream.annotations.toString()).contains("@org.junit.jupiter.api.Disabled(\"no sync method\")");
        assertThat(eventStream.code.toString()).contains("// Not generated: operation WatchThings uses an event stream");

        MethodSpec skippedButGenerated = method(specs.get(1), "response_GetThingEmptyOutput");
        assertThat(skippedButGenerated.annotations.toString()).contains("@org.junit.jupiter.api.Disabled(\"flaky\")");
        assertThat(skippedButGenerated.code.toString()).contains("HARNESS.respondWith(response");
        assertThat(method(specs.get(1), "response_GetThingOutput").annotations.toString()).doesNotContain("Disabled");
    }

    @Test
    void ungeneratableCaseThatIsNotSkipped_failsNamingTheId() {
        assertThatThrownBy(() -> generator("[]", ProtocolTestGenerator.DEFAULT_MAX_TESTS_PER_CLASS))
            .isInstanceOf(ProtocolTestCodegenException.class)
            .hasMessageContaining("WatchThingsEventStream (aws.protocols#awsJson1_0): operation WatchThings uses an event "
                                  + "stream")
            .hasMessageNotContaining("PutThingAllTypes");
    }

    @Test
    void skipOfAnUnknownCase_failsNamingTheEntry() {
        String skips = "[{\"id\": \"WatchThingsEventStream\", \"reason\": \"r\"}, {\"id\": \"NoSuchCase\", \"reason\": \"r\"},"
                       + " {\"id\": \"PutThingServerOnly\", \"reason\": \"server cases are not selected\"},"
                       + " {\"id\": \"GetThingOutput\", \"reason\": \"r\", \"protocol\": \"aws.protocols#restXml\"}]";
        assertThatThrownBy(() -> generator(skips, ProtocolTestGenerator.DEFAULT_MAX_TESTS_PER_CLASS))
            .isInstanceOf(ProtocolTestCodegenException.class)
            .hasMessageContaining("do not exist")
            .hasMessageContaining("id=NoSuchCase")
            .hasMessageContaining("id=PutThingServerOnly")
            .hasMessageContaining("id=GetThingOutput, protocol=aws.protocols#restXml")
            .hasMessageNotContaining("id=WatchThingsEventStream");
    }

    @Test
    void skipWithAnEmptyReason_failsNamingTheEntry() {
        assertThatThrownBy(() -> generator("[{\"id\": \"WatchThingsEventStream\", \"reason\": \" \"}]",
                                           ProtocolTestGenerator.DEFAULT_MAX_TESTS_PER_CLASS))
            .isInstanceOf(ProtocolTestCodegenException.class)
            .hasMessageContaining("entry 0 (id \"WatchThingsEventStream\") has an empty \"reason\"");
    }

    @Test
    void requestTest_buildsTheInputFromParamsAndAssertsEveryRequestProperty() throws IOException {
        List<ClassSpec> specs = generator(SKIP_EVENT_STREAM, ProtocolTestGenerator.DEFAULT_MAX_TESTS_PER_CLASS)
            .classSpecs();
        String code = method(specs.get(0), "request_PutThingAllTypes").code.toString();

        assertThat(code).contains(
            ".name(\"thing\")",
            ".count(-2147483648)",
            ".big(9007199254740993L)",
            ".small((short) 7)",
            ".tiny((byte) -3)",
            ".ratio(1.5F)",
            ".fraction(java.lang.Double.NaN)",
            ".infinite(java.lang.Double.NEGATIVE_INFINITY)",
            ".flag(true)",
            ".when(java.time.Instant.ofEpochSecond(1576540098L, 500000000L))",
            ".data(software.amazon.awssdk.core.SdkBytes.fromUtf8String(\"blob \\\"quoted\\\"\"))",
            ".color(\"red\")",
            ".colorsWithStrings(java.util.Arrays.<java.lang.String>asList(\"red\", \"green\"))",
            ".tags(java.util.Arrays.<java.lang.String>asList(\"a\", (java.lang.String) null, \"b\"))",
            ".empty(java.util.Collections.<java.lang.String>emptyList())",
            ".attributes(software.amazon.awssdk.protocol.smithytests.ProtocolTestValues.<java.lang.String>mapOf(\"k1\", "
            + "\"v1\", \"k2\", \"v2\"))",
            ".children(java.util.Arrays.<software.amazon.awssdk.services.protodemo.model.Node>asList(",
            ".choice(software.amazon.awssdk.services.protodemo.model.Choice.builder()",
            ".doc(software.amazon.awssdk.core.document.Document.fromMap(",
            "Document.fromNumber(\"2.5\")",
            "Document.fromNull()",
            ".precise(new java.math.BigDecimal(",
            ".level(2)",
            ".grid(java.util.Arrays.<java.util.List<java.lang.Integer>>asList(java.util.Arrays.<java.lang.Integer>asList(1, 2), "
            + "java.util.Collections.<java.lang.Integer>emptyList()))",
            ".byColorWithStrings(software.amazon.awssdk.protocol.smithytests.ProtocolTestValues.<java.util.List<java.lang.Integer>>"
            + "mapOf(\"red\", java.util.Arrays.<java.lang.Integer>asList(1)))",
            "HARNESS.captureRequest(c -> c.putThing(input), \"https://example.com\")",
            "ProtocolAsserts.method(request, \"POST\")",
            "ProtocolAsserts.uri(request, \"/\")",
            "ProtocolAsserts.headers(request, software.amazon.awssdk.protocol.smithytests.ProtocolTestValues.<java.lang.String>"
            + "mapOf(\"Content-Type\", \"application/x-amz-json-1.0\", \"X-Amz-Target\", \"ProtoDemo.PutThing\"))",
            "ProtocolAsserts.forbidHeaders(request, java.util.Arrays.asList(\"X-Forbidden\"))",
            "ProtocolAsserts.requireHeaders(request, java.util.Arrays.asList(\"Content-Length\"))",
            "ProtocolAsserts.body(request, \"{\\\"Name\\\":\\\"thing\\\"}\", \"application/json\")");
        assertThat(code).doesNotContain(".token(");

        String rest = method(specs.get(2), "request_PutThingRestJson").code.toString();
        assertThat(rest).contains("\"https://example.com/base\")",
                                  "ProtocolAsserts.queryParams(request, java.util.Arrays.asList(\"a=b\"))",
                                  "ProtocolAsserts.forbidQueryParams(request, java.util.Arrays.asList(\"c\"))",
                                  "ProtocolAsserts.requireQueryParams(request, java.util.Arrays.asList(\"a\"))",
                                  "ProtocolAsserts.resolvedHost(request, \"example.com\")",
                                  "ProtocolAsserts.body(request, \"\", null)");

        String classSource = source(specs.get(0));
        assertThat(classSource).contains("ProtocolTestHarnesses.<ProtoDemoClient, ProtoDemoClientBuilder>forClient("
                                         + "\"aws.protocols#awsJson1_0\", ProtoDemoClient::builder,",
                                         "StaticCredentialsProvider.create(AwsBasicCredentials.create(\"akid\", \"secret\"))",
                                         "o.retryStrategy(AwsRetryStrategy.doNotRetry())",
                                         "@BeforeAll",
                                         "IdempotentUtils.setGenerator(() -> \"00000000-0000-4000-8000-000000000000\")",
                                         "@AfterAll");
        assertThat(source(specs.get(1))).doesNotContain("IdempotentUtils");
    }

    @Test
    void responseAndErrorTests_buildExpectedValuesFromParams() throws IOException {
        ClassSpec responses = generator(SKIP_EVENT_STREAM, ProtocolTestGenerator.DEFAULT_MAX_TESTS_PER_CLASS)
            .classSpecs().get(1);

        String output = method(responses, "response_GetThingOutput").code.toString();
        assertThat(output).contains(
            ".label(\"x\")",
            ".values(java.util.Arrays.<java.lang.Integer>asList(1, 2))",
            ".created(java.time.Instant.ofEpochSecond(0L))",
            "StubbedResponse.fromTestCase(200, software.amazon.awssdk.protocol.smithytests.ProtocolTestValues."
            + "<java.lang.String>mapOf(\"Content-Type\", \"application/x-amz-json-1.0\"), ",
            "\"application/json\")",
            "HARNESS.respondWith(response, c -> c.getThing(software.amazon.awssdk.services.protodemo.model.GetThingRequest"
            + ".builder().build()))",
            "ProtocolAsserts.outputEquals(expected, actual)");

        String error = method(responses, "error_ThingNotFoundError").code.toString();
        assertThat(error).contains(
            "ThingNotFoundException expected = software.amazon.awssdk.services.protodemo.model.ThingNotFoundException"
            + ".builder()",
            ".message(\"missing\")",
            ".resource(\"thing\")",
            "org.junit.jupiter.api.Assertions.assertThrows(software.amazon.awssdk.services.protodemo.model."
            + "ThingNotFoundException.class, () -> HARNESS.respondWith(response, c -> c.getThing(",
            "ProtocolAsserts.outputEquals(expected, actual)");
    }

    @Test
    void streamingPayloads_arePassedAsClientMethodArguments() throws IOException {
        List<ClassSpec> specs = ProtocolTestGenerator.builder()
                                                     .model(intermediateModel(dir, STREAMING, null))
                                                     .build()
                                                     .classSpecs();
        assertThat(specs).extracting(s -> s.className().simpleName())
                         .containsExactly("StreamDemoRequestTests1", "StreamDemoResponseTests1");

        String upload = method(specs.get(0), "request_UploadThingStream").code.toString();
        assertThat(upload).contains(".label(\"l\")",
                                    "c -> c.uploadThing(input, software.amazon.awssdk.core.sync.RequestBody.fromBytes("
                                    + "software.amazon.awssdk.core.SdkBytes.fromUtf8String(\"payload\").asByteArray()))")
                          .doesNotContain(".body(software");

        String download = method(specs.get(1), "response_DownloadThingStream").code.toString();
        assertThat(download).contains("software.amazon.awssdk.core.ResponseBytes<",
                                      "c -> c.downloadThing(software.amazon.awssdk.services.streamdemo.model."
                                      + "DownloadThingRequest.builder().build(), software.amazon.awssdk.core.sync."
                                      + "ResponseTransformer.<software.amazon.awssdk.services.streamdemo.model."
                                      + "DownloadThingResponse>toBytes())",
                                      "ProtocolAsserts.outputEquals(expected, actual.response())",
                                      "SdkBytes.fromUtf8String(\"payload\"), software.amazon.awssdk.core.SdkBytes"
                                      + ".fromByteArray(actual.asByteArray()))")
                            .doesNotContain(".body(software");
    }

    @Test
    void streamingMemberTheClientMethodDoesNotExpose_isUngeneratable() throws IOException {
        // Under awsJson1_0, the default protocol of demo.smithy, the intermediate model marks an @httpPayload stream as
        // streaming but the client method takes no RequestBody; the case cannot be expressed.
        String model = resource(DEMO).replace("operations: [PutThing, GetThing, WatchThings]",
                                              "operations: [PutThing, GetThing, WatchThings, UploadThing]")
                       + resource(STREAMING).substring(resource(STREAMING).indexOf("@http(method: \"PUT\""),
                                                       resource(STREAMING).indexOf("@http(method: \"GET\""))
                                            .replace("protocol: restJson1", "protocol: awsJson1_0")
                       + "@streaming\nblob StreamingBlob\n";
        IntermediateModel intermediateModel = CodegenPipeline.create().prepare(inputs(dir, model, null).build())
                                                             .intermediateModel();
        assertThatThrownBy(() -> ProtocolTestGenerator.builder().model(intermediateModel)
                                                      .skips(ProtocolTestSkipList.parse(SKIP_EVENT_STREAM, "skips.json"))
                                                      .build())
            .isInstanceOf(ProtocolTestCodegenException.class)
            .hasMessageContaining("UploadThingStream (aws.protocols#awsJson1_0): params.Body is a streaming member");
    }

    @Test
    void protocolTestsOff_generatesNothing() throws IOException {
        IntermediateModel model = intermediateModel(dir, DEMO, null);
        assertThat(ProtocolTestGenerator.isEnabled(model)).isFalse();
    }

    @Test
    void relativeSkipListPath_resolvesAgainstTheConfigurationDirectory() {
        CustomizationConfig config = CustomizationConfig.create();
        config.setProtocolTestSkips("../protocol-test-skips.json");
        config.resolveProtocolTestSkips(dir.resolve("codegen-resources"));
        assertThat(config.getProtocolTestSkips())
            .isEqualTo(dir.resolve("protocol-test-skips.json").toAbsolutePath().normalize().toString());

        config.resolveProtocolTestSkips(dir.resolve("elsewhere"));
        assertThat(config.getProtocolTestSkips())
            .isEqualTo(dir.resolve("protocol-test-skips.json").toAbsolutePath().normalize().toString());
    }

    @Test
    void missingSkipListFile_failsNamingTheFile() throws IOException {
        CustomizationConfig config = CustomizationConfig.create();
        config.setProtocolTests(true);
        config.setProtocolTestSkips(dir.resolve("missing.json").toString());
        IntermediateModel model = intermediateModel(dir, DEMO, config);

        assertThatThrownBy(() -> ProtocolTestGenerator.create(model))
            .isInstanceOf(ProtocolTestCodegenException.class)
            .hasMessageContaining("missing.json");
    }

    @Test
    void skipListParser_matchesTheRuntimeFormat() {
        ProtocolTestSkipList skips = ProtocolTestSkipList.parse(
            "[{\"id\": \"A\", \"reason\": \"any protocol\"}, {\"id\": \"A\", \"reason\": \"json\", \"protocol\": \"p#json\"}]",
            "skips.json");
        assertThat(skips.find("A", "p#json")).map(ProtocolTestSkipList.Entry::reason).contains("json");
        assertThat(skips.find("A", "p#xml")).map(ProtocolTestSkipList.Entry::reason).contains("any protocol");
        assertThat(skips.find("B", "p#xml")).isEqualTo(Optional.empty());

        for (String invalid : Arrays.asList("{}", "[1]", "[{\"id\": \"A\"}]", "[{\"id\": \"A\", \"reason\": \"r\", \"x\": 1}]",
                                            "[{\"id\": \"A\", \"reason\": \"r\"}, {\"id\": \"A\", \"reason\": \"s\"}]",
                                            "not json")) {
            assertThatThrownBy(() -> ProtocolTestSkipList.parse(invalid, "skips.json"))
                .as(invalid)
                .isInstanceOf(ProtocolTestCodegenException.class)
                .hasMessageContaining("skips.json");
        }
    }

    @Test
    void responseCaseOfAnOperationWithUnitOutput_expectsTheSynthesizedResponse() throws IOException {
        // An operation with no output has no linked output shape in the intermediate model (as in C2J), but its
        // client method returns the synthesized empty <Op>Response.
        String model = resource(DEMO).replace("operations: [PutThing, GetThing, WatchThings]",
                                              "operations: [PutThing, GetThing, WatchThings, PingThing]")
                       + "\n@httpResponseTests([{ id: \"PingThingNoOutput\", protocol: awsJson1_0, code: 200 }])\n"
                       + "@http(method: \"POST\", uri: \"/ping\")\noperation PingThing {}\n";
        IntermediateModel intermediateModel = CodegenPipeline.create().prepare(inputs(dir, model, null).build())
                                                             .intermediateModel();
        List<ClassSpec> specs = ProtocolTestGenerator.builder().model(intermediateModel)
                                                     .skips(ProtocolTestSkipList.parse(SKIP_EVENT_STREAM, "skips.json"))
                                                     .build()
                                                     .classSpecs();
        String code = method(specs.get(1), "response_PingThingNoOutput").code.toString();
        assertThat(code).contains("PingThingResponse expected = software.amazon.awssdk.services.protodemo.model"
                                  + ".PingThingResponse.builder().build()",
                                  "c -> c.pingThing(software.amazon.awssdk.services.protodemo.model.PingThingRequest"
                                  + ".builder().build())");
    }

    @Test
    void skipEntriesForAnotherService_areIgnored() throws IOException {
        // One skip file can serve several services (test/smithy-protocol-tests); entries limited to another service
        // neither fail validation nor skip this service's cases.
        String skips = "[{\"id\": \"WatchThingsEventStream\", \"reason\": \"no sync method\","
                       + " \"service\": \"demo.protocoltests#ProtoDemo\"},"
                       + " {\"id\": \"NotACaseHere\", \"reason\": \"other service\", \"service\": \"other#Service\"},"
                       + " {\"id\": \"GetThingOutput\", \"reason\": \"other service\", \"service\": \"other#Service\"}]";
        List<ClassSpec> specs = generator(skips, 150).classSpecs();
        assertThat(method(specs.get(1), "response_GetThingOutput").annotations.toString()).doesNotContain("Disabled");
        assertThat(method(specs.get(0), "request_WatchThingsEventStream").annotations.toString())
            .contains("@org.junit.jupiter.api.Disabled(\"no sync method\")");

        ProtocolTestSkipList parsed = ProtocolTestSkipList.parse(skips, "skips.json");
        assertThat(parsed.forService("other#Service").entries()).extracting(ProtocolTestSkipList.Entry::reason)
                                                                .containsExactly("other service", "other service");
        assertThat(parsed.entries().get(0).service()).isEqualTo("demo.protocoltests#ProtoDemo");
        assertThatThrownBy(() -> ProtocolTestSkipList.parse(
            "[{\"id\": \"A\", \"reason\": \"r\", \"service\": \"s#S\"}, {\"id\": \"A\", \"reason\": \"t\", \"service\": \"s#S\"}]",
            "skips.json"))
            .isInstanceOf(ProtocolTestCodegenException.class)
            .hasMessageContaining("repeats the skip for id \"A\" and service s#S");
    }

    // ---------------------------------------------------------------------------------------------------------------

    private ProtocolTestGenerator generator(String skips, int maxTestsPerClass) throws IOException {
        return ProtocolTestGenerator.builder()
                                    .model(intermediateModel(dir, DEMO, null))
                                    .skips(ProtocolTestSkipList.parse(skips, "skips.json"))
                                    .maxTestsPerClass(maxTestsPerClass)
                                    .build();
    }

    /**
     * The intermediate model of {@code demo.smithy}, built by the Codegen_Pipeline's native Smithy route.
     */
    static IntermediateModel intermediateModel(Path dir, String resource, CustomizationConfig config) throws IOException {
        return CodegenPipeline.create().prepare(inputs(dir, resource(resource), config).build()).intermediateModel();
    }

    static String resource(String name) throws IOException {
        try (InputStream in = ProtocolTestGeneratorTest.class.getResourceAsStream(name)) {
            return IoUtils.toUtf8String(in);
        }
    }

    static ServiceInputs.Builder inputs(Path dir, String smithyModel, CustomizationConfig config) throws IOException {
        Path model = dir.resolve("model/model.smithy");
        Files.createDirectories(model.getParent());
        Files.write(model, smithyModel.getBytes(StandardCharsets.UTF_8));
        return ServiceInputs.builder()
                            .serviceName("protocoltests")
                            .smithyModelSources(Collections.singletonList(model))
                            .customizationConfig(config)
                            .sourcesDirectory(dir.resolve("out/src"))
                            .resourcesDirectory(dir.resolve("out/resources"))
                            .testsDirectory(dir.resolve("out/tests"))
                            .buildDirectory(dir.resolve("target"));
    }

    private static List<String> testMethods(ClassSpec spec) {
        return spec.poetSpec().methodSpecs.stream()
                                          .filter(m -> m.annotations.toString().contains("api.Test"))
                                          .map(m -> m.name)
                                          .collect(Collectors.toList());
    }

    private static MethodSpec method(ClassSpec spec, String name) {
        TypeSpec type = spec.poetSpec();
        return type.methodSpecs.stream().filter(m -> m.name.equals(name)).findFirst()
                               .orElseThrow(() -> new AssertionError(name + " not in " + type.name));
    }

    private static String source(ClassSpec spec) {
        return JavaFile.builder(spec.className().packageName(), spec.poetSpec()).build().toString();
    }
}
