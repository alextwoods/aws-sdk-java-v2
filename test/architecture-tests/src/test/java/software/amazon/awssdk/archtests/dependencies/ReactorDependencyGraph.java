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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * The declared dependency graph of the Maven reactor, read from the {@code pom.xml} files without running Maven
 * (task 9.8, Requirement 1.7; task 9.14, Requirement 35.10).
 *
 * <p>{@link #load(Path)} starts at the root {@code pom.xml} and follows every {@code <module>}, including the modules of
 * profiles that are off by default (for example {@code services/pom.xml}'s {@code fleet-services}). A module's
 * dependencies are its own and those of its reactor parents, from {@code <dependencies>} and from every profile's
 * {@code <dependencies>}. A dependency without a {@code <scope>} takes the scope of the nearest matching
 * {@code <dependencyManagement>} entry in the parent chain, and is {@code compile} otherwise. Imported BOMs are not read.
 *
 * <p>{@link #dependencyPath(String, Predicate)} and {@link #smithyModelPath(String)} walk compile and runtime edges the
 * way Maven resolves them: test and provided
 * dependencies are not transitive, optional dependencies count only for the module that declares them, and
 * {@code <exclusions>} apply below the dependency that declares them. Artifacts outside the reactor are not expanded.
 * Every {@code software.amazon.smithy} artifact except {@code smithy-utils} counts as carrying {@code smithy-model},
 * because every Smithy artifact the SDK uses depends on it and {@code smithy-utils} has no dependencies.
 */
final class ReactorDependencyGraph {
    static final String SMITHY_GROUP_ID = "software.amazon.smithy";
    static final String SMITHY_MODEL = SMITHY_GROUP_ID + ":smithy-model";

    private static final Set<String> SMITHY_ARTIFACTS_WITHOUT_SMITHY_MODEL =
        Collections.singleton(SMITHY_GROUP_ID + ":smithy-utils");
    private static final Set<String> TRANSITIVE_SCOPES = new HashSet<>(Arrays.asList("compile", "runtime"));

    private final Map<String, List<Dependency>> dependencies;
    private final Map<String, String> parents;

    /**
     * @param dependencies each reactor module ({@code groupId:artifactId}) to its effective dependencies
     */
    ReactorDependencyGraph(Map<String, List<Dependency>> dependencies) {
        this(dependencies, Collections.emptyMap());
    }

    /**
     * @param dependencies each reactor module ({@code groupId:artifactId}) to its effective dependencies
     * @param parents each reactor module that has a {@code <parent>} to that parent ({@code groupId:artifactId})
     */
    ReactorDependencyGraph(Map<String, List<Dependency>> dependencies, Map<String, String> parents) {
        this.dependencies = Collections.unmodifiableMap(new LinkedHashMap<>(dependencies));
        this.parents = Collections.unmodifiableMap(new LinkedHashMap<>(parents));
    }

    /**
     * Read the reactor whose root {@code pom.xml} is in {@code rootDir}.
     */
    static ReactorDependencyGraph load(Path rootDir) {
        Map<String, PomFile> poms = new LinkedHashMap<>();
        Deque<Path> pending = new ArrayDeque<>();
        Set<Path> seen = new HashSet<>();
        pending.add(rootDir.resolve("pom.xml").toAbsolutePath().normalize());
        while (!pending.isEmpty()) {
            Path pomPath = pending.remove();
            if (!seen.add(pomPath)) {
                continue;
            }
            PomFile pom = PomFile.read(pomPath);
            poms.put(pom.key, pom);
            for (String module : pom.modules) {
                Path modulePath = pomPath.getParent().resolve(module).normalize();
                pending.add(module.endsWith(".xml") ? modulePath : modulePath.resolve("pom.xml"));
            }
        }

        Map<String, List<Dependency>> graph = new LinkedHashMap<>();
        Map<String, String> parents = new LinkedHashMap<>();
        for (PomFile pom : poms.values()) {
            graph.put(pom.key, effectiveDependencies(pom, poms));
            if (pom.parentKey != null) {
                parents.put(pom.key, pom.parentKey);
            }
        }
        return new ReactorDependencyGraph(graph, parents);
    }

    /**
     * The {@code groupId:artifactId} of every artifact managed by a BOM {@code pom.xml}, such as {@code bom/pom.xml}.
     */
    static Set<String> managedArtifacts(Path bomPom) {
        PomFile bom = PomFile.read(bomPom);
        return Collections.unmodifiableSet(new TreeSet<>(bom.managedScopes.keySet()));
    }

    /**
     * Every reactor module, as {@code groupId:artifactId}.
     */
    Set<String> modules() {
        return dependencies.keySet();
    }

    /**
     * The reactor modules whose {@code <parent>} is {@code parent}, such as the service modules of
     * {@code software.amazon.awssdk:services}.
     */
    Set<String> childrenOf(String parent) {
        Set<String> children = new TreeSet<>();
        parents.forEach((module, moduleParent) -> {
            if (moduleParent.equals(parent)) {
                children.add(module);
            }
        });
        return children;
    }

    /**
     * A compile-scope or runtime-scope dependency path from {@code module} to an artifact that carries
     * {@code smithy-model}, starting with {@code module}, or empty if there is none.
     */
    Optional<List<String>> smithyModelPath(String module) {
        return dependencyPath(module, ReactorDependencyGraph::carriesSmithyModel);
    }

    /**
     * A compile-scope or runtime-scope dependency path from {@code module} to an artifact ({@code groupId:artifactId})
     * that matches {@code target}, starting with {@code module}, or empty if there is none.
     */
    Optional<List<String>> dependencyPath(String module, Predicate<String> target) {
        Deque<String> path = new ArrayDeque<>();
        path.add(module);
        return search(module, target, true, Collections.emptySet(), path, new HashSet<>());
    }

    private Optional<List<String>> search(String node, Predicate<String> target, boolean declaringModule,
                                          Set<String> exclusions, Deque<String> path, Set<String> visited) {
        if (!visited.add(node + exclusions)) {
            return Optional.empty();
        }
        for (Dependency dependency : dependencies.getOrDefault(node, Collections.emptyList())) {
            if (!TRANSITIVE_SCOPES.contains(dependency.scope)
                || (dependency.optional && !declaringModule)
                || isExcluded(dependency.key, exclusions)) {
                continue;
            }
            path.addLast(dependency.key);
            if (target.test(dependency.key)) {
                return Optional.of(new ArrayList<>(path));
            }
            if (dependencies.containsKey(dependency.key)) {
                Set<String> childExclusions = new TreeSet<>(exclusions);
                childExclusions.addAll(dependency.exclusions);
                Optional<List<String>> found = search(dependency.key, target, false, childExclusions, path, visited);
                if (found.isPresent()) {
                    return found;
                }
            }
            path.removeLast();
        }
        return Optional.empty();
    }

    private static boolean carriesSmithyModel(String key) {
        return key.startsWith(SMITHY_GROUP_ID + ":") && !SMITHY_ARTIFACTS_WITHOUT_SMITHY_MODEL.contains(key);
    }

    private static boolean isExcluded(String key, Set<String> exclusions) {
        String[] groupAndArtifact = key.split(":", 2);
        for (String exclusion : exclusions) {
            String[] pattern = exclusion.split(":", 2);
            if (matches(pattern[0], groupAndArtifact[0]) && matches(pattern[1], groupAndArtifact[1])) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String pattern, String value) {
        return "*".equals(pattern) || pattern.equals(value);
    }

    private static List<Dependency> effectiveDependencies(PomFile pom, Map<String, PomFile> poms) {
        List<PomFile> chain = new ArrayList<>();
        PomFile current = pom;
        while (current != null) {
            chain.add(current);
            current = current.parentKey == null ? null : poms.get(current.parentKey);
        }

        // Nearest declaration wins: walk from the module up to the root, keeping the first one seen per key.
        Map<String, Dependency> effective = new LinkedHashMap<>();
        for (PomFile declaring : chain) {
            for (RawDependency raw : declaring.dependencies) {
                if (effective.containsKey(raw.key)) {
                    continue;
                }
                String scope = raw.scope != null ? raw.scope : managedScope(raw.key, chain);
                effective.put(raw.key, new Dependency(raw.key, scope, raw.optional, raw.exclusions));
            }
        }
        return new ArrayList<>(effective.values());
    }

    private static String managedScope(String key, List<PomFile> chain) {
        for (PomFile pom : chain) {
            String scope = pom.managedScopes.get(key);
            if (scope != null) {
                return scope;
            }
        }
        return "compile";
    }

    /**
     * One effective dependency of a module.
     */
    static final class Dependency {
        final String key;
        final String scope;
        final boolean optional;
        final Set<String> exclusions;

        /**
         * @param key {@code groupId:artifactId}
         * @param scope the Maven scope
         * @param optional whether the dependency is {@code <optional>}
         * @param exclusions the {@code groupId:artifactId} exclusion patterns, where either part may be {@code *}
         */
        Dependency(String key, String scope, boolean optional, Set<String> exclusions) {
            this.key = key;
            this.scope = scope;
            this.optional = optional;
            this.exclusions = Collections.unmodifiableSet(new TreeSet<>(exclusions));
        }

        static Dependency of(String key, String scope) {
            return new Dependency(key, scope, false, Collections.emptySet());
        }
    }

    private static final class RawDependency {
        private final String key;
        private final String scope;
        private final boolean optional;
        private final Set<String> exclusions;

        private RawDependency(String key, String scope, boolean optional, Set<String> exclusions) {
            this.key = key;
            this.scope = scope;
            this.optional = optional;
            this.exclusions = exclusions;
        }
    }

    private static final class PomFile {
        private final String key;
        private final String parentKey;
        private final List<String> modules = new ArrayList<>();
        private final List<RawDependency> dependencies = new ArrayList<>();
        private final Map<String, String> managedScopes = new HashMap<>();

        private PomFile(Element project) {
            Element parent = child(project, "parent");
            String groupId = text(project, "groupId");
            if (groupId == null && parent != null) {
                groupId = text(parent, "groupId");
            }
            this.key = groupId + ":" + text(project, "artifactId");
            this.parentKey = parent == null ? null : text(parent, "groupId") + ":" + text(parent, "artifactId");

            List<Element> sections = new ArrayList<>();
            sections.add(project);
            Element profiles = child(project, "profiles");
            if (profiles != null) {
                sections.addAll(children(profiles, "profile"));
            }
            for (Element section : sections) {
                Element moduleList = child(section, "modules");
                if (moduleList != null) {
                    for (Element module : children(moduleList, "module")) {
                        modules.add(module.getTextContent().trim());
                    }
                }
                for (Element dependency : dependencyElements(child(section, "dependencies"))) {
                    dependencies.add(rawDependency(dependency, groupId));
                }
            }
            Element management = child(project, "dependencyManagement");
            for (Element dependency : dependencyElements(management == null ? null : child(management, "dependencies"))) {
                RawDependency managed = rawDependency(dependency, groupId);
                if (!"import".equals(managed.scope)) {
                    managedScopes.put(managed.key, managed.scope == null ? "compile" : managed.scope);
                }
            }
        }

        static PomFile read(Path pomPath) {
            try (InputStream in = Files.newInputStream(pomPath)) {
                return new PomFile(newDocumentBuilder().parse(in).getDocumentElement());
            } catch (IOException | SAXException | ParserConfigurationException e) {
                throw new IllegalStateException("Cannot read " + pomPath, e);
            }
        }

        private static RawDependency rawDependency(Element dependency, String projectGroupId) {
            String groupId = text(dependency, "groupId");
            if ("${project.groupId}".equals(groupId)) {
                groupId = projectGroupId;
            }
            Set<String> exclusions = new TreeSet<>();
            Element exclusionList = child(dependency, "exclusions");
            if (exclusionList != null) {
                for (Element exclusion : children(exclusionList, "exclusion")) {
                    exclusions.add(text(exclusion, "groupId") + ":" + text(exclusion, "artifactId"));
                }
            }
            return new RawDependency(groupId + ":" + text(dependency, "artifactId"),
                                     text(dependency, "scope"),
                                     "true".equals(text(dependency, "optional")),
                                     exclusions);
        }

        private static List<Element> dependencyElements(Element dependencies) {
            return dependencies == null ? Collections.emptyList() : children(dependencies, "dependency");
        }

        private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder();
        }

        private static String text(Element parent, String name) {
            Element element = child(parent, name);
            return element == null ? null : element.getTextContent().trim();
        }

        private static Element child(Element parent, String name) {
            List<Element> matching = children(parent, name);
            return matching.isEmpty() ? null : matching.get(0);
        }

        private static List<Element> children(Element parent, String name) {
            List<Element> matching = new ArrayList<>();
            for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element && name.equals(node.getNodeName())) {
                    matching.add((Element) node);
                }
            }
            return matching;
        }
    }
}
