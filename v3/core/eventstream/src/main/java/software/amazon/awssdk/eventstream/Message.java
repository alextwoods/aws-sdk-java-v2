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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One immutable AWS event stream message: ordered, uniquely named typed headers and a payload. {@link #toString()}
 * reports header names and the payload length only, never header or payload values.
 */
public final class Message {
    private final Map<String, HeaderValue> headers;
    private final byte[] payload;

    /**
     * A message of {@code headers} (in iteration order) and a copy of {@code payload}.
     *
     * @throws IllegalArgumentException if a header name is empty or longer than 255 UTF-8 bytes
     */
    public Message(Map<String, HeaderValue> headers, byte[] payload) {
        this(copy(headers), Objects.requireNonNull(payload, "payload").clone(), true);
    }

    private Message(Map<String, HeaderValue> headers, byte[] payload, boolean owned) {
        this.headers = headers;
        this.payload = payload;
    }

    /** Package-private: a decoded message that takes ownership of its arguments. */
    static Message wrap(LinkedHashMap<String, HeaderValue> headers, byte[] payload) {
        return new Message(Collections.unmodifiableMap(headers), payload, true);
    }

    private static Map<String, HeaderValue> copy(Map<String, HeaderValue> headers) {
        Objects.requireNonNull(headers, "headers");
        LinkedHashMap<String, HeaderValue> copy = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            Objects.requireNonNull(name, "header name");
            Objects.requireNonNull(value, "header value");
            int length = Utf8.length(name);
            if (length == 0 || length > MessageEncoder.MAX_HEADER_NAME_LENGTH) {
                throw new IllegalArgumentException("An event stream header name must have 1 to "
                                                   + MessageEncoder.MAX_HEADER_NAME_LENGTH
                                                   + " UTF-8 bytes, not " + length);
            }
            copy.put(name, value);
        });
        return Collections.unmodifiableMap(copy);
    }

    /** The headers, in wire order; unmodifiable. */
    public Map<String, HeaderValue> headers() {
        return headers;
    }

    /** The header named {@code name}, or {@code null}. */
    public HeaderValue header(String name) {
        return headers.get(name);
    }

    /** The payload as a read-only buffer. */
    public ByteBuffer payload() {
        return ByteBuffer.wrap(payload).asReadOnlyBuffer();
    }

    /** A copy of the payload. */
    public byte[] payloadBytes() {
        return payload.clone();
    }

    /** The payload length in bytes. */
    public int payloadLength() {
        return payload.length;
    }

    /** The payload without copying, for the encoder. */
    byte[] rawPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Message other && headers.equals(other.headers) && Arrays.equals(payload, other.payload);
    }

    @Override
    public int hashCode() {
        return 31 * headers.hashCode() + Arrays.hashCode(payload);
    }

    @Override
    public String toString() {
        return "Message[headers=" + headers.keySet() + ", payloadLength=" + payload.length + "]";
    }
}
