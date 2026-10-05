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

package software.amazon.awssdk.awscodegen.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.AnnotationTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitService;

/**
 * Schema holder generation (Requirements 2.11, 4.1, 4.5-4.7): a golden holder for a model with a structure/list/union
 * cycle, self-recursion, an enum, an intEnum, errors, defaults, constraints and unknown traits; an independent
 * closure oracle for "exactly one schema per shape"; partitioning; naming; and the literal and trait encodings.
 */
class SchemaGenerationTest {

    private static final String SERVICE = "example.weather#Weather";
    private static final String PACKAGE = "software.amazon.awssdk.services.weather";
    private static final Path HOLDER = Path.of("software/amazon/awssdk/services/weather/model/internal/"
                                               + "WeatherSchemas.java");
    /** A schema creation in generated source: {@code Schema.<factory or builder>(ShapeId.from("<id>")}. */
    private static final Pattern SCHEMA_CREATION =
        Pattern.compile("Schema\\.(create\\w+|\\w+Builder)\\(ShapeId\\.from\\(\"([^\"]+)\"\\)");
    private static final Pattern EXPORTED = Pattern.compile("\n    public static final Schema (\\w+) = ");

    @TempDir
    Path temp;

    @Test
    void holderMatchesGolden() throws IOException {
        AwsCodegenResult result = generate(Map.of("weather.smithy", resource("weather.smithy")));
        String actual = Files.readString(result.sourceRoot().resolve(HOLDER));
        String expected = resource("WeatherSchemas.java.golden");
        if (!expected.equals(actual)) {
            Path written = Path.of("target", "golden-actual", "WeatherSchemas.java.golden");
            Files.createDirectories(written.getParent());
            Files.writeString(written, actual);
            assertEquals(expected, actual, "Generated holder differs from the golden file; actual output: " + written);
        }
    }

    /**
     * Oracle: an independent walk of the assembled model. Every shape of the service closure except members,
     * resources and prelude shapes is created exactly once; the service, operations, structures, unions, enums and
     * intEnums are the exported constants, nothing else is.
     */
    @Test
    void everyClosureShapeHasExactlyOneSchemaAndPublicTypesAreExported() throws IOException {
        String model = resource("weather.smithy");
        AwsCodegenResult result = generate(Map.of("weather.smithy", model));
        String holder = Files.readString(result.sourceRoot().resolve(HOLDER));

        Model assembled = assemble(model);
        ServiceShape service = assembled.expectShape(ShapeId.from(SERVICE), ServiceShape.class);
        Set<ShapeId> expected = new TreeSet<>();
        Set<String> exported = new TreeSet<>();
        for (Shape shape : new Walker(assembled).walkShapes(service)) {
            if (shape.isMemberShape() || shape.isResourceShape() || shape.getId().getNamespace().equals("smithy.api")) {
                continue;
            }
            expected.add(shape.getId());
            if (Set.of(ShapeType.SERVICE, ShapeType.OPERATION, ShapeType.STRUCTURE, ShapeType.UNION, ShapeType.ENUM,
                       ShapeType.INT_ENUM).contains(shape.getType())) {
                exported.add(SchemaSymbols.constantName(shape.getId().getName(service)));
            }
        }
        assertEquals(14, expected.size(), expected.toString());

        List<ShapeId> created = new ArrayList<>();
        Matcher matcher = SCHEMA_CREATION.matcher(holder);
        while (matcher.find()) {
            created.add(ShapeId.from(matcher.group(2)));
        }
        assertEquals(expected, new TreeSet<>(created));
        assertEquals(expected.size(), created.size(), "a shape is created more than once: " + created);

        Set<String> constants = new TreeSet<>();
        matcher = EXPORTED.matcher(holder);
        while (matcher.find()) {
            assertTrue(constants.add(matcher.group(1)), matcher.group(1));
        }
        assertEquals(exported, constants);
    }

