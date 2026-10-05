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

package software.amazon.awssdk.endpoints.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small strict JSON (RFC 8259) reader for the canonical endpoint documents: rule sets, endpoint test suites and the
 * partition table. It exists so the endpoints runtime needs no codec or Smithy model classes to load them.
 *
 * <p>Values are immutable Java objects: objects are insertion-ordered {@code Map<String, Object>}, arrays are
 * {@code List<Object>}, strings are {@link String}, {@code true}/{@code false} are {@link Boolean}, integers are
 * {@link Integer} (or {@link Long} when they do not fit), other numbers are {@link Double}, and {@code null} is Java
 * {@code null}. Duplicate object keys are rejected.
 */
public final class Json {
    private final String text;
    private int position;

    private Json(String text) {
        this.text = text;
    }

    /**
     * Parses one JSON document.
     *
     * @throws IllegalArgumentException if {@code text} is not exactly one valid JSON value
     */
    public static Object parse(String text) {
        Json json = new Json(text);
        json.skipWhitespace();
        Object value = json.readValue();
        json.skipWhitespace();
        if (json.position != text.length()) {
            throw json.error("unexpected trailing content");
        }
        return value;
    }

    /** Parses a document whose root must be an object. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("Expected a JSON object at the document root.");
        }
        return (Map<String, Object>) value;
    }

    private Object readValue() {
        if (position >= text.length()) {
            throw error("unexpected end of input");
        }
        char c = text.charAt(position);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) {
                    yield readNumber();
                }
                throw error("unexpected character '" + c + "'");
            }
        };
    }

    private Map<String, Object> readObject() {
        position++;
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            position++;
            return Collections.unmodifiableMap(result);
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("expected an object key");
            }
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            Object value = readValue();
            if (result.containsKey(key)) {
                throw error("duplicate object key \"" + key + "\"");
            }
            result.put(key, value);
            skipWhitespace();
            char c = next();
            if (c == '}') {
                return Collections.unmodifiableMap(result);
            }
            if (c != ',') {
                throw error("expected ',' or '}'");
            }
        }
    }

    private List<Object> readArray() {
        position++;
        List<Object> result = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            position++;
            return Collections.unmodifiableList(result);
        }
        while (true) {
            skipWhitespace();
            result.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                return Collections.unmodifiableList(result);
            }
            if (c != ',') {
                throw error("expected ',' or ']'");
            }
        }
    }

    private String readString() {
        position++;
        StringBuilder builder = null;
        int start = position;
        while (true) {
            if (position >= text.length()) {
                throw error("unterminated string");
            }
            char c = text.charAt(position);
            if (c == '"') {
                String result = builder == null ? text.substring(start, position)
                                                : builder.append(text, start, position).toString();
                position++;
                return result;
            }
            if (c < 0x20) {
                throw error("unescaped control character in string");
            }
            if (c != '\\') {
                position++;
                continue;
            }
            if (builder == null) {
                builder = new StringBuilder();
            }
            builder.append(text, start, position);
            position++;
            char escape = next();
            switch (escape) {
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                case '/' -> builder.append('/');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    if (position + 4 > text.length()) {
                        throw error("truncated unicode escape");
                    }
                    try {
                        builder.append((char) Integer.parseInt(text.substring(position, position + 4), 16));
                    } catch (NumberFormatException e) {
                        throw error("invalid unicode escape");
                    }
                    position += 4;
                }
                default -> throw error("invalid escape '\\" + escape + "'");
            }
            start = position;
        }
    }

    private Object readNumber() {
        int start = position;
        if (peek() == '-') {
            position++;
        }
        boolean integral = true;
        digits();
        if (position < text.length() && text.charAt(position) == '.') {
            integral = false;
            position++;
            digits();
        }
        if (position < text.length() && (text.charAt(position) == 'e' || text.charAt(position) == 'E')) {
            integral = false;
            position++;
            if (position < text.length() && (text.charAt(position) == '+' || text.charAt(position) == '-')) {
                position++;
            }
            digits();
        }
        String number = text.substring(start, position);
        if (!integral) {
            return Double.parseDouble(number);
        }
        long value;
        try {
            value = Long.parseLong(number);
        } catch (NumberFormatException e) {
            return Double.parseDouble(number);
        }
        if (value == (int) value) {
            return Integer.valueOf((int) value);
        }
        return Long.valueOf(value);
    }

    private void digits() {
        int start = position;
        while (position < text.length() && Character.isDigit(text.charAt(position))
               && text.charAt(position) < 128) {
            position++;
        }
        if (position == start) {
            throw error("expected a digit");
        }
    }

    private Object literal(String word, Object value) {
        if (!text.startsWith(word, position)) {
            throw error("invalid literal");
        }
        position += word.length();
        return value;
    }

    private void skipWhitespace() {
        while (position < text.length()) {
            char c = text.charAt(position);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return;
            }
            position++;
        }
    }

    private char peek() {
        if (position >= text.length()) {
            throw error("unexpected end of input");
        }
        return text.charAt(position);
    }

    private char next() {
        char c = peek();
        position++;
        return c;
    }

    private void expect(char c) {
        if (next() != c) {
            position--;
            throw error("expected '" + c + "'");
        }
    }

    private IllegalArgumentException error(String problem) {
        return new IllegalArgumentException("Invalid JSON at offset " + position + ": " + problem + ".");
    }
}
