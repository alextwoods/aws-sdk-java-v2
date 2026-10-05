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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.smithy.runtime.document.DocumentCoercionException.Reason;
import software.amazon.awssdk.smithy.runtime.fixture.Widget;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

class SchemaDocumentsTest {

    private static final String NS = "software.amazon.awssdk.fixture";
    private static final Schema COLOR = Schema.createEnum(id("Color"), Set.of("red", "green"));
    private static final Schema SIZE = Schema.createIntEnum(id("Size"), Set.of(1, 10));
    private static final Schema SECRET = Schema.createString(id("Secret"), new SensitiveTrait());
    private static final Schema LABELS = Schema.listBuilder(id("Labels"))
                                               .putMember("member", PreludeSchemas.STRING)
                                               .build();
    private static final Schema WHENS = Schema.mapBuilder(id("Whens"))
                                              .putMember("key", PreludeSchemas.STRING)
                                              .putMember("value", PreludeSchemas.TIMESTAMP)
                                              .build();
    private static final Schema INNER = Schema.structureBuilder(id("Inner"))
                                              .putMember("label", PreludeSchemas.STRING)
                                              .putMember("count", PreludeSchemas.INTEGER)
                                              .build();
    private static final Schema CHOICE = Schema.unionBuilder(id("Choice"))
                                               .putMember("text", PreludeSchemas.STRING)
                                               .putMember("inner", INNER)
                                               .build();
    private static final Schema ALL = Schema.structureBuilder(id("AllKinds"))
        .putMember("boolean", PreludeSchemas.BOOLEAN)
        .putMember("byte", PreludeSchemas.BYTE)
        .putMember("short", PreludeSchemas.SHORT)
        .putMember("integer", PreludeSchemas.INTEGER)
        .putMember("long", PreludeSchemas.LONG)
        .putMember("float", PreludeSchemas.FLOAT)
        .putMember("double", PreludeSchemas.DOUBLE)
        .putMember("bigInteger", PreludeSchemas.BIG_INTEGER)
        .putMember("bigDecimal", PreludeSchemas.BIG_DECIMAL)
        .putMember("string", PreludeSchemas.STRING)
        .putMember("secret", SECRET)
        .putMember("color", COLOR)
        .putMember("size", SIZE)
        .putMember("blob", PreludeSchemas.BLOB)
        .putMember("timestamp", PreludeSchemas.TIMESTAMP)
        .putMember("dateTime", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
        .putMember("document", PreludeSchemas.DOCUMENT)
        .putMember("labels", LABELS)
        .putMember("whens", WHENS)
        .putMember("inner", INNER)
        .putMember("choice", CHOICE)
        .build();

    private static final Instant WHEN = Instant.parse("2026-03-04T05:06:07.250Z");
    private static final BigInteger HUGE = BigInteger.TWO.pow(70);

    private static ShapeId id(String name) {
        return ShapeId.from(NS + "#" + name);
    }

    /** An untyped Smithy Java document (no discriminators, no schemas) with a value for every member of ALL. */
    private static Document allKindsSource() {
        Map<String, Document> members = new LinkedHashMap<>();
        // Deliberately not in schema order.
        members.put("choice", Document.of(Map.of("inner", Document.of(Map.of("label", Document.of("nested"))))));
        members.put("boolean", Document.of(true));
        members.put("byte", Document.of((byte) -7));
        members.put("short", Document.of((short) 300));
        members.put("integer", Document.of(70_000));
        members.put("long", Document.of(1L << 40));
        members.put("float", Document.of(1.5f));
        members.put("double", Document.of(0.1d));
        members.put("bigInteger", Document.of(HUGE));
        members.put("bigDecimal", Document.of(new BigDecimal("12.25")));
        members.put("string", Document.of("text"));
        members.put("secret", Document.of("hunter2"));
        members.put("color", Document.of("red"));
        members.put("size", Document.of(10));
        members.put("blob", Document.of(new byte[] {1, 2, 3}));
        members.put("timestamp", Document.of(WHEN));
        members.put("dateTime", Document.of(WHEN));
        members.put("document", Document.of(Map.of("free", Document.of(List.of(Document.of(1), Document.of("x"))))));
        members.put("labels", Document.of(List.of(Document.of("b"), Document.of("a"), Document.of("b"))));
        members.put("whens", Document.of(Map.of("at", Document.of(WHEN))));
        members.put("inner", Document.of(Map.of("label", Document.of("inside"), "count", Document.of(3))));
        return Document.of(members);
    }

    private static ModeledDocument all() {
        return assertInstanceOf(ModeledDocument.class, SchemaDocuments.coerce(allKindsSource(), ALL));
    }

    @Test
    void everyFiniteKindKeepsItsExactKindShapeIdAndValue() {
        ModeledDocument document = all();

        assertEquals(ShapeType.STRUCTURE, document.type());
        assertEquals(ALL.id(), document.discriminator());
        assertSame(ALL, document.schema());
        assertEquals(List.of("boolean", "byte", "short", "integer", "long", "float", "double", "bigInteger",
                             "bigDecimal", "string", "secret", "color", "size", "blob", "timestamp", "dateTime",
                             "document", "labels", "whens", "inner", "choice"),
                     List.copyOf(document.getMemberNames()), "members are in schema order");

        assertKind(document, "boolean", ShapeType.BOOLEAN, PreludeSchemas.BOOLEAN.id());
        assertKind(document, "byte", ShapeType.BYTE, PreludeSchemas.BYTE.id());
        assertKind(document, "short", ShapeType.SHORT, PreludeSchemas.SHORT.id());
        assertKind(document, "integer", ShapeType.INTEGER, PreludeSchemas.INTEGER.id());
        assertKind(document, "long", ShapeType.LONG, PreludeSchemas.LONG.id());
        assertKind(document, "float", ShapeType.FLOAT, PreludeSchemas.FLOAT.id());
        assertKind(document, "double", ShapeType.DOUBLE, PreludeSchemas.DOUBLE.id());
        assertKind(document, "bigInteger", ShapeType.BIG_INTEGER, PreludeSchemas.BIG_INTEGER.id());
        assertKind(document, "bigDecimal", ShapeType.BIG_DECIMAL, PreludeSchemas.BIG_DECIMAL.id());
        assertKind(document, "string", ShapeType.STRING, PreludeSchemas.STRING.id());
        assertKind(document, "color", ShapeType.ENUM, COLOR.id());
        assertKind(document, "size", ShapeType.INT_ENUM, SIZE.id());
        assertKind(document, "blob", ShapeType.BLOB, PreludeSchemas.BLOB.id());
        assertKind(document, "timestamp", ShapeType.TIMESTAMP, PreludeSchemas.TIMESTAMP.id());
        assertKind(document, "labels", ShapeType.LIST, LABELS.id());
        assertKind(document, "whens", ShapeType.MAP, WHENS.id());
        assertKind(document, "inner", ShapeType.STRUCTURE, INNER.id());
        assertKind(document, "choice", ShapeType.UNION, CHOICE.id());
        assertKind(document.getMember("choice"), "inner", ShapeType.STRUCTURE, INNER.id());

        assertEquals(-7, document.getMember("byte").asByte());
        assertEquals(HUGE, document.getMember("bigInteger").asBigInteger());
        assertEquals(new BigDecimal("12.25"), document.getMember("bigDecimal").asBigDecimal());
        assertEquals("red", document.getMember("color").asString());
        assertEquals(10, document.getMember("size").asInteger());
        assertEquals(10, document.getMember("size").asNumber());
        assertEquals(WHEN, document.getMember("timestamp").asTimestamp());
        assertEquals(List.of("b", "a", "b"),
                     document.getMember("labels").asList().stream().map(Document::asString).toList());
        assertEquals(WHEN, document.getMember("whens").getMember("at").asTimestamp());
        assertEquals("nested", document.getMember("choice").getMember("inner").getMember("label").asString());

        Document untypedDocument = allKindsSource().getMember("document");
        assertTrue(Document.equals(untypedDocument, document.getMember("document")),
                   "a document member keeps the document value");
    }

    private static void assertKind(Document container, String member, ShapeType kind, ShapeId shapeId) {
        ModeledDocument value = assertInstanceOf(ModeledDocument.class, container.getMember(member), member);
        assertEquals(kind, value.type(), member);
        assertEquals(shapeId, value.discriminator(), member);
        assertEquals(shapeId, value.shapeId(), member);
    }

    @Test
    void blobsAreCopiedAndReadOnly() {
        byte[] bytes = {1, 2, 3};
        ModeledDocument blob = assertInstanceOf(ModeledDocument.class,
                                                SchemaDocuments.coerce(Document.of(bytes), PreludeSchemas.BLOB));
        bytes[0] = 9;
        ByteBuffer view = blob.asBlob();
        assertTrue(view.isReadOnly());
        byte[] read = new byte[view.remaining()];
        view.get(read);
        assertArrayEquals(new byte[] {1, 2, 3}, read);
        assertEquals(3, blob.asBlob().remaining(), "reading one view does not consume the document");
    }

    @Test
    void jsonRoundTripKeepsEveryValueThroughTheDocumentsProtocolContext() {
        ModeledDocument document = all();
        JsonCodec json = JsonCodec.builder().useTimestampFormat(true).useJsonName(true).build();

        String encoded = json.serializeToString(document);
        assertTrue(encoded.contains("\"blob\":\"AQID\""), encoded);
        assertTrue(encoded.contains("\"dateTime\":\"2026-03-04T05:06:07.25"), encoded);
        Document decoded = json.createDeserializer(encoded.getBytes(UTF_8)).readDocument();

        // The JSON document has JSON kinds (a base64 string, a date-time string, maps) and is not equal to the
        // modeled document, but coercing it through the schema restores exactly the same modeled value.
        assertNotEquals(document, decoded);
        assertEquals(ShapeType.STRING, decoded.getMember("blob").type());
        ModeledDocument restored = assertInstanceOf(ModeledDocument.class, SchemaDocuments.coerce(decoded, ALL));
        assertEquals(document, restored);
        assertEquals(document.hashCode(), restored.hashCode());
        assertTrue(SchemaDocuments.equivalent(ALL, decoded, allKindsSource()));
    }

    @Test
    void generatedStyleValuesRoundTripThroughTheirBuilders() {
        Widget widget = Widget.builder()
                              .widgetId("w-1")
                              .count(7)
                              .createdAt(Instant.ofEpochSecond(1_700_000_000L))
                              .payload(ByteBuffer.wrap(new byte[] {4, 5}))
                              .tags(List.of("z", "a"))
                              .attributes(Map.of("k", "v"))
                              .build();
        ModeledDocument document = SchemaDocuments.of(widget);
        SerializableStruct structure = assertInstanceOf(SerializableStruct.class, document);
        assertSame(document, SchemaDocuments.of(structure));
        assertEquals(Widget.ID, document.discriminator());
        assertEquals(List.of("z", "a"), document.getMember("tags").asList().stream().map(Document::asString).toList());

        Widget copy = SchemaDocuments.toShapeStrict(document, Widget.builder());
        assertEquals(widget.widgetId(), copy.widgetId());
        assertEquals(widget.count(), copy.count());
        assertEquals(widget.createdAt(), copy.createdAt());
        assertEquals(widget.payload(), copy.payload());
        assertEquals(widget.tags(), copy.tags());
        assertEquals(widget.attributes(), copy.attributes());
        assertEquals(document, SchemaDocuments.of(copy));

        // Smithy Java's own typed document of the value is converted to the same modeled document.
        assertEquals(document, SchemaDocuments.coerce(Document.of(widget), Widget.SCHEMA));
        assertTrue(SchemaDocuments.equivalent(Widget.SCHEMA, Document.of(widget), document));
    }

    @Test
    void integralTargetsAcceptExactValuesWithinRangeOnly() {
        assertEquals((byte) 127, coerce(Document.of(127), PreludeSchemas.BYTE).asByte());
        assertEquals((byte) -128, coerce(Document.of(-128L), PreludeSchemas.BYTE).asByte());
        assertFailure(Reason.OUT_OF_RANGE, ShapeType.INTEGER, PreludeSchemas.BYTE, () -> coerce(Document.of(128),
                                                                                                PreludeSchemas.BYTE));
        assertFailure(Reason.OUT_OF_RANGE, ShapeType.INTEGER, PreludeSchemas.BYTE,
                      () -> coerce(Document.of(-129), PreludeSchemas.BYTE));
        assertEquals(Short.MAX_VALUE, coerce(Document.of((int) Short.MAX_VALUE), PreludeSchemas.SHORT).asShort());
        assertFailure(Reason.OUT_OF_RANGE, ShapeType.INTEGER, PreludeSchemas.SHORT,
                      () -> coerce(Document.of(Short.MAX_VALUE + 1), PreludeSchemas.SHORT));
        assertEquals(Integer.MIN_VALUE, coerce(Document.of((long) Integer.MIN_VALUE), PreludeSchemas.INTEGER)
            .asInteger());
        assertFailure(Reason.OUT_OF_RANGE, ShapeType.LONG, PreludeSchemas.INTEGER,
                      () -> coerce(Document.of(Integer.MAX_VALUE + 1L), PreludeSchemas.INTEGER));
        BigInteger longMax = BigInteger.valueOf(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, coerce(Document.of(longMax), PreludeSchemas.LONG).asLong());
        assertFailure(Reason.OUT_OF_RANGE, ShapeType.BIG_INTEGER, PreludeSchemas.LONG,
                      () -> coerce(Document.of(longMax.add(BigInteger.ONE)), PreludeSchemas.LONG));
        assertEquals(3, coerce(Document.of(3.0d), PreludeSchemas.INTEGER).asInteger());
        assertEquals(100, coerce(Document.of(new BigDecimal("1E+2")), PreludeSchemas.INTEGER).asInteger());
        assertFailure(Reason.NOT_INTEGRAL, ShapeType.DOUBLE, PreludeSchemas.INTEGER,
                      () -> coerce(Document.of(2.5d), PreludeSchemas.INTEGER));
        assertFailure(Reason.NOT_INTEGRAL, ShapeType.BIG_DECIMAL, PreludeSchemas.LONG,
                      () -> coerce(Document.of(new BigDecimal("1.5")), PreludeSchemas.LONG));
        assertFailure(Reason.NOT_INTEGRAL, ShapeType.FLOAT, PreludeSchemas.INTEGER,
                      () -> coerce(Document.of(Float.NaN), PreludeSchemas.INTEGER));
        assertFailure(Reason.OUT_OF_RANGE, ShapeType.DOUBLE, PreludeSchemas.BIG_INTEGER,
                      () -> coerce(Document.of(Double.POSITIVE_INFINITY), PreludeSchemas.BIG_INTEGER));
        assertEquals(HUGE, coerce(Document.of(new BigDecimal(HUGE)), PreludeSchemas.BIG_INTEGER).asBigInteger());
        assertEquals(10, coerce(Document.of(10), SIZE).asInteger());
        assertFailure(Reason.OUT_OF_RANGE, ShapeType.LONG, SIZE, () -> coerce(Document.of(1L << 32), SIZE));
    }

    @Test
    void floatingAndDecimalTargetsRejectOnlyUnrepresentableMagnitudes() {
        assertEquals(Float.MAX_VALUE, coerce(Document.of((double) Float.MAX_VALUE), PreludeSchemas.FLOAT).asFloat());
        assertFailure(Reason.OUT_OF_RANGE, ShapeType.DOUBLE, PreludeSchemas.FLOAT,
                      () -> coerce(Document.of(1e39d), PreludeSchemas.FLOAT));
        assertEquals(Float.POSITIVE_INFINITY,
                     coerce(Document.of(Double.POSITIVE_INFINITY), PreludeSchemas.FLOAT).asFloat());
        assertTrue(Double.isNaN(coerce(Document.of(Float.NaN), PreludeSchemas.DOUBLE).asDouble()));
        assertEquals(0.1d, coerce(Document.of(new BigDecimal("0.1")), PreludeSchemas.DOUBLE).asDouble());
        assertFailure(Reason.OUT_OF_RANGE, ShapeType.BIG_DECIMAL, PreludeSchemas.DOUBLE,
                      () -> coerce(Document.of(new BigDecimal("1E+400")), PreludeSchemas.DOUBLE));
        assertFailure(Reason.OUT_OF_RANGE, ShapeType.BIG_INTEGER, PreludeSchemas.FLOAT,
                      () -> coerce(Document.of(BigInteger.TWO.pow(200)), PreludeSchemas.FLOAT));
        assertEquals(new BigDecimal("0.1"), coerce(Document.of(0.1f), PreludeSchemas.BIG_DECIMAL).asBigDecimal());
        assertEquals(new BigDecimal(Long.MIN_VALUE),
                     coerce(Document.of(Long.MIN_VALUE), PreludeSchemas.BIG_DECIMAL).asBigDecimal());
        assertFailure(Reason.OUT_OF_RANGE, ShapeType.DOUBLE, PreludeSchemas.BIG_DECIMAL,
                      () -> coerce(Document.of(Double.NaN), PreludeSchemas.BIG_DECIMAL));
    }

    @Test
    void nonFiniteFloatsAreReadFromTheirStringProtocolRepresentation() {
        // JSON writes non-finite floats and doubles as strings; found by DocumentRoundTripPropertyTest.
        JsonCodec json = JsonCodec.builder().build();
        Document nan = json.createDeserializer("\"NaN\"".getBytes(UTF_8)).readDocument();
        Document infinity = json.createDeserializer("\"Infinity\"".getBytes(UTF_8)).readDocument();
        Document negativeInfinity = json.createDeserializer("\"-Infinity\"".getBytes(UTF_8)).readDocument();
        assertTrue(Float.isNaN(coerce(nan, PreludeSchemas.FLOAT).asFloat()));
        assertTrue(Double.isNaN(coerce(nan, PreludeSchemas.DOUBLE).asDouble()));
        assertEquals(Float.POSITIVE_INFINITY, coerce(infinity, PreludeSchemas.FLOAT).asFloat());
        assertEquals(Double.NEGATIVE_INFINITY, coerce(negativeInfinity, PreludeSchemas.DOUBLE).asDouble());

        Document all = json.createDeserializer("{\"float\":\"-Infinity\",\"double\":\"NaN\"}".getBytes(UTF_8))
                           .readDocument();
        ModeledDocument restored = coerce(all, ALL);
        assertEquals(Float.NEGATIVE_INFINITY, restored.getMember("float").asFloat());
        assertTrue(Double.isNaN(restored.getMember("double").asDouble()));

        // Other strings are still not numbers, and only floats and doubles keep non-finite values.
        assertFailure(Reason.INCOMPATIBLE_KIND, ShapeType.STRING, PreludeSchemas.DOUBLE,
                      () -> coerce(Document.of("1.5"), PreludeSchemas.DOUBLE));
        assertFailure(Reason.INCOMPATIBLE_KIND, ShapeType.STRING, PreludeSchemas.FLOAT,
                      () -> coerce(Document.of("nan"), PreludeSchemas.FLOAT));
        assertFailure(Reason.INCOMPATIBLE_KIND, ShapeType.STRING, PreludeSchemas.BIG_DECIMAL,
                      () -> coerce(nan, PreludeSchemas.BIG_DECIMAL));
    }

    @Test
    void blobsAndTimestampsUseTheDocumentsProtocolRepresentationOnly() {
        JsonCodec json = JsonCodec.builder().build();
        Document base64 = json.createDeserializer("\"AQID\"".getBytes(UTF_8)).readDocument();
        ByteBuffer decoded = coerce(base64, PreludeSchemas.BLOB).asBlob();
        assertEquals(ByteBuffer.wrap(new byte[] {1, 2, 3}), decoded);
        Document epochSeconds = json.createDeserializer("1.5".getBytes(UTF_8)).readDocument();
        assertEquals(Instant.ofEpochSecond(1, 500_000_000),
                     coerce(epochSeconds, PreludeSchemas.TIMESTAMP).asTimestamp());

        Document badBase64 = json.createDeserializer("\"not base64!\"".getBytes(UTF_8)).readDocument();
        assertFailure(Reason.INVALID_REPRESENTATION, ShapeType.STRING, PreludeSchemas.BLOB,
                      () -> coerce(badBase64, PreludeSchemas.BLOB));
        // A protocol-neutral string has no blob or timestamp representation.
        assertFailure(Reason.INVALID_REPRESENTATION, ShapeType.STRING, PreludeSchemas.BLOB,
                      () -> coerce(Document.of("AQID"), PreludeSchemas.BLOB));
        assertFailure(Reason.INVALID_REPRESENTATION, ShapeType.LONG, PreludeSchemas.TIMESTAMP,
                      () -> coerce(Document.of(1L), PreludeSchemas.TIMESTAMP));
        assertFailure(Reason.INCOMPATIBLE_KIND, ShapeType.BOOLEAN, PreludeSchemas.TIMESTAMP,
                      () -> coerce(Document.of(true), PreludeSchemas.TIMESTAMP));
    }

    @Test
    void incompatibleKindsFailWithTheSourceKindRequestedShapeAndPath() {
        assertFailure(Reason.INCOMPATIBLE_KIND, ShapeType.STRING, PreludeSchemas.INTEGER,
                      () -> coerce(Document.of("1"), PreludeSchemas.INTEGER));
        assertFailure(Reason.INCOMPATIBLE_KIND, ShapeType.INTEGER, PreludeSchemas.STRING,
                      () -> coerce(Document.of(1), PreludeSchemas.STRING));
        assertFailure(Reason.INCOMPATIBLE_KIND, ShapeType.STRING, PreludeSchemas.BOOLEAN,
                      () -> coerce(Document.of("true"), PreludeSchemas.BOOLEAN));
        assertFailure(Reason.INCOMPATIBLE_KIND, ShapeType.LIST, WHENS,
                      () -> coerce(Document.of(List.of()), WHENS));
        assertFailure(Reason.INCOMPATIBLE_KIND, ShapeType.MAP, LABELS,
                      () -> coerce(Document.EMPTY_MAP, LABELS));
        assertFailure(Reason.INCOMPATIBLE_KIND, ShapeType.STRING, INNER,
                      () -> coerce(Document.of("x"), INNER));

        Document nested = Document.of(Map.of(
            "labels", Document.of(List.of(Document.of("ok"), Document.of(5))),
            "whens", Document.of(Map.of("at", Document.of("soon"))),
            "inner", Document.of(Map.of("count", Document.of(Long.MAX_VALUE)))));
        DocumentCoercionException labels = assertThrows(
            DocumentCoercionException.class, () -> coerce(Document.of(Map.of("labels", nested.getMember("labels"))),
                                                          ALL));
        assertEquals("$.labels[1]", labels.path());
        assertEquals(PreludeSchemas.STRING.id(), labels.requestedShape());
        DocumentCoercionException whens = assertThrows(
            DocumentCoercionException.class, () -> coerce(Document.of(Map.of("whens", nested.getMember("whens"))),
                                                          ALL));
        assertEquals("$.whens[\"at\"]", whens.path());
        assertEquals(Reason.INVALID_REPRESENTATION, whens.reason());
        DocumentCoercionException inner = assertThrows(
            DocumentCoercionException.class, () -> coerce(Document.of(Map.of("inner", nested.getMember("inner"))),
                                                          ALL));
        assertEquals("$.inner.count", inner.path());
        assertEquals(Reason.OUT_OF_RANGE, inner.reason());
        assertEquals("Cannot coerce long document at $.inner.count to smithy.api#Integer (integer): numeric value "
                     + "out of range", inner.getMessage());
    }

    @Test
    void discriminatorsMustMatchTheRequestedSchema() {
        Widget widget = Widget.builder().widgetId("w").build();
        DocumentCoercionException mismatch = assertThrows(
            DocumentCoercionException.class, () -> SchemaDocuments.toShape(SchemaDocuments.of(widget),
                                                                           shapeBuilderOf(INNER)));
        assertEquals(Reason.DISCRIMINATOR_MISMATCH, mismatch.reason());
        assertEquals(Widget.ID, mismatch.discriminator());
        assertEquals(INNER.id(), mismatch.requestedShape());
        assertTrue(mismatch.getMessage().contains(Widget.ID.toString()), mismatch.getMessage());
        assertTrue(mismatch.getMessage().contains(INNER.id().toString()), mismatch.getMessage());

        // Smithy Java's typed document of another shape, and a JSON __type discriminator, are checked too.
        assertEquals(Reason.DISCRIMINATOR_MISMATCH,
                     assertThrows(DocumentCoercionException.class,
                                  () -> coerce(Document.of(widget), INNER)).reason());
        JsonCodec json = JsonCodec.builder().build();
        Document typed = json.createDeserializer(("{\"__type\":\"" + INNER.id() + "\",\"label\":\"x\"}")
                                                     .getBytes(UTF_8)).readDocument();
        assertEquals("x", coerce(typed, INNER).getMember("label").asString());
        assertEquals(ShapeType.UNION, coerce(Document.of(Map.of("inner", typed)), CHOICE).type());
        Document wrong = json.createDeserializer(("{\"__type\":\"" + Widget.ID + "\"}").getBytes(UTF_8))
                             .readDocument();
        DocumentCoercionException nested = assertThrows(DocumentCoercionException.class,
                                                        () -> coerce(Document.of(Map.of("inner", wrong)), CHOICE));
        assertEquals(Reason.DISCRIMINATOR_MISMATCH, nested.reason());
        assertEquals("$.inner", nested.path());
        assertEquals(Widget.ID, nested.discriminator());
        assertEquals(INNER.id(), nested.requestedShape());
        Document invalid = json.createDeserializer("{\"__type\":\"not a shape id!\"}".getBytes(UTF_8))
                               .readDocument();
        assertEquals(Reason.INVALID_DISCRIMINATOR,
                     assertThrows(DocumentCoercionException.class, () -> coerce(invalid, INNER)).reason());
        // An absent discriminator is fine when the schema is requested explicitly.
        assertEquals("y", coerce(Document.of(Map.of("label", Document.of("y"))), INNER).getMember("label")
                                                                                        .asString());
    }

    @Test
    void unionDocumentsSetAtMostOneMember() {
        Document two = Document.of(Map.of("text", Document.of("a"), "inner", Document.EMPTY_MAP));
        DocumentCoercionException error = assertThrows(DocumentCoercionException.class, () -> coerce(two, CHOICE));
        assertEquals(Reason.UNION_MEMBER_COUNT, error.reason());
        assertEquals(CHOICE.id(), error.requestedShape());
        assertEquals(ShapeType.UNION, coerce(Document.of(Map.of("text", Document.of("a"))), CHOICE).type());
    }

    @Test
    void failuresAreDeterministic() {
        Document source = Document.of(Map.of("integer", Document.of(2.5d), "long", Document.of("x")));
        DocumentCoercionException first = assertThrows(DocumentCoercionException.class, () -> coerce(source, ALL));
        DocumentCoercionException second = assertThrows(DocumentCoercionException.class, () -> coerce(source, ALL));
        assertEquals(first.getMessage(), second.getMessage());
        assertEquals(first.reason(), second.reason());
        assertEquals(first.path(), second.path());
        assertEquals(first.sourceKind(), second.sourceKind());
    }

    @Test
    void modeledEqualityIsSymmetricValueEquality() {
        ModeledDocument left = all();
        ModeledDocument right = all();
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        Document smithy = allKindsSource();
        assertFalse(left.equals(smithy));
        assertFalse(smithy.equals(left));
        assertNotEquals(coerce(Document.of("red"), COLOR), coerce(Document.of("red"), PreludeSchemas.STRING),
                        "the shape ID is part of a modeled value");
        assertEquals(coerce(Document.of(new BigDecimal("1.50")), PreludeSchemas.BIG_DECIMAL),
                     coerce(Document.of(new BigDecimal("1.5")), PreludeSchemas.BIG_DECIMAL));
        assertEquals(coerce(Document.of(new BigDecimal("1.50")), PreludeSchemas.BIG_DECIMAL).hashCode(),
                     coerce(Document.of(new BigDecimal("1.5")), PreludeSchemas.BIG_DECIMAL).hashCode());
        assertNotEquals(coerce(Document.of(List.of(Document.of("a"), Document.of("b"))), LABELS),
                        coerce(Document.of(List.of(Document.of("b"), Document.of("a"))), LABELS),
                        "list order is significant");
    }

    @Test
    void sensitiveValuesAreRedactedFromToString() {
        String rendered = all().toString();
        assertFalse(rendered.contains("hunter2"), rendered);
        assertTrue(rendered.contains("*** Sensitive Data Redacted ***"), rendered);
        assertTrue(rendered.contains("text"), rendered);
    }

    @Test
    void sparseNullsAndDocumentValuesArePreserved() {
        Schema sparse = Schema.listBuilder(id("Sparse")).putMember("member", PreludeSchemas.STRING).build();
        List<Document> values = new ArrayList<>();
        values.add(Document.of("a"));
        values.add(null);
        ModeledDocument list = coerce(Document.of(values), sparse);
        assertEquals(2, list.size());
        assertNull(list.asList().get(1));
        Document free = Document.of(Map.of("any", Document.of(1)));
        assertSame(free, SchemaDocuments.coerce(free, PreludeSchemas.DOCUMENT));
    }

    private static ModeledDocument coerce(Document document, Schema schema) {
        return assertInstanceOf(ModeledDocument.class, SchemaDocuments.coerce(document, schema));
    }

    private static void assertFailure(Reason reason, ShapeType sourceKind, Schema requested, Executable executable) {
        DocumentCoercionException error = assertThrows(DocumentCoercionException.class, executable);
        assertEquals(reason, error.reason(), error.getMessage());
        assertEquals(sourceKind, error.sourceKind(), error.getMessage());
        assertEquals(requested.id(), error.requestedShape(), error.getMessage());
        assertEquals("$", error.path(), error.getMessage());
    }

    /** A builder of the modeled document of {@code schema}, used to request a schema explicitly. */
    private static ShapeBuilder<ModeledDocument> shapeBuilderOf(Schema schema) {
        return new ShapeBuilder<>() {
            private Document source;

            @Override
            public ModeledDocument build() {
                return (ModeledDocument) SchemaDocuments.coerce(source, schema);
            }

            @Override
            public ShapeBuilder<ModeledDocument> deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(schema, this, (state, member, value) -> { });
                this.source = decoder.readDocument();
                return this;
            }

            @Override
            public Schema schema() {
                return schema;
            }
        };
    }
}
