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
 * Specifies the location where the bucket will be created.
 *
 * <p>For directory buckets, the location type is Availability Zone or Local Zone. For more information about directory
 * buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/directory-buckets-overview.html">Working with directory buckets</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <p>This functionality is only supported by directory buckets.
 */
@SmithyGenerated
public final class LocationInfo implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LOCATION_INFO;
    private static final Schema $SCHEMA_TYPE = $SCHEMA.member("Type");
    private static final Schema $SCHEMA_NAME = $SCHEMA.member("Name");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient LocationType type;
    private final transient String name;

    private LocationInfo(Builder builder) {
        this.type = builder.type;
        this.name = builder.name;
    }

    /**
     * The type of location where the bucket will be created.
     */
    public LocationType getType() {
        return type;
    }

    /**
     * The name of the location where the bucket will be created.
     *
     * <p>For directory buckets, the name of the location is the Zone ID of the Availability Zone (AZ) or Local Zone
     * (LZ) where the bucket will be created. An example AZ ID value is <code>usw2-az1</code>.
     */
    public String getName() {
        return name;
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
        LocationInfo that = (LocationInfo) other;
        return Objects.equals(this.name, that.name)
               && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(type);
        $hc = 31 * $hc + Objects.hashCode(name);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (type != null) {
            serializer.writeString($SCHEMA_TYPE, type.getValue());
        }
        if (name != null) {
            serializer.writeString($SCHEMA_NAME, name);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TYPE, member, type);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAME, member, name);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link LocationInfo}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.type(this.type);
        builder.name(this.name);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LocationInfo}.
     */
    public static final class Builder implements ShapeBuilder<LocationInfo> {
        private LocationType type;
        private String name;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The type of location where the bucket will be created.
         *
         * @return this builder.
         */
        public Builder type(LocationType type) {
            this.type = type;
            return this;
        }

        /**
         * The name of the location where the bucket will be created.
         *
         * <p>For directory buckets, the name of the location is the Zone ID of the Availability Zone (AZ) or Local Zone
         * (LZ) where the bucket will be created. An example AZ ID value is <code>usw2-az1</code>.
         *
         * @return this builder.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public LocationInfo build() {
            return new LocationInfo(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> type((LocationType) SchemaUtils.validateSameMember($SCHEMA_TYPE, member, value));
                case 1 -> name((String) SchemaUtils.validateSameMember($SCHEMA_NAME, member, value));
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
                    case 0 -> builder.type(LocationType.builder().deserializeMember(de, member).build());
                    case 1 -> builder.name(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
