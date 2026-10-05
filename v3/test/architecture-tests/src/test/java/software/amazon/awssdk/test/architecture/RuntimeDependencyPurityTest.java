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

package software.amazon.awssdk.test.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary;
import software.amazon.awssdk.test.architecture.DependencyPolicy.EdgeRules;
import software.amazon.awssdk.test.architecture.DependencyPolicy.ModuleRule;
import software.amazon.awssdk.test.architecture.PurityFixtures.LeakFixture;

/**
 * Requirements 15.3-15.7 and 15.9 on resolved Maven dependency trees: every V3 module's runtime (compile + runtime
 * scope) tree, including every generated service under {@code services/*} as soon as it exists, excludes the
 * artifact classes its policy forbids and declares its required edges; HTTP implementations and telemetry exporters
 * stay opt-in; and a minimal generated service resolves to exactly the documented service-and-required-runtime set.
 * Synthetic fixture services prove that each forbidden artifact class is detected directly and transitively, with
 * the full dependency path in the diagnostic. The optional dynamic layer (dynamic-model, dynamic-client) resolves only
 * in its own and test modules' trees (Requirements 14.18, 15.5); a fixture service declaring dynamic-client proves
 * the leak is reported through it.
 */
class RuntimeDependencyPurityTest {

    private static final String HTTP_IMPLEMENTATION = "http-implementation";
    private static final String TELEMETRY_EXPORTER = "telemetry-exporter";
    private static final String DYNAMIC_KIND = "dynamic";

    private static ResolvedDependencyTrees trees;
    private static V3Reactor reactor;
    private static DependencyPolicy policy;
    private static DependencyTreeInspector inspector;

    @BeforeAll
    static void resolveTrees() {
        trees = ResolvedDependencyTrees.load();
        reactor = trees.reactor();
        policy = trees.policy();
        inspector = new DependencyTreeInspector(policy);
    }

    @Test
    void everyModuleRuntimeTreeExcludesItsForbiddenArtifactClasses() {
        assertFalse(reactor.generatedCodeModules().isEmpty(),
                    "Expected at least one module inheriting from " + V3Reactor.SERVICES_PARENT);
        List<String> violations = new ArrayList<>();
        for (String module : reactor.artifactModules().keySet()) {
            violations.addAll(inspector.forbiddenArtifacts(reactor.forbiddenClasses(policy, module),
                                                           trees.tree(module)));
        }
        assertEquals(List.of(), violations);
    }

    @Test
    void everyModuleDeclaresItsRequiredEdges() {
        List<String> violations = new ArrayList<>();
        for (String module : reactor.artifactModules().keySet()) {
            violations.addAll(inspector.missingRequiredEdges(policy.rulesFor(module).orElseThrow(),
                                                             trees.tree(module)));
        }
        assertEquals(List.of(), violations);
    }

    @Test
    void httpImplementationsAndTelemetryExportersAreOptIn() {
        // Requirement 15.7. Policy level: no module or the service template requires an opt-in module, and every
        // opt-in module is a release artifact that customers can add through the BOM.
        Set<String> optIn = new TreeSet<>();
        policy.modules().values().stream()
              .filter(m -> m.kind().equals(HTTP_IMPLEMENTATION) || m.kind().equals(TELEMETRY_EXPORTER))
              .map(ModuleRule::artifactId)
              .forEach(optIn::add);
        assertFalse(optIn.isEmpty(), "The policy lists no HTTP implementation modules");
        List<String> violations = new ArrayList<>();
        Map<String, EdgeRules> allRules = new TreeMap<>();
        policy.modules().values().forEach(m -> allRules.put(m.artifactId(), m.rules()));
        allRules.put("<serviceTemplate>", policy.serviceTemplate());
        allRules.forEach((owner, rules) -> {
            Set<String> required = new TreeSet<>(rules.requires());
            rules.requiresAnyOf().forEach(required::addAll);
            required.retainAll(optIn);
            if (!required.isEmpty()) {
                violations.add(owner + " requires opt-in modules " + required);
            }
        });
        optIn.stream().filter(m -> !policy.releaseArtifacts().contains(m))
             .forEach(m -> violations.add("Opt-in module " + m + " is not a release artifact"));

        // Tree level, independent of each module's own <forbids>: no runtime module other than an HTTP
        // implementation reaches a concrete HTTP artifact, and none other than an exporter reaches an exporter.
        for (String module : reactor.artifactModules().keySet()) {
            String kind = policy.kindOf(module).orElseThrow();
            if (!V3Reactor.RUNTIME_KINDS.contains(kind)) {
                continue;
            }
            Set<String> classes = new TreeSet<>();
            if (!kind.equals(HTTP_IMPLEMENTATION)) {
                classes.add("concrete-http");
            }
            if (!kind.equals(TELEMETRY_EXPORTER)) {
                classes.add("telemetry-exporter");
            }
            violations.addAll(inspector.forbiddenArtifacts(classes, trees.tree(module)));
        }
        assertEquals(List.of(), violations);
    }

