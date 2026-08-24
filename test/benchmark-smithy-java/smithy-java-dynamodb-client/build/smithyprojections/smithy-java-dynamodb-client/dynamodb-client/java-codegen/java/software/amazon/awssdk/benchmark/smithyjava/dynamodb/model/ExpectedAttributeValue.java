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
 * Represents a condition to be compared with an attribute value. This condition can be used with <code>DeleteItem</code>
 * , <code>PutItem</code>, or <code>UpdateItem</code> operations; if the comparison evaluates to true, the operation
 * succeeds; if not, the operation fails. You can use <code>ExpectedAttributeValue</code> in one of two different ways:
 *
 * <ul>
 *   <li>
 *     Use <code>AttributeValueList</code> to specify one or more values to compare against an attribute. Use <code>
 *     ComparisonOperator</code> to specify how you want to perform the comparison. If the comparison evaluates to
 *     true, then the conditional operation succeeds.
 *   </li>
 *   <li>
 *     Use <code>Value</code> to specify a value that DynamoDB will compare against an attribute. If the values
 *     match, then <code>ExpectedAttributeValue</code> evaluates to true and the conditional operation succeeds.
 *     Optionally, you can also set <code>Exists</code> to false, indicating that you <i>do not</i> expect to find
 *     the attribute value in the table. In this case, the conditional operation succeeds only if the comparison
 *     evaluates to false.
 *   </li>
 * </ul>
 *
 * <p><code>Value</code> and <code>Exists</code> are incompatible with <code>AttributeValueList</code> and <code>
 * ComparisonOperator</code>. Note that if you use both sets of parameters at once, DynamoDB will return a <code>
 * ValidationException</code> exception.
 */
