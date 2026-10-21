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

package software.amazon.awssdk.schema.serde.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.RecordingShapeSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;

class DocumentTest {

    static Stream<Arguments> typedDocuments() {
        return Stream.of(
            Arguments.of(Document.of(true), ShapeType.BOOLEAN),
            Arguments.of(Document.of((byte) 1), ShapeType.BYTE),
            Arguments.of(Document.of((short) 1), ShapeType.SHORT),
            Arguments.of(Document.of(1), ShapeType.INTEGER),
            Arguments.of(Document.of(1L), ShapeType.LONG),
            Arguments.of(Document.of(1.5f), ShapeType.FLOAT),
            Arguments.of(Document.of(1.5d), ShapeType.DOUBLE),
            Arguments.of(Document.of(BigInteger.ONE), ShapeType.BIG_INTEGER),
            Arguments.of(Document.of(BigDecimal.ONE), ShapeType.BIG_DECIMAL),
            Arguments.of(Document.of("a"), ShapeType.STRING),
            Arguments.of(Document.of(new byte[] {1, 2}), ShapeType.BLOB),
            Arguments.of(Document.of(Instant.EPOCH), ShapeType.TIMESTAMP),
            Arguments.of(Document.of(Collections.<Document>emptyList()), ShapeType.LIST),
            Arguments.of(Document.EMPTY_MAP, ShapeType.MAP));
    }

    @ParameterizedTest
    @MethodSource("typedDocuments")
    void type_matchesFactory(Document document, ShapeType type) {
        assertThat(document.type()).isEqualTo(type);
        assertThat(document.isType(type)).isTrue();
    }

    @Test
    void numericCoercion_followsJavaPrimitiveConversions() {
        Document longDocument = Document.of(4294967297L);
        assertThat(longDocument.asInteger()).isEqualTo(1);
        assertThat(longDocument.asShort()).isEqualTo((short) 1);
        assertThat(longDocument.asByte()).isEqualTo((byte) 1);
        assertThat(longDocument.asDouble()).isEqualTo(4294967297d);

        Document doubleDocument = Document.of(1.9d);
        assertThat(doubleDocument.asInteger()).isEqualTo(1);
        assertThat(doubleDocument.asLong()).isEqualTo(1L);
        assertThat(doubleDocument.asBigInteger()).isEqualTo(BigInteger.ONE);
        assertThat(doubleDocument.asBigDecimal()).isEqualTo(new BigDecimal("1.9"));

        Document byteDocument = Document.of((byte) -1);
        assertThat(byteDocument.asInteger()).isEqualTo(-1);
        assertThat(byteDocument.asFloat()).isEqualTo(-1f);
    }

