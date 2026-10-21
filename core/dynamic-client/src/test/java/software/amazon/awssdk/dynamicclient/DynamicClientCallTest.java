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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.dynamicclient.RecordingHttpClients.Recorded;
import software.amazon.awssdk.dynamicclient.RecordingHttpClients.Stub;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Calls through {@link DynamicClient#call} and {@link DynamicClient#callAsync}, end to end through the SDK pipeline to
 * a recording HTTP client, for awsJson1_0 and restJson1 (Requirements 28.1, 28.3, 28.7, 28.9 and 28.12).
 */
@Timeout(60)
class DynamicClientCallTest {
    private final RecordingHttpClients.Sync sync = new RecordingHttpClients.Sync();
    private final RecordingHttpClients.Async async = new RecordingHttpClients.Async();
    private final RecordingInterceptor interceptor = new RecordingInterceptor();
    private DynamicClient client;

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void call_awsJson10_sendsTheCoercedInputAndReturnsTheOutputDocument() {
        client = CallTestClients.client(DynamicTestModels.json10(), sync, async, interceptor);
        sync.stub(Stub.of(200, "{\"id\":\"i-1\",\"created\":1700000000.5,\"tags\":[\"a\",\"b\"],"
                               + "\"nested\":{\"label\":\"n\"}}",
                          "x-amzn-RequestId", "request-1"));

        Document output = client.call("PutItem", map("name", "widget", "count", 3, "created", 1700000000,
                                                     "nested", map("when", "2023-11-14T22:13:20Z")));

        Recorded sent = sync.lastRequest();
        assertThat(sent.request.method()).isEqualTo(SdkHttpMethod.POST);
        assertThat(sent.request.getUri().toString()).isEqualTo("https://example.com/");
        assertThat(sent.request.firstMatchingHeader("X-Amz-Target")).hasValue("JsonTen.PutItem");
        assertThat(sent.request.firstMatchingHeader("Content-Type")).hasValue("application/x-amz-json-1.0");
        assertThat(sent.request.firstMatchingHeader("Authorization").orElse(""))
            .startsWith("AWS4-HMAC-SHA256").contains("/us-west-2/jsonten/aws4_request");
        assertThat(sent.body).isEqualTo("{\"name\":\"widget\",\"count\":3,\"created\":1700000000,"
                                        + "\"nested\":{\"when\":1700000000}}");

        assertThat(output).isInstanceOf(StructDocument.class);
        assertThat(output.getDiscriminator()).isEqualTo(ShapeId.from("test.json10#PutItemOutput"));
        assertThat(output.getMemberNames()).containsExactly("id", "created", "tags", "nested");
        assertThat(output.getMember("id").asString()).isEqualTo("i-1");
        assertThat(output.getMember("created").asTimestamp()).isEqualTo(Instant.ofEpochMilli(1700000000500L));
        assertThat(output.getMember("tags").asList()).extracting(Document::asString).containsExactly("a", "b");
        assertThat(output.getMember("nested").getDiscriminator()).isEqualTo(ShapeId.from("test.json10#Nested"));
        assertThat(output.getMember("missing")).isNull();

        DynamicResponse response = (DynamicResponse) interceptor.response;
        assertThat(response.document()).isSameAs(output);
        assertThat(response.responseMetadata().requestId()).isEqualTo("request-1");
        assertThat(response.sdkHttpResponse().statusCode()).isEqualTo(200);
    }

    @Test
    void callAsync_restJson1_bindsLabelQueryHeaderAndBodyAndReturnsTheOutputDocument() {
        client = CallTestClients.client(DynamicTestModels.restJson(), sync, async);
        async.stub(Stub.of(200, "{\"display_name\":\"New name\",\"updatedAt\":1700000001}", "X-Version", "7"));

        Document output = client.callAsync("UpdateWidget",
                                           map("widgetId", "w 1", "dryRun", true, "trace", "abc",
                                               "displayName", "Widget", "updatedAt", "2023-11-14T22:13:20Z"))
                                .join();

        Recorded sent = async.lastRequest();
        assertThat(sent.request.method()).isEqualTo(SdkHttpMethod.PUT);
        assertThat(sent.request.getUri().toString()).isEqualTo("https://example.com/widgets/w%201?dryRun=true");
        assertThat(sent.request.firstMatchingHeader("X-Trace")).hasValue("abc");
        assertThat(sent.request.firstMatchingHeader("Content-Type")).hasValue("application/json");
        assertThat(sent.body).isEqualTo("{\"display_name\":\"Widget\",\"updatedAt\":1700000000}");
        assertThat(sync.requests()).isEmpty();

        assertThat(output.getDiscriminator()).isEqualTo(ShapeId.from("test.restjson#UpdateWidgetOutput"));
        assertThat(output.getMemberNames()).containsExactlyInAnyOrder("version", "displayName", "updatedAt");
        assertThat(output.getMember("version").type()).isEqualTo(ShapeType.INTEGER);
        assertThat(output.getMember("version").asInteger()).isEqualTo(7);
        assertThat(output.getMember("displayName").asString()).isEqualTo("New name");
        assertThat(output.getMember("updatedAt").asTimestamp()).isEqualTo(Instant.ofEpochSecond(1700000001));
        assertThat(output.getMember("missing")).isNull();
    }

    @Test
    void call_restJson1_sync_bindsTheSameRequest() {
        client = CallTestClients.client(DynamicTestModels.restJson(), sync, async);
        sync.stub(Stub.of(200, "{}"));

        Document output = client.call("UpdateWidget", map("widgetId", "w1"));

        assertThat(sync.lastRequest().request.getUri().toString()).isEqualTo("https://example.com/widgets/w1");
        assertThat(output.getDiscriminator()).isEqualTo(ShapeId.from("test.restjson#UpdateWidgetOutput"));
        assertThat(output.getMemberNames()).isEmpty();
    }

    @Test
    void call_withoutInput_usesAnEmptyInput() {
        client = CallTestClients.client(DynamicTestModels.json10(), sync, async, interceptor);

        Document output = client.call("Ping");

        assertThat(sync.lastRequest().request.firstMatchingHeader("X-Amz-Target")).hasValue("JsonTen.Ping");
        assertThat(((DynamicRequest) interceptor.beforeExecution).document().getMemberNames()).isEmpty();
        assertThat(output.getDiscriminator()).isEqualTo(ShapeId.from("smithy.api#Unit"));
    }

    @Test
    void call_overrideConfiguration_appliesToThatCallOnly() {
        client = CallTestClients.client(DynamicTestModels.json10(), sync, async);
        AwsRequestOverrideConfiguration override = AwsRequestOverrideConfiguration.builder()
                                                                                  .putHeader("x-test", "one")
                                                                                  .build();

        client.call("GetThing", map("thingId", "t"), override);
        client.call("GetThing", map("thingId", "t"));

        assertThat(sync.requests().get(0).request.firstMatchingHeader("x-test")).hasValue("one");
        assertThat(sync.requests().get(1).request.firstMatchingHeader("x-test")).isEmpty();
    }

    @Test
    void call_interceptors_seeTheCoercedInputAndTheOperationInstance() {
        client = CallTestClients.client(DynamicTestModels.json10(), sync, async, interceptor);

        client.call("PutItem", map("name", "widget", "created", 10));
        client.call("PutItem", map("name", "widget"));

        assertThat(interceptor.hooks).startsWith("beforeExecution", "modifyRequest", "beforeMarshalling");
        DynamicRequest request = (DynamicRequest) interceptor.beforeExecution;
        assertThat(interceptor.modifyRequest).isSameAs(interceptor.beforeExecution);
        assertThat(request.document().getMember("created").type()).isEqualTo(ShapeType.TIMESTAMP);
        assertThat(request.document().getMember("created").asTimestamp()).isEqualTo(Instant.ofEpochSecond(10));
        assertThat(request.sdkSchema()).isSameAs(client.getOperation("PutItem").inputSchema());
        assertThat(interceptor.operations).hasSize(2)
                                          .allSatisfy(op -> assertThat(op).isSameAs(client.getOperation("PutItem")));
    }

    @Test
    void call_invalidInput_failsBeforeAnyInterceptorAndSendsNothing() {
        client = CallTestClients.client(DynamicTestModels.json10(), sync, async, interceptor);

        assertThatThrownBy(() -> client.call("PutItem", map("name", "widget", "tiny", 300)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Cannot coerce member test.json10#PutItemInput$tiny: the value is out of range for a byte member");
        assertThatThrownBy(() -> client.call("PutItem", map("nme", "widget")))
            .isInstanceOf(SerializationException.class)
            .hasMessageStartingWith("Structure test.json10#PutItemInput has no member named \"nme\".");

        assertThat(interceptor.hooks).isEmpty();
        assertThat(sync.requests()).isEmpty();
    }

    @Test
    void callAsync_invalidInput_returnsAFailedFutureWithoutSending() {
        client = CallTestClients.client(DynamicTestModels.json10(), sync, async, interceptor);

        CompletableFuture<Document> future = client.callAsync("PutItem", map("data", "aGVsbG8="));

        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(future::join).isInstanceOf(CompletionException.class)
                                        .hasCauseInstanceOf(SerializationException.class)
                                        .hasMessageContaining("test.json10#PutItemInput$data");
        assertThat(interceptor.hooks).isEmpty();
        assertThat(async.requests()).isEmpty();
    }

    @Test
    void callAsync_unknownOperationOrNullInput_returnsAFailedFuture() {
        client = CallTestClients.client(DynamicTestModels.json10(), sync, async);

        CompletableFuture<Document> unknown = client.callAsync("Nope");
        CompletableFuture<Document> nullInput = client.callAsync("Ping", null);

        assertThatThrownBy(unknown::join).hasCauseInstanceOf(IllegalArgumentException.class)
                                         .hasMessageContaining("Operation Nope not found in service test.json10#JsonTen");
        assertThatThrownBy(nullInput::join).hasCauseInstanceOf(NullPointerException.class);
        assertThat(async.requests()).isEmpty();
    }

    @Test
    void callAsync_serviceError_completesTheFutureWithTheServiceException() throws Exception {
        client = CallTestClients.client(DynamicTestModels.json10(), sync, async);
        async.stub(Stub.of(400, "{\"__type\":\"test.json10#ItemFault\",\"message\":\"bad item\"}"));

        CompletableFuture<Document> future = client.callAsync("PutItem", map("name", "widget"));

        assertThatThrownBy(() -> future.get(30, TimeUnit.SECONDS))
            .hasCauseInstanceOf(AwsServiceException.class)
            .satisfies(e -> assertThat(((AwsServiceException) e.getCause()).statusCode()).isEqualTo(400));
    }

    /**
     * Records the hooks it sees, the request of {@code beforeExecution} and {@code modifyRequest}, the operation of
     * each call and the final response.
     */
    private static final class RecordingInterceptor implements ExecutionInterceptor {
        private final List<String> hooks = Collections.synchronizedList(new ArrayList<>());
        private final List<Object> operations = Collections.synchronizedList(new ArrayList<>());
        private volatile SdkRequest beforeExecution;
        private volatile SdkRequest modifyRequest;
        private volatile SdkResponse response;

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes attributes) {
            hooks.add("beforeExecution");
            if (beforeExecution == null) {
                beforeExecution = context.request();
            }
            operations.add(attributes.getAttribute(SdkExecutionAttribute.API_OPERATION));
        }

        @Override
        public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes attributes) {
            hooks.add("modifyRequest");
            if (modifyRequest == null) {
                modifyRequest = context.request();
            }
            return context.request();
        }

        @Override
        public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes attributes) {
            hooks.add("beforeMarshalling");
        }

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes attributes) {
            hooks.add("afterExecution");
            response = context.response();
        }
    }
}
