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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.pipeline.PipelineFixtures.ENDPOINT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.SERVICE;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.core.SdkResponseMetadata;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.internal.exception.SdkExceptionCallMetadata;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.ModeledProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.awssdk.protocols.core.SelectedProtocol;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.awssdk.protocols.core.error.ErrorCodes;
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.smithy.runtime.registry.RegisteredType;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;

/**
 * Property 66: Response metadata preserves supplied presence (Requirements 21.1, 21.13).
 *
 * <p>For any subset of request ID, extended request ID, HTTP status, service error code, attempt count and
 * transport metadata supplied by a response (and the call), the modeled response or SDK exception exposes exactly
 * the supplied values, and the documented absent value for each other field, without a dereference failure. The
 * oracle of every check is a {@link Presence} map built from the generated input alone; it never consults the
 * production code. The documented absent values it encodes:
 * <ul>
 *   <li>V3 metadata ({@link SdkResponseMetadata}): an empty {@code Optional}/{@code OptionalInt}, and an empty
 *       transport map;</li>
 *   <li>the V2-compatible response view ({@link AwsResponse#responseMetadata()}): {@code "UNKNOWN"};</li>
 *   <li>the V2-compatible exception accessors: {@code null} request IDs, error code and attempt count, and status
 *       code {@code 0}.</li>
 * </ul>
 *
 * <p>Two properties share the oracle:
 * <ul>
 *   <li>{@link #responseMetadataPreservesSuppliedPresenceEndToEnd} runs one call per try through the real
 *       {@link AwsCallEngine} (sync or async driver), whose scripted transport returns responses whose headers carry
 *       or omit each request-ID header (including S3's {@code x-amz-request-id} and {@code x-amz-id-2}), success
 *       and modeled or unmodeled error responses decoded through the shared {@link ModeledErrors} the protocol
 *       runtimes use, transport failures, and failures before the first attempt, after 0-3 retried attempts whose
 *       stale metadata must not leak into the result. Every subset the wire can express is reachable this way:
 *       a call always has transport metadata (an HTTP Implementation name is required), and a status, request IDs
 *       and an error code only arrive together with a response.</li>
 *   <li>{@link #everyFieldSubsetIsPreservedByTheMetadataTypes} checks all 64 field subsets in every try, through the
 *       public builders of the metadata, response and exception types and the pipeline's call-metadata recorder,
 *       including the subsets no response can supply (for example a request ID without a status, or no transport
 *       metadata).</li>
 * </ul>
 */
class ResponseMetadataPresencePropertyTest {
    /** The {@link SdkResponseMetadata#HTTP_CLIENT} transport-metadata key, as documented. */
    private static final String HTTP_CLIENT_KEY = "httpClient";
    /** The V2-compatible absent value, as documented. */
    private static final String UNKNOWN = "UNKNOWN";
    /** The extended request ID header (S3's {@code x-amz-id-2}). */
    private static final String EXTENDED_REQUEST_ID_HEADER = "x-amz-id-2";
    private static final List<String> REQUEST_ID_HEADERS =
        List.of("x-amzn-RequestId", "x-amz-request-id", "x-amzn-request-id", "X-Amzn-RequestId", "X-AMZ-REQUEST-ID");
    private static final List<String> EXTENDED_REQUEST_ID_HEADERS = List.of("x-amz-id-2", "X-Amz-Id-2");

    private static final Schema THING_NOT_FOUND = Schema.structureBuilder(ShapeId.from("com.example#ThingNotFound"),
                                                                          new ErrorTrait("client")).build();
    private static final AwsOperation<Struct, PresenceResponse> GET_PRESENCE = operation();

    /** A response-metadata field. */
    enum Field {
        REQUEST_ID, EXTENDED_REQUEST_ID, STATUS_CODE, ERROR_CODE, ATTEMPT_COUNT, TRANSPORT
    }

    /**
     * The oracle: the value of each supplied field (a String, an Integer, or the transport map); a field without an
     * entry was not supplied.
     */
    record Presence(Map<Field, Object> values) {
        Presence {
            values = Map.copyOf(values);
        }

        Set<Field> supplied() {
            return values.isEmpty() ? EnumSet.noneOf(Field.class) : EnumSet.copyOf(values.keySet());
        }

        Optional<String> string(Field field) {
            return Optional.ofNullable((String) values.get(field));
        }

        OptionalInt integer(Field field) {
            Integer value = (Integer) values.get(field);
            return value == null ? OptionalInt.empty() : OptionalInt.of(value);
        }

