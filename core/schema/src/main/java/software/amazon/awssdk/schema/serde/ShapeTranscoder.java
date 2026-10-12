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
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.serde.internal.ShapeTranscoderSchemaExtensions;

/**
 * Converts one serializable shape into another without an intermediate wire format or document tree.
 *
 * <p>A transcoder synchronously connects the calls a source shape makes to a {@link ShapeSerializer} to the calls a
 * target builder makes to a {@link ShapeDeserializer}. Structure and union members are matched by their Smithy
 * member names. During error-correcting conversion, members that do not exist in the target schema are reported to
 * {@link ShapeDeserializer.StructMemberConsumer#unknownMember(Object, String)}. Strict conversion drops source
 * members that do not exist in the target and rejects incompatible or lossy conversions.
 *
 * <p>A read whose type does not match the value the source wrote throws a {@link SerializationException} naming the
 * ShapeId being read, and the target is not built.
 *
 * <p>The deserializers passed to consumers are only valid for the duration of the consumer call. This matches how
 * generated builders consume deserializers and lets a transcoder reuse cursors based on maximum nesting depth rather
 * than allocating a deserializer for every value.
 *
 * <p>Instances are reusable, but are not thread-safe and cannot perform nested conversions. Use {@link #convert} to
 * create a transcoder for a single conversion, or reuse an instance with {@link #transcode} to keep its cursor pool
 * between conversions. The corresponding strict operations are {@link #convertStrict} and {@link #transcodeStrict}.
 */
@SdkPublicApi
public final class ShapeTranscoder {

    private static final int INITIAL_CURSOR_CAPACITY = 8;

    private Cursor[] cursors = new Cursor[INITIAL_CURSOR_CAPACITY];
    private boolean active;
    private boolean strict;

    /**
     * Converts a shape into another generated shape.
     *
     * @param source Source shape to serialize.
     * @param target Target builder to deserialize into.
     * @param <T> Target shape type.
     * @return the built and error-corrected target shape.
     */
    public static <T extends SerializableShape> T convert(SerializableShape source, ShapeBuilder<T> target) {
        return new ShapeTranscoder().transcode(source, target);
    }

    /**
     * Strictly converts a shape into another generated shape.
     *
     * <p>Strict conversion drops source members that do not exist in the target and rejects incompatible or lossy
     * conversions. Lossless numeric conversions between different Smithy shape types are allowed. The target is
     * built without applying client error correction.
     *
     * @param source Source shape to serialize.
     * @param target Target builder to deserialize into.
     * @param <T> Target shape type.
     * @return the built target shape.
     */
    public static <T extends SerializableShape> T convertStrict(SerializableShape source, ShapeBuilder<T> target) {
        return new ShapeTranscoder().transcodeStrict(source, target);
    }

    /**
     * Converts a shape, reusing this transcoder's cursors.
     *
     * @param source Source shape to serialize.
     * @param target Target builder to deserialize into.
     * @param <T> Target shape type.
     * @return the built and error-corrected target shape.
     * @throws IllegalStateException if this transcoder is already performing a conversion.
     */
    public <T extends SerializableShape> T transcode(SerializableShape source, ShapeBuilder<T> target) {
        return transcode(source, target, false);
    }

    /**
     * Strictly converts a shape, reusing this transcoder's cursors.
     *
     * @param source Source shape to serialize.
     * @param target Target builder to deserialize into.
     * @param <T> Target shape type.
     * @return the built target shape.
     * @throws IllegalStateException if this transcoder is already performing a conversion.
     */
    public <T extends SerializableShape> T transcodeStrict(SerializableShape source, ShapeBuilder<T> target) {
        return transcode(source, target, true);
    }

    private <T extends SerializableShape> T transcode(SerializableShape source, ShapeBuilder<T> target, boolean strict) {
        Objects.requireNonNull(source, "source is null");
        Objects.requireNonNull(target, "target is null");
        if (active) {
            throw new IllegalStateException("ShapeTranscoder is already performing a conversion");
        }

        active = true;
        this.strict = strict;
        Cursor root = cursor(0);
        try {
            Schema targetSchema = target.sdkSchema();
            root.expectedSchema = targetSchema;
            source.serialize(root);
            root.requireValue();
            if (strict) {
                root.requireCompatibleSchema(targetSchema);
            }
            target.deserialize(root);
        } finally {
            root.clearChain();
            this.strict = false;
            active = false;
        }

        return strict ? target.build() : target.errorCorrection().build();
    }

