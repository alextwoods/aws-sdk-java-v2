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

package software.amazon.awssdk.test.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.services.dynamodb.paginators.QueryIterable;
import software.amazon.awssdk.services.dynamodb.paginators.QueryPublisher;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.test.generated.support.Collect;
import software.amazon.awssdk.test.generated.support.FakeHttp;
import software.amazon.awssdk.test.generated.support.FakeHttp.Recorded;

/**
 * Compile and runtime fixtures of the API generated from the canonical DynamoDB model: V2 call shapes of typed,
 * consumer and override overloads, the query paginator, the {@code tableExists} waiter and the async client compile
 * against the generated V3 types (same FQCNs as V2) and run against a fake transport (Requirements 7.1-7.9, 8.1,
 * 8.3, 17.1, 17.2).
 */
class DynamoDbGeneratedApiTest {

    private static final URI ENDPOINT = URI.create("https://dynamodb.test");

    /** Answers by {@code X-Amz-Target}; a query with an exclusive start key gets the last page. */
    private static SdkHttpResponse dynamoDb(Recorded recorded) {
        String target = recorded.request().headers().firstValue("X-Amz-Target").orElse("");
        return switch (target) {
            case "DynamoDB_20120810.GetItem" -> FakeHttp.ok("{\"Item\":{\"id\":{\"S\":\"1\"},\"n\":{\"N\":\"7\"}}}");
            case "DynamoDB_20120810.PutItem" -> FakeHttp.ok("{}");
            case "DynamoDB_20120810.Query" -> recorded.body().contains("ExclusiveStartKey")
                                              ? FakeHttp.ok("{\"Items\":[{\"id\":{\"S\":\"b\"}}],\"Count\":1}")
                                              : FakeHttp.ok("{\"Items\":[{\"id\":{\"S\":\"a\"}}],\"Count\":1,"
                                                            + "\"LastEvaluatedKey\":{\"id\":{\"S\":\"a\"}}}");
            case "DynamoDB_20120810.DescribeTable" ->
                FakeHttp.ok("{\"Table\":{\"TableName\":\"t\",\"TableStatus\":\"ACTIVE\"}}");
            default -> throw new AssertionError("Unexpected target " + target);
        };
    }

    private static DynamoDbClient syncClient(FakeHttp http) {
        return DynamoDbClient.builder().region(Region.of("us-east-1")).credentialsProvider(FakeHttp.CREDENTIALS)
                             .endpointOverride(ENDPOINT).httpClient(http).build();
    }

    private static DynamoDbAsyncClient asyncClient(FakeHttp http) {
        return DynamoDbAsyncClient.builder().region(Region.of("us-east-1")).credentialsProvider(FakeHttp.CREDENTIALS)
                                  .endpointOverride(ENDPOINT).httpClient(http).build();
    }

    @Test
    void syncTypedConsumerAndOverrideOverloadsCallTheService() {
        FakeHttp http = FakeHttp.sync(DynamoDbGeneratedApiTest::dynamoDb);
        try (DynamoDbClient client = syncClient(http)) {
            Map<String, AttributeValue> key = Map.of("id", AttributeValue.builder().s("1").build());
            GetItemResponse typed = client.getItem(GetItemRequest.builder().tableName("t").key(key).build());
            GetItemResponse consumer = client.getItem(r -> r.tableName("t").key(key).consistentRead(true));
            GetItemResponse override = client.getItem(GetItemRequest.builder().tableName("t").key(key).build(),
                                                      null);
            client.putItem(PutItemRequest.builder().tableName("t").item(key).build());
            client.putItem(r -> r.tableName("t").item(key));

            assertEquals("1", typed.item().get("id").s());
            assertEquals("7", consumer.item().get("n").n());
            assertEquals(typed, override);
            List<String> targets = http.requests().stream()
                                       .map(r -> r.request().headers().firstValue("X-Amz-Target").orElseThrow())
                                       .toList();
            assertEquals(List.of("DynamoDB_20120810.GetItem", "DynamoDB_20120810.GetItem",
                                 "DynamoDB_20120810.GetItem", "DynamoDB_20120810.PutItem",
                                 "DynamoDB_20120810.PutItem"), targets);
            assertTrue(http.requests().get(1).body().contains("\"ConsistentRead\":true"),
                       http.requests().get(1).body());
        }
    }

    @Test
    void syncQueryPaginatorFollowsTheLastEvaluatedKey() {
        FakeHttp http = FakeHttp.sync(DynamoDbGeneratedApiTest::dynamoDb);
        try (DynamoDbClient client = syncClient(http)) {
            QueryIterable pages = client.queryPaginator(r -> r.tableName("t").keyConditionExpression("id = :id"));
            List<String> ids = new ArrayList<>();
            for (QueryResponse page : pages) {
                page.items().forEach(item -> ids.add(item.get("id").s()));
            }
            assertEquals(List.of("a", "b"), ids);
            assertEquals(2, http.requests().size());
        }
    }

    @Test
    void syncTableExistsWaiterMatchesAnActiveTable() {
        FakeHttp http = FakeHttp.sync(DynamoDbGeneratedApiTest::dynamoDb);
        try (DynamoDbClient client = syncClient(http);
             DynamoDbWaiter waiter = client.waiter()) {
            WaiterResponse<DescribeTableResponse> response =
                waiter.waitUntilTableExists(DescribeTableRequest.builder().tableName("t").build());
            Optional<DescribeTableResponse> matched = response.matched().response();
            assertEquals(TableStatus.ACTIVE, matched.orElseThrow().table().tableStatus());
        }
    }

    @Test
    void asyncClientPaginatorAndWaiterCompleteFutures() {
        FakeHttp http = FakeHttp.async(DynamoDbGeneratedApiTest::dynamoDb);
        try (DynamoDbAsyncClient client = asyncClient(http)) {
            CompletableFuture<GetItemResponse> item =
                client.getItem(r -> r.tableName("t").key(Map.of("id", AttributeValue.builder().s("1").build())));
            assertEquals("1", item.join().item().get("id").s());

            QueryPublisher pages = client.queryPaginator(QueryRequest.builder().tableName("t").build());
            List<String> ids = Collect.all(pages).join().stream()
                                      .flatMap(page -> page.items().stream())
                                      .map(i -> i.get("id").s())
                                      .toList();
            assertEquals(List.of("a", "b"), ids);

            try (DynamoDbAsyncWaiter waiter = client.waiter()) {
                WaiterResponse<DescribeTableResponse> exists =
                    waiter.waitUntilTableExists(r -> r.tableName("t")).join();
                assertEquals(TableStatus.ACTIVE, exists.matched().response().orElseThrow().table().tableStatus());
            }
        }
    }
}
