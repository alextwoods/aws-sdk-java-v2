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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSerde;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.ToString;

/**
 * The LifecycleEventData is only visible for
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/lifecycle-transition-general-considerations.html">
 *     S3 Lifecycle transition</a> related events.
 */
@SdkPublicApi
public class LifecycleEventData implements SerializableStruct {
    private static final Schema SCHEMA = S3EventNotificationSerde.LIFECYCLE_EVENT_DATA;
    private static final Schema M_TRANSITION_EVENT_DATA = SCHEMA.member("transitionEventData");

    private final TransitionEventData transitionEventData;

    public LifecycleEventData(TransitionEventData transitionEventData) {
        this.transitionEventData = transitionEventData;
    }

    public TransitionEventData getTransitionEventData() {
        return transitionEventData;
    }

    @Override
    public Schema sdkSchema() {
        return SCHEMA;
    }

    /**
     * Writes {@code transitionEventData} only when it is set.
     */
    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (transitionEventData != null) {
            serializer.writeStruct(M_TRANSITION_EVENT_DATA, transitionEventData);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        if (member.memberIndex() == 0) {
            return (T) SchemaUtils.validateSameMember(M_TRANSITION_EVENT_DATA, member, transitionEventData);
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

        LifecycleEventData that = (LifecycleEventData) o;

        return Objects.equals(transitionEventData, that.transitionEventData);
    }

    @Override
    public int hashCode() {
        return transitionEventData != null ? transitionEventData.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("LifecycleEventDataEntity")
                       .add("transitionEventData", transitionEventData)
                       .build();
    }
}
