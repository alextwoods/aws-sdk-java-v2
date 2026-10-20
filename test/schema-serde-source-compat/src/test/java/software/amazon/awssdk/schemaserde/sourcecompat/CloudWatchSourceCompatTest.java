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
import software.amazon.awssdk.schemaserde.sourcecompat.fixtures.CloudWatchFixture;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.testutils.mock.MockPlugin;

/**
 * Runs {@link CloudWatchFixture} against mocked CloudWatch clients (rpcv2Cbor on the feature build).
 */
class CloudWatchSourceCompatTest {
    private final ClientSettings.CallCounter counter = new ClientSettings.CallCounter();

    @Test
    void describeAlarms_sync_readsMockedOutputThroughGetters() {
        MockPlugin plugin = MockedCalls.plugin(CloudWatchFixture.describeAlarmsResponse());
        try (CloudWatchClient client = CloudWatchFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            String summary = CloudWatchFixture.describeAlarms(client);

            assertThat(summary).isEqualTo(CloudWatchFixture.summarize(CloudWatchFixture.describeAlarmsResponse()))
                               .startsWith("cpu-high ALARM/ALARM GreaterThanThreshold/GreaterThanThreshold 80.0 true "
                                           + "2023-11-14T22:13:20Z InstanceId=i-1 Az=us-west-2a");
        }
        MockedCalls.assertSentWithClientSettings(plugin, 1);
        assertThat(counter.calls()).isEqualTo(1);
    }

    @Test
    void describeAlarms_async_readsMockedOutputThroughGetters() {
        MockPlugin plugin = MockedCalls.plugin(CloudWatchFixture.describeAlarmsResponse());
        try (CloudWatchAsyncClient client = CloudWatchFixture.asyncClient(new NoNetworkAsyncHttpClient(), plugin)) {
            assertThat(CloudWatchFixture.describeAlarmsAsync(client).join())
                .isEqualTo(CloudWatchFixture.summarize(CloudWatchFixture.describeAlarmsResponse()));
        }
    }

    @Test
    void describeAlarmsPaginator_readsItemsOfEveryPage() {
        MockPlugin plugin = MockedCalls.plugin(CloudWatchFixture.describeAlarmsResponse().toBuilder().nextToken("p2").build(),
                                               CloudWatchFixture.describeAlarmsResponse());
        try (CloudWatchClient client = CloudWatchFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(CloudWatchFixture.alarmNames(client)).containsExactly("cpu-high", "cpu-high");
        }
        assertThat(plugin.getRequests()).hasSize(2);
    }

    @Test
    void waitUntilAlarmExists_matchesSuccessAcceptor() {
        MockPlugin plugin = MockedCalls.plugin(CloudWatchFixture.describeAlarmsResponse());
        try (CloudWatchClient client = CloudWatchFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(CloudWatchFixture.waitUntilAlarmExists(client)).isTrue();
        }
    }

    @Test
    void getDashboard_modeledError_caughtByType() {
        MockPlugin plugin = MockedCalls.plugin(CloudWatchFixture.dashboardNotFound());
        try (CloudWatchClient client = CloudWatchFixture.syncClient(new NoNetworkHttpClient(), plugin, counter)) {
            assertThat(CloudWatchFixture.getMissingDashboard(client))
                .startsWith("DashboardNotFoundErrorException ")
                .endsWith(" Dashboard cpu does not exist");
        }
    }

    @Test
    void toBuilder_copiesRequestAndOverridesMember() {
        DescribeAlarmsRequest request = CloudWatchFixture.describeAlarmsRequest();
        DescribeAlarmsRequest copy = CloudWatchFixture.withNextToken(request, "t");

        assertThat(copy.nextToken()).isEqualTo("t");
        assertThat(copy.toBuilder().nextToken(null).build()).isEqualTo(request);
        assertThat(copy.alarmTypesAsStrings()).containsExactly("MetricAlarm");
    }
}
