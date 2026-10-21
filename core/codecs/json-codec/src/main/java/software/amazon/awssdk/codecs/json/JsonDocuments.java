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

package software.amazon.awssdk.codecs.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.schema.MemberLookup;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.serde.document.DocumentDeserializer;

/**
 * The JSON Protocol_Documents: the {@link Document}s the JSON codec reads with
 * {@link ShapeDeserializer#readDocument()}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.JsonDocuments}. Each document holds the
 * {@link JsonSettings} of the codec that read it, and applies them where JSON has no native type:
 * <ul>
 *     <li>{@link Document#asBlob()} on a string document base64-decodes the string;</li>
 *     <li>{@link Document#asTimestamp()} on a string or number document parses the value with the settings' default
 *     timestamp format;</li>
 *     <li>{@link Document#getDiscriminator()} on an object document reads its {@code __type} member, resolving a
 *     relative shape name against the settings' default namespace;</li>
 *     <li>{@link Document#asShape} matches members by {@code jsonName} when the settings use it, and reads timestamp
 *     members with their {@code timestampFormat} trait when the settings use it.</li>
 * </ul>
 * Numeric coercions follow the {@link Document} rules (JLS widening and narrowing). Equality and hash codes are those of
 * {@link Document#equals(Object, Object)} and {@link Document#hash(Document)}: settings are not compared, so a JSON
 * document equals any document of the same type and value.
 *
 * <p>Lists and maps passed to the factories are wrapped, not copied, and must not be modified afterwards.
 */
@SdkProtectedApi
public final class JsonDocuments {

    private JsonDocuments() {
    }

    /**
     * Creates a JSON string document.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(String value, JsonSettings settings) {
        return new StringDocument(Objects.requireNonNull(value, "value must not be null"), settings);
    }

    /**
     * Creates a JSON boolean document.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(boolean value, JsonSettings settings) {
        return new BooleanDocument(value, settings);
    }

    /**
     * Creates a JSON number document of type {@code integer}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(int value, JsonSettings settings) {
        return new IntegralDocument(ShapeType.INTEGER, value, settings);
    }

    /**
     * Creates a JSON number document of type {@code long}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(long value, JsonSettings settings) {
        return new IntegralDocument(ShapeType.LONG, value, settings);
    }

    /**
     * Creates a JSON number document of type {@code double}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(double value, JsonSettings settings) {
        return new FloatingPointDocument(ShapeType.DOUBLE, value, settings);
    }

    /**
     * Creates a JSON number document of type {@code bigInteger}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(BigInteger value, JsonSettings settings) {
        return new BigIntegerDocument(Objects.requireNonNull(value, "value must not be null"), settings);
    }

    /**
     * Creates a JSON number document of type {@code bigDecimal}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(BigDecimal value, JsonSettings settings) {
        return new BigDecimalDocument(Objects.requireNonNull(value, "value must not be null"), settings);
    }

    /**
     * Creates a JSON number document whose type matches the class of {@code value}: {@link Byte}, {@link Short},
     * {@link Integer}, {@link Long}, {@link Float}, {@link Double}, {@link BigInteger} or {@link BigDecimal}.
     *
     * @param value the value
     * @param settings the settings of the codec that read the value
     * @return the document
     * @throws IllegalArgumentException if {@code value} is another {@link Number} class
     */
    public static Document of(Number value, JsonSettings settings) {
        Objects.requireNonNull(value, "value must not be null");
        if (value instanceof Integer) {
            return of(value.intValue(), settings);
        } else if (value instanceof Long) {
            return of(value.longValue(), settings);
        } else if (value instanceof Double) {
            return of(value.doubleValue(), settings);
        } else if (value instanceof BigInteger) {
            return of((BigInteger) value, settings);
        } else if (value instanceof BigDecimal) {
            return of((BigDecimal) value, settings);
        } else if (value instanceof Byte) {
            return new IntegralDocument(ShapeType.BYTE, value.byteValue(), settings);
        } else if (value instanceof Short) {
            return new IntegralDocument(ShapeType.SHORT, value.shortValue(), settings);
        } else if (value instanceof Float) {
            return new FloatingPointDocument(ShapeType.FLOAT, value.floatValue(), settings);
        }
        throw new IllegalArgumentException("Unsupported Number: " + value.getClass());
    }

