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

package software.amazon.awssdk.smithy.runtime.registry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.smithy.runtime.document.DocumentCoercionException;
import software.amazon.awssdk.smithy.runtime.document.DocumentCoercionException.Reason;
import software.amazon.awssdk.smithy.runtime.document.SchemaDocuments;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Property 18 (Requirements 6.10-6.14): for any incompatible document kind and schema, absent discriminator, unknown
 * discriminator, conflicting registry entries or discriminator/schema mismatch, the same input produces the same typed
 * error containing every required source kind, shape ID and conflicting type.
 *
 * <p>Each try draws one of five failure families. Every family runs its operation twice on the same input and compares
 * both errors to a structured oracle computed independently of the code under test: the exception type, every
 * structured accessor and the exact message (rebuilt from the documented message formats).
 * <ul>
 *     <li><b>Kinds (6.10).</b> A document of one of eighteen source kinds (untyped Smithy Java documents and modeled
 *         enum and intEnum documents) is coerced to one of nineteen target schemas, nested zero to two levels deep in
 *         structure members, list elements and map values. The oracle's compatibility table follows the documented
 *         rules: booleans read booleans, strings read strings and enums, numbers read any number (all generated
 *         numbers are 7, which every numeric type can hold), blobs, timestamps, lists and maps read their own kind,
 *         structures and unions read structure, union and map documents, and documents read anything. A compatible
 *         pair must coerce; an incompatible pair fails with {@code INCOMPATIBLE_KIND}, the source kind, the requested
 *         (target) shape and the nested path. Protocol representations (a string blob, a string or number timestamp,
 *         a map document as a union) are {@code INVALID_REPRESENTATION} or protocol-specific territory and are not
 *         generated.</li>
 *     <li><b>Absent discriminator (6.11).</b> A registry's {@code deserialize} and {@code deserializeStrict} of an
 *         undiscriminated document fail with {@link MissingDiscriminatorException} naming its kind.</li>
 *     <li><b>Unknown discriminator (6.12).</b> A document discriminated (as a JSON {@code __type}, a modeled document
 *         or a Smithy Java typed document) with a shape ID the registry does not register, including one that differs
 *         from a registered ID only by namespace, fails with {@link UnknownDiscriminatorException} naming it.</li>
 *     <li><b>Conflicts (6.13).</b> Two to four registries register overlapping IDs whose entries vary in schema, class
 *         and factory. The oracle lists, sorted by shape ID and registry positions, every later registration whose
 *         schema, class or factory object differs from the first registration's, with both registry descriptions,
 *         both entries, both Java types and the differing components. No expected conflict means composition
 *         succeeds.</li>
 *     <li><b>Mismatch (6.14).</b> A document discriminated with structure ID A, coerced (or, at the root, built
 *         through a builder) as a different structure or union B, fails with {@code DISCRIMINATOR_MISMATCH} naming A,
 *         B, the source kind and the path.</li>
 * </ul>
 */
class RegistryAndCoercionFailurePropertyTest {

    private static final JsonCodec JSON = JsonCodec.builder().build();
    private static final List<String> NAMESPACES = List.of("com.example", "org.other");
    private static final List<String> NAMES = List.of("Alpha", "Beta", "Gamma", "Delta");
    private static final List<ShapeId> POOL =
        NAMESPACES.stream().flatMap(ns -> NAMES.stream().map(n -> ShapeId.fromParts(ns, n))).toList();
    private static final List<Class<? extends Value>> CLASSES = List.of(ValueA.class, ValueB.class);
    private static final List<Function<Schema, Value>> CONSTRUCTORS = List.of(ValueA::new, ValueB::new);

    // Feature: aws-sdk-java-v3, Property 18: Registry and coercion failures are deterministic
    // Embedded edge cases are disabled: they collapse the families to their first alternatives. The statistics
    // coverage checks below fail the property if a family or outcome stops being generated.
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 18: Registry and coercion failures are deterministic")
    void registryAndCoercionFailuresAreDeterministic(@ForAll("cases") Case failureCase) {
        Statistics.label("family").collect(failureCase.getClass().getSimpleName());
        Statistics.label("family").coverage(c -> {
            for (String family : List.of("KindCase", "MissingCase", "UnknownCase", "ConflictCase", "MismatchCase")) {
                c.check(family).percentage(p -> p >= 5);
            }
        });
        switch (failureCase) {
            case KindCase k -> checkKind(k);
            case MissingCase m -> checkMissing(m);
            case UnknownCase u -> checkUnknown(u);
            case ConflictCase c -> checkConflict(c);
            case MismatchCase m -> checkMismatch(m);
        }
    }

