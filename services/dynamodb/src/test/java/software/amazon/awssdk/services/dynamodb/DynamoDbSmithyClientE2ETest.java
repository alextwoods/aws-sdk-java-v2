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

package software.amazon.awssdk.services.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.bridge.smithyjava.transport.V2TransportBridge;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.operations.GetItemOperation;
import software.amazon.awssdk.services.dynamodb.operations.ListTablesOperation;
import software.amazon.awssdk.services.dynamodb.operations.PutItemOperation;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol;
import software.amazon.smithy.java.client.core.ClientTransport;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.io.uri.SmithyUri;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * End-to-end test demonstrating that the v2 DynamoDB POJOs can be used through an entirely
 * smithy-java-native client pipeline for both serialization and deserialization.
 *
 * <p>This is what the generated {@code DefaultDynamoDbClient} would look like once it's switched
 * to smithy-java: requests are serialized via {@code SerializableStruct.serializeMembers()},
 * responses are deserialized via {@code ShapeBuilder.deserialize()}, and the transport is a
 * {@code V2TransportBridge} wrapping the v2 HTTP client.
 *
 * <p>The mock HTTP backend captures the outgoing request (proving smithy-java serialized it)
 * and returns canned responses (proving smithy-java deserializes them back into v2 POJOs).
 */
public class DynamoDbSmithyClientE2ETest {

    private static final ShapeId SERVICE_ID = ShapeId.from("com.amazonaws.dynamodb#DynamoDB_20120810");
    private static final SmithyUri ENDPOINT = SmithyUri.of("http://localhost:8000/");

    /**
     * Simulates what a smithy-java-powered DynamoDbClient.putItem() call would do.
     * The entire serde path is smithy-java — no v2 marshaller/unmarshaller involved.
     */
    @Test
    void putItem_serializesAndDeserializesViaSmithyJava() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        AtomicReference<String> capturedTarget = new AtomicReference<>();

        SdkHttpClient mockHttp = mockClient(capturedBody, capturedTarget, 200, "{}");

        PutItemRequest request = PutItemRequest.builder()
            .tableName("Users")
            .item(Map.of(
                "id", AttributeValue.builder().s("user-42").build(),
                "name", AttributeValue.builder().s("Grace Hopper").build(),
                "score", AttributeValue.builder().n("100").build()))
            .build();

        PutItemResponse response = callSmithyJava(request, PutItemOperation.instance(), mockHttp);

        // Verify the request was serialized by smithy-java
        assertThat(capturedTarget.get()).isEqualTo("DynamoDB_20120810.PutItem");
        assertThat(capturedBody.get()).contains("\"TableName\":\"Users\"");
        assertThat(capturedBody.get()).contains("\"id\":{\"S\":\"user-42\"}");
        assertThat(capturedBody.get()).contains("\"name\":{\"S\":\"Grace Hopper\"}");
        assertThat(capturedBody.get()).contains("\"score\":{\"N\":\"100\"}");

