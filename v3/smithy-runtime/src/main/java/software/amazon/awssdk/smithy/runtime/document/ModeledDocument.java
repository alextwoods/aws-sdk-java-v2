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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.document.DocumentEqualsFlags;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * An immutable Smithy Java {@link Document} node that retains the exact modeled schema of its value (Requirements 6.1
 * and 6.2).
 *
 * <p>Smithy Java's own typed documents report enums as strings, intEnums as integers, sets as lists and unions as
 * structures, and give nested unions no discriminator. A modeled document instead reports the exact modeled kind
 * from {@link #type()} ({@code ENUM}, {@code INT_ENUM}, {@code SET}, {@code UNION}, ...), and every node's
 * {@link #discriminator()} is the exact absolute shape ID of its modeled shape (the target shape for a member).
 * Nodes are created by {@link SchemaDocuments#of} from a serializable value, or by {@link SchemaDocuments#coerce}
 * from any document and a schema.
 *
 * <p>Values are immutable: blobs are copied on creation and exposed as read-only buffers, and lists and maps are
 * unmodifiable. List order and map/structure member order are retained. A member that targets
 * {@code smithy.api#Document} keeps the original document value unchanged.
 *
 * <p>Serializing a modeled document with a codec writes it exactly as its modeled value would be written: members are
 * written with their member schemas (so member traits such as {@code jsonName} and {@code timestampFormat} apply),
 * and a structure written as a standalone document is written with its target schema.
 *
 * <p><b>Equality.</b> {@link #equals} is value equality between modeled documents: the same node class, the same
 * shape ID, and equal values (numbers of one kind by exact value, blobs by bytes, lists in order, maps and
 * structures regardless of order; the original documents of {@code smithy.api#Document} members by Smithy Java's
 * implementation-independent {@code Document.equals} with number promotion). A modeled document is never equal to a
 * document of another implementation, which
 * keeps {@code equals} symmetric; compare documents of different origins through a schema with
 * {@link SchemaDocuments#equivalent}.
 *
 * <p>Accessors follow the Smithy Java {@link Document} contract, including its lenient numeric casts; use
 * {@link SchemaDocuments#coerce} or {@link SchemaDocuments#toShape} for exact, schema-checked conversion.
 */
public abstract sealed class ModeledDocument implements Document
    permits ModeledDocument.Scalar, ModeledDocument.Sequence, ModeledDocument.StringMap, ModeledDocument.Aggregate {

    private static final String REDACTED = "*** Sensitive Data Redacted ***";

    /** The schema the value was written with: a member schema when the value is a member, element or entry. */
    final Schema written;
    /** The modeled shape of the value: the target of {@link #written} when that is a member. */
    final Schema target;

    ModeledDocument(Schema written) {
        this.written = Objects.requireNonNull(written, "schema");
        this.target = written.isMember() ? written.memberTarget() : written;
    }

    /** The exact modeled schema of the value (the target shape's schema when the value is a member). */
    public final Schema schema() {
        return target;
    }

    /** The exact absolute shape ID of the value's modeled shape. */
    public final ShapeId shapeId() {
        return target.id();
    }

    /** The exact modeled kind, for example {@code ENUM}, {@code INT_ENUM}, {@code SET} or {@code UNION}. */
    @Override
    public final ShapeType type() {
        return target.type();
    }

    /** The exact absolute shape ID of the value's modeled shape; never {@code null}. */
    @Override
    public final ShapeId discriminator() {
        return target.id();
    }

    @Override
    public void serializeContents(ShapeSerializer serializer) {
        write(serializer, written);
    }

    /** Writes the value with {@code schema}, the member schema of the enclosing aggregate when there is one. */
    abstract void write(ShapeSerializer serializer, Schema schema);

    abstract boolean valueEquals(ModeledDocument other);

    abstract int valueHash();

    abstract String render();

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ModeledDocument other) || !sameNodeClass(other)) {
            return false;
        }
        return target.id().equals(other.target.id()) && valueEquals(other);
    }

    private boolean sameNodeClass(ModeledDocument other) {
        return this instanceof Scalar && other instanceof Scalar
               || this instanceof Sequence && other instanceof Sequence
               || this instanceof StringMap && other instanceof StringMap
               || this instanceof Aggregate && other instanceof Aggregate;
    }

    @Override
    public final int hashCode() {
        return 31 * target.id().hashCode() + valueHash();
    }

    @Override
    public final String toString() {
        String value = written.hasTrait(TraitKey.SENSITIVE_TRAIT) || target.hasTrait(TraitKey.SENSITIVE_TRAIT)
                       ? REDACTED : render();
        return target.type() + "Document[" + target.id() + "]" + value;
    }

    /** Writes one member, element or entry value with its member schema. */
    static void writeChild(ShapeSerializer serializer, Schema member, Document child) {
        if (child == null) {
            serializer.writeNull(member);
        } else if (member.type() != ShapeType.DOCUMENT && child instanceof ModeledDocument modeled) {
            modeled.write(serializer, member);
        } else {
            serializer.writeDocument(member, child);
        }
    }

    /**
     * Compares two members, elements or entries. Modeled documents compare with {@link #equals}; the original
     * documents of {@code smithy.api#Document} members compare by Smithy Java's implementation-independent
     * {@link Document#equals(Object, Object, int)} with number promotion; a modeled document never equals another
     * implementation's document.
     */
    static boolean childEquals(Document left, Document right) {
        if (left == right) {
            return true;
        }
        if (left instanceof ModeledDocument || right instanceof ModeledDocument) {
            return left != null && left.equals(right);
        }
        return Document.equals(left, right, DocumentEqualsFlags.NUMBER_PROMOTION);
    }

    /** A hash consistent with {@link #childEquals}: other implementations' documents all hash to zero. */
    static int childHash(Document child) {
        return child instanceof ModeledDocument ? child.hashCode() : 0;
    }

    static boolean childrenEqual(Map<String, Document> left, Map<String, Document> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (Map.Entry<String, Document> entry : left.entrySet()) {
            if (!right.containsKey(entry.getKey()) || !childEquals(entry.getValue(), right.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    static int childrenHash(Map<String, Document> children) {
        int hash = 0;
        for (Map.Entry<String, Document> entry : children.entrySet()) {
            hash += entry.getKey().hashCode() ^ childHash(entry.getValue());
        }
        return hash;
    }

    private static String renderChild(Document child) {
        return child == null ? "null" : child.toString();
    }

    /** A boolean, number, string, enum, intEnum, blob or timestamp. */
    static final class Scalar extends ModeledDocument {
        /** Boolean, the boxed number of the kind, BigInteger, BigDecimal, String, byte[] (a copy) or Instant. */
        private final Object value;

        Scalar(Schema written, Object value) {
            super(written);
            this.value = Objects.requireNonNull(value, "value");
        }

        static Scalar blob(Schema written, ByteBuffer buffer) {
            ByteBuffer view = buffer.duplicate();
            byte[] copy = new byte[view.remaining()];
            view.get(copy);
            return new Scalar(written, copy);
        }

        @Override
        void write(ShapeSerializer serializer, Schema schema) {
            switch (target.type()) {
                case BOOLEAN -> serializer.writeBoolean(schema, (Boolean) value);
                case BYTE -> serializer.writeByte(schema, (Byte) value);
                case SHORT -> serializer.writeShort(schema, (Short) value);
                case INTEGER, INT_ENUM -> serializer.writeInteger(schema, (Integer) value);
                case LONG -> serializer.writeLong(schema, (Long) value);
                case FLOAT -> serializer.writeFloat(schema, (Float) value);
                case DOUBLE -> serializer.writeDouble(schema, (Double) value);
                case BIG_INTEGER -> serializer.writeBigInteger(schema, (BigInteger) value);
                case BIG_DECIMAL -> serializer.writeBigDecimal(schema, (BigDecimal) value);
                case STRING, ENUM -> serializer.writeString(schema, (String) value);
                case BLOB -> serializer.writeBlob(schema, asBlob());
                case TIMESTAMP -> serializer.writeTimestamp(schema, (Instant) value);
                default -> throw new IllegalStateException("Not a scalar kind: " + target.type());
            }
        }

        private Number number() {
            if (value instanceof Number number) {
                return number;
            }
            throw mismatch("a number");
        }

        private SerializationException mismatch(String expected) {
            return new SerializationException("Expected " + expected + " document, but found " + type());
        }

        @Override
        public boolean asBoolean() {
            if (value instanceof Boolean b) {
                return b;
            }
            throw mismatch("a boolean");
        }

        @Override
        public byte asByte() {
            return number().byteValue();
        }

        @Override
        public short asShort() {
            return number().shortValue();
        }

        @Override
        public int asInteger() {
            return number().intValue();
        }

        @Override
        public long asLong() {
            return number().longValue();
        }

        @Override
        public float asFloat() {
            return number().floatValue();
        }

        @Override
        public double asDouble() {
            return number().doubleValue();
        }

        @Override
        public BigInteger asBigInteger() {
            Number number = number();
            if (number instanceof BigInteger bigInteger) {
                return bigInteger;
            }
            return asBigDecimal().toBigInteger();
        }

        @Override
        public BigDecimal asBigDecimal() {
            return switch (number()) {
                case BigDecimal bigDecimal -> bigDecimal;
                case BigInteger bigInteger -> new BigDecimal(bigInteger);
                case Float f when Float.isFinite(f) -> new BigDecimal(Float.toString(f));
                case Double d when Double.isFinite(d) -> BigDecimal.valueOf(d);
                case Float f -> throw new SerializationException("A non-finite float has no bigDecimal value");
                case Double d -> throw new SerializationException("A non-finite double has no bigDecimal value");
                case Number n -> BigDecimal.valueOf(n.longValue());
            };
        }

        @Override
        public Number asNumber() {
            return number();
        }

        @Override
        public String asString() {
            if (value instanceof String s) {
                return s;
            }
            throw mismatch("a string");
        }

        @Override
        public ByteBuffer asBlob() {
            if (value instanceof byte[] bytes) {
                return ByteBuffer.wrap(bytes).asReadOnlyBuffer();
            }
            throw mismatch("a blob");
        }

        @Override
        public Instant asTimestamp() {
            if (value instanceof Instant instant) {
                return instant;
            }
            throw mismatch("a timestamp");
        }

        @Override
        boolean valueEquals(ModeledDocument other) {
            Object that = ((Scalar) other).value;
            if (value instanceof BigDecimal left && that instanceof BigDecimal right) {
                return left.compareTo(right) == 0;
            }
            if (value instanceof byte[] left && that instanceof byte[] right) {
                return Arrays.equals(left, right);
            }
            return value.equals(that);
        }

        @Override
        int valueHash() {
            if (value instanceof BigDecimal bigDecimal) {
                return bigDecimal.signum() == 0 ? 0 : bigDecimal.stripTrailingZeros().hashCode();
            }
            if (value instanceof byte[] bytes) {
                return Arrays.hashCode(bytes);
            }
            return value.hashCode();
        }

        @Override
        String render() {
            if (value instanceof byte[] bytes) {
                return "(" + bytes.length + " bytes)";
            }
            return "(" + value + ")";
        }
    }

    /** A list or set; elements are modeled documents, original documents (document members) or null (sparse). */
    static final class Sequence extends ModeledDocument {
        private final List<Document> values;

        Sequence(Schema written, List<Document> values) {
            super(written);
            this.values = Collections.unmodifiableList(new ArrayList<>(values));
        }

        @Override
        void write(ShapeSerializer serializer, Schema schema) {
            serializer.writeList(schema, values, values.size(), (elements, elementSerializer) -> {
                Schema member = schema.listMember();
                for (Document element : elements) {
                    writeChild(elementSerializer, member, element);
                }
            });
        }

        @Override
        public List<Document> asList() {
            return values;
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        boolean valueEquals(ModeledDocument other) {
            List<Document> that = ((Sequence) other).values;
            if (values.size() != that.size()) {
                return false;
            }
            for (int i = 0; i < values.size(); i++) {
                if (!childEquals(values.get(i), that.get(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        int valueHash() {
            int hash = 1;
            for (Document value : values) {
                hash = 31 * hash + childHash(value);
            }
            return hash;
        }

        @Override
        String render() {
            StringBuilder result = new StringBuilder("[");
            for (int i = 0; i < values.size(); i++) {
                result.append(i == 0 ? "" : ", ").append(renderChild(values.get(i)));
            }
            return result.append(']').toString();
        }
    }

    /** A map; values are modeled documents, original documents (document values) or null (sparse). */
    static final class StringMap extends ModeledDocument {
        private final Map<String, Document> entries;

        StringMap(Schema written, Map<String, Document> entries) {
            super(written);
            this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
        }

        @Override
        void write(ShapeSerializer serializer, Schema schema) {
            serializer.writeMap(schema, entries, entries.size(), (map, mapSerializer) -> {
                Schema key = schema.mapKeyMember();
                Schema value = schema.mapValueMember();
                for (Map.Entry<String, Document> entry : map.entrySet()) {
                    mapSerializer.writeEntry(key, entry.getKey(), entry.getValue(),
                                             (document, valueSerializer) -> writeChild(valueSerializer, value, document));
                }
            });
        }

        @Override
        public Map<String, Document> asStringMap() {
            return entries;
        }

        @Override
        public Document getMember(String memberName) {
            return entries.get(memberName);
        }

        @Override
        public Set<String> getMemberNames() {
            return entries.keySet();
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        boolean valueEquals(ModeledDocument other) {
            return childrenEqual(entries, ((StringMap) other).entries);
        }

        @Override
        int valueHash() {
            return childrenHash(entries);
        }

        @Override
        String render() {
            return renderEntries(entries);
        }
    }

    /** A structure or union with its present members, in schema member order. */
    static final class Aggregate extends ModeledDocument implements SerializableStruct {
        private final Map<String, Document> members;

        Aggregate(Schema written, Map<String, Document> members) {
            super(written);
            this.members = Collections.unmodifiableMap(new LinkedHashMap<>(members));
        }

        /** A structure written as a standalone document uses its target schema, so codecs name the target shape. */
        @Override
        public void serializeContents(ShapeSerializer serializer) {
            write(serializer, target);
        }

        @Override
        void write(ShapeSerializer serializer, Schema schema) {
            serializer.writeStruct(schema, this);
        }

        @Override
        public void serialize(ShapeSerializer serializer) {
            // Like every document, a structure document is written as a document (de-conflicting SerializableStruct).
            serializer.writeDocument(PreludeSchemas.DOCUMENT, this);
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            for (Map.Entry<String, Document> member : members.entrySet()) {
                writeChild(serializer, target.member(member.getKey()), member.getValue());
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            Schema own = member == null ? null : target.member(member.memberName());
            if (own == null || !own.id().equals(member.id())) {
                throw new IllegalArgumentException("Attempted to get non-existent member of " + target.id() + ": "
                                                   + (member == null ? null : member.id()));
            }
            Document value = members.get(member.memberName());
            return value == null ? null : (T) value.asObject();
        }

        @Override
        public Map<String, Document> asStringMap() {
            return members;
        }

        @Override
        public Document getMember(String memberName) {
            return members.get(memberName);
        }

        @Override
        public Set<String> getMemberNames() {
            return members.keySet();
        }

        @Override
        boolean valueEquals(ModeledDocument other) {
            return childrenEqual(members, ((Aggregate) other).members);
        }

        @Override
        int valueHash() {
            return childrenHash(members);
        }

        @Override
        String render() {
            return renderEntries(members);
        }
    }

    private static String renderEntries(Map<String, Document> entries) {
        StringBuilder result = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Document> entry : entries.entrySet()) {
            result.append(first ? "" : ", ").append(entry.getKey()).append('=').append(renderChild(entry.getValue()));
            first = false;
        }
        return result.append('}').toString();
    }
}
