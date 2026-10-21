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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Reads an S3 event notification with the JSON codec and {@link S3EventNotificationSerde}'s builders.
 *
 * <p>It accepts and rejects what the previous {@code JsonNodeParser} reader did, with the same exception types:
 * <ul>
 *     <li>Malformed JSON throws {@link UncheckedIOException} whose cause is a {@link JsonParseException} (with the
 *     codec's {@link SerializationException} as its cause). The previous reader parsed the whole input before reading
 *     any member, so an input that is not well-formed JSON gets this exception even when the first failure the codec
 *     met was a value of the wrong type.</li>
 *     <li>A value of the wrong JSON type throws {@link IllegalArgumentException}; an instant that
 *     {@link java.time.Instant#parse} rejects throws its {@link java.time.format.DateTimeParseException}.</li>
 *     <li>A UTF-8 byte order mark and Java comments ({@code //} and {@code /* *}{@code /}) are accepted, as the
 *     previous reader's Jackson factory accepted them; content after the first JSON value is ignored.</li>
 * </ul>
 */
@SdkInternalApi
public final class CodecS3EventNotificationReader implements S3EventNotificationReader {
    private static final CodecS3EventNotificationReader INSTANCE = new CodecS3EventNotificationReader();

    private CodecS3EventNotificationReader() {
    }

    public static S3EventNotificationReader create() {
        return INSTANCE;
    }

    @Override
    public S3EventNotification read(String event) {
        return read(event.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public S3EventNotification read(InputStream event) {
        try {
            return read(IoUtils.toByteArray(event));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public S3EventNotification read(byte[] event) {
        try {
            return parse(event);
        } catch (RuntimeException e) {
            byte[] withoutExtensions = withoutJsonExtensions(event);
            if (withoutExtensions != null) {
                return read(withoutExtensions);
            }
            throw classify(event, e);
        }
    }

    private static S3EventNotification parse(byte[] json) {
        ShapeDeserializer deserializer = S3EventNotificationSerde.JSON.createDeserializer(json);
        S3EventNotification event = S3EventNotificationSerde.notificationBuilder().deserialize(deserializer).build();
        try {
            // Releases the deserializer's buffers, then fails if content follows the value.
            deserializer.close();
        } catch (SerializationException e) {
            // The previous reader read only the first JSON value and ignored what follows it.
        }
        return event;
    }

    /**
     * Malformed JSON gets the previous reader's exception, whatever failed first; other failures are rethrown, with
     * a codec exception (which only the reader's own structure syntax throws) as an {@link IllegalArgumentException}.
     */
    private static RuntimeException classify(byte[] json, RuntimeException failure) {
        try {
            // Checks that the input is well-formed up to the end of its first value.
            S3EventNotificationSerde.JSON.createDeserializer(json).readDocument();
        } catch (SerializationException syntaxError) {
            return new UncheckedIOException(new JsonParseException((JsonParser) null, syntaxError.getMessage(),
                                                                   syntaxError));
        }
        if (failure instanceof SerializationException) {
            return new IllegalArgumentException(failure.getMessage(), failure);
        }
        return failure;
    }

    /**
     * Removes a leading UTF-8 byte order mark and replaces every comment outside a string with spaces.
     *
     * @return the changed copy, or null if there is nothing to remove or a block comment is not closed (which the
     *     previous reader rejected as well)
     */
    static byte[] withoutJsonExtensions(byte[] json) {
        int start = json.length >= 3 && (json[0] & 0xFF) == 0xEF && (json[1] & 0xFF) == 0xBB && (json[2] & 0xFF) == 0xBF
                    ? 3 : 0;
        byte[] out = null;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < json.length; i++) {
            byte b = json[i];
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (b == '\\') {
                    escaped = true;
                } else if (b == '"') {
                    inString = false;
                }
                continue;
            }
            if (b == '"') {
                inString = true;
                continue;
            }
            if (b != '/' || i + 1 >= json.length) {
                continue;
            }
            int end;
            if (json[i + 1] == '/') {
                end = i + 2;
                while (end < json.length && json[end] != '\n' && json[end] != '\r') {
                    end++;
                }
            } else if (json[i + 1] == '*') {
                end = blockCommentEnd(json, i + 2);
                if (end < 0) {
                    return null;
                }
            } else {
                continue;
            }
            if (out == null) {
                out = json.clone();
            }
            Arrays.fill(out, i, end, (byte) ' ');
            i = end - 1;
        }
        if (out == null && start == 0) {
            return null;
        }
        return Arrays.copyOfRange(out == null ? json : out, start, json.length);
    }

    private static int blockCommentEnd(byte[] json, int from) {
        for (int i = from; i + 1 < json.length; i++) {
            if (json[i] == '*' && json[i + 1] == '/') {
                return i + 2;
            }
        }
        return -1;
    }
}
