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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.AlarmType;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.ComparisonOperator;
import software.amazon.awssdk.services.cloudwatch.model.DashboardNotFoundErrorException;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.StateValue;
import software.amazon.awssdk.services.cloudwatch.waiters.CloudWatchWaiter;

/**
 * CloudWatch, written against the Baseline_Build public API (Requirement 16.8).
 */
public final class CloudWatchFixture {
    private CloudWatchFixture() {
    }

    public static CloudWatchClient syncClient(SdkHttpClient httpClient, SdkPlugin plugin,
                                              ClientSettings.CallCounter counter) {
        return CloudWatchClient.builder()
                               .region(ClientSettings.REGION)
                               .credentialsProvider(ClientSettings.credentials())
                               .overrideConfiguration(ClientSettings.overrideConfiguration(counter))
                               .endpointOverride(ClientSettings.ENDPOINT)
                               .httpClient(httpClient)
                               .addPlugin(plugin)
                               .build();
    }

    public static CloudWatchAsyncClient asyncClient(SdkAsyncHttpClient httpClient, SdkPlugin plugin) {
        return CloudWatchAsyncClient.builder()
                                    .region(ClientSettings.REGION)
                                    .credentialsProvider(ClientSettings.credentials())
                                    .overrideConfiguration(o -> o.apiCallTimeout(Duration.ofSeconds(30)))
                                    .endpointOverride(ClientSettings.ENDPOINT)
                                    .httpClient(httpClient)
                                    .addPlugin(plugin)
                                    .build();
    }

    public static DescribeAlarmsRequest describeAlarmsRequest() {
        return DescribeAlarmsRequest.builder()
                                    .alarmNamePrefix("cpu")
                                    .stateValue(StateValue.ALARM)
                                    .alarmTypes(AlarmType.METRIC_ALARM)
                                    .maxRecords(10)
                                    .build();
    }

    public static DescribeAlarmsRequest withNextToken(DescribeAlarmsRequest request, String nextToken) {
        return request.toBuilder().nextToken(nextToken).build();
    }

    public static DescribeAlarmsResponse describeAlarmsResponse() {
        MetricAlarm alarm = MetricAlarm.builder()
                                       .alarmName("cpu-high")
                                       .stateValue(StateValue.ALARM)
                                       .threshold(80.0)
                                       .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                                       .dimensions(Dimension.builder().name("InstanceId").value("i-1").build(),
                                                   Dimension.builder().name("Az").value("us-west-2a").build())
                                       .stateUpdatedTimestamp(Instant.ofEpochSecond(1_700_000_000L))
                                       .actionsEnabled(true)
                                       .build();
        return DescribeAlarmsResponse.builder().metricAlarms(alarm).build();
    }

    /**
     * Reads a response through its getters: a list of nested structures, enums with {@code xxxAsString}, a list of
     * structures inside them, a timestamp and a boolean.
     */
    public static String summarize(DescribeAlarmsResponse response) {
        StringBuilder result = new StringBuilder();
        for (MetricAlarm alarm : response.metricAlarms()) {
            result.append(alarm.alarmName())
                  .append(' ').append(alarm.stateValue()).append('/').append(alarm.stateValueAsString())
                  .append(' ').append(alarm.comparisonOperator()).append('/').append(alarm.comparisonOperatorAsString())
                  .append(' ').append(alarm.threshold())
                  .append(' ').append(alarm.actionsEnabled())
                  .append(' ').append(alarm.stateUpdatedTimestamp());
            for (Dimension dimension : alarm.dimensions()) {
                result.append(' ').append(dimension.name()).append('=').append(dimension.value());
            }
        }
        result.append(" compositeAlarms=").append(response.hasCompositeAlarms()).append(response.compositeAlarms());
        return result.toString();
    }

    public static String describeAlarms(CloudWatchClient client) {
        return summarize(client.describeAlarms(r -> r.alarmNamePrefix("cpu").stateValue(StateValue.ALARM)));
    }

    public static CompletableFuture<String> describeAlarmsAsync(CloudWatchAsyncClient client) {
        return client.describeAlarms(describeAlarmsRequest()).thenApply(CloudWatchFixture::summarize);
    }

    public static List<String> alarmNames(CloudWatchClient client) {
        List<String> names = new ArrayList<>();
        client.describeAlarmsPaginator(describeAlarmsRequest()).metricAlarms().forEach(a -> names.add(a.alarmName()));
        return names;
    }

    public static boolean waitUntilAlarmExists(CloudWatchClient client) {
        try (CloudWatchWaiter waiter = client.waiter()) {
            WaiterResponse<DescribeAlarmsResponse> response = waiter.waitUntilAlarmExists(r -> r.alarmNames("cpu-high"));
            return response.matched().response().isPresent();
        }
    }

    public static DashboardNotFoundErrorException dashboardNotFound() {
        return DashboardNotFoundErrorException.builder().message("Dashboard cpu does not exist").statusCode(404).build();
    }

    /**
     * Calls {@code GetDashboard} and catches the modeled exception by type.
     */
    public static String getMissingDashboard(CloudWatchClient client) {
        try {
            client.getDashboard(r -> r.dashboardName("cpu"));
            return "no exception";
        } catch (DashboardNotFoundErrorException e) {
            DashboardNotFoundErrorException copy = (DashboardNotFoundErrorException) e.toBuilder().build();
            return "DashboardNotFoundErrorException " + e.statusCode() + " " + e.awsErrorDetails().errorCode()
                   + " " + copy.awsErrorDetails().errorMessage();
        } catch (CloudWatchException e) {
            return "CloudWatchException " + e.awsErrorDetails().errorCode();
        }
    }
}
