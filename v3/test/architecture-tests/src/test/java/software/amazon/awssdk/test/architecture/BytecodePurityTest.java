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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary;

/**
 * Requirements 15.3-15.8 on the compiled classes of every production V3 module (release artifacts, generated
 * services, and modules built by the generated-service lifecycle):
 * <ul>
 *     <li>no class references a class of a forbidden artifact class (for example a Smithy parser class inside the
 *     waived {@code smithy-model} jar, or a shaded engine);</li>
 *     <li>every class referenced by a runtime module resolves to the JDK, the module, or an artifact of its runtime
 *     dependency tree, which rules out V2 runtime classes (V2 and V3 share packages) and compile-only
 *     dependencies;</li>
 *     <li>no public or protected API exposes a third-party logging implementation type;</li>
 *     <li>only the optional dynamic modules define {@code software.amazon.awssdk.dynamic} classes or reference
 *     Smithy model parsing and the libraries built on the semantic model (Requirements 14.18, 15.5).</li>
 * </ul>
 * The classes are read from each module's {@code target/classes}, so these checks require the production modules to
 * be compiled earlier in the reactor; {@code v3-all} and {@code v3-tests} build them before {@code test/}.
 */
class BytecodePurityTest {

    private static final String DYNAMIC_PACKAGE = "software.amazon.awssdk.dynamic.";

    private static ResolvedDependencyTrees trees;
    private static V3Reactor reactor;
    private static DependencyPolicy policy;

    @BeforeAll
    static void load() {
        trees = ResolvedDependencyTrees.load();
        reactor = trees.reactor();
        policy = trees.policy();
    }

    @Test
    void everyForbiddenArtifactClassIsEnforcedInBytecode() {
        Set<String> forbidden = new TreeSet<>(policy.serviceTemplate().forbids());
        policy.modules().values().forEach(m -> forbidden.addAll(m.rules().forbids()));
        Set<String> covered = new TreeSet<>(ForbiddenClassNames.ruleClasses());
        covered.addAll(ForbiddenClassNames.TREE_ONLY_CLASSES);
        Set<String> uncovered = new TreeSet<>(forbidden);
        uncovered.removeAll(covered);
        assertEquals(Set.of(), uncovered, "Forbidden artifact classes without a bytecode rule in ForbiddenClassNames "
                                          + "or ForbiddenClassNames.TREE_ONLY_CLASSES");
        Set<String> unknown = new TreeSet<>(covered);
        unknown.removeAll(policy.artifactClasses().keySet());
        assertEquals(Set.of(), unknown, "ForbiddenClassNames names artifact classes the policy does not define");
    }

    @Test
    void everyModuleKindIsClassifiedAsRuntimeOrBuildTime() {
        Set<String> kinds = new TreeSet<>();
        policy.modules().values().forEach(m -> kinds.add(m.kind()));
        kinds.add(DependencyPolicy.SERVICE_KIND);
        Set<String> unclassified = new TreeSet<>(kinds);
        unclassified.removeAll(V3Reactor.RUNTIME_KINDS);
        unclassified.removeAll(V3Reactor.BUILD_TIME_KINDS);
        assertEquals(Set.of(), unclassified, "Add new module kinds to V3Reactor.RUNTIME_KINDS or BUILD_TIME_KINDS");
        Set<String> both = new TreeSet<>(V3Reactor.RUNTIME_KINDS);
        both.retainAll(V3Reactor.BUILD_TIME_KINDS);
        assertEquals(Set.of(), both);
    }

    @Test
    void productionBytecodeReferencesNoForbiddenArtifactClass() {
        List<String> violations = new ArrayList<>();
        for (String module : productionModules()) {
            violations.addAll(BytecodeInspector.forbiddenReferences(module, classesOf(module),
                                                                    reactor.forbiddenClasses(policy, module)));
        }
        assertEquals(List.of(), violations);
    }

