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

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Requirements 7.8, 7.9, 8.6, 8.7 and 16.5: a synthetic awsJson1_0 service with a {@code @paginated} and a
 * {@code @waitable} operation gets V2-shaped paginators and waiters. A probe written against the generated public API
 * pages across three pages (sync and items), drives the async publisher one {@code request(1)} at a time and
 * cancels it, and runs sync and async waiters to success, failure and retry-then-success over a scripted transport
 * with millisecond delays; the async waiter returns before any delay elapses.
 */
class GeneratedPaginatorWaiterTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.things";

    private static final String MODEL = """
        $version: "2"
        namespace example.things

        use smithy.waiters#waitable

        @aws.protocols#awsJson1_0
        @aws.api#service(sdkId: "Things", endpointPrefix: "things")
        @paginated(inputToken: "nextToken", outputToken: "nextToken", pageSize: "maxResults")
        service Things {
            version: "2024-01-01"
            operations: [ListThings, GetThing]
        }

        @auth([])
        @readonly
        @paginated(items: "things")
        operation ListThings {
            input := {
                nextToken: String
                maxResults: Integer
            }
            output := {
                nextToken: String
                things: ThingList
            }
        }

        list ThingList {
            member: Thing
        }

        structure Thing {
            name: String
        }

        list PartList {
            member: Part
        }

        structure Part {
            state: String
        }

        @auth([])
        @readonly
        @waitable(
            ThingReady: {
                minDelay: 2
                maxDelay: 120
                acceptors: [
                    {
                        state: "success"
                        matcher: { output: { path: "status", expected: "READY", comparator: "stringEquals" } }
                    }
                    {
                        state: "failure"
                        matcher: { output: { path: "status", expected: "BROKEN", comparator: "stringEquals" } }
                    }
                    {
                        state: "success"
                        matcher: { output: { path: "parts[].state", expected: "OK", comparator: "allStringEquals" } }
                    }
                    {
                        state: "success"
                        matcher: {
                            inputOutput: {
                                path: "length(output.parts) > `2`", expected: "true", comparator: "booleanEquals"
                            }
                        }
                    }
                    {
                        state: "retry"
                        matcher: { errorType: "ThingNotFound" }
                    }
                ]
            }
        )
        operation GetThing {
            input := {
                @required
                name: String
            }
            output := {
                name: String
                status: String
                parts: PartList
            }
            errors: [ThingNotFound]
        }

        @error("client")
        structure ThingNotFound {
            message: String
        }
        """;

    private static final String PROBE = """
        package example.probe;

        import java.net.URI;
        import java.time.Duration;
        import java.util.ArrayList;
        import java.util.List;
        import java.util.concurrent.CompletableFuture;
        import java.util.concurrent.CompletionException;
        import java.util.concurrent.ConcurrentLinkedQueue;
        import java.util.function.IntSupplier;
        import org.reactivestreams.Subscriber;
        import org.reactivestreams.Subscription;
        import software.amazon.awssdk.core.exception.SdkClientException;
        import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
        import software.amazon.awssdk.core.waiters.WaiterResponse;
        import software.amazon.awssdk.http.SdkAsyncHttpClient;
        import software.amazon.awssdk.http.SdkSyncHttpClient;
        import software.amazon.awssdk.regions.Region;
        import PKG.ThingsAsyncClient;
        import PKG.ThingsClient;
        import PKG.model.GetThingRequest;
        import PKG.model.GetThingResponse;
        import PKG.model.ListThingsRequest;
        import PKG.model.ListThingsResponse;
        import PKG.model.Thing;
        import PKG.paginators.ListThingsIterable;
        import PKG.paginators.ListThingsPublisher;
        import PKG.waiters.ThingsAsyncWaiter;
        import PKG.waiters.ThingsWaiter;

        public final class Probe {
            private static final URI ENDPOINT = URI.create("https://things.test");
            static final WaiterOverrideConfiguration FAST = WaiterOverrideConfiguration.builder()
                .minDelay(Duration.ofMillis(1)).maxDelay(Duration.ofMillis(5)).waitTimeout(Duration.ofSeconds(10))
                .build();

            private Probe() {
            }

            static ThingsClient sync(SdkSyncHttpClient http) {
                return ThingsClient.builder().region(Region.of("us-east-1")).endpointOverride(ENDPOINT)
                                   .httpClient(http).build();
            }

            static ThingsAsyncClient async(SdkAsyncHttpClient http) {
                return ThingsAsyncClient.builder().region(Region.of("us-east-1")).endpointOverride(ENDPOINT)
                                        .httpClient(http).build();
            }

            public static String syncPages(SdkSyncHttpClient http) {
                try (ThingsClient client = sync(http)) {
                    ListThingsIterable pages = client.listThingsPaginator(ListThingsRequest.builder().build());
                    List<String> tokens = new ArrayList<>();
                    for (ListThingsResponse page : pages) {
                        tokens.add(String.valueOf(page.nextToken()));
                    }
                    List<String> names = new ArrayList<>();
                    for (Thing thing : client.listThingsPaginator(r -> r.maxResults(1)).things()) {
                        names.add(thing.name());
                    }
                    return tokens + "|" + names + "|" + pages.stream().count();
                }
            }

            /** Requests one page at a time, reporting the transport's ListThings call count after each step. */
            public static String asyncPages(SdkAsyncHttpClient http, IntSupplier calls) throws Exception {
                try (ThingsAsyncClient client = async(http)) {
                    ListThingsPublisher pages = client.listThingsPaginator(ListThingsRequest.builder().build());
                    ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();
                    CompletableFuture<Subscription> subscribed = new CompletableFuture<>();
                    pages.subscribe(new Subscriber<ListThingsResponse>() {
                        @Override
                        public void onSubscribe(Subscription s) {
                            subscribed.complete(s);
                        }

                        @Override
                        public void onNext(ListThingsResponse page) {
                            received.add(String.valueOf(page.nextToken()));
                        }

                        @Override
                        public void onError(Throwable t) {
                            received.add("error:" + t);
                        }

                        @Override
                        public void onComplete() {
                            received.add("complete");
                        }
                    });
                    Subscription subscription = subscribed.join();
                    List<String> steps = new ArrayList<>();
                    steps.add("subscribed=" + calls.getAsInt());
                    subscription.request(1);
                    awaitSize(received, 1);
                    steps.add("request1=" + calls.getAsInt());
                    subscription.request(1);
                    awaitSize(received, 2);
                    steps.add("request2=" + calls.getAsInt());
                    subscription.cancel();
                    subscription.request(1);
                    Thread.sleep(50);
                    steps.add("cancelled=" + calls.getAsInt());
                    steps.add("received=" + received);
                    return String.join(",", steps);
                }
            }

            private static void awaitSize(ConcurrentLinkedQueue<String> queue, int size) throws Exception {
                long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
                while (queue.size() < size && System.nanoTime() < deadline) {
                    Thread.sleep(1);
                }
            }

            public static String syncWaiter(SdkSyncHttpClient http) {
                try (ThingsClient client = sync(http)) {
                    List<String> results = new ArrayList<>();
                    try (ThingsWaiter waiter = client.waiter()) {
                        WaiterResponse<GetThingResponse> ready = waiter.waitUntilThingReady(r -> r.name("eventually"));
                        results.add(ready.matched().response().get().status() + "@" + ready.attemptsExecuted());
                    }
                    ThingsWaiter waiter = ThingsWaiter.builder().client(client).overrideConfiguration(FAST).build();
                    try {
                        waiter.waitUntilThingReady(r -> r.name("broken"));
                        results.add("none");
                    } catch (SdkClientException e) {
                        results.add("failed:" + e.getMessage().contains("failure state"));
                    }
                    results.add("parts@" + waiter.waitUntilThingReady(r -> r.name("parts")).attemptsExecuted());
                    results.add("many@" + waiter.waitUntilThingReady(r -> r.name("many")).attemptsExecuted());
                    waiter.close();
                    // Closing a waiter leaves the borrowed client open.
                    results.add(client.getThing(r -> r.name("ready")).status());
                    return String.join(",", results);
                }
            }

            /** Runs one wait whose delays complete only when the test releases them. */
            public static String asyncWaiter(SdkAsyncHttpClient http) throws Exception {
                try (ThingsAsyncClient client = async(http)) {
                    List<CompletableFuture<Void>> delays = new ArrayList<>();
                    List<String> results = new ArrayList<>();
                    try (ThingsAsyncWaiter waiter = ThingsAsyncWaiter.builder().client(client).retryScheduler(d -> {
                            CompletableFuture<Void> delay = new CompletableFuture<>();
                            synchronized (delays) {
                                delays.add(delay);
                            }
                            return delay;
                        }).build()) {
                        CompletableFuture<WaiterResponse<GetThingResponse>> wait =
                            waiter.waitUntilThingReady(r -> r.name("eventually-async"));
                        // The first poll is retried: the wait is pending on the scheduler, not on this thread.
                        results.add("returned=" + !wait.isDone());
                        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
                        while (System.nanoTime() < deadline) {
                            synchronized (delays) {
                                if (!delays.isEmpty()) {
                                    break;
                                }
                            }
                            Thread.sleep(1);
                        }
                        results.add("pendingOnDelay=" + !wait.isDone());
                        while (!wait.isDone()) {
                            CompletableFuture<Void> next;
                            synchronized (delays) {
                                next = delays.isEmpty() ? null : delays.remove(0);
                            }
                            if (next != null) {
                                next.complete(null);
                            }
                            Thread.sleep(1);
                        }
                        WaiterResponse<GetThingResponse> ready = wait.join();
                        results.add(ready.matched().response().get().status() + "@" + ready.attemptsExecuted());
                    }
                    // client.waiter() schedules on the client's own retry scheduler.
                    try (ThingsAsyncWaiter waiter = client.waiter()) {
                        try {
                            waiter.waitUntilThingReady(GetThingRequest.builder().name("broken").build(), FAST).join();
                            results.add("none");
                        } catch (CompletionException e) {
                            results.add("failed:" + (e.getCause() instanceof SdkClientException));
                        }
                        results.add("retried@" + waiter.waitUntilThingReady(
                            GetThingRequest.builder().name("eventually-own").build(), FAST)
                                                       .join().attemptsExecuted());
                    }
                    results.add(client.getThing(r -> r.name("ready")).join().status());
                    return String.join(",", results);
                }
            }
        }
        """.replace("PKG", PACKAGE);

    @TempDir
    static Path temp;

    private static AwsCodegenResult result;
    private static URLClassLoader loader;

    @BeforeAll
    static void generateAndCompile() throws Exception {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("things.smithy"), MODEL, StandardCharsets.UTF_8);
        result = AwsCodegen.generate(new AwsCodegenRequest("example.things#Things", PACKAGE, models,
                                                           temp.resolve("out")), List.of());
        Path sources = temp.resolve("sources");
        copyTree(result.sourceRoot(), sources);
        Path probe = Files.createDirectories(sources.resolve("example/probe")).resolve("Probe.java");
        Files.writeString(probe, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sources, temp.resolve("classes")).loader();
    }

    @AfterAll
    static void close() throws Exception {
        loader.close();
    }

    @Test
    void thePaginatorAndWaiterTypesAreGenerated() {
        String root = PACKAGE.replace('.', '/') + "/";
        for (String file : List.of("paginators/ListThingsIterable.java", "paginators/ListThingsPublisher.java",
                                   "waiters/ThingsWaiter.java", "waiters/ThingsAsyncWaiter.java",
                                   "waiters/DefaultThingsWaiter.java", "waiters/DefaultThingsAsyncWaiter.java")) {
            assertTrue(result.generatedFiles().stream().anyMatch(p -> p.toString().replace('\\', '/')
                                                                         .equals(root + file)),
                       root + file + " was not generated: " + result.generatedFiles());
        }
        assertFalse(result.generatedFiles().stream().anyMatch(p -> p.toString().contains("GetThingIterable")),
                    "only paginated operations get paginators");
    }

    @Test
    void theSyncPaginatorFollowsTokensAcrossThreePagesLazily() throws Exception {
        ScriptedHttp http = new ScriptedHttp(HttpTransportMode.SYNC);
        assertEquals("[t1, t2, null]|[a, b, c]|3", probe("syncPages", http));
        assertEquals(9, http.listCalls.get(), "three pages for each of three iterations");
    }

    @Test
    void theAsyncPaginatorCallsOncePerDemandAndStopsOnCancel() throws Exception {
        ScriptedHttp http = new ScriptedHttp(HttpTransportMode.ASYNC);
        Method probe = Class.forName("example.probe.Probe", true, loader)
                            .getMethod("asyncPages", SdkAsyncHttpClient.class, java.util.function.IntSupplier.class);
        String steps = invoke(probe, http, (java.util.function.IntSupplier) http.listCalls::get);
        assertEquals("subscribed=0,request1=1,request2=2,cancelled=2,received=[t1, t2]", steps);
    }

    @Test
    void theSyncWaiterSucceedsFailsAndRetries() throws Exception {
        ScriptedHttp http = new ScriptedHttp(HttpTransportMode.SYNC);
        // "eventually" uses the modeled 2s minimum delay once: NotFound, then READY.
        assertEquals("READY@2,failed:true,parts@1,many@1,READY", probe("syncWaiter", http));
        assertFalse(http.closed);
    }

    @Test
    void theAsyncWaiterNeverBlocksTheCaller() throws Exception {
        ScriptedHttp http = new ScriptedHttp(HttpTransportMode.ASYNC);
        assertEquals("returned=true,pendingOnDelay=true,READY@3,failed:true,retried@3,READY",
                     probe("asyncWaiter", http));
        assertFalse(http.closed);
    }

    private static String probe(String method, Object http) throws Exception {
        Class<?> parameter = http instanceof ScriptedHttp s && s.mode == HttpTransportMode.SYNC
                             ? SdkSyncHttpClient.class : SdkAsyncHttpClient.class;
        return invoke(Class.forName("example.probe.Probe", true, loader).getMethod(method, parameter), http);
    }

    private static String invoke(Method method, Object... arguments) throws Exception {
        try {
            return (String) method.invoke(null, arguments);
        } catch (InvocationTargetException e) {
            throw new AssertionError("Probe." + method.getName() + " failed", e.getCause());
        }
    }

    private static void copyTree(Path from, Path to) throws Exception {
        try (var walk = Files.walk(from)) {
            for (Path source : walk.toList()) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
    }

    /**
     * A transport scripted by operation and request. {@code ListThings} pages {@code a}, {@code b}, {@code c} with
     * tokens {@code t1}, {@code t2} and none. {@code GetThing} by name: {@code eventually*} is not found until its
     * attempt (two for the sync one, three otherwise) and then {@code READY}; {@code broken} is {@code BROKEN};
     * {@code parts} has only {@code OK} parts; {@code many} has three parts; {@code ready} is {@code READY}.
     */
    static final class ScriptedHttp implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final AtomicInteger listCalls = new AtomicInteger();
        final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
        final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
        final HttpTransportMode mode;
        volatile boolean closed;

        ScriptedHttp(HttpTransportMode mode) {
            this.mode = mode;
        }

        private SdkHttpResponse respond(SdkHttpRequest request) {
            requests.add(request);
            ByteBuffer buffer = request.body().buffered().asByteBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            String body = new String(bytes, StandardCharsets.UTF_8);
            String target = request.headers().firstValue("X-Amz-Target").orElse("");
            if (target.endsWith("ListThings")) {
                listCalls.incrementAndGet();
                if (body.contains("\"t2\"")) {
                    return ok("{\"things\":[{\"name\":\"c\"}]}");
                }
                if (body.contains("\"t1\"")) {
                    return ok("{\"nextToken\":\"t2\",\"things\":[{\"name\":\"b\"}]}");
                }
                return ok("{\"nextToken\":\"t1\",\"things\":[{\"name\":\"a\"}]}");
            }
            String name = body.replaceAll(".*\"name\"\\s*:\\s*\"([^\"]*)\".*", "$1");
            int attempt = attempts.computeIfAbsent(name, n -> new AtomicInteger()).incrementAndGet();
            if (name.startsWith("eventually")) {
                int readyAt = name.equals("eventually") ? 2 : 3;
                if (attempt < readyAt) {
                    return SdkHttpResponse.builder().statusCode(400)
                                          .body(HttpBody.fromUtf8("{\"__type\":\"example.things#ThingNotFound\","
                                                                  + "\"message\":\"not yet\"}"))
                                          .build();
                }
                return ok("{\"status\":\"READY\"}");
            }
            return switch (name) {
                case "broken" -> ok("{\"status\":\"BROKEN\"}");
                case "parts" -> ok("{\"parts\":[{\"state\":\"OK\"},{\"state\":\"OK\"}]}");
                case "many" -> ok("{\"parts\":[{\"state\":\"A\"},{\"state\":\"B\"},{\"state\":\"C\"}]}");
                default -> ok("{\"status\":\"READY\"}");
            };
        }

        private static SdkHttpResponse ok(String json) {
            return SdkHttpResponse.builder().statusCode(200).body(HttpBody.fromUtf8(json)).build();
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    return respond(request);
                }

                @Override
                public void abort() {
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            SdkHttpResponse response = respond(request);
            AsyncExchangeCompletion exchange =
                new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
            ByteBuffer body = response.body().buffered().asByteBuffer();
            handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
            handler.onStream(publisherOf(body));
            exchange.complete();
            return exchange;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(mode).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "ScriptedHttp";
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static Publisher<ByteBuffer> publisherOf(ByteBuffer bytes) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (done) {
                    return;
                }
                done = true;
                if (bytes.hasRemaining()) {
                    subscriber.onNext(bytes.asReadOnlyBuffer());
                }
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }
}
