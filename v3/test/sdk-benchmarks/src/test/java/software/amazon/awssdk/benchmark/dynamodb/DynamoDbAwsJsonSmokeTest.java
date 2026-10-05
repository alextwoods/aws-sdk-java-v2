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

package software.amazon.awssdk.benchmark.dynamodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.awsjson.AwsJson10ProtocolProvider;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolSelector;
import software.amazon.awssdk.services.dynamodb.DynamoDbServiceMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Round-trips the generated V3 DynamoDB types through the V3 awsJson1.0 protocol runtime, using only generated
 * schemas, operation descriptors, service metadata and type registry (no client, signer or endpoint runtime).
 */
class DynamoDbAwsJsonSmokeTest {
    private static final URI ENDPOINT = URI.create("https://dynamodb.us-east-1.amazonaws.com");
    private static final ClientProtocol PROTOCOL =
        ProtocolSelector.select(DynamoDbServiceMetadata.serviceProtocols(), new AwsJson10ProtocolProvider())
                        .createClientProtocol(DynamoDbServiceMetadata.typeRegistry(), true);

    private static OperationProtocol operation(String name) {
        AwsOperationDescriptor descriptor =
            DynamoDbServiceMetadata.operation(ShapeId.from("com.amazonaws.dynamodb#" + name)).orElseThrow();
        return PROTOCOL.operation(descriptor.schemas().orElseThrow());
    }

    private static SdkHttpRequest serialize(String operation, SerializableStruct input) {
        SdkHttpRequest request = operation(operation).serializeRequest(input, ENDPOINT);
        assertEquals("POST", request.method());
        assertEquals("dynamodb.us-east-1.amazonaws.com", request.host());
        assertEquals("/", request.encodedPath());
        assertEquals(Optional.of("application/x-amz-json-1.0"), request.headers().firstValue("Content-Type"));
        assertEquals(Optional.of("DynamoDB_20120810." + operation), request.headers().firstValue("X-Amz-Target"));
        return request;
    }

    private static SdkHttpResponse ok(String body) {
        return SdkHttpResponse.builder().statusCode(200)
                              .addHeader("Content-Type", "application/x-amz-json-1.0")
                              .body(HttpBody.fromUtf8(body))
                              .build();
    }

    private static void assertJson(String expected, SdkHttpRequest request) {
        assertEquals(Node.parse(expected), Node.parse(request.body().asUtf8String()));
    }

    private static Map<String, AttributeValue> key(String pk, String sk) {
        Map<String, AttributeValue> key = new LinkedHashMap<>();
        key.put("pk", AttributeValue.fromS(pk));
        key.put("sk", sk.startsWith("profile") ? AttributeValue.fromS(sk) : AttributeValue.fromN(sk));
        return key;
    }

    @Test
    void smallItemCoversEveryAttributeValueKind() {
        Set<AttributeValue.Type> kinds = EnumSet.noneOf(AttributeValue.Type.class);
        collectKinds(DynamoDbItems.smallItemAllKinds().values(), kinds);
        Set<AttributeValue.Type> all = EnumSet.allOf(AttributeValue.Type.class);
        all.remove(AttributeValue.Type.UNKNOWN_TO_SDK_VERSION);
        assertEquals(all, kinds);
    }

    private static void collectKinds(Iterable<AttributeValue> values, Set<AttributeValue.Type> kinds) {
        for (AttributeValue value : values) {
            kinds.add(value.type());
            if (value.type() == AttributeValue.Type.M) {
                collectKinds(value.m().values(), kinds);
            } else if (value.type() == AttributeValue.Type.L) {
                collectKinds(value.l(), kinds);
            }
        }
    }

    @Test
    void serializesGetItemRequest() {
        GetItemRequest request = GetItemRequest.builder()
                                               .tableName(DynamoDbItems.TABLE_NAME)
                                               .key(key(DynamoDbItems.SMALL_KEY, "100"))
                                               .consistentRead(true)
                                               .build();
        assertJson("{\"TableName\":\"benchmark-table\",\"Key\":{\"pk\":{\"S\":\"benchmark-key\"},\"sk\":{\"N\":\"100\"}},"
                   + "\"ConsistentRead\":true}", serialize("GetItem", request));
    }

    @Test
    void serializesPutItemRequestWithEveryAttributeKind() {
        PutItemRequest request = PutItemRequest.builder()
                                               .tableName(DynamoDbItems.TABLE_NAME)
                                               .item(DynamoDbItems.smallItemAllKinds())
                                               .build();
        assertJson("{\"TableName\":\"benchmark-table\",\"Item\":" + DynamoDbItems.json(DynamoDbItems.smallItemAllKinds())
                   + "}",
                   serialize("PutItem", request));
    }

