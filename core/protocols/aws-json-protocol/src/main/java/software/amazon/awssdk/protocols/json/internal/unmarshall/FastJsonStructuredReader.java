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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReferenceArray;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.protocols.json.JsonMemberTable;
import software.amazon.awssdk.protocols.json.StructuredJsonReader;
import software.amazon.awssdk.utils.DateUtils;

/**
 * A {@link StructuredJsonReader} over raw JSON text bytes, bypassing Jackson entirely. Used for
 * generated {@code StructuredJsonReadable} builders when the response body is JSON text (CBOR keeps
 * the Jackson-backed reader).
 *
 * <p>Performance techniques (modelled on smithy-java's byte-level deserializer):
 * <ul>
 *     <li>Single {@code byte[]} cursor; no token objects, no intermediate text buffers.</li>
 *     <li>Member matching against {@link JsonMemberTable}'s packed-long name identities — one long
 *     comparison per candidate for names up to 7 bytes, no String allocation.</li>
 *     <li>Short-string dedup cache: strings of up to 8 bytes (map keys, enum-ish values, repeated
 *     across collection entries) are decoded once and shared; the cache arrays are pooled across
 *     parses in a lock-free striped pool.</li>
 *     <li>Numbers parsed directly from digits; base64 decoded straight from the buffer region.</li>
 * </ul>
 */
@SdkInternalApi
final class FastJsonStructuredReader implements StructuredJsonReader {

    private static final int MAX_DEPTH = 1000;

    // ------------------------------------------------------------------
    // Short-string dedup cache, pooled across parses (see smithy-java's SmithyJsonDeserializer:
    // repeated short keys/values are decoded once; the packed bytes of a string <= 8 bytes form an
    // exact identity, so a hit returns the shared String with no byte comparison; stale entries
    // from a prior document are still byte-identical to any new match, so reuse without clearing
    // is safe).
    // ------------------------------------------------------------------
    private static final int STR_CACHE_SIZE = 256;
    private static final int STR_CACHE_MASK = STR_CACHE_SIZE - 1;
    private static final int CACHE_POOL_SLOTS;
    private static final int CACHE_POOL_MASK;
    private static final AtomicReferenceArray<StringCache> CACHE_POOL;
    private static final int CACHE_MAX_PROBE = 3;

    static {
        int raw = Runtime.getRuntime().availableProcessors() * 4;
        CACHE_POOL_SLOTS = Integer.highestOneBit(Math.max(raw - 1, 1)) << 1;
        CACHE_POOL_MASK = CACHE_POOL_SLOTS - 1;
        CACHE_POOL = new AtomicReferenceArray<>(CACHE_POOL_SLOTS);
    }

    private static final class StringCache {
        private final long[] keys = new long[STR_CACHE_SIZE];
        private final String[] vals = new String[STR_CACHE_SIZE];
    }

    private final byte[] buf;
    private final int end;
    private final TimestampFormatTrait.Format defaultTimestampFormat;
    private int pos;
    private int depth;
    private StringCache stringCache;

    private FastJsonStructuredReader(byte[] buf, int offset, int length,
                                     TimestampFormatTrait.Format defaultTimestampFormat) {
        this.buf = buf;
        this.pos = offset;
        this.end = offset + length;
        this.defaultTimestampFormat = defaultTimestampFormat;
    }

    /**
     * Parses a complete document into the given readable builder. Returns false if the document is
     * the JSON literal {@code null} (callers return a null shape), true otherwise (including an
     * empty body, which leaves the builder untouched, matching the Jackson path).
     */
    static boolean parseDocument(byte[] body, int offset, int length,
                                 TimestampFormatTrait.Format defaultTimestampFormat,
                                 software.amazon.awssdk.protocols.json.StructuredJsonReadable target) {
        FastJsonStructuredReader reader = new FastJsonStructuredReader(body, offset, length, defaultTimestampFormat);
        try {
            reader.skipWs();
            if (reader.pos >= reader.end) {
                return true;
            }
            if (reader.buf[reader.pos] == 'n') {
                reader.expectLiteral("null");
                return false;
            }
            target.readJsonFields(reader);
            return true;
        } finally {
            reader.releaseCache();
        }
    }

    // ------------------------------------------------------------------
    // Structure
    // ------------------------------------------------------------------

