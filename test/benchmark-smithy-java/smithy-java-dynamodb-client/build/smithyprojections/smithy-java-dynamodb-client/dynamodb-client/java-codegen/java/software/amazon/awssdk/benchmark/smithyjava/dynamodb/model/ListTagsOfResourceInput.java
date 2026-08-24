package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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

@SmithyGenerated
public final class ListTagsOfResourceInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_TAGS_OF_RESOURCE_INPUT;
    private static final Schema $SCHEMA_RESOURCE_ARN = $SCHEMA.member("ResourceArn");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String resourceArn;
    private final transient String nextToken;

    private ListTagsOfResourceInput(Builder builder) {
        this.resourceArn = builder.resourceArn;
        this.nextToken = builder.nextToken;
    }

    /**
     * The Amazon DynamoDB resource with tags to be listed. This value is an Amazon Resource Name (ARN).
     */
    public String getResourceArn() {
        return resourceArn;
    }

    /**
     * An optional string that, if supplied, must be copied from the output of a previous call to ListTagOfResource.
     * When provided in this manner, this API fetches the next page of results.
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
        ListTagsOfResourceInput that = (ListTagsOfResourceInput) other;
        return Objects.equals(this.resourceArn, that.resourceArn)
               && Objects.equals(this.nextToken, that.nextToken);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(resourceArn);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_RESOURCE_ARN, resourceArn);
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, resourceArn);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListTagsOfResourceInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.resourceArn(this.resourceArn);
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
     * Builder for {@link ListTagsOfResourceInput}.
     */
    public static final class Builder implements ShapeBuilder<ListTagsOfResourceInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String resourceArn;
        private String nextToken;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Amazon DynamoDB resource with tags to be listed. This value is an Amazon Resource Name (ARN).
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder resourceArn(String resourceArn) {
            this.resourceArn = Objects.requireNonNull(resourceArn, "resourceArn cannot be null");
            tracker.setMember($SCHEMA_RESOURCE_ARN);
            return this;
        }

        /**
         * An optional string that, if supplied, must be copied from the output of a previous call to ListTagOfResource.
         * When provided in this manner, this API fetches the next page of results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        @Override
        public ListTagsOfResourceInput build() {
            tracker.validate();
            return new ListTagsOfResourceInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> resourceArn((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCE_ARN, member, value));
                case 1 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ListTagsOfResourceInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RESOURCE_ARN)) {
                resourceArn("");
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
                    case 0 -> builder.resourceArn(de.readString(member));
                    case 1 -> builder.nextToken(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
