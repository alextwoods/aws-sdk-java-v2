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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.smithy.runtime.fixture.Widget;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

class SchemaTypeRegistryTest {

    private static final ShapeId OTHER_ID = ShapeId.from("software.amazon.awssdk.fixture#OtherWidget");
    private static final Schema OTHER = Schema.structureBuilder(OTHER_ID)
                                              .putMember("name", PreludeSchemas.STRING)
                                              .build();

    private static final RegisteredType<Widget> WIDGET = RegisteredType.of(Widget.SCHEMA, Widget.class,
                                                                           Widget::builder);
    private static final RegisteredType<Widget> OTHER_ENTRY = RegisteredType.of(OTHER, Widget.class,
                                                                                Widget::builder);

    @Test
    void lookupReturnsTheExactRegisteredObjects() {
        SchemaTypeRegistry registry = SchemaTypeRegistry.builder()
                                                        .partition(List.of(Widget.ID), () -> List.of(WIDGET))
                                                        .build();
        assertSame(WIDGET, registry.entry(Widget.ID).orElseThrow());
        assertSame(Widget.SCHEMA, registry.schema(Widget.ID).orElseThrow());
        assertSame(Widget.class, registry.getShapeClass(Widget.ID));
        assertInstanceOf(Widget.Builder.class, registry.createBuilder(Widget.ID));
        assertTrue(registry.contains(Widget.ID));
        assertEquals(List.of(WIDGET), registry.entries());

        assertEquals(Optional.empty(), registry.entry(OTHER_ID));
        assertEquals(Optional.empty(), registry.entry(Widget.ID.withMember("count")));
        assertEquals(Optional.empty(), registry.entry(null));
        assertNull(registry.getShapeClass(OTHER_ID));
        assertNull(registry.createBuilder(OTHER_ID));
        assertFalse(registry.contains(OTHER_ID));
    }

    @Test
    void partitionsLoadOnceAndOnlyWhenOneOfTheirTypesIsLookedUp() {
        AtomicInteger widgetLoads = new AtomicInteger();
        AtomicInteger otherLoads = new AtomicInteger();
        SchemaTypeRegistry registry = SchemaTypeRegistry.builder()
            .partition(List.of(Widget.ID), () -> {
                widgetLoads.incrementAndGet();
                return List.of(WIDGET);
            })
            .partition(List.of(OTHER_ID), () -> {
                otherLoads.incrementAndGet();
                return List.of(OTHER_ENTRY);
            })
            .build();

        assertEquals(List.of(OTHER_ID, Widget.ID), List.copyOf(registry.shapeIds()));
        assertTrue(registry.contains(Widget.ID) && registry.contains(OTHER_ID));
        assertEquals(0, widgetLoads.get() + otherLoads.get());

        registry.entry(Widget.ID);
        registry.getShapeClass(Widget.ID);
        assertEquals(1, widgetLoads.get());
        assertEquals(0, otherLoads.get());

        assertEquals(List.of(OTHER_ENTRY, WIDGET), registry.entries());
        assertEquals(1, widgetLoads.get());
        assertEquals(1, otherLoads.get());
    }

    @Test
    void discriminatedDocumentIsConstructedThroughTheRegisteredBuilder() {
        SchemaTypeRegistry registry = SchemaTypeRegistry.builder()
                                                        .partition(List.of(Widget.ID), () -> List.of(WIDGET))
                                                        .build();
        Widget widget = Widget.builder().widgetId("w-1").count(3).build();
        Document document = Document.of(widget);
        assertEquals(Widget.ID, document.discriminator());

        Widget copy = assertInstanceOf(Widget.class, registry.deserialize(document));
        assertEquals("w-1", copy.widgetId());
        assertEquals(3, copy.count());
    }

    @Test
    void registrationIsStrict() {
        assertThrows(IllegalArgumentException.class,
                     () -> SchemaTypeRegistry.builder().partition(List.of(Widget.ID, Widget.ID), List::of));
        assertThrows(IllegalArgumentException.class,
                     () -> SchemaTypeRegistry.builder().partition(List.of(Widget.ID), List::of)
                                             .partition(List.of(Widget.ID), List::of));
        assertThrows(IllegalArgumentException.class,
                     () -> SchemaTypeRegistry.builder().partition(List.of(Widget.ID.withMember("count")), List::of));
        assertThrows(IllegalArgumentException.class,
                     () -> RegisteredType.of(PreludeSchemas.STRING, Widget.class, Widget::builder));
        assertThrows(IllegalArgumentException.class,
                     () -> RegisteredType.of(Widget.SCHEMA.member("count"), Widget.class, Widget::builder));
    }

    @Test
    void partitionSupplyingOtherTypesFailsDeterministically() {
        SchemaTypeRegistry registry = SchemaTypeRegistry.builder()
                                                        .partition(List.of(Widget.ID), () -> List.of(OTHER_ENTRY))
                                                        .build();
        IllegalStateException first = assertThrows(IllegalStateException.class, () -> registry.entry(Widget.ID));
        IllegalStateException second = assertThrows(IllegalStateException.class, () -> registry.entry(Widget.ID));
        assertEquals(first.getMessage(), second.getMessage());
        assertTrue(first.getMessage().contains("missing [" + Widget.ID + "]"), first.getMessage());
        assertTrue(first.getMessage().contains("undeclared [" + OTHER_ID + "]"), first.getMessage());
    }
}
