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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The V3 reactor under test ({@code -Dv3.root}): its jar/maven-plugin modules and the dependency policy with the
 * reactor's generated service modules registered. Loaded once per test JVM.
 */
final class V3Reactor {

    /** Parent POM of every generated service module (and of fixture modules that exercise generated code). */
    static final String SERVICES_PARENT = "v3-services";

    /**
     * Module kinds that customers put on their runtime class path. Their bytecode must resolve entirely within their
     * published runtime dependency tree.
     */
    static final Set<String> RUNTIME_KINDS = Set.of("api", "runtime", "protocol", "http-spi", "http-implementation",
                                                    "telemetry-spi", "telemetry-exporter", "dynamic",
                                                    "high-level-library", DependencyPolicy.SERVICE_KIND);

    /** Build-time and verification module kinds, which may compile against provided or tooling dependencies. */
    static final Set<String> BUILD_TIME_KINDS = Set.of("codegen", "migration-tool", "release-tooling", "test");

    private static final Set<String> SKIPPED_DIRECTORIES = Set.of("target", "src", ".git", ".mvn", "node_modules");

    private static V3Reactor instance;

    private final Path root;
    private final Map<String, PomFile> artifactModules;
    private final DependencyPolicy basePolicy;
    private final DependencyPolicy policy;
    private final Map<String, Map<String, ClassFile>> classes = new HashMap<>();

    private V3Reactor(Path root) {
        this.root = root;
        this.artifactModules = scanArtifactModules(root);
        this.basePolicy = DependencyPolicy.load();
        this.policy = basePolicy.withServiceModules(serviceModules());
    }

    static synchronized V3Reactor load() {
        if (instance == null) {
            instance = new V3Reactor(Path.of(System.getProperty("v3.root")).toAbsolutePath().normalize());
        }
        return instance;
    }

    Path root() {
        return root;
    }

    /** Jar and maven-plugin modules keyed by artifact ID. */
    Map<String, PomFile> artifactModules() {
        return artifactModules;
    }

    /** The policy with every generated service module of the reactor registered. */
    DependencyPolicy policy() {
        return policy;
    }

    /** The policy with the reactor's generated service modules plus {@code extraServices} registered. */
    DependencyPolicy policyWithServices(Set<String> extraServices) {
        Set<String> services = new TreeSet<>(serviceModules());
        services.addAll(extraServices);
        return basePolicy.withServiceModules(services);
    }

    Path moduleDirectory(String artifactId) {
        PomFile pom = artifactModules.get(artifactId);
        if (pom == null) {
            throw new IllegalArgumentException("No reactor module " + artifactId);
        }
        return pom.path().getParent();
    }

    /** Module directory relative to the V3 root, with '/' separators. */
    String relativePath(String artifactId) {
        return root.relativize(moduleDirectory(artifactId)).toString().replace('\\', '/');
    }

    /** True if {@code path} (relative to the V3 root) matches the policy's generated-service template path. */
    boolean isServicePath(String path) {
        String prefix = basePolicy.serviceTemplatePath().replace("*", "");
        return path.startsWith(prefix) && path.indexOf('/', prefix.length()) < 0;
    }

    /** Generated service modules: artifact modules under the service template path. */
    Set<String> serviceModules() {
        Set<String> result = new TreeSet<>();
        artifactModules.keySet().stream().filter(id -> isServicePath(relativePath(id))).forEach(result::add);
        return result;
    }

    /**
     * Modules whose sources come from the generated-service lifecycle: every module that inherits from
     * {@value #SERVICES_PARENT}, including fixture modules outside {@code services/} such as
     * {@code codegen-lifecycle-tests}. Their dependency trees and bytecode must satisfy the service template's
     * forbidden artifact classes in addition to their own rules.
     */
    Set<String> generatedCodeModules() {
        Set<String> result = new TreeSet<>();
        artifactModules.forEach((id, pom) -> {
            if (pom.parentArtifactId().filter(SERVICES_PARENT::equals).isPresent()) {
                result.add(id);
            }
        });
        return result;
    }

    /**
     * The forbidden artifact classes of a reactor module under {@code p}: its own rules, plus the service template's
     * for {@linkplain #generatedCodeModules() generated-code modules}.
     */
    Set<String> forbiddenClasses(DependencyPolicy p, String artifactId) {
        Set<String> result = new TreeSet<>(p.rulesFor(artifactId)
                                            .orElseThrow(() -> new IllegalStateException("No policy for " + artifactId))
                                            .forbids());
        if (generatedCodeModules().contains(artifactId)) {
            result.addAll(p.serviceTemplate().forbids());
        }
        return result;
    }

    /**
     * Release artifacts, generated services, and generated-code modules: every module whose compiled classes the
     * bytecode checks require, because {@code v3-all} and {@code v3-tests} build them before {@code test/}.
     */
    Set<String> productionModules() {
        Set<String> result = new TreeSet<>(serviceModules());
        result.addAll(generatedCodeModules());
        policy.modules().values().stream().filter(DependencyPolicy.ModuleRule::release)
              .map(DependencyPolicy.ModuleRule::artifactId).forEach(result::add);
        return result;
    }

    /** The module's {@code target/classes}; fails if the module has not been compiled. */
    Path compiledClasses(String artifactId) {
        Path classes = moduleDirectory(artifactId).resolve("target/classes");
        if (!Files.isDirectory(classes)) {
            throw new AssertionError(artifactId + " has not been compiled (" + classes + " is missing). The "
                                     + "architecture tests inspect the bytecode of every production module; build "
                                     + "them first, for example with mvn -f v3/pom.xml verify.");
        }
        return classes;
    }

    /** The module's compiled main classes, read once per test JVM. */
    synchronized Map<String, ClassFile> classesOf(String artifactId) {
        return classes.computeIfAbsent(artifactId, m -> ClassPath.readClasses(compiledClasses(m)));
    }

    private static Map<String, PomFile> scanArtifactModules(Path root) {
        Map<String, PomFile> result = new TreeMap<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return !dir.equals(root) && SKIPPED_DIRECTORIES.contains(dir.getFileName().toString())
                           ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().equals("pom.xml")) {
                        PomFile pom = PomFile.read(file);
                        if (!"pom".equals(pom.packaging())) {
                            PomFile previous = result.put(pom.artifactId(), pom);
                            if (previous != null) {
                                throw new IllegalStateException("Duplicate artifactId " + pom.artifactId() + ": "
                                                                + previous.path() + ", " + file);
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("No V3 artifact modules found under " + root);
        }
        return Collections.unmodifiableMap(result);
    }
}
