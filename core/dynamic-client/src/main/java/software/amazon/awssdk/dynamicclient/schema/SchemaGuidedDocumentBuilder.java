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

package software.amazon.awssdk.dynamicclient.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * A shape builder that reads a structure or union into a {@link StructDocument}, guided by its schema.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.dynamicschemas.SchemaGuidedDocumentBuilder}.
 */
@SdkInternalApi
final class SchemaGuidedDocumentBuilder implements ShapeBuilder<StructDocument> {
    private static final int DEFAULT_COLLECTION_CAPACITY = 16;

    private final ShapeId service;
    private final Schema target;
    private final Document[] values;
    private final StructConsumer structConsumer = new StructConsumer();
    private final UnionConsumer unionConsumer = new UnionConsumer();
    private final ListConsumer listConsumer = new ListConsumer();
    private final MapConsumer mapConsumer = new MapConsumer();

    SchemaGuidedDocumentBuilder(Schema target, ShapeId service) {
        if (target.type() != ShapeType.STRUCTURE && target.type() != ShapeType.UNION) {
            throw new IllegalArgumentException("StructDocument can only deserialize a structure or union, but got "
                                               + target);
        }
        this.target = target;
        this.service = service;
        this.values = new Document[target.members().size()];
    }

    @Override
    public Schema sdkSchema() {
        return target;
    }

    @Override
    public StructDocument build() {
        if (target.type() == ShapeType.UNION) {
            boolean hasValue = false;
            for (Document v : values) {
                if (v != null) {
                    hasValue = true;
                    break;
                }
            }
            if (!hasValue) {
                throw new IllegalArgumentException("No value set for union document: " + target.id());
            }
        }
        return new StructDocument(target, values.clone());
    }

    /**
     * Sets a member from a {@link Document}, a {@link SerializableShape} (converted with {@link Document#of}), or a
     * standard library value: {@link String}, {@link Boolean}, a {@link Number}, {@code byte[]}, {@link ByteBuffer},
     * {@link Instant}, a {@link List} or a {@link Map} with string keys. A null value clears the member.
     */
    @Override
    public void setMemberValue(Schema member, Object value) {
        Schema expected = member == null ? null : target.member(member.memberName());
        if (expected == null) {
            throw SchemaUtils.notAMember(target, member);
        }
        SchemaUtils.validateSameMember(expected, member, value);
        values[expected.memberIndex()] =
            value == null ? null : StructDocument.convertDocument(expected, toDocument(value), service);
    }

