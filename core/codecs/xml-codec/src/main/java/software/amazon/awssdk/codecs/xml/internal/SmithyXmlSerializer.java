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

package software.amazon.awssdk.codecs.xml.internal;

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
import software.amazon.awssdk.codecs.commons.ByteBufferUtils;
import software.amazon.awssdk.codecs.commons.internal.NumberCodec;
import software.amazon.awssdk.codecs.commons.internal.StripedPool;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.InterceptingSerializer;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.XmlNameTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;

/**
 * The native XML serializer: writes UTF-8 XML directly into a pooled byte buffer, using the pre-encoded names of
 * {@link XmlSchemaExtensions}. Instances are pooled ({@link StripedPool}) and acquired through
 * {@link LazyXmlSerializer}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.xml.SmithyXmlSerializer}.
 *
 * <p>SDK deviation: with a sink, a full buffer is written to the sink before the buffer grows, so the buffer stays at
 * its initial 4 KB unless a single value needs more. smithy-java grows the buffer to the whole document and writes it
 * to the sink on {@link #close}, which allocates every intermediate buffer and then copies the document again into a
 * sink that callers such as {@code HttpClientProtocol} have already sized for it. The initial size is unchanged
 * because the serializer and its buffer are pooled, so the buffer is not allocated per call.
 */
@SdkInternalApi
final class SmithyXmlSerializer extends InterceptingSerializer {

    private static final int DEFAULT_BUF_SIZE = 4096;
    private static final int MAX_CACHEABLE_BUF = DEFAULT_BUF_SIZE * 4;

    private static final StripedPool<SmithyXmlSerializer, AcquireContext> POOL = new XmlStripedPool();

    private final StructElementSerializer structElementSerializer = new StructElementSerializer();
    private final StructAttributeSerializer structAttributeSerializer = new StructAttributeSerializer();
    private final ValueSerializer valueSerializer = new ValueSerializer();

    private byte[] buf;
    private byte[] base64Scratch;
    private int pos;
    private OutputStream sink;
    private XmlNamespaceTrait defaultNamespace;
    private XmlInfo xmlInfo;

    private byte[][] currentNameTable;
    private boolean[] currentIsAttributeTable;
    private boolean[] currentIsFlattenedTable;

    // When true, the opening tag is not yet closed with '>'.
    // writeStruct will close it after writing attributes; scalar writes close it immediately.
    private boolean pendingClose;

    private SmithyXmlSerializer(XmlNamespaceTrait defaultNamespace, XmlInfo xmlInfo) {
        this.defaultNamespace = defaultNamespace;
        this.xmlInfo = xmlInfo;
        this.buf = new byte[DEFAULT_BUF_SIZE];
        this.pos = 0;
    }

    static SmithyXmlSerializer acquire(XmlNamespaceTrait defaultNamespace, XmlInfo xmlInfo, OutputStream sink) {
        return POOL.acquire(new AcquireContext(defaultNamespace, xmlInfo, sink));
    }

