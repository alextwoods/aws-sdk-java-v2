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

package software.amazon.awssdk.protocols.core.xml;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.protocols.core.serde.TimestampFormats;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;

/**
 * A schema-guided {@link ShapeDeserializer} over an {@link XmlElement} tree, implementing the Smithy XML binding
 * shared by {@code awsQuery}, {@code ec2Query} and {@code restXml} bodies:
 * <ul>
 *   <li>a structure or union is an element whose child elements (or, for {@code xmlAttribute} members, attributes)
 *       are its members, named by the member's own {@code xmlName} else its member name; unknown elements are
 *       ignored;</li>
 *   <li>a list is a wrapper element of {@code <member>} items (the list member's {@code xmlName} renames them), or,
 *       for an {@code xmlFlattened} member, the repeated member elements themselves;</li>
 *   <li>a map is a wrapper of {@code <entry>} elements, or the repeated member elements when flattened, each holding
 *       {@code <key>} and {@code <value>} (renamed by the key and value members' {@code xmlName});</li>
 *   <li>timestamps honor {@code timestampFormat} and default to {@code date-time} (offsets allowed); epoch seconds
 *       are read exactly; blobs are base64; floats accept {@code NaN}, {@code Infinity}, {@code -Infinity};</li>
 *   <li>strings are the element's text as is; other scalars are trimmed.</li>
 * </ul>
 *
 * <p>Use {@link #of(XmlElement)} with the element that holds a structure's members (for example an operation's
 * {@code <OpResult>} element, a REST-XML body root, or an error's {@code <Error>} element), wrapped by
 * {@code LocatingDeserializer} for located failures. Failures are {@link SerializationException}s whose messages
 * never quote wire values. Documents are not supported by XML protocols.
 *
 * <p>Not thread safe; one instance reads one value.
 */
public final class XmlShapeDeserializer implements ShapeDeserializer {
    private static final ShapeDeserializer ABSENT = new XmlShapeDeserializer(null, null, null);

    /** The value's element; {@code null} for an attribute value, flattened items or an absent value. */
    private final XmlElement element;
    /** The elements of a flattened list or map; {@code null} otherwise. */
    private final List<XmlElement> flattened;
    /** The text of an attribute value; {@code null} otherwise. */
    private final String attribute;

    private XmlShapeDeserializer(XmlElement element, List<XmlElement> flattened, String attribute) {
        this.element = element;
        this.flattened = flattened;
        this.attribute = attribute;
    }

    /** A deserializer of the value held by {@code element} (a structure's members, a scalar's text, and so on). */
    public static XmlShapeDeserializer of(XmlElement element) {
        return new XmlShapeDeserializer(Objects.requireNonNull(element, "element"), null, null);
    }

    /** The XML element or attribute name of a structure or union member: its own {@code xmlName}, else its name. */
    public static String memberName(Schema member) {
        XmlNameTrait name = member.getDirectTrait(TraitKey.XML_NAME_TRAIT);
        return name != null ? name.getValue() : member.memberName();
    }

    /** The item or entry-part name of a list member or map key/value member: its {@code xmlName}, else the default. */
    private static String partName(Schema member, String defaultName) {
        XmlNameTrait name = member.getDirectTrait(TraitKey.XML_NAME_TRAIT);
        return name != null ? name.getValue() : defaultName;
    }

    private String text() {
        if (attribute != null) {
            return attribute;
        }
        if (element == null) {
            throw new SerializationException("Expected a scalar value");
        }
        return element.text();
    }

    private String trimmed() {
        return text().strip();
    }

    private XmlElement container(Schema schema) {
        if (element == null) {
            throw new SerializationException("Expected an element for a " + schema.type() + " value");
        }
        return element;
    }

