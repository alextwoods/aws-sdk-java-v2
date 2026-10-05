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

package software.amazon.awssdk.endpoints.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointAuthScheme;
import software.amazon.awssdk.endpoints.EndpointParameters;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.endpoints.internal.Json;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.evaluation.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.evaluation.value.ArrayValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.BooleanValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.EndpointValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.IntegerValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.RecordValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.StringValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;

/**
 * Property 45 (Requirement 13.5): for any valid combination of endpoint parameters of a canonical service rule set
 * (region across every partition, {@code UseFIPS}, {@code UseDualStack}, an {@code Endpoint} override,
 * {@code AccountId}/{@code AccountIdEndpointMode} and the service-specific parameters), the V3 rules runtime resolves
 * exactly what the canonical Smithy rules evaluator resolves for the same rule set: the same URL, headers and
 * properties (including every {@code authSchemes} entry), or the same error-rule message.
 *
 * <p>The oracle is the pinned canonical evaluator ({@code smithy-rules-engine} with the AWS functions of
 * {@code smithy-aws-endpoints}), a test-scope dependency only. Both engines read the same {@code
 * smithy.rules#endpointRuleSet} trait of the pinned canonical model ({@code v3.endpoints.canonicalModels}). Parameter
 * values are drawn from each parameter's type: regions from every canonical partition (listed regions, unlisted regions
 * matching each partition's region pattern, global and pseudo regions, and invalid host labels), endpoint overrides,
 * account IDs and account-endpoint modes, and the string values that occur in the service's canonical endpoint test
 * cases (bucket names, ARNs, stream and access point identifiers), combined freely so most combinations are not a
 * canonical test case.
 *
 * <p>The only non-identical mapping is a rule-produced URL that is not a {@link java.net.URI}: the canonical evaluator
 * returns the string, the runtime fails resolution naming exactly that URL (see {@link Outcome#invalidUrl}).
 */
class CanonicalEndpointDimensionsPropertyTest {
    private static final String RULE_SET = "smithy.rules#endpointRuleSet";
    private static final String TESTS = "smithy.rules#endpointTests";
    private static final String PARTITIONS =
        "software/amazon/smithy/rulesengine/aws/language/functions/partition/partitions.json";
    private static final List<String> SERVICE_NAMES =
        List.of("dynamodb", "s3", "s3control", "sts", "eventbridge", "kinesis");

    /** Regions that no partition lists, each matching exactly one partition's region pattern. */
    private static final List<String> UNLISTED_REGIONS = List.of(
        "us-east-9", "eu-west-7", "ap-northeast-9", "us-gov-central-3", "cn-south-4", "us-iso-north-2",
        "us-isob-west-5", "eu-isoe-west-1", "us-isof-north-3", "eusc-de-west-2");
    /** Pseudo regions, regions of no partition (the rules fall back to the aws partition) and invalid host labels. */
    private static final List<String> OTHER_REGIONS = List.of(
        "fips-us-east-1", "us-east-1-fips", "us-gov-west-1-fips", "fips-aws-global", "xx-unknown-1", "mars-east-1",
        "invalid region", "us-east-1.example", "");
    private static final List<String> ENDPOINT_OVERRIDES = List.of(
        "https://example.com", "https://example.com:8443", "https://example.com/custom/path", "http://localhost:4566",
        "https://127.0.0.1:9000", "https://vpce-0123456789abcdef-abcdefgh.service.us-east-1.vpce.amazonaws.com",
        "not a url");
    private static final List<String> ACCOUNT_IDS = List.of(
        "123456789012", "012345678901", "111122223333", "1234", "not-an-account!");
    private static final List<String> ACCOUNT_ID_ENDPOINT_MODES = List.of("preferred", "required", "disabled");

    private static final Partitions CANONICAL_PARTITIONS = Partitions.load();
    private static final Map<String, Service> SERVICES = loadServices();

    // Feature: aws-sdk-java-v3, Property 45: Endpoint dimensions follow canonical rules
    @Property(tries = 5_000, edgeCases = EdgeCasesMode.MIXIN)
    @Label("Property 45: Endpoint dimensions follow canonical rules")
    void endpointDimensionsFollowCanonicalRules(@ForAll("combinations") Combination combination) {
        Service service = SERVICES.get(combination.service());
        Outcome expected = service.canonicalOutcome(combination.params());
        Outcome actual = service.runtimeOutcome(combination.params());

        collectStatistics(combination, expected);
        assertEquals(expected, actual, () -> combination + " resolved differently from the canonical rules");
    }

