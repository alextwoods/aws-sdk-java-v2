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

package software.amazon.awssdk.awscodegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Requirements 7.1-7.7, 7.11, 8.1-8.3 and 17.1: the generated client sources of a small synthetic service, compared
 * against the expected V2-shaped signatures. {@code GeneratedClientTest} in {@code codegen-tests} compiles and calls
 * them.
 */
class ClientGenerationTest {
    private static final String ROOT = "software/amazon/awssdk/services/weather/";

    private static final String WEATHER = """
        $version: "2"
        namespace example.weather

        @aws.protocols#awsJson1_0
        @aws.auth#sigv4(name: "weather")
        service Weather {
            version: "2006-03-01"
            operations: [GetForecast, GetStatus, Upload, Download]
            errors: [Throttled]
        }

        operation GetForecast {
            input := { @required city: String }
            output := { summary: String }
            errors: [NoSuchCity]
        }

        @readonly
        operation GetStatus {
            output := { status: String }
        }

        operation Upload {
            input := {
                @required
                body: Body
            }
        }

        @readonly
        operation Download {
            input := { name: String }
            output := { @required body: Body }
        }

        @streaming
        blob Body

        @error("client")
        structure NoSuchCity {}

        @error("server")
        structure Throttled {}
        """;

    @TempDir
    Path temp;

    @Test
    void emitsTheClientsBuildersImplementationsAndBindings() throws IOException {
        AwsCodegenResult result = generate();
        List<Path> clients = result.generatedFiles().stream().filter(ClientSources::isClientFile).toList();
        assertEquals(List.of("DefaultWeatherAsyncClient.java", "DefaultWeatherAsyncClientBuilder.java",
                             "DefaultWeatherClient.java", "DefaultWeatherClientBuilder.java",
                             "WeatherAsyncClient.java", "WeatherAsyncClientBuilder.java", "WeatherClient.java",
                             "WeatherClientBuilder.java", "WeatherOperationBindings.java").stream()
                         .map(name -> Path.of(ROOT + name)).toList(),
                     clients);
    }

    @Test
    void theSyncClientHasTheV2Surface() throws IOException {
        String client = read(generate(), "WeatherClient.java");
        assertContains(client, "public interface WeatherClient extends AutoCloseable {\n");
        assertContains(client, "    String SERVICE_NAME = \"weather\";\n");
        assertContains(client, "    static WeatherClient create() {\n        return builder().build();\n    }\n");
        assertContains(client, "    static WeatherClientBuilder builder() {\n"
                               + "        return new DefaultWeatherClientBuilder();\n    }\n");
        assertContains(client, "    GetForecastResponse getForecast(GetForecastRequest request, "
                               + "AwsRequestOverrideConfiguration overrideConfiguration)\n"
                               + "            throws NoSuchCityException, ThrottledException, AwsServiceException, "
                               + "SdkClientException, WeatherException;\n");
        assertContains(client, "    default GetForecastResponse getForecast(Consumer<GetForecastRequest.Builder> "
                               + "request)\n");
        // GetForecast has a required member: no no-argument overload. Download has none: it has one.
        assertFalse(client.contains("getForecast()"), client);
        // Unit input: the no-argument form.
        assertContains(client, "    default GetStatusResponse getStatus()\n");
        // Streaming input: request-body overloads only.
        assertContains(client, "    Unit upload(UploadRequest request, RequestBody requestBody, "
                               + "AwsRequestOverrideConfiguration overrideConfiguration)\n");
        assertContains(client, "    default Unit upload(Consumer<UploadRequest.Builder> request, RequestBody "
                               + "requestBody)\n");
        assertFalse(client.contains("upload(UploadRequest request)"), client);
        // Streaming output: the response-transformer overloads; as in V2 the interface default fails, and the
        // generated client overrides it.
        assertContains(client, "    default <ReturnT> ReturnT download(DownloadRequest request, "
                               + "ResponseTransformer<DownloadResponse, ReturnT> responseTransformer)\n");
        assertContains(client, "throw new UnsupportedOperationException();");
        assertContains(client, "    SdkClientConfiguration serviceClientConfiguration();\n");
        assertContains(client, "    @Override\n    void close();\n");
    }

    @Test
    void theAsyncClientReturnsFuturesAndTakesAsyncBodies() throws IOException {
        String client = read(generate(), "WeatherAsyncClient.java");
        assertContains(client, "    CompletableFuture<GetForecastResponse> getForecast(GetForecastRequest request, "
                               + "AwsRequestOverrideConfiguration overrideConfiguration);\n");
        assertContains(client, "    CompletableFuture<Unit> upload(UploadRequest request, AsyncRequestBody "
                               + "requestBody, AwsRequestOverrideConfiguration overrideConfiguration);\n");
        assertContains(client, "AsyncResponseTransformer<DownloadResponse, ReturnT> responseTransformer)");
        assertContains(client, "return CompletableFuture.failedFuture(new UnsupportedOperationException(");
        assertFalse(client.contains(" throws "), client);
    }

