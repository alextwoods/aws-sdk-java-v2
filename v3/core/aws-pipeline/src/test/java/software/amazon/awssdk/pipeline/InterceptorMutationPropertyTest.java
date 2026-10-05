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
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.HeaderProtocol;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.retries.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.AttemptFailure;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 37: Interceptor mutation composes into the next stage (Requirements 11.4, 11.8).
 *
 * <p>For any chain of 1-3 interceptors, each applying a generated compatible mutation at every one of the eight
 * mutable hooks, any driver and any outcome script (success, 503 then success, or 400 at most twice), every interceptor
 * of a hook receives its predecessor's replacement, and the stage after the hook receives exactly the last replacement:
 * the protocol's serializer (the modeled request), the signer (the unsigned request), the transport (the signed
 * request), the protocol's decoders (the response with its replaced body), the retry strategy's classifier (the attempt
 * failure), and the caller (the output or the terminal failure). Each next-stage input also equals {@link Oracle}'s
 * fold of the generated mutations over plain values, and the transmitted {@code Authorization} header, recomputed
 * independently over the transmitted headers and body, covers every pre-sign mutation.
 *
 * <p>The retry decision follows the replacement too: a 400 whose final attempt failure has an {@link IOException} in
 * its cause chain is retried once. The async driver runs inline, so each try is deterministic on the test thread.
 */
class InterceptorMutationPropertyTest {
    private static final Identity IDENTITY = new Identity() {
    };
    private static final AwsOperation<Struct, Struct> SIGNED_GET = PipelineFixtures.operation(
        "com.example#SignedGet", GET_INPUT, GET_OUTPUT,
        ModeledAuth.builder().operationId("com.example#SignedGet").addServiceScheme(HeaderSigningScheme.ID).build());
    private static final int MAX_ATTEMPTS = 2;

    enum Script {
        SUCCESS,
        /** HTTP 503 (retried whatever the replacement), then success. */
        RETRY_THEN_SUCCESS,
        /** HTTP 400, again if the attempt failure's replacement is retryable. */
        BAD_REQUEST
    }

    enum Kind {
        IDENTITY, SET, APPEND, PUT_NAME, PUT_A, REMOVE_A, BODY, ADD, REPLACE, RETRYABLE, WRAP
    }

    /** The compatible mutations each mutable hook may apply. */
    private static final Map<PipelineStage, List<Kind>> KINDS = kinds();

    /** One interceptor's mutation at one hook; {@code token} is the value it introduces. */
    record Mutation(Kind kind, String token) {
        @Override
        public String toString() {
            return kind == Kind.IDENTITY ? "id" : kind + "(" + token + ")";
        }
    }

    /** @param interceptors per interceptor in registration order, its mutation at each mutable hook */
    record Scenario(Driver driver, Script script, List<Map<PipelineStage, Mutation>> interceptors) {
        List<Mutation> at(PipelineStage hook) {
            return interceptors.stream().map(i -> i.get(hook)).toList();
        }
    }

    /** One hook invocation of one interceptor. */
    record Seen(Object in, Object out) {
    }

    /** The headers and body the signature covers; {@code a} is null when {@code X-A} is absent. */
    record Wire(String name, String a, String body) {
        static Wire of(SdkHttpRequest request) {
            return new Wire(request.headers().firstValue("X-Name").orElse(null),
                            request.headers().firstValue("X-A").orElse(null), request.body().asUtf8String());
        }

        String signature() {
            return "X-Name=" + name + ";X-A=" + (a == null ? "" : a) + ";body=" + body;
        }
    }

    /** An attempt failure as the oracle models it: its {@link #describe} text and whether it is retryable. */
    record Failure(String text, boolean retryable) {
    }

