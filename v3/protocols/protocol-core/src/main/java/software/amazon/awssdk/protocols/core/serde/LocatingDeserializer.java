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

package software.amazon.awssdk.protocols.core.serde;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.protocols.core.SdkProtocolException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableShape;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * A {@link ShapeDeserializer} decorator that tracks the member path being read and turns any codec failure into a
 * located {@link SdkProtocolException} (Requirements 5.9, 5.10). Every protocol runtime reads bodies (and, for
 * HTTP-bound protocols, bound members) through {@link #deserialize}.
 *
 * <p>The failure names the protocol, operation, the shape being read, the member path ({@code a.b[2].c{key}}) and the
 * wire location. The codec's own message is not copied because codecs quote wire input; only the exception type and
 * the expected shape type are reported. The builder is built only after the whole input was read, so a failure never
 * yields a partial value.
 *
 * <p>Streaming members ({@link #readDataStream}, {@link #readEventStream}) are passed through from the delegate
 * unread.
 *
 * <p>Not thread safe; one instance reads one message.
 */
public final class LocatingDeserializer implements ShapeDeserializer {
    /**
     * Reads a timestamp from the underlying deserializer, for protocols whose codec's timestamp reading must be
     * corrected (for example exact epoch-seconds, see {@link TimestampFormats#EXACT_EPOCH_SECONDS}).
     */
    @FunctionalInterface
    public interface TimestampReader {
        Instant read(Schema schema, ShapeDeserializer delegate);
    }

    private final ShapeDeserializer delegate;
    private final State state;

    private LocatingDeserializer(ShapeDeserializer delegate, State state) {
        this.delegate = delegate;
        this.state = state;
    }

    /**
     * Deserializes {@code builder}'s shape from {@code delegate}, then error-corrects and builds it.
     *
     * @param context the protocol and operation
     * @param wireLocation where the input comes from, for example {@link WireContext#BODY}
     * @param delegate the codec deserializer
     * @param builder a fresh builder of the target structure
     * @param timestamps the timestamp reader, or {@code null} to use the codec's
     * @return the built value
     * @throws SdkProtocolException if the input violates the protocol or schema
     */
    public static <T extends SerializableShape> T deserialize(WireContext context, String wireLocation,
                                                              ShapeDeserializer delegate, ShapeBuilder<T> builder,
                                                              TimestampReader timestamps) {
        return build(context, wireLocation, deserializeInto(context, wireLocation, delegate, builder, timestamps));
    }

    /**
     * Deserializes {@code delegate} into {@code builder} without building it, so that the caller can add
     * protocol-derived values (for example error metadata or HTTP-bound members) before {@link #build}.
     *
     * @return {@code builder}
     * @throws SdkProtocolException if the input violates the protocol or schema
     */
    public static <B extends ShapeBuilder<?>> B deserializeInto(WireContext context, String wireLocation,
                                                                ShapeDeserializer delegate, B builder,
                                                                TimestampReader timestamps) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(builder, "builder");
        State state = new State(builder.schema(), timestamps);
        LocatingDeserializer reader = new LocatingDeserializer(delegate, state);
        try {
            builder.deserialize(reader);
        } catch (SdkProtocolException | UnsupportedOperationException e) {
            throw e;
        } catch (RuntimeException | StackOverflowError e) {
            throw state.failure(context, wireLocation, e);
        }
        return builder;
    }

    /**
     * Error-corrects and builds a deserialized builder, reporting a build failure (such as a structural constraint
     * the builder enforces) as a root-level {@link SdkProtocolException}.
     */
    public static <T extends SerializableShape> T build(WireContext context, String wireLocation,
                                                        ShapeBuilder<T> builder) {
        try {
            return builder.errorCorrection().build();
        } catch (RuntimeException e) {
            throw context.failure(builder.schema().id(), List.of(), wireLocation,
                                  "the value does not satisfy " + builder.schema().id() + " ("
                                  + e.getClass().getSimpleName() + ")");
        }
    }

    private ShapeDeserializer wrap(ShapeDeserializer de) {
        return de == delegate || de == this ? this : new LocatingDeserializer(de, state);
    }

    @Override
    public boolean readBoolean(Schema schema) {
        return delegate.readBoolean(schema);
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        return delegate.readBlob(schema);
    }

    @Override
    public byte readByte(Schema schema) {
        return delegate.readByte(schema);
    }

    @Override
    public short readShort(Schema schema) {
        return delegate.readShort(schema);
    }

    @Override
    public int readInteger(Schema schema) {
        return delegate.readInteger(schema);
    }

    @Override
    public long readLong(Schema schema) {
        return delegate.readLong(schema);
    }

    @Override
    public float readFloat(Schema schema) {
        return delegate.readFloat(schema);
    }

    @Override
    public double readDouble(Schema schema) {
        return delegate.readDouble(schema);
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        return delegate.readBigInteger(schema);
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        return delegate.readBigDecimal(schema);
    }

    @Override
    public String readString(Schema schema) {
        return delegate.readString(schema);
    }

    @Override
    public Document readDocument() {
        return delegate.readDocument();
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        return state.timestamps == null ? delegate.readTimestamp(schema) : state.timestamps.read(schema, delegate);
    }

    @Override
    public <T> void readStruct(Schema schema, T target, StructMemberConsumer<T> consumer) {
        delegate.readStruct(schema, target, new StructMemberConsumer<T>() {
            @Override
            public void accept(T t, Schema member, ShapeDeserializer de) {
                state.push(member.memberName(), member);
                consumer.accept(t, member, wrap(de));
                state.pop();
            }

            @Override
            public void unknownMember(T t, String memberName) {
                consumer.unknownMember(t, memberName);
            }
        });
    }

    @Override
    public <T> void readList(Schema schema, T target, ListMemberConsumer<T> consumer) {
        Schema element = schema.listMember();
        int[] index = new int[1];
        delegate.readList(schema, target, (t, de) -> {
            state.push("[" + index[0]++ + "]", element);
            consumer.accept(t, wrap(de));
            state.pop();
        });
    }

    @Override
    public int containerSize() {
        return delegate.containerSize();
    }

    @Override
    public int containerPreAllocationLimit() {
        return delegate.containerPreAllocationLimit();
    }

    @Override
    public <T> void readStringMap(Schema schema, T target, MapMemberConsumer<String, T> consumer) {
        Schema value = schema.mapValueMember();
        delegate.readStringMap(schema, target, (t, key, de) -> {
            state.push("{" + key + "}", value);
            consumer.accept(t, key, wrap(de));
            state.pop();
        });
    }

    @Override
    public boolean isNull() {
        return delegate.isNull();
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
    public EventStream<?> readEventStream(Schema schema) {
        return delegate.readEventStream(schema);
    }

    @Override
    public void close() {
        delegate.close();
    }

    /** The path shared by a deserializer and the nested deserializers it wraps. */
    private static final class State {
        private final Schema root;
        private final TimestampReader timestamps;
        private final List<String> segments = new ArrayList<>();
        private final List<Schema> schemas = new ArrayList<>();

        State(Schema root, TimestampReader timestamps) {
            this.root = root;
            this.timestamps = timestamps;
        }

        void push(String segment, Schema schema) {
            segments.add(segment);
            schemas.add(schema);
        }

        void pop() {
            segments.remove(segments.size() - 1);
            schemas.remove(schemas.size() - 1);
        }

        /** The failure at the current (deepest, not popped on failure) position. */
        SdkProtocolException failure(WireContext context, String wireLocation, Throwable cause) {
            Schema current = schemas.isEmpty() ? root : schemas.get(schemas.size() - 1);
            Schema target = current.isMember() ? current.memberTarget() : current;
            String reason = "the value is not a valid " + target.type() + " (" + cause.getClass().getSimpleName()
                            + ")";
            return context.failure(target.id(), List.copyOf(segments), wireLocation, reason);
        }
    }
}
