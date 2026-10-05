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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.awssdk.smithy.runtime.schema.UnknownTraits;
import software.amazon.awssdk.test.codegen.lifecycle.internal.operation.CodegenLifecycleFixtureOperations;
import software.amazon.awssdk.test.codegen.lifecycle.model.internal.CodegenLifecycleFixtureSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;

/**
 * The fixture's generated schemas, compiled into this module, match the canonical fixture model (Requirements 2.11,
 * 4.1, 4.5-4.7): the oracle is the fixture model assembled here, independently of the generator, and walked from the
 * service. It checks the schema inventory, identities and types, modeled member order and targets, runtime traits
 * (requiredness, defaults and constraints), recursive links, enum member schemas, unknown traits by exact ID, and the
 * operation links of every descriptor.
 */
class SchemaFixtureTest {

    private static final String NAMESPACE = "software.amazon.awssdk.test.codegen.lifecycle";
    private static final Path MODEL = Path.of(System.getProperty("v3.fixture.modelDirectory"))
                                          .resolve("codegen-lifecycle-fixture.smithy");
    /** Runtime-relevant traits the fixture uses; each must be on the schema exactly as modeled. */
    private static final Set<Class<? extends Trait>> RUNTIME_TRAITS = Set.of(
        RequiredTrait.class, DefaultTrait.class, RangeTrait.class, LengthTrait.class, PatternTrait.class,
        UniqueItemsTrait.class, ErrorTrait.class, RetryableTrait.class, ReadonlyTrait.class, StreamingTrait.class,
        EnumValueTrait.class);

    private static Model model;
    private static ServiceShape service;
    private static ServiceSchemaIndex index;
    private static List<Shape> closure;