    @Test
    void minimalServiceTreesExactlyMatchTheDocumentedRequiredRuntimeSet() {
        // Requirement 15.9. The documented set is the closure of the policy's required V3 module edges plus the
        // closed Smithy Java runtime set documented by SmithyRuntimeBoundary (smithy-runtime's only third-party
        // dependencies, see the smithy-model-shape-identity waiver), plus the Reactive Streams API that http-spi
        // requires for its async body contract (design, "HTTP SPI": http-spi depends on the Reactive Streams API).
        Set<String> documentedThirdParty = new TreeSet<>();
        documentedThirdParty.add("org.reactivestreams:reactive-streams");
        SmithyRuntimeBoundary.SMITHY_JAVA_RUNTIME_ARTIFACTS
            .forEach(a -> documentedThirdParty.add(SmithyRuntimeBoundary.SMITHY_JAVA_GROUP_ID + ":" + a));
        SmithyRuntimeBoundary.TRANSITIONAL_SMITHY_ARTIFACTS
            .forEach(a -> documentedThirdParty.add(SmithyRuntimeBoundary.SMITHY_GROUP_ID + ":" + a));
        List<String> protocols = PurityFixtures.protocols(policy);
        assertFalse(protocols.isEmpty());
        for (String protocol : protocols) {
            DependencyTree tree = trees.tree(PurityFixtures.minimalService(protocol));
            Set<String> requiredModules = policy.requiredClosure(policy.serviceTemplate(), Set.of(protocol));
            for (String module : requiredModules) {
                policy.modules().get(module).rules().requiredArtifacts().forEach(a -> assertTrue(
                    documentedThirdParty.contains(a.groupId() + ":" + a.artifactId()),
                    module + " requires " + a + ", which the documented runtime set does not contain"));
            }
            assertEquals(List.of(), inspector.forbiddenArtifacts(policy.serviceTemplate().forbids(), tree));
            assertEquals(List.of(), inspector.missingRequiredEdges(policy.serviceTemplate(), tree));
            assertEquals(requiredModules, inspector.resolvedV3Modules(tree), "V3 modules of the minimal " + protocol
                                                                             + " service");
            assertEquals(documentedThirdParty, inspector.resolvedThirdPartyArtifacts(tree),
                         "Third-party artifacts of the minimal " + protocol + " service");
        }
    }

