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

@SmithyGenerated
public final class ListImportsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_IMPORTS_INPUT;
    private static final Schema $SCHEMA_TABLE_ARN = $SCHEMA.member("TableArn");
    private static final Schema $SCHEMA_PAGE_SIZE = $SCHEMA.member("PageSize");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableArn;
    private final transient Integer pageSize;
    private final transient String nextToken;

    private ListImportsInput(Builder builder) {
        this.tableArn = builder.tableArn;
        this.pageSize = builder.pageSize;
        this.nextToken = builder.nextToken;
    }

    /**
     * The Amazon Resource Name (ARN) associated with the table that was imported to.
     */
    public String getTableArn() {
        return tableArn;
    }

    /**
     * The number of <code>ImportSummary </code>objects returned in a single page.
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * An optional string that, if supplied, must be copied from the output of a previous call to
     * <code>ListImports</code>. When provided in this manner, the API fetches the next page of results.
     */
    public String getNextToken() {
        return nextToken;
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
        ListImportsInput that = (ListImportsInput) other;
        return Objects.equals(this.pageSize, that.pageSize)
               && Objects.equals(this.tableArn, that.tableArn)
               && Objects.equals(this.nextToken, that.nextToken);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableArn);
        $hc = 31 * $hc + Objects.hashCode(pageSize);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (tableArn != null) {
            serializer.writeString($SCHEMA_TABLE_ARN, tableArn);
        }
        if (pageSize != null) {
            serializer.writeInteger($SCHEMA_PAGE_SIZE, pageSize);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, tableArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PAGE_SIZE, member, pageSize);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListImportsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableArn(this.tableArn);
        builder.pageSize(this.pageSize);
        builder.nextToken(this.nextToken);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListImportsInput}.
     */
    public static final class Builder implements ShapeBuilder<ListImportsInput> {
        private String tableArn;
        private Integer pageSize;
        private String nextToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon Resource Name (ARN) associated with the table that was imported to.
         *
         * @return this builder.
         */
        public Builder tableArn(String tableArn) {
            this.tableArn = tableArn;
            return this;
        }

        /**
         * The number of <code>ImportSummary </code>objects returned in a single page.
         *
         * @return this builder.
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * An optional string that, if supplied, must be copied from the output of a previous call to
         * <code>ListImports</code>. When provided in this manner, the API fetches the next page of results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        @Override
        public ListImportsInput build() {
            return new ListImportsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableArn((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_ARN, member, value));
                case 1 -> pageSize((Integer) SchemaUtils.validateSameMember($SCHEMA_PAGE_SIZE, member, value));
                case 2 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
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
                    case 0 -> builder.tableArn(de.readString(member));
                    case 1 -> builder.pageSize(de.readInteger(member));
                    case 2 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
