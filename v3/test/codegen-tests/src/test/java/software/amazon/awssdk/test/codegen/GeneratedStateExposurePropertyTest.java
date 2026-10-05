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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.smithy.runtime.registry.RegisteredType;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels.ModelSpec;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.java.core.serde.MapSerializer;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.TimestampFormatter;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.RangeTrait;

/**
 * Property 8: generated values expose complete modeled state (Requirements 4.2, 4.3, 4.4, 4.8, 4.9).
 *
 * <p>A small fixed set of models (a hand-written model covering every member kind, plus seeded random models of
 * Property 5) is generated and compiled once. jqwik then generates random <em>instances</em> of every structure, union,
 * request, response and error of those models as a canonical member-map oracle: a {@code Map} from member name to
 * canonical value, independent of the generated code. Each instance is built through the schema-driven
 * {@code setMemberValue} of the generated builders, and then:
 * <ul>
 *   <li>{@code serializeMembers}, captured by a recording {@link ShapeSerializer}, emits exactly one event per present
 *       member, with the member's own schema, the write method of its type, and the oracle value (recursively);</li>
 *   <li>{@code getMemberValue} of every member equals the oracle (absent members read {@code null});</li>
 *   <li>{@code toBuilder().build()}, builder deserialization from a deserializer replaying the oracle, and a Smithy Java
 *       JSON codec round trip each reconstruct the same state, and (for non-error types) a value {@code equals} to the
 *       original with the same {@code hashCode}.</li>
 * </ul>
 *
 * <p>Canonical values: structures and unions are member-name maps (a union has exactly one entry, {@code Unit} is the
 * empty map), lists are lists, maps are string-keyed maps, enums and intEnums are their modeled {@code String} or
 * {@code Integer} value (known or unknown to the model), blobs are {@link ByteBuffer}s, documents are {@link Doc}s of
 * normalized JSON-like trees, and every other simple value is its boxed Java value. Absent members with a modeled
 * default are expected to hold the default.
 *
 * <p>Modeled errors are exceptions, whose equality is identity (as in V2); their state is compared through the
 * recorder instead of {@code equals}. JSON documents decode to Smithy Java JSON document classes, which are not
 * {@code equals} to the {@code Document.of} values they were written from, so {@code equals} after the JSON round trip
 * is asserted only for values without documents; their state is still compared through the recorder. Collection
 * length constraints are not honored (no generated type or codec validates them); string length/pattern and numeric
 * range constraints are.
 */
class GeneratedStateExposurePropertyTest {

    private static final int MAX_DEPTH = 3;
    private static final Object ABSENT = new Object() {
        @Override
        public String toString() {
            return "ABSENT";
        }
    };
    /**
     * The codec of the round trip. Timestamps are written as date-time, which is exact to the nanosecond: Smithy Java
     * 1.6.1 reads epoch-seconds numbers through {@code double} arithmetic that truncates, for example
     * {@code 512.035} to {@code 00:08:32.034Z}, a codec defect outside the generated types (Property 12's domain).
     */
    private static final JsonCodec JSON = JsonCodec.builder().useTimestampFormat(false)
                                                   .defaultTimestampFormat(TimestampFormatter.Prelude.DATE_TIME)
                                                   .build();

