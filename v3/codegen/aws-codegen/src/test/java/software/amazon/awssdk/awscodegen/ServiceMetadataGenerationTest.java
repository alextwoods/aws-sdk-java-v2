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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Generated service metadata and primary type registry (task 3.3; Requirements 4.6, 6.5-6.7, 6.9): golden files for
 * the schema test model (a recursive structure, a union, an enum and an intEnum, operation and service errors), the
 * registry inventory, lazy partitions of large registries, and the exclusion of non-modeled request state. Runtime
 * lookups are covered by the lifecycle fixture's {@code ServiceMetadataFixtureTest}.
 */
class ServiceMetadataGenerationTest {

    private static final String SERVICE = "example.weather#Weather";
    private static final String PACKAGE = "software.amazon.awssdk.services.weather";
    private static final Path ROOT = Path.of("software/amazon/awssdk/services/weather");
    private static final Pattern REGISTERED = Pattern.compile(
        "RegisteredType\\.of\\((\\w+)\\.(\\w+), (\\w+)\\.class, (\\w+)::builder\\)");

    @TempDir
    static Path temp;

    private static AwsCodegenResult result;

    private static synchronized AwsCodegenResult result() throws IOException {
        if (result == null) {
            Path models = Files.createDirectories(temp.resolve("model"));
            Files.writeString(models.resolve("weather.smithy"),
                              resource("/software/amazon/awssdk/awscodegen/schema/weather.smithy"));
            result = AwsCodegen.generate(new AwsCodegenRequest(SERVICE, PACKAGE, models, temp.resolve("generated")),
                                         List.of());
        }
        return result;
    }