    // Feature: aws-sdk-java-v3, Property 37: Interceptor mutation composes into the next stage
    @Property(tries = 500, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 37: Interceptor mutation composes into the next stage")
    void mutationsComposeIntoTheNextStage(@ForAll("scenarios") Scenario scenario) {
        List<Integer> statuses = Oracle.statuses(scenario);
        boolean succeeded = scenario.script() != Script.BAD_REQUEST;
        int attempts = statuses.size();
        Call call = new Call(scenario);
        Object outcome = call.run();
        List<Mutating> chain = call.chain;
        String why = scenario + "\n  outcome: " + outcome;

        assertThat(call.http.requests).as("attempts of " + why).hasSize(attempts);
        assertThat(outcome).as("outcome of " + why).isInstanceOf(succeeded ? Struct.class : RuntimeException.class);
        assertChain(chain, PipelineStage.MODIFY_REQUEST, 1, why);
        assertChain(chain, PipelineStage.MODIFY_BEFORE_SIGNING, attempts, why);
        assertChain(chain, PipelineStage.MODIFY_AFTER_SIGNING, attempts, why);
        assertChain(chain, PipelineStage.MODIFY_HTTP_RESPONSE, attempts, why);
        assertChain(chain, PipelineStage.MODIFY_RESPONSE_BODY, attempts, why);
        assertChain(chain, PipelineStage.MODIFY_ATTEMPT_EXCEPTION, attempts - (succeeded ? 1 : 0), why);
        assertChain(chain, PipelineStage.MODIFY_RESPONSE, succeeded ? 1 : 0, why);
        assertChain(chain, PipelineStage.MODIFY_EXCEPTION, succeeded ? 0 : 1, why);

        // modifyRequest: the serializer of every attempt reads the final replacement.
        Object request = last(chain, PipelineStage.MODIFY_REQUEST, 0);
        assertThat(first(chain, PipelineStage.MODIFY_REQUEST, 0)).as(why).isSameAs(call.request);
        String name = Oracle.text("a", scenario.at(PipelineStage.MODIFY_REQUEST));
        assertThat(member((SerializableStruct) request, "Name")).as("request of " + why).isEqualTo(name);
        assertThat(call.protocol.serialized).as("serializer inputs of " + why).isNotEmpty()
                                            .allSatisfy(input -> assertThat(input).isSameAs(request));

        Wire wire = Oracle.wire(name, scenario.at(PipelineStage.MODIFY_BEFORE_SIGNING));
        List<String> postSign = Oracle.added(scenario.at(PipelineStage.MODIFY_AFTER_SIGNING));
        List<String> responseHeaders = Oracle.added(scenario.at(PipelineStage.MODIFY_HTTP_RESPONSE));
        List<Throwable> classified = call.retries.failures;
        assertThat(call.scheme.inputs).as("signer inputs of " + why).hasSize(attempts);
        assertThat(call.protocol.responses).as("decoder inputs of " + why).hasSize(attempts);
        assertThat(classified).as("classifier inputs of " + why).hasSize(attempts - (succeeded ? 1 : 0));
        for (int k = 0; k < attempts; k++) {
            String at = "attempt " + (k + 1) + " of " + why;
            int status = statuses.get(k);
            // modifyBeforeSigning: from the serialized request to the signer's input.
            SdkHttpRequest serialized = (SdkHttpRequest) first(chain, PipelineStage.MODIFY_BEFORE_SIGNING, k);
            assertThat(Wire.of(serialized)).as("serialized " + at).isEqualTo(new Wire(name, null, name));
            SdkHttpRequest unsigned = call.scheme.inputs.get(k);
            assertThat(unsigned).as("signer input of " + at)
                                .isSameAs(last(chain, PipelineStage.MODIFY_BEFORE_SIGNING, k));
            assertThat(Wire.of(unsigned)).as("signer input of " + at).isEqualTo(wire);

            // modifyAfterSigning: from the signer's output to the transport.
            SignedRequest signed = (SignedRequest) first(chain, PipelineStage.MODIFY_AFTER_SIGNING, k);
            assertThat(signed).as("signer output of " + at).isSameAs(call.scheme.outputs.get(k));
            SdkHttpRequest sent = call.http.requests.get(k);
            assertThat(sent).as("transmitted " + at)
                            .isSameAs(((SignedRequest) last(chain, PipelineStage.MODIFY_AFTER_SIGNING, k)).request());
            assertThat(Wire.of(sent)).as("transmitted " + at).isEqualTo(wire);
            assertThat(sent.headers().allValues("X-P")).as("post-sign headers of " + at).isEqualTo(postSign);
            // The signature covers the pre-sign mutations: recomputed over what was sent, and as the oracle predicts.
            assertThat(sent.headers().firstValue("Authorization")).as("signature of " + at)
                                                                  .contains(Wire.of(sent).signature())
                                                                  .contains(wire.signature());

            // modifyHttpResponse, then modifyResponseBody: from the received response to the decoder's input.
            SdkHttpResponse received = (SdkHttpResponse) first(chain, PipelineStage.MODIFY_HTTP_RESPONSE, k);
            assertThat(received.statusCode()).as("received " + at).isEqualTo(status);
            SdkHttpResponse replaced = (SdkHttpResponse) last(chain, PipelineStage.MODIFY_HTTP_RESPONSE, k);
            assertThat(first(chain, PipelineStage.MODIFY_RESPONSE_BODY, k)).as("body hook input of " + at)
                                                                           .isSameAs(replaced.body());
            SdkHttpResponse decoded = call.protocol.responses.get(k);
            assertThat(decoded.body()).as("decoded body of " + at)
                                      .isSameAs(last(chain, PipelineStage.MODIFY_RESPONSE_BODY, k));
            assertThat(decoded.body().asUtf8String()).as("decoded body of " + at)
                                                     .isEqualTo(Oracle.responseBody(status, scenario));
            assertThat(decoded.statusCode()).as("decoded status of " + at).isEqualTo(status);
            assertThat(decoded.headers().allValues("X-R")).as("decoded headers of " + at).isEqualTo(responseHeaders);

            if (status != 200) {
                // modifyAttemptException: from the decoded error to the classifier.
                assertThat(first(chain, PipelineStage.MODIFY_ATTEMPT_EXCEPTION, k))
                    .as("attempt failure of " + at).isSameAs(call.protocol.decoded.get(k));
                Throwable failure = classified.get(k);
                assertThat(failure).as("classified failure of " + at)
                                   .isSameAs(last(chain, PipelineStage.MODIFY_ATTEMPT_EXCEPTION, k));
                assertThat(describe(failure)).as("classified failure of " + at)
                                             .isEqualTo(Oracle.attemptFailure(status, scenario).text());
            }
        }

        if (succeeded) {
            // modifyResponse: from the decoded output to the caller.
            Object output = first(chain, PipelineStage.MODIFY_RESPONSE, 0);
            assertThat(output).as("decoded output of " + why).isSameAs(call.protocol.decoded.get(attempts - 1));
            assertThat(outcome).as("output of " + why).isSameAs(last(chain, PipelineStage.MODIFY_RESPONSE, 0));
            assertThat(member((SerializableStruct) outcome, "Value")).as("output of " + why).isEqualTo(
                Oracle.text(Oracle.responseBody(200, scenario), scenario.at(PipelineStage.MODIFY_RESPONSE)));
        } else {
            // modifyException: from the last classified failure to the caller.
            Throwable terminal = classified.get(attempts - 1);
            assertThat(first(chain, PipelineStage.MODIFY_EXCEPTION, 0)).as("mapped failure of " + why)
                                                                       .isSameAs(terminal);
            assertThat(outcome).as("surfaced failure of " + why)
                               .isSameAs(last(chain, PipelineStage.MODIFY_EXCEPTION, 0));
            String mapped = Oracle.attemptFailure(400, scenario).text();
            String surfaced = Oracle.terminal(mapped, scenario.at(PipelineStage.MODIFY_EXCEPTION));
            assertThat(describe((Throwable) outcome)).as("surfaced failure of " + why).isEqualTo(surfaced);
        }

        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("script").collect(scenario.script());
        Statistics.label("interceptors").collect(chain.size());
        Statistics.label("interceptors").coverage(c -> c.check(3).percentage(p -> p >= 20));
        Statistics.label("a 400 retried by its replacement").collect(scenario.script() == Script.BAD_REQUEST
                                                                    && attempts == 2);
        Statistics.label("a 400 retried by its replacement").coverage(c -> c.check(true).percentage(p -> p >= 4));
        Statistics.label("pre-sign mutation changed the signature")
                  .collect(!wire.equals(new Wire(name, null, name)));
        Statistics.label("pre-sign mutation changed the signature")
                  .coverage(c -> c.check(true).percentage(p -> p >= 50));
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<String> tokens = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(3);
        List<Arbitrary<Mutation>> perHook = KINDS.values().stream()
            .map(kinds -> Combinators.combine(Arbitraries.of(kinds), tokens).as(Mutation::new)).toList();
        List<PipelineStage> hooks = List.copyOf(KINDS.keySet());
        Arbitrary<Map<PipelineStage, Mutation>> interceptor = Combinators.combine(perHook).as(mutations -> {
            Map<PipelineStage, Mutation> byHook = new EnumMap<>(PipelineStage.class);
            for (int i = 0; i < hooks.size(); i++) {
                byHook.put(hooks.get(i), mutations.get(i));
            }
            return Collections.unmodifiableMap(byHook);
        });
        return Combinators.combine(PipelineArbitraries.drivers(), Arbitraries.of(Script.class),
                                   interceptor.list().ofMinSize(1).ofMaxSize(3))
                          .as(Scenario::new);
    }

    /** Every interceptor ran {@code invocations} times at {@code hook}, each on its predecessor's replacement. */
    private static void assertChain(List<Mutating> chain, PipelineStage hook, int invocations, String why) {
        for (int i = 0; i < chain.size(); i++) {
            assertThat(chain.get(i).seen.get(hook)).as(hook + " invocations of interceptor " + i + " of " + why)
                                                  .hasSize(invocations);
        }
        for (int k = 0; k < invocations; k++) {
            for (int i = 1; i < chain.size(); i++) {
                assertThat(chain.get(i).seen.get(hook).get(k).in())
                    .as(hook + " input of interceptor " + i + " (invocation " + k + ") of " + why)
                    .isSameAs(chain.get(i - 1).seen.get(hook).get(k).out());
            }
        }
    }

    private static Object first(List<Mutating> chain, PipelineStage hook, int invocation) {
        return chain.get(0).seen.get(hook).get(invocation).in();
    }

    private static Object last(List<Mutating> chain, PipelineStage hook, int invocation) {
        return chain.get(chain.size() - 1).seen.get(hook).get(invocation).out();
    }

    private static String member(SerializableStruct struct, String member) {
        return struct.getMemberValue(struct.schema().member(member));
    }

    /** A failure and its causes, e.g. {@code SdkClientException(x)<-Service400}. */
    static String describe(Throwable failure) {
        String head = failure instanceof AwsServiceException service
                      ? "Service" + service.statusCode()
                      : failure.getClass().getSimpleName() + "("
                        + (failure instanceof SdkException sdk ? sdk.rawMessage() : failure.getMessage()) + ")";
        return failure.getCause() == null ? head : head + "<-" + describe(failure.getCause());
    }

    private static Map<PipelineStage, List<Kind>> kinds() {
        Map<PipelineStage, List<Kind>> kinds = new EnumMap<>(PipelineStage.class);
        kinds.put(PipelineStage.MODIFY_REQUEST, List.of(Kind.IDENTITY, Kind.SET, Kind.APPEND));
        kinds.put(PipelineStage.MODIFY_BEFORE_SIGNING,
                  List.of(Kind.IDENTITY, Kind.PUT_NAME, Kind.PUT_A, Kind.REMOVE_A, Kind.BODY));
        kinds.put(PipelineStage.MODIFY_AFTER_SIGNING, List.of(Kind.IDENTITY, Kind.ADD));
        kinds.put(PipelineStage.MODIFY_HTTP_RESPONSE, List.of(Kind.IDENTITY, Kind.ADD, Kind.BODY));
        kinds.put(PipelineStage.MODIFY_RESPONSE_BODY, List.of(Kind.IDENTITY, Kind.SET, Kind.APPEND));
        kinds.put(PipelineStage.MODIFY_ATTEMPT_EXCEPTION,
                  List.of(Kind.IDENTITY, Kind.REPLACE, Kind.RETRYABLE, Kind.WRAP));
        kinds.put(PipelineStage.MODIFY_RESPONSE, List.of(Kind.IDENTITY, Kind.SET, Kind.APPEND));
        kinds.put(PipelineStage.MODIFY_EXCEPTION, List.of(Kind.IDENTITY, Kind.REPLACE, Kind.WRAP));
        return Collections.unmodifiableMap(kinds);
    }

    /** The expected value after each hook's chain: the generated mutations folded over plain values. */
    static final class Oracle {
        private Oracle() {
        }

        /** The HTTP status of each attempt. */
        static List<Integer> statuses(Scenario scenario) {
            return switch (scenario.script()) {
                case SUCCESS -> List.of(200);
                case RETRY_THEN_SUCCESS -> List.of(503, 200);
                case BAD_REQUEST -> attemptFailure(400, scenario).retryable() ? List.of(400, 400) : List.of(400);
            };
        }

        static String text(String start, List<Mutation> mutations) {
            String value = start;
            for (Mutation m : mutations) {
                value = switch (m.kind()) {
                    case SET -> m.token();
                    case APPEND -> value + m.token();
                    default -> value;
                };
            }
            return value;
        }

        /** The unsigned request after the pre-sign chain, from the serialized {@code X-Name} header and body. */
        static Wire wire(String name, List<Mutation> mutations) {
            Wire wire = new Wire(name, null, name);
            for (Mutation m : mutations) {
                wire = switch (m.kind()) {
                    case PUT_NAME -> new Wire(m.token(), wire.a(), wire.body());
                    case PUT_A -> new Wire(wire.name(), m.token(), wire.body());
                    case REMOVE_A -> new Wire(wire.name(), null, wire.body());
                    case BODY -> new Wire(wire.name(), wire.a(), m.token());
                    default -> wire;
                };
            }
            return wire;
        }

        /** The tokens of the {@code ADD} mutations: the values of the header they add, in order. */
        static List<String> added(List<Mutation> mutations) {
            return mutations.stream().filter(m -> m.kind() == Kind.ADD).map(Mutation::token).toList();
        }

        /** The body the decoders read for a response of {@code status}. */
        static String responseBody(int status, Scenario scenario) {
            String body = status == 200 ? "v" : "";
            for (Mutation m : scenario.at(PipelineStage.MODIFY_HTTP_RESPONSE)) {
                body = m.kind() == Kind.BODY ? m.token() : body;
            }
            return text(body, scenario.at(PipelineStage.MODIFY_RESPONSE_BODY));
        }

        /** The failure the classifier receives for an error response; a 503 is retryable whatever replaces it. */
        static Failure attemptFailure(int status, Scenario scenario) {
            Failure failure = new Failure("Service" + status, false);
            for (Mutation m : scenario.at(PipelineStage.MODIFY_ATTEMPT_EXCEPTION)) {
                String t = m.token();
                failure = switch (m.kind()) {
                    case REPLACE -> new Failure("SdkClientException(" + t + ")", false);
                    case RETRYABLE -> new Failure("SdkClientException(" + t + ")<-IOException(" + t + ")", true);
                    case WRAP -> new Failure("SdkClientException(" + t + ")<-" + failure.text(), failure.retryable());
                    default -> failure;
                };
            }
            return new Failure(failure.text(), failure.retryable() || status == 503);
        }

        static String terminal(String start, List<Mutation> mutations) {
            String text = start;
            for (Mutation m : mutations) {
                text = switch (m.kind()) {
                    case REPLACE -> "IllegalStateException(" + m.token() + ")";
                    case WRAP -> "SdkClientException(" + m.token() + ")<-" + text;
                    default -> text;
                };
            }
            return text;
        }
    }

    /** One call of a scenario and what its next stages received. */
    private static final class Call {
        final ScriptedHttp http = new ScriptedHttp();
        final RecordingProtocol protocol = new RecordingProtocol();
        final ClassifierInputs retries = new ClassifierInputs();
        final HeaderSigningScheme scheme = new HeaderSigningScheme();
        final List<Mutating> chain;
        final Struct request = PipelineFixtures.request(GET_INPUT, "a");
        private final Scenario scenario;

        Call(Scenario scenario) {
            this.scenario = scenario;
            this.chain = scenario.interceptors().stream().map(Mutating::new).toList();
        }

        /** Runs the call; its output, or the failure it surfaced. */
        Object run() {
            for (int status : Oracle.statuses(scenario)) {
                http.respond(status, status == 200 ? "v" : null);
            }
            ClientRuntime.Builder runtime =
                ClientRuntime.builder()
                             .configuration(SdkClientConfiguration.builder()
                                                                  .option(SdkClientOption.CLIENT_ENDPOINT,
                                                                          PipelineFixtures.ENDPOINT)
                                                                  .build())
                             .protocol(protocol)
                             .transportMode(scenario.driver().mode)
                             .retryStrategy(retries, "things")
                             .authSchemeSelector(AuthSchemeSelector.create(List.of(scheme)));
            if (scenario.driver() == Driver.ASYNC) {
                runtime.completionExecutor(Runnable::run)
                       .retryScheduler(delay -> CompletableFuture.completedFuture(null));
            }
            chain.forEach(runtime::addInterceptor);
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build());
            AwsCallEngine engine = (scenario.driver() == Driver.SYNC ? builder.syncHttpClient(http)
                                                                    : builder.asyncHttpClient(http)).build();
            PipelinePlan<Struct, Struct> plan = engine.runtime().plan(SIGNED_GET);
            try {
                if (scenario.driver() == Driver.SYNC) {
                    return engine.executeSync(plan, request, null, null);
                }
                CompletableFuture<Struct> future = engine.executeAsync(plan, request, null, null);
                assertThat(future).as("inline async call of " + scenario).isDone();
                return future.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                return e.getCause();
            } catch (Exception e) {
                return e;
            }
        }
    }

