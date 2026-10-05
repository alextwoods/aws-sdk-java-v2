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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.dynamic.model.DynamicModelException.Reason;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;

/** Loading, service selection and operation lookup (Requirements 14.1-14.7). */
class DynamicModelTest {

    private static final String WEATHER = "$version: \"2\"\n"
        + "namespace example.weather\n"
        + "@aws.protocols#awsJson1_0\n"
        + "@aws.auth#sigv4(name: \"weather\")\n"
        + "service Weather { version: \"2020-01-01\", operations: [GetThing, ListThings, Delete] }\n"
        + "operation GetThing { input := { city: String } }\n"
        + "operation ListThings {}\n"
        + "operation Delete {}\n";

    @Test
    void selectsTheOnlyServiceAndListsItsOperationsLexically() {
        DynamicModel model = TestModels.load(WEATHER);
        DynamicService service = model.service();
        assertEquals(ShapeId.from("example.weather#Weather"), service.id());
        assertEquals(List.of(service.id()), model.serviceIds());
        assertEquals(List.of("Delete", "GetThing", "ListThings"), service.operationNames());
        assertEquals(service.operationNames(), List.copyOf(service.operations().keySet()));
        assertSame(service, model.service(service.id()), "each service is converted once");
    }

    @Test
    void resolvesExactOperationNamesOnly() {
        DynamicService service = TestModels.load(WEATHER).service();
        DynamicAwsOperation operation = service.operation("GetThing");
        assertEquals("GetThing", operation.name());
        assertEquals(ShapeId.from("example.weather#GetThing"), operation.id());
        assertSame(operation, service.operation("GetThing"));

        DynamicModelException e = assertThrows(DynamicModelException.class, () -> service.operation("getThing"));
        assertEquals(Reason.UNKNOWN_OPERATION, e.reason());
        assertEquals(Optional.of(service.id()), e.service());
        assertEquals(Optional.of("getThing"), e.operationName());
        assertEquals(List.of("Delete", "GetThing", "ListThings"), e.available());
        assertEquals("Service example.weather#Weather has no operation named 'getThing'; available operations: "
                     + "Delete, GetThing, ListThings", e.getMessage());
        assertFalse(e.retryable());
    }

    @Test
    void rejectsAModelWithoutAService() {
        DynamicModel model = TestModels.load("$version: \"2\"\nnamespace example.empty\nstring Name\n");
        assertEquals(List.of(), model.serviceIds());
        assertEquals(Reason.NO_SERVICE, assertThrows(DynamicModelException.class, model::service).reason());
        assertEquals(Reason.NO_SERVICE, assertThrows(DynamicModelException.class,
                                                     () -> model.service(ShapeId.from("a#B"))).reason());
    }

    @Test
    void requiresAnExactServiceIdWhenTheModelHasSeveralServices() {
        // Lexical (code point) order: upper case before lower case, unlike ShapeId's natural order.
        DynamicModel model = TestModels.load("$version: \"2\"\nnamespace example.multi\n"
                                             + "service Zeta {}\nservice alpha {}\nservice Beta {}\n");
        List<String> sorted = List.of("example.multi#Beta", "example.multi#Zeta", "example.multi#alpha");
        assertEquals(sorted, model.serviceIds().stream().map(ShapeId::toString).toList());

        DynamicModelException ambiguous = assertThrows(DynamicModelException.class, model::service);
        assertEquals(Reason.AMBIGUOUS_SERVICE, ambiguous.reason());
        assertEquals(sorted, ambiguous.available());
        assertTrue(ambiguous.getMessage().endsWith(String.join(", ", sorted)), ambiguous.getMessage());

        assertEquals(ShapeId.from("example.multi#Zeta"), model.service(ShapeId.from("example.multi#Zeta")).id());
        assertEquals(List.of(), model.service(ShapeId.from("example.multi#alpha")).operationNames());

        ShapeId missing = ShapeId.from("example.multi#ALPHA");
        DynamicModelException unknown = assertThrows(DynamicModelException.class, () -> model.service(missing));
        assertEquals(Reason.UNKNOWN_SERVICE, unknown.reason());
        assertEquals(Optional.of(missing), unknown.service());
        assertEquals(sorted, unknown.available());
        ShapeId notAService = ShapeId.from("example.multi#Zeta$member");
        assertEquals(Reason.UNKNOWN_SERVICE, assertThrows(DynamicModelException.class,
                                                          () -> model.service(notAService)).reason());
    }

    @Test
    void preservesUnknownTraitsInTheRuntimeModel() {
        DynamicModel model = TestModels.load("$version: \"2\"\nnamespace example.t\n"
                                             + "@a.b#c({x: [1, \"two\", null]})\n@d#e(1.5)\nstring Name\n");
        var name = model.model().expectShape(ShapeId.from("example.t#Name"));
        DynamicTrait first = (DynamicTrait) name.findTrait(ShapeId.from("a.b#c")).orElseThrow();
        assertEquals(TestModels.node("{\"x\": [1, \"two\", null]}"), first.toNode());
        assertEquals(TestModels.node("1.5"), name.findTrait(ShapeId.from("d#e")).orElseThrow().toNode());
    }

    @Test
    void loadsAJsonAstFile(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("weather.json");
        Files.writeString(file, TestModels.jsonAst(WEATHER), StandardCharsets.UTF_8);
        assertEquals(3, DynamicModel.load(file).service().operations().size());
    }

    @Test
    void rejectsInputThatIsNotAValidSmithyJsonAst() {
        DynamicModelException notJson = assertThrows(DynamicModelException.class,
                                                     () -> DynamicModel.fromJsonAst(WEATHER, "weather.smithy"));
        assertEquals(Reason.INVALID_MODEL, notJson.reason());
        assertTrue(notJson.getMessage().startsWith("Invalid runtime model weather.smithy: "), notJson.getMessage());

        assertEquals(Reason.INVALID_MODEL, assertThrows(DynamicModelException.class,
            () -> DynamicModel.fromJsonAst("[1, 2]", "array.json")).reason());

        DynamicModelException broken = assertThrows(DynamicModelException.class, () -> DynamicModel.fromJsonAst(
            "{\"smithy\": \"2.0\", \"shapes\": {\"example.b#S\": {\"type\": \"structure\", \"members\": "
            + "{\"m\": {\"target\": \"example.b#Missing\"}}}}}", "broken.json"));
        assertEquals(Reason.INVALID_MODEL, broken.reason());
        assertTrue(broken.getMessage().contains("example.b#Missing"), broken.getMessage());
    }
}