    @Override
    public boolean readBoolean(Schema schema) {
        return switch (trimmed()) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new SerializationException("Expected true or false");
        };
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        String text = trimmed();
        try {
            return ByteBuffer.wrap(Base64.getDecoder().decode(stripWhitespace(text)));
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Expected base64");
        }
    }

    private static String stripWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                StringBuilder out = new StringBuilder(text.length());
                for (int j = 0; j < text.length(); j++) {
                    char c = text.charAt(j);
                    if (!Character.isWhitespace(c)) {
                        out.append(c);
                    }
                }
                return out.toString();
            }
        }
        return text;
    }

    @Override
    public byte readByte(Schema schema) {
        return Byte.parseByte(trimmed());
    }

    @Override
    public short readShort(Schema schema) {
        return Short.parseShort(trimmed());
    }

    @Override
    public int readInteger(Schema schema) {
        return Integer.parseInt(trimmed());
    }

    @Override
    public long readLong(Schema schema) {
        return Long.parseLong(trimmed());
    }

    @Override
    public float readFloat(Schema schema) {
        return switch (trimmed()) {
            case "NaN" -> Float.NaN;
            case "Infinity" -> Float.POSITIVE_INFINITY;
            case "-Infinity" -> Float.NEGATIVE_INFINITY;
            default -> Float.parseFloat(finiteNumber());
        };
    }

    @Override
    public double readDouble(Schema schema) {
        return switch (trimmed()) {
            case "NaN" -> Double.NaN;
            case "Infinity" -> Double.POSITIVE_INFINITY;
            case "-Infinity" -> Double.NEGATIVE_INFINITY;
            default -> Double.parseDouble(finiteNumber());
        };
    }

    /** The trimmed text, which must be a decimal number (Java's parsers would also accept hex and type suffixes). */
    private String finiteNumber() {
        String text = trimmed();
        new BigDecimal(text); // throws NumberFormatException unless decimal
        return text;
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        return new BigInteger(trimmed());
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        return new BigDecimal(trimmed());
    }

    @Override
    public String readString(Schema schema) {
        return text();
    }

    @Override
    public Document readDocument() {
        throw new SerializationException("Document values are not supported by XML protocols");
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        TimestampFormatTrait.Format format = TimestampFormats.formatOf(schema, TimestampFormatTrait.Format.DATE_TIME);
        String text = trimmed();
        if (format == TimestampFormatTrait.Format.EPOCH_SECONDS) {
            return TimestampFormats.fromEpochSeconds(new BigDecimal(text));
        }
        return TimestampFormats.read(text, format);
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        XmlElement container = container(schema);
        for (Schema member : schema.members()) {
            String name = memberName(member);
            if (member.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT)) {
                String value = container.attribute(name);
                if (value != null) {
                    consumer.accept(state, member, new XmlShapeDeserializer(null, null, value));
                }
                continue;
            }
            ShapeType type = member.type();
            if ((type == ShapeType.LIST || type == ShapeType.SET || type == ShapeType.MAP)
                && member.hasTrait(TraitKey.XML_FLATTENED_TRAIT)) {
                List<XmlElement> items = container.children(name);
                if (!items.isEmpty()) {
                    consumer.accept(state, member, new XmlShapeDeserializer(null, items, null));
                }
                continue;
            }
            XmlElement child = container.child(name);
            if (child != null) {
                consumer.accept(state, member, new XmlShapeDeserializer(child, null, null));
            }
        }
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        List<XmlElement> items = flattened != null ? flattened
                                                   : container(schema).children(partName(schema.listMember(),
                                                                                          "member"));
        for (XmlElement item : items) {
            consumer.accept(state, new XmlShapeDeserializer(item, null, null));
        }
    }

    @Override
    public int containerSize() {
        return flattened != null ? flattened.size() : -1;
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        List<XmlElement> entries = flattened != null ? flattened : container(schema).children("entry");
        String keyName = partName(schema.mapKeyMember(), "key");
        String valueName = partName(schema.mapValueMember(), "value");
        for (XmlElement entry : entries) {
            XmlElement key = entry.child(keyName);
            if (key == null) {
                throw new SerializationException("A map entry has no key element");
            }
            XmlElement value = entry.child(valueName);
            consumer.accept(state, key.text(), value == null ? ABSENT : new XmlShapeDeserializer(value, null, null));
        }
    }

    @Override
    public boolean isNull() {
        return element == null && flattened == null && attribute == null;
    }
}
