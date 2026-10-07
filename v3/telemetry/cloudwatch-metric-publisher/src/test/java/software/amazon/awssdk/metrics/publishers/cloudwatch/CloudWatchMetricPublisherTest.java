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

package software.amazon.awssdk.metrics.publishers.cloudwatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.telemetry.spi.AttemptRecord;
import software.amazon.awssdk.telemetry.spi.CallRecord;
import software.amazon.awssdk.telemetry.spi.TelemetryMetric;
import software.amazon.awssdk.telemetry.spi.TelemetryOutcome;
import software.amazon.awssdk.telemetry.spi.TelemetryPublisher;

/**
 * The V3 CloudWatch exporter over the generated V3 {@link CloudWatchAsyncClient} API: V2's aggregation, dimensions,
 * category/level filtering, detailed metrics, request limits, upload schedule and client ownership, fed by the
 * pipeline's terminal call records through {@link TelemetryPublisher#fromMetricPublisher}.
 */
class CloudWatchMetricPublisherTest {
    /** A caller-owned CloudWatch client that records each {@code PutMetricData} request and its own close. */
    private static final class RecordingCloudWatch {
        final List<PutMetricDataRequest> requests = new CopyOnWriteArrayList<>();
        final AtomicInteger closes = new AtomicInteger();
        final CloudWatchAsyncClient client = (CloudWatchAsyncClient) Proxy.newProxyInstance(
            CloudWatchAsyncClient.class.getClassLoader(), new Class<?>[] {CloudWatchAsyncClient.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "putMetricData" -> {
                    requests.add((PutMetricDataRequest) args[0]);
                    yield CompletableFuture.completedFuture(null);
                }
                case "close" -> {
                    closes.incrementAndGet();
                    yield null;
                }
                case "toString" -> "RecordingCloudWatch";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            });

        List<MetricDatum> data() {
            return requests.stream().flatMap(r -> r.metricData().stream()).toList();
        }
    }

    private static CallRecord call(String operation, TelemetryOutcome outcome, long millis, int... statuses) {
        List<AttemptRecord> attempts = new java.util.ArrayList<>();
        for (int i = 0; i < statuses.length; i++) {
            boolean last = i == statuses.length - 1;
            TelemetryOutcome attemptOutcome = last ? outcome : TelemetryOutcome.FAILED;
            attempts.add(new AttemptRecord("DynamoDB", operation, i + 1, attemptOutcome, "UrlConnection", "standard",
                                           Duration.ofMillis(millis), OptionalInt.of(statuses[i]),
                                           attemptOutcome == TelemetryOutcome.FAILED ? Optional.of("ServerError")
                                                                                     : Optional.empty()));
        }
        return new CallRecord("DynamoDB", operation, outcome, statuses.length, "UrlConnection", "standard",
                              Duration.ofMillis(millis * statuses.length), outcome == TelemetryOutcome.FAILED
                                                                           ? Optional.of("ServerError")
                                                                           : Optional.empty(), attempts);
    }

    private static Map<String, MetricDatum> byName(List<MetricDatum> data, String operation) {
        return data.stream()
                   .filter(d -> d.dimensions().contains(Dimension.builder().name("OperationName").value(operation)
                                                                 .build()))
                   .collect(Collectors.toMap(MetricDatum::metricName, d -> d));
    }

