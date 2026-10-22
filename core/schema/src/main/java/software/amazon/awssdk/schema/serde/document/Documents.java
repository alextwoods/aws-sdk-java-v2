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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.Validate;

/**
 * The protocol-agnostic {@link Document} implementations and the equality helpers they share.
 *
 * <p>Port of smithy-java {@code Documents} and the equality parts of {@code DocumentUtils}. Each implementation holds
 * the {@link Schema} it was created with, which {@link Document#serializeContents(ShapeSerializer)} passes to the
 * serializer. The factories on {@link Document} pass no schema; those documents use the prelude schema of their type,
 * resolved when first needed so that creating a document never initializes {@link PreludeSchemas}.
 */
@SdkInternalApi
final class Documents {

    private Documents() {
    }

    /**
     * Returns the coercion failure for a document that does not hold the requested type, with smithy-java's message:
     * {@code "Expected a string document, but found integer"}.
     */
    static SerializationException typeMismatch(String expected, Document actual) {
        String article = "aeiou".indexOf(expected.charAt(0)) >= 0 ? "an " : "a ";
        return new SerializationException("Expected " + article + expected + " document, but found " + actual.type());
    }

    static boolean documentEquals(Document l, Document r, int options) {
        if (isStream(l) || isStream(r)) {
            // A stream has no value to compare: it equals only a document of the same kind over the same stream.
            return l.getClass() == r.getClass() && l.asObject() == r.asObject();
        }
        boolean promote = (options & DocumentEqualsFlags.NUMBER_PROMOTION) != 0;
        ShapeType type = l.type();
        if (promote && isNumeric(type)) {
            return numberEquals(l.asNumber(), r);
        }
        if (type != r.type()) {
            return false;
        }
        switch (type) {
            case BLOB:
                return l.asBlob().equals(r.asBlob());
            case BOOLEAN:
                return l.asBoolean() == r.asBoolean();
            case STRING:
            case ENUM:
                return l.asString().equals(r.asString());
            case TIMESTAMP:
                return l.asTimestamp().equals(r.asTimestamp());
            case BYTE:
            case SHORT:
            case INTEGER:
            case INT_ENUM:
            case LONG:
                return l.asLong() == r.asLong();
            case FLOAT:
            case DOUBLE:
                return Double.compare(l.asDouble(), r.asDouble()) == 0;
            case BIG_INTEGER:
                return l.asBigInteger().equals(r.asBigInteger());
            case BIG_DECIMAL:
                return l.asBigDecimal().compareTo(r.asBigDecimal()) == 0;
            case LIST:
                return listEquals(l.asList(), r.asList(), options);
            case MAP:
            case STRUCTURE:
            case UNION:
                return mapEquals(l.asMap(), r.asMap(), options);
            case DOCUMENT:
                return Objects.equals(l.asObject(), r.asObject());
            default:
                return false;
        }
    }

    static int hash(Document document) {
        ShapeType type = document.type();
        if (isStream(document)) {
            return mix(type, System.identityHashCode(document.asObject()));
        }
        switch (type) {
            case BLOB:
                return mix(type, document.asBlob().hashCode());
            case BOOLEAN:
                return mix(type, Boolean.hashCode(document.asBoolean()));
            case STRING:
            case ENUM:
                return mix(type, document.asString().hashCode());
            case TIMESTAMP:
                return mix(type, document.asTimestamp().hashCode());
            case BYTE:
            case SHORT:
            case INTEGER:
            case INT_ENUM:
            case LONG:
                return mix(type, Long.hashCode(document.asLong()));
            case FLOAT:
            case DOUBLE:
                return mix(type, Double.hashCode(document.asDouble()));
            case BIG_INTEGER:
                return mix(type, document.asBigInteger().hashCode());
            case BIG_DECIMAL:
                return mix(type, stripTrailingZeros(document.asBigDecimal()).hashCode());
            case LIST: {
                int result = 1;
                for (Document element : document.asList()) {
                    result = 31 * result + Document.hash(element);
                }
                return mix(type, result);
            }
            case MAP:
            case STRUCTURE:
            case UNION: {
                int result = 0;
                for (Map.Entry<String, Document> entry : document.asMap().entrySet()) {
                    result += entry.getKey().hashCode() ^ Document.hash(entry.getValue());
                }
                return mix(type, result);
            }
            default:
                return type.hashCode();
        }
    }

