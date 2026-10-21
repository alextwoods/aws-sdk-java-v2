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

package software.amazon.awssdk.codecs.cbor.internal;

import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.INDEFINITE;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_ARRAY;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_BYTESTRING;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_MAP;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_MASK;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_NEGINT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_POSINT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_SHIFT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_SIMPLE;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_TAG;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MAJOR_TYPE_TEXTSTRING;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.MINOR_TYPE_MASK;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.SIMPLE_DOUBLE;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.SIMPLE_FALSE;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.SIMPLE_NULL;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.SIMPLE_STREAM_BREAK;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.SIMPLE_TRUE;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.SIMPLE_UNDEFINED;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.SIMPLE_VALUE_1;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TAG_DECIMAL;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TAG_NEG_BIG_INT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TAG_POS_BIG_INT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TAG_TIME_EPOCH;
import static software.amazon.awssdk.codecs.cbor.internal.CborReadUtil.argLength;
import static software.amazon.awssdk.codecs.cbor.internal.CborReadUtil.readByteString;
import static software.amazon.awssdk.codecs.cbor.internal.CborReadUtil.readPosInt;
import static software.amazon.awssdk.codecs.cbor.internal.CborReadUtil.readStrLen;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.cbor.CborDocuments;
import software.amazon.awssdk.codecs.cbor.CborSettings;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Reads shapes from CBOR (RFC 8949) following the Smithy RPCv2 CBOR shape serialization rules. Definite- and
 * indefinite-length strings, arrays and maps are accepted; floating-point members also accept integers; null
 * structure members are treated as absent; unknown members are skipped and reported to
 * {@link StructMemberConsumer#unknownMember}. Malformed input throws {@link SerializationException}.
 *
 * <p>The deserializer is a one-token-lookahead tokenizer over the input array: the current token is the value the next
 * {@code read*} call consumes. Reads do not copy the input when they can avoid it; blobs are views of the input.
 *
 * <p>Port of smithy-java {@code CborDeserializer}.
 */
@SdkInternalApi
final class CborDeserializer implements ShapeDeserializer {
    private static final int MAX_DEPTH = 1000;

    private static final int FLAG_INDEFINITE_LEN = 1 << 31;
    private static final int MASK_LEN = ~FLAG_INDEFINITE_LEN;

    private static final String TYPE_MEMBER = "__type";

    private final CborSettings settings;
    private final byte[] payload;
    private final int end;
    private int idx;
    private byte token;

    // Definite sizes shrink to zero, indefinite sizes start at -1 and decrement meaninglessly towards Long.MIN_VALUE.
    // Must be long because we need to store 2 * size for maps, and a map can have up to Integer.MAX_VALUE elements.
    // Count is left shifted one. Low bit is collection type: 0 == map, 1 == array.
    private long currentState;
    private long[] previousStates = new long[4];
    private boolean inCollection;
    private int historyDepth;
    private int itemLength;
    // overhead is [0,8]
    private int overhead;
    private boolean readingTag;
    private int depth;

    CborDeserializer(byte[] payload, CborSettings settings) {
        this.payload = payload;
        this.settings = settings;
        this.end = payload.length;
        this.idx = 0;
        advance();
    }

    CborDeserializer(ByteBuffer byteBuffer, CborSettings settings) {
        this.settings = settings;
        if (byteBuffer.hasArray()) {
            this.payload = byteBuffer.array();
            int start = byteBuffer.arrayOffset() + byteBuffer.position();
            this.idx = start;
            this.end = start + byteBuffer.remaining();
        } else {
            this.payload = new byte[byteBuffer.remaining()];
            byteBuffer.duplicate().get(this.payload);
            this.idx = 0;
            this.end = this.payload.length;
        }
        advance();
    }

    static boolean isIndefinite(int itemLength) {
        return itemLength < 0;
    }

    static int itemLength(int itemLength) {
        return itemLength & MASK_LEN;
    }

    private byte advance() {
        token = nextToken0();
        return token;
    }

    private byte nextToken0() {
        if (inCollection) {
            long state = currentState;
            if (state >> 1 == 0) {
                return getEndToken(state);
            } else if ((state & 3) == 0) {
                idx += itemLength(itemLength) + overhead;
                if (idx >= end) {
                    throwIncompleteCollectionException();
                }
                return dispatchKey(payload[idx]);
            }
        }

        idx += itemLength(itemLength) + overhead;
        if (idx >= end) {
            return endOfBuffer(idx);
        }
        return dispatch(payload[idx]);
    }

    private byte dispatchKey(byte b) {
        byte major = (byte) ((b & MAJOR_TYPE_MASK) >> MAJOR_TYPE_SHIFT);
        if (major == MAJOR_TYPE_TEXTSTRING) {
            byte minor = (byte) (b & MINOR_TYPE_MASK);
            string(major, minor);
            return Token.KEY;
        } else if (b == SIMPLE_STREAM_BREAK) {
            return endStreamCollection();
        }
        throw new BadCborException("map keys must be strings");
    }

    private byte endOfBuffer(int i) {
        itemLength = 0;
        overhead = 0;
        if (i > end) {
            throw new BadCborException("unexpected end of payload");
        }
        if (inCollection) {
            throwIncompleteCollectionException();
        }
        return Token.FINISHED;
    }

    private byte getEndToken(long state) {
        byte retVal = state == 0 ? Token.END_OBJECT : Token.END_ARRAY;
        if (historyDepth > 0) {
            currentState = previousStates[--historyDepth];
        } else {
            inCollection = false;
            currentState = 0;
        }
        return retVal;
    }

    private byte dispatch(byte b) {
        byte major = (byte) ((b & MAJOR_TYPE_MASK) >> MAJOR_TYPE_SHIFT);
        byte minor = (byte) (b & MINOR_TYPE_MASK);
        switch (major) {
            case MAJOR_TYPE_POSINT:
            case MAJOR_TYPE_NEGINT:
                return integer(major, minor);
            case MAJOR_TYPE_BYTESTRING:
            case MAJOR_TYPE_TEXTSTRING:
                return string(major, minor);
            case MAJOR_TYPE_ARRAY:
            case MAJOR_TYPE_MAP:
                return collection(major, minor);
            case MAJOR_TYPE_TAG:
                return tag(minor);
            case MAJOR_TYPE_SIMPLE:
                return simple(major, minor);
            default:
                throw new BadCborException("unknown major type: " + major);
        }
    }

    private byte tag(byte minor) {
        // RFC 8949 3.4 permits nested tags, but only the simple tags of the Smithy data model are supported.
        if (readingTag) {
            throw new BadCborException("nested tags not permitted");
        }
        overhead = 1;
        itemLength = 0;
        readingTag = true;
        byte next = advance();
        readingTag = false;
        if (next == Token.FINISHED) {
            throw new BadCborException("unexpected end of payload: tag " + minor + " has no value");
        }
        switch (minor) {
            case TAG_TIME_EPOCH:
                if (next != Token.FLOAT && next > Token.NEG_INT) {
                    throw new BadCborException("malformed instant: got " + Token.name(next));
                }
                return (byte) (next | Token.TAG_FLAG);
            case TAG_POS_BIG_INT:
                if (next != Token.BYTE_STRING) {
                    throw new BadCborException("malformed +bignum: got " + Token.name(next));
                }
                return Token.POS_BIGINT;
            case TAG_NEG_BIG_INT:
                if (next != Token.BYTE_STRING) {
                    throw new BadCborException("malformed -bignum: got " + Token.name(next));
                }
                return Token.NEG_BIGINT;
            case TAG_DECIMAL:
                tagDecimalFp(next);
                return Token.BIG_DECIMAL;
            default:
                throw new BadCborException("unsupported tag minor " + minor);
        }
    }

    private void tagDecimalFp(byte next) {
        if (next != Token.START_ARRAY) {
            throw new BadCborException("malformed BIG_DECIMAL: got " + Token.name(next));
        }
        int start = idx;
        byte exponent = advance();
        if (exponent != Token.POS_INT && exponent != Token.NEG_INT) {
            throw new BadCborException("malformed BIG_DECIMAL: expected int 1, got " + Token.name(exponent));
        }
        byte mantissa = advance();
        int tmp = mantissa & 0b11110;
        if (tmp != Token.POS_INT && tmp != Token.POS_BIGINT) {
            throw new BadCborException("malformed BIG_DECIMAL: expected int 2, got " + Token.name(mantissa));
        }
        byte last = advance();
        if (last != Token.END_ARRAY) {
            throw new BadCborException("malformed BIG_DECIMAL: expected END_ARRAY, got " + Token.name(last));
        }
        itemLength = idx - start + itemLength(itemLength) + overhead - 1;
        overhead = 1;
        idx = start;
    }

    private byte integer(byte major, byte minor) {
        if (minor == INDEFINITE) {
            throw new BadCborException("numeric type has indefinite length");
        }
        int argLength = argLength(minor);
        if (argLength > 0) {
            overhead = 0;
            idx++;
            requireAvailable(idx, argLength);
        } else {
            overhead = 1;
        }
        itemLength = argLength;
        // 2 because the count is left-shifted one (2 == 1 << 1)
        currentState -= 2;
        return major;
    }

    private byte simple(byte major, byte minor) {
        if (minor <= SIMPLE_VALUE_1) {
            currentState -= 2;
            itemLength = 1;
            overhead = 0;
            switch (minor) {
                case SIMPLE_FALSE:
                    return Token.FALSE;
                case SIMPLE_TRUE:
                    return Token.TRUE;
                case SIMPLE_NULL:
                case SIMPLE_UNDEFINED:
                    return Token.NULL;
                default:
                    throw new BadCborException("bad simple minor type " + minor);
            }
        } else if (minor <= SIMPLE_DOUBLE) {
            integer(major, minor);
            return Token.FLOAT;
        } else if (minor == INDEFINITE) {
            return endStreamCollection();
        }
        throw new BadCborException("illegal simple minor type " + minor);
    }

    private byte endStreamCollection() {
        itemLength = 0;
        overhead = 1;
        // The collection type can stay in the low bit: all that matters is that the number is negative, and the low
        // bit only makes a positive number more positive and a negative number more negative.
        if (!inCollection || currentState >= 0) {
            throw new BadCborException("unexpected indefinite terminator");
        }
        long state = currentState;
        if (historyDepth > 0) {
            currentState = previousStates[--historyDepth];
        } else {
            inCollection = false;
            currentState = 0;
        }
        return (state & 1) == 0 ? Token.END_OBJECT : Token.END_ARRAY;
    }

    private byte string(byte major, byte minor) {
        overhead = 0;
        if (minor == INDEFINITE) {
            readIndefiniteLength(major);
        } else {
            int argLen = argLength(minor);
            itemLength = readImm(minor, argLen);
            requireAvailable(idx, itemLength);
        }
        currentState -= 2;
        return major;
    }

    private int readImm(int minor, int argLen) {
        idx++;
        if (argLen == 0) {
            return minor;
        }
        requireAvailable(idx, argLen);
        int ret = readPosInt(payload, idx, argLen);
        idx += argLen;
        return ret;
    }

    private void requireAvailable(int from, int len) {
        if ((long) from + len > end) {
            throw new BadCborException("unexpected end of payload");
        }
    }

    private byte collection(byte major, byte minor) {
        itemLength = 0;
        long size;
        if (minor == INDEFINITE) {
            overhead = 1;
            size = -1;
        } else {
            int argLen = argLength(minor);
            overhead = 0;
            size = readImm(minor, argLen);
        }
        if (inCollection) {
            currentState -= 2;
            if (historyDepth == previousStates.length) {
                previousStates = Arrays.copyOf(previousStates, previousStates.length * 2);
            }
            previousStates[historyDepth++] = currentState;
        }
        inCollection = true;
        if (major == MAJOR_TYPE_ARRAY) {
            currentState = (size << 1) | 1;
            return Token.START_ARRAY;
        }
        currentState = size << 2;
        return Token.START_OBJECT;
    }

    private void readIndefiniteLength(byte type) {
        itemLength = 0;
        idx++;
        long scan = idx;
        while (true) {
            if (scan >= end) {
                throw new BadCborException("non-terminating string");
            }
            byte b = payload[(int) scan];
            if (b == SIMPLE_STREAM_BREAK) {
                overhead++;
                break;
            }
            int major = (b & MAJOR_TYPE_MASK) >> MAJOR_TYPE_SHIFT;
            int minor = b & MINOR_TYPE_MASK;
            if (major != type) {
                throw new BadCborException("major type misalign: " + type + " " + major);
            }
            if (minor == INDEFINITE) {
                throw new BadCborException("expected finite length");
            }
            int argLen = argLength(minor);
            requireAvailable((int) scan + 1, argLen);
            int strLen = readStrLen(payload, (int) scan, minor, argLen);
            int totalOverhead = argLen + 1;
            scan += totalOverhead + (long) strLen;
            if (scan > end || (long) itemLength + strLen > MASK_LEN) {
                throw new BadCborException("indefinite-length string chunk exceeds the payload");
            }
            overhead += totalOverhead;
            itemLength += strLen;
        }
        itemLength |= FLAG_INDEFINITE_LEN;
    }

    private int collectionSize() {
        long s = currentState >> 2;
        return s >= 0 ? (int) s : -1;
    }

    private void throwIncompleteCollectionException() {
        String type = (currentState & 1L) == 0 ? "map" : "array";
        String msg = currentState < 0 ? "stream break" : ((currentState >> 1) + " more elements");
        throw new BadCborException("incomplete " + type + ": expecting " + msg);
    }

    /**
     * Checks that the input holds nothing after the value that was read. Throws {@link SerializationException} if
     * a collection is still open or more data follows.
     */
    @Override
    public void close() {
        byte current = token;
        if (current != Token.FINISHED && !inCollection && current != Token.START_ARRAY
            && current != Token.START_OBJECT) {
            current = advance();
        }
        if (current != Token.FINISHED) {
            throw new SerializationException("Unexpected CBOR content at end of object");
        }
    }

    @Override
    public boolean readBoolean(Schema schema) {
        byte current = this.token;
        if (current == Token.TRUE) {
            return true;
        } else if (current == Token.FALSE) {
            return false;
        }
        throw badType("boolean", current, schema);
    }

    private static SerializationException badType(String type, byte token, Schema schema) {
        String message = "Can't read " + Token.name(token) + " as a " + type;
        if (schema != null) {
            message += " for " + schema.id();
        }
        return new SerializationException(message);
    }

    private static SerializationException outOfRange(String type, long value, Schema schema) {
        String message = "Value " + value + " is out of range for a " + type;
        if (schema != null) {
            message += " for " + schema.id();
        }
        return new SerializationException(message);
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        byte current = this.token;
        if (current == Token.BYTE_STRING) {
            int len = itemLength;
            if (isIndefinite(len)) {
                return ByteBuffer.wrap(readByteString(payload, idx, len));
            }
            return ByteBuffer.wrap(payload, idx, len).slice();
        }
        throw badType("blob", current, schema);
    }

    @Override
    public byte readByte(Schema schema) {
        long value = readLong("byte", this.token, schema);
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw outOfRange("byte", value, schema);
        }
        return (byte) value;
    }

    @Override
    public short readShort(Schema schema) {
        long value = readLong("short", this.token, schema);
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw outOfRange("short", value, schema);
        }
        return (short) value;
    }

    @Override
    public int readInteger(Schema schema) {
        long value = readLong("integer", this.token, schema);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw outOfRange("integer", value, schema);
        }
        return (int) value;
    }

    @Override
    public long readLong(Schema schema) {
        return readLong("long", this.token, schema);
    }

    private long readLong(String type, byte current, Schema schema) {
        if (current != Token.POS_INT && current != Token.NEG_INT) {
            throw badType(type, current, schema);
        }
        int len = itemLength;
        long val = CborReadUtil.readLong(payload, current, idx, len);
        if (len < 8) {
            return val;
        }
        // A 64-bit argument above Long.MAX_VALUE wraps; saturate as smithy-java does.
        if (current == Token.POS_INT) {
            return val < 0 ? Long.MAX_VALUE : val;
        }
        return val < 0 ? val : Long.MIN_VALUE;
    }

    @Override
    public float readFloat(Schema schema) {
        return (float) readDouble("float", this.token, schema);
    }

    @Override
    public double readDouble(Schema schema) {
        return readDouble("double", this.token, schema);
    }

    private double readDouble(String type, byte current, Schema schema) {
        if (current == Token.FLOAT) {
            return readFloatingPoint();
        } else if (current == Token.POS_INT) {
            // Tolerate an integer-encoded value for a floating-point member: other encoders commonly write a
            // whole-valued double (for example 1.0) as a CBOR integer.
            long val = CborReadUtil.readLong(payload, current, idx, itemLength);
            // Lengths below 8 are zero-extended, so only a value above Long.MAX_VALUE can wrap to negative.
            if (val < 0) {
                throw new SerializationException("Cannot read a bignum-sized integer as a " + type);
            }
            return val;
        } else if (current == Token.NEG_INT) {
            long val = CborReadUtil.readLong(payload, current, idx, itemLength);
            // Likewise, only a value below Long.MIN_VALUE can wrap to >= 0.
            if (val >= 0) {
                throw new SerializationException("Cannot read a bignum-sized integer as a " + type);
            }
            return val;
        }
        throw badType(type, current, schema);
    }

    /** Reads the current half-, single- or double-precision float token. */
    private double readFloatingPoint() {
        int len = itemLength;
        long fp = CborReadUtil.readLong(payload, Token.FLOAT, idx, len);
        // ordered by how likely it is we'll encounter each case
        if (len == 8) {
            return Double.longBitsToDouble(fp);
        } else if (len == 4) {
            return Float.intBitsToFloat((int) fp);
        }
        return float16((int) fp);
    }

    /** Reads the current float token as a document of the width it was written with. */
    private Document readFloatingPointDocument() {
        int len = itemLength;
        long fp = CborReadUtil.readLong(payload, Token.FLOAT, idx, len);
        if (len == 8) {
            return CborDocuments.of(Double.longBitsToDouble(fp), settings);
        } else if (len == 4) {
            return CborDocuments.of(Float.intBitsToFloat((int) fp), settings);
        }
        return CborDocuments.of(float16((int) fp), settings);
    }

    // https://stackoverflow.com/questions/6162651/half-precision-floating-point-in-java/6162687
    private static float float16(int hbits) {
        // 10 bits mantissa
        int mant = hbits & 0x03ff;
        // 5 bits exponent
        int exp = hbits & 0x7c00;
        if (exp == 0x7c00) {
            // NaN/Inf
            exp = 0x3fc00;
        } else if (exp != 0) {
            // normalized value: exp - 15 + 127
            exp += 0x1c000;
            if (mant == 0 && exp > 0x1c400) {
                // smooth transition
                return Float.intBitsToFloat((hbits & 0x8000) << 16 | exp << 13);
            }
        } else if (mant != 0) {
            // && exp==0 -> subnormal: make it normal
            exp = 0x1c400;
            do {
                // mantissa * 2, decrease exp by 1, while not normal
                mant <<= 1;
                exp -= 0x400;
            } while ((mant & 0x400) == 0);
            // discard subnormal bit
            mant &= 0x3ff;
        }
        // else +/-0 -> +/-0; combine all parts: sign << (31 - 15), value << (23 - 10)
        return Float.intBitsToFloat((hbits & 0x8000) << 16 | (exp | mant) << 13);
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        byte current = this.token;
        int tmp = current & 0b11110;
        if (tmp != Token.POS_INT && tmp != Token.POS_BIGINT) {
            throw badType("biginteger", current, schema);
        }
        return CborReadUtil.readBigInteger(payload, current, idx, itemLength);
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        byte current = this.token;
        if (current == Token.BIG_DECIMAL) {
            return CborReadUtil.readBigDecimal(payload, idx);
        } else if (current == Token.FLOAT) {
            double value = readFloatingPoint();
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new SerializationException("Cannot read " + value + " as a bigdecimal");
            }
            return BigDecimal.valueOf(value);
        } else if (current == Token.POS_INT || current == Token.NEG_INT) {
            return new BigDecimal(CborReadUtil.readBigInteger(payload, current, idx, itemLength));
        }
        throw badType("bigdecimal", current, schema);
    }

    @Override
    public String readString(Schema schema) {
        byte current = this.token;
        if (current != Token.TEXT_STRING) {
            throw badType("string", current, schema);
        }
        return CborReadUtil.readTextString(payload, idx, itemLength);
    }

    /**
     * Reads the current value as a {@link CborDocuments} document that holds this deserializer's settings: integers as
     * long documents, floats as float or double documents (by their encoded width), tag 1 values as timestamps, byte
     * strings as blobs, bignums as big integers, decimal fractions as big decimals, arrays as list documents and maps
     * as map documents, which resolve a {@code __type} discriminator with the settings. CBOR null reads as
     * {@code null}.
     *
     * <p>smithy-java returns only maps as {@code CborDocuments} documents, and other values as untyped documents.
     */
    @Override
    public Document readDocument() {
        byte current = this.token;
        switch (current) {
            case Token.FINISHED:
                throw new SerializationException("No CBOR value to read");
            case Token.POS_INT:
            case Token.NEG_INT:
                return CborDocuments.of(readLong(null), settings);
            case Token.NULL:
                return null;
            case Token.TEXT_STRING:
                return CborDocuments.of(readString(null), settings);
            case Token.BYTE_STRING:
                return CborDocuments.of(readBlob(null), settings);
            case Token.TRUE:
                return CborDocuments.of(true, settings);
            case Token.FALSE:
                return CborDocuments.of(false, settings);
            case Token.EPOCH_INEG:
            case Token.EPOCH_IPOS:
            case Token.EPOCH_F:
                return CborDocuments.of(readTimestamp(null), settings);
            case Token.FLOAT:
                return readFloatingPointDocument();
            case Token.POS_BIGINT:
            case Token.NEG_BIGINT:
                return CborDocuments.of(readBigInteger(null), settings);
            case Token.BIG_DECIMAL:
                return CborDocuments.of(readBigDecimal(null), settings);
            case Token.START_ARRAY:
                return readListDocument();
            case Token.START_OBJECT:
                return readMapDocument();
            default:
                throw new SerializationException("Unexpected token: " + Token.name(current));
        }
    }

    private Document readListDocument() {
        enterContainer();
        List<Document> values = new ArrayList<>();
        for (byte current = advance(); current != Token.END_ARRAY; current = advance()) {
            values.add(readDocument());
        }
        depth--;
        return CborDocuments.of(values, settings);
    }

    private Document readMapDocument() {
        enterContainer();
        Map<String, Document> values = new LinkedHashMap<>();
        for (byte current = advance(); current != Token.END_OBJECT; current = advance()) {
            if (current != Token.KEY) {
                throw badType("struct member", current, null);
            }
            String key = CborReadUtil.readTextString(payload, idx, itemLength);
            advance();
            values.put(key, readDocument());
        }
        depth--;
        return CborDocuments.of(values, settings);
    }

    private void enterContainer() {
        depth++;
        if (depth > MAX_DEPTH) {
            throw new SerializationException("Maximum nesting depth exceeded: " + MAX_DEPTH);
        }
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        byte current = this.token;
        try {
            if (current == Token.EPOCH_IPOS || current == Token.EPOCH_INEG) {
                byte untagged = (byte) (current ^ Token.TAG_FLAG);
                return Instant.ofEpochSecond(readLong("timestamp", untagged, schema));
            } else if (current == Token.EPOCH_F) {
                double d = readFloatingPoint();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    throw new SerializationException("Cannot read " + d + " as a timestamp"
                                                     + (schema == null ? "" : " for " + schema.id()));
                }
                return Instant.ofEpochMilli(Math.round(d * 1000d));
            }
        } catch (DateTimeException e) {
            throw new SerializationException("timestamp out of range", e);
        }
        throw badType("timestamp", current, schema);
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        byte current = this.token;
        if (current != Token.START_OBJECT) {
            readStructEmpty(schema, current);
            return;
        }
        enterContainer();
        Schema structSchema = schema.isMember() ? schema.memberTarget() : schema;
        CborSchemaExtensions.CborExtension ext = structSchema.getExtension(CborSchemaExtensions.KEY);
        CborMemberLookup lookup = ext != null ? ext.memberLookup() : null;
        int expectedNext = 0;

        for (current = advance(); current != Token.END_OBJECT; current = advance()) {
            if (current != Token.KEY) {
                throw badType("struct member", current, structSchema);
            }

            int memberPos = idx;
            int memberLen = itemLength;
            if (advance() == Token.NULL) {
                continue;
            }

            Schema member = null;
            if (!isIndefinite(memberLen) && lookup != null) {
                if (expectedNext >= 0 && expectedNext < lookup.orderedNameBytes.length) {
                    byte[] expected = lookup.orderedNameBytes[expectedNext];
                    if (expected.length == memberLen
                        && CborReadUtil.regionEquals(payload, memberPos, memberPos + memberLen, expected, 0)) {
                        member = lookup.orderedSchemas[expectedNext];
                    }
                }
                if (member == null) {
                    member = lookup.lookup(payload, memberPos, memberPos + memberLen, -1);
                }
            }

            if (member == null) {
                expectedNext = resolveMemberFallback(structSchema, memberPos, memberLen, expectedNext,
                                                     state, consumer);
                continue;
            }

            expectedNext = member.memberIndex() + 1;
            consumer.accept(state, member, this);
        }
        depth--;
    }

    private void readStructEmpty(Schema schema, byte current) {
        if (current == Token.FINISHED && schema.hasTrait(TraitKey.UNIT_TYPE_TRAIT)) {
            return;
        }
        throw badType("struct", current, schema);
    }

    private <T> int resolveMemberFallback(Schema structSchema, int memberPos, int memberLen, int expectedNext,
                                          T state, StructMemberConsumer<T> consumer) {
        String name = CborReadUtil.readTextString(payload, memberPos, memberLen);
        Schema member = structSchema.member(name);
        if (member != null) {
            consumer.accept(state, member, this);
            return member.memberIndex() + 1;
        }
        if (structSchema.type() == ShapeType.UNION) {
            // RPCv2 CBOR: deserializers must ignore an unrecognized __type member of a union.
            if (TYPE_MEMBER.equals(name)) {
                skipUnknownMember();
                return expectedNext;
            }
            if (settings.forbidUnknownUnionMembers()) {
                throw new SerializationException("Unknown member " + name + " encountered for union "
                                                 + structSchema.id());
            }
        }
        consumer.unknownMember(state, name);
        skipUnknownMember();
        return expectedNext;
    }

    private void skipUnknownMember() {
        byte current = token;
        if (current != Token.START_OBJECT && current != Token.START_ARRAY) {
            return;
        }

        int nesting = 0;
        while (true) {
            if (current == Token.START_OBJECT || current == Token.START_ARRAY) {
                nesting++;
            } else if ((current == Token.END_OBJECT || current == Token.END_ARRAY) && --nesting == 0) {
                return;
            }
            current = advance();
        }
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        enterContainer();
        byte current = this.token;
        if (current != Token.START_ARRAY) {
            throw badType("list", current, schema);
        }

        for (current = advance(); current != Token.END_ARRAY; current = advance()) {
            consumer.accept(state, this);
        }
        depth--;
    }

    @Override
    public int containerSize() {
        return collectionSize();
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        enterContainer();
        byte current = this.token;
        if (current != Token.START_OBJECT) {
            throw badType("map", current, schema);
        }

        for (current = advance(); current != Token.END_OBJECT; current = advance()) {
            if (current != Token.KEY) {
                throw badType("key", current, schema);
            }
            String key = CborReadUtil.readTextString(payload, idx, itemLength);
            advance();
            consumer.accept(state, key, this);
        }
        depth--;
    }

    @Override
    public boolean isNull() {
        return token == Token.NULL;
    }

    /**
     * Token kinds produced by the tokenizer. The low values of the scalar tokens are the CBOR major types they come
     * from; {@link #TAG_FLAG} marks a tag 1 (epoch time) value.
     */
    static final class Token {
        static final byte TAG_FLAG = 1 << 4;

        static final byte POS_INT = MAJOR_TYPE_POSINT;
        static final byte NEG_INT = MAJOR_TYPE_NEGINT;
        static final byte BYTE_STRING = MAJOR_TYPE_BYTESTRING;
        static final byte TEXT_STRING = MAJOR_TYPE_TEXTSTRING;

        static final byte NULL = 4;
        static final byte KEY = 5;
        static final byte START_OBJECT = 6;
        static final byte START_ARRAY = 7;
        static final byte END_OBJECT = 8;
        static final byte END_ARRAY = 9;

        static final byte POS_BIGINT = 10;
        static final byte NEG_BIGINT = 11;
        static final byte FLOAT = 12;
        static final byte BIG_DECIMAL = 14;
        static final byte TRUE = 15;
        static final byte FALSE = TRUE | TAG_FLAG;

        static final byte EPOCH_IPOS = POS_INT | TAG_FLAG;
        static final byte EPOCH_INEG = NEG_INT | TAG_FLAG;
        static final byte EPOCH_F = FLOAT | TAG_FLAG;

        static final byte FINISHED = -1;

        private Token() {
        }

        static String name(byte token) {
            switch (token) {
                case POS_INT:
                    return "POS_INT";
                case NEG_INT:
                    return "NEG_INT";
                case BYTE_STRING:
                    return "BYTE_STRING";
                case TEXT_STRING:
                    return "TEXT_STRING";
                case POS_BIGINT:
                    return "POS_BIGINT";
                case NEG_BIGINT:
                    return "NEG_BIGINT";
                case FLOAT:
                    return "FLOAT";
                case BIG_DECIMAL:
                    return "BIG_DECIMAL";
                case TRUE:
                    return "TRUE";
                case FALSE:
                    return "FALSE";
                case EPOCH_IPOS:
                    return "EPOCH_IPOS";
                case EPOCH_INEG:
                    return "EPOCH_INEG";
                case EPOCH_F:
                    return "EPOCH_F";
                case NULL:
                    return "NULL";
                case KEY:
                    return "KEY";
                case START_OBJECT:
                    return "START_OBJECT";
                case START_ARRAY:
                    return "START_ARRAY";
                case END_ARRAY:
                    return "END_ARRAY";
                case END_OBJECT:
                    return "END_OBJECT";
                case FINISHED:
                    return "FINISHED";
                default:
                    return "UNKNOWN(" + token + ")";
            }
        }
    }
}
