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

import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSerde;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.ToString;

/**
 * The s3 key provides information about the bucket and object involved in the event. The object key name value is URL encoded.
 * For example, "red flower.jpg" becomes "red+flower.jpg" (Amazon S3 returns "application/x-www-form-urlencoded" as the content
 * type in the response).
 */
@SdkPublicApi
public class S3 implements SerializableStruct {
    private static final Schema SCHEMA = S3EventNotificationSerde.S3;
    private static final Schema M_S3_SCHEMA_VERSION = SCHEMA.member("s3SchemaVersion");
    private static final Schema M_CONFIGURATION_ID = SCHEMA.member("configurationId");
    private static final Schema M_BUCKET = SCHEMA.member("bucket");
    private static final Schema M_OBJECT = SCHEMA.member("object");
    private static final Schema M_OBJECT_ANNOTATION = SCHEMA.member("objectAnnotation");

    private final String configurationId;
    private final S3Bucket bucket;
    private final S3Object object;
    private final String s3SchemaVersion;
    private final List<S3ObjectAnnotation> objectAnnotation;

    public S3(String configurationId, S3Bucket bucket, S3Object object, String s3SchemaVersion) {
        this(configurationId, bucket, object, s3SchemaVersion, null);
    }

    public S3(String configurationId, S3Bucket bucket, S3Object object, String s3SchemaVersion,
              List<S3ObjectAnnotation> objectAnnotation) {
        this.configurationId = configurationId;
        this.bucket = bucket;
        this.object = object;
        this.s3SchemaVersion = s3SchemaVersion;
        this.objectAnnotation = objectAnnotation;
    }

    /**
     * @return the ID found in the bucket notification configuration
     */
    public String getConfigurationId() {
        return configurationId;
    }

    /**
     * @return the bucket information.
     */
    public S3Bucket getBucket() {
        return bucket;
    }

    /**
     *
     * @return
     */
    public S3Object getObject() {
        return object;
    }

    public String getS3SchemaVersion() {
        return s3SchemaVersion;
    }

    public List<S3ObjectAnnotation> getObjectAnnotation() {
        return objectAnnotation;
    }

    @Override
    public Schema sdkSchema() {
        return SCHEMA;
    }

    /**
     * Writes every member but {@code objectAnnotation} even when it is null, as {@code null}, and
     * {@code objectAnnotation} only when it is set.
     */
    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        writeNullable(serializer, M_S3_SCHEMA_VERSION, s3SchemaVersion);
        writeNullable(serializer, M_CONFIGURATION_ID, configurationId);
        writeNullable(serializer, M_BUCKET, bucket);
        writeNullable(serializer, M_OBJECT, object);
        if (objectAnnotation != null) {
            S3EventNotificationSerde.writeAnnotations(serializer, M_OBJECT_ANNOTATION, objectAnnotation);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        switch (member.memberIndex()) {
            case 0:
                return (T) SchemaUtils.validateSameMember(M_S3_SCHEMA_VERSION, member, s3SchemaVersion);
            case 1:
                return (T) SchemaUtils.validateSameMember(M_CONFIGURATION_ID, member, configurationId);
            case 2:
                return (T) SchemaUtils.validateSameMember(M_BUCKET, member, bucket);
            case 3:
                return (T) SchemaUtils.validateSameMember(M_OBJECT, member, object);
            case 4:
                return (T) SchemaUtils.validateSameMember(M_OBJECT_ANNOTATION, member, objectAnnotation);
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

        S3 s3 = (S3) o;

        if (!Objects.equals(configurationId, s3.configurationId)) {
            return false;
        }
        if (!Objects.equals(bucket, s3.bucket)) {
            return false;
        }
        if (!Objects.equals(object, s3.object)) {
            return false;
        }
        if (!Objects.equals(s3SchemaVersion, s3.s3SchemaVersion)) {
            return false;
        }
        return Objects.equals(objectAnnotation, s3.objectAnnotation);
    }

    @Override
    public int hashCode() {
        int result = configurationId != null ? configurationId.hashCode() : 0;
        result = 31 * result + (bucket != null ? bucket.hashCode() : 0);
        result = 31 * result + (object != null ? object.hashCode() : 0);
        result = 31 * result + (s3SchemaVersion != null ? s3SchemaVersion.hashCode() : 0);
        result = 31 * result + (objectAnnotation != null ? objectAnnotation.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("S3")
                       .add("configurationId", configurationId)
                       .add("bucket", bucket)
                       .add("object", object)
                       .add("s3SchemaVersion", s3SchemaVersion)
                       .add("objectAnnotation", objectAnnotation)
                       .build();
    }
}
