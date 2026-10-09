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
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Fleet schema fidelity (Requirements 19.3, 19.14): every generated schema of every fleet service matches its
 * canonical Smithy shape. For each service of {@code services/service-fleet.json} (one dynamic test each), the
 * service's vendored canonical model files are assembled independently of {@code aws-codegen}, its released jar (the
 * fleet artifact on the test class path) is loaded in its own class loader, and {@link SchemaFidelity} compares every
 * generated view with the canonical model: exactly one generated schema per source shape, with the exact shape ID,
 * shape type, recursive member graph, requiredness, defaults, constraints and runtime-relevant traits, plus the model
 * types, registry and operation descriptors that expose them. A service with any problem fails its test with the
 * problems listed; nothing is skipped or sampled.
 *
 * <p>{@code target/schema-fidelity/schema-fidelity-report.json} records, per service, the compared shape, operation,
 * type, schema and member counts and the problems, for the release evidence (task 20.2).
 */
class FleetSchemaFidelityTest {
    private static final int MAX_LISTED_PROBLEMS = 40;
    private static final SortedMap<String, SchemaFidelity.Result> RESULTS = new TreeMap<>();

    @TestFactory
    Stream<DynamicTest> everyGeneratedSchemaMatchesItsCanonicalShape() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Fleet.Module module : Fleet.get().modules()) {
            for (Fleet.Service service : module.services()) {
                tests.add(DynamicTest.dynamicTest(service.name(), () -> check(module, service)));
            }
        }
        return tests.stream();
    }

    private static void check(Fleet.Module module, Fleet.Service service) throws IOException {
        Model model = Fleet.canonicalModel(service);
        SchemaFidelity.Result result;
        try (Fleet.ServiceClassLoader loader = module.open()) {
            result = SchemaFidelity.check(service, model, loader);
        }
        synchronized (RESULTS) {
            RESULTS.put(service.name(), result);
        }
        List<String> problems = result.problems();
        assertTrue(result.schemas() > 0 && result.operations() > 0, service + " compared no schemas or operations");
        assertEquals(List.of(), problems.subList(0, Math.min(problems.size(), MAX_LISTED_PROBLEMS)),
                     problems.size() + " schema-fidelity problems in " + service + " (" + module.jar() + ")");
    }

    /**
     * The oracle's runtime-relevant trait set is exactly the set {@code aws-codegen} documents and uses
     * ({@code awscodegen.schema.RuntimeTraits}, package-private, read reflectively), as in Property 1.
     */
    @Test
    void oracleTraitSet_isTheGeneratorSet() throws ReflectiveOperationException {
        Field table = Class.forName("software.amazon.awssdk.awscodegen.schema.RuntimeTraits")
                           .getDeclaredField("TRAITS");
        table.setAccessible(true);
        SortedMap<?, ?> traits = (SortedMap<?, ?>) table.get(null);
        assertEquals(SchemaFidelity.RUNTIME_TRAIT_IDS, new TreeSet<>(traits.keySet()));
        Map<ShapeId, String> classes = new TreeMap<>();
        for (Class<? extends Trait> type : SchemaFidelity.RUNTIME_TRAITS) {
            classes.put(SchemaFidelity.traitId(type), type.getName());
        }
        assertEquals(classes, new TreeMap<>(traits));
    }

    @AfterAll
    static void writeReport() throws IOException {
        if (RESULTS.isEmpty()) {
            return;
        }
        ArrayNode.Builder services = ArrayNode.builder();
        int shapes = 0;
        int schemas = 0;
        int members = 0;
        int failing = 0;
        for (SchemaFidelity.Result r : RESULTS.values()) {
            shapes += r.shapes();
            schemas += r.schemas();
            members += r.members();
            failing += r.problems().isEmpty() ? 0 : 1;
            services.withValue(Node.objectNodeBuilder()
                                   .withMember("service", r.service().name())
                                   .withMember("shapeId", r.service().id().toString())
                                   .withMember("module", r.service().module())
                                   .withMember("shapes", r.shapes())
                                   .withMember("operations", r.operations())
                                   .withMember("types", r.types())
                                   .withMember("schemas", r.schemas())
                                   .withMember("members", r.members())
                                   .withMember("problems", ArrayNode.fromStrings(r.problems()))
                                   .build());
        }
        ObjectNode report = Node.objectNodeBuilder()
                                .withMember("formatVersion", 1)
                                .withMember("services", RESULTS.size())
                                .withMember("failingServices", failing)
                                .withMember("shapes", shapes)
                                .withMember("schemas", schemas)
                                .withMember("members", members)
                                .withMember("results", services.build())
                                .build();
        Path dir = Path.of(System.getProperty("v3.schemaFidelity.report", "target/schema-fidelity"));
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("schema-fidelity-report.json"), Node.prettyPrintJson(report) + "\n",
                          StandardCharsets.UTF_8);
    }
}
