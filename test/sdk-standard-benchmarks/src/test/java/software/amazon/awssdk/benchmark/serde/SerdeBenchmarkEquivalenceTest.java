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

package software.amazon.awssdk.benchmark.serde;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.jmh.annotations.Param;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Pair;

/**
 * Proves that every {@code serde} benchmark does the same serde work on the schema-based path as the Baseline_Build
 * does on the legacy path: for every {@code (class, testCaseId)} combination, the request the schema path serializes
 * equals the request the baseline's legacy marshaller produces from the same input, and the output the schema path
 * deserializes equals the legacy unmarshaller's output for the same response.
 *
 * <p>The legacy side is {@link LegacySerdeReference}, the baseline's {@code @Setup} and {@code @Benchmark} code.
 * Requests are compared part by part (method, scheme, host, port, path, query, headers by case-insensitive name) and
 * bodies by value under their format (JSON and CBOR trees with numbers compared by value, XML by a canonical form
 * that ignores whitespace-only text and namespace prefixes, form bodies as sorted decoded pairs). Outputs are compared
 * member by member ({@link SdkPojo#equalsBySdkFields}) plus the response state outside the members.
 *
 * <p>The test case IDs are read from each class's {@link Param} annotation, so the list cannot drift from what JMH
 * runs. Every difference between the two paths must be listed, with its reason, in {@link #ALLOWED_DIFFERENCES};
 * unknown differences fail.
 */
class SerdeBenchmarkEquivalenceTest {

    private static final String FEATURE_LEGACY_GREEDY_LABEL = "{Key+}";

