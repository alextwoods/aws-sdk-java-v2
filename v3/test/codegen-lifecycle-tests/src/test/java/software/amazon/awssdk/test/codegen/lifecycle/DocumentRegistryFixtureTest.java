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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.smithy.runtime.document.DocumentCoercionException;
import software.amazon.awssdk.smithy.runtime.document.ModeledDocument;
import software.amazon.awssdk.smithy.runtime.document.SchemaDocuments;
import software.amazon.awssdk.smithy.runtime.registry.MissingDiscriminatorException;
import software.amazon.awssdk.smithy.runtime.registry.RegisteredType;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.smithy.runtime.registry.TypeRegistryConflictException;
import software.amazon.awssdk.smithy.runtime.registry.UnknownDiscriminatorException;
import software.amazon.awssdk.test.codegen.lifecycle.model.Color;
import software.amazon.awssdk.test.codegen.lifecycle.model.GetWidgetResponse;
import software.amazon.awssdk.test.codegen.lifecycle.model.Size;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetAnnotation;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetShape;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetTree;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Document conversion and registry composition against the generated fixture types and primary registry
 * (Requirements 6.1-6.4 and 6.8-6.14).
 */
class DocumentRegistryFixtureTest {

    private static final SchemaTypeRegistry REGISTRY = CodegenLifecycleFixtureServiceMetadata.typeRegistry();
    private static final Document FREE_DOCUMENT = Document.of(Map.of("free", Document.of(List.of(Document.of(1)))));

    private static GetWidgetResponse response() {
        WidgetTree leaf = WidgetTree.builder().label("leaf").build();
        return GetWidgetResponse.builder()
            .widgetId("w-1")
            .name("widget")
            .tree(t -> t.label("root").children(leaf, WidgetTree.builder().label("second").build())
                        .parent(p -> p.label("up")))
            .outline(o -> o.nested(n -> n.tree(leaf)))
            .color(Color.GREEN)
            .size(Size.LARGE)
            .limits(l -> l.percent(100).code("abc").flags(true, false).enabled(false))
            .details(d -> d.owner("owner").serial(Long.MAX_VALUE).revision(Integer.MIN_VALUE).ratio(0.1)
                           .secret("hunter2")
                           .secretsByName(Map.of("k", "v"))
                           .colors(Color.RED, Color.GREEN)
                           .colorsByName(Map.of("fav", Color.RED))
                           .sizes(Size.SMALL, Size.LARGE)
                           .matrix(List.of("a", "b"), List.of())
                           .tagsByGroup(Map.of("g", List.of("x", "y")))
                           .thumbnail(SdkBytes.fromByteArray(new byte[] {0, 1, (byte) 0xff}))
                           .createdAt(Instant.parse("2026-01-02T03:04:05.678Z"))
                           .annotation(a -> a.body(FREE_DOCUMENT).note("n")))
            .build();
    }

    @Test
    void generatedValuesConvertToDocumentsWithExactKindsAndDiscriminators() {
        ModeledDocument document = SchemaDocuments.of(response());

        assertEquals(GetWidgetResponse.ID, document.discriminator());
        assertEquals(ShapeType.ENUM, document.getMember("color").type());
        assertEquals(Color.ID, document.getMember("color").discriminator());
        assertEquals(ShapeType.INT_ENUM, document.getMember("size").type());
        assertEquals(Size.ID, document.getMember("size").discriminator());
        Document outline = document.getMember("outline");
        assertEquals(ShapeType.UNION, outline.type());
        assertEquals(WidgetShape.ID, outline.discriminator());
        assertEquals(WidgetShape.ID, outline.getMember("nested").discriminator());
        assertEquals(WidgetTree.ID, outline.getMember("nested").getMember("tree").discriminator());
        assertEquals(List.of("leaf", "second"), document.getMember("tree").getMember("children").asList().stream()
                                                        .map(child -> child.getMember("label").asString()).toList());
        Document details = document.getMember("details");
        assertEquals(ShapeType.BLOB, details.getMember("thumbnail").type());
        assertEquals(ShapeType.TIMESTAMP, details.getMember("createdAt").type());
        assertEquals(List.of(1, 10), details.getMember("sizes").asList().stream().map(Document::asInteger).toList());
        assertEquals(ShapeType.DOCUMENT, WidgetAnnotation.SCHEMA.member("body").type());
        assertTrue(details.getMember("annotation").getMember("body").isType(ShapeType.MAP));
        assertTrue(!document.toString().contains("hunter2"), "sensitive values are redacted");
    }