    @Example
    void theRuntimeAndCanonicalRuleSetsDeclareTheSameParameters() {
        SERVICES.values().forEach(service -> {
            Map<String, String> runtime = new TreeMap<>();
            service.runtime().parameters().forEach(p -> runtime.put(p.name(), describe(p)));
            Map<String, String> canonical = new TreeMap<>();
            service.parameters().forEach(p -> canonical.put(p.name(), p.describe()));
            assertEquals(canonical, runtime, service.name());
        });
    }

    @Provide
    Arbitrary<Combination> combinations() {
        return Arbitraries.of(SERVICE_NAMES).flatMap(name -> {
            Service service = SERVICES.get(name);
            List<Arbitrary<Optional<Object>>> values = new ArrayList<>();
            service.parameters().forEach(p -> values.add(p.values()));
            return Combinators.combine(values).as(generated -> {
                Map<String, Object> params = new LinkedHashMap<>();
                for (int i = 0; i < generated.size(); i++) {
                    Object value = generated.get(i).orElse(null);
                    if (value != null) {
                        params.put(service.parameters().get(i).name(), value);
                    }
                }
                return new Combination(name, Collections.unmodifiableMap(params));
            });
        });
    }

    private static void collectStatistics(Combination combination, Outcome expected) {
        Map<String, Object> params = combination.params();
        String outcome = expected.error() == null ? "endpoint" : "error";
        Statistics.label("service").collect(combination.service());
        Statistics.label("outcome").collect(outcome);
        Statistics.label("partition").collect(CANONICAL_PARTITIONS.partitionOf((String) params.get("Region")));
        Statistics.label("resolved FIPS/dual-stack").collect(
            outcome, Boolean.TRUE.equals(params.get("UseFIPS")), Boolean.TRUE.equals(params.get("UseDualStack")));
        Statistics.label("resolved dimension").collect(dimension(params, expected));

        // Without these, the property could pass while never reaching the dimensions it is about.
        Statistics.label("service").coverage(c -> SERVICE_NAMES.forEach(s -> c.check(s).count(n -> n > 0)));
        Statistics.label("outcome").coverage(c -> {
            c.check("endpoint").percentage(p -> p > 25);
            c.check("error").count(n -> n > 0);
        });
        Statistics.label("partition").coverage(
            c -> CANONICAL_PARTITIONS.ids().forEach(id -> c.check(id).count(n -> n > 0)));
        Statistics.label("resolved FIPS/dual-stack").coverage(c -> {
            for (boolean fips : new boolean[] {false, true}) {
                for (boolean dualStack : new boolean[] {false, true}) {
                    c.check("endpoint", fips, dualStack).count(n -> n > 0);
                }
            }
        });
        Statistics.label("resolved dimension").coverage(c -> {
            c.check("endpoint override").count(n -> n > 0);
            c.check("account-based").count(n -> n > 0);
            c.check("sigv4a auth").count(n -> n > 0);
            c.check("s3express auth").count(n -> n > 0);
        });
    }

    /** The routing dimension a resolved endpoint shows, for coverage. */
    private static String dimension(Map<String, Object> params, Outcome expected) {
        if (expected.error() != null) {
            return "error";
        }
        if (params.get("Endpoint") instanceof String override && expected.url().startsWith(stripPath(override))) {
            return "endpoint override";
        }
        if (params.get("AccountId") instanceof String accountId && !accountId.isEmpty()
            && expected.url().contains(accountId)) {
            return "account-based";
        }
        for (Map<String, Object> scheme : expected.authSchemes()) {
            if ("sigv4a".equals(scheme.get("name"))) {
                return "sigv4a auth";
            }
            if ("sigv4-s3express".equals(scheme.get("name"))) {
                return "s3express auth";
            }
        }
        return "regional";
    }

    private static String stripPath(String url) {
        int scheme = url.indexOf("://");
        int path = scheme < 0 ? -1 : url.indexOf('/', scheme + 3);
        return path < 0 ? url : url.substring(0, path);
    }

    private static String describe(RuleSetParameter p) {
        return describe(p.type().name(), p.required(), p.builtIn(), p.defaultValue());
    }

    private static String describe(String type, boolean required, String builtIn, Object defaultValue) {
        return type + (required ? " required" : "") + (builtIn == null ? "" : " builtIn=" + builtIn)
               + (defaultValue == null ? "" : " default=" + defaultValue);
    }

