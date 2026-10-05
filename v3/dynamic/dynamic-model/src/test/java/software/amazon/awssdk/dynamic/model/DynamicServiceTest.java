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

package software.amazon.awssdk.dynamic.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/** Runtime schemas and dynamic operation descriptors of a service (Requirements 14.1, 14.6, 14.17). */
class DynamicServiceTest {

    private static final String NS = "example.weather";
    private static final TraitKey<DynamicTrait> DYNAMIC_TRAIT = TraitKey.get(DynamicTrait.class);

    private static final String MODEL = "$version: \"2\"\n"
        + "namespace " + NS + "\n"
        + "@aws.protocols#awsJson1_0\n"
        + "@aws.auth#sigv4(name: \"weather\")\n"
        + "@xmlNamespace(uri: \"https://example.com/\")\n"
        + "@a#a(\"service\")\n"
        + "service Weather {\n"
        + "    version: \"2020-01-01\"\n"
        + "    operations: [GetThing, Ping, Upload]\n"
        + "    errors: [ServiceFault]\n"
        + "}\n"
        + "@a#a([1])\n"
        + "operation GetThing {\n"
        + "    input := {\n"
        + "        @required @jsonName(\"CITY\") @documentation(\"The city.\") @a#a(true)\n"
        + "        city: String\n"
        + "        color: Color\n"
        + "        level: Level\n"
        + "    }\n"
        + "    output := { thing: Thing, derived: Derived, tree: Tree }\n"
        + "    errors: [ZFault, NotFound]\n"
        + "}\n"
        + "operation Ping {}\n"
        + "operation Upload {\n"
        + "    input := { @required body: Body }\n"
        + "    output := { events: Events }\n"
        + "}\n"
        + "@a#a({k: \"v\"}) @b.c#d(-1) @documentation(\"A thing.\")\n"
        + "structure Thing { name: Name, count: Integer }\n"
        + "@a#a(1) @b#b(\"two\")\n"
        + "string Name\n"
        + "enum Color {\n"
        + "    @a#a(null)\n"
        + "    RED\n"
        + "    GREEN = \"green\"\n"
        + "}\n"
        + "intEnum Level {\n"
        + "    @a#a(1) @b#b(2)\n"
        + "    LOW = 1\n"
        + "    HIGH = 2\n"
        + "}\n"
        + "@mixin\n"
        + "structure Base { @a#a(\"mixin member\") id: String }\n"
        + "structure Derived with [Base] { extra: String }\n"
        + "structure Tree { children: Trees, value: String }\n"
        + "list Trees { member: Tree }\n"
        + "@streaming blob Body\n"
        + "@streaming union Events { tick: Tick }\n"
        + "structure Tick {}\n"
        + "@error(\"client\") structure NotFound {}\n"
        + "@error(\"client\") structure ZFault {}\n"
        + "@error(\"server\") structure ServiceFault {}\n";

    private static DynamicModel model;
    private static DynamicService service;
    private static ServiceSchemaIndex index;

    @BeforeAll
    static void load() {
        model = TestModels.load(MODEL);
        service = model.service();
        index = service.schemaIndex();
    }

    @Test
    void indexesEveryShapeOfTheClosureWithItsExactIdAndType() {
        assertSame(index.service(), service.schema());
        assertEquals(ShapeType.SERVICE, service.schema().type());
        assertEquals(List.of(id("GetThing"), id("Ping"), id("Upload")),
                     index.operations().stream().map(Schema::id).toList());
        assertEquals(ShapeType.STRUCTURE, schema("Thing").type());
        assertEquals(ShapeType.UNION, schema("Events").type());
        assertEquals(ShapeType.ENUM, schema("Color").type());
        assertEquals(ShapeType.INT_ENUM, schema("Level").type());
        assertEquals(ShapeType.LIST, schema("Trees").type());
        assertEquals(ShapeType.STRING, schema("Name").type());
        assertEquals(ShapeType.BLOB, schema("Body").type());
        assertEquals(List.of("name", "count"), index.members(schema("Thing")).stream().map(Schema::memberName)
                                                    .toList());
        // Prelude member targets are Smithy Java's shared prelude schemas, as in generated code.
        assertSame(PreludeSchemas.INTEGER, schema("Thing").member("count").memberTarget());
        assertSame(schema("Name"), schema("Thing").member("name").memberTarget());
    }

    @Test
    void schemasCarryRuntimeTraitsButNeitherDesignTimeNorUnknownTraits() {
        Schema city = index.schema(id("GetThingInput$city")).orElseThrow();
        assertNotNull(city.getDirectTrait(TraitKey.REQUIRED_TRAIT));
        assertEquals("CITY", city.getDirectTrait(TraitKey.get(JsonNameTrait.class)).getValue());
        assertNull(city.getDirectTrait(TraitKey.get(DocumentationTrait.class)));
        assertEquals("https://example.com/",
                     service.schema().getDirectTrait(TraitKey.get(XmlNamespaceTrait.class)).getUri());
        for (Schema schema : List.of(city, schema("Thing"), schema("Name"), service.schema(), schema("GetThing"))) {
            assertNull(schema.getDirectTrait(DYNAMIC_TRAIT), schema.id() + " carries an unknown trait by class");
        }
    }

