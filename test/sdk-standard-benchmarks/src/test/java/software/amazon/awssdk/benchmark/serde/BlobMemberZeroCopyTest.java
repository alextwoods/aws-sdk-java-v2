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

package software.amazon.awssdk.benchmark.serde;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Pins FU-12 on the path the JMH marshall benchmarks measure: the generated {@code PutItemRequest} writes every blob
 * member, list element and map value ({@code AttributeValue} {@code B} and {@code BS}) with
 * {@code writeBlob(Schema, byte[])}, so the codecs read the {@code SdkBytes} array instead of a read-only view that the
 * JDK base64 encoder copies.
 */
class BlobMemberZeroCopyTest {

    static Stream<Arguments> binaryDataCases() {
        // Blob count and total blob bytes of each case's B and BS values: the test data's strings, as UTF-8.
        Class<?> json = JsonRpc10MarshallBenchmark.class;
        Class<?> cbor = RpcV2CborMarshallBenchmark.class;
        return Stream.of(Arguments.of(json, "awsJson1_0_PutItemRequest_BinaryData_S", 1, 89),
                         Arguments.of(json, "awsJson1_0_PutItemRequest_BinaryData_M", 10, 7_190),
                         Arguments.of(json, "awsJson1_0_PutItemRequest_BinaryData_L", 78, 64_722),
                         Arguments.of(cbor, "rpcv2Cbor_PutItemRequest_BinaryData_S", 1, 89),
                         Arguments.of(cbor, "rpcv2Cbor_PutItemRequest_BinaryData_M", 10, 7_190),
                         Arguments.of(cbor, "rpcv2Cbor_PutItemRequest_BinaryData_L", 78, 64_722));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("binaryDataCases")
    void binaryDataRequest_writesEveryBlobFromAByteArray(Class<?> type, String testCaseId, int blobs, int blobBytes)
        throws Exception {
        Object benchmark = type.getDeclaredConstructor().newInstance();
        field(type, "testCaseId").set(benchmark, testCaseId);
        type.getMethod("setup").invoke(benchmark);
        SerializableStruct request = (SerializableStruct) field(type, "request").get(benchmark);

        BlobWriteCounter counter = new BlobWriteCounter();
        request.serializeMembers(counter);

        assertThat(counter.bufferWrites).isZero();
        assertThat(counter.arrayWrites).isEqualTo(blobs);
        assertThat(counter.arrayBytes).isEqualTo(blobBytes);

        Method marshallOnce = type.getDeclaredMethod("marshallOnce");
        marshallOnce.setAccessible(true);
        SdkHttpFullRequest marshalled = (SdkHttpFullRequest) marshallOnce.invoke(benchmark);
        assertThat(marshalled.contentStreamProvider()).isPresent();
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /**
     * Walks structures, lists and map values and counts the blob writes of each kind; other values are ignored.
     */
    private static final class BlobWriteCounter implements ShapeSerializer {
        private int bufferWrites;
        private int arrayWrites;
        private long arrayBytes;

        private final MapSerializer mapSerializer = new MapSerializer() {
            @Override
            public <T> void writeEntry(Schema keySchema, String key, T state, BiConsumer<T, ShapeSerializer> value) {
                value.accept(state, BlobWriteCounter.this);
            }
        };

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            struct.serializeMembers(this);
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            consumer.accept(listState, this);
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            consumer.accept(mapState, mapSerializer);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            bufferWrites++;
        }

        @Override
        public void writeBlob(Schema schema, byte[] value) {
            arrayWrites++;
            arrayBytes += value.length;
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
        }

        @Override
        public void writeByte(Schema schema, byte value) {
        }

        @Override
        public void writeShort(Schema schema, short value) {
        }

        @Override
        public void writeInteger(Schema schema, int value) {
        }

        @Override
        public void writeLong(Schema schema, long value) {
        }

        @Override
        public void writeFloat(Schema schema, float value) {
        }

        @Override
        public void writeDouble(Schema schema, double value) {
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
        }

        @Override
        public void writeString(Schema schema, String value) {
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
        }

        @Override
        public void writeNull(Schema schema) {
        }
    }
}
