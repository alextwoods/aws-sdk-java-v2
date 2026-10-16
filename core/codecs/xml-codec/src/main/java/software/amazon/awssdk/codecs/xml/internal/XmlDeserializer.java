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
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.SpecificShapeDeserializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.XmlNameTrait;

/**
 * The StAX XML deserializer, reading through an {@link XmlReader}. The codec uses the native
 * {@link SmithyXmlDeserializer} unless the StAX implementation is selected.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.xml.XmlDeserializer}. Value errors name the target
 * schema's ShapeId.
 */
@SdkInternalApi
final class XmlDeserializer implements ShapeDeserializer, XmlErrorCodeParser {

    private final XmlInfo xmlInfo;
    private final XmlReader reader;
    private final XMLEventFactory eventFactory;
    private final InnerDeserializer innerDeserializer;
    private final boolean isTopLevel;
    private final List<String> wrapperElements;
    private final boolean strictRootElement;

    private XmlDeserializer(XmlInfo xmlInfo,
                            XMLEventFactory eventFactory,
                            XmlReader reader,
                            boolean isTopLevel,
                            List<String> wrapperElements,
                            boolean strictRootElement) {
        this.xmlInfo = xmlInfo;
        this.reader = reader;
        this.isTopLevel = isTopLevel;
        this.wrapperElements = wrapperElements;
        this.strictRootElement = strictRootElement;
        this.eventFactory = eventFactory;
        this.innerDeserializer = new InnerDeserializer();
    }

    static XmlDeserializer topLevel(XmlInfo xmlInfo, XMLEventFactory eventFactory, XmlReader reader) {
        return new XmlDeserializer(xmlInfo, eventFactory, reader, true, Collections.emptyList(), true);
    }

    static XmlDeserializer topLevel(XmlInfo xmlInfo,
                                    XMLEventFactory eventFactory,
                                    XmlReader reader,
                                    List<String> wrapperElements,
                                    boolean strictRootElement) {
        return new XmlDeserializer(xmlInfo, eventFactory, reader, true, wrapperElements, strictRootElement);
    }

    static XmlDeserializer flattened(XmlInfo xmlInfo, XMLEventFactory eventFactory, XmlReader reader) {
        return new XmlDeserializer(xmlInfo, eventFactory, reader, false, Collections.emptyList(), true);
    }

    @Override
    public void close() {
        reader.close();
    }

    // The first deserialization of XML expects a containing XML element for the shape.
    // The inner deserializer deserializes members and doesn't have this expectation.
    // If the name is ErrorResponse or Error, it means we are deserializing an ErrorResponse like:
    // <ErrorResponse>
    //   <Error>
    //     <Type>Sender</Type>
    //     <Code>InvalidInput</Code>
    //     <Message>Invalid input</Message>
    //   </Error>
    // </ErrorResponse>
    // We should skip all the way to the Code element to deserialize the rest of the fields and
    // return early to skip the name comparison.
    private void enter(Schema schema) {
        try {
            if (!isTopLevel) {
                // List and map members are validated in those deserializers, not here.
                return;
            }

            if (!wrapperElements.isEmpty()) {
                // Skip wrapper elements (exact match), then deserialize content without root validation.
                skipWrapperElements();
                return;
            }

            String name = reader.nextMemberElement();
            String expected;
            XmlNameTrait trait = xmlInfo.xmlNameTrait(schema);
            if (trait != null) {
                expected = trait.getValue();
            } else if (schema.isMember()) {
                expected = schema.memberTarget().id().getName();
            } else if (name != null && ("ErrorResponse".equals(name) || "Error".equals(name)
                                        || "Response".equals(name))) {
                skipToCodeElement(name);
                return;
            } else {
                expected = schema.id().getName();
            }

            // In lenient mode (clients) a mismatched root element name is tolerated and the children are
            // read regardless (e.g. S3's <CopyObjectResult> for CopyObjectOutput).
            if (!expected.equals(name) && strictRootElement) {
                throw new SerializationException("Expected XML element named '" + expected + "', found " + name);
            }
        } catch (XMLStreamException e) {
            throw new SerializationException(e);
        }
    }

