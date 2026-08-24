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
 * Represents a request to perform a <code>PutItem</code> operation.
 */
@SmithyGenerated
public final class Put implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.PUT;
    private static final Schema $SCHEMA_ITEM = $SCHEMA.member("Item");
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_CONDITION_EXPRESSION = $SCHEMA.member("ConditionExpression");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_NAMES = $SCHEMA.member("ExpressionAttributeNames");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_VALUES = $SCHEMA.member("ExpressionAttributeValues");
    private static final Schema $SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE = $SCHEMA.member("ReturnValuesOnConditionCheckFailure");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, AttributeValue> item;
    private final transient String tableName;
    private final transient String conditionExpression;
    private final transient Map<String, String> expressionAttributeNames;
    private final transient Map<String, AttributeValue> expressionAttributeValues;
    private final transient ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure;

    private Put(Builder builder) {
        this.item = Collections.unmodifiableMap(builder.item);
        this.tableName = builder.tableName;
        this.conditionExpression = builder.conditionExpression;
        this.expressionAttributeNames = builder.expressionAttributeNames == null ? null : Collections.unmodifiableMap(builder.expressionAttributeNames);
        this.expressionAttributeValues = builder.expressionAttributeValues == null ? null : Collections.unmodifiableMap(builder.expressionAttributeValues);
        this.returnValuesOnConditionCheckFailure = builder.returnValuesOnConditionCheckFailure;
    }

    /**
     * A map of attribute name to attribute values, representing the primary key of the item to be written by
     * <code>PutItem</code>. All of the table's primary key attributes must be specified, and their data types must
     * match those of the table's key schema. If any attributes are present in the item that are part of an index key
     * schema for the table, their types must match the index key schema.
     */
    public Map<String, AttributeValue> getItem() {
        return item;
    }

    public boolean hasItem() {
        return true;
    }

    /**
     * Name of the table in which to write the item. You can also provide the Amazon Resource Name (ARN) of the table in
     * this parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * A condition that must be satisfied in order for a conditional update to succeed.
     */
    public String getConditionExpression() {
        return conditionExpression;
    }

    /**
     * One or more substitution tokens for attribute names in an expression.
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

    /**
     * One or more values that can be substituted in an expression.
     */
    public Map<String, AttributeValue> getExpressionAttributeValues() {
        if (expressionAttributeValues == null) {
            return Collections.emptyMap();
        }
        return expressionAttributeValues;
    }

    public boolean hasExpressionAttributeValues() {
        return expressionAttributeValues != null;
    }

    /**
     * Use <code>ReturnValuesOnConditionCheckFailure</code> to get the item attributes if the <code>Put</code> condition
     * fails. For <code>ReturnValuesOnConditionCheckFailure</code>, the valid values are: NONE and ALL_OLD.
     */
    public ReturnValuesOnConditionCheckFailure getReturnValuesOnConditionCheckFailure() {
        return returnValuesOnConditionCheckFailure;
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
        Put that = (Put) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.conditionExpression, that.conditionExpression)
               && Objects.equals(this.returnValuesOnConditionCheckFailure, that.returnValuesOnConditionCheckFailure)
               && Objects.equals(this.item, that.item)
               && Objects.equals(this.expressionAttributeNames, that.expressionAttributeNames)
               && Objects.equals(this.expressionAttributeValues, that.expressionAttributeValues);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(item);
        $hc = 31 * $hc + Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(conditionExpression);
        $hc = 31 * $hc + Objects.hashCode(expressionAttributeNames);
        $hc = 31 * $hc + Objects.hashCode(expressionAttributeValues);
        $hc = 31 * $hc + Objects.hashCode(returnValuesOnConditionCheckFailure);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeMap($SCHEMA_ITEM, item, item.size(), SharedSerde.PutItemInputAttributeMapSerializer.INSTANCE);
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        if (conditionExpression != null) {
            serializer.writeString($SCHEMA_CONDITION_EXPRESSION, conditionExpression);
        }
        if (expressionAttributeNames != null) {
            serializer.writeMap($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, expressionAttributeNames, expressionAttributeNames.size(), SharedSerde.ExpressionAttributeNameMapSerializer.INSTANCE);
        }
        if (expressionAttributeValues != null) {
            serializer.writeMap($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, expressionAttributeValues, expressionAttributeValues.size(), SharedSerde.ExpressionAttributeValueMapSerializer.INSTANCE);
        }
        if (returnValuesOnConditionCheckFailure != null) {
            serializer.writeString($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, returnValuesOnConditionCheckFailure.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, item);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONDITION_EXPRESSION, member, conditionExpression);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, expressionAttributeNames);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, expressionAttributeValues);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, member, returnValuesOnConditionCheckFailure);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Put}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.item(this.item);
        builder.tableName(this.tableName);
        builder.conditionExpression(this.conditionExpression);
        builder.expressionAttributeNames(this.expressionAttributeNames);
        builder.expressionAttributeValues(this.expressionAttributeValues);
        builder.returnValuesOnConditionCheckFailure(this.returnValuesOnConditionCheckFailure);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Put}.
     */
    public static final class Builder implements ShapeBuilder<Put> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Map<String, AttributeValue> item;
        private String tableName;
        private String conditionExpression;
        private Map<String, String> expressionAttributeNames;
        private Map<String, AttributeValue> expressionAttributeValues;
        private ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A map of attribute name to attribute values, representing the primary key of the item to be written by
         * <code>PutItem</code>. All of the table's primary key attributes must be specified, and their data types must
         * match those of the table's key schema. If any attributes are present in the item that are part of an index key
         * schema for the table, their types must match the index key schema.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder item(Map<String, AttributeValue> item) {
            this.item = Objects.requireNonNull(item, "item cannot be null");
            tracker.setMember($SCHEMA_ITEM);
            return this;
        }

        /**
         * Name of the table in which to write the item. You can also provide the Amazon Resource Name (ARN) of the table in
         * this parameter.
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
         * A condition that must be satisfied in order for a conditional update to succeed.
         *
         * @return this builder.
         */
        public Builder conditionExpression(String conditionExpression) {
            this.conditionExpression = conditionExpression;
            return this;
        }

        /**
         * One or more substitution tokens for attribute names in an expression.
         *
         * @return this builder.
         */
        public Builder expressionAttributeNames(Map<String, String> expressionAttributeNames) {
            this.expressionAttributeNames = expressionAttributeNames;
            return this;
        }

        /**
         * One or more values that can be substituted in an expression.
         *
         * @return this builder.
         */
        public Builder expressionAttributeValues(Map<String, AttributeValue> expressionAttributeValues) {
            this.expressionAttributeValues = expressionAttributeValues;
            return this;
        }

        /**
         * Use <code>ReturnValuesOnConditionCheckFailure</code> to get the item attributes if the <code>Put</code> condition
         * fails. For <code>ReturnValuesOnConditionCheckFailure</code>, the valid values are: NONE and ALL_OLD.
         *
         * @return this builder.
         */
        public Builder returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure) {
            this.returnValuesOnConditionCheckFailure = returnValuesOnConditionCheckFailure;
            return this;
        }

        @Override
        public Put build() {
            tracker.validate();
            return new Put(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> item((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_ITEM, member, value));
                case 1 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 2 -> conditionExpression((String) SchemaUtils.validateSameMember($SCHEMA_CONDITION_EXPRESSION, member, value));
                case 3 -> expressionAttributeNames((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, value));
                case 4 -> expressionAttributeValues((Map<String, AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_VALUES, member, value));
                case 5 -> returnValuesOnConditionCheckFailure((ReturnValuesOnConditionCheckFailure) SchemaUtils.validateSameMember($SCHEMA_RETURN_VALUES_ON_CONDITION_CHECK_FAILURE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Put> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ITEM)) {
                item(Collections.emptyMap());
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
                    case 0 -> builder.item(SharedSerde.deserializePutItemInputAttributeMap(member, de));
                    case 1 -> builder.tableName(de.readString(member));
                    case 2 -> builder.conditionExpression(de.readString(member));
                    case 3 -> builder.expressionAttributeNames(SharedSerde.deserializeExpressionAttributeNameMap(member, de));
                    case 4 -> builder.expressionAttributeValues(SharedSerde.deserializeExpressionAttributeValueMap(member, de));
                    case 5 -> builder.returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
