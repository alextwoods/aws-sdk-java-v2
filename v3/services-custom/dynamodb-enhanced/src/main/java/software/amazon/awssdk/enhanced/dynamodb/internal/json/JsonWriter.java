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

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * A compact JSON writer producing the same text as the V2 enhanced client's Jackson-based generator: no whitespace,
 * quotation mark and reverse solidus escaped with a backslash, short escapes for backspace, tab, line feed, form feed
 * and carriage return, other control characters below U+0020 as a six-character upper-case hexadecimal escape,
 * every other character (including non-ASCII and supplementary code
 * points) written as is, numbers written as their exact text and binary values as padded standard Base64 strings.
 *
 * <p>Separators are inserted automatically: a value or field name that follows a sibling is preceded by a comma.
 */
public final class JsonWriter {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private final StringBuilder out = new StringBuilder();
    /** One entry per open container: true once the container holds a member. */
    private boolean[] hasMember = new boolean[8];
    private int depth;
    private boolean afterFieldName;

    public JsonWriter writeStartObject() {
        beforeValue();
        out.append('{');
        push();
        return this;
    }

    public JsonWriter writeEndObject() {
        depth--;
        out.append('}');
        return this;
    }

    public JsonWriter writeStartArray() {
        beforeValue();
        out.append('[');
        push();
        return this;
    }

    public JsonWriter writeEndArray() {
        depth--;
        out.append(']');
        return this;
    }

    public JsonWriter writeFieldName(String name) {
        separator();
        appendQuoted(name);
        out.append(':');
        afterFieldName = true;
        return this;
    }

    public JsonWriter writeNull() {
        beforeValue();
        out.append("null");
        return this;
    }

    public JsonWriter writeValue(String value) {
        beforeValue();
        appendQuoted(value);
        return this;
    }

    public JsonWriter writeValue(boolean value) {
        beforeValue();
        out.append(value);
        return this;
    }

    /** Writes {@code number} verbatim; the caller guarantees it is a JSON number. */
    public JsonWriter writeNumber(String number) {
        beforeValue();
        out.append(number);
        return this;
    }

    /** Writes the remaining bytes of {@code bytes} (without consuming them) as a Base64 string. */
    public JsonWriter writeValue(ByteBuffer bytes) {
        ByteBuffer copy = bytes.asReadOnlyBuffer();
        byte[] array = new byte[copy.remaining()];
        copy.get(array);
        return writeValue(Base64.getEncoder().encodeToString(array));
    }

    public JsonWriter writeNode(JsonNode node) {
        if (node.isNull()) {
            return writeNull();
        }
        if (node.isBoolean()) {
            return writeValue(node.asBoolean());
        }
        if (node.isNumber()) {
            return writeNumber(node.asNumber());
        }
        if (node.isString()) {
            return writeValue(node.asString());
        }
        if (node.isArray()) {
            writeStartArray();
            for (JsonNode element : node.asArray()) {
                writeNode(element);
            }
            return writeEndArray();
        }
        writeStartObject();
        for (Map.Entry<String, JsonNode> entry : node.asObject().entrySet()) {
            writeFieldName(entry.getKey());
            writeNode(entry.getValue());
        }
        return writeEndObject();
    }

    /** Writes {@code elements} as an array of strings. */
    public JsonWriter writeStringArray(List<String> elements) {
        writeStartArray();
        elements.forEach(this::writeValue);
        return writeEndArray();
    }

    @Override
    public String toString() {
        return out.toString();
    }

    private void push() {
        if (depth == hasMember.length) {
            boolean[] grown = new boolean[depth * 2];
            System.arraycopy(hasMember, 0, grown, 0, depth);
            hasMember = grown;
        }
        hasMember[depth++] = false;
    }

    private void beforeValue() {
        if (afterFieldName) {
            afterFieldName = false;
            return;
        }
        separator();
    }

    private void separator() {
        if (depth > 0) {
            if (hasMember[depth - 1]) {
                out.append(',');
            }
            hasMember[depth - 1] = true;
        }
    }

    private void appendQuoted(String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\t' -> out.append("\\t");
                case '\n' -> out.append("\\n");
                case '\f' -> out.append("\\f");
                case '\r' -> out.append("\\r");
                default -> {
                    if (c < 0x20) {
                        out.append("\\u00").append(HEX[c >> 4]).append(HEX[c & 0xF]);
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
