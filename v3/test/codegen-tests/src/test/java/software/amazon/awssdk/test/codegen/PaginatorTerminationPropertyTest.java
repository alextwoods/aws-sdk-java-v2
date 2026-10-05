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

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
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
 * Property 19 (Requirements 7.8, 8.6): paginator termination follows modeled tokens.
 *
 * <p>One synthetic awsJson1_0 model, generated and compiled once, has a paginated operation with a top-level output
 * token and items ({@code ListFlat}) and one whose output token and items sit under a nested structure
 * ({@code ListNested}: {@code window.next}, {@code window.entries}, input token {@code cursor}). Each try generates a
 * finite page graph as wire documents: continuing tokens (with inner, leading and trailing spaces, or whitespace only,
 * which the modeled condition treats as present) and a final page whose token is absent, JSON {@code null} or empty
 * (and, nested, whose enclosing structure is absent); items per page (absent, empty or a few). A strict transport
 * serves page {@code k} only to the {@code k}-th call carrying page {@code k-1}'s token, and records any other call.
 *
 * <p>The generated sync pages iterator (pulled up to a limit, then asked {@code hasNext}), the flattened sync items
 * (pulled up to a limit), and the async publisher (driven by a demand trace, optionally cancelled between demand
 * steps or from {@code onNext}) are compared with an oracle that evaluates the canonical {@code @paginated} member
 * paths on the wire documents: pagination continues exactly while the output token is a present, non-empty string.
 * It predicts the emitted pages, the flattened items, the number of calls and the stop point; for async, the events
 * and calls after every demand step, {@code onComplete} exactly when demand reaches past the tokenless final page,
 * and nothing after cancellation. {@code PaginationTest} in {@code sdk-core} covers the runtime's per-page order and
 * per-demand calls on synthetic fetchers; this property covers the generated token wiring end to end.
 */
class PaginatorTerminationPropertyTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.pages";
    private static final Duration AWAIT = Duration.ofSeconds(5);

    private static final String MODEL = """
        $version: "2"
        namespace example.pages

        @aws.protocols#awsJson1_0
        @aws.api#service(sdkId: "Pages", endpointPrefix: "pages")
        service Pages {
            version: "2024-01-01"
            operations: [ListFlat, ListNested]
        }

        @auth([])
        @readonly
        @paginated(inputToken: "nextToken", outputToken: "nextToken", items: "things")
        operation ListFlat {
            input := {
                nextToken: String
            }
            output := {
                pageId: Integer
                nextToken: String
                things: NameList
            }
        }

        @auth([])
        @readonly
        @paginated(inputToken: "cursor", outputToken: "window.next", items: "window.entries")
        operation ListNested {
            input := {
                cursor: String
            }
            output := {
                pageId: Integer
                window: Window
            }
        }

        structure Window {
            next: String
            entries: NameList
        }

        list NameList {
            member: String
        }
        """;

    private static final String PROBE = """
        package example.probe;

        import java.net.URI;
        import java.util.ArrayList;
        import java.util.Iterator;
        import java.util.List;
        import java.util.Queue;
        import java.util.concurrent.atomic.AtomicReference;
        import org.reactivestreams.Subscriber;
        import org.reactivestreams.Subscription;
        import software.amazon.awssdk.http.SdkAsyncHttpClient;
        import software.amazon.awssdk.http.SdkSyncHttpClient;
        import software.amazon.awssdk.regions.Region;
        import PKG.PagesAsyncClient;
        import PKG.PagesClient;
        import PKG.model.ListFlatRequest;
        import PKG.model.ListFlatResponse;
        import PKG.model.ListNestedRequest;
        import PKG.model.ListNestedResponse;
        import PKG.model.Window;

        public final class Probe {
            private static final URI ENDPOINT = URI.create("https://pages.test");

            private Probe() {
            }

            /** Pulls at most {@code limit} pages, then reports whether the iterator has another. */
            public static List<String> pages(SdkSyncHttpClient http, boolean nested, int limit) {
                try (PagesClient client = PagesClient.builder().region(Region.of("us-east-1"))
                                                     .endpointOverride(ENDPOINT).httpClient(http).build()) {
                    Iterator<?> pages = nested
                        ? client.listNestedPaginator(ListNestedRequest.builder().build()).iterator()
                        : client.listFlatPaginator(ListFlatRequest.builder().build()).iterator();
                    List<String> seen = new ArrayList<>();
                    while (seen.size() < limit && pages.hasNext()) {
                        seen.add(describe(pages.next()));
                    }
                    seen.add("hasNext=" + pages.hasNext());
                    return seen;
                }
            }

            /** Pulls at most {@code limit} flattened items. */
            public static List<String> items(SdkSyncHttpClient http, boolean nested, int limit) {
                try (PagesClient client = PagesClient.builder().region(Region.of("us-east-1"))
                                                     .endpointOverride(ENDPOINT).httpClient(http).build()) {
                    Iterator<String> items = nested
                        ? client.listNestedPaginator(ListNestedRequest.builder().build()).entries().iterator()
                        : client.listFlatPaginator(ListFlatRequest.builder().build()).things().iterator();
                    List<String> seen = new ArrayList<>();
                    while (seen.size() < limit && items.hasNext()) {
                        seen.add(items.next());
                    }
                    return seen;
                }
            }

            public static AutoCloseable asyncClient(SdkAsyncHttpClient http) {
                return PagesAsyncClient.builder().region(Region.of("us-east-1")).endpointOverride(ENDPOINT)
                                       .httpClient(http).build();
            }

            /** Subscribes, recording events; cancels from {@code onNext} of emission {@code cancelAt} (0: never). */
            public static Subscription subscribe(AutoCloseable asyncClient, boolean nested, Queue<String> events,
                                                 int cancelAt) {
                PagesAsyncClient client = (PagesAsyncClient) asyncClient;
                AtomicReference<Subscription> subscription = new AtomicReference<>();
                Subscriber<Object> recorder = new Subscriber<Object>() {
                    private int emitted;

                    @Override
                    public void onSubscribe(Subscription s) {
                        subscription.set(s);
                    }

                    @Override
                    public void onNext(Object page) {
                        events.add(describe(page));
                        if (++emitted == cancelAt) {
                            subscription.get().cancel();
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        events.add("error:" + t);
                    }

                    @Override
                    public void onComplete() {
                        events.add("complete");
                    }
                };
                if (nested) {
                    client.listNestedPaginator(ListNestedRequest.builder().build()).subscribe(recorder);
                } else {
                    client.listFlatPaginator(ListFlatRequest.builder().build()).subscribe(recorder);
                }
                return subscription.get();
            }

            private static String describe(Object page) {
                if (page instanceof ListFlatResponse flat) {
                    return describe(flat.pageId(), flat.nextToken(), flat.things());
                }
                Window window = ((ListNestedResponse) page).window();
                return describe(((ListNestedResponse) page).pageId(), window == null ? null : window.next(),
                                window == null ? null : window.entries());
            }

            private static String describe(Integer id, String token, List<String> items) {
                return "page " + id + " token=" + (token == null ? "-" : "'" + token + "'") + " items="
                       + (items == null ? List.of() : items);
            }
        }
        """.replace("PKG", PACKAGE);

    private static URLClassLoader loader;
    private static Method syncPages;
    private static Method syncItems;
    private static Method asyncClient;
    private static Method subscribe;

    @BeforeContainer
    static void generateAndCompile() throws Exception {
        Path work = Files.createTempDirectory(Files.createDirectories(Path.of("target", "paginator-termination")),
                                              "fixture-");
        Path models = Files.createDirectories(work.resolve("model"));
        Files.writeString(models.resolve("pages.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(
            new AwsCodegenRequest("example.pages#Pages", PACKAGE, models, work.resolve("out")), List.of());
        Path probe = Files.createDirectories(result.sourceRoot().resolve("example/probe")).resolve("Probe.java");
        Files.writeString(probe, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(result.sourceRoot(), work.resolve("classes")).loader();
        Class<?> type = Class.forName("example.probe.Probe", true, loader);
        syncPages = type.getMethod("pages", SdkSyncHttpClient.class, boolean.class, int.class);
        syncItems = type.getMethod("items", SdkSyncHttpClient.class, boolean.class, int.class);
        asyncClient = type.getMethod("asyncClient", SdkAsyncHttpClient.class);
        subscribe = type.getMethod("subscribe", AutoCloseable.class, boolean.class, Queue.class, int.class);
    }

    @AfterContainer
    static void close() throws Exception {
        loader.close();
    }

    @Property(tries = 250, edgeCases = EdgeCasesMode.NONE)
    void generatedPaginatorsStopExactlyWhereTheModeledTokenEnds(@ForAll("scenarios") Scenario scenario)
        throws Exception {
        List<Map<String, Object>> pages = Oracle.reachable(scenario.shape(), scenario.documents());
        boolean nested = scenario.shape() == Shape.NESTED;

        GraphHttp http = new GraphHttp(scenario.shape(), pages, HttpTransportMode.SYNC);
        List<String> pulled = invoke(http, syncPages, http, nested, scenario.pageLimit());
        assertEquals(Oracle.syncPages(scenario.shape(), pages, scenario.pageLimit()), pulled, "sync pages");
        assertEquals(Math.min(scenario.pageLimit(), pages.size()), http.calls.get(), "sync page calls");

        http = new GraphHttp(scenario.shape(), pages, HttpTransportMode.SYNC);
        List<String> items = invoke(http, syncItems, http, nested, scenario.itemLimit());
        List<String> allItems = Oracle.items(scenario.shape(), pages);
        assertEquals(allItems.subList(0, Math.min(scenario.itemLimit(), allItems.size())), items, "sync items");
        assertEquals(Oracle.itemCalls(scenario.shape(), pages, scenario.itemLimit()), http.calls.get(),
                     "sync item calls");

        http = new GraphHttp(scenario.shape(), pages, HttpTransportMode.ASYNC);
        Queue<String> events = new ConcurrentLinkedQueue<>();
        List<Oracle.Step> expected = Oracle.async(scenario, pages);
        try (AutoCloseable client = invoke(http, asyncClient, http)) {
            Subscription subscription = invoke(http, subscribe, client, nested, events, scenario.cancelAtEmission());
            GraphHttp transport = http;
            for (int i = 0; i < scenario.demand().size(); i++) {
                subscription.request(scenario.demand().get(i));
                Oracle.Step step = expected.get(i);
                await(() -> events.size() >= step.events().size() && transport.calls.get() >= step.calls());
                assertEquals(List.of(), http.violations, "transport violations after demand step " + i);
                assertEquals(step.events(), List.copyOf(events), "async events after demand step " + i);
                assertEquals(step.calls(), http.calls.get(), "async calls after demand step " + i);
                if (i == scenario.cancelAfterStep()) {
                    subscription.cancel();
                }
            }
            // Late signals or calls would be cumulative: one short settle catches any after the last step.
            Thread.sleep(1);
            Oracle.Step last = expected.get(expected.size() - 1);
            assertEquals(last.events(), List.copyOf(events), "async events after the trace");
            assertEquals(last.calls(), http.calls.get(), "async calls after the trace");
        }
        assertEquals(List.of(), http.violations, "transport violations");
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        return Arbitraries.of(Shape.values()).flatMap(shape -> Arbitraries.integers().between(1, 8).flatMap(n -> {
            Arbitrary<List<String>> tokens = n == 1 ? Arbitraries.just(List.of()) : Combinators.combine(
                IntStream.range(0, n - 1).mapToObj(PaginatorTerminationPropertyTest::continuingToken).toList())
                                                                                  .as(List::copyOf);
            Arbitrary<Terminal> terminal = shape == Shape.NESTED
                                           ? Arbitraries.of(Terminal.values())
                                           : Arbitraries.of(Terminal.ABSENT, Terminal.NULL, Terminal.EMPTY);
            Arbitrary<List<Integer>> itemCounts = Arbitraries.integers().between(-1, 3).list().ofSize(n);
            Arbitrary<List<Long>> demand = Arbitraries.frequencyOf(
                Tuple.of(9, Arbitraries.longs().between(1, 3)),
                Tuple.of(1, Arbitraries.just(Long.MAX_VALUE))).list().ofMinSize(1).ofMaxSize(6);
            Arbitrary<Integer> cancelAfterStep = Arbitraries.frequencyOf(
                Tuple.of(2, Arbitraries.just(-1)), Tuple.of(1, Arbitraries.integers().between(0, 5)));
            Arbitrary<Integer> cancelAtEmission = Arbitraries.frequencyOf(
                Tuple.of(2, Arbitraries.just(0)), Tuple.of(1, Arbitraries.integers().between(1, n)));
            return Combinators.combine(tokens, terminal, itemCounts, Arbitraries.integers().between(1, n + 1),
                                       Arbitraries.integers().between(0, 3 * n + 1), demand, cancelAfterStep,
                                       cancelAtEmission)
                              .as((t, end, counts, pageLimit, itemLimit, d, afterStep, atEmission) ->
                                      new Scenario(shape, t, end, counts, pageLimit, itemLimit, d, afterStep,
                                                   atEmission));
        }));
    }

    /**
     * A continuing token, unique to page {@code k}: index-prefixed text with spaces anywhere after the prefix, or
     * {@code k + 1} spaces (present and non-empty, so pagination continues).
     */
    private static Arbitrary<String> continuingToken(int k) {
        return Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.strings().withChars("abxyz09 ").ofMaxLength(5).map(s -> k + "~" + s)),
            Tuple.of(1, Arbitraries.just(" ".repeat(k + 1))));
    }

    private static <T> T invoke(GraphHttp http, Method method, Object... arguments) throws Exception {
        try {
            @SuppressWarnings("unchecked")
            T value = (T) method.invoke(null, arguments);
            return value;
        } catch (InvocationTargetException e) {
            throw new AssertionError("Probe." + method.getName() + " failed; transport violations: "
                                     + http.violations, e.getCause());
        }
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + AWAIT.toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(0, 100_000);
        }
    }

    /** The paginated operations: their wire input token and their modeled output token and items paths. */
    enum Shape {
        FLAT("ListFlat", "nextToken", "nextToken", "things"),
        NESTED("ListNested", "cursor", "window.next", "window.entries");

        final String operation;
        final String inputToken;
        final String outputToken;
        final String items;

        Shape(String operation, String inputToken, String outputToken, String items) {
            this.operation = operation;
            this.inputToken = inputToken;
            this.outputToken = outputToken;
            this.items = items;
        }
    }

    /** How the final page ends pagination: token absent, JSON null or empty; nested, the window absent. */
    enum Terminal {
        ABSENT, NULL, EMPTY, WINDOW_ABSENT
    }

    /**
     * One try: page {@code k < tokens.size()} continues with {@code tokens[k]}, the last page ends with
     * {@code terminal}; {@code itemCounts[k]} items ({@code -1}: absent). Sync pulls at most {@code pageLimit} pages
     * and {@code itemLimit} items; async requests each {@code demand} entry in turn, cancelling after step
     * {@code cancelAfterStep} ({@code -1}: never) or from {@code onNext} of emission {@code cancelAtEmission}
     * ({@code 0}: never).
     */
    record Scenario(Shape shape, List<String> tokens, Terminal terminal, List<Integer> itemCounts, int pageLimit,
                    int itemLimit, List<Long> demand, int cancelAfterStep, int cancelAtEmission) {
        /** The wire documents of the pages, in order. */
        List<Map<String, Object>> documents() {
            List<Map<String, Object>> documents = new ArrayList<>();
            for (int k = 0; k <= tokens.size(); k++) {
                Map<String, Object> page = new LinkedHashMap<>();
                page.put("pageId", k);
                boolean last = k == tokens.size();
                if (last && terminal == Terminal.WINDOW_ABSENT) {
                    documents.add(page);
                    continue;
                }
                if (shape == Shape.NESTED) {
                    page.put("window", new LinkedHashMap<String, Object>());
                }
                if (!last) {
                    put(page, shape.outputToken, tokens.get(k));
                } else if (terminal != Terminal.ABSENT) {
                    put(page, shape.outputToken, terminal == Terminal.EMPTY ? "" : null);
                }
                if (itemCounts.get(k) >= 0) {
                    int page1 = k;
                    put(page, shape.items, IntStream.range(0, itemCounts.get(k)).mapToObj(j -> "p" + page1 + "i" + j)
                                                    .toList());
                }
                documents.add(page);
            }
            return documents;
        }

        @SuppressWarnings("unchecked")
        private static void put(Map<String, Object> document, String path, Object value) {
            String[] steps = path.split("\\.");
            Map<String, Object> current = document;
            for (int i = 0; i < steps.length - 1; i++) {
                current = (Map<String, Object>) current.computeIfAbsent(steps[i], s -> new LinkedHashMap<>());
            }
            current.put(steps[steps.length - 1], value);
        }
    }

    /**
     * The reference: evaluates the canonical {@code @paginated} member paths on wire documents, independent of the
     * generated code. Pagination continues while the selected output token is a present, non-empty string.
     */
    static final class Oracle {
        /** Events and cumulative calls after one async demand step. */
        record Step(List<String> events, int calls) {
        }

        private Oracle() {
        }

        static Object select(Map<String, Object> document, String path) {
            Object current = document;
            for (String step : path.split("\\.")) {
                if (!(current instanceof Map<?, ?> map)) {
                    return null;
                }
                current = map.get(step);
            }
            return current;
        }

        static boolean continues(Object token) {
            return token instanceof String string && !string.isEmpty();
        }

        /** The pages reached from the first by following tokens; the graph must end where the tokens do. */
        static List<Map<String, Object>> reachable(Shape shape, List<Map<String, Object>> documents) {
            int k = 0;
            while (continues(select(documents.get(k), shape.outputToken))) {
                k++;
                if (k == documents.size()) {
                    throw new AssertionError("the generated graph continues past its last page: " + documents);
                }
            }
            return documents.subList(0, k + 1);
        }

        static String describe(Shape shape, Map<String, Object> page) {
            Object token = select(page, shape.outputToken);
            return "page " + page.get("pageId") + " token=" + (token == null ? "-" : "'" + token + "'") + " items="
                   + pageItems(shape, page);
        }

        static List<String> syncPages(Shape shape, List<Map<String, Object>> pages, int limit) {
            List<String> seen = new ArrayList<>();
            pages.stream().limit(limit).forEach(page -> seen.add(describe(shape, page)));
            seen.add("hasNext=" + (limit < pages.size()));
            return seen;
        }

        static List<String> items(Shape shape, List<Map<String, Object>> pages) {
            return pages.stream().flatMap(page -> pageItems(shape, page).stream()).toList();
        }

        /** Pages called to pull {@code limit} items: through the page of the last one, or all when too few. */
        static int itemCalls(Shape shape, List<Map<String, Object>> pages, int limit) {
            int taken = 0;
            for (int k = 0; k < pages.size(); k++) {
                if (taken >= limit) {
                    return k;
                }
                taken += pageItems(shape, pages.get(k)).size();
            }
            return pages.size();
        }

        /** Events and calls after each demand step of {@code scenario}. */
        static List<Step> async(Scenario scenario, List<Map<String, Object>> pages) {
            List<String> events = new ArrayList<>();
            List<Step> steps = new ArrayList<>();
            long demand = 0;
            int emitted = 0;
            boolean cancelled = false;
            boolean completed = false;
            for (int i = 0; i < scenario.demand().size(); i++) {
                long n = scenario.demand().get(i);
                demand = demand > Long.MAX_VALUE - n ? Long.MAX_VALUE : demand + n;
                while (!cancelled && !completed && demand > 0) {
                    if (emitted == pages.size()) {
                        events.add("complete");
                        completed = true;
                    } else {
                        events.add(describe(scenario.shape(), pages.get(emitted++)));
                        demand--;
                        cancelled = emitted == scenario.cancelAtEmission();
                    }
                }
                steps.add(new Step(List.copyOf(events), emitted));
                cancelled |= i == scenario.cancelAfterStep();
            }
            return steps;
        }

        @SuppressWarnings("unchecked")
        private static List<String> pageItems(Shape shape, Map<String, Object> page) {
            Object items = select(page, shape.items);
            return items == null ? List.of() : (List<String>) items;
        }
    }

    /**
     * Serves the oracle's reachable pages in order: call {@code k} must be the shape's operation carrying page
     * {@code k-1}'s output token as its input token (none for the first); any other call is a violation.
     */
    static final class GraphHttp implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final AtomicInteger calls = new AtomicInteger();
        final List<String> violations = new CopyOnWriteArrayList<>();
        private final Shape shape;
        private final List<Map<String, Object>> pages;
        private final HttpTransportMode mode;
        private final Pattern inputToken;

        GraphHttp(Shape shape, List<Map<String, Object>> pages, HttpTransportMode mode) {
            this.shape = shape;
            this.pages = pages;
            this.mode = mode;
            this.inputToken = Pattern.compile("\"" + shape.inputToken + "\"\\s*:\\s*\"([^\"\\\\]*)\"");
        }

        private SdkHttpResponse respond(SdkHttpRequest request) {
            ByteBuffer buffer = request.body().buffered().asByteBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            String body = new String(bytes, StandardCharsets.UTF_8);
            String target = request.headers().firstValue("X-Amz-Target").orElse("");
            int call = calls.getAndIncrement();
            if (!target.endsWith("." + shape.operation)) {
                return violation("call " + call + " targets " + target);
            }
            if (call >= pages.size()) {
                return violation("call " + call + " after the final page " + (pages.size() - 1) + ": " + body);
            }
            Matcher matcher = inputToken.matcher(body);
            String actual = matcher.find() ? matcher.group(1) : null;
            Object expected = call == 0 ? null : Oracle.select(pages.get(call - 1), shape.outputToken);
            if (!Objects.equals(expected, actual)) {
                return violation("call " + call + " carries " + actual + " instead of " + expected + ": " + body);
            }
            return SdkHttpResponse.builder().statusCode(200).body(HttpBody.fromUtf8(json(pages.get(call)))).build();
        }

        private SdkHttpResponse violation(String message) {
            violations.add(message);
            return SdkHttpResponse.builder().statusCode(400)
                                  .body(HttpBody.fromUtf8("{\"__type\":\"example.pages#Unexpected\"}")).build();
        }

        /** JSON of a document; the generated strings need no escaping. */
        private static String json(Object value) {
            if (value == null || value instanceof Number) {
                return String.valueOf(value);
            }
            if (value instanceof String string) {
                return "\"" + string + "\"";
            }
            if (value instanceof List<?> list) {
                return list.stream().map(GraphHttp::json).collect(joining(",", "[", "]"));
            }
            return ((Map<?, ?>) value).entrySet().stream().map(e -> "\"" + e.getKey() + "\":" + json(e.getValue()))
                                      .collect(joining(",", "{", "}"));
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
            return "GraphHttp";
        }

        @Override
        public void close() {
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
}
