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
public final class DescribeImportOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.DESCRIBE_IMPORT_OUTPUT;
    private static final Schema $SCHEMA_IMPORT_TABLE_DESCRIPTION = $SCHEMA.member("ImportTableDescription");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ImportTableDescription importTableDescription;

    private DescribeImportOutput(Builder builder) {
        this.importTableDescription = builder.importTableDescription;
    }

    /**
     * Represents the properties of the table created for the import, and parameters of the import. The import
     * parameters include import status, how many items were processed, and how many errors were encountered.
     */
    public ImportTableDescription getImportTableDescription() {
        return importTableDescription;
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
        DescribeImportOutput that = (DescribeImportOutput) other;
        return Objects.equals(this.importTableDescription, that.importTableDescription);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(importTableDescription);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (importTableDescription != null) {
            serializer.writeStruct($SCHEMA_IMPORT_TABLE_DESCRIPTION, importTableDescription);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_IMPORT_TABLE_DESCRIPTION, member, importTableDescription);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link DescribeImportOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.importTableDescription(this.importTableDescription);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DescribeImportOutput}.
     */
    public static final class Builder implements ShapeBuilder<DescribeImportOutput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private ImportTableDescription importTableDescription;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the properties of the table created for the import, and parameters of the import. The import
         * parameters include import status, how many items were processed, and how many errors were encountered.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder importTableDescription(ImportTableDescription importTableDescription) {
            this.importTableDescription = Objects.requireNonNull(importTableDescription, "importTableDescription cannot be null");
            tracker.setMember($SCHEMA_IMPORT_TABLE_DESCRIPTION);
            return this;
        }

        @Override
        public DescribeImportOutput build() {
            tracker.validate();
            return new DescribeImportOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> importTableDescription((ImportTableDescription) SchemaUtils.validateSameMember($SCHEMA_IMPORT_TABLE_DESCRIPTION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<DescribeImportOutput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_IMPORT_TABLE_DESCRIPTION)) {
                tracker.setMember($SCHEMA_IMPORT_TABLE_DESCRIPTION);
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
                    case 0 -> builder.importTableDescription(ImportTableDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
