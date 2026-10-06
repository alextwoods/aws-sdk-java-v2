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

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A lossless scanner for the XML subset POMs use: elements, attributes, text, entity and character references,
 * comments, CDATA sections, processing instructions and a DOCTYPE. It records exact offsets and never rewrites input,
 * so element ranges can be edited in place. Well-formedness errors (mismatched or unclosed tags, unterminated
 * constructs) are rejected with the line of the problem.
 */
final class XmlScanner {

    private final String path;
    private final String text;
    private int pos;

    private XmlScanner(String path, String text) {
        this.path = path;
        this.text = text;
    }

    /** The root element of {@code text}. */
    static XmlElement scan(String path, String text) {
        return new XmlScanner(path, text).document();
    }

    private XmlElement document() {
        Deque<XmlElement> open = new ArrayDeque<>();
        XmlElement root = null;
        while (pos < text.length()) {
            if (text.startsWith("<!--", pos)) {
                pos = after("-->", pos + 4, "comment");
            } else if (text.startsWith("<![CDATA[", pos)) {
                requireOpen(open, "CDATA section");
                pos = after("]]>", pos + 9, "CDATA section");
            } else if (text.startsWith("<?", pos)) {
                pos = after("?>", pos + 2, "processing instruction");
            } else if (text.startsWith("<!", pos)) {
                pos = afterDoctype();
            } else if (text.startsWith("</", pos)) {
                int tagStart = pos;
                pos += 2;
                String name = name();
                skipWhitespace();
                expect('>');
                if (open.isEmpty() || !open.peek().name().equals(name)) {
                    throw error(tagStart, "end tag </" + name + "> does not match "
                                          + (open.isEmpty() ? "any open element" : "<" + open.peek().name() + ">"));
                }
                open.pop().close(tagStart, pos);
            } else if (text.charAt(pos) == '<') {
                int tagStart = pos;
                pos++;
                XmlElement element = new XmlElement(text, name(), tagStart);
                boolean selfClosing = attributes();
                element.contentStart(pos);
                if (open.isEmpty()) {
                    if (root != null) {
                        throw error(tagStart, "a document has exactly one root element");
                    }
                    root = element;
                } else {
                    open.peek().addChild(element);
                }
                if (selfClosing) {
                    element.close(pos, pos);
                } else {
                    open.push(element);
                }
            } else {
                if (open.isEmpty() && !Character.isWhitespace(text.charAt(pos)) && text.charAt(pos) != '\uFEFF') {
                    throw error(pos, "text outside the root element");
                }
                pos++;
            }
        }
        if (!open.isEmpty()) {
            throw error(open.peek().start(), "element <" + open.peek().name() + "> is not closed");
        }
        if (root == null) {
            throw error(0, "no root element");
        }
        return root;
    }

    /** Scans attributes through the start tag's {@code >}; true if the tag is self-closing. */
    private boolean attributes() {
        while (true) {
            skipWhitespace();
            if (pos >= text.length()) {
                throw error(pos, "unterminated start tag");
            }
            char c = text.charAt(pos);
            if (c == '>') {
                pos++;
                return false;
            }
            if (c == '/' && text.startsWith("/>", pos)) {
                pos += 2;
                return true;
            }
            name();
            skipWhitespace();
            expect('=');
            skipWhitespace();
            if (pos >= text.length() || (text.charAt(pos) != '"' && text.charAt(pos) != '\'')) {
                throw error(pos, "attribute value must be quoted");
            }
            char quote = text.charAt(pos);
            int close = text.indexOf(quote, pos + 1);
            if (close < 0) {
                throw error(pos, "unterminated attribute value");
            }
            pos = close + 1;
        }
    }

    private String name() {
        int start = pos;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (Character.isWhitespace(c) || c == '>' || c == '/' || c == '=' || c == '<') {
                break;
            }
            pos++;
        }
        if (pos == start) {
            throw error(start, "expected a name");
        }
        return text.substring(start, pos);
    }

    private int afterDoctype() {
        int depth = 0;
        for (int i = pos + 2; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
            } else if (c == '>' && depth <= 0) {
                return i + 1;
            }
        }
        throw error(pos, "unterminated DOCTYPE");
    }

    private int after(String terminator, int from, String construct) {
        int index = text.indexOf(terminator, from);
        if (index < 0) {
            throw error(pos, "unterminated " + construct);
        }
        return index + terminator.length();
    }

    private void requireOpen(Deque<XmlElement> open, String construct) {
        if (open.isEmpty()) {
            throw error(pos, construct + " outside the root element");
        }
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private void expect(char c) {
        if (pos >= text.length() || text.charAt(pos) != c) {
            throw error(pos, "expected '" + c + "'");
        }
        pos++;
    }

    private XmlFormatException error(int offset, String message) {
        int line = 1;
        for (int i = 0; i < offset && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return new XmlFormatException(path + ":" + line + ": " + message);
    }

    /** The decoded text of {@code [start, end)}: comments removed, CDATA verbatim, references resolved. */
    static String decodeText(String text, int start, int end) {
        StringBuilder out = new StringBuilder();
        int i = start;
        while (i < end) {
            if (text.startsWith("<!--", i)) {
                int close = text.indexOf("-->", i + 4);
                i = close < 0 || close + 3 > end ? end : close + 3;
            } else if (text.startsWith("<![CDATA[", i)) {
                int close = text.indexOf("]]>", i + 9);
                int stop = close < 0 || close > end ? end : close;
                out.append(text, i + 9, stop);
                i = stop == end ? end : stop + 3;
            } else if (text.startsWith("<?", i)) {
                int close = text.indexOf("?>", i + 2);
                i = close < 0 || close + 2 > end ? end : close + 2;
            } else if (text.charAt(i) == '&') {
                int semi = text.indexOf(';', i);
                if (semi < 0 || semi >= end) {
                    out.append('&');
                    i++;
                    continue;
                }
                out.append(reference(text.substring(i + 1, semi)));
                i = semi + 1;
            } else {
                out.append(text.charAt(i++));
            }
        }
        return out.toString();
    }

    private static String reference(String name) {
        switch (name) {
            case "lt":
                return "<";
            case "gt":
                return ">";
            case "amp":
                return "&";
            case "quot":
                return "\"";
            case "apos":
                return "'";
            default:
                break;
        }
        try {
            if (name.startsWith("#x")) {
                return new String(Character.toChars(Integer.parseInt(name.substring(2), 16)));
            }
            if (name.startsWith("#")) {
                return new String(Character.toChars(Integer.parseInt(name.substring(1))));
            }
        } catch (IllegalArgumentException e) {
            // Not a valid character reference: keep it verbatim below.
        }
        return "&" + name + ";";
    }
}
