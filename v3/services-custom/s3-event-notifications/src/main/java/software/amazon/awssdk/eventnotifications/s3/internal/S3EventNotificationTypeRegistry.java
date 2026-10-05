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

import java.util.List;
import java.util.function.Supplier;
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
import software.amazon.awssdk.smithy.runtime.registry.RegisteredType;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * The primary type registry of the S3 event notification shapes: every structure, mapped to its schema, Java class
 * and builder factory (Requirements 6.5-6.7).
 *
 * <p>The registry is built on first use from the shape IDs alone (which loads no model type); its one partition
 * creates the entries, and so initializes the model types, on the first lookup of one of them.
 */
public final class S3EventNotificationTypeRegistry {
    private S3EventNotificationTypeRegistry() {
    }

    /** The registry, built on first use. */
    public static SchemaTypeRegistry registry() {
        return Holder.REGISTRY;
    }

    /** The registered shape IDs: every structure schema of the namespace. */
    static List<ShapeId> shapeIds() {
        return S3EventNotificationSchemas.all().stream()
                                         .filter(s -> s.type() == ShapeType.STRUCTURE)
                                         .map(Schema::id)
                                         .toList();
    }

    private static List<RegisteredType<?>> entries() {
        return List.of(
            entry(EventHoldDuration.SCHEMA, EventHoldDuration.class, S3EventNotificationBuilders::eventHoldDuration),
            entry(GlacierEventData.SCHEMA, GlacierEventData.class, S3EventNotificationBuilders::glacierEventData),
            entry(IntelligentTieringEventData.SCHEMA, IntelligentTieringEventData.class,
                  S3EventNotificationBuilders::intelligentTieringEventData),
            entry(LifecycleEventData.SCHEMA, LifecycleEventData.class, S3EventNotificationBuilders::lifecycleEventData),
            entry(ObjectRetentionEventData.SCHEMA, ObjectRetentionEventData.class,
                  S3EventNotificationBuilders::objectRetentionEventData),
            entry(ReplicationEventData.SCHEMA, ReplicationEventData.class,
                  S3EventNotificationBuilders::replicationEventData),
            entry(RequestParameters.SCHEMA, RequestParameters.class, S3EventNotificationBuilders::requestParameters),
            entry(ResponseElements.SCHEMA, ResponseElements.class, S3EventNotificationBuilders::responseElements),
            entry(RestoreEventData.SCHEMA, RestoreEventData.class, S3EventNotificationBuilders::restoreEventData),
            entry(S3.SCHEMA, S3.class, S3EventNotificationBuilders::s3),
            entry(S3Bucket.SCHEMA, S3Bucket.class, S3EventNotificationBuilders::s3Bucket),
            entry(S3EventNotification.SCHEMA, S3EventNotification.class,
                  S3EventNotificationBuilders::s3EventNotification),
            entry(S3EventNotificationRecord.SCHEMA, S3EventNotificationRecord.class,
                  S3EventNotificationBuilders::s3EventNotificationRecord),
            entry(S3Object.SCHEMA, S3Object.class, S3EventNotificationBuilders::s3Object),
            entry(S3ObjectAnnotation.SCHEMA, S3ObjectAnnotation.class, S3EventNotificationBuilders::s3ObjectAnnotation),
            entry(TransitionEventData.SCHEMA, TransitionEventData.class,
                  S3EventNotificationBuilders::transitionEventData),
            entry(UserIdentity.SCHEMA, UserIdentity.class, S3EventNotificationBuilders::userIdentity));
    }

    private static <T extends SerializableStruct> RegisteredType<T> entry(
        Schema schema, Class<T> type, Supplier<ShapeBuilder<T>> builder) {
        return RegisteredType.of(schema, type, builder);
    }

    /** Initialization-on-demand holder of the registry. */
    private static final class Holder {
        static final SchemaTypeRegistry REGISTRY = SchemaTypeRegistry.builder()
            .partition(shapeIds(), S3EventNotificationTypeRegistry::entries)
            .build();

        private Holder() {
        }
    }
}
