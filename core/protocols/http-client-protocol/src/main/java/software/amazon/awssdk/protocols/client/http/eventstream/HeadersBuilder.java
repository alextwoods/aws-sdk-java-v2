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

package software.amazon.awssdk.protocols.client.http.eventstream;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.eventstream.HeaderValue;

/**
 * Builds the headers of an event stream message.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.events.HeadersBuilder}. The headers are kept in
 * case-insensitive name order, the order in which the legacy SDK encodes them (it copies them from an
 * {@code SdkHttpFullRequest}), instead of a {@code HashMap}'s order, so the encoded bytes of an event with only string
 * headers are the same as the legacy SDK's.
 */
@SdkInternalApi
final class HeadersBuilder {
    static final String MESSAGE_TYPE = ":message-type";
    static final String EVENT_TYPE = ":event-type";
    static final String EXCEPTION_TYPE = ":exception-type";
    static final String CONTENT_TYPE = ":content-type";
    static final String ERROR_CODE = ":error-code";
    static final String ERROR_MESSAGE = ":error-message";

    private final Map<String, HeaderValue> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private HeadersBuilder() {
    }

    static HeadersBuilder forEvent() {
        return new HeadersBuilder().messageType(MessageType.EVENT);
    }

    static HeadersBuilder forError() {
        return new HeadersBuilder().messageType(MessageType.ERROR);
    }

    static HeadersBuilder forException() {
        return new HeadersBuilder().messageType(MessageType.EXCEPTION);
    }

    HeadersBuilder messageType(MessageType messageType) {
        return put(MESSAGE_TYPE, messageType.toString());
    }

    HeadersBuilder contentType(String contentType) {
        return put(CONTENT_TYPE, contentType);
    }

    HeadersBuilder eventType(String eventType) {
        return put(EVENT_TYPE, eventType);
    }

    HeadersBuilder exceptionType(String exceptionType) {
        return put(EXCEPTION_TYPE, exceptionType);
    }

    HeadersBuilder errorCode(String errorCode) {
        return put(ERROR_CODE, errorCode);
    }

    HeadersBuilder errorMessage(String errorMessage) {
        return put(ERROR_MESSAGE, errorMessage);
    }

    HeadersBuilder put(String name, String value) {
        headers.put(name, HeaderValue.fromString(value));
        return this;
    }

    HeadersBuilder put(String name, int value) {
        headers.put(name, HeaderValue.fromInteger(value));
        return this;
    }

    HeadersBuilder put(String name, long value) {
        headers.put(name, HeaderValue.fromLong(value));
        return this;
    }

    HeadersBuilder put(String name, boolean value) {
        headers.put(name, HeaderValue.fromBoolean(value));
        return this;
    }

    HeadersBuilder put(String name, short value) {
        headers.put(name, HeaderValue.fromShort(value));
        return this;
    }

    HeadersBuilder put(String name, byte value) {
        headers.put(name, HeaderValue.fromByte(value));
        return this;
    }

    HeadersBuilder put(String name, ByteBuffer value) {
        headers.put(name, HeaderValue.fromByteBuffer(value));
        return this;
    }

    HeadersBuilder put(String name, Instant value) {
        headers.put(name, HeaderValue.fromTimestamp(value));
        return this;
    }

    Map<String, HeaderValue> build() {
        return headers;
    }

    enum MessageType {
        EVENT("event"),
        ERROR("error"),
        EXCEPTION("exception");

        private final String value;

        MessageType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
