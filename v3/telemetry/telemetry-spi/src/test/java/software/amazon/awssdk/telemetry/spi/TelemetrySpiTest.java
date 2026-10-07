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

package software.amazon.awssdk.telemetry.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.LogCapture;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.telemetry.spi.internal.LoggingTelemetryFailureListener;

/**
 * The telemetry SPI's records, configuration, default failure channel and V2 {@code MetricPublisher} bridge
 * (Requirements 21.2-21.4, 21.12, 21.15).
 */
class TelemetrySpiTest {
    private static AttemptRecord attempt(int number, TelemetryOutcome outcome, Integer status, String errorType) {
        return new AttemptRecord("DynamoDB", "GetItem", number, outcome, "UrlConnection", "standard",
                                 Duration.ofMillis(10L * number),
                                 status == null ? OptionalInt.empty() : OptionalInt.of(status),
                                 Optional.ofNullable(errorType));
    }

    @Test
    void recordsRequireEveryDimensionAndConsistentAttempts() {
        AttemptRecord first = attempt(1, TelemetryOutcome.FAILED, 503, "ServerError");
        CallRecord call = new CallRecord("DynamoDB", "GetItem", TelemetryOutcome.SUCCEEDED, 2, "UrlConnection",
                                         "standard", Duration.ofMillis(40), Optional.empty(),
                                         List.of(first, attempt(2, TelemetryOutcome.SUCCEEDED, 200, null)));
        assertThat(call.attempts()).hasSize(2).first().isEqualTo(first);
        assertThatThrownBy(() -> call.attempts().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new CallRecord("DynamoDB", "GetItem", TelemetryOutcome.FAILED, 2, "UrlConnection",
                                                "standard", Duration.ZERO, Optional.of("IO"), List.of(first)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("attemptCount 2");
        assertThatThrownBy(() -> attempt(0, TelemetryOutcome.FAILED, null, "IO"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AttemptRecord("DynamoDB", "GetItem", 1, TelemetryOutcome.FAILED, null,
                                                   "standard", Duration.ZERO, OptionalInt.empty(), Optional.empty()))
            .isInstanceOf(NullPointerException.class).hasMessage("httpImplementation");
    }

    @Test
    void configurationKeepsPublisherOrderAndDefaultsToTheLoggingChannel() {
        TelemetryPublisher a = call -> { };
        TelemetryPublisher b = call -> { };
        TelemetryConfiguration configuration = TelemetryConfiguration.builder().addPublisher(a).addPublisher(b).build();
        assertThat(configuration.publishers()).containsExactly(a, b);
        assertThat(configuration.isEmpty()).isFalse();
        assertThat(configuration.failureListener()).isInstanceOf(LoggingTelemetryFailureListener.class);
        assertThat(TelemetryConfiguration.builder().build().isEmpty()).isTrue();
        TelemetryFailureListener listener = failure -> { };
        TelemetryConfiguration rebuilt = configuration.toBuilder().failureListener(listener).build();
        assertThat(rebuilt.publishers()).containsExactly(a, b);
        assertThat(rebuilt.failureListener()).isSameAs(listener);
        assertThat(configuration.toString()).startsWith("TelemetryConfiguration(publishers=[");
        assertThat(TelemetryOptions.TELEMETRY.valueType()).isEqualTo(TelemetryConfiguration.class);
    }

    @Test
    void theLoggingChannelWarnsOncePerPublisherAndCallbackThroughTheFacade() {
        TelemetryPublisher publisher = call -> { };
        RuntimeException cause = new IllegalStateException("exporter down");
        try (LogCapture log = LogCapture.of(LoggingTelemetryFailureListener.LOGGER_NAME, Level.FINE)) {
            TelemetryFailureListener listener = TelemetryFailureListener.logging();
            for (int i = 0; i < 3; i++) {
                listener.telemetryFailed(new TelemetryFailure(publisher, TelemetryFailure.Callback.CALL_COMPLETED,
                                                              "DynamoDB", "GetItem", cause));
            }
            listener.telemetryFailed(new TelemetryFailure(publisher, TelemetryFailure.Callback.ATTEMPT_COMPLETED,
                                                          "DynamoDB", "GetItem", cause));
            assertThat(log.levels()).containsExactly(Level.WARNING, Level.FINE, Level.FINE, Level.WARNING);
            LogRecord first = log.records().get(0);
            assertThat(first.getThrown()).isSameAs(cause);
            assertThat(first.getMessage()).contains(publisher.getClass().getName(), "CALL_COMPLETED",
                                                    "DynamoDB.GetItem", "result is unchanged");
            // Each listener is its own suppression scope.
            TelemetryFailureListener.logging().telemetryFailed(
                new TelemetryFailure(publisher, TelemetryFailure.Callback.CALL_COMPLETED, "DynamoDB", "GetItem",
                                     cause));
            assertThat(log.levels()).hasSize(5).last().isEqualTo(Level.WARNING);
        }
    }

    @Test
    void metricPublishersReceiveOneV2ShapedCollectionPerCall() {
        List<MetricCollection> published = new ArrayList<>();
        MetricPublisher v2 = new MetricPublisher() {
            @Override
            public void publish(MetricCollection metricCollection) {
                published.add(metricCollection);
            }

            @Override
            public void close() {
            }
        };
        TelemetryPublisher bridge = TelemetryConfiguration.builder().addMetricPublisher(v2).build().publishers()
                                                          .get(0);
        bridge.attemptCompleted(attempt(1, TelemetryOutcome.FAILED, 503, "ServerError"));
        assertThat(published).isEmpty();
        bridge.callCompleted(new CallRecord("DynamoDB", "GetItem", TelemetryOutcome.FAILED, 2, "UrlConnection",
                                            "adaptive", Duration.ofMillis(70), Optional.of("IO"),
                                            List.of(attempt(1, TelemetryOutcome.FAILED, 503, "ServerError"),
                                                    attempt(2, TelemetryOutcome.FAILED, null, "IO"))));
        assertThat(published).hasSize(1);
        MetricCollection call = published.get(0);
        assertThat(call.name()).isEqualTo("ApiCall");
        assertThat(call.metricValues(CoreMetric.SERVICE_ID)).containsExactly("DynamoDB");
        assertThat(call.metricValues(CoreMetric.OPERATION_NAME)).containsExactly("GetItem");
        assertThat(call.metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(false);
        assertThat(call.metricValues(CoreMetric.API_CALL_DURATION)).containsExactly(Duration.ofMillis(70));
        assertThat(call.metricValues(CoreMetric.RETRY_COUNT)).containsExactly(1);
        assertThat(call.metricValues(CoreMetric.ERROR_TYPE)).containsExactly("IO");
        assertThat(call.metricValues(TelemetryMetric.RETRY_MODE)).containsExactly("adaptive");
        List<MetricCollection> attempts = call.childrenWithName("ApiCallAttempt").toList();
        assertThat(attempts).hasSize(2);
        assertThat(attempts.get(0).metricValues(TelemetryMetric.HTTP_STATUS_CODE)).containsExactly(503);
        assertThat(attempts.get(0).metricValues(CoreMetric.ERROR_TYPE)).containsExactly("ServerError");
        assertThat(attempts.get(0).metricValues(TelemetryMetric.HTTP_CLIENT_NAME)).containsExactly("UrlConnection");
        assertThat(attempts.get(1).metricValues(TelemetryMetric.HTTP_STATUS_CODE)).isEmpty();
        assertThat(attempts.get(1).metricValues(CoreMetric.SERVICE_CALL_DURATION))
            .containsExactly(Duration.ofMillis(20));
        assertThat(TelemetryMetric.HTTP_CLIENT_NAME.name()).isEqualTo("HttpClientName");
        assertThat(TelemetryMetric.HTTP_STATUS_CODE.name()).isEqualTo("HttpStatusCode");
    }
}
