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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.dynamic.client.ControlledHttp.EVENT_LOOP;
import static software.amazon.awssdk.dynamic.client.ControlledHttp.TIMEOUT;
import static software.amazon.awssdk.dynamic.client.ControlledHttp.await;
import static software.amazon.awssdk.dynamic.client.TestSupport.doc;
import static software.amazon.awssdk.dynamic.client.TestSupport.messageFrame;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.dynamic.client.ControlledHttp.DemandTransformer;
import software.amazon.awssdk.dynamic.client.ControlledHttp.Exchange;
import software.amazon.awssdk.dynamic.client.ControlledHttp.LoopPublisher;
import software.amazon.awssdk.dynamic.client.ControlledHttp.Reply;
import software.amazon.awssdk.dynamic.client.ControlledHttp.SourcePublisher;
import software.amazon.awssdk.dynamic.client.DynamicInvocationException.Reason;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;

/**
 * Asynchronous dynamic streaming calls (Requirements 14.10, 14.12, 14.13): the same exact request-body and
 * response-transformer demand, event stream framing, terminal behavior and cancellation through the shared pipeline
 * as the generated async client's streaming calls ({@code GeneratedAsyncBehaviorTest}).
 */
class DynamicAsyncStreamingTest {
    private final ControlledHttp http = new ControlledHttp();
    private final ExecutorService producers = Executors.newCachedThreadPool();
    private final DynamicAsyncClient client = TestSupport.mediaAsyncClient(http).build();

    @AfterEach
    void close() {
        client.close();
        http.close();
        producers.shutdownNow();
    }

    private static Document name(String name) {
        return doc(Map.of("name", Document.of(name)));
    }

    private static Document message(String text) {
        return doc(Map.of("message", doc(Map.of("text", Document.of(text)))));
    }

