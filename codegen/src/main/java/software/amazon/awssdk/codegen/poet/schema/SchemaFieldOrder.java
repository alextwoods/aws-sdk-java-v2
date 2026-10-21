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

package software.amazon.awssdk.codegen.poet.schema;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.ToIntFunction;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.schema.SchemaCodegenException;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;
import software.amazon.smithy.utils.CaseUtils;

/**
 * Decides which shapes get a generated Root_Schema, the name of each schema field, the order in which the fields are
 * initialized, which member edges are deferred, and how the fields are split into {@code <Svc>Schemas},
 * {@code <Svc>Schemas1}, … partition classes (design A.17). Port of smithy-java {@code SchemaFieldOrder}.
 *
 * <p><b>Closure</b> (Requirement 10.1): the input, output and errors of every operation the service contains, the
 * service's errors, any extra root shapes the caller gives, and every shape reachable from those through members.
 * Prelude shapes are left out; members that target them reference {@code PreludeSchemas}.
 *
 * <p><b>Order</b>: shapes are grouped into strongly connected components of the member graph and the components are
 * emitted in dependency order (Tarjan's algorithm, roots in ShapeId order, members in declaration order), so every
 * field is initialized after the fields it references. A component with more than one shape, or one shape with a
 * member that targets itself, is recursive. Inside a recursive component the shapes are built in depth-first
 * post-order; a member that targets a shape still on the depth-first path closes a cycle and is the only kind of edge
 * built from a {@code SchemaBuilder} (a Deferred_Schema, Requirement 10.5). Only those targets get an
 * {@code X_BUILDER} field.
 *
 * <p><b>Partitions</b> (Requirement 10.7): components are assigned greedily, in order, to partition classes whose
 * estimated static-initializer bytecode stays under a budget; a component is never split. A partition therefore only
 * references itself and earlier partitions, so initializing any one of them first initializes only partitions whose
 * fields it needs, without a cycle between class initializers.
 *
 * <p>Iteration uses ShapeId order and model declaration order only, so the result does not depend on hash order or on
 * the order of the model's source files (Requirement 10.9).
 */
@SdkInternalApi
public final class SchemaFieldOrder {
    /**
     * The name of the per-partition constant that holds the service namespace.
     */
    static final String NAMESPACE_CONSTANT = "NS";

    private static final String BUILDER_SUFFIX = "_BUILDER";
    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    private final List<SchemaField> fields;
    private final Map<ShapeId, SchemaField> byShapeId;
    private final List<List<SchemaField>> partitions;

    private SchemaFieldOrder(List<SchemaField> fields, List<List<SchemaField>> partitions) {
        this.fields = Collections.unmodifiableList(fields);
        Map<ShapeId, SchemaField> map = new HashMap<>();
        for (SchemaField field : fields) {
            map.put(field.shape().getId(), field);
        }
        this.byShapeId = Collections.unmodifiableMap(map);
        this.partitions = Collections.unmodifiableList(partitions);
    }

    /**
     * Computes the order for a service.
     *
     * @param model the model that contains the service
     * @param service the service
     * @param reservedNames names a schema field must not take, such as the simple names of generated model classes
     * @param sizeEstimate the estimated static-initializer bytecode of a field, including its builder if it has one
     * @param partitionBudget the bytecode budget of one partition class
     * @return the order
     * @throws SchemaCodegenException if two shapes have the same schema ShapeId
     */
    public static SchemaFieldOrder create(Model model, ServiceShape service, Set<String> reservedNames,
                                          ToIntFunction<SchemaField> sizeEstimate, int partitionBudget) {
        return create(model, service, Collections.<ShapeId>emptySet(), reservedNames, sizeEstimate, partitionBudget);
    }

    /**
     * Computes the order for a service, with more shapes in the closure: the shapes {@code rootShapes} lists and every
     * shape reachable from them through members. The types-only mode of {@link SchemaCodegen} uses them for shapes that
     * no operation reaches.
     *
     * @param model the model that contains the service
     * @param service the service
     * @param rootShapes more roots of the closure
     * @param reservedNames names a schema field must not take, such as the simple names of generated model classes
     * @param sizeEstimate the estimated static-initializer bytecode of a field, including its builder if it has one
     * @param partitionBudget the bytecode budget of one partition class
     * @return the order
     * @throws SchemaCodegenException if two shapes have the same schema ShapeId, or a root shape is not in the model or
     *     is a service, operation, resource or member
     */
    public static SchemaFieldOrder create(Model model, ServiceShape service, Set<ShapeId> rootShapes,
                                          Set<String> reservedNames, ToIntFunction<SchemaField> sizeEstimate,
                                          int partitionBudget) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(rootShapes, "rootShapes");
        Set<ShapeId> closure = closure(model, service, rootShapes);
        List<List<Shape>> components = new Tarjan(model, closure).run();

