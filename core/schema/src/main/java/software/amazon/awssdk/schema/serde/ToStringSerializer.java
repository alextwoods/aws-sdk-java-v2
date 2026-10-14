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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.serde.internal.ToStringPlanExtension;

/**
 * Implements {@code toString} for shapes, redacting sensitive data.
 *
 * <p>The output has the format of the SDK's generated {@code toString} methods:
 * <ul>
 *     <li>structures and unions: {@code ShapeName(Member=value, Member2=value2)}, with the Smithy member names in the
 *     order the shape writes them (schema member order for generated shapes). Members written as null, and members
 *     targeting a streaming blob or an event stream, are left out; a streaming member's value is never read. For a
 *     schema with a {@linkplain Schema#shapeClass() shape class} (a generated SDK class), the name is the class's simple
 *     name and each member name has its first character upper-cased, which is the output of the SDK's previous
 *     generated {@code toString} methods.</li>
 *     <li>lists: {@code [a, b]}; maps: {@code {key=value, key2=value2}}; null elements and values: {@code null}.</li>
 *     <li>blobs: {@code SdkBytes(bytes=0x<lowercase hex>)}; timestamps: {@link Instant#toString()}; strings, enums
 *     and numbers: their value.</li>
 *     <li>documents: the format of the SDK's {@code software.amazon.awssdk.core.document.Document#toString()}, for
 *     example {@code {"key": "value","n": 1}}.</li>
 * </ul>
 *
 * <p>A value whose schema is sensitive (the {@code sensitive} trait on the member or on its target shape) is written
 * as {@value #REDACTED}. A list or map whose elements, keys or values are sensitive is redacted as a whole. This
 * applies at every nesting depth.
 */
@SdkPublicApi
public final class ToStringSerializer implements ShapeSerializer {

    /**
     * The placeholder written in place of a sensitive value.
     */
    public static final String REDACTED = "*** Sensitive Data Redacted ***";

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private final StringBuilder builder = new StringBuilder();

    /**
     * Writes a shape to a string.
     *
     * @param shape the shape to write
     * @return the string form of the shape
     */
    public static String serialize(SerializableShape shape) {
        ToStringSerializer serializer = new ToStringSerializer();
        shape.serialize(serializer);
        return serializer.toString();
    }

    @Override
    public void close() {
        builder.setLength(0);
        builder.trimToSize();
    }

    /**
     * Returns everything written so far.
     */
    @Override
    public String toString() {
        return builder.toString();
    }

    private static boolean isRedacted(Schema schema) {
        return ToStringPlanExtension.plan(schema).redact();
    }

    private void append(Schema schema, Object value) {
        if (isRedacted(schema)) {
            builder.append(REDACTED);
        } else {
            builder.append(value);
        }
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        if (isRedacted(schema)) {
            builder.append(REDACTED);
            return;
        }
        Schema root = schema.isMember() ? schema.memberTarget() : schema;
        ToStringPlanExtension.Plan structure = ToStringPlanExtension.plan(root);
        builder.append(structure.structureName() != null ? structure.structureName() : root.id().getName()).append('(');
        struct.serializeMembers(new StructureWriter(this, structure.usesSdkNames()));
        builder.append(')');
    }

    @Override
    public <T> void writeList(Schema schema, T state, int size, BiConsumer<T, ShapeSerializer> consumer) {
        if (isRedacted(schema)) {
            builder.append(REDACTED);
            return;
        }
        builder.append('[');
        consumer.accept(state, new ListSerializer(this, this::writeComma));
        builder.append(']');
    }

    private void writeComma(int position) {
        if (position > 0) {
            builder.append(", ");
        }
    }

    @Override
    public <T> void writeMap(Schema schema, T state, int size, BiConsumer<T, MapSerializer> consumer) {
        if (isRedacted(schema)) {
            builder.append(REDACTED);
            return;
        }
        builder.append('{');
        consumer.accept(state, new ToStringMapSerializer(this));
        builder.append('}');
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        append(schema, value);
    }

    @Override
    public void writeShort(Schema schema, short value) {
        append(schema, value);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        append(schema, value);
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        append(schema, value);
    }

    @Override
    public void writeLong(Schema schema, long value) {
        append(schema, value);
    }

    @Override
    public void writeFloat(Schema schema, float value) {
        append(schema, value);
    }

    @Override
    public void writeDouble(Schema schema, double value) {
        append(schema, value);
    }

    @Override
    public void writeBigInteger(Schema schema, BigInteger value) {
        append(schema, value);
    }

    @Override
    public void writeBigDecimal(Schema schema, BigDecimal value) {
        append(schema, value);
    }

    @Override
    public void writeString(Schema schema, String value) {
        append(schema, value);
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        if (isRedacted(schema)) {
            builder.append(REDACTED);
        } else {
            appendBlob(value);
        }
    }

    private void appendBlob(ByteBuffer value) {
        // Same as SdkBytes.toString(). Absolute reads leave the buffer's position unchanged.
        builder.append("SdkBytes(bytes=0x");
        for (int i = value.position(); i < value.limit(); i++) {
            int b = value.get(i);
            builder.append(HEX_DIGITS[(b >> 4) & 0xF]).append(HEX_DIGITS[b & 0xF]);
        }
        builder.append(')');
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        append(schema, value);
    }