    // ---- Requirement 6.10: incompatible kinds ----

    /** The kinds of generated source documents; the modeled ones are coerced from the untyped string or number. */
    enum Source {
        BOOLEAN, STRING, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL, BLOB, TIMESTAMP, LIST,
        MAP, ENUM, INT_ENUM;

        ShapeType kind() {
            return ShapeType.valueOf(name());
        }

        Document document() {
            return switch (this) {
                case BOOLEAN -> Document.of(true);
                case STRING -> Document.of("text");
                case BYTE -> Document.of((byte) 7);
                case SHORT -> Document.of((short) 7);
                case INTEGER -> Document.of(7);
                case LONG -> Document.of(7L);
                case FLOAT -> Document.of(7f);
                case DOUBLE -> Document.of(7d);
                case BIG_INTEGER -> Document.of(BigInteger.valueOf(7));
                case BIG_DECIMAL -> Document.of(new BigDecimal("7"));
                case BLOB -> Document.of(new byte[] {1, 2, 3});
                case TIMESTAMP -> Document.of(Instant.ofEpochSecond(7));
                case LIST -> Document.of(List.of());
                case MAP -> Document.of(Map.of());
                case ENUM -> SchemaDocuments.coerce(Document.of("text"), Target.ENUM.schema);
                case INT_ENUM -> SchemaDocuments.coerce(Document.of(7), Target.INT_ENUM.schema);
            };
        }

        boolean numeric() {
            return switch (this) {
                case BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL, INT_ENUM -> true;
                default -> false;
            };
        }
    }

    /** The requested target schemas. */
    enum Target {
        BOOLEAN(PreludeSchemas.BOOLEAN), STRING(PreludeSchemas.STRING), BYTE(PreludeSchemas.BYTE),
        SHORT(PreludeSchemas.SHORT), INTEGER(PreludeSchemas.INTEGER), LONG(PreludeSchemas.LONG),
        FLOAT(PreludeSchemas.FLOAT), DOUBLE(PreludeSchemas.DOUBLE), BIG_INTEGER(PreludeSchemas.BIG_INTEGER),
        BIG_DECIMAL(PreludeSchemas.BIG_DECIMAL), BLOB(PreludeSchemas.BLOB), TIMESTAMP(PreludeSchemas.TIMESTAMP),
        DOCUMENT(PreludeSchemas.DOCUMENT),
        ENUM(Schema.createEnum(id("Color"), Set.of("text", "other"))),
        INT_ENUM(Schema.createIntEnum(id("Level"), Set.of(7, 8))),
        LIST(Schema.listBuilder(id("Names")).putMember("member", PreludeSchemas.STRING).build()),
        MAP(Schema.mapBuilder(id("Labels")).putMember("key", PreludeSchemas.STRING)
                  .putMember("value", PreludeSchemas.STRING).build()),
        STRUCTURE(Schema.structureBuilder(id("Record")).putMember("name", PreludeSchemas.STRING).build()),
        UNION(Schema.unionBuilder(id("Choice")).putMember("name", PreludeSchemas.STRING).build());

        final Schema schema;

        Target(Schema schema) {
            this.schema = schema;
        }
    }

    /** How a value is nested in its parent: a structure member, the first list element or a map value. */
    enum Wrapper {
        MEMBER, ELEMENT, VALUE
    }

    record KindCase(Source source, Target target, List<Wrapper> wrappers) implements Case {
    }

    /** Whether the documented rules let a {@code source} document be read as {@code target}. */
    static boolean compatible(Source source, Target target) {
        return switch (target) {
            case DOCUMENT -> true;
            case BOOLEAN -> source == Source.BOOLEAN;
            case STRING, ENUM -> source == Source.STRING || source == Source.ENUM;
            case BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL, INT_ENUM -> source.numeric();
            case BLOB -> source == Source.BLOB;
            case TIMESTAMP -> source == Source.TIMESTAMP;
            case LIST -> source == Source.LIST;
            case MAP -> source == Source.MAP;
            case STRUCTURE, UNION -> source == Source.MAP;
        };
    }

