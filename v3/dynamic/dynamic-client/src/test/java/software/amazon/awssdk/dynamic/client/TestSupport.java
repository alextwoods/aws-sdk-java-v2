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

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.zip.CRC32;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.dynamic.model.DynamicModel;
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
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ShapeId;

/** Test models, a controllable in-memory transport and recording interceptors for the dynamic client tests. */
final class TestSupport {
    static final String NS = "example.weather";
    static final ShapeId WEATHER = ShapeId.from(NS + "#Weather");

    /**
     * An awsJson1_0, SigV4 service with an endpoint rule set bound through a context parameter, a finite operation
     * with every kind of member, a Unit operation, a streaming operation and modeled errors.
     */
    static final String WEATHER_MODEL = """
        $version: "2"
        namespace example.weather

        use aws.auth#sigv4
        use aws.protocols#awsJson1_0
        use smithy.rules#contextParam
        use smithy.rules#endpointRuleSet

        @awsJson1_0
        @sigv4(name: "weather")
        @aws.api#service(sdkId: "Weather")
        @endpointRuleSet({
            version: "1.0"
            parameters: {
                Region: { type: "string", builtIn: "AWS::Region", required: false, documentation: "The region." }
                City: { type: "string", required: false, documentation: "The city." }
            }
            rules: [
                {
                    type: "endpoint"
                    conditions: [
                        { fn: "isSet", argv: [{ ref: "City" }] }
                        { fn: "isSet", argv: [{ ref: "Region" }] }
                    ]
                    endpoint: { url: "https://{City}.weather.{Region}.example.com" }
                }
                {
                    type: "endpoint"
                    conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
                    endpoint: { url: "https://weather.{Region}.example.com" }
                }
                { type: "error", conditions: [], error: "A region is required" }
            ]
        })
        service Weather {
            version: "2020-01-01"
            operations: [GetForecast, Ping, Upload]
            errors: [ServiceFault]
        }

        operation GetForecast {
            input := {
                @required
                @contextParam(name: "City")
                city: String
                days: Integer
                small: Byte
                tags: TagList
                labels: Labels
                when: Timestamp
                unit: TemperatureUnit
                location: Location
                filter: Filter
                @idempotencyToken
                token: String
            }
            output := {
                city: String
                high: Integer
                readings: Readings
            }
            errors: [NotFound]
        }

        operation Ping {}

        operation Upload {
            input := {
                @required
                body: Body
            }
        }

        @streaming
        blob Body

        list TagList {
            member: String
        }

        map Labels {
            key: String
            value: String
        }

        enum TemperatureUnit {
            CELSIUS
            FAHRENHEIT
        }

        structure Location {
            @required
            latitude: Double
            longitude: Double
        }

        union Filter {
            minimum: Integer
            maximum: Integer
        }

        list Readings {
            member: Reading
        }

        structure Reading {
            hour: Integer
            value: Double
        }

        @error("client")
        structure NotFound {
            message: String
            city: String
        }

        @error("server")
        structure ServiceFault {
            message: String
        }
        """;

    /** Two services; selecting one needs its shape ID. */
    static final String TWO_SERVICES_MODEL = """
        $version: "2"
        namespace example.multi

        @aws.protocols#awsJson1_1
        service Zeta {
            operations: [ZetaPing]
        }

        @aws.protocols#awsJson1_1
        service Alpha {
            operations: [AlphaPing]
        }

        operation ZetaPing {}

        operation AlphaPing {}
        """;

    /** No service. */
    static final String NO_SERVICE_MODEL = """
        $version: "2"
        namespace example.none

        structure Lonely {}
        """;

    static final ShapeId MEDIA = ShapeId.from("example.media#Media");

