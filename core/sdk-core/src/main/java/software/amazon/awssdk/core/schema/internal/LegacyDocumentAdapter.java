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

package software.amazon.awssdk.core.schema.internal;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Lazy views between the SDK's Legacy_Document ({@link software.amazon.awssdk.core.document.Document}) and the schema
 * {@link Document} (design B.4), behind {@code SchemaSerdeSupport.wrapLegacy}/{@code unwrapLegacy} (generated code) and
 * {@code DocumentConverter} (customers). Neither direction copies a tree: a list or map view converts an element or
 * value when it is read, so a document member is converted only as far as a codec walks it.
 *
 * <p>Schema views of a Legacy_Document compare like other schema documents ({@link Document#equals(Object, Object)}).
 * Legacy views of a schema document are the SDK's own {@code ListDocument}/{@code MapDocument}/scalar classes over lazy
 * collections, so their {@code equals}, {@code hashCode} and {@code toString} are the Legacy_Document ones. The lazy
 * collections serialize (Java serialization) as a {@link ArrayList} or {@link LinkedHashMap} copy.
 */
@SdkInternalApi
public final class LegacyDocumentAdapter {
    /**
     * The schema document of a top-level Legacy_Document null, for {@code DocumentConverter}. The schema module has no
     * null document (a Smithy null is a Java {@code null}), so this view is the only one: its {@code type()} is
     * {@link ShapeType#DOCUMENT}, it writes {@code writeNull}, and {@link #toLegacy} returns
     * {@code Document.fromNull()} for it.
     */
    public static final Document NULL = new NullView();

    /**
     * An integral number with more digits than this is a big decimal document rather than a big integer, so that a
     * number such as {@code 1E+999999999} does not expand into a billion-digit integer.
     */
    private static final int MAX_BIG_INTEGER_DIGITS = 1000;
    private static final int DOUBLE_DIGITS = 15;

    private LegacyDocumentAdapter() {
    }

    /**
     * @return the schema view of a Legacy_Document, or null for null or a null document
     */
    public static Document toSchema(software.amazon.awssdk.core.document.Document legacy) {
        if (legacy == null || legacy.isNull()) {
            return null;
        }
        if (legacy.isString()) {
            return Document.of(legacy.asString());
        }
        if (legacy.isBoolean()) {
            return Document.of(legacy.asBoolean());
        }
        if (legacy.isNumber()) {
            return new NumberView(legacy);
        }
        if (legacy.isList()) {
            return new ListView(legacy);
        }
        if (legacy.isMap()) {
            return new MapView(legacy);
        }
        throw new SerializationException("Unsupported document: " + legacy.getClass().getName());
    }

    /**
     * @return the Legacy_Document view of a schema document: the original Legacy_Document of a view made by
     *     {@link #toSchema}, {@code Document.fromNull()} for null and {@link #NULL}
     */
    public static software.amazon.awssdk.core.document.Document toLegacy(Document document) {
        if (document == null) {
            return software.amazon.awssdk.core.document.Document.fromNull();
        }
        if (document instanceof SchemaView) {
            return ((SchemaView) document).legacy();
        }
        switch (document.type()) {
            case BOOLEAN:
                return software.amazon.awssdk.core.document.Document.fromBoolean(document.asBoolean());
            case STRING:
            case ENUM:
                return software.amazon.awssdk.core.document.Document.fromString(document.asString());
            case BYTE:
            case SHORT:
            case INTEGER:
            case INT_ENUM:
                return software.amazon.awssdk.core.document.Document.fromNumber(document.asInteger());
            case LONG:
                return software.amazon.awssdk.core.document.Document.fromNumber(document.asLong());
            case FLOAT:
                return software.amazon.awssdk.core.document.Document.fromNumber(document.asFloat());
            case DOUBLE:
                return software.amazon.awssdk.core.document.Document.fromNumber(document.asDouble());
            case BIG_INTEGER:
                return software.amazon.awssdk.core.document.Document.fromNumber(document.asBigInteger());
            case BIG_DECIMAL:
                return software.amazon.awssdk.core.document.Document.fromNumber(document.asBigDecimal());
            case BLOB:
                return software.amazon.awssdk.core.document.Document.fromString(
                    BinaryUtils.toBase64(BinaryUtils.copyRemainingBytesFrom(document.asBlob())));
            case TIMESTAMP:
                return software.amazon.awssdk.core.document.Document.fromNumber(epochSeconds(document.asTimestamp()));
            case LIST:
                return software.amazon.awssdk.core.document.Document.fromList(new LegacyList(document.asList()));
            case MAP:
            case STRUCTURE:
            case UNION:
                return software.amazon.awssdk.core.document.Document.fromMap(new LegacyMap(document.asMap()));
            default:
                throw new SerializationException("Unsupported " + document.type() + " document");
        }
    }

    /**
     * Epoch seconds with at most three fraction digits, as the JSON protocols write {@code epoch-seconds} timestamps.
     * Digits below a millisecond are dropped toward negative infinity, as {@link Instant#toEpochMilli()} drops them,
     * but without its overflow: every {@link Instant} converts.
     */
    private static BigDecimal epochSeconds(Instant timestamp) {
        BigDecimal seconds = BigDecimal.valueOf(timestamp.getEpochSecond())
                                       .add(BigDecimal.valueOf(timestamp.getNano() / 1_000_000, 3))
                                       .stripTrailingZeros();
        return seconds.scale() < 0 ? seconds.setScale(0) : seconds;
    }

    /**
     * The schema document a Legacy_Document number reads as. The type is the first of integer, long, bigInteger,
     * double and bigDecimal that holds the value exactly (Requirement 18.5); NaN and infinities are doubles.
     */
    static Document numberDocument(SdkNumber number) {
        String text = number.stringValue();
        switch (text) {
            case "NaN":
                return Document.of(Double.NaN);
            case "Infinity":
                return Document.of(Double.POSITIVE_INFINITY);
            case "-Infinity":
                return Document.of(Double.NEGATIVE_INFINITY);
            default:
                break;
        }
        BigDecimal value;
        try {
            value = new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new SerializationException("A document number is not a valid number", e);
        }
        BigDecimal stripped = value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
        if (stripped.scale() <= 0) {
            int integerDigits = stripped.precision() - stripped.scale();
            if (integerDigits <= 19) {
                BigInteger integer = stripped.toBigIntegerExact();
                if (integer.bitLength() < 32) {
                    return Document.of(integer.intValue());
                }
                if (integer.bitLength() < 64) {
                    return Document.of(integer.longValue());
                }
                return Document.of(integer);
            }
            if (integerDigits <= MAX_BIG_INTEGER_DIGITS) {
                return Document.of(stripped.toBigIntegerExact());
            }
            return Document.of(value);
        }
        double asDouble = value.doubleValue();
        if (Double.isFinite(asDouble)
            && (stripped.precision() <= DOUBLE_DIGITS && Math.abs(asDouble) >= Double.MIN_NORMAL
                || BigDecimal.valueOf(asDouble).compareTo(value) == 0)) {
            return Document.of(asDouble);
        }
        return Document.of(value);
    }

    /**
     * A schema view that remembers the Legacy_Document it was made from.
     */
    private interface SchemaView extends Document {
        software.amazon.awssdk.core.document.Document legacy();
    }

    /**
     * Base class of the schema views: value equality and hash code of schema documents.
     */
    private abstract static class AbstractSchemaView implements SchemaView {
        @Override
        public final boolean equals(Object obj) {
            return Document.equals(this, obj);
        }

        @Override
        public final int hashCode() {
            return Document.hash(this);
        }
    }

    /**
     * {@link #NULL}: a Legacy_Document null as a document value. Every {@code as<Type>} method except
     * {@code asObject()} (null) throws, {@code getMember} returns null, and it is written with {@code writeNull}.
     */
    private static final class NullView extends AbstractSchemaView {
        @Override
        public software.amazon.awssdk.core.document.Document legacy() {
            return software.amazon.awssdk.core.document.Document.fromNull();
        }

        @Override
        public ShapeType type() {
            return ShapeType.DOCUMENT;
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            serializer.writeNull(PreludeSchemas.DOCUMENT);
        }

        @Override
        public Object asObject() {
            return null;
        }

        @Override
        public Document getMember(String memberName) {
            return null;
        }

        @Override
        public String toString() {
            return "Document(null)";
        }
    }

    /**
     * A Legacy_Document number. Typed and coerced like {@link #numberDocument(SdkNumber)}; the original
     * Legacy_Document is kept, so converting back returns it, and its {@link SdkNumber} string form.
     */
    private static final class NumberView extends AbstractSchemaView {
        private final software.amazon.awssdk.core.document.Document legacy;
        private final Document value;

        private NumberView(software.amazon.awssdk.core.document.Document legacy) {
            this.legacy = legacy;
            this.value = numberDocument(legacy.asNumber());
        }

        @Override
        public software.amazon.awssdk.core.document.Document legacy() {
            return legacy;
        }

        @Override
        public ShapeType type() {
            return value.type();
        }

        @Override
        public byte asByte() {
            return value.asByte();
        }

        @Override
        public short asShort() {
            return value.asShort();
        }

        @Override
        public int asInteger() {
            return value.asInteger();
        }

        @Override
        public long asLong() {
            return value.asLong();
        }

        @Override
        public float asFloat() {
            return value.asFloat();
        }

        @Override
        public double asDouble() {
            return value.asDouble();
        }

        @Override
        public BigInteger asBigInteger() {
            return value.asBigInteger();
        }

        @Override
        public BigDecimal asBigDecimal() {
            return value.asBigDecimal();
        }

        @Override
        public Number asNumber() {
            return value.asNumber();
        }

        @Override
        public String toString() {
            return "Document(" + type() + ": " + legacy + ")";
        }
    }

    /**
     * A Legacy_Document list; elements are converted when read.
     */
    private static final class ListView extends AbstractSchemaView {
        private final software.amazon.awssdk.core.document.Document legacy;

        private ListView(software.amazon.awssdk.core.document.Document legacy) {
            this.legacy = legacy;
        }

        @Override
        public software.amazon.awssdk.core.document.Document legacy() {
            return legacy;
        }

        @Override
        public ShapeType type() {
            return ShapeType.LIST;
        }

        @Override
        public List<Document> asList() {
            return new SchemaList(legacy.asList());
        }

        @Override
        public int size() {
            return legacy.asList().size();
        }

        @Override
        public String toString() {
            return "Document(LIST: " + legacy + ")";
        }
    }

    /**
     * A Legacy_Document map; values are converted when read.
     */
    private static final class MapView extends AbstractSchemaView {
        private final software.amazon.awssdk.core.document.Document legacy;

        private MapView(software.amazon.awssdk.core.document.Document legacy) {
            this.legacy = legacy;
        }

        @Override
        public software.amazon.awssdk.core.document.Document legacy() {
            return legacy;
        }

        @Override
        public ShapeType type() {
            return ShapeType.MAP;
        }

        @Override
        public Map<String, Document> asMap() {
            return new SchemaMap(legacy.asMap());
        }

        @Override
        public int size() {
            return legacy.asMap().size();
        }

        @Override
        public Document getMember(String memberName) {
            return toSchema(legacy.asMap().get(memberName));
        }

        @Override
        public Set<String> getMemberNames() {
            return Collections.unmodifiableSet(legacy.asMap().keySet());
        }

        @Override
        public String toString() {
            return "Document(MAP: " + legacy + ")";
        }
    }

    /**
     * An unmodifiable list that converts the elements of a Legacy_Document list when they are read.
     */
    private static final class SchemaList extends AbstractList<Document> implements RandomAccess {
        private final List<software.amazon.awssdk.core.document.Document> elements;

        private SchemaList(List<software.amazon.awssdk.core.document.Document> elements) {
            this.elements = elements;
        }

        @Override
        public Document get(int index) {
            return toSchema(elements.get(index));
        }

        @Override
        public int size() {
            return elements.size();
        }
    }

    /**
     * An unmodifiable map that converts the values of a Legacy_Document map when they are read, in the map's order.
     */
    private static final class SchemaMap extends AbstractMap<String, Document> {
        private final Map<String, software.amazon.awssdk.core.document.Document> entries;

        private SchemaMap(Map<String, software.amazon.awssdk.core.document.Document> entries) {
            this.entries = entries;
        }

        @Override
        public Document get(Object key) {
            return toSchema(entries.get(key));
        }

        @Override
        public boolean containsKey(Object key) {
            return entries.containsKey(key);
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public Set<Entry<String, Document>> entrySet() {
            return new ConvertingEntrySet<>(entries, LegacyDocumentAdapter::toSchema);
        }
    }

    /**
     * An unmodifiable list that converts the elements of a schema list document to Legacy_Documents when they are read.
     * Java serialization writes an {@link ArrayList} copy.
     */
    private static final class LegacyList extends AbstractList<software.amazon.awssdk.core.document.Document>
        implements RandomAccess, Serializable {
        private static final long serialVersionUID = 1L;

        private final transient List<Document> elements;

        private LegacyList(List<Document> elements) {
            this.elements = elements;
        }

        @Override
        public software.amazon.awssdk.core.document.Document get(int index) {
            return toLegacy(elements.get(index));
        }

        @Override
        public int size() {
            return elements.size();
        }

        private Object writeReplace() {
            return new ArrayList<>(this);
        }

        private void readObject(ObjectInputStream in) throws InvalidObjectException {
            throw new InvalidObjectException("Serialized as an ArrayList");
        }
    }

    /**
     * An unmodifiable map that converts the values of a schema map document to Legacy_Documents when they are read, in
     * the document's order. Java serialization writes a {@link LinkedHashMap} copy.
     */
    private static final class LegacyMap extends AbstractMap<String, software.amazon.awssdk.core.document.Document>
        implements Serializable {
        private static final long serialVersionUID = 1L;

        private final transient Map<String, Document> entries;

        private LegacyMap(Map<String, Document> entries) {
            this.entries = entries;
        }

        @Override
        public software.amazon.awssdk.core.document.Document get(Object key) {
            return entries.containsKey(key) ? toLegacy(entries.get(key)) : null;
        }

        @Override
        public boolean containsKey(Object key) {
            return entries.containsKey(key);
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public Set<Entry<String, software.amazon.awssdk.core.document.Document>> entrySet() {
            return new ConvertingEntrySet<>(entries, LegacyDocumentAdapter::toLegacy);
        }

        private Object writeReplace() {
            return new LinkedHashMap<>(this);
        }

        private void readObject(ObjectInputStream in) throws InvalidObjectException {
            throw new InvalidObjectException("Serialized as a LinkedHashMap");
        }
    }

    /**
     * The entries of a map with converted values, converted as they are iterated.
     */
    private static final class ConvertingEntrySet<S, T> extends AbstractSet<Map.Entry<String, T>> {
        private final Map<String, S> source;
        private final Function<S, T> converter;

        private ConvertingEntrySet(Map<String, S> source, Function<S, T> converter) {
            this.source = source;
            this.converter = converter;
        }

        @Override
        public Iterator<Map.Entry<String, T>> iterator() {
            Iterator<Map.Entry<String, S>> iterator = source.entrySet().iterator();
            return new Iterator<Map.Entry<String, T>>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Map.Entry<String, T> next() {
                    Map.Entry<String, S> entry = iterator.next();
                    return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), converter.apply(entry.getValue()));
                }
            };
        }

        @Override
        public int size() {
            return source.size();
        }
    }
}