    /** Pairs whose outcome depends on a protocol representation rather than the kind, which are not generated. */
    static boolean representational(Source source, Target target) {
        return target == Target.BLOB && source == Source.STRING
               || target == Target.TIMESTAMP && (source == Source.STRING || source.numeric())
               || target == Target.UNION && source == Source.MAP;
    }

    private static void checkKind(KindCase kindCase) {
        Schema schema = kindCase.target().schema;
        Document document = kindCase.source().document();
        assertEquals(kindCase.source().kind(), document.type(), "generator: source kind");
        String path = "$";
        List<Wrapper> wrappers = kindCase.wrappers();
        // Wrap from the innermost level outwards; the path is built from the root inwards.
        for (int level = wrappers.size() - 1; level >= 0; level--) {
            String name = "w" + level;
            ShapeId wrapperId = id("Wrapper" + level);
            switch (wrappers.get(level)) {
                case MEMBER -> {
                    schema = Schema.structureBuilder(wrapperId).putMember(name, schema).build();
                    document = Document.of(Map.of(name, document));
                }
                case ELEMENT -> {
                    schema = Schema.listBuilder(wrapperId).putMember("member", schema).build();
                    document = Document.of(List.of(document));
                }
                case VALUE -> {
                    schema = Schema.mapBuilder(wrapperId).putMember("key", PreludeSchemas.STRING)
                                   .putMember("value", schema).build();
                    document = Document.of(Map.of(name, document));
                }
            }
        }
        for (int level = 0; level < wrappers.size(); level++) {
            path += switch (wrappers.get(level)) {
                case MEMBER -> ".w" + level;
                case ELEMENT -> "[0]";
                case VALUE -> "[\"w" + level + "\"]";
            };
        }
        Statistics.label("kind outcome").collect(compatible(kindCase.source(), kindCase.target()));
        Statistics.label("kind outcome").coverage(c -> {
            c.check(true).percentage(p -> p >= 5);
            c.check(false).percentage(p -> p >= 5);
        });
        Schema requested = schema;
        Document input = document;
        if (compatible(kindCase.source(), kindCase.target())) {
            SchemaDocuments.coerce(input, requested);
            return;
        }
        Schema target = kindCase.target().schema;
        Coercion expected = new Coercion(Reason.INCOMPATIBLE_KIND, kindCase.source().kind(), target.id(),
                                         target.type(), null, path);
        assertCoercionTwice(expected, () -> SchemaDocuments.coerce(input, requested));
    }

    // ---- Requirements 6.11 and 6.12: absent and unknown discriminators ----

    record MissingCase(Set<ShapeId> registered, Source source, boolean strict) implements Case {
    }

    /** How the unknown discriminator is carried. */
    enum Carrier {
        JSON_TYPE, MODELED, SMITHY_TYPED
    }

    record UnknownCase(Set<ShapeId> registered, ShapeId discriminator, Carrier carrier, boolean strict)
        implements Case {
    }

    private static void checkMissing(MissingCase missing) {
        SchemaTypeRegistry registry = registry(missing.registered());
        Document document = missing.source().document();
        // Modeled documents always carry their shape ID; only untyped sources lack a discriminator.
        assertNull(document.discriminator(), "generator: an undiscriminated document");
        String message = "Cannot construct a registered type from a " + missing.source().kind()
                         + " document: the document has no shape discriminator";
        for (int attempt = 0; attempt < 2; attempt++) {
            MissingDiscriminatorException error = assertThrows(
                MissingDiscriminatorException.class,
                () -> deserialize(registry, document, missing.strict()));
            assertEquals(missing.source().kind(), error.sourceKind());
            assertEquals(message, error.getMessage());
        }
    }

    private static void checkUnknown(UnknownCase unknown) {
        SchemaTypeRegistry registry = registry(unknown.registered());
        ShapeId id = unknown.discriminator();
        assertFalse(registry.contains(id), "generator: an unregistered discriminator");
        Schema schema = Schema.structureBuilder(id).build();
        Document document = switch (unknown.carrier()) {
            case JSON_TYPE -> JSON.createDeserializer(("{\"__type\":\"" + id + "\"}").getBytes(UTF_8)).readDocument();
            case MODELED -> SchemaDocuments.of(new ValueA(schema));
            case SMITHY_TYPED -> Document.of(new ValueA(schema));
        };
        assertEquals(id, document.discriminator(), "generator: the discriminator");
        for (int attempt = 0; attempt < 2; attempt++) {
            UnknownDiscriminatorException error = assertThrows(
                UnknownDiscriminatorException.class,
                () -> deserialize(registry, document, unknown.strict()));
            assertEquals(id, error.shapeId());
            assertEquals("No type is registered for the document discriminator " + id, error.getMessage());
        }
    }

