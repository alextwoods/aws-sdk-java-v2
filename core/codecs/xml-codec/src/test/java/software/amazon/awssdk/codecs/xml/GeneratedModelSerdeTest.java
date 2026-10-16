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

package software.amazon.awssdk.codecs.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.codecs.xml.TestCollections.listOf;
import static software.amazon.awssdk.codecs.xml.TestCollections.mapOf;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.ALL_LISTS_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.BLOB_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.COMPLEX_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.DENSE_LIST_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.FLATTENED_LIST_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.FLATTENED_MAP_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.INNER_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.NAMESPACED_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.NESTED_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.NUMERIC_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.RECURSIVE_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.SIMPLE_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.SPARSE_LIST_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.STRING_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.STRUCT_ITEM;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.TIMESTAMP_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.XML_ATTRIBUTE_STRUCT;
import static software.amazon.awssdk.codecs.xml.XmlTestModel.XML_NAME_STRUCT;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;

/**
 * Port of smithy-java {@code software.amazon.smithy.java.xml.GeneratedModelSerdeTest}. smithy-java runs these tests
 * against classes generated from {@code model/test.smithy}; the port uses {@link XmlTestModel} schemas with
 * {@link SchemaStruct} values (enums as strings).
 */
public class GeneratedModelSerdeTest extends ProviderTestBase {

    private static SchemaStruct roundtrip(boolean useNative, SchemaStruct original) {
        return roundtrip(useNative, useNative, original);
    }

    private static SchemaStruct roundtrip(boolean ser, boolean de, SchemaStruct original) {
        try (XmlCodec serCodec = XmlCodec.builder().useNative(ser).build();
             XmlCodec deCodec = XmlCodec.builder().useNative(de).build()) {
            ByteBuffer serialized = serCodec.serialize(original);
            return deCodec.deserializeShape(serialized, SchemaStruct.builder(original.sdkSchema()));
        }
    }

