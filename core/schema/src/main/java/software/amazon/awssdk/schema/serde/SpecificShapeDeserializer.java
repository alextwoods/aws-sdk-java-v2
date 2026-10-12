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
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * A deserializer that can read one specific kind of shape and fails when any other kind is read.
 *
 * <p>Every read method throws the exception returned by {@link #throwForInvalidState(Schema)}, and {@link #isNull()}
 * throws {@link UnsupportedOperationException}. Subclasses override the read methods for the shapes they accept.
 */
@SdkPublicApi
public abstract class SpecificShapeDeserializer implements ShapeDeserializer {

    /**
     * Invoked when an unexpected shape is read.
     *
     * @param schema Schema of the unexpected shape.
     * @return the exception to throw. The default is a {@link SerializationException} whose message contains the
     *     schema's ShapeId.
     */
    protected RuntimeException throwForInvalidState(Schema schema) {
        return new SerializationException("Unexpected schema type: " + schema);
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public byte readByte(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public short readShort(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public int readInteger(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public long readLong(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public float readFloat(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public double readDouble(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public String readString(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public boolean readBoolean(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public Document readDocument() {
        throw throwForInvalidState(PreludeSchemas.DOCUMENT);
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        throw throwForInvalidState(schema);
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        throw throwForInvalidState(schema);
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        throw throwForInvalidState(schema);
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        throw throwForInvalidState(schema);
    }

    @Override
    public boolean isNull() {
        throw new UnsupportedOperationException("cannot look ahead for null values");
    }
}