    @Test
    void largeServicesArePartitionedWithEveryExportInTheHolder() throws IOException {
        // Wide rather than deep: Smithy's TopologicalIndex (used by CodegenDirector) is slow on long chains.
        int count = 150;
        StringBuilder model = new StringBuilder("""
            $version: "2"
            namespace example.big

            @aws.protocols#awsJson1_0
            @aws.auth#sigv4(name: "big")
            service Big {
                version: "1"
                operations: [Get]
            }

            operation Get {
                output := {
                    tree: Tree
            """);
        for (int i = 0; i < count; i++) {
            model.append("        s").append(i).append(": S").append(i).append("\n");
        }
        model.append("""
                }
            }

            structure Tree {
                children: Trees
            }

            list Trees {
                member: Tree
            }

            """);
        for (int i = 0; i < count; i++) {
            model.append("structure S").append(i).append(" {\n");
            for (int m = 0; m < 5; m++) {
                model.append("    @length(min: 1)\n    m").append(m).append(": String\n");
            }
            model.append("    tree: Tree\n}\n\n");
        }
        AwsCodegenResult result = AwsCodegen.generate(
            new AwsCodegenRequest("example.big#Big", "software.amazon.awssdk.services.big",
                                  write(Map.of("big.smithy", model.toString())), temp.resolve("out")), List.of());
        Path internal = Path.of("software/amazon/awssdk/services/big/model/internal");
        List<Path> files = result.generatedFiles().stream().filter(p -> p.startsWith(internal))
                                 .filter(p -> p.getFileName().toString().startsWith("BigSchemas")).toList();
        assertTrue(files.size() > 3, files.toString());
        String holder = Files.readString(result.sourceRoot().resolve(internal.resolve("BigSchemas.java")));

        Set<String> exports = new TreeSet<>();
        Matcher exported = Pattern.compile("\n    public static final Schema (\\w+) = BigSchemas(\\d+)\\.(\\w+);")
                                  .matcher(holder);
        while (exported.find()) {
            assertEquals(exported.group(1), exported.group(3));
            assertTrue(exports.add(exported.group(1)));
        }
        Set<String> expectedExports = new TreeSet<>(Set.of("BIG", "GET", "GET_OUTPUT", "TREE"));
        for (int i = 0; i < count; i++) {
            expectedExports.add("S" + i);
        }
        assertEquals(expectedExports, exports);
        assertTrue(holder.contains("ServiceSchemaIndex.Builder builder = ServiceSchemaIndex.builder(BIG);"), holder);
        assertTrue(holder.contains("            BigSchemas1.register(builder);\n"), holder);
        assertTrue(!SCHEMA_CREATION.matcher(holder).find(), "the holder only re-exports partition fields");

        Set<ShapeId> created = new TreeSet<>();
        for (int part = 1; part < files.size(); part++) {
            String source = Files.readString(result.sourceRoot().resolve(internal.resolve("BigSchemas" + part
                                                                                          + ".java")));
            assertTrue(source.contains("\nfinal class BigSchemas" + part + " {"), source);
            assertTrue(source.contains("    static void register(ServiceSchemaIndex.Builder builder) {"), source);
            Matcher reference = Pattern.compile("BigSchemas(\\d+)\\.").matcher(source);
            while (reference.find()) {
                assertTrue(Integer.parseInt(reference.group(1)) < part,
                           "partition " + part + " references a later partition: " + reference.group());
            }
            Matcher creation = SCHEMA_CREATION.matcher(source);
            while (creation.find()) {
                assertTrue(created.add(ShapeId.from(creation.group(2))), creation.group(2));
            }
        }
        assertEquals(count + 5, created.size(), "S0..S149, Tree, Trees, GetOutput, Get and Big: " + created);
    }

