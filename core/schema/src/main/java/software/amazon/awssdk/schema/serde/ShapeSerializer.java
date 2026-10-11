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

package software.amazon.awssdk.schema.serde;

import java.io.Flushable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Serializes a shape: receives the Smithy data model and writes it to an output owned by the serializer.
 *
 * <p>Null values are written only with {@link #writeNull(Schema)}. Every other method expects a non-null value.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.ShapeSerializer}. The data stream and event
 * stream write methods ({@code writeDataStream}, {@code writeEventStream}) are added with the streaming types
 * (design A.13). The serializer is a {@link SdkAutoCloseable} (an {@link AutoCloseable} whose {@code close} throws
 * no checked exception), following the SDK convention.
 */
@SdkPublicApi
public interface ShapeSerializer extends Flushable, SdkAutoCloseable {
    /**
     * Returns a serializer that writes nothing.
     *
     * @return the null serializer
     */
    static ShapeSerializer nullSerializer() {
        return NullSerializer.INSTANCE;
    }

    /**
     * Makes every value written so far available in the serializer's output. The default does nothing, for
     * serializers that do not buffer.
     */
    @Override
    default void flush() {
    }

    /**
     * Closes the serializer. The default calls {@link #flush()}.
     */
    @Override
    default void close() {
        flush();
    }

    /**
     * Writes a structure or union.
     *
     * @param schema the schema of the value, usually a member schema that targets the structure
     * @param struct the structure to serialize
     */
    void writeStruct(Schema schema, SerializableStruct struct);

    /**
     * Begins a list and writes zero or more values into it through {@code consumer}.
     *
     * @param schema the list schema
     * @param listState the state to pass to the consumer
     * @param size the number of elements in the list, or -1 if unknown
     * @param consumer called in the context of the list; writes the elements
     * @param <T> the state type
     */
    <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer);

    /**
     * Begins a map and writes zero or more entries into it through {@code consumer}.
     *
     * @param schema the map schema
     * @param mapState the state to pass to the consumer
     * @param size the number of entries in the map, or -1 if unknown
     * @param consumer called in the context of the map; writes the entries
     * @param <T> the state type
     */
    <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer);

    /**
     * Writes a boolean.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeBoolean(Schema schema, boolean value);

    /**
     * Writes a byte.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeByte(Schema schema, byte value);

    /**
     * Writes a short.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeShort(Schema schema, short value);

    /**
     * Writes an integer or int enum.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeInteger(Schema schema, int value);

    /**
     * Writes a long.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeLong(Schema schema, long value);

    /**
     * Writes a float.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeFloat(Schema schema, float value);

    /**
     * Writes a double.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeDouble(Schema schema, double value);

    /**
     * Writes a big integer.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeBigInteger(Schema schema, BigInteger value);

    /**
     * Writes a big decimal.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeBigDecimal(Schema schema, BigDecimal value);

    /**
     * Writes a string or enum.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeString(Schema schema, String value);

    /**
     * Writes a blob from the remaining bytes of a buffer. Implementations must not change the buffer's position or
     * limit.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeBlob(Schema schema, ByteBuffer value);

    /**
     * Writes a blob from a byte array. The default wraps the array, without copying, and calls
     * {@link #writeBlob(Schema, ByteBuffer)}.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    default void writeBlob(Schema schema, byte[] value) {
        writeBlob(schema, ByteBuffer.wrap(value));
    }

    /**
     * Writes a timestamp.
     *
     * @param schema the schema of the value
     * @param value the value
     */
    void writeTimestamp(Schema schema, Instant value);

    /**
     * Writes a document.
     *
     * @param schema the schema of the value; generally {@link PreludeSchemas#DOCUMENT} unless the document wraps a
     *               modeled shape
     * @param value the value
     */
    void writeDocument(Schema schema, Document value);

    /**
     * Writes a null value.
     *
     * @param schema the schema of the null value
     */
    void writeNull(Schema schema);
}
