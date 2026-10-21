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
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.ByteBufferUtils;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.codecs.commons.internal.NumberCodec;
import software.amazon.awssdk.codecs.commons.internal.StripedPool;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * JSON serializer that writes directly to a byte array buffer, using the pre-computed field name bytes of
 * {@link SmithyJsonSchemaExtensions}. Output is compact (no whitespace); {@link JsonPrettyPrinter} indents it.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.smithy.SmithyJsonSerializer}. Documents are written by
 * type over the SDK {@link Document} accessors (see {@link #writeDocument}).
 *
 * <p>SDK deviation: with a sink, a full buffer is written to the sink before the buffer grows, so the buffer stays at
 * its initial size unless a single value needs more. smithy-java grows the buffer to the whole document and writes it
 * to the sink on {@link #close}, which allocates every intermediate buffer and then copies the document again into a
 * sink that callers such as {@code HttpClientProtocol} have already sized for it. Because of that, a serializer with a
 * sink starts with a 1 KB buffer instead of smithy-java's 8 KB; the sink-less serializer keeps 8 KB.
 */
@SdkInternalApi
final class SmithyJsonSerializer implements ShapeSerializer {

    private static final int MAX_DEPTH = 64;
    // The sink-less serializer holds the whole document, and Codec.serialize pools it, so it starts larger.
    private static final int DEFAULT_BUF_SIZE = 8192;
    private static final int MAX_CACHEABLE_BUF = DEFAULT_BUF_SIZE * 16;
    // A serializer with a sink is allocated per call and drains its buffer to the sink when full (see grow), so a
    // small buffer only adds sink writes for large documents.
    private static final int SINK_BUF_SIZE = 1024;

    private static final StripedPool<SmithyJsonSerializer, JsonSettings> POOL = new JsonStripedPool();

    private final OutputStream sink;
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
        this.sink = sink;
        this.settings = settings;
        this.useJsonName = settings.useJsonName();
        this.buf = new byte[sink == null ? DEFAULT_BUF_SIZE : SINK_BUF_SIZE];
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
            buf = null;
        } catch (IOException e) {
            throw new SerializationException(e);
        }
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
        ensureCapacity(2);
        buf[pos++] = '{';
        enterContainer();

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
     * Writes a document by its type: scalars as their JSON values (blobs as base64 strings and timestamps in the
     * default format), lists as arrays, maps as objects, and a null document as {@code null}.
     *
     * <p>smithy-java writes documents through {@code Document.serializeContents}, and writes structure documents with a
     * {@code __type} member when {@link JsonSettings#serializeTypeInDocuments()} is on. Writing structure documents
     * and their discriminators is not ported yet (task 11.9), so structure and union documents are rejected.
     *
     * @throws SerializationException for a structure or union document
     */
    @Override
    public void writeDocument(Schema schema, Document value) {
        if (value == null) {
            writeNull(schema);
            return;
        }
        switch (value.type()) {
            case BOOLEAN:
                writeBoolean(PreludeSchemas.BOOLEAN, value.asBoolean());
                break;
            case BYTE:
                writeByte(PreludeSchemas.BYTE, value.asByte());
                break;
            case SHORT:
                writeShort(PreludeSchemas.SHORT, value.asShort());
                break;
            case INTEGER:
            case INT_ENUM:
                writeInteger(PreludeSchemas.INTEGER, value.asInteger());
                break;
            case LONG:
                writeLong(PreludeSchemas.LONG, value.asLong());
                break;
            case FLOAT:
                writeFloat(PreludeSchemas.FLOAT, value.asFloat());
                break;
            case DOUBLE:
                writeDouble(PreludeSchemas.DOUBLE, value.asDouble());
                break;
            case BIG_INTEGER:
                writeBigInteger(PreludeSchemas.BIG_INTEGER, value.asBigInteger());
                break;
            case BIG_DECIMAL:
                writeBigDecimal(PreludeSchemas.BIG_DECIMAL, value.asBigDecimal());
                break;
            case STRING:
            case ENUM:
                writeString(PreludeSchemas.STRING, value.asString());
                break;
            case BLOB:
                writeBlob(PreludeSchemas.BLOB, value.asBlob());
                break;
            case TIMESTAMP:
                writeTimestamp(PreludeSchemas.TIMESTAMP, value.asTimestamp());
                break;
            case LIST:
                List<Document> elements = value.asList();
                writeList(PreludeSchemas.DOCUMENT, elements, elements.size(), SmithyJsonSerializer::writeDocumentElements);
                break;
            case MAP:
                Map<String, Document> entries = value.asMap();
                writeMap(PreludeSchemas.DOCUMENT, entries, entries.size(), SmithyJsonSerializer::writeDocumentEntries);
                break;
            default:
                throw new SerializationException("Unsupported document type: " + value.type());
        }
    }

    @Override
    public void writeNull(Schema schema) {
        ensureCapacity(JsonWriteUtils.NULL_BYTES.length);
        System.arraycopy(JsonWriteUtils.NULL_BYTES, 0, buf, pos, JsonWriteUtils.NULL_BYTES.length);
        pos += JsonWriteUtils.NULL_BYTES.length;
    }

    private static void writeDocumentElements(List<Document> elements, ShapeSerializer serializer) {
        for (Document element : elements) {
            serializer.writeDocument(PreludeSchemas.DOCUMENT, element);
        }
    }

    private static void writeDocumentEntries(Map<String, Document> entries, MapSerializer serializer) {
        for (Map.Entry<String, Document> entry : entries.entrySet()) {
            serializer.writeEntry(PreludeSchemas.STRING, entry.getKey(), entry.getValue(),
                                  (v, s) -> s.writeDocument(PreludeSchemas.DOCUMENT, v));
        }
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
