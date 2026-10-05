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

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.Document;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonNodeParser;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPage;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.enhanced.dynamodb.testing.EnhancedApi.TableApi;
import software.amazon.awssdk.enhanced.dynamodb.testing.EnhancedApi;
import software.amazon.awssdk.enhanced.dynamodb.testing.FakeDynamoDbHttp;
import software.amazon.awssdk.enhanced.dynamodb.testing.InMemoryDynamoDb;
import software.amazon.awssdk.enhanced.dynamodb.testing.models.Address;
import software.amazon.awssdk.enhanced.dynamodb.testing.models.Order;
import software.amazon.awssdk.enhanced.dynamodb.testing.models.Product;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Sync/async parity of the DynamoDB Enhanced Client over the generated V3 DynamoDB clients (Requirement 16.6): one
 * scenario covering bean, immutable and document schemas, table and index operations, conditional writes and
 * optimistic locking, query/scan pagination and filters, projections, batch get/write with unprocessed-item
 * pagination, transactions with cancellation reasons, the versioned-record, atomic-counter and custom extensions,
 * and table lifecycle runs through {@link DynamoDbEnhancedClient} and {@link DynamoDbEnhancedAsyncClient}. Each run
 * asserts the V2-documented results; the two runs must then produce the same observable transcript, the same
 * stored items and byte-identical DynamoDB wire requests.
 */
class EnhancedClientSyncAsyncParityTest {
    private static final Instant PLACED_AT = Instant.parse("2024-05-06T07:08:09.123Z");

    @Test
    void syncAndAsyncClientsHaveIdenticalBehaviorAndWireRequests() {
        InMemoryDynamoDb syncService = new InMemoryDynamoDb();
        FakeDynamoDbHttp syncHttp = FakeDynamoDbHttp.sync(syncService);
        List<String> syncTranscript;
        try (DynamoDbClient client = FakeDynamoDbHttp.syncClient(syncHttp, 1)) {
            syncTranscript = scenario(EnhancedApi.sync(DynamoDbEnhancedClient.builder().dynamoDbClient(client).build()),
                                      syncService);
        }

        InMemoryDynamoDb asyncService = new InMemoryDynamoDb();
        FakeDynamoDbHttp asyncHttp = FakeDynamoDbHttp.async(asyncService);
        List<String> asyncTranscript;
        try (DynamoDbAsyncClient client = FakeDynamoDbHttp.asyncClient(asyncHttp, 1)) {
            asyncTranscript = scenario(EnhancedApi.async(DynamoDbEnhancedAsyncClient.builder()
                                                                                    .dynamoDbClient(client).build()),
                                       asyncService);
        }

        assertThat(asyncTranscript).containsExactlyElementsOf(syncTranscript);
        assertThat(asyncService.items("products")).isEqualTo(syncService.items("products"));
        assertThat(asyncService.items("docs")).isEqualTo(syncService.items("docs"));
        assertThat(wire(asyncHttp)).containsExactlyElementsOf(wire(syncHttp));
        assertThat(syncHttp.openExchanges()).isZero();
        assertThat(asyncHttp.openExchanges()).isZero();
    }

    /** Request operation and body, without the generated idempotency token. */
    private static List<String> wire(FakeDynamoDbHttp http) {
        return http.requests().stream()
                   .map(r -> r.operation() + " " + r.body().replaceAll("\"ClientRequestToken\":\"[^\"]*\",?", ""))
                   .toList();
    }

