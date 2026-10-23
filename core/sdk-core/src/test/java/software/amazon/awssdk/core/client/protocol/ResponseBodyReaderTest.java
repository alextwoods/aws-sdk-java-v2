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

package software.amazon.awssdk.core.client.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.client.protocol.ResponseBodyReader.DEFAULT_UNKNOWN_LENGTH_CAPACITY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.http.SdkHttpResponse;

class ResponseBodyReaderTest {

    @Test
    void knownLength_isReadIntoAnExactSizeArray() throws IOException {
        byte[] body = bytes(1000);
        ByteBuffer buffer = ResponseBodyReader.read(trickle(body), 1000, DEFAULT_UNKNOWN_LENGTH_CAPACITY);

        assertThat(buffer.hasArray()).isTrue();
        assertThat(buffer.array()).hasSize(1000);
        assertThat(buffer.arrayOffset() + buffer.position()).isZero();
        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @Test
    void knownLength_ignoresTheUnknownLengthCapacity() throws IOException {
        byte[] body = bytes(100);

        assertThat(ResponseBodyReader.read(new ByteArrayInputStream(body), 100, 5000).array()).hasSize(100);
        assertThat(ResponseBodyReader.read(new ByteArrayInputStream(body), 100, 0).array()).hasSize(100);
    }

    @Test
    void knownLength_largeBody_isReadIntoAnExactSizeArray() throws IOException {
        byte[] body = bytes(3 * 1024 * 1024 + 17);
        ByteBuffer buffer = ResponseBodyReader.read(trickle(body), body.length, DEFAULT_UNKNOWN_LENGTH_CAPACITY);

        assertThat(buffer.array()).hasSize(body.length);
        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @Test
    void knownLength_zero_isEmpty() throws IOException {
        ByteBuffer buffer = ResponseBodyReader.read(new ByteArrayInputStream(new byte[0]), 0, 64);

        assertThat(buffer.remaining()).isZero();
        assertThat(buffer.array()).isEmpty();
    }

    @Test
    void bodyShorterThanTheDeclaredLength_isReturnedAsRead() throws IOException {
        byte[] body = bytes(10);
        ByteBuffer buffer = ResponseBodyReader.read(trickle(body), 100, DEFAULT_UNKNOWN_LENGTH_CAPACITY);

        assertThat(buffer.position()).isZero();
        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @Test
    void bodyLongerThanTheDeclaredLength_isReadCompletely() throws IOException {
        byte[] body = bytes(3000);
        ByteBuffer buffer = ResponseBodyReader.read(trickle(body), 100, DEFAULT_UNKNOWN_LENGTH_CAPACITY);

        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @ParameterizedTest
    @CsvSource({"0, 1024", "1, 1024", "1023, 1024", "1024, 1024", "1025, 2048", "70000, 131072"})
    void unknownLength_withTheDefaultCapacity_doublesFrom1KiB(int size, int expectedArrayLength) throws IOException {
        byte[] body = bytes(size);
        ByteBuffer buffer = ResponseBodyReader.read(trickle(body), -1, DEFAULT_UNKNOWN_LENGTH_CAPACITY);

        assertThat(buffer.array()).hasSize(expectedArrayLength);
        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 299, 300})
    void unknownLength_bodyWithinTheCapacity_isReadWithoutGrowing(int size) throws IOException {
        byte[] body = bytes(size);
        ByteBuffer buffer = ResponseBodyReader.read(trickle(body), -1, 300);

        assertThat(buffer.array()).hasSize(300);
        assertThat(buffer.position()).isZero();
        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @Test
    void unknownLength_bodyBeyondTheCapacity_growsAndIsComplete() throws IOException {
        byte[] body = bytes(5000);
        ByteBuffer buffer = ResponseBodyReader.read(trickle(body), -1, 300);

        assertThat(buffer.array().length).isGreaterThanOrEqualTo(5000);
        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @Test
    void unknownLength_largeBody_isComplete() throws IOException {
        byte[] body = bytes(3 * 1024 * 1024 + 17);

        assertThat(toArray(ResponseBodyReader.read(trickle(body), -1, DEFAULT_UNKNOWN_LENGTH_CAPACITY))).isEqualTo(body);
        assertThat(toArray(ResponseBodyReader.read(trickle(body), -1, 1 << 20))).isEqualTo(body);
    }

    @Test
    void unknownLength_zeroCapacity_readsEmptyAndNonEmptyBodies() throws IOException {
        ByteBuffer empty = ResponseBodyReader.read(new ByteArrayInputStream(new byte[0]), -1, 0);
        assertThat(empty.remaining()).isZero();
        assertThat(empty.array()).isEmpty();

        byte[] body = bytes(10);
        ByteBuffer buffer = ResponseBodyReader.read(trickle(body), -1, 0);
        assertThat(buffer.array()).hasSize(DEFAULT_UNKNOWN_LENGTH_CAPACITY);
        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -100, Integer.MAX_VALUE - 7, Long.MAX_VALUE})
    void negativeOrTooLargeDeclaredLength_isTreatedAsUnknown(long declaredLength) throws IOException {
        byte[] body = bytes(2000);
        ByteBuffer buffer = ResponseBodyReader.read(trickle(body), declaredLength, 300);

        assertThat(toArray(buffer)).isEqualTo(body);
    }

    @Test
    void negativeCapacity_isRejected() {
        assertThatThrownBy(() -> ResponseBodyReader.read(new ByteArrayInputStream(new byte[0]), -1, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unknownLengthCapacity");
    }

    @Test
    void readFailure_propagates() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("reset");
            }
        };

        assertThatThrownBy(() -> ResponseBodyReader.read(failing, 10, DEFAULT_UNKNOWN_LENGTH_CAPACITY))
            .isInstanceOf(IOException.class).hasMessage("reset");
        assertThatThrownBy(() -> ResponseBodyReader.read(failing, -1, DEFAULT_UNKNOWN_LENGTH_CAPACITY))
            .isInstanceOf(IOException.class).hasMessage("reset");
    }

    @Test
    void read_doesNotCloseTheStream() throws IOException {
        boolean[] closed = new boolean[1];
        InputStream in = new ByteArrayInputStream(bytes(10)) {
            @Override
            public void close() {
                closed[0] = true;
            }
        };

        ResponseBodyReader.read(in, 10, DEFAULT_UNKNOWN_LENGTH_CAPACITY);
        ResponseBodyReader.read(in, -1, DEFAULT_UNKNOWN_LENGTH_CAPACITY);
        assertThat(closed[0]).isFalse();
    }

    @Test
    void declaredLength_parsesTheFirstContentLength() {
        assertThat(ResponseBodyReader.declaredLength(SdkHttpResponse.builder().statusCode(200).build())).isEqualTo(-1);
        assertThat(ResponseBodyReader.declaredLength(withContentLength(" 10 "))).isEqualTo(10);
        assertThat(ResponseBodyReader.declaredLength(withContentLength("-1"))).isEqualTo(-1);
        assertThat(ResponseBodyReader.declaredLength(withContentLength("-5"))).isEqualTo(-5);
        assertThat(ResponseBodyReader.declaredLength(withContentLength("x"))).isEqualTo(-1);
        assertThat(ResponseBodyReader.declaredLength(withContentLength("99999999999"))).isEqualTo(99999999999L);
        assertThat(ResponseBodyReader.declaredLength(SdkHttpResponse.builder().statusCode(200)
                                                                    .putHeader("content-length", "7").build()))
            .isEqualTo(7);
    }

    private static SdkHttpResponse withContentLength(String value) {
        return SdkHttpResponse.builder().statusCode(200).putHeader("Content-Length", value).build();
    }

    /**
     * A stream that returns at most 7 bytes per read, to exercise partial reads.
     */
    private static InputStream trickle(byte[] body) {
        return new ByteArrayInputStream(body) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                return super.read(b, off, Math.min(len, 7));
            }
        };
    }

    private static byte[] bytes(int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) (i * 31);
        }
        return bytes;
    }

    private static byte[] toArray(ByteBuffer buffer) {
        return Arrays.copyOfRange(buffer.array(), buffer.arrayOffset() + buffer.position(),
                                  buffer.arrayOffset() + buffer.limit());
    }
}
