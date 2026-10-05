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

package software.amazon.awssdk.awscodegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.awssdk.awscodegen.validation.DiagnosticCode;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;

/**
 * {@link DiagnosticCode#GENERATED_NAME_COLLISION}: shapes whose generated Java types would share a name or a source
 * file, compared case-insensitively, fail generation with one diagnostic per collision naming every shape and the
 * type, before anything is staged or published.
 */
class GeneratedNameCollisionsTest {
    private static final String NAMESPACE = "example.fleet";
    private static final String SERVICE = NAMESPACE + "#Fleet";
    private static final String PACKAGE = "software.amazon.awssdk.services.fleet";
    private static final String MODEL_PACKAGE = PACKAGE + ".model.";

    @TempDir
    Path temp;

    /** The minimal counterexample of Property 1: two errors with the same V2 exception name. */
    @Test
    void faultAndExceptionErrorsOfOneOperationCollide() throws IOException {
        Path out = temp.resolve("out");
        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> generate(
            "operations: [Get]", """
                operation Get {
                    input := {}
                    output := {}
                    errors: [ClusterFault, ClusterException]
                }
                @error("server")
                structure ClusterFault {}
                @error("client")
                structure ClusterException {}
                """, out, List.of()));

        Diagnostic diagnostic = single(e);
        assertEquals(SERVICE, diagnostic.service());
        assertEquals(NAMESPACE + "#ClusterException", diagnostic.shape().orElseThrow());
        assertEquals(List.of(AwsCodegen.GENERATOR_IDENTIFIER), diagnostic.integrations());
        assertEquals("type " + MODEL_PACKAGE + "ClusterException", diagnostic.element());
        assertTrue(diagnostic.message().contains(NAMESPACE + "#ClusterException and " + NAMESPACE + "#ClusterFault"),
                   diagnostic.message());
        assertTrue(e.getMessage().contains(diagnostic.render()), e.getMessage());
        assertNothingPublished(out);
    }

    static Stream<Arguments> collisions() {
        return Stream.of(
            Arguments.of("an error and a structure named like its exception", "Widget", "WidgetException",
                         "WidgetException", """
                             operation Get {
                                 input := { a: WidgetException }
                                 output := {}
                                 errors: [Widget]
                             }
                             @error("client")
                             structure Widget {}
                             structure WidgetException {}
                             """),
            Arguments.of("an operation input and a structure with its V2 request name", "GetInput", "GetRequest",
                         "GetRequest", """
                             operation Get {
                                 input: GetInput
                                 output := { r: GetRequest }
                             }
                             @input
                             structure GetInput {}
                             structure GetRequest {}
                             """),
            Arguments.of("an operation output and a union with its V2 response name", "GetOutput", "GetResponse",
                         "GetResponse", """
                             operation Get {
                                 input := { u: GetResponse }
                                 output: GetOutput
                             }
                             @output
                             structure GetOutput {}
                             union GetResponse { a: String }
                             """),
            Arguments.of("an error and an enum named like its exception", "ThingException", "Thing",
                         "ThingException", """
                             operation Get {
                                 input := { e: ThingException }
                                 output := {}
                                 errors: [Thing]
                             }
                             @error("client")
                             structure Thing {}
                             enum ThingException { A }
                             """),
            Arguments.of("names that differ only in case", "Thing", "Thingexception", "ThingException", """
                             operation Get {
                                 input := { s: Thingexception }
                                 output := {}
                                 errors: [Thing]
                             }
                             @error("client")
                             structure Thing {}
                             structure Thingexception {}
                             """),
            Arguments.of("a structure named like the service's base exception", "FleetException", "Fleet",
                         "FleetException", """
                             operation Get {
                                 input := { s: FleetException }
                                 output := {}
                             }
                             structure FleetException {}
                             """));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("collisions")
    void everyGeneratedTypeKindIsChecked(String description, String first, String second, String javaType,
                                         String shapes) throws IOException {
        Path out = temp.resolve("out");
        AwsCodegenException e = assertThrows(AwsCodegenException.class,
                                             () -> generate("operations: [Get]", shapes, out, List.of()));

        Diagnostic diagnostic = single(e);
        String lowest = (first.compareTo(second) < 0 ? first : second);
        assertEquals(NAMESPACE + "#" + lowest, diagnostic.shape().orElseThrow());
        assertEquals(List.of(AwsCodegen.GENERATOR_IDENTIFIER), diagnostic.integrations());
        assertTrue(diagnostic.element().equalsIgnoreCase("type " + MODEL_PACKAGE + javaType), diagnostic.element());
        assertTrue(diagnostic.message().contains(NAMESPACE + "#" + first), diagnostic.message());
        assertTrue(diagnostic.message().contains(NAMESPACE + "#" + second), diagnostic.message());
        assertTrue(diagnostic.message().contains(MODEL_PACKAGE + javaType), diagnostic.message());
        assertNothingPublished(out);
    }

    @Test
    void threeCollidingShapesAreOneDiagnostic() throws IOException {
        Path out = temp.resolve("out");
        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> generate(
            "operations: [Get]\n    errors: [Node, NodeFault, NodeException]", """
                operation Get {
                    input := {}
                    output := {}
                }
                @error("client")
                structure Node {}
                @error("client")
                structure NodeFault {}
                @error("client")
                structure NodeException {}
                """, out, List.of()));

        Diagnostic diagnostic = single(e);
        assertEquals(NAMESPACE + "#Node", diagnostic.shape().orElseThrow());
        assertTrue(diagnostic.message().contains(NAMESPACE + "#Node and " + NAMESPACE + "#NodeException and "
                                                 + NAMESPACE + "#NodeFault"), diagnostic.message());
        assertNothingPublished(out);
    }

    /** A rename by an integration's symbol decorator is checked on the final symbol provider. */
    @Test
    void integrationRenamesAreCheckedAndNameTheIntegration() throws IOException {
        Path out = temp.resolve("out");
        String shapes = """
            operation Get {
                input := { a: Alpha, b: Beta }
                output := {}
            }
            structure Alpha {}
            structure Beta {}
            """;
        generate("operations: [Get]", shapes, temp.resolve("unrenamed"), List.of());

        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> generate(
            "operations: [Get]", shapes, out, List.of(new Rename("test.rename", NAMESPACE + "#Alpha", "Beta"))));

        Diagnostic diagnostic = single(e);
        assertEquals(NAMESPACE + "#Alpha", diagnostic.shape().orElseThrow());
        assertEquals(List.of(AwsCodegen.GENERATOR_IDENTIFIER, "test.rename"), diagnostic.integrations());
        assertEquals("type " + MODEL_PACKAGE + "Beta", diagnostic.element());
        assertTrue(diagnostic.message().contains(NAMESPACE + "#Alpha and " + NAMESPACE + "#Beta"),
                   diagnostic.message());
        assertNothingPublished(out);
    }

    @Test
    void distinctNamesGenerate() throws IOException {
        AwsCodegenResult result = generate("operations: [Get]", """
            operation Get {
                input := { s: ClusterSummary }
                output := {}
                errors: [ClusterFault, ClusterError]
            }
            @error("server")
            structure ClusterFault {}
            @error("client")
            structure ClusterError {}
            structure ClusterSummary {}
            """, temp.resolve("out"), List.of());
        assertTrue(result.generatedFiles().contains(Path.of(PACKAGE.replace('.', '/'), "model",
                                                            "ClusterException.java")));
        assertTrue(result.generatedFiles().contains(Path.of(PACKAGE.replace('.', '/'), "model",
                                                            "ClusterErrorException.java")));
    }

    private static Diagnostic single(AwsCodegenException e) {
        assertEquals(List.of(DiagnosticCode.GENERATED_NAME_COLLISION),
                     e.diagnostics().stream().map(Diagnostic::code).toList(), e.getMessage());
        assertTrue(e.getMessage().startsWith("Code generation of service " + SERVICE + " would generate colliding "
                                             + "Java types (1 diagnostic(s)); no Generated Service Package artifact "
                                             + "was produced:"), e.getMessage());
        return e.diagnostics().get(0);
    }

    private void assertNothingPublished(Path out) throws IOException {
        assertFalse(Files.exists(out), "no source tree is published");
        assertFalse(Files.exists(out.resolveSibling(out.getFileName() + "-resources")), "no resource tree");
        try (Stream<Path> children = Files.list(temp)) {
            assertTrue(children.map(p -> p.getFileName().toString())
                               .allMatch(name -> name.equals("model") || name.startsWith("unrenamed")),
                       "no staging directory remains");
        }
    }

    private AwsCodegenResult generate(String serviceBody, String shapes, Path out,
                                      List<AwsCodegenIntegration> integrations) throws IOException {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("fleet.smithy"), """
            $version: "2"
            namespace example.fleet

            @aws.protocols#awsJson1_0
            @aws.auth#sigv4(name: "fleet")
            service Fleet {
                version: "1"
                %s
            }

            %s
            """.formatted(serviceBody, shapes), StandardCharsets.UTF_8);
        return AwsCodegen.generate(new AwsCodegenRequest(SERVICE, PACKAGE, models, out), integrations);
    }

    /** Renames one shape's generated type within the model package. */
    private record Rename(String identifier, String target, String newName) implements AwsCodegenIntegration {
        @Override
        public SymbolProvider decorateSymbolProvider(Model model, AwsCodegenSettings settings,
                                                     SymbolProvider delegate) {
            return new SymbolProvider() {
                @Override
                public Symbol toSymbol(Shape shape) {
                    Symbol symbol = delegate.toSymbol(shape);
                    if (!shape.getId().toString().equals(target)) {
                        return symbol;
                    }
                    String namespace = PACKAGE + ".model";
                    return symbol.toBuilder().namespace(namespace, ".").name(newName)
                                 .definitionFile(namespace.replace('.', '/') + "/" + newName + ".java").build();
                }

                @Override
                public String toMemberName(MemberShape member) {
                    return delegate.toMemberName(member);
                }
            };
        }
    }
}
