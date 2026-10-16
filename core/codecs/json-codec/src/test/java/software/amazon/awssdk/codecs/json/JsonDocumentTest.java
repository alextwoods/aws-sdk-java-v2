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


package software.amazon.awssdk.codecs.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static software.amazon.awssdk.codecs.json.TestCollections.listOf;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;

public class JsonDocumentTest extends ProviderTestBase {


    @PerProvider
    public void convertsNumberToNumber(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        ShapeDeserializer de = codec.createDeserializer("120".getBytes(StandardCharsets.UTF_8));

        Document document = de.readDocument();
        assertThat(document.type()).isEqualTo(ShapeType.INTEGER);
        assertThat(document.asByte()).isEqualTo((byte) 120);
        assertThat(document.asShort()).isEqualTo((short) 120);
        assertThat(document.asInteger()).isEqualTo(120);
        assertThat(document.asLong()).isEqualTo(120L);
        assertThat(document.asFloat()).isEqualTo(120f);
        assertThat(document.asDouble()).isEqualTo(120.0);
        assertThat(document.asBigInteger()).isEqualTo(BigInteger.valueOf(120));
        assertThat(document.asBigDecimal()).isEqualByComparingTo(BigDecimal.valueOf(120.0));
    }

    @PerProvider
    public void convertsDoubleToNumber(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        ShapeDeserializer de = codec.createDeserializer("1.1".getBytes(StandardCharsets.UTF_8));

        Document document = de.readDocument();
        assertThat(document.type()).isEqualTo(ShapeType.DOUBLE);
        assertThat(document.asFloat()).isEqualTo(1.1f);
        assertThat(document.asDouble()).isEqualTo(1.1);
    }

    @PerProvider
    public void convertsToBoolean(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        ShapeDeserializer de = codec.createDeserializer("true".getBytes(StandardCharsets.UTF_8));

        Document document = de.readDocument();
        assertThat(document.type()).isEqualTo(ShapeType.BOOLEAN);
        assertThat(document.asBoolean()).isEqualTo(true);
    }

    @PerProvider
    public void convertsToTimestampFailsOnUnknownType(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        ShapeDeserializer de = codec.createDeserializer("true".getBytes(StandardCharsets.UTF_8));
        Document document = de.readDocument();

        SerializationException e = Assertions.assertThrows(SerializationException.class, document::asTimestamp);
        assertThat(e.getMessage()).contains("Expected a timestamp document");
    }

    @PerProvider
    public void convertsToList(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        ShapeDeserializer de = codec.createDeserializer("[1, 2, 3]".getBytes(StandardCharsets.UTF_8));

        Document document = de.readDocument();
        assertThat(document.type()).isEqualTo(ShapeType.LIST);
        assertThat(document.size()).isEqualTo(3);

        List<Document> list = document.asList();
        assertThat(list).hasSize(3);
        assertThat(list.get(0).type()).isEqualTo(ShapeType.INTEGER);
        assertThat(list.get(0).asInteger()).isEqualTo(1);
        assertThat(list.get(1).asInteger()).isEqualTo(2);
        assertThat(list.get(2).asInteger()).isEqualTo(3);
    }

    @PerProvider
    public void convertsToMap(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        ShapeDeserializer de = codec.createDeserializer("{\"a\":1,\"b\":true}".getBytes(StandardCharsets.UTF_8));

        Document document = de.readDocument();
        assertThat(document.type()).isEqualTo(ShapeType.MAP);
        assertThat(document.size()).isEqualTo(2);

        Map<String, Document> map = document.asMap();
        assertThat(map.keySet()).hasSize(2);
        assertThat(map.get("a").type()).isEqualTo(ShapeType.INTEGER);
        assertThat(map.get("a").asInteger()).isEqualTo(1);
        assertThat(document.getMember("a").type()).isEqualTo(ShapeType.INTEGER);

        assertThat(map.get("b").type()).isEqualTo(ShapeType.BOOLEAN);
        assertThat(map.get("b").asBoolean()).isEqualTo(true);
        assertThat(document.getMember("b").type()).isEqualTo(ShapeType.BOOLEAN);
    }

    @PerProvider
    public void otherDocumentsReturnSizeOfNegativeOne(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        ShapeDeserializer de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
        Document document = de.readDocument();

        assertThat(document.size()).isEqualTo(-1);
    }

    @PerProvider
    public void nullAndMissingMapMembersReturnsNull(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        ShapeDeserializer de = codec.createDeserializer("{\"a\":null}".getBytes(StandardCharsets.UTF_8));

        Document document = de.readDocument();
        assertThat(document.getMember("c")).isNull();
        assertThat(document.getMember("d")).isNull();
    }

    @PerProvider
    public void nullMapMemberRoundtrip(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        Document doc = codec.createDeserializer("{\"a\":null}".getBytes(StandardCharsets.UTF_8)).readDocument();
        Document roundtrip = codec.createDeserializer(codec.serialize(doc)).readDocument();

        assertEquals(doc, roundtrip);
    }

    @PerProvider
    public void nullListMemberRoundtrip(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        Document doc = codec.createDeserializer("[null]".getBytes(StandardCharsets.UTF_8)).readDocument();
        Document roundtrip = codec.createDeserializer(codec.serialize(doc)).readDocument();

        assertEquals(doc, roundtrip);
    }

