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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GeneratedSourceGuardTest {

    private static final String SERVICE = "example.weather#Weather";
    private static final String PACKAGE = "software.amazon.awssdk.services.weather";

    @TempDir
    Path temp;

    @ParameterizedTest
    @ValueSource(strings = {
        "Class<?> c = Class.forName(\"example.GetForecastOperation\");",
        "ClassLoader loader = Thread.currentThread().getContextClassLoader();",
        "Object o = loader.loadClass(name);",
        "ServiceLoader.load(Object.class).findFirst();",
        "java.lang.reflect.Method m = null;",
        "type.getDeclaredMethod(\"instance\");",
        "type.getDeclaredField(\"INSTANCE\");",
        "type.getMethod(\"builder\");",
        "type.getField(\"INSTANCE\");",
        "type.getConstructor();",
        "type.getDeclaredConstructors();",
        "type.newInstance();",
        "var lookup = MethodHandles.lookup();",
        "// Resolved with ModelAssembler at runtime",
        "Model.assembler().discoverModels();",
        "import software.amazon.smithy.model.loader.ModelAssembler;"
    })
    void rejectsDynamicResolutionAndModelLoadingInJavaSources(String line) {
        List<String> violations = GeneratedSourceGuard.violations("a/B.java", "class B {\n" + line + "\n}\n");
        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.get(0).startsWith("a/B.java:2: "), violations.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "static final AwsOperationDescriptor INSTANCE = AwsOperationDescriptor.builder().build();",
        "public static final AwsOperationDescriptor GET = GetOperation.INSTANCE;",
        "String method = request.method();",
        "Optional<AwsOperationDescriptor> operation = WeatherOperations.operation(id);",
        "String className = \"MyClass\";"
    })
    void acceptsStaticReferences(String line) {
        assertEquals(List.of(), GeneratedSourceGuard.violations("a/B.java", "class B {\n" + line + "\n}\n"));
    }

    @Test
    void rejectsSerializedSmithyModels() {
        assertEquals(List.of("META-INF/model.json: serialized Smithy model (JSON AST)"),
                     GeneratedSourceGuard.violations("META-INF/model.json",
                                                     "{\n  \"smithy\": \"2.0\",\n  \"shapes\": {}\n}\n"));
        assertEquals(List.of("META-INF/model.smithy: serialized Smithy model (IDL)"),
                     GeneratedSourceGuard.violations("META-INF/model.smithy", "$version: \"2\"\n"));
        assertEquals(List.of(), GeneratedSourceGuard.violations("META-INF/service.json",
                                                                "{\"serviceId\": \"example.weather#Weather\"}"));
    }

    @Test
    void generatedTreePassesTheGuard() throws IOException {
        AwsCodegenResult result = AwsCodegen.generate(request(temp.resolve("generated")), List.of());
        for (Path file : result.generatedFiles()) {
            assertEquals(List.of(), GeneratedSourceGuard.violations(
                StagedFileManifest.portable(file), Files.readString(result.sourceRoot().resolve(file))));
        }
    }

    @Test
    void integrationWritingAClassNameLookupFailsGenerationAndPublishesNothing() throws IOException {
        Path output = temp.resolve("generated");
        Consumer<AwsCodegenContext> reflective = context -> context.writerDelegator().useFileWriter(
            "software/amazon/awssdk/services/weather/Lookup.java", PACKAGE,
            writer -> writer.write("final class Lookup {\n    Object o = Class.forName(\"x.Y\");\n}"));
        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> AwsCodegen.generate(
            request(output), List.of(new Writing("aws.reflective", reflective))));
        assertTrue(e.getMessage().contains("software/amazon/awssdk/services/weather/Lookup.java:")
                   && e.getMessage().contains("class-name lookup"), e.getMessage());
        assertFalse(Files.exists(output));
    }

    @Test
    void integrationEmbeddingTheModelFailsGeneration() {
        Consumer<AwsCodegenContext> embed = context -> context.fileManifest().writeFile(
            "META-INF/aws-sdk-java-v3/model.json", "{\"smithy\": \"2.0\", \"shapes\": {}}");
        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> AwsCodegen.generate(
            request(temp.resolve("generated")), List.of(new Writing("aws.embed", embed))));
        assertTrue(e.getMessage().contains("META-INF/aws-sdk-java-v3/model.json: serialized Smithy model"),
                   e.getMessage());
    }

    private AwsCodegenRequest request(Path output) throws IOException {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("weather.smithy"),
                          "$version: \"2\"\nnamespace example.weather\n"
                          + "@aws.protocols#awsJson1_0\n@aws.auth#sigv4(name: \"weather\")\n"
                          + "service Weather { version: \"2006-03-01\", operations: [GetForecast] }\n"
                          + "operation GetForecast { input := { city: String } }\n");
        return new AwsCodegenRequest(SERVICE, PACKAGE, models, output);
    }

    private record Writing(String identifier, Consumer<AwsCodegenContext> onCustomize)
        implements AwsCodegenIntegration {

        @Override
        public void customize(AwsCodegenContext context) {
            onCustomize.accept(context);
        }
    }
}
