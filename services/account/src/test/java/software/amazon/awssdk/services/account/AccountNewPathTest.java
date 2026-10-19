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

package software.amazon.awssdk.services.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.client.restjson.RestJsonClientProtocol;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.services.account.model.AccountOperations;
import software.amazon.awssdk.services.account.model.AlternateContactType;
import software.amazon.awssdk.services.account.model.GetAlternateContactResponse;
import software.amazon.awssdk.services.account.model.PutAccountNameResponse;
import software.amazon.awssdk.services.account.model.ResourceNotFoundException;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * The generated Account clients, built from the native Smithy model, run on the schema-based pipeline path with the
 * restJson1 client protocol (task 9.3). Every Account operation is {@code POST /<operation>} with a JSON body; the only
 * HTTP binding is the {@code x-amzn-ErrorType} header member {@code errorType} of the errors.
 */
class AccountNewPathTest {
    private static final String SYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.SyncApiCallPipeline";
    private static final String ASYNC_PIPELINE = "software.amazon.awssdk.core.internal.http.AsyncApiCallPipeline";
    private static final String LEGACY_PIPELINE = "software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline";
    private static final String ACCOUNT_ID = "123456789012";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getAlternateContact_runsOnTheNewPipeline(boolean async) {
        Recorder recorder = new Recorder();

        String body = "{\"AlternateContact\":{\"Name\":\"n\",\"AlternateContactType\":\"BILLING\"}}";
        AlternateContactType billing = AlternateContactType.BILLING;
        GetAlternateContactResponse response = call(
            async, recorder, null, response(200, body, null),
            c -> c.getAlternateContact(r -> r.accountId(ACCOUNT_ID).alternateContactType(billing)),
            c -> c.getAlternateContact(r -> r.accountId(ACCOUNT_ID).alternateContactType(billing)));

        assertThat(response.alternateContact().name()).isEqualTo("n");
        assertThat(response.alternateContact().alternateContactType()).isEqualTo(AlternateContactType.BILLING);
        recorder.assertRanOnTheNewPipeline(async, AccountOperations.GET_ALTERNATE_CONTACT);
        SdkHttpRequest sent = recorder.sent.get(0);
        assertThat(sent.method()).isEqualTo(SdkHttpMethod.POST);
        assertThat(sent.encodedPath()).isEqualTo("/getAlternateContact");
        assertThat(sent.firstMatchingHeader("Content-Type")).hasValue("application/json");
        // Model member order, as the legacy marshaller.
        assertThat(recorder.body()).isEqualTo("{\"AlternateContactType\":\"BILLING\",\"AccountId\":\"123456789012\"}");
    }