    @Test
    void documentsRoundTripThroughTheRegisteredBuilders() {
        GetWidgetResponse response = response();
        ModeledDocument document = SchemaDocuments.of(response);

        assertEquals(response, REGISTRY.deserialize(document));
        assertEquals(response, REGISTRY.deserializeStrict(document));
        assertEquals(response, SchemaDocuments.toShapeStrict(document, GetWidgetResponse.builder()));
        assertEquals(response, REGISTRY.deserialize(Document.of(response)), "Smithy Java typed documents too");
        assertEquals(document, SchemaDocuments.of(assertInstanceOf(GetWidgetResponse.class,
                                                                   REGISTRY.deserialize(document))));
    }

    @Test
    void protocolDecodedDocumentsKeepTheirProtocolContext() {
        GetWidgetResponse response = response();
        JsonCodec json = JsonCodec.builder().serializeTypeInDocuments(true).useTimestampFormat(true).build();
        String encoded = json.serializeToString(SchemaDocuments.of(response));
        assertTrue(encoded.startsWith("{\"__type\":\"" + GetWidgetResponse.ID + "\""), encoded);
        assertTrue(encoded.contains("\"thumbnail\":\"AAH/\""), encoded);

        Document decoded = json.createDeserializer(encoded.getBytes(UTF_8)).readDocument();
        assertEquals(ShapeType.MAP, decoded.type());
        assertEquals(GetWidgetResponse.ID, decoded.discriminator());
        assertNotEquals(SchemaDocuments.of(response), decoded);
        GetWidgetResponse typed = assertInstanceOf(GetWidgetResponse.class, REGISTRY.deserialize(decoded));
        assertEquals(response.details().thumbnail(), typed.details().thumbnail());
        assertEquals(response.details().createdAt(), typed.details().createdAt());
        assertEquals(response.outline(), typed.outline());
        assertEquals(response.tree(), typed.tree());
        // The document member is now a JSON document, which the generated equals (Smithy Java document equality)
        // does not equate with the original; the modeled documents of the two values are equal.
        assertNotEquals(response.details().annotation(), typed.details().annotation());
        assertEquals(SchemaDocuments.of(response), SchemaDocuments.of(typed));
        assertTrue(SchemaDocuments.equivalent(GetWidgetResponse.SCHEMA, decoded, Document.of(response)));
    }

