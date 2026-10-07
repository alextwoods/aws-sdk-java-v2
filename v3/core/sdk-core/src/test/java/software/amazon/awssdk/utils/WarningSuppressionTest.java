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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.junit.jupiter.api.Test;

/** At most one equivalent benign warning per category, operation and suppression scope (Requirement 21.12). */
class WarningSuppressionTest {
    private static final String NAME = "software.amazon.awssdk.utils.WarningSuppressionTest.subject";

    @Test
    void equivalentWarningsAreEmittedOncePerCategoryOperationAndScope() {
        try (LogCapture capture = LogCapture.of(NAME, Level.ALL)) {
            Logger logger = Logger.loggerFor(NAME);
            WarningSuppression scope = WarningSuppression.create();
            IllegalStateException cause = new IllegalStateException();
            assertTrue(scope.warn(logger, "fallback", "GetItem", () -> "first", cause));
            assertFalse(scope.warn(logger, "fallback", "GetItem", () -> "repeat"));
            assertTrue(scope.warn(logger, "fallback", "PutItem", () -> "other operation"));
            assertTrue(scope.warn(logger, "cleanup", "GetItem", () -> "other category"));
            assertTrue(scope.warn(logger, "cleanup", null, () -> "no operation"));
            assertFalse(scope.warn(logger, "cleanup", null, () -> "no operation again"));
            WarningSuppression other = WarningSuppression.create();
            assertTrue(other.warn(logger, "fallback", "GetItem", () -> "other scope"));
            assertEquals(List.of("first", "other operation", "other category", "no operation", "other scope"),
                         capture.messages());
            assertSame(cause, capture.records().get(0).getThrown());
            assertTrue(scope.emitted("fallback", "GetItem"));
            assertFalse(scope.emitted("fallback", "DeleteItem"));
        }
    }

    @Test
    void nothingIsBuiltOrRecordedWhileWarnIsDisabled() {
        AtomicInteger built = new AtomicInteger();
        try (LogCapture capture = LogCapture.of(NAME, Level.SEVERE)) {
            WarningSuppression scope = WarningSuppression.create();
            assertFalse(scope.warn(Logger.loggerFor(NAME), "fallback", "op", () -> "w" + built.incrementAndGet()));
            assertEquals(0, built.get());
            assertFalse(scope.emitted("fallback", "op"));
            assertEquals(List.of(), capture.records());
        }
    }

    @Test
    void concurrentEquivalentWarningsEmitExactlyOneEvent() throws Exception {
        int threads = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try (LogCapture capture = LogCapture.of(NAME, Level.ALL)) {
            Logger logger = Logger.loggerFor(NAME);
            WarningSuppression scope = WarningSuppression.create();
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return scope.warn(logger, "fallback", "op", () -> "once");
                }));
            }
            start.countDown();
            int emitted = 0;
            for (Future<Boolean> result : results) {
                emitted += result.get(10, TimeUnit.SECONDS) ? 1 : 0;
            }
            assertEquals(1, emitted);
            assertEquals(List.of("once"), capture.messages());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void aFullScopeSuppressesNewKeysInsteadOfGrowing() {
        try (LogCapture capture = LogCapture.of(NAME, Level.ALL)) {
            Logger logger = Logger.loggerFor(NAME);
            WarningSuppression scope = WarningSuppression.create();
            for (int i = 0; i < WarningSuppression.MAX_KEYS; i++) {
                assertTrue(scope.warn(logger, "c", "op" + i, () -> "w"));
            }
            assertFalse(scope.warn(logger, "c", "one-more", () -> "w"));
            assertEquals(WarningSuppression.MAX_KEYS, capture.records().size());
        }
    }
}