    private Cursor cursor(int depth) {
        if (depth == cursors.length) {
            cursors = Arrays.copyOf(cursors, cursors.length << 1);
        }

        Cursor cursor = cursors[depth];
        if (cursor == null) {
            cursor = new Cursor(this, depth);
            cursors[depth] = cursor;
        }

        return cursor;
    }

    private enum Kind {
        EMPTY,
        NULL,
        BOOLEAN,
        BYTE,
        SHORT,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE,
        BIG_INTEGER,
        BIG_DECIMAL,
        STRING,
        BLOB,
        TIMESTAMP,
        DOCUMENT,
        STRUCT,
        LIST,
        MAP
    }

    private enum Mode {
        CAPTURE, STRUCT, LIST, MAP
    }

    private static final class Cursor implements ShapeSerializer, ShapeDeserializer, MapSerializer {

        private final ShapeTranscoder owner;
        private final int depth;
        private Cursor child;

        private Kind kind = Kind.EMPTY;
        private Mode mode = Mode.CAPTURE;
        private boolean booleanValue;
        private long integralValue;
        private double floatingPointValue;
        private Object objectValue;
        private Schema sourceSchema;
        private Object sourceState;
        private int containerSize = -1;
        private BiConsumer<Object, ShapeSerializer> listWriter;
        private BiConsumer<Object, MapSerializer> mapWriter;

        // The target schema this cursor's value is read as: the target member, or the target builder's schema at the
        // root. Used to name the shape in errors raised by reads that take no schema.
        private Schema expectedSchema;

        private Schema targetMember;
        private ShapeTranscoderSchemaExtensions.MemberMapping memberMapping;
        private ShapeTranscoderSchemaExtensions.MemberMapping cachedMemberMapping;
        private Object targetState;
        private Schema targetMapKey;
        private StructMemberConsumer<Object> structReader;
        private ListMemberConsumer<Object> listReader;
        private MapMemberConsumer<String, Object> mapReader;

        Cursor(ShapeTranscoder owner, int depth) {
            this.owner = owner;
            this.depth = depth;
        }

        @Override
        public void close() {
        }

        void clearChain() {
            for (Cursor cursor = this; cursor != null; cursor = cursor.child) {
                cursor.clear();
            }
        }

        private void clear() {
            kind = Kind.EMPTY;
            mode = Mode.CAPTURE;
            objectValue = null;
            sourceSchema = null;
            sourceState = null;
            containerSize = -1;
            listWriter = null;
            mapWriter = null;
            expectedSchema = null;
            targetMember = null;
            memberMapping = null;
            targetState = null;
            targetMapKey = null;
            structReader = null;
            listReader = null;
            mapReader = null;
        }

        void requireValue() {
            if (kind == Kind.EMPTY) {
                throw new SerializationException("Shape did not serialize a value for " + describe(expectedSchema));
            }
        }

        private void require(Kind expected, Schema targetSchema) {
            if (kind != expected) {
                throw new SerializationException(
                    "Expected " + expected + " for " + describe(targetSchema) + ", but found " + kind);
            }
            requireCompatibleSchema(targetSchema);
        }

        private SerializationException notANumber(Schema targetSchema) {
            return new SerializationException("Expected a number for " + describe(targetSchema) + ", but found " + kind);
        }

        private String describe(Schema schema) {
            Schema described = schema != null ? schema : expectedSchema;
            return described != null ? described.id().toString() : "<unknown shape>";
        }

        private void requireCompatibleSchema(Schema targetSchema) {
            if (!owner.strict || sourceSchema.type() == targetSchema.type()) {
                return;
            }
            if (isNumericType(sourceSchema.type())
                && isNumericType(targetSchema.type())
                && isLosslessNumericConversion(targetSchema.type())) {
                return;
            }
            throw incompatibleTypes(sourceSchema, targetSchema);
        }

        private static void requireMatchingSchemaTypes(Schema sourceSchema, Schema targetSchema) {
            if (sourceSchema.type() != targetSchema.type()) {
                throw incompatibleTypes(sourceSchema, targetSchema);
            }
        }

        private static SerializationException incompatibleTypes(Schema sourceSchema, Schema targetSchema) {
            return new SerializationException(
                "Strict conversion cannot losslessly convert " + sourceSchema.type() + " to " + targetSchema.type()
                + " for " + targetSchema.id());
        }

