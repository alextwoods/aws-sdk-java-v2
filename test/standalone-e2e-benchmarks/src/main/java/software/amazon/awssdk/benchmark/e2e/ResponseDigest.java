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

package software.amazon.awssdk.benchmark.e2e;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

/**
 * Prints a canonical digest of what the synchronous DynamoDB client actually returns, so the same digest can be
 * taken from every benchmark jar and compared.
 *
 * <p>Exists because a performance comparison between two pipelines is only meaningful if they produce the same
 * result. "Faster" is not interesting if the faster path skipped work, dropped members, or returned a partially
 * materialized response — and nothing in the timing harness would notice, since it discards responses. This
 * renders the response instead, field by field in sorted order, so the bridged and stock builds can be diffed
 * as text.
 *
 * <p>Deliberately not part of the timing path: it runs as its own main, against the same canned mock server.
 *
 * <p>Usage: {@code ResponseDigest <endpoint>}
 */
public final class ResponseDigest {

    private ResponseDigest() {
    }

    public static void main(String[] args) {
        URI endpoint = URI.create(args.length > 0 ? args[0] : "http://127.0.0.1:" + MockDdbServer.DEFAULT_PORT);
        try (DynamoDbClient ddb = DynamoDbClient.builder()
                                                .endpointOverride(endpoint)
                                                .region(Region.US_EAST_1)
                                                .credentialsProvider(StaticCredentialsProvider.create(
                                                    AwsBasicCredentials.create(Workloads.ACCESS_KEY,
                                                                               Workloads.SECRET_KEY)))
                                                .build()) {
            digestGetItem(ddb);
            digestBatchGetItem(ddb);
            digestDescribeTable(ddb);
        }
    }

    private static void digestGetItem(DynamoDbClient ddb) {
        GetItemResponse response = ddb.getItem(GetItemRequest.builder()
                                                             .tableName(BenchmarkItems.TABLE_NAME)
                                                             .key(Map.of("pk", AttributeValue.fromS(
                                                                 BenchmarkItems.SMALL_KEY)))
                                                             .build());
        System.out.println("getItem.hasItem=" + response.hasItem());
        System.out.println("getItem.item=" + renderItem(response.item()));
    }

    private static void digestBatchGetItem(DynamoDbClient ddb) {
        List<Map<String, AttributeValue>> keys = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            keys.add(Map.of("pk", AttributeValue.fromS(BenchmarkItems.batchKey(i))));
        }
        BatchGetItemResponse response = ddb.batchGetItem(
            BatchGetItemRequest.builder()
                               .requestItems(Map.of(BenchmarkItems.TABLE_NAME,
                                                    KeysAndAttributes.builder().keys(keys).build()))
                               .build());
        List<Map<String, AttributeValue>> items = response.responses().get(BenchmarkItems.TABLE_NAME);
        System.out.println("batchGetItem.tables=" + new TreeMap<>(response.responses()).keySet());
        System.out.println("batchGetItem.itemCount=" + (items == null ? -1 : items.size()));
        System.out.println("batchGetItem.unprocessedKeys=" + response.unprocessedKeys().size());
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                System.out.println("batchGetItem.item[" + i + "]=" + renderItem(items.get(i)));
            }
        }
    }

    private static void digestDescribeTable(DynamoDbClient ddb) {
        TableDescription t = ddb.describeTable(DescribeTableRequest.builder()
                                                                  .tableName(BenchmarkItems.TABLE_NAME)
                                                                  .build())
                               .table();
        System.out.println("describeTable.name=" + t.tableName());
        System.out.println("describeTable.status=" + t.tableStatusAsString());
        System.out.println("describeTable.itemCount=" + t.itemCount());
        System.out.println("describeTable.creationDateTime=" + t.creationDateTime());
        System.out.println("describeTable.attributeDefinitions=" + t.attributeDefinitions());
        System.out.println("describeTable.keySchema=" + t.keySchema());
        System.out.println("describeTable.provisionedThroughput=" + t.provisionedThroughput());
        System.out.println("describeTable.gsiCount=" + t.globalSecondaryIndexes().size());
        t.globalSecondaryIndexes().forEach(g -> System.out.println("describeTable.gsi=" + g));
        System.out.println("describeTable.lsiCount=" + t.localSecondaryIndexes().size());
        t.localSecondaryIndexes().forEach(l -> System.out.println("describeTable.lsi=" + l));
        System.out.println("describeTable.streamSpecification=" + t.streamSpecification());
        System.out.println("describeTable.sseDescription=" + t.sseDescription());
        System.out.println("describeTable.deletionProtection=" + t.deletionProtectionEnabled());
    }

    /**
     * Renders an item with keys sorted, so two runs differing only in map iteration order compare equal.
     */
    private static String renderItem(Map<String, AttributeValue> item) {
        return new TreeMap<>(item).toString();
    }
}
