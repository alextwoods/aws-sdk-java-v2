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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Static operation descriptor and operation table emission (Requirements 3.2, 3.8, 3.9, 10.12).
 */
class OperationDescriptorGenerationTest {

    private static final String SERVICE = "example.weather#Weather";
    private static final String PACKAGE = "software.amazon.awssdk.services.weather";
    private static final Path OPERATIONS = Path.of("software/amazon/awssdk/services/weather/internal/operation");
    private static final Path TABLE = OPERATIONS.resolve("WeatherOperations.java");
    private static final Path MODEL = Path.of("software/amazon/awssdk/services/weather/model");

    private static final String WEATHER = """
        $version: "2"
        namespace example.weather

        @aws.protocols#awsJson1_0
        @aws.auth#sigv4(name: "weather")
        service Weather {
            version: "2006-03-01"
            operations: [GetForecast, GetStatus, example.other#DescribeRegion]
            resources: [City]
            errors: [Throttled]
        }

        operation GetForecast {
            input := { city: String }
            output := { summary: String }
        }

        @readonly
        operation GetStatus {
            output := { status: String }
        }

        resource City {
            identifiers: { cityId: String }
            read: GetCity
        }

        @readonly
        operation GetCity {
            input := { @required cityId: String }
            output := { @required cityId: String }
            errors: [NoSuchCity]
        }

        @error("client")
        structure NoSuchCity {}

        @error("server")
        structure Throttled {}
        """;

    private static final String OTHER = """
        $version: "2"
        namespace example.other

        /// Bound from another namespace, with neither input nor output.
        operation DescribeRegion {}
        """;

    @TempDir
    Path temp;

    @Test
    void emitsOneDescriptorPerOperationAndTheTable() throws IOException {
        AwsCodegenResult result = generate();
        assertEquals(List.of(Path.of("software/amazon/awssdk/services/weather/GeneratedServiceInfo.java"),
                             Path.of("software/amazon/awssdk/services/weather/WeatherServiceMetadata.java"),
                             OPERATIONS.resolve("DescribeRegionOperation.java"),
                             OPERATIONS.resolve("GetCityOperation.java"),
                             OPERATIONS.resolve("GetForecastOperation.java"),
                             OPERATIONS.resolve("GetStatusOperation.java"),
                             TABLE,
                             MODEL.resolve("GetCityRequest.java"),
                             MODEL.resolve("GetCityResponse.java"),
                             MODEL.resolve("GetForecastRequest.java"),
                             MODEL.resolve("GetForecastResponse.java"),
                             MODEL.resolve("GetStatusResponse.java"),
                             MODEL.resolve("NoSuchCityException.java"),
                             MODEL.resolve("ThrottledException.java"),
                             MODEL.resolve("WeatherException.java"),
                             MODEL.resolve("WeatherResponse.java"),
                             MODEL.resolve("WeatherResponseMetadata.java"),
                             MODEL.resolve("internal/WeatherSchemas.java"),
                             MODEL.resolve("internal/WeatherTypeRegistry.java")),
                     ClientSources.withoutClients(result.generatedFiles()));
    }

    @Test
    void descriptorIsAStaticConstantLinkedToItsSchemas() throws IOException {
        AwsCodegenResult result = generate();
        assertEquals(String.join("\n",
                                 "/*",
                                 " * Generated by software.amazon.awssdk:aws-codegen. Do not edit.",
                                 " */",
                                 "",
                                 "package " + PACKAGE + ".internal.operation;",
                                 "",
                                 "import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;",
                                 "import software.amazon.awssdk.services.weather.model.internal.WeatherSchemas;",
                                 "import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;",
                                 "import software.amazon.smithy.model.shapes.ShapeId;",
                                 "",
                                 "/**",
                                 " * Static descriptor of the {@code example.weather#GetCity} operation.",
                                 " *",
                                 " * <p>It is reached only through a direct static reference from the service's "
                                 + "operation table.",
                                 " */",
                                 "final class GetCityOperation {",
                                 "    static final AwsOperationDescriptor INSTANCE = AwsOperationDescriptor.builder()",
                                 "            .id(ShapeId.from(\"example.weather#GetCity\"))",
                                 "            .service(ShapeId.from(\"example.weather#Weather\"))",
                                 "            .input(ShapeId.from(\"example.weather#GetCityInput\"))",
                                 "            .output(ShapeId.from(\"example.weather#GetCityOutput\"))",
                                 "            .errors(ShapeId.from(\"example.weather#NoSuchCity\"))",
                                 "            .errors(ShapeId.from(\"example.weather#Throttled\"))",
                                 "            .schemas(OperationSchemas.builder(WeatherSchemas.GET_CITY)",
                                 "                    .input(WeatherSchemas.GET_CITY_INPUT)",
                                 "                    .output(WeatherSchemas.GET_CITY_OUTPUT)",
                                 "                    .errors(WeatherSchemas.NO_SUCH_CITY)",
                                 "                    .errors(WeatherSchemas.THROTTLED)",
                                 "                    .build())",
                                 "            .build();",
                                 "",
                                 "    private GetCityOperation() {",
                                 "    }",
                                 "}",
                                 ""),
                     read(result, OPERATIONS.resolve("GetCityOperation.java")));

        String status = read(result, OPERATIONS.resolve("GetStatusOperation.java"));
        assertTrue(status.contains(".input(ShapeId.from(\"smithy.api#Unit\"))"), status);
        String region = read(result, OPERATIONS.resolve("DescribeRegionOperation.java"));
        assertTrue(region.contains(".id(ShapeId.from(\"example.other#DescribeRegion\"))"), region);
        assertTrue(region.contains(".input(ShapeId.from(\"smithy.api#Unit\"))"), region);
        assertTrue(region.contains(".output(ShapeId.from(\"smithy.api#Unit\"))"), region);
        assertTrue(region.contains(".errors(ShapeId.from(\"example.weather#Throttled\"))"), region);
        // Operations without input or output link Smithy Java's Unit schema; an operation from another namespace
        // links the schema constant of its contextual name.
        assertTrue(region.contains(".schemas(OperationSchemas.builder(WeatherSchemas.DESCRIBE_REGION)\n"
                                   + "                    .input(Unit.SCHEMA)\n"
                                   + "                    .output(Unit.SCHEMA)\n"), region);
        assertTrue(region.contains("import software.amazon.smithy.java.core.schema.Unit;\n"), region);
        assertTrue(status.contains("                    .input(Unit.SCHEMA)\n"
                                   + "                    .output(WeatherSchemas.GET_STATUS_OUTPUT)\n"), status);
    }