        @SuppressWarnings("unchecked")
        Map<String, String> transport() {
            return (Map<String, String>) values.getOrDefault(Field.TRANSPORT, Map.of());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // End to end through the AWS pipeline
    // ---------------------------------------------------------------------------------------------------------------

    /** How the call ends. */
    enum Terminal {
        SUCCESS, MODELED_ERROR, UNMODELED_ERROR, TRANSPORT_FAILURE, BEFORE_FIRST_ATTEMPT;

        /** Whether the final attempt receives an HTTP response. */
        boolean responds() {
            return this == SUCCESS || this == MODELED_ERROR || this == UNMODELED_ERROR;
        }

        boolean serviceError() {
            return this == MODELED_ERROR || this == UNMODELED_ERROR;
        }
    }

    /** A retried attempt before the final one. */
    enum Prior {
        /** HTTP 503 carrying stale request IDs and an error code of its own. */
        SERVICE_UNAVAILABLE,
        /** A connection failure: no response. */
        CONNECTION_RESET
    }

    /** One response header. */
    record Header(String name, String value) {
    }

    /**
     * A wire error type and the error code callers see for it.
     *
     * @param raw the {@code X-Amzn-Errortype} header value
     * @param reported the shape name it denotes: without a namespace or a {@code :}-separated suffix
     */
    record WireCode(String raw, String reported) {
    }

    /**
     * One generated call.
     *
     * @param priors the retried attempts before the final one (none for {@link Terminal#BEFORE_FIRST_ATTEMPT})
     * @param status the final response's status code; 0 if it has none
     * @param requestId the final response's request-ID header, or null
     * @param extendedRequestId the final response's extended-request-ID header, or null
     * @param errorCode the final error response's error type, or null
     * @param spareAttempts attempts the retry strategy allows beyond those made (only for a non-retryable end)
     */
    record Scenario(Driver driver, String clientName, List<Prior> priors, Terminal terminal, int status,
                    Header requestId, Header extendedRequestId, WireCode errorCode, int spareAttempts) {

        /** Drops what the terminal cannot carry, and spare attempts after a retryable final failure. */
        static Scenario of(Driver driver, String clientName, List<Prior> priors, Terminal terminal, int status,
                           Header requestId, Header extendedRequestId, WireCode errorCode, int spareAttempts) {
            boolean responds = terminal.responds();
            boolean retryableEnd = terminal == Terminal.TRANSPORT_FAILURE || terminal.serviceError() && status >= 500;
            return new Scenario(driver, clientName,
                                terminal == Terminal.BEFORE_FIRST_ATTEMPT ? List.of() : priors, terminal,
                                responds ? status : 0, responds ? requestId : null,
                                responds ? extendedRequestId : null, terminal.serviceError() ? errorCode : null,
                                retryableEnd ? 0 : spareAttempts);
        }

        /** The attempts the call makes. */
        int attempts() {
            return terminal == Terminal.BEFORE_FIRST_ATTEMPT ? 0 : priors.size() + 1;
        }

        int maxAttempts() {
            return Math.max(1, attempts()) + spareAttempts;
        }

        /** The presence map: what the final response and the call supply. */
        Presence expected() {
            Map<Field, Object> values = new EnumMap<>(Field.class);
            values.put(Field.TRANSPORT, Map.of(HTTP_CLIENT_KEY, clientName));
            if (attempts() > 0) {
                values.put(Field.ATTEMPT_COUNT, attempts());
            }
            if (terminal.responds()) {
                values.put(Field.STATUS_CODE, status);
                if (requestId != null) {
                    values.put(Field.REQUEST_ID, requestId.value());
                }
                if (extendedRequestId != null) {
                    values.put(Field.EXTENDED_REQUEST_ID, extendedRequestId.value());
                }
                if (errorCode != null) {
                    values.put(Field.ERROR_CODE, errorCode.reported());
                }
            }
            return new Presence(values);
        }
    }

    // Feature: aws-sdk-java-v3, Property 66: Response metadata preserves supplied presence
    // Embedded edge cases are disabled: they are mostly the first value of each generator. The statistics coverage
    // checks below fail the property if any ending, retries, or the presence or absence of any field the wire can
    // omit stop being generated.
    @Property(tries = 500, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 66: Response metadata preserves supplied presence (end to end)")
    void responseMetadataPreservesSuppliedPresenceEndToEnd(@ForAll("scenarios") Scenario scenario) {
        Presence expected = scenario.expected();
        Call call = new Call(scenario);
        Object result = call.run();
        String description = scenario + "\n  expected: " + expected + "\n  result: " + result;

        // The scenario ran as generated: every scripted outcome was consumed, one per attempt.
        assertThat(call.transmissions).as("transmissions of " + description).isEqualTo(scenario.attempts());
        assertThat(call.outcomes).as("unused outcomes of " + description).isEmpty();
        switch (scenario.terminal()) {
            case SUCCESS -> {
                assertThat(result).as(description).isInstanceOf(PresenceResponse.class);
                PresenceResponse output = (PresenceResponse) result;
                assertThat(output.value).as(description).isEqualTo("value");
                assertMetadata(output.sdkResponseMetadata(), expected, description);
                assertResponseViews(output, expected, true, description);
            }
            case MODELED_ERROR -> {
                assertThat(result).as(description).isInstanceOf(ThingNotFoundException.class);
                assertException((SdkException) result, expected, description);
            }
            case UNMODELED_ERROR -> {
                assertThat(result).as(description).isExactlyInstanceOf(AwsServiceException.class);
                assertException((SdkException) result, expected, description);
            }
            case TRANSPORT_FAILURE -> {
                assertThat(result).as(description).isInstanceOf(HttpTransportException.class);
                assertException((SdkException) result, expected, description);
            }
            case BEFORE_FIRST_ATTEMPT -> {
                assertThat(result).as(description).isInstanceOf(SdkException.class)
                                  .isNotInstanceOf(SdkServiceException.class);
                assertException((SdkException) result, expected, description);
            }
            default -> throw new IllegalStateException(scenario.terminal().toString());
        }

        Set<Field> supplied = expected.supplied();
        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("driver").coverage(c -> {
            c.check(Driver.SYNC).percentage(p -> p >= 30);
            c.check(Driver.ASYNC).percentage(p -> p >= 30);
        });
        Statistics.label("terminal").collect(scenario.terminal());
        Statistics.label("terminal").coverage(c -> {
            for (Terminal terminal : Terminal.values()) {
                c.check(terminal).percentage(p -> p >= 5);
            }
        });
        Statistics.label("retried").collect(!scenario.priors().isEmpty());
        Statistics.label("retried").coverage(c -> c.check(true).percentage(p -> p >= 25));
        Statistics.label("a stale earlier response").collect(scenario.priors().contains(Prior.SERVICE_UNAVAILABLE));
        Statistics.label("a stale earlier response").coverage(c -> c.check(true).percentage(p -> p >= 15));
        Statistics.label("spare attempts").collect(scenario.spareAttempts() > 0);
        Statistics.label("spare attempts").coverage(c -> c.check(true).percentage(p -> p >= 15));
        // TRANSPORT is always supplied end to end; its absence is covered by the builder property.
        for (Field field : EnumSet.complementOf(EnumSet.of(Field.TRANSPORT))) {
            String label = field + " supplied";
            Statistics.label(label).collect(supplied.contains(field));
            Statistics.label(label).coverage(c -> {
                c.check(true).percentage(p -> p >= 5);
                c.check(false).percentage(p -> p >= 5);
            });
        }
        Statistics.label("supplied subset").collect(supplied.toString());
        Statistics.label("supplied subset").coverage(c -> {
            for (Set<Field> reachable : wireSubsets()) {
                c.check(reachable.toString()).percentage(p -> p >= 1);
            }
        });
    }

    /**
     * The 10 subsets a call can supply: transport metadata always; the attempt count once an attempt started; and a
     * status, with any combination of request ID, extended request ID and (for an error) error code, only together
     * with a response.
     */
    private static List<Set<Field>> wireSubsets() {
        List<Set<Field>> subsets = new ArrayList<>();
        subsets.add(EnumSet.of(Field.TRANSPORT));
        subsets.add(EnumSet.of(Field.ATTEMPT_COUNT, Field.TRANSPORT));
        List<Field> optional = List.of(Field.REQUEST_ID, Field.EXTENDED_REQUEST_ID, Field.ERROR_CODE);
        for (int mask = 0; mask < 1 << optional.size(); mask++) {
            Set<Field> subset = EnumSet.of(Field.STATUS_CODE, Field.ATTEMPT_COUNT, Field.TRANSPORT);
            for (int i = 0; i < optional.size(); i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(optional.get(i));
                }
            }
            subsets.add(subset);
        }
        return subsets;
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Terminal> terminals = Arbitraries.frequency(
            Tuple.of(3, Terminal.SUCCESS), Tuple.of(2, Terminal.MODELED_ERROR), Tuple.of(2, Terminal.UNMODELED_ERROR),
            Tuple.of(1, Terminal.TRANSPORT_FAILURE), Tuple.of(1, Terminal.BEFORE_FIRST_ATTEMPT));
        return terminals.flatMap(terminal -> Combinators.combine(
            PipelineArbitraries.drivers(),
            tokens().map(name -> "Http" + name),
            Arbitraries.of(Prior.values()).list().ofMaxSize(3),
            terminal == Terminal.SUCCESS ? Arbitraries.of(200, 201, 202) : Arbitraries.of(400, 403, 404, 409, 500, 503),
            headers(REQUEST_ID_HEADERS, "req-"),
            headers(EXTENDED_REQUEST_ID_HEADERS, "ext-"),
            terminal == Terminal.MODELED_ERROR ? modeledCodes() : unmodeledCodes(),
            Arbitraries.integers().between(0, 2))
            .as((driver, clientName, priors, status, requestId, extendedRequestId, errorCode, spare) ->
                    Scenario.of(driver, clientName, priors, terminal, status, requestId, extendedRequestId, errorCode,
                                spare)));
    }

    private static Arbitrary<String> tokens() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(16);
    }

