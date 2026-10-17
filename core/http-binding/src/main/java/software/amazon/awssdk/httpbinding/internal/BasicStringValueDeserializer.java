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

package software.amazon.awssdk.httpbinding.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

/**
 * Reads one string value from a header, label or query parameter as the requested type.
 *
 * <p>Port of smithy-java {@code BasicStringValueDeserializer}. Every conversion failure throws
 * {@link SerializationException} naming the location and the member, without the value, which may be sensitive
 * (Requirement 15.8).
 */
@SdkInternalApi
abstract class BasicStringValueDeserializer implements ShapeDeserializer {
    private final String value;
    private final String location;
    // The structure member being read, when it differs from the schema passed to each read (list elements).
    private final Schema owner;

    BasicStringValueDeserializer(String value, String location, Schema owner) {
        this.value = value;
        this.location = location;
        this.owner = owner;
    }

    abstract TimestampFormatTrait.Format defaultTimestampFormat();

    private SerializationException invalid(String type, Schema schema, Throwable cause) {
        Schema member = owner != null ? owner : schema;
        return new SerializationException("Invalid " + type + " in " + location + " for member " + member.id(), cause);
    }

    @Override
    public boolean readBoolean(Schema schema) {
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw invalid("boolean", schema, null);
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        try {
            return ByteBuffer.wrap(Base64.getDecoder().decode(value.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException e) {
            throw invalid("base64 blob", schema, e);
        }
    }

    @Override
    public byte readByte(Schema schema) {
        try {
            return Byte.parseByte(value);
        } catch (NumberFormatException e) {
            throw invalid("byte", schema, e);
        }
    }

    @Override
    public short readShort(Schema schema) {
        try {
            return Short.parseShort(value);
        } catch (NumberFormatException e) {
            throw invalid("short", schema, e);
        }
    }

    @Override
    public int readInteger(Schema schema) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw invalid("integer", schema, e);
        }
    }

    @Override
    public long readLong(Schema schema) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw invalid("long", schema, e);
        }
    }

    @Override
    public float readFloat(Schema schema) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw invalid("float", schema, e);
        }
    }

    @Override
    public double readDouble(Schema schema) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw invalid("double", schema, e);
        }
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        try {
            return new BigInteger(value);
        } catch (NumberFormatException e) {
            throw invalid("bigInteger", schema, e);
        }
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw invalid("bigDecimal", schema, e);
        }
    }

    @Override
    public String readString(Schema schema) {
        if (schema.hasTrait(TraitKey.MEDIA_TYPE_TRAIT)) {
            try {
                return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                throw invalid("base64 string", schema, e);
            }
        }
        return value;
    }

    @Override
    public Document readDocument() {
        throw new SerializationException("Documents are not supported in " + location);
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        TimestampFormatter formatter = TimestampFormatter.of(schema, defaultTimestampFormat());
        try {
            // Labels, query parameters and headers are always strings, so epoch-seconds is read from its text.
            return formatter.readFromString(value, false);
        } catch (RuntimeException e) {
            throw invalid(formatter.format() + " timestamp", schema, e);
        }
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> structMemberConsumer) {
        throw new SerializationException("Structures are not supported in " + location);
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> listMemberConsumer) {
        throw new SerializationException("Lists are not supported in " + location);
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> mapMemberConsumer) {
        throw new SerializationException("Maps are not supported in " + location);
    }

    @Override
    public boolean isNull() {
        return value == null;
    }
}
