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

package software.amazon.awssdk.dynamicclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.dynamicclient.CallTestClients.map;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.dynamicclient.RecordingHttpClients.Recorded;
import software.amazon.awssdk.dynamicclient.RecordingHttpClients.Stub;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.IoUtils;

/**
 * The runtime features that read operation and member metadata, on dynamic calls through the shared pipeline: host
 * prefixes, endpoint auth scheme signer properties and metric values, idempotency tokens, request compression,
 * checksums, protocol metadata, and the client and API metadata that interceptors and the user agent see. Every
 * setting comes from the schemas of {@code pipeline-features.smithy}.
 */
class DynamicClientPipelineFeaturesTest {
    private final RecordingHttpClients.Sync sync = new RecordingHttpClients.Sync();
    private final RecordingHttpClients.Async async = new RecordingHttpClients.Async();
    private final CapturingInterceptor interceptor = new CapturingInterceptor();
    private DynamicClient client;
    private Supplier<String> originalTokenGenerator;

    @BeforeEach
    void setUp() {
        client = CallTestClients.client(DynamicTestModels.pipelineFeatures(), sync, async, interceptor);
        originalTokenGenerator = IdempotentUtils.getGenerator();
    }

    @AfterEach
    void tearDown() {
        IdempotentUtils.setGenerator(originalTokenGenerator);
        client.close();
    }

    @Test
    void call_labeledHostPrefix_prefixesTheEndpointHostWithTheLabel() {
        client.call("PutLabeled", map("tenant", "acme", "value", "v"));
        client.callAsync("PutLabeled", map("tenant", "acme", "value", "v")).join();

        assertThat(sync.lastRequest().request.host()).isEqualTo("acme-data.example.com");
        assertThat(async.lastRequest().request.host()).isEqualTo("acme-data.example.com");
        assertThat(sync.lastRequest().request.encodedPath()).isEqualTo("/labeled");
    }

    @Test
    void call_staticHostPrefix_prefixesTheEndpointHost() {
        client.call("PutStatic", map("value", "v"));

        assertThat(sync.lastRequest().request.host()).isEqualTo("static.example.com");
    }