    @Test
    void absentUnknownMismatchedAndIncompatibleDocumentsFailPrecisely() {
        JsonCodec json = JsonCodec.builder().build();
        MissingDiscriminatorException missing = assertThrows(
            MissingDiscriminatorException.class,
            () -> REGISTRY.deserialize(json.createDeserializer("{\"label\":\"x\"}".getBytes(UTF_8)).readDocument()));
        assertEquals(ShapeType.MAP, missing.sourceKind());

        ShapeId unknownId = ShapeId.from("software.amazon.awssdk.test.codegen.lifecycle#Color");
        UnknownDiscriminatorException unknown = assertThrows(
            UnknownDiscriminatorException.class, () -> REGISTRY.deserialize(SchemaDocuments.coerce(Document.of("red"),
                                                                                                   Color.SCHEMA)));
        assertEquals(unknownId, unknown.shapeId());

        DocumentCoercionException mismatch = assertThrows(
            DocumentCoercionException.class,
            () -> SchemaDocuments.toShape(SchemaDocuments.of(WidgetTree.builder().label("x").build()),
                                          GetWidgetResponse.builder()));
        assertEquals(DocumentCoercionException.Reason.DISCRIMINATOR_MISMATCH, mismatch.reason());
        assertEquals(WidgetTree.ID, mismatch.discriminator());
        assertEquals(GetWidgetResponse.ID, mismatch.requestedShape());

        Document badSize = json.createDeserializer(("{\"__type\":\"" + GetWidgetResponse.ID + "\",\"widgetId\":\"w\","
                                                    + "\"size\":4294967296}").getBytes(UTF_8)).readDocument();
        DocumentCoercionException range = assertThrows(DocumentCoercionException.class,
                                                       () -> REGISTRY.deserialize(badSize));
        assertEquals(DocumentCoercionException.Reason.OUT_OF_RANGE, range.reason());
        assertEquals("$.size", range.path());
        assertEquals(Size.ID, range.requestedShape());

        Document badUnion = json.createDeserializer(("{\"__type\":\"" + WidgetShape.ID + "\",\"circle\":1.0,"
                                                     + "\"none\":{}}").getBytes(UTF_8)).readDocument();
        assertEquals(DocumentCoercionException.Reason.UNION_MEMBER_COUNT,
                     assertThrows(DocumentCoercionException.class, () -> REGISTRY.deserialize(badUnion)).reason());
    }

    @Test
    void thePrimaryRegistryComposesWithIdentityAndRejectsConflicts() {
        Schema eventSchema = Schema.structureBuilder(ShapeId.from("com.example.events#WidgetEvent")).build();
        RegisteredType<WidgetTree> event = RegisteredType.of(eventSchema, WidgetTree.class, WidgetTree::builder);
        SchemaTypeRegistry events = SchemaTypeRegistry.builder()
                                                      .partition(List.of(event.shapeId()), () -> List.of(event))
                                                      .build();
        SchemaTypeRegistry composed = SchemaTypeRegistry.compose(events, REGISTRY);
        for (ShapeId id : REGISTRY.shapeIds()) {
            RegisteredType<?> entry = REGISTRY.entry(id).orElseThrow();
            assertSame(entry, composed.entry(id).orElseThrow(), id.toString());
            assertSame(entry.schema(), composed.schema(id).orElseThrow());
            assertSame(entry.type(), composed.getShapeClass(id));
        }
        assertSame(event, composed.entry(event.shapeId()).orElseThrow());
        assertEquals(REGISTRY.shapeIds().size() + 1, composed.shapeIds().size());
        assertSame(REGISTRY.entry(WidgetTree.ID).orElseThrow(),
                   SchemaTypeRegistry.compose(REGISTRY, composed, REGISTRY).entry(WidgetTree.ID).orElseThrow());
        assertEquals(response(), composed.deserialize(SchemaDocuments.of(response())));

        RegisteredType<GetWidgetResponse> conflicting = RegisteredType.of(WidgetTree.SCHEMA, GetWidgetResponse.class,
                                                                          GetWidgetResponse::builder);
        SchemaTypeRegistry other = SchemaTypeRegistry.builder()
                                                     .partition(List.of(WidgetTree.ID), () -> List.of(conflicting))
                                                     .build();
        TypeRegistryConflictException error = assertThrows(TypeRegistryConflictException.class,
                                                           () -> SchemaTypeRegistry.compose(REGISTRY, other));
        assertEquals(List.of(WidgetTree.ID), error.conflicts().stream().map(c -> c.shapeId()).toList());
        assertTrue(error.getMessage().contains(WidgetTree.class.getName()), error.getMessage());
        assertTrue(error.getMessage().contains(GetWidgetResponse.class.getName()), error.getMessage());
        assertTrue(error.getMessage().contains(WidgetTree.ID.toString()), error.getMessage());
        assertEquals(List.of("type", "builder factory"), error.conflicts().get(0).differences());
    }
}