        private static boolean isNumericType(ShapeType type) {
            switch (type) {
                case BYTE:
                case SHORT:
                case INTEGER:
                case INT_ENUM:
                case LONG:
                case FLOAT:
                case DOUBLE:
                case BIG_INTEGER:
                case BIG_DECIMAL:
                    return true;
                default:
                    return false;
            }
        }

        private boolean isLosslessNumericConversion(ShapeType targetType) {
            if (kind == Kind.NULL) {
                return true;
            }
            switch (targetType) {
                case BYTE:
                    return isExactInteger(Byte.MIN_VALUE, Byte.MAX_VALUE);
                case SHORT:
                    return isExactInteger(Short.MIN_VALUE, Short.MAX_VALUE);
                case INTEGER:
                case INT_ENUM:
                    return isExactInteger(Integer.MIN_VALUE, Integer.MAX_VALUE);
                case LONG:
                    return isExactInteger(Long.MIN_VALUE, Long.MAX_VALUE);
                case FLOAT:
                    return isExactFloatingPoint(true);
                case DOUBLE:
                    return isExactFloatingPoint(false);
                case BIG_INTEGER:
                    return isExactBigInteger();
                case BIG_DECIMAL:
                    return isExactBigDecimal();
                default:
                    return false;
            }
        }

        private boolean isExactInteger(long minimum, long maximum) {
            BigDecimal value = exactNumericValue();
            if (value == null) {
                return false;
            }
            try {
                BigInteger integer = value.toBigIntegerExact();
                return integer.compareTo(BigInteger.valueOf(minimum)) >= 0
                       && integer.compareTo(BigInteger.valueOf(maximum)) <= 0;
            } catch (ArithmeticException e) {
                return false;
            }
        }

        private boolean isExactBigInteger() {
            BigDecimal value = exactNumericValue();
            if (value == null) {
                return false;
            }
            try {
                BigInteger integer = value.toBigIntegerExact();
                if (kind == Kind.FLOAT || kind == Kind.DOUBLE) {
                    // readBigInteger converts floating-point values through a long.
                    return integer.equals(BigInteger.valueOf((long) floatingPointValue));
                }
                return true;
            } catch (ArithmeticException e) {
                return false;
            }
        }

        private boolean isExactBigDecimal() {
            switch (kind) {
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                case BIG_INTEGER:
                case BIG_DECIMAL:
                    return true;
                case FLOAT:
                case DOUBLE:
                    return isFinite(floatingPointValue);
                default:
                    return false;
            }
        }

        private boolean isExactFloatingPoint(boolean targetFloat) {
            double converted;
            switch (kind) {
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                    converted = targetFloat ? (float) integralValue : (double) integralValue;
                    break;
                case FLOAT:
                case DOUBLE:
                    converted = targetFloat ? (float) floatingPointValue : floatingPointValue;
                    break;
                case BIG_INTEGER:
                    converted = targetFloat
                                ? ((BigInteger) objectValue).floatValue()
                                : ((BigInteger) objectValue).doubleValue();
                    break;
                case BIG_DECIMAL:
                    converted = targetFloat
                                ? ((BigDecimal) objectValue).floatValue()
                                : ((BigDecimal) objectValue).doubleValue();
                    break;
                default:
                    converted = Double.NaN;
                    break;
            }
            if ((kind == Kind.FLOAT || kind == Kind.DOUBLE) && !isFinite(floatingPointValue)) {
                // NaN converts to NaN; an infinity converts to the same infinity.
                return Double.compare(converted, floatingPointValue) == 0;
            }
            if (!isFinite(converted)) {
                return false;
            }
            BigDecimal value = exactNumericValue();
            return value != null && value.compareTo(new BigDecimal(converted)) == 0;
        }

        private static boolean isFinite(double value) {
            return !Double.isNaN(value) && !Double.isInfinite(value);
        }

        private BigDecimal exactNumericValue() {
            switch (kind) {
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                    return BigDecimal.valueOf(integralValue);
                case FLOAT:
                case DOUBLE:
                    return isFinite(floatingPointValue) ? new BigDecimal(floatingPointValue) : null;
                case BIG_INTEGER:
                    return new BigDecimal((BigInteger) objectValue);
                case BIG_DECIMAL:
                    return (BigDecimal) objectValue;
                default:
                    return null;
            }
        }

        private void prepareCapture(Schema schema, Kind capturedKind) {
            if (mode != Mode.CAPTURE) {
                throw new SerializationException(
                    "Cannot capture a value of " + schema.id() + " while forwarding " + mode);
            } else if (kind != Kind.EMPTY) {
                throw new SerializationException(
                    "A shape serialized more than one value; found " + schema.id() + " after " + kind);
            }

            sourceSchema = schema;
            kind = capturedKind;
        }

