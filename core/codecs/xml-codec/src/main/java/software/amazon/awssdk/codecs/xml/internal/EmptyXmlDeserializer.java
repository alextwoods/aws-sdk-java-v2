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

package software.amazon.awssdk.codecs.xml.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * A no-op {@link ShapeDeserializer} for empty XML payloads: structures, lists and maps read as empty, and scalar reads
 * throw.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.xml.EmptyXmlDeserializer}.
 */
@SdkInternalApi
final class EmptyXmlDeserializer implements ShapeDeserializer {

    static final EmptyXmlDeserializer INSTANCE = new EmptyXmlDeserializer();

    private EmptyXmlDeserializer() {
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
    }

    // Scalar reads are not valid at the top level of an empty XML body.

    @Override
    public boolean readBoolean(Schema schema) {
        throw empty(schema);
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        throw empty(schema);
    }

    @Override
    public byte readByte(Schema schema) {
        throw empty(schema);
    }

    @Override
    public short readShort(Schema schema) {
        throw empty(schema);
    }

    @Override
    public int readInteger(Schema schema) {
        throw empty(schema);
    }

    @Override
    public long readLong(Schema schema) {
        throw empty(schema);
    }

    @Override
    public float readFloat(Schema schema) {
        throw empty(schema);
    }

    @Override
    public double readDouble(Schema schema) {
        throw empty(schema);
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        throw empty(schema);
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        throw empty(schema);
    }

    @Override
    public String readString(Schema schema) {
        throw empty(schema);
    }

    @Override
    public Document readDocument() {
        throw new SerializationException("Cannot read a document value from an empty XML payload");
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        throw empty(schema);
    }

    private static SerializationException empty(Schema schema) {
        return new SerializationException("Cannot read " + schema.id() + " value from an empty XML payload");
    }
}
