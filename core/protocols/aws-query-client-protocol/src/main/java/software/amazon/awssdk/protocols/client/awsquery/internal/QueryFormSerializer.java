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

package software.amazon.awssdk.protocols.client.awsquery.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.ByteBufferUtils;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.protocols.client.awsquery.internal.AwsQuerySchemaExtensions.QueryMemberBinding;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;
import software.amazon.awssdk.utils.Validate;

/**
 * Writes the {@code application/x-www-form-urlencoded} request body of the {@code awsQuery} and {@code ec2Query}
 * protocols: {@code Action} and {@code Version} first, then one {@code key=value} pair per serialized value.
 *
 * <p>Create one with {@link #forAwsQuery} or {@link #forEc2Query}, pass it to the input's
 * {@link SerializableStruct#serializeMembers}, then call {@link #finish()} or {@link #finishArray()} once. An instance
 * is single-use and not thread-safe.
 *
 * <p>Keys follow the protocol rules:
 * <ul>
 *     <li>A member key is the member's {@code xmlName} or member name ({@code awsQuery}), or its {@code ec2QueryName},
 *     else its capitalized {@code xmlName} or member name ({@code ec2Query}). Nested structure members are joined with
 *     {@code .}.</li>
 *     <li>{@code awsQuery} lists write {@code Name.member.N} (the list member's {@code xmlName} replaces
 *     {@code member}), or {@code Name.N} with {@code xmlFlattened}; an empty list writes {@code Name=}.
 *     {@code ec2Query} lists always write {@code Name.N} and an empty list writes nothing.</li>
 *     <li>Maps write {@code Name.entry.N.key} and {@code Name.entry.N.value}, or {@code Name.N.key} with
 *     {@code xmlFlattened}; the key and value members' {@code xmlName} replace {@code key} and {@code value}.</li>
 * </ul>
 *
 * <p>Values: strings as is, booleans as {@code true}/{@code false}, integers in decimal, {@code float} and
 * {@code double} as {@link Float#toString(float)}/{@link Double#toString(double)} (so {@code NaN}, {@code Infinity},
 * {@code -Infinity}), {@code bigDecimal} in plain notation, blobs in padded base64, timestamps in their
 * {@code timestampFormat} with a {@code date-time} default. Keys and values are percent-encoded as UTF-8 with the
 * RFC 3986 unreserved characters ({@code A-Z a-z 0-9 - . _ ~}) left as is, so a space is {@code %20}. Documents are
 * not supported.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.client.awsquery.QueryFormSerializer}.
 */
@SdkInternalApi
public final class QueryFormSerializer implements ShapeSerializer {
    private static final byte[] ACTION_PREFIX = {'A', 'c', 't', 'i', 'o', 'n', '='};
    private static final byte[] VERSION_PREFIX = {'&', 'V', 'e', 'r', 's', 'i', 'o', 'n', '='};
    private static final byte[] TRUE = {'t', 'r', 'u', 'e'};
    private static final byte[] FALSE = {'f', 'a', 'l', 's', 'e'};
    private static final byte[] MEMBER = {'m', 'e', 'm', 'b', 'e', 'r'};
    private static final byte[] ENTRY = {'e', 'n', 't', 'r', 'y'};
    private static final byte[] KEY = {'k', 'e', 'y'};
    private static final byte[] VALUE = {'v', 'a', 'l', 'u', 'e'};

    private static final boolean[] UNRESERVED = new boolean[128];
    private static final byte[] PERCENT_ENCODED = new byte[256 * 3];