    private static SerializableStruct deserialize(SchemaTypeRegistry registry, Document document, boolean strict) {
        return strict ? registry.deserializeStrict(document) : registry.deserialize(document);
    }

    private static SchemaTypeRegistry registry(Set<ShapeId> ids) {
        List<RegisteredType<?>> entries = new ArrayList<>();
        for (ShapeId id : new TreeSet<>(ids)) {
            Schema schema = Schema.structureBuilder(id).build();
            entries.add(entry(schema, 0, new Factory(schema, CONSTRUCTORS.get(0))));
        }
        return SchemaTypeRegistry.builder()
                                 .partition(entries.stream().map(RegisteredType::shapeId).toList(), () -> entries)
                                 .build();
    }

    // ---- Requirement 6.13: conflicting entries ----

    /**
     * One registration of an ID by a component: whether it varies the schema, class and factory from the ID's base
     * objects (a bit mask: 1 schema, 2 class, 4 factory), and, when it varies nothing, whether it reuses the base entry
     * object itself rather than an equivalent entry.
     */
    record Registration(int component, ShapeId id, int variation, boolean sameObject) {
    }

    record ConflictCase(int componentCount, List<Registration> registrations) implements Case {
    }

    /** The base and varied objects of one ID. */
    record Variants(Schema[] schemas, Factory[] factories, RegisteredType<?> base) {
        RegisteredType<?> entry(int variation, boolean sameObject) {
            if (variation == 0 && sameObject) {
                return base;
            }
            return RegisteredTypes.of(schemas[variation & 1], (variation >> 1) & 1,
                                      factories[factoryIndex(variation)]);
        }

        /** The factory of a variation: the base factory, or a varied factory building the variation's class. */
        static int factoryIndex(int variation) {
            return (variation & 4) == 0 ? 0 : 1 + ((variation >> 1) & 1);
        }
    }

    /** The oracle's view of one expected conflict. */
    record ExpectedConflict(ShapeId shapeId, int firstRegistry, String firstSource, RegisteredType<?> first,
                            int secondRegistry, String secondSource, RegisteredType<?> second,
                            List<String> differences) {
        String describe() {
            return shapeId + " is registered as " + first.type().getName() + " by " + firstSource + " and as "
                   + second.type().getName() + " by " + secondSource + " (differing " + String.join(", ", differences)
                   + ")";
        }
    }

