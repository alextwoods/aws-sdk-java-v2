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

package software.amazon.awssdk.core.internal.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.zip.CRC32;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.protocol.OwnedResponseBody;
import software.amazon.awssdk.core.client.protocol.ResponseBodyReader;
import software.amazon.awssdk.core.exception.Crc32MismatchException;
import software.amazon.awssdk.core.http.Crc32Validation;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * The {@code x-amz-crc32} validation and {@code Content-Encoding: gzip} decompression of the new pipeline path, applied
 * to every buffered response before the protocol reads it. The legacy path applies {@link Crc32Validation} through the
 * response handlers that {@code AwsSyncClientHandler} and {@code BaseAsyncClientHandler} decorate; those handlers do not
 * exist on the new path.
 *
 * <p>The checks and the failure are {@link Crc32Validation}'s: the checksum is the CRC32 of the body as received when
 * {@code calculateCrc32FromCompressedData} is set (DynamoDB), otherwise of the decompressed body; a mismatch throws the
 * retryable {@link Crc32MismatchException} with the legacy message. Unlike the legacy stream, which validates when the
 * unmarshaller closes it, the whole body is read and validated before the protocol parses it, so a mismatch is never
 * reported as an unmarshalling failure, and bytes after the JSON value are included, as the legacy handler does by
 * draining the stream.
 *
 * <p>A response with neither header is returned unchanged. A body the asynchronous transport already owns
 * ({@link OwnedResponseBody}) and that is not compressed is checked in place, without a copy. Otherwise the validated
 * body is buffered once, into an array sized from {@code Content-Length} when the body is not compressed
 * ({@link ResponseBodyReader}), and handed to the protocol as an owned body, so the protocol does not copy it again.
 */
@SdkInternalApi
final class Crc32ResponseValidation {
    private static final Logger log = Logger.loggerFor(Crc32ResponseValidation.class);
    private static final String CRC32_HEADER = "x-amz-crc32";
    private static final String CONTENT_ENCODING = "Content-Encoding";

    private Crc32ResponseValidation() {
    }

    /**
     * Validates the CRC32 checksum of a response and decompresses its body if needed.
     *
     * @param attributes the call's attributes, holding the transport's owned body if there is one
     * @param response the response, whose body has not been read
     * @param crc32FromCompressedData the client's {@code SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED}
     * @return the response unchanged, or with its validated (and decompressed) body as an owned body
     * @throws Crc32MismatchException if the checksum does not match
     * @throws UncheckedIOException if reading the body fails
     */
    static SdkHttpFullResponse validate(ExecutionAttributes attributes,
                                        SdkHttpFullResponse response,
                                        boolean crc32FromCompressedData) {
        Optional<String> checksum = response.firstMatchingHeader(CRC32_HEADER);
        boolean gzip = response.firstMatchingHeader(CONTENT_ENCODING).filter("gzip"::equals).isPresent();
        if (!checksum.isPresent() && !gzip) {
            return response;
        }
        if (!gzip) {
            ByteBuffer owned = OwnedResponseBody.ownedBuffer(attributes, response);
            if (owned != null) {
                validateInPlace(owned, Long.parseLong(checksum.get()));
                return response;
            }
        }
        SdkHttpFullResponse validating;
        try {
            validating = Crc32Validation.validate(crc32FromCompressedData, response);
        } catch (RuntimeException e) {
            // A non-zero checksum for a response without content, or a body that is not gzip.
            response.content().ifPresent(content -> IoUtils.closeQuietlyV2(content, log));
            throw e;
        }
        if (!validating.content().isPresent()) {
            return validating;
        }
        // The checksum stream does not change the length, so Content-Length sizes the array unless the body is
        // decompressed.
        long declaredLength = gzip ? -1 : ResponseBodyReader.declaredLength(response);
        ByteBuffer body;
        // Closing the validating stream checks the checksum and throws Crc32MismatchException on a mismatch.
        try (AbortableInputStream in = validating.content().get()) {
            body = ResponseBodyReader.read(in, declaredLength, ResponseBodyReader.DEFAULT_UNKNOWN_LENGTH_CAPACITY);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        OwnedResponseBody owned = OwnedResponseBody.of(body.array(), body.remaining());
        owned.attach(attributes);
        return validating.toBuilder().content(owned.content()).build();
    }

    private static void validateInPlace(ByteBuffer body, long expected) {
        CRC32 crc32 = new CRC32();
        crc32.update(body.array(), body.arrayOffset() + body.position(), body.remaining());
        long actual = crc32.getValue();
        if (actual != expected) {
            throw Crc32MismatchException.builder()
                                        .message(String.format("Expected %d as the Crc32 checksum but the actual "
                                                               + "calculated checksum was %d", expected, actual))
                                        .build();
        }
    }
}
