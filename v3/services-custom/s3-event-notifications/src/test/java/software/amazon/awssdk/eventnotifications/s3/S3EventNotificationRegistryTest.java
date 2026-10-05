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

package software.amazon.awssdk.eventnotifications.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationBuilders;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3Bucket;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.eventnotifications.s3.model.S3Object;
import software.amazon.awssdk.eventnotifications.s3.model.S3ObjectAnnotation;
import software.amazon.awssdk.eventnotifications.s3.model.UserIdentity;
import software.amazon.awssdk.services.s3.S3ServiceMetadata;
import software.amazon.awssdk.services.s3.model.Owner;
import software.amazon.awssdk.smithy.runtime.document.SchemaDocuments;
import software.amazon.awssdk.smithy.runtime.registry.MissingDiscriminatorException;
import software.amazon.awssdk.smithy.runtime.registry.RegisteredType;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.smithy.runtime.registry.TypeRegistryConflictException;
import software.amazon.awssdk.smithy.runtime.registry.UnknownDiscriminatorException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * The primary type registry of the event notification types and its composition with the generated S3 registry
 * (Requirements 6.5-6.9, 6.11-6.13; design "Service metadata, operation descriptors, and registry").
 */
class S3EventNotificationRegistryTest {
    private static final String MODEL_PACKAGE = "software.amazon.awssdk.eventnotifications.s3.model.";
    private static final List<String> MODEL_TYPES = List.of(
        "EventHoldDuration", "GlacierEventData", "IntelligentTieringEventData", "LifecycleEventData",
        "ObjectRetentionEventData", "ReplicationEventData", "RequestParameters", "ResponseElements",
        "RestoreEventData", "S3", "S3Bucket", "S3EventNotification", "S3EventNotificationRecord", "S3Object",
        "S3ObjectAnnotation", "TransitionEventData", "UserIdentity");

    @Test
    void typeRegistry_whenInspected_registersEveryModelTypeWithItsOwnSchemaClassAndBuilder() throws Exception {
        SchemaTypeRegistry registry = S3EventNotificationMetadata.typeRegistry();
        Set<ShapeId> expected = new TreeSet<>();
        for (String name : MODEL_TYPES) {
            Class<?> type = Class.forName(MODEL_PACKAGE + name);
            Schema schema = (Schema) type.getField("SCHEMA").get(null);
            ShapeId id = (ShapeId) type.getField("ID").get(null);
            expected.add(id);

            assertThat(schema.id()).isEqualTo(id);
            assertThat(id).isEqualTo(ShapeId.fromParts(S3EventNotificationMetadata.namespace(), name));
            RegisteredType<?> entry = registry.entry(id).orElseThrow();
            assertThat(entry.schema()).isSameAs(schema);
            assertThat(entry.type()).isSameAs(type);
            assertThat(registry.getShapeClass(id)).isSameAs(type);
            ShapeBuilder<?> builder = registry.createBuilder(id);
            assertThat(builder.schema()).isSameAs(schema);
            assertThat(builder.build()).isInstanceOf(type);
            assertThat(registry.createBuilder(id)).isNotSameAs(builder);
        }
        assertThat(registry.shapeIds()).containsExactlyElementsOf(expected);
    }

    @Test
    void typeRegistry_whenCalledTwice_isTheSameRegistry() {
        assertThat(S3EventNotificationMetadata.typeRegistry()).isSameAs(S3EventNotificationMetadata.typeRegistry());
    }

    @Test
    void schemas_whenListed_coverTheNamespaceSortedAndResolveById() {
        List<Schema> schemas = S3EventNotificationMetadata.schemas();
        assertThat(schemas).extracting(Schema::id).isSorted();
        assertThat(schemas).allMatch(s -> s.id().getNamespace().equals(S3EventNotificationMetadata.namespace()));
        for (Schema schema : schemas) {
            assertThat(S3EventNotificationMetadata.schema(schema.id())).containsSame(schema);
        }
        assertThat(schemas.stream().filter(s -> s.type() == ShapeType.STRUCTURE).map(Schema::id))
            .containsExactlyElementsOf(S3EventNotificationMetadata.typeRegistry().shapeIds());
        assertThat(S3EventNotificationMetadata.schema(Owner.ID)).isEmpty();
        assertThat(S3EventNotificationMetadata.schema(null)).isEmpty();
    }

    @Test
    void compose_withS3ServiceRegistry_registersTheUnionAndPreservesEntryIdentity() {
        SchemaTypeRegistry s3 = S3ServiceMetadata.typeRegistry();
        SchemaTypeRegistry events = S3EventNotificationMetadata.typeRegistry();

        SchemaTypeRegistry combined = SchemaTypeRegistry.compose(s3, events);

        assertThat(combined.shapeIds()).hasSize(s3.shapeIds().size() + events.shapeIds().size());
        assertThat(combined.shapeIds()).containsAll(s3.shapeIds()).containsAll(events.shapeIds());
        for (ShapeId id : events.shapeIds()) {
            assertThat(combined.entry(id).orElseThrow()).isSameAs(events.entry(id).orElseThrow());
        }
        assertThat(combined.entry(Owner.ID).orElseThrow()).isSameAs(s3.entry(Owner.ID).orElseThrow());
        assertThat(combined.schema(S3Object.ID)).containsSame(S3Object.SCHEMA);
        // The S3 service's own Object shape is a different type with a different ID.
        assertThat(combined.getShapeClass(software.amazon.awssdk.services.s3.model.S3Object.ID))
            .isSameAs(software.amazon.awssdk.services.s3.model.S3Object.class);
    }

