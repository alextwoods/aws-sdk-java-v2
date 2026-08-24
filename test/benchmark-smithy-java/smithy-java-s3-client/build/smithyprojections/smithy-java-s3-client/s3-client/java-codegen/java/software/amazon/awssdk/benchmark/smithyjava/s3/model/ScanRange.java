package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Specifies the byte range of the object to get the records from. A record is processed when its first byte is
 * contained by the range. This parameter is optional, but when specified, it must not be empty. See RFC 2616, Section
 * 14.35.1 about how to specify the start and end of the range.
 */
@SmithyGenerated
public final class ScanRange implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas6.SCAN_RANGE;
    private static final Schema $SCHEMA_START = $SCHEMA.member("Start");
    private static final Schema $SCHEMA_END = $SCHEMA.member("End");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Long start;
    private final transient Long end;

    private ScanRange(Builder builder) {
        this.start = builder.start;
        this.end = builder.end;
    }

    /**
     * Specifies the start of the byte range. This parameter is optional. Valid values: non-negative integers. The
     * default value is 0. If only <code>start</code> is supplied, it means scan from that point to the end of the file.
     * For example, <code>50</code> means scan from byte 50 until the end of the file.
     */
    public Long getStart() {
        return start;
    }

    /**
     * Specifies the end of the byte range. This parameter is optional. Valid values: non-negative integers. The default
     * value is one less than the size of the object being queried. If only the End parameter is supplied, it is
     * interpreted to mean scan the last N bytes of the file. For example, <code>50</code> means scan the last 50 bytes.
     */
    public Long getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        ScanRange that = (ScanRange) other;
        return Objects.equals(this.start, that.start)
               && Objects.equals(this.end, that.end);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(start);
        $hc = 31 * $hc + Objects.hashCode(end);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (start != null) {
            serializer.writeLong($SCHEMA_START, start);
        }
        if (end != null) {
            serializer.writeLong($SCHEMA_END, end);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_START, member, start);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_END, member, end);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ScanRange}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.start(this.start);
        builder.end(this.end);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ScanRange}.
     */
    public static final class Builder implements ShapeBuilder<ScanRange> {
        private Long start;
        private Long end;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies the start of the byte range. This parameter is optional. Valid values: non-negative integers. The
         * default value is 0. If only <code>start</code> is supplied, it means scan from that point to the end of the file.
         * For example, <code>50</code> means scan from byte 50 until the end of the file.
         *
         * @return this builder.
         */
        public Builder start(Long start) {
            this.start = start;
            return this;
        }

        /**
         * Specifies the end of the byte range. This parameter is optional. Valid values: non-negative integers. The default
         * value is one less than the size of the object being queried. If only the End parameter is supplied, it is
         * interpreted to mean scan the last N bytes of the file. For example, <code>50</code> means scan the last 50 bytes.
         *
         * @return this builder.
         */
        public Builder end(Long end) {
            this.end = end;
            return this;
        }

        @Override
        public ScanRange build() {
            return new ScanRange(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> start((Long) SchemaUtils.validateSameMember($SCHEMA_START, member, value));
                case 1 -> end((Long) SchemaUtils.validateSameMember($SCHEMA_END, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct($SCHEMA, this, $InnerDeserializer.INSTANCE);
            return this;
        }

        @Override
        public Builder deserializeMember(ShapeDeserializer decoder, Schema schema) {
            decoder.readStruct(schema.assertMemberTargetIs($SCHEMA), this, $InnerDeserializer.INSTANCE);
            return this;
        }

        private static final class $InnerDeserializer implements ShapeDeserializer.StructMemberConsumer<Builder> {
            private static final $InnerDeserializer INSTANCE = new $InnerDeserializer();

            @Override
            @SuppressWarnings("unchecked")
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {
                switch (member.memberIndex()) {
                    case 0 -> builder.start(de.readLong(member));
                    case 1 -> builder.end(de.readLong(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
