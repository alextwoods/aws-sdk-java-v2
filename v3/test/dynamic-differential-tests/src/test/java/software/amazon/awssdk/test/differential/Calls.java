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

package software.amazon.awssdk.test.differential;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.dynamic.client.DynamicAsyncClient;
import software.amazon.awssdk.dynamic.client.DynamicClient;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageDecoder;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.eventstream.SigV4FrameSignatureAlgorithm;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.AuthOptionsResolver;
import software.amazon.awssdk.pipeline.client.AwsClientBuildState;
import software.amazon.awssdk.protocols.core.eventstream.PublisherEventStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.test.differential.Matrix.Event;
import software.amazon.awssdk.test.differential.Matrix.Mode;
import software.amazon.awssdk.test.differential.Matrix.Scenario;
import software.amazon.awssdk.test.differential.ScriptedTransport.Exchange;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * Runs one scenario through a generated client or the Dynamic Client and records its {@link Trace}.
 *
 * <p>Both clients get exactly the same configuration through their public builders: region, static credentials, a
 * retry strategy of two attempts without backoff or jitter, the scripted transport and no external configuration
 * sources (set on the generated builder's build state, which its public API does not expose). Nondeterministic
 * metadata is fixed rather than normalized away: the signing clock of both clients is the same fixed instant (neither
 * builder exposes signer properties, so each built client's auth options resolver is wrapped to add
 * {@link HttpSigner#SIGNING_CLOCK}), every input with an {@code @idempotencyToken} member sets it explicitly, retries
 * have no backoff, waiters wait {@value #WAITER_DELAY_MILLIS} ms between polls, and V3 sends no invocation ID. The
 * wire is therefore compared byte for byte, including {@code Authorization} and event frame signatures.
 *
 * <p>The generated client is called through the same public overloads an application uses (located by reflection on
 * the generated client interface, so one runner serves every generated service): {@code op(Request, override)},
 * {@code op(Request, RequestBody|AsyncRequestBody)}, {@code op(Request, ResponseTransformer|AsyncResponseTransformer)},
 * {@code op(Request, Publisher)}, {@code op(Request, <Op>ResponseHandler)}, {@code opPaginator(Request)} and
 * {@code waiter().waitUntil<Name>(Request, override)}; the Dynamic Client through the matching {@code call},
 * {@code callAsync}, {@code paginator} and {@code waiter().waitUntil}.
 */
final class Calls {
    static final Instant SIGNED_AT = Instant.parse("2024-06-01T12:34:56Z");
    static final Region REGION = Region.US_WEST_2;
    static final String ACCESS_KEY = "AKIDDIFFERENTIAL";
    static final String SECRET_KEY = "differential-secret-key";
    static final long WAITER_DELAY_MILLIS = 1;
    static final long TIMEOUT_SECONDS = 30;
    private static final String EVENT_STREAM = "application/vnd.amazon.eventstream";

    private Calls() {
    }

    /** One client's call: every transmitted request, exactly, and the observed results. */
    static final class Trace {
        final List<String> requests = new ArrayList<>();
        final List<String> handler = new CopyOnWriteArrayList<>();
        String outcome = "none";

        String render() {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < requests.size(); i++) {
                out.append("-- request ").append(i + 1).append('\n').append(requests.get(i)).append('\n');
            }
            if (!handler.isEmpty()) {
                out.append("-- handler\n");
                handler.forEach(line -> out.append(line).append('\n'));
            }
            return out.append("-- outcome\n").append(outcome).toString();
        }

        long handlerEvents() {
            return handler.stream().filter(line -> line.startsWith("event ")).count();
        }
    }

    static Trace run(Scenario scenario, Mode mode, boolean dynamic) throws Exception {
        Trace trace = new Trace();
        ScriptedTransport http = new ScriptedTransport(mode == Mode.SYNC ? HttpTransportMode.SYNC
                                                                         : HttpTransportMode.ASYNC,
                                                       scenario.replies());
        try {
            try (AutoCloseable client = dynamic ? dynamicClient(scenario, mode, http)
                                                : generatedClient(scenario, mode, http)) {
                fixSigningClock(client, Clock.fixed(SIGNED_AT, ZoneOffset.UTC));
                try {
                    trace.outcome = dynamic ? callDynamic(client, scenario, mode, trace)
                                            : callGenerated(client, scenario, mode, trace);
                } catch (InvocationTargetException e) {
                    trace.outcome = "failure " + Values.failure(e.getCause());
                } catch (ExecutionException e) {
                    trace.outcome = "failure " + Values.failure(e.getCause());
                } catch (TimeoutException e) {
                    trace.outcome = "timed out after " + TIMEOUT_SECONDS + "s";
                } catch (RuntimeException e) {
                    trace.outcome = "failure " + Values.failure(e);
                }
            }
        } finally {
            http.close();
        }
        for (Exchange exchange : http.exchanges()) {
            trace.requests.add(render(exchange, scenario));
        }
        return trace;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Clients

    private static StandardRetryStrategy retries() {
        return StandardRetryStrategy.builder().maxAttempts(2).backoffStrategy(BackoffStrategy.none())
                                    .throttlingBackoffStrategy(BackoffStrategy.none()).build();
    }

    private static WaiterOverrideConfiguration waiterOverride() {
        Duration delay = Duration.ofMillis(WAITER_DELAY_MILLIS);
        return WaiterOverrideConfiguration.builder().maxAttempts(6).minDelay(delay).maxDelay(delay).build();
    }

    private static AutoCloseable dynamicClient(Scenario scenario, Mode mode, ScriptedTransport http) {
        if (mode == Mode.SYNC) {
            return DynamicClient.builder().model(scenario.target().model().get())
                                .serviceId(scenario.target().serviceId()).region(REGION)
                                .credentialsProvider(new StaticCredentials()).retryStrategy(retries())
                                .configurationSources(ConfigurationSources.none()).httpClient(http).build();
        }
        return DynamicAsyncClient.builder().model(scenario.target().model().get())
                                 .serviceId(scenario.target().serviceId()).region(REGION)
                                 .credentialsProvider(new StaticCredentials()).retryStrategy(retries())
                                 .configurationSources(ConfigurationSources.none()).httpClient(http).build();
    }

    /** The generated client interface of the scenario's service in {@code mode}. */
    static Class<?> clientInterface(Scenario scenario, Mode mode) throws ClassNotFoundException {
        String type = scenario.target().clientPackage() + "." + scenario.target().clientPrefix()
                      + (mode == Mode.SYNC ? "Client" : "AsyncClient");
        return Class.forName(type, true, Calls.class.getClassLoader());
    }

    private static AutoCloseable generatedClient(Scenario scenario, Mode mode, ScriptedTransport http)
        throws Exception {
        Method builderMethod = clientInterface(scenario, mode).getMethod("builder");
        Class<?> builderType = builderMethod.getReturnType();
        Object builder = builderMethod.invoke(null);
        invoke(builderType, builder, "region", REGION);
        invoke(builderType, builder, "credentialsProvider", new StaticCredentials());
        invoke(builderType, builder, "retryStrategy", retries());
        invoke(builderType, builder, "httpClient", http);
        ((AwsClientBuildState) field(builder.getClass(), "state").get(builder))
            .configurationSources(ConfigurationSources.none());
        return (AutoCloseable) builderType.getMethod("build").invoke(builder);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Generated calls

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String callGenerated(Object client, Scenario scenario, Mode mode, Trace trace) throws Exception {
        Class<?> api = clientInterface(scenario, mode);
        String name = lowerCamel(scenario.operation());
        boolean sync = mode == Mode.SYNC;
        Document input = Values.toDocument(scenario.input());
        switch (scenario.shape()) {
            case FINITE, MODELED_ERROR, UNMODELED_ERROR -> {
                Method method = method(api, name, p -> p.length == 2 && p[1] == AwsRequestOverrideConfiguration.class);
                Object result = method.invoke(client, request(method, input), null);
                return "success " + Values.render(sync ? result : await(result));
            }
            case STREAMING_INPUT -> {
                Class<?> bodyType = sync ? RequestBody.class : AsyncRequestBody.class;
                Method method = method(api, name, p -> p.length == 2 && p[1] == bodyType);
                Object body = sync ? RequestBody.fromBytes(scenario.requestBody())
                                   : AsyncRequestBody.fromBytes(scenario.requestBody());
                Object result = method.invoke(client, request(method, input), body);
                return "success " + Values.render(sync ? result : await(result));
            }
            case STREAMING_OUTPUT -> {
                Class<?> transformerType = sync ? ResponseTransformer.class : AsyncResponseTransformer.class;
                Method method = method(api, name, p -> p.length == 2 && p[1] == transformerType);
                Object transformer = sync ? ResponseTransformer.toBytes() : AsyncResponseTransformer.toBytes();
                Object result = method.invoke(client, request(method, input), transformer);
                return bytesOutcome((ResponseBytes<?>) (sync ? result : await(result)));
            }
            case EVENT_STREAM_INPUT -> {
                Method method = method(api, name, p -> p.length == 2 && p[1] == Publisher.class);
                Method unionBuilder = Class.forName(scenario.target().clientPackage() + ".model."
                                                    + scenario.eventUnion()).getMethod("builder");
                List<Object> events = new ArrayList<>();
                for (Event event : scenario.inputEvents()) {
                    events.add(eventDocument(event).asShape((ShapeBuilder) unionBuilder.invoke(null)));
                }
                Object result = method.invoke(client, request(method, input), new ListPublisher<>(events));
                return "success " + Values.render(await(result));
            }
            case EVENT_STREAM_OUTPUT -> {
                Method method = method(api, name, p -> p.length == 2 && p[1].getSimpleName().endsWith(
                    "ResponseHandler"));
                Class<?> handlerType = method.getParameterTypes()[1];
                StreamRecorder recorder = new StreamRecorder(trace.handler);
                Object handler = Proxy.newProxyInstance(handlerType.getClassLoader(), new Class<?>[] {handlerType},
                                                        (proxy, m, args) -> forward(recorder, proxy, m, args));
                Object result = method.invoke(client, request(method, input), handler);
                Object value = await(result);
                return value == null ? "complete" : "success " + Values.render(value);
            }
            case PAGINATED -> {
                Method method = method(api, name + "Paginator", p -> p.length == 1);
                Object pages = method.invoke(client, request(method, input));
                return sync ? pagesOutcome((Iterable<Object>) pages) : pagesOutcome((Publisher<Object>) pages);
            }
            case WAITER -> {
                Method waiterMethod = api.getMethod("waiter");
                try (AutoCloseable waiter = (AutoCloseable) waiterMethod.invoke(client)) {
                    Method method = method(waiterMethod.getReturnType(), "waitUntil" + scenario.waiter(),
                                           p -> p.length == 2 && p[1] == WaiterOverrideConfiguration.class);
                    Object result = method.invoke(waiter, request(method, input), waiterOverride());
                    return waiterOutcome((WaiterResponse<?>) (sync ? result : await(result)));
                }
            }
            default -> throw new IllegalStateException(scenario.shape().toString());
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Dynamic calls

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String callDynamic(Object client, Scenario scenario, Mode mode, Trace trace) throws Exception {
        String operation = scenario.operation();
        Document input = Values.toDocument(scenario.input());
        if (mode == Mode.SYNC) {
            DynamicClient sync = (DynamicClient) client;
            return switch (scenario.shape()) {
                case FINITE, MODELED_ERROR, UNMODELED_ERROR -> "success " + Values.render(
                    sync.call(operation, input, (AwsRequestOverrideConfiguration) null));
                case STREAMING_INPUT -> "success " + Values.render(
                    sync.call(operation, input, RequestBody.fromBytes(scenario.requestBody())));
                case STREAMING_OUTPUT -> bytesOutcome(sync.call(operation, input,
                                                                ResponseTransformer.<Document>toBytes()));
                case PAGINATED -> pagesOutcome((Iterable) sync.paginator(operation, input));
                case WAITER -> waiterOutcome(sync.waiter().waitUntil(scenario.waiter(), input, waiterOverride()));
                default -> throw new IllegalStateException(scenario.shape() + " has no sync call");
            };
        }
        DynamicAsyncClient async = (DynamicAsyncClient) client;
        return switch (scenario.shape()) {
            case FINITE, MODELED_ERROR, UNMODELED_ERROR -> "success " + Values.render(
                await(async.callAsync(operation, input)));
            case STREAMING_INPUT -> "success " + Values.render(
                await(async.callAsync(operation, input, AsyncRequestBody.fromBytes(scenario.requestBody()))));
            case STREAMING_OUTPUT -> bytesOutcome((ResponseBytes<?>) await(
                async.callAsync(operation, input, AsyncResponseTransformer.<Document>toBytes())));
            case EVENT_STREAM_INPUT -> {
                List<SerializableStruct> events = new ArrayList<>();
                for (Event event : scenario.inputEvents()) {
                    events.add(async.inputEvent(operation, eventDocument(event)));
                }
                Map<String, Document> members = new LinkedHashMap<>(input.asStringMap());
                members.put(scenario.eventsMember(),
                            Document.ofObject(PublisherEventStream.of(new ListPublisher<>(events))));
                yield "success " + Values.render(await(async.callAsync(operation, Document.of(members))));
            }
            case EVENT_STREAM_OUTPUT -> {
                StreamRecorder recorder = new StreamRecorder(trace.handler);
                EventStreamResponseHandler<Document, Document> handler =
                    (EventStreamResponseHandler<Document, Document>) (EventStreamResponseHandler<?, ?>) recorder;
                Object value = await(async.callAsync(operation, input, handler));
                yield value == null ? "complete" : "success " + Values.render(value);
            }
            case PAGINATED -> pagesOutcome((Publisher) async.paginator(operation, input));
            case WAITER -> waiterOutcome((WaiterResponse<?>) await(
                async.waiter().waitUntil(scenario.waiter(), input, waiterOverride())));
        };
    }

    // -------------------------------------------------------------------------------------------------------------
    // Outcomes

    private static Object await(Object future) throws Exception {
        return ((CompletableFuture<?>) future).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static String bytesOutcome(ResponseBytes<?> bytes) {
        StringBuilder out = new StringBuilder("success ").append(Values.render(bytes.response())).append(" payload[")
                                                         .append(bytes.asByteArray().length).append("]=");
        return Values.escape(out, bytes.asByteArray()).toString();
    }

    private static String pagesOutcome(Iterable<Object> pages) {
        List<String> rendered = new ArrayList<>();
        for (Object page : pages) {
            rendered.add(Values.render(page));
        }
        return pages(rendered);
    }

    private static String pagesOutcome(Publisher<Object> pages) throws Exception {
        List<String> rendered = new CopyOnWriteArrayList<>();
        CompletableFuture<Void> done = new CompletableFuture<>();
        pages.subscribe(new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object page) {
                rendered.add(Values.render(page));
            }

            @Override
            public void onError(Throwable t) {
                done.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                done.complete(null);
            }
        });
        await(done);
        return pages(rendered);
    }

    private static String pages(List<String> rendered) {
        StringBuilder out = new StringBuilder("pages=").append(rendered.size());
        for (int i = 0; i < rendered.size(); i++) {
            out.append("\npage ").append(i + 1).append(' ').append(rendered.get(i));
        }
        return out.toString();
    }

    private static String waiterOutcome(WaiterResponse<?> response) {
        StringBuilder out = new StringBuilder("waiter attempts=").append(response.attemptsExecuted()).append(' ');
        if (response.matched().response().isPresent()) {
            out.append("response ").append(Values.render(response.matched().response().get()));
        } else {
            out.append("exception ").append(Values.failure(response.matched().exception().orElse(null)));
        }
        return out.toString();
    }

    // -------------------------------------------------------------------------------------------------------------
    // Event streams

    private static Document eventDocument(Event event) {
        return Values.toDocument(Map.of(event.member(), event.values()));
    }

    /**
     * A received event: its union member name and that member's value. {@code Document.of} views a generated union
     * as a structure document while a dynamic event is a union document, so the container is not rendered; it has
     * exactly one member either way.
     */
    private static String renderEvent(Object event) {
        Document document = event instanceof Document d ? d : Document.of((SerializableStruct) event);
        Map<String, Document> members = document.asStringMap();
        if (members.size() != 1) {
            return "not one member: " + Values.render(event);
        }
        Map.Entry<String, Document> member = members.entrySet().iterator().next();
        return member.getKey() + "=" + Values.render(member.getValue());
    }

    @SuppressWarnings("unchecked")
    private static Object forward(StreamRecorder recorder, Object proxy, Method method, Object[] args)
        throws Throwable {
        switch (method.getName()) {
            case "responseReceived" -> recorder.responseReceived(args[0]);
            case "onEventStream" -> recorder.onEventStream((SdkPublisher<Object>) args[0]);
            case "exceptionOccurred" -> recorder.exceptionOccurred((Throwable) args[0]);
            case "complete" -> recorder.complete();
            case "toString" -> {
                return "ResponseHandler";
            }
            case "hashCode" -> {
                return System.identityHashCode(proxy);
            }
            case "equals" -> {
                return proxy == args[0];
            }
            default -> {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
        }
        return null;
    }

    /** Records the handler's callbacks and every event, requesting all events at once. */
    static final class StreamRecorder implements EventStreamResponseHandler<Object, Object> {
        private final List<String> log;

        StreamRecorder(List<String> log) {
            this.log = log;
        }

        @Override
        public void responseReceived(Object response) {
            log.add("response " + Values.render(response));
        }

        @Override
        public void onEventStream(SdkPublisher<Object> publisher) {
            publisher.subscribe(new Subscriber<Object>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(Object event) {
                    log.add("event " + renderEvent(event));
                }

                @Override
                public void onError(Throwable t) {
                    log.add("onError " + Values.failure(t));
                }

                @Override
                public void onComplete() {
                    log.add("onComplete");
                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            log.add("exceptionOccurred " + Values.failure(throwable));
        }

        @Override
        public void complete() {
            log.add("complete");
        }
    }

    /** Emits its items under demand on the requesting thread, then completes. */
    static final class ListPublisher<T> implements Publisher<T> {
        private final List<T> items;

        ListPublisher(List<T> items) {
            this.items = List.copyOf(items);
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private int next;
                private long demand;
                private boolean emitting;
                private boolean done;

                @Override
                public void request(long n) {
                    synchronized (this) {
                        demand = demand + n < 0 ? Long.MAX_VALUE : demand + n;
                        if (emitting) {
                            return;
                        }
                        emitting = true;
                    }
                    while (true) {
                        T item;
                        synchronized (this) {
                            if (done) {
                                emitting = false;
                                return;
                            }
                            if (next == items.size()) {
                                done = true;
                                item = null;
                            } else if (demand == 0) {
                                emitting = false;
                                return;
                            } else {
                                demand--;
                                item = items.get(next++);
                            }
                        }
                        if (item == null) {
                            subscriber.onComplete();
                            return;
                        }
                        subscriber.onNext(item);
                    }
                }

                @Override
                public synchronized void cancel() {
                    done = true;
                }
            });
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // The wire text of a request

    /**
     * The exact text of a transmitted request: method, scheme, authority and raw target; every header in order; the
     * body kind and bytes; and, for an event stream body, every decoded frame (see {@link #frames}).
     */
    static String render(Exchange exchange, Scenario scenario) {
        SdkHttpRequest request = exchange.request;
        StringBuilder out = new StringBuilder(request.method()).append(' ').append(request.scheme()).append("://")
                                                               .append(request.authority())
                                                               .append(request.rawTarget()).append('\n');
        HttpHeaders headers = request.headers();
        for (int i = 0; i < headers.size(); i++) {
            out.append(headers.name(i)).append(": ").append(headers.value(i)).append('\n');
        }
        byte[] body = exchange.bodyBytes();
        out.append("body ").append(request.body().kind()).append('[').append(body.length).append("]=");
        Values.escape(out, body);
        if (exchange.bodyError != null) {
            out.append("\nbody error ").append(exchange.bodyError);
        }
        if (headers.firstValue("Content-Type").map(EVENT_STREAM::equals).orElse(false)) {
            frames(out.append('\n'), request, body, scenario);
        }
        return out.toString();
    }

    /**
     * The decoded request frames: each signed envelope's headers in order and whether its {@code :chunk-signature}
     * chains from the prior signature (the first from the request's) under {@link SigV4FrameSignatureAlgorithm}, then
     * each wrapped event's headers and payload bytes.
     */
    private static void frames(StringBuilder out, SdkHttpRequest request, byte[] body, Scenario scenario) {
        out.append("-- request frames");
        List<Message> envelopes;
        try {
            envelopes = new MessageDecoder().decode(ByteBuffer.wrap(body));
        } catch (RuntimeException e) {
            out.append("\nundecodable ").append(e);
            return;
        }
        String authorization = request.headers().firstValue("Authorization").orElse("");
        int at = authorization.indexOf("Signature=");
        byte[] prior = at < 0 ? new byte[0] : HexFormat.of().parseHex(authorization.substring(at + 10));
        String region = REGION.id();
        String service = scenario.target().signingName();
        SigV4FrameSignatureAlgorithm algorithm = SigV4FrameSignatureAlgorithm.create(
            region, service, date -> sigV4Key(date.format(DateTimeFormatter.BASIC_ISO_DATE), region, service));
        for (int i = 0; i < envelopes.size(); i++) {
            Message envelope = envelopes.get(i);
            out.append("\nframe ").append(i).append(' ');
            renderHeaders(out, envelope.headers());
            HeaderValue date = envelope.headers().get(":date");
            HeaderValue signature = envelope.headers().get(":chunk-signature");
            if (date == null || signature == null) {
                out.append(" chain=INVALID (unsigned)");
                continue;
            }
            Map<String, HeaderValue> dateHeader = new LinkedHashMap<>();
            dateHeader.put(":date", date);
            byte[] expected = algorithm.sign(prior, date.asTimestamp(), MessageEncoder.encodeHeaders(dateHeader),
                                             ByteBuffer.wrap(envelope.payloadBytes()));
            byte[] actual = signature.asByteArray();
            out.append(Arrays.equals(expected, actual) ? " chain=valid" : " chain=INVALID");
            prior = actual;
            if (envelope.payloadBytes().length == 0) {
                out.append("\n  end");
                continue;
            }
            for (Message inner : new MessageDecoder().decode(ByteBuffer.wrap(envelope.payloadBytes()))) {
                out.append("\n  event ");
                renderHeaders(out, inner.headers());
                out.append(" payload=");
                Values.escape(out, inner.payloadBytes());
            }
        }
    }

    private static void renderHeaders(StringBuilder out, Map<String, HeaderValue> headers) {
        out.append('{');
        headers.forEach((name, value) -> out.append(name).append('=').append(headerValue(value)).append(';'));
        out.append('}');
    }

    private static String headerValue(HeaderValue value) {
        return value.type() + ":" + switch (value.type()) {
            case BOOLEAN_TRUE, BOOLEAN_FALSE -> String.valueOf(value.asBoolean());
            case BYTE -> String.valueOf(value.asByte());
            case SHORT -> String.valueOf(value.asShort());
            case INTEGER -> String.valueOf(value.asInteger());
            case LONG -> String.valueOf(value.asLong());
            case BYTE_ARRAY -> HexFormat.of().formatHex(value.asByteArray());
            case STRING -> value.asString();
            case TIMESTAMP -> value.asTimestamp().toString();
            case UUID -> value.asUuid().toString();
        };
    }

    private static byte[] sigV4Key(String date, String region, String service) {
        byte[] key = hmac(("AWS4" + SECRET_KEY).getBytes(StandardCharsets.UTF_8), date);
        key = hmac(key, region);
        key = hmac(key, service);
        return hmac(key, "aws4_request");
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Reflection helpers

    static String lowerCamel(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /** The public method {@code name} of {@code type} whose parameter types match, or a failure listing overloads. */
    static Method method(Class<?> type, String name, Predicate<Class<?>[]> parameters) throws NoSuchMethodException {
        List<String> overloads = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)) {
                if (parameters.test(method.getParameterTypes())
                    && !java.util.function.Consumer.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    return method;
                }
                overloads.add(Arrays.toString(method.getParameterTypes()));
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name + " has no matching overload: " + overloads);
    }

    /** The generated request of {@code method}'s first parameter type, read from {@code input}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object request(Method method, Document input) throws Exception {
        Class<?> requestType = method.getParameterTypes()[0];
        return input.asShape((ShapeBuilder) requestType.getMethod("builder").invoke(null));
    }

    private static void invoke(Class<?> type, Object target, String name, Object argument) throws Exception {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1
                && method.getParameterTypes()[0].isInstance(argument)) {
                method.invoke(target, argument);
                return;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                // keep looking in the superclass
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    /**
     * Fixes the signing clock of a built client: the call engine's auth options resolver is wrapped to add
     * {@link HttpSigner#SIGNING_CLOCK} to every candidate option. Both clients share the engine type.
     */
    private static void fixSigningClock(Object client, Clock clock) throws Exception {
        Class<?> stagesType = Class.forName("software.amazon.awssdk.pipeline.CallStages");
        List<Object> stages = new ArrayList<>();
        collect(client, stagesType, Collections.newSetFromMap(new IdentityHashMap<>()), 0, stages);
        if (stages.size() != 1) {
            throw new IllegalStateException("expected one call engine in " + client + ", found " + stages.size());
        }
        Field field = field(stagesType, "authOptions");
        AuthOptionsResolver original = (AuthOptionsResolver) field.get(stages.get(0));
        AuthOptionsResolver fixed = (auth, options) -> {
            List<AuthSchemeOption> out = new ArrayList<>();
            for (AuthSchemeOption option : original.options(auth, options)) {
                out.add(option.toBuilder().putSignerProperty(HttpSigner.SIGNING_CLOCK, clock).build());
            }
            return out;
        };
        field.set(stages.get(0), fixed);
    }

    private static void collect(Object value, Class<?> type, Set<Object> seen, int depth, List<Object> found)
        throws IllegalAccessException {
        if (value == null || depth > 6 || !seen.add(value)) {
            return;
        }
        if (type.isInstance(value)) {
            found.add(value);
            return;
        }
        Class<?> c = value.getClass();
        String name = c.getName();
        if (c.isArray() || name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")
            || name.startsWith("sun.") || name.startsWith("com.sun.")) {
            return;
        }
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field field : k.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                } catch (RuntimeException e) {
                    continue;
                }
                collect(field.get(value), type, seen, depth + 1, found);
            }
        }
    }

    /** Static credentials, never refreshed. */
    static final class StaticCredentials implements IdentityProvider<AwsCredentialsIdentity> {
        private static final AwsCredentialsIdentity IDENTITY = AwsCredentialsIdentity.create(ACCESS_KEY, SECRET_KEY);

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(IDENTITY);
        }
    }
}
