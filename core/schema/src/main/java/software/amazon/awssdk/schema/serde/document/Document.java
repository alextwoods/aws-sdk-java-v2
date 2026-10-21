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
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.Validate;

/**
 * A Smithy document: untyped data from the Smithy data model.
 *
 * <p>This is the SDK port of smithy-java {@code software.amazon.smithy.java.core.serde.document.Document}. It covers
 * the Smithy data model: booleans, the eight numeric types, strings, blobs, timestamps, lists, string-keyed maps, and
 * structures and unions. A document reports its data model type with {@link #type()} and returns its value through
 * the matching {@code as<Type>} method.
 *
 * <h2>Serializing documents and their contents</h2>
 * <p>A document is a {@link SerializableShape}: {@link #serialize(ShapeSerializer)} always writes it as one
 * {@link ShapeSerializer#writeDocument} call. A serializer that receives a document can read its value through the
 * {@code as<Type>} methods, or have the document write its value with {@link #serializeContents(ShapeSerializer)}.
 *
 * <h2>Typed documents</h2>
 * <p>{@link #of(SerializableShape)} creates a document from a modeled shape, such as an instance of a generated model
 * class. A structure becomes a structure document whose members are keyed by Smithy member name, whose
 * {@link #getDiscriminator() discriminator} is the structure's ShapeId, and which writes the original structure when
 * its contents are serialized. {@link #asShape(ShapeBuilder)} converts a document back into a modeled shape. There is
 * no {@code asStructure} or {@code asUnion}: structures and unions are read only through {@code asShape}.
 *
 * <h2>Null values</h2>
 * <p>A Smithy {@code null} is represented by a Java {@code null} where a document is expected, for example as a list
 * element or map value.
 *
 * <h2>Coercion</h2>
 * <p>The numeric {@code as<Type>} methods accept any numeric document and convert its value with the Java widening
 * (JLS 5.1.2) and narrowing (JLS 5.1.3) primitive conversions, without throwing on overflow or loss of precision. For
 * example, {@code asInteger()} on a long document holding 4294967297 returns 1, and on a double document holding 1.9
 * returns 1. The one numeric conversion that throws is {@code asBigDecimal()} on a NaN or infinite float or double.
 *
 * <p>{@link #asMap()} also accepts structure and union documents. Any other call for a type the document does not
 * hold throws a {@link SerializationException} whose message names the requested type and the document's type, for
 * example {@code "Expected a string document, but found integer"}. Codec documents (such as JSON documents, which
 * hold blobs as base64 strings) may accept more conversions.
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
     * Writes the document's value as the Smithy data model, with the most specific schema the document has.
     *
     * <p>{@link #serialize(ShapeSerializer)} always writes a document with {@code writeDocument}; this method writes
     * what the document holds. A string document calls {@code writeString}, a list document calls {@code writeList}
     * and writes each element with {@link #serialize(ShapeSerializer)}, and a structure document calls
     * {@code writeStruct}. Documents created from a modeled shape pass the schemas they were created with; other
     * documents pass the {@link PreludeSchemas} schema of their type, or a document list or map schema.
     *
     * <p>Implementations must not write the value itself with {@code writeDocument}, because a serializer that calls
     * this method from its {@code writeDocument} would recurse forever.
     *
     * <p>The default implementation writes the value returned by the {@code as<Type>} method that matches
     * {@link #type()}, with the {@link PreludeSchemas} schema of that type; lists and maps are written as an untyped
     * list or map document holding {@link #asList()} or {@link #asMap()}. Documents that hold a structure or union
     * must override it.
     *
     * @param serializer where to write the document's value
     * @throws SerializationException in the default implementation, if the document holds a structure, a union, or
     *     another type that has no data model value
     */
    default void serializeContents(ShapeSerializer serializer) {
        Documents.serializeContentsByType(this, serializer);
    }

    /**
     * Returns the ShapeId of the shape the document holds, if the document has one.
     *
     * <p>A structure document created from a modeled structure with {@link #of(SerializableShape)} returns that
     * structure's ShapeId. Codec documents return the discriminator they read from the data, such as JSON's
     * {@code __type} member. Other documents return null.
     *
     * <p>This is smithy-java's {@code discriminator()}, named {@code getDiscriminator()} as in the Document SEP.
     *
     * @return the discriminator, or null if the document has none
     * @throws DiscriminatorException if the document holds a discriminator that is not a valid ShapeId
     */
    default ShapeId getDiscriminator() {
        return null;
    }

    /**
     * Returns the document's discriminator, which must be present.
     *
     * @return the discriminator
     * @throws DiscriminatorException if the document has no discriminator, or holds one that is not a valid ShapeId
     * @see #getDiscriminator()
     */
    default ShapeId expectDiscriminator() {
        ShapeId result = getDiscriminator();
        if (result == null) {
            throw new DiscriminatorException(type() + " document has no discriminator");
        }
        return result;
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
     * Returns the entries of a map document, or the members of a structure or union document, as an unmodifiable map.
     *
     * <p>Entries are in the order they were added or read. A structure or union document is keyed by Smithy member
     * name (not {@code jsonName} or {@code xmlName}) and holds only the members that have a value. Map values may be
     * null.
     *
     * <p>This is smithy-java's {@code asStringMap()}, named {@code asMap()} as in the Document SEP.
     *
     * @return the map entries
     * @throws SerializationException if the document is not a map, structure, or union
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
     * Returns a map entry, or a structure or union member, by name.
     *
     * @param memberName the entry key, or the Smithy member name
     * @return the member, or null if the document has no such member or the member's value is null
     * @throws SerializationException if the document is not a map, structure, or union
     */
    default Document getMember(String memberName) {
        throw Documents.typeMismatch("map, structure, or union", this);
    }

    /**
     * Returns the entry keys of a map document, or the names of the members a structure or union document holds, as
     * an unmodifiable set in the same order as {@link #asMap()}.
     *
     * @return the member names
     * @throws SerializationException if the document is not a map, structure, or union
     */
    default Set<String> getMemberNames() {
        throw Documents.typeMismatch("map, structure, or union", this);
    }

    /**
     * Creates a deserializer that reads the document.
     *
     * @return the deserializer
     */
    default ShapeDeserializer createDeserializer() {
        return new DocumentDeserializer(this);
    }

    /**
     * Populates a shape builder from the document, through the builder's
     * {@link ShapeBuilder#deserialize(ShapeDeserializer)} method.
     *
     * @param builder the builder to populate
     * @param <T> the shape type
     * @throws SerializationException if the document does not hold data of the builder's shape type
     */
    default <T extends SerializableShape> void deserializeInto(ShapeBuilder<T> builder) {
        builder.deserialize(createDeserializer());
    }

    /**
     * Converts the document into a modeled shape: populates the builder with {@link #deserializeInto(ShapeBuilder)},
     * applies {@link ShapeBuilder#errorCorrection()}, and builds the shape.
     *
     * <p>A structure is read from the document's members by Smithy member name. Members the shape does not have are
     * ignored.
     *
     * @param builder the builder of the shape to create
     * @param <T> the shape type
     * @return the built shape
     * @throws SerializationException if the document does not hold data of the builder's shape type, for example a
     *     string document with a structure builder
     */
    default <T extends SerializableShape> T asShape(ShapeBuilder<T> builder) {
        deserializeInto(builder);
        return builder.errorCorrection().build();
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
        return new Documents.IntegralDocument(ShapeType.BYTE, null, value);
    }

    /**
     * Creates a short document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(short value) {
        return new Documents.IntegralDocument(ShapeType.SHORT, null, value);
    }

    /**
     * Creates an integer document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(int value) {
        return new Documents.IntegralDocument(ShapeType.INTEGER, null, value);
    }

    /**
     * Creates a long document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(long value) {
        return new Documents.IntegralDocument(ShapeType.LONG, null, value);
    }

    /**
     * Creates a float document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(float value) {
        return new Documents.FloatingPointDocument(ShapeType.FLOAT, null, value);
    }

    /**
     * Creates a double document.
     *
     * @param value the value
     * @return the document
     */
    static Document of(double value) {
        return new Documents.FloatingPointDocument(ShapeType.DOUBLE, null, value);
    }

    /**
     * Creates a big integer document.
     *
     * @param value the value
     * @return the document
     * @throws NullPointerException if {@code value} is null
     */
    static Document of(BigInteger value) {
        return new Documents.BigIntegerDocument(null, value);
    }

    /**
     * Creates a big decimal document.
     *
     * @param value the value
     * @return the document
     * @throws NullPointerException if {@code value} is null
     */
    static Document of(BigDecimal value) {
        return new Documents.BigDecimalDocument(null, value);
    }

    /**
     * Creates a string document.
     *
     * @param value the value
     * @return the document
     * @throws NullPointerException if {@code value} is null
     */
    static Document of(String value) {
        return new Documents.StringDocument(null, value);
    }

    /**
     * Creates a blob document over the remaining bytes of a buffer. The buffer is not copied.
     *
     * @param value the bytes
     * @return the document
     * @throws NullPointerException if {@code value} is null
     */
    static Document of(ByteBuffer value) {
        return new Documents.BlobDocument(null, value);
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
        return new Documents.TimestampDocument(null, value);
    }

    /**
     * Creates a list document. The list is not copied. Elements may be null.
     *
     * @param values the elements
     * @return the document
     * @throws NullPointerException if {@code values} is null
     */
    static Document of(List<Document> values) {
        return new Documents.ListDocument(null, values);
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
        return new Documents.StringMapDocument(null, members);
    }

    /**
     * Creates a document from a modeled shape, such as an instance of a generated model class.
     *
     * <p>The shape is written to a {@link DocumentParser}, which calls only the shape's
     * {@link SerializableShape#serialize(ShapeSerializer)} method (and, for a structure, its {@code serializeMembers}
     * method). The document's {@link #type()} is the data model type of the shape. A structure becomes a structure
     * document:
     * <ul>
     *     <li>{@link #getDiscriminator()} returns the structure's ShapeId;</li>
     *     <li>{@link #asMap()}, {@link #getMember(String)} and {@link #getMemberNames()} hold the members that have a
     *     value, keyed by Smithy member name, in the order the structure writes them. The members are read from the
     *     structure when they are first needed, so the structure must not change after the document is created;</li>
     *     <li>{@link #serializeContents(ShapeSerializer)} writes the structure itself, so a codec serializes the
     *     document exactly as it would serialize the structure.</li>
     * </ul>
     * A union is read at once into a {@link ShapeType#STRUCTURE} document that has no discriminator. A document
     * passed to this method is copied from its contents.
     *
     * @param shape the shape
     * @return the document, or null if the shape writes a null value
     * @throws NullPointerException if {@code shape} is null
     */
    static Document of(SerializableShape shape) {
        DocumentParser parser = new DocumentParser();
        Validate.paramNotNull(shape, "shape").serialize(parser);
        return parser.getResult();
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
     * values. Structure documents are compared by their members; schemas and discriminators are not compared. With
     * {@link DocumentEqualsFlags#NUMBER_PROMOTION}, numeric documents of different numeric types are equal when their
     * values are equal after JLS 5.1.2 widening, at every nesting depth.
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
