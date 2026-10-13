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

package software.amazon.awssdk.codecs.cbor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Port of smithy-java {@code software.amazon.smithy.java.cbor.CborDocumentTest}.
 *
 * <p>The smithy-java tests build their input from JSON text with the JSON codec, which is not ported yet (task 7.1);
 * these build the same values with {@link Document#of}, write them with the CBOR codec and read them back. The JSON
 * integers become long documents and the JSON decimals double documents, as the smithy-java JSON documents serialize.
 * Hamcrest matchers become AssertJ assertions. The discriminator tests and {@code deserializesIntoBuilder} are not
 * ported: the SDK {@code Document} has no discriminator or {@code deserializeInto} yet (design B.1, task 11.5).
 */
class CborDocumentTest {
    private static final Rpcv2CborCodec CODEC = Rpcv2CborCodec.builder().build();

    private static ShapeDeserializer toCbor(Document d) {
        return CODEC.createDeserializer(CODEC.serialize(d));
    }

    private static Map<String, Document> map(Object... keysAndValues) {
        Map<String, Document> result = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            result.put((String) keysAndValues[i], (Document) keysAndValues[i + 1]);
        }
        return result;
    }

    private static Document list(Document... values) {
        return Document.of(Arrays.asList(values));
    }

    @Test
    void convertsNumberToNumber() {
        Document document = toCbor(Document.of(120L)).readDocument();

        assertThat(document.type()).isEqualTo(ShapeType.LONG);
        assertThat(document.asByte()).isEqualTo((byte) 120);
        assertThat(document.asShort()).isEqualTo((short) 120);
        assertThat(document.asInteger()).isEqualTo(120);
        assertThat(document.asLong()).isEqualTo(120L);
        assertThat(document.asFloat()).isEqualTo(120f);
        assertThat(document.asDouble()).isEqualTo(120.0);
        assertThat(document.asBigInteger()).isEqualTo(BigInteger.valueOf(120));
        assertThat(document.asBigDecimal()).isEqualByComparingTo(BigDecimal.valueOf(120.0));
    }

    @Test
    void convertsDoubleToNumber() {
        Document document = toCbor(Document.of(1.1)).readDocument();

        assertThat(document.type()).isEqualTo(ShapeType.DOUBLE);
        assertThat(document.asFloat()).isEqualTo(1.1f);
        assertThat(document.asDouble()).isEqualTo(1.1);
    }

    @Test
    void convertsTrueToBoolean() {
        Document document = toCbor(Document.of(true)).readDocument();

        assertThat(document.type()).isEqualTo(ShapeType.BOOLEAN);
        assertThat(document.asBoolean()).isTrue();
    }

    @Test
    void convertsFalseToBoolean() {
        Document document = toCbor(Document.of(false)).readDocument();

        assertThat(document.type()).isEqualTo(ShapeType.BOOLEAN);
        assertThat(document.asBoolean()).isFalse();
    }

    @Test
    void convertsToTimestampWithEpochSeconds() {
        Document document = toCbor(Document.of(Instant.EPOCH)).readDocument();

        assertThat(document.type()).isEqualTo(ShapeType.TIMESTAMP);
        assertThat(document.asTimestamp()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void convertsToTimestampFailsOnUnknownType() {
        Document document = toCbor(Document.of(true)).readDocument();

        assertThatThrownBy(document::asTimestamp)
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("Expected a timestamp document, but found");
    }

    @Test
    void convertsToList() {
        Document document = toCbor(list(Document.of(1L), Document.of(2L), Document.of(3L))).readDocument();

        assertThat(document.type()).isEqualTo(ShapeType.LIST);
        assertThat(document.size()).isEqualTo(3);

        List<Document> list = document.asList();
        assertThat(list).hasSize(3);
        assertThat(list.get(0).type()).isEqualTo(ShapeType.LONG);
        assertThat(list.get(0).asInteger()).isEqualTo(1);
        assertThat(list.get(1).asInteger()).isEqualTo(2);
        assertThat(list.get(2).asInteger()).isEqualTo(3);
    }

    @Test
    void convertsToMap() {
        Document document = toCbor(Document.of(map("a", Document.of(1L), "b", Document.of(true)))).readDocument();

        assertThat(document.type()).isEqualTo(ShapeType.MAP);
        assertThat(document.size()).isEqualTo(2);

        Map<String, Document> map = document.asMap();
        assertThat(map.keySet()).hasSize(2);
        assertThat(map.get("a").type()).isEqualTo(ShapeType.LONG);
        assertThat(map.get("a").asInteger()).isEqualTo(1);
        assertThat(document.getMember("a").type()).isEqualTo(ShapeType.LONG);

        assertThat(map.get("b").type()).isEqualTo(ShapeType.BOOLEAN);
        assertThat(map.get("b").asBoolean()).isTrue();
        assertThat(document.getMember("b").type()).isEqualTo(ShapeType.BOOLEAN);
    }

    @Test
    void otherDocumentsReturnSizeOfNegativeOne() {
        Document document = toCbor(Document.of(1L)).readDocument();

        assertThat(document.size()).isEqualTo(-1);
    }

    @Test
    void nullAndMissingMapMembersReturnsNull() {
        Document document = toCbor(Document.of(map("a", null))).readDocument();

        assertThat(document.getMember("a")).isNull();
        assertThat(document.getMember("d")).isNull();
    }

    @ParameterizedTest
    @MethodSource("failToConvertSource")
    void failToConvert(Document input, Consumer<Document> consumer) {
        Document document = toCbor(input).readDocument();

        assertThatThrownBy(() -> consumer.accept(document)).isInstanceOf(SerializationException.class);
    }

    static List<Arguments> failToConvertSource() {
        Document one = Document.of(1L);
        Document text = Document.of("1");
        return Arrays.asList(
            Arguments.of(one, (Consumer<Document>) Document::asBoolean),
            Arguments.of(one, (Consumer<Document>) Document::asBlob),
            Arguments.of(one, (Consumer<Document>) Document::asString),
            Arguments.of(one, (Consumer<Document>) Document::asList),
            Arguments.of(one, (Consumer<Document>) Document::asMap),
            Arguments.of(one, (Consumer<Document>) Document::asBlob),

            Arguments.of(text, (Consumer<Document>) Document::asBoolean),
            Arguments.of(text, (Consumer<Document>) Document::asList),
            Arguments.of(text, (Consumer<Document>) Document::asMap),
            Arguments.of(text, (Consumer<Document>) Document::asBlob),
            Arguments.of(text, (Consumer<Document>) Document::asBoolean),
            Arguments.of(text, (Consumer<Document>) Document::asByte),
            Arguments.of(text, (Consumer<Document>) Document::asShort),
            Arguments.of(text, (Consumer<Document>) Document::asInteger),
            Arguments.of(text, (Consumer<Document>) Document::asLong),
            Arguments.of(text, (Consumer<Document>) Document::asFloat),
            Arguments.of(text, (Consumer<Document>) Document::asDouble),
            Arguments.of(text, (Consumer<Document>) Document::asBigInteger),
            Arguments.of(text, (Consumer<Document>) Document::asBigDecimal));
    }

    @ParameterizedTest
    @MethodSource("serializeContentSource")
    void serializeContent(Document input) {
        Document document = toCbor(input).readDocument();
        ByteBuffer ser = CODEC.serialize(document);
        Document roundtrip = CODEC.createDeserializer(ser).readDocument();
        assertThat(roundtrip).isEqualTo(document);
    }

    static List<Arguments> serializeContentSource() {
        return Arrays.asList(
            Arguments.of(Document.of(true)),
            Arguments.of(Document.of(false)),
            Arguments.of(Document.of(1L)),
            Arguments.of(Document.of(1.1)),
            Arguments.of(list(Document.of(1L), Document.of(2L), Document.of(3L))),
            Arguments.of(Document.of(map("a", Document.of(1L),
                                         "b", list(Document.of(1L), Document.of(true), Document.of(-20L),
                                                   Document.of("hello"))))));
    }

    @ParameterizedTest
    @MethodSource("checkEqualitySource")
    void checkEquality(Document left, Document right, boolean equal) {
        Document leftValue = toCbor(left).readDocument();
        Document rightValue = toCbor(right).readDocument();

        assertThat(leftValue.equals(rightValue)).isEqualTo(equal);
    }

    static List<Arguments> checkEqualitySource() {
        return Arrays.asList(
            Arguments.of(Document.of(1L), Document.of(1L), true),
            Arguments.of(Document.of(1L), Document.of(1.1), false),
            Arguments.of(Document.of(true), Document.of(true), true),
            Arguments.of(Document.of(true), Document.of(false), false),
            Arguments.of(Document.of(1L), Document.of(false), false),
            Arguments.of(Document.of(1L), Document.of("1"), false),
            Arguments.of(Document.of("foo"), Document.of("foo"), true),
            Arguments.of(list(Document.of("foo")), list(Document.of("foo")), true),
            Arguments.of(Document.of(map("foo", Document.of("foo"))), Document.of(map("foo", Document.of("foo"))), true),
            Arguments.of(Document.of(map("foo", Document.of("foo"))), Document.of(map("foo", Document.of("bar"))),
                         false));
    }

    @ParameterizedTest
    @MethodSource("bigDecimalDocumentSource")
    void convertsBigDecimalToDocument(BigDecimal bigDecimal) {
        ByteBuffer ser = CODEC.serialize(Document.of(bigDecimal));
        Document document = CODEC.createDeserializer(ser).readDocument();

        assertThat(document.type()).isEqualTo(ShapeType.BIG_DECIMAL);
        assertThat(document.asBigDecimal()).isEqualByComparingTo(bigDecimal);
        assertThat(document).isEqualTo(Document.of(bigDecimal));
    }

    static List<Arguments> bigDecimalDocumentSource() {
        return Arrays.asList(
            Arguments.of(new BigDecimal("12345.6789")),
            Arguments.of(new BigDecimal("0.00001")),
            Arguments.of(new BigDecimal("9999999999999999999999999999.123456789")),
            Arguments.of(new BigDecimal("3.14159")),
            Arguments.of(new BigDecimal("-42.5")),
            Arguments.of(BigDecimal.ZERO));
    }

    @Test
    void canNormalizeCborDocuments() {
        Document cbor = toCbor(Document.of(true)).readDocument();

        assertThat(Document.equals(cbor, Document.of(true))).isTrue();
    }
}
