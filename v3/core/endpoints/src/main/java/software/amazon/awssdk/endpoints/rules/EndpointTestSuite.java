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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointParameters;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.endpoints.internal.Json;

/**
 * The endpoint test vectors of a service (the value of its {@code smithy.rules#endpointTests} trait) and a runner that
 * checks them against an endpoint resolver.
 *
 * <p>Each {@link TestCase} has rule-set parameters and an expected endpoint (URL, headers and properties, compared
 * exactly) or an expected error rule message. {@link #run(EndpointRuleSet)} checks every case against a rule set
 * directly; {@link TestCase#check(Supplier)} checks one case against any resolution (for example a generated client's
 * parameter binding driven by the case's {@link OperationInput}s), so rule evaluation and parameter binding are
 * verified against the same canonical vectors. Instances are immutable.
 */
public final class EndpointTestSuite {
    private final String version;
    private final List<TestCase> testCases;

    private EndpointTestSuite(String version, List<TestCase> testCases) {
        this.version = version;
        this.testCases = List.copyOf(testCases);
    }

    /**
     * Parses the canonical {@code smithy.rules#endpointTests} JSON document.
     *
     * @throws IllegalArgumentException if it is not a valid endpoint test suite
     */
    public static EndpointTestSuite fromJson(String json) {
        return fromDocument(Json.parseObject(json));
    }

    /** A test suite from an already parsed JSON document. */
    public static EndpointTestSuite fromDocument(Map<String, Object> document) {
        List<TestCase> cases = new ArrayList<>();
        Object testCases = document.getOrDefault("testCases", List.of());
        if (!(testCases instanceof List<?> list)) {
            throw new IllegalArgumentException("endpointTests testCases must be an array");
        }
        for (Object testCase : list) {
            cases.add(TestCase.parse(cases.size(), object(testCase, "test case")));
        }
        return new EndpointTestSuite(document.get("version") instanceof String v ? v : "", cases);
    }

    /** The version of the test suite document. */
    public String version() {
        return version;
    }

    /** Every test case, in document order. Immutable. */
    public List<TestCase> testCases() {
        return testCases;
    }

    /** Checks every test case against {@code ruleSet}; returns one message per failing case, in order. */
    public List<String> run(EndpointRuleSet ruleSet) {
        List<String> failures = new ArrayList<>();
        for (TestCase testCase : testCases) {
            testCase.check(ruleSet).ifPresent(failures::add);
        }
        return failures;
    }

