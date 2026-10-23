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

package software.amazon.awssdk.codecs.cbor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
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
class CborSinkBufferTest {
    private static final Rpcv2CborCodec CODEC = Rpcv2CborCodec.builder().build();
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
        // Non-ASCII text takes the backpatching encoder.
        docs.add(doc(repeat('\u00e9', 700), Collections.nCopies(20, "\u65e5\u672c\u8a9e")));
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
    void writeAfterClose_fails() {
        for (OutputStream sink : Arrays.asList(new ByteBufferOutputStream(), new ByteArrayOutputStream())) {
            ShapeSerializer serializer = CODEC.createSerializer(sink);
            serializer.writeStruct(DOC, doc("a", Collections.emptyList()));
            serializer.close();
            assertThatThrownBy(() -> serializer.writeStruct(DOC, doc("b", Collections.emptyList())))
                .as(sink.getClass().getSimpleName())
                .isInstanceOf(RuntimeException.class);
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
     * {@code s} member. The length header of {@code s} grows at 24 and 256 bytes, so the padding is searched.
     */
    private static SerializableStruct docOfSize(int size) {
        for (int items = Math.max(0, (size - 40) / 11); items >= 0; items--) {
            List<String> list = size < 64 ? null : Collections.nCopies(items, "0123456789");
            for (int pad = 0; pad <= size; pad++) {
                SerializableStruct doc = doc(repeat('p', pad), list);
                int actual = CODEC.serialize(doc).remaining();
                if (actual == size) {
                    return doc;
                }
                if (actual > size) {
                    break;
                }
            }
        }
        throw new AssertionError("No document of " + size + " bytes");
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
