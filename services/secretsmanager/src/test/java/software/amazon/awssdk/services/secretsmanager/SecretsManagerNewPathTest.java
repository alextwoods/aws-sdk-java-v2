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

package software.amazon.awssdk.services.secretsmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson11Protocol;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerOperations;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * The generated Secrets Manager clients, built from the native Smithy model, run on the schema-based pipeline path with
 * the awsJson1_1 client protocol (task 9.3). The {@code @idempotencyToken} member {@code ClientRequestToken} is filled
 * from {@link IdempotentUtils#getGenerator()} when it is unset, as the legacy marshallers did, and is sent as given
 * otherwise.
 */
class SecretsManagerNewPathTest {
    private static final String SYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.SyncApiCallPipeline";
    private static final String ASYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.AsyncApiCallPipeline";
    private static final String LEGACY_PIPELINE = "software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline";
    private static final String GENERATED_TOKEN = "00000000-0000-4000-8000-000000000000";

    private Supplier<String> previousGenerator;

    @BeforeEach
    void fixTheIdempotencyToken() {
        previousGenerator = IdempotentUtils.getGenerator();
        IdempotentUtils.setGenerator(() -> GENERATED_TOKEN);
    }

    @AfterEach
    void restoreTheIdempotencyTokenGenerator() {
        IdempotentUtils.setGenerator(previousGenerator);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createSecret_withoutToken_runsOnTheNewPipelineAndFillsTheToken(boolean async) {
        Recorder recorder = new Recorder();

        CreateSecretResponse response = call(async, recorder, r -> r.name("s").secretString("v"), response(
            200, "{\"ARN\":\"arn:aws:secretsmanager:us-west-2:123456789012:secret:s\",\"Name\":\"s\","
                 + "\"VersionId\":\"v1\"}"));

        assertThat(response.name()).isEqualTo("s");
        assertThat(response.versionId()).isEqualTo("v1");
        recorder.assertRanOnTheNewPipeline(async, SecretsManagerOperations.CREATE_SECRET);
        SdkHttpRequest sent = recorder.sent.get(0);
        assertThat(sent.firstMatchingHeader("X-Amz-Target")).hasValue("secretsmanager.CreateSecret");
        assertThat(sent.firstMatchingHeader("Content-Type")).hasValue("application/x-amz-json-1.1");
        Map<String, JsonNode> body = recorder.body();
        assertThat(body.get("Name").asString()).isEqualTo("s");
        assertThat(body.get("SecretString").asString()).isEqualTo("v");
        assertThat(body.get("ClientRequestToken").asString()).isEqualTo(GENERATED_TOKEN);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createSecret_withToken_sendsTheGivenToken(boolean async) {
        Recorder recorder = new Recorder();

        call(async, recorder, r -> r.name("s").clientRequestToken("my-token"), response(200, "{}"));

        assertThat(recorder.body().get("ClientRequestToken").asString()).isEqualTo("my-token");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createSecret_withModeledError_throwsTheModeledException(boolean async) {
        Recorder recorder = new Recorder();
        Throwable failure = catchThrowable(() -> call(async, recorder, r -> r.name("s"), response(
            400, "{\"__type\":\"ResourceNotFoundException\",\"Message\":\"no secret\"}")));
        if (failure instanceof CompletionException) {
            failure = failure.getCause();
        }

        assertThat(failure).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("no secret");
        ResourceNotFoundException error = (ResourceNotFoundException) failure;
        assertThat(error.awsErrorDetails().errorCode()).isEqualTo("ResourceNotFoundException");
        assertThat(error.statusCode()).isEqualTo(400);
        recorder.assertRanOnTheNewPipeline(async, SecretsManagerOperations.CREATE_SECRET);
    }

    private static CreateSecretResponse call(boolean async, Recorder recorder,
                                             Consumer<CreateSecretRequest.Builder> request,
                                             HttpExecuteResponse response) {
        StaticCredentialsProvider credentials =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("ak", "sk"));
        if (async) {
            MockAsyncHttpClient http = new MockAsyncHttpClient();
            http.stubResponses(response);
            try (SecretsManagerAsyncClient client =
                     SecretsManagerAsyncClient.builder()
                                              .region(Region.US_WEST_2)
                                              .credentialsProvider(credentials)
                                              .httpClient(http)
                                              .overrideConfiguration(o -> o.addExecutionInterceptor(recorder)
                                                                           .retryStrategy(r -> r.maxAttempts(1)))
                                              .build()) {
                try {
                    return client.createSecret(request).join();
                } finally {
                    http.getStreamingPayload().ifPresent(recorder::asyncBody);
                }
            }
        }
        MockSyncHttpClient http = new MockSyncHttpClient();
        http.stubResponses(response);
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                                                               .region(Region.US_WEST_2)
                                                               .credentialsProvider(credentials)
                                                               .httpClient(http)
                                                               .overrideConfiguration(
                                                                   o -> o.addExecutionInterceptor(recorder)
                                                                         .retryStrategy(r -> r.maxAttempts(1)))
                                                               .build()) {
            return client.createSecret(request);
        }
    }

    private static HttpExecuteResponse response(int status, String body) {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(status).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(body)))
                                  .build();
    }

    /**
     * Records the sent request and its body, and checks at {@code beforeTransmission} which pipeline is on the calling
     * thread's stack and the call's operation and protocol.
     */
    private static final class Recorder implements ExecutionInterceptor {
        private final List<SdkHttpRequest> sent = new CopyOnWriteArrayList<>();
        private final List<String> stacks = new CopyOnWriteArrayList<>();
        private volatile byte[] body;
        private volatile ApiOperation<?, ?> operation;
        private volatile Object protocol;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes attributes) {
            sent.add(context.httpRequest());
            if (context.requestBody().isPresent()) {
                body = FunctionalUtils.invokeSafely(
                    () -> IoUtils.toByteArray(context.requestBody().get().contentStreamProvider().newStream()));
            }
            StringBuilder stack = new StringBuilder();
            for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
                stack.append(frame.getClassName()).append('\n');
            }
            stacks.add(stack.toString());
            operation = attributes.getAttribute(SdkExecutionAttribute.API_OPERATION);
            protocol = attributes.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL);
        }

        void asyncBody(byte[] bytes) {
            if (body == null) {
                body = bytes;
            }
        }

        Map<String, JsonNode> body() {
            assertThat(body).as("request body").isNotNull();
            return JsonNodeParser.create().parse(new String(body, StandardCharsets.UTF_8)).asObject();
        }

        void assertRanOnTheNewPipeline(boolean async, ApiOperation<?, ?> expected) {
            assertThat(stacks).hasSize(1);
            assertThat(stacks.get(0)).contains(async ? ASYNC_PIPELINE : SYNC_PIPELINE).doesNotContain(LEGACY_PIPELINE);
            assertThat(operation).isSameAs(expected);
            assertThat(protocol).isInstanceOf(AwsJson11Protocol.class);
        }
    }
}
