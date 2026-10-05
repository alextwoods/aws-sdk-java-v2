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

package software.amazon.awssdk.awscodegen.schema;

import static software.amazon.awssdk.awscodegen.schema.SchemaRuntimeSymbols.SCHEMA;
import static software.amazon.awssdk.awscodegen.schema.SchemaRuntimeSymbols.SCHEMA_BUILDER;
import static software.amazon.awssdk.awscodegen.schema.SchemaRuntimeSymbols.SET;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.awssdk.awscodegen.CodegenOrdering;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.AnnotationTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Plans the schema files of one service: its {@code <Service>Schemas} holder and, for services whose schemas do not
 * fit one class, package-private partitions of it (Requirements 4.1, 4.5-4.7, 2.11).
 *
 * <p><b>Schemas.</b> Every shape of the service closure except resources and prelude shapes gets exactly one schema
 * field: the service, each operation, each public model type (structure, union, enum, intEnum) and each other shape a
 * member targets (lists, maps and simple shapes). Service, operation and type schemas are the exported
 * {@code public static final} constants named by {@link SchemaSymbols#constantName}; the others are internal fields
 * named {@code s<Name>}. Each schema has the exact shape ID and type, its members in modeled order with their exact
 * targets and direct member traits, and its {@linkplain RuntimeTraits runtime-relevant traits} (requiredness,
 * defaults and constraints among them) created from their exact node values.
 *
 * <p><b>Recursion.</b> Shapes are emitted in dependency order: a schema's member targets are always initialized
 * before it. Shapes in a cycle (a strongly connected component of the member-target graph with more than one shape,
 * or a shape that targets itself: structure/union/list/map recursion) are built in two phases, the way Smithy Java's
 * own generator does: a {@code SchemaBuilder} per shape ({@code b<Name>}), then a static block that adds every member,
 * targeting the builders of the same cycle, then {@code b<Name>.build().resolve()}. After resolution a member's target
 * is the resolved schema, so recursive links and the exported constant are the same objects. Shapes outside a cycle
 * reference the resolved constants directly.
 *
 * <p><b>ID-keyed metadata.</b> Each file also registers its schemas with the service's {@code ServiceSchemaIndex}:
 * exported types, non-exported shapes, operations, modeled member order of structures and unions, one member schema
 * per enum and intEnum member (with its runtime traits), and every unknown trait of every shape and member by exact
 * trait ID. The index is built on first use, not when the schemas are initialized.
 *
 * <p><b>Partitions.</b> The dependency-ordered emission is cut into partitions of at most {@code partitionWeight}
 * units (roughly one unit per schema, member, trait and node value, so that one class stays far below the JVM limits
 * on initializer code and constants); a cycle is never split. A single partition is the holder itself. Otherwise the
 * partitions are package-private classes {@code <Service>Schemas1..n} whose fields reference earlier partitions only,
 * and the holder re-exports every exported constant, so each exported schema still has exactly one object and one
 * public name.
 *
 * <p>Everything is ordered by content: shapes by absolute shape ID, members in modeled order and traits by trait ID;
 * the dependency order is a depth-first traversal in that order.
 */
final class SchemaPlanner {

    /** Default partition size; see the class documentation. */
    static final int DEFAULT_PARTITION_WEIGHT = 1000;

    /** Line break and indentation of a continued field initializer. */
    private static final String CONTINUATION = "\n        ";

    private enum Kind {
        SERVICE, OPERATION, TYPE, SHAPE;

        boolean exported() {
            return this != SHAPE;
        }
    }

    /** The one schema field of a shape. */
    private static final class Field {
        private final Shape shape;
        private final Kind kind;
        private String name;
        private String builder;
        private int partition = -1;

        private Field(Shape shape, Kind kind) {
            this.shape = shape;
            this.kind = kind;
        }
    }

    /** A dependency-ordered emission unit: one shape, or every shape of one cycle. */
    private record Block(List<Field> fields, boolean recursive, int weight) {
    }

    private final Model model;
    private final ServiceShape service;
    private final Symbol holder;
    private final int partitionWeight;
    private final Map<ShapeId, Field> fields = new TreeMap<>();
    private final List<Symbol> partitions = new ArrayList<>();

    SchemaPlanner(Model model, ServiceShape service, Symbol holder, int partitionWeight) {
        this.model = model;
        this.service = service;
        this.holder = holder;
        this.partitionWeight = partitionWeight;
    }

    /** The holder's plan first, then the plan of each partition in order. */
    List<SchemaFilePlan> plan() {
        collect();
        List<Block> blocks = blocks();
        name();
        List<List<Block>> parts = partition(blocks);
        List<SchemaFilePlan> plans = new ArrayList<>();
        Field serviceField = fields.get(service.getId());
        if (parts.size() == 1) {
            plans.add(new SchemaFilePlan(holder, true, service.getId(), declarations(parts.get(0), 0, true),
                                         registrations(parts.get(0)), serviceField.name));
            return plans;
        }
        List<SchemaFilePlan.Code> exports = new ArrayList<>();
        List<SchemaFilePlan.Code> calls = new ArrayList<>();
        for (Field field : fields.values()) {
            if (field.kind.exported()) {
                exports.add(new JavaCode().text("/** Schema of {@code " + field.shape.getId() + "}. */\n"
                                                + "public static final ").type(SCHEMA).text(" " + field.name + " = ")
                                          .type(partitions.get(field.partition)).text("." + field.name + ";")
                                          .build());
            }
        }
        for (Symbol partition : partitions) {
            calls.add(new JavaCode().type(partition).text(".register(builder);").build());
        }
        plans.add(new SchemaFilePlan(holder, true, service.getId(), exports, calls, serviceField.name));
        for (int i = 0; i < parts.size(); i++) {
            plans.add(new SchemaFilePlan(partitions.get(i), false, service.getId(),
                                         declarations(parts.get(i), i, false), registrations(parts.get(i)), ""));
        }
        return plans;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Closure, names and order.

    private void collect() {
        for (Shape shape : CodegenOrdering.byShapeId(new Walker(model).walkShapes(service))) {
            if (shape.isMemberShape() || shape.isResourceShape() || SchemaSymbols.prelude(shape.getId()).isPresent()) {
                continue;
            }
            Kind kind = switch (shape.getType()) {
                case SERVICE -> Kind.SERVICE;
                case OPERATION -> Kind.OPERATION;
                case STRUCTURE, UNION, ENUM, INT_ENUM -> Kind.TYPE;
                default -> Kind.SHAPE;
            };
            if (kind == Kind.SERVICE && !shape.getId().equals(service.getId())) {
                throw new AwsCodegenException("Service " + shape.getId() + " is in the closure of " + service.getId());
            }
            fields.put(shape.getId(), new Field(shape, kind));
        }
    }

    /**
     * Exported constants are named from the contextual shape name and must be unique; internal fields and builders
     * start with a lower-case letter, so they never collide with a constant, and are made unique in shape-ID order.
     */
    private void name() {
        Map<String, ShapeId> constants = new HashMap<>();
        Set<String> internal = new HashSet<>();
        for (Field field : fields.values()) {
            String contextual = field.shape.getId().getName(service);
            if (field.kind.exported()) {
                field.name = SchemaSymbols.constantName(contextual);
                ShapeId previous = constants.putIfAbsent(field.name, field.shape.getId());
                if (previous != null) {
                    throw new AwsCodegenException("Shapes " + previous + " and " + field.shape.getId() + " of service "
                                                  + service.getId() + " both map to the schema constant " + field.name
                                                  + ".");
                }
            } else {
                field.name = unique("s" + contextual, internal);
            }
        }
        for (Field field : fields.values()) {
            if (field.builder != null) {
                field.builder = unique("b" + field.shape.getId().getName(service), internal);
            }
        }
    }

    private static String unique(String base, Set<String> taken) {
        String name = base;
        for (int i = 2; !taken.add(name); i++) {
            name = base + "$" + i;
        }
        return name;
    }

    /**
     * Strongly connected components of the member-target graph in dependency order (Tarjan's algorithm, iterative,
     * over shapes in shape-ID order and members in modeled order), then the operations and the service. Marks every
     * shape of a cycle as needing a builder.
     */
    private List<Block> blocks() {
        List<ShapeId> nodes = new ArrayList<>();
        fields.values().stream().filter(f -> f.kind == Kind.TYPE || f.kind == Kind.SHAPE)
              .forEach(f -> nodes.add(f.shape.getId()));
        Map<ShapeId, Integer> index = new HashMap<>();
        Map<ShapeId, Integer> low = new HashMap<>();
        Deque<ShapeId> stack = new ArrayDeque<>();
        Set<ShapeId> onStack = new HashSet<>();
        List<Block> blocks = new ArrayList<>();
        record Frame(ShapeId node, List<ShapeId> successors, int[] next) {
        }
        for (ShapeId start : nodes) {
            if (index.containsKey(start)) {
                continue;
            }
            Deque<Frame> work = new ArrayDeque<>();
            index.put(start, index.size());
            low.put(start, index.get(start));
            stack.push(start);
            onStack.add(start);
            work.push(new Frame(start, successors(start), new int[1]));
            while (!work.isEmpty()) {
                Frame frame = work.peek();
                ShapeId node = frame.node();
                if (frame.next()[0] < frame.successors().size()) {
                    ShapeId successor = frame.successors().get(frame.next()[0]++);
                    if (!index.containsKey(successor)) {
                        index.put(successor, index.size());
                        low.put(successor, index.get(successor));
                        stack.push(successor);
                        onStack.add(successor);
                        work.push(new Frame(successor, successors(successor), new int[1]));
                    } else if (onStack.contains(successor)) {
                        low.put(node, Math.min(low.get(node), index.get(successor)));
                    }
                    continue;
                }
                work.pop();
                if (!work.isEmpty()) {
                    ShapeId parent = work.peek().node();
                    low.put(parent, Math.min(low.get(parent), low.get(node)));
                }
                if (low.get(node).equals(index.get(node))) {
                    List<ShapeId> component = new ArrayList<>();
                    ShapeId member;
                    do {
                        member = stack.pop();
                        onStack.remove(member);
                        component.add(member);
                    } while (!member.equals(node));
                    blocks.add(block(component, component.size() > 1 || frame.successors().contains(node)));
                }
            }
        }
        fields.values().stream().filter(f -> f.kind == Kind.OPERATION)
              .forEach(f -> blocks.add(block(List.of(f.shape.getId()), false)));
        blocks.add(block(List.of(service.getId()), false));
        return blocks;
    }

    /** Member targets that have schema fields (not prelude shapes), in modeled member order. */
    private List<ShapeId> successors(ShapeId node) {
        Shape shape = fields.get(node).shape;
        if (!isAggregate(shape)) {
            return List.of();
        }
        List<ShapeId> result = new ArrayList<>();
        for (MemberShape member : CodegenOrdering.members(shape)) {
            if (fields.containsKey(member.getTarget())) {
                result.add(member.getTarget());
            }
        }
        return result;
    }

    private Block block(List<ShapeId> ids, boolean recursive) {
        List<Field> blockFields = new ArrayList<>();
        int weight = 0;
        for (ShapeId id : CodegenOrdering.sorted(ids)) {
            Field field = fields.get(id);
            if (recursive) {
                field.builder = "";
            }
            blockFields.add(field);
            weight += weight(field.shape);
        }
        return new Block(List.copyOf(blockFields), recursive, weight);
    }

    private List<List<Block>> partition(List<Block> blocks) {
        List<List<Block>> parts = new ArrayList<>();
        List<Block> current = new ArrayList<>();
        int weight = 0;
        for (Block block : blocks) {
            if (!current.isEmpty() && weight + block.weight() > partitionWeight) {
                parts.add(current);
                current = new ArrayList<>();
                weight = 0;
            }
            current.add(block);
            weight += block.weight();
        }
        parts.add(current);
        for (int i = 0; i < parts.size(); i++) {
            Symbol symbol = parts.size() == 1 ? holder : partitionSymbol(i + 1);
            partitions.add(symbol);
            for (Block block : parts.get(i)) {
                for (Field field : block.fields()) {
                    field.partition = i;
                }
            }
        }
        return parts;
    }

    private Symbol partitionSymbol(int number) {
        String name = holder.getName() + number;
        return Symbol.builder().namespace(holder.getNamespace(), ".").name(name)
                     .definitionFile(holder.getNamespace().replace('.', '/') + "/" + name + ".java").build();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Declarations.

    private List<SchemaFilePlan.Code> declarations(List<Block> blocks, int partition, boolean single) {
        List<SchemaFilePlan.Code> result = new ArrayList<>();
        for (Block block : blocks) {
            if (!block.recursive()) {
                Field field = block.fields().get(0);
                result.add(declaration(field, single).code(initializer(field, partition, Set.of())).text(";")
                                                      .build());
                continue;
            }
            Set<ShapeId> cycle = new HashSet<>();
            block.fields().forEach(f -> cycle.add(f.shape.getId()));
            for (Field field : block.fields()) {
                result.add(new JavaCode().text("private static final ").type(SCHEMA_BUILDER)
                                         .text(" " + field.builder + " = ").code(builder(field.shape)).text(";")
                                         .build());
            }
            JavaCode wiring = new JavaCode().text("static {");
            for (Field field : block.fields()) {
                wiring.text("\n    " + field.builder)
                      .code(members(field.shape, partition, cycle, "", "\n            "))
                      .text(";");
            }
            result.add(wiring.text("\n}").build());
            for (Field field : block.fields()) {
                result.add(declaration(field, single).text(field.builder + ".build().resolve();").build());
            }
        }
        return result;
    }

    private JavaCode declaration(Field field, boolean single) {
        JavaCode code = new JavaCode();
        if (field.kind.exported()) {
            code.text(single ? "/** Schema of {@code " + field.shape.getId() + "}. */\npublic static final "
                             : "static final ");
        } else {
            code.text(single ? "private static final " : "static final ");
        }
        return code.type(SCHEMA).text(" " + field.name + " = ");
    }

    /** The initializer of a shape outside a cycle. */
    private SchemaFilePlan.Code initializer(Field field, int partition, Set<ShapeId> cycle) {
        Shape shape = field.shape;
        JavaCode code = new JavaCode();
        switch (shape.getType()) {
            case STRUCTURE, UNION, LIST, SET, MAP -> code.code(builder(shape))
                                                         .code(members(shape, partition, cycle, CONTINUATION,
                                                                       CONTINUATION))
                                                         .text(CONTINUATION + ".build()");
            case ENUM, INT_ENUM -> {
                code.type(SCHEMA).text(shape.getType() == ShapeType.ENUM ? ".createEnum(" : ".createIntEnum(")
                    .shapeId(shape.getId()).text(", ").type(SET).text(".of(");
                String separator = "";
                for (MemberShape member : CodegenOrdering.members(shape)) {
                    EnumValueTrait value = member.expectTrait(EnumValueTrait.class);
                    code.text(separator);
                    if (shape.getType() == ShapeType.ENUM) {
                        code.string(value.expectStringValue());
                    } else {
                        code.text(Integer.toString(value.expectIntValue()));
                    }
                    separator = ", ";
                }
                code.text(")").code(traits(shape)).text(")");
            }
            default -> code.type(SCHEMA).text("." + factory(shape) + "(").shapeId(shape.getId()).code(traits(shape))
                           .text(")");
        }
        return code.build();
    }

    /** {@code Schema.structureBuilder(ShapeId.from("..."), traits)} and its list, map and union forms. */
    private SchemaFilePlan.Code builder(Shape shape) {
        String method = switch (shape.getType()) {
            case STRUCTURE -> "structureBuilder";
            case UNION -> "unionBuilder";
            case LIST, SET -> "listBuilder";
            case MAP -> "mapBuilder";
            default -> throw new AwsCodegenException("No schema builder for " + shape.getType() + " " + shape.getId());
        };
        return new JavaCode().type(SCHEMA).text("." + method + "(").shapeId(shape.getId()).code(traits(shape))
                             .text(")").build();
    }

    /** One {@code .putMember(...)} per member in modeled order, preceded by {@code first}, then {@code separator}. */
    private SchemaFilePlan.Code members(Shape shape, int partition, Set<ShapeId> cycle, String first,
                                        String separator) {
        JavaCode code = new JavaCode();
        String next = first;
        for (MemberShape member : CodegenOrdering.members(shape)) {
            code.text(next + ".putMember(").string(member.getMemberName()).text(", ")
                .code(reference(member.getTarget(), partition, cycle)).code(traits(member)).text(")");
            next = separator;
        }
        return code.build();
    }

    private SchemaFilePlan.Code reference(ShapeId target, int partition, Set<ShapeId> cycle) {
        Optional<SchemaSymbols.Reference> prelude = SchemaSymbols.prelude(target);
        if (prelude.isPresent()) {
            return new JavaCode().type(prelude.get().owner()).text("." + prelude.get().field()).build();
        }
        Field field = fields.get(target);
        if (field == null) {
            throw new AwsCodegenException("Member target " + target + " is not in the closure of " + service.getId());
        }
        if (cycle.contains(target)) {
            return new JavaCode().text(field.builder).build();
        }
        if (field.partition == partition) {
            return new JavaCode().text(field.name).build();
        }
        return new JavaCode().type(partitions.get(field.partition)).text("." + field.name).build();
    }

    /** {@code , trait, trait} for the runtime traits of a shape or member, or nothing. */
    private static SchemaFilePlan.Code traits(Shape shape) {
        JavaCode code = new JavaCode();
        for (Trait trait : RuntimeTraits.runtimeTraits(shape)) {
            code.text(", ").trait(trait, shape.getId());
        }
        return code.build();
    }

    private static String factory(Shape shape) {
        return switch (shape.getType()) {
            case BLOB -> "createBlob";
            case BOOLEAN -> "createBoolean";
            case STRING -> "createString";
            case BYTE -> "createByte";
            case SHORT -> "createShort";
            case INTEGER -> "createInteger";
            case LONG -> "createLong";
            case FLOAT -> "createFloat";
            case DOUBLE -> "createDouble";
            case BIG_INTEGER -> "createBigInteger";
            case BIG_DECIMAL -> "createBigDecimal";
            case TIMESTAMP -> "createTimestamp";
            case DOCUMENT -> "createDocument";
            case OPERATION -> "createOperation";
            case SERVICE -> "createService";
            default -> throw new AwsCodegenException("No schema factory for " + shape.getType() + " "
                                                     + shape.getId());
        };
    }

    private static boolean isAggregate(Shape shape) {
        return switch (shape.getType()) {
            case STRUCTURE, UNION, LIST, SET, MAP -> true;
            default -> false;
        };
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Index registrations.

    private List<SchemaFilePlan.Code> registrations(List<Block> blocks) {
        List<SchemaFilePlan.Code> result = new ArrayList<>();
        for (Block block : blocks) {
            for (Field field : block.fields()) {
                Shape shape = field.shape;
                switch (field.kind) {
                    case TYPE -> result.add(new JavaCode().text("builder.type(" + field.name + ");").build());
                    case SHAPE -> result.add(new JavaCode().text("builder.shape(" + field.name + ");").build());
                    case OPERATION -> result.add(new JavaCode().text("builder.operation(" + field.name + ");")
                                                               .build());
                    default -> {
                    }
                }
                List<MemberShape> members = CodegenOrdering.members(shape);
                if ((shape.isStructureShape() || shape.isUnionShape()) && members.size() > 1) {
                    JavaCode order = new JavaCode().text("builder.memberOrder(" + field.name);
                    members.forEach(member -> order.text(", ").string(member.getMemberName()));
                    result.add(order.text(");").build());
                }
                if (shape.isEnumShape() || shape.isIntEnumShape()) {
                    for (MemberShape member : members) {
                        result.add(new JavaCode().text("builder.enumMember(" + field.name + ", ")
                                                 .string(member.getMemberName()).code(traits(member)).text(");")
                                                 .build());
                    }
                }
                unknownTraits(shape).ifPresent(result::add);
                for (MemberShape member : members) {
                    unknownTraits(member).ifPresent(result::add);
                }
            }
        }
        return result;
    }

    private static Optional<SchemaFilePlan.Code> unknownTraits(Shape shape) {
        List<DynamicTrait> unknown = RuntimeTraits.unknownTraits(shape);
        if (unknown.isEmpty()) {
            return Optional.empty();
        }
        JavaCode code = new JavaCode().text("builder.unknownTraits(").shapeId(shape.getId());
        for (DynamicTrait trait : unknown) {
            code.text(",\n        ").trait(trait, shape.getId());
        }
        return Optional.of(code.text(");").build());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Weights.

    private static int weight(Shape shape) {
        int weight = 2 + traitWeight(shape);
        for (MemberShape member : CodegenOrdering.members(shape)) {
            weight += 3 + traitWeight(member);
        }
        return weight;
    }

    private static int traitWeight(Shape shape) {
        int weight = 0;
        for (Trait trait : CodegenOrdering.traits(shape)) {
            boolean annotation = trait instanceof AnnotationTrait && trait.toNode().isObjectNode()
                                 && trait.toNode().expectObjectNode().isEmpty();
            if (trait instanceof DynamicTrait || !annotation && RuntimeTraits.TRAITS.containsKey(trait.toShapeId())) {
                weight += 2 + nodeWeight(trait.toNode());
            } else if (annotation && RuntimeTraits.TRAITS.containsKey(trait.toShapeId())) {
                weight += 1;
            }
        }
        return weight;
    }

    private static int nodeWeight(Node node) {
        if (node instanceof ArrayNode array) {
            int weight = 1;
            for (Node element : array.getElements()) {
                weight += nodeWeight(element);
            }
            return weight;
        } else if (node instanceof ObjectNode object) {
            int weight = 1;
            for (Node value : object.getMembers().values()) {
                weight += 1 + nodeWeight(value);
            }
            return weight;
        }
        return 1;
    }
}
