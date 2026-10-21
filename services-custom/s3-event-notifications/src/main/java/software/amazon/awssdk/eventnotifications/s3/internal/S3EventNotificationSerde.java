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

package software.amazon.awssdk.eventnotifications.s3.internal;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.eventnotifications.s3.model.GlacierEventData;
import software.amazon.awssdk.eventnotifications.s3.model.IntelligentTieringEventData;
import software.amazon.awssdk.eventnotifications.s3.model.LifecycleEventData;
import software.amazon.awssdk.eventnotifications.s3.model.ReplicationEventData;
import software.amazon.awssdk.eventnotifications.s3.model.RequestParameters;
import software.amazon.awssdk.eventnotifications.s3.model.ResponseElements;
import software.amazon.awssdk.eventnotifications.s3.model.RestoreEventData;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3Bucket;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.eventnotifications.s3.model.S3Object;
import software.amazon.awssdk.eventnotifications.s3.model.S3ObjectAnnotation;
import software.amazon.awssdk.eventnotifications.s3.model.TransitionEventData;
import software.amazon.awssdk.eventnotifications.s3.model.UserIdentity;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.JsonNameTrait;

/**
 * Schema serde of the S3 Event Notification model classes (design D.7): the schemas generated from
 * {@code s3-event-notification.smithy}, the {@link ShapeBuilder}s, and the helpers the model classes'
 * {@code serializeMembers} use.
 *
 * <p>The serde reproduces the JSON of the JsonNodeParser reader and JsonWriter writer that it replaces:
 * <ul>
 *     <li>Reading: absent and null members are null; unknown members are skipped; null list elements and map values
 *     are kept. A value of the wrong JSON type throws {@link IllegalArgumentException} with the previous reader's
 *     message, which names the JSON field. Instants are read with {@link Instant#parse}.</li>
 *     <li>Writing: the members the previous writer always wrote are written as {@code null} when they are null (the
 *     {@code writeNullable} helpers); the others are left out when null. Instants are written with
 *     {@link DateTimeFormatter#ISO_INSTANT}.</li>
 * </ul>
 */
@SdkInternalApi
public final class S3EventNotificationSerde {
    public static final Schema S3_EVENT_NOTIFICATION = S3EventNotificationSchemas.S3_EVENT_NOTIFICATION;
    public static final Schema S3_EVENT_NOTIFICATION_RECORD = S3EventNotificationSchemas.S3_EVENT_NOTIFICATION_RECORD;
    public static final Schema USER_IDENTITY = S3EventNotificationSchemas.USER_IDENTITY;
    public static final Schema REQUEST_PARAMETERS = S3EventNotificationSchemas.REQUEST_PARAMETERS;
    public static final Schema RESPONSE_ELEMENTS = S3EventNotificationSchemas.RESPONSE_ELEMENTS;
    public static final Schema S3 = S3EventNotificationSchemas.SOFTWARE_AMAZON_AWSSDK_EVENTNOTIFICATIONS_S3_S3;
    public static final Schema S3_BUCKET = S3EventNotificationSchemas.S3_BUCKET;
    public static final Schema S3_OBJECT = S3EventNotificationSchemas.S3_OBJECT;
    public static final Schema S3_OBJECT_ANNOTATION = S3EventNotificationSchemas.S3_OBJECT_ANNOTATION;
    public static final Schema GLACIER_EVENT_DATA = S3EventNotificationSchemas.GLACIER_EVENT_DATA;
    public static final Schema RESTORE_EVENT_DATA = S3EventNotificationSchemas.RESTORE_EVENT_DATA;
    public static final Schema REPLICATION_EVENT_DATA = S3EventNotificationSchemas.REPLICATION_EVENT_DATA;
    public static final Schema INTELLIGENT_TIERING_EVENT_DATA = S3EventNotificationSchemas.INTELLIGENT_TIERING_EVENT_DATA;
    public static final Schema LIFECYCLE_EVENT_DATA = S3EventNotificationSchemas.LIFECYCLE_EVENT_DATA;
    public static final Schema TRANSITION_EVENT_DATA = S3EventNotificationSchemas.TRANSITION_EVENT_DATA;

