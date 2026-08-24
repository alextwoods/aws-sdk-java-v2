package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
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
 * Represents the DynamoDB Streams configuration for a table in DynamoDB.
 */
@SmithyGenerated
public final class StreamSpecification implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.STREAM_SPECIFICATION;
    private static final Schema $SCHEMA_STREAM_ENABLED = $SCHEMA.member("StreamEnabled");
    private static final Schema $SCHEMA_STREAM_VIEW_TYPE = $SCHEMA.member("StreamViewType");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient boolean streamEnabled;
    private final transient StreamViewType streamViewType;

    private StreamSpecification(Builder builder) {
        this.streamEnabled = builder.streamEnabled;
        this.streamViewType = builder.streamViewType;
    }

    /**
     * Indicates whether DynamoDB Streams is enabled (true) or disabled (false) on the table.
     */
    public boolean isStreamEnabled() {
        return streamEnabled;
    }

    /**
     * When an item in the table is modified, <code>StreamViewType</code> determines what information is written to the
     * stream for this table. Valid values for <code>StreamViewType</code> are:
     *
     * <ul>
     *   <li>
     *     <code>KEYS_ONLY</code> - Only the key attributes of the modified item are written to the stream.
     *   </li>
     *   <li>
     *     <code>NEW_IMAGE</code> - The entire item, as it appears after it was modified, is written to the stream.
     *   </li>
     *   <li>
     *     <code>OLD_IMAGE</code> - The entire item, as it appeared before it was modified, is written to the
     *     stream.
     *   </li>
     *   <li>
     *     <code>NEW_AND_OLD_IMAGES</code> - Both the new and the old item images of the item are written to the
     *     stream.
     *   </li>
     * </ul>
     */
    public StreamViewType getStreamViewType() {
        return streamViewType;
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
        StreamSpecification that = (StreamSpecification) other;
        return this.streamEnabled == that.streamEnabled
               && Objects.equals(this.streamViewType, that.streamViewType);
    }

    @Override
    public int hashCode() {
        int $hc = Boolean.hashCode(streamEnabled);
        $hc = 31 * $hc + Objects.hashCode(streamViewType);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeBoolean($SCHEMA_STREAM_ENABLED, streamEnabled);
        if (streamViewType != null) {
            serializer.writeString($SCHEMA_STREAM_VIEW_TYPE, streamViewType.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STREAM_ENABLED, member, streamEnabled);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_STREAM_VIEW_TYPE, member, streamViewType);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link StreamSpecification}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.streamEnabled(this.streamEnabled);
        builder.streamViewType(this.streamViewType);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link StreamSpecification}.
     */
    public static final class Builder implements ShapeBuilder<StreamSpecification> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private boolean streamEnabled;
        private StreamViewType streamViewType;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Indicates whether DynamoDB Streams is enabled (true) or disabled (false) on the table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder streamEnabled(boolean streamEnabled) {
            this.streamEnabled = streamEnabled;
            tracker.setMember($SCHEMA_STREAM_ENABLED);
            return this;
        }

        /**
         * When an item in the table is modified, <code>StreamViewType</code> determines what information is written to the
         * stream for this table. Valid values for <code>StreamViewType</code> are:
         *
         * <ul>
         *   <li>
         *     <code>KEYS_ONLY</code> - Only the key attributes of the modified item are written to the stream.
         *   </li>
         *   <li>
         *     <code>NEW_IMAGE</code> - The entire item, as it appears after it was modified, is written to the stream.
         *   </li>
         *   <li>
         *     <code>OLD_IMAGE</code> - The entire item, as it appeared before it was modified, is written to the
         *     stream.
         *   </li>
         *   <li>
         *     <code>NEW_AND_OLD_IMAGES</code> - Both the new and the old item images of the item are written to the
         *     stream.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder streamViewType(StreamViewType streamViewType) {
            this.streamViewType = streamViewType;
            return this;
        }

        @Override
        public StreamSpecification build() {
            tracker.validate();
            return new StreamSpecification(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> streamEnabled((boolean) SchemaUtils.validateSameMember($SCHEMA_STREAM_ENABLED, member, value));
                case 1 -> streamViewType((StreamViewType) SchemaUtils.validateSameMember($SCHEMA_STREAM_VIEW_TYPE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<StreamSpecification> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_STREAM_ENABLED)) {
                tracker.setMember($SCHEMA_STREAM_ENABLED);
            }
            return this;
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
                    case 0 -> builder.streamEnabled(de.readBoolean(member));
                    case 1 -> builder.streamViewType(StreamViewType.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
