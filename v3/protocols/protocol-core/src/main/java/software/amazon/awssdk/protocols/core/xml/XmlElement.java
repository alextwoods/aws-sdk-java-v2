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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One element of a parsed XML document ({@link XmlTreeReader}): its name, attributes, child elements and the text
 * directly inside it.
 *
 * <p>Names are matched the way Smithy {@code xmlName} values are written: a name without a prefix matches an
 * element's local name in any namespace; a {@code prefix:local} name matches the element's qualified name exactly.
 * Namespace URIs are recorded but not matched, as AWS XML protocols do not require clients to validate them.
 *
 * <p>Immutable and thread safe.
 */
public final class XmlElement {
    private final String localName;
    private final String prefix;
    private final String namespaceUri;
    private final Map<String, String> attributes;
    private final List<XmlElement> children;
    private final String text;

    XmlElement(String localName, String prefix, String namespaceUri, Map<String, String> attributes,
               List<XmlElement> children, String text) {
        this.localName = Objects.requireNonNull(localName, "localName");
        this.prefix = prefix == null ? "" : prefix;
        this.namespaceUri = namespaceUri == null ? "" : namespaceUri;
        this.attributes = Map.copyOf(attributes);
        this.children = List.copyOf(children);
        this.text = Objects.requireNonNull(text, "text");
    }

    /** The element's local name. */
    public String localName() {
        return localName;
    }

    /** The element's namespace prefix; empty if it has none. */
    public String prefix() {
        return prefix;
    }

    /** The element's namespace URI; empty if it is in no namespace. */
    public String namespaceUri() {
        return namespaceUri;
    }

    /** {@code prefix:localName}, or the local name if the element has no prefix. */
    public String qualifiedName() {
        return prefix.isEmpty() ? localName : prefix + ":" + localName;
    }

    /** True if this element's name matches {@code xmlName} (see the class documentation). */
    public boolean matches(String xmlName) {
        return xmlName.indexOf(':') < 0 ? localName.equals(xmlName) : qualifiedName().equals(xmlName);
    }

    /**
     * The value of the attribute named {@code xmlName} ({@code local} or {@code prefix:local}), or {@code null} if
     * the element has none. Namespace declarations are not attributes.
     */
    public String attribute(String xmlName) {
        return attributes.get(xmlName);
    }

    /** The attributes by qualified name. */
    public Map<String, String> attributes() {
        return attributes;
    }

    /** The child elements, in document order. */
    public List<XmlElement> children() {
        return children;
    }

    /** The child elements whose name matches {@code xmlName}, in document order. */
    public List<XmlElement> children(String xmlName) {
        List<XmlElement> out = null;
        for (XmlElement child : children) {
            if (child.matches(xmlName)) {
                if (out == null) {
                    out = new ArrayList<>();
                }
                out.add(child);
            }
        }
        return out == null ? List.of() : out;
    }

    /** The first child element whose name matches {@code xmlName}, or {@code null} if there is none. */
    public XmlElement child(String xmlName) {
        for (XmlElement child : children) {
            if (child.matches(xmlName)) {
                return child;
            }
        }
        return null;
    }

    /**
     * The text directly inside this element (character data and CDATA, entities resolved), excluding the text of
     * child elements; empty for an empty element.
     */
    public String text() {
        return text;
    }

    /** The trimmed text of the first child matching {@code xmlName}, or {@code null} if there is none. */
    public String childText(String xmlName) {
        XmlElement child = child(xmlName);
        return child == null ? null : child.text().strip();
    }

    @Override
    public String toString() {
        return "<" + qualifiedName() + "> with " + children.size() + " children";
    }
}
