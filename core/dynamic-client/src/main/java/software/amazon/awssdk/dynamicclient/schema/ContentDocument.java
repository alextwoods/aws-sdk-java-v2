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
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * A document that holds an unwrapped scalar, list or map value together with the schema to serialize it with.
 *
 * <p>The value's class matches the schema type: {@link String} for strings and enums, {@link Boolean}, the boxed
 * number of the numeric type ({@link Integer} for intEnums), {@link Instant}, {@link ByteBuffer}, an unmodifiable list
 * of documents, or an unmodifiable map of documents. {@link #type()} is the data model type, so it is
 * {@link ShapeType#STRING} for enums and {@link ShapeType#INTEGER} for intEnums, as for other documents.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.dynamicschemas.ContentDocument}.
 */
@SdkInternalApi
final class ContentDocument implements Document {
    private static final String REDACTED = "*** Sensitive Data Redacted ***";

    // Non-capturing, concretely typed consumers for MapSerializer#writeEntry, which keep the value write monomorphic.
    private static final BiConsumer<Schema, ShapeSerializer> NULL_VALUE_WRITER = (vm, ser) -> ser.writeNull(vm);
    private static final BiConsumer<StructDocument, ShapeSerializer> STRUCT_VALUE_WRITER =
        (d, ser) -> ser.writeStruct(d.sdkSchema(), d);
    private static final BiConsumer<ContentDocument, ShapeSerializer> STRING_VALUE_WRITER =
        (d, ser) -> ser.writeString(d.schema, (String) d.value);

    private final Schema schema;
    private final Object value;

    ContentDocument(Schema schema, Object value) {
        this.schema = schema;
        this.value = unmodifiable(schema.type(), value);
    }

    @SuppressWarnings("unchecked")
    private static Object unmodifiable(ShapeType type, Object value) {
        switch (type) {
            case LIST:
                return Collections.unmodifiableList((List<Document>) value);
            case MAP:
                return Collections.unmodifiableMap((Map<String, Document>) value);
            default:
                return value;
        }
    }

    Object rawValue() {
        return value;
    }

    boolean isScalar() {
        ShapeType type = schema.type();
        return type != ShapeType.LIST && type != ShapeType.MAP;
    }

    /**
     * Writes a member value with its member schema: a member that targets {@code document} with
     * {@code writeDocument}, any other with the value's {@link Document#serializeContents(ShapeSerializer)}.
     */
    static void writeValue(ShapeSerializer serializer, Schema member, Document value) {
        if (member.type() == ShapeType.DOCUMENT) {
            serializer.writeDocument(member, value);
        } else {
            value.serializeContents(serializer);
        }
    }

    /**
     * Returns smithy-java's coercion failure: {@code "Expected a string document, but found integer"}.
     */
    static SerializationException typeMismatch(String expected, Document actual) {
        String article = "integer".equals(expected) ? "an " : "a ";
        return new SerializationException("Expected " + article + expected + " document, but found " + actual.type());
    }

    /**
     * Formats a document as {@code Document(<type>: <value>)}, redacting the value when the schema has the
     * {@code sensitive} trait (on the member or its target), as the documents of {@code core/schema} do.
     */
    static String describe(Schema schema, ShapeType type, Object value) {
        Object shown = schema != null && schema.hasTrait(TraitKey.SENSITIVE_TRAIT) ? REDACTED : value;
        return "Document(" + type + ": " + shown + ")";
    }

    @Override
    public ShapeType type() {
        return dataModelType(schema.type());
    }

    private static ShapeType dataModelType(ShapeType type) {
        switch (type) {
            case ENUM:
                return ShapeType.STRING;
            case INT_ENUM:
                return ShapeType.INTEGER;
            default:
                return type;
        }
    }

    @Override
    public void serializeContents(ShapeSerializer serializer) {
        // Keep in sync with serializeScalar; split for the C2 inline budget.
        Schema s = schema;
        switch (s.type()) {
            case STRING:
            case ENUM:
                serializer.writeString(s, (String) value);
                break;
            case BOOLEAN:
                serializer.writeBoolean(s, (Boolean) value);
                break;
            case INTEGER:
            case INT_ENUM:
                serializer.writeInteger(s, ((Number) value).intValue());
                break;
            case LONG:
                serializer.writeLong(s, ((Number) value).longValue());
                break;
            case DOUBLE:
                serializer.writeDouble(s, ((Number) value).doubleValue());
                break;
            case FLOAT:
                serializer.writeFloat(s, ((Number) value).floatValue());
                break;
            case BYTE:
                serializer.writeByte(s, ((Number) value).byteValue());
                break;
            case SHORT:
                serializer.writeShort(s, ((Number) value).shortValue());
                break;
            case TIMESTAMP:
                serializer.writeTimestamp(s, (Instant) value);
                break;
            default:
                serializeNonScalar(serializer, s);
                break;
        }
    }

    /**
     * Writes a raw union member value, stored without a {@code ContentDocument} wrapper. Keep in sync with
     * {@link #serializeContents(ShapeSerializer)}.
     */
    static void serializeScalar(ShapeSerializer serializer, Schema schema, Object value) {
        switch (schema.type()) {
            case STRING:
            case ENUM:
                serializer.writeString(schema, (String) value);
                break;
            case BOOLEAN:
                serializer.writeBoolean(schema, (Boolean) value);
                break;
            case INTEGER:
            case INT_ENUM:
                serializer.writeInteger(schema, ((Number) value).intValue());
                break;
            case LONG:
                serializer.writeLong(schema, ((Number) value).longValue());
                break;
            case DOUBLE:
                serializer.writeDouble(schema, ((Number) value).doubleValue());
                break;
            case FLOAT:
                serializer.writeFloat(schema, ((Number) value).floatValue());
                break;
            case BYTE:
                serializer.writeByte(schema, ((Number) value).byteValue());
                break;
            case SHORT:
                serializer.writeShort(schema, ((Number) value).shortValue());
                break;
            case TIMESTAMP:
                serializer.writeTimestamp(schema, (Instant) value);
                break;
            case BLOB:
                serializer.writeBlob(schema, ((ByteBuffer) value).duplicate());
                break;
            case BIG_INTEGER:
                serializer.writeBigInteger(schema, toBigInteger((Number) value));
                break;
            case BIG_DECIMAL:
                serializer.writeBigDecimal(schema, toBigDecimal(ShapeType.BIG_DECIMAL, (Number) value));
                break;
            default:
                throw new UnsupportedOperationException("Unsupported scalar type: " + schema.type());
        }
    }

    private void serializeNonScalar(ShapeSerializer serializer, Schema s) {
        switch (s.type()) {
            case BLOB:
                serializer.writeBlob(s, ((ByteBuffer) value).duplicate());
                break;
            case BIG_INTEGER:
                serializer.writeBigInteger(s, asBigInteger());
                break;
            case BIG_DECIMAL:
                serializer.writeBigDecimal(s, asBigDecimal());
                break;
            case LIST:
                serializer.writeList(s, this, size(), ContentDocument::serializeListContents);
                break;
            case MAP:
                serializer.writeMap(s, this, size(), ContentDocument::serializeMapContents);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported type: " + s.type());
        }
    }

    @SuppressWarnings("unchecked")
    private static void serializeListContents(ContentDocument doc, ShapeSerializer ser) {
        Schema member = doc.schema.listMember();
        List<Document> list = (List<Document>) doc.value;
        // String and enum fast path: a monomorphic writeString loop without per-element dispatch.
        ShapeType memberType = member.type();
        if (memberType == ShapeType.STRING || memberType == ShapeType.ENUM) {
            writeStringList(ser, list, member);
        } else if (list instanceof RandomAccess) {
            for (int i = 0, n = list.size(); i < n; i++) {
                writeElement(ser, member, list.get(i));
            }
        } else {
            for (Document e : list) {
                writeElement(ser, member, e);
            }
        }
    }

    private static void writeElement(ShapeSerializer ser, Schema member, Document e) {
        if (e == null) {
            ser.writeNull(member);
        } else {
            writeValue(ser, member, e);
        }
    }

    private static void writeStringList(ShapeSerializer ser, List<Document> list, Schema member) {
        for (int i = 0, n = list.size(); i < n; i++) {
            Document e = list.get(i);
            if (e == null) {
                ser.writeNull(member);
            } else {
                ser.writeString(member, e.asString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void serializeMapContents(ContentDocument doc, MapSerializer ms) {
        Schema key = doc.schema.mapKeyMember();
        Schema valueMember = doc.schema.mapValueMember();
        Map<String, Document> map = (Map<String, Document>) doc.value;
        switch (valueMember.type()) {
            case STRUCTURE:
            case UNION:
                for (Map.Entry<String, Document> entry : map.entrySet()) {
                    Document v = entry.getValue();
                    if (v == null) {
                        ms.writeEntry(key, entry.getKey(), valueMember, NULL_VALUE_WRITER);
                    } else {
                        ms.writeEntry(key, entry.getKey(), (StructDocument) v, STRUCT_VALUE_WRITER);
                    }
                }
                break;
            case STRING:
            case ENUM:
                for (Map.Entry<String, Document> entry : map.entrySet()) {
                    Document v = entry.getValue();
                    if (v == null) {
                        ms.writeEntry(key, entry.getKey(), valueMember, NULL_VALUE_WRITER);
                    } else {
                        ms.writeEntry(key, entry.getKey(), (ContentDocument) v, STRING_VALUE_WRITER);
                    }
                }
                break;
            default:
                for (Map.Entry<String, Document> entry : map.entrySet()) {
                    Document v = entry.getValue();
                    if (v == null) {
                        ms.writeEntry(key, entry.getKey(), valueMember, NULL_VALUE_WRITER);
                    } else {
                        ms.writeEntry(key, entry.getKey(), v, (d, ser) -> writeValue(ser, valueMember, d));
                    }
                }
                break;
        }
    }

    private boolean isNumeric() {
        switch (schema.type()) {
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

    private Number number(String requested) {
        if (!isNumeric()) {
            throw typeMismatch(requested, this);
        }
        return (Number) value;
    }

    private static boolean isFloatingPoint(Number number) {
        return number instanceof Double || number instanceof Float;
    }

    private static BigInteger toBigInteger(Number number) {
        if (number instanceof BigInteger) {
            return (BigInteger) number;
        }
        if (number instanceof BigDecimal) {
            return ((BigDecimal) number).toBigInteger();
        }
        // JLS 5.1.3 for floating point: truncate toward zero, saturate at the long range, NaN becomes 0.
        if (isFloatingPoint(number)) {
            return BigInteger.valueOf((long) number.doubleValue());
        }
        return BigInteger.valueOf(number.longValue());
    }

    private static BigDecimal toBigDecimal(ShapeType type, Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }
        if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        }
        if (isFloatingPoint(number)) {
            double d = number.doubleValue();
            if (!Double.isFinite(d)) {
                throw new SerializationException("Cannot convert a non-finite " + type + " document (" + d
                                                 + ") to a bigDecimal");
            }
            return BigDecimal.valueOf(d);
        }
        return BigDecimal.valueOf(number.longValue());
    }

    @Override
    public byte asByte() {
        return number("byte").byteValue();
    }

    @Override
    public short asShort() {
        return number("short").shortValue();
    }

    @Override
    public int asInteger() {
        return number("integer").intValue();
    }

    @Override
    public long asLong() {
        return number("long").longValue();
    }

    @Override
    public float asFloat() {
        return number("float").floatValue();
    }

    @Override
    public double asDouble() {
        return number("double").doubleValue();
    }

    @Override
    public BigInteger asBigInteger() {
        return toBigInteger(number("bigInteger"));
    }

    @Override
    public BigDecimal asBigDecimal() {
        return toBigDecimal(type(), number("bigDecimal"));
    }

    @Override
    public boolean asBoolean() {
        if (schema.type() != ShapeType.BOOLEAN) {
            throw typeMismatch("boolean", this);
        }
        return (Boolean) value;
    }

    @Override
    public String asString() {
        ShapeType type = schema.type();
        if (type != ShapeType.STRING && type != ShapeType.ENUM) {
            throw typeMismatch("string", this);
        }
        return (String) value;
    }

    @Override
    public ByteBuffer asBlob() {
        if (schema.type() != ShapeType.BLOB) {
            throw typeMismatch("blob", this);
        }
        return ((ByteBuffer) value).duplicate();
    }

    @Override
    public Instant asTimestamp() {
        if (schema.type() != ShapeType.TIMESTAMP) {
            throw typeMismatch("timestamp", this);
        }
        return (Instant) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Document> asList() {
        if (schema.type() != ShapeType.LIST) {
            throw typeMismatch("list", this);
        }
        return (List<Document>) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Document> asMap() {
        if (schema.type() != ShapeType.MAP) {
            throw typeMismatch("map", this);
        }
        return (Map<String, Document>) value;
    }

    @Override
    public int size() {
        switch (schema.type()) {
            case LIST:
                return ((List<?>) value).size();
            case MAP:
                return ((Map<?, ?>) value).size();
            default:
                return -1;
        }
    }

    @Override
    public Document getMember(String memberName) {
        if (schema.type() == ShapeType.MAP) {
            return asMap().get(memberName);
        }
        return Document.super.getMember(memberName);
    }

    @Override
    public Set<String> getMemberNames() {
        if (schema.type() == ShapeType.MAP) {
            return asMap().keySet();
        }
        return Document.super.getMemberNames();
    }

    @Override
    public boolean equals(Object obj) {
        return Document.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return Document.hash(this);
    }

    @Override
    public String toString() {
        return describe(schema, type(), value);
    }

    /**
     * Wraps another document to give it the schema of a member that targets a {@code document} shape, delegating
     * every value accessor to the wrapped document.
     *
     * <p>Unlike {@link ContentDocument}, which holds an unwrapped value of a known type, the wrapped value can be any
     * kind of document, so {@link #type()} and {@link #getDiscriminator()} are the wrapped document's.
     */
    static final class SchemaDocument implements Document {
        private final Schema schema;
        private final Document document;

        SchemaDocument(Schema schema, Document document) {
            this.schema = schema;
            this.document = document;
        }

        Schema schema() {
            return schema;
        }

        Document document() {
            return document;
        }

        @Override
        public ShapeType type() {
            return document.type();
        }

        @Override
        public ShapeId getDiscriminator() {
            return document.getDiscriminator();
        }

        @Override
        public void serialize(ShapeSerializer serializer) {
            if (schema.type() == ShapeType.DOCUMENT) {
                serializer.writeDocument(schema, this);
            } else {
                serializeContents(serializer);
            }
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            document.serializeContents(serializer);
        }

        @Override
        public BigDecimal asBigDecimal() {
            return document.asBigDecimal();
        }

        @Override
        public BigInteger asBigInteger() {
            return document.asBigInteger();
        }

        @Override
        public ByteBuffer asBlob() {
            return document.asBlob();
        }

        @Override
        public boolean asBoolean() {
            return document.asBoolean();
        }

        @Override
        public byte asByte() {
            return document.asByte();
        }

        @Override
        public double asDouble() {
            return document.asDouble();
        }

        @Override
        public float asFloat() {
            return document.asFloat();
        }

        @Override
        public int asInteger() {
            return document.asInteger();
        }

        @Override
        public List<Document> asList() {
            return document.asList();
        }

        @Override
        public long asLong() {
            return document.asLong();
        }

        @Override
        public Number asNumber() {
            return document.asNumber();
        }

        @Override
        public Object asObject() {
            return document.asObject();
        }

        @Override
        public ShapeDeserializer createDeserializer() {
            return document.createDeserializer();
        }

        @Override
        public <T extends SerializableShape> T asShape(ShapeBuilder<T> builder) {
            return document.asShape(builder);
        }

        @Override
        public short asShort() {
            return document.asShort();
        }

        @Override
        public String asString() {
            return document.asString();
        }

        @Override
        public Map<String, Document> asMap() {
            return document.asMap();
        }

        @Override
        public Instant asTimestamp() {
            return document.asTimestamp();
        }

        @Override
        public int size() {
            return document.size();
        }

        @Override
        public Document getMember(String memberName) {
            return document.getMember(memberName);
        }

        @Override
        public Set<String> getMemberNames() {
            return document.getMemberNames();
        }

        @Override
        public boolean equals(Object obj) {
            return Document.equals(this, obj);
        }

        @Override
        public int hashCode() {
            return Document.hash(this);
        }

        @Override
        public String toString() {
            return describe(schema, type(), document);
        }
    }
}
