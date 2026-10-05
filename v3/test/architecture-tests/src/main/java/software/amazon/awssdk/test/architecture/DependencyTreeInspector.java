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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.test.architecture.DependencyPolicy.Coordinate;
import software.amazon.awssdk.test.architecture.DependencyPolicy.EdgeRules;
import software.amazon.awssdk.test.architecture.DependencyTree.Node;

/**
 * Checks resolved runtime dependency trees (compile and runtime scope) against the {@link DependencyPolicy}: forbidden
 * artifact classes anywhere in the tree (Requirements 15.3-15.8) and required edges (Requirement 15.9). Every
 * diagnostic names the full dependency path from the module to the offending artifact.
 */
public final class DependencyTreeInspector {

    private final DependencyPolicy policy;

    public DependencyTreeInspector(DependencyPolicy policy) {
        this.policy = policy;
    }

    /**
     * Returns one diagnostic per dependency path that reaches an artifact of a class in {@code forbiddenClassIds}.
     * Paths to omitted (mediated-away) nodes count: the artifact is on the class path through another path, and this
     * path is an additional declared route to it. A waiver applies only when its "via" module is on the path, which
     * includes the root module itself. A module's optional dependencies count as well: they are part of its own
     * runtime tree even though Maven does not propagate them to consumers.
     */
    public List<String> forbiddenArtifacts(Set<String> forbiddenClassIds, DependencyTree tree) {
        EdgeRules forbidsOnly = new EdgeRules(Set.of(), List.of(), Set.of(), List.of(), Set.of(), forbiddenClassIds);
        Set<String> result = new LinkedHashSet<>();
        for (List<Node> path : tree.paths()) {
            Node node = path.get(path.size() - 1);
            List<String> classes = policy.forbiddenClasses(forbidsOnly, node.coordinate(), v3ModulesOn(path));
            if (!classes.isEmpty()) {
                result.add(DependencyTree.describe(path) + ": forbidden artifact class " + classes);
            }
        }
        return List.copyOf(result);
    }

    /** Returns one diagnostic per required module edge, requiresAnyOf group or required artifact that is not declared. */
    public List<String> missingRequiredEdges(EdgeRules rules, DependencyTree tree) {
        Set<String> directModules = new TreeSet<>();
        Set<String> directArtifacts = new TreeSet<>();
        for (Node node : tree.directDependencies()) {
            directArtifacts.add(node.key());
            if (policy.isV3Module(node.coordinate())) {
                directModules.add(node.coordinate().artifactId());
            }
        }
        String root = tree.root().coordinate().toString();
        List<String> result = new ArrayList<>();
        for (String required : rules.requires()) {
            if (!directModules.contains(required)) {
                result.add(root + ": missing required dependency on V3 module " + required);
            }
        }
        for (String required : rules.requiredServices()) {
            if (!directModules.contains(required)) {
                result.add(root + ": missing required dependency on generated service module " + required);
            }
        }
        for (Set<String> group : rules.requiresAnyOf()) {
            if (group.stream().noneMatch(directModules::contains)) {
                result.add(root + ": missing required dependency on one of the V3 modules " + new TreeSet<>(group));
            }
        }
        for (Coordinate artifact : rules.requiredArtifacts()) {
            if (!directArtifacts.contains(artifact.groupId() + ":" + artifact.artifactId())) {
                result.add(root + ": missing required dependency on artifact " + artifact);
            }
        }
        return result;
    }

    /** Artifact IDs of the V3 modules on the resolved runtime class path, excluding the root. */
    public Set<String> resolvedV3Modules(DependencyTree tree) {
        Set<String> result = new TreeSet<>();
        for (Node node : tree.resolvedArtifacts()) {
            if (policy.isV3Module(node.coordinate())) {
                result.add(node.coordinate().artifactId());
            }
        }
        return result;
    }

    /** {@code groupId:artifactId} of the non-V3 artifacts on the resolved runtime class path. */
    public Set<String> resolvedThirdPartyArtifacts(DependencyTree tree) {
        Set<String> result = new TreeSet<>();
        for (Node node : tree.resolvedArtifacts()) {
            if (!policy.isV3Module(node.coordinate())) {
                result.add(node.key());
            }
        }
        return result;
    }

    private Set<String> v3ModulesOn(List<Node> path) {
        Set<String> result = new TreeSet<>();
        for (Node node : path) {
            if (policy.isV3Module(node.coordinate())) {
                result.add(node.coordinate().artifactId());
            }
        }
        return result;
    }
}
