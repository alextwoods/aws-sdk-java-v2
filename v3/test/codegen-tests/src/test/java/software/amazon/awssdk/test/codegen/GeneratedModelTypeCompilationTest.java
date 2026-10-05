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

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels.ModelSpec;

/**
 * Everything aws-codegen emits for a model compiles against the generated-client runtime (Requirements 4.1-4.4, 4.8):
 * a model of naming and typing edge cases, and a fixed sample of the random models of Property 5. Generation succeeding
 * is not enough: a member name, default or target the model-type templates mishandle surfaces only when the Java
 * sources are compiled.
 */
class GeneratedModelTypeCompilationTest {

    private static final String PACKAGE = "software.amazon.awssdk.services.edge";
    private static final String MODEL_PACKAGE = PACKAGE + ".model.";

    /**
     * Member names that are Java keywords, V2-reserved or generated method names, or names of the templates' locals
     * and nested types; enum constants that clash with generated constants; defaults on every target that has one;
     * sets, sparse and enum-keyed collections, nested enum collections; a recursive union with a unit member; and
     * errors whose {@code message} member is not a string, whose name ends in {@code Fault} or clashes with the service
     * exception, or which shadow exception builder methods.
     */
    private static final String EDGE_CASES = """
        $version: "2"
        namespace example.edge

        @aws.protocols#awsJson1_0
        @aws.auth#sigv4(name: "edge")
        service Edge {
            version: "1"
            operations: [Do]
            errors: [BadMessage, EdgeException, Fault, Type]
        }

        operation Do {
            input := {
                builder: String
                toBuilder: String
                hashCode: Integer
                getClass: String
                class: String
                default: String
                type: String
                equals: String
                toString: String
                schema: String
                serializeMembers: String
                getMemberValue: String
                copy: String
                sdkFields: String
                BuilderImpl: String
                value: String
                values: String
                member: String
                de: String
                serializer: String
                e0: String
                sdkHttpResponse: String
                overrideConfiguration: String
                responseMetadata: String
                doc: Document
                dec: BigDecimal
                bigInt: BigInteger
                byteValue: Byte
                floatValue: Float
                shortValue: Short
                set: StringSet
                legacySet: LegacySet
                sparseList: SparseList
                sparseMap: SparseMap
                enumMap: EnumMap
                intEnumList: IntEnumList
                enumSet: EnumSet
                nestedList: NestedList
                nestedMap: NestedMap
                recursiveUnion: Tree
                withDefaults: Defaults
                caps: Caps
                otherUnion: CapsUnion
                unionWithType: UnionWithType
                blob: Blob
            }
            output := {
                @required
                requiredList: StringSet

                @required
                requiredStruct: Defaults

                @required
                requiredUnion: Tree

                @required
                requiredEnum: Color

                @required
                requiredIntEnum: Level

                @required
                requiredDoc: Document

                @required
                requiredBlob: Blob

                @required
                requiredTimestamp: Timestamp

                @required
                requiredBig: BigDecimal
            }
        }

        structure Defaults {
            blob: Blob = "aGVsbG8="
            timestamp: Timestamp = 0
            doc: Document = "text"
            emptyDoc: Document = []
            dec: BigDecimal = 1.5
            bigInt: BigInteger = 12345678901234567890
            list: StringSet = []
            map: SparseMap = {}
            color: Color = "red"
            level: Level = 2
            bool: Boolean = true
            byte: Byte = -1
            short: Short = 7
            long: Long = 9007199254740993
            float: Float = 1.25
            double: Double = -0.5
            string: String = "with \\"quotes\\" and \\\\ backslash $ {braces}"

            @required
            requiredDefault: Integer = 0

            @required
            requiredList: StringSet = []
        }

        enum Color {
            RED = "red"
            GREEN = "green"
            UNKNOWN_TO_SDK_VERSION = "unknown"
            SCHEMA = "schema"
            ID = "id"
            lowercase = "lower"
            lowerCamel = "lowerCamel"
        }

        intEnum Level {
            LOW = 1
            HIGH = 2
            UNKNOWN_TO_SDK_VERSION = 3
        }

        @uniqueItems
        list StringSet {
            member: String
        }

        @sparse
        list SparseList {
            member: String
        }

        @sparse
        map SparseMap {
            key: String
            value: Integer
        }

        map EnumMap {
            key: Color
            value: Level
        }

        list IntEnumList {
            member: Level
        }

        @uniqueItems
        list EnumSet {
            member: Color
        }

        list NestedList {
            member: EnumMap
        }

        map NestedMap {
            key: String
            value: NestedList
        }

        union Tree {
            leaf: String
            node: TreeList
            self: Tree
            unit: Unit
            type: String
            UNKNOWN_TO_SDK_VERSION: String
        }

        list TreeList {
            member: Tree
        }

        union UnionWithType {
            type: String
            builder: Integer
            values: Color
        }

        @error("client")
        structure BadMessage {
            message: Integer
        }

        @error("client")
        structure EdgeException {
            Message: String
            cause: String
            statusCode: Integer
            requestId: String
            retryable: Boolean
        }

        @error("server")
        structure Fault {
            message: String
            type: String
        }

        @error("client")
        structure Type {
            message: Document
        }

        structure Caps {
            Builder: String
            SCHEMA: String
            Type: String
            ID: String
            UNKNOWN_TO_SDK_VERSION: String
        }

        union CapsUnion {
            Type: Integer
            SCHEMA_TYPE: String
            builder: String
        }
        """;

