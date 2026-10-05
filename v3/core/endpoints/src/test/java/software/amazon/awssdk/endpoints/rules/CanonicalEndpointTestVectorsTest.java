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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import software.amazon.awssdk.endpoints.internal.Json;

/**
 * Requirement 19.2: every endpoint rule set and every endpoint test case of the pinned canonical Smithy models
 * (the {@code smithy.rules#endpointRuleSet} and {@code smithy.rules#endpointTests} traits of each service shape) runs
 * through the V3 rules runtime, and each case resolves to exactly its expected endpoint or error. The models are the
 * AWS models checkout that service generation reads ({@code v3.endpoints.canonicalModels}); a missing checkout fails
 * rather than skips the gate.
 */
class CanonicalEndpointTestVectorsTest {
    private static final String RULE_SET = "smithy.rules#endpointRuleSet";
    private static final String TESTS = "smithy.rules#endpointTests";

    private static Path modelsRoot() {
        String root = System.getProperty("v3.endpoints.canonicalModels");
        assertTrue(root != null && !root.isBlank(), "v3.endpoints.canonicalModels is not set");
        Path path = Path.of(root);
        assertTrue(Files.isDirectory(path), "The canonical models checkout " + path + " does not exist");
        return path;
    }

    private static List<Path> models() {
        List<Path> models = new ArrayList<>();
        try (Stream<Path> services = Files.list(modelsRoot())) {
            services.sorted().forEach(service -> {
                Path model = service.resolve("smithy").resolve("model.json");
                if (Files.isRegularFile(model)) {
                    models.add(model);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return models;
    }

    @Test
    void theCanonicalCheckoutHasEndpointRuleSets() {
        assertFalse(models().isEmpty(), "No canonical Smithy models under " + modelsRoot());
    }

    @TestFactory
    Stream<DynamicTest> everyCanonicalEndpointTestCasePasses() {
        AtomicInteger cases = new AtomicInteger();
        return Stream.concat(services(cases), Stream.of(DynamicTest.dynamicTest("every case was executed", () -> {
            // The pinned checkout has over ten thousand cases; far fewer means vectors were silently not run.
            assertTrue(cases.get() > 10_000, "Only " + cases.get() + " canonical endpoint test cases ran");
        })));
    }

    @Test
    void theRunnerReportsCasesAgainstTheWrongRuleSet() throws IOException {
        List<Path> models = models();
        Map<String, Object> first = services(models.get(0)).get(0);
        Map<String, Object> other = services(models.get(models.size() - 1)).get(0);
        EndpointTestSuite suite = EndpointTestSuite.fromDocument(object(first.get(TESTS)));
        List<String> failures = suite.run(EndpointRuleSet.fromDocument(object(other.get(RULE_SET))));
        assertFalse(failures.isEmpty(), "Test vectors of one service passed against another service's rules");
    }

    private Stream<DynamicTest> services(AtomicInteger cases) {
        return models().stream().map(model -> DynamicTest.dynamicTest(
            model.getParent().getParent().getFileName().toString(), () -> {
                List<Map<String, Object>> services = services(model);
                assertFalse(services.isEmpty(), model + " has no service shape with an endpoint rule set");
                for (Map<String, Object> traits : services) {
                    EndpointRuleSet ruleSet = EndpointRuleSet.fromDocument(object(traits.get(RULE_SET)));
                    Object tests = traits.get(TESTS);
                    if (tests == null) {
                        continue;
                    }
                    EndpointTestSuite suite = EndpointTestSuite.fromDocument(object(tests));
                    assertFalse(suite.testCases().isEmpty(), model + " has an empty endpoint test suite");
                    List<String> failures = suite.run(ruleSet);
                    cases.addAndGet(suite.testCases().size());
                    assertEquals(List.of(), failures, model + ": " + failures.size() + " of "
                                                      + suite.testCases().size() + " endpoint test cases failed");
                }
            }));
    }

    /** The traits of every service shape in the model that has an endpoint rule set. */
    private static List<Map<String, Object>> services(Path model) throws IOException {
        Map<String, Object> root = Json.parseObject(Files.readString(model, StandardCharsets.UTF_8));
        List<Map<String, Object>> services = new ArrayList<>();
        for (Object shape : object(root.get("shapes")).values()) {
            Map<String, Object> definition = object(shape);
            if ("service".equals(definition.get("type"))) {
                Map<String, Object> traits = object(definition.getOrDefault("traits", Map.of()));
                if (traits.containsKey(RULE_SET)) {
                    services.add(traits);
                }
            }
        }
        return services;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }
}
