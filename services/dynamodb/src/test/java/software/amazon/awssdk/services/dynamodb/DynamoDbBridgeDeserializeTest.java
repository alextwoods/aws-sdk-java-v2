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

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.operations.GetItemOperation;
import software.amazon.awssdk.services.dynamodb.operations.ListTablesOperation;
import software.amazon.awssdk.services.dynamodb.operations.QueryOperation;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.ModifiableHttpResponse;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Verifies that the smithy-java bridge deserialization path correctly reconstructs
 * DynamoDB response POJOs and exceptions from JSON response bodies.
 */
public class DynamoDbBridgeDeserializeTest {

    private static final ShapeId SERVICE_ID = ShapeId.from("com.amazonaws.dynamodb#DynamoDB_20120810");
    private static final AwsJson1Protocol PROTOCOL = new AwsJson1Protocol(SERVICE_ID);
    private static final Context CONTEXT = Context.create();

    @Test
    void deserializeGetItemResponse_withAttributes() {
        String json = "{\"Item\":{\"id\":{\"S\":\"user-1\"},\"name\":{\"S\":\"Ada\"},\"age\":{\"N\":\"30\"}}}";

        GetItemResponse response = deserialize(json, GetItemOperation.instance());

        assertThat(response.hasItem()).isTrue();
        assertThat(response.item()).containsKey("id");
        assertThat(response.item().get("id").s()).isEqualTo("user-1");
        assertThat(response.item().get("name").s()).isEqualTo("Ada");
        assertThat(response.item().get("age").n()).isEqualTo("30");
    }

    @Test
    void deserializeGetItemResponse_withNestedMap() {
        String json = "{\"Item\":{\"id\":{\"S\":\"nested-1\"},"
            + "\"metadata\":{\"M\":{\"key1\":{\"S\":\"val1\"},\"key2\":{\"N\":\"99\"}}}}}";

        GetItemResponse response = deserialize(json, GetItemOperation.instance());

        assertThat(response.item().get("metadata").m()).containsKey("key1");
        assertThat(response.item().get("metadata").m().get("key1").s()).isEqualTo("val1");
        assertThat(response.item().get("metadata").m().get("key2").n()).isEqualTo("99");
    }

    @Test
    void deserializeGetItemResponse_withList() {
        String json = "{\"Item\":{\"id\":{\"S\":\"list-1\"},"
            + "\"tags\":{\"L\":[{\"S\":\"tag1\"},{\"S\":\"tag2\"},{\"N\":\"42\"}]}}}";

        GetItemResponse response = deserialize(json, GetItemOperation.instance());

        assertThat(response.item().get("tags").l()).hasSize(3);
        assertThat(response.item().get("tags").l().get(0).s()).isEqualTo("tag1");
        assertThat(response.item().get("tags").l().get(1).s()).isEqualTo("tag2");
        assertThat(response.item().get("tags").l().get(2).n()).isEqualTo("42");
    }

    @Test
    void deserializeListTablesResponse() {
        String json = "{\"TableNames\":[\"Table1\",\"Table2\",\"Table3\"]}";

        ListTablesResponse response = deserialize(json, ListTablesOperation.instance());

        assertThat(response.tableNames()).containsExactly("Table1", "Table2", "Table3");
    }

    @Test
    void deserializeQueryResponse_withItemsAndCount() {
        String json = "{\"Items\":[{\"id\":{\"S\":\"q1\"},\"val\":{\"N\":\"1\"}},"
            + "{\"id\":{\"S\":\"q2\"},\"val\":{\"N\":\"2\"}}],"
            + "\"Count\":2,\"ScannedCount\":5}";

        QueryResponse response = deserialize(json, QueryOperation.instance());

        assertThat(response.count()).isEqualTo(2);
        assertThat(response.scannedCount()).isEqualTo(5);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).get("id").s()).isEqualTo("q1");
        assertThat(response.items().get(1).get("val").n()).isEqualTo("2");
    }

    @Test
    void deserializeErrorResponse_resourceNotFound() {
        String errorJson = "{\"__type\":\"com.amazonaws.dynamodb.v20120810#ResourceNotFoundException\","
            + "\"message\":\"Requested resource not found: Table: NoSuchTable not found\"}";

        ApiOperation<SerializableStruct, SerializableStruct> operation =
            castOp(GetItemOperation.instance());

        assertThatThrownBy(() -> {
            ModifiableHttpResponse response = HttpResponse.create();
            response.setStatusCode(400);
            response.setBody(DataStream.ofBytes(errorJson.getBytes(StandardCharsets.UTF_8),
                                                "application/x-amz-json-1.0"));
            PROTOCOL.deserializeResponse(
                operation, CONTEXT, operation.errorRegistry(),
                HttpRequest.create(), response);
        }).isInstanceOf(Exception.class); // smithy-java wraps errors differently; verify it throws
    }

    @SuppressWarnings("unchecked")
    private <I extends SerializableStruct, O extends SerializableStruct> O deserialize(
            String json, ApiOperation<I, O> operation) {
        ModifiableHttpResponse response = HttpResponse.create();
        response.setStatusCode(200);
        response.setBody(DataStream.ofBytes(json.getBytes(StandardCharsets.UTF_8),
                                            "application/x-amz-json-1.0"));
        return PROTOCOL.deserializeResponse(
            operation, CONTEXT, operation.errorRegistry(),
            HttpRequest.create(), response);
    }

    @SuppressWarnings("unchecked")
    private static ApiOperation<SerializableStruct, SerializableStruct> castOp(ApiOperation<?, ?> op) {
        return (ApiOperation<SerializableStruct, SerializableStruct>) op;
    }
}
