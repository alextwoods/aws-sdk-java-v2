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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSerde;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.ToString;

/**
 * Object information.
 */
@SdkPublicApi
public class S3Object implements SerializableStruct {
    private static final Schema SCHEMA = S3EventNotificationSerde.S3_OBJECT;
    private static final Schema M_KEY = SCHEMA.member("key");
    private static final Schema M_SIZE = SCHEMA.member("size");
    private static final Schema M_E_TAG = SCHEMA.member("eTag");
    private static final Schema M_VERSION_ID = SCHEMA.member("versionId");
    private static final Schema M_SEQUENCER = SCHEMA.member("sequencer");
    private static final Schema M_HAS_OBJECT_ANNOTATION = SCHEMA.member("hasObjectAnnotation");

    private final String key;
    private final Long size;
    private final String eTag;
    private final String versionId;
    private final String sequencer;
    private final Boolean hasObjectAnnotation;

    public S3Object(String key, Long size, String eTag, String versionId, String sequencer) {
        this(key, size, eTag, versionId, sequencer, null);
    }

    public S3Object(String key, Long size, String eTag, String versionId, String sequencer, Boolean hasObjectAnnotation) {
        this.key = key;
        this.size = size;
        this.eTag = eTag;
        this.versionId = versionId;
        this.sequencer = sequencer;
        this.hasObjectAnnotation = hasObjectAnnotation;
    }

    /**
     * @return the object key.
     */
    public String getKey() {
        return key;
    }

    /**
     * S3 URL encodes the key of the object involved in the event. This is a convenience method to automatically URL decode the
     * key.
     *
     * @return The URL decoded object key.
     */
    public String getUrlDecodedKey() {
        try {
            return URLDecoder.decode(getKey(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the object size in bytes.
     */
    public Long getSizeAsLong() {
        return size;
    }

    /**
     * @return the object eTag
     */
    public String getETag() {
        return eTag;
    }

    /**
     * @return the object version if bucket is versioning-enabled, otherwise null
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * The sequencer key provides a way to determine the sequence of events. Event notifications aren't guaranteed to arrive in
     * the same order that the events occurred. However, notifications from events that create objects (PUTs) and delete
     * objects contain a sequencer. It can be used to determine the order of events for a given object key.
     * @return A string representation of a hexadecimal value used to determine event sequence, only used with PUTs and DELETEs.
     */
    public String getSequencer() {
        return sequencer;
    }

    /**
     * This field is only set on CopyObject events. Returns false when no annotations were copied,
     * true when annotations were copied, or null if not applicable.
     *
     * @return Boolean indicating whether object annotations were copied, or null if not applicable
     */
    public Boolean getHasObjectAnnotation() {
        return hasObjectAnnotation;
    }

    @Override
    public Schema sdkSchema() {
        return SCHEMA;
    }

    /**
     * Writes every member but {@code hasObjectAnnotation} even when it is null, as {@code null}, and
     * {@code hasObjectAnnotation} only when it is set.
     */
    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        writeNullable(serializer, M_KEY, key);
        writeNullable(serializer, M_SIZE, size);
        writeNullable(serializer, M_E_TAG, eTag);
        writeNullable(serializer, M_VERSION_ID, versionId);
        writeNullable(serializer, M_SEQUENCER, sequencer);
        if (hasObjectAnnotation != null) {
            serializer.writeBoolean(M_HAS_OBJECT_ANNOTATION, hasObjectAnnotation);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        switch (member.memberIndex()) {
            case 0:
                return (T) SchemaUtils.validateSameMember(M_KEY, member, key);
            case 1:
                return (T) SchemaUtils.validateSameMember(M_SIZE, member, size);
            case 2:
                return (T) SchemaUtils.validateSameMember(M_E_TAG, member, eTag);
            case 3:
                return (T) SchemaUtils.validateSameMember(M_VERSION_ID, member, versionId);
            case 4:
                return (T) SchemaUtils.validateSameMember(M_SEQUENCER, member, sequencer);
            case 5:
                return (T) SchemaUtils.validateSameMember(M_HAS_OBJECT_ANNOTATION, member, hasObjectAnnotation);
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

        S3Object s3Object = (S3Object) o;

        if (!Objects.equals(key, s3Object.key)) {
            return false;
        }
        if (!Objects.equals(size, s3Object.size)) {
            return false;
        }
        if (!Objects.equals(eTag, s3Object.eTag)) {
            return false;
        }
        if (!Objects.equals(versionId, s3Object.versionId)) {
            return false;
        }
        if (!Objects.equals(sequencer, s3Object.sequencer)) {
            return false;
        }
        return Objects.equals(hasObjectAnnotation, s3Object.hasObjectAnnotation);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (eTag != null ? eTag.hashCode() : 0);
        result = 31 * result + (versionId != null ? versionId.hashCode() : 0);
        result = 31 * result + (sequencer != null ? sequencer.hashCode() : 0);
        result = 31 * result + (hasObjectAnnotation != null ? hasObjectAnnotation.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (hasObjectAnnotation != null) {
            return ToString.builder("S3Object")
                           .add("key", key)
                           .add("size", size)
                           .add("eTag", eTag)
                           .add("versionId", versionId)
                           .add("sequencer", sequencer)
                           .add("hasObjectAnnotation", hasObjectAnnotation)
                           .build();
        }
        return ToString.builder("S3Object")
                       .add("key", key)
                       .add("size", size)
                       .add("eTag", eTag)
                       .add("versionId", versionId)
                       .add("sequencer", sequencer)
                       .build();
    }
}