        private Cursor beginValue(Schema sourceSchema) {
            switch (mode) {
                case STRUCT:
                    targetMember = memberMapping.targetMember(sourceSchema);
                    if (targetMember == null) {
                        if (!owner.strict) {
                            structReader.unknownMember(targetState, sourceSchema.memberName());
                        }
                        return null;
                    }
                    break;
                case LIST:
                    break;
                default:
                    throw new SerializationException(
                        "Cannot write a value of " + sourceSchema.id() + " while forwarding " + mode);
            }

            Cursor result = child();
            result.sourceSchema = sourceSchema;
            result.expectedSchema = targetMember;
            return result;
        }

        private Cursor child() {
            Cursor result = child;
            if (result == null) {
                result = owner.cursor(depth + 1);
                child = result;
            }
            return result;
        }

        private void emitValue(Cursor value) {
            if (owner.strict) {
                value.requireCompatibleSchema(targetMember);
            }
            switch (mode) {
                case STRUCT:
                    structReader.accept(targetState, targetMember, value);
                    break;
                case LIST:
                    listReader.accept(targetState, value);
                    break;
                default:
                    throw new SerializationException("Cannot emit a value while forwarding " + mode);
            }
        }

        @Override
        public void writeStruct(Schema schema, SerializableStruct struct) {
            if (mode == Mode.CAPTURE) {
                prepareCapture(schema, Kind.STRUCT);
                objectValue = struct;
                return;
            }
            Cursor value = beginValue(schema);
            if (value != null) {
                value.kind = Kind.STRUCT;
                value.objectValue = struct;
                emitValue(value);
            }
        }

        @Override
        public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
            if (mode == Mode.CAPTURE) {
                prepareCapture(schema, Kind.LIST);
                captureListUnchecked(listState, size, consumer);
                return;
            }
            Cursor value = beginValue(schema);
            if (value != null) {
                value.captureListUnchecked(listState, size, consumer);
                emitValue(value);
            }
        }

        @SuppressWarnings("unchecked")
        private <T> void captureListUnchecked(T state, int size, BiConsumer<T, ShapeSerializer> consumer) {
            kind = Kind.LIST;
            sourceState = state;
            containerSize = size;
            listWriter = (BiConsumer<Object, ShapeSerializer>) consumer;
        }

        @Override
        public <T> void writeMap(Schema schema, T mapState, int size, BiConsumer<T, MapSerializer> consumer) {
            if (mode == Mode.CAPTURE) {
                prepareCapture(schema, Kind.MAP);
                captureMapUnchecked(mapState, size, consumer);
                return;
            }
            Cursor value = beginValue(schema);
            if (value != null) {
                value.captureMapUnchecked(mapState, size, consumer);
                emitValue(value);
            }
        }

        @SuppressWarnings("unchecked")
        private <T> void captureMapUnchecked(T state, int size, BiConsumer<T, MapSerializer> consumer) {
            kind = Kind.MAP;
            sourceState = state;
            containerSize = size;
            mapWriter = (BiConsumer<Object, MapSerializer>) consumer;
        }

        @Override
        public void writeBoolean(Schema schema, boolean value) {
            if (mode == Mode.CAPTURE) {
                prepareCapture(schema, Kind.BOOLEAN);
                booleanValue = value;
            } else {
                Cursor cursor = beginValue(schema);
                if (cursor != null) {
                    cursor.kind = Kind.BOOLEAN;
                    cursor.booleanValue = value;
                    emitValue(cursor);
                }
            }
        }

        @Override
        public void writeByte(Schema schema, byte value) {
            writeIntegral(schema, Kind.BYTE, value);
        }

        @Override
        public void writeShort(Schema schema, short value) {
            writeIntegral(schema, Kind.SHORT, value);
        }

        @Override
        public void writeInteger(Schema schema, int value) {
            writeIntegral(schema, Kind.INTEGER, value);
        }

        @Override
        public void writeLong(Schema schema, long value) {
            writeIntegral(schema, Kind.LONG, value);
        }

        private void writeIntegral(Schema schema, Kind numberKind, long value) {
            if (mode == Mode.CAPTURE) {
                prepareCapture(schema, numberKind);
                integralValue = value;
            } else {
                Cursor cursor = beginValue(schema);
                if (cursor != null) {
                    cursor.kind = numberKind;
                    cursor.integralValue = value;
                    emitValue(cursor);
                }
            }
        }

