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
 * The identity of the principal that caused the event or that owns the bucket.
 */
public class UserIdentity implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.USER_IDENTITY.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.USER_IDENTITY;
    private static final Schema SCHEMA_PRINCIPAL_ID = SCHEMA.member("principalId");

    private final String principalId;

    public UserIdentity(String principalId) {
        this.principalId = principalId;
    }

    /**
     * @return the Amazon customer ID of the user who caused the event.
     */
    public String getPrincipalId() {
        return principalId;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (principalId != null) {
            serializer.writeString(SCHEMA_PRINCIPAL_ID, principalId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        if (member.memberIndex() == 0) {
            return (T) SchemaUtils.validateSameMember(SCHEMA_PRINCIPAL_ID, member, principalId);
        }
        throw SerdeSupport.unknownMember(SCHEMA, member);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserIdentity that = (UserIdentity) o;

        return Objects.equals(principalId, that.principalId);
    }

    @Override
    public int hashCode() {
        return principalId != null ? principalId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("UserIdentity")
                       .add("principalId", principalId)
                       .build();
    }
}
