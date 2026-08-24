package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents the selection criteria for a <code>Query</code> or <code>Scan</code> operation:
 *
 * <ul>
 *   <li>
 *     For a <code>Query</code> operation, <code>Condition</code> is used for specifying the <code>KeyConditions</code>
 *     to use when querying a table or an index. For <code>KeyConditions</code>, only the following comparison
 *     operators are supported:<code>EQ | LE | LT | GE | GT | BEGINS_WITH | BETWEEN</code><code>Condition</code> is
 *     also used in a <code>QueryFilter</code>, which evaluates the query results and returns only the desired
 *     values.
 *   </li>
 *   <li>
 *     For a <code>Scan</code> operation, <code>Condition</code> is used in a <code>ScanFilter</code>, which
 *     evaluates the scan results and returns only the desired values.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class Condition implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.CONDITION;
    private static final Schema $SCHEMA_ATTRIBUTE_VALUE_LIST = $SCHEMA.member("AttributeValueList");
    private static final Schema $SCHEMA_COMPARISON_OPERATOR = $SCHEMA.member("ComparisonOperator");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<AttributeValue> attributeValueList;
    private final transient ComparisonOperator comparisonOperator;

    private Condition(Builder builder) {
        this.attributeValueList = builder.attributeValueList == null ? null : Collections.unmodifiableList(builder.attributeValueList);
        this.comparisonOperator = builder.comparisonOperator;
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

    /**
     * A comparator for evaluating attributes. For example, equals, greater than, less than, etc.
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
     * <p>For usage examples of <code>AttributeValueList</code> and <code>ComparisonOperator</code>, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.html">Legacy
     * Conditional Parameters</a> in the <i>Amazon DynamoDB Developer Guide</i>.
     */
    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
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
        Condition that = (Condition) other;
        return Objects.equals(this.comparisonOperator, that.comparisonOperator)
               && Objects.equals(this.attributeValueList, that.attributeValueList);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(attributeValueList);
        $hc = 31 * $hc + Objects.hashCode(comparisonOperator);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (attributeValueList != null) {
            serializer.writeList($SCHEMA_ATTRIBUTE_VALUE_LIST, attributeValueList, attributeValueList.size(), SharedSerde.AttributeValueListSerializer.INSTANCE);
        }
        serializer.writeString($SCHEMA_COMPARISON_OPERATOR, comparisonOperator.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, comparisonOperator);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_VALUE_LIST, member, attributeValueList);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Condition}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.attributeValueList(this.attributeValueList);
        builder.comparisonOperator(this.comparisonOperator);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Condition}.
     */
    public static final class Builder implements ShapeBuilder<Condition> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<AttributeValue> attributeValueList;
        private ComparisonOperator comparisonOperator;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
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
         * @return this builder.
         */
        public Builder attributeValueList(List<AttributeValue> attributeValueList) {
            this.attributeValueList = attributeValueList;
            return this;
        }

        /**
         * A comparator for evaluating attributes. For example, equals, greater than, less than, etc.
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
         * <p>For usage examples of <code>AttributeValueList</code> and <code>ComparisonOperator</code>, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.html">Legacy
         * Conditional Parameters</a> in the <i>Amazon DynamoDB Developer Guide</i>.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder comparisonOperator(ComparisonOperator comparisonOperator) {
            this.comparisonOperator = Objects.requireNonNull(comparisonOperator, "comparisonOperator cannot be null");
            tracker.setMember($SCHEMA_COMPARISON_OPERATOR);
            return this;
        }

        @Override
        public Condition build() {
            tracker.validate();
            return new Condition(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> comparisonOperator((ComparisonOperator) SchemaUtils.validateSameMember($SCHEMA_COMPARISON_OPERATOR, member, value));
                case 1 -> attributeValueList((List<AttributeValue>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_VALUE_LIST, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<Condition> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_COMPARISON_OPERATOR)) {
                comparisonOperator(ComparisonOperator.unknown(""));
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
                    case 0 -> builder.comparisonOperator(ComparisonOperator.builder().deserializeMember(de, member).build());
                    case 1 -> builder.attributeValueList(SharedSerde.deserializeAttributeValueList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