    /**
     * The default {@link Document#serializeContents(ShapeSerializer)}, for implementations outside this class (such as
     * views over other document types): writes the value of {@link Document#type()} with its prelude schema.
     */
    static void serializeContentsByType(Document document, ShapeSerializer serializer) {
        ShapeType type = document.type();
        switch (type) {
            case BOOLEAN:
                serializer.writeBoolean(PreludeSchemas.BOOLEAN, document.asBoolean());
                break;
            case BYTE:
                serializer.writeByte(PreludeSchemas.BYTE, document.asByte());
                break;
            case SHORT:
                serializer.writeShort(PreludeSchemas.SHORT, document.asShort());
                break;
            case INTEGER:
            case INT_ENUM:
                serializer.writeInteger(PreludeSchemas.INTEGER, document.asInteger());
                break;
            case LONG:
                serializer.writeLong(PreludeSchemas.LONG, document.asLong());
                break;
            case FLOAT:
                serializer.writeFloat(PreludeSchemas.FLOAT, document.asFloat());
                break;
            case DOUBLE:
                serializer.writeDouble(PreludeSchemas.DOUBLE, document.asDouble());
                break;
            case BIG_INTEGER:
                serializer.writeBigInteger(PreludeSchemas.BIG_INTEGER, document.asBigInteger());
                break;
            case BIG_DECIMAL:
                serializer.writeBigDecimal(PreludeSchemas.BIG_DECIMAL, document.asBigDecimal());
                break;
            case STRING:
            case ENUM:
                serializer.writeString(PreludeSchemas.STRING, document.asString());
                break;
            case BLOB:
                serializer.writeBlob(PreludeSchemas.BLOB, document.asBlob());
                break;
            case TIMESTAMP:
                serializer.writeTimestamp(PreludeSchemas.TIMESTAMP, document.asTimestamp());
                break;
            case LIST:
                new ListDocument(null, document.asList()).serializeContents(serializer);
                break;
            case MAP:
                new StringMapDocument(null, document.asMap()).serializeContents(serializer);
                break;
            default:
                throw new SerializationException("A " + type + " document must implement serializeContents");
        }
    }

    /**
     * Returns the value of a structure document's member for {@link SerializableStruct#getMemberValue(Schema)}: the
     * member document converted with {@link Document#asObject()}, or null when the member is absent.
     *
     * <p>Port of smithy-java {@code DocumentUtils.getMemberValue}.
     */
    @SuppressWarnings("unchecked")
    static <T> T getMemberValue(Document container, Schema containerSchema, Schema member) {
        Schema expected = member == null ? null : containerSchema.member(member.memberName());
        if (expected == null) {
            throw SchemaUtils.notAMember(containerSchema, member);
        }
        Document value = SchemaUtils.validateSameMember(expected, member, container.getMember(member.memberName()));
        if (value == null) {
            return null;
        }
        try {
            return (T) value.asObject();
        } catch (ClassCastException e) {
            throw new ClassCastException("Unable to cast document member `" + member.id() + "` from document with "
                                         + "schema `" + containerSchema.id() + "`: " + e.getMessage());
        }
    }

    private static String describe(Schema schema, ShapeType type, Object value) {
        // Documents parsed from a model shape keep the member schemas, so a sensitive member's value is redacted.
        Object shown = schema != null && schema.hasTrait(TraitKey.SENSITIVE_TRAIT) ? "*** Sensitive Data Redacted ***"
                                                                                    : value;
        return "Document(" + type + ": " + shown + ")";
    }

    /**
     * Returns whether the document holds a data stream or an event stream, which have no value to compare or hash.
     */
    private static boolean isStream(Document document) {
        return document instanceof DataStreamDocument || document instanceof EventStreamDocument;
    }

    private static int mix(ShapeType type, int valueHash) {
        return 31 * type.ordinal() + valueHash;
    }

