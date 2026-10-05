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

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationReader;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationSchemas;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationWriter;
import software.amazon.awssdk.eventnotifications.s3.internal.SerdeSupport;
import software.amazon.awssdk.utils.ToString;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A helper class that represents a strongly typed S3 Event Notification item sent to SQS, SNS, or Lambda. For more
 * information about Amazon S3 Event Notifications, visit the
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/EventNotifications.html">S3 User Guide</a>.
 * This class can be used to parse notification messages in the json format or to serialize a S3EventNotification
 * instance to json.
 *
 * <p>Every model type of this package is also a Smithy {@link SerializableStruct} with one published {@code SCHEMA},
 * registered in {@link software.amazon.awssdk.eventnotifications.s3.S3EventNotificationMetadata#typeRegistry()}, so
 * notifications convert to and from Smithy Documents.
 */
public class S3EventNotification implements SerializableStruct {
    /** The absolute shape ID of this type. */
    public static final ShapeId ID = S3EventNotificationSchemas.S3_EVENT_NOTIFICATION.id();
    /** The one schema of this type. */
    public static final Schema SCHEMA = S3EventNotificationSchemas.S3_EVENT_NOTIFICATION;
    private static final Schema SCHEMA_RECORDS = SCHEMA.member("Records");

    private final List<S3EventNotificationRecord> records;

    public S3EventNotification(List<S3EventNotificationRecord> records) {
        this.records = records;
    }

    /**
     * @return the records of the notification, as given to the constructor or read from json; {@code null} elements
     *         and a {@code null} list are kept.
     */
    public List<S3EventNotificationRecord> getRecords() {
        return records;
    }

    /**
     * Converts a json representation of the notification message to an instance of S3EventNotification. Any missing
     * fields of the json will be null in the resulting object. Any extra fields will be ignored.
     *
     * @param json the notification message in json format
     * @return an instance of notification message S3EventNotification
     * @throws IllegalArgumentException if a field of the message has an unexpected JSON type
     * @throws java.io.UncheckedIOException if {@code json} is not valid JSON
     */
    public static S3EventNotification fromJson(String json) {
        return S3EventNotificationReader.read(json);
    }

    /**
     * Converts a json representation of the notification message to an instance of S3EventNotification. Any missing
     * fields of the json will be null in the resulting object. Any extra fields will be ignored.
     *
     * @param json the notification message in UTF-8 encoded json format
     * @return an instance of notification message S3EventNotification
     * @throws IllegalArgumentException if a field of the message has an unexpected JSON type
     * @throws java.io.UncheckedIOException if {@code json} is not valid JSON
     */
    public static S3EventNotification fromJson(byte[] json) {
        return S3EventNotificationReader.read(json);
    }

    /**
     * Converts a json representation of the notification message to an instance of S3EventNotification. Any missing
     * fields of the json will be null in the resulting object. Any extra fields will be ignored. The stream is read to
     * its end and not closed.
     *
     * @param json the notification message in UTF-8 encoded json format
     * @return an instance of notification message S3EventNotification
     * @throws IllegalArgumentException if a field of the message has an unexpected JSON type
     * @throws java.io.UncheckedIOException if {@code json} cannot be read or is not valid JSON
     */
    public static S3EventNotification fromJson(InputStream json) {
        return S3EventNotificationReader.read(json);
    }

    /**
     * Serialize this instance to json format. {@link GlacierEventData}, {@link ReplicationEventData},
     * {@link IntelligentTieringEventData}, {@link LifecycleEventData} and {@link ObjectRetentionEventData} keys will be
     * excluded from the json if {@code null}. Any other null fields of the object will be serialized as json
     * {@code null}.
     *
     * @return the json representation of this class.
     */
    public String toJson() {
        return S3EventNotificationWriter.COMPACT.writeToString(this);
    }

    /**
     * Serialize this instance to json format, with new line and correct indentation levels. {@link GlacierEventData},
     * {@link ReplicationEventData}, {@link IntelligentTieringEventData}, {@link LifecycleEventData} and
     * {@link ObjectRetentionEventData} keys will be excluded from the json if {@code null}. Any other null fields of
     * the object will be serialized as json {@code null}.
     *
     * @return the json representation of this class.
     */
    public String toJsonPretty() {
        return S3EventNotificationWriter.PRETTY.writeToString(this);
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (records != null) {
            SerdeSupport.writeStructList(serializer, SCHEMA_RECORDS, records);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        if (member.memberIndex() == 0) {
            return (T) SchemaUtils.validateSameMember(SCHEMA_RECORDS, member, records);
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

        S3EventNotification that = (S3EventNotification) o;

        return Objects.equals(records, that.records);
    }

    @Override
    public int hashCode() {
        return records != null ? records.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("S3EventNotification")
                       .add("records", records)
                       .build();
    }
}
