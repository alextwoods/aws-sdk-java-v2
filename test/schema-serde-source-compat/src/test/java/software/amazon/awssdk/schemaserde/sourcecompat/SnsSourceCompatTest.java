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
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.SnsFixture;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListSmsSandboxPhoneNumbersResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.Topic;
import software.amazon.awssdk.testutils.mock.MockPlugin;

/**
 * Runs {@link SnsFixture} against mocked SNS clients (awsQuery).
 */
class SnsSourceCompatTest {
    private final ClientSettings.CallCounter counter = new ClientSettings.CallCounter();

    @Test
    void publish_sync_readsMockedOutput() {
        MockPlugin plugin = MockedCalls.plugin(SnsFixture.publishResponse());
        try (SnsClient client = SnsFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(SnsFixture.publish(client)).isEqualTo("msg-1 10");
        }
        MockedCalls.assertSentWithClientSettings(plugin, 1);
        assertThat(counter.calls()).isEqualTo(1);
    }

    @Test
    void getTopicAttributes_async_readsStringMap() {
        MockPlugin plugin = MockedCalls.plugin(SnsFixture.getTopicAttributesResponse());
        try (SnsAsyncClient client = SnsFixture.asyncClient(new NoNetworkAsyncHttpClient(), plugin)) {
            assertThat(SnsFixture.getTopicAttributesAsync(client).join())
                .isEqualTo("true {DisplayName=Fixture, SubscriptionsConfirmed=2}");
        }
    }

    @Test
    void listSmsSandboxPhoneNumbersPaginator_readsEnumOfEveryPage() {
        MockPlugin plugin = MockedCalls.plugin(SnsFixture.sandboxPhoneNumbersResponse().toBuilder().nextToken("p2").build(),
                                               ListSmsSandboxPhoneNumbersResponse.builder().build());
        try (SnsClient client = SnsFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(SnsFixture.sandboxPhoneNumbers(client)).containsExactly("+12065550100 Verified/Verified");
        }
        assertThat(plugin.getRequests()).hasSize(2);
    }

    @Test
    void listTopicsPaginator_async_readsItemsOfEveryPage() {
        MockPlugin plugin = MockedCalls.plugin(
            ListTopicsResponse.builder().topics(Topic.builder().topicArn("arn:t1").build()).nextToken("p2").build(),
            ListTopicsResponse.builder().topics(Topic.builder().topicArn("arn:t2").build()).build());
        try (SnsAsyncClient client = SnsFixture.asyncClient(new NoNetworkAsyncHttpClient(), plugin)) {
            assertThat(SnsFixture.topicArnsAsync(client).join()).containsExactly("arn:t1", "arn:t2");
        }
    }

    @Test
    void getTopicAttributes_modeledError_caughtByType() {
        MockPlugin plugin = MockedCalls.plugin(SnsFixture.notFound());
        try (SnsClient client = SnsFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(SnsFixture.getMissingTopic(client)).isEqualTo("NotFoundException 404 NotFound");
        }
    }

    @Test
    void toBuilder_copiesRequestAndOverridesMember() {
        PublishRequest request = SnsFixture.publishRequest();
        PublishRequest copy = request.toBuilder().subject("copy").build();

        assertThat(copy.subject()).isEqualTo("copy");
        assertThat(copy.messageAttributes().get("color").stringValue()).isEqualTo("blue");
        assertThat(copy.toBuilder().subject("fixture").build()).isEqualTo(request);
    }
}