    @Test
    void theStreamingRequestBodyIsPulledOnlyAsTheTransportRequests() throws Exception {
        SourcePublisher body = new SourcePublisher("a", "b", "c");
        CompletableFuture<Document> future = client.callAsync("PutBlob", name("n"),
                                                              AsyncRequestBody.fromPublisher(body));
        Exchange exchange = http.next();
        http.settle();
        assertEquals(0, body.delivered.get(), "nothing is read before the transport demands");

        exchange.pullBody(1);
        assertEquals(1, body.delivered.get());
        exchange.pullBody(1);
        assertEquals(2, body.delivered.get());
        assertFalse(exchange.bodyComplete.get());
        exchange.pullBody(5);
        await(exchange.bodyComplete::get);
        assertEquals("abc", exchange.bodyText());
        assertTrue(body.requested.get() <= exchange.pulled.get(), "the source saw no more demand than the transport");
        assertFalse(future.isDone(), "incomplete until the transport answers");

        exchange.respond(Reply.json(200, "{}"));
        assertNotNull(future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertEquals("n", exchange.request.headers().firstValue("X-Name").orElseThrow());
    }

    @Test
    void theStreamingResponseIsDeliveredOnlyAsTheTransformerRequests() throws Exception {
        DemandTransformer transformer = new DemandTransformer();
        CompletableFuture<String> future = client.callAsync("GetBlob", name("b"), transformer);
        Exchange exchange = http.next();
        LoopPublisher payload = LoopPublisher.ofText(http.eventLoop, "x", "y", "z");
        exchange.respond(Reply.stream(payload, "application/octet-stream"));
        await(() -> transformer.subscription != null);
        http.settle();
        assertEquals(0, payload.delivered.get(), "nothing is delivered before the transformer demands");
        assertEquals("k", transformer.response.get().getMember("kind").asString());

        transformer.subscription.request(1);
        await(() -> transformer.received.size() == 1);
        http.settle();
        assertEquals(1, payload.delivered.get());
        assertFalse(future.isDone());
        transformer.subscription.request(2);
        await(() -> transformer.received.size() == 3);
        transformer.subscription.request(1);
        assertEquals("xyz", future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertTrue(transformer.callbackThreads.stream().noneMatch(t -> t.getName().equals(EVENT_LOOP)),
                   "transformer callbacks ran on " + transformer.callbackThreads);
    }

    @Test
    void inputEventFramesAreTransmittedOnlyAsTheTransportRequests() throws Exception {
        EventStream<SerializableStruct> writer = EventStream.newWriter();
        CompletableFuture<Document> future = client.callAsync(
            "Chat", doc(Map.of("messages", Document.ofObject(writer))));
        Exchange exchange = http.next();
        assertEquals("application/vnd.amazon.eventstream",
                     exchange.request.headers().firstValue("Content-Type").orElseThrow());
        SerializableStruct first = client.inputEvent("Chat", message("first"));
        SerializableStruct second = client.inputEvent("Chat", message("second"));
        CompletableFuture<Void> producer = CompletableFuture.runAsync(() -> {
            writer.asWriter().write(first);
            writer.asWriter().write(second);
            writer.close();
        }, producers);

        exchange.pullBody(1);
        await(() -> exchange.bodyChunks.size() == 1);
        http.settle();
        assertEquals(1, exchange.bodyChunks.size(), "one frame per unit of transport demand");
        assertTrue(exchange.bodyText().contains("first") && !exchange.bodyText().contains("second"),
                   exchange.bodyText());

        exchange.pullBody(1);
        await(() -> exchange.bodyChunks.size() == 2);
        assertTrue(exchange.bodyText().contains("second"), exchange.bodyText());
        exchange.pullBody(Long.MAX_VALUE);
        await(exchange.bodyComplete::get);
        producer.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        assertFalse(future.isDone(), "incomplete until the transport answers");

        exchange.respond(Reply.json(200, "{}"));
        assertNotNull(future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    }

    @Test
    void anOutputEventStreamIsHandedOverBeforeItsBodyAndDecodedOnlyUnderDemand() throws Exception {
        EventsHandler handler = new EventsHandler();
        CompletableFuture<Void> future = client.callAsync("Watch", Document.of(Map.of()), handler);
        Exchange exchange = http.next();
        LoopPublisher frames = new LoopPublisher(http.eventLoop, List.of(ByteBuffer.wrap(messageFrame("a")),
                                                                         ByteBuffer.wrap(messageFrame("b"))));
        exchange.respond(Reply.stream(frames, "application/vnd.amazon.eventstream"));

        assertNotNull(handler.response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        EventsSubscriber events = handler.subscribe();
        http.settle();
        assertEquals(0, frames.delivered.get(), "the output arrives before any event byte is read");
        assertFalse(future.isDone(), "the call lasts as long as its event stream");

        events.subscription.request(1);
        await(() -> events.received.size() == 1);
        http.settle();
        assertEquals(1, frames.delivered.get(), "one frame per event demanded");
        assertEquals("a", text(events.received.get(0)));
        assertFalse(future.isDone());

        events.subscription.request(1);
        assertNull(future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertEquals(2, events.received.size());
        assertEquals("b", text(events.received.get(1)));
        await(() -> events.completions.get() == 1 && handler.completions.get() == 1);
        assertTrue(handler.failures.isEmpty() && events.errors.isEmpty());
        assertTrue(handler.callbackThreads.stream().noneMatch(t -> t.getName().equals(EVENT_LOOP)),
                   "handler callbacks ran on " + handler.callbackThreads);
    }

    @Test
    void cancellingTheOutputEventSubscriptionCancelsTheCallAndTheExchange() throws Exception {
        EventsHandler handler = new EventsHandler();
        CompletableFuture<Void> future = client.callAsync("Watch", Document.of(Map.of()), handler);
        Exchange exchange = http.next();
        LoopPublisher frames = new LoopPublisher(http.eventLoop, List.of(ByteBuffer.wrap(messageFrame("a")),
                                                                         ByteBuffer.wrap(messageFrame("b"))));
        exchange.respond(Reply.stream(frames, "application/vnd.amazon.eventstream"));
        handler.response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        EventsSubscriber events = handler.subscribe();
        events.subscription.request(1);
        await(() -> events.received.size() == 1);

        events.subscription.cancel();
        await(() -> exchange.cancels.get() == 1);
        assertEquals(List.of(CancellationReason.CALLER_CANCELLED), exchange.reasons);
        await(frames.cancelled::get);
        assertThrows(CancellationException.class, () -> future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        await(() -> handler.failures.size() == 1);
        events.subscription.request(5);
        http.settle();
        assertEquals(1, frames.delivered.get(), "no event decoded after cancellation");
        assertEquals(1, events.received.size());
        assertEquals(1, http.calls.get());
    }

    @Test
    void anEventStreamHandlerForAnOperationWithoutOneReturnsAFailedFutureAndSendsNothing() {
        assertEquals(Reason.NO_STREAMING_MEMBER, reason(client.callAsync("ListItems", Document.of(Map.of()),
                                                                         new EventsHandler())));
        assertEquals(0, http.calls.get());
    }

    @Test
    void streamingArgumentMismatchesReturnFailedFuturesAndSendNothing() {
        assertEquals(Reason.REQUIRES_STREAMING_CALL, reason(client.callAsync("PutBlob", name("n"))));
        assertEquals(Reason.REQUIRES_STREAMING_CALL, reason(client.callAsync("GetBlob", name("b"))));
        // An async output event stream is never buffered into the output document: it needs the handler overload.
        assertEquals(Reason.REQUIRES_STREAMING_CALL, reason(client.callAsync("Watch", Document.of(Map.of()))));
        assertEquals(Reason.NO_STREAMING_MEMBER, reason(client.callAsync("Watch", Document.of(Map.of()),
                                                                         AsyncRequestBody.fromString("x"))));
        assertEquals(Reason.NO_STREAMING_MEMBER, reason(client.callAsync("ListItems", Document.of(Map.of()),
                                                                         new DemandTransformer())));
        assertEquals(0, http.calls.get());
    }

    // -------------------------------------------------------------------------------------------------------------
    // Cancellation reaches the shared pipeline and the transport

    @Test
    void cancellingDuringTheRequestBodyCancelsTheBodySubscriptionAndTheExchange() throws Exception {
        SourcePublisher body = new SourcePublisher("a", "b", "c");
        CompletableFuture<Document> future = client.callAsync("PutBlob", name("n"),
                                                              AsyncRequestBody.fromPublisher(body));
        Exchange exchange = http.next();
        exchange.pullBody(1);
        assertEquals(1, body.delivered.get());

        assertTrue(future.cancel(true));
        await(() -> exchange.cancels.get() == 1);
        assertEquals(List.of(CancellationReason.CALLER_CANCELLED), exchange.reasons);
        await(body.cancelled::get);
        exchange.pullBody(5);
        http.settle();
        assertEquals(1, body.delivered.get(), "no body consumption after cancellation");
        assertTrue(future.isCancelled());
        assertEquals(1, http.calls.get(), "no later attempt");
    }

    @Test
    void cancellingDuringTheResponseStreamCancelsThePayloadSubscriptionAndTheExchange() throws Exception {
        DemandTransformer transformer = new DemandTransformer();
        CompletableFuture<String> future = client.callAsync("GetBlob", name("b"), transformer);
        Exchange exchange = http.next();
        LoopPublisher payload = LoopPublisher.ofText(http.eventLoop, "x", "y", "z");
        exchange.respond(Reply.stream(payload, "application/octet-stream"));
        await(() -> transformer.subscription != null);
        transformer.subscription.request(1);
        await(() -> transformer.received.size() == 1);

        assertTrue(future.cancel(true));
        await(payload.cancelled::get);
        await(() -> exchange.cancels.get() == 1);
        await(() -> transformer.exception.get() != null);
        transformer.subscription.request(5);
        http.settle();
        assertEquals(1, payload.delivered.get(), "no payload delivery after cancellation");
        assertEquals(1, transformer.received.size());
        assertEquals(1, http.calls.get());
    }

    @Test
    void cancellingDuringAnInputEventStreamCancelsTheExchangeAndTheEventStream() throws Exception {
        EventStream<SerializableStruct> writer = EventStream.newWriter();
        CompletableFuture<Document> future = client.callAsync(
            "Chat", doc(Map.of("messages", Document.ofObject(writer))));
        Exchange exchange = http.next();
        SerializableStruct first = client.inputEvent("Chat", message("first"));
        SerializableStruct later = client.inputEvent("Chat", message("later"));
        CompletableFuture<Void> producer = CompletableFuture.runAsync(() -> writer.asWriter().write(first),
                                                                      producers);
        exchange.pullBody(1);
        await(() -> exchange.bodyChunks.size() == 1);
        producer.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        assertTrue(future.cancel(true));
        await(() -> exchange.cancels.get() == 1);
        assertEquals(List.of(CancellationReason.CALLER_CANCELLED), exchange.reasons);
        CompletableFuture<Void> lateWrite = CompletableFuture.runAsync(() -> writer.asWriter().write(later),
                                                                       producers);
        ExecutionException rejected = assertThrows(ExecutionException.class,
                                                   () -> lateWrite.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        assertNotNull(rejected.getCause(), "a write after cancellation fails");
        exchange.pullBody(5);
        http.settle();
        assertFalse(exchange.bodyText().contains("later"), "no frame after cancellation");
        assertEquals(1, http.calls.get());
    }

    private static Reason reason(CompletableFuture<?> future) {
        assertTrue(future.isCompletedExceptionally(), "a rejected call returns an already failed future");
        ExecutionException e = assertThrows(ExecutionException.class, future::get);
        return assertInstanceOf(DynamicInvocationException.class, e.getCause()).reason();
    }

    private static String text(Object event) {
        return assertInstanceOf(Document.class, event).getMember("message").getMember("text").asString();
    }

    /** Records the event stream handler's callbacks and their threads. */
    private static final class EventsHandler implements EventStreamResponseHandler<Document, Document> {
        final CompletableFuture<Document> response = new CompletableFuture<>();
        final CompletableFuture<SdkPublisher<Document>> stream = new CompletableFuture<>();
        final List<Throwable> failures = new CopyOnWriteArrayList<>();
        final AtomicInteger completions = new AtomicInteger();
        final List<Thread> callbackThreads = new CopyOnWriteArrayList<>();

        @Override
        public void responseReceived(Document output) {
            callbackThreads.add(Thread.currentThread());
            response.complete(output);
        }

        @Override
        public void onEventStream(SdkPublisher<Document> publisher) {
            callbackThreads.add(Thread.currentThread());
            stream.complete(publisher);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            callbackThreads.add(Thread.currentThread());
            failures.add(throwable);
        }

        @Override
        public void complete() {
            callbackThreads.add(Thread.currentThread());
            completions.incrementAndGet();
        }

        EventsSubscriber subscribe() throws Exception {
            EventsSubscriber subscriber = new EventsSubscriber();
            stream.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).subscribe(subscriber);
            return subscriber;
        }
    }

    /** An event subscriber that signals no demand of its own. */
    private static final class EventsSubscriber implements Subscriber<Document> {
        final List<Document> received = new CopyOnWriteArrayList<>();
        final List<Throwable> errors = new CopyOnWriteArrayList<>();
        final AtomicInteger completions = new AtomicInteger();
        volatile Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        @Override
        public void onNext(Document event) {
            received.add(event);
        }

        @Override
        public void onError(Throwable t) {
            errors.add(t);
        }

        @Override
        public void onComplete() {
            completions.incrementAndGet();
        }
    }
}
