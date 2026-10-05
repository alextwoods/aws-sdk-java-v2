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

package software.amazon.awssdk.core.client.config;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;

/**
 * Property 28: unsupported options never disappear silently (Requirements 9.9, 9.10).
 *
 * <p><b>Generators.</b> A scenario is a Parity Manifest fragment of 1–12 V2 configuration options, each with a
 * Compatibility Classification from the manifest vocabulary ({@code PRESERVED | ADAPTED | EXCLUDED}, requirements
 * glossary and design "Parity Manifest schema") and whether the option has V3 behavior: {@code PRESERVED} always does,
 * {@code EXCLUDED} never does, {@code ADAPTED} may or may not (design: conflicting surfaces are "{@code EXCLUDED} or
 * {@code ADAPTED} with migration diagnostics"). Dispositions are the plain strings the resolver accepts today: the
 * classification name, optionally followed by free rationale text. Every option lacking V3 behavior is registered as
 * unsupported, in a random registration order; a random subset of all options is supplied, in a random supply order,
 * each with a random {@link ConfigurationSource} provenance, and a secret value.
 *
 * <p><b>Oracle</b> (independent of the resolver): construction fails if and only if a supplied option lacks V3
 * behavior; the failure's {@link #render rendered diagnostic} lists exactly the supplied unsupported options, in supply
 * order, each with its disposition and source; the first is {@link SdkClientConfigurationException#option()}; supported
 * and unsupplied options and supplied values are never mentioned; the diagnostic does not depend on registration order
 * and is identical on repeated resolution. When construction succeeds every supplied option is present in the resolved
 * configuration with its value and provenance, so no option vanishes.
 *
 * <p><b>Migration diagnostic equality (Requirement 9.10) is deferred.</b> No migration diagnostic producer exists yet:
 * {@code v3/tools/parity-manifest} is a skeleton and {@code v3/tools/v2-migration} does not exist. This test pins the
 * runtime half of the equality obligation as the seam {@link UnsupportedOptionDiagnostic}: option name, Parity
 * Manifest disposition and source. Task 15.2 (configuration recipes, which validates 9.10), on top of 15.1's manifest
 * reader and source locations and 13.1's manifest dispositions, must report for each V2 configuration option without
 * V3 behavior a diagnostic whose option name and disposition equal the {@link UnsupportedOptionDiagnostic} the runtime
 * reports for the same manifest entry, plus the V2 source file and line (which only migration tooling can know; at
 * runtime the source is the {@link ConfigurationSource} provenance). That task should extend this property, or add a
 * cross-module one, comparing the two for generated V2 builder code.
 *
 * <p>Gap, not covered here: options with no V3 behavior are only detected when supplied through the client builder
 * configuration. The resolver has no JVM property, environment variable or profile property names for unsupported
 * options ({@link ClientConfigurationResolver.Builder#addUnsupportedOption} takes only the option and disposition, and
 * an option cannot be both a setting and unsupported), so a V2 external setting without V3 behavior is ignored.
 */
class UnsupportedOptionDiagnosticsPropertyTest {
    private static final int MAX_OPTIONS = 12;

    /** Independent rendering of a source in the diagnostic. */
    private static final Map<ConfigurationSource, String> SOURCE_NAMES = new EnumMap<>(Map.of(
        ConfigurationSource.REQUEST_OVERRIDE, "request override",
        ConfigurationSource.BUILDER, "client builder",
        ConfigurationSource.JVM_PROPERTY, "JVM property",
        ConfigurationSource.ENVIRONMENT, "environment variable",
        ConfigurationSource.PROFILE, "profile property",
        ConfigurationSource.DEFAULT, "SDK default"));