    @Test
    void schemaConstantCollisionsFailNamingBothShapes() throws IOException {
        String model = """
            $version: "2"
            namespace example.clash

            @aws.protocols#awsJson1_0
            @aws.auth#sigv4(name: "clash")
            service Clash {
                version: "1"
                operations: [Get]
            }

            operation Get {
                output := {
                    a: FooBar
                    b: Foo_Bar
                }
            }

            structure FooBar {}

            structure Foo_Bar {}
            """;
        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> AwsCodegen.generate(
            new AwsCodegenRequest("example.clash#Clash", "software.amazon.awssdk.services.clash",
                                  write(Map.of("clash.smithy", model)), temp.resolve("out")), List.of()));
        assertTrue(e.getMessage().contains("example.clash#FooBar") && e.getMessage().contains("example.clash#Foo_Bar")
                   && e.getMessage().contains("FOO_BAR"), e.getMessage());
    }

    @Test
    void stringLiteralsAreAsciiAndExact() {
        assertEquals("\"plain\"", JavaCode.stringLiteral("plain"));
        assertEquals("\"q\\\" b\\\\ n\\n r\\r t\\t b\\b f\\f\"", JavaCode.stringLiteral("q\" b\\ n\n r\r t\t b\b f\f"));
        assertEquals("\"\\000\\037\\177\"", JavaCode.stringLiteral("\u0000\u001f\u007f"));
        assertEquals("\"\\u00e9\\u2028\\ufeff\\ud83d\\ude00\"", JavaCode.stringLiteral("é\u2028\ufeff\uD83D\uDE00"));
        // A literal backslash-u stays two characters: the escaped backslash is not a Unicode escape.
        assertEquals("\"\\\\u0041\"", JavaCode.stringLiteral("\\u0041"));
        assertEquals("\"$$$${x}\"", new JavaCode().string("$${x}").build().template(), "$ is escaped for the writer");
    }

    /**
     * Every runtime trait class can be created the way generated code creates it: annotation traits with a public
     * no-argument constructor, and every other trait with its public {@code Provider}, which is the trait's
     * {@code TraitService}.
     */
    @Test
    void everyRuntimeTraitHasItsCreationPath() throws ReflectiveOperationException {
        for (Map.Entry<ShapeId, String> entry : RuntimeTraits.TRAITS.entrySet()) {
            Class<? extends Trait> type = Class.forName(entry.getValue()).asSubclass(Trait.class);
            assertEquals(entry.getKey(), type.getField("ID").get(null));
            assertEquals("software.amazon.smithy.model.traits", type.getPackageName(), type.getName());
            Class<?> provider = Class.forName(type.getName() + "$Provider");
            assertTrue(TraitService.class.isAssignableFrom(provider) && Modifier.isPublic(provider.getModifiers()),
                       provider.getName());
            TraitService service = (TraitService) provider.getConstructor().newInstance();
            assertEquals(entry.getKey(), service.getShapeId());
            if (AnnotationTrait.class.isAssignableFrom(type)) {
                Constructor<? extends Trait> constructor = type.getConstructor();
                assertEquals(entry.getKey(), constructor.newInstance().toShapeId());
            }
        }
    }

    private AwsCodegenResult generate(Map<String, String> files) throws IOException {
        return AwsCodegen.generate(new AwsCodegenRequest(SERVICE, PACKAGE, write(files), temp.resolve("generated")),
                                   List.of());
    }

    private Path write(Map<String, String> files) throws IOException {
        Path models = Files.createDirectories(temp.resolve("model-" + files.hashCode()));
        for (Map.Entry<String, String> file : new TreeMap<>(files).entrySet()) {
            Files.writeString(models.resolve(file.getKey()), file.getValue(), StandardCharsets.UTF_8);
        }
        return models;
    }

    private static Model assemble(String model) {
        ClassLoader loader = SchemaGenerationTest.class.getClassLoader();
        return Model.assembler(loader).discoverModels(loader).putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                    .addUnparsedModel("weather.smithy", model).assemble().unwrap();
    }

    static String resource(String name) {
        try (InputStream in = SchemaGenerationTest.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
