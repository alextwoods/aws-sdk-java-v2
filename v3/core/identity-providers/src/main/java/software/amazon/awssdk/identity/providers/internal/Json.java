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

package software.amazon.awssdk.identity.providers.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small strict JSON reader and writer for credential documents (IMDS, container, process, SSO token caches and
 * responses). Objects become {@code LinkedHashMap}, arrays {@code ArrayList}, numbers {@code BigDecimal}. Error messages
 * never include document content, which may hold secrets. Not a public API.
 */
public final class Json {
    private final String text;
    private int pos;

    private Json(String text) {
        this.text = text;
    }

    /**
     * Parses {@code text} as one JSON value.
     *
     * @throws IllegalArgumentException with the failing offset if it is not valid JSON
     */
    public static Object parse(String text) {
        Json json = new Json(text);
        json.skipWhitespace();
        Object value = json.value();
        json.skipWhitespace();
        if (json.pos != text.length()) {
            throw json.error("Unexpected trailing content");
        }
        return value;
    }

    /**
     * Parses {@code text} as a JSON object.
     *
     * @throws IllegalArgumentException if it is not a JSON object
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("Expected a JSON object");
        }
        return (Map<String, Object>) value;
    }

    /** The string member {@code name} of {@code object}, or null if absent or null. */
    public static String string(Map<String, Object> object, String name) {
        Object value = object.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof BigDecimal || value instanceof Boolean) {
            return value.toString();
        }
        throw new IllegalArgumentException("Expected '" + name + "' to be a string");
    }

    /** Renders {@code object} (strings, numbers, booleans, nulls, maps, lists) as compact JSON. */
    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value);
        return out.toString();
    }

    private static void write(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String s) {
            out.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            out.append(String.format("\\u%04x", (int) c));
                        } else {
                            out.append(c);
                        }
                    }
                }
            }
            out.append('"');
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                write(out, String.valueOf(e.getKey()));
                out.append(':');
                write(out, e.getValue());
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                write(out, list.get(i));
            }
            out.append(']');
        } else {
            out.append(value);
        }
    }

    private Object value() {
        if (pos >= text.length()) {
            throw error("Unexpected end of document");
        }
        char c = text.charAt(pos);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> result = new LinkedHashMap<>();
        pos++;
        skipWhitespace();
        if (peek('}')) {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            if (!peek('"')) {
                throw error("Expected a member name");
            }
            String name = string();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            result.put(name, value());
            skipWhitespace();
            if (peek(',')) {
                pos++;
            } else {
                expect('}');
                return result;
            }
        }
    }

    private List<Object> array() {
        List<Object> result = new ArrayList<>();
        pos++;
        skipWhitespace();
        if (peek(']')) {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            result.add(value());
            skipWhitespace();
            if (peek(',')) {
                pos++;
            } else {
                expect(']');
                return result;
            }
        }
    }

    private String string() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw error("Unterminated string");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\') {
                if (pos >= text.length()) {
                    throw error("Unterminated escape");
                }
                char e = text.charAt(pos++);
                switch (e) {
                    case '"', '\\', '/' -> out.append(e);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (pos + 4 > text.length()) {
                            throw error("Invalid unicode escape");
                        }
                        try {
                            out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("Invalid unicode escape");
                        }
                        pos += 4;
                    }
                    default -> throw error("Invalid escape");
                }
            } else if (c < 0x20) {
                throw error("Control character in string");
            } else {
                out.append(c);
            }
        }
    }

    private BigDecimal number() {
        int start = pos;
        while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
        if (start == pos) {
            throw error("Unexpected character");
        }
        try {
            return new BigDecimal(text.substring(start, pos));
        } catch (NumberFormatException e) {
            throw error("Invalid number");
        }
    }

    private Object literal(String literal, Object value) {
        if (!text.startsWith(literal, pos)) {
            throw error("Unexpected character");
        }
        pos += literal.length();
        return value;
    }

    private boolean peek(char c) {
        return pos < text.length() && text.charAt(pos) == c;
    }

    private void expect(char c) {
        if (!peek(c)) {
            throw error("Expected '" + c + "'");
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < text.length() && " \t\r\n".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at offset " + pos + " of the JSON document");
    }
}
