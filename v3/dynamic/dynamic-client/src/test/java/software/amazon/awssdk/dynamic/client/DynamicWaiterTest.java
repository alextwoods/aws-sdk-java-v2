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

package software.amazon.awssdk.dynamic.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.dynamic.client.ControlledHttp.TIMEOUT;
import static software.amazon.awssdk.dynamic.client.ControlledHttp.await;
import static software.amazon.awssdk.dynamic.client.TestSupport.doc;
import static software.amazon.awssdk.dynamic.client.TestSupport.restJson;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.dynamic.client.ControlledHttp.Exchange;
import software.amazon.awssdk.dynamic.client.ControlledHttp.HeldScheduler;
import software.amazon.awssdk.dynamic.client.ControlledHttp.Reply;
import software.amazon.awssdk.dynamic.client.TestSupport.TestTransport;
import software.amazon.awssdk.dynamic.model.DynamicModelException;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * Dynamic waiters (Requirements 14.13, 14.15): modeled acceptors (output, inputOutput, errorType and success
 * matchers, filter projections) and delays on the sdk-core waiter runtime generated waiters use; sync waits on the
 * calling thread; async waits that never block the caller, schedule every delay on the configured scheduler (by
 * default the client's retry scheduler) and cancel their pending poll or delay through the shared pipeline.
 */
class DynamicWaiterTest {
    private static final WaiterOverrideConfiguration FAST = WaiterOverrideConfiguration.builder()
                                                                                       .minDelay(Duration.ofMillis(1))
                                                                                       .maxDelay(Duration.ofMillis(2))
                                                                                       .build();
    private static final List<String> WAITERS = List.of("ItemEchoed", "ItemReady", "ItemTagged");

    private static Document id(String id) {
        return doc(Map.of("id", Document.of(id)));
    }

    private static SdkHttpResponse missing() {
        return restJson(404, "{\"message\":\"gone\"}", "X-Amzn-ErrorType", "ItemMissing");
    }

    /** A sync transport answering with {@code replies} in order, then repeating the last one. */
    private static TestTransport replying(SdkHttpResponse... replies) {
        Queue<SdkHttpResponse> queue = new ConcurrentLinkedQueue<>(List.of(replies));
        SdkHttpResponse last = replies[replies.length - 1];
        return TestTransport.sync(r -> {
            SdkHttpResponse next = queue.poll();
            return next != null ? next : last;
        });
    }

    // -------------------------------------------------------------------------------------------------------------
    // Sync

    @Test
    void aSyncWaiterPollsUntilTheSuccessAcceptorMatches() {
        TestTransport transport = replying(restJson(200, "{\"value\":\"PENDING\"}"), missing(),
                                           restJson(200, "{\"value\":\"READY\"}"));
        try (DynamicClient client = TestSupport.mediaClient(transport).build()) {
            DynamicWaiter waiter = client.waiter();
            assertEquals(WAITERS, waiter.waiterNames());
            WaiterResponse<Document> response = waiter.waitUntil("ItemReady", id("a"), FAST);
            assertEquals(3, response.attemptsExecuted());
            assertEquals("READY", response.matched().response().orElseThrow().getMember("value").asString());
        }
        assertEquals(3, transport.transmissions());
        assertTrue(transport.requests().stream().allMatch(r -> r.request().encodedPath().equals("/items/a")));
    }

    @Test
    void aSyncWaiterFailsWhenTheFailureAcceptorMatches() {
        TestTransport transport = replying(restJson(200, "{\"value\":\"FAILED\"}"));
        try (DynamicClient client = TestSupport.mediaClient(transport).build()) {
            SdkClientException e = assertThrows(SdkClientException.class,
                                                () -> client.waiter().waitUntil("ItemReady", id("a"), FAST));
            assertTrue(e.getMessage().contains("failure state"), e.getMessage());
        }
        assertEquals(1, transport.transmissions());
    }

    @Test
    void aSyncWaiterTimesOutAfterItsMaximumWaitTime() {
        TestTransport transport = replying(restJson(200, "{\"value\":\"PENDING\"}"));
        WaiterOverrideConfiguration timeout = WaiterOverrideConfiguration.builder()
                                                                         .minDelay(Duration.ofMillis(5))
                                                                         .maxDelay(Duration.ofMillis(5))
                                                                         .waitTimeout(Duration.ofMillis(60))
                                                                         .build();
        try (DynamicClient client = TestSupport.mediaClient(transport).build()) {
            DynamicWaiter waiter = DynamicWaiter.builder().client(client).overrideConfiguration(timeout).build();
            SdkClientException e = assertThrows(SdkClientException.class, () -> waiter.waitUntil("ItemReady", id("a")));
            assertTrue(e.getMessage().contains("maximum wait time"), e.getMessage());
        }
        assertTrue(transport.transmissions() > 1, "polled until the time ran out");
    }

    @Test
    void inputOutputFilterAndExceptionAcceptorsMatchAsGeneratedAcceptorsDo() {
        TestTransport echoed = replying(restJson(200, "{\"id\":\"b\"}"), restJson(200, "{\"id\":\"a\"}"));
        try (DynamicClient client = TestSupport.mediaClient(echoed).build()) {
            assertEquals(2, client.waiter().waitUntil("ItemEchoed", id("a"), FAST).attemptsExecuted());
        }

        TestTransport tagged = replying(restJson(200, "{\"tags\":[\"cold\"]}"),
                                        restJson(200, "{\"tags\":[\"cold\",\"hot\"]}"));
        try (DynamicClient client = TestSupport.mediaClient(tagged).build()) {
            assertEquals(2, client.waiter().waitUntil("ItemTagged", id("a"), FAST).attemptsExecuted());
        }

        TestTransport gone = replying(missing());
        try (DynamicClient client = TestSupport.mediaClient(gone).build()) {
            WaiterResponse<Document> response = client.waiter().waitUntil("ItemTagged", id("a"), FAST);
            DynamicServiceException error = assertInstanceOf(DynamicServiceException.class,
                                                             response.matched().exception().orElseThrow());
            assertEquals("example.media#ItemMissing", error.errorShapeId().toString());
        }
    }

    @Test
    void unknownWaitersAndInvalidInputsAreRejectedBeforeAnyPoll() {
        TestTransport transport = replying(restJson(200, "{}"));
        try (DynamicClient client = TestSupport.mediaClient(transport).build()) {
            DynamicModelException unknown = assertThrows(DynamicModelException.class,
                                                         () -> client.waiter().waitUntil("itemReady", id("a")));
            assertEquals(DynamicModelException.Reason.UNKNOWN_WAITER, unknown.reason());
            assertEquals("itemReady", unknown.waiterName().orElseThrow());
            assertEquals(WAITERS, unknown.available());
            assertTrue(unknown.getMessage().contains(String.join(", ", WAITERS)), unknown.getMessage());

            DynamicInvocationException invalid = assertThrows(
                DynamicInvocationException.class,
                () -> client.waiter().waitUntil("ItemReady", doc(Map.of("id", Document.of(1)))));
            assertEquals("input.id", invalid.violations().get(0).path());
        }
        assertEquals(0, transport.transmissions());
    }

    // -------------------------------------------------------------------------------------------------------------
    // Async

    @Test
    void anAsyncWaiterReturnsAtOnceAndDelaysOnTheClientsRetryScheduler() throws Exception {
        ControlledHttp http = new ControlledHttp();
        HeldScheduler scheduler = new HeldScheduler();
        DynamicAsyncClient.Builder builder = TestSupport.mediaAsyncClient(http);
        // The public builder has no retry-scheduler setter, as for generated builders; it forwards to the state.
        builder.state.retryScheduler(scheduler);
        try (DynamicAsyncClient client = builder.build()) {
            CompletableFuture<WaiterResponse<Document>> future = client.waiter().waitUntil("ItemReady", id("a"));
            assertFalse(future.isDone(), "the waiter returns before polling completes");
            Exchange first = http.next();
            first.respond(Reply.json(200, "{\"value\":\"PENDING\"}"));
            CompletableFuture<Void> delay = scheduler.next();
            assertFalse(future.isDone());
            Duration requested = scheduler.requested.get(0);
            assertTrue(requested.compareTo(Duration.ofSeconds(2)) >= 0
                       && requested.compareTo(Duration.ofSeconds(120)) <= 0, "the modeled delay bounds: " + requested);
            http.settle();
            assertEquals(1, http.calls.get(), "no poll before the delay completes");

            delay.complete(null);
            http.next().respond(Reply.json(200, "{\"value\":\"READY\"}"));
            WaiterResponse<Document> response = future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertEquals(2, response.attemptsExecuted());
            assertEquals("READY", response.matched().response().orElseThrow().getMember("value").asString());
        } finally {
            http.close();
        }
    }

    @Test
    void anAsyncWaiterFailsOnTheFailureAcceptorAndTimesOut() throws Exception {
        ControlledHttp http = new ControlledHttp();
        HeldScheduler scheduler = new HeldScheduler();
        try (DynamicAsyncClient client = TestSupport.mediaAsyncClient(http).build()) {
            DynamicAsyncWaiter waiter = DynamicAsyncWaiter.builder().client(client).retryScheduler(scheduler).build();
            CompletableFuture<WaiterResponse<Document>> failing = waiter.waitUntil("ItemReady", id("a"));
            http.next().respond(Reply.json(200, "{\"value\":\"FAILED\"}"));
            assertTrue(failure(failing).getMessage().contains("failure state"));

            WaiterOverrideConfiguration timeout = WaiterOverrideConfiguration.builder()
                                                                             .minDelay(Duration.ofMillis(10))
                                                                             .maxDelay(Duration.ofMillis(10))
                                                                             .waitTimeout(Duration.ofMillis(500))
                                                                             .build();
            CompletableFuture<WaiterResponse<Document>> timing = waiter.waitUntil("ItemReady", id("b"), timeout);
            http.next().respond(Reply.json(200, "{\"value\":\"PENDING\"}"));
            CompletableFuture<Void> delay = scheduler.next();
            Thread.sleep(600);
            delay.complete(null);
            http.next().respond(Reply.json(200, "{\"value\":\"PENDING\"}"));
            assertTrue(failure(timing).getMessage().contains("maximum wait time"));
            assertEquals(3, http.calls.get());

            CompletableFuture<WaiterResponse<Document>> unknown = waiter.waitUntil("Nope", id("c"));
            assertTrue(unknown.isCompletedExceptionally(), "an unknown waiter returns an already failed future");
            assertEquals(DynamicModelException.Reason.UNKNOWN_WAITER,
                         assertInstanceOf(DynamicModelException.class, failure(unknown)).reason());
            assertEquals(3, http.calls.get());
        } finally {
            http.close();
        }
    }

    @Test
    void cancellingAnAsyncWaiterCancelsItsPendingPollOrDelay() throws Exception {
        ControlledHttp http = new ControlledHttp();
        HeldScheduler scheduler = new HeldScheduler();
        try (DynamicAsyncClient client = TestSupport.mediaAsyncClient(http).build()) {
            DynamicAsyncWaiter waiter = DynamicAsyncWaiter.builder().client(client).retryScheduler(scheduler).build();

            CompletableFuture<WaiterResponse<Document>> polling = waiter.waitUntil("ItemReady", id("a"));
            Exchange poll = http.next();
            assertTrue(polling.cancel(true));
            await(() -> poll.cancels.get() == 1);
            assertEquals(List.of(CancellationReason.CALLER_CANCELLED), poll.reasons);
            poll.respond(Reply.json(200, "{\"value\":\"READY\"}"));
            http.settle();
            assertEquals(1, http.calls.get());
            assertTrue(scheduler.delays.isEmpty(), "no delay after a cancelled poll");

            CompletableFuture<WaiterResponse<Document>> delaying = waiter.waitUntil("ItemReady", id("b"));
            http.next().respond(Reply.error(404, "ItemMissing"));
            CompletableFuture<Void> delay = scheduler.next();
            assertFalse(delaying.isDone());
            assertTrue(delaying.cancel(true));
            assertTrue(delay.isCancelled(), "the pending waiter delay is cancelled");
            delay.complete(null);
            http.settle();
            assertEquals(2, http.calls.get(), "no poll after cancellation");
        } finally {
            http.close();
        }
    }

    private static Throwable failure(CompletableFuture<?> future) {
        ExecutionException e = assertThrows(ExecutionException.class,
                                            () -> future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        return e.getCause();
    }
}
