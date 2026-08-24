package software.amazon.awssdk.benchmark.smithyjava.s3.model;

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
 * If an S3 Metadata V1 <code>CreateBucketMetadataTableConfiguration</code> or V2 <code>
 * CreateBucketMetadataConfiguration</code> request succeeds, but S3 Metadata was unable to create the table, this
 * structure contains the error code and error message.
 *
 * <p>If you created your S3 Metadata configuration before July 15, 2025, we recommend that you delete and re-create
 * your configuration by using <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucketMetadataConfiguration.html">CreateBucketMetadataConfiguration</a> so that you can expire journal table records and
 * create a live inventory table.
 */
@SmithyGenerated
public final class ErrorDetails implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas2.ERROR_DETAILS;
    private static final Schema $SCHEMA_ERROR_CODE = $SCHEMA.member("ErrorCode");
    private static final Schema $SCHEMA_ERROR_MESSAGE = $SCHEMA.member("ErrorMessage");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String errorCode;
    private final transient String errorMessage;

    private ErrorDetails(Builder builder) {
        this.errorCode = builder.errorCode;
        this.errorMessage = builder.errorMessage;
    }

    /**
     * If the V1 <code>CreateBucketMetadataTableConfiguration</code> request succeeds, but S3 Metadata was unable to
     * create the table, this structure contains the error code. The possible error codes and error messages are as
     * follows:
     *
     * <ul>
     *   <li>
     *     <code>AccessDeniedCreatingResources</code> - You don't have sufficient permissions to create the required
     *     resources. Make sure that you have <code>s3tables:CreateNamespace</code>, <code>s3tables:CreateTable</code>
     *     , <code>s3tables:GetTable</code> and <code>s3tables:PutTablePolicy</code> permissions, and then try
     *     again. To create a new metadata table, you must delete the metadata configuration for this bucket, and
     *     then create a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>AccessDeniedWritingToTable</code> - Unable to write to the metadata table because of missing
     *     resource permissions. To fix the resource policy, Amazon S3 needs to create a new metadata table. To
     *     create a new metadata table, you must delete the metadata configuration for this bucket, and then create
     *     a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>DestinationTableNotFound</code> - The destination table doesn't exist. To create a new metadata
     *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
     *     configuration.
     *   </li>
     *   <li>
     *     <code>ServerInternalError</code> - An internal error has occurred. To create a new metadata table, you
     *     must delete the metadata configuration for this bucket, and then create a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>TableAlreadyExists</code> - The table that you specified already exists in the table bucket's
     *     namespace. Specify a different table name. To create a new metadata table, you must delete the metadata
     *     configuration for this bucket, and then create a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>TableBucketNotFound</code> - The table bucket that you specified doesn't exist in this Amazon Web
     *     Services Region and account. Create or choose a different table bucket. To create a new metadata table,
     *     you must delete the metadata configuration for this bucket, and then create a new metadata configuration.
     *   </li>
     * </ul>
     *
     * <p> If the V2 <code>CreateBucketMetadataConfiguration</code> request succeeds, but S3 Metadata was unable to
     * create the table, this structure contains the error code. The possible error codes and error messages are as
     * follows:
     *
     * <ul>
     *   <li>
     *     <code>AccessDeniedCreatingResources</code> - You don't have sufficient permissions to create the required
     *     resources. Make sure that you have <code>s3tables:CreateTableBucket</code>, <code>
     *     s3tables:CreateNamespace</code>, <code>s3tables:CreateTable</code>, <code>s3tables:GetTable</code>, <code>
     *     s3tables:PutTablePolicy</code>, <code>kms:DescribeKey</code>, and <code>s3tables:PutTableEncryption</code>
     *     permissions. Additionally, ensure that the KMS key used to encrypt the table still exists, is active and
     *     has a resource policy granting access to the S3 service principals '<code>
     *     maintenance.s3tables.amazonaws.com</code>' and '<code>metadata.s3.amazonaws.com</code>'. To create a new
     *     metadata table, you must delete the metadata configuration for this bucket, and then create a new
     *     metadata configuration.
     *   </li>
     *   <li>
     *     <code>AccessDeniedWritingToTable</code> - Unable to write to the metadata table because of missing
     *     resource permissions. To fix the resource policy, Amazon S3 needs to create a new metadata table. To
     *     create a new metadata table, you must delete the metadata configuration for this bucket, and then create
     *     a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>DestinationTableNotFound</code> - The destination table doesn't exist. To create a new metadata
     *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
     *     configuration.
     *   </li>
     *   <li>
     *     <code>ServerInternalError</code> - An internal error has occurred. To create a new metadata table, you
     *     must delete the metadata configuration for this bucket, and then create a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>JournalTableAlreadyExists</code> - A journal table already exists in the Amazon Web Services
     *     managed table bucket's namespace. Delete the journal table, and then try again. To create a new metadata
     *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
     *     configuration.
     *   </li>
     *   <li>
     *     <code>InventoryTableAlreadyExists</code> - An inventory table already exists in the Amazon Web Services
     *     managed table bucket's namespace. Delete the inventory table, and then try again. To create a new
     *     metadata table, you must delete the metadata configuration for this bucket, and then create a new
     *     metadata configuration.
     *   </li>
     *   <li>
     *     <code>JournalTableNotAvailable</code> - The journal table that the inventory table relies on has a <code>
     *     FAILED</code> status. An inventory table requires a journal table with an <code>ACTIVE</code> status. To
     *     create a new journal or inventory table, you must delete the metadata configuration for this bucket,
     *     along with any journal or inventory tables, and then create a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>NoSuchBucket</code> - The specified general purpose bucket does not exist.
     *   </li>
     * </ul>
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * If the V1 <code>CreateBucketMetadataTableConfiguration</code> request succeeds, but S3 Metadata was unable to
     * create the table, this structure contains the error message. The possible error codes and error messages are as
     * follows:
     *
     * <ul>
     *   <li>
     *     <code>AccessDeniedCreatingResources</code> - You don't have sufficient permissions to create the required
     *     resources. Make sure that you have <code>s3tables:CreateNamespace</code>, <code>s3tables:CreateTable</code>
     *     , <code>s3tables:GetTable</code> and <code>s3tables:PutTablePolicy</code> permissions, and then try
     *     again. To create a new metadata table, you must delete the metadata configuration for this bucket, and
     *     then create a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>AccessDeniedWritingToTable</code> - Unable to write to the metadata table because of missing
     *     resource permissions. To fix the resource policy, Amazon S3 needs to create a new metadata table. To
     *     create a new metadata table, you must delete the metadata configuration for this bucket, and then create
     *     a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>DestinationTableNotFound</code> - The destination table doesn't exist. To create a new metadata
     *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
     *     configuration.
     *   </li>
     *   <li>
     *     <code>ServerInternalError</code> - An internal error has occurred. To create a new metadata table, you
     *     must delete the metadata configuration for this bucket, and then create a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>TableAlreadyExists</code> - The table that you specified already exists in the table bucket's
     *     namespace. Specify a different table name. To create a new metadata table, you must delete the metadata
     *     configuration for this bucket, and then create a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>TableBucketNotFound</code> - The table bucket that you specified doesn't exist in this Amazon Web
     *     Services Region and account. Create or choose a different table bucket. To create a new metadata table,
     *     you must delete the metadata configuration for this bucket, and then create a new metadata configuration.
     *   </li>
     * </ul>
     *
     * <p> If the V2 <code>CreateBucketMetadataConfiguration</code> request succeeds, but S3 Metadata was unable to
     * create the table, this structure contains the error code. The possible error codes and error messages are as
     * follows:
     *
     * <ul>
     *   <li>
     *     <code>AccessDeniedCreatingResources</code> - You don't have sufficient permissions to create the required
     *     resources. Make sure that you have <code>s3tables:CreateTableBucket</code>, <code>
     *     s3tables:CreateNamespace</code>, <code>s3tables:CreateTable</code>, <code>s3tables:GetTable</code>, <code>
     *     s3tables:PutTablePolicy</code>, <code>kms:DescribeKey</code>, and <code>s3tables:PutTableEncryption</code>
     *     permissions. Additionally, ensure that the KMS key used to encrypt the table still exists, is active and
     *     has a resource policy granting access to the S3 service principals '<code>
     *     maintenance.s3tables.amazonaws.com</code>' and '<code>metadata.s3.amazonaws.com</code>'. To create a new
     *     metadata table, you must delete the metadata configuration for this bucket, and then create a new
     *     metadata configuration.
     *   </li>
     *   <li>
     *     <code>AccessDeniedWritingToTable</code> - Unable to write to the metadata table because of missing
     *     resource permissions. To fix the resource policy, Amazon S3 needs to create a new metadata table. To
     *     create a new metadata table, you must delete the metadata configuration for this bucket, and then create
     *     a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>DestinationTableNotFound</code> - The destination table doesn't exist. To create a new metadata
     *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
     *     configuration.
     *   </li>
     *   <li>
     *     <code>ServerInternalError</code> - An internal error has occurred. To create a new metadata table, you
     *     must delete the metadata configuration for this bucket, and then create a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>JournalTableAlreadyExists</code> - A journal table already exists in the Amazon Web Services
     *     managed table bucket's namespace. Delete the journal table, and then try again. To create a new metadata
     *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
     *     configuration.
     *   </li>
     *   <li>
     *     <code>InventoryTableAlreadyExists</code> - An inventory table already exists in the Amazon Web Services
     *     managed table bucket's namespace. Delete the inventory table, and then try again. To create a new
     *     metadata table, you must delete the metadata configuration for this bucket, and then create a new
     *     metadata configuration.
     *   </li>
     *   <li>
     *     <code>JournalTableNotAvailable</code> - The journal table that the inventory table relies on has a <code>
     *     FAILED</code> status. An inventory table requires a journal table with an <code>ACTIVE</code> status. To
     *     create a new journal or inventory table, you must delete the metadata configuration for this bucket,
     *     along with any journal or inventory tables, and then create a new metadata configuration.
     *   </li>
     *   <li>
     *     <code>NoSuchBucket</code> - The specified general purpose bucket does not exist.
     *   </li>
     * </ul>
     */
    public String getErrorMessage() {
        return errorMessage;
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
        ErrorDetails that = (ErrorDetails) other;
        return Objects.equals(this.errorCode, that.errorCode)
               && Objects.equals(this.errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(errorCode);
        $hc = 31 * $hc + Objects.hashCode(errorMessage);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (errorCode != null) {
            serializer.writeString($SCHEMA_ERROR_CODE, errorCode);
        }
        if (errorMessage != null) {
            serializer.writeString($SCHEMA_ERROR_MESSAGE, errorMessage);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ERROR_CODE, member, errorCode);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ERROR_MESSAGE, member, errorMessage);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ErrorDetails}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.errorCode(this.errorCode);
        builder.errorMessage(this.errorMessage);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ErrorDetails}.
     */
    public static final class Builder implements ShapeBuilder<ErrorDetails> {
        private String errorCode;
        private String errorMessage;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * If the V1 <code>CreateBucketMetadataTableConfiguration</code> request succeeds, but S3 Metadata was unable to
         * create the table, this structure contains the error code. The possible error codes and error messages are as
         * follows:
         *
         * <ul>
         *   <li>
         *     <code>AccessDeniedCreatingResources</code> - You don't have sufficient permissions to create the required
         *     resources. Make sure that you have <code>s3tables:CreateNamespace</code>, <code>s3tables:CreateTable</code>
         *     , <code>s3tables:GetTable</code> and <code>s3tables:PutTablePolicy</code> permissions, and then try
         *     again. To create a new metadata table, you must delete the metadata configuration for this bucket, and
         *     then create a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>AccessDeniedWritingToTable</code> - Unable to write to the metadata table because of missing
         *     resource permissions. To fix the resource policy, Amazon S3 needs to create a new metadata table. To
         *     create a new metadata table, you must delete the metadata configuration for this bucket, and then create
         *     a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>DestinationTableNotFound</code> - The destination table doesn't exist. To create a new metadata
         *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
         *     configuration.
         *   </li>
         *   <li>
         *     <code>ServerInternalError</code> - An internal error has occurred. To create a new metadata table, you
         *     must delete the metadata configuration for this bucket, and then create a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>TableAlreadyExists</code> - The table that you specified already exists in the table bucket's
         *     namespace. Specify a different table name. To create a new metadata table, you must delete the metadata
         *     configuration for this bucket, and then create a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>TableBucketNotFound</code> - The table bucket that you specified doesn't exist in this Amazon Web
         *     Services Region and account. Create or choose a different table bucket. To create a new metadata table,
         *     you must delete the metadata configuration for this bucket, and then create a new metadata configuration.
         *   </li>
         * </ul>
         *
         * <p> If the V2 <code>CreateBucketMetadataConfiguration</code> request succeeds, but S3 Metadata was unable to
         * create the table, this structure contains the error code. The possible error codes and error messages are as
         * follows:
         *
         * <ul>
         *   <li>
         *     <code>AccessDeniedCreatingResources</code> - You don't have sufficient permissions to create the required
         *     resources. Make sure that you have <code>s3tables:CreateTableBucket</code>, <code>
         *     s3tables:CreateNamespace</code>, <code>s3tables:CreateTable</code>, <code>s3tables:GetTable</code>, <code>
         *     s3tables:PutTablePolicy</code>, <code>kms:DescribeKey</code>, and <code>s3tables:PutTableEncryption</code>
         *     permissions. Additionally, ensure that the KMS key used to encrypt the table still exists, is active and
         *     has a resource policy granting access to the S3 service principals '<code>
         *     maintenance.s3tables.amazonaws.com</code>' and '<code>metadata.s3.amazonaws.com</code>'. To create a new
         *     metadata table, you must delete the metadata configuration for this bucket, and then create a new
         *     metadata configuration.
         *   </li>
         *   <li>
         *     <code>AccessDeniedWritingToTable</code> - Unable to write to the metadata table because of missing
         *     resource permissions. To fix the resource policy, Amazon S3 needs to create a new metadata table. To
         *     create a new metadata table, you must delete the metadata configuration for this bucket, and then create
         *     a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>DestinationTableNotFound</code> - The destination table doesn't exist. To create a new metadata
         *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
         *     configuration.
         *   </li>
         *   <li>
         *     <code>ServerInternalError</code> - An internal error has occurred. To create a new metadata table, you
         *     must delete the metadata configuration for this bucket, and then create a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>JournalTableAlreadyExists</code> - A journal table already exists in the Amazon Web Services
         *     managed table bucket's namespace. Delete the journal table, and then try again. To create a new metadata
         *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
         *     configuration.
         *   </li>
         *   <li>
         *     <code>InventoryTableAlreadyExists</code> - An inventory table already exists in the Amazon Web Services
         *     managed table bucket's namespace. Delete the inventory table, and then try again. To create a new
         *     metadata table, you must delete the metadata configuration for this bucket, and then create a new
         *     metadata configuration.
         *   </li>
         *   <li>
         *     <code>JournalTableNotAvailable</code> - The journal table that the inventory table relies on has a <code>
         *     FAILED</code> status. An inventory table requires a journal table with an <code>ACTIVE</code> status. To
         *     create a new journal or inventory table, you must delete the metadata configuration for this bucket,
         *     along with any journal or inventory tables, and then create a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>NoSuchBucket</code> - The specified general purpose bucket does not exist.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * If the V1 <code>CreateBucketMetadataTableConfiguration</code> request succeeds, but S3 Metadata was unable to
         * create the table, this structure contains the error message. The possible error codes and error messages are as
         * follows:
         *
         * <ul>
         *   <li>
         *     <code>AccessDeniedCreatingResources</code> - You don't have sufficient permissions to create the required
         *     resources. Make sure that you have <code>s3tables:CreateNamespace</code>, <code>s3tables:CreateTable</code>
         *     , <code>s3tables:GetTable</code> and <code>s3tables:PutTablePolicy</code> permissions, and then try
         *     again. To create a new metadata table, you must delete the metadata configuration for this bucket, and
         *     then create a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>AccessDeniedWritingToTable</code> - Unable to write to the metadata table because of missing
         *     resource permissions. To fix the resource policy, Amazon S3 needs to create a new metadata table. To
         *     create a new metadata table, you must delete the metadata configuration for this bucket, and then create
         *     a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>DestinationTableNotFound</code> - The destination table doesn't exist. To create a new metadata
         *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
         *     configuration.
         *   </li>
         *   <li>
         *     <code>ServerInternalError</code> - An internal error has occurred. To create a new metadata table, you
         *     must delete the metadata configuration for this bucket, and then create a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>TableAlreadyExists</code> - The table that you specified already exists in the table bucket's
         *     namespace. Specify a different table name. To create a new metadata table, you must delete the metadata
         *     configuration for this bucket, and then create a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>TableBucketNotFound</code> - The table bucket that you specified doesn't exist in this Amazon Web
         *     Services Region and account. Create or choose a different table bucket. To create a new metadata table,
         *     you must delete the metadata configuration for this bucket, and then create a new metadata configuration.
         *   </li>
         * </ul>
         *
         * <p> If the V2 <code>CreateBucketMetadataConfiguration</code> request succeeds, but S3 Metadata was unable to
         * create the table, this structure contains the error code. The possible error codes and error messages are as
         * follows:
         *
         * <ul>
         *   <li>
         *     <code>AccessDeniedCreatingResources</code> - You don't have sufficient permissions to create the required
         *     resources. Make sure that you have <code>s3tables:CreateTableBucket</code>, <code>
         *     s3tables:CreateNamespace</code>, <code>s3tables:CreateTable</code>, <code>s3tables:GetTable</code>, <code>
         *     s3tables:PutTablePolicy</code>, <code>kms:DescribeKey</code>, and <code>s3tables:PutTableEncryption</code>
         *     permissions. Additionally, ensure that the KMS key used to encrypt the table still exists, is active and
         *     has a resource policy granting access to the S3 service principals '<code>
         *     maintenance.s3tables.amazonaws.com</code>' and '<code>metadata.s3.amazonaws.com</code>'. To create a new
         *     metadata table, you must delete the metadata configuration for this bucket, and then create a new
         *     metadata configuration.
         *   </li>
         *   <li>
         *     <code>AccessDeniedWritingToTable</code> - Unable to write to the metadata table because of missing
         *     resource permissions. To fix the resource policy, Amazon S3 needs to create a new metadata table. To
         *     create a new metadata table, you must delete the metadata configuration for this bucket, and then create
         *     a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>DestinationTableNotFound</code> - The destination table doesn't exist. To create a new metadata
         *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
         *     configuration.
         *   </li>
         *   <li>
         *     <code>ServerInternalError</code> - An internal error has occurred. To create a new metadata table, you
         *     must delete the metadata configuration for this bucket, and then create a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>JournalTableAlreadyExists</code> - A journal table already exists in the Amazon Web Services
         *     managed table bucket's namespace. Delete the journal table, and then try again. To create a new metadata
         *     table, you must delete the metadata configuration for this bucket, and then create a new metadata
         *     configuration.
         *   </li>
         *   <li>
         *     <code>InventoryTableAlreadyExists</code> - An inventory table already exists in the Amazon Web Services
         *     managed table bucket's namespace. Delete the inventory table, and then try again. To create a new
         *     metadata table, you must delete the metadata configuration for this bucket, and then create a new
         *     metadata configuration.
         *   </li>
         *   <li>
         *     <code>JournalTableNotAvailable</code> - The journal table that the inventory table relies on has a <code>
         *     FAILED</code> status. An inventory table requires a journal table with an <code>ACTIVE</code> status. To
         *     create a new journal or inventory table, you must delete the metadata configuration for this bucket,
         *     along with any journal or inventory tables, and then create a new metadata configuration.
         *   </li>
         *   <li>
         *     <code>NoSuchBucket</code> - The specified general purpose bucket does not exist.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        @Override
        public ErrorDetails build() {
            return new ErrorDetails(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> errorCode((String) SchemaUtils.validateSameMember($SCHEMA_ERROR_CODE, member, value));
                case 1 -> errorMessage((String) SchemaUtils.validateSameMember($SCHEMA_ERROR_MESSAGE, member, value));
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
                    case 0 -> builder.errorCode(de.readString(member));
                    case 1 -> builder.errorMessage(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
