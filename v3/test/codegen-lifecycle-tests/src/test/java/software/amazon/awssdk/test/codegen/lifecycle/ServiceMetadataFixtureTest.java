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

package software.amazon.awssdk.test.codegen.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.protocols.core.InstalledProtocols;
import software.amazon.awssdk.protocols.core.ModeledProtocol;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.awssdk.protocols.core.ProtocolSelectionException;
import software.amazon.awssdk.protocols.core.ProtocolSelector;
import software.amazon.awssdk.protocols.core.SelectedProtocol;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.awssdk.smithy.runtime.registry.RegisteredType;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.test.codegen.lifecycle.internal.operation.CodegenLifecycleFixtureOperations;
import software.amazon.awssdk.test.codegen.lifecycle.model.Color;
import software.amazon.awssdk.test.codegen.lifecycle.model.GetStatusResponse;
import software.amazon.awssdk.test.codegen.lifecycle.model.GetWidgetRequest;
import software.amazon.awssdk.test.codegen.lifecycle.model.GetWidgetResponse;
import software.amazon.awssdk.test.codegen.lifecycle.model.Limits;
import software.amazon.awssdk.test.codegen.lifecycle.model.PingRequest;
import software.amazon.awssdk.test.codegen.lifecycle.model.PingResponse;
import software.amazon.awssdk.test.codegen.lifecycle.model.ServiceUnavailableException;
import software.amazon.awssdk.test.codegen.lifecycle.model.UploadRequest;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetAnnotation;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetDetails;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetNotFoundException;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetShape;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetTree;
import software.amazon.awssdk.test.codegen.lifecycle.model.internal.CodegenLifecycleFixtureSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * The generated {@code CodegenLifecycleFixtureServiceMetadata} and primary type registry, compiled and run against
 * the V3 runtime (task 3.3; Requirements 4.6, 6.5-6.7, 6.9): exact service identity, the operation table's
 * descriptors, and one registry entry per structure, union and error mapping its exact shape ID to the type's own
 * schema object, class and a working builder, including recursive structures, unions, unknown enum values and errors.
 */
class ServiceMetadataFixtureTest {

    private static final String NAMESPACE = "software.amazon.awssdk.test.codegen.lifecycle";
    private static final ShapeId SERVICE = id("CodegenLifecycleFixture");
    private static final JsonCodec JSON = JsonCodec.builder().build();
    private static final SchemaTypeRegistry REGISTRY = CodegenLifecycleFixtureServiceMetadata.typeRegistry();

    private static ShapeId id(String relative) {
        return ShapeId.from(NAMESPACE + "#" + relative);
    }

    @Test
    void metadataExposesTheExactServiceIdentity() {
        assertEquals(SERVICE, CodegenLifecycleFixtureServiceMetadata.serviceId());
        assertEquals(CodegenLifecycleFixture.serviceShapeId(), CodegenLifecycleFixtureServiceMetadata.serviceId()
                                                                                                     .toString());
        assertEquals(Optional.empty(), CodegenLifecycleFixtureServiceMetadata.sdkId());
        assertEquals("2026-01-01", CodegenLifecycleFixtureServiceMetadata.version());
        assertEquals(List.of(ShapeId.from("aws.protocols#awsJson1_0")),
                     CodegenLifecycleFixtureServiceMetadata.protocols());
        assertEquals(List.of(ShapeId.from("aws.auth#sigv4")), CodegenLifecycleFixtureServiceMetadata.authSchemes());
        assertSame(CodegenLifecycleFixtureSchemas.CODEGEN_LIFECYCLE_FIXTURE,
                   CodegenLifecycleFixtureServiceMetadata.serviceSchema());
        assertSame(CodegenLifecycleFixture.schemaIndex(), CodegenLifecycleFixtureServiceMetadata.schemaIndex());
        assertEquals(CodegenLifecycleFixture.provenance(), CodegenLifecycleFixtureServiceMetadata.provenance());
    }

