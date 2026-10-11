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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * A Smithy document: untyped data from the Smithy data model.
 *
 * <p>This is the SDK port of the core of smithy-java {@code software.amazon.smithy.java.core.serde.document.Document}.
 * It covers the value types of the Smithy data model (booleans, the eight numeric types, strings, blobs, timestamps,
 * lists and string-keyed maps) and the operations on them: {@link #type()}, the {@code as<Type>} coercions,
 * {@link #getMember(String)}, {@link #getMemberNames()}, {@link #size()} and value equality through
 * {@link #equals(Object, Object)}.
 *
 * <p>A document is a {@link SerializableShape}: {@link #serialize(ShapeSerializer)} always writes it as one
 * {@link ShapeSerializer#writeDocument} call. Schema-backed documents, structure documents, discriminators,
 * {@code Document.of(SerializableShape)}, {@code asShape} and {@code serializeContents} are added on top of this
 * interface by the full document port (design B.1). Those additions are default methods and new factories, so they do
 * not change the behavior of the methods defined here.
 *
 * <h2>Null values</h2>
 * <p>A Smithy {@code null} is represented by a Java {@code null} where a document is expected, for example as a list
 * element or map value.
 *
 * <h2>Numeric coercion</h2>
 * <p>The numeric {@code as<Type>} methods accept any numeric document and convert its value with the Java widening
 * (JLS 5.1.2) and narrowing (JLS 5.1.3) primitive conversions, without throwing on overflow or loss of precision.
 *
 * <h2>Equality</h2>
 * <p>The implementations returned by the factories on this interface implement {@code equals} with
 * {@link #equals(Object, Object)}: two documents are equal when they have the same {@link #type()} and equal values,
 * ignoring the map entry order. Floating-point values are compared like {@link Double#compare(double, double)}, so
 * {@code NaN} equals {@code NaN} and {@code 0.0} does not equal {@code -0.0}. Big decimals are compared ignoring
 * trailing zeros.
 *
 * <p>Implementations are immutable as long as the lists, maps and buffers passed to the factories are not modified
 * after the document is created. The factories do not copy them.
 */
@SdkPublicApi
public interface Document extends SerializableShape {

    /**
     * An empty map document.
     */
    Document EMPTY_MAP = of(Collections.<String, Document>emptyMap());

    /**
     * Writes the document with exactly one call to {@link ShapeSerializer#writeDocument(Schema, Document)}, passing
     * {@link PreludeSchemas#DOCUMENT} and this document. Serializers read the document's contents from the document
     * itself.
     *
     * @param serializer where to serialize the document
     */
    @Override
    default void serialize(ShapeSerializer serializer) {
        serializer.writeDocument(PreludeSchemas.DOCUMENT, this);
    }

    /**
     * Returns the Smithy data model type of the document's contents. The type tells callers which {@code as<Type>}
     * method returns the document's value.
     *
     * @return the Smithy data model type
     */
    ShapeType type();

    /**
     * Returns whether the document is of the given type.
     *
     * @param type the type to check against
     * @return true if {@link #type()} equals {@code type}
     */
    default boolean isType(ShapeType type) {
        return type == type();
    }

    /**
     * Returns the boolean value of a boolean document.
     *
     * @return the boolean value
     * @throws SerializationException if the document is not a boolean
     */
    default boolean asBoolean() {
        throw Documents.typeMismatch("boolean", this);
    }

    /**
     * Returns the value of a numeric document as a byte, converted with JLS 5.1.3 narrowing when necessary.
     *
     * @return the byte value
     * @throws SerializationException if the document is not numeric
     */
    default byte asByte() {
        throw Documents.typeMismatch("byte", this);
    }

    /**
     * Returns the value of a numeric document as a short, converted with JLS 5.1.2/5.1.3 rules when necessary.
     *
     * @return the short value
     * @throws SerializationException if the document is not numeric
     */
    default short asShort() {
        throw Documents.typeMismatch("short", this);
    }

    /**
     * Returns the value of a numeric document as an integer, converted with JLS 5.1.2/5.1.3 rules when necessary.
     *
     * @return the integer value
     * @throws SerializationException if the document is not numeric
     */
    default int asInteger() {
        throw Documents.typeMismatch("integer", this);
    }

    /**
     * Returns the value of a numeric document as a long, converted with JLS 5.1.2/5.1.3 rules when necessary.
     *
     * @return the long value
     * @throws SerializationException if the document is not numeric
     */
    default long asLong() {
        throw Documents.typeMismatch("long", this);
    }

    /**
     * Returns the value of a numeric document as a float, converted with JLS 5.1.2/5.1.3 rules when necessary.
     *
     * @return the float value
     * @throws SerializationException if the document is not numeric
     */
    default float asFloat() {
        throw Documents.typeMismatch("float", this);
    }

    /**
     * Returns the value of a numeric document as a double, converted with JLS 5.1.2/5.1.3 rules when necessary.
     *
     * @return the double value
     * @throws SerializationException if the document is not numeric
     */
    default double asDouble() {
        throw Documents.typeMismatch("double", this);
    }

    /**
     * Returns the value of a numeric document as a big integer. Fractional parts are discarded; a float or double is
     * first converted to a long with JLS 5.1.3 rules.
     *
     * @return the big integer value
     * @throws SerializationException if the document is not numeric
     */
    default BigInteger asBigInteger() {
        throw Documents.typeMismatch("bigInteger", this);
    }

    /**
     * Returns the value of a numeric document as a big decimal.
     *
     * @return the big decimal value
     * @throws SerializationException if the document is not numeric, or is a non-finite float or double
     */
    default BigDecimal asBigDecimal() {
        throw Documents.typeMismatch("bigDecimal", this);
    }

    /**
     * Returns the value of a numeric document as a boxed number whose class matches {@link #type()}: {@link Byte},
     * {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double}, {@link BigInteger} or
     * {@link BigDecimal}.
     *
     * @return the number
     * @throws SerializationException if the document is not numeric
     */
    default Number asNumber() {
        switch (type()) {
            case BYTE:
                return asByte();
            case SHORT:
                return asShort();
            case INTEGER:
            case INT_ENUM:
                return asInteger();
            case LONG:
                return asLong();
            case FLOAT:
                return asFloat();
            case DOUBLE:
                return asDouble();
            case BIG_INTEGER:
                return asBigInteger();
            case BIG_DECIMAL:
                return asBigDecimal();
            default:
                throw Documents.typeMismatch("number", this);
        }
    }

    /**
     * Returns the value of a string document.
     *
     * @return the string value
     * @throws SerializationException if the document is not a string
     */
    default String asString() {
        throw Documents.typeMismatch("string", this);
    }

    /**
     * Returns the bytes of a blob document. The returned buffer shares its content with the document but has its own
     * position and limit.
     *
     * @return the blob bytes
     * @throws SerializationException if the document is not a blob
     */
    default ByteBuffer asBlob() {
        throw Documents.typeMismatch("blob", this);
    }

    /**
     * Returns the value of a timestamp document.
     *
     * @return the timestamp value
     * @throws SerializationException if the document is not a timestamp
     */
    default Instant asTimestamp() {
        throw Documents.typeMismatch("timestamp", this);
    }

    /**
     * Returns the elements of a list document as an unmodifiable list. Elements may be null.
     *
     * @return the list elements
     * @throws SerializationException if the document is not a list
     */
    default List<Document> asList() {
        throw Documents.typeMismatch("list", this);
    }

    /**
     * Returns the entries of a map document as an unmodifiable map, in the order the entries were added. Values may be
     * null.
     *
     * <p>This is smithy-java's {@code asStringMap()}, named {@code asMap()} as in the Document SEP.
     *
     * @return the map entries
     * @throws SerializationException if the document is not a map
     */
    default Map<String, Document> asMap() {
        throw Documents.typeMismatch("map", this);
    }

    /**
     * Converts the document to standard library types: {@link Boolean}, the boxed number type that matches
     * {@link #type()}, {@link String}, {@link ByteBuffer}, {@link Instant}, a {@link List} of converted elements or
     * a {@link Map} of converted values. Null list elements and map values stay null.
     *
     * @return the converted value
     * @throws UnsupportedOperationException if the document's type has no standard library form
     */
    default Object asObject() {
        switch (type()) {
            case BLOB:
                return asBlob();
            case BOOLEAN:
                return asBoolean();
            case STRING:
            case ENUM:
                return asString();
            case TIMESTAMP:
                return asTimestamp();
            case BYTE:
            case SHORT:
            case INTEGER:
            case INT_ENUM:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_INTEGER:
            case BIG_DECIMAL:
                return asNumber();
            case LIST: {
                List<Document> elements = asList();
                List<Object> result = new ArrayList<>(elements.size());
                for (Document element : elements) {
                    result.add(element == null ? null : element.asObject());
                }
                return result;
            }
            case MAP:
            case STRUCTURE:
            case UNION: {
                Map<String, Document> entries = asMap();
                Map<String, Object> result = new LinkedHashMap<>(entries.size() * 4 / 3 + 1);
                for (Map.Entry<String, Document> entry : entries.entrySet()) {
                    Document value = entry.getValue();
                    result.put(entry.getKey(), value == null ? null : value.asObject());
                }
                return result;
            }
            default:
                throw new UnsupportedOperationException("Unable to convert a " + type() + " document to an object");
        }
    }

    /**
     * Returns the number of elements of a list document or the number of entries of a map document.
     *
     * @return the size, or -1 for any other document
     */
    default int size() {
        return -1;
    }

    /**
     * Returns a map member by name.
     *
     * @param memberName the member name, which for a map document is the entry key
     * @return the member, or null if the document has no such member or the member's value is null
     * @throws SerializationException if the document is not a map
     */
    default Document getMember(String memberName) {
        throw Documents.typeMismatch("map, structure, or union", this);
    }

    /**
     * Returns the member names of a map document as an unmodifiable set, in the order the entries were added.
     *
     * @return the member names
     * @throws SerializationException if the document is not a map
     */
    default Set<String> getMemberNames() {
        throw Documents.typeMismatch("map, structure, or union", this);
    }

    /**
     * Creates a boolean document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(boolean value) {
        return value ? Documents.BooleanDocument.TRUE : Documents.BooleanDocument.FALSE;
    }

    /**
     * Creates a byte document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(byte value) {
        return new Documents.IntegralDocument(ShapeType.BYTE, value);
    }

    /**
     * Creates a short document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(short value) {
        return new Documents.IntegralDocument(ShapeType.SHORT, value);
    }

    /**
     * Creates an integer document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(int value) {
        return new Documents.IntegralDocument(ShapeType.INTEGER, value);
    }

    /**
     * Creates a long document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(long value) {
        return new Documents.IntegralDocument(ShapeType.LONG, value);
    }

    /**
     * Creates a float document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(float value) {
        return new Documents.FloatingPointDocument(ShapeType.FLOAT, value);
    }

    /**
     * Creates a double document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(double value) {
        return new Documents.FloatingPointDocument(ShapeType.DOUBLE, value);
    }

    /**
     * Creates a big integer document.
     *
     * @param value the value
     * @return the document
     * @throws NullPointerException if {@code value} is null
     */
    static Document of(BigInteger value) {
        return new Documents.BigIntegerDocument(value);
    }

    /**
     * Creates a big decimal document.
     *
     * @param value the value
     * @return the document
     * @throws NullPointerException if {@code value} is null
     */
    static Document of(BigDecimal value) {
        return new Documents.BigDecimalDocument(value);
    }

    /**
     * Creates a string document.
     *
     * @param value the value
     * @return the document
     * @throws NullPointerException if {@code value} is null
     */
    static Document of(String value) {
        return new Documents.StringDocument(value);
    }

    /**
     * Creates a blob document over the remaining bytes of a buffer. The buffer is not copied.
     *
     * @param value the bytes
     * @return the document
     * @throws NullPointerException if {@code value} is null
     */
    static Document of(ByteBuffer value) {
        return new Documents.BlobDocument(value);
    }

    /**
     * Creates a blob document over a byte array. The array is not copied.
     *
     * @param value the bytes
     * @return the document
     * @throws NullPointerException if {@code value} is null
     */
    static Document of(byte[] value) {
        return of(ByteBuffer.wrap(value));
    }

    /**
     * Creates a timestamp document.
     *
     * @param value the value
     * @return the document
     * @throws NullPointerException if {@code value} is null
     */
    static Document of(Instant value) {
        return new Documents.TimestampDocument(value);
    }

    /**
     * Creates a list document. The list is not copied. Elements may be null.
     *
     * @param values the elements
     * @return the document
     * @throws NullPointerException if {@code values} is null
     */
    static Document of(List<Document> values) {
        return new Documents.ListDocument(values);
    }

    /**
     * Creates a map document. The map is not copied, and its iteration order is the document's member order. Values
     * may be null.
     *
     * @param members the entries
     * @return the document
     * @throws NullPointerException if {@code members} is null
     */
    static Document of(Map<String, Document> members) {
        return new Documents.StringMapDocument(members);
    }

    /**
     * Returns whether two documents are equal: both null, or both documents of the same type with equal values.
     *
     * @param left the left value
     * @param right the right value
     * @return true if they are equal documents
     */
    static boolean equals(Object left, Object right) {
        return equals(left, right, 0);
    }

    /**
     * Returns whether two documents are equal, using the given {@link DocumentEqualsFlags}.
     *
     * <p>Without flags, documents are equal when both are null, or both are documents of the same type with equal
     * values. With {@link DocumentEqualsFlags#NUMBER_PROMOTION}, numeric documents of different numeric types are
     * equal when their values are equal after JLS 5.1.2 widening.
     *
     * @param left the left value
     * @param right the right value
     * @param options flags from {@link DocumentEqualsFlags}, OR'd together
     * @return true if they are equal documents
     */
    static boolean equals(Object left, Object right, int options) {
        if (left == null && right == null) {
            return true;
        }
        if (!(left instanceof Document) || !(right instanceof Document)) {
            return false;
        }
        if (left == right) {
            return true;
        }
        return Documents.documentEquals((Document) left, (Document) right, options);
    }

    /**
     * Returns a hash code for a document that is consistent with {@link #equals(Object, Object)} without flags.
     * Document implementations should return this value from {@code hashCode()}.
     *
     * @param document the document, which may be null
     * @return the hash code, or 0 for null
     */
    static int hash(Document document) {
        return document == null ? 0 : Documents.hash(document);
    }
}