    /**
     * Creates a JSON array document. Elements may be null.
     *
     * @param values the elements
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(List<Document> values, JsonSettings settings) {
        return new ListDocument(Objects.requireNonNull(values, "values must not be null"), settings);
    }

    /**
     * Creates a JSON object document. The map's iteration order is the document's member order. Values may be null.
     *
     * @param values the members
     * @param settings the settings of the codec that read the value
     * @return the document
     */
    public static Document of(Map<String, Document> values, JsonSettings settings) {
        return new MapDocument(Objects.requireNonNull(values, "values must not be null"), settings);
    }

    /**
     * Reads a JSON value as a timestamp with {@code format}: a string with a string format, or a number with
     * {@code epoch-seconds}. A number keeps its exact value, to the nanosecond.
     */
    static Instant readTimestamp(Document value, TimestampFormatter format) {
        switch (value.type()) {
            case STRING: {
                String text = value.asString();
                try {
                    return format.readFromString(text, true);
                } catch (DateTimeException e) {
                    throw new SerializationException("Expected a " + format + " timestamp, but found \"" + text + "\"",
                                                     e);
                }
            }
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_INTEGER:
            case BIG_DECIMAL:
                if (format == TimestampFormatter.Prelude.EPOCH_SECONDS) {
                    return epochSecondsToInstant(value);
                }
                return format.readFromNumber(value.asNumber());
            default:
                return value.asTimestamp();
        }
    }

