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

package software.amazon.awssdk.schema.serde.document;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;

/**
 * A {@link ShapeDeserializer} that reads a {@link Document}, used by {@link Document#createDeserializer()} and so by
 * {@link Document#asShape(software.amazon.awssdk.schema.ShapeBuilder)}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.document.DocumentDeserializer}. Each read calls
 * the document's matching {@code as<Type>} method, so reads follow the document's coercion rules and throw
 * {@link SerializationException} when the document holds another type. A structure is read from the document's
 * {@link Document#getMemberNames() member names}: names the schema does not have are reported as unknown members, and
 * members whose value is null are skipped.
 *
 * <p>Codecs extend this class to customize how their documents are read, overriding {@link #deserializer(Document)}
 * so that nested values use the same subclass.
 */
@SdkProtectedApi
public class DocumentDeserializer implements ShapeDeserializer {

    private final Document value;

    /**
     * Creates a deserializer over a document.
     *
     * @param value the document to read, which may be null for a null value
     */
    public DocumentDeserializer(Document value) {
        this.value = value;
    }

    /**
     * Creates the deserializer used for a nested value: a structure member, list element or map value.
     *
     * @param value the nested value, which may be null
     * @return the deserializer
     */
    protected DocumentDeserializer deserializer(Document value) {
        return new DocumentDeserializer(value);
    }

    @Override
    public String readString(Schema schema) {
        return value.asString();
    }

    @Override
    public boolean readBoolean(Schema schema) {
        return value.asBoolean();
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        return value.asBlob();
    }

    @Override
    public byte readByte(Schema schema) {
        return value.asByte();
    }

    @Override
    public short readShort(Schema schema) {
        return value.asShort();
    }

    @Override
    public int readInteger(Schema schema) {
        return value.asInteger();
    }

    @Override
    public long readLong(Schema schema) {
        return value.asLong();
    }

    @Override
    public float readFloat(Schema schema) {
        return value.asFloat();
    }

    @Override
    public double readDouble(Schema schema) {
        return value.asDouble();
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        return value.asBigInteger();
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        return value.asBigDecimal();
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        return value.asTimestamp();
    }

    @Override
    public Document readDocument() {
        return value;
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        for (String memberName : value.getMemberNames()) {
            Schema memberSchema = schema.member(memberName);
            if (memberSchema == null) {
                consumer.unknownMember(state, memberName);
                continue;
            }
            Document memberValue = value.getMember(memberName);
            if (memberValue != null) {
                consumer.accept(state, memberSchema, deserializer(memberValue));
            }
        }
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        for (Document element : value.asList()) {
            consumer.accept(state, deserializer(element));
        }
    }

    @Override
    public int containerSize() {
        return value.size();
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        for (Map.Entry<String, Document> entry : value.asMap().entrySet()) {
            consumer.accept(state, entry.getKey(), deserializer(entry.getValue()));
        }
    }

    @Override
    public boolean isNull() {
        return value == null;
    }

    /**
     * Reads a null value.
     *
     * @return null
     * @throws SerializationException if the value is not null
     */
    @Override
    public <T> T readNull() {
        if (value != null) {
            throw new SerializationException("Attempted to read a non-null " + value.type() + " document as null");
        }
        return null;
    }
}
