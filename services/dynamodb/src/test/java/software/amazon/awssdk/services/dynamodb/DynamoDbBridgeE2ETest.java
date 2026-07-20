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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
import software.amazon.awssdk.services.dynamodb.operations.GetItemOperation;
import software.amazon.awssdk.services.dynamodb.operations.ListTablesOperation;
import software.amazon.awssdk.services.dynamodb.operations.PutItemOperation;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol;
import software.amazon.smithy.java.client.core.ClientTransport;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.io.uri.SmithyUri;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * End-to-end smoke test proving the full smithy-java pipeline works for DynamoDB:
 * serialize request → transport bridge → mock HTTP → deserialize response.
 *
 * <p>Uses a mock {@link SdkHttpClient} that captures the outgoing request and returns
 * canned JSON responses, proving the full pipeline (protocol serde + transport bridge)
 * works without needing a real DynamoDB endpoint.
 */
public class DynamoDbBridgeE2ETest {

    private static final ShapeId SERVICE_ID = ShapeId.from("com.amazonaws.dynamodb#DynamoDB_20120810");
    private static final SmithyUri ENDPOINT = SmithyUri.of("http://localhost:8000/");
    private static final AwsJson1Protocol PROTOCOL = new AwsJson1Protocol(SERVICE_ID);

    @Test
    void fullRoundTrip_listTables() {
        // Canned response
        String responseJson = "{\"TableNames\":[\"MyTable\",\"AnotherTable\"]}";
        CapturedRequest captured = new CapturedRequest();
        ClientTransport<HttpRequest, HttpResponse> transport =
            new V2TransportBridge(mockHttpClient(captured, 200, responseJson));

        // Serialize
        ListTablesRequest request = ListTablesRequest.builder().build();
        HttpRequest httpRequest = PROTOCOL.createRequest(
            castOp(ListTablesOperation.instance()),
            (SerializableStruct) request,
            Context.create(),
            ENDPOINT);

        // Transport
        HttpResponse httpResponse = transport.send(Context.create(), httpRequest);

        // Deserialize
        ListTablesResponse result = PROTOCOL.deserializeResponse(
            ListTablesOperation.instance(),
            Context.create(),
            ListTablesOperation.instance().errorRegistry(),
            httpRequest,
            httpResponse);

        // Assert response
        assertThat(result.tableNames()).containsExactly("MyTable", "AnotherTable");

        // Assert request was sent with correct headers
        assertThat(captured.contentType).contains("application/x-amz-json-1.0");
        assertThat(captured.target).isEqualTo("DynamoDB_20120810.ListTables");
    }

    @Test
    void fullRoundTrip_putItemAndGetItem() {
        // --- PutItem ---
        String putResponseJson = "{}";
        CapturedRequest putCapture = new CapturedRequest();
        ClientTransport<HttpRequest, HttpResponse> putTransport =
            new V2TransportBridge(mockHttpClient(putCapture, 200, putResponseJson));

        PutItemRequest putRequest = PutItemRequest.builder()
            .tableName("TestTable")
            .item(Map.of(
                "id", AttributeValue.builder().s("user-1").build(),
                "name", AttributeValue.builder().s("Ada").build()))
            .build();

        HttpRequest putHttpReq = PROTOCOL.createRequest(
            castOp(PutItemOperation.instance()),
            (SerializableStruct) putRequest,
            Context.create(), ENDPOINT);
        HttpResponse putHttpResp = putTransport.send(Context.create(), putHttpReq);

        PutItemResponse putResult = PROTOCOL.deserializeResponse(
            PutItemOperation.instance(), Context.create(),
            PutItemOperation.instance().errorRegistry(),
            putHttpReq, putHttpResp);

        assertThat(putResult).isNotNull();
        assertThat(putCapture.target).isEqualTo("DynamoDB_20120810.PutItem");
        assertThat(putCapture.body).contains("\"TableName\":\"TestTable\"");
        assertThat(putCapture.body).contains("\"id\":{\"S\":\"user-1\"}");

        // --- GetItem ---
        String getResponseJson = "{\"Item\":{\"id\":{\"S\":\"user-1\"},\"name\":{\"S\":\"Ada\"}}}";
        CapturedRequest getCapture = new CapturedRequest();
        ClientTransport<HttpRequest, HttpResponse> getTransport =
            new V2TransportBridge(mockHttpClient(getCapture, 200, getResponseJson));

        GetItemRequest getRequest = GetItemRequest.builder()
            .tableName("TestTable")
            .key(Map.of("id", AttributeValue.builder().s("user-1").build()))
            .build();

        HttpRequest getHttpReq = PROTOCOL.createRequest(
            castOp(GetItemOperation.instance()),
            (SerializableStruct) getRequest,
            Context.create(), ENDPOINT);
        HttpResponse getHttpResp = getTransport.send(Context.create(), getHttpReq);

        GetItemResponse getResult = PROTOCOL.deserializeResponse(
            GetItemOperation.instance(), Context.create(),
            GetItemOperation.instance().errorRegistry(),
            getHttpReq, getHttpResp);

        assertThat(getResult.item().get("id").s()).isEqualTo("user-1");
        assertThat(getResult.item().get("name").s()).isEqualTo("Ada");
        assertThat(getCapture.target).isEqualTo("DynamoDB_20120810.GetItem");
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private static ApiOperation<SerializableStruct, SerializableStruct> castOp(ApiOperation<?, ?> op) {
        return (ApiOperation<SerializableStruct, SerializableStruct>) op;
    }

    private static SdkHttpClient mockHttpClient(CapturedRequest capture, int statusCode, String responseBody) {
        return new SdkHttpClient() {
            @Override
            public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
                // Capture outgoing request details
                capture.target = request.httpRequest().firstMatchingHeader("X-Amz-Target").orElse(null);
                capture.contentType = request.httpRequest().firstMatchingHeader("Content-Type").orElse(null);
                if (request.contentStreamProvider().isPresent()) {
                    try {
                        capture.body = new String(
                            request.contentStreamProvider().get().newStream().readAllBytes(),
                            StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        capture.body = "<error reading body>";
                    }
                }

                // Return canned response
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
                    public HttpExecuteResponse call() {
                        return response;
                    }

                    @Override
                    public void abort() {}
                };
            }

            @Override
            public void close() {}

            @Override
            public String clientName() {
                return "MockHttpClient";
            }
        };
    }

    private static class CapturedRequest {
        String target;
        String contentType;
        String body;
    }
}
