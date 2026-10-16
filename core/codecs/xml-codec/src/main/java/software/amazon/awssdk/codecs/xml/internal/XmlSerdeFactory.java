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

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.ByteBufferUtils;
import software.amazon.awssdk.codecs.xml.XmlSettings;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;

/**
 * Creates the serializers and deserializers of one {@code XmlCodec}: the native implementation
 * ({@link SmithyXmlSerializer}, {@link SmithyXmlDeserializer}) by default, or the StAX implementation
 * ({@link XmlSerializer}, {@link XmlDeserializer}). Immutable and thread-safe.
 *
 * <p>SDK addition. smithy-java keeps this logic in {@code XmlCodec}; it moves here because the implementation classes
 * are package-private in this {@code internal} package.
 */
@SdkInternalApi
public final class XmlSerdeFactory {

    private final XmlInfo xmlInfo;
    private final XmlNamespaceTrait defaultNamespace;
    private final List<String> wrapperElements;
    private final boolean strictRootElement;
    private final boolean useNative;
    private final XMLInputFactory xmlInputFactory;
    private final XMLOutputFactory xmlOutputFactory;
    private final XMLEventFactory eventFactory;

    /**
     * Creates a factory.
     *
     * @param settings the codec settings
     * @param wrapperElements the top-level wrapper elements to skip, in order, when deserializing
     * @param strictRootElement whether a root element that does not match the shape is rejected
     * @param useNative whether to use the native implementation instead of StAX
     */
    public XmlSerdeFactory(XmlSettings settings, List<String> wrapperElements, boolean strictRootElement,
                           boolean useNative) {
        this.xmlInfo = new XmlInfo(settings);
        this.defaultNamespace = settings.defaultXmlNamespace();
        this.wrapperElements = Collections.unmodifiableList(wrapperElements);
        this.strictRootElement = strictRootElement;
        this.useNative = useNative;
        if (useNative) {
            this.xmlInputFactory = null;
            this.xmlOutputFactory = null;
            this.eventFactory = null;
        } else {
            this.xmlInputFactory = newHardenedInputFactory();
            this.xmlOutputFactory = XMLOutputFactory.newInstance();
            this.eventFactory = XMLEventFactory.newInstance();
        }
    }

    /**
     * Returns a new {@link XMLInputFactory} that does not process DTDs or external entities and does not replace entity
     * references, so that XML External Entity (XXE) and entity expansion attacks are not possible. These are the
     * settings of the SDK's {@code XmlDomParser} plus the two non-replacing and non-coalescing settings of
     * smithy-java.
     *
     * @return the hardened factory
     */
    public static XMLInputFactory newHardenedInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        factory.setProperty(XMLInputFactory.IS_COALESCING, false);
        return factory;
    }

    /**
     * Returns whether this factory uses the native implementation.
     *
     * @return {@code true} for the native implementation, {@code false} for StAX
     */
    public boolean usesNative() {
        return useNative;
    }

    /**
     * Returns the top-level wrapper elements skipped when deserializing.
     *
     * @return the wrapper element names, in order
     */
    public List<String> wrapperElements() {
        return wrapperElements;
    }

    /**
     * Returns whether a root element that does not match the shape is rejected.
     *
     * @return whether the root element must match
     */
    public boolean strictRootElement() {
        return strictRootElement;
    }

    /**
     * Creates a serializer that writes XML to {@code sink}.
     *
     * @param sink where to write
     * @return a new single-use serializer
     */
    public ShapeSerializer createSerializer(OutputStream sink) {
        if (useNative) {
            return new LazyXmlSerializer(defaultNamespace, xmlInfo, sink);
        }
        try {
            return new XmlSerializer(xmlOutputFactory.createXMLStreamWriter(sink, "UTF-8"), xmlInfo, defaultNamespace);
        } catch (XMLStreamException e) {
            throw new SerializationException(e);
        }
    }

    /**
     * Creates a deserializer that reads the remaining bytes of {@code source}, without changing its position. An empty
     * or null source reads as an empty document: structures, lists and maps are empty, and scalars throw.
     *
     * @param source the XML bytes; the caller must not modify them while the deserializer is in use
     * @return a new single-use deserializer
     */
    public ShapeDeserializer createDeserializer(ByteBuffer source) {
        if (source == null || !source.hasRemaining()) {
            return EmptyXmlDeserializer.INSTANCE;
        }

        if (useNative) {
            byte[] bytes;
            int offset;
            int length = source.remaining();
            if (source.hasArray()) {
                bytes = source.array();
                offset = source.arrayOffset() + source.position();
            } else {
                bytes = ByteBufferUtils.getBytes(source);
                offset = 0;
            }
            return new SmithyXmlDeserializer(bytes, offset, length, xmlInfo, true, wrapperElements, strictRootElement);
        }

        try {
            XmlReader.StreamReader reader = new XmlReader.StreamReader(
                xmlInputFactory.createXMLStreamReader(ByteBufferUtils.byteBufferInputStream(source.duplicate())),
                xmlInputFactory);
            return XmlDeserializer.topLevel(xmlInfo, eventFactory, reader, wrapperElements, strictRootElement);
        } catch (XMLStreamException e) {
            throw new SerializationException(e);
        }
    }

    /**
     * Reads the error code of an XML error response, from {@code <ErrorResponse><Error><Code>},
     * {@code <Response><Errors><Error><Code>} or {@code <Error><Code>}, and closes the deserializer.
     *
     * @param deserializer a deserializer created by an {@code XmlCodec}
     * @return the error code
     * @throws SerializationException if the body is not an XML error response, or the deserializer was not created by
     *     an XML codec over a non-empty body
     */
    public static String parseErrorCodeName(ShapeDeserializer deserializer) {
        try {
            if (!(deserializer instanceof XmlErrorCodeParser)) {
                throw new SerializationException("Expected a deserializer of a non-empty XML error response, found "
                                                 + deserializer.getClass().getSimpleName());
            }
            return ((XmlErrorCodeParser) deserializer).parseErrorCodeName();
        } finally {
            deserializer.close();
        }
    }
}
