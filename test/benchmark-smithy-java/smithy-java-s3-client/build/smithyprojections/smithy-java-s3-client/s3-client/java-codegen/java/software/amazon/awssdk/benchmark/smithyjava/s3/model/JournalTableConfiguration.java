package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * The journal table configuration for an S3 Metadata configuration.
 */
@SmithyGenerated
public final class JournalTableConfiguration implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.JOURNAL_TABLE_CONFIGURATION;
    private static final Schema $SCHEMA_RECORD_EXPIRATION = $SCHEMA.member("RecordExpiration");
    private static final Schema $SCHEMA_ENCRYPTION_CONFIGURATION = $SCHEMA.member("EncryptionConfiguration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient RecordExpiration recordExpiration;
    private final transient MetadataTableEncryptionConfiguration encryptionConfiguration;

    private JournalTableConfiguration(Builder builder) {
        this.recordExpiration = builder.recordExpiration;
        this.encryptionConfiguration = builder.encryptionConfiguration;
    }

    /**
     * The journal table record expiration settings for the journal table.
     */
    public RecordExpiration getRecordExpiration() {
        return recordExpiration;
    }

    /**
     * The encryption configuration for the journal table.
     */
    public MetadataTableEncryptionConfiguration getEncryptionConfiguration() {
        return encryptionConfiguration;
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
        JournalTableConfiguration that = (JournalTableConfiguration) other;
        return Objects.equals(this.recordExpiration, that.recordExpiration)
               && Objects.equals(this.encryptionConfiguration, that.encryptionConfiguration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(recordExpiration);
        $hc = 31 * $hc + Objects.hashCode(encryptionConfiguration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (recordExpiration != null) {
            serializer.writeStruct($SCHEMA_RECORD_EXPIRATION, recordExpiration);
        }
        if (encryptionConfiguration != null) {
            serializer.writeStruct($SCHEMA_ENCRYPTION_CONFIGURATION, encryptionConfiguration);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RECORD_EXPIRATION, member, recordExpiration);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_CONFIGURATION, member, encryptionConfiguration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link JournalTableConfiguration}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.recordExpiration(this.recordExpiration);
        builder.encryptionConfiguration(this.encryptionConfiguration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link JournalTableConfiguration}.
     */
    public static final class Builder implements ShapeBuilder<JournalTableConfiguration> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private RecordExpiration recordExpiration;
        private MetadataTableEncryptionConfiguration encryptionConfiguration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The journal table record expiration settings for the journal table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder recordExpiration(RecordExpiration recordExpiration) {
            this.recordExpiration = Objects.requireNonNull(recordExpiration, "recordExpiration cannot be null");
            tracker.setMember($SCHEMA_RECORD_EXPIRATION);
            return this;
        }

        /**
         * The encryption configuration for the journal table.
         *
         * @return this builder.
         */
        public Builder encryptionConfiguration(MetadataTableEncryptionConfiguration encryptionConfiguration) {
            this.encryptionConfiguration = encryptionConfiguration;
            return this;
        }

        @Override
        public JournalTableConfiguration build() {
            tracker.validate();
            return new JournalTableConfiguration(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> recordExpiration((RecordExpiration) SchemaUtils.validateSameMember($SCHEMA_RECORD_EXPIRATION, member, value));
                case 1 -> encryptionConfiguration((MetadataTableEncryptionConfiguration) SchemaUtils.validateSameMember($SCHEMA_ENCRYPTION_CONFIGURATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<JournalTableConfiguration> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RECORD_EXPIRATION)) {
                tracker.setMember($SCHEMA_RECORD_EXPIRATION);
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
                    case 0 -> builder.recordExpiration(RecordExpiration.builder().deserializeMember(de, member).build());
                    case 1 -> builder.encryptionConfiguration(MetadataTableEncryptionConfiguration.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
