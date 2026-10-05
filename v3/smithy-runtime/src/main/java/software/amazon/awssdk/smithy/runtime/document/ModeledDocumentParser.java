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

package software.amazon.awssdk.smithy.runtime.document;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.InterceptingSerializer;
import software.amazon.smithy.java.core.serde.MapSerializer;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Converts the serialization events of a value into {@link ModeledDocument} nodes, keeping each value's schema.
 *
 * <p>Document values (members targeting {@code smithy.api#Document}) are kept unchanged. Streaming values are not
 * finite and are rejected rather than silently dropped, so that member presence is never lost.
 */
final class ModeledDocumentParser implements ShapeSerializer {

    private Document result;

    Document result() {
        return result;
    }

    private void set(Document document) {
        this.result = document;
    }

    private void scalar(Schema schema, Set<ShapeType> kinds, Object value) {
        if (!kinds.contains(schema.type())) {
            throw new SerializationException("A " + kinds + " value was written with the " + schema.type()
                                             + " schema " + schema.id());
        }
        set(new ModeledDocument.Scalar(schema, value));
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        if (schema.type() != ShapeType.STRUCTURE && schema.type() != ShapeType.UNION) {
            throw new SerializationException("A structure was written with the " + schema.type() + " schema "
                                             + schema.id());
        }
        Map<String, Document> members = new LinkedHashMap<>();
        ModeledDocumentParser memberParser = new ModeledDocumentParser();
        struct.serializeMembers(new InterceptingSerializer() {
            @Override
            protected ShapeSerializer before(Schema member) {
                memberParser.result = null;
                return memberParser;
            }

            @Override
            protected void after(Schema member) {
                // A null member is absent.
                if (memberParser.result != null) {
                    members.put(member.memberName(), memberParser.result);
                }
            }
        });
        set(new ModeledDocument.Aggregate(schema, members));
    }

    @Override
    public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
        if (schema.type() != ShapeType.LIST && schema.type() != ShapeType.SET) {
            throw new SerializationException("A list was written with the " + schema.type() + " schema " + schema.id());
        }
        List<Document> elements = new ArrayList<>(Math.max(size, 0));
        ModeledDocumentParser elementParser = new ModeledDocumentParser();
        consumer.accept(listState, new InterceptingSerializer() {
            @Override
            protected ShapeSerializer before(Schema member) {
                elementParser.result = null;
                return elementParser;
            }

            @Override
            protected void after(Schema member) {
                elements.add(elementParser.result);
            }
        });
        set(new ModeledDocument.Sequence(schema, elements));
    }

    @Override
    public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
        if (schema.type() != ShapeType.MAP) {
            throw new SerializationException("A map was written with the " + schema.type() + " schema " + schema.id());
        }
        Map<String, Document> entries = new LinkedHashMap<>();
        consumer.accept(mapState, new MapSerializer() {
            @Override
            public <U> void writeEntry(Schema keySchema, String key, U state, BiConsumer<U, ShapeSerializer> value) {
                ModeledDocumentParser valueParser = new ModeledDocumentParser();
                value.accept(state, valueParser);
                entries.put(key, valueParser.result);
            }
        });
        set(new ModeledDocument.StringMap(schema, entries));
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        scalar(schema, EnumSet.of(ShapeType.BOOLEAN), value);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        scalar(schema, EnumSet.of(ShapeType.BYTE), value);
    }

    @Override
    public void writeShort(Schema schema, short value) {
        scalar(schema, EnumSet.of(ShapeType.SHORT), value);
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        scalar(schema, EnumSet.of(ShapeType.INTEGER, ShapeType.INT_ENUM), value);
    }

    @Override
    public void writeLong(Schema schema, long value) {
        scalar(schema, EnumSet.of(ShapeType.LONG), value);
    }

    @Override
    public void writeFloat(Schema schema, float value) {
        scalar(schema, EnumSet.of(ShapeType.FLOAT), value);
    }

    @Override
    public void writeDouble(Schema schema, double value) {
        scalar(schema, EnumSet.of(ShapeType.DOUBLE), value);
    }

    @Override
    public void writeBigInteger(Schema schema, BigInteger value) {
        scalar(schema, EnumSet.of(ShapeType.BIG_INTEGER), value);
    }

    @Override
    public void writeBigDecimal(Schema schema, BigDecimal value) {
        scalar(schema, EnumSet.of(ShapeType.BIG_DECIMAL), value);
    }

    @Override
    public void writeString(Schema schema, String value) {
        scalar(schema, EnumSet.of(ShapeType.STRING, ShapeType.ENUM), value);
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        if (schema.type() != ShapeType.BLOB) {
            throw new SerializationException("A blob was written with the " + schema.type() + " schema " + schema.id());
        }
        set(ModeledDocument.Scalar.blob(schema, value));
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        scalar(schema, EnumSet.of(ShapeType.TIMESTAMP), value);
    }

    @Override
    public void writeDocument(Schema schema, Document value) {
        set(value);
    }

    @Override
    public void writeNull(Schema schema) {
        set(null);
    }

    @Override
    public void writeDataStream(Schema schema, DataStream value) {
        throw new SerializationException("The streaming blob " + schema.id() + " is not a finite value and cannot be "
                                         + "converted to a document");
    }

    @Override
    public void writeEventStream(Schema schema, EventStream<? extends SerializableStruct> value) {
        throw new SerializationException("The event stream " + schema.id() + " is not a finite value and cannot be "
                                         + "converted to a document");
    }
}
