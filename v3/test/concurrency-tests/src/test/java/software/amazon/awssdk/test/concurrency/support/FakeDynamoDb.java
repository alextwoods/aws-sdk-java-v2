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

package software.amazon.awssdk.test.concurrency.support;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;

/**
 * A deterministic in-memory DynamoDB (awsJson1.0) transport, sync and async, so the generated clients run their real
 * pipelines (serialization, endpoint, SigV4 signing, retries, interceptors, deserialization) without a network.
 *
 * <p>Served operations:
 * <ul>
 *   <li>{@code PutItem} stores the item under its table and {@code id} string attribute.</li>
 *   <li>{@code GetItem} returns the stored item, else an echo item {@code {id, echo: "<table>/<id>"}}, so a caller can
 *       check it received the answer to its own request.</li>
 *   <li>{@code ListTables} pages through {@link #tables(int)} names ({@code Limit} per page, by
 *       {@code ExclusiveStartTableName}).</li>
 *   <li>{@code DescribeTable} answers {@code CREATING} for the first {@link #creatingPolls(int)} polls of each table,
 *       then {@code ACTIVE}.</li>
 * </ul>
 * The first {@link #failFirstAttempts(int, String)} attempts of each distinct request (same operation, table and key)
 * answer a scripted error, so a retried call is recognizable. A {@linkplain #onRequest blocking hook} (sync) or a
 * {@linkplain #gate non-blocking gate} (async) holds requests inside the transport, which is where suites prove
 * overlap. Async responses are delivered on a dedicated "fake event loop" pool. Every exchange is counted.
 */
public final class FakeDynamoDb implements SdkSyncHttpClient, SdkAsyncHttpClient {
    public static final URI ENDPOINT = URI.create("https://dynamodb.concurrency.test");
    public static final String EVENT_LOOP = "fake-dynamodb-event-loop";

    private static final Pattern TABLE = Pattern.compile("\"TableName\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\\{\\s*\"S\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern LIMIT = Pattern.compile("\"Limit\"\\s*:\\s*(\\d+)");
    private static final Pattern START = Pattern.compile("\"ExclusiveStartTableName\"\\s*:\\s*\"([^\"]*)\"");

    private final HttpTransportMode mode;
    private final ExecutorService eventLoop;
    private final Map<String, String> items = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> describePolls = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> operations = new ConcurrentHashMap<>();
    private final Queue<Received> received = new ConcurrentLinkedQueue<>();
    private final AtomicInteger openExchanges = new AtomicInteger();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger maxInFlight = new AtomicInteger();
    private volatile int tableCount;
    private volatile int creatingPolls;
    private volatile int failFirstAttempts;
    private volatile String failureErrorType = "InternalServerError";
    private volatile Consumer<Received> onRequest = r -> { };
    private volatile Function<Received, CompletableFuture<Void>> gate = r -> CompletableFuture.completedFuture(null);

    private FakeDynamoDb(HttpTransportMode mode) {
        this.mode = mode;
        AtomicInteger ids = new AtomicInteger();
        this.eventLoop = mode == HttpTransportMode.ASYNC
                         ? Executors.newFixedThreadPool(2, r -> {
                             Thread t = new Thread(r, EVENT_LOOP + "-" + ids.getAndIncrement());
                             t.setDaemon(true);
                             return t;
                         })
                         : null;
    }

    public static FakeDynamoDb sync() {
        return new FakeDynamoDb(HttpTransportMode.SYNC);
    }

    public static FakeDynamoDb async() {
        return new FakeDynamoDb(HttpTransportMode.ASYNC);
    }

    /** Serves {@code count} table names {@code table-000}, {@code table-001}, ... to {@code ListTables}. */
    public FakeDynamoDb tables(int count) {
        this.tableCount = count;
        return this;
    }

    /** Answers {@code CREATING} to the first {@code polls} {@code DescribeTable} requests of each table. */
    public FakeDynamoDb creatingPolls(int polls) {
        this.creatingPolls = polls;
        return this;
    }

    /** Fails the first {@code count} attempts of each distinct request with a 4xx/5xx {@code errorType} error. */
    public FakeDynamoDb failFirstAttempts(int count, String errorType) {
        this.failFirstAttempts = count;
        this.failureErrorType = Objects.requireNonNull(errorType, "errorType");
        return this;
    }

