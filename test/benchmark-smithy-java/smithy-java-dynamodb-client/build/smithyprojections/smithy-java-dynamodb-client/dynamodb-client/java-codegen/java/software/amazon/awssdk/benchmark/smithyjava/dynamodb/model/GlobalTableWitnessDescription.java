package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents the properties of a witness Region in a MRSC global table.
 */
@SmithyGenerated
public final class GlobalTableWitnessDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GLOBAL_TABLE_WITNESS_DESCRIPTION;
    private static final Schema $SCHEMA_REGION_NAME = $SCHEMA.member("RegionName");
    private static final Schema $SCHEMA_WITNESS_STATUS = $SCHEMA.member("WitnessStatus");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String regionName;
    private final transient WitnessStatus witnessStatus;

    private GlobalTableWitnessDescription(Builder builder) {
        this.regionName = builder.regionName;
        this.witnessStatus = builder.witnessStatus;
    }

    /**
     * The name of the Amazon Web Services Region that serves as a witness for the MRSC global table.
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * The current status of the witness Region in the MRSC global table.
     */
    public WitnessStatus getWitnessStatus() {
        return witnessStatus;
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
        GlobalTableWitnessDescription that = (GlobalTableWitnessDescription) other;
        return Objects.equals(this.regionName, that.regionName)
               && Objects.equals(this.witnessStatus, that.witnessStatus);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(regionName);
        $hc = 31 * $hc + Objects.hashCode(witnessStatus);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (regionName != null) {
            serializer.writeString($SCHEMA_REGION_NAME, regionName);
        }
        if (witnessStatus != null) {
            serializer.writeString($SCHEMA_WITNESS_STATUS, witnessStatus.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, regionName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_WITNESS_STATUS, member, witnessStatus);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GlobalTableWitnessDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.regionName(this.regionName);
        builder.witnessStatus(this.witnessStatus);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GlobalTableWitnessDescription}.
     */
    public static final class Builder implements ShapeBuilder<GlobalTableWitnessDescription> {
        private String regionName;
        private WitnessStatus witnessStatus;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the Amazon Web Services Region that serves as a witness for the MRSC global table.
         *
         * @return this builder.
         */
        public Builder regionName(String regionName) {
            this.regionName = regionName;
            return this;
        }

        /**
         * The current status of the witness Region in the MRSC global table.
         *
         * @return this builder.
         */
        public Builder witnessStatus(WitnessStatus witnessStatus) {
            this.witnessStatus = witnessStatus;
            return this;
        }

        @Override
        public GlobalTableWitnessDescription build() {
            return new GlobalTableWitnessDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> regionName((String) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, value));
                case 1 -> witnessStatus((WitnessStatus) SchemaUtils.validateSameMember($SCHEMA_WITNESS_STATUS, member, value));
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
                    case 0 -> builder.regionName(de.readString(member));
                    case 1 -> builder.witnessStatus(WitnessStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
