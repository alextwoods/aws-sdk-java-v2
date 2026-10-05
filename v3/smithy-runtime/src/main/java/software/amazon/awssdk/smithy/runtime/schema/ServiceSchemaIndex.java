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

package software.amazon.awssdk.smithy.runtime.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaBuilder;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * The exact-ID view of every schema a generated service defines, with the modeled facts that Smithy Java's
 * {@code Schema} does not carry (Requirements 2.11, 4.5-4.7, 14.17).
 *
 * <p>A generated service creates one index lazily, from its generated {@code <Service>Schemas} holder, and registers:
 * <ul>
 *   <li>the service schema and one operation schema per operation;</li>
 *   <li>{@linkplain Builder#type the exported schema} of every public model type (structures, including requests,
 *       responses and errors, unions, enums and intEnums), exactly one per type;</li>
 *   <li>{@linkplain Builder#shape the non-exported schemas} of the other shapes the types' members target (lists,
 *       maps and simple shapes);</li>
 *   <li>the modeled member order of structures and unions; Smithy Java orders a structure's {@code members()} with
 *       required members that have no default first, which is not always the modeled order;</li>
 *   <li>one member schema per enum and intEnum member, which Smithy Java's enum schemas do not have: each is a member
 *       schema with the exact member ID (for example {@code example#Color$RED}) that targets {@code smithy.api#Unit}
 *       and carries the member's runtime traits, including its {@code smithy.api#enumValue};</li>
 *   <li>the unknown traits of every shape and member, keyed by exact trait ID ({@link UnknownTraits}).</li>
 * </ul>
 *
 * <p>Lookups are by exact absolute shape or member ID and never involve reflection, class names or a Smithy model.
 * Member targets in the {@code smithy.api} prelude (Smithy Java's shared prelude schemas) are indexed as reached.
 * {@link #build} checks the registrations for consistency, so a generator defect fails at the service's first use
 * rather than producing a partial view.
 *
 * <p>Instances are immutable and safe for concurrent use.
 */
public final class ServiceSchemaIndex {

    private static final String PRELUDE_NAMESPACE = "smithy.api";
    private static final Comparator<Schema> BY_ID = Comparator.comparing(Schema::id);

    private final Schema service;
    private final List<Schema> operations;
    private final List<Schema> types;
    private final List<Schema> shapes;
    private final Map<ShapeId, Schema> schemas;
    private final Map<ShapeId, List<Schema>> members;
    private final SortedMap<ShapeId, UnknownTraits> unknownTraits;

    private ServiceSchemaIndex(Builder builder) {
        this.service = builder.service;
        this.operations = sorted(builder.operations);
        this.types = sorted(builder.types);
        this.shapes = sorted(builder.shapes);

        Map<ShapeId, Schema> byId = new HashMap<>();
        List<Schema> roots = new ArrayList<>();
        roots.add(service);
        roots.addAll(operations);
        roots.addAll(types);
        roots.addAll(shapes);
        roots.forEach(root -> put(byId, root));

        Map<ShapeId, List<Schema>> memberLists = new HashMap<>();
        Map<ShapeId, List<String>> memberOrders = new HashMap<>(builder.memberOrders);
        List<Schema> pending = new ArrayList<>(roots);
        while (!pending.isEmpty()) {
            Schema root = pending.remove(pending.size() - 1);
            for (Schema member : root.members()) {
                put(byId, member);
                Schema target = member.memberTarget();
                Schema indexed = byId.get(target.id());
                if (indexed == null) {
                    if (!target.id().getNamespace().equals(PRELUDE_NAMESPACE)) {
                        throw new IllegalStateException("Member " + member.id() + " targets " + target.id()
                                                        + ", which is not registered in the index of " + service.id());
                    }
                    put(byId, target);
                    pending.add(target);
                } else if (indexed != target) {
                    throw new IllegalStateException("Member " + member.id() + " targets a different " + target.id()
                                                    + " schema than the one registered in the index");
                }
            }
            memberLists.put(root.id(), orderedMembers(root, memberOrders.remove(root.id())));
        }
        if (!memberOrders.isEmpty()) {
            throw new IllegalStateException("Member order registered for unindexed schemas "
                                            + new TreeMap<>(memberOrders).keySet());
        }

        for (Map.Entry<ShapeId, EnumMembers> entry : builder.enumMembers.entrySet()) {
            Schema enumSchema = byId.get(entry.getKey());
            if (enumSchema != entry.getValue().enumSchema) {
                throw new IllegalStateException("Enum members registered for unindexed schema " + entry.getKey());
            }
            List<Schema> enumMembers = entry.getValue().build();
            enumMembers.forEach(member -> put(byId, member));
            memberLists.put(entry.getKey(), enumMembers);
            // Enum members target smithy.api#Unit, which is indexed like any other reached prelude shape.
            put(byId, Unit.SCHEMA);
            memberLists.putIfAbsent(Unit.SCHEMA.id(), List.of());
        }
        for (Schema root : roots) {
            if (isEnum(root) && !builder.enumMembers.containsKey(root.id())) {
                throw new IllegalStateException("No member schemas are registered for " + root.type() + " "
                                                + root.id());
            }
        }

        TreeMap<ShapeId, UnknownTraits> unknown = new TreeMap<>();
        builder.unknownTraits.forEach((target, traits) -> {
            if (!byId.containsKey(target)) {
                throw new IllegalStateException("Unknown traits registered for " + target + ", which has no schema "
                                                + "in the index of " + service.id());
            }
            unknown.put(target, traits);
        });

        this.schemas = Map.copyOf(byId);
        this.members = Map.copyOf(memberLists);
        this.unknownTraits = Collections.unmodifiableSortedMap(unknown);
    }

    /**
     * A builder for the index of the service described by {@code service}.
     *
     * @throws IllegalArgumentException if {@code service} is not a service schema
     */
    public static Builder builder(Schema service) {
        return new Builder(service);
    }

    /** The service schema. */
    public Schema service() {
        return service;
    }

    /** The operation schemas, sorted by absolute shape ID. */
    public List<Schema> operations() {
        return operations;
    }

    /** The exported schema of every public model type, exactly one per type, sorted by absolute shape ID. */
    public List<Schema> types() {
        return types;
    }

    /** The non-exported schemas of the other shapes that members target, sorted by absolute shape ID. */
    public List<Schema> shapes() {
        return shapes;
    }

    /**
     * The schema with exactly the absolute shape or member ID {@code id}: the service, an operation, a shape, a
     * structure, union, list or map member, an enum or intEnum member, or a reached prelude shape.
     */
    public Optional<Schema> schema(ShapeId id) {
        return Optional.ofNullable(schemas.get(id));
    }

    /**
     * The member schemas of {@code schema} (of its target, for a member schema) in modeled order, including the member
     * schemas of an enum or intEnum.
     *
     * @throws IllegalArgumentException if the schema is not in this index
     */
    public List<Schema> members(Schema schema) {
        Schema root = schema.isMember() ? schema.memberTarget() : schema;
        List<Schema> result = members.get(root.id());
        if (result == null || schemas.get(root.id()) != root) {
            throw new IllegalArgumentException(root.id() + " is not a schema of the index of " + service.id());
        }
        return result;
    }

    /** The unknown traits of the shape or member with exactly the absolute ID {@code target}. */
    public UnknownTraits unknownTraits(ShapeId target) {
        return unknownTraits.getOrDefault(target, UnknownTraits.EMPTY);
    }

    /** The unknown traits of {@code schema}, looked up by its exact shape or member ID. */
    public UnknownTraits unknownTraits(Schema schema) {
        return unknownTraits(schema.id());
    }

    /** Every shape and member that has at least one unknown trait, sorted by absolute ID; unmodifiable. */
    public SortedMap<ShapeId, UnknownTraits> unknownTraits() {
        return unknownTraits;
    }

    @Override
    public String toString() {
        return "ServiceSchemaIndex[" + service.id() + "]";
    }

    private static List<Schema> sorted(Map<ShapeId, Schema> schemas) {
        List<Schema> result = new ArrayList<>(schemas.values());
        result.sort(BY_ID);
        return List.copyOf(result);
    }

    private static void put(Map<ShapeId, Schema> byId, Schema schema) {
        Schema previous = byId.putIfAbsent(schema.id(), schema);
        if (previous != null && previous != schema) {
            throw new IllegalStateException("Two schemas have the ID " + schema.id());
        }
    }

    private static List<Schema> orderedMembers(Schema root, List<String> order) {
        List<Schema> declared = root.members();
        if (order == null) {
            return List.copyOf(declared);
        }
        if (root.type() != ShapeType.STRUCTURE && root.type() != ShapeType.UNION) {
            throw new IllegalStateException("Member order registered for " + root.type() + " " + root.id());
        }
        Set<String> names = new HashSet<>(order);
        if (names.size() != order.size() || order.size() != declared.size()) {
            throw new IllegalStateException("Member order " + order + " of " + root.id() + " does not list each of its "
                                            + declared.size() + " members exactly once");
        }
        List<Schema> result = new ArrayList<>(order.size());
        for (String name : order) {
            Schema member = root.member(name);
            if (member == null) {
                throw new IllegalStateException("Member order of " + root.id() + " names unknown member " + name);
            }
            result.add(member);
        }
        return List.copyOf(result);
    }

    private static boolean isEnum(Schema schema) {
        return schema.type() == ShapeType.ENUM || schema.type() == ShapeType.INT_ENUM;
    }

    /** Builder of {@link ServiceSchemaIndex}. Every registration is checked again, as a whole, by {@link #build}. */
    public static final class Builder {
        private final Schema service;
        private final Map<ShapeId, Schema> operations = new HashMap<>();
        private final Map<ShapeId, Schema> types = new HashMap<>();
        private final Map<ShapeId, Schema> shapes = new HashMap<>();
        private final Map<ShapeId, List<String>> memberOrders = new HashMap<>();
        private final Map<ShapeId, EnumMembers> enumMembers = new LinkedHashMap<>();
        private final Map<ShapeId, UnknownTraits> unknownTraits = new HashMap<>();
        private final Set<ShapeId> registered = new HashSet<>();

        private Builder(Schema service) {
            Objects.requireNonNull(service, "service");
            if (service.type() != ShapeType.SERVICE) {
                throw new IllegalArgumentException("Not a service schema: " + service.type() + " " + service.id());
            }
            this.service = service;
            registered.add(service.id());
        }

        /**
         * Registers an operation schema.
         *
         * @throws IllegalArgumentException if it is not an operation schema or its ID is already registered
         */
        public Builder operation(Schema operation) {
            requireRoot(operation, ShapeType.OPERATION);
            operations.put(operation.id(), operation);
            return this;
        }

        /**
         * Registers the exported schema of a public model type: a structure, union, enum or intEnum.
         *
         * @throws IllegalArgumentException if it is another kind of schema or its ID is already registered
         */
        public Builder type(Schema type) {
            requireRoot(type, ShapeType.STRUCTURE, ShapeType.UNION, ShapeType.ENUM, ShapeType.INT_ENUM);
            types.put(type.id(), type);
            return this;
        }

        /**
         * Registers the non-exported schema of a shape that members target: a list, map or simple shape.
         *
         * @throws IllegalArgumentException if it is a member, service or operation schema, or its ID is already
         *         registered
         */
        public Builder shape(Schema shape) {
            requireRoot(shape);
            if (shape.type() == ShapeType.SERVICE || shape.type() == ShapeType.OPERATION) {
                throw new IllegalArgumentException("Register " + shape.id() + " as the service or an operation");
            }
            shapes.put(shape.id(), shape);
            return this;
        }

        /**
         * Records the modeled member order of a structure or union.
         *
         * @throws IllegalArgumentException if an order was already recorded for it
         */
        public Builder memberOrder(Schema aggregate, String... memberNames) {
            Objects.requireNonNull(aggregate, "aggregate");
            if (memberOrders.putIfAbsent(aggregate.id(), List.of(memberNames)) != null) {
                throw new IllegalArgumentException("Member order of " + aggregate.id() + " is recorded twice");
            }
            return this;
        }

        /**
         * Appends, in modeled order, the member schema {@code <enum ID>$<name>} of an enum or intEnum member with its
         * runtime traits (which must include its {@code smithy.api#enumValue}).
         *
         * @throws IllegalArgumentException if {@code enumSchema} is not an enum or intEnum schema, the member was
         *         already added, or the traits have no enum value
         */
        public Builder enumMember(Schema enumSchema, String name, Trait... traits) {
            Objects.requireNonNull(enumSchema, "enumSchema");
            Objects.requireNonNull(name, "name");
            if (enumSchema.isMember() || !isEnum(enumSchema)) {
                throw new IllegalArgumentException("Not an enum or intEnum schema: " + enumSchema.id());
            }
            EnumMembers members = enumMembers.computeIfAbsent(enumSchema.id(), id -> new EnumMembers(enumSchema));
            if (members.enumSchema != enumSchema) {
                throw new IllegalArgumentException("Two schemas have the ID " + enumSchema.id());
            }
            members.add(name, traits);
            return this;
        }

        /**
         * Records the unknown traits of the shape or member with exactly the absolute ID {@code target}.
         *
         * @throws IllegalArgumentException if unknown traits were already recorded for {@code target}, or two traits
         *         have the same trait ID
         */
        public Builder unknownTraits(ShapeId target, DynamicTrait... traits) {
            Objects.requireNonNull(target, "target");
            if (unknownTraits.putIfAbsent(target, UnknownTraits.of(traits)) != null) {
                throw new IllegalArgumentException("Unknown traits of " + target + " are recorded twice");
            }
            return this;
        }

        /**
         * Builds the index.
         *
         * @throws IllegalStateException if the registrations are inconsistent: a member targets an unregistered
         *         non-prelude schema, two schemas share an ID, an enum or intEnum has no or mismatched member schemas,
         *         a member order does not list the members exactly, or unknown traits target an unindexed ID
         */
        public ServiceSchemaIndex build() {
            return new ServiceSchemaIndex(this);
        }

        private void requireRoot(Schema schema, ShapeType... allowed) {
            Objects.requireNonNull(schema, "schema");
            if (schema.isMember()) {
                throw new IllegalArgumentException("Member schema " + schema.id() + " cannot be registered directly");
            }
            if (allowed.length > 0 && !List.of(allowed).contains(schema.type())) {
                throw new IllegalArgumentException(schema.type() + " schema " + schema.id() + " is not one of "
                                                   + List.of(allowed));
            }
            if (!registered.add(schema.id())) {
                throw new IllegalArgumentException("Schema " + schema.id() + " is registered twice");
            }
        }
    }

    /**
     * The member schemas of one enum or intEnum. Smithy Java's public API builds member schemas only through a
     * structure, union, list or map builder, so they are built through a structure builder carrying the enum's ID;
     * that structure itself is discarded. Each member schema therefore has the exact member ID, targets
     * {@code smithy.api#Unit} (as enum members do in the Smithy model) and carries exactly the given member traits.
     */
    private static final class EnumMembers {
        private final Schema enumSchema;
        private final List<String> names = new ArrayList<>();
        private final List<Trait[]> traits = new ArrayList<>();

        private EnumMembers(Schema enumSchema) {
            this.enumSchema = enumSchema;
        }

        private void add(String name, Trait[] memberTraits) {
            if (names.contains(name)) {
                throw new IllegalArgumentException("Enum member " + enumSchema.id().withMember(name)
                                                   + " is added twice");
            }
            EnumValueTrait value = null;
            for (Trait trait : memberTraits) {
                if (trait instanceof EnumValueTrait enumValue) {
                    value = enumValue;
                }
            }
            if (value == null) {
                throw new IllegalArgumentException("Enum member " + enumSchema.id().withMember(name)
                                                   + " has no smithy.api#enumValue trait");
            }
            boolean known = enumSchema.type() == ShapeType.ENUM
                            ? value.getStringValue().filter(enumSchema.stringEnumValues()::contains).isPresent()
                            : value.getIntValue().filter(enumSchema.intEnumValues()::contains).isPresent();
            if (!known) {
                throw new IllegalArgumentException("Enum member " + enumSchema.id().withMember(name) + " has value "
                                                   + value.toNode() + ", which is not a value of " + enumSchema.id());
            }
            names.add(name);
            traits.add(memberTraits.clone());
        }

        private List<Schema> build() {
            int values = enumSchema.type() == ShapeType.ENUM ? enumSchema.stringEnumValues().size()
                                                             : enumSchema.intEnumValues().size();
            if (values != names.size()) {
                throw new IllegalStateException(enumSchema.id() + " has " + values + " values but " + names.size()
                                                + " member schemas");
            }
            SchemaBuilder builder = Schema.structureBuilder(enumSchema.id());
            for (int i = 0; i < names.size(); i++) {
                builder.putMember(names.get(i), Unit.SCHEMA, traits.get(i));
            }
            List<Schema> built = builder.build().members();
            List<Schema> result = new ArrayList<>(names.size());
            for (String name : names) {
                result.add(built.stream().filter(m -> m.memberName().equals(name)).findFirst().orElseThrow());
            }
            return List.copyOf(result);
        }
    }
}