    /** One of {@code names} with a fresh value distinct from every stale value, or (35%) none. */
    private static Arbitrary<Header> headers(List<String> names, String prefix) {
        return Combinators.combine(Arbitraries.of(names), tokens().map(token -> prefix + token))
                          .as(Header::new)
                          .injectNull(0.35);
    }

    private static Arbitrary<WireCode> modeledCodes() {
        return Arbitraries.of(new WireCode("ThingNotFound", "ThingNotFound"),
                              new WireCode("com.example#ThingNotFound", "ThingNotFound"),
                              new WireCode("ThingNotFound:http://internal.amazon.com/coral/com.example/",
                                           "ThingNotFound"));
    }

    /** An error type no operation or registry declares, in each wire form, or (30%) none. */
    private static Arbitrary<WireCode> unmodeledCodes() {
        Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(12)
                                             .map(name -> "Unmodeled" + name);
        return Combinators.combine(names, Arbitraries.integers().between(0, 2))
                          .as((name, form) -> switch (form) {
                              case 0 -> new WireCode(name, name);
                              case 1 -> new WireCode("aws.example#" + name, name);
                              default -> new WireCode(name + ":http://internal.example.com/", name);
                          })
                          .injectNull(0.3);
    }

    /** One call of a scenario through a fresh engine, its scripted transport, and what the transport observed. */
    private static final class Call implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final Deque<Object> outcomes = new ArrayDeque<>();
        int transmissions;
        private final Scenario scenario;

