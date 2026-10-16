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

package software.amazon.awssdk.codecs.xml.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Low-level utilities for writing XML content directly to byte arrays as UTF-8 with escaping.
 *
 * <p>Text escapes {@code &}, {@code <}, {@code >} and carriage return; attribute values also escape {@code "},
 * {@code '}, tab and line feed. Carriage returns (and, in attributes, tabs and line feeds) are written as character
 * references because XML parsers normalize them (XML 1.0 sections 2.11 and 3.3.3), so a raw character would not read
 * back as itself. An unpaired surrogate is written as {@code ?}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.xml.XmlWriteUtils}. The {@code CompactStringAccess}
 * Latin-1 path is not ported (no {@code sun.misc.Unsafe}); the {@code charAt} loop is the only path.
 */
@SdkInternalApi
final class XmlWriteUtils {

    private static final byte[] AMP_ESC = {'&', 'a', 'm', 'p', ';'};
    private static final byte[] LT_ESC = {'&', 'l', 't', ';'};
    private static final byte[] GT_ESC = {'&', 'g', 't', ';'};
    private static final byte[] QUOT_ESC = {'&', 'q', 'u', 'o', 't', ';'};
    private static final byte[] APOS_ESC = {'&', 'a', 'p', 'o', 's', ';'};
    private static final byte[] CR_ESC = {'&', '#', 'x', 'D', ';'};
    private static final byte[] LF_ESC = {'&', '#', 'x', 'A', ';'};
    private static final byte[] TAB_ESC = {'&', '#', 'x', '9', ';'};

    private XmlWriteUtils() {
    }

    /**
     * Writes {@code value} as escaped element text and returns the new position. Reserve
     * {@link #maxEscapedTextBytes(String)} bytes.
     */
    static int writeEscapedText(byte[] buf, int pos, String value) {
        return writeEscaped(buf, pos, value, false);
    }

    /**
     * Writes {@code value} as an escaped attribute value (without quotes) and returns the new position. Reserve
     * {@link #maxEscapedAttributeBytes(String)} bytes.
     */
    static int writeEscapedAttribute(byte[] buf, int pos, String value) {
        return writeEscaped(buf, pos, value, true);
    }

    private static int writeEscaped(byte[] buf, int pos, String value, boolean attribute) {
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c < 0x80) {
                byte[] escape = asciiEscape(c, attribute);
                if (escape == null) {
                    buf[pos++] = (byte) c;
                } else {
                    System.arraycopy(escape, 0, buf, pos, escape.length);
                    pos += escape.length;
                }
            } else if (c < 0x800) {
                buf[pos++] = (byte) (0xC0 | (c >> 6));
                buf[pos++] = (byte) (0x80 | (c & 0x3F));
            } else if (!Character.isSurrogate(c)) {
                buf[pos++] = (byte) (0xE0 | (c >> 12));
                buf[pos++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                buf[pos++] = (byte) (0x80 | (c & 0x3F));
            } else if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(value.charAt(i + 1))) {
                int cp = Character.toCodePoint(c, value.charAt(++i));
                buf[pos++] = (byte) (0xF0 | (cp >> 18));
                buf[pos++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
                buf[pos++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                buf[pos++] = (byte) (0x80 | (cp & 0x3F));
            } else {
                buf[pos++] = '?';
            }
        }
        return pos;
    }

    private static byte[] asciiEscape(char c, boolean attribute) {
        switch (c) {
            case '&':
                return AMP_ESC;
            case '<':
                return LT_ESC;
            case '>':
                return GT_ESC;
            case '\r':
                return CR_ESC;
            case '"':
                return attribute ? QUOT_ESC : null;
            case '\'':
                return attribute ? APOS_ESC : null;
            case '\n':
                return attribute ? LF_ESC : null;
            case '\t':
                return attribute ? TAB_ESC : null;
            default:
                return null;
        }
    }

    static int maxEscapedTextBytes(String value) {
        return value.length() * 5;
    }

    static int maxEscapedAttributeBytes(String value) {
        return value.length() * 6;
    }
}
