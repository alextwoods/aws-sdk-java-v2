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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The JSON reader of the policy builder: parses one JSON value into a {@link Node} tree with the document semantics
 * of the V2 policy builder's parser (V2 {@code JsonNodeParser.create()}, Jackson with Java comments allowed), so V3
 * accepts and rejects exactly the documents V2 did:
 *
 * <ul>
 *   <li>strict JSON (RFC 8259) plus {@code //} and {@code /* *}{@code /} comments wherever whitespace is allowed;</li>
 *   <li>only the first root value is read: content after it is ignored, except that a root number must be followed by
 *       whitespace or the end of input;</li>
 *   <li>duplicate object member names keep the first position and the last value;</li>
 *   <li>numbers keep their exact source text;</li>
 *   <li>byte input is decoded as UTF-8, UTF-16 or UTF-32 (detected from a BOM or the zero-byte pattern of the first
 *       bytes); malformed encodings are rejected;</li>
 *   <li>Jackson's default stream limits: at most {@value #MAX_DEPTH} nested containers, {@value #MAX_NUMBER_LENGTH}
 *       characters per number, {@value #MAX_NAME_LENGTH} per member name and {@value #MAX_STRING_LENGTH} per
 *       string.</li>
 * </ul>
 *
 * <p>Malformed documents fail with {@link PolicyJsonParseException}. Not a public API.
 */
public final class PolicyJsonReader {
    static final int MAX_DEPTH = 1000;
    static final int MAX_NUMBER_LENGTH = 1000;
    static final int MAX_NAME_LENGTH = 50_000;
    static final int MAX_STRING_LENGTH = 20_000_000;

    private static final int EOF = -1;

    private final Reader reader;
    private final char[] buffer = new char[8192];
    private int length;
    private int position;
    private long line = 1;
    private long column;

    private PolicyJsonReader(Reader reader) {
        this.reader = reader;
    }

    /** A parsed JSON value. */
    public sealed interface Node permits ObjectNode, ArrayNode, StringNode, NumberNode, BooleanNode, NullNode {
    }

    /** A JSON object; members in document order. */
    public record ObjectNode(Map<String, Node> members) implements Node {
    }

    /** A JSON array. */
    public record ArrayNode(List<Node> elements) implements Node {
    }

    /** A JSON string. */
    public record StringNode(String value) implements Node {
    }

    /** A JSON number, as its exact source text. */
    public record NumberNode(String text) implements Node {
    }

    /** A JSON boolean. */
    public record BooleanNode(boolean value) implements Node {
    }

    /** The JSON {@code null}. */
    public enum NullNode implements Node {
        INSTANCE
    }

    /**
     * Parses the first JSON value of {@code input}, reading no further than its end (and one character after a root
     * scalar). Does not close {@code input}.
     *
     * @return the value, or {@code null} if the input holds only whitespace and comments
     * @throws PolicyJsonParseException if the document is not well-formed
     * @throws IOException if reading {@code input} fails or its encoding is malformed
     */
    public static Node parse(InputStream input) throws IOException {
        BufferedInputStream in = new BufferedInputStream(input);
        in.mark(4);
        byte[] head = in.readNBytes(4);
        in.reset();
        Encoding encoding = detect(head);
        in.skipNBytes(encoding.skip);
        Reader reader = new InputStreamReader(in, encoding.charset.newDecoder()
                                                                  .onMalformedInput(CodingErrorAction.REPORT)
                                                                  .onUnmappableCharacter(CodingErrorAction.REPORT));
        return new PolicyJsonReader(reader).root();
    }

    private record Encoding(Charset charset, int skip) {
    }

    /** The encoding of a JSON byte stream, from its byte-order mark or the zero bytes of its first characters. */
    private static Encoding detect(byte[] head) throws PolicyJsonParseException {
        int n = head.length;
        int b0 = n > 0 ? head[0] & 0xFF : -1;
        int b1 = n > 1 ? head[1] & 0xFF : -1;
        if (n == 4) {
            int quad = (b0 << 24) | (b1 << 16) | ((head[2] & 0xFF) << 8) | (head[3] & 0xFF);
            switch (quad) {
                case 0x0000FEFF:
                    return new Encoding(Charset.forName("UTF-32BE"), 4);
                case 0xFFFE0000:
                    return new Encoding(Charset.forName("UTF-32LE"), 4);
                case 0x0000FFFE:
                case 0xFEFF0000:
                    throw new PolicyJsonParseException("Unsupported UCS-4 byte order (2143 or 3412)");
                default:
                    break;
            }
        }
        if (n >= 2) {
            int pair = (b0 << 8) | b1;
            if (pair == 0xFEFF) {
                return new Encoding(StandardCharsets.UTF_16BE, 2);
            }
            if (pair == 0xFFFE) {
                return new Encoding(StandardCharsets.UTF_16LE, 2);
            }
        }
        if (n >= 3 && b0 == 0xEF && b1 == 0xBB && (head[2] & 0xFF) == 0xBF) {
            return new Encoding(StandardCharsets.UTF_8, 3);
        }
        if (n == 4) {
            int quad = (b0 << 24) | (b1 << 16) | ((head[2] & 0xFF) << 8) | (head[3] & 0xFF);
            if ((quad >>> 8) == 0) {
                return new Encoding(Charset.forName("UTF-32BE"), 0);
            }
            if ((quad & 0x00FFFFFF) == 0) {
                return new Encoding(Charset.forName("UTF-32LE"), 0);
            }
            if ((quad & ~0x00FF0000) == 0 || (quad & ~0x0000FF00) == 0) {
                throw new PolicyJsonParseException("Unsupported UCS-4 byte order (2143 or 3412)");
            }
        }
        if (n >= 2) {
            if (b0 == 0) {
                return new Encoding(StandardCharsets.UTF_16BE, 0);
            }
            if (b1 == 0) {
                return new Encoding(StandardCharsets.UTF_16LE, 0);
            }
        }
        return new Encoding(StandardCharsets.UTF_8, 0);
    }

    private Node root() throws IOException {
        int c = skipSeparators();
        if (c == EOF) {
            return null;
        }
        Node value = value(c, 0);
        if (value instanceof NumberNode) {
            int next = peek();
            if (next != EOF && !isWhitespace(next)) {
                throw error("Expected space separating root-level values", next);
            }
        }
        return value;
    }

    /** Parses the value starting with the already consumed character {@code c}. */
    private Node value(int c, int depth) throws IOException {
        switch (c) {
            case '{':
                return object(depth + 1);
            case '[':
                return array(depth + 1);
            case '"':
                return new StringNode(string(MAX_STRING_LENGTH));
            case 't':
                literal("rue");
                return new BooleanNode(true);
            case 'f':
                literal("alse");
                return new BooleanNode(false);
            case 'n':
                literal("ull");
                return NullNode.INSTANCE;
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return number(c);
                }
                throw error("Unexpected character, expected a JSON value", c);
        }
    }

    private ObjectNode object(int depth) throws IOException {
        checkDepth(depth);
        Map<String, Node> members = new LinkedHashMap<>();
        int c = skipSeparators();
        if (c == '}') {
            return new ObjectNode(Collections.unmodifiableMap(members));
        }
        while (true) {
            if (c != '"') {
                throw error("Unexpected character, expected a double-quoted member name", c);
            }
            String name = string(MAX_NAME_LENGTH);
            c = skipSeparators();
            if (c != ':') {
                throw error("Unexpected character, expected ':' after a member name", c);
            }
            members.put(name, value(skipSeparators(), depth));
            c = skipSeparators();
            if (c == '}') {
                return new ObjectNode(Collections.unmodifiableMap(members));
            }
            if (c != ',') {
                throw error("Unexpected character, expected ',' or '}' in an object", c);
            }
            c = skipSeparators();
        }
    }

    private ArrayNode array(int depth) throws IOException {
        checkDepth(depth);
        List<Node> elements = new ArrayList<>();
        int c = skipSeparators();
        if (c == ']') {
            return new ArrayNode(Collections.unmodifiableList(elements));
        }
        while (true) {
            elements.add(value(c, depth));
            c = skipSeparators();
            if (c == ']') {
                return new ArrayNode(Collections.unmodifiableList(elements));
            }
            if (c != ',') {
                throw error("Unexpected character, expected ',' or ']' in an array", c);
            }
            c = skipSeparators();
        }
    }

    private void checkDepth(int depth) throws PolicyJsonParseException {
        if (depth > MAX_DEPTH) {
            throw limit("Document nesting depth", MAX_DEPTH);
        }
    }

    /** Reads the rest of a string whose opening quote was consumed. */
    private String string(int maxLength) throws IOException {
        StringBuilder result = new StringBuilder();
        while (true) {
            int c = read();
            if (c == '"') {
                return result.toString();
            }
            if (c == EOF) {
                throw error("Unexpected end-of-input in a string", c);
            }
            if (c < 0x20) {
                throw error("Illegal unquoted control character in a string", c);
            }
            if (c == '\\') {
                c = escape();
            }
            if (result.length() == maxLength) {
                throw limit("String length", maxLength);
            }
            result.append((char) c);
        }
    }

    private int escape() throws IOException {
        int c = read();
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
            case 'u': {
                int value = 0;
                for (int i = 0; i < 4; i++) {
                    int h = read();
                    int digit = Character.digit(h, 16);
                    if (h == EOF || h > 0x7F || digit < 0) {
                        throw error("Expected a hexadecimal digit in a \\u escape", h);
                    }
                    value = (value << 4) | digit;
                }
                return value;
            }
            default:
                throw error("Unrecognized character escape", c);
        }
    }

    private void literal(String rest) throws IOException {
        for (int i = 0; i < rest.length(); i++) {
            int c = read();
            if (c != rest.charAt(i)) {
                throw error("Unrecognized token", c);
            }
        }
        int next = peek();
        if (next != EOF && Character.isJavaIdentifierPart(next)) {
            throw error("Unrecognized token", next);
        }
    }

    /** Reads the rest of a number whose first character {@code first} was consumed; keeps its exact text. */
    private NumberNode number(int first) throws IOException {
        StringBuilder text = new StringBuilder();
        text.append((char) first);
        int c = first;
        if (c == '-') {
            c = read();
            if (c < '0' || c > '9') {
                throw error("Expected a digit after a minus sign", c);
            }
            text.append((char) c);
        }
        if (c == '0') {
            int next = peek();
            if (next >= '0' && next <= '9') {
                throw error("Invalid numeric value: Leading zeroes not allowed", next);
            }
        } else {
            digits(text);
        }
        if (peek() == '.') {
            text.append((char) read());
            if (digits(text) == 0) {
                throw error("Decimal point not followed by a digit", peek());
            }
        }
        if (peek() == 'e' || peek() == 'E') {
            text.append((char) read());
            if (peek() == '+' || peek() == '-') {
                text.append((char) read());
            }
            if (digits(text) == 0) {
                throw error("Exponent indicator not followed by a digit", peek());
            }
        }
        if (text.length() > MAX_NUMBER_LENGTH) {
            throw limit("Number length", MAX_NUMBER_LENGTH);
        }
        return new NumberNode(text.toString());
    }

    private int digits(StringBuilder text) throws IOException {
        int count = 0;
        while (peek() >= '0' && peek() <= '9') {
            text.append((char) read());
            count++;
            if (text.length() > MAX_NUMBER_LENGTH) {
                throw limit("Number length", MAX_NUMBER_LENGTH);
            }
        }
        return count;
    }

    /** Skips whitespace and comments; returns (consuming) the next character, or {@link #EOF}. */
    private int skipSeparators() throws IOException {
        while (true) {
            int c = read();
            if (isWhitespace(c)) {
                continue;
            }
            if (c != '/') {
                return c;
            }
            int kind = read();
            if (kind == '/') {
                int d = peek();
                while (d != EOF && d != '\n' && d != '\r') {
                    read();
                    d = peek();
                }
            } else if (kind == '*') {
                int previous = 0;
                while (true) {
                    int d = read();
                    if (d == EOF) {
                        throw error("Unexpected end-of-input in a comment", d);
                    }
                    if (previous == '*' && d == '/') {
                        break;
                    }
                    previous = d;
                }
            } else {
                throw error("Unexpected character, expected '/' or '*' to start a comment", kind);
            }
        }
    }

    private static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private int peek() throws IOException {
        if (position == length && !fill()) {
            return EOF;
        }
        return buffer[position];
    }

    private int read() throws IOException {
        if (position == length && !fill()) {
            return EOF;
        }
        char c = buffer[position++];
        if (c == '\n') {
            line++;
            column = 0;
        } else {
            column++;
        }
        return c;
    }

    private boolean fill() throws IOException {
        int n = reader.read(buffer, 0, buffer.length);
        if (n <= 0) {
            return false;
        }
        length = n;
        position = 0;
        return true;
    }

    private PolicyJsonParseException error(String message, int c) {
        String found = c == EOF ? "end-of-input"
                                : c < 0x20 || c == 0x7F ? String.format("character U+%04X", c)
                                                        : "'" + (char) c + "'";
        return new PolicyJsonParseException(message + ": found " + found + location());
    }

    private PolicyJsonParseException limit(String what, int maximum) {
        return new PolicyJsonParseException(what + " exceeds the maximum allowed (" + maximum + ")" + location());
    }

    /** The position of the last consumed character, as Jackson reports it (1-based line and column). */
    private String location() {
        return " at [line: " + line + ", column: " + column + "]";
    }
}
