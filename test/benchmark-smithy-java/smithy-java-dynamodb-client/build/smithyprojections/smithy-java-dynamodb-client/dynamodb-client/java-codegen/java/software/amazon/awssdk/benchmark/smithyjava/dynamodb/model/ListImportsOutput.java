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

@SmithyGenerated
public final class ListImportsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_IMPORTS_OUTPUT;
    private static final Schema $SCHEMA_IMPORT_SUMMARY_LIST = $SCHEMA.member("ImportSummaryList");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<ImportSummary> importSummaryList;
    private final transient String nextToken;

    private ListImportsOutput(Builder builder) {
        this.importSummaryList = builder.importSummaryList == null ? null : Collections.unmodifiableList(builder.importSummaryList);
        this.nextToken = builder.nextToken;
    }

    /**
     * A list of <code>ImportSummary</code> objects.
     */
    public List<ImportSummary> getImportSummaryList() {
        if (importSummaryList == null) {
            return Collections.emptyList();
        }
        return importSummaryList;
    }

    public boolean hasImportSummaryList() {
        return importSummaryList != null;
    }

    /**
     * If this value is returned, there are additional results to be displayed. To retrieve them, call
     * <code>ListImports</code> again, with <code>NextToken</code> set to this value.
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
        ListImportsOutput that = (ListImportsOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.importSummaryList, that.importSummaryList);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(importSummaryList);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (importSummaryList != null) {
            serializer.writeList($SCHEMA_IMPORT_SUMMARY_LIST, importSummaryList, importSummaryList.size(), SharedSerde.ImportSummaryListSerializer.INSTANCE);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_IMPORT_SUMMARY_LIST, member, importSummaryList);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListImportsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.importSummaryList(this.importSummaryList);
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
     * Builder for {@link ListImportsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListImportsOutput> {
        private List<ImportSummary> importSummaryList;
        private String nextToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A list of <code>ImportSummary</code> objects.
         *
         * @return this builder.
         */
        public Builder importSummaryList(List<ImportSummary> importSummaryList) {
            this.importSummaryList = importSummaryList;
            return this;
        }

        /**
         * If this value is returned, there are additional results to be displayed. To retrieve them, call
         * <code>ListImports</code> again, with <code>NextToken</code> set to this value.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        @Override
        public ListImportsOutput build() {
            return new ListImportsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> importSummaryList((List<ImportSummary>) SchemaUtils.validateSameMember($SCHEMA_IMPORT_SUMMARY_LIST, member, value));
                case 1 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
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
                    case 0 -> builder.importSummaryList(SharedSerde.deserializeImportSummaryList(member, de));
                    case 1 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