    private void skipWrapperElements() throws XMLStreamException {
        // Navigate through wrapper elements (exact match).
        // After this, we should be positioned so the next nextMemberElement returns content.
        for (String wrapperName : wrapperElements) {
            String name = reader.nextMemberElement();
            if (name == null) {
                return;
            }
            if (!name.equals(wrapperName)) {
                // Not the expected wrapper element - protocol mismatch.
                throw new SerializationException("Expected wrapper element '" + wrapperName + "', found '" + name + "'");
            }
            // Continue to next wrapper level.
        }
        // Now positioned inside the innermost wrapper, ready for readStruct to read members.
    }

    private void exit() {
        try {
            reader.closeElement();
        } catch (XMLStreamException e) {
            throw new SerializationException(e);
        }
    }

    private void skipToCodeElement(String name) throws XMLStreamException {
        if ("ErrorResponse".equals(name)) {
            // Move to the Error element.
            reader.nextMemberElement();
        } else if ("Response".equals(name)) {
            // EC2 Query: Response > Errors > Error
            reader.nextMemberElement();
            reader.nextMemberElement();
        }
        String element = reader.nextMemberElement();
        while (element != null) {
            if ("Code".equals(element)) {
                reader.closeElement();
                return;
            }
            reader.closeElement();
            element = reader.nextMemberElement();
        }
    }

