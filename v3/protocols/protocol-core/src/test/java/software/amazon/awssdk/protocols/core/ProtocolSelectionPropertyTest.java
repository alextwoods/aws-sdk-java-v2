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

package software.amazon.awssdk.protocols.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import software.amazon.awssdk.protocols.core.ProtocolSelectionException.Reason;
import software.amazon.awssdk.protocols.core.SelectedProtocol.Source;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 11 (Requirements 5.6, 5.12): for any declared protocol set, installed provider set and explicit selection,
 * client construction ({@link InstalledProtocols#of} followed by a {@link ProtocolSelector#select} overload) selects
 * exactly one declared, supported protocol or rejects construction with a {@link ProtocolSelectionException} that
 * identifies the invalid selection.
 *
 * <p>Each try generates:
 * <ul>
 *   <li>a protocol universe: a subset of the seven launch protocols plus random non-launch protocol IDs (some in the
 *       launch namespaces, with near-launch and mixed-case names, so that shape ID order interleaves with them);</li>
 *   <li>the declared set, a subset of the universe (possibly empty);</li>
 *   <li>the installed providers, at most one per universe ID except that one or two IDs may get two providers
 *       (of the same or of different classes); any provider may reject the service in {@code validate};</li>
 *   <li>the enumeration order of the providers and of the declarations;</li>
 *   <li>the explicit selection: none (model default), a protocol ID (from the universe, another launch protocol or an
 *       ID no service declares) or a caller-supplied provider.</li>
 * </ul>
 *
 * <p>An independent oracle ({@link #oracle}) restates the documented rules with set operations and an explicit
 * precedence list and predicts the selection (protocol, source and provider) or the rejection (reason, service,
 * requested protocol, declared/installed/provider lists and, for a runtime rejection, the rejecting provider). The
 * property then asserts, for the generated order, its reverse and seeded shuffles, each constructed twice from the
 * same provider instances, that:
 * <ul>
 *   <li>the observed outcome matches the prediction exactly;</li>
 *   <li>the outcome is identical across enumeration orders and repeated calls (a duplicate-provider failure names
 *       the same duplicate each time);</li>
 *   <li>{@link ProtocolProvider#validate} is called once per construction on the selected provider, with the service
 *       and the selected declaration, and never on any other provider; a runtime rejection propagates the exact
 *       exception the provider threw.</li>
 * </ul>
 *
 * <p>{@link Reason#DISCOVERY_FAILED} is not generated: it arises only from {@link InstalledProtocols#discover}
 * class-path failures, which {@code InstalledProtocolsTest} covers.
 */
class ProtocolSelectionPropertyTest {
    private static final ShapeId SERVICE = ShapeId.from("com.example#Widgets");

    /** The documented default precedence, restated independently of {@link ProtocolIds#LAUNCH_PRECEDENCE}. */
    private static final List<ShapeId> ORACLE_PRECEDENCE = Stream.of(
        "smithy.protocols#rpcv2Cbor", "aws.protocols#awsJson1_0", "aws.protocols#awsJson1_1",
        "aws.protocols#restJson1", "aws.protocols#restXml", "aws.protocols#awsQuery", "aws.protocols#ec2Query")
                                                                 .map(ShapeId::from)
                                                                 .toList();

    /** An explicit ID outside every generated universe. */
    private static final ShapeId NEVER_DECLARED = ShapeId.from("unlisted.protocols#neverDeclared");

    private static final List<String> NON_LAUNCH_NAMESPACES =
        List.of("a.proto", "aws.protocols", "com.example", "smithy.protocols", "zz.custom");

    private static final List<String> NEAR_LAUNCH_NAMES =
        List.of("awsJson1_2", "restXML", "RestJson1", "rpcv2Json", "ec2query", "awsQueryCompatible");

    private static final List<Class<? extends RecordingProvider>> PROVIDER_CLASSES =
        List.of(ProviderA.class, ProviderB.class, ProviderC.class);

    /** Seeded shuffles checked besides the generated enumeration order and its reverse. */
    private static final int SHUFFLES = 3;

    /** Constructions per enumeration order, reusing the same provider instances. */
    private static final int REPEATS = 2;

    /** Minimum number of tries per statistics category (of 1,000 tries). */
    private static final int MIN_COVERAGE = 5;

    // Feature: aws-sdk-java-v3, Property 11: Protocol selection is singular and valid
    // Embedded edge cases are disabled: through the flatMaps they collapse to empty universes and the first
    // alternative of each choice. The statistics coverage checks below fail the property if an outcome is never
    // exercised.
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 11: Protocol selection is singular and valid")
    void protocolSelectionIsSingularAndValid(@ForAll("scenarios") Scenario scenario) {
        Expected expected = oracle(scenario);
        Observed first = null;
        for (Order order : orders(scenario)) {
            Observed observed = construct(scenario, order, expected);
            if (first == null) {
                first = observed;
            } else {
                assertEquals(first, observed, "The outcome depends on the enumeration order " + order);
            }
        }
        collectStatistics(scenario, expected);
    }

    // The construction under test and its checks.

    /** Constructs {@code REPEATS} times with one set of provider instances in {@code order}, checking each outcome. */
    private static Observed construct(Scenario scenario, Order order, Expected expected) {
        ServiceProtocols.Builder builder = ServiceProtocols.builder(Schema.createService(SERVICE));
        order.declared().forEach(id -> builder.protocol(ModeledProtocol.of(id)));
        ServiceProtocols service = builder.build();
        List<RecordingProvider> installed = order.providers().stream().map(ProviderSpec::create).toList();
        RecordingProvider explicit = scenario.explicit() instanceof ByProvider p ? p.spec().create() : null;
        List<RecordingProvider> all = new ArrayList<>(installed);
        if (explicit != null) {
            all.add(explicit);
        }

        Observed previous = null;
        for (int call = 1; call <= REPEATS; call++) {
            Observed observed;
            try {
                SelectedProtocol selected = switch (scenario.explicit()) {
                    case ModelDefault d -> ProtocolSelector.select(service, InstalledProtocols.of(installed));
                    case ById r -> ProtocolSelector.select(service, InstalledProtocols.of(installed), r.id());
                    case ByProvider p -> ProtocolSelector.select(service, explicit);
                };
                observed = checkSelection(expected, service, installed, explicit, all, selected, call);
            } catch (ProtocolSelectionException e) {
                observed = checkRejection(expected, installed, explicit, all, e, call);
            }
            if (previous != null) {
                assertEquals(previous, observed, "Repeated construction changed the outcome");
            }
            previous = observed;
        }
        return previous;
    }

    private static Observed checkSelection(Expected expected, ServiceProtocols service,
                                           List<RecordingProvider> installed, RecordingProvider explicit,
                                           List<RecordingProvider> all, SelectedProtocol selected, int calls) {
        if (!(expected instanceof Selection s)) {
            return fail("Expected rejection " + expected + " but selected " + selected);
        }
        assertNotNull(selected);
        assertEquals(s.id(), selected.protocolId(), "selected protocol");
        assertEquals(s.source(), selected.source(), "selection source");
        assertTrue(service.declares(selected.protocolId()), "selected protocol is not declared");
        assertSame(service, selected.service());
        assertSame(service.protocol(s.id()).orElseThrow(), selected.declaration(), "selected declaration");
        RecordingProvider provider = s.source() == Source.EXPLICIT_PROVIDER ? explicit : only(installed, s.id());
        assertSame(provider, selected.provider(), "selected provider");
        assertEquals(s.spec(), provider.spec);
        assertValidatedOnlyBy(all, provider, calls);
        for (Call call : provider.calls) {
            assertSame(service, call.service(), "validate service");
            assertSame(selected.declaration(), call.declaration(), "validate declaration");
        }
        return new Observed(selected.protocolId(), selected.source(), provider.spec, null, Optional.empty(),
                            Optional.empty(), List.of(), List.of(), List.of(), "");
    }

    private static Observed checkRejection(Expected expected, List<RecordingProvider> installed,
                                           RecordingProvider explicit, List<RecordingProvider> all,
                                           ProtocolSelectionException e, int calls) {
        if (!(expected instanceof Rejection r)) {
            throw new AssertionError("Expected selection " + expected + " but construction failed", e);
        }
        assertEquals(r.reason(), e.reason(), () -> "reason of: " + e.getMessage());
        assertEquals(r.serviceId(), e.serviceId(), "serviceId");
        assertEquals(r.requested(), e.requested(), "requested");
        assertEquals(r.declared(), e.declared(), "declared");
        assertEquals(r.installed(), e.installed(), "installed");
        assertTrue(r.acceptableProviders().contains(e.providers()),
                   () -> "providers " + e.providers() + " not one of " + r.acceptableProviders());
        assertFalse(e.retryable());
        String message = e.getMessage();
        assertNotNull(message);
        e.serviceId().ifPresent(id -> assertTrue(message.contains(id.toString()), message));
        e.requested().ifPresent(id -> assertTrue(message.contains(id.toString()), message));
        e.providers().forEach(name -> assertTrue(message.contains(name), message));

        if (r.rejecting() == null) {
            assertValidatedOnlyBy(all, null, 0);
        } else {
            RecordingProvider rejecting = explicit != null ? explicit : only(installed, r.rejecting().id());
            assertEquals(r.rejecting(), rejecting.spec);
            assertSame(rejecting.thrown, e, "the runtime's own exception must propagate");
            assertValidatedOnlyBy(all, rejecting, calls);
        }
        return new Observed(null, null, r.rejecting(), e.reason(), e.serviceId(), e.requested(), e.declared(),
                            e.installed(), e.providers(), message);
    }

    /** {@code validated} (if any) was validated exactly {@code calls} times and no other provider ever. */
    private static void assertValidatedOnlyBy(List<RecordingProvider> all, RecordingProvider validated, int calls) {
        for (RecordingProvider provider : all) {
            int expected = provider == validated ? calls : 0;
            assertEquals(expected, provider.calls.size(), () -> "validate calls of " + provider);
        }
    }

    private static RecordingProvider only(List<RecordingProvider> installed, ShapeId id) {
        List<RecordingProvider> matching = installed.stream().filter(p -> p.spec.id().equals(id)).toList();
        assertEquals(1, matching.size(), () -> "installed providers of " + id + ": " + matching);
        return matching.get(0);
    }

    /** The generated order, its reverse and {@link #SHUFFLES} seeded shuffles of providers and declarations. */
    private static List<Order> orders(Scenario scenario) {
        List<Order> orders = new ArrayList<>();
        orders.add(new Order(scenario.declared(), scenario.providers()));
        orders.add(new Order(reversed(scenario.declared()), reversed(scenario.providers())));
        Random random = new Random(scenario.shuffleSeed());
        for (int i = 0; i < SHUFFLES; i++) {
            orders.add(new Order(shuffled(scenario.declared(), random), shuffled(scenario.providers(), random)));
        }
        return orders;
    }

    private static <T> List<T> reversed(List<T> values) {
        List<T> copy = new ArrayList<>(values);
        Collections.reverse(copy);
        return copy;
    }

    private static <T> List<T> shuffled(List<T> values, Random random) {
        List<T> copy = new ArrayList<>(values);
        Collections.shuffle(copy, random);
        return copy;
    }

    // The oracle.

    /**
     * The documented selection rules, restated with set operations:
     * <ol>
     *   <li>explicit provider: selected if its protocol is declared (installed runtimes are not consulted), else
     *       {@code NOT_DECLARED} with the supplied protocol as the installed set;</li>
     *   <li>two providers of one protocol: {@code DUPLICATE_PROVIDER} for the installed set itself (no service,
     *       requested or declared data), naming the provider classes of one duplicated protocol;</li>
     *   <li>explicit ID: must be declared ({@code NOT_DECLARED}), then installed ({@code NOT_INSTALLED});</li>
     *   <li>model default: the first protocol of the launch precedence in declared &#8745; installed, else the least
     *       shape ID in it; {@code NO_INSTALLED_DECLARED_PROTOCOL} if it is empty;</li>
     *   <li>the chosen runtime's {@code validate} may reject the service: {@code UNSUPPORTED_BY_RUNTIME}.</li>
     * </ol>
     */
    static Expected oracle(Scenario scenario) {
        TreeSet<ShapeId> declared = new TreeSet<>(scenario.declared());
        List<ShapeId> declaredList = List.copyOf(declared);

        if (scenario.explicit() instanceof ByProvider p) {
            ProviderSpec spec = p.spec();
            if (!declared.contains(spec.id())) {
                return Rejection.of(Reason.NOT_DECLARED, spec.id(), declaredList, List.of(spec.id()));
            }
            return validated(new Selection(spec.id(), Source.EXPLICIT_PROVIDER, spec));
        }

        Map<ShapeId, List<ProviderSpec>> byId = new TreeMap<>();
        scenario.providers().forEach(spec -> byId.computeIfAbsent(spec.id(), k -> new ArrayList<>()).add(spec));
        List<ShapeId> installed = List.copyOf(byId.keySet());
        Set<List<String>> duplicates = byId.values().stream()
                                           .filter(specs -> specs.size() > 1)
                                           .map(specs -> specs.stream().map(ProviderSpec::className).sorted().toList())
                                           .collect(Collectors.toSet());
        if (!duplicates.isEmpty()) {
            return new Rejection(Reason.DUPLICATE_PROVIDER, Optional.empty(), Optional.empty(), List.of(), installed,
                                 duplicates, null);
        }

        ShapeId chosen;
        Source source;
        if (scenario.explicit() instanceof ById r) {
            if (!declared.contains(r.id())) {
                return Rejection.of(Reason.NOT_DECLARED, r.id(), declaredList, installed);
            }
            if (!byId.containsKey(r.id())) {
                return Rejection.of(Reason.NOT_INSTALLED, r.id(), declaredList, installed);
            }
            chosen = r.id();
            source = Source.EXPLICIT_ID;
        } else {
            TreeSet<ShapeId> candidates = candidates(declared, byId.keySet());
            if (candidates.isEmpty()) {
                return Rejection.of(Reason.NO_INSTALLED_DECLARED_PROTOCOL, null, declaredList, installed);
            }
            chosen = ORACLE_PRECEDENCE.stream().filter(candidates::contains).findFirst().orElse(candidates.first());
            source = Source.MODEL_DEFAULT;
        }
        return validated(new Selection(chosen, source, byId.get(chosen).get(0)));
    }

    private static TreeSet<ShapeId> candidates(Set<ShapeId> declared, Set<ShapeId> installed) {
        TreeSet<ShapeId> candidates = new TreeSet<>(declared);
        candidates.retainAll(installed);
        return candidates;
    }

    /** {@code selection}, or the rejection its runtime raises in {@code validate}. */
    private static Expected validated(Selection selection) {
        ProviderSpec spec = selection.spec();
        if (!spec.rejects()) {
            return selection;
        }
        // The fields RecordingProvider.validate sets on the exception it throws.
        return new Rejection(Reason.UNSUPPORTED_BY_RUNTIME, Optional.of(SERVICE), Optional.of(spec.id()), List.of(),
                             List.of(), Set.of(List.of()), spec);
    }

    // Generators.

    @Provide
    Arbitrary<Scenario> scenarios() {
        // One universe in four has no launch protocol, so that non-launch protocols are ordered by shape ID alone.
        Arbitrary<Set<ShapeId>> launch = Arbitraries.frequencyOf(Tuple.of(3, Arbitraries.subsetOf(ORACLE_PRECEDENCE)),
                                                                 Tuple.of(1, Arbitraries.just(Set.of())));
        Arbitrary<List<ShapeId>> universes = Combinators
            .combine(launch, nonLaunchIds().set().ofMaxSize(4))
            .as((launchIds, other) -> {
                TreeSet<ShapeId> universe = new TreeSet<>(launchIds);
                universe.addAll(other);
                return List.copyOf(universe);
            });
        return universes.flatMap(universe -> Arbitraries.subsetOf(universe).flatMap(declared -> Combinators
            .combine(Arbitraries.just(sorted(declared)),
                     Arbitraries.subsetOf(universe).flatMap(ids -> providers(sorted(ids))),
                     explicit(universe, sorted(declared)),
                     Arbitraries.longs())
            .as(Scenario::new)));
    }

    private static Arbitrary<ShapeId> nonLaunchIds() {
        Arbitrary<String> random = Combinators
            .combine(Arbitraries.chars().range('a', 'z'),
                     Arbitraries.strings().withCharRange('a', 'z').withCharRange('A', 'Z').withCharRange('0', '9')
                                .withChars('_').ofMaxLength(8))
            .as((head, tail) -> head + tail);
        Arbitrary<String> names = Arbitraries.frequencyOf(Tuple.of(3, random),
                                                          Tuple.of(1, Arbitraries.of(NEAR_LAUNCH_NAMES)));
        return Combinators.combine(Arbitraries.of(NON_LAUNCH_NAMESPACES), names).as(ShapeId::fromParts);
    }

    /** Providers of {@code ids} in a shuffled order; none, one or two IDs get a second provider. */
    private static Arbitrary<List<ProviderSpec>> providers(List<ShapeId> ids) {
        if (ids.isEmpty()) {
            return Arbitraries.just(List.of());
        }
        Arbitrary<Set<ShapeId>> duplicated = Arbitraries.frequencyOf(
            Tuple.of(9, Arbitraries.just(Set.of())),
            Tuple.of(2, Arbitraries.subsetOf(ids).ofSize(1)),
            Tuple.of(1, Arbitraries.subsetOf(ids).ofMinSize(1).ofMaxSize(Math.min(2, ids.size()))));
        return duplicated.flatMap(dups -> {
            List<Arbitrary<ProviderSpec>> specs = new ArrayList<>();
            for (ShapeId id : ids) {
                int count = dups.contains(id) ? 2 : 1;
                for (int i = 0; i < count; i++) {
                    specs.add(providerSpec(id));
                }
            }
            return Combinators.combine(specs).as(List::copyOf);
        }).flatMap(Arbitraries::shuffle);
    }

    private static Arbitrary<ProviderSpec> providerSpec(ShapeId id) {
        return Combinators.combine(Arbitraries.integers().between(0, PROVIDER_CLASSES.size() - 1),
                                   Arbitraries.frequency(Tuple.of(5, false), Tuple.of(1, true)))
                          .as((variant, rejects) -> new ProviderSpec(id, variant, rejects));
    }

    /**
     * No explicit selection, an explicit ID or an explicit provider: mostly of a declared protocol, else of another
     * universe protocol, a launch protocol outside the universe or a protocol no service declares.
     */
    private static Arbitrary<Explicit> explicit(List<ShapeId> universe, List<ShapeId> declared) {
        TreeSet<ShapeId> outside = new TreeSet<>(ORACLE_PRECEDENCE);
        outside.add(NEVER_DECLARED);
        outside.removeAll(universe);
        List<Tuple.Tuple2<Integer, Arbitrary<ShapeId>>> choices = new ArrayList<>();
        choices.add(Tuple.of(1, Arbitraries.of(List.copyOf(outside))));
        if (!universe.isEmpty()) {
            choices.add(Tuple.of(2, Arbitraries.of(universe)));
        }
        if (!declared.isEmpty()) {
            choices.add(Tuple.of(4, Arbitraries.of(declared)));
        }
        Arbitrary<ShapeId> requested = Arbitraries.frequencyOf(choices);
        return Arbitraries.frequencyOf(
            Tuple.of(4, Arbitraries.just(new ModelDefault())),
            Tuple.of(3, requested.map(ById::new)),
            Tuple.of(2, requested.flatMap(ProtocolSelectionPropertyTest::providerSpec).map(ByProvider::new)));
    }

    private static List<ShapeId> sorted(Set<ShapeId> ids) {
        return List.copyOf(new TreeSet<>(ids));
    }

    // Statistics.

    /** Fails the property if an outcome or a precedence situation is generated too rarely to be exercised. */
    private static void collectStatistics(Scenario scenario, Expected expected) {
        String outcome = switch (expected) {
            case Selection s -> "selected " + s.source();
            case Rejection r -> r.reason().toString();
        };
        Statistics.label("outcome").collect(outcome);
        // Minimum counts per 1,000 tries, well below the observed rates (the rarest categories average about 15 per
        // 1,000) so that a random seed does not fail them; they catch generators that stop producing an outcome.
        Statistics.label("outcome").coverage(c -> {
            for (Source source : Source.values()) {
                c.check("selected " + source).count(n -> n >= MIN_COVERAGE);
            }
            for (Reason reason : Reason.values()) {
                if (reason != Reason.DISCOVERY_FAILED) {
                    c.check(reason.toString()).count(n -> n >= MIN_COVERAGE);
                }
            }
        });

        String precedence = "not a model default selection";
        if (expected instanceof Selection s && s.source() == Source.MODEL_DEFAULT) {
            Set<ShapeId> candidates = candidates(new TreeSet<>(scenario.declared()),
                                                 scenario.providers().stream().map(ProviderSpec::id)
                                                         .collect(Collectors.toSet()));
            long launch = candidates.stream().filter(ORACLE_PRECEDENCE::contains).count();
            precedence = candidates.size() == 1 ? "single candidate"
                                                : launch >= 2 ? "launch precedence"
                                                              : launch == 1 ? "launch over non-launch"
                                                                            : "non-launch by shape ID";
        }
        Statistics.label("default precedence").collect(precedence);
        Statistics.label("default precedence").coverage(c -> {
            c.check("single candidate").count(n -> n >= MIN_COVERAGE);
            c.check("launch precedence").count(n -> n >= MIN_COVERAGE);
            c.check("launch over non-launch").count(n -> n >= MIN_COVERAGE);
            c.check("non-launch by shape ID").count(n -> n >= MIN_COVERAGE);
        });
    }

    // Scenario and outcome values.

    /**
     * One try: the declared protocols and installed providers in their generated enumeration order, the explicit
     * selection, and the seed of the shuffled orders.
     */
    record Scenario(List<ShapeId> declared, List<ProviderSpec> providers, Explicit explicit, long shuffleSeed) {
    }

    /** An enumeration order of the declarations and installed providers. */
    record Order(List<ShapeId> declared, List<ProviderSpec> providers) {
    }

    /** The explicit selection of a try. */
    sealed interface Explicit permits ModelDefault, ById, ByProvider {
    }

    /** No explicit selection. */
    record ModelDefault() implements Explicit {
    }

    /** The caller names protocol {@code id}. */
    record ById(ShapeId id) implements Explicit {
    }

    /** The caller supplies the provider {@code spec}. */
    record ByProvider(ProviderSpec spec) implements Explicit {
    }

    /** A provider of {@code id}, of class {@code PROVIDER_CLASSES[variant]}, whose validate rejects if {@code rejects}. */
    record ProviderSpec(ShapeId id, int variant, boolean rejects) {
        RecordingProvider create() {
            return switch (variant) {
                case 0 -> new ProviderA(this);
                case 1 -> new ProviderB(this);
                default -> new ProviderC(this);
            };
        }

        String className() {
            return PROVIDER_CLASSES.get(variant).getName();
        }
    }

    /** The oracle's prediction. */
    sealed interface Expected permits Selection, Rejection {
    }

    /** Protocol {@code id} is selected from {@code source} with the provider {@code spec}. */
    record Selection(ShapeId id, Source source, ProviderSpec spec) implements Expected {
    }

    /**
     * Construction fails with these facts; {@code providers()} is one of {@code acceptableProviders};
     * {@code rejecting} is the provider whose validate threw, if any.
     */
    record Rejection(Reason reason, Optional<ShapeId> serviceId, Optional<ShapeId> requested, List<ShapeId> declared,
                     List<ShapeId> installed, Set<List<String>> acceptableProviders, ProviderSpec rejecting)
        implements Expected {

        static Rejection of(Reason reason, ShapeId requested, List<ShapeId> declared, List<ShapeId> installed) {
            return new Rejection(reason, Optional.of(SERVICE), Optional.ofNullable(requested), declared, installed,
                                 Set.of(List.of()), null);
        }
    }

    /** What one construction produced, compared across enumeration orders and repeated calls. */
    record Observed(ShapeId protocolId, Source source, ProviderSpec provider, Reason reason,
                    Optional<ShapeId> serviceId, Optional<ShapeId> requested, List<ShapeId> declared,
                    List<ShapeId> installed, List<String> providers, String message) {
    }

    /** One {@code validate} call. */
    record Call(ServiceProtocols service, ModeledProtocol declaration) {
    }

    // Providers.

    /** A provider that records its validate calls and, if its spec says so, rejects the service. */
    abstract static class RecordingProvider implements ProtocolProvider {
        final ProviderSpec spec;
        final List<Call> calls = new ArrayList<>();
        ProtocolSelectionException thrown;

        RecordingProvider(ProviderSpec spec) {
            this.spec = spec;
        }

        @Override
        public ShapeId protocolId() {
            return spec.id();
        }

        @Override
        public boolean usesHttpBindingTraits() {
            return false;
        }

        @Override
        public void validate(ServiceProtocols service, ModeledProtocol declaration) {
            calls.add(new Call(service, declaration));
            if (spec.rejects()) {
                thrown = ProtocolSelectionException.builder()
                                                   .reason(Reason.UNSUPPORTED_BY_RUNTIME)
                                                   .serviceId(service.serviceId())
                                                   .requested(declaration.id())
                                                   .message(getClass().getSimpleName() + " cannot serve "
                                                            + service.serviceId() + " with " + declaration.id())
                                                   .build();
                throw thrown;
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + spec;
        }
    }

    static final class ProviderA extends RecordingProvider {
        ProviderA(ProviderSpec spec) {
            super(spec);
        }
    }

    static final class ProviderB extends RecordingProvider {
        ProviderB(ProviderSpec spec) {
            super(spec);
        }
    }

    static final class ProviderC extends RecordingProvider {
        ProviderC(ProviderSpec spec) {
            super(spec);
        }
    }
}
