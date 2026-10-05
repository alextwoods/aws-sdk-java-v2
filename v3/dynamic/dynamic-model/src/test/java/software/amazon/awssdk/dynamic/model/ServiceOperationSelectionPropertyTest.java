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

package software.amazon.awssdk.dynamic.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.dynamic.model.DynamicModelException.Reason;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 52: dynamic service and operation selection is exact (Requirements 14.2-14.7).
 *
 * <p>Random runtime models with zero to four services, each with zero to five operations, are drawn from small
 * alphabets of namespaces, service names and operation names that differ only in case or in characters that sort
 * differently by code point than case-insensitively ({@code _} sits between upper and lower case letters). Service and
 * operation selection, and every diagnostic, is compared with an oracle built from the generated inventory sorted by
 * {@link String#compareTo}.
 *
 * <p>Absolute shape IDs are unique case-insensitively across a model, and operation names case-insensitively within a
 * service, because Smithy rejects shape ID and service-closure name conflicts that differ only in case.
 */
class ServiceOperationSelectionPropertyTest {

    private static final List<String> NAMESPACES = List.of("a.b", "a.B", "A.b", "a.b_c", "z", "Z.a");
    private static final List<String> SERVICE_NAMES = List.of("S", "s", "Svc", "sVC", "S_1", "s2");
    private static final List<String> OPERATION_NAMES = List.of("Get", "get", "GET", "Put", "Put_1", "put_1", "PutA",
                                                                "Puta", "Delete", "delete2", "List", "list_B");
    private static final String OUTCOME = "outcome";
    private static final List<String> OUTCOMES = List.of("service():NO_SERVICE", "service():AMBIGUOUS_SERVICE",
                                                         "service():selected", "service(id):NO_SERVICE",
                                                         "service(id):UNKNOWN_SERVICE", "service(id):selected",
                                                         "operation:UNKNOWN_OPERATION", "operation:selected");

    /** A generated service: its absolute ID, the namespace of its operation shapes and its operation names. */
    record GenService(String id, String operationNamespace, List<String> operations) {
        String operationId(String name) {
            return operationNamespace + "#" + name;
        }
    }

    @Property(tries = 300)
    @Label("Property 52: dynamic service and operation selection is exact")
    void selectionIsExact(@ForAll("models") List<GenService> services,
                          @ForAll("serviceIds") String extraServiceId,
                          @ForAll("operationNames") String extraOperationName) {
        DynamicModel model = DynamicModel.fromJsonAst(jsonAst(services), "property-52.json");
        Statistics.label("services").collect(services.size());

        // Oracle: every absolute service ID in code point order.
        List<String> expectedIds = services.stream().map(GenService::id).sorted(String::compareTo).toList();
        assertEquals(expectedIds, model.serviceIds().stream().map(ShapeId::toString).toList());

        // service(): the only service, else NO_SERVICE or AMBIGUOUS_SERVICE listing every ID.
        if (expectedIds.isEmpty()) {
            expectFailure(model::service, Reason.NO_SERVICE, null, null, List.of());
            collect("service():NO_SERVICE");
        } else if (expectedIds.size() > 1) {
            expectFailure(model::service, Reason.AMBIGUOUS_SERVICE, null, null, expectedIds);
            collect("service():AMBIGUOUS_SERVICE");
        } else {
            DynamicService only = model.service();
            assertEquals(expectedIds.get(0), only.id().toString());
            assertSame(only, model.service(ShapeId.from(expectedIds.get(0))));
            collect("service():selected");
        }

        // service(id): every existing ID, case variants, an operation shape ID, a random and an absent ID.
        Set<String> queries = new LinkedHashSet<>();
        for (GenService service : services) {
            queries.add(service.id());
            queries.addAll(caseVariants(service.id()));
            service.operations().stream().findFirst().map(service::operationId).ifPresent(queries::add);
        }
        queries.add(extraServiceId);
        queries.add("zz.none#Nope");
        for (String query : queries) {
            ShapeId queryId = ShapeId.from(query);
            if (expectedIds.isEmpty()) {
                expectFailure(() -> model.service(queryId), Reason.NO_SERVICE, null, null, List.of());
                collect("service(id):NO_SERVICE");
            } else if (expectedIds.contains(query)) {
                DynamicService selected = model.service(queryId);
                assertEquals(query, selected.id().toString());
                assertSame(selected, model.service(queryId));
                assertSame(selected, model.service(ShapeId.from(query)));
                collect("service(id):selected");
            } else {
                expectFailure(() -> model.service(queryId), Reason.UNKNOWN_SERVICE, query, null, expectedIds);
                collect("service(id):UNKNOWN_SERVICE");
            }
        }

        // operation(name): exact, case-sensitive names of each service, else UNKNOWN_OPERATION listing every name.
        for (GenService generated : services) {
            DynamicService service = model.service(ShapeId.from(generated.id()));
            List<String> expectedNames = generated.operations().stream().sorted(String::compareTo).toList();
            assertEquals(expectedNames, service.operationNames());

            Set<String> names = new LinkedHashSet<>();
            for (String name : generated.operations()) {
                names.add(name);
                names.addAll(caseVariants(name));
            }
            names.add(extraOperationName);
            names.add("Nope");
            names.add("");
            for (String name : names) {
                if (expectedNames.contains(name)) {
                    DynamicAwsOperation operation = service.operation(name);
                    assertEquals(name, operation.name());
                    assertEquals(generated.operationId(name), operation.id().toString());
                    assertSame(operation, service.operation(name));
                    collect("operation:selected");
                } else {
                    expectFailure(() -> service.operation(name), Reason.UNKNOWN_OPERATION, generated.id(), name,
                                  expectedNames);
                    collect("operation:UNKNOWN_OPERATION");
                }
            }
        }

        Statistics.label(OUTCOME).coverage(coverage -> {
            for (String outcome : OUTCOMES) {
                coverage.check(outcome).count((Predicate<Integer>) count -> count > 0);
            }
        });
    }

    @Provide
    Arbitrary<List<GenService>> models() {
        Arbitrary<List<String>> operations = operationNames().list().ofMaxSize(5)
                                                             .uniqueElements(ServiceOperationSelectionPropertyTest::fold);
        return serviceIds().list().ofMaxSize(4).uniqueElements(ServiceOperationSelectionPropertyTest::fold)
                           .flatMap(ids -> operations.list().ofSize(ids.size()).map(ops -> {
                               List<GenService> services = new ArrayList<>();
                               for (int i = 0; i < ids.size(); i++) {
                                   services.add(new GenService(ids.get(i), "op" + i + ".ns", ops.get(i)));
                               }
                               return services;
                           }));
    }

    @Provide
    Arbitrary<String> serviceIds() {
        return Combinators.combine(Arbitraries.of(NAMESPACES), Arbitraries.of(SERVICE_NAMES))
                          .as((namespace, name) -> namespace + "#" + name);
    }

    @Provide
    Arbitrary<String> operationNames() {
        return Arbitraries.of(OPERATION_NAMES);
    }

    /** A minimal Smithy 2.0 JSON AST: services with a version and operations without input or output. */
    private static String jsonAst(List<GenService> services) {
        ObjectNode.Builder shapes = Node.objectNodeBuilder();
        for (GenService service : services) {
            List<Node> targets = new ArrayList<>();
            for (String operation : service.operations()) {
                targets.add(Node.objectNode().withMember("target", service.operationId(operation)));
                shapes.withMember(service.operationId(operation), Node.objectNode().withMember("type", "operation"));
            }
            shapes.withMember(service.id(), Node.objectNodeBuilder()
                                                .withMember("type", "service")
                                                .withMember("version", "2024-01-01")
                                                .withMember("operations", ArrayNode.fromNodes(targets))
                                                .build());
        }
        return Node.printJson(Node.objectNodeBuilder()
                                  .withMember("smithy", "2.0")
                                  .withMember("shapes", shapes.build())
                                  .build());
    }

    /** Upper case, lower case and first-character-swapped variants of {@code value}, which may equal it. */
    private static List<String> caseVariants(String value) {
        List<String> variants = new ArrayList<>();
        variants.add(value.toUpperCase(Locale.ROOT));
        variants.add(value.toLowerCase(Locale.ROOT));
        if (!value.isEmpty()) {
            char first = value.charAt(0);
            char swapped = Character.isUpperCase(first) ? Character.toLowerCase(first) : Character.toUpperCase(first);
            variants.add(swapped + value.substring(1));
        }
        return variants;
    }

    private static String fold(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static void collect(String outcome) {
        Statistics.label(OUTCOME).collect(outcome);
    }

    /** Asserts that {@code call} fails with exactly the oracle's diagnostic. */
    private static void expectFailure(Supplier<?> call, Reason reason, String service, String operationName,
                                      List<String> available) {
        DynamicModelException e = assertThrows(DynamicModelException.class, call::get);
        assertEquals(reason, e.reason());
        assertEquals(Optional.ofNullable(service).map(ShapeId::from), e.service());
        assertEquals(Optional.ofNullable(operationName), e.operationName());
        assertEquals(available, e.available());
        assertFalse(e.retryable());

        String message = e.getMessage();
        int from = 0;
        if (service != null) {
            from = message.indexOf(service);
            assertTrue(from >= 0, () -> "the message does not name " + service + ": " + message);
            from += service.length();
        }
        if (operationName != null) {
            int at = message.indexOf("'" + operationName + "'", from);
            assertTrue(at >= 0, () -> "the message does not name '" + operationName + "': " + message);
            from = at + operationName.length() + 2;
        }
        for (String choice : available) {
            int at = message.indexOf(choice, from);
            assertTrue(at >= 0, () -> "the message does not list " + available + " in order: " + message);
            from = at + choice.length();
        }
    }
}