        List<SchemaField> fields = new ArrayList<>();
        int unit = 0;
        for (List<Shape> component : components) {
            boolean recursive = component.size() > 1 || targetsItself(component.get(0));
            if (!recursive) {
                fields.add(new SchemaField(component.get(0), schemaId(component.get(0)), unit, false,
                                           Collections.<ShapeId>emptySet(), false));
            } else {
                fields.addAll(recursiveComponent(model, component, unit));
            }
            unit++;
        }
        assignNames(fields, reservedNames);
        checkUniqueIds(fields);
        List<List<SchemaField>> partitions = partition(fields, sizeEstimate, partitionBudget);
        return new SchemaFieldOrder(fields, partitions);
    }

    /**
     * @return every schema field, in initialization order
     */
    public List<SchemaField> fields() {
        return fields;
    }

    /**
     * @return the partitions, in order; partition {@code i} holds the fields whose {@link SchemaField#partition()} is i
     */
    public List<List<SchemaField>> partitions() {
        return partitions;
    }

    /**
     * @param shapeId the model ShapeId of a shape
     * @return its schema field, or null if the shape is not in the closure
     */
    public SchemaField field(ShapeId shapeId) {
        return byShapeId.get(shapeId);
    }

    private static Set<ShapeId> closure(Model model, ServiceShape service, Set<ShapeId> rootShapes) {
        List<ShapeId> roots = new ArrayList<>();
        TreeSet<OperationShape> operations = new TreeSet<>(TopDownIndex.of(model).getContainedOperations(service));
        for (OperationShape operation : operations) {
            roots.add(operation.getInputShape());
            roots.add(operation.getOutputShape());
            roots.addAll(operation.getErrors());
        }
        roots.addAll(service.getErrors());
        for (ShapeId root : new TreeSet<>(rootShapes)) {
            if (!model.getShape(root).isPresent()) {
                throw new SchemaCodegenException("Root shape " + root + " is not in the model");
            }
            roots.add(root);
        }

        Set<ShapeId> closure = new TreeSet<>();
        Deque<ShapeId> pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            ShapeId id = pending.pop();
            if (Prelude.isPreludeShape(id) || closure.contains(id)) {
                continue;
            }
            Shape shape = model.expectShape(id);
            switch (shape.getType()) {
                case SERVICE:
                case OPERATION:
                case RESOURCE:
                case MEMBER:
                    throw new SchemaCodegenException("Shape " + id + " of type " + shape.getType()
                                                     + " cannot be the target of a member");
                default:
                    break;
            }
            closure.add(id);
            for (MemberShape member : edges(shape)) {
                pending.push(member.getTarget());
            }
        }
        return closure;
    }

    /**
     * The members whose targets the shape's schema references: none for enums, whose members are enum values.
     */
    static Collection<MemberShape> edges(Shape shape) {
        if (shape.getType() == ShapeType.ENUM || shape.getType() == ShapeType.INT_ENUM) {
            return Collections.emptyList();
        }
        return shape.members();
    }

    private static boolean targetsItself(Shape shape) {
        for (MemberShape member : edges(shape)) {
            if (member.getTarget().equals(shape.getId())) {
                return true;
            }
        }
        return false;
    }

    private static ShapeId schemaId(Shape shape) {
        return shape.getTrait(OriginalShapeIdTrait.class).map(OriginalShapeIdTrait::getOriginalId).orElse(shape.getId());
    }

    /**
     * Builds a recursive component in depth-first post-order, deferring only the edges that close a cycle.
     */
    private static List<SchemaField> recursiveComponent(Model model, List<Shape> component, int unit) {
        Map<ShapeId, Shape> members = new LinkedHashMap<>();
        for (Shape shape : component) {
            members.put(shape.getId(), shape);
        }
        List<Shape> order = new ArrayList<>();
        Set<ShapeId> done = new HashSet<>();
        Set<ShapeId> deferredMembers = new HashSet<>();
        Set<ShapeId> builderTargets = new HashSet<>();
        for (ShapeId start : new TreeSet<>(members.keySet())) {
            if (!done.contains(start)) {
                visit(members.get(start), members, new LinkedHashSet<ShapeId>(), done, order, deferredMembers,
                      builderTargets);
            }
        }
        List<SchemaField> result = new ArrayList<>(order.size());
        for (Shape shape : order) {
            Set<ShapeId> deferred = new HashSet<>();
            for (MemberShape member : edges(shape)) {
                if (deferredMembers.contains(member.getId())) {
                    deferred.add(member.getId());
                }
            }
            result.add(new SchemaField(shape, schemaId(shape), unit, true, deferred,
                                       builderTargets.contains(shape.getId())));
        }
        return result;
    }

    private static void visit(Shape shape, Map<ShapeId, Shape> component, Set<ShapeId> path, Set<ShapeId> done,
                              List<Shape> order, Set<ShapeId> deferredMembers, Set<ShapeId> builderTargets) {
        path.add(shape.getId());
        for (MemberShape member : edges(shape)) {
            ShapeId target = member.getTarget();
            if (!component.containsKey(target)) {
                continue;
            }
            if (path.contains(target)) {
                deferredMembers.add(member.getId());
                builderTargets.add(target);
            } else if (!done.contains(target)) {
                visit(component.get(target), component, path, done, order, deferredMembers, builderTargets);
            }
        }
        path.remove(shape.getId());
        done.add(shape.getId());
        order.add(shape);
    }

    private static void assignNames(List<SchemaField> fields, Set<String> reservedNames) {
        Set<String> used = new HashSet<>(reservedNames);
        used.add(NAMESPACE_CONSTANT);
        for (SchemaField field : fields) {
            ShapeId id = field.shape().getId();
            String name = toSchemaName(id.getName());
            if (used.contains(name) || name.equals(id.getName())) {
                name = toSchemaName(id.toString().replace('.', '_').replace('#', '_'));
            }
            field.fieldName = unique(name, used);
        }
        for (SchemaField field : fields) {
            if (field.hasBuilder()) {
                field.builderName = unique(field.fieldName + BUILDER_SUFFIX, used);
            }
        }
    }

    private static String unique(String name, Set<String> used) {
        String candidate = name;
        for (int i = 2; used.contains(candidate); i++) {
            candidate = name + "_" + i;
        }
        used.add(candidate);
        return candidate;
    }

    static String toSchemaName(String name) {
        return CaseUtils.toSnakeCase(name).toUpperCase(Locale.ENGLISH);
    }

    private static void checkUniqueIds(List<SchemaField> fields) {
        Map<ShapeId, ShapeId> seen = new HashMap<>();
        for (SchemaField field : fields) {
            ShapeId previous = seen.put(field.schemaId(), field.shape().getId());
            if (previous != null) {
                throw new SchemaCodegenException("Shapes " + previous + " and " + field.shape().getId()
                                                 + " have the same original shape ID " + field.schemaId());
            }
        }
    }

    private static List<List<SchemaField>> partition(List<SchemaField> fields, ToIntFunction<SchemaField> sizeEstimate,
                                                     int budget) {
        List<List<SchemaField>> result = new ArrayList<>();
        List<SchemaField> current = new ArrayList<>();
        long currentSize = 0;
        int i = 0;
        while (i < fields.size()) {
            int unit = fields.get(i).unit();
            int end = i;
            long unitSize = 0;
            while (end < fields.size() && fields.get(end).unit() == unit) {
                unitSize += sizeEstimate.applyAsInt(fields.get(end));
                end++;
            }
            if (!current.isEmpty() && currentSize + unitSize > budget) {
                result.add(current);
                current = new ArrayList<>();
                currentSize = 0;
            }
            for (int j = i; j < end; j++) {
                SchemaField field = fields.get(j);
                field.partition = result.size();
                current.add(field);
            }
            currentSize += unitSize;
            i = end;
        }
        if (!current.isEmpty()) {
            result.add(current);
        }
        List<List<SchemaField>> unmodifiable = new ArrayList<>(result.size());
        for (List<SchemaField> partition : result) {
            unmodifiable.add(Collections.unmodifiableList(partition));
        }
        return unmodifiable;
    }

    /**
     * One shape's schema field.
     */
    public static final class SchemaField {
        private final Shape shape;
        private final ShapeId schemaId;
        private final int unit;
        private final boolean recursive;
        private final Set<ShapeId> deferredMembers;
        private final boolean hasBuilder;
        private String fieldName;
        private String builderName;
        private int partition = -1;

        private SchemaField(Shape shape, ShapeId schemaId, int unit, boolean recursive, Set<ShapeId> deferredMembers,
                            boolean hasBuilder) {
            this.shape = shape;
            this.schemaId = schemaId;
            this.unit = unit;
            this.recursive = recursive;
            this.deferredMembers = Collections.unmodifiableSet(deferredMembers);
            this.hasBuilder = hasBuilder;
        }

        /**
         * @return the model shape
         */
        public Shape shape() {
            return shape;
        }

        /**
         * @return the ShapeId the runtime schema carries: the shape's original ID if a model transform recorded one
         *     (Requirement 10.6), otherwise its own ID
         */
        public ShapeId schemaId() {
            return schemaId;
        }

        /**
         * @return whether the shape was synthesized from {@code smithy.api#Unit}, so its schema keeps the shape's own
         *     ID and carries the {@code unitType} trait
         */
        public boolean isSyntheticUnit() {
            return UNIT.equals(schemaId);
        }

        /**
         * @return the ShapeId to emit: {@link #schemaId()}, or the shape's own ID for a synthesized unit
         */
        public ShapeId emittedId() {
            return isSyntheticUnit() ? shape.getId() : schemaId;
        }

        /**
         * @return the name of the static field that holds the schema
         */
        public String fieldName() {
            return fieldName;
        }

        /**
         * @return the name of the static {@code SchemaBuilder} field, or null if no member edge is deferred to this
         *     shape
         */
        public String builderName() {
            return builderName;
        }

        /**
         * @return whether a cycle-closing member edge targets this shape, so it is built from a builder field
         */
        public boolean hasBuilder() {
            return hasBuilder;
        }

        /**
         * @return whether the shape is in a recursive strongly connected component
         */
        public boolean isRecursive() {
            return recursive;
        }

        /**
         * @param member a member of {@link #shape()}
         * @return whether the member's target is referenced through its builder
         */
        public boolean isDeferred(MemberShape member) {
            return deferredMembers.contains(member.getId());
        }

        /**
         * @return whether any member is deferred, so the built schema must be resolved
         */
        public boolean hasDeferredMember() {
            return !deferredMembers.isEmpty();
        }

        /**
         * @return the index of the shape's strongly connected component in initialization order
         */
        public int unit() {
            return unit;
        }

        /**
         * @return the index of the partition class that holds the field
         */
        public int partition() {
            return partition;
        }

        @Override
        public String toString() {
            return "SchemaField(" + shape.getId() + " -> " + fieldName + ")";
        }
    }

    /**
     * Tarjan's strongly connected components algorithm. Emits each component after every component it references, so
     * the result is in dependency order. Recursion depth is bounded by the length of the longest member chain.
     */
    private static final class Tarjan {
        private final Model model;
        private final Set<ShapeId> closure;
        private final Map<ShapeId, Integer> index = new HashMap<>();
        private final Map<ShapeId, Integer> lowLink = new HashMap<>();
        private final Deque<ShapeId> stack = new ArrayDeque<>();
        private final Set<ShapeId> onStack = new HashSet<>();
        private final List<List<Shape>> components = new ArrayList<>();
        private int counter;

        private Tarjan(Model model, Set<ShapeId> closure) {
            this.model = model;
            this.closure = closure;
        }

        private List<List<Shape>> run() {
            for (ShapeId id : closure) {
                if (!index.containsKey(id)) {
                    connect(id);
                }
            }
            return components;
        }

        private void connect(ShapeId id) {
            index.put(id, counter);
            lowLink.put(id, counter);
            counter++;
            stack.push(id);
            onStack.add(id);
            for (MemberShape member : edges(model.expectShape(id))) {
                ShapeId target = member.getTarget();
                if (!closure.contains(target)) {
                    continue;
                }
                if (!index.containsKey(target)) {
                    connect(target);
                    lowLink.put(id, Math.min(lowLink.get(id), lowLink.get(target)));
                } else if (onStack.contains(target)) {
                    lowLink.put(id, Math.min(lowLink.get(id), index.get(target)));
                }
            }
            if (lowLink.get(id).equals(index.get(id))) {
                List<Shape> component = new ArrayList<>();
                ShapeId member;
                do {
                    member = stack.pop();
                    onStack.remove(member);
                    component.add(model.expectShape(member));
                } while (!member.equals(id));
                Collections.reverse(component);
                components.add(component);
            }
        }
    }
}