    /**
     * The codec of {@code fromJson} and {@code toJson}: member names from {@code jsonName}
     * ({@code x-amz-request-id}, {@code x-amz-id-2}).
     */
    static final JsonCodec JSON = JsonCodec.builder().useJsonName(true).build();

    /**
     * The codec of {@code toJsonPretty}.
     */
    static final JsonCodec PRETTY_JSON = JsonCodec.builder().useJsonName(true).prettyPrint(true).build();

    private static final Schema RECORD_ELEMENT = S3EventNotificationSchemas.S3_EVENT_NOTIFICATION_RECORD_LIST.listMember();
    private static final Schema ANNOTATION_ELEMENT = S3EventNotificationSchemas.S3_OBJECT_ANNOTATION_LIST.listMember();
    private static final Schema TAG_KEY = S3EventNotificationSchemas.AWS_GENERATED_TAGS.mapKeyMember();
    private static final Schema TAG_VALUE = S3EventNotificationSchemas.AWS_GENERATED_TAGS.mapValueMember();

    private static final BiConsumer<List<S3EventNotificationRecord>, ShapeSerializer> WRITE_RECORDS =
        (records, serializer) -> records.forEach(r -> writeNullable(serializer, RECORD_ELEMENT, r));
    private static final BiConsumer<List<S3ObjectAnnotation>, ShapeSerializer> WRITE_ANNOTATIONS =
        (annotations, serializer) -> annotations.forEach(a -> writeNullable(serializer, ANNOTATION_ELEMENT, a));
    private static final BiConsumer<String, ShapeSerializer> WRITE_TAG_VALUE =
        (value, serializer) -> writeNullable(serializer, TAG_VALUE, value);
    private static final BiConsumer<Map<String, String>, MapSerializer> WRITE_TAGS =
        (tags, serializer) -> tags.forEach((key, value) -> serializer.writeEntry(TAG_KEY, key, value, WRITE_TAG_VALUE));

    private static final ShapeDeserializer.ListMemberConsumer<List<S3EventNotificationRecord>> READ_RECORD =
        (records, d) -> records.add(d.isNull() ? d.readNull()
                                               : readStruct(d, RECORD_ELEMENT, new RecordBuilder(), "Records[]"));
    private static final ShapeDeserializer.ListMemberConsumer<List<S3ObjectAnnotation>> READ_ANNOTATION =
        (annotations, d) -> annotations.add(d.isNull() ? d.readNull()
                                                       : readStruct(d, ANNOTATION_ELEMENT, new AnnotationBuilder(),
                                                                    "objectAnnotation[]"));
    private static final ShapeDeserializer.MapMemberConsumer<String, Map<String, String>> READ_TAG =
        S3EventNotificationSerde::readTag;

    private S3EventNotificationSerde() {
    }

    /**
     * @return a builder of {@link S3EventNotification}
     */
    public static ShapeBuilder<S3EventNotification> notificationBuilder() {
        return new NotificationBuilder();
    }

    // Writing

    /**
     * Writes a string member, or {@code null} if the value is null.
     */
    public static void writeNullable(ShapeSerializer serializer, Schema member, String value) {
        if (value == null) {
            serializer.writeNull(member);
        } else {
            serializer.writeString(member, value);
        }
    }

    /**
     * Writes a long member, or {@code null} if the value is null.
     */
    public static void writeNullable(ShapeSerializer serializer, Schema member, Long value) {
        if (value == null) {
            serializer.writeNull(member);
        } else {
            serializer.writeLong(member, value);
        }
    }

    /**
     * Writes a structure member or element, or {@code null} if the value is null.
     */
    public static void writeNullable(ShapeSerializer serializer, Schema member, SerializableStruct value) {
        if (value == null) {
            serializer.writeNull(member);
        } else {
            serializer.writeStruct(member, value);
        }
    }

