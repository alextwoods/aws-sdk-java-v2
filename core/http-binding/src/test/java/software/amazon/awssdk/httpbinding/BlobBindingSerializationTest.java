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

package software.amazon.awssdk.httpbinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static software.amazon.awssdk.httpbinding.RequestSerializerTest.body;
import static software.amazon.awssdk.httpbinding.RequestSerializerTest.serialize;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.HttpQueryTrait;
import software.amazon.awssdk.schema.traits.HttpTrait;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Blob {@code @httpHeader} and {@code @httpQuery} members and list elements, and blob body members, written from a
 * {@code byte[]} (as generated models write them) and from every kind of {@link ByteBuffer} give the same request as the
 * read-only {@code SdkBytes} view did, with {@link Base64} standard encoding (an empty blob also keeps today's handling);
 * a {@code byte[]} {@code @httpPayload} is sent as a read-only view of the same array.
 * The text bindings encode the array without copying it (FU-12).
 */
class BlobBindingSerializationTest {
    private static final Schema BLOB_LIST = Schema.listBuilder(TestModel.id("BlobList"))
                                                  .putMember("member", PreludeSchemas.BLOB)
                                                  .build();
    private static final Schema BLOB_BINDINGS =
        Schema.structureBuilder(TestModel.id("BlobBindings"))
              .putMember("header", PreludeSchemas.BLOB, new HttpHeaderTrait("X-Blob"))
              .putMember("query", PreludeSchemas.BLOB, new HttpQueryTrait("b"))
              .putMember("headerList", BLOB_LIST, new HttpHeaderTrait("X-Blobs"))
              .putMember("queryList", BLOB_LIST, new HttpQueryTrait("bs"))
              .putMember("body", PreludeSchemas.BLOB)
              .build();
    private static final ApiOperation<SerializableStruct, SerializableStruct> BLOB_BINDINGS_OPERATION =
        TestModel.operation("BlobBindings", new HttpTrait("POST", "/blobs"), BLOB_BINDINGS, BLOB_BINDINGS);

    static IntStream lengths() {
        // Base64 padding boundaries, the benchmark blob sizes, and seeded random lengths up to 10,000.
        return IntStream.concat(IntStream.of(0, 1, 2, 3, 4, 54, 432, 2160, 10_000),
                                new Random(12).ints(24, 0, 10_001));
    }

    @ParameterizedTest
    @MethodSource("lengths")
    void everyBlobForm_writesStandardBase64InEveryBinding(int length) throws IOException {
        byte[] bytes = randomBytes(length);
        byte[] original = bytes.clone();
        // Reference: the read-only view SdkBytes.asByteBuffer() gives, which generated models passed before FU-12.
        ByteBuffer sdkBytesView = ByteBuffer.wrap(bytes.clone()).asReadOnlyBuffer();
        SdkHttpFullRequest reference = serialize(BLOB_BINDINGS_OPERATION,
                                                 bindings((s, schema) -> s.writeBlob(schema, sdkBytesView)));
        if (length > 0) {
            assertBindings(reference, Base64.getEncoder().encodeToString(bytes));
        }

        assertSameRequest(serialize(BLOB_BINDINGS_OPERATION, bindings((s, schema) -> s.writeBlob(schema, bytes))),
                          reference, "byte[]");
        assertThat(bytes).isEqualTo(original);

        for (Map.Entry<String, ByteBuffer> entry : buffers(bytes).entrySet()) {
            ByteBuffer buffer = entry.getValue();
            int position = buffer.position();
            int limit = buffer.limit();
            assertSameRequest(serialize(BLOB_BINDINGS_OPERATION, bindings((s, schema) -> s.writeBlob(schema, buffer))),
                              reference, entry.getKey());
            assertThat(buffer.position()).as(entry.getKey()).isEqualTo(position);
            assertThat(buffer.limit()).as(entry.getKey()).isEqualTo(limit);
        }
    }

    @Test
    void byteArrayPayload_isSentAsAReadOnlyViewOfTheSameArray() throws IOException {
        byte[] bytes = RequestSerializerTest.utf8("hello");
        SerializableStruct input = new SerializableStruct() {
            @Override
            public Schema sdkSchema() {
                return TestModel.BLOB_PAYLOAD;
            }

            @Override
            public void serializeMembers(ShapeSerializer serializer) {
                serializer.writeBlob(TestModel.BLOB_PAYLOAD.member("data"), bytes);
            }

            @Override
            public <T> T getMemberValue(Schema member) {
                return null;
            }
        };

        SdkHttpFullRequest request = serialize(TestModel.BLOB_OPERATION, input);

        assertThat(request.firstMatchingHeader("Content-Length")).hasValue("5");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/octet-stream");
        ByteBuffer content = request.contentStreamProvider().get().contentAsByteBufferOrNull();
        assertThat(content.isReadOnly()).isTrue();
        assertThat(content.remaining()).isEqualTo(5);
        assertThat(body(request)).isEqualTo("hello");
        // Shared, not copied: a later change to the array shows in the body.
        bytes[0] = 'j';
        assertThat(body(request)).isEqualTo("jello");
    }

    @Test
    void byteArrayTextBindings_areEncodedWithoutCopyingTheBytes() {
        com.sun.management.ThreadMXBean threads = allocationCounter();
        byte[] bytes = randomBytes(256 * 1024);
        ByteBuffer readOnly = ByteBuffer.wrap(bytes).asReadOnlyBuffer();

        long fromArray = allocatedBySecondRequest(threads, textBindings((s, schema) -> s.writeBlob(schema, bytes)));
        long fromReadOnly = allocatedBySecondRequest(threads,
                                                     textBindings((s, schema) -> s.writeBlob(schema, readOnly)));

        // Six text values of 256 KiB (a header, a query parameter, and two elements in each list). Both paths build the
        // encoded text; the read-only buffer has no accessible array, so the JDK encoder also copies each blob first.
        assertThat(fromReadOnly - fromArray).isGreaterThan(6L * bytes.length * 9 / 10);
    }

    private static void assertBindings(SdkHttpFullRequest request, String encoded) throws IOException {
        assertThat(request.firstMatchingHeader("X-Blob")).hasValue(encoded);
        assertThat(request.matchingHeaders("X-Blobs")).containsExactly(encoded + ", " + encoded);
        assertThat(request.rawQueryParameters().get("b")).containsExactly(encoded);
        assertThat(request.rawQueryParameters().get("bs")).containsExactly(encoded, encoded);
        assertThat(body(request)).isEqualTo("{\"body\":\"" + encoded + "\"}");
    }

    private static void assertSameRequest(SdkHttpFullRequest actual, SdkHttpFullRequest expected, String form)
        throws IOException {
        assertThat(actual.headers()).as(form).isEqualTo(expected.headers());
        assertThat(actual.rawQueryParameters()).as(form).isEqualTo(expected.rawQueryParameters());
        assertThat(body(actual)).as(form).isEqualTo(body(expected));
    }

    private static SerializableStruct bindings(BiConsumer<ShapeSerializer, Schema> writeBlob) {
        return struct(writeBlob, Arrays.asList("header", "query", "headerList", "queryList", "body"));
    }

    private static SerializableStruct textBindings(BiConsumer<ShapeSerializer, Schema> writeBlob) {
        return struct(writeBlob, Arrays.asList("header", "query", "headerList", "queryList"));
    }

    private static SerializableStruct struct(BiConsumer<ShapeSerializer, Schema> writeBlob, Iterable<String> members) {
        return new SerializableStruct() {
            @Override
            public Schema sdkSchema() {
                return BLOB_BINDINGS;
            }

            @Override
            public void serializeMembers(ShapeSerializer serializer) {
                for (String name : members) {
                    Schema member = BLOB_BINDINGS.member(name);
                    if (member.type() == ShapeType.LIST) {
                        serializer.writeList(member, writeBlob, 2, (w, list) -> {
                            w.accept(list, BLOB_LIST.listMember());
                            w.accept(list, BLOB_LIST.listMember());
                        });
                    } else {
                        writeBlob.accept(serializer, member);
                    }
                }
            }

            @Override
            public <T> T getMemberValue(Schema member) {
                return null;
            }
        };
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

    private static long allocatedBySecondRequest(com.sun.management.ThreadMXBean threads, SerializableStruct input) {
        // The first request loads and initializes the classes involved; the second is measured.
        serialize(BLOB_BINDINGS_OPERATION, input);
        long threadId = Thread.currentThread().getId();
        long before = threads.getThreadAllocatedBytes(threadId);
        serialize(BLOB_BINDINGS_OPERATION, input);
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
