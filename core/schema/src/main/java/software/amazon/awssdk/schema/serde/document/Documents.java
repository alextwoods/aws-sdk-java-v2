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
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.utils.Validate;

/**
 * The protocol-agnostic {@link Document} implementations and the equality helpers they share.
 *
 * <p>Port of smithy-java {@code Documents} and the equality parts of {@code DocumentUtils}. The implementations do not
 * hold a schema yet; the full document port (design B.2) adds one.
 */
@SdkInternalApi
final class Documents {

    private Documents() {
    }

    static SerializationException typeMismatch(String expected, Document actual) {
        return new SerializationException("Expected a " + expected + " document, but found " + actual.type());
    }

    static boolean documentEquals(Document l, Document r, int options) {
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

    private static String describe(ShapeType type, Object value) {
        return "Document(" + type + ": " + value + ")";
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
     * Base class for the implementations, with value-based {@code equals} and {@code hashCode}.
     */
    abstract static class AbstractDocument implements Document {
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
        static final BooleanDocument TRUE = new BooleanDocument(true);
        static final BooleanDocument FALSE = new BooleanDocument(false);

        private final boolean value;

        private BooleanDocument(boolean value) {
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
            return describe(type(), value);
        }
    }

    /**
     * A byte, short, integer or long document. The value is held as a long; every value of the narrower types is
     * represented exactly, so the narrowing casts give the same results as casting from the original type.
     */
    static final class IntegralDocument extends AbstractDocument {
        private final ShapeType type;
        private final long value;

        IntegralDocument(ShapeType type, long value) {
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
        public String toString() {
            return describe(type, value);
        }
    }

    /**
     * A float or double document. A float is held as a double; float-to-double widening is exact, so every coercion
     * gives the same result as coercing the original float.
     */
    static final class FloatingPointDocument extends AbstractDocument {
        private final ShapeType type;
        private final double value;

        FloatingPointDocument(ShapeType type, double value) {
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
        public String toString() {
            return describe(type, type == ShapeType.FLOAT ? (Object) (float) value : (Object) value);
        }
    }

    static final class BigIntegerDocument extends AbstractDocument {
        private final BigInteger value;

        BigIntegerDocument(BigInteger value) {
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
        public String toString() {
            return describe(type(), value);
        }
    }

    static final class BigDecimalDocument extends AbstractDocument {
        private final BigDecimal value;

        BigDecimalDocument(BigDecimal value) {
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
        public String toString() {
            return describe(type(), value);
        }
    }

    static final class StringDocument extends AbstractDocument {
        private final String value;

        StringDocument(String value) {
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
        public String toString() {
            return describe(type(), value);
        }
    }

    static final class BlobDocument extends AbstractDocument {
        private final ByteBuffer value;

        BlobDocument(ByteBuffer value) {
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
        public String toString() {
            return describe(type(), value.remaining() + " bytes");
        }
    }

    static final class TimestampDocument extends AbstractDocument {
        private final Instant value;

        TimestampDocument(Instant value) {
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
        public String toString() {
            return describe(type(), value);
        }
    }

    static final class ListDocument extends AbstractDocument {
        private final List<Document> values;

        ListDocument(List<Document> values) {
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
        public String toString() {
            return describe(type(), values);
        }
    }

    static final class StringMapDocument extends AbstractDocument {
        private final Map<String, Document> members;

        StringMapDocument(Map<String, Document> members) {
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
        public String toString() {
            return describe(type(), members);
        }
    }
}
