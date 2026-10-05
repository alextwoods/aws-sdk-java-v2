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

package software.amazon.awssdk.smithy.runtime.document;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import software.amazon.smithy.java.cbor.Rpcv2CborCodec;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.Codec;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.document.DocumentEqualsFlags;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Property 15: Complete Document round trip.
 *
 * <p>For any finite Smithy data-model value—including blob, timestamp, nested collection, structure, union, enum, and
 * document—conversion to a {@code Document} and back through its matching schema preserves kind, value,
 * discriminator, protocol context, and significant order.
 *
 * <p>Each try generates a random structure or union schema (every finite Smithy kind, prelude and custom shape IDs,
 * sparse and dense collections, timestamp formats and JSON names, nested to depth {@value #MAX_DEPTH} with at most
 * {@value #MAX_SIZE} members, elements or entries per aggregate) and a random value of it, held by
 * {@link GenericStruct} in place of a generated type.
 *
 * <p><b>Validates: Requirements 6.1, 6.2, 6.3, 6.4</b>
 */
class DocumentRoundTripPropertyTest {

    private static final int TRIES = 1000;
    private static final int MAX_DEPTH = 3;
    private static final int MAX_SIZE = 5;
    private static final String NS = "software.amazon.awssdk.fixture.generated";

    /** Protocols whose decoded documents carry their own representation context (field names, blobs, timestamps). */
    private static final List<Codec> CODECS = List.of(
        JsonCodec.builder().useTimestampFormat(true).useJsonName(true).build(),
        JsonCodec.builder().build(),
        Rpcv2CborCodec.builder().build());
    private static final List<String> CODEC_NAMES = List.of("json(timestampFormat, jsonName)", "json", "rpcv2Cbor");
    /**
     * Smithy Java's JSON documents parse every fractional or exponent JSON number as a {@code double} (and do not
     * apply {@code useStringForArbitraryPrecision}), so a JSON document can only carry big decimals of double
     * precision; the JSON cases generate those.
     */
    private static final int CBOR = 2;

    /** A generated schema and a value of it. */
    record Case(Schema schema, GenericStruct value) {
    }

    /** A case and the index of the codec in {@link #CODECS} to exchange it with. */
    record ProtocolCase(int codec, Case value) {
        @Override
        public String toString() {
            return CODEC_NAMES.get(codec) + ": " + value;
        }
    }

    @Provide
    Arbitrary<Case> cases() {
        return Arbitraries.randomValue(random -> new Generator(random, false, true, true, true).rootCase());
    }

    @Provide
    Arbitrary<ProtocolCase> protocolCases() {
        return Arbitraries.randomValue(random -> {
            int codec = random.nextInt(CODECS.size());
            return new ProtocolCase(codec, new Generator(random, codec != CBOR, codec == CBOR, codec != CBOR,
                                                         codec == CBOR).rootCase());
        });
    }

    /**
     * A typed value converts to a modeled document of exactly its kinds, shape IDs, values and order, and the
     * document converts back through the schema to an equal value (Requirements 6.1, 6.2 and 6.4).
     */
    @Property(tries = TRIES)
    void typedValueRoundTripsThroughItsDocument(@ForAll("cases") Case c) {
        ModeledDocument document = SchemaDocuments.of(c.value());
        assertDocument(c.schema(), c.value(), document, "$", true);

        GenericStruct copy = SchemaDocuments.toShapeStrict(document, new GenericStruct.Builder(c.schema()));
        assertValue(c.schema(), c.value(), copy, "$", true);

        ModeledDocument again = SchemaDocuments.of(copy);
        assertEquals(document, again);
        assertEquals(document.hashCode(), again.hashCode());
        assertEquals(document, SchemaDocuments.coerce(document, c.schema()));
        assertTrue(SchemaDocuments.equivalent(c.schema(), document, again));
    }

    /**
     * A document decoded by a protocol keeps the protocol's representation context, so coercing it through the
     * schema restores the same modeled document and value (Requirement 6.3). Map entry order is not significant
     * across protocols; list order and structure member order are.
     */
    @Property(tries = TRIES)
    void protocolDecodedDocumentRoundTripsThroughTheSchema(@ForAll("protocolCases") ProtocolCase protocolCase) {
        Codec codec = CODECS.get(protocolCase.codec());
        Case c = protocolCase.value();
        ModeledDocument document = SchemaDocuments.of(c.value());
        Document decoded = codec.createDeserializer(codec.serialize(document)).readDocument();

        Document restored = SchemaDocuments.coerce(decoded, c.schema());
        assertDocument(c.schema(), c.value(), restored, "$", false);
        assertEquals(document, restored);

        GenericStruct copy = SchemaDocuments.toShapeStrict(decoded, new GenericStruct.Builder(c.schema()));
        assertValue(c.schema(), c.value(), copy, "$", false);
    }

    /** Smithy Java's own typed document of a value coerces to the same modeled document as the value itself. */
    @Property(tries = TRIES)
    void smithyTypedDocumentCoercesToTheSameModeledDocument(@ForAll("cases") Case c) {
        assertEquals(SchemaDocuments.of(c.value()), SchemaDocuments.coerce(Document.of(c.value()), c.schema()));
    }

    /**
     * Asserts that {@code actual} is the document of {@code expected}, a value of {@code schema}: the exact modeled
     * kind, schema and discriminator at every node, the exact value, and the significant order. {@code exact}
     * additionally requires map entry order and big-decimal scale to be kept.
     */
    private static void assertDocument(Schema schema, Object expected, Document actual, String path, boolean exact) {
        if (expected == null) {
            assertNull(actual, path);
            return;
        }
        Schema target = schema.isMember() ? schema.memberTarget() : schema;
        if (target.type() == ShapeType.DOCUMENT) {
            assertTrue(Document.equals(expected, actual, DocumentEqualsFlags.NUMBER_PROMOTION),
                       path + ": expected " + expected + " but was " + actual);
            return;
        }
        ModeledDocument modeled = assertInstanceOf(ModeledDocument.class, actual, path);
        assertEquals(target.type(), modeled.type(), path);
        assertEquals(target.id(), modeled.discriminator(), path);
        assertSame(target, modeled.schema(), path);
        switch (target.type()) {
            case BOOLEAN -> assertEquals(expected, modeled.asBoolean(), path);
            case STRING, ENUM -> assertEquals(expected, modeled.asString(), path);
            case BLOB -> assertArrayEquals((byte[]) expected, bytes(modeled.asBlob()), path);
            case TIMESTAMP -> assertEquals(expected, modeled.asTimestamp(), path);
            case BIG_DECIMAL -> assertDecimal((BigDecimal) expected, modeled.asNumber(), path, exact);
            // Boxed equality: the exact kind, NaN equal to NaN and -0.0 distinct from 0.0.
            case BYTE, SHORT, INTEGER, INT_ENUM, LONG, FLOAT, DOUBLE, BIG_INTEGER ->
                assertEquals(expected, modeled.asNumber(), path);
            case LIST, SET -> {
                List<?> elements = (List<?>) expected;
                List<Document> documents = modeled.asList();
                assertEquals(elements.size(), documents.size(), path);
                for (int i = 0; i < elements.size(); i++) {
                    assertDocument(target.listMember(), elements.get(i), documents.get(i), path + "[" + i + "]", exact);
                }
            }
            case MAP -> {
                Map<?, ?> entries = (Map<?, ?>) expected;
                assertKeys(entries.keySet(), modeled.getMemberNames(), path, exact);
                for (Map.Entry<?, ?> entry : entries.entrySet()) {
                    assertDocument(target.mapValueMember(), entry.getValue(), modeled.getMember((String) entry.getKey()),
                                   path + "[\"" + entry.getKey() + "\"]", exact);
                }
            }
            case STRUCTURE, UNION -> {
                Map<String, Object> members = ((GenericStruct) expected).values();
                // Present members, in schema member order.
                assertEquals(List.copyOf(members.keySet()), List.copyOf(modeled.getMemberNames()), path);
                for (Map.Entry<String, Object> member : members.entrySet()) {
                    assertDocument(target.member(member.getKey()), member.getValue(),
                                   modeled.getMember(member.getKey()), path + "." + member.getKey(), exact);
                }
            }
            default -> throw new IllegalStateException("Unexpected kind " + target.type());
        }
    }

    /** Asserts that {@code actual}, read back through {@code schema}, is the value {@code expected}. */
    private static void assertValue(Schema schema, Object expected, Object actual, String path, boolean exact) {
        if (expected == null) {
            assertNull(actual, path);
            return;
        }
        Schema target = schema.isMember() ? schema.memberTarget() : schema;
        switch (target.type()) {
            case DOCUMENT -> assertTrue(Document.equals(expected, actual, DocumentEqualsFlags.NUMBER_PROMOTION),
                                        path + ": expected " + expected + " but was " + actual);
            case BLOB -> assertArrayEquals((byte[]) expected, (byte[]) actual, path);
            case BIG_DECIMAL -> assertDecimal((BigDecimal) expected, (Number) actual, path, exact);
            case LIST, SET -> {
                List<?> elements = (List<?>) expected;
                List<?> copies = assertInstanceOf(List.class, actual, path);
                assertEquals(elements.size(), copies.size(), path);
                for (int i = 0; i < elements.size(); i++) {
                    assertValue(target.listMember(), elements.get(i), copies.get(i), path + "[" + i + "]", exact);
                }
            }
            case MAP -> {
                Map<?, ?> entries = (Map<?, ?>) expected;
                Map<?, ?> copies = assertInstanceOf(Map.class, actual, path);
                assertKeys(entries.keySet(), copies.keySet(), path, exact);
                for (Map.Entry<?, ?> entry : entries.entrySet()) {
                    assertValue(target.mapValueMember(), entry.getValue(), copies.get(entry.getKey()),
                                path + "[\"" + entry.getKey() + "\"]", exact);
                }
            }
            case STRUCTURE, UNION -> {
                Map<String, Object> members = ((GenericStruct) expected).values();
                GenericStruct copy = assertInstanceOf(GenericStruct.class, actual, path);
                assertSame(target, copy.schema(), path);
                assertEquals(List.copyOf(members.keySet()), List.copyOf(copy.values().keySet()), path);
                for (Map.Entry<String, Object> member : members.entrySet()) {
                    assertValue(target.member(member.getKey()), member.getValue(), copy.values().get(member.getKey()),
                                path + "." + member.getKey(), exact);
                }
            }
            // Booleans, boxed numbers of the exact kind, strings, enums and timestamps.
            default -> assertEquals(expected, actual, path);
        }
    }

    private static void assertDecimal(BigDecimal expected, Number actual, String path, boolean exact) {
        BigDecimal decimal = assertInstanceOf(BigDecimal.class, actual, path);
        if (exact) {
            assertEquals(expected, decimal, path);
        } else {
            assertEquals(0, expected.compareTo(decimal), path + ": expected " + expected + " but was " + decimal);
        }
    }

    private static void assertKeys(Set<?> expected, Set<?> actual, String path, boolean ordered) {
        if (ordered) {
            assertEquals(List.copyOf(expected), List.copyOf(actual), path);
        } else {
            assertEquals(Set.copyOf(expected), Set.copyOf(actual), path);
        }
    }

    private static byte[] bytes(ByteBuffer buffer) {
        ByteBuffer view = buffer.duplicate();
        byte[] bytes = new byte[view.remaining()];
        view.get(bytes);
        return bytes;
    }

    /** Generates one random schema tree and a value of it from a jqwik-provided {@link Random}. */
    private static final class Generator {
        private static final int[] INTS = {0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
        private static final long[] LONGS = {0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE};
        private static final float[] FLOATS = {Float.NaN, 0.0f, -0.0f, Float.POSITIVE_INFINITY,
                                               Float.NEGATIVE_INFINITY, Float.MIN_VALUE, Float.MAX_VALUE, 0.1f};
        private static final double[] DOUBLES = {Double.NaN, 0.0d, -0.0d, Double.POSITIVE_INFINITY,
                                                 Double.NEGATIVE_INFINITY, Double.MIN_VALUE, Double.MAX_VALUE, 0.1d};
        private static final String[] TIMESTAMP_FORMATS = {TimestampFormatTrait.EPOCH_SECONDS,
                                                           TimestampFormatTrait.DATE_TIME,
                                                           TimestampFormatTrait.HTTP_DATE};
        /** 1900-01-01T00:00:00Z and 2100-01-01T00:00:00Z. */
        private static final long MIN_SECONDS = -2_208_988_800L;
        private static final long MAX_SECONDS = 4_102_444_800L;
        private static final int SCALAR_KINDS = 15;
        private static final int ALL_KINDS = 19;

        private final Random random;
        private final boolean doublePrecisionDecimals;
        /**
         * KNOWN V3 BUG (reported with task 4.9): Smithy Java 1.6.1's JSON codec loses the sign of negative zero.
         * {@code SmithyJsonSerializer.writeFloat}/{@code writeDouble} delegate to
         * {@code software.amazon.smithy.java.codecs.commons.NumberCodec.writeFloatFullQuoted}/
         * {@code writeDoubleFullQuoted} (codec-commons 1.6.1), which write {@code -0.0f} and {@code -0.0d} as
         * {@code 0}; that decodes as the integer document {@code 0} and coerces to {@code 0.0}. CBOR keeps the sign.
         * Until this is fixed upstream, JSON cases generate no negative-zero floats, doubles or document numbers.
         */
        private final boolean signedZeros;
        /**
         * KNOWN V3 BUG (found by task 5.1): Smithy Java 1.6.1's CBOR codec saturates big integers outside the
         * {@code long} range whose magnitude fits in 64 bits ({@code 2^63} to {@code 2^64 - 1} and {@code -2^63 - 1}
         * to {@code -2^64}) when it reads them as an untyped document. The serializer writes them as CBOR major type 0
         * or 1 with an 8-byte argument (valid CBOR, and schema-directed {@code readBigInteger} decodes them exactly),
         * but {@code readDocument()} returns a {@code long} document clamped to {@code Long.MAX_VALUE} or
         * {@code Long.MIN_VALUE}; for example {@code -10053264111979820310} decodes as {@code -9223372036854775808}.
         * Wider values use bignum tags and are unaffected. Until this is fixed upstream, CBOR cases generate no big
         * integers in that range.
         */
        private final boolean unsignedLongBigIntegers;
        /**
         * KNOWN V3 BUG (found by task 5.1): Smithy Java 1.6.1 truncates epoch-seconds timestamps read from a JSON
         * number. {@code software.amazon.smithy.java.core.serde.TimestampFormatter.Prelude.EPOCH_SECONDS} writes
         * {@code toEpochMilli() / 1000.0} (the shortest double, e.g. {@code -2.186396766299E9} for
         * {@code 1900-09-19T11:33:53.701Z}), but {@code readFromNumber(Double)} returns
         * {@code Instant.ofEpochMilli((long) (value * 1000f))}; when the double is just below the decimal value
         * ({@code -2186396766.298999786...}) the product is {@code -2186396766298.9998}, which the cast truncates
         * toward zero to {@code .702}. About 1% of millisecond values of either sign are affected; CBOR keeps the
         * timestamp and is unaffected. Until this is fixed upstream, JSON cases generate no timestamps that this read
         * changes.
         */
        private final boolean truncatedEpochSeconds;
        private int shapes;

        Generator(Random random, boolean doublePrecisionDecimals, boolean signedZeros,
                  boolean unsignedLongBigIntegers, boolean truncatedEpochSeconds) {
            this.random = random;
            this.doublePrecisionDecimals = doublePrecisionDecimals;
            this.signedZeros = signedZeros;
            this.unsignedLongBigIntegers = unsignedLongBigIntegers;
            this.truncatedEpochSeconds = truncatedEpochSeconds;
        }

        Case rootCase() {
            Schema schema = random.nextBoolean() ? structure(0) : union(0);
            return new Case(schema, aggregate(schema));
        }

        private ShapeId nextId(String kind) {
            return ShapeId.from(NS + "#" + kind + shapes++);
        }

        // ---- Schemas ----

        /** A target shape at {@code depth}; aggregates and collections only below the maximum depth. */
        private Schema target(int depth) {
            boolean custom = random.nextBoolean();
            int kind = random.nextInt(depth < MAX_DEPTH ? ALL_KINDS : SCALAR_KINDS);
            return switch (kind) {
                case 0 -> custom ? Schema.createBoolean(nextId("Boolean")) : PreludeSchemas.BOOLEAN;
                case 1 -> custom ? Schema.createByte(nextId("Byte")) : PreludeSchemas.BYTE;
                case 2 -> custom ? Schema.createShort(nextId("Short")) : PreludeSchemas.SHORT;
                case 3 -> custom ? Schema.createInteger(nextId("Integer")) : PreludeSchemas.INTEGER;
                case 4 -> custom ? Schema.createLong(nextId("Long")) : PreludeSchemas.LONG;
                case 5 -> custom ? Schema.createFloat(nextId("Float")) : PreludeSchemas.FLOAT;
                case 6 -> custom ? Schema.createDouble(nextId("Double")) : PreludeSchemas.DOUBLE;
                case 7 -> custom ? Schema.createBigInteger(nextId("BigInteger")) : PreludeSchemas.BIG_INTEGER;
                case 8 -> custom ? Schema.createBigDecimal(nextId("BigDecimal")) : PreludeSchemas.BIG_DECIMAL;
                case 9 -> custom ? Schema.createString(nextId("String")) : PreludeSchemas.STRING;
                case 10 -> custom ? Schema.createBlob(nextId("Blob")) : PreludeSchemas.BLOB;
                case 11 -> custom ? Schema.createTimestamp(nextId("Timestamp")) : PreludeSchemas.TIMESTAMP;
                case 12 -> Schema.createEnum(nextId("Enum"), enumValues());
                case 13 -> Schema.createIntEnum(nextId("IntEnum"), intEnumValues());
                case 14 -> custom ? Schema.createDocument(nextId("Document")) : PreludeSchemas.DOCUMENT;
                case 15 -> list(depth);
                case 16 -> map(depth);
                case 17 -> structure(depth);
                default -> union(depth);
            };
        }

        private Set<String> enumValues() {
            Set<String> values = new LinkedHashSet<>();
            int count = 1 + random.nextInt(3);
            while (values.size() < count) {
                values.add(string());
            }
            return values;
        }

        private Set<Integer> intEnumValues() {
            Set<Integer> values = new LinkedHashSet<>();
            int count = 1 + random.nextInt(3);
            while (values.size() < count) {
                values.add(anInt());
            }
            return values;
        }

        private Schema list(int depth) {
            Schema element = target(depth + 1);
            return Schema.listBuilder(nextId("List"), collectionTraits())
                         .putMember("member", element, memberTraits(element))
                         .build();
        }

        private Schema map(int depth) {
            Schema value = target(depth + 1);
            return Schema.mapBuilder(nextId("Map"), collectionTraits())
                         .putMember("key", PreludeSchemas.STRING)
                         .putMember("value", value, memberTraits(value))
                         .build();
        }

        private Schema structure(int depth) {
            return aggregateSchema(Schema.structureBuilder(nextId("Structure")), random.nextInt(MAX_SIZE + 1), depth);
        }

        private Schema union(int depth) {
            return aggregateSchema(Schema.unionBuilder(nextId("Union")), 1 + random.nextInt(MAX_SIZE), depth);
        }

        private Schema aggregateSchema(SchemaBuilder builder, int size, int depth) {
            for (int i = 0; i < size; i++) {
                String name = "m" + i;
                Schema target = target(depth + 1);
                List<Trait> traits = new ArrayList<>(List.of(memberTraits(target)));
                if (random.nextInt(5) == 0) {
                    traits.add(new JsonNameTrait("json-" + name));
                }
                builder.putMember(name, target, traits.toArray(new Trait[0]));
            }
            return builder.build();
        }

        private Trait[] collectionTraits() {
            return random.nextBoolean() ? new Trait[] {new SparseTrait()} : new Trait[0];
        }

        /** A timestamp format for most timestamp members. */
        private Trait[] memberTraits(Schema target) {
            if (target.type() == ShapeType.TIMESTAMP && random.nextInt(4) != 0) {
                return new Trait[] {new TimestampFormatTrait(TIMESTAMP_FORMATS[random.nextInt(3)])};
            }
            return new Trait[0];
        }

        // ---- Values ----

        private GenericStruct aggregate(Schema target) {
            Map<String, Object> values = new LinkedHashMap<>();
            List<Schema> members = target.members();
            if (target.type() == ShapeType.UNION) {
                Schema member = members.get(random.nextInt(members.size()));
                values.put(member.memberName(), value(member));
            } else {
                for (Schema member : members) {
                    if (random.nextInt(3) != 0) {
                        values.put(member.memberName(), value(member));
                    }
                }
            }
            return new GenericStruct(target, values);
        }

        /** A non-null value of the member schema {@code member}. */
        private Object value(Schema member) {
            Schema target = member.memberTarget();
            return switch (target.type()) {
                case BOOLEAN -> random.nextBoolean();
                case BYTE -> (byte) random.nextInt();
                case SHORT -> (short) random.nextInt();
                case INTEGER -> anInt();
                case INT_ENUM -> pick(target.intEnumValues());
                case LONG -> aLong();
                case FLOAT -> aFloat();
                case DOUBLE -> aDouble();
                case BIG_INTEGER -> bigIntegerMember();
                case BIG_DECIMAL -> doublePrecisionDecimals ? BigDecimal.valueOf(finiteDouble())
                                                            : new BigDecimal(bigInteger(), random.nextInt(31) - 10);
                case STRING -> string();
                case ENUM -> pick(target.stringEnumValues());
                case BLOB -> {
                    byte[] bytes = new byte[random.nextInt(9)];
                    random.nextBytes(bytes);
                    yield bytes;
                }
                case TIMESTAMP -> timestamp(member);
                case DOCUMENT -> document(0);
                case LIST -> {
                    boolean sparse = target.hasTrait(TraitKey.SPARSE_TRAIT);
                    List<Object> elements = new ArrayList<>();
                    int size = random.nextInt(MAX_SIZE + 1);
                    for (int i = 0; i < size; i++) {
                        elements.add(sparse && random.nextInt(4) == 0 ? null : value(target.listMember()));
                    }
                    yield elements;
                }
                case MAP -> {
                    boolean sparse = target.hasTrait(TraitKey.SPARSE_TRAIT);
                    Map<String, Object> entries = new LinkedHashMap<>();
                    int size = random.nextInt(MAX_SIZE + 1);
                    while (entries.size() < size) {
                        entries.put(string(), sparse && random.nextInt(4) == 0 ? null : value(target.mapValueMember()));
                    }
                    yield entries;
                }
                case STRUCTURE, UNION -> aggregate(target);
                default -> throw new IllegalStateException("Unexpected kind " + target.type());
            };
        }

        private <T extends Comparable<T>> T pick(Set<T> values) {
            List<T> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            return sorted.get(random.nextInt(sorted.size()));
        }

        private int anInt() {
            return random.nextInt(3) == 0 ? INTS[random.nextInt(INTS.length)] : random.nextInt();
        }

        private long aLong() {
            return random.nextInt(3) == 0 ? LONGS[random.nextInt(LONGS.length)] : random.nextLong();
        }

        private float aFloat() {
            float value = random.nextInt(3) == 0 ? FLOATS[random.nextInt(FLOATS.length)]
                                                 : Float.intBitsToFloat(random.nextInt());
            // Adding 0.0f turns -0.0f into 0.0f and keeps every other value (see signedZeros).
            return signedZeros ? value : value + 0.0f;
        }

        private double aDouble() {
            double value = random.nextInt(3) == 0 ? DOUBLES[random.nextInt(DOUBLES.length)]
                                                  : Double.longBitsToDouble(random.nextLong());
            return signedZeros ? value : value + 0.0d;
        }

        private double finiteDouble() {
            double value = aDouble();
            return Double.isFinite(value) ? value : 0.5d;
        }

        private BigInteger bigInteger() {
            BigInteger magnitude = new BigInteger(1 + random.nextInt(100), random);
            return random.nextBoolean() ? magnitude : magnitude.negate();
        }

        /** A big integer member value (see {@link #unsignedLongBigIntegers}). */
        private BigInteger bigIntegerMember() {
            BigInteger value = bigInteger();
            while (!unsignedLongBigIntegers && value.bitLength() == 64) {
                value = bigInteger();
            }
            return value;
        }

        /**
         * Millisecond precision, or whole seconds for http-date, which has no fractional seconds (see
         * {@link #truncatedEpochSeconds}).
         */
        private Instant timestamp(Schema member) {
            long seconds = MIN_SECONDS + (long) (random.nextDouble() * (MAX_SECONDS - MIN_SECONDS));
            TimestampFormatTrait format = member.getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT);
            boolean httpDate = format != null && TimestampFormatTrait.HTTP_DATE.equals(format.getValue());
            Instant value;
            do {
                value = Instant.ofEpochSecond(seconds, httpDate ? 0 : random.nextInt(1000) * 1_000_000L);
            } while (!truncatedEpochSeconds && (long) (value.toEpochMilli() / 1000.0 * 1000f) != value.toEpochMilli());
            return value;
        }

        /** Printable ASCII (including quotes and backslashes), controls, other BMP and supplementary code points. */
        private String string() {
            StringBuilder builder = new StringBuilder();
            int length = random.nextInt(7);
            for (int i = 0; i < length; i++) {
                int codePoint = switch (random.nextInt(4)) {
                    case 0 -> 0x20 + random.nextInt(0x5F);
                    case 1 -> random.nextInt(0x20);
                    case 2 -> 0x80 + random.nextInt(0xD800 - 0x80);
                    default -> 0x10000 + random.nextInt(0x110000 - 0x10000);
                };
                builder.appendCodePoint(codePoint);
            }
            return builder.toString();
        }

        /**
         * An untyped document of protocol-neutral kinds (booleans, strings, integral and finite floating numbers,
         * lists and maps), which every protocol represents without a schema.
         */
        private Document document(int depth) {
            return switch (random.nextInt(depth < 2 ? 7 : 5)) {
                case 0 -> Document.of(random.nextBoolean());
                case 1 -> Document.of(string());
                case 2 -> Document.of(anInt());
                case 3 -> Document.of(aLong());
                case 4 -> Document.of(finiteDouble());
                case 5 -> {
                    List<Document> elements = new ArrayList<>();
                    int size = random.nextInt(MAX_SIZE + 1);
                    for (int i = 0; i < size; i++) {
                        elements.add(document(depth + 1));
                    }
                    yield Document.of(elements);
                }
                default -> {
                    Map<String, Document> entries = new LinkedHashMap<>();
                    int size = random.nextInt(MAX_SIZE + 1);
                    while (entries.size() < size) {
                        entries.put(string(), document(depth + 1));
                    }
                    yield Document.of(entries);
                }
            };
        }
    }
}