    /**
     * The generated protocol declaration compiles against protocol-core and drives protocol selection (task 4.1;
     * Requirements 5.6, 5.12).
     */
    @Test
    void metadataDeclaresTheModeledProtocolsForSelection() {
        ServiceProtocols declared = CodegenLifecycleFixtureServiceMetadata.serviceProtocols();
        assertSame(declared, CodegenLifecycleFixtureServiceMetadata.serviceProtocols());
        assertSame(CodegenLifecycleFixtureServiceMetadata.serviceSchema(), declared.serviceSchema());
        assertEquals(SERVICE, declared.serviceId());
        assertEquals(CodegenLifecycleFixtureServiceMetadata.protocols(), declared.protocolIds());
        assertEquals(List.of(ModeledProtocol.of(ProtocolIds.AWS_JSON_1_0)), List.copyOf(declared.protocols()));
        assertFalse(declared.awsQueryCompatible());
        assertTrue(declared.awsQueryErrors().isEmpty());

        ProtocolProvider awsJson = new ProtocolProvider() {
            @Override
            public ShapeId protocolId() {
                return ProtocolIds.AWS_JSON_1_0;
            }

            @Override
            public boolean usesHttpBindingTraits() {
                return false;
            }
        };
        SelectedProtocol selected = ProtocolSelector.select(declared, InstalledProtocols.of(awsJson));
        assertSame(awsJson, selected.provider());
        assertEquals(Optional.of("1.0"), selected.declaration().awsJsonVersion());
        ProtocolSelectionException undeclared = assertThrows(ProtocolSelectionException.class, () ->
            ProtocolSelector.select(declared, InstalledProtocols.of(awsJson), ProtocolIds.REST_JSON_1));
        assertEquals(ProtocolSelectionException.Reason.NOT_DECLARED, undeclared.reason());
        ProtocolSelectionException unavailable = assertThrows(ProtocolSelectionException.class, () ->
            ProtocolSelector.select(declared, InstalledProtocols.none()));
        assertEquals(ProtocolSelectionException.Reason.NO_INSTALLED_DECLARED_PROTOCOL, unavailable.reason());
    }

    @Test
    void metadataOperationsAreTheStaticTableConstants() {
        assertSame(CodegenLifecycleFixtureOperations.operations(), CodegenLifecycleFixtureServiceMetadata.operations());
        for (AwsOperationDescriptor operation : CodegenLifecycleFixtureOperations.operations()) {
            assertSame(operation, CodegenLifecycleFixtureServiceMetadata.operation(operation.id()).orElseThrow());
        }
        assertEquals(Optional.empty(), CodegenLifecycleFixtureServiceMetadata.operation(id("Unknown")));
        assertEquals(Optional.empty(), CodegenLifecycleFixtureServiceMetadata.operation(id("WidgetTree")),
                     "a type ID is not an operation ID");
    }

    @Test
    void thereIsExactlyOnePrimaryRegistry() {
        assertSame(REGISTRY, CodegenLifecycleFixtureServiceMetadata.typeRegistry());
    }

    @Test
    void registryContainsExactlyEveryStructureUnionAndErrorOfTheService() {
        List<ShapeId> aggregates = CodegenLifecycleFixture.schemaIndex().types().stream()
                                                          .filter(s -> s.type() == ShapeType.STRUCTURE
                                                                       || s.type() == ShapeType.UNION)
                                                          .map(Schema::id)
                                                          .toList();
        assertEquals(List.of(id("GetStatusOutput"), id("GetWidgetInput"), id("GetWidgetOutput"), id("Limits"),
                             id("PingInput"), id("PingOutput"), id("ServiceUnavailable"), id("UploadInput"),
                             id("WidgetAnnotation"), id("WidgetDetails"), id("WidgetNotFound"), id("WidgetShape"),
                             id("WidgetTree")),
                     aggregates);
        assertEquals(aggregates, List.copyOf(REGISTRY.shapeIds()));
        assertEquals(aggregates, REGISTRY.entries().stream().map(RegisteredType::shapeId).toList());
        assertFalse(REGISTRY.contains(id("Color")), "enums have no builder and are not registered");
        assertFalse(REGISTRY.contains(id("WidgetTreeList")));
        assertFalse(REGISTRY.contains(ShapeId.from("smithy.api#Unit")));
        assertFalse(REGISTRY.contains(SERVICE));
        assertFalse(REGISTRY.contains(id("GetWidget")), "operations are not types");
        assertFalse(REGISTRY.contains(id("WidgetTree$parent")));
    }

    @Test
    void everyEntryMapsItsExactIdToTheTypesSchemaClassAndBuilder() {
        assertEntry(GetStatusResponse.ID, GetStatusResponse.SCHEMA, GetStatusResponse.class);
        assertEntry(GetWidgetRequest.ID, GetWidgetRequest.SCHEMA, GetWidgetRequest.class);
        assertEntry(GetWidgetResponse.ID, GetWidgetResponse.SCHEMA, GetWidgetResponse.class);
        assertEntry(Limits.ID, Limits.SCHEMA, Limits.class);
        assertEntry(PingRequest.ID, PingRequest.SCHEMA, PingRequest.class);
        assertEntry(PingResponse.ID, PingResponse.SCHEMA, PingResponse.class);
        assertEntry(ServiceUnavailableException.ID, ServiceUnavailableException.SCHEMA,
                    ServiceUnavailableException.class);
        assertEntry(UploadRequest.ID, UploadRequest.SCHEMA, UploadRequest.class);
        assertEntry(WidgetAnnotation.ID, WidgetAnnotation.SCHEMA, WidgetAnnotation.class);
        assertEntry(WidgetDetails.ID, WidgetDetails.SCHEMA, WidgetDetails.class);
        assertEntry(WidgetNotFoundException.ID, WidgetNotFoundException.SCHEMA, WidgetNotFoundException.class);
        assertEntry(WidgetShape.ID, WidgetShape.SCHEMA, WidgetShape.class);
        assertEntry(WidgetTree.ID, WidgetTree.SCHEMA, WidgetTree.class);
        assertEquals(13, REGISTRY.entries().size());
    }

