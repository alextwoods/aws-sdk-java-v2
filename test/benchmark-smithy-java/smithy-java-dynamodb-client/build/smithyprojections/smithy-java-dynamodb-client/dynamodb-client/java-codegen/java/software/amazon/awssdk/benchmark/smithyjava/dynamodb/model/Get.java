package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.Map;
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
 * Specifies an item and related attribute values to retrieve in a <code>TransactGetItem</code> object.
 */
@SmithyGenerated
public final class Get implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GET;
    private static final Schema $SCHEMA_KEY = $SCHEMA.member("Key");
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_PROJECTION_EXPRESSION = $SCHEMA.member("ProjectionExpression");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_NAMES = $SCHEMA.member("ExpressionAttributeNames");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, AttributeValue> key;
    private final transient String tableName;
    private final transient String projectionExpression;
    private final transient Map<String, String> expressionAttributeNames;

    private Get(Builder builder) {
        this.key = Collections.unmodifiableMap(builder.key);
        this.tableName = builder.tableName;
        this.projectionExpression = builder.projectionExpression;
        this.expressionAttributeNames = builder.expressionAttributeNames == null ? null : Collections.unmodifiableMap(builder.expressionAttributeNames);
    }

    /**
     * A map of attribute names to <code>AttributeValue</code> objects that specifies the primary key of the item to
     * retrieve.
     */
    public Map<String, AttributeValue> getKey() {
        return key;
    }

    public boolean hasKey() {
        return true;
    }

    /**
     * The name of the table from which to retrieve the specified item. You can also provide the Amazon Resource Name
     * (ARN) of the table in this parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * A string that identifies one or more attributes of the specified item to retrieve from the table. The attributes
     * in the expression must be separated by commas. If no attribute names are specified, then all attributes of the
     * specified item are returned. If any of the requested attributes are not found, they do not appear in the result.
     */
    public String getProjectionExpression() {
        return projectionExpression;
    }

    /**
     * One or more substitution tokens for attribute names in the ProjectionExpression parameter.
     */
    public Map<String, String> getExpressionAttributeNames() {
        if (expressionAttributeNames == null) {
            return Collections.emptyMap();
        }
        return expressionAttributeNames;
    }

    public boolean hasExpressionAttributeNames() {
        return expressionAttributeNames != null;
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
        Get that = (Get) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.projectionExpression, that.projectionExpression)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.expressionAttributeNames, that.expressionAttributeNames);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(key);
        $hc = 31 * $hc + Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(projectionExpression);
        $hc = 31 * $hc + Objects.hashCode(expressionAttributeNames);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeMap($SCHEMA_KEY, key, key.size(), SharedSerde.KeySerializer.INSTANCE);
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        if (projectionExpression != null) {
            serializer.writeString($SCHEMA_PROJECTION_EXPRESSION, projectionExpression);
        }
        if (expressionAttributeNames != null) {
            serializer.writeMap($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, expressionAttributeNames, expressionAttributeNames.size(), SharedSerde.ExpressionAttributeNameMapSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY, member, key);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_EXPRESSION, member, projectionExpression);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, expressionAttributeNames);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Get}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.key(this.key);
        builder.tableName(this.tableName);
        builder.projectionExpression(this.projectionExpression);
        builder.expressionAttributeNames(this.expressionAttributeNames);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Get}.
     */
    public static final class Builder implements ShapeBuilder<Get> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Map<String, AttributeValue> key;
        private String tableName;
        private String projectionExpression;
        private Map<String, String> expressionAttributeNames;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A map of attribute names to <code>AttributeValue</code> objects that specifies the primary key of the item to
         * retrieve.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder key(Map<String, AttributeValue> key) {
            this.key = Objects.requireNonNull(key, "key cannot be null");
            tracker.setMember($SCHEMA_KEY);
            return this;
        }

        /**
         * The name of the table from which to retrieve the specified item. You can also provide the Amazon Resource Name
         * (ARN) of the table in this parameter.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
            tracker.setMember($SCHEMA_TABLE_NAME);
            return this;
        }

        /**
         * A string that identifies one or more attributes of the specified item to retrieve from the table. The attributes
         * in the expression must be separated by commas. If no attribute names are specified, then all attributes of the
         * specified item are returned. If any of the requested attributes are not found, they do not appear in the result.
         *
         * @return this builder.
         */
        public Builder projectionExpression(String projectionExpression) {
            this.projectionExpression = projectionExpression;
            return this;
        }

        /**
         * One or more substitution tokens for attribute names in the ProjectionExpression parameter.
         *
         * @return this builder.
         */
        public Builder expressionAttributeNames(Map<String, String> expressionAttributeNames) {
            this.expressionAttributeNames = expressionAttributeNames;
            return this;
        }

        @Override
        public Get build() {
            tracker.validate();
            return new Get(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> key((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_KEY, member, value));
                case 1 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 2 -> projectionExpression((String) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_EXPRESSION, member, value));
                case 3 -> expressionAttributeNames((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Get> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_KEY)) {
                key(Collections.emptyMap());
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
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
                    case 0 -> builder.key(SharedSerde.deserializeKey(member, de));
                    case 1 -> builder.tableName(de.readString(member));
                    case 2 -> builder.projectionExpression(de.readString(member));
                    case 3 -> builder.expressionAttributeNames(SharedSerde.deserializeExpressionAttributeNameMap(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
