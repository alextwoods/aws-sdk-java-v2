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

package software.amazon.awssdk.dynamicclient.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.io.DataStream;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * The dynamic client's input coercion: {@link StructDocument#convertDocument} with the SDK's stricter rules (design
 * C.3, Requirements 28.3 to 28.6, 28.10 and 28.11).
 *
 * <ul>
 *     <li>A name that is not a member of its structure fails, instead of being dropped.</li>
 *     <li>A value whose document type does not fit the member fails with a message naming the member, before any
 *     {@code as<Type>} call: a non-map for a structure, union or map, a non-list for a list, a non-number for a
 *     number, and so on.</li>
 *     <li>A byte, short, integer, intEnum, long or bigInteger member needs an integral number in its range; it is not
 *     narrowed with the Java primitive conversions.</li>
 *     <li>A timestamp member takes a timestamp, a number of epoch seconds (the fraction is kept down to
 *     milliseconds), or a {@code date-time} string.</li>
 *     <li>A blob member takes only a blob document: a string is rejected, whatever produced the document.</li>
 *     <li>A streaming blob member takes only a data stream document, and an event stream member only an event stream
 *     document; neither stream is read.</li>
 *     <li>A union with more than one member set fails.</li>
 * </ul>
 *
 * <p>Error messages name the member (its ShapeId) and never include the value.
 */
@SdkInternalApi
final class StrictDocumentCoercion {
    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigDecimal MILLIS_PER_SECOND = BigDecimal.valueOf(1000);

    private StrictDocumentCoercion() {
    }

    /**
     * Coerces a document to a structure document.
     *
     * @param schema the structure schema
     * @param value the map or structure document
     * @param service the service ShapeId, passed on as {@link StructDocument#of(Schema, Document, ShapeId)} does
     * @return the structure document
     * @throws SerializationException if the value cannot be coerced
     */
    static StructDocument coerceStructure(Schema schema, Document value, ShapeId service) {
        if (!isMapLike(value.type())) {
            throw new SerializationException("Cannot coerce a document to structure " + schema.id() + ": expected a "
                                             + "map or structure document, but found " + withArticle(value.type())
                                             + " document");
        }
        return structure(schema, value, service);
    }

    /**
     * Returns the document of a streaming member, a streaming blob or an event stream: the value's data stream or
     * events with the member's schema. The streams are not read.
     *
     * @param member a member with the {@code smithy.api#streaming} trait, which targets a blob or a union
     * @param value the member's value
     * @return the stream document
     * @throws SerializationException if the value is not a data stream document (for a blob) or an event stream
     *     document (for a union)
     */
    static Document streamMember(Schema member, Document value) {
        if (member.type() == ShapeType.BLOB) {
            if (!isDataStream(value)) {
                throw new SerializationException(
                    cannotCoerce(member) + "a streaming blob member needs a data stream document, but found "
                    + withArticle(value.type()) + " document. Create one with DynamicStreams.dataStream(RequestBody) "
                    + "or DynamicStreams.dataStream(AsyncRequestBody).");
            }
            return Document.of(member, value.asDataStream());
        }
        if (!isEventStream(value)) {
            throw new SerializationException(
                cannotCoerce(member) + "an event stream member needs an event stream document, but found "
                + withArticle(value.type()) + " document. Create one with DynamicStreams.eventStream(Publisher).");
        }
        return Document.ofEventStream(member, value.asEventStream());
    }

    /**
     * Returns whether a document is a data stream document ({@code Document.of(Schema, DataStream)}), whose
     * {@code asObject()} is its stream.
     */
    static boolean isDataStream(Document value) {
        return value.type() == ShapeType.BLOB && value.asObject() instanceof DataStream;
    }

    /**
     * Returns whether a document is an event stream document ({@code Document.ofEventStream}), whose
     * {@code asObject()} is its publisher.
     */
    static boolean isEventStream(Document value) {
        return value.type() == ShapeType.UNION && value.asObject() instanceof Publisher;
    }