    /**
     * A restJson1 service without auth (as the generated async behavior fixture's operations) with a streaming blob
     * input, a streaming blob output, an input and an output event stream, a paginated operation and a waitable
     * operation with output, inputOutput, errorType and success acceptors.
     */
    static final String MEDIA_MODEL = """
        $version: "2"
        namespace example.media

        use smithy.waiters#waitable

        @aws.protocols#restJson1
        service Media {
            version: "2024-01-01"
            operations: [PutBlob, GetBlob, Chat, Watch, ListItems, GetItem]
        }

        @http(method: "PUT", uri: "/blobs")
        operation PutBlob {
            input := {
                @httpHeader("X-Name")
                name: String

                @httpPayload
                @required
                body: StreamingBlob
            }
            output := {
                @httpHeader("X-Etag")
                etag: String
            }
        }

        @readonly
        @http(method: "GET", uri: "/blobs/{name}")
        operation GetBlob {
            input := {
                @required
                @httpLabel
                name: String
            }
            output := {
                @httpHeader("X-Kind")
                kind: String

                @httpPayload
                @required
                body: StreamingBlob
            }
        }

        @streaming
        blob StreamingBlob

        @http(method: "POST", uri: "/chat")
        operation Chat {
            input := {
                @httpHeader("X-Room")
                room: String

                @httpPayload
                messages: Messages
            }
            output := {}
        }

        @readonly
        @http(method: "GET", uri: "/watch")
        operation Watch {
            input := {}
            output := {
                @httpPayload
                events: Messages
            }
        }

        @streaming
        union Messages {
            message: Message
        }

        structure Message {
            text: String
        }

        @readonly
        @http(method: "GET", uri: "/items")
        @paginated(inputToken: "nextToken", outputToken: "nextToken", items: "items", pageSize: "limit")
        operation ListItems {
            input := {
                @httpQuery("next")
                nextToken: String

                @httpQuery("limit")
                limit: Integer
            }
            output := {
                nextToken: String
                items: ItemList
            }
        }

        list ItemList {
            member: String
        }

        @readonly
        @http(method: "GET", uri: "/items/{id}")
        @waitable(
            ItemReady: {
                minDelay: 2
                acceptors: [
                    {
                        state: "success"
                        matcher: { output: { path: "value", expected: "READY", comparator: "stringEquals" } }
                    }
                    {
                        state: "failure"
                        matcher: { output: { path: "value", expected: "FAILED", comparator: "stringEquals" } }
                    }
                    {
                        state: "retry"
                        matcher: { errorType: "ItemMissing" }
                    }
                ]
            }
            ItemEchoed: {
                acceptors: [
                    {
                        state: "success"
                        matcher: { inputOutput: { path: "output.id == input.id", expected: "true", comparator: "booleanEquals" } }
                    }
                ]
            }
            ItemTagged: {
                acceptors: [
                    {
                        state: "success"
                        matcher: { output: { path: "length(tags[?@ == 'hot']) > `0`", expected: "true", comparator: "booleanEquals" } }
                    }
                    {
                        state: "success"
                        matcher: { success: false }
                    }
                ]
            }
        )
        operation GetItem {
            input := {
                @required
                @httpLabel
                id: String
            }
            output := {
                id: String
                value: String
                tags: ItemList
            }
            errors: [ItemMissing]
        }

        @error("client")
        @httpError(404)
        structure ItemMissing {
            message: String
        }
        """;

    /** Static test credentials, so SigV4 never consults the default chain. */
    static final IdentityProvider<AwsCredentialsIdentity> CREDENTIALS = new IdentityProvider<>() {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(
                AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"));
        }
    };

    private static DynamicModel weather;
    private static DynamicModel media;

    private TestSupport() {
    }

    /** The weather model, loaded once through its JSON AST. */
    static synchronized DynamicModel weather() {
        if (weather == null) {
            weather = load(WEATHER_MODEL);
        }
        return weather;
    }

    /** The media model, loaded once through its JSON AST. */
    static synchronized DynamicModel media() {
        if (media == null) {
            media = load(MEDIA_MODEL);
        }
        return media;
    }

    /** A sync client of the media service over {@code http}, isolated from the environment. */
    static DynamicClient.Builder mediaClient(SdkSyncHttpClient http) {
        return DynamicClient.builder()
                            .model(media())
                            .region(Region.US_EAST_1)
                            .endpointOverride(URI.create("https://media.test"))
                            .configurationSources(ConfigurationSources.none())
                            .httpClient(http);
    }

    /** An async client of the media service over {@code http}, isolated from the environment. */
    static DynamicAsyncClient.Builder mediaAsyncClient(SdkAsyncHttpClient http) {
        return DynamicAsyncClient.builder()
                                 .model(media())
                                 .region(Region.US_EAST_1)
                                 .endpointOverride(URI.create("https://media.test"))
                                 .configurationSources(ConfigurationSources.none())
                                 .httpClient(http);
    }

    /** A restJson1 response with {@code headers}. */
    static SdkHttpResponse restJson(int status, String body, String... headers) {
        SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(status)
                                                          .addHeader("Content-Type", "application/json");
        for (int i = 0; i < headers.length; i += 2) {
            response.addHeader(headers[i], headers[i + 1]);
        }
        return response.body(HttpBody.fromUtf8(body)).build();
    }

    /**
     * One encoded {@code message} event of the {@code Messages} event stream, with {@code text}, in the
     * {@code application/vnd.amazon.eventstream} binary format: prelude (total length, headers length, prelude
     * CRC32), string headers, the JSON payload, and the message CRC32.
     */
    static byte[] messageFrame(String text) {
        ByteArrayOutputStream headers = new ByteArrayOutputStream();
        stringHeader(headers, ":message-type", "event");
        stringHeader(headers, ":event-type", "message");
        stringHeader(headers, ":content-type", "application/json");
        byte[] headerBytes = headers.toByteArray();
        byte[] payload = ("{\"text\":\"" + text + "\"}").getBytes(StandardCharsets.UTF_8);
        int total = 12 + headerBytes.length + payload.length + 4;
        ByteBuffer frame = ByteBuffer.allocate(total);
        frame.putInt(total).putInt(headerBytes.length);
        frame.putInt((int) crc32(frame.array(), 0, 8));
        frame.put(headerBytes).put(payload);
        frame.putInt((int) crc32(frame.array(), 0, total - 4));
        return frame.array();
    }