    private static final String RICH_MODEL = """
        $version: "2"
        namespace example.state

        @aws.protocols#awsJson1_0
        @aws.auth#sigv4(name: "state")
        service StateService {
            version: "1"
            operations: [Put]
            errors: [Invalid, Broken]
        }

        operation Put {
            input := {
                @required
                id: String
                scalars: Scalars
                tree: Tree
                chain: Chain
                collections: Collections
                defaults: Defaults
                choice: Choice
            }
            output := {
                @required
                choice: Choice
                doc: Document
                stamp: Timestamp
                defaults: Defaults
            }
        }

        structure Scalars {
            flag: Boolean
            tiny: Byte
            small: Short
            count: Integer
            total: Long
            ratio: Float
            precise: Double
            huge: BigInteger
            exact: BigDecimal
            text: String
            data: Blob
            epoch: Timestamp
            @timestampFormat("date-time")
            iso: Timestamp
            @timestampFormat("http-date")
            http: Timestamp
            doc: Document
            color: Color
            level: Level
            @range(min: -5, max: 5)
            bounded: Integer
            @length(min: 2, max: 4)
            @pattern("^[a-z0-9-]+$")
            slug: String
            @required
            requiredCount: Integer
            @required
            requiredColor: Color
        }

        enum Color {
            RED = "red"
            GREEN = "green"
            BLUE = "blue"
        }

        intEnum Level {
            LOW = 1
            HIGH = 10
        }

        structure Collections {
            strings: StringList
            sparse: SparseList
            unique: UniqueList
            colors: ColorList
            levels: LevelMap
            sparseStructs: SparseStructMap
            colorKeys: ColorKeyMap
            nested: NestedList
            nestedMap: NestedMap
            blobs: BlobList
            stamps: StampMap
            docs: DocList
            trees: TreeList
            choices: ChoiceMap
            levelList: LevelList
        }

        list StringList {
            member: String
        }

        @sparse
        list SparseList {
            member: Integer
        }

        @uniqueItems
        list UniqueList {
            member: String
        }

        list ColorList {
            member: Color
        }

        list LevelList {
            member: Level
        }

        map LevelMap {
            key: String
            value: Level
        }

        @sparse
        map SparseStructMap {
            key: String
            value: Scalars
        }

        map ColorKeyMap {
            key: Color
            value: Long
        }

        list NestedList {
            member: StringList
        }

        map NestedMap {
            key: String
            value: NestedList
        }

        list BlobList {
            member: Blob
        }

        map StampMap {
            key: String
            value: Timestamp
        }

        list DocList {
            member: Document
        }

        list TreeList {
            member: Tree
        }

        map ChoiceMap {
            key: String
            value: Choice
        }

        structure Tree {
            @required
            label: String
            children: TreeList
            parent: Tree
            choice: Choice
        }

        structure Chain {
            value: Integer
            next: Chain
        }

        union Choice {
            text: String
            number: Integer
            color: Color
            level: Level
            data: Blob
            stamp: Timestamp
            tree: Tree
            strings: StringList
            nested: Choice
            empty: Unit
            doc: Document
            big: BigDecimal
        }

        structure Defaults {
            flag: Boolean = true
            tiny: Byte = 1
            small: Short = -2
            count: Integer = 3
            total: Long = 9007199254740993
            ratio: Float = 1.5
            precise: Double = -2.25
            huge: BigInteger = 12345678901234567890
            exact: BigDecimal = 1.5
            text: String = "text"
            data: Blob = "aGVsbG8="
            stamp: Timestamp = 1
            doc: Document = "text"
            emptyDoc: Document = []
            color: Color = "green"
            level: Level = 10
            strings: StringList = []
            levels: LevelMap = {}

            @required
            requiredDefault: Integer = 0
        }

        @error("client")
        structure Invalid {
            message: String
            field: String
            reasons: StringList
            code: Integer
        }

        @error("server")
        structure Broken {
            message: Integer
            detail: Scalars
            choice: Choice
        }
        """;

    /** Seeds of the random models of Property 5 generated alongside the hand-written model. */
    private static final long[] RANDOM_MODEL_SEEDS = {3, 11, 17, 29};