    @Test
    void deserializesCannedGetItemResponse() {
        OperationProtocol getItem = operation("GetItem");
        SdkHttpResponse response = ok(DynamoDbItems.getItemResponseJson());
        assertFalse(getItem.isError(response));
        GetItemResponse output = getItem.deserializeResponse(response, GetItemResponse.builder());

        Map<String, AttributeValue> expected = DynamoDbItems.smallItem();
        assertEquals(expected.keySet(), output.item().keySet());
        for (Map.Entry<String, AttributeValue> e : expected.entrySet()) {
            assertEquals(e.getValue(), output.item().get(e.getKey()), e.getKey());
        }
        assertEquals(expected, output.item());

        AttributeValue nullField = output.item().get("nullField");
        assertEquals(AttributeValue.Type.NULL, nullField.type());
        assertEquals(Boolean.TRUE, nullField.nullValue());
        AttributeValue listNull = output.item().get("listField").l().get(3);
        assertEquals(AttributeValue.Type.NULL, listNull.type());
        assertEquals("999", output.item().get("mapField").m().get("deepNested").m().get("level2").n());
        assertEquals("hello world", output.item().get("binaryField").b().asUtf8String());
    }

    @Test
    void deserializesEmptyPutItemResponse() {
        PutItemResponse output = operation("PutItem").deserializeResponse(ok("{}"), PutItemResponse.builder());
        assertFalse(output.hasAttributes());
    }

    @Test
    void batchGetItemWith25Items() {
        List<Map<String, AttributeValue>> keys = new ArrayList<>();
        for (int i = 0; i < DynamoDbItems.BATCH_SIZE; i++) {
            keys.add(key(DynamoDbItems.batchKey(i), "profile#v2"));
        }
        BatchGetItemRequest request =
            BatchGetItemRequest.builder()
                               .requestItems(Map.of(DynamoDbItems.TABLE_NAME, KeysAndAttributes.builder().keys(keys).build()))
                               .build();
        StringBuilder expected = new StringBuilder("{\"RequestItems\":{\"benchmark-table\":{\"Keys\":[");
        for (int i = 0; i < keys.size(); i++) {
            expected.append(i > 0 ? "," : "").append(DynamoDbItems.json(keys.get(i)));
        }
        assertJson(expected.append("]}}}").toString(), serialize("BatchGetItem", request));

        BatchGetItemResponse output = operation("BatchGetItem").deserializeResponse(
            ok(DynamoDbItems.batchGetItemResponseJson()), BatchGetItemResponse.builder());
        assertEquals(DynamoDbItems.batchItems(), output.responses().get(DynamoDbItems.TABLE_NAME));
        assertTrue(output.hasUnprocessedKeys());
        assertTrue(output.unprocessedKeys().isEmpty());
    }

    @Test
    void batchWriteItemWith25Items() {
        List<WriteRequest> writes = new ArrayList<>();
        StringBuilder expected = new StringBuilder("{\"RequestItems\":{\"benchmark-table\":[");
        List<Map<String, AttributeValue>> items = DynamoDbItems.batchItems();
        for (int i = 0; i < items.size(); i++) {
            Map<String, AttributeValue> item = items.get(i);
            writes.add(WriteRequest.builder().putRequest(p -> p.item(item)).build());
            expected.append(i > 0 ? "," : "").append("{\"PutRequest\":{\"Item\":")
                    .append(DynamoDbItems.json(item)).append("}}");
        }
        BatchWriteItemRequest request = BatchWriteItemRequest.builder()
                                                             .requestItems(Map.of(DynamoDbItems.TABLE_NAME, writes))
                                                             .build();
        assertJson(expected.append("]}}").toString(), serialize("BatchWriteItem", request));

        BatchWriteItemResponse output = operation("BatchWriteItem").deserializeResponse(
            ok("{\"UnprocessedItems\":{}}"), BatchWriteItemResponse.builder());
        assertTrue(output.hasUnprocessedItems());
        assertTrue(output.unprocessedItems().isEmpty());
    }

    @Test
    void deserializesModeledError() {
        OperationProtocol getItem = operation("GetItem");
        SdkHttpResponse response = SdkHttpResponse.builder().statusCode(400)
                                                  .addHeader("Content-Type", "application/x-amz-json-1.0")
                                                  .body(HttpBody.fromUtf8(
                                                      "{\"__type\":\"com.amazonaws.dynamodb#ResourceNotFoundException\","
                                                      + "\"message\":\"Requested resource not found\"}"))
                                                  .build();
        assertTrue(getItem.isError(response));
        AwsServiceException error = getItem.deserializeError(response);
        ResourceNotFoundException notFound = assertInstanceOf(ResourceNotFoundException.class, error);
        assertTrue(notFound.getMessage().contains("Requested resource not found"), notFound.getMessage());
    }
}
