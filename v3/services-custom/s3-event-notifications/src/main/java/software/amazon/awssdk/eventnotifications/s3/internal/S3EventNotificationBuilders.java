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
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.eventnotifications.s3.model.EventHoldDuration;
import software.amazon.awssdk.eventnotifications.s3.model.GlacierEventData;
import software.amazon.awssdk.eventnotifications.s3.model.IntelligentTieringEventData;
import software.amazon.awssdk.eventnotifications.s3.model.LifecycleEventData;
import software.amazon.awssdk.eventnotifications.s3.model.ObjectRetentionEventData;
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
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;

/**
 * The Smithy {@link ShapeBuilder}s of the event notification model types: they construct the V2-shaped types (which
 * have constructors, not builders) from a {@link ShapeDeserializer}, for example a Document's, so that the types can
 * be registered in a type registry. Each builder reads the members of its schema by member index and ignores unknown
 * members; it is mutable and not thread safe.
 */
public final class S3EventNotificationBuilders {
    private S3EventNotificationBuilders() {
    }

    /** The builder base: struct reading by member index, and member-checked {@link #setMemberValue}. */
    abstract static class Base<T extends SerializableStruct>
        implements ShapeBuilder<T>, ShapeDeserializer.StructMemberConsumer<Base<T>> {
        private final Schema schema;

        Base(Schema schema) {
            this.schema = schema;
        }

        @Override
        public final Schema schema() {
            return schema;
        }

        @Override
        public final ShapeBuilder<T> deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(schema, this, this);
            return this;
        }

        @Override
        public final ShapeBuilder<T> deserializeMember(ShapeDeserializer decoder, Schema member) {
            decoder.readStruct(member.assertMemberTargetIs(schema), this, this);
            return this;
        }

        @Override
        public final void accept(Base<T> state, Schema member, ShapeDeserializer deserializer) {
            read(member.memberIndex(), member, deserializer);
        }

        @Override
        public final void setMemberValue(Schema member, Object value) {
            int index = member.memberIndex();
            List<Schema> members = schema.members();
            if (index < 0 || index >= members.size() || members.get(index) != member) {
                throw SerdeSupport.unknownMember(schema, member);
            }
            set(index, value);
        }

        /** Reads the member with index {@code index}. */
        abstract void read(int index, Schema member, ShapeDeserializer deserializer);

