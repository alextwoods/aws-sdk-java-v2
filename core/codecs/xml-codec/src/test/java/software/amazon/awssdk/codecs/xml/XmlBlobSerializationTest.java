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

package software.amazon.awssdk.codecs.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Blob members, list elements and map values written from a {@code byte[]} (as generated models write them) and from
 * every kind of {@link ByteBuffer} produce {@link Base64} standard encoding with both XML implementations, and the
 * {@code byte[]} path encodes without copying the bytes (FU-12).
 */
class XmlBlobSerializationTest extends ProviderTestBase {
    private static final Schema BLOB_LIST = Schema.listBuilder(ShapeId.from("smithy.example#BlobList"))
                                                  .putMember("member", PreludeSchemas.BLOB)
                                                  .build();
    private static final Schema BLOB_MAP = Schema.mapBuilder(ShapeId.from("smithy.example#BlobMap"))
                                                 .putMember("key", PreludeSchemas.STRING)
                                                 .putMember("value", PreludeSchemas.BLOB)
                                                 .build();
    private static final Schema BLOBS = Schema.structureBuilder(ShapeId.from("smithy.example#Blobs"))
                                              .putMember("data", PreludeSchemas.BLOB)
                                              .putMember("list", BLOB_LIST)
                                              .putMember("map", BLOB_MAP)
                                              .build();

    static Stream<Arguments> providersAndLengths() {
        // Base64 padding boundaries, the benchmark blob sizes, and seeded random lengths up to 10,000.
        int[] lengths = IntStream.concat(IntStream.of(0, 1, 2, 3, 4, 54, 432, 2160, 10_000),
                                         new Random(12).ints(24, 0, 10_001))
                                 .toArray();
        return Stream.of(Named.of("stax", STAX), Named.of("native", NATIVE))
                     .flatMap(provider -> IntStream.of(lengths).mapToObj(length -> Arguments.of(provider, length)));
    }

    @ParameterizedTest
    @MethodSource("providersAndLengths")
    void everyBlobForm_writesStandardBase64(boolean useNative, int length) {
        byte[] bytes = randomBytes(length);
        byte[] original = bytes.clone();
        String encoded = Base64.getEncoder().encodeToString(bytes);
        String expected = "<Blobs><data>" + encoded + "</data><list><member>" + encoded + "</member><member>" + encoded
                          + "</member></list><map><entry><key>k</key><value>" + encoded + "</value></entry></map></Blobs>";

        try (XmlCodec codec = codec(useNative)) {
            assertThat(serialize(codec, blobs((s, schema) -> s.writeBlob(schema, bytes)))).isEqualTo(expected);
            assertThat(bytes).isEqualTo(original);

            for (Map.Entry<String, ByteBuffer> entry : buffers(bytes).entrySet()) {
                ByteBuffer buffer = entry.getValue();
                int position = buffer.position();
                int limit = buffer.limit();
                assertThat(serialize(codec, blobs((s, schema) -> s.writeBlob(schema, buffer)))).as(entry.getKey())
                                                                                              .isEqualTo(expected);
                assertThat(buffer.position()).as(entry.getKey()).isEqualTo(position);
                assertThat(buffer.limit()).as(entry.getKey()).isEqualTo(limit);
            }
        }
    }

    @PerProvider
    void byteArrayBlobs_areEncodedWithoutCopyingTheBytes(boolean useNative) {
        com.sun.management.ThreadMXBean threads = allocationCounter();
        byte[] bytes = randomBytes(256 * 1024);
        ByteBuffer readOnly = ByteBuffer.wrap(bytes).asReadOnlyBuffer();

        try (XmlCodec codec = codec(useNative)) {
            long fromArray = allocatedBySecondWrite(threads, codec, blobs((s, schema) -> s.writeBlob(schema, bytes)));
            long fromReadOnly = allocatedBySecondWrite(threads, codec,
                                                       blobs((s, schema) -> s.writeBlob(schema, readOnly)));

            // Four blob writes of 256 KiB. The read-only buffer has no accessible array, so the JDK encoder copies
            // each blob first; the array path does not. StAX also builds the encoded text as a String, on both paths.
            assertThat(fromReadOnly - fromArray).isGreaterThan(4L * bytes.length * 9 / 10);
            if (useNative) {
                assertThat(fromArray).isLessThan(64 * 1024);
            }
        }
    }

    private static SerializableStruct blobs(BiConsumer<ShapeSerializer, Schema> writeBlob) {
        return new SerializableStruct() {
            @Override
            public Schema sdkSchema() {
                return BLOBS;
            }

            @Override
            public void serializeMembers(ShapeSerializer serializer) {
                writeBlob.accept(serializer, BLOBS.member("data"));
                serializer.writeList(BLOBS.member("list"), writeBlob, 2, (w, list) -> {
                    w.accept(list, BLOB_LIST.listMember());
                    w.accept(list, BLOB_LIST.listMember());
                });
                serializer.writeMap(BLOBS.member("map"), writeBlob, 1, (w, map) -> map.writeEntry(
                    BLOB_MAP.mapKeyMember(), "k", w, (v, value) -> v.accept(value, BLOB_MAP.mapValueMember())));
            }

            @Override
            public <T> T getMemberValue(Schema member) {
                return null;
            }
        };
    }

    private static String serialize(XmlCodec codec, SerializableStruct struct) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ShapeSerializer serializer = codec.createSerializer(output)) {
            serializer.writeStruct(BLOBS, struct);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Every kind of buffer a caller can pass, each holding {@code bytes} from its position to its limit.
     */
    private static Map<String, ByteBuffer> buffers(byte[] bytes) {
        Map<String, ByteBuffer> buffers = new LinkedHashMap<>();
        buffers.put("writable, whole array", ByteBuffer.wrap(bytes.clone()));
        byte[] padded = new byte[bytes.length + 5];
        System.arraycopy(bytes, 0, padded, 2, bytes.length);
        ByteBuffer positioned = ByteBuffer.wrap(padded, 2, bytes.length);
        buffers.put("writable, non-zero position", positioned);
        buffers.put("writable, non-zero array offset", positioned.slice());
        buffers.put("read-only, whole array", ByteBuffer.wrap(bytes.clone()).asReadOnlyBuffer());
        buffers.put("read-only, non-zero position", positioned.asReadOnlyBuffer());
        ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length + 3);
        direct.position(1);
        direct.put(bytes);
        direct.limit(direct.position());
        direct.position(1);
        buffers.put("direct, non-zero position", direct);
        return buffers;
    }

    private static long allocatedBySecondWrite(com.sun.management.ThreadMXBean threads, XmlCodec codec,
                                               SerializableStruct struct) {
        OutputStream discard = new OutputStream() {
            @Override
            public void write(int b) {
            }

            @Override
            public void write(byte[] b, int off, int len) {
            }
        };
        try (ShapeSerializer serializer = codec.createSerializer(discard)) {
            // The first write sizes the serializer's buffers; the second is measured.
            serializer.writeStruct(BLOBS, struct);
            serializer.flush();
            long threadId = Thread.currentThread().getId();
            long before = threads.getThreadAllocatedBytes(threadId);
            serializer.writeStruct(BLOBS, struct);
            serializer.flush();
            return threads.getThreadAllocatedBytes(threadId) - before;
        }
    }

    private static com.sun.management.ThreadMXBean allocationCounter() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled());
        return threads;
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random(length).nextBytes(bytes);
        return bytes;
    }
}