    @Test
    void call_invalidOrMissingHostLabel_failsBeforeSending() {
        assertThatThrownBy(() -> client.call("PutLabeled", map("tenant", "not.a.label")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The provided putLabeledInput is not valid: the 'tenant' component must match the pattern "
                        + "\"[A-Za-z0-9\\-]+\".");
        assertThatThrownBy(() -> client.call("PutLabeled", map("value", "v")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The provided putLabeledInput is not valid: the required 'tenant' component is missing.");
        assertThat(client.callAsync("PutLabeled", map("tenant", "")))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .withCauseInstanceOf(IllegalArgumentException.class);
        assertThat(sync.requests()).isEmpty();
        assertThat(async.requests()).isEmpty();
        assertThat(interceptor.hooks).isEmpty();
    }

    @Test
    void call_hostPrefixInjectionDisabled_keepsTheEndpointHost() {
        try (DynamicClient disabled = DynamicClient.builder()
                                                   .model(DynamicTestModels.pipelineFeatures())
                                                   .region(Region.US_WEST_2)
                                                   .credentialsProvider(TestCredentials.PROVIDER)
                                                   .endpointOverride(CallTestClients.ENDPOINT)
                                                   .httpClient(sync)
                                                   .httpClient(async)
                                                   .overrideConfiguration(o -> o.putAdvancedOption(
                                                       SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true))
                                                   .build()) {
            disabled.call("PutLabeled", map("tenant", "acme"));
        }

        assertThat(sync.lastRequest().request.host()).isEqualTo("example.com");
    }

    @Test
    void call_endpointAuthSchemes_overrideTheSigningNameAndRegionAndAddMetricValues() {
        SigV4AuthScheme endpointScheme = SigV4AuthScheme.builder()
                                                        .signingName("othername")
                                                        .signingRegion("eu-central-1")
                                                        .build();
        Endpoint endpoint = Endpoint.builder()
                                    .url(URI.create("https://custom.example.com"))
                                    .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                  Collections.singletonList(endpointScheme))
                                    .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Collections.singletonList("ZZ"))
                                    .build();
        try (DynamicClient custom = DynamicClient.builder()
                                                 .model(DynamicTestModels.pipelineFeatures())
                                                 .region(Region.US_WEST_2)
                                                 .credentialsProvider(TestCredentials.PROVIDER)
                                                 .endpointProvider(p -> CompletableFuture.completedFuture(endpoint))
                                                 .httpClient(sync)
                                                 .httpClient(async)
                                                 .build()) {
            custom.call("PutStatic", map("value", "v"));
        }

        SdkHttpRequest request = sync.lastRequest().request;
        assertThat(request.host()).isEqualTo("static.custom.example.com");
        assertThat(request.firstMatchingHeader("Authorization").get())
            .contains("/eu-central-1/othername/aws4_request");
        assertThat(request.firstMatchingHeader("User-Agent").get())
            .matches(BusinessMetricCollection.METRIC_SEARCH_PATTERN.apply("ZZ"));
    }

    @Test
    void call_withoutEndpointAuthSchemes_signsWithTheModelSigningNameAndRegion() {
        client.call("PutStatic", map("value", "v"));

        assertThat(sync.lastRequest().request.firstMatchingHeader("Authorization").get())
            .contains("/us-west-2/features/aws4_request");
    }

    @Test
    void call_idempotencyTokenAbsent_fillsAGeneratedToken() {
        IdempotentUtils.setGenerator(() -> "generated-token");

        client.call("CreateThing", map("name", "n"));
        client.callAsync("CreateThing", map("name", "n")).join();

        assertThat(sync.lastRequest().body).isEqualTo("{\"clientToken\":\"generated-token\",\"name\":\"n\"}");
        assertThat(async.lastRequest().body).isEqualTo("{\"clientToken\":\"generated-token\",\"name\":\"n\"}");
    }

    @Test
    void call_idempotencyTokenSet_keepsTheCallersToken() {
        IdempotentUtils.setGenerator(() -> "generated-token");

        client.call("CreateThing", map("clientToken", "mine", "name", "n"));

        assertThat(sync.lastRequest().body).isEqualTo("{\"clientToken\":\"mine\",\"name\":\"n\"}");
        assertThat(interceptor.requestDocuments.get(0).getMember("clientToken").asString()).isEqualTo("mine");
    }

    @Test
    void call_requestCompressionAboveTheMinimumSize_gzipsTheBody() throws IOException {
        String data = repeat('a', 20_000);

        client.call("UploadLogs", map("data", data));
        client.callAsync("UploadLogs", map("data", data)).join();

        for (Recorded recorded : new Recorded[] {sync.lastRequest(), async.lastRequest()}) {
            assertThat(recorded.request.firstMatchingHeader("Content-Encoding")).hasValue("gzip");
            assertThat(gunzip(recorded.bytes)).isEqualTo("{\"data\":\"" + data + "\"}");
        }
    }

    @Test
    void call_requestCompressionBelowTheMinimumSize_sendsTheBodyUncompressed() {
        client.call("UploadLogs", map("data", "small"));

        assertThat(sync.lastRequest().request.firstMatchingHeader("Content-Encoding")).isEmpty();
        assertThat(sync.lastRequest().body).isEqualTo("{\"data\":\"small\"}");
    }

    @Test
    void call_operationWithoutRequestCompression_neverCompresses() {
        client.call("PutStatic", map("value", repeat('a', 20_000)));

        assertThat(sync.lastRequest().request.firstMatchingHeader("Content-Encoding")).isEmpty();
    }

    @Test
    void call_httpChecksumWithRequestAlgorithm_sendsThatChecksumOfTheBody() throws NoSuchAlgorithmException {
        client.call("PutChecksummed", map("checksumAlgorithm", "SHA256", "content", "hello"));

        Recorded recorded = sync.lastRequest();
        assertThat(recorded.request.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).hasValue("SHA256");
        assertThat(recorded.request.firstMatchingHeader("x-amz-checksum-sha256")).hasValue(sha256(recorded.bytes));
        assertThat(recorded.request.firstMatchingHeader("x-amz-checksum-crc32")).isEmpty();
    }

    @Test
    void call_httpChecksumRequiredWithoutAlgorithm_sendsTheDefaultCrc32Checksum() {
        client.call("PutChecksummed", map("content", "hello"));
        client.callAsync("PutChecksummed", map("content", "hello")).join();

        for (Recorded recorded : new Recorded[] {sync.lastRequest(), async.lastRequest()}) {
            assertThat(recorded.request.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).hasValue("CRC32");
            assertThat(recorded.request.firstMatchingHeader("x-amz-checksum-crc32")).hasValue(crc32(recorded.bytes));
        }
    }

    @Test
    void call_httpChecksumRequiredTrait_sendsAChecksumOfTheBody() {
        client.call("PutChecksumRequired", map("content", "hello"));

        Recorded recorded = sync.lastRequest();
        assertThat(recorded.request.firstMatchingHeader("x-amz-checksum-crc32")).hasValue(crc32(recorded.bytes));
    }

    @Test
    void call_operationWithoutChecksumTraits_sendsNoChecksum() {
        client.call("PutStatic", map("value", "v"));

        assertThat(sync.lastRequest().request.headers().keySet())
            .noneMatch(name -> name.toLowerCase(Locale.US).startsWith("x-amz-checksum"))
            .doesNotContain("Content-MD5");
    }

    @Test
    void call_responseChecksumValidationEnabled_validatesTheResponseBody() {
        String body = "{\"content\":\"world\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        sync.stub(Stub.of(200, body, "x-amz-checksum-crc32", crc32(bytes)));
        async.stub(Stub.of(200, body, "x-amz-checksum-crc32", crc32(bytes)));

        Document output = client.call("PutChecksummed", map("checksumMode", "ENABLED", "content", "hello"));
        Document asyncOutput = client.callAsync("PutChecksummed", map("checksumMode", "ENABLED", "content", "hello"))
                                     .join();

        assertThat(output.getMember("content").asString()).isEqualTo("world");
        assertThat(asyncOutput.getMember("content").asString()).isEqualTo("world");
        assertThat(sync.lastRequest().request.firstMatchingHeader("x-amz-checksum-mode")).hasValue("ENABLED");
        assertThat(interceptor.responseChecksumValidations).containsOnly("VALIDATED");
    }

    @Test
    void call_responseChecksumMismatch_fails() {
        String body = "{\"content\":\"world\"}";
        sync.stub(Stub.of(200, body, "x-amz-checksum-crc32", crc32("other".getBytes(StandardCharsets.UTF_8))));

        assertThatThrownBy(() -> client.call("PutChecksummed", map("checksumMode", "ENABLED", "content", "hello")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Data read has a different checksum than expected");
    }

    @Test
    void call_responseChecksumValidationNotRequested_doesNotValidate() {
        sync.stub(Stub.of(200, "{\"content\":\"world\"}", "x-amz-checksum-crc32", "AAAAAA=="));

        Document output = client.call("PutChecksummed", map("content", "hello"));

        assertThat(output.getMember("content").asString()).isEqualTo("world");
    }

    @Test
    void call_interceptors_seeTheClientTheProtocolMetadataAndTheApiMetadata() {
        client.call("PutStatic", map("value", "v"));
        client.callAsync("PutStatic", map("value", "v")).join();

        assertThat(interceptor.sdkClients).containsExactly(client, client);
        assertThat(interceptor.serviceProtocols).containsExactly("rest-json", "rest-json");
        for (Recorded recorded : new Recorded[] {sync.lastRequest(), async.lastRequest()}) {
            assertThat(recorded.request.firstMatchingHeader("User-Agent").get())
                .containsPattern(" api/Pipeline_Features#\\d+\\.\\d+\\.x\\S* ");
        }
        assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.US_WEST_2);
    }

    @Test
    void close_afterCalls_closesOnceWithoutReachingTheClientAgain() {
        client.call("PutStatic", map("value", "v"));

        client.close();
        client.close();
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }

    private static String gunzip(byte[] bytes) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return new String(IoUtils.toByteArray(in), StandardCharsets.UTF_8);
        }
    }

    private static String crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, bytes.length);
        long value = crc.getValue();
        byte[] big = {(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
        return Base64.getEncoder().encodeToString(big);
    }

    private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    /**
     * Records what interceptors see of a call.
     */
    private static final class CapturingInterceptor implements ExecutionInterceptor {
        private final List<String> hooks = Collections.synchronizedList(new ArrayList<>());
        private final List<Object> sdkClients = Collections.synchronizedList(new ArrayList<>());
        private final List<String> serviceProtocols = Collections.synchronizedList(new ArrayList<>());
        private final List<Document> requestDocuments = Collections.synchronizedList(new ArrayList<>());
        private final List<String> responseChecksumValidations = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            hooks.add("beforeExecution");
            sdkClients.add(executionAttributes.getAttribute(SdkInternalExecutionAttribute.SDK_CLIENT));
            serviceProtocols.add(executionAttributes.getAttribute(SdkInternalExecutionAttribute.PROTOCOL_METADATA)
                                                    .serviceProtocol());
            requestDocuments.add(((DynamicRequest) context.request()).document());
        }

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
            executionAttributes.getOptionalAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION)
                               .ifPresent(v -> responseChecksumValidations.add(v.name()));
        }
    }
}
