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

package software.amazon.awssdk.testutils.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.account.AccountClient;
import software.amazon.awssdk.services.account.model.AlternateContactType;
import software.amazon.awssdk.services.account.model.GetAlternateContactResponse;
import software.amazon.awssdk.services.account.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.DashboardNotFoundErrorException;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.GetDashboardResponse;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.utils.StringInputStream;

class MockPluginTest {
    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    private final FailingHttpClient httpClient = new FailingHttpClient();
    private final FailingAsyncHttpClient asyncHttpClient = new FailingAsyncHttpClient();

    @Test
    void syncClient_whenOutputsQueued_returnsThemFirstInFirstOutAndRecordsRequests() {
        MockQueue queue = new MockQueue().enqueue(alarms("first")).enqueue(alarms("second"));
        MockPlugin mock = MockPlugin.builder().addQueue(queue).build();

        try (CloudWatchClient client = cloudWatch(mock)) {
            assertSameFields(alarms("first"), client.describeAlarms(r -> r.alarmNamePrefix("a")));
            assertSameFields(alarms("second"), client.describeAlarms(r -> r.alarmNamePrefix("b")));
        }

        assertThat(queue.remaining()).isZero();
        List<MockedRequest> requests = mock.getRequests();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).input()).isEqualTo(DescribeAlarmsRequest.builder().alarmNamePrefix("a").build());
        assertThat(requests.get(1).input()).isEqualTo(DescribeAlarmsRequest.builder().alarmNamePrefix("b").build());
        assertThat(requests.get(0).request().method()).isEqualTo(SdkHttpMethod.POST);
        assertThat(requests.get(0).request().firstMatchingHeader("Authorization")).isPresent();
        mock.clearRequests();
        assertThat(mock.getRequests()).isEmpty();
        assertThat(httpClient.calls()).isZero();
    }

    @Test
    void syncClient_whenModeledErrorQueued_throwsItWithStatus() {
        MockQueue queue = new MockQueue().enqueueError(
            DashboardNotFoundErrorException.builder().message("no dashboard").requestId("r-1").build());

        try (CloudWatchClient client = cloudWatch(MockPlugin.builder().addQueue(queue).build())) {
            Throwable thrown = catchThrowable(() -> client.getDashboard(r -> r.dashboardName("d")));

            assertThat(thrown).isInstanceOf(DashboardNotFoundErrorException.class);
            DashboardNotFoundErrorException error = (DashboardNotFoundErrorException) thrown;
            assertThat(error.statusCode()).isEqualTo(404);
            assertThat(error.awsErrorDetails().errorMessage()).isEqualTo("no dashboard");
            assertThat(error.requestId()).isEqualTo("r-1");
        }
        assertThat(httpClient.calls()).isZero();
    }

    @Test
    void syncClient_whenWrongSchemaQueued_throwsNamingExpectedAndQueuedIdsAndConsumesIt() {
        MockQueue queue = new MockQueue().enqueue(GetDashboardResponse.builder().dashboardName("d").build());

        try (CloudWatchClient client = cloudWatch(MockPlugin.builder().addQueue(queue).build())) {
            assertThatThrownBy(client::describeAlarms)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Expected one of [com.amazonaws.cloudwatch#DescribeAlarmsOutput, ")
                .hasMessageEndingWith("] but queued com.amazonaws.cloudwatch#GetDashboardOutput");
        }
        assertThat(queue.remaining()).isZero();
    }

    @Test
    void syncClient_whenQueueExhausted_failsWithoutCallingTheHttpClient() {
        MockPlugin mock = MockPlugin.builder().addQueue(new MockQueue()).build();

        try (CloudWatchClient client = cloudWatch(mock)) {
            assertThatThrownBy(client::describeAlarms)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Mock queue exhausted");
        }
        assertThat(mock.getRequests()).hasSize(1);
        assertThat(httpClient.calls()).isZero();
    }

    @Test
    void asyncClient_whenOutputAndErrorQueued_completesThenFailsInOrder() {
        MockQueue queue = new MockQueue().enqueue(alarms("async"))
                                         .enqueueError(DashboardNotFoundErrorException.builder().message("gone").build());

        try (CloudWatchAsyncClient client = cloudWatchAsync(MockPlugin.builder().addQueue(queue).build())) {
            assertSameFields(alarms("async"), client.describeAlarms().join());
            CompletableFuture<GetDashboardResponse> failed = client.getDashboard(r -> r.dashboardName("d"));

            assertThatThrownBy(failed::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(DashboardNotFoundErrorException.class)
                .hasMessageContaining("gone");
        }
        assertThat(asyncHttpClient.calls()).isZero();
    }

    @Test
    void asyncClient_whenQueueExhausted_failsWithoutCallingTheHttpClient() {
        try (CloudWatchAsyncClient client = cloudWatchAsync(MockPlugin.builder().addQueue(new MockQueue()).build())) {
            CompletableFuture<DescribeAlarmsResponse> future = client.describeAlarms();

            Throwable thrown = catchThrowable(() -> future.get(10, TimeUnit.SECONDS));
            assertThat(thrown).hasRootCauseInstanceOf(IllegalStateException.class)
                              .hasRootCauseMessage("Mock queue exhausted: no queued result or matcher for the call to "
                                                   + "com.amazonaws.cloudwatch#DescribeAlarms");
        }
        assertThat(asyncHttpClient.calls()).isZero();
    }

    @Test
    void restJsonClient_whenRawResponseQueued_deserializesItUnchanged() {
        String body = "{\"AlternateContact\":{\"AlternateContactType\":\"BILLING\",\"Name\":\"raw\"}}";
        SdkHttpFullResponse raw = SdkHttpFullResponse.builder()
                                                     .statusCode(200)
                                                     .putHeader("Content-Type", "application/json")
                                                     .putHeader("x-amzn-RequestId", "raw-request")
                                                     .putHeader("x-custom", "kept")
                                                     .content(AbortableInputStream.create(new StringInputStream(body)))
                                                     .build();
        MockQueue queue = new MockQueue().enqueue(raw);

        try (AccountClient client = account(MockPlugin.builder().addQueue(queue).build())) {
            GetAlternateContactResponse response =
                client.getAlternateContact(r -> r.alternateContactType(AlternateContactType.BILLING));

            assertThat(response.alternateContact().name()).isEqualTo("raw");
            assertThat(response.alternateContact().alternateContactType()).isEqualTo(AlternateContactType.BILLING);
            assertThat(response.sdkHttpResponse().firstMatchingHeader("x-custom")).hasValue("kept");
            assertThat(response.responseMetadata().requestId()).isEqualTo("raw-request");
        }
        assertThat(httpClient.calls()).isZero();
    }

    @Test
    void restJsonClient_whenMatcherAndQueue_usesTheFirstMatchingResult() {
        MockQueue queue = new MockQueue().enqueueError(ResourceNotFoundException.builder().message("missing").build());
        MockPlugin mock = MockPlugin.builder()
                                    .addMatcher(r -> r.request().encodedPath().contains("putAlternateContact")
                                                     ? MockedResult.error(new IllegalArgumentException("not this one"))
                                                     : null)
                                    .addQueue(queue)
                                    .build();

        try (AccountClient client = account(mock)) {
            assertThatThrownBy(() -> client.getAlternateContact(r -> r.alternateContactType(AlternateContactType.BILLING)))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(e -> assertThat(((ResourceNotFoundException) e).statusCode()).isEqualTo(404));
        }
        assertThat(mock.getRequests()).singleElement()
                                      .satisfies(r -> assertThat(r.request().encodedPath())
                                          .isEqualTo("/getAlternateContact"));
    }

    @Test
    void configureClient_whenNotAGeneratedClientConfiguration_throws() {
        SdkServiceClientConfiguration.Builder notGenerated = () -> {
            throw new UnsupportedOperationException();
        };

        assertThatThrownBy(() -> MockPlugin.builder().build().configureClient(notGenerated))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("wraps no SdkClientConfiguration.Builder");
    }

    private static void assertSameFields(SdkPojo expected, Object actual) {
        assertThat(expected.equalsBySdkFields(actual)).as("%s", actual).isTrue();
    }

    private static DescribeAlarmsResponse alarms(String name) {
        return DescribeAlarmsResponse.builder()
                                     .metricAlarms(MetricAlarm.builder().alarmName(name).threshold(1.5).build())
                                     .nextToken(name + "-token")
                                     .build();
    }

    private CloudWatchClient cloudWatch(MockPlugin mock) {
        return CloudWatchClient.builder().region(Region.US_EAST_1).credentialsProvider(CREDENTIALS)
                               .httpClient(httpClient).addPlugin(mock).build();
    }

    private CloudWatchAsyncClient cloudWatchAsync(MockPlugin mock) {
        return CloudWatchAsyncClient.builder().region(Region.US_EAST_1).credentialsProvider(CREDENTIALS)
                                    .httpClient(asyncHttpClient).addPlugin(mock).build();
    }

    private AccountClient account(MockPlugin mock) {
        return AccountClient.builder().region(Region.US_EAST_1).credentialsProvider(CREDENTIALS)
                            .httpClient(httpClient).addPlugin(mock).build();
    }

    /**
     * An HTTP client that counts and fails every call: a mocked client must never reach it.
     */
    private static final class FailingHttpClient implements SdkHttpClient {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            calls.incrementAndGet();
            throw new AssertionError("The mocked client called its HTTP client");
        }

        int calls() {
            return calls.get();
        }

        @Override
        public void close() {
        }
    }

    private static final class FailingAsyncHttpClient implements SdkAsyncHttpClient {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            calls.incrementAndGet();
            throw new AssertionError("The mocked client called its async HTTP client");
        }

        int calls() {
            return calls.get();
        }

        @Override
        public void close() {
        }
    }
}
