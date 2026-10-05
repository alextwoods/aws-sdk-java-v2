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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaBuilder;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait;
import software.amazon.smithy.model.traits.HttpBasicAuthTrait;
import software.amazon.smithy.model.traits.HttpBearerAuthTrait;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.model.traits.HttpDigestAuthTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * Converts the closure of one service of a runtime model into the runtime schema view generated services use: Smithy
 * Java {@link Schema}s registered in a {@link ServiceSchemaIndex} (Requirements 14.1, 14.17).
 *
 * <p>The conversion builds, from the model, the same schemas {@code aws-codegen} generates for the service, so dynamic
 * and generated operations share one schema shape:
 * <ul>
 *   <li>every shape of the service closure except resources and the prelude shapes Smithy Java defines
 *       ({@link PreludeSchemas}, {@link Unit#SCHEMA}) gets exactly one schema with its exact shape ID and type, its
 *       members in modeled order with their exact targets and direct member traits;</li>
 *   <li>schemas carry the {@linkplain #RUNTIME_TRAITS runtime-relevant traits} (the trait set of generated schemas),
 *       sorted by trait ID;</li>
 *   <li>unknown traits ({@link DynamicTrait}s) are never put in a Smithy Java schema, whose class-keyed trait map
 *       would keep at most one of them; the index exposes every one by exact trait ID, any number per shape or
 *       member;</li>
 *   <li>every enum and intEnum member gets a member schema in the index (Smithy Java's enum schemas have none), with
 *       its runtime traits and its unknown traits;</li>
 *   <li>recursive shapes (strongly connected components of the member-target graph) are built in two phases through
 *       {@link SchemaBuilder}s and resolved, as generated schemas are, so a recursive member's target is the
 *       registered schema object;</li>
 *   <li>structure and union schemas create schema-guided {@code StructDocument} builders, so runtime consumers that
 *       build values from a schema (for example event stream decoders) work on dynamic schemas.</li>
 * </ul>
 *
 * <p>Smithy Java's own {@code SchemaConverter} is not used for the schemas themselves: it puts every trait, unknown
 * ones included, in the class-keyed schema trait map (so a second unknown trait on a shape is lost) and builds no
 * enum member schemas.
 *
 * <p>Single use and not thread-safe; {@link DynamicModel} converts each service once.
 */
final class ServiceSchemaConverter {

    /**
     * The runtime-relevant traits: the trait IDs generated schemas carry as runtime trait objects. This is the same
     * set as {@code aws-codegen}'s {@code RuntimeTraits.TRAITS}; codegen-tests compares generated and dynamic schemas
     * of the same model to keep the two equal.
     */
    static final Set<ShapeId> RUNTIME_TRAITS = Set.of(
        // Constraints and presence.
        RequiredTrait.ID, DefaultTrait.ID, LengthTrait.ID, PatternTrait.ID, RangeTrait.ID, UniqueItemsTrait.ID,
        SparseTrait.ID, RequiresLengthTrait.ID, EnumValueTrait.ID, EnumTrait.ID,
        // Errors and retries.
        ErrorTrait.ID, HttpErrorTrait.ID, RetryableTrait.ID,
        // Serialization.
        JsonNameTrait.ID, TimestampFormatTrait.ID, MediaTypeTrait.ID, XmlNameTrait.ID, XmlAttributeTrait.ID,
        XmlFlattenedTrait.ID, XmlNamespaceTrait.ID, EventHeaderTrait.ID, EventPayloadTrait.ID, StreamingTrait.ID,
        // HTTP bindings and endpoints.
        HttpTrait.ID, HttpHeaderTrait.ID, HttpLabelTrait.ID, HttpQueryTrait.ID, HttpQueryParamsTrait.ID,
        HttpPayloadTrait.ID, HttpPrefixHeadersTrait.ID, HttpResponseCodeTrait.ID, HttpChecksumRequiredTrait.ID,
        EndpointTrait.ID, HostLabelTrait.ID,
        // Behavior.
        IdempotencyTokenTrait.ID, IdempotentTrait.ID, ReadonlyTrait.ID, PaginatedTrait.ID, RequestCompressionTrait.ID,
        SensitiveTrait.ID, UnitTypeTrait.ID,
        // Auth.
        AuthTrait.ID, OptionalAuthTrait.ID, HttpBasicAuthTrait.ID, HttpBearerAuthTrait.ID, HttpDigestAuthTrait.ID,
        HttpApiKeyAuthTrait.ID);

    private static final String PRELUDE_NAMESPACE = "smithy.api";

    /** Prelude shapes by name and the shared Smithy Java schema members targeting them use, as in generated code. */
    private static final Map<String, Schema> PRELUDE = Map.ofEntries(
        Map.entry("Blob", PreludeSchemas.BLOB), Map.entry("Boolean", PreludeSchemas.BOOLEAN),
        Map.entry("String", PreludeSchemas.STRING), Map.entry("Timestamp", PreludeSchemas.TIMESTAMP),
        Map.entry("Byte", PreludeSchemas.BYTE), Map.entry("Short", PreludeSchemas.SHORT),
        Map.entry("Integer", PreludeSchemas.INTEGER), Map.entry("Long", PreludeSchemas.LONG),
        Map.entry("Float", PreludeSchemas.FLOAT), Map.entry("Double", PreludeSchemas.DOUBLE),
        Map.entry("BigInteger", PreludeSchemas.BIG_INTEGER), Map.entry("BigDecimal", PreludeSchemas.BIG_DECIMAL),
        Map.entry("Document", PreludeSchemas.DOCUMENT),
        Map.entry("PrimitiveBoolean", PreludeSchemas.PRIMITIVE_BOOLEAN),
        Map.entry("PrimitiveByte", PreludeSchemas.PRIMITIVE_BYTE),
        Map.entry("PrimitiveShort", PreludeSchemas.PRIMITIVE_SHORT),
        Map.entry("PrimitiveInteger", PreludeSchemas.PRIMITIVE_INTEGER),
        Map.entry("PrimitiveLong", PreludeSchemas.PRIMITIVE_LONG),
        Map.entry("PrimitiveFloat", PreludeSchemas.PRIMITIVE_FLOAT),
        Map.entry("PrimitiveDouble", PreludeSchemas.PRIMITIVE_DOUBLE),
        Map.entry("Unit", Unit.SCHEMA));

    private enum Kind { SERVICE, OPERATION, TYPE, SHAPE }

    private final Model model;
    private final ServiceShape service;
    /** The shapes that get a schema, by shape ID, with their kind. */
    private final Map<ShapeId, Kind> kinds = new TreeMap<>();
    private final Map<ShapeId, Schema> schemas = new HashMap<>();
    /** The schema each structure and union document builder supplier creates builders for. */
    private final Map<ShapeId, AtomicReference<Schema>> selfReferences = new HashMap<>();

    private ServiceSchemaConverter(Model model, ServiceShape service) {
        this.model = model;
        this.service = service;
    }

    /**
     * The schemas of the closure of {@code service} in {@code model}, which must already have had the codegen view
     * transforms applied (mixins flattened and removed, service errors copied to operations).
     */
    static ServiceSchemaIndex convert(Model model, ServiceShape service) {
        return new ServiceSchemaConverter(model, service).convert();
    }

    /**
     * The schema the member of a structure, union, list or map, or an operation input or output, targeting
     * {@code target} references: a shared Smithy Java prelude schema or the converted schema.
     */
    static Schema target(ServiceSchemaIndex index, ShapeId target) {
        Schema prelude = prelude(target);
        if (prelude != null) {
            return prelude;
        }
        return index.schema(target).orElseThrow(
            () -> new IllegalStateException(target + " has no schema in the index of " + index.service().id()));
    }

    private ServiceSchemaIndex convert() {
        collect();
        for (List<ShapeId> component : components()) {
            build(component);
        }
        for (Map.Entry<ShapeId, Kind> entry : kinds.entrySet()) {
            if (entry.getValue() == Kind.OPERATION) {
                schemas.put(entry.getKey(), Schema.createOperation(entry.getKey(),
                                                                   runtimeTraits(shape(entry.getKey()))));
            }
        }
        Schema serviceSchema = Schema.createService(service.getId(), runtimeTraits(service));
        schemas.put(service.getId(), serviceSchema);
        selfReferences.forEach((id, self) -> self.set(schemas.get(id)));
        return index(serviceSchema);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Closure and dependency order.

    private void collect() {
        for (Shape shape : new Walker(model).walkShapes(service)) {
            if (shape.isMemberShape() || shape.isResourceShape() || prelude(shape.getId()) != null) {
                continue;
            }
            Kind kind = switch (shape.getType()) {
                case SERVICE -> Kind.SERVICE;
                case OPERATION -> Kind.OPERATION;
                case STRUCTURE, UNION, ENUM, INT_ENUM -> Kind.TYPE;
                default -> Kind.SHAPE;
            };
            if (kind == Kind.SERVICE && !shape.getId().equals(service.getId())) {
                throw new IllegalStateException("Service " + shape.getId() + " is in the closure of "
                                                + service.getId());
            }
            kinds.put(shape.getId(), kind);
        }
    }

    /**
     * Strongly connected components of the member-target graph of the type and shape schemas, in dependency order
     * (iterative Tarjan over shapes in shape-ID order and members in modeled order): every member target outside a
     * component is in an earlier one.
     */
    private List<List<ShapeId>> components() {
        Map<ShapeId, Integer> index = new HashMap<>();
        Map<ShapeId, Integer> low = new HashMap<>();
        Deque<ShapeId> stack = new ArrayDeque<>();
        Set<ShapeId> onStack = new HashSet<>();
        List<List<ShapeId>> components = new ArrayList<>();
        record Frame(ShapeId node, List<ShapeId> successors, int[] next) {
        }
        for (Map.Entry<ShapeId, Kind> entry : kinds.entrySet()) {
            ShapeId start = entry.getKey();
            if (entry.getValue() == Kind.SERVICE || entry.getValue() == Kind.OPERATION || index.containsKey(start)) {
                continue;
            }
            Deque<Frame> work = new ArrayDeque<>();
            visit(start, index, low, stack, onStack);
            work.push(new Frame(start, successors(start), new int[1]));
            while (!work.isEmpty()) {
                Frame frame = work.peek();
                ShapeId node = frame.node();
                if (frame.next()[0] < frame.successors().size()) {
                    ShapeId successor = frame.successors().get(frame.next()[0]++);
                    if (!index.containsKey(successor)) {
                        visit(successor, index, low, stack, onStack);
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
                    component.sort(null);
                    components.add(List.copyOf(component));
                }
            }
        }
        return components;
    }

    private static void visit(ShapeId node, Map<ShapeId, Integer> index, Map<ShapeId, Integer> low,
                              Deque<ShapeId> stack, Set<ShapeId> onStack) {
        index.put(node, index.size());
        low.put(node, index.get(node));
        stack.push(node);
        onStack.add(node);
    }

    /** Member targets that get a schema (not Smithy Java prelude schemas), in modeled member order. */
    private List<ShapeId> successors(ShapeId node) {
        Shape shape = shape(node);
        if (!isAggregate(shape)) {
            return List.of();
        }
        List<ShapeId> result = new ArrayList<>();
        for (MemberShape member : shape.members()) {
            if (kinds.containsKey(member.getTarget())) {
                result.add(member.getTarget());
            }
        }
        return result;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Schemas.

    /** Builds the schemas of one component, whose member targets outside it are already built. */
    private void build(List<ShapeId> component) {
        ShapeId first = component.get(0);
        boolean recursive = component.size() > 1 || successors(first).contains(first);
        if (!recursive) {
            Shape shape = shape(first);
            if (isAggregate(shape)) {
                SchemaBuilder builder = builder(shape);
                for (MemberShape member : shape.members()) {
                    builder.putMember(member.getMemberName(), built(member.getTarget()), runtimeTraits(member));
                }
                schemas.put(first, builder.build());
            } else {
                schemas.put(first, simple(shape));
            }
            return;
        }
        Map<ShapeId, SchemaBuilder> builders = new TreeMap<>();
        component.forEach(id -> builders.put(id, builder(shape(id))));
        for (Map.Entry<ShapeId, SchemaBuilder> entry : builders.entrySet()) {
            for (MemberShape member : shape(entry.getKey()).members()) {
                SchemaBuilder cycleTarget = builders.get(member.getTarget());
                if (cycleTarget != null) {
                    entry.getValue().putMember(member.getMemberName(), cycleTarget, runtimeTraits(member));
                } else {
                    entry.getValue().putMember(member.getMemberName(), built(member.getTarget()),
                                               runtimeTraits(member));
                }
            }
        }
        builders.forEach((id, builder) -> schemas.put(id, builder.build().resolve()));
    }

    /** {@code Schema.structureBuilder(id, traits)} and its union, list and map forms. */
    private SchemaBuilder builder(Shape shape) {
        ShapeId id = shape.getId();
        Trait[] traits = runtimeTraits(shape);
        return switch (shape.getType()) {
            case STRUCTURE -> documents(Schema.structureBuilder(id, traits), id);
            case UNION -> documents(Schema.unionBuilder(id, traits), id);
            case LIST, SET -> Schema.listBuilder(id, traits);
            case MAP -> Schema.mapBuilder(id, traits);
            default -> throw new IllegalStateException("No schema builder for " + shape.getType() + " " + id);
        };
    }

    /**
     * Attaches a schema-guided {@code StructDocument} builder supplier to a structure or union builder. The supplier
     * refers to the built schema, which only exists once the builder is built (and, in a cycle, resolved), so its
     * reference is filled in by {@link #convert()} once every schema is built.
     */
    private SchemaBuilder documents(SchemaBuilder builder, ShapeId id) {
        ShapeId serviceId = service.getId();
        AtomicReference<Schema> self = new AtomicReference<>();
        selfReferences.put(id, self);
        return builder.builderSupplier(() -> SchemaConverter.createDocumentBuilder(self.get(), serviceId));
    }

    private Schema simple(Shape shape) {
        ShapeId id = shape.getId();
        Trait[] traits = runtimeTraits(shape);
        return switch (shape.getType()) {
            case BLOB -> Schema.createBlob(id, traits);
            case BOOLEAN -> Schema.createBoolean(id, traits);
            case STRING -> Schema.createString(id, traits);
            case BYTE -> Schema.createByte(id, traits);
            case SHORT -> Schema.createShort(id, traits);
            case INTEGER -> Schema.createInteger(id, traits);
            case LONG -> Schema.createLong(id, traits);
            case FLOAT -> Schema.createFloat(id, traits);
            case DOUBLE -> Schema.createDouble(id, traits);
            case BIG_INTEGER -> Schema.createBigInteger(id, traits);
            case BIG_DECIMAL -> Schema.createBigDecimal(id, traits);
            case TIMESTAMP -> Schema.createTimestamp(id, traits);
            case DOCUMENT -> Schema.createDocument(id, traits);
            case ENUM -> {
                Set<String> values = new LinkedHashSet<>();
                shape.members().forEach(m -> values.add(m.expectTrait(EnumValueTrait.class).expectStringValue()));
                yield Schema.createEnum(id, values, traits);
            }
            case INT_ENUM -> {
                Set<Integer> values = new LinkedHashSet<>();
                shape.members().forEach(m -> values.add(m.expectTrait(EnumValueTrait.class).expectIntValue()));
                yield Schema.createIntEnum(id, values, traits);
            }
            default -> throw new IllegalStateException("No schema factory for " + shape.getType() + " " + id);
        };
    }

    /** The schema of an already built member target. */
    private Schema built(ShapeId target) {
        Schema prelude = prelude(target);
        if (prelude != null) {
            return prelude;
        }
        Schema schema = schemas.get(target);
        if (schema == null) {
            throw new IllegalStateException("Member target " + target + " of the closure of " + service.getId()
                                            + " has no schema");
        }
        return schema;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Index.

    private ServiceSchemaIndex index(Schema serviceSchema) {
        ServiceSchemaIndex.Builder index = ServiceSchemaIndex.builder(serviceSchema);
        unknownTraits(index, service);
        for (Map.Entry<ShapeId, Kind> entry : kinds.entrySet()) {
            ShapeId id = entry.getKey();
            Schema schema = schemas.get(id);
            Shape shape = shape(id);
            switch (entry.getValue()) {
                case OPERATION -> index.operation(schema);
                case TYPE -> index.type(schema);
                case SHAPE -> index.shape(schema);
                default -> {
                    continue;
                }
            }
            List<MemberShape> members = List.copyOf(shape.members());
            if ((shape.isStructureShape() || shape.isUnionShape()) && members.size() > 1) {
                index.memberOrder(schema, members.stream().map(MemberShape::getMemberName).toArray(String[]::new));
            }
            if (shape.isEnumShape() || shape.isIntEnumShape()) {
                for (MemberShape member : members) {
                    index.enumMember(schema, member.getMemberName(), runtimeTraits(member));
                }
            }
            unknownTraits(index, shape);
            members.forEach(member -> unknownTraits(index, member));
        }
        return index.build();
    }

    private static void unknownTraits(ServiceSchemaIndex.Builder index, Shape shape) {
        List<DynamicTrait> unknown = new ArrayList<>();
        for (Trait trait : new TreeMap<>(shape.getAllTraits()).values()) {
            if (trait instanceof DynamicTrait dynamic) {
                unknown.add(dynamic);
            }
        }
        if (!unknown.isEmpty()) {
            index.unknownTraits(shape.getId(), unknown.toArray(DynamicTrait[]::new));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers.

    /** The runtime-relevant traits applied directly to {@code shape}, sorted by trait ID. */
    static Trait[] runtimeTraits(Shape shape) {
        List<Trait> result = new ArrayList<>();
        for (Trait trait : new TreeMap<>(shape.getAllTraits()).values()) {
            if (!(trait instanceof DynamicTrait) && RUNTIME_TRAITS.contains(trait.toShapeId())) {
                result.add(trait);
            }
        }
        return result.toArray(Trait[]::new);
    }

    private static Schema prelude(ShapeId id) {
        if (!id.getNamespace().equals(PRELUDE_NAMESPACE) || id.hasMember()) {
            return null;
        }
        return PRELUDE.get(id.getName());
    }

    private Shape shape(ShapeId id) {
        return model.expectShape(id);
    }

    private static boolean isAggregate(Shape shape) {
        return switch (shape.getType()) {
            case STRUCTURE, UNION, LIST, SET, MAP -> true;
            default -> false;
        };
    }
}
