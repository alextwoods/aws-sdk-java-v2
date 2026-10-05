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
package software.amazon.awssdk.enhanced.dynamodb.internal.json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A strict RFC 8259 JSON parser with the V2 enhanced client's observable behavior: it reads the first JSON value of
 * the input (returning {@code null} when the input holds none), keeps numbers as their exact text, keeps object
 * member order with the last duplicate member winning, and reports malformed input as an {@link UncheckedIOException}
 * whose message names the offset. Comments, single quotes, unquoted names, leading zeros, non-finite numbers and raw
 * control characters inside strings are rejected, as by the default Jackson parser V2 used.
 */
public final class JsonNodeParser {
    private static final int MAX_DEPTH = 1000;

    private JsonNodeParser() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static JsonNodeParser create() {
        return new JsonNodeParser();
    }

    /** The first JSON value of {@code content}, or {@code null} if it contains only whitespace. */
    public JsonNode parse(String content) {
        Cursor cursor = new Cursor(content);
        cursor.skipWhitespace();
        if (cursor.atEnd()) {
            return null;
        }
        return cursor.value(0);
    }

    public static final class Builder {
        private Builder() {
        }

        public JsonNodeParser build() {
            return new JsonNodeParser();
        }
    }

    private static final class Cursor {
        private final String s;
        private int pos;

        Cursor(String s) {
            this.s = s;
        }

        boolean atEnd() {
            return pos >= s.length();
        }

        void skipWhitespace() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    return;
                }
            }
        }

        JsonNode value(int depth) {
            if (depth > MAX_DEPTH) {
                throw error("JSON nesting deeper than " + MAX_DEPTH);
            }
            skipWhitespace();
            if (atEnd()) {
                throw error("Unexpected end of input");
            }
            char c = s.charAt(pos);
            switch (c) {
                case '{':
                    return object(depth);
                case '[':
                    return array(depth);
                case '"':
                    return new StringJsonNode(string());
                case 't':
                    literal("true");
                    return new BooleanJsonNode(true);
                case 'f':
                    literal("false");
                    return new BooleanJsonNode(false);
                case 'n':
                    literal("null");
                    return NullJsonNode.instance();
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return new NumberJsonNode(number());
                    }
                    throw error("Unexpected character '" + printable(c) + "'");
            }
        }

        private JsonNode object(int depth) {
            pos++;
            Map<String, JsonNode> members = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return new ObjectJsonNode(members);
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw error("Expected a double-quoted member name");
                }
                String name = string();
                skipWhitespace();
                expect(':');
                JsonNode member = value(depth + 1);
                members.remove(name);
                members.put(name, member);
                skipWhitespace();
                char next = peek();
                pos++;
                if (next == '}') {
                    return new ObjectJsonNode(members);
                }
                if (next != ',') {
                    pos--;
                    throw error("Expected ',' or '}' in an object");
                }
            }
        }

        private JsonNode array(int depth) {
            pos++;
            List<JsonNode> elements = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return new ArrayJsonNode(elements);
            }
            while (true) {
                elements.add(value(depth + 1));
                skipWhitespace();
                char next = peek();
                pos++;
                if (next == ']') {
                    return new ArrayJsonNode(elements);
                }
                if (next != ',') {
                    pos--;
                    throw error("Expected ',' or ']' in an array");
                }
            }
        }

        private String string() {
            pos++;
            StringBuilder out = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw error("Unterminated string");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    return out.toString();
                }
                if (c < 0x20) {
                    pos--;
                    throw error("Unescaped control character in a string");
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (atEnd()) {
                    throw error("Unterminated escape sequence");
                }
                char e = s.charAt(pos++);
                switch (e) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> out.append(hexChar());
                    default -> {
                        pos--;
                        throw error("Invalid escape character '" + printable(e) + "'");
                    }
                }
            }
        }

        private char hexChar() {
            if (pos + 4 > s.length()) {
                throw error("Truncated unicode escape");
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(s.charAt(pos + i), 16);
                if (digit < 0) {
                    throw error("Invalid unicode escape");
                }
                value = (value << 4) | digit;
            }
            pos += 4;
            return (char) value;
        }

        private String number() {
            int start = pos;
            if (peek() == '-') {
                pos++;
            }
            if (peek() == '0') {
                pos++;
                if (isDigit(peek())) {
                    throw error("Leading zeros are not allowed in numbers");
                }
            } else {
                digits();
            }
            if (peek() == '.') {
                pos++;
                digits();
            }
            if (peek() == 'e' || peek() == 'E') {
                pos++;
                if (peek() == '+' || peek() == '-') {
                    pos++;
                }
                digits();
            }
            return s.substring(start, pos);
        }

        private void digits() {
            if (!isDigit(peek())) {
                throw error("Expected a digit");
            }
            while (isDigit(peek())) {
                pos++;
            }
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private void literal(String literal) {
            if (!s.startsWith(literal, pos)) {
                throw error("Unrecognized token");
            }
            pos += literal.length();
        }

        private void expect(char c) {
            if (peek() != c) {
                throw error("Expected '" + c + "'");
            }
            pos++;
        }

        private char peek() {
            return pos < s.length() ? s.charAt(pos) : '\0';
        }

        private static String printable(char c) {
            return c < 0x20 ? String.format("0x%02X", (int) c) : String.valueOf(c);
        }

        private UncheckedIOException error(String message) {
            return new UncheckedIOException(new IOException(message + " at offset " + pos + " of the JSON input"));
        }
    }
}