        @Override
        public void writeFloat(Schema schema, float value) {
            writeFloatingPoint(schema, Kind.FLOAT, value);
        }

        @Override
        public void writeDouble(Schema schema, double value) {
            writeFloatingPoint(schema, Kind.DOUBLE, value);
        }

        private void writeFloatingPoint(Schema schema, Kind numberKind, double value) {
            if (mode == Mode.CAPTURE) {
                prepareCapture(schema, numberKind);
                floatingPointValue = value;
            } else {
                Cursor cursor = beginValue(schema);
                if (cursor != null) {
                    cursor.kind = numberKind;
                    cursor.floatingPointValue = value;
                    emitValue(cursor);
                }
            }
        }

        @Override
        public void writeBigInteger(Schema schema, BigInteger value) {
            writeObject(schema, Kind.BIG_INTEGER, value);
        }

        @Override
        public void writeBigDecimal(Schema schema, BigDecimal value) {
            writeObject(schema, Kind.BIG_DECIMAL, value);
        }

        @Override
        public void writeString(Schema schema, String value) {
            writeObject(schema, Kind.STRING, value);
        }

        @Override
        public void writeBlob(Schema schema, ByteBuffer value) {
            writeObject(schema, Kind.BLOB, value);
        }

        @Override
        public void writeTimestamp(Schema schema, Instant value) {
            writeObject(schema, Kind.TIMESTAMP, value);
        }

        @Override
        public void writeDocument(Schema schema, Document value) {
            writeObject(schema, Kind.DOCUMENT, value);
        }

        private void writeObject(Schema schema, Kind valueKind, Object value) {
            if (mode == Mode.CAPTURE) {
                prepareCapture(schema, valueKind);
                objectValue = value;
            } else {
                Cursor cursor = beginValue(schema);
                if (cursor != null) {
                    cursor.kind = valueKind;
                    cursor.objectValue = value;
                    emitValue(cursor);
                }
            }
        }

        @Override
        public void writeNull(Schema schema) {
            if (mode == Mode.CAPTURE) {
                prepareCapture(schema, Kind.NULL);
            } else {
                Cursor cursor = beginValue(schema);
                if (cursor != null) {
                    cursor.kind = Kind.NULL;
                    emitValue(cursor);
                }
            }
        }

        @Override
        public <T> void writeEntry(Schema keySchema, String key, T state, BiConsumer<T, ShapeSerializer> valueSerializer) {
            if (mode != Mode.MAP) {
                throw new SerializationException(
                    "Cannot write a map entry of " + keySchema.id() + " while forwarding " + mode);
            }

            Cursor value = child();
            value.kind = Kind.EMPTY;
            value.expectedSchema = targetMember;
            valueSerializer.accept(state, value);
            value.requireValue();
            if (owner.strict) {
                requireMatchingSchemaTypes(keySchema, targetMapKey);
                value.requireCompatibleSchema(targetMember);
            }

            mapReader.accept(targetState, key, value);
        }

        @Override
        public boolean readBoolean(Schema schema) {
            require(Kind.BOOLEAN, schema);
            return booleanValue;
        }

        @Override
        public byte readByte(Schema schema) {
            requireCompatibleSchema(schema);
            return (byte) readLongNumber(schema);
        }

        @Override
        public short readShort(Schema schema) {
            requireCompatibleSchema(schema);
            return (short) readLongNumber(schema);
        }

        @Override
        public int readInteger(Schema schema) {
            requireCompatibleSchema(schema);
            return (int) readLongNumber(schema);
        }

        @Override
        public long readLong(Schema schema) {
            requireCompatibleSchema(schema);
            return readLongNumber(schema);
        }

        private long readLongNumber(Schema schema) {
            switch (kind) {
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                    return integralValue;
                case FLOAT:
                case DOUBLE:
                    return (long) floatingPointValue;
                case BIG_INTEGER:
                    return ((BigInteger) objectValue).longValue();
                case BIG_DECIMAL:
                    return ((BigDecimal) objectValue).longValue();
                default:
                    throw notANumber(schema);
            }
        }

        @Override
        public float readFloat(Schema schema) {
            requireCompatibleSchema(schema);
            switch (kind) {
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                    return integralValue;
                case FLOAT:
                case DOUBLE:
                    return (float) floatingPointValue;
                case BIG_INTEGER:
                    return ((BigInteger) objectValue).floatValue();
                case BIG_DECIMAL:
                    return ((BigDecimal) objectValue).floatValue();
                default:
                    throw notANumber(schema);
            }
        }