    /**
     * The values smithy-java's {@code Document.ofObject} accepts, which {@code core/schema} does not port.
     */
    @SuppressWarnings("unchecked")
    private static Document toDocument(Object value) {
        if (value instanceof Document) {
            return (Document) value;
        }
        if (value instanceof SerializableShape) {
            return Document.of((SerializableShape) value);
        }
        if (value instanceof String) {
            return Document.of((String) value);
        }
        if (value instanceof Boolean) {
            return Document.of((Boolean) value);
        }
        if (value instanceof Number) {
            return number((Number) value);
        }
        if (value instanceof byte[]) {
            return Document.of((byte[]) value);
        }
        if (value instanceof ByteBuffer) {
            return Document.of((ByteBuffer) value);
        }
        if (value instanceof Instant) {
            return Document.of((Instant) value);
        }
        if (value instanceof List) {
            List<Document> elements = new ArrayList<>();
            for (Object element : (List<Object>) value) {
                elements.add(element == null ? null : toDocument(element));
            }
            return Document.of(elements);
        }
        if (value instanceof Map) {
            Map<String, Document> entries = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("Map keys must be strings, but found " + entry.getKey());
                }
                Object element = entry.getValue();
                entries.put((String) entry.getKey(), element == null ? null : toDocument(element));
            }
            return Document.of(entries);
        }
        throw new IllegalArgumentException("Unsupported member value type: " + value.getClass().getName());
    }

    private static Document number(Number value) {
        if (value instanceof Integer) {
            return Document.of(value.intValue());
        }
        if (value instanceof Long) {
            return Document.of(value.longValue());
        }
        if (value instanceof Double) {
            return Document.of(value.doubleValue());
        }
        if (value instanceof Float) {
            return Document.of(value.floatValue());
        }
        if (value instanceof Short) {
            return Document.of(value.shortValue());
        }
        if (value instanceof Byte) {
            return Document.of(value.byteValue());
        }
        if (value instanceof BigInteger) {
            return Document.of((BigInteger) value);
        }
        if (value instanceof BigDecimal) {
            return Document.of((BigDecimal) value);
        }
        throw new IllegalArgumentException("Unsupported number type: " + value.getClass().getName());
    }

    @Override
    public ShapeBuilder<StructDocument> deserialize(ShapeDeserializer decoder) {
        decoder.readStruct(target, values, structConsumer);
        return this;
    }

    @Override
    public ShapeBuilder<StructDocument> deserializeMember(ShapeDeserializer decoder, Schema schema) {
        decoder.readStruct(schema.assertMemberTargetIs(target), values, structConsumer);
        return this;
    }

    // Common scalars inline here; rare scalars and aggregates are split out to stay under C2's FreqInlineSize.
    private Document deserializeValue(ShapeDeserializer decoder, Schema schema) {
        switch (schema.type()) {
            case STRING:
            case ENUM:
                return new ContentDocument(schema, decoder.readString(schema));
            case BOOLEAN:
                return new ContentDocument(schema, decoder.readBoolean(schema));
            case INTEGER:
            case INT_ENUM:
                return new ContentDocument(schema, decoder.readInteger(schema));
            case LONG:
                return new ContentDocument(schema, decoder.readLong(schema));
            case DOUBLE:
                return new ContentDocument(schema, decoder.readDouble(schema));
            case LIST:
            case MAP:
            case STRUCTURE:
            case UNION:
                return deserializeAggregate(decoder, schema);
            default:
                return deserializeRareScalar(decoder, schema);
        }
    }

    // Union variant: returns raw boxed scalars (no ContentDocument wrapper) to avoid an allocation per union.
    private Object deserializeUnionValue(ShapeDeserializer decoder, Schema schema) {
        switch (schema.type()) {
            case STRING:
            case ENUM:
                return decoder.readString(schema);
            case BOOLEAN:
                return decoder.readBoolean(schema);
            case INTEGER:
            case INT_ENUM:
                return decoder.readInteger(schema);
            case LONG:
                return decoder.readLong(schema);
            case DOUBLE:
                return decoder.readDouble(schema);
            case LIST:
            case MAP:
            case STRUCTURE:
            case UNION:
                return deserializeAggregate(decoder, schema);
            default:
                return deserializeRareUnionScalar(decoder, schema);
        }
    }

    private Object deserializeRareUnionScalar(ShapeDeserializer decoder, Schema schema) {
        switch (schema.type()) {
            case FLOAT:
                return decoder.readFloat(schema);
            case BYTE:
                return decoder.readByte(schema);
            case SHORT:
                return decoder.readShort(schema);
            case TIMESTAMP:
                return decoder.readTimestamp(schema);
            case BIG_DECIMAL:
                return decoder.readBigDecimal(schema);
            case BIG_INTEGER:
                return decoder.readBigInteger(schema);
            case BLOB:
                StructDocument.rejectStreaming(schema);
                return decoder.readBlob(schema);
            case DOCUMENT:
                // Document members keep their wrapper; they are not scalars.
                return new ContentDocument.SchemaDocument(schema, decoder.readDocument());
            default:
                throw new UnsupportedOperationException("Unsupported target type: " + schema.type());
        }
    }

    private Document deserializeRareScalar(ShapeDeserializer decoder, Schema schema) {
        switch (schema.type()) {
            case FLOAT:
                return new ContentDocument(schema, decoder.readFloat(schema));
            case BYTE:
                return new ContentDocument(schema, decoder.readByte(schema));
            case SHORT:
                return new ContentDocument(schema, decoder.readShort(schema));
            case TIMESTAMP:
                return new ContentDocument(schema, decoder.readTimestamp(schema));
            case BIG_DECIMAL:
                return new ContentDocument(schema, decoder.readBigDecimal(schema));
            case BIG_INTEGER:
                return new ContentDocument(schema, decoder.readBigInteger(schema));
            case DOCUMENT:
                return new ContentDocument.SchemaDocument(schema, decoder.readDocument());
            case BLOB:
                StructDocument.rejectStreaming(schema);
                return new ContentDocument(schema, decoder.readBlob(schema));
            default:
                throw new UnsupportedOperationException("Unsupported target type: " + schema.type());
        }
    }

    private Document deserializeAggregate(ShapeDeserializer decoder, Schema schema) {
        switch (schema.type()) {
            case LIST:
                return deserializeList(decoder, schema);
            case MAP:
                return deserializeMap(decoder, schema);
            case STRUCTURE:
                return createStructDocument(decoder, schema);
            case UNION:
                StructDocument.rejectStreaming(schema);
                return createUnionDocument(decoder, schema);
            default:
                throw new UnsupportedOperationException("Unsupported target type: " + schema.type());
        }
    }

    private Document deserializeList(ShapeDeserializer decoder, Schema schema) {
        Schema savedSchema = listConsumer.schema;
        boolean savedSparse = listConsumer.sparse;
        listConsumer.schema = schema.listMember();
        listConsumer.sparse = schema.hasTrait(TraitKey.SPARSE_TRAIT);
        int size = decoder.containerSize();
        List<Document> items = size >= 0 && size <= decoder.containerPreAllocationLimit()
                               ? new ArrayList<>(size)
                               : new ArrayList<>(DEFAULT_COLLECTION_CAPACITY);
        decoder.readList(schema, items, listConsumer);
        listConsumer.schema = savedSchema;
        listConsumer.sparse = savedSparse;
        return new ContentDocument(schema, items);
    }

    private Document deserializeMap(ShapeDeserializer decoder, Schema schema) {
        Schema savedSchema = mapConsumer.schema;
        boolean savedSparse = mapConsumer.sparse;
        mapConsumer.schema = schema.mapValueMember();
        mapConsumer.sparse = schema.hasTrait(TraitKey.SPARSE_TRAIT);
        int size = decoder.containerSize();
        // LinkedHashMap: iteration without empty-bucket scanning, in wire order.
        Map<String, Document> map = size >= 0 && size <= decoder.containerPreAllocationLimit()
                                    ? new LinkedHashMap<>(capacityFor(size))
                                    : new LinkedHashMap<>(DEFAULT_COLLECTION_CAPACITY);
        decoder.readStringMap(schema, map, mapConsumer);
        mapConsumer.schema = savedSchema;
        mapConsumer.sparse = savedSparse;
        return new ContentDocument(schema, map);
    }

    /**
     * The initial capacity that holds {@code size} entries without a resize ({@code LinkedHashMap.newLinkedHashMap}
     * is JDK 19).
     */
    private static int capacityFor(int size) {
        return (int) Math.ceil(size / 0.75d);
    }

    private StructDocument createStructDocument(ShapeDeserializer decoder, Schema schema) {
        Document[] nestedValues = new Document[schema.members().size()];
        decoder.readStruct(schema, nestedValues, structConsumer);
        return new StructDocument(schema, nestedValues);
    }

    private StructDocument createUnionDocument(ShapeDeserializer decoder, Schema schema) {
        unionConsumer.value = null;
        unionConsumer.memberSchema = null;
        decoder.readStruct(schema, null, unionConsumer);
        return new StructDocument(schema, unionConsumer.value, unionConsumer.memberSchema);
    }

    @Override
    public ShapeBuilder<StructDocument> errorCorrection() {
        // smithy-java has a TODO to fill in defaults here.
        return this;
    }

    private final class StructConsumer implements ShapeDeserializer.StructMemberConsumer<Document[]> {
        @Override
        public void accept(Document[] state, Schema memberSchema, ShapeDeserializer memberDeserializer) {
            state[memberSchema.memberIndex()] = deserializeValue(memberDeserializer, memberSchema);
        }
    }

    private final class UnionConsumer implements ShapeDeserializer.StructMemberConsumer<Object> {
        // Object, not Document: scalar members are stored raw, aggregates as documents.
        private Object value;
        private Schema memberSchema;

        @Override
        public void accept(Object state, Schema memberSchema, ShapeDeserializer memberDeserializer) {
            value = deserializeUnionValue(memberDeserializer, memberSchema);
            this.memberSchema = memberSchema;
        }
    }

    private final class ListConsumer implements ShapeDeserializer.ListMemberConsumer<List<Document>> {
        private Schema schema;
        private boolean sparse;

        @Override
        public void accept(List<Document> state, ShapeDeserializer memberDeserializer) {
            if (memberDeserializer.isNull()) {
                if (!sparse) {
                    throw new SerializationException("Null value found in dense list: " + schema.id());
                }
                state.add(memberDeserializer.readNull());
            } else {
                state.add(deserializeValue(memberDeserializer, schema));
            }
        }
    }

    private final class MapConsumer implements ShapeDeserializer.MapMemberConsumer<String, Map<String, Document>> {
        private Schema schema;
        private boolean sparse;

        @Override
        public void accept(Map<String, Document> state, String key, ShapeDeserializer memberDeserializer) {
            if (memberDeserializer.isNull()) {
                if (!sparse) {
                    throw new SerializationException("Null value found in dense map: " + schema.id());
                }
                state.put(key, memberDeserializer.readNull());
            } else {
                state.put(key, deserializeValue(memberDeserializer, schema));
            }
        }
    }
}
