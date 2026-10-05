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

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.statistics.Statistics;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.dynamic.client.DynamicAsyncClient;
import software.amazon.awssdk.dynamic.client.DynamicClient;
import software.amazon.awssdk.dynamic.model.DynamicModel;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageDecoder;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.eventstream.SigV4FrameSignatureAlgorithm;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.pipeline.AuthOptionsResolver;
import software.amazon.awssdk.pipeline.client.AwsClientBuildState;
import software.amazon.awssdk.protocols.core.eventstream.PublisherEventStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.test.codegen.ClientProjectionLifecyclePropertyTest.StaticCredentials;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 54 (Requirements 14.16, 19.6): generated and dynamic calls are wire equivalent.
 *
 * <p>A synthetic model with one SigV4 service per launch protocol ({@code awsJson1_0}, {@code awsJson1_1},
 * {@code rpcv2Cbor}, {@code awsQuery}, {@code ec2Query}, {@code restJson1}, {@code restXml}) sharing its shapes is
 * generated and compiled once, and its JSON AST is loaded once as the Dynamic Client's runtime model. Every service has
 * an endpoint rule set bound to the region, the endpoint override and an input context parameter, and operations
 * covering: every scalar type (byte, short, integer, long, float, double, big integer, big decimal, boolean, string,
 * blob, enum, intEnum) and timestamps with the default, {@code epoch-seconds}, {@code http-date} and
 * {@code date-time} formats; nested structures, lists, maps and a union whose structures declare optional members
 * before required ones; an {@code @idempotencyToken} member; an {@code httpChecksum} operation
 * ({@code requestChecksumRequired} and a {@code requestAlgorithmMember}); and an {@code endpoint} {@code hostPrefix}
 * operation with a host label that is also a context parameter. The two REST services also bind HTTP labels, query
 * strings (scalar, list, timestamp), headers (scalar, boolean, timestamp, list), prefix headers and a structure
 * {@code @httpPayload}.
 *
 * <p>Each try generates an operation, its modeled input (one map of values, converted to one {@link Document} that the
 * generated client reads into its request type and the Dynamic Client receives as is), the client configuration
 * (region, endpoint override or not, retries disabled, static credentials, a fixed signing instant) and a scripted
 * reply (a modeled success with output headers and body in the protocol's format, a modeled error, or an unmodeled
 * error status). The call runs through the generated sync client (the reference) and the {@link DynamicClient}.
 *
 * <p>Oracle, byte equality against the generated client: every transmitted request's method, scheme, authority and raw
 * target (path and raw query), its complete ordered header list (names and values exactly, including
 * {@code Authorization}, {@code X-Amz-Date}, checksum and content headers) and its body bytes; then the terminal
 * outcome, the modeled output viewed as a {@link Document}, or the failure with its modeled error shape and members,
 * status, error code, message and request ID (see {@link ClientProjectionLifecyclePropertyTest#failure}). Nothing is
 * excluded. Normally nondeterministic metadata is made deterministic instead: the idempotency token is set
 * explicitly in every input that has one, and the signing clock is fixed. Neither public builder exposes signer
 * properties, so both clients get the same override after they are built: their call engine's auth options resolver
 * is wrapped to add {@link HttpSigner#SIGNING_CLOCK} with {@link Clock#fixed} to every candidate option
 * ({@link #fixSigningClock}). V3 emits no {@code amz-sdk-invocation-id}.
 *
 * <p>Event streams ({@link #dynamicEventStreamCallsAreWireEquivalentToGeneratedCalls}): the {@code awsJson1_0},
 * {@code awsJson1_1} and {@code rpcv2Cbor} services bind an output event stream operation ({@code RpcWatch}, with an
 * {@code initial-response}) and an input event stream operation ({@code RpcUpload}); the {@code restJson1} service
 * binds an output ({@code RestSubscribe}), an input ({@code RestPublish}) and a bidirectional ({@code RestChat})
 * operation with HTTP-bound members. Event union members cover scalars, blobs, timestamps, lists, a nested structure,
 * {@code @eventHeader} members of several header types, an {@code @eventPayload} blob and structure, and events that
 * declare an optional member before a required one. Each try generates the input, zero to four input events, the
 * client configuration and a reply: a stream of zero to four events ending normally, with the modeled exception
 * event or with an error frame; a buffered success; a modeled error; or an unmodeled error status. The call runs
 * through the generated async client (its {@code op(Request, Publisher, Handler)} overloads, the input events built
 * as generated union values) and the {@link DynamicAsyncClient} (input events from
 * {@link DynamicAsyncClient#inputEvent}, published through {@link PublisherEventStream}), over an async transport
 * that drains the request body before it answers on its own event loop. Oracle, byte equality against the generated
 * client: the request line and ordered headers, the complete request body bytes, and, decoded from them, every signed
 * envelope's headers in order, whether its {@code :chunk-signature} chains from the prior signature (the first from
 * the request's) under {@link SigV4FrameSignatureAlgorithm}, and each wrapped event's headers and payload bytes; then
 * the response handler's callbacks in order (the initial response, every received event, {@code onError} or
 * {@code onComplete}, {@code exceptionOccurred} or {@code complete}) and the future's outcome. The generated client's
 * own frames must also chain validly. Every call is bounded by {@value #STREAM_TIMEOUT_SECONDS} seconds, and a call
 * that does not end fails the try with its trace.
 *
 * <p>Not covered:
 * <ul>
 *   <li>Streaming blob members: their bodies are covered by {@code GeneratedStreamingClientTest}, and this property's
 *       sync transport compares buffered bodies only.</li>
 *   <li>Event streams on {@code awsQuery} and {@code ec2Query}: those protocols do not support event streams.</li>
 *   <li>Event streams on {@code restXml}: the protocol supports them, but this property's reply encoder writes JSON
 *       and CBOR event payloads only, so the {@code restXml} service binds no event stream operation.</li>
 *   <li>Presigning: the Dynamic Client has no presigner.</li>
 * </ul>
 */
class GeneratedDynamicWireEquivalencePropertyTest {
    private static final String NAMESPACE = "example.wire";
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.wire.";
    private static final URI ENDPOINT_OVERRIDE = URI.create("https://override.example.test");
    private static final AwsCredentialsIdentity IDENTITY =
        AwsCredentialsIdentity.create("AKIDWIRE", "wire-secret-key");
    private static final String XMLNS = "https://example.test/wire/";

    private static final String RULES = """
        @smithy.rules#endpointRuleSet({
            version: "1.0"
            parameters: {
                Region: { type: "string", builtIn: "AWS::Region", required: false, documentation: "The region." }
                Endpoint: { type: "string", builtIn: "SDK::Endpoint", required: false, documentation: "An override." }
                Zone: { type: "string", required: false, documentation: "The zone." }
            }
            rules: [
                {
                    type: "endpoint"
                    conditions: [{ fn: "isSet", argv: [{ ref: "Endpoint" }] }]
                    endpoint: { url: { ref: "Endpoint" } }
                }
                {
                    type: "endpoint"
                    conditions: [
                        { fn: "isSet", argv: [{ ref: "Zone" }] }
                        { fn: "isSet", argv: [{ ref: "Region" }] }
                    ]
                    endpoint: { url: "https://PREFIX.{Region}.example.test/{Zone}" }
                }
                {
                    type: "endpoint"
                    conditions: [{ fn: "isSet", argv: [{ ref: "Region" }] }]
                    endpoint: { url: "https://PREFIX.{Region}.example.test" }
                }
                { type: "error", conditions: [], error: "A region is required" }
            ]
        })
        """;

    private static final String SERVICES = """
        @aws.protocols#awsJson1_0
        @aws.auth#sigv4(name: "jsonzero")
        @aws.api#service(sdkId: "JsonZero", endpointPrefix: "jsonzero")
        RULES_jsonzero
        service JsonZero {
            version: "2024-01-01"
            operations: [Send, Checked, Locate, RpcWatch, RpcUpload]
        }

        @aws.protocols#awsJson1_1
        @aws.auth#sigv4(name: "jsonone")
        @aws.api#service(sdkId: "JsonOne", endpointPrefix: "jsonone")
        RULES_jsonone
        service JsonOne {
            version: "2024-01-01"
            operations: [Send, Checked, Locate, RpcWatch, RpcUpload]
        }

        @smithy.protocols#rpcv2Cbor
        @aws.auth#sigv4(name: "cbor")
        @aws.api#service(sdkId: "Cbor", endpointPrefix: "cbor")
        RULES_cbor
        service Cbor {
            version: "2024-01-01"
            operations: [Send, Checked, Locate, RpcWatch, RpcUpload]
        }

        @aws.protocols#awsQuery
        @xmlNamespace(uri: "XMLNS")
        @aws.auth#sigv4(name: "query")
        @aws.api#service(sdkId: "Query", endpointPrefix: "query")
        RULES_query
        service Query {
            version: "2024-01-01"
            operations: [Send, Checked, Locate]
        }

        @aws.protocols#ec2Query
        @xmlNamespace(uri: "XMLNS")
        @aws.auth#sigv4(name: "ec2")
        @aws.api#service(sdkId: "Ec2", endpointPrefix: "ec2")
        RULES_ec2
        service Ec2 {
            version: "2024-01-01"
            operations: [Send, Checked, Locate]
        }

        @aws.protocols#restJson1
        @aws.auth#sigv4(name: "restjson")
        @aws.api#service(sdkId: "RestJson", endpointPrefix: "restjson")
        RULES_restjson
        service RestJson {
            version: "2024-01-01"
            operations: [RestSend, RestPayload, RestChecked, RestLocate, RestSubscribe, RestPublish, RestChat]
        }

        @aws.protocols#restXml
        @xmlNamespace(uri: "XMLNS")
        @aws.auth#sigv4(name: "restxml")
        @aws.api#service(sdkId: "RestXml", endpointPrefix: "restxml")
        RULES_restxml
        service RestXml {
            version: "2024-01-01"
            operations: [RestSend, RestPayload, RestChecked, RestLocate]
        }
        """;

    private static final String SHAPES = """
        operation Send {
            input := with [Scalars] {
                @idempotencyToken
                token: String

                @smithy.rules#contextParam(name: "Zone")
                zone: String

                @required
                key: String
            }
            output := with [Outputs] {}
            errors: [Bad]
        }

        @aws.protocols#httpChecksum(requestChecksumRequired: true, requestAlgorithmMember: "checksumAlgorithm")
        operation Checked {
            input := {
                data: String
                checksumAlgorithm: ChecksumAlgorithm
            }
            output := with [Outputs] {}
            errors: [Bad]
        }

        @endpoint(hostPrefix: "{zone}.data.")
        operation Locate {
            input := {
                name: String

                @required
                @hostLabel
                @smithy.rules#contextParam(name: "Zone")
                zone: String
            }
            output := with [Outputs] {}
            errors: [Bad]
        }

        @http(method: "POST", uri: "/items/{id}")
        operation RestSend {
            input := with [Scalars] {
                @required
                @httpLabel
                id: String

                @httpQuery("q")
                q: String

                @httpQuery("n")
                n: Integer

                @httpQuery("tag")
                qtags: TagList

                @httpQuery("at")
                at: Timestamp

                @httpHeader("X-Flag")
                hflag: Boolean

                @httpHeader("X-Stamp")
                stamp: Timestamp

                @httpHeader("X-List")
                hlist: TagList

                @httpPrefixHeaders("X-Meta-")
                meta: AttrMap

                @httpHeader("X-Token")
                @idempotencyToken
                token: String

                @smithy.rules#contextParam(name: "Zone")
                @httpHeader("X-Zone")
                zone: String
            }
            output := with [Outputs] {
                @httpHeader("X-Version")
                version: Integer
            }
            errors: [Bad]
        }

        @idempotent
        @http(method: "PUT", uri: "/payload/{id}")
        operation RestPayload {
            input := {
                @required
                @httpLabel
                id: String

                @httpPayload
                body: Inner
            }
            output := {
                @httpHeader("X-Version")
                version: Integer

                @httpPayload
                body: Inner
            }
            errors: [Bad]
        }

        @aws.protocols#httpChecksum(requestChecksumRequired: true, requestAlgorithmMember: "checksumAlgorithm")
        @http(method: "POST", uri: "/checked")
        operation RestChecked {
            input := {
                data: String

                @httpHeader("x-amz-sdk-checksum-algorithm")
                checksumAlgorithm: ChecksumAlgorithm
            }
            output := with [Outputs] {}
            errors: [Bad]
        }

        @endpoint(hostPrefix: "{zone}.data.")
        @http(method: "POST", uri: "/locate")
        operation RestLocate {
            input := {
                name: String

                @required
                @hostLabel
                @smithy.rules#contextParam(name: "Zone")
                zone: String
            }
            output := with [Outputs] {}
            errors: [Bad]
        }

        @http(method: "POST", uri: "/subscribe/{id}")
        operation RestSubscribe {
            input := {
                @required
                @httpLabel
                id: String

                @httpHeader("X-Topic")
                topic: String

                @httpQuery("q")
                q: String
            }
            output := {
                @httpHeader("X-Session")
                session: String

                @httpHeader("X-Version")
                version: Integer

                @httpPayload
                events: OutEvents
            }
            errors: [Bad]
        }

        @http(method: "POST", uri: "/publish/{id}")
        operation RestPublish {
            input := {
                @required
                @httpLabel
                id: String

                @httpHeader("X-Topic")
                topic: String

                @httpPayload
                events: InEvents
            }
            output := {
                @httpHeader("X-Version")
                version: Integer

                name: String
                inner: Inner
            }
            errors: [Bad]
        }

        @http(method: "POST", uri: "/chat")
        operation RestChat {
            input := {
                @httpHeader("X-Topic")
                topic: String

                @httpPayload
                events: InEvents
            }
            output := {
                @httpHeader("X-Session")
                session: String

                @httpPayload
                events: OutEvents
            }
            errors: [Bad]
        }

        operation RpcWatch {
            input := {
                name: String
                inner: Inner
            }
            output := {
                status: String
                count: Integer
                inner: Inner
                events: OutEvents
            }
            errors: [Bad]
        }

        operation RpcUpload {
            input := {
                name: String
                inner: Inner
                events: InEvents
            }
            output := {
                count: Integer
                name: String
            }
            errors: [Bad]
        }

        @streaming
        union OutEvents {
            note: Note
            chunk: Chunk
            tagged: Tagged
            mixed: Mixed
            failure: Failure
        }

        @streaming
        union InEvents {
            note: Note
            chunk: Chunk
            tagged: Tagged
            mixed: Mixed
        }

        structure Note {
            text: String
            flag: Boolean

            @required
            level: Integer

            big: Long
            data: Blob
            when: Timestamp
            inner: Inner
            tags: TagList
        }

        structure Chunk {
            @eventHeader
            kind: String

            @eventPayload
            data: Blob
        }

        structure Tagged {
            @eventHeader
            id: String

            @eventHeader
            seq: Integer

            @eventHeader
            flag: Boolean

            @eventHeader
            at: Timestamp

            @eventHeader
            big: Long

            @eventPayload
            inner: Inner
        }

        structure Mixed {
            @eventHeader
            tag: String

            label: String

            @required
            level: Integer
        }

        @error("client")
        structure Failure {
            message: String
            reason: String
            inner: Inner
        }

        @mixin
        structure Scalars {
            text: String
            flag: Boolean
            tiny: Byte
            small: Short
            num: Integer
            big: Long
            ratio: Float
            precise: Double
            hugeInt: BigInteger
            hugeDec: BigDecimal
            data: Blob
            when: Timestamp

            @timestampFormat("epoch-seconds")
            epoch: Timestamp

            @timestampFormat("http-date")
            httpDate: Timestamp

            @timestampFormat("date-time")
            dateTime: Timestamp

            color: Color
            level: Level
            inner: Inner
            items: InnerList
            tags: TagList
            attrs: AttrMap
            innerMap: InnerMap
            choice: Choice
        }

        @mixin
        structure Outputs {
            name: String
            count: Integer
            flag: Boolean
            created: Timestamp
            tags: TagList
            color: Color
            inner: Inner
        }

        structure Inner {
            label: String

            @required
            level: Integer

            extra: Long
        }

        union Choice {
            text: String
            number: Integer
            inner: Inner
        }

        list TagList {
            member: String
        }

        list InnerList {
            member: Inner
        }

        map AttrMap {
            key: String
            value: String
        }

        map InnerMap {
            key: String
            value: Inner
        }

        enum Color {
            RED
            GREEN
            BLUE
        }

        intEnum Level {
            LOW = 1
            HIGH = 2
        }

        enum ChecksumAlgorithm {
            CRC32
            SHA256
        }

        @error("client")
        @httpError(400)
        structure Bad {
            message: String
            reason: String
        }
        """;

    private static final String MODEL;

    static {
        String services = SERVICES.replace("XMLNS", XMLNS);
        for (Proto proto : Proto.values()) {
            services = services.replace("RULES_" + proto.prefix + "\n",
                                        RULES.replace("PREFIX", proto.prefix));
        }
        MODEL = "$version: \"2\"\nnamespace " + NAMESPACE + "\n\n" + services + "\n" + SHAPES;
    }

    private static URLClassLoader loader;
    private static DynamicModel runtimeModel;

    @BeforeContainer
    static void generateAndCompile() throws Exception {
        Path work = Files.createTempDirectory(Files.createDirectories(Path.of("target", "wire-equivalence")),
                                              "fixture-");
        Path models = Files.createDirectories(work.resolve("model"));
        Path modelFile = models.resolve("wire.smithy");
        Files.writeString(modelFile, MODEL, StandardCharsets.UTF_8);
        Path sources = work.resolve("sources");
        for (Proto proto : Proto.values()) {
            AwsCodegenResult result = AwsCodegen.generate(
                new AwsCodegenRequest(NAMESPACE + "#" + proto.sdkId, proto.pkg(), models, work.resolve(proto.prefix)),
                List.of());
            ClientProjectionLifecyclePropertyTest.copyTree(result.sourceRoot(), sources);
        }
        loader = CompiledSources.compile(sources, work.resolve("classes")).loader();
        runtimeModel = DynamicModel.fromJsonAst(ClientProjectionLifecyclePropertyTest.jsonAst(modelFile), "wire.json");
    }

    @AfterContainer
    static void close() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    // The combined edge cases of the larger inputs outnumber the tries and would crowd out the other operations, so
    // edge cases are off and the numeric generators mix in their extremes instead.
    @Property(tries = 1000, edgeCases = EdgeCasesMode.NONE)
    void dynamicCallsAreWireEquivalentToGeneratedCalls(@ForAll("scenarios") Scenario scenario) throws Exception {
        Trace reference = run(false, scenario);
        Trace dynamic = run(true, scenario);
        Statistics.label("operation").collect(scenario.proto() + " " + scenario.operation());
        Statistics.label("outcome").collect(scenario.proto() + " " + outcome(reference.terminal));
        assertFalse(reference.requests.isEmpty(), "the generated client sent nothing:\n" + reference.render());
        assertEquals(reference.render(), dynamic.render(),
                     "the Dynamic Client's wire differs from the generated client's for " + scenario);
    }

    /** The kind of a terminal outcome, for the statistics. */
    private static String outcome(String terminal) {
        if (terminal.startsWith("success")) {
            return terminal.equals("success structure{}") ? "success (no members)" : "success";
        }
        if (terminal.contains("modeled=")) {
            return "modeled error";
        }
        if (terminal.contains("unmodeled")) {
            return "unmodeled error";
        }
        int colon = terminal.indexOf(':');
        return colon < 0 ? terminal : terminal.substring(0, colon);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Generators

    @Provide
    Arbitrary<Scenario> scenarios() {
        // One arbitrary per protocol and operation, chosen uniformly (each with its own edge cases).
        List<Arbitrary<? extends Scenario>> calls = new ArrayList<>();
        for (Proto proto : Proto.values()) {
            for (String operation : proto.operations()) {
                calls.add(Combinators.combine(
                    input(operation),
                    reply(proto, operation),
                    Arbitraries.of(Region.US_EAST_1, Region.US_WEST_2, Region.EU_CENTRAL_1),
                    Arbitraries.of(false, true),
                    Arbitraries.longs().between(1_600_000_000L, 1_900_000_000L).map(Instant::ofEpochSecond))
                    .as((input, reply, region, override, signedAt) ->
                            new Scenario(proto, operation, input, reply, region, override, signedAt)));
            }
        }
        return Arbitraries.oneOf(calls);
    }

    private static Arbitrary<Map<String, Object>> input(String operation) {
        return switch (operation) {
            case "Send" -> members(concat(scalars(), required("token", token()), optional("zone", label()),
                                          required("key", text())));
            case "Checked", "RestChecked" -> members(optional("data", text()),
                                                     optional("checksumAlgorithm", Arbitraries.of("CRC32", "SHA256")));
            case "Locate", "RestLocate" -> members(optional("name", text()), required("zone", label()));
            case "RestSend" -> members(concat(scalars(),
                                              required("id", nonEmptyText()),
                                              optional("q", text()),
                                              optional("n", ints()),
                                              optional("qtags", list(text())),
                                              optional("at", instants()),
                                              optional("hflag", Arbitraries.of(true, false)),
                                              optional("stamp", instants()),
                                              optional("hlist", list(token())),
                                              optional("meta", Arbitraries.maps(label(), token()).ofMaxSize(3)),
                                              required("token", token()),
                                              optional("zone", label())));
            case "RestPayload" -> members(required("id", nonEmptyText()), optional("body", inner()));
            default -> throw new IllegalArgumentException(operation);
        };
    }

    private static Member[] scalars() {
        return new Member[] {
            optional("text", text()),
            optional("flag", Arbitraries.of(true, false)),
            optional("tiny", extremes(Arbitraries.bytes(), Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 0)),
            optional("small", extremes(Arbitraries.shorts(), Short.MIN_VALUE, Short.MAX_VALUE, (short) 0)),
            optional("num", ints()),
            optional("big", extremes(Arbitraries.longs(), Long.MIN_VALUE, Long.MAX_VALUE, 0L)),
            optional("ratio", Arbitraries.floats().between(-1e6f, 1e6f).ofScale(2)),
            optional("precise", Arbitraries.doubles().between(-1e9, 1e9).ofScale(4)),
            optional("hugeInt", Arbitraries.bigIntegers().between(BigInteger.TEN.pow(20).negate(),
                                                                  BigInteger.TEN.pow(20))),
            optional("hugeDec", Arbitraries.bigDecimals().between(BigDecimal.valueOf(-1e12), BigDecimal.valueOf(1e12))
                                           .ofScale(3)),
            optional("data", Arbitraries.bytes().array(byte[].class).ofMaxSize(16)),
            optional("when", instants()),
            optional("epoch", instants()),
            optional("httpDate", instants()),
            optional("dateTime", instants()),
            optional("color", Arbitraries.of("RED", "GREEN", "BLUE")),
            optional("level", Arbitraries.of(1, 2)),
            optional("inner", inner()),
            optional("items", list(inner())),
            optional("tags", list(text())),
            optional("attrs", Arbitraries.maps(token(), text()).ofMaxSize(3)),
            optional("innerMap", Arbitraries.maps(token(), inner()).ofMaxSize(2)),
            optional("choice", Arbitraries.oneOf(text().map(v -> Map.<String, Object>of("text", v)),
                                                 ints().map(v -> Map.<String, Object>of("number", v)),
                                                 inner().map(v -> Map.<String, Object>of("inner", v))))
        };
    }

    private static Arbitrary<Map<String, Object>> inner() {
        return members(optional("label", text()), required("level", Arbitraries.integers().between(-1000, 1000)),
                       optional("extra", extremes(Arbitraries.longs(), Long.MIN_VALUE, Long.MAX_VALUE, 0L)));
    }

    private static <T> Arbitrary<List<T>> list(Arbitrary<T> items) {
        return items.list().ofMaxSize(3);
    }

    private static Arbitrary<Reply> reply(Proto proto, String operation) {
        Arbitrary<Reply> success = output(operation).map(Reply.Success::new);
        Arbitrary<Reply> modeled = Combinators.combine(token(), text().injectNull(0.3))
                                              .as(Reply.Modeled::new);
        Arbitrary<Reply> unmodeled = Arbitraries.of(new Reply.Unmodeled(500, "InternalFailure"),
                                                    new Reply.Unmodeled(403, "AccessDenied"),
                                                    new Reply.Unmodeled(400, "ValidationError"));
        return Arbitraries.frequencyOf(net.jqwik.api.Tuple.of(3, success), net.jqwik.api.Tuple.of(1, modeled),
                                       net.jqwik.api.Tuple.of(1, unmodeled));
    }

    private static Arbitrary<Map<String, Object>> output(String operation) {
        if (operation.equals("RestPayload")) {
            return members(optional("version", Arbitraries.integers().between(0, 100)), optional("body", inner()));
        }
        Member[] members = {
            optional("name", text()),
            optional("count", ints()),
            optional("flag", Arbitraries.of(true, false)),
            optional("created", Arbitraries.longs().between(0, 2_000_000_000L).map(Instant::ofEpochSecond)),
            optional("tags", list(token())),
            optional("color", Arbitraries.of("RED", "GREEN", "BLUE")),
            optional("inner", inner())
        };
        return operation.equals("RestSend")
               ? members(concat(members, optional("version", Arbitraries.integers().between(0, 100))))
               : members(members);
    }

    private static Arbitrary<Map<String, Object>> members(Member... members) {
        List<Arbitrary<Object>> values = new ArrayList<>();
        for (Member member : members) {
            Arbitrary<Object> value = member.values.map(v -> (Object) v);
            values.add(member.required ? value : value.injectNull(0.5));
        }
        return Combinators.combine(values).as(generated -> {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < members.length; i++) {
                if (generated.get(i) != null) {
                    map.put(members[i].name, generated.get(i));
                }
            }
            return map;
        });
    }

    private static Member optional(String name, Arbitrary<?> values) {
        return new Member(name, false, values);
    }

    private static Member required(String name, Arbitrary<?> values) {
        return new Member(name, true, values);
    }

    private static Member[] concat(Member[] first, Member... second) {
        Member[] out = new Member[first.length + second.length];
        System.arraycopy(first, 0, out, 0, first.length);
        System.arraycopy(second, 0, out, first.length, second.length);
        return out;
    }

    private static Arbitrary<Integer> ints() {
        return extremes(Arbitraries.integers(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1);
    }

    /** {@code random} mixed with its {@code extremes}, which edge cases would otherwise supply. */
    @SafeVarargs
    private static <T> Arbitrary<T> extremes(Arbitrary<T> random, T... extremes) {
        return Arbitraries.frequencyOf(net.jqwik.api.Tuple.of(4, random),
                                       net.jqwik.api.Tuple.of(1, Arbitraries.of(extremes)));
    }

    /** Text with characters every encoding must escape or percent-encode. */
    private static Arbitrary<String> text() {
        return Arbitraries.strings().withCharRange('a', 'z').numeric()
                          .withChars(' ', '/', '?', '&', '=', '%', '+', '<', '>', '"', '\'', 'é', '中')
                          .ofMaxLength(10);
    }

    private static Arbitrary<String> nonEmptyText() {
        return text().filter(s -> !s.isEmpty());
    }

    /** Header-safe tokens. */
    private static Arbitrary<String> token() {
        return Arbitraries.strings().withCharRange('a', 'z').numeric().withChars('-', '_', '.')
                          .ofMinLength(1).ofMaxLength(10);
    }

    /** Valid host labels. */
    private static Arbitrary<String> label() {
        return Arbitraries.strings().withCharRange('a', 'z').numeric().ofMinLength(1).ofMaxLength(8);
    }

    private static Arbitrary<Instant> instants() {
        return Arbitraries.longs().between(0, 4_000_000_000_000L).map(Instant::ofEpochMilli);
    }

    record Member(String name, boolean required, Arbitrary<?> values) {
    }

    sealed interface Reply {
        record Success(Map<String, Object> output) implements Reply {
        }

        record Modeled(String reason, String message) implements Reply {
        }

        record Unmodeled(int status, String code) implements Reply {
        }
    }

    record Scenario(Proto proto, String operation, Map<String, Object> input, Reply reply, Region region,
                    boolean endpointOverride, Instant signedAt) {
    }

    enum Proto {
        AWS_JSON_1_0("JsonZero", "jsonzero", "application/x-amz-json-1.0"),
        AWS_JSON_1_1("JsonOne", "jsonone", "application/x-amz-json-1.1"),
        RPCV2_CBOR("Cbor", "cbor", "application/cbor"),
        AWS_QUERY("Query", "query", "text/xml"),
        EC2_QUERY("Ec2", "ec2", "text/xml;charset=UTF-8"),
        REST_JSON_1("RestJson", "restjson", "application/json"),
        REST_XML("RestXml", "restxml", "application/xml");

        private final String sdkId;
        private final String prefix;
        private final String contentType;

        Proto(String sdkId, String prefix, String contentType) {
            this.sdkId = sdkId;
            this.prefix = prefix;
            this.contentType = contentType;
        }

        String pkg() {
            return PACKAGE + prefix;
        }

        ShapeId id() {
            return ShapeId.fromParts(NAMESPACE, sdkId);
        }

        boolean rest() {
            return this == REST_JSON_1 || this == REST_XML;
        }

        List<String> operations() {
            return rest() ? List.of("RestSend", "RestPayload", "RestChecked", "RestLocate")
                          : List.of("Send", "Checked", "Locate");
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Running a call

    /** One client's wire trace: every transmitted request, exactly, and the terminal outcome. */
    static final class Trace {
        final List<String> requests = new ArrayList<>();
        String terminal = "none ";

        String render() {
            return String.join("\n", requests) + "\n-- terminal\n" + terminal;
        }
    }

    private static Trace run(boolean dynamic, Scenario scenario) throws Exception {
        Trace trace = new Trace();
        ScriptedHttp http = new ScriptedHttp(scenario, trace);
        Document input = ClientProjectionLifecyclePropertyTest.toDocument(scenario.input());
        AwsRequestOverrideConfiguration override = AwsRequestOverrideConfiguration.builder().build();
        Clock clock = Clock.fixed(scenario.signedAt(), ZoneOffset.UTC);
        try (AutoCloseable client = dynamic ? dynamicClient(scenario, http) : generatedClient(scenario, http)) {
            fixSigningClock(client, clock);
            Object result;
            try {
                result = dynamic ? ((DynamicClient) client).call(scenario.operation(), input, override)
                                 : generatedCall(client, scenario, input, override);
            } catch (InvocationTargetException e) {
                trace.terminal = "failure " + ClientProjectionLifecyclePropertyTest.failure(e.getCause());
                return trace;
            } catch (RuntimeException e) {
                trace.terminal = "failure " + ClientProjectionLifecyclePropertyTest.failure(e);
                return trace;
            }
            trace.terminal = "success "
                             + ClientProjectionLifecyclePropertyTest.render(Document.of((SerializableStruct) result));
        }
        return trace;
    }

    private static AutoCloseable dynamicClient(Scenario scenario, ScriptedHttp http) {
        DynamicClient.Builder builder = DynamicClient.builder().model(runtimeModel).serviceId(scenario.proto().id())
            .region(scenario.region()).credentialsProvider(new StaticCredentials(IDENTITY))
            .retryStrategy(noRetries()).configurationSources(ConfigurationSources.none()).httpClient(http);
        if (scenario.endpointOverride()) {
            builder.endpointOverride(ENDPOINT_OVERRIDE);
        }
        return builder.build();
    }

    private static StandardRetryStrategy noRetries() {
        return StandardRetryStrategy.builder().maxAttempts(1).backoffStrategy(BackoffStrategy.none())
                                    .throttlingBackoffStrategy(BackoffStrategy.none()).build();
    }

    /**
     * The generated sync client configured exactly as the dynamic one, through its public builder (external
     * configuration sources disabled through the build state, which the public builder does not expose).
     */
    private static AutoCloseable generatedClient(Scenario scenario, ScriptedHttp http) throws Exception {
        Proto proto = scenario.proto();
        Class<?> clientType = Class.forName(proto.pkg() + "." + proto.sdkId + "Client", true, loader);
        Class<?> builderType = Class.forName(proto.pkg() + "." + proto.sdkId + "ClientBuilder", true, loader);
        Object builder = clientType.getMethod("builder").invoke(null);
        invoke(builderType, builder, "region", scenario.region());
        invoke(builderType, builder, "credentialsProvider", new StaticCredentials(IDENTITY));
        invoke(builderType, builder, "retryStrategy", noRetries());
        invoke(builderType, builder, "httpClient", http);
        if (scenario.endpointOverride()) {
            invoke(builderType, builder, "endpointOverride", ENDPOINT_OVERRIDE);
        }
        Field state = field(builder.getClass(), "state");
        ((AwsClientBuildState) state.get(builder)).configurationSources(ConfigurationSources.none());
        return (AutoCloseable) builderType.getMethod("build").invoke(builder);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object generatedCall(Object client, Scenario scenario, Document input,
                                        AwsRequestOverrideConfiguration override) throws Exception {
        Proto proto = scenario.proto();
        String operation = scenario.operation();
        Class<?> clientType = Class.forName(proto.pkg() + "." + proto.sdkId + "Client", true, loader);
        Class<?> requestType = Class.forName(proto.pkg() + ".model." + operation + "Request", true, loader);
        Object request = input.asShape((ShapeBuilder) requestType.getMethod("builder").invoke(null));
        String method = Character.toLowerCase(operation.charAt(0)) + operation.substring(1);
        return clientType.getMethod(method, requestType, AwsRequestOverrideConfiguration.class)
                         .invoke(client, request, override);
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
     * {@link HttpSigner#SIGNING_CLOCK} to every candidate. The same override applies to both clients, which share
     * the engine type; no public builder exposes signer properties.
     */
    static void fixSigningClock(Object client, Clock clock) throws Exception {
        Class<?> stagesType = Class.forName("software.amazon.awssdk.pipeline.CallStages");
        List<Object> stages = new ArrayList<>();
        collect(client, stagesType, Collections.newSetFromMap(new IdentityHashMap<>()), 0, stages);
        assertEquals(1, stages.size(), "expected one call engine in " + client);
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

    // -------------------------------------------------------------------------------------------------------------
    // The scripted transport and the protocol response formats

    /** The exact text of a transmitted request: target, every header in order, and the body bytes. */
    static String wire(SdkHttpRequest request) {
        byte[] body = switch (request.body().kind()) {
            case EMPTY -> new byte[0];
            case BYTES -> request.body().asByteArray();
            case STREAMING -> request.body().buffered().asByteArray();
        };
        StringBuilder out = new StringBuilder(request.method()).append(' ').append(request.scheme()).append("://")
                                                               .append(request.authority())
                                                               .append(request.rawTarget()).append('\n');
        HttpHeaders headers = request.headers();
        for (int i = 0; i < headers.size(); i++) {
            out.append(headers.name(i)).append(": ").append(headers.value(i)).append('\n');
        }
        out.append("body[").append(body.length).append("]=");
        escape(out, body);
        return out.toString();
    }

    /** {@code bytes} as printable ASCII, every other byte as {@code \xNN}. */
    private static StringBuilder escape(StringBuilder out, byte[] bytes) {
        for (byte b : bytes) {
            int c = b & 0xff;
            if (c >= 0x20 && c < 0x7f && c != '\\') {
                out.append((char) c);
            } else {
                out.append(String.format("\\x%02x", c));
            }
        }
        return out;
    }

    static final class ScriptedHttp implements SdkSyncHttpClient {
        private final Scenario scenario;
        private final Trace trace;

        ScriptedHttp(Scenario scenario, Trace trace) {
            this.scenario = scenario;
            this.trace = trace;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    trace.requests.add(wire(request));
                    return Responses.of(scenario, "req-" + trace.requests.size());
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
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(HttpTransportMode.SYNC).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(java.time.Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "WireHttp";
        }

        @Override
        public void close() {
        }
    }

    /** Responses in each protocol's wire format. */
    static final class Responses {
        private Responses() {
        }

        static SdkHttpResponse of(Scenario scenario, String requestId) {
            Proto proto = scenario.proto();
            String operation = scenario.operation();
            SdkHttpResponse.Builder response = SdkHttpResponse.builder().addHeader("x-amzn-RequestId", requestId)
                                                              .addHeader("Content-Type", proto.contentType);
            if (proto == Proto.RPCV2_CBOR) {
                response.addHeader("smithy-protocol", "rpc-v2-cbor");
            }
            return switch (scenario.reply()) {
                case Reply.Success success -> {
                    Map<String, Object> body = new LinkedHashMap<>(success.output());
                    Object version = body.remove("version");
                    if (version != null) {
                        response.addHeader("X-Version", version.toString());
                    }
                    if (operation.equals("RestPayload")) {
                        Object inner = body.get("body");
                        body = inner == null ? null : castMap(inner);
                    }
                    yield response.statusCode(200).body(success(proto, operation, body, requestId)).build();
                }
                case Reply.Modeled error -> {
                    Map<String, Object> members = new LinkedHashMap<>();
                    if (error.message() != null) {
                        members.put("message", error.message());
                    }
                    members.put("reason", error.reason());
                    yield error(response.statusCode(400), proto, "Bad", members, requestId);
                }
                case Reply.Unmodeled error -> error(response.statusCode(error.status()), proto, error.code(),
                                                     Map.of("message", "unmodeled " + error.code()), requestId);
            };
        }

        private static HttpBody success(Proto proto, String operation, Map<String, Object> members,
                                        String requestId) {
            if (members == null) {
                return HttpBody.empty();
            }
            return switch (proto) {
                case AWS_JSON_1_0, AWS_JSON_1_1, REST_JSON_1 -> HttpBody.fromUtf8(json(members));
                case RPCV2_CBOR -> HttpBody.fromBytes(Cbor.encode(members));
                case REST_XML -> {
                    String root = operation.equals("RestPayload") ? "Inner" : operation + "Output";
                    yield HttpBody.fromUtf8("<" + root + " xmlns=\"" + XMLNS + "\">" + xml(members) + "</" + root
                                            + ">");
                }
                case AWS_QUERY -> HttpBody.fromUtf8("<" + operation + "Response xmlns=\"" + XMLNS + "\"><"
                                                    + operation + "Result>" + xml(members) + "</" + operation
                                                    + "Result><ResponseMetadata><RequestId>" + requestId
                                                    + "</RequestId></ResponseMetadata></" + operation
                                                    + "Response>");
                case EC2_QUERY -> HttpBody.fromUtf8("<" + operation + "Response xmlns=\"" + XMLNS + "\"><requestId>"
                                                    + requestId + "</requestId>" + xml(members) + "</" + operation
                                                    + "Response>");
            };
        }

        private static SdkHttpResponse error(SdkHttpResponse.Builder response, Proto proto, String code,
                                             Map<String, Object> members, String requestId) {
            Map<String, Object> typed = new LinkedHashMap<>();
            String body = switch (proto) {
                case AWS_JSON_1_0, AWS_JSON_1_1 -> {
                    typed.put("__type", NAMESPACE + "#" + code);
                    typed.putAll(members);
                    yield json(typed);
                }
                case REST_JSON_1 -> {
                    response.addHeader("X-Amzn-ErrorType", code);
                    yield json(members);
                }
                case RPCV2_CBOR -> {
                    typed.put("__type", NAMESPACE + "#" + code);
                    typed.putAll(members);
                    yield null;
                }
                case REST_XML, AWS_QUERY -> "<ErrorResponse><Error><Type>Sender</Type><Code>" + code + "</Code>"
                                            + errorXml(members) + "</Error><RequestId>" + requestId
                                            + "</RequestId></ErrorResponse>";
                case EC2_QUERY -> "<Response><Errors><Error><Code>" + code + "</Code>" + errorXml(members)
                                  + "</Error></Errors><RequestID>" + requestId + "</RequestID></Response>";
            };
            return response.body(body == null ? HttpBody.fromBytes(Cbor.encode(typed)) : HttpBody.fromUtf8(body))
                           .build();
        }

        private static String errorXml(Map<String, Object> members) {
            StringBuilder out = new StringBuilder();
            members.forEach((name, value) -> {
                String element = name.equals("message") ? "Message" : name;
                out.append('<').append(element).append('>').append(escape(value.toString())).append("</")
                   .append(element).append('>');
            });
            return out.toString();
        }

        private static String json(Map<String, Object> members) {
            Map<String, Object> converted = new LinkedHashMap<>();
            members.forEach((k, v) -> converted.put(k, v instanceof Instant t ? t.getEpochSecond() : v));
            return ClientProjectionLifecyclePropertyTest.json(converted);
        }

        private static String xml(Map<String, Object> members) {
            StringBuilder out = new StringBuilder();
            members.forEach((name, value) -> {
                out.append('<').append(name).append('>');
                if (value instanceof Map<?, ?> map) {
                    out.append(xml(castMap(map)));
                } else if (value instanceof List<?> list) {
                    list.forEach(item -> out.append("<member>").append(escape(item.toString())).append("</member>"));
                } else {
                    out.append(escape(value.toString()));
                }
                out.append("</").append(name).append('>');
            });
            return out.toString();
        }

        private static String escape(String value) {
            return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                        .replace("'", "&apos;");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    /** A minimal CBOR encoder for the response values: maps, arrays, text, integers, booleans and epoch timestamps. */
    static final class Cbor {
        private Cbor() {
        }

        static byte[] encode(Object value) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            write(out, value);
            return out.toByteArray();
        }

        private static void write(ByteArrayOutputStream out, Object value) {
            if (value instanceof String s) {
                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                head(out, 3, bytes.length);
                out.writeBytes(bytes);
            } else if (value instanceof byte[] bytes) {
                head(out, 2, bytes.length);
                out.writeBytes(bytes);
            } else if (value instanceof Boolean b) {
                out.write(b ? 0xf5 : 0xf4);
            } else if (value instanceof Integer || value instanceof Long) {
                long n = ((Number) value).longValue();
                if (n >= 0) {
                    head(out, 0, n);
                } else {
                    head(out, 1, -1 - n);
                }
            } else if (value instanceof Instant t) {
                out.write(0xc1);
                write(out, t.getEpochSecond());
            } else if (value instanceof List<?> list) {
                head(out, 4, list.size());
                list.forEach(item -> write(out, item));
            } else if (value instanceof Map<?, ?> map) {
                head(out, 5, map.size());
                map.forEach((k, v) -> {
                    write(out, k);
                    write(out, v);
                });
            } else {
                throw new IllegalArgumentException(String.valueOf(value));
            }
        }

        private static void head(ByteArrayOutputStream out, int major, long n) {
            int type = major << 5;
            if (n < 24) {
                out.write(type | (int) n);
            } else if (n < 0x100) {
                out.write(type | 24);
                out.write((int) n);
            } else if (n < 0x10000) {
                out.write(type | 25);
                out.write((int) (n >> 8));
                out.write((int) n);
            } else if (n < 0x100000000L) {
                out.write(type | 26);
                for (int shift = 24; shift >= 0; shift -= 8) {
                    out.write((int) (n >> shift));
                }
            } else {
                out.write(type | 27);
                for (int shift = 56; shift >= 0; shift -= 8) {
                    out.write((int) (n >> shift));
                }
            }
        }
    }

    // =============================================================================================================
    // Event streams

    private static final long STREAM_TIMEOUT_SECONDS = 20;
    private static final String SECRET = "wire-secret-key";
    private static final String EVENT_STREAM = "application/vnd.amazon.eventstream";

    @Property(tries = 300, edgeCases = EdgeCasesMode.NONE)
    void dynamicEventStreamCallsAreWireEquivalentToGeneratedCalls(@ForAll("streamScenarios") StreamScenario scenario)
        throws Exception {
        StreamTrace reference = runStream(false, scenario);
        StreamTrace dynamic = runStream(true, scenario);
        Statistics.label("operation").collect(scenario.proto() + " " + scenario.operation());
        Statistics.label("outcome").collect(scenario.proto() + " " + streamOutcome(reference));
        Statistics.label("input events").collect(scenario.inputEvents().size());
        String expected = reference.render(scenario);
        String actual = dynamic.render(scenario);
        assertFalse(reference.request == null, "the generated client sent nothing:\n" + expected);
        assertFalse(reference.terminal.equals("timed out"), "the generated call did not end within "
                                                           + STREAM_TIMEOUT_SECONDS + "s:\n" + expected);
        assertFalse(dynamic.terminal.equals("timed out"), "the Dynamic Client's call did not end within "
                                                         + STREAM_TIMEOUT_SECONDS + "s:\n" + actual);
        assertFalse(expected.contains("INVALID"), "the generated client's frames are not chain-signed:\n" + expected);
        assertEquals(expected, actual,
                     "the Dynamic Client's event stream call differs from the generated client's for " + scenario);
    }

    /** The kind of an event stream call's end, for the statistics. */
    private static String streamOutcome(StreamTrace trace) {
        String terminal = trace.terminal;
        String kind;
        if (terminal.startsWith("failure")) {
            kind = terminal.contains("modeled=") ? "modeled failure"
                   : terminal.contains("unmodeled") ? "unmodeled failure"
                   : "failure " + terminal.substring("failure ".length(), Math.max(terminal.indexOf(':'), 8));
        } else {
            kind = terminal.startsWith("success") ? "success" : terminal;
        }
        return trace.handler.stream().anyMatch(l -> l.startsWith("event ")) ? kind + " after events" : kind;
    }

    record Event(String member, Map<String, Object> values) {
    }

    record StreamScenario(Proto proto, String operation, Map<String, Object> input, List<Event> inputEvents,
                          StreamReply reply, Region region, boolean endpointOverride, Instant signedAt) {
    }

    sealed interface StreamReply {
        /** An event stream response: the non-stream output members, the events, and how the stream ends. */
        record Stream(Map<String, Object> output, List<Event> events, Terminal terminal) implements StreamReply {
        }

        /** The buffered success of an operation with only an input event stream. */
        record Buffered(Map<String, Object> output) implements StreamReply {
        }

        record Modeled(String reason, String message) implements StreamReply {
        }

        record Unmodeled(int status, String code) implements StreamReply {
        }
    }

    sealed interface Terminal {
        /** The stream ends after its events. */
        record End() implements Terminal {
        }

        /** The stream ends with the modeled {@code failure} exception event. */
        record ExceptionEvent(Map<String, Object> members) implements Terminal {
        }

        /** The stream ends with an error frame. */
        record ErrorFrame(String code, String message) implements Terminal {
        }
    }

    private static List<String> streamOperations(Proto proto) {
        return proto == Proto.REST_JSON_1 ? List.of("RestSubscribe", "RestPublish", "RestChat")
                                          : List.of("RpcWatch", "RpcUpload");
    }

    private static boolean hasInputStream(String operation) {
        return !operation.equals("RestSubscribe") && !operation.equals("RpcWatch");
    }

    private static boolean hasOutputStream(String operation) {
        return !operation.equals("RestPublish") && !operation.equals("RpcUpload");
    }

    @Provide
    Arbitrary<StreamScenario> streamScenarios() {
        List<Arbitrary<? extends StreamScenario>> calls = new ArrayList<>();
        for (Proto proto : List.of(Proto.REST_JSON_1, Proto.AWS_JSON_1_0, Proto.AWS_JSON_1_1, Proto.RPCV2_CBOR)) {
            for (String operation : streamOperations(proto)) {
                Arbitrary<List<Event>> events = hasInputStream(operation) ? event(instants()).list().ofMaxSize(4)
                                                                          : Arbitraries.just(List.of());
                calls.add(Combinators.combine(
                    streamInput(operation),
                    events,
                    streamReply(operation),
                    Arbitraries.of(Region.US_EAST_1, Region.US_WEST_2, Region.EU_CENTRAL_1),
                    Arbitraries.of(false, true),
                    Arbitraries.longs().between(1_600_000_000_000L, 1_900_000_000_000L).map(Instant::ofEpochMilli))
                    .as((input, inputEvents, reply, region, override, signedAt) ->
                            new StreamScenario(proto, operation, input, inputEvents, reply, region, override,
                                               signedAt)));
            }
        }
        return Arbitraries.oneOf(calls);
    }

    private static Arbitrary<Map<String, Object>> streamInput(String operation) {
        return switch (operation) {
            case "RestSubscribe" -> members(required("id", nonEmptyText()), optional("topic", token()),
                                            optional("q", text()));
            case "RestPublish" -> members(required("id", nonEmptyText()), optional("topic", token()));
            case "RestChat" -> members(optional("topic", token()));
            case "RpcWatch", "RpcUpload" -> members(optional("name", text()), optional("inner", inner()));
            default -> throw new IllegalArgumentException(operation);
        };
    }

    /**
     * One event of {@code InEvents} (or the non-exception members of {@code OutEvents}): scalars, a nested structure
     * that declares an optional member before a required one, blobs, header members and payload members.
     */
    private static Arbitrary<Event> event(Arbitrary<Instant> bodyInstants) {
        Arbitrary<byte[]> blobs = Arbitraries.bytes().array(byte[].class).ofMaxSize(16);
        Arbitrary<Long> longs = extremes(Arbitraries.longs(), Long.MIN_VALUE, Long.MAX_VALUE, 0L);
        Arbitrary<Boolean> flags = Arbitraries.of(true, false);
        List<Arbitrary<? extends Event>> kinds = List.of(
            members(optional("text", text()), optional("flag", flags), required("level", ints()),
                    optional("big", longs), optional("data", blobs), optional("when", bodyInstants),
                    optional("inner", inner()), optional("tags", list(text())))
                .map(values -> new Event("note", values)),
            members(optional("kind", text()), optional("data", blobs)).map(values -> new Event("chunk", values)),
            members(optional("id", text()), optional("seq", ints()), optional("flag", flags),
                    optional("at", instants()), optional("big", longs), optional("inner", inner()))
                .map(values -> new Event("tagged", values)),
            members(optional("tag", text()), optional("label", text()), required("level", ints()))
                .map(values -> new Event("mixed", values)));
        return Arbitraries.oneOf(kinds);
    }

    private static Arbitrary<StreamReply> streamReply(String operation) {
        Arbitrary<StreamReply> success;
        if (hasOutputStream(operation)) {
            Arbitrary<Terminal> end = Arbitraries.just(new Terminal.End());
            Arbitrary<Terminal> exception = members(optional("message", text()), optional("reason", token()),
                                                    optional("inner", inner()))
                .map(Terminal.ExceptionEvent::new);
            Arbitrary<Terminal> error = Combinators.combine(Arbitraries.of("Boom", "InternalFailure"), text())
                                                   .as(Terminal.ErrorFrame::new);
            Arbitrary<Terminal> terminal = Arbitraries.frequencyOf(net.jqwik.api.Tuple.of(3, end),
                                                                   net.jqwik.api.Tuple.of(1, exception),
                                                                   net.jqwik.api.Tuple.of(1, error));
            // Body timestamps of response events are whole seconds, which the CBOR encoder writes exactly.
            Arbitrary<Instant> seconds = Arbitraries.longs().between(0, 4_000_000_000L).map(Instant::ofEpochSecond);
            success = Combinators.combine(streamOutput(operation), event(seconds).list().ofMaxSize(4), terminal)
                                 .as(StreamReply.Stream::new);
        } else {
            success = streamOutput(operation).map(StreamReply.Buffered::new);
        }
        Arbitrary<StreamReply> modeled = Combinators.combine(token(), text().injectNull(0.3))
                                                    .as(StreamReply.Modeled::new);
        Arbitrary<StreamReply> unmodeled = Arbitraries.of(new StreamReply.Unmodeled(500, "InternalFailure"),
                                                          new StreamReply.Unmodeled(403, "AccessDenied"));
        return Arbitraries.frequencyOf(net.jqwik.api.Tuple.of(4, success), net.jqwik.api.Tuple.of(1, modeled),
                                       net.jqwik.api.Tuple.of(1, unmodeled));
    }

    /** The non-stream output members of a success. */
    private static Arbitrary<Map<String, Object>> streamOutput(String operation) {
        Arbitrary<Integer> versions = Arbitraries.integers().between(0, 100);
        return switch (operation) {
            case "RestSubscribe" -> members(optional("session", token()), optional("version", versions));
            case "RestChat" -> members(optional("session", token()));
            case "RestPublish" -> members(optional("version", versions), optional("name", text()),
                                          optional("inner", inner()));
            case "RpcWatch" -> members(optional("status", text()), optional("count", ints()),
                                       optional("inner", inner()));
            case "RpcUpload" -> members(optional("count", ints()), optional("name", text()));
            default -> throw new IllegalArgumentException(operation);
        };
    }

    // -------------------------------------------------------------------------------------------------------------
    // Running an event stream call

    /** One client's event stream call: the request, its body bytes and frames, the handler's log and the end. */
    static final class StreamTrace {
        final AtomicInteger requests = new AtomicInteger();
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        final List<String> handler = new CopyOnWriteArrayList<>();
        volatile SdkHttpRequest request;
        volatile String bodyError;
        volatile String terminal = "none";

        String render(StreamScenario scenario) {
            StringBuilder out = new StringBuilder("requests=").append(requests.get()).append('\n');
            SdkHttpRequest sent = request;
            if (sent != null) {
                out.append(sent.method()).append(' ').append(sent.scheme()).append("://").append(sent.authority())
                   .append(sent.rawTarget()).append('\n');
                HttpHeaders headers = sent.headers();
                for (int i = 0; i < headers.size(); i++) {
                    out.append(headers.name(i)).append(": ").append(headers.value(i)).append('\n');
                }
                byte[] bytes;
                synchronized (body) {
                    bytes = body.toByteArray();
                }
                out.append("body ").append(sent.body().kind()).append('[').append(bytes.length).append("]=");
                escape(out, bytes).append('\n');
                if (bodyError != null) {
                    out.append("body error ").append(bodyError).append('\n');
                }
                if (sent.body().kind() == HttpBody.Kind.STREAMING) {
                    requestFrames(out, sent, bytes, scenario);
                }
            }
            out.append("-- handler\n");
            handler.forEach(line -> out.append(line).append('\n'));
            return out.append("-- terminal\n").append(terminal).toString();
        }
    }

    /**
     * The decoded request frames: each signed envelope's headers in order and whether its signature chains from the
     * prior one (the first from the request signature), and each wrapped event's headers and payload bytes.
     */
    private static void requestFrames(StringBuilder out, SdkHttpRequest request, byte[] body,
                                      StreamScenario scenario) {
        out.append("-- request frames\n");
        List<Message> envelopes;
        try {
            envelopes = new MessageDecoder().decode(ByteBuffer.wrap(body));
        } catch (RuntimeException e) {
            out.append("undecodable ").append(e).append('\n');
            return;
        }
        String authorization = request.headers().firstValue("Authorization").orElse("");
        int at = authorization.indexOf("Signature=");
        byte[] prior = at < 0 ? new byte[0] : HexFormat.of().parseHex(authorization.substring(at + 10));
        String region = scenario.region().id();
        String service = scenario.proto().prefix;
        SigV4FrameSignatureAlgorithm algorithm = SigV4FrameSignatureAlgorithm.create(
            region, service, date -> GeneratedEventStreamClientTest.sigV4Key(
                SECRET, date.format(DateTimeFormatter.BASIC_ISO_DATE), region, service));
        for (int i = 0; i < envelopes.size(); i++) {
            Message envelope = envelopes.get(i);
            out.append("frame ").append(i).append(' ');
            renderHeaders(out, envelope.headers());
            HeaderValue date = envelope.headers().get(":date");
            HeaderValue signature = envelope.headers().get(":chunk-signature");
            if (date == null || signature == null) {
                out.append(" chain=INVALID (unsigned)\n");
                continue;
            }
            Map<String, HeaderValue> dateHeader = new LinkedHashMap<>();
            dateHeader.put(":date", date);
            byte[] expected = algorithm.sign(prior, date.asTimestamp(), MessageEncoder.encodeHeaders(dateHeader),
                                             ByteBuffer.wrap(envelope.payloadBytes()));
            byte[] actual = signature.asByteArray();
            out.append(Arrays.equals(expected, actual) ? " chain=valid\n" : " chain=INVALID\n");
            prior = actual;
            if (envelope.payloadBytes().length == 0) {
                out.append("  end\n");
                continue;
            }
            for (Message inner : new MessageDecoder().decode(ByteBuffer.wrap(envelope.payloadBytes()))) {
                out.append("  event ");
                renderHeaders(out, inner.headers());
                out.append(" payload=");
                escape(out, inner.payloadBytes()).append('\n');
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

    private static StreamTrace runStream(boolean dynamic, StreamScenario scenario) throws Exception {
        StreamTrace trace = new StreamTrace();
        StreamHttp http = new StreamHttp(scenario, trace);
        Clock clock = Clock.fixed(scenario.signedAt(), ZoneOffset.UTC);
        try (AutoCloseable client = dynamic ? dynamicAsyncClient(scenario, http)
                                            : generatedAsyncClient(scenario, http)) {
            fixSigningClock(client, clock);
            StreamRecorder recorder = new StreamRecorder(trace.handler);
            CompletableFuture<?> future;
            try {
                future = dynamic ? dynamicStreamCall((DynamicAsyncClient) client, scenario, recorder)
                                 : generatedStreamCall(client, scenario, recorder);
            } catch (InvocationTargetException e) {
                trace.terminal = "thrown " + ClientProjectionLifecyclePropertyTest.failure(e.getCause());
                return trace;
            } catch (RuntimeException e) {
                trace.terminal = "thrown " + ClientProjectionLifecyclePropertyTest.failure(e);
                return trace;
            }
            try {
                Object result = future.get(STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                trace.terminal = result == null ? "complete" : "success " + renderValue(result);
            } catch (ExecutionException e) {
                trace.terminal = "failure " + ClientProjectionLifecyclePropertyTest.failure(e.getCause());
            } catch (TimeoutException e) {
                trace.terminal = "timed out";
            }
        } finally {
            http.close();
        }
        return trace;
    }

    /** A modeled value, a document or a generated model type, as a document. */
    private static String renderValue(Object value) {
        Document document = value instanceof Document d ? d : Document.of((SerializableStruct) value);
        return ClientProjectionLifecyclePropertyTest.render(document);
    }

    /**
     * A received event: its union member name and that member's value. {@code Document.of} views a generated union
     * as a structure document while a dynamic event is a union document, so the union container itself is not
     * rendered; it has exactly one member either way.
     */
    private static String renderEvent(Object event) {
        Document document = event instanceof Document d ? d : Document.of((SerializableStruct) event);
        Map<String, Document> members = document.asStringMap();
        if (members.size() != 1) {
            return "not one member: " + renderValue(event);
        }
        Map.Entry<String, Document> member = members.entrySet().iterator().next();
        return member.getKey() + "=" + ClientProjectionLifecyclePropertyTest.render(member.getValue());
    }

    private static Document eventDocument(Event event) {
        return ClientProjectionLifecyclePropertyTest.toDocument(Map.of(event.member(), event.values()));
    }

    private static AutoCloseable dynamicAsyncClient(StreamScenario scenario, StreamHttp http) {
        DynamicAsyncClient.Builder builder = DynamicAsyncClient.builder().model(runtimeModel)
            .serviceId(scenario.proto().id()).region(scenario.region())
            .credentialsProvider(new StaticCredentials(IDENTITY)).retryStrategy(noRetries())
            .configurationSources(ConfigurationSources.none()).httpClient(http);
        if (scenario.endpointOverride()) {
            builder.endpointOverride(ENDPOINT_OVERRIDE);
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<?> dynamicStreamCall(DynamicAsyncClient client, StreamScenario scenario,
                                                          StreamRecorder recorder) {
        String operation = scenario.operation();
        Map<String, Document> members = new LinkedHashMap<>();
        scenario.input().forEach((name, value) -> members.put(name, ClientProjectionLifecyclePropertyTest
            .toDocument(value)));
        if (hasInputStream(operation)) {
            List<SerializableStruct> events = new ArrayList<>();
            for (Event event : scenario.inputEvents()) {
                events.add(client.inputEvent(operation, eventDocument(event)));
            }
            members.put("events", Document.ofObject(PublisherEventStream.of(new ListPublisher<>(events))));
        }
        Document input = Document.of(members);
        if (!hasOutputStream(operation)) {
            return client.callAsync(operation, input);
        }
        EventStreamResponseHandler<Document, Document> handler =
            (EventStreamResponseHandler<Document, Document>) (EventStreamResponseHandler<?, ?>) recorder;
        return client.callAsync(operation, input, handler);
    }

    /** The generated async client configured exactly as the dynamic one, through its public builder. */
    private static AutoCloseable generatedAsyncClient(StreamScenario scenario, StreamHttp http) throws Exception {
        Proto proto = scenario.proto();
        Class<?> clientType = Class.forName(proto.pkg() + "." + proto.sdkId + "AsyncClient", true, loader);
        Method builderMethod = clientType.getMethod("builder");
        Class<?> builderType = builderMethod.getReturnType();
        Object builder = builderMethod.invoke(null);
        invoke(builderType, builder, "region", scenario.region());
        invoke(builderType, builder, "credentialsProvider", new StaticCredentials(IDENTITY));
        invoke(builderType, builder, "retryStrategy", noRetries());
        invoke(builderType, builder, "httpClient", http);
        if (scenario.endpointOverride()) {
            invoke(builderType, builder, "endpointOverride", ENDPOINT_OVERRIDE);
        }
        Field state = field(builder.getClass(), "state");
        ((AwsClientBuildState) state.get(builder)).configurationSources(ConfigurationSources.none());
        return (AutoCloseable) builderType.getMethod("build").invoke(builder);
    }

    /**
     * The generated event stream overload: {@code op(Request, Handler)}, {@code op(Request, Publisher)} or
     * {@code op(Request, Publisher, Handler)}; the handler is a proxy of {@code <Op>ResponseHandler} forwarding to
     * {@code recorder}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CompletableFuture<?> generatedStreamCall(Object client, StreamScenario scenario,
                                                            StreamRecorder recorder) throws Exception {
        Proto proto = scenario.proto();
        String operation = scenario.operation();
        String model = proto.pkg() + ".model.";
        Class<?> clientType = Class.forName(proto.pkg() + "." + proto.sdkId + "AsyncClient", true, loader);
        Class<?> requestType = Class.forName(model + operation + "Request", true, loader);
        Object request = ClientProjectionLifecyclePropertyTest.toDocument(scenario.input())
                                                              .asShape((ShapeBuilder) requestType.getMethod("builder")
                                                                                                 .invoke(null));
        List<Object> arguments = new ArrayList<>(List.of(request));
        if (hasInputStream(operation)) {
            Method unionBuilder = Class.forName(model + "InEvents", true, loader).getMethod("builder");
            List<Object> events = new ArrayList<>();
            for (Event event : scenario.inputEvents()) {
                events.add(eventDocument(event).asShape((ShapeBuilder) unionBuilder.invoke(null)));
            }
            arguments.add(new ListPublisher<>(events));
        }
        if (hasOutputStream(operation)) {
            Class<?> handlerType = Class.forName(model + operation + "ResponseHandler", true, loader);
            arguments.add(Proxy.newProxyInstance(loader, new Class<?>[] {handlerType},
                                                 (proxy, method, args) -> forward(recorder, proxy, method, args)));
        }
        String name = Character.toLowerCase(operation.charAt(0)) + operation.substring(1);
        for (Method method : clientType.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == arguments.size()
                && method.getParameterTypes()[0] == requestType) {
                return (CompletableFuture<?>) method.invoke(client, arguments.toArray());
            }
        }
        throw new NoSuchMethodException(clientType.getName() + "." + name + " with " + arguments.size());
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
            log.add("response " + renderValue(response));
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
                    log.add("onError " + ClientProjectionLifecyclePropertyTest.failure(t));
                }

                @Override
                public void onComplete() {
                    log.add("onComplete");
                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            log.add("exceptionOccurred " + ClientProjectionLifecyclePropertyTest.failure(throwable));
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

    /**
     * An async transport that drains a streaming request body, then answers on its event loop with the scenario's
     * reply: an event stream body of encoded frames, one frame per unit of demand, or a buffered body.
     */
    static final class StreamHttp implements SdkAsyncHttpClient {
        private final StreamScenario scenario;
        private final StreamTrace trace;
        private final ExecutorService loop = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "wire-event-loop");
            thread.setDaemon(true);
            return thread;
        });

        StreamHttp(StreamScenario scenario, StreamTrace trace) {
            this.scenario = scenario;
            this.trace = trace;
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            AtomicReference<Subscription> body = new AtomicReference<>();
            AsyncExchangeCompletion completion = new AsyncExchangeCompletion("WireStreamHttp", (reason, failure) -> {
                Subscription subscription = body.get();
                if (subscription != null) {
                    subscription.cancel();
                }
                handler.onError(failure);
            });
            int number = trace.requests.incrementAndGet();
            trace.request = request;
            loop.execute(() -> {
                switch (request.body().kind()) {
                    case EMPTY -> answer(handler, completion, number);
                    case BYTES -> {
                        synchronized (trace.body) {
                            trace.body.writeBytes(request.body().asByteArray());
                        }
                        answer(handler, completion, number);
                    }
                    case STREAMING -> request.body().streamingBody().subscribe(new Subscriber<ByteBuffer>() {
                        @Override
                        public void onSubscribe(Subscription subscription) {
                            body.set(subscription);
                            subscription.request(Long.MAX_VALUE);
                        }

                        @Override
                        public void onNext(ByteBuffer chunk) {
                            byte[] bytes = new byte[chunk.remaining()];
                            chunk.duplicate().get(bytes);
                            synchronized (trace.body) {
                                trace.body.writeBytes(bytes);
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            trace.bodyError = String.valueOf(t);
                            loop.execute(() -> {
                                handler.onError(t);
                                completion.fail(t);
                            });
                        }

                        @Override
                        public void onComplete() {
                            loop.execute(() -> answer(handler, completion, number));
                        }
                    });
                }
            });
            return completion;
        }

        /** Runs on the event loop. */
        private void answer(AsyncHttpResponseHandler handler, AsyncExchangeCompletion completion, int number) {
            if (completion.isDone()) {
                return;
            }
            String requestId = "req-" + number;
            SdkHttpResponse response;
            List<ByteBuffer> chunks;
            if (scenario.reply() instanceof StreamReply.Stream stream) {
                response = streamHeaders(scenario.proto(), stream.output(), requestId).build();
                chunks = responseFrames(scenario.proto(), stream);
            } else {
                SdkHttpResponse full = streamReplyResponse(scenario, requestId);
                byte[] bytes = full.body().kind() == HttpBody.Kind.EMPTY ? new byte[0] : full.body().asByteArray();
                response = full.toBuilder().body(HttpBody.empty()).build();
                chunks = bytes.length == 0 ? List.of() : List.of(ByteBuffer.wrap(bytes));
            }
            handler.onHeaders(response);
            handler.onStream(new ChunkPublisher(loop, chunks, completion::complete));
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(HttpTransportMode.ASYNC).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(java.time.Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "WireStreamHttp";
        }

        @Override
        public void close() {
            loop.shutdownNow();
        }
    }

    /** A response body emitting one chunk per unit of demand on the event loop, then completing the exchange. */
    static final class ChunkPublisher implements Publisher<ByteBuffer> {
        private final ExecutorService loop;
        private final List<ByteBuffer> chunks;
        private final Runnable onTerminal;

        ChunkPublisher(ExecutorService loop, List<ByteBuffer> chunks, Runnable onTerminal) {
            this.loop = loop;
            this.chunks = chunks;
            this.onTerminal = onTerminal;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private int delivered;
                private boolean done;

                @Override
                public void request(long n) {
                    loop.execute(() -> {
                        for (long i = 0; i < n && !done && delivered < chunks.size(); i++) {
                            subscriber.onNext(chunks.get(delivered++).duplicate());
                        }
                        if (!done && delivered == chunks.size()) {
                            done = true;
                            subscriber.onComplete();
                            onTerminal.run();
                        }
                    });
                }

                @Override
                public void cancel() {
                    loop.execute(() -> done = true);
                }
            });
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Event stream responses

    /** The response of a buffered success or an error reply. */
    private static SdkHttpResponse streamReplyResponse(StreamScenario scenario, String requestId) {
        Proto proto = scenario.proto();
        SdkHttpResponse.Builder response = SdkHttpResponse.builder().addHeader("x-amzn-RequestId", requestId)
                                                          .addHeader("Content-Type", proto.contentType);
        if (proto == Proto.RPCV2_CBOR) {
            response.addHeader("smithy-protocol", "rpc-v2-cbor");
        }
        return switch (scenario.reply()) {
            case StreamReply.Buffered buffered -> {
                Map<String, Object> body = new LinkedHashMap<>(buffered.output());
                Object version = body.remove("version");
                if (version != null) {
                    response.addHeader("X-Version", version.toString());
                }
                yield response.statusCode(200).body(HttpBody.fromBytes(payload(proto, body))).build();
            }
            case StreamReply.Modeled error -> {
                Map<String, Object> members = new LinkedHashMap<>();
                if (error.message() != null) {
                    members.put("message", error.message());
                }
                members.put("reason", error.reason());
                yield Responses.error(response.statusCode(400), proto, "Bad", members, requestId);
            }
            case StreamReply.Unmodeled error -> Responses.error(response.statusCode(error.status()), proto,
                                                                 error.code(),
                                                                 Map.of("message", "unmodeled " + error.code()),
                                                                 requestId);
            case StreamReply.Stream stream -> throw new IllegalStateException("not buffered");
        };
    }

    /** The headers of an event stream response; a REST output's header members are bound here. */
    private static SdkHttpResponse.Builder streamHeaders(Proto proto, Map<String, Object> output, String requestId) {
        SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(200)
                                                          .addHeader("x-amzn-RequestId", requestId)
                                                          .addHeader("Content-Type", EVENT_STREAM);
        if (proto == Proto.RPCV2_CBOR) {
            response.addHeader("smithy-protocol", "rpc-v2-cbor");
        }
        if (proto.rest()) {
            if (output.containsKey("session")) {
                response.addHeader("X-Session", output.get("session").toString());
            }
            if (output.containsKey("version")) {
                response.addHeader("X-Version", output.get("version").toString());
            }
        }
        return response.body(HttpBody.empty());
    }

    /** The frames of an event stream response: an RPC {@code initial-response}, the events and the end. */
    private static List<ByteBuffer> responseFrames(Proto proto, StreamReply.Stream stream) {
        List<ByteBuffer> frames = new ArrayList<>();
        if (!proto.rest()) {
            frames.add(frame("event", ":event-type", "initial-response", contentType(proto), new LinkedHashMap<>(),
                             payload(proto, stream.output())));
        }
        for (Event event : stream.events()) {
            frames.add(eventFrame(proto, event));
        }
        switch (stream.terminal()) {
            case Terminal.End end -> {
            }
            case Terminal.ExceptionEvent exception -> frames.add(
                frame("exception", ":exception-type", "failure", contentType(proto), new LinkedHashMap<>(),
                      payload(proto, exception.members())));
            case Terminal.ErrorFrame error -> {
                Map<String, HeaderValue> headers = new LinkedHashMap<>();
                headers.put(":message-type", HeaderValue.ofString("error"));
                headers.put(":error-code", HeaderValue.ofString(error.code()));
                headers.put(":error-message", HeaderValue.ofString(error.message()));
                frames.add(MessageEncoder.encode(new Message(headers, new byte[0])));
            }
        }
        return frames;
    }

    /** One event frame: header members as typed headers, the payload member or the other members as the payload. */
    private static ByteBuffer eventFrame(Proto proto, Event event) {
        Map<String, Object> values = new LinkedHashMap<>(event.values());
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        return switch (event.member()) {
            case "chunk" -> {
                Object kind = values.get("kind");
                if (kind != null) {
                    headers.put("kind", HeaderValue.ofString((String) kind));
                }
                byte[] data = values.containsKey("data") ? (byte[]) values.get("data") : new byte[0];
                yield frame("event", ":event-type", "chunk", "application/octet-stream", headers, data);
            }
            case "tagged" -> {
                if (values.containsKey("id")) {
                    headers.put("id", HeaderValue.ofString((String) values.get("id")));
                }
                if (values.containsKey("seq")) {
                    headers.put("seq", HeaderValue.ofInteger((Integer) values.get("seq")));
                }
                if (values.containsKey("flag")) {
                    headers.put("flag", HeaderValue.ofBoolean((Boolean) values.get("flag")));
                }
                if (values.containsKey("at")) {
                    headers.put("at", HeaderValue.ofTimestamp((Instant) values.get("at")));
                }
                if (values.containsKey("big")) {
                    headers.put("big", HeaderValue.ofLong((Long) values.get("big")));
                }
                Object inner = values.get("inner");
                yield frame("event", ":event-type", "tagged", contentType(proto), headers,
                            inner == null ? new byte[0] : payload(proto, castMap(inner)));
            }
            case "mixed" -> {
                Object tag = values.remove("tag");
                if (tag != null) {
                    headers.put("tag", HeaderValue.ofString((String) tag));
                }
                yield frame("event", ":event-type", "mixed", contentType(proto), headers, payload(proto, values));
            }
            default -> frame("event", ":event-type", event.member(), contentType(proto), headers,
                             payload(proto, values));
        };
    }

    private static ByteBuffer frame(String messageType, String typeHeader, String type, String contentType,
                                    Map<String, HeaderValue> members, byte[] payload) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":message-type", HeaderValue.ofString(messageType));
        headers.put(typeHeader, HeaderValue.ofString(type));
        headers.put(":content-type", HeaderValue.ofString(contentType));
        headers.putAll(members);
        return MessageEncoder.encode(new Message(headers, payload));
    }

    private static String contentType(Proto proto) {
        return proto == Proto.RPCV2_CBOR ? "application/cbor" : "application/json";
    }

    private static byte[] payload(Proto proto, Map<String, Object> members) {
        return proto == Proto.RPCV2_CBOR ? Cbor.encode(members)
                                         : ClientProjectionLifecyclePropertyTest.json(members)
                                                                                .getBytes(StandardCharsets.UTF_8);
    }
}