    // Feature: aws-sdk-java-v3, Property 28: Unsupported options never disappear silently
    @Property(tries = 1_000)
    @Label("Property 28: Unsupported options never disappear silently")
    void unsupportedOptionsNeverDisappearSilently(@ForAll("scenarios") Scenario scenario) {
        List<UnsupportedOptionDiagnostic> expected = scenario.expectedDiagnostics();
        Statistics.label("outcome").collect(expected.isEmpty() ? "accepted" : "rejected");
        Statistics.label("rejected options").collect(expected.size() >= 3 ? "3+" : String.valueOf(expected.size()));

        SdkClientConfiguration supplied = scenario.suppliedConfiguration();
        ClientConfigurationResolver resolver = scenario.resolver(scenario.registrationOrder());
        ClientConfigurationResolver reordered = scenario.resolver(reversed(scenario.registrationOrder()));

        if (expected.isEmpty()) {
            for (SdkClientConfiguration resolved : List.of(resolver.resolve(supplied), reordered.resolve(supplied))) {
                assertEquals(scenario.suppliedOptionSet(), resolved.options(), "every supplied option is kept");
                for (int index : scenario.supplied()) {
                    assertEquals(supplied.resolved(scenario.option(index)), resolved.resolved(scenario.option(index)));
                }
            }
        } else {
            SdkClientConfigurationException first =
                assertThrows(SdkClientConfigurationException.class, () -> resolver.resolve(supplied));
            String diagnostic = render(expected);
            assertEquals(diagnostic, first.getMessage());
            assertEquals(Optional.of(expected.get(0).option()), first.option());
            assertFalse(first.retryable());
            assertNull(first.getCause());
            for (int index = 0; index < scenario.size(); index++) {
                String name = scenario.option(index).name();
                assertEquals(expected.stream().anyMatch(d -> d.option().equals(name)), diagnostic.contains(name),
                             name + " is mentioned iff it is a supplied unsupported option");
                assertFalse(diagnostic.contains(value(index)), "supplied values are never rendered");
            }

            // Repeated resolution, and a different registration order, give an identical diagnostic.
            for (ClientConfigurationResolver again : List.of(resolver, reordered)) {
                SdkClientConfigurationException repeated =
                    assertThrows(SdkClientConfigurationException.class, () -> again.resolve(supplied));
                assertEquals(first.getMessage(), repeated.getMessage());
                assertEquals(first.option(), repeated.option());
            }
        }

        // No registered option can also be resolved as a setting, so none can be silently accepted that way.
        for (int index : scenario.registrationOrder()) {
            ClientOption<String> option = scenario.option(index);
            ClientConfigurationResolver.Builder conflicting = scenario.resolverBuilder(scenario.registrationOrder())
                .addSetting(ClientSetting.builder(option).parser(text -> text).build());
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, conflicting::build);
            assertTrue(e.getMessage().contains(option.name()), e.getMessage());
        }
    }

    @Example
    void rejectionNamesEveryUnsupportedOptionWithDispositionAndSourceInSupplyOrder() {
        ClientOption<String> signer = ClientOption.create("Signer", String.class);
        ClientOption<String> retryPolicy = ClientOption.create("RetryPolicy", String.class);
        ClientOption<String> region = ClientOption.create("Region", String.class);
        ClientConfigurationResolver resolver = ClientConfigurationResolver.builder()
                                                                          .addUnsupportedOption(retryPolicy, "EXCLUDED")
                                                                          .addUnsupportedOption(signer, "EXCLUDED")
                                                                          .build();

        SdkClientConfigurationException e = assertThrows(
            SdkClientConfigurationException.class,
            () -> resolver.resolve(SdkClientConfiguration.builder()
                                                         .option(signer, "legacy-signer")
                                                         .option(region, "us-west-2")
                                                         .option(retryPolicy, "legacy-retry",
                                                                 ConfigurationSource.ENVIRONMENT)
                                                         .build()));

        assertEquals("Client construction failed: configuration options Signer (disposition EXCLUDED) from client "
                     + "builder, RetryPolicy (disposition EXCLUDED) from environment variable have no V3 behavior. "
                     + "Remove them or use the V3 replacement named by the migration guide.", e.getMessage());
        assertEquals(Optional.of("Signer"), e.option());
    }

    @Example
    void singleRejectionUsesSingularWording() {
        ClientOption<String> signer = ClientOption.create("Signer", String.class);
        ClientConfigurationResolver resolver = ClientConfigurationResolver.builder()
                                                                          .addUnsupportedOption(signer, "EXCLUDED")
                                                                          .build();

        SdkClientConfigurationException e = assertThrows(
            SdkClientConfigurationException.class,
            () -> resolver.resolve(SdkClientConfiguration.builder().option(signer, "legacy-signer").build()));

        assertEquals("Client construction failed: configuration option Signer (disposition EXCLUDED) from client "
                     + "builder has no V3 behavior. Remove it or use the V3 replacement named by the migration guide.",
                     e.getMessage());
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<ManifestEntry> entry = Combinators.combine(
            Arbitraries.of(Classification.class),
            Arbitraries.of(true, false),
            Arbitraries.strings().withChars("abcdefghijklmnopqrstuvwxyz :-(),").ofMaxLength(16))
            .as((classification, adaptedHasBehavior, rationale) -> {
                boolean v3Behavior = switch (classification) {
                    case PRESERVED -> true;
                    case EXCLUDED -> false;
                    case ADAPTED -> adaptedHasBehavior;
                };
                String disposition = rationale.isEmpty() ? classification.name() : classification + " " + rationale;
                return new ManifestEntry(classification, v3Behavior, disposition);
            });

        return entry.list().ofMinSize(1).ofMaxSize(MAX_OPTIONS).flatMap(manifest -> {
            List<Integer> indices = IntStream.range(0, manifest.size()).boxed().toList();
            List<Integer> unsupported = indices.stream().filter(i -> !manifest.get(i).v3Behavior()).toList();
            Arbitrary<List<Integer>> registration = Arbitraries.shuffle(unsupported);
            Arbitrary<List<Integer>> supplied =
                Arbitraries.subsetOf(indices).flatMap(subset -> Arbitraries.shuffle(new ArrayList<>(subset)));
            return registration.flatMap(order -> supplied.flatMap(
                suppliedOrder -> Arbitraries.of(ConfigurationSource.class).list().ofSize(suppliedOrder.size())
                                            .map(sources -> Scenario.of(manifest, order, suppliedOrder, sources))));
        });
    }

    /**
     * The runtime half of the Requirement 9.10 equality seam: what construction reports for one supplied option without
     * V3 behavior. Migration tooling (task 15.2) must report the same option name and disposition for the same manifest
     * entry, plus the V2 source file and line.
     */
    record UnsupportedOptionDiagnostic(String option, String disposition, ConfigurationSource source) {
    }

    /** The diagnostic construction must report, rendered independently of the resolver. */
    static String render(List<UnsupportedOptionDiagnostic> diagnostics) {
        boolean one = diagnostics.size() == 1;
        return "Client construction failed: configuration option" + (one ? " " : "s ")
               + diagnostics.stream()
                            .map(d -> d.option() + " (disposition " + d.disposition() + ") from "
                                      + SOURCE_NAMES.get(d.source()))
                            .collect(joining(", "))
               + (one ? " has" : " have") + " no V3 behavior. Remove " + (one ? "it" : "them")
               + " or use the V3 replacement named by the migration guide.";
    }

    enum Classification { PRESERVED, ADAPTED, EXCLUDED }

    record ManifestEntry(Classification classification, boolean v3Behavior, String disposition) {
    }

    /**
     * Manifest entries and their options (option {@code i} is named {@code V2Option<i>}, so no name contains another),
     * the registration order of the entries without V3 behavior, and the supplied entries in supply order with their
     * sources.
     */
    record Scenario(List<ManifestEntry> manifest, List<ClientOption<String>> options, List<Integer> registrationOrder,
                    List<Integer> supplied, List<ConfigurationSource> suppliedSources) {
        Scenario {
            manifest = List.copyOf(manifest);
            options = List.copyOf(options);
            registrationOrder = List.copyOf(registrationOrder);
            supplied = List.copyOf(supplied);
            suppliedSources = List.copyOf(suppliedSources);
        }

        // Options are created per scenario: resolvers key options by instance, so scenarios never share them.
        static Scenario of(List<ManifestEntry> manifest, List<Integer> registrationOrder, List<Integer> supplied,
                           List<ConfigurationSource> suppliedSources) {
            List<ClientOption<String>> options =
                IntStream.range(0, manifest.size())
                         .mapToObj(i -> ClientOption.create("V2Option<" + i + ">", String.class))
                         .toList();
            return new Scenario(manifest, options, registrationOrder, supplied, suppliedSources);
        }

        int size() {
            return manifest.size();
        }

        ClientOption<String> option(int index) {
            return options.get(index);
        }

        ClientConfigurationResolver.Builder resolverBuilder(List<Integer> order) {
            ClientConfigurationResolver.Builder builder = ClientConfigurationResolver.builder();
            order.forEach(index -> builder.addUnsupportedOption(option(index), manifest.get(index).disposition()));
            return builder;
        }

        ClientConfigurationResolver resolver(List<Integer> order) {
            return resolverBuilder(order).build();
        }

        SdkClientConfiguration suppliedConfiguration() {
            SdkClientConfiguration.Builder builder = SdkClientConfiguration.builder();
            for (int i = 0; i < supplied.size(); i++) {
                int index = supplied.get(i);
                builder.option(option(index), value(index), suppliedSources.get(i));
            }
            return builder.build();
        }

        Set<ClientOption<?>> suppliedOptionSet() {
            Set<ClientOption<?>> result = new LinkedHashSet<>();
            supplied.forEach(index -> result.add(option(index)));
            return result;
        }

        List<UnsupportedOptionDiagnostic> expectedDiagnostics() {
            List<UnsupportedOptionDiagnostic> result = new ArrayList<>();
            for (int i = 0; i < supplied.size(); i++) {
                ManifestEntry entry = manifest.get(supplied.get(i));
                if (!entry.v3Behavior()) {
                    result.add(new UnsupportedOptionDiagnostic(option(supplied.get(i)).name(), entry.disposition(),
                                                               suppliedSources.get(i)));
                }
            }
            return result;
        }
    }

    /** A supplied value that must never appear in a diagnostic; {@code !} occurs in no name or disposition. */
    private static String value(int index) {
        return "s3cr3t-" + index + "!";
    }

    private static List<Integer> reversed(List<Integer> order) {
        List<Integer> result = new ArrayList<>(order);
        Collections.reverse(result);
        return result;
    }
}
