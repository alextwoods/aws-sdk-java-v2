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

package software.amazon.awssdk.utils;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The logging facade of every V3 runtime module (Requirements 15.8, 21.8, 21.9): the V2 {@code Logger} names
 * ({@code loggerFor}, {@code debug}, {@code trace}, {@code warn}, {@code error}, {@code isLoggingLevelEnabled}) over
 * the JDK platform logging API ({@link System.Logger}), so the SDK requires no application logging implementation
 * and exposes no third-party logging type.
 *
 * <p>Without configuration, platform logging writes to {@code java.util.logging}, whose default level shows
 * {@code WARN}/{@code ERROR} and hides {@code DEBUG}/{@code TRACE}. Applications route SDK logs to their logging
 * implementation with that implementation's platform-logging bridge, for example
 * {@code org.slf4j:slf4j-jdk-platform-logging} for SLF4J 2 or {@code org.apache.logging.log4j:log4j-jpl} for Log4j 2;
 * logger names are unchanged from V2.
 *
 * <p>Differences from V2: there is no {@code info} level (runtime artifacts emit no INFO events; use {@code debug}),
 * and no SLF4J-typed method ({@code logger()}, {@code isLoggingLevelEnabled(Level)}, {@code log(Level, Supplier)}).
 *
 * <p>Every message is a {@link Supplier} evaluated only when its level is enabled, so building and redacting a message
 * ({@link Redaction}) costs nothing when the level is disabled. Messages must never contain credential values,
 * authorization or cookie headers, tokens or {@code @sensitive} member values; render them with {@link Redaction}.
 * Failures returned to the caller must not also be logged at {@code WARN} or {@code ERROR}, and repeated benign
 * warnings go through a {@link WarningSuppression} scope.
 *
 * <p>Thread safe; declare instances as {@code private static final} fields.
 */
public final class Logger {
    private final System.Logger log;

    private Logger(System.Logger log) {
        this.log = log;
    }

    /** A logger named after {@code clz}, as V2 {@code Logger.loggerFor(Class)}. */
    public static Logger loggerFor(Class<?> clz) {
        return loggerFor(Objects.requireNonNull(clz, "clz").getName());
    }

    /** A logger with the name {@code name}, as V2 {@code Logger.loggerFor(String)}. */
    public static Logger loggerFor(String name) {
        return new Logger(System.getLogger(Objects.requireNonNull(name, "name")));
    }

    /** The logger's name. */
    public String name() {
        return log.getName();
    }

    /** Logs the supplied message at ERROR, if enabled. Only for failures the caller does not receive. */
    public void error(Supplier<String> msg) {
        log(System.Logger.Level.ERROR, msg, null);
    }

    /** Logs the supplied message and {@code throwable} at ERROR, if enabled. */
    public void error(Supplier<String> msg, Throwable throwable) {
        log(System.Logger.Level.ERROR, msg, throwable);
    }

    /** Logs the supplied message at WARN, if enabled. Only for conditions the caller does not receive. */
    public void warn(Supplier<String> msg) {
        log(System.Logger.Level.WARNING, msg, null);
    }

    /** Logs the supplied message and {@code throwable} at WARN, if enabled. */
    public void warn(Supplier<String> msg, Throwable throwable) {
        log(System.Logger.Level.WARNING, msg, throwable);
    }

    /** Logs the supplied message at DEBUG, if enabled. */
    public void debug(Supplier<String> msg) {
        log(System.Logger.Level.DEBUG, msg, null);
    }

    /** Logs the supplied message and {@code throwable} at DEBUG, if enabled. */
    public void debug(Supplier<String> msg, Throwable throwable) {
        log(System.Logger.Level.DEBUG, msg, throwable);
    }

    /** Logs the supplied message at TRACE, if enabled. */
    public void trace(Supplier<String> msg) {
        log(System.Logger.Level.TRACE, msg, null);
    }

    /** Logs the supplied message and {@code throwable} at TRACE, if enabled. */
    public void trace(Supplier<String> msg, Throwable throwable) {
        log(System.Logger.Level.TRACE, msg, throwable);
    }

    /** Whether DEBUG is enabled. */
    public boolean isDebugEnabled() {
        return log.isLoggable(System.Logger.Level.DEBUG);
    }

    /** Whether TRACE is enabled. */
    public boolean isTraceEnabled() {
        return log.isLoggable(System.Logger.Level.TRACE);
    }

    /** Whether WARN is enabled. */
    public boolean isWarnEnabled() {
        return log.isLoggable(System.Logger.Level.WARNING);
    }

    /**
     * Whether the level named {@code logLevel} ({@code trace}, {@code debug}, {@code info}, {@code warn} or
     * {@code error}, ignoring case) is enabled in the logging backend, as V2 {@code isLoggingLevelEnabled(String)}.
     *
     * @throws IllegalArgumentException if {@code logLevel} names no level
     */
    public boolean isLoggingLevelEnabled(String logLevel) {
        String lowerLogLevel = logLevel == null ? null : logLevel.toLowerCase(Locale.ENGLISH);
        if (lowerLogLevel == null) {
            throw new IllegalArgumentException("Unknown log level: null");
        }
        System.Logger.Level level = switch (lowerLogLevel) {
            case "trace" -> System.Logger.Level.TRACE;
            case "debug" -> System.Logger.Level.DEBUG;
            case "info" -> System.Logger.Level.INFO;
            case "warn" -> System.Logger.Level.WARNING;
            case "error" -> System.Logger.Level.ERROR;
            default -> throw new IllegalArgumentException("Unknown log level: " + lowerLogLevel);
        };
        return log.isLoggable(level);
    }

    private void log(System.Logger.Level level, Supplier<String> msg, Throwable throwable) {
        if (!log.isLoggable(level)) {
            return;
        }
        String message = msg.get();
        if (throwable == null) {
            log.log(level, message);
        } else {
            log.log(level, message, throwable);
        }
    }

    @Override
    public String toString() {
        return "Logger(" + log.getName() + ")";
    }
}
