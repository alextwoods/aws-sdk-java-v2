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

package software.amazon.awssdk.codecs.cbor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.serde.document.DocumentDeserializer;

/**
 * The CBOR Protocol_Documents: the {@link Document}s the CBOR codec reads with
 * {@link ShapeDeserializer#readDocument()}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.cbor.CborDocuments}, which has only a map document; here
 * every CBOR value the codec decodes has a document that holds the {@link CborSettings} of the codec that read it.
 * CBOR has native blobs and timestamps, so no value is coerced to another type: a byte string is a {@code blob}
 * document, a tag 1 value is a {@code timestamp} document, an integer is a {@code long} document, a float is a
 * {@code float} or {@code double} document by its encoded width, a bignum is a {@code bigInteger} document and a
 * decimal fraction is a {@code bigDecimal} document. A map document reads its {@code __type} discriminator with the
 * settings' default namespace, and {@link Document#asShape} handles unknown union members as the CBOR codec does.
 *
 * <p>Equality and hash codes are those of {@link Document#equals(Object, Object)} and {@link Document#hash(Document)}:
 * settings are not compared. Lists, maps and buffers passed to the factories are wrapped, not copied, and must not be
 * modified afterwards.
 */
@SdkProtectedApi
public final class CborDocuments {

    private CborDocuments() {
    }

    /**
     * Creates a CBOR boolean document.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(boolean value, CborSettings settings) {
        return new BooleanDocument(value, settings);
    }

    /**
     * Creates a CBOR integer document, of type {@code long}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(long value, CborSettings settings) {
        return new LongDocument(value, settings);
    }

    /**
     * Creates a CBOR single-precision (or half-precision) float document, of type {@code float}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(float value, CborSettings settings) {
        return new FloatingPointDocument(ShapeType.FLOAT, value, settings);
    }

    /**
     * Creates a CBOR double-precision float document, of type {@code double}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(double value, CborSettings settings) {
        return new FloatingPointDocument(ShapeType.DOUBLE, value, settings);
    }

    /**
     * Creates a CBOR bignum document, of type {@code bigInteger}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(BigInteger value, CborSettings settings) {
        return new BigIntegerDocument(requireNonNull(value, "value"), settings);
    }

    /**
     * Creates a CBOR decimal fraction document, of type {@code bigDecimal}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(BigDecimal value, CborSettings settings) {
        return new BigDecimalDocument(requireNonNull(value, "value"), settings);
    }

    /**
     * Creates a CBOR text string document.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(String value, CborSettings settings) {
        return new StringDocument(requireNonNull(value, "value"), settings);
    }

    /**
     * Creates a CBOR byte string document over the remaining bytes of a buffer, of type {@code blob}.
     *
     * @param value the bytes
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(ByteBuffer value, CborSettings settings) {
        return new BlobDocument(requireNonNull(value, "value"), settings);
    }

    /**
     * Creates a CBOR tag 1 (epoch-based date/time) document, of type {@code timestamp}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(Instant value, CborSettings settings) {
        return new TimestampDocument(requireNonNull(value, "value"), settings);
    }

    /**
     * Creates a CBOR array document. Elements may be null.
     *
     * @param values the elements
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(List<Document> values, CborSettings settings) {
        return new ListDocument(requireNonNull(values, "values"), settings);
    }

    /**
     * Creates a CBOR map document with text string keys. The map's iteration order is the document's member order.
     * Values may be null.
     *
     * @param values the members
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(Map<String, Document> values, CborSettings settings) {
        return new MapDocument(requireNonNull(values, "values"), settings);
    }

    private static <T> T requireNonNull(T value, String name) {
        return Objects.requireNonNull(value, name + " must not be null");
    }

    /**
     * Base class of the CBOR documents: holds the settings, creates the settings-aware deserializer, and implements
     * {@code equals} and {@code hashCode} with the {@link Document} rules. Contents are serialized with the default
     * {@link Document#serializeContents}, which writes the value of {@link #type()} with its prelude schema.
     */
    abstract static class CborDocument implements Document {
        private final CborSettings settings;

        CborDocument(CborSettings settings) {
            this.settings = requireNonNull(settings, "settings");
        }

        /**
         * Returns the settings of the codec that read the document.
         */
        final CborSettings settings() {
            return settings;
        }

        @Override
        public final ShapeDeserializer createDeserializer() {
            return new CborDocumentDeserializer(settings, this);
        }

        @Override
        public final boolean equals(Object obj) {
            return Document.equals(this, obj);
        }

        @Override
        public final int hashCode() {
            return Document.hash(this);
        }

        final String describe(Object value) {
            return "CborDocument(" + type() + ": " + value + ")";
        }
    }

    static final class BooleanDocument extends CborDocument {
        private final boolean value;

        BooleanDocument(boolean value, CborSettings settings) {
            super(settings);
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
        public String toString() {
            return describe(value);
        }
    }

    static final class LongDocument extends CborDocument {
        private final long value;

        LongDocument(long value, CborSettings settings) {
            super(settings);
            this.value = value;
        }

        @Override
        public ShapeType type() {
            return ShapeType.LONG;
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
        public String toString() {
            return describe(value);
        }
    }

    /**
     * A float or double, held as a double (float-to-double widening is exact).
     */
    static final class FloatingPointDocument extends CborDocument {
        private final ShapeType type;
        private final double value;

