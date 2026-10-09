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

package software.amazon.awssdk.benchmark.harness;

import static software.amazon.awssdk.benchmark.harness.FiniteWorkloads.response;
import static software.amazon.awssdk.benchmark.harness.FiniteWorkloads.utf8;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.dynamic.client.DynamicAsyncClient;
import software.amazon.awssdk.dynamic.client.DynamicClient;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeWithResponseStreamRequest;
import software.amazon.awssdk.services.lambda.model.InvokeWithResponseStreamResponse;
import software.amazon.awssdk.services.lambda.model.InvokeWithResponseStreamResponseEvent;
import software.amazon.awssdk.services.lambda.model.InvokeWithResponseStreamResponseHandler;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.event.EventStreamReader;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The restJson1 Event Stream fixture (Req 20.3): Lambda {@code InvokeWithResponseStream}. The mock service answers
 * with an {@code application/vnd.amazon.eventstream} body of {@link #CHUNKS} {@code PayloadChunk} events of
 * {@link #CHUNK_BYTES} bytes each (consecutive slices of {@link StreamingWorkloads#content}) and one
 * {@code InvokeComplete} event, every message framed (prelude, headers, payload, both CRCs) by V3's
 * {@code core/eventstream} {@link MessageEncoder}.
 *
 * <p>The client consumes the events one at a time under demand of one. Every operation fails unless it received
 * exactly {@code CHUNKS} chunks totalling {@code CHUNKS * CHUNK_BYTES} bytes and then the completion event;
 * {@link CellWorkload#verify()} additionally compares every chunk byte, in order, with the content and the completion
 * event's decoded {@code LogResult} with the fixture's.
 *
 * <p>Paths: generated-async, dynamic-async and dynamic-sync. On dynamic-sync the call returns the output document
 * holding the Smithy Java event stream reader, which the calling thread pulls one event at a time to its end under
 * the same checks. V3, like V2, generates event-stream operations on the Async Client only, so generated-sync Event
 * Stream cells are inapplicable ({@link CellMatrix#inapplicability}).
 *
 * <p>{@link EventCheck}, {@link #handler}, {@link #readSync} and {@link EventStreamWorkload} are shared with the other
 * Event Stream fixtures.
 */
final class LambdaEventStreamWorkloads implements WorkloadProvider {
    static final String FUNCTION = LambdaWorkloads.FUNCTION;
    static final int CHUNKS = 64;
    static final int CHUNK_BYTES = 1024;
    static final String LOG_RESULT = "YmVuY2htYXJr";
    static final byte[] REQUEST = utf8("{\"benchmark\":true}");
    private static final String PATH = "/2021-11-15/functions/" + FUNCTION + "/response-streaming-invocations";
    private static final ShapeId SERVICE = ShapeId.from("com.amazonaws.lambda#AWSGirApiService");
    private static final String OPERATION = "InvokeWithResponseStream";
    private static final String EVENT_STREAM_MEMBER = "EventStream";

    @Override
    public String name() {
        return "lambda-invokewithresponsestream";
    }

    @Override
    public boolean supports(BenchmarkCell cell) {
        // generated-sync Event Stream cells are inapplicable and never reach a provider (CellMatrix.inapplicability)
        return cell.protocol() == BenchProtocol.REST_JSON_1 && cell.kind() == WorkloadKind.EVENT_STREAM
               && cell.path() != ClientPath.GENERATED_SYNC;
    }

    /** The framed response body: the chunk events then the completion event. */
    static byte[] eventStreamBody() {
        byte[] content = StreamingWorkloads.sharedContent();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (int i = 0; i < CHUNKS; i++) {
            write(body, event("PayloadChunk", "application/octet-stream",
                              Arrays.copyOfRange(content, i * CHUNK_BYTES, (i + 1) * CHUNK_BYTES)));
        }
        write(body, event("InvokeComplete", "application/json", utf8("{\"LogResult\":\"" + LOG_RESULT + "\"}")));
        return body.toByteArray();
    }

    /** An event message of {@code type}; {@code contentType} {@code null} omits the {@code :content-type} header. */
    static Message event(String type, String contentType, byte[] payload) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":message-type", HeaderValue.ofString("event"));
        headers.put(":event-type", HeaderValue.ofString(type));
        if (contentType != null) {
            headers.put(":content-type", HeaderValue.ofString(contentType));
        }
        return new Message(headers, payload);
    }

    /** Appends {@code message}, framed by V3's {@link MessageEncoder}, to {@code out}. */
    static void write(ByteArrayOutputStream out, Message message) {
        ByteBuffer frame = MessageEncoder.encode(message);
        byte[] bytes = new byte[frame.remaining()];
        frame.get(bytes);
        out.writeBytes(bytes);
    }

    @Override
    public ServiceFixture fixture(BenchmarkCell cell) {
        ServiceFixture.MockResponse success = response(200, "application/vnd.amazon.eventstream", eventStreamBody(),
                                                       Map.of("X-Amz-Executed-Version", "$LATEST"));
        ServiceFixture.MockResponse error = response(404, "application/json",
                                                     utf8("{\"Type\":\"User\",\"message\":\"Function not found\"}"),
                                                     Map.of("X-Amzn-ErrorType", "ResourceNotFoundException"));
        ServiceFixture.MockResponse retry = response(500, "application/json",
                                                     utf8("{\"Type\":\"Service\",\"message\":\"injected retry\"}"),
                                                     Map.of("X-Amzn-ErrorType", "ServiceException"));
        return new ServiceFixture() {
            @Override
            public boolean matches(String method, String path, String routingHeader) {
                return "POST".equals(method) && PATH.equals(path);
            }

            @Override
            public boolean matchesBody(byte[] body) {
                return Arrays.equals(REQUEST, body);
            }

            @Override
            public String routingHeaderName() {
                return null;
            }

            @Override
            public MockResponse success() {
                return success;
            }

            @Override
            public MockResponse modeledError() {
                return error;
            }

            @Override
            public MockResponse retryableError() {
                return retry;
            }

            @Override
            public long encodedBytes() {
                return REQUEST.length + (long) success.body().length;
            }
        };
    }

    /**
     * Counts and (with {@code exact}) checks the decoded events of one call; see the class comment. An Event Stream
     * fixture's events are {@link #CHUNKS} content chunks of {@link #CHUNK_BYTES} bytes, then, unless
     * {@code completion} is {@code null} (a stream that just ends), one completion event whose decoded value must
     * equal {@code completion} (with {@code exact}), then, if {@code requiresEnd}, one end event; nothing else, in no
     * other order.
     */
    static final class EventCheck {
        private final boolean exact;
        private final String completion;
        private final boolean requiresEnd;
        private int chunks;
        private long bytes;
        private boolean completed;
        private boolean ended;

        EventCheck(boolean exact) {
            this(exact, LOG_RESULT, false);
        }

        EventCheck(boolean exact, String completion, boolean requiresEnd) {
            this.exact = exact;
            this.completion = completion;
            this.requiresEnd = requiresEnd;
        }

        void chunk(ByteBuffer payload) {
            if (completed) {
                throw new IllegalStateException("chunk after the completion event");
            }
            int n = payload.remaining();
            if (exact && (bytes + n > (long) CHUNKS * CHUNK_BYTES || payload.mismatch(
                ByteBuffer.wrap(StreamingWorkloads.sharedContent(), (int) bytes, n)) != -1)) {
                throw new IllegalStateException("event payload differs from the content at or after offset " + bytes);
            }
            chunks++;
            bytes += n;
        }

        /** The number of chunks received so far. */
        int chunks() {
            return chunks;
        }

        boolean exact() {
            return exact;
        }

        void complete(String value) {
            if (completed || completion == null || exact && !completion.equals(value)) {
                throw new IllegalStateException("unexpected completion event: " + value);
            }
            completed = true;
        }

        void end() {
            if (!requiresEnd || !completed || ended) {
                throw new IllegalStateException("unexpected end event, completed=" + completed + " ended=" + ended);
            }
            ended = true;
        }

        void finish() {
            if (chunks != CHUNKS || bytes != (long) CHUNKS * CHUNK_BYTES || completed != (completion != null)
                || requiresEnd && !ended) {
                throw new IllegalStateException("received " + chunks + " chunks of " + bytes + " bytes, completed="
                                                + completed + ", ended=" + ended + "; expected " + CHUNKS
                                                + " chunks of " + (long) CHUNKS * CHUNK_BYTES + " bytes"
                                                + (completion != null ? " and the completion event" : "")
                                                + (requiresEnd ? " then the end event" : ""));
            }
        }
    }

    /**
     * Reads a Dynamic Client sync output's event stream member {@code member} to its end on the calling thread, one
     * event per pull, feeding each event document to {@code onEvent}, then runs {@link EventCheck#finish()}.
     */
    static void readSync(Document output, String member, EventCheck check, BiConsumer<EventCheck, Document> onEvent) {
        Document stream = output.getMember(member);
        if (stream == null) {
            throw new IllegalStateException("output has no event stream member " + member + ": " + output);
        }
        try (EventStream<? extends SerializableStruct> events = stream.asEventStream()) {
            EventStreamReader<? extends SerializableStruct> reader = events.asReader();
            for (SerializableStruct event = reader.read(); event != null; event = reader.read()) {
                onEvent.accept(check, (Document) event);
            }
        }
        check.finish();
    }

    /**
     * A handler that subscribes to the events with demand of one, feeds each to {@code onEvent} and completes
     * {@code done} after the call completes and {@link EventCheck#finish()} passes.
     */
    static <R, E> EventStreamResponseHandler<R, E> handler(EventCheck check, BiConsumer<EventCheck, E> onEvent,
                                                           CompletableFuture<Void> done) {
        return new EventStreamResponseHandler<>() {
            @Override
            public void responseReceived(R response) {
                // the initial response has no member the workload checks
            }

            @Override
            public void onEventStream(SdkPublisher<E> publisher) {
                publisher.subscribe(new Subscriber<E>() {
                    private Subscription subscription;

                    @Override
                    public void onSubscribe(Subscription s) {
                        subscription = s;
                        s.request(1);
                    }

                    @Override
                    public void onNext(E event) {
                        try {
                            onEvent.accept(check, event);
                        } catch (RuntimeException e) {
                            subscription.cancel();
                            done.completeExceptionally(e);
                            return;
                        }
                        subscription.request(1);
                    }

                    @Override
                    public void onError(Throwable t) {
                        done.completeExceptionally(t);
                    }

                    @Override
                    public void onComplete() {
                        // the call's end is reported through complete()/exceptionOccurred()
                    }
                });
            }

            @Override
            public void exceptionOccurred(Throwable throwable) {
                done.completeExceptionally(throwable);
            }

            @Override
            public void complete() {
                try {
                    check.finish();
                    done.complete(null);
                } catch (RuntimeException e) {
                    done.completeExceptionally(e);
                }
            }
        };
    }

    @Override
    public CellWorkload create(BenchmarkCell cell, URI endpoint, Options options) {
        AutoCloseable transport = cell.transport().create(cell.concurrency());
        try {
            AutoCloseable client;
            Function<Boolean, CompletableFuture<Void>> call;
            Path model = options.models().getOrDefault(
                "lambda", Path.of("../../services/lambda/src/main/smithy/model.json"));
            Map<String, Document> members = new LinkedHashMap<>();
            members.put("FunctionName", Document.of(FUNCTION));
            members.put("Payload", Document.of(ByteBuffer.wrap(REQUEST)));
            Document input = Document.of(members);
            if (cell.path() == ClientPath.DYNAMIC_SYNC) {
                DynamicClient dynamic = DynamicClient.builder().model(model).serviceId(SERVICE)
                    .region(Region.US_EAST_1).credentialsProvider(DynamoDbWorkloads.credentials())
                    .endpointOverride(endpoint).retryStrategy(DynamoDbWorkloads.retryStrategy())
                    .httpClient((SdkSyncHttpClient) transport).build();
                return EventStreamWorkload.sync(cell, transport, dynamic, exact -> readSync(
                    dynamic.call(OPERATION, input), EVENT_STREAM_MEMBER, new EventCheck(exact),
                    LambdaEventStreamWorkloads::onDynamicEvent));
            }
            if (cell.path().isDynamic()) {
                DynamicAsyncClient dynamic = DynamicAsyncClient.builder().model(model).serviceId(SERVICE)
                    .region(Region.US_EAST_1).credentialsProvider(DynamoDbWorkloads.credentials())
                    .endpointOverride(endpoint).retryStrategy(DynamoDbWorkloads.retryStrategy())
                    .httpClient((SdkAsyncHttpClient) transport).build();
                client = dynamic;
                call = exact -> {
                    CompletableFuture<Void> done = new CompletableFuture<>();
                    EventStreamResponseHandler<Document, Document> handler =
                        handler(new EventCheck(exact), LambdaEventStreamWorkloads::onDynamicEvent, done);
                    dynamic.callAsync(OPERATION, input, handler).whenComplete((r, t) -> {
                        if (t != null) {
                            done.completeExceptionally(t);
                        }
                    });
                    return done;
                };
            } else {
                LambdaAsyncClient generated = LambdaAsyncClient.builder().region(Region.US_EAST_1)
                    .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                    .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkAsyncHttpClient) transport)
                    .build();
                InvokeWithResponseStreamRequest request = InvokeWithResponseStreamRequest.builder()
                    .functionName(FUNCTION).payload(SdkBytes.fromByteArray(REQUEST)).build();
                client = generated;
                call = exact -> {
                    CompletableFuture<Void> done = new CompletableFuture<>();
                    EventStreamResponseHandler<InvokeWithResponseStreamResponse, InvokeWithResponseStreamResponseEvent>
                        handler = handler(new EventCheck(exact), LambdaEventStreamWorkloads::onGeneratedEvent, done);
                    generated.invokeWithResponseStream(request, new InvokeWithResponseStreamResponseHandler() {
                        @Override
                        public void responseReceived(InvokeWithResponseStreamResponse response) {
                            handler.responseReceived(response);
                        }

                        @Override
                        public void onEventStream(SdkPublisher<InvokeWithResponseStreamResponseEvent> publisher) {
                            handler.onEventStream(publisher);
                        }

                        @Override
                        public void exceptionOccurred(Throwable throwable) {
                            handler.exceptionOccurred(throwable);
                        }

                        @Override
                        public void complete() {
                            handler.complete();
                        }
                    }).whenComplete((r, t) -> {
                        if (t != null) {
                            done.completeExceptionally(t);
                        }
                    });
                    return done;
                };
            }
            return EventStreamWorkload.async(cell, transport, client, call);
        } catch (RuntimeException e) {
            try {
                transport.close();
            } catch (Exception ignored) {
                // best effort while failing construction
            }
            throw e;
        }
    }

    private static void onGeneratedEvent(EventCheck check, InvokeWithResponseStreamResponseEvent event) {
        switch (event.type()) {
            case PAYLOAD_CHUNK -> check.chunk(event.payloadChunk().payload().asByteBuffer());
            case INVOKE_COMPLETE -> check.complete(event.invokeComplete().logResult());
            default -> throw new IllegalStateException("unexpected event " + event);
        }
    }

    private static void onDynamicEvent(EventCheck check, Document event) {
        Document chunk = event.getMember("PayloadChunk");
        Document complete = event.getMember("InvokeComplete");
        if (chunk != null) {
            check.chunk(chunk.getMember("Payload").asBlob());
        } else if (complete != null) {
            Document log = complete.getMember("LogResult");
            check.complete(log == null ? null : log.asString());
        } else {
            throw new IllegalStateException("unexpected event " + event);
        }
    }

    /**
     * An Event Stream cell's client workload: one call whose events are checked as they arrive, either asynchronously
     * (the generated and Dynamic Async Clients' response handler) or on the calling thread (the Dynamic Client's sync
     * output event stream reader).
     */
    static final class EventStreamWorkload implements CellWorkload {
        private final BenchmarkCell cell;
        private final AutoCloseable transport;
        private final AutoCloseable client;
        private final Function<Boolean, CompletableFuture<Void>> asyncCall;
        private final Consumer<Boolean> syncCall;

        private EventStreamWorkload(BenchmarkCell cell, AutoCloseable transport, AutoCloseable client,
                                    Function<Boolean, CompletableFuture<Void>> asyncCall, Consumer<Boolean> syncCall) {
            this.cell = cell;
            this.transport = transport;
            this.client = client;
            this.asyncCall = asyncCall;
            this.syncCall = syncCall;
        }

        /** An async workload; {@code call} takes {@code exact} and completes after the checked call ends. */
        static EventStreamWorkload async(BenchmarkCell cell, AutoCloseable transport, AutoCloseable client,
                                         Function<Boolean, CompletableFuture<Void>> call) {
            return new EventStreamWorkload(cell, transport, client, call, null);
        }

        /** A sync workload; {@code call} takes {@code exact} and returns after the checked call ends. */
        static EventStreamWorkload sync(BenchmarkCell cell, AutoCloseable transport, AutoCloseable client,
                                        Consumer<Boolean> call) {
            return new EventStreamWorkload(cell, transport, client, null, call);
        }

        @Override
        public void call() {
            if (syncCall == null) {
                throw new UnsupportedOperationException("async path");
            }
            try {
                syncCall.accept(false);
            } catch (RuntimeException e) {
                throw new UnexpectedOutcomeException(cell, e);
            }
        }

        @Override
        public CompletableFuture<Void> callAsync() {
            if (asyncCall == null) {
                throw new UnsupportedOperationException("sync path");
            }
            return asyncCall.apply(false).exceptionally(t -> {
                Throwable cause = t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
                throw new UnexpectedOutcomeException(cell, cause);
            });
        }

        @Override
        public boolean isAsync() {
            return asyncCall != null;
        }

        @Override
        public String transportClass() {
            return transport.getClass().getName();
        }

        @Override
        public void verify() {
            if (syncCall != null) {
                syncCall.accept(true);
                return;
            }
            try {
                asyncCall.apply(true).join();
            } catch (CompletionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                throw cause instanceof RuntimeException r ? r : e;
            }
        }

        @Override
        public void close() throws Exception {
            try {
                client.close();
            } finally {
                transport.close();
            }
        }
    }
}