    private static List<String> scenario(EnhancedApi api, InMemoryDynamoDb service) {
        List<String> transcript = new ArrayList<>();
        TableApi<Order> orders = api.table("orders", TableSchema.fromBean(Order.class));

        // Table lifecycle and description.
        orders.createTable(CreateTableEnhancedRequest.builder()
                                                     .globalSecondaryIndices(EnhancedGlobalSecondaryIndex.builder()
                                                                                .indexName("byStatus")
                                                                                .projection(p -> p.projectionType(
                                                                                    ProjectionType.ALL))
                                                                                .build())
                                                     .build());
        DescribeTableEnhancedResponse description = orders.describeTable();
        assertThat(description.table().tableName()).isEqualTo("orders");
        assertThat(description.table().keySchema()).extracting(k -> k.attributeName() + ":" + k.keyTypeAsString())
                                                   .containsExactly("customerId:HASH", "orderNumber:RANGE");
        assertThat(description.table().globalSecondaryIndexes()).extracting(i -> i.indexName())
                                                                 .containsExactly("byStatus");
        transcript.add("describe " + description.table().keySchema());

        // Puts; the versioned-record extension sets version 1 and makes each first put conditional.
        Order full = Order.of("c1", 1, "OPEN", 10);
        full.setNote("fragile");
        full.setShipTo(new Address("Seattle", 98101));
        full.setLines(List.of("book", "pen"));
        full.setQuantities(Map.of("book", 1));
        full.setTags(new LinkedHashSet<>(List.of("gift", "rush")));
        full.setPriority(Order.Priority.HIGH);
        full.setPlacedAt(PLACED_AT);
        orders.putItem(full);
        for (int n = 2; n <= 5; n++) {
            orders.putItem(Order.of("c1", n, n % 2 == 0 ? "SHIPPED" : "OPEN", 10 * n));
        }
        orders.putItem(Order.of("c2", 1, "SHIPPED", 15));
        orders.putItem(Order.of("c2", 2, "OPEN", 25));

        Order stored = orders.getItem(key("c1", 1));
        full.setVersion(1L);
        assertThat(stored).isEqualTo(full);
        transcript.add("get " + stored);
        assertThat(orders.getItem(key("c9", 9))).isNull();

        // The renamed attribute and the native DynamoDB types on the wire.
        Map<String, AttributeValue> raw = service.items("orders").get(0);
        assertThat(raw.get("order_note").s()).isEqualTo("fragile");
        assertThat(raw.get("shipTo").m().get("zip").n()).isEqualTo("98101");
        assertThat(raw.get("tags").ss()).containsExactlyInAnyOrder("gift", "rush");
        assertThat(raw.get("placedAt").s()).isEqualTo("2024-05-06T07:08:09.123Z");
        assertThat(raw.get("version").n()).isEqualTo("1");

        // Optimistic locking: a write with a stale (absent) version fails the generated condition.
        assertThatThrownBy(() -> orders.putItem(Order.of("c1", 1, "LOST", 0)))
            .isInstanceOf(ConditionalCheckFailedException.class);
        transcript.add("stale put rejected");

        // Update with ignoreNulls merges into the stored item and bumps the version.
        Order patch = Order.of("c1", 2, "DELIVERED", 20);
        patch.setVersion(1L);
        Order updated = orders.updateItem(UpdateItemEnhancedRequest.builder(Order.class).item(patch)
                                                                   .ignoreNulls(true).build());
        assertThat(updated.getStatus()).isEqualTo("DELIVERED");
        assertThat(updated.getVersion()).isEqualTo(2L);
        transcript.add("update " + updated);

        // Conditional put with a custom expression.
        Expression notExpensive = Expression.builder().expression("#a < :max")
                                            .putExpressionName("#a", "amount")
                                            .putExpressionValue(":max", AttributeValue.fromN("100")).build();
        Order repriced = Order.of("c1", 3, "OPEN", 31);
        repriced.setVersion(1L);
        transcript.add("conditional put " + orders.putItem(PutItemEnhancedRequest.builder(Order.class)
                                                                                 .item(repriced)
                                                                                 .conditionExpression(notExpensive)
                                                                                 .build()));

        // Query with a sort-key range, pagination by limit, filters and projections.
        List<Page<Order>> range = orders.query(QueryEnhancedRequest.builder()
                                                                   .queryConditional(QueryConditional.sortBetween(
                                                                       key("c1", 2), key("c1", 4)))
                                                                   .build());
        assertThat(range).hasSize(1);
        assertThat(numbers(range.get(0).items())).containsExactly(2, 3, 4);

        List<Page<Order>> pages = orders.query(QueryEnhancedRequest.builder()
                                                                   .queryConditional(QueryConditional.keyEqualTo(
                                                                       Key.builder().partitionValue("c1").build()))
                                                                   .limit(2).build());
        assertThat(pages).extracting(p -> numbers(p.items())).containsExactly(List.of(1, 2), List.of(3, 4), List.of(5));
        assertThat(pages.get(0).lastEvaluatedKey()).containsKeys("customerId", "orderNumber");
        assertThat(pages.get(2).lastEvaluatedKey()).isNull();
        transcript.add("pages " + pages.stream().map(p -> numbers(p.items())).toList());

        List<Order> descending = orders.queryItems(QueryEnhancedRequest.builder()
                                                                       .queryConditional(QueryConditional.keyEqualTo(
                                                                           k -> k.partitionValue("c1")))
                                                                       .scanIndexForward(false)
                                                                       .filterExpression(Expression.builder()
                                                                           .expression("amount >= :min")
                                                                           .putExpressionValue(":min",
                                                                                               AttributeValue.fromN("30"))
                                                                           .build())
                                                                       .attributesToProject("customerId", "orderNumber",
                                                                                            "amount")
                                                                       .build());
        assertThat(numbers(descending)).containsExactly(5, 4, 3);
        assertThat(descending).allSatisfy(o -> assertThat(o.getStatus()).isNull());
        transcript.add("descending " + descending);

        // Scan with a filter, and a GSI query ordered by the index sort key.
        List<Page<Order>> scanned = orders.scan(ScanEnhancedRequest.builder()
                                                                  .filterExpression(Expression.builder()
                                                                      .expression("#s = :open")
                                                                      .putExpressionName("#s", "status")
                                                                      .putExpressionValue(":open",
                                                                                          AttributeValue.fromS("OPEN"))
                                                                      .build())
                                                                  .limit(3).build());
        List<String> open = scanned.stream().flatMap(p -> p.items().stream())
                                   .map(o -> o.getCustomerId() + "/" + o.getOrderNumber()).toList();
        assertThat(open).containsExactly("c1/1", "c1/3", "c1/5", "c2/2");
        assertThat(scanned).hasSize(3);
        transcript.add("scan " + open);

        List<Page<Order>> shipped = orders.queryIndex("byStatus", QueryEnhancedRequest.builder()
                                                                                      .queryConditional(
                                                                                          QueryConditional.keyEqualTo(
                                                                                              k -> k.partitionValue(
                                                                                                  "SHIPPED")))
                                                                                      .build());
        assertThat(shipped.stream().flatMap(p -> p.items().stream()).map(Order::getAmount).toList())
            .containsExactly(15, 40);
        transcript.add("index " + shipped.get(0).items());

        // Batch get over unprocessed-key pages (the service answers two keys per call).
        service.batchGetKeysPerCall(2);
        List<BatchGetResultPage> batchPages =
            api.batchGetItem(BatchGetItemEnhancedRequest.builder()
                                                        .readBatches(ReadBatch.builder(Order.class)
                                                                              .mappedTableResource(orders.resource())
                                                                              .addGetItem(key("c1", 1))
                                                                              .addGetItem(key("c1", 3))
                                                                              .addGetItem(key("c2", 1))
                                                                              .addGetItem(key("c9", 9))
                                                                              .build())
                                                        .build());
        service.batchGetKeysPerCall(100);
        assertThat(batchPages).hasSize(2);
        List<String> batchKeys = batchPages.stream()
                                           .flatMap(p -> p.resultsForTable(orders.resource()).stream())
                                           .map(o -> o.getCustomerId() + "/" + o.getOrderNumber()).sorted().toList();
        assertThat(batchKeys).containsExactly("c1/1", "c1/3", "c2/1");
        transcript.add("batchGet " + batchKeys);

        // Batch write with an unprocessed remainder reported to the caller (the service answers one write per call).
        // A versioned item cannot be batch-put (its extension adds a condition), as in V2, so the batch deletes.
        service.batchWriteItemsPerCall(1);
        BatchWriteResult written =
            api.batchWriteItem(BatchWriteItemEnhancedRequest.builder()
                                                            .writeBatches(WriteBatch.builder(Order.class)
                                                                                    .mappedTableResource(
                                                                                        orders.resource())
                                                                                    .addDeleteItem(key("c2", 1))
                                                                                    .addDeleteItem(key("c2", 2))
                                                                                    .build())
                                                            .build());
        service.batchWriteItemsPerCall(25);
        assertThat(written.unprocessedPutItemsForTable(orders.resource())).isEmpty();
        assertThat(written.unprocessedDeleteItemsForTable(orders.resource())).containsExactly(key("c2", 2));
        assertThat(orders.getItem(key("c2", 1))).isNull();
        assertThat(orders.getItem(key("c2", 2))).isNotNull();
        transcript.add("batchWrite unprocessed " + written.unprocessedDeleteItemsForTable(orders.resource()));

        // Transactions: a successful write set, then one whose condition check cancels it.
        api.transactWriteItems(TransactWriteItemsEnhancedRequest.builder()
                                                                .addUpdateItem(orders.resource(),
                                                                               TransactUpdateItemEnhancedRequest
                                                                                   .builder(Order.class)
                                                                                   .item(versioned(Order.of(
                                                                                       "c1", 4, "SHIPPED", 44), 1L))
                                                                                   .build())
                                                                .addDeleteItem(orders.resource(), key("c1", 5))
                                                                .build());
        List<Document> documents = api.transactGetItems(TransactGetItemsEnhancedRequest.builder()
                                                                                       .addGetItem(orders.resource(),
                                                                                                   key("c1", 4))
                                                                                       .addGetItem(orders.resource(),
                                                                                                   key("c1", 5))
                                                                                       .build());
        assertThat(documents.get(0).getItem(orders.resource()).getAmount()).isEqualTo(44);
        assertThat(documents.get(1).getItem(orders.resource())).isNull();
        transcript.add("transactGet " + documents.get(0).getItem(orders.resource()));

        assertThatThrownBy(() -> api.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addConditionCheck(orders.resource(),
                                                                c -> c.key(key("c1", 1))
                                                                      .conditionExpression(Expression.builder()
                                                                          .expression("attribute_not_exists(amount)")
                                                                          .build()))
                                             .addPutItem(orders.resource(), Order.of("c4", 1, "OPEN", 1))
                                             .build()))
            .isInstanceOfSatisfying(TransactionCanceledException.class, e -> {
                assertThat(e.cancellationReasons()).extracting(r -> r.code())
                                                   .containsExactly("ConditionalCheckFailed", "None");
            });
        assertThat(orders.getItem(key("c4", 1))).isNull();
        transcript.add("transaction cancelled");

