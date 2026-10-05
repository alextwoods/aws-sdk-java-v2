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

import java.io.ByteArrayInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import software.amazon.smithy.java.core.serde.SerializationException;

/**
 * Parses a finite XML body into an {@link XmlElement} tree with the JDK's StAX parser, configured for untrusted
 * input: DTDs and external entities are disabled, so only the predefined and character entities are resolved.
 *
 * <p>Parsing is iterative, so document depth cannot exhaust the stack. Whitespace-only text between child elements
 * is kept in {@link XmlElement#text()} of the parent; readers of leaf values use the leaf's text.
 *
 * <p>Thread safe.
 */
public final class XmlTreeReader {
    private static final XMLInputFactory FACTORY = createFactory();

    private XmlTreeReader() {
    }

    private static XMLInputFactory createFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);
        return factory;
    }

    /** True if {@code body} is empty or only XML whitespace, which query and REST-XML clients read as no content. */
    public static boolean isBlank(byte[] body) {
        for (byte b : body) {
            if (b != ' ' && b != '\t' && b != '\r' && b != '\n') {
                return false;
            }
        }
        return true;
    }

    /**
     * The root element of {@code body}.
     *
     * @throws SerializationException if {@code body} is not a well-formed XML document; the message never quotes the
     *         input
     */
    public static XmlElement parse(byte[] body) {
        XMLStreamReader reader;
        try {
            reader = FACTORY.createXMLStreamReader(new ByteArrayInputStream(body));
        } catch (XMLStreamException e) {
            throw new SerializationException("The body is not an XML document");
        }
        try {
            return read(reader);
        } catch (XMLStreamException | RuntimeException e) {
            if (e instanceof SerializationException serialization) {
                throw serialization;
            }
            throw new SerializationException("The body is not well-formed XML");
        } finally {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                // Nothing to release beyond the in-memory stream.
            }
        }
    }

    private static XmlElement read(XMLStreamReader reader) throws XMLStreamException {
        Deque<Pending> open = new ArrayDeque<>();
        XmlElement root = null;
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT -> {
                    if (root != null) {
                        throw new SerializationException("The body has more than one root element");
                    }
                    open.push(new Pending(reader));
                }
                case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA, XMLStreamConstants.SPACE,
                     XMLStreamConstants.ENTITY_REFERENCE -> {
                    if (!open.isEmpty()) {
                        open.peek().text.append(reader.getText());
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    XmlElement done = open.pop().build();
                    if (open.isEmpty()) {
                        root = done;
                    } else {
                        open.peek().children.add(done);
                    }
                }
                default -> {
                    // Comments, processing instructions and the document end carry no modeled data.
                }
            }
        }
        if (root == null) {
            throw new SerializationException("The body has no root element");
        }
        return root;
    }

    /** An element whose end tag has not been read yet. */
    private static final class Pending {
        private final String localName;
        private final String prefix;
        private final String namespaceUri;
        private final Map<String, String> attributes;
        private final List<XmlElement> children = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();

        Pending(XMLStreamReader reader) {
            this.localName = reader.getLocalName();
            this.prefix = reader.getPrefix();
            this.namespaceUri = reader.getNamespaceURI();
            int count = reader.getAttributeCount();
            if (count == 0) {
                this.attributes = Map.of();
            } else {
                this.attributes = new LinkedHashMap<>();
                for (int i = 0; i < count; i++) {
                    String attributePrefix = reader.getAttributePrefix(i);
                    String name = reader.getAttributeLocalName(i);
                    attributes.put(attributePrefix == null || attributePrefix.isEmpty() ? name
                                                                                         : attributePrefix + ":" + name,
                                   reader.getAttributeValue(i));
                }
            }
        }

        XmlElement build() {
            return new XmlElement(localName, prefix, namespaceUri, attributes, children, text.toString());
        }
    }
}