    @Override
    public <T> void readStruct(T state, JsonMemberTable table, StructMemberConsumer<T> consumer) {
        expect('{');
        if (++depth > MAX_DEPTH) {
            throw error("Maximum nesting depth exceeded");
        }
        skipWs();
        if (peek() == '}') {
            pos++;
            depth--;
            return;
        }
        while (true) {
            skipWs();
            if (peek() != '"') {
                throw error("Expected field name");
            }
            pos++;
            // Scan the name; escape-free names match directly against the table's bytes.
            int nameStart = pos;
            boolean escaped = false;
            while (pos < end) {
                byte b = buf[pos];
                if (b == '"') {
                    break;
                }
                if (b == '\\') {
                    escaped = true;
                    pos++;
                }
                pos++;
            }
            if (pos >= end) {
                throw error("Unterminated field name");
            }
            int nameEnd = pos;
            pos++; // closing quote

            int memberIndex;
            if (!escaped) {
                memberIndex = table.indexOf(buf, nameStart, nameEnd);
            } else {
                String decoded = decodeEscapedString(nameStart, nameEnd);
                memberIndex = table.indexOf(decoded);
            }

            skipWs();
            expect(':');
            skipWs();

            if (memberIndex >= 0) {
                if (!readNullIfPresent()) {
                    consumer.accept(state, memberIndex, this);
                }
            } else {
                skipValue();
            }

            skipWs();
            byte b = peek();
            if (b == ',') {
                pos++;
            } else if (b == '}') {
                pos++;
                depth--;
                return;
            } else {
                throw error("Expected ',' or '}'");
            }
        }
    }

    @Override
    public <T> void readList(T state, ListElementConsumer<T> consumer) {
        expect('[');
        if (++depth > MAX_DEPTH) {
            throw error("Maximum nesting depth exceeded");
        }
        skipWs();
        if (peek() == ']') {
            pos++;
            depth--;
            return;
        }
        while (true) {
            skipWs();
            consumer.accept(state, this);
            skipWs();
            byte b = peek();
            if (b == ',') {
                pos++;
            } else if (b == ']') {
                pos++;
                depth--;
                return;
            } else {
                throw error("Expected ',' or ']'");
            }
        }
    }

    @Override
    public <T> void readStringMap(T state, MapEntryConsumer<T> consumer) {
        expect('{');
        if (++depth > MAX_DEPTH) {
            throw error("Maximum nesting depth exceeded");
        }
        skipWs();
        if (peek() == '}') {
            pos++;
            depth--;
            return;
        }
        while (true) {
            skipWs();
            String key = readString();
            skipWs();
            expect(':');
            skipWs();
            consumer.accept(state, key, this);
            skipWs();
            byte b = peek();
            if (b == ',') {
                pos++;
            } else if (b == '}') {
                pos++;
                depth--;
                return;
            } else {
                throw error("Expected ',' or '}'");
            }
        }
    }

