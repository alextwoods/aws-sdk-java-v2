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
 * Specifies an item to be retrieved as part of the transaction.
 */
@SmithyGenerated
public final class TransactGetItem implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TRANSACT_GET_ITEM;
    private static final Schema $SCHEMA_GET = $SCHEMA.member("Get");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Get get;

    private TransactGetItem(Builder builder) {
        this.get = builder.get;
    }

    /**
     * Contains the primary key that identifies the item to get, together with the name of the table that contains the
     * item, and optionally the specific attributes of the item to retrieve.
     */
    public Get getGet() {
        return get;
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
        TransactGetItem that = (TransactGetItem) other;
        return Objects.equals(this.get, that.get);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(get);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (get != null) {
            serializer.writeStruct($SCHEMA_GET, get);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_GET, member, get);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TransactGetItem}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.get(this.get);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TransactGetItem}.
     */
    public static final class Builder implements ShapeBuilder<TransactGetItem> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Get get;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Contains the primary key that identifies the item to get, together with the name of the table that contains the
         * item, and optionally the specific attributes of the item to retrieve.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder get(Get get) {
            this.get = Objects.requireNonNull(get, "get cannot be null");
            tracker.setMember($SCHEMA_GET);
            return this;
        }

        @Override
        public TransactGetItem build() {
            tracker.validate();
            return new TransactGetItem(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> get((Get) SchemaUtils.validateSameMember($SCHEMA_GET, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<TransactGetItem> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_GET)) {
                tracker.setMember($SCHEMA_GET);
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
                    case 0 -> builder.get(Get.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
