package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
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
 * Represents attributes that are copied (projected) from the table into an index. These are in addition to the primary
 * key attributes and index key attributes, which are automatically projected.
 */
@SmithyGenerated
public final class Projection implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PROJECTION;
    private static final Schema $SCHEMA_PROJECTION_TYPE = $SCHEMA.member("ProjectionType");
    private static final Schema $SCHEMA_NON_KEY_ATTRIBUTES = $SCHEMA.member("NonKeyAttributes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient ProjectionType projectionType;
    private final transient List<String> nonKeyAttributes;

    private Projection(Builder builder) {
        this.projectionType = builder.projectionType;
        this.nonKeyAttributes = builder.nonKeyAttributes == null ? null : Collections.unmodifiableList(builder.nonKeyAttributes);
    }

    /**
     * The set of attributes that are projected into the index:
     *
     * <ul>
     *   <li>
     *     <code>KEYS_ONLY</code> - Only the index and primary keys are projected into the index.
     *   </li>
     *   <li>
     *     <code>INCLUDE</code> - In addition to the attributes described in <code>KEYS_ONLY</code>, the secondary
     *     index will include other non-key attributes that you specify.
     *   </li>
     *   <li>
     *     <code>ALL</code> - All of the table attributes are projected into the index.
     *   </li>
     * </ul>
     *
     * <p>When using the DynamoDB console, <code>ALL</code> is selected by default.
     */
    public ProjectionType getProjectionType() {
        return projectionType;
    }

    /**
     * Represents the non-key attribute names which will be projected into the index.
     *
     * <p>For global and local secondary indexes, the total count of <code>NonKeyAttributes</code> summed across all of
     * the secondary indexes, must not exceed 100. If you project the same attribute into two different indexes, this
     * counts as two distinct attributes when determining the total. This limit only applies when you specify the
     * ProjectionType of <code>INCLUDE</code>. You still can specify the ProjectionType of <code>ALL</code> to project
     * all attributes from the source table, even if the table has more than 100 attributes.
     */
    public List<String> getNonKeyAttributes() {
        if (nonKeyAttributes == null) {
            return Collections.emptyList();
        }
        return nonKeyAttributes;
    }

    public boolean hasNonKeyAttributes() {
        return nonKeyAttributes != null;
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
        Projection that = (Projection) other;
        return Objects.equals(this.projectionType, that.projectionType)
               && Objects.equals(this.nonKeyAttributes, that.nonKeyAttributes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(projectionType);
        $hc = 31 * $hc + Objects.hashCode(nonKeyAttributes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (projectionType != null) {
            serializer.writeString($SCHEMA_PROJECTION_TYPE, projectionType.getValue());
        }
        if (nonKeyAttributes != null) {
            serializer.writeList($SCHEMA_NON_KEY_ATTRIBUTES, nonKeyAttributes, nonKeyAttributes.size(), SharedSerde.NonKeyAttributeNameListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_TYPE, member, projectionType);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NON_KEY_ATTRIBUTES, member, nonKeyAttributes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Projection}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.projectionType(this.projectionType);
        builder.nonKeyAttributes(this.nonKeyAttributes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Projection}.
     */
    public static final class Builder implements ShapeBuilder<Projection> {
        private ProjectionType projectionType;
        private List<String> nonKeyAttributes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The set of attributes that are projected into the index:
         *
         * <ul>
         *   <li>
         *     <code>KEYS_ONLY</code> - Only the index and primary keys are projected into the index.
         *   </li>
         *   <li>
         *     <code>INCLUDE</code> - In addition to the attributes described in <code>KEYS_ONLY</code>, the secondary
         *     index will include other non-key attributes that you specify.
         *   </li>
         *   <li>
         *     <code>ALL</code> - All of the table attributes are projected into the index.
         *   </li>
         * </ul>
         *
         * <p>When using the DynamoDB console, <code>ALL</code> is selected by default.
         *
         * @return this builder.
         */
        public Builder projectionType(ProjectionType projectionType) {
            this.projectionType = projectionType;
            return this;
        }

        /**
         * Represents the non-key attribute names which will be projected into the index.
         *
         * <p>For global and local secondary indexes, the total count of <code>NonKeyAttributes</code> summed across all of
         * the secondary indexes, must not exceed 100. If you project the same attribute into two different indexes, this
         * counts as two distinct attributes when determining the total. This limit only applies when you specify the
         * ProjectionType of <code>INCLUDE</code>. You still can specify the ProjectionType of <code>ALL</code> to project
         * all attributes from the source table, even if the table has more than 100 attributes.
         *
         * @return this builder.
         */
        public Builder nonKeyAttributes(List<String> nonKeyAttributes) {
            this.nonKeyAttributes = nonKeyAttributes;
            return this;
        }

        @Override
        public Projection build() {
            return new Projection(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> projectionType((ProjectionType) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_TYPE, member, value));
                case 1 -> nonKeyAttributes((List<String>) SchemaUtils.validateSameMember($SCHEMA_NON_KEY_ATTRIBUTES, member, value));
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
                    case 0 -> builder.projectionType(ProjectionType.builder().deserializeMember(de, member).build());
                    case 1 -> builder.nonKeyAttributes(SharedSerde.deserializeNonKeyAttributeNameList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
