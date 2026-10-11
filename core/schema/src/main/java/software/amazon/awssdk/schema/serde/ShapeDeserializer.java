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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Deserializes a shape: reads the Smithy data model from an input, guided by schemas.
 *
 * <p>Structures, lists and maps are read through consumers that receive a caller-supplied state value, so that
 * generated code can pass static, non-capturing consumers.
 *
 * <p>Implementations throw {@link SerializationException} when the input is malformed or does not match the type of
 * the read method called.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.ShapeDeserializer}. The data stream and event
 * stream read methods ({@code readDataStream}, {@code readEventStream}) are added with the streaming types
 * (design A.13). The deserializer is a {@link SdkAutoCloseable} (an {@link AutoCloseable} whose {@code close} throws
 * no checked exception), following the SDK convention.
 */
@SdkPublicApi
public interface ShapeDeserializer extends SdkAutoCloseable {
    /**
     * The default value of {@link #containerPreAllocationLimit()}: 10,000 elements, unless the
     * {@code aws.serde.containerPreAllocationLimit} system property is set to an integer when this interface is
     * initialized.
     */
    int CONTAINER_PRE_ALLOCATION_LIMIT = Integer.getInteger("aws.serde.containerPreAllocationLimit", 10_000);

    /**
     * Closes the deserializer. The default does nothing.
     */
    @Override
    default void close() {
    }

    /**
     * Reads a boolean.
     *
     * @param schema the schema of the value
     * @return the value
     */
    boolean readBoolean(Schema schema);

    /**
     * Reads a blob.
     *
     * @param schema the schema of the value
     * @return the value
     */
    ByteBuffer readBlob(Schema schema);

    /**
     * Reads a byte.
     *
     * @param schema the schema of the value
     * @return the value
     */
    byte readByte(Schema schema);

    /**
     * Reads a short.
     *
     * @param schema the schema of the value
     * @return the value
     */
    short readShort(Schema schema);

    /**
     * Reads an integer or int enum.
     *
     * @param schema the schema of the value
     * @return the value
     */
    int readInteger(Schema schema);

    /**
     * Reads a long.
     *
     * @param schema the schema of the value
     * @return the value
     */
    long readLong(Schema schema);

    /**
     * Reads a float.
     *
     * @param schema the schema of the value
     * @return the value
     */
    float readFloat(Schema schema);

    /**
     * Reads a double.
     *
     * @param schema the schema of the value
     * @return the value
     */
    double readDouble(Schema schema);

    /**
     * Reads a big integer.
     *
     * @param schema the schema of the value
     * @return the value
     */
    BigInteger readBigInteger(Schema schema);

    /**
     * Reads a big decimal.
     *
     * @param schema the schema of the value
     * @return the value
     */
    BigDecimal readBigDecimal(Schema schema);

    /**
     * Reads a string or enum.
     *
     * @param schema the schema of the value
     * @return the value
     */
    String readString(Schema schema);

    /**
     * Reads a document.
     *
     * @return the value
     */
    Document readDocument();

    /**
     * Reads a timestamp.
     *
     * @param schema the schema of the value
     * @return the value
     */
    Instant readTimestamp(Schema schema);

    /**
     * Reads a structure or union. The consumer is called once per member present in the input that the schema
     * defines; members the schema does not define are skipped and reported to
     * {@link StructMemberConsumer#unknownMember(Object, String)}.
     *
     * @param schema the schema of the value
     * @param state the state to pass to the consumer
     * @param consumer receives the state, the member schema and the deserializer positioned at the member's value
     * @param <T> the state type
     */
    <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer);

    /**
     * Reads a list. The consumer is called once per element.
     *
     * @param schema the schema of the value
     * @param state the state to pass to the consumer
     * @param consumer receives the state and the deserializer positioned at the element
     * @param <T> the state type
     */
    <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer);

    /**
     * Returns the number of elements or entries of the list or map about to be read, when the input declares it.
     * The default returns -1.
     *
     * @return the number of elements or entries, or -1 if unknown or if the next value is not a list or map
     */
    default int containerSize() {
        return -1;
    }

    /**
     * Returns the maximum number of elements to pre-allocate when deserializing a list or map.
     *
     * <p>{@link #containerSize()} reports the size claimed by the input, which may be untrusted. Code that pre-sizes a
     * collection from it should cap the size at this limit, and grow the collection dynamically beyond it, so that a
     * maliciously large claimed size cannot exhaust memory. The default returns
     * {@link #CONTAINER_PRE_ALLOCATION_LIMIT}.
     *
     * @return the pre-allocation limit; 0 means never pre-allocate
     */
    default int containerPreAllocationLimit() {
        return CONTAINER_PRE_ALLOCATION_LIMIT;
    }

    /**
     * Reads a map with string keys. The consumer is called once per entry.
     *
     * @param schema the schema of the value
     * @param state the state to pass to the consumer
     * @param consumer receives the state, the entry key and the deserializer positioned at the entry value
     * @param <T> the state type
     */
    <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer);

    /**
     * Returns whether the next value is null, without consuming it. Useful for sparse collections.
     *
     * @return true if the next value is null
     */
    boolean isNull();

    /**
     * Consumes a null value; call only after {@link #isNull()} returned true. The default consumes nothing and returns
     * null.
     *
     * @param <T> the type expected by the caller
     * @return null
     */
    default <T> T readNull() {
        return null;
    }

    /**
     * Consumer of structure and union members.
     *
     * @param <T> the state type, passed in so that the consumer need not capture state
     */
    @FunctionalInterface
    interface StructMemberConsumer<T> {
        /**
         * Reads one member.
         *
         * @param state the state given to {@link #readStruct}
         * @param memberSchema the schema of the member
         * @param memberDeserializer the deserializer positioned at the member's value
         */
        void accept(T state, Schema memberSchema, ShapeDeserializer memberDeserializer);

        /**
         * Called for a member that the structure schema does not define, after its value has been skipped. The
         * default does nothing.
         *
         * @param state the state given to {@link #readStruct}
         * @param memberName the name of the unknown member
         */
        default void unknownMember(T state, String memberName) {
        }
    }

    /**
     * Consumer of list elements.
     *
     * @param <T> the state type, passed in so that the consumer need not capture state
     */
    @FunctionalInterface
    interface ListMemberConsumer<T> {
        /**
         * Reads one element.
         *
         * @param state the state given to {@link #readList}
         * @param memberDeserializer the deserializer positioned at the element
         */
        void accept(T state, ShapeDeserializer memberDeserializer);
    }

    /**
     * Consumer of map entries.
     *
     * @param <K> the key type
     * @param <T> the state type, passed in so that the consumer need not capture state
     */
    @FunctionalInterface
    interface MapMemberConsumer<K, T> {
        /**
         * Reads one entry.
         *
         * @param state the state given to {@link #readStringMap}
         * @param key the entry key
         * @param memberDeserializer the deserializer positioned at the entry value
         */
        void accept(T state, K key, ShapeDeserializer memberDeserializer);
    }
}
