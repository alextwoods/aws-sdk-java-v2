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
public final class DescribeExportInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_EXPORT_INPUT;
    private static final Schema $SCHEMA_EXPORT_ARN = $SCHEMA.member("ExportArn");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String exportArn;

    private DescribeExportInput(Builder builder) {
        this.exportArn = builder.exportArn;
    }

    /**
     * The Amazon Resource Name (ARN) associated with the export.
     */
    public String getExportArn() {
        return exportArn;
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
        DescribeExportInput that = (DescribeExportInput) other;
        return Objects.equals(this.exportArn, that.exportArn);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(exportArn);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_EXPORT_ARN, exportArn);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPORT_ARN, member, exportArn);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeExportInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.exportArn(this.exportArn);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeExportInput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeExportInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String exportArn;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) associated with the export.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder exportArn(String exportArn) {
            this.exportArn = Objects.requireNonNull(exportArn, "exportArn cannot be null");
            tracker.setMember($SCHEMA_EXPORT_ARN);
            return this;
        }

        @Override
        public DescribeExportInput build() {
            tracker.validate();
            return new DescribeExportInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> exportArn((String) SchemaUtils.validateSameMember($SCHEMA_EXPORT_ARN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DescribeExportInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_EXPORT_ARN)) {
                exportArn("");
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
                    case 0 -> builder.exportArn(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
