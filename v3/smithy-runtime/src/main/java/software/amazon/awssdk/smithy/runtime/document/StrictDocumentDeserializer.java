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
import software.amazon.awssdk.smithy.runtime.document.DocumentCoercionException.Reason;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.document.DiscriminatorException;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * A schema-checked view of a document's own deserializer (Requirements 6.3, 6.10 and 6.14).
 *
 * <p>The wrapped deserializer is the one the document creates for itself ({@link Document#createDeserializer()}), so
 * the document's protocol context keeps applying: a JSON document maps members to JSON field names, decodes base64
 * blobs and parses timestamps in the codec's formats, and parses {@code __type} discriminators in the codec's default
 * namespace. On top of it this view:
 *
 * <ul>
 *     <li>accepts only document kinds that can represent the requested schema, and fails with
 *         {@link Reason#INCOMPATIBLE_KIND} otherwise;</li>
 *     <li>converts numbers exactly: an integral target accepts an integral value within its range (failing with
 *         {@link Reason#NOT_INTEGRAL} or {@link Reason#OUT_OF_RANGE}); a float or double target accepts any finite
 *         value within its finite range, rounding to nearest; a bigDecimal accepts any finite value; NaN and
 *         infinities are kept only by float and double, which also read them from the string protocol
 *         representation {@code "NaN"}, {@code "Infinity"} and {@code "-Infinity"} (as JSON writes them);</li>
 *     <li>reads a blob or timestamp from its native kind, or from the document's protocol representation (for
 *         example a base64 string or epoch-seconds number), failing with {@link Reason#INVALID_REPRESENTATION};</li>
 *     <li>rejects a structure or union document whose discriminator names a different shape than the requested
 *         schema ({@link Reason#DISCRIMINATOR_MISMATCH}), and a union document that sets more than one member;</li>
 *     <li>returns blobs as read-only buffers so the document's bytes cannot be changed through them.</li>
 * </ul>
 *
 * <p>Every failure is a {@link DocumentCoercionException} carrying the value's path.
 */
final class StrictDocumentDeserializer implements ShapeDeserializer {

    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private final ShapeDeserializer delegate;
    private final String path;
    private final Document node;

    StrictDocumentDeserializer(Document document, String path) {
        this(document.createDeserializer(), path, document);
    }

    private StrictDocumentDeserializer(ShapeDeserializer delegate, String path, Document node) {
        this.delegate = delegate;
        this.path = path;
        this.node = node;
    }

    /** Wraps the deserializer of a member, element or entry; its document is the delegate's current value. */
    private static StrictDocumentDeserializer child(ShapeDeserializer delegate, String path) {
        return new StrictDocumentDeserializer(delegate, path, delegate.readDocument());
    }

    private static Schema target(Schema schema) {
        return schema.isMember() ? schema.memberTarget() : schema;
    }

    private DocumentCoercionException error(Reason reason, Schema schema, ShapeId discriminator, Throwable cause) {
        Schema target = target(schema);
        return new DocumentCoercionException(reason, node == null ? null : node.type(), target.id(), target.type(),
                                             discriminator, path, cause);
    }

    private DocumentCoercionException error(Reason reason, Schema schema) {
        return error(reason, schema, null, null);
    }

    private ShapeType kind(Schema schema) {
        if (node == null) {
            throw error(Reason.INCOMPATIBLE_KIND, schema);
        }
        return node.type();
    }

    // ---- Scalars ----

    @Override
    public boolean readBoolean(Schema schema) {
        if (kind(schema) != ShapeType.BOOLEAN) {
            throw error(Reason.INCOMPATIBLE_KIND, schema);
        }
        return node.asBoolean();
    }

    @Override
    public String readString(Schema schema) {
        ShapeType kind = kind(schema);
        if (kind != ShapeType.STRING && kind != ShapeType.ENUM) {
            throw error(Reason.INCOMPATIBLE_KIND, schema);
        }
        return node.asString();
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        ShapeType kind = kind(schema);
        if (kind == ShapeType.BLOB) {
            return node.asBlob().asReadOnlyBuffer();
        }
        if (kind != ShapeType.STRING) {
            throw error(Reason.INCOMPATIBLE_KIND, schema);
        }
        try {
            return delegate.readBlob(schema).asReadOnlyBuffer();
        } catch (RuntimeException e) {
            throw error(Reason.INVALID_REPRESENTATION, schema, null, e);
        }
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        ShapeType kind = kind(schema);
        if (kind == ShapeType.TIMESTAMP) {
            return node.asTimestamp();
        }
        if (kind != ShapeType.STRING && !isNumber(kind)) {
            throw error(Reason.INCOMPATIBLE_KIND, schema);
        }
        try {
            Instant result = delegate.readTimestamp(schema);
            if (result == null) {
                throw error(Reason.INVALID_REPRESENTATION, schema);
            }
            return result;
        } catch (DocumentCoercionException e) {
            throw e;
        } catch (RuntimeException e) {
            throw error(Reason.INVALID_REPRESENTATION, schema, null, e);
        }
    }

    // ---- Numbers ----

    private static boolean isNumber(ShapeType kind) {
        return switch (kind) {
            case BYTE, SHORT, INTEGER, INT_ENUM, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL -> true;
            default -> false;
        };
    }

    /** The exact numeric value of the document: Long, BigInteger, Float, Double or BigDecimal. */
    private Number exactNumber(Schema schema) {
        return switch (kind(schema)) {
            case BYTE, SHORT, INTEGER, INT_ENUM, LONG -> node.asLong();
            case BIG_INTEGER -> node.asBigInteger();
            case FLOAT -> node.asFloat();
            case DOUBLE -> node.asDouble();
            case BIG_DECIMAL -> node.asBigDecimal();
            default -> throw error(Reason.INCOMPATIBLE_KIND, schema);
        };
    }

    /** The exact integral value of the document, failing if it has a fraction, is NaN or is infinite. */
    private BigInteger exactInteger(Schema schema) {
        Number value = exactNumber(schema);
        if (value instanceof Long l) {
            return BigInteger.valueOf(l);
        }
        if (value instanceof BigInteger bigInteger) {
            return bigInteger;
        }
        BigDecimal decimal;
        if (value instanceof BigDecimal bigDecimal) {
            decimal = bigDecimal;
        } else {
            double d = value.doubleValue();
            if (Double.isNaN(d)) {
                throw error(Reason.NOT_INTEGRAL, schema);
            }
            if (Double.isInfinite(d)) {
                throw error(Reason.OUT_OF_RANGE, schema);
            }
            decimal = new BigDecimal(d);
        }
        try {
            return decimal.toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw error(Reason.NOT_INTEGRAL, schema, null, e);
        }
    }

    private long integral(Schema schema, long min, long max) {
        Number value = exactNumber(schema);
        if (value instanceof Long l) {
            if (l < min || l > max) {
                throw error(Reason.OUT_OF_RANGE, schema);
            }
            return l;
        }
        BigInteger exact = exactInteger(schema);
        if (exact.compareTo(LONG_MIN) < 0 || exact.compareTo(LONG_MAX) > 0) {
            throw error(Reason.OUT_OF_RANGE, schema);
        }
        long l = exact.longValue();
        if (l < min || l > max) {
            throw error(Reason.OUT_OF_RANGE, schema);
        }
        return l;
    }

    @Override
    public byte readByte(Schema schema) {
        return (byte) integral(schema, Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    @Override
    public short readShort(Schema schema) {
        return (short) integral(schema, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    @Override
    public int readInteger(Schema schema) {
        return (int) integral(schema, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public long readLong(Schema schema) {
        return integral(schema, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        return exactInteger(schema);
    }

    /**
     * The non-finite value of a string document in the representation Smithy protocols use for non-finite floats
     * ({@code "NaN"}, {@code "Infinity"} or {@code "-Infinity"}; for example a JSON document), or {@code null}.
     */
    private Double nonFinite(Schema schema) {
        if (kind(schema) != ShapeType.STRING) {
            return null;
        }
        return switch (node.asString()) {
            case "NaN" -> Double.NaN;
            case "Infinity" -> Double.POSITIVE_INFINITY;
            case "-Infinity" -> Double.NEGATIVE_INFINITY;
            default -> null;
        };
    }

    @Override
    public float readFloat(Schema schema) {
        Double nonFinite = nonFinite(schema);
        if (nonFinite != null) {
            return nonFinite.floatValue();
        }
        Number value = exactNumber(schema);
        if (value instanceof Float f) {
            return f;
        }
        if (value instanceof Double d) {
            float f = (float) d.doubleValue();
            if (Float.isInfinite(f) && !d.isInfinite()) {
                throw error(Reason.OUT_OF_RANGE, schema);
            }
            return f;
        }
        float f = value.floatValue();
        if (Float.isInfinite(f)) {
            throw error(Reason.OUT_OF_RANGE, schema);
        }
        return f;
    }

    @Override
    public double readDouble(Schema schema) {
        Double nonFinite = nonFinite(schema);
        if (nonFinite != null) {
            return nonFinite;
        }
        Number value = exactNumber(schema);
        if (value instanceof Float || value instanceof Double) {
            return value.doubleValue();
        }
        double d = value.doubleValue();
        if (Double.isInfinite(d)) {
            throw error(Reason.OUT_OF_RANGE, schema);
        }
        return d;
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        Number value = exactNumber(schema);
        return switch (value) {
            case BigDecimal bigDecimal -> bigDecimal;
            case BigInteger bigInteger -> new BigDecimal(bigInteger);
            case Long l -> BigDecimal.valueOf(l);
            case Float f when Float.isFinite(f) -> new BigDecimal(Float.toString(f));
            case Double d when Double.isFinite(d) -> BigDecimal.valueOf(d);
            default -> throw error(Reason.OUT_OF_RANGE, schema);
        };
    }

    // ---- Documents, aggregates and collections ----

    @Override
    public Document readDocument() {
        return node;
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        ShapeType kind = kind(schema);
        if (kind != ShapeType.STRUCTURE && kind != ShapeType.UNION && kind != ShapeType.MAP) {
            throw error(Reason.INCOMPATIBLE_KIND, schema);
        }
        checkDiscriminator(schema);
        boolean union = schema.type() == ShapeType.UNION;
        int[] present = new int[1];
        delegate.readStruct(schema, state, new StructMemberConsumer<T>() {
            @Override
            public void accept(T memberState, Schema member, ShapeDeserializer memberDeserializer) {
                present[0]++;
                if (union && present[0] > 1) {
                    throw error(Reason.UNION_MEMBER_COUNT, schema);
                }
                consumer.accept(memberState, member, child(memberDeserializer, path + "." + member.memberName()));
            }

            @Override
            public void unknownMember(T memberState, String memberName) {
                consumer.unknownMember(memberState, memberName);
            }
        });
    }

    private void checkDiscriminator(Schema schema) {
        ShapeId discriminator;
        try {
            discriminator = node.discriminator();
        } catch (DiscriminatorException e) {
            throw error(Reason.INVALID_DISCRIMINATOR, schema, null, e);
        }
        if (discriminator == null) {
            return;
        }
        // Smithy Java's own nested structure documents use the member ID of the member they were written as.
        if (!discriminator.equals(target(schema).id()) && !discriminator.equals(schema.id())) {
            throw error(Reason.DISCRIMINATOR_MISMATCH, schema, discriminator, null);
        }
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        ShapeType kind = kind(schema);
        if (kind != ShapeType.LIST && kind != ShapeType.SET) {
            throw error(Reason.INCOMPATIBLE_KIND, schema);
        }
        int[] index = new int[1];
        delegate.readList(schema, state, (listState, elementDeserializer) -> {
            String elementPath = path + "[" + index[0]++ + "]";
            consumer.accept(listState, child(elementDeserializer, elementPath));
        });
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        if (kind(schema) != ShapeType.MAP) {
            throw error(Reason.INCOMPATIBLE_KIND, schema);
        }
        delegate.readStringMap(schema, state, (mapState, key, valueDeserializer) -> {
            consumer.accept(mapState, key, child(valueDeserializer, path + "[\"" + key + "\"]"));
        });
    }

    @Override
    public int containerSize() {
        return delegate.containerSize();
    }

    @Override
    public boolean isNull() {
        return node == null;
    }

    @Override
    public <T> T readNull() {
        return delegate.readNull();
    }

    @Override
    public DataStream readDataStream(Schema schema) {
        return delegate.readDataStream(schema);
    }

    @Override
    public EventStream<? extends SerializableStruct> readEventStream(Schema schema) {
        return delegate.readEventStream(schema);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
