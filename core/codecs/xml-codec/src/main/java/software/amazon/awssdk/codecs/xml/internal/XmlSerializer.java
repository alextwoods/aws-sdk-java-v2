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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.function.BiConsumer;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.ByteBufferUtils;
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
 * The StAX XML serializer, writing through an {@link XMLStreamWriter}. The codec uses the native
 * {@link SmithyXmlSerializer} unless the StAX implementation is selected.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.xml.XmlSerializer}. Element text writes carriage returns as
 * {@code &#xD;} character references so that they read back as themselves.
 */
@SdkInternalApi
final class XmlSerializer extends InterceptingSerializer {

    private final XmlInfo xmlInfo;
    private final XMLStreamWriter writer;
    private final XmlNamespaceTrait defaultNamespace;
    private final NonFlattenedMemberSerializer nonFlattenedMemberSerializer = new NonFlattenedMemberSerializer();
    private final ValueSerializer valueSerializer = new ValueSerializer();
    private final StructMemberSerializer structMemberSerializer = new StructMemberSerializer();
    private final StructAttributeSerializer structAttributeSerializer = new StructAttributeSerializer();
    private final AttributeSerializer attributeSerializer = new AttributeSerializer();

    XmlSerializer(XMLStreamWriter writer, XmlInfo xmlInfo, XmlNamespaceTrait defaultNamespace) {
        this.writer = writer;
        this.xmlInfo = xmlInfo;
        this.defaultNamespace = defaultNamespace;
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (XMLStreamException e) {
            throw new SerializationException(e);
        }
    }

    // Handles writing top-level shapes that are not members. The element uses xmlName or the shape name.
    @Override
    protected ShapeSerializer before(Schema schema) {
        try {
            // Top-level members are things like httpPayload members. They peek-through to the target shape xmlName.
            String xmlName;
            XmlNameTrait trait = xmlInfo.xmlNameTrait(schema);
            if (trait != null) {
                xmlName = trait.getValue();
            } else if (schema.isMember()) {
                xmlName = schema.memberTarget().id().getName();
            } else {
                xmlName = schema.id().getName();
            }

            writer.writeStartElement(xmlName);

            // Add a namespace if present, and peek-through to the target shape for a namespace when it's a member.
            XmlNamespaceTrait ns = schema.getTrait(TraitKey.XML_NAMESPACE_TRAIT);
            if (ns == null) {
                ns = defaultNamespace;
            }
            if (ns != null) {
                writer.writeNamespace(ns.getPrefix().orElse(null), ns.getUri());
            }

            return valueSerializer;
        } catch (XMLStreamException e) {
            throw new SerializationException(e);
        }
    }

    // Close the top-level shape element.
    @Override
    protected void after(Schema schema) {
        try {
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new SerializationException(e);
        }
    }

    private String formatTimestamp(Schema schema, Instant value) {
        return xmlInfo.timestampFormatter(schema).writeString(value);
    }

    private void writeStart(String xmlName, Schema schema) throws XMLStreamException {
        writer.writeStartElement(xmlName);
        // Add a namespace if present.
        XmlNamespaceTrait ns = schema.getDirectTrait(TraitKey.XML_NAMESPACE_TRAIT);
        if (ns != null) {
            writer.writeNamespace(ns.getPrefix().orElse(null), ns.getUri());
        }
    }

    private String getMemberXmlName(Schema schema) {
        XmlNameTrait trait = xmlInfo.directXmlNameTrait(schema);
        if (trait != null) {
            return trait.getValue();
        } else if (schema.isMember()) {
            return schema.memberName();
        } else {
            throw new IllegalArgumentException("Expected member schema in XML serializer, found " + schema);
        }
    }

    // Writes element text; carriage returns become character references, since parsers normalize a raw CR to LF.
    private void writeText(String value) throws XMLStreamException {
        int start = 0;
        int cr = value.indexOf('\r');
        while (cr >= 0) {
            if (cr > start) {
                writer.writeCharacters(value.substring(start, cr));
            }
            writer.writeEntityRef("#xD");
            start = cr + 1;
            cr = value.indexOf('\r', start);
        }
        if (start == 0) {
            writer.writeCharacters(value);
        } else if (start < value.length()) {
            writer.writeCharacters(value.substring(start));
        }
    }

    // Write to structure elements that are _not_ attributes.
    private final class StructMemberSerializer extends InterceptingSerializer {
        @Override
        protected ShapeSerializer before(Schema schema) {
            if (schema.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT)) {
                // Attributes have to be written before writing non-attribute members.
                return ShapeSerializer.nullSerializer();
            } else if (schema.hasTrait(TraitKey.XML_FLATTENED_TRAIT)) {
                return valueSerializer;
            } else {
                return nonFlattenedMemberSerializer;
            }
        }
    }

    // Write to structure attributes.
    private final class StructAttributeSerializer extends InterceptingSerializer {
        @Override
        protected ShapeSerializer before(Schema schema) {
            if (schema.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT)) {
                return attributeSerializer;
            } else {
                return ShapeSerializer.nullSerializer();
            }
        }
    }

    // Writes members by writing their containing element, their value, then the closing element.
    // Note that flattened structure members skip this writer and just use MemberValueSerializer.
    private final class NonFlattenedMemberSerializer extends InterceptingSerializer {
        @Override
        protected ShapeSerializer before(Schema schema) {
            try {
                writeStart(getMemberXmlName(schema), schema);
                return valueSerializer;
            } catch (XMLStreamException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        protected void after(Schema schema) {
            try {
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new SerializationException(e);
            }
        }
    }

    // Serialize member values. This does not open and close a containing element.
    private final class ValueSerializer implements ShapeSerializer {
        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            XmlInfo.StructInfo structInfo = xmlInfo.getStructInfo(schema);
            // Serialize attributes first; they have to occur before writing siblings and closing the opening node.
            if (!structInfo.attributes.isEmpty()) {
                struct.serializeMembers(structAttributeSerializer);
            }
            // Now serialize nested elements.
            struct.serializeMembers(structMemberSerializer);
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            consumer.accept(listState, new XmlListItemSerializer(xmlInfo.getListInfo(schema)));
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            consumer.accept(mapState, new XmlMapEntrySerializer(xmlInfo.getMapInfo(schema)));
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            write(value ? "true" : "false");
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            write(Byte.toString(value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            write(Short.toString(value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            write(Integer.toString(value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            write(Long.toString(value));
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            write(Float.toString(value));
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            write(Double.toString(value));
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            write(value.toString());
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            write(value.toString());
        }

        @Override
        public void writeString(Schema schema, String value) {
            write(value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            write(ByteBufferUtils.base64Encode(value));
        }

        @Override
        public void writeBlob(Schema schema, byte[] value) {
            // The bytes are only read here, so a writable view of the whole array lets base64 read it without copying.
            writeBlob(schema, ByteBuffer.wrap(value));
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            write(formatTimestamp(schema, value));
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            // do nothing
        }

        @Override
        public void writeNull(Schema schema) {
            // do nothing
        }

        private void write(String value) {
            try {
                writeText(value);
            } catch (XMLStreamException e) {
                throw new SerializationException(e);
            }
        }
    }

    // Serialize XML attributes of structures and unions.
    private final class AttributeSerializer extends SpecificShapeSerializer {
        @Override
        public void writeBoolean(Schema schema, boolean value) {
            write(schema, value ? "true" : "false");
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            write(schema, Byte.toString(value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            write(schema, Short.toString(value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            write(schema, Integer.toString(value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            write(schema, Long.toString(value));
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            write(schema, Float.toString(value));
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            write(schema, Double.toString(value));
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            write(schema, value.toString());
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            write(schema, value.toString());
        }

        @Override
        public void writeString(Schema schema, String value) {
            write(schema, value);
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            write(schema, formatTimestamp(schema, value));
        }

        @Override
        public void writeNull(Schema schema) {
            // do nothing
        }

        private void write(Schema schema, String value) {
            try {
                writer.writeAttribute(getMemberXmlName(schema), value);
            } catch (XMLStreamException e) {
                throw new SerializationException(e);
            }
        }
    }

    // Wrap list member values in the "item" node.
    private final class XmlListItemSerializer extends InterceptingSerializer {
        private final XmlInfo.ListMemberInfo info;

        XmlListItemSerializer(XmlInfo.ListMemberInfo info) {
            this.info = info;
        }

        @Override
        protected ShapeSerializer before(Schema schema) {
            try {
                writeStart(info.memberName, schema);
                return valueSerializer;
            } catch (XMLStreamException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        protected void after(Schema schema) {
            try {
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new SerializationException(e);
            }
        }
    }

    private final class XmlMapEntrySerializer implements MapSerializer {
        private final XmlInfo.MapMemberInfo info;

        XmlMapEntrySerializer(XmlInfo.MapMemberInfo info) {
            this.info = info;
        }

        @Override
        public <T> void writeEntry(Schema keySchema, String key, T state, BiConsumer<T, ShapeSerializer> valueSerializer) {
            try {
                // Write the "<entry>" element.
                writer.writeStartElement(info.entryName);

                // Write the "<key>" element.
                writeStart(info.keyName, keySchema);
                writeText(key);
                writer.writeEndElement();

                // The <value> element is opened and closed by the nonFlattenedMemberSerializer.
                valueSerializer.accept(state, nonFlattenedMemberSerializer);

                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new SerializationException(e);
            }
        }
    }
}