    // Feature: aws-sdk-java-v3, Property 8: Generated values expose complete modeled state
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE)
    void generatedValuesExposeCompleteModeledState(@ForAll("instances") Instance instance) {
        Fixture fixture = instance.fixture();
        Schema schema = instance.schema();
        Map<String, Object> oracle = instance.members();
        boolean error = schema.hasTrait(TraitKey.ERROR_TRAIT);
        Statistics.label("fixture").collect(fixture.toString());
        Statistics.label("kind").collect(error ? "error" : schema.type().toString());

        SerializableStruct value = (SerializableStruct) fixture.toJava(schema, oracle);
        assertEquals(schema.id(), value.schema().id());

        // Direct serialization: one event per present member, with its schema and value.
        assertEquals(oracle, Recorder.members(value), "serializeMembers of " + schema.id());

        // Schema-member access of every member, present or absent.
        for (Schema member : stateMembers(schema)) {
            assertEquals(oracle.get(member.memberName()), canonical(member, value.getMemberValue(member)),
                         "getMemberValue(" + member.id() + ")");
        }
        assertNotNull(value.toString());

        // Copy builder.
        SerializableStruct copy = (SerializableStruct) ((ShapeBuilder<?>) invoke(value, "toBuilder")).build();
        assertSameState(value, copy, oracle, error, "toBuilder().build()");

        // Builder deserialization from a deserializer replaying the oracle.
        SerializableStruct replayed = (SerializableStruct) fixture.registry().createBuilder(schema.id())
                                                                  .deserialize(new OracleDeserializer(oracle))
                                                                  .build();
        assertSameState(value, replayed, oracle, error, "oracle deserialization");

        // Protocol codec round trip. The JSON codec writes a negative zero as 0, so zeros are compared unsigned.
        ByteBuffer json = JSON.serialize(value);
        String text = StandardCharsets.UTF_8.decode(json.duplicate()).toString();
        SerializableStruct decoded = (SerializableStruct) JSON.deserializeShape(
            json, fixture.registry().createBuilder(schema.id()));
        Object unsigned = withoutNegativeZero(oracle);
        assertEquals(unsigned, withoutNegativeZero(Recorder.members(decoded)), "JSON round trip of " + text);
        if (!error && !containsDocument(oracle) && unsigned.equals(oracle)) {
            assertEquals(value, decoded, "JSON round trip of " + text);
            assertEquals(value.hashCode(), decoded.hashCode(), "JSON round trip of " + text);
        }
    }

    @Provide
    Arbitrary<Instance> instances() {
        return Arbitraries.of(Fixtures.TARGETS)
                          .flatMap(target -> target.fixture().value(target.schema(), 0)
                                                   .map(members -> new Instance(target.fixture(), target.schema(),
                                                                                castMap(members))));
    }

    private static void assertSameState(SerializableStruct original, SerializableStruct other,
                                        Map<String, Object> oracle, boolean error, String description) {
        assertEquals(oracle, Recorder.members(other), description);
        if (!error) {
            assertEquals(original, other, description);
            assertEquals(original.hashCode(), other.hashCode(), description);
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Fixtures: models generated and compiled once.

    /** An exported aggregate schema of a compiled fixture. */
    record Target(Fixture fixture, Schema schema) {
        @Override
        public String toString() {
            return schema.id().toString();
        }
    }

    /** One generated instance: the canonical member map of an aggregate schema. */
    record Instance(Fixture fixture, Schema schema, Map<String, Object> members) {
        @Override
        public String toString() {
            return schema.id() + " " + members;
        }
    }

    /** A normalized document: {@code String}, {@code Boolean}, stripped {@code BigDecimal}, list or string map. */
    record Doc(Object value) {
    }

    private static final class Fixtures {
        static final List<Target> TARGETS = load();

        private static List<Target> load() {
            try {
                Path work = Files.createTempDirectory(Files.createDirectories(Path.of("target", "state-exposure")),
                                                      "fixtures-");
                List<Target> targets = new ArrayList<>();
                Fixture rich = compile(work.resolve("rich"), "example.state#StateService",
                                       "software.amazon.awssdk.services.state", Map.of("state.smithy", RICH_MODEL));
                assertCoversEveryMemberKind(rich);
                rich.aggregates().forEach(s -> targets.add(new Target(rich, s)));
                for (long seed : RANDOM_MODEL_SEEDS) {
                    Random random = new Random(seed);
                    ModelSpec spec = RandomSmithyModels.model(random, 1 + random.nextInt(4), 6 + random.nextInt(10),
                                                              random.nextInt(2), 1 + random.nextInt(3),
                                                              random.nextBoolean());
                    Fixture fixture = compile(work.resolve("seed-" + seed), spec.service().toString(),
                                              spec.packageName(), RandomSmithyModels.render(spec, random));
                    fixture.aggregates().forEach(s -> targets.add(new Target(fixture, s)));
                }
                return List.copyOf(targets);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static Fixture compile(Path dir, String service, String packageName, Map<String, String> files)
            throws IOException {
            Path models = Files.createDirectories(dir.resolve("model"));
            for (Map.Entry<String, String> file : files.entrySet()) {
                Path target = models.resolve(file.getKey());
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.getValue(), StandardCharsets.UTF_8);
            }
            AwsCodegenResult result = AwsCodegen.generate(
                new AwsCodegenRequest(service, packageName, models, dir.resolve("out")), List.of());
            Path classes = dir.resolve("classes");
            URLClassLoader loader = CompiledSources.compile(result.sourceRoot(), classes).loader();
            String registryClass;
            try (Stream<Path> walk = Files.walk(classes)) {
                registryClass = walk.map(p -> classes.relativize(p).toString().replace('\\', '/'))
                                    .filter(p -> p.endsWith("TypeRegistry.class") && p.contains("/model/internal/")
                                                 && !p.contains("$"))
                                    .map(p -> p.substring(0, p.length() - ".class".length()).replace('/', '.'))
                                    .findFirst()
                                    .orElseThrow(() -> new AssertionError("No type registry under " + classes));
            }
            SchemaTypeRegistry registry = (SchemaTypeRegistry) CompiledSources.invokeStatic(loader, registryClass,
                                                                                             "registry");
            return new Fixture(service, registry);
        }

        /** The hand-written model reaches a member of every modeled kind the generated types hold. */
        private static void assertCoversEveryMemberKind(Fixture rich) {
            Set<ShapeType> kinds = EnumSet.noneOf(ShapeType.class);
            for (Schema aggregate : rich.aggregates()) {
                for (Schema member : aggregate.members()) {
                    kinds.add(member.type());
                }
            }
            Set<ShapeType> expected = EnumSet.of(
                ShapeType.BOOLEAN, ShapeType.BYTE, ShapeType.SHORT, ShapeType.INTEGER, ShapeType.LONG, ShapeType.FLOAT,
                ShapeType.DOUBLE, ShapeType.BIG_INTEGER, ShapeType.BIG_DECIMAL, ShapeType.STRING, ShapeType.BLOB,
                ShapeType.TIMESTAMP, ShapeType.DOCUMENT, ShapeType.ENUM, ShapeType.INT_ENUM, ShapeType.LIST,
                ShapeType.MAP, ShapeType.STRUCTURE, ShapeType.UNION);
            assertTrue(kinds.containsAll(expected), "member kinds " + kinds);
        }
    }

    /** A compiled model: its type registry, and the memoized instance generators of its schemas. */
    static final class Fixture {
        private final String name;
        private final SchemaTypeRegistry registry;
        private final Map<String, Arbitrary<Object>> generators = new HashMap<>();

        Fixture(String name, SchemaTypeRegistry registry) {
            this.name = name;
            this.registry = registry;
        }

        SchemaTypeRegistry registry() {
            return registry;
        }

        List<Schema> aggregates() {
            return registry.entries().stream().map(RegisteredType::schema).toList();
        }

        @Override
        public String toString() {
            return name;
        }

        /** Builds the generated Java value of canonical {@code value} of {@code schema} through the builders. */
        Object toJava(Schema schema, Object value) {
            if (value == null) {
                return null;
            }
            Schema target = target(schema);
            return switch (target.type()) {
                case STRUCTURE, UNION -> {
                    if (target.id().equals(Unit.ID)) {
                        yield Unit.getInstance();
                    }
                    ShapeBuilder<?> builder = registry.createBuilder(target.id());
                    for (Map.Entry<String, Object> member : castMap(value).entrySet()) {
                        Schema memberSchema = target.member(member.getKey());
                        builder.setMemberValue(memberSchema, toJava(memberSchema, member.getValue()));
                    }
                    yield builder.build();
                }
                case LIST, SET -> {
                    List<Object> list = new ArrayList<>();
                    ((List<?>) value).forEach(e -> list.add(toJava(target.listMember(), e)));
                    yield list;
                }
                case MAP -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    castMap(value).forEach((k, v) -> map.put(k, toJava(target.mapValueMember(), v)));
                    yield map;
                }
                case BLOB -> SdkBytes.fromByteArray(bytes((ByteBuffer) value));
                case DOCUMENT -> toDocument(((Doc) value).value());
                default -> value;
            };
        }

        /**
         * Canonical values of {@code schema} at nesting {@code depth}; for an aggregate, its member map. Beyond
         * {@link #MAX_DEPTH}, optional members are absent, collections empty and unions prefer simple members.
         */
        Arbitrary<Object> value(Schema schema, int depth) {
            String key = schema.id() + "@" + Math.min(depth, MAX_DEPTH + 1);
            Arbitrary<Object> generator = generators.get(key);
            if (generator == null) {
                generator = createValue(schema, depth);
                generators.put(key, generator);
            }
            return generator;
        }

        private Arbitrary<Object> createValue(Schema schema, int depth) {
            Schema target = target(schema);
            boolean deep = depth >= MAX_DEPTH;
            return switch (target.type()) {
                case BOOLEAN -> Arbitraries.of(true, false).asGeneric();
                case BYTE -> Arbitraries.bytes().asGeneric();
                case SHORT -> Arbitraries.shorts().asGeneric();
                case INTEGER -> ints(schema);
                case LONG -> longs(schema);
                case FLOAT -> Arbitraries.frequencyOf(List.of(
                    Tuple.of(8, Arbitraries.floats().asGeneric()),
                    Tuple.of(1, Arbitraries.of(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -0.0f,
                                               Float.MIN_VALUE, Float.MAX_VALUE).asGeneric())));
                case DOUBLE -> Arbitraries.frequencyOf(List.of(
                    Tuple.of(8, Arbitraries.doubles().asGeneric()),
                    Tuple.of(1, Arbitraries.of(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.0d,
                                               Double.MIN_VALUE, Double.MAX_VALUE).asGeneric())));
                case BIG_INTEGER -> bigIntegers().asGeneric();
                case BIG_DECIMAL -> Combinators.combine(bigIntegers(), Arbitraries.integers().between(0, 8))
                                               .as((unscaled, scale) -> (Object) new BigDecimal(unscaled, scale));
                case STRING -> strings(schema).asGeneric();
                case ENUM -> Arbitraries.frequencyOf(List.of(
                    Tuple.of(3, Arbitraries.of(List.copyOf(target.stringEnumValues())).asGeneric()),
                    Tuple.of(1, Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(6)
                                           .filter(v -> !target.stringEnumValues().contains(v)).asGeneric())));
                case INT_ENUM -> Arbitraries.frequencyOf(List.of(
                    Tuple.of(3, Arbitraries.of(List.copyOf(target.intEnumValues())).asGeneric()),
                    Tuple.of(1, Arbitraries.integers().filter(v -> !target.intEnumValues().contains(v))
                                           .asGeneric())));
                case BLOB -> Arbitraries.bytes().array(byte[].class).ofMaxSize(8).map(v -> (Object) ByteBuffer.wrap(v));
                case TIMESTAMP -> timestamps();
                case DOCUMENT -> documents(deep ? 0 : 2).map(Doc::new);
                case LIST, SET -> {
                    if (deep) {
                        yield Arbitraries.just((Object) List.of());
                    }
                    Arbitrary<Object> element = value(target.listMember(), depth + 1);
                    if (target.hasTrait(TraitKey.SPARSE_TRAIT)) {
                        element = element.injectNull(0.2);
                    }
                    var list = element.list().ofMaxSize(3);
                    if (target.hasTrait(TraitKey.UNIQUE_ITEMS_TRAIT)) {
                        list = list.uniqueElements();
                    }
                    yield list.asGeneric();
                }
                case MAP -> {
                    if (deep) {
                        yield Arbitraries.just((Object) Map.of());
                    }
                    Arbitrary<Object> entryValue = value(target.mapValueMember(), depth + 1);
                    if (target.hasTrait(TraitKey.SPARSE_TRAIT)) {
                        entryValue = entryValue.injectNull(0.2);
                    }
                    Arbitrary<String> keys = value(target.mapKeyMember(), depth + 1).map(String.class::cast);
                    yield Arbitraries.maps(keys, entryValue).ofMaxSize(3).map(m -> (Object) new LinkedHashMap<>(m));
                }
                case STRUCTURE -> target.id().equals(Unit.ID) ? Arbitraries.just((Object) new LinkedHashMap<String, Object>())
                                                              : structure(target, depth);
                case UNION -> union(target, depth);
                default -> throw new AssertionError("Unsupported member " + schema.id() + " of type " + target.type());
            };
        }

        private Arbitrary<Object> structure(Schema target, int depth) {
            List<Schema> members = stateMembers(target);
            if (members.isEmpty()) {
                return Arbitraries.just((Object) new LinkedHashMap<String, Object>());
            }
            List<Arbitrary<Object>> values = new ArrayList<>();
            for (Schema member : members) {
                Object fallback = member.hasTrait(TraitKey.DEFAULT_TRAIT)
                                  ? defaultValue(member, member.expectTrait(TraitKey.DEFAULT_TRAIT).toNode())
                                  : ABSENT;
                Arbitrary<Object> absent = Arbitraries.just(fallback);
                if (member.hasTrait(TraitKey.REQUIRED_TRAIT)) {
                    values.add(value(member, depth + 1));
                } else if (depth >= MAX_DEPTH) {
                    values.add(absent);
                } else {
                    values.add(Arbitraries.frequencyOf(List.of(Tuple.of(1, absent), Tuple.of(2, value(member, depth + 1)))));
                }
            }
            return Combinators.combine(values).as(list -> {
                Map<String, Object> map = new LinkedHashMap<>();
                for (int i = 0; i < members.size(); i++) {
                    if (list.get(i) != ABSENT) {
                        map.put(members.get(i).memberName(), list.get(i));
                    }
                }
                return map;
            });
        }

        private Arbitrary<Object> union(Schema target, int depth) {
            List<Schema> members = stateMembers(target);
            if (depth >= MAX_DEPTH) {
                List<Schema> simple = members.stream().filter(m -> !aggregateOrCollection(m.type())).toList();
                if (!simple.isEmpty()) {
                    members = simple;
                }
            }
            List<Arbitrary<? extends Object>> choices = new ArrayList<>();
            for (Schema member : members) {
                choices.add(value(member, depth + 1).map(v -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put(member.memberName(), v);
                    return map;
                }));
            }
            return Arbitraries.oneOf(choices);
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Simple value generators.

    private static Arbitrary<Object> ints(Schema schema) {
        RangeTrait range = schema.getTrait(TraitKey.RANGE_TRAIT);
        int min = range == null ? Integer.MIN_VALUE : range.getMin().map(BigDecimal::intValue).orElse(Integer.MIN_VALUE);
        int max = range == null ? Integer.MAX_VALUE : range.getMax().map(BigDecimal::intValue).orElse(Integer.MAX_VALUE);
        return Arbitraries.integers().between(min, max).asGeneric();
    }

    private static Arbitrary<Object> longs(Schema schema) {
        RangeTrait range = schema.getTrait(TraitKey.RANGE_TRAIT);
        long min = range == null ? Long.MIN_VALUE : range.getMin().map(BigDecimal::longValue).orElse(Long.MIN_VALUE);
        long max = range == null ? Long.MAX_VALUE : range.getMax().map(BigDecimal::longValue).orElse(Long.MAX_VALUE);
        return Arbitraries.longs().between(min, max).asGeneric();
    }

    private static Arbitrary<BigInteger> bigIntegers() {
        BigInteger bound = BigInteger.TEN.pow(30);
        return Arbitraries.bigIntegers().between(bound.negate(), bound);
    }

    private static Arbitrary<String> strings(Schema schema) {
        LengthTrait length = schema.getTrait(TraitKey.LENGTH_TRAIT);
        int min = length == null ? 0 : length.getMin().map(Long::intValue).orElse(0);
        int max = length == null ? 8 : length.getMax().map(v -> (int) Math.min(v, min + 8L)).orElse(min + 8);
        if (schema.hasTrait(TraitKey.PATTERN_TRAIT)) {
            // The only pattern of the models: ^[a-z0-9-]+$.
            return Arbitraries.strings().withCharRange('a', 'z').withCharRange('0', '9').withChars('-')
                              .ofMinLength(Math.max(1, min)).ofMaxLength(Math.max(1, max));
        }
        return Arbitraries.strings().withCharRange(' ', '~').withChars('\n', '\t', '\u00e9', '\u2603', '\u4e2d')
                          .ofMinLength(min).ofMaxLength(max);
    }

    private static Arbitrary<Object> timestamps() {
        // 1970 to 2100, in milliseconds.
        return Arbitraries.longs().between(0, 4_102_444_800_000L).map(Instant::ofEpochMilli).asGeneric();
    }

    /** Normalized JSON-representable document trees (Smithy Java JSON documents do not keep other kinds). */
    private static Arbitrary<Object> documents(int depth) {
        List<Tuple.Tuple2<Integer, Arbitrary<Object>>> kinds = new ArrayList<>(List.of(
            Tuple.of(2, Arbitraries.strings().alpha().ofMaxLength(5).asGeneric()),
            Tuple.of(1, Arbitraries.of(true, false).asGeneric()),
            Tuple.of(2, Arbitraries.longs().map(v -> (Object) jsonExactNumber(v))),
            Tuple.of(1, Arbitraries.integers().between(-100_000, 100_000)
                                   .map(v -> (Object) BigDecimal.valueOf(v, 2).stripTrailingZeros()))));
        if (depth > 0) {
            Arbitrary<Object> nested = Arbitraries.lazy(() -> documents(depth - 1));
            kinds.add(Tuple.of(1, nested.list().ofMaxSize(3).asGeneric()));
            kinds.add(Tuple.of(1, Arbitraries.maps(Arbitraries.strings().alpha().ofMaxLength(4), nested).ofMaxSize(3)
                                             .map(m -> (Object) new LinkedHashMap<>(m))));
        }
        return Arbitraries.frequencyOf(kinds);
    }

    /**
     * {@code v} as a normalized document number that Smithy Java's JSON codec round-trips exactly. A stripped value
     * with trailing zeros is written in exponent form ({@code 1.3600540588824127E+17}), which the codec reads back as a
     * {@code double}; such values are rounded to 15 significant digits, which every {@code double} preserves, so the
     * exponent form is still exercised. Values in plain notation are read back exactly at any size.
     */
    private static BigDecimal jsonExactNumber(long v) {
        BigDecimal value = BigDecimal.valueOf(v).stripTrailingZeros();
        return value.scale() < 0 && value.precision() > 15
               ? value.round(new MathContext(15)).stripTrailingZeros() : value;
    }

    /** The canonical value of the modeled default {@code node} of {@code member}. */
    private static Object defaultValue(Schema member, Node node) {
        Schema target = target(member);
        return switch (target.type()) {
            case BOOLEAN -> node.expectBooleanNode().getValue();
            case BYTE -> decimal(node).byteValueExact();
            case SHORT -> decimal(node).shortValueExact();
            case INTEGER, INT_ENUM -> decimal(node).intValueExact();
            case LONG -> decimal(node).longValueExact();
            case FLOAT -> decimal(node).floatValue();
            case DOUBLE -> decimal(node).doubleValue();
            case BIG_INTEGER -> decimal(node).toBigIntegerExact();
            case BIG_DECIMAL -> decimal(node);
            case STRING, ENUM -> node.expectStringNode().getValue();
            case BLOB -> ByteBuffer.wrap(Base64.getDecoder().decode(node.expectStringNode().getValue()));
            case TIMESTAMP -> {
                if (node.isStringNode()) {
                    yield Instant.parse(node.expectStringNode().getValue());
                }
                BigDecimal value = decimal(node);
                BigDecimal whole = value.setScale(0, RoundingMode.FLOOR);
                yield Instant.ofEpochSecond(whole.longValueExact(),
                                            value.subtract(whole).movePointRight(9).intValueExact());
            }
            case DOCUMENT -> new Doc(normalize(node));
            case LIST, SET -> List.of();
            case MAP -> new LinkedHashMap<>();
            default -> throw new AssertionError("Unsupported default of " + member.id());
        };
    }

    private static BigDecimal decimal(Node node) {
        return new BigDecimal(node.expectNumberNode().getValue().toString());
    }

    private static Object normalize(Node node) {
        if (node.isStringNode()) {
            return node.expectStringNode().getValue();
        } else if (node.isBooleanNode()) {
            return node.expectBooleanNode().getValue();
        } else if (node.isNumberNode()) {
            return decimal(node).stripTrailingZeros();
        } else if (node.isArrayNode()) {
            return node.expectArrayNode().getElements().stream().map(GeneratedStateExposurePropertyTest::normalize)
                       .toList();
        } else if (node.isObjectNode()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.expectObjectNode().getStringMap().forEach((k, v) -> map.put(k, normalize(v)));
            return map;
        }
        throw new AssertionError("Unsupported document default " + node);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Canonical form of generated Java values and documents.

    /** The canonical form of the value {@code getMemberValue} returns for {@code member}. */
    private static Object canonical(Schema member, Object value) {
        if (value == null) {
            return null;
        }
        Schema target = target(member);
        return switch (target.type()) {
            case STRUCTURE, UNION -> Recorder.members((SerializableStruct) value);
            case LIST, SET -> {
                List<Object> list = new ArrayList<>();
                ((Collection<?>) value).forEach(e -> list.add(canonical(target.listMember(), e)));
                yield list;
            }
            case MAP -> {
                Map<String, Object> map = new LinkedHashMap<>();
                ((Map<?, ?>) value).forEach((k, v) -> map.put((String) k, canonical(target.mapValueMember(), v)));
                yield map;
            }
            case BLOB -> ByteBuffer.wrap(((SdkBytes) value).asByteArray());
            case DOCUMENT -> new Doc(normalize((Document) value));
            default -> value;
        };
    }

    private static Object normalize(Document document) {
        if (document == null) {
            return null;
        }
        return switch (document.type()) {
            case STRING -> document.asString();
            case BOOLEAN -> document.asBoolean();
            case BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL ->
                new BigDecimal(document.asNumber().toString()).stripTrailingZeros();
            case LIST -> {
                List<Object> list = new ArrayList<>();
                document.asList().forEach(e -> list.add(normalize(e)));
                yield list;
            }
            case MAP, STRUCTURE -> {
                Map<String, Object> map = new LinkedHashMap<>();
                document.asStringMap().forEach((k, v) -> map.put(k, normalize(v)));
                yield map;
            }
            default -> throw new AssertionError("Unexpected document " + document);
        };
    }

    private static Document toDocument(Object normalized) {
        if (normalized instanceof String s) {
            return Document.of(s);
        } else if (normalized instanceof Boolean b) {
            return Document.of(b);
        } else if (normalized instanceof BigDecimal d) {
            return Document.of(d);
        } else if (normalized instanceof List<?> list) {
            return Document.of(list.stream().map(GeneratedStateExposurePropertyTest::toDocument).toList());
        } else if (normalized instanceof Map<?, ?> map) {
            Map<String, Document> members = new LinkedHashMap<>();
            map.forEach((k, v) -> members.put((String) k, toDocument(v)));
            return Document.of(members);
        }
        throw new AssertionError("Unexpected document value " + normalized);
    }

    /** {@code canonical} with every negative floating-point zero replaced by a positive zero. */
    private static Object withoutNegativeZero(Object canonical) {
        if (canonical instanceof Float f && f == 0.0f) {
            return 0.0f;
        } else if (canonical instanceof Double d && d == 0.0d) {
            return 0.0d;
        } else if (canonical instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(k, withoutNegativeZero(v)));
            return copy;
        } else if (canonical instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            list.forEach(e -> copy.add(withoutNegativeZero(e)));
            return copy;
        }
        return canonical;
    }

    private static boolean containsDocument(Object canonical) {
        if (canonical instanceof Doc) {
            return true;
        } else if (canonical instanceof Map<?, ?> map) {
            return map.values().stream().anyMatch(GeneratedStateExposurePropertyTest::containsDocument);
        } else if (canonical instanceof List<?> list) {
            return list.stream().anyMatch(GeneratedStateExposurePropertyTest::containsDocument);
        }
        return false;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Recording serializer.

    /**
     * A {@link ShapeSerializer} that turns the events of {@code serializeMembers} into canonical values, checking that
     * each event uses the expected schema and the write method of the schema's type.
     */
    private abstract static class Recorder implements ShapeSerializer {

        /** The canonical member map of {@code struct}, from its direct serialization. */
        static Map<String, Object> members(SerializableStruct struct) {
            Schema schema = struct.schema();
            Map<String, Object> members = new LinkedHashMap<>();
            struct.serializeMembers(new Recorder() {
                @Override
                void accept(Schema member, Object value) {
                    Schema expected = member.isMember() ? schema.member(member.memberName()) : null;
                    assertEquals(expected, member, "event schema " + member.id() + " is not a member of "
                                                   + schema.id());
                    assertTrue(!members.containsKey(member.memberName()), "second event for " + member.id());
                    members.put(member.memberName(), value);
                }
            });
            return members;
        }

        abstract void accept(Schema schema, Object value);

        private static void expectType(Schema schema, ShapeType... types) {
            assertTrue(List.of(types).contains(schema.type()),
                       "write of " + List.of(types) + " for " + schema.id() + " of type " + schema.type());
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            expectType(schema, ShapeType.STRUCTURE, ShapeType.UNION);
            assertEquals(target(schema).id(), struct.schema().id(), "struct written for " + schema.id());
            accept(schema, members(struct));
        }

        @Override
        public <T> void writeList(Schema schema, T list, int size, BiConsumer<T, ShapeSerializer> consumer) {
            expectType(schema, ShapeType.LIST, ShapeType.SET);
            List<Object> elements = new ArrayList<>();
            consumer.accept(list, new Recorder() {
                @Override
                void accept(Schema element, Object value) {
                    assertEquals(schema.listMember(), element, "element schema of " + schema.id());
                    elements.add(value);
                }
            });
            assertEquals(size, elements.size(), "list size of " + schema.id());
            accept(schema, elements);
        }

        @Override
        public <T> void writeMap(Schema schema, T map, int size, BiConsumer<T, MapSerializer> consumer) {
            expectType(schema, ShapeType.MAP);
            Map<String, Object> entries = new LinkedHashMap<>();
            consumer.accept(map, new MapSerializer() {
                @Override
                public <U> void writeEntry(Schema key, String name, U state, BiConsumer<U, ShapeSerializer> writer) {
                    assertEquals(schema.mapKeyMember(), key, "key schema of " + schema.id());
                    assertTrue(!entries.containsKey(name), "second entry " + name + " of " + schema.id());
                    List<Object> written = new ArrayList<>();
                    writer.accept(state, new Recorder() {
                        @Override
                        void accept(Schema entryValue, Object value) {
                            assertEquals(schema.mapValueMember(), entryValue, "value schema of " + schema.id());
                            written.add(value);
                        }
                    });
                    assertEquals(1, written.size(), "events for entry " + name + " of " + schema.id());
                    entries.put(name, written.get(0));
                }
            });
            assertEquals(size, entries.size(), "map size of " + schema.id());
            accept(schema, entries);
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            expectType(schema, ShapeType.BOOLEAN);
            accept(schema, value);
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            expectType(schema, ShapeType.BYTE);
            accept(schema, value);
        }

        @Override
        public void writeShort(Schema schema, short value) {
            expectType(schema, ShapeType.SHORT);
            accept(schema, value);
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            expectType(schema, ShapeType.INTEGER, ShapeType.INT_ENUM);
            accept(schema, value);
        }

        @Override
        public void writeLong(Schema schema, long value) {
            expectType(schema, ShapeType.LONG);
            accept(schema, value);
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            expectType(schema, ShapeType.FLOAT);
            accept(schema, value);
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            expectType(schema, ShapeType.DOUBLE);
            accept(schema, value);
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            expectType(schema, ShapeType.BIG_INTEGER);
            accept(schema, value);
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            expectType(schema, ShapeType.BIG_DECIMAL);
            accept(schema, value);
        }

        @Override
        public void writeString(Schema schema, String value) {
            expectType(schema, ShapeType.STRING, ShapeType.ENUM);
            accept(schema, value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            expectType(schema, ShapeType.BLOB);
            accept(schema, ByteBuffer.wrap(bytes(value)));
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            expectType(schema, ShapeType.TIMESTAMP);
            accept(schema, value);
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            expectType(schema, ShapeType.DOCUMENT);
            accept(schema, new Doc(normalize(value)));
        }

        @Override
        public void writeNull(Schema schema) {
            accept(schema, null);
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Replaying deserializer.

    /** A {@link ShapeDeserializer} reading one canonical value (a member map for aggregates). */
    private static final class OracleDeserializer implements ShapeDeserializer {
        private final Object value;

        OracleDeserializer(Object value) {
            this.value = value;
        }

        @Override
        public boolean readBoolean(Schema schema) {
            return (Boolean) value;
        }

        @Override
        public ByteBuffer readBlob(Schema schema) {
            return ((ByteBuffer) value).duplicate();
        }

        @Override
        public byte readByte(Schema schema) {
            return (Byte) value;
        }

        @Override
        public short readShort(Schema schema) {
            return (Short) value;
        }

        @Override
        public int readInteger(Schema schema) {
            return (Integer) value;
        }

        @Override
        public long readLong(Schema schema) {
            return (Long) value;
        }

        @Override
        public float readFloat(Schema schema) {
            return (Float) value;
        }

        @Override
        public double readDouble(Schema schema) {
            return (Double) value;
        }

        @Override
        public BigInteger readBigInteger(Schema schema) {
            return (BigInteger) value;
        }

        @Override
        public BigDecimal readBigDecimal(Schema schema) {
            return (BigDecimal) value;
        }

        @Override
        public String readString(Schema schema) {
            return (String) value;
        }

        @Override
        public Document readDocument() {
            return toDocument(((Doc) value).value());
        }

        @Override
        public Instant readTimestamp(Schema schema) {
            return (Instant) value;
        }

        @Override
        public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
            Schema target = target(schema);
            castMap(value).forEach((name, member) -> {
                Schema memberSchema = target.member(name);
                assertNotNull(memberSchema, name + " of " + target.id());
                consumer.accept(state, memberSchema, new OracleDeserializer(member));
            });
        }

        @Override
        public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
            ((List<?>) value).forEach(e -> consumer.accept(state, new OracleDeserializer(e)));
        }

        @Override
        public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
            castMap(value).forEach((k, v) -> consumer.accept(state, k, new OracleDeserializer(v)));
        }

        @Override
        public int containerSize() {
            return value instanceof Collection<?> c ? c.size() : value instanceof Map<?, ?> m ? m.size() : -1;
        }

        @Override
        public boolean isNull() {
            return value == null;
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Utilities.

    private static Schema target(Schema schema) {
        return schema.isMember() ? schema.memberTarget() : schema;
    }

    /** The members held as state: every member except {@code @streaming} ones, which are bound to the call. */
    private static List<Schema> stateMembers(Schema aggregate) {
        return aggregate.members().stream().filter(m -> !m.hasTrait(TraitKey.STREAMING_TRAIT)).toList();
    }

    private static boolean aggregateOrCollection(ShapeType type) {
        return switch (type) {
            case STRUCTURE, UNION, LIST, SET, MAP -> true;
            default -> false;
        };
    }

    private static byte[] bytes(ByteBuffer buffer) {
        ByteBuffer copy = buffer.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        return bytes;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static Object invoke(Object target, String method) {
        try {
            var accessor = target.getClass().getMethod(method);
            accessor.setAccessible(true);
            return accessor.invoke(target);
        } catch (InvocationTargetException e) {
            throw new AssertionError(method + "() of " + target.getClass().getName() + " failed", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

}