    private static void stringHeader(ByteArrayOutputStream out, String name, String value) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        out.write(nameBytes.length);
        out.writeBytes(nameBytes);
        out.write(7);
        out.write(valueBytes.length >> 8);
        out.write(valueBytes.length & 0xFF);
        out.writeBytes(valueBytes);
    }

    private static long crc32(byte[] bytes, int offset, int length) {
        CRC32 crc = new CRC32();
        crc.update(bytes, offset, length);
        return crc.getValue();
    }

    /** {@code idl} loaded as a runtime model through its JSON AST. */
    static DynamicModel load(String idl) {
        ClassLoader classLoader = TestSupport.class.getClassLoader();
        Model model = Model.assembler(classLoader).discoverModels(classLoader)
                           .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                           .addUnparsedModel("test-model.smithy", idl)
                           .assemble().unwrap();
        String jsonAst = Node.prettyPrintJson(ModelSerializer.builder().build().serialize(model));
        return DynamicModel.fromJsonAst(jsonAst, "test-model.json");
    }

    /** A sync client of the weather service over {@code transport}, isolated from the environment. */
    static DynamicClient.Builder syncClient(TestTransport transport) {
        return DynamicClient.builder()
                            .model(weather())
                            .region(Region.US_WEST_2)
                            .credentialsProvider(CREDENTIALS)
                            .configurationSources(ConfigurationSources.none())
                            .httpClient(transport);
    }

    /** An async client of the weather service over {@code transport}, isolated from the environment. */
    static DynamicAsyncClient.Builder asyncClient(TestTransport transport) {
        return DynamicAsyncClient.builder()
                                 .model(weather())
                                 .region(Region.US_WEST_2)
                                 .credentialsProvider(CREDENTIALS)
                                 .configurationSources(ConfigurationSources.none())
                                 .httpClient(transport);
    }

    /** A map document of {@code members}. */
    static Document doc(Map<String, Document> members) {
        return Document.of(members);
    }

    /** An awsJson1_0 response. */
    static SdkHttpResponse json(int status, String body) {
        return SdkHttpResponse.builder()
                              .statusCode(status)
                              .addHeader("Content-Type", "application/x-amz-json-1.0")
                              .body(HttpBody.fromUtf8(body))
                              .build();
    }

    /**
     * An in-memory sync and async transport: records every request with its buffered body and answers it with the
     * response its responder builds. An async transport may hold its responses until {@link #release()}.
     */
    static final class TestTransport implements SdkSyncHttpClient, SdkAsyncHttpClient {
        private final HttpTransportMode mode;
        private final Function<Recorded, SdkHttpResponse> responder;
        private final List<Recorded> requests = new CopyOnWriteArrayList<>();
        private final List<Runnable> held = new ArrayList<>();
        private boolean hold;

        private TestTransport(HttpTransportMode mode, Function<Recorded, SdkHttpResponse> responder) {
            this.mode = mode;
            this.responder = responder;
        }

        static TestTransport sync(Function<Recorded, SdkHttpResponse> responder) {
            return new TestTransport(HttpTransportMode.SYNC, responder);
        }

        static TestTransport async(Function<Recorded, SdkHttpResponse> responder) {
            return new TestTransport(HttpTransportMode.ASYNC, responder);
        }

        /** Holds every async response until {@link #release()}. */
        synchronized TestTransport holdResponses() {
            hold = true;
            return this;
        }

        /** Delivers every held response, and every later one immediately. */
        void release() {
            List<Runnable> deliveries;
            synchronized (this) {
                hold = false;
                deliveries = new ArrayList<>(held);
                held.clear();
            }
            deliveries.forEach(Runnable::run);
        }

        /** Every request transmitted so far, in order. */
        List<Recorded> requests() {
            return List.copyOf(requests);
        }

        int transmissions() {
            return requests.size();
        }

        private SdkHttpResponse respond(SdkHttpRequest request) {
            ByteBuffer buffer = request.body().buffered().asByteBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Recorded recorded = new Recorded(request, new String(bytes, StandardCharsets.UTF_8));
            requests.add(recorded);
            return responder.apply(recorded);
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
            Runnable delivery = () -> {
                ByteBuffer body = response.body().buffered().asByteBuffer();
                handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
                handler.onStream(publisherOf(body));
                exchange.complete();
            };
            synchronized (this) {
                if (hold) {
                    held.add(delivery);
                    return exchange;
                }
            }
            delivery.run();
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
            return "TestTransport";
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

    /** A request the transport received and its body as UTF-8. */
    record Recorded(SdkHttpRequest request, String body) {
        String header(String name) {
            return request.headers().firstValue(name).orElse(null);
        }
    }

    /** Records every hook it sees, with the operation of the call, in order. */
    static final class RecordingInterceptor implements ExecutionInterceptor {
        final List<String> hooks = new CopyOnWriteArrayList<>();
        final List<HookContext> contexts = new CopyOnWriteArrayList<>();

        private void record(String hook, HookContext context) {
            hooks.add(hook);
            contexts.add(context);
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            record("beforeExecution", context);
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            record("beforeTransmission", context);
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            record("afterExecution", context);
        }
    }
}
