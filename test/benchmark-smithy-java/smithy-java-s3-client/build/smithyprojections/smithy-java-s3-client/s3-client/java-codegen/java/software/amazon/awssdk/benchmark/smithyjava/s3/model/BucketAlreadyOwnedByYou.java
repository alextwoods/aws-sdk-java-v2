package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import software.amazon.smithy.java.core.error.ErrorFault;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * The bucket you tried to create already exists, and you own it. Amazon S3 returns this error in all Amazon Web
 * Services Regions except in the North Virginia Region. For legacy compatibility, if you re-create an existing bucket
 * that you already own in the North Virginia Region, Amazon S3 returns 200 OK and resets the bucket access control
 * lists (ACLs).
 */
@SmithyGenerated
public final class BucketAlreadyOwnedByYou extends S3Exception {

    public static final Schema $SCHEMA = Schemas.BUCKET_ALREADY_OWNED_BY_YOU;

    public static final ShapeId $ID = $SCHEMA.id();

    private BucketAlreadyOwnedByYou(Builder builder) {
        super($SCHEMA, null, builder.$cause, ErrorFault.CLIENT, builder.$captureStackTrace, builder.$deserialized);
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {

    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link BucketAlreadyOwnedByYou}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BucketAlreadyOwnedByYou}.
     */
    public static final class Builder implements ShapeBuilder<BucketAlreadyOwnedByYou> {
        private Throwable $cause;
        private Boolean $captureStackTrace;
        private boolean $deserialized;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        public Builder withStackTrace() {
            this.$captureStackTrace = true;
            return this;
        }

        public Builder withoutStackTrace() {
            this.$captureStackTrace = false;
            return this;
        }

        public Builder withCause(Throwable cause) {
            this.$cause = cause;
            return this;
        }

        @Override
        public BucketAlreadyOwnedByYou build() {
            return new BucketAlreadyOwnedByYou(this);
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            this.$deserialized = true;
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
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {}
        }
    }
}
