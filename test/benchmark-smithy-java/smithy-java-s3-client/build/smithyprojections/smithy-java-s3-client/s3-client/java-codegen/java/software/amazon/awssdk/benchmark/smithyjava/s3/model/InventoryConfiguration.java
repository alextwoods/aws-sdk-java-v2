package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.Collections;
import java.util.List;
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
 * Specifies the S3 Inventory configuration for an Amazon S3 bucket. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGETInventoryConfig.html">GET Bucket inventory</a>
 * in the <i>Amazon S3 API Reference</i>.
 */
@SmithyGenerated
public final class InventoryConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.INVENTORY_CONFIGURATION;
    private static final Schema $SCHEMA_DESTINATION = $SCHEMA.member("Destination");
    private static final Schema $SCHEMA_IS_ENABLED = $SCHEMA.member("IsEnabled");
    private static final Schema $SCHEMA_FILTER = $SCHEMA.member("Filter");
    private static final Schema $SCHEMA_ID = $SCHEMA.member("Id");
    private static final Schema $SCHEMA_INCLUDED_OBJECT_VERSIONS = $SCHEMA.member("IncludedObjectVersions");
    private static final Schema $SCHEMA_OPTIONAL_FIELDS = $SCHEMA.member("OptionalFields");
    private static final Schema $SCHEMA_SCHEDULE = $SCHEMA.member("Schedule");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient InventoryDestination destination;
    private final transient boolean isEnabled;
    private final transient InventoryFilter filter;
    private final transient String id;
    private final transient InventoryIncludedObjectVersions includedObjectVersions;
    private final transient List<InventoryOptionalField> optionalFields;
    private final transient InventorySchedule schedule;

    private InventoryConfiguration(Builder builder) {
        this.destination = builder.destination;
        this.isEnabled = builder.isEnabled;
        this.filter = builder.filter;
        this.id = builder.id;
        this.includedObjectVersions = builder.includedObjectVersions;
        this.optionalFields = builder.optionalFields == null ? null : Collections.unmodifiableList(builder.optionalFields);
        this.schedule = builder.schedule;
    }

    /**
     * Contains information about where to publish the inventory results.
     */
    public InventoryDestination getDestination() {
        return destination;
    }

    /**
     * Specifies whether the inventory is enabled or disabled. If set to <code>True</code>, an inventory list is
     * generated. If set to <code>False</code>, no inventory list is generated.
     */
    public boolean isIsEnabled() {
        return isEnabled;
    }

    /**
     * Specifies an inventory filter. The inventory only includes objects that meet the filter's criteria.
     */
    public InventoryFilter getFilter() {
        return filter;
    }

    /**
     * The ID used to identify the inventory configuration.
     */
    public String getId() {
        return id;
    }

    /**
     * Object versions to include in the inventory list. If set to <code>All</code>, the list includes all the object
     * versions, which adds the version-related fields <code>VersionId</code>, <code>IsLatest</code>, and
     * <code>DeleteMarker</code> to the list. If set to <code>Current</code>, the list does not contain these
     * version-related fields.
     */
    public InventoryIncludedObjectVersions getIncludedObjectVersions() {
        return includedObjectVersions;
    }

    /**
     * Contains the optional fields that are included in the inventory results.
     *
     * <p>The following optional fields are supported for directory buckets <code>Size | LastModifiedDate | StorageClass
     * | ETag | IsMultipartUploaded | EncryptionStatus | BucketKeyStatus | ChecksumAlgorithm | LifecycleExpirationDate.</code>
     * Throws MalformedXML error if unsupported optional field is provided.
     */
    public List<InventoryOptionalField> getOptionalFields() {
        if (optionalFields == null) {
            return Collections.emptyList();
        }
        return optionalFields;
    }

    public boolean hasOptionalFields() {
        return optionalFields != null;
    }

    /**
     * Specifies the schedule for generating inventory results.
     */
    public InventorySchedule getSchedule() {
        return schedule;
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
        InventoryConfiguration that = (InventoryConfiguration) other;
        return this.isEnabled == that.isEnabled
               && Objects.equals(this.id, that.id)
               && Objects.equals(this.includedObjectVersions, that.includedObjectVersions)
               && Objects.equals(this.destination, that.destination)
               && Objects.equals(this.filter, that.filter)
               && Objects.equals(this.schedule, that.schedule)
               && Objects.equals(this.optionalFields, that.optionalFields);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(destination);
        $hc = 31 * $hc + Boolean.hashCode(isEnabled);
        $hc = 31 * $hc + Objects.hashCode(filter);
        $hc = 31 * $hc + Objects.hashCode(id);
        $hc = 31 * $hc + Objects.hashCode(includedObjectVersions);
        $hc = 31 * $hc + Objects.hashCode(optionalFields);
        $hc = 31 * $hc + Objects.hashCode(schedule);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (destination != null) {
            serializer.writeStruct($SCHEMA_DESTINATION, destination);
        }
        serializer.writeBoolean($SCHEMA_IS_ENABLED, isEnabled);
        if (filter != null) {
            serializer.writeStruct($SCHEMA_FILTER, filter);
        }
        serializer.writeString($SCHEMA_ID, id);
        serializer.writeString($SCHEMA_INCLUDED_OBJECT_VERSIONS, includedObjectVersions.getValue());
        if (optionalFields != null) {
            serializer.writeList($SCHEMA_OPTIONAL_FIELDS, optionalFields, optionalFields.size(), SharedSerde.InventoryOptionalFieldsSerializer.INSTANCE);
        }
        if (schedule != null) {
            serializer.writeStruct($SCHEMA_SCHEDULE, schedule);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_DESTINATION, member, destination);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_IS_ENABLED, member, isEnabled);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ID, member, id);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_INCLUDED_OBJECT_VERSIONS, member, includedObjectVersions);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_SCHEDULE, member, schedule);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, filter);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_OPTIONAL_FIELDS, member, optionalFields);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InventoryConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.destination(this.destination);
        builder.isEnabled(this.isEnabled);
        builder.filter(this.filter);
        builder.id(this.id);
        builder.includedObjectVersions(this.includedObjectVersions);
        builder.optionalFields(this.optionalFields);
        builder.schedule(this.schedule);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InventoryConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<InventoryConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private InventoryDestination destination;
        private boolean isEnabled;
        private InventoryFilter filter;
        private String id;
        private InventoryIncludedObjectVersions includedObjectVersions;
        private List<InventoryOptionalField> optionalFields;
        private InventorySchedule schedule;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Contains information about where to publish the inventory results.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder destination(InventoryDestination destination) {
            this.destination = Objects.requireNonNull(destination, "destination cannot be null");
            tracker.setMember($SCHEMA_DESTINATION);
            return this;
        }

        /**
         * Specifies whether the inventory is enabled or disabled. If set to <code>True</code>, an inventory list is
         * generated. If set to <code>False</code>, no inventory list is generated.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder isEnabled(boolean isEnabled) {
            this.isEnabled = isEnabled;
            tracker.setMember($SCHEMA_IS_ENABLED);
            return this;
        }

        /**
         * Specifies an inventory filter. The inventory only includes objects that meet the filter's criteria.
         *
         * @return this builder.
         */
        public Builder filter(InventoryFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * The ID used to identify the inventory configuration.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder id(String id) {
            this.id = Objects.requireNonNull(id, "id cannot be null");
            tracker.setMember($SCHEMA_ID);
            return this;
        }

        /**
         * Object versions to include in the inventory list. If set to <code>All</code>, the list includes all the object
         * versions, which adds the version-related fields <code>VersionId</code>, <code>IsLatest</code>, and
         * <code>DeleteMarker</code> to the list. If set to <code>Current</code>, the list does not contain these
         * version-related fields.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder includedObjectVersions(InventoryIncludedObjectVersions includedObjectVersions) {
            this.includedObjectVersions = Objects.requireNonNull(includedObjectVersions, "includedObjectVersions cannot be null");
            tracker.setMember($SCHEMA_INCLUDED_OBJECT_VERSIONS);
            return this;
        }

        /**
         * Contains the optional fields that are included in the inventory results.
         *
         * <p>The following optional fields are supported for directory buckets <code>Size | LastModifiedDate | StorageClass
         * | ETag | IsMultipartUploaded | EncryptionStatus | BucketKeyStatus | ChecksumAlgorithm | LifecycleExpirationDate.</code>
         * Throws MalformedXML error if unsupported optional field is provided.
         *
         * @return this builder.
         */
        public Builder optionalFields(List<InventoryOptionalField> optionalFields) {
            this.optionalFields = optionalFields;
            return this;
        }

        /**
         * Specifies the schedule for generating inventory results.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder schedule(InventorySchedule schedule) {
            this.schedule = Objects.requireNonNull(schedule, "schedule cannot be null");
            tracker.setMember($SCHEMA_SCHEDULE);
            return this;
        }

        @Override
        public InventoryConfiguration build() {
            tracker.validate();
            return new InventoryConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> destination((InventoryDestination) SchemaUtils.validateSameMember($SCHEMA_DESTINATION, member, value));
                case 1 -> isEnabled((boolean) SchemaUtils.validateSameMember($SCHEMA_IS_ENABLED, member, value));
                case 2 -> id((String) SchemaUtils.validateSameMember($SCHEMA_ID, member, value));
                case 3 -> includedObjectVersions((InventoryIncludedObjectVersions) SchemaUtils.validateSameMember($SCHEMA_INCLUDED_OBJECT_VERSIONS, member, value));
                case 4 -> schedule((InventorySchedule) SchemaUtils.validateSameMember($SCHEMA_SCHEDULE, member, value));
                case 5 -> filter((InventoryFilter) SchemaUtils.validateSameMember($SCHEMA_FILTER, member, value));
                case 6 -> optionalFields((List<InventoryOptionalField>) SchemaUtils.validateSameMember($SCHEMA_OPTIONAL_FIELDS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<InventoryConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_DESTINATION)) {
                tracker.setMember($SCHEMA_DESTINATION);
            }
            if (!tracker.checkMember($SCHEMA_IS_ENABLED)) {
                tracker.setMember($SCHEMA_IS_ENABLED);
            }
            if (!tracker.checkMember($SCHEMA_ID)) {
                id("");
            }
            if (!tracker.checkMember($SCHEMA_INCLUDED_OBJECT_VERSIONS)) {
                includedObjectVersions(InventoryIncludedObjectVersions.unknown(""));
            }
            if (!tracker.checkMember($SCHEMA_SCHEDULE)) {
                tracker.setMember($SCHEMA_SCHEDULE);
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
                    case 0 -> builder.destination(InventoryDestination.builder().deserializeMember(de, member).build());
                    case 1 -> builder.isEnabled(de.readBoolean(member));
                    case 2 -> builder.id(de.readString(member));
                    case 3 -> builder.includedObjectVersions(InventoryIncludedObjectVersions.builder().deserializeMember(de, member).build());
                    case 4 -> builder.schedule(InventorySchedule.builder().deserializeMember(de, member).build());
                    case 5 -> builder.filter(InventoryFilter.builder().deserializeMember(de, member).build());
                    case 6 -> builder.optionalFields(SharedSerde.deserializeInventoryOptionalFields(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
