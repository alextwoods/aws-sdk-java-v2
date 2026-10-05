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

package software.amazon.awssdk.test.protocols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.http.HttpQueryParameter;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.pipeline.feature.IdempotencyTokenFeature;
import software.amazon.awssdk.protocols.awsjson.AwsJson10ProtocolProvider;
import software.amazon.awssdk.protocols.awsjson.AwsJson11ProtocolProvider;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.awssdk.protocols.restjson.RestJsonProtocolProvider;
import software.amazon.awssdk.test.protocols.support.DynamicService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 47 (Requirement 13.9): for any request of an operation whose input has an {@code @idempotencyToken}
 * member, a caller-supplied token (including the empty string) is sent unchanged, and an omitted token receives exactly
 * one generated value that every attempt of the call sends.
 *
 * <p>The pipeline is not built yet, so each call models its documented contract: {@link IdempotencyTokenFeature#apply}
 * once per call, then every attempt serializes the returned request through a real protocol runtime (AWS JSON 1.0/1.1
 * with the token in the body; REST-JSON with the token bound to the body, a header or a query parameter), and the
 * token is read back from each serialized attempt's wire form. Each try generates the binding, the non-token members
 * (each present or absent), and one to three calls through one feature (as through one client), each with its own
 * supplied-or-absent token and one to eight attempts.
 *
 * <p>The oracle is a counting token generator that returns a distinct value on each call, so a token regenerated for
 * a retry, or shared by two calls, is observable on the wire. The property asserts, per call, that:
 * <ul>
 *   <li>a supplied token: the generator is not called, the request is used as is, and every attempt carries exactly
 *       the supplied value;</li>
 *   <li>an omitted token: the generator is called exactly once, and every attempt carries exactly that value;</li>
 *   <li>every attempt of a call has the same wire form, which apart from the token equals the wire form of the
 *       caller's request (every other member is preserved);</li>
 *   <li>applying the feature again to the call's request (as a retry must not) would neither generate nor change it.</li>
 * </ul>
 */
class IdempotencyTokenStabilityPropertyTest {
    private static final String MODEL = """
        $version: "2"
        namespace com.example.idempotency

        use aws.protocols#awsJson1_0
        use aws.protocols#awsJson1_1
        use aws.protocols#restJson1

        @awsJson1_0
        @awsJson1_1
        @restJson1
        service Things {
            version: "2024-01-01"
            operations: [CreateInBody, CreateInHeader, CreateInQuery]
        }

        @http(method: "POST", uri: "/body")
        operation CreateInBody {
            input := {
                @idempotencyToken
                clientToken: String
                name: String
                count: Integer
                tags: Tags
            }
            output := {}
        }

        @http(method: "POST", uri: "/header")
        operation CreateInHeader {
            input := {
                @idempotencyToken
                @httpHeader("x-amz-client-token")
                clientToken: String
                name: String
                count: Integer
                tags: Tags
            }
            output := {}
        }

        @http(method: "PUT", uri: "/query")
        operation CreateInQuery {
            input := {
                @idempotencyToken
                @httpQuery("clientToken")
                clientToken: String
                name: String
                count: Integer
                tags: Tags
            }
            output := {}
        }

        list Tags {
            member: String
        }
        """;

    private static final String NAMESPACE = "com.example.idempotency#";
    private static final URI ENDPOINT = URI.create("https://things.example.com");
    private static final String TOKEN_MEMBER = "clientToken";
    private static final String TOKEN_HEADER = "x-amz-client-token";
    /** Prefix of generated tokens; supplied tokens never contain {@code #}, so the two cannot be confused. */
    private static final String GENERATED_PREFIX = "#generated-";

    /** Where the token is sent. */
    private enum Location { BODY, HEADER, QUERY }

    /** A protocol runtime and the operation whose token binding it serializes. */
    private enum Binding {
        AWS_JSON_1_0_BODY(new AwsJson10ProtocolProvider(), "CreateInBody", Location.BODY),
        AWS_JSON_1_1_BODY(new AwsJson11ProtocolProvider(), "CreateInBody", Location.BODY),
        REST_JSON_BODY(new RestJsonProtocolProvider(), "CreateInBody", Location.BODY),
        REST_JSON_HEADER(new RestJsonProtocolProvider(), "CreateInHeader", Location.HEADER),
        REST_JSON_QUERY(new RestJsonProtocolProvider(), "CreateInQuery", Location.QUERY);

        private final ProtocolProvider provider;
        private final ShapeId operation;
        private final Location location;

        Binding(ProtocolProvider provider, String operation, Location location) {
            this.provider = provider;
            this.operation = ShapeId.from(NAMESPACE + operation);
            this.location = location;
        }
    }

    /** The non-token members of a request; {@code null} is absent. */
    private record Members(String name, Integer count, List<String> tags) {
    }

    /** One call: the supplied token (empty if omitted) and its number of attempts. */
    private record Call(Optional<String> suppliedToken, int attempts) {
    }

    /** The operation protocol and input schema of each binding. */
    private static final Map<Binding, Plan> PLANS = plans();

    private record Plan(OperationProtocol protocol, Schema input) {
    }

    // Feature: aws-sdk-java-v3, Property 47: Idempotency values are stable across retries
    @Property(tries = 1_000)
    void idempotencyValuesAreStableAcrossRetries(@ForAll Binding binding,
                                                 @ForAll("members") Members members,
                                                 @ForAll("calls") List<Call> calls) {
        Plan plan = PLANS.get(binding);
        AtomicInteger generated = new AtomicInteger();
        IdempotencyTokenFeature<StructDocument> feature =
            IdempotencyTokenFeature.forOperation(binding.operation, plan.input(), () -> builder(plan.input()),
                                                 () -> GENERATED_PREFIX + generated.incrementAndGet())
                                   .orElseThrow();
        assertEquals(TOKEN_MEMBER, feature.tokenMember());

        List<String> generatedTokens = new ArrayList<>();
        for (Call call : calls) {
            Statistics.label("token").collect(call.suppliedToken().map(t -> t.isEmpty() ? "empty" : "supplied")
                                                  .orElse("omitted"));
            StructDocument request = request(plan.input(), members, call.suppliedToken().orElse(null));
            int generatedBefore = generated.get();
            int expectedGenerations = call.suppliedToken().isPresent() ? 0 : 1;

            // Once per call, before the first attempt.
            IdempotencyTokenFeature.Result<StructDocument> execution = feature.apply(request);

            String expectedToken;
            if (call.suppliedToken().isPresent()) {
                expectedToken = call.suppliedToken().get();
                assertEquals(Optional.empty(), execution.generatedToken());
                assertSame(request, execution.request(), "a request with a token is used as is");
            } else {
                expectedToken = GENERATED_PREFIX + generated.get();
                assertEquals(Optional.of(expectedToken), execution.generatedToken());
                generatedTokens.add(expectedToken);
            }
            assertEquals(expectedGenerations, generated.get() - generatedBefore, "generator calls of apply");

            // Every attempt serializes the call's request.
            Wire callerWire = wire(binding, plan.protocol().serializeRequest(request, ENDPOINT)).withoutToken();
            Wire first = null;
            for (int attempt = 1; attempt <= call.attempts(); attempt++) {
                Wire wire = wire(binding, plan.protocol().serializeRequest(execution.request(), ENDPOINT));
                assertEquals(Optional.of(expectedToken), wire.token(), "token of attempt " + attempt);
                assertEquals(callerWire, wire.withoutToken(), "members of attempt " + attempt);
                if (first == null) {
                    first = wire;
                } else {
                    assertEquals(first, wire, "wire form of attempt " + attempt);
                }
            }
            // One generator call per call, whatever the number of attempts.
            assertEquals(expectedGenerations, generated.get() - generatedBefore, "generator calls of the call");

            // A (forbidden) re-application to the call's request would be a no-op.
            IdempotencyTokenFeature.Result<StructDocument> again = feature.apply(execution.request());
            assertSame(execution.request(), again.request());
            assertEquals(Optional.empty(), again.generatedToken());
            assertEquals(expectedGenerations, generated.get() - generatedBefore, "generator calls of re-application");
        }
        // Each call that omitted its token got its own value.
        assertEquals(generatedTokens.size(), Set.copyOf(generatedTokens).size(), "distinct tokens per call");
        assertEquals(generatedTokens.size(), generated.get());
        Statistics.label("binding").collect(binding);
    }

    @Provide
    Arbitrary<Members> members() {
        Arbitrary<String> text = Arbitraries.strings().withCharRange(' ', '~').withChars('\u00e9', '\u4e16', '\n')
                                            .ofMaxLength(20);
        return Combinators.combine(text.injectNull(0.3),
                                   Arbitraries.integers().injectNull(0.3),
                                   text.list().ofMaxSize(3).injectNull(0.3))
                          .as(Members::new);
    }

    @Provide
    Arbitrary<List<Call>> calls() {
        // Visible ASCII (valid in a header and a query parameter), including JSON and URI metacharacters; never '#',
        // so no supplied token looks like a generated one.
        Arbitrary<Optional<String>> supplied = Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.just(Optional.<String>empty())),
            Tuple.of(1, Arbitraries.just(Optional.of(""))),
            Tuple.of(3, Arbitraries.strings().withCharRange('!', '~').excludeChars('#')
                                                 .ofMinLength(1).ofMaxLength(40).map(Optional::of)));
        return Combinators.combine(supplied, Arbitraries.integers().between(1, 8)).as(Call::new)
                          .list().ofMinSize(1).ofMaxSize(3);
    }

    // ---- The execution's request and its wire form ----

    private static ShapeBuilder<StructDocument> builder(Schema input) {
        return SchemaConverter.createDocumentBuilder(input);
    }

    private static StructDocument request(Schema input, Members members, String token) {
        ShapeBuilder<StructDocument> builder = builder(input);
        set(builder, input, "name", members.name());
        set(builder, input, "count", members.count());
        set(builder, input, "tags", members.tags());
        set(builder, input, TOKEN_MEMBER, token);
        return builder.build();
    }

    private static void set(ShapeBuilder<StructDocument> builder, Schema input, String member, Object value) {
        if (value != null) {
            builder.setMemberValue(input.member(member), value);
        }
    }

    /**
     * The wire form of one serialized attempt: method, path, headers and query parameters in order (the token's
     * header or parameter kept apart) and the parsed JSON body (the token's body member kept apart).
     */
    private record Wire(String method, String path, List<String> headers, List<String> query, ObjectNode body,
                        Optional<String> token) {
        Wire withoutToken() {
            return new Wire(method, path, headers, query, body, Optional.empty());
        }
    }

    private static Wire wire(Binding binding, SdkHttpRequest request) {
        List<String> headers = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < request.headers().size(); i++) {
            String name = request.headers().name(i);
            String value = request.headers().value(i);
            if (binding.location == Location.HEADER && name.toLowerCase(Locale.ROOT).equals(TOKEN_HEADER)) {
                tokens.add(value);
            } else {
                headers.add(name.toLowerCase(Locale.ROOT) + ": " + value);
            }
        }
        List<String> query = new ArrayList<>();
        for (HttpQueryParameter parameter : request.queryParameters()) {
            if (binding.location == Location.QUERY && parameter.rawName().equals(TOKEN_MEMBER)) {
                tokens.add(parameter.rawValue() == null ? null : percentDecode(parameter.rawValue()));
            } else {
                query.add(parameter.render());
            }
        }
        String text = request.body().buffered().asUtf8String();
        ObjectNode body = text.isBlank() ? Node.objectNode() : Node.parse(text).expectObjectNode();
        if (binding.location == Location.BODY) {
            body.getMember(TOKEN_MEMBER).ifPresent(token -> tokens.add(token.expectStringNode().getValue()));
            body = body.withoutMember(TOKEN_MEMBER);
        }
        // At most one occurrence of the token on the wire.
        assertTrue(tokens.size() <= 1, "the token occurs " + tokens.size() + " times");
        return new Wire(request.method(), request.encodedPath(), headers, query, body,
                        tokens.isEmpty() ? Optional.empty() : Optional.ofNullable(tokens.get(0)));
    }

    /** RFC 3986 percent-decoding of a raw query value (independent of the SPI's encoder). */
    private static String percentDecode(String raw) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '%') {
                out.write(Integer.parseInt(raw.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                out.write(c);
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    // ---- The test service, served through Smithy Java dynamic schemas in place of generated types ----

    private static Map<Binding, Plan> plans() {
        ClassLoader loader = IdempotencyTokenStabilityPropertyTest.class.getClassLoader();
        Model model = Model.assembler(loader).discoverModels(loader)
                           .addUnparsedModel("idempotency.smithy", MODEL)
                           .assemble()
                           .unwrap();
        DynamicService service = new DynamicService(model, ShapeId.from(NAMESPACE + "Things"));
        Map<Binding, Plan> plans = new EnumMap<>(Binding.class);
        for (Binding binding : Binding.values()) {
            OperationShape operation = service.operation(binding.operation);
            OperationProtocol protocol = service.client(binding.provider).operation(service.schemas(operation));
            plans.put(binding, new Plan(protocol, service.schema(operation.getInputShape())));
        }
        return plans;
    }
}
