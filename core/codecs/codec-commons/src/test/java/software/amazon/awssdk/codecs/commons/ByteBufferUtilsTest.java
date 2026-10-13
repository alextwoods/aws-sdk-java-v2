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

package software.amazon.awssdk.codecs.commons;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Port of smithy-java {@code software.amazon.smithy.java.io.ByteBufferUtilsTest}, plus SDK tests for the other helpers.
 */
class ByteBufferUtilsTest {

    @Test
    public void base64EncodingPreservesBufferPosition() {
        byte[] data = "prefix-hello-suffix".getBytes(StandardCharsets.UTF_8);
        byte[] expected = Base64.getEncoder()
                                .encode("hello".getBytes(StandardCharsets.UTF_8));

        assertEncoding(expected, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
        assertEncoding(expected, ByteBuffer.wrap(data, 7, 5));
        assertEncoding(expected, ByteBuffer.wrap(data, 7, 5).asReadOnlyBuffer());

        ByteBuffer direct = ByteBuffer.allocateDirect(data.length);
        direct.put(data);
        direct.position(7);
        direct.limit(12);
        assertEncoding(expected, direct);
    }

    private static void assertEncoding(byte[] expected, ByteBuffer buffer) {
        int position = buffer.position();
        assertThat(ByteBufferUtils.base64EncodeToBytes(buffer)).isEqualTo(expected);
        assertThat(buffer.position()).isEqualTo(position);
    }

    @Test
    public void base64EncodeToWritesAtOffsetForAllBufferShapes() {
        byte[] data = "prefix-hello-suffix".getBytes(StandardCharsets.UTF_8);
        byte[] expected = Base64.getEncoder()
                                .encode("hello".getBytes(StandardCharsets.UTF_8));

        assertEncodeTo(expected, ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
        assertEncodeTo(expected, ByteBuffer.wrap(data, 7, 5));
        assertEncodeTo(expected, ByteBuffer.wrap(data, 7, 5).asReadOnlyBuffer());

        ByteBuffer direct = ByteBuffer.allocateDirect(data.length);
        direct.put(data);
        direct.position(7);
        direct.limit(12);
        assertEncodeTo(expected, direct);
    }

    @Test
    public void base64EncodedSizeMatchesJdkOutput() {
        for (int len = 0; len < 12; len++) {
            byte[] encoded = Base64.getEncoder().encode(new byte[len]);
            assertThat(ByteBufferUtils.base64EncodedSize(len)).isEqualTo(encoded.length);
        }
    }

    private static void assertEncodeTo(byte[] expected, ByteBuffer buffer) {
        int position = buffer.position();
        byte[] scratch = new byte[ByteBufferUtils.base64EncodedSize(buffer.remaining())];
        byte[] dst = new byte[expected.length + 3];
        dst[0] = '<';
        int written = ByteBufferUtils.base64EncodeTo(buffer, dst, 1, scratch);

        assertThat(written).isEqualTo(expected.length);
        byte[] region = new byte[written];
        System.arraycopy(dst, 1, region, 0, written);
        assertThat(region).isEqualTo(expected);
        assertThat(dst[0]).isEqualTo((byte) '<');
        assertThat(dst[written + 1]).isEqualTo((byte) 0);
        assertThat(buffer.position()).isEqualTo(position);
    }

    @Test
    void base64Encode_slicedBuffer_returnsBase64OfRemainingBytes() {
        ByteBuffer buffer = ByteBuffer.wrap("xxhi!".getBytes(StandardCharsets.UTF_8), 2, 3);

        assertThat(ByteBufferUtils.base64Encode(buffer)).isEqualTo("aGkh");
        assertThat(ByteBufferUtils.base64Encode(ByteBuffer.allocate(0))).isEmpty();
    }

    @Test
    void getBytes_bufferOverWholeArray_returnsSameArray() {
        byte[] array = {1, 2, 3};

        assertThat(ByteBufferUtils.getBytes(ByteBuffer.wrap(array))).isSameAs(array);
    }

    @Test
    void getBytes_partialOrDirectBuffer_copiesRemainingBytesWithoutMovingPosition() {
        byte[] array = {1, 2, 3, 4};
        ByteBuffer partial = ByteBuffer.wrap(array, 1, 2);
        ByteBuffer direct = ByteBuffer.allocateDirect(4);
        direct.put(array);
        direct.position(2);

        assertThat(ByteBufferUtils.getBytes(partial)).containsExactly(2, 3).isNotSameAs(array);
        assertThat(partial.position()).isEqualTo(1);
        assertThat(ByteBufferUtils.getBytes(direct)).containsExactly(3, 4);
        assertThat(direct.position()).isEqualTo(2);
    }

    @Test
    void getUTF8String_slicedBuffer_decodesRemainingBytes() {
        String text = "caf\u00e9"; // e with acute accent, two bytes in UTF-8
        byte[] data = ("--" + text + "--").getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(data, 2, data.length - 4);

        assertThat(ByteBufferUtils.getUTF8String(buffer)).isEqualTo(text);
        assertThat(buffer.position()).isEqualTo(2);
    }

    @Test
    void byteBufferInputStream_readToEnd_readsRemainingBytesAndAdvancesBuffer() throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap("abcdef".getBytes(StandardCharsets.US_ASCII), 1, 4);
        InputStream stream = ByteBufferUtils.byteBufferInputStream(buffer);

        assertThat(stream.available()).isEqualTo(4);
        assertThat(stream.read()).isEqualTo('b');
        assertThat(IoUtils.toByteArray(stream)).isEqualTo("cde".getBytes(StandardCharsets.US_ASCII));
        assertThat(stream.read()).isEqualTo(-1);
        assertThat(stream.read(new byte[1], 0, 1)).isEqualTo(-1);
        assertThat(buffer.hasRemaining()).isFalse();
    }

    @Test
    void byteBufferInputStream_highBitByte_readsUnsignedValue() throws IOException {
        InputStream stream = ByteBufferUtils.byteBufferInputStream(ByteBuffer.wrap(new byte[] {(byte) 0xFF}));

        assertThat(stream.read()).isEqualTo(255);
    }
}
