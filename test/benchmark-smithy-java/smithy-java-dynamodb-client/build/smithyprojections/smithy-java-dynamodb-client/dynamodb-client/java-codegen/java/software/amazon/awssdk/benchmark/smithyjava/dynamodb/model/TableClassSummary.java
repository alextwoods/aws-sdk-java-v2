package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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
 * Contains details of the table class.
 */
@SmithyGenerated
public final class TableClassSummary implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TABLE_CLASS_SUMMARY;
    private static final Schema $SCHEMA_TABLE_CLASS = $SCHEMA.member("TableClass");
    private static final Schema $SCHEMA_LAST_UPDATE_DATE_TIME = $SCHEMA.member("LastUpdateDateTime");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient TableClass tableClass;
    private final transient Instant lastUpdateDateTime;

    private TableClassSummary(Builder builder) {
        this.tableClass = builder.tableClass;
        this.lastUpdateDateTime = builder.lastUpdateDateTime;
    }

    /**
     * The table class of the specified table. Valid values are <code>STANDARD</code> and
     * <code>STANDARD_INFREQUENT_ACCESS</code>.
     */
    public TableClass getTableClass() {
        return tableClass;
    }

    /**
     * The date and time at which the table class was last updated.
     */
    public Instant getLastUpdateDateTime() {
        return lastUpdateDateTime;
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
        TableClassSummary that = (TableClassSummary) other;
        return Objects.equals(this.tableClass, that.tableClass)
               && Objects.equals(this.lastUpdateDateTime, that.lastUpdateDateTime);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableClass);
        $hc = 31 * $hc + Objects.hashCode(lastUpdateDateTime);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (tableClass != null) {
            serializer.writeString($SCHEMA_TABLE_CLASS, tableClass.getValue());
        }
        if (lastUpdateDateTime != null) {
            serializer.writeTimestamp($SCHEMA_LAST_UPDATE_DATE_TIME, lastUpdateDateTime);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_CLASS, member, tableClass);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATE_DATE_TIME, member, lastUpdateDateTime);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TableClassSummary}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableClass(this.tableClass);
        builder.lastUpdateDateTime(this.lastUpdateDateTime);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TableClassSummary}.
     */
    public static final class Builder implements ShapeBuilder<TableClassSummary> {
        private TableClass tableClass;
        private Instant lastUpdateDateTime;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The table class of the specified table. Valid values are <code>STANDARD</code> and
         * <code>STANDARD_INFREQUENT_ACCESS</code>.
         *
         * @return this builder.
         */
        public Builder tableClass(TableClass tableClass) {
            this.tableClass = tableClass;
            return this;
        }

        /**
         * The date and time at which the table class was last updated.
         *
         * @return this builder.
         */
        public Builder lastUpdateDateTime(Instant lastUpdateDateTime) {
            this.lastUpdateDateTime = lastUpdateDateTime;
            return this;
        }

        @Override
        public TableClassSummary build() {
            return new TableClassSummary(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableClass((TableClass) SchemaUtils.validateSameMember($SCHEMA_TABLE_CLASS, member, value));
                case 1 -> lastUpdateDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_LAST_UPDATE_DATE_TIME, member, value));
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
                    case 0 -> builder.tableClass(TableClass.builder().deserializeMember(de, member).build());
                    case 1 -> builder.lastUpdateDateTime(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