    @Test
    void tableReferencesEveryDescriptorInstanceDirectly() throws IOException {
        AwsCodegenResult result = generate();
        String n = "\n";
        assertEquals("/*" + n
                     + " * Generated by software.amazon.awssdk:aws-codegen. Do not edit." + n
                     + " */" + n
                     + n
                     + "package " + PACKAGE + ".internal.operation;" + n
                     + n
                     + "import java.util.List;" + n
                     + "import java.util.Optional;" + n
                     + "import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;" + n
                     + "import software.amazon.smithy.model.shapes.ShapeId;" + n
                     + n
                     + "/**" + n
                     + " * Static operation table of the {@code example.weather#Weather} service." + n
                     + " *" + n
                     + " * <p>Each constant is a direct static reference to the descriptor of one operation in the "
                     + "service" + n
                     + " * closure. Operations are resolved only through these references." + n
                     + " */" + n
                     + "public final class WeatherOperations {" + n
                     + "    /** Descriptor of {@code example.other#DescribeRegion}. */" + n
                     + "    public static final AwsOperationDescriptor DESCRIBE_REGION = "
                     + "DescribeRegionOperation.INSTANCE;" + n
                     + n
                     + "    /** Descriptor of {@code example.weather#GetCity}. */" + n
                     + "    public static final AwsOperationDescriptor GET_CITY = GetCityOperation.INSTANCE;" + n
                     + n
                     + "    /** Descriptor of {@code example.weather#GetForecast}. */" + n
                     + "    public static final AwsOperationDescriptor GET_FORECAST = GetForecastOperation.INSTANCE;" + n
                     + n
                     + "    /** Descriptor of {@code example.weather#GetStatus}. */" + n
                     + "    public static final AwsOperationDescriptor GET_STATUS = GetStatusOperation.INSTANCE;" + n
                     + n
                     + "    private static final List<AwsOperationDescriptor> operations = List.of(" + n
                     + "            DESCRIBE_REGION," + n
                     + "            GET_CITY," + n
                     + "            GET_FORECAST," + n
                     + "            GET_STATUS);" + n
                     + n
                     + "    private WeatherOperations() {" + n
                     + "    }" + n
                     + n
                     + "    /** Every operation of the service, sorted by absolute shape ID. */" + n
                     + "    public static List<AwsOperationDescriptor> operations() {" + n
                     + "        return operations;" + n
                     + "    }" + n
                     + n
                     + "    /** The operation of this service with exactly the absolute shape ID {@code id}. */" + n
                     + "    public static Optional<AwsOperationDescriptor> operation(ShapeId id) {" + n
                     + "        return switch (id.toString()) {" + n
                     + "            case \"example.other#DescribeRegion\" -> Optional.of(DESCRIBE_REGION);" + n
                     + "            case \"example.weather#GetCity\" -> Optional.of(GET_CITY);" + n
                     + "            case \"example.weather#GetForecast\" -> Optional.of(GET_FORECAST);" + n
                     + "            case \"example.weather#GetStatus\" -> Optional.of(GET_STATUS);" + n
                     + "            default -> Optional.empty();" + n
                     + "        };" + n
                     + "    }" + n
                     + "}" + n,
                     read(result, TABLE));
    }

