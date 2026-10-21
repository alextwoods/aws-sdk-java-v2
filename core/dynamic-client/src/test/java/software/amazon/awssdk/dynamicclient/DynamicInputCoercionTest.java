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

package software.amazon.awssdk.dynamicclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.dynamicclient.CallTestClients.map;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The input coercion rules of {@link DynamicClient#createStruct} and of calls (Requirements 28.3 to 28.6, 28.8, 28.10
 * and 28.11), with their exact messages.
 */
class DynamicInputCoercionTest {
    private static final ShapeId PUT_ITEM_INPUT = ShapeId.from("test.json10#PutItemInput");
    private static final String MEMBER = "Cannot coerce member test.json10#PutItemInput$";

    private static DynamicClient client;

    @BeforeAll
    static void createClient() {
        client = CallTestClients.client(DynamicTestModels.json10(), new RecordingHttpClients.Sync(),
                                        new RecordingHttpClients.Async());
    }

    @AfterAll
    static void closeClient() {
        client.close();
    }

    @Test
    void createStruct_mapKeyedByMemberName_givesStructDocumentWithTheShapeIdAsDiscriminator() {
        Document result = client.createStruct(PUT_ITEM_INPUT, map("name", "widget", "count", 3, "flag", true));

        assertThat(result).isInstanceOf(StructDocument.class);
        assertThat(result.getDiscriminator())
            .isEqualTo(software.amazon.awssdk.schema.ShapeId.from("test.json10#PutItemInput"));
        assertThat(((StructDocument) result).sdkSchema()).isSameAs(client.getOperation("PutItem").inputSchema());
        assertThat(result.getMemberNames()).containsExactly("name", "count", "flag");
        assertThat(result.getMember("count").type()).isEqualTo(ShapeType.INTEGER);
        assertThat(result.getMember("count").asInteger()).isEqualTo(3);
    }

    @Test
    void createStruct_documentAlreadyOfTheSchema_returnsIt() {
        Document first = client.createStruct(PUT_ITEM_INPUT, map("name", "widget"));

        assertThat(client.createStruct(PUT_ITEM_INPUT, first)).isSameAs(first);
    }

    @Test
    void createStruct_nestedValues_areCoercedToTheirMemberTypes() {
        Document result = client.createStruct(PUT_ITEM_INPUT, map(
            "tags", Document.of(Arrays.asList(Document.of("a"), Document.of("b"))),
            "attributes", map("x", 1L),
            "nested", map("label", "n", "when", 10),
            "choice", map("number", 4L)));

        assertThat(result.getMember("tags").asList()).extracting(Document::asString).containsExactly("a", "b");
        assertThat(result.getMember("attributes").getMember("x").type()).isEqualTo(ShapeType.INTEGER);
        assertThat(result.getMember("nested").getDiscriminator())
            .isEqualTo(software.amazon.awssdk.schema.ShapeId.from("test.json10#Nested"));
        assertThat(result.getMember("nested").getMember("when").asTimestamp()).isEqualTo(Instant.ofEpochSecond(10));
        assertThat(result.getMember("choice").getMember("number").asInteger()).isEqualTo(4);
    }

