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

package software.amazon.awssdk.pipeline;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.eventstream.EventPayloadCodec;
import software.amazon.awssdk.protocols.core.eventstream.EventStreams;
import software.amazon.awssdk.protocols.core.eventstream.OperationEventStreams;
import software.amazon.awssdk.protocols.core.eventstream.OutputEventStream;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.event.EventStreamReader;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The engine's output event stream (Requirements 5.11, 8.5, 8.9, 14.12): the async call hands the output to its
 * {@link EventStreamResponseHandler} before any event byte is read, decodes events from the transport's body only
 * under the subscriber's demand, ends exactly once with the call's mapped outcome, is never retried once handed over,
 * and is cancelled with its exchange from the subscription or the future; the sync call keeps its exchange open until
 * the event stream is drained or closed.
 */
class AwsCallEngineEventStreamTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Model MODEL = Model.assembler().addUnparsedModel("events.smithy", String.join("\n",
        "$version: \"2\"",
        "namespace test.es",
        "structure WatchInput { name: String }",
        "structure WatchOutput { value: String, events: Events }",
        "@streaming",
        "union Events { data: DataEvent, boom: BoomError }",
        "structure DataEvent {",
        "    @eventPayload",
        "    payload: Blob",
        "}",
        "@error(\"server\")",
        "structure BoomError { message: String }")).assemble().unwrap();
    private static final SchemaConverter SCHEMAS = new SchemaConverter(MODEL);
    private static final Schema INPUT = SCHEMAS.getSchema(MODEL.expectShape(ShapeId.from("test.es#WatchInput")));
    private static final Schema OUTPUT = SCHEMAS.getSchema(MODEL.expectShape(ShapeId.from("test.es#WatchOutput")));
    private static final AwsOperation<StructDocument, StructDocument> WATCH = watch();

    private final ExecutorService completion = Executors.newSingleThreadExecutor(r -> new Thread(r, "completion"));
    private final FakeAsyncHttp http = new FakeAsyncHttp();

    @AfterEach
    void shutDown() {
        completion.shutdownNow();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Async: demand-driven delivery

    @Test
    void theOutputArrivesBeforeAnyEventByte_andEachDemandDecodesExactlyOneFrame() throws Exception {
        FrameBody body = http.respond(200, "v1", data("a"), data("b"), data("c"));
        Handler handler = new Handler();
        CompletableFuture<Void> future = start(false, handler);

        StructDocument output = handler.response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        assertThat(output.getMember("value").asString()).isEqualTo("v1");
        Recorder events = handler.subscribe();
        assertThat(body.pulled.get()).as("no body chunk is read before the subscriber demands").isZero();
        assertThat(future).isNotDone();

        events.subscription.request(1);
        assertThat(events.texts()).containsExactly("a");
        assertThat(body.pulled.get()).as("one chunk (one frame) per event demanded").isEqualTo(1);
        events.subscription.request(1);
        assertThat(events.texts()).containsExactly("a", "b");
        assertThat(body.pulled.get()).isEqualTo(2);
        assertThat(future).isNotDone();

        events.subscription.request(5);
        assertThat(future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isNull();
        assertThat(events.texts()).containsExactly("a", "b", "c");
        await(() -> events.completions.get() == 1);
        assertThat(events.errors).isEmpty();
        assertThat(handler.completions.get()).isEqualTo(1);
        assertThat(handler.failures).isEmpty();
        assertThat(http.transmitted).hasSize(1);
        assertThat(http.cancelled).isEmpty();
    }

    @Test
    void aFrameSplitAcrossChunks_isDecodedWithoutReadingAhead() throws Exception {
        byte[] first = data("first");
        byte[] second = data("second");
        int half = first.length / 2;
        FrameBody body = http.respond(200, "v", Arrays.copyOfRange(first, 0, half),
                                      Arrays.copyOfRange(first, half, first.length), second);
        Handler handler = new Handler();
        start(false, handler);
        handler.response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        Recorder events = handler.subscribe();

        events.subscription.request(1);
        assertThat(events.texts()).containsExactly("first");
        assertThat(body.pulled.get()).as("the two chunks of the first frame, not the next frame").isEqualTo(2);
    }

    @Test
    void anErrorResponseBeforeHandOver_isRetried_andTheHandlerSeesOnlyTheSuccess() throws Exception {
        http.respond(503, null);
        http.respond(200, "v2", data("x"));
        Handler handler = new Handler();
        CompletableFuture<Void> future = start(false, handler);

        assertThat(handler.response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).getMember("value").asString())
            .isEqualTo("v2");
        Recorder events = handler.subscribe();
        events.subscription.request(Long.MAX_VALUE);
        future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        assertThat(events.texts()).containsExactly("x");
        assertThat(http.transmitted).hasSize(2);
        assertThat(handler.failures).isEmpty();
    }

    @Test
    void anRpcOutput_readsOnlyItsInitialResponseEvent_beforeTheHandlerHasIt() throws Exception {
        FrameBody body = http.respond(200, null, initialResponse("{\"value\":\"rpc\"}"), data("e1"), data("e2"));
        Handler handler = new Handler();
        CompletableFuture<Void> future = start(true, handler);

        StructDocument output = handler.response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        assertThat(output.getMember("value").asString()).isEqualTo("rpc");
        Recorder events = handler.subscribe();
        assertThat(body.pulled.get()).as("only the initial-response frame is read without demand").isEqualTo(1);

        events.subscription.request(1);
        assertThat(events.texts()).containsExactly("e1");
        assertThat(body.pulled.get()).isEqualTo(2);
        events.subscription.request(1);
        events.subscription.request(1);
        future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        assertThat(events.texts()).containsExactly("e1", "e2");
        await(() -> events.completions.get() == 1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Async: in-stream failures end the call once, and are never retried

    @Test
    void aModeledExceptionEvent_endsTheStreamOnce_withTheCallsMappedFailure() throws Exception {
        http.respond(200, "v", data("a"), exception("boom", "{\"message\":\"bad\"}"), data("never"));
        http.respond(200, "unused", data("unused"));
        Handler handler = new Handler();
        CompletableFuture<Void> future = start(false, handler);
        handler.response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        Recorder events = handler.subscribe();

        events.subscription.request(10);
        Throwable failure = failure(future);
        assertThat(failure).isInstanceOfSatisfying(AwsServiceException.class,
                                                   e -> assertThat(e.awsErrorDetails().errorCode()).isEqualTo("boom"));
        await(() -> events.errors.size() == 1);
        assertThat(events.texts()).containsExactly("a");
        assertThat(events.errors).containsExactly(failure);
        assertThat(events.completions.get()).isZero();
        await(() -> handler.failures.size() == 1);
        assertThat(handler.failures).containsExactly(failure);
        assertThat(handler.completions.get()).isZero();
        assertThat(http.transmitted).as("no retry once the stream was handed over").hasSize(1);
    }

    @Test
    void aRetryableErrorFrame_endsTheStreamOnce_andIsNotRetried() throws Exception {
        http.respond(200, "v", error("ThrottlingException", "slow down"));
        http.respond(200, "unused", data("unused"));
        Handler handler = new Handler();
        CompletableFuture<Void> future = start(false, handler);
        handler.response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        Recorder events = handler.subscribe();

        events.subscription.request(1);
        Throwable failure = failure(future);
        assertThat(failure).isInstanceOfSatisfying(
            AwsServiceException.class,
            e -> assertThat(e.awsErrorDetails().errorCode()).isEqualTo("ThrottlingException"));
        await(() -> events.errors.size() == 1 && handler.failures.size() == 1);
        assertThat(events.errors).containsExactly(failure);
        assertThat(handler.failures).containsExactly(failure);
        assertThat(events.completions.get()).isZero();
        assertThat(http.transmitted).hasSize(1);
    }

    @Test
    void aBodyThatEndsMidFrame_isATerminalFailure() throws Exception {
        byte[] frame = data("cut");
        http.respond(200, "v", data("whole"), Arrays.copyOf(frame, frame.length - 3));
        Handler handler = new Handler();
        CompletableFuture<Void> future = start(false, handler);
        handler.response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        Recorder events = handler.subscribe();

        events.subscription.request(10);
        Throwable failure = failure(future);
        await(() -> events.errors.size() == 1 && handler.failures.size() == 1);
        assertThat(events.texts()).containsExactly("whole");
        assertThat(events.errors).containsExactly(failure);
        assertThat(events.completions.get()).isZero();
        assertThat(http.transmitted).hasSize(1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Async: cancellation

    @Test
    void cancellingTheSubscription_cancelsTheCallAndTheExchange_andNothingMoreIsDecoded() throws Exception {
        FrameBody body = http.respond(200, "v", data("a"), data("b"), data("c"));
        Handler handler = new Handler();
        CompletableFuture<Void> future = start(false, handler);
        handler.response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        Recorder events = handler.subscribe();
        events.subscription.request(1);
        assertThat(events.texts()).containsExactly("a");

        events.subscription.cancel();
        assertThat(failure(future)).isInstanceOf(CancellationException.class);
        await(() -> !http.cancelled.isEmpty());
        assertThat(http.cancelled).containsExactly(CancellationReason.CALLER_CANCELLED);
        assertThat(body.cancelled.get()).isTrue();
        await(() -> handler.failures.size() == 1);
        assertThat(handler.failures.get(0)).isInstanceOf(CancellationException.class);

        events.subscription.request(5);
        assertThat(events.texts()).containsExactly("a");
        assertThat(body.pulled.get()).isEqualTo(1);
        assertThat(events.errors).as("a subscriber that cancelled receives no terminal signal").isEmpty();
        assertThat(events.completions.get()).isZero();
        assertThat(http.transmitted).hasSize(1);
    }

    @Test
    void cancellingTheFuture_cancelsTheExchange_andFailsTheSubscriberOnce() throws Exception {
        FrameBody body = http.respond(200, "v", data("a"), data("b"));
        Handler handler = new Handler();
        CompletableFuture<Void> future = start(false, handler);
        handler.response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        Recorder events = handler.subscribe();
        events.subscription.request(1);

        assertThat(future.cancel(true)).isTrue();
        assertThat(future).isCancelled();
        await(() -> events.errors.size() == 1 && handler.failures.size() == 1);
        assertThat(events.errors.get(0)).isInstanceOf(CancellationException.class);
        assertThat(http.cancelled).containsExactly(CancellationReason.CALLER_CANCELLED);
        assertThat(body.cancelled.get()).isTrue();

        events.subscription.request(5);
        assertThat(events.texts()).containsExactly("a");
        assertThat(body.pulled.get()).isEqualTo(1);
        assertThat(events.completions.get()).isZero();
        assertThat(events.errors).hasSize(1);
    }

    @Test
    void anOperationWithoutAnOutputEventStream_isRejected() {
        AwsCallEngine engine = asyncEngine(false);
        assertThatThrownBy(() -> engine.executeAsync(engine.runtime().plan(PipelineFixtures.GET_THING),
                                                     PipelineFixtures.request(PipelineFixtures.GET_INPUT, "n"), null,
                                                     null, structHandler()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has no output event stream");
        assertThat(http.transmitted).isEmpty();
    }

    @Test
    void anOutputEventStreamOperation_isRejectedByTheBufferingDriver_andNothingIsSent() {
        AwsCallEngine engine = asyncEngine(false);
        assertThatThrownBy(() -> engine.executeAsync(engine.runtime().plan(WATCH), input(), null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has an output event stream")
            .hasMessageContaining("EventStreamResponseHandler");
        assertThat(http.transmitted).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Sync: the exchange lives as long as the event stream

    @Test
    void theSyncExchangeStaysOpenUntilTheEventStreamIsDrained() {
        FakeSyncHttp sync = new FakeSyncHttp(concat(data("a"), data("b")));
        EsProtocol protocol = new EsProtocol(false);
        StructDocument output = call(syncEngine(protocol, sync));

        assertThat(output.getMember("value").asString()).isEqualTo("v");
        assertThat(sync.closed.get()).as("open while the caller reads the events").isFalse();
        EventStreamReader<? extends SerializableStruct> reader = protocol.reader.get().asReader();
        assertThat(text(reader.read())).isEqualTo("a");
        assertThat(sync.closed.get()).isFalse();
        assertThat(text(reader.read())).isEqualTo("b");
        assertThat(reader.read()).isNull();
        assertThat(sync.closed.get()).as("released at the end of the stream").isTrue();
        assertThat(sync.closes.get()).isEqualTo(1);
    }

    @Test
    void closingTheSyncEventStream_releasesTheExchange() {
        FakeSyncHttp sync = new FakeSyncHttp(concat(data("a"), data("b")));
        EsProtocol protocol = new EsProtocol(false);
        call(syncEngine(protocol, sync));
        EventStreamReader<? extends SerializableStruct> reader = protocol.reader.get().asReader();
        assertThat(text(reader.read())).isEqualTo("a");
        assertThat(sync.closed.get()).isFalse();

        reader.close();
        assertThat(sync.closed.get()).isTrue();
        assertThat(sync.closes.get()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Fixtures

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EventStreamResponseHandler<PipelineFixtures.Struct, Object> structHandler() {
        return (EventStreamResponseHandler) new Handler();
    }

    private CompletableFuture<Void> start(boolean rpc, Handler handler) {
        AwsCallEngine engine = asyncEngine(rpc);
        return engine.executeAsync(engine.runtime().plan(WATCH), input(), null, null, handler);
    }

    private static StructDocument call(AwsCallEngine engine) {
        return engine.executeSync(engine.runtime().plan(WATCH), input(), null, null);
    }

    private AwsCallEngine asyncEngine(boolean rpc) {
        return AwsCallEngine.builder()
                            .runtime(PipelineFixtures.engineRuntime(retries(3), HttpTransportMode.ASYNC)
                                                     .protocol(new EsProtocol(rpc))
                                                     .completionExecutor(completion)
                                                     .retryScheduler(d -> CompletableFuture.completedFuture(null))
                                                     .build())
                            .asyncHttpClient(http)
                            .build();
    }

    private static AwsCallEngine syncEngine(EsProtocol protocol, FakeSyncHttp sync) {
        return AwsCallEngine.builder()
                            .runtime(PipelineFixtures.engineRuntime(retries(3), HttpTransportMode.SYNC)
                                                     .protocol(protocol)
                                                     .build())
                            .syncHttpClient(sync)
                            .build();
    }

    private static AwsOperation<StructDocument, StructDocument> watch() {
        Schema operation = Schema.createOperation(ShapeId.from("test.es#Watch"));
        OperationSchemas schemas = OperationSchemas.builder(operation)
                                                   .input(INPUT)
                                                   .output(OUTPUT)
                                                   .outputStreamMember("events")
                                                   .build();
        AwsOperationDescriptor descriptor = AwsOperationDescriptor.builder()
                                                                  .id(operation.id())
                                                                  .service(PipelineFixtures.SERVICE)
                                                                  .input(INPUT.id())
                                                                  .output(OUTPUT.id())
                                                                  .schemas(schemas)
                                                                  .build();
        return AwsOperation.builder(descriptor, () -> SchemaConverter.createDocumentBuilder(INPUT),
                                    () -> SchemaConverter.createDocumentBuilder(OUTPUT))
                           .build();
    }

    private static StructDocument input() {
        ShapeBuilder<StructDocument> builder = SchemaConverter.createDocumentBuilder(INPUT);
        builder.setMemberValue(INPUT.member("name"), "n");
        return builder.build();
    }

    private static byte[] data(String text) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":message-type", HeaderValue.ofString("event"));
        headers.put(":event-type", HeaderValue.ofString("data"));
        headers.put(":content-type", HeaderValue.ofString("application/octet-stream"));
        return encode(new Message(headers, text.getBytes(UTF_8)));
    }

    private static byte[] initialResponse(String json) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":message-type", HeaderValue.ofString("event"));
        headers.put(":event-type", HeaderValue.ofString("initial-response"));
        headers.put(":content-type", HeaderValue.ofString("application/json"));
        return encode(new Message(headers, json.getBytes(UTF_8)));
    }

    private static byte[] exception(String type, String json) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":message-type", HeaderValue.ofString("exception"));
        headers.put(":exception-type", HeaderValue.ofString(type));
        headers.put(":content-type", HeaderValue.ofString("application/json"));
        return encode(new Message(headers, json.getBytes(UTF_8)));
    }

    private static byte[] error(String code, String message) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":message-type", HeaderValue.ofString("error"));
        headers.put(":error-code", HeaderValue.ofString(code));
        headers.put(":error-message", HeaderValue.ofString(message));
        return encode(new Message(headers, new byte[0]));
    }

    private static byte[] encode(Message message) {
        ByteBuffer frame = MessageEncoder.encode(message);
        byte[] bytes = new byte[frame.remaining()];
        frame.get(bytes);
        return bytes;
    }

    private static byte[] concat(byte[]... frames) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] frame : frames) {
            out.writeBytes(frame);
        }
        return out.toByteArray();
    }

    private static String text(Object event) {
        ByteBuffer payload = ((StructDocument) event).getMember("data").getMember("payload").asBlob();
        return UTF_8.decode(payload.duplicate()).toString();
    }

    private static Throwable failure(CompletableFuture<?> future) {
        try {
            future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (CancellationException e) {
            return e;
        } catch (ExecutionException e) {
            return e.getCause();
        } catch (Exception e) {
            throw new AssertionError("the call did not fail", e);
        }
        throw new AssertionError("the call succeeded");
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("condition not met within " + TIMEOUT);
            }
            Thread.sleep(5);
        }
    }

    /** Records the handler's callbacks; {@link #subscribe()} subscribes a {@link Recorder} to the event stream. */
    private static final class Handler implements EventStreamResponseHandler<StructDocument, Object> {
        final CompletableFuture<StructDocument> response = new CompletableFuture<>();
        final CompletableFuture<SdkPublisher<Object>> stream = new CompletableFuture<>();
        final List<Throwable> failures = new CopyOnWriteArrayList<>();
        final AtomicInteger completions = new AtomicInteger();

        @Override
        public void responseReceived(StructDocument output) {
            response.complete(output);
        }

        @Override
        public void onEventStream(SdkPublisher<Object> publisher) {
            stream.complete(publisher);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            failures.add(throwable);
        }

        @Override
        public void complete() {
            completions.incrementAndGet();
        }

        Recorder subscribe() throws Exception {
            Recorder recorder = new Recorder();
            stream.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).subscribe(recorder);
            return recorder;
        }
    }

    /** An event subscriber that signals no demand of its own. */
    private static final class Recorder implements Subscriber<Object> {
        final List<Object> events = new CopyOnWriteArrayList<>();
        final List<Throwable> errors = new CopyOnWriteArrayList<>();
        final AtomicInteger completions = new AtomicInteger();
        volatile Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        @Override
        public void onNext(Object event) {
            events.add(event);
        }

        @Override
        public void onError(Throwable t) {
            errors.add(t);
        }

        @Override
        public void onComplete() {
            completions.incrementAndGet();
        }

        List<String> texts() {
            List<String> texts = new ArrayList<>();
            events.forEach(e -> texts.add(text(e)));
            return texts;
        }
    }

    /**
     * A response body of fixed chunks that delivers one chunk per unit of demand, on the requesting thread, and
     * counts the chunks it delivered.
     */
    private static final class FrameBody implements Publisher<ByteBuffer> {
        private final List<byte[]> chunks;
        final AtomicInteger pulled = new AtomicInteger();
        final AtomicBoolean cancelled = new AtomicBoolean();

        FrameBody(List<byte[]> chunks) {
            this.chunks = chunks;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private long demand;
                private boolean draining;
                private boolean done;

                @Override
                public synchronized void request(long n) {
                    demand = demand + n < 0 ? Long.MAX_VALUE : demand + n;
                    if (draining) {
                        return;
                    }
                    draining = true;
                    while (!done && demand > 0 && pulled.get() < chunks.size()) {
                        demand--;
                        subscriber.onNext(ByteBuffer.wrap(chunks.get(pulled.getAndIncrement())));
                    }
                    if (!done && pulled.get() == chunks.size()) {
                        done = true;
                        subscriber.onComplete();
                    }
                    draining = false;
                }

                @Override
                public void cancel() {
                    done = true;
                    cancelled.set(true);
                }
            });
        }
    }

    /** A scripted async transport: each exchange's headers, then its {@link FrameBody}. */
    private static final class FakeAsyncHttp implements SdkAsyncHttpClient {
        private final Queue<SdkHttpResponse> responses = new ConcurrentLinkedQueue<>();
        private final Queue<FrameBody> bodies = new ConcurrentLinkedQueue<>();
        final List<SdkHttpRequest> transmitted = new CopyOnWriteArrayList<>();
        final List<CancellationReason> cancelled = new CopyOnWriteArrayList<>();

        FrameBody respond(int status, String value, byte[]... chunks) {
            SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(status).body(HttpBody.empty())
                                                              .addHeader("Content-Type",
                                                                         EventStreams.CONTENT_TYPE);
            if (value != null) {
                response.addHeader("X-Value", value);
            }
            FrameBody body = new FrameBody(List.of(chunks));
            responses.add(response.build());
            bodies.add(body);
            return body;
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            transmitted.add(request);
            SdkHttpResponse response = responses.remove();
            FrameBody body = bodies.remove();
            AsyncExchangeCompletion exchange = new AsyncExchangeCompletion(clientName(), (reason, failure) -> {
                cancelled.add(reason);
                handler.onError(failure);
            });
            CompletableFuture.runAsync(() -> {
                handler.onHeaders(response);
                handler.onStream(body);
            });
            return exchange;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "FakeAsyncHttp";
        }

        @Override
        public void close() {
        }
    }

    /** A sync transport whose one exchange answers 200 with {@code X-Value: v} and a streaming body. */
    private static final class FakeSyncHttp implements SdkSyncHttpClient {
        private final byte[] body;
        final AtomicBoolean closed = new AtomicBoolean();
        final AtomicInteger closes = new AtomicInteger();

        FakeSyncHttp(byte[] body) {
            this.body = body;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    return SdkHttpResponse.builder().statusCode(200).addHeader("X-Value", "v")
                                          .body(HttpBody.streaming(StreamingBodies.fromInputStream(
                                              new java.io.ByteArrayInputStream(body), body.length,
                                              EventStreams.CONTENT_TYPE)))
                                          .build();
                }

                @Override
                public void abort() {
                }

                @Override
                public void close() {
                    closes.incrementAndGet();
                    closed.set(true);
                }
            };
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "FakeSyncHttp";
        }

        @Override
        public void close() {
        }
    }

    /**
     * Binds {@code value} to the {@code X-Value} header and {@code events} to the body (as the HTTP binding
     * protocols do), or with {@code rpc} reads the output from the {@code initial-response} event (as the RPC
     * protocols do), through protocol-core's real event stream codec.
     */
    private static final class EsProtocol implements ClientProtocol {
        private final boolean rpc;
        /** The sync event stream reader of the last deserialized response. */
        final AtomicReference<EventStream<? extends SerializableStruct>> reader = new AtomicReference<>();

        EsProtocol(boolean rpc) {
            this.rpc = rpc;
        }

        @Override
        public ShapeId protocolId() {
            return ShapeId.from("aws.protocols#awsJson1_0");
        }

        @Override
        public OperationProtocol operation(OperationSchemas operation) {
            OperationEventStreams events = OperationEventStreams.of(
                new WireContext(ShapeId.from(rpc ? "aws.protocols#awsJson1_0" : "aws.protocols#restJson1"),
                                operation.id()),
                operation, EventPayloadCodec.of(JsonCodec.builder().build(), "application/json"), null);
            return new OperationProtocol() {
                @Override
                public OperationSchemas schemas() {
                    return operation;
                }

                @Override
                public SdkHttpRequest serializeRequest(SerializableStruct input, URI endpoint) {
                    return SdkHttpRequest.builder().method("POST").scheme(endpoint.getScheme())
                                         .host(endpoint.getHost()).body(HttpBody.empty()).build();
                }

                @Override
                public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response,
                                                                            ShapeBuilder<T> output) {
                    // The sync reader over the live body; the output holds only the header member here.
                    reader.set(EventStreams.reader(response, events.outputCodec(), 1024));
                    output.setMemberValue(output.schema().member("value"),
                                          response.headers().firstValue("X-Value").orElse(null));
                    return output.build();
                }

                @Override
                public <T extends SerializableStruct> T deserializeStreamingOutput(SdkHttpResponse response,
                                                                                   ShapeBuilder<T> output) {
                    output.setMemberValue(output.schema().member("value"),
                                          response.headers().firstValue("X-Value").orElse(null));
                    return output.build();
                }

                @Override
                public Optional<OutputEventStream> outputEventStream() {
                    return Optional.ofNullable(rpc ? events.rpcOutput() : events.restOutput());
                }

                @Override
                public AwsServiceException deserializeError(SdkHttpResponse response) {
                    return AwsServiceException.builder()
                                              .statusCode(response.statusCode())
                                              .message("Service error " + response.statusCode())
                                              .awsErrorDetails(AwsErrorDetails.builder()
                                                                              .errorCode("Error" + response.statusCode())
                                                                              .build())
                                              .build();
                }
            };
        }
    }
}