    @Override
    public boolean readNullIfPresent() {
        skipWs();
        if (pos + 4 <= end && buf[pos] == 'n' && buf[pos + 1] == 'u' && buf[pos + 2] == 'l' && buf[pos + 3] == 'l') {
            pos += 4;
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Scalars
    // ------------------------------------------------------------------

    @Override
    public String readString() {
        skipWs();
        if (peek() != '"') {
            // The Jackson path returns the textual form of any scalar token for string fields.
            if (isNumberStart(peek())) {
                int start = pos;
                skipNumber();
                return new String(buf, start, pos - start, StandardCharsets.US_ASCII);
            }
            if (peek() == 't') {
                expectLiteral("true");
                return "true";
            }
            if (peek() == 'f') {
                expectLiteral("false");
                return "false";
            }
            throw error("Expected string");
        }
        pos++;
        int start = pos;
        // Fast scan: bail to the escape decoder on the first backslash; reject raw control bytes.
        while (pos < end) {
            byte b = buf[pos];
            if (b == '"') {
                int length = pos - start;
                pos++;
                return decodeCached(start, length);
            }
            if (b == '\\') {
                return readStringSlow(start);
            }
            if ((b & 0xFF) < 0x20) {
                throw error("Illegal unquoted control character in string");
            }
            pos++;
        }
        throw error("Unterminated string");
    }

    private String readStringSlow(int start) {
        // Continue scanning to find the end while unescaping into a builder.
        StringBuilder sb = new StringBuilder(16);
        // Copy the already-validated prefix.
        sb.append(new String(buf, start, pos - start, StandardCharsets.UTF_8));
        while (pos < end) {
            byte b = buf[pos];
            if (b == '"') {
                pos++;
                return sb.toString();
            }
            if (b == '\\') {
                pos++;
                if (pos >= end) {
                    break;
                }
                byte esc = buf[pos++];
                switch (esc) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (pos + 4 > end) {
                            throw error("Truncated unicode escape");
                        }
                        sb.append((char) ((hexDigit(buf[pos]) << 12)
                                          | (hexDigit(buf[pos + 1]) << 8)
                                          | (hexDigit(buf[pos + 2]) << 4)
                                          | hexDigit(buf[pos + 3])));
                        pos += 4;
                        break;
                    default:
                        throw error("Invalid escape character");
                }
            } else if ((b & 0xFF) < 0x20) {
                throw error("Illegal unquoted control character in string");
            } else if ((b & 0x80) == 0) {
                sb.append((char) b);
                pos++;
            } else {
                // Multi-byte UTF-8: find the run of non-special bytes and decode together.
                int runStart = pos;
                while (pos < end && buf[pos] != '"' && buf[pos] != '\\') {
                    pos++;
                }
                sb.append(new String(buf, runStart, pos - runStart, StandardCharsets.UTF_8));
            }
        }
        throw error("Unterminated string");
    }

    private int hexDigit(byte b) {
        int c = b & 0xFF;
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        throw error("Invalid hex digit in unicode escape");
    }

    private String decodeEscapedString(int start, int endExclusive) {
        int savedPos = pos;
        int savedEndScan;
        // Reuse the slow decoder over the name region by temporarily repositioning.
        this.pos = start;
        try {
            StringBuilder sb = new StringBuilder(endExclusive - start);
            while (pos < endExclusive) {
                byte b = buf[pos];
                if (b == '\\') {
                    pos++;
                    byte esc = buf[pos++];
                    switch (esc) {
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        case '/':
                            sb.append('/');
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            sb.append((char) ((hexDigit(buf[pos]) << 12)
                                              | (hexDigit(buf[pos + 1]) << 8)
                                              | (hexDigit(buf[pos + 2]) << 4)
                                              | hexDigit(buf[pos + 3])));
                            pos += 4;
                            break;
                        default:
                            throw error("Invalid escape character");
                    }
                } else if ((b & 0x80) == 0) {
                    sb.append((char) b);
                    pos++;
                } else {
                    int runStart = pos;
                    while (pos < endExclusive && buf[pos] != '\\') {
                        pos++;
                    }
                    sb.append(new String(buf, runStart, pos - runStart, StandardCharsets.UTF_8));
                }
            }
            return sb.toString();
        } finally {
            this.pos = savedPos;
        }
    }

    @Override
    public int readInt() {
        long value = readIntegral("int");
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw error("Value out of int range");
        }
        return (int) value;
    }

    @Override
    public long readLong() {
        return readIntegral("long");
    }

