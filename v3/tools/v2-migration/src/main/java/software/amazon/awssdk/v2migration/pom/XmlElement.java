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

package software.amazon.awssdk.v2migration.pom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One element of a losslessly scanned XML document, with exact character offsets into the document text so that
 * recipes can replace an element, its content or its text without reformatting anything else.
 *
 * <pre>
 *   &lt;version&gt;2.25.0&lt;/version&gt;
 *   ^start   ^contentStart
 *                  ^contentEnd ^end
 * </pre>
 * For a self-closing element ({@code <x/>}), {@code contentStart == contentEnd == end}.
 */
public final class XmlElement {

    private final String document;
    private final String name;
    private final int start;
    private final List<XmlElement> children = new ArrayList<>();
    private int contentStart;
    private int contentEnd;
    private int end;

    XmlElement(String document, String name, int start) {
        this.document = document;
        this.name = name;
        this.start = start;
    }

    /** The element name, including any namespace prefix. */
    public String name() {
        return name;
    }

    /** The offset of the start tag's {@code <}. */
    public int start() {
        return start;
    }

    /** The offset after the start tag's {@code >}. */
    public int contentStart() {
        return contentStart;
    }

    /** The offset of the end tag's {@code <}. */
    public int contentEnd() {
        return contentEnd;
    }

    /** The offset after the end tag's {@code >}. */
    public int end() {
        return end;
    }

    /** The child elements in document order. */
    public List<XmlElement> children() {
        return Collections.unmodifiableList(children);
    }

    /** The child elements named {@code childName}, in document order. */
    public List<XmlElement> children(String childName) {
        return children.stream().filter(c -> c.name.equals(childName)).toList();
    }

    /** The first child element named {@code childName}. */
    public Optional<XmlElement> child(String childName) {
        return children.stream().filter(c -> c.name.equals(childName)).findFirst();
    }

    /**
     * The trimmed, entity-decoded text content of a leaf element (CDATA sections included verbatim, comments removed),
     * or empty if the element has child elements or no non-blank text.
     */
    public Optional<String> text() {
        if (!children.isEmpty()) {
            return Optional.empty();
        }
        String decoded = XmlScanner.decodeText(document, contentStart, contentEnd).trim();
        return decoded.isEmpty() ? Optional.empty() : Optional.of(decoded);
    }

    /** The trimmed text of the child element {@code childName}. */
    public Optional<String> childText(String childName) {
        return child(childName).flatMap(XmlElement::text);
    }

    /** The exact source text of the element, from its start tag through its end tag. */
    public String source() {
        return document.substring(start, end);
    }

    void addChild(XmlElement child) {
        children.add(child);
    }

    void contentStart(int offset) {
        this.contentStart = offset;
    }

    void close(int contentEndOffset, int endOffset) {
        this.contentEnd = contentEndOffset;
        this.end = endOffset;
    }

    @Override
    public String toString() {
        return "<" + name + "> [" + start + ", " + end + ")";
    }
}
