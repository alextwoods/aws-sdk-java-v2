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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.MissingRequiredMembersException;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 10 (Requirement 4.11): building a generated model type with any subset of its required members omitted
 * throws one {@link MissingRequiredMembersException} whose member IDs are exactly the omitted required members, in
 * modeled order, whichever way the builder was populated.
 *
 * <p>The oracle is this test's own description of the model ({@link #TYPES}), from which the Smithy model is also
 * rendered: which members are {@code @required}, {@code @clientOptional} or carry a {@code @default}, and each member's
 * wire value, default and Smithy client error-correction value. Nothing is read from the generated schemas except the
 * member {@link Schema} handles that {@link ShapeBuilder#setMemberValue} and {@link SerializableStruct#getMemberValue}
 * take. A member is required at build time when it is {@code @required} and not {@code @clientOptional} (none of the
 * described structures is an operation input). A required member with a default is missing only when explicitly
 * cleared; on the wire, an absent defaulted member keeps its default.
 *
 * <p>Each try picks a described type and a subset of its members to omit, and builds the type four ways: named builder
 * setters, {@link ShapeBuilder#setMemberValue}, raw JSON deserialization without error correction, and
 * {@code toBuilder()} of a complete value with the omitted members cleared. Each builder is then error-corrected, which
 * must fill every missing required member with its zero value and build.
 */
class MissingRequiredMembersPropertyTest {

    private static final String NAMESPACE = "example.missing";
    private static final String PACKAGE = "software.amazon.awssdk.services.missingrequired";
    private static final String MODEL_PACKAGE = PACKAGE + ".model.";
    private static final JsonCodec JSON = JsonCodec.builder().build();
    /** The wire value of every string member; never part of an exception message, which names members only. */
    private static final String STRING_SENTINEL = "string-value-sentinel";

    /** The target of a described member, its JSON wire value, and the Smithy target it renders to. */
    enum Kind {
        STRING("String", "\"" + STRING_SENTINEL + "\""),
        BOOLEAN("Boolean", "true"),
        BYTE("Byte", "3"),
        SHORT("Short", "4"),
        INTEGER("Integer", "7"),
        LONG("Long", "6"),
        FLOAT("Float", "1.5"),
        DOUBLE("Double", "2.5"),
        BIG_INTEGER("BigInteger", "12345678901234567890"),
        BIG_DECIMAL("BigDecimal", "1.25"),
        BLOB("Blob", "\"aGk=\""),
        TIMESTAMP("Timestamp", "1700000000"),
        DOCUMENT("Document", "{\"a\":[1,\"b\"]}"),
        ENUM("Color", "\"red\""),
        INT_ENUM("Level", "2"),
        LIST("StringList", "[\"a\",\"b\"]"),
        MAP("IntMap", "{\"k\":1}"),
        STRUCTURE("Inner", "{\"note\":\"n\"}"),
        UNION("Choice", "{\"leaf\":\"x\"}");

        final String target;
        final String json;

        Kind(String target, String json) {
            this.target = target;
            this.json = json;
        }
    }

    /**
     * A described member.
     *
     * @param defaultLiteral Smithy {@code @default} literal; null if none
     * @param defaultValue the generated type's internal value of the default (as {@code getMemberValue} returns it)
     */
    record Member(String name, Kind kind, boolean required, boolean clientOptional, String defaultLiteral,
                  Object defaultValue) {
        boolean requiredAtBuild() {
            return required && !clientOptional;
        }

        boolean defaulted() {
            return defaultLiteral != null;
        }
    }

    /** A described structure or error. */
    record Type(String name, boolean error, List<Member> members) {
        String javaClass() {
            return MODEL_PACKAGE + name + (error ? "Exception" : "");
        }

        ShapeId id() {
            return ShapeId.from(NAMESPACE + "#" + name);
        }

        ShapeId memberId(Member member) {
            return ShapeId.from(NAMESPACE + "#" + name + "$" + member.name());
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final List<Type> TYPES = List.of(
        new Type("One", false, List.of(req("text", Kind.STRING))),
        new Type("Scalars", false, List.of(
            req("text", Kind.STRING), opt("note", Kind.STRING), req("flag", Kind.BOOLEAN), req("tiny", Kind.BYTE),
            req("small", Kind.SHORT), opt("extra", Kind.INTEGER), req("count", Kind.INTEGER), req("total", Kind.LONG),
            req("fraction", Kind.FLOAT), req("measure", Kind.DOUBLE), req("huge", Kind.BIG_INTEGER),
            req("precise", Kind.BIG_DECIMAL))),
        new Type("Aggregates", false, List.of(
            req("items", Kind.LIST), opt("spare", Kind.LIST), req("counts", Kind.MAP), req("inner", Kind.STRUCTURE),
            req("choice", Kind.UNION), opt("otherChoice", Kind.UNION), req("color", Kind.ENUM),
            req("level", Kind.INT_ENUM), req("doc", Kind.DOCUMENT), req("bytes", Kind.BLOB),
            req("when", Kind.TIMESTAMP))),
        new Type("Defaults", false, List.of(
            req("text", Kind.STRING), reqDefault("count", Kind.INTEGER, "5", 5),
            reqDefault("flag", Kind.BOOLEAN, "false", false), clientOptional("maybe", Kind.STRING),
            reqDefault("label", Kind.STRING, "\"dflt\"", "dflt"), reqDefault("items", Kind.LIST, "[]", List.of()),
            opt("note", Kind.STRING), reqDefault("color", Kind.ENUM, "\"green\"", "green"),
            reqDefault("ratio", Kind.DOUBLE, "1.5", 1.5d), reqDefault("when", Kind.TIMESTAMP, "0", Instant.EPOCH),
            req("counts", Kind.MAP))),
        new Type("Rejected", true, List.of(
            req("reason", Kind.STRING), opt("detail", Kind.STRING), req("limit", Kind.LONG),
            req("retryAt", Kind.TIMESTAMP), reqDefault("hint", Kind.STRING, "\"h\"", "h"), req("codes", Kind.LIST))));

    /** The ways a builder is populated. */
    enum Route {
        SETTERS, SET_MEMBER_VALUE, WIRE, TO_BUILDER
    }

    /** A described type with the names of the members it omits. */
    record Scenario(Type type, Set<String> omitted) {
        @Override
        public String toString() {
            return type + " omitting " + omitted;
        }
    }

    private static Generated generated;

    // Feature: aws-sdk-java-v3, Property 10: Missing required members are aggregated
    @Property(tries = 1_000)
    void missingRequiredMembersAreAggregated(@ForAll("scenarios") Scenario scenario) throws Exception {
        Generated g = generated();
        Type type = scenario.type();
        List<Member> omittedRequired = type.members().stream()
                                           .filter(m -> m.requiredAtBuild() && scenario.omitted().contains(m.name()))
                                           .toList();
        Statistics.label("omitted required members").collect(omittedRequired.isEmpty() ? "none" : "some");
        Statistics.label("omitted required members")
                  .coverage(c -> {
                      c.check("none").percentage(p -> p >= 3);
                      c.check("some").percentage(p -> p >= 60);
                  });
        Statistics.label("type").collect(type.name());
        Statistics.label("omits a cleared default")
                  .collect(omittedRequired.stream().anyMatch(Member::defaulted));

        for (Route path : Route.values()) {
            // On the wire an absent defaulted member keeps its default; every other path clears it explicitly.
            List<ShapeId> expected = omittedRequired.stream()
                                                    .filter(m -> path != Route.WIRE || !m.defaulted())
                                                    .map(type::memberId)
                                                    .toList();
            String where = scenario + " via " + path;

            ShapeBuilder<?> builder = populate(g, type, path, scenario.omitted());
            if (expected.isEmpty()) {
                SerializableStruct built = (SerializableStruct) builder.build();
                assertValues(g, type, path, scenario.omitted(), built, false, where);
            } else {
                MissingRequiredMembersException e = assertThrows(MissingRequiredMembersException.class, builder::build,
                                                                 where);
                assertEquals(type.id(), e.shape(), where);
                assertEquals(expected, e.missingMembers(), where);
                for (ShapeId member : expected) {
                    assertTrue(e.getMessage().contains(member.toString()), where + ": " + e.getMessage());
                }
                assertFalse(e.getMessage().contains(STRING_SENTINEL), where + ": " + e.getMessage());
                assertNull(e.getCause(), where);
                assertEquals(0, e.getSuppressed().length, where);
            }

            // Error correction fills every missing required member with its zero value, so build() cannot fail.
            SerializableStruct corrected = (SerializableStruct) builder.errorCorrection().build();
            assertValues(g, type, path, scenario.omitted(), corrected, true, where + " + errorCorrection()");
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        return Arbitraries.of(TYPES).flatMap(
            // Shrinks toward omitting nothing, so a shrunk counterexample names the fewest omitted members.
            type -> Arbitraries.of(false, true).list().ofSize(type.members().size()).map(
                mask -> new Scenario(type, IntStream.range(0, mask.size())
                                                    .filter(mask::get)
                                                    .mapToObj(i -> type.members().get(i).name())
                                                    .collect(Collectors.toCollection(LinkedHashSet::new)))));
    }

    @AfterContainer
    static void cleanUp() throws IOException {
        if (generated != null) {
            generated.loader().close();
            try (Stream<Path> walk = Files.walk(generated.root())) {
                for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            }
            generated = null;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Populating builders.

    private static ShapeBuilder<?> populate(Generated g, Type type, Route path, Set<String> omitted) throws Exception {
        SerializableStruct complete = g.complete(type);
        switch (path) {
            case SETTERS -> {
                ShapeBuilder<?> builder = g.newBuilder(type);
                for (Member m : type.members()) {
                    if (!omitted.contains(m.name())) {
                        Object value = g.modelClass(type).getMethod(m.name()).invoke(complete);
                        invokeSetter(g, type, builder, m, value);
                    } else if (m.defaulted()) {
                        invokeSetter(g, type, builder, m, null);
                    }
                }
                return builder;
            }
            case SET_MEMBER_VALUE -> {
                ShapeBuilder<?> builder = g.newBuilder(type);
                for (Member m : type.members()) {
                    Schema member = builder.schema().member(m.name());
                    if (!omitted.contains(m.name())) {
                        builder.setMemberValue(member, complete.getMemberValue(member));
                    } else if (m.defaulted()) {
                        builder.setMemberValue(member, null);
                    }
                }
                return builder;
            }
            case WIRE -> {
                ShapeBuilder<?> builder = g.newBuilder(type);
                byte[] json = json(type, omitted).getBytes(StandardCharsets.UTF_8);
                builder.deserialize(JSON.createDeserializer(json));
                return builder;
            }
            case TO_BUILDER -> {
                ShapeBuilder<?> builder = (ShapeBuilder<?>) ((ToCopyableBuilder<?, ?>) complete).toBuilder();
                for (Member m : type.members()) {
                    if (omitted.contains(m.name())) {
                        invokeSetter(g, type, builder, m, null);
                    }
                }
                return builder;
            }
            default -> throw new AssertionError(path);
        }
    }

    /** Calls the named builder setter of {@code member} that accepts {@code value} (any overload for null). */
    private static void invokeSetter(Generated g, Type type, ShapeBuilder<?> builder, Member member, Object value)
        throws Exception {
        for (Method method : g.builderInterface(type).getMethods()) {
            if (method.getName().equals(member.name()) && method.getParameterCount() == 1
                && !method.getParameterTypes()[0].isArray()
                && (value == null || method.getParameterTypes()[0].isInstance(value))) {
                try {
                    method.invoke(builder, value);
                } catch (InvocationTargetException e) {
                    throw new AssertionError(type + "." + member.name() + "(" + value + ") failed", e.getCause());
                }
                return;
            }
        }
        fail("No setter " + member.name() + " of " + type + " accepts " + value);
    }

    /** A JSON object of every described member's wire value, without {@code omitted}. */
    private static String json(Type type, Set<String> omitted) {
        return type.members().stream()
                   .filter(m -> !omitted.contains(m.name()))
                   .map(m -> "\"" + m.name() + "\":" + m.kind().json)
                   .collect(Collectors.joining(",", "{", "}"));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The oracle of built values.

    /**
     * Asserts every member of {@code built}: a kept member has its complete value; an omitted member that is not
     * required at build time is absent; an omitted required member keeps its default when absent from the wire, and is
     * otherwise its zero value if error-corrected.
     */
    private static void assertValues(Generated g, Type type, Route path, Set<String> omitted, SerializableStruct built,
                                     boolean corrected, String where) {
        SerializableStruct complete = g.complete(type);
        for (Member m : type.members()) {
            Schema member = built.schema().member(m.name());
            Object actual = built.getMemberValue(member);
            String what = where + ": " + m.name();
            if (!omitted.contains(m.name())) {
                assertSameValue(complete.getMemberValue(member), actual, what);
            } else if (!m.requiredAtBuild()) {
                assertNull(actual, what);
            } else if (path == Route.WIRE && m.defaulted()) {
                assertEquals(m.defaultValue(), actual, what);
            } else if (corrected) {
                assertSameValue(zeroValue(g, m.kind()), actual, what);
            } else {
                fail(what + " is missing but " + type + " built");
            }
        }
    }

    /**
     * The Smithy client error-correction value of a missing required member of {@code kind}, as the generated type
     * stores it: enums hold their wire value; a structure is empty; a union has no member set; a document is null.
     */
    private static Object zeroValue(Generated g, Kind kind) {
        return switch (kind) {
            case STRING, ENUM -> "";
            case BOOLEAN -> false;
            case BYTE -> (byte) 0;
            case SHORT -> (short) 0;
            case INTEGER, INT_ENUM -> 0;
            case LONG -> 0L;
            case FLOAT -> 0f;
            case DOUBLE -> 0d;
            case BIG_INTEGER -> BigInteger.ZERO;
            case BIG_DECIMAL -> BigDecimal.ZERO;
            case BLOB -> SdkBytes.fromByteArray(new byte[0]);
            case TIMESTAMP -> Instant.EPOCH;
            case DOCUMENT -> null;
            case LIST -> List.of();
            case MAP -> Map.of();
            case STRUCTURE -> g.empty("Inner");
            case UNION -> g.empty("Choice");
        };
    }

    private static void assertSameValue(Object expected, Object actual, String what) {
        if (expected instanceof Document document && actual instanceof Document other) {
            assertEquals(JSON.serializeToString(document), JSON.serializeToString(other), what);
        } else if (expected instanceof SdkBytes bytes && actual instanceof SdkBytes other) {
            assertArrayEquals(bytes.asByteArray(), other.asByteArray(), what);
        } else {
            assertEquals(expected, actual, what);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The described model, generated and compiled once.

    private static Member req(String name, Kind kind) {
        return new Member(name, kind, true, false, null, null);
    }

    private static Member opt(String name, Kind kind) {
        return new Member(name, kind, false, false, null, null);
    }

    private static Member clientOptional(String name, Kind kind) {
        return new Member(name, kind, true, true, null, null);
    }

    private static Member reqDefault(String name, Kind kind, String literal, Object value) {
        return new Member(name, kind, true, false, literal, value);
    }

    /** The Smithy IDL of {@link #TYPES}, reachable from one operation's output and the service's errors. */
    private static String smithy() {
        StringBuilder out = new StringBuilder();
        out.append("$version: \"2\"\nnamespace ").append(NAMESPACE).append("\n\n")
           .append("@aws.protocols#awsJson1_0\n@aws.auth#sigv4(name: \"missing\")\n")
           .append("service Missing {\n    version: \"1\"\n    operations: [Get]\n    errors: [")
           .append(TYPES.stream().filter(Type::error).map(Type::name).collect(Collectors.joining(", ")))
           .append("]\n}\n\n")
           .append("operation Get {\n    input := {}\n    output := {\n");
        for (Type type : TYPES) {
            if (!type.error()) {
                out.append("        f").append(type.name()).append(": ").append(type.name()).append('\n');
            }
        }
        out.append("    }\n}\n\n");
        for (Type type : TYPES) {
            if (type.error()) {
                out.append("@error(\"client\")\n");
            }
            out.append("structure ").append(type.name()).append(" {\n");
            for (Member m : type.members()) {
                if (m.required()) {
                    out.append("    @required\n");
                }
                if (m.clientOptional()) {
                    out.append("    @clientOptional\n");
                }
                out.append("    ").append(m.name()).append(": ").append(m.kind().target);
                if (m.defaulted()) {
                    out.append(" = ").append(m.defaultLiteral());
                }
                out.append("\n\n");
            }
            out.append("}\n\n");
        }
        out.append("""
            enum Color {
                RED = "red"
                GREEN = "green"
            }

            intEnum Level {
                LOW = 1
                HIGH = 2
            }

            list StringList {
                member: String
            }

            map IntMap {
                key: String
                value: Integer
            }

            structure Inner {
                note: String
            }

            union Choice {
                leaf: String
                other: Integer
            }
            """);
        return out.toString();
    }

    private static synchronized Generated generated() {
        if (generated == null) {
            try {
                Path root = Files.createTempDirectory("missing-required-property10-");
                Path models = Files.createDirectories(root.resolve("model"));
                Files.writeString(models.resolve("missing.smithy"), smithy(), StandardCharsets.UTF_8);
                AwsCodegenResult result = AwsCodegen.generate(
                    new AwsCodegenRequest(NAMESPACE + "#Missing", PACKAGE, models, root.resolve("out")), List.of());
                CompiledSources compiled = CompiledSources.compile(result.sourceRoot(), root.resolve("classes"));
                generated = new Generated(root, compiled.loader(), new LinkedHashMap<>());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return generated;
    }

    /** The compiled model and the complete value of each described type (every member at its wire value). */
    private record Generated(Path root, URLClassLoader loader, Map<String, SerializableStruct> completes) {
        Class<?> modelClass(Type type) {
            return load(type.javaClass());
        }

        Class<?> builderInterface(Type type) {
            return load(type.javaClass() + "$Builder");
        }

        ShapeBuilder<?> newBuilder(Type type) {
            return newBuilder(type.javaClass());
        }

        ShapeBuilder<?> newBuilder(String className) {
            try {
                return (ShapeBuilder<?>) load(className).getMethod("builder").invoke(null);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }

        /** The value of a generated type built with no member set. */
        Object empty(String simpleName) {
            return newBuilder(MODEL_PACKAGE + simpleName).build();
        }

        synchronized SerializableStruct complete(Type type) {
            return completes.computeIfAbsent(type.name(), name -> {
                ShapeBuilder<?> builder = newBuilder(type);
                builder.deserialize(JSON.createDeserializer(json(type, Set.of()).getBytes(StandardCharsets.UTF_8)));
                SerializableStruct value = (SerializableStruct) builder.build();
                assertSame(modelClass(type), value.getClass());
                return value;
            });
        }

        private Class<?> load(String name) {
            try {
                return Class.forName(name, true, loader);
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);
            }
        }
    }
}