    private static Document coerce(Schema member, Document value, ShapeId service) {
        ShapeType type = value.type();
        if (member.hasTrait(TraitKey.STREAMING_TRAIT)) {
            return streamMember(member, value);
        }
        switch (member.type()) {
            case STRUCTURE:
                expect(member, isMapLike(type), "a map or structure", value);
                return structure(member, value, service);
            case UNION:
                expect(member, isMapLike(type), "a map or structure", value);
                return union(member, value, service);
            case LIST:
                expect(member, type == ShapeType.LIST, "a list", value);
                return list(member, value, service);
            case MAP:
                expect(member, isMapLike(type), "a map or structure", value);
                return map(member, value, service);
            case BOOLEAN:
                expect(member, type == ShapeType.BOOLEAN, "a boolean", value);
                return new ContentDocument(member, value.asBoolean());
            case STRING:
            case ENUM:
                expect(member, type == ShapeType.STRING || type == ShapeType.ENUM, "a string", value);
                return new ContentDocument(member, value.asString());
            case TIMESTAMP:
                return new ContentDocument(member, timestamp(member, value));
            case BYTE:
                return new ContentDocument(member, (byte) integral(member, value, Byte.MIN_VALUE, Byte.MAX_VALUE));
            case SHORT:
                return new ContentDocument(member, (short) integral(member, value, Short.MIN_VALUE, Short.MAX_VALUE));
            case INTEGER:
            case INT_ENUM:
                return new ContentDocument(member, (int) integral(member, value, Integer.MIN_VALUE, Integer.MAX_VALUE));
            case LONG:
                return new ContentDocument(member, integral(member, value, Long.MIN_VALUE, Long.MAX_VALUE));
            case BIG_INTEGER:
                return new ContentDocument(member, bigInteger(member, value));
            case FLOAT:
                expectNumber(member, value);
                return new ContentDocument(member, value.asFloat());
            case DOUBLE:
                expectNumber(member, value);
                return new ContentDocument(member, value.asDouble());
            case BIG_DECIMAL:
                return new ContentDocument(member, bigDecimal(member, value));
            case BLOB:
                if (type != ShapeType.BLOB) {
                    throw new SerializationException(
                        cannotCoerce(member) + "a blob member needs a blob document, but found " + withArticle(type)
                        + " document. Pass the bytes with Document.of(byte[]) or Document.of(ByteBuffer).");
                }
                return new ContentDocument(member, value.asBlob());
            case DOCUMENT:
                return new ContentDocument.SchemaDocument(member, value);
            default:
                throw new IllegalArgumentException("Unsupported schema type: " + member);
        }
    }

    private static StructDocument structure(Schema schema, Document value, ShapeId service) {
        Document[] values = new Document[schema.members().size()];
        for (Map.Entry<String, Document> entry : value.asMap().entrySet()) {
            Schema member = memberOf(schema, entry.getKey());
            Document memberValue = entry.getValue();
            if (memberValue != null) {
                values[member.memberIndex()] = coerce(member, memberValue, service);
            }
        }
        return new StructDocument(schema, values);
    }

    private static StructDocument union(Schema schema, Document value, ShapeId service) {
        Schema setMember = null;
        Document setValue = null;
        for (Map.Entry<String, Document> entry : value.asMap().entrySet()) {
            Schema member = memberOf(schema, entry.getKey());
            Document memberValue = entry.getValue();
            if (memberValue == null) {
                continue;
            }
            if (setMember != null) {
                throw new SerializationException(cannotCoerce(schema) + "union " + targetId(schema) + " can have one "
                                                 + "member set, but " + setMember.memberName() + " and "
                                                 + member.memberName() + " are both set");
            }
            setMember = member;
            setValue = coerce(member, memberValue, service);
        }
        if (setMember == null) {
            return new StructDocument(schema, null, null);
        }
        return new StructDocument(schema, unwrapScalar(setValue), setMember);
    }

