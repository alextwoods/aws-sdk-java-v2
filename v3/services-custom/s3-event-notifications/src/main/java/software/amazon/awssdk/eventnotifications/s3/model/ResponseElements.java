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
 * The trace information of the request that caused the event: the {@code x-amz-id-2} and
 * {@code x-amz-request-id} values Amazon S3 returned for it.
 */
public class ResponseElements implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.RESPONSE_ELEMENTS.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.RESPONSE_ELEMENTS;
    private static final Schema SCHEMA_X_AMZ_REQUEST_ID = SCHEMA.member("xAmzRequestId");
    private static final Schema SCHEMA_X_AMZ_ID2 = SCHEMA.member("xAmzId2");

    private final String xAmzId2;
    private final String xAmzRequestId;

    public ResponseElements(String xAmzId2, String xAmzRequestId) {
        this.xAmzId2 = xAmzId2;
        this.xAmzRequestId = xAmzRequestId;
    }

    /**
     * @return the Amazon S3 host that processed the request.
     */
    public String getXAmzId2() {
        return xAmzId2;
    }

    /**
     * @return the Amazon S3 generated request ID.
     */
    public String getXAmzRequestId() {
        return xAmzRequestId;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (xAmzRequestId != null) {
            serializer.writeString(SCHEMA_X_AMZ_REQUEST_ID, xAmzRequestId);
        }
        if (xAmzId2 != null) {
            serializer.writeString(SCHEMA_X_AMZ_ID2, xAmzId2);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_X_AMZ_REQUEST_ID, member, xAmzRequestId);
            case 1 -> (T) SchemaUtils.validateSameMember(SCHEMA_X_AMZ_ID2, member, xAmzId2);
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

        ResponseElements that = (ResponseElements) o;

        if (!Objects.equals(xAmzId2, that.xAmzId2)) {
            return false;
        }
        return Objects.equals(xAmzRequestId, that.xAmzRequestId);
    }

    @Override
    public int hashCode() {
        int result = xAmzId2 != null ? xAmzId2.hashCode() : 0;
        result = 31 * result + (xAmzRequestId != null ? xAmzRequestId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("ResponseElements")
                       .add("xAmzId2", xAmzId2)
                       .add("xAmzRequestId", xAmzRequestId)
                       .build();
    }
}