    @Test
    void callRecordsAreAggregatedPerOperationAndUploadedOnClose() {
        RecordingCloudWatch cloudWatch = new RecordingCloudWatch();
        CloudWatchMetricPublisher publisher = CloudWatchMetricPublisher.builder()
                                                                       .cloudWatchClient(cloudWatch.client)
                                                                       .namespace("Test/Telemetry")
                                                                       .build();
        TelemetryPublisher bridge = TelemetryPublisher.fromMetricPublisher(publisher);
        bridge.callCompleted(call("GetItem", TelemetryOutcome.SUCCEEDED, 10, 200));
        bridge.callCompleted(call("GetItem", TelemetryOutcome.SUCCEEDED, 30, 503, 200));
        bridge.callCompleted(call("PutItem", TelemetryOutcome.FAILED, 5, 400));
        assertThat(cloudWatch.requests).isEmpty();

        publisher.close();
        assertThat(publisher.isShutdown()).isTrue();
        assertThat(cloudWatch.closes).hasValue(0);
        assertThat(cloudWatch.requests).isNotEmpty()
                                       .allSatisfy(r -> assertThat(r.namespace()).isEqualTo("Test/Telemetry"));

        Map<String, MetricDatum> getItem = byName(cloudWatch.data(), "GetItem");
        assertThat(getItem.get("ApiCallDuration").dimensions())
            .containsExactly(Dimension.builder().name("ServiceId").value("DynamoDB").build(),
                             Dimension.builder().name("OperationName").value("GetItem").build());
        assertThat(getItem.get("ApiCallDuration").unit()).isEqualTo(StandardUnit.MILLISECONDS);
        assertThat(getItem.get("ApiCallDuration").statisticValues())
            .satisfies(s -> {
                assertThat(s.sampleCount()).isEqualTo(2.0);
                assertThat(s.minimum()).isEqualTo(10.0);
                assertThat(s.maximum()).isEqualTo(60.0);
                assertThat(s.sum()).isEqualTo(70.0);
            });
        assertThat(getItem.get("ApiCallSuccessful").statisticValues().sum()).isEqualTo(2.0);
        assertThat(getItem.get("RetryCount").statisticValues().sum()).isEqualTo(1.0);
        assertThat(getItem.get("RetryCount").unit()).isEqualTo(StandardUnit.NONE);
        // Attempt metrics: three attempts, under the call's dimensions.
        assertThat(getItem.get("ServiceCallDuration").statisticValues().sampleCount()).isEqualTo(3.0);
        // HttpStatusCode is a TRACE metric, above the default INFO level; strings are not uploaded.
        assertThat(getItem).doesNotContainKeys("HttpStatusCode", "ServiceId", "ErrorType", "HttpClientName");

        Map<String, MetricDatum> putItem = byName(cloudWatch.data(), "PutItem");
        assertThat(putItem.get("ApiCallSuccessful").statisticValues().sum()).isEqualTo(0.0);
    }