    @Test
    void compose_inEitherOrder_resolvesTheSameObjects() {
        SchemaTypeRegistry first = SchemaTypeRegistry.compose(S3ServiceMetadata.typeRegistry(),
                                                              S3EventNotificationMetadata.typeRegistry());
        SchemaTypeRegistry second = SchemaTypeRegistry.compose(S3EventNotificationMetadata.typeRegistry(),
                                                               S3ServiceMetadata.typeRegistry());
        assertThat(second.shapeIds()).isEqualTo(first.shapeIds());
        for (ShapeId id : List.of(S3EventNotification.ID, S3Bucket.ID, Owner.ID)) {
            assertThat(second.entry(id).orElseThrow()).isSameAs(first.entry(id).orElseThrow());
        }
    }

    @Test
    void deserialize_whenDocumentIsDiscriminated_constructsTheRegisteredTypeOfEitherRegistry() {
        SchemaTypeRegistry combined = SchemaTypeRegistry.compose(S3ServiceMetadata.typeRegistry(),
                                                                 S3EventNotificationMetadata.typeRegistry());
        S3EventNotification event = sampleEvent();
        Owner owner = Owner.builder().id("owner-id").displayName("owner").build();

        SerializableStruct typedEvent = combined.deserialize(SchemaDocuments.of(event));
        SerializableStruct typedOwner = combined.deserialize(SchemaDocuments.of(owner));

        assertThat(typedEvent).isInstanceOf(S3EventNotification.class).isEqualTo(event);
        assertThat(typedOwner).isInstanceOf(Owner.class).isEqualTo(owner);
        assertThat(combined.deserializeStrict(SchemaDocuments.of(event))).isEqualTo(event);
    }

    @Test
    void deserialize_whenDocumentHasNoDiscriminator_failsNamingTheMissingDiscriminator() {
        Document untyped = Document.of(Map.of("Records", Document.of(List.of())));
        assertThatThrownBy(() -> S3EventNotificationMetadata.typeRegistry().deserialize(untyped))
            .isInstanceOf(MissingDiscriminatorException.class);
    }

    @Test
    void deserialize_whenDiscriminatorIsNotRegistered_failsNamingTheShapeId() {
        Document owner = SchemaDocuments.of(Owner.builder().id("x").build());
        assertThatThrownBy(() -> S3EventNotificationMetadata.typeRegistry().deserialize(owner))
            .isInstanceOf(UnknownDiscriminatorException.class)
            .hasMessageContaining(Owner.ID.toString());
    }

    @Test
    void compose_whenAnotherRegistryRegistersADifferentEntryForAnEventShape_failsDeterministically() {
        // The same schema and class, but another builder factory: a different entry for the same shape ID.
        SchemaTypeRegistry impostor = SchemaTypeRegistry.builder()
            .partition(List.of(S3EventNotification.ID),
                       () -> List.of(RegisteredType.of(S3EventNotification.SCHEMA, S3EventNotification.class,
                                                       () -> S3EventNotificationBuilders.s3EventNotification())))
            .build();

        List<String> messages = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> SchemaTypeRegistry.compose(S3ServiceMetadata.typeRegistry(),
                                                                 S3EventNotificationMetadata.typeRegistry(), impostor))
                .isInstanceOf(TypeRegistryConflictException.class)
                .hasMessageContaining(S3EventNotification.ID.toString())
                .satisfies(e -> messages.add(e.getMessage()));
        }
        assertThat(messages.get(1)).isEqualTo(messages.get(0));
    }

    @Test
    void compose_withItselfOrTheSameEntries_isAccepted() {
        SchemaTypeRegistry events = S3EventNotificationMetadata.typeRegistry();
        SchemaTypeRegistry twice = SchemaTypeRegistry.compose(events, events);
        assertThat(twice.shapeIds()).isEqualTo(events.shapeIds());
        assertThat(twice.entry(S3EventNotificationRecord.ID).orElseThrow())
            .isSameAs(events.entry(S3EventNotificationRecord.ID).orElseThrow());
    }

    @Test
    void schemaDocument_whenConverted_keepsDiscriminatorsAndMemberPresence() {
        S3EventNotification event = sampleEvent();
        Document document = SchemaDocuments.of(event);

        assertThat(document.discriminator()).isEqualTo(S3EventNotification.ID);
        Document rec = document.getMember("Records").asList().get(0);
        assertThat(rec.discriminator()).isEqualTo(S3EventNotificationRecord.ID);
        assertThat(rec.getMember("eventTime").asTimestamp()).isEqualTo(Instant.parse("2024-01-02T03:04:05.678Z"));
        assertThat(rec.getMember("glacierEventData")).isNull();
        assertThat(rec.getMember("s3").getMember("bucket").getMember("awsGeneratedTags").asStringMap().keySet())
            .containsExactly("aws:z", "aws:a");
        assertThat(document.getMember("Records").asList().get(1)).isNull();
    }

    private static S3EventNotification sampleEvent() {
        S3EventNotificationRecord rec = new S3EventNotificationRecord();
        rec.setEventVersion("2.1");
        rec.setEventSource("aws:s3");
        rec.setEventName("ObjectCreated:Put");
        rec.setEventTime(Instant.parse("2024-01-02T03:04:05.678Z"));
        rec.setUserIdentity(new UserIdentity("AIDAJDPLRKLG7UEXAMPLE"));
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("aws:z", "1");
        tags.put("aws:a", null);
        rec.setS3(new S3("config", new S3Bucket("bucket", new UserIdentity("owner"), "arn:aws:s3:::bucket", tags),
                         new S3Object("key", 1024L, "etag", null, "seq", true), "1.0",
                         Arrays.asList(null, new S3ObjectAnnotation("a", 1L, "e"))));
        return new S3EventNotification(Arrays.asList(rec, null));
    }
}