    private static void assertEntry(ShapeId id, Schema schema, Class<? extends SerializableStruct> type) {
        RegisteredType<?> entry = REGISTRY.entry(id).orElseThrow();
        assertSame(entry, REGISTRY.entry(id).orElseThrow(), "entries are singletons");
        assertEquals(id, entry.shapeId());
        assertSame(schema, entry.schema());
        assertSame(schema, REGISTRY.schema(id).orElseThrow());
        assertSame(type, entry.type());
        assertSame(type, REGISTRY.getShapeClass(id));
        ShapeBuilder<?> first = REGISTRY.createBuilder(id);
        ShapeBuilder<?> second = entry.createBuilder();
        assertNotSame(first, second, "each call creates a new builder");
        assertSame(schema, first.schema());
    }

    @Test
    void unknownIdsAreAbsent() {
        assertEquals(Optional.empty(), REGISTRY.entry(id("Missing")));
        assertNull(REGISTRY.getShapeClass(id("Missing")));
        assertNull(REGISTRY.createBuilder(id("Missing")));
        assertEquals(Optional.empty(), REGISTRY.entry(ShapeId.from("other.namespace#WidgetTree")));
    }

    @Test
    void recursiveStructureIsConstructedFromItsDiscriminatedDocument() {
        WidgetTree root = WidgetTree.builder().label("root").build();
        WidgetTree tree = WidgetTree.builder()
                                    .label("mid")
                                    .parent(root)
                                    .children(c -> c.label("leaf").parent(root), c -> c.label("other"))
                                    .build();
        Document document = Document.of(tree);
        assertEquals(WidgetTree.ID, document.discriminator());
        assertEquals(tree, assertInstanceOf(WidgetTree.class, REGISTRY.deserialize(document)));
    }

    /**
     * Smithy Java 1.6.1 gives union documents no discriminator (exact discriminators for every document kind are task
     * 4.4), so the union is constructed through the builder registered for its exact ID.
     */
    @Test
    void unionIsConstructedThroughItsRegisteredBuilder() {
        for (WidgetShape shape : List.of(WidgetShape.fromNested(WidgetShape.fromTree(t -> t.label("inner"))),
                                         WidgetShape.fromCircle(2.5))) {
            ShapeBuilder<WidgetShape> builder = REGISTRY.createBuilder(WidgetShape.ID, WidgetShape.class);
            Document.of(shape).deserializeInto(builder);
            assertEquals(shape, builder.build());
        }
    }

    @Test
    void registeredBuilderKeepsUnknownEnumValues() {
        ShapeBuilder<GetWidgetResponse> builder = REGISTRY.createBuilder(GetWidgetResponse.ID, GetWidgetResponse.class);
        GetWidgetResponse response = JSON.deserializeShape(
            "{\"widgetId\":\"w\",\"color\":\"purple\",\"size\":7}", builder);
        assertEquals(Color.UNKNOWN_TO_SDK_VERSION, response.color());
        assertEquals("purple", response.colorAsString());
        assertEquals(7, response.sizeAsInteger());

        GetWidgetResponse copy = assertInstanceOf(GetWidgetResponse.class,
                                                  REGISTRY.deserialize(Document.of(response)));
        assertEquals(response, copy);
        assertEquals("purple", copy.colorAsString());
    }

    @Test
    void errorIsConstructedThroughItsRegisteredBuilder() {
        WidgetNotFoundException error = WidgetNotFoundException.builder().message("gone").widgetId("w-9").build();
        Document document = Document.of(error);
        assertEquals(WidgetNotFoundException.ID, document.discriminator());
        WidgetNotFoundException copy = assertInstanceOf(WidgetNotFoundException.class, REGISTRY.deserialize(document));
        assertEquals("w-9", copy.widgetId());
        assertTrue(copy.getMessage().contains("gone"), copy.getMessage());
        assertTrue(REGISTRY.createBuilder(ServiceUnavailableException.ID)
                           .build() instanceof ServiceUnavailableException);
    }
}
