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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.archtests.dependencies.ReactorDependencyGraph.Dependency;

/**
 * No generated client module depends on {@code client-mock-plugin} at compile or runtime scope (task 9.14,
 * Requirement 35.10). The Mock_Plugin and Response_Generator are published for users' test classpaths only.
 *
 * <p>A generated module is a reactor module whose parent is {@code software.amazon.awssdk:services} (every module under
 * {@code services/}, including the {@code fleet-services} profile). The check is transitive over compile and runtime
 * edges, with the rules of {@link ReactorDependencyGraph}; a {@code test}-scope dependency is allowed.
 *
 * <p>The repository root comes from the {@code dependency-tree.root} system property, which this module's
 * {@code pom.xml} sets.
 */
class DependencyScopeTest {
    private static final String SDK = "software.amazon.awssdk:";
    private static final String SERVICES = SDK + "services";
    private static final String CLIENT_MOCK_PLUGIN = SDK + "client-mock-plugin";

    private static ReactorDependencyGraph reactor;

    @BeforeAll
    static void loadReactor() {
        Path root = Paths.get(System.getProperty("dependency-tree.root", "../..")).toAbsolutePath().normalize();
        reactor = ReactorDependencyGraph.load(root);
    }

    @Test
    void generatedModules_doNotDependOnClientMockPluginAtRuntime() {
        assertThat(violations(reactor, reactor.childrenOf(SERVICES)))
            .as("generated modules with a compile or runtime path to %s", CLIENT_MOCK_PLUGIN)
            .isEmpty();
    }

    @Test
    void load_reactor_seesGeneratedModulesAndClientMockPlugin() {
        // Guards the check above against a scan that finds no generated modules.
        assertThat(reactor.modules()).contains(CLIENT_MOCK_PLUGIN);
        assertThat(reactor.childrenOf(SERVICES)).contains(SDK + "sqs", SDK + "secretsmanager", SDK + "account",
                                                          SDK + "route53", SDK + "sns", SDK + "cloudwatch",
                                                          SDK + "dynamodb");
    }

    @Test
    void violations_syntheticGraph_reportCompileAndRuntimePathsOnly() {
        Map<String, List<Dependency>> graph = new LinkedHashMap<>();
        Map<String, String> parents = new LinkedHashMap<>();
        // Direct compile and transitive runtime dependencies are reported.
        graph.put(SDK + "direct", Arrays.asList(Dependency.of(CLIENT_MOCK_PLUGIN, "compile")));
        graph.put(SDK + "transitive", Arrays.asList(Dependency.of(SDK + "helper", "compile")));
        graph.put(SDK + "helper", Arrays.asList(Dependency.of(CLIENT_MOCK_PLUGIN, "runtime")));
        // A test-scope dependency is allowed.
        graph.put(SDK + "tested", Arrays.asList(Dependency.of(CLIENT_MOCK_PLUGIN, "test")));
        // A module that is not generated is not checked.
        graph.put(SDK + "not-generated", Arrays.asList(Dependency.of(CLIENT_MOCK_PLUGIN, "compile")));
        for (String module : Arrays.asList("direct", "transitive", "tested")) {
            parents.put(SDK + module, SERVICES);
        }
        ReactorDependencyGraph synthetic = new ReactorDependencyGraph(graph, parents);

        List<String> violations = violations(synthetic, synthetic.childrenOf(SERVICES));

        assertThat(violations).containsExactlyInAnyOrder(
            SDK + "direct -> " + CLIENT_MOCK_PLUGIN,
            SDK + "transitive -> " + SDK + "helper -> " + CLIENT_MOCK_PLUGIN);
    }

    /**
     * One {@code module -> ... -> client-mock-plugin} path per module in {@code generated} that reaches
     * {@code client-mock-plugin} at compile or runtime scope.
     */
    static List<String> violations(ReactorDependencyGraph graph, Set<String> generated) {
        List<String> violations = new ArrayList<>();
        for (String module : generated) {
            Optional<List<String>> path = graph.dependencyPath(module, CLIENT_MOCK_PLUGIN::equals);
            path.ifPresent(p -> violations.add(String.join(" -> ", p)));
        }
        Collections.sort(violations);
        return violations;
    }
}