    /** Applies its generated mutation at each mutable hook and records each (input, replacement). */
    private static final class Mutating implements ExecutionInterceptor {
        final Map<PipelineStage, List<Seen>> seen = new EnumMap<>(PipelineStage.class);
        private final Map<PipelineStage, Mutation> mutations;

        Mutating(Map<PipelineStage, Mutation> mutations) {
            this.mutations = mutations;
            mutations.keySet().forEach(hook -> seen.put(hook, new CopyOnWriteArrayList<>()));
        }

        private <T> T apply(PipelineStage hook, T in, Function<Mutation, T> mutate) {
            Mutation m = mutations.get(hook);
            T out = m.kind() == Kind.IDENTITY ? in : mutate.apply(m);
            seen.get(hook).add(new Seen(in, out));
            return out;
        }

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            SerializableStruct in = context.request();
            return apply(PipelineStage.MODIFY_REQUEST, in, m -> PipelineFixtures.request(
                GET_INPUT, m.kind() == Kind.SET ? m.token() : member(in, "Name") + m.token()));
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            SdkHttpRequest in = context.httpRequest();
            return apply(PipelineStage.MODIFY_BEFORE_SIGNING, in, m -> switch (m.kind()) {
                case PUT_NAME -> in.toBuilder().putHeader("X-Name", m.token()).build();
                case PUT_A -> in.toBuilder().putHeader("X-A", m.token()).build();
                case REMOVE_A -> in.toBuilder().removeHeader("X-A").build();
                default -> in.toBuilder().body(HttpBody.fromUtf8(m.token())).build();
            });
        }

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            SignedRequest in = context.signedRequest();
            return apply(PipelineStage.MODIFY_AFTER_SIGNING, in, m -> new SignedRequest(
                in.request().toBuilder().addHeader("X-P", m.token()).build(), in.signature()));
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            SdkHttpResponse in = context.httpResponse();
            return apply(PipelineStage.MODIFY_HTTP_RESPONSE, in, m -> m.kind() == Kind.ADD
                ? in.toBuilder().addHeader("X-R", m.token()).build()
                : in.toBuilder().body(HttpBody.fromUtf8(m.token())).build());
        }

        @Override
        public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
            HttpBody in = context.body();
            return apply(PipelineStage.MODIFY_RESPONSE_BODY, in, m -> HttpBody.fromUtf8(
                m.kind() == Kind.SET ? m.token() : in.asUtf8String() + m.token()));
        }

        @Override
        public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
            Throwable in = context.failure();
            return apply(PipelineStage.MODIFY_ATTEMPT_EXCEPTION, in, m -> switch (m.kind()) {
                case REPLACE -> SdkClientException.create(m.token());
                case RETRYABLE -> SdkClientException.create(m.token(), new IOException(m.token()));
                default -> SdkClientException.create(m.token(), in);
            });
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            SerializableStruct in = context.response();
            return apply(PipelineStage.MODIFY_RESPONSE, in, m -> new Struct(
                GET_OUTPUT, Map.of("Value", m.kind() == Kind.SET ? m.token() : member(in, "Value") + m.token())));
        }

        @Override
        public Throwable modifyException(HookContext.ModifyException context) {
            Throwable in = context.failure();
            return apply(PipelineStage.MODIFY_EXCEPTION, in, m -> m.kind() == Kind.REPLACE
                ? new IllegalStateException(m.token())
                : SdkClientException.create(m.token(), in));
        }
    }

    /** {@link HeaderProtocol}, recording its serializer's inputs and its decoders' inputs and results. */
    private static final class RecordingProtocol implements ClientProtocol {
        final List<SerializableStruct> serialized = new CopyOnWriteArrayList<>();
        final List<SdkHttpResponse> responses = new CopyOnWriteArrayList<>();
        final List<Object> decoded = new CopyOnWriteArrayList<>();
        private final HeaderProtocol delegate = new HeaderProtocol();

        @Override
        public ShapeId protocolId() {
            return delegate.protocolId();
        }

        @Override
        public OperationProtocol operation(OperationSchemas operation) {
            OperationProtocol codec = delegate.operation(operation);
            return new OperationProtocol() {
                @Override
                public OperationSchemas schemas() {
                    return codec.schemas();
                }

                @Override
                public SdkHttpRequest serializeRequest(SerializableStruct input, URI endpoint) {
                    serialized.add(input);
                    return codec.serializeRequest(input, endpoint);
                }

                @Override
                public <T extends SerializableStruct> T deserializeResponse(SdkHttpResponse response,
                                                                            ShapeBuilder<T> output) {
                    responses.add(response);
                    T result = codec.deserializeResponse(response, output);
                    decoded.add(result);
                    return result;
                }

                @Override
                public AwsServiceException deserializeError(SdkHttpResponse response) {
                    responses.add(response);
                    AwsServiceException error = codec.deserializeError(response);
                    decoded.add(error);
                    return error;
                }
            };
        }
    }

    /** A standard retry strategy of {@link #MAX_ATTEMPTS} that records each failure its classifier receives. */
    private static final class ClassifierInputs implements RetryStrategy {
        final List<Throwable> failures = new CopyOnWriteArrayList<>();
        private final RetryStrategy delegate = PipelineFixtures.retries(MAX_ATTEMPTS);

        @Override
        public int maxAttempts() {
            return delegate.maxAttempts();
        }

        @Override
        public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
            return delegate.acquireInitialToken(request);
        }

        @Override
        public RetryDecision refreshRetryToken(RetryToken token, AttemptFailure failure) {
            failures.add(failure.failure());
            return delegate.refreshRetryToken(token, failure);
        }

        @Override
        public RetryToken recordSuccess(RetryToken token) {
            return delegate.recordSuccess(token);
        }

        @Override
        public RetryToken recordFailure(RetryToken token) {
            return delegate.recordFailure(token);
        }
    }

    /**
     * Signs by putting {@link Wire#signature()} of the request, which covers {@code X-Name}, {@code X-A} and the body,
     * in {@code Authorization}; records each request it signs and each result; resolves {@link #IDENTITY}.
     */
    private static final class HeaderSigningScheme implements AuthScheme<Identity> {
        static final String ID = "test#headers";
        final List<SdkHttpRequest> inputs = new CopyOnWriteArrayList<>();
        final List<SignedRequest> outputs = new CopyOnWriteArrayList<>();

        @Override
        public String schemeId() {
            return ID;
        }

        @Override
        public Class<Identity> identityType() {
            return Identity.class;
        }

        @Override
        public Optional<IdentityProvider<? extends Identity>> identityProvider(IdentityProviders providers) {
            return Optional.of(new IdentityProvider<>() {
                @Override
                public Class<Identity> identityType() {
                    return Identity.class;
                }

                @Override
                public CompletableFuture<Identity> resolveIdentity(ResolveIdentityRequest request) {
                    return CompletableFuture.completedFuture(IDENTITY);
                }
            });
        }

        @Override
        public HttpSigner<Identity> signer() {
            return sign -> {
                SdkHttpRequest request = sign.request();
                inputs.add(request);
                String signature = Wire.of(request).signature();
                SignedRequest signed =
                    new SignedRequest(request.toBuilder().putHeader("Authorization", signature).build(), signature);
                outputs.add(signed);
                return signed;
            };
        }
    }
}
