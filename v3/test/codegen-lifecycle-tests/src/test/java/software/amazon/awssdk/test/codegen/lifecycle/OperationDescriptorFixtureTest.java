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

package software.amazon.awssdk.test.codegen.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.test.codegen.lifecycle.internal.operation.CodegenLifecycleFixtureOperations;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Static operation descriptors generated for the fixture model: an operation with input and output ({@code Ping}),
 * one without input ({@code GetStatus}), a resource-bound operation with operation and service errors
 * ({@code GetWidget}) and a streaming operation ({@code Upload}).
 */
class OperationDescriptorFixtureTest {

    private static final String NAMESPACE = "software.amazon.awssdk.test.codegen.lifecycle";
    private static final ShapeId SERVICE = id("CodegenLifecycleFixture");
    private static final Path OPERATION_SOURCES = Path.of(System.getProperty("v3.fixture.generatedSourceRoot"))
                                                      .resolve(NAMESPACE.replace('.', '/') + "/internal/operation");

    @Test
    void tableListsEveryModeledOperationSortedByShapeId() {
        assertEquals(List.of(id("GetStatus"), id("GetWidget"), id("Ping"), id("Upload")),
                     CodegenLifecycleFixtureOperations.operations().stream().map(AwsOperationDescriptor::id).toList());
        assertEquals(List.of(CodegenLifecycleFixtureOperations.GET_STATUS, CodegenLifecycleFixtureOperations.GET_WIDGET,
                             CodegenLifecycleFixtureOperations.PING, CodegenLifecycleFixtureOperations.UPLOAD),
                     CodegenLifecycleFixtureOperations.operations());
        CodegenLifecycleFixtureOperations.operations().forEach(o -> assertEquals(SERVICE, o.serviceId()));
    }

    @Test
    void lookupByShapeIdReturnsTheStaticConstants() {
        for (AwsOperationDescriptor operation : CodegenLifecycleFixtureOperations.operations()) {
            assertSame(operation, CodegenLifecycleFixtureOperations.operation(operation.id()).orElseThrow());
        }
        assertSame(CodegenLifecycleFixture.pingOperation(), CodegenLifecycleFixtureOperations.PING);
        assertEquals(Optional.empty(), CodegenLifecycleFixtureOperations.operation(id("Unknown")));
        assertEquals(Optional.empty(), CodegenLifecycleFixtureOperations.operation(id("Ping$message")));
        assertEquals(Optional.empty(), CodegenLifecycleFixtureOperations.operation(SERVICE));
    }

    @Test
    void descriptorsRecordExactModeledIdentity() {
        AwsOperationDescriptor ping = CodegenLifecycleFixtureOperations.PING;
        assertEquals(id("PingInput"), ping.inputId());
        assertEquals(id("PingOutput"), ping.outputId());
        assertEquals(List.of(id("ServiceUnavailable")), ping.errorIds());

        AwsOperationDescriptor status = CodegenLifecycleFixtureOperations.GET_STATUS;
        assertEquals(ShapeId.from("smithy.api#Unit"), status.inputId());
        assertEquals(id("GetStatusOutput"), status.outputId());

        AwsOperationDescriptor widget = CodegenLifecycleFixtureOperations.GET_WIDGET;
        assertEquals(id("GetWidgetInput"), widget.inputId());
        assertEquals(List.of(id("ServiceUnavailable"), id("WidgetNotFound")), widget.errorIds());
    }

    @Test
    void tableConstantsArePublicStaticFinalDescriptors() {
        List<Field> constants = Arrays.stream(CodegenLifecycleFixtureOperations.class.getDeclaredFields())
                                      .filter(f -> Modifier.isPublic(f.getModifiers()))
                                      .toList();
        assertEquals(List.of("GET_STATUS", "GET_WIDGET", "PING", "UPLOAD"),
                     constants.stream().map(Field::getName).sorted().toList());
        for (Field constant : constants) {
            assertTrue(Modifier.isStatic(constant.getModifiers()) && Modifier.isFinal(constant.getModifiers()),
                       constant.toString());
            assertEquals(AwsOperationDescriptor.class, constant.getType());
        }
    }

    @Test
    void tableReferencesEachDescriptorInstanceExactlyOnce() throws IOException {
        String table = Files.readString(OPERATION_SOURCES.resolve("CodegenLifecycleFixtureOperations.java"));
        for (String operation : List.of("GetStatus", "GetWidget", "Ping", "Upload")) {
            String descriptorSource = Files.readString(OPERATION_SOURCES.resolve(operation + "Operation.java"));
            assertTrue(descriptorSource.contains("\nfinal class " + operation + "Operation {"), operation);
            assertTrue(descriptorSource.contains("    static final AwsOperationDescriptor INSTANCE = "), operation);
            assertEquals(1, occurrences(table, operation + "Operation.INSTANCE"), operation);
        }
        try (Stream<Path> sources = Files.list(OPERATION_SOURCES)) {
            assertEquals(5, sources.count(), "four descriptors and one table");
        }
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        for (int i = text.indexOf(token); i >= 0; i = text.indexOf(token, i + token.length())) {
            count++;
        }
        return count;
    }

    private static ShapeId id(String name) {
        return ShapeId.from(NAMESPACE + "#" + name);
    }
}