        // Delete returns the old item.
        assertThat(orders.deleteItem(key("c1", 3)).attributes().getAmount()).isEqualTo(31);

        // Immutable schema and the atomic-counter extension.
        TableApi<Product> products = api.table("products", TableSchema.fromImmutableClass(Product.class));
        products.createTable(CreateTableEnhancedRequest.builder().build());
        Product first = products.updateItem(UpdateItemEnhancedRequest.builder(Product.class)
                                                                     .item(Product.builder().sku("p1").name("Lamp")
                                                                                  .build())
                                                                     .build());
        Product second = products.updateItem(UpdateItemEnhancedRequest.builder(Product.class)
                                                                      .item(Product.builder().sku("p1").name("Lamp")
                                                                                   .build())
                                                                      .build());
        assertThat(first.views()).isEqualTo(10L);
        assertThat(second.views()).isEqualTo(15L);
        transcript.add("counter " + first + " " + second);

        // The document API: JSON in, attribute values on the wire, JSON out.
        TableApi<EnhancedDocument> docs =
            api.table("docs", TableSchema.documentSchemaBuilder()
                                         .addIndexPartitionKey(TableMetadata.primaryIndexName(), "id",
                                                               AttributeValueType.S)
                                         .attributeConverterProviders(AttributeConverterProvider.defaultProvider())
                                         .build());
        docs.createTable(CreateTableEnhancedRequest.builder().build());
        String json = "{\"id\":\"d1\",\"n\":1.50,\"list\":[true,null,\"x\"],\"nested\":{\"a\":\"\\u00e9\\n\"}}";
        docs.putItem(EnhancedDocument.fromJson(json));
        EnhancedDocument doc = docs.getItem(Key.builder().partitionValue("d1").build());
        assertThat(JsonNodeParser.create().parse(doc.toJson())).isEqualTo(JsonNodeParser.create().parse(json));
        assertThat(doc.getNumber("n").stringValue()).isEqualTo("1.50");
        transcript.add("document " + doc.toJson());

        // Table deletion.
        orders.deleteTable();
        assertThatThrownBy(orders::describeTable).isInstanceOf(ResourceNotFoundException.class);
        transcript.add("deleted");
        return transcript;
    }

    private static Order versioned(Order order, long version) {
        order.setVersion(version);
        return order;
    }

    private static Key key(String customer, int number) {
        return Key.builder().partitionValue(customer).sortValue(number).build();
    }

    private static List<Integer> numbers(List<Order> orders) {
        return orders.stream().map(Order::getOrderNumber).collect(Collectors.toList());
    }
}
