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

import java.util.List;
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
 * The bucket and object involved in the event.
 */
public class S3 implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.S3.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.S3;
    private static final Schema SCHEMA_S3_SCHEMA_VERSION = SCHEMA.member("s3SchemaVersion");
    private static final Schema SCHEMA_CONFIGURATION_ID = SCHEMA.member("configurationId");
    private static final Schema SCHEMA_BUCKET = SCHEMA.member("bucket");
    private static final Schema SCHEMA_OBJECT = SCHEMA.member("object");
    private static final Schema SCHEMA_OBJECT_ANNOTATION = SCHEMA.member("objectAnnotation");

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
     * @return the ID found in the bucket notification configuration.
     */
    public String getConfigurationId() {
        return configurationId;
    }

    /**
     * @return the bucket involved in the event.
     */
    public S3Bucket getBucket() {
        return bucket;
    }

    /**
     * @return the object involved in the event.
     */
    public S3Object getObject() {
        return object;
    }

    /**
     * @return the version of the {@code s3} part of the message, for example {@code 1.0}.
     */
    public String getS3SchemaVersion() {
        return s3SchemaVersion;
    }

    /**
     * @return the annotations of the object, or {@code null} if the event has none.
     */
    public List<S3ObjectAnnotation> getObjectAnnotation() {
        return objectAnnotation;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (s3SchemaVersion != null) {
            serializer.writeString(SCHEMA_S3_SCHEMA_VERSION, s3SchemaVersion);
        }
        if (configurationId != null) {
            serializer.writeString(SCHEMA_CONFIGURATION_ID, configurationId);
        }
        if (bucket != null) {
            serializer.writeStruct(SCHEMA_BUCKET, bucket);
        }
        if (object != null) {
            serializer.writeStruct(SCHEMA_OBJECT, object);
        }
        if (objectAnnotation != null) {
            SerdeSupport.writeStructList(serializer, SCHEMA_OBJECT_ANNOTATION, objectAnnotation);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_S3_SCHEMA_VERSION, member, s3SchemaVersion);
            case 1 -> (T) SchemaUtils.validateSameMember(SCHEMA_CONFIGURATION_ID, member, configurationId);
            case 2 -> (T) SchemaUtils.validateSameMember(SCHEMA_BUCKET, member, bucket);
            case 3 -> (T) SchemaUtils.validateSameMember(SCHEMA_OBJECT, member, object);
            case 4 -> (T) SchemaUtils.validateSameMember(SCHEMA_OBJECT_ANNOTATION, member, objectAnnotation);
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