    /** A blocking hook run inside each sync exchange (and each async exchange, on the calling thread). */
    public FakeDynamoDb onRequest(Consumer<Received> hook) {
        this.onRequest = Objects.requireNonNull(hook, "hook");
        return this;
    }

    /** A non-blocking gate: each async response is delivered once the request's gate future completes. */
    public FakeDynamoDb gate(Function<Received, CompletableFuture<Void>> gate) {
        this.gate = Objects.requireNonNull(gate, "gate");
        return this;
    }

    /** Every received request, in arrival order. */
    public List<Received> received() {
        return new ArrayList<>(received);
    }

    /** The number of requests of {@code operation} received. */
    public long count(String operation) {
        LongAdder adder = operations.get(operation);
        return adder == null ? 0 : adder.sum();
    }

    /** Exchanges started and not yet closed (sync) or terminated (async). */
    public int openExchanges() {
        return openExchanges.get();
    }

    /** The largest number of exchanges inside the transport at the same time. */
    public int maxInFlight() {
        return maxInFlight.get();
    }

    /** The stored item JSON of {@code table}/{@code id}, or null. */
    public String item(String table, String id) {
        return items.get(table + "/" + id);
    }

    public int storedItems() {
        return items.size();
    }

    // ---- sync ------------------------------------------------------------------------------------------------------