    @Test
    void createStruct_unknownMember_failsNamingTheMemberAndTheStructure() {
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, map("name", "widget", "bogus", 1)))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Structure test.json10#PutItemInput has no member named \"bogus\". Its members are [name, "
                        + "count, tiny, small, big, huge, ratio, exact, flag, created, data, tags, attributes, nested, "
                        + "choice]. Members are keyed by Smithy member name, not by a protocol name such as jsonName.");
    }

    @Test
    void createStruct_unknownNestedMember_failsNamingTheNestedStructure() {
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, map("nested", map("labl", "x"))))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Structure test.json10#Nested has no member named \"labl\". Its members are [label, when]. "
                        + "Members are keyed by Smithy member name, not by a protocol name such as jsonName.");
    }

    static Stream<Arguments> timestamps() {
        return Stream.of(
            Arguments.of(Document.of(1700000000), Instant.ofEpochSecond(1700000000)),
            Arguments.of(Document.of(-5L), Instant.ofEpochSecond(-5)),
            Arguments.of(Document.of(1700000000.1239), Instant.ofEpochMilli(1700000000123L)),
            Arguments.of(Document.of(1.001), Instant.ofEpochMilli(1001)),
            Arguments.of(Document.of(new BigDecimal("12.3456789")), Instant.ofEpochMilli(12345)),
            Arguments.of(Document.of(BigInteger.valueOf(42)), Instant.ofEpochSecond(42)),
            Arguments.of(Document.of("2023-11-14T22:13:20.5Z"), Instant.parse("2023-11-14T22:13:20.5Z")),
            Arguments.of(Document.of(Instant.ofEpochSecond(7, 1)), Instant.ofEpochSecond(7, 1)));
    }

    @ParameterizedTest
    @MethodSource("timestamps")
    void createStruct_timestampMember_takesEpochSecondsDateTimeOrTimestamp(Document value, Instant expected) {
        Document result = client.createStruct(PUT_ITEM_INPUT, map("created", value));

        assertThat(result.getMember("created").asTimestamp()).isEqualTo(expected);
    }

    @Test
    void createStruct_invalidDateTime_fails() {
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, map("created", "yesterday")))
            .isInstanceOf(SerializationException.class)
            .hasMessage(MEMBER + "created: the string is not a valid date-time (RFC 3339) timestamp, such as "
                        + "1985-04-12T23:20:50.52Z");
    }

    @Test
    void createStruct_booleanForTimestamp_fails() {
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, map("created", true)))
            .isInstanceOf(SerializationException.class)
            .hasMessage(MEMBER + "created: expected a timestamp, number or string document, but found a boolean "
                        + "document");
    }

    @Test
    void createStruct_nonFiniteEpochSeconds_fails() {
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, map("created", Double.NaN)))
            .isInstanceOf(SerializationException.class)
            .hasMessage(MEMBER + "created: the value is out of range for a timestamp");
    }

    @Test
    void createStruct_stringForBlob_fails() {
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, map("data", "aGVsbG8=")))
            .isInstanceOf(SerializationException.class)
            .hasMessage(MEMBER + "data: a blob member needs a blob document, but found a string document. Pass the "
                        + "bytes with Document.of(byte[]) or Document.of(ByteBuffer).");
    }

    @Test
    void createStruct_blobDocumentForBlob_keepsTheBytes() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

        Document result = client.createStruct(PUT_ITEM_INPUT, map("data", Document.of(bytes)));

        assertThat(result.getMember("data").asBlob()).isEqualTo(ByteBuffer.wrap(bytes));
    }

    static Stream<Arguments> outOfRange() {
        return Stream.of(
            Arguments.of("tiny", Document.of(128), "a byte"),
            Arguments.of("tiny", Document.of(-129), "a byte"),
            Arguments.of("small", Document.of(32768), "a short"),
            Arguments.of("count", Document.of(2147483648L), "an integer"),
            Arguments.of("count", Document.of(-2147483649.0), "an integer"),
            Arguments.of("big", Document.of(BigInteger.ONE.shiftLeft(63)), "a long"),
            Arguments.of("big", Document.of(new BigDecimal("-9223372036854775809")), "a long"));
    }

    @ParameterizedTest
    @MethodSource("outOfRange")
    void createStruct_integerOutOfRange_failsInsteadOfNarrowing(String member, Document value, String type) {
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, map(member, value)))
            .isInstanceOf(SerializationException.class)
            .hasMessage(MEMBER + member + ": the value is out of range for " + type + " member");
    }

    @Test
    void createStruct_integerBounds_areAccepted() {
        Document result = client.createStruct(PUT_ITEM_INPUT, map(
            "tiny", Document.of(-128), "small", Document.of(32767L), "count", Document.of(2147483647.0),
            "big", Document.of(BigInteger.valueOf(Long.MAX_VALUE)), "huge", Document.of(new BigDecimal("1E+30"))));

        assertThat(result.getMember("tiny").asByte()).isEqualTo((byte) -128);
        assertThat(result.getMember("small").asShort()).isEqualTo((short) 32767);
        assertThat(result.getMember("count").asInteger()).isEqualTo(Integer.MAX_VALUE);
        assertThat(result.getMember("big").asLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(result.getMember("huge").asBigInteger()).isEqualTo(BigInteger.TEN.pow(30));
    }

    @Test
    void createStruct_fractionForInteger_fails() {
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, map("count", 1.5)))
            .isInstanceOf(SerializationException.class)
            .hasMessage(MEMBER + "count: an integer member needs an integral number, but the value is not one");
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, map("huge", Document.of(new BigDecimal("0.5")))))
            .isInstanceOf(SerializationException.class)
            .hasMessage(MEMBER + "huge: a bigInteger member needs an integral number, but the value is not one");
    }

    @Test
    void createStruct_floatingPointMembers_takeAnyNumber() {
        Document result = client.createStruct(PUT_ITEM_INPUT, map("ratio", 3, "exact", 0.1));

        assertThat(result.getMember("ratio").asDouble()).isEqualTo(3.0);
        assertThat(result.getMember("exact").asBigDecimal()).isEqualTo(new BigDecimal("0.1"));
    }

    static Stream<Arguments> wrongTypes() {
        return Stream.of(
            Arguments.of(map("count", "3"), MEMBER + "count: expected a number document, but found a string document"),
            Arguments.of(map("name", 3), MEMBER + "name: expected a string document, but found an integer document"),
            Arguments.of(map("flag", "true"), MEMBER + "flag: expected a boolean document, but found a string document"),
            Arguments.of(map("nested", "x"),
                         MEMBER + "nested: expected a map or structure document, but found a string document"),
            Arguments.of(map("tags", map("a", "b")), MEMBER + "tags: expected a list document, but found a map document"),
            Arguments.of(map("attributes", Document.of(Arrays.asList(Document.of(1)))),
                         MEMBER + "attributes: expected a map or structure document, but found a list document"),
            Arguments.of(map("tags", Document.of(Arrays.asList(Document.of(1)))),
                         "Cannot coerce member test.json10#TagList$member: expected a string document, but found an "
                         + "integer document"),
            Arguments.of(map("attributes", map("a", 1L << 40)),
                         "Cannot coerce member test.json10#AttributeMap$value: the value is out of range for an integer "
                         + "member"));
    }

    @ParameterizedTest
    @MethodSource("wrongTypes")
    void createStruct_valueOfTheWrongType_failsNamingTheMember(Document input, String message) {
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, input))
            .isInstanceOf(SerializationException.class)
            .hasMessage(message);
    }

    @Test
    void createStruct_unionWithTwoMembers_fails() {
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, map("choice", map("text", "a", "number", 1))))
            .isInstanceOf(SerializationException.class)
            .hasMessage(MEMBER + "choice: union test.json10#Choice can have one member set, but text and number are "
                        + "both set");
    }

    @Test
    void createStruct_notAMap_fails() {
        assertThatThrownBy(() -> client.createStruct(PUT_ITEM_INPUT, Document.of("x")))
            .isInstanceOf(SerializationException.class)
            .hasMessage("Cannot coerce a document to structure test.json10#PutItemInput: expected a map or structure "
                        + "document, but found a string document");
    }

    @Test
    void createStruct_shapeThatIsNotAStructure_fails() {
        assertThatThrownBy(() -> client.createStruct(ShapeId.from("test.json10#Nope"), map()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Shape test.json10#Nope is not in the model");
        assertThatThrownBy(() -> client.createStruct(ShapeId.from("test.json10#TagList"), map()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Shape test.json10#TagList is a list, not a structure");
        assertThatThrownBy(() -> client.createStruct(ShapeId.from("test.json10#Choice"), map()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Shape test.json10#Choice is a union, not a structure");
    }
}
