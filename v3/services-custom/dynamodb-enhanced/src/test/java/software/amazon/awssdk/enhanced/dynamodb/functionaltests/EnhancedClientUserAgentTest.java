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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.enhanced.dynamodb.testing.EnhancedApi;
import software.amazon.awssdk.enhanced.dynamodb.testing.EnhancedApi.TableApi;
import software.amazon.awssdk.enhanced.dynamodb.testing.FakeDynamoDbHttp;
import software.amazon.awssdk.enhanced.dynamodb.testing.InMemoryDynamoDb;
import software.amazon.awssdk.enhanced.dynamodb.testing.models.Order;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Every DynamoDB request the enhanced sync and async clients send identifies the DynamoDB Enhanced Client in its user
 * agent with the V2 {@code DDB_MAPPER} feature identifier ({@code m/d}, V2 {@code sdk-metrics/d}), including every
 * page request of paginated queries, scans and batch gets; a request the application sends through the same DynamoDB
 * client does not (Requirements 16.6, 21.6, 21.7).
 */
class EnhancedClientUserAgentTest {
    private static final String ENHANCED_FEATURE = " m/d";

    @Test
    void everyRequestOfTheSyncEnhancedClientCarriesTheFeatureAndDirectCallsDoNot() {
        InMemoryDynamoDb service = new InMemoryDynamoDb();
        FakeDynamoDbHttp http = FakeDynamoDbHttp.sync(service);
        try (DynamoDbClient client = FakeDynamoDbHttp.syncClient(http, 1)) {
            scenario(EnhancedApi.sync(DynamoDbEnhancedClient.builder().dynamoDbClient(client).build()), service);
            assertEveryRequestIsTagged(http);
            // The table is gone, so the application's own call fails; it is still sent, untagged.
            assertThatThrownBy(() -> client.describeTable(r -> r.tableName("orders")))
                .isInstanceOf(ResourceNotFoundException.class);
        }
        assertLastRequestIsUntagged(http);
    }

    @Test
    void everyRequestOfTheAsyncEnhancedClientCarriesTheFeatureAndDirectCallsDoNot() {
        InMemoryDynamoDb service = new InMemoryDynamoDb();
        FakeDynamoDbHttp http = FakeDynamoDbHttp.async(service);
        try (DynamoDbAsyncClient client = FakeDynamoDbHttp.asyncClient(http, 1)) {
            scenario(EnhancedApi.async(DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(client).build()),
                     service);
            assertEveryRequestIsTagged(http);
            assertThatThrownBy(() -> client.describeTable(r -> r.tableName("orders")).join())
                .hasCauseInstanceOf(ResourceNotFoundException.class);
        }
        assertLastRequestIsUntagged(http);
    }

    private static void scenario(EnhancedApi api, InMemoryDynamoDb service) {
        TableApi<Order> orders = api.table("orders", TableSchema.fromBean(Order.class));
        orders.createTable(CreateTableEnhancedRequest.builder().build());
        orders.describeTable();
        for (int n = 1; n <= 4; n++) {
            orders.putItem(Order.of("c1", n, "OPEN", 10 * n));
        }
        orders.getItem(key(1));
        Order patch = Order.of("c1", 2, "SHIPPED", 20);
        patch.setVersion(1L);
        orders.updateItem(UpdateItemEnhancedRequest.builder(Order.class).item(patch).ignoreNulls(true).build());
        // Paginated operations: every page request is an enhanced request.
        assertThat(orders.query(QueryEnhancedRequest.builder().queryConditional(QueryConditional.keyEqualTo(
            Key.builder().partitionValue("c1").build())).limit(2).build())).hasSizeGreaterThan(1);
        assertThat(orders.scan(ScanEnhancedRequest.builder().limit(3).build())).hasSizeGreaterThan(1);
        service.batchGetKeysPerCall(1);
        assertThat(api.batchGetItem(BatchGetItemEnhancedRequest.builder()
                                                               .readBatches(ReadBatch.builder(Order.class)
                                                                                     .mappedTableResource(
                                                                                         orders.resource())
                                                                                     .addGetItem(key(1))
                                                                                     .addGetItem(key(3))
                                                                                     .build())
                                                               .build())).hasSizeGreaterThan(1);
        service.batchGetKeysPerCall(100);
        api.batchWriteItem(BatchWriteItemEnhancedRequest.builder()
                                                        .writeBatches(WriteBatch.builder(Order.class)
                                                                                .mappedTableResource(orders.resource())
                                                                                .addDeleteItem(key(4))
                                                                                .build())
                                                        .build());
        api.transactWriteItems(TransactWriteItemsEnhancedRequest.builder()
                                                                .addDeleteItem(orders.resource(), key(3))
                                                                .build());
        api.transactGetItems(TransactGetItemsEnhancedRequest.builder().addGetItem(orders.resource(), key(1)).build());
        orders.deleteItem(key(1));
        orders.deleteTable();
    }

    private static void assertEveryRequestIsTagged(FakeDynamoDbHttp http) {
        List<String> operations = http.operations();
        assertThat(operations).contains("CreateTable", "DescribeTable", "PutItem", "GetItem", "UpdateItem", "Query",
                                        "Scan", "BatchGetItem", "BatchWriteItem", "TransactWriteItems",
                                        "TransactGetItems", "DeleteItem", "DeleteTable");
        List<String> userAgents = http.userAgents();
        assertThat(userAgents).hasSameSizeAs(operations).allSatisfy(userAgent -> {
            assertThat(userAgent).startsWith("aws-sdk-java/").contains(" api/").contains(" md/op#")
                                 .contains(" cfg/retry-mode#standard");
            assertThat(userAgent.split(" ")).containsOnlyOnce(ENHANCED_FEATURE.trim());
        });
        for (int i = 0; i < operations.size(); i++) {
            assertThat(userAgents.get(i)).contains(" md/op#" + operations.get(i));
        }
    }

    private static void assertLastRequestIsUntagged(FakeDynamoDbHttp http) {
        List<String> userAgents = http.userAgents();
        assertThat(http.operations().get(userAgents.size() - 1)).isEqualTo("DescribeTable");
        assertThat(userAgents.get(userAgents.size() - 1)).startsWith("aws-sdk-java/").contains(" md/op#DescribeTable")
                                                         .doesNotContain(" m/");
    }

    private static Key key(int number) {
        return Key.builder().partitionValue("c1").sortValue(number).build();
    }
}