    /** One generated combination: a service and its rule-set parameter values (absent parameters are omitted). */
    record Combination(String service, Map<String, Object> params) {
        @Override
        public String toString() {
            return service + " " + params;
        }
    }

    /**
     * A resolution outcome, compared exactly: an endpoint (URL, headers, properties and the auth-scheme entries), an
     * error-rule message, or a failure that reached no error rule.
     */
    record Outcome(String url, Map<String, List<String>> headers, Map<String, Object> properties,
                   List<Map<String, Object>> authSchemes, String error) {
        static final String NO_RULE_MATCHED = "<resolution failed without reaching an error rule>";
        /** The runtime's failure message suffix for a rule-produced URL that is not a URI (see {@link #invalidUrl}). */
        static final String INVALID_URL = "the endpoint rules produced an invalid URL: ";

        /**
         * The one intentional difference: the canonical evaluator returns any rule-produced URL string, while the
         * runtime endpoint URL is a {@link java.net.URI}, so the runtime fails (without an error rule) on a URL that is
         * not a URI, for example a region containing a space. The URL itself must still be exactly the canonical one.
         */
        static Outcome invalidUrl(String url) {
            return error("<invalid endpoint URL " + url + ">");
        }

        static Outcome endpoint(String url, Map<String, List<String>> headers, Map<String, Object> properties,
                                List<Map<String, Object>> authSchemes) {
            return new Outcome(url, headers, properties, authSchemes, null);
        }

        static Outcome error(String message) {
            return new Outcome(null, Map.of(), Map.of(), List.of(), message);
        }
    }

    /** A canonical service: its rule set in both engines and the generators of its parameters. */
    record Service(String name, EndpointRuleSet runtime,
                   software.amazon.smithy.rulesengine.language.EndpointRuleSet canonical, List<Param> parameters) {

        Outcome canonicalOutcome(Map<String, Object> params) {
            Map<Identifier, Value> inputs = new LinkedHashMap<>();
            params.forEach((name, value) -> inputs.put(Identifier.of(name), canonicalValue(value)));
            Value result;
            try {
                result = RuleEvaluator.evaluate(canonical, inputs);
            } catch (RuntimeException e) {
                return Outcome.error(Outcome.NO_RULE_MATCHED);
            }
            if (result instanceof StringValue message) {
                return Outcome.error(message.getValue());
            }
            EndpointValue endpoint = result.expectEndpointValue();
            try {
                URI.create(endpoint.getUrl());
            } catch (IllegalArgumentException e) {
                return Outcome.invalidUrl(endpoint.getUrl());
            }
            Map<String, Object> properties = new LinkedHashMap<>();
            endpoint.getProperties().forEach((key, value) -> properties.put(key, javaValue(value)));
            List<Map<String, Object>> authSchemes = new ArrayList<>();
            if (properties.get(Endpoint.AUTH_SCHEMES_PROPERTY) instanceof List<?> schemes) {
                schemes.forEach(scheme -> authSchemes.add(object(scheme)));
            }
            return Outcome.endpoint(endpoint.getUrl(), endpoint.getHeaders(), properties, authSchemes);
        }

        Outcome runtimeOutcome(Map<String, Object> params) {
            EndpointParameters.Builder builder = EndpointParameters.builder();
            params.forEach(builder::put);
            Endpoint endpoint;
            try {
                endpoint = runtime.resolve(builder.build());
            } catch (EndpointResolutionException e) {
                if (e.ruleError().isPresent()) {
                    return Outcome.error(e.ruleError().get());
                }
                String message = String.valueOf(e.getMessage());
                int invalidUrl = message.indexOf(Outcome.INVALID_URL);
                return invalidUrl < 0 ? Outcome.error(Outcome.NO_RULE_MATCHED)
                                      : Outcome.invalidUrl(message.substring(invalidUrl + Outcome.INVALID_URL.length()));
            }
            return Outcome.endpoint(endpoint.url().toString(), endpoint.headers(), endpoint.properties(),
                                    endpoint.authSchemes().stream().map(EndpointAuthScheme::properties).toList());
        }
    }