    @Override
    public String parseErrorCodeName() {
        try {
            String element = reader.nextMemberElement();
            if (element == null
                || (!"ErrorResponse".equals(element) && !"Error".equals(element) && !"Response".equals(element))) {
                throw new SerializationException(
                    "Expected element <ErrorResponse>, <Response>, or <Error> for XML error response");
            }
            if ("ErrorResponse".equals(element)) {
                element = reader.nextMemberElement();
                if (!"Error".equals(element)) {
                    throw new SerializationException("Expected <Error> element inside <ErrorResponse>");
                }
            } else if ("Response".equals(element)) {
                // EC2 Query error format: Response > Errors > Error > Code
                element = reader.nextMemberElement();
                if (!"Errors".equals(element)) {
                    throw new SerializationException("Expected <Errors> element inside <Response>");
                }
                element = reader.nextMemberElement();
                if (!"Error".equals(element)) {
                    throw new SerializationException("Expected <Error> element inside <Errors>");
                }
            }
            String childElement = reader.nextMemberElement();
            while (childElement != null) {
                if ("Code".equals(childElement)) {
                    String code = reader.getText();
                    reader.closeElement();
                    return code;
                }
                reader.closeElement();
                childElement = reader.nextMemberElement();
            }
            throw new SerializationException("Expected <Code> element inside <Error>");
        } catch (XMLStreamException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public boolean readBoolean(Schema schema) {
        enter(schema);
        boolean result = innerDeserializer.readBoolean(schema);
        exit();
        return result;
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        enter(schema);
        ByteBuffer result = innerDeserializer.readBlob(schema);
        exit();
        return result;
    }

    @Override
    public byte readByte(Schema schema) {
        enter(schema);
        byte result = innerDeserializer.readByte(schema);
        exit();
        return result;
    }

    @Override
    public short readShort(Schema schema) {
        enter(schema);
        short result = innerDeserializer.readShort(schema);
        exit();
        return result;
    }

    @Override
    public int readInteger(Schema schema) {
        enter(schema);
        int result = innerDeserializer.readInteger(schema);
        exit();
        return result;
    }

    @Override
    public long readLong(Schema schema) {
        enter(schema);
        long result = innerDeserializer.readLong(schema);
        exit();
        return result;
    }

    @Override
    public float readFloat(Schema schema) {
        enter(schema);
        float result = innerDeserializer.readFloat(schema);
        exit();
        return result;
    }

    @Override
    public double readDouble(Schema schema) {
        enter(schema);
        double result = innerDeserializer.readDouble(schema);
        exit();
        return result;
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        enter(schema);
        BigInteger result = innerDeserializer.readBigInteger(schema);
        exit();
        return result;
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        enter(schema);
        BigDecimal result = innerDeserializer.readBigDecimal(schema);
        exit();
        return result;
    }

    @Override
    public String readString(Schema schema) {
        enter(schema);
        String result = innerDeserializer.readString(schema);
        exit();
        return result;
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        enter(schema);
        Instant result = innerDeserializer.readTimestamp(schema);
        exit();
        return result;
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        enter(schema);
        innerDeserializer.readStruct(schema, state, consumer);
        exit();
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        enter(schema);
        innerDeserializer.readList(schema, state, consumer);
        exit();
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        enter(schema);
        innerDeserializer.readStringMap(schema, state, consumer);
        exit();
    }

    @Override
    public boolean isNull() {
        return innerDeserializer.isNull();
    }

    @Override
    public <T> T readNull() {
        if (!innerDeserializer.isNull()) {
            throw new SerializationException("Expected null, found " + reader);
        }
        return null;
    }

    @Override
    public Document readDocument() {
        return null;
    }

    private SerializationException error(String message, Exception previous) {
        return error(this.reader, message, previous);
    }

    static SerializationException error(XmlReader reader, String message, Exception previous) {
        Location location = reader.getLocation();
        String msg = location == null
                     ? ("Error reading XML: " + message)
                     : ("Error reading XML near line " + location.getLineNumber()
                        + ", column " + location.getColumnNumber() + ": " + message);
        return new SerializationException(msg, previous);
    }

    private static boolean parseBoolean(XmlReader reader, Schema schema, String value) {
        if ("true".equals(value)) {
            return true;
        } else if ("false".equals(value)) {
            return false;
        }
        throw error(reader, "Expected boolean 'true' or 'false' for " + schema.id() + ", found '" + value + "'", null);
    }

    private static Instant parseTimestamp(XmlInfo xmlInfo, Schema schema, String value, XmlReader reader) {
        try {
            return xmlInfo.timestampFormatter(schema).readFromString(value, false);
        } catch (TimestampFormatter.TimestampSyntaxError e) {
            throw error(reader, "Failed to read timestamp for " + schema.id() + ": " + e.getMessage(), e);
        } catch (DateTimeException e) {
            throw error(reader, "Failed to read timestamp for " + schema.id(), e);
        }
    }

    private static final class AttributeDeserializer extends SpecificShapeDeserializer {
        private final XmlInfo xmlInfo;
        private final XmlReader reader;
        private final String value;

        private AttributeDeserializer(XmlInfo xmlInfo, XmlReader reader, String value) {
            this.xmlInfo = xmlInfo;
            this.reader = reader;
            this.value = value;
        }

        @Override
        public boolean readBoolean(Schema schema) {
            return parseBoolean(reader, schema, value);
        }

        @Override
        public String readString(Schema schema) {
            return value;
        }

        @Override
        public Instant readTimestamp(Schema schema) {
            return parseTimestamp(xmlInfo, schema, value, reader);
        }

        @Override
        public byte readByte(Schema schema) {
            try {
                return Byte.parseByte(value);
            } catch (NumberFormatException e) {
                throw attributeError(schema, e);
            }
        }

        @Override
        public BigDecimal readBigDecimal(Schema schema) {
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                throw attributeError(schema, e);
            }
        }

        @Override
        public BigInteger readBigInteger(Schema schema) {
            try {
                return new BigInteger(value);
            } catch (NumberFormatException e) {
                throw attributeError(schema, e);
            }
        }

        @Override
        public double readDouble(Schema schema) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw attributeError(schema, e);
            }
        }