    private static final ObjectMapper JSON = new ObjectMapper()
        .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true))
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final ObjectMapper CBOR = new ObjectMapper(new CBORFactory())
        .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true))
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    /**
     * Compares JSON and CBOR number nodes by value, so {@code 1.0} equals {@code 1.00} and an int equals a long.
     */
    private static final Comparator<JsonNode> NUMBER_BY_VALUE = (a, b) -> {
        if (a.isNumber() && b.isNumber()) {
            return a.decimalValue().compareTo(b.decimalValue());
        }
        return a.equals(b) ? 0 : 1;
    };

    private static Map<String, Allowance> allowedDifferences() {
        Map<String, Allowance> allowed = new LinkedHashMap<>();

        // The baseline harness (BenchmarkTestCaseLoader.buildOperationInfo) sets OperationInfo.operationIdentifier to
        // the bare operation name, and the legacy JsonProtocolMarshaller sends X-Amz-Target whenever it is set. The
        // generated legacy clients set it only for awsJson, to "<target prefix>.<operation>"; the protocols send what
        // the protocol requires. Same header work, a few bytes apart.
        allowed.put("JsonRpc10MarshallBenchmark:header:x-amz-target", new Allowance(
            "awsJson1_0 X-Amz-Target is <service>.<operation>; the baseline harness sends the bare operation name",
            (legacy, schema) -> schema != null && legacy != null
                                && single(schema).endsWith("." + single(legacy))));
        allowed.put("RestJsonMarshallBenchmark:header:x-amz-target", new Allowance(
            "restJson1 has no X-Amz-Target; the baseline harness's operationIdentifier makes the legacy marshaller send "
            + "one", (legacy, schema) -> schema == null));
        allowed.put("RpcV2CborMarshallBenchmark:header:x-amz-target", new Allowance(
            "rpcv2Cbor has no X-Amz-Target; the baseline harness's operationIdentifier makes the legacy marshaller send "
            + "one", (legacy, schema) -> schema == null));

        // rpcv2Cbor requests are POST /service/<service>/operation/<operation>, as the generated legacy client also
        // sends them. The baseline harness takes the request URI from the test case, which is "/".
        allowed.put("RpcV2CborMarshallBenchmark:encodedPath", new Allowance(
            "rpcv2Cbor path is /service/<service>/operation/<operation>; the baseline harness uses the test case's /",
            (legacy, schema) -> "/".equals(legacy) && String.valueOf(schema).matches("/service/[^/]+/operation/[^/]+")));

        // The bodies are equal CBOR data (compared as trees below), but the legacy Jackson CBOR generator writes every
        // map with an indefinite length (0xBF ... 0xFF) while the ported codec writes maps with a definite length
        // header (structures stay indefinite), one byte less per map.
        allowed.put("RpcV2CborMarshallBenchmark:header:content-length", new Allowance(
            "definite-length CBOR maps are one byte shorter than the legacy indefinite-length maps",
            (legacy, schema) -> legacy != null && schema != null
                                && Long.parseLong(single(schema)) < Long.parseLong(single(legacy))));

        // The CopyObject URI has the greedy label {Key+}. On this (feature) classpath the legacy reference leaves it
        // unexpanded, because SdkFieldsFromSchema maps httpLabel members to MarshallLocation.PATH and never to
        // GREEDY_PATH; on the Baseline_Build (codegen SdkFields with GREEDY_PATH) the same legacy code expands it
        // (verified by running LegacySerdeReference against the baseline jar). The schema path expands it, so it
        // matches the baseline.
        BiPredicate<Object, Object> greedyLabelExpanded = (legacy, schema) -> {
            String l = String.valueOf(legacy);
            String s = String.valueOf(schema);
            return l.endsWith("/" + FEATURE_LEGACY_GREEDY_LABEL)
                   && s.startsWith(l.substring(0, l.length() - FEATURE_LEGACY_GREEDY_LABEL.length()))
                   && !s.contains("{");
        };
        allowed.put("RestJsonMarshallBenchmark:encodedPath", new Allowance(
            "the feature-classpath legacy reference does not expand {Key+}; the baseline and the schema path do",
            greedyLabelExpanded));
        allowed.put("RestXmlMarshallBenchmark:encodedPath", new Allowance(
            "the feature-classpath legacy reference does not expand {Key+}; the baseline and the schema path do",
            greedyLabelExpanded));

        // For a blob payload the legacy XmlProtocolMarshaller adds a default Content-Type: binary/octet-stream before
        // the ContentType member's header is appended, so the baseline sends both values (it does so on the baseline
        // jar too). The protocol sends the member's value only.
        allowed.put("RestXmlMarshallBenchmark:header:content-type", new Allowance(
            "the legacy marshaller adds binary/octet-stream before the ContentType member's value",
            (legacy, schema) -> legacy != null && schema != null
                                && ((List<?>) legacy).equals(Arrays.asList("binary/octet-stream", single(schema)))));

        // awsQuery: the legacy QueryProtocolMarshaller leaves the parameters in the raw query; the legacy client's
        // QueryParametersToBodyStage then moves them into a form body and sets Content-Type and Content-Length, after
        // marshalling, so the baseline benchmark does not measure the form encoding. The protocol writes the form body
        // and both headers in serializeRequest. The parameters are compared (legacy raw query against the schema body)
        // in requestDifferences.
        allowed.put("QueryMarshallBenchmark:encodedPath", new Allowance(
            "the legacy marshaller leaves the path empty; the protocol sends POST /",
            (legacy, schema) -> "".equals(legacy) && "/".equals(schema)));
        allowed.put("QueryMarshallBenchmark:header:content-type", new Allowance(
            "set by the legacy client's QueryParametersToBodyStage after marshalling; set by the protocol",
            (legacy, schema) -> legacy == null && schema != null
                                && single(schema).startsWith("application/x-www-form-urlencoded")));
        allowed.put("QueryMarshallBenchmark:header:content-length", new Allowance(
            "set by the legacy client's QueryParametersToBodyStage after marshalling; set by the protocol",
            (legacy, schema) -> legacy == null && schema != null));
        // The baseline harness's OperationInfo has no apiVersion, so the legacy marshaller writes Version=null; the
        // protocol writes the service version, as the generated legacy client does.
        allowed.put("QueryMarshallBenchmark:param:Version", new Allowance(
            "the baseline harness's OperationInfo has no apiVersion; the protocol sends the service version",
            (legacy, schema) -> "null".equals(legacy) && "1999-12-31".equals(schema)));

        // deserializeResponse returns the output with the SdkHttpResponse and the AwsResponseMetadata (request ID and
        // response headers) attached. The legacy unmarshallers return the members only; the legacy client's response
        // handler attaches both after unmarshalling, outside the baseline benchmark's timed method.
        for (UnmarshallBenchmark benchmark : UNMARSHALL_BENCHMARKS) {
            String type = benchmark.type.getSimpleName();
            allowed.put(type + ":sdkHttpResponse", new Allowance(
                "attached by deserializeResponse; by the legacy client's response handler after unmarshalling",
                (legacy, schema) -> legacy == null && schema != null));
            allowed.put(type + ":responseMetadata", new Allowance(
                "attached by deserializeResponse; by the legacy client's response handler after unmarshalling",
                (legacy, schema) -> "null".equals(legacy) && String.valueOf(schema).contains("AWS_REQUEST_ID")));
        }

        return Collections.unmodifiableMap(allowed);
    }

    private enum BodyFormat {
        JSON, CBOR, XML, QUERY
    }

    @FunctionalInterface
    private interface LegacyCall<T> {
        T apply(String testCaseId) throws Exception;
    }

    private static final class Allowance {
        private final String reason;
        private final BiPredicate<Object, Object> accepts;

        private Allowance(String reason, BiPredicate<Object, Object> accepts) {
            this.reason = reason;
            this.accepts = accepts;
        }
    }

    private static final class Difference {
        private final String key;
        private final Object legacy;
        private final Object schema;

        private Difference(String key, Object legacy, Object schema) {
            this.key = key;
            this.legacy = legacy;
            this.schema = schema;
        }

        private boolean allowed() {
            Allowance allowance = ALLOWED_DIFFERENCES.get(key);
            return allowance != null && allowance.accepts.test(legacy, schema);
        }

        @Override
        public String toString() {
            Allowance allowance = ALLOWED_DIFFERENCES.get(key);
            String note = allowance == null ? "" : " (the allowance '" + allowance.reason + "' rejects these values)";
            return key + " legacy=" + legacy + " schema=" + schema + note;
        }
    }

    private static final class MarshallBenchmark {
        private final Class<?> type;
        private final LegacyCall<SdkHttpFullRequest> legacy;
        private final BodyFormat format;

        private MarshallBenchmark(Class<?> type, LegacyCall<SdkHttpFullRequest> legacy, BodyFormat format) {
            this.type = type;
            this.legacy = legacy;
            this.format = format;
        }

        @Override
        public String toString() {
            return type.getSimpleName();
        }
    }

    private static final class UnmarshallBenchmark {
        private final Class<?> type;
        private final LegacyCall<Object> legacy;

        private UnmarshallBenchmark(Class<?> type, LegacyCall<Object> legacy) {
            this.type = type;
            this.legacy = legacy;
        }

        @Override
        public String toString() {
            return type.getSimpleName();
        }
    }

    private static final List<MarshallBenchmark> MARSHALL_BENCHMARKS = Arrays.asList(
        new MarshallBenchmark(JsonRpc10MarshallBenchmark.class, LegacySerdeReference::jsonRpc10Marshall,
                              BodyFormat.JSON),
        new MarshallBenchmark(RestJsonMarshallBenchmark.class, LegacySerdeReference::restJsonMarshall,
                              BodyFormat.JSON),
        new MarshallBenchmark(RestXmlMarshallBenchmark.class, LegacySerdeReference::restXmlMarshall,
                              BodyFormat.XML),
        new MarshallBenchmark(QueryMarshallBenchmark.class, LegacySerdeReference::queryMarshall,
                              BodyFormat.QUERY),
        new MarshallBenchmark(RpcV2CborMarshallBenchmark.class, LegacySerdeReference::rpcV2CborMarshall,
                              BodyFormat.CBOR));

    private static final List<UnmarshallBenchmark> UNMARSHALL_BENCHMARKS = Arrays.asList(
        new UnmarshallBenchmark(JsonRpc10UnmarshallBenchmark.class, LegacySerdeReference::jsonRpc10Unmarshall),
        new UnmarshallBenchmark(RestJsonUnmarshallBenchmark.class, LegacySerdeReference::restJsonUnmarshall),
        new UnmarshallBenchmark(RestXmlUnmarshallBenchmark.class, LegacySerdeReference::restXmlUnmarshall),
        new UnmarshallBenchmark(QueryUnmarshallBenchmark.class, LegacySerdeReference::queryUnmarshall),
        new UnmarshallBenchmark(RpcV2CborUnmarshallBenchmark.class, LegacySerdeReference::rpcV2CborUnmarshall));

    /**
     * Differences between the legacy and the schema path that are inherent to the two paths, keyed by
     * {@code <benchmark class>:<part>}, where the part is a request part ({@code encodedPath},
     * {@code header:<lower-case name>}, {@code param:<form parameter>}) or an output part
     * ({@code sdkHttpResponse}, {@code responseMetadata}). An entry only allows the values its predicate accepts.
     * Declared after the benchmark lists, which {@link #allowedDifferences()} reads.
     */
    private static final Map<String, Allowance> ALLOWED_DIFFERENCES = allowedDifferences();

    static Stream<Arguments> marshallCases() {
        return MARSHALL_BENCHMARKS.stream()
                                  .flatMap(b -> testCaseIds(b.type).stream().map(id -> Arguments.of(b, id)));
    }

    static Stream<Arguments> unmarshallCases() {
        return UNMARSHALL_BENCHMARKS.stream()
                                    .flatMap(b -> testCaseIds(b.type).stream().map(id -> Arguments.of(b, id)));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("marshallCases")
    void marshallOnce_whenComparedWithBaselineLegacyMarshaller_producesTheSameRequest(MarshallBenchmark benchmark,
                                                                                       String testCaseId)
        throws Exception {
        Object instance = setUp(benchmark.type, testCaseId);

        SdkHttpFullRequest schema = (SdkHttpFullRequest) invoke(instance, "marshallOnce");
        SdkHttpFullRequest legacy = benchmark.legacy.apply(testCaseId);

        assertThat(notAllowed(requestDifferences(benchmark, legacy, schema)))
            .as("differences not in ALLOWED_DIFFERENCES")
            .isEmpty();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("unmarshallCases")
    void unmarshallOnce_whenComparedWithBaselineLegacyUnmarshaller_producesAnEqualOutput(UnmarshallBenchmark benchmark,
                                                                                         String testCaseId)
        throws Exception {
        Object instance = setUp(benchmark.type, testCaseId);

        SdkPojo schema = (SdkPojo) invoke(instance, "unmarshallOnce");
        Object legacyResult = benchmark.legacy.apply(testCaseId);
        // The legacy Query unmarshaller returns the output paired with the ResponseMetadata it read; the legacy
        // client's response handler attaches that map to the output after unmarshalling. Compare the output.
        SdkPojo legacy = (SdkPojo) (legacyResult instanceof Pair ? ((Pair<?, ?>) legacyResult).left() : legacyResult);

        assertThat(schema).isInstanceOf(legacy.getClass());
        assertThat(schema.equalsBySdkFields(legacy))
            .as("members of %s equal those of %s", schema, legacy)
            .isTrue();
        assertThat(notAllowed(outputDifferences(benchmark.type, (SdkResponse) legacy, (SdkResponse) schema)))
            .as("differences not in ALLOWED_DIFFERENCES")
            .isEmpty();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("marshallCases")
    void marshallOnce_whenCalledRepeatedly_leavesTheReusedExecutionAttributesUnchanged(MarshallBenchmark benchmark,
                                                                                        String testCaseId)
        throws Exception {
        assertAttributesUnchanged(benchmark.type, testCaseId, "marshallOnce");
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("unmarshallCases")
    void unmarshallOnce_whenCalledRepeatedly_leavesTheReusedExecutionAttributesUnchanged(
        UnmarshallBenchmark benchmark, String testCaseId) throws Exception {
        assertAttributesUnchanged(benchmark.type, testCaseId, "unmarshallOnce");
    }

    private static void assertAttributesUnchanged(Class<?> type, String testCaseId, String method) throws Exception {
        Object instance = setUp(type, testCaseId);
        ExecutionAttributes attributes = (ExecutionAttributes) field(type, "attributes").get(instance);
        Map<?, ?> before = new LinkedHashMap<>(attributes.getAttributes());

        Object first = invoke(instance, method);
        Object second = invoke(instance, method);

        assertThat(attributes.getAttributes()).isEqualTo(before);
        assertThat(second).isNotSameAs(first);
    }

    // ---- Benchmark access ----

    private static List<String> testCaseIds(Class<?> type) {
        try {
            return Arrays.asList(type.getDeclaredField("testCaseId").getAnnotation(Param.class).value());
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(type.getSimpleName() + " has no testCaseId @Param", e);
        }
    }

    private static Object setUp(Class<?> type, String testCaseId) throws Exception {
        Object instance = type.getDeclaredConstructor().newInstance();
        field(type, "testCaseId").set(instance, testCaseId);
        type.getMethod("setup").invoke(instance);
        return instance;
    }

    private static Object invoke(Object instance, String method) throws Exception {
        Method m = instance.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        return m.invoke(instance);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static List<Difference> notAllowed(List<Difference> differences) {
        return differences.stream().filter(d -> !d.allowed()).collect(Collectors.toList());
    }

    // ---- Request comparison ----

    private static List<Difference> requestDifferences(MarshallBenchmark benchmark, SdkHttpFullRequest legacy,
                                                       SdkHttpFullRequest schema) throws Exception {
        String prefix = benchmark.type.getSimpleName() + ":";
        List<Difference> differences = new ArrayList<>();
        compare(differences, prefix + "method", legacy.method(), schema.method());
        compare(differences, prefix + "protocol", legacy.protocol(), schema.protocol());
        compare(differences, prefix + "host", legacy.host(), schema.host());
        compare(differences, prefix + "port", legacy.port(), schema.port());
        compare(differences, prefix + "encodedPath", legacy.encodedPath(), schema.encodedPath());

        Map<String, List<String>> legacyHeaders = caseInsensitive(legacy.headers());
        Map<String, List<String>> schemaHeaders = caseInsensitive(schema.headers());
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(legacyHeaders.keySet());
        names.addAll(schemaHeaders.keySet());
        for (String name : names) {
            compare(differences, prefix + "header:" + name.toLowerCase(), legacyHeaders.get(name),
                    schemaHeaders.get(name));
        }

        byte[] legacyBody = body(legacy);
        byte[] schemaBody = body(schema);
        if (benchmark.format == BodyFormat.QUERY) {
            // The legacy parameters are still in the raw query (see ALLOWED_DIFFERENCES); compare them, and an empty
            // raw query, with the schema form body.
            compare(differences, prefix + "legacyBody", "<empty>", normalizedBody(benchmark.format, legacyBody));
            compare(differences, prefix + "schemaRawQueryParameters", Collections.emptyMap(),
                    schema.rawQueryParameters());
            Map<String, String> legacyParams = queryParameters(legacy.rawQueryParameters());
            Map<String, String> schemaParams = formParameters(new String(schemaBody, StandardCharsets.UTF_8));
            compare(differences, prefix + "param:Version", legacyParams.remove("Version"),
                    schemaParams.remove("Version"));
            compare(differences, prefix + "params", legacyParams, schemaParams);
        } else {
            compare(differences, prefix + "rawQueryParameters", legacy.rawQueryParameters(),
                    schema.rawQueryParameters());
            if (!Arrays.equals(legacyBody, schemaBody)) {
                compare(differences, prefix + "body", normalizedBody(benchmark.format, legacyBody),
                        normalizedBody(benchmark.format, schemaBody));
            }
        }
        return differences;
    }

    private static void compare(List<Difference> differences, String key, Object legacy, Object schema) {
        if (!Objects.equals(legacy, schema)) {
            differences.add(new Difference(key, legacy, schema));
        }
    }

    private static String single(Object headerValues) {
        List<?> values = (List<?>) headerValues;
        return values.size() == 1 ? String.valueOf(values.get(0)) : String.valueOf(values);
    }

    private static Map<String, List<String>> caseInsensitive(Map<String, List<String>> headers) {
        Map<String, List<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((name, values) -> result.computeIfAbsent(name, n -> new ArrayList<>()).addAll(values));
        return result;
    }

    private static byte[] body(SdkHttpFullRequest request) {
        return request.contentStreamProvider()
                      .map(p -> readAll(p.newStream()))
                      .orElse(new byte[0]);
    }

    /**
     * A value whose {@code equals} is equality of the body's content under its format.
     */
    private static Object normalizedBody(BodyFormat format, byte[] body) throws Exception {
        if (body.length == 0) {
            return "<empty>";
        }
        switch (format) {
            case JSON:
                return new ComparableTree(JSON.readTree(body));
            case CBOR:
                return new ComparableTree(CBOR.readTree(body));
            case XML:
                return canonicalXml(body);
            case QUERY:
                return formParameters(new String(body, StandardCharsets.UTF_8));
            default:
                throw new IllegalArgumentException(format.toString());
        }
    }

    /**
     * The decoded parameters of a form body, sorted by name.
     */
    private static Map<String, String> formParameters(String form) throws Exception {
        Map<String, String> params = new TreeMap<>();
        for (String pair : form.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            String previous = params.put(URLDecoder.decode(key, "UTF-8"), URLDecoder.decode(value, "UTF-8"));
            assertThat(previous).as("repeated form parameter %s", key).isNull();
        }
        return params;
    }

    /**
     * The raw query parameters, sorted by name; every legacy Query parameter has one value.
     */
    private static Map<String, String> queryParameters(Map<String, List<String>> raw) {
        Map<String, String> params = new TreeMap<>();
        raw.forEach((name, values) -> {
            assertThat(values).as("values of query parameter %s", name).hasSize(1);
            params.put(name, String.valueOf(values.get(0)));
        });
        return params;
    }

    /**
     * The XML document as a canonical string: elements as {@code {namespace}name[attributes](children)}, attributes
     * sorted and without namespace declarations, whitespace-only text dropped.
     */
    private static String canonicalXml(byte[] body) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(body)).getDocumentElement();
        StringBuilder out = new StringBuilder();
        canonicalXml(root, out);
        return out.toString();
    }

    private static void canonicalXml(Element element, StringBuilder out) {
        out.append('{').append(element.getNamespaceURI()).append('}').append(element.getLocalName()).append('[');
        NamedNodeMap attributes = element.getAttributes();
        List<String> attrs = new ArrayList<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            if ("http://www.w3.org/2000/xmlns/".equals(attr.getNamespaceURI())) {
                continue;
            }
            attrs.add("{" + attr.getNamespaceURI() + "}" + attr.getLocalName() + "=" + attr.getValue());
        }
        Collections.sort(attrs);
        out.append(String.join(",", attrs)).append("](");
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                canonicalXml((Element) child, out);
            } else if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                String text = child.getNodeValue();
                if (!text.trim().isEmpty()) {
                    out.append('"').append(text).append('"');
                }
            }
        }
        out.append(')');
    }

    private static byte[] readAll(InputStream stream) {
        try (InputStream in = stream) {
            return IoUtils.toByteArray(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---- Output comparison ----

    /**
     * The response state outside the members, which {@link SdkPojo#equalsBySdkFields} does not compare.
     */
    private static List<Difference> outputDifferences(Class<?> type, SdkResponse legacy, SdkResponse schema) {
        String prefix = type.getSimpleName() + ":";
        List<Difference> differences = new ArrayList<>();
        compare(differences, prefix + "sdkHttpResponse", describe(legacy.sdkHttpResponse()),
                describe(schema.sdkHttpResponse()));
        if (legacy instanceof AwsResponse) {
            compare(differences, prefix + "responseMetadata",
                    String.valueOf(((AwsResponse) legacy).responseMetadata()),
                    String.valueOf(((AwsResponse) schema).responseMetadata()));
        }
        return differences;
    }

    private static String describe(SdkHttpResponse response) {
        if (response == null) {
            return null;
        }
        return "statusCode=" + response.statusCode() + " headers=" + caseInsensitive(response.headers());
    }

    /**
     * A JSON or CBOR tree whose {@code equals} compares numbers by value.
     */
    private static final class ComparableTree {
        private final JsonNode node;

        private ComparableTree(JsonNode node) {
            this.node = node;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ComparableTree && node.equals(NUMBER_BY_VALUE, ((ComparableTree) o).node);
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return node.toString();
        }
    }
}
