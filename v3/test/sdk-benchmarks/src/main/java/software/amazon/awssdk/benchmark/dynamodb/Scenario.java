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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;

/**
 * racecar's four item scenarios. Each owns its prebuilt request (built once per JVM and reused every call, as in
 * racecar's {@code Workloads}) and the independently written wire JSON that request must serialize to.
 */
enum Scenario {
    SMALL_GET("small-get", "GetItem", GetItemResponse::builder),
    SMALL_PUT("small-put", "PutItem", PutItemResponse::builder),
    BATCH_GET("batch-get", "BatchGetItem", BatchGetItemResponse::builder),
    BATCH_PUT("batch-put", "BatchWriteItem", BatchWriteItemResponse::builder);

    final String cliName;
    final String operation;
    private final Supplier<? extends ShapeBuilder<? extends SerializableStruct>> output;

    Scenario(String cliName, String operation, Supplier<? extends ShapeBuilder<? extends SerializableStruct>> output) {
        this.cliName = cliName;
        this.operation = operation;
        this.output = output;
    }

    String target() {
        return MockDdbServer.TARGET_PREFIX + operation;
    }

    ShapeBuilder<? extends SerializableStruct> newOutputBuilder() {
        return output.get();
    }

    static Scenario fromCli(String name) {
        for (Scenario s : values()) {
            if (s.cliName.equals(name)) {
                return s;
            }
        }
        throw new IllegalArgumentException("unknown scenario: " + name);
    }

    /** The prebuilt request, identical in content to racecar's V2 request for this scenario. */
    SerializableStruct request() {
        return switch (this) {
            case SMALL_GET -> GetItemRequest.builder()
                                            .tableName(DynamoDbItems.TABLE_NAME)
                                            .key(Map.of("pk", AttributeValue.fromS(DynamoDbItems.SMALL_KEY)))
                                            .build();
            case SMALL_PUT -> PutItemRequest.builder()
                                            .tableName(DynamoDbItems.TABLE_NAME)
                                            .item(DynamoDbItems.smallItem())
                                            .build();
            case BATCH_GET -> {
                List<Map<String, AttributeValue>> keys = new ArrayList<>();
                for (int i = 0; i < DynamoDbItems.BATCH_SIZE; i++) {
                    keys.add(Map.of("pk", AttributeValue.fromS(DynamoDbItems.batchKey(i))));
                }
                yield BatchGetItemRequest.builder()
                                         .requestItems(Map.of(DynamoDbItems.TABLE_NAME,
                                                              KeysAndAttributes.builder().keys(keys).build()))
                                         .build();
            }
            case BATCH_PUT -> {
                List<WriteRequest> writes = new ArrayList<>();
                for (int i = 0; i < DynamoDbItems.BATCH_SIZE; i++) {
                    writes.add(WriteRequest.builder()
                                           .putRequest(PutRequest.builder().item(DynamoDbItems.mediumItem(i)).build())
                                           .build());
                }
                yield BatchWriteItemRequest.builder().requestItems(Map.of(DynamoDbItems.TABLE_NAME, writes)).build();
            }
        };
    }

    /** The exact request body bytes the server must receive, written without the V3 serializer. */
    String expectedRequestJson() {
        return switch (this) {
            case SMALL_GET -> "{\"TableName\":\"" + DynamoDbItems.TABLE_NAME + "\",\"Key\":{\"pk\":{\"S\":\""
                              + DynamoDbItems.SMALL_KEY + "\"}}}";
            case SMALL_PUT -> "{\"TableName\":\"" + DynamoDbItems.TABLE_NAME + "\",\"Item\":"
                              + DynamoDbItems.json(DynamoDbItems.smallItem()) + "}";
            case BATCH_GET -> {
                StringBuilder sb = new StringBuilder("{\"RequestItems\":{\"" + DynamoDbItems.TABLE_NAME
                                                     + "\":{\"Keys\":[");
                for (int i = 0; i < DynamoDbItems.BATCH_SIZE; i++) {
                    sb.append(i > 0 ? "," : "").append("{\"pk\":{\"S\":\"").append(DynamoDbItems.batchKey(i))
                      .append("\"}}");
                }
                yield sb.append("]}}}").toString();
            }
            case BATCH_PUT -> {
                StringBuilder sb = new StringBuilder("{\"RequestItems\":{\"" + DynamoDbItems.TABLE_NAME + "\":[");
                for (int i = 0; i < DynamoDbItems.BATCH_SIZE; i++) {
                    sb.append(i > 0 ? "," : "").append("{\"PutRequest\":{\"Item\":")
                      .append(DynamoDbItems.json(DynamoDbItems.mediumItem(i))).append("}}");
                }
                yield sb.append("]}}").toString();
            }
        };
    }
}
