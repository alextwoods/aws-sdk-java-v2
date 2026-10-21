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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.InterceptingSerializer;
import software.amazon.awssdk.schema.serde.ListSerializer;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * A {@link ShapeSerializer} that converts the Smithy data model it is given into a {@link Document}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.document.DocumentParser}.
 * {@link Document#of(software.amazon.awssdk.schema.SerializableShape)} uses it. Each document it creates holds the
 * schema it was written with, so a struct's member documents hold their member schemas.
 *
 * <ul>
 *     <li>A structure becomes a structure document over the struct, whose members are parsed only when first read,
 *     and whose {@link Document#getDiscriminator() discriminator} is the structure's ShapeId.</li>
 *     <li>A union is parsed at once into a structure document with no discriminator.</li>
 *     <li>Structure and union documents hold only the members that were written with a value; a member written as
 *     null is left out.</li>
 *     <li>List elements and map values written as null are kept as null.</li>
 *     <li>A document value is parsed from its {@link Document#serializeContents(ShapeSerializer) contents}.</li>
 * </ul>
 */
@SdkProtectedApi
public final class DocumentParser implements ShapeSerializer {

    private Document result;

    /**
     * Returns the document created by the last write, or null if nothing was written or null was written.
     *
     * @return the result
     */
    public Document getResult() {
        return result;
    }

    /**
     * Returns the stored result and clears it.
     *
     * @return the result that was stored, or null
     */
    public Document clearResult() {
        Document value = result;
        result = null;
        return value;
    }

    /**
     * Parses the members a struct writes, keyed by Smithy member name in the order written, leaving out members
     * written as null.
     */
    static Map<String, Document> parseMembers(SerializableStruct struct) {
        StructureParser parser = new StructureParser();
        struct.serializeMembers(parser);
        return parser.members;
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        if (schema.type() == ShapeType.STRUCTURE) {
            result = new Documents.LazyStructure(schema, struct);
        } else {
            result = new Documents.StructureDocument(schema, parseMembers(struct));
        }
    }

    @Override
    public <T> void writeList(Schema schema, T state, int size, BiConsumer<T, ShapeSerializer> consumer) {
        List<Document> elements = size < 0 ? new ArrayList<>() : new ArrayList<>(size);
        DocumentParser elementParser = new DocumentParser();
        ListSerializer serializer = new ListSerializer(elementParser, position -> {
            if (position > 0) {
                elements.add(elementParser.clearResult());
            }
        });
        consumer.accept(state, serializer);
        // Null elements are kept: an untyped document cannot declare a list sparse, so dropping them would lose data.
        // The last element is added only if one was written, so an empty list gets no spurious null.
        if (serializer.position() > 0) {
            elements.add(elementParser.clearResult());
        }
        result = new Documents.ListDocument(schema.type() == ShapeType.DOCUMENT ? null : schema, elements);
    }

    @Override
    public <T> void writeMap(Schema schema, T state, int size, BiConsumer<T, MapSerializer> consumer) {
        if (schema.type() != ShapeType.DOCUMENT) {
            ShapeType keyType = schema.mapKeyMember().type();
            if (keyType != ShapeType.STRING && keyType != ShapeType.ENUM) {
                throw new SerializationException("Unexpected map key schema: " + schema);
            }
        }
        DocumentMapSerializer serializer = new DocumentMapSerializer(size);
        consumer.accept(state, serializer);
        result = new Documents.StringMapDocument(schema.type() == ShapeType.DOCUMENT ? null : schema,
                                                 serializer.entries);
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        result = new Documents.BooleanDocument(schema, value);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        result = new Documents.IntegralDocument(ShapeType.BYTE, schema, value);
    }

    @Override
    public void writeShort(Schema schema, short value) {
        result = new Documents.IntegralDocument(ShapeType.SHORT, schema, value);
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        result = new Documents.IntegralDocument(ShapeType.INTEGER, schema, value);
    }

    @Override
    public void writeLong(Schema schema, long value) {
        result = new Documents.IntegralDocument(ShapeType.LONG, schema, value);
    }

    @Override
    public void writeFloat(Schema schema, float value) {
        result = new Documents.FloatingPointDocument(ShapeType.FLOAT, schema, value);
    }

    @Override
    public void writeDouble(Schema schema, double value) {
        result = new Documents.FloatingPointDocument(ShapeType.DOUBLE, schema, value);
    }

    @Override
    public void writeBigInteger(Schema schema, BigInteger value) {
        result = new Documents.BigIntegerDocument(schema, value);
    }

    @Override
    public void writeBigDecimal(Schema schema, BigDecimal value) {
        result = new Documents.BigDecimalDocument(schema, value);
    }

    @Override
    public void writeString(Schema schema, String value) {
        result = new Documents.StringDocument(schema, value);
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        result = new Documents.BlobDocument(schema, value);
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        result = new Documents.TimestampDocument(schema, value);
    }

    @Override
    public void writeDocument(Schema schema, Document value) {
        if (value == null) {
            result = null;
        } else {
            value.serializeContents(this);
        }
    }

    @Override
    public void writeNull(Schema schema) {
        result = null;
    }

    /**
     * Parses each member of a struct with a shared parser and keeps the members that were written with a value.
     */
    private static final class StructureParser extends InterceptingSerializer {
        private final DocumentParser parser = new DocumentParser();
        private final Map<String, Document> members = new LinkedHashMap<>();

        @Override
        protected ShapeSerializer before(Schema schema) {
            return parser;
        }

        @Override
        protected void after(Schema schema) {
            Document value = parser.clearResult();
            if (value != null) {
                members.put(schema.memberName(), value);
            }
        }
    }

    private static final class DocumentMapSerializer implements MapSerializer {
        private final Map<String, Document> entries;

        DocumentMapSerializer(int size) {
            entries = size < 0 ? new LinkedHashMap<>() : new LinkedHashMap<>(size * 4 / 3 + 1);
        }

        @Override
        public <U> void writeEntry(Schema keySchema, String key, U state, BiConsumer<U, ShapeSerializer> valueSerializer) {
            DocumentParser valueParser = new DocumentParser();
            valueSerializer.accept(state, valueParser);
            entries.put(key, valueParser.result);
        }
    }
}
