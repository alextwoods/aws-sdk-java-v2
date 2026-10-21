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

package software.amazon.awssdk.codecs.json.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.codecs.commons.internal.ByteArrays;
import software.amazon.awssdk.codecs.commons.internal.NumberCodec;
import software.amazon.awssdk.codecs.json.JsonDocuments;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * JSON deserializer that parses directly from a byte array. Strict RFC 8259: validates every token, rejects
 * malformed input and enforces a depth limit.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.smithy.SmithyJsonDeserializer}, without the
 * {@code generated*} entry points of runtime code generation (not ported). SDK changes: type mismatches and range
 * errors name the target schema's ShapeId; a JSON number read for a {@code string} member returns the number's
 * literal text; documents are SDK {@link Document}s (see {@link #readDocument()}).
 */
@SdkInternalApi
final class SmithyJsonDeserializer implements ShapeDeserializer {

    private static final int MAX_DEPTH = 1000;

    // Short-string dedup cache. Repeated JSON keys and values (common in collections) are decoded once and shared.
    // The packed bytes of a string of at most 8 bytes form an exact identity key (every content byte on the no-escape
    // fast path is >= 0x20, so leading bytes are non-zero and the length is encoded implicitly), so a hit returns the
    // shared String with no byte comparison.
    //
    // The arrays are reused across documents through a striped pool. Reuse without clearing is safe: the packed key
    // is the exact bytes, so a stale entry from a prior document is byte-identical to any new match.
    private static final int STR_CACHE_SIZE = 256; // power of two
    private static final int STR_CACHE_MASK = STR_CACHE_SIZE - 1;

    // Mutable results of the JsonReadUtils parse calls; avoids allocating result arrays on every parse. Safe because a
    // deserializer is used by one thread.
    long parsedLong;
    int parsedEndPos;
    double parsedDouble;
    String parsedString;

    private final byte[] buf;
    private final int end;
    private final JsonSettings settings;
    private final boolean useJsonName;
    private int pos;
    private int depth;

    private long[] strCacheKeys; // null until the first short string is decoded
    private String[] strCacheVals;
    private StringCache pooledCache; // non-null when the arrays came from the pool

    SmithyJsonDeserializer(byte[] buf, int pos, int end, JsonSettings settings) {
        this.buf = buf;
        this.end = end;
        this.settings = settings;
        this.useJsonName = settings.useJsonName();
        // Position at the first non-whitespace token
        this.pos = JsonReadUtils.skipWhitespace(buf, pos, end);
    }

    /**
     * Releases the string cache and verifies that only whitespace follows the value.
     *
     * @throws SerializationException if non-whitespace content follows
     */
    @Override
    public void close() {
        releaseCache();
        int p = JsonReadUtils.skipWhitespace(buf, pos, end);
        if (p < end) {
            throw new SerializationException("Unexpected JSON content: " + JsonReadUtils.describePos(buf, p, end));
        }
    }

    /**
     * Decodes an unescaped UTF-8 string, deduplicating strings of at most 8 bytes through the per-document cache.
     */
    String decodeUtf8Cached(byte[] bytes, int start, int len) {
        return decodeCached(bytes, start, len, false);
    }

    /**
     * Decodes an unescaped ASCII string, deduplicating strings of at most 8 bytes through the per-document cache.
     */
    String decodeAsciiCached(byte[] bytes, int start, int len) {
        // ISO-8859-1 is identical for ASCII and avoids UTF-8's multi-byte scan.
        return decodeCached(bytes, start, len, true);
    }

    @Override
    public boolean readBoolean(Schema schema) {
        skipWhitespace();
        if (pos + 4 <= end && buf[pos] == 't') {
            if (buf[pos + 1] == 'r' && buf[pos + 2] == 'u' && buf[pos + 3] == 'e') {
                pos += 4;
                return true;
            }
            throw new SerializationException("Invalid token: expected 'true'");
        }
        if (pos + 5 <= end && buf[pos] == 'f') {
            if (buf[pos + 1] == 'a' && buf[pos + 2] == 'l' && buf[pos + 3] == 's' && buf[pos + 4] == 'e') {
                pos += 5;
                return false;
            }
            throw new SerializationException("Invalid token: expected 'false'");
        }
        throw typeMismatch("boolean", schema);
    }

    @Override
    public byte readByte(Schema schema) {
        long value = readIntegral(schema);
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw outOfRange("byte", value, schema);
        }
        return (byte) value;
    }

    @Override
    public short readShort(Schema schema) {
        long value = readIntegral(schema);
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw outOfRange("short", value, schema);
        }
        return (short) value;
    }

    @Override
    public int readInteger(Schema schema) {
        long value = readIntegral(schema);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw outOfRange("integer", value, schema);
        }
        return (int) value;
    }

    @Override
    public long readLong(Schema schema) {
        return readIntegral(schema);
    }

    @Override
    public float readFloat(Schema schema) {
        skipWhitespace();
        if (pos < end && buf[pos] == '"') {
            String s = readStringValue();
            switch (s) {
                case "NaN":
                    return Float.NaN;
                case "Infinity":
                    return Float.POSITIVE_INFINITY;
                case "-Infinity":
                    return Float.NEGATIVE_INFINITY;
                default:
                    throw new SerializationException("Expected float" + forSchema(schema) + ", found string: \"" + s
                                                     + "\"");
            }
        }
        expectNumber("float", schema);
        JsonReadUtils.parseDouble(buf, pos, end, this);
        pos = parsedEndPos;
        return (float) parsedDouble;
    }

    @Override
    public double readDouble(Schema schema) {
        skipWhitespace();
        if (pos < end && buf[pos] == '"') {
            String s = readStringValue();
            switch (s) {
                case "NaN":
                    return Double.NaN;
                case "Infinity":
                    return Double.POSITIVE_INFINITY;
                case "-Infinity":
                    return Double.NEGATIVE_INFINITY;
                default:
                    throw new SerializationException("Expected double" + forSchema(schema) + ", found string: \"" + s
                                                     + "\"");
            }
        }
        expectNumber("double", schema);
        JsonReadUtils.parseDouble(buf, pos, end, this);
        pos = parsedEndPos;
        return parsedDouble;
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        String numStr = readArbitraryPrecisionText("BigInteger", schema);
        try {
            return new BigInteger(numStr);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid BigInteger value" + forSchema(schema) + ": " + numStr, e);
        }
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        String numStr = readArbitraryPrecisionText("BigDecimal", schema);
        try {
            return new BigDecimal(numStr);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid BigDecimal value" + forSchema(schema) + ": " + numStr, e);
        }
    }

    /**
     * Reads a string. SDK addition: a JSON number is accepted too, and its literal text is returned unchanged (the
     * number grammar is still validated). The legacy SDK unmarshallers read numbers sent for string members this way.
     */
    @Override
    public String readString(Schema schema) {
        skipWhitespace();
        if (pos < end && isNumberStart(buf[pos])) {
            int start = pos;
            JsonReadUtils.parseDouble(buf, pos, end, this);
            pos = parsedEndPos;
            return new String(buf, start, pos - start, StandardCharsets.US_ASCII);
        }
        if (pos >= end || buf[pos] != '"') {
            throw typeMismatch("string", schema);
        }
        return readStringValue();
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        skipWhitespace();
        if (pos >= end || buf[pos] != '"') {
            throw typeMismatch("blob (base64 string)", schema);
        }
        ByteBuffer decoded = JsonReadUtils.decodeBase64String(buf, pos, end, this);
        pos = parsedEndPos;
        return decoded;
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        TimestampFormatter format = settings.timestampResolver().resolve(schema);
        skipWhitespace();
        if (format == TimestampFormatter.Prelude.EPOCH_SECONDS) {
            return readEpochTimestamp(schema);
        }
        if (pos < end && buf[pos] == '"') {
            // Fast path: parse ISO-8601 and HTTP-date directly from bytes, without a String or DateTimeFormatter.
            Instant result = null;
            if (format == TimestampFormatter.Prelude.DATE_TIME) {
                result = JsonReadUtils.parseIso8601(buf, pos, end, this);
            } else if (format == TimestampFormatter.Prelude.HTTP_DATE) {
                result = JsonReadUtils.parseHttpDate(buf, pos, end, this);
            }
            if (result != null) {
                pos = parsedEndPos;
                return result;
            }
        }
        return readTimestampFallback(format, schema);
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> structMemberConsumer) {
        // Keep the hot fields in locals between callbacks: the JIT cannot keep instance fields in registers across
        // the virtual consumer call.
        byte[] localBuf = this.buf;
        int localEnd = this.end;
        int p = JsonReadUtils.skipWhitespace(localBuf, this.pos, localEnd);
        if (p >= localEnd) {
            // An empty input is an empty structure (for example an empty response body).
            this.pos = p;
            return;
        }
        if (localBuf[p] != '{') {
            this.pos = p;
            throw typeMismatch("'{'", schema);
        }
        p++;
        enterContainer(p);
        p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);
        if (p < localEnd && localBuf[p] == '}') {
            this.pos = p + 1;
            depth--;
            return;
        }

        Schema structSchema = schema.isMember() ? schema.memberTarget() : schema;
        SmithyJsonSchemaExtensions.NativeJsonExtension ext = structSchema.getExtension(SmithyJsonSchemaExtensions.KEY);
        SmithyMemberLookup lookup = null;
        if (ext != null) {
            lookup = useJsonName ? ext.jsonNameLookup() : ext.memberNameLookup();
        }
        int expectedNext = 0;
        boolean first = true;

        while (true) {
            if (!first) {
                p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);
                if (p >= localEnd) {
                    this.pos = p;
                    throw new SerializationException("Unterminated object");
                }
                if (localBuf[p] == '}') {
                    this.pos = p + 1;
                    depth--;
                    return;
                }
                if (localBuf[p] != ',') {
                    this.pos = p;
                    throw new SerializationException("Expected ',', found: "
                                                     + JsonReadUtils.describePos(localBuf, p, localEnd));
                }
                p++;
            }
            first = false;

            p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);
            if (p >= localEnd || localBuf[p] != '"') {
                this.pos = p;
                throw new SerializationException("Expected field name, found: "
                                                 + JsonReadUtils.describePos(localBuf, p, localEnd));
            }
            p++; // skip opening quote

            // Speculative path: compare the expected field name at the current position without scanning for the
            // closing quote. It hits on every field when JSON arrives in schema definition order.
            Schema member = null;
            if (lookup != null && expectedNext >= 0 && expectedNext < lookup.orderedNameBytes.length) {
                byte[] expected = lookup.orderedNameBytes[expectedNext];
                int expLen = expected.length;
                long packedName = lookup.orderedPackedNames[expectedNext];
                boolean matches;
                if (packedName != 0 && p + Long.BYTES <= localEnd) {
                    long mask = -1L >>> ((SmithyMemberLookup.PACK_MAX_LEN - expLen) << 3);
                    matches = (JsonReadUtils.readWord(localBuf, p) & mask) == packedName;
                } else {
                    matches = p + expLen < localEnd
                              && localBuf[p + expLen] == '"'
                              && ByteArrays.rangeEquals(localBuf, p, p + expLen, expected, 0, expLen);
                }
                if (matches) {
                    member = lookup.orderedSchemas[expectedNext];
                    expectedNext = member.memberIndex() + 1;
                    p += expLen + 1; // skip name + closing quote
                }
            }

            // Slow path: scan for the closing quote; the member is looked up below.
            int nameStart = -1;
            int nameEnd = -1;
            boolean escapedName = false;
            if (member == null) {
                nameStart = p;
                while (p < localEnd && localBuf[p] != '"') {
                    if (localBuf[p] == '\\') {
                        escapedName = true;
                        p++; // skip escaped char
                    }
                    p++;
                }
                if (p >= localEnd) {
                    this.pos = p;
                    throw new SerializationException("Unterminated field name");
                }
                nameEnd = p;
                p++; // skip closing quote
            }

            // An escaped field name cannot be compared against the lookup table; decode it first.
            String decodedName = null;
            if (escapedName) {
                JsonReadUtils.parseString(localBuf, nameStart - 1, localEnd, this);
                decodedName = this.parsedString;
            }

            p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);
            if (p >= localEnd || localBuf[p] != ':') {
                this.pos = p;
                throw new SerializationException("Expected ':', found: "
                                                 + JsonReadUtils.describePos(localBuf, p, localEnd));
            }
            p++;
            p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);

            if (member == null && lookup != null) {
                if (decodedName != null) {
                    byte[] nameBytes = decodedName.getBytes(StandardCharsets.UTF_8);
                    member = lookup.lookup(nameBytes, 0, nameBytes.length, expectedNext);
                } else {
                    member = lookup.lookup(localBuf, nameStart, nameEnd, expectedNext);
                }
                if (member != null) {
                    expectedNext = member.memberIndex() + 1;
                }
            }

            if (member != null) {
                if (p + 4 <= localEnd
                    && localBuf[p] == 'n'
                    && localBuf[p + 1] == 'u'
                    && localBuf[p + 2] == 'l'
                    && localBuf[p + 3] == 'l'
                    && member.type() != ShapeType.DOCUMENT) {
                    // A null member is treated as absent, except a document member, whose null is a null document.
                    p += 4;
                } else {
                    this.pos = p;
                    structMemberConsumer.accept(state, member, this);
                    p = this.pos;
                }
            } else {
                // Unknown field: validate the field name bytes per RFC 8259 (cold path only).
                String fieldName;
                if (decodedName != null) {
                    fieldName = decodedName; // already validated and unescaped by parseString
                } else {
                    validateSkippedString(localBuf, nameStart, nameEnd);
                    fieldName = new String(localBuf, nameStart, nameEnd - nameStart, StandardCharsets.UTF_8);
                }
                this.pos = p;
                handleUnknownMember(structSchema, state, structMemberConsumer, fieldName);
                p = this.pos;
            }
        }
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> listMemberConsumer) {
        byte[] localBuf = this.buf;
        int localEnd = this.end;
        int p = JsonReadUtils.skipWhitespace(localBuf, this.pos, localEnd);
        if (p >= localEnd || localBuf[p] != '[') {
            this.pos = p;
            throw new SerializationException("Expected a list" + forSchema(schema) + ", but found "
                                             + describeCurrentToken());
        }
        p++;
        enterContainer(p);
        p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);
        if (p < localEnd && localBuf[p] == ']') {
            this.pos = p + 1;
            depth--;
            return;
        }
        this.pos = p;
        listMemberConsumer.accept(state, this);
        p = this.pos;
        while (true) {
            p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);
            if (p < localEnd && localBuf[p] == ']') {
                this.pos = p + 1;
                depth--;
                return;
            }
            if (p < localEnd && localBuf[p] == ',') {
                p++;
                p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);
                this.pos = p;
                listMemberConsumer.accept(state, this);
                p = this.pos;
            } else {
                this.pos = p;
                throw new SerializationException("Expected end of list, but found " + describeCurrentToken());
            }
        }
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> mapMemberConsumer) {
        byte[] localBuf = this.buf;
        int localEnd = this.end;
        int p = JsonReadUtils.skipWhitespace(localBuf, this.pos, localEnd);
        if (p >= localEnd || localBuf[p] != '{') {
            this.pos = p;
            throw typeMismatch("'{'", schema);
        }
        p++;
        enterContainer(p);
        p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);
        if (p < localEnd && localBuf[p] == '}') {
            this.pos = p + 1;
            depth--;
            return;
        }

        boolean first = true;
        while (true) {
            if (!first) {
                p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);
                if (p < localEnd && localBuf[p] == '}') {
                    this.pos = p + 1;
                    depth--;
                    return;
                }
                if (p >= localEnd || localBuf[p] != ',') {
                    this.pos = p;
                    throw new SerializationException("Expected ',', found: "
                                                     + JsonReadUtils.describePos(localBuf, p, localEnd));
                }
                p++;
                p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);
            }
            first = false;

            this.pos = p;
            String key = readStringValue();
            p = JsonReadUtils.skipWhitespace(localBuf, this.pos, localEnd);
            if (p >= localEnd || localBuf[p] != ':') {
                this.pos = p;
                throw new SerializationException("Expected ':', found: "
                                                 + JsonReadUtils.describePos(localBuf, p, localEnd));
            }
            p++;
            p = JsonReadUtils.skipWhitespace(localBuf, p, localEnd);
            this.pos = p;
            mapMemberConsumer.accept(state, key, this);
            p = this.pos;
        }
    }

    /**
     * Reads any JSON value as a {@link JsonDocuments} document that holds this deserializer's settings: {@code null} as
     * null, booleans, strings, integers as {@code INTEGER}, {@code LONG} or {@code BIG_INTEGER} documents by magnitude,
     * other numbers as {@code DOUBLE}, or as {@code BIG_DECIMAL} when a double does not hold the exact value, arrays as
     * list documents (null elements kept) and objects as map documents in input order. The documents read blobs and
     * timestamps, the {@code __type} discriminator and shapes with the settings (see {@link JsonDocuments}).
     */
    @Override
    public Document readDocument() {
        skipWhitespace();
        if (pos >= end) {
            throw new SerializationException("Expected a JSON value");
        }
        switch (buf[pos]) {
            case 'n':
                expectLiteral("null");
                return null;
            case 't':
                expectLiteral("true");
                return JsonDocuments.of(true, settings);
            case 'f':
                expectLiteral("false");
                return JsonDocuments.of(false, settings);
            case '"':
                return JsonDocuments.of(readStringValue(), settings);
            case '[':
                return readDocumentList();
            case '{':
                return readDocumentMap();
            default:
                if (isNumberStart(buf[pos])) {
                    return parseDocumentNumber();
                }
                throw new SerializationException("Unexpected token: " + JsonReadUtils.describePos(buf, pos, end));
        }
    }

    @Override
    public boolean isNull() {
        skipWhitespace();
        return pos < end && buf[pos] == 'n';
    }

    @Override
    public <T> T readNull() {
        skipWhitespace();
        expectLiteral("null");
        return null;
    }

    @Override
    public String toString() {
        return "SmithyJsonDeserializer(pos=" + pos + ", end=" + end + ")";
    }

    private void releaseCache() {
        StringCache cache = pooledCache;
        if (cache == null) {
            return;
        }
        pooledCache = null;
        strCacheKeys = null;
        strCacheVals = null;
        StringCache.release(cache);
    }

    /**
     * Decodes an unescaped string, deduplicating strings of at most 8 bytes. On a collision the existing entry is
     * overwritten (bounded memory); strings longer than 8 bytes bypass the cache.
     */
    private String decodeCached(byte[] bytes, int start, int len, boolean ascii) {
        if (len == 0) {
            return "";
        }
        if (len > 8) {
            return ascii
                   ? new String(bytes, start, len, StandardCharsets.ISO_8859_1)
                   : new String(bytes, start, len, StandardCharsets.UTF_8);
        }

        long key = 0;
        for (int i = 0; i < len; i++) {
            key = (key << 8) | (bytes[start + i] & 0xFFL);
        }

        long[] keys = strCacheKeys;
        String[] vals = strCacheVals;
        if (keys == null) {
            StringCache cache = StringCache.acquire();
            pooledCache = cache;
            keys = cache.keys;
            vals = cache.vals;
            strCacheKeys = keys;
            strCacheVals = vals;
        }

        // Fibonacci hash: mix so short keys that differ only in low bits spread out.
        int slot = (int) ((key * 0x9E3779B97F4A7C15L) >>> 48) & STR_CACHE_MASK;
        if (keys[slot] == key) {
            return vals[slot];
        }
        String s = ascii
                   ? new String(bytes, start, len, StandardCharsets.ISO_8859_1)
                   : new String(bytes, start, len, StandardCharsets.UTF_8);
        keys[slot] = key;
        vals[slot] = s;
        return s;
    }

    private long readIntegral(Schema schema) {
        skipWhitespace();
        expectNumber("integer number", schema);
        JsonReadUtils.parseLong(buf, pos, end, this);
        pos = parsedEndPos;
        return parsedLong;
    }

    private String readArbitraryPrecisionText(String type, Schema schema) {
        skipWhitespace();
        if (settings.useStringForArbitraryPrecision()) {
            if (pos >= end || buf[pos] != '"') {
                throw typeMismatch("string for " + type, schema);
            }
            return readStringValue();
        }
        expectNumber("number for " + type, schema);
        int start = pos;
        pos = JsonReadUtils.findNumberEnd(buf, pos, end);
        return new String(buf, start, pos - start, StandardCharsets.US_ASCII);
    }

    private String readStringValue() {
        JsonReadUtils.parseString(buf, pos, end, this);
        pos = parsedEndPos;
        return parsedString;
    }

    private Instant readEpochTimestamp(Schema schema) {
        if (pos < end && isNumberStart(buf[pos])) {
            long fastSecond = JsonReadUtils.tryParseTenDigitEpochSecond(buf, pos, end);
            if (fastSecond >= 0) {
                pos += 10;
                return instantFromEpochSecond(fastSecond);
            }
            int startPos = pos;
            JsonReadUtils.parseLong(buf, startPos, end, this);
            int endPos = parsedEndPos;
            if (endPos >= end || (buf[endPos] != '.' && buf[endPos] != 'e' && buf[endPos] != 'E')) {
                pos = endPos;
                return instantFromEpochSecond(parsedLong);
            }
            return readFractionalEpochTimestamp(startPos, endPos);
        }
        return readTimestampFallback(TimestampFormatter.Prelude.EPOCH_SECONDS, schema);
    }

    private Instant readFractionalEpochTimestamp(int startPos, int integerEnd) {
        if (buf[integerEnd] == '.') {
            // Avoid losing nanosecond precision through double.
            int fracPos = integerEnd + 1;
            int fracStart = fracPos;
            while (fracPos < end && buf[fracPos] >= '0' && buf[fracPos] <= '9') {
                fracPos++;
            }
            int fracLen = fracPos - fracStart;
            boolean hasExponent = fracPos < end && (buf[fracPos] == 'e' || buf[fracPos] == 'E');
            if (fracLen > 0 && !hasExponent) {
                int nano = 0;
                for (int i = 0; i < 9; i++) {
                    nano *= 10;
                    if (i < fracLen) {
                        nano += buf[fracStart + i] - '0';
                    }
                }
                pos = fracPos;
                long epochSecond = parsedLong;
                if (buf[startPos] == '-' && nano > 0) {
                    epochSecond -= 1;
                    nano = 1_000_000_000 - nano;
                }
                try {
                    return Instant.ofEpochSecond(epochSecond, nano);
                } catch (DateTimeException e) {
                    throw new SerializationException("Epoch seconds out of range: " + parsedLong, e);
                }
            }
        }
        pos = startPos;
        JsonReadUtils.parseDouble(buf, startPos, end, this);
        pos = parsedEndPos;
        return TimestampFormatter.Prelude.EPOCH_SECONDS.readFromNumber(parsedDouble);
    }

    private Instant instantFromEpochSecond(long epochSecond) {
        try {
            return Instant.ofEpochSecond(epochSecond);
        } catch (DateTimeException e) {
            throw new SerializationException("Epoch seconds out of range: " + epochSecond, e);
        }
    }

    private Instant readTimestampFallback(TimestampFormatter format, Schema schema) {
        if (pos < end && buf[pos] == '"') {
            String s = readStringValue();
            try {
                return format.readFromString(s, true);
            } catch (DateTimeException e) {
                throw new SerializationException("Invalid timestamp" + forSchema(schema) + ": " + s, e);
            }
        }
        if (pos < end && isNumberStart(buf[pos])) {
            JsonReadUtils.parseDouble(buf, pos, end, this);
            pos = parsedEndPos;
            return format.readFromNumber(parsedDouble);
        }
        throw new SerializationException("Expected a timestamp" + forSchema(schema) + ", but found "
                                         + describeCurrentToken());
    }

    private <T> void handleUnknownMember(Schema structSchema, T state, StructMemberConsumer<T> structMemberConsumer,
                                         String fieldName) {
        if (structSchema.type() == ShapeType.STRUCTURE) {
            structMemberConsumer.unknownMember(state, fieldName);
            skipValue();
        } else if ("__type".equals(fieldName)) {
            skipValue();
        } else if (settings.forbidUnknownUnionMembers()) {
            throw new SerializationException("Unknown member " + fieldName + " encountered for union "
                                             + structSchema.id());
        } else {
            structMemberConsumer.unknownMember(state, fieldName);
            skipValue();
        }
    }

    private Document readDocumentList() {
        pos++; // skip '['
        enterContainer(pos);
        List<Document> values = new ArrayList<>();
        skipWhitespace();
        if (pos < end && buf[pos] != ']') {
            values.add(readDocument());
            skipWhitespace();
            while (pos < end && buf[pos] == ',') {
                pos++;
                values.add(readDocument());
                skipWhitespace();
            }
        }
        expect(']');
        depth--;
        return JsonDocuments.of(values, settings);
    }

    private Document readDocumentMap() {
        pos++; // skip '{'
        enterContainer(pos);
        Map<String, Document> values = new LinkedHashMap<>();
        skipWhitespace();
        if (pos < end && buf[pos] != '}') {
            readDocumentEntry(values);
            while (pos < end && buf[pos] == ',') {
                pos++;
                skipWhitespace();
                readDocumentEntry(values);
            }
        }
        expect('}');
        depth--;
        return JsonDocuments.of(values, settings);
    }

    private void readDocumentEntry(Map<String, Document> values) {
        String key = readStringValue();
        skipWhitespace();
        expect(':');
        values.put(key, readDocument());
        skipWhitespace();
    }

    /**
     * Parses a number in document context with strict RFC 8259 validation, choosing an integer, long, big integer or
     * double document.
     */
    private Document parseDocumentNumber() {
        // parseDouble strictly validates the number grammar
        JsonReadUtils.parseDouble(buf, pos, end, this);
        int newPos = parsedEndPos;

        boolean isFloat = false;
        for (int i = pos; i < newPos; i++) {
            if (buf[i] == '.' || buf[i] == 'e' || buf[i] == 'E') {
                isFloat = true;
                break;
            }
        }

        Document number;
        if (isFloat) {
            number = floatingPointDocument(newPos);
        } else {
            int len = newPos - pos;
            int digitStart = buf[pos] == '-' ? pos + 1 : pos;
            int digitLen = newPos - digitStart;
            if (digitLen <= 18) {
                number = integralDocument(NumberCodec.parseLong(buf, pos, len));
            } else {
                String numStr = new String(buf, pos, len, StandardCharsets.US_ASCII);
                BigInteger value = new BigInteger(numStr);
                number = value.bitLength() < 64 ? integralDocument(value.longValue())
                                                : JsonDocuments.of(value, settings);
            }
        }
        pos = newPos;
        return number;
    }

    /**
     * A number with a fraction or exponent, already parsed into {@code parsedDouble}: a double document when the double
     * holds the number's value, else a big decimal document of the exact value (Requirement 18.5), so that a document
     * does not lose digits, overflow to infinity or underflow to zero. A number with at most 15 significant digits in the
     * normal double range always round-trips through a double, so only longer or extreme numbers build a
     * {@link BigDecimal}.
     */
    private Document floatingPointDocument(int numberEnd) {
        double value = parsedDouble;
        if (Double.isFinite(value) && Math.abs(value) >= Double.MIN_NORMAL
            && significantDigits(buf, pos, numberEnd) <= 15) {
            return JsonDocuments.of(value, settings);
        }
        BigDecimal exact;
        try {
            exact = new BigDecimal(new String(buf, pos, numberEnd - pos, StandardCharsets.US_ASCII));
        } catch (NumberFormatException e) {
            // The exponent does not fit a BigDecimal scale: keep the double (infinity or zero), as smithy-java does.
            return JsonDocuments.of(value, settings);
        }
        if (Double.isFinite(value) && BigDecimal.valueOf(value).compareTo(exact) == 0) {
            return JsonDocuments.of(value, settings);
        }
        return JsonDocuments.of(exact, settings);
    }

    /**
     * Counts the digits of a JSON number's integer and fraction parts, without leading zeros.
     */
    private static int significantDigits(byte[] bytes, int start, int end) {
        int digits = 0;
        for (int i = start; i < end; i++) {
            byte b = bytes[i];
            if (b == 'e' || b == 'E') {
                break;
            }
            if (b >= '1' && b <= '9' || b == '0' && digits > 0) {
                digits++;
            }
        }
        return digits;
    }

    private Document integralDocument(long value) {
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return JsonDocuments.of((int) value, settings);
        }
        return JsonDocuments.of(value, settings);
    }

    /**
     * Validates string bytes (between quotes) for RFC 8259 compliance without building a String: no unescaped control
     * characters and only valid escape sequences.
     */
    private static void validateSkippedString(byte[] bytes, int start, int stop) {
        int p = start;
        while (p < stop) {
            byte b = bytes[p];
            if (b == '\\') {
                p++;
                if (p >= stop) {
                    throw new SerializationException("Unterminated escape sequence");
                }
                p = validateEscape(bytes, p, stop);
            } else if ((b & 0xFF) < 0x20) {
                throw new SerializationException("Unescaped control character (0x" + Integer.toHexString(b & 0xFF)
                                                 + ")");
            }
            p++;
        }
    }

    /**
     * Validates the escape character at {@code p} (after the backslash) and returns the position of its last byte.
     */
    private static int validateEscape(byte[] bytes, int p, int stop) {
        byte esc = bytes[p];
        switch (esc) {
            case '"':
            case '\\':
            case '/':
            case 'b':
            case 'f':
            case 'n':
            case 'r':
            case 't':
                return p;
            case 'u':
                if (p + 4 >= stop) {
                    throw new SerializationException("Unterminated \\u escape");
                }
                return p + 4;
            default:
                throw new SerializationException("Invalid escape sequence: \\" + (char) esc);
        }
    }

    private void skipValue() {
        skipWhitespace();
        if (pos >= end) {
            throw new SerializationException("Unexpected end of input");
        }
        switch (buf[pos]) {
            case '"':
                skipString();
                break;
            case '{':
                skipObject();
                break;
            case '[':
                skipArray();
                break;
            case 't':
                expectLiteral("true");
                break;
            case 'f':
                expectLiteral("false");
                break;
            case 'n':
                expectLiteral("null");
                break;
            default:
                if (isNumberStart(buf[pos])) {
                    // parseDouble for strict RFC 8259 number validation (leading zeros, double exponents, ...)
                    JsonReadUtils.parseDouble(buf, pos, end, this);
                    pos = parsedEndPos;
                } else {
                    throw new SerializationException("Unexpected token: " + JsonReadUtils.describePos(buf, pos, end));
                }
                break;
        }
    }

    private void skipString() {
        if (pos >= end || buf[pos] != '"') {
            throw new SerializationException("Expected '\"', found: " + JsonReadUtils.describePos(buf, pos, end));
        }
        int p = pos + 1; // skip opening '"'
        while (p < end) {
            byte b = buf[p];
            if (b == '"') {
                pos = p + 1;
                return;
            }
            if (b == '\\') {
                p++;
                if (p >= end) {
                    break;
                }
                p = validateEscape(buf, p, end);
            } else if ((b & 0xFF) < 0x20) {
                throw new SerializationException("Unescaped control character (0x" + Integer.toHexString(b & 0xFF)
                                                 + ")");
            }
            p++;
        }
        throw new SerializationException("Unterminated string");
    }

    private void skipObject() {
        pos++; // skip '{'
        enterContainer(pos);
        skipWhitespace();
        if (pos < end && buf[pos] == '}') {
            pos++;
            depth--;
            return;
        }
        boolean first = true;
        while (true) {
            if (!first) {
                skipWhitespace();
                if (pos >= end) {
                    throw new SerializationException("Unexpected end of input in object");
                }
                if (buf[pos] == '}') {
                    pos++;
                    depth--;
                    return;
                }
                expect(',');
                skipWhitespace();
            }
            first = false;
            skipString(); // skip key
            skipWhitespace();
            expect(':');
            skipValue();
        }
    }

    private void skipArray() {
        pos++; // skip '['
        enterContainer(pos);
        skipWhitespace();
        if (pos < end && buf[pos] == ']') {
            pos++;
            depth--;
            return;
        }
        boolean first = true;
        while (true) {
            if (!first) {
                skipWhitespace();
                if (pos >= end) {
                    throw new SerializationException("Unexpected end of input in array");
                }
                if (buf[pos] == ']') {
                    pos++;
                    depth--;
                    return;
                }
                expect(',');
                skipWhitespace();
            }
            first = false;
            skipValue();
        }
    }

    private void enterContainer(int newPos) {
        depth++;
        if (depth > MAX_DEPTH) {
            this.pos = newPos;
            throw new SerializationException("Maximum nesting depth exceeded: " + MAX_DEPTH);
        }
    }

    private void skipWhitespace() {
        pos = JsonReadUtils.skipWhitespace(buf, pos, end);
    }

    private void expect(char c) {
        if (pos >= end || buf[pos] != c) {
            throw new SerializationException("Expected '" + c + "', found: " + JsonReadUtils.describePos(buf, pos, end));
        }
        pos++;
    }

    private void expectLiteral(String literal) {
        int len = literal.length();
        if (pos + len > end) {
            throw new SerializationException("Unexpected end of input, expected '" + literal + "'");
        }
        for (int i = 0; i < len; i++) {
            if (buf[pos + i] != literal.charAt(i)) {
                throw new SerializationException("Expected '" + literal + "', found: "
                                                 + new String(buf, pos, Math.min(len, end - pos),
                                                              StandardCharsets.US_ASCII));
            }
        }
        pos += len;
    }

    private void expectNumber(String type, Schema schema) {
        if (pos >= end || !isNumberStart(buf[pos])) {
            throw typeMismatch(type, schema);
        }
    }

    private static boolean isNumberStart(byte b) {
        return b == '-' || (b >= '0' && b <= '9');
    }

    private SerializationException typeMismatch(String expected, Schema schema) {
        return new SerializationException("Expected " + expected + forSchema(schema) + ", found: "
                                          + JsonReadUtils.describePos(buf, pos, end));
    }

    private static SerializationException outOfRange(String type, long value, Schema schema) {
        return new SerializationException("Value out of " + type + " range" + forSchema(schema) + ": " + value);
    }

    private static String forSchema(Schema schema) {
        return schema == null ? "" : " for " + schema.id();
    }

    private String describeCurrentToken() {
        if (pos >= end) {
            return "end of input";
        }
        switch (buf[pos]) {
            case 't':
            case 'f':
                return "Boolean value";
            case 'n':
                return "Null value";
            case '[':
                return "Array value";
            case '{':
                return "Object value";
            case '"':
                return "String value";
            default:
                return JsonReadUtils.describePos(buf, pos, end);
        }
    }

    /**
     * Reusable dedup arrays, kept in a striped pool shared by all threads (allocating about 4 KB per parse measurably
     * slows small payloads). The pool lives in this nested class so that only it holds shared state.
     */
    static final class StringCache {
        private static final int POOL_SLOTS = Integer.highestOneBit(Runtime.getRuntime().availableProcessors() * 4 - 1)
                                              << 1;
        private static final int POOL_MASK = POOL_SLOTS - 1;
        private static final AtomicReferenceArray<StringCache> POOL = new AtomicReferenceArray<>(POOL_SLOTS);
        private static final int MAX_PROBE = 3;

        final long[] keys = new long[STR_CACHE_SIZE];
        final String[] vals = new String[STR_CACHE_SIZE];

        /**
         * Takes a cache from the pool, or allocates one. The compare-and-set to null gives the caller exclusive
         * ownership until {@link #release}.
         */
        static StringCache acquire() {
            int base = probe();
            for (int i = 0; i < MAX_PROBE; i++) {
                int idx = (base + i) & POOL_MASK;
                StringCache c = POOL.get(idx);
                if (c != null && POOL.compareAndSet(idx, c, null)) {
                    return c;
                }
            }
            return new StringCache();
        }

        /**
         * Returns a cache to the pool for a later parse; drops it when the probed slots are full.
         */
        static void release(StringCache cache) {
            int base = probe();
            for (int i = 0; i < MAX_PROBE; i++) {
                int idx = (base + i) & POOL_MASK;
                if (POOL.get(idx) == null && POOL.compareAndSet(idx, null, cache)) {
                    return;
                }
            }
        }

        private static int probe() {
            long id = Thread.currentThread().getId();
            return (int) (id ^ (id >>> 16)) & POOL_MASK;
        }
    }
}