    /**
     * Oracle: the operations {@link TopDownIndex} finds in an independently assembled model. Every one of them has
     * exactly one descriptor file and exactly one static {@code INSTANCE} reference in the table, and the table
     * references nothing else.
     */
    @Test
    void everyModeledOperationHasExactlyOneStaticDescriptorReference() throws IOException {
        AwsCodegenResult result = generate();
        Model model = Model.assembler().discoverModels(getClass().getClassLoader())
                           .addImport(temp.resolve("model/weather.smithy"))
                           .addImport(temp.resolve("model/other.smithy")).assemble().unwrap();
        ServiceShape service = model.expectShape(ShapeId.from(SERVICE), ServiceShape.class);
        Set<OperationShape> operations = TopDownIndex.of(model).getContainedOperations(service);
        assertEquals(4, operations.size());

        String table = read(result, TABLE);
        Set<String> descriptorFiles = new TreeSet<>();
        for (OperationShape operation : operations) {
            String descriptor = operation.getId().getName(service) + "Operation";
            descriptorFiles.add(descriptor + ".java");
            assertEquals(1, occurrences(table, " = " + descriptor + ".INSTANCE;"), descriptor);
            String source = read(result, OPERATIONS.resolve(descriptor + ".java"));
            assertEquals(1, occurrences(source, ".id(ShapeId.from(\"" + operation.getId() + "\"))"), descriptor);
            assertEquals(1, occurrences(source, "static final AwsOperationDescriptor INSTANCE = "), descriptor);
        }
        assertEquals(operations.size(), occurrences(table, ".INSTANCE"));
        Set<String> emitted = new TreeSet<>();
        result.generatedFiles().stream().filter(p -> p.startsWith(OPERATIONS)).map(p -> p.getFileName().toString())
              .filter(name -> !name.equals("WeatherOperations.java")).forEach(emitted::add);
        assertEquals(descriptorFiles, emitted);
    }

    @Test
    void serviceWithoutOperationsGetsAnEmptyTable() throws IOException {
        Path models = Files.createDirectories(temp.resolve("empty"));
        Files.writeString(models.resolve("empty.smithy"),
                          "$version: \"2\"\nnamespace example.empty\n@aws.protocols#awsJson1_0\n"
                          + "@aws.auth#sigv4(name: \"empty\")\nservice Empty { version: \"1\" }\n");
        AwsCodegenResult result = AwsCodegen.generate(
            new AwsCodegenRequest("example.empty#Empty", "software.amazon.awssdk.services.empty", models,
                                  temp.resolve("empty-out")), List.of());
        Path table = Path.of("software/amazon/awssdk/services/empty/internal/operation/EmptyOperations.java");
        assertEquals(List.of(Path.of("software/amazon/awssdk/services/empty/EmptyServiceMetadata.java"),
                             Path.of("software/amazon/awssdk/services/empty/GeneratedServiceInfo.java"), table,
                             Path.of("software/amazon/awssdk/services/empty/model/EmptyException.java"),
                             Path.of("software/amazon/awssdk/services/empty/model/EmptyResponse.java"),
                             Path.of("software/amazon/awssdk/services/empty/model/EmptyResponseMetadata.java"),
                             Path.of("software/amazon/awssdk/services/empty/model/internal/EmptySchemas.java"),
                             Path.of("software/amazon/awssdk/services/empty/model/internal/EmptyTypeRegistry.java")),
                     ClientSources.withoutClients(result.generatedFiles()));
        String source = read(result, table);
        assertTrue(source.contains("    private static final List<AwsOperationDescriptor> operations = List.of();\n"),
                   source);
        assertTrue(source.contains("        return switch (id.toString()) {\n"
                                   + "            default -> Optional.empty();\n"
                                   + "        };\n"), source);
    }

    @Test
    void constantNamesFollowJavaConstantConventions() {
        assertEquals("GET_OBJECT", OperationTablePlan.constantName("GetObject"));
        assertEquals("LIST_OBJECTS_V2", OperationTablePlan.constantName("ListObjectsV2"));
        assertEquals("PUT_BUCKET_ACL", OperationTablePlan.constantName("PutBucketAcl"));
        assertEquals("_PRIVATE", OperationTablePlan.constantName("_Private"));
        assertThrows(AwsCodegenException.class, () -> OperationTablePlan.constantName("9Lives"));
    }

    private AwsCodegenResult generate() throws IOException {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("weather.smithy"), WEATHER, StandardCharsets.UTF_8);
        Files.writeString(models.resolve("other.smithy"), OTHER, StandardCharsets.UTF_8);
        return AwsCodegen.generate(new AwsCodegenRequest(SERVICE, PACKAGE, models, temp.resolve("generated")),
                                   List.of());
    }

    private static String read(AwsCodegenResult result, Path relative) throws IOException {
        return Files.readString(result.sourceRoot().resolve(relative));
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        for (int i = text.indexOf(token); i >= 0; i = text.indexOf(token, i + token.length())) {
            count++;
        }
        return count;
    }
}
