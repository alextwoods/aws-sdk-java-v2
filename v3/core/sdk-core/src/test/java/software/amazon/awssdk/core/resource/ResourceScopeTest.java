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

package software.amazon.awssdk.core.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkCloseException;

class ResourceScopeTest {

    @Test
    void closesEverySdkOwnedResourceOnceAndNoCallerOwnedResource() {
        List<String> closes = Collections.synchronizedList(new ArrayList<>());
        Recording first = new Recording("first", closes);
        Recording second = new Recording("second", closes);
        Recording borrowed = new Recording("borrowed", closes);
        ResourceScope scope = ResourceScope.of(List.of(ManagedResource.sdkOwned(first),
                                                       ManagedResource.callerOwned(borrowed),
                                                       ManagedResource.sdkOwned(second),
                                                       ManagedResource.sdkOwned("not closeable")));

        assertFalse(scope.isClosed());
        scope.close();
        scope.close();
        scope.close();

        assertTrue(scope.isClosed());
        assertEquals(List.of("second", "first"), closes, "reverse registration order, once each");
        assertEquals(0, borrowed.closes.get());
    }

    @Test
    void aliasesAreDeduplicatedByIdentityNotEquals() {
        List<String> closes = new ArrayList<>();
        Recording a = new Recording("same", closes);
        Recording equalButDistinct = new Recording("same", closes);
        ResourceScope scope = ResourceScope.of(List.of(ManagedResource.sdkOwned(a),
                                                       ManagedResource.sdkOwned(a),
                                                       ManagedResource.sdkOwned(equalButDistinct)));

        assertEquals(a, equalButDistinct);
        assertEquals(2, scope.resources().size());
        scope.close();
        assertEquals(1, a.closes.get());
        assertEquals(1, equalButDistinct.closes.get());
    }

    @Test
    void callerOwnershipOfAnAliasWins() {
        Recording shared = new Recording("shared", new ArrayList<>());
        ResourceScope scope = ResourceScope.of(List.of(ManagedResource.sdkOwned(shared),
                                                       ManagedResource.callerOwned(shared)));

        assertEquals(Optional.of(ResourceOwnership.CALLER_OWNED), scope.ownership(shared));
        assertEquals(Optional.empty(), scope.ownership(new Object()));
        scope.close();
        assertEquals(0, shared.closes.get());
    }

    @Test
    void attemptsEveryResourceAndAggregatesEveryFailureOnce() {
        List<String> closes = new ArrayList<>();
        IllegalStateException firstFailure = new IllegalStateException("one");
        Exception secondFailure = new Exception("two");
        Recording ok = new Recording("ok", closes);
        ResourceScope scope = ResourceScope.of(List.of(
            ManagedResource.sdkOwned(failing("a", closes, secondFailure)),
            ManagedResource.sdkOwned(ok),
            ManagedResource.sdkOwned(failing("b", closes, firstFailure))));

        SdkCloseException e = assertThrows(SdkCloseException.class, scope::close);

        assertEquals(List.of("b", "ok", "a"), closes);
        assertEquals(List.of(firstFailure, secondFailure), e.failures());
        assertEquals(2, e.getSuppressed().length);
        scope.close();
        assertEquals(List.of("b", "ok", "a"), closes, "a failed close is not retried");
    }

    @Test
    void interruptedCloseIsReportedAndTheInterruptIsRestored() {
        InterruptedException interrupted = new InterruptedException("stop");
        List<String> closes = new ArrayList<>();
        ResourceScope scope = ResourceScope.of(List.of(ManagedResource.sdkOwned(new Recording("after", closes)),
                                                       ManagedResource.sdkOwned(failing("x", closes, interrupted))));
        try {
            SdkCloseException e = assertThrows(SdkCloseException.class, scope::close);
            assertSame(interrupted, e.failures().get(0));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(List.of("x", "after"), closes);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void reentrantCloseFromAResourceReturnsImmediately() {
        AtomicInteger inner = new AtomicInteger();
        ResourceScope[] holder = new ResourceScope[1];
        AutoCloseable reentrant = () -> {
            inner.incrementAndGet();
            holder[0].close();
        };
        holder[0] = ResourceScope.of(List.of(ManagedResource.sdkOwned(reentrant)));

        holder[0].close();
        assertEquals(1, inner.get());
    }

    @Test
    void concurrentClosesAttemptEachResourceOnceAndWaitForTheFirst() throws Exception {
        int threads = 8;
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger slowCloses = new AtomicInteger();
        List<Recording> resources = new ArrayList<>();
        List<ManagedResource<?>> managed = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Recording r = new Recording("r" + i, Collections.synchronizedList(new ArrayList<>()));
            resources.add(r);
            managed.add(ManagedResource.sdkOwned(r));
        }
        AutoCloseable slow = () -> {
            slowCloses.incrementAndGet();
            release.await(10, TimeUnit.SECONDS);
            throw new IllegalStateException("slow failed");
        };
        managed.add(ManagedResource.sdkOwned(slow));
        ResourceScope scope = ResourceScope.of(managed);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CyclicBarrier start = new CyclicBarrier(threads);
            List<Future<Throwable>> results = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        scope.close();
                        return null;
                    } catch (SdkCloseException e) {
                        return e;
                    }
                }));
            }
            Thread.sleep(100);
            // Every thread is still inside close(): the first is closing the slow resource, the rest wait for it.
            for (Future<Throwable> result : results) {
                assertFalse(result.isDone());
            }
            release.countDown();

            int reported = 0;
            for (Future<Throwable> result : results) {
                Throwable thrown = result.get(10, TimeUnit.SECONDS);
                if (thrown != null) {
                    reported++;
                    assertEquals(1, ((SdkCloseException) thrown).failures().size());
                }
            }
            assertEquals(1, reported, "exactly one caller reports the failures");
            assertEquals(1, slowCloses.get());
            for (Recording r : resources) {
                assertEquals(1, r.closes.get(), r.name);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void toStringNamesResourceTypesOnly() {
        ResourceScope scope = ResourceScope.of(List.of(ManagedResource.callerOwned(new Recording("secret-name",
                                                                                                 new ArrayList<>()))));
        assertFalse(scope.toString().contains("secret-name"), scope.toString());
        assertTrue(scope.toString().contains("CALLER_OWNED"), scope.toString());
    }

    private static AutoCloseable failing(String name, List<String> closes, Exception failure) {
        return () -> {
            closes.add(name);
            throw failure;
        };
    }

    /** A closeable counting its closes; equal to every other recording with the same name. */
    private static final class Recording implements AutoCloseable {
        private final String name;
        private final List<String> log;
        private final AtomicInteger closes = new AtomicInteger();

        private Recording(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public void close() {
            closes.incrementAndGet();
            log.add(name);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Recording that && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
