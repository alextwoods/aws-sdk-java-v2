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

package software.amazon.awssdk.test.v2migration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Maven dependency graph read from the text output of {@code maven-dependency-plugin:tree} (optionally
 * {@code -Dverbose}). Artifacts are identified by {@code groupId:artifactId}. In verbose output an omitted occurrence
 * (a duplicate or a losing conflict) lists no children; its children are those of the occurrence Maven kept, so
 * {@link #closure} is the set of artifacts reachable through every occurrence, which is the dependency set Maven
 * resolves for a consumer that declares the roots (apart from path-specific exclusions).
 */
final class DependencyGraph {

    /** One resolved artifact: the occurrence Maven kept. */
    record Node(String groupId, String artifactId, String type, String classifier, String version, String scope,
                List<String> trail) {
        String key() {
            return groupId + ":" + artifactId;
        }
    }

    private final Node root;
    private final Map<String, Node> nodes;
    private final Map<String, Set<String>> edges;

    private DependencyGraph(Node root, Map<String, Node> nodes, Map<String, Set<String>> edges) {
        this.root = root;
        this.nodes = nodes;
        this.edges = edges;
    }

    /** Parses tree output; {@code origin} names it in errors. */
    static DependencyGraph parse(String text, String origin) {
        Node root = null;
        Map<String, Node> nodes = new LinkedHashMap<>();
        Map<String, Set<String>> edges = new LinkedHashMap<>();
        Deque<String> path = new ArrayDeque<>();
        for (String raw : text.split("\\R")) {
            if (raw.isBlank()) {
                continue;
            }
            int start = 0;
            while (start < raw.length() && "|+\\- ".indexOf(raw.charAt(start)) >= 0) {
                start++;
            }
            if (start % 3 != 0) {
                throw new IllegalArgumentException(origin + ": unexpected tree line " + raw);
            }
            int depth = start / 3;
            String body = raw.substring(start).strip();
            boolean omitted = body.startsWith("(");
            if (omitted) {
                int end = body.indexOf(" - ");
                body = body.substring(1, end < 0 ? body.lastIndexOf(')') : end);
            }
            int note = body.indexOf(' ');
            String coordinates = note < 0 ? body : body.substring(0, note);
            String[] parts = coordinates.split(":");
            if (depth == 0) {
                if (root != null || parts.length < 4) {
                    throw new IllegalArgumentException(origin + ": unexpected tree root " + raw);
                }
                root = new Node(parts[0], parts[1], parts[2], null, parts[parts.length - 1], null, List.of());
                path.push(root.key());
                continue;
            }
            if (parts.length != 5 && parts.length != 6) {
                throw new IllegalArgumentException(origin + ": unexpected tree node " + raw);
            }
            while (path.size() > depth) {
                path.pop();
            }
            String key = parts[0] + ":" + parts[1];
            String parent = path.peek();
            edges.computeIfAbsent(parent, k -> new LinkedHashSet<>()).add(key);
            if (!omitted) {
                List<String> trail = new ArrayList<>();
                path.descendingIterator().forEachRemaining(trail::add);
                trail.remove(0);
                trail.add(key);
                String classifier = parts.length == 6 ? parts[3] : null;
                nodes.putIfAbsent(key, new Node(parts[0], parts[1], parts[2], classifier, parts[parts.length - 2],
                                                parts[parts.length - 1], List.copyOf(trail)));
            }
            path.push(key);
        }
        if (root == null) {
            throw new IllegalArgumentException(origin + ": no dependency tree");
        }
        return new DependencyGraph(root, Collections.unmodifiableMap(nodes), edges);
    }

    Node root() {
        return root;
    }

    /** Every resolved artifact (the kept occurrences), in tree order. */
    List<Node> nodes() {
        return List.copyOf(nodes.values());
    }

    Node node(String key) {
        return nodes.get(key);
    }

    /** {@code roots} and every artifact reachable from them, in breadth-first order. */
    Set<String> closure(Set<String> roots) {
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            String next = queue.poll();
            if (seen.add(next)) {
                queue.addAll(edges.getOrDefault(next, Set.of()));
            }
        }
        return seen;
    }
}