    @Test
    void theDefaultClientHoldsFinalPlansAndCallsTheEngineDirectly() throws IOException {
        String client = read(generate(), "DefaultWeatherClient.java");
        assertContains(client, "final class DefaultWeatherClient implements WeatherClient {\n");
        assertContains(client, "    private final PipelinePlan<GetForecastRequest, GetForecastResponse> "
                               + "getForecastPlan;\n");
        assertContains(client, "            this.getForecastPlan = runtime.plan("
                               + "WeatherOperationBindings.GET_FORECAST);\n");
        assertContains(client, "        return engine.executeSync(getForecastPlan, request, overrideConfiguration, "
                               + "null);\n");
        assertContains(client, "        return engine.executeSync(uploadPlan, request, overrideConfiguration, "
                               + "requestBody);\n");
        assertContains(client, "        return engine.executeSync(getStatusPlan, Unit.getInstance(), "
                               + "overrideConfiguration, null);\n");
        // The streaming-output overload goes through the engine's transformer entry point.
        assertContains(client, "    public <ReturnT> ReturnT download(DownloadRequest request, "
                               + "ResponseTransformer<DownloadResponse, ReturnT> responseTransformer) {\n"
                               + "        return engine.executeSync(downloadPlan, request, null, null, "
                               + "responseTransformer);\n");
        String async = read(generate(), "DefaultWeatherAsyncClient.java");
        assertContains(async, "        return engine.executeAsync(getForecastPlan, request, overrideConfiguration, "
                              + "null);\n");
        assertContains(async, "    public <ReturnT> CompletableFuture<ReturnT> download(DownloadRequest request, "
                              + "AsyncResponseTransformer<DownloadResponse, ReturnT> responseTransformer) {\n"
                              + "        return engine.executeAsync(downloadPlan, request, null, null, "
                              + "responseTransformer);\n");
    }

    @Test
    void theBindingsAreStaticReferencesToTheDescriptorsAndBuilders() throws IOException {
        String bindings = read(generate(), "WeatherOperationBindings.java");
        assertContains(bindings, "    static final AwsOperation<GetForecastRequest, GetForecastResponse> GET_FORECAST "
                                 + "= AwsOperation.<GetForecastRequest, GetForecastResponse>builder(\n"
                                 + "            WeatherOperations.GET_FORECAST, GetForecastRequest::builder, "
                                 + "GetForecastResponse::builder)\n");
        assertContains(bindings, "                .addServiceScheme(\"aws.auth#sigv4\", Map.of(\"name\", "
                                 + "\"weather\"))\n");
        String builder = read(generate(), "DefaultWeatherClientBuilder.java");
        assertContains(builder, "        return new DefaultWeatherClient(state.buildSync(definition()));\n");
        assertContains(builder, "WeatherServiceMetadata.serviceProtocols(), WeatherServiceMetadata.typeRegistry(), "
                                + "null)\n");
        // A SigV4 service: without a credentials provider, the default chain over the client's sources.
        assertContains(builder, "            .withDefaultCredentials(sources -> DefaultCredentialsProvider.builder()\n"
                                + "                .settings(new SystemSettings(sources.jvmProperties(), "
                                + "sources.environment()))\n"
                                + "                .build());\n");
    }

    /**
     * An operation with {@code aws.protocols#httpChecksum} binds a static {@code HttpChecksumMetadata} of the trait
     * value, as the dynamic client reads it; operations without the trait bind none.
     */
    @Test
    void theBindingOfAnHttpChecksumOperationCarriesTheTraitValue() throws IOException {
        String checksum = WEATHER.replace("operation GetForecast {",
                                          "@aws.protocols#httpChecksum(requestChecksumRequired: true)\n"
                                          + "operation GetForecast {");
        String bindings = read(generate(checksum), "WeatherOperationBindings.java");
        assertContains(bindings, "                .build())\n"
                                 + "            .httpChecksum(HttpChecksumMetadata.builder()\n"
                                 + "                .requestChecksumRequired(true)\n"
                                 + "                .build())\n"
                                 + "            .build();\n");
        assertContains(bindings, "import software.amazon.awssdk.checksums.HttpChecksumMetadata;\n");
        assertFalse(read(generate(), "WeatherOperationBindings.java").contains("httpChecksum"));
    }

    private static void assertContains(String source, String expected) {
        assertTrue(source.contains(expected), () -> "Expected\n" + expected + "\nin\n" + source);
    }

    /**
     * Only services whose modeled auth signs with AWS credentials default to the credentials chain: a bearer-token
     * service gets the AWS auth schemes but no default credentials.
     */
    @Test
    void aServiceWithoutAwsCredentialAuthHasNoDefaultCredentials() throws IOException {
        String bearer = WEATHER.replace("@aws.auth#sigv4(name: \"weather\")\n", "@httpBearerAuth\n");
        String builder = read(generate(bearer), "DefaultWeatherClientBuilder.java");
        assertContains(builder, "WeatherServiceMetadata.serviceProtocols(), WeatherServiceMetadata.typeRegistry(), "
                                + "null);\n");
        assertFalse(builder.contains("DefaultCredentialsProvider"), builder);
    }

    private AwsCodegenResult generate() throws IOException {
        return generate(WEATHER);
    }

    private AwsCodegenResult generate(String model) throws IOException {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("weather.smithy"), model, StandardCharsets.UTF_8);
        return AwsCodegen.generate(new AwsCodegenRequest("example.weather#Weather",
                                                         "software.amazon.awssdk.services.weather", models,
                                                         temp.resolve("generated-" + System.nanoTime())), List.of());
    }

    private static String read(AwsCodegenResult result, String name) throws IOException {
        return Files.readString(result.sourceRoot().resolve(ROOT + name));
    }
}
