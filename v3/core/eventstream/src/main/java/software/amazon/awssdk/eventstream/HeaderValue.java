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

package software.amazon.awssdk.eventstream;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * An immutable typed header value of an event stream {@link Message}. Timestamps have millisecond precision (the wire
 * format); {@link #ofTimestamp} truncates finer precision. {@link #toString()} reports the type only, never the value.
 */
public final class HeaderValue {
    /** The largest byte array or UTF-8 string header value, in bytes. */
    public static final int MAX_VALUE_LENGTH = Short.MAX_VALUE;

    private static final HeaderValue TRUE = new HeaderValue(HeaderType.BOOLEAN_TRUE, Boolean.TRUE);
    private static final HeaderValue FALSE = new HeaderValue(HeaderType.BOOLEAN_FALSE, Boolean.FALSE);

    private final HeaderType type;
    private final Object value;

    private HeaderValue(HeaderType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static HeaderValue ofBoolean(boolean value) {
        return value ? TRUE : FALSE;
    }

    public static HeaderValue ofByte(byte value) {
        return new HeaderValue(HeaderType.BYTE, value);
    }

    public static HeaderValue ofShort(short value) {
        return new HeaderValue(HeaderType.SHORT, value);
    }

    public static HeaderValue ofInteger(int value) {
        return new HeaderValue(HeaderType.INTEGER, value);
    }

    public static HeaderValue ofLong(long value) {
        return new HeaderValue(HeaderType.LONG, value);
    }

    /**
     * A byte array value (copied).
     *
     * @throws IllegalArgumentException if longer than {@link #MAX_VALUE_LENGTH}
     */
    public static HeaderValue ofByteArray(byte[] value) {
        Objects.requireNonNull(value, "value");
        checkLength(value.length, "byte array");
        return new HeaderValue(HeaderType.BYTE_ARRAY, value.clone());
    }

    /**
     * A string value.
     *
     * @throws IllegalArgumentException if its UTF-8 form is longer than {@link #MAX_VALUE_LENGTH}
     */
    public static HeaderValue ofString(String value) {
        Objects.requireNonNull(value, "value");
        checkLength(Utf8.length(value), "string");
        return new HeaderValue(HeaderType.STRING, value);
    }

    /** A timestamp value, truncated to milliseconds. */
    public static HeaderValue ofTimestamp(Instant value) {
        Objects.requireNonNull(value, "value");
        return new HeaderValue(HeaderType.TIMESTAMP, Instant.ofEpochMilli(value.toEpochMilli()));
    }

    public static HeaderValue ofUuid(UUID value) {
        return new HeaderValue(HeaderType.UUID, Objects.requireNonNull(value, "value"));
    }

    /** Package-private: a decoded byte array, owned by the value. */
    static HeaderValue wrapByteArray(byte[] value) {
        return new HeaderValue(HeaderType.BYTE_ARRAY, value);
    }

    private static void checkLength(int length, String kind) {
        if (length > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("An event stream " + kind + " header value is limited to "
                                               + MAX_VALUE_LENGTH + " bytes, not " + length);
        }
    }

    /** The value's type; a boolean is {@link HeaderType#BOOLEAN_TRUE} or {@link HeaderType#BOOLEAN_FALSE}. */
    public HeaderType type() {
        return type;
    }

    public boolean asBoolean() {
        return (Boolean) expect(HeaderType.BOOLEAN_TRUE, HeaderType.BOOLEAN_FALSE);
    }

    public byte asByte() {
        return (Byte) expect(HeaderType.BYTE, HeaderType.BYTE);
    }

    public short asShort() {
        return (Short) expect(HeaderType.SHORT, HeaderType.SHORT);
    }

    public int asInteger() {
        return (Integer) expect(HeaderType.INTEGER, HeaderType.INTEGER);
    }

    public long asLong() {
        return (Long) expect(HeaderType.LONG, HeaderType.LONG);
    }

    /** A copy of the byte array value. */
    public byte[] asByteArray() {
        return ((byte[]) expect(HeaderType.BYTE_ARRAY, HeaderType.BYTE_ARRAY)).clone();
    }

    public String asString() {
        return (String) expect(HeaderType.STRING, HeaderType.STRING);
    }

    public Instant asTimestamp() {
        return (Instant) expect(HeaderType.TIMESTAMP, HeaderType.TIMESTAMP);
    }

    public UUID asUuid() {
        return (UUID) expect(HeaderType.UUID, HeaderType.UUID);
    }

    /** The byte array value without copying, for the encoder. */
    byte[] rawBytes() {
        return (byte[]) value;
    }

    private Object expect(HeaderType first, HeaderType second) {
        if (type != first && type != second) {
            throw new IllegalStateException("The header value is a " + type + ", not a "
                                            + (first == HeaderType.BOOLEAN_TRUE ? "BOOLEAN" : first));
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HeaderValue other) || type != other.type) {
            return false;
        }
        return type == HeaderType.BYTE_ARRAY ? Arrays.equals((byte[]) value, (byte[]) other.value)
                                             : value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return 31 * type.hashCode() + (type == HeaderType.BYTE_ARRAY ? Arrays.hashCode((byte[]) value)
                                                                     : value.hashCode());
    }

    /** The type only; header values may be sensitive and are never rendered. */
    @Override
    public String toString() {
        return "HeaderValue[" + type + "]";
    }
}
