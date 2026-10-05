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

package software.amazon.awssdk.smithy.runtime.registry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.smithy.runtime.document.DocumentCoercionException;
import software.amazon.awssdk.smithy.runtime.document.SchemaDocuments;
import software.amazon.awssdk.smithy.runtime.fixture.Widget;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

class SchemaTypeRegistryCompositionTest {

    private static final Supplier<Widget.Builder> WIDGET_BUILDER = Widget::builder;
    private static final RegisteredType<Widget> WIDGET = RegisteredType.of(Widget.SCHEMA, Widget.class,
                                                                           WIDGET_BUILDER);
    private static final RegisteredType<Gadget> GADGET = RegisteredType.of(Gadget.SCHEMA, Gadget.class,
                                                                           Gadget.Builder::new);

    private static SchemaTypeRegistry registry(RegisteredType<?>... entries) {
        List<ShapeId> ids = Arrays.stream(entries).map(RegisteredType::shapeId).toList();
        return SchemaTypeRegistry.builder().partition(ids, () -> List.of(entries)).build();
    }

    @Test
    void composedLookupsReturnTheComponentsOwnObjectsInAnyOrder() {
        SchemaTypeRegistry widgets = registry(WIDGET);
        SchemaTypeRegistry gadgets = registry(GADGET);
        for (SchemaTypeRegistry composed : List.of(SchemaTypeRegistry.compose(widgets, gadgets),
                                                   SchemaTypeRegistry.compose(gadgets, widgets))) {
            assertEquals(List.of(Gadget.ID, Widget.ID), List.copyOf(composed.shapeIds()));
            assertSame(WIDGET, composed.entry(Widget.ID).orElseThrow());
            assertSame(GADGET, composed.entry(Gadget.ID).orElseThrow());
            assertSame(Widget.SCHEMA, composed.schema(Widget.ID).orElseThrow());
            assertSame(Gadget.class, composed.getShapeClass(Gadget.ID));
            assertSame(WIDGET_BUILDER, composed.entry(Widget.ID).orElseThrow().builderFactory());
            assertInstanceOf(Widget.Builder.class, composed.createBuilder(Widget.ID));
            assertEquals(List.of(GADGET, WIDGET), composed.entries());
        }
        assertSame(widgets, SchemaTypeRegistry.compose(widgets));
        assertEquals(0, SchemaTypeRegistry.compose().shapeIds().size());
    }

    @Test
    void disjointCompositionLoadsNoPartitionAndSharesLoadedPartitions() {
        AtomicInteger loads = new AtomicInteger();
        SchemaTypeRegistry widgets = SchemaTypeRegistry.builder()
            .partition(List.of(Widget.ID), () -> {
                loads.incrementAndGet();
                return List.of(WIDGET);
            })
            .build();
        SchemaTypeRegistry composed = SchemaTypeRegistry.compose(widgets, registry(GADGET));
        assertEquals(0, loads.get());
        assertTrue(composed.contains(Widget.ID));
        assertEquals(0, loads.get());
        assertSame(WIDGET, composed.entry(Widget.ID).orElseThrow());
        assertSame(WIDGET, widgets.entry(Widget.ID).orElseThrow());
        assertEquals(1, loads.get(), "the composed registry shares the component's partition");
    }

    @Test
    void identicalEntriesAreNotConflicts() {
        SchemaTypeRegistry first = registry(WIDGET);
        SchemaTypeRegistry second = registry(WIDGET, GADGET);
        SchemaTypeRegistry equivalent = registry(RegisteredType.of(Widget.SCHEMA, Widget.class, WIDGET_BUILDER));

        SchemaTypeRegistry composed = SchemaTypeRegistry.compose(first, second, equivalent);
        assertSame(WIDGET, composed.entry(Widget.ID).orElseThrow());
        assertSame(Widget.SCHEMA, SchemaTypeRegistry.compose(equivalent, first).schema(Widget.ID).orElseThrow());
        assertSame(WIDGET, SchemaTypeRegistry.compose(composed, first, composed).entry(Widget.ID).orElseThrow());
    }

