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

package software.amazon.awssdk.schemaserde.sourcecompat.fixtures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

/**
 * DynamoDB and DynamoDB Streams, written against the Baseline_Build public API (Requirement 16.8).
 */
public final class DynamoDbFixture {
    public static final String TABLE = "fixture-table";

    private DynamoDbFixture() {
    }

    public static DynamoDbClient syncClient(SdkHttpClient httpClient, SdkPlugin plugin,
                                            ClientSettings.CallCounter counter) {
        return DynamoDbClient.builder()
                             .region(ClientSettings.REGION)
                             .credentialsProvider(ClientSettings.credentials())
                             .overrideConfiguration(ClientSettings.overrideConfiguration(counter))
                             .endpointOverride(ClientSettings.ENDPOINT)
                             .httpClient(httpClient)
                             .endpointDiscoveryEnabled(false)
                             .addPlugin(plugin)
                             .build();
    }

    public static DynamoDbAsyncClient asyncClient(SdkAsyncHttpClient httpClient, SdkPlugin plugin) {
        return DynamoDbAsyncClient.builder()
                                  .region(ClientSettings.REGION)
                                  .credentialsProvider(ClientSettings.credentials())
                                  .endpointOverride(ClientSettings.ENDPOINT)
                                  .httpClient(httpClient)
                                  .addPlugin(plugin)
                                  .build();
    }

    /**
     * DynamoDB Streams is still generated on the legacy route (task 9.2); it is called through {@code httpClient}.
     */
    public static DynamoDbStreamsClient streamsClient(SdkHttpClient httpClient) {
        return DynamoDbStreamsClient.builder()
                                    .region(ClientSettings.REGION)
                                    .credentialsProvider(ClientSettings.credentials())
                                    .endpointOverride(ClientSettings.ENDPOINT)
                                    .httpClient(httpClient)
                                    .build();
    }

    public static GetItemRequest getItemRequest() {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("pk", AttributeValue.builder().s("user#1").build());
        return GetItemRequest.builder()
                             .tableName(TABLE)
                             .key(key)
                             .consistentRead(true)
                             .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                             .build();
    }

    public static GetItemResponse getItemResponse() {
        Map<String, AttributeValue> address = new HashMap<>();
        address.put("city", AttributeValue.fromS("Seattle"));
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.fromS("user#1"));
        item.put("age", AttributeValue.builder().n("42").build());
        item.put("tags", AttributeValue.builder().ss("a", "b").build());
        item.put("address", AttributeValue.builder().m(address).build());
        item.put("history", AttributeValue.fromL(Arrays.asList(AttributeValue.fromN("1"), AttributeValue.fromBool(true))));
        item.put("avatar", AttributeValue.fromB(SdkBytes.fromUtf8String("png")));
        return GetItemResponse.builder()
                              .item(item)
                              .consumedCapacity(ConsumedCapacity.builder().tableName(TABLE).capacityUnits(0.5).build())
                              .build();
    }

    /**
     * Reads a response through its getters: a map of a recursive union-like structure (string, number, string set,
     * nested map, list, boolean, blob) and a nested structure.
     */
    public static String summarize(GetItemResponse response) {
        Map<String, AttributeValue> item = new TreeMap<>(response.item());
        StringBuilder result = new StringBuilder();
        result.append(item.get("pk").s())
              .append(' ').append(item.get("age").n())
              .append(' ').append(item.get("tags").ss())
              .append(' ').append(item.get("address").m().get("city").s())
              .append(' ').append(item.get("history").l().get(0).n()).append(',').append(item.get("history").l().get(1).bool())
              .append(' ').append(item.get("avatar").b().asUtf8String())
              .append(' ').append(item.get("pk").type())
              .append(' ').append(response.consumedCapacity().tableName())
              .append(' ').append(response.consumedCapacity().capacityUnits());
        return result.toString();
    }

    public static String getItem(DynamoDbClient client) {
        return summarize(client.getItem(getItemRequest()));
    }

    public static DescribeTableResponse describeTableResponse(TableStatus status) {
        TableDescription table = TableDescription.builder()
                                                 .tableName(TABLE)
                                                 .tableStatus(status)
                                                 .keySchema(KeySchemaElement.builder()
                                                                            .attributeName("pk")
                                                                            .keyType(KeyType.HASH)
                                                                            .build())
                                                 .itemCount(7L)
                                                 .build();
        return DescribeTableResponse.builder().table(table).build();
    }

    public static CompletableFuture<String> describeTableAsync(DynamoDbAsyncClient client) {
        return client.describeTable(r -> r.tableName(TABLE))
                     .thenApply(r -> r.table().tableName() + " " + r.table().tableStatus() + "/"
                                     + r.table().tableStatusAsString() + " " + r.table().keySchema().get(0).keyType()
                                     + " " + r.table().itemCount());
    }

    public static boolean waitUntilTableExists(DynamoDbClient client) {
        try (DynamoDbWaiter waiter = DynamoDbWaiter.builder().client(client).build()) {
            WaiterResponse<DescribeTableResponse> response = waiter.waitUntilTableExists(r -> r.tableName(TABLE));
            return response.matched().response().map(r -> r.table().tableStatus() == TableStatus.ACTIVE).orElse(false);
        }
    }

    public static List<String> scanKeys(DynamoDbClient client) {
        List<String> keys = new ArrayList<>();
        client.scanPaginator(r -> r.tableName(TABLE)).items().forEach(i -> keys.add(i.get("pk").s()));
        return keys;
    }

    public static ConditionalCheckFailedException conditionalCheckFailed() {
        return ConditionalCheckFailedException.builder().message("The conditional request failed").statusCode(400).build();
    }

    public static String putItemConditionally(DynamoDbClient client) {
        try {
            client.putItem(r -> r.tableName(TABLE)
                                 .item(getItemRequest().key())
                                 .conditionExpression("attribute_not_exists(pk)"));
            return "no exception";
        } catch (ConditionalCheckFailedException e) {
            return "ConditionalCheckFailedException " + e.statusCode() + " " + e.awsErrorDetails().errorCode()
                   + " " + e.awsErrorDetails().serviceName();
        } catch (DynamoDbException e) {
            return "DynamoDbException " + e.awsErrorDetails().errorCode();
        }
    }

    public static List<String> streamArns(DynamoDbStreamsClient client) {
        List<String> arns = new ArrayList<>();
        client.listStreams(r -> r.tableName(TABLE)).streams().forEach(s -> arns.add(s.streamArn() + " " + s.tableName()));
        return arns;
    }
}