    /** One rule-set parameter (read from the canonical rule set) and the values generated for it. */
    record Param(String name, ParameterType type, boolean required, String builtIn, Object defaultValue,
                 List<Object> pool, double absentProbability) {

        /** Present values of the parameter's type, or empty (unbound) where the rule set allows that. */
        Arbitrary<Optional<Object>> values() {
            Arbitrary<Object> present = Arbitraries.of(pool);
            if (required && defaultValue == null) {
                return present.map(Optional::of);
            }
            return present.injectNull(absentProbability).map(Optional::ofNullable);
        }

        String describe() {
            String runtimeType = switch (type) {
                case STRING -> RuleSetParameter.Type.STRING.name();
                case BOOLEAN -> RuleSetParameter.Type.BOOLEAN.name();
                case STRING_ARRAY -> RuleSetParameter.Type.STRING_ARRAY.name();
            };
            return CanonicalEndpointDimensionsPropertyTest.describe(runtimeType, required, builtIn, defaultValue);
        }
    }

    private static Map<String, Service> loadServices() {
        Map<String, Service> services = new LinkedHashMap<>();
        for (String name : SERVICE_NAMES) {
            services.put(name, loadService(name));
        }
        return Collections.unmodifiableMap(services);
    }

    private static Service loadService(String name) {
        Path model = modelsRoot().resolve(name).resolve("smithy").resolve("model.json");
        String json;
        try {
            json = Files.readString(model, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // The canonical engine parses the trait with smithy-model's Node; the runtime with its own JSON reader.
        ObjectNode canonicalTraits = null;
        for (Map.Entry<String, Node> shape : Node.parse(json).expectObjectNode().expectObjectMember("shapes")
                                                  .getStringMap().entrySet()) {
            ObjectNode definition = shape.getValue().expectObjectNode();
            if ("service".equals(definition.expectStringMember("type").getValue())) {
                canonicalTraits = definition.getObjectMember("traits").orElse(Node.objectNode());
            }
        }
        Map<String, Object> runtimeTraits = null;
        for (Object shape : object(Json.parseObject(json).get("shapes")).values()) {
            Map<String, Object> definition = object(shape);
            if ("service".equals(definition.get("type"))) {
                runtimeTraits = object(definition.getOrDefault("traits", Map.of()));
            }
        }
        assertTrue(canonicalTraits != null && runtimeTraits != null, model + " has no service shape");
        software.amazon.smithy.rulesengine.language.EndpointRuleSet canonical =
            software.amazon.smithy.rulesengine.language.EndpointRuleSet.fromNode(
                canonicalTraits.expectObjectMember(RULE_SET));
        EndpointRuleSet runtime = EndpointRuleSet.fromDocument(object(runtimeTraits.get(RULE_SET)));

        Map<String, Set<Object>> testValues = testCaseValues(runtimeTraits.get(TESTS));
        List<Param> parameters = new ArrayList<>();
        for (Parameter p : canonical.getParameters()) {
            String paramName = p.getNameString();
            parameters.add(new Param(paramName, p.getType(), p.isRequired(), p.getBuiltIn().orElse(null),
                                     p.getDefault().map(CanonicalEndpointDimensionsPropertyTest::javaValue)
                                      .orElse(null),
                                     pool(paramName, p.getType(), testValues.getOrDefault(paramName, Set.of())),
                                     absentProbability(paramName)));
        }
        return new Service(name, runtime, canonical, List.copyOf(parameters));
    }

    /** The distinct parameter values of the service's canonical endpoint test cases, by parameter name. */
    private static Map<String, Set<Object>> testCaseValues(Object tests) {
        Map<String, Set<Object>> values = new LinkedHashMap<>();
        if (tests == null) {
            return values;
        }
        for (Object testCase : (List<?>) object(tests).getOrDefault("testCases", List.of())) {
            object(object(testCase).getOrDefault("params", Map.of())).forEach(
                (name, value) -> values.computeIfAbsent(name, n -> new LinkedHashSet<>()).add(value));
        }
        return values;
    }

    private static List<Object> pool(String name, ParameterType type, Set<Object> testValues) {
        Set<Object> pool = new LinkedHashSet<>();
        switch (type) {
            case BOOLEAN -> {
                pool.add(Boolean.FALSE);
                pool.add(Boolean.TRUE);
            }
            case STRING -> {
                switch (name) {
                    case "Region" -> {
                        pool.addAll(CANONICAL_PARTITIONS.listedRegions());
                        pool.addAll(UNLISTED_REGIONS);
                        pool.addAll(OTHER_REGIONS);
                    }
                    case "Endpoint" -> pool.addAll(ENDPOINT_OVERRIDES);
                    case "AccountId" -> pool.addAll(ACCOUNT_IDS);
                    case "AccountIdEndpointMode" -> pool.addAll(ACCOUNT_ID_ENDPOINT_MODES);
                    default -> {
                    }
                }
                testValues.stream().filter(String.class::isInstance).forEach(pool::add);
                if (pool.isEmpty()) {
                    pool.add("value");
                    pool.add("arn:aws:service:us-east-1:123456789012:resource/name");
                }
            }
            case STRING_ARRAY -> {
                testValues.stream().filter(List.class::isInstance).forEach(pool::add);
                pool.add(List.of());
                pool.add(List.of("arn:aws:dynamodb:us-east-1:222222222222:table/table_name"));
                pool.add(List.of("arn:aws:dynamodb:us-east-1:333333333333:table/table_name",
                                 "arn:aws:dynamodb:us-west-2:444444444444:table/table_name"));
                pool.add(List.of("arn:aws:s3:us-east-1:333333333333:stuff"));
            }
            default -> throw new IllegalStateException("Unknown parameter type " + type);
        }
        return List.copyOf(pool);
    }

    private static double absentProbability(String name) {
        return switch (name) {
            case "Region" -> 0.05;
            case "Endpoint" -> 0.8;
            default -> 0.5;
        };
    }

    private static Path modelsRoot() {
        String root = System.getProperty("v3.endpoints.canonicalModels");
        assertTrue(root != null && !root.isBlank(), "v3.endpoints.canonicalModels is not set");
        Path path = Path.of(root);
        assertTrue(Files.isDirectory(path), "The canonical models checkout " + path + " does not exist");
        return path;
    }

    private static Value canonicalValue(Object value) {
        if (value instanceof String s) {
            return Value.stringValue(s);
        }
        if (value instanceof Boolean b) {
            return Value.booleanValue(b);
        }
        if (value instanceof List<?> list) {
            return Value.arrayValue(list.stream().map(v -> (Value) Value.stringValue((String) v)).toList());
        }
        throw new IllegalArgumentException("Not a rule-set parameter value: " + value);
    }

    /** A canonical rule-set value as the runtime's value types (String, Boolean, Integer, List, Map). */
    private static Object javaValue(Value value) {
        if (value instanceof StringValue s) {
            return s.getValue();
        }
        if (value instanceof BooleanValue b) {
            return b.getValue();
        }
        if (value instanceof IntegerValue i) {
            return i.getValue();
        }
        if (value instanceof ArrayValue a) {
            return a.getValues().stream().map(CanonicalEndpointDimensionsPropertyTest::javaValue).toList();
        }
        if (value instanceof RecordValue r) {
            Map<String, Object> record = new LinkedHashMap<>();
            r.getValue().forEach((key, v) -> record.put(key.getName().getValue(), javaValue(v)));
            return record;
        }
        throw new IllegalArgumentException("Unexpected canonical value " + value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return (Map<String, Object>) Objects.requireNonNull(value);
    }

    /** The canonical partition table of the pinned smithy-aws-endpoints release, for generation and coverage. */
    record Partitions(Map<String, Pattern> regexes, Map<String, String> regionToPartition) {
        static Partitions load() {
            Map<String, Pattern> regexes = new LinkedHashMap<>();
            Map<String, String> regions = new LinkedHashMap<>();
            try (InputStream in = CanonicalEndpointDimensionsPropertyTest.class.getClassLoader()
                                                                              .getResourceAsStream(PARTITIONS)) {
                Objects.requireNonNull(in, PARTITIONS + " is not on the test classpath");
                Node root = Node.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                for (Node node : root.expectObjectNode().expectArrayMember("partitions").getElements()) {
                    ObjectNode partition = node.expectObjectNode();
                    String id = partition.expectStringMember("id").getValue();
                    regexes.put(id, Pattern.compile(partition.expectStringMember("regionRegex").getValue()));
                    partition.getObjectMember("regions").orElse(Node.objectNode()).getStringMap()
                             .keySet().forEach(region -> regions.put(region, id));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new Partitions(Collections.unmodifiableMap(regexes), Collections.unmodifiableMap(regions));
        }

        Set<String> ids() {
            return regexes.keySet();
        }

        Set<String> listedRegions() {
            return regionToPartition.keySet();
        }

        /** The partition the canonical aws.partition function selects, as a coverage label. */
        String partitionOf(String region) {
            if (region == null) {
                return "<no region>";
            }
            String listed = regionToPartition.get(region);
            if (listed != null) {
                return listed;
            }
            for (Map.Entry<String, Pattern> partition : regexes.entrySet()) {
                if (partition.getValue().matcher(region).matches()) {
                    return partition.getKey();
                }
            }
            return "<no partition matches>";
        }
    }
}
