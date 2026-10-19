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

package software.amazon.awssdk.archtests.dependencies;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.archtests.dependencies.ReactorDependencyGraph.SMITHY_MODEL;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.archtests.dependencies.ReactorDependencyGraph.Dependency;

/**
 * {@code dynamic-client} is the only SDK runtime module that depends on {@code smithy-model} (task 9.8,
 * Requirement 1.7, design K9).
 *
 * <p>A published module is one managed by {@code bom/pom.xml}. Requirement 1.7 exempts the codegen modules, the codegen
 * Maven plugin and test modules; the published ones that depend on {@code smithy-model} are listed with a reason in
 * {@link #BUILD_TIME_MODULES}. Unpublished modules (the test modules under {@code test/}) are not checked. A published
 * test utility that users put on their classpath, such as {@code client-mock-plugin} under {@code test-utils/}, is a
 * runtime module and is checked.
 *
 * <p>The check reads the declared dependency graph from the reactor's {@code pom.xml} files (see
 * {@link ReactorDependencyGraph}) and is transitive: a module that reaches {@code smithy-model} through another reactor
 * module fails, including through {@code dynamic-client}. Requirement 1.7 only says "declares", so this is stricter.
 * A module that is meant to build on {@code dynamic-client} needs a decision and an entry in
 * {@link #BUILD_TIME_MODULES} (or a new allowlist). Requirement 1.6 is enforced with Maven's own resolution by the
 * {@code ban-smithy-runtime} enforcer execution of {@code services/pom.xml}, on each Prototype_Service_Set module.
 *
 * <p>The repository root comes from the {@code dependency-tree.root} system property, which this module's
 * {@code pom.xml} sets.
 */
class DependencyTreeTest {
    private static final String SDK = "software.amazon.awssdk:";
    private static final String DYNAMIC_CLIENT = SDK + "dynamic-client";

    /**
     * Published build-time modules that may depend on {@code smithy-model}, with the reason Requirement 1.7 exempts them.
     */
    private static final Map<String, String> BUILD_TIME_MODULES;

    static {
        Map<String, String> modules = new TreeMap<>();
        modules.put(SDK + "codegen", "codegen module: reads the Smithy model to generate clients");
        modules.put(SDK + "codegen-c2j-to-smithy", "codegen module: the C2J_Converter, used by codegen");
        modules.put(SDK + "codegen-maven-plugin", "the codegen Maven plugin; clients use it as a build plugin");
        BUILD_TIME_MODULES = Collections.unmodifiableMap(modules);
    }

    private static ReactorDependencyGraph reactor;
    private static Set<String> published;

    @BeforeAll
    static void loadReactor() {
        Path root = Paths.get(System.getProperty("dependency-tree.root", "../..")).toAbsolutePath().normalize();
        reactor = ReactorDependencyGraph.load(root);
        published = ReactorDependencyGraph.managedArtifacts(root.resolve("bom").resolve("pom.xml"));
    }

    @Test
    void publishedModules_otherThanDynamicClientAndBuildTimeModules_doNotDependOnSmithyModel() {
        assertThat(violations(reactor, published, BUILD_TIME_MODULES.keySet()))
            .as("published runtime modules with a compile or runtime path to %s", SMITHY_MODEL)
            .isEmpty();
    }

    @Test
    void load_reactor_seesDynamicClientSmithyModelDependency() {
        // Guards the check above against a scan that finds no modules or no dependencies.
        assertThat(reactor.modules()).contains(DYNAMIC_CLIENT, SDK + "sqs", SDK + "schema", SDK + "client-mock-plugin");
        assertThat(published).contains(DYNAMIC_CLIENT);
        assertThat(reactor.smithyModelPath(DYNAMIC_CLIENT)).isPresent();
    }