    /**
     * One endpoint test case.
     *
     * @param index the position of the case in its suite
     * @param documentation the case documentation, or an empty string
     * @param params the rule-set parameter values of the case, by name; immutable
     * @param expectedEndpoint the expected endpoint, or {@code null} if an error is expected
     * @param expectedError the expected error rule message, or {@code null} if an endpoint is expected
     * @param operationInputs the operation calls that must resolve to the same expectation; immutable
     */
    public record TestCase(int index, String documentation, Map<String, Object> params, Endpoint expectedEndpoint,
                           String expectedError, List<OperationInput> operationInputs) {
        public TestCase {
            params = Map.copyOf(params);
            operationInputs = List.copyOf(operationInputs);
            if ((expectedEndpoint == null) == (expectedError == null)) {
                throw new IllegalArgumentException("A test case expects exactly one of an endpoint or an error.");
            }
        }

        static TestCase parse(int index, Map<String, Object> doc) {
            String documentation = doc.get("documentation") instanceof String d ? d : "";
            Map<String, Object> expect = object(doc.get("expect"), "expect of test case " + index);
            Endpoint endpoint = null;
            String error = null;
            if (expect.get("endpoint") != null) {
                Map<String, Object> expected = object(expect.get("endpoint"), "expected endpoint");
                Endpoint.Builder builder = Endpoint.builder().url(URI.create((String) expected.get("url")));
                object(expected.getOrDefault("headers", Map.of()), "expected headers").forEach(
                    (name, values) -> builder.putHeader(name, strings(values)));
                object(expected.getOrDefault("properties", Map.of()), "expected properties")
                    .forEach(builder::putProperty);
                endpoint = builder.build();
            } else if (expect.get("error") instanceof String message) {
                error = message;
            } else {
                throw new IllegalArgumentException("Test case " + index + " expects neither an endpoint nor an error");
            }
            List<OperationInput> inputs = new ArrayList<>();
            if (doc.get("operationInputs") instanceof List<?> list) {
                for (Object input : list) {
                    Map<String, Object> in = object(input, "operation input");
                    inputs.add(new OperationInput(
                        (String) in.get("operationName"),
                        object(in.getOrDefault("builtInParams", Map.of()), "builtInParams"),
                        object(in.getOrDefault("clientParams", Map.of()), "clientParams"),
                        object(in.getOrDefault("operationParams", Map.of()), "operationParams")));
                }
            }
            return new TestCase(index, documentation, object(doc.getOrDefault("params", Map.of()), "params"),
                                endpoint, error, inputs);
        }

        /** The case's {@link #params()} as endpoint parameters. */
        public EndpointParameters parameters() {
            EndpointParameters.Builder builder = EndpointParameters.builder();
            params.forEach(builder::put);
            return builder.build();
        }

        /** Checks the case against {@code ruleSet}: resolves {@link #parameters()} and compares the outcome. */
        public Optional<String> check(EndpointRuleSet ruleSet) {
            Objects.requireNonNull(ruleSet, "ruleSet");
            return check(() -> ruleSet.resolve(parameters()));
        }

        /**
         * Checks the case against a resolution: {@code resolution} must return exactly {@link #expectedEndpoint()} or
         * fail with an {@link EndpointResolutionException} whose rule error is exactly {@link #expectedError()}.
         *
         * @return a failure message, or empty if the outcome is the expected one
         */
        public Optional<String> check(Supplier<Endpoint> resolution) {
            Endpoint actual;
            try {
                actual = resolution.get();
            } catch (EndpointResolutionException e) {
                if (expectedError != null && e.ruleError().filter(expectedError::equals).isPresent()) {
                    return Optional.empty();
                }
                return Optional.of(describe() + ": expected " + expectation() + " but resolution failed: "
                                   + e.getMessage());
            } catch (RuntimeException e) {
                return Optional.of(describe() + ": expected " + expectation() + " but resolution threw " + e);
            }
            if (expectedEndpoint != null && expectedEndpoint.equals(actual)) {
                return Optional.empty();
            }
            return Optional.of(describe() + ": expected " + expectation() + " but got " + actual);
        }

        private String expectation() {
            return expectedEndpoint != null ? expectedEndpoint.toString() : "error \"" + expectedError + "\"";
        }

        private String describe() {
            return "test case " + index + (documentation.isEmpty() ? "" : " (" + documentation + ")");
        }
    }

    /**
     * One operation call of a test case, which must resolve to the case's expectation through the operation's
     * parameter binding.
     *
     * @param operationName the operation shape name
     * @param builtInParams built-in values by built-in ID (for example {@code AWS::Region}); immutable
     * @param clientParams client context parameter values by parameter name; immutable
     * @param operationParams the operation input, as a JSON document of its members; immutable
     */
    public record OperationInput(String operationName, Map<String, Object> builtInParams,
                                 Map<String, Object> clientParams, Map<String, Object> operationParams) {
        public OperationInput {
            Objects.requireNonNull(operationName, "operationName");
            builtInParams = Map.copyOf(builtInParams);
            clientParams = Map.copyOf(clientParams);
        }
    }

    private static List<String> strings(Object values) {
        if (!(values instanceof List<?> list)) {
            throw new IllegalArgumentException("Expected header values must be an array: " + values);
        }
        return list.stream().map(String.class::cast).toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String what) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Invalid endpoint test suite: " + what + " must be an object");
    }
}
