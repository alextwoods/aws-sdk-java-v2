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

package software.amazon.awssdk.protocols.client.awsquery.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Blob members, list elements and map values written from a {@code byte[]} (as generated models write them) and from
 * every kind of {@link ByteBuffer} give the same form body: {@link Base64} standard encoding, percent-encoded. The
 * {@code byte[]} path encodes without copying the bytes (FU-12).
 */
class QueryBlobSerializationTest {
    private static final String PREFIX = "Action=Op&Version=2020-01-08";

    private static final Schema BLOB_LIST = Schema.listBuilder(ShapeId.from("smithy.example#BlobList"))
                                                  .putMember("member", PreludeSchemas.BLOB)
                                                  .build();
    private static final Schema BLOB_MAP = Schema.mapBuilder(ShapeId.from("smithy.example#BlobMap"))
                                                 .putMember("key", PreludeSchemas.STRING)
                                                 .putMember("value", PreludeSchemas.BLOB)
                                                 .build();
    private static final Schema BLOBS = Schema.structureBuilder(ShapeId.from("smithy.example#Blobs"))
                                              .putMember("Data", PreludeSchemas.BLOB)
                                              .putMember("List", BLOB_LIST)
                                              .putMember("Map", BLOB_MAP)
                                              .build();

    static IntStream lengths() {
        // Base64 padding boundaries, the benchmark blob sizes, and seeded random lengths up to 10,000.
        return IntStream.concat(IntStream.of(0, 1, 2, 3, 4, 54, 432, 2160, 10_000),
                                new Random(12).ints(24, 0, 10_001));
    }

    @ParameterizedTest
    @MethodSource("lengths")
    void everyBlobForm_writesPercentEncodedStandardBase64(int length) {
        byte[] bytes = randomBytes(length);
        byte[] original = bytes.clone();
        // Base64 text is unreserved except for '+', '/' and '='.
        String encoded = Base64.getEncoder().encodeToString(bytes)
                               .replace("+", "%2B").replace("/", "%2F").replace("=", "%3D");
        String expected = PREFIX + "&Data=" + encoded + "&List.member.1=" + encoded + "&List.member.2=" + encoded
                          + "&Map.entry.1.key=k&Map.entry.1.value=" + encoded;

        assertThat(awsQuery(blobs((s, schema) -> s.writeBlob(schema, bytes)))).isEqualTo(expected);
        assertThat(bytes).isEqualTo(original);

        for (Map.Entry<String, ByteBuffer> entry : buffers(bytes).entrySet()) {
            ByteBuffer buffer = entry.getValue();
            int position = buffer.position();
            int limit = buffer.limit();
            assertThat(awsQuery(blobs((s, schema) -> s.writeBlob(schema, buffer)))).as(entry.getKey())
                                                                                   .isEqualTo(expected);
            assertThat(buffer.position()).as(entry.getKey()).isEqualTo(position);
            assertThat(buffer.limit()).as(entry.getKey()).isEqualTo(limit);
        }
    }

    @Test
    void topLevelByteArrayBlob_isWrittenAsAMember() {
        // Base64 "+/8=".
        byte[] bytes = {(byte) 0xfb, (byte) 0xff};
        QueryFormSerializer serializer = QueryFormSerializer.forAwsQuery("Op", "2020-01-08");

        serializer.writeBlob(BLOBS.member("Data"), bytes);

        assertThat(finish(serializer)).isEqualTo(PREFIX + "&Data=%2B%2F8%3D");
    }

    @Test
    void byteArrayBlobs_areEncodedWithoutCopyingTheBytes() {
        com.sun.management.ThreadMXBean threads = allocationCounter();
        byte[] bytes = randomBytes(256 * 1024);
        ByteBuffer readOnly = ByteBuffer.wrap(bytes).asReadOnlyBuffer();

        long fromArray = allocatedBySecondBody(threads, blobs((s, schema) -> s.writeBlob(schema, bytes)));
        long fromReadOnly = allocatedBySecondBody(threads, blobs((s, schema) -> s.writeBlob(schema, readOnly)));

        // Four blob writes of 256 KiB. Both paths build the encoded bytes before percent-encoding them; the read-only
        // buffer has no accessible array, so the JDK encoder also copies each blob first.
        assertThat(fromReadOnly - fromArray).isGreaterThan(4L * bytes.length * 9 / 10);
    }

    private static SerializableStruct blobs(BiConsumer<ShapeSerializer, Schema> writeBlob) {
        return new SerializableStruct() {
            @Override
            public Schema sdkSchema() {
                return BLOBS;
            }

            @Override
            public void serializeMembers(ShapeSerializer serializer) {
                writeBlob.accept(serializer, BLOBS.member("Data"));
                serializer.writeList(BLOBS.member("List"), writeBlob, 2, (w, list) -> {
                    w.accept(list, BLOB_LIST.listMember());
                    w.accept(list, BLOB_LIST.listMember());
                });
                serializer.writeMap(BLOBS.member("Map"), writeBlob, 1, (w, map) -> map.writeEntry(
                    BLOB_MAP.mapKeyMember(), "k", w, (v, value) -> v.accept(value, BLOB_MAP.mapValueMember())));
            }

            @Override
            public <T> T getMemberValue(Schema member) {
                return null;
            }
        };
    }

    private static String awsQuery(SerializableStruct input) {
        QueryFormSerializer serializer = QueryFormSerializer.forAwsQuery("Op", "2020-01-08");
        input.serializeMembers(serializer);
        return finish(serializer);
    }

    private static String finish(QueryFormSerializer serializer) {
        ByteBuffer body = serializer.finish();
        byte[] bytes = new byte[body.remaining()];
        body.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
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

    private static long allocatedBySecondBody(com.sun.management.ThreadMXBean threads, SerializableStruct input) {
        // The first body loads and initializes the classes involved; the second is measured. Each serializer grows
        // its own buffer, by the same amount on both paths.
        awsQuery(input);
        long threadId = Thread.currentThread().getId();
        long before = threads.getThreadAllocatedBytes(threadId);
        QueryFormSerializer serializer = QueryFormSerializer.forAwsQuery("Op", "2020-01-08");
        input.serializeMembers(serializer);
        serializer.finish();
        return threads.getThreadAllocatedBytes(threadId) - before;
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
