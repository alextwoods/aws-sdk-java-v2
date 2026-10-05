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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 17 (Requirement 6.8): for any nonconflicting registries, their composition resolves the union of their
 * shape IDs while preserving each registered entry's schema, Java type and builder factory object identity.
 *
 * <p><b>Scenarios.</b> Each try builds one to five component registries over a pool of shape IDs that repeat names
 * across namespaces (so exact lookup is exercised). Every registered ID gets fresh structure or union schema, builder
 * factory and entry objects and one of three Java classes. An ID may be registered by several components, either with
 * the very same entry object or with a distinct entry holding the same schema, class and factory objects; both are
 * nonconflicting. Each component spreads its IDs over one to three lazy partitions that count their loads. The
 * components are composed in a shuffled order, and optionally a nested composition of the first two components is
 * composed together with one of them again (which shares that component's partitions).
 *
 * <p><b>Oracle.</b> The expected entry of an ID is computed independently of the registry: the entry of the first
 * component, in composition order, that registers it (recursively for a nested composition), which is the entry the
 * composition documents to return. Each check compares objects by identity:
 * <ul>
 *     <li>{@code shapeIds} is the sorted union and {@code contains} is true exactly for it (including IDs that differ
 *         only in namespace or are member IDs of registered shapes);</li>
 *     <li>composing and the non-loading queries load no partition whose IDs no other component registers;</li>
 *     <li>{@code entry}, {@code schema}, {@code getShapeClass}, {@code createBuilder} and {@code entries} return the
 *         expected entry and exactly its schema, class and factory (the builder is created by that factory);</li>
 *     <li>composing the same components in reverse order resolves the same schema, class and factory objects;</li>
 *     <li>every component still resolves its own entries, and no partition is loaded more than once, whichever
 *         registry the lookups go through.</li>
 * </ul>
 */
class RegistryCompositionPropertyTest {

    private static final List<String> NAMESPACES = List.of("com.example", "com.example.inner", "org.other");
    private static final List<String> NAMES = List.of("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta");
    private static final List<ShapeId> POOL =
        NAMESPACES.stream().flatMap(ns -> NAMES.stream().map(n -> ShapeId.fromParts(ns, n))).toList();
    private static final List<Class<? extends Probe>> CLASSES = List.of(ProbeA.class, ProbeB.class, ProbeC.class);
    private static final List<Function<Schema, Probe>> CONSTRUCTORS = List.of(ProbeA::new, ProbeB::new, ProbeC::new);
    private static final int MAX_COMPONENTS = 5;
    private static final int MAX_PARTITIONS = 3;

    // Feature: aws-sdk-java-v3, Property 17: Registry composition preserves identity
    // Embedded edge cases are disabled: they collapse to empty registries and single components. The statistics
    // coverage checks below fail the property if shared or nested compositions stop being generated.
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 17: Registry composition preserves identity")
    void registryCompositionPreservesIdentity(@ForAll("plans") Plan plan) {
        Scenario scenario = Scenario.build(plan);
        collectStatistics(scenario);

        SchemaTypeRegistry composed = SchemaTypeRegistry.compose(scenario.composition());
        if (scenario.composition().size() == 1) {
            assertSame(scenario.composition().get(0), composed, "composing one registry returns it");
        }

        // The union, and no partition loaded by composing or by the non-loading queries unless it must be compared.
        TreeSet<ShapeId> union = new TreeSet<>(scenario.canonical().keySet());
        assertEquals(union, composed.shapeIds());
        for (ShapeId id : POOL) {
            assertEquals(union.contains(id), composed.contains(id), id::toString);
            assertFalse(composed.contains(ShapeId.fromParts(id.getNamespace(), id.getName(), "member")));
        }
        for (Partition partition : scenario.partitions()) {
            if (!scenario.mayLoadOnCompose(partition)) {
                assertEquals(0, partition.loads.get(), () -> "composition loaded the partition " + partition.ids);
            }
        }

        // Every lookup returns the expected entry and its very schema, class and factory objects.
        Map<ShapeId, RegisteredType<?>> expected = scenario.expectedEntries(scenario.composition());
        for (ShapeId id : union) {
            Canonical canonical = scenario.canonical().get(id);
            RegisteredType<?> entry = composed.entry(id).orElseThrow();
            assertSame(expected.get(id), entry, id::toString);
            assertTrue(scenario.registeredEntries(id).containsKey(entry), "the entry is a registered object");
            assertSame(canonical.schema(), entry.schema());
            assertSame(canonical.type(), entry.type());
            assertSame(canonical.factory(), entry.builderFactory());
            assertSame(canonical.schema(), composed.schema(id).orElseThrow());
            assertSame(canonical.type(), composed.getShapeClass(id));
            ProbeBuilder builder = assertInstanceOf(ProbeBuilder.class, composed.createBuilder(id));
            assertSame(canonical.factory(), builder.factory, "the builder is created by the registered factory");
            assertSame(canonical.schema(), builder.schema());
            assertSame(canonical.type(), builder.build().getClass());
        }
        List<RegisteredType<?>> entries = composed.entries();
        assertEquals(union.size(), entries.size());
        int index = 0;
        for (ShapeId id : union) {
            assertSame(expected.get(id), entries.get(index++));
        }
        for (ShapeId id : POOL) {
            if (!union.contains(id)) {
                assertEquals(Optional.empty(), composed.entry(id));
                assertEquals(Optional.empty(), composed.schema(id));
                assertNull(composed.getShapeClass(id));
                assertNull(composed.createBuilder(id));
            }
        }

        // The order of the components never changes the resolved schema, class or factory.
        List<SchemaTypeRegistry> reversed = new ArrayList<>(scenario.composition());
        Collections.reverse(reversed);
        SchemaTypeRegistry reverseComposed = SchemaTypeRegistry.compose(reversed);
        assertEquals(union, reverseComposed.shapeIds());
        for (ShapeId id : union) {
            Canonical canonical = scenario.canonical().get(id);
            RegisteredType<?> entry = reverseComposed.entry(id).orElseThrow();
            assertTrue(scenario.registeredEntries(id).containsKey(entry), "the entry is a registered object");
            assertSame(canonical.schema(), entry.schema());
            assertSame(canonical.type(), entry.type());
            assertSame(canonical.factory(), entry.builderFactory());
        }

        // Components are unchanged, and partitions are shared: each is loaded at most once in total.
        for (Component component : scenario.components()) {
            assertEquals(new TreeSet<>(component.entries().keySet()), component.registry().shapeIds());
            for (Map.Entry<ShapeId, RegisteredType<?>> own : component.entries().entrySet()) {
                assertSame(own.getValue(), component.registry().entry(own.getKey()).orElseThrow());
            }
        }
        for (Partition partition : scenario.partitions()) {
            assertTrue(partition.loads.get() <= 1, () -> "partition " + partition.ids + " loaded "
                                                         + partition.loads.get() + " times");
        }
    }

    // Generators.

    @Provide
    Arbitrary<Plan> plans() {
        Arbitrary<Integer> componentCount = Arbitraries.integers().between(1, MAX_COMPONENTS);
        return componentCount.flatMap(count -> {
            Arbitrary<IdPlan> idPlan = Combinators.combine(
                Arbitraries.integers().between(0, POOL.size() - 1),
                Arbitraries.of(true, false),
                Arbitraries.integers().between(0, CLASSES.size() - 1),
                // Owners: mostly one component, sometimes several.
                Arbitraries.frequencyOf(
                    Tuple.of(3, Arbitraries.integers().between(0, count - 1).map(i -> 1 << i)),
                    Tuple.of(1, Arbitraries.integers().between(1, (1 << count) - 1))),
                Arbitraries.integers().between(0, (1 << MAX_COMPONENTS) - 1),
                Arbitraries.integers().between(0, MAX_PARTITIONS * MAX_COMPONENTS - 1))
                .as(IdPlan::new);
            return Combinators.combine(
                Arbitraries.just(count),
                idPlan.list().ofMaxSize(18).uniqueElements(IdPlan::poolIndex),
                Arbitraries.longs(),
                Arbitraries.of(true, false))
                .as(Plan::new);
        });
    }

    // Statistics.

    private static void collectStatistics(Scenario scenario) {
        boolean shared = scenario.canonical().keySet().stream().anyMatch(id -> scenario.owners(id) > 1);
        boolean equivalent = scenario.canonical().keySet().stream()
                                     .anyMatch(id -> scenario.registeredEntries(id).size() > 1);
        String kind = scenario.nested() ? "nested"
                                        : scenario.components().size() == 1 ? "single"
                                                                              : shared ? "shared" : "disjoint";
        Statistics.label("composition").collect(kind);
        Statistics.label("composition").coverage(c -> {
            c.check("single").percentage(p -> p >= 3);
            c.check("disjoint").percentage(p -> p >= 3);
            c.check("shared").percentage(p -> p >= 3);
            c.check("nested").percentage(p -> p >= 3);
        });
        Statistics.label("equivalent entries").collect(equivalent);
        Statistics.label("equivalent entries").coverage(c -> c.check(true).percentage(p -> p >= 5));
    }

    // Scenario values.

    /**
     * One registered ID: its pool index, whether its schema is a union, its class, the components that register it
     * (a bit mask), which extra owners register a distinct equivalent entry rather than the same entry (a bit mask)
     * and its partition slot.
     */
    record IdPlan(int poolIndex, boolean union, int classIndex, int ownerMask, int equivalentMask, int slot) {
    }

    /**
     * One try: the number of components, the registered IDs, the seed of the composition order and whether the first
     * two (shuffled) components are also composed into a nested composition that is composed with the second again.
     */
    record Plan(int componentCount, List<IdPlan> ids, long orderSeed, boolean nest) {
    }

    /** The one schema, class and factory objects of an ID. */
    record Canonical(Schema schema, Class<? extends Probe> type, Factory factory, RegisteredType<?> entry) {
    }

    /** A component registry and the entry objects it registers. */
    record Component(SchemaTypeRegistry registry, Map<ShapeId, RegisteredType<?>> entries, List<Partition> partitions) {
    }

    /** A lazy partition that counts its loads. */
    static final class Partition {
        final List<ShapeId> ids;
        final List<RegisteredType<?>> entries;
        final AtomicInteger loads = new AtomicInteger();

        Partition(List<ShapeId> ids, List<RegisteredType<?>> entries) {
            this.ids = ids;
            this.entries = entries;
        }

        List<RegisteredType<?>> load() {
            loads.incrementAndGet();
            return entries;
        }
    }

    /**
     * A built try: the canonical objects of every ID, the components, and the composition (the components in their
     * shuffled order, or a nested composition followed by the second and remaining components).
     */
    record Scenario(Map<ShapeId, Canonical> canonical, List<Component> components, List<SchemaTypeRegistry> composition,
                    Map<SchemaTypeRegistry, List<SchemaTypeRegistry>> nestedParts, boolean nested) {

        static Scenario build(Plan plan) {
            Map<ShapeId, Canonical> canonical = new TreeMap<>();
            List<Map<ShapeId, RegisteredType<?>>> owned = new ArrayList<>();
            List<Map<Integer, List<ShapeId>>> slots = new ArrayList<>();
            for (int i = 0; i < plan.componentCount(); i++) {
                owned.add(new LinkedHashMap<>());
                slots.add(new TreeMap<>());
            }
            for (IdPlan idPlan : plan.ids()) {
                ShapeId id = POOL.get(idPlan.poolIndex());
                Schema schema = idPlan.union()
                                ? Schema.unionBuilder(id).putMember("value", PreludeSchemas.STRING).build()
                                : Schema.structureBuilder(id).putMember("value", PreludeSchemas.INTEGER).build();
                Class<? extends Probe> type = CLASSES.get(idPlan.classIndex());
                Factory factory = new Factory(schema, CONSTRUCTORS.get(idPlan.classIndex()));
                RegisteredType<?> entry = entry(schema, type, factory);
                canonical.put(id, new Canonical(schema, type, factory, entry));
                boolean first = true;
                for (int component = 0; component < plan.componentCount(); component++) {
                    if ((idPlan.ownerMask() & (1 << component)) == 0) {
                        continue;
                    }
                    boolean equivalent = !first && (idPlan.equivalentMask() & (1 << component)) != 0;
                    owned.get(component).put(id, equivalent ? entry(schema, type, factory) : entry);
                    slots.get(component).computeIfAbsent(idPlan.slot() % MAX_PARTITIONS, s -> new ArrayList<>())
                         .add(id);
                    first = false;
                }
            }
            List<Component> components = new ArrayList<>();
            for (int i = 0; i < plan.componentCount(); i++) {
                SchemaTypeRegistry.Builder builder = SchemaTypeRegistry.builder();
                List<Partition> partitions = new ArrayList<>();
                for (List<ShapeId> ids : slots.get(i).values()) {
                    Map<ShapeId, RegisteredType<?>> own = owned.get(i);
                    Partition partition = new Partition(List.copyOf(ids), ids.stream().<RegisteredType<?>>map(own::get)
                                                                             .toList());
                    partitions.add(partition);
                    builder.partition(partition.ids, partition::load);
                }
                components.add(new Component(builder.build(), Map.copyOf(owned.get(i)), partitions));
            }
            List<Component> order = new ArrayList<>(components);
            Collections.shuffle(order, new Random(plan.orderSeed()));
            List<SchemaTypeRegistry> composition = order.stream().map(Component::registry)
                                                        .collect(Collectors.toCollection(ArrayList::new));
            Map<SchemaTypeRegistry, List<SchemaTypeRegistry>> nestedParts = new IdentityHashMap<>();
            boolean nested = plan.nest() && composition.size() >= 2;
            if (nested) {
                List<SchemaTypeRegistry> parts = List.of(composition.get(0), composition.get(1));
                SchemaTypeRegistry inner = SchemaTypeRegistry.compose(parts);
                nestedParts.put(inner, parts);
                composition.set(0, inner);
            }
            return new Scenario(canonical, components, List.copyOf(composition), nestedParts, nested);
        }

        /** The number of components that register {@code id}. */
        int owners(ShapeId id) {
            return (int) components.stream().filter(c -> c.entries().containsKey(id)).count();
        }

        /** The distinct entry objects registered for {@code id}, by identity. */
        Map<RegisteredType<?>, Boolean> registeredEntries(ShapeId id) {
            Map<RegisteredType<?>, Boolean> result = new IdentityHashMap<>();
            for (Component component : components) {
                RegisteredType<?> entry = component.entries().get(id);
                if (entry != null) {
                    result.put(entry, true);
                }
            }
            return result;
        }

        /** Whether composing may load {@code partition}: another component registers one of its IDs. */
        boolean mayLoadOnCompose(Partition partition) {
            return partition.ids.stream().anyMatch(id -> owners(id) > 1);
        }

        List<Partition> partitions() {
            return components.stream().flatMap(c -> c.partitions().stream()).toList();
        }

        /** The oracle: the entry of the first registry, in order, that registers each ID. */
        Map<ShapeId, RegisteredType<?>> expectedEntries(List<SchemaTypeRegistry> registries) {
            Map<ShapeId, RegisteredType<?>> result = new TreeMap<>();
            for (SchemaTypeRegistry registry : registries) {
                expectedEntries(registry).forEach(result::putIfAbsent);
            }
            return result;
        }

        private Map<ShapeId, RegisteredType<?>> expectedEntries(SchemaTypeRegistry registry) {
            List<SchemaTypeRegistry> parts = nestedParts.get(registry);
            if (parts != null) {
                return expectedEntries(parts);
            }
            return components.stream().filter(c -> c.registry() == registry).findFirst().orElseThrow().entries();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RegisteredType<?> entry(Schema schema, Class<? extends Probe> type, Factory factory) {
        return RegisteredType.of(schema, (Class) type, (Supplier) factory);
    }

    // Generated-style types.

    /** The builder factory of one registered ID; its builders remember it. */
    static final class Factory implements Supplier<ProbeBuilder> {
        private final Schema schema;
        private final Function<Schema, Probe> constructor;

        Factory(Schema schema, Function<Schema, Probe> constructor) {
            this.schema = schema;
            this.constructor = constructor;
        }

        @Override
        public ProbeBuilder get() {
            return new ProbeBuilder(this);
        }
    }

    /** A builder created by a {@link Factory}. */
    static final class ProbeBuilder implements ShapeBuilder<Probe> {
        final Factory factory;

        ProbeBuilder(Factory factory) {
            this.factory = factory;
        }

        @Override
        public Schema schema() {
            return factory.schema;
        }

        @Override
        public ShapeBuilder<Probe> deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(factory.schema, this, (state, member, value) -> value.readDocument());
            return this;
        }

        @Override
        public Probe build() {
            return factory.constructor.apply(factory.schema);
        }
    }

    /** A memberless generated-style value of any registered schema. */
    abstract static class Probe implements SerializableStruct {
        private final Schema schema;

        Probe(Schema schema) {
            this.schema = schema;
        }

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return null;
        }
    }

    static final class ProbeA extends Probe {
        ProbeA(Schema schema) {
            super(schema);
        }
    }

    static final class ProbeB extends Probe {
        ProbeB(Schema schema) {
            super(schema);
        }
    }

    static final class ProbeC extends Probe {
        ProbeC(Schema schema) {
            super(schema);
        }
    }
}
