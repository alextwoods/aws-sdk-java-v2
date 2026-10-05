/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.eventnotifications.s3.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSchemas;
import software.amazon.awssdk.eventnotifications.s3.internal.SerdeSupport;
import software.amazon.awssdk.utils.ToString;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The bucket involved in the event.
 */
public class S3Bucket implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.S3_BUCKET.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.S3_BUCKET;
    private static final Schema SCHEMA_NAME = SCHEMA.member("name");
    private static final Schema SCHEMA_OWNER_IDENTITY = SCHEMA.member("ownerIdentity");
    private static final Schema SCHEMA_ARN = SCHEMA.member("arn");
    private static final Schema SCHEMA_AWS_GENERATED_TAGS = SCHEMA.member("awsGeneratedTags");

    private final String name;
    private final UserIdentity ownerIdentity;
    private final String arn;
    private final Map<String, String> awsGeneratedTags;

    public S3Bucket(String name, UserIdentity ownerIdentity, String arn) {
        this(name, ownerIdentity, arn, null);
    }

    /**
     * @param awsGeneratedTags the AWS-generated tags of the bucket; copied, in iteration order, so later changes to the
     *        argument are not visible. {@code null} values are kept.
     */
    public S3Bucket(String name, UserIdentity ownerIdentity, String arn, Map<String, String> awsGeneratedTags) {
        this.name = name;
        this.ownerIdentity = ownerIdentity;
        this.arn = arn;
        this.awsGeneratedTags = awsGeneratedTags == null
                                ? null
                                : Collections.unmodifiableMap(new LinkedHashMap<>(awsGeneratedTags));
    }

    /**
     * @return the bucket name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the identity of the bucket owner.
     */
    public UserIdentity getOwnerIdentity() {
        return ownerIdentity;
    }

    /**
     * @return the bucket ARN.
     */
    public String getArn() {
        return arn;
    }

    /**
     * The AWS-generated tags of the bucket (for example {@code aws:resource:owner}), when Amazon S3 includes them.
     *
     * @return an unmodifiable map of the tags in message order, or {@code null} if the event has none.
     */
    public Map<String, String> getAwsGeneratedTags() {
        return awsGeneratedTags;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (name != null) {
            serializer.writeString(SCHEMA_NAME, name);
        }
        if (ownerIdentity != null) {
            serializer.writeStruct(SCHEMA_OWNER_IDENTITY, ownerIdentity);
        }
        if (arn != null) {
            serializer.writeString(SCHEMA_ARN, arn);
        }
        if (awsGeneratedTags != null) {
            SerdeSupport.writeStringMap(serializer, SCHEMA_AWS_GENERATED_TAGS, awsGeneratedTags);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_NAME, member, name);
            case 1 -> (T) SchemaUtils.validateSameMember(SCHEMA_OWNER_IDENTITY, member, ownerIdentity);
            case 2 -> (T) SchemaUtils.validateSameMember(SCHEMA_ARN, member, arn);
            case 3 -> (T) SchemaUtils.validateSameMember(SCHEMA_AWS_GENERATED_TAGS, member, awsGeneratedTags);
            default -> throw SerdeSupport.unknownMember(SCHEMA, member);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3Bucket s3Bucket = (S3Bucket) o;

        if (!Objects.equals(name, s3Bucket.name)) {
            return false;
        }
        if (!Objects.equals(ownerIdentity, s3Bucket.ownerIdentity)) {
            return false;
        }
        if (!Objects.equals(arn, s3Bucket.arn)) {
            return false;
        }
        return Objects.equals(awsGeneratedTags, s3Bucket.awsGeneratedTags);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (ownerIdentity != null ? ownerIdentity.hashCode() : 0);
        result = 31 * result + (arn != null ? arn.hashCode() : 0);
        result = 31 * result + (awsGeneratedTags != null ? awsGeneratedTags.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("S3Bucket")
                       .add("name", name)
                       .add("ownerIdentity", ownerIdentity)
                       .add("arn", arn)
                       .add("awsGeneratedTags", awsGeneratedTags)
                       .build();
    }
}