    static Stream<Arguments> leakFixtures() {
        return PurityFixtures.leaks(System.getProperty("v3.releaseVersion"), System.getProperty("v3.smithyVersion"),
                                    System.getProperty("v3.smithyJavaVersion"))
                             .stream()
                             .map(leak -> Arguments.of(leak.service(), leak));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("leakFixtures")
    void leakingServiceIsRejectedWithItsDependencyPath(String service, LeakFixture leak) {
        DependencyTree tree = trees.tree(service);
        List<String> violations = new ArrayList<>(inspector.forbiddenArtifacts(policy.serviceTemplate().forbids(),
                                                                               tree));
        violations.addAll(inspector.missingRequiredEdges(policy.serviceTemplate(), tree));
        assertEquals(new TreeSet<>(leak.expectedViolations()), new TreeSet<>(violations));
        assertEquals(leak.expectedViolations().size(), violations.size(), String.join("\n", violations));
    }

    @Test
    void dynamicArtifactsResolveOnlyInTreesThePolicyAllows() {
        // Requirements 14.18 and 15.5 on resolved trees, independent of each module's <forbids>: dynamic-model and
        // dynamic-client are optional artifacts that only the dynamic layer itself and test modules (outside the
        // generated-service lifecycle) resolve. The two leaking fixture services prove that the scan detects them.
        Set<String> dynamic = new TreeSet<>();
        policy.modules().values().stream().filter(m -> m.kind().equals(DYNAMIC_KIND))
              .forEach(m -> dynamic.add(m.artifactId()));
        Set<String> modules = new TreeSet<>(reactor.artifactModules().keySet());
        modules.addAll(PurityFixtures.services(reactor.policy(), System.getProperty("v3.smithyVersion"),
                                               System.getProperty("v3.smithyJavaVersion")).keySet());
        Map<String, Set<String>> holders = new TreeMap<>();
        Map<String, Set<String>> disallowed = new TreeMap<>();
        for (String module : modules) {
            Set<String> resolved = new TreeSet<>(inspector.resolvedV3Modules(trees.tree(module)));
            resolved.retainAll(dynamic);
            if (resolved.isEmpty()) {
                continue;
            }
            holders.put(module, resolved);
            String kind = policy.kindOf(module).orElseThrow();
            boolean allowed = kind.equals(DYNAMIC_KIND)
                              || kind.equals("test") && !reactor.generatedCodeModules().contains(module);
            if (!allowed) {
                disallowed.put(module, resolved);
            }
        }
        assertEquals(Set.of("dynamic-model"), holders.get("dynamic-client"), "the dynamic client's own tree");
        assertEquals(Map.of(PurityFixtures.SERVICE_PREFIX + "leaks-smithy-model-parser", Set.of("dynamic-model"),
                            PurityFixtures.DYNAMIC_CLIENT_LEAK, dynamic),
                     disallowed,
                     "Only the dynamic layer and test modules may resolve dynamic-model or dynamic-client; any other "
                     + "entry is a generated service, runtime module or library whose tree leaks them");
    }

    @Test
    void serviceDependingOnTheDynamicClientIsRejectedWithItsDependencyPaths() {
        DependencyTree tree = trees.tree(PurityFixtures.DYNAMIC_CLIENT_LEAK);
        String release = policy.releaseVersion();
        String client = PurityFixtures.GROUP + ":" + PurityFixtures.DYNAMIC_CLIENT_LEAK + ":" + release + " -> "
                        + PurityFixtures.GROUP + ":dynamic-client:" + release;
        String parser = ": forbidden artifact class [smithy-model-parser]";
        List<String> violations = inspector.forbiddenArtifacts(policy.serviceTemplate().forbids(), tree);
        assertTrue(violations.contains(client + parser), String.join("\n", violations));
        assertTrue(violations.contains(client + " -> " + PurityFixtures.GROUP + ":dynamic-model:" + release + parser),
                   String.join("\n", violations));
        // Everything else the fixture declares is the clean minimal service: every diagnostic is a model-parser
        // artifact reached through the dynamic client.
        violations.forEach(v -> assertTrue(v.startsWith(client) && v.endsWith(parser), v));
        assertEquals(List.of(), inspector.missingRequiredEdges(policy.serviceTemplate(), tree));
    }

    @Test
    void serviceMissingRequiredEdgesIsRejected() {
        DependencyTree tree = trees.tree(PurityFixtures.MISSING_REQUIRED_EDGES);
        String root = PurityFixtures.GROUP + ":" + PurityFixtures.MISSING_REQUIRED_EDGES + ":" + policy.releaseVersion();
        Set<String> protocols = new TreeSet<>(PurityFixtures.protocols(policy));
        assertEquals(List.of(root + ": missing required dependency on V3 module aws-core",
                             root + ": missing required dependency on V3 module aws-pipeline",
                             root + ": missing required dependency on V3 module protocol-core",
                             root + ": missing required dependency on one of the V3 modules " + protocols),
                     inspector.missingRequiredEdges(policy.serviceTemplate(), tree));
        assertEquals(List.of(), inspector.forbiddenArtifacts(policy.serviceTemplate().forbids(), tree));
    }
}