    private static void checkConflict(ConflictCase conflictCase) {
        Map<ShapeId, Variants> variants = new TreeMap<>();
        for (Registration registration : conflictCase.registrations()) {
            variants.computeIfAbsent(registration.id(), id -> {
                Schema base = Schema.structureBuilder(id).build();
                Schema other = Schema.structureBuilder(id).putMember("extra", PreludeSchemas.STRING).build();
                // Factory 0 builds class 0; the varied factories build class 0 (index 1) and class 1 (index 2).
                Factory[] factories = {new Factory(base, CONSTRUCTORS.get(0)), new Factory(base, CONSTRUCTORS.get(0)),
                                       new Factory(base, CONSTRUCTORS.get(1))};
                return new Variants(new Schema[] {base, other}, factories,
                                    RegisteredTypes.of(base, 0, factories[0]));
            });
        }
        // The entries of each component, in registration order.
        List<Map<ShapeId, RegisteredType<?>>> owned = new ArrayList<>();
        List<Map<ShapeId, Integer>> variations = new ArrayList<>();
        for (int i = 0; i < conflictCase.componentCount(); i++) {
            owned.add(new LinkedHashMap<>());
            variations.add(new HashMap<>());
        }
        for (Registration registration : conflictCase.registrations()) {
            Variants v = variants.get(registration.id());
            owned.get(registration.component()).put(registration.id(),
                                                    v.entry(registration.variation(), registration.sameObject()));
            variations.get(registration.component()).put(registration.id(), registration.variation());
        }
        List<SchemaTypeRegistry> registries = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        for (int i = 0; i < conflictCase.componentCount(); i++) {
            List<RegisteredType<?>> entries = List.copyOf(owned.get(i).values());
            registries.add(SchemaTypeRegistry.builder().partition(List.copyOf(owned.get(i).keySet()), () -> entries)
                                             .build());
            SortedSet<String> namespaces = owned.get(i).keySet().stream().map(ShapeId::getNamespace)
                                                .collect(Collectors.toCollection(TreeSet::new));
            sources.add("registry #" + i + " (namespaces " + namespaces + ")");
        }

        // The oracle: each later registration whose objects differ from the first registration's.
        List<ExpectedConflict> expected = new ArrayList<>();
        for (ShapeId id : variants.keySet()) {
            int owner = -1;
            for (int i = 0; i < conflictCase.componentCount(); i++) {
                if (!owned.get(i).containsKey(id)) {
                    continue;
                }
                if (owner < 0) {
                    owner = i;
                    continue;
                }
                int differing = variations.get(owner).get(id) ^ variations.get(i).get(id);
                boolean factoryDiffers = Variants.factoryIndex(variations.get(owner).get(id))
                                         != Variants.factoryIndex(variations.get(i).get(id));
                List<String> differences = new ArrayList<>();
                if ((differing & 1) != 0) {
                    differences.add("schema");
                }
                if ((differing & 2) != 0) {
                    differences.add("type");
                }
                if (factoryDiffers) {
                    differences.add("builder factory");
                }
                if (!differences.isEmpty()) {
                    expected.add(new ExpectedConflict(id, owner, sources.get(owner), owned.get(owner).get(id), i,
                                                      sources.get(i), owned.get(i).get(id), List.copyOf(differences)));
                }
            }
        }
        Statistics.label("conflicts").collect(expected.isEmpty() ? "none" : expected.size() == 1 ? "one" : "several");
        Statistics.label("conflicts").coverage(c -> {
            c.check("none").percentage(p -> p >= 3);
            c.check("one").percentage(p -> p >= 10);
            c.check("several").percentage(p -> p >= 10);
        });
        if (expected.isEmpty()) {
            SchemaTypeRegistry composed = SchemaTypeRegistry.compose(registries);
            assertEquals(variants.keySet(), composed.shapeIds());
            return;
        }
        long ids = expected.stream().map(ExpectedConflict::shapeId).distinct().count();
        StringBuilder message = new StringBuilder("Cannot compose type registries: ")
            .append(ids).append(ids == 1 ? " shape ID is" : " shape IDs are")
            .append(" registered with conflicting entries");
        expected.forEach(conflict -> message.append("; ").append(conflict.describe()));

        for (int attempt = 0; attempt < 2; attempt++) {
            TypeRegistryConflictException error = assertThrows(TypeRegistryConflictException.class,
                                                               () -> SchemaTypeRegistry.compose(registries));
            assertEquals(message.toString(), error.getMessage());
            assertEquals(expected.size(), error.conflicts().size(), error::getMessage);
            for (int i = 0; i < expected.size(); i++) {
                ExpectedConflict want = expected.get(i);
                TypeRegistryConflictException.Conflict got = error.conflicts().get(i);
                assertEquals(want.shapeId(), got.shapeId());
                assertEquals(want.firstRegistry(), got.firstRegistry());
                assertEquals(want.firstSource(), got.firstSource());
                assertSame(want.first(), got.first());
                assertEquals(want.secondRegistry(), got.secondRegistry());
                assertEquals(want.secondSource(), got.secondSource());
                assertSame(want.second(), got.second());
                assertEquals(want.differences(), got.differences());
                assertEquals("Conflict[" + want.describe() + "]", got.toString());
            }
        }
    }

    // ---- Requirement 6.14: discriminator/schema mismatch ----

    record MismatchCase(ShapeId discriminator, ShapeId requested, boolean union, Carrier carrier,
                        List<Wrapper> wrappers, int root) implements Case {
    }

