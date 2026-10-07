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

package software.amazon.awssdk.awscodegen.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;

/**
 * Generated model types (task 3.2; Requirements 4.1-4.4, 4.8-4.11): golden files for a recursive structure with a
 * required list, a union with a {@code Unit} member, an enum and a throttling error of the schema test model, plus
 * the service exception; and the model-type inventory of the closure. Runtime behavior of generated types (SERDE,
 * equality, immutability, requiredness) is covered by the lifecycle fixture's {@code GeneratedModelTypeFixtureTest}.
 */
class ModelTypeGenerationTest {

    private static final String SERVICE = "example.weather#Weather";
    private static final String PACKAGE = "software.amazon.awssdk.services.weather";
    private static final Path MODEL = Path.of("software/amazon/awssdk/services/weather/model");

    @TempDir
    static Path temp;

    private static AwsCodegenResult result;

    private static synchronized AwsCodegenResult result() throws IOException {
        if (result == null) {
            Path models = Files.createDirectories(temp.resolve("model"));
            Files.writeString(models.resolve("weather.smithy"), resource("/software/amazon/awssdk/awscodegen/schema/weather.smithy"),
                              StandardCharsets.UTF_8);
            result = AwsCodegen.generate(new AwsCodegenRequest(SERVICE, PACKAGE, models, temp.resolve("generated")),
                                         List.of());
        }
        return result;
    }

    static Stream<Arguments> goldens() {
        return Stream.of(Arguments.of("Forecast.java", "structure"),
                         Arguments.of("Day.java", "union"),
                         Arguments.of("TemperatureUnit.java", "enum"),
                         Arguments.of("ThrottledException.java", "error"),
                         Arguments.of("WeatherException.java", "service exception"),
                         Arguments.of("WeatherResponse.java", "service response"),
                         Arguments.of("WeatherResponseMetadata.java", "service response metadata"));
    }

    @ParameterizedTest(name = "{1} {0}")
    @MethodSource("goldens")
    void generatedTypeMatchesGolden(String file, String kind) throws IOException {
        String actual = Files.readString(result().sourceRoot().resolve(MODEL.resolve(file)));
        String expected = resource(file + ".golden");
        if (!expected.equals(actual)) {
            Path written = Path.of("target", "golden-actual", file + ".golden");
            Files.createDirectories(written.getParent());
            Files.writeString(written, actual);
            assertEquals(expected, actual, "Generated " + kind + " differs from the golden file; actual output: "
                                           + written);
        }
    }

    @Test
    void everyStructureUnionEnumAndErrorOfTheClosureHasOneType() throws IOException {
        List<String> types = result().generatedFiles().stream()
                                     .filter(p -> p.getParent().equals(MODEL))
                                     .map(p -> p.getFileName().toString())
                                     .toList();
        assertEquals(List.of("Day.java", "Forecast.java", "GetForecastRequest.java", "GetForecastResponse.java",
                             "Level.java", "NoSuchCityException.java", "RainDetail.java", "TemperatureUnit.java",
                             "ThrottledException.java", "WeatherException.java", "WeatherResponse.java",
                             "WeatherResponseMetadata.java"),
                     types);
    }

    @Test
    void generatedTypesUseDirectMemberSwitchesOnly() throws IOException {
        for (Path file : result().generatedFiles()) {
            if (!file.getParent().equals(MODEL)) {
                continue;
            }
            String source = Files.readString(result().sourceRoot().resolve(file));
            assertFalse(source.contains("java.lang.reflect"), file.toString());
            assertFalse(source.contains("getDeclaredField"), file.toString());
            assertFalse(source.contains("Class.forName"), file.toString());
            if (source.contains(" implements SerializableStruct")) {
                assertTrue(source.contains("switch (member.memberIndex())"), file.toString());
            }
        }
    }

    /**
     * Task 16.1 (Requirement 21.1): an operation output extends the service's base response, so it carries the
     * response metadata, and its builders extend the base response's builders; other structures do not.
     */
    @Test
    void operationOutputsExtendTheServiceResponse() throws IOException {
        String output = Files.readString(result().sourceRoot().resolve(MODEL.resolve("GetForecastResponse.java")));
        assertTrue(output.contains("public final class GetForecastResponse extends WeatherResponse implements "
                                   + "SerializableStruct, ToCopyableBuilder<GetForecastResponse.Builder, "
                                   + "GetForecastResponse> {"), output);
        assertTrue(output.contains("public interface Builder extends WeatherResponse.Builder, CopyableBuilder<Builder, "
                                   + "GetForecastResponse>, ShapeBuilder<GetForecastResponse> {"), output);
        assertTrue(output.contains("static final class BuilderImpl extends WeatherResponse.BuilderImpl implements "
                                   + "Builder {"), output);
        for (String type : List.of("GetForecastRequest.java", "Forecast.java")) {
            String source = Files.readString(result().sourceRoot().resolve(MODEL.resolve(type)));
            assertFalse(source.contains("WeatherResponse"), type);
        }
    }

    /**
     * Most canonical AWS models declare enums as Smithy 1 {@code @enum} string shapes; they generate the same Java
     * enum as an enum shape.
     */
    @Test
    void aSmithy1EnumTraitStringGeneratesAJavaEnum() throws IOException {
        String weather = resource("/software/amazon/awssdk/awscodegen/schema/weather.smithy");
        int start = weather.indexOf("enum TemperatureUnit {");
        int end = weather.indexOf("}", start) + 1;
        String smithy1 = weather.substring(0, start)
                         + "@enum([{ value: \"C\", name: \"CELSIUS\" }, { value: \"F\", name: \"FAHRENHEIT\" }, "
                         + "{ value: \"kelvin\", name: \"KELVIN\" }])\nstring TemperatureUnit"
                         + weather.substring(end);
        Path models = Files.createDirectories(temp.resolve("smithy1-model"));
        Files.writeString(models.resolve("weather.smithy"), smithy1, StandardCharsets.UTF_8);
        AwsCodegenResult generated = AwsCodegen.generate(
            new AwsCodegenRequest(SERVICE, PACKAGE, models, temp.resolve("smithy1-generated")), List.of());
        String source = Files.readString(generated.sourceRoot().resolve(MODEL.resolve("TemperatureUnit.java")));
        assertTrue(source.contains("public enum TemperatureUnit {\n    CELSIUS(\"C\"),\n    FAHRENHEIT(\"F\"),\n"
                                   + "    KELVIN(\"kelvin\"),\n    UNKNOWN_TO_SDK_VERSION(null);"), source);
    }

    private static String resource(String name) {
        try (InputStream in = ModelTypeGenerationTest.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
