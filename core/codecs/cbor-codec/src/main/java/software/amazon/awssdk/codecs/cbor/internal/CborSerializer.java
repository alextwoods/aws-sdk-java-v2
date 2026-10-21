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

import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.EIGHT_BYTES;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.FOUR_BYTES;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.INDEFINITE;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.ONE_BYTE;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TAG_DECIMAL;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TAG_NEG_BIG_INT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TAG_POS_BIG_INT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TAG_TIME_EPOCH;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TWO_BYTES;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_ARRAY;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_BYTESTRING;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_MAP;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_NEGINT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_POSINT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_SIMPLE_BREAK_STREAM;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_SIMPLE_DOUBLE;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_SIMPLE_FALSE;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_SIMPLE_FLOAT;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_SIMPLE_NULL;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_SIMPLE_TRUE;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_TAG;
import static software.amazon.awssdk.codecs.cbor.internal.CborConstants.TYPE_TEXTSTRING;
import static software.amazon.awssdk.codecs.cbor.internal.CborReadUtil.flipBytes;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.internal.StripedPool;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Writes shapes as CBOR (RFC 8949) following the Smithy RPCv2 CBOR shape serialization rules: integers in their
 * shortest form, structures as indefinite-length maps keyed by member name, lists and maps with a definite length
 * when the size is known, timestamps as tag 1 epoch seconds, big integers as integers or tag 2/3 bignums, and big
 * decimals as tag 4 decimal fractions.
 *
 * <p>Output is buffered in an internal array and written to the sink on {@link #flush()} and {@link #close()}. A
 * serializer created without a sink is pooled per thread by {@link DefaultCborSerdeProvider}.
 *
 * <p>SDK deviation: with a sink, a full buffer is written to the sink before the buffer grows, so the buffer stays at
 * its initial size (1 KB, instead of smithy-java's 4 KB) unless a single value needs more. smithy-java grows the buffer
 * to the whole document and writes it to the sink on {@link #close}, which allocates every intermediate buffer and then
 * copies the document again into a sink that callers such as {@code HttpClientProtocol} have already sized for it.
 *
 * <p>Port of smithy-java {@code CborSerializer}. {@code VarHandle} writes are replaced by explicit shifts, and the
 * {@code CompactStringAccess} fast path by a {@code charAt} loop that copies ASCII text four characters at a time.
 */
@SdkInternalApi
final class CborSerializer implements ShapeSerializer {

    private static final int MAP_STREAM = TYPE_MAP | INDEFINITE;
    private static final int ARRAY_STREAM = TYPE_ARRAY | INDEFINITE;

    // The sink-less serializer holds the whole document, and Codec.serialize pools it, so it starts larger.
    private static final int DEFAULT_BUF_SIZE = 4096;
    private static final int MAX_CACHEABLE_BUF = DEFAULT_BUF_SIZE * 4;
    // A serializer with a sink is allocated per call and drains its buffer to the sink when full (see grow), so a
    // small buffer only adds sink writes for large documents.
    private static final int SINK_BUF_SIZE = 1024;

    private static final StripedPool<CborSerializer, Void> POOL = new CborStripedPool();

    // The "__type" key written before the members of a structure document with a discriminator.
    private static final byte[] TYPE_FIELD_BYTES = encodeMemberName("__type");

    byte[] buf;
    int pos;

    private final OutputStream sink;

    // Bit i of collectionMask records whether level i was opened as indefinite-length.
    private long collectionMask;
    private int collectionDepth;
    private long[] collectionOverflow;

    private byte[][] currentFieldNameTable;

    private final CborStructSerializer structSerializer = new CborStructSerializer();
    private final CborMapSerializer mapSerializer = new CborMapSerializer();

    CborSerializer() {
        this(null);
    }

    CborSerializer(OutputStream sink) {
        this.sink = sink;
        this.buf = new byte[sink == null ? DEFAULT_BUF_SIZE : SINK_BUF_SIZE];
        this.pos = 0;
    }

    static CborSerializer acquire() {
        return POOL.acquire(null);
    }

    static void release(CborSerializer serializer) {
        POOL.release(serializer);
    }

    ByteBuffer extractResult() {
        return ByteBuffer.wrap(Arrays.copyOf(buf, pos));
    }

    private void ensureCapacity(int needed) {
        if (pos + needed > buf.length) {
            grow(needed);
        }
    }

    // Separate cold grow method helps the JIT inline ensureCapacity's fast path
    private void grow(int needed) {
        // With a sink, write out what is buffered and reuse the buffer; grow only if one value does not fit in it.
        // Callers reserve a value's full size before writing it and only write at pos after this returns. The one
        // backpatch, of a non-ASCII text string's length header, stays inside that reservation, so draining is safe.
        if (sink != null && pos > 0) {
            drainToSink();
            if (needed <= buf.length) {
                return;
            }
        }
        buf = Arrays.copyOf(buf, Math.max(buf.length * 2, pos + needed));
    }

    private void drainToSink() {
        try {
            sink.write(buf, 0, pos);
            pos = 0;
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void flush() {
        try {
            if (sink != null && pos > 0) {
                sink.write(buf, 0, pos);
                pos = 0;
                sink.flush();
            }
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void close() {
        try {
            if (sink != null && pos > 0) {
                sink.write(buf, 0, pos);
                pos = 0;
            }
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    private void tagAndLength(int type, int len) {
        // max: 1 type byte + 4 length bytes (int)
        ensureCapacity(5);
        tagAndLengthUnchecked(type, len);
    }

    /** Writes the initial byte and length without ensureCapacity; the caller must have reserved 5 bytes. */
    private void tagAndLengthUnchecked(int type, int len) {
        if (len < ONE_BYTE) {
            buf[pos++] = (byte) (type | len);
        } else if (len <= 0xFF) {
            buf[pos++] = (byte) (type | ONE_BYTE);
            buf[pos++] = (byte) len;
        } else if (len <= 0xFFFF) {
            buf[pos++] = (byte) (type | TWO_BYTES);
            putShort(buf, pos, len);
            pos += 2;
        } else {
            buf[pos++] = (byte) (type | FOUR_BYTES);
            putInt(buf, pos, len);
            pos += 4;
        }
    }

    private void writeLong(long l) {
        // max: 1 type byte + 8 data bytes
        ensureCapacity(9);
        writeLongUnchecked(l);
    }

    /** Writes a CBOR integer without ensureCapacity; the caller must have reserved 9 bytes. */
    private void writeLongUnchecked(long value) {
        long l = value;
        int type;
        if (l < 0) {
            l = -l - 1;
            type = TYPE_NEGINT;
        } else {
            type = TYPE_POSINT;
        }

        if (l < ONE_BYTE) {
            buf[pos++] = (byte) (type | (int) l);
        } else if (l <= 0xFFL) {
            buf[pos++] = (byte) (type | ONE_BYTE);
            buf[pos++] = (byte) l;
        } else if (l <= 0xFFFFL) {
            buf[pos++] = (byte) (type | TWO_BYTES);
            putShort(buf, pos, (int) l);
            pos += 2;
        } else if (l <= 0xFFFF_FFFFL) {
            buf[pos++] = (byte) (type | FOUR_BYTES);
            putInt(buf, pos, (int) l);
            pos += 4;
        } else {
            buf[pos++] = (byte) (type | EIGHT_BYTES);
            putLong(buf, pos, l);
            pos += 8;
        }
    }

    private void writeDoubleUnchecked(long bits) {
        buf[pos++] = (byte) TYPE_SIMPLE_DOUBLE;
        putLong(buf, pos, bits);
        pos += 8;
    }

    private void writeFloatUnchecked(float value) {
        buf[pos++] = (byte) TYPE_SIMPLE_FLOAT;
        putInt(buf, pos, Float.floatToRawIntBits(value));
        pos += 4;
    }

    private void writeBytes0(int type, byte[] b, int off, int len) {
        ensureCapacity(5 + len);
        tagAndLengthUnchecked(type, len);
        System.arraycopy(b, off, buf, pos, len);
        pos += len;
    }

    private static void putShort(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 8);
        b[off + 1] = (byte) v;
    }

    private static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 24);
        b[off + 1] = (byte) (v >>> 16);
        b[off + 2] = (byte) (v >>> 8);
        b[off + 3] = (byte) v;
    }

    private static void putLong(byte[] b, int off, long v) {
        putInt(b, off, (int) (v >>> 32));
        putInt(b, off + 4, (int) v);
    }

    private void startMap(int size) {
        boolean indefinite = size < 0;
        if (indefinite) {
            ensureCapacity(1);
            buf[pos++] = (byte) MAP_STREAM;
        } else {
            tagAndLength(TYPE_MAP, size);
        }
        startCollection(indefinite);
    }

    private void startArray(int size) {
        boolean indefinite = size < 0;
        if (indefinite) {
            ensureCapacity(1);
            buf[pos++] = (byte) ARRAY_STREAM;
        } else {
            tagAndLength(TYPE_ARRAY, size);
        }
        startCollection(indefinite);
    }

    private void startCollection(boolean indefinite) {
        int d = collectionDepth;
        if (d < 64) {
            if (indefinite) {
                collectionMask |= 1L << d;
            } else {
                collectionMask &= ~(1L << d);
            }
        } else {
            pushOverflow(d, indefinite);
        }
        collectionDepth = d + 1;
    }

    private void pushOverflow(int d, boolean indefinite) {
        int overflowIdx = d - 64;
        long[] stack = collectionOverflow;
        if (stack == null) {
            stack = new long[Math.max(4, (overflowIdx >> 6) + 1)];
            collectionOverflow = stack;
        } else if ((overflowIdx >> 6) >= stack.length) {
            stack = Arrays.copyOf(stack, stack.length * 2);
            collectionOverflow = stack;
        }
        long bit = 1L << (overflowIdx & 63);
        int slot = overflowIdx >>> 6;
        if (indefinite) {
            stack[slot] |= bit;
        } else {
            stack[slot] &= ~bit;
        }
    }

    private boolean popIndefinite() {
        collectionDepth--;
        int d = collectionDepth;
        if (d < 64) {
            return ((collectionMask >>> d) & 1L) != 0L;
        }
        int overflowIdx = d - 64;
        return ((collectionOverflow[overflowIdx >>> 6] >>> (overflowIdx & 63)) & 1L) != 0L;
    }

    private void endCollection() {
        if (popIndefinite()) {
            ensureCapacity(1);
            buf[pos++] = (byte) TYPE_SIMPLE_BREAK_STREAM;
        }
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        writeStruct(schema, struct, null);
    }

    /**
     * Writes a structure, with a {@code __type} member first when {@code discriminator} is not null.
     */
    private void writeStruct(Schema schema, SerializableStruct struct, ShapeId discriminator) {
        ensureCapacity(1);
        buf[pos++] = (byte) MAP_STREAM;
        startCollection(true);
        if (discriminator != null) {
            ensureCapacity(TYPE_FIELD_BYTES.length);
            System.arraycopy(TYPE_FIELD_BYTES, 0, buf, pos, TYPE_FIELD_BYTES.length);
            pos += TYPE_FIELD_BYTES.length;
            writeStringValue(discriminator.toString());
        }

        byte[][] savedTable = currentFieldNameTable;
        Schema structSchema = schema.isMember() ? schema.memberTarget() : schema;
        CborSchemaExtensions.CborExtension ext = structSchema.getExtension(CborSchemaExtensions.KEY);
        currentFieldNameTable = ext != null ? ext.fieldNameTable() : null;

        struct.serializeMembers(structSerializer);

        currentFieldNameTable = savedTable;
        endCollection();
    }

    @Override
    public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
        startArray(size);
        consumer.accept(listState, this);
        endCollection();
    }

    @Override
    public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
        startMap(size);
        consumer.accept(mapState, mapSerializer);
        endCollection();
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        ensureCapacity(1);
        buf[pos++] = (byte) (value ? TYPE_SIMPLE_TRUE : TYPE_SIMPLE_FALSE);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        writeLong(value);
    }

    @Override
    public void writeShort(Schema schema, short value) {
        writeLong(value);
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        writeLong(value);
    }

    @Override
    public void writeLong(Schema schema, long value) {
        writeLong(value);
    }

    @Override
    public void writeFloat(Schema schema, float value) {
        ensureCapacity(5);
        writeFloatUnchecked(value);
    }

    @Override
    public void writeDouble(Schema schema, double value) {
        ensureCapacity(9);
        writeDoubleUnchecked(Double.doubleToRawLongBits(value));
    }

    @Override
    public void writeString(Schema schema, String value) {
        writeStringValue(value);
    }

    private void writeStringValue(String value) {
        int charLen = value.length();
        if (charLen == 0) {
            ensureCapacity(1);
            buf[pos++] = (byte) TYPE_TEXTSTRING;
            return;
        }
        ensureCapacity(5 + charLen * 3);
        int headerStart = pos;
        // Optimistically assume ASCII, where the UTF-8 length equals the character count and the header can be
        // written up front. On the first non-ASCII character, start over with the general encoder.
        tagAndLengthUnchecked(TYPE_TEXTSTRING, charLen);
        byte[] b = buf;
        int p = pos;
        int i = 0;
        int blockEnd = charLen - 3;
        while (i < blockEnd) {
            char c0 = value.charAt(i);
            char c1 = value.charAt(i + 1);
            char c2 = value.charAt(i + 2);
            char c3 = value.charAt(i + 3);
            if ((c0 | c1 | c2 | c3) >= 0x80) {
                encodeUtf8TextStringRewind(value, charLen, headerStart);
                return;
            }
            b[p] = (byte) c0;
            b[p + 1] = (byte) c1;
            b[p + 2] = (byte) c2;
            b[p + 3] = (byte) c3;
            p += 4;
            i += 4;
        }
        while (i < charLen) {
            char c = value.charAt(i);
            if (c >= 0x80) {
                encodeUtf8TextStringRewind(value, charLen, headerStart);
                return;
            }
            b[p++] = (byte) c;
            i++;
        }
        pos = p;
    }

    /**
     * Encodes {@code value} as a CBOR text string starting at {@code headerStart}. The caller must have reserved
     * {@code 5 + charLen * 3} bytes from {@code headerStart}. Writes the data after a 5-byte header scratch area,
     * backpatches the header with the actual UTF-8 byte length, and shifts the payload left if the real header is
     * shorter than 5 bytes. On exit, {@code pos} points past the payload.
     */
    private void encodeUtf8TextStringRewind(String value, int charLen, int headerStart) {
        int writeStart = headerStart + 5;
        int p = writeStart;
        for (int j = 0; j < charLen; j++) {
            int c = value.charAt(j);
            if (c < 0x80) {
                buf[p++] = (byte) c;
            } else if (c < 0x800) {
                buf[p++] = (byte) (0xC0 | (c >> 6));
                buf[p++] = (byte) (0x80 | (c & 0x3F));
            } else if (c >= 0xD800 && c <= 0xDBFF && j + 1 < charLen) {
                char low = value.charAt(j + 1);
                if (low >= 0xDC00 && low <= 0xDFFF) {
                    int cp = Character.toCodePoint((char) c, low);
                    buf[p++] = (byte) (0xF0 | (cp >> 18));
                    buf[p++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
                    buf[p++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                    buf[p++] = (byte) (0x80 | (cp & 0x3F));
                    j++;
                } else {
                    p = writeReplacementCharacter(p);
                }
            } else if (c >= 0xD800 && c <= 0xDFFF) {
                // Unpaired surrogate: U+FFFD, as String.getBytes(UTF_8) writes.
                p = writeReplacementCharacter(p);
            } else {
                buf[p++] = (byte) (0xE0 | (c >> 12));
                buf[p++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                buf[p++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        finishTextStringRewind(headerStart, p - writeStart);
    }

    private int writeReplacementCharacter(int p) {
        buf[p] = (byte) 0xEF;
        buf[p + 1] = (byte) 0xBF;
        buf[p + 2] = (byte) 0xBD;
        return p + 3;
    }

    private void finishTextStringRewind(int headerStart, int byteLen) {
        int writeStart = headerStart + 5;
        pos = headerStart;
        tagAndLengthUnchecked(TYPE_TEXTSTRING, byteLen);
        int actualHeaderLen = pos - headerStart;
        int shift = 5 - actualHeaderLen;
        if (shift > 0) {
            System.arraycopy(buf, writeStart, buf, writeStart - shift, byteLen);
        }
        pos = headerStart + actualHeaderLen + byteLen;
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        int len = value.remaining();
        ensureCapacity(5 + len);
        tagAndLengthUnchecked(TYPE_BYTESTRING, len);
        if (value.hasArray()) {
            System.arraycopy(value.array(), value.arrayOffset() + value.position(), buf, pos, len);
        } else {
            ByteBuffer view = value.duplicate();
            view.get(buf, pos, len);
        }
        pos += len;
    }

    @Override
    public void writeBlob(Schema schema, byte[] value) {
        writeBytes0(TYPE_BYTESTRING, value, 0, value.length);
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        long millis;
        try {
            millis = value.toEpochMilli();
        } catch (ArithmeticException e) {
            throw new SerializationException("Timestamp " + value + " cannot be written as epoch milliseconds", e);
        }
        // Always a double-precision float, also for whole seconds (smithy-java writes an integer there): the
        // Baseline_Build's smithy-rpcv2-protocol marshallers and the rpcv2Cbor protocol test fixtures send a double, and
        // the protocol allows both, so the wire format stays the same as the legacy SDK's.
        ensureCapacity(10);
        buf[pos++] = (byte) (TYPE_TAG | TAG_TIME_EPOCH);
        writeDoubleUnchecked(Double.doubleToRawLongBits(millis / 1000D));
    }

    @Override
    public void writeNull(Schema schema) {
        ensureCapacity(1);
        buf[pos++] = (byte) TYPE_SIMPLE_NULL;
    }

    @Override
    public void writeBigInteger(Schema schema, BigInteger value) {
        writeBigInteger(value);
    }

    @Override
    public void writeBigDecimal(Schema schema, BigDecimal value) {
        ensureCapacity(2);
        buf[pos++] = (byte) (TYPE_TAG | TAG_DECIMAL);
        tagAndLengthUnchecked(TYPE_ARRAY, 2);
        writeLong(-value.scale());
        writeBigInteger(value.unscaledValue());
    }

    /**
     * Writes a document through {@link Document#serializeContents}: scalars as the matching CBOR value, list documents
     * as arrays, map documents as maps, and a null document as CBOR null. The settings of the codec that read a
     * document are not used.
     *
     * <p>A structure document is written as an indefinite-length map of its members. When the document has a
     * {@link Document#getDiscriminator() discriminator}, its absolute ShapeId is written first as the {@code __type}
     * member; CBOR has no setting to turn this off, as in smithy-java. Only the document's own structure gets
     * {@code __type}; structures nested in its members do not, but a document nested in its members is written by
     * this method again.
     *
     * @throws SerializationException if a structure document does not write a structure
     * @throws software.amazon.awssdk.schema.serde.document.DiscriminatorException if reading the discriminator fails
     */
    @Override
    public void writeDocument(Schema schema, Document value) {
        if (value == null) {
            writeNull(schema);
        } else if (value.type() != ShapeType.STRUCTURE) {
            value.serializeContents(this);
        } else {
            value.serializeContents(new DocumentStructSerializer(value.getDiscriminator()));
        }
    }

    private void writeBigInteger(BigInteger value) {
        int bits = value.bitLength();
        if (bits < 64) {
            writeLong(value.longValue());
            return;
        }
        int signum = value.signum() >> 1;
        if (bits == 64) {
            int type = signum < 0 ? TYPE_NEGINT : TYPE_POSINT;
            ensureCapacity(9);
            buf[pos++] = (byte) (type | EIGHT_BYTES);
            putLong(buf, pos, value.longValue() ^ signum);
            pos += 8;
        } else {
            byte[] bytes = value.toByteArray();
            byte tag;
            if (signum < 0) {
                tag = TAG_NEG_BIG_INT;
                flipBytes(bytes);
            } else {
                tag = TAG_POS_BIG_INT;
            }
            ensureCapacity(1);
            buf[pos++] = (byte) (TYPE_TAG | tag);
            writeBytes0(TYPE_BYTESTRING, bytes, 0, bytes.length);
        }
    }

    private byte[] resolveFieldNameBytes(Schema schema) {
        byte[][] table = currentFieldNameTable;
        int idx = schema.memberIndex();
        if (table != null && idx >= 0 && idx < table.length && table[idx] != null) {
            return table[idx];
        }
        CborSchemaExtensions.CborExtension ext = schema.getExtension(CborSchemaExtensions.KEY);
        if (ext != null && ext.memberNameBytes() != null) {
            return ext.memberNameBytes();
        }
        return encodeMemberName(schema.memberName());
    }

    /**
     * Returns the CBOR text-string encoding (header and UTF-8 bytes) of a member name.
     */
    static byte[] encodeMemberName(String name) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        int len = nameBytes.length;
        int headerSize;
        if (len < ONE_BYTE) {
            headerSize = 1;
        } else if (len <= 0xFF) {
            headerSize = 2;
        } else if (len <= 0xFFFF) {
            headerSize = 3;
        } else {
            headerSize = 5;
        }
        byte[] result = new byte[headerSize + len];
        if (len < ONE_BYTE) {
            result[0] = (byte) (TYPE_TEXTSTRING | len);
        } else if (len <= 0xFF) {
            result[0] = (byte) (TYPE_TEXTSTRING | ONE_BYTE);
            result[1] = (byte) len;
        } else if (len <= 0xFFFF) {
            result[0] = (byte) (TYPE_TEXTSTRING | TWO_BYTES);
            putShort(result, 1, len);
        } else {
            result[0] = (byte) (TYPE_TEXTSTRING | FOUR_BYTES);
            putInt(result, 1, len);
        }
        System.arraycopy(nameBytes, 0, result, headerSize, len);
        return result;
    }

    private static final class CborStripedPool extends StripedPool<CborSerializer, Void> {
        @Override
        protected CborSerializer create(Void ctx) {
            return new CborSerializer();
        }

        @Override
        protected boolean canPool(CborSerializer s) {
            return s.buf != null && s.sink == null;
        }

        @Override
        protected void prepareForPool(CborSerializer s) {
            if (s.buf.length > MAX_CACHEABLE_BUF) {
                s.buf = new byte[DEFAULT_BUF_SIZE];
            }
        }

        @Override
        protected boolean reset(CborSerializer s, Void ctx) {
            s.pos = 0;
            s.collectionMask = 0L;
            s.collectionDepth = 0;
            s.currentFieldNameTable = null;
            return true;
        }
    }

    private final class CborStructSerializer implements ShapeSerializer {

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 1);
            putName(nameBytes);
            buf[pos++] = (byte) (value ? TYPE_SIMPLE_TRUE : TYPE_SIMPLE_FALSE);
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            writeIntegral(schema, value);
        }

        @Override
        public void writeShort(Schema schema, short value) {
            writeIntegral(schema, value);
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            writeIntegral(schema, value);
        }

        @Override
        public void writeLong(Schema schema, long value) {
            writeIntegral(schema, value);
        }

        private void writeIntegral(Schema schema, long value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 9);
            putName(nameBytes);
            writeLongUnchecked(value);
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 5);
            putName(nameBytes);
            writeFloatUnchecked(value);
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 9);
            putName(nameBytes);
            writeDoubleUnchecked(Double.doubleToRawLongBits(value));
        }

        @Override
        public void writeNull(Schema schema) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 1);
            putName(nameBytes);
            buf[pos++] = (byte) TYPE_SIMPLE_NULL;
        }

        @Override
        public void writeString(Schema schema, String value) {
            writeFieldNameBytes(schema);
            CborSerializer.this.writeString(schema, value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            writeFieldNameBytes(schema);
            CborSerializer.this.writeBlob(schema, value);
        }

        @Override
        public void writeBlob(Schema schema, byte[] value) {
            writeFieldNameBytes(schema);
            CborSerializer.this.writeBlob(schema, value);
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            writeFieldNameBytes(schema);
            CborSerializer.this.writeTimestamp(schema, value);
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            writeFieldNameBytes(schema);
            CborSerializer.this.writeBigInteger(schema, value);
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            writeFieldNameBytes(schema);
            CborSerializer.this.writeBigDecimal(schema, value);
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            writeFieldNameBytes(schema);
            CborSerializer.this.writeStruct(schema, struct);
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            writeFieldNameBytes(schema);
            CborSerializer.this.writeList(schema, listState, size, consumer);
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            writeFieldNameBytes(schema);
            CborSerializer.this.writeMap(schema, mapState, size, consumer);
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            writeFieldNameBytes(schema);
            CborSerializer.this.writeDocument(schema, value);
        }

        private void writeFieldNameBytes(Schema schema) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length);
            putName(nameBytes);
        }

        /** Copies a pre-encoded member key; the caller must have reserved its length. */
        private void putName(byte[] nameBytes) {
            System.arraycopy(nameBytes, 0, buf, pos, nameBytes.length);
            pos += nameBytes.length;
        }
    }

    /**
     * Receives the contents of a structure document, which must be a single structure, and writes it with the
     * document's discriminator (null for none) as its {@code __type} member.
     */
    private final class DocumentStructSerializer extends SpecificShapeSerializer {
        private final ShapeId discriminator;

        DocumentStructSerializer(ShapeId discriminator) {
            this.discriminator = discriminator;
        }

        @Override
        protected RuntimeException throwForInvalidState(String message, Schema schema) {
            return new SerializationException("A structure document must write a structure, but wrote a value of "
                                              + "type " + schema.type() + " with schema " + schema.id());
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            CborSerializer.this.writeStruct(schema, struct, discriminator);
        }
    }

    private final class CborMapSerializer implements MapSerializer {
        @Override
        public <T> void writeEntry(Schema keySchema, String key, T state,
                                   BiConsumer<T, ShapeSerializer> valueSerializer) {
            writeStringValue(key);
            valueSerializer.accept(state, CborSerializer.this);
        }
    }
}
