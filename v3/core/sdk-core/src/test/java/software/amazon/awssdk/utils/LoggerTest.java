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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;

/**
 * The logging facade (Requirements 15.8, 21.8, 21.9): the V2 {@code Logger} names over JDK platform logging, lazy
 * messages, no INFO level and no third-party logging types.
 */
class LoggerTest {
    private static final String NAME = "software.amazon.awssdk.utils.LoggerTest.subject";

    @Test
    void loggerForNamesTheLoggerAfterTheClassOrName() {
        assertEquals(LoggerTest.class.getName(), Logger.loggerFor(LoggerTest.class).name());
        assertEquals("custom.name", Logger.loggerFor("custom.name").name());
        assertThrows(NullPointerException.class, () -> Logger.loggerFor((String) null));
    }

    @Test
    void messagesAreNotBuiltWhileTheLevelIsDisabled() {
        AtomicInteger built = new AtomicInteger();
        try (LogCapture capture = LogCapture.of(NAME, Level.WARNING)) {
            Logger logger = Logger.loggerFor(NAME);
            logger.debug(() -> "debug " + built.incrementAndGet());
            logger.trace(() -> "trace " + built.incrementAndGet(), new IllegalStateException());
            assertEquals(0, built.get());
            assertFalse(logger.isDebugEnabled());
            assertFalse(logger.isTraceEnabled());
            assertTrue(logger.isWarnEnabled());
            assertEquals(List.of(), capture.records());
        }
    }

    @Test
    void eachLevelWritesToTheMatchingPlatformLevelWithItsThrowable() {
        IllegalStateException cause = new IllegalStateException("cause");
        try (LogCapture capture = LogCapture.of(NAME, Level.ALL)) {
            Logger logger = Logger.loggerFor(NAME);
            logger.trace(() -> "t");
            logger.debug(() -> "d", cause);
            logger.warn(() -> "w");
            logger.error(() -> "e", cause);
            List<LogRecord> records = capture.records();
            assertEquals(List.of("t", "d", "w", "e"), capture.messages());
            assertEquals(List.of(Level.FINER, Level.FINE, Level.WARNING, Level.SEVERE),
                         records.stream().map(LogRecord::getLevel).toList());
            assertSame(cause, records.get(1).getThrown());
            assertSame(cause, records.get(3).getThrown());
            assertNull(records.get(2).getThrown());
        }
    }

    @Test
    void isLoggingLevelEnabledKeepsTheV2LevelNames() {
        try (LogCapture capture = LogCapture.of(NAME, Level.INFO)) {
            Logger logger = Logger.loggerFor(NAME);
            assertFalse(logger.isLoggingLevelEnabled("TRACE"));
            assertFalse(logger.isLoggingLevelEnabled("debug"));
            assertTrue(logger.isLoggingLevelEnabled("Info"));
            assertTrue(logger.isLoggingLevelEnabled("warn"));
            assertTrue(logger.isLoggingLevelEnabled("error"));
            IllegalArgumentException unknown =
                assertThrows(IllegalArgumentException.class, () -> logger.isLoggingLevelEnabled("FATAL"));
            assertEquals("Unknown log level: fatal", unknown.getMessage());
            assertThrows(IllegalArgumentException.class, () -> logger.isLoggingLevelEnabled(null));
        }
    }

    @Test
    void theFacadeHasNoInfoLevelAndExposesNoThirdPartyLoggingType() {
        List<Method> api = Arrays.stream(Logger.class.getDeclaredMethods())
                                 .filter(m -> Modifier.isPublic(m.getModifiers()))
                                 .toList();
        assertTrue(api.stream().noneMatch(m -> m.getName().equals("info") || m.getName().equals("log")
                                               || m.getName().equals("logger")),
                   api.toString());
        for (Method method : api) {
            assertFalse(method.getReturnType().getName().startsWith("org."), method.toString());
            for (Class<?> parameter : method.getParameterTypes()) {
                assertFalse(parameter.getName().startsWith("org."), method.toString());
            }
        }
    }
}
