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

package software.amazon.awssdk.httpbinding.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.utils.IoUtils;

class ByteBufferContentStreamProviderTest {
    private static final byte[] HELLO = utf8("hello");

    @Test
    void create_whenBufferIsReadOnly_sendsTheCallersBytesWithoutCopying() throws IOException {
        // SdkBytes.asByteBuffer() over a slice gives this kind of view: read-only, so it has no accessible array.
        byte[] arr = utf8("xhellox");
        ByteBuffer data = ByteBuffer.wrap(arr, 1, 5).slice().asReadOnlyBuffer();
        ByteBufferContentStreamProvider provider = ByteBufferContentStreamProvider.create(data);

        arr[1] = 'j';

        assertThat(read(provider.newStream())).isEqualTo("jello");
        assertThat(utf8(provider.contentAsByteBufferOrNull())).isEqualTo("jello");
    }

    @Test
    void contentAsByteBufferOrNull_whenBufferIsWritableHeap_sharesTheCallersArray() {
        byte[] arr = utf8("xxhelxx");
        ByteBuffer view = ByteBufferContentStreamProvider.create(ByteBuffer.wrap(arr, 2, 3)).contentAsByteBufferOrNull();

        assertThat(view.array()).isSameAs(arr);
        assertThat(view.arrayOffset() + view.position()).isEqualTo(2);
        assertThat(view.remaining()).isEqualTo(3);
        assertThat(view.isReadOnly()).isFalse();
    }

    @Test
    void contentAsByteBufferOrNull_whenBufferIsReadOnly_staysReadOnly() {
        ByteBuffer view = ByteBufferContentStreamProvider.create(ByteBuffer.wrap(utf8("hello")).asReadOnlyBuffer())
                                                         .contentAsByteBufferOrNull();

        assertThat(view.isReadOnly()).isTrue();
        assertThat(view.hasArray()).isFalse();
    }

    @Test
    void contentAsByteBufferOrNull_whenCalledTwice_returnsIndependentViews() {
        ByteBuffer data = ByteBuffer.wrap(utf8("hello"));
        ByteBufferContentStreamProvider provider = ByteBufferContentStreamProvider.create(data);

        ByteBuffer first = provider.contentAsByteBufferOrNull();
        ByteBuffer second = provider.contentAsByteBufferOrNull();
        first.position(first.limit());

        assertThat(second).isNotSameAs(first);
        assertThat(second.remaining()).isEqualTo(5);
        assertThat(provider.contentAsByteBufferOrNull().remaining()).isEqualTo(5);
        assertThat(data.remaining()).isEqualTo(5);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    void newStream_whenCalledTwice_returnsTheWholeBodyEachTime(String kind, ByteBuffer data) throws IOException {
        ByteBufferContentStreamProvider provider = ByteBufferContentStreamProvider.create(data);

        assertThat(read(provider.newStream())).isEqualTo("hello");
        assertThat(read(provider.newStream())).isEqualTo("hello");
        assertThat(provider.length()).isEqualTo(5);
        assertThat(data.remaining()).isEqualTo(5);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("buffers")
    void newStream_whenReset_returnsToTheMarkOrTheStart(String kind, ByteBuffer data) throws IOException {
        InputStream stream = ByteBufferContentStreamProvider.create(data).newStream();

        assertThat(stream.markSupported()).isTrue();
        assertThat(stream.read()).isEqualTo('h');
        stream.reset();
        assertThat(stream.read()).isEqualTo('h');
        stream.mark(100);
        assertThat(stream.read()).isEqualTo('e');
        stream.reset();
        assertThat(stream.read()).isEqualTo('e');
        assertThat(stream.available()).isEqualTo(3);
        assertThat(stream.skip(1)).isEqualTo(1);
        assertThat(stream.skip(0)).isZero();
        assertThat(stream.read(new byte[4], 1, 0)).isZero();
        byte[] rest = new byte[4];
        assertThat(stream.read(rest, 1, 3)).isEqualTo(2);
        assertThat(new String(rest, 1, 2, StandardCharsets.UTF_8)).isEqualTo("lo");
        assertThat(stream.read()).isEqualTo(-1);
        assertThat(stream.read(rest, 0, 1)).isEqualTo(-1);
        assertThat(stream.skip(10)).isZero();
        assertThat(stream.available()).isZero();
    }

    @Test
    void create_whenCallerMovesPositionOrLimitLater_keepsTheBodyCapturedAtCreation() throws IOException {
        ByteBuffer data = ByteBuffer.wrap(utf8("xhellox"), 1, 5);
        ByteBufferContentStreamProvider provider = ByteBufferContentStreamProvider.create(data);

        data.position(3).limit(4);

        assertThat(provider.length()).isEqualTo(5);
        assertThat(read(provider.newStream())).isEqualTo("hello");
        assertThat(utf8(provider.contentAsByteBufferOrNull())).isEqualTo("hello");
    }

    @Test
    void create_whenBufferIsEmpty_hasNoBytes() throws IOException {
        ByteBufferContentStreamProvider provider = ByteBufferContentStreamProvider.create(ByteBuffer.allocate(0));

        assertThat(provider.length()).isZero();
        assertThat(provider.newStream().read()).isEqualTo(-1);
        assertThat(provider.contentAsByteBufferOrNull().remaining()).isZero();
    }

    @Test
    void name_isBytes() {
        assertThat(ByteBufferContentStreamProvider.create(ByteBuffer.wrap(HELLO)).name()).isEqualTo("Bytes");
    }

    static Stream<Arguments> buffers() {
        ByteBuffer direct = ByteBuffer.allocateDirect(5);
        direct.put(HELLO);
        direct.flip();
        return Stream.of(Arguments.of("exact heap", ByteBuffer.wrap(utf8("hello"))),
                         Arguments.of("sliced heap", ByteBuffer.wrap(utf8("xhellox"), 1, 5)),
                         Arguments.of("read-only", ByteBuffer.wrap(utf8("xhellox"), 1, 5).slice().asReadOnlyBuffer()),
                         Arguments.of("direct", direct));
    }

    private static String read(InputStream stream) throws IOException {
        return IoUtils.toUtf8String(stream);
    }

    private static String utf8(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
