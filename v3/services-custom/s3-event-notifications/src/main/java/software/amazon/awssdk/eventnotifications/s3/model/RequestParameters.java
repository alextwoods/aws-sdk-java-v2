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
 * The request parameters of the request that caused the event.
 */
public class RequestParameters implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.REQUEST_PARAMETERS.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.REQUEST_PARAMETERS;
    private static final Schema SCHEMA_SOURCE_IPADDRESS = SCHEMA.member("sourceIPAddress");

    private final String sourceIpAddress;

    public RequestParameters(String sourceIpAddress) {
        this.sourceIpAddress = sourceIpAddress;
    }

    /**
     * @return the IP address where the request came from.
     */
    public String getSourceIpAddress() {
        return sourceIpAddress;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (sourceIpAddress != null) {
            serializer.writeString(SCHEMA_SOURCE_IPADDRESS, sourceIpAddress);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember(SCHEMA_SOURCE_IPADDRESS, member, sourceIpAddress);
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

        RequestParameters that = (RequestParameters) o;

        return Objects.equals(sourceIpAddress, that.sourceIpAddress);
    }

    @Override
    public int hashCode() {
        return sourceIpAddress != null ? sourceIpAddress.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("RequestParameters")
                       .add("sourceIpAddress", sourceIpAddress)
                       .build();
    }
}