        FloatingPointDocument(ShapeType type, double value, CborSettings settings) {
            super(settings);
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
            if (!Double.isFinite(value)) {
                throw new SerializationException("Cannot convert a non-finite " + type + " document (" + value
                                                 + ") to a bigDecimal");
            }
            return BigDecimal.valueOf(value);
        }

        @Override
        public String toString() {
            return describe(type == ShapeType.FLOAT ? (Object) (float) value : (Object) value);
        }
    }

    static final class BigIntegerDocument extends CborDocument {
        private final BigInteger value;

        BigIntegerDocument(BigInteger value, CborSettings settings) {
            super(settings);
            this.value = value;
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
        public String toString() {
            return describe(value);
        }
    }

    static final class BigDecimalDocument extends CborDocument {
        private final BigDecimal value;

        BigDecimalDocument(BigDecimal value, CborSettings settings) {
            super(settings);
            this.value = value;
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
        public String toString() {
            return describe(value);
        }
    }

    static final class StringDocument extends CborDocument {
        private final String value;

        StringDocument(String value, CborSettings settings) {
            super(settings);
            this.value = value;
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
        public String toString() {
            return describe(value);
        }
    }

    static final class BlobDocument extends CborDocument {
        private final ByteBuffer value;

        BlobDocument(ByteBuffer value, CborSettings settings) {
            super(settings);
            this.value = value.duplicate();
        }

        @Override
        public ShapeType type() {
            return ShapeType.BLOB;
        }

        /**
         * Returns the bytes. The buffer shares its content with the document but has its own position and limit.
         */
        @Override
        public ByteBuffer asBlob() {
            return value.duplicate();
        }

        @Override
        public String toString() {
            return describe(value.remaining() + " bytes");
        }
    }

    static final class TimestampDocument extends CborDocument {
        private final Instant value;

        TimestampDocument(Instant value, CborSettings settings) {
            super(settings);
            this.value = value;
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
        public String toString() {
            return describe(value);
        }
    }

    static final class ListDocument extends CborDocument {
        private final List<Document> values;

        ListDocument(List<Document> values, CborSettings settings) {
            super(settings);
            this.values = Collections.unmodifiableList(values);
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
        public String toString() {
            return describe(values);
        }
    }

    static final class MapDocument extends CborDocument {
        private static final Object NO_DISCRIMINATOR = new Object();

        private final Map<String, Document> values;
        // The parsed discriminator, NO_DISCRIMINATOR when there is none, or null until first read.
        private volatile Object discriminator;

        MapDocument(Map<String, Document> values, CborSettings settings) {
            super(settings);
            this.values = Collections.unmodifiableMap(values);
        }

        @Override
        public ShapeType type() {
            return ShapeType.MAP;
        }

        /**
         * Returns the ShapeId in the {@code __type} member, resolving a relative shape name against the default
         * namespace of the settings. The result is parsed on the first call and cached.
         *
         * @return the discriminator, or null if there is no text string {@code __type} member
         * @throws software.amazon.awssdk.schema.serde.document.DiscriminatorException if {@code __type} is not a valid
         *     ShapeId, or is relative and the settings have no default namespace
         */
        @Override
        public ShapeId getDiscriminator() {
            Object result = discriminator;
            if (result == null) {
                // A race parses twice; both parses give the same ShapeId.
                Document member = values.get("__type");
                String text = member != null && member.type() == ShapeType.STRING ? member.asString() : null;
                ShapeId parsed = DocumentDeserializer.parseDiscriminator(text, settings().defaultNamespace());
                result = parsed == null ? NO_DISCRIMINATOR : parsed;
                discriminator = result;
            }
            return result == NO_DISCRIMINATOR ? null : (ShapeId) result;
        }

        @Override
        public Map<String, Document> asMap() {
            return values;
        }

        @Override
        public Document getMember(String memberName) {
            return values.get(memberName);
        }

        @Override
        public Set<String> getMemberNames() {
            return values.keySet();
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public String toString() {
            return describe(values);
        }
    }

    /**
     * A {@link DocumentDeserializer} that reads CBOR documents with the CBOR codec's rules for unknown members:
     * reported for structures; for unions, {@code __type} is skipped and other members are reported, or rejected when
     * the settings forbid unknown union members.
     */
    private static final class CborDocumentDeserializer extends DocumentDeserializer {
        private final CborSettings settings;

        CborDocumentDeserializer(CborSettings settings, Document value) {
            super(value);
            this.settings = settings;
        }

        @Override
        protected DocumentDeserializer deserializer(Document value) {
            return new CborDocumentDeserializer(settings, value);
        }

        @Override
        public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
            Document value = readDocument();
            Schema container = schema.isMember() ? schema.memberTarget() : schema;
            for (String name : value.getMemberNames()) {
                Schema member = container.member(name);
                if (member == null) {
                    unknownMember(container, state, consumer, name);
                    continue;
                }
                Document memberValue = value.getMember(name);
                if (memberValue != null) {
                    consumer.accept(state, member, deserializer(memberValue));
                }
            }
        }

        private <T> void unknownMember(Schema container, T state, StructMemberConsumer<T> consumer, String name) {
            if (container.type() == ShapeType.UNION) {
                if ("__type".equals(name)) {
                    return;
                }
                if (settings.forbidUnknownUnionMembers()) {
                    throw new SerializationException("Unknown member " + name + " encountered for union "
                                                     + container.id());
                }
            }
            consumer.unknownMember(state, name);
        }
    }
}