        Call(Scenario scenario) {
            this.scenario = scenario;
            for (int i = 0; i < scenario.priors().size(); i++) {
                outcomes.add(switch (scenario.priors().get(i)) {
                    case SERVICE_UNAVAILABLE -> SdkHttpResponse.builder()
                                                               .statusCode(503)
                                                               .addHeader("x-amzn-RequestId", "stale-req-" + i)
                                                               .addHeader("x-amz-request-id", "stale-s3-" + i)
                                                               .addHeader(EXTENDED_REQUEST_ID_HEADER, "stale-ext-" + i)
                                                               .addHeader(ErrorCodes.ERROR_TYPE_HEADER, "StaleError")
                                                               .body(HttpBody.fromUtf8("stale"))
                                                               .build();
                    case CONNECTION_RESET -> connectionReset();
                });
            }
            switch (scenario.terminal()) {
                case SUCCESS, MODELED_ERROR, UNMODELED_ERROR -> outcomes.add(finalResponse());
                case TRANSPORT_FAILURE -> outcomes.add(connectionReset());
                case BEFORE_FIRST_ATTEMPT -> {
                }
                default -> throw new IllegalStateException(scenario.terminal().toString());
            }
        }

        private SdkHttpResponse finalResponse() {
            SdkHttpResponse.Builder response = SdkHttpResponse.builder()
                                                              .statusCode(scenario.status())
                                                              // Not a request ID: must never be reported as one.
                                                              .addHeader("X-Amz-Cf-Id", "cloudfront-id")
                                                              .body(HttpBody.fromUtf8("value"));
            if (scenario.requestId() != null) {
                response.addHeader(scenario.requestId().name(), scenario.requestId().value());
            }
            if (scenario.extendedRequestId() != null) {
                response.addHeader(scenario.extendedRequestId().name(), scenario.extendedRequestId().value());
            }
            if (scenario.errorCode() != null) {
                response.addHeader(ErrorCodes.ERROR_TYPE_HEADER, scenario.errorCode().raw());
            }
            return response.build();
        }

        private static HttpTransportException connectionReset() {
            return HttpTransportException.create(HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "reset", null);
        }

        /** Runs the call; its output, or the failure it surfaced. */
        Object run() {
            boolean async = scenario.driver() == Driver.ASYNC;
            ClientRuntime.Builder runtime =
                ClientRuntime.builder()
                             .configuration(SdkClientConfiguration.builder()
                                                                  .option(SdkClientOption.CLIENT_ENDPOINT, ENDPOINT)
                                                                  .build())
                             .protocol(new PresenceProtocol())
                             .transportMode(scenario.driver().mode)
                             .retryStrategy(retries(scenario.maxAttempts()), "things");
            if (async) {
                runtime.completionExecutor(Runnable::run)
                       .retryScheduler(delay -> CompletableFuture.completedFuture(null));
            }
            if (scenario.terminal() == Terminal.BEFORE_FIRST_ATTEMPT) {
                runtime.addInterceptor(new ExecutionInterceptor() {
                    @Override
                    public void beforeExecution(HookContext.BeforeExecution context) {
                        throw new IllegalStateException("rejected before the first attempt");
                    }
                });
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build());
            AwsCallEngine engine = (async ? builder.asyncHttpClient(this) : builder.syncHttpClient(this)).build();
            PipelinePlan<Struct, PresenceResponse> plan = engine.runtime().plan(GET_PRESENCE);
            Struct request = PipelineFixtures.request(GET_INPUT, "a");
            try {
                if (!async) {
                    return engine.executeSync(plan, request, null, null);
                }
                return engine.executeAsync(plan, request, null, null).get(5, TimeUnit.SECONDS);
            } catch (ExecutionException failure) {
                return failure.getCause();
            } catch (Exception failure) {
                return failure;
            }
        }

        private static RetryStrategy retries(int maxAttempts) {
            return StandardRetryStrategy.builder()
                                        .maxAttempts(maxAttempts)
                                        .backoffStrategy(BackoffStrategy.none())
                                        .throttlingBackoffStrategy(BackoffStrategy.none())
                                        .build();
        }

        private Object next() {
            transmissions++;
            return outcomes.remove();
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    Object outcome = next();
                    if (outcome instanceof RuntimeException failure) {
                        throw failure;
                    }
                    return (SdkHttpResponse) outcome;
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
            Object outcome = next();
            AsyncExchangeCompletion exchange =
                new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
            if (outcome instanceof RuntimeException failure) {
                exchange.fail(failure);
                handler.onError(failure);
                return exchange;
            }
            SdkHttpResponse response = (SdkHttpResponse) outcome;
            handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
            handler.onStream(bodyOf(response.body().asUtf8String().getBytes(StandardCharsets.UTF_8)));
            exchange.complete();
            return exchange;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return scenario.clientName();
        }

        @Override
        public void close() {
        }
    }

