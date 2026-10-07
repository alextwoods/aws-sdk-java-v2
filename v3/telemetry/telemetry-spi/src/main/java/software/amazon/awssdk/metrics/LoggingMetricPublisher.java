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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.utils.Logger;

/**
 * A {@link MetricPublisher} that logs every published collection, for debugging (V2-compatible).
 *
 * <p>ADAPTED from V2, which logs through SLF4J: this publisher logs through the SDK logging facade
 * ({@link Logger}, over JDK platform logging) under the V2 logger name
 * {@code software.amazon.awssdk.metrics.LoggingMetricPublisher}, so V3 declares no logging dependency and exposes no
 * logging type (Requirements 15.8, 21.8). Because V3 runtime artifacts emit no INFO-level events (Requirement 21.9), it
 * logs at {@link LogLevel#DEBUG} or {@link LogLevel#TRACE} only: {@link #create()} logs at {@code DEBUG} instead of
 * V2's {@code INFO}, and the V2 {@code create(org.slf4j.event.Level, Format)} overload is replaced by
 * {@link #create(LogLevel, Format)}.
 */
public final class LoggingMetricPublisher implements MetricPublisher {
    /** How each collection is rendered. */
    public enum Format {
        /** One line per collection. */
        PLAIN,
        /** A box per collection and child, indented by depth; several lines per collection. */
        PRETTY
    }

    /** The level the collections are logged at; V3 runtime artifacts log nothing at INFO or above for metrics. */
    public enum LogLevel {
        /** {@code DEBUG}, the default. */
        DEBUG,
        /** {@code TRACE}. */
        TRACE
    }

    private static final Logger LOGGER = Logger.loggerFor(LoggingMetricPublisher.class);
    private static final int PRETTY_INDENT_SIZE = 4;

    private final Logger logger;
    private final LogLevel logLevel;
    private final Format format;

    private LoggingMetricPublisher(Logger logger, LogLevel logLevel, Format format) {
        this.logger = logger;
        this.logLevel = Objects.requireNonNull(logLevel, "logLevel");
        this.format = Objects.requireNonNull(format, "format");
    }

    /** A publisher logging {@link Format#PLAIN} collections at {@link LogLevel#DEBUG}. */
    public static LoggingMetricPublisher create() {
        return new LoggingMetricPublisher(LOGGER, LogLevel.DEBUG, Format.PLAIN);
    }

    /** A publisher logging collections at {@code logLevel} in {@code format}. */
    public static LoggingMetricPublisher create(LogLevel logLevel, Format format) {
        return new LoggingMetricPublisher(LOGGER, logLevel, format);
    }

    /** {@link #create(LogLevel, Format)} through {@code logger}; for tests. */
    static LoggingMetricPublisher create(Logger logger, LogLevel logLevel, Format format) {
        return new LoggingMetricPublisher(Objects.requireNonNull(logger, "logger"), logLevel, format);
    }

    @Override
    public void publish(MetricCollection metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("metricCollection must not be null");
        }
        if (!enabled()) {
            return;
        }
        if (format == Format.PLAIN) {
            log("Metrics published: " + metrics);
        } else {
            logPretty(Integer.toHexString(metrics.hashCode()), metrics, 0);
        }
    }

    private void logPretty(String guid, MetricCollection metrics, int indent) {
        List<String> values = new ArrayList<>();
        metrics.forEach(m -> values.add(m.metric().name() + "=" + m.value()));
        int maxLen = 0;
        for (String value : values) {
            maxLen = Math.max(maxLen, value.length());
        }
        String prefix = "[" + guid + "]" + " ".repeat(indent);
        log(prefix + " " + metrics.name());
        log(prefix + " ┌" + "─".repeat(maxLen + 2) + "┐");
        for (String value : values) {
            log(prefix + " │ " + value + " ".repeat(maxLen - value.length()) + " │");
        }
        log(prefix + " └" + "─".repeat(maxLen + 2) + "┘");
        for (MetricCollection child : metrics.children()) {
            logPretty(guid, child, indent + PRETTY_INDENT_SIZE);
        }
    }

    private boolean enabled() {
        return logLevel == LogLevel.DEBUG ? logger.isDebugEnabled() : logger.isTraceEnabled();
    }

    private void log(String message) {
        if (logLevel == LogLevel.DEBUG) {
            logger.debug(() -> message);
        } else {
            logger.trace(() -> message);
        }
    }

    @Override
    public void close() {
    }
}
