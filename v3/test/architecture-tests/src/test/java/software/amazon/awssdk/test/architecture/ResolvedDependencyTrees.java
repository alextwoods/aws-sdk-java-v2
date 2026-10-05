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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.test.architecture.DependencyPolicy.Coordinate;
import software.amazon.awssdk.test.architecture.DependencyTree.Node;
import software.amazon.awssdk.test.architecture.PurityFixtures.Dep;
import software.amazon.awssdk.test.architecture.PurityFixtures.Module;

/**
 * The resolved runtime (compile + runtime scope) dependency trees of every V3 reactor module and every
 * {@link PurityFixtures} module, computed once per test JVM.
 *
 * <p>A generated aggregator under {@code target/} lists the real V3 reactor and the fixture modules, and one forked
 * Maven run executes {@code maven-dependency-plugin:tree -Dscope=runtime -Dverbose=true} across it. Because the real
 * modules are part of the forked reactor, their POMs resolve from the checkout and nothing needs to be installed;
 * dependency collection reads POMs only, so no module is compiled and no file outside {@code target/} is written.
 * The trees are therefore exactly what Maven resolves for a consumer of each module's POM (with its own optional
 * dependencies included), independent of the outer build's profile or test classpath.
 */
final class ResolvedDependencyTrees {

    private static ResolvedDependencyTrees instance;

    private final V3Reactor reactor;
    private final DependencyPolicy policy;
    private final String smithyVersion;
    private final String smithyJavaVersion;
    private final Map<String, DependencyTree> trees;

    private ResolvedDependencyTrees(V3Reactor reactor) throws IOException, InterruptedException {
        this.reactor = reactor;
        this.smithyVersion = System.getProperty("v3.smithyVersion");
        this.smithyJavaVersion = System.getProperty("v3.smithyJavaVersion");
        this.policy = reactor.policyWithServices(
            PurityFixtures.services(reactor.policy(), smithyVersion, smithyJavaVersion).keySet());
        this.trees = resolve();
    }

    static synchronized ResolvedDependencyTrees load() {
        if (instance == null) {
            try {
                instance = new ResolvedDependencyTrees(V3Reactor.load());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        return instance;
    }

    V3Reactor reactor() {
        return reactor;
    }

    /** The reactor policy with the reactor's and the fixtures' generated service modules registered. */
    DependencyPolicy policy() {
        return policy;
    }

    String smithyVersion() {
        return smithyVersion;
    }

    /** The tree of a V3 or fixture module ({@code groupId:artifactId}). */
    DependencyTree tree(String groupId, String artifactId) {
        DependencyTree tree = trees.get(groupId + ":" + artifactId);
        if (tree == null) {
            throw new AssertionError("maven-dependency-plugin produced no tree for " + groupId + ":" + artifactId
                                     + "; resolved trees: " + trees.keySet());
        }
        return tree;
    }

    /** The tree of a module in the V3 group. */
    DependencyTree tree(String artifactId) {
        return tree(policy.groupId(), artifactId);
    }

    /**
     * The roots of the resolved runtime class path of a module in the V3 group (excluding the module's own classes),
     * in tree order and keyed by coordinate: {@code target/classes} for V3 reactor modules, local-repository jars
     * otherwise. Fails if a module has not been compiled or a jar has not been downloaded.
     */
    Map<String, Path> runtimeClassPathRoots(String artifactId) {
        Path localRepository = Path.of(System.getProperty("v3.localRepository"));
        Map<String, Path> roots = new LinkedHashMap<>();
        for (Node node : tree(artifactId).resolvedArtifacts()) {
            Coordinate c = node.coordinate();
            if (policy.isV3Module(c)) {
                roots.put(c.toString(), reactor.compiledClasses(c.artifactId()));
            } else if (node.type().equals("jar") || node.type().equals("test-jar")) {
                String classifier = node.type().equals("test-jar") ? "tests" : node.classifier();
                Path jar = localRepository.resolve(c.groupId().replace('.', '/')).resolve(c.artifactId())
                                          .resolve(c.version())
                                          .resolve(c.artifactId() + "-" + c.version()
                                                   + (classifier.isEmpty() ? "" : "-" + classifier) + ".jar");
                if (!Files.isRegularFile(jar)) {
                    throw new AssertionError(c + " (runtime dependency of " + artifactId + ") is not in the local "
                                             + "repository at " + jar + "; build the module first so Maven "
                                             + "downloads it");
                }
                roots.put(c.toString(), jar);
            }
        }
        return roots;
    }

    private Map<String, DependencyTree> resolve() throws IOException, InterruptedException {
        Path work = Path.of(System.getProperty("v3.architectureTests.workDirectory"), "dependency-purity-reactor")
                        .toAbsolutePath().normalize();
        MavenFork.deleteRecursively(work);
        Files.createDirectories(work);

        List<String> modules = new ArrayList<>();
        modules.add(work.relativize(reactor.root()).toString().replace('\\', '/'));
        Map<String, Module> fixtures = new LinkedHashMap<>();
        PurityFixtures.libraries(smithyVersion).forEach(m -> fixtures.put(m.coordinate().artifactId(), m));
        fixtures.putAll(PurityFixtures.services(reactor.policy(), smithyVersion, smithyJavaVersion));
        for (Module fixture : fixtures.values()) {
            String directory = "fixtures/" + fixture.coordinate().groupId() + "/" + fixture.coordinate().artifactId();
            MavenFork.write(work.resolve(directory).resolve("pom.xml"), pom(fixture));
            modules.add(directory);
        }
        StringBuilder aggregator = new StringBuilder();
        modules.forEach(m -> aggregator.append("<module>").append(m).append("</module>"));
        MavenFork.write(work.resolve("pom.xml"), project(
            "<groupId>" + PurityFixtures.FIXTURE_GROUP + "</groupId><artifactId>dependency-purity-reactor</artifactId>"
            + "<version>" + PurityFixtures.FIXTURE_VERSION + "</version><packaging>pom</packaging><modules>"
            + aggregator + "</modules>"));

        Path output = work.resolve("dependency-trees.txt");
        MavenFork.Result result = MavenFork.run(
            work,
            "org.apache.maven.plugins:maven-dependency-plugin:" + System.getProperty("v3.dependencyPluginVersion")
            + ":tree",
            "-Dscope=runtime", "-Dverbose=true", "-DappendOutput=true", "-DoutputFile=" + output);
        if (result.exitCode() != 0 || !Files.isRegularFile(output)) {
            throw new AssertionError("Resolving dependency trees failed (exit " + result.exitCode() + "):\n"
                                     + result.output());
        }
        return DependencyTree.parseAll(Files.readString(output, StandardCharsets.UTF_8));
    }

    private static String pom(Module module) {
        StringBuilder dependencies = new StringBuilder();
        for (Dep dep : module.dependencies()) {
            Coordinate c = dep.coordinate();
            dependencies.append("<dependency><groupId>").append(c.groupId()).append("</groupId><artifactId>")
                        .append(c.artifactId()).append("</artifactId><version>").append(c.version())
                        .append("</version>").append(dep.optional() ? "<optional>true</optional>" : "")
                        .append("</dependency>");
        }
        Coordinate c = module.coordinate();
        return project("<groupId>" + c.groupId() + "</groupId><artifactId>" + c.artifactId() + "</artifactId>"
                       + "<version>" + c.version() + "</version><dependencies>" + dependencies + "</dependencies>");
    }

    private static String project(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
               + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion>"
               + body + "</project>\n";
    }
}
