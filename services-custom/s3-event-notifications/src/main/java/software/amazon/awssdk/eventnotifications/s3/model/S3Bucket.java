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


import static software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSerde.writeNullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSerde;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.ToString;

/**
 * Bucket information.
 */
@SdkPublicApi
public class S3Bucket implements SerializableStruct {
    private static final Schema SCHEMA = S3EventNotificationSerde.S3_BUCKET;
    private static final Schema M_NAME = SCHEMA.member("name");
    private static final Schema M_OWNER_IDENTITY = SCHEMA.member("ownerIdentity");
    private static final Schema M_ARN = SCHEMA.member("arn");
    private static final Schema M_AWS_GENERATED_TAGS = SCHEMA.member("awsGeneratedTags");

    private final String name;
    private final UserIdentity ownerIdentity;
    private final String arn;
    private final Map<String, String> awsGeneratedTags;

    public S3Bucket(String name, UserIdentity ownerIdentity, String arn) {
        this(name, ownerIdentity, arn, null);
    }

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
     * @return the user identity containing the Amazon customer ID of the bucket owner.
     */
    public UserIdentity getOwnerIdentity() {
        return ownerIdentity;
    }

    /**
     * @return The bucket ARN.
     */
    public String getArn() {
        return arn;
    }

    /**
     * @return The AWS-generated system tags for the bucket, or {@code null} if not present in the event. The returned map
     *         is unmodifiable.
     */
    public Map<String, String> getAwsGeneratedTags() {
        return awsGeneratedTags;
    }

    @Override
    public Schema sdkSchema() {
        return SCHEMA;
    }

    /**
     * Writes {@code name}, {@code ownerIdentity} and {@code arn} even when they are null, as {@code null}; an owner
     * identity without a principal ID is written as {@code null} too. {@code awsGeneratedTags} is written only when it
     * is set and not empty.
     */
    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        writeNullable(serializer, M_NAME, name);
        if (ownerIdentity == null || ownerIdentity.getPrincipalId() == null) {
            serializer.writeNull(M_OWNER_IDENTITY);
        } else {
            serializer.writeStruct(M_OWNER_IDENTITY, ownerIdentity);
        }
        writeNullable(serializer, M_ARN, arn);
        if (awsGeneratedTags != null && !awsGeneratedTags.isEmpty()) {
            S3EventNotificationSerde.writeTags(serializer, M_AWS_GENERATED_TAGS, awsGeneratedTags);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        switch (member.memberIndex()) {
            case 0:
                return (T) SchemaUtils.validateSameMember(M_NAME, member, name);
            case 1:
                return (T) SchemaUtils.validateSameMember(M_OWNER_IDENTITY, member, ownerIdentity);
            case 2:
                return (T) SchemaUtils.validateSameMember(M_ARN, member, arn);
            case 3:
                return (T) SchemaUtils.validateSameMember(M_AWS_GENERATED_TAGS, member, awsGeneratedTags);
            default:
                throw SchemaUtils.notAMember(SCHEMA, member);
        }
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
        ToString builder = ToString.builder("S3Bucket")
                                   .add("name", name)
                                   .add("ownerIdentity", ownerIdentity)
                                   .add("arn", arn);
        if (awsGeneratedTags != null) {
            builder.add("awsGeneratedTags", awsGeneratedTags);
        }
        return builder.build();
    }
}
