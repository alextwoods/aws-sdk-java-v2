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
 * For the <code>UpdateItem</code> operation, represents the attributes to be modified, the action to perform on each,
 * and the new value for each.
 *
 * <p>You cannot use <code>UpdateItem</code> to update any primary key attributes. Instead, you will need to delete the
 * item, and then use <code>PutItem</code> to create a new item with new attributes.
 *
 * <p>Attribute values cannot be null; string and binary type attributes must have lengths greater than zero; and set
 * type attributes must not be empty. Requests with empty values will be rejected with a <code>ValidationException</code>
 * exception.
 */
@SmithyGenerated
public final class AttributeValueUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ATTRIBUTE_VALUE_UPDATE;
    private static final Schema $SCHEMA_VALUE = $SCHEMA.member("Value");
    private static final Schema $SCHEMA_ACTION = $SCHEMA.member("Action");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient AttributeValue value;
    private final transient AttributeAction action;

    private AttributeValueUpdate(Builder builder) {
        this.value = builder.value;
        this.action = builder.action;
    }

    /**
     * Represents the data for an attribute.
     *
     * <p>Each attribute value is described as a name-value pair. The name is the data type, and the value is the data
     * itself.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes">Data Types</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public AttributeValue getValue() {
        return value;
    }

    /**
     * Specifies how to perform the update. Valid values are <code>PUT</code> (default), <code>DELETE</code>, and <code>
     * ADD</code>. The behavior depends on whether the specified primary key already exists in the table.
     *
     * <p><b>If an item with the specified <i>Key</i> is found in the table:</b>
     *
     * <ul>
     *   <li>
     *     <code>PUT</code> - Adds the specified attribute to the item. If the attribute already exists, it is
     *     replaced by the new value.
     *   </li>
     *   <li>
     *     <code>DELETE</code> - If no value is specified, the attribute and its value are removed from the item.
     *     The data type of the specified value must match the existing value's data type.If a <i>set</i> of values
     *     is specified, then those values are subtracted from the old set. For example, if the attribute value was
     *     the set <code>[a,b,c]</code> and the <code>DELETE</code> action specified <code>[a,c]</code>, then the
     *     final attribute value would be <code>[b]</code>. Specifying an empty set is an error.
     *   </li>
     *   <li>
     *     <code>ADD</code> - If the attribute does not already exist, then the attribute and its values are added
     *     to the item. If the attribute does exist, then the behavior of <code>ADD</code> depends on the data type
     *     of the attribute:
     *
     *     <ul>
     *       <li>
     *         If the existing attribute is a number, and if <code>Value</code> is also a number, then the <code>
     *         Value</code> is mathematically added to the existing attribute. If <code>Value</code> is a
     *         negative number, then it is subtracted from the existing attribute. If you use <code>ADD</code>
     *         to increment or decrement a number value for an item that doesn't exist before the update,
     *         DynamoDB uses 0 as the initial value.In addition, if you use <code>ADD</code> to update an
     *         existing item, and intend to increment or decrement an attribute value which does not yet exist,
     *         DynamoDB uses <code>0</code> as the initial value. For example, suppose that the item you want to
     *         update does not yet have an attribute named <i>itemcount</i>, but you decide to <code>ADD</code>
     *         the number <code>3</code> to this attribute anyway, even though it currently does not exist.
     *         DynamoDB will create the <i>itemcount</i> attribute, set its initial value to <code>0</code>, and
     *         finally add <code>3</code> to it. The result will be a new <i>itemcount</i> attribute in the
     *         item, with a value of <code>3</code>.
     *       </li>
     *       <li>
     *         If the existing data type is a set, and if the <code>Value</code> is also a set, then the <code>
     *         Value</code> is added to the existing set. (This is a <i>set</i> operation, not mathematical
     *         addition.) For example, if the attribute value was the set <code>[1,2]</code>, and the <code>ADD</code>
     *         action specified <code>[3]</code>, then the final attribute value would be <code>[1,2,3]</code>.
     *         An error occurs if an Add action is specified for a set attribute and the attribute type
     *         specified does not match the existing set type. Both sets must have the same primitive data type.
     *         For example, if the existing data type is a set of strings, the <code>Value</code> must also be a
     *         set of strings. The same holds true for number sets and binary sets.
     *       </li>
     *     </ul>This action is only valid for an existing attribute whose data type is number or is a set. Do not
     *     use <code>ADD</code> for any other data types.
     *   </li>
     * </ul>
     *
     * <p><b>If no item with the specified <i>Key</i> is found:</b>
     *
     * <ul>
     *   <li>
     *     <code>PUT</code> - DynamoDB creates a new item with the specified primary key, and then adds the
     *     attribute.
     *   </li>
     *   <li>
     *     <code>DELETE</code> - Nothing happens; there is no attribute to delete.
     *   </li>
     *   <li>
     *     <code>ADD</code> - DynamoDB creates a new item with the supplied primary key and number (or set) for the
     *     attribute value. The only data types allowed are number, number set, string set or binary set.
     *   </li>
     * </ul>
     */
    public AttributeAction getAction() {
        return action;
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
        AttributeValueUpdate that = (AttributeValueUpdate) other;
        return Objects.equals(this.action, that.action)
               && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(value);
        $hc = 31 * $hc + Objects.hashCode(action);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (value != null) {
            serializer.writeStruct($SCHEMA_VALUE, value);
        }
        if (action != null) {
            serializer.writeString($SCHEMA_ACTION, action.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACTION, member, action);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link AttributeValueUpdate}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.value(this.value);
        builder.action(this.action);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AttributeValueUpdate}.
     */
    public static final class Builder implements ShapeBuilder<AttributeValueUpdate> {
        private AttributeValue value;
        private AttributeAction action;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the data for an attribute.
         *
         * <p>Each attribute value is described as a name-value pair. The name is the data type, and the value is the data
         * itself.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes">Data Types</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder value(AttributeValue value) {
            this.value = value;
            return this;
        }

        /**
         * Specifies how to perform the update. Valid values are <code>PUT</code> (default), <code>DELETE</code>, and <code>
         * ADD</code>. The behavior depends on whether the specified primary key already exists in the table.
         *
         * <p><b>If an item with the specified <i>Key</i> is found in the table:</b>
         *
         * <ul>
         *   <li>
         *     <code>PUT</code> - Adds the specified attribute to the item. If the attribute already exists, it is
         *     replaced by the new value.
         *   </li>
         *   <li>
         *     <code>DELETE</code> - If no value is specified, the attribute and its value are removed from the item.
         *     The data type of the specified value must match the existing value's data type.If a <i>set</i> of values
         *     is specified, then those values are subtracted from the old set. For example, if the attribute value was
         *     the set <code>[a,b,c]</code> and the <code>DELETE</code> action specified <code>[a,c]</code>, then the
         *     final attribute value would be <code>[b]</code>. Specifying an empty set is an error.
         *   </li>
         *   <li>
         *     <code>ADD</code> - If the attribute does not already exist, then the attribute and its values are added
         *     to the item. If the attribute does exist, then the behavior of <code>ADD</code> depends on the data type
         *     of the attribute:
         *
         *     <ul>
         *       <li>
         *         If the existing attribute is a number, and if <code>Value</code> is also a number, then the <code>
         *         Value</code> is mathematically added to the existing attribute. If <code>Value</code> is a
         *         negative number, then it is subtracted from the existing attribute. If you use <code>ADD</code>
         *         to increment or decrement a number value for an item that doesn't exist before the update,
         *         DynamoDB uses 0 as the initial value.In addition, if you use <code>ADD</code> to update an
         *         existing item, and intend to increment or decrement an attribute value which does not yet exist,
         *         DynamoDB uses <code>0</code> as the initial value. For example, suppose that the item you want to
         *         update does not yet have an attribute named <i>itemcount</i>, but you decide to <code>ADD</code>
         *         the number <code>3</code> to this attribute anyway, even though it currently does not exist.
         *         DynamoDB will create the <i>itemcount</i> attribute, set its initial value to <code>0</code>, and
         *         finally add <code>3</code> to it. The result will be a new <i>itemcount</i> attribute in the
         *         item, with a value of <code>3</code>.
         *       </li>
         *       <li>
         *         If the existing data type is a set, and if the <code>Value</code> is also a set, then the <code>
         *         Value</code> is added to the existing set. (This is a <i>set</i> operation, not mathematical
         *         addition.) For example, if the attribute value was the set <code>[1,2]</code>, and the <code>ADD</code>
         *         action specified <code>[3]</code>, then the final attribute value would be <code>[1,2,3]</code>.
         *         An error occurs if an Add action is specified for a set attribute and the attribute type
         *         specified does not match the existing set type. Both sets must have the same primitive data type.
         *         For example, if the existing data type is a set of strings, the <code>Value</code> must also be a
         *         set of strings. The same holds true for number sets and binary sets.
         *       </li>
         *     </ul>This action is only valid for an existing attribute whose data type is number or is a set. Do not
         *     use <code>ADD</code> for any other data types.
         *   </li>
         * </ul>
         *
         * <p><b>If no item with the specified <i>Key</i> is found:</b>
         *
         * <ul>
         *   <li>
         *     <code>PUT</code> - DynamoDB creates a new item with the specified primary key, and then adds the
         *     attribute.
         *   </li>
         *   <li>
         *     <code>DELETE</code> - Nothing happens; there is no attribute to delete.
         *   </li>
         *   <li>
         *     <code>ADD</code> - DynamoDB creates a new item with the supplied primary key and number (or set) for the
         *     attribute value. The only data types allowed are number, number set, string set or binary set.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder action(AttributeAction action) {
            this.action = action;
            return this;
        }

        @Override
        public AttributeValueUpdate build() {
            return new AttributeValueUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> value((AttributeValue) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value));
                case 1 -> action((AttributeAction) SchemaUtils.validateSameMember($SCHEMA_ACTION, member, value));
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
                    case 0 -> builder.value(AttributeValue.builder().deserializeMember(de, member).build());
                    case 1 -> builder.action(AttributeAction.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