    private static void checkMismatch(MismatchCase mismatch) {
        ShapeId a = mismatch.discriminator();
        Schema aSchema = Schema.structureBuilder(a).build();
        Document document = switch (mismatch.carrier()) {
            case JSON_TYPE -> JSON.createDeserializer(("{\"__type\":\"" + a + "\"}").getBytes(UTF_8)).readDocument();
            case MODELED -> SchemaDocuments.of(new ValueA(aSchema));
            case SMITHY_TYPED -> Document.of(new ValueA(aSchema));
        };
        assertEquals(a, document.discriminator(), "generator: the discriminator");
        ShapeType sourceKind = mismatch.carrier() == Carrier.JSON_TYPE ? ShapeType.MAP : ShapeType.STRUCTURE;
        assertEquals(sourceKind, document.type(), "generator: the source kind");
        Schema b = (mismatch.union() ? Schema.unionBuilder(mismatch.requested())
                                     : Schema.structureBuilder(mismatch.requested()))
            .putMember("name", PreludeSchemas.STRING).build();

        Schema schema = b;
        String path = "$";
        List<Wrapper> wrappers = mismatch.wrappers();
        for (int level = wrappers.size() - 1; level >= 0; level--) {
            String name = "w" + level;
            ShapeId wrapperId = id("Wrapper" + level);
            switch (wrappers.get(level)) {
                case MEMBER -> {
                    schema = Schema.structureBuilder(wrapperId).putMember(name, schema).build();
                    document = Document.of(Map.of(name, document));
                }
                case ELEMENT -> {
                    schema = Schema.listBuilder(wrapperId).putMember("member", schema).build();
                    document = Document.of(List.of(document));
                }
                case VALUE -> {
                    schema = Schema.mapBuilder(wrapperId).putMember("key", PreludeSchemas.STRING)
                                   .putMember("value", schema).build();
                    document = Document.of(Map.of(name, document));
                }
            }
        }
        for (int level = 0; level < wrappers.size(); level++) {
            path += switch (wrappers.get(level)) {
                case MEMBER -> ".w" + level;
                case ELEMENT -> "[0]";
                case VALUE -> "[\"w" + level + "\"]";
            };
        }
        Coercion expected = new Coercion(Reason.DISCRIMINATOR_MISMATCH, sourceKind, b.id(), b.type(), a, path);
        Schema requested = schema;
        Document input = document;
        if (!wrappers.isEmpty() || mismatch.root() == 0) {
            assertCoercionTwice(expected, () -> SchemaDocuments.coerce(input, requested));
        } else if (mismatch.root() == 1) {
            assertCoercionTwice(expected, () -> SchemaDocuments.toShape(input, new Factory(b, ValueA::new).get()));
        } else {
            assertCoercionTwice(expected,
                                () -> SchemaDocuments.toShapeStrict(input, new Factory(b, ValueA::new).get()));
        }
    }

    // ---- Coercion oracle ----

    /** The expected structured diagnostic of a coercion failure. */
    record Coercion(Reason reason, ShapeType sourceKind, ShapeId requestedShape, ShapeType requestedType,
                    ShapeId discriminator, String path) {
        String message() {
            String message = "Cannot coerce " + sourceKind + " document at " + path + " to " + requestedShape + " ("
                             + requestedType + "): " + reason.description();
            if (discriminator != null) {
                message += " (document discriminator " + discriminator + ", requested shape " + requestedShape + ")";
            }
            return message;
        }
    }

    private static void assertCoercionTwice(Coercion expected, Executable executable) {
        for (int attempt = 0; attempt < 2; attempt++) {
            DocumentCoercionException error = assertThrows(DocumentCoercionException.class, executable);
            assertEquals(expected.message(), error.getMessage());
            assertEquals(expected.reason(), error.reason());
            assertEquals(expected.sourceKind(), error.sourceKind());
            assertEquals(expected.requestedShape(), error.requestedShape());
            assertEquals(expected.discriminator(), error.discriminator());
            assertEquals(expected.path(), error.path());
        }
    }

    // ---- Generators ----

    /** One try of one failure family. */
    sealed interface Case permits KindCase, MissingCase, UnknownCase, ConflictCase, MismatchCase {
    }

    @Provide
    Arbitrary<Case> cases() {
        // Kind cases have by far the largest input space (source, target and nesting), so they get three shares.
        return Arbitraries.frequencyOf(Tuple.of(3, kindCases()), Tuple.of(1, missingCases()),
                                       Tuple.of(1, unknownCases()), Tuple.of(1, conflictCases()),
                                       Tuple.of(1, mismatchCases()));
    }

    private static Arbitrary<List<Wrapper>> wrappers() {
        return Arbitraries.of(Wrapper.class).list().ofMaxSize(2);
    }

