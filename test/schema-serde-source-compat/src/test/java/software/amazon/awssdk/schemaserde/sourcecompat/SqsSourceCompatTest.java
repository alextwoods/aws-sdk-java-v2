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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schemaserde.sourcecompat.MockedCalls.NoNetworkAsyncHttpClient;
import software.amazon.awssdk.schemaserde.sourcecompat.MockedCalls.NoNetworkHttpClient;
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.ClientSettings;
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.SqsFixture;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.testutils.mock.MockPlugin;

/**
 * Runs {@link SqsFixture} against mocked SQS clients (awsJson1_0 with awsQueryCompatible).
 */
class SqsSourceCompatTest {
    private final ClientSettings.CallCounter counter = new ClientSettings.CallCounter();

    @Test
    void receiveMessage_sync_readsMockedOutputThroughGetters() {
        MockPlugin plugin = MockedCalls.plugin(SqsFixture.receiveMessageResponse());
        try (SqsClient client = SqsFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(SqsFixture.receiveMessage(client))
                .isEqualTo(SqsFixture.summarize(SqsFixture.receiveMessageResponse()))
                .isEqualTo("m-1 hello 1700000000000 [SentTimestamp] color=String:blue raw=Binary:bytes");
        }
        MockedCalls.assertSentWithClientSettings(plugin, 1);
        assertThat(counter.calls()).isEqualTo(1);
    }

    @Test
    void getQueueAttributes_async_readsEnumKeyedMap() {
        MockPlugin plugin = MockedCalls.plugin(SqsFixture.getQueueAttributesResponse());
        try (SqsAsyncClient client = SqsFixture.asyncClient(new NoNetworkAsyncHttpClient(), plugin)) {
            assertThat(SqsFixture.getQueueAttributesAsync(client).join())
                .isEqualTo("3 {ApproximateNumberOfMessages=3, VisibilityTimeout=30}");
        }
    }

    @Test
    void listQueuesPaginator_readsItemsOfEveryPage() {
        MockPlugin plugin = MockedCalls.plugin(ListQueuesResponse.builder().queueUrls("q1").nextToken("p2").build(),
                                               ListQueuesResponse.builder().queueUrls("q2").build());
        try (SqsClient client = SqsFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(SqsFixture.queueUrls(client)).containsExactly("q1", "q2");
        }
    }

    @Test
    void getQueueUrl_modeledError_caughtByType() {
        MockPlugin plugin = MockedCalls.plugin(SqsFixture.queueDoesNotExist());
        try (SqsClient client = SqsFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(SqsFixture.getMissingQueueUrl(client)).startsWith("QueueDoesNotExistException 400 ");
        }
    }

    @Test
    void toBuilder_copiesRequestAndOverridesMember() {
        ReceiveMessageRequest request = SqsFixture.receiveMessageRequest();
        ReceiveMessageRequest copy = request.toBuilder().visibilityTimeout(30).build();

        assertThat(copy.visibilityTimeout()).isEqualTo(30);
        assertThat(copy.messageSystemAttributeNamesAsStrings()).containsExactly("SentTimestamp");
        assertThat(copy.toBuilder().visibilityTimeout(null).build()).isEqualTo(request);
    }
}
