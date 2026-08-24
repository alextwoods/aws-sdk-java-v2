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

@SmithyGenerated
public final class DescribeImportInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_IMPORT_INPUT;
    private static final Schema $SCHEMA_IMPORT_ARN = $SCHEMA.member("ImportArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String importArn;

    private DescribeImportInput(Builder builder) {
        this.importArn = builder.importArn;
    }

    /**
     * The Amazon Resource Name (ARN) associated with the table you're importing to.
     */
    public String getImportArn() {
        return importArn;
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
        DescribeImportInput that = (DescribeImportInput) other;
        return Objects.equals(this.importArn, that.importArn);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(importArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_IMPORT_ARN, importArn);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_IMPORT_ARN, member, importArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeImportInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.importArn(this.importArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeImportInput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeImportInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String importArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) associated with the table you're importing to.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder importArn(String importArn) {
            this.importArn = Objects.requireNonNull(importArn, "importArn cannot be null");
            tracker.setMember($SCHEMA_IMPORT_ARN);
            return this;
        }

        @Override
        public DescribeImportInput build() {
            tracker.validate();
            return new DescribeImportInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> importArn((String) SchemaUtils.validateSameMember($SCHEMA_IMPORT_ARN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DescribeImportInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_IMPORT_ARN)) {
                importArn("");
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
                    case 0 -> builder.importArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