    static {
        for (int c = 'A'; c <= 'Z'; c++) {
            UNRESERVED[c] = true;
        }
        for (int c = 'a'; c <= 'z'; c++) {
            UNRESERVED[c] = true;
        }
        for (int c = '0'; c <= '9'; c++) {
            UNRESERVED[c] = true;
        }
        UNRESERVED['-'] = true;
        UNRESERVED['.'] = true;
        UNRESERVED['_'] = true;
        UNRESERVED['~'] = true;
        byte[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        for (int b = 0; b < 256; b++) {
            int off = b * 3;
            PERCENT_ENCODED[off] = '%';
            PERCENT_ENCODED[off + 1] = hex[(b >> 4) & 0xF];
            PERCENT_ENCODED[off + 2] = hex[b & 0xF];
        }
    }

    private static final int DEFAULT_BUF_SIZE = 1024;
    // A char is at most three UTF-8 bytes (a surrogate pair is four bytes for two chars), each written as %XX.
    private static final int MAX_ENCODED_BYTES_PER_CHAR = 9;
    private static final int MAX_INT_LENGTH = 11;
    private static final int MAX_LONG_LENGTH = 20;
    private static final int MAX_BOOLEAN_LENGTH = 5;

    private final boolean ec2;
    private final MemberSerializer members = new MemberSerializer();
    private final ListItemSerializer listItems = new ListItemSerializer();
    private final MapEntrySerializer mapEntries = new MapEntrySerializer();
    private final MapValueSerializer mapValue = new MapValueSerializer();

    private byte[] buf;
    private int pos;
    // The largest buffer size a write has reserved: each value reserves its worst-case encoded length.
    private int requiredCapacity;
    private byte[] prefixBuf = new byte[128];
    private int prefixLen;
    private int[] prefixStack = new int[8];
    private int prefixDepth;

    private QueryFormSerializer(boolean ec2, String action, String version, int initialCapacity) {
        this.ec2 = ec2;
        Validate.paramNotNull(action, "action");
        Validate.paramNotNull(version, "version");
        int prelude = ACTION_PREFIX.length + VERSION_PREFIX.length + (action.length() + version.length()) * 3;
        // At least the prelude's reservation, so writing Action and Version never grows the array.
        this.buf = new byte[Math.max(initialCapacity, prelude)];
        ensureCapacity(prelude);
        writeBytes(ACTION_PREFIX);
        writeUrlEncoded(action);
        writeBytes(VERSION_PREFIX);
        writeUrlEncoded(version);
    }

    /**
     * Creates a serializer for an {@code awsQuery} request body that starts with
     * {@code Action=<action>&Version=<version>}.
     *
     * @param action the operation name
     * @param version the service's API version
     * @return a new serializer
     */
    public static QueryFormSerializer forAwsQuery(String action, String version) {
        return forAwsQuery(action, version, DEFAULT_BUF_SIZE);
    }

    /**
     * Creates a serializer for an {@code awsQuery} request body that starts with
     * {@code Action=<action>&Version=<version>}, in an array of {@code initialCapacity} bytes (more if the prelude
     * needs it) that grows as needed. SDK addition.
     *
     * @param action the operation name
     * @param version the service's API version
     * @param initialCapacity the starting size of the body array, for example the size of the operation's last body
     * @return a new serializer
     */
    public static QueryFormSerializer forAwsQuery(String action, String version, int initialCapacity) {
        return new QueryFormSerializer(false, action, version, initialCapacity);
    }

    /**
     * Creates a serializer for an {@code ec2Query} request body that starts with
     * {@code Action=<action>&Version=<version>}.
     *
     * @param action the operation name
     * @param version the service's API version
     * @return a new serializer
     */
    public static QueryFormSerializer forEc2Query(String action, String version) {
        return forEc2Query(action, version, DEFAULT_BUF_SIZE);
    }

    /**
     * Creates a serializer for an {@code ec2Query} request body that starts with
     * {@code Action=<action>&Version=<version>}, in an array of {@code initialCapacity} bytes (more if the prelude
     * needs it) that grows as needed. SDK addition.
     *
     * @param action the operation name
     * @param version the service's API version
     * @param initialCapacity the starting size of the body array, for example the size of the operation's last body
     * @return a new serializer
     */
    public static QueryFormSerializer forEc2Query(String action, String version, int initialCapacity) {
        return new QueryFormSerializer(true, action, version, initialCapacity);
    }

    /**
     * Returns the body written so far. The buffer is array-backed and starts at position 0; the serializer must not be
     * used afterwards.
     *
     * @return the form-encoded body
     * @throws IllegalStateException if called twice
     */
    public ByteBuffer finish() {
        byte[] result = checkNotFinished();
        buf = null;
        return ByteBuffer.wrap(result, 0, pos).slice();
    }

    /**
     * Returns the number of body bytes written so far.
     *
     * @return the body length
     */
    public int size() {
        return pos;
    }

    /**
     * Returns the smallest initial capacity with which the writes so far would not have grown the array. Each value
     * reserves its worst-case encoded length before it is written, so this can be larger than {@link #size()}. SDK
     * addition, for sizing the next body of the same operation.
     *
     * @return the capacity the body needed
     */
    public int requiredCapacity() {
        return Math.max(requiredCapacity, pos);
    }

    /**
     * Returns the internal array holding the body, without the buffer views {@link #finish()} creates. The body is
     * bytes {@code [0, size())}; the array may be longer. The serializer must not be used afterwards. SDK addition.
     *
     * @return the array holding the form-encoded body
     * @throws IllegalStateException if the body was already finished
     */
    public byte[] finishArray() {
        byte[] result = checkNotFinished();
        buf = null;
        return result;
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        members.writeStruct(schema, struct);
    }

    @Override
    public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
        members.writeList(schema, listState, size, consumer);
    }