    @Test
    void modelParserAndDynamicClassesLiveOnlyInTheDynamicLayer() {
        // Requirements 14.18 and 15.5, independent of each module's policy entry: only the optional dynamic modules
        // define software.amazon.awssdk.dynamic classes, and only they reference Smithy model parsing/assembly, the
        // Smithy libraries built on the semantic model (waiters, rules engine, AWS/protocol traits), Smithy Java's
        // dynamic schemas or the V3 dynamic layer. Generated services, the core runtime, protocols, HTTP
        // implementations and high-level libraries reference none of them.
        Set<String> dynamicModules = new TreeSet<>();
        policy.modules().values().stream().filter(m -> m.kind().equals("dynamic"))
              .forEach(m -> dynamicModules.add(m.artifactId()));
        Map<String, Set<String>> definers = new TreeMap<>();
        Map<String, Set<String>> users = new TreeMap<>();
        for (String module : productionModules()) {
            if (!isRuntime(module)) {
                continue;
            }
            for (ClassFile type : classesOf(module).values()) {
                if (type.name().startsWith(DYNAMIC_PACKAGE)) {
                    definers.computeIfAbsent(module, m -> new TreeSet<>()).add(type.name());
                }
                for (String reference : type.referencedClasses()) {
                    if (SmithyRuntimeBoundary.isForbiddenRuntimeClass(reference)) {
                        users.computeIfAbsent(module, m -> new TreeSet<>()).add(reference);
                    }
                }
            }
        }
        assertEquals(dynamicModules, definers.keySet(), "Modules defining " + DYNAMIC_PACKAGE + " classes");
        Map<String, Set<String>> outside = new TreeMap<>(users);
        outside.keySet().removeAll(dynamicModules);
        assertEquals(Map.of(), outside,
                     "Runtime modules outside the dynamic layer reference Smithy model parsing or dynamic classes");
        // The scan is not vacuous: the dynamic model assembles models and the dynamic client uses the dynamic model.
        assertTrue(users.getOrDefault("dynamic-model", Set.of()).contains("software.amazon.smithy.model.Model"),
                   String.valueOf(users.get("dynamic-model")));
        assertTrue(users.getOrDefault("dynamic-client", Set.of()).stream()
                        .anyMatch(r -> r.startsWith(DYNAMIC_PACKAGE + "model.")),
                   String.valueOf(users.get("dynamic-client")));
    }

    @Test
    void runtimeBytecodeResolvesWithinTheRuntimeDependencyTree() {
        List<String> violations = new ArrayList<>();
        for (String module : productionModules()) {
            if (isRuntime(module)) {
                violations.addAll(BytecodeInspector.unresolvedReferences(module, classesOf(module),
                                                                         runtimeClassPath(module)));
            }
        }
        assertEquals(List.of(), violations);
    }

    @Test
    void publicAndProtectedApisExposeNoLoggingImplementationTypes() {
        List<String> violations = new ArrayList<>();
        for (String module : productionModules()) {
            violations.addAll(BytecodeInspector.loggingImplementationApiTypes(module, classesOf(module)));
        }
        assertEquals(List.of(), violations);
    }

    /** Release artifacts, generated services, and generated-code modules. */
    private static Set<String> productionModules() {
        Set<String> result = reactor.productionModules();
        assertFalse(result.isEmpty());
        return result;
    }

    private static boolean isRuntime(String module) {
        return reactor.generatedCodeModules().contains(module)
               || V3Reactor.RUNTIME_KINDS.contains(policy.kindOf(module).orElseThrow());
    }

    private static Map<String, ClassFile> classesOf(String module) {
        return reactor.classesOf(module);
    }

    /** The module's resolved runtime class path: reactor classes for V3 modules, local-repository jars otherwise. */
    private static ClassPath runtimeClassPath(String module) {
        return ClassPath.of(trees.runtimeClassPathRoots(module));
    }
}
