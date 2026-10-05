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
 * An annotation of the object involved in the event.
 */
public class S3ObjectAnnotation implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.S3_OBJECT_ANNOTATION.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.S3_OBJECT_ANNOTATION;
    private static final Schema SCHEMA_NAME = SCHEMA.member("name");
    private static final Schema SCHEMA_SIZE = SCHEMA.member("size");
    private static final Schema SCHEMA_E_TAG = SCHEMA.member("eTag");

    private final String name;
    private final Long size;
    private final String eTag;

    public S3ObjectAnnotation(String name, Long size, String eTag) {
        this.name = name;
        this.size = size;
        this.eTag = eTag;
    }

    /**
     * @return the annotation name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the annotation size, in bytes.
     */
    public Long getSize() {
        return size;
    }

    /**
     * @return the annotation entity tag.
     */
    public String getETag() {
        return eTag;
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
        if (size != null) {
            serializer.writeLong(SCHEMA_SIZE, size);
        }
        if (eTag != null) {
            serializer.writeString(SCHEMA_E_TAG, eTag);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_NAME, member, name);
            case 1 -> (T) SchemaUtils.validateSameMember(SCHEMA_SIZE, member, size);
            case 2 -> (T) SchemaUtils.validateSameMember(SCHEMA_E_TAG, member, eTag);
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

        S3ObjectAnnotation that = (S3ObjectAnnotation) o;

        if (!Objects.equals(name, that.name)) {
            return false;
        }
        if (!Objects.equals(size, that.size)) {
            return false;
        }
        return Objects.equals(eTag, that.eTag);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (eTag != null ? eTag.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("S3ObjectAnnotation")
                       .add("name", name)
                       .add("size", size)
                       .add("eTag", eTag)
                       .build();
    }
}
