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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.test.architecture.DependencyPolicy.Coordinate;

/**
 * A module's resolved Maven dependency tree, parsed from the text output of {@code maven-dependency-plugin:tree}
 * with {@code -Dverbose=true}.
 *
 * <p>Verbose output keeps the nodes that Maven's conflict resolution omitted (duplicates and losing versions) in
 * parentheses, so every dependency path that reaches an artifact is visible, not only the one that won mediation.
 * The children of an omitted node are not repeated; they appear under the node that was kept.
 */
public final class DependencyTree {

    private static final String TREE_CHARACTERS = "| +-\\";

    private final Node root;

    private DependencyTree(Node root) {
        this.root = root;
    }

    /**
     * A tree node. {@code scope} is empty for the root. {@code omitted} nodes were removed by Maven's conflict
     * resolution because the same artifact is reached through another path.
     */
    public record Node(Coordinate coordinate, String type, String classifier, String scope, boolean omitted,
                       List<Node> children) {

        /** {@code groupId:artifactId}. */
        public String key() {
            return coordinate.groupId() + ":" + coordinate.artifactId();
        }
    }

    /**
     * Parses the concatenated tree output of one or more modules (for example a reactor run with
     * {@code -DappendOutput=true}); each line at depth zero starts a new tree.
     *
     * @return trees keyed by the root's {@code groupId:artifactId}, in output order
     */
    public static Map<String, DependencyTree> parseAll(String text) {
        Map<String, DependencyTree> result = new LinkedHashMap<>();
        Deque<List<Node>> openChildren = new ArrayDeque<>();
        int lineNumber = 0;
        for (String line : text.split("\\R")) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            int start = 0;
            while (start < line.length() && TREE_CHARACTERS.indexOf(line.charAt(start)) >= 0) {
                start++;
            }
            if (start % 3 != 0) {
                throw new IllegalArgumentException("Unexpected tree indentation at line " + lineNumber + ": " + line);
            }
            int depth = start / 3;
            // Depth 0 always starts a new tree; any other depth must attach to an open parent.
            if (depth > openChildren.size()) {
                throw new IllegalArgumentException("Tree line without a parent at line " + lineNumber + ": " + line);
            }
            Node node = parseNode(line.substring(start), depth == 0, lineNumber);
            if (depth == 0) {
                openChildren.clear();
                if (result.put(node.key(), new DependencyTree(node)) != null) {
                    throw new IllegalArgumentException("Duplicate tree for " + node.key() + " at line " + lineNumber);
                }
            } else {
                while (openChildren.size() > depth) {
                    openChildren.pop();
                }
                openChildren.peek().add(node);
            }
            openChildren.push(node.children());
        }
        return Collections.unmodifiableMap(result);
    }

    public Node root() {
        return root;
    }

    /** The root's direct dependencies. */
    public List<Node> directDependencies() {
        return root.children();
    }

    /** Every artifact on the resolved runtime class path: the non-omitted nodes other than the root, in tree order. */
    public List<Node> resolvedArtifacts() {
        List<Node> result = new ArrayList<>();
        for (List<Node> path : paths()) {
            Node node = path.get(path.size() - 1);
            if (!node.omitted()) {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * Every dependency path, including paths that end at an omitted node. Each path starts at the root and ends at the
     * node it describes; the root alone is not a path.
     */
    public List<List<Node>> paths() {
        List<List<Node>> result = new ArrayList<>();
        collect(new ArrayList<>(List.of(root)), result);
        return result;
    }

    /** Renders a path as {@code g:a:v -> g:a:v -> ...}. */
    public static String describe(List<Node> path) {
        return path.stream().map(n -> n.coordinate().toString()).collect(Collectors.joining(" -> "));
    }

    private static void collect(List<Node> path, List<List<Node>> result) {
        for (Node child : path.get(path.size() - 1).children()) {
            path.add(child);
            result.add(List.copyOf(path));
            collect(path, result);
            path.remove(path.size() - 1);
        }
    }

    private static Node parseNode(String text, boolean root, int lineNumber) {
        boolean omitted = text.startsWith("(");
        String rest = omitted ? text.substring(1) : text;
        int end = 0;
        while (end < rest.length() && rest.charAt(end) != ' ' && rest.charAt(end) != ')') {
            end++;
        }
        // Anything after the coordinate is an annotation, for example "- omitted for duplicate" or
        // "(version managed from ...)". Verbose output does not mark optional dependencies.
        String[] parts = rest.substring(0, end).split(":", -1);
        if (root) {
            if (parts.length != 4 || omitted) {
                throw new IllegalArgumentException("Expected groupId:artifactId:packaging:version at line " + lineNumber
                                                   + ": " + text);
            }
            return new Node(new Coordinate(parts[0], parts[1], parts[3]), parts[2], "", "", false, new ArrayList<>());
        }
        return switch (parts.length) {
            case 5 -> new Node(new Coordinate(parts[0], parts[1], parts[3]), parts[2], "", parts[4], omitted,
                               new ArrayList<>());
            case 6 -> new Node(new Coordinate(parts[0], parts[1], parts[4]), parts[2], parts[3], parts[5], omitted,
                               new ArrayList<>());
            default -> throw new IllegalArgumentException("Expected groupId:artifactId:type[:classifier]:version:scope at "
                                                          + "line " + lineNumber + ": " + text);
        };
    }
}
