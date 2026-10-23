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

package software.amazon.awssdk.codecs.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.commons.internal.SinkBufferPool;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * A serializer with a {@link ByteBufferOutputStream} sink writes straight into the stream's array; one with another
 * sink writes into a 1 KiB buffer lent by {@link SinkBufferPool} and returns it on close. Both outputs match the
 * sink-less serializer for documents below, at and above 1 KiB and for a value larger than that, including when the
 * lent buffer still holds an earlier, longer document.
 */
class JsonSinkBufferTest {
    private static final JsonCodec CODEC = JsonCodec.builder().build();
    private static final Schema STRINGS = Schema.listBuilder(ShapeId.from("smithy.example#Strings"))
                                                .putMember("member", PreludeSchemas.STRING)
                                                .build();
    private static final Schema DOC = Schema.structureBuilder(ShapeId.from("smithy.example#Doc"))
                                            .putMember("s", PreludeSchemas.STRING)
                                            .putMember("list", STRINGS)
                                            .build();
    private static final OutputStream DISCARD = new OutputStream() {
        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }
    };

    @Test
    void sinkOutput_matchesTheSinkLessSerializer() {
        List<SerializableStruct> docs = new ArrayList<>();
        for (int size : new int[] {10, 1023, 1024, 1025, 5000}) {
            docs.add(docOfSize(size));
        }
        docs.add(doc(repeat('v', 3000), Collections.emptyList()));
        // Non-ASCII and escaped text take the general string encoder.
        docs.add(doc(repeat('\u00e9', 700), Collections.nCopies(20, "\u65e5\u672c\u8a9e \"q\" \\")));
        // Largest first, then smallest first: a lent buffer that held a longer document must not leak into a shorter
        // one.
        List<SerializableStruct> order = new ArrayList<>(docs);
        Collections.reverse(order);
        order.addAll(docs);
        for (SerializableStruct doc : order) {
            byte[] expected = bytes(CODEC.serialize(doc));
            assertThat(viaStream(doc)).as("ByteBufferOutputStream").isEqualTo(expected);
            assertThat(viaOtherSink(doc)).as("other sink").isEqualTo(expected);
        }
    }

    @Test
    void byteBufferOutputStreamSink_isWrittenInPlaceAfterItsContent() {
        SerializableStruct doc = doc("hello", Arrays.asList("a", "b"));
        byte[] expected = bytes(CODEC.serialize(doc));
        ByteBufferOutputStream out = new ByteBufferOutputStream(256);
        out.write(new byte[] {'x', 'y'}, 0, 2);
        byte[] array = out.array();
        try (ShapeSerializer serializer = CODEC.createSerializer(out)) {
            serializer.writeStruct(DOC, doc);
            serializer.flush();
            assertThat(out.size()).isEqualTo(2 + expected.length);
        }
        // Written into the stream's own array, after the two bytes already in it.
        assertThat(out.array()).isSameAs(array);
        assertThat(out.size()).isEqualTo(2 + expected.length);
        assertThat(Arrays.copyOfRange(out.array(), 2, out.size())).isEqualTo(expected);
        assertThat(out.array()[0]).isEqualTo((byte) 'x');
    }

    @Test
    void byteBufferOutputStreamSink_growsTheStream() {
        SerializableStruct doc = docOfSize(5000);
        ByteBufferOutputStream out = new ByteBufferOutputStream(0);
        try (ShapeSerializer serializer = CODEC.createSerializer(out)) {
            serializer.writeStruct(DOC, doc);
        }
        assertThat(Arrays.copyOf(out.array(), out.size())).isEqualTo(bytes(CODEC.serialize(doc)));
    }

    @Test
    void byteBufferOutputStreamSink_allocatesNoBuffer() {
        com.sun.management.ThreadMXBean threads = allocationCounter();
        SerializableStruct doc = doc("hello", Arrays.asList("a", "b"));
        ByteBufferOutputStream out = new ByteBufferOutputStream(256);
        for (int i = 0; i < 200; i++) {
            writeTo(out, doc);
        }
        int rounds = 100;
        long threadId = Thread.currentThread().getId();
        long before = threads.getThreadAllocatedBytes(threadId);
        for (int i = 0; i < rounds; i++) {
            writeTo(out, doc);
        }
        long perDocument = (threads.getThreadAllocatedBytes(threadId) - before) / rounds;

        // Only the serializer's own objects; no scratch buffer and no copy of the body.
        assertThat(perDocument).isLessThan(SinkBufferPool.BUFFER_SIZE / 2);
    }

    private static void writeTo(ByteBufferOutputStream out, SerializableStruct doc) {
        out.reset();
        try (ShapeSerializer serializer = CODEC.createSerializer(out)) {
            serializer.writeStruct(DOC, doc);
        }
    }

    @Test
    void closeTwice_returnsTheBufferOnce() {
        List<byte[]> held = drainPool();
        try {
            ShapeSerializer serializer = CODEC.createSerializer(new ByteArrayOutputStream());
            serializer.writeStruct(DOC, doc("a", Collections.emptyList()));
            serializer.close();
            serializer.close();
            byte[] first = SinkBufferPool.acquire();
            byte[] second = SinkBufferPool.acquire();
            assertThat(first).isNotSameAs(second);
            held.add(first);
            held.add(second);
        } finally {
            held.forEach(SinkBufferPool::release);
        }
    }

    @Test
    void writeAfterClose_failsWithIllegalStateException() {
        for (OutputStream sink : Arrays.asList(new ByteBufferOutputStream(), new ByteArrayOutputStream())) {
            ShapeSerializer serializer = CODEC.createSerializer(sink);
            serializer.writeStruct(DOC, doc("a", Collections.emptyList()));
            serializer.close();
            // A second close and a flush after close do nothing.
            serializer.close();
            serializer.flush();
            assertThatThrownBy(() -> serializer.writeStruct(DOC, doc("b", Collections.emptyList())))
                .as(sink.getClass().getSimpleName())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
        }
    }

    // The caller writes or resets the sink while the serializer is open. A ByteBufferOutputStream sink, written in
    // place, must give the bytes a buffering sink (ByteArrayOutputStream) gives.

    @Test
    void otherWriteBeforeTheFirstValue_isKeptBeforeTheDocument() {
        assertSameAsBufferedSink(16, (serializer, out) -> {
            write(out, "prefix");
            serializer.writeStruct(DOC, doc("first", Arrays.asList("a", "b")));
            serializer.close();
        });
    }

    @Test
    void otherWritesAroundTopLevelValuesThatAreNotStructures_areKeptInOrder() {
        // Only writeStruct takes the stream's size up front; any other first write takes it in grow.
        assertSameAsBufferedSink(16, (serializer, out) -> {
            write(out, "prefix");
            serializer.writeString(PreludeSchemas.STRING, "first");
            serializer.flush();
            write(out, "middle");
            serializer.writeString(PreludeSchemas.STRING, "second");
            serializer.close();
        });
    }

    @Test
    void flushThenOtherWriteThenClose_keepsTheOtherBytesAfterTheDocument() {
        assertSameAsBufferedSink(16, (serializer, out) -> {
            serializer.writeStruct(DOC, doc("first", Arrays.asList("a", "b")));
            serializer.flush();
            write(out, "trailer");
            serializer.close();
        });
    }

    @Test
    void flushThenOtherWriteThenMoreWrites_appendsAfterTheOtherBytes() {
        assertSameAsBufferedSink(16, (serializer, out) -> {
            serializer.writeStruct(DOC, doc("first", Arrays.asList("a", "b")));
            serializer.flush();
            write(out, "middle");
            serializer.writeStruct(DOC, doc("second", Collections.singletonList("c")));
            serializer.close();
        });
    }

    @Test
    void flushThenOtherWriteThatGrowsTheStream_appendsAfterTheOtherBytes() {
        assertSameAsBufferedSink(16, (serializer, out) -> {
            serializer.writeStruct(DOC, doc("first", Collections.emptyList()));
            serializer.flush();
            write(out, repeat('m', 3000));
            serializer.writeStruct(DOC, doc("second", Collections.emptyList()));
            serializer.close();
        });
    }

    @Test
    void flushThenReset_continuesFromTheEmptyStream() {
        assertSameAsBufferedSink(16, (serializer, out) -> {
            serializer.writeStruct(DOC, doc("first", Arrays.asList("a", "b")));
            serializer.flush();
            reset(out);
            serializer.writeStruct(DOC, doc("second", Collections.singletonList("c")));
            serializer.close();
        });
    }

    @Test
    void flushThenSharedReset_continuesInTheNewArray() {
        ByteBufferOutputStream out = new ByteBufferOutputStream(16);
        ShapeSerializer serializer = CODEC.createSerializer(out);
        serializer.writeStruct(DOC, doc("first", Arrays.asList("a", "b")));
        serializer.flush();
        ByteBuffer shared = out.toByteBuffer();
        byte[] sharedBytes = bytes(shared);
        out.reset();
        SerializableStruct second = doc("second", Collections.singletonList("c"));
        serializer.writeStruct(DOC, second);
        serializer.close();
        assertThat(Arrays.copyOf(out.array(), out.size())).isEqualTo(bytes(CODEC.serialize(second)));
        // The content handed out before the reset is unchanged.
        assertThat(bytes(shared)).isEqualTo(sharedBytes);
    }

    @Test
    void unflushedThenOtherWriteThatGrowsTheStream_appendsTheIntactBytes() {
        // The other write moves the stream to a new array, so the serializer's bytes in the old one are intact.
        assertSameAsBufferedSink(256, (serializer, out) -> {
            serializer.writeStruct(DOC, doc("first", Collections.emptyList()));
            write(out, repeat('m', 3000));
            serializer.close();
        });
    }

    @Test
    void unflushedThenSharedReset_appendsTheIntactBytes() {
        ByteBufferOutputStream out = new ByteBufferOutputStream(64);
        write(out, "xy");
        out.toByteBuffer();
        SerializableStruct first = doc("first", Collections.emptyList());
        ShapeSerializer serializer = CODEC.createSerializer(out);
        serializer.writeStruct(DOC, first);
        out.reset();
        serializer.close();
        assertThat(Arrays.copyOf(out.array(), out.size())).isEqualTo(bytes(CODEC.serialize(first)));
    }

    @Test
    void unflushedThenReset_appendsTheIntactBytes() {
        assertSameAsBufferedSink(256, (serializer, out) -> {
            write(out, "prefix");
            serializer.writeStruct(DOC, doc("first", Collections.emptyList()));
            reset(out);
            write(out, "xy");
            serializer.close();
        });
    }

    @Test
    void unflushedThenOtherWriteOverTheBytes_failsInsteadOfCorruptingTheBody() {
        ByteBufferOutputStream out = new ByteBufferOutputStream(256);
        ShapeSerializer serializer = CODEC.createSerializer(out);
        serializer.writeStruct(DOC, doc("first", Arrays.asList("a", "b")));
        // Written into the stream's array where the serializer's bytes are.
        write(out, "over");
        assertThatThrownBy(serializer::close).isInstanceOf(IllegalStateException.class)
                                             .hasMessageContaining("flush()");
        assertThat(new String(out.array(), 0, out.size(), StandardCharsets.UTF_8)).isEqualTo("over");
    }

    private interface Interleaving {
        void run(ShapeSerializer serializer, OutputStream out);
    }

    // capacity: the in-place stream's initial capacity. The unflushed cases use one the document fits in, so the
    // serializer adds all of its bytes at close(), as the buffering sink writes all of them there.
    private static void assertSameAsBufferedSink(int capacity, Interleaving interleaving) {
        ByteArrayOutputStream buffered = new ByteArrayOutputStream();
        interleaving.run(CODEC.createSerializer(buffered), buffered);
        ByteBufferOutputStream inPlace = new ByteBufferOutputStream(capacity);
        interleaving.run(CODEC.createSerializer(inPlace), inPlace);
        assertThat(Arrays.copyOf(inPlace.array(), inPlace.size())).isEqualTo(buffered.toByteArray());
    }

    private static void write(OutputStream out, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        try {
            out.write(bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void reset(OutputStream out) {
        if (out instanceof ByteBufferOutputStream) {
            ((ByteBufferOutputStream) out).reset();
        } else {
            ((ByteArrayOutputStream) out).reset();
        }
    }

    @Test
    void otherSink_smallDocument_doesNotAllocateTheSinkBuffer() {
        com.sun.management.ThreadMXBean threads = allocationCounter();
        SerializableStruct doc = doc("hello", Arrays.asList("a", "b"));
        for (int i = 0; i < 200; i++) {
            writeToDiscard(doc);
        }
        int rounds = 100;
        long threadId = Thread.currentThread().getId();
        long before = threads.getThreadAllocatedBytes(threadId);
        for (int i = 0; i < rounds; i++) {
            writeToDiscard(doc);
        }
        long perDocument = (threads.getThreadAllocatedBytes(threadId) - before) / rounds;

        // Allocating the 1 KiB buffer per serializer costs at least 1,040 B (array header included).
        assertThat(perDocument).isLessThan(SinkBufferPool.BUFFER_SIZE);
    }

    private static void writeToDiscard(SerializableStruct doc) {
        try (ShapeSerializer serializer = CODEC.createSerializer(DISCARD)) {
            serializer.writeStruct(DOC, doc);
        }
    }

    private static byte[] viaStream(SerializableStruct doc) {
        ByteBufferOutputStream out = new ByteBufferOutputStream(16);
        try (ShapeSerializer serializer = CODEC.createSerializer(out)) {
            serializer.writeStruct(DOC, doc);
        }
        return Arrays.copyOf(out.array(), out.size());
    }

    private static byte[] viaOtherSink(SerializableStruct doc) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(16);
        try (ShapeSerializer serializer = CODEC.createSerializer(out)) {
            serializer.writeStruct(DOC, doc);
        }
        return out.toByteArray();
    }

    /**
     * A document of exactly {@code size} bytes: a list of short strings (none below 64 bytes), padded with the
     * {@code s} member.
     */
    private static SerializableStruct docOfSize(int size) {
        List<String> list = size < 64 ? null : Collections.nCopies((size - 40) / 13, "0123456789");
        int base = CODEC.serialize(doc("", list)).remaining();
        SerializableStruct doc = doc(repeat('p', size - base), list);
        assertThat(CODEC.serialize(doc).remaining()).isEqualTo(size);
        return doc;
    }

    private static SerializableStruct doc(String s, List<String> list) {
        return new SerializableStruct() {
            @Override
            public Schema sdkSchema() {
                return DOC;
            }

            @Override
            public void serializeMembers(ShapeSerializer serializer) {
                serializer.writeString(DOC.member("s"), s);
                if (list == null) {
                    return;
                }
                serializer.writeList(DOC.member("list"), list, list.size(), (values, elements) -> {
                    for (String value : values) {
                        elements.writeString(STRINGS.listMember(), value);
                    }
                });
            }

            @Override
            public <T> T getMemberValue(Schema member) {
                return null;
            }
        };
    }

    private static List<byte[]> drainPool() {
        List<byte[]> held = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            held.add(SinkBufferPool.acquire());
        }
        return held;
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static com.sun.management.ThreadMXBean allocationCounter() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled());
        return threads;
    }
}