        /** Sets the member with index {@code index} to {@code value}. */
        abstract void set(int index, Object value);
    }

    public static ShapeBuilder<S3EventNotification> s3EventNotification() {
        return new Base<S3EventNotification>(S3EventNotificationSchemas.S3_EVENT_NOTIFICATION) {
            private List<S3EventNotificationRecord> records;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                records = SerdeSupport.readStructList(d, member, S3EventNotificationBuilders::s3EventNotificationRecord);
            }

            @Override
            @SuppressWarnings("unchecked")
            void set(int index, Object value) {
                records = (List<S3EventNotificationRecord>) value;
            }

            @Override
            public S3EventNotification build() {
                return new S3EventNotification(records);
            }
        };
    }

    public static ShapeBuilder<S3EventNotificationRecord> s3EventNotificationRecord() {
        return new Base<S3EventNotificationRecord>(S3EventNotificationSchemas.S3_EVENT_NOTIFICATION_RECORD) {
            private final S3EventNotificationRecord value = new S3EventNotificationRecord();

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                switch (index) {
                    case 0 -> value.setEventVersion(d.readString(member));
                    case 1 -> value.setEventSource(d.readString(member));
                    case 2 -> value.setAwsRegion(d.readString(member));
                    case 3 -> value.setEventTime(d.readTimestamp(member));
                    case 4 -> value.setEventName(d.readString(member));
                    case 5 -> value.setUserIdentity(struct(userIdentity(), d, member));
                    case 6 -> value.setRequestParameters(struct(requestParameters(), d, member));
                    case 7 -> value.setResponseElements(struct(responseElements(), d, member));
                    case 8 -> value.setS3(struct(s3(), d, member));
                    case 9 -> value.setGlacierEventData(struct(glacierEventData(), d, member));
                    case 10 -> value.setReplicationEventData(struct(replicationEventData(), d, member));
                    case 11 -> value.setIntelligentTieringEventData(struct(intelligentTieringEventData(), d, member));
                    case 12 -> value.setLifecycleEventData(struct(lifecycleEventData(), d, member));
                    case 13 -> value.setObjectRetentionEventData(struct(objectRetentionEventData(), d, member));
                    default -> {
                    }
                }
            }

            @Override
            void set(int index, Object v) {
                switch (index) {
                    case 0 -> value.setEventVersion((String) v);
                    case 1 -> value.setEventSource((String) v);
                    case 2 -> value.setAwsRegion((String) v);
                    case 3 -> value.setEventTime((Instant) v);
                    case 4 -> value.setEventName((String) v);
                    case 5 -> value.setUserIdentity((UserIdentity) v);
                    case 6 -> value.setRequestParameters((RequestParameters) v);
                    case 7 -> value.setResponseElements((ResponseElements) v);
                    case 8 -> value.setS3((S3) v);
                    case 9 -> value.setGlacierEventData((GlacierEventData) v);
                    case 10 -> value.setReplicationEventData((ReplicationEventData) v);
                    case 11 -> value.setIntelligentTieringEventData((IntelligentTieringEventData) v);
                    case 12 -> value.setLifecycleEventData((LifecycleEventData) v);
                    case 13 -> value.setObjectRetentionEventData((ObjectRetentionEventData) v);
                    default -> {
                    }
                }
            }

            @Override
            public S3EventNotificationRecord build() {
                S3EventNotificationRecord copy = new S3EventNotificationRecord();
                copy.setEventVersion(value.getEventVersion());
                copy.setEventSource(value.getEventSource());
                copy.setAwsRegion(value.getAwsRegion());
                copy.setEventTime(value.getEventTime());
                copy.setEventName(value.getEventName());
                copy.setUserIdentity(value.getUserIdentity());
                copy.setRequestParameters(value.getRequestParameters());
                copy.setResponseElements(value.getResponseElements());
                copy.setS3(value.getS3());
                copy.setGlacierEventData(value.getGlacierEventData());
                copy.setReplicationEventData(value.getReplicationEventData());
                copy.setIntelligentTieringEventData(value.getIntelligentTieringEventData());
                copy.setLifecycleEventData(value.getLifecycleEventData());
                copy.setObjectRetentionEventData(value.getObjectRetentionEventData());
                return copy;
            }
        };
    }

    public static ShapeBuilder<UserIdentity> userIdentity() {
        return new Base<UserIdentity>(S3EventNotificationSchemas.USER_IDENTITY) {
            private String principalId;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                principalId = d.readString(member);
            }

            @Override
            void set(int index, Object value) {
                principalId = (String) value;
            }

            @Override
            public UserIdentity build() {
                return new UserIdentity(principalId);
            }
        };
    }

    public static ShapeBuilder<RequestParameters> requestParameters() {
        return new Base<RequestParameters>(S3EventNotificationSchemas.REQUEST_PARAMETERS) {
            private String sourceIpAddress;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                sourceIpAddress = d.readString(member);
            }

            @Override
            void set(int index, Object value) {
                sourceIpAddress = (String) value;
            }

            @Override
            public RequestParameters build() {
                return new RequestParameters(sourceIpAddress);
            }
        };
    }

    public static ShapeBuilder<ResponseElements> responseElements() {
        return new Base<ResponseElements>(S3EventNotificationSchemas.RESPONSE_ELEMENTS) {
            private final String[] values = new String[2];

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                values[index] = d.readString(member);
            }

            @Override
            void set(int index, Object value) {
                values[index] = (String) value;
            }

            @Override
            public ResponseElements build() {
                // Members: xAmzRequestId, xAmzId2; constructor: (xAmzId2, xAmzRequestId).
                return new ResponseElements(values[1], values[0]);
            }
        };
    }

    public static ShapeBuilder<S3> s3() {
        return new Base<S3>(S3EventNotificationSchemas.S3) {
            private String s3SchemaVersion;
            private String configurationId;
            private S3Bucket bucket;
            private S3Object object;
            private List<S3ObjectAnnotation> objectAnnotation;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                switch (index) {
                    case 0 -> s3SchemaVersion = d.readString(member);
                    case 1 -> configurationId = d.readString(member);
                    case 2 -> bucket = struct(s3Bucket(), d, member);
                    case 3 -> object = struct(s3Object(), d, member);
                    case 4 -> objectAnnotation =
                        SerdeSupport.readStructList(d, member, S3EventNotificationBuilders::s3ObjectAnnotation);
                    default -> {
                    }
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            void set(int index, Object value) {
                switch (index) {
                    case 0 -> s3SchemaVersion = (String) value;
                    case 1 -> configurationId = (String) value;
                    case 2 -> bucket = (S3Bucket) value;
                    case 3 -> object = (S3Object) value;
                    case 4 -> objectAnnotation = (List<S3ObjectAnnotation>) value;
                    default -> {
                    }
                }
            }

            @Override
            public S3 build() {
                return new S3(configurationId, bucket, object, s3SchemaVersion, objectAnnotation);
            }
        };
    }

    public static ShapeBuilder<S3Bucket> s3Bucket() {
        return new Base<S3Bucket>(S3EventNotificationSchemas.S3_BUCKET) {
            private String name;
            private UserIdentity ownerIdentity;
            private String arn;
            private Map<String, String> awsGeneratedTags;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                switch (index) {
                    case 0 -> name = d.readString(member);
                    case 1 -> ownerIdentity = struct(userIdentity(), d, member);
                    case 2 -> arn = d.readString(member);
                    case 3 -> awsGeneratedTags = SerdeSupport.readStringMap(d, member);
                    default -> {
                    }
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            void set(int index, Object value) {
                switch (index) {
                    case 0 -> name = (String) value;
                    case 1 -> ownerIdentity = (UserIdentity) value;
                    case 2 -> arn = (String) value;
                    case 3 -> awsGeneratedTags = (Map<String, String>) value;
                    default -> {
                    }
                }
            }

            @Override
            public S3Bucket build() {
                return new S3Bucket(name, ownerIdentity, arn, awsGeneratedTags);
            }
        };
    }

    public static ShapeBuilder<S3Object> s3Object() {
        return new Base<S3Object>(S3EventNotificationSchemas.S3_OBJECT) {
            private final Object[] values = new Object[6];

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                values[index] = switch (index) {
                    case 1 -> d.readLong(member);
                    case 5 -> d.readBoolean(member);
                    default -> d.readString(member);
                };
            }

            @Override
            void set(int index, Object value) {
                values[index] = value;
            }

            @Override
            public S3Object build() {
                return new S3Object((String) values[0], (Long) values[1], (String) values[2], (String) values[3],
                                    (String) values[4], (Boolean) values[5]);
            }
        };
    }

    public static ShapeBuilder<S3ObjectAnnotation> s3ObjectAnnotation() {
        return new Base<S3ObjectAnnotation>(S3EventNotificationSchemas.S3_OBJECT_ANNOTATION) {
            private final Object[] values = new Object[3];

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                values[index] = index == 1 ? d.readLong(member) : d.readString(member);
            }

            @Override
            void set(int index, Object value) {
                values[index] = value;
            }

            @Override
            public S3ObjectAnnotation build() {
                return new S3ObjectAnnotation((String) values[0], (Long) values[1], (String) values[2]);
            }
        };
    }

    public static ShapeBuilder<GlacierEventData> glacierEventData() {
        return new Base<GlacierEventData>(S3EventNotificationSchemas.GLACIER_EVENT_DATA) {
            private RestoreEventData restoreEventData;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                restoreEventData = struct(restoreEventData(), d, member);
            }

            @Override
            void set(int index, Object value) {
                restoreEventData = (RestoreEventData) value;
            }

            @Override
            public GlacierEventData build() {
                return new GlacierEventData(restoreEventData);
            }
        };
    }

    public static ShapeBuilder<RestoreEventData> restoreEventData() {
        return new Base<RestoreEventData>(S3EventNotificationSchemas.RESTORE_EVENT_DATA) {
            private Instant lifecycleRestorationExpiryTime;
            private String lifecycleRestoreStorageClass;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                if (index == 0) {
                    lifecycleRestorationExpiryTime = d.readTimestamp(member);
                } else {
                    lifecycleRestoreStorageClass = d.readString(member);
                }
            }

            @Override
            void set(int index, Object value) {
                if (index == 0) {
                    lifecycleRestorationExpiryTime = (Instant) value;
                } else {
                    lifecycleRestoreStorageClass = (String) value;
                }
            }

            @Override
            public RestoreEventData build() {
                // The V2 constructor takes the ISO-8601 text; ISO_INSTANT renders every Instant so that it parses back
                // to the same Instant.
                String expiry = lifecycleRestorationExpiryTime == null
                                ? null
                                : DateTimeFormatter.ISO_INSTANT.format(lifecycleRestorationExpiryTime);
                return new RestoreEventData(expiry, lifecycleRestoreStorageClass);
            }
        };
    }

    public static ShapeBuilder<ReplicationEventData> replicationEventData() {
        return new Base<ReplicationEventData>(S3EventNotificationSchemas.REPLICATION_EVENT_DATA) {
            private final String[] values = new String[7];

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                values[index] = d.readString(member);
            }

            @Override
            void set(int index, Object value) {
                values[index] = (String) value;
            }

            @Override
            public ReplicationEventData build() {
                return new ReplicationEventData(values[0], values[1], values[2], values[3], values[4], values[5],
                                                values[6]);
            }
        };
    }

    public static ShapeBuilder<IntelligentTieringEventData> intelligentTieringEventData() {
        return new Base<IntelligentTieringEventData>(S3EventNotificationSchemas.INTELLIGENT_TIERING_EVENT_DATA) {
            private String destinationAccessTier;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                destinationAccessTier = d.readString(member);
            }

            @Override
            void set(int index, Object value) {
                destinationAccessTier = (String) value;
            }

            @Override
            public IntelligentTieringEventData build() {
                return new IntelligentTieringEventData(destinationAccessTier);
            }
        };
    }

    public static ShapeBuilder<LifecycleEventData> lifecycleEventData() {
        return new Base<LifecycleEventData>(S3EventNotificationSchemas.LIFECYCLE_EVENT_DATA) {
            private TransitionEventData transitionEventData;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                transitionEventData = struct(transitionEventData(), d, member);
            }

            @Override
            void set(int index, Object value) {
                transitionEventData = (TransitionEventData) value;
            }

            @Override
            public LifecycleEventData build() {
                return new LifecycleEventData(transitionEventData);
            }
        };
    }

    public static ShapeBuilder<TransitionEventData> transitionEventData() {
        return new Base<TransitionEventData>(S3EventNotificationSchemas.TRANSITION_EVENT_DATA) {
            private String destinationStorageClass;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                destinationStorageClass = d.readString(member);
            }

            @Override
            void set(int index, Object value) {
                destinationStorageClass = (String) value;
            }

            @Override
            public TransitionEventData build() {
                return new TransitionEventData(destinationStorageClass);
            }
        };
    }

    public static ShapeBuilder<ObjectRetentionEventData> objectRetentionEventData() {
        return new Base<ObjectRetentionEventData>(S3EventNotificationSchemas.OBJECT_RETENTION_EVENT_DATA) {
            private final String[] strings = new String[3];
            private EventHoldDuration eventHoldDuration;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                if (index == 3) {
                    eventHoldDuration = struct(eventHoldDuration(), d, member);
                } else {
                    strings[index] = d.readString(member);
                }
            }

            @Override
            void set(int index, Object value) {
                if (index == 3) {
                    eventHoldDuration = (EventHoldDuration) value;
                } else {
                    strings[index] = (String) value;
                }
            }

            @Override
            public ObjectRetentionEventData build() {
                return new ObjectRetentionEventData(strings[0], strings[1], strings[2], eventHoldDuration);
            }
        };
    }

    public static ShapeBuilder<EventHoldDuration> eventHoldDuration() {
        return new Base<EventHoldDuration>(S3EventNotificationSchemas.EVENT_HOLD_DURATION) {
            private String unit;
            private Long value;

            @Override
            void read(int index, Schema member, ShapeDeserializer d) {
                if (index == 0) {
                    unit = d.readString(member);
                } else {
                    value = d.readLong(member);
                }
            }

            @Override
            void set(int index, Object v) {
                if (index == 0) {
                    unit = (String) v;
                } else {
                    value = (Long) v;
                }
            }

            @Override
            public EventHoldDuration build() {
                return new EventHoldDuration(unit, value);
            }
        };
    }

    private static <T extends SerializableStruct> T struct(ShapeBuilder<T> builder, ShapeDeserializer deserializer,
                                                         Schema member) {
        return builder.deserializeMember(deserializer, member).build();
    }
}