    @Test
    void numericCoercion_toBigTypesIsExactForIntegralAndBigValues() {
        assertThat(Document.of(Long.MAX_VALUE).asBigDecimal()).isEqualTo(BigDecimal.valueOf(Long.MAX_VALUE));
        BigInteger big = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN).add(BigInteger.ONE);
        assertThat(Document.of(big).asBigDecimal()).isEqualTo(new BigDecimal(big));
        BigDecimal decimal = new BigDecimal(big).add(new BigDecimal("0.5"));
        assertThat(Document.of(decimal).asBigInteger()).isEqualTo(big);
        assertThat(Document.of(big).asLong()).isEqualTo(big.longValue());
    }

    @Test
    void asBigDecimal_onNonFiniteDouble_throwsSerializationException() {
        assertThatThrownBy(() -> Document.of(Double.NaN).asBigDecimal())
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("non-finite");
    }

    @Test
    void asNumber_returnsBoxedTypeMatchingDocumentType() {
        assertThat(Document.of((byte) 1).asNumber()).isInstanceOf(Byte.class);
        assertThat(Document.of((short) 1).asNumber()).isInstanceOf(Short.class);
        assertThat(Document.of(1).asNumber()).isInstanceOf(Integer.class);
        assertThat(Document.of(1L).asNumber()).isInstanceOf(Long.class);
        assertThat(Document.of(1f).asNumber()).isInstanceOf(Float.class);
        assertThat(Document.of(1d).asNumber()).isInstanceOf(Double.class);
        assertThat(Document.of(BigInteger.ONE).asNumber()).isInstanceOf(BigInteger.class);
        assertThat(Document.of(BigDecimal.ONE).asNumber()).isInstanceOf(BigDecimal.class);
    }

    @Test
    void coercion_toDifferentNonNumericType_throwsNamingBothTypes() {
        assertThatThrownBy(() -> Document.of("a").asBoolean())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a boolean document, but found string");
        assertThatThrownBy(() -> Document.of(1).asString())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Expected a string document, but found integer");
        assertThatThrownBy(() -> Document.of(true).asInteger()).isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> Document.of("1").asNumber()).isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> Document.of(1).asList()).isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> Document.of(1).asMap()).isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> Document.of(1).getMember("a")).isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> Document.of(1).getMemberNames()).isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> Document.of("a").asBlob()).isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> Document.of("a").asTimestamp()).isInstanceOf(SerializationException.class);
    }

    @Test
    void size_isElementCountForContainersAndMinusOneOtherwise() {
        assertThat(Document.of(Arrays.asList(Document.of(1), null)).size()).isEqualTo(2);
        assertThat(Document.of(Collections.singletonMap("a", Document.of(1))).size()).isEqualTo(1);
        assertThat(Document.of("a").size()).isEqualTo(-1);
    }

    @Test
    void map_keepsEntryOrderAndReturnsNullForAbsentMembers() {
        Map<String, Document> members = new LinkedHashMap<>();
        members.put("z", Document.of(1));
        members.put("a", null);
        members.put("m", Document.of("x"));
        Document map = Document.of(members);

        assertThat(map.getMemberNames()).containsExactly("z", "a", "m");
        assertThat(map.asMap().keySet()).containsExactly("z", "a", "m");
        assertThat(map.getMember("z")).isEqualTo(Document.of(1));
        assertThat(map.getMember("a")).isNull();
        assertThat(map.getMember("missing")).isNull();
    }

    @Test
    void containers_areUnmodifiableThroughTheDocument() {
        List<Document> elements = new ArrayList<>(Collections.singletonList(Document.of(1)));
        Document list = Document.of(elements);
        assertThatThrownBy(() -> list.asList().add(Document.of(2))).isInstanceOf(UnsupportedOperationException.class);

        Map<String, Document> members = new HashMap<>(Collections.singletonMap("a", Document.of(1)));
        Document map = Document.of(members);
        assertThatThrownBy(() -> map.asMap().put("b", Document.of(2))).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.getMemberNames().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void blob_returnsIndependentBuffersOverTheSameBytes() {
        ByteBuffer source = ByteBuffer.wrap("abc".getBytes(StandardCharsets.UTF_8));
        Document blob = Document.of(source);
        source.position(3);

        ByteBuffer first = blob.asBlob();
        first.get();
        ByteBuffer second = blob.asBlob();

        assertThat(second.remaining()).isEqualTo(3);
        assertThat(second.get()).isEqualTo((byte) 'a');
    }

    @Test
    void factories_rejectNullValues() {
        assertThatThrownBy(() -> Document.of((String) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Document.of((BigInteger) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Document.of((Instant) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Document.of((List<Document>) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Document.of((Map<String, Document>) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void equals_comparesTypeAndValue() {
        assertEqualDocuments(Document.of("a"), Document.of("a"));
        assertEqualDocuments(Document.of(1), Document.of(1));
        assertEqualDocuments(Document.of(new byte[] {1, 2}), Document.of(ByteBuffer.wrap(new byte[] {1, 2})));
        assertEqualDocuments(Document.of(Instant.EPOCH), Document.of(Instant.ofEpochMilli(0)));
        assertEqualDocuments(Document.of(new BigDecimal("1.10")), Document.of(new BigDecimal("1.1")));
        assertEqualDocuments(Document.of(new BigDecimal("0.00")), Document.of(BigDecimal.ZERO));
        assertEqualDocuments(Document.of(Double.NaN), Document.of(Double.NaN));
        assertEqualDocuments(Document.of(Arrays.asList(Document.of(1), null)),
                             Document.of(Arrays.asList(Document.of(1), null)));

        assertThat(Document.of(1)).isNotEqualTo(Document.of(1L));
        assertThat(Document.of(1)).isNotEqualTo(Document.of(2));
        assertThat(Document.of(1.0f)).isNotEqualTo(Document.of(1.0d));
        assertThat(Document.of(0.0d)).isNotEqualTo(Document.of(-0.0d));
        assertThat(Document.of("a")).isNotEqualTo("a");
        assertThat(Document.of(Collections.singletonList(Document.of(1))))
            .isNotEqualTo(Document.of(Arrays.asList(Document.of(1), Document.of(2))));
    }

    @Test
    void equals_ignoresMapOrderButNotKeyPresence() {
        Map<String, Document> first = new LinkedHashMap<>();
        first.put("a", Document.of(1));
        first.put("b", null);
        Map<String, Document> second = new LinkedHashMap<>();
        second.put("b", null);
        second.put("a", Document.of(1));
        assertEqualDocuments(Document.of(first), Document.of(second));

        Map<String, Document> other = new LinkedHashMap<>();
        other.put("a", Document.of(1));
        other.put("c", null);
        assertThat(Document.of(first)).isNotEqualTo(Document.of(other));
    }

    @Test
    void equals_withNumberPromotion_comparesNumericValuesAcrossTypes() {
        int promotion = DocumentEqualsFlags.NUMBER_PROMOTION;
        assertThat(Document.equals(Document.of(1), Document.of(1L), promotion)).isTrue();
        assertThat(Document.equals(Document.of((byte) 1), Document.of(BigDecimal.ONE), promotion)).isTrue();
        assertThat(Document.equals(Document.of(1.5f), Document.of(1.5d), promotion)).isTrue();
        assertThat(Document.equals(Document.of(new BigDecimal("10.01")), Document.of(BigInteger.TEN), promotion))
            .isFalse();
        assertThat(Document.equals(Document.of(1), Document.of("1"), promotion)).isFalse();
        assertThat(Document.equals(Document.of(Double.NaN), Document.of(BigDecimal.ONE), promotion)).isFalse();

        Document left = Document.of(Collections.singletonMap("a", Document.of(1)));
        Document right = Document.of(Collections.singletonMap("a", Document.of(1L)));
        assertThat(Document.equals(left, right)).isFalse();
        assertThat(Document.equals(left, right, promotion)).isTrue();
    }

    @Test
    void staticEquals_handlesNulls() {
        assertThat(Document.equals(null, null)).isTrue();
        assertThat(Document.equals(Document.of(1), null)).isFalse();
        assertThat(Document.equals(null, Document.of(1))).isFalse();
        assertThat(Document.hash(null)).isZero();
    }

    @Test
    void asObject_convertsToStandardLibraryTypes() {
        Map<String, Document> members = new LinkedHashMap<>();
        members.put("list", Document.of(Arrays.asList(Document.of(1), null)));
        members.put("string", Document.of("s"));
        members.put("null", null);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("list", Arrays.asList(1, null));
        expected.put("string", "s");
        expected.put("null", null);

        assertThat(Document.of(members).asObject()).isEqualTo(expected);
        assertThat(Document.of(true).asObject()).isEqualTo(true);
        assertThat(Document.of(Instant.EPOCH).asObject()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void numericCoercion_atIntegralBoundaries_wrapsLikeJavaCasts() {
        assertThat(Document.of(Integer.MAX_VALUE + 1L).asInteger()).isEqualTo(Integer.MIN_VALUE);
        assertThat(Document.of(128).asByte()).isEqualTo((byte) -128);
        assertThat(Document.of(Short.MAX_VALUE + 1).asShort()).isEqualTo(Short.MIN_VALUE);
        assertThat(Document.of(Long.MIN_VALUE).asInteger()).isZero();
        assertThat(Document.of(Long.MAX_VALUE).asFloat()).isEqualTo((float) Long.MAX_VALUE);
        assertThat(Document.of(Long.MAX_VALUE).asDouble()).isEqualTo((double) Long.MAX_VALUE);
        assertThat(Document.of(Byte.MIN_VALUE).asLong()).isEqualTo(Byte.MIN_VALUE);
        assertThat(Document.of(Short.MIN_VALUE).asBigInteger()).isEqualTo(BigInteger.valueOf(Short.MIN_VALUE));
    }

    @Test
    void numericCoercion_fromFloatingPoint_truncatesTowardZeroAndSaturates() {
        assertThat(Document.of(-1.9d).asInteger()).isEqualTo(-1);
        assertThat(Document.of(300.7d).asByte()).isEqualTo((byte) 44);
        assertThat(Document.of(1.9f).asShort()).isEqualTo((short) 1);
        assertThat(Document.of(1e20d).asLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(Document.of(1e20d).asInteger()).isEqualTo(Integer.MAX_VALUE);
        assertThat(Document.of(-1e20d).asLong()).isEqualTo(Long.MIN_VALUE);
        assertThat(Document.of(1e20d).asBigInteger()).isEqualTo(BigInteger.valueOf(Long.MAX_VALUE));
        assertThat(Document.of(Double.MAX_VALUE).asFloat()).isEqualTo(Float.POSITIVE_INFINITY);
        assertThat(Document.of(0.1d).asFloat()).isEqualTo(0.1f);
        assertThat(Document.of(0.1f).asBigDecimal()).isEqualTo(BigDecimal.valueOf((double) 0.1f));
    }

    @Test
    void numericCoercion_fromNanAndInfinity_followsJavaCasts() {
        assertThat(Document.of(Double.NaN).asInteger()).isZero();
        assertThat(Document.of(Float.NaN).asLong()).isZero();
        assertThat(Document.of(Double.NaN).asBigInteger()).isEqualTo(BigInteger.ZERO);
        assertThat(Document.of(Double.POSITIVE_INFINITY).asInteger()).isEqualTo(Integer.MAX_VALUE);
        assertThat(Document.of(Double.NEGATIVE_INFINITY).asLong()).isEqualTo(Long.MIN_VALUE);
        assertThat(Document.of(Float.POSITIVE_INFINITY).asDouble()).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(Document.of(Double.NaN).asFloat()).isNaN();
    }

    @Test
    void asBigDecimal_onNonFiniteFloatingPoint_throwsNamingTheTypes() {
        assertThatThrownBy(() -> Document.of(Float.POSITIVE_INFINITY).asBigDecimal())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Cannot convert a non-finite float document (Infinity) to a bigDecimal");
        assertThatThrownBy(() -> Document.of(Double.NEGATIVE_INFINITY).asBigDecimal())
            .isInstanceOf(SerializationException.class)
            .hasMessage("Cannot convert a non-finite double document (-Infinity) to a bigDecimal");
    }

    @Test
    void numericCoercion_fromBigValues_followsNumberConversions() {
        BigInteger twoTo64PlusOne = BigInteger.ONE.shiftLeft(64).add(BigInteger.ONE);
        assertThat(Document.of(twoTo64PlusOne).asLong()).isEqualTo(1L);
        assertThat(Document.of(twoTo64PlusOne).asInteger()).isEqualTo(1);
        assertThat(Document.of(twoTo64PlusOne).asDouble()).isEqualTo(twoTo64PlusOne.doubleValue());

        assertThat(Document.of(new BigDecimal("1.9")).asInteger()).isEqualTo(1);
        assertThat(Document.of(new BigDecimal("-1.9")).asLong()).isEqualTo(-1L);
        assertThat(Document.of(new BigDecimal("-1.9")).asBigInteger()).isEqualTo(BigInteger.ONE.negate());
        assertThat(Document.of(new BigDecimal("1e400")).asDouble()).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(Document.of(new BigDecimal("1.00000000000000000001")).asDouble()).isEqualTo(1.0d);
    }

    static Stream<Arguments> coercionFailures() {
        Document string = Document.of("12");
        Document integer = Document.of(1);
        return Stream.of(
            Arguments.of((Runnable) string::asBoolean, "Expected a boolean document, but found string"),
            Arguments.of((Runnable) string::asByte, "Expected a byte document, but found string"),
            Arguments.of((Runnable) string::asShort, "Expected a short document, but found string"),
            Arguments.of((Runnable) string::asInteger, "Expected an integer document, but found string"),
            Arguments.of((Runnable) string::asLong, "Expected a long document, but found string"),
            Arguments.of((Runnable) string::asFloat, "Expected a float document, but found string"),
            Arguments.of((Runnable) Document.of("NaN")::asDouble, "Expected a double document, but found string"),
            Arguments.of((Runnable) string::asBigInteger, "Expected a bigInteger document, but found string"),
            Arguments.of((Runnable) Document.of("1.5")::asBigDecimal,
                         "Expected a bigDecimal document, but found string"),
            Arguments.of((Runnable) string::asNumber, "Expected a number document, but found string"),
            Arguments.of((Runnable) integer::asString, "Expected a string document, but found integer"),
            Arguments.of((Runnable) string::asBlob, "Expected a blob document, but found string"),
            Arguments.of((Runnable) string::asTimestamp, "Expected a timestamp document, but found string"),
            Arguments.of((Runnable) integer::asList, "Expected a list document, but found integer"),
            Arguments.of((Runnable) Document.of(Collections.<Document>emptyList())::asMap,
                         "Expected a map document, but found list"),
            Arguments.of((Runnable) () -> integer.getMember("a"),
                         "Expected a map, structure, or union document, but found integer"),
            Arguments.of((Runnable) integer::getMemberNames,
                         "Expected a map, structure, or union document, but found integer"),
            Arguments.of((Runnable) Document.of(Instant.EPOCH)::asBoolean,
                         "Expected a boolean document, but found timestamp"),
            Arguments.of((Runnable) Document.of(new byte[0])::asString, "Expected a string document, but found blob"));
    }

    @ParameterizedTest
    @MethodSource("coercionFailures")
    void coercion_toTypeTheDocumentDoesNotHold_throwsNamingRequestedAndActualTypes(Runnable call, String message) {
        assertThatThrownBy(call::run)
            .isInstanceOf(SerializationException.class)
            .hasMessage(message);
    }

    @Test
    void getDiscriminator_onUntypedDocument_isNullAndExpectDiscriminatorThrows() {
        assertThat(Document.of("a").getDiscriminator()).isNull();
        assertThat(Document.EMPTY_MAP.getDiscriminator()).isNull();
        assertThatThrownBy(() -> Document.EMPTY_MAP.expectDiscriminator())
            .isInstanceOf(DiscriminatorException.class)
            .hasMessage("map document has no discriminator");
    }

    static Stream<Arguments> scalarContents() {
        ByteBuffer bytes = ByteBuffer.wrap(new byte[] {1});
        return Stream.of(
            Arguments.of(Document.of(true), "writeBoolean", PreludeSchemas.BOOLEAN, true),
            Arguments.of(Document.of((byte) 1), "writeByte", PreludeSchemas.BYTE, (byte) 1),
            Arguments.of(Document.of((short) 1), "writeShort", PreludeSchemas.SHORT, (short) 1),
            Arguments.of(Document.of(1), "writeInteger", PreludeSchemas.INTEGER, 1),
            Arguments.of(Document.of(1L), "writeLong", PreludeSchemas.LONG, 1L),
            Arguments.of(Document.of(1.5f), "writeFloat", PreludeSchemas.FLOAT, 1.5f),
            Arguments.of(Document.of(1.5d), "writeDouble", PreludeSchemas.DOUBLE, 1.5d),
            Arguments.of(Document.of(BigInteger.TEN), "writeBigInteger", PreludeSchemas.BIG_INTEGER, BigInteger.TEN),
            Arguments.of(Document.of(BigDecimal.TEN), "writeBigDecimal", PreludeSchemas.BIG_DECIMAL, BigDecimal.TEN),
            Arguments.of(Document.of("s"), "writeString", PreludeSchemas.STRING, "s"),
            Arguments.of(Document.of(bytes), "writeBlob", PreludeSchemas.BLOB, bytes),
            Arguments.of(Document.of(Instant.EPOCH), "writeTimestamp", PreludeSchemas.TIMESTAMP, Instant.EPOCH));
    }

    @ParameterizedTest
    @MethodSource("scalarContents")
    void serializeContents_scalarDocument_writesValueWithPreludeSchema(Document document, String method, Schema schema,
                                                                       Object value) {
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        document.serializeContents(serializer);

        assertThat(serializer.calls()).hasSize(1);
        assertThat(serializer.calls().get(0).method()).isEqualTo(method);
        assertThat(serializer.calls().get(0).schema()).isSameAs(schema);
        assertThat(serializer.calls().get(0).value()).isEqualTo(value);
    }

    @Test
    void serializeContents_containerDocuments_writeDocumentListAndMapSchemas() {
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        Document.of(Collections.singletonList(Document.of(1))).serializeContents(serializer);
        Document.EMPTY_MAP.serializeContents(serializer);

        assertThat(serializer.calls().get(0).method()).isEqualTo("writeList");
        assertThat(serializer.calls().get(0).schema().listMember().memberTarget()).isSameAs(PreludeSchemas.DOCUMENT);
        assertThat(serializer.calls().get(1).method()).isEqualTo("writeMap");
        assertThat(serializer.calls().get(1).schema().mapKeyMember().memberTarget()).isSameAs(PreludeSchemas.STRING);
        assertThat(serializer.calls().get(1).schema().mapValueMember().memberTarget())
            .isSameAs(PreludeSchemas.DOCUMENT);
    }

    @Test
    void serializeContents_defaultImplementation_writesValueOfType() {
        Document number = new Document() {
            @Override
            public ShapeType type() {
                return ShapeType.LONG;
            }

            @Override
            public long asLong() {
                return 7L;
            }
        };
        Document list = new Document() {
            @Override
            public ShapeType type() {
                return ShapeType.LIST;
            }

            @Override
            public List<Document> asList() {
                return Arrays.asList(number, null);
            }
        };
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        number.serializeContents(serializer);

        assertThat(serializer.calls().get(0).method()).isEqualTo("writeLong");
        assertThat(serializer.calls().get(0).schema()).isSameAs(PreludeSchemas.LONG);
        assertThat(serializer.calls().get(0).value()).isEqualTo(7L);
        assertThat(Document.of(list)).isEqualTo(Document.of(Arrays.asList(Document.of(7L), null)));
    }

    @Test
    void serializeContents_defaultImplementationOnStructure_throwsSerializationException() {
        Document structure = new Document() {
            @Override
            public ShapeType type() {
                return ShapeType.STRUCTURE;
            }
        };

        assertThatThrownBy(() -> structure.serializeContents(new RecordingShapeSerializer()))
            .isInstanceOf(SerializationException.class)
            .hasMessage("A structure document must implement serializeContents");
    }

    @Test
    void serialize_writesOneDocumentCall() {
        Document document = Document.of(1);
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        document.serialize(serializer);

        assertThat(serializer.calls()).hasSize(1);
        assertThat(serializer.calls().get(0).method()).isEqualTo("writeDocument");
        assertThat(serializer.calls().get(0).schema()).isSameAs(PreludeSchemas.DOCUMENT);
        assertThat(serializer.calls().get(0).value()).isSameAs(document);
    }

    @Test
    void booleanDocuments_areShared() {
        assertThat(Document.of(true)).isSameAs(Document.of(true));
        assertThat(Document.of(false).asBoolean()).isFalse();
    }

    private static void assertEqualDocuments(Document left, Document right) {
        assertThat(left).isEqualTo(right);
        assertThat(right).isEqualTo(left);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
        assertThat(Document.hash(left)).isEqualTo(left.hashCode());
    }
}
