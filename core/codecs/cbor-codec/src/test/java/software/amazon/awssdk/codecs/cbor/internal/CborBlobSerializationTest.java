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

package software.amazon.awssdk.codecs.cbor.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codecs.cbor.CborSettings;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * A blob written from a {@code byte[]} (as generated models write blob members) gives the same CBOR bytes as the same
 * blob written from every kind of {@link ByteBuffer}: at the top level, as a structure member, a list element and a map
 * value (FU-12).
 */
class CborBlobSerializationTest {
    private static final CborSettings SETTINGS = CborSettings.defaultSettings();
    private static final DefaultCborSerdeProvider CODEC = new DefaultCborSerdeProvider();

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

    static IntStream lengths() {
        // The byte string length header boundaries (1, 2, 3 and 5 bytes) and seeded random lengths up to 10,000.
        return IntStream.concat(IntStream.of(0, 1, 23, 24, 255, 256, 65_535, 65_536),
                                new Random(12).ints(24, 0, 10_001));
    }

    @ParameterizedTest
    @MethodSource("lengths")
    void topLevelBlob_writesTheSameBytesFromEveryBlobForm(int length) {
        byte[] bytes = randomBytes(length);
        byte[] expected = concat(byteStringHeader(length), bytes);

        assertThat(serialize(s -> s.writeBlob(PreludeSchemas.BLOB, bytes))).isEqualTo(expected);
        for (Map.Entry<String, ByteBuffer> entry : buffers(bytes).entrySet()) {
            ByteBuffer buffer = entry.getValue();
            int position = buffer.position();
            int limit = buffer.limit();
            assertThat(serialize(s -> s.writeBlob(PreludeSchemas.BLOB, buffer))).as(entry.getKey()).isEqualTo(expected);
            assertThat(buffer.position()).as(entry.getKey()).isEqualTo(position);
            assertThat(buffer.limit()).as(entry.getKey()).isEqualTo(limit);
        }
    }

    @ParameterizedTest
    @MethodSource("lengths")
    void memberListElementAndMapValue_writeTheSameBytesFromEveryBlobForm(int length) {
        byte[] bytes = randomBytes(length);
        byte[] original = bytes.clone();
        // Reference: the ByteBuffer path over the whole array, which this change does not touch.
        ByteBuffer whole = ByteBuffer.wrap(bytes.clone());
        byte[] expected = serialize(s -> s.writeStruct(BLOBS, blobs((w, schema) -> w.writeBlob(schema, whole))));

        assertThat(serialize(s -> s.writeStruct(BLOBS, blobs((w, schema) -> w.writeBlob(schema, bytes)))))
            .isEqualTo(expected);
        assertThat(bytes).isEqualTo(original);
        for (Map.Entry<String, ByteBuffer> entry : buffers(bytes).entrySet()) {
            ByteBuffer buffer = entry.getValue();
            int position = buffer.position();
            int limit = buffer.limit();
            assertThat(serialize(s -> s.writeStruct(BLOBS, blobs((w, schema) -> w.writeBlob(schema, buffer)))))
                .as(entry.getKey())
                .isEqualTo(expected);
            assertThat(buffer.position()).as(entry.getKey()).isEqualTo(position);
            assertThat(buffer.limit()).as(entry.getKey()).isEqualTo(limit);
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

    private static byte[] serialize(Consumer<ShapeSerializer> write) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ShapeSerializer serializer = CODEC.newSerializer(output, SETTINGS)) {
            write.accept(serializer);
        }
        return output.toByteArray();
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

    /**
     * The RFC 8949 header of a byte string (major type 2) of {@code length} bytes, in its shortest form.
     */
    private static byte[] byteStringHeader(int length) {
        if (length < 24) {
            return new byte[] {(byte) (0x40 | length)};
        }
        if (length < 0x100) {
            return new byte[] {0x58, (byte) length};
        }
        if (length < 0x10000) {
            return new byte[] {0x59, (byte) (length >>> 8), (byte) length};
        }
        return new byte[] {0x5a, (byte) (length >>> 24), (byte) (length >>> 16), (byte) (length >>> 8), (byte) length};
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random(length).nextBytes(bytes);
        return bytes;
    }
}
