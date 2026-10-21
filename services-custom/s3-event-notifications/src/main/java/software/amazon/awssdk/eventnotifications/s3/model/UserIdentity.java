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
public class UserIdentity implements SerializableStruct {
    private static final Schema SCHEMA = S3EventNotificationSerde.USER_IDENTITY;
    private static final Schema M_PRINCIPAL_ID = SCHEMA.member("principalId");

    private final String principalId;

    public UserIdentity(String principalId) {
        this.principalId = principalId;
    }

    public String getPrincipalId() {
        return principalId;
    }

    @Override
    public Schema sdkSchema() {
        return SCHEMA;
    }

    /**
     * Writes {@code principalId}, as {@code null} when it is null.
     */
    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        writeNullable(serializer, M_PRINCIPAL_ID, principalId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        if (member.memberIndex() == 0) {
            return (T) SchemaUtils.validateSameMember(M_PRINCIPAL_ID, member, principalId);
        }
        throw SchemaUtils.notAMember(SCHEMA, member);
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