    private static boolean listEquals(List<Document> left, List<Document> right, int options) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            if (!Document.equals(left.get(i), right.get(i), options)) {
                return false;
            }
        }
        return true;
    }

    private static boolean mapEquals(Map<String, Document> left, Map<String, Document> right, int options) {
        if (left.size() != right.size()) {
            return false;
        }
        for (Map.Entry<String, Document> entry : left.entrySet()) {
            if (!right.containsKey(entry.getKey())
                || !Document.equals(entry.getValue(), right.get(entry.getKey()), options)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNumeric(ShapeType type) {
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

    // Emulates JLS 5.1.2 type promotion. Reports false instead of throwing when the right side is not numeric.
    private static boolean numberEquals(Number left, Document right) {
        if (!isNumeric(right.type())) {
            return false;
        }
        Number rightNumber = right.asNumber();
        if (left.equals(rightNumber)) {
            return true;
        }
        if (isBig(left) || isBig(rightNumber)) {
            if (isNonFinite(left) || isNonFinite(rightNumber)) {
                return false;
            }
            return toBigDecimal(left).compareTo(toBigDecimal(rightNumber)) == 0;
        }
        if (isFloatingPoint(left) || isFloatingPoint(rightNumber)) {
            return Double.compare(left.doubleValue(), rightNumber.doubleValue()) == 0;
        }
        return left.longValue() == rightNumber.longValue();
    }

    private static boolean isBig(Number number) {
        return number instanceof BigDecimal || number instanceof BigInteger;
    }

    private static boolean isFloatingPoint(Number number) {
        return number instanceof Double || number instanceof Float;
    }

    private static boolean isNonFinite(Number number) {
        return isFloatingPoint(number) && !Double.isFinite(number.doubleValue());
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }
        if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        }
        if (isFloatingPoint(number)) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.valueOf(number.longValue());
    }

    private static BigDecimal stripTrailingZeros(BigDecimal value) {
        // BigDecimal.stripTrailingZeros keeps the scale of zero values on some Java 8 releases.
        return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }

    private static BigDecimal floatingPointToBigDecimal(ShapeType type, double value) {
        if (!Double.isFinite(value)) {
            throw new SerializationException("Cannot convert a non-finite " + type + " document (" + value
                                             + ") to a bigDecimal");
        }
        return BigDecimal.valueOf(value);
    }

    /**
     * The schemas of list and map documents created without a schema, held in their own class so that they are built
     * (and {@link PreludeSchemas} is initialized) only when a document's contents are first serialized.
     */
    private static final class ContainerSchemas {
        static final Schema LIST = Schema.listBuilder(PreludeSchemas.DOCUMENT.id())
                                         .putMember("member", PreludeSchemas.DOCUMENT)
                                         .build();

        static final Schema STRING_MAP = Schema.mapBuilder(PreludeSchemas.DOCUMENT.id())
                                               .putMember("key", PreludeSchemas.STRING)
                                               .putMember("value", PreludeSchemas.DOCUMENT)
                                               .build();
    }

    /**
     * Base class for the implementations: holds the schema, and implements {@code equals} and {@code hashCode} with
     * {@link Document#equals(Object, Object)} and {@link Document#hash(Document)}.
     */
    abstract static class AbstractDocument implements Document {
        private final Schema schema;

        /**
         * @param schema the schema to serialize the contents with, or null to use the prelude schema of the type
         */
        AbstractDocument(Schema schema) {
            this.schema = schema;
        }

        /**
         * Returns the schema the document was created with, or the default schema of its type.
         */
        final Schema schema() {
            if (schema != null) {
                return schema;
            }
            switch (type()) {
                case LIST:
                    return ContainerSchemas.LIST;
                case MAP:
                    return ContainerSchemas.STRING_MAP;
                default:
                    return PreludeSchemas.getSchemaForType(type());
            }
        }

        final String describe(Object value) {
            return Documents.describe(schema, type(), value);
        }

        @Override
        public final boolean equals(Object obj) {
            return Document.equals(this, obj);
        }

        @Override
        public final int hashCode() {
            return Document.hash(this);
        }
    }

    static final class BooleanDocument extends AbstractDocument {
        static final BooleanDocument TRUE = new BooleanDocument(null, true);
        static final BooleanDocument FALSE = new BooleanDocument(null, false);

        private final boolean value;

        BooleanDocument(Schema schema, boolean value) {
            super(schema);
            this.value = value;
        }

        @Override
        public ShapeType type() {
            return ShapeType.BOOLEAN;
        }

        @Override
        public boolean asBoolean() {
            return value;
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            serializer.writeBoolean(schema(), value);
        }

        @Override
        public String toString() {
            return describe(value);
        }
    }

    /**
     * A byte, short, integer or long document. The value is held as a long; every value of the narrower types is
     * represented exactly, so the narrowing casts give the same results as casting from the original type.
     */
    static final class IntegralDocument extends AbstractDocument {
        private final ShapeType type;
        private final long value;

        IntegralDocument(ShapeType type, Schema schema, long value) {
            super(schema);
            this.type = type;
            this.value = value;
        }

        @Override
        public ShapeType type() {
            return type;
        }

        @Override
        public byte asByte() {
            return (byte) value;
        }

        @Override
        public short asShort() {
            return (short) value;
        }

        @Override
        public int asInteger() {
            return (int) value;
        }

        @Override
        public long asLong() {
            return value;
        }

        @Override
        public float asFloat() {
            return value;
        }

        @Override
        public double asDouble() {
            return value;
        }

        @Override
        public BigInteger asBigInteger() {
            return BigInteger.valueOf(value);
        }

        @Override
        public BigDecimal asBigDecimal() {
            return BigDecimal.valueOf(value);
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            Schema schema = schema();
            switch (type) {
                case BYTE:
                    serializer.writeByte(schema, (byte) value);
                    break;
                case SHORT:
                    serializer.writeShort(schema, (short) value);
                    break;
                case INTEGER:
                    serializer.writeInteger(schema, (int) value);
                    break;
                default:
                    serializer.writeLong(schema, value);
                    break;
            }
        }

        @Override
        public String toString() {
            return describe(value);
        }
    }

    /**
     * A float or double document. A float is held as a double; float-to-double widening is exact, so every coercion
     * gives the same result as coercing the original float.
     */
    static final class FloatingPointDocument extends AbstractDocument {
        private final ShapeType type;
        private final double value;

        FloatingPointDocument(ShapeType type, Schema schema, double value) {
            super(schema);
            this.type = type;
            this.value = value;
        }

        @Override
        public ShapeType type() {
            return type;
        }

        @Override
        public byte asByte() {
            return (byte) value;
        }

        @Override
        public short asShort() {
            return (short) value;
        }

        @Override
        public int asInteger() {
            return (int) value;
        }

        @Override
        public long asLong() {
            return (long) value;
        }

        @Override
        public float asFloat() {
            return (float) value;
        }

        @Override
        public double asDouble() {
            return value;
        }

        @Override
        public BigInteger asBigInteger() {
            return BigInteger.valueOf((long) value);
        }

        @Override
        public BigDecimal asBigDecimal() {
            return floatingPointToBigDecimal(type, value);
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            if (type == ShapeType.FLOAT) {
                serializer.writeFloat(schema(), (float) value);
            } else {
                serializer.writeDouble(schema(), value);
            }
        }

        @Override
        public String toString() {
            return describe(type == ShapeType.FLOAT ? (Object) (float) value : (Object) value);
        }
    }

    static final class BigIntegerDocument extends AbstractDocument {
        private final BigInteger value;

        BigIntegerDocument(Schema schema, BigInteger value) {
            super(schema);
            this.value = Validate.paramNotNull(value, "value");
        }

        @Override
        public ShapeType type() {
            return ShapeType.BIG_INTEGER;
        }

        @Override
        public byte asByte() {
            return value.byteValue();
        }

        @Override
        public short asShort() {
            return value.shortValue();
        }

        @Override
        public int asInteger() {
            return value.intValue();
        }

        @Override
        public long asLong() {
            return value.longValue();
        }

        @Override
        public float asFloat() {
            return value.floatValue();
        }

        @Override
        public double asDouble() {
            return value.doubleValue();
        }

        @Override
        public BigInteger asBigInteger() {
            return value;
        }

        @Override
        public BigDecimal asBigDecimal() {
            return new BigDecimal(value);
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            serializer.writeBigInteger(schema(), value);
        }

        @Override
        public String toString() {
            return describe(value);
        }
    }

    static final class BigDecimalDocument extends AbstractDocument {
        private final BigDecimal value;

        BigDecimalDocument(Schema schema, BigDecimal value) {
            super(schema);
            this.value = Validate.paramNotNull(value, "value");
        }

        @Override
        public ShapeType type() {
            return ShapeType.BIG_DECIMAL;
        }

        @Override
        public byte asByte() {
            return value.byteValue();
        }

        @Override
        public short asShort() {
            return value.shortValue();
        }

        @Override
        public int asInteger() {
            return value.intValue();
        }

        @Override
        public long asLong() {
            return value.longValue();
        }

        @Override
        public float asFloat() {
            return value.floatValue();
        }

        @Override
        public double asDouble() {
            return value.doubleValue();
        }

        @Override
        public BigInteger asBigInteger() {
            return value.toBigInteger();
        }

        @Override
        public BigDecimal asBigDecimal() {
            return value;
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            serializer.writeBigDecimal(schema(), value);
        }

        @Override
        public String toString() {
            return describe(value);
        }
    }

    static final class StringDocument extends AbstractDocument {
        private final String value;

        StringDocument(Schema schema, String value) {
            super(schema);
            this.value = Validate.paramNotNull(value, "value");
        }

        @Override
        public ShapeType type() {
            return ShapeType.STRING;
        }

        @Override
        public String asString() {
            return value;
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            serializer.writeString(schema(), value);
        }

        @Override
        public String toString() {
            return describe(value);
        }
    }

    static final class BlobDocument extends AbstractDocument {
        private final ByteBuffer value;

        BlobDocument(Schema schema, ByteBuffer value) {
            super(schema);
            this.value = Validate.paramNotNull(value, "value").duplicate();
        }

        @Override
        public ShapeType type() {
            return ShapeType.BLOB;
        }

        @Override
        public ByteBuffer asBlob() {
            return value.duplicate();
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            serializer.writeBlob(schema(), value.duplicate());
        }

        @Override
        public String toString() {
            return describe(value.remaining() + " bytes");
        }
    }

    static final class TimestampDocument extends AbstractDocument {
        private final Instant value;

        TimestampDocument(Schema schema, Instant value) {
            super(schema);
            this.value = Validate.paramNotNull(value, "value");
        }

        @Override
        public ShapeType type() {
            return ShapeType.TIMESTAMP;
        }

        @Override
        public Instant asTimestamp() {
            return value;
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            serializer.writeTimestamp(schema(), value);
        }

        @Override
        public String toString() {
            return describe(value);
        }
    }

    static final class ListDocument extends AbstractDocument {
        private final List<Document> values;

        ListDocument(Schema schema, List<Document> values) {
            super(schema);
            this.values = Collections.unmodifiableList(Validate.paramNotNull(values, "values"));
        }

        @Override
        public ShapeType type() {
            return ShapeType.LIST;
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
        public void serializeContents(ShapeSerializer serializer) {
            Schema schema = schema();
            serializer.writeList(schema, values, values.size(), (elements, s) -> {
                for (Document element : elements) {
                    if (element == null) {
                        s.writeNull(schema.listMember());
                    } else {
                        element.serialize(s);
                    }
                }
            });
        }

        @Override
        public String toString() {
            return describe(values);
        }
    }

    static final class StringMapDocument extends AbstractDocument {
        private final Map<String, Document> members;

        StringMapDocument(Schema schema, Map<String, Document> members) {
            super(schema);
            this.members = Collections.unmodifiableMap(Validate.paramNotNull(members, "members"));
        }

        @Override
        public ShapeType type() {
            return ShapeType.MAP;
        }

        @Override
        public Map<String, Document> asMap() {
            return members;
        }

        @Override
        public int size() {
            return members.size();
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
        public void serializeContents(ShapeSerializer serializer) {
            Schema schema = schema();
            serializer.writeMap(schema, members, members.size(), (entries, s) -> {
                Schema key = schema.mapKeyMember();
                for (Map.Entry<String, Document> entry : entries.entrySet()) {
                    s.writeEntry(key, entry.getKey(), entry.getValue(), (value, valueSerializer) -> {
                        if (value == null) {
                            valueSerializer.writeNull(schema.mapValueMember());
                        } else {
                            value.serialize(valueSerializer);
                        }
                    });
                }
            });
        }

        @Override
        public String toString() {
            return describe(members);
        }
    }

    /**
     * A structure or union document holding its members, keyed by Smithy member name, in the order they were written.
     * {@link DocumentParser} creates one for each union; structures use {@link LazyStructure}, which creates one
     * when its members are first read.
     */
    static final class StructureDocument extends AbstractDocument implements SerializableStruct {
        private final Map<String, Document> members;

        StructureDocument(Schema schema, Map<String, Document> members) {
            super(Validate.paramNotNull(schema, "schema"));
            this.members = Collections.unmodifiableMap(Validate.paramNotNull(members, "members"));
        }

        @Override
        public ShapeType type() {
            return ShapeType.STRUCTURE;
        }

        @Override
        public Map<String, Document> asMap() {
            return members;
        }

        @Override
        public int size() {
            return members.size();
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
        public void serialize(ShapeSerializer serializer) {
            // Written as a document, like Document.serialize; SerializableStruct.serialize would call writeStruct.
            serializer.writeDocument(PreludeSchemas.DOCUMENT, this);
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            serializer.writeStruct(schema(), this);
        }

        @Override
        public Schema sdkSchema() {
            return schema();
        }

        /**
         * Writes each member's contents. Members were parsed with their member schemas, so they are written with
         * those; a member that targets a document is written with {@code writeDocument} and its member schema.
         */
        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            for (Map.Entry<String, Document> entry : members.entrySet()) {
                Document value = entry.getValue();
                if (value == null) {
                    continue;
                }
                Schema member = schema().member(entry.getKey());
                if (member != null && member.type() == ShapeType.DOCUMENT) {
                    serializer.writeDocument(member, value);
                } else {
                    value.serializeContents(serializer);
                }
            }
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return Documents.getMemberValue(this, schema(), member);
        }

        @Override
        public String toString() {
            return describe(members);
        }
    }

    /**
     * A structure document over a {@link SerializableStruct}. The struct's members are parsed into a
     * {@link StructureDocument} only when they are first read; serializing the document writes the struct itself.
     */
    static final class LazyStructure extends AbstractDocument implements SerializableStruct {
        private final SerializableStruct struct;
        private volatile StructureDocument parsed;

        LazyStructure(Schema schema, SerializableStruct struct) {
            super(Validate.paramNotNull(schema, "schema"));
            this.struct = Validate.paramNotNull(struct, "struct");
        }

        private StructureDocument parsed() {
            StructureDocument result = parsed;
            if (result == null) {
                // A race parses the struct twice and keeps either result; both hold the same members.
                result = new StructureDocument(schema(), DocumentParser.parseMembers(struct));
                parsed = result;
            }
            return result;
        }

        @Override
        public ShapeType type() {
            return ShapeType.STRUCTURE;
        }

        /**
         * Returns the ShapeId of the structure's shape. For a nested structure, which was written with its member
         * schema, this is the member's target, not the member ID.
         */
        @Override
        public ShapeId getDiscriminator() {
            Schema schema = schema();
            return schema.isMember() ? schema.memberTarget().id() : schema.id();
        }

        @Override
        public Map<String, Document> asMap() {
            return parsed().asMap();
        }

        @Override
        public int size() {
            return parsed().size();
        }

        @Override
        public Document getMember(String memberName) {
            return parsed().getMember(memberName);
        }

        @Override
        public Set<String> getMemberNames() {
            return parsed().getMemberNames();
        }

        @Override
        public void serialize(ShapeSerializer serializer) {
            // Written as a document, like Document.serialize; SerializableStruct.serialize would call writeStruct.
            serializer.writeDocument(PreludeSchemas.DOCUMENT, this);
        }

        @Override
        public void serializeContents(ShapeSerializer serializer) {
            serializer.writeStruct(schema(), struct);
        }

        @Override
        public Schema sdkSchema() {
            return schema();
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            struct.serializeMembers(serializer);
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return struct.getMemberValue(member);
        }

        @Override
        public String toString() {
            return parsed().toString();
        }
    }
}