    private static SchemaStruct.Builder builder(Schema schema) {
        return SchemaStruct.builder(schema);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(SchemaStruct struct, String member) {
        return (List<Object>) struct.get(member);
    }

    private static ByteBuffer utf8(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }

    // --- Simple Struct ---

    @PerProvider
    void simpleStructRoundtrip(boolean useNative) {
        SchemaStruct original = builder(SIMPLE_STRUCT)
            .set("name", "test")
            .set("age", 42)
            .set("active", true)
            .set("score", 98.6)
            .set("createdAt", Instant.parse("2025-01-15T10:30:00Z"))
            .build();

        assertThat(roundtrip(useNative, original)).isEqualTo(original);
    }

    // --- Complex Struct ---

    private static SchemaStruct nested(String field1, int field2) {
        return builder(NESTED_STRUCT).set("field1", field1).set("field2", field2).build();
    }

    private static SchemaStruct buildComplexStruct() {
        SchemaStruct inner = builder(INNER_STRUCT)
            .set("value", "inner-value")
            .set("numbers", listOf(1, 2, 3, 4, 5))
            .build();
        SchemaStruct nested = builder(NESTED_STRUCT)
            .set("field1", "nested-field")
            .set("field2", 100)
            .set("inner", inner)
            .build();
        return builder(COMPLEX_STRUCT)
            .set("id", "bench-001")
            .set("count", 999)
            .set("enabled", true)
            .set("ratio", 1.618)
            .set("score", 2.718f)
            .set("bigCount", 1_000_000L)
            .set("optionalString", "optional-value")
            .set("optionalInt", 42)
            .set("createdAt", Instant.parse("2025-06-01T12:00:00Z"))
            .set("payload", utf8("binary-payload-data"))
            .set("tags", listOf("alpha", "beta", "gamma", "delta"))
            .set("intList", listOf(10, 20, 30, 40, 50))
            .set("metadata", mapOf("key1", "value1", "key2", "value2"))
            .set("intMap", mapOf("a", 1, "b", 2))
            .set("nested", nested)
            .set("optionalNested", nested("opt-nested", 200))
            .set("structList", listOf(nested, nested))
            .set("structMap", mapOf("first", nested))
            .set("color", "GREEN")
            .set("colorList", listOf("RED", "BLUE", "YELLOW"))
            .set("bigIntValue", new BigInteger("123456789012345678901234567890"))
            .set("bigDecValue", new BigDecimal("99999.99999"))
            .build();
    }

    @Test
    void complexStructRoundtrip() {
        SchemaStruct original = buildComplexStruct();
        assertThat(roundtrip(NATIVE, original)).isEqualTo(original);
    }

    // --- Numeric Boundary Roundtrips ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void numericMinValuesRoundtrip(boolean ser, boolean de) {
        SchemaStruct original = builder(NUMERIC_STRUCT)
            .set("byteVal", Byte.MIN_VALUE)
            .set("shortVal", Short.MIN_VALUE)
            .set("intVal", Integer.MIN_VALUE)
            .set("longVal", Long.MIN_VALUE)
            .set("floatVal", Float.MIN_VALUE)
            .set("doubleVal", Double.MIN_VALUE)
            .set("bigIntVal", new BigInteger("-99999999999999999999999999999"))
            .set("bigDecVal", new BigDecimal("-99999.99999"))
            .build();
        assertThat(roundtrip(ser, de, original)).isEqualTo(original);
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void numericMaxValuesRoundtrip(boolean ser, boolean de) {
        SchemaStruct original = builder(NUMERIC_STRUCT)
            .set("byteVal", Byte.MAX_VALUE)
            .set("shortVal", Short.MAX_VALUE)
            .set("intVal", Integer.MAX_VALUE)
            .set("longVal", Long.MAX_VALUE)
            .set("floatVal", Float.MAX_VALUE)
            .set("doubleVal", Double.MAX_VALUE)
            .set("bigIntVal", new BigInteger("99999999999999999999999999999"))
            .set("bigDecVal", new BigDecimal("123456789.123456789"))
            .build();
        assertThat(roundtrip(ser, de, original)).isEqualTo(original);
    }

    @PerProvider
    void numericZeroValues(boolean useNative) {
        SchemaStruct original = builder(NUMERIC_STRUCT)
            .set("byteVal", (byte) 0)
            .set("shortVal", (short) 0)
            .set("intVal", 0)
            .set("longVal", 0L)
            .set("floatVal", 0.0f)
            .set("doubleVal", 0.0)
            .set("bigIntVal", BigInteger.ZERO)
            .set("bigDecVal", BigDecimal.ZERO)
            .build();
        assertThat(roundtrip(useNative, original)).isEqualTo(original);
    }

    @PerProvider
    void bigDecimalVariousScales(boolean useNative) {
        for (BigDecimal bd : listOf(new BigDecimal("42"),
                                    new BigDecimal("1.005"),
                                    new BigDecimal("1E+10"),
                                    new BigDecimal("12345678901234567890.12345"))) {
            SchemaStruct original = builder(NUMERIC_STRUCT).set("bigDecVal", bd).build();
            assertThat((BigDecimal) roundtrip(useNative, original).get("bigDecVal")).isEqualByComparingTo(bd);
        }
    }

    @PerProvider
    void bigIntegerSmallAndLarge(boolean useNative) {
        for (BigInteger bi : listOf(BigInteger.valueOf(42),
                                    BigInteger.valueOf(-42),
                                    new BigInteger("123456789012345678901234567890123456789"))) {
            SchemaStruct original = builder(NUMERIC_STRUCT).set("bigIntVal", bi).build();
            assertThat(roundtrip(useNative, original)).isEqualTo(original);
        }
    }

    // --- String Edge Cases ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void stringEdgeCasesRoundtrip(boolean ser, boolean de) {
        // Note: \r is excluded because XML spec Section 2.11 normalizes CR to LF
        for (String s : listOf("",
                               "abcdefghijklmnopqrstuvwxyz0123456789",
                               "quote:\" backslash:\\ tab:\t newline:\n",
                               "caf\u00e9",
                               "\u00e9\u4e2d\u00fc",
                               "\ud83d\ude00\ud83c\udf89",
                               "hello\tworld\n\u00e9\ud83d\ude00\"quoted\"")) {
            SchemaStruct original = builder(STRING_STRUCT).set("value", s).build();
            assertThat(roundtrip(ser, de, original)).isEqualTo(original);
        }
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void xmlSpecialCharactersRoundtrip(boolean ser, boolean de) {
        for (String s : listOf("<tag>value</tag>",
                               "a & b",
                               "1 < 2 > 0",
                               "'single' and \"double\" quotes",
                               "<![CDATA[not actually cdata]]>")) {
            SchemaStruct original = builder(STRING_STRUCT).set("value", s).build();
            assertThat(roundtrip(ser, de, original)).isEqualTo(original);
        }
    }

    // --- Timestamp Format Roundtrips ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void timestampAllFormatsRoundtrip(boolean ser, boolean de) {
        Instant epoch = Instant.EPOCH;
        SchemaStruct original = builder(TIMESTAMP_STRUCT)
            .set("epochSeconds", epoch)
            .set("dateTime", epoch)
            .set("httpDate", epoch)
            .build();
        assertThat(roundtrip(ser, de, original)).isEqualTo(original);
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void timestampWithNanoseconds(boolean ser, boolean de) {
        Instant withNanos = Instant.parse("2025-01-15T10:30:00.123456789Z");
        SchemaStruct original = builder(TIMESTAMP_STRUCT).set("dateTime", withNanos).build();
        assertThat(roundtrip(ser, de, original)).isEqualTo(original);
    }

    @PerProvider
    void timestampPreEpoch(boolean useNative) {
        Instant preEpoch = Instant.parse("1969-12-31T23:59:59Z");
        SchemaStruct original = builder(TIMESTAMP_STRUCT)
            .set("epochSeconds", preEpoch)
            .set("dateTime", preEpoch)
            .set("httpDate", preEpoch)
            .build();
        assertThat(roundtrip(useNative, original)).isEqualTo(original);
    }

    @PerProvider
    void timestampLeapYear(boolean useNative) {
        Instant leapDay = LocalDate.of(2024, 2, 29).atStartOfDay(ZoneOffset.UTC).toInstant();
        SchemaStruct original = builder(TIMESTAMP_STRUCT).set("dateTime", leapDay).build();
        assertThat(roundtrip(useNative, original)).isEqualTo(original);
    }

    @PerProvider
    void timestampAllMonthsHttpDate(boolean useNative) {
        for (int month = 1; month <= 12; month++) {
            Instant instant = LocalDate.of(2025, month, 15).atStartOfDay(ZoneOffset.UTC).toInstant();
            SchemaStruct original = builder(TIMESTAMP_STRUCT).set("httpDate", instant).build();
            assertThat(roundtrip(useNative, original)).isEqualTo(original);
        }
    }

    // --- Collection Roundtrips ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void emptyCollections(boolean ser, boolean de) {
        SchemaStruct original = builder(COMPLEX_STRUCT)
            .set("id", "empty")
            .set("count", 0)
            .set("enabled", false)
            .set("ratio", 0.0)
            .set("score", 0.0f)
            .set("bigCount", 0L)
            .set("nested", nested("f", 0))
            .set("tags", listOf())
            .set("intList", listOf())
            .set("metadata", mapOf())
            .set("intMap", mapOf())
            .build();
        assertThat(roundtrip(ser, de, original)).isEqualTo(original);
    }

