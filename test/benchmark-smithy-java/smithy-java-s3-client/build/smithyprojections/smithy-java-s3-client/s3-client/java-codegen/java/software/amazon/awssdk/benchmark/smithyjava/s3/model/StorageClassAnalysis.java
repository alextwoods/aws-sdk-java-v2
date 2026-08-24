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
 * Specifies data related to access patterns to be collected and made available to analyze the tradeoffs between
 * different storage classes for an Amazon S3 bucket.
 */
@SmithyGenerated
public final class StorageClassAnalysis implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.STORAGE_CLASS_ANALYSIS;
    private static final Schema $SCHEMA_DATA_EXPORT = $SCHEMA.member("DataExport");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient StorageClassAnalysisDataExport dataExport;

    private StorageClassAnalysis(Builder builder) {
        this.dataExport = builder.dataExport;
    }

    /**
     * Specifies how data related to the storage class analysis for an Amazon S3 bucket should be exported.
     */
    public StorageClassAnalysisDataExport getDataExport() {
        return dataExport;
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
        StorageClassAnalysis that = (StorageClassAnalysis) other;
        return Objects.equals(this.dataExport, that.dataExport);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(dataExport);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (dataExport != null) {
            serializer.writeStruct($SCHEMA_DATA_EXPORT, dataExport);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATA_EXPORT, member, dataExport);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link StorageClassAnalysis}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.dataExport(this.dataExport);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link StorageClassAnalysis}.
     */
    public static final class Builder implements ShapeBuilder<StorageClassAnalysis> {
        private StorageClassAnalysisDataExport dataExport;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies how data related to the storage class analysis for an Amazon S3 bucket should be exported.
         *
         * @return this builder.
         */
        public Builder dataExport(StorageClassAnalysisDataExport dataExport) {
            this.dataExport = dataExport;
            return this;
        }

        @Override
        public StorageClassAnalysis build() {
            return new StorageClassAnalysis(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> dataExport((StorageClassAnalysisDataExport) SchemaUtils.validateSameMember($SCHEMA_DATA_EXPORT, member, value));
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
                    case 0 -> builder.dataExport(StorageClassAnalysisDataExport.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
