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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import software.amazon.awssdk.protocols.core.serde.TimestampFormats;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.MapSerializer;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * A schema-guided {@link ShapeSerializer} writing one XML document, the Smithy XML binding of {@code restXml}
 * request bodies (the inverse of {@link XmlShapeDeserializer}):
 * <ul>
 *   <li>the root element is a structure or union written with {@link #writeStruct}; it is named by
 *       {@link #rootName}: a payload member's own {@code xmlName}, else the structure's {@code xmlName}, else its
 *       shape name;</li>
 *   <li>a structure's members are child elements, or for {@code xmlAttribute} members attributes, named by the
 *       member's {@code xmlName} else its member name ({@link XmlShapeDeserializer#memberName});</li>
 *   <li>a list is a wrapper element of {@code <member>} items (renamed by the list member's {@code xmlName}), or,
 *       for an {@code xmlFlattened} member, the repeated member elements themselves;</li>
 *   <li>a map is a wrapper of {@code <entry>} elements, or the repeated member elements when flattened, each holding
 *       {@code <key>} and {@code <value>} (renamed by the key and value members' {@code xmlName}); a sparse map's null
 *       value is written as an entry without a value element;</li>
 *   <li>{@code xmlNamespace} declares {@code xmlns} (or {@code xmlns:prefix}) on the element it applies to: the
 *       root's (payload member's, else the structure's) namespace, else the default (service) namespace; a member's,
 *       list member's or map key/value member's own namespace, else its target's. Element and attribute names are
 *       written as their {@code xmlName} spells them, so a prefixed attribute ({@code xsi:type}) relies on its
 *       structure's prefixed namespace, as Smithy forbids {@code xmlNamespace} on {@code xmlAttribute} members;</li>
 *   <li>timestamps honor {@code timestampFormat} and default to {@code date-time}; blobs are base64; floats are
 *       written in their shortest decimal form, or {@code NaN}, {@code Infinity}, {@code -Infinity}; big decimals in
 *       plain notation;</li>
 *   <li>text and attribute values are escaped ({@code &}, {@code <}, {@code >}, {@code "}, and {@code \r}; also
 *       {@code \n} and {@code \t} in attributes, so that they survive attribute-value normalization); other
 *       characters, including non-ASCII ones, are written as UTF-8;</li>
 *   <li>null values and absent members are omitted.</li>
 * </ul>
 *
 * <p>The document has no XML declaration. Documents are not supported by XML protocols. Failures are
 * {@link SerializationException}s whose messages never quote values; characters XML 1.0 cannot represent (most C0
 * controls, unpaired surrogates, {@code U+FFFE}, {@code U+FFFF}) fail.
 *
 * <p>Not thread safe; one instance writes one document. {@link #flush} flushes the stream; {@link #close} flushes
 * and closes it.
 */
public final class XmlShapeSerializer implements ShapeSerializer {
    private static final String DEFAULT_LIST_ITEM = "member";
    private static final String DEFAULT_MAP_ENTRY = "entry";
    private static final String DEFAULT_MAP_KEY = "key";
    private static final String DEFAULT_MAP_VALUE = "value";

    private final Writer out;
    private final XmlNamespaceTrait defaultNamespace;
    private final ElementWriter members = new ElementWriter(null, null);
    private boolean wroteRoot;

    private XmlShapeSerializer(OutputStream out, XmlNamespaceTrait defaultNamespace) {
        this.out = new BufferedWriter(new OutputStreamWriter(Objects.requireNonNull(out, "out"),
                                                             StandardCharsets.UTF_8));
        this.defaultNamespace = defaultNamespace;
    }

    /**
     * A serializer of one XML document into {@code out}.
     *
     * @param defaultNamespace the namespace of the root element when neither it (nor its payload member) has an
     *        {@code xmlNamespace}, typically the service's; {@code null} for none
     */
    public static XmlShapeSerializer create(OutputStream out, XmlNamespaceTrait defaultNamespace) {
        return new XmlShapeSerializer(out, defaultNamespace);
    }

    /**
     * {@code value} as an XML document whose root element is named by {@code value.schema()} (see
     * {@link #rootName}). To write only an input's body members under the input's root element, pass a view of
     * {@code value} that serializes only those members, such as {@code HttpBindingWriter.bodyMembers(input)}.
     */
    public static byte[] serialize(SerializableStruct value, XmlNamespaceTrait defaultNamespace) {
        return serialize(value.schema(), value, defaultNamespace);
    }

    /**
     * {@code value} as an XML document whose root element is named by {@code root} (see {@link #rootName}), for
     * example an {@code httpPayload} member, whose own {@code xmlName} and {@code xmlNamespace} then apply.
     */
    public static byte[] serialize(Schema root, SerializableStruct value, XmlNamespaceTrait defaultNamespace) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (XmlShapeSerializer serializer = create(bytes, defaultNamespace)) {
            serializer.writeStruct(root, value);
        }
        return bytes.toByteArray();
    }

    /**
     * The name of the root element of a structure or union: for a member (an {@code httpPayload} member), its own
     * {@code xmlName}, else its target's; for a shape, its {@code xmlName}; else the (target) shape's name.
     */
    public static String rootName(Schema schema) {
        XmlNameTrait name = schema.getTrait(TraitKey.XML_NAME_TRAIT);
        if (name != null) {
            return name.getValue();
        }
        return (schema.isMember() ? schema.memberTarget() : schema).id().getName();
    }

    private static String partName(Schema member, String defaultName) {
        XmlNameTrait name = member.getDirectTrait(TraitKey.XML_NAME_TRAIT);
        return name != null ? name.getValue() : defaultName;
    }

    // Root writes.

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        if (wroteRoot) {
            throw new SerializationException("An XML document has one root element");
        }
        wroteRoot = true;
        XmlNamespaceTrait namespace = schema.getTrait(TraitKey.XML_NAMESPACE_TRAIT);
        writeStructElement(rootName(schema), namespace != null ? namespace : defaultNamespace, schema, struct);
    }

    @Override
    public void writeNull(Schema schema) {
        // An absent root writes nothing.
    }

    private SerializationException notRoot(Schema schema) {
        return new SerializationException("The root of an XML document must be a structure or union, not a "
                                          + schema.type());
    }

    @Override
    public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
        throw notRoot(schema);
    }

    @Override
    public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
        throw notRoot(schema);
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        throw notRoot(schema);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        throw notRoot(schema);
    }

    @Override
    public void writeShort(Schema schema, short value) {
        throw notRoot(schema);
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        throw notRoot(schema);
    }

    @Override
    public void writeLong(Schema schema, long value) {
        throw notRoot(schema);
    }

    @Override
    public void writeFloat(Schema schema, float value) {
        throw notRoot(schema);
    }

    @Override
    public void writeDouble(Schema schema, double value) {
        throw notRoot(schema);
    }

    @Override
    public void writeBigInteger(Schema schema, BigInteger value) {
        throw notRoot(schema);
    }

    @Override
    public void writeBigDecimal(Schema schema, BigDecimal value) {
        throw notRoot(schema);
    }

    @Override
    public void writeString(Schema schema, String value) {
        throw notRoot(schema);
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        throw notRoot(schema);
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        throw notRoot(schema);
    }

    @Override
    public void writeDocument(Schema schema, Document value) {
        throw documentsUnsupported();
    }

    @Override
    public void flush() {
        try {
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            out.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static SerializationException documentsUnsupported() {
        return new SerializationException("Document values are not supported by XML protocols");
    }

    // Element and text output.

    /** Writes {@code <name xmlns...>}, with {@code struct}'s attribute members, its member elements, and the end. */
    private void writeStructElement(String name, XmlNamespaceTrait namespace, Schema schema,
                                    SerializableStruct struct) {
        List<String[]> attributes = attributes(schema, struct);
        raw('<').raw(name);
        declare(namespace);
        if (attributes != null) {
            for (String[] attribute : attributes) {
                raw(' ').raw(attribute[0]).raw("=\"");
                escape(attribute[1], true);
                raw('"');
            }
        }
        raw('>');
        struct.serializeMembers(members);
        end(name);
    }

    /** {@code struct}'s attribute members as {name, value}; {@code null} if its schema has none. */
    private static List<String[]> attributes(Schema schema, SerializableStruct struct) {
        boolean any = false;
        for (Schema member : schema.members()) {
            if (member.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return null;
        }
        AttributeCollector collector = new AttributeCollector();
        struct.serializeMembers(collector);
        return collector.attributes;
    }

    private void start(String name, XmlNamespaceTrait namespace) {
        raw('<').raw(name);
        declare(namespace);
        raw('>');
    }

    private void declare(XmlNamespaceTrait namespace) {
        if (namespace == null) {
            return;
        }
        raw(" xmlns");
        namespace.getPrefix().ifPresent(prefix -> raw(':').raw(prefix));
        raw("=\"");
        escape(namespace.getUri(), true);
        raw('"');
    }

    private void end(String name) {
        raw("</").raw(name).raw('>');
    }

    private void textElement(String name, XmlNamespaceTrait namespace, String text) {
        start(name, namespace);
        escape(text, false);
        end(name);
    }

    private XmlShapeSerializer raw(String text) {
        try {
            out.write(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    private XmlShapeSerializer raw(char c) {
        try {
            out.write(c);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    /** Writes {@code text} escaped for element content or, if {@code attribute}, a double-quoted attribute value. */
    private void escape(String text, boolean attribute) {
        int start = 0;
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            String replacement = switch (c) {
                case '&' -> "&amp;";
                case '<' -> "&lt;";
                case '>' -> "&gt;";
                case '\r' -> "&#xD;";
                case '"' -> attribute ? "&quot;" : null;
                case '\n' -> attribute ? "&#xA;" : null;
                case '\t' -> attribute ? "&#x9;" : null;
                default -> {
                    checkRepresentable(text, i, c);
                    yield null;
                }
            };
            if (replacement != null) {
                if (i > start) {
                    raw(text.substring(start, i));
                }
                raw(replacement);
                start = i + 1;
            }
        }
        if (start == 0) {
            raw(text);
        } else if (start < length) {
            raw(text.substring(start));
        }
    }

    /** Fails for a character XML 1.0 cannot represent (a low surrogate is checked with its high surrogate). */
    private static void checkRepresentable(String text, int index, char c) {
        boolean valid;
        if (c < 0x20) {
            valid = c == '\t' || c == '\n';
        } else if (Character.isHighSurrogate(c)) {
            valid = index + 1 < text.length() && Character.isLowSurrogate(text.charAt(index + 1));
        } else if (Character.isLowSurrogate(c)) {
            valid = index > 0 && Character.isHighSurrogate(text.charAt(index - 1));
        } else {
            valid = c != 0xFFFE && c != 0xFFFF;
        }
        if (!valid) {
            throw new SerializationException("A string holds a character that XML 1.0 cannot represent");
        }
    }

    // Scalar rendering.

    private static String timestamp(Schema schema, Instant value) {
        return TimestampFormats.write(value, TimestampFormats.formatOf(schema, TimestampFormatTrait.Format.DATE_TIME));
    }

    private static String base64(ByteBuffer value) {
        ByteBuffer view = value.duplicate();
        byte[] bytes = new byte[view.remaining()];
        view.get(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** A float in its shortest decimal form ({@code 1.1f} is {@code 1.1}, not its widened double value). */
    private static String formatFloat(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return formatDouble(value);
        }
        // -0.0 keeps its sign ("-0.0"): only other integral values are written as integers.
        if (value == Math.rint(value) && Math.abs(value) < 1e15 && Float.compare(value, -0.0f) != 0) {
            return Long.toString((long) value);
        }
        return Float.toString(value);
    }

    private static String formatDouble(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "Infinity" : "-Infinity";
        }
        // -0.0 keeps its sign ("-0.0"): only other integral values are written as integers.
        if (value == Math.rint(value) && Math.abs(value) < 1e15 && Double.compare(value, -0.0) != 0) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    /**
     * Writes each value as an element. With a fixed name (list items, map values) every value is an element of that
     * name and namespace; otherwise (a structure's members) each value is an element named by its member schema, and
     * attribute members, written by {@link AttributeCollector}, are skipped.
     */
    private final class ElementWriter implements ShapeSerializer {
        private final String fixedName;
        private final XmlNamespaceTrait fixedNamespace;

        ElementWriter(String fixedName, XmlNamespaceTrait fixedNamespace) {
            this.fixedName = fixedName;
            this.fixedNamespace = fixedNamespace;
        }

        private boolean isMembers() {
            return fixedName == null;
        }

        private boolean skip(Schema schema) {
            return isMembers() && schema.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT);
        }

        private String name(Schema schema) {
            return isMembers() ? XmlShapeDeserializer.memberName(schema) : fixedName;
        }

        private XmlNamespaceTrait namespace(Schema schema) {
            return isMembers() ? schema.getTrait(TraitKey.XML_NAMESPACE_TRAIT) : fixedNamespace;
        }

        private void text(Schema schema, String text) {
            if (!skip(schema)) {
                textElement(name(schema), namespace(schema), text);
            }
        }

        private boolean isFlattened(Schema schema) {
            return isMembers() && schema.hasTrait(TraitKey.XML_FLATTENED_TRAIT);
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            if (!skip(schema)) {
                writeStructElement(name(schema), namespace(schema), schema, struct);
            }
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            if (skip(schema)) {
                return;
            }
            if (isFlattened(schema)) {
                consumer.accept(listState, new ElementWriter(name(schema), namespace(schema)));
                return;
            }
            String name = name(schema);
            start(name, namespace(schema));
            Schema item = schema.listMember();
            consumer.accept(listState, new ElementWriter(partName(item, DEFAULT_LIST_ITEM),
                                                         item.getTrait(TraitKey.XML_NAMESPACE_TRAIT)));
            end(name);
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            if (skip(schema)) {
                return;
            }
            Schema key = schema.mapKeyMember();
            Schema value = schema.mapValueMember();
            EntryWriter entries = new EntryWriter(partName(key, DEFAULT_MAP_KEY),
                                                  key.getTrait(TraitKey.XML_NAMESPACE_TRAIT),
                                                  new ElementWriter(partName(value, DEFAULT_MAP_VALUE),
                                                                    value.getTrait(TraitKey.XML_NAMESPACE_TRAIT)));
            if (isFlattened(schema)) {
                entries.entryName = name(schema);
                entries.entryNamespace = namespace(schema);
                consumer.accept(mapState, entries);
                return;
            }
            String name = name(schema);
            start(name, namespace(schema));
            entries.entryName = DEFAULT_MAP_ENTRY;
            consumer.accept(mapState, entries);
            end(name);
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            text(schema, Boolean.toString(value));
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            text(schema, Byte.toString(value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            text(schema, Short.toString(value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            text(schema, Integer.toString(value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            text(schema, Long.toString(value));
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            text(schema, formatFloat(value));
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            text(schema, formatDouble(value));
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            text(schema, value.toString());
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            text(schema, value.toPlainString());
        }

        @Override
        public void writeString(Schema schema, String value) {
            text(schema, value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            text(schema, base64(value));
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            text(schema, timestamp(schema, value));
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            throw documentsUnsupported();
        }

        @Override
        public void writeNull(Schema schema) {
            // Null values are omitted.
        }
    }

    /** Writes each map entry as {@code <entry><key>k</key><value>v</value></entry>} (names per the map's schema). */
    private final class EntryWriter implements MapSerializer {
        private final String keyName;
        private final XmlNamespaceTrait keyNamespace;
        private final ElementWriter values;
        private String entryName;
        private XmlNamespaceTrait entryNamespace;

        EntryWriter(String keyName, XmlNamespaceTrait keyNamespace, ElementWriter values) {
            this.keyName = keyName;
            this.keyNamespace = keyNamespace;
            this.values = values;
        }

        @Override
        public <T> void writeEntry(Schema keySchema, String key, T state, BiConsumer<T, ShapeSerializer> consumer) {
            start(entryName, entryNamespace);
            textElement(keyName, keyNamespace, key);
            consumer.accept(state, values);
            end(entryName);
        }
    }

    /** Collects a structure's {@code xmlAttribute} member values as text; other members are ignored. */
    private static final class AttributeCollector implements ShapeSerializer {
        private final List<String[]> attributes = new ArrayList<>();

        private void attribute(Schema member, String text) {
            if (member.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT)) {
                attributes.add(new String[] {XmlShapeDeserializer.memberName(member), text});
            }
        }

        private static SerializationException notScalar(Schema member) {
            return new SerializationException("The xmlAttribute member " + member.memberName()
                                              + " must target a simple shape");
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            if (schema.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT)) {
                throw notScalar(schema);
            }
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            if (schema.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT)) {
                throw notScalar(schema);
            }
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            if (schema.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT)) {
                throw notScalar(schema);
            }
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            attribute(schema, Boolean.toString(value));
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            attribute(schema, Byte.toString(value));
        }

        @Override
        public void writeShort(Schema schema, short value) {
            attribute(schema, Short.toString(value));
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            attribute(schema, Integer.toString(value));
        }

        @Override
        public void writeLong(Schema schema, long value) {
            attribute(schema, Long.toString(value));
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            attribute(schema, formatFloat(value));
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            attribute(schema, formatDouble(value));
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            attribute(schema, value.toString());
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            attribute(schema, value.toPlainString());
        }

        @Override
        public void writeString(Schema schema, String value) {
            attribute(schema, value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            attribute(schema, base64(value));
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            attribute(schema, timestamp(schema, value));
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            if (schema.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT)) {
                throw documentsUnsupported();
            }
        }

        @Override
        public void writeNull(Schema schema) {
            // Null attribute values are omitted.
        }
    }
}
