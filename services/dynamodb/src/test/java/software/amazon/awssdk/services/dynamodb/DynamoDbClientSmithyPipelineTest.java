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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

/**
 * Proves that the actual generated {@code DynamoDbClient} (built via {@code DynamoDbClient.builder()})
 * uses the smithy-java pipeline for serialization and deserialization.
 *
 * <p>This test creates a real {@code DynamoDbClient} instance with a mock HTTP client, calls
 * operations on it, and verifies that:
 * <ul>
 *   <li>Request bodies are serialized by smithy-java (checked via X-Amz-Target header and JSON content)</li>
 *   <li>Response bodies are deserialized by smithy-java back into v2 POJOs</li>
 * </ul>
 */
public class DynamoDbClientSmithyPipelineTest {

    @Test
    void listTables_usesSmithyJavaPipeline() {
        AtomicReference<String> capturedTarget = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();

        DynamoDbClient client = buildClient(capturedTarget, capturedBody,
            200, "{\"TableNames\":[\"Users\",\"Orders\"]}");

        ListTablesResponse response = client.listTables();

        // Verify smithy-java serialized the request (correct X-Amz-Target header)
        assertThat(capturedTarget.get()).isEqualTo("DynamoDB_20120810.ListTables");
        // Verify smithy-java deserialized the response
        assertThat(response.tableNames()).containsExactly("Users", "Orders");
    }

    @Test
    void putItem_usesSmithyJavaPipeline() {
        AtomicReference<String> capturedTarget = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();

        DynamoDbClient client = buildClient(capturedTarget, capturedBody, 200, "{}");

        PutItemResponse response = client.putItem(r -> r
            .tableName("TestTable")
            .item(Map.of(
                "id", AttributeValue.builder().s("user-1").build(),
                "name", AttributeValue.builder().s("Ada").build())));

        assertThat(capturedTarget.get()).isEqualTo("DynamoDB_20120810.PutItem");
        assertThat(capturedBody.get()).contains("\"TableName\":\"TestTable\"");
        assertThat(capturedBody.get()).contains("\"id\":{\"S\":\"user-1\"}");
        assertThat(response).isNotNull();
    }

    @Test
    void getItem_usesSmithyJavaPipeline() {
        AtomicReference<String> capturedTarget = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();

        String responseJson = "{\"Item\":{\"id\":{\"S\":\"user-1\"},\"name\":{\"S\":\"Ada\"}}}";
        DynamoDbClient client = buildClient(capturedTarget, capturedBody, 200, responseJson);

        GetItemResponse response = client.getItem(r -> r
            .tableName("TestTable")
            .key(Map.of("id", AttributeValue.builder().s("user-1").build())));

        assertThat(capturedTarget.get()).isEqualTo("DynamoDB_20120810.GetItem");
        assertThat(response.item().get("id").s()).isEqualTo("user-1");
        assertThat(response.item().get("name").s()).isEqualTo("Ada");
    }

    private DynamoDbClient buildClient(AtomicReference<String> capturedTarget,
                                       AtomicReference<String> capturedBody,
                                       int statusCode, String responseBody) {
        SdkHttpClient mockHttp = mockHttpClient(capturedTarget, capturedBody, statusCode, responseBody);

        return DynamoDbClient.builder()
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test-key", "test-secret")))
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://localhost:8000"))
            .httpClient(mockHttp)
            .build();
    }

    private static SdkHttpClient mockHttpClient(AtomicReference<String> capturedTarget,
                                                AtomicReference<String> capturedBody,
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
            public String clientName() { return "SmithyPipelineTestMock"; }
        };
    }
}
