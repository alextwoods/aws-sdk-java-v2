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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.awssdk.test.architecture.PomFile.Dependency;

/**
 * Executable model of how Maven 3 selects the version of every artifact in a consumer project's dependency graph when
 * the project imports BOMs. It models the rules that decide whether importing {@code software.amazon.awssdk:bom}
 * aligns a V3 dependency tree:
 *
 * <ul>
 *   <li>The consumer's own {@code dependencyManagement} entries win over imported BOMs, and among imported BOMs the
 *       first import that manages an artifact wins.</li>
 *   <li>A direct dependency with an explicit version keeps that version; a direct dependency without a version takes
 *       the managed version (and is an error when nothing manages it).</li>
 *   <li>Managed versions replace the declared versions of transitive dependencies.</li>
 *   <li>Unmanaged transitive dependencies are mediated nearest-first; among equally near occurrences the first one in
 *       breadth-first declaration order wins. The subtree of a losing occurrence is not visited.</li>
 * </ul>
 *
 * Artifacts are identified by groupId and artifactId only (type {@code jar}, no classifier). Nested BOM imports inside
 * an imported BOM are not modelled and are rejected.
 */
final class MavenResolutionModel {

    private MavenResolutionModel() {
    }

    /** A dependency-management key. */
    record Key(String groupId, String artifactId) implements Comparable<Key> {
        Key {
            Objects.requireNonNull(groupId, "groupId");
            Objects.requireNonNull(artifactId, "artifactId");
        }

        @Override
        public int compareTo(Key other) {
            int byGroup = groupId.compareTo(other.groupId);
            return byGroup != 0 ? byGroup : artifactId.compareTo(other.artifactId);
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId;
        }
    }

    /**
     * A node of a declared dependency graph: the version its parent declares ({@code null} when the declaration omits
     * the version) and the dependencies that the node's own POM declares.
     */
    record Node(Key key, String version, List<Node> children) {
        Node {
            Objects.requireNonNull(key, "key");
            children = List.copyOf(children);
        }
    }

    /** An imported ({@code <scope>import</scope>}) BOM: its managed versions. */
    record ImportedBom(String name, Map<Key, String> managedVersions) {
        ImportedBom {
            managedVersions = Map.copyOf(managedVersions);
        }
    }

    /** A consumer project: declared dependencies in declaration order, own management, and BOM imports in order. */
    record Project(List<Node> dependencies, Map<Key, String> management, List<ImportedBom> imports) {
        Project {
            dependencies = List.copyOf(dependencies);
            management = Map.copyOf(management);
            imports = List.copyOf(imports);
        }
    }

    /**
     * The versions a BOM POM manages. Only plain {@code jar} entries are supported; duplicate keys, nested imports and
     * entries without a version are rejected because Maven would either warn and pick one silently or fail.
     */
    static Map<Key, String> managedVersions(PomFile bom) {
        Map<Key, String> result = new LinkedHashMap<>();
        for (Dependency d : bom.managedDependencies()) {
            if ("import".equals(d.scope()) || !"jar".equals(d.type())) {
                throw new IllegalStateException(bom.path() + ": unsupported managed dependency " + d);
            }
            if (d.version().isEmpty()) {
                throw new IllegalStateException(bom.path() + ": managed dependency without a version " + d);
            }
            String previous = result.put(new Key(d.groupId(), d.artifactId()), d.version());
            if (previous != null) {
                throw new IllegalStateException(bom.path() + ": duplicate managed dependency " + d);
            }
        }
        return result;
    }

    /** The effective dependency management of {@code project}. */
    static Map<Key, String> effectiveManagement(Project project) {
        Map<Key, String> management = new LinkedHashMap<>(project.management());
        for (ImportedBom bom : project.imports()) {
            bom.managedVersions().forEach(management::putIfAbsent);
        }
        return management;
    }

    /** Resolves every artifact of the graph to one version, in breadth-first resolution (class-path) order. */
    static Map<Key, String> resolve(Project project) {
        Map<Key, String> management = effectiveManagement(project);
        Map<Key, String> resolved = new LinkedHashMap<>();
        Set<Key> directKeys = new HashSet<>();
        Deque<Node> queue = new ArrayDeque<>();
        for (Node direct : project.dependencies()) {
            if (!directKeys.add(direct.key())) {
                throw new IllegalStateException("Duplicate direct dependency " + direct.key());
            }
            String version = direct.version() != null ? direct.version() : management.get(direct.key());
            if (version == null) {
                throw new IllegalStateException("'dependencies.dependency.version' for " + direct.key()
                                                + " is missing");
            }
            resolved.put(direct.key(), version);
            queue.addAll(direct.children());
        }
        while (!queue.isEmpty()) {
            Node node = queue.removeFirst();
            if (resolved.containsKey(node.key())) {
                continue; // a nearer or earlier occurrence already won; this subtree is omitted
            }
            String version = management.getOrDefault(node.key(), node.version());
            if (version == null) {
                throw new IllegalStateException("No version for transitive dependency " + node.key());
            }
            resolved.put(node.key(), version);
            queue.addAll(node.children());
        }
        return resolved;
    }
}