    /** A Smithy IDL 1.0 {@code set}, which assembles to a list with {@code @uniqueItems}. */
    private static final String LEGACY_SET = """
        $version: "1.0"
        namespace example.edge

        set LegacySet {
            member: String
        }
        """;

    @TempDir
    Path temp;

    @Test
    void edgeCaseModelCompilesAndBehaves() throws Exception {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("edge.smithy"), EDGE_CASES, StandardCharsets.UTF_8);
        Files.writeString(models.resolve("legacy.smithy"), LEGACY_SET, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest("example.edge#Edge", PACKAGE, models,
                                                                            temp.resolve("out")), List.of());
        CompiledSources compiled = CompiledSources.compile(result.sourceRoot(), temp.resolve("classes"));

        try (URLClassLoader loader = compiled.loader()) {
            // A non-string message member is state of the error; message(String) stays the exception message.
            Object badMessage = build(loader, "BadMessageException", Map.of("messageValue", 5, "message", "text"));
            assertEquals(5, call(badMessage, "messageValue"));
            assertTrue(((Throwable) badMessage).getMessage().contains("text"), ((Throwable) badMessage).getMessage());

            // Modeled defaults of every kind of target initialize the builder.
            Object defaults = build(loader, "Defaults", Map.of());
            assertEquals(SdkBytes.fromUtf8String("hello"), call(defaults, "blob"));
            assertEquals(Instant.EPOCH, call(defaults, "timestamp"));
            assertEquals(new BigDecimal("1.5"), call(defaults, "dec"));
            assertEquals(new BigInteger("12345678901234567890"), call(defaults, "bigInt"));
            assertEquals(List.of(), call(defaults, "list"));
            assertEquals(true, call(defaults, "hasList"));
            assertEquals("red", call(defaults, "colorAsString"));
            assertEquals(2, call(defaults, "levelAsInteger"));
            assertEquals((byte) -1, call(defaults, "byteValue"));
            assertEquals(9007199254740993L, call(defaults, "longValue"));
            assertEquals(1.25f, call(defaults, "floatValue"));
            assertEquals("with \"quotes\" and \\ backslash $ {braces}", call(defaults, "string"));
        }
    }

    static LongStream seeds() {
        return LongStream.rangeClosed(1, 40);
    }

    /** A random model of Property 5, with a size drawn from the seed, generates sources that compile. */
    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void randomModelCompiles(long seed) throws IOException {
        Random random = new Random(seed);
        ModelSpec spec = RandomSmithyModels.model(random, 1 + random.nextInt(6), 4 + random.nextInt(12),
                                                  random.nextInt(3), random.nextInt(4), random.nextBoolean());
        Path models = Files.createDirectories(temp.resolve("model"));
        for (Map.Entry<String, String> file : RandomSmithyModels.render(spec, random).entrySet()) {
            Path target = models.resolve(file.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.getValue(), StandardCharsets.UTF_8);
        }
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest(spec.service().toString(),
                                                                            spec.packageName(), models,
                                                                            temp.resolve("out")), List.of());
        CompiledSources.compile(result.sourceRoot(), temp.resolve("classes"));
    }

    /** Builds a generated model type after calling each builder setter of {@code values} (by name, one argument). */
    private static Object build(ClassLoader loader, String type, Map<String, Object> values) throws Exception {
        Object builder = Class.forName(MODEL_PACKAGE + type, true, loader).getMethod("builder").invoke(null);
        for (Map.Entry<String, Object> value : values.entrySet()) {
            Method setter = null;
            for (Method method : builder.getClass().getMethods()) {
                if (method.getName().equals(value.getKey()) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isInstance(value.getValue())) {
                    setter = method;
                }
            }
            if (setter == null) {
                throw new AssertionError("No setter " + value.getKey() + " on " + type);
            }
            setter.setAccessible(true);
            setter.invoke(builder, value.getValue());
        }
        Method build = builder.getClass().getMethod("build");
        build.setAccessible(true);
        return build.invoke(builder);
    }

    private static Object call(Object target, String accessor) throws Exception {
        return target.getClass().getMethod(accessor).invoke(target);
    }
}
