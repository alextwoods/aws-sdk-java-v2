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

package software.amazon.awssdk.codecs.json.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.commons.ByteBufferUtils;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.codecs.commons.internal.NumberCodec;
import software.amazon.awssdk.codecs.commons.internal.SinkBufferPool;
import software.amazon.awssdk.codecs.commons.internal.StripedPool;
import software.amazon.awssdk.codecs.json.JsonSettings;
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
 * JSON serializer that writes directly to a byte array buffer, using the pre-computed field name bytes of
 * {@link SmithyJsonSchemaExtensions}. Output is compact (no whitespace); {@link JsonPrettyPrinter} indents it.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.smithy.SmithyJsonSerializer}. Documents are written by
 * contents, with a {@code __type} member for structure documents (see {@link #writeDocument}).
 *
 * <p>SDK deviation: with a sink, a full buffer is written to the sink before the buffer grows, so the buffer stays at
 * its initial size unless a single value needs more. smithy-java grows the buffer to the whole document and writes it
 * to the sink on {@link #close}, which allocates every intermediate buffer and then copies the document again into a
 * sink that callers such as {@code HttpClientProtocol} have already sized for it. Because of that, a serializer with a
 * sink starts with a 1 KB buffer instead of smithy-java's 8 KB; the sink-less serializer keeps 8 KB. The 1 KB buffer is
 * lent by {@link SinkBufferPool} and returned on {@link #close}, so a serializer with a sink does not allocate it per
 * call; it must not be used after {@code close}. A {@link ByteBufferOutputStream} sink, which every SDK protocol uses,
 * needs no buffer: the serializer writes into the stream's array ({@link ByteBufferOutputStream#reserve}) and adds the
 * bytes on {@link #flush}, {@link #close} and when it needs more room ({@link ByteBufferOutputStream#advance}), so the
 * body is not copied either.
 */
@SdkInternalApi
final class SmithyJsonSerializer implements ShapeSerializer {

    private static final int MAX_DEPTH = 64;
    // The sink-less serializer holds the whole document, and Codec.serialize pools it, so it starts larger.
    private static final int DEFAULT_BUF_SIZE = 8192;
    private static final int MAX_CACHEABLE_BUF = DEFAULT_BUF_SIZE * 16;
    private static final StripedPool<SmithyJsonSerializer, JsonSettings> POOL = new JsonStripedPool();

    // The "__type": field name token written before the members of a structure document with a discriminator.
    private static final byte[] TYPE_FIELD_NAME_BYTES = JsonWriteUtils.encodeFieldNameToken("__type");

    // A sink other than a ByteBufferOutputStream, or null.
    private final OutputStream sink;
    // A ByteBufferOutputStream sink, whose array is buf; bytes [direct.size(), pos) are written but not yet added.
    private final ByteBufferOutputStream direct;
    private final JsonSettings settings;
    private final boolean useJsonName;

    // Nesting state for comma insertion
    private final boolean[] needsComma = new boolean[MAX_DEPTH];

    private final ShapeSerializer structSerializer = new StructSerializer();
    private final ShapeSerializer listElementSerializer = new ListElementSerializer();
    private final MapSerializer mapSerializer = new SmithyMapSerializer();

    private byte[] buf;
    private byte[] base64Scratch;
    private int pos;
    private int depth;

    // The field name table of the structure being written: resolved once per writeStruct, used by every member write.
    private byte[][] currentFieldNameTable;

    SmithyJsonSerializer(OutputStream sink, JsonSettings settings) {
        this.settings = settings;
        this.useJsonName = settings.useJsonName();
        if (sink instanceof ByteBufferOutputStream) {
            // Writes go straight into the stream's array, from its current size; see commitToDirect.
            this.direct = (ByteBufferOutputStream) sink;
            this.sink = null;
            this.pos = direct.size();
            this.buf = direct.reserve(0);
        } else {
            // A serializer with another sink drains its buffer to the sink when full (see grow), so its small buffer
            // only adds sink writes for large documents. The buffer is lent by SinkBufferPool until close().
            this.direct = null;
            this.sink = sink;
            this.buf = sink == null ? new byte[DEFAULT_BUF_SIZE] : SinkBufferPool.acquire();
        }
    }

    /**
     * Creates a serializer for direct buffer extraction (no OutputStream), for {@link #acquire} or one-off use.
     */
    SmithyJsonSerializer(JsonSettings settings) {
        this(null, settings);
    }

    static SmithyJsonSerializer acquire(JsonSettings settings) {
        return POOL.acquire(settings);
    }

    static void release(SmithyJsonSerializer serializer, boolean exception) {
        if (exception) {
            Arrays.fill(serializer.needsComma, false);
        }
        POOL.release(serializer);
    }

    ByteBuffer extractResult() {
        return ByteBuffer.wrap(Arrays.copyOf(buf, pos));
    }

    @Override
    public void flush() {
        if (direct != null) {
            commitToDirect();
            return;
        }
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

    // Adds the bytes written into the ByteBufferOutputStream's array since the last commit to the stream.
    private void commitToDirect() {
        if (buf != null) {
            direct.advance(pos - direct.size());
        }
    }

    @Override
    public void close() {
        if (direct != null) {
            commitToDirect();
            buf = null;
            return;
        }
        try {
            if (sink != null && pos > 0) {
                sink.write(buf, 0, pos);
                pos = 0;
            }
        } catch (IOException e) {
            throw new SerializationException(e);
        }
        // The pool drops a buffer the serializer grew past its pooled size. A second close releases nothing; a close
        // whose write failed keeps the buffer, as before, and does not return it to the pool.
        if (sink != null && buf != null) {
            SinkBufferPool.release(buf);
        }
        buf = null;
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        ensureCapacity(5);
        pos = NumberCodec.writeBoolean(buf, pos, value);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        ensureCapacity(4);
        pos = JsonWriteUtils.writeInt(buf, pos, value);
    }

    @Override
    public void writeShort(Schema schema, short value) {
        ensureCapacity(6);
        pos = JsonWriteUtils.writeInt(buf, pos, value);
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        ensureCapacity(11);
        pos = JsonWriteUtils.writeInt(buf, pos, value);
    }

    @Override
    public void writeLong(Schema schema, long value) {
        ensureCapacity(20);
        pos = JsonWriteUtils.writeLong(buf, pos, value);
    }

    @Override
    public void writeFloat(Schema schema, float value) {
        ensureCapacity(NumberCodec.FLOAT_MAX_BYTES);
        pos = NumberCodec.writeFloatFullQuoted(buf, pos, value);
    }

    @Override
    public void writeDouble(Schema schema, double value) {
        ensureCapacity(NumberCodec.DOUBLE_MAX_BYTES);
        pos = NumberCodec.writeDoubleFullQuoted(buf, pos, value);
    }

    @Override
    public void writeBigInteger(Schema schema, BigInteger value) {
        int maxLen = value.bitLength() / 3 + 2;
        ensureCapacity(maxLen + 2);
        if (settings.useStringForArbitraryPrecision()) {
            buf[pos++] = '"';
            pos = NumberCodec.writeBigInteger(buf, pos, value);
            buf[pos++] = '"';
        } else {
            pos = NumberCodec.writeBigInteger(buf, pos, value);
        }
    }

    @Override
    public void writeBigDecimal(Schema schema, BigDecimal value) {
        ensureCapacity(NumberCodec.maxBigDecimalLength(value) + 2);
        if (settings.useStringForArbitraryPrecision()) {
            buf[pos++] = '"';
            pos = NumberCodec.writeBigDecimal(buf, pos, value);
            buf[pos++] = '"';
        } else {
            pos = NumberCodec.writeBigDecimal(buf, pos, value);
        }
    }

    @Override
    public void writeString(Schema schema, String value) {
        ensureCapacity(value.length() + 2);
        writeQuotedStringExact(value);
    }

    @Override
    public void writeBlob(Schema schema, byte[] value) {
        writeBlobValue(ByteBuffer.wrap(value));
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        writeBlobValue(value);
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        TimestampFormatter format = settings.timestampResolver().resolve(schema);
        if (format == TimestampFormatter.Prelude.EPOCH_SECONDS) {
            // '-' + 19 digits (Long.MIN_VALUE) + '.' + 9 nano digits = 30 bytes
            ensureCapacity(30);
            pos = JsonWriteUtils.writeEpochSeconds(buf, pos, value.getEpochSecond(), value.getNano());
            return;
        }
        if (format == TimestampFormatter.Prelude.DATE_TIME) {
            ensureCapacity(42);
            pos = JsonWriteUtils.writeIso8601Timestamp(buf, pos, value);
            return;
        }
        if (format == TimestampFormatter.Prelude.HTTP_DATE) {
            // "Sat, 01 Jan 2026 00:00:00 GMT" = 29 chars + 2 quotes; years past 9999 need a few more.
            ensureCapacity(35);
            pos = JsonWriteUtils.writeHttpDate(buf, pos, value);
            return;
        }
        format.writeToSerializer(schema, value, this);
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        writeStruct(schema, struct, null);
    }

    /**
     * Writes a structure, with a {@code __type} member first when {@code discriminator} is not null.
     */
    private void writeStruct(Schema schema, SerializableStruct struct, ShapeId discriminator) {
        ensureCapacity(2);
        buf[pos++] = '{';
        enterContainer();

        if (discriminator != null) {
            String typeValue = discriminator.toString();
            ensureCapacity(TYPE_FIELD_NAME_BYTES.length + 1 + typeValue.length() + 2);
            writeFieldNameBytesUnchecked(TYPE_FIELD_NAME_BYTES);
            writeQuotedStringExact(typeValue);
        }

        // Resolve the field name table once per structure for all member writes.
        byte[][] savedTable = currentFieldNameTable;
        Schema structSchema = schema.isMember() ? schema.memberTarget() : schema;
        SmithyJsonSchemaExtensions.NativeJsonExtension ext = structSchema.getExtension(SmithyJsonSchemaExtensions.KEY);
        if (ext != null) {
            currentFieldNameTable = useJsonName ? ext.jsonFieldNameTable() : ext.memberFieldNameTable();
        } else {
            currentFieldNameTable = null;
        }

        struct.serializeMembers(structSerializer);

        currentFieldNameTable = savedTable; // restore for nested struct returns
        depth--;
        ensureCapacity(1);
        buf[pos++] = '}';
    }

    @Override
    public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
        ensureCapacity(2);
        buf[pos++] = '[';
        enterContainer();
        consumer.accept(listState, listElementSerializer);
        depth--;
        ensureCapacity(1);
        buf[pos++] = ']';
    }

    @Override
    public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
        ensureCapacity(2);
        buf[pos++] = '{';
        enterContainer();
        consumer.accept(mapState, mapSerializer);
        depth--;
        ensureCapacity(1);
        buf[pos++] = '}';
    }

    /**
     * Writes a document inline through {@link Document#serializeContents}, with this serializer's settings: scalars
     * as their JSON values (blobs as base64 strings, timestamps in the resolved format), lists as arrays, maps as
     * objects, and a null document as {@code null}. The settings of the codec that read a document are not used.
     *
     * <p>A structure document is written as an object of its members, named by {@code jsonName} when the settings
     * use it. When {@link JsonSettings#serializeTypeInDocuments()} is on and the document has a
     * {@link Document#getDiscriminator() discriminator}, its absolute ShapeId is written first as the {@code __type}
     * member. Only the document's own structure gets {@code __type}; structures nested in its members do not, but a
     * document nested in its members is written by this method again.
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
            ShapeId discriminator = settings.serializeTypeInDocuments() ? value.getDiscriminator() : null;
            value.serializeContents(new DocumentStructSerializer(discriminator));
        }
    }

    @Override
    public void writeNull(Schema schema) {
        ensureCapacity(JsonWriteUtils.NULL_BYTES.length);
        System.arraycopy(JsonWriteUtils.NULL_BYTES, 0, buf, pos, JsonWriteUtils.NULL_BYTES.length);
        pos += JsonWriteUtils.NULL_BYTES.length;
    }

    private void enterContainer() {
        depth++;
        if (depth >= MAX_DEPTH) {
            throw new SerializationException("Maximum nesting depth exceeded: " + MAX_DEPTH);
        }
        needsComma[depth] = false;
    }

    private void ensureCapacity(int needed) {
        if (pos + needed > buf.length) {
            grow(needed);
        }
    }

    // Separate cold grow method helps the JIT inline ensureCapacity's fast path
    private void grow(int needed) {
        // Writing into a ByteBufferOutputStream: add what is written and let the stream grow its own array.
        if (direct != null) {
            commitToDirect();
            buf = direct.reserve(needed);
            return;
        }
        // With a sink, write out what is buffered and reuse the buffer; grow only if one value does not fit in it.
        // Callers write at pos after this returns and never refer back to earlier positions, so draining is safe.
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

    /**
     * Writes with the exact ASCII reservation, widening only when needed.
     */
    private void writeQuotedStringExact(String value) {
        writeQuotedStringExact(value, 0);
    }

    private void writeQuotedStringExact(String value, int trailingBytes) {
        int next = JsonWriteUtils.tryWriteQuotedAscii(buf, pos, value);
        if (next < 0) {
            ensureCapacity(JsonWriteUtils.maxQuotedStringBytes(value) + trailingBytes);
            next = JsonWriteUtils.writeQuotedStringGeneral(buf, pos, value);
        }
        pos = next;
    }

    private void writeBlobValue(ByteBuffer value) {
        int remaining = value.remaining();
        ensureCapacity(ByteBufferUtils.base64EncodedSize(remaining) + 2);
        if (remaining >= 32) {
            byte[] b = buf;
            int p = pos;
            b[p++] = '"';
            p += ByteBufferUtils.base64EncodeTo(value, b, p, base64Scratch(remaining));
            b[p++] = '"';
            pos = p;
            return;
        }
        pos = JsonWriteUtils.writeBase64String(buf, pos, value);
    }

    private byte[] base64Scratch(int dataLen) {
        int capacity = ByteBufferUtils.base64EncodedSize(dataLen);
        byte[] scratch = base64Scratch;
        if (scratch == null || scratch.length < capacity) {
            scratch = new byte[capacity];
            base64Scratch = scratch;
        }
        return scratch;
    }

    private void writeCommaIfNeeded() {
        if (needsComma[depth]) {
            if (pos >= buf.length) {
                grow(1);
            }
            buf[pos++] = ',';
        } else {
            needsComma[depth] = true;
        }
    }

    /**
     * Resolves the pre-computed field name bytes for a schema member.
     */
    private byte[] resolveFieldNameBytes(Schema schema) {
        byte[][] table = currentFieldNameTable;
        int idx = schema.memberIndex();
        if (table != null && idx >= 0 && idx < table.length && table[idx] != null) {
            return table[idx];
        }
        SmithyJsonSchemaExtensions.NativeJsonExtension ext = schema.getExtension(SmithyJsonSchemaExtensions.KEY);
        return useJsonName ? ext.jsonNameBytes() : ext.memberNameBytes();
    }

    /**
     * Writes a comma (if needed) and the field name bytes. The caller must have ensured capacity for
     * {@code nameBytes.length + 1} plus the value bytes.
     */
    private void writeFieldNameBytesUnchecked(byte[] nameBytes) {
        if (needsComma[depth]) {
            buf[pos++] = ',';
        } else {
            needsComma[depth] = true;
        }
        System.arraycopy(nameBytes, 0, buf, pos, nameBytes.length);
        pos += nameBytes.length;
    }

    /**
     * Resolves the field name, ensures capacity for the name and a comma, and writes them. Used by the writes whose
     * values check their own capacity.
     */
    private void writeFieldNameBytes(Schema schema) {
        byte[] nameBytes = resolveFieldNameBytes(schema);
        int needed = nameBytes.length + 1;
        if (pos + needed > buf.length) {
            grow(needed);
        }
        writeFieldNameBytesUnchecked(nameBytes);
    }

    private static final class JsonStripedPool extends StripedPool<SmithyJsonSerializer, JsonSettings> {
        @Override
        protected SmithyJsonSerializer create(JsonSettings settings) {
            return new SmithyJsonSerializer(settings);
        }

        @Override
        protected boolean canPool(SmithyJsonSerializer s) {
            return s.buf != null;
        }

        @Override
        protected void prepareForPool(SmithyJsonSerializer s) {
            if (s.buf.length > MAX_CACHEABLE_BUF) {
                s.buf = new byte[DEFAULT_BUF_SIZE];
            }
            if (s.base64Scratch != null && s.base64Scratch.length > MAX_CACHEABLE_BUF) {
                s.base64Scratch = null;
            }
        }

        @Override
        protected boolean reset(SmithyJsonSerializer s, JsonSettings settings) {
            if (!s.settings.equals(settings)) {
                return false;
            }
            s.pos = 0;
            s.depth = 0;
            s.currentFieldNameTable = null;
            return true;
        }
    }

    /**
     * Writes structure members: each write emits the comma and the field name before the value. The fixed-size writes
     * check capacity for the name, comma and largest value once.
     */
    private final class StructSerializer implements ShapeSerializer {
        @Override
        public void writeBoolean(Schema schema, boolean value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 1 + 5);
            writeFieldNameBytesUnchecked(nameBytes);
            pos = NumberCodec.writeBoolean(buf, pos, value);
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 1 + 4);
            writeFieldNameBytesUnchecked(nameBytes);
            pos = JsonWriteUtils.writeInt(buf, pos, value);
        }

        @Override
        public void writeShort(Schema schema, short value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 1 + 6);
            writeFieldNameBytesUnchecked(nameBytes);
            pos = JsonWriteUtils.writeInt(buf, pos, value);
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 1 + 11);
            writeFieldNameBytesUnchecked(nameBytes);
            pos = JsonWriteUtils.writeInt(buf, pos, value);
        }

        @Override
        public void writeLong(Schema schema, long value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 1 + 20);
            writeFieldNameBytesUnchecked(nameBytes);
            pos = JsonWriteUtils.writeLong(buf, pos, value);
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 1 + NumberCodec.FLOAT_MAX_BYTES);
            writeFieldNameBytesUnchecked(nameBytes);
            pos = NumberCodec.writeFloatFullQuoted(buf, pos, value);
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 1 + NumberCodec.DOUBLE_MAX_BYTES);
            writeFieldNameBytesUnchecked(nameBytes);
            pos = NumberCodec.writeDoubleFullQuoted(buf, pos, value);
        }

        @Override
        public void writeNull(Schema schema) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 1 + 4);
            writeFieldNameBytesUnchecked(nameBytes);
            System.arraycopy(JsonWriteUtils.NULL_BYTES, 0, buf, pos, JsonWriteUtils.NULL_BYTES.length);
            pos += JsonWriteUtils.NULL_BYTES.length;
        }

        // Variable-size and recursive types: delegate to the outer class (separate capacity checks)

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            writeFieldNameBytes(schema);
            SmithyJsonSerializer.this.writeBigInteger(schema, value);
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            writeFieldNameBytes(schema);
            SmithyJsonSerializer.this.writeBigDecimal(schema, value);
        }

        @Override
        public void writeString(Schema schema, String value) {
            byte[] nameBytes = resolveFieldNameBytes(schema);
            ensureCapacity(nameBytes.length + 1 + value.length() + 2);
            writeFieldNameBytesUnchecked(nameBytes);
            writeQuotedStringExact(value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            writeFieldNameBytes(schema);
            SmithyJsonSerializer.this.writeBlob(schema, value);
        }

        @Override
        public void writeBlob(Schema schema, byte[] value) {
            writeFieldNameBytes(schema);
            SmithyJsonSerializer.this.writeBlob(schema, value);
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            writeFieldNameBytes(schema);
            SmithyJsonSerializer.this.writeTimestamp(schema, value);
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            writeFieldNameBytes(schema);
            SmithyJsonSerializer.this.writeStruct(schema, struct);
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            writeFieldNameBytes(schema);
            SmithyJsonSerializer.this.writeList(schema, listState, size, consumer);
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            writeFieldNameBytes(schema);
            SmithyJsonSerializer.this.writeMap(schema, mapState, size, consumer);
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            writeFieldNameBytes(schema);
            SmithyJsonSerializer.this.writeDocument(schema, value);
        }
    }

    /**
     * Writes list elements: each write emits the separating comma before the value.
     */
    private final class ListElementSerializer implements ShapeSerializer {
        @Override
        public void writeBoolean(Schema schema, boolean value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeBoolean(schema, value);
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeByte(schema, value);
        }

        @Override
        public void writeShort(Schema schema, short value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeShort(schema, value);
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeInteger(schema, value);
        }

        @Override
        public void writeLong(Schema schema, long value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeLong(schema, value);
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeFloat(schema, value);
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeDouble(schema, value);
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeBigInteger(schema, value);
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeBigDecimal(schema, value);
        }

        @Override
        public void writeString(Schema schema, String value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeString(schema, value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeBlob(schema, value);
        }

        @Override
        public void writeBlob(Schema schema, byte[] value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeBlob(schema, value);
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeTimestamp(schema, value);
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeStruct(schema, struct);
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeList(schema, listState, size, consumer);
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeMap(schema, mapState, size, consumer);
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeDocument(schema, value);
        }

        @Override
        public void writeNull(Schema schema) {
            writeCommaIfNeeded();
            SmithyJsonSerializer.this.writeNull(schema);
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
            SmithyJsonSerializer.this.writeStruct(schema, struct, discriminator);
        }
    }

    private final class SmithyMapSerializer implements MapSerializer {
        @Override
        public <T> void writeEntry(Schema keySchema, String key, T state, BiConsumer<T, ShapeSerializer> valueSerializer) {
            writeCommaIfNeeded();
            ensureCapacity(key.length() + 2 + 1);
            writeQuotedStringExact(key, 1);
            buf[pos++] = ':';
            valueSerializer.accept(state, SmithyJsonSerializer.this);
        }
    }
}
