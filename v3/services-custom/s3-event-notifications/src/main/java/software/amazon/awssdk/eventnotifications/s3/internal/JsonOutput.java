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

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A streaming JSON writer with the output format of the V2 {@code JsonWriter} that V2's S3 event notification writer
 * used (Jackson's generator defaults): compact output, or Jackson's default pretty printer ({@code "name" : value},
 * two-space indented object members on new lines, arrays inline as {@code [ a, b ]}, empty containers as {@code { }}
 * and {@code [ ]}). Strings escape {@code "} and {@code \}, write {@code \b \t \n \f \r} in short form and other control
 * characters and every UTF-16 surrogate (so characters outside the BMP, and unpaired surrogates) as
 * {@code \}{@code uXXXX} with upper-case hex digits, as Jackson's UTF-8 generator does; everything else, including
 * {@code /} and other non-ASCII characters, is written as is.
 *
 * <p>Not thread safe; one writer renders one document.
 */
final class JsonOutput {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private final StringBuilder out = new StringBuilder();
    private final boolean pretty;
    private final String lineSeparator;
    /** The value count of each open container; positive for objects, negative for arrays. */
    private final Deque<int[]> containers = new ArrayDeque<>();
    private int nesting;

    JsonOutput(boolean pretty) {
        this.pretty = pretty;
        this.lineSeparator = System.lineSeparator();
    }

    JsonOutput startObject() {
        beforeValue();
        out.append('{');
        containers.push(new int[] {0, 1});
        if (pretty) {
            nesting++;
        }
        return this;
    }

    JsonOutput endObject() {
        int[] object = containers.pop();
        if (pretty) {
            nesting--;
            if (object[0] > 0) {
                indent();
            } else {
                out.append(' ');
            }
        }
        out.append('}');
        return this;
    }

    JsonOutput startArray() {
        beforeValue();
        out.append('[');
        containers.push(new int[] {0, 0});
        return this;
    }

    JsonOutput endArray() {
        containers.pop();
        if (pretty) {
            out.append(' ');
        }
        out.append(']');
        return this;
    }

    JsonOutput fieldName(String name) {
        int[] object = containers.peek();
        if (object[0]++ > 0) {
            out.append(',');
        }
        if (pretty) {
            indent();
        }
        quote(name);
        out.append(pretty ? " : " : ":");
        return this;
    }

    /** Writes {@code value}, or {@code null} if it is {@code null}. */
    JsonOutput value(String value) {
        beforeValue();
        if (value == null) {
            out.append("null");
        } else {
            quote(value);
        }
        return this;
    }

    /** Writes {@code value}, or {@code null} if it is {@code null}. */
    JsonOutput value(Long value) {
        beforeValue();
        out.append(value == null ? "null" : value.toString());
        return this;
    }

    JsonOutput value(boolean value) {
        beforeValue();
        out.append(value);
        return this;
    }

    JsonOutput nullValue() {
        beforeValue();
        out.append("null");
        return this;
    }

    /** The rendered document. */
    String result() {
        return out.toString();
    }

    /** The separator before an array element (an object value follows its field name directly). */
    private void beforeValue() {
        int[] container = containers.peek();
        if (container == null || container[1] == 1) {
            return;
        }
        if (container[0]++ > 0) {
            out.append(',');
        }
        if (pretty) {
            out.append(' ');
        }
    }

    private void indent() {
        out.append(lineSeparator);
        for (int i = 0; i < nesting; i++) {
            out.append("  ");
        }
    }

    private void quote(String value) {
        out.append('"');
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String shortEscape = switch (c) {
                case '"' -> "\\\"";
                case '\\' -> "\\\\";
                case '\b' -> "\\b";
                case '\t' -> "\\t";
                case '\n' -> "\\n";
                case '\f' -> "\\f";
                case '\r' -> "\\r";
                default -> null;
            };
            if (shortEscape == null && c >= 0x20 && !Character.isSurrogate(c)) {
                continue;
            }
            out.append(value, start, i);
            if (shortEscape != null) {
                out.append(shortEscape);
            } else {
                out.append("\\u").append(HEX[c >> 12 & 0xF]).append(HEX[c >> 8 & 0xF]).append(HEX[c >> 4 & 0xF])
                   .append(HEX[c & 0xF]);
            }
            start = i + 1;
        }
        out.append(value, start, value.length()).append('"');
    }
}
