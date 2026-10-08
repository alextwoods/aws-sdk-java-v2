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

package software.amazon.awssdk.test.endpointtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import software.amazon.awssdk.endpoints.internal.Json;
import software.amazon.awssdk.endpoints.rules.EndpointRuleSet;
import software.amazon.awssdk.endpoints.rules.EndpointTestSuite;

/**
 * Endpoint conformance of the generated fleet (Requirements 19.2, 19.14, 19.15): every endpoint test case
 * ({@code smithy.rules#endpointTests}) of every service pinned by {@code services/service-fleet.json} runs through the
 * V3 endpoint rule evaluator against that service's {@code smithy.rules#endpointRuleSet}, both read from the vendored
 * model files the service module is generated from. A case must resolve to exactly its expected endpoint (URL,
 * headers and properties) or rule error.
 *
 * <p>Gate rules: a failing case fails the build unless {@code approved-endpoint-omissions.txt} lists it with a reason;
 * a listed case that passes, a listed key that names no case, a case that runs longer than {@link #CASE_TIMEOUT}
 * (approved or not), a fleet service whose model is missing, and a fleet service with endpoint tests but no rule set
 * all fail the build. Nothing is skipped. The run writes {@code endpoint-conformance-report.json} with per-service
 * counts.
 *
 * <p>The {@code operationInputs} of each case are not bound through a generated operation here (the fleet is not
 * built); the case {@code params} they resolve to are evaluated, and the generated binding of operation inputs is
 * covered by {@code codegen-tests} ({@code CanonicalEndpointBindingTest}, {@code GeneratedEndpointBindingTest}).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndpointConformanceTest {
    static final Duration CASE_TIMEOUT = Duration.ofSeconds(10);
    private static final String RULE_SET = "smithy.rules#endpointRuleSet";
    private static final String TESTS = "smithy.rules#endpointTests";
    private static final String OMISSIONS = "approved-endpoint-omissions.txt";

    private final Map<String, ServiceResult> results = new TreeMap<>();
    private Map<String, Omission> omissions;
    private ExecutorService executor;

    /** One approved omission: {@code <service name> <case index> | <reason> | <reference>}. */
    record Omission(String key, String reason, String reference) {
    }

    /** The outcome of one fleet service. */
    record ServiceResult(String service, int cases, int passed, int approved, int operationInputs, boolean tests,
                         List<String> failures) {
    }

    @BeforeAll
    void setUp() {
        omissions = loadOmissions();
        executor = newExecutor();
    }

    @AfterAll
    void tearDown() throws IOException {
        executor.shutdownNow();
        writeReport();
    }

    @TestFactory
    Stream<DynamicTest> everyFleetEndpointTestCase() {
        Stream<DynamicTest> services = fleet().stream().map(entry -> DynamicTest.dynamicTest(
            entry.name(), () -> runService(entry)));
        // Dynamic tests run in stream order, so these run after every service.
        Stream<DynamicTest> gates = Stream.of(
            DynamicTest.dynamicTest("every fleet service ran", () -> assertEquals(fleet().size(), results.size())),
            DynamicTest.dynamicTest("every approved omission names a fleet case", this::noStaleOmissions));
        return Stream.concat(services, gates);
    }

    private void noStaleOmissions() {
        List<String> stale = new ArrayList<>();
        for (String key : omissions.keySet()) {
            String[] parts = key.split(" ");
            ServiceResult result = parts.length == 2 ? results.get(parts[0]) : null;
            if (result == null || !parts[1].matches("\\d+") || Integer.parseInt(parts[1]) >= result.cases()) {
                stale.add(key);
            }
        }
        assertEquals(List.of(), stale, "Approved endpoint omissions that name no fleet endpoint test case");
    }

    private void runService(FleetEntry entry) {
        Map<String, Object> traits = serviceTraits(entry);
        Object tests = traits.get(TESTS);
        if (tests == null) {
            // Nothing to run: the canonical model has no endpoint test vectors for this service.
            results.put(entry.name(), new ServiceResult(entry.service(), 0, 0, 0, 0, false, List.of()));
            return;
        }
        assertTrue(traits.containsKey(RULE_SET), entry.module() + " has endpoint tests but no endpoint rule set");
        EndpointRuleSet ruleSet = EndpointRuleSet.fromDocument(object(traits.get(RULE_SET)));
        EndpointTestSuite suite = EndpointTestSuite.fromDocument(object(tests));
        assertFalse(suite.testCases().isEmpty(), entry.module() + " has an empty endpoint test suite");

        int passed = 0;
        int approved = 0;
        int inputs = 0;
        List<String> failures = new ArrayList<>();
        for (EndpointTestSuite.TestCase testCase : suite.testCases()) {
            inputs += testCase.operationInputs().size();
            String key = entry.name() + " " + testCase.index();
            Optional<String> failure = check(ruleSet, testCase);
            Omission omission = omissions.get(key);
            if (failure.isEmpty()) {
                if (omission != null) {
                    failures.add(key + ": approved omission passes; remove it from " + OMISSIONS);
                } else {
                    passed++;
                }
            } else if (omission != null && !failure.get().startsWith(TIMEOUT)) {
                approved++;
            } else {
                failures.add(key + ": " + failure.get());
            }
        }
        results.put(entry.name(), new ServiceResult(entry.service(), suite.testCases().size(), passed, approved,
                                                      inputs, true, List.copyOf(failures)));
        assertEquals(List.of(), failures, entry.module() + ": " + failures.size() + " of " + suite.testCases().size()
                                          + " endpoint test cases failed");
    }

    private static final String TIMEOUT = "timed out";

    /** Checks one case on the shared worker, bounded by {@link #CASE_TIMEOUT}. */
    private Optional<String> check(EndpointRuleSet ruleSet, EndpointTestSuite.TestCase testCase) {
        Future<Optional<String>> future = executor.submit(() -> testCase.check(ruleSet));
        try {
            return future.get(CASE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            // The stuck worker is abandoned; later cases run on a fresh one.
            executor.shutdownNow();
            executor = newExecutor();
            return Optional.of(TIMEOUT + ": did not finish within " + CASE_TIMEOUT);
        } catch (ExecutionException e) {
            return Optional.of("the runner threw " + e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.of("interrupted");
        }
    }

    private static ExecutorService newExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "endpoint-conformance-case");
            thread.setDaemon(true);
            return thread;
        });
    }

    // -----------------------------------------------------------------------------------------------------------
    // The pinned fleet and its vendored models

    /** One service of {@code service-fleet.json}. */
    record FleetEntry(String name, String module, String service, Path modelDirectory, List<String> modelFiles) {
    }

    private static Path servicesRoot() {
        String root = System.getProperty("v3.endpointTests.services");
        if (root == null || root.isBlank()) {
            throw new IllegalStateException("v3.endpointTests.services is not set; run the suite through Maven.");
        }
        return Path.of(root);
    }

    private static List<FleetEntry> fleet;

    static synchronized List<FleetEntry> fleet() {
        if (fleet == null) {
            Path root = servicesRoot();
            Map<String, Object> document = Json.parseObject(read(root.resolve("service-fleet.json")));
            List<FleetEntry> entries = new ArrayList<>();
            for (Object service : (List<?>) document.get("services")) {
                Map<String, Object> s = object(service);
                String module = (String) s.get("module");
                List<String> files = ((List<?>) s.get("modelFiles")).stream()
                                                                    .map(f -> (String) object(f).get("path"))
                                                                    .toList();
                entries.add(new FleetEntry((String) s.get("name"), module, (String) s.get("service"),
                                           root.resolve(module).resolve((String) s.get("modelDirectory")), files));
            }
            if (entries.size() < 400) {
                throw new IllegalStateException("Only " + entries.size() + " services in the pinned fleet");
            }
            fleet = List.copyOf(entries);
        }
        return fleet;
    }

    /** The traits of the fleet service's shape, found in its vendored model files. */
    private static Map<String, Object> serviceTraits(FleetEntry entry) {
        for (String file : entry.modelFiles()) {
            Path model = entry.modelDirectory().resolve(file);
            assertTrue(Files.isRegularFile(model), entry.module() + ": the vendored model " + model + " is missing");
            if (!file.endsWith(".json")) {
                continue;
            }
            Map<String, Object> root = Json.parseObject(read(model));
            Object shape = object(root.getOrDefault("shapes", Map.of())).get(entry.service());
            if (shape != null) {
                return object(object(shape).getOrDefault("traits", Map.of()));
            }
        }
        throw new AssertionError(entry.module() + ": no vendored model file defines " + entry.service());
    }

    // -----------------------------------------------------------------------------------------------------------
    // Approved omissions and the report

    private static Map<String, Omission> loadOmissions() {
        Map<String, Omission> entries = new LinkedHashMap<>();
        try (InputStream in = EndpointConformanceTest.class.getClassLoader().getResourceAsStream(OMISSIONS)) {
            if (in == null) {
                throw new IllegalStateException("The approved-omission list " + OMISSIONS + " is missing");
            }
            String[] lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String text = lines[i].strip();
                if (text.isEmpty() || text.startsWith("#")) {
                    continue;
                }
                String[] fields = text.split("\\s+\\|\\s+", -1);
                if (fields.length != 3 || fields[0].isBlank() || fields[1].isBlank() || fields[2].isBlank()) {
                    throw new IllegalStateException(OMISSIONS + ":" + (i + 1)
                                                    + ": expected '<service name> <case index> | <reason> | <reference>'");
                }
                if (entries.put(fields[0].strip(), new Omission(fields[0].strip(), fields[1].strip(),
                                                                fields[2].strip())) != null) {
                    throw new IllegalStateException(OMISSIONS + ":" + (i + 1) + ": duplicate entry " + fields[0]);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return entries;
    }

    private void writeReport() throws IOException {
        String dir = System.getProperty("v3.endpointTests.report");
        if (dir == null || dir.isBlank()) {
            return;
        }
        Path out = Path.of(dir);
        Files.createDirectories(out);
        int cases = results.values().stream().mapToInt(ServiceResult::cases).sum();
        int passed = results.values().stream().mapToInt(ServiceResult::passed).sum();
        int approved = results.values().stream().mapToInt(ServiceResult::approved).sum();
        int inputs = results.values().stream().mapToInt(ServiceResult::operationInputs).sum();
        long withoutTests = results.values().stream().filter(r -> !r.tests()).count();
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"services\": ").append(results.size())
            .append(",\n  \"servicesWithoutEndpointTests\": ").append(withoutTests)
            .append(",\n  \"cases\": ").append(cases)
            .append(",\n  \"passed\": ").append(passed)
            .append(",\n  \"approvedOmissions\": ").append(approved)
            .append(",\n  \"failed\": ").append(cases - passed - approved)
            .append(",\n  \"operationInputs\": ").append(inputs)
            .append(",\n  \"perService\": {\n");
        json.append(results.entrySet().stream().map(e -> {
            ServiceResult r = e.getValue();
            return "    " + quote(e.getKey()) + ": {\"service\": " + quote(r.service()) + ", \"cases\": " + r.cases()
                   + ", \"passed\": " + r.passed() + ", \"approved\": " + r.approved() + ", \"operationInputs\": "
                   + r.operationInputs() + ", \"failures\": ["
                   + r.failures().stream().map(EndpointConformanceTest::quote).collect(Collectors.joining(", "))
                   + "]}";
        }).collect(Collectors.joining(",\n")));
        json.append("\n  },\n  \"omissions\": [");
        json.append(omissions.values().stream()
                             .map(o -> "{\"case\": " + quote(o.key()) + ", \"reason\": " + quote(o.reason()) + "}")
                             .collect(Collectors.joining(", ")));
        json.append("]\n}\n");
        Files.writeString(out.resolve("endpoint-conformance-report.json"), json.toString(), StandardCharsets.UTF_8);
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }
}