    @Test
    void buildTimeModules_eachEntry_isPublishedAndDependsOnSmithyModel() {
        // No stale entries: every exemption must still be needed.
        for (String module : BUILD_TIME_MODULES.keySet()) {
            assertThat(published).as("published modules").contains(module);
            assertThat(reactor.smithyModelPath(module)).as("path from %s to %s", module, SMITHY_MODEL).isPresent();
        }
    }

    @Test
    void violations_syntheticGraph_reportTransitiveRuntimePathsOnly() {
        Map<String, List<Dependency>> graph = new LinkedHashMap<>();
        // Reaches smithy-model through a reactor module at runtime scope.
        graph.put("sdk:client", Arrays.asList(Dependency.of("sdk:core", "compile")));
        graph.put("sdk:core", Arrays.asList(Dependency.of("sdk:converter", "runtime")));
        graph.put("sdk:converter", Arrays.asList(Dependency.of(SMITHY_MODEL, "compile")));
        // Not violations: test and provided scope, an optional dependency below the module, an exclusion, smithy-utils.
        graph.put("sdk:tested", Arrays.asList(Dependency.of(SMITHY_MODEL, "test"),
                                              Dependency.of("sdk:converter", "provided")));
        graph.put("sdk:optional-user", Arrays.asList(Dependency.of("sdk:optional-holder", "compile")));
        graph.put("sdk:optional-holder", Arrays.asList(
            new Dependency(SMITHY_MODEL, "compile", true, Collections.emptySet())));
        graph.put("sdk:excluding", Arrays.asList(
            new Dependency("sdk:converter", "compile", false, Collections.singleton("software.amazon.smithy:*"))));
        graph.put("sdk:utils-user", Arrays.asList(Dependency.of("software.amazon.smithy:smithy-utils", "compile")));
        // Unpublished and allowlisted modules are not reported.
        graph.put("sdk:unpublished", Arrays.asList(Dependency.of(SMITHY_MODEL, "compile")));
        graph.put("sdk:allowed", Arrays.asList(Dependency.of(SMITHY_MODEL, "compile")));
        Set<String> syntheticPublished = new HashSet<>(graph.keySet());
        syntheticPublished.remove("sdk:unpublished");

        List<String> violations = violations(new ReactorDependencyGraph(graph), syntheticPublished,
                                             Collections.singleton("sdk:allowed"));

        assertThat(violations).containsExactlyInAnyOrder(
            "sdk:client -> sdk:core -> sdk:converter -> " + SMITHY_MODEL,
            "sdk:core -> sdk:converter -> " + SMITHY_MODEL,
            "sdk:converter -> " + SMITHY_MODEL,
            "sdk:optional-holder -> " + SMITHY_MODEL);
    }

    @Test
    void violations_moduleDependingOnDynamicClient_isReported() {
        Map<String, List<Dependency>> graph = new LinkedHashMap<>();
        graph.put(DYNAMIC_CLIENT, Arrays.asList(Dependency.of(SMITHY_MODEL, "compile")));
        graph.put(SDK + "uses-dynamic-client", Arrays.asList(Dependency.of(DYNAMIC_CLIENT, "compile")));

        List<String> violations = violations(new ReactorDependencyGraph(graph), graph.keySet(), Collections.emptySet());

        assertThat(violations).containsExactly(
            SDK + "uses-dynamic-client -> " + DYNAMIC_CLIENT + " -> " + SMITHY_MODEL);
    }

    /**
     * One {@code module -> ... -> smithy artifact} path per published module, other than {@code dynamic-client} and
     * {@code exempt}, that reaches {@code smithy-model} at compile or runtime scope.
     */
    static List<String> violations(ReactorDependencyGraph graph, Set<String> publishedModules, Set<String> exempt) {
        List<String> violations = new ArrayList<>();
        for (String module : graph.modules()) {
            if (!publishedModules.contains(module) || DYNAMIC_CLIENT.equals(module) || exempt.contains(module)) {
                continue;
            }
            Optional<List<String>> path = graph.smithyModelPath(module);
            path.ifPresent(p -> violations.add(String.join(" -> ", p)));
        }
        return violations;
    }
}
