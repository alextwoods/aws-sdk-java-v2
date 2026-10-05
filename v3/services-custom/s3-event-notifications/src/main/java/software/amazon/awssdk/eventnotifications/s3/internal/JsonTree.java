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

package software.amazon.awssdk.eventnotifications.s3.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A strict JSON parser to an in-memory tree, with the reading behavior of the V2 {@code JsonNodeParser} that V2's
 * S3 event notification reader used (Jackson defaults plus {@code ALLOW_JAVA_COMMENTS}):
 *
 * <ul>
 *     <li>RFC 8259 grammar; {@code //} and {@code /* *}{@code /} comments are allowed wherever whitespace is;</li>
 *     <li>only the first value is read: content after it is ignored, and empty input (whitespace and comments only)
 *         is the absent value {@code null};</li>
 *     <li>numbers keep their JSON text; object members keep their document order, and of duplicate names the last
 *         value wins at the position of the first;</li>
 *     <li>the nesting depth, number length and string length are limited (1000, 1000 and 20,000,000 characters, the
 *         Jackson stream-read defaults), so hostile input fails instead of exhausting the stack or memory.</li>
 * </ul>
 *
 * <p>Tree values: {@link String}, {@link JsonNumber}, {@link Boolean}, {@link #NULL}, {@code Map<String, Object>} and
 * {@code List<Object>}. Input is UTF-8; a leading byte order mark is skipped. Malformed input fails with an
 * {@link IOException} whose message gives the line and column.
 */
public final class JsonTree {
    /** The JSON {@code null} value, distinct from an absent member ({@code null}). */
    public static final Object NULL = new Object() {
        @Override
        public String toString() {
            return "null";
        }
    };

    static final int MAX_DEPTH = 1000;
    static final int MAX_NUMBER_LENGTH = 1000;
    static final int MAX_STRING_LENGTH = 20_000_000;

    private final String text;
    private int pos;
    private int depth;

    private JsonTree(String text) {
        this.text = text;
    }

    /** A JSON number, as its JSON text. */
    public record JsonNumber(String text) {
        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * Parses the first JSON value of UTF-8 encoded {@code json}.
     *
     * @return the value, or {@code null} if the input has no value
     * @throws IOException if the input is not valid UTF-8 or not valid JSON
     */
    public static Object parse(byte[] json) throws IOException {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                                         .onMalformedInput(CodingErrorAction.REPORT)
                                         .onUnmappableCharacter(CodingErrorAction.REPORT)
                                         .decode(ByteBuffer.wrap(json))
                                         .toString();
        } catch (CharacterCodingException e) {
            throw new IOException("Invalid UTF-8 in JSON input", e);
        }
        JsonTree parser = new JsonTree(text);
        if (text.startsWith("\uFEFF")) {
            parser.pos = 1;
        }
        parser.skipWhitespace();
        if (parser.pos >= text.length()) {
            return null;
        }
        return parser.readValue();
    }

    /**
     * Renders a tree value the way V2's {@code JsonNode.toString()} did, for diagnostics: strings are quoted with only
     * backslashes and quotes escaped, objects are {@code {"name": value,...}}.
     */
    public static String describe(Object value) {
        if (value instanceof String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                return "{}";
            }
            StringBuilder output = new StringBuilder("{");
            map.forEach((k, v) -> output.append('"').append(k).append("\": ").append(describe(v)).append(','));
            output.setCharAt(output.length() - 1, '}');
            return output.toString();
        }
        if (value instanceof List<?> list) {
            List<String> elements = new ArrayList<>(list.size());
            list.forEach(e -> elements.add(describe(e)));
            return elements.toString();
        }
        return String.valueOf(value);
    }

    private Object readValue() throws IOException {
        char c = peek();
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return readString();
            case 't':
                expectLiteral("true");
                return Boolean.TRUE;
            case 'f':
                expectLiteral("false");
                return Boolean.FALSE;
            case 'n':
                expectLiteral("null");
                return NULL;
            default:
                if (c == '-' || c >= '0' && c <= '9') {
                    return readNumber();
                }
                throw error("Unexpected character " + describeChar(c) + ": expected a JSON value");
        }
    }

    private Map<String, Object> readObject() throws IOException {
        enter();
        pos++;
        Map<String, Object> members = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            depth--;
            return members;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("Unexpected character " + describeChar(peek()) + ": expected a double-quoted field name");
            }
            String name = readString();
            skipWhitespace();
            if (peek() != ':') {
                throw error("Unexpected character " + describeChar(peek()) + ": expected ':' after a field name");
            }
            pos++;
            skipWhitespace();
            members.put(name, readValue());
            skipWhitespace();
            char c = peek();
            pos++;
            if (c == '}') {
                depth--;
                return members;
            }
            if (c != ',') {
                pos--;
                throw error("Unexpected character " + describeChar(c) + ": expected ',' or '}' in an object");
            }
        }
    }

    private List<Object> readArray() throws IOException {
        enter();
        pos++;
        List<Object> elements = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            depth--;
            return elements;
        }
        while (true) {
            skipWhitespace();
            elements.add(readValue());
            skipWhitespace();
            char c = peek();
            pos++;
            if (c == ']') {
                depth--;
                return elements;
            }
            if (c != ',') {
                pos--;
                throw error("Unexpected character " + describeChar(c) + ": expected ',' or ']' in an array");
            }
        }
    }

    private void enter() throws IOException {
        if (++depth > MAX_DEPTH) {
            throw error("Document nesting depth exceeds the maximum allowed (" + MAX_DEPTH + ")");
        }
    }

    private String readString() throws IOException {
        pos++;
        StringBuilder value = null;
        int start = pos;
        while (true) {
            if (pos >= text.length()) {
                throw error("Unexpected end of input in a string");
            }
            char c = text.charAt(pos);
            if (c == '"') {
                String result = value == null ? text.substring(start, pos) : value.append(text, start, pos).toString();
                pos++;
                checkStringLength(result.length());
                return result;
            }
            if (c < 0x20) {
                throw error("Illegal unquoted character " + describeChar(c) + ": control characters must be escaped "
                            + "in a string");
            }
            if (c == '\\') {
                if (value == null) {
                    value = new StringBuilder();
                }
                value.append(text, start, pos);
                pos++;
                value.append(readEscape());
                checkStringLength(value.length());
                start = pos;
            } else {
                pos++;
            }
        }
    }

    private char readEscape() throws IOException {
        if (pos >= text.length()) {
            throw error("Unexpected end of input in a character escape");
        }
        char c = text.charAt(pos++);
        switch (c) {
            case '"':
            case '\\':
            case '/':
                return c;
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case 'u':
                if (pos + 4 > text.length()) {
                    throw error("Unexpected end of input in a unicode escape");
                }
                int code = 0;
                for (int i = 0; i < 4; i++) {
                    int digit = hexDigit(text.charAt(pos));
                    if (digit < 0) {
                        throw error("Unexpected character " + describeChar(text.charAt(pos)) + ": expected a hex digit "
                                    + "in a unicode escape");
                    }
                    code = code * 16 + digit;
                    pos++;
                }
                return (char) code;
            default:
                pos--;
                throw error("Unrecognized character escape " + describeChar(c));
        }
    }

    private void checkStringLength(int length) throws IOException {
        if (length > MAX_STRING_LENGTH) {
            throw error("String length exceeds the maximum allowed (" + MAX_STRING_LENGTH + ")");
        }
    }

    private JsonNumber readNumber() throws IOException {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        if (pos >= text.length() || !isDigit(text.charAt(pos))) {
            throw error("Invalid number: expected a digit");
        }
        if (text.charAt(pos) == '0') {
            pos++;
            if (pos < text.length() && isDigit(text.charAt(pos))) {
                throw error("Invalid number: leading zeroes are not allowed");
            }
        } else {
            skipDigits();
        }
        if (pos < text.length() && text.charAt(pos) == '.') {
            pos++;
            if (pos >= text.length() || !isDigit(text.charAt(pos))) {
                throw error("Invalid number: decimal point not followed by a digit");
            }
            skipDigits();
        }
        if (pos < text.length() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
            pos++;
            if (pos < text.length() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
                pos++;
            }
            if (pos >= text.length() || !isDigit(text.charAt(pos))) {
                throw error("Invalid number: exponent indicator not followed by a digit");
            }
            skipDigits();
        }
        if (pos - start > MAX_NUMBER_LENGTH) {
            throw error("Number length exceeds the maximum allowed (" + MAX_NUMBER_LENGTH + ")");
        }
        return new JsonNumber(text.substring(start, pos));
    }

    private void skipDigits() {
        while (pos < text.length() && isDigit(text.charAt(pos))) {
            pos++;
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void expectLiteral(String literal) throws IOException {
        if (!text.startsWith(literal, pos)) {
            throw error("Unrecognized token: expected '" + literal + "'");
        }
        pos += literal.length();
        if (pos < text.length() && Character.isLetterOrDigit(text.charAt(pos))) {
            throw error("Unrecognized token '" + literal + text.charAt(pos) + "...': expected '" + literal + "'");
        }
    }

    /** Skips whitespace and comments. */
    private void skipWhitespace() throws IOException {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else if (c == '/' && pos + 1 < text.length() && text.charAt(pos + 1) == '/') {
                pos += 2;
                while (pos < text.length() && text.charAt(pos) != '\n' && text.charAt(pos) != '\r') {
                    pos++;
                }
            } else if (c == '/' && pos + 1 < text.length() && text.charAt(pos + 1) == '*') {
                int end = text.indexOf("*/", pos + 2);
                if (end < 0) {
                    throw error("Unexpected end of input in a comment");
                }
                pos = end + 2;
            } else {
                return;
            }
        }
    }

    private char peek() throws IOException {
        if (pos >= text.length()) {
            throw error("Unexpected end of input");
        }
        return text.charAt(pos);
    }

    private static String describeChar(char c) {
        return c < 0x20 ? "(CTRL-CHAR, code " + (int) c + ")" : "('" + c + "' (code " + (int) c + "))";
    }

    private IOException error(String message) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < pos && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new IOException(message + " at line " + line + ", column " + column);
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }
}