    @Test
    void levelsCategoriesDetailedMetricsAndDimensionsAreConfigurable() {
        RecordingCloudWatch cloudWatch = new RecordingCloudWatch();
        CloudWatchMetricPublisher publisher = CloudWatchMetricPublisher.builder()
                                                                       .cloudWatchClient(cloudWatch.client)
                                                                       .metricLevel(MetricLevel.TRACE)
                                                                       .metricCategories(MetricCategory.HTTP_CLIENT)
                                                                       .detailedMetrics(TelemetryMetric.HTTP_STATUS_CODE)
                                                                       .dimensions(CoreMetric.OPERATION_NAME)
                                                                       .build();
        TelemetryPublisher bridge = TelemetryPublisher.fromMetricPublisher(publisher);
        bridge.callCompleted(call("GetItem", TelemetryOutcome.SUCCEEDED, 10, 503, 503, 200));
        publisher.close();

        List<MetricDatum> data = cloudWatch.data();
        assertThat(data).extracting(MetricDatum::metricName).containsExactly("HttpStatusCode");
        MetricDatum status = data.get(0);
        assertThat(status.dimensions()).containsExactly(Dimension.builder().name("OperationName").value("GetItem")
                                                                 .build());
        assertThat(status.statisticValues()).isNull();
        Map<Double, Double> counts = new java.util.HashMap<>();
        for (int i = 0; i < status.values().size(); i++) {
            counts.put(status.values().get(i), status.counts().get(i));
        }
        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(503.0, 2.0, 200.0, 1.0));
    }

    @Test
    void requestsStayWithinCloudWatchLimitsAndTheCallCap() {
        RecordingCloudWatch cloudWatch = new RecordingCloudWatch();
        CloudWatchMetricPublisher publisher = CloudWatchMetricPublisher.builder()
                                                                       .cloudWatchClient(cloudWatch.client)
                                                                       .maximumCallsPerUpload(2)
                                                                       .build();
        // 30 operations x 4 reported numeric metrics = 120 data: 6 requests of 20, capped at 2 calls.
        for (int i = 0; i < 30; i++) {
            MetricCollector collector = MetricCollector.create("ApiCall");
            collector.reportMetric(CoreMetric.SERVICE_ID, "DynamoDB");
            collector.reportMetric(CoreMetric.OPERATION_NAME, "Op" + i);
            collector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, true);
            collector.reportMetric(CoreMetric.API_CALL_DURATION, Duration.ofMillis(i));
            collector.reportMetric(CoreMetric.RETRY_COUNT, 0);
            collector.reportMetric(CoreMetric.SERVICE_CALL_DURATION, Duration.ofMillis(i));
            publisher.publish(collector.collect());
        }
        publisher.close();
        assertThat(cloudWatch.requests).hasSize(2)
                                       .allSatisfy(r -> assertThat(r.metricData()).hasSize(20));
    }

    @Test
    void metricsAreUploadedOnTheScheduleAndAnOwnedClientIsNeverClosedByMistake() throws Exception {
        RecordingCloudWatch cloudWatch = new RecordingCloudWatch();
        CloudWatchMetricPublisher publisher = CloudWatchMetricPublisher.builder()
                                                                       .cloudWatchClient(cloudWatch.client)
                                                                       .uploadFrequency(Duration.ofMillis(20))
                                                                       .build();
        TelemetryPublisher.fromMetricPublisher(publisher)
                          .callCompleted(call("GetItem", TelemetryOutcome.SUCCEEDED, 10, 200));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (cloudWatch.requests.isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(cloudWatch.requests).isNotEmpty();
        publisher.close();
        publisher.close();
        assertThat(cloudWatch.closes).hasValue(0);
        // A closed publisher drops further collections without throwing.
        publisher.publish(MetricCollector.create("ApiCall").collect());
    }

    @Test
    void droppedCollectionsWarnOncePerPublisherThroughTheFacadeThenLogAtDebug() {
        java.util.logging.Logger jul = java.util.logging.Logger.getLogger(
            "software.amazon.awssdk.metrics.publishers.cloudwatch");
        List<LogRecord> records = new CopyOnWriteArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Level previousLevel = jul.getLevel();
        boolean previousUseParent = jul.getUseParentHandlers();
        jul.setLevel(Level.FINE);
        jul.setUseParentHandlers(false);
        jul.addHandler(handler);
        try {
            RecordingCloudWatch cloudWatch = new RecordingCloudWatch();
            for (int p = 0; p < 2; p++) {
                CloudWatchMetricPublisher publisher = CloudWatchMetricPublisher.builder()
                                                                               .cloudWatchClient(cloudWatch.client)
                                                                               .build();
                publisher.close();
                publisher.publish(MetricCollector.create("ApiCall").collect());
                publisher.publish(MetricCollector.create("ApiCall").collect());
            }
            List<LogRecord> dropped = records.stream().filter(r -> r.getMessage().contains("have been dropped"))
                                             .toList();
            // One warning per publisher (its own suppression scope), then DEBUG; never INFO (Requirement 21.9).
            assertThat(dropped).extracting(LogRecord::getLevel)
                               .containsExactly(Level.WARNING, Level.FINE, Level.WARNING, Level.FINE);
            assertThat(dropped).allSatisfy(r -> assertThat(r.getThrown())
                .isInstanceOf(java.util.concurrent.RejectedExecutionException.class));
            assertThat(records).extracting(LogRecord::getLevel).doesNotContain(Level.INFO);
        } finally {
            jul.removeHandler(handler);
            jul.setLevel(previousLevel);
            jul.setUseParentHandlers(previousUseParent);
        }
    }

    @Test
    void invalidSettingsAreRejected() {
        RecordingCloudWatch cloudWatch = new RecordingCloudWatch();
        assertThatThrownBy(() -> CloudWatchMetricPublisher.builder().cloudWatchClient(cloudWatch.client)
                                                          .uploadFrequency(Duration.ZERO).build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CloudWatchMetricPublisher.builder().cloudWatchClient(cloudWatch.client)
                                                          .maximumCallsPerUpload(0).build())
            .isInstanceOf(IllegalArgumentException.class);
        CloudWatchMetricPublisher publisher = CloudWatchMetricPublisher.builder().cloudWatchClient(cloudWatch.client)
                                                                       .build();
        assertThatThrownBy(() -> publisher.publish(null)).isInstanceOf(IllegalArgumentException.class);
        publisher.close();
    }
}
