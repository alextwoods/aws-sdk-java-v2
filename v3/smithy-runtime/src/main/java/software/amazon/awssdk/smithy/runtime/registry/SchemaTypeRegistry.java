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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import software.amazon.awssdk.smithy.runtime.document.SchemaDocuments;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A Smithy Java {@link TypeRegistry} whose entries map each exact shape ID to the type's schema, Java class and
 * builder factory ({@link RegisteredType}; Requirements 6.5-6.7, 6.9).
 *
 * <p>Smithy Java's own registry builder maps an ID to a class and builder only, keeps the last of two registrations
 * of one ID, and has no schema. This registry implements the same interface, so it works wherever Smithy Java
 * expects a {@code TypeRegistry} (for example {@link #deserialize} constructs a discriminated document's type through
 * its registered builder), and adds exact entry and schema lookup and strict registration.
 *
 * <p><b>Lazy partitions.</b> A generated service registers its types in partitions. Each partition declares its
 * shape IDs up front and supplies its entries on first use: {@link #contains} and {@link #shapeIds} never load a
 * partition, and a lookup loads (once) only the partition that declares the ID, so looking up one type does not
 * initialize the entries of every other type. A loaded partition must supply exactly one entry for each declared ID,
 * otherwise the lookup fails with an {@link IllegalStateException} naming the IDs.
 *
 * <p><b>Typed construction.</b> {@link #deserialize} and {@link #deserializeStrict} construct the registered type
 * named by a document's discriminator through its registered builder (Requirement 6.9), reading the document with
 * {@link SchemaDocuments}' schema checks (so a document incompatible with the registered schema fails with a
 * {@code DocumentCoercionException}). A document without a discriminator fails with
 * {@link MissingDiscriminatorException} and one whose discriminator is not registered with
 * {@link UnknownDiscriminatorException} (Requirements 6.11 and 6.12).
 *
 * <p><b>Composition.</b> {@link #compose} unions registries without copying entries (Requirement 6.8): the composed
 * registry shares the component registries' partitions, so a lookup returns the very {@link RegisteredType}, schema,
 * class and builder factory objects the component registered, and loading a partition through either registry loads
 * it once for both. The schema, class and factory a lookup returns never depend on the order of the components, and
 * composition rejects two different entries for one
 * shape ID with a deterministic {@link TypeRegistryConflictException} (Requirement 6.13). Use it instead of Smithy
 * Java's {@code TypeRegistry.compose}, which silently prefers the first registry.
 *
 * <p>Lookups are by exact absolute shape ID; they never load classes by name, use reflection, or read a model.
 * Instances are immutable (apart from loading partitions once) and safe for concurrent use.
 */
public final class SchemaTypeRegistry implements TypeRegistry {

    private static final SchemaTypeRegistry EMPTY = new SchemaTypeRegistry(Map.of());

    private final Map<ShapeId, Partition> partitions;
    private final SortedSet<ShapeId> shapeIds;

    private SchemaTypeRegistry(Builder builder) {
        this(builder.partitions);
    }

    private SchemaTypeRegistry(Map<ShapeId, Partition> partitions) {
        this.partitions = Map.copyOf(partitions);
        this.shapeIds = Collections.unmodifiableSortedSet(new TreeSet<>(partitions.keySet()));
    }

    /** A builder of a registry. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Composes registries into one that registers the union of their types (Requirement 6.8).
     *
     * <p>The composed registry references the components' partitions and entries; nothing is copied, and every
     * lookup returns the component's own entry, schema, class and builder factory objects. Registries that
     * register disjoint types compose without loading any partition. A shape ID registered by several components is
     * accepted when they register the identical entry, or entries with the identical schema, class and builder
     * factory objects; the composed lookup then returns the first such component's entry, whose schema, class and
     * factory are the same objects whatever the order of {@code registries}. Comparing those entries loads their
     * partitions.
     *
     * @param registries the registries to compose; composing one returns it, composing none returns an empty
     *        registry
     * @throws TypeRegistryConflictException if two registries register different entries for one shape ID; it lists
     *         every such conflict
     */
    public static SchemaTypeRegistry compose(SchemaTypeRegistry... registries) {
        return compose(List.of(registries));
    }

    /**
     * Composes registries into one that registers the union of their types; see {@link #compose(SchemaTypeRegistry...)}.
     */
    public static SchemaTypeRegistry compose(List<SchemaTypeRegistry> registries) {
        List<SchemaTypeRegistry> components = List.copyOf(registries);
        if (components.isEmpty()) {
            return EMPTY;
        }
        if (components.size() == 1) {
            return components.get(0);
        }
        Map<ShapeId, Partition> merged = new HashMap<>();
        Map<ShapeId, Integer> owners = new HashMap<>();
        List<TypeRegistryConflictException.Conflict> conflicts = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            SchemaTypeRegistry component = components.get(i);
            for (ShapeId id : component.shapeIds) {
                Partition partition = component.partitions.get(id);
                Integer owner = owners.putIfAbsent(id, i);
                if (owner == null) {
                    merged.put(id, partition);
                    continue;
                }
                if (merged.get(id) == partition) {
                    // The same partition, for example when a composition is composed with one of its components.
                    continue;
                }
                RegisteredType<?> first = merged.get(id).entries().get(id);
                RegisteredType<?> second = partition.entries().get(id);
                if (!sameEntry(first, second)) {
                    conflicts.add(new TypeRegistryConflictException.Conflict(
                        id, owner, source(owner, components.get(owner)), first, i, source(i, component), second));
                }
            }
        }
        if (!conflicts.isEmpty()) {
            conflicts.sort(Comparator.comparing(TypeRegistryConflictException.Conflict::shapeId)
                                     .thenComparingInt(TypeRegistryConflictException.Conflict::firstRegistry)
                                     .thenComparingInt(TypeRegistryConflictException.Conflict::secondRegistry));
            throw new TypeRegistryConflictException(conflicts);
        }
        return new SchemaTypeRegistry(merged);
    }

    private static boolean sameEntry(RegisteredType<?> first, RegisteredType<?> second) {
        return first == second
               || first.schema() == second.schema()
                  && first.type() == second.type()
                  && first.builderFactory() == second.builderFactory();
    }

    /** A deterministic description of a component: its position and the namespaces it registers. */
    private static String source(int position, SchemaTypeRegistry registry) {
        SortedSet<String> namespaces = new TreeSet<>();
        for (ShapeId id : registry.shapeIds) {
            namespaces.add(id.getNamespace());
        }
        return "registry #" + position + " (namespaces " + namespaces + ")";
    }

    /** The shape IDs of every registered type, sorted; reading them loads no partition. */
    public SortedSet<ShapeId> shapeIds() {
        return shapeIds;
    }

    /**
     * The entry registered for exactly the absolute shape ID {@code shapeId}, loading its partition if needed.
     *
     * @throws IllegalStateException if the partition declaring {@code shapeId} does not supply exactly its entries
     */
    public Optional<RegisteredType<?>> entry(ShapeId shapeId) {
        Partition partition = shapeId == null ? null : partitions.get(shapeId);
        return partition == null ? Optional.empty() : Optional.of(partition.entries().get(shapeId));
    }

    /** The schema registered for exactly {@code shapeId}: the same object as the generated type's {@code SCHEMA}. */
    public Optional<Schema> schema(ShapeId shapeId) {
        return entry(shapeId).map(RegisteredType::schema);
    }

    /** Every entry, sorted by shape ID; loads every partition. */
    public List<RegisteredType<?>> entries() {
        List<RegisteredType<?>> result = new ArrayList<>(shapeIds.size());
        for (ShapeId id : shapeIds) {
            result.add(partitions.get(id).entries().get(id));
        }
        return List.copyOf(result);
    }

    @Override
    public Class<? extends SerializableStruct> getShapeClass(ShapeId shapeId) {
        return entry(shapeId).<Class<? extends SerializableStruct>>map(RegisteredType::type).orElse(null);
    }

    @Override
    public ShapeBuilder<?> createBuilder(ShapeId shapeId) {
        return entry(shapeId).<ShapeBuilder<?>>map(RegisteredType::createBuilder).orElse(null);
    }

    /** Whether a type is registered for exactly {@code shapeId}; loads no partition. */
    @Override
    public boolean contains(ShapeId shapeId) {
        return shapeId != null && partitions.containsKey(shapeId);
    }

    /**
     * Constructs the registered type named by {@code document}'s discriminator through its registered builder, reading
     * the document against the registered schema, and builds it with error correction (for non-authoritative
     * consumers such as clients).
     *
     * @throws MissingDiscriminatorException if the document has no discriminator
     * @throws UnknownDiscriminatorException if no type is registered for the discriminator
     * @throws software.amazon.smithy.java.core.serde.document.DiscriminatorException if the discriminator is not a
     *         valid shape ID
     * @throws software.amazon.awssdk.smithy.runtime.document.DocumentCoercionException if the document cannot
     *         represent a value of the registered schema
     */
    @Override
    public SerializableStruct deserialize(Document document) {
        return SchemaDocuments.toShape(document, registeredBuilder(document));
    }

    /**
     * Like {@link #deserialize}, but builds without error correction, so a generated builder rejects missing required
     * members.
     */
    @Override
    public SerializableStruct deserializeStrict(Document document) {
        return SchemaDocuments.toShapeStrict(document, registeredBuilder(document));
    }

    private ShapeBuilder<? extends SerializableStruct> registeredBuilder(Document document) {
        Objects.requireNonNull(document, "document");
        ShapeId discriminator = document.discriminator();
        if (discriminator == null) {
            throw new MissingDiscriminatorException(document.type());
        }
        RegisteredType<?> entry = entry(discriminator)
            .orElseThrow(() -> new UnknownDiscriminatorException(discriminator));
        return entry.createBuilder();
    }

    @Override
    public String toString() {
        return "SchemaTypeRegistry[" + shapeIds.size() + " types]";
    }

    /** One lazily loaded partition: its declared shape IDs and the supplier of its entries. */
    private static final class Partition {
        private final Set<ShapeId> declared;
        private final Supplier<? extends List<? extends RegisteredType<?>>> source;
        private volatile Map<ShapeId, RegisteredType<?>> entries;

        private Partition(Set<ShapeId> declared, Supplier<? extends List<? extends RegisteredType<?>>> source) {
            this.declared = declared;
            this.source = source;
        }

        private Map<ShapeId, RegisteredType<?>> entries() {
            Map<ShapeId, RegisteredType<?>> result = entries;
            if (result == null) {
                synchronized (this) {
                    result = entries;
                    if (result == null) {
                        result = load();
                        entries = result;
                    }
                }
            }
            return result;
        }

        private Map<ShapeId, RegisteredType<?>> load() {
            List<? extends RegisteredType<?>> supplied = Objects.requireNonNull(source.get(), "partition entries");
            Map<ShapeId, RegisteredType<?>> byId = new HashMap<>();
            SortedSet<ShapeId> duplicates = new TreeSet<>();
            for (RegisteredType<?> entry : supplied) {
                Objects.requireNonNull(entry, "partition entry");
                if (byId.putIfAbsent(entry.shapeId(), entry) != null) {
                    duplicates.add(entry.shapeId());
                }
            }
            SortedSet<ShapeId> missing = new TreeSet<>(declared);
            missing.removeAll(byId.keySet());
            SortedSet<ShapeId> undeclared = new TreeSet<>(byId.keySet());
            undeclared.removeAll(declared);
            if (!duplicates.isEmpty() || !missing.isEmpty() || !undeclared.isEmpty()) {
                throw new IllegalStateException("A type registry partition does not supply exactly its declared "
                                                + "types: duplicate " + duplicates + ", missing " + missing
                                                + ", undeclared " + undeclared);
            }
            return Map.copyOf(byId);
        }
    }

    /** Builder of {@link SchemaTypeRegistry}. */
    public static final class Builder {
        private final Map<ShapeId, Partition> partitions = new HashMap<>();

        private Builder() {
        }

        /**
         * Registers a partition that declares {@code shapeIds} and supplies their entries on first use.
         *
         * @param shapeIds the absolute shape IDs of the partition's types
         * @param entries supplies exactly one entry per declared ID; called at most once, when one of the IDs is
         *        first looked up
         * @throws IllegalArgumentException if an ID is a member ID or is declared twice (in this or another
         *         partition)
         */
        public Builder partition(List<ShapeId> shapeIds, Supplier<? extends List<? extends RegisteredType<?>>> entries) {
            Objects.requireNonNull(shapeIds, "shapeIds");
            Objects.requireNonNull(entries, "entries");
            Set<ShapeId> declared = new LinkedHashSet<>();
            for (ShapeId id : shapeIds) {
                Objects.requireNonNull(id, "shapeId");
                if (id.hasMember()) {
                    throw new IllegalArgumentException("A type registry registers shapes, not the member " + id);
                }
                if (!declared.add(id) || partitions.containsKey(id)) {
                    throw new IllegalArgumentException("Type " + id + " is registered more than once");
                }
            }
            Partition partition = new Partition(Set.copyOf(declared), entries);
            declared.forEach(id -> partitions.put(id, partition));
            return this;
        }

        /** Builds the registry. */
        public SchemaTypeRegistry build() {
            return new SchemaTypeRegistry(this);
        }
    }
}