    private static Arbitrary<Case> kindCases() {
        return Combinators.combine(Arbitraries.of(Source.class), Arbitraries.of(Target.class), wrappers())
                          .filter((source, target, wrappers) -> !representational(source, target))
                          .as(KindCase::new);
    }

    private static Arbitrary<Set<ShapeId>> registeredIds() {
        return Arbitraries.of(POOL).set().ofMaxSize(POOL.size() - 1);
    }

    private static Arbitrary<Case> missingCases() {
        Arbitrary<Source> untyped = Arbitraries.of(Source.class)
                                               .filter(s -> s != Source.ENUM && s != Source.INT_ENUM);
        return Combinators.combine(registeredIds(), untyped, Arbitraries.of(true, false)).as(MissingCase::new);
    }

    private static Arbitrary<Case> unknownCases() {
        return registeredIds().flatMap(registered -> {
            List<ShapeId> unregistered = POOL.stream().filter(id -> !registered.contains(id)).toList();
            return Combinators.combine(Arbitraries.just(registered), Arbitraries.of(unregistered),
                                       Arbitraries.of(Carrier.class), Arbitraries.of(true, false))
                              .as(UnknownCase::new);
        });
    }

    private static Arbitrary<Case> conflictCases() {
        return Arbitraries.integers().between(2, 4).flatMap(count -> {
            Arbitrary<Registration> registration = Combinators.combine(
                Arbitraries.integers().between(0, count - 1), Arbitraries.of(POOL),
                Arbitraries.frequency(Tuple.of(2, 0), Tuple.of(1, 1),
                                      Tuple.of(1, 2), Tuple.of(1, 4),
                                      Tuple.of(1, 3), Tuple.of(1, 7)),
                Arbitraries.of(true, false)).as(Registration::new);
            return registration.list().ofMinSize(2).ofMaxSize(12)
                               .uniqueElements(r -> List.of(r.component(), r.id()))
                               .map(list -> (Case) new ConflictCase(count, list));
        });
    }

    private static Arbitrary<Case> mismatchCases() {
        return Arbitraries.of(POOL).list().ofSize(2).uniqueElements().flatMap(pair -> Combinators.combine(
            Arbitraries.just(pair.get(0)), Arbitraries.just(pair.get(1)), Arbitraries.of(true, false),
            Arbitraries.of(Carrier.class), wrappers(), Arbitraries.integers().between(0, 2))
            .as(MismatchCase::new));
    }

    // ---- Fixtures ----

    private static ShapeId id(String name) {
        return ShapeId.fromParts("com.example.types", name);
    }

    private static RegisteredType<?> entry(Schema schema, int classIndex, Factory factory) {
        return RegisteredTypes.of(schema, classIndex, factory);
    }

    /** Creates entries of the fixture classes without generic noise. */
    static final class RegisteredTypes {
        private RegisteredTypes() {
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        static RegisteredType<?> of(Schema schema, int classIndex, Factory factory) {
            return RegisteredType.of(schema, (Class) CLASSES.get(classIndex), (Supplier) factory);
        }
    }

    /** A builder factory of fixture values. */
    static final class Factory implements Supplier<ValueBuilder> {
        private final Schema schema;
        private final Function<Schema, Value> constructor;

        Factory(Schema schema, Function<Schema, Value> constructor) {
            this.schema = schema;
            this.constructor = constructor;
        }

        @Override
        public ValueBuilder get() {
            return new ValueBuilder(schema, constructor);
        }
    }

    /** A builder that reads (and ignores) the members of its schema. */
    static final class ValueBuilder implements ShapeBuilder<Value> {
        private final Schema schema;
        private final Function<Schema, Value> constructor;

        ValueBuilder(Schema schema, Function<Schema, Value> constructor) {
            this.schema = schema;
            this.constructor = constructor;
        }

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public ShapeBuilder<Value> deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(schema, this, (state, member, value) -> value.readDocument());
            return this;
        }

        @Override
        public Value build() {
            return constructor.apply(schema);
        }
    }

    /** A memberless value of any structure schema. */
    abstract static class Value implements SerializableStruct {
        private final Schema schema;

        Value(Schema schema) {
            this.schema = schema;
        }

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return null;
        }
    }

    static final class ValueA extends Value {
        ValueA(Schema schema) {
            super(schema);
        }
    }

    static final class ValueB extends Value {
        ValueB(Schema schema) {
            super(schema);
        }
    }
}
