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

package software.amazon.awssdk.awscodegen;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Deterministic JSON writer for generated resources.
 *
 * <p>The same value always produces the same bytes: object keys are sorted by their UTF-16 code units (so the order of
 * the input map never matters), arrays keep their given order (callers sort them explicitly), indentation is two
 * spaces, lines end in {@code \n}, the document ends in one {@code \n}, and output is UTF-8. Strings escape
 * {@code "}, {@code \} and control characters only. Supported values are {@link Map} with {@link String} keys,
 * {@link List}, {@link String}, {@link Integer}, {@link Long} and {@link Boolean}; {@code null} is rejected so that an
 * absent value can never be written by accident.
 */
final class CanonicalJson {

    private static final String INDENT = "  ";

    private CanonicalJson() {
    }

    /** The UTF-8 bytes of {@code value}. */
    static byte[] bytes(Object value) {
        return write(value).getBytes(StandardCharsets.UTF_8);
    }

    /** The canonical text of {@code value}, ending in a newline. */
    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value, 0);
        return out.append('\n').toString();
    }

    private static void write(StringBuilder out, Object value, int depth) {
        if (value instanceof Map<?, ?> map) {
            writeObject(out, map, depth);
        } else if (value instanceof List<?> list) {
            writeArray(out, list, depth);
        } else if (value instanceof String string) {
            writeString(out, string);
        } else if (value instanceof Integer || value instanceof Long || value instanceof Boolean) {
            out.append(value);
        } else {
            throw new IllegalArgumentException("Unsupported JSON value: " + (value == null ? "null"
                                                                                   : value.getClass().getName()));
        }
    }

    private static void writeObject(StringBuilder out, Map<?, ?> map, int depth) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        TreeMap<String, Object> sorted = new TreeMap<>();
        map.forEach((key, value) -> {
            if (!(key instanceof String name)) {
                throw new IllegalArgumentException("JSON object keys must be strings: " + key);
            }
            sorted.put(name, value);
        });
        out.append("{\n");
        Iterator<Map.Entry<String, Object>> entries = sorted.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Object> entry = entries.next();
            indent(out, depth + 1);
            writeString(out, entry.getKey());
            out.append(": ");
            write(out, entry.getValue(), depth + 1);
            out.append(entries.hasNext() ? ",\n" : "\n");
        }
        indent(out, depth);
        out.append('}');
    }

    private static void writeArray(StringBuilder out, List<?> list, int depth) {
        if (list.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            indent(out, depth + 1);
            write(out, list.get(i), depth + 1);
            out.append(i + 1 < list.size() ? ",\n" : "\n");
        }
        indent(out, depth);
        out.append(']');
    }

    private static void writeString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
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
    }

    private static void indent(StringBuilder out, int depth) {
        out.append(INDENT.repeat(depth));
    }
}
