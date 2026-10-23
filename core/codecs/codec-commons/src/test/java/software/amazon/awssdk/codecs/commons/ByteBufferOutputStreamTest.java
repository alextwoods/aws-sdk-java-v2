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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.utils.IoUtils;

class ByteBufferOutputStreamTest {

    @Test
    void write_bytesAndRanges_appendsLowEightBitsAndRanges() {
        ByteBufferOutputStream out = new ByteBufferOutputStream();
        out.write('a');
        out.write(0x162); // only the low eight bits are written
        out.write("xxhelloxx".getBytes(StandardCharsets.UTF_8), 2, 5);
        out.write(new byte[0], 0, 0);

        assertThat(out.size()).isEqualTo(7);
        assertThat(written(out)).isEqualTo("abhello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void writeAscii_asciiStrings_appendsOneBytePerChar() {
        ByteBufferOutputStream out = new ByteBufferOutputStream(1);
        out.writeAscii("");
        out.writeAscii("{\"a\":");
        out.writeAscii("1}");

        assertThat(new String(written(out), StandardCharsets.US_ASCII)).isEqualTo("{\"a\":1}");
    }

    @Test
    void write_fromZeroAndSmallCapacities_growsAndKeepsAllBytes() {
        Random random = new Random(42);
        for (int initial : new int[] {0, 1, 2, 31, 32}) {
            ByteBufferOutputStream out = new ByteBufferOutputStream(initial);
            ByteArrayOutputStream expected = new ByteArrayOutputStream();
            for (int i = 0; i < 200; i++) {
                byte[] chunk = new byte[random.nextInt(70)];
                random.nextBytes(chunk);
                if (chunk.length == 1) {
                    out.write(chunk[0]);
                } else {
                    out.write(chunk, 0, chunk.length);
                }
                expected.write(chunk, 0, chunk.length);
            }
            assertThat(written(out)).isEqualTo(expected.toByteArray());
            assertThat(out.array().length).isGreaterThanOrEqualTo(out.size());
        }
    }

    @Test
    void write_beyondCapacity_growsByAtLeastCurrentCapacity() {
        ByteBufferOutputStream out = new ByteBufferOutputStream(8);
        out.write(new byte[9], 0, 9);
        assertThat(out.array().length).isEqualTo(16);
        out.write(new byte[100], 0, 100);
        assertThat(out.array().length).isEqualTo(109);
    }

    @Test
    void write_invalidArguments_throwsAndWritesNothing() {
        ByteBufferOutputStream out = new ByteBufferOutputStream();
        byte[] b = new byte[4];
        assertThatThrownBy(() -> out.write(b, -1, 1)).isInstanceOf(IndexOutOfBoundsException.class)
                                                       .hasMessageContaining("out of bounds for length 4");
        assertThatThrownBy(() -> out.write(b, 0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> out.write(b, 3, 2)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> out.write(b, Integer.MAX_VALUE, 1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> out.write(null, 0, 0)).isInstanceOf(NullPointerException.class)
                                                        .hasMessageContaining("b must not be null");
        assertThatThrownBy(() -> new ByteBufferOutputStream(-1)).isInstanceOf(IllegalArgumentException.class)
                                                                 .hasMessageContaining("initialLength");
        assertThat(out.size()).isZero();
    }

    @Test
    void toByteBuffer_afterWrites_wrapsInternalArrayWithoutCopy() {
        ByteBufferOutputStream out = new ByteBufferOutputStream();
        out.writeAscii("hello");

        ByteBuffer buffer = out.toByteBuffer();

        assertThat(buffer.hasArray()).isTrue();
        assertThat(buffer.array()).isSameAs(out.array());
        assertThat(buffer.arrayOffset()).isZero();
        assertThat(buffer.position()).isZero();
        assertThat(buffer.remaining()).isEqualTo(5);
    }

    @Test
    void writeTo_afterWrites_writesWrittenBytes() throws IOException {
        ByteBufferOutputStream out = new ByteBufferOutputStream();
        out.writeAscii("abc");
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        out.writeTo(sink);
        assertThat(sink.toByteArray()).isEqualTo("abc".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void reset_notShared_reusesArray() {
        ByteBufferOutputStream out = new ByteBufferOutputStream();
        out.writeAscii("hello");
        byte[] array = out.array();

        out.reset();
        out.writeAscii("hi");

        assertThat(out.array()).isSameAs(array);
        assertThat(new String(written(out), StandardCharsets.US_ASCII)).isEqualTo("hi");
    }

    @Test
    void toContentStreamProvider_contentAsByteBuffer_isWritableViewOverSameArray() {
        ByteBufferOutputStream out = new ByteBufferOutputStream();
        out.writeAscii("{\"k\":\"v\"}");
        byte[] internal = out.array();

        ByteBuffer view = out.toContentStreamProvider().contentAsByteBufferOrNull();

        assertThat(view).isNotNull();
        assertThat(view.isReadOnly()).isFalse();
        assertThat(view.hasArray()).isTrue();
        assertThat(view.array()).isSameAs(internal);
        assertThat(view.arrayOffset()).isZero();
        assertThat(view.position()).isZero();
        assertThat(view.limit()).isEqualTo(out.size());
        assertThat(view.isDirect()).isFalse();
    }

    @Test
    void toContentStreamProvider_contentAsByteBufferCalledTwice_returnsNewViewEachTime() {
        ByteBufferOutputStream out = new ByteBufferOutputStream();
        out.writeAscii("abcdef");
        ContentStreamProvider provider = out.toContentStreamProvider();

        ByteBuffer first = provider.contentAsByteBufferOrNull();
        first.position(3);
        first.limit(4);
        first.mark();
        ByteBuffer second = provider.contentAsByteBufferOrNull();

        assertThat(second).isNotSameAs(first);
        assertThat(second.array()).isSameAs(first.array());
        assertThat(second.position()).isZero();
        assertThat(second.remaining()).isEqualTo(6);
    }

    @Test
    void toContentStreamProvider_newStreamCalledRepeatedly_yieldsSameBytes() throws IOException {
        ByteBufferOutputStream out = new ByteBufferOutputStream(2);
        byte[] data = new byte[1000];
        new Random(7).nextBytes(data);
        out.write(data, 0, data.length);
        ContentStreamProvider provider = out.toContentStreamProvider();

        for (int i = 0; i < 3; i++) {
            try (InputStream stream = provider.newStream()) {
                assertThat(IoUtils.toByteArray(stream)).isEqualTo(data);
            }
        }
        assertThat(bytes(provider.contentAsByteBufferOrNull())).isEqualTo(data);
        assertThat(provider.name()).isEqualTo(ContentStreamProvider.ProviderType.BYTES.getName());
    }

    @Test
    void toContentStreamProvider_emptyStream_providesEmptyContent() throws IOException {
        ContentStreamProvider provider = new ByteBufferOutputStream(0).toContentStreamProvider();
        assertThat(provider.contentAsByteBufferOrNull().remaining()).isZero();
        try (InputStream stream = provider.newStream()) {
            assertThat(stream.read()).isEqualTo(-1);
        }
    }

    @Test
    void toContentStreamProvider_writeAfterCreation_contentUnchanged() throws IOException {
        // Capacity 16: the first append stays in the shared array, the second one grows it.
        ByteBufferOutputStream out = new ByteBufferOutputStream(16);
        out.writeAscii("hello");
        ContentStreamProvider provider = out.toContentStreamProvider();
        byte[] sharedArray = provider.contentAsByteBufferOrNull().array();

        out.writeAscii(" world");
        assertThat(out.array()).isSameAs(sharedArray);
        assertContent(provider, "hello");

        out.writeAscii(", and more text that grows the array");
        assertThat(out.array()).isNotSameAs(sharedArray);
        assertContent(provider, "hello");
        assertThat(new String(written(out), StandardCharsets.US_ASCII))
            .isEqualTo("hello world, and more text that grows the array");
    }

    @Test
    void toContentStreamProvider_resetAndWriteAfterCreation_contentUnchanged() throws IOException {
        ByteBufferOutputStream out = new ByteBufferOutputStream(16);
        out.writeAscii("hello");
        ContentStreamProvider provider = out.toContentStreamProvider();
        byte[] sharedArray = out.array();

        out.reset();
        out.writeAscii("HELLO");

        assertThat(out.array()).isNotSameAs(sharedArray).hasSize(16);
        assertContent(provider, "hello");
        assertThat(new String(written(out), StandardCharsets.US_ASCII)).isEqualTo("HELLO");

        // The new array is not shared yet, so the next reset reuses it.
        byte[] newArray = out.array();
        out.reset();
        assertThat(out.array()).isSameAs(newArray);
    }

    @Test
    void toByteBuffer_resetAndWriteAfterCall_bufferUnchanged() {
        ByteBufferOutputStream out = new ByteBufferOutputStream(16);
        out.writeAscii("hello");
        ByteBuffer buffer = out.toByteBuffer();

        out.reset();
        out.writeAscii("HELLO");

        assertThat(new String(bytes(buffer), StandardCharsets.US_ASCII)).isEqualTo("hello");
    }

    @Test
    void toContentStreamProvider_calledTwice_eachCapturesBytesWrittenBeforeIt() throws IOException {
        ByteBufferOutputStream out = new ByteBufferOutputStream();
        out.writeAscii("ab");
        ContentStreamProvider first = out.toContentStreamProvider();
        out.writeAscii("cd");
        ContentStreamProvider second = out.toContentStreamProvider();

        assertContent(first, "ab");
        assertContent(second, "abcd");
    }

    @Test
    void reserveAndAdvance_writeIntoTheInternalArray() {
        ByteBufferOutputStream out = new ByteBufferOutputStream(8);
        out.write('a');
        byte[] array = out.reserve(3);
        assertThat(array).isSameAs(out.array());
        array[1] = 'b';
        array[2] = 'c';
        out.advance(2);
        assertThat(out.size()).isEqualTo(3);
        assertThat(written(out)).isEqualTo("abc".getBytes(StandardCharsets.UTF_8));
        assertThat(out.reserve(0)).isSameAs(array);
    }

    @Test
    void reserve_growsAndKeepsTheBytesWritten() {
        ByteBufferOutputStream out = new ByteBufferOutputStream(2);
        out.write("ab".getBytes(StandardCharsets.UTF_8), 0, 2);
        byte[] before = out.array();
        byte[] array = out.reserve(100);
        assertThat(array).isNotSameAs(before).isSameAs(out.array());
        assertThat(array.length).isGreaterThanOrEqualTo(102);
        System.arraycopy("cd".getBytes(StandardCharsets.UTF_8), 0, array, 2, 2);
        out.advance(2);
        assertThat(written(out)).isEqualTo("abcd".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void reserve_afterToByteBuffer_writesPastTheCapturedBytes() {
        ByteBufferOutputStream out = new ByteBufferOutputStream(16);
        out.write("ab".getBytes(StandardCharsets.UTF_8), 0, 2);
        ByteBuffer captured = out.toByteBuffer();
        byte[] array = out.reserve(2);
        array[2] = 'c';
        out.advance(1);
        assertThat(bytes(captured)).isEqualTo("ab".getBytes(StandardCharsets.UTF_8));
        assertThat(written(out)).isEqualTo("abc".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void reserveAndAdvance_rejectInvalidLengths() {
        ByteBufferOutputStream out = new ByteBufferOutputStream(4);
        out.write('a');
        assertThatThrownBy(() -> out.reserve(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> out.advance(-1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> out.advance(4)).isInstanceOf(IndexOutOfBoundsException.class);
        out.advance(3);
        assertThat(out.size()).isEqualTo(4);
        assertThatThrownBy(() -> out.reserve(Integer.MAX_VALUE)).isInstanceOf(OutOfMemoryError.class);
    }

    private static void assertContent(ContentStreamProvider provider, String expected) throws IOException {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
        assertThat(bytes(provider.contentAsByteBufferOrNull())).isEqualTo(expectedBytes);
        try (InputStream stream = provider.newStream()) {
            assertThat(IoUtils.toByteArray(stream)).isEqualTo(expectedBytes);
        }
    }

    private static byte[] written(ByteBufferOutputStream out) {
        return Arrays.copyOf(out.array(), out.size());
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] result = new byte[buffer.remaining()];
        buffer.duplicate().get(result);
        return result;
    }
}