    /** A response body publisher of {@code bytes}, delivered on the first request. */
    private static Publisher<ByteBuffer> bodyOf(byte[] bytes) {
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

    // ---------------------------------------------------------------------------------------------------------------
    // Every field subset through the metadata, response and exception types
    // ---------------------------------------------------------------------------------------------------------------

    /** The exception types, and the fields each can carry. */
    enum ExceptionKind {
        SDK(EnumSet.of(Field.ATTEMPT_COUNT, Field.TRANSPORT)),
        SERVICE(EnumSet.complementOf(EnumSet.of(Field.ERROR_CODE))),
        AWS_SERVICE(EnumSet.allOf(Field.class));

        final Set<Field> expressible;

        ExceptionKind(Set<Field> expressible) {
            this.expressible = expressible;
        }
    }

    /**
     * The value each field takes when it is supplied.
     *
     * @param detailsWithoutCode for an AWS service exception without an error code: whether it has error details
     *     (with no code) rather than none
     * @param attemptsFromCall whether a supplied attempt count is recorded by the call rather than the builder
     */
    record Values(String requestId, String extendedRequestId, int status, String errorCode, int attempts,
                  Map<String, String> transport, boolean detailsWithoutCode, boolean attemptsFromCall) {

        Presence presence(Set<Field> supplied) {
            Map<Field, Object> values = new EnumMap<>(Field.class);
            for (Field field : supplied) {
                values.put(field, switch (field) {
                    case REQUEST_ID -> requestId;
                    case EXTENDED_REQUEST_ID -> extendedRequestId;
                    case STATUS_CODE -> status;
                    case ERROR_CODE -> errorCode;
                    case ATTEMPT_COUNT -> attempts;
                    case TRANSPORT -> transport;
                });
            }
            return new Presence(values);
        }
    }

    // Feature: aws-sdk-java-v3, Property 66: Response metadata preserves supplied presence
    // Every try checks all 64 field subsets with its generated values, so each subset is covered in every run.
    @Property(tries = 200, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 66: Response metadata preserves supplied presence (every field subset)")
    void everyFieldSubsetIsPreservedByTheMetadataTypes(@ForAll("values") Values values) {
        int subsets = 1 << Field.values().length;
        for (int mask = 0; mask < subsets; mask++) {
            Set<Field> supplied = EnumSet.noneOf(Field.class);
            for (Field field : Field.values()) {
                if ((mask & (1 << field.ordinal())) != 0) {
                    supplied.add(field);
                }
            }
            Presence expected = values.presence(supplied);
            String description = "supplied " + supplied + " of " + values;

            SdkResponseMetadata metadata = metadata(expected);
            assertMetadata(metadata, expected, "metadata of " + description);
            assertThat(metadata.transportMetadata()).as("transport order of " + description)
                                                    .containsExactlyEntriesOf(expected.transport());
            assertThat(metadata.toBuilder().build()).as(description).isEqualTo(metadata);
            if (supplied.isEmpty()) {
                assertThat(metadata).as(description).isSameAs(SdkResponseMetadata.empty());
            }

            PresenceResponse response = response(metadata, expected);
            assertThat(response.sdkResponseMetadata()).as(description).isSameAs(metadata);
            assertResponseViews(response, expected, supplied.contains(Field.STATUS_CODE), "response of " + description);

            for (ExceptionKind kind : ExceptionKind.values()) {
                Set<Field> expressible = EnumSet.copyOf(supplied);
                expressible.retainAll(kind.expressible);
                Presence carried = values.presence(expressible);
                assertException(exception(kind, carried, values), carried, kind + " of " + description);
            }
        }

        Statistics.label("details without a code").collect(values.detailsWithoutCode());
        Statistics.label("details without a code").coverage(c -> {
            c.check(true).percentage(p -> p >= 20);
            c.check(false).percentage(p -> p >= 20);
        });
        Statistics.label("attempts recorded by the call").collect(values.attemptsFromCall());
        Statistics.label("attempts recorded by the call").coverage(c -> {
            c.check(true).percentage(p -> p >= 20);
            c.check(false).percentage(p -> p >= 20);
        });
        Statistics.label("transport facts").collect(values.transport().size());
    }

    @Provide
    Arbitrary<Values> values() {
        // Insertion-ordered, so the builder's documented insertion order can be checked.
        Arbitrary<Map<String, String>> transport =
            Arbitraries.maps(Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8), tokens())
                       .ofMinSize(1).ofMaxSize(3)
                       .map(facts -> new LinkedHashMap<>(facts));
        return Combinators.combine(tokens().map(t -> "req-" + t), tokens().map(t -> "ext-" + t),
                                   Arbitraries.integers().between(100, 599), tokens().map(t -> "Code" + t),
                                   Arbitraries.integers().between(1, 10), transport, Arbitraries.of(true, false),
                                   Arbitraries.of(true, false))
                          .as(Values::new);
    }

    /** The metadata of exactly the supplied fields, through the public builder. */
    private static SdkResponseMetadata metadata(Presence presence) {
        SdkResponseMetadata.Builder builder = SdkResponseMetadata.builder();
        presence.string(Field.REQUEST_ID).ifPresent(builder::requestId);
        presence.string(Field.EXTENDED_REQUEST_ID).ifPresent(builder::extendedRequestId);
        presence.integer(Field.STATUS_CODE).ifPresent(builder::statusCode);
        presence.string(Field.ERROR_CODE).ifPresent(builder::errorCode);
        presence.integer(Field.ATTEMPT_COUNT).ifPresent(builder::attemptCount);
        if (presence.supplied().contains(Field.TRANSPORT)) {
            builder.transportMetadata(presence.transport());
        }
        return builder.build();
    }

    /**
     * A built response carrying {@code metadata} and, when a status is supplied (a response was received), the HTTP
     * response whose headers carry the supplied request IDs.
     */
    private static PresenceResponse response(SdkResponseMetadata metadata, Presence presence) {
        PresenceResponse.PresenceBuilder builder = PresenceResponse.builder();
        builder.sdkResponseMetadata(metadata);
        presence.integer(Field.STATUS_CODE).ifPresent(status -> {
            SdkHttpResponse.Builder http = SdkHttpResponse.builder().statusCode(status);
            presence.string(Field.REQUEST_ID).ifPresent(id -> http.addHeader("x-amzn-RequestId", id));
            presence.string(Field.EXTENDED_REQUEST_ID).ifPresent(id -> http.addHeader(EXTENDED_REQUEST_ID_HEADER, id));
            builder.sdkHttpResponse(http.build());
        });
        return builder.build();
    }

    /** An exception of {@code kind} carrying exactly the supplied fields, as a call would surface it. */
    private static SdkException exception(ExceptionKind kind, Presence presence, Values values) {
        Integer builderAttempts = values.attemptsFromCall() ? null : boxed(presence.integer(Field.ATTEMPT_COUNT));
        SdkException exception = switch (kind) {
            case SDK -> SdkException.builder().message("failed").numAttempts(builderAttempts).build();
            case SERVICE -> SdkServiceException.builder()
                                               .message("failed")
                                               .numAttempts(builderAttempts)
                                               .requestId(presence.string(Field.REQUEST_ID).orElse(null))
                                               .extendedRequestId(presence.string(Field.EXTENDED_REQUEST_ID)
                                                                          .orElse(null))
                                               .statusCode(presence.integer(Field.STATUS_CODE).orElse(0))
                                               .build();
            case AWS_SERVICE -> {
                Optional<String> code = presence.string(Field.ERROR_CODE);
                AwsErrorDetails details = code.isPresent() || values.detailsWithoutCode()
                                          ? AwsErrorDetails.builder().errorCode(code.orElse(null)).build()
                                          : null;
                yield AwsServiceException.builder()
                                         .message("failed")
                                         .numAttempts(builderAttempts)
                                         .requestId(presence.string(Field.REQUEST_ID).orElse(null))
                                         .extendedRequestId(presence.string(Field.EXTENDED_REQUEST_ID).orElse(null))
                                         .statusCode(presence.integer(Field.STATUS_CODE).orElse(0))
                                         .awsErrorDetails(details)
                                         .build();
            }
        };
        int callAttempts = values.attemptsFromCall() ? presence.integer(Field.ATTEMPT_COUNT).orElse(0) : 0;
        if (callAttempts > 0 || presence.supplied().contains(Field.TRANSPORT)) {
            // What the pipeline records on the exception a call surfaces (0 attempts records no count).
            SdkExceptionCallMetadata.record(exception, callAttempts, Map.copyOf(presence.transport()));
        }
        return exception;
    }

    private static Integer boxed(OptionalInt value) {
        return value.isPresent() ? value.getAsInt() : null;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Shared assertions: accessors against the presence map
    // ---------------------------------------------------------------------------------------------------------------

    /** Each V3 metadata accessor reports the supplied value, or the documented empty value. */
    private static void assertMetadata(SdkResponseMetadata metadata, Presence expected, String description) {
        assertThat(metadata).as(description).isNotNull();
        assertThat(metadata.requestId()).as("requestId of " + description)
                                        .isEqualTo(expected.string(Field.REQUEST_ID));
        assertThat(metadata.extendedRequestId()).as("extendedRequestId of " + description)
                                                .isEqualTo(expected.string(Field.EXTENDED_REQUEST_ID));
        assertThat(metadata.statusCode()).as("statusCode of " + description)
                                         .isEqualTo(expected.integer(Field.STATUS_CODE));
        assertThat(metadata.errorCode()).as("errorCode of " + description)
                                        .isEqualTo(expected.string(Field.ERROR_CODE));
        assertThat(metadata.attemptCount()).as("attemptCount of " + description)
                                           .isEqualTo(expected.integer(Field.ATTEMPT_COUNT));
        assertThat(metadata.transportMetadata()).as("transportMetadata of " + description)
                                                .isNotNull()
                                                .isEqualTo(expected.transport());
        assertThat(metadata.toString()).as(description).isNotNull();
    }

    /**
     * The V2-compatible views of a response: its HTTP response (present iff one was received) and its
     * {@code "UNKNOWN"}-for-absent metadata view, whose extended request ID is read from the received headers.
     */
    private static void assertResponseViews(PresenceResponse response, Presence expected, boolean received,
                                            String description) {
        AwsResponseMetadata view = response.responseMetadata();
        assertThat(view).as(description).isNotNull();
        assertThat(view.requestId()).as("V2 requestId of " + description)
                                    .isEqualTo(expected.string(Field.REQUEST_ID).orElse(UNKNOWN));
        assertThat(new V2View(view).value(EXTENDED_REQUEST_ID_HEADER))
            .as("V2 extended request ID of " + description)
            .isEqualTo(received ? expected.string(Field.EXTENDED_REQUEST_ID).orElse(UNKNOWN) : UNKNOWN);
        assertThat(view.toString()).as(description).isNotNull();
        if (received) {
            assertThat(response.sdkHttpResponse()).as(description).isNotNull();
            assertThat(response.sdkHttpResponse().statusCode()).as(description)
                                                               .isEqualTo(expected.integer(Field.STATUS_CODE)
                                                                                  .getAsInt());
            assertThat(response.sdkHttpResponse().body().kind()).as(description).isEqualTo(HttpBody.Kind.EMPTY);
        } else {
            assertThat(response.sdkHttpResponse()).as(description).isNull();
        }
    }

    /** The exception's metadata and its V2-compatible accessors, whose absent values are null and status 0. */
    private static void assertException(SdkException exception, Presence expected, String description) {
        assertMetadata(exception.sdkResponseMetadata(), expected, description);
        OptionalInt attempts = expected.integer(Field.ATTEMPT_COUNT);
        assertThat(exception.numAttempts()).as("numAttempts of " + description).isEqualTo(boxed(attempts));
        // Rendering never dereferences an absent value. Only the AWS service exception's documented V2 message format
        // includes the attempt count.
        String message = exception.getMessage();
        assertThat(message).as(description).isNotNull();
        if (attempts.isPresent() && exception instanceof AwsServiceException) {
            assertThat(message).as(description).contains("(SDK Attempt Count: " + attempts.getAsInt() + ")");
        }
        assertThat(exception.toString()).as(description).isNotNull();
        if (exception instanceof SdkServiceException service) {
            assertThat(service.requestId()).as("V2 requestId of " + description)
                                           .isEqualTo(expected.string(Field.REQUEST_ID).orElse(null));
            assertThat(service.extendedRequestId()).as("V2 extendedRequestId of " + description)
                                                   .isEqualTo(expected.string(Field.EXTENDED_REQUEST_ID).orElse(null));
            assertThat(service.statusCode()).as("V2 statusCode of " + description)
                                            .isEqualTo(expected.integer(Field.STATUS_CODE).orElse(0));
        } else {
            assertThat(expected.supplied()).as(description)
                                           .doesNotContain(Field.REQUEST_ID, Field.EXTENDED_REQUEST_ID,
                                                           Field.STATUS_CODE, Field.ERROR_CODE);
        }
        if (exception instanceof AwsServiceException aws) {
            AwsErrorDetails details = aws.awsErrorDetails();
            String code = details == null ? null : details.errorCode();
            assertThat(code).as("V2 errorCode of " + description)
                            .isEqualTo(expected.string(Field.ERROR_CODE).orElse(null));
            if (details != null && details.sdkHttpResponse() != null) {
                // A decoded error keeps the received status code and headers, without the body.
                assertThat(details.sdkHttpResponse().statusCode()).as(description)
                                                                  .isEqualTo(expected.integer(Field.STATUS_CODE)
                                                                                     .getAsInt());
                assertThat(details.sdkHttpResponse().headers().firstValue(EXTENDED_REQUEST_ID_HEADER))
                    .as(description).isEqualTo(expected.string(Field.EXTENDED_REQUEST_ID));
                assertThat(details.sdkHttpResponse().body().kind()).as(description).isEqualTo(HttpBody.Kind.EMPTY);
            }
        } else if (exception instanceof SdkServiceException) {
            assertThat(expected.supplied()).as(description).doesNotContain(Field.ERROR_CODE);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Fixtures: a generated-style output and modeled error, and a header-based protocol over ModeledErrors
    // ---------------------------------------------------------------------------------------------------------------

    private static AwsOperation<Struct, PresenceResponse> operation() {
        Schema operation = Schema.createOperation(ShapeId.from("com.example#GetPresence"));
        OperationSchemas schemas = OperationSchemas.builder(operation)
                                                   .input(GET_INPUT)
                                                   .output(GET_OUTPUT)
                                                   .errors(THING_NOT_FOUND)
                                                   .build();
        AwsOperationDescriptor descriptor = AwsOperationDescriptor.builder()
                                                                  .id(operation.id())
                                                                  .service(SERVICE)
                                                                  .input(GET_INPUT.id())
                                                                  .output(GET_OUTPUT.id())
                                                                  .errors(THING_NOT_FOUND.id())
                                                                  .schemas(schemas)
                                                                  .build();
        return AwsOperation.builder(descriptor, InputBuilder::new, PresenceResponse::builder).build();
    }

    /** The V2 view's header lookup, which generated service subclasses use. */
    private static final class V2View extends AwsResponseMetadata {
        V2View(AwsResponseMetadata metadata) {
            super(metadata);
        }

        String value(String key) {
            return getValue(key);
        }
    }

    /**
     * An {@code awsJson1_0}-style runtime: the body is the output's {@code Value}; an error's type is read from
     * {@value ErrorCodes#ERROR_TYPE_HEADER} and resolved, and the exception completed with the response's metadata,
     * through the shared {@link ModeledErrors} every protocol runtime uses.
     */
    private static final class PresenceProtocol implements ClientProtocol {
        private final ModeledErrors errors = ModeledErrors.of(context());

        private static ProtocolContext context() {
            ServiceProtocols service = ServiceProtocols.builder(Schema.createService(SERVICE))
                                                       .protocol(ModeledProtocol.of(ProtocolIds.AWS_JSON_1_0))
                                                       .build();
            ProtocolProvider provider = new ProtocolProvider() {
                @Override
                public ShapeId protocolId() {
                    return ProtocolIds.AWS_JSON_1_0;
                }

                @Override
                public boolean usesHttpBindingTraits() {
                    return false;
                }
            };
            SelectedProtocol selected = new SelectedProtocol(service,
                                                             service.protocol(ProtocolIds.AWS_JSON_1_0).orElseThrow(),
                                                             provider, SelectedProtocol.Source.EXPLICIT_PROVIDER);
            SchemaTypeRegistry registry =
                SchemaTypeRegistry.builder()
                                  .partition(List.of(THING_NOT_FOUND.id()),
                                             () -> List.of(RegisteredType.of(THING_NOT_FOUND,
                                                                             ThingNotFoundException.class,
                                                                             ThingNotFoundException::builder)))
                                  .build();
            return ProtocolContext.of(selected, registry);
        }

        @Override
        public ShapeId protocolId() {
            return ProtocolIds.AWS_JSON_1_0;
        }

        @Override
        public OperationProtocol operation(OperationSchemas operation) {
            return new PresenceOperation(operation, errors, ModeledErrors.operationErrorsByName(operation));
        }
    }

    private record PresenceOperation(OperationSchemas schemas, ModeledErrors errors,
                                     Map<String, ShapeId> operationErrors) implements OperationProtocol {
        @Override
        public SdkHttpRequest serializeRequest(SerializableStruct input, URI endpoint) {
            return SdkHttpRequest.builder()
                                 .method("POST")
                                 .scheme(endpoint.getScheme())
                                 .host(endpoint.getHost())
                                 .body(HttpBody.fromUtf8("{}"))
                                 .build();
        }

        @Override
        public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response, ShapeBuilder<T> output) {
            output.setMemberValue(output.schema().member("Value"), response.body().asUtf8String());
            return output.build();
        }

        @Override
        public AwsServiceException deserializeError(SdkHttpResponse response) {
            String type = ErrorCodes.sanitizeErrorType(response.headers().firstValue(ErrorCodes.ERROR_TYPE_HEADER)
                                                               .orElse(null));
            ModeledErrors.ErrorResponse metadata = errors.errorResponse(type, null, response);
            Optional<ShapeId> error = errors.byName(operationErrors, type);
            if (error.isEmpty()) {
                return errors.generic(metadata);
            }
            return errors.complete(errors.newBuilder(error.get()), metadata);
        }
    }

    /** A generated-style operation output: an {@link AwsResponse} with one {@code Value} member. */
    static final class PresenceResponse extends AwsResponse implements SerializableStruct {
        private final String value;

        private PresenceResponse(PresenceBuilder builder) {
            super(builder);
            this.value = builder.value;
        }

        static PresenceBuilder builder() {
            return new PresenceBuilder();
        }

        @Override
        public PresenceBuilder toBuilder() {
            return new PresenceBuilder(this);
        }

        @Override
        public Schema schema() {
            return GET_OUTPUT;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) value;
        }

        static final class PresenceBuilder extends AwsResponse.BuilderImpl implements ShapeBuilder<PresenceResponse> {
            private String value;

            private PresenceBuilder() {
            }

            private PresenceBuilder(PresenceResponse response) {
                super(response);
                this.value = response.value;
            }

            @Override
            public PresenceResponse build() {
                return new PresenceResponse(this);
            }

            @Override
            public ShapeBuilder<PresenceResponse> deserialize(ShapeDeserializer decoder) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setMemberValue(Schema member, Object memberValue) {
                this.value = (String) memberValue;
            }

            @Override
            public Schema schema() {
                return GET_OUTPUT;
            }
        }
    }

    /** A generated-style modeled error of the operation, with no members. */
    static final class ThingNotFoundException extends AwsServiceException implements SerializableStruct {
        private static final long serialVersionUID = 1L;

        private ThingNotFoundException(ThingNotFoundBuilder builder) {
            super(builder);
        }

        public static ThingNotFoundBuilder builder() {
            return new ThingNotFoundBuilder();
        }

        @Override
        public Schema schema() {
            return THING_NOT_FOUND;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return null;
        }

        static final class ThingNotFoundBuilder extends AwsServiceException.BuilderImpl
            implements ShapeBuilder<ThingNotFoundException> {

            @Override
            public ThingNotFoundException build() {
                return new ThingNotFoundException(this);
            }

            @Override
            public ShapeBuilder<ThingNotFoundException> deserialize(ShapeDeserializer decoder) {
                return this;
            }

            @Override
            public void setMemberValue(Schema member, Object memberValue) {
            }

            @Override
            public Schema schema() {
                return THING_NOT_FOUND;
            }
        }
    }

    /** The input builder of {@link #GET_PRESENCE}. */
    private static final class InputBuilder implements ShapeBuilder<Struct> {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Struct build() {
            return new Struct(GET_INPUT, values);
        }

        @Override
        public ShapeBuilder<Struct> deserialize(ShapeDeserializer decoder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            values.put(member.memberName(), value);
        }

        @Override
        public Schema schema() {
            return GET_INPUT;
        }
    }
}
