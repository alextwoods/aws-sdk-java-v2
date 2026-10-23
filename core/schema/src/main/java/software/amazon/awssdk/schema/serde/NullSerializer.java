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
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Serializes nothing.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.NullSerializer}.
 *
 * @see ShapeSerializer#nullSerializer()
 */
@SdkInternalApi
final class NullSerializer implements ShapeSerializer {
    static final NullSerializer INSTANCE = new NullSerializer();

    private NullSerializer() {
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
    }

    @Override
    public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
    }

    @Override
    public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
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
    public void writeBlob(Schema schema, ByteBuffer value) {
    }

    @Override
    public void writeBlob(Schema schema, byte[] value) {
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
