package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.Collections;
import java.util.List;
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
 * Represents a set of primary keys and, for each key, the attributes to retrieve from the table.
 *
 * <p>For each primary key, you must provide <i>all</i> of the key attributes. For example, with a simple primary key,
 * you only need to provide the partition key. For a composite primary key, you must provide <i>both</i> the partition
 * key and the sort key.
 */
@SmithyGenerated
public final class KeysAndAttributes implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.KEYS_AND_ATTRIBUTES;
    private static final Schema $SCHEMA_KEYS = $SCHEMA.member("Keys");
    private static final Schema $SCHEMA_ATTRIBUTES_TO_GET = $SCHEMA.member("AttributesToGet");
    private static final Schema $SCHEMA_CONSISTENT_READ = $SCHEMA.member("ConsistentRead");
    private static final Schema $SCHEMA_PROJECTION_EXPRESSION = $SCHEMA.member("ProjectionExpression");
    private static final Schema $SCHEMA_EXPRESSION_ATTRIBUTE_NAMES = $SCHEMA.member("ExpressionAttributeNames");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<Map<String, AttributeValue>> keys;
    private final transient List<String> attributesToGet;
    private final transient Boolean consistentRead;
    private final transient String projectionExpression;
    private final transient Map<String, String> expressionAttributeNames;

    private KeysAndAttributes(Builder builder) {
        this.keys = Collections.unmodifiableList(builder.keys);
        this.attributesToGet = builder.attributesToGet == null ? null : Collections.unmodifiableList(builder.attributesToGet);
        this.consistentRead = builder.consistentRead;
        this.projectionExpression = builder.projectionExpression;
        this.expressionAttributeNames = builder.expressionAttributeNames == null ? null : Collections.unmodifiableMap(builder.expressionAttributeNames);
    }

    /**
     * The primary key attribute values that define the items and the attributes associated with the items.
     */
    public List<Map<String, AttributeValue>> getKeys() {
        return keys;
    }

    public boolean hasKeys() {
        return true;
    }

    /**
     * This is a legacy parameter. Use <code>ProjectionExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.html">Legacy
     * Conditional Parameters</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public List<String> getAttributesToGet() {
        if (attributesToGet == null) {
            return Collections.emptyList();
        }
        return attributesToGet;
    }

    public boolean hasAttributesToGet() {
        return attributesToGet != null;
    }

    /**
     * The consistency of a read operation. If set to <code>true</code>, then a strongly consistent read is used;
     * otherwise, an eventually consistent read is used.
     */
    public Boolean isConsistentRead() {
        return consistentRead;
    }

    /**
     * A string that identifies one or more attributes to retrieve from the table. These attributes can include scalars,
     * sets, or elements of a JSON document. The attributes in the <code>ProjectionExpression</code> must be separated
     * by commas.
     *
     * <p>If no attribute names are specified, then all attributes will be returned. If any of the requested attributes
     * are not found, they will not appear in the result.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public String getProjectionExpression() {
        return projectionExpression;
    }

    /**
     * One or more substitution tokens for attribute names in an expression. The following are some use cases for using <code>
     * ExpressionAttributeNames</code>:
     *
     * <ul>
     *   <li>
     *     To access an attribute whose name conflicts with a DynamoDB reserved word.
     *   </li>
     *   <li>
     *     To create a placeholder for repeating occurrences of an attribute name in an expression.
     *   </li>
     *   <li>
     *     To prevent special characters in an attribute name from being misinterpreted in an expression.
     *   </li>
     * </ul>
     *
     * <p>Use the <b>#</b> character in an expression to dereference an attribute name. For example, consider the
     * following attribute name:
     *
     * <ul>
     *   <li>
     *     <code>Percentile</code>
     *   </li>
     * </ul>
     *
     * <p>The name of this attribute conflicts with a reserved word, so it cannot be used directly in an expression.
     * (For the complete list of reserved words, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html">Reserved Words</a> in the <i>Amazon DynamoDB Developer Guide</i>
     * ). To work around this, you could specify the following for <code>ExpressionAttributeNames</code>:
     *
     * <ul>
     *   <li>
     *     <code>{"#P":"Percentile"}</code>
     *   </li>
     * </ul>
     *
     * <p>You could then use this substitution in an expression, as in this example:
     *
     * <ul>
     *   <li>
     *     <code>#P = :val</code>
     *   </li>
     * </ul>
     *
     * <p>Tokens that begin with the <b>:</b> character are <i>expression attribute values</i>, which are placeholders
     * for the actual value at runtime.
     *
     * <p>For more information on expression attribute names, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing Item Attributes</a> in the <i>Amazon
     * DynamoDB Developer Guide</i>.
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
        KeysAndAttributes that = (KeysAndAttributes) other;
        return Objects.equals(this.consistentRead, that.consistentRead)
               && Objects.equals(this.projectionExpression, that.projectionExpression)
               && Objects.equals(this.keys, that.keys)
               && Objects.equals(this.attributesToGet, that.attributesToGet)
               && Objects.equals(this.expressionAttributeNames, that.expressionAttributeNames);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(keys);
        $hc = 31 * $hc + Objects.hashCode(attributesToGet);
        $hc = 31 * $hc + Objects.hashCode(consistentRead);
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
        serializer.writeList($SCHEMA_KEYS, keys, keys.size(), SharedSerde.KeyListSerializer.INSTANCE);
        if (attributesToGet != null) {
            serializer.writeList($SCHEMA_ATTRIBUTES_TO_GET, attributesToGet, attributesToGet.size(), SharedSerde.AttributeNameListSerializer.INSTANCE);
        }
        if (consistentRead != null) {
            serializer.writeBoolean($SCHEMA_CONSISTENT_READ, consistentRead);
        }
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
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEYS, member, keys);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTES_TO_GET, member, attributesToGet);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_CONSISTENT_READ, member, consistentRead);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_EXPRESSION, member, projectionExpression);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, expressionAttributeNames);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link KeysAndAttributes}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.keys(this.keys);
        builder.attributesToGet(this.attributesToGet);
        builder.consistentRead(this.consistentRead);
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
     * Builder for {@link KeysAndAttributes}.
     */
    public static final class Builder implements ShapeBuilder<KeysAndAttributes> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<Map<String, AttributeValue>> keys;
        private List<String> attributesToGet;
        private Boolean consistentRead;
        private String projectionExpression;
        private Map<String, String> expressionAttributeNames;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The primary key attribute values that define the items and the attributes associated with the items.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder keys(List<Map<String, AttributeValue>> keys) {
            this.keys = Objects.requireNonNull(keys, "keys cannot be null");
            tracker.setMember($SCHEMA_KEYS);
            return this;
        }

        /**
         * This is a legacy parameter. Use <code>ProjectionExpression</code> instead. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.html">Legacy
         * Conditional Parameters</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder attributesToGet(List<String> attributesToGet) {
            this.attributesToGet = attributesToGet;
            return this;
        }

        /**
         * The consistency of a read operation. If set to <code>true</code>, then a strongly consistent read is used;
         * otherwise, an eventually consistent read is used.
         *
         * @return this builder.
         */
        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }

        /**
         * A string that identifies one or more attributes to retrieve from the table. These attributes can include scalars,
         * sets, or elements of a JSON document. The attributes in the <code>ProjectionExpression</code> must be separated
         * by commas.
         *
         * <p>If no attribute names are specified, then all attributes will be returned. If any of the requested attributes
         * are not found, they will not appear in the result.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing Item Attributes</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder projectionExpression(String projectionExpression) {
            this.projectionExpression = projectionExpression;
            return this;
        }

        /**
         * One or more substitution tokens for attribute names in an expression. The following are some use cases for using <code>
         * ExpressionAttributeNames</code>:
         *
         * <ul>
         *   <li>
         *     To access an attribute whose name conflicts with a DynamoDB reserved word.
         *   </li>
         *   <li>
         *     To create a placeholder for repeating occurrences of an attribute name in an expression.
         *   </li>
         *   <li>
         *     To prevent special characters in an attribute name from being misinterpreted in an expression.
         *   </li>
         * </ul>
         *
         * <p>Use the <b>#</b> character in an expression to dereference an attribute name. For example, consider the
         * following attribute name:
         *
         * <ul>
         *   <li>
         *     <code>Percentile</code>
         *   </li>
         * </ul>
         *
         * <p>The name of this attribute conflicts with a reserved word, so it cannot be used directly in an expression.
         * (For the complete list of reserved words, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ReservedWords.html">Reserved Words</a> in the <i>Amazon DynamoDB Developer Guide</i>
         * ). To work around this, you could specify the following for <code>ExpressionAttributeNames</code>:
         *
         * <ul>
         *   <li>
         *     <code>{"#P":"Percentile"}</code>
         *   </li>
         * </ul>
         *
         * <p>You could then use this substitution in an expression, as in this example:
         *
         * <ul>
         *   <li>
         *     <code>#P = :val</code>
         *   </li>
         * </ul>
         *
         * <p>Tokens that begin with the <b>:</b> character are <i>expression attribute values</i>, which are placeholders
         * for the actual value at runtime.
         *
         * <p>For more information on expression attribute names, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.AccessingItemAttributes.html">Accessing Item Attributes</a> in the <i>Amazon
         * DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder expressionAttributeNames(Map<String, String> expressionAttributeNames) {
            this.expressionAttributeNames = expressionAttributeNames;
            return this;
        }

        @Override
        public KeysAndAttributes build() {
            tracker.validate();
            return new KeysAndAttributes(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> keys((List<Map<String, AttributeValue>>) SchemaUtils.validateSameMember($SCHEMA_KEYS, member, value));
                case 1 -> attributesToGet((List<String>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTES_TO_GET, member, value));
                case 2 -> consistentRead((Boolean) SchemaUtils.validateSameMember($SCHEMA_CONSISTENT_READ, member, value));
                case 3 -> projectionExpression((String) SchemaUtils.validateSameMember($SCHEMA_PROJECTION_EXPRESSION, member, value));
                case 4 -> expressionAttributeNames((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_EXPRESSION_ATTRIBUTE_NAMES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<KeysAndAttributes> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_KEYS)) {
                keys(Collections.emptyList());
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
                    case 0 -> builder.keys(SharedSerde.deserializeKeyList(member, de));
                    case 1 -> builder.attributesToGet(SharedSerde.deserializeAttributeNameList(member, de));
                    case 2 -> builder.consistentRead(de.readBoolean(member));
                    case 3 -> builder.projectionExpression(de.readString(member));
                    case 4 -> builder.expressionAttributeNames(SharedSerde.deserializeExpressionAttributeNameMap(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
