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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSerde;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.ToString;

@SdkPublicApi
public class S3ObjectAnnotation implements SerializableStruct {
    private static final Schema SCHEMA = S3EventNotificationSerde.S3_OBJECT_ANNOTATION;
    private static final Schema M_NAME = SCHEMA.member("name");
    private static final Schema M_SIZE = SCHEMA.member("size");
    private static final Schema M_E_TAG = SCHEMA.member("eTag");
    private final String name;
    private final Long size;
    private final String eTag;

    public S3ObjectAnnotation(String name, Long size, String eTag) {
        this.name = name;
        this.size = size;
        this.eTag = eTag;
    }

    public String getName() {
        return name;
    }

    public Long getSize() {
        return size;
    }

    public String getETag() {
        return eTag;
    }

    @Override
    public Schema sdkSchema() {
        return SCHEMA;
    }

    /**
     * Writes every member, as {@code null} when it is null.
     */
    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        writeNullable(serializer, M_NAME, name);
        writeNullable(serializer, M_SIZE, size);
        writeNullable(serializer, M_E_TAG, eTag);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        switch (member.memberIndex()) {
            case 0:
                return (T) SchemaUtils.validateSameMember(M_NAME, member, name);
            case 1:
                return (T) SchemaUtils.validateSameMember(M_SIZE, member, size);
            case 2:
                return (T) SchemaUtils.validateSameMember(M_E_TAG, member, eTag);
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