    @Override
    public short readShort() {
        long value = readIntegral("short");
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw error("Value out of short range");
        }
        return (short) value;
    }

    @Override
    public byte readByte() {
        long value = readIntegral("byte");
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw error("Value out of byte range");
        }
        return (byte) value;
    }

    private long readIntegral(String type) {
        skipWs();
        if (peek() == '"') {
            // Quoted numbers behave like the Jackson path: parse the decoded string.
            return Long.parseLong(readString());
        }
        boolean negative = false;
        int p = pos;
        if (p < end && buf[p] == '-') {
            negative = true;
            p++;
        }
        if (p >= end || buf[p] < '0' || buf[p] > '9') {
            throw error("Expected " + type);
        }
        long value = 0;
        int digits = 0;
        while (p < end) {
            byte b = buf[p];
            if (b >= '0' && b <= '9') {
                value = value * 10 + (b - '0');
                digits++;
                p++;
            } else if (b == '.' || b == 'e' || b == 'E') {
                throw error("Expected integral " + type + ", found decimal");
            } else {
                break;
            }
        }
        if (digits > 18) {
            // Rare: may exceed long accumulation; re-parse precisely with overflow detection.
            int start = pos;
            pos = p;
            return Long.parseLong(new String(buf, start, p - start, StandardCharsets.US_ASCII));
        }
        pos = p;
        return negative ? -value : value;
    }

    @Override
    public float readFloat() {
        return (float) readFloating();
    }

    @Override
    public double readDouble() {
        return readFloating();
    }

    private double readFloating() {
        skipWs();
        if (peek() == '"') {
            // Handles quoted numbers plus "NaN"/"Infinity"/"-Infinity", like the Jackson path.
            return Double.parseDouble(readString());
        }
        int start = pos;
        skipNumber();
        return Double.parseDouble(new String(buf, start, pos - start, StandardCharsets.US_ASCII));
    }

    @Override
    public BigDecimal readBigDecimal() {
        skipWs();
        if (peek() == '"') {
            return new BigDecimal(readString());
        }
        int start = pos;
        skipNumber();
        return new BigDecimal(new String(buf, start, pos - start, StandardCharsets.US_ASCII));
    }

    @Override
    public boolean readBoolean() {
        skipWs();
        byte b = peek();
        if (b == 't') {
            expectLiteral("true");
            return true;
        }
        if (b == 'f') {
            expectLiteral("false");
            return false;
        }
        if (b == '"') {
            return Boolean.parseBoolean(readString());
        }
        throw error("Expected boolean");
    }

    @Override
    public SdkBytes readSdkBytes() {
        skipWs();
        if (peek() != '"') {
            throw error("Expected base64 string");
        }
        pos++;
        int start = pos;
        while (pos < end && buf[pos] != '"') {
            if (buf[pos] == '\\') {
                // Escapes in base64 are not produced by AWS services; fall back to string decode.
                pos = start - 1;
                return SdkBytes.fromByteArrayUnsafe(Base64.getDecoder().decode(readString()));
            }
            pos++;
        }
        if (pos >= end) {
            throw error("Unterminated string");
        }
        int length = pos - start;
        pos++;
        byte[] region = new byte[length];
        System.arraycopy(buf, start, region, 0, length);
        return SdkBytes.fromByteArrayUnsafe(Base64.getDecoder().decode(region));
    }

    @Override
    public Instant readInstant(TimestampFormatTrait.Format format) {
        skipWs();
        TimestampFormatTrait.Format resolved = format != null ? format : defaultTimestampFormat;
        switch (resolved) {
            case UNIX_TIMESTAMP_MILLIS:
                return Instant.ofEpochMilli(readIntegral("timestamp"));
            case UNIX_TIMESTAMP: {
                String text = readNumberOrStringText();
                try {
                    return DateUtils.parseUnixTimestampInstant(text);
                } catch (NumberFormatException e) {
                    throw SdkClientException.builder()
                                            .message("Unable to parse date : " + text)
                                            .cause(e)
                                            .build();
                }
            }
            case ISO_8601:
                return DateUtils.parseIso8601Date(readQuotedText());
            case RFC_822:
                return DateUtils.parseRfc822Date(readQuotedText());
            default:
                throw SdkClientException.create("Unrecognized timestamp format - " + resolved);
        }
    }

    private String readNumberOrStringText() {
        if (peek() == '"') {
            return readString();
        }
        int start = pos;
        skipNumber();
        return new String(buf, start, pos - start, StandardCharsets.US_ASCII);
    }

    private String readQuotedText() {
        if (peek() != '"') {
            throw error("Expected string");
        }
        return readString();
    }

    // ------------------------------------------------------------------
    // Skipping
    // ------------------------------------------------------------------

    private void skipValue() {
        skipWs();
        byte b = peek();
        switch (b) {
            case '"':
                skipString();
                return;
            case '{':
            case '[':
                skipContainer();
                return;
            case 't':
                expectLiteral("true");
                return;
            case 'f':
                expectLiteral("false");
                return;
            case 'n':
                expectLiteral("null");
                return;
            default:
                if (isNumberStart(b)) {
                    skipNumber();
                    return;
                }
                throw error("Unexpected character");
        }
    }

    private void skipString() {
        pos++; // opening quote
        while (pos < end) {
            byte b = buf[pos];
            if (b == '"') {
                pos++;
                return;
            }
            if (b == '\\') {
                pos++;
            }
            pos++;
        }
        throw error("Unterminated string");
    }

    private void skipContainer() {
        int nesting = 0;
        while (pos < end) {
            byte b = buf[pos];
            if (b == '"') {
                skipString();
                continue;
            }
            if (b == '{' || b == '[') {
                nesting++;
                if (depth + nesting > MAX_DEPTH) {
                    throw error("Maximum nesting depth exceeded");
                }
            } else if (b == '}' || b == ']') {
                nesting--;
                if (nesting == 0) {
                    pos++;
                    return;
                }
            }
            pos++;
        }
        throw error("Unterminated value");
    }

    private void skipNumber() {
        if (pos < end && buf[pos] == '-') {
            pos++;
        }
        while (pos < end) {
            byte b = buf[pos];
            if ((b >= '0' && b <= '9') || b == '.' || b == 'e' || b == 'E' || b == '+' || b == '-') {
                pos++;
            } else {
                break;
            }
        }
    }

    private static boolean isNumberStart(byte b) {
        return b == '-' || (b >= '0' && b <= '9');
    }

    // ------------------------------------------------------------------
    // Short-string dedup cache
    // ------------------------------------------------------------------

    private String decodeCached(int start, int length) {
        if (length == 0) {
            return "";
        }
        if (length > 8) {
            return new String(buf, start, length, StandardCharsets.UTF_8);
        }
        long key = 0;
        for (int i = 0; i < length; i++) {
            key = (key << 8) | (buf[start + i] & 0xFFL);
        }
        StringCache cache = stringCache;
        if (cache == null) {
            cache = acquireCache();
        }
        int slot = (int) ((key * 0x9E3779B97F4A7C15L) >>> 48) & STR_CACHE_MASK;
        if (cache.keys[slot] == key) {
            return cache.vals[slot];
        }
        String s = new String(buf, start, length, StandardCharsets.UTF_8);
        cache.keys[slot] = key;
        cache.vals[slot] = s;
        return s;
    }

    private static int cachePoolProbe() {
        long id = Thread.currentThread().getId();
        return (int) (id ^ (id >>> 16)) & CACHE_POOL_MASK;
    }

    private StringCache acquireCache() {
        int base = cachePoolProbe();
        for (int i = 0; i < CACHE_MAX_PROBE; i++) {
            int idx = (base + i) & CACHE_POOL_MASK;
            StringCache c = CACHE_POOL.get(idx);
            if (c != null && CACHE_POOL.compareAndSet(idx, c, null)) {
                stringCache = c;
                return c;
            }
        }
        StringCache c = new StringCache();
        stringCache = c;
        return c;
    }

    private void releaseCache() {
        StringCache cache = stringCache;
        if (cache == null) {
            return;
        }
        stringCache = null;
        int base = cachePoolProbe();
        for (int i = 0; i < CACHE_MAX_PROBE; i++) {
            int idx = (base + i) & CACHE_POOL_MASK;
            if (CACHE_POOL.get(idx) == null && CACHE_POOL.compareAndSet(idx, null, cache)) {
                return;
            }
        }
        // Pool full: drop, GC collects.
    }

    // ------------------------------------------------------------------
    // Primitives
    // ------------------------------------------------------------------

    /**
     * Positions {@link #pos} on the next non-whitespace byte.
     *
     * <p>Called several times per member — before the name, around the colon, and again inside whichever value
     * reader runs — so on a response of any size this runs thousands of times, and AWS JSON responses contain no
     * whitespace at all. The check is therefore split: every whitespace byte is {@code <= ' '} and every byte that
     * can legally start a JSON name or value is {@code > ' '}, so one comparison settles the common case, and this
     * method stays small enough to inline into all of its callers. Anything else, including the whitespace that
     * hand-written or proxied JSON does contain, falls into the out-of-line loop.
     */
    private void skipWs() {
        if (pos < end && buf[pos] > ' ') {
            return;
        }
        skipWsSlow();
    }

    private void skipWsSlow() {
        while (pos < end) {
            byte b = buf[pos];
            if (b == ' ' || b == '\n' || b == '\r' || b == '\t') {
                pos++;
            } else {
                break;
            }
        }
    }

    private byte peek() {
        if (pos >= end) {
            throw error("Unexpected end of input");
        }
        return buf[pos];
    }

    private void expect(char c) {
        skipWs();
        if (pos >= end || buf[pos] != c) {
            throw error("Expected '" + c + "'");
        }
        pos++;
    }

    private void expectLiteral(String literal) {
        int len = literal.length();
        if (pos + len > end) {
            throw error("Unexpected end of input");
        }
        for (int i = 0; i < len; i++) {
            if (buf[pos + i] != literal.charAt(i)) {
                throw error("Invalid token");
            }
        }
        pos += len;
    }

    private SdkClientException error(String message) {
        int contextEnd = Math.min(end, pos + 16);
        int contextStart = Math.max(0, Math.min(pos, end));
        String context = new String(buf, contextStart, contextEnd - contextStart, StandardCharsets.UTF_8);
        return SdkClientException.create(message + " at offset " + pos + " near: " + context);
    }
}
