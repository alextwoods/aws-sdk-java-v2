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

package software.amazon.awssdk.policybuilder.iam.internal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The streaming JSON writer of the policy builder. It produces the exact text the V2 policy builder's writer produced
 * (V2 {@code JsonWriter} over Jackson's UTF-8 generator), so V3 policy JSON is byte-identical to V2's:
 *
 * <ul>
 *   <li>compact output has no whitespace;</li>
 *   <li>pretty output is Jackson's {@code DefaultPrettyPrinter}: object members on their own lines indented by two
 *       spaces per level with the platform line separator, {@code " : "} between name and value, arrays inline as
 *       {@code [ a, b ]}, and empty containers as <code>{ }</code> and {@code [ ]};</li>
 *   <li>strings escape {@code "} and {@code \}, write {@code \b \t \n \f \r} in short form and other control
 *       characters as {@code \}{@code u00XX}, write every UTF-16 surrogate as an upper-case {@code \}{@code uXXXX}
 *       escape, and write every other character (including {@code /} and non-ASCII text) as is.</li>
 * </ul>
 *
 * <p>Not thread safe; one instance writes one document. Not a public API.
 */
public final class PolicyJsonWriter {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final StringBuilder out = new StringBuilder(256);
    private final boolean prettyPrint;
    private final Deque<Context> contexts = new ArrayDeque<>();
    private int nesting;

    public PolicyJsonWriter(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    /** An open object or array and the number of members or elements written to it. */
    private static final class Context {
        private final boolean object;
        private int entries;

        private Context(boolean object) {
            this.object = object;
        }
    }

    public PolicyJsonWriter writeStartObject() {
        beforeValue();
        out.append('{');
        contexts.push(new Context(true));
        nesting++;
        return this;
    }

    public PolicyJsonWriter writeEndObject() {
        Context context = contexts.pop();
        nesting--;
        if (prettyPrint) {
            if (context.entries > 0) {
                indent();
            } else {
                out.append(' ');
            }
        }
        out.append('}');
        return this;
    }

    public PolicyJsonWriter writeStartArray() {
        beforeValue();
        out.append('[');
        contexts.push(new Context(false));
        return this;
    }

    public PolicyJsonWriter writeEndArray() {
        contexts.pop();
        if (prettyPrint) {
            out.append(' ');
        }
        out.append(']');
        return this;
    }

    public PolicyJsonWriter writeFieldName(String fieldName) {
        Context context = contexts.peek();
        if (context == null || !context.object) {
            throw new IllegalStateException("A member name can only be written in an object");
        }
        if (context.entries > 0) {
            out.append(',');
        }
        if (prettyPrint) {
            indent();
        }
        context.entries++;
        quote(fieldName);
        out.append(prettyPrint ? " : " : ":");
        return this;
    }

    public PolicyJsonWriter writeValue(String value) {
        beforeValue();
        quote(value);
        return this;
    }

    /** The document as text. */
    public String getString() {
        return out.toString();
    }

    /** The document as UTF-8 bytes. */
    public byte[] getBytes() {
        return getString().getBytes(StandardCharsets.UTF_8);
    }

    /** Writes the separator before a value in an array; a value in an object follows its member name. */
    private void beforeValue() {
        Context context = contexts.peek();
        if (context == null || context.object) {
            return;
        }
        if (context.entries > 0) {
            out.append(',');
        }
        if (prettyPrint) {
            out.append(' ');
        }
        context.entries++;
    }

    private void indent() {
        out.append(LINE_SEPARATOR);
        for (int i = 0; i < nesting; i++) {
            out.append("  ");
        }
    }

    private void quote(String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                default:
                    if (c < 0x20 || Character.isSurrogate(c)) {
                        out.append("\\u").append(HEX[(c >> 12) & 0xF]).append(HEX[(c >> 8) & 0xF])
                           .append(HEX[(c >> 4) & 0xF]).append(HEX[c & 0xF]);
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }
}