    /**
     * An input with body members of which none is set is sent as {@code {}}, and a Unit output reads an empty body.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void putAccountName_withEmptyResponse_andGetAccountInformation_withNoMembers_followTheEmptyPayloadRules(
        boolean async) {
        Recorder recorder = new Recorder();
        PutAccountNameResponse put = call(async, recorder, null, response(200, "", null),
                                          c -> c.putAccountName(r -> r.accountName("a")),
                                          c -> c.putAccountName(r -> r.accountName("a")));
        assertThat(put).isNotNull();
        assertThat(recorder.body()).isEqualTo("{\"AccountName\":\"a\"}");

        Recorder empty = new Recorder();
        call(async, empty, null, response(200, "{}", null),
             c -> c.getAccountInformation(r -> { }), c -> c.getAccountInformation(r -> { }));
        assertThat(empty.body()).isEqualTo("{}");
        assertThat(empty.sent.get(0).encodedPath()).isEqualTo("/getAccountInformation");
    }

    /**
     * An endpoint with a path is prefixed to the operation's URI, with no extra slash.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getAccountInformation_withEndpointPath_prefixesTheOperationPath(boolean async) {
        Recorder recorder = new Recorder();

        call(async, recorder, URI.create("https://example.com/custom"), response(200, "{}", null),
             c -> c.getAccountInformation(r -> { }), c -> c.getAccountInformation(r -> { }));

        assertThat(recorder.sent.get(0).host()).isEqualTo("example.com");
        assertThat(recorder.sent.get(0).encodedPath()).isEqualTo("/custom/getAccountInformation");
    }

    /**
     * The error code is read from {@code x-amzn-ErrorType} (without the part after {@code :}), and the full header
     * value is bound to the error's {@code errorType} member.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getAlternateContact_withModeledError_throwsTheModeledException(boolean async) {
        Recorder recorder = new Recorder();
        String errorType = "ResourceNotFoundException:http://internal.amazon.com/coral/com.amazonaws.account/";
        Throwable failure = catchThrowable(() -> call(
            async, recorder, null, response(404, "{\"message\":\"no contact\"}", errorType),
            c -> c.getAlternateContact(r -> r.alternateContactType(AlternateContactType.BILLING)),
            c -> c.getAlternateContact(r -> r.alternateContactType(AlternateContactType.BILLING))));
        if (failure instanceof CompletionException) {
            failure = failure.getCause();
        }

        assertThat(failure).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("no contact");
        ResourceNotFoundException error = (ResourceNotFoundException) failure;
        assertThat(error.awsErrorDetails().errorCode()).isEqualTo("ResourceNotFoundException");
        assertThat(error.errorType()).isEqualTo(errorType);
        assertThat(error.statusCode()).isEqualTo(404);
        recorder.assertRanOnTheNewPipeline(async, AccountOperations.GET_ALTERNATE_CONTACT);
    }

    private static <R> R call(boolean async, Recorder recorder, URI endpoint, HttpExecuteResponse response,
                              Function<AccountClient, R> sync,
                              Function<AccountAsyncClient, CompletableFuture<R>> asyncCall) {
        StaticCredentialsProvider credentials =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("ak", "sk"));
        if (async) {
            MockAsyncHttpClient http = new MockAsyncHttpClient();
            http.stubResponses(response);
            AccountAsyncClientBuilder builder =
                AccountAsyncClient.builder()
                                  .region(Region.US_EAST_1)
                                  .credentialsProvider(credentials)
                                  .httpClient(http)
                                  .overrideConfiguration(o -> o.addExecutionInterceptor(recorder)
                                                               .retryStrategy(r -> r.maxAttempts(1)));
            if (endpoint != null) {
                builder.endpointOverride(endpoint);
            }
            try (AccountAsyncClient client = builder.build()) {
                try {
                    return asyncCall.apply(client).join();
                } finally {
                    http.getStreamingPayload().ifPresent(recorder::asyncBody);
                }
            }
        }
        MockSyncHttpClient http = new MockSyncHttpClient();
        http.stubResponses(response);
        AccountClientBuilder builder =
            AccountClient.builder()
                         .region(Region.US_EAST_1)
                         .credentialsProvider(credentials)
                         .httpClient(http)
                         .overrideConfiguration(o -> o.addExecutionInterceptor(recorder)
                                                      .retryStrategy(r -> r.maxAttempts(1)));
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
        try (AccountClient client = builder.build()) {
            return sync.apply(client);
        }
    }

    private static HttpExecuteResponse response(int status, String body, String errorType) {
        SdkHttpResponse.Builder http = SdkHttpResponse.builder().statusCode(status);
        if (errorType != null) {
            http.putHeader("x-amzn-ErrorType", errorType);
        }
        return HttpExecuteResponse.builder()
                                  .response(http.build())
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

        String body() {
            assertThat(body).as("request body").isNotNull();
            return new String(body, StandardCharsets.UTF_8);
        }

        void assertRanOnTheNewPipeline(boolean async, ApiOperation<?, ?> expected) {
            assertThat(stacks).hasSize(1);
            assertThat(stacks.get(0)).contains(async ? ASYNC_PIPELINE : SYNC_PIPELINE).doesNotContain(LEGACY_PIPELINE);
            assertThat(operation).isSameAs(expected);
            assertThat(protocol).isInstanceOf(RestJsonClientProtocol.class);
        }
    }
}
