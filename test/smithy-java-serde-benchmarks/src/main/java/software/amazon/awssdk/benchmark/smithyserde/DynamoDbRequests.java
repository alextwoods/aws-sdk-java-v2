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

package software.amazon.awssdk.benchmark.smithyserde;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/** The four standard DynamoDB requests, built exactly as {@code test/standalone-e2e-benchmarks}' Workloads builds them. */
final class DynamoDbRequests {

    private DynamoDbRequests() {
    }

    static GetItemRequest smallGet() {
        return GetItemRequest.builder()
                             .tableName(BenchmarkItems.TABLE_NAME)
                             .key(Map.of("pk", AttributeValue.fromS(BenchmarkItems.SMALL_KEY)))
                             .build();
    }

    static PutItemRequest smallPut() {
        return PutItemRequest.builder()
                             .tableName(BenchmarkItems.TABLE_NAME)
                             .item(BenchmarkItems.toV2(BenchmarkItems.smallItem()))
                             .build();
    }

    static BatchGetItemRequest batchGet() {
        List<Map<String, AttributeValue>> keys = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            keys.add(Map.of("pk", AttributeValue.fromS(BenchmarkItems.batchKey(i))));
        }
        return BatchGetItemRequest.builder()
                                  .requestItems(Map.of(BenchmarkItems.TABLE_NAME,
                                                       KeysAndAttributes.builder().keys(keys).build()))
                                  .build();
    }

    static BatchWriteItemRequest batchPut() {
        List<WriteRequest> writes = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            writes.add(WriteRequest.builder()
                                   .putRequest(PutRequest.builder()
                                                         .item(BenchmarkItems.toV2(BenchmarkItems.mediumItem(i)))
                                                         .build())
                                   .build());
        }
        return BatchWriteItemRequest.builder()
                                    .requestItems(Map.of(BenchmarkItems.TABLE_NAME, writes))
                                    .build();
    }
}