    /**
     * Writes an instant member as an {@link DateTimeFormatter#ISO_INSTANT} string, or {@code null} if it is null.
     */
    public static void writeNullable(ShapeSerializer serializer, Schema member, Instant value) {
        writeNullable(serializer, member, formatInstant(value));
    }

    /**
     * Writes the {@code Records} list, or {@code null} if it is null. Null records are written as {@code null}.
     */
    public static void writeRecords(ShapeSerializer serializer, Schema member, List<S3EventNotificationRecord> records) {
        if (records == null) {
            serializer.writeNull(member);
        } else {
            serializer.writeList(member, records, records.size(), WRITE_RECORDS);
        }
    }

    /**
     * Writes the {@code objectAnnotation} list. Null annotations are written as {@code null}.
     */
    public static void writeAnnotations(ShapeSerializer serializer, Schema member, List<S3ObjectAnnotation> annotations) {
        serializer.writeList(member, annotations, annotations.size(), WRITE_ANNOTATIONS);
    }

    /**
     * Writes the {@code awsGeneratedTags} map in its iteration order. Null values are written as {@code null}.
     */
    public static void writeTags(ShapeSerializer serializer, Schema member, Map<String, String> tags) {
        serializer.writeMap(member, tags, tags.size(), WRITE_TAGS);
    }

