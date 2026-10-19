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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.protocol.OwnedResponseBody;
import software.amazon.awssdk.core.exception.Crc32MismatchException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.IoUtils;

class Crc32ResponseValidationTest {
    private static final byte[] JSON = "{\"StringMember\":\"foo\"}".getBytes(StandardCharsets.UTF_8);

    @Test
    void validate_withoutChecksumOrGzip_returnsTheResponseUnread() {
        TrackedStream body = new TrackedStream(JSON);
        SdkHttpFullResponse response = response(body).build();

        assertThat(Crc32ResponseValidation.validate(new ExecutionAttributes(), response, false)).isSameAs(response);
        assertThat(body.read).isFalse();
    }

    @Test
    void validate_withMatchingChecksum_buffersTheBodyAsAnOwnedBody() throws IOException {
        ExecutionAttributes attributes = new ExecutionAttributes();
        TrackedStream body = new TrackedStream(JSON);
        SdkHttpFullResponse validated = Crc32ResponseValidation.validate(
            attributes, response(body).putHeader("x-amz-crc32", crc32(JSON)).build(), false);

        assertThat(body.closed.get()).isTrue();
        ByteBuffer owned = OwnedResponseBody.ownedBuffer(attributes, validated);
        assertThat(owned).isNotNull();
        assertThat(bytes(owned)).isEqualTo(JSON);
        assertThat(IoUtils.toByteArray(validated.content().get())).isEqualTo(JSON);
    }

    @Test
    void validate_withMismatchingChecksum_throwsTheLegacyExceptionAndClosesTheBody() {
        TrackedStream body = new TrackedStream(JSON);
        SdkHttpFullResponse response = response(body).putHeader("x-amz-crc32", "1").build();

        assertThatThrownBy(() -> Crc32ResponseValidation.validate(new ExecutionAttributes(), response, false))
            .isInstanceOf(Crc32MismatchException.class)
            .hasMessage("Expected 1 as the Crc32 checksum but the actual calculated checksum was " + crc32(JSON));
        assertThat(body.closed.get()).isTrue();
        assertThat(Crc32MismatchException.builder().build().retryable()).isTrue();
    }

    @Test
    void validate_withAnOwnedBody_checksItInPlace() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        OwnedResponseBody owned = OwnedResponseBody.of(JSON.clone(), JSON.length);
        owned.attach(attributes);
        SdkHttpFullResponse response = SdkHttpFullResponse.builder().statusCode(200).content(owned.content())
                                                           .putHeader("x-amz-crc32", crc32(JSON)).build();

        assertThat(Crc32ResponseValidation.validate(attributes, response, false)).isSameAs(response);
        assertThat(OwnedResponseBody.ownedBuffer(attributes, response)).isNotNull();

        SdkHttpFullResponse mismatching = response.toBuilder().putHeader("x-amz-crc32", "7").build();
        assertThatThrownBy(() -> Crc32ResponseValidation.validate(attributes, mismatching, false))
            .isInstanceOf(Crc32MismatchException.class)
            .hasMessage("Expected 7 as the Crc32 checksum but the actual calculated checksum was " + crc32(JSON));
    }

    @Test
    void validate_withGzipAndCrc32FromCompressedData_checksTheCompressedBodyAndDecompresses() throws IOException {
        byte[] gzipped = gzip(JSON);
        SdkHttpFullResponse response = gzipResponse(gzipped, crc32(gzipped));

        SdkHttpFullResponse validated = Crc32ResponseValidation.validate(new ExecutionAttributes(), response, true);
        assertThat(IoUtils.toByteArray(validated.content().get())).isEqualTo(JSON);

        SdkHttpFullResponse uncompressedChecksum = gzipResponse(gzipped, crc32(JSON));
        assertThatThrownBy(() -> Crc32ResponseValidation.validate(new ExecutionAttributes(), uncompressedChecksum, true))
            .isInstanceOf(Crc32MismatchException.class);
    }

    @Test
    void validate_withGzipAndCrc32FromDecompressedData_checksTheDecompressedBody() throws IOException {
        byte[] gzipped = gzip(JSON);

        SdkHttpFullResponse validated =
            Crc32ResponseValidation.validate(new ExecutionAttributes(), gzipResponse(gzipped, crc32(JSON)), false);
        assertThat(IoUtils.toByteArray(validated.content().get())).isEqualTo(JSON);

        assertThatThrownBy(() -> Crc32ResponseValidation.validate(new ExecutionAttributes(),
                                                                  gzipResponse(gzipped, crc32(gzipped)), false))
            .isInstanceOf(Crc32MismatchException.class);
    }

    @Test
    void validate_withGzipAndNoChecksum_decompresses() throws IOException {
        SdkHttpFullResponse response =
            response(new TrackedStream(gzip(JSON))).putHeader("Content-Encoding", "gzip").build();

        SdkHttpFullResponse validated = Crc32ResponseValidation.validate(new ExecutionAttributes(), response, false);
        assertThat(IoUtils.toByteArray(validated.content().get())).isEqualTo(JSON);
    }

    @Test
    void validate_withoutContent_acceptsOnlyAZeroChecksum() {
        SdkHttpFullResponse zero = SdkHttpFullResponse.builder().statusCode(200).putHeader("x-amz-crc32", "0").build();
        assertThat(Crc32ResponseValidation.validate(new ExecutionAttributes(), zero, false)).isSameAs(zero);

        SdkHttpFullResponse nonZero = zero.toBuilder().putHeader("x-amz-crc32", "3049587505").build();
        assertThatThrownBy(() -> Crc32ResponseValidation.validate(new ExecutionAttributes(), nonZero, false))
            .isInstanceOf(Crc32MismatchException.class)
            .hasMessage("Expected 3049587505 as the Crc32 checksum but the response had no content");
    }

    private static SdkHttpFullResponse gzipResponse(byte[] gzipped, String checksum) {
        return response(new TrackedStream(gzipped)).putHeader("Content-Encoding", "gzip")
                                                   .putHeader("x-amz-crc32", checksum)
                                                   .build();
    }

    private static SdkHttpFullResponse.Builder response(TrackedStream body) {
        return SdkHttpFullResponse.builder().statusCode(200).content(AbortableInputStream.create(body));
    }

    private static String crc32(byte[] bytes) {
        CRC32 crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return String.valueOf(crc32.getValue());
    }

    private static byte[] gzip(byte[] bytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes);
        }
        return out.toByteArray();
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    private static final class TrackedStream extends ByteArrayInputStream {
        private final AtomicBoolean closed = new AtomicBoolean();
        private boolean read;

        TrackedStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) {
            read = true;
            return super.read(b, off, len);
        }

        @Override
        public synchronized int read() {
            read = true;
            return super.read();
        }

        @Override
        public void close() throws IOException {
            closed.set(true);
            super.close();
        }
    }
}