@SmithyGenerated
public final class ExpectedAttributeValue implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.EXPECTED_ATTRIBUTE_VALUE;
    private static final Schema $SCHEMA_VALUE = $SCHEMA.member("Value");
    private static final Schema $SCHEMA_EXISTS = $SCHEMA.member("Exists");
    private static final Schema $SCHEMA_COMPARISON_OPERATOR = $SCHEMA.member("ComparisonOperator");
    private static final Schema $SCHEMA_ATTRIBUTE_VALUE_LIST = $SCHEMA.member("AttributeValueList");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient AttributeValue value;
    private final transient Boolean exists;
    private final transient ComparisonOperator comparisonOperator;
    private final transient List<AttributeValue> attributeValueList;

    private ExpectedAttributeValue(Builder builder) {
        this.value = builder.value;
        this.exists = builder.exists;
        this.comparisonOperator = builder.comparisonOperator;
        this.attributeValueList = builder.attributeValueList == null ? null : Collections.unmodifiableList(builder.attributeValueList);
    }

    /**
     * Represents the data for the expected attribute.
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
     * Causes DynamoDB to evaluate the value before attempting a conditional operation:
     *
     * <ul>
     *   <li>
     *     If <code>Exists</code> is <code>true</code>, DynamoDB will check to see if that attribute value already
     *     exists in the table. If it is found, then the operation succeeds. If it is not found, the operation fails
     *     with a <code>ConditionCheckFailedException</code>.
     *   </li>
     *   <li>
     *     If <code>Exists</code> is <code>false</code>, DynamoDB assumes that the attribute value does not exist in
     *     the table. If in fact the value does not exist, then the assumption is valid and the operation succeeds.
     *     If the value is found, despite the assumption that it does not exist, the operation fails with a <code>
     *     ConditionCheckFailedException</code>.
     *   </li>
     * </ul>
     *
     * <p>The default setting for <code>Exists</code> is <code>true</code>. If you supply a <code>Value</code> all by
     * itself, DynamoDB assumes the attribute exists: You don't have to set <code>Exists</code> to <code>true</code>,
     * because it is implied.
     *
     * <p>DynamoDB returns a <code>ValidationException</code> if:
     *
     * <ul>
     *   <li>
     *     <code>Exists</code> is <code>true</code> but there is no <code>Value</code> to check. (You expect a value
     *     to exist, but don't specify what that value is.)
     *   </li>
     *   <li>
     *     <code>Exists</code> is <code>false</code> but you also provide a <code>Value</code>. (You cannot expect
     *     an attribute to have a value, while also expecting it not to exist.)
     *   </li>
     * </ul>
     */
    public Boolean isExists() {
        return exists;
    }

    /**
     * A comparator for evaluating attributes in the <code>AttributeValueList</code>. For example, equals, greater than,
     * less than, etc.
     *
     * <p>The following comparison operators are available:
     *
     * <p><code>EQ | NE | LE | LT | GE | GT | NOT_NULL | NULL | CONTAINS | NOT_CONTAINS | BEGINS_WITH | IN | BETWEEN</code>
     *
     * <p>The following are descriptions of each comparison operator.
     *
     * <ul>
     *   <li>
     *     <code>EQ</code> : Equal. <code>EQ</code> is supported for all data types, including lists and maps.<code>
     *     AttributeValueList</code> can contain only one <code>AttributeValue</code> element of type String,
     *     Number, Binary, String Set, Number Set, or Binary Set. If an item contains an <code>AttributeValue</code>
     *     element of a different type than the one provided in the request, the value does not match. For example, <code>
     *     {"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>{"N":"6"}</code> does not equal <code>
     *     {"NS":["6", "2", "1"]}</code>.
     *   </li>
     *   <li>
     *     <code>NE</code> : Not equal. <code>NE</code> is supported for all data types, including lists and maps.<code>
     *     AttributeValueList</code> can contain only one <code>AttributeValue</code> of type String, Number,
     *     Binary, String Set, Number Set, or Binary Set. If an item contains an <code>AttributeValue</code> of a
     *     different type than the one provided in the request, the value does not match. For example, <code>
     *     {"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>{"N":"6"}</code> does not equal <code>
     *     {"NS":["6", "2", "1"]}</code>.
     *   </li>
     *   <li>
     *     <code>LE</code> : Less than or equal. <code>AttributeValueList</code> can contain only one <code>
     *     AttributeValue</code> element of type String, Number, or Binary (not a set type). If an item contains an <code>
     *     AttributeValue</code> element of a different type than the one provided in the request, the value does
     *     not match. For example, <code>{"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>
     *     {"N":"6"}</code> does not compare to <code>{"NS":["6", "2", "1"]}</code>.
     *   </li>
     *   <li>
     *     <code>LT</code> : Less than. <code>AttributeValueList</code> can contain only one <code>AttributeValue</code>
     *     of type String, Number, or Binary (not a set type). If an item contains an <code>AttributeValue</code>
     *     element of a different type than the one provided in the request, the value does not match. For example, <code>
     *     {"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>{"N":"6"}</code> does not compare to <code>
     *     {"NS":["6", "2", "1"]}</code>.
     *   </li>
     *   <li>
     *     <code>GE</code> : Greater than or equal. <code>AttributeValueList</code> can contain only one <code>
     *     AttributeValue</code> element of type String, Number, or Binary (not a set type). If an item contains an <code>
     *     AttributeValue</code> element of a different type than the one provided in the request, the value does
     *     not match. For example, <code>{"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>
     *     {"N":"6"}</code> does not compare to <code>{"NS":["6", "2", "1"]}</code>.
     *   </li>
     *   <li>
     *     <code>GT</code> : Greater than. <code>AttributeValueList</code> can contain only one <code>AttributeValue</code>
     *     element of type String, Number, or Binary (not a set type). If an item contains an <code>AttributeValue</code>
     *     element of a different type than the one provided in the request, the value does not match. For example, <code>
     *     {"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>{"N":"6"}</code> does not compare to <code>
     *     {"NS":["6", "2", "1"]}</code>.
     *   </li>
     *   <li>
     *     <code>NOT_NULL</code> : The attribute exists. <code>NOT_NULL</code> is supported for all data types,
     *     including lists and maps.This operator tests for the existence of an attribute, not its data type. If the
     *     data type of attribute "<code>a</code>" is null, and you evaluate it using <code>NOT_NULL</code>, the
     *     result is a Boolean <code>true</code>. This result is because the attribute "<code>a</code>" exists; its
     *     data type is not relevant to the <code>NOT_NULL</code> comparison operator.
     *   </li>
     *   <li>
     *     <code>NULL</code> : The attribute does not exist. <code>NULL</code> is supported for all data types,
     *     including lists and maps.This operator tests for the nonexistence of an attribute, not its data type. If
     *     the data type of attribute "<code>a</code>" is null, and you evaluate it using <code>NULL</code>, the
     *     result is a Boolean <code>false</code>. This is because the attribute "<code>a</code>" exists; its data
     *     type is not relevant to the <code>NULL</code> comparison operator.
     *   </li>
     *   <li>
     *     <code>CONTAINS</code> : Checks for a subsequence, or value in a set.<code>AttributeValueList</code> can
     *     contain only one <code>AttributeValue</code> element of type String, Number, or Binary (not a set type).
     *     If the target attribute of the comparison is of type String, then the operator checks for a substring
     *     match. If the target attribute of the comparison is of type Binary, then the operator looks for a
     *     subsequence of the target that matches the input. If the target attribute of the comparison is a set ("<code>
     *     SS</code>", "<code>NS</code>", or "<code>BS</code>"), then the operator evaluates to true if it finds an
     *     exact match with any member of the set.CONTAINS is supported for lists: When evaluating "<code>a CONTAINS
     *     b</code>", "<code>a</code>" can be a list; however, "<code>b</code>" cannot be a set, a map, or a list.
     *   </li>
     *   <li>
     *     <code>NOT_CONTAINS</code> : Checks for absence of a subsequence, or absence of a value in a set.<code>
     *     AttributeValueList</code> can contain only one <code>AttributeValue</code> element of type String,
     *     Number, or Binary (not a set type). If the target attribute of the comparison is a String, then the
     *     operator checks for the absence of a substring match. If the target attribute of the comparison is
     *     Binary, then the operator checks for the absence of a subsequence of the target that matches the input.
     *     If the target attribute of the comparison is a set ("<code>SS</code>", "<code>NS</code>", or "<code>BS</code>
     *     "), then the operator evaluates to true if it <i>does not</i> find an exact match with any member of the
     *     set.NOT_CONTAINS is supported for lists: When evaluating "<code>a NOT CONTAINS b</code>", "<code>a</code>
     *     " can be a list; however, "<code>b</code>" cannot be a set, a map, or a list.
     *   </li>
     *   <li>
     *     <code>BEGINS_WITH</code> : Checks for a prefix. <code>AttributeValueList</code> can contain only one <code>
     *     AttributeValue</code> of type String or Binary (not a Number or a set type). The target attribute of the
     *     comparison must be of type String or Binary (not a Number or a set type).
     *   </li>
     *   <li>
     *     <code>IN</code> : Checks for matching elements in a list.<code>AttributeValueList</code> can contain one
     *     or more <code>AttributeValue</code> elements of type String, Number, or Binary. These attributes are
     *     compared against an existing attribute of an item. If any elements of the input are equal to the item
     *     attribute, the expression evaluates to true.
     *   </li>
     *   <li>
     *     <code>BETWEEN</code> : Greater than or equal to the first value, and less than or equal to the second
     *     value. <code>AttributeValueList</code> must contain two <code>AttributeValue</code> elements of the same
     *     type, either String, Number, or Binary (not a set type). A target attribute matches if the target value
     *     is greater than, or equal to, the first element and less than, or equal to, the second element. If an
     *     item contains an <code>AttributeValue</code> element of a different type than the one provided in the
     *     request, the value does not match. For example, <code>{"S":"6"}</code> does not compare to <code>
     *     {"N":"6"}</code>. Also, <code>{"N":"6"}</code> does not compare to <code>{"NS":["6", "2", "1"]}</code>
     *   </li>
     * </ul>
     */
    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    /**
     * One or more values to evaluate against the supplied attribute. The number of values in the list depends on the <code>
     * ComparisonOperator</code> being used.
     *
     * <p>For type Number, value comparisons are numeric.
     *
     * <p>String value comparisons for greater than, equals, or less than are based on ASCII character code values. For
     * example, <code>a</code> is greater than <code>A</code>, and <code>a</code> is greater than <code>B</code>. For a
     * list of code values, see <a href="http://en.wikipedia.org/wiki/ASCII#ASCII_printable_characters">http://en.wikipedia.org/wiki/ASCII#ASCII_printable_characters</a>.
     *
     * <p>For Binary, DynamoDB treats each byte of the binary data as unsigned when it compares binary values.
     *
     * <p>For information on specifying data types in JSON, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DataFormat.html">JSON Data Format</a> in the <i>Amazon DynamoDB
     * Developer Guide</i>.
     */
    public List<AttributeValue> getAttributeValueList() {
        if (attributeValueList == null) {
            return Collections.emptyList();
        }
        return attributeValueList;
    }

    public boolean hasAttributeValueList() {
        return attributeValueList != null;
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
        ExpectedAttributeValue that = (ExpectedAttributeValue) other;
        return Objects.equals(this.exists, that.exists)
               && Objects.equals(this.comparisonOperator, that.comparisonOperator)
               && Objects.equals(this.value, that.value)
               && Objects.equals(this.attributeValueList, that.attributeValueList);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(value);
        $hc = 31 * $hc + Objects.hashCode(exists);
        $hc = 31 * $hc + Objects.hashCode(comparisonOperator);
        $hc = 31 * $hc + Objects.hashCode(attributeValueList);
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
        if (exists != null) {
            serializer.writeBoolean($SCHEMA_EXISTS, exists);
        }
        if (comparisonOperator != null) {
            serializer.writeString($SCHEMA_COMPARISON_OPERATOR, comparisonOperator.getValue());
        }
        if (attributeValueList != null) {
            serializer.writeList($SCHEMA_ATTRIBUTE_VALUE_LIST, attributeValueList, attributeValueList.size(), SharedSerde.AttributeValueListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXISTS, member, exists);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, comparisonOperator);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_VALUE_LIST, member, attributeValueList);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ExpectedAttributeValue}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.value(this.value);
        builder.exists(this.exists);
        builder.comparisonOperator(this.comparisonOperator);
        builder.attributeValueList(this.attributeValueList);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ExpectedAttributeValue}.
     */
    public static final class Builder implements ShapeBuilder<ExpectedAttributeValue> {
        private AttributeValue value;
        private Boolean exists;
        private ComparisonOperator comparisonOperator;
        private List<AttributeValue> attributeValueList;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the data for the expected attribute.
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
         * Causes DynamoDB to evaluate the value before attempting a conditional operation:
         *
         * <ul>
         *   <li>
         *     If <code>Exists</code> is <code>true</code>, DynamoDB will check to see if that attribute value already
         *     exists in the table. If it is found, then the operation succeeds. If it is not found, the operation fails
         *     with a <code>ConditionCheckFailedException</code>.
         *   </li>
         *   <li>
         *     If <code>Exists</code> is <code>false</code>, DynamoDB assumes that the attribute value does not exist in
         *     the table. If in fact the value does not exist, then the assumption is valid and the operation succeeds.
         *     If the value is found, despite the assumption that it does not exist, the operation fails with a <code>
         *     ConditionCheckFailedException</code>.
         *   </li>
         * </ul>
         *
         * <p>The default setting for <code>Exists</code> is <code>true</code>. If you supply a <code>Value</code> all by
         * itself, DynamoDB assumes the attribute exists: You don't have to set <code>Exists</code> to <code>true</code>,
         * because it is implied.
         *
         * <p>DynamoDB returns a <code>ValidationException</code> if:
         *
         * <ul>
         *   <li>
         *     <code>Exists</code> is <code>true</code> but there is no <code>Value</code> to check. (You expect a value
         *     to exist, but don't specify what that value is.)
         *   </li>
         *   <li>
         *     <code>Exists</code> is <code>false</code> but you also provide a <code>Value</code>. (You cannot expect
         *     an attribute to have a value, while also expecting it not to exist.)
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder exists(Boolean exists) {
            this.exists = exists;
            return this;
        }

        /**
         * A comparator for evaluating attributes in the <code>AttributeValueList</code>. For example, equals, greater than,
         * less than, etc.
         *
         * <p>The following comparison operators are available:
         *
         * <p><code>EQ | NE | LE | LT | GE | GT | NOT_NULL | NULL | CONTAINS | NOT_CONTAINS | BEGINS_WITH | IN | BETWEEN</code>
         *
         * <p>The following are descriptions of each comparison operator.
         *
         * <ul>
         *   <li>
         *     <code>EQ</code> : Equal. <code>EQ</code> is supported for all data types, including lists and maps.<code>
         *     AttributeValueList</code> can contain only one <code>AttributeValue</code> element of type String,
         *     Number, Binary, String Set, Number Set, or Binary Set. If an item contains an <code>AttributeValue</code>
         *     element of a different type than the one provided in the request, the value does not match. For example, <code>
         *     {"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>{"N":"6"}</code> does not equal <code>
         *     {"NS":["6", "2", "1"]}</code>.
         *   </li>
         *   <li>
         *     <code>NE</code> : Not equal. <code>NE</code> is supported for all data types, including lists and maps.<code>
         *     AttributeValueList</code> can contain only one <code>AttributeValue</code> of type String, Number,
         *     Binary, String Set, Number Set, or Binary Set. If an item contains an <code>AttributeValue</code> of a
         *     different type than the one provided in the request, the value does not match. For example, <code>
         *     {"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>{"N":"6"}</code> does not equal <code>
         *     {"NS":["6", "2", "1"]}</code>.
         *   </li>
         *   <li>
         *     <code>LE</code> : Less than or equal. <code>AttributeValueList</code> can contain only one <code>
         *     AttributeValue</code> element of type String, Number, or Binary (not a set type). If an item contains an <code>
         *     AttributeValue</code> element of a different type than the one provided in the request, the value does
         *     not match. For example, <code>{"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>
         *     {"N":"6"}</code> does not compare to <code>{"NS":["6", "2", "1"]}</code>.
         *   </li>
         *   <li>
         *     <code>LT</code> : Less than. <code>AttributeValueList</code> can contain only one <code>AttributeValue</code>
         *     of type String, Number, or Binary (not a set type). If an item contains an <code>AttributeValue</code>
         *     element of a different type than the one provided in the request, the value does not match. For example, <code>
         *     {"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>{"N":"6"}</code> does not compare to <code>
         *     {"NS":["6", "2", "1"]}</code>.
         *   </li>
         *   <li>
         *     <code>GE</code> : Greater than or equal. <code>AttributeValueList</code> can contain only one <code>
         *     AttributeValue</code> element of type String, Number, or Binary (not a set type). If an item contains an <code>
         *     AttributeValue</code> element of a different type than the one provided in the request, the value does
         *     not match. For example, <code>{"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>
         *     {"N":"6"}</code> does not compare to <code>{"NS":["6", "2", "1"]}</code>.
         *   </li>
         *   <li>
         *     <code>GT</code> : Greater than. <code>AttributeValueList</code> can contain only one <code>AttributeValue</code>
         *     element of type String, Number, or Binary (not a set type). If an item contains an <code>AttributeValue</code>
         *     element of a different type than the one provided in the request, the value does not match. For example, <code>
         *     {"S":"6"}</code> does not equal <code>{"N":"6"}</code>. Also, <code>{"N":"6"}</code> does not compare to <code>
         *     {"NS":["6", "2", "1"]}</code>.
         *   </li>
         *   <li>
         *     <code>NOT_NULL</code> : The attribute exists. <code>NOT_NULL</code> is supported for all data types,
         *     including lists and maps.This operator tests for the existence of an attribute, not its data type. If the
         *     data type of attribute "<code>a</code>" is null, and you evaluate it using <code>NOT_NULL</code>, the
         *     result is a Boolean <code>true</code>. This result is because the attribute "<code>a</code>" exists; its
         *     data type is not relevant to the <code>NOT_NULL</code> comparison operator.
         *   </li>
         *   <li>
         *     <code>NULL</code> : The attribute does not exist. <code>NULL</code> is supported for all data types,
         *     including lists and maps.This operator tests for the nonexistence of an attribute, not its data type. If
         *     the data type of attribute "<code>a</code>" is null, and you evaluate it using <code>NULL</code>, the
         *     result is a Boolean <code>false</code>. This is because the attribute "<code>a</code>" exists; its data
         *     type is not relevant to the <code>NULL</code> comparison operator.
         *   </li>
         *   <li>
         *     <code>CONTAINS</code> : Checks for a subsequence, or value in a set.<code>AttributeValueList</code> can
         *     contain only one <code>AttributeValue</code> element of type String, Number, or Binary (not a set type).
         *     If the target attribute of the comparison is of type String, then the operator checks for a substring
         *     match. If the target attribute of the comparison is of type Binary, then the operator looks for a
         *     subsequence of the target that matches the input. If the target attribute of the comparison is a set ("<code>
         *     SS</code>", "<code>NS</code>", or "<code>BS</code>"), then the operator evaluates to true if it finds an
         *     exact match with any member of the set.CONTAINS is supported for lists: When evaluating "<code>a CONTAINS
         *     b</code>", "<code>a</code>" can be a list; however, "<code>b</code>" cannot be a set, a map, or a list.
         *   </li>
         *   <li>
         *     <code>NOT_CONTAINS</code> : Checks for absence of a subsequence, or absence of a value in a set.<code>
         *     AttributeValueList</code> can contain only one <code>AttributeValue</code> element of type String,
         *     Number, or Binary (not a set type). If the target attribute of the comparison is a String, then the
         *     operator checks for the absence of a substring match. If the target attribute of the comparison is
         *     Binary, then the operator checks for the absence of a subsequence of the target that matches the input.
         *     If the target attribute of the comparison is a set ("<code>SS</code>", "<code>NS</code>", or "<code>BS</code>
         *     "), then the operator evaluates to true if it <i>does not</i> find an exact match with any member of the
         *     set.NOT_CONTAINS is supported for lists: When evaluating "<code>a NOT CONTAINS b</code>", "<code>a</code>
         *     " can be a list; however, "<code>b</code>" cannot be a set, a map, or a list.
         *   </li>
         *   <li>
         *     <code>BEGINS_WITH</code> : Checks for a prefix. <code>AttributeValueList</code> can contain only one <code>
         *     AttributeValue</code> of type String or Binary (not a Number or a set type). The target attribute of the
         *     comparison must be of type String or Binary (not a Number or a set type).
         *   </li>
         *   <li>
         *     <code>IN</code> : Checks for matching elements in a list.<code>AttributeValueList</code> can contain one
         *     or more <code>AttributeValue</code> elements of type String, Number, or Binary. These attributes are
         *     compared against an existing attribute of an item. If any elements of the input are equal to the item
         *     attribute, the expression evaluates to true.
         *   </li>
         *   <li>
         *     <code>BETWEEN</code> : Greater than or equal to the first value, and less than or equal to the second
         *     value. <code>AttributeValueList</code> must contain two <code>AttributeValue</code> elements of the same
         *     type, either String, Number, or Binary (not a set type). A target attribute matches if the target value
         *     is greater than, or equal to, the first element and less than, or equal to, the second element. If an
         *     item contains an <code>AttributeValue</code> element of a different type than the one provided in the
         *     request, the value does not match. For example, <code>{"S":"6"}</code> does not compare to <code>
         *     {"N":"6"}</code>. Also, <code>{"N":"6"}</code> does not compare to <code>{"NS":["6", "2", "1"]}</code>
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder comparisonOperator(ComparisonOperator comparisonOperator) {
            this.comparisonOperator = comparisonOperator;
            return this;
        }

        /**
         * One or more values to evaluate against the supplied attribute. The number of values in the list depends on the <code>
         * ComparisonOperator</code> being used.
         *
         * <p>For type Number, value comparisons are numeric.
         *
         * <p>String value comparisons for greater than, equals, or less than are based on ASCII character code values. For
         * example, <code>a</code> is greater than <code>A</code>, and <code>a</code> is greater than <code>B</code>. For a
         * list of code values, see <a href="http://en.wikipedia.org/wiki/ASCII#ASCII_printable_characters">http://en.wikipedia.org/wiki/ASCII#ASCII_printable_characters</a>.
         *
         * <p>For Binary, DynamoDB treats each byte of the binary data as unsigned when it compares binary values.
         *
         * <p>For information on specifying data types in JSON, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DataFormat.html">JSON Data Format</a> in the <i>Amazon DynamoDB
         * Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder attributeValueList(List<AttributeValue> attributeValueList) {
            this.attributeValueList = attributeValueList;
            return this;
        }

        @Override
        public ExpectedAttributeValue build() {
            return new ExpectedAttributeValue(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> value((AttributeValue) SchemaUtils.validateSameMember($SCHEMA_VALUE, member, value));
                case 1 -> exists((Boolean) SchemaUtils.validateSameMember($SCHEMA_EXISTS, member, value));
                case 2 -> comparisonOperator((ComparisonOperator) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, value));
                case 3 -> attributeValueList((List<AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_VALUE_LIST, member, value));
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
                    case 1 -> builder.exists(de.readBoolean(member));
                    case 2 -> builder.comparisonOperator(ComparisonOperator.builder().deserializeMember(de, member).build());
                    case 3 -> builder.attributeValueList(SharedSerde.deserializeAttributeValueList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