    @Override
    public void writeDocument(Schema schema, Document value) {
        if (isRedacted(schema)) {
            builder.append(REDACTED);
        } else {
            appendDocument(value);
        }
    }

    private void appendDocument(Document document) {
        if (document == null) {
            builder.append("null");
            return;
        }
        switch (document.type()) {
            case BOOLEAN:
                builder.append(document.asBoolean());
                break;
            case BYTE:
            case SHORT:
            case INTEGER:
            case INT_ENUM:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_INTEGER:
            case BIG_DECIMAL:
                builder.append(document.asNumber());
                break;
            case STRING:
            case ENUM:
                builder.append('"')
                       .append(document.asString().replace("\\", "\\\\").replace("\"", "\\\""))
                       .append('"');
                break;
            case BLOB:
                appendBlob(document.asBlob());
                break;
            case TIMESTAMP:
                builder.append(document.asTimestamp());
                break;
            case LIST:
                appendDocumentList(document.asList());
                break;
            case MAP:
            case STRUCTURE:
            case UNION:
                appendDocumentMap(document.asMap());
                break;
            default:
                builder.append(document);
                break;
        }
    }

    private void appendDocumentList(List<Document> elements) {
        builder.append('[');
        Iterator<Document> iterator = elements.iterator();
        while (iterator.hasNext()) {
            appendDocument(iterator.next());
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }

    private void appendDocumentMap(Map<String, Document> members) {
        builder.append('{');
        Iterator<Map.Entry<String, Document>> iterator = members.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Document> entry = iterator.next();
            builder.append('"').append(entry.getKey()).append("\": ");
            appendDocument(entry.getValue());
            if (iterator.hasNext()) {
                builder.append(',');
            }
        }
        builder.append('}');
    }

    @Override
    public void writeNull(Schema schema) {
        builder.append("null");
    }

    /**
     * Writes the members of a structure, each preceded by its name. Null and streaming members are skipped.
     */
    private static final class StructureWriter implements ShapeSerializer {
        private final ToStringSerializer parent;
        private final boolean sdkNames;
        private boolean isFirst = true;

        private StructureWriter(ToStringSerializer parent, boolean sdkNames) {
            this.parent = parent;
            this.sdkNames = sdkNames;
        }

        private boolean begin(Schema member) {
            ToStringPlanExtension.Plan plan = ToStringPlanExtension.plan(member);
            if (plan.omit()) {
                return false;
            }
            if (isFirst) {
                isFirst = false;
            } else {
                parent.builder.append(", ");
            }
            parent.builder.append(plan.memberName(sdkNames)).append('=');
            return true;
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            if (begin(schema)) {
                parent.writeStruct(schema, struct);
            }
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            if (begin(schema)) {
                parent.writeList(schema, listState, size, consumer);
            }
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            if (begin(schema)) {
                parent.writeMap(schema, mapState, size, consumer);
            }
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            if (begin(schema)) {
                parent.writeBoolean(schema, value);
            }
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            if (begin(schema)) {
                parent.writeByte(schema, value);
            }
        }

        @Override
        public void writeShort(Schema schema, short value) {
            if (begin(schema)) {
                parent.writeShort(schema, value);
            }
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            if (begin(schema)) {
                parent.writeInteger(schema, value);
            }
        }

        @Override
        public void writeLong(Schema schema, long value) {
            if (begin(schema)) {
                parent.writeLong(schema, value);
            }
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            if (begin(schema)) {
                parent.writeFloat(schema, value);
            }
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            if (begin(schema)) {
                parent.writeDouble(schema, value);
            }
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            if (begin(schema)) {
                parent.writeBigInteger(schema, value);
            }
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            if (begin(schema)) {
                parent.writeBigDecimal(schema, value);
            }
        }

        @Override
        public void writeString(Schema schema, String value) {
            if (begin(schema)) {
                parent.writeString(schema, value);
            }
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            if (begin(schema)) {
                parent.writeBlob(schema, value);
            }
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            if (begin(schema)) {
                parent.writeTimestamp(schema, value);
            }
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            if (begin(schema)) {
                parent.writeDocument(schema, value);
            }
        }

        @Override
        public void writeNull(Schema schema) {
            // An unset member is left out, as in the SDK's generated toString methods.
        }
    }

    private static final class ToStringMapSerializer implements MapSerializer {
        private final ToStringSerializer serializer;
        private boolean isFirst = true;

        ToStringMapSerializer(ToStringSerializer serializer) {
            this.serializer = serializer;
        }

        @Override
        public <T> void writeEntry(Schema keySchema, String key, T state, BiConsumer<T, ShapeSerializer> valueSerializer) {
            if (isFirst) {
                isFirst = false;
            } else {
                serializer.builder.append(", ");
            }
            serializer.append(keySchema, key);
            serializer.builder.append('=');
            valueSerializer.accept(state, serializer);
        }
    }
}