    private static Object unwrapScalar(Document value) {
        if (value instanceof ContentDocument && ((ContentDocument) value).isScalar()) {
            return ((ContentDocument) value).rawValue();
        }
        return value;
    }

    private static Document list(Schema schema, Document value, ShapeId service) {
        Schema element = schema.listMember();
        List<Document> result = new ArrayList<>(value.size());
        for (Document item : value.asList()) {
            result.add(item == null ? null : coerce(element, item, service));
        }
        return new ContentDocument(schema, result);
    }

    private static Document map(Schema schema, Document value, ShapeId service) {
        Schema valueMember = schema.mapValueMember();
        Map<String, Document> result = new LinkedHashMap<>();
        for (Map.Entry<String, Document> entry : value.asMap().entrySet()) {
            Document item = entry.getValue();
            result.put(entry.getKey(), item == null ? null : coerce(valueMember, item, service));
        }
        return new ContentDocument(schema, result);
    }

    /**
     * Returns the member of a structure or union, or fails naming the member and the structure (Requirement 28.10).
     */
    private static Schema memberOf(Schema schema, String name) {
        Schema member = schema.member(name);
        if (member == null) {
            ShapeId structure = targetId(schema);
            List<String> names = new ArrayList<>(schema.members().size());
            for (Schema m : schema.members()) {
                names.add(m.memberName());
            }
            throw new SerializationException("Structure " + structure + " has no member named \"" + name + "\". Its "
                                             + "members are " + names + ". Members are keyed by Smithy member name, "
                                             + "not by a protocol name such as jsonName.");
        }
        return member;
    }

    private static ShapeId targetId(Schema schema) {
        return schema.isMember() ? schema.memberTarget().id() : schema.id();
    }

    /**
     * Reads a timestamp from a timestamp document, a number of epoch seconds (Requirement 28.4) or a {@code date-time}
     * string (Requirement 28.5).
     */
    private static Instant timestamp(Schema member, Document value) {
        ShapeType type = value.type();
        if (type == ShapeType.TIMESTAMP) {
            return value.asTimestamp();
        }
        if (type == ShapeType.STRING) {
            try {
                return TimestampFormatter.Prelude.DATE_TIME.readFromString(value.asString(), false);
            } catch (DateTimeParseException e) {
                throw new SerializationException(cannotCoerce(member) + "the string is not a valid date-time "
                                                 + "(RFC 3339) timestamp, such as 1985-04-12T23:20:50.52Z", e);
            }
        }
        expect(member, isNumber(type), "a timestamp, number or string", value);
        Number number = value.asNumber();
        if (isFixedWidthInteger(number)) {
            return epochSeconds(member, BigDecimal.valueOf(number.longValue()));
        }
        BigDecimal seconds = toBigDecimal(number);
        if (seconds == null) {
            throw outOfRange(member, "a timestamp");
        }
        return epochSeconds(member, seconds);
    }

    /**
     * Epoch seconds with the fraction kept down to milliseconds; digits below a millisecond are dropped.
     */
    private static Instant epochSeconds(Schema member, BigDecimal seconds) {
        BigInteger millis = seconds.multiply(MILLIS_PER_SECOND).setScale(0, RoundingMode.DOWN).toBigInteger();
        if (millis.compareTo(LONG_MIN) < 0 || millis.compareTo(LONG_MAX) > 0) {
            throw outOfRange(member, "a timestamp");
        }
        return Instant.ofEpochMilli(millis.longValue());
    }

    /**
     * An integral number within [min, max], without narrowing (Requirement 28.11).
     */
    private static long integral(Schema member, Document value, long min, long max) {
        expectNumber(member, value);
        Number number = value.asNumber();
        if (isFixedWidthInteger(number)) {
            long result = number.longValue();
            if (result < min || result > max) {
                throw outOfRange(member, withArticle(member.type()) + " member");
            }
            return result;
        }
        BigInteger result = integralValue(member, number);
        if (result.compareTo(BigInteger.valueOf(min)) < 0 || result.compareTo(BigInteger.valueOf(max)) > 0) {
            throw outOfRange(member, withArticle(member.type()) + " member");
        }
        return result.longValue();
    }