    @Override
    public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
        members.writeMap(schema, mapState, size, consumer);
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        members.writeBoolean(schema, value);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        members.writeByte(schema, value);
    }

    @Override
    public void writeShort(Schema schema, short value) {
        members.writeShort(schema, value);
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        members.writeInteger(schema, value);
    }

    @Override
    public void writeLong(Schema schema, long value) {
        members.writeLong(schema, value);
    }

    @Override
    public void writeFloat(Schema schema, float value) {
        members.writeFloat(schema, value);
    }

    @Override
    public void writeDouble(Schema schema, double value) {
        members.writeDouble(schema, value);
    }

    @Override
    public void writeBigInteger(Schema schema, BigInteger value) {
        members.writeBigInteger(schema, value);
    }

    @Override
    public void writeBigDecimal(Schema schema, BigDecimal value) {
        members.writeBigDecimal(schema, value);
    }

    @Override
    public void writeString(Schema schema, String value) {
        members.writeString(schema, value);
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        members.writeBlob(schema, value);
    }

    @Override
    public void writeBlob(Schema schema, byte[] value) {
        members.writeBlob(schema, value);
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        members.writeTimestamp(schema, value);
    }

    @Override
    public void writeDocument(Schema schema, Document value) {
        members.writeDocument(schema, value);
    }

    @Override
    public void writeNull(Schema schema) {
        members.writeNull(schema);
    }

    /**
     * Percent-encodes a member name once, for {@link AwsQuerySchemaExtensions}.
     */
    static byte[] encodeName(String name) {
        int len = name.length();
        boolean allUnreserved = true;
        for (int i = 0; i < len && allUnreserved; i++) {
            char c = name.charAt(i);
            allUnreserved = c < 0x80 && UNRESERVED[c];
        }
        byte[] result = new byte[allUnreserved ? len : len * MAX_ENCODED_BYTES_PER_CHAR];
        int end = writeUrlEncoded(result, 0, name, 0);
        return end == result.length ? result : Arrays.copyOf(result, end);
    }

    private byte[] checkNotFinished() {
        if (buf == null) {
            throw new IllegalStateException("The query form body has already been finished");
        }
        return buf;
    }

    private void ensureCapacity(int needed) {
        byte[] current = checkNotFinished();
        int required = pos + needed;
        if (required > requiredCapacity) {
            requiredCapacity = required;
        }
        if (required > current.length) {
            buf = Arrays.copyOf(current, Math.max(required, current.length + (current.length >> 1)));
        }
    }

    private void writeBytes(byte[] bytes) {
        System.arraycopy(bytes, 0, buf, pos, bytes.length);
        pos += bytes.length;
    }

    // The caller reserves one byte per char; every char of these strings (digits, signs, '.', 'E', NaN, Infinity) is
    // unreserved.
    private void writeAscii(String value) {
        for (int i = 0; i < value.length(); i++) {
            buf[pos++] = (byte) value.charAt(i);
        }
    }

    // The caller reserves three bytes per char; non-ASCII text reserves more as it is reached.
    private void writeUrlEncoded(String value) {
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c >= 0x80) {
                ensureCapacity((len - i) * MAX_ENCODED_BYTES_PER_CHAR);
                pos = writeUrlEncoded(buf, pos, value, i);
                return;
            }
            if (UNRESERVED[c]) {
                buf[pos++] = (byte) c;
            } else {
                writePercentEncoded(c);
            }
        }
    }

    // The caller reserves three bytes per input byte.
    private void writeUrlEncodedAscii(byte[] data) {
        for (byte b : data) {
            if (UNRESERVED[b]) {
                buf[pos++] = b;
            } else {
                writePercentEncoded(b);
            }
        }
    }

    private void writePercentEncoded(int b) {
        System.arraycopy(PERCENT_ENCODED, b * 3, buf, pos, 3);
        pos += 3;
    }

    /**
     * Writes {@code value} from {@code start} as percent-encoded UTF-8. The caller reserves nine bytes per char. An
     * unpaired surrogate is written as {@code ?} ({@code %3F}), as {@code String.getBytes(UTF_8)} does.
     */
    private static int writeUrlEncoded(byte[] out, int pos, String value, int start) {
        int len = value.length();
        for (int i = start; i < len; i++) {
            int c = value.charAt(i);
            if (c < 0x80) {
                if (UNRESERVED[c]) {
                    out[pos++] = (byte) c;
                } else {
                    pos = percentEncode(out, pos, c);
                }
            } else if (c < 0x800) {
                pos = percentEncode(out, pos, 0xC0 | (c >> 6));
                pos = percentEncode(out, pos, 0x80 | (c & 0x3F));
            } else if (Character.isHighSurrogate((char) c) && i + 1 < len
                       && Character.isLowSurrogate(value.charAt(i + 1))) {
                int cp = Character.toCodePoint((char) c, value.charAt(++i));
                pos = percentEncode(out, pos, 0xF0 | (cp >> 18));
                pos = percentEncode(out, pos, 0x80 | ((cp >> 12) & 0x3F));
                pos = percentEncode(out, pos, 0x80 | ((cp >> 6) & 0x3F));
                pos = percentEncode(out, pos, 0x80 | (cp & 0x3F));
            } else if (Character.isSurrogate((char) c)) {
                pos = percentEncode(out, pos, '?');
            } else {
                pos = percentEncode(out, pos, 0xE0 | (c >> 12));
                pos = percentEncode(out, pos, 0x80 | ((c >> 6) & 0x3F));
                pos = percentEncode(out, pos, 0x80 | (c & 0x3F));
            }
        }
        return pos;
    }

    private static int percentEncode(byte[] out, int pos, int b) {
        System.arraycopy(PERCENT_ENCODED, b * 3, out, pos, 3);
        return pos + 3;
    }

    private void pushPrefix(byte[] name, int index) {
        if (prefixDepth == prefixStack.length) {
            prefixStack = Arrays.copyOf(prefixStack, prefixStack.length * 2);
        }
        prefixStack[prefixDepth++] = prefixLen;
        String indexText = index > 0 ? Integer.toString(index) : null;
        int needed = 1 + (name == null ? 0 : name.length) + (indexText == null ? 0 : indexText.length() + 1);
        if (prefixLen + needed > prefixBuf.length) {
            prefixBuf = Arrays.copyOf(prefixBuf, Math.max(prefixLen + needed, prefixBuf.length * 2));
        }
        if (name != null) {
            appendPrefixSeparator();
            System.arraycopy(name, 0, prefixBuf, prefixLen, name.length);
            prefixLen += name.length;
        }
        if (indexText != null) {
            appendPrefixSeparator();
            for (int i = 0; i < indexText.length(); i++) {
                prefixBuf[prefixLen++] = (byte) indexText.charAt(i);
            }
        }
    }

    private void appendPrefixSeparator() {
        if (prefixLen > 0) {
            prefixBuf[prefixLen++] = '.';
        }
    }

    private void popPrefix() {
        prefixLen = prefixStack[--prefixDepth];
    }

    /**
     * Writes {@code &prefix=}, or with {@code name} and {@code index} (each optional) {@code &prefix.name.index=}, and
     * reserves {@code maxValueBytes} more.
     */
    private void writeKey(byte[] name, int index, int maxValueBytes) {
        String indexText = index > 0 ? Integer.toString(index) : null;
        int nameLen = name == null ? 0 : name.length + 1;
        int indexLen = indexText == null ? 0 : indexText.length() + 1;
        ensureCapacity(1 + prefixLen + nameLen + indexLen + 1 + maxValueBytes);
        buf[pos++] = '&';
        System.arraycopy(prefixBuf, 0, buf, pos, prefixLen);
        pos += prefixLen;
        boolean separate = prefixLen > 0;
        if (name != null) {
            if (separate) {
                buf[pos++] = '.';
            }
            writeBytes(name);
            separate = true;
        }
        if (indexText != null) {
            if (separate) {
                buf[pos++] = '.';
            }
            writeAscii(indexText);
        }
        buf[pos++] = '=';
    }

    private static QueryMemberBinding bindingOrNull(Schema schema) {
        return schema.isMember() ? AwsQuerySchemaExtensions.binding(schema) : null;
    }

    private static byte[] listMemberName(Schema list) {
        QueryMemberBinding binding = bindingOrNull(list);
        if (binding != null) {
            return binding.flattened() ? null : binding.listMemberName();
        }
        XmlNameTrait xmlName = list.listMember().getDirectTrait(TraitKey.XML_NAME_TRAIT);
        return xmlName == null ? MEMBER : encodeName(xmlName.getValue());
    }

    private <T> void writeListItems(Schema list, int size, T listState, BiConsumer<T, ShapeSerializer> consumer) {
        if (size == 0) {
            // awsQuery sends an explicitly empty list as "Name="; ec2Query omits it.
            if (!ec2) {
                writeKey(null, 0, 0);
            }
            return;
        }
        byte[] memberName = ec2 ? null : listMemberName(list);
        // A nested list re-enters and resets the shared item serializer, so the outer state is saved and restored.
        byte[] savedMemberName = listItems.memberName;
        int savedIndex = listItems.index;
        listItems.memberName = memberName;
        listItems.index = 1;
        consumer.accept(listState, listItems);
        listItems.memberName = savedMemberName;
        listItems.index = savedIndex;
    }

    private <T> void writeMapEntries(Schema map, T mapState, BiConsumer<T, MapSerializer> consumer) {
        QueryMemberBinding binding = bindingOrNull(map);
        byte[] entryName = binding != null ? binding.mapEntryName() : ENTRY;
        byte[] keyName = binding != null ? binding.mapKeyName() : KEY;
        byte[] valueName = binding != null ? binding.mapValueName() : VALUE;
        // A nested map re-enters and resets the shared entry serializer, so the outer state is saved and restored.
        byte[] savedEntryName = mapEntries.entryName;
        byte[] savedKeyName = mapEntries.keyName;
        byte[] savedValueName = mapEntries.valueName;
        int savedIndex = mapEntries.index;
        mapEntries.entryName = entryName;
        mapEntries.keyName = keyName;
        mapEntries.valueName = valueName;
        mapEntries.index = 1;
        consumer.accept(mapState, mapEntries);
        mapEntries.entryName = savedEntryName;
        mapEntries.keyName = savedKeyName;
        mapEntries.valueName = savedValueName;
        mapEntries.index = savedIndex;
    }

    private static TimestampFormatter timestampFormatter(Schema schema) {
        QueryMemberBinding binding = bindingOrNull(schema);
        TimestampFormatter formatter = binding != null ? binding.timestampFormatter() : null;
        // Null for a non-member schema or an unknown format; of() resolves the first and rejects the second.
        return formatter != null ? formatter : TimestampFormatter.of(schema, TimestampFormatTrait.Format.DATE_TIME);
    }

    /**
     * Writes one value per call, under a key that depends on where the value is: a structure member, a list element or
     * a map value.
     */
    private abstract class ValueSerializer implements ShapeSerializer {
        /**
         * Writes the key of {@code schema}'s value and the {@code =}, reserving {@code maxValueBytes} after it.
         */
        abstract void writeKey(Schema schema, int maxValueBytes);

        /**
         * Pushes the key prefix of a structure, list or map value, returning whether a prefix was pushed.
         */
        abstract boolean enter(Schema schema);

        /**
         * Called after each value, including a null one.
         */
        void afterValue() {
        }

        private void exit(boolean pushed) {
            if (pushed) {
                popPrefix();
            }
            afterValue();
        }

        @Override
        public final void writeStruct(Schema schema, SerializableStruct struct) {
            boolean pushed = enter(schema);
            struct.serializeMembers(members);
            exit(pushed);
        }

        @Override
        public final <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            boolean pushed = enter(schema);
            writeListItems(schema, size, listState, consumer);
            exit(pushed);
        }

        @Override
        public final <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            boolean pushed = enter(schema);
            writeMapEntries(schema, mapState, consumer);
            exit(pushed);
        }

        @Override
        public final void writeBoolean(Schema schema, boolean value) {
            writeKey(schema, MAX_BOOLEAN_LENGTH);
            writeBytes(value ? TRUE : FALSE);
            afterValue();
        }

        @Override
        public final void writeByte(Schema schema, byte value) {
            writeInteger(schema, value);
        }

        @Override
        public final void writeShort(Schema schema, short value) {
            writeInteger(schema, value);
        }

        @Override
        public final void writeInteger(Schema schema, int value) {
            writeKey(schema, MAX_INT_LENGTH);
            writeAscii(Integer.toString(value));
            afterValue();
        }

        @Override
        public final void writeLong(Schema schema, long value) {
            writeKey(schema, MAX_LONG_LENGTH);
            writeAscii(Long.toString(value));
            afterValue();
        }

        @Override
        public final void writeFloat(Schema schema, float value) {
            writeNumber(schema, Float.toString(value));
        }

        @Override
        public final void writeDouble(Schema schema, double value) {
            writeNumber(schema, Double.toString(value));
        }

        @Override
        public final void writeBigInteger(Schema schema, BigInteger value) {
            writeNumber(schema, value.toString());
        }

        @Override
        public final void writeBigDecimal(Schema schema, BigDecimal value) {
            writeNumber(schema, value.toPlainString());
        }

        private void writeNumber(Schema schema, String text) {
            writeKey(schema, text.length());
            writeAscii(text);
            afterValue();
        }

        @Override
        public final void writeString(Schema schema, String value) {
            writeKey(schema, value.length() * 3);
            writeUrlEncoded(value);
            afterValue();
        }

        @Override
        public final void writeBlob(Schema schema, ByteBuffer value) {
            byte[] encoded = ByteBufferUtils.base64EncodeToBytes(value);
            writeKey(schema, encoded.length * 3);
            writeUrlEncodedAscii(encoded);
            afterValue();
        }

        @Override
        public final void writeBlob(Schema schema, byte[] value) {
            // The bytes are only read here, so a writable view of the whole array lets base64 read it without copying.
            writeBlob(schema, ByteBuffer.wrap(value));
        }

        @Override
        public final void writeTimestamp(Schema schema, Instant value) {
            writeString(schema, timestampFormatter(schema).writeString(value));
        }

        @Override
        public final void writeDocument(Schema schema, Document value) {
            throw new SerializationException("Query protocols do not support document types: " + schema.id());
        }

        @Override
        public final void writeNull(Schema schema) {
            afterValue();
        }
    }

    /**
     * Writes structure members as {@code &prefix.Name=value}.
     */
    private final class MemberSerializer extends ValueSerializer {
        @Override
        void writeKey(Schema schema, int maxValueBytes) {
            QueryFormSerializer.this.writeKey(memberName(schema), 0, maxValueBytes);
        }

        @Override
        boolean enter(Schema schema) {
            // The input structure itself has no key; its members are written at the top level.
            if (!schema.isMember()) {
                return false;
            }
            pushPrefix(memberName(schema), 0);
            return true;
        }

        private byte[] memberName(Schema schema) {
            if (!schema.isMember()) {
                throw new SerializationException("A query form value must be a structure member, but got " + schema.id());
            }
            return AwsQuerySchemaExtensions.binding(schema).name(ec2);
        }
    }

    /**
     * Writes list elements as {@code &prefix.member.N=value}, or {@code &prefix.N=value} for a flattened or
     * {@code ec2Query} list.
     */
    private final class ListItemSerializer extends ValueSerializer {
        private byte[] memberName;
        private int index;

        @Override
        void writeKey(Schema schema, int maxValueBytes) {
            QueryFormSerializer.this.writeKey(memberName, index, maxValueBytes);
        }

        @Override
        boolean enter(Schema schema) {
            pushPrefix(memberName, index);
            return true;
        }

        @Override
        void afterValue() {
            index++;
        }
    }

    /**
     * Writes a map value as {@code &prefix=value}; the entry serializer has already pushed the value's key.
     */
    private final class MapValueSerializer extends ValueSerializer {
        @Override
        void writeKey(Schema schema, int maxValueBytes) {
            QueryFormSerializer.this.writeKey(null, 0, maxValueBytes);
        }

        @Override
        boolean enter(Schema schema) {
            return false;
        }
    }

    /**
     * Writes map entries as {@code &prefix.entry.N.key=key} followed by the value under {@code prefix.entry.N.value}.
     */
    private final class MapEntrySerializer implements MapSerializer {
        private byte[] entryName;
        private byte[] keyName;
        private byte[] valueName;
        private int index;

        @Override
        public <T> void writeEntry(Schema keySchema, String key, T state,
                                   BiConsumer<T, ShapeSerializer> valueSerializer) {
            pushPrefix(entryName, index);
            QueryFormSerializer.this.writeKey(keyName, 0, key.length() * 3);
            writeUrlEncoded(key);
            pushPrefix(valueName, 0);
            valueSerializer.accept(state, mapValue);
            popPrefix();
            popPrefix();
            index++;
        }
    }
}