    static Stream<Arguments> goldens() {
        return Stream.of(Arguments.of("WeatherServiceMetadata.java", ROOT),
                         Arguments.of("WeatherTypeRegistry.java", ROOT.resolve("model/internal")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldens")
    void generatedFileMatchesGolden(String file, Path directory) throws IOException {
        String actual = Files.readString(result().sourceRoot().resolve(directory.resolve(file)));
        String expected = resource(file + ".golden");
        if (!expected.equals(actual)) {
            Path written = Path.of("target", "golden-actual", file + ".golden");
            Files.createDirectories(written.getParent());
            Files.writeString(written, actual);
            assertEquals(expected, actual, "Generated " + file + " differs from the golden file; actual output: "
                                           + written);
        }
    }

    @Test
    void registryHasOneEntryPerStructureUnionAndErrorWithItsOwnSchemaClassAndBuilder() throws IOException {
        String registry = Files.readString(result().sourceRoot().resolve(ROOT.resolve(
            "model/internal/WeatherTypeRegistry.java")));
        List<String> types = new ArrayList<>();
        Matcher matcher = REGISTERED.matcher(registry);
        while (matcher.find()) {
            assertEquals("WeatherSchemas", matcher.group(1));
            assertEquals(matcher.group(3), matcher.group(4), "class and builder factory of one type");
            types.add(matcher.group(3) + "=" + matcher.group(2));
        }
        // Enums and intEnums (TemperatureUnit, Level) have no builder and are not registered; Unit is prelude.
        assertEquals(List.of("Day=DAY", "Forecast=FORECAST", "GetForecastRequest=GET_FORECAST_INPUT",
                             "GetForecastResponse=GET_FORECAST_OUTPUT", "NoSuchCityException=NO_SUCH_CITY",
                             "RainDetail=RAIN_DETAIL", "ThrottledException=THROTTLED"),
                     types);
    }

    @Test
    void registryIsASeparateClassFromTheOperationTable() throws IOException {
        String table = Files.readString(result().sourceRoot().resolve(ROOT.resolve(
            "internal/operation/WeatherOperations.java")));
        String registry = Files.readString(result().sourceRoot().resolve(ROOT.resolve(
            "model/internal/WeatherTypeRegistry.java")));
        assertFalse(table.contains("TypeRegistry"), table);
        assertFalse(registry.contains("AwsOperationDescriptor"), registry);
    }

    @Test
    void largeRegistriesAreSplitIntoLazyPartitions() throws IOException {
        int count = TypeRegistryPlan.DEFAULT_PARTITION_SIZE * 2 + 10;
        StringBuilder model = new StringBuilder("""
            $version: "2"
            namespace example.big
            @aws.protocols#awsJson1_0
            @aws.auth#sigv4(name: "big")
            service Big { version: "1", operations: [Get] }
            operation Get { output := {
            """);
        for (int i = 0; i < count; i++) {
            model.append("    m").append(i).append(": S").append(i).append('\n');
        }
        model.append("} }\n");
        for (int i = 0; i < count; i++) {
            model.append("structure S").append(i).append(" { value: String }\n");
        }
        Path models = Files.createDirectories(temp.resolve("big-model"));
        Files.writeString(models.resolve("big.smithy"), model.toString());
        AwsCodegenResult big = AwsCodegen.generate(
            new AwsCodegenRequest("example.big#Big", "software.amazon.awssdk.services.big", models,
                                  temp.resolve("big-out")), List.of());
        Path internal = Path.of("software/amazon/awssdk/services/big/model/internal");
        List<String> files = big.generatedFiles().stream().filter(p -> p.startsWith(internal))
                                .map(p -> p.getFileName().toString()).filter(n -> n.startsWith("BigTypeRegistry"))
                                .toList();
        assertEquals(List.of("BigTypeRegistry.java", "BigTypeRegistry1.java", "BigTypeRegistry2.java",
                             "BigTypeRegistry3.java"), files);

        String holder = Files.readString(big.sourceRoot().resolve(internal.resolve("BigTypeRegistry.java")));
        String indent = " ".repeat(16);
        assertTrue(holder.contains(indent + ".partition(BigTypeRegistry1.shapeIds(), BigTypeRegistry1::entries)\n"
                                   + indent + ".partition(BigTypeRegistry2.shapeIds(), BigTypeRegistry2::entries)\n"
                                   + indent + ".partition(BigTypeRegistry3.shapeIds(), BigTypeRegistry3::entries)\n"
                                   + indent + ".build();\n"), holder);
        assertFalse(holder.contains("RegisteredType.of("), "the holder registers only its partitions");

        Set<String> registered = new TreeSet<>();
        for (int part = 1; part <= 3; part++) {
            String source = Files.readString(big.sourceRoot().resolve(internal.resolve("BigTypeRegistry" + part
                                                                                        + ".java")));
            assertTrue(source.contains("\nfinal class BigTypeRegistry" + part + " {"), source);
            Matcher matcher = REGISTERED.matcher(source);
            int inPart = 0;
            while (matcher.find()) {
                assertTrue(registered.add(matcher.group(3)), matcher.group(3));
                inPart++;
            }
            assertTrue(inPart <= TypeRegistryPlan.DEFAULT_PARTITION_SIZE, "partition " + part + ": " + inPart);
        }
        assertEquals(count + 1, registered.size(), "S0..Sn and GetResponse: " + registered);
    }

    /**
     * A request member named like the non-modeled request override is modeled state under a {@code Value} name, so
     * that the override accessor stays free for the non-modeled override configuration of task 6.1, and generated
     * requests carry no state beyond their modeled members.
     */
    @Test
    void requestStateIsExactlyTheModeledMembers() throws IOException {
        Path models = Files.createDirectories(temp.resolve("override-model"));
        Files.writeString(models.resolve("override.smithy"), """
            $version: "2"
            namespace example.override
            @aws.protocols#awsJson1_0
            @aws.auth#sigv4(name: "override")
            service Override { version: "1", operations: [Put] }
            operation Put { input := { overrideConfiguration: String, name: String } }
            """);
        AwsCodegenResult generated = AwsCodegen.generate(
            new AwsCodegenRequest("example.override#Override", "software.amazon.awssdk.services.override", models,
                                  temp.resolve("override-out")), List.of());
        String request = Files.readString(generated.sourceRoot().resolve(
            "software/amazon/awssdk/services/override/model/PutRequest.java"));
        assertTrue(request.contains("public String overrideConfigurationValue() {"), request);
        assertFalse(request.contains(" overrideConfiguration("), request);
        List<String> fields = new ArrayList<>();
        Matcher field = Pattern.compile("\n    private final [\\w<>, ]+ (\\w+);").matcher(request);
        while (field.find()) {
            fields.add(field.group(1));
        }
        assertEquals(List.of("overrideConfigurationValue", "name"), fields);
    }

    /**
     * The options of AWS protocol traits, which are build-time only and so not in schemas, are rendered as runtime
     * values (task 4.1; Requirements 5.6, 5.12): every declared protocol sorted by trait ID with its {@code http} and
     * {@code eventStreamHttp} lists and {@code restXml} {@code noErrorWrapping}, {@code awsQueryCompatible}, and the
     * {@code awsQueryError} codes of operation and service errors sorted by error ID; query services also record
     * their version, and {@code ec2Query} services the {@code ec2QueryName} of every member of the closure sorted
     * by member ID (task 4.2).
     */
    @Test
    void protocolTraitOptionsAreRenderedAsServiceProtocols() throws IOException {
        Path models = Files.createDirectories(temp.resolve("multi-model"));
        Files.writeString(models.resolve("multi.smithy"), """
            $version: "2"
            namespace example.multi
            @aws.protocols#awsJson1_0(http: ["h2", "http/1.1"], eventStreamHttp: ["h2"])
            @aws.protocols#awsQuery
            @aws.protocols#awsQueryCompatible
            @smithy.protocols#rpcv2Cbor(http: ["h2"])
            @xmlNamespace(uri: "https://example.com/doc/")
            @aws.auth#sigv4(name: "multi")
            service Multi { version: "1", operations: [Get], errors: [Busy] }
            operation Get { input := {}, output := {}, errors: [Missing] }
            @error("client")
            @aws.protocols#awsQueryError(code: "Missing.Thing", httpResponseCode: 404)
            structure Missing {}
            @error("server")
            @aws.protocols#awsQueryError(code: "Busy", httpResponseCode: 503)
            structure Busy {}
            """);
        String metadata = Files.readString(AwsCodegen.generate(
            new AwsCodegenRequest("example.multi#Multi", "software.amazon.awssdk.services.multi", models,
                                  temp.resolve("multi-out")), List.of())
            .sourceRoot().resolve("software/amazon/awssdk/services/multi/MultiServiceMetadata.java"));
        String indent = " ".repeat(16);
        assertTrue(metadata.contains(
            "        static final ServiceProtocols DECLARED = ServiceProtocols.builder(MultiSchemas.MULTI)\n"
            + indent + ".protocol(new ModeledProtocol(ShapeId.from(\"aws.protocols#awsJson1_0\"), "
            + "List.of(\"h2\", \"http/1.1\"), List.of(\"h2\"), false))\n"
            + indent + ".protocol(new ModeledProtocol(ShapeId.from(\"aws.protocols#awsQuery\"), List.of(), "
            + "List.of(), false))\n"
            + indent + ".protocol(new ModeledProtocol(ShapeId.from(\"smithy.protocols#rpcv2Cbor\"), "
            + "List.of(\"h2\"), List.of(), false))\n"
            + indent + ".awsQueryCompatible(true)\n"
            + indent + ".awsQueryError(ShapeId.from(\"example.multi#Busy\"), \"Busy\", 503)\n"
            + indent + ".awsQueryError(ShapeId.from(\"example.multi#Missing\"), \"Missing.Thing\", 404)\n"
            + indent + ".serviceVersion(\"1\")\n"
            + indent + ".build();\n"), metadata);
        assertFalse(metadata.contains(".ec2QueryName("), metadata);
        assertTrue(metadata.contains("public static ServiceProtocols serviceProtocols() {\n"
                                     + "        return Protocols.DECLARED;\n"), metadata);

        Path xmlModels = Files.createDirectories(temp.resolve("xml-model"));
        Files.writeString(xmlModels.resolve("xml.smithy"), """
            $version: "2"
            namespace example.xml
            @aws.protocols#restXml(noErrorWrapping: true)
            @aws.auth#sigv4(name: "xml")
            service Xml { version: "1", operations: [Put] }
            @http(method: "PUT", uri: "/p")
            operation Put { input := {} }
            """);
        String xml = Files.readString(AwsCodegen.generate(
            new AwsCodegenRequest("example.xml#Xml", "software.amazon.awssdk.services.xml", xmlModels,
                                  temp.resolve("xml-out")), List.of())
            .sourceRoot().resolve("software/amazon/awssdk/services/xml/XmlServiceMetadata.java"));
        assertTrue(xml.contains(".protocol(new ModeledProtocol(ShapeId.from(\"aws.protocols#restXml\"), List.of(), "
                                + "List.of(), true))\n" + indent + ".build();\n"), xml);
        assertFalse(xml.contains(".awsQueryCompatible(") || xml.contains(".awsQueryError(")
                    || xml.contains(".serviceVersion(") || xml.contains(".ec2QueryName("), xml);

        Path ec2Models = Files.createDirectories(temp.resolve("ec2-model"));
        Files.writeString(ec2Models.resolve("ec2.smithy"), """
            $version: "2"
            namespace example.ec2
            @aws.protocols#ec2Query
            @xmlNamespace(uri: "https://example.com/ec2/")
            @aws.auth#sigv4(name: "ec2")
            service Ec2 { version: "2016-11-15", operations: [Run] }
            operation Run {
                input := {
                    @aws.protocols#ec2QueryName("ZoneName")
                    zone: String
                    nested: Nested
                }
            }
            structure Nested {
                @aws.protocols#ec2QueryName("Id")
                id: String
                plain: String
            }
            """);
        String ec2 = Files.readString(AwsCodegen.generate(
            new AwsCodegenRequest("example.ec2#Ec2", "software.amazon.awssdk.services.ec2", ec2Models,
                                  temp.resolve("ec2-out")), List.of())
            .sourceRoot().resolve("software/amazon/awssdk/services/ec2/Ec2ServiceMetadata.java"));
        assertTrue(ec2.contains(".protocol(new ModeledProtocol(ShapeId.from(\"aws.protocols#ec2Query\"), List.of(), "
                                + "List.of(), false))\n"
                                + indent + ".serviceVersion(\"2016-11-15\")\n"
                                + indent + ".ec2QueryName(ShapeId.from(\"example.ec2#Nested$id\"), \"Id\")\n"
                                + indent + ".ec2QueryName(ShapeId.from(\"example.ec2#RunInput$zone\"), \"ZoneName\")\n"
                                + indent + ".build();\n"), ec2);
    }

    /**
     * EC2 declares about 5,900 {@code ec2QueryName}s: past {@link ServiceMetadataGenerator#MAX_CHAINED_DECLARATIONS}
     * the declarations are builder statements split into methods, since one chain that long overflows javac's stack
     * and exceeds the JVM method size limit.
     */
    @Test
    void manyProtocolDeclarationsAreSplitIntoMethods() throws IOException {
        int count = ServiceMetadataGenerator.DECLARATIONS_PER_METHOD + 10;
        StringBuilder model = new StringBuilder("""
            $version: "2"
            namespace example.ec2
            @aws.protocols#ec2Query
            @xmlNamespace(uri: "https://example.com/ec2/")
            @aws.auth#sigv4(name: "ec2")
            service Ec2 { version: "2016-11-15", operations: [Run] }
            operation Run { input := {
            """);
        for (int i = 0; i < count; i++) {
            model.append(String.format("    @aws.protocols#ec2QueryName(\"M%04d\")%n    m%04d: String%n", i, i));
        }
        model.append("} }\n");
        Path models = Files.createDirectories(temp.resolve("ec2-many-model"));
        Files.writeString(models.resolve("ec2.smithy"), model.toString());
        String ec2 = Files.readString(AwsCodegen.generate(
            new AwsCodegenRequest("example.ec2#Ec2", "software.amazon.awssdk.services.ec2", models,
                                  temp.resolve("ec2-many-out")), List.of())
            .sourceRoot().resolve("software/amazon/awssdk/services/ec2/Ec2ServiceMetadata.java"));
        assertTrue(ec2.contains("        static final ServiceProtocols DECLARED = declared();\n"), ec2);
        assertTrue(ec2.contains("            ServiceProtocols.Builder builder = ServiceProtocols.builder("
                                + "Ec2Schemas.EC2);\n"
                                + "            declare1(builder);\n            declare2(builder);\n"
                                + "            return builder.build();\n"), ec2);
        assertTrue(ec2.contains("        private static void declare1(ServiceProtocols.Builder builder) {\n"
                                + "            builder.protocol(new ModeledProtocol(ShapeId.from("
                                + "\"aws.protocols#ec2Query\"), List.of(), List.of(), false));\n"
                                + "            builder.serviceVersion(\"2016-11-15\");\n"
                                + "            builder.ec2QueryName(ShapeId.from(\"example.ec2#RunInput$m0000\"), "
                                + "\"M0000\");\n"), ec2);
        assertFalse(ec2.contains("declare3"), ec2);
        assertFalse(ec2.contains("\n                .ec2QueryName("), "no chained declarations remain");
        String second = ec2.substring(ec2.indexOf("private static void declare2("));
        assertEquals(count + 2 - ServiceMetadataGenerator.DECLARATIONS_PER_METHOD,
                     second.split("\n            builder\\.", -1).length - 1, second);
        assertEquals(count, ec2.split("builder\\.ec2QueryName\\(", -1).length - 1);
    }

    private static String resource(String name) {
        try (InputStream in = ServiceMetadataGenerationTest.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
