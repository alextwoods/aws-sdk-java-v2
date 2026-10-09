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

package software.amazon.awssdk.test.fleet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * The fleet wire differential (Requirements 19.4, 19.5, 19.14, 19.15): for every case {@link WireCases} selects
 * (deterministically, per protocol and binding category, over the fixed {@link WireCases#V2_SERVICES} whose released V2
 * jars are the frozen baseline), the same request members are sent through the V3 generated client and the V2
 * {@code v3.v2Baseline.version} client over capturing transports that answer with the same canned response. Each case:
 * <ol>
 *   <li>checks the V3 request and response against the <b>canonical expectations</b> the Smithy model and the
 *       protocol specification determine ({@link #canonical}): method, path and label values, query parameters,
 *       headers, the RPC target/action/version/route, the body members, the deserialized output member, and the
 *       modeled or unmodeled error the response names;</li>
 *   <li>compares every normalized aspect of the V3 observation with V2's ({@link Wire}); a difference passes only when
 *       a record of {@code wire-differential/intentional-deltas.json} covers it and names an ADAPTED or EXCLUDED
 *       Parity Manifest entry ({@link IntentionalDeltas}). Every other difference is <b>unclassified</b> and fails
 *       the case.</li>
 * </ol>
 * {@code target/wire-differential/wire-differential-report.json} records every case, its category, the canonical
 * problems and the classified and unclassified deltas.
 */
class WireDifferentialTest {
    private static final String OUTPUT_STRING = "fd-out";
    private static final List<Map<String, Object>> REPORT = new ArrayList<>();
    private static List<IntentionalDeltas.Delta> deltas;
    private static Clients.V2Baseline baseline;

    @BeforeAll
    static void openBaseline() {
        deltas = IntentionalDeltas.load();
        baseline = Clients.V2Baseline.open(Path.of(System.getProperty("v3.v2Baseline.dir", "target/v2-baseline")));
    }

    @TestFactory
    Stream<DynamicTest> everyCaseMatchesCanonicalAndV2OrNamesAnIntentionalDelta() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String module : WireCases.V2_SERVICES) {
            Fleet.Service service = Fleet.get().service(module);
            Model model = Fleet.canonicalModel(service);
            List<WireCases.Case> cases = WireCases.select(service, model);
            for (WireCases.Case c : cases) {
                tests.add(DynamicTest.dynamicTest(c.id(), () -> check(model, c)));
            }
        }
        return tests.stream();
    }

    /** Every checked-in intentional delta is valid against the Parity Manifest classification rules. */
    @Test
    void everyIntentionalDeltaNamesAnAdaptedOrExcludedManifestEntry() {
        List<String> problems = new ArrayList<>();
        for (IntentionalDeltas.Delta delta : deltas) {
            problems.addAll(IntentionalDeltas.validate(delta));
        }
        assertEquals(List.of(), problems);
    }

    /** The record format rejects a delta that names a known gap or a PRESERVED entry (Property 62's precondition). */
    @Test
    void aDeltaNamingAKnownGapOrPreservedEntryIsInvalid() {
        IntentionalDeltas.Delta knownGap = new IntentionalDeltas.Delta(
            "probe-known-gap", IntentionalDeltas.glob("s3/*"), IntentionalDeltas.glob("request.path"),
            "behavior:s3.bucket-addressing", "default-behavior", "probe");
        IntentionalDeltas.Delta preserved = new IntentionalDeltas.Delta(
            "probe-preserved", IntentionalDeltas.glob("dynamodb/*"), IntentionalDeltas.glob("request.body"),
            "api:software.amazon.awssdk.services.dynamodb.DynamoDbClient#getItem(x", "dynamodb-get-item-v2-bytecode",
            "probe");
        assertTrue(!IntentionalDeltas.validate(knownGap).isEmpty());
        assertTrue(!IntentionalDeltas.validate(preserved).isEmpty());
        IntentionalDeltas.Delta adapted = new IntentionalDeltas.Delta(
            "probe-adapted", IntentionalDeltas.glob("*"), IntentionalDeltas.glob("*"),
            "behavior:telemetry.metrics-publishing", "telemetry-metrics-publishing-behavior", "probe");
        assertEquals(List.of(), IntentionalDeltas.validate(adapted));
    }

    private static void check(Model model, WireCases.Case c) {
        String outputMember = c.outputMember().map(MemberShape::getMemberName).orElse(null);
        Function<Clients.Captured, Clients.Canned> responder = request -> respond(model, c, request);
        Clients.Outcome v3 = Clients.v3(c.service().packageName(), c.operation().getId().getName(), c.input(),
                                        outputMember, responder);
        Clients.Outcome v2 = Clients.v2(baseline, c.service().packageName(), c.operation().getId().getName(),
                                        c.input(), outputMember, responder);
        List<String> canonical = canonical(model, c, v3);
        SortedMap<String, String> v3Aspects = aspects(model, c, v3);
        SortedMap<String, String> v2Aspects = aspects(model, c, v2);
        List<String> unclassified = new ArrayList<>();
        List<String> classified = new ArrayList<>();
        for (String aspect : new TreeSet<>(union(v3Aspects.keySet(), v2Aspects.keySet()))) {
            String a = v3Aspects.get(aspect);
            String b = v2Aspects.get(aspect);
            if (Objects.equals(a, b)) {
                continue;
            }
            String delta = aspect + ": V3 " + a + ", V2 " + b;
            Optional<IntentionalDeltas.Delta> record = deltas.stream().filter(d -> d.covers(c.id(), aspect)).findFirst();
            if (record.isPresent()) {
                classified.add(delta + " [" + record.get().id() + " -> " + record.get().manifestEntry() + "]");
            } else {
                unclassified.add(delta);
            }
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("case", c.id());
        entry.put("category", c.category().name());
        entry.put("canonicalProblems", canonical);
        entry.put("classifiedDeltas", classified);
        entry.put("unclassifiedDeltas", unclassified);
        synchronized (REPORT) {
            REPORT.add(entry);
        }
        List<String> failures = new ArrayList<>();
        canonical.forEach(p -> failures.add("canonical: " + p));
        unclassified.forEach(d -> failures.add("unclassified V3/V2 delta: " + d));
        assertEquals(List.of(), failures, c.id());
    }

    private static <T> java.util.Set<T> union(java.util.Set<T> a, java.util.Set<T> b) {
        java.util.Set<T> result = new TreeSet<>(a);
        result.addAll(b);
        return result;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Normalized observations
    // -------------------------------------------------------------------------------------------------------------

    private static SortedMap<String, String> aspects(Model model, WireCases.Case c, Clients.Outcome outcome) {
        SortedMap<String, String> result = new TreeMap<>(outcome.result());
        if (outcome.request() == null) {
            result.put("request", "not sent");
            return result;
        }
        result.putAll(normalize(model, c, outcome.request()).aspects());
        return result;
    }

    static Wire.Request normalize(Model model, WireCases.Case c, Clients.Captured captured) {
        SortedMap<String, String> headers = new TreeMap<>();
        List<String> names = new ArrayList<>(Wire.SEMANTIC_HEADERS);
        List<String> prefixes = new ArrayList<>();
        for (MemberShape member : model.expectShape(c.operation().getInputShape()).members()) {
            member.getTrait(HttpHeaderTrait.class).ifPresent(t -> names.add(t.getValue().toLowerCase(Locale.ROOT)));
            member.getTrait(HttpPrefixHeadersTrait.class)
                  .ifPresent(t -> prefixes.add(t.getValue().toLowerCase(Locale.ROOT)));
        }
        captured.headers().forEach((name, values) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.equals("content-type")) {
                // The media type decides the wire format; its parameters (charset) do not change a
                // form-urlencoded, JSON or CBOR body's meaning.
                headers.put(lower, values.get(0).split(";")[0].trim().toLowerCase(Locale.ROOT));
            } else if (names.contains(lower) || prefixes.stream().anyMatch(p -> !p.isEmpty() && lower.startsWith(p))) {
                headers.put(lower, String.join(",", values));
            }
        });
        byte[] body = captured.body();
        String encoding = captured.header("content-encoding");
        if (encoding != null && encoding.contains("aws-chunked")) {
            body = decodeAwsChunked(body);
        }
        // An empty path and "/" are the same request target (RFC 9112 3.2.1).
        String path = captured.path() == null || captured.path().isEmpty() ? "/" : captured.path();
        return new Wire.Request(captured.method(), captured.host(), path, captured.query(), headers,
                                Wire.normalizeBody(captured.header("content-type"), body));
    }

    /** The data frames of an {@code aws-chunked} body, without the framing, signatures and trailer. */
    private static byte[] decodeAwsChunked(byte[] body) {
        String text = new String(body, StandardCharsets.ISO_8859_1);
        StringBuilder decoded = new StringBuilder();
        int position = 0;
        while (position < text.length()) {
            int lineEnd = text.indexOf("\r\n", position);
            if (lineEnd < 0) {
                break;
            }
            int size = Integer.parseInt(text.substring(position, lineEnd).split(";")[0].trim(), 16);
            position = lineEnd + 2;
            if (size == 0) {
                break;
            }
            decoded.append(text, position, position + size);
            position += size + 2;
        }
        return decoded.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Protocols and canned responses
    // -------------------------------------------------------------------------------------------------------------

    /** The protocol a captured request speaks, from its wire form (each client is answered in its own protocol). */
    static String protocol(Model model, WireCases.Case c, Clients.Captured request) {
        ServiceShape service = model.expectShape(c.service().id(), ServiceShape.class);
        String contentType = String.valueOf(request.header("content-type")).toLowerCase(Locale.ROOT);
        if ("rpc-v2-cbor".equals(request.header("smithy-protocol"))) {
            return "rpcv2Cbor";
        }
        if (contentType.startsWith("application/x-amz-json-1.0")) {
            return "awsJson1_0";
        }
        if (contentType.startsWith("application/x-amz-json-1.1")) {
            return "awsJson1_1";
        }
        if (contentType.startsWith("application/x-www-form-urlencoded")
            && new String(request.body(), StandardCharsets.UTF_8).contains("Action=")) {
            return service.hasTrait("aws.protocols#ec2Query") ? "ec2Query" : "awsQuery";
        }
        return service.hasTrait("aws.protocols#restXml") ? "restXml" : "restJson1";
    }

    private static Clients.Canned respond(Model model, WireCases.Case c, Clients.Captured request) {
        String protocol = protocol(model, c, request);
        String operation = c.operation().getId().getName();
        Map<String, String> headers = new TreeMap<>();
        headers.put("x-amzn-RequestId", "fd-request-id");
        headers.put("x-amz-request-id", "fd-request-id");
        if (protocol.equals("rpcv2Cbor")) {
            headers.put("smithy-protocol", "rpc-v2-cbor");
            headers.put("Content-Type", "application/cbor");
        } else if (protocol.startsWith("awsJson")) {
            headers.put("Content-Type", "application/x-amz-json-" + protocol.substring(7).replace('_', '.'));
        } else if (protocol.equals("restJson1")) {
            headers.put("Content-Type", "application/json");
        } else {
            headers.put("Content-Type", "text/xml");
        }
        String namespace = model.expectShape(c.service().id()).getTrait(XmlNamespaceTrait.class)
                                .map(XmlNamespaceTrait::getUri).orElse("https://example.com/doc/");
        switch (c.category()) {
            case MODELED_ERROR:
            case UNMODELED_ERROR: {
                Shape error = c.error().map(model::expectShape).orElse(null);
                String code = error == null ? WireCases.UNMODELED_ERROR_CODE
                                            : error.getTrait(AwsQueryErrorTrait.class).map(AwsQueryErrorTrait::getCode)
                                                   .orElse(error.getId().getName());
                int status = error == null ? 400 : error.getTrait(HttpErrorTrait.class).map(HttpErrorTrait::getCode)
                                                        .orElse(400);
                String typeName = error == null ? code : error.getId().toString();
                return new Clients.Canned(status, headersWithError(headers, protocol, code),
                                          errorBody(protocol, code, typeName, namespace));
            }
            case SUCCESS: {
                MemberShape member = c.outputMember().orElseThrow();
                Object value = outputValue(model, member);
                if (protocol.startsWith("rest")) {
                    headers.put(member.expectTrait(HttpHeaderTrait.class).getValue(), String.valueOf(value));
                    return new Clients.Canned(200, headers, bytes(protocol.equals("restJson1") ? "{}" : ""));
                }
                return new Clients.Canned(200, headers, successBody(protocol, operation, member, value, namespace));
            }
            default:
                if (protocol.equals("rpcv2Cbor")) {
                    return new Clients.Canned(200, headers, Wire.cbor(Map.of()));
                }
                return new Clients.Canned(200, headers, bytes(switch (protocol) {
                    case "awsQuery" -> "<" + operation + "Response xmlns=\"" + namespace + "\"><" + operation
                                       + "Result/><ResponseMetadata><RequestId>fd-request-id</RequestId>"
                                       + "</ResponseMetadata></" + operation + "Response>";
                    case "ec2Query" -> "<" + operation + "Response xmlns=\"" + namespace + "\"><requestId>"
                                       + "fd-request-id</requestId></" + operation + "Response>";
                    case "restXml" -> "";
                    default -> "{}";
                }));
        }
    }

    private static Map<String, String> headersWithError(Map<String, String> headers, String protocol, String code) {
        Map<String, String> result = new TreeMap<>(headers);
        if (protocol.equals("restJson1")) {
            result.put("X-Amzn-ErrorType", code);
        }
        return result;
    }

    private static byte[] errorBody(String protocol, String code, String typeName, String namespace) {
        return switch (protocol) {
            case "rpcv2Cbor" -> Wire.cbor(Map.of("__type", typeName, "message", "fd-error"));
            case "awsJson1_0", "awsJson1_1" -> bytes("{\"__type\":\"" + typeName + "\",\"message\":\"fd-error\"}");
            case "restJson1" -> bytes("{\"message\":\"fd-error\"}");
            case "restXml" -> bytes("<Error><Code>" + code + "</Code><Message>fd-error</Message>"
                                    + "<RequestId>fd-request-id</RequestId></Error>");
            case "ec2Query" -> bytes("<Response><Errors><Error><Code>" + code + "</Code><Message>fd-error</Message>"
                                     + "</Error></Errors><RequestID>fd-request-id</RequestID></Response>");
            default -> bytes("<ErrorResponse xmlns=\"" + namespace + "\"><Error><Type>Sender</Type><Code>" + code
                             + "</Code><Message>fd-error</Message></Error><RequestId>fd-request-id</RequestId>"
                             + "</ErrorResponse>");
        };
    }

    private static byte[] successBody(String protocol, String operation, MemberShape member, Object value,
                                      String namespace) {
        String name = member.getMemberName();
        String xmlName = member.getTrait(XmlNameTrait.class).map(XmlNameTrait::getValue).orElse(name);
        return switch (protocol) {
            case "rpcv2Cbor" -> Wire.cbor(Map.of(name, value));
            case "awsJson1_0", "awsJson1_1" -> bytes(Node.printJson(Node.objectNode().withMember(name,
                value instanceof String s ? Node.from(s) : Node.from((Number) value))));
            case "ec2Query" -> {
                String ec2 = member.getTrait(XmlNameTrait.class).map(XmlNameTrait::getValue)
                                   .orElse(Character.toLowerCase(name.charAt(0)) + name.substring(1));
                yield bytes("<" + operation + "Response xmlns=\"" + namespace + "\"><requestId>fd-request-id"
                            + "</requestId><" + ec2 + ">" + value + "</" + ec2 + "></" + operation + "Response>");
            }
            default -> bytes("<" + operation + "Response xmlns=\"" + namespace + "\"><" + operation + "Result><"
                             + xmlName + ">" + value + "</" + xmlName + "></" + operation + "Result>"
                             + "<ResponseMetadata><RequestId>fd-request-id</RequestId></ResponseMetadata></"
                             + operation + "Response>");
        };
    }

    private static Object outputValue(Model model, MemberShape member) {
        return switch (model.expectShape(member.getTarget()).getType()) {
            case INTEGER, LONG -> 7;
            default -> OUTPUT_STRING;
        };
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Canonical expectations
    // -------------------------------------------------------------------------------------------------------------

    /** Every way the V3 observation departs from what the model and protocol specification require. */
    static List<String> canonical(Model model, WireCases.Case c, Clients.Outcome v3) {
        List<String> problems = new ArrayList<>();
        Clients.Captured request = v3.request();
        if (request == null) {
            problems.add("V3 sent no request: " + v3.result());
            return problems;
        }
        ServiceShape service = model.expectShape(c.service().id(), ServiceShape.class);
        OperationShape operation = c.operation();
        String protocol = protocol(model, c, request);
        Wire.Request normalized = normalize(model, c, request);
        String name = operation.getId().getName();
        List<String> modeled = new ArrayList<>();
        for (String p : List.of("awsJson1_0", "awsJson1_1", "restJson1", "restXml", "awsQuery", "ec2Query")) {
            if (service.hasTrait("aws.protocols#" + p)) {
                modeled.add(p);
            }
        }
        if (service.hasTrait("smithy.protocols#rpcv2Cbor")) {
            modeled.add("rpcv2Cbor");
        }
        if (!modeled.contains(protocol)) {
            problems.add("speaks " + protocol + ", the service models " + modeled);
        }
        switch (protocol) {
            case "awsJson1_0", "awsJson1_1" -> {
                expect(problems, "POST", request.method(), "method");
                expect(problems, service.getId().getName() + "." + name, request.header("x-amz-target"),
                       "X-Amz-Target");
            }
            case "awsQuery", "ec2Query" -> {
                expect(problems, "POST", request.method(), "method");
                String form = normalized.body();
                if (!form.contains("Action=" + name) || !form.contains("Version=" + service.getVersion())) {
                    problems.add("form body " + form + " lacks Action=" + name + " / Version=" + service.getVersion());
                }
            }
            case "rpcv2Cbor" -> {
                expect(problems, "POST", request.method(), "method");
                String route = "/service/" + service.getId().getName() + "/operation/" + name;
                if (!request.path().endsWith(route)) {
                    problems.add("path " + request.path() + ", expected the route " + route);
                }
            }
            default -> operation.getTrait(HttpTrait.class).ifPresent(http -> {
                expect(problems, http.getMethod(), request.method(), "method");
            });
        }
        for (MemberShape member : model.expectShape(operation.getInputShape()).members()) {
            Object value = c.input().get(member.getMemberName());
            if (value == null) {
                continue;
            }
            String text = value instanceof java.time.Instant ? null : String.valueOf(value);
            WireCases.Category category = WireCases.category(model, member, protocol.startsWith("rest"));
            switch (category) {
                case LABEL -> {
                    if (!java.net.URLDecoder.decode(request.path(), StandardCharsets.UTF_8).contains(text)) {
                        problems.add("label " + member.getMemberName() + "=" + text + " is not in path "
                                     + request.path());
                    }
                }
                case QUERY -> member.getTrait(HttpQueryTrait.class).ifPresent(q -> {
                    String expected = q.getValue() + "=" + (text == null ? "" : text);
                    if (text == null ? request.query().stream().noneMatch(e -> e.startsWith(q.getValue() + "="))
                                     : !request.query().contains(expected)) {
                        problems.add("query " + request.query() + " lacks " + expected);
                    }
                });
                case HEADER -> member.getTrait(HttpHeaderTrait.class).ifPresent(h -> {
                    String actual = request.header(h.getValue());
                    if (actual == null || text != null && !actual.equals(text)) {
                        problems.add("header " + h.getValue() + " is " + actual + ", expected " + text);
                    }
                });
                case STREAMING, PAYLOAD -> {
                    if (category == WireCases.Category.PAYLOAD
                        && !member.hasTrait(software.amazon.smithy.model.traits.HttpPayloadTrait.class)) {
                        bodyMember(problems, protocol, member, normalized);
                        continue;
                    }
                    String expected = "bytes:" + Base64.getEncoder().encodeToString(bytes(text));
                    if (!normalized.body().equals(expected) && !normalized.body().equals(text)) {
                        problems.add("streaming body " + normalized.body() + ", expected " + text);
                    }
                }
                default -> bodyMember(problems, protocol, member, normalized);
            }
        }
        return responseExpectations(problems, model, c, v3.result());
    }

    /** A member serialized into a document body appears under its wire name. */
    private static void bodyMember(List<String> problems, String protocol, MemberShape member,
                                   Wire.Request normalized) {
        String wireName = protocol.equals("restJson1")
                          ? member.getTrait(JsonNameTrait.class).map(JsonNameTrait::getValue)
                                  .orElse(member.getMemberName())
                          : member.getMemberName();
        if (protocol.equals("ec2Query")) {
            wireName = member.getTrait(software.amazon.smithy.aws.traits.protocols.Ec2QueryNameTrait.class)
                             .map(t -> t.getValue())
                             .orElse(Character.toUpperCase(wireName.charAt(0)) + wireName.substring(1));
        }
        if (!normalized.body().contains(wireName)) {
            problems.add("body member " + wireName + " is not in body " + normalized.body());
        }
    }

    private static List<String> responseExpectations(List<String> problems, Model model, WireCases.Case c,
                                                      SortedMap<String, String> result) {
        switch (c.category()) {
            case SUCCESS -> expect(problems, String.valueOf(outputValue(model, c.outputMember().orElseThrow())),
                                   result.get("response.value"), "deserialized " + c.outputMember().get().getMemberName());
            case MODELED_ERROR -> {
                Shape error = model.expectShape(c.error().orElseThrow());
                String shapeName = error.getId().getName();
                String exception = result.get("response.exception");
                if (!shapeName.equals(exception) && !(shapeName + "Exception").equals(exception)) {
                    problems.add("modeled error " + shapeName + " surfaced as " + result);
                }
                String code = error.getTrait(AwsQueryErrorTrait.class).map(AwsQueryErrorTrait::getCode)
                                   .orElse(shapeName);
                expect(problems, code, result.get("response.errorCode"), "error code");
            }
            case UNMODELED_ERROR -> {
                // An unmodeled error code surfaces as the service's base exception (<Service>Exception), not as a
                // modeled error or the generic AWS exception.
                String exception = result.get("response.exception");
                if (!"exception".equals(result.get("response.outcome")) || exception == null
                    || exception.equals("AwsServiceException") || !exception.endsWith("Exception")) {
                    problems.add("unmodeled error surfaced as " + result + ", not as the service base exception");
                }
                expect(problems, WireCases.UNMODELED_ERROR_CODE, result.get("response.errorCode"), "error code");
            }
            default -> expect(problems, "success", result.get("response.outcome"), "outcome " + result);
        }
        return problems;
    }

    private static void expect(List<String> problems, Object expected, Object actual, String what) {
        if (!Objects.equals(String.valueOf(expected), String.valueOf(actual))) {
            problems.add(what + " is " + actual + ", expected " + expected);
        }
    }

    @AfterAll
    static void writeReport() throws IOException {
        if (baseline != null) {
            baseline.close();
        }
        if (REPORT.isEmpty()) {
            return;
        }
        ArrayNode.Builder cases = ArrayNode.builder();
        Map<String, Integer> perCategory = new TreeMap<>();
        int unclassified = 0;
        int failing = 0;
        for (Map<String, Object> entry : REPORT) {
            perCategory.merge((String) entry.get("category"), 1, Integer::sum);
            @SuppressWarnings("unchecked")
            List<String> u = (List<String>) entry.get("unclassifiedDeltas");
            @SuppressWarnings("unchecked")
            List<String> p = (List<String>) entry.get("canonicalProblems");
            unclassified += u.size();
            failing += u.isEmpty() && p.isEmpty() ? 0 : 1;
            @SuppressWarnings("unchecked")
            List<String> classified = (List<String>) entry.get("classifiedDeltas");
            cases.withValue(Node.objectNodeBuilder()
                                .withMember("case", (String) entry.get("case"))
                                .withMember("category", (String) entry.get("category"))
                                .withMember("canonicalProblems", ArrayNode.fromStrings(p))
                                .withMember("classifiedDeltas", ArrayNode.fromStrings(classified))
                                .withMember("unclassifiedDeltas", ArrayNode.fromStrings(u))
                                .build());
        }
        software.amazon.smithy.model.node.ObjectNode.Builder categories = Node.objectNodeBuilder();
        perCategory.forEach(categories::withMember);
        Node report = Node.objectNodeBuilder()
                          .withMember("formatVersion", 1)
                          .withMember("v2Baseline", System.getProperty(Clients.V2_VERSION_PROPERTY, "unknown"))
                          .withMember("cases", REPORT.size())
                          .withMember("failingCases", failing)
                          .withMember("unclassifiedDeltas", unclassified)
                          .withMember("casesPerCategory", categories.build())
                          .withMember("results", cases.build())
                          .build();
        Path dir = Path.of(System.getProperty("v3.wireDifferential.report", "target/wire-differential"));
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("wire-differential-report.json"), Node.prettyPrintJson(report) + "\n",
                          StandardCharsets.UTF_8);
    }
}
