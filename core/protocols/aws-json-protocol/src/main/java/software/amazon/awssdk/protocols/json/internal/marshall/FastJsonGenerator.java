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

package software.amazon.awssdk.protocols.json.internal.marshall;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.awssdk.utils.DateUtils;

/**
 * A {@link StructuredJsonGenerator} that writes JSON directly into a {@code byte[]} without going through
 * Jackson or an {@code OutputStream}. Produces byte-identical output to the Jackson-based
 * {@link software.amazon.awssdk.protocols.json.SdkJsonGenerator} for the write operations the SDK marshallers
 * use (verified by {@code FastJsonGeneratorWireIdentityTest}).
 *
 * <p>Performance techniques (modelled on smithy-java's native JSON serializer):
 * <ul>
 *     <li>Single {@code byte[]} cursor with worst-case capacity reservations per write and a separate cold
 *     {@link #grow} method so the JIT can inline the {@link #ensureCapacity} fast path.</li>
 *     <li>Comma insertion tracked with a per-depth {@code boolean[]} instead of per-level context objects.</li>
 *     <li>Two-digits-at-a-time integer emission.</li>
 *     <li>ASCII fast path for strings and field names (single pass, no intermediate encode buffers); slow path
 *     only when a character needs escaping or multi-byte encoding.</li>
 *     <li>Base64 encoded directly into the output buffer.</li>
 *     <li>Zero-copy handoff to the HTTP layer via {@link ContentStreamProvider#contentAsByteBufferOrNull()}.</li>
 * </ul>
 */
@SdkInternalApi
public final class FastJsonGenerator implements StructuredJsonGenerator {

    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final int INITIAL_DEPTH = 32;

    private static final byte[] NULL_BYTES = {'n', 'u', 'l', 'l'};
    private static final byte[] TRUE_BYTES = {'t', 'r', 'u', 'e'};
    private static final byte[] FALSE_BYTES = {'f', 'a', 'l', 's', 'e'};
    private static final byte[] NAN_BYTES = {'"', 'N', 'a', 'N', '"'};
    private static final byte[] POS_INF_BYTES = {'"', 'I', 'n', 'f', 'i', 'n', 'i', 't', 'y', '"'};
    private static final byte[] NEG_INF_BYTES = {'"', '-', 'I', 'n', 'f', 'i', 'n', 'i', 't', 'y', '"'};