        @Override
        public float readFloat(Schema schema) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                throw attributeError(schema, e);
            }
        }

        @Override
        public int readInteger(Schema schema) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw attributeError(schema, e);
            }
        }

        @Override
        public long readLong(Schema schema) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw attributeError(schema, e);
            }
        }

        @Override
        public short readShort(Schema schema) {
            try {
                return Short.parseShort(value);
            } catch (NumberFormatException e) {
                throw attributeError(schema, e);
            }
        }

        private SerializationException attributeError(Schema schema, NumberFormatException e) {
            return error(reader, "Failed to parse " + schema.type() + " attribute for " + schema.id(), e);
        }
    }

    private final class InnerDeserializer implements ShapeDeserializer {

        @Override
        public Document readDocument() {
            return null;
        }

        @Override
        public boolean readBoolean(Schema schema) {
            try {
                return parseBoolean(reader, schema, reader.getText());
            } catch (XMLStreamException e) {
                throw error("Failed to read boolean for " + schema.id(), e);
            }
        }

        @Override
        public ByteBuffer readBlob(Schema schema) {
            try {
                return ByteBuffer.wrap(Base64.getDecoder().decode(reader.getText()));
            } catch (XMLStreamException | IllegalArgumentException e) {
                throw error("Failed to read blob for " + schema.id(), e);
            }
        }

        @Override
        public byte readByte(Schema schema) {
            try {
                return Byte.parseByte(reader.getText());
            } catch (XMLStreamException | NumberFormatException e) {
                throw error("Failed to read byte for " + schema.id(), e);
            }
        }

        @Override
        public short readShort(Schema schema) {
            try {
                return Short.parseShort(reader.getText());
            } catch (XMLStreamException | NumberFormatException e) {
                throw error("Failed to read short for " + schema.id(), e);
            }
        }

        @Override
        public int readInteger(Schema schema) {
            try {
                return Integer.parseInt(reader.getText());
            } catch (XMLStreamException | NumberFormatException e) {
                throw error("Failed to read integer for " + schema.id(), e);
            }
        }

        @Override
        public long readLong(Schema schema) {
            try {
                return Long.parseLong(reader.getText());
            } catch (XMLStreamException | NumberFormatException e) {
                throw error("Failed to read long for " + schema.id(), e);
            }
        }

        @Override
        public float readFloat(Schema schema) {
            try {
                return Float.parseFloat(reader.getText());
            } catch (XMLStreamException | NumberFormatException e) {
                throw error("Failed to read float for " + schema.id(), e);
            }
        }

        @Override
        public double readDouble(Schema schema) {
            try {
                return Double.parseDouble(reader.getText());
            } catch (XMLStreamException | NumberFormatException e) {
                throw error("Failed to read double for " + schema.id(), e);
            }
        }

        @Override
        public BigInteger readBigInteger(Schema schema) {
            try {
                return new BigInteger(reader.getText());
            } catch (XMLStreamException | NumberFormatException e) {
                throw error("Failed to read BigInteger for " + schema.id(), e);
            }
        }

        @Override
        public BigDecimal readBigDecimal(Schema schema) {
            try {
                return new BigDecimal(reader.getText());
            } catch (XMLStreamException | NumberFormatException e) {
                throw error("Failed to read BigDecimal for " + schema.id(), e);
            }
        }

        @Override
        public String readString(Schema schema) {
            try {
                return reader.getText();
            } catch (XMLStreamException e) {
                throw error("Failed to read string for " + schema.id(), e);
            }
        }

        @Override
        public Instant readTimestamp(Schema schema) {
            try {
                return parseTimestamp(xmlInfo, schema, reader.getText(), reader);
            } catch (XMLStreamException e) {
                throw error("Failed to read timestamp for " + schema.id(), e);
            }
        }

        @Override
        public boolean isNull() {
            try {
                // An element is null only when it is genuinely empty: no text content AND no child
                // elements. getText() returns "" both for a truly empty element (<x/> or <x></x>)
                // and for an element whose content is entirely child elements, e.g. a structure
                // (<x><child>..</child></x>). To tell them apart, require that consuming the text left
                // us on this element's own end tag rather than on a nested child node.
                return reader.getText().isEmpty() && reader.atEndElement();
            } catch (XMLStreamException e) {
                throw error("Failed to determine if value is null", e);
            }
        }

        @Override
        public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
            try {
                XmlInfo.StructInfo decoder = xmlInfo.getStructInfo(schema);
                readStructAttributes(decoder, state, consumer);
                // Create a state object to buffer flattened values so they can be emitted all at once.
                // Operations like S3's ListObjectVersions contain multiple interspersed flattened lists.
                Map<Schema, Deque<XMLEvent>> flattenedState = decoder.createFlattenedState();
                for (String member = reader.nextMemberElement(); member != null; member = reader.nextMemberElement()) {
                    Schema elementSchema = decoder.elements.get(member);
                    if (elementSchema != null) {
                        decoder.readMember(flattenedState, eventFactory, this, reader, state, consumer, member,
                                           elementSchema);
                    } else {
                        xmlInfo.unknownMember(schema, state, consumer, member);
                    }
                    reader.closeElement();
                }
                decoder.finishReadingStruct(flattenedState, eventFactory, xmlInfo, state, consumer);
            } catch (XMLStreamException e) {
                throw error("Failed to read struct " + schema.id(), e);
            }
        }

        private <T> void readStructAttributes(XmlInfo.StructInfo decoder, T state, StructMemberConsumer<T> consumer) {
            for (Map.Entry<String, Schema> entry : decoder.attributes.entrySet()) {
                String attributeName = entry.getKey();
                // For namespace-prefixed attributes (e.g., "xsi:someName"), use the local part
                // for lookup since namespace-aware XML parsers store them by local name.
                int colonIdx = attributeName.indexOf(':');
                String lookupName = colonIdx >= 0 ? attributeName.substring(colonIdx + 1) : attributeName;
                String attributeValue = reader.getAttributeValue(null, lookupName);
                if (attributeValue != null) {
                    consumer.accept(state, entry.getValue(), new AttributeDeserializer(xmlInfo, reader, attributeValue));
                }
            }
        }

        @Override
        public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
            try {
                XmlInfo.ListMemberInfo info = xmlInfo.getListInfo(schema);
                for (String member = reader.nextMemberElement(); member != null; member = reader.nextMemberElement()) {
                    if (!member.equals(info.memberName)) {
                        throw error(String.format("Expected list item '%s' but found '%s': %s",
                                                  info.memberName, member, reader), null);
                    }
                    consumer.accept(state, this);
                    // Close the list member.
                    reader.closeElement();
                }
            } catch (XMLStreamException e) {
                throw error("Failed to read list " + schema.id(), e);
            }
        }

        @Override
        public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
            try {
                XmlInfo.MapMemberInfo decoder = xmlInfo.getMapInfo(schema);
                for (String member = reader.nextMemberElement(); member != null; member = reader.nextMemberElement()) {
                    // Verify the element name matches expected map entry name.
                    if (!decoder.entryName.equals(member)) {
                        // Handle non-flattened maps by breaking on unexpected elements.
                        if (!decoder.flattened) {
                            break;
                        } else {
                            throw error("Unexpected element in map: " + reader, null);
                        }
                    }

                    // Open the key element.
                    String keyElement = reader.nextMemberElement();
                    if (keyElement == null) {
                        throw error("Expected map key, but map unexpectedly closed", null);
                    } else if (!keyElement.equals(decoder.keyName)) {
                        throw error(String.format("Expected map key '%s' but found %s", decoder.keyName, reader), null);
                    }

                    // Consume the key content.
                    String key = reader.getText();
                    // Close the key element.
                    reader.closeElement();

                    // Open the value element.
                    String valueElement = reader.nextMemberElement();
                    if (valueElement == null) {
                        throw error("Expected map value, but map unexpectedly closed", null);
                    } else if (!decoder.valueName.equals(valueElement)) {
                        throw error(String.format("Expected map value '%s' but found %s", decoder.valueName, reader),
                                    null);
                    }

                    // Consume the value content.
                    consumer.accept(state, key, this);
                    // Close the value element.
                    reader.closeElement();
                    // The </entry> is not closed by the consumer but the map container is, so close one element.
                    reader.closeElement();
                }
            } catch (XMLStreamException e) {
                throw error("Failed to read map " + schema.id(), e);
            }
        }
    }
}