        @Override
        public double readDouble(Schema schema) {
            requireCompatibleSchema(schema);
            switch (kind) {
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                    return integralValue;
                case FLOAT:
                case DOUBLE:
                    return floatingPointValue;
                case BIG_INTEGER:
                    return ((BigInteger) objectValue).doubleValue();
                case BIG_DECIMAL:
                    return ((BigDecimal) objectValue).doubleValue();
                default:
                    throw notANumber(schema);
            }
        }

        @Override
        public BigInteger readBigInteger(Schema schema) {
            requireCompatibleSchema(schema);
            switch (kind) {
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                    return BigInteger.valueOf(integralValue);
                case FLOAT:
                case DOUBLE:
                    return BigInteger.valueOf((long) floatingPointValue);
                case BIG_INTEGER:
                    return (BigInteger) objectValue;
                case BIG_DECIMAL:
                    return ((BigDecimal) objectValue).toBigInteger();
                default:
                    throw notANumber(schema);
            }
        }

        @Override
        public BigDecimal readBigDecimal(Schema schema) {
            requireCompatibleSchema(schema);
            switch (kind) {
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                    return BigDecimal.valueOf(integralValue);
                case FLOAT:
                case DOUBLE:
                    return BigDecimal.valueOf(floatingPointValue);
                case BIG_INTEGER:
                    return new BigDecimal((BigInteger) objectValue);
                case BIG_DECIMAL:
                    return (BigDecimal) objectValue;
                default:
                    throw notANumber(schema);
            }
        }

        @Override
        public String readString(Schema schema) {
            require(Kind.STRING, schema);
            return (String) objectValue;
        }

        @Override
        public ByteBuffer readBlob(Schema schema) {
            require(Kind.BLOB, schema);
            return (ByteBuffer) objectValue;
        }

        @Override
        public Instant readTimestamp(Schema schema) {
            require(Kind.TIMESTAMP, schema);
            return (Instant) objectValue;
        }

        @Override
        public Document readDocument() {
            requireKind(Kind.DOCUMENT);
            return (Document) objectValue;
        }

        private void requireKind(Kind expected) {
            if (kind != expected) {
                throw new SerializationException(
                    "Expected " + expected + " for " + describe(null) + ", but found " + kind);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
            require(Kind.STRUCT, schema);
            SerializableStruct struct = (SerializableStruct) objectValue;
            mode = Mode.STRUCT;
            Schema structSchema = struct.sdkSchema();
            ShapeTranscoderSchemaExtensions.MemberMapping mapping = cachedMemberMapping;
            if (mapping == null || !mapping.matches(structSchema, schema)) {
                mapping = ShapeTranscoderSchemaExtensions.mapping(structSchema, schema);
                cachedMemberMapping = mapping;
            }

            memberMapping = mapping;
            targetState = state;
            structReader = (StructMemberConsumer<Object>) consumer;

            try {
                struct.serializeMembers(this);
            } finally {
                clearForwardingState();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
            require(Kind.LIST, schema);
            mode = Mode.LIST;
            targetMember = schema.listMember();
            targetState = state;
            listReader = (ListMemberConsumer<Object>) consumer;

            try {
                listWriter.accept(sourceState, this);
            } finally {
                clearForwardingState();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
            require(Kind.MAP, schema);
            mode = Mode.MAP;
            targetMapKey = schema.mapKeyMember();
            targetMember = schema.mapValueMember();
            targetState = state;
            mapReader = (MapMemberConsumer<String, Object>) consumer;

            try {
                mapWriter.accept(sourceState, this);
            } finally {
                clearForwardingState();
            }
        }

        private void clearForwardingState() {
            Mode previousMode = mode;
            mode = Mode.CAPTURE;
            targetState = null;
            targetMember = null;
            switch (previousMode) {
                case STRUCT:
                    memberMapping = null;
                    structReader = null;
                    break;
                case LIST:
                    listReader = null;
                    break;
                case MAP:
                    targetMapKey = null;
                    mapReader = null;
                    break;
                default:
                    break;
            }
        }

        @Override
        public int containerSize() {
            return kind == Kind.LIST || kind == Kind.MAP ? containerSize : -1;
        }

        @Override
        public boolean isNull() {
            return kind == Kind.NULL;
        }

        @Override
        public <T> T readNull() {
            requireKind(Kind.NULL);
            return null;
        }
    }
}