    /**
     * Uppercase, matching Jackson's escape output.
     */
    private static final byte[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static final byte[] BASE64 = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    /**
     * Two-character decimal digit pairs "00".."99", used to emit integers two digits at a time.
     */
    private static final byte[] DIGIT_PAIRS = new byte[200];

    /**
     * Escape metadata for ASCII characters: 0 = no escape needed, -1 = \\u00XX escape, otherwise the
     * character of a two-byte backslash escape (e.g. 'n' for \\n).
     */
    private static final byte[] ESCAPE = new byte[128];

    static {
        for (int i = 0; i < 100; i++) {
            DIGIT_PAIRS[i * 2] = (byte) ('0' + i / 10);
            DIGIT_PAIRS[i * 2 + 1] = (byte) ('0' + i % 10);
        }
        for (int i = 0; i < 0x20; i++) {
            ESCAPE[i] = -1;
        }
        ESCAPE['"'] = '"';
        ESCAPE['\\'] = '\\';
        ESCAPE['\b'] = 'b';
        ESCAPE['\t'] = 't';
        ESCAPE['\n'] = 'n';
        ESCAPE['\f'] = 'f';
        ESCAPE['\r'] = 'r';
    }

    private final String contentType;

    private byte[] buf;
    private int pos;

    private int depth;
    private boolean[] needsComma = new boolean[INITIAL_DEPTH];

    /**
     * True immediately after a field name was written; suppresses the comma the following value write
     * would otherwise emit.
     */
    private boolean afterFieldName;

    public FastJsonGenerator(String contentType) {
        this(contentType, DEFAULT_BUFFER_SIZE);
    }

    /**
     * @param initialBufferCapacity initial capacity of the output buffer, e.g. the size of recently marshalled
     *                              bodies for the same operation (see MarshallBufferSizeHints). A correct value
     *                              lets the buffer be allocated once instead of growing to the body size by
     *                              doubling.
     */
    public FastJsonGenerator(String contentType, int initialBufferCapacity) {
        this.contentType = contentType;
        this.buf = new byte[Math.max(16, initialBufferCapacity)];
    }

    // ----------------------------------------------------------------------
    // Structural writes
    // ----------------------------------------------------------------------

    @Override
    public StructuredJsonGenerator writeStartObject() {
        beforeValue(1);
        buf[pos++] = '{';
        push();
        return this;
    }

    @Override
    public StructuredJsonGenerator writeEndObject() {
        depth--;
        ensureCapacity(1);
        buf[pos++] = '}';
        return this;
    }

    @Override
    public StructuredJsonGenerator writeStartArray() {
        beforeValue(1);
        buf[pos++] = '[';
        push();
        return this;
    }

    @Override
    public StructuredJsonGenerator writeEndArray() {
        depth--;
        ensureCapacity(1);
        buf[pos++] = ']';
        return this;
    }

    @Override
    public StructuredJsonGenerator writeFieldName(String fieldName) {
        // Comma (if needed) + name + colon. The following value write must not emit a comma.
        if (needsComma[depth]) {
            ensureCapacity(1);
            buf[pos++] = ',';
        } else {
            needsComma[depth] = true;
        }
        writeQuotedString(fieldName);
        ensureCapacity(1);
        buf[pos++] = ':';
        afterFieldName = true;
        return this;
    }

    @Override
    public StructuredJsonGenerator writeFieldName(String fieldName, byte[] jsonFieldNameToken) {
        // Comma (if needed) + pre-encoded "name": token in a single reservation and copy.
        int tokenLength = jsonFieldNameToken.length;
        ensureCapacity(tokenLength + 1);
        if (needsComma[depth]) {
            buf[pos++] = ',';
        } else {
            needsComma[depth] = true;
        }
        System.arraycopy(jsonFieldNameToken, 0, buf, pos, tokenLength);
        pos += tokenLength;
        afterFieldName = true;
        return this;
    }

    /**
     * Pre-encodes a field name as complete {@code "name":} token bytes (opening quote, escaped UTF-8
     * name, closing quote, colon) for {@link #writeFieldName(String, byte[])}. Uses the exact escaping
     * of {@link #writeFieldName(String)}, so the two paths are byte-identical.
     */
    public static byte[] encodeFieldNameToken(String fieldName) {
        FastJsonGenerator scratch = new FastJsonGenerator(null, fieldName.length() + 3);
        scratch.writeQuotedString(fieldName);
        scratch.ensureCapacity(1);
        scratch.buf[scratch.pos++] = ':';
        return Arrays.copyOf(scratch.buf, scratch.pos);
    }

    // ----------------------------------------------------------------------
    // Values
    // ----------------------------------------------------------------------

    @Override
    public StructuredJsonGenerator writeNull() {
        beforeValue(4);
        System.arraycopy(NULL_BYTES, 0, buf, pos, 4);
        pos += 4;
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(String val) {
        beforeValue(0);
        writeQuotedString(val);
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(boolean bool) {
        if (bool) {
            beforeValue(4);
            System.arraycopy(TRUE_BYTES, 0, buf, pos, 4);
            pos += 4;
        } else {
            beforeValue(5);
            System.arraycopy(FALSE_BYTES, 0, buf, pos, 5);
            pos += 5;
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(int val) {
        beforeValue(11);
        writeIntUnchecked(val);
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(long val) {
        beforeValue(20);
        writeLongUnchecked(val);
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(short val) {
        return writeValue((int) val);
    }

    @Override
    public StructuredJsonGenerator writeValue(byte val) {
        return writeValue((int) val);
    }

    @Override
    public StructuredJsonGenerator writeValue(double val) {
        if (Double.isNaN(val)) {
            writeRawAscii(NAN_BYTES);
        } else if (Double.isInfinite(val)) {
            writeRawAscii(val > 0 ? POS_INF_BYTES : NEG_INF_BYTES);
        } else {
            // Double.toString on JDK 19+ produces the same shortest-round-trip representation as
            // Jackson's fast double writer. On older JDKs the representations can differ in length
            // for a small set of values, but both remain correct round-trippable JSON numbers.
            writeAsciiChars(Double.toString(val));
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(float val) {
        if (Float.isNaN(val)) {
            writeRawAscii(NAN_BYTES);
        } else if (Float.isInfinite(val)) {
            writeRawAscii(val > 0 ? POS_INF_BYTES : NEG_INF_BYTES);
        } else {
            writeAsciiChars(Float.toString(val));
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(BigDecimal value) {
        // Matches SdkJsonGenerator: BigDecimal goes over the wire as a JSON string (see API-433).
        return writeValue(value.toString());
    }

    @Override
    public StructuredJsonGenerator writeValue(BigInteger value) {
        writeAsciiChars(value.toString());
        return this;
    }

    @Override
    public StructuredJsonGenerator writeNumber(String number) {
        writeAsciiChars(number);
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(Instant instant) {
        // Matches SdkJsonGenerator: unix timestamp with millisecond precision, as a raw JSON number.
        writeAsciiChars(DateUtils.formatUnixTimestampInstant(instant));
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(ByteBuffer bytes) {
        ByteBuffer readOnly = bytes.asReadOnlyBuffer();
        int len = readOnly.remaining();
        byte[] data;
        int off;
        if (bytes.hasArray()) {
            data = bytes.array();
            off = bytes.arrayOffset() + bytes.position();
        } else {
            data = new byte[len];
            readOnly.get(data);
            off = 0;
        }
        writeBase64(data, off, len);
        return this;
    }

    @Override
    public StructuredJsonGenerator writeBinaryValue(byte[] bytes) {
        writeBase64(bytes, 0, bytes.length);
        return this;
    }

    // ----------------------------------------------------------------------
    // Content extraction
    // ----------------------------------------------------------------------

    @Override
    public byte[] getBytes() {
        return Arrays.copyOf(buf, pos);
    }

    @Override
    public int contentSize() {
        return pos;
    }

    @Override
    public ContentStreamProvider contentStreamProvider() {
        return new BufferContentStreamProvider(buf, pos);
    }

    /**
     * Zero-copy view over the generator's internal buffer. Same contract as
     * {@code SdkByteArrayOutputStream.SingleBufferContentStreamProvider}: safe because the marshaller
     * finishes all writes before handing out the provider, and each call returns an independent
     * position/limit view over the shared content.
     */
    private static final class BufferContentStreamProvider implements ContentStreamProvider {
        private final byte[] content;
        private final int length;

        private BufferContentStreamProvider(byte[] content, int length) {
            this.content = content;
            this.length = length;
        }

        @Override
        public InputStream newStream() {
            return new ByteArrayInputStream(content, 0, length);
        }

        @Override
        public ByteBuffer contentAsByteBufferOrNull() {
            return ByteBuffer.wrap(content, 0, length);
        }
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    // ----------------------------------------------------------------------
    // Internals
    // ----------------------------------------------------------------------

    /**
     * Emits the comma separating this value from a preceding sibling (unless this value directly follows a
     * field name) and reserves {@code maxValueBytes + 1} of capacity.
     */
    private void beforeValue(int maxValueBytes) {
        ensureCapacity(maxValueBytes + 1);
        if (afterFieldName) {
            afterFieldName = false;
        } else if (needsComma[depth]) {
            buf[pos++] = ',';
        } else {
            needsComma[depth] = true;
        }
    }

    private void push() {
        depth++;
        if (depth >= needsComma.length) {
            needsComma = Arrays.copyOf(needsComma, needsComma.length * 2);
        }
        needsComma[depth] = false;
    }

    private void ensureCapacity(int needed) {
        if (pos + needed > buf.length) {
            grow(needed);
        }
    }

    /**
     * Kept separate and cold so that the JIT inlines {@link #ensureCapacity}'s fast path.
     */
    private void grow(int needed) {
        buf = Arrays.copyOf(buf, Math.max(buf.length * 2, pos + needed));
    }

    /**
     * Writes a pre-encoded ASCII token as a JSON value (comma handling included).
     */
    private void writeRawAscii(byte[] token) {
        beforeValue(token.length);
        System.arraycopy(token, 0, buf, pos, token.length);
        pos += token.length;
    }

    /**
     * Writes a string known to contain only ASCII (numbers, timestamps) as a raw JSON value.
     */
    private void writeAsciiChars(String val) {
        int len = val.length();
        beforeValue(len);
        int p = pos;
        for (int i = 0; i < len; i++) {
            buf[p++] = (byte) val.charAt(i);
        }
        pos = p;
    }

    /**
     * Writes a quoted, escaped JSON string. Fast path handles pure printable-ASCII strings in a single pass;
     * any character needing escaping or multi-byte encoding falls back to the general encoder from that point.
     */
    private void writeQuotedString(String val) {
        int len = val.length();
        // Reserve for the all-ASCII case; the slow path re-reserves pessimistically.
        ensureCapacity(len + 2);
        byte[] b = buf;
        int p = pos;
        b[p++] = '"';
        int i = 0;
        // The JIT auto-vectorizes this loop; any char that breaks the fast path exits to the slow path.
        for (; i < len; i++) {
            char c = val.charAt(i);
            if (c >= 0x80 || (ESCAPE[c] != 0)) {
                break;
            }
            b[p++] = (byte) c;
        }
        if (i == len) {
            b[p++] = '"';
            pos = p;
            return;
        }
        pos = p;
        writeStringSlowPath(val, i, len);
        ensureCapacity(1);
        buf[pos++] = '"';
    }

    /**
     * Encodes the remainder of a string starting at the first character that needs escaping or multi-byte
     * encoding. Matches Jackson's UTF8JsonGenerator escaping: two-character escapes for the JSON control
     * shorthands, {@code \\u00XX} for other control characters, raw UTF-8 for everything above ASCII.
     */
    private void writeStringSlowPath(String val, int start, int len) {
        // Worst case per char: 6 bytes (\ u X X X X).
        ensureCapacity((len - start) * 6);
        byte[] b = buf;
        int p = pos;
        for (int i = start; i < len; i++) {
            char c = val.charAt(i);
            if (c < 0x80) {
                byte esc = ESCAPE[c];
                if (esc == 0) {
                    b[p++] = (byte) c;
                } else if (esc > 0) {
                    b[p++] = '\\';
                    b[p++] = esc;
                } else {
                    b[p++] = '\\';
                    b[p++] = 'u';
                    b[p++] = '0';
                    b[p++] = '0';
                    b[p++] = HEX[(c >> 4) & 0xF];
                    b[p++] = HEX[c & 0xF];
                }
            } else if (c < 0x800) {
                b[p++] = (byte) (0xC0 | (c >> 6));
                b[p++] = (byte) (0x80 | (c & 0x3F));
            } else if (!Character.isSurrogate(c)) {
                b[p++] = (byte) (0xE0 | (c >> 12));
                b[p++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                b[p++] = (byte) (0x80 | (c & 0x3F));
            } else {
                // Jackson's UTF8JsonGenerator escapes every surrogate char (paired or not) as \\uXXXX
                // in writeString, rather than emitting raw 4-byte UTF-8. Match it exactly.
                b[p++] = '\\';
                b[p++] = 'u';
                b[p++] = HEX[(c >> 12) & 0xF];
                b[p++] = HEX[(c >> 8) & 0xF];
                b[p++] = HEX[(c >> 4) & 0xF];
                b[p++] = HEX[c & 0xF];
            }
        }
        pos = p;
    }

    /**
     * Writes {@code val} as decimal digits. Caller must have reserved 11 bytes.
     */
    private void writeIntUnchecked(int val) {
        if (val < 0) {
            if (val == Integer.MIN_VALUE) {
                writeAsciiUnchecked("-2147483648");
                return;
            }
            buf[pos++] = '-';
            val = -val;
        }
        writeDigits(val);
    }

    /**
     * Writes {@code val} as decimal digits. Caller must have reserved 20 bytes.
     */
    private void writeLongUnchecked(long val) {
        if (val < 0) {
            if (val == Long.MIN_VALUE) {
                writeAsciiUnchecked("-9223372036854775808");
                return;
            }
            buf[pos++] = '-';
            val = -val;
        }
        // Emit in up-to-9-digit chunks through the int path to avoid 64-bit division in the loop.
        if (val <= Integer.MAX_VALUE) {
            writeDigits((int) val);
        } else {
            long upper = val / 1_000_000_000L;
            int lower = (int) (val - upper * 1_000_000_000L);
            if (upper <= Integer.MAX_VALUE) {
                writeDigits((int) upper);
            } else {
                int top = (int) (upper / 1_000_000_000L);
                int mid = (int) (upper - top * 1_000_000_000L);
                writeDigits(top);
                writeDigitsPadded(mid);
            }
            writeDigitsPadded(lower);
        }
    }

    /**
     * Writes a non-negative int without leading zeros, two digits at a time.
     */
    private void writeDigits(int val) {
        byte[] b = buf;
        // Count digits to write back-to-front without reversal.
        int digits = decimalDigits(val);
        int p = pos + digits;
        pos = p;
        while (val >= 100) {
            int q = val / 100;
            int rem = (val - q * 100) * 2;
            b[--p] = DIGIT_PAIRS[rem + 1];
            b[--p] = DIGIT_PAIRS[rem];
            val = q;
        }
        if (val >= 10) {
            int rem = val * 2;
            b[--p] = DIGIT_PAIRS[rem + 1];
            b[--p] = DIGIT_PAIRS[rem];
        } else {
            b[--p] = (byte) ('0' + val);
        }
    }

    /**
     * Writes a non-negative int as exactly 9 digits with leading zeros (continuation chunk of a long).
     */
    private void writeDigitsPadded(int val) {
        byte[] b = buf;
        int p = pos + 9;
        pos = p;
        for (int i = 0; i < 4; i++) {
            int q = val / 100;
            int rem = (val - q * 100) * 2;
            b[--p] = DIGIT_PAIRS[rem + 1];
            b[--p] = DIGIT_PAIRS[rem];
            val = q;
        }
        b[--p] = (byte) ('0' + val);
    }

    private static int decimalDigits(int val) {
        if (val < 10) {
            return 1;
        }
        if (val < 100) {
            return 2;
        }
        if (val < 1_000) {
            return 3;
        }
        if (val < 10_000) {
            return 4;
        }
        if (val < 100_000) {
            return 5;
        }
        if (val < 1_000_000) {
            return 6;
        }
        if (val < 10_000_000) {
            return 7;
        }
        if (val < 100_000_000) {
            return 8;
        }
        if (val < 1_000_000_000) {
            return 9;
        }
        return 10;
    }

    private void writeAsciiUnchecked(String s) {
        byte[] b = buf;
        int p = pos;
        for (int i = 0; i < s.length(); i++) {
            b[p++] = (byte) s.charAt(i);
        }
        pos = p;
    }

    /**
     * Base64-encodes directly into the output buffer as a quoted JSON string (standard alphabet, padded —
     * identical to Jackson's default {@code writeBinary}).
     */
    private void writeBase64(byte[] data, int off, int len) {
        int encoded = ((len + 2) / 3) * 4;
        beforeValue(encoded + 2);
        byte[] b = buf;
        int p = pos;
        b[p++] = '"';
        int end = off + len - 2;
        int i = off;
        for (; i < end; i += 3) {
            int chunk = ((data[i] & 0xFF) << 16) | ((data[i + 1] & 0xFF) << 8) | (data[i + 2] & 0xFF);
            b[p++] = BASE64[(chunk >>> 18) & 0x3F];
            b[p++] = BASE64[(chunk >>> 12) & 0x3F];
            b[p++] = BASE64[(chunk >>> 6) & 0x3F];
            b[p++] = BASE64[chunk & 0x3F];
        }
        int remaining = off + len - i;
        if (remaining == 1) {
            int chunk = (data[i] & 0xFF) << 16;
            b[p++] = BASE64[(chunk >>> 18) & 0x3F];
            b[p++] = BASE64[(chunk >>> 12) & 0x3F];
            b[p++] = '=';
            b[p++] = '=';
        } else if (remaining == 2) {
            int chunk = ((data[i] & 0xFF) << 16) | ((data[i + 1] & 0xFF) << 8);
            b[p++] = BASE64[(chunk >>> 18) & 0x3F];
            b[p++] = BASE64[(chunk >>> 12) & 0x3F];
            b[p++] = BASE64[(chunk >>> 6) & 0x3F];
            b[p++] = '=';
        }
        b[p++] = '"';
        pos = p;
    }

}