    /**
     * @return the instant in the {@link DateTimeFormatter#ISO_INSTANT} format, or null
     */
    public static String formatInstant(Instant instant) {
        return instant == null ? null : DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    // Reading

    private static String readString(ShapeDeserializer d, Schema member) {
        try {
            return d.readString(member);
        } catch (SerializationException e) {
            throw nonConforming(String.format("'%s' was not a string", jsonName(member)), e);
        }
    }

    private static Long readLong(ShapeDeserializer d, Schema member) {
        try {
            return d.readLong(member);
        } catch (SerializationException e) {
            throw nonConforming(String.format("expected '%s' to be numeric, but was not", jsonName(member)), e);
        }
    }

    private static Boolean readBoolean(ShapeDeserializer d, Schema member) {
        try {
            return d.readBoolean(member);
        } catch (SerializationException e) {
            throw nonConforming(String.format("expected '%s' to be boolean, but was not", jsonName(member)), e);
        }
    }

    private static <T extends SerializableShape> T readStruct(ShapeDeserializer d, Schema member,
                                                              ShapeBuilder<T> builder) {
        return readStruct(d, member, builder, jsonName(member));
    }

    private static <T extends SerializableShape> T readStruct(ShapeDeserializer d, Schema member,
                                                              ShapeBuilder<T> builder, String name) {
        try {
            builder.deserializeMember(d, member);
        } catch (SerializationException e) {
            throw nonConforming(String.format("expected '%s' to be an object, but was not.", name), e);
        }
        return builder.build();
    }

    private static <T> List<T> readList(ShapeDeserializer d, Schema member,
                                        ShapeDeserializer.ListMemberConsumer<List<T>> elementReader) {
        List<T> values = new ArrayList<>();
        try {
            d.readList(member, values, elementReader);
        } catch (SerializationException e) {
            throw nonConforming(String.format("expected '%s' to be an array, but was not.", jsonName(member)), e);
        }
        return values;
    }

    private static Map<String, String> readTags(ShapeDeserializer d, Schema member) {
        Map<String, String> tags = new LinkedHashMap<>();
        try {
            d.readStringMap(member, tags, READ_TAG);
        } catch (SerializationException e) {
            throw nonConforming(String.format("expected '%s' to be an object, but was not.", jsonName(member)), e);
        }
        return tags;
    }

    /**
     * Reads a tag value through a document, because {@link ShapeDeserializer#readString} accepts a JSON number too,
     * and the previous reader rejected every value but a string or null.
     */
    private static void readTag(Map<String, String> tags, String key, ShapeDeserializer d) {
        if (d.isNull()) {
            tags.put(key, d.readNull());
            return;
        }
        Document value = d.readDocument();
        if (value.type() != ShapeType.STRING) {
            StringBuilder text = new StringBuilder();
            appendJsonNodeText(text, value);
            throw new IllegalArgumentException(String.format(
                "The value of tag '%s' in '%s' was not a string, but was: %s", key, "awsGeneratedTags", text));
        }
        tags.put(key, value.asString());
    }

    /**
     * Appends a value as the previous reader's {@code JsonNode.toString()} spelled it in its error message: lists as
     * {@code [a, b]}, objects as <code>{"k": v,"k2": v2}</code> with unescaped keys, strings with only {@code \} and
     * {@code "} escaped. Numbers are written in the codec's form ({@code 1.5} for {@code 1.50}).
     */
    private static void appendJsonNodeText(StringBuilder text, Document value) {
        if (value == null) {
            text.append("null");
            return;
        }
        switch (value.type()) {
            case STRING:
                text.append('"').append(value.asString().replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
                break;
            case LIST:
                text.append('[');
                List<Document> elements = value.asList();
                for (int i = 0; i < elements.size(); i++) {
                    text.append(i == 0 ? "" : ", ");
                    appendJsonNodeText(text, elements.get(i));
                }
                text.append(']');
                break;
            case MAP:
            case STRUCTURE:
                text.append('{');
                boolean first = true;
                for (Map.Entry<String, Document> entry : value.asMap().entrySet()) {
                    text.append(first ? "" : ",").append('"').append(entry.getKey()).append("\": ");
                    appendJsonNodeText(text, entry.getValue());
                    first = false;
                }
                text.append('}');
                break;
            default:
                text.append(JSON.serializeToString(value));
                break;
        }
    }

    private static Instant parseInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private static String jsonName(Schema member) {
        JsonNameTrait jsonName = member.getDirectTrait(TraitKey.JSON_NAME_TRAIT);
        return jsonName == null ? member.memberName() : jsonName.getValue();
    }

    private static IllegalArgumentException nonConforming(String message, SerializationException cause) {
        return new IllegalArgumentException(message, cause);
    }

    /**
     * A builder that reads its own structure's members. It is its own member consumer.
     */
    private abstract static class StructBuilder<T extends SerializableShape, B extends StructBuilder<T, B>>
        implements ShapeBuilder<T>, ShapeDeserializer.StructMemberConsumer<B> {
        private final Schema schema;

        StructBuilder(Schema schema) {
            this.schema = schema;
        }

        @Override
        public final Schema sdkSchema() {
            return schema;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ShapeBuilder<T> deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(schema, (B) this, this);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public final ShapeBuilder<T> deserializeMember(ShapeDeserializer decoder, Schema member) {
            decoder.readStruct(member, (B) this, this);
            return this;
        }
    }

    private static final class NotificationBuilder extends StructBuilder<S3EventNotification, NotificationBuilder> {
        private List<S3EventNotificationRecord> records;

        NotificationBuilder() {
            super(S3_EVENT_NOTIFICATION);
        }

        /**
         * A top-level {@code null} is a notification with null records, and any other value that is not an object
         * gets the previous reader's message.
         */
        @Override
        public ShapeBuilder<S3EventNotification> deserialize(ShapeDeserializer decoder) {
            if (decoder.isNull()) {
                decoder.readNull();
                return this;
            }
            try {
                return super.deserialize(decoder);
            } catch (SerializationException e) {
                throw nonConforming("expected 'Records' to be an object, but was not.", e);
            }
        }

        @Override
        public void accept(NotificationBuilder b, Schema member, ShapeDeserializer d) {
            if (member.memberIndex() == 0) {
                b.records = readList(d, member, READ_RECORD);
            }
        }

        @Override
        public S3EventNotification build() {
            return new S3EventNotification(records);
        }
    }

    private static final class RecordBuilder extends StructBuilder<S3EventNotificationRecord, RecordBuilder> {
        private final S3EventNotificationRecord value = new S3EventNotificationRecord();

        RecordBuilder() {
            super(S3_EVENT_NOTIFICATION_RECORD);
        }

        @Override
        public void accept(RecordBuilder b, Schema member, ShapeDeserializer d) {
            S3EventNotificationRecord r = b.value;
            switch (member.memberIndex()) {
                case 0:
                    r.setEventVersion(readString(d, member));
                    break;
                case 1:
                    r.setEventSource(readString(d, member));
                    break;
                case 2:
                    r.setAwsRegion(readString(d, member));
                    break;
                case 3:
                    r.setEventTime(parseInstant(readString(d, member)));
                    break;
                case 4:
                    r.setEventName(readString(d, member));
                    break;
                case 5:
                    r.setUserIdentity(readStruct(d, member, new UserIdentityBuilder()));
                    break;
                case 6:
                    r.setRequestParameters(readStruct(d, member, new RequestParametersBuilder()));
                    break;
                case 7:
                    r.setResponseElements(readStruct(d, member, new ResponseElementsBuilder()));
                    break;
                case 8:
                    r.setS3(readStruct(d, member, new S3Builder()));
                    break;
                case 9:
                    r.setGlacierEventData(readStruct(d, member, new GlacierEventDataBuilder()));
                    break;
                case 10:
                    r.setReplicationEventData(readStruct(d, member, new ReplicationEventDataBuilder()));
                    break;
                case 11:
                    r.setIntelligentTieringEventData(readStruct(d, member, new IntelligentTieringEventDataBuilder()));
                    break;
                case 12:
                    r.setLifecycleEventData(readStruct(d, member, new LifecycleEventDataBuilder()));
                    break;
                default:
                    break;
            }
        }

        @Override
        public S3EventNotificationRecord build() {
            return value;
        }
    }

    private static final class UserIdentityBuilder extends StructBuilder<UserIdentity, UserIdentityBuilder> {
        private String principalId;

        UserIdentityBuilder() {
            super(USER_IDENTITY);
        }

        @Override
        public void accept(UserIdentityBuilder b, Schema member, ShapeDeserializer d) {
            if (member.memberIndex() == 0) {
                b.principalId = readString(d, member);
            }
        }

        @Override
        public UserIdentity build() {
            return new UserIdentity(principalId);
        }
    }

    private static final class RequestParametersBuilder
        extends StructBuilder<RequestParameters, RequestParametersBuilder> {
        private String sourceIpAddress;

        RequestParametersBuilder() {
            super(REQUEST_PARAMETERS);
        }

        @Override
        public void accept(RequestParametersBuilder b, Schema member, ShapeDeserializer d) {
            if (member.memberIndex() == 0) {
                b.sourceIpAddress = readString(d, member);
            }
        }

        @Override
        public RequestParameters build() {
            return new RequestParameters(sourceIpAddress);
        }
    }

    private static final class ResponseElementsBuilder extends StructBuilder<ResponseElements, ResponseElementsBuilder> {
        private String xAmzRequestId;
        private String xAmzId2;

        ResponseElementsBuilder() {
            super(RESPONSE_ELEMENTS);
        }

        @Override
        public void accept(ResponseElementsBuilder b, Schema member, ShapeDeserializer d) {
            switch (member.memberIndex()) {
                case 0:
                    b.xAmzRequestId = readString(d, member);
                    break;
                case 1:
                    b.xAmzId2 = readString(d, member);
                    break;
                default:
                    break;
            }
        }

        @Override
        public ResponseElements build() {
            return new ResponseElements(xAmzId2, xAmzRequestId);
        }
    }

    private static final class S3Builder extends StructBuilder<S3, S3Builder> {
        private String s3SchemaVersion;
        private String configurationId;
        private S3Bucket bucket;
        private S3Object object;
        private List<S3ObjectAnnotation> objectAnnotation;

        S3Builder() {
            super(S3);
        }

        @Override
        public void accept(S3Builder b, Schema member, ShapeDeserializer d) {
            switch (member.memberIndex()) {
                case 0:
                    b.s3SchemaVersion = readString(d, member);
                    break;
                case 1:
                    b.configurationId = readString(d, member);
                    break;
                case 2:
                    b.bucket = readStruct(d, member, new BucketBuilder());
                    break;
                case 3:
                    b.object = readStruct(d, member, new ObjectBuilder());
                    break;
                case 4:
                    b.objectAnnotation = readList(d, member, READ_ANNOTATION);
                    break;
                default:
                    break;
            }
        }

        @Override
        public S3 build() {
            return new S3(configurationId, bucket, object, s3SchemaVersion, objectAnnotation);
        }
    }

    private static final class BucketBuilder extends StructBuilder<S3Bucket, BucketBuilder> {
        private String name;
        private UserIdentity ownerIdentity;
        private String arn;
        private Map<String, String> awsGeneratedTags;

        BucketBuilder() {
            super(S3_BUCKET);
        }

        @Override
        public void accept(BucketBuilder b, Schema member, ShapeDeserializer d) {
            switch (member.memberIndex()) {
                case 0:
                    b.name = readString(d, member);
                    break;
                case 1:
                    b.ownerIdentity = readStruct(d, member, new UserIdentityBuilder());
                    break;
                case 2:
                    b.arn = readString(d, member);
                    break;
                case 3:
                    b.awsGeneratedTags = readTags(d, member);
                    break;
                default:
                    break;
            }
        }

        @Override
        public S3Bucket build() {
            return new S3Bucket(name, ownerIdentity, arn, awsGeneratedTags);
        }
    }

    private static final class ObjectBuilder extends StructBuilder<S3Object, ObjectBuilder> {
        private String key;
        private Long size;
        private String eTag;
        private String versionId;
        private String sequencer;
        private Boolean hasObjectAnnotation;

        ObjectBuilder() {
            super(S3_OBJECT);
        }

        @Override
        public void accept(ObjectBuilder b, Schema member, ShapeDeserializer d) {
            switch (member.memberIndex()) {
                case 0:
                    b.key = readString(d, member);
                    break;
                case 1:
                    b.size = readLong(d, member);
                    break;
                case 2:
                    b.eTag = readString(d, member);
                    break;
                case 3:
                    b.versionId = readString(d, member);
                    break;
                case 4:
                    b.sequencer = readString(d, member);
                    break;
                case 5:
                    b.hasObjectAnnotation = readBoolean(d, member);
                    break;
                default:
                    break;
            }
        }

        @Override
        public S3Object build() {
            return new S3Object(key, size, eTag, versionId, sequencer, hasObjectAnnotation);
        }
    }

    private static final class AnnotationBuilder extends StructBuilder<S3ObjectAnnotation, AnnotationBuilder> {
        private String name;
        private Long size;
        private String eTag;

        AnnotationBuilder() {
            super(S3_OBJECT_ANNOTATION);
        }

        @Override
        public void accept(AnnotationBuilder b, Schema member, ShapeDeserializer d) {
            switch (member.memberIndex()) {
                case 0:
                    b.name = readString(d, member);
                    break;
                case 1:
                    b.size = readLong(d, member);
                    break;
                case 2:
                    b.eTag = readString(d, member);
                    break;
                default:
                    break;
            }
        }

        @Override
        public S3ObjectAnnotation build() {
            return new S3ObjectAnnotation(name, size, eTag);
        }
    }

    private static final class GlacierEventDataBuilder extends StructBuilder<GlacierEventData, GlacierEventDataBuilder> {
        private RestoreEventData restoreEventData;

        GlacierEventDataBuilder() {
            super(GLACIER_EVENT_DATA);
        }

        @Override
        public void accept(GlacierEventDataBuilder b, Schema member, ShapeDeserializer d) {
            if (member.memberIndex() == 0) {
                b.restoreEventData = readStruct(d, member, new RestoreEventDataBuilder());
            }
        }

        @Override
        public GlacierEventData build() {
            return new GlacierEventData(restoreEventData);
        }
    }

    private static final class RestoreEventDataBuilder extends StructBuilder<RestoreEventData, RestoreEventDataBuilder> {
        private String lifecycleRestorationExpiryTime;
        private String lifecycleRestoreStorageClass;

        RestoreEventDataBuilder() {
            super(RESTORE_EVENT_DATA);
        }

        @Override
        public void accept(RestoreEventDataBuilder b, Schema member, ShapeDeserializer d) {
            switch (member.memberIndex()) {
                case 0:
                    b.lifecycleRestorationExpiryTime = readString(d, member);
                    break;
                case 1:
                    b.lifecycleRestoreStorageClass = readString(d, member);
                    break;
                default:
                    break;
            }
        }

        /**
         * The constructor parses the expiry time with {@link Instant#parse}.
         */
        @Override
        public RestoreEventData build() {
            return new RestoreEventData(lifecycleRestorationExpiryTime, lifecycleRestoreStorageClass);
        }
    }

    private static final class ReplicationEventDataBuilder
        extends StructBuilder<ReplicationEventData, ReplicationEventDataBuilder> {
        private final String[] values = new String[7];

        ReplicationEventDataBuilder() {
            super(REPLICATION_EVENT_DATA);
        }

        @Override
        public void accept(ReplicationEventDataBuilder b, Schema member, ShapeDeserializer d) {
            int index = member.memberIndex();
            if (index >= 0 && index < b.values.length) {
                b.values[index] = readString(d, member);
            }
        }

        @Override
        public ReplicationEventData build() {
            return new ReplicationEventData(values[0], values[1], values[2], values[3], values[4], values[5], values[6]);
        }
    }

    private static final class IntelligentTieringEventDataBuilder
        extends StructBuilder<IntelligentTieringEventData, IntelligentTieringEventDataBuilder> {
        private String destinationAccessTier;

        IntelligentTieringEventDataBuilder() {
            super(INTELLIGENT_TIERING_EVENT_DATA);
        }

        @Override
        public void accept(IntelligentTieringEventDataBuilder b, Schema member, ShapeDeserializer d) {
            if (member.memberIndex() == 0) {
                b.destinationAccessTier = readString(d, member);
            }
        }

        @Override
        public IntelligentTieringEventData build() {
            return new IntelligentTieringEventData(destinationAccessTier);
        }
    }

    private static final class LifecycleEventDataBuilder
        extends StructBuilder<LifecycleEventData, LifecycleEventDataBuilder> {
        private TransitionEventData transitionEventData;

        LifecycleEventDataBuilder() {
            super(LIFECYCLE_EVENT_DATA);
        }

        @Override
        public void accept(LifecycleEventDataBuilder b, Schema member, ShapeDeserializer d) {
            if (member.memberIndex() == 0) {
                b.transitionEventData = readStruct(d, member, new TransitionEventDataBuilder());
            }
        }

        @Override
        public LifecycleEventData build() {
            return new LifecycleEventData(transitionEventData);
        }
    }

    private static final class TransitionEventDataBuilder
        extends StructBuilder<TransitionEventData, TransitionEventDataBuilder> {
        private String destinationStorageClass;

        TransitionEventDataBuilder() {
            super(TRANSITION_EVENT_DATA);
        }

        @Override
        public void accept(TransitionEventDataBuilder b, Schema member, ShapeDeserializer d) {
            if (member.memberIndex() == 0) {
                b.destinationStorageClass = readString(d, member);
            }
        }

        @Override
        public TransitionEventData build() {
            return new TransitionEventData(destinationStorageClass);
        }
    }
}