    @Test
    void exposesEveryUnknownTraitByExactTraitIdIncludingEnumMembers() {
        Map<String, Node> actual = new TreeMap<>();
        index.unknownTraits().forEach((target, traits) -> traits.asMap().forEach(
            (traitId, trait) -> actual.put(target + " @" + traitId, trait.toNode())));
        Map<String, Node> expected = new TreeMap<>();
        expected.put(NS + "#Weather @a#a", Node.from("service"));
        expected.put(NS + "#GetThing @a#a", Node.fromNodes(Node.from(1)));
        expected.put(NS + "#GetThingInput$city @a#a", Node.from(true));
        expected.put(NS + "#Thing @a#a", TestModels.node("{\"k\": \"v\"}"));
        expected.put(NS + "#Thing @b.c#d", Node.from(-1));
        expected.put(NS + "#Name @a#a", Node.from(1));
        expected.put(NS + "#Name @b#b", Node.from("two"));
        expected.put(NS + "#Color$RED @a#a", Node.nullNode());
        expected.put(NS + "#Level$LOW @a#a", Node.from(1));
        expected.put(NS + "#Level$LOW @b#b", Node.from(2));
        // The mixin member's trait is on the member that inherits it; the mixin itself is not in the closure.
        expected.put(NS + "#Derived$id @a#a", Node.from("mixin member"));
        assertEquals(expected, actual);

        assertEquals(Optional.of(Node.from("two")), index.unknownTraits(schema("Name")).value(ShapeId.from("b#b")));
        assertEquals(service.operation("GetThing").unknownTraits(), index.unknownTraits(id("GetThing")));
        assertTrue(index.schema(id("Base")).isEmpty());
        assertTrue(model.model().getShape(id("Base")).isPresent(), "the runtime model keeps the mixin");
    }

    @Test
    void buildsEnumAndIntEnumMemberSchemas() {
        Schema red = index.schema(id("Color$RED")).orElseThrow();
        assertEquals(id("Color$RED"), red.id());
        assertEquals("RED", red.getDirectTrait(TraitKey.get(EnumValueTrait.class)).expectStringValue());
        assertSame(Unit.SCHEMA, red.memberTarget());
        assertNull(red.getDirectTrait(DYNAMIC_TRAIT));
        assertEquals(List.of("RED", "GREEN"), index.members(schema("Color")).stream().map(Schema::memberName)
                                                   .toList());
        assertEquals(2, index.schema(id("Level$HIGH")).orElseThrow()
                             .getDirectTrait(TraitKey.get(EnumValueTrait.class)).expectIntValue());
        assertEquals(Set.of("RED", "green"), schema("Color").stringEnumValues());
    }

    @Test
    void resolvesRecursiveShapesToTheIndexedSchemas() {
        Schema tree = schema("Tree");
        Schema trees = schema("Trees");
        assertSame(trees, tree.member("children").memberTarget());
        assertSame(tree, trees.member("member").memberTarget());
    }

    @Test
    void createsDescriptorsWithTheContentsOfGeneratedDescriptors() {
        DynamicAwsOperation getThing = service.operation("GetThing");
        AwsOperationDescriptor descriptor = getThing.descriptor();
        assertEquals(id("GetThing"), descriptor.id());
        assertEquals(service.id(), descriptor.serviceId());
        assertEquals(id("GetThingInput"), descriptor.inputId());
        assertEquals(id("GetThingOutput"), descriptor.outputId());
        // Operation and service errors, sorted by shape ID.
        assertEquals(List.of(id("NotFound"), id("ServiceFault"), id("ZFault")), descriptor.errorIds());

        OperationSchemas schemas = getThing.schemas();
        assertSame(schemas, descriptor.schemas().orElseThrow());
        assertSame(schema("GetThing"), schemas.operation());
        assertSame(schema("GetThingInput"), schemas.input());
        assertSame(schema("GetThingOutput"), schemas.output());
        assertEquals(List.of(schema("NotFound"), schema("ServiceFault"), schema("ZFault")), schemas.errors());
        assertTrue(schemas.inputStreamMember().isEmpty());

        OperationSchemas ping = service.operation("Ping").schemas();
        assertEquals(ShapeId.from("smithy.api#Unit"), service.operation("Ping").descriptor().inputId());
        assertSame(Unit.SCHEMA, ping.input());
        assertSame(Unit.SCHEMA, ping.output());
        assertEquals(List.of(schema("ServiceFault")), ping.errors());

        OperationSchemas upload = service.operation("Upload").schemas();
        assertEquals(Optional.of("body"), upload.inputStreamMember().map(Schema::memberName));
        assertEquals(Optional.of("events"), upload.outputStreamMember().map(Schema::memberName));
    }

    @Test
    void buildsSchemaGuidedDocumentsForInputsOutputsAndAggregates() {
        DynamicAwsOperation getThing = service.operation("GetThing");
        assertSame(getThing.schemas().input(), getThing.inputBuilder().get().schema());
        assertSame(getThing.schemas().output(), getThing.outputBuilder().get().schema());
        assertFalse(getThing.inputBuilder().get() == getThing.inputBuilder().get(), "fresh builders");
        // Runtime consumers such as event stream decoders build values from the schema itself.
        assertSame(schema("Tick"), schema("Tick").shapeBuilder().schema());
        assertTrue(schema("Tree").shapeBuilder().build() instanceof StructDocument);
    }

    private static ShapeId id(String relative) {
        return ShapeId.from(NS + "#" + relative);
    }

    private static Schema schema(String relative) {
        return index.schema(id(relative)).orElseThrow(() -> new AssertionError("no schema for " + relative));
    }
}
