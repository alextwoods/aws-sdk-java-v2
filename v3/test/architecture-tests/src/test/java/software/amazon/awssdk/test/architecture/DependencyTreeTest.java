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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.test.architecture.DependencyTree.Node;

/** Parsing of verbose {@code dependency:tree} output and path-scoped policy evaluation. */
class DependencyTreeTest {

    private static final String SAMPLE = String.join("\n",
        "com.example:app:jar:1.0",
        "+- com.example:a:jar:1.0:compile",
        "|  +- com.example:b:jar:tests:1.0:runtime",
        "|  \\- (com.example:c:jar:1.0:compile - omitted for duplicate)",
        "\\- com.example:c:jar:1.0:compile (version managed from 0.9)",
        "com.example:empty:pom:1.0",
        "");

    @Test
    void parsesConcatenatedVerboseTrees() {
        Map<String, DependencyTree> trees = DependencyTree.parseAll(SAMPLE);
        assertEquals(List.of("com.example:app", "com.example:empty"), List.copyOf(trees.keySet()));
        DependencyTree app = trees.get("com.example:app");
        assertEquals("jar", app.root().type());
        assertEquals(List.of("com.example:a", "com.example:c"),
                     app.directDependencies().stream().map(Node::key).toList());
        assertEquals(List.of("com.example:a", "com.example:b", "com.example:c"),
                     app.resolvedArtifacts().stream().map(Node::key).toList());

        List<List<Node>> paths = app.paths();
        assertEquals(4, paths.size());
        Node b = paths.get(1).get(2);
        assertEquals("tests", b.classifier());
        assertEquals("runtime", b.scope());
        Node omitted = paths.get(2).get(2);
        assertTrue(omitted.omitted());
        assertEquals("com.example:app:1.0 -> com.example:a:1.0 -> com.example:c:1.0",
                     DependencyTree.describe(paths.get(2)));
        assertEquals("1.0", paths.get(3).get(1).coordinate().version());
        assertFalse(paths.get(3).get(1).omitted());
        assertEquals(List.of(), trees.get("com.example:empty").paths());
    }

    @Test
    void rejectsMalformedTrees() {
        assertThrows(IllegalArgumentException.class, () -> DependencyTree.parseAll("+- com.example:a:jar:1.0:compile"));
        assertThrows(IllegalArgumentException.class, () -> DependencyTree.parseAll("com.example:a:1.0"));
        assertThrows(IllegalArgumentException.class,
                     () -> DependencyTree.parseAll("com.example:a:jar:1.0\n+- com.example:b:1.0"));
        assertThrows(IllegalArgumentException.class,
                     () -> DependencyTree.parseAll("com.example:a:jar:1.0\ncom.example:a:jar:1.0"));
    }

    @Test
    void waiverAppliesOnlyToPathsThroughItsViaModule() {
        // smithy-model is waived through smithy-runtime only; the same artifact reached through another library
        // (here an omitted duplicate, which verbose output still shows) is a violation.
        DependencyPolicy base = DependencyPolicy.load();
        String r = base.releaseVersion();
        DependencyPolicy policy = base.withServiceModules(Set.of("svc"));
        DependencyTree tree = DependencyTree.parseAll(String.join("\n",
            "software.amazon.awssdk:svc:jar:" + r,
            "+- software.amazon.awssdk:smithy-runtime:jar:" + r + ":compile",
            "|  \\- software.amazon.smithy:smithy-model:jar:1.73.0:compile",
            "\\- com.example:tools:jar:1.0:compile",
            "   \\- (software.amazon.smithy:smithy-model:jar:1.73.0:compile - omitted for duplicate)"))
                                            .get("software.amazon.awssdk:svc");
        DependencyTreeInspector inspector = new DependencyTreeInspector(policy);
        assertEquals(List.of("software.amazon.awssdk:svc:" + r + " -> com.example:tools:1.0 -> "
                             + "software.amazon.smithy:smithy-model:1.73.0: forbidden artifact class "
                             + "[smithy-model-parser]"),
                     inspector.forbiddenArtifacts(policy.serviceTemplate().forbids(), tree));
        assertEquals(Set.of("smithy-runtime"), inspector.resolvedV3Modules(tree));
        assertEquals(Set.of("software.amazon.smithy:smithy-model", "com.example:tools"),
                     inspector.resolvedThirdPartyArtifacts(tree));
    }
}