    static void release(SmithyXmlSerializer serializer) {
        POOL.release(serializer);
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
        } finally {
            release(this);
        }
    }

    private void ensureCapacity(int needed) {
        if (pos + needed > buf.length) {
            grow(needed);
        }
    }

    // Separate cold grow method helps the JIT inline ensureCapacity's fast path
    private void grow(int needed) {
        // With a sink, write out what is buffered and reuse the buffer; grow only if one value does not fit in it.
        // Every write appends at pos after this returns; an open start tag is closed by appending '>' later, so no
        // write goes back to an earlier position and draining is safe.
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

    private byte[] base64Scratch(int dataLen) {
        int capacity = ByteBufferUtils.base64EncodedSize(dataLen);
        byte[] scratch = base64Scratch;
        if (scratch == null || scratch.length < capacity) {
            scratch = new byte[capacity];
            base64Scratch = scratch;
        }
        return scratch;
    }

    @Override
    protected ShapeSerializer before(Schema schema) {
        byte[] nameBytes = resolveTopLevelName(schema);
        byte[] nsBytes = resolveTopLevelNamespace(schema);
        int nsLen = nsBytes != null ? nsBytes.length : 0;
        ensureCapacity(1 + nameBytes.length + nsLen);
        buf[pos++] = '<';
        System.arraycopy(nameBytes, 0, buf, pos, nameBytes.length);
        pos += nameBytes.length;
        if (nsBytes != null) {
            System.arraycopy(nsBytes, 0, buf, pos, nsBytes.length);
            pos += nsBytes.length;
        }
        pendingClose = true;
        return valueSerializer;
    }

    @Override
    protected void after(Schema schema) {
        writeCloseTag(resolveTopLevelName(schema));
    }

    private byte[] resolveTopLevelName(Schema schema) {
        XmlNameTrait trait = xmlInfo.xmlNameTrait(schema);
        if (trait != null) {
            return trait.getValue().getBytes(StandardCharsets.UTF_8);
        }
        XmlSchemaExtensions.StructExtension ext = getStructExtension(schema);
        if (ext != null) {
            return ext.structNameBytes();
        }
        if (schema.isMember()) {
            return schema.memberTarget().id().getName().getBytes(StandardCharsets.UTF_8);
        }
        return schema.id().getName().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] resolveTopLevelNamespace(Schema schema) {
        XmlSchemaExtensions.StructExtension ext = getStructExtension(schema);
        if (ext != null && ext.namespaceBytes() != null) {
            return ext.namespaceBytes();
        }
        XmlNamespaceTrait ns = schema.getTrait(TraitKey.XML_NAMESPACE_TRAIT);
        if (ns == null) {
            ns = defaultNamespace;
        }
        if (ns != null) {
            return XmlSchemaExtensions.buildNamespaceBytes(ns);
        }
        return null;
    }

    private XmlSchemaExtensions.StructExtension getStructExtension(Schema schema) {
        Schema target = schema.isMember() ? schema.memberTarget() : schema;
        XmlSchemaExtensions.XmlExtension ext = xmlInfo.extension(target);
        if (ext instanceof XmlSchemaExtensions.StructExtension) {
            return (XmlSchemaExtensions.StructExtension) ext;
        }
        return null;
    }

    private void writeOpenTag(byte[] nameBytes) {
        ensureCapacity(1 + nameBytes.length + 1);
        buf[pos++] = '<';
        System.arraycopy(nameBytes, 0, buf, pos, nameBytes.length);
        pos += nameBytes.length;
        buf[pos++] = '>';
    }

    private void writeCloseTag(byte[] nameBytes) {
        ensureCapacity(3 + nameBytes.length);
        buf[pos++] = '<';
        buf[pos++] = '/';
        System.arraycopy(nameBytes, 0, buf, pos, nameBytes.length);
        pos += nameBytes.length;
        buf[pos++] = '>';
    }

    private byte[] resolveMemberNameBytes(Schema schema) {
        byte[][] table = currentNameTable;
        int idx = schema.memberIndex();
        if (table != null && idx >= 0 && idx < table.length && table[idx] != null) {
            return table[idx];
        }
        XmlSchemaExtensions.XmlExtension ext = xmlInfo.extension(schema);
        if (ext instanceof XmlSchemaExtensions.MemberExtension) {
            return ((XmlSchemaExtensions.MemberExtension) ext).nameBytes();
        }
        return schema.memberName().getBytes(StandardCharsets.UTF_8);
    }

    private void closePendingTag() {
        if (pendingClose) {
            ensureCapacity(1);
            buf[pos++] = '>';
            pendingClose = false;
        }
    }

    private byte[] resolveMemberNamespaceBytes(Schema schema) {
        XmlSchemaExtensions.XmlExtension ext = xmlInfo.extension(schema);
        if (ext instanceof XmlSchemaExtensions.MemberExtension) {
            return ((XmlSchemaExtensions.MemberExtension) ext).namespaceBytes();
        }
        XmlNamespaceTrait ns = schema.getDirectTrait(TraitKey.XML_NAMESPACE_TRAIT);
        if (ns != null) {
            return XmlSchemaExtensions.buildNamespaceBytes(ns);
        }
        return null;
    }

    private String formatTimestamp(Schema schema, Instant value) {
        return xmlInfo.timestampFormatter(schema).writeString(value);
    }

    private boolean isAttributeMember(int idx) {
        return currentIsAttributeTable != null && idx >= 0 && idx < currentIsAttributeTable.length
               && currentIsAttributeTable[idx];
    }

    private boolean isFlattenedMember(int idx) {
        return currentIsFlattenedTable != null && idx >= 0 && idx < currentIsFlattenedTable.length
               && currentIsFlattenedTable[idx];
    }

    private static final class AcquireContext {
        private final XmlNamespaceTrait defaultNamespace;
        private final XmlInfo xmlInfo;
        private final OutputStream sink;

        private AcquireContext(XmlNamespaceTrait defaultNamespace, XmlInfo xmlInfo, OutputStream sink) {
            this.defaultNamespace = defaultNamespace;
            this.xmlInfo = xmlInfo;
            this.sink = sink;
        }
    }

    private static final class XmlStripedPool extends StripedPool<SmithyXmlSerializer, AcquireContext> {
        @Override
        protected SmithyXmlSerializer create(AcquireContext ctx) {
            SmithyXmlSerializer s = new SmithyXmlSerializer(ctx.defaultNamespace, ctx.xmlInfo);
            s.sink = ctx.sink;
            return s;
        }

        @Override
        protected void cleanup(SmithyXmlSerializer s) {
            s.sink = null;
        }

        @Override
        protected boolean canPool(SmithyXmlSerializer s) {
            return s.buf != null;
        }

        @Override
        protected void prepareForPool(SmithyXmlSerializer s) {
            if (s.buf.length > MAX_CACHEABLE_BUF) {
                s.buf = new byte[DEFAULT_BUF_SIZE];
            }
            if (s.base64Scratch != null && s.base64Scratch.length > MAX_CACHEABLE_BUF) {
                s.base64Scratch = null;
            }
        }

        @Override
        protected boolean reset(SmithyXmlSerializer s, AcquireContext ctx) {
            s.pos = 0;
            s.sink = ctx.sink;
            s.defaultNamespace = ctx.defaultNamespace;
            s.xmlInfo = ctx.xmlInfo;
            s.currentNameTable = null;
            s.currentIsAttributeTable = null;
            s.currentIsFlattenedTable = null;
            s.pendingClose = false;
            return true;
        }
    }

    private final class StructElementSerializer extends InterceptingSerializer {
        @Override
        protected ShapeSerializer before(Schema schema) {
            int idx = schema.memberIndex();
            if (isAttributeMember(idx)) {
                return ShapeSerializer.nullSerializer();
            }
            if (isFlattenedMember(idx)) {
                return valueSerializer;
            }
            byte[] nameBytes = resolveMemberNameBytes(schema);
            ensureCapacity(1 + nameBytes.length);
            buf[pos++] = '<';
            System.arraycopy(nameBytes, 0, buf, pos, nameBytes.length);
            pos += nameBytes.length;
            byte[] nsBytes = resolveMemberNamespaceBytes(schema);
            if (nsBytes != null) {
                ensureCapacity(nsBytes.length);
                System.arraycopy(nsBytes, 0, buf, pos, nsBytes.length);
                pos += nsBytes.length;
            }
            pendingClose = true;
            return valueSerializer;
        }

        @Override
        protected void after(Schema schema) {
            int idx = schema.memberIndex();
            if (isAttributeMember(idx) || isFlattenedMember(idx)) {
                return;
            }
            writeCloseTag(resolveMemberNameBytes(schema));
        }
    }

    private final class StructAttributeSerializer extends InterceptingSerializer {
        @Override
        protected ShapeSerializer before(Schema schema) {
            if (isAttributeMember(schema.memberIndex())) {
                return new InlineAttributeSerializer(schema);
            }
            return ShapeSerializer.nullSerializer();
        }
    }

    private final class InlineAttributeSerializer extends SpecificShapeSerializer {
        private final byte[] nameBytes;

        InlineAttributeSerializer(Schema schema) {
            this.nameBytes = resolveMemberNameBytes(schema);
        }

        private void writeAttrPrefix() {
            ensureCapacity(1 + nameBytes.length + 2);
            buf[pos++] = ' ';
            System.arraycopy(nameBytes, 0, buf, pos, nameBytes.length);
            pos += nameBytes.length;
            buf[pos++] = '=';
            buf[pos++] = '"';
        }

        private void writeAttrSuffix() {
            ensureCapacity(1);
            buf[pos++] = '"';
        }

        @Override
        public void writeString(Schema schema, String value) {
            writeAttrPrefix();
            ensureCapacity(XmlWriteUtils.maxEscapedAttributeBytes(value));
            pos = XmlWriteUtils.writeEscapedAttribute(buf, pos, value);
            writeAttrSuffix();
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            writeAttrPrefix();
            ensureCapacity(5);
            pos = NumberCodec.writeBoolean(buf, pos, value);
            writeAttrSuffix();
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            writeInteger(schema, value);
        }

        @Override
        public void writeShort(Schema schema, short value) {
            writeInteger(schema, value);
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            writeAttrPrefix();
            ensureCapacity(11);
            pos = NumberCodec.writeInt(buf, pos, value);
            writeAttrSuffix();
        }

        @Override
        public void writeLong(Schema schema, long value) {
            writeAttrPrefix();
            ensureCapacity(20);
            pos = NumberCodec.writeLong(buf, pos, value);
            writeAttrSuffix();
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            writeAttrPrefix();
            ensureCapacity(NumberCodec.FLOAT_MAX_BYTES);
            pos = NumberCodec.writeFloatFull(buf, pos, value);
            writeAttrSuffix();
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            writeAttrPrefix();
            ensureCapacity(NumberCodec.DOUBLE_MAX_BYTES);
            pos = NumberCodec.writeDoubleFull(buf, pos, value);
            writeAttrSuffix();
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            writeString(schema, value.toString());
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            writeString(schema, value.toString());
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            writeString(schema, formatTimestamp(schema, value));
        }

        @Override
        public void writeNull(Schema schema) {
        }
    }

    private final class ValueSerializer implements ShapeSerializer {
        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            XmlSchemaExtensions.StructExtension ext = getStructExtension(schema);
            byte[][] savedNameTable = currentNameTable;
            boolean[] savedIsAttrTable = currentIsAttributeTable;
            boolean[] savedIsFlatTable = currentIsFlattenedTable;

            if (ext != null) {
                currentNameTable = ext.nameTable();
                currentIsAttributeTable = ext.isAttributeTable();
                currentIsFlattenedTable = ext.isFlattenedTable();
                if (ext.hasAttributes()) {
                    struct.serializeMembers(structAttributeSerializer);
                }
            } else {
                currentNameTable = null;
                currentIsAttributeTable = null;
                currentIsFlattenedTable = null;
            }

            closePendingTag();
            struct.serializeMembers(structElementSerializer);

            currentNameTable = savedNameTable;
            currentIsAttributeTable = savedIsAttrTable;
            currentIsFlattenedTable = savedIsFlatTable;
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            closePendingTag();
            XmlSchemaExtensions.XmlExtension ext = xmlInfo.extension(schema);
            byte[] memberNameBytes;
            if (ext instanceof XmlSchemaExtensions.ListExtension) {
                memberNameBytes = ((XmlSchemaExtensions.ListExtension) ext).memberNameBytes();
            } else {
                memberNameBytes = xmlInfo.getListInfo(schema).memberName.getBytes(StandardCharsets.UTF_8);
            }
            byte[] memberNsBytes = null;
            XmlNamespaceTrait ns = schema.listMember().getDirectTrait(TraitKey.XML_NAMESPACE_TRAIT);
            if (ns != null) {
                memberNsBytes = XmlSchemaExtensions.buildNamespaceBytes(ns);
            }
            consumer.accept(listState, new ListItemSerializer(memberNameBytes, memberNsBytes));
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            closePendingTag();
            XmlSchemaExtensions.XmlExtension ext = xmlInfo.extension(schema);
            byte[] entryBytes;
            byte[] keyBytes;
            byte[] valueBytes;
            if (ext instanceof XmlSchemaExtensions.MapExtension) {
                XmlSchemaExtensions.MapExtension mapExt = (XmlSchemaExtensions.MapExtension) ext;
                entryBytes = mapExt.entryNameBytes();
                keyBytes = mapExt.keyNameBytes();
                valueBytes = mapExt.valueNameBytes();
            } else {
                XmlInfo.MapMemberInfo info = xmlInfo.getMapInfo(schema);
                entryBytes = info.entryName.getBytes(StandardCharsets.UTF_8);
                keyBytes = info.keyName.getBytes(StandardCharsets.UTF_8);
                valueBytes = info.valueName.getBytes(StandardCharsets.UTF_8);
            }

            byte[] keyNsBytes = null;
            byte[] valueNsBytes = null;
            XmlNamespaceTrait keyNs = schema.mapKeyMember().getDirectTrait(TraitKey.XML_NAMESPACE_TRAIT);
            if (keyNs != null) {
                keyNsBytes = XmlSchemaExtensions.buildNamespaceBytes(keyNs);
            }
            XmlNamespaceTrait valueNs = schema.mapValueMember().getDirectTrait(TraitKey.XML_NAMESPACE_TRAIT);
            if (valueNs != null) {
                valueNsBytes = XmlSchemaExtensions.buildNamespaceBytes(valueNs);
            }

            consumer.accept(mapState, new XmlMapEntrySerializer(entryBytes, keyBytes, valueBytes, keyNsBytes,
                                                                valueNsBytes));
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            closePendingTag();
            ensureCapacity(5);
            pos = NumberCodec.writeBoolean(buf, pos, value);
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            closePendingTag();
            ensureCapacity(4);
            pos = NumberCodec.writeInt(buf, pos, value);
        }

        @Override
        public void writeShort(Schema schema, short value) {
            closePendingTag();
            ensureCapacity(6);
            pos = NumberCodec.writeInt(buf, pos, value);
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            closePendingTag();
            ensureCapacity(11);
            pos = NumberCodec.writeInt(buf, pos, value);
        }

        @Override
        public void writeLong(Schema schema, long value) {
            closePendingTag();
            ensureCapacity(20);
            pos = NumberCodec.writeLong(buf, pos, value);
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            closePendingTag();
            ensureCapacity(NumberCodec.FLOAT_MAX_BYTES);
            pos = NumberCodec.writeFloatFull(buf, pos, value);
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            closePendingTag();
            ensureCapacity(NumberCodec.DOUBLE_MAX_BYTES);
            pos = NumberCodec.writeDoubleFull(buf, pos, value);
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            closePendingTag();
            ensureCapacity(value.bitLength() / 3 + 2);
            pos = NumberCodec.writeBigInteger(buf, pos, value);
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            closePendingTag();
            String s = value.toString();
            int len = s.length();
            ensureCapacity(len);
            // BigDecimal.toString is ASCII.
            for (int i = 0; i < len; i++) {
                buf[pos++] = (byte) s.charAt(i);
            }
        }

        @Override
        public void writeString(Schema schema, String value) {
            closePendingTag();
            writeTextContent(value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            closePendingTag();
            int remaining = value.remaining();
            ensureCapacity(ByteBufferUtils.base64EncodedSize(remaining));
            pos += ByteBufferUtils.base64EncodeTo(value, buf, pos, base64Scratch(remaining));
        }

        @Override
        public void writeBlob(Schema schema, byte[] value) {
            // The bytes are only read here, so a writable view of the whole array lets base64 read it without copying.
            writeBlob(schema, ByteBuffer.wrap(value));
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            closePendingTag();
            writeTextContent(formatTimestamp(schema, value));
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            closePendingTag();
        }

        @Override
        public void writeNull(Schema schema) {
            closePendingTag();
        }

        private void writeTextContent(String value) {
            ensureCapacity(XmlWriteUtils.maxEscapedTextBytes(value));
            pos = XmlWriteUtils.writeEscapedText(buf, pos, value);
        }
    }

    private final class ListItemSerializer extends InterceptingSerializer {
        private final byte[] memberNameBytes;
        private final byte[] nsBytes;

        ListItemSerializer(byte[] memberNameBytes, byte[] nsBytes) {
            this.memberNameBytes = memberNameBytes;
            this.nsBytes = nsBytes;
        }

        @Override
        protected ShapeSerializer before(Schema schema) {
            int nsLen = nsBytes != null ? nsBytes.length : 0;
            ensureCapacity(1 + memberNameBytes.length + nsLen);
            buf[pos++] = '<';
            System.arraycopy(memberNameBytes, 0, buf, pos, memberNameBytes.length);
            pos += memberNameBytes.length;
            if (nsBytes != null) {
                System.arraycopy(nsBytes, 0, buf, pos, nsBytes.length);
                pos += nsBytes.length;
            }
            pendingClose = true;
            return valueSerializer;
        }

        @Override
        protected void after(Schema schema) {
            writeCloseTag(memberNameBytes);
        }
    }

    private final class XmlMapEntrySerializer implements MapSerializer {
        private final byte[] entryBytes;
        private final byte[] keyBytes;
        private final byte[] valueBytes;
        private final byte[] keyNsBytes;
        private final byte[] valueNsBytes;

        XmlMapEntrySerializer(byte[] entryBytes, byte[] keyBytes, byte[] valueBytes, byte[] keyNsBytes,
                              byte[] valueNsBytes) {
            this.entryBytes = entryBytes;
            this.keyBytes = keyBytes;
            this.valueBytes = valueBytes;
            this.keyNsBytes = keyNsBytes;
            this.valueNsBytes = valueNsBytes;
        }

        @Override
        public <T> void writeEntry(Schema keySchema, String key, T state, BiConsumer<T, ShapeSerializer> valueSerializer) {
            writeOpenTag(entryBytes);
            writeTagWithNs(keyBytes, keyNsBytes);
            ensureCapacity(1 + XmlWriteUtils.maxEscapedTextBytes(key));
            buf[pos++] = '>';
            pos = XmlWriteUtils.writeEscapedText(buf, pos, key);
            writeCloseTag(keyBytes);
            writeTagWithNs(valueBytes, valueNsBytes);
            pendingClose = true;
            valueSerializer.accept(state, SmithyXmlSerializer.this.valueSerializer);
            writeCloseTag(valueBytes);
            writeCloseTag(entryBytes);
        }

        // Writes '<name' and the namespace declaration, leaving the tag open.
        private void writeTagWithNs(byte[] nameBytes, byte[] nsBytes) {
            int nsLen = nsBytes != null ? nsBytes.length : 0;
            ensureCapacity(1 + nameBytes.length + nsLen);
            buf[pos++] = '<';
            System.arraycopy(nameBytes, 0, buf, pos, nameBytes.length);
            pos += nameBytes.length;
            if (nsBytes != null) {
                System.arraycopy(nsBytes, 0, buf, pos, nsBytes.length);
                pos += nsBytes.length;
            }
        }
    }
}
