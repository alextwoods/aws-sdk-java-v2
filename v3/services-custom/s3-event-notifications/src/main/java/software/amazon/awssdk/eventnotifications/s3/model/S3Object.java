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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
 * The object involved in the event.
 */
public class S3Object implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.S3_OBJECT.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.S3_OBJECT;
    private static final Schema SCHEMA_KEY = SCHEMA.member("key");
    private static final Schema SCHEMA_SIZE = SCHEMA.member("size");
    private static final Schema SCHEMA_E_TAG = SCHEMA.member("eTag");
    private static final Schema SCHEMA_VERSION_ID = SCHEMA.member("versionId");
    private static final Schema SCHEMA_SEQUENCER = SCHEMA.member("sequencer");
    private static final Schema SCHEMA_HAS_OBJECT_ANNOTATION = SCHEMA.member("hasObjectAnnotation");

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
     * The key of the object, URL-encoded as Amazon S3 sends it: for example "red flower.jpg" is "red+flower.jpg".
     *
     * @return the URL-encoded object key.
     * @see #getUrlDecodedKey()
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the object key, URL-decoded as UTF-8.
     * @throws NullPointerException if the key is {@code null}
     * @throws IllegalArgumentException if the key is not a valid URL-encoded string
     */
    public String getUrlDecodedKey() {
        return URLDecoder.decode(getKey(), StandardCharsets.UTF_8);
    }

    /**
     * @return the object size in bytes.
     */
    public Long getSizeAsLong() {
        return size;
    }

    /**
     * @return the object entity tag.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * @return the object version, if the bucket is versioning-enabled; otherwise {@code null}.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * The sequencer provides a way to determine the sequence of events: notifications are not guaranteed to arrive in
     * the order the events occurred, but notifications of events that create objects (PUTs) and delete objects
     * contain a sequencer that, compared as a string of hexadecimal values, orders the events of one object key.
     *
     * @return the sequencer.
     */
    public String getSequencer() {
        return sequencer;
    }

    /**
     * @return whether the object has annotations, or {@code null} if the event does not say.
     */
    public Boolean getHasObjectAnnotation() {
        return hasObjectAnnotation;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (key != null) {
            serializer.writeString(SCHEMA_KEY, key);
        }
        if (size != null) {
            serializer.writeLong(SCHEMA_SIZE, size);
        }
        if (eTag != null) {
            serializer.writeString(SCHEMA_E_TAG, eTag);
        }
        if (versionId != null) {
            serializer.writeString(SCHEMA_VERSION_ID, versionId);
        }
        if (sequencer != null) {
            serializer.writeString(SCHEMA_SEQUENCER, sequencer);
        }
        if (hasObjectAnnotation != null) {
            serializer.writeBoolean(SCHEMA_HAS_OBJECT_ANNOTATION, hasObjectAnnotation);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_KEY, member, key);
            case 1 -> (T) SchemaUtils.validateSameMember(SCHEMA_SIZE, member, size);
            case 2 -> (T) SchemaUtils.validateSameMember(SCHEMA_E_TAG, member, eTag);
            case 3 -> (T) SchemaUtils.validateSameMember(SCHEMA_VERSION_ID, member, versionId);
            case 4 -> (T) SchemaUtils.validateSameMember(SCHEMA_SEQUENCER, member, sequencer);
            case 5 -> (T) SchemaUtils.validateSameMember(SCHEMA_HAS_OBJECT_ANNOTATION, member, hasObjectAnnotation);
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
        // hasObjectAnnotation is omitted when null, by ToString itself (V2 rendered the same two forms).
        return ToString.builder("S3Object")
                       .add("key", key)
                       .add("size", size)
                       .add("eTag", eTag)
                       .add("versionId", versionId)
                       .add("sequencer", sequencer)
                       .add("hasObjectAnnotation", hasObjectAnnotation)
                       .build();
    }
}
