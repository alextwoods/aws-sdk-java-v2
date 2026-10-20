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

package software.amazon.awssdk.schemaserde.sourcecompat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schemaserde.sourcecompat.MockedCalls.NoNetworkAsyncHttpClient;
import software.amazon.awssdk.schemaserde.sourcecompat.MockedCalls.NoNetworkHttpClient;
import software.amazon.awssdk.schemaserde.sourcecompat.MockedCalls.StubHttpClient;
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.ClientSettings;
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.DynamoDbFixture;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;
import software.amazon.awssdk.testutils.mock.MockPlugin;

/**
 * Runs {@link DynamoDbFixture} against mocked DynamoDB clients (awsJson1_0), and DynamoDB Streams (legacy route) against
 * a stub HTTP client.
 */
class DynamoDbSourceCompatTest {
    private final ClientSettings.CallCounter counter = new ClientSettings.CallCounter();

    @Test
    void getItem_sync_readsAttributeValueMap() {
        MockPlugin plugin = MockedCalls.plugin(DynamoDbFixture.getItemResponse());
        try (DynamoDbClient client = DynamoDbFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(DynamoDbFixture.getItem(client))
                .isEqualTo(DynamoDbFixture.summarize(DynamoDbFixture.getItemResponse()))
                .isEqualTo("user#1 42 [a, b] Seattle 1,true png S fixture-table 0.5");
        }
        MockedCalls.assertSentWithClientSettings(plugin, 1);
        assertThat(counter.calls()).isEqualTo(1);
    }

    @Test
    void describeTable_async_readsNestedStructureAndEnum() {
        MockPlugin plugin = MockedCalls.plugin(DynamoDbFixture.describeTableResponse(TableStatus.ACTIVE));
        try (DynamoDbAsyncClient client = DynamoDbFixture.asyncClient(new NoNetworkAsyncHttpClient(), plugin)) {
            assertThat(DynamoDbFixture.describeTableAsync(client).join()).isEqualTo("fixture-table ACTIVE/ACTIVE HASH 7");
        }
    }

    @Test
    void waitUntilTableExists_matchesSuccessAcceptor() {
        MockPlugin plugin = MockedCalls.plugin(DynamoDbFixture.describeTableResponse(TableStatus.ACTIVE));
        try (DynamoDbClient client = DynamoDbFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(DynamoDbFixture.waitUntilTableExists(client)).isTrue();
        }
    }

    @Test
    void scanPaginator_readsItemsOfEveryPage() {
        ScanResponse first = ScanResponse.builder()
                                         .items(Collections.singletonMap("pk", AttributeValue.fromS("a")))
                                         .lastEvaluatedKey(Collections.singletonMap("pk", AttributeValue.fromS("a")))
                                         .build();
        ScanResponse last = ScanResponse.builder().items(Collections.singletonMap("pk", AttributeValue.fromS("b"))).build();
        MockPlugin plugin = MockedCalls.plugin(first, last);
        try (DynamoDbClient client = DynamoDbFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(DynamoDbFixture.scanKeys(client)).containsExactly("a", "b");
        }
    }

    @Test
    void putItem_modeledError_caughtByType() {
        MockPlugin plugin = MockedCalls.plugin(DynamoDbFixture.conditionalCheckFailed());
        try (DynamoDbClient client = DynamoDbFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(DynamoDbFixture.putItemConditionally(client))
                .isEqualTo("ConditionalCheckFailedException 400 ConditionalCheckFailedException DynamoDb");
        }
    }

    @Test
    void listStreams_legacyRoute_readsStubResponse() {
        StubHttpClient http = new StubHttpClient("{\"Streams\":[{\"StreamArn\":\"arn:stream/1\","
                                                 + "\"TableName\":\"fixture-table\"}]}");
        try (DynamoDbStreamsClient client = DynamoDbFixture.streamsClient(http)) {
            assertThat(DynamoDbFixture.streamArns(client)).containsExactly("arn:stream/1 fixture-table");
        }
        assertThat(http.requests()).hasSize(1);
        assertThat(http.requests().get(0).firstMatchingHeader("X-Amz-Target"))
            .hasValue("DynamoDBStreams_20120810.ListStreams");
    }

    @Test
    void toBuilder_copiesRequestAndOverridesMember() {
        GetItemRequest request = DynamoDbFixture.getItemRequest();
        GetItemRequest copy = request.toBuilder().consistentRead(false).build();

        assertThat(copy.consistentRead()).isFalse();
        assertThat(copy.key()).isEqualTo(request.key());
        assertThat(copy.returnConsumedCapacityAsString()).isEqualTo("TOTAL");
        assertThat(copy.toBuilder().consistentRead(true).build()).isEqualTo(request);
    }
}