    private static BigInteger bigInteger(Schema member, Document value) {
        expectNumber(member, value);
        Number number = value.asNumber();
        if (isFixedWidthInteger(number)) {
            return BigInteger.valueOf(number.longValue());
        }
        return integralValue(member, number);
    }

    private static BigInteger integralValue(Schema member, Number number) {
        if (number instanceof BigInteger) {
            return (BigInteger) number;
        }
        BigDecimal decimal = toBigDecimal(number);
        if (decimal == null || decimal.signum() != 0 && decimal.stripTrailingZeros().scale() > 0) {
            throw new SerializationException(cannotCoerce(member) + withArticle(member.type()) + " member needs an "
                                             + "integral number, but the value is not one");
        }
        return decimal.toBigInteger();
    }

    private static BigDecimal bigDecimal(Schema member, Document value) {
        expectNumber(member, value);
        Number number = value.asNumber();
        if (isFixedWidthInteger(number)) {
            return BigDecimal.valueOf(number.longValue());
        }
        BigDecimal result = toBigDecimal(number);
        if (result == null) {
            throw new SerializationException(cannotCoerce(member) + "a bigDecimal member needs a finite number");
        }
        return result;
    }

    /**
     * The exact decimal value of a number, or null for NaN and infinities. A float or double is read through its
     * shortest decimal form ({@link Double#toString}), so 1.001 is 1.001, not 1.00099999999999988987.
     */
    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }
        if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        }
        if (number instanceof Float) {
            float f = number.floatValue();
            return Float.isNaN(f) || Float.isInfinite(f) ? null : new BigDecimal(Float.toString(f));
        }
        if (number instanceof Double) {
            double d = number.doubleValue();
            return Double.isNaN(d) || Double.isInfinite(d) ? null : BigDecimal.valueOf(d);
        }
        if (isFixedWidthInteger(number)) {
            return BigDecimal.valueOf(number.longValue());
        }
        // Another Number class: its decimal string form, as BigDecimal reads it.
        double asDouble = number.doubleValue();
        return Double.isNaN(asDouble) || Double.isInfinite(asDouble) ? null : new BigDecimal(number.toString());
    }

    private static boolean isFixedWidthInteger(Number number) {
        return number instanceof Integer || number instanceof Long || number instanceof Short
               || number instanceof Byte;
    }

    private static boolean isMapLike(ShapeType type) {
        return type == ShapeType.MAP || type == ShapeType.STRUCTURE || type == ShapeType.UNION;
    }

    private static boolean isNumber(ShapeType type) {
        switch (type) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case INT_ENUM:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_INTEGER:
            case BIG_DECIMAL:
                return true;
            default:
                return false;
        }
    }

    private static void expectNumber(Schema member, Document value) {
        expect(member, isNumber(value.type()), "a number", value);
    }

    private static void expect(Schema member, boolean matches, String expected, Document value) {
        if (!matches) {
            throw new SerializationException(cannotCoerce(member) + "expected " + expected + " document, but found "
                                             + withArticle(value.type()) + " document");
        }
    }

    private static SerializationException outOfRange(Schema member, String target) {
        return new SerializationException(cannotCoerce(member) + "the value is out of range for " + target);
    }

    private static String cannotCoerce(Schema member) {
        return "Cannot coerce member " + member.id() + ": ";
    }

    /**
     * The type name with its indefinite article: "an integer", "a string".
     */
    private static String withArticle(ShapeType type) {
        String name = type.toString();
        return ("aeiou".indexOf(name.charAt(0)) >= 0 ? "an " : "a ") + name;
    }
}