    @Test
    void conflictingEntriesAreRejectedWithEveryConflictInDeterministicOrder() {
        Schema otherWidgetSchema = Schema.structureBuilder(Widget.ID).putMember("name", PreludeSchemas.STRING).build();
        Schema otherGadgetSchema = Schema.structureBuilder(Gadget.ID).build();
        SchemaTypeRegistry first = registry(WIDGET, GADGET);
        SchemaTypeRegistry second = registry(RegisteredType.of(otherWidgetSchema, Gadget.class, Gadget.Builder::new),
                                             RegisteredType.of(otherGadgetSchema, Gadget.class, Gadget.Builder::new));
        SchemaTypeRegistry factoryOnly = registry(RegisteredType.of(Widget.SCHEMA, Widget.class, Widget::builder));

        TypeRegistryConflictException error = assertThrows(TypeRegistryConflictException.class,
                                                           () -> SchemaTypeRegistry.compose(first, second));
        List<TypeRegistryConflictException.Conflict> conflicts = error.conflicts();
        assertEquals(List.of(Gadget.ID, Widget.ID), conflicts.stream().map(c -> c.shapeId()).toList());
        TypeRegistryConflictException.Conflict widget = conflicts.get(1);
        assertSame(WIDGET, widget.first());
        assertSame(Gadget.class, widget.second().type());
        assertEquals(0, widget.firstRegistry());
        assertEquals(1, widget.secondRegistry());
        assertEquals("registry #0 (namespaces [software.amazon.awssdk.fixture])", widget.firstSource());
        assertEquals(List.of("schema", "type", "builder factory"), widget.differences());
        assertEquals(List.of("schema", "builder factory"), conflicts.get(0).differences());
        assertTrue(error.getMessage().contains(Widget.ID + " is registered as " + Widget.class.getName()
                                               + " by registry #0"), error.getMessage());
        assertTrue(error.getMessage().contains("and as " + Gadget.class.getName() + " by registry #1"),
                   error.getMessage());
        assertEquals(error.getMessage(), assertThrows(TypeRegistryConflictException.class,
                                                      () -> SchemaTypeRegistry.compose(first, second)).getMessage());

        TypeRegistryConflictException factory = assertThrows(TypeRegistryConflictException.class,
                                                             () -> SchemaTypeRegistry.compose(first, factoryOnly));
        assertEquals(List.of("builder factory"), factory.conflicts().get(0).differences());
    }

    @Test
    void conflictMessageCountsShapeIdsNotConflictingPairs() {
        // Regression (found by Property 18): one ID conflicting in three registries was reported as "2 shape IDs".
        Schema otherWidgetSchema = Schema.structureBuilder(Widget.ID).build();
        SchemaTypeRegistry second = registry(RegisteredType.of(otherWidgetSchema, Gadget.class, Gadget.Builder::new));
        SchemaTypeRegistry third = registry(RegisteredType.of(otherWidgetSchema, Gadget.class, Gadget.Builder::new));

        TypeRegistryConflictException error = assertThrows(TypeRegistryConflictException.class,
                                                           () -> SchemaTypeRegistry.compose(registry(WIDGET), second,
                                                                                            third));
        assertEquals(2, error.conflicts().size());
        assertTrue(error.getMessage().startsWith("Cannot compose type registries: 1 shape ID is registered with "
                                                 + "conflicting entries; "), error.getMessage());
    }

    @Test
    void typedConstructionReportsAbsentUnknownAndIncompatibleDocumentsPrecisely() {
        SchemaTypeRegistry registry = SchemaTypeRegistry.compose(registry(WIDGET), registry(GADGET));
        Widget widget = Widget.builder().widgetId("w-1").count(3).build();
        Widget copy = assertInstanceOf(Widget.class, registry.deserialize(SchemaDocuments.of(widget)));
        assertEquals(3, copy.count());
        assertInstanceOf(Gadget.class, registry.deserializeStrict(Document.of(new Gadget())));

        MissingDiscriminatorException missing = assertThrows(
            MissingDiscriminatorException.class, () -> registry.deserialize(Document.of(Map.of())));
        assertEquals(ShapeType.MAP, missing.sourceKind());
        assertEquals("Cannot construct a registered type from a map document: the document has no shape "
                     + "discriminator", missing.getMessage());

        JsonCodec json = JsonCodec.builder().build();
        Document unknownDocument = json.createDeserializer("{\"__type\":\"com.example#Missing\"}".getBytes(UTF_8))
                                       .readDocument();
        UnknownDiscriminatorException unknown = assertThrows(UnknownDiscriminatorException.class,
                                                             () -> registry.deserialize(unknownDocument));
        assertEquals(ShapeId.from("com.example#Missing"), unknown.shapeId());
        assertEquals("No type is registered for the document discriminator com.example#Missing",
                     unknown.getMessage());

        Document badCount = json.createDeserializer(("{\"__type\":\"" + Widget.ID + "\",\"widgetId\":\"w\","
                                                     + "\"count\":\"many\"}").getBytes(UTF_8)).readDocument();
        DocumentCoercionException coercion = assertThrows(DocumentCoercionException.class,
                                                          () -> registry.deserialize(badCount));
        assertEquals("$.count", coercion.path());
        assertEquals(ShapeType.STRING, coercion.sourceKind());
        assertEquals(PreludeSchemas.INTEGER.id(), coercion.requestedShape());
    }

    /** A second generated-style type. */
    static final class Gadget implements SerializableStruct {
        static final ShapeId ID = ShapeId.from("software.amazon.awssdk.fixture#Gadget");
        static final Schema SCHEMA = Schema.structureBuilder(ID).build();

        @Override
        public Schema schema() {
            return SCHEMA;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return null;
        }

        static final class Builder implements ShapeBuilder<Gadget> {
            @Override
            public Gadget build() {
                return new Gadget();
            }

            @Override
            public ShapeBuilder<Gadget> deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (state, member, value) -> { });
                return this;
            }

            @Override
            public Schema schema() {
                return SCHEMA;
            }
        }
    }
}