    @BeforeAll
    static void load() {
        // Without the AWS trait definitions, the aws.* traits load as unknown; the generator knows them.
        model = Model.assembler().putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true).addImport(MODEL).assemble()
                     .unwrap();
        service = model.expectShape(id("CodegenLifecycleFixture"), ServiceShape.class);
        index = CodegenLifecycleFixture.schemaIndex();
        closure = new Walker(model).walkShapes(service).stream()
                                   .filter(s -> !s.isMemberShape() && !s.isResourceShape()
                                                && !s.getId().getNamespace().equals("smithy.api"))
                                   .sorted()
                                   .toList();
    }

    @Test
    void thereIsExactlyOneSchemaPerShapeAndOneExportedSchemaPerPublicType() {
        Set<ShapeId> types = new TreeSet<>();
        Set<ShapeId> operations = new TreeSet<>();
        Set<ShapeId> shapes = new TreeSet<>();
        for (Shape shape : closure) {
            switch (shape.getType()) {
                case STRUCTURE, UNION, ENUM, INT_ENUM -> types.add(shape.getId());
                case OPERATION -> operations.add(shape.getId());
                case SERVICE -> assertEquals(service.getId(), shape.getId());
                default -> shapes.add(shape.getId());
            }
        }
        assertEquals(service.getId(), index.service().id());
        assertEquals(List.copyOf(operations), index.operations().stream().map(Schema::id).toList());
        assertEquals(List.copyOf(types), index.types().stream().map(Schema::id).toList());
        assertEquals(List.copyOf(shapes), index.shapes().stream().map(Schema::id).toList());
        assertEquals(Set.of(id("FlagList"), id("Label"), id("Radius"), id("Body"), id("WidgetTreeList"), id("Secret"),
                            id("SecretMap"), id("ColorList"), id("ColorMap"), id("SizeList"), id("LabelMatrix"),
                            id("StringList"), id("TagGroups")), shapes);
        assertSame(CodegenLifecycleFixtureSchemas.WIDGET_TREE, CodegenLifecycleFixture.widgetTreeSchema());
        assertSame(CodegenLifecycleFixtureSchemas.WIDGET_TREE, index.schema(id("WidgetTree")).orElseThrow());
        assertSame(CodegenLifecycleFixtureSchemas.CODEGEN_LIFECYCLE_FIXTURE, index.service());
    }

    @Test
    void schemasHaveTheModeledIdentityTypeMembersTargetsAndRuntimeTraits() {
        for (Shape shape : closure) {
            Schema schema = index.schema(shape.getId()).orElseThrow(() -> new AssertionError(shape.getId()));
            assertEquals(shape.getId(), schema.id());
            assertEquals(shape.getType(), schema.type(), shape.getId().toString());
            assertRuntimeTraits(shape, schema);
            List<MemberShape> members = List.copyOf(shape.members());
            List<Schema> memberSchemas = index.members(schema);
            assertEquals(members.stream().map(MemberShape::getMemberName).toList(),
                         memberSchemas.stream().map(Schema::memberName).toList(), "member order of " + shape.getId());
            for (int i = 0; i < members.size(); i++) {
                MemberShape member = members.get(i);
                Schema memberSchema = memberSchemas.get(i);
                assertSame(memberSchema, index.schema(member.getId()).orElseThrow());
                assertEquals(member.getId(), memberSchema.id());
                assertEquals(member.getTarget(), memberSchema.memberTarget().id(), member.getId().toString());
                assertRuntimeTraits(member, memberSchema);
            }
        }
    }

    @Test
    void enumAndIntEnumMembersHaveMemberSchemasWithTheirValues() {
        Schema color = CodegenLifecycleFixtureSchemas.COLOR;
        assertEquals(Set.of("red", "green"), color.stringEnumValues());
        assertEquals(List.of(), color.members());
        assertEquals(List.of(id("Color$RED"), id("Color$GREEN")),
                     index.members(color).stream().map(Schema::id).toList());
        Schema size = CodegenLifecycleFixtureSchemas.SIZE;
        assertEquals(Set.of(1, 10), size.intEnumValues());
        Schema large = index.schema(id("Size$LARGE")).orElseThrow();
        assertEquals(10, large.getDirectTrait(TraitKey.get(EnumValueTrait.class)).expectIntValue());
        assertSame(Unit.SCHEMA, large.memberTarget());
    }

    @Test
    void recursiveLinksAreTheExportedSchemas() {
        Schema tree = CodegenLifecycleFixtureSchemas.WIDGET_TREE;
        Schema shape = CodegenLifecycleFixtureSchemas.WIDGET_SHAPE;
        Schema list = index.schema(id("WidgetTreeList")).orElseThrow();
        assertSame(tree, tree.member("parent").memberTarget());
        assertSame(list, tree.member("children").memberTarget());
        assertSame(tree, list.listMember().memberTarget());
        assertSame(shape, shape.member("nested").memberTarget());
        assertSame(tree, shape.member("tree").memberTarget());
        assertSame(Unit.SCHEMA, shape.member("none").memberTarget());
        assertSame(tree, CodegenLifecycleFixtureSchemas.GET_WIDGET_OUTPUT.member("tree").memberTarget());
        // Deep traversal through the cycle keeps returning the same objects.
        assertSame(tree, tree.member("children").listMember().memberTarget().member("parent").memberTarget());
    }

    @Test
    void defaultsAndConstraintsArePreserved() {
        Schema limits = CodegenLifecycleFixtureSchemas.LIMITS;
        assertEquals(model.expectShape(id("Limits$percent")).expectTrait(DefaultTrait.class).toNode(),
                     limits.member("percent").expectTrait(TraitKey.DEFAULT_TRAIT).toNode());
        assertEquals(50, limits.member("percent").expectTrait(TraitKey.DEFAULT_TRAIT).toNode().expectNumberNode()
                               .getValue().intValue());
        assertEquals(Node.from(true), limits.member("enabled").expectTrait(TraitKey.DEFAULT_TRAIT).toNode());
        assertEquals(100, limits.member("percent").expectTrait(TraitKey.RANGE_TRAIT).getMax().orElseThrow()
                                .intValueExact());
        // Member traits override the target's: code has length 1..8, its Label target only min 1.
        assertEquals(8L, limits.member("code").expectTrait(TraitKey.LENGTH_TRAIT).getMax().orElseThrow());
        assertEquals("^[a-z]+$", limits.member("code").expectTrait(TraitKey.PATTERN_TRAIT).getValue());
        assertTrue(limits.member("code").hasTrait(TraitKey.REQUIRED_TRAIT));
        // Smithy Java puts required members without a default first; the index keeps the modeled order.
        assertEquals("code", limits.members().get(0).memberName());
        assertEquals(List.of("percent", "code", "flags", "enabled"),
                     index.members(limits).stream().map(Schema::memberName).toList());
        assertNull(CodegenLifecycleFixtureSchemas.WIDGET_TREE.getTrait(TraitKey.DOCUMENTATION_TRAIT),
                   "documentation is not a runtime trait");
    }

    @Test
    void unknownTraitsAreExposedByExactTraitIdOnShapesAndMembers() {
        Map<ShapeId, UnknownTraits> expected = new TreeMap<>();
        for (Shape shape : closure) {
            addUnknownTraits(shape, expected);
            shape.members().forEach(member -> addUnknownTraits(member, expected));
        }
        assertEquals(Set.of(id("CodegenLifecycleFixture"), id("Color"), id("Color$RED"), id("Size$LARGE"),
                            id("WidgetTree$parent")), expected.keySet());
        assertEquals(expected, index.unknownTraits());
        UnknownTraits red = index.unknownTraits(index.schema(id("Color$RED")).orElseThrow());
        assertEquals(List.of(ShapeId.from("fixture.meta#note"), ShapeId.from("fixture.meta#owner")), red.ids());
        assertEquals(Node.from("paint"), red.value(ShapeId.from("fixture.meta#owner")).orElseThrow());
        assertEquals(Node.nullNode(), index.unknownTraits(id("Size$LARGE")).value(ShapeId.from("fixture.meta#note"))
                                           .orElseThrow());
    }

    @Test
    void descriptorsLinkTheExactOperationSchemas() {
        OperationIndex operations = OperationIndex.of(model);
        for (AwsOperationDescriptor descriptor : CodegenLifecycleFixtureOperations.operations()) {
            OperationShape operation = model.expectShape(descriptor.id(), OperationShape.class);
            OperationSchemas schemas = descriptor.schemas().orElseThrow();
            assertSame(index.schema(operation.getId()).orElseThrow(), schemas.operation());
            assertSame(schemaOf(operation.getInputShape()), schemas.input());
            assertSame(schemaOf(operation.getOutputShape()), schemas.output());
            List<Schema> errors = new ArrayList<>();
            operations.getErrors(service, operation).stream().map(Shape::getId).sorted()
                      .forEach(error -> errors.add(schemaOf(error)));
            assertEquals(errors, schemas.errors());
        }
        OperationSchemas upload = CodegenLifecycleFixtureOperations.UPLOAD.schemas().orElseThrow();
        assertSame(CodegenLifecycleFixtureSchemas.UPLOAD_INPUT.member("body"),
                   upload.inputStreamMember().orElseThrow());
        assertTrue(upload.outputStreamMember().isEmpty());
        assertSame(Unit.SCHEMA, CodegenLifecycleFixtureOperations.GET_STATUS.schemas().orElseThrow().input());
        assertTrue(CodegenLifecycleFixtureSchemas.GET_STATUS.hasTrait(TraitKey.READ_ONLY_TRAIT));
    }

    private static Schema schemaOf(ShapeId id) {
        return id.equals(Unit.ID) ? Unit.SCHEMA : index.schema(id).orElseThrow(() -> new AssertionError(id));
    }

    private static void assertRuntimeTraits(Shape shape, Schema schema) {
        for (Trait trait : shape.getAllTraits().values()) {
            if (RUNTIME_TRAITS.contains(trait.getClass())) {
                assertEquals(trait, schema.getDirectTrait(TraitKey.get(trait.getClass())),
                             trait.toShapeId() + " on " + shape.getId());
            }
        }
        assertNull(schema.getDirectTrait(TraitKey.get(DynamicTrait.class)),
                   "unknown traits are exposed by ID through the index, not by class: " + shape.getId());
        if (shape.getType() == ShapeType.ENUM || shape.getType() == ShapeType.INT_ENUM) {
            for (MemberShape member : shape.members()) {
                Schema memberSchema = index.schema(member.getId()).orElseThrow();
                assertEquals(member.expectTrait(EnumValueTrait.class),
                             memberSchema.getDirectTrait(TraitKey.get(EnumValueTrait.class)));
            }
        }
    }

    private static void addUnknownTraits(Shape shape, Map<ShapeId, UnknownTraits> expected) {
        List<DynamicTrait> unknown = shape.getAllTraits().values().stream()
                                          .filter(t -> t instanceof DynamicTrait)
                                          .filter(t -> !t.toShapeId().getNamespace().startsWith("aws."))
                                          .map(DynamicTrait.class::cast)
                                          .toList();
        if (!unknown.isEmpty()) {
            expected.put(shape.getId(), UnknownTraits.of(unknown));
        }
    }

    private static ShapeId id(String name) {
        return ShapeId.from(NAMESPACE + "#" + name);
    }
}