    @Test
    void nestedStructCollections() {
        SchemaStruct nested1 = nested("first", 1);
        SchemaStruct nested2 = nested("second", 2);
        SchemaStruct original = builder(COMPLEX_STRUCT)
            .set("id", "nested-collections")
            .set("count", 0)
            .set("enabled", false)
            .set("ratio", 0.0)
            .set("score", 0.0f)
            .set("bigCount", 0L)
            .set("nested", nested1)
            .set("structList", listOf(nested1, nested2))
            .set("structMap", mapOf("a", nested1, "b", nested2))
            .build();
        assertThat(roundtrip(NATIVE, original)).isEqualTo(original);
    }

    // --- Enum Roundtrips ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void enumAllVariantsRoundtrip(boolean ser, boolean de) {
        for (String color : listOf("RED", "GREEN", "BLUE", "YELLOW")) {
            SchemaStruct original = builder(COMPLEX_STRUCT)
                .set("id", "enum")
                .set("count", 0)
                .set("enabled", false)
                .set("ratio", 0.0)
                .set("score", 0.0f)
                .set("bigCount", 0L)
                .set("nested", nested("f", 0))
                .set("color", color)
                .build();
            assertThat(roundtrip(ser, de, original)).isEqualTo(original);
        }
    }

    // --- XML-specific: xmlName Trait ---

    private static SchemaStruct xmlNameStruct() {
        return builder(XML_NAME_STRUCT)
            .set("id", "test-id")
            .set("displayName", "Test Name")
            .set("normalField", "normal")
            .build();
    }

    @PerProvider
    void xmlNameTraitRoundtrip(boolean useNative) {
        SchemaStruct original = xmlNameStruct();
        assertThat(roundtrip(useNative, original)).isEqualTo(original);
    }

    @PerProvider
    void xmlNameTraitSerialization(boolean useNative) {
        try (XmlCodec codec = codec(useNative)) {
            String xml = codec.serializeToString(xmlNameStruct());
            assertThat(xml).contains("<CustomRoot>");
            assertThat(xml).contains("<ID>");
            assertThat(xml).contains("<DisplayName>");
            assertThat(xml).contains("<normalField>");
        }
    }

    // --- XML-specific: xmlAttribute Trait ---

    private static SchemaStruct xmlAttributeStruct(String version, String identifier) {
        return builder(XML_ATTRIBUTE_STRUCT)
            .set("version", version)
            .set("identifier", identifier)
            .set("content", "hello world")
            .build();
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void xmlAttributeRoundtrip(boolean ser, boolean de) {
        SchemaStruct original = xmlAttributeStruct("1.0", "abc-123");
        assertThat(roundtrip(ser, de, original)).isEqualTo(original);
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void xmlAttributeSpecialCharactersRoundtrip(boolean ser, boolean de) {
        SchemaStruct original = xmlAttributeStruct("caf\u00e9 & \"quoted\"", "l'\u00e9t\u00e9 < 2027");
        assertThat(roundtrip(ser, de, original)).isEqualTo(original);
    }

    @PerProvider
    void xmlAttributeSerialization(boolean useNative) {
        try (XmlCodec codec = codec(useNative)) {
            String xml = codec.serializeToString(xmlAttributeStruct("1.0", "abc-123"));
            assertThat(xml).contains("version=\"1.0\"");
            assertThat(xml).contains("id=\"abc-123\"");
            assertThat(xml).contains("<content>hello world</content>");
        }
    }

    // --- XML-specific: xmlFlattened Trait ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void flattenedListRoundtrip(boolean ser, boolean de) {
        SchemaStruct original = builder(FLATTENED_LIST_STRUCT)
            .set("items", listOf("a", "b", "c"))
            .set("numbers", listOf(1, 2, 3))
            .set("normalList", listOf("x", "y"))
            .build();
        assertThat(roundtrip(ser, de, original)).isEqualTo(original);
    }

    @PerProvider
    void flattenedListSerialization(boolean useNative) {
        SchemaStruct original = builder(FLATTENED_LIST_STRUCT)
            .set("items", listOf("a", "b", "c"))
            .set("normalList", listOf("x", "y"))
            .build();
        try (XmlCodec codec = codec(useNative)) {
            String xml = codec.serializeToString(original);
            assertThat(xml).contains("<items>a</items>");
            assertThat(xml).contains("<items>b</items>");
            assertThat(xml).contains("<items>c</items>");
            assertThat(xml).contains("<normalList><member>x</member><member>y</member></normalList>");
        }
    }

    @ParameterizedTest
    @MethodSource("crossProviders")
    void flattenedMapRoundtrip(boolean ser, boolean de) {
        SchemaStruct original = builder(FLATTENED_MAP_STRUCT)
            .set("entries", mapOf("k1", "v1", "k2", "v2"))
            .set("normalMap", mapOf("a", "b"))
            .build();
        assertThat(roundtrip(ser, de, original)).isEqualTo(original);
    }

    // --- XML-specific: xmlNamespace Trait ---

    private static SchemaStruct namespacedStruct() {
        return builder(NAMESPACED_STRUCT).set("name", "test").set("value", 42).build();
    }

    @PerProvider
    void namespacedStructRoundtrip(boolean useNative) {
        SchemaStruct original = namespacedStruct();
        assertThat(roundtrip(useNative, original)).isEqualTo(original);
    }

    @PerProvider
    void namespacedStructSerialization(boolean useNative) {
        try (XmlCodec codec = codec(useNative)) {
            assertThat(codec.serializeToString(namespacedStruct())).contains("xmlns=\"https://example.com/test\"");
        }
    }

    // --- Recursive / Nested Depth ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void recursiveStructRoundtrip(boolean ser, boolean de) {
        SchemaStruct current = builder(RECURSIVE_STRUCT).set("value", "leaf").build();
        for (int i = 9; i >= 1; i--) {
            current = builder(RECURSIVE_STRUCT).set("value", "level-" + i).set("child", current).build();
        }
        assertThat(roundtrip(ser, de, current)).isEqualTo(current);
    }

    // --- Blob Roundtrips ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void blobRoundtrip(boolean ser, boolean de) {
        SchemaStruct empty = builder(BLOB_STRUCT).set("data", ByteBuffer.wrap(new byte[0])).build();
        assertThat(roundtrip(ser, de, empty)).isEqualTo(empty);

        SchemaStruct small = builder(BLOB_STRUCT).set("data", utf8("hello")).build();
        assertThat(roundtrip(ser, de, small)).isEqualTo(small);

        byte[] largeData = new byte[1000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        SchemaStruct large = builder(BLOB_STRUCT).set("data", ByteBuffer.wrap(largeData)).build();
        assertThat(roundtrip(ser, de, large)).isEqualTo(large);
    }

    // --- Null optional members ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void nullOptionalMembersRoundtrip(boolean ser, boolean de) {
        SchemaStruct original = builder(NUMERIC_STRUCT).build();
        assertThat(roundtrip(ser, de, original)).isEqualTo(original);
    }

    // --- All-types list roundtrip ---

    @ParameterizedTest
    @MethodSource("crossProviders")
    void allListTypesRoundtrip(boolean ser, boolean de) {
        SchemaStruct original = builder(ALL_LISTS_STRUCT)
            .set("booleans", listOf(true, false, true))
            .set("bytes", listOf((byte) 1, (byte) -128, (byte) 127))
            .set("shorts", listOf((short) 1, (short) -32768, (short) 32767))
            .set("ints", listOf(1, Integer.MIN_VALUE, Integer.MAX_VALUE))
            .set("longs", listOf(1L, Long.MIN_VALUE, Long.MAX_VALUE))
            .set("floats", listOf(1.5f, Float.MIN_VALUE, Float.MAX_VALUE))
            .set("doubles", listOf(1.5, Double.MIN_VALUE, Double.MAX_VALUE))
            .set("bigInts", listOf(BigInteger.ZERO, new BigInteger("99999999999999999999")))
            .set("bigDecs", listOf(BigDecimal.ZERO, new BigDecimal("99999.99999")))
            .set("strings", listOf("hello", "world"))
            .set("blobs", listOf(utf8("a"), utf8("b")))
            .set("timestamps", listOf(Instant.EPOCH, Instant.parse("2025-01-15T10:30:00Z")))
            .build();
        assertThat(roundtrip(ser, de, original)).isEqualTo(original);
    }

    // --- Malformed XML validation (parity between native and StAX) ---

    private static SchemaStruct deserialize(boolean useNative, String xml, Schema schema) {
        try (XmlCodec codec = codec(useNative)) {
            return codec.deserializeShape(xml, SchemaStruct.builder(schema));
        }
    }

    private static SchemaStruct deserialize(boolean useNative, byte[] xml, Schema schema) {
        try (XmlCodec codec = codec(useNative)) {
            return codec.deserializeShape(ByteBuffer.wrap(xml), SchemaStruct.builder(schema));
        }
    }

    // <SimpleStruct><name>{content}</{endName}></SimpleStruct> with raw bytes in the middle.
    private static byte[] simpleStructBytes(byte[] content, byte[] endName) {
        byte[] head = "<SimpleStruct><name>".getBytes(StandardCharsets.US_ASCII);
        byte[] mid = "</".getBytes(StandardCharsets.US_ASCII);
        byte[] tail = "></SimpleStruct>".getBytes(StandardCharsets.US_ASCII);
        ByteBuffer out = ByteBuffer.allocate(head.length + content.length + mid.length + endName.length + tail.length);
        out.put(head).put(content).put(mid).put(endName).put(tail);
        return out.array();
    }

    @PerProvider
    void invalidUtf8IsRejected(boolean useNative) {
        byte[] input = simpleStructBytes(new byte[] {'h', 'i'}, new byte[] {'n', (byte) 0x97, 'm', 'e'});
        assertThatThrownBy(() -> deserialize(useNative, input, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void invalidUtf8ContinuationByteAlone(boolean useNative) {
        byte[] input = simpleStructBytes(new byte[] {(byte) 0x80}, "name".getBytes(StandardCharsets.US_ASCII));
        assertThatThrownBy(() -> deserialize(useNative, input, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void nullByteInContentIsRejected(boolean useNative) {
        byte[] input = "<SimpleStruct><name>hi\0there</name></SimpleStruct>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> deserialize(useNative, input, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void numericCharRefToControlCharIsRejected(boolean useNative) {
        String xml = "<SimpleStruct><name>hi&#1;there</name></SimpleStruct>";
        assertThatThrownBy(() -> deserialize(useNative, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void mismatchedEndTagOnScalar(boolean useNative) {
        String xml = "<SimpleStruct><name>hello</val></SimpleStruct>";
        assertThatThrownBy(() -> deserialize(useNative, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void mismatchedOuterEndTag(boolean useNative) {
        String xml = "<SimpleStruct><name>hi</name></WrongName>";
        assertThatThrownBy(() -> deserialize(useNative, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    private static final String COMPLEX_PREFIX = "<ComplexStruct><id>1</id><count>1</count><enabled>true</enabled>"
                                                 + "<ratio>1.0</ratio><score>1.0</score><bigCount>1</bigCount>"
                                                 + "<nested><field1>a</field1><field2>1</field2></nested>";

    @PerProvider
    void mismatchedEndTagInList(boolean useNative) {
        String xml = COMPLEX_PREFIX + "<tags><member>hello</wrong></tags></ComplexStruct>";
        assertThatThrownBy(() -> deserialize(useNative, xml, COMPLEX_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void mismatchedEndTagInMapKey(boolean useNative) {
        String xml = COMPLEX_PREFIX + "<metadata><entry><key>k</wrong><value>v</value></entry></metadata></ComplexStruct>";
        assertThatThrownBy(() -> deserialize(useNative, xml, COMPLEX_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @Test
    void emptyListMembersHandledConsistently() {
        String xml = COMPLEX_PREFIX
                     + "<tags><member></member><member>hello</member><member></member><member>world</member></tags>"
                     + "</ComplexStruct>";
        SchemaStruct staxResult = deserialize(STAX, xml, COMPLEX_STRUCT);
        SchemaStruct nativeResult = deserialize(NATIVE, xml, COMPLEX_STRUCT);
        assertThat(nativeResult.get("tags")).isEqualTo(staxResult.get("tags"));
    }

    @PerProvider
    void selfClosingElementDeserializesAsEmpty(boolean useNative) {
        String xml = "<SimpleStruct><name/><age>5</age></SimpleStruct>";
        SchemaStruct result = deserialize(useNative, xml, SIMPLE_STRUCT);
        assertThat(result.get("name")).isEqualTo("");
        assertThat(result.get("age")).isEqualTo(5);
    }

    @PerProvider
    void selfClosingElementDeserializesAsEmptyInStruct(boolean useNative) {
        String xml = "<SimpleStruct><name/><age>5</age></SimpleStruct>";
        SchemaStruct result = deserialize(useNative, xml, SIMPLE_STRUCT);
        assertThat(result.get("age")).isEqualTo(5);
    }

    @Test
    void selfClosingElementsInListSkippedAsNull() {
        // Self-closing and empty elements in sparse lists are treated as null and preserved
        String xml = COMPLEX_PREFIX + "<tags><member/><member>hello</member><member/></tags></ComplexStruct>";
        SchemaStruct staxResult = deserialize(STAX, xml, COMPLEX_STRUCT);
        SchemaStruct nativeResult = deserialize(NATIVE, xml, COMPLEX_STRUCT);
        assertThat(nativeResult.get("tags")).isEqualTo(staxResult.get("tags"));
        assertThat(list(nativeResult, "tags")).containsExactly(null, "hello", null);
    }

    @PerProvider
    void denseStructListDeserializes(boolean useNative) {
        String xml = "<DenseListStruct><structs><member><id>abcd</id></member></structs></DenseListStruct>";
        SchemaStruct result = deserialize(useNative, xml, DENSE_LIST_STRUCT);
        assertThat(list(result, "structs")).containsExactly(builder(STRUCT_ITEM).set("id", "abcd").build());
    }

    @PerProvider
    void prettyPrintedStructListDeserializes(boolean useNative) {
        String xml = "<DenseListStruct>\n"
                     + "  <structs>\n"
                     + "    <member>\n"
                     + "      <id>abcd</id>\n"
                     + "    </member>\n"
                     + "  </structs>\n"
                     + "</DenseListStruct>\n";
        SchemaStruct result = deserialize(useNative, xml, DENSE_LIST_STRUCT);
        assertThat(list(result, "structs")).containsExactly(builder(STRUCT_ITEM).set("id", "abcd").build());
    }

    // Both empty forms, <member/> and <member></member>, are null and rejected by a dense list.
    @PerProvider
    void denseStructListNullMemberThrows(boolean useNative) {
        String singleTag = "<DenseListStruct><structs><member/></structs></DenseListStruct>";
        assertThatThrownBy(() -> deserialize(useNative, singleTag, DENSE_LIST_STRUCT))
            .isInstanceOf(SerializationException.class);
        String separateTag = "<DenseListStruct><structs><member></member></structs></DenseListStruct>";
        assertThatThrownBy(() -> deserialize(useNative, separateTag, DENSE_LIST_STRUCT))
            .isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void sparseStructListNullMemberIsNull(boolean useNative) {
        String xml = "<SparseListStruct><structs><member/></structs></SparseListStruct>";
        SchemaStruct result = deserialize(useNative, xml, SPARSE_LIST_STRUCT);
        assertThat(list(result, "structs")).containsExactly((Object) null);
    }

    @PerProvider
    void densePopulatedStringListDeserializes(boolean useNative) {
        String xml = "<DenseListStruct><strings><member>a</member><member>x</member></strings></DenseListStruct>";
        SchemaStruct result = deserialize(useNative, xml, DENSE_LIST_STRUCT);
        assertThat(list(result, "strings")).containsExactly("a", "x");
    }

    @PerProvider
    void denseStringListEmptyMemberThrows(boolean useNative) {
        String xml = "<DenseListStruct><strings><member></member><member>x</member></strings></DenseListStruct>";
        assertThatThrownBy(() -> deserialize(useNative, xml, DENSE_LIST_STRUCT))
            .isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void sparseStringListEmptyMemberIsNull(boolean useNative) {
        String xml = "<SparseListStruct><strings><member></member><member>x</member></strings></SparseListStruct>";
        SchemaStruct result = deserialize(useNative, xml, SPARSE_LIST_STRUCT);
        assertThat(list(result, "strings")).containsExactly(null, "x");
    }

    @PerProvider
    void trailingContentAfterRootIsRejected(boolean useNative) {
        String xml = "<SimpleStruct><name>hi</name><age>1</age></SimpleStruct>extra";
        assertThatThrownBy(() -> deserialize(useNative, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void unknownElementWithMismatchedInnerTags(boolean useNative) {
        String xml = "<SimpleStruct><garbled></SimpleStruct>";
        assertThatThrownBy(() -> deserialize(useNative, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void unclosedElementRejectsAtEof(boolean useNative) {
        String xml = "<SimpleStruct><unknown>text";
        assertThatThrownBy(() -> deserialize(useNative, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void truncatedEndTagRejectsAtEof(boolean useNative) {
        String xml = "<SimpleStruct></SimpleStruct";
        assertThatThrownBy(() -> deserialize(useNative, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void unterminatedEntityReferenceIsRejected(boolean useNative) {
        String xml = "<SimpleStruct><name>hello&world</name></SimpleStruct>";
        assertThatThrownBy(() -> deserialize(useNative, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void undeclaredEntityReferenceIsRejected(boolean useNative) {
        String xml = "<SimpleStruct><name>&foo;</name></SimpleStruct>";
        assertThatThrownBy(() -> deserialize(useNative, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void cdataCloseInTextContentIsRejected(boolean useNative) {
        String xml = "<SimpleStruct><name>a]]>b</name></SimpleStruct>";
        assertThatThrownBy(() -> deserialize(useNative, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @PerProvider
    void invalidMarkupDeclarationIsRejected(boolean useNative) {
        String xml = "<SimpleStruct><!ELEMENT foo></SimpleStruct>";
        assertThatThrownBy(() -> deserialize(useNative, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @Test
    void infinityParsedCorrectlyNotFalsePositive() {
        String xml = "<SimpleStruct><name>x</name><age>1</age><score>ISO-885</score></SimpleStruct>";
        assertThatThrownBy(() -> deserialize(STAX, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
        assertThatThrownBy(() -> deserialize(NATIVE, xml, SIMPLE_STRUCT)).isInstanceOf(SerializationException.class);
    }

    @Test
    void actualInfinityParsedCorrectly() {
        String xml = "<SimpleStruct><name>x</name><age>1</age><score>Infinity</score></SimpleStruct>";
        SchemaStruct stax = deserialize(STAX, xml, SIMPLE_STRUCT);
        SchemaStruct nat = deserialize(NATIVE, xml, SIMPLE_STRUCT);
        assertThat(nat.get("score")).isEqualTo(stax.get("score"));
        assertThat(nat.get("score")).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    void carriageReturnNormalization() {
        String xml = "<SimpleStruct><name>a\rb\r\nc</name><age>1</age></SimpleStruct>";
        SchemaStruct staxResult = deserialize(STAX, xml, SIMPLE_STRUCT);
        SchemaStruct nativeResult = deserialize(NATIVE, xml, SIMPLE_STRUCT);
        assertThat(nativeResult.get("name")).isEqualTo(staxResult.get("name"));
        assertThat(nativeResult.get("name")).isEqualTo("a\nb\nc");
    }

    @Test
    void crossCodecRoundtripWithListsAndMaps() {
        String xml = "<ComplexStruct><id>x</id><count>5</count><enabled>false</enabled>"
                     + "<ratio>2.5</ratio><score>1.5</score><bigCount>100</bigCount>"
                     + "<nested><field1>n1</field1><field2>10</field2></nested>"
                     + "<tags><member>a</member><member>b</member></tags>"
                     + "<metadata><entry><key>k1</key><value>v1</value></entry></metadata>"
                     + "</ComplexStruct>";
        SchemaStruct staxResult = deserialize(STAX, xml, COMPLEX_STRUCT);
        SchemaStruct nativeResult = deserialize(NATIVE, xml, COMPLEX_STRUCT);
        assertThat(nativeResult.get("id")).isEqualTo(staxResult.get("id"));
        assertThat(nativeResult.get("tags")).isEqualTo(staxResult.get("tags"));
        assertThat(nativeResult.get("metadata")).isEqualTo(staxResult.get("metadata"));
    }

    // --- Error code parsing ---

    private static String parseErrorCode(boolean useNative, String xml) {
        XmlCodec codec = codec(useNative);
        ShapeDeserializer deser = codec.createDeserializer(ByteBuffer.wrap(xml.getBytes(StandardCharsets.UTF_8)));
        return XmlUtil.parseErrorCodeName(deser);
    }

    @PerProvider
    void parseErrorCodeFromErrorResponse(boolean useNative) {
        String xml = "<ErrorResponse><Error><Type>Sender</Type><Code>InvalidGreeting</Code>"
                     + "<Message>Hi</Message></Error></ErrorResponse>";
        assertThat(parseErrorCode(useNative, xml)).isEqualTo("InvalidGreeting");
    }

    @PerProvider
    void parseErrorCodeFromBareError(boolean useNative) {
        String xml = "<Error><Code>ComplexError</Code><Message>Something</Message></Error>";
        assertThat(parseErrorCode(useNative, xml)).isEqualTo("ComplexError");
    }

    @PerProvider
    void parseErrorCodeFromEc2Response(boolean useNative) {
        String xml = "<Response><Errors><Error><Code>AuthFailure</Code>"
                     + "<Message>Unauthorized</Message></Error></Errors></Response>";
        assertThat(parseErrorCode(useNative, xml)).isEqualTo("AuthFailure");
    }

    @Test
    void errorStructDeserializationThroughErrorResponse() {
        String xml = "<ErrorResponse><Error><name>test</name><age>42</age></Error></ErrorResponse>";
        SchemaStruct result = deserialize(NATIVE, xml, SIMPLE_STRUCT);
        assertThat(result.get("name")).isEqualTo("test");
        assertThat(result.get("age")).isEqualTo(42);
    }
}
