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

package software.amazon.awssdk.test.generated.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * Reads {@code compatibility-fixtures.json}: the source, linkage, signature/hierarchy and behavior fixtures generated
 * from the Parity Manifest for this module's test scope ({@code compatibility-scope.txt}) by the parity-manifest
 * tool's {@code CompatibilityFixtureGenerator}, which documents each fixture kind. The tool's
 * {@code CompatibilityFixturesGoldenTest} keeps the file current.
 */
public final class CompatibilityFixtures {
    /** The class every generated caller declares, in the unnamed package. */
    public static final String CALLER_CLASS = "CompatCaller";

    private static final String RESOURCE = "/compatibility-fixtures.json";
    private static final ObjectNode DOCUMENT = load();

    private CompatibilityFixtures() {
    }

    /**
     * The V2 element a fixture targets.
     *
     * @param kind        {@code TYPE}, {@code METHOD}, {@code CONSTRUCTOR} or {@code FIELD}
     * @param owner       binary name of the type or of the member's declaring type
     * @param ownerKind   {@code class}, {@code interface}, {@code enum} or {@code annotation}
     * @param ownerFinal  whether the V2 owner is final
     * @param name        member name, or {@code null} for a type
     * @param descriptor  member JVM descriptor, or {@code null} for a type
     * @param isStatic    whether the V2 member is static
     * @param isProtected whether the V2 member or nested type is protected
     */
    public record Target(String kind, String owner, String ownerKind, boolean ownerFinal, String name,
                         String descriptor, boolean isStatic, boolean isProtected) {
    }

    /** One generated fixture; see the generator for the meaning of each kind. */
    public record Fixture(String id, String entryId, String classification, String kind, String dimension,
                          String v2Surface, Target target, String source, List<String> symbols, String diagnostic,
                          List<String> supertypes, List<String> evidenceIds) {
        /** Parameterized-test display name. */
        @Override
        public String toString() {
            return id;
        }
    }

    /** The V2 version whose API jars the fixtures were generated from. */
    public static String v2Version() {
        return DOCUMENT.expectObjectMember("baseline").expectStringMember("v2Version").getValue();
    }

    /** Every fixture. */
    public static Stream<Fixture> all() {
        return DOCUMENT.expectArrayMember("fixtures").getElementsAs(ObjectNode.class).stream()
                       .map(CompatibilityFixtures::fixture);
    }

    /** The fixtures of the given kinds, in document order. */
    public static Stream<Fixture> ofKinds(String... kinds) {
        Set<String> wanted = Set.of(kinds);
        return all().filter(f -> wanted.contains(f.kind()));
    }

    private static Fixture fixture(ObjectNode node) {
        Node target = node.expectMember("target");
        return new Fixture(string(node, "id"), string(node, "entryId"), string(node, "classification"),
                           string(node, "kind"), string(node, "dimension"), string(node, "v2Surface"),
                           target.isNullNode() ? null : target(target.expectObjectNode()), string(node, "source"),
                           strings(node, "symbols"), string(node, "diagnostic"), strings(node, "supertypes"),
                           strings(node, "evidenceIds"));
    }

    private static Target target(ObjectNode node) {
        return new Target(string(node, "kind"), string(node, "owner"), string(node, "ownerKind"),
                          node.expectBooleanMember("ownerFinal").getValue(), string(node, "name"),
                          string(node, "descriptor"), node.expectBooleanMember("static").getValue(),
                          node.expectBooleanMember("protected").getValue());
    }

    private static String string(ObjectNode node, String member) {
        Node value = node.expectMember(member);
        return value.isNullNode() ? null : value.expectStringNode().getValue();
    }

    private static List<String> strings(ObjectNode node, String member) {
        return node.expectArrayMember(member).getElementsAs(StringNode.class).stream().map(StringNode::getValue)
                   .toList();
    }

    private static ObjectNode load() {
        try (InputStream in = CompatibilityFixtures.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + RESOURCE);
            }
            return Node.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)).expectObjectNode();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
