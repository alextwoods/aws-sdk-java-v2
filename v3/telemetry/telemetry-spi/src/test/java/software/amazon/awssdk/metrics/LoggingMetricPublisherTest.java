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

package software.amazon.awssdk.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.logging.Level;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.utils.Logger;

/**
 * {@link LoggingMetricPublisher}: the V2 plain and pretty formats through the SDK logging facade, at DEBUG by default
 * and never at INFO (Requirement 21.9).
 */
class LoggingMetricPublisherTest {
    private static final String V2_LOGGER_NAME = "software.amazon.awssdk.metrics.LoggingMetricPublisher";

    private static MetricCollection collection() {
        MetricCollector call = MetricCollector.create("ApiCall");
        call.reportMetric(CoreMetric.SERVICE_ID, "DynamoDB");
        call.reportMetric(CoreMetric.RETRY_COUNT, 0);
        call.createChild("ApiCallAttempt").reportMetric(CoreMetric.ERROR_TYPE, "IO");
        return call.collect();
    }

    @Test
    void theDefaultPublisherLogsOnePlainLineAtDebugUnderTheV2LoggerName() {
        try (LogCapture log = LogCapture.of(V2_LOGGER_NAME, Level.ALL)) {
            LoggingMetricPublisher publisher = LoggingMetricPublisher.create();
            publisher.publish(collection());
            assertThat(log.levels()).containsExactly(Level.FINE);
            assertThat(log.messages().get(0)).startsWith("Metrics published: MetricCollection(name=ApiCall")
                                             .contains("ServiceId", "DynamoDB", "ApiCallAttempt", "ErrorType");
            publisher.close();
        }
    }

    @Test
    void prettyFormatDrawsABoxPerCollectionIndentedByDepthAtTheConfiguredLevel() {
        String name = "test.LoggingMetricPublisherTest.pretty";
        try (LogCapture log = LogCapture.of(name, Level.ALL)) {
            LoggingMetricPublisher.create(Logger.loggerFor(name), LoggingMetricPublisher.LogLevel.TRACE,
                                          LoggingMetricPublisher.Format.PRETTY)
                                  .publish(collection());
            List<String> lines = log.messages();
            assertThat(lines).hasSize(9);
            assertThat(log.levels()).containsOnly(Level.FINER);
            assertThat(lines.get(0)).endsWith(" ApiCall");
            assertThat(lines.get(1)).contains("┌").endsWith("┐");
            assertThat(lines.get(2)).contains("│ ServiceId=DynamoDB │");
            assertThat(lines.get(3)).contains("│ RetryCount=0       │");
            assertThat(lines.get(5)).endsWith("     ApiCallAttempt");
            assertThat(lines.get(7)).contains("│ ErrorType=IO │");
            String guid = lines.get(0).substring(0, lines.get(0).indexOf(']') + 1);
            assertThat(lines).allMatch(line -> line.startsWith(guid));
        }
    }

    @Test
    void disabledLevelsLogNothingAndInvalidArgumentsAreRejected() {
        String name = "test.LoggingMetricPublisherTest.disabled";
        try (LogCapture log = LogCapture.of(name, Level.FINE)) {
            LoggingMetricPublisher publisher =
                LoggingMetricPublisher.create(Logger.loggerFor(name), LoggingMetricPublisher.LogLevel.TRACE,
                                              LoggingMetricPublisher.Format.PRETTY);
            publisher.publish(collection());
            assertThat(log.records()).isEmpty();
            assertThatThrownBy(() -> publisher.publish(null)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(LoggingMetricPublisher.create(LoggingMetricPublisher.LogLevel.DEBUG,
                                                 LoggingMetricPublisher.Format.PLAIN)).isNotNull();
        assertThatThrownBy(() -> LoggingMetricPublisher.create(null, LoggingMetricPublisher.Format.PLAIN))
            .isInstanceOf(NullPointerException.class);
        // No level of the publisher maps to INFO or above (Requirement 21.9).
        assertThat(LoggingMetricPublisher.LogLevel.values()).containsExactly(LoggingMetricPublisher.LogLevel.DEBUG,
                                                                              LoggingMetricPublisher.LogLevel.TRACE);
    }
}