        // Verify the response was deserialized by smithy-java
        assertThat(response).isNotNull();
    }

    /**
     * Full round-trip: PutItem then GetItem, both through smithy-java serde.
     */
    @Test
    void getItem_deserializesResponseViaSmithyJava() {
        String responseJson = "{\"Item\":{"
            + "\"id\":{\"S\":\"user-42\"},"
            + "\"name\":{\"S\":\"Grace Hopper\"},"
            + "\"tags\":{\"L\":[{\"S\":\"pioneer\"},{\"S\":\"navy\"}]},"
            + "\"metadata\":{\"M\":{\"created\":{\"S\":\"1906-12-09\"}}}}}";

        SdkHttpClient mockHttp = mockClient(new AtomicReference<>(), new AtomicReference<>(),
                                            200, responseJson);

        GetItemRequest request = GetItemRequest.builder()
            .tableName("Users")
            .key(Map.of("id", AttributeValue.builder().s("user-42").build()))
            .build();

        GetItemResponse response = callSmithyJava(request, GetItemOperation.instance(), mockHttp);

        // Verify deserialization through smithy-java produced correct v2 POJOs
        assertThat(response.hasItem()).isTrue();
        assertThat(response.item().get("id").s()).isEqualTo("user-42");
        assertThat(response.item().get("name").s()).isEqualTo("Grace Hopper");
        assertThat(response.item().get("tags").l()).hasSize(2);
        assertThat(response.item().get("tags").l().get(0).s()).isEqualTo("pioneer");
        assertThat(response.item().get("metadata").m().get("created").s()).isEqualTo("1906-12-09");
    }

    /**
     * ListTables through smithy-java — proves simple string-list responses work.
     */
    @Test
    void listTables_fullSmithyJavaPipeline() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        AtomicReference<String> capturedTarget = new AtomicReference<>();

        SdkHttpClient mockHttp = mockClient(capturedBody, capturedTarget, 200,
            "{\"TableNames\":[\"Users\",\"Orders\",\"Products\"]}");

        ListTablesRequest request = ListTablesRequest.builder().build();
        ListTablesResponse response = callSmithyJava(request, ListTablesOperation.instance(), mockHttp);

        assertThat(capturedTarget.get()).isEqualTo("DynamoDB_20120810.ListTables");
        assertThat(capturedBody.get()).isEqualTo("{}");
        assertThat(response.tableNames()).containsExactly("Users", "Orders", "Products");
    }

    /**
     * Error deserialization: smithy-java reads the error JSON and the protocol raises an exception.
     */
    @Test
    void errorResponse_deserializedViaSmithyJava() {
        String errorJson = "{\"__type\":\"com.amazonaws.dynamodb.v20120810#ResourceNotFoundException\","
            + "\"message\":\"Requested resource not found: Table: NoSuchTable not found\"}";

        SdkHttpClient mockHttp = mockClient(new AtomicReference<>(), new AtomicReference<>(),
                                            400, errorJson);

        GetItemRequest request = GetItemRequest.builder()
            .tableName("NoSuchTable")
            .key(Map.of("id", AttributeValue.builder().s("x").build()))
            .build();

        assertThatThrownBy(() -> callSmithyJava(request, GetItemOperation.instance(), mockHttp))
            .isInstanceOf(CallException.class);
    }

    // ---- The smithy-java client pipeline (what a future DefaultDynamoDbClient would do) ----

    /**
     * This method represents the core of what a smithy-java-powered DynamoDbClient method does:
     * 1. Serialize the request POJO via smithy-java's AwsJson1Protocol (using SerializableStruct)
     * 2. Send through V2TransportBridge (wrapping the v2 SdkHttpClient)
     * 3. Deserialize the response via smithy-java's protocol (using ShapeBuilder)
     */
    @SuppressWarnings("unchecked")
    private <I extends SerializableStruct, O extends SerializableStruct> O callSmithyJava(
            Object request, ApiOperation<I, O> operation, SdkHttpClient httpClient) {

        AwsJson1Protocol protocol = new AwsJson1Protocol(SERVICE_ID);
        ClientTransport<HttpRequest, HttpResponse> transport = new V2TransportBridge(httpClient);
        Context context = Context.create();

        // Step 1: Serialize request via smithy-java (the POJO IS a SerializableStruct)
        HttpRequest httpRequest = protocol.createRequest(
            (ApiOperation<SerializableStruct, SerializableStruct>) operation,
            (SerializableStruct) request,
            context,
            ENDPOINT);

        // Step 2: Send through the v2 transport bridge
        HttpResponse httpResponse = transport.send(context, httpRequest);

        // Step 3: Deserialize response via smithy-java (the builder IS a ShapeBuilder)
        return protocol.deserializeResponse(
            operation, context, operation.errorRegistry(),
            httpRequest, httpResponse);
    }

    // ---- Mock HTTP client ----

    private static SdkHttpClient mockClient(AtomicReference<String> capturedBody,
                                            AtomicReference<String> capturedTarget,
                                            int statusCode, String responseBody) {
        return new SdkHttpClient() {
            @Override
            public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
                capturedTarget.set(request.httpRequest().firstMatchingHeader("X-Amz-Target").orElse(null));
                if (request.contentStreamProvider().isPresent()) {
                    try {
                        capturedBody.set(new String(
                            request.contentStreamProvider().get().newStream().readAllBytes(),
                            StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        capturedBody.set("<error>");
                    }
                }

                byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                HttpExecuteResponse response = HttpExecuteResponse.builder()
                    .response(SdkHttpResponse.builder()
                        .statusCode(statusCode)
                        .putHeader("Content-Type", "application/x-amz-json-1.0")
                        .putHeader("Content-Length", String.valueOf(responseBytes.length))
                        .build())
                    .responseBody(AbortableInputStream.create(new ByteArrayInputStream(responseBytes)))
                    .build();

                return new ExecutableHttpRequest() {
                    @Override
                    public HttpExecuteResponse call() { return response; }

                    @Override
                    public void abort() {}
                };
            }

            @Override
            public void close() {}

            @Override
            public String clientName() { return "SmithyJavaE2EMock"; }
        };
    }
}