    @Override
    public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
        requireMode(HttpTransportMode.SYNC);
        openExchanges.incrementAndGet();
        return new SyncHttpExchange() {
            private boolean closed;

            @Override
            public SdkHttpResponse execute() {
                Received r = receive(request);
                enter();
                try {
                    onRequest.accept(r);
                    return serve(r);
                } finally {
                    inFlight.decrementAndGet();
                }
            }

            @Override
            public void abort() {
            }

            @Override
            public void close() {
                if (!closed) {
                    closed = true;
                    openExchanges.decrementAndGet();
                }
            }
        };
    }

    // ---- async -----------------------------------------------------------------------------------------------------

    @Override
    public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                     HttpExecutionOptions options) {
        requireMode(HttpTransportMode.ASYNC);
        openExchanges.incrementAndGet();
        AtomicInteger open = new AtomicInteger(1);
        Runnable release = () -> {
            if (open.getAndSet(0) == 1) {
                openExchanges.decrementAndGet();
                inFlight.decrementAndGet();
            }
        };
        AsyncExchangeCompletion exchange = new AsyncExchangeCompletion(clientName(), (reason, failure) -> {
            release.run();
            handler.onError(failure);
        });
        exchange.completion().whenComplete((ignored, failure) -> release.run());
        Received r = receive(request);
        enter();
        exchange.enter(HttpPhase.RESPONSE_CONSUMPTION);
        onRequest.accept(r);
        gate.apply(r).whenCompleteAsync((ignored, failure) -> {
            if (failure != null) {
                if (exchange.fail(failure)) {
                    handler.onError(exchange.asTransportException(failure));
                }
                return;
            }
            if (exchange.isDone()) {
                return;
            }
            SdkHttpResponse response = serve(r);
            byte[] body = response.body().asByteArray();
            handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
            handler.onStream(publisherOf(body));
            exchange.complete();
        }, eventLoop);
        return exchange;
    }

    private static Publisher<ByteBuffer> publisherOf(byte[] bytes) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (done) {
                    return;
                }
                done = true;
                if (bytes.length > 0) {
                    subscriber.onNext(ByteBuffer.wrap(bytes).asReadOnlyBuffer());
                }
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }

    // ---- service ---------------------------------------------------------------------------------------------------

    private void enter() {
        maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
    }

    private Received receive(SdkHttpRequest request) {
        ByteBuffer buffer = request.body().buffered().asByteBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String target = request.headers().firstValue("X-Amz-Target").orElse("");
        String body = new String(bytes, StandardCharsets.UTF_8);
        Received r = new Received(target.substring(target.indexOf('.') + 1), body, request,
                                  Thread.currentThread().getName());
        received.add(r);
        operations.computeIfAbsent(r.operation(), k -> new LongAdder()).increment();
        return r;
    }

    private SdkHttpResponse serve(Received r) {
        if (failFirstAttempts > 0) {
            String key = r.operation() + "|" + r.table() + "|" + r.id();
            int attempt = attempts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
            if (attempt <= failFirstAttempts) {
                int status = failureErrorType.contains("Throttl") || failureErrorType.contains("Exceeded") ? 400 : 500;
                return json(status, "{\"__type\":\"com.amazonaws.dynamodb.v20120810#" + failureErrorType
                                    + "\",\"message\":\"injected attempt " + attempt + "\"}");
            }
        }
        return switch (r.operation()) {
            case "PutItem" -> {
                items.put(r.table() + "/" + r.id(), object(r.body(), "Item"));
                yield json(200, "{}");
            }
            case "GetItem" -> {
                String stored = items.get(r.table() + "/" + r.id());
                String item = stored != null ? stored
                                             : "{\"id\":{\"S\":\"" + r.id() + "\"},\"echo\":{\"S\":\"" + r.table() + "/"
                                               + r.id() + "\"}}";
                yield json(200, "{\"Item\":" + item + "}");
            }
            case "ListTables" -> json(200, listTables(r.body()));
            case "DescribeTable" -> {
                int poll = describePolls.computeIfAbsent(r.table(), k -> new AtomicInteger()).incrementAndGet();
                String status = poll <= creatingPolls ? "CREATING" : "ACTIVE";
                yield json(200, "{\"Table\":{\"TableName\":\"" + r.table() + "\",\"TableStatus\":\"" + status
                                + "\",\"ItemCount\":" + poll + "}}");
            }
            default -> json(400, "{\"__type\":\"com.amazon.coral.service#UnknownOperationException\"}");
        };
    }

    private String listTables(String body) {
        int limit = find(LIMIT, body) == null ? 100 : Integer.parseInt(find(LIMIT, body));
        String startAfter = find(START, body);
        int from = startAfter == null ? 0 : Integer.parseInt(startAfter.substring("table-".length())) + 1;
        int to = Math.min(tableCount, from + limit);
        StringBuilder out = new StringBuilder("{\"TableNames\":[");
        for (int i = from; i < to; i++) {
            out.append(i == from ? "" : ",").append('"').append(tableName(i)).append('"');
        }
        out.append(']');
        if (to < tableCount) {
            out.append(",\"LastEvaluatedTableName\":\"").append(tableName(to - 1)).append('"');
        }
        return out.append('}').toString();
    }

    /** The name of the {@code index}-th table {@code ListTables} serves. */
    public static String tableName(int index) {
        return String.format("table-%03d", index);
    }

    private static SdkHttpResponse json(int status, String body) {
        return SdkHttpResponse.builder().statusCode(status)
                              .addHeader("Content-Type", "application/x-amz-json-1.0")
                              .body(HttpBody.fromUtf8(body)).build();
    }

    /** The JSON object value of the top-level member {@code name} (values in these suites contain no braces). */
    private static String object(String json, String name) {
        int key = json.indexOf("\"" + name + "\"");
        int open = json.indexOf('{', key);
        int depth = 0;
        for (int i = open; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return json.substring(open, i + 1);
            }
        }
        throw new IllegalArgumentException("no object member " + name + " in " + json);
    }

    private static String find(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private void requireMode(HttpTransportMode expected) {
        if (mode != expected) {
            throw new IllegalStateException("this fake is " + mode);
        }
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return HttpClientCapabilities.builder().mode(mode).addProtocol(Protocol.HTTP1_1)
                                     .maxBufferedBytesPerExchange(1 << 20)
                                     .cancellationTimeout(Duration.ofSeconds(1)).build();
    }

    @Override
    public String clientName() {
        return "FakeDynamoDb";
    }

    /** Stops the async event loop; the fake is caller-owned, so clients never call this. */
    public void shutdown() {
        if (eventLoop != null) {
            eventLoop.shutdownNow();
        }
    }

    @Override
    public void close() {
    }

    /**
     * One received request: its operation, JSON body, transport request and the thread that handed it to the transport.
     */
    public record Received(String operation, String body, SdkHttpRequest request, String thread) {
        /** The {@code TableName} of the request, or null. */
        public String table() {
            return find(TABLE, body);
        }

        /** The {@code id} string attribute of its key or item, or null. */
        public String id() {
            return find(ID, body);
        }

        /** The first value of header {@code name}, or null. */
        public String header(String name) {
            return request.headers().firstValue(name).orElse(null);
        }
    }
}