    private static Instant epochSecondsToInstant(Document value) {
        BigDecimal seconds;
        switch (value.type()) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                return ofEpochSecond(value.asLong(), 0, value);
            case FLOAT:
            case DOUBLE: {
                double d = value.asDouble();
                if (!Double.isFinite(d)) {
                    throw new SerializationException("Expected an epoch-seconds timestamp, but found " + d);
                }
                // The shortest decimal form of the double: the JSON text the double was read from.
                seconds = BigDecimal.valueOf(d);
                break;
            }
            case BIG_INTEGER:
                seconds = new BigDecimal(value.asBigInteger());
                break;
            default:
                seconds = value.asBigDecimal();
                break;
        }
        if (seconds.precision() - seconds.scale() > 19) {
            throw new SerializationException("Epoch seconds out of range: " + seconds);
        }
        BigDecimal whole = seconds.setScale(0, RoundingMode.FLOOR);
        // Digits past nanoseconds are dropped, as the JSON deserializer drops them.
        int nanos = seconds.subtract(whole).movePointRight(9).setScale(0, RoundingMode.DOWN).intValue();
        long epochSecond;
        try {
            epochSecond = whole.longValueExact();
        } catch (ArithmeticException e) {
            throw new SerializationException("Epoch seconds out of range: " + seconds, e);
        }
        return ofEpochSecond(epochSecond, nanos, value);
    }

    private static Instant ofEpochSecond(long epochSecond, int nanos, Document value) {
        try {
            return Instant.ofEpochSecond(epochSecond, nanos);
        } catch (DateTimeException e) {
            throw new SerializationException("Epoch seconds out of range: " + value.asNumber(), e);
        }
    }

    /**
     * Base class of the JSON documents: holds the settings, creates the settings-aware deserializer, and implements
     * {@code equals} and {@code hashCode} with the {@link Document} rules. Contents are serialized with the default
     * {@link Document#serializeContents}, which writes the value of {@link #type()} with its prelude schema.
     */
    abstract static class JsonDocument implements Document {
        private final JsonSettings settings;

        JsonDocument(JsonSettings settings) {
            this.settings = Objects.requireNonNull(settings, "settings must not be null");
        }

        /**
         * Returns the settings of the codec that read the document.
         */
        final JsonSettings settings() {
            return settings;
        }

        @Override
        public final ShapeDeserializer createDeserializer() {
            return new JsonDocumentDeserializer(settings, this);
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
            return "JsonDocument(" + type() + ": " + value + ")";
        }
    }

    static final class BooleanDocument extends JsonDocument {
        private final boolean value;

        BooleanDocument(boolean value, JsonSettings settings) {
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

    static final class StringDocument extends JsonDocument {
        private final String value;

        StringDocument(String value, JsonSettings settings) {
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

        /**
         * Returns the base64-decoded bytes of the string; JSON writes blobs as base64 strings.
         *
         * @throws SerializationException if the string is not valid base64
         */
        @Override
        public ByteBuffer asBlob() {
            try {
                return ByteBuffer.wrap(Base64.getDecoder().decode(value));
            } catch (IllegalArgumentException e) {
                throw new SerializationException("Expected a base64 encoded blob value", e);
            }
        }

        /**
         * Parses the string with the default timestamp format of the settings.
         *
         * @throws SerializationException if the default format is not a string format, or the string is not in it
         */
        @Override
        public Instant asTimestamp() {
            return readTimestamp(this, settings().timestampResolver().defaultFormat());
        }

        @Override
        public String toString() {
            return describe(value);
        }
    }

    /**
     * A byte, short, integer or long number, held as a long (every value of the narrower types is exact).
     */
    static final class IntegralDocument extends JsonDocument {
        private final ShapeType type;
        private final long value;

        IntegralDocument(ShapeType type, long value, JsonSettings settings) {
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

        /**
         * Reads the number as epoch seconds when that is the default timestamp format of the settings.
         *
         * @throws SerializationException if the default format is not {@code epoch-seconds}
         */
        @Override
        public Instant asTimestamp() {
            return readTimestamp(this, settings().timestampResolver().defaultFormat());
        }

        @Override
        public String toString() {
            return describe(value);
        }
    }

    /**
     * A float or double number, held as a double (float-to-double widening is exact).
     */
    static final class FloatingPointDocument extends JsonDocument {
        private final ShapeType type;
        private final double value;

        FloatingPointDocument(ShapeType type, double value, JsonSettings settings) {
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

        /**
         * Reads the number as epoch seconds when that is the default timestamp format of the settings.
         *
         * @throws SerializationException if the default format is not {@code epoch-seconds}
         */
        @Override
        public Instant asTimestamp() {
            return readTimestamp(this, settings().timestampResolver().defaultFormat());
        }

        @Override
        public String toString() {
            return describe(type == ShapeType.FLOAT ? (Object) (float) value : (Object) value);
        }
    }

    static final class BigIntegerDocument extends JsonDocument {
        private final BigInteger value;

        BigIntegerDocument(BigInteger value, JsonSettings settings) {
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

        /**
         * Reads the number as epoch seconds when that is the default timestamp format of the settings.
         *
         * @throws SerializationException if the default format is not {@code epoch-seconds}, or the value is out of
         *     range
         */
        @Override
        public Instant asTimestamp() {
            return readTimestamp(this, settings().timestampResolver().defaultFormat());
        }

        @Override
        public String toString() {
            return describe(value);
        }
    }

    static final class BigDecimalDocument extends JsonDocument {
        private final BigDecimal value;

        BigDecimalDocument(BigDecimal value, JsonSettings settings) {
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

        /**
         * Reads the number as epoch seconds when that is the default timestamp format of the settings.
         *
         * @throws SerializationException if the default format is not {@code epoch-seconds}, or the value is out of
         *     range
         */
        @Override
        public Instant asTimestamp() {
            return readTimestamp(this, settings().timestampResolver().defaultFormat());
        }

        @Override
        public String toString() {
            return describe(value);
        }
    }

    static final class ListDocument extends JsonDocument {
        private final List<Document> values;

        ListDocument(List<Document> values, JsonSettings settings) {
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

    static final class MapDocument extends JsonDocument {
        private static final Object NO_DISCRIMINATOR = new Object();

        private final Map<String, Document> values;
        // The parsed discriminator, NO_DISCRIMINATOR when there is none, or null until first read.
        private volatile Object discriminator;

        MapDocument(Map<String, Document> values, JsonSettings settings) {
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
         * @return the discriminator, or null if there is no string {@code __type} member
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
     * A {@link DocumentDeserializer} that reads JSON documents with the JSON codec's rules: structure members by
     * {@code jsonName} when the settings use it, timestamps with the resolved timestamp format, and the string forms
     * the JSON codec writes for non-finite floating-point numbers and, when the settings use them, for arbitrary
     * precision numbers.
     */
    private static final class JsonDocumentDeserializer extends DocumentDeserializer {
        private final JsonSettings settings;

        JsonDocumentDeserializer(JsonSettings settings, Document value) {
            super(value);
            this.settings = settings;
        }

        @Override
        protected DocumentDeserializer deserializer(Document value) {
            return new JsonDocumentDeserializer(settings, value);
        }

        /**
         * Reads the object's fields in document order, matching them to members with the settings' field mapper.
         * Unknown fields are handled as the JSON deserializer handles them: reported for structures; for unions,
         * {@code __type} is skipped and other fields are reported, or rejected when the settings forbid unknown union
         * members. Null values are skipped.
         */
        @Override
        public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
            Document value = readDocument();
            Schema container = schema.isMember() ? schema.memberTarget() : schema;
            MemberLookup lookup = settings.fieldMapper().fieldToMember(container);
            if (lookup == null) {
                lookup = container;
            }
            for (String field : value.getMemberNames()) {
                Schema member = lookup.member(field);
                if (member == null) {
                    unknownMember(container, state, consumer, field);
                    continue;
                }
                Document memberValue = value.getMember(field);
                if (memberValue != null) {
                    consumer.accept(state, member, deserializer(memberValue));
                }
            }
        }

        private <T> void unknownMember(Schema container, T state, StructMemberConsumer<T> consumer, String field) {
            if (container.type() == ShapeType.STRUCTURE) {
                consumer.unknownMember(state, field);
            } else if ("__type".equals(field)) {
                return;
            } else if (settings.forbidUnknownUnionMembers()) {
                throw new SerializationException("Unknown member " + field + " encountered for union "
                                                 + container.id());
            } else {
                consumer.unknownMember(state, field);
            }
        }

        /**
         * Reads a timestamp with the member's {@code timestampFormat} trait when the settings use it, else with the
         * settings' default format.
         */
        @Override
        public Instant readTimestamp(Schema schema) {
            return JsonDocuments.readTimestamp(readDocument(), settings.timestampResolver().resolve(schema));
        }

        /**
         * Also reads the strings {@code "NaN"}, {@code "Infinity"} and {@code "-Infinity"}, which the JSON codec
         * writes for non-finite values.
         */
        @Override
        public float readFloat(Schema schema) {
            Document value = readDocument();
            if (value != null && value.type() == ShapeType.STRING) {
                Double nonFinite = nonFinite(value.asString());
                if (nonFinite != null) {
                    return nonFinite.floatValue();
                }
            }
            return super.readFloat(schema);
        }

        /**
         * Also reads the strings {@code "NaN"}, {@code "Infinity"} and {@code "-Infinity"}, which the JSON codec
         * writes for non-finite values.
         */
        @Override
        public double readDouble(Schema schema) {
            Document value = readDocument();
            if (value != null && value.type() == ShapeType.STRING) {
                Double nonFinite = nonFinite(value.asString());
                if (nonFinite != null) {
                    return nonFinite;
                }
            }
            return super.readDouble(schema);
        }

        /**
         * Also reads a string when the settings write arbitrary precision numbers as strings.
         */
        @Override
        public BigInteger readBigInteger(Schema schema) {
            String text = arbitraryPrecisionText();
            if (text == null) {
                return super.readBigInteger(schema);
            }
            try {
                return new BigInteger(text);
            } catch (NumberFormatException e) {
                throw new SerializationException("Invalid BigInteger value for " + schema.id() + ": " + text, e);
            }
        }

        /**
         * Also reads a string when the settings write arbitrary precision numbers as strings.
         */
        @Override
        public BigDecimal readBigDecimal(Schema schema) {
            String text = arbitraryPrecisionText();
            if (text == null) {
                return super.readBigDecimal(schema);
            }
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException e) {
                throw new SerializationException("Invalid BigDecimal value for " + schema.id() + ": " + text, e);
            }
        }

        private String arbitraryPrecisionText() {
            Document value = readDocument();
            if (settings.useStringForArbitraryPrecision() && value != null && value.type() == ShapeType.STRING) {
                return value.asString();
            }
            return null;
        }

        private static Double nonFinite(String text) {
            switch (text) {
                case "NaN":
                    return Double.NaN;
                case "Infinity":
                    return Double.POSITIVE_INFINITY;
                case "-Infinity":
                    return Double.NEGATIVE_INFINITY;
                default:
                    return null;
            }
        }
    }
}