    @PerProvider
    public void nullDocument(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        ShapeDeserializer de = codec.createDeserializer("null".getBytes(StandardCharsets.UTF_8));

        Document document = de.readDocument();
        assertNull(document);
    }

    @ParameterizedTest
    @MethodSource("failToConvertSource")
    public void failToConvert(JsonSerdeProvider provider, String json, Consumer<Document> consumer) {
        JsonCodec codec = codec(provider);
        ShapeDeserializer de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
        Document document = de.readDocument();

        Assertions.assertThrows(SerializationException.class, () -> consumer.accept(document));
    }

    public static List<Arguments> failToConvertSource() {
        List<Arguments> tests = listOf(
                Arguments.of("1", (Consumer<Document>) Document::asBoolean),
                Arguments.of("1", (Consumer<Document>) Document::asBlob),
                Arguments.of("1", (Consumer<Document>) Document::asString),
                Arguments.of("1", (Consumer<Document>) Document::asList),
                Arguments.of("1", (Consumer<Document>) Document::asMap),
                Arguments.of("1", (Consumer<Document>) Document::asBlob),

                Arguments.of("\"1\"", (Consumer<Document>) Document::asBoolean),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asList),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asMap),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asBlob),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asBoolean),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asByte),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asShort),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asInteger),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asLong),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asFloat),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asDouble),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asBigInteger),
                Arguments.of("\"1\"", (Consumer<Document>) Document::asBigDecimal));

        ArrayList<Arguments> result = new ArrayList<Arguments>();
        for (Arguments provider : providers()) {
            for (Arguments test : tests) {
                result.add(Arguments.of(provider.get()[0], test.get()[0], test.get()[1]));
            }
        }
        return result;
    }

    @ParameterizedTest
    @MethodSource("serializeContentSource")
    public void serializeContent(JsonSerdeProvider provider, String json) {
        JsonCodec codec = codec(provider);
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        ShapeSerializer se = codec.createSerializer(sink);
        ShapeDeserializer de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
        Document document = de.readDocument();
        // Adapted: SDK Document has no serializeContents yet; serialize() writes the same value.
        document.serialize(se);
        se.flush();

        assertThat(new String(sink.toByteArray(), StandardCharsets.UTF_8)).isEqualTo(json);
    }

    @ParameterizedTest
    @MethodSource("serializeContentSource")
    public void serializeDocument(JsonSerdeProvider provider, String json) {
        JsonCodec codec = codec(provider);
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        ShapeSerializer se = codec.createSerializer(sink);
        ShapeDeserializer de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
        Document document = de.readDocument();
        document.serialize(se);
        se.flush();

        assertThat(new String(sink.toByteArray(), StandardCharsets.UTF_8)).isEqualTo(json);
    }

    public static List<Arguments> serializeContentSource() {
        List<Arguments> tests = listOf(
                Arguments.of("true"),
                Arguments.of("false"),
                Arguments.of("1"),
                Arguments.of("1.1"),
                Arguments.of("[1,2,3]"),
                Arguments.of("{\"a\":1,\"b\":[1,true,-20,\"hello\"]}"));

        ArrayList<Arguments> result = new ArrayList<Arguments>();
        for (Arguments provider : providers()) {
            for (Arguments test : tests) {
                result.add(Arguments.of(provider.get()[0], test.get()[0]));
            }
        }
        return result;
    }

    @ParameterizedTest
    @MethodSource("checkEqualitySource")
    public void checkEquality(JsonSerdeProvider provider, String left, String right, boolean equal) {
        JsonCodec codec = codec(provider);

        ShapeDeserializer de1 = codec.createDeserializer(left.getBytes(StandardCharsets.UTF_8));
        Document leftValue = de1.readDocument();

        ShapeDeserializer de2 = codec.createDeserializer(right.getBytes(StandardCharsets.UTF_8));
        Document rightValue = de2.readDocument();

        assertThat(leftValue.equals(rightValue)).isEqualTo(equal);
    }

    public static List<Arguments> checkEqualitySource() {
        List<Arguments> tests = listOf(
                Arguments.of("1", "1", true),
                Arguments.of("1", "1.1", false),
                Arguments.of("true", "true", true),
                Arguments.of("true", "false", false),
                Arguments.of("1", "false", false),
                Arguments.of("1", "\"1\"", false),
                Arguments.of("\"foo\"", "\"foo\"", true),
                Arguments.of("[\"foo\"]", "[\"foo\"]", true),
                Arguments.of("{\"foo\":\"foo\"}", "{\"foo\":\"foo\"}", true),
                Arguments.of("{\"foo\":\"foo\"}", "{\"foo\":\"bar\"}", false));

        ArrayList<Arguments> result = new ArrayList<Arguments>();
        for (Arguments provider : providers()) {
            for (Arguments test : tests) {
                result.add(Arguments.of(provider.get()[0], test.get()[0], test.get()[1], test.get()[2]));
            }
        }
        return result;
    }

    @PerProvider
    public void canNormalizeJsonDocuments(JsonSerdeProvider provider) {
        JsonCodec codec = codec(provider);
        ShapeDeserializer de = codec.createDeserializer("true".getBytes(StandardCharsets.UTF_8));
        Document json = de.readDocument();

        assertThat(Document.equals(json, Document.of(true))).isEqualTo(true);
    }

}
