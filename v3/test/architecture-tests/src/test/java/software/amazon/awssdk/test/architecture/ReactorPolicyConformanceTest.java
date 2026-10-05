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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.test.architecture.DependencyPolicy.Coordinate;
import software.amazon.awssdk.test.architecture.DependencyPolicy.ModuleRule;
import software.amazon.awssdk.test.architecture.PomFile.Dependency;

/**
 * Checks the V3 reactor against the dependency policy: module inventory, BOM contents (Requirement 15.1) and declared
 * module dependencies (Requirement 15.2).
 */
class ReactorPolicyConformanceTest {

    private static V3Reactor reactor;
    private static Path v3Root;
    private static Map<String, PomFile> artifactModules;
    private static DependencyPolicy policy;

    @BeforeAll
    static void loadReactor() {
        reactor = V3Reactor.load();
        v3Root = reactor.root();
        artifactModules = reactor.artifactModules();
        policy = reactor.policy();
    }

    @Test
    void policyListsExactlyTheReactorArtifactModules() {
        Map<String, String> modules = new TreeMap<>();
        artifactModules.keySet().forEach(id -> {
            if (!reactor.isServicePath(reactor.relativePath(id))) {
                modules.put(id, reactor.relativePath(id));
            }
        });
        Map<String, String> listed = new TreeMap<>();
        policy.modules().values().forEach(m -> listed.put(m.artifactId(), m.path()));
        assertEquals(listed, modules,
                     "Every jar/maven-plugin module under v3/ must have exactly one <module> entry (artifactId and "
                     + "path) in bom-internal/src/main/policy/dependency-policy.xml");
    }

    @Test
    void bomManagesExactlyTheReleaseArtifactsAtTheReleaseVersion() {
        PomFile bom = PomFile.read(v3Root.resolve("bom/pom.xml"));
        assertEquals("bom", bom.artifactId());
        assertEquals("pom", bom.packaging());
        assertEquals(policy.releaseVersion(), bom.version());

        List<String> problems = new ArrayList<>();
        Set<String> managed = new TreeSet<>();
        for (Dependency d : bom.managedDependencies()) {
            managed.add(d.artifactId());
            if (!policy.groupId().equals(d.groupId()) || !policy.releaseVersion().equals(d.version())
                || !"compile".equals(d.scope()) || !"jar".equals(d.type())) {
                problems.add(d + " must be " + policy.groupId() + ":" + d.artifactId() + " at ${project.version} "
                             + "with default type and scope");
            }
        }
        assertEquals(List.of(), problems);
        assertEquals(policy.releaseArtifacts(), managed, "BOM entries must equal the policy's release artifacts");
        assertEquals(managed.size(), bom.managedDependencies().size(), "BOM has duplicate entries");
    }

    @Test
    void bomInheritsNoDependencyManagement() {
        // An imported BOM exposes inherited dependencyManagement; the public BOM must manage only V3 artifacts.
        PomFile bom = PomFile.read(v3Root.resolve("bom/pom.xml"));
        assertEquals("aws-sdk-java-v3-parent", bom.parentArtifactId().orElseThrow());
        assertFalse(PomFile.read(v3Root.resolve("parent/pom.xml")).declaresDependencyManagement(),
                    "v3/parent/pom.xml must not declare dependencyManagement; manage third-party versions in "
                    + "bom-internal");
    }

    @Test
    void bomInternalImportsThePublicBom() {
        PomFile internal = PomFile.read(v3Root.resolve("bom-internal/pom.xml"));
        assertTrue(internal.managedDependencies().stream()
                           .anyMatch(d -> d.artifactId().equals("bom") && d.scope().equals("import")
                                          && d.version().equals(policy.releaseVersion())),
                   "bom-internal must import software.amazon.awssdk:bom:${project.version}");
    }

    @Test
    void declaredDependenciesSatisfyThePolicy() {
        List<String> violations = new ArrayList<>();
        artifactModules.forEach((id, pom) -> {
            for (Dependency d : pom.dependencies()) {
                if (!"test".equals(d.scope()) && !"import".equals(d.scope())) {
                    policy.checkDirectDependency(id, new Coordinate(d.groupId(), d.artifactId(), d.version()))
                          .ifPresent(v -> violations.add(v + " (" + v3Root.relativize(pom.path()) + ")"));
                }
            }
        });
        assertEquals(List.of(), violations);
    }

    @Test
    void requiredServiceEdgesNameDeclaredReactorServiceModules() {
        // <requiresService> targets are not <modules> entries, so the schema cannot check them: each one must be a
        // generated service module of this reactor that the requiring module declares at compile scope.
        List<String> violations = new ArrayList<>();
        for (ModuleRule module : policy.modules().values()) {
            for (String service : module.rules().requiredServices()) {
                if (!policy.kindOf(service).filter(DependencyPolicy.SERVICE_KIND::equals).isPresent()) {
                    violations.add(module.artifactId() + " requires " + service
                                   + ", which is not a generated service module of the reactor");
                    continue;
                }
                PomFile pom = artifactModules.get(module.artifactId());
                boolean declared = pom != null && pom.dependencies().stream().anyMatch(
                    d -> policy.groupId().equals(d.groupId()) && service.equals(d.artifactId())
                         && "compile".equals(d.scope()));
                if (!declared) {
                    violations.add(module.artifactId() + " does not declare its required service " + service
                                   + " at compile scope");
                }
            }
        }
        assertEquals(List.of(), violations);
    }

    @Test
    void thisModulePublishesItsRuntimeVersionResource() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader()
                                        .getResourceAsStream("META-INF/aws-sdk-java-v3/modules/architecture-tests"
                                                             + ".properties")) {
            assertTrue(in != null, "The V3 parent POM must generate the module version resource");
            properties.load(in);
        }
        assertEquals("architecture-tests", properties.getProperty("artifactId"));
        assertEquals(policy.releaseVersion(), properties.getProperty("version"));
    }
}
